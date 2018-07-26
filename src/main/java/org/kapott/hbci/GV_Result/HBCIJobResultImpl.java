/*  $Id: HBCIJobResultImpl.java,v 1.1 2011/05/04 22:37:48 willuhn Exp $

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

package org.kapott.hbci.GV_Result;

import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIRetVal;
import org.kapott.hbci.status.HBCIStatus;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class HBCIJobResultImpl implements Serializable, HBCIJobResult {

    public HBCIStatus jobStatus;
    public HBCIStatus globStatus;
    private HBCIPassportInternal passport;
    private HashMap<String, String> resultData;

    public HBCIJobResultImpl(HBCIPassportInternal passport) {
        this.passport = passport;
        resultData = new HashMap<>();
        jobStatus = new HBCIStatus();
        globStatus = new HBCIStatus();
    }

    public void storeResult(String key, String value) {
        if (value != null)
            resultData.put(key, value);
    }

    public int getRetNumber() {
        return jobStatus.getRetVals().size();
    }

    public HBCIRetVal getRetVal(int idx) {
        return jobStatus.getRetVals().get(idx);
    }

    public boolean isOK() {
        return globStatus.getStatusCode() != HBCIStatus.STATUS_ERR &&
                jobStatus.getStatusCode() != HBCIStatus.STATUS_ERR &&
                (globStatus.getStatusCode() != HBCIStatus.STATUS_UNKNOWN ||
                        jobStatus.getStatusCode() != HBCIStatus.STATUS_UNKNOWN);
    }

    public String getDialogId() {
        return resultData.get("basic.dialogid");
    }

    public String getMsgNum() {
        return resultData.get("basic.msgnum");
    }

    public String getSegNum() {
        return resultData.get("basic.segnum");
    }

    public String getJobId() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        return format.format(new Date()) + "/" + getDialogId() + "/" + getMsgNum() + "/" + getSegNum();
    }

    public HashMap<String, String> getResultData() {
        return resultData;
    }

    public HBCIStatus getGlobStatus() {
        return globStatus;
    }

    public HBCIStatus getJobStatus() {
        return jobStatus;
    }

    @Override
    public HBCIPassportInternal getPassport() {
        return passport;
    }

    public String toString() {
        StringBuffer ret = new StringBuffer();
        Object[] a = resultData.keySet().toArray();

        Arrays.sort(a);
        for (int i = 0; i < a.length; i++) {
            String key = (String) (a[i]);
            ret.append(key).append(" = ").append(resultData.get(key)).append(System.getProperty("line.separator"));
        }

        return ret.toString().trim();
    }

}