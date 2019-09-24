/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kapott.hbci.dialog;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.exceptions.ProcessException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.kapott.hbci.manager.HBCIKernel.DONT_CRYPTIT;
import static org.kapott.hbci.manager.HBCIKernel.DONT_SIGNIT;

@Slf4j
public final class HBCIBpdDialog extends AbstractHbciDialog {

    private static final String BPD_KEY_LASTUPDATE = "_lastupdate";
    private static final String BPD_KEY_HBCIVERSION = "_hbciversion";

    private static final String maxAge = "7";

    public HBCIBpdDialog(PinTanPassport passport) {
        super(passport);
    }

    @Override
    public HBCIExecStatus execute(boolean close) {
        try {
            log.debug("registering institute");
            fetchBPDAnonymousInternal();
            if (close) {
                dialogEnd();
            }
        } catch (Exception ex) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_CANT_REG_INST"), ex);
        }
        return null;
    }

    @Override
    public long getMsgnum() {
        return 2;
    }

    /**
     * gets the BPD out of the result and store it in the
     * passport field
     */
    void updateBPD(HashMap<String, String> result) {
        log.debug("extracting BPD from results");
        HashMap<String, String> newBPD = new HashMap<>();

        result.keySet().forEach(key -> {
            if (key.startsWith("BPD.")) {
                newBPD.put(key.substring(("BPD.").length()), result.get(key));
            }
        });

        if (newBPD.size() != 0) {
            newBPD.put(BPD_KEY_HBCIVERSION, passport.getHBCIVersion());
            newBPD.put(BPD_KEY_LASTUPDATE, String.valueOf(System.currentTimeMillis()));
            passport.setBPD(newBPD);
            log.info("installed new BPD with version " + passport.getBPDVersion());
            passport.getCallback().status(HBCICallback.STATUS_INST_BPD_INIT_DONE, passport.getBPD());
        }
    }

    /**
     * Prueft, ob die BPD abgelaufen sind und neu geladen werden muessen.
     *
     * @return true, wenn die BPD abgelaufen sind.
     */
    private boolean isBPDExpired() {
        Map<String, String> bpd = passport.getBPD();
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
    private void fetchBPDAnonymousInternal() {
        // BPD abholen, wenn nicht vorhanden oder HBCI-Version geaendert
        Map<String, String> bpd = passport.getBPD();
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
                this.dialogId = msgStatus.getData().get("MsgHead.dialogid");

                updateBPD(msgStatus.getData());

                if (!msgStatus.isOK()) {
                    log.error("fetching BPD failed");
                    throw new ProcessException(HBCIUtils.getLocMsg("ERR_INST_BPDFAILED"), msgStatus);
                }
            } catch (HBCI_Exception e) {
                if (e.isFatal())
                    throw e;
            } catch (Exception e) {
                // Viele Kreditinstitute unterstützen den anonymen Login nicht. Dass sollte nicht als Fehler den
                // Anwender beunruhigen
                log.info("FAILED! - maybe this institute does not support anonymous logins");
                log.info("we will nevertheless go on");
            }
        }

        // ueberpruefen, ob angeforderte sicherheitsmethode auch
        // tatsaechlich unterstuetzt wird
        log.debug("checking if requested hbci parameters are supported");
        if (passport.getBPD() != null) {
            if (!Arrays.asList(passport.getSuppVersions()).contains(passport.getHBCIVersion())) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_VERSIONNOTSUPP");
                throw new InvalidUserDataException(msg);
            }
        } else {
            log.warn("can not check if requested parameters are supported");
        }
    }

    private HBCIMsgStatus anonymousDialogInit() {
        Message dialogInitMessage = MessageFactory.createAnonymousDialogInit(passport);
        return kernel.rawDoIt(dialogInitMessage, null, DONT_SIGNIT, DONT_CRYPTIT);
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }
}
