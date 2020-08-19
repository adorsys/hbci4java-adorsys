/*
 * Copyright 2018-2020 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.GVRVeuStep;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.SEG;
import org.kapott.hbci.protocol.SyntaxElement;

import java.util.Date;
import java.util.Random;

public class GVVeuStep extends AbstractHBCIJob {

    public GVVeuStep(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRVeuStep(passport));

        addConstraint("my.number", "my.number", null);
        addConstraint("my.subnumber", "my.subnumber", "");
        addConstraint("my.blz", "my.KIK.blz", null);
        addConstraint("my.country", "my.KIK.country", "DE");

        addConstraint("orderref", "orderref", null);

        addConstraint("sig", "sig", "B" + createSig());
    }

    private String createSig() {
        SEG sighead = new SEG("SigHeadUser", "SigHead", null, 0, passport.getSyntaxDocument());

        String sigheadName = sighead.getPath();
        String seccheckref = Integer.toString(Math.abs(new Random().nextInt()));

        Date d = new Date();

        sighead.propagateValue(sigheadName + ".secfunc", passport.getSigFunction(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".seccheckref", seccheckref,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".role", "1",
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SecIdnDetails.func", "1",
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SecIdnDetails.sysid", passport.getSysId(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SecTimestamp.date", HBCIUtils.date2StringISO(d),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SecTimestamp.time", HBCIUtils.time2StringISO(d),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.propagateValue(sigheadName + ".secref", "1",
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.propagateValue(sigheadName + ".HashAlg.alg", passport.getHashAlg(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SigAlg.alg", passport.getSigAlg(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SigAlg.mode", passport.getSigMode(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.propagateValue(sigheadName + ".KeyName.KIK.country", passport.getCountry(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".KeyName.KIK.blz", passport.getBLZ(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".KeyName.userid", passport.getMySigKeyName(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".KeyName.keynum", passport.getMySigKeyNum(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".KeyName.keyversion", passport.getMySigKeyVersion(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.propagateValue(sigheadName + ".SecProfile.method", passport.getProfileMethod(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SecProfile.version", passport.getProfileVersion(),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);


        SEG sigtail = new SEG("SigTailUser", "SigTail", null, 0, passport.getSyntaxDocument());
        sigtail.propagateValue(sigtail.getPath() + ".seccheckref", seccheckref,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.validate();
        sigtail.validate();

        return sighead.toString(0) + sigtail.toString(0);
    }

    public static String getLowlevelName() {
        return "VeuStep";
    }


}
