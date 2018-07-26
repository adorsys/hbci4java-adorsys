/*  $Id: GVInfoOrder.java,v 1.1 2011/05/04 22:37:54 willuhn Exp $

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


import org.kapott.hbci.GV_Result.GVRInfoOrder;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

public final class GVInfoOrder extends AbstractHBCIJob {

    public GVInfoOrder(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRInfoOrder(passport));

        addConstraint("code", "InfoCodes.code", null);

        addConstraint("name", "Address.name1", "");
        addConstraint("name2", "Address.name2", "");
        addConstraint("street", "Address.street_pf", "");
        addConstraint("ort", "Address.ort", "");
        addConstraint("plz", "Address.plz_ort", "");
        addConstraint("plz", "Address.plz", "");
        addConstraint("country", "Address.country", "");
        addConstraint("tel", "Address.tel", "");
        addConstraint("fax", "Address.fax", "");
        addConstraint("email", "Address.email", "");

        // TODO: country fehlt

        for (int i = 1; i < 10; i++) {
            addConstraint(HBCIUtils.withCounter("code", i),
                    HBCIUtils.withCounter("InfoCodes.code", i),
                    "");
        }
    }

    public static String getLowlevelName() {
        return "InfoDetails";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        for (int i = 0; ; i++) {
            String header2 = HBCIUtils.withCounter(header + ".Info", i);

            if (result.get(header2 + ".code") == null)
                break;

            GVRInfoOrder.Info entry = new GVRInfoOrder.Info();

            entry.code = result.get(header2 + ".code");
            entry.msg = result.get(header2 + ".msg");

            ((GVRInfoOrder) (jobResult)).addEntry(entry);
        }
    }
}
