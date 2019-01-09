/**********************************************************************
 *
 * Copyright (c) by Olaf Willuhn
 * All rights reserved
 * LGPL
 *
 **********************************************************************/

package org.kapott.hbci.GV;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV_Result.GVRKontoauszug;
import org.kapott.hbci.GV_Result.GVRKontoauszug.Format;
import org.kapott.hbci.GV_Result.GVRKontoauszug.GVRKontoauszugEntry;
import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;

/**
 * Implementierung des Geschaeftsvorfalls fuer den elektronischen Kontoauszug
 * (HKEKP) im PDF-Format
 */
@Slf4j
public class GVKontoauszugPdf extends AbstractHBCIJob {

    public GVKontoauszugPdf(HBCIPassportInternal passport, String name) {
        super(passport, name, new GVRKontoauszug(passport));
    }

    public GVKontoauszugPdf(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());

        addConstraint("my.bic", "My.bic", null);
        addConstraint("my.iban", "My.iban", null);

        if (this.canNationalAcc(passport)) {
            addConstraint("my.country", "My.KIK.country", "DE");
            addConstraint("my.blz", "My.KIK.blz", "");
            addConstraint("my.number", "My.number", "");
            addConstraint("my.subnumber", "My.subnumber", "");
        }

        addConstraint("idx", "idx", "");
        addConstraint("year", "year", "");
        addConstraint("maxentries", "maxentries", "");
        addConstraint("offset", "offset", "");
    }

    /**
     * Liefert den Lowlevel-Namen des Auftrags.
     *
     * @return der Lowlevel-Name des Auftrags.
     */
    public static String getLowlevelName() {
        return "KontoauszugPdf";
    }

    /**
     * @see org.kapott.hbci.GV.HBCIJobImpl#extractResults(org.kapott.hbci.status.HBCIMsgStatus, java.lang.String, int)
     */
    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRKontoauszug list = (GVRKontoauszug) jobResult;

        GVRKontoauszugEntry auszug = new GVRKontoauszugEntry();
        list.getEntries().add(auszug);

        // Das Format setzen wir hier pauschal auf PDF, weil HKEKP immer PDF liefert
        auszug.setFormat(Format.PDF);

        ////////////////////////////////////////////////////////////////////////
        // Die folgenden Parameter existieren in Segment-Version 1 noch
        // nicht - daher die Null-Checks. Auch dann, wenn die Properties
        // in Segment-Version u.U. Pflicht sind.

        String start = result.get(header + ".TimeRange.startdate");
        String end = result.get(header + ".TimeRange.enddate");
        String date = result.get(header + ".date");
        String year = result.get(header + ".year");
        String number = result.get(header + ".number");

        if (start != null && start.length() > 0)
            auszug.setStartDate(HBCIUtils.string2DateISO(start));

        if (end != null && end.length() > 0)
            auszug.setEndDate(HBCIUtils.string2DateISO(end));

        if (date != null && date.length() > 0)
            auszug.setDate(HBCIUtils.string2DateISO(date));

        if (year != null && year.length() > 0)
            auszug.setYear(Integer.parseInt(year));

        if (number != null && number.length() > 0)
            auszug.setNumber(Integer.parseInt(number));

        // Wenn hier NULL drin steht, ist es nicht weiter schlimm
        auszug.setIBAN(result.get(header + ".iban"));
        auszug.setBIC(result.get(header + ".bic"));
        auszug.setName(result.get(header + ".name"));
        auszug.setName2(result.get(header + ".name2"));
        auszug.setName3(result.get(header + ".name3"));
        auszug.setFilename(result.get(header + ".filename"));
        //
        ////////////////////////////////////////////////////////////////////////

        // Den Rest gibts auch in Segment-Version 1

        // In Segment-Version sind die PDF-Daten Base64-codiert, obwohl sie als
        // Typ "bin" angegeben sind. Das ist ein Fehler in der Spec. In Segment-
        // Version 2 wurde das korrigiert. Allerdings gibts es jetzt einen BPD-
        // Parameter anhand dem erkannt werden kann, ob es Base64-codiert ist
        // oder nicht. Das sind mir zuviele Variablen. Zumal mich nicht wundern
        // wuerde, wenn es Banken gibt, die in den BPD nicht reinschreiben, dass
        // sie Base64 senden und es dann trotzdem tun. Also checken wir einfach
        // selbst. Wenn "data" nur ASCII-Zeichen enthaelt, kann es nur Base64
        // sein. Ansonsten ist es binaer.
        String data = result.get(header + ".booked");

        if (data != null && data.length() > 0) {
            try {
                if (data.startsWith("%PDF-")) {
                    // Ist Bin
                    auszug.setData(data.getBytes(CommPinTan.ENCODING));

                } else {
                    // Ist Base64
                    auszug.setData(Base64.getDecoder().decode(data.getBytes(CommPinTan.ENCODING)));
                }
            } catch (UnsupportedEncodingException e) {
                // Kann eigentlich nicht passieren
                log.warn(e.getMessage(), e);
            }
        }

        String receipt = result.get(header + ".receipt");
        if (receipt != null) {
            try {
                auszug.setReceipt(receipt.getBytes(CommPinTan.ENCODING));
            } catch (UnsupportedEncodingException e) {
                log.warn(e.getMessage(), e);

                // Wir versuchen es als Fallback ohne explizites Encoding
                auszug.setReceipt(receipt.getBytes());
            }
        }
    }

    /**
     * @see org.kapott.hbci.GV.HBCIJobImpl#verifyConstraints()
     */
    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
