package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.dispatch.Dispatcher;
import com.bloomreach.trafficgenerator.site.dispatch.SearchApiResponse;
import com.bloomreach.trafficgenerator.site.dispatch.SearchApiResponseDoc;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.build.apiparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepSuggestSelectTerm extends StepBase {

    public StepSuggestSelectTerm () {
    }

    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  String selectedAqTerm,  // "aq"
                                  String selectedQueryTerm,  // "q"
                                  CampaignRecord activeCampaignRecord,
                                  PixelTemplates pixelTemplates,
                                  ApiTemplates apiTemplates,
                                  Dispatcher dispatcher,
                                  boolean testData) throws Exception {

        StepResultSearchResponse thisStepResult;
        SearchApiResponse searchApiResponse;
        String srchPageUrl;
        StepResultInvalidData inputInvalid;

        MessageLogger.logDebug ("Handle step suggest select term: " + selectedQueryTerm);

        if (selectedQueryTerm == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSuggestSelectTerm, null suggest terms");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } 

        searchApiResponse = handleStepInternal (prevStepResult, userRecord, logTime, selectedAqTerm, selectedQueryTerm,
                                                activeCampaignRecord,
                                                pixelTemplates, apiTemplates, dispatcher, testData);
        if (searchApiResponse == null) {
            String searchResultPageUrl;

            // even when apiResponse is null, conceptually we are on that page, perhaps
            // with message '0 results' or 'exception'. Therefore, set ref and url for that page
            searchResultPageUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryTerm);

            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getUrl ());
            inputInvalid.setUrl (searchResultPageUrl);
            inputInvalid.setMessage ("StepSuggestSelectTerm, null suggest searchApi response");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        thisStepResult = new StepResultSearchResponse ();
        srchPageUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryTerm);
        super.setUrlHistory (prevStepResult, thisStepResult, srchPageUrl);
        thisStepResult.setSearchApiResponse (searchApiResponse);
        thisStepResult.setSearchTerm (selectedQueryTerm);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_SELECT_SUG_TERM);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_SELECT_SUG_TERM);

        return (thisStepResult);
    }

    private SearchApiResponse handleStepInternal (StepResult prevStepResult,
                                                  UserRecord userRecord,
                                                  long logTime,
                                                  String selectedAqTerm,
                                                  String selectedQueryTerm,
                                                  CampaignRecord activeCampaignRecord,
                                                  PixelTemplates pixelTemplates,
                                                  ApiTemplates apiTemplates,
                                                  Dispatcher dispatcher,
                                                  boolean testData) throws Exception {
        PixelBRData pixelData;
        SearchApiResponse searchApiResponse;
        PixelBRData suggestEventPixelData;

        // trigger suggest event pixel first. The refUrl, url do not change for SuggestEvent
        suggestEventPixelData = buildSuggestEventPixelFromTemplate (userRecord, logTime, 
                                                                    prevStepResult.getRefUrl(), // refUrl for suggestEvent
                                                                    prevStepResult.getUrl (),   // url for suggestEvent
                                                                    selectedAqTerm, selectedQueryTerm, 
                                                                    pixelTemplates, testData);
        if (suggestEventPixelData == null) {
            MessageLogger.logWarning ("Failed to build suggest event pixel");
        } else {
            dispatcher.dispatchPixel (suggestEventPixelData);
        }

        // search api call using the selectedQueryTerm
        searchApiResponse = collectSearchApiResponse (userRecord, logTime, 
                                                      prevStepResult.getRefUrl (),
                                                      prevStepResult.getUrl (),
                                                      selectedQueryTerm, activeCampaignRecord,  
                                                      apiTemplates, dispatcher);

        // SearchResultPage pixel. Even if searchApiResponse is null, conceptually
        // there is a page that shows "0 results" OR some exception message
        // Note: For searchResult page, the prevstep url is this page's ref
        pixelData = buildSearchResultPagePixelFromTemplate (userRecord, logTime, prevStepResult.getUrl(),
                                                            selectedQueryTerm, pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build search result page pixel");
        } else {
            dispatcher.dispatchPixel (pixelData);
        }

        return searchApiResponse;   // may be null
    }

    private PixelBRData buildSuggestEventPixelFromTemplate (UserRecord userRecord, 
                                                                long logTime, 
                                                                String refUrl,
                                                                String url, 
                                                                String selectedAqTerm,
                                                                String selectedQueryTerm,
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
                                          selectedAqTerm, selectedQueryTerm, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }

    // NOTE: This 'collect' method does a 'search' api call using the term picked from suggest-term-list
    private SearchApiResponse collectSearchApiResponse (UserRecord userRecord, 
                                                        long startTime,
                                                        String refUrl, 
                                                        String url, 
                                                        String selectedQueryTerm,
                                                        CampaignRecord activeCampaignRecord,
                                                        ApiTemplates apiTemplates,
                                                        Dispatcher dispatcher) throws Exception {
        ApiBRData apiData;
        SearchApiResponse searchApiResponse;
        ArrayList<SearchApiResponseDoc> allResponseDocs;

        // execute API call, get api response and pick a pid from that response
        // NOTE - this is 'search' api call, using a term selected from suggested-term-list
        apiData = buildSearchApiFromTemplate (userRecord, startTime, 
                                              refUrl, url,
                                              selectedQueryTerm, activeCampaignRecord, apiTemplates);
        if (apiData == null) {
            MessageLogger.logError ("Cannot populate API brData: " + selectedQueryTerm);
            return null;
        }

        searchApiResponse = dispatcher.getSearchApiResponse (apiData); 
        if (searchApiResponse == null) {
            MessageLogger.logError ("Suggest api response is null for selected search term: " + selectedQueryTerm);
            return null;
        }

        allResponseDocs = searchApiResponse.getResponseDocs ();
        if ((allResponseDocs == null) || (allResponseDocs.size () == 0)) {
            MessageLogger.logWarning ("Suggest api response has zero results for selected search term: " + selectedQueryTerm);
            return null;
        }

        return searchApiResponse;
    }

    private ApiBRData buildSearchApiFromTemplate (UserRecord userRecord, 
                                                  long logTime,
                                                  String refUrl, 
                                                  String url, 
                                                  String selectedQueryTerm,
                                                  CampaignRecord activeCampaignRecord,
                                                  ApiTemplates apiTemplates) throws Exception {
        ApiBRData searchApiData;
        BuildSearchApi buildSearchApi;
        int buildStatus;

        // prepare a search ApiBRData from template
        searchApiData = apiTemplates.loadApiTemplate (userRecord.getDeviceType());

        // update template
        buildSearchApi = new BuildSearchApi ();
        buildStatus = buildSearchApi.build (searchApiData, userRecord, logTime, refUrl, url,
                                            selectedQueryTerm, activeCampaignRecord);

        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return apiLog object from this builder
            return (searchApiData);
        }

        return (null);
    }

    private PixelBRData buildSearchResultPagePixelFromTemplate (UserRecord userRecord, 
                                                                long logTime, 
                                                                String refUrl, 
                                                                String selectedQueryTerm,
                                                                PixelTemplates pixelTemplates,
                                                                boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildSearchResultPagePixel pixelBuilder;
        int buildStatus;
        String url;

        // prepare a search result page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        url = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryTerm);
        pixelBuilder = new BuildSearchResultPagePixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, 
                                          refUrl, url,
                                          selectedQueryTerm, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }
}

