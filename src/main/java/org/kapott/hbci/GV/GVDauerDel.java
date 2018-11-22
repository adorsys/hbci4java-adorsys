/*  $Id: GVDauerDel.java,v 1.1 2011/05/04 22:37:54 willuhn Exp $

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

import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;

import java.util.Map;

public final class GVDauerDel extends AbstractHBCIJob {

    public GVDauerDel(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new HBCIJobResultImpl(passport));

        addConstraint("src.number", "My.number", "");
        addConstraint("src.subnumber", "My.subnumber", "");
        addConstraint("dst.blz", "Other.KIK.blz", "");
        addConstraint("dst.number", "Other.number", "");
        addConstraint("dst.subnumber", "Other.subnumber", "");
        addConstraint("btg.value", "BTG.value", "");
        addConstraint("btg.curr", "BTG.curr", "");
        addConstraint("name", "name", "");
        addConstraint("firstdate", "DauerDetails.firstdate", "");
        addConstraint("timeunit", "DauerDetails.timeunit", "");
        addConstraint("turnus", "DauerDetails.turnus", "");
        addConstraint("execday", "DauerDetails.execday", "");

        addConstraint("src.blz", "My.KIK.blz", null);
        addConstraint("src.country", "My.KIK.country", "DE");
        addConstraint("dst.country", "Other.KIK.country", "DE");
        addConstraint("name2", "name2", "");
        addConstraint("key", "key", "52");
        addConstraint("date", "date", "");
        addConstraint("orderid", "orderid", "");
        addConstraint("lastdate", "DauerDetails.lastdate", "");

        // TODO: daten fuer aussetzung fehlen
        // TODO: addkey fehlt

        // Properties parameters=getJobRestrictions();
        // int        maxusage=Integer.parseInt(parameters.getProperty("maxusage"));
        //
        // TODO this is a dirty hack because we need the "maxusage" job restriction
        // from GVDauerNew here, but we have no chance to access this parameter
        // from here. The design changes of the next HBCI4Java version may solve
        // this problem.
        int maxusage = 99;

        for (int i = 0; i < maxusage; i++) {
            String name = HBCIUtils.withCounter("usage", i);
            addConstraint(name, "usage." + name, "");
        }
    }

    public static String getLowlevelName() {
        return "DauerDel";
    }

    public void setParam(String paramName, String value) {
        if (paramName.equals("date")) {
            Map<String, String> res = getJobRestrictions();
            String st_cantermdel = res.get("cantermdel");

            if (st_cantermdel != null && st_cantermdel.equals("N")) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_SCHEDDELSTANDORDUNAVAIL");
                throw new InvalidUserDataException(msg);
            }

            // TODO: minpretime und maxpretime auswerten
        } else if (paramName.equals("orderid")) {
//TODO            HashMap<String, String> p = (HashMap<String, String>) passport.getPersistentData("dauer_" + value);
//            if (p != null && p.size() != 0) {
//                p.keySet().forEach(key -> {
//                    if (!key.equals("date") &&
//                        !key.startsWith("Aussetzung.")) {
//                        setLowlevelParam(getName() + "." + key, p.get(key));
//                    }
//                });
//            }
        }

        super.setParam(paramName, value);
    }
}
