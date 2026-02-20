package com.bloomreach.analyticssimulator.build.pixellog;

// abstract base class for all simulatePixel classes
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Base64;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.PixelLogParam;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.SimulatorConfig;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;

public abstract class BuildPixelBase {

    protected BuildPixelBase () {
    }

    // common fields to simulate in ALL pixels
    protected int setCommonFields (PixelLog.Builder pixelLogBuilder, UidToSegmentRecord uidToSegmentRecord, long logTime) throws Exception {
        String uid;
        String segment;
        String cookie2;
        String dateStr;
        Date currentDate;
        SimpleDateFormat dateFormatter;
    
        // collect segment for given uid
        uid = uidToSegmentRecord.getUid ();
        segment = uidToSegmentRecord.getSegment (); // may be null OR "NONE" if this acct does not use RTCS or RBS segments

        // fill up necessary fields in the template

        // acctId
        pixelLogBuilder.setAcctId (SimulatorConfig.getConfigParam ("ACCOUNT_ID"));

        // domainKey
        pixelLogBuilder.setDomain (SimulatorConfig.getConfigParam ("DOMAIN"));
        replacePixelLogParam (pixelLogBuilder, "domain_key", SimulatorConfig.getConfigParam ("DOMAIN"));

        // link (Not sure what this value should be - for now, set to HOME_PAGE url
        pixelLogBuilder.setLink (SimulatorConfig.getConfigParam ("HOMEPAGE_URL"));

        // uid
        pixelLogBuilder.setUid (uid);

        // cookie2
        cookie2 = generateCookieString (uid, segment, Long.toString (logTime));
        pixelLogBuilder.setCookie2 (cookie2);
        replacePixelLogParam (pixelLogBuilder, "cookie2", cookie2);

        // log time
        pixelLogBuilder.setLogTime (logTime);

        // corresponding 'date'
        currentDate = new Date (logTime);
        dateFormatter = new SimpleDateFormat ("yyyyMMdd"); // year,month,day
        dateStr = dateFormatter.format (currentDate);
        pixelLogBuilder.setDate (dateStr);

        // If this acct uses RTCS segments then....
        if (segment != null) {
            if (SimulatorConfig.getSegmentationType () == SimulatorConstants.SEGMENTATION_TYPE_RTCS) {
                String cdpSegment;

                // in RTCS world, cdpSegment is included in ALL pixels
                // eg, segment = budget / luxury
                // Map to corresponding cdp_segments values
                cdpSegment = SimulatorConfig.getRTCSKeyValuePair (segment);
                if (cdpSegment == null)
                    return (SimulatorConstants.SIMULATE_STATUS_ERROR);
                pixelLogBuilder.setCdpSegments (cdpSegment); // value like: 123213:213213
            } else if (SimulatorConfig.getSegmentationType () == SimulatorConstants.SEGMENTATION_TYPE_RBS) {
                String rbsCustomerProfile;

                rbsCustomerProfile = SimulatorConfig.getRBSCustomerProfileValue (segment);
                if (rbsCustomerProfile == null)
                    return (SimulatorConstants.SIMULATE_STATUS_ERROR);
                pixelLogBuilder.setCustomerProfile (rbsCustomerProfile); // value like: "Milwaukee"
            }
        }

        // view if it is != NONE
        if (uidToSegmentRecord.getView ().equals ("NONE") == false) {
            addPixelLogParam (pixelLogBuilder, "view_id", uidToSegmentRecord.getView());
        }

        return (SimulatorConstants.SIMULATE_STATUS_OK);
    }


    // replace an existing parameter's value
    // first obtain entire list of params in the pixel. Then look for specified key's index
    // and use that to replace value. Looks like the "setParams" method does not have a
    // overloaded method where it takes a paramKey as an argument
    protected boolean replacePixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey, String paramValue) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
                paramBuilder.setKey(paramKey);
                paramBuilder.setValue(paramValue);
                PixelLogParam newPixelLogParam = paramBuilder.build();

                pixelLogBuilder.setParams(i, newPixelLogParam);

                return (true);
            }
        }

        // specified paramKey not found in current paramsList
        return (false);
    }

    /**
     * Add PixelLogParam corresponding to the passed parameter key-value pair to the pixel builder.
     * @param pixelBuilder
     * @param paramKey
     * @param paramValue
     */
    protected void addPixelLogParam (PixelLog.Builder pixelBuilder, String paramKey, String paramValue) {
        if (paramKey.equals("prod_name") || paramKey.equals("title")) {
            paramValue = StringEscapeUtils.unescapeHtml(paramValue);
        }

        PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
        paramBuilder.setKey(paramKey);
        paramBuilder.setValue(paramValue);
        pixelBuilder.addParams(paramBuilder.build());
    }

    // generate a cookie value with required format
    private String generateCookieString (String uid, String segment, String timeValue) {
        String cookie2;

        cookie2 = "uid=" + uid + ":v=12.0:ts=" + timeValue + ":hc=1";

        // If this acct uses RTCS segments then....
        if (segment != null) {
            if (SimulatorConfig.getSegmentationType () == SimulatorConstants.SEGMENTATION_TYPE_RTCS) {
                String cdpSegment;
                String cdpSegmentBase64Str;

                // in RTCS world, cdpSegment is included in cookie2 as well as a separate 
                // attribute cdp_segment
                // eg, segment = budget / luxury
                // Map to corresponding cdp_segments values
                cdpSegment = SimulatorConfig.getRTCSKeyValuePair (segment);
                if (cdpSegment != null) {
                    cdpSegmentBase64Str = Base64.getEncoder().encodeToString (cdpSegment.getBytes());
                    cookie2 = cookie2 + ":cdp_segments=" + cdpSegmentBase64Str;
                } // else just use 'basic' cookie2 value
            }
        }
        return (cookie2);
    }
}

/*********
//      if (segment.equals (SimulatorConstants.RTCS_SEGMENTNAME_BUDGET) == true) {
//          cdpSegment = SimulatorConstants.RTCS_SEGMENTATION_ID + ':' + SimulatorConstants.RTCS_BUDGET_SEGMENT_ID;
//      } else if (segment.equals (SimulatorConstants.RTCS_SEGMENTNAME_LUXURY) == true) {
//          cdpSegment = SimulatorConstants.RTCS_SEGMENTATION_ID + ':' + SimulatorConstants.RTCS_LUXURY_SEGMENT_ID;
//      } else {
//          return (SimulatorConstants.SIMULATE_STATUS_ERROR);
//     }
*******/
