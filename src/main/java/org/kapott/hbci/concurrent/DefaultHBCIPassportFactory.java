package org.kapott.hbci.concurrent;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;

import java.util.Properties;

/**
 * Standard-Implementierung, die das Passport über {@link AbstractHBCIPassport#getInstance(String, Object)}
 * erzeugt.
 *
 * @author Hendrik Schnepel
 */
public class DefaultHBCIPassportFactory implements HBCIPassportFactory
{

    private final String name;
    private final Object init;

    public DefaultHBCIPassportFactory(String name)
    {
        this(name, null);
    }

    public DefaultHBCIPassportFactory(Object init)
    {
        this(null, init);
    }

    public DefaultHBCIPassportFactory(String name, Object init)
    {
        this.name = name;
        this.init = init;
    }

    @Override
    public HBCIPassport createPassport(HBCICallback callback) throws Exception
    {
        if (name == null)
        {
            return AbstractHBCIPassport.getInstance(callback, new Properties(), init);
        }
        else
        {
            return AbstractHBCIPassport.getInstance(callback, new Properties(), name, init);
        }
    }

}
