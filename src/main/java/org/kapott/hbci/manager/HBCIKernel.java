
/*  $Id: HBCIKernel.java,v 1.1 2011/05/04 22:37:46 willuhn Exp $

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

import java.util.Hashtable;
import java.util.List;

/** HBCI-Kernel fÃ¼r eine bestimmte HBCI-Version. Objekte dieser Klasse 
 * werden intern fÃ¼r die Nachrichtenerzeugung und -analyse verwendet. */
public interface HBCIKernel
{
    /** Gibt die HBCI-Versionsnummer zurÃ¼ck, fÃ¼r die dieses Kernel-Objekt 
     * Nachrichten erzeugen und analysieren kann.
     * @return HBCI-Versionsnummer */
    public String getHBCIVersion();

    /** <p>Gibt die Namen und Versionen aller von <em>HBCI4Java</em> fÃ¼r die
     * aktuelle HBCI-Version (siehe {@link #getHBCIVersion()}) unterstÃ¼tzten 
     * Lowlevel-GeschÃ¤ftsvorfÃ¤lle zurÃ¼ck. Es ist zu beachten, dass ein konkreter
     * HBCI-Zugang i.d.R. nicht alle in dieser Liste aufgefÃ¼hrten 
     * GeschÃ¤ftsvorfÃ¤lle auch tatsÃ¤chlich anbietet (siehe dafÃ¼r
     * {@link HBCIHandler#getSupportedLowlevelJobs()}).</p>
     * <p>Die zurÃ¼ckgegebene Hashtable enthÃ¤lt als Key jeweils einen String mit 
     * dem Bezeichner eines Lowlevel-Jobs, welcher fÃ¼r die Erzeugung eines
     * Lowlevel-Jobs mit {@link HBCIHandler#newLowlevelJob(String)} verwendet
     * werden kann. Der dazugehÃ¶rige Wert ist ein List-Objekt (bestehend aus 
     * Strings), welches alle GV-Versionsnummern enthÃ¤lt, die von 
     * <em>HBCI4Java</em> fÃ¼r diesen GV unterstÃ¼tzt werden.</p>
     * @return Hashtable aller Lowlevel-Jobs, die prinzipiell vom aktuellen
     * Handler-Objekt unterstÃ¼tzt werden. */
    public Hashtable<String, List<String>> getAllLowlevelJobs();

    /** <p>Gibt alle fÃ¼r einen bestimmten Lowlevel-Job mÃ¶glichen Job-Parameter-Namen
     * zurÃ¼ck. Der Ã¼bergebene Job-Name ist einer der von <em>HBCI4Java</em>
     * unterstÃ¼tzten Jobnamen, die Versionsnummer muss eine der fÃ¼r diesen GV
     * unterstÃ¼tzten Versionsnummern sein (siehe {@link #getAllLowlevelJobs()}).
     * Als Ergebnis erhÃ¤lt man eine Liste aller Parameter-Namen, die fÃ¼r einen
     * Lowlevel-Job (siehe {@link HBCIHandler#newLowlevelJob(String)}) gesetzt
     * werden kÃ¶nnen (siehe 
     * {@link org.kapott.hbci.GV.HBCIJob#setParam(String, String)}).</p>
     * <p>Aus der Liste der mÃ¶glichen Parameternamen ist nicht ersichtlich, 
     * welche Parameter zwingend und welche optional sind, bzw. wie oft ein
     * Parameter mindestens oder hÃ¶chstens auftreten darf. FÃ¼r diese Art der
     * Informationen stehen zur Zeit noch keine Methoden bereit.</p>
     * <p>Siehe dazu auch {@link HBCIHandler#getLowlevelJobParameterNames(String)}.</p>
     * @param gvname Name des Lowlevel-Jobs
     * @param version Version des Lowlevel-jobs
     * @return Liste aller Job-Parameter, die beim Erzeugen des angegebenen
     * Lowlevel-Jobs gesetzt werden kÃ¶nnen */
    public List getLowlevelJobParameterNames(String gvname,String version);

    /** <p>Gibt fÃ¼r einen bestimmten Lowlevel-Job die Namen aller
     * mÃ¶glichen Lowlevel-Result-Properties zurÃ¼ck 
     * (siehe {@link org.kapott.hbci.GV_Result.HBCIJobResult#getResultData()}).
     * Der Ã¼bergebene Job-Name ist einer der von <em>HBCI4Java</em>
     * unterstÃ¼tzten Jobnamen, die Versionsnummer muss eine der fÃ¼r diesen GV
     * unterstÃ¼tzten Versionsnummern sein (siehe {@link #getAllLowlevelJobs()}).
     * Als Ergebnis erhÃ¤lt man eine Liste aller Property-Namen, die in den
     * Lowlevel-Ergebnisdaten eines Jobs auftreten kÃ¶nnen.</p>
     * <p>Aus der resultierenden Liste ist nicht ersichtlich, 
     * welche Properties immer zurÃ¼ckgeben werden und welche optional sind, bzw. 
     * wie oft ein bestimmter Wert mindestens oder hÃ¶chstens auftreten kann. 
     * FÃ¼r diese Art der Informationen stehen zur Zeit noch keine Methoden 
     * bereit.</p>
     * <p>Siehe dazu auch {@link HBCIHandler#getLowlevelJobResultNames(String)}.</p>
     * @param gvname Name des Lowlevel-Jobs
     * @param version Version des Lowlevel-jobs
     * @return Liste aller Property-Namen, die in den Lowlevel-Antwortdaten
     * eines Jobs auftreten kÃ¶nnen */
    public List getLowlevelJobResultNames(String gvname,String version);

    /** <p>Gibt fÃ¼r einen bestimmten Lowlevel-Job die Namen aller
     * mÃ¶glichen Job-Restriction-Parameter zurÃ¼ck 
     * (siehe auch {@link org.kapott.hbci.GV.HBCIJob#getJobRestrictions()} und
     * {@link HBCIHandler#getLowlevelJobRestrictions(String)}).
     * Der Ã¼bergebene Job-Name ist einer der von <em>HBCI4Java</em>
     * unterstÃ¼tzten Jobnamen, die Versionsnummer muss eine der fÃ¼r diesen GV
     * unterstÃ¼tzten Versionsnummern sein (siehe {@link #getAllLowlevelJobs()}).
     * Als Ergebnis erhÃ¤lt man eine Liste aller Property-Namen, die in den
     * Job-Restrictions-Daten eines Jobs auftreten kÃ¶nnen.</p>
     * @param gvname Name des Lowlevel-Jobs
     * @param version Version des Lowlevel-jobs
     * @return Liste aller Property-Namen, die in den Job-Restriction-Daten
     * eines Jobs auftreten kÃ¶nnen */
    public List getLowlevelJobRestrictionNames(String gvname,String version);
}