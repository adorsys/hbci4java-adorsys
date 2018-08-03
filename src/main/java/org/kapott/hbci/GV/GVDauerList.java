/*  $Id: GVDauerList.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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

import org.kapott.hbci.GV_Result.GVRDauerList;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

import java.util.HashMap;


public final class GVDauerList extends AbstractHBCIJob {

    public GVDauerList(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRDauerList(passport));

        addConstraint("my.country", "KTV.KIK.country", "DE");
        addConstraint("my.blz", "KTV.KIK.blz", null);
        addConstraint("my.number", "KTV.number", null);
        addConstraint("my.subnumber", "KTV.subnumber", "");
        addConstraint("orderid", "orderid", "");
        addConstraint("maxentries", "maxentries", "");
    }

    public static String getLowlevelName() {
        return "DauerList";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRDauerList.Dauer entry = new GVRDauerList.Dauer();

        entry.my = new Konto();
        entry.my.country = result.get(header + ".My.KIK.country");
        entry.my.blz = result.get(header + ".My.KIK.blz");
        entry.my.number = result.get(header + ".My.number");
        entry.my.subnumber = result.get(header + ".My.subnumber");
        passport.fillAccountInfo(entry.my);

        entry.other = new Konto();
        entry.other.country = result.get(header + ".Other.KIK.country");
        entry.other.blz = result.get(header + ".Other.KIK.blz");
        entry.other.number = result.get(header + ".Other.number");
        entry.other.subnumber = result.get(header + ".Other.subnumber");
        entry.other.name = result.get(header + ".name");
        entry.other.name2 = result.get(header + ".name2");

        entry.value = new Value(
            result.get(header + ".BTG.value"),
            result.get(header + ".BTG.curr"));
        entry.key = result.get(header + ".key");
        entry.addkey = result.get(header + ".addkey");

        for (int i = 0; ; i++) {
            String usage = result.get(header + ".usage." + HBCIUtils.withCounter("usage", i));
            if (usage == null)
                break;
            entry.addUsage(usage);
        }

        String st;
        if ((st = result.get(header + ".date")) != null)
            entry.nextdate = HBCIUtils.string2DateISO(st);

        entry.orderid = result.get(header + ".orderid");

        entry.firstdate = HBCIUtils.string2DateISO(result.get(header + ".DauerDetails.firstdate"));
        entry.timeunit = result.get(header + ".DauerDetails.timeunit");
        entry.turnus = Integer.parseInt(result.get(header + ".DauerDetails.turnus"));
        entry.execday = Integer.parseInt(result.get(header + ".DauerDetails.execday"));
        if ((st = result.get(header + ".DauerDetails.lastdate")) != null)
            entry.lastdate = HBCIUtils.string2DateISO(st);

        entry.aus_available = result.get(header + ".Aussetzung.annual") != null;
        if (entry.aus_available) {
            entry.aus_annual = result.get(header + ".Aussetzung.annual").equals("J");
            if ((st = result.get(header + ".Aussetzung.startdate")) != null)
                entry.aus_start = HBCIUtils.string2DateISO(st);
            if ((st = result.get(header + ".Aussetzung.enddate")) != null)
                entry.aus_end = HBCIUtils.string2DateISO(st);
            entry.aus_breakcount = result.get(header + ".Aussetzung.number");
            if ((st = result.get(header + ".Aussetzung.newvalue.value")) != null) {
                entry.aus_newvalue = new Value(
                    st,
                    result.get(header + ".Aussetzung.newvalue.curr"));
            }
        }

        ((GVRDauerList) (jobResult)).addEntry(entry);

        if (entry.orderid != null && entry.orderid.length() != 0) {
            HashMap<String, String> p2 = new HashMap<>();

            for (String key : result.keySet()) {
                if (key.startsWith(header + ".") &&
                    !key.startsWith(header + ".SegHead.") &&
                    !key.endsWith(".orderid")) {
                    p2.put(key.substring(header.length() + 1),
                        result.get(key));
                }
            }

//TODO            passport.setPersistentData("dauer_" + entry.orderid, p2);
        }
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
