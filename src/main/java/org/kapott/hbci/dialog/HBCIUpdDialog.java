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
import org.kapott.hbci.exceptions.ProcessException;
import org.kapott.hbci.manager.HBCIKernel;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;
import java.util.Map;

/* @brief Instances of this class represent a certain user in combination with
    a certain institute. */
@Slf4j
public final class HBCIUpdDialog extends AbstractHbciDialog {

    public HBCIUpdDialog(PinTanPassport passport) {
        super(passport);
    }

    @Override
    public HBCIExecStatus execute() {
        if (passport.getUPD() == null) {
            try {
                log.debug("registering user");
                updateUserData();
            } catch (Exception ex) {
                throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_CANT_REG_USER"), ex);
            }
        }
        return null;
    }

    @Override
    public long getMsgnum() {
        return 2;
    }

    private void fetchSysId() {
        HBCIMsgStatus syncStatus = null;
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_SYSID, null);
            log.info("fetching new sys-id from institute");

            passport.setSigId(1L);
            passport.setSysId("0");

            syncStatus = doDialogInitSync("Synch", "0");
            if (!syncStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_SYNCSYSIDFAIL"), syncStatus);

            updateUPD(syncStatus.getData());

            passport.setSysId(syncStatus.getData().get("SyncRes.sysid"));
            passport.getCallback().status(HBCICallback.STATUS_INIT_SYSID_DONE, new Object[]{syncStatus,
                passport.getSysId()});
            log.debug("new sys-id is " + passport.getSysId());

            this.dialogId = syncStatus.getData().get("MsgHead.dialogid");
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SYNCSYSIDFAIL"), e);
        } finally {
            if (syncStatus != null) {
                passport.postInitResponseHook(syncStatus);
            }
        }
    }

    private void fetchSigId() {
        HBCIMsgStatus msgStatus = null;
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_SIGID, null);
            log.info("syncing signature id");

            passport.setSigId(new Long("9999999999999999"));

            msgStatus = doDialogInitSync("Synch", "2");
            if (!msgStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_SYNCSIGIDFAIL"), msgStatus);

            HashMap<String, String> syncResult = msgStatus.getData();

            updateUPD(syncResult);
            passport.setSigId(new Long(syncResult.get("SyncRes.sigid") != null ? syncResult.get("SyncRes.sigid") : "1"
            ));
            passport.incSigId();

            passport.getCallback().status(HBCICallback.STATUS_INIT_SIGID_DONE, new Object[]{msgStatus,
                passport.getSigId()});
            log.debug("signature id set to " + passport.getSigId());

            this.dialogId = syncResult.get("MsgHead.dialogid");
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SYNCSIGIDFAIL"), e);
        } finally {
            if (msgStatus != null) {
                passport.postInitResponseHook(msgStatus);
            }
        }
    }

    void updateUPD(Map<String, String> result) {
        log.debug("extracting UPD from results");

        Map<String, String> newUpd = new HashMap<>();

        result.forEach((key, value) -> {
            if (key.startsWith("UPD.")) {
                newUpd.put(key.substring(4), value);
            }
        });

        if (newUpd.size() != 0) {
            newUpd.put("_hbciversion", passport.getHBCIVersion());

            String oldVersion = passport.getUPDVersion();
            passport.setUPD(newUpd);

            log.info("installed new UPD [old version: " + oldVersion + ", new version: " + passport.getUPDVersion() + "]");
            passport.getCallback().status(HBCICallback.STATUS_INIT_UPD_DONE, passport.getUPD());
        }
    }

    private void fetchUPD() {
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_UPD, null);
            log.info("fetching UPD (BPD-Version: " + passport.getBPDVersion() + ")");

            HBCIMsgStatus msgStatus = doDialogInitSync("DialogInit", null);

            if (!msgStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_GETUPDFAIL"), msgStatus);

            HashMap<String, String> result = msgStatus.getData();

            passport.postInitResponseHook(msgStatus);
            updateUPD(result);

            this.dialogId = result.get("MsgHead.dialogid");
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_GETUPDFAIL"), e);
        }
    }

    private HBCIMsgStatus doDialogInitSync(String messageName, String syncMode) {
        Message message = MessageFactory.createDialogInit(messageName, syncMode, passport);
        return kernel.rawDoIt(message, HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT);
    }

    void updateUserData() {
        if (passport.getSysStatus().equals("1")) {
            if (passport.getSysId().equals("0"))
                fetchSysId();
            if (passport.getSigId() == -1)
                fetchSigId();
        }
//
//        Map<String, String> upd = passport.getUPD();
//        Map<String, String> bpd = passport.getBPD();
//        String hbciVersionOfUPD = upd != null ? upd.get("_hbciversion") : null;
//
//        // BPD und UPD exlizit anfordern
//        if (bpd == null || upd == null || hbciVersionOfUPD == null || !hbciVersionOfUPD.equals(passport
//        .getHBCIVersion())) {
//            fetchUPD();
//        }
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public HBCIMsgStatus dialogInit() {
        throw new UnsupportedOperationException();
    }
}
