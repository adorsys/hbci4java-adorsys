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
        addConstraint("mediatype", "mediatype", "0"); // "1" gibts nicht. Siehe FinTS_3.0_Security_Sicherheitsverfahren_PINTAN_Rel_20101027_final_version.pdf "TAN-Medium-Art"
        addConstraint("mediacategory", "mediacategory", "A");
    }

    public static String getLowlevelName() {
        return "TANMediaList";
    }

    public void extractResults(HBCIMsgStatus msgstatus, String header, int idx) {
        HashMap<String, String> result = msgstatus.getData();

        String s = result.get(header + ".tanoption");
        if (s != null) {
            ((GVRTANMediaList) jobResult).setTanOption(Integer.parseInt(s));
        }

        // Da drin speichern wir die Namen der TAN-Medien - kommt direkt in die UPD im Passport
        StringBuffer mediaNames = new StringBuffer();

        for (int i = 0; ; i++) {
            String mediaheader = HBCIUtils.withCounter(header + ".MediaInfo", i);

            String st = result.get(mediaheader + ".mediacategory");
            if (st == null)
                break;

            GVRTANMediaList.TANMediaInfo info = new GVRTANMediaList.TANMediaInfo();

            info.mediaCategory = st;
            info.cardNumber = result.get(mediaheader + ".cardnumber");
            info.cardSeqNumber = result.get(mediaheader + ".cardseqnumber");
            info.mediaName = result.get(mediaheader + ".medianame");
            info.mobileNumber = result.get(mediaheader + ".mobilenumber");
            info.mobileNumberSecure = result.get(mediaheader + ".mobilenumber_secure");
            info.status = result.get(mediaheader + ".status");
            info.tanListNumber = result.get(mediaheader + ".tanlistnumber");

            st = result.get(mediaheader + ".freetans");
            if (st != null) info.freeTans = Integer.parseInt(st);

            st = result.get(mediaheader + ".cardtype");
            if (st != null) info.cardType = Integer.parseInt(st);

            st = result.get(mediaheader + ".validfrom");
            if (st != null) {
                info.validFrom = HBCIUtils.string2DateISO(st);
            }

            st = result.get(mediaheader + ".validto");
            if (st != null) {
                info.validTo = HBCIUtils.string2DateISO(st);
            }

            st = result.get(mediaheader + ".lastuse");
            if (st != null) {
                info.lastUse = HBCIUtils.string2DateISO(st);
            }

            st = result.get(mediaheader + ".activatedon");
            if (st != null) {
                info.activatedOn = HBCIUtils.string2DateISO(st);
            }

            ((GVRTANMediaList) jobResult).add(info);

            // Es gibt auch noch "verfuegbar", da muss das Medium aber erst noch freigeschaltet werden
            boolean isActive = info.status != null && info.status.equals("1");
            boolean haveName = info.mediaName != null && info.mediaName.length() > 0;
            // boolean isMobileTan = info.mediaCategory != null && info.mediaCategory.equalsIgnoreCase("M");

            // Zu den UPD hinzufuegen
            if (isActive && haveName) {
                if (mediaNames.length() != 0)
                    mediaNames.append("|");

                mediaNames.append(info.mediaName);
            }
        }

        String names = mediaNames.toString();
        if (names.length() > 0) {
            log.info("adding TAN media names to UPD: " + names);
            ;
            passport.getUPD().put("tanmedia.names", names);
        }
    }

}
