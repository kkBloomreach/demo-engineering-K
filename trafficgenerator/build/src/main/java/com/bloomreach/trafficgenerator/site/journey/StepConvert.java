package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.DiscoveryUserAccess;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeylogs.StepLog;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepConvert extends StepBase {

    public StepConvert () {
    }

    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  StepLog stepLog,
                                  Cart userCart,
                                  OrderIdGenerator orderIdGenerator,
                                  PixelTemplates pixelTemplates,
                                  DiscoveryUserAccess DiscoveryUserAccess,
                                  boolean testData) throws Exception {
        StepResult thisStepResult;
        StepResultInvalidData inputInvalid;
        String thankyouRefUrl;
        String thankyouUrl;

        MessageLogger.logDebug ("Handle step Convert");

        if ((userCart == null) || (userCart.getItems () == null)) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepConvert, invalid cart");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } else if (userCart.getItems ().size() == 0) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepConvert, empty cart");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }
        
        handleStepInternal (prevStepResult, userRecord, logTime, stepLog, userCart, 
                            orderIdGenerator, pixelTemplates, DiscoveryUserAccess, testData);

        thisStepResult = new StepResultVoid ();
        // NOTE: final url = 'thankyou'. Internally, a conversion pixel + post-conversion-page pixels are 
        // dispatched
        thankyouUrl = BuildPostConversionPagePixel.getPostConversionPageUrl ();
        super.setUrlHistory (prevStepResult, thisStepResult, thankyouUrl);

        // Since two page pixels (conversion followed by thankyou) are dispatched, 
        // set the thankyou stepResult's 'ref' to  'conversion' page
        thankyouRefUrl = SiteConfig.getUrlConfigParam ("CONVERSION_PAGE_URL"); // conversion page url "/checkout" == thankyou_ref_url
        thisStepResult.setRefUrl (thankyouRefUrl);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_CONVERT);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_CONVERT);

        return (thisStepResult);
    }

    private void handleStepInternal (StepResult prevStepResult, 
                                     UserRecord userRecord,
                                     long logTime,
                                     StepLog stepLog,
                                     Cart userCart,
                                     OrderIdGenerator orderIdGenerator,
                                     PixelTemplates pixelTemplates,
                                     DiscoveryUserAccess DiscoveryUserAccess, 
                                     boolean testData) throws Exception {

        PixelBRData pixelData;
        String thankyouUrl;
        String refUrl;
        String url;

        // dispatch 'conversion' pixel
        refUrl = prevStepResult.getUrl ();  // prev result's "url" is this page's ref-url
        url = SiteConfig.getUrlConfigParam ("CONVERSION_PAGE_URL"); // conversion page url
        pixelData = buildConversionPixelFromTemplate (userRecord, logTime, 
                                                      refUrl, url,
                                                      stepLog, 
                                                      userCart, orderIdGenerator, 
                                                      pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build conversion pixel");
        } else {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        }

        // dispatch 'post-conversion' page pixel. We assume all conversions are always successful
        // For this page, "refUrl" is always 'checkout' page, url is always 'thankyou'
        refUrl = url; // conversion-page-url is thankyou page's 'refUrl'
        thankyouUrl = BuildPostConversionPagePixel.getPostConversionPageUrl ();
        pixelData = buildPostConversionPixelFromTemplate (userRecord, logTime, 
                                                          refUrl, 
                                                          thankyouUrl, 
                                                          pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build post conversion pixel");
        } else {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        }
    }

    private PixelBRData buildConversionPixelFromTemplate (UserRecord userRecord, 
                                                          long logTime,
                                                          String refUrl,
                                                          String url,
                                                          StepLog stepLog, 
                                                          Cart userCart,
                                                          OrderIdGenerator orderIdGenerator, 
                                                          PixelTemplates pixelTemplates,
                                                          boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildConversionPixel pixelBuilder;
        int buildStatus;

        // prepare a ATC page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildConversionPixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, refUrl, url,
                                          stepLog,
                                          userCart, orderIdGenerator, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }

    private PixelBRData buildPostConversionPixelFromTemplate (UserRecord userRecord, 
                                                              long logTime,
                                                              String refUrl,
                                                              String url,
                                                              PixelTemplates pixelTemplates,
                                                              boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildPostConversionPagePixel pixelBuilder;
        int buildStatus;

        // prepare a ATC page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildPostConversionPagePixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, refUrl, url, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }
}

