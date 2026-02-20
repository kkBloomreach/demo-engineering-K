package com.bloomreach.analyticssimulator.build.pixellog;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.SimulatorConfig;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;

public class BuildAddToCartPixel extends BuildPixelBase  {

    public BuildAddToCartPixel () {
    }

    public int build (PixelLog.Builder pixelLogBuilder, UidToSegmentRecord uidToSegmentRecord, 
                      long logTime, String refUrl, String pid, String productSkuId) throws Exception {
        // let base class update 'common' fields
        int simulateStatus;

        simulateStatus = setCommonFields (pixelLogBuilder, uidToSegmentRecord, logTime);
        if (simulateStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            String atcPageUrl;

            // pid
            pixelLogBuilder.setProdId (pid);
            replacePixelLogParam (pixelLogBuilder, "prod_id", pid);

            // sku, if it is available
            if (productSkuId != null) {
                // setProdSku not in builder API
                // pixelLogBuilder.setProdSku(productSkuId);
                // Add param if it is not already in the pixel template. Not sure what should be
                // the paramName (prod_sku or sku) ? Checked with engr team but no confirmation
                if (replacePixelLogParam (pixelLogBuilder, "prod_sku", productSkuId) == false) 
                    addPixelLogParam (pixelLogBuilder, "prod_sku", productSkuId);
                if (replacePixelLogParam (pixelLogBuilder, "sku", productSkuId) == false) 
                    addPixelLogParam (pixelLogBuilder, "sku", productSkuId);
            }


            // ref_url == set to product-page-url on which add-to-card button is clicked
            pixelLogBuilder.setRefUrl (refUrl);
            replacePixelLogParam (pixelLogBuilder, "ref", refUrl);

            // addToCart page url  == same as the product page's url
            atcPageUrl = BuildProductPagePixel.getProductPageUrl (pid, pid);
            pixelLogBuilder.setUrl (atcPageUrl);
            replacePixelLogParam (pixelLogBuilder, "url", atcPageUrl);

            simulateStatus = SimulatorConstants.SIMULATE_STATUS_OK; 
        }

        return (simulateStatus);
    }
}

