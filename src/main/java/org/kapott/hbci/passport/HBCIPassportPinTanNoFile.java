package org.kapott.hbci.passport;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.manager.BankInfo;
import org.kapott.hbci.manager.HBCIUtils;

import java.net.URL;
import java.util.Properties;

public class HBCIPassportPinTanNoFile extends HBCIPassportPinTan {

    public HBCIPassportPinTanNoFile(Properties properties, HBCICallback callback, Object initObject) {
        super(properties, callback, initObject);
    }

    @Override
    protected void read() {
        setCountry(properties.getProperty("client.passport.country"));
        setBLZ(properties.getProperty("client.passport.blz"));
        setCustomerId(properties.getProperty("client.passport.customerId"));
        if (properties.getProperty("client.passport.userId") != null) {
            setUserId(properties.getProperty("client.passport.userId"));
        } else {
            setUserId(getCustomerId());
        }

        BankInfo bankInfo = HBCIUtils.getBankInfo(getBLZ());
        setHBCIVersion(bankInfo.getPinTanVersion().getId());
        setFilterType("Base64");

        String url = bankInfo.getPinTanAddress();
        String proxyPrefix = System.getProperty("proxyPrefix", null);
        if (proxyPrefix != null) {
            url = proxyPrefix+url;
        }
        setHost(url);
    }

    @Override
    public void saveChanges() {}
}
