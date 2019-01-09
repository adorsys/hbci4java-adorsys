/**********************************************************************
 *
 * Copyright (c) by Olaf Willuhn
 * All rights reserved
 * LGPL
 *
 **********************************************************************/

package org.kapott.hbci.GV;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV.parsers.ISEPAParser;
import org.kapott.hbci.GV.parsers.SEPAParserFactory;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.GV_Result.GVRKUms.BTag;
import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.sepa.SepaVersion.Type;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementierung des Geschaeftsvorfalls zum Abruf von Umsaetzen mit Angabe des Zeitraums im CAMT-Format (HKCAZ).
 */
@Slf4j
public class GVKUmsAllCamt extends AbstractSEPAGV {

    public GVKUmsAllCamt(HBCIPassportInternal passport, String name) {
        super(passport, name, new GVRKUms(passport));
    }

    public GVKUmsAllCamt(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());

        addConstraint("my.bic", "KTV.bic", null);
        addConstraint("my.iban", "KTV.iban", null);

        if (this.canNationalAcc(passport)) {
            addConstraint("my.country", "KTV.KIK.country", "DE");
            addConstraint("my.blz", "KTV.KIK.blz", null);
            addConstraint("my.number", "KTV.number", null);
            addConstraint("my.subnumber", "KTV.subnumber", "");
        }

        // Das DE erlaubt zwar, dass wir alle CAMT-Versionen mitschicken,
        // die wir unterstuetzen. Einige Banken (u.a. die Sparkassen) kommen
        // damit aber nicht klar. Deswegen schicken wir immer genau eine Version
        // mit. Und zwar genau die hoechste, die die Bank in den GV-spezifischen BPD
        // mitgeteilt hat
        addConstraint("suppformat", "formats.suppformat", this.getPainVersion().getURN());
        addConstraint("dummy", "allaccounts", "N");

        addConstraint("startdate", "startdate", this.getStartdate());
        addConstraint("enddate", "enddate", "");
        addConstraint("maxentries", "maxentries", "");
        addConstraint("offset", "offset", "");
    }

    /**
     * @return der Lowlevelname.
     */
    public static String getLowlevelName() {
        return "KUmsZeitCamt";
    }

    @Override
    protected SepaVersion getDefaultPainVersion() {
        return SepaVersion.CAMT_052_001_01;
    }

    @Override
    protected Type getPainType() {
        return Type.CAMT_052;
    }

    /**
     * Liefert das fruehest moegliche Startdatum fuer den Abruf der Umsaetze.
     * Im Gegensatz zur alten MT940-Version ist es jetzt bei CAMT offensichtlich
     * so, dass man (zumindest bei einigen Banken) nicht mehr pauschal das Start-Datum
     * weglassen kann und die Bank dann alles an Daten liefert. Zumindest bei der
     * Sparkasse kam dann die Fehlermeldung "9010:Abfrage uebersteigt gueltigen Zeitraum".
     * Also muessen wir - falls kein Startdatum angegeben ist (daher als Default-Wert)
     * selbst anhand der BPD herausfinden, was das Limit ist und dieses als Default-Wert
     * verwenden.
     *
     * @return das fruehest moegliche Startdatum fuer den Abruf der Umsaetze.
     */
    private String getStartdate() {
        Map<String, String> bpd = this.getJobRestrictions();
        String days = bpd.get("timerange");

        String date = "";
        if (days != null && days.length() > 0 && days.matches("[0-9]{1,4}")) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -Integer.parseInt(days));
            date = HBCIUtils.date2StringISO(cal.getTime());
        }
        log.info("earliest start date according to BPD: " + (date != null && date.length() > 0 ? date : "<none>"));
        return date;
    }

    @Override
    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> data = msgstatus.getData();
        GVRKUms result = (GVRKUms) jobResult;
        final String format = data.get(header + ".format");

        for (int i = 0; ; i++) {
            final String booked = data.get(header + ".booked." + HBCIUtils.withCounter("message", i));
            if (booked == null)
                break;

            try {
                // Im Prinzip wuerde es reichen, die verwendete CAMT-Version einmalig anhand
                // des uebergebenen camt-Deskriptors in "format" zu ermitteln. Aber es gibt
                // tatsaechlich Banken, die in der HBCI-Nachricht eine andere Version angeben,
                // als sie tatsaechlich senden. Siehe https://www.willuhn.de/bugzilla/show_bug.cgi?id=1806
                // Das betraf PAIN-Messages. Ich weiss nicht, ob das bei CAMT auch vorkommt.
                // Ich gehe aber auf Nummer sicher.
                final SepaVersion version = SepaVersion.choose(format, booked);
                ISEPAParser<List<BTag>> parser = SEPAParserFactory.get(version);

                log.debug("  parsing camt data: " + booked);
                result.camtBooked.add(booked);
                parser.parse(new ByteArrayInputStream(booked.getBytes(CommPinTan.ENCODING)), result.getDataPerDay());
                log.debug("  parsed camt data, entries: " + result.getFlatData().size());
            } catch (Exception e) {
                log.error("  unable to parse camt data: " + e.getMessage());
                throw new HBCI_Exception("Error parsing CAMT document", e);
            }
        }

        final String notbooked = data.get(header + ".notbooked");
        if (notbooked != null) {
            try {
                final SepaVersion version = SepaVersion.choose(format, notbooked);
                ISEPAParser<List<BTag>> parser = SEPAParserFactory.get(version);

                log.debug("  parsing unbooked camt data: " + notbooked);
                result.camtNotBooked.add(notbooked);
                parser.parse(new ByteArrayInputStream(notbooked.getBytes(CommPinTan.ENCODING)),
                    result.getDataPerDayUnbooked());
                log.debug("  parsed unbooked camt data, entries: " + result.getFlatDataUnbooked().size());
            } catch (Exception e) {
                log.error("  unable to parse unbooked camt data: " + e.getMessage());
                throw new HBCI_Exception("Error parsing CAMT document", e);
            }
        }
    }

    @Override
    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
