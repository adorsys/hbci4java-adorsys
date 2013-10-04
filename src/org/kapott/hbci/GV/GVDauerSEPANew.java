package org.kapott.hbci.GV;

import java.text.DecimalFormat;
import java.util.Properties;

import org.kapott.hbci.exceptions.InvalidUserDataException;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.manager.LogFilter;
import org.kapott.hbci.sepa.PainVersion;
import org.kapott.hbci.sepa.PainVersion.Type;

public class GVDauerSEPANew extends AbstractSEPAGV {

    private final static PainVersion DEFAULT = PainVersion.PAIN_001_001_02;
    
    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getDefaultPainVersion()
     */
    @Override
    protected PainVersion getDefaultPainVersion()
    {
        return DEFAULT;
    }

    /**
     * @see org.kapott.hbci.GV.AbstractSEPAGV#getPainType()
     */
    @Override
    protected Type getPainType()
    {
        return Type.PAIN_001;
    }
    
    /**
     * Liefert den Lowlevel-Namen des Jobs.
     * @return der Lowlevel-Namen des Jobs.
     */
    public static String getLowlevelName()
    {
        return "DauerSEPANew";
    }

    public GVDauerSEPANew(HBCIHandler handler) {
        super(handler,getLowlevelName());

        addConstraint("src.bic",  "My.bic",  null, LogFilter.FILTER_MOST);
        addConstraint("src.iban", "My.iban", null, LogFilter.FILTER_IDS);
        addConstraint("_sepadescriptor", "sepadescr", this.getPainVersion().getURN(), LogFilter.FILTER_NONE);
        addConstraint("_sepapain",       "sepapain", null, LogFilter.FILTER_IDS);

        /* dummy constraints to allow an application to set these values. the
         * overriden setLowlevelParam() stores these values in a special structure
         * which is later used to create the SEPA pain document. */
        addConstraint("src.bic",   "sepa.src.bic",   null, LogFilter.FILTER_MOST);
        addConstraint("src.iban",  "sepa.src.iban",  null, LogFilter.FILTER_IDS);
        addConstraint("src.name",  "sepa.src.name",  null, LogFilter.FILTER_IDS);
        addConstraint("dst.bic",   "sepa.dst.bic",   null, LogFilter.FILTER_MOST);
        addConstraint("dst.iban",  "sepa.dst.iban",  null, LogFilter.FILTER_IDS);
        addConstraint("dst.name",  "sepa.dst.name",  null, LogFilter.FILTER_IDS);
        addConstraint("btg.value", "sepa.btg.value", null, LogFilter.FILTER_NONE);
        addConstraint("btg.curr",  "sepa.btg.curr",  "EUR", LogFilter.FILTER_NONE);
        addConstraint("usage",     "sepa.usage",     null, LogFilter.FILTER_NONE);
      
        //Constraints f�r die PmtInfId (eindeutige SEPA Message ID) und EndToEndId (eindeutige ID um Transaktion zu identifizieren)
        addConstraint("sepaid",    "sepa.sepaid",      getSEPAMessageId(),      LogFilter.FILTER_NONE);
        addConstraint("endtoendid", "sepa.endtoendid", ENDTOEND_ID_NOTPROVIDED, LogFilter.FILTER_NONE);
        
        // DauerDetails
        addConstraint("firstdate","DauerDetails.firstdate",null, LogFilter.FILTER_NONE);
        addConstraint("timeunit","DauerDetails.timeunit",null, LogFilter.FILTER_NONE);
        addConstraint("turnus","DauerDetails.turnus",null, LogFilter.FILTER_NONE);
        addConstraint("execday","DauerDetails.execday",null, LogFilter.FILTER_NONE);
        addConstraint("lastdate","DauerDetails.lastdate","", LogFilter.FILTER_NONE);
    }
    
    public void setParam(String paramName,String value)
    {
        Properties res=getJobRestrictions();
        
        if (paramName.equals("timeunit")) {
            if (!(value.equals("W") || value.equals("M"))) {
                String msg=HBCIUtilsInternal.getLocMsg("EXCMSG_INV_TIMEUNIT",value);
                if (!HBCIUtilsInternal.ignoreError(getMainPassport(),"client.errors.ignoreWrongJobDataErrors",msg))
                    throw new InvalidUserDataException(msg);
            }
        } else if (paramName.equals("turnus")) {
            String timeunit=getLowlevelParams().getProperty(getName()+".DauerDetails.timeunit");
            
            if (timeunit!=null) {
                if (timeunit.equals("W")) {
                    String st=res.getProperty("turnusweeks");
                    
                    if (st!=null) {
                        String value2=new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2,st)) {
                            String msg=HBCIUtilsInternal.getLocMsg("EXCMSG_INV_TURNUS",value);
                            if (!HBCIUtilsInternal.ignoreError(getMainPassport(),"client.errors.ignoreWrongJobDataErrors",msg))
                                throw new InvalidUserDataException(msg);
                        }
                    }
                } else if (timeunit.equals("M")) {
                    String st=res.getProperty("turnusmonths");

                    if (st!=null) {
                        String value2=new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2,st)) {
                            String msg=HBCIUtilsInternal.getLocMsg("EXCMSG_INV_TURNUS",value);
                            if (!HBCIUtilsInternal.ignoreError(getMainPassport(),"client.errors.ignoreWrongJobDataErrors",msg))
                                throw new InvalidUserDataException(msg);
                        }
                    }
                }
            }
        } else if (paramName.equals("execday")) {
            String timeunit=getLowlevelParams().getProperty(getName()+".DauerDetails.timeunit");

            if (timeunit!=null) {
                if (timeunit.equals("W")) {
                    String st=res.getProperty("daysperweek");

                    if (st!=null && !st.equals("0") && st.indexOf(value)==-1) {
                        String msg=HBCIUtilsInternal.getLocMsg("EXCMSG_INV_EXECDAY",value);
                        if (!HBCIUtilsInternal.ignoreError(getMainPassport(),"client.errors.ignoreWrongJobDataErrors",msg))
                            throw new InvalidUserDataException(msg);
                    }
                } else if (timeunit.equals("M")) {
                    String st=res.getProperty("dayspermonth");

                    if (st!=null) {
                        String value2=new DecimalFormat("00").format(Integer.parseInt(value));

                        if (!st.equals("00") && !twoDigitValueInList(value2,st)) {
                            String msg=HBCIUtilsInternal.getLocMsg("EXCMSG_INV_EXECDAY",value);
                            if (!HBCIUtilsInternal.ignoreError(getMainPassport(),"client.errors.ignoreWrongJobDataErrors",msg))
                                throw new InvalidUserDataException(msg);
                        }
                    }
                }
            }
        } else if (paramName.equals("key")) {
            boolean atLeastOne=false;
            boolean found=false;
            
            for (int i=0;;i++) {
                String st=res.getProperty(HBCIUtilsInternal.withCounter("textkey",i));
                
                if (st==null)
                    break;
                
                atLeastOne=true;
                
                if (st.equals(value)) {
                    found=true;
                    break;
                }
            }
            
            if (atLeastOne && !found) {
                String msg=HBCIUtilsInternal.getLocMsg("EXCMSG_INV_KEY",value);
                if (!HBCIUtilsInternal.ignoreError(getMainPassport(),"client.errors.ignoreWrongJobDataErrors",msg))
                    throw new InvalidUserDataException(msg);
            }
        }
        
        super.setParam(paramName,value);
    }
    
    public String getPainJobName() {
        return "UebSEPA";
    }
}