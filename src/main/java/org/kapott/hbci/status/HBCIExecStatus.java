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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.manager.HBCIUtils;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class HBCIExecStatus {

    @Getter
    private final List<HBCIMsgStatus> msgStatusList;
    @Getter
    private List<Exception> exceptions;

    public void addException(Exception e) {
        if (exceptions == null) {
            exceptions = new ArrayList<>();
        }
        exceptions.add(e);
    }

    public boolean hasMessage(String messageCode) {
        return msgStatusList.stream()
            .anyMatch(hbciMsgStatus -> hbciMsgStatus.globStatus.getRetVals().stream()
                .anyMatch(hbciRetVal -> hbciRetVal.code.equals(messageCode)));
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

        if (msgStatusList != null) {
            msgStatusList.forEach(hbciMsgStatus -> ret.addAll(hbciMsgStatus.getErrorList()));

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

        if (msgStatusList != null) {
            for (int i = 0; i < msgStatusList.size(); i++) {
                ret.append(HBCIUtils.getLocMsg("STAT_MSG")).append(" #").append(i + 1).append(":").append(System.getProperty("line.separator"));
                ret.append(msgStatusList.get(i).toString());
                ret.append(System.getProperty("line.separator"));
            }
        }

        return ret.toString().trim();
    }

    public boolean isOK() {
        if (exceptions != null && !exceptions.isEmpty()) {
            return false;
        }

        boolean ret = true;

        if (msgStatusList != null) {
            for (HBCIMsgStatus hbciMsgStatus : msgStatusList) {
                ret &= hbciMsgStatus.isOK();
            }
        }

        return ret;
    }
}
