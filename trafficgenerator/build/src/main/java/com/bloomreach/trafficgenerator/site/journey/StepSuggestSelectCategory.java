package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.DiscoveryUserAccess;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.SearchApiResponse;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.SearchApiResponseDoc;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.build.apiparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.CategoryCollector;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.discoveryconnector.nonuseraccess.CategoryInfo;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepSuggestSelectCategory extends StepBase {

    public StepSuggestSelectCategory () {
    }

    // note - result is 'searchResponse' (ie, select a catId, execure category api call, return that response)
    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  String selectedAqTerm, // "aq"
                                  String selectedQueryCatId, // "q"
                                  CampaignRecord activeCampaignRecord,
                                  PixelTemplates pixelTemplates,
                                  ApiTemplates apiTemplates,
                                  DiscoveryUserAccess DiscoveryUserAccess,
                                  CategoryCollector categoryCollector,
                                  boolean testData) throws Exception {

        StepResultSearchResponse thisStepResult;
        SearchApiResponse searchApiResponse;
        String catPageUrl;
        StepResultInvalidData inputInvalid;
        CategoryInfo selectedCatInfo;

        MessageLogger.logDebug ("Handle step suggest select category: " + selectedQueryCatId);

        if (selectedQueryCatId == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSuggestSelectCategory, null suggestCategories");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } 

        selectedCatInfo = categoryCollector.lookupCategoryInfo (selectedQueryCatId);
        if (selectedCatInfo == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSuggestSelectCategory, no categoryInfo for " + selectedQueryCatId);
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } 

        searchApiResponse = handleStepInternal (prevStepResult, userRecord, logTime, 
                                                selectedAqTerm, selectedCatInfo,
                                                activeCampaignRecord,
                                                pixelTemplates, apiTemplates, DiscoveryUserAccess, categoryCollector, testData);

        // may be null if we could not find info for selected category
        if (searchApiResponse == null) {
            // even when apiResponse is null, conceptually we are on that page, perhaps
            // with message '0 results' or 'exception'. Therefore, set ref and url for that page
            catPageUrl = BuildCategoryPagePixel.getCategoryPageUrl (selectedQueryCatId);

            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getUrl ());
            inputInvalid.setUrl (catPageUrl);
            inputInvalid.setMessage ("StepSuggestSelectCategory, null categorySearchApiResponse");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        thisStepResult = new StepResultSearchResponse ();
        catPageUrl = BuildCategoryPagePixel.getCategoryPageUrl (selectedQueryCatId);
        super.setUrlHistory (prevStepResult, thisStepResult, catPageUrl);
        thisStepResult.setSearchApiResponse (searchApiResponse);    // may be empty (ie, numdocs = 0)
        thisStepResult.setSearchCatId (selectedQueryCatId);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_SELECT_SUG_CAT);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_SELECT_SUG_CAT);

        return (thisStepResult);
    }

    private SearchApiResponse handleStepInternal (StepResult prevStepResult,
                                                  UserRecord userRecord,
                                                  long logTime,
                                                  String selectedAqTerm,
                                                  CategoryInfo selectedCatInfo,
                                                  CampaignRecord activeCampaignRecord,
                                                  PixelTemplates pixelTemplates,
                                                  ApiTemplates apiTemplates,
                                                  DiscoveryUserAccess DiscoveryUserAccess,
                                                  CategoryCollector categoryCollector,
                                                  boolean testData) throws Exception {
        SearchApiResponse searchApiResponse;
        PixelBRData pixelData;
        PixelBRData suggestEventPixelData;

        // trigger suggest event pixel first. The refUrl, url do not change for SuggestEvent
        suggestEventPixelData = buildSuggestEventPixelFromTemplate (userRecord, logTime, 
                                                                    prevStepResult.getRefUrl(), // refUrl for suggestEvent
                                                                    prevStepResult.getUrl (),   // url for suggestEvent
                                                                    selectedAqTerm, selectedCatInfo.getCatId(), 
                                                                    pixelTemplates, testData);
        if (suggestEventPixelData == null) {
            MessageLogger.logWarning ("Failed to build suggest event pixel");
        } else {
            DiscoveryUserAccess.dispatchPixel (suggestEventPixelData);
        }

        // search api call using the selectedTerm
        searchApiResponse = collectCategoryApiResponse (userRecord, logTime, 
                                                        prevStepResult.getRefUrl (), 
                                                        prevStepResult.getUrl (),
                                                        selectedCatInfo.getCatId(), 
                                                        activeCampaignRecord,  
                                                        apiTemplates, DiscoveryUserAccess);

        // CategoryPage pixel. Even if searchApiResponse is null, conceptually
        // there is a page that shows "0 results" OR some exception message
        // Note: For this page, the prevstep url is this page's ref
        pixelData = buildCategoryPagePixelFromTemplate (userRecord, logTime, prevStepResult.getUrl(),
                                                        selectedCatInfo, pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build category page pixel");
        } else {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        } 

        return searchApiResponse;
    }

    private PixelBRData buildSuggestEventPixelFromTemplate (UserRecord userRecord, 
                                                                long logTime, 
                                                                String refUrl,
                                                                String url, 
                                                                String selectedAqTerm,
                                                                String selectedQueryCatId,
                                                                PixelTemplates pixelTemplates,
                                                                boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildSuggestEventPixel pixelBuilder;
        int buildStatus;

        // prepare a suggestEvent pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildSuggestEventPixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, refUrl, url,
                                          selectedAqTerm, selectedQueryCatId, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }

    // Note: This collect method does a 'search' api call using the catId picked from suggest-category-list
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
            MessageLogger.logError ("Suggest api response is null for selected category: " + selectedCatId);
            return null;
        }

        allResponseDocs = searchApiResponse.getResponseDocs ();
        if ((allResponseDocs == null) || (allResponseDocs.size () == 0)) {
            MessageLogger.logWarning ("Suggest api response has zero results for selected category: " + selectedCatId);
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

