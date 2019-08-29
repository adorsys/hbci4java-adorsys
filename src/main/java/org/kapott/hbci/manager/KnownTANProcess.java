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

/**
 * Enthaelt die Liste der bekannten TAN-Prozesse.
 */
public enum KnownTANProcess {
    /**
     * Prozess-Variante 1.
     */
    PROCESS1("1"),

    /**
     * Prozess-Variante 2, Schritt 1.
     */
    PROCESS2_STEP1("4"),

    /**
     * Prozess-Variante 2, Schritt 2.
     */
    PROCESS2_STEP2("2"),

    ;

    private String code = null;

    /**
     * ct.
     *
     * @param code
     */
    KnownTANProcess(String code) {
        this.code = code;
    }

    /**
     * Ermittelt den passenden TAN-Prozess fuer den angegebenen Code.
     *
     * @param code der Code.
     * @return der TAN-Prozess oder NULL, wenn er nicht gefunden wurde.
     */
    public static KnownTANProcess determine(String code) {
        if (code == null || code.length() == 0)
            return null;

        for (KnownTANProcess t : values()) {
            if (t.is(code))
                return t;
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
     * Liefert den Code des TAN-Prozess-Schrittes.
     *
     * @return der Code des TAN-Prozess-Schrittes.
     */
    public String getCode() {
        return this.code;
    }
}
