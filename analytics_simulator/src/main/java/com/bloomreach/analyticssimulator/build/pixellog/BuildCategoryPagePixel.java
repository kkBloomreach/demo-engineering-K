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

public class BuildCategoryPagePixel extends BuildPixelBase  {

    public BuildCategoryPagePixel () {
    }

    public int build (PixelLog.Builder pixelLogBuilder, UidToSegmentRecord uidToSegmentRecord, long logTime, 
                      String refUrl, String catId, String catName, String catRelPath) throws Exception {
        // let base class update 'common' fields
        int simulateStatus;

        simulateStatus = setCommonFields (pixelLogBuilder, uidToSegmentRecord, logTime);
        if (simulateStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            String catUrl;
            String encodedCatName;

            // cat name
            encodedCatName = URLEncoder.encode (catName);
            pixelLogBuilder.setCat (encodedCatName);
            replacePixelLogParam (pixelLogBuilder, "cat", encodedCatName);

            // catid
            replacePixelLogParam (pixelLogBuilder, "cat_id", catId);

            // ref_url
            pixelLogBuilder.setRefUrl (refUrl);
            replacePixelLogParam (pixelLogBuilder, "ref", refUrl);

            // cat page Url for this cat
            catUrl = getCategoryPageUrl (catId);
            pixelLogBuilder.setUrl (catUrl);
            replacePixelLogParam (pixelLogBuilder, "url", catUrl);

            // title 
            pixelLogBuilder.setTitle (encodedCatName);
            replacePixelLogParam (pixelLogBuilder, "title", encodedCatName);

            simulateStatus = SimulatorConstants.SIMULATE_STATUS_OK; 
        }

        return (simulateStatus);
    }

    // this method is called within this class and also externally so that
    // its value can be further used as 'ref_url' in subsequent pixels
    public static String getCategoryPageUrl (String catId) {
        String catUrl;

        catUrl = SimulatorConfig.getConfigParam ("CATEGORY_URL_PREFIX")  + catId;
        return catUrl;
    }
}

