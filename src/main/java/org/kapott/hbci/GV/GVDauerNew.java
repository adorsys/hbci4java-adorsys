/*  $Id: GVDauerNew.java,v 1.1 2011/05/04 22:37:52 willuhn Exp $

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

import org.kapott.hbci.GV_Result.GVRDauerNew;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public final class GVDauerNew extends AbstractHBCIJob {

    public GVDauerNew(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRDauerNew(passport));

        addConstraint("src.number", "My.number", null);
        addConstraint("src.subnumber", "My.subnumber", "");
        addConstraint("dst.blz", "Other.KIK.blz", null);
        addConstraint("dst.number", "Other.number", null);
        addConstraint("dst.subnumber", "Other.subnumber", "");
        addConstraint("btg.value", "BTG.value", null);
        addConstraint("btg.curr", "BTG.curr", null);
        addConstraint("name", "name", null);
        addConstraint("firstdate", "DauerDetails.firstdate", null);
        addConstraint("timeunit", "DauerDetails.timeunit", null);
        addConstraint("turnus", "DauerDetails.turnus", null);
        addConstraint("execday", "DauerDetails.execday", null);

        addConstraint("src.blz", "My.KIK.blz", null);
        addConstraint("src.country", "My.KIK.country", "DE");
        addConstraint("dst.country", "Other.KIK.country", "DE");
        addConstraint("name2", "name2", "");
        addConstraint("lastdate", "DauerDetails.lastdate", "");
        addConstraint("key", "key", "52");

        // TODO: aussetzung fehlt
        // TODO: addkey fehlt

        Map<String, String> parameters = getJobRestrictions();
        int maxusage = Integer.parseInt(parameters.get("maxusage"));

        for (int i = 0; i < maxusage; i++) {
            String name = HBCIUtils.withCounter("usage", i);
            addConstraint(name, "usage." + name, "");
        }
    }

    public static String getLowlevelName() {
        return "DauerNew";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        Map<String, String> result = msgstatus.getData();
        String orderid = result.get(header + ".orderid");
        ((GVRDauerNew) (jobResult)).setOrderId(orderid);

        if (orderid != null && orderid.length() != 0) {
            HashMap<String, String> p2 = new HashMap<>();
            getLowlevelParams().forEach((key, value) ->
                p2.put(key.substring(key.indexOf(".") + 1), value));
        }
    }

    public void setParam(String paramName, String value) {
        Map<String, String> res = getJobRestrictions();

        if (paramName.equals("timeunit")) {
            if (!(value.equals("W") || value.equals("M"))) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_INV_TIMEUNIT", value);
                throw new InvalidUserDataException(msg);
            }
        } else if (paramName.equals("turnus")) {
            String timeunit = getLowlevelParams().get(getName() + ".DauerDetails.timeunit");

            if (timeunit != null) {
                if (timeunit.equals("W")) {
                    String st = res.get("turnusweeks");

                    if (st != null) {
                        String value2 = new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2, st)) {
                            String msg = HBCIUtils.getLocMsg("EXCMSG_INV_TURNUS", value);
                            throw new InvalidUserDataException(msg);
                        }
                    }
                } else if (timeunit.equals("M")) {
                    String st = res.get("turnusmonths");

                    if (st != null) {
                        String value2 = new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2, st)) {
                            String msg = HBCIUtils.getLocMsg("EXCMSG_INV_TURNUS", value);
                            throw new InvalidUserDataException(msg);
                        }
                    }
                }
            }
        } else if (paramName.equals("execday")) {
            String timeunit = getLowlevelParams().get(getName() + ".DauerDetails.timeunit");

            if (timeunit != null) {
                if (timeunit.equals("W")) {
                    String st = res.get("daysperweek");

                    if (st != null && !st.equals("0") && st.indexOf(value) == -1) {
                        String msg = HBCIUtils.getLocMsg("EXCMSG_INV_EXECDAY", value);
                        throw new InvalidUserDataException(msg);
                    }
                } else if (timeunit.equals("M")) {
                    String st = res.get("dayspermonth");

                    if (st != null) {
                        String value2 = new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2, st)) {
                            String msg = HBCIUtils.getLocMsg("EXCMSG_INV_EXECDAY", value);
                            throw new InvalidUserDataException(msg);
                        }
                    }
                }
            }
        } else if (paramName.equals("key")) {
            boolean atLeastOne = false;
            boolean found = false;

            for (int i = 0; ; i++) {
                String st = res.get(HBCIUtils.withCounter("textkey", i));

                if (st == null)
                    break;

                atLeastOne = true;

                if (st.equals(value)) {
                    found = true;
                    break;
                }
            }

            if (atLeastOne && !found) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_INV_KEY", value);
                throw new InvalidUserDataException(msg);
            }
        }

        super.setParam(paramName, value);
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("src");
        checkAccountCRC("dst");
    }
}
