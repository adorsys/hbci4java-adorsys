/*
 * $Id: PinTanSSLSocketFactory.java,v 1.1 2011/05/04 22:37:51 willuhn Exp $
 *
 * This file is part of HBCI4Java Copyright (C) 2001-2008 Stefan Palme
 *
 * HBCI4Java is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * HBCI4Java is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.kapott.hbci.comm;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;

/* This SSLSocketFactory will be used in certain circumstances as a drop-in
 * replacement for Java's standard SSLSocketFactory:
 *   - when certificate checking should be disabled (because we have to install
 *       an own SSLContext with a modified TrustManager in this case)
 *   - when we want to log the HTTP traffic going through the SSL socket
 *       (because in this case we have to create "LoggingSockets" instead of
 *       standard Java sockets)
 *
 * If none of this applies, Java's standard factory will be used.
 *
 * This socket factory works as a Delegator by creating a java standard socket
 * first and then delegating all relevant API calls to the standard socket
 * while hooking in some special code to log traffic etc...
 *
 * This socket factory CAN NOT BE USED WITH OPERA<10 because of a bug in operas
 * Java plugin, so when using this class in a Java Applet in Opera, the user
 * has to ENABLE certificate checking and DISABLE ssl-logging */
@Slf4j
public class PinTanSSLSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory realSocketFactory;

    public PinTanSSLSocketFactory() {
        try {
            log.warn("creating socket factory with disabled cert checking");

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null,
                new TrustManager[]{new PinTanSSLTrustManager()},
                new SecureRandom());
            this.realSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OutputStream getLogger() {
        return new HBCI4JavaLogOutputStream();
    }

    public Socket createSocket() throws IOException {
        log.debug("createSocket()");
        return this.realSocketFactory.createSocket();
    }

    public Socket createSocket(Socket sock, String host, int port, boolean autoClose)
        throws IOException {
        log.debug("createSocket(sock,host,port,autoClose)");
        return this.realSocketFactory.createSocket(sock, host, port, autoClose);
    }

    public String[] getDefaultCipherSuites() {
        return this.realSocketFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return this.realSocketFactory.getSupportedCipherSuites();
    }

    public Socket createSocket(String host, int port) throws IOException {
        log.debug("createSocket(host,port)");
        return this.realSocketFactory.createSocket(host, port);
    }

    public Socket createSocket(InetAddress addr, int port) throws IOException {
        log.debug("createSocket(addr,port)");
        return this.realSocketFactory.createSocket(addr, port);
    }

    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        log.debug("createSocket(host,port,localHost,localPort)");
        return this.realSocketFactory.createSocket(host, port, localHost, localPort);
    }

    public Socket createSocket(InetAddress addr, int port, InetAddress localHost, int localPort) throws IOException {
        log.debug("createSocket(addr,port,localHost,localPort)");
        return this.realSocketFactory.createSocket(addr, port, localHost, localPort);
    }
}
