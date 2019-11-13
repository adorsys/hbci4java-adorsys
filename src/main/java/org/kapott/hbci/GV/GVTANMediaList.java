package org.kapott.hbci.GV;

import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV_Result.GVRTANMediaList;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassportInternal;
import org.kapott.hbci.status.HBCIMsgStatus;

import java.util.HashMap;

@Slf4j
public class GVTANMediaList extends AbstractHBCIJob {

    public GVTANMediaList(HBCIPassportInternal passport) {
        super(passport, getLowlevelName(), new GVRTANMediaList(passport));
        addConstraint("mediatype", "mediatype", "0"); // "1" gibts nicht. Siehe FinTS_3
        // .0_Security_Sicherheitsverfahren_PINTAN_Rel_20101027_final_version.pdf "TAN-Medium-Art"
        addConstraint("mediacategory", "mediacategory", "A");
    }

    public static String getLowlevelName() {
        return "TANMediaList";
    }

    @Override
    public void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();

        String tanOption = result.get(header + ".tanoption");
        if (tanOption != null) {
            ((GVRTANMediaList) jobResult).setTanOption(Integer.parseInt(tanOption));
        }

        for (int i = 0; ; i++) {
            String mediaheader = HBCIUtils.withCounter(header + ".MediaInfo", i);

            tanOption = result.get(mediaheader + ".mediacategory");
            if (tanOption == null)
                break;

            GVRTANMediaList.TANMediaInfo info = new GVRTANMediaList.TANMediaInfo();

            info.mediaCategory = tanOption;
            info.cardNumber = result.get(mediaheader + ".cardnumber");
            info.cardSeqNumber = result.get(mediaheader + ".cardseqnumber");
            info.mediaName = result.get(mediaheader + ".medianame");
            info.mobileNumber = result.get(mediaheader + ".mobilenumber");
            info.mobileNumberSecure = result.get(mediaheader + ".mobilenumber_secure");
            info.status = result.get(mediaheader + ".status");
            info.tanListNumber = result.get(mediaheader + ".tanlistnumber");

            tanOption = result.get(mediaheader + ".freetans");
            if (tanOption != null) info.freeTans = Integer.parseInt(tanOption);

            tanOption = result.get(mediaheader + ".cardtype");
            if (tanOption != null) info.cardType = Integer.parseInt(tanOption);

            tanOption = result.get(mediaheader + ".validfrom");
            if (tanOption != null) {
                info.validFrom = HBCIUtils.string2DateISO(tanOption);
            }

            tanOption = result.get(mediaheader + ".validto");
            if (tanOption != null) {
                info.validTo = HBCIUtils.string2DateISO(tanOption);
            }

            tanOption = result.get(mediaheader + ".lastuse");
            if (tanOption != null) {
                info.lastUse = HBCIUtils.string2DateISO(tanOption);
            }

            tanOption = result.get(mediaheader + ".activatedon");
            if (tanOption != null) {
                info.activatedOn = HBCIUtils.string2DateISO(tanOption);
            }


            // Es gibt auch noch "verfuegbar", da muss das Medium aber erst noch freigeschaltet werden
            boolean isActive = info.status != null && info.status.equals("1");
            boolean haveName = info.mediaName != null && info.mediaName.length() > 0;
            // boolean isMobileTan = info.mediaCategory != null && info.mediaCategory.equalsIgnoreCase("M");

            if (isActive && haveName) {
                log.info("adding TAN media: " + info.mediaName);
                ((GVRTANMediaList) jobResult).add(info);
            }
        }
    }

}
