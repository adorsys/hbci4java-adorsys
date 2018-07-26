package org.kapott.hbci4java.bpd;

import junit.framework.Assert;
import org.junit.Test;
import org.kapott.hbci.manager.HBCIKernel;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.rewrite.Rewrite;
import org.kapott.hbci4java.AbstractTest;

import java.lang.reflect.Constructor;
import java.util.*;

public class AllowedGVTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        String data = getFile("bpd/bpd-allowedgv.txt");
        HBCIKernel kernel = new HBCIKernel(null);

//	    Rewrite.setData("msgName","Synch");
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
        String newmsgstring = data;
        for (int i = 0; i < rewriters.length; i++) {
            newmsgstring = rewriters[i].incomingClearText(newmsgstring, null);
        }

        Message msg = new Message("SynchRes", newmsgstring, newmsgstring.length(), null, Message.CHECK_SEQ, true);
        HashMap<String, String> ht = new HashMap<>();
        msg.extractValues(ht);
    }

    @Test
    public void test2() throws Exception {
        String data = getFile("bpd/bpd-allowedgv2.txt");
        HBCIKernel kernel = new HBCIKernel(null);

//	        Rewrite.setData("msgName","Synch");
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
        String newmsgstring = data;
        for (int i = 0; i < rewriters.length; i++) {
            newmsgstring = rewriters[i].incomingClearText(newmsgstring, null);
        }

        Message msg = new Message("SynchRes", newmsgstring, newmsgstring.length(), null, Message.CHECK_SEQ, true);
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
