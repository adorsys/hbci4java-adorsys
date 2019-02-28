package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Implementierung des HBCI-Jobs fuer die Löschung einer SEPA-Terminsamellüberweisung.
 */
public class GVTermMultiUebSEPADel extends AbstractHBCIJob {

    public GVTermMultiUebSEPADel(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());
    }

    public GVTermMultiUebSEPADel(HBCIPassportInternal passport, String name) {
        super(passport, name, new HBCIJobResultImpl(passport));

        addConstraint("src.bic", "My.bic", null);
        addConstraint("src.iban", "My.iban", null);

        if (this.canNationalAcc(passport)) // nationale Bankverbindung mitschicken, wenn erlaubt
        {
            addConstraint("src.country", "My.KIK.country", "");
            addConstraint("src.blz", "My.KIK.blz", "");
            addConstraint("src.number", "My.number", "");
            addConstraint("src.subnumber", "My.subnumber", "");
        }

        addConstraint("orderid", "orderid", null);
    }

    public static String getLowlevelName() {
        return "TermSammelUebSEPADel";
    }
}
