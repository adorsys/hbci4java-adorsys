/*  $Id: Rewrite.java,v 1.1 2011/05/04 22:37:57 willuhn Exp $

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

package org.kapott.hbci.rewrite;

import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.w3c.dom.Document;

public abstract class Rewrite {

    protected Rewrite() {
    }

    public Message outgoingClearText(Message msg) {
        return msg;
    }

    public Message outgoingSigned(Message msg) {
        return msg;
    }

    public Message outgoingCrypted(Message msg) {
        return msg;
    }

    public String incomingCrypted(String st, HBCIMsgStatus msgStatus, String msgName) {
        return st;
    }

    public String incomingClearText(String st) {
        return st;
    }

    public String incomingClearText(String st, Document document, String msgName) {
        return st;
    }

    public Message incomingData(Message msg) {
        return msg;
    }
}
