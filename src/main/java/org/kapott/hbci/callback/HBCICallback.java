/*  $Id: HBCICallback.java,v 1.2 2011/05/09 15:07:02 willuhn Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2008  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    HBCI4Java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General License for more details.

    You should have received a copy of the GNU General License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.callback;

import org.kapott.hbci.manager.HHDVersion;

import java.util.List;

/**
 * <p>Schnittstelle, die eine Callback-Klasse implementieren muss. Beim Initialisieren von <em>HBCI4Java</em>
 * muss ein Callback-Objekt angegeben werden. Die Klasse dieses Objektes muss die HBCICallback-Schnittstelle
 * implementieren. Der HBCI-Kernel ruft in bestimmten Situationen Methoden dieser Klasse auf. Das ist
 * z.B. dann der Fall, wenn eine bestimmte Aktion (Einlegen der Chipkarte) oder Eingabe (Passwort)
 * vom Anwender erwartet wird. AuÃerdem werden auf diesem Weg Informationen an den Anwender weitergegeben
 * (Mitteilungen des Kreditinstitutes bei der Dialoginitialisierung).</p>
 * <p>Ein Anwendungsentwickler muss die Methoden dieser Schnittstelle also geeignet implementieren,
 * um bei jeder möglichen Ursache für den Aufruf einer der Callback-Methoden sinnvoll zu reagieren.
 * Dabei müssen nicht immer tatsächlich alle Anfragen an den Anwender weitergegeben werden. Ist z.B.
 * das Passwort für die Schlüsseldatei eines Passports bereits bekannt, so kann die entsprechende
 * Methode dieses Passwort direkt zurückgeben, ohne den Anwender erneut danach zu fragen. </p>
 */
public interface HBCICallback {
    /**
     * Ursache des Callback-Aufrufes: Chipkarte benötigt (im Chipkartenterminal). Dieser Callback
     * tritt auf, wenn der HBCI-Kernel auf das Einlegen der HBCI-Chipkarte in den Chipkartenleser
     * wartet. Als Reaktion auf diesen Callback darf nur eine entsprechende Aufforderung o.ä.
     * angezeigt werden, die Callback-Methode muss anschlieÃend sofort beendet werden. Das eigentliche
     * "Warten" auf die Chipkarte sowie das Erkennen, dass eine Chipkarte eingelegt wurde,
     * wird von <em>HBCI4Java</em> übernommen. Ist das Einlegen der Chipkarte abgeschlossen, so wird ein
     * weiterer Callback mit dem Code <code>HAVE_CHIPCARD</code> erzeugt.
     */
    int NEED_CHIPCARD = 2;
    /**
     * Ursache des Callback-Aufrufes: PIN-Eingabe am Chipkartenterminal erwartet. Dieser Callback
     * zeigt an, dass der Anwender jetzt die HBCI-PIN am Chipkartenterminal eingeben muss. Hier
     * gilt das gleiche wie beim Code <code>NEED_CHIPCARD</code>: Die Callback-Methode darf hier
     * nur eine entsprechende Meldung o.ä. anzeigen und muss dann sofort zurückkehren -- <em>HBCI4Java</em> erledigt die
     * eigentliche Entgegennahme der PIN. Wurde die PIN eingegeben (oder die Eingabe abgebrochen),
     * so wird ein weiterer Callback-Aufruf mit dem Code <code>HAVE_HARDPIN</code> erzeugt.
     */
    int NEED_HARDPIN = 3;
    /**
     * Ursache des Callback-Aufrufes: PIN-Eingabe über Computer-Tastatur benötigt. Alternativ zum
     * Callback <code>NEED_HARDPIN</code> kann dieser Callback auftreten, wenn die direkte PIN-Eingabe
     * am Chipkartenterminal nicht möglich oder deaktiviert ist. In diesem Fall muss die PIN
     * "softwaremäÃig" eingegeben werden, d.h. der Anwender gibt die PIN über die PC-Tastatur
     * ein, welche über diesen Callback-Aufruf an den HBCI-Kernel übergeben wird. Der Kernel
     * übermittelt die PIN anschlieÃend zur Verifikation an die Chipkarte. In diesem Falle gibt es
     * keinen weiteren Callback-Aufruf, wenn die PIN-Verifikation abgeschlossen ist!
     */
    int NEED_SOFTPIN = 4;
    /**
     * Ursache des Callback-Aufrufes: PIN-Eingabe über Chipkartenterminal abgeschlossen. Dieser Callback
     * tritt auf, wenn die direkte PIN-Eingabe am Chipkartenleser abgeschlossen (oder abgebrochen) ist.
     * Dieser Aufruf kann dazu genutzt werden, evtl. angezeigte Meldungsfenster ("Bitte jetzt PIN eingeben")
     * wieder zu schlieÃen.
     */
    int HAVE_HARDPIN = 5;
    /**
     * Ursache des Callback-Aufrufes: Chipkarte wurde in Chipkartenterminal eingelegt. Dieser Callback
     * tritt auf, wenn das Einlegen der Chipkarte in den Chipkartenleser abgeschlossen (oder abgebrochen) ist.
     * Dieser Aufruf kann dazu genutzt werden, evtl. angezeigte Meldungsfenster ("Bitte jetzt Karte einlegen einlegen")
     * wieder zu schlieÃen.
     */
    int HAVE_CHIPCARD = 6;
    /**
     * Ursache des Callback-Aufrufes: Länderkennzeichen der Bankverbindung benötigt. Der Kernel benötigt
     * für ein neu zu erstellendes Passport-Medium das Länderkennzeichen der Bank, für die dieses
     * Passport benutzt werden soll. Da es sich i.d.R. um deutsche Banken handelt, kann die Callback-Routine
     * hier immer "DE" zurückgeben, anstatt tatsächlich auf eine Nutzereingabe zu warten.
     */
    int NEED_COUNTRY = 7;
    /**
     * Ursache des Callback-Aufrufes: Bankleitzahl der Bank benötigt. Für ein neu zu erstellendes Passport-Medium
     * wird die Bankleitzahl der Bank benötigt, für die dieses Passport verwendet werden soll.
     */
    int NEED_BLZ = 8;
    /**
     * Ursache des Callback-Aufrufes: Netzwerkadresse des HBCI-Servers benötigt. Es wird die Hostadresse
     * benötigt, unter welcher der HBCI-Server der Bank zu erreichen ist. Dieses Callback tritt nur auf,
     * wenn der Kernel ein neues Passport-Medium erzeugt. Bei RDH- bzw. DDV-Passports wird hier eine
     * IP-Adresse oder ein vollständiger Hostname erwartet. Für PIN/TAN-Passports wird hier die URL
     * erwartet, unter der der HBCI-PIN/TAN-Handler auf entsprechende HTTPS-Requests reagiert. Dabei
     * muss das Prefix "<code>https://</code>" weggelassen werden (also beispielsweise
     * "<code>www.hbci-kernel.de/pintan/PinTanServlet</code>").
     */
    int NEED_HOST = 9;
    /**
     * Ursache des Callback-Aufrufes: TCP-Port, auf dem der HBCI-Server arbeitet (3000), benötigt. Dieser
     * Callback tritt nur auf, wenn ein neues Passport-Medium vom Kernel erzeugt wird. Da die TCP-Portnummer
     * für HBCI-Server immer "3000" ist, kann dieser Wert direkt von der Callback-Methode zurückgegeben
     * werden, anstatt auf eine Nutzereingabe zu warten.
     */
    int NEED_PORT = 10;
    /**
     * Ursache des Callback-Aufrufes: Nutzerkennung für HBCI-Zugang benötigt. Wird beim Anlegen eines neuen
     * Passport-Mediums und manchmal beim erstmaligen Benutzen einer DDV-Chipkarte erzeugt, wenn auf der
     * Chipkarte die Benutzerkennung noch nicht gespeichert ist.
     */
    int NEED_USERID = 11;

    /**
     * Ursache des Callback-Aufrufes: Institutsnachricht erhalten. Tritt dieser Callback auf, so enthält
     * der <code>msg</code>-Parameter der <code>callback</code>-Methode einen
     * String, den die Bank als Kreditinstitutsnachricht an den Kunden gesandt hat. Diese Nachricht sollte
     * dem Anwender i.d.R. angezeigt werden. <em>HBCI4Java</em> erwartet auf diesen Callback keine Antwortdaten.
     */
    int HAVE_INST_MSG = 14;
    /**
     * Ursache des Callback-Aufrufes: Chipkarte soll aus Chipkartenterminal entfernt werden. Dieser Callback
     * wird zur Zeit noch nicht benutzt.
     */
    int NEED_REMOVE_CHIPCARD = 15;
    /**
     * Ursache des Callback-Aufrufes: PIN für PIN/TAN-Verfahren benötigt. Dieser Callback tritt nur bei
     * Verwendung von PIN/TAN-Passports auf. Benötigt <em>HBCI4Java</em> die PIN, um die digitale Signatur zu
     * erzeugen, wird sie über diesen Callback abgefragt.
     */
    int NEED_PT_PIN = 16;
    /**
     * Ursache des Callback-Aufrufes: Kunden-ID für HBCI-Zugang benötigt. Dieser Callback tritt nur beim
     * Erzeugen eines neuen Passports auf. <em>HBCI4Java</em> benötigt die Kunden-ID, die das Kreditinstitut
     * dem Bankkunden zugewiesen hat (steht meist in dem Brief mit den Zugangsdaten). Hat eine Bank einem
     * Kunden keine separate Kunden-ID zugewiesen, so muss an dieser Stelle die Benutzer-Kennung (User-ID)
     * zurückgegeben werden.
     */
    int NEED_CUSTOMERID = 18;
    /**
     * <p>Ursache des Callback-Aufrufes: Fehler beim Verifizieren einer Kontonummer mit Hilfe
     * des jeweiligen Prüfzifferverfahrens. Tritt dieser Callback auf, so hat <em>HBCI4Java</em>
     * festgestellt, dass eine verwendete Kontonummer den Prüfziffercheck der dazugehörigen Bank nicht
     * bestanden hat. Der Anwender soll die Möglichkeit erhalten, die Kontonummer und/oder
     * Bankleitzahl zu korrigieren. Dazu wird ein String in der Form "BLZ|KONTONUMMER" im Parameter
     * <code>retData</code> der <code>callback</code>-Methode übergeben. Die Anwendung kann dem
     * Anwender also BLZ und Kontonummer anzeigen und diese evtl. ändern lassen. Die neue BLZ und
     * Kontonummer muss im Ergebnis wieder in der o.g. Form in die Rückgabevariable
     * <code>retData</code> eingetragen werden. Wurden BLZ oder Kontonummer geändert,
     * so führt <em>HBCI4Java</em> eine erneute Prüfung der Daten durch - schlägt diese
     * wieder fehl, so wird der Callback erneut erzeugt, diesmal natürlich mit den neuen
     * (vom Anwender eingegebenen) Daten. Werden die Daten innerhalb der Callback-Methode nicht
     * geändert (bleibt also der Inhalt von <code>retData</code> unverändert), so übernimmt
     * <em>HBCI4Java</em> die Kontodaten trotz des fehlgeschlagenen Prüfziffern-Checks</p>
     * <p>Die automatische Ãberprüfung von Kontonummern findet statt, wenn HBCI-Jobs mit
     * Hilfe des Highlevel-Interfaces (siehe dazu Paketbeschreibung von <code>org.kapott.hbci.GV</code>)
     * erzeugt werden. Beim Hinzufügen eines so erzeugten Jobs zur Menge der auszuführenden
     * Aufträge wird die Ãberprüfung für alle in diesem Job benutzten Kontonummern durchgeführt. Für jeden
     * Prüfzifferfehler, der dabei entdeckt wird, wird dieser Callback erzeugt.<br/>
     * Tritt beim Ãberprüfen einer IBAN ein Fehler auf, wird statt dessen
     * {@link #HAVE_IBAN_ERROR} als Callback-Reason verwendet.
     */
    int HAVE_CRC_ERROR = 19;
    /**
     * <p>Ursache des Callback-Aufrufes: Es ist ein Fehler aufgetreten, der auf Wunsch
     * des Anwenders ignoriert werden kann. Durch Setzen bestimmter Kernel-Parameter kann
     * festgelegt werden, dass beim Auftreten bestimmter Fehler zur Laufzeit nicht sofort eine Exception
     * geworfen wird, sondern dass statt dessen erst dieser Callback erzeugt wird, welcher als <code>msg</code>
     * eine entsprechende Problembeschreibung enthält. <em>HBCI4Java</em> erwartet einen
     * boolschen Rückgabewert, der beschreibt, ob der Fehler ignoriert werden soll oder ob eine
     * enstprechende Exception erzeugt werden soll. Der Anwender kann den Fehler ignorieren, indem
     * im <code>retData</code> Rückgabedaten-Objekt ein leerer String zurückgegeben wird, oder er kann
     * erzwingen, dass <em>HBCI4Java</em> tatsächlich abbricht, indem ein nicht-leerer String im
     * <code>retData</code>-Objekt zurückgegen wird. Siehe dazu auch die Beschreibung des
     * Rückgabe-Datentyps {@link #TYPE_BOOLEAN}.</p>
     * <p>Das Ignorieren eines Fehlers kann dazu führen, dass <em>HBCI4Java</em> später trotzdem eine
     * Exception erzeugt, z.B. weil der Fehler in einem bestimmten Submodul doch nicht einfach ignoriert
     * werden kann, oder es kann auch dazu führen, dass Aufträge von der Bank nicht angenommen werden usw.
     * Es wird aber in jedem Fall eine entsprechende Fehlermeldung erzeugt.</p>
     */
    int HAVE_ERROR = 20;

    /**
     * Ursache des Callback-Aufrufes: Passwort für das Einlesen der Schlüsseldatei
     * benötigt. Dieser Callback tritt beim Laden eines Passport-Files auf, um nach dem
     * Passwort für die Entschlüsselung zu fragen.
     * ACHTUNG: Die folgenden Zeichen duerfen NICHT im Passwort enthalten sein: ÃÂ´Â°Â§üÃöäÃÃ
     */
    int NEED_PASSPHRASE_LOAD = 21;
    /**
     * Ursache des Callback-Aufrufes: Passwort für das Erzeugen der Schlüsseldatei
     * benötigt. Dieser Callback tritt beim Erzeugen eines neuen Passport-Files bzw. beim
     * Ãndern der Passphrase für eine Schlüsseldatei auf, um nach dem
     * Passwort für die Verschlüsselung zu fragen.
     * ACHTUNG: Die folgenden Zeichen duerfen NICHT im Passwort enthalten sein: ÃÂ´Â°Â§üÃöäÃÃ
     */
    int NEED_PASSPHRASE_SAVE = 22;
    /**
     * <p>Ursache des Callback-Aufrufes: Auswahl eines Eintrages aus einer SIZ-RDH-Datei
     * benötigt. Dieser Callback tritt nur bei Verwendung der Passport-Variante
     * SIZRDHFile auf. In einer SIZ-RDH-Schlüsseldatei können mehrere HBCI-Zugangsdatensätze
     * gespeichert sein. Wird eine solche Datei mit mehreren Datensätzen geladen,
     * so wird dieser Callback erzeugt, um den zu benutzenden Datensatz aus der Datei
     * auswählen zu können.</p>
     * <p>Dazu wird beim Aufruf der Callback-Routine im Parameter <code>retData</code>
     * ein String übergeben, der aus Informationen über alle in der Datei vorhandenen
     * Zugangsdatensätze besteht. Das Format dieses Strings ist
     * <code>&lt;ID&gt;;&lt;BLZ&gt;;&lt;USERID&gt;[|&lt;ID&gt;;&lt;BLZ&gt;;&lt;USERID&gt;...]</code>
     * Es werden also die verschiedenen Datensätze durch "|" getrennt dargestellt,
     * wobei jeder einzelne Datensatz durch eine ID, die Bankleitzahl und die UserID
     * dieses Datensatzes repräsentiert wird.</p>
     * <p>Dem Anwender müssen diese Daten in geeigneter Weise zur Auswahl angezeigt
     * werden. Die Callback-Routine muss schlieÃlich die ID des vom Anwender ausgewählten
     * Eintrages im <code>retData</code>-Rückgabedatenobjekt zurückgeben.</p>
     * <p>Beim Aufruf der Callback-Routine könnte <code>retData</code> also folgendes
     * enthalten: <code>0;09950003;Kunde-001|1;01234567;Kunde8|4;8765432;7364634564564</code>.
     * Der Anwender muss sich also zwischen den Datensätzen "09950003;Kunde-001",
     * "01234567;Kunde8" und "8765432;7364634564564" entscheiden. Je nach Auswahl
     * muss in <code>retData</code> dann jeweils "0", "1" oder "4" zurückgegeben werden.</p>
     */
    int NEED_SIZENTRY_SELECT = 23;
    /**
     * <p>Ursache des Callback-Aufrufes: es wird eine Netz-Verbindung zum HBCI-Server benötigt.
     * Dieser Callback wird erzeugt, bevor <em>HBCI4Java</em> eine Verbindung zum HBCI-Server
     * aufbaut. Bei Client-Anwendungen, die mit einer Dialup-Verbindung zum Internet arbeiten,
     * kann dieser Callback benutzt werden, um den Anwender zum Aktivieren der Internet-Verbindung
     * aufzufordern. Es werden keine Rückgabedaten erwartet. Sobald die Internet-Verbindung
     * nicht mehr benötigt wird, wird ein anderer Callback ({@link #CLOSE_CONNECTION}) erzeugt.</p>
     * <p>Dieses Callback-Paar wird immer dann erzeugt, wenn von der aktuellen
     * <em>HBCI4Java</em>-Verarbeitungsstufe tatsächlich eine Verbindung zum Internet benötigt
     * wird bzw. nicht mehr ({@link #CLOSE_CONNECTION}) benötigt wird. U.U. werden allerdings
     * mehrere solcher Verarbeitungsstufen direkt hintereinander ausgeführt - das kann zur Folge
     * haben, dass auch diese Callback-Paare mehrmals direkt hintereinander auftreten. Das tritt
     * vor allem beim erstmaligen Initialiseren eines Passports auf. Beim Aufruf von
     * <code>new&nbsp;HBCIHandler(...)</code> werden verschiedene Passport-Daten mit
     * der Bank abgeglichen, dabei wird u.U. mehrmals
     * <code>NEED_CONNECTION</code>/<code>CLOSE_CONNECTION</code> aufgerufen. Evtl.
     * sollte der Callback-Handler der Anwendung in diesem Fall also entsprechende
     * MaÃnahmen treffen.</p>
     */
    int NEED_CONNECTION = 24;
    /**
     * Ursache des Callback-Aufrufes: die Netzwerk-Verbindung zum HBCI-Server wird nicht länger
     * benötigt. Dieser Callback wird aufgerufen, sobald <em>HBCI4Java</em> die Kommunikation
     * mit dem HBCI-Server vorläufig beendet hat. Dieser Callback kann zusammen mit dem
     * Callback {@link #NEED_CONNECTION} benutzt werden, um für Clients mit Dialup-Verbindungen
     * die Online-Zeiten zu optimieren. Bei diesem Callback werden keine Rückgabedaten
     * erwartet
     */
    int CLOSE_CONNECTION = 25;
    /**
     * Ursache des Callbacks: es wird ein Nutzername für die Authentifizierung
     * am Proxy-Server benötigt. Wird für die HTTPS-Verbindungen bei HBCI-PIN/TAN
     * ein Proxy-Server verwendet, und verlangt dieser Proxy-Server eine
     * Authentifizierung, so wird über diesen Callback nach dem Nutzernamen
     * gefragt, falls dieser nicht schon durch den Kernel-Parameter
     * <code>client.passport.PinTan.proxyuser</code> gesetzt wurde
     */
    int NEED_PROXY_USER = 28;

    /**
     * Ursache des Callbacks: es wird ein Passwort für die Authentifizierung
     * am Proxy-Server benötigt. Wird für die HTTPS-Verbindungen bei HBCI-PIN/TAN
     * ein Proxy-Server verwendet, und verlangt dieser Proxy-Server eine
     * Authentifizierung, so wird über diesen Callback nach dem Passwort
     * gefragt, falls dieses nicht schon durch den Kernel-Parameter
     * <code>client.passport.PinTan.proxypass</code> gesetzt wurde
     */
    int NEED_PROXY_PASS = 29;

    /**
     * Ursache des Callbacks: beim Ãberprüfen einer IBAN ist ein Fehler aufgetreten.
     * in <code>retData</code> wird die fehlerhafte IBAN übergeben. Der Nutzer
     * sollte die IBAN korrieren. Die korrigierte IBAN sollte wieder in <code>retData</code>
     * zurückgegeben werden. Wird die IBAN nicht verändert, wird diese IBAN trotz
     * des Fehlers verwendet. Wird eine korrigierte IBAN zum Nutzer zurückgegeben,
     * wird für diese erneut ein Prüfsummencheck ausgeführt. Schlägt der wieder fehl,
     * wird der Callback erneut erzeugt. Das geht so lange, bis entweder der
     * Prüfsummencheck erfolgreich war oder bis die IBAN vom Nutzer nicht verändert
     * wird. Siehe dazu auch {@link #HAVE_CRC_ERROR}.
     */
    int HAVE_IBAN_ERROR = 30;

    /**
     * Ursache des Callbacks: Kernel fragt um Erlaubnis, Daten an den InfoPoint-Server
     * zu senden. An bestimmten Punkten der HBCI-Kommunikation sendet der HBCI-Kernel
     * Daten über erfolgreich gelaufene Verbindungen an den InfoPoint-Server (siehe
     * Kernel-Parameter "<code>infoPoint.enabled</code>" und Datei <em>README.InfoPoint</em>).
     * Bei diesem Callback wird im StringBuffer <code>retData</code> das XML-Document
     * übergeben, welches an den InfoPoint-Server gesendet werden soll. Als Antwort
     * wird ein Boolean-Wert erwartet (siehe {@link #TYPE_BOOLEAN}). Dürfen die
     * Daten gesendet werden, ist von der Anwendung also ein leerer String in
     * <code>retData</code> zurückzugeben, ansonsten ein beliebiger nicht-leerer String.
     */
    int NEED_INFOPOINT_ACK = 31;

    /**
     * Ursache des Callback-Aufrufes: eine Photo-TAN für PIN/TAN-Verfahren benötigt. Dieser
     * Callback tritt nur bei Verwendung von PIN/TAN-Passports mit dem photoTAN-Verfahren auf.
     * Im Callback wird im StringBuffer der Wert aus dem HHDuc uebergeben. Das sind die Roh-Daten
     * des Bildes inclusive Angaben zum Bildformat. HBCI4Java enthaelt eine Klasse "MatrixCode",
     * mit dem diese Daten dann gelesen werden koennen.
     **/
    int NEED_PT_PHOTOTAN = 33;

    /**
     * <p>Ursache des Callbacks: falsche PIN eingegeben
     */
    int WRONG_PIN = 40;

    /** <p>Ursache des Callbacks: Dialogantwort 3072 der GAD - UserID und CustomerID werden ausgetauscht */
    /**
     * <p>im Parameter retData stehen die neuen Daten im Format UserID|CustomerID drin
     */
    int USERID_CHANGED = 41;

    /**
     * erwarteter Datentyp der Antwort: keiner (keine Antwortdaten erwartet)
     */
    int TYPE_NONE = 0;
    /**
     * erwarteter Datentyp der Antwort: geheimer Text (bei Eingabe nicht anzeigen)
     */
    int TYPE_SECRET = 1;
    /**
     * erwarteter Datentyp der Antwort: "normaler" Text
     */
    int TYPE_TEXT = 2;
    /**
     * <p>erwarteter Datentyp der Antwort: ja/nein, true/false, weiter/abbrechen
     * oder ähnlich. Da das
     * Rückgabedatenobjekt immer ein <code>StringBuffer</code> ist, wird hier
     * folgende Kodierung verwendet: die beiden möglichen Werte für die
     * Antwort (true/false, ja/nein, weiter/abbrechen, usw.) werden dadurch
     * unterschieden, dass für den einen Wert ein <em>leerer</em> String
     * zurückgegeben wird, für den anderen Wert ein <em>nicht leerer</em>
     * beliebiger String. Einige Callback-Reasons können auch den Inhalt
     * des nicht-leeren Strings auswerten. Eine genaue Beschreibung der jeweilis
     * möglichen Rückgabedaten befinden sich in der Beschreibung der
     * Callback-Reasons (<code>HAVE_*</code> bzw. <code>NEED_*</code>), bei
     * denen Boolean-Daten als Rückgabewerte benötigt werden.</p>
     * <p>Siehe dazu auch die Hinweise in der Paketbeschreibung zum Paket
     * <code>org.kapott.hbci.callback</code>.</p>
     */
    int TYPE_BOOLEAN = 3;

    /**
     * Kernel-Status: Erzeuge Auftrag zum Versenden. Als Zusatzinformation
     * wird bei diesem Callback das <code>HBCIJob</code>-Objekt des
     * Auftrages übergeben, dessen Auftragsdaten gerade erzeugt werden.
     */
    int STATUS_SEND_TASK = 1;
    /**
     * Kernel-Status: Auftrag gesendet. Tritt auf, wenn zu einem bestimmten Job
     * Auftragsdaten empfangen und ausgewertet wurden. Als Zusatzinformation wird
     * das <code>HBCIJob</code>-Objekt des jeweiligen Auftrages übergeben.
     */
    int STATUS_SEND_TASK_DONE = 2;
    /**
     * Kernel-Status: hole BPD. Kann nur während der Passport-Initialisierung
     * auftreten und zeigt an, dass die BPD von der Bank abgeholt werden müssen,
     * weil sie noch nicht lokal vorhanden sind. Es werden keine zusätzlichen
     * Informationen übergeben.
     */
    int STATUS_INST_BPD_INIT = 3;
    /**
     * Kernel-Status: BPD aktualisiert. Dieser Status-Callback tritt nach dem expliziten
     * Abholen der BPD ({@link #STATUS_INST_BPD_INIT}) auf und kann auch nach einer
     * Dialog-Initialisierung auftreten, wenn dabei eine neue BPD vom Kreditinstitut
     * empfangen wurde. Als Zusatzinformation wird ein <code>Properties</code>-Objekt
     * mit den neuen BPD übergeben.
     */
    int STATUS_INST_BPD_INIT_DONE = 4;
    /**
     * Kernel-Status: hole Institutsschlüssel. Dieser Status-Callback zeigt an, dass
     * <em>HBCI4Java</em> die öffentlichen Schlüssel des Kreditinstitutes abholt.
     * Dieser Callback kann nur beim Initialisieren eines Passportes
     * und bei Verwendung von RDH als Sicherheitsverfahren auftreten. Es werden keine
     * zusätzlichen Informationen übergeben.
     */
    int STATUS_INST_GET_KEYS = 5;
    /**
     * Kernel-Status: Institutsschlüssel aktualisiert. Dieser Callback tritt
     * auf, wenn <em>HBCI4Java</em> neue öffentliche Schlüssel der Bank
     * empfangen hat. Dieser Callback kann nach dem expliziten Anfordern der
     * neuen Schlüssel ({@link #STATUS_INST_GET_KEYS}) oder nach einer Dialog-Initialisierung
     * auftreten, wenn das Kreditinstitut neue Schlüssel übermittelt hat. Es
     * werden keine zusätzlichen Informationen übergeben.
     */
    int STATUS_INST_GET_KEYS_DONE = 6;
    /**
     * Kernel-Status: Sende Nutzerschlüssel. Wird erzeugt, wenn <em>HBCI4Java</em>
     * neue Schlüssel des Anwenders an die Bank versendet. Das tritt beim erstmaligen
     * Einrichten eines RDH-Passportes bzw. nach dem manuellen Erzeugen neuer
     * RDH-Schlüssel auf. Es werden keine zusätzlichen Informationen übergeben.
     */
    int STATUS_SEND_KEYS = 7;
    /**
     * Kernel-Status: Nutzerschlüssel gesendet. Dieser Callback zeigt an, dass die RDH-Schlüssel
     * des Anwenders an die Bank versandt wurden. Der Erfolg dieser Aktion kann nicht
     * allein durch das Auftreten dieses Callbacks angenommen werden! Es wird der Status
     * des Nachrichtenaustauschs ({@link org.kapott.hbci.status.HBCIMsgStatus})
     * als Zusatzinformation übergeben.
     */
    int STATUS_SEND_KEYS_DONE = 8;
    /**
     * Kernel-Status: aktualisiere System-ID. Dieser Status-Callback wird erzeugt, wenn
     * <em>HBCI4Java</em> die System-ID, die für das RDH-Verfahren benötigt
     * wird, synchronisiert. Der Callback kann nur beim Initialisieren eines Passports
     * auftreten. Es werden keine Zusatzinformationen übergeben.
     */
    int STATUS_INIT_SYSID = 9;
    /**
     * Kernel-Status: System-ID aktualisiert. Dieser Callback tritt auf, wenn im Zuge der
     * Synchronisierung ({@link #STATUS_INIT_SYSID}) eine System-ID empfangen wurde. Als
     * Zusatzinformation wird ein Array übergeben, dessen erstes Element die Statusinformation
     * zu diesem Nachrichtenaustausch darstellt ({@link org.kapott.hbci.status.HBCIMsgStatus})
     * und dessen zweites Element die neue System-ID ist.
     */
    int STATUS_INIT_SYSID_DONE = 10;
    /**
     * Kernel-Status: hole UPD. Kann nur während der Passport-Initialisierung
     * auftreten und zeigt an, dass die UPD von der Bank abgeholt werden müssen,
     * weil sie noch nicht lokal vorhanden sind. Es werden keine zusätzlichen
     * Informationen übergeben.
     */
    int STATUS_INIT_UPD = 11;
    /**
     * Kernel-Status: UPD aktualisiert. Dieser Status-Callback tritt nach dem expliziten
     * Abholen der UPD ({@link #STATUS_INIT_UPD}) auf und kann auch nach einer
     * Dialog-Initialisierung auftreten, wenn dabei eine neue UPD vom Kreditinstitut
     * empfangen wurde. Als Zusatzinformation wird ein <code>Properties</code>-Objekt
     * mit den neuen UPD übergeben.
     */
    int STATUS_INIT_UPD_DONE = 12;
    /**
     * Kernel-Status: sperre Nutzerschlüssel. Dieser Status-Callback wird erzeugt, wenn
     * <em>HBCI4Java</em> einen Auftrag zur Sperrung der aktuellen Nutzerschlüssel
     * generiert. Es werden keine Zusatzinformationen übergeben.
     */
    int STATUS_LOCK_KEYS = 13;
    /**
     * Kernel-Status: Nutzerschlüssel gesperrt. Dieser Callback tritt auf, nachdem die
     * Antwort auf die Nachricht "Sperren der Nutzerschlüssel" eingetroffen ist. Ein
     * Auftreten dieses Callbacks ist keine Garantie dafür, dass die Schlüsselsperrung
     * erfolgreich abgelaufen ist. Es wird der Status
     * des Nachrichtenaustauschs ({@link org.kapott.hbci.status.HBCIMsgStatus})
     * als Zusatzinformation übergeben.
     */
    int STATUS_LOCK_KEYS_DONE = 14;
    /**
     * Kernel-Status: aktualisiere Signatur-ID. Dieser Status-Callback wird erzeugt, wenn
     * <em>HBCI4Java</em> die Signatur-ID, die für das RDH-Verfahren benötigt
     * wird, synchronisiert. Der Callback kann nur beim Initialisieren eines Passports
     * auftreten. Es werden keine Zusatzinformationen übergeben.
     */
    int STATUS_INIT_SIGID = 15;
    /**
     * Kernel-Status: Signatur-ID aktualisiert. Dieser Callback tritt auf, wenn im Zuge der
     * Synchronisierung ({@link #STATUS_INIT_SIGID}) eine Signatur-ID empfangen wurde. Als
     * Zusatzinformation wird ein Array übergeben, dessen erstes Element die Statusinformation
     * zu diesem Nachrichtenaustausch darstellt ({@link org.kapott.hbci.status.HBCIMsgStatus})
     * und dessen zweites Element die neue Signatur-ID (ein Long-Objekt) ist.
     */
    int STATUS_INIT_SIGID_DONE = 16;
    /**
     * Kernel-Status: Starte Dialog-Initialisierung. Dieser Status-Callback zeigt an, dass
     * <em>HBCI4Java</em> eine Dialog-Initialisierung startet. Es werden keine
     * zusätzlichen Informationen übergeben.
     */
    int STATUS_DIALOG_INIT = 17;
    /**
     * Kernel-Status: Dialog-Initialisierung ausgeführt. Dieser Callback tritt nach dem
     * Durchführen der Dialog-Initialisierung auf. Als
     * Zusatzinformation wird ein Array übergeben, dessen erstes Element die Statusinformation
     * zu diesem Nachrichtenaustausch darstellt ({@link org.kapott.hbci.status.HBCIMsgStatus})
     * und dessen zweites Element die neue Dialog-ID ist.
     */
    int STATUS_DIALOG_INIT_DONE = 18;
    /**
     * Kernel-Status: Beende Dialog. Wird ausgelöst, wenn <em>HBCI4Java</em> den
     * aktuellen Dialog beendet. Es werden keine zusätzlichen Daten übergeben.
     */
    int STATUS_DIALOG_END = 19;
    /**
     * Kernel-Status: Dialog beendet. Wird ausgeführt, wenn der HBCI-Dialog tatsächlich
     * beendet ist. Es wird der Status
     * des Nachrichtenaustauschs ({@link org.kapott.hbci.status.HBCIMsgStatus})
     * als Zusatzinformation übergeben.
     */
    int STATUS_DIALOG_END_DONE = 20;
    /**
     * Kernel-Status: Erzeuge HBCI-Nachricht. Dieser Callback zeigt an, dass <em>HBCI4Java</em>
     * gerade eine HBCI-Nachricht erzeugt. Es wird der Name der Nachricht als zusätzliches
     * Objekt übergeben.
     */
    int STATUS_MSG_CREATE = 21;
    /**
     * Kernel-Status: Signiere HBCI-Nachricht. Dieser Callback wird aufgerufen, wenn
     * <em>HBCI4Java</em> die ausgehende HBCI-Nachricht signiert. Es werden keine
     * zusätzlichen Informationen übergeben.
     */
    int STATUS_MSG_SIGN = 22;
    /**
     * Kernel-Status: Verschlüssele HBCI-Nachricht. Wird aufgerufen, wenn <em>HBCI4Java</em>
     * die ausgehende HBCI-Nachricht verschlüsselt. Es werden keine zusätzlichen
     * Informationen übergeben.
     */
    int STATUS_MSG_CRYPT = 23;
    /**
     * Kernel-Status: Sende HBCI-Nachricht (bei diesem Callback ist das
     * <code>passport</code>-Objekt immer <code>null</code>). Wird aufgerufen,
     * wenn die erzeugte HBCI-Nachricht an den HBCI-Server versandt wird. Es werden
     * keine zusätzlichen Informationen übergeben.
     */
    int STATUS_MSG_SEND = 24;
    /**
     * Kernel-Status: Entschlüssele HBCI-Nachricht. Wird aufgerufen, wenn die empfangene
     * HBCI-Nachricht von <em>HBCI4Java</em> entschlüsselt wird. Es werden keine
     * zusätzlichen Informationen übergeben.
     */
    int STATUS_MSG_DECRYPT = 25;
    /**
     * Kernel-Status: Ãberprüfe digitale Signatur der Nachricht. Wird aufgerufen, wenn
     * <em>HBCI4Java</em> die digitale Signatur der empfangenen Antwortnachricht
     * überprüft. Es werden keine zusätzlichen Informationen übergeben.
     */
    int STATUS_MSG_VERIFY = 26;
    /**
     * Kernel-Status: Empfange HBCI-Antwort-Nachricht (bei diesem Callback ist das
     * <code>passport</code>-Objekt immer <code>null</code>). Wird aufgerufen, wenn
     * die Antwort-HBCI-Nachricht vom HBCI-Server empfangen wird. Es werden keine
     * zusätzlichen Informationen übergeben.
     */
    int STATUS_MSG_RECV = 27;
    /**
     * Kernel-Status: Parse HBCI-Antwort-Nachricht (bei diesem Callback ist das
     * <code>passport</code>-Objekt immer <code>null</code>). Wird aufgerufen, wenn
     * <em>HBCI4Java</em> versucht, die empfangene Nachricht zu parsen. Es wird
     * der Name der erwarteten Nachricht als zusätzliche Information übergeben.
     */
    int STATUS_MSG_PARSE = 28;
    /**
     * Kernel-Status: Der Kernel sendet Informationen über eine erfolgreiche
     * Dialog-Initialisierung an den InfoPoint-Server (siehe auch <em>README.InfoPoint</em>).
     * Als zusätzlicher Parameter wird das XML-Dokument (als String) übergeben,
     * welches an den InfoPoint-Server gesendet wird.
     */
    int STATUS_SEND_INFOPOINT_DATA = 29;

    /**
     * Wird aufgerufen unmittelbar bevor die HBCI-Nachricht an den Server gesendet wird.
     * Als zusaetzliche Information wird die zu sendende Nachricht als String uebergeben.
     * Sie kann dann z.Bsp. in einem Log gesammelt werden, welches ausschliesslich
     * (zusammen mit {@link HBCICallback#STATUS_MSG_RAW_RECV}) die gesendeten und
     * empfangenen rohen HBCI-Nachrichten enthaelt. Sinnvoll zum Debuggen der Kommunikation
     * mit der Bank.
     */
    int STATUS_MSG_RAW_SEND = 30;

    /**
     * Wird aufgerufen unmittelbar nachdem die HBCI-Nachricht vom Server empfangen wurde.
     * Als zusaetzliche Information wird die empfangene Nachricht als String uebergeben.
     * Sie kann dann z.Bsp. in einem Log gesammelt werden, welches ausschliesslich
     * (zusammen mit {@link HBCICallback#STATUS_MSG_RAW_SEND}) die gesendeten und
     * empfangenen rohen HBCI-Nachrichten enthaelt. Sinnvoll zum Debuggen der Kommunikation
     * mit der Bank.
     */
    int STATUS_MSG_RAW_RECV = 31;

    /**
     * Wird vom HBCI-Kernel aufgerufen, wenn die Interaktion mit der
     * Anwendung erforderlich ist. In bestimmten Situationen benötigt der
     * HBCI-Kernel zusätzliche Daten bzw. muss auf die Ausführung einer
     * Aktion des Nutzers warten. Dann wird diese Methode aufgerufen. Dabei wird
     * ein Code (<code>reason</code>) übergeben, der anzeigt, welche Ursache
     * dieser Callbackaufruf hat, d.h. welche Daten oder Aktionen erwartet werden.
     * Falls Daten erwartet werden (z.B. ein Passwort, eine Benutzerkennung, ...),
     * so ist legt der Parameter <code>datatype</code> fest, wie diese Daten erwartet
     * werden. Die eigentlichen Daten muss die Anwendung im Objekt <code>retData</code>
     * ablegen (keinen neuen StringBuffer erzeugen, sondern den Inhalt von <code>retData</code>
     * überschreiben!). Bei einigen Callbacks übergibt <em>HBCI4Java</em> einen vorgeschlagenen
     * default-Wert für die Nutzereingabe im <em>retData</em>-Objekt. Diese Tatsache ist
     * besonders bei der Auswertung des Callbacks {@link #HAVE_CRC_ERROR} zu beachten!
     *
     * @param reason   gibt den Grund für diesen Aufruf an. Dieser Parameter kann
     *                 alle Werte annehmen, die als "Ursache des Callback-Aufrufes" in der Dokumentation
     *                 aufgeführt sind. Je nach Wert dieses Parameters werden vom Nutzer
     *                 Aktionen oder Eingaben erwartet.
     * @param msg      ein Hinweistext, der den Grund des Callbacks näher beschreibt.
     *                 Dieser Parameter muss nicht ausgewertet werden, der Parameter
     *                 <code>reason</code> ist bereits eindeutig. Er dient nur dazu,
     *                 bei Anwendungen, die nicht für jeden Ursache des Callback-Aufrufes einen eigenen
     *                 Hinweistext bereitstellen wollen, eine Art default-Wert für den
     *                 anzuzeigenden Text bereitzustellen.
     * @param datatype legt fest, welchen Datentyp die vom HBCI-Kernel erwarteten
     *                 Antwortdaten haben müssen. Ist dieser Wert gleich
     *                 <code>TYPE_NONE</code>, so werden keine Antwortdaten (also keine
     *                 Nutzereingabe) erwartet, bei <code>TYPE_SECRET</code> und
     *                 <code>TYPE_TEXT</code> wird ein normaler String erwartet.<br/>
     *                 Der Unterschied zwischen beiden ist der, dass bei
     *                 <code>TYPE_SECRET</code> sensible Daten (Passwörter usw.) eingegeben
     *                 werden sollen, so dass die Eingaberoutine evtl. anders arbeiten
     *                 muss (z.B. Sternchen anstatt dem eingegebenen Text darstellen).
     * @param retData  In diesem StringBuffer-Objekt müssen die Antwortdaten
     *                 abgelegt werden. Beim Aufruf der Callback-Methode von <em>HBCI4Java</em> wird dieser
     *                 StringBuffer u.U. mit einem vorgeschlagenen default-Wert für die Nutzereingabe
     *                 gefüllt.
     */
    void callback(int reason, List<String> messages, int datatype, StringBuilder retData);

    void tanChallengeCallback(String orderRef, String challenge, String challenge_hhd_uc, HHDVersion.Type type);

    String needTAN();

    /**
     * Wird vom HBCI-Kernel aufgerufen, um einen bestimmten Status der
     * Abarbeitung bekanntzugeben.
     *
     * @param statusTag gibt an, welche Stufe der Abarbeitung gerade erreicht
     *                  wurde (alle oben beschriebenen Konstanten, die mit <code>STATUS_</code>
     *                  beginnen)
     * @param o         ein Array aus Objekten, das zusätzliche Informationen zum jeweiligen
     *                  Status enthält. In den meisten Fällen handelt es sich um einen
     *                  String, der zusätzliche Informationen im Klartext enthält. Welche Informationen
     *                  das jeweils sind, ist der Beschreibung zu den einzelnen <code>STATUS_*</code>-Tag-Konstanten
     *                  zu entnehmen.
     */
    void status(int statusTag, Object[] o);

    /**
     * Kurzform für {@link #status(int, Object[])} für den Fall,
     * dass das <code>Object[]</code> nur ein einziges Objekt enthält
     */
    void status(int statusTag, Object o);

}

