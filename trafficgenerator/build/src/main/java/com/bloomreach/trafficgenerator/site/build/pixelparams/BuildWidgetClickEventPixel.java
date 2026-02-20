package com.bloomreach.trafficgenerator.site.build.pixelparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class BuildWidgetClickEventPixel extends BuildPixelBase  {

    public BuildWidgetClickEventPixel () {
    }

    public int build (PixelBRData pixelData, 
                      UserRecord userRecord, 
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String wid,   // widget id
                      String wrid,  // response id (from api response)
                      String wty,   // widget type (from api response)
                      String itemId,   // itemId 
                      boolean testData) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonFields (pixelData, userRecord, logTime, testData);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String pageType;

            // pixeltype = event 
            pixelData.setParam (PixelBRData.PARAMNAME_PIXEL_TYPE, PixelBRData.PIXEL_TYPE_EVENT);

            // eventgroup - note, group is called "suggest" even though event is 'search'
            pixelData.setParam (PixelBRData.PARAMNAME_EVENT_GROUP, PixelBRData.EVENT_GROUP_WIDGET);

            // etype (aka action type)
            pixelData.setParam (PixelBRData.PARAMNAME_EVENT_ETYPE, PixelBRData.EVENT_ETYPE_WIDGET_CLICK);

            // widget parameters
            pixelData.setParam (PixelBRData.PARAMNAME_WIDGET_WRID, wrid);
            pixelData.setParam (PixelBRData.PARAMNAME_WIDGET_WID, wid);
            pixelData.setParam (PixelBRData.PARAMNAME_WIDGET_WTY, wty);
            if (itemId != null) // optional
                pixelData.setParam (PixelBRData.PARAMNAME_WIDGET_ITEMID, itemId);

            // refUrl
            pixelData.setParam (PixelBRData.PARAMNAME_REF_URL, refUrl);

            // url 
            pixelData.setParam (PixelBRData.PARAMNAME_URL, url);

            // set 'ptype' based on refUrl value (homepage/productpage/categorypage)
            // ie, the pageType on which this event occured
            // Following if-then assumes the refUrl is generated using the PREFIX defined in config file
            if (refUrl.indexOf ("products") > 0)
                pageType = PixelBRData.PAGE_TYPE_PRODUCT;
            else if (refUrl.indexOf ("categories") > 0)
                pageType = PixelBRData.PAGE_TYPE_CATEGORY;
            else
                pageType = PixelBRData.PAGE_TYPE_HOME;
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TYPE, pageType);

            // update pixelCount
            updatePixelCountLog(PixelBRData.PIXEL_TYPE_EVENT, PixelBRData.EVENT_GROUP_WIDGET, PixelBRData.EVENT_ETYPE_WIDGET_CLICK);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }
}

