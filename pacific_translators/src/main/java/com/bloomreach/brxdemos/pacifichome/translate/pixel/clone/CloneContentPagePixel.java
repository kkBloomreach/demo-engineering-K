package com.bloomreach.brxdemos.pacifichome.translate.pixel.clone;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.CloneConstants;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.ProductURLPidMapReader;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.CategoryURLCrumbMapReader;


public class CloneContentPagePixel extends ClonePixelLogBase  {

    public CloneContentPagePixel () {
    }

    public int clonePixel (Builder pixelLogBuilder, ProcessedFeed processedFeed, ProductURLPidMapReader productUrlPidMapReader, CategoryURLCrumbMapReader catUrlCrumbMapReader) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
        if (cloneStatus == CloneConstants.CLONE_STATUS_OK) { 
      
            // key-value
            // url 
            pixelLogBuilder.setUrl (CloneConstants.HOMEPAGE_URL); 

            // param url 
            replacePixelLogParam (pixelLogBuilder, "url", CloneConstants.HOMEPAGE_URL);

            cloneStatus = CloneConstants.CLONE_STATUS_OK;
        }

        return (cloneStatus);
    }

}


