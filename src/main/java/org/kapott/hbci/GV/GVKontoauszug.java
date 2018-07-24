
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

import org.kapott.hbci.GV_Result.GVRKontoauszug;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.swift.Swift;
import org.w3c.dom.Document;

import java.util.Properties;

// TODO: doku fehlt (html)
public class GVKontoauszug extends AbstractHBCIJob {

    public final static String FORMAT_MT940 = "1";
    public final static String FORMAT_ISO8583 = "2";
    public final static String FORMAT_PDF = "3";

    public static String getLowlevelName() {
        return "Kontoauszug";
    }

    public GVKontoauszug(HBCIPassportInternal passport, String name) {
        super(passport, name, new GVRKontoauszug(passport));
    }

    public GVKontoauszug(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());

        addConstraint("my.country", "My.KIK.country", "DE");
        addConstraint("my.blz", "My.KIK.blz", null);
        addConstraint("my.number", "My.number", null);
        addConstraint("my.subnumber", "My.subnumber", "");
        addConstraint("format", "format", "");
        addConstraint("idx", "idx", "");
        addConstraint("year", "year", "");
        addConstraint("maxentries", "maxentries", "");
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        Properties result = msgstatus.getData();
        GVRKontoauszug umsResult = (GVRKontoauszug) jobResult;

        String format = result.getProperty(header + ".format");
        String rawData = result.getProperty(header + ".booked");

        if (rawData != null) {
            if (format.equals("1")) {
                umsResult.appendMT940Data(Swift.decodeUmlauts(rawData));
            } else if (format.equals("2")) {
                umsResult.appendISOData(rawData);
            } else if (format.equals("3")) {
                umsResult.appendPDFData(rawData);
            } else {
                HBCIUtils.log(
                        "unknown format in result for GV Kontoauszug: " + format,
                        HBCIUtils.LOG_ERR);
            }
        }

        umsResult.setFormat(format);
        umsResult.setStartDate(HBCIUtils.string2DateISO(result.getProperty(header + ".TimeRange.startdate")));
        umsResult.setEndDate(HBCIUtils.string2DateISO(result.getProperty(header + ".TimeRange.enddate")));
        umsResult.setAbschlussInfo(result.getProperty(header + ".abschlussinfo"));
        umsResult.setKundenInfo(result.getProperty(header + ".kondinfo"));
        umsResult.setWerbetext(result.getProperty(header + ".ads"));
        umsResult.setIBAN(result.getProperty(header + ".iban"));
        umsResult.setBIC(result.getProperty(header + ".bic"));
        umsResult.setName(result.getProperty(header + ".name"));
        umsResult.setName2(result.getProperty(header + ".name2"));
        umsResult.setName3(result.getProperty(header + ".name3"));
        umsResult.setReceipt(result.getProperty(header + ".receipt"));
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
