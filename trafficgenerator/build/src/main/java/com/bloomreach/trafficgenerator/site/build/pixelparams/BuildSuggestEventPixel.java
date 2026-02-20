package com.bloomreach.trafficgenerator.site.build.pixelparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class BuildSuggestEventPixel extends BuildPixelBase  {

    public BuildSuggestEventPixel () {
    }

    public int build (PixelBRData pixelData, 
                      UserRecord userRecord, 
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String aq,
                      String query, // 'q' in suggest event
                      boolean testData) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonFields (pixelData, userRecord, logTime, testData);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String pageType;

            // pixeltype = event 
            pixelData.setParam (PixelBRData.PARAMNAME_PIXEL_TYPE, PixelBRData.PIXEL_TYPE_EVENT);

            // eventgroup - note, group is called "suggest" even though event is 'search'
            pixelData.setParam (PixelBRData.PARAMNAME_EVENT_GROUP, PixelBRData.EVENT_GROUP_SUGGEST);

            // etype (aka action type)
            pixelData.setParam (PixelBRData.PARAMNAME_EVENT_ETYPE, PixelBRData.EVENT_ETYPE_CLICK);

            // query 
            pixelData.setParam (PixelBRData.PARAMNAME_SEARCH_Q, query);
            pixelData.setParam (PixelBRData.PARAMNAME_SEARCH_AQ, aq);

            // refUrl
            pixelData.setParam (PixelBRData.PARAMNAME_REF_URL, refUrl);

            // url 
            pixelData.setParam (PixelBRData.PARAMNAME_URL, url);

            // set 'ptype' based on refUrl value (homepage/productpage/categorypage)
            // ie, the pageType on which this searchEvent occured
            // Following if-then assumes the refUrl is generated using the PREFIX defined in config file
            if (refUrl.indexOf ("products") > 0)
                pageType = PixelBRData.PAGE_TYPE_PRODUCT;
            else if (refUrl.indexOf ("categories") > 0)
                pageType = PixelBRData.PAGE_TYPE_CATEGORY;
            else
                pageType = PixelBRData.PAGE_TYPE_HOME;
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TYPE, pageType);

            // update pixelCount
            updatePixelCountLog(PixelBRData.PIXEL_TYPE_EVENT, PixelBRData.EVENT_GROUP_SUGGEST, PixelBRData.EVENT_ETYPE_CLICK);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }
}

