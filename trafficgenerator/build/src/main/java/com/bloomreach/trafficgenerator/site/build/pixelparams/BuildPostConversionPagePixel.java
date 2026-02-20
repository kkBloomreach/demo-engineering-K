package com.bloomreach.trafficgenerator.site.build.pixelparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.config.*;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class BuildPostConversionPagePixel extends BuildPixelBase  {

    public BuildPostConversionPagePixel () {
    }

    public int build (PixelBRData pixelData, 
                      UserRecord userRecord, 
                      long logTime, 
                      String refUrl,
                      String url,
                      boolean testData) throws Exception {

        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonFields (pixelData, userRecord, logTime, testData);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String domain;
            String title;

            // pixeltype = pageview
            pixelData.setParam (PixelBRData.PARAMNAME_PIXEL_TYPE, PixelBRData.PIXEL_TYPE_PAGEVIEW);

            // title (similar to the one currently set in SPA)
            domain = SiteConfig.getAccountConfigParam ("DOMAIN");
            title = String.format ("Thank you | %s", domain);
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TITLE, title);

            // ptype
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TYPE, PixelBRData.PAGE_TYPE_OTHER);

            // ref_url ("checkout")
            pixelData.setParam (PixelBRData.PARAMNAME_REF_URL, refUrl);

            // url ("thank you")
            pixelData.setParam (PixelBRData.PARAMNAME_URL, url);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }

    // this method is called within this class and also externally so that
    // its value can be further used as 'ref_url' in subsequent pixels
    public static String getPostConversionPageUrl () {
        String url;

        url = SiteConfig.getUrlConfigParam ("POST_CONVERSION_PAGE_URL");
        return url;
    }

}

/***
//    // this method is called within this class and also externally so that
//    // its value can be further used as 'ref_url' in subsequent pixels
//    public static String getHomePageUrl (String catId) {
//        String url = SiteConfig.getUrlConfigParam ("HOMEPAGE_URL");
//        return url;
//    }
***/

