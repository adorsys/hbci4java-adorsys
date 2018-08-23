/*  $Id: GVDauerList.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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

import org.kapott.hbci.GV.parsers.ISEPAParser;
import org.kapott.hbci.GV.parsers.SEPAParserFactory;
import org.kapott.hbci.GV_Result.GVRTermUebList;
import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;


public final class GVTermUebSEPAList extends AbstractSEPAGV {
    private final static SepaVersion DEFAULT = SepaVersion.PAIN_001_001_02;

    public GVTermUebSEPAList(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRTermUebList(passport));

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
        addConstraint("startdate", "startdate", "");
        addConstraint("enddate", "enddate", "");
        addConstraint("maxentries", "maxentries", "");
    }

    public static String getLowlevelName() {
        return "TermUebSEPAList";
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getDefaultPainVersion()
     */
    @Override
    protected SepaVersion getDefaultPainVersion() {
        return DEFAULT;
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getPainType()
     */
    @Override
    protected SepaVersion.Type getPainType() {
        return SepaVersion.Type.PAIN_001;
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRTermUebList.Entry entry = new GVRTermUebList.Entry();

        entry.my = new Konto();
        entry.my.country = result.get(header + ".My.KIK.country");
        entry.my.blz = result.get(header + ".My.KIK.blz");
        entry.my.number = result.get(header + ".My.number");
        entry.my.subnumber = result.get(header + ".My.subnumber");
        entry.my.iban = result.get(header + ".My.iban");
        entry.my.bic = result.get(header + ".My.bic");
        passport.fillAccountInfo(entry.my);

        entry.other = new Konto();

        final String sepadescr = result.get(header + ".sepadescr");
        final String pain = result.get(header + ".sepapain");
        final SepaVersion version = SepaVersion.choose(sepadescr, pain);

        ISEPAParser parser = SEPAParserFactory.get(version);
        ArrayList<HashMap<String, String>> sepaResults = new ArrayList<>();
        try {
            // Wir duerfen das hier nicht als UTF-8 interpretieren (das war vorher hier das Fall),
            // auch dann nicht, wenn wir genau wissen, dass das XML mit "<?xml version="1.0" encoding="UTF-8"?>"
            // beginnt. Stattdessen muessen wir den selben Zeichensatz nehmen, der bei der Byte->String Conversion
            // beim Empfamg der rohen HBCI-Daten ueber TCP verwendet wurde. Siehe CommStandard/CommPinTan.
            // Die eigentliche Codierung der XML-Datei spielt hier keine Rolle - wichtig ist, dass die
            // Rueckwandlung String->Bytes (in pain.getBytes) den selben Zeichensatz verwendet wie beim Empfang der
            // Daten vom Server. Nur so ist sichergestellt, dass die Bytes wieder genauso aussehen, wie sie
            // beim Empfang vom Server kamen, wenn der XML-Parser sie kriegt. Er macht dann die Conversion Byte->String
            // korrekt basierend auf dem im XML angegebenen Header.
            // Siehe auch AbstractSEPAGenerator#marshal
            parser.parse(new ByteArrayInputStream(pain.getBytes(CommPinTan.ENCODING)), sepaResults);
        } catch (Exception e) {
            throw new HBCI_Exception("Error parsing SEPA pain document", e);
        }

        if (sepaResults.isEmpty()) return;
        HashMap<String, String> separesult = sepaResults.get(0);
        entry.other.iban = separesult.get("dst.iban");
        entry.other.bic = separesult.get("dst.bic");
        entry.other.name = separesult.get("dst.name");
        entry.value = new Value(
            separesult.get("value"),
            separesult.get("curr"));
        entry.addUsage(separesult.get("usage"));

        entry.orderid = result.get(header + ".orderid");
        entry.date = HBCIUtils.string2DateISO(separesult.get("date"));

        entry.can_change = result.get(header + ".canchange") == null || result.get(header + ".canchange").equals("J");
        entry.can_delete = result.get(header + ".candel") == null || result.get(header + ".candel").equals("J");

        ((GVRTermUebList) (jobResult)).addEntry(entry);

        if (entry.orderid != null && entry.orderid.length() != 0) {
            HashMap<String, String> p2 = new HashMap();

            result.keySet().forEach(key -> {
                if (key.startsWith(header + ".") &&
                    !key.startsWith(header + ".SegHead.") &&
                    !key.endsWith(".orderid")) {
                    p2.put(key.substring(header.length() + 1),
                        result.get(key));
                }
            });

//TODO            passport.setPersistentData("termueb_" + entry.orderid, p2);
        }
    }

    public String getPainJobName() {
        return "UebSEPA";
    }

}
