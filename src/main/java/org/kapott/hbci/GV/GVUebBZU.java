/*  $Id: GVUebBZU.java,v 1.1 2011/05/04 22:37:54 willuhn Exp $

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

import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;

import java.util.HashMap;

public final class GVUebBZU extends GVUeb {

    public GVUebBZU(HBCIPassportInternal passport) {
        super(passport, getLowlevelName());

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
        addConstraint("bzudata", "usage.usage", null);

        addConstraint("name2", "name2", "");
        addConstraint("key", "key", "67");

        HashMap<String, String> parameters = getJobRestrictions();
        int maxusage = Integer.parseInt(parameters.get("maxusage"));

        for (int i = 1; i < maxusage; i++) {
            String name = HBCIUtils.withCounter("usage", i);
            addConstraint(name, "usage." + name, "");
        }
    }

    public static String getLowlevelName() {
        return "Ueb";
    }

    private void checkBZUData(String bzudata) {
        if (bzudata == null)
            throw new InvalidUserDataException(HBCIUtils.getLocMsg("EXCMSG_BZUMISSING"));

        int len = bzudata.length();
        if (len != 13)
            throw new InvalidUserDataException(HBCIUtils.getLocMsg("EXCMSG_INV_BZULEN", Integer.toString(len)));

        byte[] digits = bzudata.getBytes();
        int p = 10;
        int s = 0;
        int mod;

        for (int j = 1; j <= 13; j++) {
            s = (p % 11) + (digits[j - 1] - 0x30);

            if ((mod = (s % 10)) == 0)
                mod = 10;
            p = mod << 1;
        }

        if ((s % 10) != 1) {
            String msg = HBCIUtils.getLocMsg("EXCMSG_BZUERR", bzudata);
            throw new InvalidUserDataException(msg);
        }
    }

    public void setParam(String paramName, String value) {
        if (paramName.equals("bzudata"))
            checkBZUData(value);

        super.setParam(paramName, value);
    }
}
