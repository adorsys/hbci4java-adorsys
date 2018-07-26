package org.kapott.hbci.GV;

import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Umsatzabfrage eines SEPA-Kontos
 */
public class GVKUmsZeitSEPA extends GVKUmsAll {
    public GVKUmsZeitSEPA(HBCIPassportInternal passport) {
        super(passport, getLowlevelName());

        addConstraint("my.bic", "KTV.bic", null);
        addConstraint("my.iban", "KTV.iban", null);
        addConstraint("startdate", "sepa.startdate", "");
        addConstraint("enddate", "sepa.enddate", "");
        addConstraint("maxentries", "maxentries", "");
        addConstraint("offset", "offset", "");
        addConstraint("all", "allaccounts", "N");
    }

    /**
     * Liefert den Lowlevel-Namen des Jobs.
     *
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName() {
        return "KUmsZeitSEPA";
    }
}
