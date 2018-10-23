/*  $Id: HBCIUser.java,v 1.2 2011/08/31 14:05:21 willuhn Exp $

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
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.ProcessException;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.Message;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;
import java.util.Map;

/* @brief Instances of this class represent a certain user in combination with
    a certain institute. */
@Slf4j
public final class HBCIUser implements IHandlerData {

    private HBCIPassportInternal passport;
    private HBCIKernel kernel;

    /**
     * @brief This constructor initializes a new user instance with the given values
     */
    public HBCIUser(HBCIKernel kernel, HBCIPassportInternal passport) {
        this.kernel = kernel;
        this.passport = passport;
    }

    public void fetchSysId() {
        HBCIMsgStatus syncStatus = null;
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_SYSID, null);
            log.info("fetching new sys-id from institute");

            passport.setSigId(new Long(1));
            passport.setSysId("0");

            syncStatus = doDialogInit("Synch", "0");
            if (!syncStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_SYNCSYSIDFAIL"), syncStatus);

            HashMap<String, String> syncResult = syncStatus.getData();

            HBCIInstitute inst = new HBCIInstitute(kernel, passport);
            inst.updateBPD(syncResult);
            updateUPD(syncResult);

            passport.setSysId(syncResult.get("SyncRes.sysid"));
            passport.getCallback().status(HBCICallback.STATUS_INIT_SYSID_DONE, new Object[]{syncStatus, passport.getSysId()});
            log.debug("new sys-id is " + passport.getSysId());

            doDialogEnd(syncResult.get("MsgHead.dialogid"), "2", HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SYNCSYSIDFAIL"), e);
        } finally {
            if (syncStatus != null) {
                passport.postInitResponseHook(syncStatus);
            }
        }
    }

    public void fetchSigId() {
        HBCIMsgStatus msgStatus = null;
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_SIGID, null);
            log.info("syncing signature id");

            passport.setSigId(new Long("9999999999999999"));

            msgStatus = doDialogInit("Synch", "2");
            if (!msgStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_SYNCSIGIDFAIL"), msgStatus);

            HashMap<String, String> syncResult = msgStatus.getData();

            HBCIInstitute inst = new HBCIInstitute(kernel, passport);
            inst.updateBPD(syncResult);
            updateUPD(syncResult);
            passport.setSigId(new Long(syncResult.get("SyncRes.sigid") != null ? syncResult.get("SyncRes.sigid") : "1"));
            passport.incSigId();

            passport.getCallback().status(HBCICallback.STATUS_INIT_SIGID_DONE, new Object[]{msgStatus, passport.getSigId()});
            log.debug("signature id set to " + passport.getSigId());

            doDialogEnd(syncResult.get("MsgHead.dialogid"), "2", HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SYNCSIGIDFAIL"), e);
        } finally {
            if (msgStatus != null) {
                passport.postInitResponseHook(msgStatus);
            }
        }
    }

    public void updateUPD(HashMap<String, String> result) {
        log.debug("extracting UPD from results");

        HashMap<String, String> newUpd = new HashMap<>();

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

    public void fetchUPD() {
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_UPD, null);
            log.info("fetching UPD (BPD-Version: " + passport.getBPDVersion() + ")");

            HBCIMsgStatus msgStatus = doDialogInit("DialogInit", null);

            if (!msgStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_GETUPDFAIL"), msgStatus);

            HashMap<String, String> result = msgStatus.getData();

            HBCIInstitute inst = new HBCIInstitute(kernel, passport);
            inst.updateBPD(result);

            passport.postInitResponseHook(msgStatus);
            updateUPD(result);

            doDialogEnd(result.get("MsgHead.dialogid"), "2", HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_GETUPDFAIL"), e);
        }
    }

    private HBCIMsgStatus doDialogInit(String messageName, String syncMode) {
        Message message = MessageFactory.createDialogInit(messageName, syncMode, passport);
        return kernel.rawDoIt(message, HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT);
    }

    private void doDialogEnd(String dialogid, String msgnum, boolean signIt, boolean cryptIt) {
        passport.getCallback().status(HBCICallback.STATUS_DIALOG_END, null);

        Message message = MessageFactory.createMessage("DialogEnd", passport.getSyntaxDocument());
        message.rawSet("MsgHead.dialogid", dialogid);
        message.rawSet("MsgHead.msgnum", msgnum);
        message.rawSet("DialogEndS.dialogid", dialogid);
        message.rawSet("MsgTail.msgnum", msgnum);

        HBCIMsgStatus status = kernel.rawDoIt(message, signIt, cryptIt);

        passport.getCallback().status(HBCICallback.STATUS_DIALOG_END_DONE, status);

        if (!status.isOK()) {
            log.error("dialog end failed: " + status.getErrorString());

            String msg = HBCIUtils.getLocMsg("ERR_INST_ENDFAILED");
            throw new ProcessException(msg, status);
        }
    }

    public void updateUserData() {
        if (passport.getSysStatus().equals("1")) {
            if (passport.getSysId().equals("0"))
                fetchSysId();
            if (passport.getSigId().longValue() == -1)
                fetchSigId();
        }

        Map<String, String> upd = passport.getUPD();
        Map<String, String> bpd = passport.getBPD();
        String hbciVersionOfUPD = upd != null ? upd.get("_hbciversion") : null;

        // Wir haben noch keine BPD. Offensichtlich unterstuetzt die Bank
        // das Abrufen von BPDs ueber einen anonymen Dialog nicht. Also machen
        // wir das jetzt hier mit einem nicht-anonymen Dialog gleich mit
        if (bpd == null || passport.getUPD() == null ||
            hbciVersionOfUPD == null ||
            !hbciVersionOfUPD.equals(passport.getHBCIVersion())) {
            fetchUPD();
        }
    }

    public HBCIPassportInternal getPassport() {
        return this.passport;
    }
}
