/*  $Id: HBCIPassport.java,v 1.1 2011/05/04 22:37:43 willuhn Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2008  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General License as published by
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

package org.kapott.hbci.passport;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.structures.Konto;

import java.util.HashMap;

/**
 * <p>Public Interface für HBCI-Passports. Ein HBCI-Passport ist eine Art "Ausweis",
 * der individuell für jeden Nutzer eines HBCI-Zugangs und für jeden
 * Zugangsmechanismus ist. Ein Passport repräsentiert ein HBCI-Sicherheitsmedium
 * und stellt Funktionen bereit, um mit dem jeweiligen Medium zu arbeiten.
 * </p><p>
 * Für jede Zugangsart gibt es eine konkrete Passport-Implementation, die dieses
 * Interface implementiert. Dabei handelt es sich um</p>
 * <ul>
 * <li><code>HBCIPassportDDV</code> für Zugang über DDV mit Chipkarte</li>
 * <li><code>HBCIPassportRDHNew</code> für Zugang über RDH mit Datei</li>
 * <li><code>HBCIPassportRDH</code> für Zugang über RDH mit Datei (<b><em>bitte nicht mehr benutzen</em></b>;
 * siehe Datei <code>README.RDHNew</code>)</li>
 * <li><code>HBCIPassportPinTan</code> für Zugang über das PIN/TAN-Verfahren</li>
 * <li><code>HBCIPassportAnonymous</code> für den anonymen Zugang</li>
 * <li><code>HBCIPassportSIZRDHFile</code> für den Zugang über RDH mit Datei,
 * wobei als Datei eine SIZ-Schlüsseldatei, wie sie z.B. von StarMoney oder GENOlite
 * erzeugt wird, verwendet werden kann</li>
 * <li><code>HBCIPassportRDHXFile</code> für den Zugang über RDH mit Datei,
 * wobei als Datei eine RDH-2- oder RDH-10-Schlüsseldatei verwendet wird,
 * wie sie z.B. von VR-NetWorld erzeugt wird.</li>
 * </ul>
 * <p>In einem Passport werden alle nutzer- und institutsspezifischen Daten verwaltet.
 * Dazu gehören</p>
 * <ul>
 * <li>die Zugangsdaten für den HBCI-Server der Bank (IP-Adresse, usw.)</li>
 * <li>die nutzerspezifischen Zugangsdaten (Nutzerkennung, System-Kennung, usw.)</li>
 * <li>die Schlüsselinformationen für die kryptografischen Operationen</li>
 * <li>die gecachten BPD und die UPD</li>
 * </ul>
 * <p>Außerdem sind in einem Passport alle Methoden implementiert, die zur Durchführung
 * der kryptografischen Operationen benötigt werden (verschlüsseln, signieren, usw.)</p>
 */
public interface HBCIPassport {

    /**
     * Gibt die gespeicherten BPD zurück. Die Auswertung der BPD seitens einer HBCI-Anwendung
     * auf direktem Weg wird nicht empfohlen, da es keine Dokumentation über die
     * Namensgebung der einzelnen Einträge gibt.
     *
     * @return die Bankparamterdaten oder <code>null</code>, falls diese nicht im
     * Passport vorhanden sind
     */
    HashMap<String, String> getBPD();

    /**
     * Gibt die HBCI-Version zurück, die zuletzt verwendet wurde. Der hier zurückgegebene
     * Wert ist der selbe, der bei der Initialisierung des
     * {@link org.kapott.hbci.manager.HBCIDialog} verwendet werden kann. Um also
     * einen HBCIHandler zu erzeugen, der mit der HBCI-Version arbeitet, mit der
     * ein Passport-Objekt zuletzt benutzt wurde, so kann das mit
     * <code>new HBCIHandler(passport.getHBCIVersion(),passport)</code> erfolgen (vorausgesetzt,
     * <code>passport.getHBCIVersion()</code> gibt einen nicht-leeren String zurück.
     *
     * @return Die zuletzt verwendete HBCI-Version. Ist diese Information nicht
     * verfügbar, so wird ein leerer String zurückgegeben.
     */
    String getHBCIVersion();

    /**
     * Gibt die gespeicherten UPD (User-Parameter-Daten) zurück. Eine direkte
     * Auswertung des Inhalts dieses Property-Objektes wird nicht empfohlen, da
     * die Benennung der einzelnen Einträge nicht explizit dokumentiert ist.
     *
     * @return die Userparameterdaten oder <code>null</code>, falls diese nicht im
     * Passport vorhanden sind
     */
    HashMap<String, String> getUPD();

    /**
     * <p>Gibt die Bankleitzahl des Kreditinstitutes zurück. Bei Verwendung dieser Methode
     * ist Vorsicht geboten, denn hier ist die Bankleitzahl der Bank gemeint, die
     * den HBCI-Server betreibt. I.d.R. deckt sich diese BLZ zwar mit der BLZ der
     * Konten des Bankkunden, es gibt aber auch Fälle, wo die BLZ, die mit dieser Methode
     * ermittelt wird, anders ist als die BLZ bei den Kontoverbindungen des
     * Kunden.
     * </p><p>
     * Für die Ermittlung der BLZ für die Kontodaten sollte statt dessen die Methode
     * {@link #getAccounts()} benutzt werden.
     * </p>
     *
     * @return die BLZ der Bank
     */
    String getBLZ();

    void setBLZ(String blz);

    /**
     * Gibt den Ländercode der Bank zurück. Für deutsche Banken ist das der String
     * "<code>DE</code>".
     *
     * @return Ländercode der Bank
     */
    String getCountry();

    void setCountry(String country);

    /**
     * Gibt ein Array mit Kontoinformationen zurück. Auf die hier zurückgegebenen Konten kann via
     * HBCI zugegriffen werden. Nicht jede Bank unterstützt diese Abfrage, so dass dieses Array
     * u.U. auch leer sein kann, obwohl natürlich via HBCI auf bestimmte Konten zugegriffen werden
     * kann. In diesem Fall müssen die Kontoinformationen anderweitig ermittelt werden (manuelle
     * Eingabe des Anwenders).
     *
     * @return Array mit Kontoinformationen über verfügbare HBCI-Konten
     */
    Konto[] getAccounts();

    /**
     * Ausfüllen fehlender Kontoinformationen. In der Liste der verfügbaren Konten (siehe
     * {@link #getAccounts()}) wird nach einem Konto gesucht, welches die
     * gleiche Kontonummer hat wie das übergebene Konto <code>account</code>. Wird ein solches
     * Konto gefunden, so werden die Daten dieses gefundenen Kontos in das <code>account</code>-Objekt
     * übertragen.<p/>
     * Diese Methode kann benutzt werden, wenn zu einem Konto nicht alle Daten bekannt sind, wenigstens
     * aber die Kontonummer.
     *
     * @param account unvollständige Konto-Informationen, bei denen die fehlenden Daten nachgetragen
     *                werden
     */
    void fillAccountInfo(Konto account);

    /**
     * Gibt ein Konto-Objekt zu einer bestimmten Kontonummer zurück. Dazu wird die Liste, die via
     * {@link #getAccounts()} erzeugt wird, nach der Kontonummer durchsucht. Es wird in
     * jedem Fall ein nicht-leeres Kontoobjekt zurückgegeben. Wird die Kontonummer jedoch nicht in
     * der Liste gefunden, so wird das Konto-Objekt aus den "allgemeinen" Bank-Daten gebildet:
     * Kontonummer=<code>number</code>; Länderkennung, BLZ und Kunden-ID aus dem Passport-Objekt;
     * Währung des Kontos hart auf "EUR"; Name=Kunden-ID.
     *
     * @param number die Kontonummer, für die ein Konto-Objekt erzeugt werden soll
     * @return ein Konto-Objekt, welches mindestens die Kontonummer enthält. Wenn
     * verfügbar, so sind auch die restlichen Informationen über dieses Konto (BLZ,
     * Inhaber, Währung usw.) ausgefüllt
     */
    Konto getAccount(String number);

    /**
     * Gibt den Hostnamen des HBCI-Servers für dieses Passport zurück. Handelt es sich bei
     * dem Passport-Objekt um ein PIN/TAN-Passport, so enthält dieser String die URL,
     * die für die HTTPS-Kommunikation mit dem HBCI-Server der Bank benutzt wird.
     *
     * @return Hostname oder IP-Adresse des HBCI-Servers
     */
    String getHost();

    void setHost(String host);

    /**
     * Gibt die TCP-Portnummer auf dem HBCI-Server zurück, zu der eine
     * HBCI-Verbindung aufgebaut werden soll. In der Regel ist das der Port 3000,
     * für PIN/TAN-Passports wird hier 443 (für HTTPS-Port) zurückgegeben.
     * Der zu benutzende TCP-Port für die Kommunikation kannn mit
     * {@link #setPort(Integer)} geändert werden.
     *
     * @return TCP-Portnummer auf dem HBCI-Server
     */
    Integer getPort();

    void setPort(Integer port);

    /**
     * Gibt die Benutzerkennung zurück, die zur Authentifikation am
     * HBCI-Server benutzt wird.
     *
     * @return Benutzerkennung für Authentifikation
     */
    String getUserId();

    void setUserId(String userid);

    /**
     * <p>Gibt die Kunden-ID zurück, die von <em>HBCI4Java</em> für die
     * Initialisierung eines Dialoges benutzt wird. Zu einer Benutzerkennung
     * ({@link #getUserId()}), welche jeweils an ein bestimmtes Medium
     * gebunden ist, kann es mehrere Kunden-IDs geben. Die verschiedenen
     * Kunden-IDs entsprechen verschiedenen Rollen, in denen der Benutzer
     * auftreten kann.</p>
     * <p>In den meisten Fällen gibt es zu einer Benutzerkennung nur eine
     * einzige Kunden-ID. Wird von der Bank keine Kunden-ID explizit vergeben,
     * so ist die Kunden-ID identisch mit der Benutzerkennung.</p>
     *
     * @return Kunden-ID für die HBCI-Kommunikation
     */
    String getCustomerId();

    /**
     * Setzen der zu verwendenden Kunden-ID. Durch Aufruf dieser Methode wird die
     * Kunden-ID gesetzt, die beim nächsten Ausführen eines HBCI-Dialoges
     * ({@link org.kapott.hbci.manager.HBCIDialog#execute(boolean)})
     * benutzt wird. Diese neue Kunden-ID wird dann außerdem permanent im
     * jeweiligen Sicherheitsmedium gespeichert (sofern das von dem Medium
     * unterstützt wird).
     *
     * @param customerid die zu verwendende Kunden-ID; wird keine customerid
     *                   angegeben (<code>null</code> oder ""), so wird automatisch die
     *                   User-ID verwendet.
     * @see #getCustomerId()
     */
    void setCustomerId(String customerid);

    boolean isSupported();

    boolean hasInstSigKey();

    boolean hasInstEncKey();

    /**
     * Gibt die Versionsnummer der lokal gespeicherten BPD zurück. Sind keine
     * BPD vorhanden, so wird "0" zurückgegeben. Leider benutzen einige Banken
     * "0" auch als Versionsnummer für die tatsächlich vorhandenen BPD, so
     * dass bei diesen Banken auch dann "0" zurückgegeben wird, wenn in Wirklichkeit
     * BPD vorhanden sind.
     *
     * @return Versionsnummer der lokalen BPD
     */
    String getBPDVersion();

    /**
     * Gibt die Versionsnummer der lokal gespeicherten UPD zurück. Sind keine UPD
     * lokal vorhanden, so wird "0" zurückgegeben. Siehe dazu auch
     * {@link #getBPDVersion()}.
     *
     * @return Versionsnummer der lokalen UPD
     */
    String getUPDVersion();

    /**
     * Gibt den Namen des Kreditinstitutes zurück. Diese Information wird aus
     * den BPD ermittelt. Sind keine BPD vorhanden bzw. steht da kein Name drin,
     * so wird <code>null</code> zurückgegeben.
     *
     * @return Name des Kreditinstitutes
     */
    String getInstName();

    int getMaxGVperMsg();

    int getMaxMsgSizeKB();

    String[] getSuppVersions();

    String getDefaultLang();

    HashMap<String, String> getProperties();

    HBCICallback getCallback();

    void setPersistentData(String s, Object p2);

    HashMap<String, String> getJobRestrictions(String name);

    Object getPersistentData(String s);

    String getProxy();

    String getProxyUser();

    String getProxyPass();

    Object getCryptFunction();

    byte[] decrypt(byte[] cryptedkey, byte[] cryptedstring);
}
