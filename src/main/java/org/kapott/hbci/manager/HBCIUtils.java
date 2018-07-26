/*  $Id: HBCIUtils.java,v 1.2 2011/11/24 21:59:37 willuhn Exp $

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

import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.exceptions.InvalidArgumentException;
import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.structures.Konto;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;


public final class HBCIUtils {

    private static final String VERSION = "HBCI4Java-2.5.12";
    public static Properties blzs = new Properties();
    public static Map<String, BankInfo> banks = new HashMap<>();
    private static ResourceBundle resourceBundle = ResourceBundle.getBundle("hbci4java-messages", Locale.getDefault());

    /**
     * Ermittelt zu einer gegebenen Bankleitzahl den Namen des Institutes.
     *
     * @param blz die Bankleitzahl
     * @return den Namen des dazugehörigen Kreditinstitutes. Falls die Bankleitzahl unbekannt ist,
     * so wird ein leerer String zurückgegeben
     */
    public static String getNameForBLZ(String blz) {
        BankInfo info = getBankInfo(blz);
        if (info == null)
            return "";
        return info.getName() != null ? info.getName() : "";
    }

    /**
     * Liefert die Bank-Informationen zur angegebenen BLZ.
     *
     * @param blz die BLZ.
     * @return die Bank-Informationen oder NULL, wenn zu der BLZ keine Informationen bekannt sind.
     */
    public static BankInfo getBankInfo(String blz) {
        return banks.get(blz);
    }

    /**
     * Liefert eine Liste von Bank-Informationen, die zum angegebenen Suchbegriff passen.
     *
     * @param query der Suchbegriff.
     *              Der Suchbegriff muss mindestens 3 Zeichen enthalten und ist nicht case-sensitive.
     *              Der Suchbegriff kann im Ort der Bank oder in deren Namen enthalten sein.
     *              Oder die BLZ oder BIC beginnt mit diesem Text.
     * @return die Liste der Bank-Informationen.
     * Die Ergebnis-Liste ist nach BLZ sortiert.
     * Die Funktion liefert niemals NULL sondern hoechstens eine leere Liste.
     */
    public static List<BankInfo> searchBankInfo(String query) {
        if (query != null)
            query = query.trim();

        List<BankInfo> list = new LinkedList<BankInfo>();
        if (query == null || query.length() < 3)
            return list;

        query = query.toLowerCase();

        for (BankInfo info : banks.values()) {
            String blz = info.getBlz();
            String bic = info.getBic();
            String name = info.getName();
            String loc = info.getLocation();

            // Anhand der BLZ?
            if (blz != null && blz.startsWith(query)) {
                list.add(info);
                continue;
            }

            // Anhand der BIC?
            if (bic != null && bic.toLowerCase().startsWith(query)) {
                list.add(info);
                continue;
            }

            // Anhand des Namens?
            if (name != null && name.toLowerCase().contains(query)) {
                list.add(info);
                continue;
            }
            // Anhand des Orts?
            if (loc != null && loc.toLowerCase().contains(query)) {
                list.add(info);
                continue;
            }
        }

        Collections.sort(list, new Comparator<BankInfo>() {
            /**
             * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
             */
            @Override
            public int compare(BankInfo o1, BankInfo o2) {
                if (o1 == null || o1.getBlz() == null)
                    return -1;
                if (o2 == null || o2.getBlz() == null)
                    return 1;

                return o1.getBlz().compareTo(o2.getBlz());
            }
        });

        return list;
    }

    /**
     * Gibt zu einer gegebenen Bankleitzahl den BIC-Code zurück.
     *
     * @param blz Bankleitzahl der Bank
     * @return BIC-Code dieser Bank. Falls kein BIC-Code bekannt ist, wird ein
     * leerer String zurückgegeben.
     * @deprecated Bitte {@link HBCIUtils#getBankInfo(String)} verwenden.
     */
    public static String getBICForBLZ(String blz) {
        BankInfo info = getBankInfo(blz);
        if (info == null)
            return "";
        return info.getBic() != null ? info.getBic() : "";
    }

    /**
     * Berechnet die IBAN fuer ein angegebenes deutsches Konto.
     *
     * @param k das Konto.
     * @return die berechnete IBAN.
     */
    public static String getIBANForKonto(Konto k) {
        String konto = k.number;

        // Die Unterkonto-Nummer muss mit eingerechnet werden.
        // Aber nur, wenn sie numerisch ist. Bei irgendeiner Bank wurde
        // "EUR" als Unterkontonummer verwendet. Das geht natuerlich nicht,
        // weil damit nicht gerechnet werden kann
        // Wir machen das auch nur dann, wenn beide Nummern zusammen max.
        // 10 Zeichen ergeben
        if (k.subnumber != null &&
                k.subnumber.length() > 0 &&
                k.subnumber.matches("[0-9]{1,8}") &&
                k.number.length() + k.subnumber.length() <= 10)
            konto += k.subnumber;

        /////////////////
        // Pruefziffer berechnen
        // Siehe http://www.iban.de/iban-pruefsumme.html
        String zeros = "0000000000";
        String filledKonto = zeros.substring(0, 10 - konto.length()) + konto; // 10-stellig mit Nullen fuellen
        StringBuffer sb = new StringBuffer();
        sb.append(k.blz);
        sb.append(filledKonto);
        sb.append("1314"); // hartcodiert fuer "DE
        sb.append("00"); // fest vorgegeben

        BigInteger mod = new BigInteger(sb.toString()).mod(new BigInteger("97")); // "97" ist fest vorgegeben in ISO 7064/Modulo 97-10
        String checksum = String.valueOf(98 - mod.intValue()); // "98" ist fest vorgegeben in ISO 7064/Modulo 97-10
        if (checksum.length() < 2)
            checksum = "0" + checksum;
        //
        /////////////////

        StringBuffer result = new StringBuffer();
        result.append("DE");
        result.append(checksum);
        result.append(k.blz);
        result.append(filledKonto);

        return result.toString();
    }

    /**
     * Gibt zu einer gegebenen Bankleitzahl den HBCI-Host (für RDH und DDV)
     * zurück.
     *
     * @param blz Bankleitzahl der Bank
     * @return HBCI-Host (DNS-Name oder IP-Adresse). Falls kein Host bekannt
     * ist, wird ein leerer String zurückgegeben.
     * @deprecated Bitte {@link HBCIUtils#getBankInfo(String)} verwenden.
     */
    public static String getHBCIHostForBLZ(String blz) {
        BankInfo info = getBankInfo(blz);
        if (info == null)
            return "";
        return info.getRdhAddress() != null ? info.getRdhAddress() : "";
    }

    /**
     * Gibt zu einer gegebenen Bankleitzahl die PIN/TAN-URL
     * zurück.
     *
     * @param blz Bankleitzahl der Bank
     * @return PIN/TAN-URL. Falls keine URL bekannt
     * ist, wird ein leerer String zurückgegeben.
     * @deprecated Bitte {@link HBCIUtils#getBankInfo(String)} verwenden.
     */
    public static String getPinTanURLForBLZ(String blz) {
        BankInfo info = getBankInfo(blz);
        if (info == null)
            return "";
        return info.getPinTanAddress() != null ? info.getPinTanAddress() : "";
    }

    /**
     * Gibt zu einer gegebenen Bankleitzahl zurück, welche HBCI-Version für DDV
     * bzw. RDH zu verwenden ist. Siehe auch {@link #getPinTanVersionForBLZ(String)}.
     *
     * @param blz
     * @return HBCI-Version
     * @deprecated Bitte {@link HBCIUtils#getBankInfo(String)} verwenden.
     */
    public static String getHBCIVersionForBLZ(String blz) {
        BankInfo info = getBankInfo(blz);
        if (info == null)
            return "";
        return info.getRdhVersion() != null ? info.getRdhVersion().getId() : "";
    }

    /**
     * Gibt zu einer gegebenen Bankleitzahl zurück, welche HBCI-Version für HBCI-PIN/TAN
     * bzw. RDH zu verwenden ist. Siehe auch {@link #getHBCIVersionForBLZ(String)}
     *
     * @param blz
     * @return HBCI-Version
     * @deprecated Bitte {@link HBCIUtils#getBankInfo(String)} verwenden.
     */
    public static String getPinTanVersionForBLZ(String blz) {
        BankInfo info = getBankInfo(blz);
        if (info == null)
            return "";
        return info.getPinTanVersion() != null ? info.getPinTanVersion().getId() : "";
    }

    /**
     * Gibt den StackTrace einer Exception zurück.
     *
     * @param e Exception
     * @return kompletter StackTrace als String
     */
    public static String exception2String(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString().trim();
    }

    /**
     * Extrahieren der root-Exception aus einer Exception-Chain.
     *
     * @param e Exception
     * @return String mit Infos zur root-Exception
     */
    public static String exception2StringShort(Exception e) {
        StringBuffer st = new StringBuffer();
        Throwable e2 = e;

        while (e2 != null) {
            String exClass = e2.getClass().getName();
            String msg = e2.getMessage();

            if (msg != null) {
                st.setLength(0);
                st.append(exClass);
                st.append(": ");
                st.append(msg);
            }
            e2 = e2.getCause();
        }

        return st.toString().trim();
    }

    /**
     * Wandelt ein Byte-Array in eine entsprechende hex-Darstellung um.
     *
     * @param data das Byte-Array, für das eine Hex-Darstellung erzeugt werden soll
     * @return einen String, der für jedes Byte aus <code>data</code>
     * zwei Zeichen (0-9,A-F) enthält.
     */
    public static String data2hex(byte[] data) {
        StringBuffer ret = new StringBuffer();

        for (int i = 0; i < data.length; i++) {
            String st = Integer.toHexString(data[i]);
            if (st.length() == 1) {
                st = '0' + st;
            }
            st = st.substring(st.length() - 2);
            ret.append(st).append(" ");
        }

        return ret.toString();
    }


    /**
     * Wandelt ein gegebenes Datumsobjekt in einen String um. Das Format
     * des erzeugten Strings ist abhängig vom gesetzten <em>HBCI4Java</em>-Locale
     * (siehe Kernel-Parameter <code>kernel.locale.*</code>)
     *
     * @param date ein Datum
     * @return die lokalisierte Darstellung dieses Datums als String
     */
    public static String date2StringLocal(Date date) {
        String ret;

        try {
            ret = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(date);
        } catch (Exception e) {
            throw new InvalidArgumentException(date.toString());
        }

        return ret;
    }

    /**
     * Wandelt einen String, der ein Datum in der lokalen Darstellung enthält
     * (abhängig von der <em>HBCI4Java</em>-Locale, siehe Kernel-Parameter
     * <code>kernel.locale.*</code>), in ein Datumsobjekt um
     *
     * @param date ein Datum in der lokalen Stringdarstellung
     * @return ein entsprechendes Datumsobjekt
     */
    public static Date string2DateLocal(String date) {
        Date ret;

        try {
            ret = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).parse(date);
        } catch (Exception e) {
            throw new InvalidArgumentException(date);
        }

        return ret;
    }

    /**
     * Wandelt ein gegebenes Datums-Objekt in einen String um, der die Uhrzeit enthält.
     * Das Format des erzeugten Strings ist abhängig von der gesetzten
     * <em>HBCI4Java</em>-Locale (siehe Kernel-Parameter <code>kernel.locale.*</code>).
     *
     * @param date ein Datumsobjekt
     * @return die lokalisierte Darstellung der Uhrzeit als String
     */
    public static String time2StringLocal(Date date) {
        String ret;

        try {
            ret = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(date);
        } catch (Exception e) {
            throw new InvalidArgumentException(date.toString());
        }

        return ret;
    }

    /**
     * Wandelt einen String, der eine Uhrzeit in der lokalen Darstellung enthält
     * (abhängig von der <em>HBCI4Java</em>-Locale, siehe Kernel-Parameter
     * <code>kernel.locale.*</code>), in ein Datumsobjekt um
     *
     * @param date eine Uhrzeit in der lokalen Stringdarstellung
     * @return ein entsprechendes Datumsobjekt
     */
    public static Date string2TimeLocal(String date) {
        Date ret;

        try {
            ret = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).parse(date);
        } catch (Exception e) {
            throw new InvalidArgumentException(date);
        }

        return ret;
    }

    /**
     * Wandelt ein gegebenes Datums-Objekt in einen String um, der sowohl Datum
     * als auch Uhrzeit enthält. Das Format des erzeugten Strings ist abhängig von der gesetzten
     * <em>HBCI4Java</em>-Locale (siehe Kernel-Parameter <code>kernel.locale.*</code>).
     *
     * @param date ein Datumsobjekt
     * @return die lokalisierte Darstellung des Datums-Objektes
     */
    public static String datetime2StringLocal(Date date) {
        String ret;

        try {
            ret = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).format(date);
        } catch (Exception e) {
            throw new InvalidArgumentException(date.toString());
        }

        return ret;
    }

    /**
     * Erzeugt ein Datums-Objekt aus Datum und Uhrzeit in der String-Darstellung.
     * Die String-Darstellung von Datum und Uhrzeit müssen dabei der aktuellen
     * <em>HBCI4Java</em>-Locale entsprechen (siehe Kernel-Parameter <code>kernel.locale.*</code>)).
     *
     * @param date ein Datum in der lokalen Stringdarstellung
     * @param time eine Uhrzeit in der lokalen Stringdarstellung (darf <code>null</code> sein)
     * @return ein entsprechendes Datumsobjekt
     */
    public static Date strings2DateTimeLocal(String date, String time) {
        Date ret;

        try {
            if (time != null)
                ret = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()).parse(date + " " + time);
            else
                ret = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).parse(date);
        } catch (Exception e) {
            throw new InvalidArgumentException(date + " / " + time);
        }

        return ret;
    }


    /**
     * Erzeugt einen String im Format YYYY-MM-DD
     */
    public static String date2StringISO(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    /**
     * Wandelt einen String der Form YYYY-MM-DD in ein <code>Date</code>-Objekt um.
     */
    public static Date string2DateISO(String st) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(st);
        } catch (ParseException e) {
            throw new InvalidArgumentException(st);
        }
    }

    /**
     * Erzeugt einen String der Form HH:MM:SS
     */
    public static String time2StringISO(Date date) {
        return new SimpleDateFormat("HH:mm:ss").format(date);
    }

    /**
     * Wandelt einen String der Form HH:MM:SS in ein <code>Date</code>-Objekt um
     */
    public static Date string2TimeISO(String st) {
        try {
            return new SimpleDateFormat("HH:mm:ss").parse(st);
        } catch (ParseException e) {
            throw new InvalidArgumentException(st);
        }
    }

    /**
     * Erzeugt einen String im Format YYYY-MM-DD HH:MM:SS
     */
    public static String datetime2StringISO(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * Erzeugt ein Datums-Objekt aus Datum und Uhrzeit in der String-Darstellung.
     * Die String-Darstellung von Datum und Uhrzeit müssen dabei im ISO-Format
     * vorlegen (Datum als yyyy-mm-dd, Zeit als hh:mm:ss). Der Parameter <code>time</code>
     * darf auch <code>null</code> sein, <code>date</code> jedoch nicht.
     *
     * @param date ein Datum in der ISO-Darstellung
     * @param time eine Uhrzeit in der ISO-Darstellung (darf auch <code>null</code> sein)
     * @return ein entsprechendes Datumsobjekt
     */
    public static Date strings2DateTimeISO(String date, String time) {
        if (date == null) {
            throw new InvalidArgumentException("*** date must not be null");
        }

        Date result;
        try {
            if (time != null) {
                result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date + " " + time);
            } else {
                result = new SimpleDateFormat("yyyy-MM-dd").parse(date);
            }
        } catch (ParseException e) {
            throw new InvalidArgumentException(date + " / " + time);
        }

        return result;
    }

    private static Method getAccountCRCMethodByAlg(String alg) {
        Class<AccountCRCAlgs> cl = null;
        Method method = null;

        try {
            cl = AccountCRCAlgs.class;
            method = cl.getMethod("alg_" + alg, new Class[]{int[].class, int[].class});
        } catch (Exception e) {
            LoggerFactory.getLogger(HBCIUtils.class).warn("CRC algorithm " + alg + " not yet implemented");
        }

        return method;
    }

    /**
     * <p>Überprüft, ob gegebene BLZ und Kontonummer zueinander passen.
     * Bei diesem Test wird wird die in die Kontonummer "eingebaute"
     * Prüziffer verifiziert. Anhand der BLZ wird ermittelt, welches
     * Prüfzifferverfahren zur Überprüfung eingesetzt werden muss.</p>
     * <p>Ein positives Ergebnis dieser Routine bedeutet <em>nicht</em>, dass das
     * entsprechende Konto bei der Bank <em>existiert</em>, sondern nur, dass
     * die Kontonummer bei der entsprechenden Bank prinzipiell gültig ist.</p>
     *
     * @param blz    die Bankleitzahl der Bank, bei der das Konto geführt wird
     * @param number die zu überprüfende Kontonummer
     * @return <code>true</code> wenn die Kontonummer nicht verifiziert werden kann (z.B.
     * weil das jeweilige Prüfzifferverfahren noch nicht in <em>HBCI4Java</em>
     * implementiert ist) oder wenn die Prüfung erfolgreich verläuft; <code>false</code>
     * wird immer nur dann zurückgegeben, wenn tatsächlich ein Prüfzifferverfahren
     * zum Überprüfen verwendet wurde und die Prüfung einen Fehler ergab
     */
    public static boolean checkAccountCRC(String blz, String number) {
        BankInfo info = getBankInfo(blz);
        String alg = info != null ? info.getChecksumMethod() : null;

        // Im Zweifel lassen wir die Bankverbindung lieber durch
        if (alg == null || alg.length() != 2) {
            LoggerFactory.getLogger(HBCIUtils.class).warn("no crc information about " + blz + " in database");
            return true;
        }

        LoggerFactory.getLogger(HBCIUtils.class).debug("crc-checking " + blz + "/" + number);
        return checkAccountCRCByAlg(alg, blz, number);
    }

    /**
     * Used to convert a blz or an account number to an array of ints, one
     * array element per digit.
     */
    private static int[] string2Ints(String st, int target_length) {
        int[] numbers = new int[target_length];
        int st_len = st.length();
        char ch;

        for (int i = 0; i < st_len; i++) {
            ch = st.charAt(i);
            numbers[target_length - st_len + i] = ch - '0';
        }

        return numbers;
    }

    /**
     * Überprüfen einer Kontonummer mit einem gegebenen CRC-Algorithmus.
     * Diese Methode wird intern von {@link HBCIUtils#checkAccountCRC(String, String)}
     * aufgerufen und kann für Debugging-Zwecke auch direkt benutzt werden.
     *
     * @param alg    Nummer des zu verwendenden Prüfziffer-Algorithmus (siehe
     *               Datei <code>blz.properties</code>).
     * @param blz    zu überprüfende Bankleitzahl
     * @param number zu überprüfende Kontonummer
     * @return <code>false</code>, wenn der Prüfzifferalgorithmus für die
     * angegebene Kontonummer einen Fehler meldet, sonst <code>true</code>
     * (siehe dazu auch {@link #checkAccountCRC(String, String)})
     */
    public static boolean checkAccountCRCByAlg(String alg, String blz, String number) {
        boolean ret = true;

        if (blz == null || number == null) {
            throw new NullPointerException("blz and number must not be null");
        }

        if (number.length() <= 10) {
            Method method = getAccountCRCMethodByAlg(alg);

            if (method != null) {
                try {
                    int[] blz_digits = string2Ints(blz, 8);
                    int[] number_digits = string2Ints(number, 10);

                    Object[] args = new Object[]{blz_digits, number_digits};
                    ret = ((Boolean) method.invoke(null, args)).booleanValue();

                    LoggerFactory.getLogger(HBCIUtils.class).debug("\"CRC check for \"+blz+\"/\"+number+\" with alg \"+alg+\": \"+ret");
                } catch (Exception e) {
                    throw new HBCI_Exception(e);
                }
            }
        } else {
            LoggerFactory.getLogger(HBCIUtils.class).warn("can not check account numbers with more than 10 digits (" + number + ")- skipping CRC check");
        }

        return ret;
    }

    /**
     * Use {@link #checkAccountCRCByAlg(String, String, String)} instead!
     *
     * @deprecated
     */
    public static boolean checkAccountCRCByAlg(String alg, String number) {
        return checkAccountCRCByAlg(alg, "", number);
    }


    /**
     * Überprüfen der Gültigkeit einer IBAN. Diese Methode prüft anhand eines
     * Prüfziffer-Algorithmus, ob die übergebene IBAN prinzipiell gültig ist.
     *
     * @return <code>false</code> wenn der Prüfzifferntest fehlschlägt, sonst
     * <code>true</code>
     */
    public static boolean checkIBANCRC(String iban) {
        return AccountCRCAlgs.checkIBAN(iban);
    }

    /**
     * Überprüfen der Gültigkeit einer Gläubiger-ID. Diese Methode prüft anhand eines
     * Prüfziffer-Algorithmus, ob die übergebene ID prinzipiell gültig ist.
     *
     * @param creditorId die zu pruefende Creditor-ID.
     * @return <code>false</code> wenn der Prüfzifferntest fehlschlägt, sonst
     * <code>true</code>
     */
    public static boolean checkCredtitorIdCRC(String creditorId) {
        return AccountCRCAlgs.checkCreditorId(creditorId);
    }

    private static void refreshBLZList(String blzpath)
            throws IOException {
        if (blzpath == null) {
            blzpath = "";
        }
        blzpath += "blz.properties";
        InputStream f = new FileInputStream(blzpath);

        if (f == null)
            throw new InvalidUserDataException(getLocMsg("EXCMSG_BLZLOAD", blzpath));

        refreshBLZList(f);
        f.close();
    }

    /**
     * Aktivieren einer neuen Bankenliste. Diese Methode kann aufgerufen
     * werden, um während der Laufzeit einer <em>HBCI4Java</em>-Anwendung
     * eine neue Bankenliste zu aktivieren. Die Bankenliste wird
     * aus dem übergebenen InputStream gelesen, welcher Daten im Format eines
     * Java-Properties-Files liefern muss. Das konkrete Format der Property-Einträge
     * der Bankenliste ist am Beispiel der bereits mitgelieferten Datei
     * <code>blz.properties</code> ersichtlich.
     *
     * @param in Eingabe-Stream, der für das Laden der Bankleitzahlen-Daten verwendet
     *           werden soll
     * @throws IOException
     **/
    public static void refreshBLZList(InputStream in)
            throws IOException {
        LoggerFactory.getLogger(HBCIUtils.class).debug("trying to load BLZ data");
        blzs.clear();
        blzs.load(in);

        banks.clear();
        for (Entry<Object, Object> e : blzs.entrySet()) {
            String blz = (String) e.getKey();
            String value = (String) e.getValue();

            BankInfo info = BankInfo.parse(value);
            info.setBlz(blz);
            banks.put(blz, info);
        }
    }

    /**
     * Konvertiert einen String in einen BigDecimal-Wert mit zwei Nachkommastellen.
     *
     * @param st String, der konvertiert werden soll (Format "<code>1234.56</code>");
     * @return BigDecimal-Wert
     */
    public static BigDecimal string2BigDecimal(String st) {
        BigDecimal result = new BigDecimal(st);
        result.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        return result;
    }

    /**
     * Konvertiert einen String in einen double-Wert (entspricht
     * <code>Double.parseDouble(st)</code>).
     *
     * @param st String, der konvertiert werden soll (Format "<code>1234.56</code>");
     * @return double-Wert
     * @deprecated use {@link #string2BigDecimal(String)}
     */
    @Deprecated
    public static double string2Value(String st) {
        return Double.parseDouble(st);
    }

    /**
     * Wandelt einen Double-Wert in einen String im Format "<code>1234.56</code>"
     * um (also ohne Tausender-Trennzeichen und mit "." als Dezimaltrennzeichen).
     *
     * @param value zu konvertierender Double-Wert
     * @return String-Darstellung dieses Wertes
     * @deprecated use {@link #bigDecimal2String(BigDecimal)}
     */
    @Deprecated
    public static String value2String(double value) {
        DecimalFormat format = new DecimalFormat("0.00");
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        format.setDecimalFormatSymbols(symbols);
        format.setDecimalSeparatorAlwaysShown(true);
        return format.format(value);
    }

    /**
     * Gibt die Versionsnummer der <em>HBCI4Java</em>-Bibliothek zurück.
     *
     * @return verwendete <em>HBCI4Java</em>-Version
     */
    public static String version() {
        return VERSION;
    }

    public static String bigDecimal2String(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("0.##");
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        format.setDecimalFormatSymbols(symbols);
        format.setDecimalSeparatorAlwaysShown(false);
        return format.format(value);
    }

    public static String getLocMsg(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException re) {
            // tolerieren wir
            LoggerFactory.getLogger(HBCIUtils.class).debug(re.getMessage(), re);
            return key;
        }
    }

    public static String getLocMsg(String key, Object o) {
        return getLocMsg(key, new Object[]{o});
    }

    public static String getLocMsg(String key, Object[] o) {
        return MessageFormat.format(getLocMsg(key), o);
    }

    public static long string2Long(String st, long factor) {
        BigDecimal result = new BigDecimal(st);
        result = result.multiply(new BigDecimal(factor));
        return result.longValue();
    }

    public static String withCounter(String st, int idx) {
        return st + ((idx != 0) ? "_" + Integer.toString(idx + 1) : "");
    }

    public static int getPosiOfNextDelimiter(String st, int posi) {
        int len = st.length();
        boolean quoting = false;
        while (posi < len) {
            char ch = st.charAt(posi);

            if (!quoting) {
                if (ch == '?') {
                    quoting = true;
                } else if (ch == '@') {
                    int endpos = st.indexOf('@', posi + 1);
                    String binlen_st = st.substring(posi + 1, endpos);
                    int binlen = Integer.parseInt(binlen_st);
                    posi += binlen_st.length() + 1 + binlen;
                } else if (ch == '\'' || ch == '+' || ch == ':') {
                    // Ende gefunden
                    break;
                }
            } else {
                quoting = false;
            }

            posi++;
        }

        return posi;
    }

    public static String stripLeadingZeroes(String st) {
        String ret = null;

        if (st != null) {
            int start = 0;
            int l = st.length();
            while (start < l && st.charAt(start) == '0') {
                start++;
            }
            ret = st.substring(start);
        }

        return ret;
    }

    public static Locale getLocale() {
        return Locale.getDefault();
    }
}
