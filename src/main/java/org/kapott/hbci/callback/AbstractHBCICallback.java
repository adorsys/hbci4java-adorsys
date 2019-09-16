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

package org.kapott.hbci.callback;

import org.kapott.hbci.manager.HHDVersion;

import java.util.List;

public class AbstractHBCICallback implements HBCICallback {
    @Override
    public void callback(int reason, List<String> messages, int datatype, StringBuilder retData) {

    }

    @Override
    public void tanChallengeCallback(String orderRef, String challenge, String challenge_hhd_uc, HHDVersion.Type type) {

    }

    @Override
    public String needTAN() {
        return null;
    }

    @Override
    public void status(int statusTag, Object[] o) {

    }

    @Override
    public void status(int statusTag, Object o) {

    }
}
