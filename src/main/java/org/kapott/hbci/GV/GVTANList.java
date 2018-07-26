/*  $Id: GVTANList.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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

package org.kapott.hbci.GV;


import org.kapott.hbci.GV_Result.GVRTANList;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

public class GVTANList extends AbstractHBCIJob {

    public GVTANList(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRTANList(passport));
    }

    public static String getLowlevelName() {
        return "TANListList";
    }

    public void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRTANList.TANList list = new GVRTANList.TANList();

        list.status = result.get(header + ".liststatus").charAt(0);
        list.number = result.get(header + ".listnumber");
        String st = result.get(header + ".date");
        if (st != null)
            list.date = HBCIUtils.string2DateISO(st);

        String noftansperlist = result.get(header + ".noftansperlist") != null ? result.get(header + ".noftansperlist") : "0";
        String nofusedtansperlist = result.get(header + ".nofusedtansperlist") != null ? result.get(header + ".nofusedtansperlist") : "0";

        list.nofTANsPerList = Integer.parseInt(noftansperlist);
        list.nofUsedTANsPerList = Integer.parseInt(nofusedtansperlist);

        for (int i = 0; ; i++) {
            String tanheader = HBCIUtils.withCounter(header + ".TANInfo", i);

            st = result.get(tanheader + ".usagecode");
            if (st == null)
                break;

            GVRTANList.TANInfo info = new GVRTANList.TANInfo();

            info.usagecode = Integer.parseInt(st);
            info.usagetxt = result.get(tanheader + ".usagetxt");
            info.tan = result.get(tanheader + ".tan");

            String usagedate = result.get(tanheader + ".usagedate");
            String usagetime = result.get(tanheader + ".usagetime");
            if (usagedate != null) {
                if (usagetime == null) {
                    info.timestamp = HBCIUtils.string2DateISO(usagedate);
                } else {
                    info.timestamp = HBCIUtils.strings2DateTimeISO(usagedate, usagetime);
                }
            }

            list.addTANInfo(info);
        }

        ((GVRTANList) jobResult).addTANList(list);
    }
}
