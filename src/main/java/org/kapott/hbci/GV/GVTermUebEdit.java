/*  $Id: GVTermUebEdit.java,v 1.1 2011/05/04 22:37:54 willuhn Exp $

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

import org.kapott.hbci.GV_Result.GVRTermUebEdit;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public final class GVTermUebEdit extends AbstractHBCIJob {

    public GVTermUebEdit(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRTermUebEdit(passport));

        addConstraint("src.country", "My.KIK.country", "DE");
        addConstraint("src.blz", "My.KIK.blz", null);
        addConstraint("src.number", "My.number", null);
        addConstraint("src.subnumber", "My.subnumber", "");
        addConstraint("dst.country", "Other.KIK.country", "DE");
        addConstraint("dst.blz", "Other.KIK.blz", null);
        addConstraint("dst.number", "Other.number", "");
        addConstraint("dst.subnumber", "Other.subnumber", "");
        addConstraint("btg.value", "BTG.value", null);
        addConstraint("btg.curr", "BTG.curr", null);
        addConstraint("name", "name", null);
        addConstraint("date", "date", null);
        addConstraint("orderid", "id", null);

        addConstraint("name2", "name2", "");
        addConstraint("key", "key", "51");

        HashMap<String, String> parameters = getJobRestrictions();
        int maxusage = Integer.parseInt(parameters.get("maxusage"));

        for (int i = 0; i < maxusage; i++) {
            String name = HBCIUtils.withCounter("usage", i);
            addConstraint(name, "usage." + name, "");
        }
    }

    public static String getLowlevelName() {
        return "TermUebEdit";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        String orderid = result.get(header + ".orderid");

        ((GVRTermUebEdit) (jobResult)).setOrderId(orderid);
        ((GVRTermUebEdit) (jobResult)).setOrderIdOld(result.get(header + ".orderidold"));

        if (orderid != null && orderid.length() != 0) {
            Properties p = getLowlevelParams();
            Properties p2 = new Properties();

            for (Enumeration e = p.propertyNames(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                if (!key.endsWith(".id")) {
                    p2.setProperty(key.substring(key.indexOf(".") + 1),
                            p.getProperty(key));
                }
            }

            passport.setPersistentData("termueb_" + orderid, p2);
        }
    }

    public void setParam(String paramName, String value) {
        super.setParam(paramName, value);

        if (paramName.equals("orderid")) {
            Properties p = (Properties) passport.getPersistentData("termueb_" + value);
            if (p == null) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_NOSUCHSCHEDTRANS", value);
                throw new InvalidUserDataException(msg);
            }

            for (Enumeration e = p.propertyNames(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                String key2 = getName() + "." + key;

                if (getLowlevelParams().getProperty(key2) == null) {
                    setLowlevelParam(key2,
                            p.getProperty(key));
                }
            }
        }
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("src");
        checkAccountCRC("dst");
    }
}
