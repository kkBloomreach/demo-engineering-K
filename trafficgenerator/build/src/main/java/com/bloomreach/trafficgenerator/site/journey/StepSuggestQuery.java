package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.dispatch.Dispatcher;
import com.bloomreach.trafficgenerator.site.dispatch.SuggestApiResponse;
import com.bloomreach.trafficgenerator.site.build.apiparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepSuggestQuery extends StepBase {

    public StepSuggestQuery () {
    }

    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  String selectedTerm,  // "aq" param in suggest event
                                  PixelTemplates pixelTemplates,
                                  ApiTemplates apiTemplates,
                                  Dispatcher dispatcher,
                                  boolean testData) throws Exception {

        StepResultSuggestResponse thisStepResult;
        StepResultInvalidData inputInvalid;
        SuggestApiResponse suggestApiResponse;

        MessageLogger.logDebug (String.format ("Handle step suggest query: %s", selectedTerm));

        if (selectedTerm == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSuggestQuery, null suggest term");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        suggestApiResponse = handleStepInternal (prevStepResult, userRecord, logTime, selectedTerm,
                                                pixelTemplates, apiTemplates, dispatcher, testData);

        if (suggestApiResponse == null) {
            // Note: this step is only-do-a-query; does not change page url 
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSuggestQuery, null suggest api response");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        thisStepResult = new StepResultSuggestResponse ();
        super.setUrlHistory (prevStepResult, thisStepResult, null); // suggest-query itself does not change url
        thisStepResult.setSuggestApiResponse (suggestApiResponse);
        thisStepResult.setAqTerm (selectedTerm);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_SUG_QUERY);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_SUG_QUERY);

        return (thisStepResult);
    }

    private SuggestApiResponse handleStepInternal (StepResult prevStepResult,
                                                   UserRecord userRecord,
                                                   long logTime,
                                                   String selectedTerm,
                                                   PixelTemplates pixelTemplates,
                                                   ApiTemplates apiTemplates,
                                                   Dispatcher dispatcher,
                                                   boolean testData) throws Exception {

        SuggestApiResponse suggestApiResponse;

        // api call
        suggestApiResponse = collectSuggestApiResponse (userRecord, logTime, 
                                                        prevStepResult.getRefUrl (),
                                                        prevStepResult.getUrl (),
                                                        selectedTerm, apiTemplates, dispatcher);

        return suggestApiResponse;   // may be null
    }

    private SuggestApiResponse collectSuggestApiResponse (UserRecord userRecord, 
                                                          long startTime,
                                                          String refUrl, 
                                                          String url, 
                                                          String selectedTerm,
                                                          ApiTemplates apiTemplates,
                                                          Dispatcher dispatcher) throws Exception {
        ApiBRData apiData;
        SuggestApiResponse suggestApiResponse;

        // execute API call, get api response and pick a pid from that response
        apiData = buildSuggestApiFromTemplate (userRecord, startTime, refUrl, url, selectedTerm, apiTemplates);
        if (apiData == null) {
            MessageLogger.logError ("Cannot populate API brData: " + selectedTerm);
            return null;
        }

        suggestApiResponse = dispatcher.getSuggestApiResponse (apiData); 
        if (suggestApiResponse == null) {
            MessageLogger.logWarning ("Suggest api response is null for suggest term: " + selectedTerm);
            return null;
        }

        return suggestApiResponse;
    }


    private ApiBRData buildSuggestApiFromTemplate (UserRecord userRecord, 
                                                  long logTime,
                                                  String refUrl, 
                                                  String url, 
                                                  String selectedTerm,
                                                  ApiTemplates apiTemplates) throws Exception {
        ApiBRData suggestApiData;
        BuildSuggestApi buildSuggestApi;
        int buildStatus;

        // prepare a suggest ApiBRData from template
        suggestApiData = apiTemplates.loadApiTemplate (userRecord.getDeviceType());

        // update template
        buildSuggestApi = new BuildSuggestApi ();
        buildStatus = buildSuggestApi.build (suggestApiData, userRecord, logTime, refUrl, url, selectedTerm);

        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return apiLog object from this builder
            return (suggestApiData);
        }

        return (null);
    }
}

