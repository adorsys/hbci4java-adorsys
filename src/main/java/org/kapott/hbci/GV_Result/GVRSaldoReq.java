/*  $Id: GVRSaldoReq.java,v 1.1 2011/05/04 22:37:48 willuhn Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2008  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    HBCI4Java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.GV_Result;

import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Saldo;
import org.kapott.hbci.structures.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ergebnisse einer Saldenabfrage. Hier ist für jedes abgefragte Konto
 * genau ein entsprechendes Saldo-Objekt eingetragen.
 */
public final class GVRSaldoReq extends HBCIJobResultImpl {

    private List<Info> saldi = new ArrayList<>();

    public GVRSaldoReq(HBCIPassportInternal passport) {
        super(passport);
    }

    public void store(GVRSaldoReq.Info info) {
        saldi.add(info);
    }

    /**
     * Gibt alle verfügbaren Saldo-Informationen in einem Feld zurück.
     * Dabei existiert für jedes abgefragte Konto ein Eintrag in diesem Feld.
     *
     * @return Array mit Saldeninformationen
     */
    public List<Info> getEntries() {
        return saldi;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();

        for (Info info : saldi) {
            ret.append(info.toString()).append(System.getProperty("line.separator"));
        }

        return ret.toString().trim();
    }

    /**
     * Saldo-Informationen für ein Konto
     */
    public static final class Info
        implements Serializable {
        /**
         * Saldo für welches Konto
         */
        public Konto konto;
        /**
         * Gebuchter Saldo
         */
        public Saldo ready;
        /**
         * Saldo noch nicht verbuchter Umsätze (optional)
         */
        public Saldo unready;
        /**
         * Saldo für vorgemerkte Umsätze (optional)
         */
        public Saldo reserved;
        /**
         * Kreditlinie (optional)
         */
        public Value kredit;
        /**
         * Aktuell verfügbarer Betrag (optional)
         */
        public Value available;
        /**
         * Bereits verfügter Betrag (optional)
         */
        public Value used;

        public String toString() {
            StringBuilder ret = new StringBuilder();
            String linesep = System.getProperty("line.separator");

            ret.append("Konto: ").append(konto.toString()).append(linesep);
            ret.append("  Gebucht: ").append(ready.toString()).append(linesep);

            if (unready != null)
                ret.append("  Pending: ").append(unready).append(linesep);
            if (kredit != null)
                ret.append("  Kredit: ").append(kredit).append(linesep);
            if(reserved != null)
                ret.append("  Vorgemerkt:").append(reserved).append(linesep);
            if (available != null)
                ret.append("  Verfügbar: ").append(available).append(linesep);
            if (used != null)
                ret.append("  Benutzt: ").append(used);

            return ret.toString().trim();
        }
    }
}
