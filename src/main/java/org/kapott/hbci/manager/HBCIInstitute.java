/*  $Id: HBCIInstitute.java,v 1.1 2011/05/04 22:37:46 willuhn Exp $

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
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.exceptions.ProcessException;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

/* @brief Class representing an HBCI institute.

    It it responsible for storing institute-specific-data (the BPD,
    the signature and encryption keys etc.) and for providing
    a Comm object for making communication with the institute */
@Slf4j
public final class HBCIInstitute implements IHandlerData {

    private final static String BPD_KEY_LASTUPDATE = "_lastupdate";
    private final static String BPD_KEY_HBCIVERSION = "_hbciversion";

    private static final String maxAge = "7";

    private HBCIPassportInternal passport;
    private HBCIKernel kernel;

    public HBCIInstitute(HBCIKernel kernel, HBCIPassportInternal passport) {
        this.kernel = kernel;
        this.passport = passport;
    }

    /**
     * gets the BPD out of the result and store it in the
     * passport field
     */
    void updateBPD(HashMap<String, String> result) {
        log.debug("extracting BPD from results");
        HashMap<String, String> p = new HashMap<>();

        result.keySet().forEach(key -> {
            if (key.startsWith("BPD.")) {
                p.put(key.substring(("BPD.").length()), result.get(key));
            }
        });

        if (p.size() != 0) {
            p.put(BPD_KEY_HBCIVERSION, passport.getHBCIVersion());
            p.put(BPD_KEY_LASTUPDATE, String.valueOf(System.currentTimeMillis()));
            passport.setBPD(p);
            log.info("installed new BPD with version " + passport.getBPDVersion());
            passport.getCallback().status(HBCICallback.STATUS_INST_BPD_INIT_DONE, passport.getBPD());
        }
    }

    /**
     * gets the server public keys from the result and store them in the passport
     */
    void extractKeys(HashMap<String, String> result) {
        boolean foundChanges = false;

        try {
            log.debug("extracting public institute keys from results");

            for (int i = 0; i < 3; i++) {
                String head = HBCIUtils.withCounter("SendPubKey", i);
                String keyType = result.get(head + ".KeyName.keytype");
                if (keyType == null)
                    continue;

                String keyCountry = result.get(head + ".KeyName.KIK.country");
                String keyBLZ = result.get(head + ".KeyName.KIK.blz");
                String keyUserId = result.get(head + ".KeyName.userid");
                String keyNum = result.get(head + ".KeyName.keynum");
                String keyVersion = result.get(head + ".KeyName.keyversion");

                log.info("found key " +
                        keyCountry + "_" + keyBLZ + "_" + keyUserId + "_" + keyType + "_" +
                        keyNum + "_" + keyVersion);

                byte[] keyExponent = result.get(head + ".PubKey.exponent").getBytes(CommPinTan.ENCODING);
                byte[] keyModulus = result.get(head + ".PubKey.modulus").getBytes(CommPinTan.ENCODING);

                KeyFactory fac = KeyFactory.getInstance("RSA");
                KeySpec spec = new RSAPublicKeySpec(new BigInteger(+1, keyModulus),
                        new BigInteger(+1, keyExponent));
                Key key = fac.generatePublic(spec);

                if (keyType.equals("S")) {
                    passport.setInstSigKey(new HBCIKey(keyCountry, keyBLZ, keyUserId, keyNum, keyVersion, key));
                    foundChanges = true;
                } else if (keyType.equals("V")) {
                    passport.setInstEncKey(new HBCIKey(keyCountry, keyBLZ, keyUserId, keyNum, keyVersion, key));
                    foundChanges = true;
                }
            }
        } catch (Exception e) {
            String msg = HBCIUtils.getLocMsg("EXCMSG_EXTR_IKEYS_ERR");
            throw new HBCI_Exception(msg, e);
        }

        if (foundChanges) {
            passport.getCallback().status(HBCICallback.STATUS_INST_GET_KEYS_DONE, null);
        }
    }

    /**
     * Prueft, ob die BPD abgelaufen sind und neu geladen werden muessen.
     *
     * @return true, wenn die BPD abgelaufen sind.
     */
    private boolean isBPDExpired() {
        HashMap<String, String> bpd = passport.getBPD();
        log.info("[BPD] max age: " + maxAge + " days");

        long maxMillis = -1L;
        try {
            int days = Integer.parseInt(maxAge);
            if (days == 0) {
                log.info("[BPD] auto-expiry disabled");
                return false;
            }

            if (days > 0)
                maxMillis = days * 24 * 60 * 60 * 1000L;
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
            return false;
        }

        long lastUpdate = 0L;
        if (bpd != null) {
            String lastUpdateProperty = bpd.get(BPD_KEY_LASTUPDATE);
            try {
                lastUpdate = lastUpdateProperty != null ? Long.parseLong(lastUpdateProperty) : lastUpdate;
            } catch (NumberFormatException e) {
                log.error(e.getMessage(), e);
                return false;
            }
            log.info("[BPD] last update: " + (lastUpdate == 0 ? "never" : new Date(lastUpdate)));
        }

        long now = System.currentTimeMillis();
        if (maxMillis < 0 || (now - lastUpdate) > maxMillis) {
            log.info("[BPD] expired, will be updated now");
            return true;
        }

        return false;
    }

    /**
     * Aktualisiert die BPD bei Bedarf.
     */
    public void fetchBPDAnonymous() {
        // BPD abholen, wenn nicht vorhanden oder HBCI-Version geaendert
        HashMap<String, String> bpd = passport.getBPD();
        String hbciVersionOfBPD = (bpd != null) ? bpd.get(BPD_KEY_HBCIVERSION) : null;

        final String version = passport.getBPDVersion();
        if (version.equals("0") || isBPDExpired() || hbciVersionOfBPD == null || !hbciVersionOfBPD.equals(passport.getHBCIVersion())) {
            try {
                // Wenn wir die BPD per anonymem Dialog neu abrufen, muessen wir sicherstellen,
                // dass die BPD-Version im Passport auf "0" zurueckgesetzt ist. Denn wenn die
                // Bank den anonymen Abruf nicht unterstuetzt, wuerde dieser Abruf hier fehlschlagen,
                // der erneute Versuch mit authentifiziertem Dialog wuerde jedoch nicht zum
                // Neuabruf der BPD fuehren, da dort (in HBCIUser#fetchUPD bzw. HBCIDialog#doDialogInit)
                // weiterhin die (u.U. ja noch aktuelle) BPD-Version an die Bank geschickt wird
                // und diese daraufhin keine neuen BPD schickt. Das wuerde in einer endlosen
                // Schleife enden, in der wir hier immer wieder versuchen wuerden, neu abzurufen
                // (weil expired). Siehe https://www.willuhn.de/bugzilla/show_bug.cgi?id=1567
                // Also muessen wir die BPD-Version auf 0 setzen. Fuer den Fall, dass wir in dem
                // "if" hier aus einem der anderen beiden o.g. Gruende (BPD-Expiry oder neue HBCI-Version)
                // gelandet sind.
                if (!version.equals("0")) {
                    log.info("resetting BPD version from " + version + " to 0");
                    passport.getBPD().put("BPA.version", "0");
                }

                passport.getCallback().status(HBCICallback.STATUS_INST_BPD_INIT, null);
                log.info("fetching BPD");

                HBCIMsgStatus msgStatus = anonymousDialogInit();

                HashMap<String, String> result = msgStatus.getData();
                updateBPD(result);

                if (!msgStatus.isDialogClosed()) {
                    try {
                        anonymousDialogEnd(result.get("MsgHead.dialogid"));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }

                if (!msgStatus.isOK()) {
                    log.error("fetching BPD failed");
                    throw new ProcessException(HBCIUtils.getLocMsg("ERR_INST_BPDFAILED"), msgStatus);
                }
            } catch (Exception e) {
                if (e instanceof HBCI_Exception) {
                    HBCI_Exception he = (HBCI_Exception) e;
                    if (he.isFatal())
                        throw he;
                }
//                log.(e);
                // Viele Kreditinstitute unterstützen den anonymen Login nicht. Dass sollte nicht als Fehler den Anwender beunruhigen
                log.info("FAILED! - maybe this institute does not support anonymous logins");
                log.info("we will nevertheless go on");
            }
        }

        // ueberpruefen, ob angeforderte sicherheitsmethode auch
        // tatsaechlich unterstuetzt wird
        log.debug("checking if requested hbci parameters are supported");
        if (passport.getBPD() != null) {
            if (!passport.isSupported()) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_SECMETHNOTSUPP");
                throw new InvalidUserDataException(msg);
            }

            if (!Arrays.asList(passport.getSuppVersions()).contains(passport.getHBCIVersion())) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_VERSIONNOTSUPP");
                throw new InvalidUserDataException(msg);
            }
        } else {
            log.warn("can not check if requested parameters are supported");
        }

        passport.setPersistentData("_registered_institute", Boolean.TRUE);
    }

    private HBCIMsgStatus anonymousDialogInit() {
        Message message = MessageFactory.createMessage("DialogInitAnon", passport.getSyntaxDocument());
        message.rawSet("Idn.KIK.blz", passport.getBLZ());
        message.rawSet("Idn.KIK.country", passport.getCountry());
        message.rawSet("ProcPrep.BPD", "0");
        message.rawSet("ProcPrep.UPD", passport.getUPDVersion());
        message.rawSet("ProcPrep.lang", "0");
        message.rawSet("ProcPrep.prodName", "HBCI4Java");
        message.rawSet("ProcPrep.prodVersion", "2.5");

        return kernel.rawDoIt(message, HBCIKernel.DONT_SIGNIT, HBCIKernel.DONT_CRYPTIT,
                HBCIKernel.DONT_NEED_SIG, HBCIKernel.DONT_NEED_CRYPT);
    }

    private void anonymousDialogEnd(String dialogid) {
        passport.getCallback().status(HBCICallback.STATUS_DIALOG_END, null);

        Message message = MessageFactory.createMessage("DialogEndAnon", passport.getSyntaxDocument());
        message.rawSet("MsgHead.dialogid", dialogid);
        message.rawSet("MsgHead.msgnum", "2");
        message.rawSet("DialogEndS.dialogid", dialogid);
        message.rawSet("MsgTail.msgnum", "2");
        HBCIMsgStatus status = kernel.rawDoIt(message, HBCIKernel.DONT_SIGNIT, HBCIKernel.DONT_CRYPTIT, HBCIKernel.DONT_NEED_SIG, HBCIKernel.DONT_NEED_CRYPT);
        passport.getCallback().status(HBCICallback.STATUS_DIALOG_END_DONE, status);

        if (!status.isOK()) {
            log.error("dialog end failed: " + status.getErrorString());

            String msg = HBCIUtils.getLocMsg("ERR_INST_ENDFAILED");
            throw new ProcessException(msg, status);
        }
    }

    public HBCIPassportInternal getPassport() {
        return this.passport;
    }
}
