/*  $Id: GVUeb.java,v 1.1 2011/05/04 22:37:54 willuhn Exp $

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

import org.kapott.hbci.GV_Result.HBCIJobResultImpl;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;

import java.util.Map;

public class GVUeb extends AbstractHBCIJob {

    public GVUeb(HBCIPassportInternal passport, String name) {
        super(passport, name, new HBCIJobResultImpl(passport));
    }

    public GVUeb(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());

        addConstraint("src.country", "My.KIK.country", "DE");
        addConstraint("src.blz", "My.KIK.blz", null);
        addConstraint("src.number", "My.number", null);
        addConstraint("src.subnumber", "My.subnumber", "");
        addConstraint("dst.country", "Other.KIK.country", "DE");
        addConstraint("dst.blz", "Other.KIK.blz", null);
        addConstraint("dst.number", "Other.number", null);
        addConstraint("dst.subnumber", "Other.subnumber", "");
        addConstraint("btg.value", "BTG.value", null);
        addConstraint("btg.curr", "BTG.curr", null);
        addConstraint("name", "name", null);

        addConstraint("name2", "name2", "");
        addConstraint("key", "key", "51");

        Map<String, String> parameters = getJobRestrictions();
        int maxusage = Integer.parseInt(parameters.get("maxusage"));

        for (int i = 0; i < maxusage; i++) {
            String name = HBCIUtils.withCounter("usage", i);
            addConstraint(name, "usage." + name, "");
        }
    }

    public static String getLowlevelName() {
        return "Ueb";
    }

    public void setParam(String paramName, String value) {
        Map<String, String> res = getJobRestrictions();

        if (paramName.equals("key")) {
            boolean atLeastOne = false;
            boolean found = false;

            for (int i = 0; ; i++) {
                String st = res.get(HBCIUtils.withCounter("key", i));

                if (st == null)
                    break;

                atLeastOne = true;

                if (st.equals(value)) {
                    found = true;
                    break;
                }
            }

            if (atLeastOne && !found) {
                String msg = HBCIUtils.getLocMsg("EXCMSG_INV_KEY", value);
                throw new InvalidUserDataException(msg);
            }
        }

        super.setParam(paramName, value);
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("src");
        checkAccountCRC("dst");
    }
}
