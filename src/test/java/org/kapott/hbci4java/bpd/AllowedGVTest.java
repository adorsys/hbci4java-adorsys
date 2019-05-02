package org.kapott.hbci4java.bpd;

import org.junit.Assert;
import org.junit.Test;
import org.kapott.hbci.GV.GVDTAZV;
import org.kapott.hbci.GV.GVUebSEPA;
import org.kapott.hbci.manager.DocumentFactory;
import org.kapott.hbci.manager.HBCIKernel;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.rewrite.Rewrite;
import org.kapott.hbci4java.AbstractTest;
import org.w3c.dom.Document;

import java.lang.reflect.Constructor;
import java.util.*;

import static org.kapott.hbci4java.bpd.HITANSTest.getBPD;

public class AllowedGVTest extends AbstractTest {

//    @Test
    public void testBpdRaw() throws Exception {
        String data = getFile("bpd/bpd2-raw.txt");

        Document document = DocumentFactory.createDocument("300");

        Message message = new Message("SynchRes", data, document,
            Message.CHECK_SEQ, true);

        System.out.println();
    }

    @Test
    public void test() throws Exception {
        HashMap<String, String> bpd = getBPD("bpd/bpd2-formatted.txt", "300");
        PinTanPassport passport = new PinTanPassport("300", new HashMap<>(), null, null);
        passport.setHost("https://foo.bar");
        passport.setBPD(bpd);

        GVUebSEPA jobUebSEPA = new GVUebSEPA(passport);
        GVDTAZV job = new GVDTAZV(passport, GVDTAZV.getLowlevelName());
    }

    @Test
    public void test2() throws Exception {
        // liste der rewriter erzeugen
        String rewriters_st = "";
        ArrayList<Rewrite> al = new ArrayList<Rewrite>();
        StringTokenizer tok = new StringTokenizer(rewriters_st, ",");
        while (tok.hasMoreTokens()) {
            String rewriterName = tok.nextToken().trim();
            if (rewriterName.length() != 0) {
                Class cl = this.getClass().getClassLoader().loadClass("org.kapott.hbci.rewrite.R" +
                    rewriterName);
                Constructor con = cl.getConstructor((Class[]) null);
                Rewrite rewriter = (Rewrite) (con.newInstance((Object[]) null));
                al.add(rewriter);
            }
        }
        Rewrite[] rewriters = al.toArray(new Rewrite[al.size()]);

        // alle patches für die unverschlüsselte nachricht durchlaufen
        String newmsgstring = getFile("bpd/bpd-allowedgv2.txt");
        for (int i = 0; i < rewriters.length; i++) {
            newmsgstring = rewriters[i].incomingClearText(newmsgstring, null, "Synch");
        }

        Message msg = new Message("SynchRes", newmsgstring, null, Message.CHECK_SEQ, true);
        HashMap<String, String> ht = new HashMap<>();
        msg.extractValues(ht);

        Properties upd = new Properties();
        for (String key : ht.keySet()) {
            if (key.startsWith("SynchRes.UPD.") && key.contains(".code")) {
                String value = ht.get(key);
                key = key.replace("SynchRes.UPD.", "");
                upd.put(key, value);
            }
        }

        Set keys = upd.keySet();
        Assert.assertEquals(keys.contains("KInfo.AllowedGV_2.code"), true);
    }
}
