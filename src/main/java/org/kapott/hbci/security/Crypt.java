/*  $Id: Crypt.java,v 1.1 2011/05/04 22:38:03 willuhn Exp $

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

package org.kapott.hbci.security;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.protocol.MultipleSyntaxElements;
import org.kapott.hbci.protocol.SEG;
import org.kapott.hbci.protocol.SyntaxElement;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Date;
import java.util.List;

@Slf4j
public final class Crypt {
    public final static String SECFUNC_ENC_3DES = "4";
    public final static String SECFUNC_ENC_PLAIN = "998";

    public final static String ENCALG_2K3DES = "13";

    public final static String ENCMODE_CBC = "2";
    public final static String ENCMODE_PKCS1 = "18";

    public final static String ENC_KEYTYPE_RSA = "6";
    public final static String ENC_KEYTYPE_DDV = "5";

    private HBCIPassportInternal passport;

    private String u_secfunc;    // 4=normal; 998=klartext
    private String u_keytype;    // 5=ddv, 6=rdh
    private String u_blz;        // schluesseldaten
    private String u_country;
    private String u_keyuserid;
    private String u_keynum;
    private String u_keyversion;
    private String u_cid;
    private String u_sysId;
    private String u_role;
    private String u_alg;       // crypthead.cryptalg.alg
    private String u_mode;      // crypthead.cryptalg.mode
    private String u_compfunc;

    public Crypt(HBCIPassportInternal passport) {
        this.passport = passport;
        init();
    }

    private void init() {
        u_secfunc = passport.getCryptFunction();
        u_keytype = passport.getCryptKeyType();
        u_blz = passport.getBLZ();
        u_country = passport.getCountry();
        u_keyuserid = passport.getInstEncKeyName();
        u_keynum = passport.getInstEncKeyNum();
        u_keyversion = passport.getInstEncKeyVersion();
        u_cid = "";
        u_sysId = passport.getSysId();
        u_role = "1";
        u_alg = passport.getCryptAlg();
        u_mode = passport.getCryptMode();
        u_compfunc = "0";// TODO: spaeter kompression implementieren
    }

    private byte[] getPlainString(Message msg) {
        try {
            // remove msghead and msgtail first
            StringBuffer ret = new StringBuffer(1024);
            List<MultipleSyntaxElements> childs = msg.getChildContainers();
            int len = childs.size();

            /* skip one segment at start and one segment at end of message
               (msghead and msgtail), the rest will be encrypted */
            for (int i = 1; i < len - 1; i++) {
                ret.append(childs.get(i).toString(0));
            }

            // pad message
            int padLength = 8 - (ret.length() % 8);
            for (int i = 0; i < padLength - 1; i++) {
                ret.append((char) (0));
            }
            ret.append((char) (padLength));

            return ret.toString().getBytes(CommPinTan.ENCODING);
        } catch (Exception ex) {
            throw new HBCI_Exception("*** error while extracting plain message string", ex);
        }
    }

    public Message cryptIt(Message msg) {
        Message newmsg = msg;

        if (passport.hasInstEncKey()) {
            Node msgNode = passport.getSyntaxDef(msg.getName());
            String dontcryptAttr = ((Element) msgNode).getAttribute("dontcrypt");

            if (dontcryptAttr.length() == 0) {
                newmsg = MessageFactory.createMessage("Crypted", passport.getSyntaxDocument());
                try {
                    byte[][] crypteds = passport.encrypt(getPlainString(msg));

                    String msgPath = msg.getPath();
                    String dialogid = msg.getValueOfDE(msgPath + ".MsgHead.dialogid");
                    String msgnum = msg.getValueOfDE(msgPath + ".MsgHead.msgnum");
                    String segnum = msg.getValueOfDE(msgPath + ".MsgTail.SegHead.seq");

                    Date d = new Date();

                    newmsg.set("Crypted.CryptData.data", "B" + new String(crypteds[1], CommPinTan.ENCODING));
                    newmsg.set("Crypted.CryptHead.CryptAlg.alg", u_alg);
                    newmsg.set("Crypted.CryptHead.CryptAlg.mode", u_mode);
                    newmsg.set("Crypted.CryptHead.CryptAlg.enckey", "B" + new String(crypteds[0], CommPinTan.ENCODING));
                    newmsg.set("Crypted.CryptHead.CryptAlg.keytype", u_keytype);
                    newmsg.set("Crypted.CryptHead.SecIdnDetails.func", (newmsg.getName().endsWith("Res") ? "2" : "1"));
                    newmsg.set("Crypted.CryptHead.KeyName.KIK.blz", u_blz);
                    newmsg.set("Crypted.CryptHead.KeyName.KIK.country", u_country);
                    newmsg.set("Crypted.CryptHead.KeyName.userid", u_keyuserid);
                    newmsg.set("Crypted.CryptHead.KeyName.keynum", u_keynum);
                    newmsg.set("Crypted.CryptHead.KeyName.keyversion", u_keyversion);
                    newmsg.set("Crypted.CryptHead.SecProfile.method", passport.getProfileMethod());
                    newmsg.set("Crypted.CryptHead.SecProfile.version", passport.getProfileVersion());
                    if (passport.getSysStatus().equals("0")) {
                        newmsg.set("Crypted.CryptHead.SecIdnDetails.cid", "B" + u_cid);
                    } else {
                        newmsg.set("Crypted.CryptHead.SecIdnDetails.sysid", u_sysId);
                    }
                    newmsg.set("Crypted.CryptHead.SecTimestamp.date", HBCIUtils.date2StringISO(d));
                    newmsg.set("Crypted.CryptHead.SecTimestamp.time", HBCIUtils.time2StringISO(d));
                    newmsg.set("Crypted.CryptHead.role", u_role);
                    newmsg.set("Crypted.CryptHead.secfunc", u_secfunc);
                    newmsg.set("Crypted.CryptHead.compfunc", u_compfunc);
                    newmsg.set("Crypted.MsgHead.dialogid", dialogid);
                    newmsg.set("Crypted.MsgHead.msgnum", msgnum);
                    newmsg.set("Crypted.MsgTail.msgnum", msgnum);
                    newmsg.complete();

                    // renumerate crypto-segments
                    for (int i = 1; i <= 2; i++) {
                        SEG seg = (SEG) newmsg.getChildContainers().get(i).getElements().get(0);
                        seg.setSeq(997 + i, SyntaxElement.ALLOW_OVERWRITE);
                    }

                    newmsg.propagateValue(newmsg.getPath() + ".MsgTail.SegHead.seq", segnum,
                        SyntaxElement.DONT_TRY_TO_CREATE,
                        SyntaxElement.ALLOW_OVERWRITE);
                    newmsg.autoSetMsgSize();
                } catch (Exception ex) {
                    throw new HBCI_Exception("*** error while encrypting", ex);
                }
            } else log.debug("did not encrypt - message does not want to be encrypted");
        } else log.warn("can not encrypt - no encryption key available");

        return newmsg;
    }

    public String decryptIt(Message message) {
        if (message.isCrypted()) {
            try {
                String msgName = message.getName();

                List<MultipleSyntaxElements> childs = message.getChildContainers();
                SEG msghead = (SEG) childs.get(0).getElements().get(0);
                SEG msgtail = (SEG) childs.get(childs.size() - 1).getElements().get(0);

                // verschluesselte daten extrahieren
                SEG cryptdata = (SEG) childs.get(2).getElements().get(0);
                byte[] cryptedstring = cryptdata.getValueOfDE(msgName + ".CryptData.data").getBytes(CommPinTan.ENCODING);

                // key extrahieren
                SEG crypthead = (SEG) childs.get(1).getElements().get(0);
                byte[] cryptedkey = crypthead.getValueOfDE(msgName +
                    ".CryptHead.CryptAlg.enckey").getBytes(CommPinTan.ENCODING);

                // neues secfunc (klartext/encrypted)
                String secfunc = crypthead.getValueOfDE(msgName + ".CryptHead.secfunc");
                if (!secfunc.equals(passport.getCryptFunction())) {
                    String errmsg = HBCIUtils.getLocMsg("EXCMSG_CRYPTSFFAIL", new Object[]{secfunc,
                        passport.getCryptFunction()});
                    throw new HBCI_Exception(errmsg);
                }

                // TODO spaeter kompression implementieren
                String compfunc = crypthead.getValueOfDE(msgName + ".CryptHead.compfunc");
                if (!compfunc.equals("0")) {
                    String errmsg = HBCIUtils.getLocMsg("EXCMSG_CRYPTCOMPFUNCFAIL", compfunc);
                    throw new HBCI_Exception(errmsg);
                }

                // TODO: hier auch die DEG SecProfile lesen und überprüfen

                byte[] plainMsg = passport.decrypt(cryptedkey, cryptedstring);
                int padLength = plainMsg[plainMsg.length - 1];

                // neuen nachrichtenstring zusammenbauen
                StringBuffer ret = new StringBuffer(1024);
                ret.append(msghead.toString(0)).
                    append(new String(plainMsg, 0, plainMsg.length - padLength, CommPinTan.ENCODING)).
                    append(msgtail.toString(0));

                log.debug("decrypted message: " + ret);

                return ret.toString();
            } catch (Exception ex) {
                throw new HBCI_Exception("*** error while decrypting", ex);
            }
        } else log.debug("did not decrypt - message is already cleartext");


        return message.toString(0);
    }
}
