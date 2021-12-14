/*  $Id: GVSaldoReq.java,v 1.1 2011/05/04 22:37:53 willuhn Exp $

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

import org.kapott.hbci.GV_Result.GVRSaldoReq;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Saldo;
import org.kapott.hbci.structures.Value;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public class GVSaldoReq extends AbstractHBCIJob {
    private static final Map<String, Function<String, String>> typeToRawResponseValueMapper;

    static {
        typeToRawResponseValueMapper = new HashMap<>();

        Function<String, String> mappingForCurrencyAndAmount = (type) -> {
            switch (type) {
                case "ready":
                    return "booked.BTG";
                case "unready":
                    return "pending.BTG";
                default:
                    return type;
            }
        };
        Function<String, String> mappingForRest = (type) -> {
            switch (type) {
                case "ready":
                    return "booked";
                case "unready":
                    return "pending";
                default:
                    return type;
            }
        };

        typeToRawResponseValueMapper.put("amount", mappingForCurrencyAndAmount);
        typeToRawResponseValueMapper.put("currency", mappingForCurrencyAndAmount);
        typeToRawResponseValueMapper.put("creditDebit", mappingForRest);
        typeToRawResponseValueMapper.put("date", mappingForRest);
        typeToRawResponseValueMapper.put("time", mappingForRest);
    }

    private static final BiFunction<String, String, String> AMOUNT_TEMPLATE = (response, type) -> String.format("%s.%s.value", response, typeToRawResponseValueMapper.get("amount").apply(type));
    private static final BiFunction<String, String, String> CURRENCY_TEMPLATE = (response, type) -> String.format("%s.%s.currency", response, typeToRawResponseValueMapper.get("currency").apply(type));
    private static final BiFunction<String, String, String> CREDIT_DEBIT_TEMPLATE = (response, type) -> String.format("%s.%s.CreditDebit", response, typeToRawResponseValueMapper.get("creditDebit").apply(type));
    private static final BiFunction<String, String, String> DATE_TEMPLATE = (response, type) -> String.format("%s.%s.date", response, typeToRawResponseValueMapper.get("date").apply(type));
    private static final BiFunction<String, String, String> TIME_TEMPLATE = (response, type) -> String.format("%s.%s.time", response, typeToRawResponseValueMapper.get("time").apply(type));

    public GVSaldoReq(HBCIPassportInternal passport, String name) {
        super(passport, name, new GVRSaldoReq(passport));
    }

    public GVSaldoReq(HBCIPassportInternal passport) {
        this(passport, getLowlevelName());

        addConstraint("my.country", "KTV.KIK.country", "DE");
        addConstraint("my.blz", "KTV.KIK.blz", null);
        addConstraint("my.number", "KTV.number", null);
        addConstraint("my.subnumber", "KTV.subnumber", "");
        addConstraint("my.curr", "curr", "EUR");
        addConstraint("dummyall", "allaccounts", "N");
        addConstraint("maxentries", "maxentries", "");
    }

    public static String getLowlevelName() {
        return "Saldo";
    }

    protected void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();
        GVRSaldoReq.Info info = new GVRSaldoReq.Info();

        info.konto = new Konto();
        info.konto.country = result.get(header + ".KTV.KIK.country");
        info.konto.blz = result.get(header + ".KTV.KIK.blz");
        info.konto.number = result.get(header + ".KTV.number");
        info.konto.subnumber = result.get(header + ".KTV.subnumber");
        info.konto.bic = result.get(header + ".KTV.bic");
        info.konto.iban = result.get(header + ".KTV.iban");
        info.konto.type = result.get(header + ".kontobez");
        info.konto.curr = result.get(header + ".curr");
        passport.fillAccountInfo(info.konto);

        //TODO: Put all relevant fields - amount, currency, credit, date and time into an object to be parsed of the map
        info.ready = createSaldo(result, AMOUNT_TEMPLATE.apply(header, "ready"),
            CURRENCY_TEMPLATE.apply(header, "ready"),
            CREDIT_DEBIT_TEMPLATE.apply(header, "ready"),
            DATE_TEMPLATE.apply(header, "ready"),
            TIME_TEMPLATE.apply(header, "ready"));

        Optional.ofNullable(result.get(header + ".pending.CreditDebit")).ifPresent(creditDebit -> {
            info.unready = createSaldo(result,
                AMOUNT_TEMPLATE.apply(header, "unready"),
                CURRENCY_TEMPLATE.apply(header, "unready"),
                CREDIT_DEBIT_TEMPLATE.apply(header, "unready"),
                DATE_TEMPLATE.apply(header, "unready"),
                TIME_TEMPLATE.apply(header, "unready"));
        });

        info.kredit = createValue(result,
            AMOUNT_TEMPLATE.apply(header, "kredit"),
            CURRENCY_TEMPLATE.apply(header, "kredit"),
            false);
        info.available = createValue(result,
            AMOUNT_TEMPLATE.apply(header, "available"),
            CURRENCY_TEMPLATE.apply(header, "available"),
            false);
        info.used = createValue(result,
            AMOUNT_TEMPLATE.apply(header, "used"),
            AMOUNT_TEMPLATE.apply(header, "used"),
            false);

        retrieveReservedBalanceInfoFromUPD().ifPresent(reservedBalanceInfo -> {
            //TODO: Edge case check if amount is number
            Optional.ofNullable(reservedBalanceInfo.get("Balance.VOR.Amount")).ifPresent(
                amount -> {
                    amount = amount.replace(",", ".");

                    info.reserved = new Saldo();
                    info.reserved.value = new Value(amount, reservedBalanceInfo.getOrDefault("Balance.VOR.Currency", "EUR"));

                    //TODO: Shouldn't be in the past
                    Optional.ofNullable(reservedBalanceInfo.get("Balance.VOR.Date")).ifPresent(
                        date -> info.reserved.timestamp = HBCIUtils.string2DateISO(reservedBalanceInfo.get("Balance.VOR.Date"), "yyyyMMdd")
                    );
                }
            );
        });

        ((GVRSaldoReq) (jobResult)).store(info);
    }

    private boolean negativeSaldo(Map<String, String> result, String creditDebitKey) {
        return result.get(creditDebitKey).equals("D");
    }

    private Saldo createSaldo(Map<String, String> result, String amountKey, String currencyKey, String creditDebitKey, String dateKey, String timeKey) {
        Saldo saldo = new Saldo();
        saldo.value = createValue(result, amountKey, currencyKey, negativeSaldo(result, creditDebitKey));
        saldo.timestamp = HBCIUtils.strings2DateTimeISO(result.get(dateKey), result.get(timeKey));
        return saldo;
    }

    private Value createValue(Map<String, String> result, String amountKey, String currencyKey, boolean negative) {
        return Optional.ofNullable(result.get(amountKey)).map(amount -> {
            String currencyValue = result.get(currencyKey);
            String possibleNegativeAmount = (negative ? "-" : "") + amount;
            if (checkCurrency(currencyValue)) {
                return new Value(possibleNegativeAmount, currencyValue);
            }
            return new Value(possibleNegativeAmount);
        }).orElse(null);
    }

    private boolean checkCurrency(String currencyCode) {
        try {
            Currency.getInstance(currencyCode);
        } catch (Exception any) {
            return false;
        }
        return true;
    }

    private Optional<Map<String, String>> retrieveReservedBalanceInfoFromUPD() {
        return Optional.ofNullable(passport.getUPD().get("KInfo.accountdata")).map(accountData -> {
                final var reservedBalancePattern = Pattern.compile("(Balance\\.VOR\\..*?)=(.*?(?=;))");
                final var matcher = reservedBalancePattern.matcher(accountData);

                Map<String, String> reservedBalanceInfo = new HashMap<>();
                while (matcher.find()) {
                    final var key = matcher.group(1);
                    final var value = matcher.group(2);
                    reservedBalanceInfo.put(key, value);
                }

                return reservedBalanceInfo;
            }
        );
    }

    public void verifyConstraints() {
        super.verifyConstraints();
        checkAccountCRC("my");
    }
}
