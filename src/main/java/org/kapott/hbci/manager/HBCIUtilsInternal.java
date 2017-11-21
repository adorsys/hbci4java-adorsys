
/*  $Id: HBCIUtilsInternal.java,v 1.1 2011/05/04 22:37:47 willuhn Exp $

    This file is part of hbci4java
    Copyright (C) 2001-2008  Stefan Palme

    hbci4java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    hbci4java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.manager;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.slf4j.LoggerFactory;

public class HBCIUtilsInternal {

    public static Properties blzs;
    public static Map<String, BankInfo> banks = null;

    public static String bigDecimal2String(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("0.##");
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        format.setDecimalFormatSymbols(symbols);
        format.setDecimalSeparatorAlwaysShown(false);
        return format.format(value);
    }

    public static String getLocMsg(String key) {
        try {
            return ResourceBundle.getBundle("hbci4java-messages", Locale.getDefault()).getString(key);
        } catch (MissingResourceException re) {
            // tolerieren wir
            LoggerFactory.getLogger(HBCIUtilsInternal.class).debug(re.getMessage(), re);
            return key;
        }
    }

    public static String getLocMsg(String key, Object o) {
        return HBCIUtilsInternal.getLocMsg(key, new Object[]{o});
    }

    public static String getLocMsg(String key, Object[] o) {
        return MessageFormat.format(getLocMsg(key), o);
    }

    public static boolean ignoreError(HBCIPassport passport, String paramName, String msg) {
        boolean ret = false;
        String  paramValue = "no";
        if (passport != null) {
            paramValue = passport.getProperties().getProperty(paramName, "no");
        }

        if (paramValue.equals("yes")) {
            LoggerFactory.getLogger(HBCIUtilsInternal.class).info(msg, HBCIUtils.LOG_ERR);
            LoggerFactory.getLogger(HBCIUtilsInternal.class).info("ignoring error because param " + paramName + "=yes", HBCIUtils.LOG_ERR);
            ret = true;
        }

        return ret;
    }

    public static long string2Long(String st, long factor) {
        BigDecimal result = new BigDecimal(st);
        result = result.multiply(new BigDecimal(factor));
        return result.longValue();
    }

    public static String withCounter(String st, int idx) {
        return st + ((idx != 0) ? "_" + Integer.toString(idx + 1) : "");
    }

    public static int getPosiOfNextDelimiter(String st, int posi) {
        int len = st.length();
        boolean quoting = false;
        while (posi < len) {
            char ch = st.charAt(posi);

            if (!quoting) {
                if (ch == '?') {
                    quoting = true;
                } else if (ch == '@') {
                    int endpos = st.indexOf('@', posi + 1);
                    String binlen_st = st.substring(posi + 1, endpos);
                    int binlen = Integer.parseInt(binlen_st);
                    posi += binlen_st.length() + 1 + binlen;
                } else if (ch == '\'' || ch == '+' || ch == ':') {
                    // Ende gefunden
                    break;
                }
            } else {
                quoting = false;
            }

            posi++;
        }

        return posi;
    }

    public static String stripLeadingZeroes(String st) {
        String ret = null;

        if (st != null) {
            int start = 0;
            int l = st.length();
            while (start < l && st.charAt(start) == '0') {
                start++;
            }
            ret = st.substring(start);
        }

        return ret;
    }
}
