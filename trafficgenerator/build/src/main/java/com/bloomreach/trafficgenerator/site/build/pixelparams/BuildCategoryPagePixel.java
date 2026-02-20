package com.bloomreach.trafficgenerator.site.build.pixelparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.config.*;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class BuildCategoryPagePixel extends BuildPixelBase  {

    public BuildCategoryPagePixel () {
    }

    public int build (PixelBRData pixelData, 
                      UserRecord userRecord, 
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String catId, 
                      String catName, 
                      String catRelPath,
                      boolean testData) throws Exception {

        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonFields (pixelData, userRecord, logTime, testData);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String title;

            // pixeltype = pageview
            pixelData.setParam (PixelBRData.PARAMNAME_PIXEL_TYPE, PixelBRData.PIXEL_TYPE_PAGEVIEW);

            // title (similar to the one currently set in SPA)
            title = String.format ("%s | %s", catName, SiteConfig.getAccountConfigParam ("DOMAIN"));
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TITLE, title);

            // ptype
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TYPE, PixelBRData.PAGE_TYPE_CATEGORY);

            // cat name -- encoded in Dispatch.java
            pixelData.setParam (PixelBRData.PARAMNAME_CAT_NAME, catName);

            // catid
            pixelData.setParam (PixelBRData.PARAMNAME_CAT_ID, catId);

            // ref_url
            pixelData.setParam (PixelBRData.PARAMNAME_REF_URL, refUrl);

            // cat page Url for this cat
            pixelData.setParam (PixelBRData.PARAMNAME_URL, url);

            // update pixelCount
            updatePixelCountLog(PixelBRData.PIXEL_TYPE_PAGEVIEW, PixelBRData.PAGE_TYPE_CATEGORY);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }

    // this method is called within this class and also externally so that
    // its value can be further used as 'ref_url' in subsequent pixels
    public static String getCategoryPageUrl (String catId) {
        String catUrl;

        catUrl = SiteConfig.getUrlConfigParam ("CATEGORY_URL_PREFIX")  + catId;
        return catUrl;
    }
}

