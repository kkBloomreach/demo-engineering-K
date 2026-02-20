package com.bloomreach.brxdemos.pacifichome.translate.pixel.clone;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.CloneConstants;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.ProductURLPidMapReader;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.CategoryURLCrumbMapReader;


public class CloneCategoryPagePixel extends ClonePixelLogBase  {

    public CloneCategoryPagePixel () {
    }

    public int clonePixel (Builder pixelLogBuilder, ProcessedFeed processedFeed, ProductURLPidMapReader productUrlPidMapReader, CategoryURLCrumbMapReader catUrlCrumbMapReader) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
        if (cloneStatus == CloneConstants.CLONE_STATUS_OK) { 
   
            String srcCrumbId;
            FeedCrumbData crumbInFeed;
            String categoryPageUrl;
            String catInPixel;
            int rGrtrIndx; // last index of ">"

            srcCrumbId = getPixelLogParam (pixelLogBuilder, "cat_id");
            if (srcCrumbId == null) {
                System.out.println ("Category page pixel has no crumb_id");
                return (CloneConstants.CLONE_STATUS_ERROR);
            }

            // use processedFeed to get crumbInFeed
            crumbInFeed = processedFeed.getCrumbDataInFeedForId (srcCrumbId);
            if (crumbInFeed == null) {
                System.out.println ("Feed does not have crumb for crumbId: " + srcCrumbId);
                return (CloneConstants.CLONE_STATUS_REJECT);
            }

            // url in pixelLog
            categoryPageUrl = generateCategoriesUrl (crumbInFeed);
            pixelLogBuilder.setUrl (categoryPageUrl);
            replacePixelLogParam (pixelLogBuilder, "url", categoryPageUrl);

            // cat in pixel == leaf-of-fullCrumb (to keep it consistent with live pixels)
            catInPixel = crumbInFeed.getLeafCrumb ();
            catInPixel = URLEncoder.encode (catInPixel);
            pixelLogBuilder.setCat (catInPixel);
            replacePixelLogParam (pixelLogBuilder, "cat", catInPixel);

            cloneStatus = CloneConstants.CLONE_STATUS_OK;
        }

        return (cloneStatus);
    }

}


