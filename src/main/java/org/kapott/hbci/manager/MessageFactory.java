/*  $Id: MsgGen.java,v 1.1 2011/05/04 22:37:46 willuhn Exp $

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

package org.kapott.hbci.manager;

import lombok.experimental.UtilityClass;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.Message;
import org.w3c.dom.Document;

import java.util.Optional;

@UtilityClass
public final class MessageFactory {

    private static final HBCIProduct HBCI_PRODUCT = new HBCIProduct("36792786FA12F235F04647689", "3.2");

    public static Message createDialogInit(String dialogName, String syncMode, HBCIPassportInternal passport,
                                           boolean withHktan, String orderSegCode) {
        Message message = createMessage(dialogName, passport.getSyntaxDocument());
        message.rawSet("MsgHead.dialogid", "0");
        message.rawSet("MsgHead.msgnum", "1");
        message.rawSet("MsgTail.msgnum", "1");
        message.rawSet("Idn.KIK.blz", passport.getBLZ());
        message.rawSet("Idn.KIK.country", passport.getCountry());
        message.rawSet("Idn.customerid", passport.getCustomerId());
        message.rawSet("Idn.sysid", passport.getSysId());
        message.rawSet("Idn.sysStatus", passport.getSysStatus());
        message.rawSet("ProcPrep.BPD", passport.getBPDVersion());
        message.rawSet("ProcPrep.UPD", passport.getUPDVersion());
        message.rawSet("ProcPrep.lang", passport.getLang());

        HBCIProduct hbciProduct = Optional.ofNullable(passport.getHbciProduct())
            .orElse(HBCI_PRODUCT);
        message.rawSet("ProcPrep.prodName", hbciProduct.getProduct());
        message.rawSet("ProcPrep.prodVersion", hbciProduct.getVersion());

        if (withHktan) {
            int hktanVersion = passport.getCurrentSecMechInfo().getSegversion();

            message.rawSet("TAN2Step" + hktanVersion + ".process", "4");
            message.rawSet("TAN2Step" + hktanVersion + ".ordersegcode", orderSegCode);
            Optional.ofNullable(passport.getCurrentSecMechInfo().getMedium())
                .ifPresent(medium -> message.rawSet("TAN2Step" + hktanVersion + ".tanmedia", medium));
        }

        if (syncMode != null) {
            message.rawSet("Sync.mode", syncMode);
        }
        return message;
    }

    public static Message createAnonymousDialogInit(HBCIPassportInternal passport) {
        Message message = createMessage("DialogInitAnon", passport.getSyntaxDocument());
        message.rawSet("MsgHead.dialogid", "0");
        message.rawSet("MsgHead.msgnum", "1");
        message.rawSet("MsgTail.msgnum", "1");
        //HKIDN
        message.rawSet("Idn.KIK.blz", passport.getBLZ());
        message.rawSet("Idn.KIK.country", passport.getCountry());
        message.rawSet("Idn.customerid", "9999999999");
        message.rawSet("Idn.sysid", "0");
        message.rawSet("Idn.sysStatus", "0");
        //HKVVB
        message.rawSet("ProcPrep.BPD", "0");
        message.rawSet("ProcPrep.UPD", passport.getUPDVersion());
        message.rawSet("ProcPrep.lang", "0");
        HBCIProduct hbciProduct = Optional.ofNullable(passport.getHbciProduct())
            .orElse(HBCI_PRODUCT);
        message.rawSet("ProcPrep.prodName", hbciProduct.getProduct());
        message.rawSet("ProcPrep.prodVersion", hbciProduct.getVersion());
        //HKTAN
        message.rawSet("TAN2Step6.process", "4");
        message.rawSet("TAN2Step6.ordersegcode", "HKIDN");

        return message;
    }

    public static Message createDialogEnd(boolean anonymous, HBCIPassportInternal passport, String dialogid,
                                          long msgNumber) {
        Message message = MessageFactory.createMessage(anonymous ? "DialogEndAnon" : "DialogEnd",
            passport.getSyntaxDocument());
        message.rawSet("DialogEndS.dialogid", dialogid);
        message.rawSet("MsgHead.dialogid", dialogid);
        message.rawSet("MsgHead.msgnum", Long.toString(msgNumber));
        message.rawSet("MsgTail.msgnum", Long.toString(msgNumber));

        return message;
    }

    /**
     * @param msgName  The name (i.e. XML-identifier for a MSGdef-node) of the message to be generated.
     * @param document hbci-definition xml document
     * @return A new MSG object representing the generated message.
     * <p>
     * The document description for the message to be generated is taken from an
     * XML node @c MSGdef where the attribute @c id equals @p msgName.
     * <p>
     * To build the message the values stored in @c clientValues will be used.
     */
    public static Message createMessage(String msgName, Document document) {
        return new Message(msgName, document);
    }

}
