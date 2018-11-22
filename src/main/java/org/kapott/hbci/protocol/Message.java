/*  $Id: MSG.java,v 1.1 2011/05/04 22:38:03 willuhn Exp $

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
import org.kapott.hbci.exceptions.NoSuchPathException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;

@Slf4j
public final class Message extends SyntaxElement {

    public final static boolean CHECK_SEQ = true;
    public final static boolean DONT_CHECK_SEQ = false;
    public final static boolean DONT_CHECK_VALIDS = false;

    private Document document;
    private HashMap<String, String> clientValues = new HashMap<>();

    public Message(String type, Document document) {
        super(type, type, null, 0, document);
    }

    public Message(String type, String res, Document document, boolean checkSeq, boolean checkValids) {
        super(type, type, null, (char) 0, 0, new StringBuffer(res),
            document,
            new HashMap<>(),
            checkValids ? new HashMap<>() : null);
        if (checkSeq)
            checkSegSeq(1);
    }

    public void init(String type, Document document) {
        super.init(type, type, null, 0, document);
    }

    protected MultipleSyntaxElements createNewChildContainer(Node ref, Document document) {
        this.document = document;

        MultipleSyntaxElements ret = null;

        if ((ref.getNodeName()).equals("SEG"))
            ret = new MultipleSEGs(ref, getPath(), document);
        else if ((ref.getNodeName()).equals("SF"))
            ret = new MultipleSFs(ref, getPath(), document);

        return ret;
    }

    protected String getElementTypeName() {
        return "MSG";
    }

    /**
     * in 'clientValues' wird eine hashtable uebergeben, die als schluessel den
     * pfadnames und als wert den wert eines zu setzenden elementes enthaelt. mit
     * der methode werden vom nutzer einzugebenede daten (wie kontonummern, namen
     * usw.) in die generierte nachricht eingebaut
     */
    private void propagateUserData(HashMap<String, String> clientValues) {
        String dottedName = getName() + ".";
        clientValues.forEach((key, value) -> {
            if (key.startsWith(dottedName) && value != null && value.length() != 0) {
                if (!propagateValue(key, value, TRY_TO_CREATE, DONT_ALLOW_OVERWRITE)) {
                    log.warn("could not insert the following user-defined data into message: " + key + "=" + value);
                }
            }
        });
    }

    /**
     * setzen des feldes "nachrichtengroesse" im nachrichtenkopf einer nachricht
     */
    private void setMsgSizeValue(int value, boolean allowOverwrite) {
        String absPath = getPath() + ".MsgHead.msgsize";
        SyntaxElement msgsizeElem = getElement(absPath);

        if (msgsizeElem == null)
            throw new NoSuchPathException(absPath);

        int size = ((DE) msgsizeElem).getMinSize();
        char[] zeros = new char[size];
        Arrays.fill(zeros, '0');
        DecimalFormat df = new DecimalFormat(String.valueOf(zeros));
        if (!propagateValue(absPath, df.format(value), DONT_TRY_TO_CREATE, allowOverwrite))
            throw new NoSuchPathException(absPath);
    }

    private void initMsgSize() {
        setMsgSizeValue(0, DONT_ALLOW_OVERWRITE);
    }

    public void autoSetMsgSize() {
        setMsgSizeValue(toString(0).length(), ALLOW_OVERWRITE);
    }

    public void complete() {
        propagateUserData(clientValues);

        enumerateSegs(0, DONT_ALLOW_OVERWRITE);
        initMsgSize();
        validate();
        enumerateSegs(1, ALLOW_OVERWRITE);
        autoSetMsgSize();
    }

    public String toString(int dummy) {
        StringBuilder ret = new StringBuilder(1024);

        if (isValid())
            for (MultipleSyntaxElements list : getChildContainers()) {
                if (list != null)
                    ret.append(list.toString(0));
            }

        return ret.toString();
    }

    // -------------------------------------------------------------------------------------------

    public void init(String type, String res, Document document, boolean checkSeq, boolean checkValids) {
        super.init(type, type, null, (char) 0, 0, new StringBuffer(res),
            document, new HashMap<>(),
            checkValids ? new HashMap<>() : null);
        if (checkSeq)
            checkSegSeq(1);
    }

    protected char getInDelim() {
        return '\'';
    }

    protected MultipleSyntaxElements parseNewChildContainer(Node segref, char predelim0, char predelim1, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        MultipleSyntaxElements ret = null;

        if ((segref.getNodeName()).equals("SEG"))
            ret = new MultipleSEGs(segref, getPath(), predelim0, predelim1, res, document, predefs, valids);
        else if ((segref.getNodeName()).equals("SF"))
            ret = new MultipleSFs(segref, getPath(), predelim0, predelim1, res, document, predefs, valids);

        return ret;
    }

    public String getValueOfDE(String path) {
        String ret = null;

        for (MultipleSyntaxElements l : getChildContainers()) {
            String temp = l.getValueOfDE(path);
            if (temp != null) {
                ret = temp;
                break;
            }
        }

        if (ret == null)
            throw new NoSuchPathException(path);

        return ret;
    }

    // -------------------------------------------------------------------------------------------

    public HashMap<String, String> getData() {
        HashMap<String, String> hash = new HashMap<>();
        HashMap p = new HashMap<String, String>();
        int nameskip = getName().length() + 1;

        extractValues(hash);

        hash.forEach((key, value) -> {
            p.put(key.substring(nameskip), value);
        });

        return p;
    }

    public void getElementPaths(HashMap<String, String> p, int[] segref, int[] degref, int[] deref) {
        segref = new int[1];
        segref[0] = 1;

        for (MultipleSyntaxElements l : getChildContainers()) {
            if (l != null) {
                l.getElementPaths(p, segref, null, null);
            }
        }
    }

    public boolean isCrypted() {
        MultipleSyntaxElements seglist = getChildContainers().get(1);
        if (seglist instanceof MultipleSEGs) {
            try {
                SEG crypthead = (SEG) (seglist.getElements().get(0));
                if (crypthead.getCode().equals("HNVSK"))
                    return true;
            } catch (Exception e) {
            }
        }
        return false;
    }


    public Document getDocument() {
        return document;
    }

    /**
     * @param path  The path to the document element for which the value is to be set. For
     *              more information about paths, see
     *              SyntaxElement::SyntaxElement()
     * @param value The new value for the specified element.
     *              Sets a certain property that is later used in message generation.
     */
    public void set(String path, String value) {
        clientValues.put(path, value);
    }

    public void rawSet(String path, String value) {
        set(getName() + "." + path, value);
    }

    public String get(String key) {
        return clientValues.get(key);
    }
}
