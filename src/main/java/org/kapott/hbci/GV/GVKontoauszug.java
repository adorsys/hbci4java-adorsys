/*  $Id: GVKontoauszug.java,v 1.1 2011/05/04 22:37:54 willuhn Exp $

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

package org.kapott.hbci.GV;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV_Result.GVRKontoauszug;
import org.kapott.hbci.GV_Result.GVRKontoauszug.Format;
import org.kapott.hbci.GV_Result.GVRKontoauszug.GVRKontoauszugEntry;
import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.swift.Swift;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Implementierung des Geschaeftsvorfalls fuer den elektronischen Kontoauszug (HKEKA)
 */
@Slf4j
public class GVKontoauszug extends AbstractHBCIJob {

    public GVKontoauszug(HBCIPassportInternal passport, String name) {
        super(passport, name, new GVRKontoauszug(passport));
    }

    public GVKontoauszug(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());

        boolean sepa = false;
        try {
            sepa = this.getSegVersion() >= 4;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        boolean nat = this.canNationalAcc(passport);

        if (sepa) {
            addConstraint("my.bic", "My.bic", null);
            addConstraint("my.iban", "My.iban", null);
        }

        if (nat || !sepa) {
            addConstraint("my.country", "My.KIK.country", "DE");
            addConstraint("my.blz", "My.KIK.blz", "");
            addConstraint("my.number", "My.number", "");
            addConstraint("my.subnumber", "My.subnumber", "");
        }

        addConstraint("format", "format", "");
        addConstraint("idx", "idx", "");
        addConstraint("year", "year", "");
        addConstraint("maxentries", "maxentries", "");
        addConstraint("offset", "offset", "");
    }

    /**
     * Liefert den Lowlevel-Namen.
     *
     * @return der Lowlevel-Name.
     */
    public static String getLowlevelName() {
        return "Kontoauszug";
    }

    /**
     * @see org.kapott.hbci.GV.HBCIJobImpl#extractResults(org.kapott.hbci.status.HBCIMsgStatus, java.lang.String, int)
     */
    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRKontoauszug list = (GVRKontoauszug) jobResult;

        GVRKontoauszugEntry auszug = new GVRKontoauszugEntry();
        list.getEntries().add(auszug);

        Format format = Format.find(result.get(header + ".format"));
        auszug.setFormat(format);

        String data = result.get(header + ".booked");

        if (data != null && data.length() > 0) {
            if (format != null && format == Format.MT940)
                data = Swift.decodeUmlauts(data);

            try {
                auszug.setData(data.getBytes(CommPinTan.ENCODING));
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage(), e);

                // Wir versuchen es als Fallback ohne explizites Encoding
                auszug.setData(data.getBytes());
            }
        }

        String date = result.get(header + ".date");
        if (date != null && date.length() > 0)
            auszug.setDate(HBCIUtils.string2DateISO(date));

        String year = result.get(header + ".year");
        String number = result.get(header + ".number");
        if (year != null && year.length() > 0)
            auszug.setYear(Integer.parseInt(year));
        if (number != null && number.length() > 0)
            auszug.setNumber(Integer.parseInt(number));

        auszug.setStartDate(HBCIUtils.string2DateISO(result.get(header + ".TimeRange.startdate")));
        auszug.setEndDate(HBCIUtils.string2DateISO(result.get(header + ".TimeRange.enddate")));
        auszug.setAbschlussInfo(result.get(header + ".abschlussinfo"));
        auszug.setKundenInfo(result.get(header + ".kondinfo"));
        auszug.setWerbetext(result.get(header + ".ads"));
        auszug.setIBAN(result.get(header + ".iban"));
        auszug.setBIC(result.get(header + ".bic"));
        auszug.setName(result.get(header + ".name"));
        auszug.setName2(result.get(header + ".name2"));
        auszug.setName3(result.get(header + ".name3"));

        String receipt = result.get(header + ".receipt");
        if (receipt != null) {
            try {
                auszug.setReceipt(receipt.getBytes(CommPinTan.ENCODING));
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage(), e);

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
