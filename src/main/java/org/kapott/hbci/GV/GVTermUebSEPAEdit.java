package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.GVRTermUebEdit;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

public class GVTermUebSEPAEdit extends AbstractSEPAGV {
    private final static SepaVersion DEFAULT = SepaVersion.PAIN_001_001_02;

    public GVTermUebSEPAEdit(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRTermUebEdit(passport));

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

        addConstraint("_sepadescriptor", "sepadescr", this.getPainVersion().getURN());
        addConstraint("_sepapain", "sepapain", null);

        /* dummy constraints to allow an application to set these values. the
         * overriden setLowlevelParam() stores these values in a special structure
         * which is later used to create the SEPA pain document. */
        addConstraint("src.bic", "sepa.src.bic", null);
        addConstraint("src.iban", "sepa.src.iban", null);
        addConstraint("src.name", "sepa.src.name", null);
        addConstraint("dst.bic", "sepa.dst.bic", null);
        addConstraint("dst.iban", "sepa.dst.iban", null);
        addConstraint("dst.name", "sepa.dst.name", null);
        addConstraint("btg.value", "sepa.btg.value", null);
        addConstraint("btg.curr", "sepa.btg.curr", "EUR");
        addConstraint("usage", "sepa.usage", "");
        addConstraint("date", "sepa.date", null);

        // Constraints für die PmtInfId (eindeutige SEPA Message ID) und EndToEndId (eindeutige ID um Transaktion zu identifizieren)
        addConstraint("sepaid", "sepa.sepaid", getPainMessageId());
        addConstraint("pmtinfid", "sepa.pmtinfid", getPainMessageId());
        addConstraint("endtoendid", "sepa.endtoendid", ENDTOEND_ID_NOTPROVIDED);
        addConstraint("purposecode", "sepa.purposecode", "");

    }

    /**
     * Liefert den Lowlevel-Namen des Jobs.
     *
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName() {
        return "TermUebSEPAEdit";
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getDefaultPainVersion()
     */
    @Override
    protected SepaVersion getDefaultPainVersion() {
        return DEFAULT;
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getPainType()
     */
    @Override
    protected SepaVersion.Type getPainType() {
        return SepaVersion.Type.PAIN_001;
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        String orderid = result.get(header + ".orderid");

        ((GVRTermUebEdit) (jobResult)).setOrderId(orderid);
        ((GVRTermUebEdit) (jobResult)).setOrderIdOld(result.get(header + ".orderidold"));

        if (orderid != null && orderid.length() != 0) {
            HashMap<String, String> p2 = new HashMap<>();
            getLowlevelParams().forEach((key, value) ->
                p2.put(key.substring(key.indexOf(".") + 1), value));
//TODO
//            passport.setPersistentData("termueb_" + orderid, p2);
        }
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getPainJobName()
     */
    public String getPainJobName() {
        return "UebSEPA";
    }
}
