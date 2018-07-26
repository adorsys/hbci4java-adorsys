/*  $Id: MultipleDEs.java,v 1.1 2011/05/04 22:38:03 willuhn Exp $

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
import org.kapott.hbci.manager.HBCIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.*;

@Slf4j
public final class MultipleDEs extends MultipleSyntaxElements {
    private char delimiter;
    private List<String> valids;

    public MultipleDEs(Node dedef, char delimiter, String path, Document document) {
        super(dedef, path, document);
        initData(delimiter);
    }

    public MultipleDEs(Node deref, char delimiter, String path, char predelim0, char predelim1, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        super(deref, path, predelim0, predelim1, res, fullResLen, document, predefs, valids);
        initData(delimiter);
    }

    protected SyntaxElement createAndAppendNewElement(Node deref, String path, int idx, Document document) {
        SyntaxElement ret;
        addElement(ret = new DE(deref, getName(), path, idx, document));
        return ret;
    }

    public void init(Node dedef, char delimiter, String path, Document document) {
        super.init(dedef, path, document);
        initData(delimiter);
    }

    protected boolean storeValidValueInDE(String destPath, String value) {
        boolean ret = false;

        // wenn dieses de gemeint ist
        if (destPath.equals(getPath())) {
            valids.add(value);
            ret = true;
        }

        return ret;
    }

    protected void validateOneElement(SyntaxElement elem, int idx) {
        ((DE) elem).setValids(valids);
        super.validateOneElement(elem, idx);
    }

    public String toString(int dummy) {
        StringBuffer ret = new StringBuffer(128);
        boolean first = true;

        for (ListIterator<SyntaxElement> i = getElements().listIterator(); i.hasNext(); ) {
            if (!first)
                ret.append(delimiter);
            first = false;

            DE de = (DE) (i.next());
            if (de != null)
                ret.append(de.toString(0));
        }

        return ret.toString();
    }

    // -------------------------------------------------------------------------------------------------------

    public void log() {
        for (ListIterator<SyntaxElement> i = getElements().listIterator(); i.hasNext(); ) {
            DE de = (DE) (i.next());
            if (de != null)
                log.trace(de.toString(0));

        }
    }

    protected SyntaxElement parseAndAppendNewElement(Node ref, String path, char predelim, int idx, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        SyntaxElement ret;

        if (idx != 0 && valids != null) {
            String header = getPath() + ".value";
            for (Enumeration<String> e = valids.keys(); e.hasMoreElements(); ) {
                String key = (e.nextElement());

                if (key.startsWith(header) &&
                        key.indexOf(".", header.length()) == -1) {

                    int dotPos = key.lastIndexOf('.');
                    String newkey = key.substring(0, dotPos) +
                            HBCIUtils.withCounter("", idx) +
                            key.substring(dotPos);
                    valids.put(newkey, valids.get(key));
                }
            }
        }

        addElement(ret = new DE(ref, getName(), path, predelim, idx, res, fullResLen, predefs, valids));
        return ret;
    }

    private void initData(char delimiter) {
        this.delimiter = delimiter;
        this.valids = new ArrayList<>();
    }

    public void init(Node deref, char delimiter, String path, char predelim0, char predelim1, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        super.init(deref, path, predelim0, predelim1, res, fullResLen, document, predefs, valids);
        initData(delimiter);
    }

    public void getElementPaths(HashMap<String, String> p, int[] segref, int[] degref, int[] deref) {
        if (getElements().size() != 0) {
            for (Iterator<SyntaxElement> i = getElements().iterator(); i.hasNext(); ) {
                SyntaxElement e = i.next();
                if (e != null) {
                    e.getElementPaths(p, segref, degref, deref);
                }
            }
        } else {
            if (deref == null) {
                p.put(Integer.toString(segref[0]) +
                        ":" + Integer.toString(degref[0]), getPath());
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

    public void destroy() {
        valids.clear();
        valids = null;

        super.destroy();
    }
}
