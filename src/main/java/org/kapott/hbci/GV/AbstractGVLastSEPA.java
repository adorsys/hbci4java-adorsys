package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.AbstractGVRLastSEPA;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

/**
 * Abstrakte Basisklasse fuer die terminierten SEPA-Lastschriften.
 */
public abstract class AbstractGVLastSEPA extends AbstractSEPAGV {
    private final static SepaVersion DEFAULT = SepaVersion.PAIN_008_001_01;

    public AbstractGVLastSEPA(HBCIPassportInternal passport, String lowlevelName, AbstractGVRLastSEPA result) {
        super(passport, lowlevelName, result);

        // My bzw. src ist das Konto des Ausführenden. Dst ist das Konto des
        // Belasteten.
        addConstraint("src.bic", "My.bic", null);
        addConstraint("src.iban", "My.iban", null);

        if (this.canNationalAcc(passport)) // nationale Bankverbindung mitschicken, wenn erlaubt
        {
            addConstraint("src.country", "My.KIK.country", "");
            addConstraint("src.blz", "My.KIK.blz", "");
            addConstraint("src.number", "My.number", "");
            addConstraint("src.subnumber", "My.subnumber", "");
        }

        addConstraint("_sepadescriptor", "sepadescr", this.getPainVersion().getURN());
        addConstraint("_sepapain", "sepapain", null);

        addConstraint("src.bic", "sepa.src.bic", null);
        addConstraint("src.iban", "sepa.src.iban", null);
        addConstraint("src.name", "sepa.src.name", null);
        addConstraint("dst.bic", "sepa.dst.bic", null, true);
        addConstraint("dst.iban", "sepa.dst.iban", null, true);
        addConstraint("dst.name", "sepa.dst.name", null, true);
        addConstraint("btg.value", "sepa.btg.value", null, true);
        addConstraint("btg.curr", "sepa.btg.curr", "EUR", true);
        addConstraint("usage", "sepa.usage", "", true);

        addConstraint("sepaid", "sepa.sepaid", getPainMessageId());
        addConstraint("pmtinfid", "sepa.pmtinfid", getPainMessageId());
        addConstraint("endtoendid", "sepa.endtoendid", ENDTOEND_ID_NOTPROVIDED, true);
        addConstraint("creditorid", "sepa.creditorid", null, true);
        addConstraint("mandateid", "sepa.mandateid", null, true);
        addConstraint("purposecode", "sepa.purposecode", "", true);

        // Datum als java.util.Date oder als ISO-Date-String im Format yyyy-MM-dd
        addConstraint("manddateofsig", "sepa.manddateofsig", null, true);
        addConstraint("amendmandindic", "sepa.amendmandindic", Boolean.toString(false), true);

        // Moegliche Werte:
        //   FRST = Erst-Einzug
        //   RCUR = Folge-Einzug
        //   OOFF = Einmal-Einzug
        //   FNAL = letztmaliger Einzug
        //
        // Ueblicherweise verwendet man bei einem Mandat bei der ersten Abbuchung "FRST"
        // und bei allen Folgeabbuchungen des selben Mandats "RCUR".
        addConstraint("sequencetype", "sepa.sequencetype", "FRST");

        // Ziel-Datum fuer den Einzug. Default: 1999-01-01 als Platzhalter fuer "zum naechstmoeglichen Zeitpunkt
        // Datum als java.util.Date oder als ISO-Date-String im Format yyyy-MM-dd
        addConstraint("targetdate", "sepa.targetdate", SepaUtil.DATE_UNDEFINED);

        // Der folgende Constraint muss in der jeweiligen abgeleiteten Klasse passend gesetzt werden.
        // Typ der Lastschrift. Moegliche Werte:
        // CORE = Basis-Lastschrift (Default)
        // COR1 = Basis-Lastschrift mit verkuerzter Vorlaufzeit
        // B2B  = Business-2-Business-Lastschrift mit eingeschraenkter Rueckgabe-Moeglichkeit
        // addConstraint("type",            "sepa.type",          "CORE");

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
        return SepaVersion.Type.PAIN_008;
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getPainJobName()
     */
    @Override
    public String getPainJobName() {
        return "LastSEPA";
    }

    /**
     * @see AbstractHBCIJob#extractResults(org.kapott.hbci.status.HBCIMsgStatus, java.lang.String, int)
     */
    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        String orderid = result.get(header + ".orderid");
        ((AbstractGVRLastSEPA) (jobResult)).setOrderId(orderid);

        if (orderid != null && orderid.length() != 0) {
            HashMap<String, String> p2 = new HashMap<>();
            getLowlevelParams().forEach((key, value) ->
                p2.put(key.substring(key.indexOf(".") + 1), value));

            // TODO: Fuer den Fall, dass sich die Order-IDs zwischen CORE, COR1 und B2B
            // ueberschneiden koennen, muessen hier unterschiedliche Keys vergeben werden.
//TODO            passport.setPersistentData("termlast_" + orderid, p2);
        }
    }
}
