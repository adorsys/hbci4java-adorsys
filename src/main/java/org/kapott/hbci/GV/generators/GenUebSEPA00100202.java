package org.kapott.hbci.GV.generators;

import org.kapott.hbci.GV.AbstractSEPAGV;
import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.sepa.jaxb.pain_001_002_02.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * SEPA-Generator fuer pain.001.002.02.
 */
public class GenUebSEPA00100202 extends AbstractSEPAGenerator<HashMap<String, String>> {
    /**
     * @see org.kapott.hbci.GV.generators.AbstractSEPAGenerator#getSepaVersion()
     */
    @Override
    public SepaVersion getSepaVersion() {
        return SepaVersion.PAIN_001_002_02;
    }

    /**
     * @see PainGeneratorIf#generate(Object, OutputStream, boolean)
     */
    @Override
    public void generate(HashMap<String, String> sepaParams, OutputStream os, boolean validate) throws Exception {
        Integer maxIndex = SepaUtil.maxIndex(sepaParams);

        //Document
        Document doc = new Document();


        //Pain00100102
        doc.setPain00100102(new Pain00100102());


        doc.getPain00100102().setGrpHdr(new GroupHeaderSCT());

        String batch = SepaUtil.getProperty(sepaParams, "batchbook", null);
        if (batch != null)
            doc.getPain00100102().getGrpHdr().setBtchBookg(batch.equals("1"));

        final String sepaId = sepaParams.get("sepaid");
        final String pmtInfId = sepaParams.get("pmtinfid");

        //Group Header
        doc.getPain00100102().getGrpHdr().setMsgId(sepaId);
        doc.getPain00100102().getGrpHdr().setCreDtTm(SepaUtil.createCalendar(null));
        doc.getPain00100102().getGrpHdr().setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        doc.getPain00100102().getGrpHdr().setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));
        doc.getPain00100102().getGrpHdr().setGrpg(Grouping1CodeSCT.MIXD);
        doc.getPain00100102().getGrpHdr().setInitgPty(new PartyIdentificationSCT1());
        doc.getPain00100102().getGrpHdr().getInitgPty().setNm(sepaParams.get("src.name"));


        //Payment Information
        ArrayList<PaymentInstructionInformationSCT> pmtInfs = (ArrayList<PaymentInstructionInformationSCT>) doc.getPain00100102().getPmtInf();
        PaymentInstructionInformationSCT pmtInf = new PaymentInstructionInformationSCT();
        pmtInfs.add(pmtInf);

        pmtInf.setPmtInfId(pmtInfId != null && pmtInfId.length() > 0 ? pmtInfId : sepaId);
        pmtInf.setPmtMtd(PaymentMethodSCTCode.TRF);

        // Payment Type Information
        pmtInf.setPmtTpInf(new PaymentTypeInformationSCT1());
        pmtInf.getPmtTpInf().setSvcLvl(new ServiceLevelSCT());
        pmtInf.getPmtTpInf().getSvcLvl().setCd(ServiceLevelSCTCode.SEPA);

        String date = sepaParams.get("date");
        if (date == null) date = SepaUtil.DATE_UNDEFINED;
        pmtInf.setReqdExctnDt(SepaUtil.createCalendar(date));
        pmtInf.setDbtr(new PartyIdentificationSCT2());
        pmtInf.setDbtrAcct(new CashAccountSCT1());
        pmtInf.setDbtrAgt(new BranchAndFinancialInstitutionIdentificationSCT());


        //Payment Information - Debtor
        pmtInf.getDbtr().setNm(sepaParams.get("src.name"));


        //Payment Information - DebtorAccount
        pmtInf.getDbtrAcct().setId(new AccountIdentificationSCT());
        pmtInf.getDbtrAcct().getId().setIBAN(sepaParams.get("src.iban"));


        //Payment Information - DebtorAgent
        pmtInf.getDbtrAgt().setFinInstnId(new FinancialInstitutionIdentificationSCT());
        pmtInf.getDbtrAgt().getFinInstnId().setBIC(sepaParams.get("src.bic"));


        //Payment Information - ChargeBearer
        pmtInf.setChrgBr(ChargeBearerTypeSCTCode.SLEV);

        //Payment Information - Credit Transfer Transaction Information
        ArrayList<CreditTransferTransactionInformationSCT> cdtTrxTxInfs = (ArrayList<CreditTransferTransactionInformationSCT>) pmtInf.getCdtTrfTxInf();
        if (maxIndex != null) {
            for (int tnr = 0; tnr <= maxIndex; tnr++) {
                cdtTrxTxInfs.add(createCreditTransferTransactionInformationSCT(sepaParams, tnr));
            }
        } else {
            cdtTrxTxInfs.add(createCreditTransferTransactionInformationSCT(sepaParams, null));
        }

        ObjectFactory of = new ObjectFactory();
        this.marshal(of.createDocument(doc), os, validate);
    }

    private CreditTransferTransactionInformationSCT createCreditTransferTransactionInformationSCT(HashMap<String, String> sepaParams, Integer index) {
        CreditTransferTransactionInformationSCT cdtTrxTxInf = new CreditTransferTransactionInformationSCT();

        //Payment Information - Credit Transfer Transaction Information - Payment Identification
        cdtTrxTxInf.setPmtId(new PaymentIdentification1());
        cdtTrxTxInf.getPmtId().setEndToEndId(SepaUtil.getProperty(sepaParams, SepaUtil.insertIndex("endtoendid", index), AbstractSEPAGV.ENDTOEND_ID_NOTPROVIDED)); // sicherstellen, dass "NOTPROVIDED" eingetragen wird, wenn keine ID angegeben ist


        //Payment Information - Credit Transfer Transaction Information - Creditor
        cdtTrxTxInf.setCdtr(new PartyIdentificationSCT2());
        cdtTrxTxInf.getCdtr().setNm(sepaParams.get(SepaUtil.insertIndex("dst.name", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Account
        cdtTrxTxInf.setCdtrAcct(new CashAccountSCT2());
        cdtTrxTxInf.getCdtrAcct().setId(new AccountIdentificationSCT());
        cdtTrxTxInf.getCdtrAcct().getId().setIBAN(sepaParams.get(SepaUtil.insertIndex("dst.iban", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Agent
        cdtTrxTxInf.setCdtrAgt(new BranchAndFinancialInstitutionIdentificationSCT());
        cdtTrxTxInf.getCdtrAgt().setFinInstnId(new FinancialInstitutionIdentificationSCT());
        cdtTrxTxInf.getCdtrAgt().getFinInstnId().setBIC(sepaParams.get(SepaUtil.insertIndex("dst.bic", index)));


        //Payment Information - Credit Transfer Transaction Information - Amount
        cdtTrxTxInf.setAmt(new AmountTypeSCT());
        cdtTrxTxInf.getAmt().setInstdAmt(new CurrencyAndAmountSCT());
        cdtTrxTxInf.getAmt().getInstdAmt().setValue(new BigDecimal(sepaParams.get(SepaUtil.insertIndex("btg.value", index))));

        cdtTrxTxInf.getAmt().getInstdAmt().setCcy(CurrencyCodeSCT.EUR);

        //Payment Information - Credit Transfer Transaction Information - Usage
        String usage = sepaParams.get(SepaUtil.insertIndex("usage", index));
        if (usage != null && usage.length() > 0) {
            cdtTrxTxInf.setRmtInf(new RemittanceInformationSCTChoice());
            cdtTrxTxInf.getRmtInf().setUstrd(usage);
        }

        String purposeCode = sepaParams.get(SepaUtil.insertIndex("purposecode", index));
        if (purposeCode != null && purposeCode.length() > 0) {
            PurposeSCT p = new PurposeSCT();
            p.setCd(purposeCode);
            cdtTrxTxInf.setPurp(p);
        }

        return cdtTrxTxInf;
    }

}
