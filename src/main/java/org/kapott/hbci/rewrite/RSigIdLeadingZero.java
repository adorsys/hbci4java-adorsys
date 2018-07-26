/*  $Id: RSigIdLeadingZero.java,v 1.2 2012/03/27 21:33:13 willuhn Exp $

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

package org.kapott.hbci.rewrite;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RSigIdLeadingZero extends Rewrite {

    @Override
    public String incomingClearText(String st) {
        StringBuffer ret = new StringBuffer(st);
        int firstPlus = st.indexOf("HNSHK");

        if (firstPlus != -1) {
            for (int i = 0; i < 6; i++) {
                firstPlus = st.indexOf("+", firstPlus + 1);
                if (firstPlus == -1) {
                    break;
                }
            }

            if (firstPlus != -1) {
                int secondPlus = st.indexOf("+", firstPlus + 1);

                if (secondPlus != -1) {
                    StringBuffer value = new StringBuffer(st.substring(firstPlus + 1, secondPlus));

                    if (value.length() > 1 && value.charAt(0) == '0') {
                        log.warn("RSigIdLeadingZero: found leading zero (" + value + "), removing it");
                        while (value.length() > 1 && value.charAt(0) == '0') {
                            value.deleteCharAt(0);
                        }

                        ret.replace(firstPlus + 1, secondPlus, value.toString());
                        log.warn("RSigIdLeadingZero: setting new sigid: " + value);
                    }
                } else {
                    log.warn("RSigIdLeadingZero: can not find end of sigid in segment");
                }
            } else {
                log.warn("RSigIdLeadingZero: can not find sigid in segment");
            }
        }

        return ret.toString();
    }
}
