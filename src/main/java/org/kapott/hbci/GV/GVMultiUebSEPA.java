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

import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Job-Implementierung fuer SEPA-Multi-Ueberweisungen.
 */
public class GVMultiUebSEPA extends GVUebSEPA {

    public GVMultiUebSEPA(HBCIPassportInternal passport) {
        this(passport, getLowlevelName(), null);
    }

    public GVMultiUebSEPA(HBCIPassportInternal passport, String name, String messageID) {
        super(passport, name, messageID);

        addConstraint("batchbook", "sepa.batchbook", "");
        addConstraint("Total.value", "Total.value", null);
        addConstraint("Total.curr", "Total.curr", null);
    }

    /**
     * Liefert den Lowlevel-Namen des Jobs.
     *
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName() {
        return "SammelUebSEPA";
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getPainJobName()
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
            return getLowlevelParam(getName()+".Total.value");
        }
        else if (path.equals("sepa.btg.curr")) {
            return getLowlevelParam(getName()+".Total.curr");
        }
        return null;
    }
}
