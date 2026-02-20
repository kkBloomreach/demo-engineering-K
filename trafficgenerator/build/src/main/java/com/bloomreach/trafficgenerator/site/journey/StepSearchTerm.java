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

public class StepSearchTerm extends StepBase {

    public StepSearchTerm () {
    }

    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  String selectedTerm,
                                  CampaignRecord activeCampaignRecord,
                                  PixelTemplates pixelTemplates,
                                  ApiTemplates apiTemplates,
                                  Dispatcher dispatcher,
                                  boolean testData) throws Exception {

        StepResultSearchResponse thisStepResult;
        SearchApiResponse searchApiResponse;
        StepResultInvalidData inputInvalid;
        String srchPageUrl;

        MessageLogger.logDebug ("Handle step search term: " + selectedTerm);

        if (selectedTerm == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSearchTerm , null search term");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        searchApiResponse = handleStepInternal (prevStepResult, userRecord, logTime, selectedTerm,
                                                activeCampaignRecord,
                                                pixelTemplates, apiTemplates, dispatcher, testData);

        if (searchApiResponse == null) {
            String searchResultPageUrl;

            // even when apiResponse is null, conceptually we are on that page, perhaps
            // with message '0 results' or 'exception'. Therefore, set ref and url for that page
            searchResultPageUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedTerm);

            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getUrl ());
            inputInvalid.setUrl (searchResultPageUrl);
            inputInvalid.setMessage ("StepSearchTerm , null search api response for " + selectedTerm);
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } 

        thisStepResult = new StepResultSearchResponse ();
        srchPageUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedTerm);
        super.setUrlHistory (prevStepResult, thisStepResult, srchPageUrl);
        thisStepResult.setSearchApiResponse (searchApiResponse);    // api response may be empty (ie, numdocs = 0)
        thisStepResult.setSearchTerm (selectedTerm);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_SEARCH_TERM);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_SEARCH_TERM);


        return (thisStepResult);
    }

    private SearchApiResponse handleStepInternal (StepResult prevStepResult,
                                                  UserRecord userRecord,
                                                  long logTime,
                                                  String selectedTerm,
                                                  CampaignRecord activeCampaignRecord,
                                                  PixelTemplates pixelTemplates,
                                                  ApiTemplates apiTemplates,
                                                  Dispatcher dispatcher,
                                                  boolean testData) throws Exception {

        PixelBRData pixelData;
        SearchApiResponse searchApiResponse;

        // search event pixel
        // NOTE: For event pixel, ref and url remain the same as prevStep ref, url
        pixelData = buildSearchEventPixelFromTemplate (userRecord, logTime, 
                                                       prevStepResult.getRefUrl(), prevStepResult.getUrl (),
                                                       selectedTerm, pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build search event pixel");
        } else {
            dispatcher.dispatchPixel (pixelData);
        }

        // api call
        searchApiResponse = collectSearchApiResponse (userRecord, logTime, 
                                                      prevStepResult.getRefUrl (), prevStepResult.getUrl (),
                                                      selectedTerm, activeCampaignRecord,  
                                                      apiTemplates, dispatcher);

        // SearchResultPage pixel. Even if searchApiResponse is null, conceptually
        // there is a page that shows "0 results" OR some exception message
        // Note: For searchResult page, the prevstep url is this page's ref
        pixelData = buildSearchResultPagePixelFromTemplate (userRecord, logTime, 
                                                            prevStepResult.getUrl(),
                                                            selectedTerm, pixelTemplates, testData);
        if (pixelData == null) {
            MessageLogger.logWarning ("Failed to build search result page pixel");
        } else {
            dispatcher.dispatchPixel (pixelData);
        }

        return searchApiResponse;   // may be null
    }

    private SearchApiResponse collectSearchApiResponse (UserRecord userRecord, 
                                                        long startTime,
                                                        String refUrl, 
                                                        String url, 
                                                        String selectedTerm,
                                                        CampaignRecord activeCampaignRecord,
                                                        ApiTemplates apiTemplates,
                                                        Dispatcher dispatcher) throws Exception {
        ApiBRData apiData;
        SearchApiResponse searchApiResponse;
        ArrayList<SearchApiResponseDoc> allResponseDocs;

        // execute API call, get api response and pick a pid from that response
        apiData = buildSearchApiFromTemplate (userRecord, startTime, 
                                              refUrl, url, 
                                              selectedTerm, activeCampaignRecord, apiTemplates);
        if (apiData == null) {
            MessageLogger.logError ("Cannot populate API brData: " + selectedTerm);
            return null;
        }

        searchApiResponse = dispatcher.getSearchApiResponse (apiData); 
        if (searchApiResponse == null) {
            MessageLogger.logError ("Keyword search api response is null for search term: " + selectedTerm);
            return null;
        }

        allResponseDocs = searchApiResponse.getResponseDocs ();
        if ((allResponseDocs == null) || (allResponseDocs.size () == 0)) {
            MessageLogger.logWarning ("Keyword search api response has zero results for search term: " + selectedTerm);
            return null;
        }

        return searchApiResponse;
    }

    private PixelBRData buildSearchEventPixelFromTemplate (UserRecord userRecord, 
                                                                long logTime, 
                                                                String refUrl, 
                                                                String url, 
                                                                String selectedTerm,
                                                                PixelTemplates pixelTemplates,
                                                                boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildSearchEventPixel pixelBuilder;
        int buildStatus;

        // prepare a searchEvent pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildSearchEventPixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, refUrl, url,
                                          selectedTerm, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }

    private PixelBRData buildSearchResultPagePixelFromTemplate (UserRecord userRecord, 
                                                                long logTime, 
                                                                String refUrl, 
                                                                String selectedTerm,
                                                                PixelTemplates pixelTemplates,
                                                                boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildSearchResultPagePixel pixelBuilder;
        int buildStatus;
        String url;

        // prepare a search result page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        url = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedTerm);
        pixelBuilder = new BuildSearchResultPagePixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, 
                                          refUrl, url,
                                          selectedTerm, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }

    private ApiBRData buildSearchApiFromTemplate (UserRecord userRecord, 
                                                  long logTime,
                                                  String refUrl, 
                                                  String url, 
                                                  String selectedTerm,
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
                                              selectedTerm, activeCampaignRecord);

        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return apiLog object from this builder
            return (searchApiData);
        }

        return (null);
    }
}

