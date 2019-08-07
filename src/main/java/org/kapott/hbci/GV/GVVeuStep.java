package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.security.Sig;

import java.util.Base64;

/**
 * Created by cbr on 26.03.19.
 */
public class GVVeuStep extends AbstractHBCIJob  {
    public GVVeuStep(HBCIPassportInternal passport, String jobnameLL, HBCIJobResultImpl jobResult) {
        super(passport, jobnameLL, jobResult);
    }

    public GVVeuStep(HBCIPassportInternal passport) {
        this(passport, getLowlevelName(), null);
        addConstraint("orderid", "orderid", null);
        addConstraint("src.country", "My.KIK.country", "280");
        addConstraint("src.blz", "My.KIK.blz", "");
        addConstraint("src.number", "My.number", "");
        addConstraint("unknown", "unknown", null);
    }

    public static String getLowlevelName() {
        return "VeuStep";
    }

    public void setParam(String paramName, String value) {
        if (paramName.equals("unknown")) {
            String test = "HNSHK:2:4+PIN:2+999+1748990525+1+1+1::201904051436175897650000020293+1+1:20190405:143639+1:999:1+6:10:16+280:25040090:ADORSYS-B:S:0:0'HNSHA:7:2+1748990525++12456:".replaceAll("1748990525", Sig.gurk.get());
            value = "B" + test + value + "'";
        }
        super.setParam(paramName, value);
    }
}
