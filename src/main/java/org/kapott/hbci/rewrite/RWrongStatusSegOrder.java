/*  $Id: RWrongStatusSegOrder.java,v 1.1 2011/05/04 22:37:57 willuhn Exp $

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

package org.kapott.hbci.rewrite;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


// dieser Rewriter muss *VOR* "WrongSequenceNumbers" ausgeführt werden,
// weil hierbei u.U. die Segment-Sequenz-Nummern durcheinandergebracht werden
@Slf4j
public class RWrongStatusSegOrder extends Rewrite {

    // Liste mit segmentInfo-Properties aus der Nachricht erzeugen
    private List<HashMap<String, String>> createSegmentListFromMessage(String msg) {
        List<HashMap<String, String>> segmentList = new ArrayList<HashMap<String, String>>();

        boolean quoteNext = false;
        int startPosi = 0;

        for (int i = 0; i < msg.length(); i++) {
            char ch = msg.charAt(i);

            if (!quoteNext && ch == '@') {
                // skip binary values
                int idx = msg.indexOf("@", i + 1);
                String len_st = msg.substring(i + 1, idx);
                i += Integer.parseInt(len_st) + 1 + len_st.length();
            } else if (!quoteNext && ch == '\'') {
                // segment-ende gefunden
                HashMap<String, String> segmentInfo = new HashMap<>();
                segmentInfo.put("code", msg.substring(startPosi, msg.indexOf(":", startPosi)));
                segmentInfo.put("start", Integer.toString(startPosi));
                segmentInfo.put("length", Integer.toString(i - startPosi + 1));

                segmentList.add(segmentInfo);
                startPosi = i + 1;
            }
            quoteNext = !quoteNext && ch == '?';
        }

        return segmentList;
    }

    @Override
    public String incomingClearText(String st) {
        List<HashMap<String, String>> segmentList = createSegmentListFromMessage(st);

        List<HashMap<String, String>> headerList = new ArrayList<>();
        List<HashMap<String, String>> HIRMGList = new ArrayList<>();
        List<HashMap<String, String>> HIRMSList = new ArrayList<>();
        List<HashMap<String, String>> dataList = new ArrayList<>();

        boolean inHeader = true;
        boolean inGlob = false;
        boolean inSeg = false;
        boolean inData = false;
        boolean errorOccured = false;

        // alle segmente aus der nachricht durchlaufen und der richtigen liste
        // zuordnen (header, globstatus, segstatus, rest)
        for (Iterator<HashMap<String, String>> i = segmentList.iterator(); i.hasNext(); ) {
            HashMap<String, String> segmentInfo = i.next();
            String segmentCode = segmentInfo.get("code");

            if (segmentCode.equals("HNHBK") || segmentCode.equals("HNSHK")) {
                // HNHBK und HNSHK gehören in den header-bereich
                headerList.add(segmentInfo);

                if (!inHeader) {
                    log.warn("RWrongStatusSegOrder: found segment " + segmentCode + " at invalid position");
                    errorOccured = true;
                }

            } else if (segmentCode.equals("HIRMG")) {
                // anschliessend muss ein HIRMG folgen
                HIRMGList.add(segmentInfo);

                if (inHeader) {
                    inHeader = false;
                    inGlob = true;
                }
                if (!inGlob) {
                    log.warn("RWrongStatusSegOrder: found segment " + segmentCode + " at invalid position");
                    errorOccured = true;
                }

            } else if (segmentCode.equals("HIRMS")) {
                // nach HIRMG folgen 0-n HIRMS
                HIRMSList.add(segmentInfo);

                if (inGlob) {
                    inGlob = false;
                    inSeg = true;
                }
                if (!inSeg) {
                    log.warn("RWrongStatusSegOrder: found segment " + segmentCode + " at invalid position");
                    errorOccured = true;
                }

            } else {
                // nach den status-segmenten folgen die datensegmente
                dataList.add(segmentInfo);

                if (inGlob || inSeg) {
                    inGlob = false;
                    inSeg = false;
                    inData = true;
                }
                if (!inData) {
                    log.warn("RWrongStatusSegOrder: found segment " + segmentCode + " at invalid position");
                    errorOccured = true;
                }
            }
        }

        StringBuffer new_msg = new StringBuffer();
        if (errorOccured) {
            // nachricht mit den richtig sortierten segmenten wieder
            // zusammensetzen
            int counter = 1;

            // alle segmente aus dem header
            new_msg.append(getDataForSegmentList(st, headerList, counter));
            counter += headerList.size();

            // HIRMG-segment
            new_msg.append(getDataForSegmentList(st, HIRMGList, counter));
            counter += HIRMGList.size();

            // HIRMS-segmente
            new_msg.append(getDataForSegmentList(st, HIRMSList, counter));
            counter += HIRMSList.size();

            // restliche daten-segmente
            new_msg.append(getDataForSegmentList(st, dataList, counter));

            log.debug("RWrongStatusSegOrder: new message after reordering: " + new_msg);
        } else {
            // kein fehler aufgetreten, also originale nachricht unverändert zurückgeben
            new_msg.append(st);
        }

        return new_msg.toString();
    }

    private String getDataForSegmentList(String origMsg, List<HashMap<String, String>> list, int counter) {
        StringBuffer data = new StringBuffer();

        for (Iterator<HashMap<String, String>> i = list.iterator(); i.hasNext(); ) {
            HashMap<String, String> segmentInfo = i.next();
            int start = Integer.parseInt(segmentInfo.get("start"));
            int len = Integer.parseInt(segmentInfo.get("length"));

            // segment aus originalnachricht extrahieren
            StringBuffer segmentData = new StringBuffer(origMsg.substring(start, start + len));

            // TODO: hier noch die segnum korrigieren (-->counter)
            // korrektur wird nun doch nicht hier vorgenommen, sondern statt
            // muss einfach der Rewriter "WrongSequenceNumbers" nach diesem
            // Rewriter angeordnet werden

            // segmentdaten hinten anhängen
            data.append(segmentData.toString());

            counter++;
        }

        return data.toString();
    }
}
