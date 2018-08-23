package org.kapott.hbci.GV.generators;

import org.kapott.hbci.GV.AbstractSEPAGV;
import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.sepa.SepaVersion;
import org.kapott.hbci.sepa.jaxb.pain_008_001_01.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * SEPA-Generator fuer das Schema pain.008.001.01.
 */
public class GenLastSEPA00800101 extends AbstractSEPAGenerator<HashMap<String, String>> {
    /**
     * @see org.kapott.hbci.GV.generators.AbstractSEPAGenerator#getSepaVersion()
     */
    @Override
    public SepaVersion getSepaVersion() {
        return SepaVersion.PAIN_008_001_01;
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
        doc.setPain00800101(new Pain00800101());
        doc.getPain00800101().setGrpHdr(new GroupHeader20());

        final String sepaId = sepaParams.get("sepaid");
        final String pmtInfId = sepaParams.get("pmtinfid");

        //Group Header
        doc.getPain00800101().getGrpHdr().setMsgId(sepaId);
        doc.getPain00800101().getGrpHdr().setCreDtTm(SepaUtil.createCalendar(null));
        doc.getPain00800101().getGrpHdr().setNbOfTxs(String.valueOf(maxIndex != null ? maxIndex + 1 : 1));
        doc.getPain00800101().getGrpHdr().setCtrlSum(SepaUtil.sumBtgValue(sepaParams, maxIndex));
        doc.getPain00800101().getGrpHdr().setGrpg(Grouping2Code.GRPD);

        doc.getPain00800101().getGrpHdr().setInitgPty(new PartyIdentification20());
        doc.getPain00800101().getGrpHdr().getInitgPty().setNm(sepaParams.get("src.name"));

        //Payment Information
        PaymentInstructionInformation5 pmtInf = new PaymentInstructionInformation5();
        doc.getPain00800101().setPmtInf(pmtInf);

        pmtInf.setPmtInfId(pmtInfId != null && pmtInfId.length() > 0 ? pmtInfId : sepaId);
        pmtInf.setPmtMtd(PaymentMethod2Code.DD);

        pmtInf.setReqdColltnDt(SepaUtil.createCalendar(sepaParams.get("targetdate")));
        pmtInf.setCdtr(new PartyIdentification22());
        pmtInf.setCdtrAcct(new CashAccount8());
        pmtInf.setCdtrAgt(new FinancialInstitution2());

        //Payment Information
        pmtInf.getCdtr().setNm(sepaParams.get("src.name"));

        //Payment Information
        pmtInf.getCdtrAcct().setId(new AccountIdentification2());
        pmtInf.getCdtrAcct().getId().setIBAN(sepaParams.get("src.iban"));

        //Payment Information
        pmtInf.getCdtrAgt().setFinInstnId(new FinancialInstitutionIdentification4());
        pmtInf.getCdtrAgt().getFinInstnId().setBIC(sepaParams.get("src.bic"));


        //Payment Information - ChargeBearer
        pmtInf.setChrgBr(ChargeBearerType2Code.SLEV);

        pmtInf.setPmtTpInf(new PaymentTypeInformation8());
        pmtInf.getPmtTpInf().setSvcLvl(new ServiceLevel4());
        pmtInf.getPmtTpInf().getSvcLvl().setCd(ServiceLevel3Code.SEPA);
        pmtInf.getPmtTpInf().setSeqTp(SequenceType1Code.fromValue(sepaParams.get("sequencetype")));

        //Payment Information - Credit Transfer Transaction Information
        ArrayList<DirectDebitTransactionInformation2> drctDbtTxInfs = (ArrayList<DirectDebitTransactionInformation2>) pmtInf.getDrctDbtTxInf();
        if (maxIndex != null) {
            for (int tnr = 0; tnr <= maxIndex; tnr++) {
                drctDbtTxInfs.add(createDirectDebitTransactionInformation2(sepaParams, tnr));
            }
        } else {
            drctDbtTxInfs.add(createDirectDebitTransactionInformation2(sepaParams, null));
        }

        ObjectFactory of = new ObjectFactory();
        this.marshal(of.createDocument(doc), os, validate);
    }

    private DirectDebitTransactionInformation2 createDirectDebitTransactionInformation2(HashMap<String, String> sepaParams, Integer index) throws Exception {
        DirectDebitTransactionInformation2 drctDbtTxInf = new DirectDebitTransactionInformation2();

        drctDbtTxInf.setDrctDbtTx(new DirectDebitTransaction4());
        drctDbtTxInf.getDrctDbtTx().setCdtrSchmeId(new PartyIdentification11());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().setId(new PartyPrivate1());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().setPrvtId(new PersonIdentification4());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().getPrvtId().setOthrId(new RestrictedIdentification2());
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().getPrvtId().getOthrId().setId(sepaParams.get(SepaUtil.insertIndex("creditorid", index)));
        drctDbtTxInf.getDrctDbtTx().getCdtrSchmeId().getId().getPrvtId().getOthrId().setIdTp("SEPA");


        drctDbtTxInf.getDrctDbtTx().setMndtRltdInf(new MandateRelatedInformation4());
        drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().setMndtId(sepaParams.get(SepaUtil.insertIndex("mandateid", index)));
        drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().setDtOfSgntr(SepaUtil.createCalendar(sepaParams.get(SepaUtil.insertIndex("manddateofsig", index))));

        boolean amend = Boolean.valueOf(sepaParams.get(SepaUtil.insertIndex("amendmandindic", index)));

        drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().setAmdmntInd(amend);

        if (amend) {
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().setAmdmntInfDtls(new AmendmentInformationDetails4());
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().getAmdmntInfDtls().setOrgnlDbtrAgt(new FinancialInstitution3());
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().getAmdmntInfDtls().getOrgnlDbtrAgt().setFinInstnId(new FinancialInstitutionIdentification5());
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().getAmdmntInfDtls().getOrgnlDbtrAgt().getFinInstnId().setPrtryId(new RestrictedIdentification1());
            drctDbtTxInf.getDrctDbtTx().getMndtRltdInf().getAmdmntInfDtls().getOrgnlDbtrAgt().getFinInstnId().getPrtryId().setId("SMNDA");
        }

        //Payment Information - Credit Transfer Transaction Information - Payment Identification
        drctDbtTxInf.setPmtId(new PaymentIdentification1());
        drctDbtTxInf.getPmtId().setEndToEndId(SepaUtil.getProperty(sepaParams, SepaUtil.insertIndex("endtoendid", index), AbstractSEPAGV.ENDTOEND_ID_NOTPROVIDED)); // sicherstellen, dass "NOTPROVIDED" eingetragen wird, wenn keine ID angegeben ist


        //Payment Information - Credit Transfer Transaction Information - Debitor
        drctDbtTxInf.setDbtr(new PartyIdentification23());
        drctDbtTxInf.getDbtr().setNm(sepaParams.get(SepaUtil.insertIndex("dst.name", index)));

        //Payment Information - Credit Transfer Transaction Information - Debitor Account
        drctDbtTxInf.setDbtrAcct(new CashAccount8());
        drctDbtTxInf.getDbtrAcct().setId(new AccountIdentification2());
        drctDbtTxInf.getDbtrAcct().getId().setIBAN(sepaParams.get(SepaUtil.insertIndex("dst.iban", index)));

        //Payment Information - Credit Transfer Transaction Information - Creditor Agent
        drctDbtTxInf.setDbtrAgt(new FinancialInstitution2());
        drctDbtTxInf.getDbtrAgt().setFinInstnId(new FinancialInstitutionIdentification4());
        drctDbtTxInf.getDbtrAgt().getFinInstnId().setBIC(sepaParams.get(SepaUtil.insertIndex("dst.bic", index)));


        //Payment Information - Credit Transfer Transaction Information - Amount
        drctDbtTxInf.setInstdAmt(new EuroMax9Amount());
        drctDbtTxInf.getInstdAmt().setValue(new BigDecimal(sepaParams.get(SepaUtil.insertIndex("btg.value", index))));

        drctDbtTxInf.getInstdAmt().setCcy(sepaParams.get(SepaUtil.insertIndex("btg.curr", index)));

        //Payment Information - Credit Transfer Transaction Information - Usage
        String usage = sepaParams.get(SepaUtil.insertIndex("usage", index));
        if (usage != null && usage.length() > 0) {
            drctDbtTxInf.setRmtInf(new RemittanceInformation3());
            drctDbtTxInf.getRmtInf().setUstrd(usage);
        }

        return drctDbtTxInf;
    }

}
