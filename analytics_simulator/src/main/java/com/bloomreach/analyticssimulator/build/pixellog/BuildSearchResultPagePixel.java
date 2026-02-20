package com.bloomreach.analyticssimulator.build.pixellog;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.SimulatorConfig;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;

public class BuildSearchResultPagePixel extends BuildPixelBase  {

    public BuildSearchResultPagePixel () {
    }

    public int build (PixelLog.Builder pixelLogBuilder, UidToSegmentRecord uidToSegmentRecord, long logTime, 
                      String refUrl, String query) throws Exception {
        // let base class update 'common' fields
        int simulateStatus;

        simulateStatus = setCommonFields (pixelLogBuilder, uidToSegmentRecord, logTime);
        if (simulateStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            String url;

            // query 
            pixelLogBuilder.setQuery (query);
            replacePixelLogParam (pixelLogBuilder, "search_term", query);
            replacePixelLogParam (pixelLogBuilder, "df_search_term", query);
            replacePixelLogParam (pixelLogBuilder, "df_q", query);

            // ref_url
            pixelLogBuilder.setRefUrl (refUrl);
            replacePixelLogParam (pixelLogBuilder, "ref", refUrl);

            // url
            url = getSearchResultPageUrl (query);
            pixelLogBuilder.setUrl (url);
            replacePixelLogParam (pixelLogBuilder, "url", url);

            simulateStatus = SimulatorConstants.SIMULATE_STATUS_OK; 
        }

        return (simulateStatus);
    }

    // this method is called within this class and also externally so that
    // its value can be further used as 'ref_url' in subsequent pixels
    public static String getSearchResultPageUrl (String query) {
        String url;
        String searchResultPageUrl;
        String encodedQuery;

        // example: .../search?_sq=Sectional+Sofa
        encodedQuery = URLEncoder.encode (query);
        searchResultPageUrl = SimulatorConfig.getConfigParam ("SEARCH_PAGE_URL_PREFIX");
        searchResultPageUrl = searchResultPageUrl + encodedQuery;
        return searchResultPageUrl;
    }

}

