
/*  $Id: AbstractHBCIPassport.java,v 1.4 2012/03/13 22:07:43 willuhn Exp $

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

package org.kapott.hbci.passport;

import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidArgumentException;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.LogFilter;
import org.kapott.hbci.manager.MsgGen;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Limit;
import org.kapott.hbci.structures.Value;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * <p>Diese Klasse stellt die Basisklasse für alle "echten" Passport-Implementationen
 * dar. Hier werden bereits einige Methoden implementiert sowie einige
 * zusätzliche Hilfsmethoden zur Verfügung gestellt.</p><p>
 * Aus einer HBCI-Anwendung heraus ist hier nur eine einzige Methode interessant,
 * um eine Instanz eines bestimmtes Passports zu erzeugen</p>
 */
public abstract class AbstractHBCIPassport implements HBCIPassportInternal, Serializable {

    private Properties bpd;
    private Properties upd;
    private String hbciversion;
    private String country;
    private String blz;
    private String host;
    private Integer port;
    private String userid;
    private String customerid;
    private String sysid;
    private Long sigid;
    private String cid;
    private Hashtable<String, Object> persistentData = new Hashtable<>();

    protected HBCICallback callback;
    protected Properties properties;

    public AbstractHBCIPassport(Properties properties, HBCICallback callback) {
        this.callback = callback;
        this.properties = properties;
    }

    public final Properties getBPD() {
        return bpd;
    }

    public final void setHBCIVersion(String hbciversion) {
        this.hbciversion = hbciversion;
    }

    public final String getHBCIVersion() {
        return (hbciversion != null) ? hbciversion : "";
    }

    public final Properties getUPD() {
        return upd;
    }

    public final String getBLZ() {
        return blz;
    }

    public final String getCountry() {
        return country;
    }

    public final Konto[] getAccounts() {
        ArrayList<Konto> ret = new ArrayList<Konto>();

        if (upd != null) {
            for (int i = 0; ; i++) {
                String header = HBCIUtils.withCounter("KInfo", i);
                String number = upd.getProperty(header + ".KTV.number");

                if (number == null)
                    break;

                Konto entry = new Konto();
                entry.blz = upd.getProperty(header + ".KTV.KIK.blz");
                entry.country = upd.getProperty(header + ".KTV.KIK.country");
                entry.number = number;
                entry.subnumber = upd.getProperty(header + ".KTV.subnumber");
                entry.curr = upd.getProperty(header + ".cur");
                entry.type = upd.getProperty(header + ".konto");
                entry.customerid = upd.getProperty(header + ".customerid");
                entry.name = upd.getProperty(header + ".name1");
                entry.name2 = upd.getProperty(header + ".name2");
                entry.bic = upd.getProperty(header + ".KTV.bic");
                entry.iban = upd.getProperty(header + ".KTV.iban");
                entry.acctype = upd.getProperty(header + ".acctype");

                String st;
                if ((st = upd.getProperty(header + ".KLimit.limittype")) != null) {
                    Limit limit = new Limit();
                    limit.type = st.charAt(0);
                    limit.value = new Value(upd.getProperty(header + ".KLimit.BTG.value"),
                            upd.getProperty(header + ".KLimit.BTG.curr"));
                    if ((st = upd.getProperty(header + ".KLimit.limitdays")) != null)
                        limit.days = Integer.parseInt(st);
                }

                // allowedGVs
                ArrayList<String> codes = new ArrayList<String>();
                for (int j = 0; ; j++) {
                    String gvHeader = HBCIUtils.withCounter(header + ".AllowedGV", j);
                    String code = upd.getProperty(gvHeader + ".code");
                    if (code == null) break;
                    codes.add(code);
                }
                if (!codes.isEmpty()) entry.allowedGVs = codes;

                ret.add(entry);
            }
        }

        return ret.toArray(new Konto[ret.size()]);
    }

    public final void fillAccountInfo(Konto account) {
        String number = HBCIUtils.stripLeadingZeroes(account.number);
        String iban = HBCIUtils.stripLeadingZeroes(account.iban);
        boolean haveNumber = (number != null && number.length() != 0);
        boolean haveIBAN = (iban != null && iban.length() != 0);

        Konto[] accounts = getAccounts();

        for (int i = 0; i < accounts.length; i++) {
            String temp_number = HBCIUtils.stripLeadingZeroes(accounts[i].number);
            String temp_iban = HBCIUtils.stripLeadingZeroes(accounts[i].iban);

            if (haveNumber && number.equals(temp_number) ||
                    haveIBAN && iban.equals(temp_iban)) {
                account.blz = accounts[i].blz;
                account.country = accounts[i].country;
                account.number = accounts[i].number;
                account.subnumber = accounts[i].subnumber;
                account.type = accounts[i].type;
                account.curr = accounts[i].curr;
                account.customerid = accounts[i].customerid;
                account.name = accounts[i].name;
                account.bic = accounts[i].bic;
                account.iban = accounts[i].iban;
                account.acctype = accounts[i].acctype;
                break;
            }
        }
    }

    public final Konto getAccount(String number) {
        Konto ret = new Konto();
        ret.number = number;
        fillAccountInfo(ret);

        if (ret.blz == null) {
            // es wurde kein Konto-Objekt in getAccounts() gefunden
            ret.blz = getBLZ();
            ret.country = getCountry();
            ret.customerid = getCustomerId();
            // TODO: very dirty!
            ret.name = getCustomerId();

            // an dieser Stelle sind jetzt alle Werte gefüllt, die teilweise
            // zwingend benÃ¶tigt werden
        }

        return ret;
    }

    public String getHost() {
        return host;
    }

    public final Integer getPort() {
        return (port != null) ? port : new Integer(0);
    }

    public String getUserId() {
        return userid;
    }

    public String getCustomerId() {
        return (customerid != null && customerid.length() != 0) ? customerid : getUserId();
    }

    public String getSysId() {
        return (sysid != null && sysid.length() != 0) ? sysid : "0";
    }

    public final String getCID() {
        return cid != null ? cid : "";
    }

    public final void clearMySigKey() {
        setMyPublicSigKey(null);
        setMyPrivateSigKey(null);
    }

    public final void clearMyEncKey() {
        setMyPublicEncKey(null);
        setMyPrivateEncKey(null);
    }

    public final void clearMyDigKey() {
        setMyPublicDigKey(null);
        setMyPrivateDigKey(null);
    }

    public final String getBPDVersion() {
        String version = ((bpd != null) ? bpd.getProperty("BPA.version") : null);
        return ((version != null) ? version : "0");
    }

    public final String getUPDVersion() {
        String version = ((upd != null) ? upd.getProperty("UPA.version") : null);
        return ((version != null) ? version : "0");
    }

    public final String getInstName() {
        return (bpd != null) ? bpd.getProperty("BPA.kiname") : null;
    }

    public int getMaxGVperMsg() {
        return (bpd != null) ? Integer.parseInt(bpd.getProperty("BPA.numgva")) : -1;
    }

    public final int getMaxMsgSizeKB() {
        return (bpd != null) ? Integer.parseInt(bpd.getProperty("BPA.maxmsgsize", "0")) : 0;
    }

    public final String[] getSuppVersions() {
        String[] ret = new String[0];

        if (bpd != null) {
            ArrayList<String> temp = new ArrayList<String>();
            String header;
            String value;
            int i = 0;

            while ((header = HBCIUtils.withCounter("BPA.SuppVersions.version", i)) != null &&
                    (value = bpd.getProperty(header)) != null) {
                temp.add(value);
                i++;
            }

            if (temp.size() != 0)
                ret = (temp.toArray(ret));
        }

        return ret;
    }

    public final String getDefaultLang() {
        String value = (bpd != null) ? bpd.getProperty("CommListRes.deflang") : null;
        return (value != null) ? value : "0";
    }

    public final boolean canMixSecMethods() {
        boolean ret = false;

        if (bpd != null) {
            String value = bpd.getProperty("SecMethod.mixing");

            if (value != null && value.equals("J"))
                ret = true;
        }

        return ret;
    }

    public final String getLang() {
        String value = (bpd != null) ? bpd.getProperty("CommListRes.deflang") : null;
        return (value != null) ? value : "0";
    }

    public final Long getSigId() {
        return sigid != null ? sigid : new Long(1);
    }

    public final void clearBPD() {
        setBPD(null);
    }

    public void setBPD(Properties bpd) {
        this.bpd = bpd;
    }

    public final void clearUPD() {
        setUPD(null);
    }

    public final void setUPD(Properties upd) {
        this.upd = upd;
    }

    public final void setCountry(String country) {
        this.country = country;
    }

    public final void setBLZ(String blz) {
        LogFilter.getInstance().addSecretData(blz, "X", LogFilter.FILTER_MOST);
        this.blz = blz;
    }

    public final void setHost(String host) {
        this.host = host;
    }

    public final void setPort(Integer port) {
        this.port = port;
    }

    public final void setUserId(String userid) {
        LogFilter.getInstance().addSecretData(userid, "X", LogFilter.FILTER_IDS);
        this.userid = userid;
    }

    public final void setCustomerId(String customerid) {
        LogFilter.getInstance().addSecretData(customerid, "X", LogFilter.FILTER_IDS);
        this.customerid = customerid;
    }

    public final void setSigId(Long sigid) {
        this.sigid = sigid;
    }

    public final void setSysId(String sysid) {
        this.sysid = sysid;
    }

    public void incSigId() {
        setSigId(new Long(getSigId().longValue() + 1));
    }

    public static HBCIPassport getInstance(HBCICallback callback, Properties properties, String name, Object init) {
        if (name == null) {
            throw new NullPointerException("name of passport implementation must not be null");
        }

        String className = "org.kapott.hbci.passport.HBCIPassport" + name;
        try {
            if (init == null)
                init = name;

            HBCIUtils.log("creating new instance of a " + name + " passport", HBCIUtils.LOG_DEBUG);
            Class cl = Class.forName(className);
            Constructor con = cl.getConstructor(new Class[]{Properties.class, HBCICallback.class, Object.class});
            HBCIPassport p = (HBCIPassport) (con.newInstance(new Object[]{properties, callback, init}));
            return p;
        } catch (ClassNotFoundException e) {
            throw new InvalidUserDataException("*** No passport implementation '" + name + "' found - there must be a class " + className);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof HBCI_Exception)
                throw (HBCI_Exception) cause;

            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_PASSPORT_INST", name), ite);
        } catch (HBCI_Exception he) {
            throw he;
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_PASSPORT_INST", name), e);
        }
    }

    /**
     * Erzeugt eine Instanz eines HBCI-Passports. Der Typ der erzeugten
     * Passport-Instanz wird hierbei dem Wert des HBCI-Parameters
     * <code>client.passport.default</code> entnommen. Gültige Werte für diesen
     * HBCI-Parameter sind die gleichen wie beim Aufruf der Methode
     *
     * @return Instanz eines HBCI-Passports
     */
    public static HBCIPassport getInstance(HBCICallback callback, Properties properties, Object init) {
        String passportName = properties.getProperty("client.passport.default");
        if (passportName == null)
            throw new InvalidUserDataException(HBCIUtils.getLocMsg("EXCMSG_NODEFPASS"));

        return getInstance(callback, properties, passportName, init);
    }

    public static HBCIPassport getInstance(HBCICallback callback, Properties properties, String name) {
        return getInstance(callback, properties, name, null);
    }

    public static HBCIPassport getInstance(HBCICallback callback, Properties properties) {
        return getInstance(callback, properties, (Object) null);
    }

    public Properties getParamSegmentNames() {
        Properties ret = new Properties();

        for (Enumeration e = bpd.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();

            if (key.startsWith("Params") &&
                    key.endsWith(".SegHead.code")) {
                int dotPos = key.indexOf('.');
                int dotPos2 = key.indexOf('.', dotPos + 1);

                String gvname = key.substring(dotPos + 1, dotPos2);
                int len = gvname.length();
                int versionPos = -1;

                for (int i = len - 1; i >= 0; i--) {
                    char ch = gvname.charAt(i);
                    if (!(ch >= '0' && ch <= '9')) {
                        versionPos = i + 1;
                        break;
                    }
                }

                String version = gvname.substring(versionPos);
                if (version.length() != 0) {
                    gvname = gvname.substring(0, versionPos - 3); // remove version and "Par"

                    String knownVersion = (String) ret.get(gvname);

                    if (knownVersion == null ||
                            Integer.parseInt(version) > Integer.parseInt(knownVersion)) {
                        ret.setProperty(gvname, version);
                    }
                }
            }
        }

        return ret;
    }

    public Properties getJobRestrictions(String specname) {
        int versionPos = specname.length() - 1;
        char ch;

        while ((ch = specname.charAt(versionPos)) >= '0' && ch <= '9') {
            versionPos--;
        }

        return getJobRestrictions(
                specname.substring(0, versionPos + 1),
                specname.substring(versionPos + 1));
    }

    public Properties getJobRestrictions(String gvname, String version) {
        Properties result = new Properties();

        String searchstring = gvname + "Par" + version;
        for (Enumeration e = bpd.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();

            if (key.startsWith("Params") &&
                    key.indexOf("." + searchstring + ".Par") != -1) {
                int searchIdx = key.indexOf(searchstring);
                result.setProperty(key.substring(key.indexOf(".",
                        searchIdx + searchstring.length() + 4) + 1),
                        bpd.getProperty(key));
            }
        }

        return result;
    }

    /**
     * <p>Gibt alle Parameter zurück, die für einen Lowlevel-Job gesetzt
     * werden können. Wird ein Job
     * erzeugt, so kann der gleiche <code>gvname</code> als Argument dieser
     * Methode verwendet werden, um eine Liste aller Parameter zu erhalten, die
     * für diesen Job durch Aufrufe der Methode
     * {@link org.kapott.hbci.GV.AbstractHBCIJob#setParam(String, String)}
     * gesetzt werden können bzw. müssen.</p>
     * <p>Aus der zurückgegebenen Liste ist nicht ersichtlich, ob ein bestimmter
     * Parameter optional ist oder gesetzt werden <em>muss</em>. Das kann aber
     * durch Benutzen des Tools {@link org.kapott.hbci.tools.ShowLowlevelGVs}
     * ermittelt werden.</p>
     * <p>Jeder Eintrag der zurückgegebenen Liste enthält einen String, welcher als
     * erster Parameter für den Aufruf von <code>AbstractHBCIJob.setParam()</code> benutzt
     * werden kann. </p>
     * <p>Zur Beschreibung von High- und Lowlevel-Jobs siehe auch die Dokumentation
     * im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @param gvname der Lowlevel-Jobname, für den eine Liste der Job-Parameter
     *               ermittelt werden soll
     * @return eine Liste aller Parameter-Bezeichnungen, die in der Methode
     * {@link org.kapott.hbci.GV.AbstractHBCIJob#setParam(String, String)}
     * benutzt werden können
     */
    public List<String> getLowlevelJobParameterNames(String gvname, MsgGen msgGen) {
        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getSupportedLowlevelJobs(msgGen).getProperty(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return msgGen.getGVParameterNames(gvname, version);
    }

    /**
     * <p>Gibt eine Liste mit Strings zurück, welche Bezeichnungen für die einzelnen Rückgabedaten
     * eines Lowlevel-Jobs darstellen. Jedem {@link org.kapott.hbci.GV.AbstractHBCIJob} ist ein
     * Result-Objekt zugeordnet, welches die Rückgabedaten und Statusinformationen zu dem jeweiligen
     * Job enthält (kann mit {@link org.kapott.hbci.GV.AbstractHBCIJob#getJobResult()}
     * ermittelt werden). Bei den meisten Highlevel-Jobs handelt es sich dabei um bereits aufbereitete
     * Daten (Kontoauszüge werden z.B. nicht in dem ursprünglichen SWIFT-Format zurückgegeben, sondern
     * bereits als fertig geparste Buchungseinträge).</p>
     * <p>Bei Lowlevel-Jobs gibt es diese Aufbereitung der Daten nicht. Statt dessen müssen die Daten
     * manuell aus der Antwortnachricht extrahiert und interpretiert werden. Die einzelnen Datenelemente
     * der Antwortnachricht werden in einem Properties-Objekt bereitgestellt. Jeder Eintrag
     * darin enthält den Namen und den Wert eines Datenelementes aus der Antwortnachricht.</p>
     * <p>Die Methode <code>getLowlevelJobResultNames()</code> gibt nun alle gültigen Namen zurück,
     * für welche in dem Result-Objekt Daten gespeichert sein können. Ob für ein Datenelement tatsächlich
     * ein Wert in dem Result-Objekt existiert, wird damit nicht bestimmt, da einzelne Datenelemente
     * optional sind.</p>
     * <p>Mit dem Tool {@link org.kapott.hbci.tools.ShowLowlevelGVRs} kann offline eine
     * Liste aller Job-Result-Datenelemente erzeugt werden.</p>
     * <p>Zur Beschreibung von High- und Lowlevel-Jobs siehe auch die Dokumentation
     * im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @param gvname Lowlevelname des Geschäftsvorfalls, für den die Namen der Rückgabedaten benötigt werden.
     * @return Liste aller möglichen Property-Keys, für die im Result-Objekt eines Lowlevel-Jobs
     * Werte vorhanden sein könnten
     */
    public List<String> getLowlevelJobResultNames(String gvname, MsgGen msgGen) {
        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getSupportedLowlevelJobs(msgGen).getProperty(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return msgGen.getGVResultNames(gvname, version);
    }

    /**
     * <p>Gibt die Namen aller vom aktuellen HBCI-Zugang (d.h. Passport)
     * unterstützten Lowlevel-Jobs zurück.</p>
     * <p>In dem zurückgegebenen Properties-Objekt enthält jeder Eintrag als
     * Key den Lowlevel-Job-Namen; als Value wird die Versionsnummer des
     * jeweiligen Geschäftsvorfalls angegeben, die von <em>HBCI4Java</em> mit dem
     * aktuellen Passport und der aktuell eingestellten HBCI-Version
     * benutzt werden wird.</p>
     * <p><em>(Prinzipiell unterstützt <em>HBCI4Java</em> für jeden
     * Geschäftsvorfall mehrere GV-Versionen. Auch eine Bank bietet i.d.R. für
     * jeden GV mehrere Versionen an. Wird mit <em>HBCI4Java</em> ein HBCI-Job
     * erzeugt, so verwendet <em>HBCI4Java</em> immer automatisch die höchste
     * von der Bank unterstützte GV-Versionsnummer. Diese Information ist
     * für den Anwendungsentwickler kaum von Bedeutung und dient hauptsächlich
     * zu Debugging-Zwecken.)</em></p>
     * <p>Zum Unterschied zwischen High- und Lowlevel-Jobs siehe die
     * Beschreibung im Package <code>org.kapott.hbci.GV</code>.</p>
     *
     * @return Sammlung aller vom aktuellen Passport unterstützten HBCI-
     * Geschäftsvorfallnamen (Lowlevel) mit der jeweils von <em>HBCI4Java</em>
     * verwendeten GV-Versionsnummer.
     */
    public Properties getSupportedLowlevelJobs(MsgGen msgGen) {
        Properties paramSegments = getParamSegmentNames();
        Properties result = new Properties();

        for (Enumeration e = paramSegments.propertyNames(); e.hasMoreElements(); ) {
            String segName = (String) e.nextElement();

            // überprüfen, ob parameter-segment tatsächlich zu einem GV gehört
            // gilt z.b. für "PinTan" nicht
            if (msgGen.getLowlevelGVs().containsKey(segName))
                result.put(segName, paramSegments.getProperty(segName));
        }

        return result;
    }

    /**
     * <p>Gibt für einen Job alle bekannten Einschränkungen zurück, die bei
     * der Ausführung des jeweiligen Jobs zu beachten sind. Diese Daten werden aus den
     * Bankparameterdaten des aktuellen Passports extrahiert. Sie können von einer HBCI-Anwendung
     * benutzt werden, um gleich entsprechende Restriktionen bei der Eingabe von
     * Geschäftsvorfalldaten zu erzwingen (z.B. die maximale Anzahl von Verwendungszweckzeilen,
     * ob das Ändern von terminierten Überweisungen erlaubt ist usw.).</p>
     * <p>Die einzelnen Einträge des zurückgegebenen Properties-Objektes enthalten als Key die
     * Bezeichnung einer Restriktion (z.B. "<code>maxusage</code>"), als Value wird der
     * entsprechende Wert eingestellt. Die Bedeutung der einzelnen Restriktionen ist zur Zeit
     * nur der HBCI-Spezifikation zu entnehmen. In späteren Programmversionen werden entsprechende
     * Dokumentationen zur internen HBCI-Beschreibung hinzugefügt, so dass dafür eine Abfrageschnittstelle
     * implementiert werden kann.</p>
     * <p>I.d.R. werden mehrere Versionen eines Geschäftsvorfalles von der Bank
     * angeboten. Diese Methode ermittelt automatisch die "richtige" Versionsnummer
     * für die Ermittlung der GV-Restriktionen aus den BPD (und zwar die selbe,
     * die <em>HBCI4Java</em> beim Erzeugen eines Jobs benutzt). </p>
     * <p>Siehe dazu auch {@link AbstractHBCIJob#getJobRestrictions()}.</p>
     *
     * @param gvname Lowlevel-Name des Geschäftsvorfalles, für den die Restriktionen
     *               ermittelt werden sollen
     * @return Properties-Objekt mit den einzelnen Restriktionen
     */
    public Properties getLowlevelJobRestrictions(String gvname, MsgGen msgGen) {
        if (gvname == null || gvname.length() == 0)
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        String version = getSupportedLowlevelJobs(msgGen).getProperty(gvname);
        if (version == null)
            throw new HBCI_Exception("*** lowlevel job " + gvname + " not supported");

        return getJobRestrictions(gvname, version);
    }

    /**
     * @param jobnameHL der Highlevel-Name des Jobs, dessen Unterstützung überprüft werden soll
     * @return <code>true</code>, wenn dieser Job von der Bank unterstützt wird und
     * mit <em>HBCI4Java</em> verwendet werden kann; ansonsten <code>false</code>
     */
    public boolean isSupported(String jobnameHL, MsgGen msgGen) {
        if (jobnameHL == null || jobnameHL.length() == 0)
            throw new InvalidArgumentException(HBCIUtils.getLocMsg("EXCMSG_EMPTY_JOBNAME"));

        try {
            Class cl = Class.forName("org.kapott.hbci.GV.GV" + jobnameHL);
            String lowlevelName = (String) cl.getMethod("getLowlevelName", (Class[]) null).invoke(null, (Object[]) null);
            return getSupportedLowlevelJobs(msgGen).keySet().contains(lowlevelName);
        } catch (Exception e) {
            throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_HANDLER_HLCHECKERR", jobnameHL), e);
        }
    }

    public void setPersistentData(String id, Object o) {
        if (o != null)
            persistentData.put(id, o);
        else
            persistentData.remove(id);
    }

    public void setPersistentData(Hashtable<String, Object> persistentData) {
        this.persistentData = persistentData;
    }

    public Object getPersistentData(String id) {
        return persistentData.get(id);
    }

    public Hashtable<String, Object> getPersistentData() {
        return persistentData;
    }

    public int getMaxGVSegsPerMsg() {
        return 0;
    }

    public Properties getProperties() {
        return properties;
    }

    public HBCICallback getCallback() {
        return callback;
    }


}
