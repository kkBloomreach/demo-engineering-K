package com.bloomreach.trafficgenerator.site.build.pixelparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.config.*;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class BuildSearchResultPagePixel extends BuildPixelBase  {

    public BuildSearchResultPagePixel () {
    }

    public int build (PixelBRData pixelData, 
                      UserRecord userRecord, 
                      long logTime, 
                      String refUrl, 
                      String url,
                      String query, 
                      boolean testData) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonFields (pixelData, userRecord, logTime, testData);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String title;

            // pixeltype = pageview
            pixelData.setParam (PixelBRData.PARAMNAME_PIXEL_TYPE, PixelBRData.PIXEL_TYPE_PAGEVIEW);

            // title (similar to the one currently set in SPA)
            title = String.format ("Search | %s", SiteConfig.getAccountConfigParam ("DOMAIN"));
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TITLE, title);

            // ptype
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TYPE, PixelBRData.PAGE_TYPE_SEARCH);

            // query 
            pixelData.setParam (PixelBRData.PARAMNAME_SEARCH_TERM, query);

            // ref_url
            pixelData.setParam (PixelBRData.PARAMNAME_REF_URL, refUrl);

            // url
            pixelData.setParam (PixelBRData.PARAMNAME_URL, url);

            // update pixelCount
            updatePixelCountLog(PixelBRData.PIXEL_TYPE_PAGEVIEW, PixelBRData.PAGE_TYPE_SEARCH);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }

    // this method is called within this class and also externally so that
    // its value can be further used as 'ref_url' in subsequent pixels
    public static String getSearchResultPageUrl (String query) {
        String searchResultPageUrl;

        // example: .../search?_sq=Sectional+Sofa
        searchResultPageUrl = SiteConfig.getUrlConfigParam ("SEARCH_PAGE_URL_PREFIX");
        searchResultPageUrl = searchResultPageUrl + query; // url-encoded when generating queryParam (see DiscoveryUserAccess.java)
        return searchResultPageUrl;
    }

}

