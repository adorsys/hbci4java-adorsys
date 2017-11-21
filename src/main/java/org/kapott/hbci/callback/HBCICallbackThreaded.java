
/*  $Id: HBCICallbackThreaded.java,v 1.1 2011/05/04 22:37:51 willuhn Exp $

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

package org.kapott.hbci.callback;

import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;

import org.kapott.hbci.GV.GVTAN2Step;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.passport.HBCIPassportInternal;

/** <p>Wrapper-Klasse, die bei Verwendung des threaded-callback-Mechanismus benÃ¶tigt
 * wird. Sollen Callbacks synchron behandelt werden, so ist es zur Aktivierung
 * dieses Mechanismus' notwendig, das "normale" Callback-Objekt in einer Instanz
 * dieser Klasse zu kapseln. Diese Klasse sorgt dafÃ¼r, dass "normale" (asynchrone)
 * Callbacks wie gewohnt von dem "normalen" Callback-Objekt behandelt werden.
 * Bei synchron zu behandelnden Callbacks sorgt diese Callback-Implementierung
 * dafÃ¼r, dass {@link org.kapott.hbci.manager.HBCIHandler#executeThreaded() hbci.executeThreaded()}
 * terminiert.</p>
 * <p>Mehr Informationen sind in der Datei <code>README.ThreadedCallbacks</code>
 * sowie unter {@link HBCICallback#useThreadedCallback(HBCIPassport, int, String, int, StringBuffer)}
 * zu finden.</p> */
public final class HBCICallbackThreaded 
    extends AbstractHBCICallback
{
    private HBCICallback realCallback;
    
    /** Erzeugt eine Instanz dieser Klasse. Ein HBCIThreadedCallback-Objekt muss
     * bei {@link org.kapott.hbci.manager.HBCIUtils#init(Properties, HBCICallback)}
     * als Callback-Objekt Ã¼bergeben werden, wenn der threaded-callback-Mechanismus
     * benutzt werden soll.
     * @param realCallback eine Instanz einer "normalen" Callback-Klasse. Alle asynchron
     * zu behandelnden Callbacks (der Normalfall) werden an dieses Objekt weitergegeben
     * - nur die synchron zu behandelnden Callbacks werden anders behandelt. */
    public HBCICallbackThreaded(HBCICallback realCallback)
    {
        this.realCallback=realCallback;
    }
    
    /** Aufruf wird an das "normale" Callback-Objekt weitergereicht. */
    public void log(String msg,int level,Date date,StackTraceElement trace)
    {
        realCallback.log(msg,level,date,trace);
    }

    /** FÃ¼r asynchron zu behandelnde Callbacks wird der Aufruf an das "normale"
     * Callback-Objekt weitergereicht. Synchron zu behandelnde Callbacks werden
     * von dieser Methode behandelt, in dem der entsprechende Aufruf von
     * {@link org.kapott.hbci.manager.HBCIHandler#executeThreaded()} terminiert
     * und Callback-Info-Daten zurÃ¼ckgibt. */
    public void callback(HBCIPassport passport,int reason,String msg,
                         int datatype,StringBuffer retData)
    {
        HBCIUtils.log("hbci thread: threaded callback received", HBCIUtils.LOG_DEBUG);
        realCallback.callback(passport,reason,msg,datatype,retData);
    }

    @Override
    public boolean tanCallback(HBCIPassport passport, GVTAN2Step hktan) {
        return false;
    }

    /** Aufruf wird an das "normale" Callback-Objekt weitergereicht. */
    public void status(HBCIPassport passport,int statusTag,Object[] o)
    {
        // TODO das hier evtl. auch in den threaded-workflow aufnehmen?
        realCallback.status(passport,statusTag,o);
    }

}
