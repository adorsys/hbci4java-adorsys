/*  $Id: MultipleSFs.java,v 1.1 2011/05/04 22:38:03 willuhn Exp $

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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;

@Slf4j
public final class MultipleSFs extends MultipleSyntaxElements {

    public MultipleSFs(Node sfref, String path, Document document) {
        super(sfref, path, document);
    }

    public MultipleSFs(Node sfref, String path, char predelim0, char predelim1, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        super(sfref, path, predelim0, predelim1, res, document, predefs, valids);
    }

    protected SyntaxElement createAndAppendNewElement(Node ref, String path, int idx, Document document) {
        SyntaxElement ret = null;
        addElement((ret = new SF(getType(), getName(), path, idx, document)));
        return ret;
    }

    public void init(Node sfref, String path, Document document) {
        super.init(sfref, path, document);
    }

    public String toString(int dummy) {
        StringBuffer ret = new StringBuffer(256);

        for (ListIterator<SyntaxElement> i = getElements().listIterator(); i.hasNext(); ) {
            SF sf = (SF) (i.next());
            if (sf != null)
                ret.append(sf.toString(0));
        }

        return ret.toString();
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void init(Node sfref, String path, char predelim0, char predelim1, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        super.init(sfref, path, predelim0, predelim1, res, document, predefs, valids);
    }

    protected SyntaxElement parseAndAppendNewElement(Node ref, String path, char predelim, int idx, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        SyntaxElement ret;
        addElement((ret = new SF(getType(), getName(), path, predelim, idx, res, document, predefs, valids)));
        return ret;
    }

    public void getElementPaths(HashMap<String, String> p, int[] segref, int[] degref, int[] deref) {
        for (Iterator<SyntaxElement> i = getElements().iterator(); i.hasNext(); ) {
            SyntaxElement e = i.next();
            if (e != null) {
                e.getElementPaths(p, segref, degref, deref);
            }
        }
    }
}
