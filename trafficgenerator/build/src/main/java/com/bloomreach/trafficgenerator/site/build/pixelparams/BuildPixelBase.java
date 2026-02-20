package com.bloomreach.trafficgenerator.site.build.pixelparams;

// abstract base class for all pixel classes
import java.util.Base64;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.config.*;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeylogs.PixelCountLog;

public abstract class BuildPixelBase {

    // pixelCounts are collected across entire site, for all users together
    // therefore it is static variable
    private static PixelCountLog pixelCountLog;

    protected BuildPixelBase () {
    }

    public static void setPixelCountLog (PixelCountLog pixelCountLog) {
        BuildPixelBase.pixelCountLog = pixelCountLog;
    }

    // common fields to in ALL pixels
    protected int setCommonFields (PixelBRData pixelData, 
                                   UserRecord userRecord, 
                                   long logTime,
                                   boolean testData) throws Exception {
        String visitorId;
        String userId;
        String segment;
        String cookie2;
        String viewId; 

        // may be null OR "NONE" if this acct does not use RTS or RBS segments
        segment = userRecord.getSegment (); 

        // fill up necessary fields in the template
        // acctId
        pixelData.setParam (PixelBRData.PARAMNAME_ACCOUNT_ID, SiteConfig.getAccountConfigParam ("ACCOUNT_ID"));

        // domainKey
        pixelData.setParam (PixelBRData.PARAMNAME_DOMAIN_KEY, SiteConfig.getAccountConfigParam ("DOMAIN"));

        // user id (different from visitorId)
        userId = userRecord.getUserId ();
        if (userId.equals ("NONE") == false)
            pixelData.setParam (PixelBRData.PARAMNAME_USER_ID, userId);

        // cookie2
        visitorId = userRecord.getVisitorId ();
        cookie2 = generateCookieString (visitorId, segment, Long.toString (logTime));
        pixelData.setParam (PixelBRData.PARAMNAME_COOKIE2, cookie2);

        // If this acct uses RTS/RBS segments then....
        if ((segment != null) && (segment.equals ("NONE") == false)) {
           String segmentationType; // RBS or RTS

           segmentationType = SiteConfig.getSegmentationType ();
           if (segmentationType != null) {
                if (segmentationType.equals (SiteConfig.SEGMENTATION_TYPE_RTS)) {
                    String cdpSegment;

                    // in RTS world, cdpSegment is included in ALL pixels (also included in cookie2)
                    // eg, segment = budget / luxury
                    // Map to corresponding cdp_segments values
                    cdpSegment = SiteConfig.getRTSKeyValuePair (segment);
                    if (cdpSegment == null)
                        return (GeneratorConstants.GENERATE_STATUS_ERROR);
                    pixelData.setParam (PixelBRData.PARAMNAME_CDP_SEGMENTS, cdpSegment); // value like 123:123
                } else if (segmentationType.equals (SiteConfig.SEGMENTATION_TYPE_RBS)) {
                    String profileParam;

                    // eg, "customer_profile:Miwaukee"
                    profileParam = String.format ("%s:%s", PixelBRData.SEGMENT_CUSTOMER_PROFILE, userRecord.getSegment ());
                    // sample: "segment=cutomer_profile:Milwaukee"
                    pixelData.setParam (PixelBRData.PARAMNAME_RBS_SEGMENT, profileParam); 
                }
            }
        }

        // view if it is != NONE
        viewId = userRecord.getView ();
        if ((viewId != null) && (viewId.equals ("NONE") == false)) {
            pixelData.setParam (PixelBRData.PARAMNAME_VIEW_ID, viewId); // value like "master"
        }

        // testData = true or false
        if (testData == true)
            pixelData.setParam (PixelBRData.PARAMNAME_TEST_DATA, "true");
        else
            pixelData.setParam (PixelBRData.PARAMNAME_TEST_DATA, "false");

        // is_conversion = 0 by default - set only for conversion
        pixelData.setParam (PixelBRData.PARAMNAME_IS_CONVERSION, "0");

        return (GeneratorConstants.GENERATE_STATUS_OK);
    }

    protected void updatePixelCountLog (String type, String pageType) {
        pixelCountLog.updateCount(type, pageType, null, null);
    }

    protected void updatePixelCountLog (String type, String eventGroup, String eventType) {
        pixelCountLog.updateCount(type, null, eventGroup, eventType);
    }

    // generate a cookie value with required format
    private String generateCookieString (String visitorId, String segment, String timeValue) {
        String cookie2;

        cookie2 = "uid=" + visitorId + ":v=15.0:ts=" + timeValue + ":hc=1";

        // If this acct uses RTS segments then....
        if (segment != null) {
            if (SiteConfig.getSegmentationType () == SiteConfig.SEGMENTATION_TYPE_RTS) {
                String cdpSegment;
                String cdpSegmentBase64Str;

                // in RTS world, cdpSegment is included in cookie2 as well as a separate 
                // attribute cdp_segment
                // eg, segment = budget / luxury
                // Map to corresponding cdp_segments values
                cdpSegment = SiteConfig.getRTSKeyValuePair (segment);
                if (cdpSegment != null) {
                    cdpSegmentBase64Str = Base64.getEncoder().encodeToString (cdpSegment.getBytes());
                    cookie2 = cookie2 + ":cdp_segments=" + cdpSegmentBase64Str;
                } // else just use 'basic' cookie2 value
            }
        }
        return (cookie2);
    }
}

