/*  $Id: HBCIExecStatus.java,v 1.1 2011/05/04 22:38:02 willuhn Exp $

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

package org.kapott.hbci.status;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.manager.HBCIUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Statusinformationen über alle ausgeführten Dialoge. Die Methode
 * {@link org.kapott.hbci.manager.HBCIHandler#execute()} gibt nach der Ausführung
 * aller HBCI-Dialoge ein Objekt dieser Klasse zurück. Dieses Objekt enthält
 * Informationen darüber, für welche Kunden-IDs tatsächlich HBCI-Dialoge geführt
 * wurden. Für jeden geführten HBCI-Dialog existiert dann ein
 * {@link HBCIDialogStatus}-Objekt, welches Informationen zu dem jeweiligen
 * Dialog enthält.
 */
@Slf4j
public class HBCIExecStatus {

    private HBCIDialogStatus dialogStatus;
    private ArrayList<Exception> exceptions;

    public HBCIExecStatus() {
        exceptions = new ArrayList<>();
    }

    /**
     * Wird von der <em>HBCI4Java</em>-Dialog-Engine aufgerufen
     */
    public void setDialogStatus(HBCIDialogStatus status) {
        this.dialogStatus = status;
    }

    /**
     * Wird von der <em>HBCI4Java</em>-Dialog-Engine aufgerufen
     */
    public void addException(Exception e) {
        if (exceptions == null) {
            exceptions = new ArrayList<>();
        }
        exceptions.add(e);
        log.error(e.getMessage(), e);
    }

    /**
     * Gibt eine Liste von Status-Informationen für jeden ausgeführten HBCI-Dialog
     * zurück. Diese Methode ist insofern von eingeschränkter Bedeutung, weil
     * es nicht möglich ist, einem {@link HBCIDialogStatus}-Objekt dieser Liste
     * die Kunden-ID zuzuordnen, unter der der jeweilige Dialog geführt wurde.
     * Dazu müssen die Methoden und {@link #getDialogStatus()}
     * verwendet werden.
     *
     * @return Menge aller gespeicherten HBCI-Dialog-Status-Informationen
     * @deprecated sinnlos
     */
    public HBCIDialogStatus getDialogStatusList() {
        return dialogStatus;
    }

    /**
     * {@link HBCIDialogStatus} für den Dialog einer bestimmten Kunden-ID zurückgeben.
     *
     * @return Status-Objekt für den ausgewählten Dialog
     */
    public HBCIDialogStatus getDialogStatus() {
        return dialogStatus;
    }

    /**
     * Exceptions zurückgeben, die beim Ausführen eines bestimmten Dialoges aufgetreten sind.
     * Dabei werden nur die Exceptions zurückgegeben, die Fehler in der Verwaltung der
     * Kunden-IDs/Dialoge betreffen. Alle Exceptions, die während der eigentlichen
     * Dialogausführung evtl. aufgetreten sind, sind im entsprechenden
     * {@link HBCIDialogStatus}-Objekt des jeweiligen Dialoges enthalten.
     *
     * @return Liste mit aufgetretenen Exceptions
     */
    public List<Exception> getExceptions() {
        return exceptions;
    }

    /**
     * Gibt einen String zurück, der alle Fehlermeldungen aller ausgeführten
     * Dialog enthält.
     *
     * @return String mit allen aufgetretenen Fehlermeldungen
     */
    public String getErrorString() {
        StringBuffer ret = new StringBuffer();
        String linesep = System.getProperty("line.separator");

        List<Exception> exc = getExceptions();
        if (exc != null && exc.size() != 0) {

            // ret.append(HBCIUtils.getLocMsg("STAT_EXCEPTIONS")).append(":").append(linesep);
            for (Iterator<Exception> j = exc.iterator(); j.hasNext(); ) {
                ret.append(HBCIUtils.exception2StringShort(j.next()));
                ret.append(linesep);
            }
        }

        HBCIDialogStatus status = getDialogStatus();
        if (status != null) {
            String errMsg = status.getErrorString();
            if (errMsg.length() != 0) {
                ret.append(errMsg + linesep);
            }
        }

        return ret.toString().trim();
    }

    public String toString() {
        StringBuffer ret = new StringBuffer();
        String linesep = System.getProperty("line.separator");

        List<Exception> exc = getExceptions();
        if (exc != null) {
            for (Iterator<Exception> j = exc.iterator(); j.hasNext(); ) {
                ret.append(HBCIUtils.exception2StringShort(j.next()));
                ret.append(linesep);
            }
        }

        HBCIDialogStatus status = getDialogStatus();
        if (status != null) {
            ret.append(status.toString() + linesep);
        }

        return ret.toString().trim();
    }

    public boolean isOK() {
        boolean ok = true;
        List<Exception> exc = getExceptions();
        HBCIDialogStatus status = getDialogStatus();

        ok &= (exc == null || exc.size() == 0);
        ok &= (status != null && status.isOK());

        return ok;
    }
}
