/*  $Id: MultipleSEGs.java,v 1.1 2011/05/04 22:38:02 willuhn Exp $

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
public final class MultipleSEGs extends MultipleSyntaxElements {

    public MultipleSEGs(Node segref, String path, Document document) {
        super(segref, path, document);
    }

    public MultipleSEGs(Node segref, String path, char predelim0, char predelim1, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        super(segref, path, predelim0, predelim1, res, fullResLen, document, predefs, valids);
    }

    protected SyntaxElement createAndAppendNewElement(Node ref, String path, int idx, Document document) {
        SyntaxElement ret = new SEG(getType(), getName(), path, idx, document);
        addElement(ret);
        return ret;
    }

    public void init(Node segref, String path, Document document) {
        super.init(segref, path, document);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public String toString(int dummy) {
        StringBuffer ret = new StringBuffer(256);

        for (SyntaxElement syntaxElement : getElements()) {
            if (syntaxElement != null)
                ret.append(((SEG) syntaxElement).toString(0));
        }

        return ret.toString();
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void log() {
        for (ListIterator<SyntaxElement> i = getElements().listIterator(); i.hasNext(); ) {
            SEG seg = (SEG) (i.next());
            if (seg != null)
                log.trace(seg.toString(0));
        }
    }

    public void init(Node segref, String path, char predelim0, char predelim1, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        super.init(segref, path, predelim0, predelim1, res, fullResLen, document, predefs, valids);
    }

    protected SyntaxElement parseAndAppendNewElement(Node ref, String path, char predelim, int idx, StringBuffer res, int fullResLen, Document document, Hashtable<String, String> predefs, Hashtable<String, String> valids) {
        SyntaxElement ret = null;
        addElement((ret = new SEG(getType(), getName(), path, predelim, idx, res, fullResLen, document, predefs, valids)));
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

    // TODO: diese Methode gehört zu einem dirty hack (der aber gut funktioniert)
    // Diese Methode wird von SyntaxElement.parse() verwendet, um bei den
    // SFs "Params" und "GVRes" dafür zu sorgen, dass nach jedem gefunden Segment
    // eine neue SF begonnen wird:
    //   Params.LastPar1
    //   Params_2.UebPar1
    //   Params_3.KUmsPar1
    // anstatt
    //   Params.LastPar1
    //   Params.UebPar1
    //   Params_2.KUmsPar1
    public boolean hasValidChilds() {
        return (getElements().size() != 0);
    }

}
