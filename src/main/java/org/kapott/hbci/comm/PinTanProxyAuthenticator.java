/*  $Id: PinTanProxyAuthenticator.java,v 1.1 2011/05/04 22:37:50 willuhn Exp $

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

import java.net.Authenticator;
import java.net.PasswordAuthentication;

@Slf4j
public class PinTanProxyAuthenticator extends Authenticator {

    private String proxyUser;
    private String proxyPassword;

    public PinTanProxyAuthenticator(String proxyUser, String proxyPassword) {
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        log.debug("need proxy authentication");

        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
    }

}
