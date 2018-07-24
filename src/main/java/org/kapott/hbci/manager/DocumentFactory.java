package org.kapott.hbci.manager;

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;

public final class DocumentFactory {

    public static Document createDocument(String hbciversion) {
        String filename = "hbci-" + hbciversion + ".xml";

        try (InputStream syntaxStream = MessageFactory.class.getClassLoader().getResourceAsStream(filename)) {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setValidating(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(syntaxStream);
        } catch (FactoryConfigurationError e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_MSGGEN_DBFAC"), e);
        } catch (ParserConfigurationException e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_MSGGEN_DB"), e);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_MSGGEN_STXFILE"), e);
        }
    }
}
