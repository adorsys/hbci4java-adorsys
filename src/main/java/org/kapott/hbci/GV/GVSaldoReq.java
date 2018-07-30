/*  $Id: GVSaldoReq.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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


import org.kapott.hbci.GV_Result.GVRSaldoReq;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Saldo;
import org.kapott.hbci.structures.Value;

import java.util.HashMap;

public class GVSaldoReq extends AbstractHBCIJob {

    public GVSaldoReq(HBCIPassportInternal passport, String name) {
        super(passport, name, new GVRSaldoReq(passport));
    }

    public GVSaldoReq(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());

        addConstraint("my.country", "KTV.KIK.country", "DE");
        addConstraint("my.blz", "KTV.KIK.blz", null);
        addConstraint("my.number", "KTV.number", null);
        addConstraint("my.subnumber", "KTV.subnumber", "");
        addConstraint("my.curr", "curr", "EUR");
        addConstraint("dummyall", "allaccounts", "N");
        addConstraint("maxentries", "maxentries", "");
    }

    public static String getLowlevelName() {
        return "Saldo";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRSaldoReq.Info info = new GVRSaldoReq.Info();

        info.konto = new Konto();
        info.konto.country = result.get(header + ".KTV.KIK.country");
        info.konto.blz = result.get(header + ".KTV.KIK.blz");
        info.konto.number = result.get(header + ".KTV.number");
        info.konto.subnumber = result.get(header + ".KTV.subnumber");
        info.konto.bic = result.get(header + ".KTV.bic");
        info.konto.iban = result.get(header + ".KTV.iban");
        info.konto.type = result.get(header + ".kontobez");
        info.konto.curr = result.get(header + ".curr");
        passport.fillAccountInfo(info.konto);

        info.ready = new Saldo();
        String cd = result.get(header + ".booked.CreditDebit");
        String bookedValue = result.get(header + ".booked.BTG.value") != null ? result.get(header + ".booked.BTG.value") : "0";
        String st = (cd.equals("D") ? "-" : "") + bookedValue;
        info.ready.value = new Value(
            st,
            result.get(header + ".booked.BTG.curr"));
        info.ready.timestamp = HBCIUtils.strings2DateTimeISO(result.get(header + ".booked.date"),
            result.get(header + ".booked.time"));

        cd = result.get(header + ".pending.CreditDebit");
        if (cd != null) {
            String pendingValue = result.get(header + ".booked.BTG.value") != null ? result.get(header + ".pending.BTG.value") : "0";
            st = (cd.equals("D") ? "-" : "") + pendingValue;
            info.unready = new Saldo();
            info.unready.value = new Value(
                st,
                result.get(header + ".pending.BTG.curr"));
            info.unready.timestamp = HBCIUtils.strings2DateTimeISO(result.get(header + ".pending.date"),
                result.get(header + ".pending.time"));
        }

        st = result.get(header + ".kredit.value");
        if (st != null) {
            info.kredit = new Value(
                st,
                result.get(header + ".kredit.curr"));
        }

        st = result.get(header + ".available.value");
        if (st != null) {
            info.available = new Value(
                st,
                result.get(header + ".available.curr"));
        }

        st = result.get(header + ".used.value");
        if (st != null) {
            info.used = new Value(
                st,
                result.get(header + ".used.curr"));
        }

        ((GVRSaldoReq) (jobResult)).store(info);
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
