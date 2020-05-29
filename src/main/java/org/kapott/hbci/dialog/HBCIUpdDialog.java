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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.ProcessException;
import org.kapott.hbci.manager.HBCIKernel;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;
import java.util.Optional;

@Slf4j
public class HBCIUpdDialog extends AbstractHbciDialog {

    @Setter
    private boolean withHktan;

    public HBCIUpdDialog(PinTanPassport passport, boolean withHktan) {
        super(passport);
        this.withHktan = withHktan;
    }

    @Override
    public HBCIExecStatus execute(boolean close) {
        try {
            log.debug("registering user");
            updateUserData();
            if (close) {
                dialogEnd();
            }
        } catch (Exception ex) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_CANT_REG_USER"), ex);
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

            if (withHktan) {
                withHktan = Optional.ofNullable(passport.getCurrentSecMechInfo())
                    .map(twoStepMechanism -> !twoStepMechanism.getId().equals("999"))
                    .orElse(false);
            }

            syncStatus = doDialogInitSync("0", withHktan);
            if (!syncStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_SYNCSYSIDFAIL"), syncStatus);

            passport.updateUPD(syncStatus.getData());
            passport.updateBPD(syncStatus.getData());

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

            passport.setSigId(9999999999999999L);

            msgStatus = doDialogInitSync("2", true);
            if (!msgStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_SYNCSIGIDFAIL"), msgStatus);

            HashMap<String, String> syncResult = msgStatus.getData();

            passport.updateUPD(syncResult);
            passport.setSigId(syncResult.get("SyncRes.sigid") != null
                ? Long.parseLong(syncResult.get("SyncRes.sigid"))
                : 1L);
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

    private HBCIMsgStatus doDialogInitSync(String syncMode, boolean withHktan) {
        Message message = MessageFactory.createDialogInit("Synch", syncMode, passport, withHktan, "HKIDN");
        return kernel.rawDoIt(message, null, HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT);
    }

    private void updateUserData() {
        if (passport.getSysId().equals("0"))
            fetchSysId();
        if (passport.getSigId() == -1)
            fetchSigId();
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }
}
