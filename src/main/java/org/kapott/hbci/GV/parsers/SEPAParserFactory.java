package org.kapott.hbci.GV.parsers;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.sepa.SepaVersion;

/**
 * Factory zum Erzeugen von Parsern fuer das Einlesen von SEPA-XML-Daten.
 */
@Slf4j
public class SEPAParserFactory {
    /**
     * Gibt den passenden SEPA Parser für die angegebene PAIN-Version.
     *
     * @param version die PAIN-Version.
     * @return ISEPAParser
     */
    public static ISEPAParser get(SepaVersion version) {
        ISEPAParser parser = null;

        String className = version.getParserClass();
        try {
            log.debug("trying to init SEPA parser: " + className);
            Class cl = Class.forName(className);
            parser = (ISEPAParser) cl.newInstance();
        } catch (Exception e) {
            String msg = "Error creating SEPA parser";
            throw new HBCI_Exception(msg, e);
        }
        return parser;
    }
}
