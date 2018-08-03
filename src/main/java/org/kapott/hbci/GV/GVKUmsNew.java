/*  $Id: GVKUmsNew.java,v 1.1 2011/05/04 22:37:52 willuhn Exp $

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
import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Implementierung des Geschaeftsvorfalls zum Abruf von neuen Umsaetzen (HKKAN).
 */
@Slf4j
public final class GVKUmsNew extends GVKUmsAll {

    public GVKUmsNew(HBCIPassportInternal passport) {
        super(passport, getLowlevelName());

        boolean sepa = false;
        try {
            // Siehe auch GVKontoauszug/HKEKA. Die einzige Aenderung war die Umstellung
            // der Bankverbindungsart von ktv auf kti (wegen IBAN-Support).
            // Bei HKKAN ist das ab Segment-Version 7 der Fall.
            sepa = this.getSegVersion() >= 7;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // Dennoch kann es sein, dass die nationale Bankverbindung auch bei der
        // SEPA-Variante noch mitgeschickt wird, wenn die Bank das zulaesst.
        // (Es scheint auch Banken zu geben, die das in dem Fall nicht nur
        // zulassen sondern erwarten).
        boolean nat = this.canNationalAcc(passport);

        if (sepa) {
            addConstraint("my.bic", "KTV.bic", null);
            addConstraint("my.iban", "KTV.iban", null);
        }

        if (nat || !sepa) {
            addConstraint("my.country", "KTV.KIK.country", "DE");
            addConstraint("my.blz", "KTV.KIK.blz", null);
            addConstraint("my.number", "KTV.number", null);
            addConstraint("my.subnumber", "KTV.subnumber", "");
        }

        addConstraint("my.curr", "curr", "EUR");
        addConstraint("maxentries", "maxentries", "");
        addConstraint("dummyall", "allaccounts", "N");
    }

    public static String getLowlevelName() {
        return "KUmsNew";
    }
}
