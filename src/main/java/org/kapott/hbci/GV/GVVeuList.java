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

import org.kapott.hbci.GV_Result.GVRVeuList;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

public class GVVeuList extends AbstractHBCIJob {

    public GVVeuList(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRVeuList(passport));

        addConstraint("my.number", "my.number", null);
        addConstraint("my.subnumber", "my.subnumber", "");
        addConstraint("my.blz", "my.KIK.blz", null);
        addConstraint("my.country", "my.KIK.country", "DE");

        addConstraint("gv", "gv", "");
        addConstraint("sign_status", "sign_status", "");
        addConstraint("offset_idx", "offset_idx", "");
    }

    public static String getLowlevelName() {
        return "VeuList";
    }

    @Override
    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        ((GVRVeuList) (jobResult)).addEntry(msgstatus.getData().get(header + ".orderid"));
    }

}
