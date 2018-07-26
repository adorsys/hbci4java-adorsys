/*  $Id: GVInfoList.java,v 1.1 2011/05/04 22:37:52 willuhn Exp $

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

package org.kapott.hbci.GV;


import org.kapott.hbci.GV_Result.GVRInfoList;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

public final class GVInfoList extends AbstractHBCIJob {

    public GVInfoList(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRInfoList(passport));

        addConstraint("maxentries", "maxentries", "");
    }

    public static String getLowlevelName() {
        return "InfoList";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        for (int i = 0; ; i++) {
            GVRInfoList.Info entry = new GVRInfoList.Info();
            String header2 = HBCIUtils.withCounter(header + ".InfoInfo", i);

            if (result.get(header2 + ".code") == null)
                break;

            entry.code = result.get(header2 + ".code");
            entry.date = HBCIUtils.string2DateISO(result.get(header2 + ".version"));
            entry.description = result.get(header2 + ".descr");
            entry.format = result.get(header2 + ".format");
            entry.type = result.get(header2 + ".type");

            for (int j = 0; ; j++) {
                String hint = result.get(header2 + HBCIUtils.withCounter(".comment", j));
                if (hint == null)
                    break;
                entry.addComment(hint);
            }

            ((GVRInfoList) (jobResult)).addEntry(entry);
        }
    }
}
