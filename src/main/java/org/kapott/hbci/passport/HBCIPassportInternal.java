/*  $Id: HBCIPassportInternal.java,v 1.1 2011/05/04 22:37:43 willuhn Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2008  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    HBCI4Java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General License for more details.

    You should have received a copy of the GNU General License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.passport;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.manager.HBCIKey;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.w3c.dom.Document;

import java.util.HashMap;

/**
 * Interface, welches alle Passport-Varianten implementieren müssen.
 * Diese Schnittstelle wird nur intern verwendet. Sie beschreibt alle
 * Methoden, die ein Passport zur Verfügung stellen muss, um von
 * <em>HBCI4Java</em> benutzt werden zu können. Dieses Interface ist
 * nicht zur Anwendung hin sichtbar (deshalb auch "<code>Internal</code>").
 */
public interface HBCIPassportInternal extends HBCIPassport {

    String getSysId();

    void setSysId(String sysid);

    String getSysStatus();

    String getProfileMethod();

    String getProfileVersion();

    boolean needUserSig();

    void setInstSigKey(HBCIKey key);

    void setInstEncKey(HBCIKey key);

    void clearMySigKey();

    void clearMyEncKey();

    void clearMyDigKey();

    void setMyPublicSigKey(HBCIKey key);

    void setMyPrivateSigKey(HBCIKey key);

    void setMyPublicEncKey(HBCIKey key);

    void setMyPrivateEncKey(HBCIKey key);

    void setMyPublicDigKey(HBCIKey key);

    void setMyPrivateDigKey(HBCIKey key);

    String getInstEncKeyName();

    String getInstEncKeyNum();

    String getInstEncKeyVersion();

    String getMySigKeyName();

    String getMySigKeyNum();

    String getMySigKeyVersion();

    String getLang();

    Long getSigId();

    void setSigId(Long sigid);

    String getCryptKeyType();

    String getCryptFunction();

    String getCryptAlg();

    String getCryptMode();

    String getSigFunction();

    String getSigAlg();

    String getSigMode();

    String getHashAlg();

    void setBPD(HashMap<String, String> bpd);

    void setUPD(HashMap<String, String> upd);

    void incSigId();

    HashMap<String, String> getParamSegmentNames();

    HashMap<String, String> getJobRestrictions(String specname);

    HashMap<String, String> getJobRestrictions(String gvname, String version);

    void setPersistentData(String id, Object o);

    Object getPersistentData(String id);

    /* Diese Methode wird nach jeder Dialog-Initialisierung aufgerufen. Ein
     * Passport-Objekt kann den Status der Response mit Hilfe von msgStatus
     * auswerten. Durch Zurückgeben von "true" wird angezeigt, dass eine
     * erneute Dialog-Initialisierung stattfinden sollte (z.B. weil sich grund-
     * legende Zugangsdaten geändert haben, secMechs neu festgelegt wurden o.ä.) */
    boolean postInitResponseHook(HBCIMsgStatus msgStatus);

    /* Gibt zurück, wieviele GV-Segmente in einer Nachricht enthalten sein dürfen.
     * Normalerweise wird das schon durch die BPD bzw. die Job-Params festgelegt,
     * deswegen geben die meisten Passport-Implementierungen hier 0 zurück (also
     * keine weiteren Einschränkungen neben den BPD-Daten). Im Fall von PIN/TAN
     * muss jedoch dafür gesorgt werden, dass tatsächlich nur ein einziges
     * Auftragssegment in einer HBCI-Nachricht steht (weil sonst das "Signieren"
     * mit einer TAN schwierig wird). Deswegen gibt die PIN/TAN-Implementierung
     * dieser Methode 1 zurück.
     * In HBCIDialog.addTask() wird diese Methode aufgerufen, um festzustellen,
     * ob für den hinzuzufügenden Task eine neue Nachricht erzeugt werden muss
     * oder nicht.
     */
    int getMaxGVSegsPerMsg();

    HashMap<String, String> getProperties();

    HBCICallback getCallback();

    String getProxy();

    byte[][] encrypt(byte[] plainString);

    byte[] decrypt(byte[] cryptedkey, byte[] cryptedstring);

    byte[] sign(byte[] hashresult);

    HashMap<String, String> getSupportedLowlevelJobs(Document document);

    HashMap<String, String> getLowlevelJobRestrictions(String gvname, Document document);

    Document getSyntaxDocument();
}
