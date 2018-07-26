package org.kapott.hbci.manager;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidArgumentException;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.passport.HBCIPassportInternal;

import java.lang.reflect.Constructor;

@Slf4j
public class HBCIJobFactory {

    /**
     * <p>Erzeugen eines neuen Highlevel-HBCI-Jobs. Diese Methode gibt ein neues Job-Objekt zurück. Dieses
     * Objekt wird allerdings noch <em>nicht</em> zum HBCI-Dialog hinzugefügt. Statt dessen
     * müssen erst alle zur Beschreibung des jeweiligen Jobs benötigten Parameter gesetzt werden.
     * <p>Eine Beschreibung aller unterstützten Geschäftsvorfälle befindet sich
     * im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @param jobname der Name des Jobs, der erzeugt werden soll. Gültige
     *                Job-Namen sowie die benötigten Parameter sind in der Beschreibung des Packages
     *                <code>org.kapott.hbci.GV</code> zu finden.
     * @return ein Job-Objekt, für das die entsprechenden Job-Parameter gesetzt werden müssen und
     * welches anschließend zum HBCI-Dialog hinzugefügt werden kann.
     */
    public static AbstractHBCIJob newJob(String jobname, HBCIPassportInternal passport) {
        log.debug("creating new job " + jobname);

        if (jobname == null || jobname.length() == 0)
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        AbstractHBCIJob ret = null;
        String className = "org.kapott.hbci.GV.GV" + jobname;

        try {
            Class cl = Class.forName(className);
            Constructor cons = cl.getConstructor(new Class[]{HBCIPassportInternal.class});
            ret = (AbstractHBCIJob) cons.newInstance(new Object[]{passport});
        } catch (ClassNotFoundException e) {
            throw new InvalidUserDataException("*** there is no highlevel job named " + jobname + " - need class " + className);
        } catch (Exception e) {
            String msg = HBCIUtils.getLocMsg("EXCMSG_JOB_CREATE_ERR", jobname);
            throw new HBCI_Exception(msg, e);
        }

        return ret;
    }

}
