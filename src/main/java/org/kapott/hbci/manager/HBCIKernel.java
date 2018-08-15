/*  $Id: HBCIKernelImpl.java,v 1.1 2011/05/04 22:37:47 willuhn Exp $

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

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.exceptions.CanNotParseMessageException;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.rewrite.Rewrite;
import org.kapott.hbci.security.Crypt;
import org.kapott.hbci.security.Sig;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

@Slf4j
public final class HBCIKernel {

    public final static boolean SIGNIT = true;
    public final static boolean DONT_SIGNIT = false;
    public final static boolean CRYPTIT = true;
    public final static boolean DONT_CRYPTIT = false;

    private HBCIPassportInternal passport;
    private CommPinTan commPinTan;

    public HBCIKernel(HBCIPassportInternal passport) {
        this.passport = passport;

        this.commPinTan = new CommPinTan(passport.getHost(), passport.getCallback())
            .withProxy(passport.getProxy(), passport.getProxyUser(),
                passport.getProxyPass());
    }

    /*  Processes the current message (mid-level API).

        This method creates the message specified earlier by the methods rawNewJob() and
        rawSet(), signs and encrypts it using the values of @p inst, @p user, @p signit
        and @p crypit and sends it to server.

        After that it waits for the response, decrypts it, checks the signature of the
        received message and returns a Properties object, that contains as keys the
        pathnames of all elements of the received message, and as values the corresponding
        value of the element with that path

        bricht diese methode mit einer exception ab, so muss die aufrufende methode
        die nachricht komplett neu erzeugen.

        @param signit A boolean value specifying, if the message to be sent should be signed.
        @param cryptit A boolean value specifying, if the message to be sent should be encrypted.
        @return A Properties object that contains a path-value-pair for each dataelement of
                the received message. */
    public HBCIMsgStatus rawDoIt(Message message, boolean signit, boolean cryptit) {
        HBCIMsgStatus msgStatus = new HBCIMsgStatus();

        try {
            message.complete();

            log.debug("generating raw message " + message.getName());
            passport.getCallback().status(HBCICallback.STATUS_MSG_CREATE, message.getName());

            // liste der rewriter erzeugen
            ArrayList<Rewrite> rewriters = getRewriters(passport.getProperties().get("kernel.rewriter"));

            // alle rewriter durchlaufen und plaintextnachricht patchen
            for (Rewrite rewriter1 : rewriters) {
                message = rewriter1.outgoingClearText(message);
            }

            // wenn nachricht signiert werden soll
            if (signit) {
                message = signMessage(message, rewriters);
            }

            processMessage(message, msgStatus);

            String messageName = message.getName();

            // soll nachricht verschlüsselt werden?
            if (cryptit) {
                message = cryptMessage(message, rewriters);
            }

            sendMessage(message, messageName, msgStatus, rewriters);
        } catch (Exception e) {
            // TODO: hack to be able to "disable" HKEND response message analysis
            // because some credit institutes are buggy regarding HKEND responses
            String paramName = "client.errors.ignoreDialogEndErrors";
            if (message.getName().startsWith("DialogEnd")) {
                log.error(e.getMessage(), e);
                log.warn("error while receiving DialogEnd response - " +
                    "but ignoring it because of special setting");
            } else {
                msgStatus.addException(e);
            }
        }

        return msgStatus;
    }

    private void processMessage(Message message, HBCIMsgStatus msgStatus) {
        /* zu jeder SyntaxElement-Referenz (2:3,1)==(SEG:DEG,DE) den Pfad
           des jeweiligen Elementes speichern */
        HashMap<String, String> paths = new HashMap<>();
        message.getElementPaths(paths, null, null, null);
        msgStatus.addData(paths);

        /* für alle Elemente (Pfadnamen) die aktuellen Werte speichern,
           wie sie bei der ausgehenden Nachricht versandt werden */
        HashMap<String, String> current = new HashMap<>();
        message.extractValues(current);
        HashMap<String, String> origs = new HashMap<>();

        current.forEach((key, value) -> {
            origs.put("orig_" + key, value);
        });
        msgStatus.addData(origs);

        // zu versendene nachricht loggen
        String outstring = message.toString(0);
        log.debug("sending message: " + outstring);

        // max. nachrichtengröße aus BPD überprüfen
        int maxmsgsize = passport.getMaxMsgSizeKB();
        if (maxmsgsize != 0 && (outstring.length() >> 10) > maxmsgsize) {
            String errmsg = HBCIUtils.getLocMsg("EXCMSG_MSGTOOLARGE",
                new Object[]{Integer.toString(outstring.length() >> 10), Integer.toString(maxmsgsize)});
            throw new HBCI_Exception(errmsg);
        }
    }

    private ArrayList<Rewrite> getRewriters(String rewriters_st) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        ArrayList<Rewrite> rewriters = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(rewriters_st, ",");
        while (tok.hasMoreTokens()) {
            String rewriterName = tok.nextToken().trim();
            if (rewriterName.length() != 0) {
                Class cl = this.getClass().getClassLoader().loadClass("org.kapott.hbci.rewrite.R" +
                    rewriterName);
                Constructor con = cl.getConstructor((Class[]) null);
                rewriters.add((Rewrite) (con.newInstance((Object[]) null)));
            }
        }
        return rewriters;
    }

    private Message signMessage(Message message, List<Rewrite> rewriters) {
        log.debug("trying to insert signature");
        passport.getCallback().status(HBCICallback.STATUS_MSG_SIGN, null);

        // signatur erzeugen und an nachricht anhängen
        Sig sig = new Sig();

        if (!sig.signIt(message, passport)) {
            String errmsg = HBCIUtils.getLocMsg("EXCMSG_CANTSIGN");
            throw new HBCI_Exception(errmsg);
        }

        // alle rewrites erledigen, die *nach* dem hinzufügen der signatur stattfinden müssen
        for (Rewrite rewriter : rewriters) {
            message = rewriter.outgoingSigned(message);
        }
        return message;
    }

    private Message cryptMessage(Message message, List<Rewrite> rewriters) {
        log.debug("trying to encrypt message");
        passport.getCallback().status(HBCICallback.STATUS_MSG_CRYPT, null);

        // nachricht verschlüsseln
        Crypt crypt = new Crypt(passport);
        message = crypt.cryptIt(message);

        if (!message.getName().equals("Crypted")) {
            String errmsg = HBCIUtils.getLocMsg("EXCMSG_CANTCRYPT");
            throw new HBCI_Exception(errmsg);
        }

        // verschlüsselte nachricht patchen
        for (Rewrite rewriter : rewriters) {
            message = rewriter.outgoingCrypted(message);
        }

        log.debug("encrypted message to be sent: " + message.toString(0));

        return message;
    }

    private void sendMessage(Message message, String messageName, HBCIMsgStatus msgStatus, List<Rewrite> rewriters) {
        Message response = commPinTan.pingpong(message, messageName, rewriters, msgStatus);
        response = decryptMessage(rewriters, response, messageName + "Res");

        // alle patches für die plaintextnachricht durchlaufen
        for (Rewrite rewriter : rewriters) {
            response = rewriter.incomingData(response);
        }

        // daten aus nachricht in status-objekt einstellen
        log.debug("extracting data from received message");
        msgStatus.addData(response.getData());
        checkResponse(response);
        checkSig(response);
    }

    private void checkSig(Message message) {
        // überprüfen der signatur
        log.debug("looking for a signature");
        passport.getCallback().status(HBCICallback.STATUS_MSG_VERIFY, null);

        if (!new Sig().verify(message, passport)) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVSIG"));
        }
    }

    private void checkResponse(Message response) {
        // überprüfen einiger constraints, die in einer antwortnachricht eingehalten werden müssen
        String responsePath = response.getPath();
        String msgnum = response.getValueOfDE(responsePath + ".MsgHead.msgnum");
        String dialogid = response.getValueOfDE(responsePath + ".MsgHead.dialogid");
        String hbciversion = response.getValueOfDE(responsePath + ".MsgHead.hbciversion");

        String hbciversion2 = response.getValueOfDE(responsePath + ".MsgHead.hbciversion");
        if (!hbciversion2.equals(hbciversion))
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVVERSION", new Object[]{hbciversion2,
                hbciversion}));
        String msgnum2 = response.getValueOfDE(responsePath + ".MsgHead.msgnum");
        if (!msgnum2.equals(msgnum))
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVMSGNUM_HEAD", new Object[]{msgnum2, msgnum}));
        msgnum2 = response.getValueOfDE(responsePath + ".MsgTail.msgnum");
        if (!msgnum2.equals(msgnum))
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVMSGNUM_TAIL", new Object[]{msgnum2, msgnum}));
        String dialogid2 = response.getValueOfDE(responsePath + ".MsgHead.dialogid");
        if (!dialogid.equals("0") && !dialogid2.equals(dialogid))
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVDIALOGID", new Object[]{dialogid2, dialogid}));
        if (!dialogid.equals("0") && !response.getValueOfDE(responsePath + ".MsgHead.MsgRef.dialogid").equals(dialogid))
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVDIALOGID_REF"));
        if (!response.getValueOfDE(responsePath + ".MsgHead.MsgRef.msgnum").equals(msgnum))
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVMSGNUM_REF"));
    }

    private Message decryptMessage(List<Rewrite> rewriters, Message response, String responseMessageName) {
        // ist antwortnachricht verschlüsselt?
        if (response.getName().equals("CryptedRes")) {
            passport.getCallback().status(HBCICallback.STATUS_MSG_DECRYPT, null);

            // wenn ja, dann nachricht entschlüsseln
            log.debug("acquire crypt instance");
            Crypt crypt = new Crypt(passport);
            log.debug("decrypting using " + crypt);

            String responseString = crypt.decryptIt(response);

            // alle patches für die unverschlüsselte nachricht durchlaufen
            log.debug("rewriting message");
            for (Rewrite rewriter : rewriters) {
                log.debug("applying rewriter " + rewriter.getClass().getSimpleName());
                responseString = rewriter.incomingClearText(responseString);
            }
            log.debug("rewriting done");

            log.debug("decrypted message after rewriting: " + responseString);

            // nachricht als plaintextnachricht parsen
            try {
                passport.getCallback().status(HBCICallback.STATUS_MSG_PARSE, response.getName() + "Res");
                log.debug("message to pe parsed: " + response.toString(0));
                response = new Message(responseMessageName, responseString, passport.getSyntaxDocument(), Message.CHECK_SEQ, true);
            } catch (Exception ex) {
                throw new CanNotParseMessageException(HBCIUtils.getLocMsg("EXCMSG_CANTPARSE"), responseString, ex);
            }
        }

        log.debug("received message after decryption: " + response.toString(0));
        return response;
    }
}
