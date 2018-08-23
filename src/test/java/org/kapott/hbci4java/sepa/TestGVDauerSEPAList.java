package org.kapott.hbci4java.sepa;

import org.junit.Test;
import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci4java.AbstractTestGV;

import java.util.Properties;

/**
 * Testet das Abrufen der SEPA-Dauerauftraege.
 */
public class TestGVDauerSEPAList extends AbstractTestGV {
    /**
     * Testet das Ausfuehren einer SEPA-Lastschrift.
     */
    @Test
    public void test() {
        this.execute(new Execution() {

            @Override
            public String getJobname() {
                return "DauerSEPAList";
            }

            /**
             * @see org.kapott.hbci4java.AbstractTestGV.Execution#configure(org.kapott.hbci.GV.AbstractHBCIJob, org.kapott.hbci.passport.HBCIPassport, java.util.Properties)
             */
            @Override
            public void configure(AbstractHBCIJob job, HBCIPassport passport, Properties params) {
                super.configure(job, passport, params);
                job.setParam("my.bic", params.getProperty("bic", System.getProperty("bic")));
                job.setParam("my.iban", params.getProperty("iban", System.getProperty("iban")));
            }
        });
    }
}
