/**********************************************************************
 * $Source: /cvsroot/hibiscus/hbci4java/test/hbci4java/bpd/HITANSTest.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/05/17 12:48:05 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package org.kapott.hbci4java.bpd;

import org.junit.Assert;
import org.junit.Test;
import org.kapott.hbci.manager.DocumentFactory;
import org.kapott.hbci.manager.HBCITwoStepMechanism;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci4java.AbstractTest;

import java.util.HashMap;

/**
 * Testet das Parsen der HITANS-Segmente aus den BPD.
 */
public class HITANSTest extends AbstractTest {

    int version = 0;

    /**
     * Liefert Pseudo-BPD aus der angegebenen Datei.
     *
     * @param file    der Dateiname.
     * @param version die HBCI-Version.
     * @return die Pseudo-BPD.
     * @throws Exception
     */
    private HashMap<String, String> getBPD(String file, String version) throws Exception {
        String data = getFile(file);

        Message msg = new Message("DialogInitAnonRes", data, DocumentFactory.createDocument(version),
            Message.CHECK_SEQ, true);
        HashMap<String, String> ht = new HashMap<>();
        msg.extractValues(ht);

        // Prefix abschneiden
        HashMap<String, String> bpd = new HashMap<>();
        ht.forEach((name, value) -> {
            if (name.startsWith("DialogInitAnonRes."))
                name = name.replace("DialogInitAnonRes.", "");
            if (name.startsWith("BPD."))
                name = name.replace("BPD.", "");
            bpd.put(name, value);
        });

        return bpd;
    }

    /**
     * Testet, dass das HITANS-Segment in Version 5 korrekt geladen wird.
     *
     * @throws Exception
     */
    @Test
    public void testHitans5() throws Exception {
        HashMap<String, String> bpd = getBPD("bpd2-formatted.txt", "300");

        bpd.forEach((name, value) -> {
            // Hoechste Versionsnummer holen. Die muss 5 sein
            if (name.contains("TAN2StepPar")) {
                System.out.println(name + " " + value);
                if (name.endsWith("SegHead.version")) {
                    int newVersion = Integer.parseInt(value);
                    if (newVersion > version)
                        version = newVersion;
                }
            }
        });
        Assert.assertEquals(version, 5);
    }

    /**
     * Testet das Ermitteln der TAN-Verfahren.
     *
     * @throws Exception
     */
    @Test
    public void testCurrentSecMechInfo() throws Exception {
        HashMap<String, String> bpd = getBPD("bpd/bpd2-formatted.txt", "300");
        PinTanPassport passport = new PinTanPassport(null, null, null, null);
        passport.setBPD(bpd);

        HBCITwoStepMechanism secmech = passport.getCurrentSecMechInfo();

        // secmech darf nicht null sein
        Assert.assertNotNull(secmech);

        // Das TAN-Verfahren 942 gibts in den BPD drei mal. In HITANS 5, 4 und 2.
        // Der Code muss die Version aus der aktuellsten Segment-Version liefern.
        Assert.assertEquals(secmech.getSegversion(), "5");
    }
}

/**********************************************************************
 * $Log: HITANSTest.java,v $
 * Revision 1.1  2011/05/17 12:48:05  willuhn
 * @N Unit-Tests
 *
 * Revision 1.1  2011-05-13 15:07:58  willuhn
 * @N Testcode fuer das Parsen der HITANS-Segmente
 *
 **********************************************************************/
