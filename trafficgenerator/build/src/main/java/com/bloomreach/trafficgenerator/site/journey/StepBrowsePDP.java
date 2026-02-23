package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.dispatch.Dispatcher;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepBrowsePDP extends StepBase {

    public StepBrowsePDP () {
    }

    // select pid from searchResponse
    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  ProductDetails productDetails,
                                  PixelTemplates pixelTemplates,
                                  Dispatcher dispatcher,
                                  boolean testData) throws Exception {

        StepResultProductDetails thisStepResult;
        String pdpUrl;

        MessageLogger.logDebug ("Handle step browse PDP");

        if (productDetails == null) {
            StepResultInvalidData inputInvalid;

            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepBrowsePDP, null productDetail");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        handleStepInternal (prevStepResult, userRecord, logTime, productDetails,
                            pixelTemplates, dispatcher, testData);

        thisStepResult = new StepResultProductDetails ();
        pdpUrl = BuildProductPagePixel.getProductPageUrl (productDetails.getPid (), productDetails.getSkuid());
        super.setUrlHistory (prevStepResult, thisStepResult, pdpUrl);
        thisStepResult.setProductDetails (productDetails);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_BROWSE_PDP);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_BROWSE_PDP);

        return (thisStepResult);
    }

    private void handleStepInternal (StepResult prevStepResult,
                                     UserRecord userRecord,
                                     long logTime,
                                     ProductDetails productDetails,
                                     PixelTemplates pixelTemplates,
                                     Dispatcher dispatcher, 
                                     boolean testData) throws Exception {

        PixelBRData pixelData;

        // NOTE: prevStep url is this page's ref
        pixelData = buildProductPagePixelFromTemplate (userRecord, logTime, prevStepResult.getUrl(),
                                                       productDetails, pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build product page pixel");
        } else {
            dispatcher.dispatchPixel (pixelData);
        }
    }

    private PixelBRData buildProductPagePixelFromTemplate (UserRecord userRecord, 
                                                         long logTime, 
                                                         String refUrl, 
                                                         ProductDetails productDetails,
                                                         PixelTemplates pixelTemplates,
                                                         boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildProductPagePixel pixelBuilder;
        int buildStatus;
        String url;
        
        // prepare a product page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        // if productdetail is from suggestAPI response, it will have br_psugg....
        // url will have "___" since FeedAlterator sets it as <pid>___<skuid>
        url = productDetails.getUrl (); 
        if (url == null)    // really, this should not happen...
            url = BuildProductPagePixel.getProductPageUrl (productDetails.getPid (), productDetails.getSkuid());

        pixelBuilder = new BuildProductPagePixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, 
                                          refUrl, url,
                                          productDetails.getPid(), productDetails.getTitle (), productDetails.getSkuid (), 
                                          testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }
}

