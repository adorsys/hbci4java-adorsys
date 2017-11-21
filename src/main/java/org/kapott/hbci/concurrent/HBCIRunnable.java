package org.kapott.hbci.concurrent;


import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.passport.HBCIPassport;

import java.util.Properties;

public abstract class HBCIRunnable
{

    private final Properties properties;
    private final HBCICallback callback;
    private HBCIPassportFactory passportFactory;

    protected HBCIPassport passport = null;
    protected HBCIHandler handler = null;

    public HBCIRunnable(Properties properties, HBCICallback callback, HBCIPassportFactory passportFactory)
    {
        this.properties = properties;
        this.callback = callback;
        this.passportFactory = passportFactory;
    }



}
