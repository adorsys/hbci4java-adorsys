/**********************************************************************
 * $Source: /cvsroot/hibiscus/hbci4java/test/hbci4java/msg/TestBug1129.java,v $
 * $Revision: 1.1 $
 * $Date: 2012/03/06 23:18:26 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package org.kapott.hbci4java.msg;

import org.junit.Assert;
import org.junit.Test;
import org.kapott.hbci.GV.parsers.ISEPAParser;
import org.kapott.hbci.GV.parsers.SEPAParserFactory;
import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci4java.AbstractTest;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.Map.Entry;

/**
 * Tests fuer BUGZILLA 1806.
 */
public class TestBug1806 extends AbstractTest {
    /**
     * @throws Exception
     */
    @Test
    public void test001() throws Exception {
        String data = getFile("bugzilla-1806.txt");
        Message msg = new Message("CustomMsgRes", data, null, Message.CHECK_SEQ, true);

        HashMap<String, String> ht = new HashMap<String, String>();
        msg.extractValues(ht);

        List<String> keys = new ArrayList<String>(ht.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            if (!key.endsWith(".sepapain"))
                continue;

            ByteArrayInputStream bis = new ByteArrayInputStream(ht.get(key).getBytes(CommPinTan.ENCODING));
            SepaVersion version = SepaVersion.autodetect(bis);
            Assert.assertNotNull(version);
            ISEPAParser<List<Properties>> parser = SEPAParserFactory.get(version);

            bis.reset();

            List<Properties> sepaResults = new ArrayList<Properties>();
            parser.parse(bis, sepaResults);
            Assert.assertTrue(sepaResults.size() > 0);
            for (int i = 0; i < sepaResults.size(); ++i) {
                System.out.println("\nDatensatz: " + (i + 1));

                Properties props = sepaResults.get(i);
                for (Entry e : props.entrySet()) {
                    System.out.println(e.getKey() + ": " + e.getValue());
                }
            }
        }
    }
}
