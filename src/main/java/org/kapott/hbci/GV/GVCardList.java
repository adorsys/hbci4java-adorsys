/*  $Id: GVCardList.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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


import org.kapott.hbci.GV_Result.GVRCardList;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.structures.Value;

import java.util.HashMap;

public class GVCardList extends AbstractHBCIJob {

    public GVCardList(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRCardList(passport));

        addConstraint("my.country", "KTV.KIK.country", "DE");
        addConstraint("my.blz", "KTV.KIK.blz", null);
        addConstraint("my.number", "KTV.number", null);
        addConstraint("my.subnumber", "KTV.subnumber", "");
    }

    public static String getLowlevelName() {
        return "CardList";
    }

    public void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRCardList.CardInfo info = new GVRCardList.CardInfo();
        String st;

        info.cardnumber = result.get(header + ".cardnumber");
        info.cardordernumber = result.get(header + ".nextcardnumber");
        info.cardtype = Integer.parseInt(result.get(header + ".cardtype"));
        info.comment = result.get(header + ".comment");
        if ((st = result.get(header + ".cardlimit.value")) != null) {
            info.limit = new Value(st, result.get(header + ".cardlimit.curr"));
        }
        info.owner = result.get(header + ".name");
        if ((st = result.get(header + ".validfrom")) != null) {
            info.validFrom = HBCIUtils.string2DateISO(st);
        }
        if ((st = result.get(header + ".validuntil")) != null) {
            info.validUntil = HBCIUtils.string2DateISO(st);
        }

        ((GVRCardList) getJobResult()).addEntry(info);
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
