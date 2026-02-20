package com.bloomreach.analyticssimulator.build.pixellog;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.SimulatorConfig;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;

public class BuildProductPagePixel extends BuildPixelBase  {

    public BuildProductPagePixel () {
    }

    public int build (PixelLog.Builder pixelLogBuilder, UidToSegmentRecord uidToSegmentRecord, long logTime, 
                      String refUrl, String pid, String productName, String productSkuId) throws Exception {
        // let base class update 'common' fields
        int simulateStatus;

        simulateStatus = setCommonFields (pixelLogBuilder, uidToSegmentRecord, logTime);
        if (simulateStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            String productUrl;
            String encodedProductName;

            // pid
            pixelLogBuilder.setProdId (pid);
            replacePixelLogParam (pixelLogBuilder, "prod_id", pid);

            // sku.
            if (productSkuId != null) { 
                // setProdSku not a builder.API
                // pixelLogBuilder.setProdSku(productSkuId);
                // Add param if it is not already in the pixel template. Not sure what should be
                // the paramName (prod_sku or sku) ? Checked with engr team but no confirmation
                if (replacePixelLogParam (pixelLogBuilder, "prod_sku", productSkuId) == false) 
                    addPixelLogParam (pixelLogBuilder, "prod_sku", productSkuId);
                if (replacePixelLogParam (pixelLogBuilder, "sku", productSkuId) == false) 
                    addPixelLogParam (pixelLogBuilder, "sku", productSkuId);
            }

            // ref_url
            pixelLogBuilder.setRefUrl (refUrl);
            replacePixelLogParam (pixelLogBuilder, "ref", refUrl);

            // product page Url for this pid
            productUrl = getProductPageUrl (pid, productSkuId);
            pixelLogBuilder.setUrl (productUrl);
            replacePixelLogParam (pixelLogBuilder, "url", productUrl);

            // product name
            encodedProductName = URLEncoder.encode (productName);
            pixelLogBuilder.setProdName (encodedProductName);
            replacePixelLogParam (pixelLogBuilder, "prod_name", encodedProductName);

            simulateStatus = SimulatorConstants.SIMULATE_STATUS_OK; 
        }

        return (simulateStatus);
    }

    // this method is called within this class and also externally so that
    // its value can be further used as 'ref_url' in subsequent pixels
    public static String getProductPageUrl (String pid, String productSkuId) {
        String productUrl;

        if (productSkuId != null)
            productUrl = SimulatorConfig.getConfigParam ("PRODUCT_URL_PREFIX")  + pid + "___" + productSkuId;
        else
            productUrl = SimulatorConfig.getConfigParam ("PRODUCT_URL_PREFIX")  + pid + "___" + pid;
        return productUrl;
    }
}

