package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.passport.HBCIPassportInternal;

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
        addConstraint("orderhash", "orderhash", null);
    }

    public static String getLowlevelName() {
        return "VeuStep";
    }

    public void setParam(String paramName, String value) {
        if (paramName.equals("orderhash")) {
            value = "B" + "EF7D08406B05A4D83E300D1B01953E60F48844D8";
        }
        super.setParam(paramName, value);
    }
}
