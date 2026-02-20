package com.bloomreach.trafficgenerator.site.build.apiparams;

// abstract base class for all API classes
import java.util.Base64;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;

public abstract class BuildApiBase {

    protected BuildApiBase () {
    }

    // common fields in ALL search apis 
    protected int setCommonSearchFields (ApiBRData apiData, 
                                         UserRecord userRecord,
                                         CampaignRecord activeCampaignRecord, 
                                         long logTime) throws Exception {
        String requestId;
        String cookie2;
        String visitorId;
        String userId;
        String segment;
        String viewId;

        // fill up necessary fields in apiData object
        // acctId
        apiData.setParam (ApiBRData.PARAMNAME_ACCOUNT_ID, SiteConfig.getAccountConfigParam ("ACCOUNT_ID"));

        // domainKey
        apiData.setParam (ApiBRData.PARAMNAME_DOMAIN_KEY, SiteConfig.getAccountConfigParam ("DOMAIN"));

        // authKey
        apiData.setParam (ApiBRData.PARAMNAME_AUTH_KEY, SiteConfig.getAccountConfigParam ("AUTH_KEY"));

        // requestId
        requestId = generateRequestId (); 
        apiData.setParam (ApiBRData.PARAMNAME_REQUEST_ID, requestId);

        // user id (different from visitorId)
        userId = userRecord.getUserId ();
        if (userId.equals ("NONE") == false)
            apiData.setParam (ApiBRData.PARAMNAME_USER_ID, userId);

        // eg, segment = budget / luxury
        // may be null OR "NONE" if this acct does not use RTS segments
        visitorId = userRecord.getVisitorId ();
        segment = userRecord.getSegment (); 
        cookie2 = generateCookieString (visitorId, segment, Long.toString (logTime));
        apiData.setParam (ApiBRData.PARAMNAME_BR_UID2, cookie2);

        // view_id
        viewId = userRecord.getView ();
        if ((viewId != null) && (viewId.equals ("NONE") == false))
            apiData.setParam (ApiBRData.PARAMNAME_VIEW_ID, viewId);

        // 'boost' if we have valid campaign in progress
        if (activeCampaignRecord != null) {
            String boostParamValue;

            // eg: boost=campaign:campaignName
            boostParamValue = String.format ("%s:%s", ApiBRData.ATTRIB_NAME_CAMPAIGN, activeCampaignRecord.getCampaignName ());
            apiData.setParam (ApiBRData.PARAMNAME_BOOST, boostParamValue);
        }

        return (GeneratorConstants.GENERATE_STATUS_OK);
    }

    protected int setCommonSuggestFields (ApiBRData apiData, 
                                          UserRecord userRecord,
                                          long logTime) throws Exception {
        String requestId;
        String cookie2;
        String visitorId;
        String userId;
        String segment;
        String viewId;
        String catalogViews;

        // fill up necessary fields in apiData object
        // acctId
        apiData.setParam (ApiBRData.PARAMNAME_ACCOUNT_ID, SiteConfig.getAccountConfigParam ("ACCOUNT_ID"));

        // domainKey
        apiData.setParam (ApiBRData.PARAMNAME_DOMAIN_KEY, SiteConfig.getAccountConfigParam ("DOMAIN"));

        // authKey - not included in Suggest API call
        apiData.setParam (ApiBRData.PARAMNAME_AUTH_KEY, "");

        // requestId
        requestId = generateRequestId (); 
        apiData.setParam (ApiBRData.PARAMNAME_REQUEST_ID, requestId);

        // user id (different from visitorId)
        userId = userRecord.getUserId ();
        if (userId.equals ("NONE") == false)
            apiData.setParam (ApiBRData.PARAMNAME_USER_ID, userId);

        // eg, segment = budget / luxury
        // may be null OR "NONE" if this acct does not use RTS segments
        visitorId = userRecord.getVisitorId ();
        segment = userRecord.getSegment (); 
        cookie2 = generateCookieString (visitorId, segment, Long.toString (logTime));
        apiData.setParam (ApiBRData.PARAMNAME_BR_UID2, cookie2);

        // catalog_views
        viewId = userRecord.getView ();
        if ((viewId != null) && (viewId.equals ("NONE") == false))
            catalogViews = String.format ("%s:%s", SiteConfig.getAccountConfigParam ("CATALOG_NAME"), viewId); // catalog_name:view 
        else
            catalogViews = String.format ("%s", SiteConfig.getAccountConfigParam ("CATALOG_NAME")); // catalog_name 
        apiData.setParam (ApiBRData.PARAMNAME_CATALOG_VIEWS, catalogViews);

        return (GeneratorConstants.GENERATE_STATUS_OK);
    }

    // common fields in ALL widget apis. This method is almost exactly the same as
    // setCommonSearchFields above 
    protected int setCommonWidgetFields (ApiBRData apiData, 
                                         UserRecord userRecord,
                                         CampaignRecord activeCampaignRecord, 
                                         long logTime) throws Exception {
        String requestId;
        String cookie2;
        String visitorId;
        String userId;
        String segment;
        String viewId;

        // fill up necessary fields in apiData object
        // acctId
        apiData.setParam (ApiBRData.PARAMNAME_ACCOUNT_ID, SiteConfig.getAccountConfigParam ("ACCOUNT_ID"));

        // domainKey
        apiData.setParam (ApiBRData.PARAMNAME_DOMAIN_KEY, SiteConfig.getAccountConfigParam ("DOMAIN"));

        // authKey
        apiData.setParam (ApiBRData.PARAMNAME_AUTH_KEY, SiteConfig.getAccountConfigParam ("AUTH_KEY"));

        // requestId
        requestId = generateRequestId (); 
        apiData.setParam (ApiBRData.PARAMNAME_REQUEST_ID, requestId);

        // user id (different from visitorId)
        userId = userRecord.getUserId ();
        if (userId.equals ("NONE") == false)
            apiData.setParam (ApiBRData.PARAMNAME_USER_ID, userId);

        // eg, segment = budget / luxury
        // may be null OR "NONE" if this acct does not use RTS segments
        visitorId = userRecord.getVisitorId ();
        segment = userRecord.getSegment (); 
        cookie2 = generateCookieString (visitorId, segment, Long.toString (logTime));
        apiData.setParam (ApiBRData.PARAMNAME_BR_UID2, cookie2);

        // view_id
        viewId = userRecord.getView ();
        if ((viewId != null) && (viewId.equals ("NONE") == false))
            apiData.setParam (ApiBRData.PARAMNAME_VIEW_ID, viewId);

        // 'boost' if we have valid campaign in progress
        if (activeCampaignRecord != null) {
            String boostParamValue;

            // eg: boost=campaign:campaignName
            boostParamValue = String.format ("%s:%s", ApiBRData.ATTRIB_NAME_CAMPAIGN, activeCampaignRecord.getCampaignName ());
            apiData.setParam (ApiBRData.PARAMNAME_BOOST, boostParamValue);
        }

        return (GeneratorConstants.GENERATE_STATUS_OK);
    }

    // searchType = 'keyword' or 'category' as per BR's specs
    // this method is used for both search and category api calls
    // "Common" values (eg, acctId, ..) are already set in the apiData param
    protected void updateSearchApiParams (ApiBRData apiData, 
                                    String searchType, 
                                    String url, 
                                    String refUrl, 
                                    String query, 
                                    String flList, 
                                    String segment) {
        try {
            apiData.setParam (ApiBRData.PARAMNAME_REQUEST_TYPE, "search");
            apiData.setParam (ApiBRData.PARAMNAME_SEARCH_TYPE, searchType); // keyword or category
            apiData.setParam (ApiBRData.PARAMNAME_Q, query);
            apiData.setParam (ApiBRData.PARAMNAME_URL, url);
            apiData.setParam (ApiBRData.PARAMNAME_REF_URL, refUrl);
            apiData.setParam (ApiBRData.PARAMNAME_START, "0");
            apiData.setParam (ApiBRData.PARAMNAME_ROWS, ApiBRData.MAX_ROWS);
            apiData.setParam (ApiBRData.PARAMNAME_FL, flList);

            // If this acct uses RTS/RBS segments then....
            if ((segment != null) && (segment.equals ("NONE") == false)) {
                String segmentationType; // RBS or RTS

                segmentationType = SiteConfig.getSegmentationType ();
                if (segmentationType != null) {
                    if (segmentationType.equals (SiteConfig.SEGMENTATION_TYPE_RTS)) {
                        String cdpSegment;

                        // in RTS world, cdpSegment is included in ALL apis
                        // eg, segment = budget / luxury
                        // Map to corresponding cdp_segments values
                        cdpSegment = SiteConfig.getRTSKeyValuePair (segment);
                        if (cdpSegment == null)
                            MessageLogger.logError (String.format ("APIBase: cannot find cdp segmentation, segment = %s", segment));
                        else
                            apiData.setParam (ApiBRData.PARAMNAME_CDP_SEGMENTS, cdpSegment); // value like 123:123
                    } else if (segmentationType.equals (SiteConfig.SEGMENTATION_TYPE_RBS)) {
                        String profileParam;

                        // eg, "customer_profile:Miwaukee"
                        profileParam = String.format ("%s:%s", ApiBRData.SEGMENT_CUSTOMER_PROFILE, segment);
                        // sample: "segment=cutomer_profile:Milwaukee"
                        apiData.setParam (ApiBRData.PARAMNAME_RBS_SEGMENT, profileParam); 
                    }
                } 
            }
        } catch (Exception e) {
            MessageLogger.logError ("updateSearchApiParams exception: " + e.getMessage ());            
        }
    }

    protected void updateSuggestApiParams (ApiBRData apiData, 
                                           String url, 
                                           String refUrl, 
                                           String aqTerm, 
                                           String flList ) {
        apiData.setParam (ApiBRData.PARAMNAME_REQUEST_TYPE, "suggest");
        apiData.setParam (ApiBRData.PARAMNAME_Q, aqTerm);
        apiData.setParam (ApiBRData.PARAMNAME_URL, url);
        apiData.setParam (ApiBRData.PARAMNAME_REF_URL, refUrl);
        apiData.setParam (ApiBRData.PARAMNAME_FL, flList);
    }

    protected void updateWidgetApiParams (ApiBRData apiData, 
                                           String url, 
                                           String refUrl, 
                                           String wid,   // widget id
                                           String wq,    // query (optional for non-query widgets)
                                           String itemId,// optional
                                           String flList,
                                           String segment ) {
        apiData.setParam (ApiBRData.PARAMNAME_URL, url);
        apiData.setParam (ApiBRData.PARAMNAME_REF_URL, refUrl);
        apiData.setParam (ApiBRData.PARAMNAME_WIDGET_WID, wid);
        apiData.setParam (ApiBRData.PARAMNAME_FIELDS, flList);
        if (wq != null)
            apiData.setParam (ApiBRData.PARAMNAME_WIDGET_WQ, wq);
        if (itemId != null)
            apiData.setParam (ApiBRData.PARAMNAME_WIDGET_ITEMID, itemId);
    }

    //// INTERNAL METHODS
    // random string of a 13-digit integer number
    private String generateRequestId () {
        long reqIdLong;
        double multFactor;

        multFactor = Math.pow (10, 13);
        reqIdLong = (long) ((Math.random () + 1) * multFactor);
        return (Long.toString (reqIdLong));
    }

    // generate a cookie value with required format
    private String generateCookieString (String visitorId, String segment, String timeValue) {
        String cookie2;

        cookie2 = "uid=" + visitorId + ":v=12.0:ts=" + timeValue + ":hc=1";

        // If this acct uses RTS segments then....
        if (segment != null) {
            if (SiteConfig.getSegmentationType () == SiteConfig.SEGMENTATION_TYPE_RTS) {
                String cdpSegment;
                String cdpSegmentBase64Str;

                // in RTS world, cdpSegment is included in ALL api logs
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

