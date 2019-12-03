
/*  $Id: GVUebSEPA.java,v 1.1 2011/05/04 22:37:54 willuhn Exp $

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
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.manager.LogFilter;

/**
 * Job-Implementierung fuer SEPA-Multi-Ueberweisungen.
 */
public class GVMultiUebSEPA extends GVUebSEPA
{
    /**
     * Liefert den Lowlevel-Namen des Jobs.
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName()
    {
        return "SammelUebSEPA";
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getPainJobName()
     */
    @Override
    public String getPainJobName()
    {
        return "UebSEPA";
    }

    /**
     * ct.
     * @param handler
     */
    public GVMultiUebSEPA(HBCIHandler handler)
    {
        this(handler, getLowlevelName());
    }

    /**
     * ct.
     * @param handler
     * @param name
     */
    public GVMultiUebSEPA(HBCIHandler handler, String name)
    {
        this(handler, name, new HBCIJobResultImpl());
    }

    /**
     * ct.
     * @param handler
     * @param name
     * @param jobResult
     */
    public GVMultiUebSEPA(HBCIHandler handler, String name, HBCIJobResultImpl jobResult)
    {
        super(handler, name, jobResult);

        addConstraint("batchbook", "sepa.batchbook", "", LogFilter.FILTER_NONE);
        addConstraint("Total.value", "Total.value", null, LogFilter.FILTER_MOST);
        addConstraint("Total.curr", "Total.curr", null, LogFilter.FILTER_NONE);
    }

    /**
     * @see org.kapott.hbci.GV.HBCIJobImpl#getChallengeParam(java.lang.String)
     */
    @Override
    public String getChallengeParam(String path)
    {
        if (path.equals("sepa.btg.value")) {
            return getLowlevelParam(getName()+".Total.value");
        }
        else if (path.equals("sepa.btg.curr")) {
            return getLowlevelParam(getName()+".Total.curr");
        }
        return null;
    }

    @Override protected void createSEPAFromParams()
    {
        super.createSEPAFromParams();
        setParam("Total", SepaUtil.sumBtgValueObject(sepaParams));
    }
}
