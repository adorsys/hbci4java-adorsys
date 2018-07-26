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
import org.kapott.hbci.exceptions.ParseErrorException;
import org.kapott.hbci.manager.HBCIKernel;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci4java.AbstractTest;

import java.util.HashMap;
import java.util.Hashtable;

/**
 * Tests fuer BUGZILLA 1129.
 */
public class TestBug1129 extends AbstractTest {
    /**
     * Versucht, die Datei mit dem Response zu parsen.
     *
     * @return die geparsten Daten.
     * @throws Exception
     */
    private HashMap<String, String> parse() throws Exception {
        String data = getFile("msg/bugzilla-1129.txt");
                Message msg = new Message("CustomMsgRes", data, data.length(), null, Message.CHECK_SEQ, true);

        HashMap<String, String> ht = new HashMap<>();
        msg.extractValues(ht);
        return ht;
    }

    /**
     * Testet das Parsen eines Responses mit ungueltigem DTAUS ohne Fehlertoleranz.
     * Code muss einen Fehler werfen.
     *
     * @throws Exception
     */
    @Test
    public void test001() throws Exception {
        try {
            parse();
            throw new Exception("Test-Code haette eine Exception werfen muessen");
        } catch (Exception e) {
            Assert.assertEquals(ParseErrorException.class, e.getClass());
            Assert.assertTrue(((ParseErrorException) e).isFatal());
        }
    }

    /**
     * Testet das Parsen eines Responses mit ungueltigem DTAUS MIT Fehlertoleranz.
     *
     * @throws Exception
     */
    @Test
    public void test002() throws Exception {
        parse();
    }

    /**
     * Testet das Decodieren der DIN-66003 Umlaute.
     *
     * @throws Exception
     */
    @Test
    public void test003() throws Exception {
        HashMap<String, String> ht = parse();
        Assert.assertEquals("EBüHREN Z.T. IM VORAUS", ht.get("CustomMsgRes.GVRes_6.DauerListRes4.usage.usage_3"));
    }

}


/**********************************************************************
 * $Log: TestBug1129.java,v $
 * Revision 1.1  2012/03/06 23:18:26  willuhn
 * @N Patch 37 - BUGZILLA 1129
 *
 **********************************************************************/