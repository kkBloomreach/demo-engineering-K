package com.bloomreach.analyticssimulator.build.apilog;

import java.io.IOException;
import java.util.ArrayList;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.SimulatorConfig;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;
import com.bloomreach.analyticssimulator.build.pixellog.*;

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

public class BuildCategoryApi extends BuildApiBase  {

    public BuildCategoryApi () {
    }

    public int build (ApiLog.Builder apiLogBuilder, UidToSegmentRecord uidToSegmentRecord, long logTime, 
                      String refUrl, SegmentCategoryToPidRecord selectedCategoryRecord,
                      ProcessedFeed processedFeed) throws Exception {
        // let base class update 'common' fields
        int simulateStatus;

        simulateStatus = setCommonFields (apiLogBuilder, uidToSegmentRecord, logTime);
        if (simulateStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            ApiRequest.Builder apiRequestBuilder;
            CommonRequest.Builder commonRequestBuilder;
            SearchRequest.Builder searchRequestBuilder;
            ApiResponse.Builder apiResponseBuilder;
            CommonResponse.Builder commonResponseBuilder;
            SearchResponse.Builder searchResponseBuilder;
            String url;
            ArrayList <String> responsePidList;
            String requestDataJson;
            String flList;

            apiRequestBuilder = apiLogBuilder.getRequestBuilder (); 
            commonRequestBuilder = apiRequestBuilder.getCommonBuilder ();
            searchRequestBuilder = apiRequestBuilder.getSearchBuilder ();

            apiResponseBuilder = apiLogBuilder.getResponseBuilder ();
            commonResponseBuilder = apiResponseBuilder.getCommonBuilder ();
            searchResponseBuilder = apiResponseBuilder.getSearchBuilder ();

            // url
            url = BuildCategoryPagePixel.getCategoryPageUrl (selectedCategoryRecord.getCatId ());
            commonRequestBuilder.setUrl (url);

            // refUrl
            commonRequestBuilder.setRefUrl (refUrl);

            // query
            searchRequestBuilder.setQuery (selectedCategoryRecord.getCatId ());

            // add request fields (currently the fields added are the ones in the sample API request)
            // addRequestField (searchRequestBuilder, "thumb_image_url");   // this is already in template
            addRequestField (searchRequestBuilder, "sale_price");
            addRequestField (searchRequestBuilder, "product_name");
            addRequestField (searchRequestBuilder, "product_id");

            // segment in API call (eg, segment=customer_profile:Milwaukee)
            // According to JIRA PACIFICHOME-53, customer_profile is included in request_data_json field
            // flList is just a place holder value in this simulator
            flList = "pid,title,description,price,sale_price,thumb_image";
            requestDataJson = prepareRequestDataJson (commonRequestBuilder, "category",
                                                      url, refUrl, selectedCategoryRecord.getCatId (), flList, uidToSegmentRecord.getSegment ());
            // continue even if requestDataJson fails
            if (requestDataJson != null)
                commonRequestBuilder.setRequestDataJson (requestDataJson);

            // add response products
            // list of products in search API response
            responsePidList = selectedCategoryRecord.getPidList ();
            for (String pid : responsePidList) {
                String prodName;
                double prodPrice;
                String encodedProductName;

                prodName = processedFeed.lookupProductName (pid);
                encodedProductName = URLEncoder.encode (prodName);
                prodPrice = Double.valueOf (processedFeed.lookupProductPrice (pid));
                commonResponseBuilder.setNumResults (selectedCategoryRecord.getNumFound());
                searchResponseBuilder.setNumResults (selectedCategoryRecord.getNumFound ());
                addResponseProduct (searchResponseBuilder, pid, encodedProductName, prodPrice);
            }

            simulateStatus = SimulatorConstants.SIMULATE_STATUS_OK; 
        }

        return (simulateStatus);
    }

    public int buildZeroResult (ApiLog.Builder apiLogBuilder, UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                String refUrl, String query, ProcessedFeed processedFeed) throws Exception {
        // let base class update 'common' fields
        int simulateStatus;

        simulateStatus = setCommonFields (apiLogBuilder, uidToSegmentRecord, logTime);
        if (simulateStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            ApiRequest.Builder apiRequestBuilder;
            CommonRequest.Builder commonRequestBuilder;
            SearchRequest.Builder searchRequestBuilder;
            ApiResponse.Builder apiResponseBuilder;
            CommonResponse.Builder commonResponseBuilder;
            SearchResponse.Builder searchResponseBuilder;
            String url;
            ArrayList <String> responsePidList;

            apiRequestBuilder = apiLogBuilder.getRequestBuilder (); 
            commonRequestBuilder = apiRequestBuilder.getCommonBuilder ();
            searchRequestBuilder = apiRequestBuilder.getSearchBuilder ();

            apiResponseBuilder = apiLogBuilder.getResponseBuilder ();
            commonResponseBuilder = apiResponseBuilder.getCommonBuilder ();
            searchResponseBuilder = apiResponseBuilder.getSearchBuilder ();

            // url
            url = BuildSearchResultPagePixel.getSearchResultPageUrl (query);
            commonRequestBuilder.setUrl (url);

            // refUrl
            commonRequestBuilder.setRefUrl (refUrl);

            // query
            searchRequestBuilder.setQuery (query);

            // add request fields (currently the fields added are the ones in the sample API request)
            // addRequestField (searchRequestBuilder, "thumb_image_url");   // this is already in template
            addRequestField (searchRequestBuilder, "sale_price");
            addRequestField (searchRequestBuilder, "product_name");
            addRequestField (searchRequestBuilder, "product_id");

            // No response products for zero-result-query
            commonResponseBuilder.setNumResults (0);
            searchResponseBuilder.setNumResults (0);

            simulateStatus = SimulatorConstants.SIMULATE_STATUS_OK; 
        }

        return (simulateStatus);
    }
}

