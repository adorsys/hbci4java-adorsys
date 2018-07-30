/**********************************************************************
 * $Source: /cvsroot/hibiscus/hbci4java/test/hbci4java/ddv/PCSCTest.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/24 21:59:37 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package org.kapott.hbci4java.sepa;

import org.junit.Assert;
import org.junit.Test;
import org.kapott.hbci.GV.generators.ISEPAGenerator;
import org.kapott.hbci.GV.generators.SEPAGeneratorFactory;
import org.kapott.hbci.sepa.PainVersion;
import org.kapott.hbci.sepa.PainVersion.Type;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * Testet das pure Generieren von Pain XML-Dateien - ohne HBCI-Context.
 */
public class TestPainGen {
    /**
     * Testet das Erstellen von SEPA-Ueberweisungen.
     *
     * @throws Exception
     */
    @Test
    public void test001() throws Exception {
        HashMap<String, String> props = new HashMap<>();
        props.put("src.bic", "ABCDEFAA123");
        props.put("src.iban", "DE1234567890");
        props.put("src.name", "Max Mustermann");
        props.put("dst.bic", "ABCDEFAA123");
        props.put("dst.iban", "DE0987654321");
        props.put("dst.name", "SEPAstian");
        props.put("btg.value", "100.00");
        props.put("btg.curr", "EUR");
        props.put("usage", "Verwendungszweck");
        props.put("sepaid", "abcde");
        props.put("endtoendid", "fghij");

        for (PainVersion version : PainVersion.getKnownVersions(Type.PAIN_001)) {
            // Der Test schlaegt automatisch fehl, wenn die Schema-Validierung nicht klappt
            ISEPAGenerator gen = SEPAGeneratorFactory.get("UebSEPA", version);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            gen.generate(props, bos, true);
        }
    }

    /**
     * Testet das Erstellen von SEPA-Lastschriften.
     *
     * @throws Exception
     */
    @Test
    public void test002() throws Exception {
        HashMap<String, String> props = new HashMap<>();
        props.put("src.bic", "ABCDEFAA123");
        props.put("src.iban", "DE1234567890");
        props.put("src.name", "Max Mustermann");
        props.put("dst.bic", "ABCDEFAA123");
        props.put("dst.iban", "DE0987654321");
        props.put("dst.name", "SEPAstian");
        props.put("btg.value", "100.00");
        props.put("btg.curr", "EUR");
        props.put("usage", "Verwendungszweck");
        props.put("sepaid", "abcde");
        props.put("endtoendid", "fghij");
        props.put("creditorid", "DE1234567890");
        props.put("mandateid", "0987654321");
        props.put("manddateofsig", "2013-11-23");
        props.put("amendmandindic", "false");
        props.put("sequencetype", "FRST");
        props.put("targetdate", "2013-11-30");
        props.put("type", "CORE");

        for (PainVersion version : PainVersion.getKnownVersions(Type.PAIN_008)) {
            // Der Test schlaegt automatisch fehl, wenn die Schema-Validierung nicht klappt
            ISEPAGenerator gen = SEPAGeneratorFactory.get("LastSEPA", version);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            gen.generate(props, bos, true);
        }
    }

    /**
     * Testet das Erstellen von SEPA-Multi-Ueberweisungen.
     *
     * @throws Exception
     */
    @Test
    public void test003() throws Exception {
        HashMap<String, String> props = new HashMap<>();
        props.put("src.bic", "ABCDEFAA123");
        props.put("src.iban", "DE1234567890");
        props.put("src.name", "Max Mustermann");
        props.put("sepaid", "abcde");

        props.put("dst[0].bic", "ABCDEFAA123");
        props.put("dst[0].iban", "DE0987654321");
        props.put("dst[0].name", "SEPAstian");
        props.put("btg[0].value", "100.00");
        props.put("btg[0].curr", "EUR");
        props.put("usage[0]", "Verwendungszweck");
        props.put("endtoendid[0]", "fghij");

        props.put("dst[1].bic", "ABCDEFBB456");
        props.put("dst[1].iban", "DE5432109876");
        props.put("dst[1].name", "BICole");
        props.put("btg[1].value", "150.00");
        props.put("btg[1].curr", "EUR");
        props.put("usage[1]", "Verwendungszweck 2");
        props.put("endtoendid[1]", "fghij");

        for (PainVersion version : PainVersion.getKnownVersions(Type.PAIN_001)) {
            // Der Test schlaegt automatisch fehl, wenn die Schema-Validierung nicht klappt
            ISEPAGenerator gen = SEPAGeneratorFactory.get("UebSEPA", version);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            gen.generate(props, bos, true);
        }
    }

    /**
     * Testet das Erstellen von SEPA-Multi-Lastschriften.
     *
     * @throws Exception
     */
    @Test
    public void test004() throws Exception {
        HashMap<String, String> props = new HashMap<>();
        props.put("src.bic", "ABCDEFAA123");
        props.put("src.iban", "DE1234567890");
        props.put("src.name", "Max Mustermann");
        props.put("sepaid", "abcde");
        props.put("sequencetype", "FRST");
        props.put("targetdate", "2013-11-30");
        props.put("type", "CORE");

        props.put("dst[0].bic", "ABCDEFAA123");
        props.put("dst[0].iban", "DE0987654321");
        props.put("dst[0].name", "SEPAstian");
        props.put("btg[0].value", "100.00");
        props.put("btg[0].curr", "EUR");
        props.put("mandateid[0]", "0987654321");
        props.put("manddateofsig[0]", "2013-11-23");
        props.put("usage[0]", "Verwendungszweck");
        props.put("amendmandindic[0]", "false");
        props.put("endtoendid[0]", "fghij");
        props.put("creditorid[0]", "DE1234567890");

        props.put("dst[1].bic", "ABCDEFBB456");
        props.put("dst[1].iban", "DE5432109876");
        props.put("dst[1].name", "BICole");
        props.put("btg[1].value", "150.00");
        props.put("btg[1].curr", "EUR");
        props.put("mandateid[1]", "5432109876");
        props.put("manddateofsig[1]", "2013-11-23");
        props.put("usage[1]", "Verwendungszweck 2");
        props.put("amendmandindic[1]", "false");
        props.put("endtoendid[1]", "fghij");
        props.put("creditorid[1]", "DE1234567890");

        for (PainVersion version : PainVersion.getKnownVersions(Type.PAIN_008)) {
            // Der Test schlaegt automatisch fehl, wenn die Schema-Validierung nicht klappt
            ISEPAGenerator gen = SEPAGeneratorFactory.get("LastSEPA", version);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            gen.generate(props, bos, true);
        }
    }

    /**
     * Testet das Mappen des SEPA-Type "B2B" auf die passende Enum.
     * Siehe https://www.willuhn.de/bugzilla/show_bug.cgi?id=1458
     *
     * @throws Exception
     */
    @Test
    public void test005() throws Exception {
        HashMap<String, String> props = new HashMap<>();
        props.put("src.bic", "ABCDEFAA123");
        props.put("src.iban", "DE1234567890");
        props.put("src.name", "Max Mustermann");
        props.put("dst.bic", "ABCDEFAA123");
        props.put("dst.iban", "DE0987654321");
        props.put("dst.name", "SEPAstian");
        props.put("btg.value", "100.00");
        props.put("btg.curr", "EUR");
        props.put("usage", "Verwendungszweck");
        props.put("sepaid", "abcde");
        props.put("endtoendid", "fghij");
        props.put("creditorid", "DE1234567890");
        props.put("mandateid", "0987654321");
        props.put("manddateofsig", "2013-11-23");
        props.put("amendmandindic", "false");
        props.put("sequencetype", "FRST");
        props.put("targetdate", "2013-11-30");
        props.put("type", "B2B");

        for (PainVersion version : PainVersion.getKnownVersions(Type.PAIN_008)) {
            // Der Test schlaegt automatisch fehl, wenn die Schema-Validierung nicht klappt
            ISEPAGenerator gen = SEPAGeneratorFactory.get("LastSEPA", version);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            gen.generate(props, bos, true);
        }
    }

    /**
     * Testet die korrekte Codierung von Umlauten im erzeugten PAIN-Dokument.
     *
     * @throws Exception
     */
    @Test
    public void test006() throws Exception {
        String umlaute = "üüüüüü";
        HashMap<String, String> props = new HashMap<>();
        props.put("src.bic", "ABCDEFAA123");
        props.put("src.iban", "DE1234567890");
        props.put("src.name", umlaute);
        props.put("dst.bic", "ABCDEFAA123");
        props.put("dst.iban", "DE0987654321");
        props.put("dst.name", "SEPAstian");
        props.put("btg.value", "100.00");
        props.put("btg.curr", "EUR");
        props.put("usage", "Verwendungszweck");
        props.put("sepaid", "abcde");
        props.put("endtoendid", "fghij");

        for (PainVersion version : PainVersion.getKnownVersions(Type.PAIN_001)) {
            ISEPAGenerator gen = SEPAGeneratorFactory.get("UebSEPA", version);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            gen.generate(props, bos, true);
            String xml = bos.toString(ISEPAGenerator.ENCODING);
            Assert.assertTrue(xml.contains(umlaute));
        }
    }

}
