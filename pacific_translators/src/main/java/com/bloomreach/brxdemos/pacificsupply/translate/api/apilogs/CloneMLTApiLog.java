package com.bloomreach.brxdemos.pacificsupply.translate.api;

import java.util.Hashtable;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.MobileApi.ApiLog;
import com.bloomreach.proto.MobileApi.ApiLog.Builder;
import com.bloomreach.proto.MobileApi.CommonRequest;
import com.bloomreach.proto.MobileApi.ApiRequest;

public class CloneMLTApiLog extends CloneApiLogBase  {

    public CloneMLTApiLog () {
    }

    public int cloneApiLog (Builder apiLogBuilder, ProcessedFeed processedFeed, Hashtable<String, String> uidToViewIdMap) {
        int cloneStatus;

        // let base class update 'common' fields
        cloneStatus = cloneCommonFields (apiLogBuilder, processedFeed, uidToViewIdMap);
        if (cloneStatus == CloneApiConstants.CLONE_STATUS_OK) { 
            ApiRequest.Builder apiRequestBuilder;
            CommonRequest.Builder commonRequestBuilder;

            apiRequestBuilder = apiLogBuilder.getRequestBuilder ();
            commonRequestBuilder = apiRequestBuilder.getCommonBuilder ();

            // url 
            commonRequestBuilder.setUrl (CloneApiConstants.HOMEPAGE_URL);

            // ref-url 
            commonRequestBuilder.setRefUrl (CloneApiConstants.HOMEPAGE_URL);

            cloneStatus = CloneApiConstants.CLONE_STATUS_OK;
        }

        return (cloneStatus);
    }

}


