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

public class CloneProductPagePixel extends ClonePixelLogBase  {

    public CloneProductPagePixel () {
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

            // in many cases, the "pid" value is actually a comma-separated pid-list
            // That is not allowed
            if (origPid.indexOf (',') >= 0) {
                // System.out.println ("PDP Product id list not supported: " + origPid);
                return (CloneConstants.CLONE_STATUS_REJECT);
            }

            translatedProdId = generateUniqPid (origPid);
            if (translatedProdId == null) {
                return (CloneConstants.CLONE_STATUS_REJECT);
            }

            // see if this pid is infact in the processed feed
            if (processedFeed.isProductInFeed (translatedProdId) == false) {
                // System.out.println ("PDP product not in processed feed: " + translatedProdId);
                return (CloneConstants.CLONE_STATUS_REJECT);
            } 

            // change prodId in the pixel
            pixelLogBuilder.setProdId (translatedProdId);
            replacePixelLogParam (pixelLogBuilder, "prod_id", translatedProdId);

            // product page url
            String productUrl = generateProductUrl (translatedProdId);
            pixelLogBuilder.setUrl (productUrl);
            replacePixelLogParam (pixelLogBuilder, "url", productUrl);

            // product name
            String prodName = pixelLogBuilder.getProdName ();
            if (StringUtils.isNotEmpty (prodName) == true) {
                if (prodName.indexOf ("World Market") >= 0) {
                    // in many cases, the prod_name in input has a registered trademark symbol
                    // Therefore, replace "world market" with just a blank, not "pacifichome"
                    // Otherwise it will look as if "pacifichome" is registered-trademark as well 
                    prodName = prodName.replaceAll ("World Market", "");
                    pixelLogBuilder.setProdName (prodName);
                    replacePixelLogParam (pixelLogBuilder, "prod_name", prodName);
                }
            }
    
 
            cloneStatus = CloneConstants.CLONE_STATUS_OK;
        }

        return (cloneStatus);
    }

}


