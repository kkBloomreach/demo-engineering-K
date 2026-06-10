package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.DiscoveryUserAccess;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeydata.StartUrlPool;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepStartUrl extends StepBase {

    public StepStartUrl () {
    }

    // select pid from searchResponse
    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  String urlType,  // defined in startUrlPool
                                  PixelTemplates pixelTemplates,
                                  DiscoveryUserAccess DiscoveryUserAccess,
                                  boolean testData) throws Exception {

        StepResult thisStepResult;
        String startPageUrl;

        MessageLogger.logDebug ("Handle step start url - 'home', 'other'");

        startPageUrl = handleStepInternal (prevStepResult, userRecord, logTime, urlType,
                                           pixelTemplates, DiscoveryUserAccess, testData);

        thisStepResult = new StepResultVoid ();
        super.setUrlHistory (prevStepResult, thisStepResult, startPageUrl);
        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_BROWSE_PDP);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_BROWSE_PDP);

        return (thisStepResult);
    }

    // returns start-page-url (home/other/...)
    private String handleStepInternal (StepResult prevStepResult,
                                     UserRecord userRecord,
                                     long logTime,
                                     String urlType,
                                     PixelTemplates pixelTemplates,
                                     DiscoveryUserAccess DiscoveryUserAccess, 
                                     boolean testData) throws Exception {

        PixelBRData pixelData;
        String nxtPageUrl;

        // NOTE: In case of "start" step, we keep the ref in this pixel same as prevStep 'ref'
        // since there is no real 'prevStep' in this case that WOULD have created its 'url' value
        // That 'ref' could be social-url/search-enging-url/blank/...
        if (urlType.equals (StartUrlPool.URL_TYPE_HOME)) {
            pixelData = buildHomePagePixelFromTemplate (userRecord, logTime, 
                                                        prevStepResult.getRefUrl(),
                                                        pixelTemplates, testData);
            nxtPageUrl = BuildHomePagePixel.getHomePageUrl ();

        } else if (urlType.equals (StartUrlPool.URL_TYPE_OTHER)) {
            pixelData = buildOtherPagePixelFromTemplate (userRecord, logTime, 
                                                         prevStepResult.getRefUrl(),
                                                         pixelTemplates, testData);
            nxtPageUrl = BuildOtherPagePixel.getOtherPageUrl ();
        } else {
            MessageLogger.logError ("Unsupported urlType in startUrl step; using urlType = home");
            pixelData = buildHomePagePixelFromTemplate (userRecord, logTime, 
                                                        prevStepResult.getRefUrl(),
                                                        pixelTemplates, testData);
            nxtPageUrl = BuildHomePagePixel.getHomePageUrl ();
        }

        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build start page pixel");
        } else {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        }

        return nxtPageUrl;
    }

    private PixelBRData buildHomePagePixelFromTemplate (UserRecord userRecord, 
                                                         long logTime, 
                                                         String refUrl, 
                                                         PixelTemplates pixelTemplates,
                                                         boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildHomePagePixel pixelBuilder;
        int buildStatus;
        String url;

        // prepare a product page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        url = SiteConfig.getUrlConfigParam ("HOMEPAGE_URL");
        pixelBuilder = new BuildHomePagePixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, refUrl, url, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }

    private PixelBRData buildOtherPagePixelFromTemplate (UserRecord userRecord, 
                                                         long logTime, 
                                                         String refUrl, 
                                                         PixelTemplates pixelTemplates,
                                                         boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildOtherPagePixel pixelBuilder;
        int buildStatus;
        String url;

        // prepare a product page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        // NOTE: Currently the 'other-page-builder' just builds a generic url
        url = BuildOtherPagePixel.getOtherPageUrl ();
        pixelBuilder = new BuildOtherPagePixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, refUrl, url, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }
}

