package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.DiscoveryUserAccess;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepATC extends StepBase {

    public StepATC () {
    }

    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  ProductDetails productDetails,
                                  Cart userCart,
                                  PixelTemplates pixelTemplates,
                                  DiscoveryUserAccess DiscoveryUserAccess,
                                  boolean testData) throws Exception {
        StepResult thisStepResult;

        MessageLogger.logDebug ("Handle step ATC");

        if (productDetails == null) {
            StepResultInvalidData inputInvalid;

            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepATC, null productDetail");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        handleStepInternal (prevStepResult, userRecord, logTime, productDetails, userCart, 
                            pixelTemplates, DiscoveryUserAccess, testData);

        thisStepResult = new StepResultVoid ();
        super.setUrlHistory (prevStepResult, thisStepResult, null); // null => this.url == prev.url
        
        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_ATC);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_ATC);

        return (thisStepResult);
    }

    private void handleStepInternal (StepResult prevStepResult, 
                                     UserRecord userRecord,
                                     long logTime,
                                     ProductDetails productDetails,
                                     Cart userCart,
                                     PixelTemplates pixelTemplates,
                                     DiscoveryUserAccess DiscoveryUserAccess, 
                                     boolean testData)  throws Exception {

        PixelBRData pixelData;
        CartItem cartItem;
        int maxQuantity;
        int quantity;
        double salePrice;

        // ATC. RefUrl,url for a ATC-event is the ref-url,url for the productPage 
        // where add-to-cart event is triggered.  Therefore, those values remain same as above
        pixelData = buildAddToCartEventPixelFromTemplate (userRecord, logTime, 
                                                          prevStepResult.getRefUrl(),
                                                          prevStepResult.getUrl(),
                                                          productDetails, pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build ATC pixel");
        } else {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        }

        // quantity - 
        // sale_price 0-20: max 3
        // sale_price 20-100: max 2
        // sale_price 100-200: max 1
        // sale_price > 200: max 1
        salePrice = productDetails.getSalePrice ();
        if (salePrice < 20)
            maxQuantity = 3;
        else if (salePrice < 100)
            maxQuantity = 2;
        else if (salePrice < 200)
            maxQuantity = 1;
        else
            maxQuantity = 1;

        if (maxQuantity > 1)
            quantity = ((int) (Math.random() * maxQuantity)) + 1;
        else
            quantity = maxQuantity;

        // add product to the cart
        cartItem = new CartItem ();
        cartItem.setItem (productDetails);
        cartItem.setQuantity (quantity);
        userCart.addItem (cartItem);
    }

    private PixelBRData buildAddToCartEventPixelFromTemplate (UserRecord userRecord, 
                                                              long logTime, 
                                                              String refUrl, 
                                                              String url, 
                                                              ProductDetails productDetails,
                                                              PixelTemplates pixelTemplates,
                                                              boolean testData)  throws Exception {
        PixelBRData atcPixelData;
        BuildAddToCartEventPixel buildATCPixel;
        int buildStatus;

        // prepare a ATC page pixelLog template
        atcPixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        buildATCPixel = new BuildAddToCartEventPixel ();
        buildStatus = buildATCPixel.build (atcPixelData, userRecord, logTime, refUrl, url,
                                           productDetails.getPid (), productDetails.getSkuid (), productDetails.getTitle (),
                                           testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (atcPixelData);
        }

        return (null);
    }
}

