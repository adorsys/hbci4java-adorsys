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
import org.kapott.hbci.manager.HHDVersion;
import org.kapott.hbci.manager.KnownReturncode;
import org.kapott.hbci.manager.KnownTANProcess;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

/**
 * @author stefan.palme
 */
@Slf4j
public class GVTAN2Step extends AbstractHBCIJob {

    private AbstractHBCIJob scaJob;
    private AbstractHBCIJob redo;
    private KnownTANProcess process = null;

    public GVTAN2Step(HBCIPassportInternal passport, AbstractHBCIJob scaJob) {
        super(passport, getLowlevelName(), new GVRSaldoReq(passport));
        this.scaJob = scaJob;

        addConstraint("process", "process", null);
        addConstraint("ordersegcode", "ordersegcode", "");
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

    /**
     * Speichert den Prozess-Schritt des HKTAN.
     *
     * @param p der Prozess-Schritt.
     */
    public void setProcess(KnownTANProcess p) {
        this.process = p;
        this.setParam("process", p.getCode());
    }

    @Override
    public void setParam(String paramName, String value) {
        if (paramName.equals("orderhash")) {
            value = "B" + value;
        }
        super.setParam(paramName, value);
    }

    @Override
    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        String segCode = result.get(header + ".SegHead.code");
        log.debug("found HKTAN response with segcode " + segCode);

        ///////////////////////////////////////////////////////////////////////
        // Die folgenden Sonderbehandlungen sind nur bei Prozess-Variante 2 in Schritt 2 noetig,
        // weil wir dort ein Response auf einen GV erhalten, wir selbst aber gar nicht der GV sind sondern das HKTAN
        // Step2
        if (this.process == KnownTANProcess.PROCESS2_STEP2 && this.scaJob != null) {
            // Pruefen, ob die Bank eventuell ein 3040 gesendet hat - sie also noch weitere Daten braucht.
            // Das 3040 bezieht sich dann aber nicht auf unser HKTAN sondern auf den eigentlichen GV
            // In dem Fall muessen wir dem eigentlichen Task mitteilen, dass er erneut ausgefuehrt werden soll.
            if (this.toInsCode(this.getHBCICode()).equals(segCode) && KnownReturncode.W3040.searchReturnValue(msgstatus.segStatus.getWarnings()) != null) {
                log.debug("found status code 3040, need to repeat task " + this.scaJob.getHBCICode());
                this.redo = this.scaJob;
            }

            // Das ist das Response auf den eigentlichen GV - an den Task durchreichen
            // Muessen wir extra pruefen, weil das hier auch das HITAN sein koennte. Das schauen wir aber nicht an
            if (this.toInsCode(this.scaJob.getHBCICode()).equals(segCode)) {
                log.debug("this is a response segment for the original task - storing results in the original job");
                this.scaJob.extractResults(msgstatus, header, idx);
            }

            // Wir haben hier nichts weiter zu tun
            return;
        }

        String orderref = result.get(header + ".orderref");

        // willuhn 2011-05-27 Challenge HHDuc aus dem Reponse holen und im Passport zwischenspeichern
        String hhdUc = result.get(header + ".challenge_hhd_uc");
        String challenge = result.get(header + ".challenge");

        HHDVersion hhd = HHDVersion.find(passport.getCurrentSecMechInfo());
        passport.getCallback().tanChallengeCallback(orderref, challenge, hhdUc, hhd.getType());
    }

    /**
     * Liefert zu einem HBCI-Code vom Client den zugehoerigen HBCI-Code des Instituts.
     *
     * @param hbciCode der HBCI-Code des Clients.
     * @return der HBCI-Code des Instituts.
     */
    private String toInsCode(String hbciCode) {
        return new StringBuffer(hbciCode).replace(1, 2, "I").toString();
    }

    @Override
    public AbstractHBCIJob redo() {
        return this.redo;
    }
}
