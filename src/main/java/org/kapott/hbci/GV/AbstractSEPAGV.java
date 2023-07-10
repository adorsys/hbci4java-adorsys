package org.kapott.hbci.GV;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV.generators.PainGeneratorFactory;
import org.kapott.hbci.GV.generators.PainGeneratorIf;
import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.comm.CommPinTan;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.sepa.SepaVersion;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Abstrakte Basis-Klasse fuer JAXB-basierte SEPA-Jobs.
 */
@Slf4j
public abstract class AbstractSEPAGV extends AbstractHBCIJob {
    /**
     * Token, der als End-to-End ID Platzhalter verwendet wird, wenn keine angegeben wurde.
     * In painVersion.001.001.02 wurde dieser Token noch explizit erwaehnt. Inzwischen nicht mehr.
     * Nach Ruecksprache mit Holger vom onlinebanking-forum.de weiss ich aber, dass VRNetworld
     * den auch verwendet und er von Banken als solcher erkannt wird.
     */
    public static final String ENDTOEND_ID_NOTPROVIDED = "NOTPROVIDED";

    @Getter
    @Setter
    private Map<String, String> painParams = new HashMap<>();
    private final SepaVersion sepaVersion;
    private PainGeneratorIf generator = null;

    public AbstractSEPAGV(HBCIPassportInternal passport, String name, SepaVersion sepaVersion, HBCIJobResultImpl jobResult) {
        super(passport, name, jobResult != null ? jobResult : new HBCIJobResultImpl(passport));
        this.sepaVersion = sepaVersion != null ? sepaVersion : this.determinePainVersion(passport, name);
    }

    /**
     * Liefert die Default-PAIN-Version, das verwendet werden soll,
     * wenn von der Bank keine geliefert wurden.
     *
     * @return Default-Pain-Version.
     */
    protected abstract SepaVersion getDefaultPainVersion();

    /**
     * Liefert den PAIN-Type.
     *
     * @return der PAIN-Type.
     */
    protected abstract SepaVersion.Type getPainType();

    /**
     * Diese Methode schaut in den BPD nach den unterstützen painVersion Versionen
     * (bei LastSEPA painVersion.008.xxx.xx) und vergleicht diese mit den von HBCI4Java
     * unterstützen painVersion Versionen. Der größte gemeinsamme Nenner wird
     * zurueckgeliefert.
     *
     * @param passport passport
     * @param gvName   der Geschaeftsvorfall fuer den in den BPD nach dem PAIN-Versionen
     *                 gesucht werden soll.
     * @return die ermittelte PAIN-Version.
     */
    private SepaVersion determinePainVersion(HBCIPassportInternal passport, String gvName) {
        // Schritt 1: Wir holen uns die globale maximale PAIN-Version
        SepaVersion globalVersion = this.determinePainVersionInternal(passport, GVSEPAInfo.getLowlevelName());

        // Schritt 2: Die des Geschaeftsvorfalls - fuer den Fall, dass die Bank
        // dort weitere Einschraenkungen hinterlegt hat
        SepaVersion jobVersion = this.determinePainVersionInternal(passport, gvName);

        // Wir haben gar keine PAIN-Version gefunden
        if (globalVersion == null && jobVersion == null) {
            SepaVersion def = this.getDefaultPainVersion();
            log.warn("unable to determine matching painVersion version, using default: " + def);
            return def;
        }

        // Wenn wir keine GV-spezifische haben, dann nehmen wir die globale
        if (jobVersion == null) {
            log.debug("have no job-specific painVersion version, using global painVersion version: " + globalVersion);
            return globalVersion;
        }

        // Ansonsten hat die vom Job Vorrang:
        log.debug("using job-specific painVersion version: " + jobVersion);
        return jobVersion;
    }

    /**
     * Diese Methode schaut in den BPD nach den unterstützen painVersion Versionen
     * (bei LastSEPA painVersion.008.xxx.xx) und vergleicht diese mit den von HBCI4Java
     * unterstützen painVersion Versionen. Der größte gemeinsamme Nenner wird
     * zurueckgeliefert.
     *
     * @param passport passport
     * @param gvName   der Geschaeftsvorfall fuer den in den BPD nach dem PAIN-Versionen
     *                 gesucht werden soll.
     * @return die ermittelte PAIN-Version oder NULL wenn keine ermittelt werden konnte.
     */
    private SepaVersion determinePainVersionInternal(HBCIPassportInternal passport, final String gvName) {
        log.debug("searching for supported painVersion versions for GV " + gvName);

        if (!passport.jobSupported(gvName)) {
            log.debug("don't have any BPD for GV " + gvName);
            return null;
        }

        List<SepaVersion> found = new ArrayList<>();

        // GV-Restrictions laden und darüber iterieren
        Map<String, String> props = passport.getLowlevelJobRestrictions(gvName);
        for (String key : props.keySet()) {
            // Die Keys, welche die Schema-Versionen enthalten, heissen alle "suppformats*"
            if (!key.startsWith("suppformats"))
                continue;

            String urn = props.get(key);
            try {
                SepaVersion version = SepaVersion.byURN(urn);
                if (version.getType() == this.getPainType()) {
                    if (!version.isSupported(this.getPainJobName())) {
                        log.debug("  unsupported " + version);
                        continue;
                    }

                    // Frueher wurde hier noch geschaut, ob die PAIN-Version per
                    // PainVersion.getKnownVersions bekannt ist. In dem Fall wurde
                    // stattdessen unsere verwendet, damit beim Senden des Auftrages
                    // der korrekte URN verwendet wird. Das ist inzwischen nicht mehr
                    // noetig, da das "PainVersion.byURN" (siehe oben) ohnehin bereits
                    // macht - wenn wir die PAIN-Version kennen, nehmen wir gleich die
                    // eigene Instanz. Siehe auch
                    // TestPainVersion#test011 bzw. http://www.onlinebanking-forum.de/phpBB2/viewtopic.php?p=95160#95160
                    log.debug("  found " + version);
                    found.add(version);
                }
            } catch (Exception e) {
                log.warn("ignoring invalid painVersion version " + urn);
                log.error(e.getMessage(), e);
            }
        }

        return SepaVersion.findGreatest(found);
    }

    /**
     * @see AbstractHBCIJob#setLowlevelParam(java.lang.String, java.lang.String)
     * This is needed to "redirect" the sepa values. They dont have to stored
     * directly in the message, but have to go into the SEPA document which will
     * by created later (in verifyConstraints())
     */
    @Override
    public void setLowlevelParam(String key, String value) {
        String intern = getName() + ".sepa.";

        if (key.startsWith(intern)) {
            String realKey = key.substring(intern.length());
            this.painParams.put(realKey, value);
            log.debug("setting SEPA param " + realKey + " = " + value);
        } else {
            super.setLowlevelParam(key, value);
        }
    }

    /**
     * This is needed for verifyConstraints(). Because verifyConstraints() tries
     * to read the lowlevel-values for each constraint, the lowlevel-values for
     * sepa.xxx would always be empty (because they do not exist in hbci
     * messages). So we read the sepa lowlevel-values from the special sepa
     * structure instead from the lowlevel params for the message
     *
     * @param key key
     * @return the lowlevel param.
     */
    @Override
    public String getLowlevelParam(String key) {
        String result;

        String intern = getName() + ".sepa.";
        if (key.startsWith(intern)) {
            String realKey = key.substring(intern.length());
            result = getPainParam(realKey);
        } else {
            result = super.getLowlevelParam(key);
        }

        return result;
    }

    /**
     * Gibt die SEPA Message ID als String zurück. Existiert noch keine wird sie
     * aus Datum und User ID erstellt.
     *
     * @return SEPA Message ID
     */
    public String getPainMessageId() {
        String result = getPainParam("messageId");
        if (result == null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSS");
            result = format.format(new Date());
            result = result.substring(0, Math.min(result.length(), 35));
            setSEPAParam("messageId", result);
        }
        return result;
    }

    /**
     * Liefert den passenden SEPA-Generator.
     *
     * @return der SEPA-Generator.
     */
    private PainGeneratorIf getPainGenerator() {
        if (this.generator == null) {
            try {
                this.generator = PainGeneratorFactory.get(this, this.getSepaVersion());
            } catch (Exception e) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_JOB_CREATE_ERR", this.getPainJobName());
                throw new HBCI_Exception(msg, e);
            }

        }
        return this.generator;
    }

    /**
     * Liefert den zu verwendenden PAIN-Version fuer die HBCI-Nachricht.
     *
     * @return der zu verwendende PAIN-Version fuer die HBCI-Nachricht.
     */
    public SepaVersion getSepaVersion() {
        return this.sepaVersion;
    }

    void setSepaVersion(String version) {
        setLowlevelParam(getName() + ".sepadescr", version);
    }

    /**
     * Erstellt die XML für diesen Job und schreibt diese in den _sepapain
     * Parameter des Jobs
     */
    protected void createPainXml() {
        // Hier wird die XML rein geschrieben
        ByteArrayOutputStream o = new ByteArrayOutputStream();

        // Passenden SEPA Generator zur verwendeten painVersion Version laden
        PainGeneratorIf gen = this.getPainGenerator();

        // Die XML in den baos schreiben, ggf fehler behandeln
        try {
            gen.generate(this.painParams, o, false);
        } catch (HBCI_Exception he) {
            throw he;
        } catch (Exception e) {
            throw new HBCI_Exception("*** the _sepapain segment for this job can not be created", e);
        }

        // Prüfen ob die XML erfolgreich generiert wurde
        if (o.size() == 0)
            throw new HBCI_Exception("*** the _sepapain segment for this job can not be created");

        try {
            String xml = o.toString(CommPinTan.ENCODING.toString());
            log.debug("generated XML:\n" + xml);
            setParam("_sepapain", "B" + xml);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String getRawData() {
        return getLowlevelParam(getName() + ".sepapain");
    }

    void setPainXml(String painXml) {
        setLowlevelParam(getName() + ".sepapain", painXml);
    }

    /**
     * @see AbstractHBCIJob#addConstraint(java.lang.String, java.lang.String, java.lang.String)
     * Ueberschrieben, um die Default-Werte der SEPA-Parameter vorher rauszufischen und in "this.painParams" zu
     * speichern. Die brauchen wir "createPainXml" beim Erstellen des XML - sie wuerden dort sonst aber
     * fehlen, weil Default-Werte eigentlich erst in "verifyConstraints" uebernommen werden.
     */
    @Override
    protected void addConstraint(String frontendName, String destinationName, String defValue) {
        super.addConstraint(frontendName, destinationName, defValue);

        if (destinationName.startsWith("sepa.") && defValue != null) {
            this.painParams.put(frontendName, defValue);
        }
    }

    /**
     * Bei SEPA Geschäftsvorfällen müssen wir verifyConstraints überschreiben um
     * die SEPA XML zu generieren
     */
    @Override
    public void verifyConstraints() {
        // creating SEPA document and storing it in _sepapain
        if (this.acceptsParam("_sepapain")) {
            createPainXml();
        }

        super.verifyConstraints();

        // TODO: checkIBANCRC
    }

    private void setSEPAParam(String name, String value) {
        this.painParams.put(name, value);
    }

    /**
     * Liest den Parameter zu einem gegeben Key aus dem speziellen SEPA
     * Parametern aus
     *
     * @param name
     * @return Value
     */
    private String getPainParam(String name) {
        return this.painParams.get(name);
    }

    /**
     * Referenzierter painVersion-Jobname. Bei vielen Geschäftsvorfällen
     * (z.B. Daueraufträgen) wird die painVersion der Einzeltransaktion verwendet.
     *
     * @return Value
     */
    public abstract String getPainJobName();
}
