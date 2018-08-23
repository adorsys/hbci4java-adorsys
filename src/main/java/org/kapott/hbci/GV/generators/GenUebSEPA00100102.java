package org.kapott.hbci.GV.generators;

import org.kapott.hbci.GV.AbstractSEPAGV;
import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.sepa.jaxb.pain_001_001_02.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * SEPA-Generator fuer pain.001.001.02.
 */
public class GenUebSEPA00100102 extends AbstractSEPAGenerator<HashMap<String, String>> {
    /**
     * @see org.kapott.hbci.GV.generators.AbstractSEPAGenerator#getSepaVersion()
     */
    @Override
    public SepaVersion getSepaVersion() {
        return SepaVersion.PAIN_001_001_02;
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


        doc.getPain00100102().setGrpHdr(new GroupHeader20());

        final String sepaId = sepaParams.get("sepaid");
        final String pmtInfId = sepaParams.get("pmtinfid");

        //Group Header
        doc.getPain00100102().getGrpHdr().setMsgId(sepaId);
        doc.getPain00100102().getGrpHdr().setCreDtTm(SepaUtil.createCalendar(null));
        doc.getPain00100102().getGrpHdr().setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        doc.getPain00100102().getGrpHdr().setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));
        doc.getPain00100102().getGrpHdr().setGrpg(Grouping2Code.GRPD);
        doc.getPain00100102().getGrpHdr().setInitgPty(new PartyIdentification20());
        doc.getPain00100102().getGrpHdr().getInitgPty().setNm(sepaParams.get("src.name"));


        //Payment Information
        PaymentInstructionInformation4 pmtInf = new PaymentInstructionInformation4();
        doc.getPain00100102().setPmtInf(pmtInf);

        pmtInf.setPmtInfId(pmtInfId != null && pmtInfId.length() > 0 ? pmtInfId : sepaId);
        pmtInf.setPmtMtd(PaymentMethod5Code.TRF);

        // Payment Type Information
        pmtInf.setPmtTpInf(new PaymentTypeInformation7());
        pmtInf.getPmtTpInf().setSvcLvl(new ServiceLevel4());
        pmtInf.getPmtTpInf().getSvcLvl().setCd(ServiceLevel3Code.SEPA);

        String date = sepaParams.get("date");
        if (date == null) date = SepaUtil.DATE_UNDEFINED;
        pmtInf.setReqdExctnDt(SepaUtil.createCalendar(date));
        pmtInf.setDbtr(new PartyIdentification23());
        pmtInf.setDbtrAcct(new CashAccount8());
        pmtInf.setDbtrAgt(new FinancialInstitution2());


        //Payment Information - Debtor
        pmtInf.getDbtr().setNm(sepaParams.get("src.name"));


        //Payment Information - DebtorAccount
        pmtInf.getDbtrAcct().setId(new AccountIdentification2());
        pmtInf.getDbtrAcct().getId().setIBAN(sepaParams.get("src.iban"));


        //Payment Information - DebtorAgent
        pmtInf.getDbtrAgt().setFinInstnId(new FinancialInstitutionIdentification4());
        pmtInf.getDbtrAgt().getFinInstnId().setBIC(sepaParams.get("src.bic"));


        //Payment Information - ChargeBearer
        pmtInf.setChrgBr(ChargeBearerType2Code.SLEV);


        //Payment Information - Credit Transfer Transaction Information
        ArrayList<CreditTransferTransactionInformation2> cdtTrxTxInfs = (ArrayList<CreditTransferTransactionInformation2>) pmtInf.getCdtTrfTxInf();
        if (maxIndex != null) {
            for (int tnr = 0; tnr <= maxIndex; tnr++) {
                cdtTrxTxInfs.add(createCreditTransferTransactionInformation2(sepaParams, tnr));
            }
        } else {
            cdtTrxTxInfs.add(createCreditTransferTransactionInformation2(sepaParams, null));
        }

        ObjectFactory of = new ObjectFactory();
        this.marshal(of.createDocument(doc), os, validate);
    }

    private CreditTransferTransactionInformation2 createCreditTransferTransactionInformation2(HashMap<String, String> sepaParams, Integer index) {
        CreditTransferTransactionInformation2 cdtTrxTxInf = new CreditTransferTransactionInformation2();

        //Payment Information - Credit Transfer Transaction Information - Payment Identification
        cdtTrxTxInf.setPmtId(new PaymentIdentification1());
        cdtTrxTxInf.getPmtId().setEndToEndId(SepaUtil.getProperty(sepaParams, SepaUtil.insertIndex("endtoendid", index), AbstractSEPAGV.ENDTOEND_ID_NOTPROVIDED)); // sicherstellen, dass "NOTPROVIDED" eingetragen wird, wenn keine ID angegeben ist


        //Payment Information - Credit Transfer Transaction Information - Creditor
        cdtTrxTxInf.setCdtr(new PartyIdentification21());
        cdtTrxTxInf.getCdtr().setNm(sepaParams.get(SepaUtil.insertIndex("dst.name", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Account
        cdtTrxTxInf.setCdtrAcct(new CashAccount8());
        cdtTrxTxInf.getCdtrAcct().setId(new AccountIdentification2());
        cdtTrxTxInf.getCdtrAcct().getId().setIBAN(sepaParams.get(SepaUtil.insertIndex("dst.iban", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Agent
        cdtTrxTxInf.setCdtrAgt(new FinancialInstitution2());
        cdtTrxTxInf.getCdtrAgt().setFinInstnId(new FinancialInstitutionIdentification4());
        cdtTrxTxInf.getCdtrAgt().getFinInstnId().setBIC(sepaParams.get(SepaUtil.insertIndex("dst.bic", index)));


        //Payment Information - Credit Transfer Transaction Information - Amount
        cdtTrxTxInf.setAmt(new AmountType3());
        cdtTrxTxInf.getAmt().setInstdAmt(new EuroMax9Amount());
        cdtTrxTxInf.getAmt().getInstdAmt().setValue(new BigDecimal(sepaParams.get(SepaUtil.insertIndex("btg.value", index))));

        cdtTrxTxInf.getAmt().getInstdAmt().setCcy("EUR");

        //Payment Information - Credit Transfer Transaction Information - Usage
        String usage = sepaParams.get(SepaUtil.insertIndex("usage", index));
        if (usage != null && usage.length() > 0) {
            cdtTrxTxInf.setRmtInf(new RemittanceInformation3());
            cdtTrxTxInf.getRmtInf().setUstrd(usage);
        }

        return cdtTrxTxInf;
    }
}
