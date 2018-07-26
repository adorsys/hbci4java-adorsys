package org.kapott.hbci.GV.parsers;

import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.sepa.jaxb.pain_001_002_03.*;

import javax.xml.bind.JAXB;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;


/**
 * Parser-Implementierung fuer Pain 001.002.03.
 */
public class ParsePain00100203 extends AbstractSepaParser {

    /**
     * @see org.kapott.hbci.GV.parsers.ISEPAParser#parse(java.io.InputStream, java.util.List)
     */
    public void parse(InputStream xml, List<HashMap<String, String>> sepaResults) {

        Document doc = JAXB.unmarshal(xml, Document.class);
        CustomerCreditTransferInitiationV03 pain = doc.getCstmrCdtTrfInitn();

        if (pain == null)
            return;

        //Payment Information 
        List<PaymentInstructionInformationSCT> pmtInfs = pain.getPmtInf();

        for (PaymentInstructionInformationSCT pmtInf : pmtInfs) {

            //Payment Information - Credit Transfer Transaction Information
            List<CreditTransferTransactionInformationSCT> txList = pmtInf.getCdtTrfTxInf();

            for (CreditTransferTransactionInformationSCT tx : txList) {
                HashMap<String, String> prop = new HashMap<>();

                put(prop, Names.PMTINFID, pmtInf.getPmtInfId());
                put(prop, Names.SRC_NAME, pain.getGrpHdr().getInitgPty().getNm());
                put(prop, Names.SRC_IBAN, pmtInf.getDbtrAcct().getId().getIBAN());
                put(prop, Names.SRC_BIC, pmtInf.getDbtrAgt().getFinInstnId().getBIC());

                put(prop, Names.DST_NAME, tx.getCdtr().getNm());
                put(prop, Names.DST_IBAN, tx.getCdtrAcct().getId().getIBAN());
                put(prop, Names.DST_BIC, tx.getCdtrAgt().getFinInstnId().getBIC());

                ActiveOrHistoricCurrencyAndAmountSEPA amt = tx.getAmt().getInstdAmt();
                put(prop, Names.VALUE, SepaUtil.format(amt.getValue()));
                put(prop, Names.CURR, amt.getCcy().value());

                if (tx.getRmtInf() != null) {
                    put(prop, Names.USAGE, tx.getRmtInf().getUstrd());
                }

                PurposeSEPA purp = tx.getPurp();
                if (purp != null)
                    put(prop, Names.PURPOSECODE, purp.getCd());

                XMLGregorianCalendar date = pmtInf.getReqdExctnDt();
                if (date != null) {
                    put(prop, Names.DATE, SepaUtil.format(date, null));
                }

                PaymentIdentificationSEPA pmtId = tx.getPmtId();
                if (pmtId != null) {
                    put(prop, Names.ENDTOENDID, pmtId.getEndToEndId());
                }

                sepaResults.add(prop);
            }
        }
    }
}
