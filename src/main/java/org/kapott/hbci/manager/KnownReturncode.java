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

package org.kapott.hbci.manager;

import org.kapott.hbci.status.HBCIRetVal;

import java.util.List;

/**
 * Liste von bekannten Returncodes.
 */
public enum KnownReturncode {
    /**
     * Es liegen weitere Informationen vor - mit Aufsetzpunkt.
     */
    W3040("3040"),

    /**
     * Geaenderte Benutzerdaten.
     */
    W3072("3072"),

    /**
     * SCA-Ausnahme.
     */
    W3076("3076"),

    /**
     * Die Liste der zugelassenen Zweischritt-Verfahren.
     */
    W3920("3920"),

    /**
     * Signatur falsch (generisch)
     */
    E9340("9340"),

    /**
     * PIN falsch (konkret)
     */
    E9942("9942"),

    ;

    /**
     * Die Liste der Return-Codes, die als "PIN falsch" interpretiert werden sollen.
     */
    public final static KnownReturncode[] LIST_AUTH_FAIL = new KnownReturncode[]{E9340, E9942};

    private String code = null;

    /**
     * ct.
     *
     * @param code der Code.
     */
    private KnownReturncode(String code) {
        this.code = code;
    }

    /**
     * Prueft, ob der angegebene Code in der Liste enthalten ist.
     *
     * @param code  der zu pruefende Code.
     * @param codes die Liste der Codes.
     * @return true, wenn er in der Liste enthalten ist.
     */
    public static boolean contains(String code, KnownReturncode... codes) {
        return find(code, codes) != null;
    }

    /**
     * Prueft, ob der angegebene Code in der Liste enthalten ist.
     *
     * @param code  der zu pruefende Code.
     * @param codes die Liste der Codes.
     * @return true, wenn er in der Liste enthalten ist.
     */
    public static KnownReturncode find(String code, KnownReturncode... codes) {
        if (code == null || code.length() == 0 || codes == null || codes.length == 0)
            return null;

        for (KnownReturncode c : codes) {
            if (c.is(code))
                return c;
        }

        return null;
    }

    /**
     * Prueft der angegebene Code identisch ist.
     *
     * @param code der zu pruefende Code.
     * @return true, wenn der Code identisch ist.
     */
    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * Sucht nach dem angegebenen Status-Code in den Rueckmeldungen und liefert den Code zurueck.
     *
     * @param rets die Rueckmeldungen.
     * @return der gesuchte Rueckmeldecode oder NULL, wenn er nicht existiert.
     */
    public HBCIRetVal searchReturnValue(List<HBCIRetVal> rets) {
        if (rets == null || rets.isEmpty())
            return null;

        return rets.stream().filter(hbciRetVal -> is(hbciRetVal.code))
            .findAny()
            .orElse(null);
    }
}
