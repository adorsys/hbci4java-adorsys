/*  $Id: DE.java,v 1.1 2011/05/04 22:38:02 willuhn Exp $

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

import org.kapott.hbci.datatypes.SyntaxDE;
import org.kapott.hbci.datatypes.factory.SyntaxDEFactory;
import org.kapott.hbci.exceptions.*;
import org.kapott.hbci.manager.HBCIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.*;

public final class DE extends SyntaxElement {

    private SyntaxDE value;
    private int minsize;
    private int maxsize;
    private List<String> valids;

    public DE(Node dedef, String name, String path, int idx, Document document) {
        super(((Element) dedef).getAttribute("type"), name, path, idx, null);
        initData(dedef, name, path, idx, document);
    }

    public DE(Node dedef, String name, String path, char predelim, int idx, StringBuffer res, HashMap<String, String> predefs, HashMap<String, String> valids) {
        super(((Element) dedef).getAttribute("type"), name, path, predelim, idx, res, null, predefs, valids);
        initData(dedef, res, predefs, predelim, valids);
    }

    @Override
    protected MultipleSyntaxElements createNewChildContainer(Node dedef, Document document) {
        return null;
    }

    @Override
    protected String getElementTypeName() {
        return "DE";
    }

    /**
     * setzen des wertes des de
     */
    @Override
    public boolean propagateValue(String destPath, String valueString, boolean tryToCreate, boolean allowOverwrite) {
        boolean ret = false;

        // wenn dieses de gemeint ist
        if (destPath.equals(getPath())) {
            if (this.value != null) { // es gibt schon einen Wert
                if (!allowOverwrite) { // überschreiben ist nicht erlaubt
                    throw new OverwriteException(getPath(), value.toString(), valueString);
                }
            }

            setValue(valueString);
            ret = true;
        }

        return ret;
    }

    @Override
    public String getValueOfDE(String path) {
        String ret = null;

        if (path.equals(getPath()))
            ret = value.toString();

        return ret;
    }

    @Override
    public String getValueOfDE(String path, int zero) {
        String ret = null;

        if (path.equals(getPath()))
            ret = value.toString(0);

        return ret;
    }

    private void initData(Node dedef, String name, String path, int idx, Document document) {
        this.value = null;
        this.valids = new ArrayList<String>();

        String st;

        minsize = 1;
        st = ((Element) dedef).getAttribute("minsize");
        if (st.length() != 0)
            minsize = Integer.parseInt(st);

        maxsize = 0;
        st = ((Element) dedef).getAttribute("maxsize");
        if (st.length() != 0)
            maxsize = Integer.parseInt(st);
    }

    public void init(Node dedef, String name, String path, int idx, Document document) {
        super.init(((Element) dedef).getAttribute("type"), name, path, idx, null);
        initData(dedef, name, path, idx, document);
    }

    /**
     * validierung eines DE: validate ist ok, wenn DE einen wert enthaelt und
     * der wert in der liste der gueltigen werte auftaucht
     */
    @Override
    public void validate() {
        if (value == null) {
            throw new NoValueGivenException(getPath());
        }

        if (valids.size() != 0) {
            boolean ok = false;
            String valString = (value != null) ? value.toString() : "";

            for (int i = 0; i < valids.size(); i++) {
                if (valids.get(i).equals(valString)) {
                    ok = true;
                    break;
                }
            }

            if (!ok) {
                throw new NoValidValueException(getPath(), valString);
            }
        }

        setValid(true);
    }

    public void setValids(List<String> valids) {
        this.valids = valids;
    }

    public int getMinSize() {
        return minsize;
    }

    public SyntaxDE getValue() {
        return value;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void setValue(String st) {
        this.value = SyntaxDEFactory.createSyntaxDE(getType(), getPath(), st, minsize, maxsize);
    }

    @Override
    protected MultipleSyntaxElements parseNewChildContainer(Node deref, char predelim0, char predelim1, StringBuffer res, Document document, HashMap<String, String> predefs, HashMap<String, String> valids) {
        return null;
    }

    @Override
    protected char getInDelim() {
        return (char) 0;
    }

    /**
     * anlegen eines de beim parsen funktioniert analog zum
     * anlegen eines de bei der message-synthese
     */
    private void parseValue(StringBuffer res, HashMap<String, String> predefs, char preDelim, HashMap<String, String> valids) {
        int len = res.length();

        if (preDelim != (char) 0 && res.charAt(0) != preDelim) {
            if (len == 0) {
                throw new ParseErrorException(HBCIUtils.getLocMsg("EXCMSG_ENDOFSTRG", getPath()));
            }

            // log.("error string: "+res.toString(),log._ERR);
            // log.("current: "+getPath()+":"+type+"("+minsize+","+maxsize+")="+value,log._ERR);
            // log.("predelimiter mismatch (required:"+getPreDelim()+" found:"+temp.charAt(0)+")",log._ERR);
            throw new PredelimErrorException(getPath(), Character.toString(preDelim), Character.toString(res.charAt(0)));
        }

        this.value = SyntaxDEFactory.createSyntaxDE(getType(), getPath(), res, minsize, maxsize);

        String valueString = value.toString(0);
        String predefined = predefs.get(getPath());
        if (predefined != null) {
            if (!valueString.equals(predefined)) {
                throw new ParseErrorException(HBCIUtils.getLocMsg("EXCMSG_PREDEFERR",
                    new Object[]{getPath(), predefined, value}));
            }
        }

        boolean atLeastOne = false;
        boolean ok = false;
        if (valids != null) {
            String header = getPath() + ".value";
            for (String key: valids.keySet()) {
                if (key.startsWith(header) &&
                    key.indexOf(".", header.length()) == -1) {

                    atLeastOne = true;
                    String validValue = valids.get(key);
                    if (valueString.equals(validValue)) {
                        ok = true;
                        break;
                    }
                }
            }
        }

        if (atLeastOne && !ok) {
            throw new NoValidValueException(getPath(), valueString);
        }
    }

    private void initData(Node dedef, StringBuffer res, HashMap<String, String> predefs, char preDelim, HashMap<String, String> valids) {
        setValid(false);

        value = null;
        this.valids = new ArrayList<>();

        String st;

        minsize = 1;
        st = ((Element) dedef).getAttribute("minsize");
        if (st.length() != 0)
            minsize = Integer.parseInt(st);

        maxsize = 0;
        st = ((Element) dedef).getAttribute("maxsize");
        if (st.length() != 0)
            maxsize = Integer.parseInt(st);

        try {
            parseValue(res, predefs, preDelim, valids);
            setValid(true);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    public void init(Node dedef, String name, String path, char predelim, int idx, StringBuffer res, HashMap<String, String> predefs, HashMap<String, String> valids) {
        super.init(((Element) dedef).getAttribute("type"), name, path, predelim, idx, res, null, predefs, valids);
        initData(dedef, res, predefs, predelim, valids);
    }

    @Override
    public void extractValues(HashMap<String, String> values) {
        if (isValid())
            values.put(getPath(), value.toString());
    }

    @Override
    public String toString() {
        return isValid() ? value.toString() : "";
    }

    @Override
    public String toString(int dummy) {
        return isValid() ? value.toString(0) : "";
    }

    public void getElementPaths(Properties p, int[] segref, int[] degref, int[] deref) {
        if (deref == null) {
            p.setProperty(Integer.toString(segref[0]) +
                ":" + Integer.toString(degref[0]), getPath());
            degref[0]++;
        } else {
            p.setProperty(Integer.toString(segref[0]) +
                    ":" +
                    Integer.toString(degref[0]) +
                    "," +
                    Integer.toString(deref[0]),
                getPath());
            deref[0]++;
        }
    }

}
