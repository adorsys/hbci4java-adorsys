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

import org.junit.*;
import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.callback.HBCICallbackConsole;
import org.kapott.hbci.manager.HBCIDialog;
import org.kapott.hbci.manager.HBCIJobFactory;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.protocol.SEG;
import org.kapott.hbci.structures.Value;
import org.kapott.hbci4java.AbstractTest;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Testet das Erstellen von SEPA-Basis-Lastschriften
 * <p>
 * Folgende Angaben sind für das Nachrichtenformat für SEPA-Lastschriften (pain.008) erforderlich:
 * - Art des Verfahrens (Basis- oder Firmen-Lastschrift, <LclInstrm>)
 * - Art der Lastschrift (einmalige, erste, wieder-kehrende, letzte Lastschrift,
 * <SeqTp>)
 * - Name des Zahlungsempfüngers (<Cdtr><Nm>)
 * - Glüubiger-Identifikationsnummer des Zahlungsempfüngers (<CdtrSchmeId>)
 * - IBAN des Zahlungskontos des Zahlungsempfüngers, auf dem die Gutschrift
 * vorgenommen werden soll (<CdtrAcct>)
 * - BIC des Kreditinstituts des Zahlungsempfüngers (<CdtrAgt>)
 * - Name des Zahlungspflichtigen (<Dbtr><Nm>)
 * - IBAN des Zahlungskontos des Zahlungspflichtigen (<DbtrAcct>)
 * - BIC des Kreditinstituts des Zahlungspflichtigen (<DbtrAgt>)
 * - Eindeutige Mandatsreferenz (<MndtId>)
 * - Datum der Unterschrift des SEPA-Lastschriftmandats, sofern dieses vom Zahlungspflichtigen erteilt wird, bzw. Datum der Mitteilung über die Weiternutzung einer Einzugsermüchtigung (<DtOfSgntr>)
 * - Hühe des Einzugsbetrags (<InstdAmt>)
 * - Angaben zum Verwendungszweck (<RmtInf>)
 * - Name der Referenzpartei des Zahlungspflichtigen (falls im SEPALastschriftmandat vorhanden, <UltmtDbtr>)
 * - Identifikationscode der Referenzpartei des Zahlungspflichtigen
 * (falls im SEPA-Lastschriftmandat vorhanden, <Dbtr><Id>)
 * - Fülligkeitsdatum des Einzugs (<ReqdColltnDt>)
 */
public class TestGVLastSEPA extends AbstractTest {
    private final static Map<Integer, String> settings = new HashMap<Integer, String>() {{
        // Demo-Konto bei der ApoBank
        put(HBCICallback.NEED_COUNTRY, "DE");
        put(HBCICallback.NEED_BLZ, "30060601");
        put(HBCICallback.NEED_CUSTOMERID, "0001956434");
        put(HBCICallback.NEED_FILTER, "Base64");
        put(HBCICallback.NEED_HOST, "hbcibanking.apobank.de/fints_pintan/receiver");
        put(HBCICallback.NEED_PASSPHRASE_LOAD, "test");
        put(HBCICallback.NEED_PASSPHRASE_SAVE, "test");
        put(HBCICallback.NEED_PORT, "443");
        put(HBCICallback.NEED_PT_PIN, "11111");
        put(HBCICallback.NEED_PT_TAN, "123456"); // hier geht jede 6-stellige Zahl
        put(HBCICallback.NEED_USERID, "0001956434");
        put(HBCICallback.NEED_PT_SECMECH, "900"); // wird IMHO nicht benoetigt, weil es beim Demo-Account eh nur dieses eine Verfahren gibt
        put(HBCICallback.NEED_CONNECTION, ""); // ignorieren
        put(HBCICallback.CLOSE_CONNECTION, ""); // ignorieren
    }};

    private static File dir = null;

    private PinTanPassport passport = null;
    private HBCIDialog dialog = null;

    /**
     * Testet das Erstellen einer SEPA-Basis-Lastschrift.
     *
     * @throws Exception
     */
    @Test
    public void test001() throws Exception {
        AbstractHBCIJob job = HBCIJobFactory.newJob("Ueb", dialog.getPassport());

        // wir nehmen einfach das erste verfuegbare Konto
        job.setParam("src", passport.getAccounts()[0]);
        job.setParam("dst", passport.getAccounts()[0]);
        job.setParam("btg", new Value(1L, "EUR"));
        job.setParam("usage", "test");
        job.setParam("name", "test");
        job.setParam("key", "51");

        dialog.addTask(job);

        SEG seg = job.createJobSegment(0);
        seg.validate();
        String msg = seg.toString(0);
        Assert.assertEquals("HKUEB:0:5+0001956434:EUR:280:30060601+0001956434:EUR:280:30060601+TEST++0,01:EUR+51++TEST'", msg);
    }

    /**
     * Erzeugt das Passport-Objekt.
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        HashMap<String, String> props = new HashMap<>();
        props.put("infoPoint.enabled", Boolean.FALSE.toString());

        props.put("client.passport.PinTan.filename", dir.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".pt");
        props.put("client.passport.PinTan.init", "1");
        props.put("client.passport.PinTan.checkcert", "0"); // Check der SSL-Zertifikate abschalten - brauchen wir nicht fuer den Test

        // falls noetig
        props.put("client.passport.PinTan.proxy", ""); // host:port
        props.put("client.passport.PinTan.proxyuser", "");
        props.put("client.passport.PinTan.proxypass", "");

        HBCICallback callback = new HBCICallbackConsole() {
            public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData) {
                // haben wir einen vordefinierten Wert?
                String value = settings.get(reason);
                if (value != null) {
                    retData.replace(0, retData.length(), value);
                    return;
                }

                // Ne, dann an Super-Klasse delegieren
                super.callback(reason, msg, datatype, retData);
            }
        };

//    HBCIUtils.init(props,callback);
        this.passport = (PinTanPassport) AbstractHBCIPassport.getInstance(new HBCICallbackConsole(), props, "PinTan");

        // init handler
        HBCIDialog dialog = new HBCIDialog(passport);


        // dump bpd
        // this.dump("BPD",this.passport.getBPD());

        // Liste der unterstuetzten Geschaeftsvorfaelle ausgeben
        // this.dump("Supported GV",this.handler.getSupportedLowlevelJobs());
    }

    /**
     * Erzeugt das Passport-Verzeichnis.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        dir = new File(tmpDir, "hbci4java-junit-" + System.currentTimeMillis());
        dir.mkdirs();
    }

    /**
     * Loescht das Passport-Verzeichnis.
     *
     * @throws Exception
     */
    @AfterClass
    public static void afterClass() throws Exception {
        if (!dir.delete())
            throw new Exception("unable to delete " + dir);
    }

    private void dump(String name, Properties props) {
        System.out.println("--- BEGIN: " + name + " -----");
        Iterator keys = props.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            System.out.println(key + ": " + props.get(key));
        }
        System.out.println("--- END: " + name + " -----");
    }

}
