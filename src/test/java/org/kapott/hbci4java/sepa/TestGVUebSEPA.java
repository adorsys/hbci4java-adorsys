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
import org.kapott.hbci.GV_Result.HBCIJobResult;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.callback.HBCICallbackConsole;
import org.kapott.hbci.manager.HBCIDialog;
import org.kapott.hbci.manager.HBCIJobFactory;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.structures.Value;
import org.kapott.hbci4java.AbstractTest;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Testet das Erstellen von SEPA-Basis-überweisung
 * <p>
 * Erforderliche Angaben für das Nachrichtenformat pain.001 (SEPA-überweisung)
 * Folgende Angaben sind für das Nachrichtenformat für SEPA-überweisungen (pain.001) erforderlich:
 * - Name des Zahlungspflichtigen (<Dbtr><Nm>)
 * - IBAN des Zahlungskontos des Zahlungspflichtigen (<DbtrAcct>)
 * - BIC des Kreditinstituts des Zahlungspflichtigen (<DbtrAgt>)
 * - überweisungsbetrag in Euro (<InstdAmt>)
 * - Angaben zum Verwendungszweck (<RmtInf>)
 * - Name des Zahlungsempfüngers (<Cdtr><Nm>)
 * - IBAN des Zahlungskontos des Zahlungsempfüngers (<CdtrAcct>)
 * - BIC des Kreditinstituts des Zahlungsempfüngers (<CdtrAgt>)
 * - Gegebenenfalls Identifikationscode des Zahlungsempfüngers (<Cdtr><Id>)
 * - Gegebenenfalls Name der Referenzpartei des Zahlungsempfüngers(<UltmtCdtr>)
 * - Gegebenenfalls Zweck der überweisung (<Purp>)
 */
public class TestGVUebSEPA extends AbstractTest {

    private final static Map<Integer, String> settings = new HashMap<Integer, String>() {{
        //TODO: Ein bisschen Geld auf folgendes Konto überweisen ;)
        put(HBCICallback.NEED_COUNTRY, "DE");
        put(HBCICallback.NEED_BLZ, "12030000");
        put(HBCICallback.NEED_CUSTOMERID, "1007318833");
        put(HBCICallback.NEED_FILTER, "Base64");
        put(HBCICallback.NEED_HOST, "hbci-pintan-by.s-hbci.de/PinTanServlet");
        put(HBCICallback.NEED_PASSPHRASE_LOAD, "test");
        put(HBCICallback.NEED_PASSPHRASE_SAVE, "test");
        put(HBCICallback.NEED_PORT, "443");
        put(HBCICallback.NEED_USERID, "1007318833");
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
        System.out.println("---------Erstelle Job");
        AbstractHBCIJob job = HBCIJobFactory.newJob("UebSEPA", dialog.getPassport());


//    //Mal schauen welche Konten ich habe
//    int i = 0;
//    for(Konto konto : passport.getAccounts()){
//    	System.out.println("Konto " + i +": " + konto);
//    	i++;
//    }

        job.setParam("src", passport.getAccounts()[2]);
        job.setParam("dst", passport.getAccounts()[0]);
        job.setParam("btg", new Value(100L, "EUR"));
        job.setParam("usage", "Hello SEPA Ueberweisung");


        System.out.println("---------Für Job zur Queue");
        dialog.addTask(job);


        HBCIExecStatus ret = dialog.execute(true);
        HBCIJobResult res = job.getJobResult();
        System.out.println("----------Result: " + res.toString());


        Assert.assertEquals("Job Result ist nicht OK!", true, res.isOK());


//    SEG seg = job.createJobSegment(0);
//    seg.validate();
//    String msg = seg.toString();
//    Assert.assertEquals("HKUEB:0:5+0001956434:EUR:280:30060601+0001956434:EUR:280:30060601+TEST++0,01:EUR+51++TEST'",msg);
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
//    this.dump("BPD",this.passport.getBPD());

        //Liste der unterstuetzten Geschaeftsvorfaelle ausgeben
//     this.dump("Supported GV",this.handler.getSupportedLowlevelJobs());
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
