package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.DiscoveryUserAccess;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.SearchApiResponse;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.SearchApiResponseDoc;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.build.apiparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.discoveryconnector.nonuseraccess.CategoryInfo;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepSearchCategory extends StepBase {

    public StepSearchCategory () {
    }

    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  CategoryInfo selectedCatInfo,
                                  CampaignRecord activeCampaignRecord,
                                  PixelTemplates pixelTemplates,
                                  ApiTemplates apiTemplates,
                                  DiscoveryUserAccess DiscoveryUserAccess,
                                  boolean testData) throws Exception {
        StepResultSearchResponse thisStepResult;
        SearchApiResponse searchApiResponse;
        StepResultInvalidData inputInvalid;
        String catPageUrl;

        if (selectedCatInfo == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSearchCategory, null categoryInfo");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } 

        MessageLogger.logDebug ("Handle step search category response: " + selectedCatInfo.getCatId());
        searchApiResponse = handleStepInternal (prevStepResult, userRecord, logTime, selectedCatInfo,
                                                activeCampaignRecord,
                                                pixelTemplates, apiTemplates, DiscoveryUserAccess, testData);

        if (searchApiResponse == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSelectCategory, null categorySearchApiResponse");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } 

        thisStepResult = new StepResultSearchResponse ();
        catPageUrl = BuildCategoryPagePixel.getCategoryPageUrl (selectedCatInfo.getCatId ());
        super.setUrlHistory (prevStepResult, thisStepResult, catPageUrl);
        thisStepResult.setSearchApiResponse (searchApiResponse);
        thisStepResult.setSearchCatId (selectedCatInfo.getCatId());

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_SEARCH_CAT);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_SEARCH_CAT);

        return (thisStepResult);
    }

    private SearchApiResponse handleStepInternal (StepResult prevStepResult,
                                     UserRecord userRecord,
                                     long logTime,
                                     CategoryInfo selectedCatInfo,
                                     CampaignRecord activeCampaignRecord,
                                     PixelTemplates pixelTemplates,
                                     ApiTemplates apiTemplates,
                                     DiscoveryUserAccess DiscoveryUserAccess,
                                     boolean testData) throws Exception {

        PixelBRData pixelData;
        SearchApiResponse searchApiResponse;

        // api call
        searchApiResponse = collectCategoryApiResponse (userRecord, logTime, 
                                                        prevStepResult.getRefUrl (), prevStepResult.getUrl (),
                                                        selectedCatInfo.getCatId (), activeCampaignRecord,  
                                                        apiTemplates, DiscoveryUserAccess);

        // pixel
        // NOTE: Prevstep url is this page's refUrl. Even if 'collect' returns
        // null response, this category-page-pixel is triggered. Conceptually
        // it could show '0 results' OR 'exception message'
        pixelData = buildCategoryPagePixelFromTemplate (userRecord, logTime, prevStepResult.getUrl(),
                                                       selectedCatInfo, pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build category page pixel");
        } else {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        }
        return searchApiResponse;   // may be null
    }

    private SearchApiResponse collectCategoryApiResponse (UserRecord userRecord, 
                                                        long startTime,
                                                        String refUrl, 
                                                        String url, 
                                                        String selectedCatId,
                                                        CampaignRecord activeCampaignRecord,
                                                        ApiTemplates apiTemplates,
                                                        DiscoveryUserAccess DiscoveryUserAccess) throws Exception {
        ApiBRData apiData;
        SearchApiResponse searchApiResponse;
        ArrayList<SearchApiResponseDoc> allResponseDocs;

        // execute API call, get api response and pick a pid from that response
        apiData = buildCategoryApiFromTemplate (userRecord, startTime, 
                                                refUrl, url,
                                                selectedCatId, activeCampaignRecord, apiTemplates);
        if (apiData == null) {
            MessageLogger.logError ("Cannot populate API brData: " + selectedCatId);
            return null;
        }

        searchApiResponse = DiscoveryUserAccess.getSearchApiResponse (apiData); 
        if (searchApiResponse == null) {
            MessageLogger.logError ("Category api response is null for search category: " + selectedCatId);
            return null;
        }

        allResponseDocs = searchApiResponse.getResponseDocs ();
        if ((allResponseDocs == null) || (allResponseDocs.size () == 0)) {
            MessageLogger.logWarning ("Category api response has zero results for search category: " + selectedCatId);
            return null;
        }

        return searchApiResponse;
    }

    private PixelBRData buildCategoryPagePixelFromTemplate (UserRecord userRecord, 
                                                         long logTime, 
                                                         String refUrl, 
                                                         CategoryInfo selectedCatInfo,
                                                         PixelTemplates pixelTemplates,
                                                         boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildCategoryPagePixel pixelBuilder;
        int buildStatus;
        String url;

        // prepare a product page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildCategoryPagePixel ();
        url = BuildCategoryPagePixel.getCategoryPageUrl (selectedCatInfo.getCatId ());
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, 
                                          refUrl, url,
                                          selectedCatInfo.getCatId (), 
                                          selectedCatInfo.getCatName (),
                                          selectedCatInfo.getCatPath (),
                                          testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }

    private ApiBRData buildCategoryApiFromTemplate (UserRecord userRecord, 
                                                    long logTime,
                                                    String refUrl, 
                                                    String url, 
                                                    String selectedCatId,
                                                    CampaignRecord activeCampaignRecord,
                                                    ApiTemplates apiTemplates) throws Exception {
        ApiBRData categoryApiData;
        BuildCategoryApi buildCategoryApi;
        int buildStatus;

        // prepare a search ApiBRData from template
        categoryApiData = apiTemplates.loadApiTemplate (userRecord.getDeviceType());

        // update template
        buildCategoryApi = new BuildCategoryApi ();
        buildStatus = buildCategoryApi.build (categoryApiData, userRecord, logTime, refUrl, url,
                                              selectedCatId, activeCampaignRecord);

        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return apiLog object from this builder
            return (categoryApiData);
        }

        return (null);
    }
}

