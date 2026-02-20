package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

public class CloneSuggestEventPixel extends ClonePixelLogBase  {

    public CloneSuggestEventPixel () {
    }

    public int clonePixel (Builder pixelLogBuilder, UidToViewIdMap uidViewIdMap, ProcessedFeed processedFeed) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, uidViewIdMap, processedFeed);
        if (cloneStatus == ClonePixelConstants.CLONE_STATUS_OK) { 

            // url 
            pixelLogBuilder.setUrl (ClonePixelConstants.HOMEPAGE_URL);
            replacePixelLogParam (pixelLogBuilder, "url", ClonePixelConstants.HOMEPAGE_URL);

            cloneStatus = ClonePixelConstants.CLONE_STATUS_OK;
        }

        return (cloneStatus);
    }

}


