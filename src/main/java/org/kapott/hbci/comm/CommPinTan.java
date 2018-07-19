
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

import org.apache.commons.codec.binary.Base64;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.CanNotParseMessageException;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.ParseErrorException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MsgGen;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.rewrite.Rewrite;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

public final class CommPinTan {

    private MsgGen msgGen;
    private String rewriter;
    private HBCICallback callback;

    private URL url;
    private HttpURLConnection conn;

    public final static String ENCODING = "ISO-8859-1";

    //Needed for jackson
    public CommPinTan() {
    }

    // die socket factory, die in jedem fall benutzt wird.
    private SSLSocketFactory mySocketFactory;

    // der hostname-verifier, der nur dann benutzt wird, wenn zertifikate
    // nicht verifiziert werden sollen
    private HostnameVerifier myHostnameVerifier;

    /**
     * Timeout fuer HTTP connect in Millisekunden.
     */
    private final static int HTTP_CONNECT_TIMEOUT = 60 * 1000;

    /**
     * Timeout fuer HTTP Read in Millisekunden.
     */
    private final static int HTTP_READ_TIMEOUT = 5 * HTTP_CONNECT_TIMEOUT;
    
    public CommPinTan(MsgGen msgGen, String rewriter, String host, HBCICallback callback) {
        this.msgGen = msgGen;
        this.rewriter = rewriter;
        this.callback = callback;

        try {
            HBCIUtils.log("connect: " + host, HBCIUtils.LOG_INFO);
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
                HBCIUtils.log(
                        "HTTPS connections will be made using proxy " +
                                proxyData[0] + "(Port " + proxyData[1] + ")",
                        HBCIUtils.LOG_INFO);

                Properties sysProps = System.getProperties();
                sysProps.put("https.proxyHost", proxyData[0]);
                sysProps.put("https.proxyPort", proxyData[1]);

                HBCIUtils.log("initializing HBCI4Java proxy authentication callback", HBCIUtils.LOG_DEBUG);
                Authenticator.setDefault(new PinTanProxyAuthenticator(proxyUser, proxyPass));
            }
        }
        return this;
    }

    public MSG pingpong(Hashtable<String, Object> kernelData, String msgName, MSG msg) {
        HBCIUtils.log("---------------- request ----------------", HBCIUtils.LOG_DEBUG);
        msg.log(HBCIUtils.LOG_DEBUG);

        // ausgehende nachricht versenden
        callback.status(HBCICallback.STATUS_MSG_SEND, null);
        callback.status(HBCICallback.STATUS_MSG_RAW_SEND, msg.toString(0));
        ping(msg);

        // nachricht empfangen
        callback.status(HBCICallback.STATUS_MSG_RECV, null);
        String st = pong(msgGen).toString();
        callback.status(HBCICallback.STATUS_MSG_RAW_RECV, st);

        HBCIUtils.log("---------------- response ----------------", HBCIUtils.LOG_DEBUG);
        String[] split = st.split("'");
        for (int i = 0; i < split.length; i++) {
            HBCIUtils.log(split[i], HBCIUtils.LOG_DEBUG);
        }

        MSG retmsg = null;

        try {
            // erzeugen der liste aller rewriter
            ArrayList<Rewrite> al = new ArrayList<Rewrite>();
            StringTokenizer tok = new StringTokenizer(rewriter, ",");
            while (tok.hasMoreTokens()) {
                String rewriterName = tok.nextToken().trim();
                if (rewriterName.length() != 0) {
                    Class cl = this.getClass().getClassLoader().loadClass("org.kapott.hbci.rewrite.R" +
                            rewriterName);
                    Constructor con = cl.getConstructor((Class[]) null);
                    Rewrite rewriter = (Rewrite) (con.newInstance((Object[]) null));
                    rewriter.setKernelData(kernelData);
                    al.add(rewriter);
                }
            }
            Rewrite[] rewriters = al.toArray(new Rewrite[al.size()]);

            // alle rewriter für verschlüsselte nachricht durchlaufen
            for (int i = 0; i < rewriters.length; i++) {
                st = rewriters[i].incomingCrypted(st, msgGen);
            }

            // versuche, nachricht als verschlüsselte nachricht zu parsen
            callback.status(HBCICallback.STATUS_MSG_PARSE, "CryptedRes");
            try {
                HBCIUtils.log("trying to parse message as crypted message", HBCIUtils.LOG_DEBUG);
                retmsg = new MSG("CryptedRes", st, st.length(), msgGen, MSG.DONT_CHECK_SEQ, true);
            } catch (ParseErrorException e) {
                // wenn das schiefgeht...
                HBCIUtils.log("message seems not to be encrypted; tring to parse it as " + msgName + "Res message", HBCIUtils.LOG_DEBUG);

                // alle rewriter durchlaufen, um nachricht evtl. als unverschlüsselte msg zu parsen
                msgGen.set("_origSignedMsg", st);
                for (int i = 0; i < rewriters.length; i++) {
                    st = rewriters[i].incomingClearText(st, msgGen);
                }

                // versuch, nachricht als unverschlüsselte msg zu parsen
                callback.status(HBCICallback.STATUS_MSG_PARSE, msgName + "Res");
                retmsg = new MSG(msgName + "Res", st, st.length(), msgGen, MSG.CHECK_SEQ, true);
            }
        } catch (Exception ex) {
            throw new CanNotParseMessageException(HBCIUtils.getLocMsg("EXCMSG_CANTPARSE"), st, ex);
        }

        return retmsg;
    }

    protected void ping(MSG msg) {
        try {
            byte[] b = Base64.encodeBase64(msg.toString(0).getBytes(ENCODING));

            HBCIUtils.log("connecting to server", HBCIUtils.LOG_DEBUG);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
            conn.setReadTimeout(HTTP_READ_TIMEOUT);

            boolean debugging = ((PinTanSSLSocketFactory) this.mySocketFactory).debug();
            if (debugging) {
                // if we have to disable cert checking or enable ssl logging,
                // we have to set some special SSL stuff on the connection object
                HttpsURLConnection connSSL = (HttpsURLConnection) conn;

                HBCIUtils.log("activating modified socket factory for"
                                + " debugging=" + debugging,
                        HBCIUtils.LOG_DEBUG);
                connSSL.setSSLSocketFactory(this.mySocketFactory);

                HBCIUtils.log("activating modified hostname verifier because cert checking is disabled",
                        HBCIUtils.LOG_DEBUG);
                connSSL.setHostnameVerifier(this.myHostnameVerifier);
            }

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setFixedLengthStreamingMode(b.length);

            conn.connect();
            OutputStream out = conn.getOutputStream();

            HBCIUtils.log("POST data to output stream", HBCIUtils.LOG_DEBUG);
            out.write(b);
            out.flush();

            HBCIUtils.log("closing output stream", HBCIUtils.LOG_DEBUG);
            out.close();
        } catch (Exception e) {
            HBCI_Exception he = new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SENDERR"), e);
            he.setFatal(true); // Abbruch. Auch dann, wenn es ein anonymer BPD-Abruf war
            throw he;
        }
    }

    protected StringBuffer pong(MsgGen gen) {
        try {
            byte[] b = new byte[1024];
            StringBuffer ret = new StringBuffer();

            HBCIUtils.log(HBCIUtils.getLocMsg("STATUS_MSG_RECV"), HBCIUtils.LOG_DEBUG);

            int msgsize = conn.getContentLength();
            int num;

            if (msgsize != -1) {
                HBCIUtils.log("found messagesize: " + msgsize, HBCIUtils.LOG_DEBUG);
            } else {
                HBCIUtils.log("can not determine message size, trying to detect automatically", HBCIUtils.LOG_DEBUG);
            }
            InputStream i = conn.getInputStream();

            while (msgsize != 0 && (num = i.read(b)) > 0) {
                HBCIUtils.log("received " + num + " bytes", HBCIUtils.LOG_DEBUG2);
                ret.append(new String(b, 0, num, ENCODING));
                msgsize -= num;
                if (msgsize >= 0) {
                    HBCIUtils.log("we still need " + msgsize + " bytes", HBCIUtils.LOG_DEBUG2);
                } else {
                    HBCIUtils.log("read " + num + " bytes, looking for more", HBCIUtils.LOG_DEBUG2);
                }
            }

            HBCIUtils.log("closing communication line", HBCIUtils.LOG_DEBUG);
            conn.disconnect();
            return new StringBuffer(new String(Base64.decodeBase64(ret.toString()), ENCODING));
        } catch (Exception e) {
            // Die hier marieren wir nicht als fatal - ich meine mich zu erinnern,
            // dass es Banken gibt, die einen anonymen BPD-Abruf mit einem HTTP-Fehlercode quittieren
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_RECVERR"), e);
        }
    }

}
