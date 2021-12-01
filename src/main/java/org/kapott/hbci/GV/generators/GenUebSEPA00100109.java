package org.kapott.hbci.GV.generators;

import org.kapott.hbci.GV.AbstractSEPAGV;
import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.sepa.jaxb.pain_001_001_09.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

/**
 * SEPA-Generator fuer pain.001.001.09.
 */
public class GenUebSEPA00100109 extends AbstractSEPAGenerator<Map<String, String>> {
    @Override
    public void generate(Map<String, String> sepaParams, OutputStream os, boolean validate) {
        Integer maxIndex = SepaUtil.maxIndex(sepaParams);

        //Document
        Document doc = new Document();

        //Customer Credit Transfer Initiation
        doc.setCstmrCdtTrfInitn(new CustomerCreditTransferInitiationV09());
        doc.getCstmrCdtTrfInitn().setGrpHdr(new GroupHeader85());

        final String sepaId = sepaParams.get("sepaid");
        final String pmtInfId = sepaParams.get("pmtinfid");

        //Group Header
        doc.getCstmrCdtTrfInitn().getGrpHdr().setMsgId(sepaId);
        doc.getCstmrCdtTrfInitn().getGrpHdr().setCreDtTm(SepaUtil.createCalendar(null));
        doc.getCstmrCdtTrfInitn().getGrpHdr().setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        doc.getCstmrCdtTrfInitn().getGrpHdr().setInitgPty(new PartyIdentification1351());
        doc.getCstmrCdtTrfInitn().getGrpHdr().getInitgPty().setNm(sepaParams.get("src.name"));
        doc.getCstmrCdtTrfInitn().getGrpHdr().setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));

        //Payment Information
        ArrayList<PaymentInstruction30> pmtInfs =
            (ArrayList<PaymentInstruction30>) doc.getCstmrCdtTrfInitn().getPmtInf();
        PaymentInstruction30 pmtInf = new PaymentInstruction30();
        pmtInfs.add(pmtInf);

        pmtInf.setPmtInfId(pmtInfId != null && pmtInfId.length() > 0 ? pmtInfId : sepaId);
        pmtInf.setPmtMtd(PaymentMethod3Code.TRF);

        pmtInf.setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        pmtInf.setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));

        pmtInf.setPmtTpInf(new PaymentTypeInformation261());
        pmtInf.getPmtTpInf().setSvcLvl(new ServiceLevel8Choice());
        pmtInf.getPmtTpInf().getSvcLvl().setCd("SEPA");

        String date = sepaParams.get("date");
        if (date == null) date = SepaUtil.DATE_UNDEFINED;
        DateAndDateTimeChoice dateAndDateTimeChoice = new DateAndDateTimeChoice();
        dateAndDateTimeChoice.setDt(SepaUtil.createCalendar(date));
        pmtInf.setReqdExctnDt(dateAndDateTimeChoice);
        pmtInf.setDbtr(new PartyIdentification1352());
        pmtInf.setDbtrAcct(new CashAccount38());
        pmtInf.setDbtrAgt(new BranchAndFinancialInstitutionIdentification5Choice());

        //Payment Information - Debtor
        pmtInf.getDbtr().setNm(sepaParams.get("src.name"));

        //Payment Information - DebtorAccount
        pmtInf.getDbtrAcct().setId(new AccountIdentification4Choice());
        pmtInf.getDbtrAcct().getId().setIBAN(sepaParams.get("src.iban"));

        //Payment Information - DebtorAgent
        pmtInf.getDbtrAgt().setFinInstnId(new FinancialInstitutionIdentification8Choice());
        String srcBic = sepaParams.get("src.bic");
        if (srcBic != null && srcBic.length() > 0) // BIC ist inzwischen optional
        {
            pmtInf.getDbtrAgt().getFinInstnId().setBICFI(srcBic);
        } else {
            pmtInf.getDbtrAgt().getFinInstnId().setOthr(new OthrIdentification());
            pmtInf.getDbtrAgt().getFinInstnId().getOthr().setId(OthrIdentificationCode.NOTPROVIDED);
        }

        //Payment Information - ChargeBearer
        pmtInf.setChrgBr(ChargeBearerTypeSEPACode.SLEV);

        //Payment Information - Credit Transfer Transaction Information
        ArrayList<CreditTransferTransaction34> cdtTrxTxInfs =
            (ArrayList<CreditTransferTransaction34>) pmtInf.getCdtTrfTxInf();
        if (maxIndex != null) {
            for (int tnr = 0; tnr <= maxIndex; tnr++) {
                cdtTrxTxInfs.add(createCreditTransferTransaction34(sepaParams, tnr));
            }
        } else {
            cdtTrxTxInfs.add(createCreditTransferTransaction34(sepaParams, null));
        }

        String batch = SepaUtil.getProperty(sepaParams, "batchbook", null);
        if (batch != null)
            pmtInf.setBtchBookg(batch.equals("1"));

        ObjectFactory of = new ObjectFactory();
        try {
            this.marshal(of.createDocument(doc), os, validate);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private CreditTransferTransaction34 createCreditTransferTransaction34(Map<String,
        String> sepaParams, Integer index) {
        CreditTransferTransaction34 cdtTrxTxInf = new CreditTransferTransaction34();

        //Payment Information - Credit Transfer Transaction Information - Payment Identification
        cdtTrxTxInf.setPmtId(new PaymentIdentification6());
        cdtTrxTxInf.getPmtId().setEndToEndId(SepaUtil.getProperty(sepaParams, SepaUtil.insertIndex("endtoendid",
            index), AbstractSEPAGV.ENDTOEND_ID_NOTPROVIDED)); // sicherstellen, dass "NOTPROVIDED" eingetragen wird,
        // wenn keine ID angegeben ist

        //Payment Information - Credit Transfer Transaction Information - Creditor
        cdtTrxTxInf.setCdtr(new PartyIdentification1352());
        cdtTrxTxInf.getCdtr().setNm(sepaParams.get(SepaUtil.insertIndex("dst.name", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Account
        cdtTrxTxInf.setCdtrAcct(new CashAccount382());
        cdtTrxTxInf.getCdtrAcct().setId(new AccountIdentification4Choice());
        cdtTrxTxInf.getCdtrAcct().getId().setIBAN(sepaParams.get(SepaUtil.insertIndex("dst.iban", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Agent
        String dstBic = sepaParams.get(SepaUtil.insertIndex("dst.bic", index));
        if (dstBic != null && dstBic.length() > 0) // BIC ist inzwischen optional
        {
            cdtTrxTxInf.setCdtrAgt(new BranchAndFinancialInstitutionIdentification5());
            cdtTrxTxInf.getCdtrAgt().setFinInstnId(new FinancialInstitutionIdentification8());
            cdtTrxTxInf.getCdtrAgt().getFinInstnId().setBICFI(dstBic);
        }

        //Payment Information - Credit Transfer Transaction Information - Amount
        cdtTrxTxInf.setAmt(new AmountType4Choice());
        cdtTrxTxInf.getAmt().setInstdAmt(new ActiveOrHistoricCurrencyAndAmount());
        cdtTrxTxInf.getAmt().getInstdAmt().setValue(new BigDecimal(sepaParams.get(SepaUtil.insertIndex("btg.value",
            index))));

        cdtTrxTxInf.getAmt().getInstdAmt().setCcy(ActiveOrHistoricCurrencyCodeEUR.EUR);

        //Payment Information - Credit Transfer Transaction Information - Usage
        String usage = sepaParams.get(SepaUtil.insertIndex("usage", index));
        if (usage != null && usage.length() > 0) {
            cdtTrxTxInf.setRmtInf(new RemittanceInformation11());
            cdtTrxTxInf.getRmtInf().setUstrd(usage);
        }

        String purposeCode = sepaParams.get(SepaUtil.insertIndex("purposecode", index));
        if (purposeCode != null && purposeCode.length() > 0) {
            Purpose2Choice p = new Purpose2Choice();
            p.setCd(purposeCode);
            cdtTrxTxInf.setPurp(p);
        }

        return cdtTrxTxInf;
    }

    public SepaVersion getSepaVersion() {
        return SepaVersion.PAIN_001_001_09;
    }
}
