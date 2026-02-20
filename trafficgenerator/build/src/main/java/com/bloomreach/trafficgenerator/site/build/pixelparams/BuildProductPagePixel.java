package com.bloomreach.trafficgenerator.site.build.pixelparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.config.*;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class BuildProductPagePixel extends BuildPixelBase  {

    public BuildProductPagePixel () {
    }

    public int build (PixelBRData pixelData, 
                      UserRecord userRecord, 
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String pid, 
                      String productName, 
                      String productSkuId,
                      boolean testData) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonFields (pixelData, userRecord, logTime, testData);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String title;

            // title (similar to the one currently set in SPA)
            title = String.format ("Product Details | %s", SiteConfig.getAccountConfigParam ("DOMAIN"));
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TITLE, title);

            // pixeltype = pageview
            pixelData.setParam (PixelBRData.PARAMNAME_PIXEL_TYPE, PixelBRData.PIXEL_TYPE_PAGEVIEW);

            // ptype
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TYPE, PixelBRData.PAGE_TYPE_PRODUCT);

            // pid
            pixelData.setParam (PixelBRData.PARAMNAME_PROD_ID, pid);

            // sku.
            if (productSkuId != null) { 
                pixelData.setParam (PixelBRData.PARAMNAME_SKU, productSkuId);
            }

            // ref_url
            pixelData.setParam (PixelBRData.PARAMNAME_REF_URL, refUrl);

            // product page Url for this pid
            pixelData.setParam (PixelBRData.PARAMNAME_URL, url);

            // product name 
            // productName encoded when generating queryParam (see Dispatcher.java)
            pixelData.setParam (PixelBRData.PARAMNAME_PROD_NAME, productName);

            // update pixelCount
            updatePixelCountLog(PixelBRData.PIXEL_TYPE_PAGEVIEW, PixelBRData.PAGE_TYPE_PRODUCT);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }

    // this method is called within this class and also externally so that
    // its value can be further used as 'ref_url' in subsequent pixels
    public static String getProductPageUrl (String pid, String productSkuId) {
        String productUrl;
        String productUrlConfig;
        int indx;

        // check if sku should be appended in the PDP url. It is needed for
        // Pacific* SPA but must not exist for Shopify. 
        productUrlConfig = SiteConfig.getUrlConfigParam("PRODUCT_URL_PATTERN");
        indx = productUrlConfig.indexOf("&");
        if (indx > 0) {
            String basePrefix;

            basePrefix = productUrlConfig.substring(0,indx); // /products/
            if (productSkuId != null)
                productUrl = basePrefix  + pid + "___" + productSkuId;
            else
                productUrl = basePrefix  + pid + "___" + pid;
        } else {
            productUrl = productUrlConfig  + pid;
        }
        return productUrl;
    }
}

