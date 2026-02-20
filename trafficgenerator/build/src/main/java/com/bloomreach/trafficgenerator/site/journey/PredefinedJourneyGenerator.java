// generate pixelLogs for different sessions
// eg, search, category, search-to-search refined, ...
package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.journeydata.*;
import com.bloomreach.trafficgenerator.site.journeydata.customjourney.CustomJourneyData;
import com.bloomreach.trafficgenerator.site.journeydata.customjourney.LPCCustomJourneyData;
import com.bloomreach.trafficgenerator.site.journeydata.queryexecutor.CategoryInfo;
import com.bloomreach.trafficgenerator.site.journeylogs.*;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.EnvironmentConfig;

public class PredefinedJourneyGenerator {

    // Traffic steps
    TrafficSteps trafficSteps;

    // steps Handler
    StepsHandler stepsHandler;

    // search terms
    SearchTerms searchTerms;

    // ZeroResult search terms
    ZeroResultSearchTerms zeroResultSearchTerms;

    // category 
    SearchCategories searchCategories;

    // 'weighted' session list containing sessionType values according to their 'weights'
    ArrayList <Integer> weightedSessionList;

    // "Weights" for each type of session. 
    //  -- There must be a 'weight' associated with each session type
    private final static int TERM_SEARCH_SESSION_WEIGHT = 20;
    private final static int CATEGORY_SEARCH_SESSION_WEIGHT = 15; 
    private final static int TERM_SEARCH_WITH_TERM_REFINEMENT_SESSION_WEIGHT  = 5; 
    private final static int TERM_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT  = 5;
    private final static int CATEGORY_SEARCH_WITH_TERM_REFINEMENT_SESSION_WEIGHT  = 5;
    private final static int CATEGORY_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT  = 5;
    private final static int ZERO_RESULT_QUERY_SESSION_WEIGHT = 1;
    private final static int ZERO_RESULT_QUERY_WITH_TERM_REFINEMENT_SESSION_WEIGHT = 0; // exclude this session type
    private final static int ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT = 0; // exclude this session type
    private final static int SUGGEST_SESSION_SELECT_NONE_WEIGHT = 5;
    private final static int SUGGEST_SESSION_SELECT_TERM_WEIGHT = 10;
    private final static int SUGGEST_SESSION_SELECT_CATEGORY_WEIGHT = 10;
    private final static int SUGGEST_SESSION_SELECT_PRODUCT_WEIGHT = 10;
    private final static int TERM_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION_WEIGHT  = 10;  // 'partial' search
    private final static int TERM_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT  = 5;
    private final static int CATEGORY_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION_WEIGHT  = 10;
    private final static int CATEGORY_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT  = 5;

    public PredefinedJourneyGenerator () {
    }

    public void setTrafficSteps (TrafficSteps trafficSteps) {
        this.trafficSteps = trafficSteps;
    }

    public void setStepsHandler (StepsHandler stepsHandler) {
        this.stepsHandler = stepsHandler;
    }

    public void setSearchTerms (SearchTerms searchTerms) {
        this.searchTerms = searchTerms;
    }

    public void setZeroResultSearchTerms (ZeroResultSearchTerms zeroResultSearchTerms) {
        this.zeroResultSearchTerms= zeroResultSearchTerms;
    }

    public void setSearchCategories (SearchCategories searchCategories) {
        this.searchCategories = searchCategories;
    }

    public boolean init () {
        // 'weighted' session list containing sessionType values according to their 'weights'
        this.weightedSessionList = prepareWeightedSessionList ();
        return true;
    }

    public void startJourney (UserRecord userRecord,
                              long journeyStartTime,
                              JourneyLog journeyLog) throws Exception {

        int numSessions;
        long sessionStartTime;
        StepResult prevStepResult;
        StepResult stepResult;
        Cart userCart;
        String envType;
        SessionLog newSessionLog;
        StepLog stepLog;

        // get it once
        envType = EnvironmentConfig.getEnvType ();

        userCart = new Cart ();
        userCart.setUserId (userRecord.getUserId ());

        // begin
        sessionStartTime = journeyStartTime; // updated after each session
        prevStepResult = new StepResultVoid ();
        prevStepResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/home/blank/...
        prevStepResult.setUrl (null); // set below in OpenUrl
        prevStepResult.setEndTime (journeyStartTime);

        // open using some URL (eg, home page). This "Session" is created just to write stepLog
        newSessionLog = journeyLog.addSessionLog (GeneratorConstants.UNDEFINED_SESSION_TYPE);
        stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_START_URL, sessionStartTime);
        stepLog.setUserSegment (userRecord.getSegment());

        sessionStartTime = sessionStartTime + 10; // add time so that later time-based-sort for journeyAnalyzer is deterministic
        stepResult = this.stepsHandler.handleStepOpenUrl (prevStepResult,
                                                         userRecord, 
                                                         sessionStartTime,
                                                         stepLog);
        // if needed, handle widgets on the page. Currently homepage and productpage widgets are supported
        long logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, stepResult.getEndTime(), newSessionLog); 
        stepResult.setEndTime(logTime);
        //if OpenUrl stepResult itself is invalid, we force it to a default case: ref = "", url = "home"
        prevStepResult = stepResult;
        newSessionLog.close ();

        // single visitor may perform multiple sessions in single visit
        numSessions = (int) (Math.random () * GeneratorConstants.MAX_SESSIONS_PER_VISITOR) + 1;
        // then execute predefined sessions
        for (int sessionNum = 0; sessionNum < numSessions; sessionNum++) { 
            int sessionType;
            int randomInt;

            MessageLogger.logDebug (String.format ("Start visitor session: userId = %s, sessionNum = %d", userRecord.getUserId(),  sessionNum));

            // session type - pick one at random from the weightedSessionList
            randomInt = (int) (Math.random () * this.weightedSessionList.size()); // values include 0 to list.size()
            sessionType = this.weightedSessionList.get (randomInt);

            // randomly, change user's segment (for RTS). This should associate views/purchases/... during this session
            // to the corresponding segment
            UserManager.updateUserSegment (userRecord);

            MessageLogger.logDebug (String.format ("BEFORE visitorId = %s, sessionNum = %d, sessionType = %s, refUrl = %s, url = %s\n",
                                                   userRecord.getVisitorId(), sessionNum, sessionType, prevStepResult.getRefUrl (), prevStepResult.getUrl ()));

            // generate traffic one session. Returned stepResult is from the last step performed in each session
            // add sessionLog to journeyLog
            newSessionLog = journeyLog.addSessionLog (sessionType);
            try {
                stepResult = startOneSession (prevStepResult, userRecord, sessionType, sessionStartTime, newSessionLog, userCart);
                MessageLogger.logDebug (String.format ("AFTER visitorId = %s, sessionNum = %d, sessionType = %s, refUrl = %s, url = %s\n",
                                                        userRecord.getVisitorId (), sessionNum, sessionType, stepResult.getRefUrl (), stepResult.getUrl ()));
            } catch (CustomJourneyException cje) {
                String msg;
                CustomJourneyData journeyData;

                journeyData = cje.getCustomJourneyData();
                msg = String.format ("Handle custom journey, visitorId = %s, sessionType = %s, journeyType = %s, journeyTarget = %s\n",
                                                        userRecord.getVisitorId (), sessionType,
                                                        journeyData.getCustomJourneyType(), journeyData.getCustomJourneyTarget());
                MessageLogger.logDebug(msg);
                
                // execute custom journey. Important -- must not throw any exception
                stepResult = handleCustomJourney (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart, journeyData);

                // after handling customJourney, re-throw customJourneyException so that there are
                // no further 'events' associated with this journey. We need to do this because Insights collects pair-wise
                // events in the 'opportunity->...->coview' report. Our intent is to not have too-many such
                // pair-wise records in case of custom journey (ie, keep that report as clean and not-confusing as possible)
                throw new CustomJourneyException(cje.getCustomJourneyData());
            } catch (Exception e) {
                StepResultInvalidData exceptionResult;

                e.printStackTrace ();
                MessageLogger.logError (String.format ("Exception in session, visitorId = %s, sessionNum = %d, sessionType = %s\n",
                                                        userRecord.getVisitorId (), sessionNum, sessionType));
                exceptionResult = new StepResultInvalidData ();
                exceptionResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/home/blank/...
                exceptionResult.setUrl (prevStepResult.getUrl ());
                exceptionResult.setMessage (String.format ("Exception in session, %s", e.getMessage()));
                exceptionResult.setEndTime (prevStepResult.getEndTime () + 1000);

                stepResult = exceptionResult;
            }

            // if above session was "zero_result" etc, stepResult will be 'invalid'.
            // In such cases, reset refUrl for subsequent sessions
            if (stepResult instanceof StepResultInvalidData) {
                StepResult fixedStepResult;

                MessageLogger.logWarning ((String) stepResult.getData ()); // Note - this COULD cause duplicate WARN message in log file
 
                // start stepResult as if new journey, starting from 'prev' url,refUrl
                stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_EXCEPTION_RESTART, stepResult.getEndTime()); 
                stepLog.setUserSegment (userRecord.getSegment());
                stepLog.setStepResult ((String) stepResult.getData ());   // exception message if any
                
                fixedStepResult = new StepResultVoid ();
                fixedStepResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/home/blank/...
                fixedStepResult.setUrl (stepResult.getUrl ());
                fixedStepResult.setEndTime (stepResult.getEndTime () + 1000);
                
                stepResult = fixedStepResult;
            } 

            // add some actual delay between subsequent sessions (skip in dev env)
            if (envType.equals (EnvironmentConfig.ENV_TYPE_QA) || envType.equals (EnvironmentConfig.ENV_TYPE_RELEASE)) {
                try {
                    Thread.currentThread().sleep (GeneratorConstants.MEAN_TIME_BETWEEN_PREDEFINED_SESSIONS);
                } catch (InterruptedException ie) {
                }
            }

            // sessionStartTime = sessionStartTime + GeneratorConstants.MEAN_TIME_BETWEEN_SESSIONS;
            sessionStartTime = stepResult.getEndTime () + GeneratorConstants.MEAN_TIME_BETWEEN_PREDEFINED_SESSIONS;

            MessageLogger.logDebug (String.format ("Predefined journey, end session: userId = %s, sessionNum = %d", userRecord.getUserId(),  sessionNum));
            newSessionLog.close ();

            prevStepResult = stepResult;    // for the next step
        }
    }

    private StepResult startOneSession (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  int sessionType, 
                                  long sessionStartTime,
                                  SessionLog newSessionLog,
                                  Cart userCart) throws Exception {

        StepResult stepResult;

        switch (sessionType) {
            case GeneratorConstants.TERM_SEARCH_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate search query session, userId = " + userRecord.getUserId()); 
                stepResult = startSearchSession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.CATEGORY_SEARCH_SESSION: 
                MessageLogger.logDebug ("Predefined journey, generate category query session, userId = " + userRecord.getUserId()); 
                stepResult = startCategorySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.ZERO_RESULT_QUERY_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate zeroQuery session, userId = " + userRecord.getUserId()); 
                stepResult = startZeroResultQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog); 
                break;

            case GeneratorConstants.TERM_SEARCH_WITH_TERM_REFINEMENT_SESSION: 
                MessageLogger.logDebug ("Predefined journey, generate searchquery + searchquery refined session, userId = " + userRecord.getUserId()); 
                stepResult = startSearch2SearchRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.TERM_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate searchquery + category refined session, userId = " + userRecord.getUserId()); 
                stepResult = startSearch2CategoryRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.CATEGORY_SEARCH_WITH_TERM_REFINEMENT_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate category + searchquery refined session, userId = " + userRecord.getUserId()); 
                stepResult = startCategory2SearchRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.CATEGORY_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate category + category refined session, userId = " + userRecord.getUserId()); 
                stepResult = startCategory2CategoryRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            // this case will not occur because its weight = 0
            case GeneratorConstants.ZERO_RESULT_QUERY_WITH_TERM_REFINEMENT_SESSION:
                MessageLogger.logDebug ("DELETED Predefined journey, generate zeroQuery + search refined session, userId = " + userRecord.getUserId()); 
                stepResult = startZeroResult2SearchRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                stepResult = prevStepResult;
                break;

            // this case will not occur because its weight = 0
            case GeneratorConstants.ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION:
                MessageLogger.logDebug ("DELETED Predefined journey, generate zeroQuery + category refined session, userId = " + userRecord.getUserId()); 
                stepResult = startZeroResult2CategoryRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                stepResult = prevStepResult;
                break;

            case GeneratorConstants.SUGGEST_SESSION_SELECT_NONE:
                MessageLogger.logDebug ("Predefined journey, generate suggest session, userId = " + userRecord.getUserId()); 
                stepResult = startSuggestSessionSelectNone (prevStepResult, userRecord, sessionStartTime, newSessionLog); 
                break;

            case GeneratorConstants.SUGGEST_SESSION_SELECT_TERM:
                MessageLogger.logDebug ("Predefined journey, generate suggest session, userId = " + userRecord.getUserId()); 
                stepResult = startSuggestSessionSelectTerm (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.SUGGEST_SESSION_SELECT_CATEGORY:
                MessageLogger.logDebug ("Predefined journey, generate suggest session, userId = " + userRecord.getUserId()); 
                stepResult = startSuggestSessionSelectCategory (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.SUGGEST_SESSION_SELECT_PRODUCT:
                MessageLogger.logDebug ("Predefined journey, generate suggest session, userId = " + userRecord.getUserId()); 
                stepResult = startSuggestSessionSelectProduct (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.TERM_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate partial s2s session, userId = " + userRecord.getUserId()); 
                stepResult = startPartialSearch2SearchRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.TERM_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate partial s2c session, userId = " + userRecord.getUserId()); 
                stepResult = startPartialSearch2CategoryRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.CATEGORY_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate partial c2s session, userId = " + userRecord.getUserId()); 
                stepResult = startPartialCategory2SearchRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            case GeneratorConstants.CATEGORY_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                MessageLogger.logDebug ("Predefined journey, generate partial c2c session, userId = " + userRecord.getUserId()); 
                stepResult = startPartialCategory2CategoryRefinedQuerySession (prevStepResult, userRecord, sessionStartTime, newSessionLog, userCart); 
                break;

            default: 
                StepResultInvalidData defaultResult;

                MessageLogger.logError ("ERROR Unknown session type: " + sessionType);
                defaultResult = new StepResultInvalidData ();  // keep java happy
                defaultResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/home/blank/...
                defaultResult.setUrl (prevStepResult.getUrl ());
                defaultResult.setMessage (String.format ("ERROR Unknown session type: %s" + sessionType));
                defaultResult.setEndTime (prevStepResult.getEndTime () + 1000);

                stepResult = defaultResult;
        }

        return stepResult;
    }

    private StepResult startSearchSession (StepResult prevStepResult,
                                           UserRecord userRecord, 
                                           long sessionStartTime,
                                           SessionLog sessionLog,
                                           Cart userCart) throws Exception {
        StepResult stepResult;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;
        // select a term and search using that term
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            StepResultProductDetails stepResultProductDetails;

            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog, 
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        } 

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            //return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startCategorySession (StepResult prevStepResult,
                                             UserRecord userRecord, 
                                             long sessionStartTime,
                                             SessionLog sessionLog,
                                             Cart userCart) throws Exception {

        StepResult stepResult;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a catId and search usingg that catId
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSearchCategory (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
         return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            //return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startZeroResultQuerySession (StepResult prevStepResult,
                                                    UserRecord userRecord, 
                                                    long sessionStartTime,
                                                    SessionLog sessionLog) throws Exception {
        StepResult stepResult;
        String selectedZeroResultSearchTerm;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // Select a query that returns zero results (from list of predefined queries)
        selectedZeroResultSearchTerm = zeroResultSearchTerms.selectZeroResultSearchTermAtRandom (prevStepResult.getUrl());

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, stepLog,
                                                             selectedZeroResultSearchTerm);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // in order to cancel this zero-result search session, do another 'phantom' search.
        // This is needed so that subsequent actions are associated with the 'phantom' search. 
        // Also, ignore stepResult from this 'phantom' search since it has no implication 
        // related to the zero-search result
        this.stepsHandler.handleStepTerminateZeroSearchSession (stepResult, userRecord, logTime);

        // since zero-search are known to be empty results, no subsequent steps needed
        return stepResult;
    }

    private StepResult startSearch2SearchRefinedQuerySession (StepResult prevStepResult,
                                                              UserRecord userRecord, 
                                                              long sessionStartTime,
                                                              SessionLog sessionLog,
                                                              Cart userCart) throws Exception {
        StepResult stepResult;
        SearchTermWithRefinements selectedSearchTerm;
        String refinedSearchTerm;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a term and search using that term
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        selectedSearchTerm = this.searchTerms.selectSearchTermAtRandom (prevStepResult.getUrl());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, 
                                                             stepLog, selectedSearchTerm.getPrimary ());
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // add-to-cart and convert product1 (without this, "coview" does not apply
        // In this case, don't check 'proceedToATC = true/false'. ATC 100% for first product
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert - Convert 100% in this case
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // after this step, handle another 'refined' (aka coviewed) search term.
        // select a term and search using that term
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        refinedSearchTerm = this.searchTerms.selectRefinedSearchTermAtRandom (selectedSearchTerm);
        stepResult = this.stepsHandler.handleStepSearchTerm (stepResult, userRecord, logTime, stepLog, refinedSearchTerm);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // add product to cart - a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert - a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    // in partial search, first search does not include atc/conversion. This flow was
    // requested/suggested by analytics engr team so that some insights reports are populated
    private StepResult startPartialSearch2SearchRefinedQuerySession (StepResult prevStepResult,
                                                                     UserRecord userRecord, 
                                                                     long sessionStartTime,
                                                                     SessionLog sessionLog,
                                                                     Cart userCart) throws Exception {
        StepResult stepResult;
        SearchTermWithRefinements selectedSearchTerm;
        String refinedSearchTerm;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a term and search using that term
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        selectedSearchTerm = this.searchTerms.selectSearchTermAtRandom (prevStepResult.getUrl());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, 
                                                             stepLog, selectedSearchTerm.getPrimary ());
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // partial-search -- no atc/conversion after FIRST search; just go to next search

        // after this step, handle another 'refined' (aka coviewed) search term.
        // select a term and search using that term
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        refinedSearchTerm = this.searchTerms.selectRefinedSearchTermAtRandom (selectedSearchTerm);
        stepResult = this.stepsHandler.handleStepSearchTerm (stepResult, userRecord, logTime, stepLog, refinedSearchTerm);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startSearch2CategoryRefinedQuerySession (StepResult prevStepResult,
                                                                UserRecord userRecord, 
                                                                long sessionStartTime,
                                                                SessionLog sessionLog,
                                                                Cart userCart) throws Exception {
        StepResult stepResult;
        CategoryInfo selectedCatInfo;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a term and search using that term
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add-to-cart and convert product1 (without this, coview does not apply)
        // In this case, don't check 'proceedToATC = true/false'. ATC 100% for first product
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert - Convert 100% in this case
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // after this step, handle refined category search
        // select a category and search using that category. 
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedCatInfo = this.searchCategories.selectCategoryAtRandom (stepResult.getUrl());
            stepResult = this.stepsHandler.handleStepSearchCategory (stepResult, userRecord, logTime, stepLog, selectedCatInfo);
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        } 

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    // in partial search, first search does not include atc/conversion. This flow was
    // requested/suggested by analytics engr team so that some insights reports are populated
    private StepResult startPartialSearch2CategoryRefinedQuerySession (StepResult prevStepResult,
                                                                       UserRecord userRecord, 
                                                                       long sessionStartTime,
                                                                       SessionLog sessionLog,
                                                                       Cart userCart) throws Exception {
        StepResult stepResult;
        CategoryInfo selectedCatInfo;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a term and search using that term
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // partial-search -- no atc/conversion after FIRST search; just go to next search

        // after this step, handle refined category search
        // select a category and search using that category.
        // Since prior search is for a 'term', selected category below can be any (no need to re-check refurl = url)
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedCatInfo = this.searchCategories.selectCategoryAtRandom ();
            stepResult = this.stepsHandler.handleStepSearchCategory (stepResult, userRecord, logTime, stepLog, selectedCatInfo);
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        } 

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startCategory2SearchRefinedQuerySession (StepResult prevStepResult,
                                                          UserRecord userRecord, 
                                                          long sessionStartTime,
                                                          SessionLog sessionLog,
                                                          Cart userCart) throws Exception {

        StepResult stepResult;
        SearchTermWithRefinements selectedRefinedSearchTerm;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a catId and search usingg that catId
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSearchCategory (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // add product to cart
        // In this case, don't check 'proceedToATC = true/false'. ATC 100% for first product
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert - Convert 100% in this case
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select a search term. Since first search is 'category', the 'refined'
        // search term can be any (no need to re-check refurl != url)
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedRefinedSearchTerm = this.searchTerms.selectSearchTermAtRandom ();
            stepResult = this.stepsHandler.handleStepSearchTerm (stepResult, userRecord, logTime, stepLog, selectedRefinedSearchTerm.getPrimary());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startPartialCategory2SearchRefinedQuerySession (StepResult prevStepResult,
                                                                       UserRecord userRecord, 
                                                                       long sessionStartTime,
                                                                       SessionLog sessionLog,
                                                                       Cart userCart) throws Exception {

        StepResult stepResult;
        SearchTermWithRefinements selectedRefinedSearchTerm;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a catId and search usingg that catId
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSearchCategory (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // partial-search -- no atc/conversion after FIRST search; just go to next search

        // select a search term. Since first search is for 'category', the 'refined'
        // term can be any (no need to re-check refurl != url)
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedRefinedSearchTerm = this.searchTerms.selectSearchTermAtRandom ();
            stepResult = this.stepsHandler.handleStepSearchTerm (stepResult, userRecord, logTime, stepLog, selectedRefinedSearchTerm.getPrimary());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startCategory2CategoryRefinedQuerySession (StepResult prevStepResult,
                                                          UserRecord userRecord, 
                                                          long sessionStartTime,
                                                          SessionLog sessionLog,
                                                          Cart userCart) throws Exception {

        StepResult stepResult;
        CategoryInfo selectedCatInfo;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a catId and search usingg that catId
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedCatInfo = this.searchCategories.selectCategoryAtRandom (prevStepResult.getUrl());
            stepResult = this.stepsHandler.handleStepSearchCategory (prevStepResult, userRecord, logTime, stepLog, selectedCatInfo);
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add product to cart
        // In this case, don't check 'proceedToATC = true/false'. ATC 100% for first product
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert - Convert 100% in this case
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // after this step, handle category search
        // select a category and search using that category. Param is selected-catInfo so that the same
        // is not picked as the 'refined' catInfo
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedCatInfo = this.searchCategories.selectRefinedSearchCategoryAtRandom (selectedCatInfo);
            stepResult = this.stepsHandler.handleStepSearchCategory (stepResult, userRecord, logTime, stepLog, selectedCatInfo);
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startPartialCategory2CategoryRefinedQuerySession (StepResult prevStepResult,
                                                                         UserRecord userRecord, 
                                                                         long sessionStartTime,
                                                                         SessionLog sessionLog,
                                                                         Cart userCart) throws Exception {

        StepResult stepResult;
        CategoryInfo selectedCatInfo;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // select a catId and search usingg that catId
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedCatInfo = this.searchCategories.selectCategoryAtRandom (prevStepResult.getUrl());
            stepResult = this.stepsHandler.handleStepSearchCategory (prevStepResult, userRecord, logTime, stepLog, selectedCatInfo);
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // partial-search -- no atc/conversion after FIRST search; just go to next search

        // after this step, handle category search
        // select a category and search using that category. Param is already-selected-catInfo so that the same
        // is not picked as the 'refined' catInfo
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedCatInfo = this.searchCategories.selectRefinedSearchCategoryAtRandom (selectedCatInfo);
            stepResult = this.stepsHandler.handleStepSearchCategory (stepResult, userRecord, logTime, stepLog, selectedCatInfo);
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    // zero followed by refined-category deleted because it generates strange looking Insights reports
    private StepResult startZeroResult2SearchRefinedQuerySession (StepResult prevStepResult,
                                                            UserRecord userRecord, 
                                                            long sessionStartTime,
                                                            SessionLog sessionLog,
                                                            Cart userCart) throws Exception {
        StepResult stepResult;
        String selectedZeroResultSearchTerm;
        SearchTermWithRefinements selectedSearchTerm;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // Select a query that returns zero results (from list of predefined queries)
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        selectedZeroResultSearchTerm = zeroResultSearchTerms.selectZeroResultSearchTermAtRandom (prevStepResult.getUrl());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, stepLog,
                                                             selectedZeroResultSearchTerm);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // after this step, handle another search term. Ideally this should be a 'refined' query
        // however, for 'zero' result queries, we don't have 'refined'. Therefore, select any 
        // 'regular' (non-zero-result) term
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedSearchTerm = this.searchTerms.selectSearchTermAtRandom ();
            stepResult = this.stepsHandler.handleStepSearchTerm (stepResult, userRecord, logTime, stepLog, selectedSearchTerm.getPrimary());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
        }

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    // zero followed by refined-category deleted because it generates strange looking Insights reports
    private StepResult startZeroResult2CategoryRefinedQuerySession (StepResult prevStepResult,
                                                              UserRecord userRecord, 
                                                              long sessionStartTime,
                                                              SessionLog sessionLog,
                                                              Cart userCart) throws Exception {
        StepResult stepResult;
        String selectedZeroResultSearchTerm;
        CategoryInfo selectedCatInfo;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        // Select a query that returns zero results (from list of predefined queries)
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        selectedZeroResultSearchTerm = zeroResultSearchTerms.selectZeroResultSearchTermAtRandom (prevStepResult.getUrl());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, stepLog,
                                                             selectedZeroResultSearchTerm);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // after this step, handle category search
        // select a category and search using that category 
        // Since prior search is for zero-result, selectedCatInfo can be any (no need to re-check refurl != url)
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            selectedCatInfo = this.searchCategories.selectCategoryAtRandom ();
            stepResult = this.stepsHandler.handleStepSearchCategory (stepResult, userRecord, logTime, stepLog, selectedCatInfo);
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startSuggestSessionSelectNone (StepResult prevStepResult,
                                                UserRecord userRecord, 
                                                long sessionStartTime,
                                                SessionLog sessionLog) throws Exception {
        StepResult stepResult;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_NONE, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSuggestQuery (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // since no selection, nothing further to do
        return stepResult;
    }

    private StepResult startSuggestSessionSelectTerm (StepResult prevStepResult,
                                                UserRecord userRecord, 
                                                long sessionStartTime,
                                                SessionLog sessionLog,
                                                Cart userCart) throws Exception {
        StepResult stepResult;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSuggestQuery (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // goes thru suggest-term -> search api call. Return stepResult has those searchApiResponse
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSuggestSelectTerm (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startSuggestSessionSelectCategory (StepResult prevStepResult,
                                                    UserRecord userRecord, 
                                                    long sessionStartTime,
                                                    SessionLog sessionLog,
                                                    Cart userCart) throws Exception {
        StepResult stepResult;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSuggestQuery (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // goes thru suggest-term -> search category api call. Return stepResult has those categorySearchApiResponse
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_CAT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSuggestSelectCategory (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private StepResult startSuggestSessionSelectProduct  (StepResult prevStepResult,
                                                    UserRecord userRecord, 
                                                    long sessionStartTime,
                                                    SessionLog sessionLog,
                                                    Cart userCart) throws Exception {
        StepResult stepResult;
        StepResultProductDetails stepResultProductDetails;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSuggestQuery (prevStepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // goes thru suggest-term -> search -> select product Return stepResult has selectProduct details
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_PROD, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSuggestSelectProduct (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog,
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        }

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            // return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    // custom journey handler
    private StepResult handleCustomJourney (StepResult prevStepResult, 
                                            UserRecord userRecord, 
                                            long sessionStartTime,
                                            SessionLog sessionLog,
                                            Cart userCart,    
                                            CustomJourneyData journeyData) {
        StepResult customJourneyStepResult;

        switch (journeyData.getCustomJourneyType()) {
            case GeneratorConstants.CUSTOM_JOURNEY_TYPE_LPC:    
                // LPC: LowPerformanceCategory 
                // Generate bounced-category-session, followed by search term using selected-search-terms
                // Note: actual category session has started and bounced. 
                // Next, 90% pick one of the selected-search-terms and use that to execute regular search-session
                //       10% pick one of the regular-search-terms
                LPCCustomJourneyData lpcJourneyData;
                ArrayList<String> lpcSelectedSearchTerms;
                int rand;
                String searchTerm;

                rand = (int) (Math.random () * 100);
                if (rand < 90) {
                    // use one of the 'select-search-terms' for this journey
                    lpcJourneyData = (LPCCustomJourneyData) journeyData;
                    lpcSelectedSearchTerms = lpcJourneyData.getSelectSearchTerms();
                    rand = (int)(Math.random () * lpcSelectedSearchTerms.size());
                    searchTerm = lpcSelectedSearchTerms.get (rand);
                } else {
                    // use regular search-term
                    SearchTermWithRefinements selectedSearchTerm;

                    selectedSearchTerm = this.searchTerms.selectSearchTermAtRandom (prevStepResult.getUrl());
                    searchTerm = selectedSearchTerm.getPrimary();
                }

                // make sure no exception is raised in custom journey. This method itself is invoked from
                // a exception handler
                try {
                    MessageLogger.logDebug(String.format ("Handle LPC custom journey, catId = %s, searchTerm = %s\n",
                                                            journeyData.getCustomJourneyTarget(), searchTerm));
                    customJourneyStepResult = this.handleCustomSearchSession(prevStepResult, userRecord, sessionStartTime, sessionLog, userCart, searchTerm);
                } catch (Exception e) {
                    StepResultInvalidData exceptionResult;
                    String msg;

                    msg = String.format ("Exception during custom journey: %s, msg = %s", journeyData.getCustomJourneyType(), e.getMessage());
                    MessageLogger.logError (msg);

                    exceptionResult = new StepResultInvalidData ();
                    exceptionResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/home/blank/...
                    exceptionResult.setUrl (prevStepResult.getUrl ());
                    exceptionResult.setMessage (msg);
                    exceptionResult.setEndTime (prevStepResult.getEndTime () + 1000);
                    customJourneyStepResult = exceptionResult;
                }
                break;

            default:
                StepResultInvalidData exceptionResult;
                String msg;

                msg = String.format ("Unknown custom journey type: %s", journeyData.getCustomJourneyType());
                MessageLogger.logError(msg);

                exceptionResult = new StepResultInvalidData ();
                exceptionResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/home/blank/...
                exceptionResult.setUrl (prevStepResult.getUrl ());
                exceptionResult.setMessage (msg);
                exceptionResult.setEndTime (prevStepResult.getEndTime () + 1000);
                customJourneyStepResult = exceptionResult;
        }
        return customJourneyStepResult;
    }

    // This customSearch session is same as regular search session except that the
    // searchTerm is already selected as part of custom-journey
    private StepResult handleCustomSearchSession (StepResult prevStepResult,
                                                  UserRecord userRecord, 
                                                  long sessionStartTime,
                                                  SessionLog sessionLog,
                                                  Cart userCart,
                                                  String searchTerm) throws Exception {
        StepResult stepResult;
        long logTime;
        StepLog stepLog;

        logTime = sessionStartTime;
        // select a term and search using that term
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepSearchTerm (prevStepResult, userRecord, logTime, stepLog, searchTerm);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();
        logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 

        // view search results
        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // select pid from search results
        stepResult = this.stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // browse selected product
        {
            StepResultProductDetails stepResultProductDetails;

            stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
            stepResultProductDetails = (StepResultProductDetails) stepResult;
            stepResult = this.stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog, 
                                                                (ProductDetails) stepResultProductDetails.getData ());
            if (stepResult instanceof StepResultInvalidData) {
                MessageLogger.logWarning ((String) stepResult.getData ());
                stepLog.setStepResult ((String) stepResult.getData ());
                return stepResult;
            }
            logTime = stepResult.getEndTime ();
            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, sessionLog); 
        } 

        // add product to cart for a % of sessions
        if (this.proceedToATC() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_ATC, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            return stepResult;
        }
        logTime = stepResult.getEndTime ();

        // convert for a % of ATCs
        if (this.proceedToConversion() == false)
            return stepResult;

        stepLog = sessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_CONVERT, logTime);
        stepLog.setUserSegment (userRecord.getSegment());
        stepResult = this.stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
        if (stepResult instanceof StepResultInvalidData) {
            MessageLogger.logWarning ((String) stepResult.getData ());
            stepLog.setStepResult ((String) stepResult.getData ());
            //return stepResult;
        }
        logTime = stepResult.getEndTime ();

        return stepResult;
    }

    private ArrayList <Integer> prepareWeightedSessionList () {
        ArrayList <Integer> weightedSessionList;
        int weight = 0;

        weightedSessionList = new ArrayList <Integer> ();
        for (int sessionType = 0; sessionType < GeneratorConstants.MAX_SESSION_TYPES; sessionType++) {
            switch (sessionType) {
                case GeneratorConstants.TERM_SEARCH_SESSION:
                    weight = TERM_SEARCH_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.CATEGORY_SEARCH_SESSION: 
                    weight = CATEGORY_SEARCH_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.TERM_SEARCH_WITH_TERM_REFINEMENT_SESSION:
                    weight = TERM_SEARCH_WITH_TERM_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.TERM_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                    weight = TERM_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.CATEGORY_SEARCH_WITH_TERM_REFINEMENT_SESSION: 
                    weight = CATEGORY_SEARCH_WITH_TERM_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.CATEGORY_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                    weight = CATEGORY_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.ZERO_RESULT_QUERY_SESSION:
                    weight = ZERO_RESULT_QUERY_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.ZERO_RESULT_QUERY_WITH_TERM_REFINEMENT_SESSION:
                    weight = ZERO_RESULT_QUERY_WITH_TERM_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION: 
                    weight = ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.SUGGEST_SESSION_SELECT_NONE:
                    weight = SUGGEST_SESSION_SELECT_NONE_WEIGHT;
                    break;

                case GeneratorConstants.SUGGEST_SESSION_SELECT_TERM:
                    weight = SUGGEST_SESSION_SELECT_TERM_WEIGHT;
                    break;

                case GeneratorConstants.SUGGEST_SESSION_SELECT_CATEGORY:
                    weight = SUGGEST_SESSION_SELECT_CATEGORY_WEIGHT;
                    break;

                case GeneratorConstants.SUGGEST_SESSION_SELECT_PRODUCT:
                    weight = SUGGEST_SESSION_SELECT_PRODUCT_WEIGHT;
                    break;

                case GeneratorConstants.TERM_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION: // 'partial' search
                    weight = TERM_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.TERM_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                    weight = TERM_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.CATEGORY_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION:
                    weight = CATEGORY_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION_WEIGHT;
                    break;

                case GeneratorConstants.CATEGORY_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                    weight = CATEGORY_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION_WEIGHT;
                    break;

                default:
                    MessageLogger.logError (String.format ("Unknown session type in predefinedJourney: %d", sessionType));
                    weight = -1;
                    break;
            } // end of switch
       
            for (int j = 0; j < weight; j++) {
                weightedSessionList.add (sessionType);
            }
        } // end of for

        // shuffle the weightedSessionList, 3 times
        for (int shuffleCount = 0; shuffleCount < 3; shuffleCount++) {
            for (int num = 0; num < weightedSessionList.size(); num++) {
                int indx1;
                int indx2;
                int temp;

                indx1 = (int) (Math.random () * weightedSessionList.size());
                indx2 = (int) (Math.random () * weightedSessionList.size());
                if (indx1 != indx2) {
                    temp = weightedSessionList.get (indx1);
                    weightedSessionList.set (indx1, weightedSessionList.get (indx2));
                    weightedSessionList.set (indx2, temp);
                }
            }
        }

        return weightedSessionList;
    }

    private boolean proceedToATC () {
        double random;

        random = Math.random() * 100;
        if (random < GeneratorConstants.MAX_ATC_PERCENT) {
            return true;
        }
        return false;
    }

    private boolean proceedToConversion () {
        double random;
        random = Math.random() * 100;
        if (random < GeneratorConstants.MAX_CONVERSION_PERCENT) {
            return true;
        }
        return false;
    }
}

/*
// } catch (ForcedSessionExitException fee) {
//                String msg;
//            msg = String.format ("Forced exit, visitorId = %s, stepId = %s\n",
//                                                        userRecord.getVisitorId (), sessionNum, sessionType);
//                MessageLogger.logWarning (msg);
//                // re-throw ForcedExit exception
//                // throw new ForcedSessionExitException(msg); -- CODE TO BE REMOVED after QA
//                StepResultInvalidData exceptionResult;
//
//                MessageLogger.logWarning (String.format ("Forced session exit, visitorId = %s, sessionNum = %d, sessionType = %s\n",
//                                                        userRecord.getVisitorId (), sessionNum, sessionType));
//                exceptionResult = new StepResultInvalidData ();
//                exceptionResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/home/blank/...
//                exceptionResult.setUrl (prevStepResult.getUrl ());
//                exceptionResult.setMessage (String.format ("Forced session exit, %s", fee.getMessage()));
//                exceptionResult.setEndTime (prevStepResult.getEndTime () + 1000);
//
//                stepResult = exceptionResult;
*/
