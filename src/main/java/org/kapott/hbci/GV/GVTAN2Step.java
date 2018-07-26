/*  $Id: GVTAN2Step.java,v 1.6 2011/05/27 10:28:38 willuhn Exp $

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


import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV_Result.GVRSaldoReq;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

/**
 * @author stefan.palme
 */
@Slf4j
public class GVTAN2Step extends AbstractHBCIJob {

    private GVTAN2Step otherTAN2StepTask;
    private AbstractHBCIJob origTask;

    public GVTAN2Step(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRSaldoReq(passport));

        addConstraint("process", "process", null);
        addConstraint("orderhash", "orderhash", "");
        addConstraint("orderref", "orderref", "");
        addConstraint("listidx", "listidx", "");
        addConstraint("notlasttan", "notlasttan", "N");
        addConstraint("info", "info", "");

        addConstraint("storno", "storno", "");
        addConstraint("challengeklass", "challengeklass", "");
        addConstraint("ChallengeKlassParam1", "ChallengeKlassParams.param1", "");
        addConstraint("ChallengeKlassParam2", "ChallengeKlassParams.param2", "");
        addConstraint("ChallengeKlassParam3", "ChallengeKlassParams.param3", "");
        addConstraint("ChallengeKlassParam4", "ChallengeKlassParams.param4", "");
        addConstraint("ChallengeKlassParam5", "ChallengeKlassParams.param5", "");
        addConstraint("ChallengeKlassParam6", "ChallengeKlassParams.param6", "");
        addConstraint("ChallengeKlassParam7", "ChallengeKlassParams.param7", "");
        addConstraint("ChallengeKlassParam8", "ChallengeKlassParams.param8", "");
        addConstraint("ChallengeKlassParam9", "ChallengeKlassParams.param9", "");

        addConstraint("tanmedia", "tanmedia", "");

        addConstraint("ordersegcode", "ordersegcode", "");

        addConstraint("orderaccount.bic", "OrderAccount.bic", null);
        addConstraint("orderaccount.iban", "OrderAccount.iban", null);
        addConstraint("orderaccount.number", "OrderAccount.number", null);
        addConstraint("orderaccount.subnumber", "OrderAccount.subnumber", "");
        addConstraint("orderaccount.blz", "OrderAccount.KIK.blz", null);
        addConstraint("orderaccount.country", "OrderAccount.KIK.country", "DE");

        // willuhn 2011-05-17 wird noch nicht genutzt
        // addConstraint("smsaccount.number","SMSAccount.number",null);
        // addConstraint("smsaccount.subnumber","SMSAccount.subnumber","");
        // addConstraint("smsaccount.blz","SMSAccount.KIK.blz",null);
        // addConstr≤aint("smsaccount.country","SMSAccount.KIK.country","DE");
    }

    public static String getLowlevelName() {
        return "TAN2Step";
    }

    public void setParam(String paramName, String value) {
        if (paramName.equals("orderhash")) {
            value = "B" + value;
        }
        super.setParam(paramName, value);
    }

    public void storeOtherTAN2StepTask(GVTAN2Step other) {
        this.otherTAN2StepTask = other;
    }

    public void storeOriginalTask(AbstractHBCIJob task) {
        this.origTask = task;
    }

    protected void saveReturnValues(HBCIMsgStatus status, int sref) {
        super.saveReturnValues(status, sref);

        if (origTask != null) {
            int orig_segnum = Integer.parseInt(origTask.getJobResult().getSegNum());
            log.debug("storing return values in orig task (segnum=" + orig_segnum + ")");
            origTask.saveReturnValues(status, orig_segnum);
        }
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        String segcode = result.get(header + ".SegHead.code");
        log.debug("found HKTAN response with segcode " + segcode);

        if (origTask != null && new StringBuffer(origTask.getHBCICode()).replace(1, 2, "I").toString().equals(segcode)) {
            // das ist für PV#2, wenn nach dem nachträglichen versenden der TAN das
            // antwortsegment des jobs aus der vorherigen Nachricht zurückommt
            log.debug("this is a response segment for the original task - storing results in the original job");
            origTask.extractResults(msgstatus, header, idx);
        } else {
            log.debug("this is a \"real\" HKTAN response - analyzing HITAN data");

            String challenge = result.get(header + ".challenge");
            if (challenge != null) {
                log.debug("found challenge '" + challenge + "' in HITAN - saving it temporarily in passport");
                // das ist für PV#1 (die antwort auf das einreichen des auftrags-hashs) oder 
                // für PV#2 (die antwort auf das einreichen des auftrages)
                // in jedem fall muss mit der nächsten nachricht die TAN übertragen werden
                passport.setPersistentData("pintan_challenge", challenge);

                // External-ID des originalen Jobs durchreichen
                passport.setPersistentData("externalid", this.getExternalId());

                // TODO: es muss hier evtl. noch überprüft werden, ob
                // der zurückgegebene auftragshashwert mit dem ursprünglich versandten
                // übereinstimmt
                // für pv#1 gilt: hitan_orderhash == sent_orderhash (from previous hktan)
                // für pv#2 gilt: hitan_orderhash == orderhash(gv from previous GV segment)

                // TODO: hier noch die optionale DEG ChallengeValidity bereitstellen
            }

            // willuhn 2011-05-27 Challenge HHDuc aus dem Reponse holen und im Passport zwischenspeichern
            String hhdUc = result.get(header + ".challenge_hhd_uc");
            if (hhdUc != null) {
                log.debug("found Challenge HHDuc '" + hhdUc + "' in HITAN - saving it temporarily in passport");
                passport.setPersistentData("pintan_challenge_hhd_uc", hhdUc);
            }

            String orderref = result.get(header + ".orderref");
            if (orderref != null) {
                // orderref ist nur für PV#2 relevant
                log.debug("found orderref '" + orderref + "' in HITAN");
                if (otherTAN2StepTask != null) {
                    // hier sind wir ganz sicher in PV#2. das hier ist die antwort auf das
                    // erste HKTAN (welches mit dem eigentlichen auftrag verschickt wird)
                    // die orderref muss im zweiten HKTAN-job gespeichert werden, weil in
                    // dieser zweiten nachricht dann die TAN mit übertragen werden muss
                    log.debug("storing it in following HKTAN task");
                    otherTAN2StepTask.setParam("orderref", orderref);
                } else {
                    log.debug("no other HKTAN task known - ignoring orderref");
                }
            }

            passport.getCallback().tanCallback(passport, otherTAN2StepTask);
        }
    }
}
