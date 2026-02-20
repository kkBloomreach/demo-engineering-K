package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

public class CloneAddToCartPixel extends ClonePixelLogBase  {

    public CloneAddToCartPixel () {
    }

    public int clonePixel (Builder pixelLogBuilder, UidToViewIdMap uidViewIdMap, ProcessedFeed processedFeed) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, uidViewIdMap, processedFeed);
        if (cloneStatus == ClonePixelConstants.CLONE_STATUS_OK) { 
    
            String translatedProdId;
 
            // set clone'd product id
            String origPid = pixelLogBuilder.getProdId ();
            if (StringUtils.isEmpty (origPid) == true) {
                System.out.println ("Product page pixel has no pid");
                return (ClonePixelConstants.CLONE_STATUS_ERROR);
            }

            translatedProdId = generateUniqPid (origPid);
            if (translatedProdId == null) {
                return (ClonePixelConstants.CLONE_STATUS_REJECT);
            }

            // see if this pid is infact in the processed feed
            if (processedFeed.isProductInFeed (translatedProdId) == false) {
                System.out.println ("Product not in processed feed: " + translatedProdId);
                return (ClonePixelConstants.CLONE_STATUS_REJECT);
            } 

            // change prodId in the pixel
            pixelLogBuilder.setProdId (translatedProdId);
            replacePixelLogParam (pixelLogBuilder, "prod_id", translatedProdId);

            // product page url
            String productUrl = generateProductUrl (translatedProdId);
            pixelLogBuilder.setUrl (productUrl);
            replacePixelLogParam (pixelLogBuilder, "url", productUrl);
 
            cloneStatus = ClonePixelConstants.CLONE_STATUS_OK;
        }

        return (cloneStatus);
    }

}


