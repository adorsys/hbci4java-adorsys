package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.passport.HBCIPassportInternal;

/**
 * Created by cbr on 26.03.19.
 */
public class GVVeuEmptySignatureHeader extends AbstractHBCIJob  {
    public GVVeuEmptySignatureHeader(HBCIPassportInternal passport, String jobnameLL, HBCIJobResultImpl jobResult) {
        super(passport, jobnameLL, jobResult);
    }

    public GVVeuEmptySignatureHeader(HBCIPassportInternal passport) {
        this(passport, getLowlevelName(), null);
    }

    public static String getLowlevelName() {
        return "VeuEmptySignatureHeader";
    }
}
