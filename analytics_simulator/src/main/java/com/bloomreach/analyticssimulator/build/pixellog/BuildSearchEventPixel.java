package com.bloomreach.analyticssimulator.build.pixellog;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;

public class BuildSearchEventPixel extends BuildPixelBase  {

    public BuildSearchEventPixel () {
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
            replacePixelLogParam (pixelLogBuilder, "q", query);

            // refUrl
            pixelLogBuilder.setRefUrl (refUrl);
            replacePixelLogParam (pixelLogBuilder, "ref", refUrl);

            // url == refUrl for searchEvent pixel
            url = refUrl;
            pixelLogBuilder.setUrl (url);
            replacePixelLogParam (pixelLogBuilder, "url", url);

            simulateStatus = SimulatorConstants.SIMULATE_STATUS_OK; 
        }

        return (simulateStatus);
    }
}

