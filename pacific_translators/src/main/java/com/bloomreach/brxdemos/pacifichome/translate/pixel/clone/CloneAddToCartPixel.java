package com.bloomreach.brxdemos.pacifichome.translate.pixel.clone;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.CloneConstants;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.ProductURLPidMapReader;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.CategoryURLCrumbMapReader;

public class CloneAddToCartPixel extends ClonePixelLogBase  {

    public CloneAddToCartPixel () {
    }

    public int clonePixel (Builder pixelLogBuilder, ProcessedFeed processedFeed, ProductURLPidMapReader productUrlPidMapReader, CategoryURLCrumbMapReader catUrlCrumbMapReader) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
        if (cloneStatus == CloneConstants.CLONE_STATUS_OK) { 
            String translatedProdId;
 
            // set clone'd product id
            String origPid = pixelLogBuilder.getProdId ();
            if (StringUtils.isEmpty (origPid) == true) {
                System.out.println ("Product page pixel has no pid");
                return (CloneConstants.CLONE_STATUS_ERROR);
            }

            translatedProdId = generateUniqPid (origPid);
            if (translatedProdId == null) {
                return (CloneConstants.CLONE_STATUS_REJECT);
            }

            // see if this pid is infact in the processed feed
            if (processedFeed.isProductInFeed (translatedProdId) == false) {
                System.out.println ("ATC product not in processed feed: " + translatedProdId);
                return (CloneConstants.CLONE_STATUS_REJECT);
            } 

            // change prodId in the pixel
            pixelLogBuilder.setProdId (translatedProdId);
            replacePixelLogParam (pixelLogBuilder, "prod_id", translatedProdId);

            // atc 'url' == set to url of productPage where add-to-cart button is clicked
            String productUrl = generateProductUrl (translatedProdId);
            pixelLogBuilder.setUrl (productUrl);
            replacePixelLogParam (pixelLogBuilder, "url", productUrl);
 
            // atc 'ref_url' remains same as in the source (but translated to PacificHome format) 
            // This is done in the Base class where ALL ref_urls are set for different pixel/pixel-types/...

            cloneStatus = CloneConstants.CLONE_STATUS_OK;
        }

        return (cloneStatus);
    }

}


