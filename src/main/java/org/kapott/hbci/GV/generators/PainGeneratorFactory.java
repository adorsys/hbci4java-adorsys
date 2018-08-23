package org.kapott.hbci.GV.generators;

import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.GV.AbstractSEPAGV;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.sepa.SepaVersion;

import java.util.logging.Logger;

/**
 * Factory zum Ermitteln des passenden Pain-Generators fuer den angegebenen Job.
 * <p>
 * WICHTIG: Diese Klasse sowie die Ableitungen sollten auch ohne initialisiertes HBCI-System
 * funktionieren, um das XML ohne HBCI-Handler erstellen zu koennen. Daher sollte auf die
 * Verwendung von "HBCIUtils" & Co verzichtet werden. Das ist auch der Grund, warum hier
 * das Java-Logging verwendet wird und nicht das HBCI4Java-eigene.
 */
public class PainGeneratorFactory {
    private final static Logger LOG = Logger.getLogger(PainGeneratorFactory.class.getName());

    /**
     * Gibt den passenden SEPA Generator für die angegebene PAIN-Version.
     *
     * @param job     der zu erzeugende Job.
     * @param version die PAIN-Version.
     * @return ISEPAGenerator
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public static PainGeneratorIf get(AbstractHBCIJob job, SepaVersion version) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String jobname = ((AbstractSEPAGV) job).getPainJobName(); // referenzierter pain-Geschäftsvorfall
        return get(jobname, version);
    }

    /**
     * Gibt den passenden SEPA Generator für die angegebene PAIN-Version.
     *
     * @param jobname der Job-Name. Z.Bsp. "UebSEPA".
     * @param version die PAIN-Version.
     * @return ISEPAGenerator
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static PainGeneratorIf get(String jobname, SepaVersion version) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (!version.canGenerate(jobname))
            throw new InvalidUserDataException("SEPA version is not supported: " + version);

        String className = version.getGeneratorClass(jobname);
        LOG.fine("trying to init SEPA creator: " + className);
        Class cl = Class.forName(className);
        return (PainGeneratorIf) cl.newInstance();
    }

}
