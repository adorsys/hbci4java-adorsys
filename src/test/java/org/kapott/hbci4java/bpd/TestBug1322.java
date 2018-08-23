/**********************************************************************
 *
 * Copyright (c) by Olaf Willuhn
 * All rights reserved
 *
 **********************************************************************/

package org.kapott.hbci4java.bpd;

import org.junit.Test;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci4java.AbstractTest;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Test fuer die neuen grossen BPD bei der GAD.
 * BUGZILLA 1322
 */
public class TestBug1322 extends AbstractTest {
    /**
     * Versucht, die BPD mit dem ueberlangen (mehr als 999 Zeichen) HIVISS Segment in der HBCI-Version
     * "FinTS3" zu parsen.
     *
     * @throws Exception
     */
    @Test
    public void test001() throws Exception {
        try {
            String data = getFile("bpd/bugzilla-1322.txt");

            Message msg = new Message("DialogInitAnonRes", data, null, Message.CHECK_SEQ, true);
            HashMap<String, String> ht = new HashMap<>();
            msg.extractValues(ht);

            // Wir checken noch, ob in der Testdatei tatsaechlich ein Segment mit
            // mehr als 999 Zeichen drin war. Wenn nicht, deckt die Testdatei
            // den Testfall gar nicht ab.

            Iterator<String> it = ht.values().iterator();
            while (it.hasNext()) {
                String value = it.next();
                if (value.length() > 999)
                    return;
            }

            throw new Exception("no BPD segment > 999 chars found");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}

