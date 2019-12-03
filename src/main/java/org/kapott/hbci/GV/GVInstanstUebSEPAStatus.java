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

import org.kapott.hbci.GV_Result.GVRInstantUebSEPAStatus;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;
import java.util.Optional;

public class GVInstanstUebSEPAStatus extends AbstractHBCIJob {

    public GVInstanstUebSEPAStatus(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRInstantUebSEPAStatus(passport));

        addConstraint("my.bic", "My.bic", null);
        addConstraint("my.iban", "My.iban", null);
        addConstraint("sepadescriptor", "sepadescr", new GVInstantUebSEPA(passport).getPainVersion().getURN());
        addConstraint("orderid", "orderid", null);
    }

    public static String getLowlevelName() {
        return "InstantUebSEPAStatus";
    }

    @Override
    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        Optional.ofNullable(result.get(header + ".status"))
            .ifPresent(s -> ((GVRInstantUebSEPAStatus) jobResult).setStatus(Integer.parseInt(s)));
        Optional.ofNullable(result.get(header + ".cancellationCode"))
            .ifPresent(s -> ((GVRInstantUebSEPAStatus) jobResult).setCancellationCode(Integer.parseInt(s)));
    }

}
