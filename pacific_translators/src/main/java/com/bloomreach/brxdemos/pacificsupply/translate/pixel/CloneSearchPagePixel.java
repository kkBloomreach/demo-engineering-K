package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

public class CloneSearchPagePixel extends ClonePixelLogBase  {

    public CloneSearchPagePixel () {
    }

    public int clonePixel (Builder pixelLogBuilder, UidToViewIdMap uidViewIdMap, ProcessedFeed processedFeed) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, uidViewIdMap, processedFeed);
        if (cloneStatus == ClonePixelConstants.CLONE_STATUS_OK) { 
            cloneStatus = ClonePixelConstants.CLONE_STATUS_OK;
        }

        // search page url
        String queryTerm = pixelLogBuilder.getQuery ();
        String searchUrl = generateSearchPageUrl (queryTerm);
        pixelLogBuilder.setUrl (searchUrl);
        replacePixelLogParam (pixelLogBuilder, "url", searchUrl);

        return (cloneStatus);
    }

}


