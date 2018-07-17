/**
 * Geschäftsvorfall SEPA Basislastschrift. Diese ist in pain.008.003.02.xsd spezifiziert.
 *
 * @author Jan Thielemann
 */

package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.AbstractGVRLastSEPA;
import org.kapott.hbci.GV_Result.GVRLastSEPA;
import org.kapott.hbci.manager.LogFilter;
import org.kapott.hbci.manager.MsgGen;
import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Implementierung des HBCI-Jobs fuer die SEPA-Basis-Multi-Lastschrift.
 */
public class GVMultiLastSEPA extends GVLastSEPA {
    /**
     * Liefert den Lowlevel-Jobnamen.
     *
     * @return der Lowlevel-Jobname.
     */
    public static String getLowlevelName() {
        return "SammelLastSEPA";
    }

    public GVMultiLastSEPA(HBCIPassportInternal passport, MsgGen msgGen) {
        this(passport, msgGen, getLowlevelName(), new GVRLastSEPA(passport));
    }

    public GVMultiLastSEPA(HBCIPassportInternal passport, MsgGen msgGen, String lowlevelName, AbstractGVRLastSEPA result) {
        super(passport, msgGen, lowlevelName, result);

        addConstraint("batchbook", "sepa.batchbook", "", LogFilter.FILTER_NONE);
        addConstraint("Total.value", "Total.value", null, LogFilter.FILTER_MOST);
        addConstraint("Total.curr", "Total.curr", null, LogFilter.FILTER_NONE);
    }

    @Override
    protected void createSEPAFromParams() {
        super.createSEPAFromParams();
        setParam("Total", SepaUtil.sumBtgValueObject(sepaParams));
    }
}
