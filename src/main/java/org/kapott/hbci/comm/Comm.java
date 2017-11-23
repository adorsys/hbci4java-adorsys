
/*  $Id: Comm.java,v 1.1 2011/05/04 22:37:51 willuhn Exp $

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

package org.kapott.hbci.comm;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.CanNotParseMessageException;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.ParseErrorException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.manager.IHandlerData;
import org.kapott.hbci.manager.MsgGen;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.protocol.MSG;
import org.kapott.hbci.rewrite.Rewrite;

public abstract class Comm {
    /**
     * Der zu verwendende Zeichensatz.
     */
    public final static String ENCODING = "ISO-8859-1";

    private HBCIPassportInternal passport;

    protected abstract void ping(MSG msg);

    protected abstract StringBuffer pong(MsgGen gen);

    protected abstract void closeConnection();

    public Comm() {
    }

    protected Comm(HBCIPassportInternal passport) {
        this.passport = passport;
    }

    public MSG pingpong(Hashtable<String, Object> kernelData, String msgName, MSG msg) {
        IHandlerData handler = getPassport().getParentHandlerData();
        MsgGen gen = handler.getMsgGen();

        HBCIUtils.log("---------------- request ----------------", HBCIUtils.LOG_DEBUG);
        msg.log(HBCIUtils.LOG_DEBUG);

        // ausgehende nachricht versenden
        getPassport().getCallback().status(getPassport(), HBCICallback.STATUS_MSG_SEND, null);
        getPassport().getCallback().status(getPassport(), HBCICallback.STATUS_MSG_RAW_SEND, msg.toString(0));
        ping(msg);

        // nachricht empfangen
        getPassport().getCallback().status(getPassport(), HBCICallback.STATUS_MSG_RECV, null);
        String st = pong(gen).toString();
        getPassport().getCallback().status(getPassport(), HBCICallback.STATUS_MSG_RAW_RECV, st);

        HBCIUtils.log("---------------- response ----------------", HBCIUtils.LOG_DEBUG);
        String[] split = st.split("'");
        for (int i = 0; i < split.length; i++) {
            HBCIUtils.log(split[i], HBCIUtils.LOG_DEBUG);
        }



        MSG retmsg = null;

        try {
            // erzeugen der liste aller rewriter
            String rewriters_st = getPassport().getProperties().getProperty("kernel.rewriter");
            ArrayList<Rewrite> al = new ArrayList<Rewrite>();
            StringTokenizer tok = new StringTokenizer(rewriters_st, ",");
            while (tok.hasMoreTokens()) {
                String rewriterName = tok.nextToken().trim();
                if (rewriterName.length() != 0) {
                    Class cl = this.getClass().getClassLoader().loadClass("org.kapott.hbci.rewrite.R" +
                            rewriterName);
                    Constructor con = cl.getConstructor((Class[]) null);
                    Rewrite rewriter = (Rewrite) (con.newInstance((Object[]) null));
                    rewriter.setKernelData(kernelData);
                    al.add(rewriter);
                }
            }
            Rewrite[] rewriters = al.toArray(new Rewrite[al.size()]);

            // alle rewriter fÃ¼r verschlÃ¼sselte nachricht durchlaufen
            for (int i = 0; i < rewriters.length; i++) {
                st = rewriters[i].incomingCrypted(st, gen);
            }

            // versuche, nachricht als verschlÃ¼sselte nachricht zu parsen
            getPassport().getCallback().status(getPassport(), HBCICallback.STATUS_MSG_PARSE, "CryptedRes");
            try {
                HBCIUtils.log("trying to parse message as crypted message", HBCIUtils.LOG_DEBUG);
                retmsg = new MSG("CryptedRes", st, st.length(), gen, MSG.DONT_CHECK_SEQ, true);
            } catch (ParseErrorException e) {
                // wenn das schiefgeht...
                HBCIUtils.log("message seems not to be encrypted; tring to parse it as " + msgName + "Res message", HBCIUtils.LOG_DEBUG);

                // alle rewriter durchlaufen, um nachricht evtl. als unverschlÃ¼sselte msg zu parsen
                gen.set("_origSignedMsg", st);
                for (int i = 0; i < rewriters.length; i++) {
                    st = rewriters[i].incomingClearText(st, gen);
                }

                // versuch, nachricht als unverschlÃ¼sselte msg zu parsen
                getPassport().getCallback().status(getPassport(), HBCICallback.STATUS_MSG_PARSE, msgName + "Res");
                retmsg = new MSG(msgName + "Res", st, st.length(), gen, MSG.CHECK_SEQ, true);
            }
        } catch (Exception ex) {
            throw new CanNotParseMessageException(HBCIUtilsInternal.getLocMsg("EXCMSG_CANTPARSE"), st, ex);
        }

        return retmsg;
    }

    public static Comm getInstance(String name, HBCIPassportInternal passport) {
        try {
            Class cl = Class.forName("org.kapott.hbci.comm.Comm" + name);
            Constructor cons = cl.getConstructor(new Class[]{HBCIPassportInternal.class});
            return (Comm) cons.newInstance(new Object[]{passport});
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtilsInternal.getLocMsg("EXCMSG_CANTCREATECOMM", name), e);
        }
    }

    protected HBCIPassportInternal getPassport() {
        return passport;
    }

    public void close() {
        closeConnection();
        getPassport().getCallback().callback(getPassport(), HBCICallback.CLOSE_CONNECTION,
                HBCIUtilsInternal.getLocMsg("CALLB_CLOSE_CONN"), HBCICallback.TYPE_NONE, new StringBuffer());
    }
}
