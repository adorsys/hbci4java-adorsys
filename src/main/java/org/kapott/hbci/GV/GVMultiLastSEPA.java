/**
 * Geschäftsvorfall SEPA Basislastschrift. Diese ist in pain.008.003.02.xsd spezifiziert.
 *
 * @author Jan Thielemann
 */

package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.AbstractGVRLastSEPA;
import org.kapott.hbci.GV_Result.GVRLastSEPA;
import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Implementierung des HBCI-Jobs fuer die SEPA-Basis-Multi-Lastschrift.
 */
public class GVMultiLastSEPA extends GVLastSEPA {
    public GVMultiLastSEPA(HBCIPassportInternal passport) {
        this(passport, getLowlevelName(), new GVRLastSEPA(passport));
    }

    public GVMultiLastSEPA(HBCIPassportInternal passport, String lowlevelName, AbstractGVRLastSEPA result) {
        super(passport, lowlevelName, result);

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
        return "SammelLastSEPA";
    }

    @Override
    protected void createPainXml() {
        super.createPainXml();
        setParam("Total", SepaUtil.sumBtgValueObject(painParams));
    }
}
