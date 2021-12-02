package org.kapott.hbci.GV.parsers;

import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.sepa.jaxb.pain_001_001_09.*;

import javax.xml.bind.JAXB;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Parser-Implementierung fuer Pain 001.001.09.
 */
public class ParsePain00100109 extends AbstractSepaParser<List<HashMap<String, String>>> {
    /**
     * @see ISEPAParser#parse(InputStream, Object)
     */
    public void parse(InputStream xml, List<HashMap<String, String>> sepaResults) {
        Document doc = JAXB.unmarshal(xml, Document.class);
        CustomerCreditTransferInitiationV09 pain = doc.getCstmrCdtTrfInitn();

        if (pain == null)
            return;

        //Payment Information
        List<PaymentInstruction30> pmtInfs = pain.getPmtInf();

        for (PaymentInstruction30 pmtInf : pmtInfs) {

            //Payment Information - Credit Transfer Transaction Information
            List<CreditTransferTransaction34> txList = pmtInf.getCdtTrfTxInf();

            for (CreditTransferTransaction34 tx : txList) {
                HashMap<String, String> prop = new HashMap<>();

                put(prop, Names.PMTINFID, pmtInf.getPmtInfId());
                put(prop, Names.SRC_NAME, pain.getGrpHdr().getInitgPty().getNm());
                put(prop, Names.SRC_IBAN, pmtInf.getDbtrAcct().getId().getIBAN());
                put(prop, Names.SRC_BIC, pmtInf.getDbtrAgt().getFinInstnId().getBICFI());

                put(prop, Names.DST_NAME, tx.getCdtr().getNm());
                put(prop, Names.DST_IBAN, tx.getCdtrAcct().getId().getIBAN());

                try {
                    put(prop, Names.DST_BIC, tx.getCdtrAgt().getFinInstnId().getBICFI());
                } catch (Exception e) {
                    // BIC darf fehlen
                }

                ActiveOrHistoricCurrencyAndAmount amt = tx.getAmt().getInstdAmt();
                put(prop, Names.VALUE, SepaUtil.format(amt.getValue()));
                put(prop, Names.CURR, amt.getCcy().value());

                if (tx.getRmtInf() != null) {
                    put(prop, Names.USAGE, tx.getRmtInf().getUstrd());
                }

                Purpose2Choice purp = tx.getPurp();
                if (purp != null)
                    put(prop, Names.PURPOSECODE, purp.getCd());

                DateAndDateTimeChoice date = pmtInf.getReqdExctnDt();
                if (date != null) {
                    Stream.of(date.getDt(), date.getDtTm())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent( calendar -> put(prop, Names.DATE, SepaUtil.format(calendar, null)));
                }

                PaymentIdentification6 pmtId = tx.getPmtId();
                if (pmtId != null) {
                    put(prop, Names.ENDTOENDID, pmtId.getEndToEndId());
                }

                sepaResults.add(prop);
            }
        }
    }
}
