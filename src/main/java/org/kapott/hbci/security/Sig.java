/*  $Id: Sig.java,v 1.2 2012/03/27 21:33:13 willuhn Exp $

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
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Slf4j
public final class Sig {

    public final static String SECFUNC_HBCI_SIG_RDH = "1";
    public final static String SECFUNC_HBCI_SIG_DDV = "2";

    public final static String SECFUNC_FINTS_SIG_DIG = "1";
    public final static String SECFUNC_FINTS_SIG_SIG = "2";

    public final static String SECFUNC_SIG_PT_1STEP = "999";
    public final static String SECFUNC_SIG_PT_2STEP_MIN = "900";
    public final static String SECFUNC_SIG_PT_2STEP_MAX = "997";

    public final static String HASHALG_SHA1 = "1";
    public final static String HASHALG_SHA256 = "3";
    public final static String HASHALG_SHA384 = "4";
    public final static String HASHALG_SHA512 = "5";
    public final static String HASHALG_SHA256_SHA256 = "6";
    public final static String HASHALG_RIPEMD160 = "999";

    public final static String SIGALG_DES = "1";
    public final static String SIGALG_RSA = "10";

    public final static String SIGMODE_ISO9796_1 = "16";
    public final static String SIGMODE_ISO9796_2 = "17";
    public final static String SIGMODE_PKCS1 = "18";
    public final static String SIGMODE_PSS = "19";
    public final static String SIGMODE_RETAIL_MAC = "999";

    private String u_secfunc;
    private String u_cid;
    private String u_role;
    private String u_range;
    private String u_keyblz;
    private String u_keycountry;
    private String u_keyuserid;
    private String u_keynum;
    private String u_keyversion;
    private String u_sysid;
    private String u_sigid;
    private String u_sigalg;
    private String u_sigmode;
    private String u_hashalg;
    private String sigstring;

    public boolean signIt(Message msg, HBCIPassportInternal passport) {
        Node msgNode = msg.getSyntaxDef(msg.getName(), passport.getSyntaxDocument());
        String dontsignAttr = ((Element) msgNode).getAttribute("dontsign");

        if (dontsignAttr.length() == 0) {
            try {
                List<MultipleSyntaxElements> msgelements = msg.getChildContainers();
                List<SyntaxElement> sigheads = msgelements.get(1).getElements();
                List<SyntaxElement> sigtails = msgelements.get(msgelements.size() - 2).getElements();

                SEG sigHead = new SEG("SigHeadUser", "SigHead", msg.getName(), 0, passport.getSyntaxDocument());
                sigheads.set(0, sigHead);
                SEG sigTail = new SEG("SigTailUser", "SigTail", msg.getName(), 0, passport.getSyntaxDocument());
                sigtails.set(0, sigTail);

                u_secfunc = passport.getSigFunction();
                u_cid = "";
                u_role = "1";
                u_range = "1";
                u_keyblz = passport.getBLZ();
                u_keycountry = passport.getCountry();
                u_keyuserid = passport.getMySigKeyName();
                u_keynum = passport.getMySigKeyNum();
                u_keyversion = passport.getMySigKeyVersion();
                u_sysid = passport.getSysId();
                u_sigid = passport.getSigId().toString();
                u_sigalg = passport.getSigAlg();
                u_sigmode = passport.getSigMode();
                u_hashalg = passport.getHashAlg();
                passport.incSigId();

                fillSigHead(sigHead, passport.getProfileMethod(), passport.getProfileVersion(), msg.getName().endsWith("Res"));
                fillSigTail(sigHead, sigTail);

                msg.enumerateSegs(0, SyntaxElement.ALLOW_OVERWRITE);
                msg.validate();
                msg.enumerateSegs(1, SyntaxElement.ALLOW_OVERWRITE);

                msgelements = msg.getChildContainers();
                sigtails = msgelements.get(msgelements.size() - 2).getElements();
                sigTail = (SEG) sigtails.get(0);

                msg.propagateValue(sigTail.getPath() + ".UserSig.pin", passport.getPIN(),
                    SyntaxElement.DONT_TRY_TO_CREATE,
                    SyntaxElement.DONT_ALLOW_OVERWRITE);

                String tan = passport.getCallback().needTAN();
                if (tan != null) {
                    msg.propagateValue(sigTail.getPath() + ".UserSig.tan", tan,
                        SyntaxElement.DONT_TRY_TO_CREATE,
                        SyntaxElement.DONT_ALLOW_OVERWRITE);
                }

                msg.validate();
                msg.enumerateSegs(1, SyntaxElement.ALLOW_OVERWRITE);
                msg.autoSetMsgSize();
            } catch (Exception ex) {
                throw new HBCI_Exception("*** error while signing", ex);
            }
        } else log.debug("did not sign - message does not want to be signed");

        return true;
    }

    // sighead-segment mit werten aus den lokalen variablen füllen
    private void fillSigHead(SEG sighead, String profileMethod, String profileVersion, boolean response) {
        String sigheadName = sighead.getPath();
        String seccheckref = Integer.toString(Math.abs(new Random().nextInt()));

        Date d = new Date();

        sighead.propagateValue(sigheadName + ".secfunc", u_secfunc,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".seccheckref", seccheckref,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        /* TODO: enable this later (when other range types are supported)
             sighead.propagateValue(sigheadName+".range",range,false); */
        sighead.propagateValue(sigheadName + ".role", u_role,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SecIdnDetails.func", (response ? "2" : "1"),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        if (u_cid.length() != 0) {
            // DDV
            sighead.propagateValue(sigheadName + ".SecIdnDetails.cid", "B" + u_cid,
                SyntaxElement.DONT_TRY_TO_CREATE,
                SyntaxElement.DONT_ALLOW_OVERWRITE);
        } else {
            // RDH und PinTan
            sighead.propagateValue(sigheadName + ".SecIdnDetails.sysid", u_sysid,
                SyntaxElement.DONT_TRY_TO_CREATE,
                SyntaxElement.DONT_ALLOW_OVERWRITE);
        }
        sighead.propagateValue(sigheadName + ".SecTimestamp.date", HBCIUtils.date2StringISO(d),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SecTimestamp.time", HBCIUtils.time2StringISO(d),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.propagateValue(sigheadName + ".secref", u_sigid,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.propagateValue(sigheadName + ".HashAlg.alg", u_hashalg,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SigAlg.alg", u_sigalg,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SigAlg.mode", u_sigmode,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.propagateValue(sigheadName + ".KeyName.KIK.country", u_keycountry,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".KeyName.KIK.blz", u_keyblz,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".KeyName.userid", u_keyuserid,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".KeyName.keynum", u_keynum,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".KeyName.keyversion", u_keyversion,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);

        sighead.propagateValue(sigheadName + ".SecProfile.method", profileMethod,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
        sighead.propagateValue(sigheadName + ".SecProfile.version", profileVersion,
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
    }

    // sigtail-segment mit werten aus den lokalen variablen füllen
    private void fillSigTail(SEG sighead, SEG sigtail) {
        String sigtailName = sigtail.getPath();

        sigtail.propagateValue(sigtailName + ".seccheckref",
            sighead.getValueOfDE(sighead.getPath() + ".seccheckref"),
            SyntaxElement.DONT_TRY_TO_CREATE,
            SyntaxElement.DONT_ALLOW_OVERWRITE);
    }

    private void readSigHead(Message msg, HBCIPassportInternal passport) {
        String sigheadName = msg.getName() + ".SigHead";

        u_secfunc = msg.getValueOfDE(sigheadName + ".secfunc");
        u_role = msg.getValueOfDE(sigheadName + ".role");
        u_range = msg.getValueOfDE(sigheadName + ".range");
        u_keycountry = msg.getValueOfDE(sigheadName + ".KeyName.KIK.country");
        u_keyuserid = msg.getValueOfDE(sigheadName + ".KeyName.userid");
        u_keynum = msg.getValueOfDE(sigheadName + ".KeyName.keynum");
        u_keyversion = msg.getValueOfDE(sigheadName + ".KeyName.keyversion");
        u_sigid = msg.getValueOfDE(sigheadName + ".secref");
        u_sigalg = msg.getValueOfDE(sigheadName + ".SigAlg.alg");
        u_sigmode = msg.getValueOfDE(sigheadName + ".SigAlg.mode");
        u_hashalg = msg.getValueOfDE(sigheadName + ".HashAlg.alg");

        // Die Angabe der BLZ ist nicht unbedingt verpflichtend (für 280 aber schon...). Trotzdem gibt es wohl
        // Banken die das nicht interessiert...
        try {
            u_keyblz = msg.getValueOfDE(sigheadName + ".KeyName.KIK.blz");
        } catch (Exception e) {
            log.warn("missing bank code in message signature, ignoring...");
        }

        if (passport.needUserSig()) {
            // TODO: bei anderen user-signaturen hier allgemeineren code schreiben
            HashMap<String, String> values = new HashMap<>();
            msg.extractValues(values);

            String pin = values.get(msg.getName() + ".SigTail.UserSig.pin");
            String tan = values.get(msg.getName() + ".SigTail.UserSig.tan");

            sigstring = ((pin != null) ? pin : "") + "|" + ((tan != null) ? tan : "");
        } else {
            sigstring = msg.getValueOfDE(msg.getName() + ".SigTail.sig");
        }

        String checkref = msg.getValueOfDE(msg.getName() + ".SigHead.seccheckref");
        String checkref2 = msg.getValueOfDE(msg.getName() + ".SigTail.seccheckref");

        if (checkref == null || !checkref.equals(checkref2)) {
            String errmsg = HBCIUtils.getLocMsg("EXCMSG_SIGREFFAIL");
            throw new HBCI_Exception(errmsg);
        }
    }

    private boolean hasSig(Message msg) {
        boolean ret = true;
        MultipleSyntaxElements seglist = (msg.getChildContainers().get(1));

        if (seglist instanceof MultipleSEGs) {
            SEG sighead = null;
            try {
                /* TODO: multiple signatures not supported until now */
                sighead = (SEG) (seglist.getElements().get(0));
            } catch (IndexOutOfBoundsException e) {
                ret = false;
            }

            if (ret) {
                String sigheadCode = "HNSHK";

                if (!sighead.getCode().equals(sigheadCode))
                    ret = false;
            }
        } else ret = false;

        return ret;
    }

    public boolean verify(Message msg, HBCIPassportInternal passport) {
        if (passport.hasInstSigKey()) {
            String msgName = msg.getName();
            Node msgNode = msg.getSyntaxDef(msgName, passport.getSyntaxDocument());
            String dontsignAttr = ((Element) msgNode).getAttribute("dontsign");

            if (dontsignAttr.length() == 0) {
                if (hasSig(msg)) {
                    readSigHead(msg, passport);
                    return true;
                } else {
                    log.warn("message has no signature");
                    /* das ist nur für den fall, dass das institut prinzipiell nicht signiert
                       (also für den client-code);
                       die verify()-funktion für den server-code überprüft selbstständig, ob
                       tatsächlich eine benötigte signatur vorhanden ist (verlässt sich also nicht
                       auf dieses TRUE, was beim fehlen einer signatur zurückgegeben wird */
                    return true;
                }
            } else {
                log.debug("message does not need a signature");
                return true;
            }
        } else {
            log.warn("can not check signature - no signature key available");
            return true;
        }
    }

}
