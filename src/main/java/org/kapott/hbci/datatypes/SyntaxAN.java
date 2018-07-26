/*  $Id: SyntaxAN.java,v 1.1 2011/05/04 22:37:56 willuhn Exp $

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

package org.kapott.hbci.datatypes;

// Speicherung im orig. Format
public class SyntaxAN extends SyntaxDE {
    /**
     * @internal @brief Creates a data element for storing alphanumeric data, used while creating a message
     * @see SyntaxDE
     */
    public SyntaxAN(String x, int minlen, int maxlen) {
        super(x.trim(), minlen, maxlen);
    }

    /**
     * @internal
     * @see SyntaxDE
     */
    protected SyntaxAN() {
        super();
    }

    /**
     * @param res     A part of the HBCI-message to be parsed. From this (sub-)string the first
     *                token will be used to initialize the data element.
     * @param minsize The minimum string length for this element.
     * @param maxsize The maximum string length for this element (or zero).
     *                See SyntaxDE::setContent(String,int,int,int).
     * @internal
     * @brief Creates a data element for storing alphanumeric data, used while parsing a message.
     * <p>
     * This constructor creates a new data element from a given HBCI message. For this the
     * first token in the HBCI message will be extracted from @p res and used as
     * init value for the data element
     */
    public SyntaxAN(StringBuffer res, int minsize, int maxsize) {
        initData(res, minsize, maxsize);
    }

    /**
     * @param x The String to be quoted.
     * @return A String where all HBCI-specific characters in @p x are quoted using @c ?
     * @internal
     * @brief Quotes all HBCI-specific characters.
     */
    protected static String quote(String x) {
        int len = x.length();
        StringBuffer temp = new StringBuffer(len << 1);

        for (int i = 0; i < len; i++) {
            char ch = x.charAt(i);

            switch (ch) {
                case '+':
                case ':':
                case '\'':
                case '?':
                case '@':
                    temp.append('?');
                    break;
                default:
                    break;
            }
            temp.append(ch);
        }

        return temp.toString();
    }

    /**
     * @param st the String to be unquoted
     * @return an unquoted string, i.e. with all HBCI-quotes (?) removed
     * @internal @brief Returns a String with all quotation characters removed
     */
    protected static String unquote(String st) {
        int len = st.length();
        StringBuffer ret = new StringBuffer(len);
        int idx = 0;

        while (idx < len) {
            char ch = st.charAt(idx++);

            if (ch == '?') {
                ch = st.charAt(idx++);
            }
            ret.append(ch);
        }

        return ret.toString();
    }

    public void init(String x, int minlen, int maxlen) {
        super.init(x.trim(), minlen, maxlen);
    }

    // --------------------------------------------------------------------------------

    protected void init() {
        super.init();
    }

    /**
     * @internal
     * @see SyntaxDE
     */
    public String toString(int zero) {
        String st = getContent();
        return (st == null) ? "" : quote(st);
    }

    private void initData(StringBuffer res, int minsize, int maxsize) {
        int startidx = skipPreDelim(res);
        int endidx = findNextDelim(res, startidx);
        String st = res.substring(startidx, endidx);

        setContent(unquote(st), minsize, maxsize);
        res.delete(0, endidx);
    }

    public void init(StringBuffer res, int minlen, int maxlen) {
        initData(res, minlen, maxlen);
    }
}
