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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.ProcessException;
import org.kapott.hbci.manager.HBCIKernel;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MessageFactory;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.status.HBCIInstMessage;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.Collections;
import java.util.HashMap;

@Slf4j
public abstract class AbstractHbciDialog {

    @Getter
    protected final PinTanPassport passport;
    protected final HBCIKernel kernel;
    @Getter
    String dialogId;
    @Getter
    long msgnum;
    private boolean closed;

    AbstractHbciDialog(PinTanPassport passport) {
        this.passport = passport;
        this.kernel = new HBCIKernel(passport);
    }

    public abstract HBCIExecStatus execute(boolean close);

    public abstract boolean isAnonymous();

    public HBCIMsgStatus dialogInit(boolean withHktan) {
        return dialogInit(withHktan, "HKIDN");
    }

    public HBCIMsgStatus dialogInit(boolean withHktan, String orderSegCode) {
        log.debug("start dialog");

        if (passport.getSysId().equals("0")) {
            HBCIUpdDialog updDialog = new HBCIUpdDialog(passport);
            updDialog.execute(true);
        }

        log.debug(HBCIUtils.getLocMsg("STATUS_DIALOG_INIT"));
        passport.getCallback().status(HBCICallback.STATUS_DIALOG_INIT, null);

        Message message = MessageFactory.createDialogInit("DialogInit", null, passport, withHktan, orderSegCode);
        HBCIMsgStatus msgStatus = kernel.rawDoIt(message, "DialogInitSCA", HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT);

        passport.postInitResponseHook(msgStatus);

        HashMap<String, String> result = msgStatus.getData();
        if (msgStatus.isOK()) {
            msgnum = 2;
            this.dialogId = result.get("MsgHead.dialogid");
            handleBankMessages(result);

            passport.updateUPD(result);
        }

        passport.getCallback().status(HBCICallback.STATUS_DIALOG_INIT_DONE, new Object[]{msgStatus, dialogId});

        return msgStatus;
    }

    public HBCIMsgStatus dialogEnd() {
        if (closed || dialogId == null) {
            return null;
        }
        HBCIMsgStatus msgStatus = new HBCIMsgStatus();

        try {
            log.debug(HBCIUtils.getLocMsg("LOG_DIALOG_END"));
            passport.getCallback().status(HBCICallback.STATUS_DIALOG_END, null);

            Message message = MessageFactory.createDialogEnd(isAnonymous(), passport, dialogId, getMsgnum());
            msgStatus = kernel.rawDoIt(message, null, !isAnonymous(), !isAnonymous());

            passport.getCallback().status(HBCICallback.STATUS_DIALOG_END_DONE, msgStatus);
        } catch (Exception e) {
            msgStatus.addException(e);
        } finally {
            closed = true;
        }

        return msgStatus;
    }

    private void handleBankMessages(HashMap<String, String> result) {
        HBCIInstMessage bankMessage;
        for (int i = 0; true; i++) {
            try {
                String header = HBCIUtils.withCounter("KIMsg", i);
                bankMessage = new HBCIInstMessage(result, header);
            } catch (Exception e) {
                break;
            }
            passport.getCallback().callback(
                HBCICallback.HAVE_INST_MSG,
                Collections.singletonList(bankMessage.toString()),
                HBCICallback.TYPE_NONE,
                new StringBuilder());
        }
    }

}
