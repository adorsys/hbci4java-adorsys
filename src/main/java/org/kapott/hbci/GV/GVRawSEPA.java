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

import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;

/**
 * Job-Implementierung fuer SEPA-Ueberweisungen.
 */
public class GVRawSEPA extends AbstractSEPAGV {

    private final static SepaVersion DEFAULT = SepaVersion.PAIN_001_001_02;

    public GVRawSEPA(HBCIPassportInternal passport) {
        this(passport, getLowlevelName(), null);
    }

    public GVRawSEPA(HBCIPassportInternal passport, String name, String pain) {
        super(passport, name);

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

        if (pain == null) {
            addConstraint("_sepapain", "sepapain", null);
        } else {
            setPainXml(pain);
        }
    }

    /**
     * Liefert den Lowlevel-Namen des Jobs.
     *
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName() {
        return "RawSEPA";
    }

    /**
     * @see AbstractSEPAGV#getDefaultPainVersion()
     */
    @Override
    protected SepaVersion getDefaultPainVersion() {
        return DEFAULT;
    }

    /**
     * @see AbstractSEPAGV#getPainType()
     */
    @Override
    protected SepaVersion.Type getPainType() {
        return SepaVersion.Type.PAIN_001;
    }
}
