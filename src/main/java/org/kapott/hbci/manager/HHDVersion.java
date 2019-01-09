/**********************************************************************
 *
 * Copyright (c) by Olaf Willuhn
 * All rights reserved
 *
 **********************************************************************/

package org.kapott.hbci.manager;

import lombok.extern.slf4j.Slf4j;

/**
 * Kapselt die Erkennung der verschiedenen HHD-Versionen.
 */
@Slf4j
public enum HHDVersion {
    /**
     * QR-Code in HHD-Version 1.3 - die Sparkasse verwendet das so.
     * Muss als erstes hier stehen, weil es sonst falsch als "HHD_1_3" erkannt wird (ID beginnt genauso).
     */
    QR_1_3(Type.QRCODE, "HHD1\\.3\\..*?QR", "1.3", -1, "hhd13"),

    /**
     * QR-Code.
     */
    QR_1_4(Type.QRCODE, "Q1S.*", null, -1, "hhd14"),

    /**
     * HHD-Version 1.4
     * Zur HKTAN-Segment-Version: Genau wissen wir es nicht, aber HHD 1.4 ist wahrscheinlich.
     */
    HHD_1_4(Type.CHIPTAN, "HHD1\\.4.*", "1.4", 5, "hhd14"),

    /**
     * HHD-Version 1.3
     * Zur HKTAN-Segment-Version: 1.4 ist in HKTAN4 noch nicht erlaubt, damit bleibt eigentlich nur 1.3
     */
    HHD_1_3(Type.CHIPTAN, "HHD1\\.3.*", "1.3", 4, "hhd13"),

    /**
     * Server-seitig generierter Matrix-Code (photoTAN)
     * ZKA-Version und HKTAN-Version bleiben hier frei, weil wir anhand diesen
     * Merkmalen das Matrix-Code-Verfahren nicht eindeutig erkennen koennen.
     * Und da chipTAN/smsTAN deutlich gebrauechlicher ist, ist es erheblich wahrscheinlicher,
     * dass dann nicht Matrix-Code ist.
     * Generell unterstuetzen wir nur server-seitig generierte Matrix-Codes.
     */
    MS_1(Type.PHOTOTAN, "MS1.*", null, -1, "hhd14"),

    /**
     * HHD-Version 1.2.
     * Fallback.
     */
    HHD_1_2(Type.CHIPTAN, null, null, -1, "hhd12"),

    ;

    private Type type = null;
    private String idMatch = null;
    private String versionStart = null;
    private int segVersion = 0;
    private String challengeVersion = null;

    /**
     * ct.
     *
     * @param type             die Art des TAN-Verfahrens.
     * @param idMatch          Pattern fuer die Technische Kennung.
     *                         Siehe "Belegungsrichtlinien TANve1.4  mit Erratum 1-3 final version vom 2010-11-12.pdf"
     *                         Der Name ist standardisiert, wenn er mit "HHD1...." beginnt, ist das die HHD-Version
     * @param versionStart     ZKA-Version bei HKTAN.
     * @param segVersion       Segment-Version des HKTAN-Elements.
     * @param challengeVersion die Kennung fuer das Lookup in den ChallengeInfo-Daten.
     */
    private HHDVersion(Type type, String idMatch, String versionStart, int segVersion, String challengeVersion) {
        this.type = type;
        this.idMatch = idMatch;
        this.versionStart = versionStart;
        this.segVersion = segVersion;
        this.challengeVersion = challengeVersion;
    }

    /**
     * Ermittelt die zu verwendende HHD-Version aus den BPD-Informationen des TAN-Verfahrens.
     *
     * @param secmech die BPD-Informationen zum TAN-Verfahren.
     * @return die HHD-Version.
     */
    public static HHDVersion find(HBCITwoStepMechanism secmech) {
        log.debug("trying to determine HHD version for secmech: " + secmech);
        // Das ist die "Technische Kennung"
        // Siehe "Belegungsrichtlinien TANve1.4  mit Erratum 1-3 final version vom 2010-11-12.pdf"
        // Der Name ist standardisiert, wenn er mit "HHD1...." beginnt, ist
        // das die HHD-Version
        String id = secmech.getId();
        log.debug("  technical HHD id: " + id);
        for (HHDVersion v : values()) {
            String s = v.idMatch;
            if (s == null || id == null)
                continue;
            if (id.matches(s)) {
                log.debug("  identified as " + v);
                return v;
            }
        }

        // Fallback 1. Wir schauen noch in "ZKA-Version bei HKTAN"
        String version = secmech.getZkamethod_version();
        log.debug("  ZKA version: " + version);
        if (version != null && version.length() > 0) {
            for (HHDVersion v : values()) {
                String s = v.versionStart;
                if (s == null)
                    continue;
                if (version.startsWith(s)) {
                    log.debug("  identified as " + v);
                    return v;
                }
            }
        }

        // Fallback 2. Wir checken noch die HITAN/HKTAN-Version
        // Bei HKTAN5 kann es HHD 1.3 oder 1.4 sein, bei HKTAN4 bleibt eigentlich nur noch 1.3
        // Ich weiss nicht, ob Fallback 2 ueberhaupt notwendig ist. Denn angeblich
        // ist zkamethod_version seit HHD 1.3.1 Pflicht (siehe
        // FinTS_3.0_Security_Sicherheitsverfahren_PINTAN_Rel_20101027_final_version.pdf,
        // Data dictionary "Version ZKA-TAN-Verfahren"
        int segversion = secmech.getSegversion();
        log.debug("  segment version: " + segversion);
        for (HHDVersion v : values()) {
            int i2 = v.segVersion;
            if (i2 <= 0)
                continue;

            if (segversion == i2) {
                log.debug("  identified as " + v);
                return v;
            }
        }

        // Default:
        HHDVersion v = HHD_1_2;
        log.debug("  identified as " + v);
        return v;
    }

    /**
     * Liefert die Kennung fuer das Lookup in den ChallengeInfo-Daten.
     *
     * @return die Kennung fuer das Lookup in den ChallengeInfo-Daten.
     */
    public String getChallengeVersion() {
        return this.challengeVersion;
    }

    /**
     * Liefert die Art des TAN-Verfahrens.
     *
     * @return die Art des TAN-Verfahrens.
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Definiert die Art des TAN-Verfahrens.
     */
    public static enum Type {
        /**
         * chipTAN oder smsTAN.
         */
        CHIPTAN,

        /**
         * photoTAN.
         */
        PHOTOTAN,

        /**
         * QR-Code.
         */
        QRCODE
    }

}


