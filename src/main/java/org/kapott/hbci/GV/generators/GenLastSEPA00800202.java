package org.kapott.hbci.GV.generators;

import org.kapott.hbci.GV.AbstractSEPAGV;
import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.sepa.jaxb.pain_008_002_02.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * SEPA-Generator fuer pain.008.002.02.
 */
public class GenLastSEPA00800202 extends AbstractSEPAGenerator<HashMap<String, String>> {
    /**
     * @see org.kapott.hbci.GV.generators.AbstractSEPAGenerator#getSepaVersion()
     */
    @Override
    public SepaVersion getSepaVersion() {
        return SepaVersion.PAIN_008_002_02;
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
        doc.setCstmrDrctDbtInitn(new CustomerDirectDebitInitiationV02());
        doc.getCstmrDrctDbtInitn().setGrpHdr(new GroupHeaderSDD());

        final String sepaId = sepaParams.get("sepaid");
        final String pmtInfId = sepaParams.get("pmtinfid");

        //Group Header
        doc.getCstmrDrctDbtInitn().getGrpHdr().setMsgId(sepaId);
        doc.getCstmrDrctDbtInitn().getGrpHdr().setCreDtTm(SepaUtil.createCalendar(null));
        doc.getCstmrDrctDbtInitn().getGrpHdr().setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        doc.getCstmrDrctDbtInitn().getGrpHdr().setInitgPty(new PartyIdentificationSEPA1());
        doc.getCstmrDrctDbtInitn().getGrpHdr().getInitgPty().setNm(sepaParams.get("src.name"));
        doc.getCstmrDrctDbtInitn().getGrpHdr().setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));


        //Payment Information
        ArrayList<PaymentInstructionInformationSDD> pmtInfs = (ArrayList<PaymentInstructionInformationSDD>) doc.getCstmrDrctDbtInitn().getPmtInf();
        PaymentInstructionInformationSDD pmtInf = new PaymentInstructionInformationSDD();
        pmtInfs.add(pmtInf);

        pmtInf.setPmtInfId(pmtInfId != null && pmtInfId.length() > 0 ? pmtInfId : sepaId);
        pmtInf.setPmtMtd(PaymentMethod2Code.DD);
        pmtInf.setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        pmtInf.setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));

        pmtInf.setReqdColltnDt(SepaUtil.createCalendar(sepaParams.get("targetdate")));
        pmtInf.setCdtr(new PartyIdentificationSEPA5());
        pmtInf.setCdtrAcct(new CashAccountSEPA1());
        pmtInf.setCdtrAgt(new BranchAndFinancialInstitutionIdentificationSEPA1());

        //Payment Information
        pmtInf.getCdtr().setNm(sepaParams.get("src.name"));

        //Payment Information
        pmtInf.getCdtrAcct().setId(new AccountIdentificationSEPA());
        pmtInf.getCdtrAcct().getId().setIBAN(sepaParams.get("src.iban"));

        //Payment Information
        pmtInf.getCdtrAgt().setFinInstnId(new FinancialInstitutionIdentificationSEPA1());
        pmtInf.getCdtrAgt().getFinInstnId().setBIC(sepaParams.get("src.bic"));


        //Payment Information - ChargeBearer
        pmtInf.setChrgBr(ChargeBearerTypeSEPACode.SLEV);

        pmtInf.setPmtTpInf(new PaymentTypeInformationSDD());
        pmtInf.getPmtTpInf().setSeqTp(SequenceType1Code.fromValue(sepaParams.get("sequencetype")));

        pmtInf.getPmtTpInf().setSvcLvl(new ServiceLevelSEPA());
        pmtInf.getPmtTpInf().getSvcLvl().setCd(ServiceLevelSEPACode.SEPA);
        pmtInf.getPmtTpInf().setLclInstrm(new LocalInstrumentSEPA());

        String type = sepaParams.get("type");
        try {
            pmtInf.getPmtTpInf().getLclInstrm().setCd(LocalInstrumentSEPACode.fromValue(type));
        } catch (IllegalArgumentException e) {
            throw new HBCI_Exception("Lastschrift-Art " + type + " wird in der SEPA-Version 008.002.02 Ihrer Bank noch nicht unterstützt", e);
        }

        //Payment Information - Credit Transfer Transaction Information
        ArrayList<DirectDebitTransactionInformationSDD> drctDbtTxInfs = (ArrayList<DirectDebitTransactionInformationSDD>) pmtInf.getDrctDbtTxInf();
        if (maxIndex != null) {
            for (int tnr = 0; tnr <= maxIndex; tnr++) {
                drctDbtTxInfs.add(createDirectDebitTransactionInformationSDD(sepaParams, tnr));
            }
        } else {
            drctDbtTxInfs.add(createDirectDebitTransactionInformationSDD(sepaParams, null));
        }

        String batch = SepaUtil.getProperty(sepaParams, "batchbook", null);
        if (batch != null)
            pmtInf.setBtchBookg(batch.equals("1"));

        ObjectFactory of = new ObjectFactory();
        this.marshal(of.createDocument(doc), os, validate);
    }

    private DirectDebitTransactionInformationSDD createDirectDebitTransactionInformationSDD(HashMap<String, String> sepaParams, Integer index) throws Exception {
        DirectDebitTransactionInformationSDD drctDbtTxInf = new DirectDebitTransactionInformationSDD();

        drctDbtTxInf.setDrctDbtTx(new DirectDebitTransactionSDD());
        drctDbtTxInf.getDrctDbtTx().setCdtrSchmeId(new PartyIdentificationSEPA3());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().setId(new PartySEPA2());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().setPrvtId(new PersonIdentificationSEPA2());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().getPrvtId().setOthr(new RestrictedPersonIdentificationSEPA());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().getPrvtId().getOthr().setId(sepaParams.get(SepaUtil.insertIndex("creditorid", index)));
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().getPrvtId().getOthr().setSchmeNm(new RestrictedPersonIdentificationSchemeNameSEPA());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().getPrvtId().getOthr().getSchmeNm().setPrtry(IdentificationSchemeNameSEPA.SEPA);
        drctDbtTxInf.getDrctDbtTx().setMndtRltdInf(new MandateRelatedInformationSDD());
        drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().setMndtId(sepaParams.get(SepaUtil.insertIndex("mandateid", index)));
        drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().setDtOfSgntr(SepaUtil.createCalendar(sepaParams.get(SepaUtil.insertIndex("manddateofsig", index))));

        boolean amend = Boolean.valueOf(sepaParams.get(SepaUtil.insertIndex("amendmandindic", index)));

        drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().setAmdmntInd(amend);

        if (amend) {
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().setAmdmntInfDtls(new AmendmentInformationDetailsSDD());
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().getAmdmntInfDtls().setOrgnlDbtrAgt(new BranchAndFinancialInstitutionIdentificationSEPA2());
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().getAmdmntInfDtls().getOrgnlDbtrAgt().setFinInstnId(new FinancialInstitutionIdentificationSEPA2());
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().getAmdmntInfDtls().getOrgnlDbtrAgt().getFinInstnId().setOthr(new RestrictedFinancialIdentificationSEPA());
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().getAmdmntInfDtls().getOrgnlDbtrAgt().getFinInstnId().getOthr().setId(RestrictedSMNDACode.SMNDA);
        }

        //Payment Information - Credit Transfer Transaction Information - Payment Identification
        drctDbtTxInf.setPmtId(new PaymentIdentificationSEPA());
        drctDbtTxInf.getPmtId().setEndToEndId(SepaUtil.getProperty(sepaParams, SepaUtil.insertIndex("endtoendid", index), AbstractSEPAGV.ENDTOEND_ID_NOTPROVIDED)); // sicherstellen, dass "NOTPROVIDED" eingetragen wird, wenn keine ID angegeben ist


        //Payment Information - Credit Transfer Transaction Information - Creditor
        drctDbtTxInf.setDbtr(new PartyIdentificationSEPA2());
        drctDbtTxInf.getDbtr().setNm(sepaParams.get(SepaUtil.insertIndex("dst.name", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Account
        drctDbtTxInf.setDbtrAcct(new CashAccountSEPA2());
        drctDbtTxInf.getDbtrAcct().setId(new AccountIdentificationSEPA());
        drctDbtTxInf.getDbtrAcct().getId().setIBAN(sepaParams.get(SepaUtil.insertIndex("dst.iban", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Agent
        drctDbtTxInf.setDbtrAgt(new BranchAndFinancialInstitutionIdentificationSEPA1());
        drctDbtTxInf.getDbtrAgt().setFinInstnId(new FinancialInstitutionIdentificationSEPA1());
        drctDbtTxInf.getDbtrAgt().getFinInstnId().setBIC(sepaParams.get(SepaUtil.insertIndex("dst.bic", index)));


        //Payment Information - Credit Transfer Transaction Information - Amount
        drctDbtTxInf.setInstdAmt(new ActiveOrHistoricCurrencyAndAmountSEPA());
        drctDbtTxInf.getInstdAmt().setValue(new BigDecimal(sepaParams.get(SepaUtil.insertIndex("btg.value", index))));

        drctDbtTxInf.getInstdAmt().setCcy(ActiveOrHistoricCurrencyCodeEUR.EUR);

        //Payment Information - Credit Transfer Transaction Information - Usage
        String usage = sepaParams.get(SepaUtil.insertIndex("usage", index));
        if (usage != null && usage.length() > 0) {
            drctDbtTxInf.setRmtInf(new RemittanceInformationSEPA1Choice());
            drctDbtTxInf.getRmtInf().setUstrd(usage);
        }

        String purposeCode = sepaParams.get(SepaUtil.insertIndex("purposecode", index));
        if (purposeCode != null && purposeCode.length() > 0) {
            PurposeSEPA p = new PurposeSEPA();
            p.setCd(purposeCode);
            drctDbtTxInf.setPurp(p);
        }

        return drctDbtTxInf;
    }

}
