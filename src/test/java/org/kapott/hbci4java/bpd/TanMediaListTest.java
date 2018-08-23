package org.kapott.hbci4java.bpd;

import org.junit.Test;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.rewrite.Rewrite;
import org.kapott.hbci4java.AbstractTest;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class TanMediaListTest extends AbstractTest {

    @Test
    public void test() throws Exception {
        String data = getFile("bpd/bpd-tanmedialist.txt");

//	    Rewrite.setData("msgName","CustomMsg");
        // liste der rewriter erzeugen
        String rewriters_st = "";
        ArrayList<Rewrite> al = new ArrayList<>();
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
            newmsgstring = rewriters[i].incomingClearText(newmsgstring, null, "CustomMsg");
        }

        Message msg = new Message("CustomMsgRes", newmsgstring, null, Message.CHECK_SEQ, true);
        HashMap<String, String> ht = new HashMap<>();
        msg.extractValues(ht);
    }

}
