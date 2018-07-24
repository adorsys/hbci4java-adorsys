
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
import java.util.*;

public final class HBCIKernel {

    public final static boolean SIGNIT = true;
    public final static boolean DONT_SIGNIT = false;
    public final static boolean CRYPTIT = true;
    public final static boolean DONT_CRYPTIT = false;
    public final static boolean NEED_SIG = true;
    public final static boolean DONT_NEED_SIG = false;
    public final static boolean NEED_CRYPT = true;
    public final static boolean DONT_NEED_CRYPT = false;

    private HBCIPassportInternal passport;
    private CommPinTan commPinTan;

    public HBCIKernel(HBCIPassportInternal passport) {
        this.passport = passport;

        this.commPinTan = new CommPinTan(passport.getProperties().getProperty("kernel.rewriter"), passport.getHost(), passport.getCallback())
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
    public HBCIMsgStatus rawDoIt(Message message, boolean signit, boolean cryptit, boolean needSig, boolean needCrypt) {
        message.complete();

        HBCIMsgStatus ret = new HBCIMsgStatus();

        try {
            HBCIUtils.log("generating raw message " + message.getName(), HBCIUtils.LOG_DEBUG);
            passport.getCallback().status(HBCICallback.STATUS_MSG_CREATE, message.getName());

            Hashtable<String, Object> kernelData = createKernelData(message.getName(), ret, signit, cryptit, needSig, needCrypt);

            // liste der rewriter erzeugen
            String rewriters_st = passport.getProperties().getProperty("kernel.rewriter");
            ArrayList<Rewrite> al = new ArrayList<>();
            StringTokenizer tok = new StringTokenizer(rewriters_st, ",");
            while (tok.hasMoreTokens()) {
                String rewriterName = tok.nextToken().trim();
                if (rewriterName.length() != 0) {
                    Class cl = this.getClass().getClassLoader().loadClass("org.kapott.hbci.rewrite.R" +
                            rewriterName);
                    Constructor con = cl.getConstructor((Class[]) null);
                    Rewrite rewriter = (Rewrite) (con.newInstance((Object[]) null));
                    // alle daten für den rewriter setzen
                    rewriter.setKernelData(kernelData);
                    al.add(rewriter);
                }
            }
            Rewrite[] rewriters = al.toArray(new Rewrite[0]);

            // alle rewriter durchlaufen und plaintextnachricht patchen
            for (Rewrite rewriter1 : rewriters) {
                Message old = message;
                message = rewriter1.outgoingClearText(old);
            }

            // wenn nachricht signiert werden soll
            if (signit) {
                HBCIUtils.log("trying to insert signature", HBCIUtils.LOG_DEBUG);
                passport.getCallback().status(HBCICallback.STATUS_MSG_SIGN, null);

                // signatur erzeugen und an nachricht anhängen
                Sig sig = new Sig(message);

                if (!sig.signIt(passport)) {
                    String errmsg = HBCIUtils.getLocMsg("EXCMSG_CANTSIGN");
                    if (!HBCIUtils.ignoreError(null, "client.errors.ignoreSignErrors", errmsg)) {
                        throw new HBCI_Exception(errmsg);
                    }
                }

                // alle rewrites erledigen, die *nach* dem hinzufügen der signatur stattfinden müssen
                for (Rewrite rewriter : rewriters) {
                    message = rewriter.outgoingSigned(message);
                }
            }
            
            /* zu jeder SyntaxElement-Referenz (2:3,1)==(SEG:DEG,DE) den Pfad
               des jeweiligen Elementes speichern */
            Properties paths = new Properties();
            message.getElementPaths(paths, null, null, null);
            ret.addData(paths);
            
            /* für alle Elemente (Pfadnamen) die aktuellen Werte speichern,
               wie sie bei der ausgehenden Nachricht versandt werden */
            Hashtable<String, String> current = new Hashtable<>();
            message.extractValues(current);
            Properties origs = new Properties();
            for (Enumeration<String> e = current.keys(); e.hasMoreElements(); ) {
                String key = e.nextElement();
                String value = current.get(key);
                origs.setProperty("orig_" + key, value);
            }
            ret.addData(origs);

            // zu versendene nachricht loggen
            String outstring = message.toString();
            HBCIUtils.log("sending message: " + outstring, HBCIUtils.LOG_DEBUG2);

            // max. nachrichtengröße aus BPD überprüfen
            int maxmsgsize = passport.getMaxMsgSizeKB();
            if (maxmsgsize != 0 && (outstring.length() >> 10) > maxmsgsize) {
                String errmsg = HBCIUtils.getLocMsg("EXCMSG_MSGTOOLARGE",
                        new Object[]{Integer.toString(outstring.length() >> 10), Integer.toString(maxmsgsize)});
                if (!HBCIUtils.ignoreError(null, "client.errors.ignoreMsgSizeErrors", errmsg))
                    throw new HBCI_Exception(errmsg);
            }

            // soll nachricht verschlüsselt werden?
            if (cryptit) {
                HBCIUtils.log("trying to encrypt message", HBCIUtils.LOG_DEBUG);
                passport.getCallback().status(HBCICallback.STATUS_MSG_CRYPT, null);

                // nachricht verschlüsseln
                Crypt crypt = new Crypt(message);
                message = crypt.cryptIt(passport, message, "Crypted");

                if (!message.getName().equals("Crypted")) {
                    String errmsg = HBCIUtils.getLocMsg("EXCMSG_CANTCRYPT");
                    if (!HBCIUtils.ignoreError(null, "client.errors.ignoreCryptErrors", errmsg))
                        throw new HBCI_Exception(errmsg);
                }

                // verschlüsselte nachricht patchen
                for (Rewrite rewriter : rewriters) {
                    message = rewriter.outgoingCrypted(message);
                }

                HBCIUtils.log("encrypted message to be sent: " + message.toString(), HBCIUtils.LOG_DEBUG2);
            }

            // basic-values der ausgehenden nachricht merken
            String msgPath = message.getPath();
            String msgnum = message.getValueOfDE(msgPath + ".MsgHead.msgnum");
            String dialogid = message.getValueOfDE(msgPath + ".MsgHead.dialogid");
            String hbciversion = message.getValueOfDE(msgPath + ".MsgHead.hbciversion");

            // nachricht versenden und antwortnachricht empfangen
            HBCIUtils.log("communicating dialogid/msgnum " + dialogid + "/" + msgnum, HBCIUtils.LOG_DEBUG);
            Message old = message;
            message = commPinTan.pingpong(kernelData, old);

            // ist antwortnachricht verschlüsselt?
            boolean crypted = message.getName().equals("CryptedRes");
            if (crypted) {
                passport.getCallback().status(HBCICallback.STATUS_MSG_DECRYPT, null);

                // wenn ja, dann nachricht entschlüsseln
                HBCIUtils.log("acquire crypt instance", HBCIUtils.LOG_DEBUG);
                Crypt crypt = new Crypt(message);
                HBCIUtils.log("decrypting using " + crypt, HBCIUtils.LOG_DEBUG);
                String newmsgstring = crypt.decryptIt(passport);
//                message.set("_origSignedMsg", newmsgstring);

                // alle patches für die unverschlüsselte nachricht durchlaufen
                HBCIUtils.log("rewriting message", HBCIUtils.LOG_DEBUG);
                for (Rewrite rewriter : rewriters) {
                    HBCIUtils.log("applying rewriter " + rewriter.getClass().getSimpleName(), HBCIUtils.LOG_DEBUG);
                    newmsgstring = rewriter.incomingClearText(newmsgstring);
                }
                HBCIUtils.log("rewriting done", HBCIUtils.LOG_DEBUG);

                HBCIUtils.log("decrypted message after rewriting: " + newmsgstring, HBCIUtils.LOG_DEBUG2);

                // nachricht als plaintextnachricht parsen
                try {
                    passport.getCallback().status(HBCICallback.STATUS_MSG_PARSE, message.getName() + "Res");
                    HBCIUtils.log("message to pe parsed: " + message.toString(), HBCIUtils.LOG_DEBUG2);
                    message = new Message(message.getName() + "Res", newmsgstring, newmsgstring.length(), message.getDocument(), Message.CHECK_SEQ, true);
                } catch (Exception ex) {
                    throw new CanNotParseMessageException(HBCIUtils.getLocMsg("EXCMSG_CANTPARSE"), newmsgstring, ex);
                }
            }

            HBCIUtils.log("received message after decryption: " + message.toString(), HBCIUtils.LOG_DEBUG2);

            // alle patches für die plaintextnachricht durchlaufen
            for (Rewrite rewriter : rewriters) {
                Message oldMsg = message;
                message = rewriter.incomingData(oldMsg);
            }

            // daten aus nachricht in status-objekt einstellen
            HBCIUtils.log("extracting data from received message", HBCIUtils.LOG_DEBUG);
            Properties p = message.getData();
//            p.setProperty("_msg", message.get("_origSignedMsg"));
            ret.addData(p);

            // überprüfen einiger constraints, die in einer antwortnachricht eingehalten werden müssen
            msgPath = message.getPath();
            try {
                String hbciversion2 = message.getValueOfDE(msgPath + ".MsgHead.hbciversion");
                if (!hbciversion2.equals(hbciversion))
                    throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVVERSION", new Object[]{hbciversion2,
                            hbciversion}));
                String msgnum2 = message.getValueOfDE(msgPath + ".MsgHead.msgnum");
                if (!msgnum2.equals(msgnum))
                    throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVMSGNUM_HEAD", new Object[]{msgnum2, msgnum}));
                msgnum2 = message.getValueOfDE(msgPath + ".MsgTail.msgnum");
                if (!msgnum2.equals(msgnum))
                    throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVMSGNUM_TAIL", new Object[]{msgnum2, msgnum}));
                String dialogid2 = message.getValueOfDE(msgPath + ".MsgHead.dialogid");
                if (!dialogid.equals("0") && !dialogid2.equals(dialogid))
                    throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVDIALOGID", new Object[]{dialogid2, dialogid}));
                if (!dialogid.equals("0") && !message.getValueOfDE(msgPath + ".MsgHead.MsgRef.dialogid").equals(dialogid))
                    throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVDIALOGID_REF"));
                if (!message.getValueOfDE(msgPath + ".MsgHead.MsgRef.msgnum").equals(msgnum))
                    throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_INVMSGNUM_REF"));
            } catch (HBCI_Exception e) {
                String errmsg = HBCIUtils.getLocMsg("EXCMSG_MSGCHECK") + ": " + HBCIUtils.exception2String(e);
                if (!HBCIUtils.ignoreError(passport, "client.errors.ignoreMsgCheckErrors", errmsg))
                    throw e;
            }

            // überprüfen der signatur
            HBCIUtils.log("looking for a signature", HBCIUtils.LOG_DEBUG);
            passport.getCallback().status(HBCICallback.STATUS_MSG_VERIFY, null);
            boolean sigOk;
            Sig sig = new Sig(message);
            sigOk = sig.verify(passport);

            // fehlermeldungen erzeugen, wenn irgendwelche fehler aufgetreten sind
            HBCIUtils.log("looking if message is encrypted", HBCIUtils.LOG_DEBUG);

            // fehler wegen falscher verschlüsselung
            if (needCrypt && !crypted) {
                String errmsg = HBCIUtils.getLocMsg("EXCMSG_NOTCRYPTED");
                if (!HBCIUtils.ignoreError(passport, "client.errors.ignoreCryptErrors", errmsg))
                    throw new HBCI_Exception(errmsg);
            }

            // signaturfehler
            if (!sigOk) {
                String errmsg = HBCIUtils.getLocMsg("EXCMSG_INVSIG");
                if (!HBCIUtils.ignoreError(null, "client.errors.ignoreSignErrors", errmsg))
                    throw new HBCI_Exception(errmsg);
            }
        } catch (Exception e) {
            // TODO: hack to be able to "disable" HKEND response message analysis
            // because some credit institutes are buggy regarding HKEND responses
            String paramName = "client.errors.ignoreDialogEndErrors";
            if (message.getName().startsWith("DialogEnd") &&
                    HBCIUtils.getParam(paramName, "no").equals("yes")) {
                HBCIUtils.log(e);
                HBCIUtils.log("error while receiving DialogEnd response - " +
                                "but ignoring it because of special setting",
                        HBCIUtils.LOG_WARN);
            } else {
                ret.addException(e);
            }
        }

        return ret;
    }

    private Hashtable<String, Object> createKernelData(String msgName, HBCIMsgStatus ret, boolean signit, boolean cryptit, boolean needSig, boolean needCrypt) {
        Hashtable<String, Object> kernelData = new Hashtable<>();
        kernelData.put("msgStatus", ret);
        kernelData.put("msgName", msgName);
        kernelData.put("signIt", signit);
        kernelData.put("cryptIt", cryptit);
        kernelData.put("needSig", needSig);
        kernelData.put("needCrypt", needCrypt);
        return kernelData;
    }

}
