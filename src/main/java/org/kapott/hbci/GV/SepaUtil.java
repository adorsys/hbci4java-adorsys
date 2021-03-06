package org.kapott.hbci.GV;

import org.kapott.hbci.exceptions.InvalidArgumentException;
import org.kapott.hbci.structures.Value;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ein paar statische Hilfs-Methoden fuer die Generierung der SEPA-Nachrichten.
 */
public class SepaUtil {

    /**
     * Das Platzhalter-Datum, welches verwendet werden soll, wenn kein Datum angegeben ist.
     */
    public static final String DATE_UNDEFINED = "1999-01-01";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\w+\\[(\\d+)\\](\\..*)?");

    /**
     * Erzeugt ein neues XMLCalender-Objekt.
     *
     * @param isoDate optional. Das zu verwendende Datum.
     *                Wird es weggelassen, dann wird das aktuelle Datum (mit Uhrzeit) verwendet.
     * @return das XML-Calendar-Objekt.
     */
    public static XMLGregorianCalendar createCalendar(String isoDate) {
        if (isoDate == null) {
            SimpleDateFormat format = new SimpleDateFormat(DATETIME_FORMAT);
            isoDate = format.format(new Date());
        }

        DatatypeFactory df = null;
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        return df.newXMLGregorianCalendar(isoDate);
    }

    /**
     * Formatiert den XML-Kalender im angegebenen Format.
     *
     * @param cal    der Kalender.
     * @param format das zu verwendende Format. Fuer Beispiele siehe
     *               {@link SepaUtil#DATE_FORMAT}
     *               {@link SepaUtil#DATETIME_FORMAT}
     *               Wenn keines angegeben ist, wird per Default {@link SepaUtil#DATE_FORMAT} verwendet.
     * @return die String das formatierte Datum.
     */
    public static String format(XMLGregorianCalendar cal, String format) {
        if (cal == null)
            return null;

        if (format == null)
            format = DATE_FORMAT;

        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(cal.toGregorianCalendar().getTime());
    }

    /**
     * Liefert ein Date-Objekt fuer den Kalender.
     *
     * @param cal der Kalender.
     * @return das Date-Objekt.
     */
    public static Date toDate(XMLGregorianCalendar cal) {
        if (cal == null)
            return null;

        return cal.toGregorianCalendar().getTime();
    }

    /**
     * Formatiert die Dezimalzahl als String.
     * Zur Zeit macht die Funktion lediglich ein "toString",
     *
     * @param value der zu formatierende Betrag.
     * @return der formatierte Betrag.
     */
    public static String format(BigDecimal value) {
        return value != null ? value.toString() : null;
    }

    /**
     * Ermittelt den maximalen Index aller indizierten Properties. Nicht indizierte Properties
     * werden ignoriert.
     *
     * @param properties die Properties, mit denen gearbeitet werden soll
     * @return Maximaler Index, oder {@code null}, wenn keine indizierten Properties gefunden wurden
     */
    public static Integer maxIndex(Map<String, String> properties) {
        Integer max = null;
        for (String key : properties.keySet()) {
            Matcher m = INDEX_PATTERN.matcher(key);
            if (m.matches()) {
                int index = Integer.parseInt(m.group(1));
                if (max == null || index > max) {
                    max = index;
                }
            }
        }
        return max;
    }

    /**
     * Liefert die Summe der Beträge aller Transaktionen. Bei einer Einzeltransaktion wird der
     * Betrag zurückgeliefert. Mehrfachtransaktionen müssen die gleiche Währung verwenden, da
     * eine Summenbildung sonst nicht möglich ist.
     *
     * @param sepaParams die Properties, mit denen gearbeitet werden soll
     * @param max        Maximaler Index, oder {@code null} für Einzeltransaktionen
     * @return Summe aller Beträge
     */
    public static BigDecimal sumBtgValue(Map<String, String> sepaParams, Integer max) {
        if (max == null)
            return new BigDecimal(sepaParams.get("btg.value"));

        BigDecimal sum = BigDecimal.ZERO;
        String curr = null;

        for (int index = 0; index <= max; index++) {
            sum = sum.add(new BigDecimal(sepaParams.get(insertIndex("btg.value", index))));

            // Sicherstellen, dass alle Transaktionen die gleiche Währung verwenden
            String indexCurr = sepaParams.get(insertIndex("btg.curr", index));
            if (curr != null) {
                if (!curr.equals(indexCurr)) {
                    throw new InvalidArgumentException("mixed currencies on multiple transactions");
                }
            } else {
                curr = indexCurr;
            }
        }
        return sum;
    }

    /**
     * Fuegt einen Index in den Property-Key ein. Wurde kein Index angegeben, wird der Key
     * unveraendert zurueckgeliefert.
     *
     * @param key   Key, der mit einem Index ergaenzt werden soll
     * @param index Index oder {@code null}, wenn kein Index gesetzt werden soll
     * @return Key mit Index
     */
    public static String insertIndex(String key, Integer index) {
        if (index == null)
            return key;

        int pos = key.indexOf('.');
        if (pos >= 0) {
            return key.substring(0, pos) + '[' + index + ']' + key.substring(pos);
        } else {
            return key + '[' + index + ']';
        }
    }

    /**
     * Liefert ein Value-Objekt mit den Summen des Auftrages.
     *
     * @param properties Auftrags-Properties.
     * @return das Value-Objekt mit der Summe.
     */
    public static Value sumBtgValueObject(Map<String, String> properties) {
        Integer maxIndex = maxIndex(properties);
        BigDecimal btg = sumBtgValue(properties, maxIndex);
        String curr = properties.get(insertIndex("btg.curr", maxIndex == null ? null : 0));
        return new Value(btg, curr);
    }

    /**
     * Liefert den Wert des Properties oder den Default-Wert.
     * Der Default-Wert wird nicht nur bei NULL verwendet sondern auch bei Leerstring.
     *
     * @param props        die Properties.
     * @param name         der Name des Properties.
     * @param defaultValue der Default-Wert.
     * @return der Wert.
     */
    public static String getProperty(Map<String, String> props, String name, String defaultValue) {
        String value = props.get(name);
        return value != null && value.length() > 0 ? value : defaultValue;
    }
}


