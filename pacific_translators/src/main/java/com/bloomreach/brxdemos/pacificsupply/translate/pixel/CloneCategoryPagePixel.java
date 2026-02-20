package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

public class CloneCategoryPagePixel extends ClonePixelLogBase  {

    public CloneCategoryPagePixel () {
    }

    public int clonePixel (Builder pixelLogBuilder, UidToViewIdMap uidViewIdMap, ProcessedFeed processedFeed) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, uidViewIdMap, processedFeed);
        if (cloneStatus == ClonePixelConstants.CLONE_STATUS_OK) { 
   
            String srcCrumbId;
            String srcCrumb;

            srcCrumbId = getPixelLogParam (pixelLogBuilder, "cat_id");
            if (srcCrumbId == null) {
                System.out.println ("Category page pixel has no crumb_id");
                return (ClonePixelConstants.CLONE_STATUS_ERROR);
            }

            // original src crumb
            srcCrumb = processedFeed.lookupCrumb (srcCrumbId);
            if (StringUtils.isEmpty (srcCrumb) == true) {
                System.out.println ("Category page pixel has no crumb");
                return (ClonePixelConstants.CLONE_STATUS_REJECT);
            }

            // convert crumb to seo-friendly string
            String seoFriendlyName = generateSEOFriendlyPath (srcCrumb);
            String seoFriendlyUrl = ClonePixelConstants.CATEGORY_URL_PREFIX + seoFriendlyName;

            // url in pixelLog
            pixelLogBuilder.setUrl (seoFriendlyUrl);
            replacePixelLogParam (pixelLogBuilder, "url", seoFriendlyUrl);

            // cat
            pixelLogBuilder.setCat (seoFriendlyName);
            replacePixelLogParam (pixelLogBuilder, "cat", seoFriendlyName);
 
            cloneStatus = ClonePixelConstants.CLONE_STATUS_OK;
        }

        return (cloneStatus);
    }

    private String generateSEOFriendlyPath (String crumb) {

        String crumbLowerCase = crumb.toLowerCase ();
        String tmpTxt1 = crumbLowerCase.replaceAll ("[^a-zA-Z0-9\\s]", "");
        String tmpTxt3 = tmpTxt1.replaceAll ("[\\s]", "_");

        return (tmpTxt3);
    }
}


