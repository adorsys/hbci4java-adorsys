/*  $Id: GVTermUebList.java,v 1.1 2011/05/04 22:37:52 willuhn Exp $

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

import org.kapott.hbci.GV_Result.GVRTermUebList;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

import java.util.HashMap;


public final class GVTermUebList extends AbstractHBCIJob {

    public GVTermUebList(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRTermUebList(passport));

        addConstraint("my.country", "KTV.KIK.country", "DE");
        addConstraint("my.blz", "KTV.KIK.blz", null);
        addConstraint("my.number", "KTV.number", null);
        addConstraint("my.subnumber", "KTV.subnumber", "");
        addConstraint("startdate", "startdate", "");
        addConstraint("enddate", "enddate", "");
        addConstraint("maxentries", "maxentries", "");
    }

    public static String getLowlevelName() {
        return "TermUebList";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRTermUebList.Entry entry = new GVRTermUebList.Entry();

        entry.my = new Konto();
        entry.my.blz = result.get(header + ".My.KIK.blz");
        entry.my.country = result.get(header + ".My.KIK.country");
        entry.my.number = result.get(header + ".My.number");
        entry.my.subnumber = result.get(header + ".My.subnumber");
        passport.fillAccountInfo(entry.my);

        entry.other = new Konto();
        entry.other.blz = result.get(header + ".Other.KIK.blz");
        entry.other.country = result.get(header + ".Other.KIK.country");
        entry.other.number = result.get(header + ".Other.number");
        entry.other.subnumber = result.get(header + ".Other.subnumber");
        entry.other.name = result.get(header + ".name");
        entry.other.name2 = result.get(header + ".name2");
        passport.fillAccountInfo(entry.other);

        entry.key = result.get(header + ".key");
        entry.addkey = result.get(header + ".addkey");
        entry.orderid = result.get(header + ".id");
        entry.date = HBCIUtils.string2DateISO(result.get(header + ".date"));

        entry.value = new Value(
            result.get(header + ".BTG.value"),
            result.get(header + ".BTG.curr"));

        for (int i = 0; ; i++) {
            String usage = result.get(HBCIUtils.withCounter(header + ".usage.usage", i));
            if (usage == null) {
                break;
            }
            entry.addUsage(usage);
        }

        ((GVRTermUebList) jobResult).addEntry(entry);

        if (entry.orderid != null && entry.orderid.length() != 0) {
            HashMap<String, String> p2 = new HashMap();

            p2.keySet().forEach(key -> {
                if (key.startsWith(header + ".") &&
                    !key.startsWith(header + ".SegHead.") &&
                    !key.endsWith(".id")) {
                    p2.put(key.substring(header.length() + 1),
                        result.get(key));
                }
            });

//TODO            passport.setPersistentData("termueb_" + entry.orderid, p2);
        }
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
