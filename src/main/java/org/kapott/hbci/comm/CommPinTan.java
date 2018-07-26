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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Properties;

@Slf4j
public final class CommPinTan {

    public final static String ENCODING = "ISO-8859-1";
    /**
     * Timeout fuer HTTP connect in Millisekunden.
     */
    private final static int HTTP_CONNECT_TIMEOUT = 60 * 1000;
    /**
     * Timeout fuer HTTP Read in Millisekunden.
     */
    private final static int HTTP_READ_TIMEOUT = 5 * HTTP_CONNECT_TIMEOUT;
    private String rewriter;
    private HBCICallback callback;
    private URL url;
    private HttpURLConnection conn;
    // die socket factory, die in jedem fall benutzt wird.
    private SSLSocketFactory mySocketFactory;
    // der hostname-verifier, der nur dann benutzt wird, wenn zertifikate
    // nicht verifiziert werden sollen
    private HostnameVerifier myHostnameVerifier;

    //Needed for jackson
    public CommPinTan() {
    }

    public CommPinTan(String rewriter, String host, HBCICallback callback) {
        this.rewriter = rewriter;
        this.callback = callback;

        try {
            log.info("connect: " + host);
            this.url = new URL(host);

            // creating instances of modified socket factories etc.
            this.mySocketFactory = new PinTanSSLSocketFactory();
            this.myHostnameVerifier = new PinTanSSLHostnameVerifier();
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
                Authenticator.setDefault(new PinTanProxyAuthenticator(proxyUser, proxyPass));
            }
        }
        return this;
    }

    public Message pingpong(List<Rewrite> rewriters, Message message) {
        log.debug("---------------- request ----------------");
        message.log();

        // ausgehende nachricht versenden
        callback.status(HBCICallback.STATUS_MSG_SEND, null);
        callback.status(HBCICallback.STATUS_MSG_RAW_SEND, message.toString(0));
        ping(message);

        // nachricht empfangen
        callback.status(HBCICallback.STATUS_MSG_RECV, null);
        String st = pong().toString();
        callback.status(HBCICallback.STATUS_MSG_RAW_RECV, st);

        log.debug("---------------- response ----------------");
        String[] split = st.split("'");
        for (String aSplit : split) {
            log.debug(aSplit);
        }

        Message responseMessage;

        try {
            // alle rewriter für verschlüsselte nachricht durchlaufen
            for (Rewrite rewriter1 : rewriters) {
                st = rewriter1.incomingCrypted(st);
            }
            // versuche, nachricht als verschlüsselte nachricht zu parsen
            callback.status(HBCICallback.STATUS_MSG_PARSE, "CryptedRes");
            try {
                log.debug("trying to parse message as crypted message");
                responseMessage = new Message("CryptedRes", st, st.length(), message.getDocument(), Message.DONT_CHECK_SEQ, true);
            } catch (ParseErrorException e) {
                // wenn das schiefgeht...
                log.debug("message seems not to be encrypted; tring to parse it as " + message.getName() + "Res message");

                // alle rewriter durchlaufen, um nachricht evtl. als unverschlüsselte msg zu parsen
//                message.set("_origSignedMsg", st);
                for (Rewrite rewriter1 : rewriters) {
                    st = rewriter1.incomingClearText(st);
                }

                // versuch, nachricht als unverschlüsselte msg zu parsen
                callback.status(HBCICallback.STATUS_MSG_PARSE, message.getName() + "Res");
                responseMessage = new Message(message.getName() + "Res", st, st.length(), message.getDocument(), Message.CHECK_SEQ, true);
            }
        } catch (Exception ex) {
            throw new CanNotParseMessageException(HBCIUtils.getLocMsg("EXCMSG_CANTPARSE"), st, ex);
        }

        return responseMessage;
    }

    private void ping(Message msg) {
        try {
            byte[] b = Base64.encodeBase64(msg.toString(0).getBytes(ENCODING));

            log.debug("connecting to server");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
            conn.setReadTimeout(HTTP_READ_TIMEOUT);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setFixedLengthStreamingMode(b.length);

            conn.connect();
            OutputStream out = conn.getOutputStream();

            log.debug("POST data to output stream");
            out.write(b);
            out.flush();

            log.debug("closing output stream");
            out.close();
        } catch (Exception e) {
            HBCI_Exception he = new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SENDERR"), e);
            he.setFatal(true); // Abbruch. Auch dann, wenn es ein anonymer BPD-Abruf war
            throw he;
        }
    }

    private StringBuffer pong() {
        try {
            byte[] b = new byte[1024];
            StringBuilder ret = new StringBuilder();

            log.debug(HBCIUtils.getLocMsg("STATUS_MSG_RECV"));

            int msgsize = conn.getContentLength();
            int num;

            if (msgsize != -1) {
                log.debug("found messagesize: " + msgsize);
            } else {
                log.debug("can not determine message size, trying to detect automatically");
            }
            InputStream i = conn.getInputStream();

            while (msgsize != 0 && (num = i.read(b)) > 0) {
                log.debug("received " + num + " bytes");
                ret.append(new String(b, 0, num, ENCODING));
                msgsize -= num;
                if (msgsize >= 0) {
                    log.debug("we still need " + msgsize + " bytes");
                } else {
                    log.debug("read " + num + " bytes, looking for more");
                }
            }

            log.debug("closing communication line");
            conn.disconnect();
            return new StringBuffer(new String(Base64.decodeBase64(ret.toString()), ENCODING));
        } catch (Exception e) {
            // Die hier marieren wir nicht als fatal - ich meine mich zu erinnern,
            // dass es Banken gibt, die einen anonymen BPD-Abruf mit einem HTTP-Fehlercode quittieren
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_RECVERR"), e);
        }
    }

}
