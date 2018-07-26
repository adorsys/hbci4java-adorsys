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
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_SYSID, null);
            log.info("fetching new sys-id from institute");

            // autosecmech
            log.debug("checking whether passport is supported (but ignoring result)");
            boolean s = passport.isSupported();
            log.debug("passport supported: " + s);

            passport.setSigId(new Long(1));
            passport.setSysId("0");

            HBCIMsgStatus msgStatus;
            boolean restarted = false;
            while (true) {
                msgStatus = doSync("0");

                boolean need_restart = passport.postInitResponseHook(msgStatus);
                if (need_restart) {
                    log.info("for some reason we have to restart this dialog");
                    if (restarted) {
                        log.warn("this dialog already has been restarted once - to avoid endless loops we stop here");
                        throw new HBCI_Exception("*** restart loop - aborting");
                    }
                    restarted = true;
                } else {
                    break;
                }
            }

            HashMap<String, String> result = msgStatus.getData();

            if (!msgStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_SYNCSYSIDFAIL"), msgStatus);

            HBCIInstitute inst = new HBCIInstitute(kernel, passport);
            inst.updateBPD(result);
            updateUPD(result);
            passport.setSysId(result.get("SyncRes.sysid"));

            passport.getCallback().status(HBCICallback.STATUS_INIT_SYSID_DONE, new Object[]{msgStatus, passport.getSysId()});
            log.debug("new sys-id is " + passport.getSysId());
            doDialogEnd(result.get("MsgHead.dialogid"), "2", HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT,
                    HBCIKernel.NEED_CRYPT);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SYNCSYSIDFAIL"), e);
        }
    }

    public void fetchSigId() {
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_SIGID, null);
            log.info("syncing signature id");

            // autosecmech
            log.debug("checking whether passport is supported (but ignoring result)");
            boolean s = passport.isSupported();
            log.debug("passport supported: " + s);

            passport.setSigId(new Long("9999999999999999"));

            HBCIMsgStatus msgStatus;
            boolean restarted = false;
            while (true) {
                msgStatus = doSync("2");

                boolean need_restart = passport.postInitResponseHook(msgStatus);
                if (need_restart) {
                    log.info("for some reason we have to restart this dialog");
                    if (restarted) {
                        log.warn("this dialog already has been restarted once - to avoid endless loops we stop here");
                        throw new HBCI_Exception("*** restart loop - aborting");
                    }
                    restarted = true;
                } else {
                    break;
                }
            }

            HashMap<String, String> result = msgStatus.getData();

            if (!msgStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_SYNCSIGIDFAIL"), msgStatus);

            HBCIInstitute inst = new HBCIInstitute(kernel, passport);
            inst.updateBPD(result);
            updateUPD(result);
            passport.setSigId(new Long(result.get("SyncRes.sigid") != null ? result.get("SyncRes.sigid") : "1"));
            passport.incSigId();

            passport.getCallback().status(HBCICallback.STATUS_INIT_SIGID_DONE, new Object[]{msgStatus, passport.getSigId()});
            log.debug("signature id set to " + passport.getSigId());
            doDialogEnd(result.get("MsgHead.dialogid"), "2", HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT,
                    HBCIKernel.NEED_CRYPT);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_SYNCSIGIDFAIL"), e);
        }
    }

    public void updateUPD(HashMap<String, String> result) {
        log.debug("extracting UPD from results");

        HashMap<String, String> p = new HashMap<>();

        result.forEach((key, value) -> {
            if (key.startsWith("UPD.")) {
                p.put(key.substring(("UPD.").length()), value);
            }
        });

        if (p.size() != 0) {
            p.put("_hbciversion", passport.getHBCIVersion());

            // Wir sichern wenigstens noch die TAN-Media-Infos, die vom HBCIHandler vorher abgerufen wurden
            // Das ist etwas unschoen. Sinnvollerweise sollten die SEPA-Infos und TAN-Medien nicht in den
            // UPD gespeichert werden. Dann gehen die auch nicht jedesmal wieder verloren und muessen nicht
            // dauernd neu abgerufen werden. Das wuerde aber einen groesseren Umbau erfordern
            HashMap<String, String> upd = passport.getUPD();
            if (upd != null) {
                String mediaInfo = upd.get("tanmedia.names");
                if (mediaInfo != null) {
                    log.info("rescued TAN media info to new UPD: " + mediaInfo);
                    p.put("tanmedia.names", mediaInfo);
                }
            }

            String oldVersion = passport.getUPDVersion();
            passport.setUPD(p);

            log.info("installed new UPD [old version: " + oldVersion + ", new version: " + passport.getUPDVersion() + "]");
            passport.getCallback().status(HBCICallback.STATUS_INIT_UPD_DONE, passport.getUPD());
        }
    }

    public void fetchUPD() {
        try {
            passport.getCallback().status(HBCICallback.STATUS_INIT_UPD, null);
            log.info("fetching UPD (BPD-Version: " + passport.getBPDVersion() + ")");

            // autosecmech
            log.debug("checking whether passport is supported (but ignoring result)");
            boolean s = passport.isSupported();
            log.debug("passport supported: " + s);

            HBCIMsgStatus msgStatus;
            boolean restarted = false;
            while (true) {
                msgStatus = doDialogInit();
                boolean need_restart = passport.postInitResponseHook(msgStatus);
                if (need_restart) {
                    log.info("for some reason we have to restart this dialog");
                    if (restarted) {
                        log.warn("this dialog already has been restarted once - to avoid endless loops we stop here");
                        throw new HBCI_Exception("*** restart loop - aborting");
                    }
                    restarted = true;
                } else {
                    break;
                }
            }

            HashMap<String, String> result = msgStatus.getData();

            if (!msgStatus.isOK())
                throw new ProcessException(HBCIUtils.getLocMsg("EXCMSG_GETUPDFAIL"), msgStatus);

            HBCIInstitute inst = new HBCIInstitute(kernel, passport);
            inst.updateBPD(result);

            updateUPD(result);

            doDialogEnd(result.get("MsgHead.dialogid"), "2", HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT,
                    HBCIKernel.NEED_CRYPT);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_GETUPDFAIL"), e);
        }
    }

    private HBCIMsgStatus doDialogInit() {
        Message message = MessageFactory.createMessage("DialogInit", passport.getSyntaxDocument());
        message.rawSet("Idn.KIK.blz", passport.getBLZ());
        message.rawSet("Idn.KIK.country", passport.getCountry());
        message.rawSet("Idn.customerid", passport.getCustomerId());
        message.rawSet("Idn.sysid", passport.getSysId());
        message.rawSet("Idn.sysStatus", passport.getSysStatus());
        message.rawSet("ProcPrep.BPD", passport.getBPDVersion());
        message.rawSet("ProcPrep.UPD", "0");
        message.rawSet("ProcPrep.lang", passport.getLang());
        message.rawSet("ProcPrep.prodName", "HBCI4Java");
        message.rawSet("ProcPrep.prodVersion", "2.5");
        return kernel.rawDoIt(message, HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT, HBCIKernel.NEED_SIG, HBCIKernel.NEED_CRYPT);
    }

    private void doDialogEnd(String dialogid, String msgnum, boolean signIt, boolean cryptIt, boolean needCrypt) {
        passport.getCallback().status(HBCICallback.STATUS_DIALOG_END, null);

        Message message = MessageFactory.createMessage("DialogEnd", passport.getSyntaxDocument());
        message.rawSet("MsgHead.dialogid", dialogid);
        message.rawSet("MsgHead.msgnum", msgnum);
        message.rawSet("DialogEndS.dialogid", dialogid);
        message.rawSet("MsgTail.msgnum", msgnum);

        HBCIMsgStatus status = kernel.rawDoIt(message, signIt, cryptIt, HBCIKernel.NEED_SIG, needCrypt);

        passport.getCallback().status(HBCICallback.STATUS_DIALOG_END_DONE, status);

        if (!status.isOK()) {
            log.error("dialog end failed: " + status.getErrorString());

            String msg = HBCIUtils.getLocMsg("ERR_INST_ENDFAILED");
            throw new ProcessException(msg, status);
        }
    }

    private HBCIMsgStatus doSync(String syncMode) {
        Message message = MessageFactory.createMessage("Synch", passport.getSyntaxDocument());
        message.rawSet("Idn.KIK.blz", passport.getBLZ());
        message.rawSet("Idn.KIK.country", passport.getCountry());
        message.rawSet("Idn.customerid", passport.getCustomerId());
        message.rawSet("Idn.sysid", passport.getSysId());
        message.rawSet("Idn.sysStatus", passport.getSysStatus());
        message.rawSet("MsgHead.dialogid", "0");
        message.rawSet("MsgHead.msgnum", "1");
        message.rawSet("MsgTail.msgnum", "1");
        message.rawSet("ProcPrep.BPD", passport.getBPDVersion());
        message.rawSet("ProcPrep.UPD", passport.getUPDVersion());
        message.rawSet("ProcPrep.lang", "0");
        message.rawSet("ProcPrep.prodName", "HBCI4Java");
        message.rawSet("ProcPrep.prodVersion", "2.5");
        message.rawSet("Sync.mode", syncMode);
        return kernel.rawDoIt(message, HBCIKernel.SIGNIT, HBCIKernel.CRYPTIT,
                HBCIKernel.NEED_SIG, HBCIKernel.NEED_CRYPT);
    }

    public void updateUserData() {
        if (passport.getSysStatus().equals("1")) {
            if (passport.getSysId().equals("0"))
                fetchSysId();
            if (passport.getSigId().longValue() == -1)
                fetchSigId();
        }

        HashMap<String, String> upd = passport.getUPD();
        HashMap<String, String> bpd = passport.getBPD();
        String hbciVersionOfUPD = upd != null ? upd.get("_hbciversion") : null;

        // Wir haben noch keine BPD. Offensichtlich unterstuetzt die Bank
        // das Abrufen von BPDs ueber einen anonymen Dialog nicht. Also machen
        // wir das jetzt hier mit einem nicht-anonymen Dialog gleich mit
        if (bpd == null || passport.getUPD() == null ||
                hbciVersionOfUPD == null ||
                !hbciVersionOfUPD.equals(passport.getHBCIVersion())) {
            fetchUPD();
        }

        passport.setPersistentData("_registered_user", Boolean.TRUE);

    }

    public HBCIPassportInternal getPassport() {
        return this.passport;
    }
}
