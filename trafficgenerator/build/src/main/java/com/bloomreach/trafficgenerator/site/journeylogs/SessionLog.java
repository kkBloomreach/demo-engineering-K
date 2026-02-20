package com.bloomreach.trafficgenerator.site.journeylogs;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.GeneratorConstants;

public class SessionLog {

    private int sessionNum;
    private int sessionType;
    private ArrayList<StepLog> stepLogs; 
    private SaveLock saveLock;

    // newly created steplog not added to array list immediately
    // because it gets populated in different stages. It is added
    // to arraylist at subsequent addStepLog method OR at session.close()
    private StepLog nextStepLog = null;

    public SessionLog (int sessionNum, int sessionType, SaveLock saveLock) {
        this.sessionNum = sessionNum;
        this.sessionType = sessionType;
        this.stepLogs = new ArrayList <StepLog> ();
        this.saveLock = saveLock;
    }

    public StepLog addStepLog (int stepId, long stepStartTime) {
        if (this.nextStepLog != null) {
            synchronized (this.saveLock) {
                this.stepLogs.add (nextStepLog);
            }
        }
        this.nextStepLog = new StepLog (stepId, stepStartTime);
        return this.nextStepLog;
    }

    // add the last remaining stepLog (if any) to arraylist
    // this method must be called from external code when session is closed
    public void close () {
        if (this.nextStepLog != null) {
            synchronized (this.saveLock) {
                this.stepLogs.add (nextStepLog);
            }
        }
    }

    // update logRecord with all steps in this session so far
    protected ArrayList <LogRecord> prepareLogRecords () {
        ArrayList <LogRecord> sessionLogRecords;

        sessionLogRecords = new ArrayList <LogRecord> ();

        for (StepLog stepLog : this.stepLogs) {
            LogRecord stepLogRecord;

            stepLogRecord = stepLog.prepareLogRecord ();
            stepLogRecord.setSessionNum (this.sessionNum);
            stepLogRecord.setSessionType (getTextForSessionType()); 
            sessionLogRecords.add (stepLogRecord);
        }
        return sessionLogRecords;
    }

    protected void performCleanup () throws Exception {
        // remove all stepLogs in arraylist. 
        // Note - there may be a 'nextStepLog' still to be added to arraylist
        this.stepLogs.clear ();
    }

    // following constants must match the ones used in steps.tsv
    private String getTextForSessionType () {
        String txt;
        switch (this.sessionType) {
            case GeneratorConstants.UNDEFINED_SESSION_TYPE: txt = "-"; break;
            case GeneratorConstants.TERM_SEARCH_SESSION: txt = "s"; break;
            case GeneratorConstants.CATEGORY_SEARCH_SESSION: txt = "c"; break;
            case GeneratorConstants.TERM_SEARCH_WITH_TERM_REFINEMENT_SESSION: txt = "s2s"; break;
            case GeneratorConstants.TERM_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION: txt = "s2c"; break;
            case GeneratorConstants.CATEGORY_SEARCH_WITH_TERM_REFINEMENT_SESSION: txt = "c2s"; break;
            case GeneratorConstants.CATEGORY_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION: txt = "c2c"; break;
            case GeneratorConstants.ZERO_RESULT_QUERY_SESSION: txt = "z"; break;
            case GeneratorConstants.ZERO_RESULT_QUERY_WITH_TERM_REFINEMENT_SESSION: txt = "z2s"; break;
            case GeneratorConstants.ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION: txt = "z2c"; break;
            case GeneratorConstants.SUGGEST_SESSION_SELECT_NONE: txt = "g-"; break;
            case GeneratorConstants.SUGGEST_SESSION_SELECT_TERM: txt = "gt"; break;
            case GeneratorConstants.SUGGEST_SESSION_SELECT_CATEGORY: txt = "gc"; break;
            case GeneratorConstants.SUGGEST_SESSION_SELECT_PRODUCT: txt = "gp"; break;
            case GeneratorConstants.TERM_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION: txt = "ps2s"; break;  // 'partial' search
            case GeneratorConstants.TERM_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION: txt = "ps2c"; break;
            case GeneratorConstants.CATEGORY_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION: txt = "pc2s"; break; 
            case GeneratorConstants.CATEGORY_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION: txt = "pc2c"; break;

            default: txt = "?"; break;
        }
        return txt;
    }
}
