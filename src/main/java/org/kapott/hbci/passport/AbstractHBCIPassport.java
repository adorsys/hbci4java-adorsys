/*  $Id: AbstractHBCIPassport.java,v 1.4 2012/03/13 22:07:43 willuhn Exp $

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

package org.kapott.hbci.passport;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidArgumentException;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.DocumentFactory;
import org.kapott.hbci.manager.HBCIProduct;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Limit;
import org.kapott.hbci.structures.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * <p>Diese Klasse stellt die Basisklasse für alle "echten" Passport-Implementationen
 * dar. Hier werden bereits einige Methoden implementiert sowie einige
 * zusätzliche Hilfsmethoden zur Verfügung gestellt.</p><p>
 * Aus einer HBCI-Anwendung heraus ist hier nur eine einzige Methode interessant,
 * um eine Instanz eines bestimmtes Passports zu erzeugen</p>
 */
@Slf4j
public abstract class AbstractHBCIPassport implements HBCIPassportInternal, Serializable {

    protected HBCICallback callback;
    protected Map<String, String> properties;
    private Map<String, String> bpd;
    private Map<String, String> upd;
    private String hbciversion;
    private String country;
    private String blz;
    private String host;
    private Integer port;
    private String userid;
    private String customerid;
    private String sysid;
    private Long sigid;
    private Document syntaxDocument;
    private HBCIProduct hbciProduct;

    public AbstractHBCIPassport(String hbciversion, Map<String, String> properties, HBCICallback callback, HBCIProduct product) {
        this.hbciversion = hbciversion;
        this.callback = callback;
        this.properties = properties;
        this.hbciProduct = product;

        init();
    }

    public static HBCIPassport getInstance(HBCICallback callback, Map<String, String> properties, String name, Object init) {
        if (name == null) {
            throw new NullPointerException("name of passport implementation must not be null");
        }

        String className = "org.kapott.hbci.passport.HBCIPassport" + name;
        try {
            if (init == null)
                init = name;

            log.debug("creating new instance of a " + name + " passport");
            Class cl = Class.forName(className);
            Constructor con = cl.getConstructor(new Class[]{Properties.class, HBCICallback.class, Object.class});
            HBCIPassport p = (HBCIPassport) (con.newInstance(new Object[]{properties, callback, init}));
            return p;
        } catch (ClassNotFoundException e) {
            throw new InvalidUserDataException("*** No passport implementation '" + name + "' found - there must be a class " + className);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof HBCI_Exception)
                throw (HBCI_Exception) cause;

            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_PASSPORT_INST", name), ite);
        } catch (HBCI_Exception he) {
            throw he;
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_PASSPORT_INST", name), e);
        }
    }

    /**
     * Erzeugt eine Instanz eines HBCI-Passports. Der Typ der erzeugten
     * Passport-Instanz wird hierbei dem Wert des HBCI-Parameters
     * <code>client.passport.default</code> entnommen. Gültige Werte für diesen
     * HBCI-Parameter sind die gleichen wie beim Aufruf der Methode
     *
     * @return Instanz eines HBCI-Passports
     */
    public static HBCIPassport getInstance(HBCICallback callback, Map<String, String> properties, Object init) {
        String passportName = properties.get("client.passport.default");
        if (passportName == null)
            throw new InvalidUserDataException(HBCIUtils.getLocMsg("EXCMSG_NODEFPASS"));

        return getInstance(callback, properties, passportName, init);
    }

    public static HBCIPassport getInstance(HBCICallback callback, Map<String, String> properties, String name) {
        return getInstance(callback, properties, name, null);
    }

    public static HBCIPassport getInstance(HBCICallback callback, Map<String, String> properties) {
        return getInstance(callback, properties, (Object) null);
    }

    private static String pathWithDot(String path) {
        return (path.length() == 0) ? path : (path + ".");
    }

    /* Initialisieren eines Message-Generators. Der <syntaFileStream> ist ein
     * Stream, mit dem eine XML-Datei mit einer HBCI-Syntaxspezifikation
     * eingelesen wird */
    private void init() {
        this.syntaxDocument = DocumentFactory.createDocument(hbciversion);

        setCountry(properties.get("client.passport.country"));
        setBLZ(properties.get("client.passport.blz"));
        setCustomerId(properties.get("client.passport.customerId"));
        if (properties.get("client.passport.userId") != null) {
            setUserId(properties.get("client.passport.userId"));
        } else {
            setUserId(getCustomerId());
        }
    }

    public final List<Konto> getAccounts() {
        ArrayList<Konto> ret = new ArrayList<>();

        if (upd != null) {
            for (int i = 0; ; i++) {
                String header = HBCIUtils.withCounter("KInfo", i);
                String number = upd.get(header + ".KTV.number");

                if (number == null)
                    break;

                Konto entry = new Konto();
                entry.blz = upd.get(header + ".KTV.KIK.blz");
                entry.country = upd.get(header + ".KTV.KIK.country");
                entry.number = number;
                entry.subnumber = upd.get(header + ".KTV.subnumber");
                entry.curr = upd.get(header + ".cur");
                entry.type = upd.get(header + ".konto");
                entry.customerid = upd.get(header + ".customerid");
                entry.name = upd.get(header + ".name1");
                entry.name2 = upd.get(header + ".name2");
                entry.bic = upd.get(header + ".KTV.bic");
                entry.iban = upd.get(header + ".KTV.iban");
                entry.acctype = upd.get(header + ".acctype");

                String st;
                if ((st = upd.get(header + ".KLimit.limittype")) != null) {
                    Limit limit = new Limit();
                    limit.type = st.charAt(0);
                    limit.value = new Value(upd.get(header + ".KLimit.BTG.value"),
                        upd.get(header + ".KLimit.BTG.curr"));
                    if ((st = upd.get(header + ".KLimit.limitdays")) != null)
                        limit.days = Integer.parseInt(st);
                }

                // allowedGVs
                ArrayList<String> codes = new ArrayList<>();
                for (int j = 0; ; j++) {
                    String gvHeader = HBCIUtils.withCounter(header + ".AllowedGV", j);
                    String code = upd.get(gvHeader + ".code");
                    if (code == null) break;
                    codes.add(code);
                }
                if (!codes.isEmpty()) entry.allowedGVs = codes;

                ret.add(entry);
            }
        }

        return ret;
    }

    public final void fillAccountInfo(Konto account) {
        String number = HBCIUtils.stripLeadingZeroes(account.number);
        String iban = HBCIUtils.stripLeadingZeroes(account.iban);
        boolean haveNumber = (number != null && number.length() != 0);
        boolean haveIBAN = (iban != null && iban.length() != 0);

        for (Konto account1 :  getAccounts()) {
            String temp_number = HBCIUtils.stripLeadingZeroes(account1.number);
            String temp_iban = HBCIUtils.stripLeadingZeroes(account1.iban);

            if (haveNumber && number.equals(temp_number) ||
                haveIBAN && iban.equals(temp_iban)) {
                account.blz = account1.blz;
                account.country = account1.country;
                account.number = account1.number;
                account.subnumber = account1.subnumber;
                account.type = account1.type;
                account.curr = account1.curr;
                account.customerid = account1.customerid;
                account.name = account1.name;
                account.bic = account1.bic;
                account.iban = account1.iban;
                account.acctype = account1.acctype;
                break;
            }
        }
    }

    public final Konto findAccountByIban(String iban) {
        return getAccounts().stream()
            .filter(konto -> konto.iban.equals(iban))
            .findFirst()
            .orElse(null);
    }

    public final Konto findAccountByAccountNumber(String number) {
        Konto ret = new Konto();
        ret.number = number;
        fillAccountInfo(ret);

        if (ret.blz == null) {
            // es wurde kein Konto-Objekt in getAccounts() gefunden
            ret.blz = getBLZ();
            ret.country = getCountry();
            ret.customerid = getCustomerId();
            // TODO: very dirty!
            ret.name = getCustomerId();

            // an dieser Stelle sind jetzt alle Werte gefüllt, die teilweise
            // zwingend benötigt werden
        }

        return ret;
    }

    public String getHost() {
        return host;
    }

    public final void setHost(String host) {
        this.host = host;
    }

    public final Integer getPort() {
        return (port != null) ? port : new Integer(0);
    }

    public final void setPort(Integer port) {
        this.port = port;
    }

    public String getUserId() {
        return userid;
    }

    public final void setUserId(String userid) {
        this.userid = userid;
    }

    public String getCustomerId() {
        return (customerid != null && customerid.length() != 0) ? customerid : getUserId();
    }

    public final void setCustomerId(String customerid) {
        this.customerid = customerid;
    }

    public String getSysId() {
        return (sysid != null && sysid.length() != 0) ? sysid : "0";
    }

    public final void setSysId(String sysid) {
        this.sysid = sysid;
    }

    public final String getBPDVersion() {
        String version = ((bpd != null) ? bpd.get("BPA.version") : null);
        return ((version != null) ? version : "0");
    }

    public final String getUPDVersion() {
        String version = ((upd != null) ? upd.get("UPA.version") : null);
        return ((version != null) ? version : "0");
    }

    public final String getInstName() {
        return (bpd != null) ? bpd.get("BPA.kiname") : null;
    }

    public int getMaxGVperMsg() {
        return (bpd != null) ? Integer.parseInt(bpd.get("BPA.numgva")) : -1;
    }

    public final int getMaxMsgSizeKB() {
        if (bpd != null && bpd.get("BPA.maxmsgsize") != null) {
            return Integer.parseInt(bpd.get("BPA.maxmsgsize"));
        }
        return 0;
    }

    public final String[] getSuppVersions() {
        String[] ret = new String[0];

        if (bpd != null) {
            ArrayList<String> temp = new ArrayList<>();
            String header;
            String value;
            int i = 0;

            while ((header = HBCIUtils.withCounter("BPA.SuppVersions.version", i)) != null &&
                (value = bpd.get(header)) != null) {
                temp.add(value);
                i++;
            }

            if (temp.size() != 0)
                ret = (temp.toArray(ret));
        }

        return ret;
    }

    public final String getDefaultLang() {
        String value = (bpd != null) ? bpd.get("CommListRes.deflang") : null;
        return (value != null) ? value : "0";
    }

    public final String getLang() {
        String value = (bpd != null) ? bpd.get("CommListRes.deflang") : null;
        return (value != null) ? value : "0";
    }

    public final Long getSigId() {
        return sigid != null ? sigid : new Long(1);
    }

    public final void setSigId(Long sigid) {
        this.sigid = sigid;
    }

    public void incSigId() {
        setSigId(getSigId() + 1);
    }

    public Map<String, String> getParamSegmentNames() {
        Map<String, String> ret = new HashMap<>();

        bpd.keySet().forEach(key -> {
            if (key.startsWith("Params") && key.endsWith(".SegHead.code")) {
                int dotPos = key.indexOf('.');
                int dotPos2 = key.indexOf('.', dotPos + 1);

                String gvname = key.substring(dotPos + 1, dotPos2);
                int len = gvname.length();
                int versionPos = -1;

                for (int i = len - 1; i >= 0; i--) {
                    char ch = gvname.charAt(i);
                    if (!(ch >= '0' && ch <= '9')) {
                        versionPos = i + 1;
                        break;
                    }
                }

                String version = gvname.substring(versionPos);
                if (version.length() != 0) {
                    gvname = gvname.substring(0, versionPos - 3); // remove version and "Par"

                    String knownVersion = (String) ret.get(gvname);

                    if (knownVersion == null ||
                        Integer.parseInt(version) > Integer.parseInt(knownVersion)) {
                        ret.put(gvname, version);
                    }
                }
            }
        });

        return ret;
    }

    public Map<String, String> getJobRestrictions(String specname) {
        int versionPos = specname.length() - 1;
        char ch;

        while ((ch = specname.charAt(versionPos)) >= '0' && ch <= '9') {
            versionPos--;
        }

        return getJobRestrictions(
            specname.substring(0, versionPos + 1),
            specname.substring(versionPos + 1));
    }

    public Map<String, String> getJobRestrictions(String gvname, String version) {
        Map<String, String> result = new HashMap<>();

        String searchstring = gvname + "Par" + version;
        bpd.keySet().forEach(key -> {
            if (key.startsWith("Params") &&
                key.indexOf("." + searchstring + ".Par") != -1) {
                int searchIdx = key.indexOf(searchstring);
                result.put(key.substring(key.indexOf(".",
                    searchIdx + searchstring.length() + 4) + 1),
                    bpd.get(key));
            }
        });

        return result;
    }

    /**
     * <p>Gibt eine Liste mit Strings zurück, welche Bezeichnungen für die einzelnen Rückgabedaten
     * eines Lowlevel-Jobs darstellen. Jedem {@link org.kapott.hbci.GV.AbstractHBCIJob} ist ein
     * Result-Objekt zugeordnet, welches die Rückgabedaten und Statusinformationen zu dem jeweiligen
     * Job enthält (kann mit {@link org.kapott.hbci.GV.AbstractHBCIJob#getJobResult()}
     * ermittelt werden). Bei den meisten Highlevel-Jobs handelt es sich dabei um bereits aufbereitete
     * Daten (Kontoauszüge werden z.B. nicht in dem ursprünglichen SWIFT-Format zurückgegeben, sondern
     * bereits als fertig geparste Buchungseinträge).</p>
     * <p>Bei Lowlevel-Jobs gibt es diese Aufbereitung der Daten nicht. Statt dessen müssen die Daten
     * manuell aus der Antwortnachricht extrahiert und interpretiert werden. Die einzelnen Datenelemente
     * der Antwortnachricht werden in einem Properties-Objekt bereitgestellt. Jeder Eintrag
     * darin enthält den Namen und den Wert eines Datenelementes aus der Antwortnachricht.</p>
     * <p>Die Methode <code>getLowlevelJobResultNames()</code> gibt nun alle gültigen Namen zurück,
     * für welche in dem Result-Objekt Daten gespeichert sein können. Ob für ein Datenelement tatsächlich
     * ein Wert in dem Result-Objekt existiert, wird damit nicht bestimmt, da einzelne Datenelemente
     * optional sind.</p>
     * <p>Mit dem Tool {@link org.kapott.hbci.tools.ShowLowlevelGVRs} kann offline eine
     * Liste aller Job-Result-Datenelemente erzeugt werden.</p>
     * <p>Zur Beschreibung von High- und Lowlevel-Jobs siehe auch die Dokumentation
     * im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @param gvname Lowlevelname des Geschäftsvorfalls, für den die Namen der Rückgabedaten benötigt werden.
     * @return Liste aller möglichen Property-Keys, für die im Result-Objekt eines Lowlevel-Jobs
     * Werte vorhanden sein könnten
     */
    public List<String> getLowlevelJobResultNames(String gvname) {
        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getSupportedLowlevelJobs().get(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return getGVResultNames(syntaxDocument, gvname, version);
    }

    /**
     * <p>Gibt die Namen aller vom aktuellen HBCI-Zugang (d.h. Passport)
     * unterstützten Lowlevel-Jobs zurück.</p>
     * <p>In dem zurückgegebenen Properties-Objekt enthält jeder Eintrag als
     * Key den Lowlevel-Job-Namen; als Value wird die Versionsnummer des
     * jeweiligen Geschäftsvorfalls angegeben, die von <em>HBCI4Java</em> mit dem
     * aktuellen Passport und der aktuell eingestellten HBCI-Version
     * benutzt werden wird.</p>
     * <p><em>(Prinzipiell unterstützt <em>HBCI4Java</em> für jeden
     * Geschäftsvorfall mehrere GV-Versionen. Auch eine Bank bietet i.d.R. für
     * jeden GV mehrere Versionen an. Wird mit <em>HBCI4Java</em> ein HBCI-Job
     * erzeugt, so verwendet <em>HBCI4Java</em> immer automatisch die höchste
     * von der Bank unterstützte GV-Versionsnummer. Diese Information ist
     * für den Anwendungsentwickler kaum von Bedeutung und dient hauptsächlich
     * zu Debugging-Zwecken.)</em></p>
     * <p>Zum Unterschied zwischen High- und Lowlevel-Jobs siehe die
     * Beschreibung im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @return Sammlung aller vom aktuellen Passport unterstützten HBCI-
     * Geschäftsvorfallnamen (Lowlevel) mit der jeweils von <em>HBCI4Java</em>
     * verwendeten GV-Versionsnummer.
     */
    public Map<String, String> getSupportedLowlevelJobs() {
        Map<String, String> paramSegments = getParamSegmentNames();
        Map<String, String> result = new HashMap<>();

        paramSegments.keySet().forEach(segName -> {
            // überprüfen, ob parameter-segment tatsächlich zu einem GV gehört
            // gilt z.b. für "PinTan" nicht
            if (getLowlevelGVs(syntaxDocument).containsKey(segName))
                result.put(segName, paramSegments.get(segName));
        });

        return result;
    }

    public boolean jobSupported(String jobName) {
        return getSupportedLowlevelJobs().containsKey(jobName);
    }

    /**
     * @param type the name of the syntaxelement to be returned
     * @return a XML-node with the definition of the requested syntaxelement
     */
    public Node getSyntaxDef(String type) {
        Node ret = syntaxDocument.getElementById(type);
        if (ret == null)
            throw new org.kapott.hbci.exceptions.NoSuchElementException("element", type);
        return ret;
    }

    /**
     * <p>Gibt für einen Job alle bekannten Einschränkungen zurück, die bei
     * der Ausführung des jeweiligen Jobs zu beachten sind. Diese Daten werden aus den
     * Bankparameterdaten des aktuellen Passports extrahiert. Sie können von einer HBCI-Anwendung
     * benutzt werden, um gleich entsprechende Restriktionen bei der Eingabe von
     * Geschäftsvorfalldaten zu erzwingen (z.B. die maximale Anzahl von Verwendungszweckzeilen,
     * ob das Ändern von terminierten Überweisungen erlaubt ist usw.).</p>
     * <p>Die einzelnen Einträge des zurückgegebenen Properties-Objektes enthalten als Key die
     * Bezeichnung einer Restriktion (z.B. "<code>maxusage</code>"), als Value wird der
     * entsprechende Wert eingestellt. Die Bedeutung der einzelnen Restriktionen ist zur Zeit
     * nur der HBCI-Spezifikation zu entnehmen. In späteren Programmversionen werden entsprechende
     * Dokumentationen zur internen HBCI-Beschreibung hinzugefügt, so dass dafür eine Abfrageschnittstelle
     * implementiert werden kann.</p>
     * <p>I.d.R. werden mehrere Versionen eines Geschäftsvorfalles von der Bank
     * angeboten. Diese Methode ermittelt automatisch die "richtige" Versionsnummer
     * für die Ermittlung der GV-Restriktionen aus den BPD (und zwar die selbe,
     * die <em>HBCI4Java</em> beim Erzeugen eines Jobs benutzt). </p>
     * <p>Siehe dazu auch {@link AbstractHBCIJob#getJobRestrictions()}.</p>
     *
     * @param gvname Lowlevel-Name des Geschäftsvorfalles, für den die Restriktionen
     *               ermittelt werden sollen
     * @return Properties-Objekt mit den einzelnen Restriktionen
     */
    public Map<String, String> getLowlevelJobRestrictions(String gvname) {
        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getSupportedLowlevelJobs().get(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return getJobRestrictions(gvname, version);
    }

    private Hashtable<String, List<String>> getLowlevelGVs(Document document) {
        Hashtable<String, List<String>> result = new Hashtable<>();

        Element gvlist = document.getElementById("GV");
        NodeList gvs = gvlist.getChildNodes();
        int len = gvs.getLength();
        StringBuilder type = new StringBuilder();

        for (int i = 0; i < len; i++) {
            Node gvref = gvs.item(i);
            if (gvref.getNodeType() == Node.ELEMENT_NODE) {
                type.setLength(0);
                type.append(((Element) gvref).getAttribute("type"));

                int pos = type.length() - 1;
                char ch;

                while ((ch = type.charAt(pos)) >= '0' && ch <= '9') {
                    pos--;
                }

                String gvname = type.substring(0, pos + 1);
                List<String> entry = result.computeIfAbsent(gvname, k -> new ArrayList<>());

                entry.add(type.substring(pos + 1));
            }
        }

        return result;
    }

    /* gibt für einen hbci-gv ("saldo3") die liste aller ll-job-parameter
     * zurück */
    public List<String> getGVParameterNames(Document document, String gvname, String version) {
        ArrayList<String> ret = new ArrayList<>();
        Element gvdef = document.getElementById(gvname + version);
        NodeList gvcontent = gvdef.getChildNodes();
        int len = gvcontent.getLength();

        boolean first = true;
        for (int i = 0; i < len; i++) {
            Node contentref = gvcontent.item(i);

            if (contentref.getNodeType() == Node.ELEMENT_NODE) {
                // skip seghead
                if (first) {
                    first = false;
                } else {
                    addLowlevelProperties(document, ret, "", (Element) contentref);
                }
            }
        }

        return ret;
    }

    /* gibt für einen hbci-gv ("saldo3") die liste aller ll-job-result-parameter
     * zurück */
    private List<String> getGVResultNames(Document document, String gvname, String version) {
        ArrayList<String> ret = new ArrayList<>();
        Element gvdef = document.getElementById(gvname + "Res" + version);

        if (gvdef != null) {
            NodeList gvcontent = gvdef.getChildNodes();
            int len = gvcontent.getLength();

            boolean first = true;
            for (int i = 0; i < len; i++) {
                Node contentref = gvcontent.item(i);

                if (contentref.getNodeType() == Node.ELEMENT_NODE) {
                    if (first) {
                        first = false;
                    } else {
                        addLowlevelProperties(document, ret, "", (Element) contentref);
                    }
                }
            }
        }

        return ret;
    }

    /* gibt für einen hbci-gv ("saldo3") die liste aller ll-job-restriction-
     * parameter zurück */
    public List<String> getGVRestrictionNames(Document document, String gvname, String version) {
        ArrayList<String> ret = new ArrayList<>();

        // SEGdef id="TermUebPar1" finden
        Element gvdef = document.getElementById(gvname + "Par" + version);

        if (gvdef != null) {
            // alle darin enthaltenen elemente durchlaufen, bis ein element
            // DEG type="ParTermUeb1" gefunden ist
            NodeList gvcontent = gvdef.getChildNodes();
            int len = gvcontent.getLength();

            for (int i = 0; i < len; i++) {
                Node contentref = gvcontent.item(i);

                if (contentref.getNodeType() == Node.ELEMENT_NODE) {
                    String type = ((Element) contentref).getAttribute("type");
                    if (type.startsWith("Par")) {
                        // wenn ein DEG type="ParTermUeb" gefunden ist, können
                        // alle umgebenenden schleifenvariablen wiederverwendet
                        // werden, weil es nur *ein* solches element geben kann
                        // und die umgebende schleife demzufolge abgebrochen werden
                        // kann, nachdem das gefundenen element bearbeitet wurde

                        // DEGdef id="ParTermUeb1" finden
                        gvdef = document.getElementById(type);
                        gvcontent = gvdef.getChildNodes();
                        len = gvcontent.getLength();

                        // darin alle elemente durchlaufen und deren namen
                        // zur ergebnisliste hinzufügen
                        for (i = 0; i < len; i++) {
                            contentref = gvcontent.item(i);

                            if (contentref.getNodeType() == Node.ELEMENT_NODE) {
                                addLowlevelProperties(document, ret, "", (Element) contentref);
                            }
                        }
                        break;
                    }
                }
            }
        }

        return ret;
    }

    private void addLowlevelProperties(Document document, ArrayList<String> result, String path, Element ref) {
        if (ref.getAttribute("type").length() != 0) {
            if (ref.getNodeName().equals("DE")) {
                String name = ref.getAttribute("name");
                result.add(pathWithDot(path) + name);
            } else {
                String name = ref.getAttribute("name");
                if (name.length() == 0)
                    name = ref.getAttribute("type");

                Element def = document.getElementById(ref.getAttribute("type"));
                NodeList defcontent = def.getChildNodes();
                int len = defcontent.getLength();

                for (int i = 0; i < len; i++) {
                    Node content = defcontent.item(i);
                    if (content.getNodeType() == Node.ELEMENT_NODE)
                        addLowlevelProperties(document, result, pathWithDot(path) + name, (Element) content);
                }
            }
        }
    }

    public String getOrderHashMode(int segVersion) {
        return getBPD().keySet().stream()
            .filter(key -> {
                // p.getProperty("Params_x.TAN2StepParY.ParTAN2StepZ.can1step")
                if (key.startsWith("Params")) {
                    String subkey = key.substring(key.indexOf('.') + 1);
                    if (subkey.startsWith("TAN2StepPar" + segVersion) && subkey.endsWith(".orderhashmode")) {
                        return true;
                    }
                }
                return false;
            })
            .findFirst()
            .map(s -> getBPD().get(s))
            .orElse("");

    }

    public HBCIProduct getHbciProduct() {
        return hbciProduct;
    }

    public Document getSyntaxDocument() {
        return syntaxDocument;
    }

    public final Map<String, String> getBPD() {
        return bpd;
    }

    public void setBPD(Map<String, String> bpd) {
        this.bpd = bpd;
    }

    public final String getHBCIVersion() {
        return (hbciversion != null) ? hbciversion : "";
    }

    public final Map<String, String> getUPD() {
        return upd;
    }

    public final void setUPD(Map<String, String> upd) {
        this.upd = upd;
    }

    public final String getBLZ() {
        return blz;
    }

    public final void setBLZ(String blz) {
        this.blz = blz;
    }

    public final String getCountry() {
        return country;
    }

    public final void setCountry(String country) {
        this.country = country;
    }

    public int getMaxGVSegsPerMsg() {
        return 0;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public HBCICallback getCallback() {
        return callback;
    }


}
