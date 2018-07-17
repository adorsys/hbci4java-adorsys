/**
 * Geschäftsvorfall SEPA Basislastschrift. Diese ist in pain.008.003.02.xsd spezifiziert.
 *
 * @author Jan Thielemann
 */

package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.AbstractGVRLastSEPA;
import org.kapott.hbci.GV_Result.GVRLastB2BSEPA;
import org.kapott.hbci.manager.LogFilter;
import org.kapott.hbci.manager.MsgGen;
import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Implementierung des HBCI-Jobs fuer die SEPA-B2B-Lastschrift.
 */
public class GVLastB2BSEPA extends AbstractGVLastSEPA {
    /**
     * Liefert den Lowlevel-Jobnamen.
     * @return der Lowlevel-Jobname.
     */
    public static String getLowlevelName() {
        return "LastB2BSEPA";
    }


    public GVLastB2BSEPA(HBCIPassportInternal passport, MsgGen msgGen) {
        this(passport, msgGen, getLowlevelName(), new GVRLastB2BSEPA(passport));
    }

    public GVLastB2BSEPA(HBCIPassportInternal passport, MsgGen msgGen, String lowlevelName, AbstractGVRLastSEPA result) {
        super(passport, msgGen, lowlevelName, result);

        // Typ der Lastschrift. Moegliche Werte:
        // CORE = Basis-Lastschrift (Default)
        // COR1 = Basis-Lastschrift mit verkuerzter Vorlaufzeit
        // B2B  = Business-2-Business-Lastschrift mit eingeschraenkter Rueckgabe-Moeglichkeit
        //
        // TODO: Wobei eigentlich nur "B2B" erlaubt ist, da dieser GV nur die B2B-Lastschrift
        // kapselt. Eigentlich sollte das gar nicht konfigurierbar sein
        addConstraint("type", "sepa.type", "B2B", LogFilter.FILTER_NONE);
    }
}
