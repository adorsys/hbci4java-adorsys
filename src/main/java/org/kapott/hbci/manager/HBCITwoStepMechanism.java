package org.kapott.hbci.manager;

import lombok.Data;
import org.kapott.hbci.exceptions.HBCI_Exception;

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

    public void setValue(String key, String value) {
        switch (key) {
            case "segversion":
                setSegversion(Integer.parseInt(value));
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
