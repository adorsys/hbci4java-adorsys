/*  $Id: GVStatus.java,v 1.1 2011/05/04 22:37:52 willuhn Exp $

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

import org.kapott.hbci.GV_Result.GVRStatus;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.status.HBCIRetVal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public final class GVStatus extends AbstractHBCIJob {

    public GVStatus(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRStatus(passport));

        addConstraint("startdate", "startdate", "");
        addConstraint("enddate", "enddate", "");
        addConstraint("maxentries", "maxentries", "");

        addConstraint("jobid", null, "");
    }

    public static String getLowlevelName() {
        return "Status";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRStatus.Entry entry = new GVRStatus.Entry();

        entry.dialogid = result.get(header + ".MsgRef.dialogid");
        entry.msgnum = result.get(header + ".MsgRef.msgnum");
        entry.retval = new HBCIRetVal(result,
            header + ".RetVal",
            result.get(header + ".segref"));
        entry.retval.element = null;

        String date = result.get(header + ".date");
        String time = result.get(header + ".time");
        entry.timestamp = HBCIUtils.strings2DateTimeISO(date, time);

        ((GVRStatus) jobResult).addEntry(entry);
    }

    public void setParam(String paramName, String value) {
        if (paramName.equals("jobid")) {
            try {
                Date dateOfJob = new SimpleDateFormat("yyyyMMdd").parse(value.substring(0, value.indexOf("/")));
                setParam("startdate", dateOfJob);
                setParam("enddate", dateOfJob);
            } catch (Exception e) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_CANTEXTRACTDATE", value);
                throw new InvalidUserDataException(msg, e);
            }
        } else {
            super.setParam(paramName, value);
        }
    }
}
