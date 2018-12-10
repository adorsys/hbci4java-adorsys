/*  $Id: SEG.java,v 1.1 2011/05/04 22:38:03 willuhn Exp $

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

import org.kapott.hbci.exceptions.InvalidSegSeqException;
import org.kapott.hbci.exceptions.NoSuchPathException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Optional;

public final class SEG extends SyntaxElement {

    public SEG(String type, String name, String path, int idx, Document document) {
        super(type, name, path, idx, document);
    }

    public SEG(String type, String name, String path, char predelim, int idx, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        super(type, name, path, predelim, idx, res, document, predefs, valids);
    }

    protected String getElementTypeName() {
        return "SEG";
    }

    protected MultipleSyntaxElements createNewChildContainer(Node ref, Document document) {
        MultipleSyntaxElements ret = null;

        if ((ref.getNodeName()).equals("DE"))
            ret = new MultipleDEs(ref, '+', getPath(), document);
        else if ((ref.getNodeName()).equals("DEG"))
            ret = new MultipleDEGs(ref, '+', getPath(), document);

        return ret;
    }

    public void init(String type, String name, String path, int idx, Document document) {
        super.init(type, name, path, idx, document);
    }

    public String toString(int dummy) {
        StringBuilder ret = new StringBuilder(256);
        boolean first = true;

        if (isValid()) {
            int tooMuch = 0;
            int saveLen;
            for (MultipleSyntaxElements multipleSyntaxElements : getChildContainers()) {
                if (!first)
                    ret.append('+');

                saveLen = ret.length();
                if (multipleSyntaxElements != null)
                    ret.append(multipleSyntaxElements.toString(0));

                if (ret.length() == saveLen && !first) {
                    tooMuch++;
                } else {
                    tooMuch = 0;
                }
                first = false;
            }

            int retlen = ret.length();
            ret.delete(retlen - tooMuch, retlen);
            ret.append('\'');
        }

        return ret.toString();
    }

    public void setSeq(int idx, boolean allowOverwrite) {
        String segcounterPath = "SegHead.seq";
        String targetPath = getPath() + "." + segcounterPath;

        if (!propagateValue(targetPath,
            Integer.toString(idx), DONT_TRY_TO_CREATE, allowOverwrite))
            throw new NoSuchPathException(targetPath);
    }

    public int enumerateSegs(int idx, boolean allowOverwrite) {
        if (idx == 0 || isValid()) {
            setSeq(idx, allowOverwrite);
            if (idx != 0)
                idx++;
        }

        return idx;
    }

    // ---------------------------------------------------------------------------------------------------------------

    // Wird in Crypt.isCrypted() benötigt, um anhand des SegCodes des zweiten
    // Segments festzustellen, ob die Nachricht verschlüsselt ist oder nicht.
    // analoges in Sig.hasSig()
    public String getCode() {
        String codePath = "SegHead.code";
        return Optional.ofNullable(getElement(getPath() + "." + codePath))
            .map(Object::toString)
            .orElse(null);
    }

    protected MultipleSyntaxElements parseNewChildContainer(Node dataref, char predelim0, char predelim1, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        MultipleSyntaxElements ret = null;

        if ((dataref.getNodeName()).equals("DEG"))
            ret = new MultipleDEGs(dataref, '+', getPath(), predelim0, predelim1, res, document, predefs, valids);
        else if ((dataref.getNodeName()).equals("DE"))
            ret = new MultipleDEs(dataref, '+', getPath(), predelim0, predelim1, res, document, predefs, valids);

        return ret;
    }

    protected char getInDelim() {
        return '+';
    }

    public void init(String type, String name, String path, char predelim, int idx, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        super.init(type, name, path, predelim, idx, res, document, predefs, valids);
    }

    public int checkSegSeq(int value) {
        int num = Integer.parseInt(getValueOfDE(getPath() + ".SegHead.seq"));
        if (num != value) {
            throw new InvalidSegSeqException(getPath(), value, num);
        }
        return value + 1;
    }

    public void getElementPaths(HashMap<String, String> p, int[] segref, int[] degref, int[] deref) {
        if (isValid()) {
            p.put(Integer.toString(segref[0]), getPath());
            degref = new int[1];
            degref[0] = 1;

            for (MultipleSyntaxElements l : getChildContainers()) {
                if (l != null) {
                    l.getElementPaths(p, segref, degref, null);
                }
            }

            segref[0]++;
        }
    }

}
