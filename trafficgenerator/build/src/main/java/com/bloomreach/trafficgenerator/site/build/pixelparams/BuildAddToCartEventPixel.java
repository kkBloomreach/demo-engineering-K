package com.bloomreach.trafficgenerator.site.build.pixelparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class BuildAddToCartEventPixel extends BuildPixelBase  {

    public BuildAddToCartEventPixel () {
    }

    public int build (PixelBRData pixelData, 
                      UserRecord userRecord, 
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String pid, 
                      String productSkuId,
                      String productName,
                      boolean testData) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonFields (pixelData, userRecord, logTime, testData);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {

            // pixeltype = event 
            pixelData.setParam (PixelBRData.PARAMNAME_PIXEL_TYPE, PixelBRData.PIXEL_TYPE_EVENT);

            // eventgroup 
            pixelData.setParam (PixelBRData.PARAMNAME_EVENT_GROUP, PixelBRData.EVENT_GROUP_CART);

            // etype (aka action type)
            pixelData.setParam (PixelBRData.PARAMNAME_EVENT_ETYPE, PixelBRData.EVENT_ETYPE_CLICKADD);

            // pid
            pixelData.setParam (PixelBRData.PARAMNAME_PROD_ID, pid);

            // sku, if it is available
            if (productSkuId != null) {
                pixelData.setParam (PixelBRData.PARAMNAME_SKU, productSkuId);
            }

            // ref_url == set to product-page-url on which add-to-card button is clicked
            pixelData.setParam (PixelBRData.PARAMNAME_REF_URL, refUrl);

            // addToCart page url  == same as the product page's url
            pixelData.setParam (PixelBRData.PARAMNAME_URL, url);

            // also include the source (product) page's info
            // ptype, prodName
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TYPE, PixelBRData.PAGE_TYPE_PRODUCT);
            pixelData.setParam (PixelBRData.PARAMNAME_PROD_NAME, productName);

            // update pixelCount
            updatePixelCountLog(PixelBRData.PIXEL_TYPE_EVENT, PixelBRData.EVENT_GROUP_CART, PixelBRData.EVENT_ETYPE_CLICKADD);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }
}

