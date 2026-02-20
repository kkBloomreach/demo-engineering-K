package com.bloomreach.analyticssimulator.build.apilog;

// abstract base class for all simulatePixel classes
import java.util.List;
import java.util.Base64;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;

import org.json.JSONObject;

import com.bloomreach.proto.MobileApi.ExParam;
import com.bloomreach.proto.MobileApi.ApiLog;
import com.bloomreach.proto.MobileApi.ApiLog.Builder;
import com.bloomreach.proto.MobileApi.CommonRequest;
import com.bloomreach.proto.MobileApi.SearchRequest;
import com.bloomreach.proto.MobileApi.ApiRequest;
import com.bloomreach.proto.MobileApi.Pagination;
import com.bloomreach.proto.MobileApi.FieldName;
import com.bloomreach.proto.MobileApi.CommonRequest.RequestType;
import com.bloomreach.proto.MobileApi.SearchRequest.SearchType;
import com.bloomreach.proto.MobileApi.CommonRequest.ResponseType;
import com.bloomreach.proto.MobileApi.ApiResponse;
import com.bloomreach.proto.MobileApi.CommonResponse;
import com.bloomreach.proto.MobileApi.SearchResponse;
import com.bloomreach.proto.MobileApi.ProductResponse;
import com.bloomreach.proto.MobileApi.ResponseMetrics;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.SimulatorConfig;
import com.bloomreach.analyticssimulator.MessageLogger;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;

public abstract class BuildApiBase {

    protected BuildApiBase () {
    }

    // common fields to simulate in ALL api logs 
    protected int setCommonFields (ApiLog.Builder apiLogBuilder, UidToSegmentRecord uidToSegmentRecord, long logTime) throws Exception {
        ApiRequest.Builder apiRequestBuilder;
        CommonRequest.Builder commonRequestBuilder;
        ApiResponse.Builder apiResponseBuilder;
        CommonResponse.Builder commonResponseBuilder;
        SearchResponse.Builder searchResponseBuilder;
        String requestId;
        String cookie2;
        String uid;
        String segment;

        apiRequestBuilder = apiLogBuilder.getRequestBuilder (); 
        commonRequestBuilder = apiRequestBuilder.getCommonBuilder ();

        apiResponseBuilder = apiLogBuilder.getResponseBuilder ();
        commonResponseBuilder = apiResponseBuilder.getCommonBuilder ();

        // fill up necessary fields in the template
        // acctId
        commonRequestBuilder.setAccountId (Integer.parseInt (SimulatorConfig.getConfigParam ("ACCOUNT_ID")));

        // domainKey
        commonRequestBuilder.setSiteDomainKey (SimulatorConfig.getConfigParam ("DOMAIN"));

        // authKey
        commonRequestBuilder.setAuthKey (SimulatorConfig.getConfigParam ("AUTH_KEY"));

        // requestId
        requestId = generateRequestId (); 
        commonRequestBuilder.setRequestId (requestId);

        // logTime (in request and in response)
        commonRequestBuilder.setLogTime (logTime);
        commonResponseBuilder.setLogTime (logTime + 500L);  // 500 millisec for response to go out

        // cookie2
        uid = uidToSegmentRecord.getUid ();
        // eg, segment = budget / luxury
        segment = uidToSegmentRecord.getSegment (); // may be null OR "NONE" if this acct does not use RTCS segments
        cookie2 = generateCookieString (uid, segment, Long.toString (logTime));
        commonRequestBuilder.setCookie2 (cookie2);

        return (SimulatorConstants.SIMULATE_STATUS_OK);
    }

    // protected methods
    // "fields" are added in the requestBuilder
    protected void addRequestField (SearchRequest.Builder searchRequestBuilder, String fieldName) {
        String external_name;
        FieldName.Builder fieldNameBuilder;

        external_name = fieldName;  // external name === fieldName always ? TO BE DONE/Check
        fieldNameBuilder = FieldName.newBuilder ();
        fieldNameBuilder.setName (fieldName);
        fieldNameBuilder.setExternalName (external_name);
        searchRequestBuilder.addFields (fieldNameBuilder.buildPartial ());
    }

    protected void addResponseProduct (SearchResponse.Builder searchResponseBuilder, 
                                     String productId, String productName, double productSalePrice) {
        ProductResponse.Builder productsBuilder;
        String encodedProductName;

        encodedProductName = URLEncoder.encode (productName);

        productsBuilder = ProductResponse.newBuilder ();
        productsBuilder.setProductId (productId);
        productsBuilder.setProductName (encodedProductName);
        productsBuilder.setSalePrice (productSalePrice);
        searchResponseBuilder.addProducts (productsBuilder.buildPartial ());
    }

    // searchType = 'keyword' or 'category' as per BR's specs
    protected String prepareRequestDataJson (CommonRequest.Builder commonRequestBuilder, String searchType, 
                                             String url, String refUrl, String query, String flList, String segment) {
        JSONObject requestData;
        String requestDataStr = null;

        try {
            requestData = new JSONObject ();
            requestData.put ("account_id", SimulatorConfig.getConfigParam ("ACCOUNT_ID"));
            requestData.put ("auth_key", SimulatorConfig.getConfigParam ("AUTH_KEY"));
            requestData.put ("domain_key", SimulatorConfig.getConfigParam ("DOMAIN"));
            requestData.put ("request_id", commonRequestBuilder.getRequestId ()); 
            requestData.put ("_br_uid_2", commonRequestBuilder.getCookie2 ()); 
            requestData.put ("url", "12321");
            requestData.put ("search_type", searchType);    // keyword or category
            requestData.put ("request_type", "search");
            requestData.put ("q", query);
            requestData.put ("ref_url", refUrl);
            requestData.put ("start", "0");
            requestData.put ("rows", "10");
            requestData.put ("fl", flList);
            if ((segment != null) && (segment.equals ("NONE") == false)) {
                String profileStr;

                profileStr = "customer_profile:" + segment;
                requestData.put ("segment", profileStr);
            }
            requestDataStr = requestData.toString ();
        } catch (Exception e) {
            MessageLogger.logError ("Prepare requestDataJson exception: " + e.getMessage ());            
        }

        return (requestDataStr);    // may be null
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
    private String generateCookieString (String uid, String segment, String timeValue) {
        String cookie2;
        String base64str;

        cookie2 = "uid=" + uid + ":v=12.0:ts=" + timeValue + ":hc=1";

        // If this acct uses RTCS segments then....
        if (segment != null) {
            if (SimulatorConfig.getSegmentationType () == SimulatorConstants.SEGMENTATION_TYPE_RTCS) {
                String cdpSegment;
                String cdpSegmentBase64Str;

                // in RTCS world, cdpSegment is included in ALL api logs
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

/********
//     // replace an existing parameter's value
//     // first obtain entire list of params in the pixel. Then look for specified key's index
//     // and use that to replace value. Looks like the "setParams" method does not have a
//     // overloaded method where it takes a paramKey as an argument
//     protected boolean replacePixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey, String paramValue) {
//         List<PixelLog.PixelLogParam> paramsList;
// 
//         paramsList = pixelLogBuilder.getParamsList ();
//         for (int i = 0; i < paramsList.size (); i++) {
//             PixelLogParam aParam;
// 
//             aParam = paramsList.get (i);
//             if (aParam.getKey ().equals (paramKey)) {
//                 PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
//                 paramBuilder.setKey(paramKey);
//                 paramBuilder.setValue(paramValue);
//                 PixelLogParam newPixelLogParam = paramBuilder.build();
// 
//                 pixelLogBuilder.setParams(i, newPixelLogParam);
// 
//                 return (true);
//             }
//         }
// 
//         // specified paramKey not found in current paramsList
//         return (false);
//     }
// 
//     / **
//      * Add PixelLogParam corresponding to the passed parameter key-value pair to the pixel builder.
//      * @param pixelBuilder
//      * @param paramKey
//      * @param paramValue
//      * /
//     protected void addPixelLogParam (PixelLog.Builder pixelBuilder, String paramKey, String paramValue) {
//         if (paramKey.equals("prod_name") || paramKey.equals("title")) {
//             paramValue = StringEscapeUtils.unescapeHtml(paramValue);
//         }
// 
//         PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
//         paramBuilder.setKey(paramKey);
//         paramBuilder.setValue(paramValue);
//         pixelBuilder.addParams(paramBuilder.build());
//     }
// }
***/
