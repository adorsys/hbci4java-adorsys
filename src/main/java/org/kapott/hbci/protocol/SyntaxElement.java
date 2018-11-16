/*  $Id: SyntaxElement.java,v 1.1 2011/05/04 22:38:03 willuhn Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2008  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    HBCI4Java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.protocol;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.NoSuchPathException;
import org.kapott.hbci.manager.HBCIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/* ein syntaxelement ist ein strukturelement einer hbci-nachricht (die nachricht
    selbst, eine segmentfolge, ein einzelnes segment, eine deg oder
    ein einzelnes de) */
@Slf4j
public abstract class SyntaxElement {

    public final static boolean TRY_TO_CREATE = true;
    public final static boolean DONT_TRY_TO_CREATE = false;
    public final static boolean ALLOW_OVERWRITE = true;
    public final static boolean DONT_ALLOW_OVERWRITE = false;
    private List<MultipleSyntaxElements> childContainers = new ArrayList<>();
    /**
     * < @internal @brief alle in diesem element enthaltenen unterelemente
     */
    private String name;
    /**
     * < @internal @brief bezeichner fuer dieses element
     */
    private String type;
    private String path;
    /**
     * < @internal @brief nur beim parsen: zeichen, das vor diesem element stehen muesste
     */
    private boolean valid;
    /**
     * < @internal @brief indicates if this element is really valid, i.e. will appear in
     * an outgoing hbci message resp. in returned results from an incoming message
     */

    private MultipleSyntaxElements parent;
    // Wird von einigen Rewriter-Modules beim Parsen verwendet, um im Antwort-String
    // an der richtigen Stelle Daten auszuschneiden oder einzufügen.
    // TODO: Problem dabei ist nur: sobald auch nur *ein* Rewriter die Antwort-
    // Message verändert, stimmen von allen anderen SyntaxElementen die
    // Werte für posInMsg nicht mehr (es sei denn, es wird nach dem
    // Verändern ein neues MSG-Objekt erzeugt).
    private int posInMsg;
    private Document document;
    private Node def;
    /**
     * wird fuer datenelemente benoetigt, die sonst unbeabsichtigt generiert werden koennten.
     * das problem ist, dass es datenelemente (bisher nur bei segmenten bekannt) gibt,
     * die aus einigen "required" unterelementen bestehen und aus einigen optionalen
     * unterelementen. wenn *alle* "required" elemente bereits durch predefined values
     * bzw. durch automatisch generierte werte vorgegeben sind, dann wird das entsprechende
     * element erzeugt, da es auch ohne angabe der optionalen unterelemente gueltig ist.
     * <p>
     * es ist aber u.U. gar nicht beabsichtigt, dass dieses element erzeugt wird (beispiel
     * segment "KIOffer", wo nur die DEG SegHead required ist, alle anderen elemente sind
     * optional). es kann also vorkommen, dass ein element *unbeabsichtigt* nur aus den
     * vorgabedaten erzeugt wird.
     * <p>
     * bei den elementen, bei denen das passieren kann, wird in der xml-spezifikation
     * deshalb zusaetzlich das attribut "needsRequestTag" angegeben. der wert dieses
     * attributes wird hier in der variablen @p needsRequestTag gespeichert.
     * <p>
     * beim ueberpruefen, ob das aktuelle element gueltig ist (mittels @c validate() ),
     * wird neben der gueltigkeit aller unterelemente zusaetzlich ueberprueft, ob dieses
     * element ein request-tag benoetigt, und wenn ja, ob es vorhanden ist. wenn die
     * <p>
     * needsRequestTag -bedingung nicht erfuellt ist, ist auch das element ungueltig,
     * und es wird nicht erzeugt.
     * <p>
     * das vorhandensein eines request-tags wird in der variablen @haveRequestTag
     * gespeichert. dieses flag kann fuer ein bestimmtes element gesetzt werden, indem
     * ihm der wert "requested" zugewiesen wird. normalerweise kann nur DE-elementen
     * ein wert zugewiesen werden, diese benoetigen aber kein request-tag. wird also einem
     * gruppierenden element der wert "requested" zugewiesen, dann wird das durch die
     * methode @c propagateValue() als explizites setzen des @p haveRequestTag
     * interpretiert.
     * <p>
     * alle klassen und methoden, die also daten fuer die erzeugung von nachrichten
     * generieren, muessen u.U. fuer bestimmte syntaxelemente diesen "requested"-wert
     * setzen.
     * <p>
     * needsRequestTag kann komplett weg, oder? -- nein. Für GV-Segmente
     * gilt das schon. Die Ãberprüfung des requested-Werted findet aber
     * in der *allgemeinen* SyntaxElement-Klasse statt, wo auch andere
     * Segmente (z.b. MsgHead) erzeugt werden. Wenn als "allgemeiner"
     * Check der Check "if SEG.isRequested" eingeführt werden würde, dann
     * würde der nur bei tatsächlich gewünschten GV-Segmenten true ergeben.
     * Bei MsgHead-Segmenten z.B. würde er false ergeben (weil diese
     * Segmente niemals auf "requested" gesetzt werden). Deshalb darf diese
     * "requested"-Überprüfung nur bei den Syntaxelementen stattfinden,
     * bei denen das explizit gewünscht ist (needsRequestTag).
     */
    private boolean needsRequestTag;
    private boolean haveRequestTag;

    /**
     * es wird ein syntaxelement mit der id 'name' initialisiert; der pfad bis zu
     * diesem element wird in 'path' uebergeben; 'idx' ist die nummer dieses
     * elementes innerhalb der syntaxelementliste fuer dieses element (falls ein
     * bestimmtes syntaxelement mehr als einmal auftreten kann)
     */
    protected SyntaxElement(String type, String name, String path, int idx, Document document) {
        initData(type, name, path, idx, document);
    }

    // TODO: aus konsistenz-gründen auch in MultipleSyntaxElements create und
    // createAndAdd trennen

    /**
     * beim parsen: initialisiert ein neues syntaxelement mit der id 'name'; in
     * 'path' wird der pfad bis zu dieser stelle uebergeben 'predelim' gibt das
     * delimiter-zeichen an, das beim parsen vor diesem document- element stehen
     * muesste 'idx' ist die nummer des syntaxelementes innerhalb der
     * uebergeordneten liste (die liste repraesentiert das evtl. mehrmalige
     * auftreten eines syntaxelementes, siehe class syntaxelementlist) 'res' ist
     * der zu parsende String 'predefs' soll eine menge von pfad-wert-paaren
     * enthalten, die fuer einige syntaxelemente den wert angeben, den diese
     * elemente zwingend haben muessen (z.b. ein bestimmter segmentcode o.ae.)
     */
    protected SyntaxElement(String type, String name, String path, char predelim, int idx, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        initData(type, name, path, predelim, idx, res, document, predefs, valids);
    }

    /**
     * gibt einen string mit den typnamen (msg,seg,deg,de,...) des
     * elementes zurueck
     */
    protected abstract String getElementTypeName();

    /**
     * liefert das delimiter-zeichen zurueck, dass innerhalb dieses
     * syntaxelementes benutzt wird, um die einzelnen child-elemente voneinander
     * zu trennen
     */
    protected abstract char getInDelim();

    /**
     * erzeugt einen neuen Child-Container, welcher durch den
     * xml-knoten 'ref' identifiziert wird; wird beim erzeugen von elementen
     * benutzt
     */
    protected abstract MultipleSyntaxElements createNewChildContainer(Node ref, Document document);

    /**
     * beim parsen: haengt an die 'childElements' ein neues Element an. der
     * xml-knoten 'ref' gibt an, um welches element es sich dabei handelt; aus
     * 'res' (der zu parsende String) wird der wert fuer das element ermittelt
     * (falls es sich um ein de handelt); in 'predefined' ist der wert des
     * elementes zu finden, der laut syntaxdefinition ('document') an dieser stelle
     * auftauchen mueste (optional; z.b. fuer segmentcodes); 'predelim*' geben
     * die delimiter an, die direkt vor dem zu erzeugenden syntaxelement
     * auftauchen muessten
     */
    protected abstract MultipleSyntaxElements parseNewChildContainer(Node ref, char predelim0, char predelim1, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids);

    private void initData(String type, String name, String ppath, int idx, Document document) {
        if (getElementTypeName().equals("SEG"))
            log.trace("creating segment " + ppath + " -> " + name + "(" + idx + ")");

        this.type = type;
        this.name = name;
        this.document = document;

        /* der pfad wird gebildet aus bisherigem pfad
         plus name des elementes
         plus indexnummer, falls diese groesser 0 ist */
        StringBuilder temppath = new StringBuilder(128);
        if (ppath != null && ppath.length() != 0)
            temppath.append(ppath).append(".");
        temppath.append(HBCIUtils.withCounter(name, idx));
        this.path = temppath.toString();

        setValid(false);

        if (document != null) {
            this.def = getSyntaxDef(type, document);

            // erzeugen der child-elemente
            String requestTag = ((Element) def).getAttribute("needsRequestTag");
            if (requestTag != null && requestTag.equals("1"))
                needsRequestTag = true;

            int syntaxIdx = 0;
            for (Node ref = def.getFirstChild(); ref != null; ref = ref.getNextSibling()) {
                if (ref.getNodeType() == Node.ELEMENT_NODE) {
                    MultipleSyntaxElements child = createAndAppendNewChildContainer(ref, document);
                    if (child != null) {
                        child.setParent(this);
                        child.setSyntaxIdx(syntaxIdx);

                        if (getElementTypeName().equals("MSG"))
                            log.trace("child container " + child.getPath() + " has syntaxIdx=" + child.getSyntaxIdx());
                    }
                    syntaxIdx++;
                }
            }

                /* durchlaufen aller "value"-knoten und setzen der
                 werte der entsprechenden de */
            // TODO: effizienter: das nicht hier machen, sondern später,
            // Wenn wir das *hier* machen, dann werden ja DOCH wieder
            // alle "minnum=0"-Segmente
            // erzeugt, weil für jedes Segment code und version gesetzt
            // werden müssten. Am besten das immer in dem Moment machen,
            // wo ein entsprechendes SyntaxDE erzeugt wird.
            // --> nein, das geht hier. Grund: die optimierte Message-Engine
            // wird nur für Segmentfolgen angewendet. Und in Segmentfolgen-
            // Definitionen sind keine values oder valids angegeben, so dass
            // dieser Code hier gar keine Relevanz für Segmentfolgen hat
            NodeList valueNodes = ((Element) def).getElementsByTagName("value");
            int len = valueNodes.getLength();
            String dottedPath = this.path + ".";
            for (int i = 0; i < len; i++) {
                Node valueNode = valueNodes.item(i);
                String valuePath = ((Element) valueNode).getAttribute("path");
                String value = (valueNode.getFirstChild()).getNodeValue();
                String destpath = dottedPath + valuePath;

                if (!propagateValue(destpath, value, TRY_TO_CREATE, DONT_ALLOW_OVERWRITE))
                    throw new NoSuchPathException(destpath);
            }

            /* durchlaufen aller "valids"-knoten und speichern der valid-values */
            // TODO: das hier ebenfalls später machen, siehe "values"
            NodeList validNodes = ((Element) def).getElementsByTagName("valids");
            len = validNodes.getLength();
            dottedPath = getPath() + ".";
            for (int i = 0; i < len; i++) {
                Node validNode = validNodes.item(i);
                String valuePath = ((Element) (validNode)).getAttribute("path");
                String absPath = dottedPath + valuePath;

                NodeList validvalueNodes = ((Element) (validNode)).getElementsByTagName("validvalue");
                int len2 = validvalueNodes.getLength();
                for (int j = 0; j < len2; j++) {
                    Node validvalue = validvalueNodes.item(j);
                    String value = (validvalue.getFirstChild()).getNodeValue();

                    storeValidValueInDE(absPath, value);
                }
            }
        }
    }

    protected void init(String type, String name, String path, int idx, Document document) {
        initData(type, name, path, idx, document);
    }

    protected MultipleSyntaxElements createAndAppendNewChildContainer(Node ref, Document document) {
        MultipleSyntaxElements ret = createNewChildContainer(ref, document);
        if (ret != null)
            addChildContainer(ret);
        return ret;
    }

    protected boolean storeValidValueInDE(String destPath, String value) {
        boolean ret = false;

        for (Iterator<MultipleSyntaxElements> i = childContainers.listIterator(); i.hasNext(); ) {
            MultipleSyntaxElements l = i.next();
            if (l.storeValidValueInDE(destPath, value)) {
                ret = true;
                break;
            }
        }

        return ret;
    }

    // -------------------------------------------------------------------------------------------

    /**
     * loop through all child-elements; the segments found there
     * will be sequentially enumerated starting with num startValue;
     * if startValue is zero, the segments will not be enumerated,
     * but all given the number 0
     *
     * @param startValue value to be used for the first segment found
     * @return next sequence number usable for enumeration
     */
    public int enumerateSegs(int startValue, boolean allowOverwrite) {
        int idx = startValue;

        for (MultipleSyntaxElements s : getChildContainers()) {
            if (s != null)
                idx = s.enumerateSegs(idx, allowOverwrite);
        }

        return idx;
    }

    private void initData(String type, String name, String ppath, char predelim, int idx, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        this.type = type;
        this.name = name;
        this.parent = null;
        this.childContainers = new ArrayList<>();
        this.needsRequestTag = false;
        this.haveRequestTag = false;
        this.document = document;
        this.def = null;
        /* position des aktuellen datenelementes berechnet sich aus der
         * gesamtlänge des ursprünglichen msg-strings minus der länge des
         * reststrings, der jetzt zu parsen ist, und der mit dem aktuellen
         * datenelement beginnt */
        int fullResLen = res.length();
        this.posInMsg = fullResLen - res.length();

        StringBuilder temppath = new StringBuilder(128);
        if (ppath != null && ppath.length() != 0)
            temppath.append(ppath).append(".");
        temppath.append(HBCIUtils.withCounter(name, idx));
        this.path = temppath.toString();

        setValid(false);

        if (document != null) {
            this.def = getSyntaxDef(type, document);

            /* fuellen der 'predefs'-tabelle mit den in der
             syntaxbeschreibung vorgegebenen werten */
            NodeList valueNodes = ((Element) def).getElementsByTagName("value");
            String dottedPath = getPath() + ".";
            int len = valueNodes.getLength();
            for (int i = 0; i < len; i++) {
                Node valueNode = valueNodes.item(i);
                String valuePath = ((Element) valueNode).getAttribute("path");
                String value = (valueNode.getFirstChild()).getNodeValue();

                predefs.put(dottedPath + valuePath, value);
            }

            if (valids != null) {
                /* durchlaufen aller "valids"-knoten und speichern der valid-values */
                NodeList validNodes = ((Element) def).getElementsByTagName("valids");
                len = validNodes.getLength();
                for (int i = 0; i < len; i++) {
                    Node validNode = validNodes.item(i);
                    String valuePath = ((Element) (validNode)).getAttribute("path");
                    String absPath = dottedPath + valuePath;

                    NodeList validvalueNodes = ((Element) (validNode)).getElementsByTagName("validvalue");
                    int len2 = validvalueNodes.getLength();
                    for (int j = 0; j < len2; j++) {
                        Node validvalue = validvalueNodes.item(j);
                        String value = (validvalue.getFirstChild()).getNodeValue();
                        valids.put(HBCIUtils.withCounter(absPath + ".value", j), value);
                    }
                }
            }

            // anlegen der child-elemente
            int counter = 0;
            for (Node ref = def.getFirstChild(); ref != null; ref = ref.getNextSibling()) {
                if (ref.getNodeType() == Node.ELEMENT_NODE) {
                    MultipleSyntaxElements child = parseAndAppendNewChildContainer(ref,
                        ((counter++) == 0) ? predelim : getInDelim(),
                        getInDelim(),
                        res, document, predefs, valids);

                    if (child != null) {
                        child.setParent(this);

                        // TODO: this is a very very dirty hack to fix the problem with the params-template;
                        // bei der SF "Params", die mit <SF type="Params" maxnum="0"/> referenziert wird,
                        // soll nach jedem erfolgreich in die SF aufgenommenen Param-Segment eine neue
                        // SF begonnen werden, damit das Problem mit dem am Ende der SF stehenden Template-
                        // Param-Segment nicht mehr auftritt
                        // dazu wird beim hinzufuegen von segmenten zur sf ueberprueft, ob diese evtl. bereits
                        // segmente enthaelt (hasValidChilds()). falls das der fall ist, so wird
                        // kein neues segment hinzugefuegt
                        // analoges gilt für die SF "GVRes" - hier muss dafür gesorgt werden, dass jede
                        // antwort in ein eigenes GVRes kommt, damit die zuordnung reihenfolge-erkennung
                        // der empfangenen GVRes-segmente funktioniert (in HBCIJobImpl.fillJobResult())
                        if ((this instanceof SF) &&
                            (getName().equals("Params") || getName().equals("GVRes")) &&
                            ((MultipleSEGs) child).hasValidChilds()) {
                            break;
                        }
                    }
                }
            }
        }

        // if there was no error until here, this syntaxelement is valid
        setValid(true);
    }

    protected void init(String type, String name, String path, char predelim, int idx, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        initData(type, name, path, predelim, idx, res, document, predefs, valids);
    }

    protected MultipleSyntaxElements parseAndAppendNewChildContainer(Node ref, char predelim0, char predelim1, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        MultipleSyntaxElements ret = parseNewChildContainer(ref, predelim0, predelim1, res, document, predefs, valids);
        if (ret != null)
            addChildContainer(ret);
        return ret;
    }

    /**
     * fuellt die hashtable 'values' mit den werten der de-syntaxelemente; dazu
     * wird in allen anderen typen von syntaxelementen die liste der
     * child-elemente durchlaufen und deren 'fillValues' methode aufgerufen
     */
    public void extractValues(HashMap<String, String> values) {
        for (MultipleSyntaxElements l : childContainers) {
            l.extractValues(values);
        }
    }


    // -------------------------------------------------------------------------------------------

    private void addChildContainer(MultipleSyntaxElements x) {
        childContainers.add(x);
    }

    /**
     * @return the ArrayList containing all child-elements (the elements
     * of the ArrayList are instances of the SyntaxElementArray class
     */
    public List<MultipleSyntaxElements> getChildContainers() {
        return childContainers;
    }

    /**
     * setzt den wert eines de; in allen syntaxelementen ausser DE wird dazu die
     * liste der child-elemente durchlaufen; jedem dieser child-elemente wird der
     * wert zum setzen uebergeben; genau _eines_ dieser elemente wird sich dafuer
     * zustaendig fuehlen (das DE mit 'path'='destPath') und den wert uebernehmen
     */
    // TODO: code splitten
    public boolean propagateValue(String destPath, String value, boolean tryToCreate, boolean allowOverwrite) {
        boolean ret = false;

        if (destPath.equals(getPath())) {
            if (value != null && value.equals("requested"))
                this.haveRequestTag = true;
            else
                throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVVALUE", new Object[]{destPath, value}));
            ret = true;
        } else {
            // damit überspringen wir gleich elemente, bei denen es mit
            // sicherheit nicht funktionieren kann
            if (destPath.startsWith(getPath())) {
                for (MultipleSyntaxElements l : childContainers) {
                    if (l.propagateValue(destPath, value, tryToCreate, allowOverwrite)) {
                        ret = true;
                        break;
                    }
                }

                if (!ret && tryToCreate) {
                    // der Wert konnte nicht gesetzt werden -> möglicherweise
                    // existiert ja nur der entsprechende child-container noch
                    // nicht
                    log.trace(getPath() + ": could not set value for " + destPath);

                    // Namen des fehlenden Elementes ermitteln
                    String subPath = destPath.substring(getPath().length() + 1);
                    log.trace("  subpath is " + subPath);
                    int dotPos = subPath.indexOf('.');
                    if (dotPos == -1) {
                        dotPos = subPath.length();
                    }
                    String subType = subPath.substring(0, dotPos);
                    log.trace("  subname is " + subType);
                    int counterPos = subType.indexOf('_');
                    if (counterPos != -1) {
                        subType = subType.substring(0, counterPos);
                    }
                    log.trace("  subType is " + subType);

                    // hier überprüfen, ob es wirklich noch keinen child-container
                    // mit diesem Namen gibt. Wenn z.B. der pfad msg.gv.ueb.kik.blz
                    // gesucht wird und msg.gv schon existiert, wird diese methode
                    // hier in msg.gv ausgeführt. wenn sie fehlschlägt (z.b. weil
                    // tatsächlich kein .ueb.kik.blz angelegt werden kann), wird false
                    // ("can not propagate") zurückgegeben. im übergeordneten modul
                    // (msg) soll dann nicht versucht werden, das nächste sub-element
                    // (gv) anzulegen - dieser test merkt, dass es "gv" schon gibt
                    boolean found = false;
                    for (MultipleSyntaxElements c : childContainers) {
                        if (c.getName().equals(subType)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // jetzt durch alle child-elemente des definierenden XML-Knotens
                        // loopen und den ref-Knoten suchen, der das fehlende Element
                        // beschreibt
                        int newChildIdx = 0;
                        Node ref;
                        found = false;
                        for (ref = def.getFirstChild(); ref != null; ref = ref.getNextSibling()) {
                            if (ref.getNodeType() == Node.ELEMENT_NODE) {
                                String type = ((Element) ref).getAttribute("type");
                                String name = ((Element) ref).getAttribute("name");
                                if (name.length() == 0) {
                                    name = type;
                                }
                                if (name.equals(subType)) {
                                    found = true;
                                    break;
                                }
                                newChildIdx++;
                            }
                        }

                        if (found) {
                            // entsprechenden child-container erzeugen
                            MultipleSyntaxElements child = createNewChildContainer(ref, document);
                            child.setParent(this);
                            child.setSyntaxIdx(newChildIdx);

                            if (getElementTypeName().equals("MSG"))
                                log.trace("child container " + child.getPath() + " has syntaxIdx=" + child.getSyntaxIdx());

                            // aktuelle child-container-liste durchlaufen und den neu
                            // erzeugten child-container dort richtig einsortieren
                            int newPosi = 0;
                            for (MultipleSyntaxElements c : childContainers) {
                                if (c.getSyntaxIdx() > newChildIdx) {
                                    // der gerade betrachtete child-container hat einen idx
                                    // gröÃer als den des einzufügenden elementes, also wird
                                    // sich diese position gemerkt und das element hier eingefügt
                                    break;
                                }
                                newPosi++;
                            }
                            log.trace("  inserting child container with syntaxIdx " + newChildIdx + " at position " + newPosi);
                            childContainers.add(newPosi, child);

                            // now try to propagate the value to the newly created child
                            ret = child.propagateValue(destPath, value, tryToCreate, allowOverwrite);
                        }
                    } else {
                        log.trace("  subtype " + subType + " already existing - will not try to create");
                    }
                }
            }
        }

        return ret;
    }

    /**
     * @return den wert eines bestimmten DE;
     * funktioniert analog zu 'propagateValue'
     */
    public String getValueOfDE(String path) {
        String ret = null;

        for (MultipleSyntaxElements l : childContainers) {
            ret = l.getValueOfDE(path);
            if (ret != null) {
                break;
            }
        }

        return ret;
    }

    public String getValueOfDE(String path, int zero) {
        String ret = null;

        for (MultipleSyntaxElements l : childContainers) {
            ret = l.getValueOfDE(path, 0);
            if (ret != null) {
                break;
            }
        }

        return ret;
    }

    /**
     * @param path path to the element to be returned
     * @return the element identified by path
     */
    public SyntaxElement getElement(String path) {
        SyntaxElement ret = null;

        if (getPath().equals(path)) {
            ret = this;
        } else {
            for (MultipleSyntaxElements l : childContainers) {
                ret = l.getElement(path);
                if (ret != null) {
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * @return the path to this element
     */
    public final String getPath() {
        return path;
    }

    protected void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the name of this element (i.e. the last component of path)
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    protected void setType(String type) {
        this.type = type;
    }

    /**
     * @param type     the name of the syntaxelement to be returned
     * @param document the structure containing the current syntaxdefinition
     * @return a XML-node with the definition of the requested syntaxelement
     */
    public final Node getSyntaxDef(String type, Document document) {
        Node ret = document.getElementById(type);
        if (ret == null)
            throw new org.kapott.hbci.exceptions.NoSuchElementException(getElementTypeName(), type);
        return ret;
    }

    public boolean isValid() {
        return valid;
    }

    protected final void setValid(boolean valid) {
        this.valid = valid;
    }

    public int checkSegSeq(int value) {
        for (MultipleSyntaxElements a : childContainers) {
            value = a.checkSegSeq(value);
        }

        return value;
    }

    public String toString(int zero) {
        return toString();
    }

    /**
     * ueberpreuft, ob das syntaxelement alle restriktionen einhaelt; ist das
     * nicht der fall, so wird eine Exception ausgeloest. die meisten
     * syntaxelemente koennen sich nicht selbst ueberpruefen, sondern rufen statt
     * dessen die validate-funktion der child-elemente auf
     */
    public void validate() {
        if (!needsRequestTag || haveRequestTag) {
            for (MultipleSyntaxElements l : childContainers) {
                l.validate();
            }

            /* wenn keine exception geworfen wurde, dann ist das aktuelle element
               offensichtlich valid */
            setValid(true);
        }
    }

    public void getElementPaths(HashMap<String, String> p, int[] segref, int[] degref, int[] deref) {
    }

    public MultipleSyntaxElements getParent() {
        return parent;
    }

    public void setParent(MultipleSyntaxElements parent) {
        this.parent = parent;
    }

    public int getPosInMsg() {
        return posInMsg;
    }
}

