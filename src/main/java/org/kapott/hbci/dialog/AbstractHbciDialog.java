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
import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.*;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractHbciDialog {

    @Getter
    protected final PinTanPassport passport;
    protected final HBCIKernel kernel;
    @Getter
    String dialogId;
    HBCIMessageQueue queue = new HBCIMessageQueue();
    private boolean closed;
    private HashMap<String, Integer> listOfGVs = new HashMap<>();

    AbstractHbciDialog(PinTanPassport passport) {
        this.passport = passport;
        this.kernel = new HBCIKernel(passport);
    }

    public abstract HBCIExecStatus execute();

    public abstract long getMsgnum();

    public abstract boolean isAnonymous();

    public abstract HBCIMsgStatus dialogInit();

    public HBCIMsgStatus close() {
        if (closed || dialogId == null) {
            return null;
        }
        HBCIMsgStatus msgStatus = new HBCIMsgStatus();

        try {
            log.debug(HBCIUtils.getLocMsg("LOG_DIALOG_END"));
            passport.getCallback().status(HBCICallback.STATUS_DIALOG_END, null);

            Message message = MessageFactory.createDialogEnd(isAnonymous(), passport, dialogId, getMsgnum());
            msgStatus = kernel.rawDoIt(message, !isAnonymous(), !isAnonymous());

            passport.getCallback().status(HBCICallback.STATUS_DIALOG_END_DONE, msgStatus);
        } catch (Exception e) {
            msgStatus.addException(e);
        } finally {
            closed = true;
        }

        return msgStatus;
    }

    public HBCIMessage addTask(AbstractHBCIJob job) {
        return this.addTask(job, true);
    }

    public HBCIMessage addTask(AbstractHBCIJob job, boolean verify) {
        try {
            log.info(HBCIUtils.getLocMsg("EXCMSG_ADDJOB", job.getName()));
            if (verify) {
                job.verifyConstraints();
            }

            // check bpd.numgva here
            String hbciCode = job.getHBCICode();
            if (hbciCode == null) {
                throw new HBCI_Exception(job.getName() + " not supported");
            }

            int gva_counter = listOfGVs.size();
            Integer counter_st = listOfGVs.get(hbciCode);
            int gv_counter = counter_st != null ? counter_st : 0;
            int total_counter = getTotalNumberOfGVSegsInCurrentMessage();

            gv_counter++;
            total_counter++;
            if (counter_st == null) {
                gva_counter++;
            }

            // BPD: max. Anzahl GV-Arten
            int maxGVA = passport.getMaxGVperMsg();
            // BPD: max. Anzahl von Job-Segmenten eines bestimmten Typs
            int maxGVSegJob = job.getMaxNumberPerMsg();
            // Passport: evtl. weitere Einschränkungen bzgl. der Max.-Anzahl
            // von Auftragssegmenten pro Nachricht
            int maxGVSegTotal = passport.getMaxGVSegsPerMsg();

            if ((maxGVA > 0 && gva_counter > maxGVA) ||
                (maxGVSegJob > 0 && gv_counter > maxGVSegJob) ||
                (maxGVSegTotal > 0 && total_counter > maxGVSegTotal)) {
                if (maxGVSegTotal > 0 && total_counter > maxGVSegTotal) {
                    log.debug(
                        "have to generate new message because current type of passport only allows " + maxGVSegTotal + " GV segs per message");
                } else {
                    log.debug(
                        "have to generate new message because of BPD restrictions for number of tasks per message; " +
                            "adding job to this new message");
                }
                newMsg();
                gv_counter = 1;
            }

            listOfGVs.put(hbciCode, gv_counter);
            HBCIMessage last = queue.getLast();
            last.append(job);
            return last;
        } catch (Exception e) {
            String msg = HBCIUtils.getLocMsg("EXCMSG_CANTADDJOB", job.getName());
            log.error("task " + job.getName() + " will not be executed in current dialog");
            throw new HBCI_Exception(msg, e);
        }
    }

    private int getTotalNumberOfGVSegsInCurrentMessage() {
        int total = 0;

        for (Map.Entry<String, Integer> hbciCode : listOfGVs.entrySet()) {
            total += hbciCode.getValue();
        }

        log.debug("there are currently " + total + " GV segs in this message");
        return total;
    }

    private void newMsg() {
        log.debug("starting new message");
        this.queue.append(new HBCIMessage());
        listOfGVs.clear();
    }
}
