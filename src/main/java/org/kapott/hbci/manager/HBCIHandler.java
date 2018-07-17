
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

import org.kapott.hbci.GV.GVTemplate;
import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV.HBCIJobImpl;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidArgumentException;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIDialogStatus;
import org.kapott.hbci.status.HBCIExecStatus;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.kapott.hbci.manager.HBCIJobFactory.newJob;

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

    private HBCIDialog dialog;

    /**
     * Anlegen eines neuen HBCI-Handler-Objektes. Beim Anlegen wird
     * überprüft, ob für die angegebene HBCI-Version eine entsprechende
     * Spezifikation verfügbar ist. Außerdem wird das übergebene
     * Passport überprüft. Dabei werden - falls nicht vorhanden - die BPD und die UPD
     * vom Kreditinstitut geholt. Bei Passports, die asymmetrische Verschlüsselungsverfahren
     * benutzen (RDH), wird zusätzlich überprüft, ob alle benötigten Schlüssel vorhanden
     * sind. Gegebenenfalls werden diese aktualisiert.
     */
    public HBCIHandler(HBCIDialog hbciDialog) {
        try {
            this.dialog = hbciDialog;

            registerInstitute();
            registerUser();
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_CANT_CREATE_HANDLE"), e);
        }

        // wenn in den UPD noch keine SEPA- und TAN-Medien-Informationen ueber die Konten enthalten
        // sind, versuchen wir, diese zu holen
        Properties upd = hbciDialog.getPassport().getUPD();
        if (upd != null && !upd.containsKey("_fetchedMetaInfo")) {
            // wir haben UPD, in denen aber nicht "_fetchedMetaInfo" drinsteht
            updateMetaInfo();
        }
    }

    public Object getProperty(String key) {
        return dialog.getPassport().getProperties().get(key);
    }

    /**
     * Ruft die SEPA-Infos der Konten sowie die TAN-Medienbezeichnungen ab.
     * unterstuetzt wird und speichert diese Infos in den UPD.
     */
    public void updateMetaInfo() {
        Properties bpd = dialog.getPassport().getBPD();
        if (bpd == null) {
            HBCIUtils.log("have no bpd, skip fetching of meta info", HBCIUtils.LOG_WARN);
            return;
        }

        try {
            final Properties lowlevel = getPassport().getSupportedLowlevelJobs(getMsgGen());

            // SEPA-Infos abrufen
            if (lowlevel.getProperty("SEPAInfo") != null) {
                HBCIUtils.log("fetching SEPA information", HBCIUtils.LOG_INFO);
                HBCIJob sepainfo = newJob("SEPAInfo", getPassport(), getMsgGen());
                addJobToDialog(sepainfo);
            }

            // TAN-Medien abrufen
            if (lowlevel.getProperty("TANMediaList") != null) {
                HBCIUtils.log("fetching TAN media list", HBCIUtils.LOG_INFO);
                HBCIJob tanMedia = newJob("TANMediaList", getPassport(), getMsgGen());
                addJobToDialog(tanMedia);
            }

            HBCIExecStatus status = this.execute(false);
            if (status.isOK()) {
                HBCIUtils.log("successfully fetched meta info", HBCIUtils.LOG_INFO);
                dialog.getPassport().getUPD().setProperty("_fetchedMetaInfo", new Date().toString());
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
        Properties bpd = dialog.getPassport().getBPD();
        if (bpd == null) {
            HBCIUtils.log("have no bpd, skipping SEPA information fetching", HBCIUtils.LOG_WARN);
            return;
        }

        // jetzt noch zusaetzliche die SEPA-Informationen abholen
        try {
            if (getPassport().getSupportedLowlevelJobs(getMsgGen()).getProperty("SEPAInfo") != null) {
                HBCIUtils.log("trying to fetch SEPA information from institute", HBCIUtils.LOG_INFO);

                // HKSPA wird unterstuetzt
                HBCIJob sepainfo = newJob("SEPAInfo", getPassport(), getMsgGen());
                addJobToDialog(sepainfo);
                HBCIExecStatus status = execute(false);
                if (status.isOK()) {
                    HBCIUtils.log("successfully fetched information about SEPA accounts from institute", HBCIUtils.LOG_INFO);

                    dialog.getPassport().getUPD().setProperty("_fetchedSEPA", "1");
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
            HBCIInstitute inst = new HBCIInstitute(dialog.getKernel(), dialog.getPassport());
            inst.register();
        } catch (Exception ex) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_CANT_REG_INST"), ex);
        }
    }

    private void registerUser() {
        try {
            HBCIUtils.log("registering user", HBCIUtils.LOG_DEBUG);
            HBCIUser user = new HBCIUser(dialog.getKernel(), dialog.getPassport());
            user.register();
        } catch (Exception ex) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_CANT_REG_USER"), ex);
        }
    }

    public HBCIDialog getDialog() {
        return dialog;
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
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        HBCIJobImpl ret = new GVTemplate(gvname, getPassport(), getMsgGen());
        return ret;
    }

    public void addJobToDialog(HBCIJob job) {
        try {
            dialog.addTask((HBCIJobImpl) job);
        } finally {
            // wenn beim hinzufügen des jobs ein fehler auftrat, und wenn der
            // entsprechende dialog extra für diesen fehlerhaften job erzeugt
            // wurde, dann kann der (leere) dialog auch wieder aus der liste
            // auszuführender dialoge entfernt werden

            if (dialog != null) {
                if (dialog.getAllTasks().size() == 0) {
                    HBCIUtils.log("removing empty dialog for customerid " + getPassport().getCustomerId() + " from list of dialogs", HBCIUtils.LOG_DEBUG);
                    dialog = null;
                }
            }
        }
    }

    /**
     * <p>Ausführen aller bisher erzeugten Aufträge. Diese Methode veranlasst den HBCI-Kernel,
     * die Aufträge, die durch die Aufrufe auszuführen. </p>
     * <p>Beim Hinzufügen der Aufträge zur Auftragsqueue wird implizit oder explizit
     * eine Kunden-ID mit angegeben, unter der der jeweilige Auftrag ausgeführt werden soll.
     * In den meisten Fällen hat ein Benutzer nur eine einzige Kunden-ID, so dass die
     * Angabe entfallen kann, es wird dann automatisch die richtige verwendet. Werden aber
     * mehrere Aufträge via <code>addToQueue()</code> zur Auftragsqueue hinzugefügt, und sind
     * diese Aufträge unter teilweise unterschiedlichen Kunden-IDs auszuführen, dann wird
     * für jede verwendete Kunden-ID ein separater HBCI-Dialog erzeugt und ausgeführt.
     * Das äußert sich dann also darin, dass beim Aufrufen der Methode execute
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
    public HBCIExecStatus execute(boolean closeDialog) {
        String origCustomerId = dialog.getPassport().getCustomerId();
        try {
            HBCIExecStatus ret = new HBCIExecStatus();

            HBCIUtils.log("executing dialog", HBCIUtils.LOG_DEBUG);

            try {
                HBCIDialogStatus dialogStatus = dialog.doIt(closeDialog);
                ret.addDialogStatus(dialogStatus);
            } catch (Exception e) {
                ret.addException(e);
            }
            return ret;
        } finally {
            dialog.getPassport().setCustomerId(origCustomerId);
        }
    }

    /**
     * Gibt das Passport zurück, welches in diesem Handle benutzt wird.
     *
     * @return Passport-Objekt, mit dem dieses Handle erzeugt wurde
     */
    public HBCIPassportInternal getPassport() {
        return dialog.getPassport();
    }

    /**
     * Gibt das HBCI-Kernel-Objekt zurück, welches von diesem HBCI-Handler
     * benutzt wird. Das HBCI-Kernel-Objekt kann u.a. benutzt werden, um
     * alle für die aktuellen HBCI-Version
     * implementierten Geschäftsvorfälle abzufragen.
     *
     * @return HBCI-Kernel-Objekt, mit dem der HBCI-Handler arbeitet
     */
    public HBCIKernel getKernel() {
        return dialog.getKernel();
    }

    public MsgGen getMsgGen() {
        return dialog.getKernel().getMsgGen();
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
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getPassport().getSupportedLowlevelJobs(getMsgGen()).getProperty(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return dialog.getKernel().getLowlevelJobParameterNames(gvname, version);
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
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getPassport().getSupportedLowlevelJobs(getMsgGen()).getProperty(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return dialog.getKernel().getLowlevelJobResultNames(gvname, version);
    }

    public void status(int statusMsgSend, Object o) {
        dialog.getPassport().getCallback().status(statusMsgSend, o);
    }

    public void callback(int closeConnection, String callb_close_conn, int typeNone, StringBuffer stringBuffer) {
        dialog.getPassport().getCallback().callback(closeConnection, callb_close_conn, typeNone, stringBuffer);
    }
}
