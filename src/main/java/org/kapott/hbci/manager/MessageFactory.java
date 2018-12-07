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

import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.Message;
import org.w3c.dom.Document;

import java.util.Optional;

/* Message-Generator-Klasse. Diese Klasse verwaltet die Syntax-Spezifikation
 * für die zu verwendende HBCI-Version. Hiermit wird das Erzeugen von
 * HBCI-Nachrichten gekapselt.
 * Dazu wird eine Hashtable verwaltet, die die Daten enthält, die in die
 * jeweilige Nachricht aufgenommen werden sollen. Die Hashtable enthält als
 * Key den "Pfad" zum Datenelement (DialogInit.MsgHead.hbciversion), als
 * Value den einzustellenden Wert im Klartext.
 * Das Erzeugen einer Nachricht geschieht in drei Schritten:
 *   1) MsgGen.reset() -- vollständiges Leeren der Daten-Hashtable
 *   2) MsgGen.set(key,value) -- speichern eines Datums für die Nachricht
 *      in der Hashtable
 *   3) MsgGen.createMessage(msgName) -- erzeugen der Nachricht mit dem Namen
 *      <msgName>. Dabei werden auch nur die Daten aus der Hashtable
 *      verwendet, dir mit "<msgName>." beginnen (so dass in der Datenhashtable
 *      auch zusätzliche Daten gespeichert werden können, solange sie nicht
 *      mit "<msgName>." beginnen).*/
public final class MessageFactory {

    private static final HBCIProduct HBCI_PRODUCT = new HBCIProduct("36792786FA12F235F04647689", "3.2");

    public static Message createDialogInit(String msgName, String syncMode, HBCIPassportInternal passport) {

        Message message = createMessage(msgName, passport.getSyntaxDocument());
        message.rawSet("Idn.KIK.blz", passport.getBLZ());
        message.rawSet("Idn.KIK.country", passport.getCountry());
        message.rawSet("Idn.customerid", passport.getCustomerId());
        message.rawSet("Idn.sysid", passport.getSysId());
        message.rawSet("Idn.sysStatus", passport.getSysStatus());
        message.rawSet("ProcPrep.BPD", passport.getBPDVersion());
        message.rawSet("ProcPrep.UPD", passport.getUPDVersion());
        message.rawSet("ProcPrep.lang", passport.getDefaultLang());

        HBCIProduct hbciProduct = Optional.ofNullable(passport.getHbciProduct())
            .orElse(HBCI_PRODUCT);
        message.rawSet("ProcPrep.prodName", hbciProduct.getProduct());
        message.rawSet("ProcPrep.prodVersion", hbciProduct.getVersion());

        if (syncMode != null) {
            message.rawSet("Sync.mode", syncMode);
        }
        return message;
    }

    public static Message createAnonymouaDialogInit(HBCIPassportInternal passport) {
        Message message = createMessage("DialogInitAnon", passport.getSyntaxDocument());
        message.rawSet("Idn.KIK.blz", passport.getBLZ());
        message.rawSet("Idn.KIK.country", passport.getCountry());
        message.rawSet("ProcPrep.BPD", "0");
        message.rawSet("ProcPrep.UPD", passport.getUPDVersion());
        message.rawSet("ProcPrep.lang", "0");

        HBCIProduct hbciProduct = Optional.ofNullable(passport.getHbciProduct())
            .orElse(HBCI_PRODUCT);
        message.rawSet("ProcPrep.prodName", hbciProduct.getProduct());
        message.rawSet("ProcPrep.prodVersion", hbciProduct.getVersion());


        return message;
    }

    public static Message createDialogEnd(HBCIPassportInternal passport, String dialogid, long msgNumber) {
        Message message = MessageFactory.createMessage("DialogEnd", passport.getSyntaxDocument());
        message.rawSet("DialogEndS.dialogid", dialogid);
        message.rawSet("MsgHead.dialogid", dialogid);
        message.rawSet("MsgHead.msgnum", Long.toString(msgNumber));
        message.rawSet("MsgTail.msgnum", Long.toString(msgNumber));

        return message;
    }

    /**
     * @param msgName The name (i.e. XML-identifier for a MSGdef-node) of the message to be generated.
     * @return A new MSG object representing the generated message.
     * @internal
     * @brief Generates the HBCI message @p msgName.
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
