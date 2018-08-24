package org.kapott.hbci.GV.parsers;

import org.kapott.hbci.GV.SepaUtil;
import org.kapott.hbci.sepa.jaxb.pain_008_001_01.*;

import javax.xml.bind.JAXB;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Parser-Implementierung fuer Pain 008.001.01.
 */
public class ParsePain00800101 extends AbstractSepaParser<List<HashMap<String, String>>> {
    /**
     * @see org.kapott.hbci.GV.parsers.ISEPAParser#parse(InputStream, Object)
     */
    public void parse(InputStream xml, List<HashMap<String, String>> sepaResults) {
        Document doc = JAXB.unmarshal(xml, Document.class);
        Pain00800101 pain = doc.getPain00800101();

        if (pain == null)
            return;

        PaymentInstructionInformation5 pmtInf = pain.getPmtInf();

        List<DirectDebitTransactionInformation2> txList = pmtInf.getDrctDbtTxInf();

        for (DirectDebitTransactionInformation2 tx : txList) {
            HashMap<String, String> prop = new HashMap();

            put(prop, Names.PMTINFID, pmtInf.getPmtInfId());
            put(prop, Names.SRC_NAME, pain.getGrpHdr().getInitgPty().getNm());
            put(prop, Names.SRC_IBAN, pmtInf.getCdtrAcct().getId().getIBAN());
            put(prop, Names.SRC_BIC, pmtInf.getCdtrAgt().getFinInstnId().getBIC());

            put(prop, Names.DST_NAME, tx.getDbtr().getNm());
            put(prop, Names.DST_IBAN, tx.getDbtrAcct().getId().getIBAN());
            put(prop, Names.DST_BIC, tx.getDbtrAgt().getFinInstnId().getBIC());

            EuroMax9Amount amt = tx.getInstdAmt();
            put(prop, Names.VALUE, SepaUtil.format(amt.getValue()));
            put(prop, Names.CURR, amt.getCcy());

            if (tx.getRmtInf() != null) {
                put(prop, Names.USAGE, tx.getRmtInf().getUstrd());
            }

            XMLGregorianCalendar date = pmtInf.getReqdColltnDt();
            if (date != null) {
                put(prop, Names.TARGETDATE, SepaUtil.format(date, null));
            }

            put(prop, Names.ENDTOENDID, tx.getPmtId().getEndToEndId());

            put(prop, Names.CREDITORID, tx.getDrctDbtTx().getCdtrSchmeId().getId().getPrvtId().getOthrId().getId());
            put(prop, Names.MANDATEID, tx.getDrctDbtTx().getMndtRltdInf().getMndtId());

            XMLGregorianCalendar mandDate = tx.getDrctDbtTx().getMndtRltdInf().getDtOfSgntr();
            if (mandDate != null) {
                put(prop, Names.MANDDATEOFSIG, SepaUtil.format(mandDate, null));
            }

            put(prop, Names.SEQUENCETYPE, pmtInf.getPmtTpInf().getSeqTp().value());

            // CORE/COR1/B2B gibts in 008.001.01 noch nicht

            sepaResults.add(prop);
        }
    }
}
