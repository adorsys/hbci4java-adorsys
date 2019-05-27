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
import java.util.List;

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
    public void addException(Exception e) {
        if (exceptions == null) {
            exceptions = new ArrayList<>();
        }
        exceptions.add(e);
        log.error(e.getMessage(), e);
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
     * Wird von der <em>HBCI4Java</em>-Dialog-Engine aufgerufen
     */
    public void setDialogStatus(HBCIDialogStatus status) {
        this.dialogStatus = status;
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
    public List<String> getErrorMessages() {
        List<String> ret = new ArrayList<>();

        List<Exception> exc = getExceptions();
        if (exc != null && !exc.isEmpty()) {
            for (Exception e : exc) {
                ret.add(HBCIUtils.exception2StringShort(e));
            }
        }

        if (getDialogStatus() != null) {
            ret.addAll(getDialogStatus().getErrorMessages());
        }

        return ret;
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        String linesep = System.getProperty("line.separator");

        List<Exception> exc = getExceptions();
        if (exc != null) {
            for (Exception e : exc) {
                ret.append(HBCIUtils.exception2StringShort(e));
                ret.append(linesep);
            }
        }

        HBCIDialogStatus status = getDialogStatus();
        if (status != null) {
            ret.append(status.toString()).append(linesep);
        }

        return ret.toString().trim();
    }

    public boolean isOK() {
        boolean ok = true;
        List<Exception> exc = getExceptions();
        HBCIDialogStatus status = getDialogStatus();

        ok = (exc == null || exc.isEmpty());
        ok &= (status != null && status.isOK());

        return ok;
    }
}
