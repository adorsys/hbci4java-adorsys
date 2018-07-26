/*  $Id: DEG.java,v 1.1 2011/05/04 22:38:02 willuhn Exp $

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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;

public final class DEG extends SyntaxElement {

    public DEG(String type, String name, String path, int idx, Document document) {
        super(type, name, path, idx, document);
    }

    public DEG(String type, String name, String path, char predelim, int idx, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        super(type, name, path, predelim, idx, res, fullResLen, document, predefs, valids);
    }

    protected String getElementTypeName() {
        return "DEG";
    }

    protected MultipleSyntaxElements createNewChildContainer(Node ref, Document document) {
        MultipleSyntaxElements ret = null;

        if ((ref.getNodeName()).equals("DE"))
            ret = new MultipleDEs(ref, ':', getPath(), document);
        else if ((ref.getNodeName()).equals("DEG"))
            ret = new MultipleDEGs(ref, ':', getPath(), document);

        return ret;
    }

    public void init(String type, String name, String path, int idx, Document document) {
        super.init(type, name, path, idx, document);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String toString(int dummy) {
        StringBuffer ret = new StringBuffer(128);
        boolean first = true;

        if (isValid()) {
            int tooMuch = 0;
            int saveLen;
            for (ListIterator<MultipleSyntaxElements> i = getChildContainers().listIterator(); i.hasNext(); ) {
                if (!first)
                    ret.append(':');

                saveLen = ret.length();
                MultipleSyntaxElements dataList = i.next();
                if (dataList != null)
                    ret.append(dataList.toString(0));

                if (ret.length() == saveLen && !first) {
                    tooMuch++;
                } else {
                    tooMuch = 0;
                }
                first = false;
            }

            // das auslassen von leeren elementen am ende darf nur erfolgen, wenn
            // es nicht um eine DEG innerhalb einer anderen DEG handelt
            if (getParent().getParent().getInDelim() != ':') {
                int retlen = ret.length();
                ret.delete(retlen - tooMuch, retlen);
            }
        }

        return ret.toString();
    }

    protected MultipleSyntaxElements parseNewChildContainer(Node dataref, char predelim0, char predelim1, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        MultipleSyntaxElements ret = null;

        if ((dataref.getNodeName()).equals("DEG"))
            ret = new MultipleDEGs(dataref, ':', getPath(), predelim0, predelim1, res, fullResLen, document, predefs, valids);
        else if ((dataref.getNodeName()).equals("DE"))
            ret = new MultipleDEs(dataref, ':', getPath(), predelim0, predelim1, res, fullResLen, document, predefs, valids);

        return ret;
    }

    protected char getInDelim() {
        return ':';
    }

    public void init(String type, String name, String path, char predelim, int idx, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        super.init(type, name, path, predelim, idx, res, fullResLen, document, predefs, valids);
    }

    public void getElementPaths(HashMap<String, String> p, int[] segref, int[] degref, int[] deref) {
        if (deref == null) {
            p.put(Integer.toString(segref[0]) +
                    ":" + Integer.toString(degref[0]), getPath());

            deref = new int[1];
            deref[0] = 1;

            for (Iterator<MultipleSyntaxElements> i = getChildContainers().iterator(); i.hasNext(); ) {
                MultipleSyntaxElements l = i.next();
                if (l != null) {
                    l.getElementPaths(p, segref, degref, deref);
                }
            }

            degref[0]++;
        } else {
            p.put(Integer.toString(segref[0]) +
                            ":" +
                            Integer.toString(degref[0]) +
                            "," +
                            Integer.toString(deref[0]),
                    getPath());
            deref[0]++;
        }
    }
}
