
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

import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.IHandlerData;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

public final class Crypt {
    public final static String SECFUNC_ENC_3DES = "4";
    public final static String SECFUNC_ENC_PLAIN = "998";

    public final static String ENCALG_2K3DES = "13";

    public final static String ENCMODE_CBC = "2";
    public final static String ENCMODE_PKCS1 = "18";

    public final static String ENC_KEYTYPE_RSA = "6";
    public final static String ENC_KEYTYPE_DDV = "5";

    private IHandlerData hand2lerdata;
    private Message msg;

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

    public Crypt(Message msg) {
        this.msg = msg;
    }

    public void setParam(String name, String value) {
        try {
            Field field = this.getClass().getDeclaredField("u_" + name);
            HBCIUtils.log("setting " + name + " to " + value, HBCIUtils.LOG_DEBUG);
            field.set(this, value);
        } catch (Exception ex) {
            throw new HBCI_Exception("*** error while setting parameter", ex);
        }
    }

    private byte[] getPlainString() {
        try {
            // remove msghead and msgtail first
            StringBuffer ret = new StringBuffer(1024);
            List<MultipleSyntaxElements> childs = msg.getChildContainers();
            int len = childs.size();

            /* skip one segment at start and one segment at end of message
               (msghead and msgtail), the rest will be encrypted */
            for (int i = 1; i < len - 1; i++) {
                ret.append(childs.get(i).toString());
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

    public Message cryptIt(HBCIPassportInternal passport, Message message, String newName) {
        Message newmsg = msg;

        if (passport.hasInstEncKey()) {
            String msgName = msg.getName();
            Node msgNode = msg.getSyntaxDef(msgName, message.getDocument());
            String dontcryptAttr = ((Element) msgNode).getAttribute("dontcrypt");

            if (dontcryptAttr.length() == 0) {
                try {
                    setParam("secfunc", passport.getCryptFunction());
                    setParam("keytype", passport.getCryptKeyType());
                    setParam("blz", passport.getBLZ());
                    setParam("country", passport.getCountry());
                    setParam("keyuserid", passport.getInstEncKeyName());
                    setParam("keynum", passport.getInstEncKeyNum());
                    setParam("keyversion", passport.getInstEncKeyVersion());
                    setParam("cid", "");
                    setParam("sysId", passport.getSysId());
                    setParam("role", "1");
                    setParam("alg", passport.getCryptAlg());
                    setParam("mode", passport.getCryptMode());
                    setParam("compfunc", "0"); // TODO: spaeter kompression implementieren

                    byte[][] crypteds = passport.encrypt(getPlainString());

                    String msgPath = msg.getPath();
                    String dialogid = msg.getValueOfDE(msgPath + ".MsgHead.dialogid");
                    String msgnum = msg.getValueOfDE(msgPath + ".MsgHead.msgnum");
                    String segnum = msg.getValueOfDE(msgPath + ".MsgTail.SegHead.seq");

                    Date d = new Date();

                    message.set(newName + ".CryptData.data", "B" + new String(crypteds[1], CommPinTan.ENCODING));
                    message.set(newName + ".CryptHead.CryptAlg.alg", u_alg);
                    message.set(newName + ".CryptHead.CryptAlg.mode", u_mode);
                    message.set(newName + ".CryptHead.CryptAlg.enckey", "B" + new String(crypteds[0], CommPinTan.ENCODING));
                    message.set(newName + ".CryptHead.CryptAlg.keytype", u_keytype);
                    message.set(newName + ".CryptHead.SecIdnDetails.func", (newmsg.getName().endsWith("Res") ? "2" : "1"));
                    message.set(newName + ".CryptHead.KeyName.KIK.blz", u_blz);
                    message.set(newName + ".CryptHead.KeyName.KIK.country", u_country);
                    message.set(newName + ".CryptHead.KeyName.userid", u_keyuserid);
                    message.set(newName + ".CryptHead.KeyName.keynum", u_keynum);
                    message.set(newName + ".CryptHead.KeyName.keyversion", u_keyversion);
                    message.set(newName + ".CryptHead.SecProfile.method", passport.getProfileMethod());
                    message.set(newName + ".CryptHead.SecProfile.version", passport.getProfileVersion());
                    if (passport.getSysStatus().equals("0")) {
                        message.set(newName + ".CryptHead.SecIdnDetails.cid", "B" + u_cid);
                    } else {
                        message.set(newName + ".CryptHead.SecIdnDetails.sysid", u_sysId);
                    }
                    message.set(newName + ".CryptHead.SecTimestamp.date", HBCIUtils.date2StringISO(d));
                    message.set(newName + ".CryptHead.SecTimestamp.time", HBCIUtils.time2StringISO(d));
                    message.set(newName + ".CryptHead.role", u_role);
                    message.set(newName + ".CryptHead.secfunc", u_secfunc);
                    message.set(newName + ".CryptHead.compfunc", u_compfunc);
                    message.set(newName + ".MsgHead.dialogid", dialogid);
                    message.set(newName + ".MsgHead.msgnum", msgnum);
                    message.set(newName + ".MsgTail.msgnum", msgnum);

                    if (newName.endsWith("Res")) {
                        message.set(newName + ".MsgHead.MsgRef.dialogid", dialogid);
                        message.set(newName + ".MsgHead.MsgRef.msgnum", msgnum);
                    }

                    newmsg = MessageFactory.createMessage(newName, passport.getSyntaxDocument());

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
            } else HBCIUtils.log("did not encrypt - message does not want to be encrypted", HBCIUtils.LOG_DEBUG);
        } else HBCIUtils.log("can not encrypt - no encryption key available", HBCIUtils.LOG_WARN);

        return newmsg;
    }

    private boolean isCrypted() {
        boolean ret = true;
        MultipleSyntaxElements seglist = (msg.getChildContainers().get(1));

        if (seglist instanceof MultipleSEGs) {
            SEG crypthead = null;

            try {
                crypthead = (SEG) (seglist.getElements().get(0));
            } catch (Exception e) {
                ret = false;
            }

            if (ret) {
                String sigheadCode = "HNVSK";

                if (!crypthead.getCode().equals(sigheadCode))
                    ret = false;
            }
        } else ret = false;

        return ret;
    }

    public String decryptIt(HBCIPassportInternal passport) {
        StringBuffer ret = new StringBuffer(msg.toString());

        if (isCrypted()) {
            try {
                String msgName = msg.getName();

                List<MultipleSyntaxElements> childs = msg.getChildContainers();
                SEG msghead = (SEG) (((childs.get(0))).getElements().get(0));
                SEG msgtail = (SEG) (((childs.get(childs.size() - 1))).getElements().get(0));

                // verschluesselte daten extrahieren
                SEG cryptdata = (SEG) (((childs.get(2))).getElements().get(0));
                byte[] cryptedstring = cryptdata.getValueOfDE(msgName + ".CryptData.data").getBytes(CommPinTan.ENCODING);

                // key extrahieren
                SEG crypthead = (SEG) (((childs.get(1))).getElements().get(0));
                byte[] cryptedkey = crypthead.getValueOfDE(msgName +
                        ".CryptHead.CryptAlg.enckey").getBytes(CommPinTan.ENCODING);

                // neues secfunc (klartext/encrypted)
                String secfunc = crypthead.getValueOfDE(msgName + ".CryptHead.secfunc");
                if (!secfunc.equals(passport.getCryptFunction())) {
                    String errmsg = HBCIUtils.getLocMsg("EXCMSG_CRYPTSFFAIL", new Object[]{secfunc,
                            passport.getCryptFunction()});
                    if (!HBCIUtils.ignoreError(null, "client.errors.ignoreCryptErrors", errmsg))
                        throw new HBCI_Exception(errmsg);
                }

                // TODO spaeter kompression implementieren
                String compfunc = crypthead.getValueOfDE(msgName + ".CryptHead.compfunc");
                if (!compfunc.equals("0")) {
                    String errmsg = HBCIUtils.getLocMsg("EXCMSG_CRYPTCOMPFUNCFAIL", compfunc);
                    if (!HBCIUtils.ignoreError(null, "client.errors.ignoreCryptErrors", errmsg))
                        throw new HBCI_Exception(errmsg);
                }

                // TODO: hier auch die DEG SecProfile lesen und überprüfen

                byte[] plainMsg = passport.decrypt(cryptedkey, cryptedstring);
                int padLength = plainMsg[plainMsg.length - 1];

                // neuen nachrichtenstring zusammenbauen
                ret = new StringBuffer(1024);
                ret.append(msghead.toString()).
                        append(new String(plainMsg, 0, plainMsg.length - padLength, CommPinTan.ENCODING)).
                        append(msgtail.toString());

                HBCIUtils.log("decrypted message: " + ret, HBCIUtils.LOG_DEBUG2);
            } catch (Exception ex) {
                throw new HBCI_Exception("*** error while decrypting", ex);
            }
        } else HBCIUtils.log("did not decrypt - message is already cleartext", HBCIUtils.LOG_DEBUG);


        return ret.toString();
    }
}
