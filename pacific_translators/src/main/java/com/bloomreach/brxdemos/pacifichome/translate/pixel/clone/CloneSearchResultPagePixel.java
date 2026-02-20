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

public class CloneSearchResultPagePixel extends ClonePixelLogBase  {

    public CloneSearchResultPagePixel () {
    }

    public int clonePixel (Builder pixelLogBuilder, ProcessedFeed processedFeed, ProductURLPidMapReader productUrlPidMapReader, CategoryURLCrumbMapReader catUrlCrumbMapReader) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
        if (cloneStatus == CloneConstants.CLONE_STATUS_OK) { 
            cloneStatus = CloneConstants.CLONE_STATUS_OK;
        }

        // search page url
        String queryTerm = pixelLogBuilder.getQuery ();
        String searchUrl = generateSearchPageUrl (queryTerm);
        pixelLogBuilder.setUrl (searchUrl);
        replacePixelLogParam (pixelLogBuilder, "url", searchUrl);

        return (cloneStatus);
    }

}


