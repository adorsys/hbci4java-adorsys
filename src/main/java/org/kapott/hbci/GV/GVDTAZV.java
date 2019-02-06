/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
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

import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;

/**
 * Job-Implementierung fuer DTAZV Auslandsüberweisungen.
 */
public class GVDTAZV extends AbstractHBCIJob {

    public GVDTAZV(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());
    }

    public GVDTAZV(HBCIPassportInternal passport, String name) {
        super(passport, name, new HBCIJobResultImpl(passport));

        addConstraint("src.country", "My.KIK.country", "DE");
        addConstraint("src.blz", "My.KIK.blz", null);
        addConstraint("src.number", "My.number", null);
        addConstraint("src.subnumber", "My.subnumber", "");
        addConstraint("dtazv", "datensatz", "");
    }

    /**
     * Liefert den Lowlevel-Namen des Jobs.
     *
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName() {
        return "DTAZV";
    }

    @Override
    public String getRawData() {
        return getLowlevelParam(getName() + ".datensatz");
    }
}
