/*  $Id: GVSEPAInfo.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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
import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.Map;

@Slf4j
public class GVSEPAInfo extends AbstractHBCIJob {

    public GVSEPAInfo(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new HBCIJobResultImpl(passport));
    }

    public static String getLowlevelName() {
        return "SEPAInfo";
    }

    public void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        Map<String, String> result = msgstatus.getData();
        Map<String, String> upd = passport.getUPD();

        for (int i = 0; ; i++) {
            String subheader = HBCIUtils.withCounter(header + ".Acc", i);
            String cansepa = result.get(subheader + ".sepa");
            if (cansepa == null) {
                // kein weiteres konto im antwortsegment
                break;
            }
            if (cansepa.equals("N")) {
                // dieses konto kann kein sepa
                continue;
            }

            // sepa-konto-informationen gefunden
            String iban = result.get(subheader + ".iban");
            String bic = result.get(subheader + ".bic");

            // normale konto-informationen extrahieren, um dieses konto
            // in den upd suchen zu koennen
            String country = result.get(subheader + ".KIK.country");
            String blz = result.get(subheader + ".KIK.blz");
            String number = result.get(subheader + ".number");

            log.debug("found BIC/IBAN = " + bic + "/" + iban + " for account " + country + "/" + blz + "/" + number);

            // konto in den UPD suchen und UPD-Informationen aktualisieren
            for (int j = 0; ; j++) {
                String temp_header = HBCIUtils.withCounter("KInfo", j);
                String temp_number = upd.get(temp_header + ".KTV.number");
                if (temp_number == null) {
                    // kein weiteres konto in den UPD
                    break;
                }
                String temp_country = upd.get(temp_header + ".KTV.KIK.country");
                String temp_blz = upd.get(temp_header + ".KTV.KIK.blz");

                if (temp_country.equals(country) &&
                    temp_blz.equals(blz) &&
                    temp_number.equals(number)) {
                    upd.put(temp_header + ".KTV.iban", iban);
                    upd.put(temp_header + ".KTV.bic", bic);
                }
            }
        }
    }
}
