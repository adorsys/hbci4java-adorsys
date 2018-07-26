package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.GVRDauerNew;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.PainVersion;
import org.kapott.hbci.sepa.PainVersion.Type;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public class GVDauerSEPANew extends AbstractSEPAGV {

    private final static PainVersion DEFAULT = PainVersion.PAIN_001_001_02;

    public GVDauerSEPANew(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRDauerNew(passport));

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

        //Constraints für die PmtInfId (eindeutige SEPA Message ID) und EndToEndId (eindeutige ID um Transaktion zu identifizieren)
        addConstraint("sepaid", "sepa.sepaid", getSEPAMessageId());
        addConstraint("pmtinfid", "sepa.pmtinfid", getSEPAMessageId());
        addConstraint("endtoendid", "sepa.endtoendid", ENDTOEND_ID_NOTPROVIDED);
        addConstraint("purposecode", "sepa.purposecode", "");

        // DauerDetails
        addConstraint("firstdate", "DauerDetails.firstdate", null);
        addConstraint("timeunit", "DauerDetails.timeunit", null);
        addConstraint("turnus", "DauerDetails.turnus", null);
        addConstraint("execday", "DauerDetails.execday", null);
        addConstraint("lastdate", "DauerDetails.lastdate", "");
    }

    /**
     * Liefert den Lowlevel-Namen des Jobs.
     *
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName() {
        return "DauerSEPANew";
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getDefaultPainVersion()
     */
    @Override
    protected PainVersion getDefaultPainVersion() {
        return DEFAULT;
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getPainType()
     */
    @Override
    protected Type getPainType() {
        return Type.PAIN_001;
    }

    public void setParam(String paramName, String value) {
        HashMap<String, String> res = getJobRestrictions();

        if (paramName.equals("timeunit")) {
            if (!(value.equals("W") || value.equals("M"))) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_INV_TIMEUNIT", value);
                throw new InvalidUserDataException(msg);
            }
        } else if (paramName.equals("turnus")) {
            String timeunit = getLowlevelParams().getProperty(getName() + ".DauerDetails.timeunit");

            if (timeunit != null) {
                if (timeunit.equals("W")) {
                    String st = res.get("turnusweeks");

                    if (st != null) {
                        String value2 = new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2, st)) {
                            String msg = HBCIUtils.getLocMsg("EXCMSG_INV_TURNUS", value);
                            throw new InvalidUserDataException(msg);
                        }
                    }
                } else if (timeunit.equals("M")) {
                    String st = res.get("turnusmonths");

                    if (st != null) {
                        String value2 = new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2, st)) {
                            String msg = HBCIUtils.getLocMsg("EXCMSG_INV_TURNUS", value);
                            throw new InvalidUserDataException(msg);
                        }
                    }
                }
            }
        } else if (paramName.equals("execday")) {
            String timeunit = getLowlevelParams().getProperty(getName() + ".DauerDetails.timeunit");

            if (timeunit != null) {
                if (timeunit.equals("W")) {
                    String st = res.get("daysperweek");

                    if (st != null && !st.equals("0") && st.indexOf(value) == -1) {
                        String msg = HBCIUtils.getLocMsg("EXCMSG_INV_EXECDAY", value);
                        throw new InvalidUserDataException(msg);
                    }
                } else if (timeunit.equals("M")) {
                    String st = res.get("dayspermonth");

                    if (st != null) {
                        String value2 = new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2, st)) {
                            String msg = HBCIUtils.getLocMsg("EXCMSG_INV_EXECDAY", value);
                            throw new InvalidUserDataException(msg);
                        }
                    }
                }
            }
        }

        super.setParam(paramName, value);
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        String orderid = result.get(header + ".orderid");
        ((GVRDauerNew) (jobResult)).setOrderId(orderid);

        if (orderid != null && orderid.length() != 0) {
            Properties p = getLowlevelParams();
            Properties p2 = new Properties();

            for (Enumeration e = p.propertyNames(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                p2.setProperty(key.substring(key.indexOf(".") + 1),
                        p.getProperty(key));
            }

            passport.setPersistentData("dauer_" + orderid, p2);
        }
    }

    public String getPainJobName() {
        return "UebSEPA";
    }

}
