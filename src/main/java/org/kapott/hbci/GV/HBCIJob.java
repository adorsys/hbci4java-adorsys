
/*  $Id: HBCIJob.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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

package org.kapott.hbci.GV;

import org.kapott.hbci.GV_Result.HBCIJobResult;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * <p>Schnittstelle für alle Aufträge, die via HBCI ausgeführt werden sollen. Ein
 * HBCIJob-Objekt wird nur innerhalb von <em>HBCI4Java</em> verwaltet. Durch Aufruf einer der Methoden
 * Die konkrete Klasse dieser Instanz ist
 * für den Anwendungsentwickler nicht von Bedeutung.</p>
 * <p>Die Anwendung muss nur die für diesen Job benötigten Parameter setzen (mit
 * {@link #setParam(String, String)}).</p>
 * <p>Nach Ausführung des HBCI-Dialoges können die Rückgabedaten und Statusinformationen für diesen
 * Job ermittelt werden. Dazu wird die Methoode {@link #getJobResult()} benötigt, welche
 * eine Instanz einer {@link org.kapott.hbci.GV_Result.HBCIJobResult}-Klasse zurückgibt.
 * Die konkrete Klasse, um die es sich bei diesem Result-Objekt handelt, ist vom Typ des ausgeführten
 * Jobs abhängig (z.B. gibt es eine Klasse, die Ergebnisdaten für Kontoauszüge enthält, eine
 * Klasse für Saldenabfragen usw.). Eine Beschreibung der einzelnen Klassen für Result-Objekte findet
 * sich im Package <code>org.kapott.hbci.GV_Result</code>. Eine Beschreibung, welcher Job welche Klasse
 * zurückgibt, befindet sich in der Package-Dokumentation zu diesem Package (<code>org.kapott.hbci.GV</code>).</p>
 */
public interface HBCIJob {
    /**
     * Gibt den internen Namen für diesen Job zurück.
     *
     * @return Job-Name, wie er intern von <em>HBCI4Java</em> verwendet wird.
     */
    String getName();

    /**
     * Gibt die für diesen Job verwendete Segment-Versionsnummer zurück
     */
    String getSegVersion();

    /**
     * Gibt alle möglichen Property-Namen für die Lowlevel-Rückgabedaten dieses
     * Jobs zurück. Die Lowlevel-Rückgabedaten können mit
     * {@link #getJobResult()} und {@link HBCIJobResult#getResultData()}
     * ermittelt werden. Diese Methode verwendet intern
     * {@link org.kapott.hbci.manager.HBCIHandler#getLowlevelJobResultNames(String)}.
     *
     * @return Liste aller prinzipiell möglichen Property-Keys für die
     * Lowlevel-Rückgabedaten dieses Jobs
     */
    List getJobResultNames();

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
     * <p>Diese Methode verwendet intern
     * {@link org.kapott.hbci.manager.HBCIHandler#getLowlevelJobRestrictions(String)}</p>.
     *
     * @return Properties-Objekt mit den einzelnen Restriktionen
     */
    Properties getJobRestrictions();

    /**
     * Gibt alle für diesen Job gesetzten Parameter zurück. In dem
     * zurückgegebenen <code>Properties</code>-Objekt sind werden die
     * Parameter als <em>Lowlevel</em>-Parameter abgelegt. Außerdem hat
     * jeder Lowlevel-Parametername zusätzlich ein Prefix, welches den
     * Lowlevel-Job angibt, für den der Parameter gilt (also z.B.
     * <code>Ueb3.BTG.value</code>
     *
     * @return aktuelle gesetzte Lowlevel-Parameter für diesen Job
     */
    Properties getLowlevelParams();

    /**
     * Setzen eines komplexen Job-Parameters (Kontodaten). Einige Jobs benötigten Kontodaten
     * als Parameter. Diese müssten auf "normalem" Wege durch mehrere Aufrufe von
     * {@link #setParam(String, String)} erzeugt werden (Länderkennung, Bankleitzahl,
     * Kontonummer, Unterkontomerkmal, Währung, IBAN, BIC).
     * Durch Verwendung dieser Methode wird dieser Weg abgekürzt. Es wird ein Kontoobjekt
     * übergeben, für welches die entsprechenden <code>setParam(String,String)</code>-Aufrufe
     * automatisch erzeugt werden.
     *
     * @param paramname die Basis der Parameter für die Kontodaten (für <code>my.country</code>,
     *                  <code>my.blz</code>, <code>my.number</code>, <code>my.subnumber</code>, <code>my.bic</code>,
     *                  <code>my.iban</code>, <code>my.curr</code> wäre das also "<code>my</code>")
     * @param acc       ein Konto-Objekt, aus welchem die zu setzenden Parameterdaten entnommen werden
     */
    void setParam(String paramname, Konto acc);

    /**
     * @param paramname
     * @param index
     * @param acc
     * @see HBCIJob#setParam(String, Konto) - jedoch mit Index.
     */
    void setParam(String paramname, Integer index, Konto acc);

    /**
     * Setzen eines komplexen Job-Parameters (Geldbetrag). Einige Jobs benötigten Geldbeträge
     * als Parameter. Diese müssten auf "normalem" Wege durch zwei Aufrufe von
     * {@link #setParam(String, String)} erzeugt werden (je einer für
     * den Wert und die Währung). Durch Verwendung dieser
     * Methode wird dieser Weg abgekürzt. Es wird ein Value-Objekt übergeben, für welches
     * die entsprechenden zwei <code>setParam(String,String)</code>-Aufrufe automatisch
     * erzeugt werden.
     *
     * @param paramname die Basis der Parameter für die Geldbetragsdaten (für "<code>btg.value</code>" und
     *                  "<code>btg.curr</code>" wäre das also "<code>btg</code>")
     * @param v         ein Value-Objekt, aus welchem die zu setzenden Parameterdaten entnommen werden
     */
    void setParam(String paramname, Value v);

    /**
     * Setzen eines Job-Parameters, bei dem ein Datums als Wert erwartet wird. Diese Methode
     * dient als Wrapper für {@link #setParam(String, String)}, um das Datum in einen korrekt
     * formatierten String umzuwandeln. Das "richtige" Datumsformat ist dabei abhängig vom
     * aktuellen Locale.
     *
     * @param paramName Name des zu setzenden Job-Parameters
     * @param date      Datum, welches als Wert für den Job-Parameter benutzt werden soll
     */
    void setParam(String paramName, Date date);

    void setParam(String paramName, Integer index, Date date);

    /**
     * Setzen eines Job-Parameters, bei dem ein Integer-Wert Da als Wert erwartet wird. Diese Methode
     * dient nur als Wrapper für {@link #setParam(String, String)}.
     *
     * @param paramName Name des zu setzenden Job-Parameters
     * @param i         Integer-Wert, der als Wert gesetzt werden soll
     */
    void setParam(String paramName, int i);

    /**
     * <p>Setzen eines Job-Parameters. Für alle Highlevel-Jobs ist in der Package-Beschreibung zum
     * Package <code>org.kapott.hbci.GV</code> eine Auflistung aller Jobs und deren Parameter zu finden.
     * Für alle Lowlevel-Jobs kann eine Liste aller Parameter entweder mit dem Tool
     * {@link org.kapott.hbci.tools.ShowLowlevelGVs} oder zur Laufzeit durch Aufruf
     * der Methode {@link org.kapott.hbci.manager.HBCIHandler#getLowlevelJobParameterNames(String)}
     * ermittelt werden.</p>
     * <p>Bei Verwendung dieser oder einer der anderen <code>setParam()</code>-Methoden werden zusätzlich
     * einige der Job-Restriktionen (siehe {@link #getJobRestrictions()}) analysiert. Beim Verletzen einer
     * der überprüften Einschränkungen wird eine Exception mit einer entsprechenden Meldung erzeugt.
     * Diese Überprüfung findet allerdings nur bei Highlevel-Jobs statt.</p>
     *
     * @param paramName der Name des zu setzenden Parameters.
     * @param value     Wert, auf den der Parameter gesetzt werden soll
     */
    void setParam(String paramName, String value);

    /**
     * <p>Setzen eines Job-Parameters. Für alle Highlevel-Jobs ist in der Package-Beschreibung zum
     * Package <code>org.kapott.hbci.GV</code> eine Auflistung aller Jobs und deren Parameter zu finden.
     * Für alle Lowlevel-Jobs kann eine Liste aller Parameter entweder mit dem Tool
     * {@link org.kapott.hbci.tools.ShowLowlevelGVs} oder zur Laufzeit durch Aufruf
     * der Methode {@link org.kapott.hbci.manager.HBCIHandler#getLowlevelJobParameterNames(String)}
     * ermittelt werden.</p>
     * <p>Bei Verwendung dieser oder einer der anderen <code>setParam()</code>-Methoden werden zusätzlich
     * einige der Job-Restriktionen (siehe {@link #getJobRestrictions()}) analysiert. Beim Verletzen einer
     * der überprüften Einschränkungen wird eine Exception mit einer entsprechenden Meldung erzeugt.
     * Diese Überprüfung findet allerdings nur bei Highlevel-Jobs statt.</p>
     *
     * @param paramName der Name des zu setzenden Parameters.
     * @param index     der Index bei Index-Parametern, sonst <code>null</code>
     * @param value     Wert, auf den der Parameter gesetzt werden soll
     */
    void setParam(String paramName, Integer index, String value);

    void setParam(String paramname, Integer index, Value v);

    /**
     * Gibt ein Objekt mit den Rückgabedaten für diesen Job zurück. Das zurückgegebene Objekt enthält
     * erst <em>nach</em> der Ausführung des Jobs gültige Daten.
     *
     * @return ein Objekt mit den Rückgabedaten und Statusinformationen zu diesem Job
     */
    HBCIJobResult getJobResult();

    /**
     * Kann von der Banking-Anwendung genutzt werden, um einen eigenen Identifier im Job zu speichern, um im spaeteren
     * Verlauf des HBCI-Dialoges (z.Bsp. bei der TAN-Eingabe) einen Bezug zum urspruenglichen Auftrag wiederherstellen zu
     * koennen.
     *
     * @param id optionale ID.
     */
    void setExternalId(String id);

    /**
     * Liefert eine optionalen Identifier, der von der Banking-Anwendung genutzt werden kann, um einen Bezug zum urspruenglichen
     * Auftrag herstellen zu koennen.
     *
     * @return der Identifier.
     */
    String getExternalId();

}
