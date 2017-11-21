
/*  $Id: HBCIKey.java,v 1.1 2011/05/04 22:37:46 willuhn Exp $

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

package org.kapott.hbci.manager;

import java.io.Serializable;
import java.security.Key;

/** Diese Klasse reprÃ¤sentiert einen von <em>HBCI4Java</em>verwendeten SchlÃ¼ssel.
    Ein solcher HBCI-SchlÃ¼ssel besteht aus
    administrativen Daten zu diesem SchlÃ¼ssel (Besitzer, Version) sowie den eigentlichen
    kryptographischen Daten. Bei Verwendung von asymmetrischen Sicherheitsverfahren
    (RDH) werden sowohl fÃ¼r den Ã¶ffentlichen als auch fÃ¼r den privaten SchlÃ¼sselteil
    intern je ein <code>HBCIKey</code>-Objekt verwendet!
    Bei einigen Sicherheitsverfahren (DDV, PinTan) werden
    die kryptografischen Daten nicht in diesem Objekt gespeichert, sondern nur die
    administrativen.*/
public final class HBCIKey
     implements Serializable
{
    private static final long serialVersionUID =1L;
    
    /** LÃ¤ndercode des SchlÃ¼sselbesitzers */
    public String country;
    /** Bankleitzahl des SchlÃ¼sselbesitzers */
    public String blz;
    /** Nutzerkennung des SchlÃ¼sselbesitzers. Wenn der SchlÃ¼ssel
        einem "richtigen" Nutzer gehÃ¶rt, so wird hier seine HBCI-Userkennung eingestellt;
        gehÃ¶rt der SchlÃ¼ssel der Bank, so steht hier eine bankinterne
        ID (u.U. die Bankleitzahl o.Ã¤.) */
    public String userid;
    /** SchlÃ¼sselnummer */
    public String num;
    /** SchlÃ¼sselversion */
    public String version;
    /** kryptographische SchlÃ¼sseldaten (kann <code>null</code> sein)*/
    public Key key;

    /** Neues <code>HBCIKey</code>-Objekt erzeugen */
    public HBCIKey()
    {
        // empty constructor
    }

    public HBCIKey(String country, String blz, String userid, String num, String version, Key key)
    {
        this.country = country;
        this.blz = blz;
        this.userid = userid;
        this.num = num;
        this.version = version;
        this.key = key;
    }
    
    @Override
    public String toString()
    {
        StringBuffer ret=new StringBuffer();
        
        ret.append("country="+this.country);
        ret.append(", blz="+this.blz);
        ret.append(", userid="+this.userid);
        ret.append(", num="+this.num);
        ret.append(", version="+this.version);
        ret.append(", key="+this.key);
        
        return ret.toString();
    }
}
