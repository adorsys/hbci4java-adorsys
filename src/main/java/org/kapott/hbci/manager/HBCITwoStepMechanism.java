package org.kapott.hbci.manager;

import lombok.Data;

import java.lang.reflect.Field;

@Data
public class HBCITwoStepMechanism {

    private String id;
    private String secfunc;
    private int segversion;
    private int process;
    private String name;
    private String inputinfo;
    private int nofactivetanmedia = 0;
    private String needtanmedia = "";
    private String needchallengeklass;
    private String needchallengevalue;
    private String zkamethod_name;
    private String zkamethod_version;
    private String medium;
    private int timeoutDecoupledFirstStatusRequest;
    private int timeoutDecoupledNextStatusRequest;
    private int maxDecoupledStatusRequests;

    public void setValue(String key, String value) {
        switch (key) {
            case "segversion":
                setSegversion(Integer.parseInt(value));
                break;
            case "timeoutDecoupledFirstStatusRequest":
                setTimeoutDecoupledFirstStatusRequest(Integer.parseInt(value));
                break;
            case "timeoutDecoupledNextStatusRequest":
                setTimeoutDecoupledNextStatusRequest(Integer.parseInt(value));
                break;
            case "maxDecoupledStatusRequests":
                setMaxDecoupledStatusRequests(Integer.parseInt(value));
                break;
            case "process":
                setProcess(Integer.parseInt(value));
                break;
            case "nofactivetanmedia":
                setNofactivetanmedia(Integer.parseInt(value));
                break;
            default:
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    f.set(this, value);
                } catch (NoSuchFieldException | IllegalAccessException ex) {
                    //ignore
                }
        }

    }
}
