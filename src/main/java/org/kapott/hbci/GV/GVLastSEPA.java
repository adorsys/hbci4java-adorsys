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
 * Implementierung des HBCI-Jobs fuer die SEPA-Basis-Lastschrift.
 */
public class GVLastSEPA extends AbstractGVLastSEPA {
    /**
     * Liefert den Lowlevel-Jobnamen.
     * @return der Lowlevel-Jobname.
     */
    public static String getLowlevelName() {
        return "LastSEPA";
    }

    public GVLastSEPA(HBCIPassportInternal passport, MsgGen msgGen) {
        this(passport, msgGen, getLowlevelName(), new GVRLastSEPA(passport));
    }

    public GVLastSEPA(HBCIPassportInternal passport, MsgGen msgGen, String lowlevelName, AbstractGVRLastSEPA result) {
        super(passport, msgGen, lowlevelName, result);

        // Typ der Lastschrift. Moegliche Werte:
        // CORE = Basis-Lastschrift (Default)
        // COR1 = Basis-Lastschrift mit verkuerzter Vorlaufzeit
        // B2B  = Business-2-Business-Lastschrift mit eingeschraenkter Rueckgabe-Moeglichkeit
        //
        // TODO: Wobei eigentlich nur "CORE" erlaubt ist, da dieser GV nur die CORE-Lastschrift
        // kapselt. Eigentlich sollte das gar nicht konfigurierbar sein
        addConstraint("type", "sepa.type", "CORE", LogFilter.FILTER_NONE);
    }
}
