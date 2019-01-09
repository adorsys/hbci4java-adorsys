package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.GVRKontoauszug;
import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Geschaeftsvorfall fuer das Senden der Empfangsquittung mittels HKQTG.
 */
public class GVReceipt extends AbstractHBCIJob {

    public GVReceipt(HBCIPassportInternal passport, String name) {
        super(passport, name, new GVRKontoauszug(passport));
    }

    public GVReceipt(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new HBCIJobResultImpl(passport));
        addConstraint("receipt", "receipt", "");
    }

    /**
     * Liefert den Lowlevel-Namen des Geschaeftsvorfalls.
     *
     * @return der Lowlevel-Namen des Geschaeftsvorfalls.
     */
    public static String getLowlevelName() {
        return "Receipt";
    }

    /**
     * @see org.kapott.hbci.GV.HBCIJobImpl#setParam(java.lang.String, java.lang.String)
     */
    public void setParam(String paramName, String value) {
        // Feld als binaer markieren
        if (paramName.equals("receipt"))
            value = "B" + value;
        super.setParam(paramName, value);
    }

}
