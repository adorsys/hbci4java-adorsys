/*  $Id: GVDauerEdit.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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

import org.kapott.hbci.GV_Result.GVRDauerEdit;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public final class GVDauerEdit extends AbstractHBCIJob {

    public GVDauerEdit(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRDauerEdit(passport));

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
        addConstraint("orderid", "orderid", null);

        addConstraint("src.blz", "My.KIK.blz", null);
        addConstraint("src.country", "My.KIK.country", "DE");
        addConstraint("dst.country", "Other.KIK.country", "DE");
        addConstraint("name2", "name2", "");
        addConstraint("key", "key", "52");
        addConstraint("date", "date", "");
        addConstraint("lastdate", "DauerDetails.lastdate", "");

        // TODO: aussetzung fehlt
        // TODO: addkey fehlt

        HashMap<String, String> parameters = getJobRestrictions();
        int maxusage = Integer.parseInt(parameters.get("maxusage"));

        for (int i = 0; i < maxusage; i++) {
            String name = HBCIUtils.withCounter("usage", i);
            addConstraint(name, "usage." + name, "");
        }
    }

    public static String getLowlevelName() {
        return "DauerEdit";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        String orderid = result.get(header + ".orderid");

        ((GVRDauerEdit) (jobResult)).setOrderId(orderid);
        ((GVRDauerEdit) (jobResult)).setOrderIdOld(result.get(header + ".orderidold"));

        if (orderid != null && orderid.length() != 0) {
            Properties p = getLowlevelParams();
            Properties p2 = new Properties();

            for (Enumeration e = p.propertyNames(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                if (!key.endsWith(".orderid")) {
                    p2.setProperty(key.substring(key.indexOf(".") + 1),
                            p.getProperty(key));
                }
            }

            passport.setPersistentData("dauer_" + orderid, p2);
        }
    }

    public void setParam(String paramName, String value) {
        HashMap<String, String> res = getJobRestrictions();

        if (paramName.equals("date")) {
            String st = res.get("numtermchanges");
            if (st != null && Integer.parseInt(st) == 0) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_SCHEDMODSTANDORDUNAVAIL");
                throw new InvalidUserDataException(msg);
            }
            // TODO: numtermchanges richtig auswerten
        } else if (paramName.equals("timeunit")) {
            if (!(value.equals("W") || value.equals("M"))) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_INV_TIMEUNIT", value);
                throw new InvalidUserDataException(msg);
            }
        } else if (paramName.equals("turnus")) {
            String timeunit = getLowlevelParams().getProperty(getName() + ".DauerDetails.timeunit");

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
            String timeunit = getLowlevelParams().getProperty(getName() + ".DauerDetails.timeunit");

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
        } else if (paramName.equals("orderid")) {
            Properties p = (Properties) passport.getPersistentData("dauer_" + value);
            if (p != null) {
                for (Enumeration e = p.propertyNames(); e.hasMoreElements(); ) {
                    String key = (String) e.nextElement();
                    String key2 = getName() + "." + key;

                    if (!key.equals("date") &&
                            !key.startsWith("Aussetzung.") &&
                            getLowlevelParams().getProperty(key2) == null) {
                        setLowlevelParam(key2,
                                p.getProperty(key));
                    }
                }
            }
        }

        super.setParam(paramName, value);
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("src");
        checkAccountCRC("dst");
    }

    // TODO: this is disabled for now because the hbci specification is inconsistent concerning this
    /* public void verifyConstraints()
    {
        super.verifyConstraints();

        if (das_ist_eine_terminierte_ueberweisung) {
        Properties newParams=getParams();
        Properties oldParams=(Properties)getPassport().getPersistentData("dauer_"+newParams.getProperty(getName()+".orderid"));

        String st1;
        String st2;
        String key;

        Properties res=getJobRestrictions();

        if (res.getProperty("recktoeditable").equals("N")) {
            if ((st1=newParams.getProperty(getName()+".Other.KIK.country"))!=null &&
                (st2=oldParams.getProperty("Other.KIK.country"))!=null &&
                !st1.equals(st2) ||
                (st1=newParams.getProperty(getName()+".Other.KIK.blz"))!=null &&
                (st2=oldParams.getProperty("Other.KIK.blz"))!=null &&
                !st1.equals(st2) ||
                (st1=newParams.getProperty(getName()+".Other.number"))!=null &&
                (st2=oldParams.getProperty("Other.number"))!=null &&
                !st1.equals(st2)) {
                throw new HBCI_Exception("*** changing of destination account not allowed");
            }
        }

        if (res.getProperty("recnameeditable").equals("N")) {
            if ((st1=newParams.getProperty(getName()+".name"))!=null &&
                (st2=oldParams.getProperty("name"))!=null &&
                !st1.equals(st2) ||
                (st1=newParams.getProperty(getName()+".name2"))!=null &&
                (st2=oldParams.getProperty("name2"))!=null &&
                !st1.equals(st2)) {
                throw new HBCI_Exception("*** can not edit recipient name"); 
            }
        }

        if (res.getProperty("valueeditable").equals("N")) {
            if ((st1=newParams.getProperty(getName()+".BTG.value"))!=null &&
                (st2=oldParams.getProperty("BTG.value"))!=null &&
                Float.parseFloat(st1)!=Float.parseFloat(st2) ||
                (st1=newParams.getProperty(getName()+".BTG.curr"))!=null &&
                (st2=oldParams.getProperty("BTG.curr"))!=null &&
                !st1.equals(st2)) {
                throw new HBCI_Exception("*** changing value is not allowed");
            }
        }

        if (res.getProperty("keyeditable").equals("N")) {
            if ((st1=newParams.getProperty(getName()+".key"))!=null &&
                (st2=oldParams.getProperty("key"))!=null &&
                !st1.equals(st2) ||
                (st1=newParams.getProperty(getName()+".addkey"))!=null &&
                (st2=oldParams.getProperty("addkey"))!=null &&
                !st1.equals(st2)) {
                throw new HBCI_Exception("*** changing key is not allowed");
            }
        }

        if (res.getProperty("usageeditable").equals("N")) {
            boolean equal=true;

            for (int i=0;;i++) {
                String h=HBCIUtils.withCounter("usage.usage",i);
                String uo=oldParams.getProperty(h);
                String un=newParams.getProperty(getName()+"."+h);
                if (uo==null) {
                    if (un!=null) {
                        equal=false;
                    }
                    break;
                }
                if (un==null||!un.equals(uo)) {
                    equal=false;
                    break;
                }
            }

            if (!equal) {
                throw new HBCI_Exception("*** changing usage not allowed");
            }
        }

        if (res.getProperty("firstexeceditable").equals("N")) {
            if (!HBCIUtils.string2Date(newParams.getProperty((key=getName()+".DauerDetails.firstdate"))).equals(HBCIUtils.string2Date(oldParams.getProperty("DauerDetails.firstdate")))) {
                throw new HBCI_Exception("*** changing firstdate not allowed");
            }
        }

        if (res.getProperty("timeuniteditable").equals("N")) {
            if (!newParams.getProperty((key=getName()+".DauerDetails.timeunit")).equals(oldParams.getProperty("DauerDetails.timeunit"))) {
                throw new HBCI_Exception("*** changing timeunit not allowed");
            }
        }

        if (res.getProperty("turnuseditable").equals("N")) {
            if (Integer.parseInt(newParams.getProperty((key=getName()+".DauerDetails.turnus")))!=Integer.parseInt(oldParams.getProperty("DauerDetails.turnus"))) {
                throw new HBCI_Exception("*** changing turnus not allowed");
            }
        }

        if (res.getProperty("execdayeditable").equals("N")) {
            if (Integer.parseInt(newParams.getProperty((key=getName()+".DauerDetails.execday")))!=Integer.parseInt(oldParams.getProperty("DauerDetails.execday"))) {
                throw new HBCI_Exception("*** changing execday not allowed");
            }
        }

        if (res.getProperty("lastexeceditable").equals("N")) {
            if ((st1=newParams.getProperty((key=getName()+".DauerDetails.lastdate")))!=null &&
                (st2=oldParams.getProperty("DauerDetails.lastdate"))!=null &&
                !HBCIUtils.string2Date(st1).equals(HBCIUtils.string2Date(st2))) {
                throw new HBCI_Exception("*** chaning lastdate not allowed");
            }
        }

        if (Integer.parseInt(res.getProperty("numtermchanges"))>1) {
            if (res.getProperty("recktoeditable").equals("J")) {
                if ((st1=newParams.getProperty(getName()+".Other.KIK.country"))!=null &&
                    (st2=oldParams.getProperty("Other.KIK.country"))!=null &&
                    st1.equals(st2) &&
                    (st1=newParams.getProperty(getName()+".Other.KIK.blz"))!=null &&
                    (st2=oldParams.getProperty("Other.KIK.blz"))!=null &&
                    st1.equals(st2) &&
                    (st1=newParams.getProperty(getName()+".Other.number"))!=null &&
                    (st2=oldParams.getProperty("Other.number"))!=null &&
                    st1.equals(st2)) {
                    newParams.setProperty(getName()+".Other.KIK.country","");
                    newParams.setProperty(getName()+".Other.KIK.blz","");
                    newParams.setProperty(getName()+".Other.number","");
                }
            }
            
            if (res.getProperty("recnameeditable").equals("J")) {
                if ((st1=newParams.getProperty(getName()+".name"))!=null &&
                    (st2=oldParams.getProperty("name"))!=null &&
                    st1.equals(st2) &&
                    (st1=newParams.getProperty(getName()+".name2"))!=null &&
                    (st2=oldParams.getProperty("name2"))!=null &&
                    st1.equals(st2)) {
                    newParams.setProperty(getName()+".name","");
                    newParams.setProperty(getName()+".name2","");
                }
            }
            
            if (res.getProperty("valueeditable").equals("J")) {
                if ((st1=newParams.getProperty(getName()+".BTG.value"))!=null &&
                    (st2=oldParams.getProperty("BTG.value"))!=null &&
                    Float.parseFloat(st1)==Float.parseFloat(st2) &&
                    (st1=newParams.getProperty(getName()+".BTG.curr"))!=null &&
                    (st2=oldParams.getProperty("BTG.curr"))!=null &&
                    st1.equals(st2)) {
                    newParams.setProperty(getName()+".BTG.value","");
                    newParams.setProperty(getName()+".BTG.curr","");
                }
            }
            
            if (res.getProperty("keyeditable").equals("J")) {
                if ((st1=newParams.getProperty(getName()+".key"))!=null &&
                    (st2=oldParams.getProperty("key"))!=null &&
                    st1.equals(st2) &&
                    (st1=newParams.getProperty(getName()+".addkey"))!=null &&
                    (st2=oldParams.getProperty("addkey"))!=null &&
                    st1.equals(st2)) {
                    newParams.setProperty(getName()+".key","");
                    newParams.setProperty(getName()+".addkey","");
                }
            }
            
            if (res.getProperty("usageeditable").equals("J")) {
                boolean equal=true;

                for (int i=0;;i++) {
                    String h=HBCIUtils.withCounter("usage.usage",i);
                    String uo=oldParams.getProperty(h);
                    String un=newParams.getProperty(getName()+"."+h);
                    if (uo==null) {
                        if (un!=null) {
                            equal=false;
                        }
                        break;
                    }
                    if (un==null||!un.equals(uo)) {
                        equal=false;
                        break;
                    }
                }

                if (equal) {
                    for (int i=0;;i++) {
                        String h=HBCIUtils.withCounter(getName()+".usage.usage",i);
                        if (newParams.getProperty(h)==null)
                            break;
                        newParams.setProperty(h,"");
                    }
                }
            }
            
            if (res.getProperty("firstexeceditable").equals("J")) {
                if (HBCIUtils.string2Date(newParams.getProperty((key=getName()+".DauerDetails.firstdate"))).equals(HBCIUtils.string2Date(oldParams.getProperty("DauerDetails.firstdate")))) {
                    newParams.setProperty(key,"");
                }
            }
            
            if (res.getProperty("timeuniteditable").equals("J")) {
                if (newParams.getProperty((key=getName()+".DauerDetails.timeunit")).equals(oldParams.getProperty("DauerDetails.timeunit"))) {
                    newParams.setProperty(key,"");
                }
            }
            
            if (res.getProperty("turnuseditable").equals("J")) {
                if (Integer.parseInt(newParams.getProperty((key=getName()+".DauerDetails.turnus")))==Integer.parseInt(oldParams.getProperty("DauerDetails.turnus"))) {
                    newParams.setProperty(key,"");
                }
            }
            
            if (res.getProperty("execdayeditable").equals("J")) {
                if (Integer.parseInt(newParams.getProperty((key=getName()+".DauerDetails.execday")))==Integer.parseInt(oldParams.getProperty("DauerDetails.execday"))) {
                    newParams.setProperty(key,"");
                }
            }
            
            if (res.getProperty("lastexeceditable").equals("J")) {
                if ((st1=newParams.getProperty((key=getName()+".DauerDetails.lastdate")))!=null &&
                    (st2=oldParams.getProperty("DauerDetails.lastdate"))!=null &&
                    HBCIUtils.string2Date(st1).equals(HBCIUtils.string2Date(st2))) {
                    newParams.setProperty(key,"");
                }
            }
        }
    } */
}
