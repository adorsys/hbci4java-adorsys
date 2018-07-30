/*  $Id: GVFestList.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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


import org.kapott.hbci.GV_Result.GVRFestCondList;
import org.kapott.hbci.GV_Result.GVRFestList;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

import java.util.HashMap;

public class GVFestList extends AbstractHBCIJob {

    public GVFestList(String name, HBCIPassportInternal passport) {
        super(passport, name, new GVRFestList(passport));
    }

    public GVFestList(HBCIPassportInternal passport) {
        this(getLowlevelName(), passport);

        addConstraint("my.number", "KTV.number", null);
        addConstraint("my.subnumber", "KTV.subnumber", "");
        addConstraint("my.blz", "KTV.KIK.blz", null);
        addConstraint("my.country", "KTV.KIK.country", "DE");
        addConstraint("dummy", "allaccounts", "N");

        // TODO: kontakt fehlt
        // TODO: maxentries fehlen
    }

    public static String getLowlevelName() {
        return "FestList";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRFestList.Entry entry = new GVRFestList.Entry();

        entry.anlagebetrag = new Value(
            result.get(header + ".Anlagebetrag.value"),
            result.get(header + ".Anlagebetrag.curr"));

        if (result.get(header + ".Anlagekto.number") != null) {
            entry.anlagekonto = new Konto();
            entry.anlagekonto.blz = result.get(header + ".Anlagekto.KIK.blz");
            entry.anlagekonto.country = result.get(header + ".Anlagekto.KIK.country");
            entry.anlagekonto.number = result.get(header + ".Anlagekto.number");
            entry.anlagekonto.subnumber = result.get(header + ".Anlagekto.subnumber");
            passport.fillAccountInfo(entry.anlagekonto);
        }

        if (result.get(header + ".Ausbuchungskto.number") != null) {
            entry.ausbuchungskonto = new Konto();
            entry.ausbuchungskonto.blz = result.get(header + ".Ausbuchungskto.KIK.blz");
            entry.ausbuchungskonto.country = result.get(header + ".Ausbuchungskto.KIK.country");
            entry.ausbuchungskonto.number = result.get(header + ".Ausbuchungskto.number");
            entry.ausbuchungskonto.subnumber = result.get(header + ".Ausbuchungskto.subnumber");
            passport.fillAccountInfo(entry.ausbuchungskonto);
        }

        entry.belastungskonto = new Konto();
        entry.belastungskonto.blz = result.get(header + ".Belastungskto.KIK.blz");
        entry.belastungskonto.country = result.get(header + ".Belastungskto.KIK.country");
        entry.belastungskonto.number = result.get(header + ".Belastungskto.number");
        entry.belastungskonto.subnumber = result.get(header + ".Belastungskto.subnumber");
        passport.fillAccountInfo(entry.belastungskonto);

        if (result.get(header + ".Zinskto.number") != null) {
            entry.zinskonto = new Konto();
            entry.zinskonto.blz = result.get(header + ".Zinskto.KIK.blz");
            entry.zinskonto.country = result.get(header + ".Zinskto.KIK.country");
            entry.zinskonto.number = result.get(header + ".Zinskto.number");
            entry.zinskonto.subnumber = result.get(header + ".Zinskto.subnumber");
            passport.fillAccountInfo(entry.zinskonto);
        }

        entry.id = result.get(header + ".kontakt");

        String st = result.get(header + ".kontoauszug");
        entry.kontoauszug = (st != null) ? Integer.parseInt(st) : 0;
        st = result.get(header + ".status");
        entry.status = (st != null) ? Integer.parseInt(st) : 0;

        entry.verlaengern = result.get(header + ".wiederanlage").equals("2");

        if (result.get(header + ".Zinsbetrag.value") != null) {
            entry.zinsbetrag = new Value(
                result.get(header + ".Zinsbetrag.value"),
                result.get(header + ".Zinsbetrag.curr"));
        }

        entry.konditionen = new GVRFestCondList.Cond();
        entry.konditionen.ablaufdatum = HBCIUtils.string2DateISO(result.get(header + ".FestCond.ablaufdate"));
        entry.konditionen.anlagedatum = HBCIUtils.string2DateISO(result.get(header + ".FestCond.anlagedate"));
        entry.konditionen.id = result.get(header + ".FestCond.condid");
        entry.konditionen.name = result.get(header + ".FestCond.condbez");

        if (result.get(header + ".FestCondVersion.version") != null) {
            entry.konditionen.date = HBCIUtils.strings2DateTimeISO(result.get(header + ".FestCondVersion.date"),
                result.get(header + ".FestCondVersion.time"));
            entry.konditionen.version = result.get(header + ".FestCondVersion.version");
        }

        st = result.get(header + ".FestCond.zinsmethode");
        if (st.equals("A"))
            entry.konditionen.zinsmethode = GVRFestCondList.Cond.METHOD_30_360;
        else if (st.equals("B"))
            entry.konditionen.zinsmethode = GVRFestCondList.Cond.METHOD_2831_360;
        else if (st.equals("C"))
            entry.konditionen.zinsmethode = GVRFestCondList.Cond.METHOD_2831_365366;
        else if (st.equals("D"))
            entry.konditionen.zinsmethode = GVRFestCondList.Cond.METHOD_30_365366;
        else if (st.equals("E"))
            entry.konditionen.zinsmethode = GVRFestCondList.Cond.METHOD_2831_365;
        else if (st.equals("F"))
            entry.konditionen.zinsmethode = GVRFestCondList.Cond.METHOD_30_365;

        entry.konditionen.zinssatz = HBCIUtils.string2Long(result.get(header + ".FestCond.zinssatz"), 1000);
        entry.konditionen.minbetrag = new Value(
            result.get(header + ".FestCond.MinBetrag.value"),
            result.get(header + ".FestCond.MinBetrag.curr"));
        entry.konditionen.name = result.get(header + ".FestCond.condbez");

        if (result.get(header + ".FestCond.MaxBetrag.value") != null) {
            entry.konditionen.maxbetrag = new Value(
                result.get(header + ".FestCond.MaxBetrag.value"),
                result.get(header + ".FestCond.MaxBetrag.curr"));
        }

        if (result.get(header + ".Prolong.laufzeit") != null) {
            entry.verlaengerung = new GVRFestList.Entry.Prolong();
            entry.verlaengerung.betrag = new Value(
                result.get(header + ".Prolong.BTG.value"),
                result.get(header + ".Prolong.BTG.curr"));
            entry.verlaengerung.laufzeit = Integer.parseInt(result.get(header + ".Prolong.laufzeit"));
            entry.verlaengerung.verlaengern = result.get(header + ".Prolong.wiederanlage").equals("2");
        }

        ((GVRFestList) jobResult).addEntry(entry);
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
