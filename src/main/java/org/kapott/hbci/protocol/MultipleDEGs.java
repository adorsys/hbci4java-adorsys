/*  $Id: MultipleDEGs.java,v 1.1 2011/05/04 22:38:03 willuhn Exp $

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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Iterator;

@Slf4j
public final class MultipleDEGs extends MultipleSyntaxElements {

    private char delimiter;

    public MultipleDEGs(Node degref, char delimiter, String path, Document document) {
        super(degref, path, document);
        initData(delimiter);
    }

    public MultipleDEGs(Node degref, char delimiter, String path, char predelim0, char predelim1, StringBuffer res,
                        Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        super(degref, path, predelim0, predelim1, res, document, predefs, valids);
        initData(delimiter);
    }

    protected SyntaxElement createAndAppendNewElement(Node ref, String path, int idx, Document document) {
        SyntaxElement ret = new DEG(getType(), getName(), path, idx, document);

        addElement(ret);
        return ret;
    }

    private void initData(char delimiter) {
        this.delimiter = delimiter;
    }

    public void init(Node degref, char delimiter, String path, Document document) {
        super.init(degref, path, document);
        initData(delimiter);
    }

    public String toString(int dummy) {
        StringBuffer ret = new StringBuffer(128);
        boolean first = true;

        for (SyntaxElement syntaxElement : getElements()) {
            if (!first)
                ret.append(delimiter);
            first = false;

            if (syntaxElement != null)
                ret.append(((DEG) syntaxElement).toString(0));
        }

        return ret.toString();
    }

    // --------------------------------------------------------------------------------------------------------------

    protected SyntaxElement parseAndAppendNewElement(Node ref, String path, char predelim, int idx, StringBuffer res,
                                                     Document document, HashMap<String, String> predefs,
                                                     HashMap<String, String> valids) {
        SyntaxElement ret;
        addElement((ret = new DEG(getType(), getName(), path, predelim, idx, res, document, predefs, valids)));
        return ret;
    }

    public void init(Node degref, char delimiter, String path, char predelim0, char predelim1, StringBuffer res,
                     Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        super.init(degref, path, predelim0, predelim1, res, document, predefs, valids);
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
                p.put(segref[0] + ":" + degref[0], getPath());
                degref[0]++;
            } else {
                p.put(segref[0] + ":" + degref[0] + "," + degref[0], getPath());
                deref[0]++;
            }
        }
    }
}
