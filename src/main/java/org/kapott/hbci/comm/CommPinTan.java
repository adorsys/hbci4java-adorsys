/*  $Id: CommPinTan.java,v 1.1 2011/05/04 22:37:50 willuhn Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2008  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    HBCI4Java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.comm;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.CanNotParseMessageException;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.ParseErrorException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.rewrite.Rewrite;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Slf4j
public final class CommPinTan {

    public static final Charset ENCODING = StandardCharsets.ISO_8859_1;
    /**
     * Timeout fuer HTTP connect in Millisekunden.
     */
    private static final int HTTP_CONNECT_TIMEOUT = 60 * 1000;
    /**
     * Timeout fuer HTTP Read in Millisekunden.
     */
    private static final int HTTP_READ_TIMEOUT = 5 * HTTP_CONNECT_TIMEOUT;
    private HBCICallback callback;
    private URL url;
    private HttpURLConnection conn;

    public CommPinTan(String host, HBCICallback callback) {
        this.callback = callback;

        try {
            log.info("connect: " + host);
            this.url = new URL(host);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_CONNERR"), e);
        }
    }

    public CommPinTan withProxy(String proxyHost, String proxyUser, String proxyPass) {
        if (proxyHost != null) {
            String[] proxyData = proxyHost.split(":");
            if (proxyData.length == 2) {
                log.info(
                    "HTTPS connections will be made using proxy " +
                        proxyData[0] + "(Port " + proxyData[1] + ")");

                Properties sysProps = System.getProperties();
                sysProps.put("https.proxyHost", proxyData[0]);
                sysProps.put("https.proxyPort", proxyData[1]);

                log.debug("initializing HBCI4Java proxy authentication callback");
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
                    }
                });
            }
        }
        return this;
    }

    public Message pingpong(Message message, String messageName, List<Rewrite> rewriters, HBCIMsgStatus msgStatus) {
        log.trace("---------------- request ----------------");
        String rawMsg = message.toString(0);
        if (log.isTraceEnabled()) {
            Arrays.stream(rawMsg.split("'")).forEach(log::trace);
        }

        // ausgehende nachricht versenden
        callback.status(HBCICallback.STATUS_MSG_SEND, null);
        callback.status(HBCICallback.STATUS_MSG_RAW_SEND, rawMsg);
        ping(message);

        // nachricht empfangen
        callback.status(HBCICallback.STATUS_MSG_RECV, null);
        rawMsg = pong();
        callback.status(HBCICallback.STATUS_MSG_RAW_RECV, rawMsg);

//        try {
//            FileOutputStream fileOutputStream = new FileOutputStream(new File(messageName+System.currentTimeMillis()+".txt"));
//            IOUtils.write(rawMsg, fileOutputStream, ENCODING);
//            fileOutputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        log.trace("---------------- response ----------------");
        if (log.isTraceEnabled()) {
            Arrays.stream(rawMsg.split("'")).forEach(log::trace);
        }

        try {
            // alle rewriter für verschlüsselte nachricht durchlaufen
            for (Rewrite rewriter1 : rewriters) {
                rawMsg = rewriter1.incomingCrypted(rawMsg, msgStatus, messageName);
            }
            return parseResponseMessage(message, rawMsg, rewriters);
        } catch (Exception ex) {
            throw new CanNotParseMessageException(HBCIUtils.getLocMsg("EXCMSG_CANTPARSE"), rawMsg, ex);
        }
    }

    private Message parseResponseMessage(Message inputMessage, String rawResponse, List<Rewrite> rewriters) {
        Message responseMessage;

        // versuche, nachricht als verschlüsselte nachricht zu parsen
        callback.status(HBCICallback.STATUS_MSG_PARSE, "CryptedRes");
        try {
            log.trace("trying to parse message as crypted message");
            responseMessage = new Message("CryptedRes", rawResponse, inputMessage.getDocument(), Message.DONT_CHECK_SEQ,
                true);
        } catch (ParseErrorException e) {
            // wenn das schiefgeht...
            log.trace("message seems not to be encrypted; tring to parse it as " + inputMessage.getName() + "Res " + inputMessage);

            // alle rewriter durchlaufen, um nachricht evtl. als unverschlüsselte rawMsg zu parsen
            for (Rewrite rewriter1 : rewriters) {
                rawResponse = rewriter1.incomingClearText(rawResponse);
            }

            log.trace(rawResponse);
            // versuch, nachricht als unverschlüsselte rawMsg zu parsen
            callback.status(HBCICallback.STATUS_MSG_PARSE, inputMessage.getName() + "Res");
            try {
                responseMessage = new Message(inputMessage.getName() + "Res", rawResponse, inputMessage.getDocument(),
                    Message.CHECK_SEQ, true);
            } catch (ParseErrorException e2) {
                responseMessage = new Message("ErrorRes", rawResponse, inputMessage.getDocument(),
                    Message.CHECK_SEQ, true);
            }
        }

        return responseMessage;
    }

    private void ping(Message msg) {
        try {
            byte[] b = Base64.encodeBase64(msg.toString(0).getBytes(ENCODING));

            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
            conn.setReadTimeout(HTTP_READ_TIMEOUT);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setFixedLengthStreamingMode(b.length);

            conn.connect();
            OutputStream out = conn.getOutputStream();
            out.write(b);
            out.flush();
            out.close();
        } catch (Exception e) {
            HBCI_Exception he = new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SENDERR"), e);
            he.setFatal(true); // Abbruch. Auch dann, wenn es ein anonymer BPD-Abruf war
            throw he;
        }
    }

    private String pong() {
        try {
            byte[] b = new byte[1024];

            InputStream input = conn.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            int n;
            while (-1 != (n = input.read(b))) {
                output.write(b, 0, n);
            }

            conn.disconnect();

            return new String(Base64.decodeBase64(output.toByteArray()), ENCODING);
        } catch (Exception e) {
            // Die hier marieren wir nicht als fatal - ich meine mich zu erinnern,
            // dass es Banken gibt, die einen anonymen BPD-Abruf mit einem HTTP-Fehlercode quittieren
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_RECVERR"), e);
        }
    }

}
