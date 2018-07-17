
/*  $Id: HBCIKernel.java,v 1.1 2011/05/04 22:37:46 willuhn Exp $

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

package org.kapott.hbci.manager;

import java.util.List;
import java.util.Properties;

/**
 * HBCI-Kernel für eine bestimmte HBCI-Version. Objekte dieser Klasse
 * werden intern für die Nachrichtenerzeugung und -analyse verwendet.
 */
interface HBCIKernel {
    /**
     * Gibt die HBCI-Versionsnummer zurück, für die dieses Kernel-Objekt
     * Nachrichten erzeugen und analysieren kann.
     *
     * @return HBCI-Versionsnummer
     */
    String getHBCIVersion();

    /**
     * <p>Gibt alle für einen bestimmten Lowlevel-Job mÃ¶glichen Job-Parameter-Namen
     * zurück. Der übergebene Job-Name ist einer der von <em>HBCI4Java</em>
     * unterstützten Jobnamen, die Versionsnummer muss eine der für diesen GV
     * unterstützten Versionsnummern sein.
     * Als Ergebnis erhält man eine Liste aller Parameter-Namen, die für einen
     * Lowlevel-Job (siehe {@link HBCIHandler#newLowlevelJob(String)}) gesetzt
     * werden kÃ¶nnen (siehe
     * {@link org.kapott.hbci.GV.HBCIJob#setParam(String, String)}).</p>
     * <p>Aus der Liste der mÃ¶glichen Parameternamen ist nicht ersichtlich,
     * welche Parameter zwingend und welche optional sind, bzw. wie oft ein
     * Parameter mindestens oder hÃ¶chstens auftreten darf. Für diese Art der
     * Informationen stehen zur Zeit noch keine Methoden bereit.</p>
     * <p>Siehe dazu auch {@link HBCIHandler#getLowlevelJobParameterNames(String)}.</p>
     *
     * @param gvname  Name des Lowlevel-Jobs
     * @param version Version des Lowlevel-jobs
     * @return Liste aller Job-Parameter, die beim Erzeugen des angegebenen
     * Lowlevel-Jobs gesetzt werden kÃ¶nnen
     */
    List getLowlevelJobParameterNames(String gvname, String version);

    /**
     * <p>Gibt für einen bestimmten Lowlevel-Job die Namen aller
     * mÃ¶glichen Lowlevel-Result-Properties zurück
     * (siehe {@link org.kapott.hbci.GV_Result.HBCIJobResult#getResultData()}).
     * Der übergebene Job-Name ist einer der von <em>HBCI4Java</em>
     * unterstützten Jobnamen, die Versionsnummer muss eine der für diesen GV
     * unterstützten Versionsnummern sein.
     * Als Ergebnis erhält man eine Liste aller Property-Namen, die in den
     * Lowlevel-Ergebnisdaten eines Jobs auftreten kÃ¶nnen.</p>
     * <p>Aus der resultierenden Liste ist nicht ersichtlich,
     * welche Properties immer zurückgeben werden und welche optional sind, bzw.
     * wie oft ein bestimmter Wert mindestens oder hÃ¶chstens auftreten kann.
     * Für diese Art der Informationen stehen zur Zeit noch keine Methoden
     * bereit.</p>
     * <p>Siehe dazu auch {@link HBCIHandler#getLowlevelJobResultNames(String)}.</p>
     *
     * @param gvname  Name des Lowlevel-Jobs
     * @param version Version des Lowlevel-jobs
     * @return Liste aller Property-Namen, die in den Lowlevel-Antwortdaten
     * eines Jobs auftreten kÃ¶nnen
     */
    List getLowlevelJobResultNames(String gvname, String version);

    /**
     * <p>Gibt für einen bestimmten Lowlevel-Job die Namen aller
     * mÃ¶glichen Job-Restriction-Parameter zurück
     * (siehe auch {@link org.kapott.hbci.GV.HBCIJob#getJobRestrictions()} und
     * {@link HBCIHandler#getLowlevelJobRestrictions(String)}).
     * Der übergebene Job-Name ist einer der von <em>HBCI4Java</em>
     * unterstützten Jobnamen, die Versionsnummer muss eine der für diesen GV
     * unterstützten Versionsnummern sein.
     * Als Ergebnis erhält man eine Liste aller Property-Namen, die in den
     * Job-Restrictions-Daten eines Jobs auftreten kÃ¶nnen.</p>
     *
     * @param gvname  Name des Lowlevel-Jobs
     * @param version Version des Lowlevel-jobs
     * @return Liste aller Property-Namen, die in den Job-Restriction-Daten
     * eines Jobs auftreten kÃ¶nnen
     */
    List getLowlevelJobRestrictionNames(String gvname, String version);

}