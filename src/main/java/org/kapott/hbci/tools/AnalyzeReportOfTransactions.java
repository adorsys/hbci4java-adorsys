/*  $Id: AnalyzeReportOfTransactions.java,v 1.1 2011/05/04 22:37:45 willuhn Exp $

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

package org.kapott.hbci.tools;

import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.GV.GVKUmsAll;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.GV_Result.GVRKUms.UmsLine;
import org.kapott.hbci.callback.HBCICallbackConsole;
import org.kapott.hbci.manager.HBCIDialog;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.passport.PinTanPassport;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.structures.Konto;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class AnalyzeReportOfTransactions {
    public static void main(String[] args)
        throws Exception {

        HBCIUtils.refreshBLZList(ClassLoader.getSystemResource("blz.properties").openStream());

        HashMap<String, String> properties = new HashMap<>();
        properties.put("kernel.rewriter", "InvalidSegment,WrongStatusSegOrder,WrongSequenceNumbers,MissingMsgRef,HBCIVersion,SigIdLeadingZero,InvalidSuppHBCIVersion,SecTypeTAN,KUmsDelimiters,KUmsEmptyBDateSets");
        properties.put("client.passport.default", "PinTanNoFile");
        properties.put("log.loglevel.default", "2");
        properties.put("default.hbciversion", "FinTS3");
        properties.put("client.passport.PinTan.checkcert", "1");
        properties.put("client.passport.PinTan.init", "1");

        // Initialize Bank Data
        properties.put("client.passport.country", "DE");
        properties.put("client.passport.blz", System.getProperty("blz"));
        properties.put("client.passport.customerId", System.getProperty("login"));

        // Initialize User Passport
        PinTanPassport passport = (PinTanPassport) PinTanPassport
            .getInstance(new HBCICallbackConsole(), properties);
        HBCIDialog dialog = new HBCIDialog(passport);

        passport.setPIN(System.getProperty("pin"));

        // Read bank account statement
        analyzeReportOfTransactions(passport, dialog);
    }

    private static void analyzeReportOfTransactions(HBCIPassportInternal hbciPassport, HBCIDialog hbciDialog) {
        // Use first available HBCI account
        Konto myaccount = hbciPassport.getAccounts().get(0);

        // Create HBCI job
        AbstractHBCIJob bankAccountStatementJob = new GVKUmsAll(hbciPassport);
        bankAccountStatementJob.setParam("my", myaccount);

        // Set bank account statement retrieval date
        // bankAccountStatementJob.setParam("startdate","21.5.2003");

        hbciDialog.addTask(bankAccountStatementJob);

        // Execute all jobs
        HBCIExecStatus ret = hbciDialog.execute(true);

        // GVRKUms = Geschäfts Vorfall Result Konto Umsatz
        GVRKUms result = (GVRKUms) bankAccountStatementJob.getJobResult();

        if (result.isOK()) {
            // Log bank account statement result
            System.out.println("************************** RESULT of **************************");
            System.out.println("****************  AnalyzeReportOfTransactions  ****************\n");
            System.out.println(result.toString());
            System.out.println("***************************************************************");

            List<UmsLine> lines = result.getFlatData();

            // Iterate revenue entries
            for (Iterator<UmsLine> j = lines.iterator(); j.hasNext(); ) {
                UmsLine entry = j.next();

                List<String> usages = entry.usage;

                // Iterate intended purpose (usage) entries
                for (Iterator<String> k = usages.iterator(); k.hasNext(); ) {
                    String usageline = k.next();

                    System.out.println(usageline);
                }
            }
        } else {
            // Log error messages
            System.out.println("Job-Error");
            System.out.println(result.getJobStatus().getErrorString());
            System.out.println("Global Error");
            System.out.println(ret.getErrorString());
        }
    }

    @Deprecated
    public final void main_multithreaded(String[] str) {
    }

    ;
}
