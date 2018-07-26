package org.kapott.hbci.GV_Result;

import org.kapott.hbci.passport.HBCIPassportInternal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GVRTANMediaList extends HBCIJobResultImpl {

    private List<TANMediaInfo> mediaList = new ArrayList<>();
    private Integer tanOption = -1;

    public GVRTANMediaList(HBCIPassportInternal passport) {
        super(passport);
    }

    public void add(TANMediaInfo info) {
        mediaList.add(info);
    }

    public List<TANMediaInfo> mediaList() {
        return mediaList;
    }

    public Integer getTanOption() {
        return tanOption;
    }

    public void setTanOption(Integer option) {
        tanOption = option;
    }

    public static class TANMediaInfo implements Serializable {
        public String mediaCategory;
        public String status;
        public String cardNumber;
        public String cardSeqNumber;
        public Integer cardType;
        public Date validFrom;
        public Date validTo;
        public String tanListNumber;
        public String mediaName;
        public String mobileNumber;
        public String mobileNumberSecure;
        public Integer freeTans;
        public Date lastUse;
        public Date activatedOn;
    }
}
