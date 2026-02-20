package com.bloomreach.trafficgenerator.site.journeylogs;

import com.bloomreach.trafficgenerator.GeneratorConstants;

public class StepLog {

    private int stepId; // eg, browsePDP, convert, atc, ...
    private long stepStartTime;
    private String selectedQuery; // query - either search or catId
    private String stepResult; 
    private String userSegment; // may change from step-to-step 

    public StepLog (int stepId, long stepStartTime) {
        this.stepId = stepId;
        this.stepStartTime = stepStartTime;
        this.stepResult = "success";    // default
        this.selectedQuery = "-";
        this.userSegment = "None";  //default
    }

    public void setQuery (String query) {
        this.selectedQuery = query;
    }

    public void setUserSegment (String segment) {
        this.userSegment = segment;
    }

    public void setStepResult (String result) {
        this.stepResult = result;
    }

    protected LogRecord prepareLogRecord () {
        LogRecord logRecord;
        String stepText;

        logRecord = new LogRecord ();
        stepText = getTextForStepId ();
        logRecord.setStep (stepText);
        logRecord.setStepStart (stepStartTime);
        logRecord.setQuery (this.selectedQuery);
        logRecord.setUserSegment (this.userSegment);
        logRecord.setStepResult (this.stepResult);

        return logRecord;
    }

    // following constants must match the ones used in steps.tsv
    private String getTextForStepId () {
        String txt;
        switch (this.stepId) {
            case GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP: txt = "browse_pdp"; break;
            case GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST:  txt = "view_list"; break; 
            case GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM:txt = "search_term"; break;
            case GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT: txt = "search_category"; break;
            case GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY:  txt = "suggest_query"; break;
            case GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_NONE: txt = "suggest_query_no_selection"; break;
            case GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_TERM: txt = "suggest_query_select_term"; break;
            case GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_CAT:  txt = "suggest_query_select_category"; break;
            case GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_PROD:  txt = "suggest_query_select_product"; break;
            case GeneratorConstants.TRAFFIC_STEPID_ATC: txt = "add_to_cart"; break;
            case GeneratorConstants.TRAFFIC_STEPID_CONVERT : txt = "convert"; break;
            case GeneratorConstants.TRAFFIC_STEPID_START_URL: txt = "start_journey"; break;
            case GeneratorConstants.TRAFFIC_STEPID_SELECT_PID_FROM_LIST: txt = "select_pid_from_list"; break;
            case GeneratorConstants.TRAFFIC_STEPID_INVALID_DATA: txt = "invalid data"; break;
            case GeneratorConstants.TRAFFIC_STEPID_EXIT: txt = "exit"; break;
            case GeneratorConstants.TRAFFIC_STEPID_EXCEPTION_RESTART: txt = "exception_restart"; break;

            default: txt = "?";
        }
        return txt;
    }
}
