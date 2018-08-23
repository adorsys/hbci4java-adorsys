package org.kapott.hbci.GV.generators;


import org.kapott.hbci.GV.AbstractSEPAGV;
import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.sepa.jaxb.pain_001_003_03.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * SEPA-Generator fuer pain.001.003.03.
 */
public class GenUebSEPA00100303 extends AbstractSEPAGenerator<HashMap<String, String>> {
    /**
     * @see org.kapott.hbci.GV.generators.AbstractSEPAGenerator#getSepaVersion()
     */
    @Override
    public SepaVersion getSepaVersion() {
        return SepaVersion.PAIN_001_003_03;
    }

    /**
     * @see PainGeneratorIf#generate(Object, OutputStream, boolean)
     */
    @Override
    public void generate(HashMap<String, String> sepaParams, OutputStream os, boolean validate) throws Exception {
        Integer maxIndex = SepaUtil.maxIndex(sepaParams);

        //Document
        Document doc = new Document();


        //Customer Credit Transfer Initiation
        doc.setCstmrCdtTrfInitn(new CustomerCreditTransferInitiationV03());
        doc.getCstmrCdtTrfInitn().setGrpHdr(new GroupHeaderSCT());

        final String sepaId = sepaParams.get("sepaid");
        final String pmtInfId = sepaParams.get("pmtinfid");

        //Group Header
        doc.getCstmrCdtTrfInitn().getGrpHdr().setMsgId(sepaId);
        doc.getCstmrCdtTrfInitn().getGrpHdr().setCreDtTm(SepaUtil.createCalendar(null));
        doc.getCstmrCdtTrfInitn().getGrpHdr().setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        doc.getCstmrCdtTrfInitn().getGrpHdr().setInitgPty(new PartyIdentificationSEPA1());
        doc.getCstmrCdtTrfInitn().getGrpHdr().getInitgPty().setNm(sepaParams.get("src.name"));
        doc.getCstmrCdtTrfInitn().getGrpHdr().setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));


        //Payment Information
        ArrayList<PaymentInstructionInformationSCT> pmtInfs = (ArrayList<PaymentInstructionInformationSCT>) doc.getCstmrCdtTrfInitn().getPmtInf();
        PaymentInstructionInformationSCT pmtInf = new PaymentInstructionInformationSCT();
        pmtInfs.add(pmtInf);

        pmtInf.setPmtInfId(pmtInfId != null && pmtInfId.length() > 0 ? pmtInfId : sepaId);
        pmtInf.setPmtMtd(PaymentMethodSCTCode.TRF);

        pmtInf.setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        pmtInf.setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));

        pmtInf.setPmtTpInf(new PaymentTypeInformationSCT1());
        pmtInf.getPmtTpInf().setSvcLvl(new ServiceLevelSEPA());
        pmtInf.getPmtTpInf().getSvcLvl().setCd("SEPA");

        String date = sepaParams.get("date");
        if (date == null) date = SepaUtil.DATE_UNDEFINED;
        pmtInf.setReqdExctnDt(SepaUtil.createCalendar(date));
        pmtInf.setDbtr(new PartyIdentificationSEPA2());
        pmtInf.setDbtrAcct(new CashAccountSEPA1());
        pmtInf.setDbtrAgt(new BranchAndFinancialInstitutionIdentificationSEPA3());


        //Payment Information - Debtor
        pmtInf.getDbtr().setNm(sepaParams.get("src.name"));


        //Payment Information - DebtorAccount
        pmtInf.getDbtrAcct().setId(new AccountIdentificationSEPA());
        pmtInf.getDbtrAcct().getId().setIBAN(sepaParams.get("src.iban"));


        //Payment Information - DebtorAgent
        pmtInf.getDbtrAgt().setFinInstnId(new FinancialInstitutionIdentificationSEPA3());
        String srcBic = sepaParams.get("src.bic");
        if (srcBic != null && srcBic.length() > 0) // BIC ist inzwischen optional
        {
            pmtInf.getDbtrAgt().getFinInstnId().setBIC(srcBic);
        } else {
            pmtInf.getDbtrAgt().getFinInstnId().setOthr(new OthrIdentification());
            pmtInf.getDbtrAgt().getFinInstnId().getOthr().setId(OthrIdentificationCode.NOTPROVIDED);
        }


        //Payment Information - ChargeBearer
        pmtInf.setChrgBr(ChargeBearerTypeSEPACode.SLEV);


        //Payment Information - Credit Transfer Transaction Information
        ArrayList<CreditTransferTransactionInformationSCT> cdtTrxTxInfs = (ArrayList<CreditTransferTransactionInformationSCT>) pmtInf.getCdtTrfTxInf();
        if (maxIndex != null) {
            for (int tnr = 0; tnr <= maxIndex; tnr++) {
                cdtTrxTxInfs.add(createCreditTransferTransactionInformationSCT(sepaParams, tnr));
            }
        } else {
            cdtTrxTxInfs.add(createCreditTransferTransactionInformationSCT(sepaParams, null));
        }

        String batch = SepaUtil.getProperty(sepaParams, "batchbook", null);
        if (batch != null)
            pmtInf.setBtchBookg(batch.equals("1"));

        ObjectFactory of = new ObjectFactory();
        this.marshal(of.createDocument(doc), os, validate);
    }

    private CreditTransferTransactionInformationSCT createCreditTransferTransactionInformationSCT(HashMap<String, String> sepaParams, Integer index) {
        CreditTransferTransactionInformationSCT cdtTrxTxInf = new CreditTransferTransactionInformationSCT();


        //Payment Information - Credit Transfer Transaction Information - Payment Identification
        cdtTrxTxInf.setPmtId(new PaymentIdentificationSEPA());
        cdtTrxTxInf.getPmtId().setEndToEndId(SepaUtil.getProperty(sepaParams, SepaUtil.insertIndex("endtoendid", index), AbstractSEPAGV.ENDTOEND_ID_NOTPROVIDED)); // sicherstellen, dass "NOTPROVIDED" eingetragen wird, wenn keine ID angegeben ist


        //Payment Information - Credit Transfer Transaction Information - Creditor
        cdtTrxTxInf.setCdtr(new PartyIdentificationSEPA2());
        cdtTrxTxInf.getCdtr().setNm(sepaParams.get(SepaUtil.insertIndex("dst.name", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Account
        cdtTrxTxInf.setCdtrAcct(new CashAccountSEPA2());
        cdtTrxTxInf.getCdtrAcct().setId(new AccountIdentificationSEPA());
        cdtTrxTxInf.getCdtrAcct().getId().setIBAN(sepaParams.get(SepaUtil.insertIndex("dst.iban", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Agent
        String dstBic = sepaParams.get(SepaUtil.insertIndex("dst.bic", index));
        if (dstBic != null && dstBic.length() > 0) // BIC ist inzwischen optional
        {
            cdtTrxTxInf.setCdtrAgt(new BranchAndFinancialInstitutionIdentificationSEPA1());
            cdtTrxTxInf.getCdtrAgt().setFinInstnId(new FinancialInstitutionIdentificationSEPA1());
            cdtTrxTxInf.getCdtrAgt().getFinInstnId().setBIC(dstBic);
        }

        //Payment Information - Credit Transfer Transaction Information - Amount
        cdtTrxTxInf.setAmt(new AmountTypeSEPA());
        cdtTrxTxInf.getAmt().setInstdAmt(new ActiveOrHistoricCurrencyAndAmountSEPA());
        cdtTrxTxInf.getAmt().getInstdAmt().setValue(new BigDecimal(sepaParams.get(SepaUtil.insertIndex("btg.value", index))));

        cdtTrxTxInf.getAmt().getInstdAmt().setCcy(ActiveOrHistoricCurrencyCodeEUR.EUR);

        //Payment Information - Credit Transfer Transaction Information - Usage
        String usage = sepaParams.get(SepaUtil.insertIndex("usage", index));
        if (usage != null && usage.length() > 0) {
            cdtTrxTxInf.setRmtInf(new RemittanceInformationSEPA1Choice());
            cdtTrxTxInf.getRmtInf().setUstrd(usage);
        }

        String purposeCode = sepaParams.get(SepaUtil.insertIndex("purposecode", index));
        if (purposeCode != null && purposeCode.length() > 0) {
            PurposeSEPA p = new PurposeSEPA();
            p.setCd(purposeCode);
            cdtTrxTxInf.setPurp(p);
        }

        return cdtTrxTxInf;
    }

}
