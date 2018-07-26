/*  $Id: AbstractPinTanPassport.java,v 1.6 2011/06/06 10:30:31 willuhn Exp $

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

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV.GVTAN2Step;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.FlickerCode;
import org.kapott.hbci.manager.HBCIKey;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HHDVersion;
import org.kapott.hbci.security.Crypt;
import org.kapott.hbci.security.Sig;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.status.HBCIRetVal;

import java.util.*;

@Slf4j
public class PinTanPassport extends AbstractHBCIPassport {

    private String proxy;
    private String proxyuser;
    private String proxypass;

    private boolean verifyTANMode;

    private HashMap<String, HashMap<String, String>> twostepMechanisms = new HashMap<>();
    private List<String> allowedTwostepMechanisms = new ArrayList<>();

    private String currentTANMethod;
    private boolean currentTANMethodWasAutoSelected;

    private String pin;

    public PinTanPassport(String hbciversion, HashMap<String, String> properties, HBCICallback callback) {
        super(hbciversion, properties, callback);
    }

    /**
     * @see org.kapott.hbci.passport.HBCIPassportInternal#sign(byte[])
     */
    @Override
    public byte[] sign(byte[] data) {
        try {
            // TODO: wenn die eingegebene PIN falsch war, muss die irgendwie
            // resettet werden, damit wieder danach gefragt wird
            if (getPIN() == null) {
                StringBuffer s = new StringBuffer();

                getCallback().callback(
                        HBCICallback.NEED_PT_PIN,
                        HBCIUtils.getLocMsg("CALLB_NEED_PTPIN"),
                        HBCICallback.TYPE_SECRET,
                        s);
                if (s.length() == 0) {
                    throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_PINZERO"));
                }
            }

            String tan = "";

            // tan darf nur beim einschrittverfahren oder bei
            // PV=1 und passport.contains(challenge)           und tan-pflichtiger auftrag oder bei
            // PV=2 und passport.contains(challenge+reference) und HKTAN
            // ermittelt werden

            String pintanMethod = getCurrentTANMethod(false);

            if (pintanMethod.equals(Sig.SECFUNC_SIG_PT_1STEP)) {
                // nur beim normalen einschritt-verfahren muss anhand der segment-
                // codes ermittelt werden, ob eine tan benötigt wird
                log.debug("onestep method - checking GVs to decide whether or not we need a TAN");

                // segment-codes durchlaufen
                String codes = collectSegCodes(new String(data, "ISO-8859-1"));
                StringTokenizer tok = new StringTokenizer(codes, "|");

                while (tok.hasMoreTokens()) {
                    String code = tok.nextToken();
                    String info = getPinTanInfo(code);

                    if (info.equals("J")) {
                        // für dieses segment wird eine tan benötigt
                        log.debug("the job with the code " + code + " needs a TAN");

                        if (tan.length() == 0) {
                            // noch keine tan bekannt --> callback

                            StringBuffer s = new StringBuffer();
                            callback.callback(
                                    HBCICallback.NEED_PT_TAN,
                                    HBCIUtils.getLocMsg("CALLB_NEED_PTTAN"),
                                    HBCICallback.TYPE_TEXT,
                                    s);
                            if (s.length() == 0) {
                                throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_TANZERO"));
                            }
                            tan = s.toString();
                        } else {
                            log.warn("there should be only one job that needs a TAN!");
                        }

                    } else if (info.equals("N")) {
                        log.debug("the job with the code " + code + " does not need a TAN");

                    } else if (info.length() == 0) {
                        // TODO: ist das hier dann nicht ein A-Segment? In dem Fall
                        // wäre diese Warnung überflüssig
                        log.warn("the job with the code " + code + " seems not to be allowed with PIN/TAN");
                    }
                }
            } else {
                log.debug("twostep method - checking passport(challenge) to decide whether or not we need a TAN");
                HashMap<String, String> secmechInfo = getCurrentSecMechInfo();

                // gespeicherte challenge aus passport holen
                String challenge = (String) getPersistentData("pintan_challenge");
                setPersistentData("pintan_challenge", null);

                if (challenge == null) {
                    // es gibt noch keine challenge
                    log.debug("will not sign with a TAN, because there is no challenge");
                } else {
                    log.debug("found challenge in passport, so we ask for a TAN");

                    // willuhn 2011-05-27 Wir versuchen, den Flickercode zu ermitteln und zu parsen
                    String hhduc = (String) getPersistentData("pintan_challenge_hhd_uc");
                    setPersistentData("pintan_challenge_hhd_uc", null); // gleich wieder aus dem Passport loeschen

                    HHDVersion hhd = HHDVersion.find(secmechInfo);
                    log.debug("detected HHD version: " + hhd);

                    final StringBuffer payload = new StringBuffer();
                    final String msg = secmechInfo.get("name") + "\n" + secmechInfo.get("inputinfo") + "\n\n" + challenge;

                    if (hhd.getType() == HHDVersion.Type.PHOTOTAN) {
                        // Bei PhotoTAN haengen wir ungeparst das HHDuc an. Das kann dann auf
                        // Anwendungsseite per MatrixCode geparst werden
                        payload.append(hhduc);
                        callback.callback(HBCICallback.NEED_PT_PHOTOTAN, msg, HBCICallback.TYPE_TEXT, payload);
                    } else {
                        // willuhn 2011-05-27: Flicker-Code anhaengen, falls vorhanden
                        String flicker = parseFlickercode(challenge, hhduc);
                        if (flicker != null)
                            payload.append(flicker);

                        callback.callback(HBCICallback.NEED_PT_TAN, msg, HBCICallback.TYPE_TEXT, payload);
                    }

                    setPersistentData("externalid", null); // External-ID aus Passport entfernen
                    if (payload.length() == 0) {
                        throw new HBCI_Exception(HBCIUtils.getLocMsg("EXCMSG_TANZERO"));
                    }
                    tan = payload.toString();
                }
            }

            return (getPIN() + "|" + tan).getBytes("ISO-8859-1");
        } catch (Exception ex) {
            throw new HBCI_Exception("*** signing failed", ex);
        }
    }

    /**
     * Versucht, aus Challenge und Challenge HHDuc den Flicker-Code zu extrahieren
     * und ihn in einen flickerfaehigen Code umzuwandeln.
     * Nur wenn tatsaechlich ein gueltiger Code enthalten ist, der als
     * HHDuc-Code geparst und in einen Flicker-Code umgewandelt werden konnte,
     * liefert die Funktion den Code. Sonst immer NULL.
     *
     * @param challenge der Challenge-Text. Das DE "Challenge HHDuc" gibt es
     *                  erst seit HITAN4. Einige Banken haben aber schon vorher optisches chipTAN
     *                  gemacht. Die haben das HHDuc dann direkt im Freitext des Challenge
     *                  mitgeschickt (mit String-Tokens zum Extrahieren markiert). Die werden vom
     *                  FlickerCode-Parser auch unterstuetzt.
     * @param hhduc     das echte Challenge HHDuc.
     * @return der geparste und in Flicker-Format konvertierte Code oder NULL.
     */
    private String parseFlickercode(String challenge, String hhduc) {
        // 1. Prioritaet hat hhduc. Gibts aber erst seit HITAN4
        if (hhduc != null && hhduc.trim().length() > 0) {
            try {
                FlickerCode code = new FlickerCode(hhduc);
                return code.render();
            } catch (Exception e) {
                log.debug("unable to parse Challenge HHDuc " + hhduc + ":" + HBCIUtils.exception2String(e));
            }
        }

        // 2. Checken, ob im Freitext-Challenge was parse-faehiges steht.
        // Kann seit HITAN1 auftreten
        if (challenge != null && challenge.trim().length() > 0) {
            try {
                FlickerCode code = new FlickerCode(challenge);
                return code.render();
            } catch (Exception e) {
                // Das darf durchaus vorkommen, weil das Challenge auch bei manuellem
                // chipTAN- und smsTAN Verfahren verwendet wird, wo gar kein Flicker-Code enthalten ist.
                // Wir loggen es aber trotzdem - fuer den Fall, dass tatsaechlich ein Flicker-Code
                // enthalten ist. Sonst koennen wir das nicht debuggen.
                log.debug("challenge contains no HHDuc (no problem in most cases):" + HBCIUtils.exception2String(e));
            }
        }
        // Ne, definitiv kein Flicker-Code.
        return null;
    }

    public byte[][] encrypt(byte[] plainMsg) {
        try {
            int padLength = plainMsg[plainMsg.length - 1];
            byte[] encrypted = new String(plainMsg, 0, plainMsg.length - padLength, "ISO-8859-1").getBytes("ISO-8859-1");
            return new byte[][]{new byte[8], encrypted};
        } catch (Exception ex) {
            throw new HBCI_Exception("*** encrypting message failed", ex);
        }
    }

    public byte[] decrypt(byte[] cryptedKey, byte[] cryptedMsg) {
        try {
            return (new String(cryptedMsg, "ISO-8859-1") + '\001').getBytes("ISO-8859-1");
        } catch (Exception ex) {
            throw new HBCI_Exception("*** decrypting of message failed", ex);
        }
    }

    public void setBPD(HashMap<String, String> p) {
        super.setBPD(p);

        if (p != null && p.size() != 0) {
            // hier die liste der verfügbaren sicherheitsverfahren aus den
            // bpd (HITANS) extrahieren

            twostepMechanisms.clear();

            // willuhn 2011-06-06 Maximal zulaessige HITANS-Segment-Version ermitteln
            // Hintergrund: Es gibt User, die nur HHD 1.3-taugliche TAN-Generatoren haben,
            // deren Banken aber auch HHD 1.4 beherrschen. In dem Fall wuerde die Bank
            // HITANS/HKTAN/HITAN in Segment-Version 5 machen, was in der Regel dazu fuehren
            // wird, dass HHD 1.4 zur Anwendung kommt. Das bewirkt, dass ein Flicker-Code
            // erzeugt wird, der vom TAN-Generator des Users gar nicht lesbar ist, da dieser
            // kein HHD 1.4 beherrscht. Mit dem folgenden Parameter kann die Maximal-Version
            // des HITANS-Segments nach oben begrenzt werden, so dass z.Bsp. HITANS5 ausgefiltert
            // wird.
            int maxAllowedVersion = 0;


            for (String key : p.keySet()) {
                // p.getProperty("Params_x.TAN2StepParY.ParTAN2StepZ.TAN2StepParamsX_z.*")
                if (key.startsWith("Params")) {
                    String subkey = key.substring(key.indexOf('.') + 1);
                    if (subkey.startsWith("TAN2StepPar")) {

                        // willuhn 2011-05-13 Wir brauchen die Segment-Version, weil mittlerweile TAN-Verfahren
                        // mit identischer Sicherheitsfunktion in unterschiedlichen Segment-Versionen auftreten koennen
                        // Wenn welche mehrfach vorhanden sind, nehmen wir nur das aus der neueren Version
                        int segVersion = Integer.parseInt(subkey.substring(11, 12));

                        subkey = subkey.substring(subkey.indexOf('.') + 1);
                        if (subkey.startsWith("ParTAN2Step") && subkey.endsWith(".secfunc")) {
                            // willuhn 2011-06-06 Segment-Versionen ueberspringen, die groesser als die max. zulaessige sind
                            if (maxAllowedVersion > 0 && segVersion > maxAllowedVersion) {
                                log.info("skipping segversion " + segVersion + ", larger than allowed version " + maxAllowedVersion);
                                continue;
                            }

                            String secfunc = p.get(key);

                            // willuhn 2011-05-13 Checken, ob wir das Verfahren schon aus einer aktuelleren Segment-Version haben
                            HashMap<String, String> prev = twostepMechanisms.get(secfunc);
                            if (prev != null) {
                                // Wir haben es schonmal. Mal sehen, welche Versionsnummer es hat
                                int prevVersion = Integer.parseInt(prev.get("segversion"));
                                if (prevVersion > segVersion) {
                                    log.debug("found another twostepmech " + secfunc + " in segversion " + segVersion + ", allready have one in segversion " + prevVersion + ", ignoring segversion " + segVersion);
                                    continue;
                                }
                            }

                            HashMap<String, String> entry = new HashMap<>();

                            // willuhn 2011-05-13 Wir merken uns die Segment-Version in dem Zweischritt-Verfahren
                            // Daran koennen wir erkennen, ob wir ein mehrfach auftretendes
                            // Verfahren ueberschreiben koennen oder nicht.
                            entry.put("segversion", Integer.toString(segVersion));

                            String paramHeader = key.substring(0, key.lastIndexOf('.'));
                            // Params_x.TAN2StepParY.ParTAN2StepZ.TAN2StepParamsX_z

                            // alle properties durchlaufen und alle suchen, die mit dem
                            // paramheader beginnen, und die entsprechenden werte im
                            // entry abspeichern
                            for (String key2 : p.keySet()) {

                                if (key2.startsWith(paramHeader + ".")) {
                                    int dotPos = key2.lastIndexOf('.');

                                    entry.put(
                                            key2.substring(dotPos + 1),
                                            p.get(key2));
                                }
                            }

                            // diesen mechanismus abspeichern
                            twostepMechanisms.put(secfunc, entry);
                        }
                    }
                }
            }
        }
    }

    private void searchFor3920s(List<HBCIRetVal> rets) {
        for (HBCIRetVal ret : rets) {
            if (ret.code.equals("3920")) {
                this.allowedTwostepMechanisms.clear();

                int l2 = ret.params.length;
                this.allowedTwostepMechanisms.addAll(Arrays.asList(ret.params).subList(0, l2));

                log.debug("autosecfunc: found 3920 in response - updated list of allowed twostepmechs with " + allowedTwostepMechanisms.size() + " entries");
            }
        }
    }

    private void searchFor3072s(List<HBCIRetVal> rets) {
        for (HBCIRetVal ret : rets) {
            if (ret.code.equals("3072")) {
                String newCustomerId = "";
                String newUserId = "";
                int l2 = ret.params.length;
                if (l2 > 0) {
                    newUserId = ret.params[0];
                    newCustomerId = ret.params[0];
                }
                if (l2 > 1) {
                    newCustomerId = ret.params[1];
                }
                if (l2 > 0) {
                    log.debug("autosecfunc: found 3072 in response - change user id");
                    // Aufrufer informieren, dass UserID und CustomerID geändert wurde
                    StringBuffer retData = new StringBuffer();
                    retData.append(newUserId)
                            .append("|")
                            .append(newCustomerId);
                    callback.callback(HBCICallback.USERID_CHANGED, "*** User ID changed", HBCICallback.TYPE_TEXT, retData);
                }
            }
        }
    }

    public boolean postInitResponseHook(HBCIMsgStatus msgStatus) {
        if (!msgStatus.isOK()) {
            log.debug("dialog init ended with errors - searching for return code 'wrong PIN'");

            if (msgStatus.isInvalidPIN()) {
                log.info("detected 'invalid PIN' error - clearing passport PIN");
                clearPIN();

                // Aufrufer informieren, dass falsche PIN eingegeben wurde (um evtl. PIN aus Puffer zu löschen, etc.)
                StringBuffer retData = new StringBuffer();
                callback.callback(HBCICallback.WRONG_PIN, "*** invalid PIN entered", HBCICallback.TYPE_TEXT, retData);
            }
        }

        log.debug("autosecfunc: search for 3920s in response to detect allowed twostep secmechs");

        searchFor3920s(msgStatus.globStatus.getWarnings());
        searchFor3920s(msgStatus.segStatus.getWarnings());
        searchFor3072s(msgStatus.segStatus.getWarnings());

        setPersistentData("_authed_dialog_executed", Boolean.TRUE);

        // aktuelle secmech merken und neue auswählen (basierend auf evtl. gerade
        // neu empfangenen informationen (3920s))
        String oldTANMethod = currentTANMethod;
        String updatedTANMethod = getCurrentTANMethod(true);

        if (oldTANMethod != null && !oldTANMethod.equals(updatedTANMethod)) {
            // wenn sich das ausgewählte secmech geändert hat, müssen wir
            // einen dialog-restart fordern, weil während eines dialoges
            // das secmech nicht gewechselt werden darf
            log.info("autosecfunc: after this dialog-init we had to change selected pintan method from " + oldTANMethod + " to " + updatedTANMethod + ", so a restart of this dialog is needed");
            return true;
        }

        return false;
    }

    public boolean isSupported() {
        boolean ret = false;
        HashMap<String, String> bpd = getBPD();

        if (bpd != null && bpd.size() != 0) {
            // loop through bpd and search for PinTanPar segment
            for (String key : bpd.keySet()) {

                if (key.startsWith("Params")) {
                    int posi = key.indexOf(".");
                    if (key.substring(posi + 1).startsWith("PinTanPar")) {
                        ret = true;
                        break;
                    }
                }
            }

            if (ret) {
                // prüfen, ob gewähltes sicherheitsverfahren unterstützt wird
                // autosecmech: hier wird ein flag uebergeben, das anzeigt, dass getCurrentTANMethod()
                // hier evtl. automatisch ermittelte secmechs neu verifzieren soll
                String current = getCurrentTANMethod(true);

                if (current.equals(Sig.SECFUNC_SIG_PT_1STEP)) {
                    // einschrittverfahren gewählt
                    if (!isOneStepAllowed()) {
                        log.error("not supported: onestep method not allowed by BPD");
                        ret = false;
                    } else {
                        log.debug("supported: pintan-onestep");
                    }
                } else {
                    // irgendein zweischritt-verfahren gewählt
                    HashMap<String, String> entry = twostepMechanisms.get(current);
                    if (entry == null) {
                        // es gibt keinen info-eintrag für das gewählte verfahren
                        log.error("not supported: twostep-method " + current + " selected, but this is not supported");
                        ret = false;
                    } else {
                        log.debug("selected twostep-method " + current + " (" + entry.get("name") + ") is supported");
                    }
                }
            }
        } else {
            ret = true;
        }

        return ret;
    }

    private boolean isOneStepAllowed() {
        // default ist true, weil entweder *nur* das einschritt-verfahren unter-
        // stützt wird oder keine BPD vorhanden sind, um das zu entscheiden
        boolean ret = true;

        HashMap<String, String> bpd = getBPD();
        if (bpd != null) {
            for (String key : bpd.keySet()) {
                // TODO: willuhn 2011-05-13: Das nimmt einfach den ersten gefundenen Parameter, liefert
                // jedoch faelschlicherweise false, wenn das erste gefundene kein Einschritt-Verfahren ist
                // Hier muesste man durch alle iterieren und dann true liefern, wenn wenigstens
                // eines den Wert "J" hat.

                // p.getProperty("Params_x.TAN2StepParY.ParTAN2StepZ.can1step")
                if (key.startsWith("Params")) {
                    String subkey = key.substring(key.indexOf('.') + 1);
                    if (subkey.startsWith("TAN2StepPar") &&
                            subkey.endsWith(".can1step")) {
                        String value = bpd.get(key);
                        ret = value.equals("J");
                        break;
                    }
                }
            }
        }

        return ret;
    }

    public void setCurrentTANMethod(String method) {
        this.currentTANMethod = method;
    }

    public String getCurrentTANMethod(boolean recheckSupportedSecMechs) {
        // autosecmech: hier auch dann checken, wenn recheckSupportedSecMechs==true
        // UND die vorherige auswahl AUTOMATISCH getroffen wurde (manuelle auswahl
        // also in jedem fall weiter verwenden) (das AUTOMATISCH erkennt man daran,
        // dass recheckCurrentTANMethodNeeded==true ist)
        if (currentTANMethod == null || recheckSupportedSecMechs) {
            log.debug("autosecfunc: (re)checking selected pintan secmech");

            // es ist noch kein zweischrittverfahren ausgewaehlt, oder die 
            // aktuelle auswahl soll gegen die liste der tatsaechlich unterstuetzten
            // verfahren validiert werden

            List<String[]> options = new ArrayList<>();

            if (isOneStepAllowed()) {
                // wenn einschrittverfahren unterstützt, dass zur liste hinzufügen
                if (allowedTwostepMechanisms.size() == 0 || allowedTwostepMechanisms.contains(Sig.SECFUNC_SIG_PT_1STEP)) {
                    options.add(new String[]{Sig.SECFUNC_SIG_PT_1STEP, "Einschritt-Verfahren"});
                }
            }

            // alle zweischritt-verfahren zur auswahlliste hinzufügen
            String[] secfuncs = twostepMechanisms.keySet().toArray(new String[0]);
            Arrays.sort(secfuncs);
            for (String secfunc : secfuncs) {
                if (allowedTwostepMechanisms.size() == 0 || allowedTwostepMechanisms.contains(secfunc)) {
                    HashMap<String, String> entry = twostepMechanisms.get(secfunc);
                    options.add(new String[]{secfunc, entry.get("name")});
                }
            }

            if (options.size() == 1) {
                // wenn nur ein verfahren unterstützt wird, das automatisch auswählen
                String autoSelection = (options.get(0))[0];

                log.debug("autosecfunc: there is only one pintan method (" + autoSelection + ") supported - choosing this automatically");
                if (currentTANMethod != null && !autoSelection.equals(currentTANMethod)) {
                    log.debug("autosecfunc: currently selected method (" + currentTANMethod + ") differs from auto-selected method (" + autoSelection + ")");
                }

                setCurrentTANMethod(autoSelection);

                // autosecmech: hier merken, dass dieses verfahren AUTOMATISCH
                // ausgewaehlt wurde, so dass wir spaeter immer mal wieder pruefen
                // muessen, ob inzwischen nicht mehr/andere unterstuetzte secmechs bekannt sind
                // (passiert z.b. wenn das anonyme abholen der bpd fehlschlaegt)
                this.currentTANMethodWasAutoSelected = true;

            } else if (options.size() > 1) {
                // es werden mehrere verfahren unterstützt

                if (currentTANMethod != null) {
                    // es ist schon ein verfahren ausgewaehlt. falls dieses verfahren
                    // nicht in der liste der unterstuetzten verfahren enthalten ist,
                    // setzen wir das auf "null" zurueck, damit das zu verwendende
                    // verfahren neu ermittelt wird

                    boolean ok = false;
                    for (String[] option : options) {
                        if (currentTANMethod.equals((option)[0])) {
                            ok = true;
                            break;
                        }
                    }

                    if (!ok) {
                        log.debug("autosecfunc: currently selected pintan method (" + currentTANMethod + ") not in list of supported methods - resetting current selection");
                        currentTANMethod = null;
                    }
                }

                if (currentTANMethod == null || this.currentTANMethodWasAutoSelected) {
                    // wenn noch kein verfahren ausgewaehlt ist, oder das bisherige
                    // verfahren automatisch ausgewaehlt wurde, muessen wir uns
                    // neu fuer eine method aus der liste entscheiden

                    // TODO: damit das sinnvoll funktioniert, sollte die liste der
                    // allowedTwostepMechs mit im passport gespeichert werden.
                    if (allowedTwostepMechanisms.size() == 0 &&
                            getPersistentData("_authed_dialog_executed") == null) {
                        // wir wählen einen secmech automatisch aus, wenn wir
                        // die liste der erlaubten secmechs nicht haben
                        // (entweder weil wir sie noch nie abgefragt haben oder weil
                        // diese daten einfach nicht geliefert werden). im fall
                        // "schon abgefragt, aber nicht geliefert" dürfen wir aber
                        // wiederum NICHT automatisch auswählen, so dass wir zusätzlich
                        // fragen, ob schon mal ein dialog gelaufen ist, bei dem 
                        // diese daten hätten geliefert werden KÃNNEN (_authed_dialog_executed).
                        // nur wenn wir die liste der gültigen secmechs noch gar
                        // nicht haben KÃNNEN, wählen wir einen automatisch aus.

                        String autoSelection = (options.get(0))[0];
                        log.debug("autosecfunc: there are " + options.size() + " pintan methods supported, but we don't know which of them are allowed for the current user, so we automatically choose " + autoSelection);
                        setCurrentTANMethod(autoSelection);

                        // autosecmech: hier merken, dass dieses verfahren AUTOMATISCH
                        // ausgewaehlt wurde, so dass wir spaeter immer mal wieder pruefen
                        // muessen, ob inzwischen nicht mehr/andere unterstuetzte secmechs bekannt sind
                        // (passiert z.b. wenn das anonyme abholen der bpd fehlschlaegt)
                        this.currentTANMethodWasAutoSelected = true;

                    } else {
                        // wir wissen schon, welche secmechs erlaubt sind (entweder
                        // durch einen vorhergehenden dialog oder aus den persistenten
                        // passport-daten), oder wir wissen es nicht (size==0), haben aber schonmal
                        // danach gefragt (ein authed_dialog ist schon gelaufen, bei dem
                        // diese daten aber nicht geliefert wurden). 
                        // in jedem fall steht in "options" die liste der prinzipiell
                        // verfügbaren secmechs drin, u.U. gekürzt auf die tatsächlich
                        // erlaubten secmechs.
                        // wir fragen also via callback nach, welcher dieser secmechs
                        // denn nun verwendet werden soll

                        log.debug("autosecfunc: we have to callback to ask for pintan method to be used");

                        // auswahlliste als string zusammensetzen
                        StringBuffer retData = new StringBuffer();
                        for (String[] option : options) {
                            if (retData.length() != 0) {
                                retData.append("|");
                            }
                            retData.append(option[0]).append(":").append(option[1]);
                        }

                        // callback erzeugen
                        callback.callback(
                                HBCICallback.NEED_PT_SECMECH,
                                "*** Select a pintan method from the list",
                                HBCICallback.TYPE_TEXT,
                                retData);

                        // überprüfen, ob das gewählte verfahren einem aus der liste entspricht
                        String selected = retData.toString();
                        boolean ok = false;
                        for (String[] option : options) {
                            if (selected.equals((option)[0])) {
                                ok = true;
                                break;
                            }
                        }

                        if (!ok) {
                            throw new InvalidUserDataException("*** selected pintan method not supported!");
                        }

                        setCurrentTANMethod(selected);
                        this.currentTANMethodWasAutoSelected = false;

                        log.debug("autosecfunc: manually selected pintan method " + currentTANMethod);
                    }
                }

            } else {
                // es wird scheinbar GAR KEIN verfahren unterstuetzt. also nehmen
                // wir automatisch 999
                log.debug("autosecfunc: absolutely no information about allowed pintan methods available - automatically falling back to 999");
                setCurrentTANMethod("999");
                this.currentTANMethodWasAutoSelected = true;
            }
        }

        return currentTANMethod;
    }

    public HashMap<String, String> getCurrentSecMechInfo() {
        return twostepMechanisms.get(getCurrentTANMethod(false));
    }

    public HashMap<String, HashMap<String, String>> getTwostepMechanisms() {
        return twostepMechanisms;
    }

    public String getProfileMethod() {
        return "PIN";
    }

    public String getProfileVersion() {
        return getCurrentTANMethod(false).equals(Sig.SECFUNC_SIG_PT_1STEP) ? "1" : "2";
    }

    public boolean needUserKeys() {
        return false;
    }

    public boolean needUserSig() {
        return true;
    }

    public String getSysStatus() {
        return "1";
    }

    public boolean hasInstSigKey() {
        return true;
    }

    public boolean hasInstEncKey() {
        return true;
    }

    public String getInstEncKeyName() {
        return getUserId();
    }

    public String getInstEncKeyNum() {
        return "0";
    }

    public String getInstEncKeyVersion() {
        return "0";
    }

    public String getMySigKeyName() {
        return getUserId();
    }

    public String getMySigKeyNum() {
        return "0";
    }

    public String getMySigKeyVersion() {
        return "0";
    }

    public HBCIKey getMyPublicEncKey() {
        return null;
    }

    public void setMyPublicEncKey(HBCIKey key) {
    }

    public HBCIKey getMyPrivateEncKey() {
        return null;
    }

    public void setMyPrivateEncKey(HBCIKey key) {
    }

    public String getCryptMode() {
        // dummy-wert
        return Crypt.ENCMODE_CBC;
    }

    public String getCryptAlg() {
        // dummy-wert
        return Crypt.ENCALG_2K3DES;
    }

    public String getCryptKeyType() {
        // dummy-wert
        return Crypt.ENC_KEYTYPE_DDV;
    }

    public String getSigFunction() {
        return getCurrentTANMethod(false);
    }

    public String getCryptFunction() {
        return Crypt.SECFUNC_ENC_PLAIN;
    }

    public String getSigAlg() {
        // dummy-wert
        return Sig.SIGALG_RSA;
    }

    public String getSigMode() {
        // dummy-wert
        return Sig.SIGMODE_ISO9796_1;
    }

    public String getHashAlg() {
        // dummy-wert
        return Sig.HASHALG_RIPEMD160;
    }

    public void setInstSigKey(HBCIKey key) {
    }

    public void setInstEncKey(HBCIKey key) {
        // TODO: implementieren für bankensignatur bei HITAN
    }

    public void setMyPublicDigKey(HBCIKey key) {
    }

    public void setMyPrivateDigKey(HBCIKey key) {
    }

    public void setMyPublicSigKey(HBCIKey key) {
    }

    public void setMyPrivateSigKey(HBCIKey key) {
    }

    public void incSigId() {
        // for PinTan we always use the same sigid
    }

    protected String collectSegCodes(String msg) {
        StringBuilder ret = new StringBuilder();
        int len = msg.length();
        int posi = 0;

        while (true) {
            int endPosi = msg.indexOf(':', posi);
            if (endPosi == -1) {
                break;
            }

            String segcode = msg.substring(posi, endPosi);
            if (ret.length() != 0) {
                ret.append("|");
            }
            ret.append(segcode);

            while (posi < len && msg.charAt(posi) != '\'') {
                posi = HBCIUtils.getPosiOfNextDelimiter(msg, posi + 1);
            }
            if (posi >= len) {
                break;
            }
            posi++;
        }

        return ret.toString();
    }

    public String getPinTanInfo(String code) {
        String ret = "";
        HashMap<String, String> bpd = getBPD();

        if (bpd != null) {
            boolean isGV = false;
            StringBuffer paramCode = new StringBuffer(code).replace(1, 2, "I").append("S");

            for (String key : bpd.keySet()) {
                if (key.startsWith("Params") &&
                        key.substring(key.indexOf(".") + 1).startsWith("PinTanPar") &&
                        key.contains(".ParPinTan.PinTanGV") &&
                        key.endsWith(".segcode")) {
                    String code2 = bpd.get(key);
                    if (code.equals(code2)) {
                        key = key.substring(0, key.length() - ("segcode").length()) + "needtan";
                        ret = bpd.get(key);
                        break;
                    }
                } else if (key.startsWith("Params") &&
                        key.endsWith(".SegHead.code")) {

                    String code2 = bpd.get(key);
                    if (paramCode.equals(code2)) {
                        isGV = true;
                    }
                }
            }

            // wenn das kein GV ist, dann ist es ein Admin-Segment
            if (ret.length() == 0 && !isGV) {
                if (verifyTANMode && code.equals("HKIDN")) {
                    // im TAN-verify-mode wird bei der dialog-initialisierung
                    // eine TAN mit versandt; die Dialog-Initialisierung erkennt
                    // man am HKIDN-segment
                    ret = "J";
                    deactivateTANVerifyMode();
                } else {
                    ret = "A";
                }
            }
        }

        return ret;
    }

    private void deactivateTANVerifyMode() {
        this.verifyTANMode = false;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getProxyPass() {
        return proxypass;
    }

    public String getProxyUser() {
        return proxyuser;
    }

    public String getOrderHashMode() {
        String ret = null;

        HashMap<String, String> bpd = getBPD();
        if (bpd != null) {
            for (String key : bpd.keySet()) {
                HashMap<String, String> props = getCurrentSecMechInfo();
                String segVersion = "";
                try {
                    int value = Integer.parseInt(props.get("segversion"));
                    segVersion += value;
                } catch (NumberFormatException nfe) {
                    //Not an integer, hence ignored
                }

                // p.getProperty("Params_x.TAN2StepParY.ParTAN2StepZ.can1step")
                if (key.startsWith("Params")) {
                    String subkey = key.substring(key.indexOf('.') + 1);
                    if (subkey.startsWith("TAN2StepPar" + segVersion) &&
                            subkey.endsWith(".orderhashmode")) {
                        ret = bpd.get(key);
                        break;
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Uebernimmt das Rueckfragen und Einsetzen der TAN-Medien-Bezeichung bei Bedarf.
     *
     * @param hktan der Job, in den der Parameter eingesetzt werden soll.
     */
    public void applyTanMedia(GVTAN2Step hktan) {
        if (hktan == null)
            return;

        // Gibts erst ab hhd1.3, siehe
        // FinTS_3.0_Security_Sicherheitsverfahren_PINTAN_Rel_20101027_final_version.pdf, Kapitel B.4.3.1.1.1
        // Zitat: Ist in der BPD als Anzahl unterstützter aktiver TAN-Medien ein Wert > 1
        //        angegeben und ist der BPD-Wert für Bezeichnung des TAN-Mediums erforderlich = 2,
        //        so muss der Kunde z. B. im Falle des mobileTAN-Verfahrens
        //        hier die Bezeichnung seines für diesen Auftrag zu verwendenden TAN-
        //        Mediums angeben.
        // Ausserdem: "Nur bei TAN-Prozess=1, 3, 4". Das muess aber der Aufrufer pruefen. Ist mir
        // hier zu kompliziert

        int hktan_version = Integer.parseInt(hktan.getSegVersion());
        log.debug("hktan_version: " + hktan_version);
        if (hktan_version >= 3) {
            HashMap<String, String> secmechInfo = getCurrentSecMechInfo();

            // Anzahl aktiver TAN-Medien ermitteln
            int num = Integer.parseInt(secmechInfo.get("nofactivetanmedia") != null ? secmechInfo.get("nofactivetanmedia") : "0");
            String needed = secmechInfo.get("needtanmedia") != null ? secmechInfo.get("needtanmedia") : "";
            log.debug("nofactivetanmedia: " + num + ", needtanmedia: " + needed);

            // Ich hab Mails von Usern erhalten, bei denen die Angabe des TAN-Mediums auch
            // dann noetig war, wenn nur eine Handy-Nummer hinterlegt war. Daher logen wir
            // "num" nur, bringen die Abfrage jedoch schon bei num<2 - insofern needed=2.
            if (needed.equals("2")) {
                log.debug("we have to add the tan media");

                String tanMediaNames = this.getUPD().get("tanmedia.names");
                String tanMedia = callback.tanMediaCallback(tanMediaNames != null ? tanMediaNames : "");
                if (tanMedia != null) {
                    hktan.setParam("tanmedia", tanMedia);
                }
            }
        }
    }

    public String getPIN() {
        return this.pin;
    }

    public void setPIN(String pin) {
        this.pin = pin;
    }

    public void clearPIN() {
        setPIN(null);
    }

    public List<String> getAllowedTwostepMechanisms() {
        return this.allowedTwostepMechanisms;
    }

    public void setAllowedTwostepMechanisms(List<String> l) {
        this.allowedTwostepMechanisms = l;
    }

    public int getMaxGVSegsPerMsg() {
        return 1;
    }
}