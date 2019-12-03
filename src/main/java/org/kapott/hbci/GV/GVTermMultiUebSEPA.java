/*  $Id: GVUebSEPA.java,v 1.1 2011/05/04 22:37:54 willuhn Exp $

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

import org.kapott.hbci.GV_Result.GVRPayment;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

/**
 * Job-Implementierung fuer SEPA-Multi-Ueberweisungen.
 */
public class GVTermMultiUebSEPA extends GVUebSEPA {

    private static final SepaVersion DEFAULT = SepaVersion.PAIN_001_001_02;

    public GVTermMultiUebSEPA(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());
    }

    public GVTermMultiUebSEPA(HBCIPassportInternal passport, String name) {
        super(passport, name, new GVRPayment(passport));

        addConstraint("batchbook", "sepa.batchbook", "");
        addConstraint("Total.value", "Total.value", null);
        addConstraint("Total.curr", "Total.curr", null);
        addConstraint("date", "sepa.date", null);
    }

    /**
     * Liefert den Lowlevel-Namen des Jobs.
     *
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName() {
        return "TermSammelUebSEPA";
    }

    /**
     * @see AbstractSEPAGV#getPainJobName()
     */
    @Override
    public String getPainJobName() {
        return "UebSEPA";
    }

    @Override
    public void verifyConstraints() {
        setParam("Total", SepaUtil.sumBtgValueObject(painParams));

        super.verifyConstraints();
    }

    @Override
    public String getChallengeParam(String path) {
        if (path.equals("sepa.btg.value")) {
            return getLowlevelParam(getName() + ".Total.value");
        } else if (path.equals("sepa.btg.curr")) {
            return getLowlevelParam(getName() + ".Total.curr");
        }
        return null;
    }

    @Override
    protected SepaVersion getDefaultPainVersion() {
        return DEFAULT;
    }

    @Override
    protected SepaVersion.Type getPainType() {
        return SepaVersion.Type.PAIN_001;
    }

    @Override
    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        String orderid = result.get(header + ".orderid");

        ((GVRPayment) (jobResult)).setOrderId(orderid);
    }
}
