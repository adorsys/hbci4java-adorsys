package org.kapott.hbci4java.msg;

import org.junit.Assert;
import org.junit.Test;
import org.kapott.hbci.manager.HBCIKernel;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.protocol.MultipleSyntaxElements;
import org.kapott.hbci4java.AbstractTest;

import java.util.HashMap;
import java.util.Hashtable;

/**
 * Testet den Workaround zum Abkuerzen multipler optionaler DEGs.
 * Siehe {@link MultipleSyntaxElements#initData}
 */
public class TestMultipleDEGs extends AbstractTest {

    /**
     * @throws Exception
     */
    @Test
    public void test() throws Exception {

        String data = getFile("msg/TestMultipleDEGs-01.txt");


        long start = System.currentTimeMillis();
        Message msg = new Message("DialogInitRes", data, data.length(), null, Message.CHECK_SEQ, true);
        HashMap<String, String> ht = new HashMap<>();
        msg.extractValues(ht);
        long end = System.currentTimeMillis();

//        List<String> keys = new ArrayList<String>(ht.keySet());
//        Collections.sort(keys);
//        for (String key:keys)
//        {
//            System.out.println(key + ": " + ht.get(key));
//        }
//
        // Das sollte unter 1 Sekunde dauern
        long used = end - start;
        System.out.println("used time: " + used + " millis");
        Assert.assertTrue("Sollte weniger als 1 Sekunde dauern", used < 1000);
    }

}
