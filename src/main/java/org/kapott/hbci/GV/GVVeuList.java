package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Created by cbr on 26.03.19.
 */
public class GVVeuList extends AbstractHBCIJob  {
    public GVVeuList(HBCIPassportInternal passport, String jobnameLL, HBCIJobResultImpl jobResult) {
        super(passport, jobnameLL, jobResult);
    }

    public GVVeuList(HBCIPassportInternal passport) {
        this(passport, getLowlevelName(), null);
        addConstraint("src.country", "My.KIK.country", "280");
        addConstraint("src.blz", "My.KIK.blz", "");
        addConstraint("src.number", "My.number", "");
        addConstraint("supported", "supported", null);
        addConstraint("tailnum", "tailnum", null);

    }

    public static String getLowlevelName() {
        return "VeuList";
    }
}
