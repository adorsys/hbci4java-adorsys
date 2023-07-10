/**
 * Geschäftsvorfall SEPA Basislastschrift. Diese ist in pain.008.003.02.xsd spezifiziert.
 *
 * @author Jan Thielemann
 */

package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.AbstractGVRLastSEPA;
import org.kapott.hbci.GV_Result.GVRLastCOR1SEPA;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;

/**
 * Implementierung des HBCI-Jobs fuer die SEPA-COR1-Multi-Lastschrift.
 */
public class GVMultiLastCOR1SEPA extends GVLastCOR1SEPA {
    public GVMultiLastCOR1SEPA(HBCIPassportInternal passport, SepaVersion sepaVersion) {
        this(passport, getLowlevelName(), sepaVersion, new GVRLastCOR1SEPA(passport));
    }

    public GVMultiLastCOR1SEPA(HBCIPassportInternal passport, String lowlevelName, SepaVersion sepaVersion, AbstractGVRLastSEPA result) {
        super(passport, lowlevelName, sepaVersion, result);

        addConstraint("batchbook", "sepa.batchbook", "");
        addConstraint("Total.value", "Total.value", null);
        addConstraint("Total.curr", "Total.curr", null);
    }

    /**
     * Liefert den Lowlevel-Jobnamen.
     *
     * @return der Lowlevel-Jobname.
     */
    public static String getLowlevelName() {
        return "SammelLastCOR1SEPA";
    }

    @Override
    protected void createPainXml() {
        super.createPainXml();
        setParam("Total", SepaUtil.sumBtgValueObject(getPainParams()));
    }
}
