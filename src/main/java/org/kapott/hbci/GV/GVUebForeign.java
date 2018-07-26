/*  $Id: GVUebForeign.java,v 1.1 2011/05/04 22:37:52 willuhn Exp $

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
import org.kapott.hbci.passport.HBCIPassportInternal;

public class GVUebForeign extends AbstractHBCIJob {

    public GVUebForeign(String name, HBCIPassportInternal passport) {
        super(passport, name, new HBCIJobResultImpl(passport));
    }

    public GVUebForeign(HBCIPassportInternal passport) {
        this(getLowlevelName(), passport);

        addConstraint("src.country", "My.KIK.country", "DE");
        addConstraint("src.blz", "My.KIK.blz", null);
        addConstraint("src.number", "My.number", null);
        addConstraint("src.subnumber", "My.subnumber", "");
        addConstraint("src.name", "myname", null);
        addConstraint("dst.country", "Other.KIK.country", "");
        addConstraint("dst.blz", "Other.KIK.blz", "");
        addConstraint("dst.number", "Other.number", "");
        addConstraint("dst.subnumber", "Other.subnumber", "");
        addConstraint("dst.iban", "otheriban", "");
        addConstraint("dst.name", "othername", null);
        addConstraint("dst.kiname", "otherkiname", null);
        addConstraint("btg.value", "BTG.value", null);
        addConstraint("btg.curr", "BTG.curr", null);

        addConstraint("kostentraeger", "kostentraeger", "1");
        addConstraint("usage", "usage", "");
    }

    public static String getLowlevelName() {
        return "UebForeign";
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("src");
    }
}
