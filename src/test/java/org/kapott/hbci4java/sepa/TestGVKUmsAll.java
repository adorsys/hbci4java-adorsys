package org.kapott.hbci4java.sepa;

import org.junit.Test;
import org.kapott.hbci.GV.AbstractHBCIJob;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci4java.AbstractTestGV;

import java.util.Properties;

/**
 * Testet die Abrufen der Umsaetze.
 */
public class TestGVKUmsAll extends AbstractTestGV {
    /**
     * Testet das Abrufen der Umsaetze.
     */
    @Test
    public void test() {
        this.execute(new Execution() {

            @Override
            public String getJobname() {
                return "KUmsAll";
            }

            @Override
            public void configure(AbstractHBCIJob job, HBCIPassport passport, Properties params) {
                super.configure(job, passport, params);
                job.setParam("my.blz", params.getProperty("blz", System.getProperty("blz")));
                job.setParam("my.number", params.getProperty("number", System.getProperty("number")));

                String start = params.getProperty("startdate", System.getProperty("startdate"));
                if (start != null)
                    job.setParam("startdate", start);

                String end = params.getProperty("enddate", System.getProperty("enddate"));
                if (end != null)
                    job.setParam("enddate", end);
            }
        });
    }
}
