/*  $Id: GVAccInfo.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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


import org.kapott.hbci.GV_Result.GVRAccInfo;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

import java.util.HashMap;

public class GVAccInfo extends AbstractHBCIJob {

    public GVAccInfo(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRAccInfo(passport));

        addConstraint("my.country", "KTV.KIK.country", "DE");
        addConstraint("my.blz", "KTV.KIK.blz", null);
        addConstraint("my.number", "KTV.number", null);
        addConstraint("my.subnumber", "KTV.subnumber", "");
        addConstraint("all", "allaccounts", "N");
    }

    public static String getLowlevelName() {
        return "AccInfo";
    }

    public void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRAccInfo.AccInfo info = new GVRAccInfo.AccInfo();
        String st;

        info.account = new Konto();
        info.account.blz = result.get(header + ".My.KIK.blz");
        info.account.country = result.get(header + ".My.KIK.country");
        info.account.number = result.get(header + ".My.number");
        info.account.subnumber = result.get(header + ".My.subnumber");
        info.account.curr = result.get(header + ".curr");
        info.account.name = result.get(header + ".name");
        info.account.name2 = result.get(header + ".name2");
        info.account.type = result.get(header + ".accbez");

        info.comment = result.get(header + ".info");
        if ((st = result.get(header + ".opendate")) != null)
            info.created = HBCIUtils.string2DateISO(st);

        info.habenzins = ((st = result.get(header + ".habenzins")) != null) ? HBCIUtils.string2Long(st, 1000) : -1;
        info.sollzins = ((st = result.get(header + ".sollzins")) != null) ? HBCIUtils.string2Long(st, 1000) : -1;
        info.ueberzins = ((st = result.get(header + ".overdrivezins")) != null) ? HBCIUtils.string2Long(st, 1000) : -1;

        if ((st = result.get(header + ".kredit.value")) != null)
            info.kredit = new Value(st, result.get(header + ".kredit.curr"));
        if ((st = result.get(header + ".refkto.number")) != null)
            info.refAccount = new Konto(result.get(header + ".refkto.KIK.country"),
                result.get(header + ".refkto.KIK.blz"),
                st,
                result.get(header + ".refkto.subnumber"));
        info.turnus = ((st = result.get(header + ".turnus")) != null) ? Integer.parseInt(st) : -1;
        info.versandart = ((st = result.get(header + ".versandart")) != null) ? Integer.parseInt(st) : -1;
        info.type = ((st = result.get(header + ".acctype")) != null) ? Integer.parseInt(st) : -1;

        if (result.get(header + ".Address.name1") != null) {
            info.address = new GVRAccInfo.AccInfo.Address();
            info.address.name1 = result.get(header + ".Address.name1");
            info.address.name2 = result.get(header + ".Address.name2");
            info.address.street_pf = result.get(header + ".Address.street_pf");

            if (result.get(header + ".Address.plz") != null) {
                // Version 2
                info.address.plz = result.get(header + ".Address.plz");
                info.address.ort = result.get(header + ".Address.ort");
                info.address.country = result.get(header + ".Address.country");
                info.address.tel = result.get(header + ".Address.tel");
                info.address.fax = result.get(header + ".Address.fax");
                info.address.email = result.get(header + ".Address.email");
            } else {
                // Version 1
                info.address.plz_ort = result.get(header + ".Address.plz_ort");
                info.address.tel = result.get(header + ".Address.tel");
            }
        }

        ((GVRAccInfo) getJobResult()).addEntry(info);
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
