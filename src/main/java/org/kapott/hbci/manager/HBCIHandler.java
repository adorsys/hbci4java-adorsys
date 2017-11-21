
/*  $Id: HBCIHandler.java,v 1.2 2011/08/31 14:05:21 willuhn Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2008  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    HBCI4Java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.manager;

import java.lang.reflect.Constructor;
import java.security.KeyPair;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.kapott.hbci.GV.GVTemplate;
import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV.HBCIJobImpl;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidArgumentException;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.passport.AbstractPinTanPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIDialogStatus;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.status.HBCIExecThreadedStatus;

/**
 * <p>Ein Handle für genau einen HBCI-Zugang. Diese Klasse stellt das Verbindungsglied
 * zwischen der Anwendung und dem HBCI-Kernel dar. Für jeden HBCI-Zugang, den
 * die Anwendung benutzt, muss ein entsprechender HBCI-Handler angelegt werden.
 * Darin sind folgende Daten zusammengefasst:</p>
 * <ul>
 * <li>Ein {@link org.kapott.hbci.passport.HBCIPassport}, welches die Nutzeridentifikationsdaten
 * sowie die Zugangsdaten zum entsprechenden HBCI-Server enthält</li>
 * <li>Die zu benutzende HBCI-Versionsnummer</li>
 * <li>interne Daten zur Verwaltung der Dialoge bei der Kommunikation
 * mit dem HBCI-Server</li>
 * </ul>
 * <p>Alle Anfragen der Anwendung an den HBCI-Kernel laufen über einen solchen
 * Handler, womit gleichzeit eindeutig festgelegt ist, welche HBCI-Verbindung
 * diese Anfrage betrifft.</p>
 * <p>Die prinzipielle Benutzung eines Handlers sieht in etwa wiefolgt aus:
 * <pre>
 * // ...
 * HBCIPassport passport=AbstractHBCIPassport.getInstance();
 * HBCIHandler handle=new HBCIHandler(passport.getHBCIVersion(),passport);
 *
 * HBCIJob jobSaldo=handle.newJob("SaldoReq");       // nächster Auftrag ist Saldenabfrage
 * jobSaldo.setParam("number","1234567890");         // Kontonummer für Saldenabfrage
 * jobSaldo.addToQueue();
 *
 * HBCIJob jobUeb=handle.newJob("Ueb");
 * jobUeb.setParam("src.number","1234567890");
 * jobUeb.setParam("dst.number","9876543210");
 * // ...
 * jobUeb.addToQueue();
 *
 * // ...
 *
 * HBCIExecStatus status=handle.execute();
 *
 * // Auswerten von status
 * // Auswerten der einzelnen job-Ergebnisse
 *
 * handle.close();
 * </pre>
 */
public final class HBCIHandler implements IHandlerData {

    public final static int REFRESH_BPD = 1;
    public final static int REFRESH_UPD = 2;

    private HBCIKernelImpl kernel;
    private HBCIPassportInternal passport;
    private HBCIDialog dialog;
    private boolean closeDialog;

    public HBCIHandler(HBCIPassport passport) {
        this(passport, null, true, true);
    }

    /**
     * Anlegen eines neuen HBCI-Handler-Objektes. Beim Anlegen wird
     * überprüft, ob für die angegebene HBCI-Version eine entsprechende
     * Spezifikation verfügbar ist. Außerdem wird das übergebene
     * Passport überprüft. Dabei werden - falls nicht vorhanden - die BPD und die UPD
     * vom Kreditinstitut geholt. Bei Passports, die asymmetrische Verschlüsselungsverfahren
     * benutzen (RDH), wird zusätzlich überprüft, ob alle benötigten Schlüssel vorhanden
     * sind. Gegebenenfalls werden diese aktualisiert.
     *
     * @param passport    das zu benutzende Passport. Dieses muss vorher mit
     *                    {@link org.kapott.hbci.passport.AbstractHBCIPassport#}
     *                    erzeugt worden sein
     * @param init        initialisiert inistitut und user
     */
    public HBCIHandler(HBCIPassport passport, HBCIDialog hbciDialog, boolean init, boolean closeDialog) {
        try {
            if (passport == null)
                throw new InvalidArgumentException(HBCIUtilsInternal.getLocMsg("EXCMSG_PASSPORT_NULL"));

            this.dialog = hbciDialog;
            this.closeDialog = closeDialog;
            this.kernel = new HBCIKernelImpl(this, passport.getHBCIVersion());
            this.passport = (HBCIPassportInternal) passport;
            this.passport.setParentHandlerData(this);

            if (hbciDialog != null) {
                hbciDialog.setKernel(this.kernel);
            }

            if (init) {
                registerInstitute();
                registerUser();
            }
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtilsInternal.getLocMsg("EXCMSG_CANT_CREATE_HANDLE"), e);
        }

        // wenn in den UPD noch keine SEPA- und TAN-Medien-Informationen ueber die Konten enthalten
        // sind, versuchen wir, diese zu holen
        Properties upd = passport.getUPD();
        if (upd != null && !upd.containsKey("_fetchedMetaInfo")) {
            // wir haben UPD, in denen aber nicht "_fetchedMetaInfo" drinsteht
            updateMetaInfo();
        }
    }

    public Object getProperty(String key) {
        return passport.getProperties().get(key);
    }

    /**
     * Ruft die SEPA-Infos der Konten sowie die TAN-Medienbezeichnungen ab.
     * unterstuetzt wird und speichert diese Infos in den UPD.
     */
    public void updateMetaInfo() {
        Properties bpd = passport.getBPD();
        if (bpd == null) {
            HBCIUtils.log("have no bpd, skip fetching of meta info", HBCIUtils.LOG_WARN);
            return;
        }

        try {
            final Properties lowlevel = this.getSupportedLowlevelJobs();

            // SEPA-Infos abrufen
            if (lowlevel.getProperty("SEPAInfo") != null) {
                HBCIUtils.log("fetching SEPA information", HBCIUtils.LOG_INFO);
                HBCIJob sepainfo = this.newJob("SEPAInfo");
                sepainfo.addToQueue();
            }

            // TAN-Medien abrufen - aber nur bei PIN/TAN-Verfahren
            if (lowlevel.getProperty("TANMediaList") != null && (this.passport instanceof AbstractPinTanPassport)) {
                HBCIUtils.log("fetching TAN media list", HBCIUtils.LOG_INFO);
                HBCIJob tanMedia = this.newJob("TANMediaList");
                tanMedia.addToQueue();
            }

            HBCIExecStatus status = this.execute();
            if (status.isOK()) {
                HBCIUtils.log("successfully fetched meta info", HBCIUtils.LOG_INFO);
                passport.getUPD().setProperty("_fetchedMetaInfo", new Date().toString());
                passport.saveChanges();
            } else {
                HBCIUtils.log("error while fetching meta info: " + status.toString(), HBCIUtils.LOG_ERR);
            }
        } catch (Exception e) {
            // Wir werfen das nicht als Exception. Unschoen, wenn das nicht klappt.
            // Aber kein Grund zur Panik.
            HBCIUtils.log(e);
        }
    }


    /**
     * Wenn der GV SEPAInfo unterstützt wird, heißt das, dass die Bank mit
     * SEPA-Konten umgehen kann. In diesem Fall holen wir die SEPA-Informationen
     * über die Konten von der Bank ab - für jedes SEPA-fähige Konto werden u.a.
     * BIC/IBAN geliefert
     *
     * @deprecated Bitte <code>updateMetaInfo</code> verwenden. Das aktualisiert auch die TAN-Medien.
     */
    public void updateSEPAInfo() {
        Properties bpd = passport.getBPD();
        if (bpd == null) {
            HBCIUtils.log("have no bpd, skipping SEPA information fetching", HBCIUtils.LOG_WARN);
            return;
        }

        // jetzt noch zusaetzliche die SEPA-Informationen abholen
        try {
            if (getSupportedLowlevelJobs().getProperty("SEPAInfo") != null) {
                HBCIUtils.log("trying to fetch SEPA information from institute", HBCIUtils.LOG_INFO);

                // HKSPA wird unterstuetzt
                HBCIJob sepainfo = newJob("SEPAInfo");
                sepainfo.addToQueue();
                HBCIExecStatus status = execute();
                if (status.isOK()) {
                    HBCIUtils.log("successfully fetched information about SEPA accounts from institute", HBCIUtils.LOG_INFO);

                    passport.getUPD().setProperty("_fetchedSEPA", "1");
                    passport.saveChanges();
                } else {
                    HBCIUtils.log("error while fetching information about SEPA accounts from institute:", HBCIUtils.LOG_ERR);
                    HBCIUtils.log(status.toString(), HBCIUtils.LOG_ERR);
                }
                /* beim execute() werden die Job-Result-Objekte automatisch
                 * gefuellt. Der GV-Klasse fuer SEPAInfo haengt sich in diese
        		 * Logik rein, um gleich die UPD mit den SEPA-Konto-Daten
        		 * zu aktualisieren, so dass an dieser Stelle die UPD um
        		 * die SEPA-Informationen erweitert wurden. 
        		 */
            } else {
                HBCIUtils.log("institute does not support SEPA accounts, so we skip fetching information about SEPA", HBCIUtils.LOG_DEBUG);
            }
        } catch (HBCI_Exception he) {
            throw he;
        } catch (Exception e) {
            throw new HBCI_Exception(e);
        }
    }

    private void registerInstitute() {
        try {
            HBCIUtils.log("registering institute", HBCIUtils.LOG_DEBUG);
            HBCIInstitute inst = new HBCIInstitute(kernel, passport, false);
            inst.register();
        } catch (Exception ex) {
            throw new HBCI_Exception(HBCIUtilsInternal.getLocMsg("EXCMSG_CANT_REG_INST"), ex);
        }
    }

    private void registerUser() {
        try {
            HBCIUtils.log("registering user", HBCIUtils.LOG_DEBUG);
            HBCIUser user = new HBCIUser(kernel, passport, false);
            user.register();
        } catch (Exception ex) {
            throw new HBCI_Exception(HBCIUtilsInternal.getLocMsg("EXCMSG_CANT_REG_USER"), ex);
        }
    }

    /**
     * <p>Schließen des Handlers. Diese Methode sollte immer dann aufgerufen werden,
     * wenn die entsprechende HBCI-Verbindung nicht mehr benötigt wird. </p><p>
     * Beim Schließen des Handlers wird das Passport ebenfalls geschlossen.
     * Sowohl das Passport-Objekt als auch das Handler-Objekt können anschließend
     * nicht mehr benutzt werden.</p>
     */
    public void close() {
        if (passport != null) {
            try {
                passport.close();
            } catch (Exception e) {
                HBCIUtils.log(e);
            }
        }

        passport = null;
        kernel = null;
        dialog = null;
    }

    /* gibt die zu verwendende Customer-Id zurück. Wenn keine angegeben wurde
     * (customerId==null), dann wird die derzeitige passport-customerid 
     * verwendet */
    private String fixUnspecifiedCustomerId(String customerId) {
        if (customerId == null) {
            customerId = passport.getCustomerId();
            HBCIUtils.log("using default customerid " + customerId, HBCIUtils.LOG_DEBUG);
        }
        return customerId;
    }

    public HBCIDialog getDialog() {
        if (dialog == null) {
            HBCIUtils.log("have to create new dialog for customerid", HBCIUtils.LOG_DEBUG);
            dialog = new HBCIDialog((HBCIPassportInternal) this.getPassport(), (HBCIKernelImpl) this.getKernel());
        }

        return dialog;
    }

    /**
     * <p>Beginn einer neuen HBCI-Nachricht innerhalb eines Dialoges festlegen.
     * Normalerweise muss diese Methode niemals manuell aufgerufen zu werden!</p>
     * <p>Mit dieser Methode wird der HBCI-Kernel gezwungen, eine neue HBCI-Nachricht
     * anzulegen, in die alle nachfolgenden Geschäftsvorfälle aufgenommen werden.
     * Die <code>customerId</code> legt fest, für welchen Dialog die neue Nachricht
     * erzeugt werden soll. Für eine genauere Beschreibung von Dialogen und
     * <code>customerid</code>s siehe {@link org.kapott.hbci.GV.HBCIJob#addToQueue(String)}. </p>
     */
    public void newMsg() {
        HBCIUtils.log("have to create new message for dialog for customer", HBCIUtils.LOG_DEBUG);
        getDialog().newMsg();
    }

    /**
     * <p>Erzeugen eines neuen Highlevel-HBCI-Jobs. Diese Methode gibt ein neues Job-Objekt zurück. Dieses
     * Objekt wird allerdings noch <em>nicht</em> zum HBCI-Dialog hinzugefügt. Statt dessen
     * müssen erst alle zur Beschreibung des jeweiligen Jobs benötigten Parameter mit
     * {@link org.kapott.hbci.GV.HBCIJob#setParam(String, String)} gesetzt werden.
     * Anschließend kann der Job mit {@link org.kapott.hbci.GV.HBCIJob#addToQueue(String)} zum
     * HBCI-Dialog hinzugefügt werden.</p>
     * <p>Eine Beschreibung aller unterstützten Geschäftsvorfälle befindet sich
     * im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @param jobname der Name des Jobs, der erzeugt werden soll. Gültige
     *                Job-Namen sowie die benötigten Parameter sind in der Beschreibung des Packages
     *                <code>org.kapott.hbci.GV</code> zu finden.
     * @return ein Job-Objekt, für das die entsprechenden Job-Parameter gesetzt werden müssen und
     * welches anschließend zum HBCI-Dialog hinzugefügt werden kann.
     */
    public HBCIJobImpl newJob(String jobname) {
        HBCIUtils.log("creating new job " + jobname, HBCIUtils.LOG_DEBUG);

        if (jobname == null || jobname.length() == 0)
            throw new InvalidArgumentException(HBCIUtilsInternal.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        HBCIJobImpl ret = null;
        String className = "org.kapott.hbci.GV.GV" + jobname;

        try {
            Class cl = Class.forName(className);
            Constructor cons = cl.getConstructor(new Class[]{HBCIHandler.class});
            ret = (HBCIJobImpl) cons.newInstance(new Object[]{this});
        } catch (ClassNotFoundException e) {
            throw new InvalidUserDataException("*** there is no highlevel job named " + jobname + " - need class " + className);
        } catch (Exception e) {
            String msg = HBCIUtilsInternal.getLocMsg("EXCMSG_JOB_CREATE_ERR", jobname);
            if (!HBCIUtilsInternal.ignoreError(null, "client.errors.ignoreCreateJobErrors", msg))
                throw new HBCI_Exception(msg, e);
        }

        return ret;
    }

    /**
     * Erzeugt ein neues Lowlevel-Job-Objekt. Für eine Beschreibung des Unterschiedes
     * zwischen High- und Lowlevel-Definition von Jobs siehe Package <code>org.kapott.hbci.GV</code>.
     *
     * @param gvname der Lowlevel-Name des zu erzeugenden Jobs
     * @return ein neues Job-Objekt, für das erst alle benötigten Lowlevel-Parameter gesetzt
     * werden müssen und das anschließend zum HBCI-Dialog hinzugefügt werden kann
     */
    public HBCIJob newLowlevelJob(String gvname) {
        HBCIUtils.log("generating new lowlevel-job " + gvname, HBCIUtils.LOG_DEBUG);

        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtilsInternal.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        HBCIJobImpl ret = new GVTemplate(gvname, this);
        return ret;
    }

    /**
     * Do NOT use! Use {@link org.kapott.hbci.GV.HBCIJob#addToQueue(String)} instead
     */
    public void addJobToDialog(String customerId, HBCIJob job) {
        // TODO: nach dem neuen Objekt-Graph kennt der HBCIJob bereits "seinen"
        // HBCIHandler, so dass ein HBCIHandler.addJob(job) eigentlich
        // redundant ist und durch HBCIJob.addToQueue() ersetzt werden
        // könnte. Deswegen muss es hier einen Überprüfung geben, ob
        // (job.getHBCIHandler() === this) ist.

        customerId = fixUnspecifiedCustomerId(customerId);

        HBCIDialog dialog = null;
        try {
            dialog = getDialog();
            dialog.addTask((HBCIJobImpl) job);
        } finally {
            // wenn beim hinzufügen des jobs ein fehler auftrat, und wenn der
            // entsprechende dialog extra für diesen fehlerhaften job erzeugt
            // wurde, dann kann der (leere) dialog auch wieder aus der liste
            // auszuführender dialoge entfernt werden

            if (dialog != null) {
                if (dialog.getAllTasks().size() == 0) {
                    HBCIUtils.log("removing empty dialog for customerid " + customerId + " from list of dialogs", HBCIUtils.LOG_DEBUG);
                    dialog = null;
                }
            }
        }
    }

    /**
     * @deprecated use {@link org.kapott.hbci.GV.HBCIJob#addToQueue(String) HBCIJob.addToQueue(String)} instead
     */
    public void addJob(String customerId, HBCIJob job) {
        addJobToDialog(customerId, job);
    }

    /**
     * @deprecated use {@link org.kapott.hbci.GV.HBCIJob#addToQueue() HBCIJob.addToQueue()} instead
     */
    public void addJob(HBCIJob job) {
        addJob(null, job);
    }

    /**
     * Erzeugen eines leeren HBCI-Dialoges. <p>Im Normalfall werden HBCI-Dialoge
     * automatisch erzeugt, wenn Geschäftsvorfälle mit der Methode {@link org.kapott.hbci.GV.HBCIJob#addToQueue(String)}
     * zur Liste der auszuführenden Jobs hinzugefügt werden. <code>createEmptyDialog()</code>
     * kann explizit aufgerufen werden, wenn ein Dialog erzeugt werden soll,
     * der keine Geschäftsvorfälle enthält, also nur aus Dialog-Initialisierung
     * und Dialog-Ende besteht.</p>
     * <p>Ist die angegebene <code>customerId=null</code>, so wird der Dialog
     * für die aktuell im Passport gespeicherte Customer-ID erzeugt.</p>
     */
    public void createEmptyDialog() {
        HBCIUtils.log("creating empty dialog", HBCIUtils.LOG_DEBUG);
        getDialog();
    }

    /**
     * <p>Ausführen aller bisher erzeugten Aufträge. Diese Methode veranlasst den HBCI-Kernel,
     * die Aufträge, die durch die Aufrufe der Methode
     * {@link org.kapott.hbci.GV.HBCIJob#addToQueue(String)}
     * zur Auftragsliste hinzugefügt wurden, auszuführen. </p>
     * <p>Beim Hinzufügen der Aufträge zur Auftragsqueue (mit {@link org.kapott.hbci.GV.HBCIJob#addToQueue()}
     * oder {@link org.kapott.hbci.GV.HBCIJob#addToQueue(String)}) wird implizit oder explizit
     * eine Kunden-ID mit angegeben, unter der der jeweilige Auftrag ausgeführt werden soll.
     * In den meisten Fällen hat ein Benutzer nur eine einzige Kunden-ID, so dass die
     * Angabe entfallen kann, es wird dann automatisch die richtige verwendet. Werden aber
     * mehrere Aufträge via <code>addToQueue()</code> zur Auftragsqueue hinzugefügt, und sind
     * diese Aufträge unter teilweise unterschiedlichen Kunden-IDs auszuführen, dann wird
     * für jede verwendete Kunden-ID ein separater HBCI-Dialog erzeugt und ausgeführt.
     * Das äußert sich dann also darin, dass beim Aufrufen der Methode {@link #execute()}
     * u.U. mehrere HBCI-Dialog mit der Bank geführt werden, und zwar je einer für jede Kunden-ID,
     * für die wenigstens ein Auftrag existiert. Innerhalb eines HBCI-Dialoges werden alle
     * auszuführenden Aufträge in möglichst wenige HBCI-Nachrichten verpackt.</p>
     * <p>Dazu wird eine Reihe von HBCI-Nachrichten mit dem HBCI-Server der Bank ausgetauscht. Die
     * Menge der dazu verwendeten HBCI-Nachrichten kann dabei nur bedingt beeinflusst werden, da <em>HBCI4Java</em>
     * u.U. selbstständig Nachrichten erzeugt, u.a. wenn ein Auftrag nicht mehr mit in eine Nachricht
     * aufgenommen werden konnte, oder wenn eine Antwortnachricht nicht alle verfügbaren Daten
     * zurückgegeben hat, so dass <em>HBCI4Java</em> mit einer oder mehreren weiteren Nachrichten den Rest
     * der Daten abholt. </p>
     * <p>Nach dem Nachrichtenaustausch wird ein Status-Objekt zurückgegeben,
     * welches zur Auswertung aller ausgeführten Dialoge benutzt werden kann.</p>
     *
     * @return ein Status-Objekt, anhand dessen der Erfolg oder das Fehlschlagen
     * der Dialoge festgestellt werden kann.
     */
    public HBCIExecStatus execute() {
        String origCustomerId = passport.getCustomerId();
        try {
            HBCIExecStatus ret = new HBCIExecStatus();

            HBCIUtils.log("executing dialog", HBCIUtils.LOG_DEBUG);

            try {
                HBCIDialog dialog = getDialog();
                HBCIDialogStatus dialogStatus = dialog.doIt(closeDialog);
                ret.addDialogStatus(dialogStatus);
            } catch (Exception e) {
                ret.addException(e);
            } finally {
                if (closeDialog) {
                    dialog = null;
                }
            }

            return ret;
        } finally {
            if (closeDialog) {
                reset();
            }
            passport.setCustomerId(origCustomerId);
            try {
                passport.closeComm();
            } catch (Exception e) {
                HBCIUtils.log("nested exception while closing passport: ", HBCIUtils.LOG_ERR);
                HBCIUtils.log(e);
            }
        }
    }

    /**
     * Zurücksetzen des Handlers auf den Ausgangszustand. Diese Methode kann
     * aufgerufen werden, wenn alle bisher hinzugefügten Nachrichten und
     * Aufträge wieder entfernt werden sollen. Nach dem Ausführen eines
     * Dialoges mit {@link #execute()} wird diese Methode
     * automatisch aufgerufen.
     */
    public void reset() {
        dialog = null;
    }

    /**
     * Gibt das Passport zurück, welches in diesem Handle benutzt wird.
     *
     * @return Passport-Objekt, mit dem dieses Handle erzeugt wurde
     */
    public HBCIPassport getPassport() {
        return passport;
    }

    /**
     * Gibt das HBCI-Kernel-Objekt zurück, welches von diesem HBCI-Handler
     * benutzt wird. Das HBCI-Kernel-Objekt kann u.a. benutzt werden, um
     * alle für die aktuellen HBCI-Version (siehe {@link #getHBCIVersion()})
     * implementierten Geschäftsvorfälle abzufragen.
     *
     * @return HBCI-Kernel-Objekt, mit dem der HBCI-Handler arbeitet
     */
    public HBCIKernel getKernel() {
        return kernel;
    }

    public MsgGen getMsgGen() {
        return kernel.getMsgGen();
    }

    /**
     * Gibt die HBCI-Versionsnummer zurück, für die der aktuelle HBCIHandler
     * konfiguriert ist.
     *
     * @return HBCI-Versionsnummer, mit welcher dieses Handler-Objekt arbeitet
     */
    public String getHBCIVersion() {
        return kernel.getHBCIVersion();
    }

    /**
     * <p>Gibt die Namen aller vom aktuellen HBCI-Zugang (d.h. Passport)
     * unterstützten Lowlevel-Jobs zurück. Alle hier zurückgegebenen Job-Namen
     * können als Argument beim Aufruf der Methode
     * {@link #newLowlevelJob(String)} benutzt werden.</p>
     * <p>In dem zurückgegebenen Properties-Objekt enthält jeder Eintrag als
     * Key den Lowlevel-Job-Namen; als Value wird die Versionsnummer des
     * jeweiligen Geschäftsvorfalls angegeben, die von <em>HBCI4Java</em> mit dem
     * aktuellen Passport und der aktuell eingestellten HBCI-Version
     * benutzt werden wird.</p>
     * <p><em>(Prinzipiell unterstützt <em>HBCI4Java</em> für jeden
     * Geschäftsvorfall mehrere GV-Versionen. Auch eine Bank bietet i.d.R. für
     * jeden GV mehrere Versionen an. Wird mit <em>HBCI4Java</em> ein HBCI-Job
     * erzeugt, so verwendet <em>HBCI4Java</em> immer automatisch die höchste
     * von der Bank unterstützte GV-Versionsnummer. Diese Information ist
     * für den Anwendungsentwickler kaum von Bedeutung und dient hauptsächlich
     * zu Debugging-Zwecken.)</em></p>
     * <p>Zum Unterschied zwischen High- und Lowlevel-Jobs siehe die
     * Beschreibung im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @return Sammlung aller vom aktuellen Passport unterstützten HBCI-
     * Geschäftsvorfallnamen (Lowlevel) mit der jeweils von <em>HBCI4Java</em>
     * verwendeten GV-Versionsnummer.
     */
    public Properties getSupportedLowlevelJobs() {
        Hashtable<String, List<String>> allValidJobNames = kernel.getAllLowlevelJobs();
        Properties paramSegments = passport.getParamSegmentNames();
        Properties result = new Properties();

        for (Enumeration e = paramSegments.propertyNames(); e.hasMoreElements(); ) {
            String segName = (String) e.nextElement();

            // überprüfen, ob parameter-segment tatsächlich zu einem GV gehört
            // gilt z.b. für "PinTan" nicht
            if (allValidJobNames.containsKey(segName))
                result.put(segName, paramSegments.getProperty(segName));
        }

        return result;
    }

    /**
     * <p>Gibt alle Parameter zurück, die für einen Lowlevel-Job gesetzt
     * werden können. Wird ein Job mit {@link #newLowlevelJob(String)}
     * erzeugt, so kann der gleiche <code>gvname</code> als Argument dieser
     * Methode verwendet werden, um eine Liste aller Parameter zu erhalten, die
     * für diesen Job durch Aufrufe der Methode
     * {@link org.kapott.hbci.GV.HBCIJob#setParam(String, String)}
     * gesetzt werden können bzw. müssen.</p>
     * <p>Aus der zurückgegebenen Liste ist nicht ersichtlich, ob ein bestimmter
     * Parameter optional ist oder gesetzt werden <em>muss</em>. Das kann aber
     * durch Benutzen des Tools {@link org.kapott.hbci.tools.ShowLowlevelGVs}
     * ermittelt werden.</p>
     * <p>Jeder Eintrag der zurückgegebenen Liste enthält einen String, welcher als
     * erster Parameter für den Aufruf von <code>HBCIJob.setParam()</code> benutzt
     * werden kann - vorausgesetzt, der entsprechende Job wurde mit
     * {@link #newLowlevelJob(String)} erzeugt. </p>
     * <p>Diese Methode verwendet intern die Methode
     * {@link HBCIKernel#getLowlevelJobParameterNames(String, String)}.
     * Unterschied ist, dass diese Methode zum einen überprüft, ob  der
     * angegebene Lowlevel-Job überhaupt vom aktuellen Passport unterstützt wird.
     * Außerdem wird automatisch die richtige Versionsnummer an
     * {@link HBCIKernel#getLowlevelJobParameterNames(String, String)} übergeben
     * (nämlich die Versionsnummer, die <em>HBCI4Java</em> auch beim Anlegen
     * eines Jobs via {@link #newLowlevelJob(String)} verwenden wird).</p>
     * <p>Zur Beschreibung von High- und Lowlevel-Jobs siehe auch die Dokumentation
     * im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @param gvname der Lowlevel-Jobname, für den eine Liste der Job-Parameter
     *               ermittelt werden soll
     * @return eine Liste aller Parameter-Bezeichnungen, die in der Methode
     * {@link org.kapott.hbci.GV.HBCIJob#setParam(String, String)}
     * benutzt werden können
     */
    public List<String> getLowlevelJobParameterNames(String gvname) {
        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtilsInternal.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getSupportedLowlevelJobs().getProperty(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return kernel.getLowlevelJobParameterNames(gvname, version);
    }

    /**
     * <p>Gibt eine Liste mit Strings zurück, welche Bezeichnungen für die einzelnen Rückgabedaten
     * eines Lowlevel-Jobs darstellen. Jedem {@link org.kapott.hbci.GV.HBCIJob} ist ein
     * Result-Objekt zugeordnet, welches die Rückgabedaten und Statusinformationen zu dem jeweiligen
     * Job enthält (kann mit {@link org.kapott.hbci.GV.HBCIJob#getJobResult()}
     * ermittelt werden). Bei den meisten Highlevel-Jobs handelt es sich dabei um bereits aufbereitete
     * Daten (Kontoauszüge werden z.B. nicht in dem ursprünglichen SWIFT-Format zurückgegeben, sondern
     * bereits als fertig geparste Buchungseinträge).</p>
     * <p>Bei Lowlevel-Jobs gibt es diese Aufbereitung der Daten nicht. Statt dessen müssen die Daten
     * manuell aus der Antwortnachricht extrahiert und interpretiert werden. Die einzelnen Datenelemente
     * der Antwortnachricht werden in einem Properties-Objekt bereitgestellt
     * ({@link org.kapott.hbci.GV_Result.HBCIJobResult#getResultData()}). Jeder Eintrag
     * darin enthält den Namen und den Wert eines Datenelementes aus der Antwortnachricht.</p>
     * <p>Die Methode <code>getLowlevelJobResultNames()</code> gibt nun alle gültigen Namen zurück,
     * für welche in dem Result-Objekt Daten gespeichert sein können. Ob für ein Datenelement tatsächlich
     * ein Wert in dem Result-Objekt existiert, wird damit nicht bestimmt, da einzelne Datenelemente
     * optional sind.</p>
     * <p>Diese Methode verwendet intern die Methode
     * {@link HBCIKernel#getLowlevelJobResultNames(String, String)}.
     * Unterschied ist, dass diese Methode zum einen überprüft, ob  der
     * angegebene Lowlevel-Job überhaupt vom aktuellen Passport unterstützt wird.
     * Außerdem wird automatisch die richtige Versionsnummer an
     * {@link HBCIKernel#getLowlevelJobResultNames(String, String)} übergeben
     * (nämlich die Versionsnummer, die <em>HBCI4Java</em> auch beim Anlegen
     * eines Jobs via {@link #newLowlevelJob(String)} verwenden wird).</p>
     * <p>Mit dem Tool {@link org.kapott.hbci.tools.ShowLowlevelGVRs} kann offline eine
     * Liste aller Job-Result-Datenelemente erzeugt werden.</p>
     * <p>Zur Beschreibung von High- und Lowlevel-Jobs siehe auch die Dokumentation
     * im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @param gvname Lowlevelname des Geschäftsvorfalls, für den die Namen der Rückgabedaten benötigt werden.
     * @return Liste aller möglichen Property-Keys, für die im Result-Objekt eines Lowlevel-Jobs
     * Werte vorhanden sein könnten
     */
    public List<String> getLowlevelJobResultNames(String gvname) {
        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtilsInternal.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getSupportedLowlevelJobs().getProperty(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return kernel.getLowlevelJobResultNames(gvname, version);
    }

    /**
     * <p>Gibt für einen Job alle bekannten Einschränkungen zurück, die bei
     * der Ausführung des jeweiligen Jobs zu beachten sind. Diese Daten werden aus den
     * Bankparameterdaten des aktuellen Passports extrahiert. Sie können von einer HBCI-Anwendung
     * benutzt werden, um gleich entsprechende Restriktionen bei der Eingabe von
     * Geschäftsvorfalldaten zu erzwingen (z.B. die maximale Anzahl von Verwendungszweckzeilen,
     * ob das Ändern von terminierten Überweisungen erlaubt ist usw.).</p>
     * <p>Die einzelnen Einträge des zurückgegebenen Properties-Objektes enthalten als Key die
     * Bezeichnung einer Restriktion (z.B. "<code>maxusage</code>"), als Value wird der
     * entsprechende Wert eingestellt. Die Bedeutung der einzelnen Restriktionen ist zur Zeit
     * nur der HBCI-Spezifikation zu entnehmen. In späteren Programmversionen werden entsprechende
     * Dokumentationen zur internen HBCI-Beschreibung hinzugefügt, so dass dafür eine Abfrageschnittstelle
     * implementiert werden kann.</p>
     * <p>I.d.R. werden mehrere Versionen eines Geschäftsvorfalles von der Bank
     * angeboten. Diese Methode ermittelt automatisch die "richtige" Versionsnummer
     * für die Ermittlung der GV-Restriktionen aus den BPD (und zwar die selbe,
     * die <em>HBCI4Java</em> beim Erzeugen eines Jobs benutzt). </p>
     * <p>Siehe dazu auch {@link HBCIJob#getJobRestrictions()}.</p>
     *
     * @param gvname Lowlevel-Name des Geschäftsvorfalles, für den die Restriktionen
     *               ermittelt werden sollen
     * @return Properties-Objekt mit den einzelnen Restriktionen
     */
    public Properties getLowlevelJobRestrictions(String gvname) {
        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtilsInternal.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getSupportedLowlevelJobs().getProperty(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return passport.getJobRestrictions(gvname, version);
    }

    /**
     * <p>Überprüfen, ein bestimmter Highlevel-Job von der Bank angeboten
     * wird. Diese Methode kann benutzt werden, um <em>vor</em> dem Erzeugen eines
     * {@link org.kapott.hbci.GV.HBCIJob}-Objektes zu überprüfen, ob
     * der gewünschte Job überhaupt von der Bank angeboten wird. Ist das
     * nicht der Fall, so würde der Aufruf von
     * {@link org.kapott.hbci.manager.HBCIHandler#newJob(String)}
     * zu einer Exception führen.</p>
     * <p>Eine Liste aller zur Zeit verfügbaren Highlevel-Jobnamen ist in der Paketbeschreibung
     * des Packages <code>org.kapott.hbci.GV</code> zu finden. Wird hier nach einem Highlevel-Jobnamen
     * gefragt, der nicht in dieser Liste enthalten ist, so wird eine Exception geworfen.</p>
     * <p>Mit dieser Methode können nur Highlevel-Jobs überprüft werden. Zum Überprüfen,
     * ob ein bestimmter Lowlevel-Job unterstützt wird, ist die Methode
     * {@link HBCIHandler#getSupportedLowlevelJobs()}
     * zu verwenden.</p>
     *
     * @param jobnameHL der Highlevel-Name des Jobs, dessen Unterstützung überprüft werden soll
     * @return <code>true</code>, wenn dieser Job von der Bank unterstützt wird und
     * mit <em>HBCI4Java</em> verwendet werden kann; ansonsten <code>false</code>
     */
    public boolean isSupported(String jobnameHL) {
        if (jobnameHL == null || jobnameHL.length() == 0)
            throw new InvalidArgumentException(HBCIUtilsInternal.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        try {
            Class cl = Class.forName("org.kapott.hbci.GV.GV" + jobnameHL);
            String lowlevelName = (String) cl.getMethod("getLowlevelName", (Class[]) null).invoke(null, (Object[]) null);
            return getSupportedLowlevelJobs().keySet().contains(lowlevelName);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtilsInternal.getLocMsg("EXCMSG_HANDLER_HLCHECKERR", jobnameHL), e);
        }
    }

    /**
     * Abholen der BPD bzw. UPD erzwingen. Beim Aufruf dieser Methode wird
     * automatisch ein HBCI-Dialog ausgeführt, der je nach Wert von <code>selectX</code>
     * die BPD und/oder UPD erneut abholt. Alle bis zu diesem Zeitpunkt erzeugten
     * ({@link org.kapott.hbci.GV.HBCIJob#addToQueue()}) und noch nicht ausgeführten Jobs werden dabei
     * wieder aus der Job-Schlange entfernt.
     *
     * @param selectX kann aus einer Kombination (Addition) der Werte
     *                {@link #REFRESH_BPD} und {@link #REFRESH_UPD} bestehen
     * @return Status-Objekt, welches Informationen über den ausgeführten
     * HBCI-Dialog enthält
     */
    public HBCIDialogStatus refreshXPD(int selectX) {
        if ((selectX & REFRESH_BPD) != 0) {
            passport.clearBPD();
        }
        if ((selectX & REFRESH_UPD) != 0) {
            passport.clearUPD();
        }

        reset();
        getDialog();
        HBCIDialogStatus result = execute().getDialogStatus();
        return result;
    }

}
