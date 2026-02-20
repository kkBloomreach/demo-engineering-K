// generate pixelLogs for different sessions
// eg, search, category, search-to-search refined, ...
package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.journeydata.*;
import com.bloomreach.trafficgenerator.site.journeylogs.*;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.GeneratorConstants;

public class RandomJourneyGenerator {

    // Traffic steps
    TrafficSteps trafficSteps;

    // steps Handler
    StepsHandler stepsHandler;

    public RandomJourneyGenerator () {
    }

    public void setTrafficSteps (TrafficSteps trafficSteps) {
        this.trafficSteps = trafficSteps;
    }

    public void setStepsHandler (StepsHandler stepsHandler) {
        this.stepsHandler = stepsHandler;
    }

    public boolean init () {
        // place holder
        return true;
    }

    // a visitor may generate multiple sessions during a single visit
    public void startJourney (UserRecord userRecord, 
                              long journeyStartTime,
                              JourneyLog journeyLog) throws Exception {

        TrafficStepInfo stepInfo;
        StepResult prevStepResult;
        StepResult stepResult;
        int stepCount;
        long logTime;
        Cart userCart;
        SessionLog newSessionLog;
        StepLog stepLog;

        // set for a visitor's entire journey 
        stepCount = 0;
        userCart = new Cart ();
        userCart.setUserId (userRecord.getUserId ());

        // begin
        logTime = journeyStartTime;
        prevStepResult = new StepResultVoid ();
        prevStepResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/blank/...
        prevStepResult.setUrl (null); // set below in OpenUrl
        prevStepResult.setEndTime (journeyStartTime);

        // add sessionLog to journeyLog
        newSessionLog = journeyLog.addSessionLog (GeneratorConstants.UNDEFINED_SESSION_TYPE);

        // initial stepInfo
        stepInfo = this.trafficSteps.lookupStep (GeneratorConstants.TRAFFIC_STEPID_START_URL);

        // loop till exit OR count-limit. For RandomJourney, there is only one "session" in trafficGenerator
        // There are multiple 'steps' the visitor takes within this
        while ((stepInfo.getStepId () != GeneratorConstants.TRAFFIC_STEPID_EXIT) &&
               (stepCount++ < GeneratorConstants.MAX_STEP_COUNT_IN_RANDOM_SESSION)) {
            try {
                JourneyStepOutcome journeyStepOutcome;

                journeyStepOutcome = performJourneyStep (userRecord, userCart, stepInfo, prevStepResult, logTime, newSessionLog);
                stepInfo = journeyStepOutcome.stepInfo;
                stepResult = journeyStepOutcome.stepResult;
                logTime = stepResult.getEndTime ();
            
            } catch (CustomJourneyException cje) {
                // CustomJourney is not supported in RandomJourneyGenerator because random-journey-steps
                // are driven via a state machine.
                StepResultInvalidData exceptionResult;
                String msg;

                msg = String.format ("CustomJourney not supported in randomJourneyGenerator; visitorId = %s, stepId = %s. journeyType = %s, journeyData = %s\n",
                                                        userRecord.getVisitorId (), stepInfo.getStepId(),
                                                        cje.getCustomJourneyData().getCustomJourneyType(), 
                                                        cje.getCustomJourneyData().getCustomJourneyTarget());
              
                /* Since customJourney is not supported in RandomJourney, just exit the entire session */
                /*
                MessageLogger.logWarning (msg);
                exceptionResult = new StepResultInvalidData ();
                exceptionResult.setRefUrl (prevStepResult.getRefUrl ()); 
                exceptionResult.setEndTime (logTime + 1000);    // some delay, for debugging
                exceptionResult.setMessage (msg);
                stepResult = exceptionResult;
                */
                exceptionResult = new StepResultInvalidData ();
                exceptionResult.setMessage (msg);
                stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_EXIT, logTime); // logTime may be incorrect in this case
                stepLog.setUserSegment (userRecord.getSegment());
                stepLog.setStepResult ((String) exceptionResult.getData ());
                throw new CustomJourneyException(cje.getCustomJourneyData());
            } catch (Exception e) {
                StepResultInvalidData exceptionResult;

                e.printStackTrace ();
                MessageLogger.logError (String.format ("Exception in Random journey, visitorId = %s, stepId = %s\n", 
                                                        userRecord.getVisitorId (), stepInfo.getStepId()));
                exceptionResult = new StepResultInvalidData ();
                exceptionResult.setRefUrl (prevStepResult.getRefUrl ());
                exceptionResult.setEndTime (logTime + 1000);    // some delay, for debugging
                exceptionResult.setMessage (String.format ("Exception in session, %s", e.getMessage()));
                stepResult = exceptionResult;
            }

            if (stepResult instanceof StepResultInvalidData) {
                StepResult fixedStepResult;

                MessageLogger.logWarning ((String) stepResult.getData () );

                // in case of 'invalidData' stepResult, again start from 'start_url'
                stepInfo = this.trafficSteps.lookupStep (GeneratorConstants.TRAFFIC_STEPID_START_URL);

                // add a stepLog to log an 'exception' message in the log
                stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_EXCEPTION_RESTART, logTime); // logTime may be incorrect in this case
                stepLog.setUserSegment (userRecord.getSegment());
                stepLog.setStepResult ((String) stepResult.getData ());   // exception message if any

                // start stepResult as if new journey, starting from 'prev' url,refUrl
                fixedStepResult = new StepResultVoid ();
                fixedStepResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/blank/...
                fixedStepResult.setEndTime (stepResult.getEndTime () + 1000);

                // set 'prev' to the fixed stepResult
                stepResult = fixedStepResult;
            }

            prevStepResult = stepResult;    // for the next step
        }

        if (stepInfo.getStepId () != GeneratorConstants.TRAFFIC_STEPID_EXIT) {
            // exiting because too-many-steps
            MessageLogger.logWarning (String.format ("Exiting random session due to too many steps, visitorId = %s", 
                                                                            userRecord.getVisitorId ()));
            stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_EXIT, logTime);
            stepLog.setUserSegment (userRecord.getSegment());
        }

        newSessionLog.close ();
    }

    private JourneyStepOutcome performJourneyStep (UserRecord userRecord,
                                                Cart userCart, 
                                                TrafficStepInfo stepInfo,
                                                StepResult stepResult,
                                                long logTime,
                                                SessionLog newSessionLog) throws Exception {

        StepLog stepLog;

        MessageLogger.logDebug (String.format ("Random journey step, userId = %s, stepId = %s", userRecord.getUserId (), stepInfo.getStepId()));
        switch (stepInfo.getStepId ()) {
                case GeneratorConstants.TRAFFIC_STEPID_START_URL:
                    // In case stepResult is invalid, start from "start_url" again
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepOpenUrl (stepResult, userRecord, logTime, stepLog); 
                    // handle widget if needed. Currently homepage and productpage widgets are supported
                    logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, newSessionLog); 
                    stepResult.setEndTime(logTime); 
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP:
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog);
                    logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, newSessionLog); 
                    stepResult.setEndTime(logTime);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_VIEW_LIST:
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepViewList (stepResult, userRecord, logTime, stepLog);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_SEARCH_TERM:
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSearchTerm (stepResult, userRecord, logTime, stepLog);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_SEARCH_CAT:
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSearchCategory (stepResult, userRecord, logTime, stepLog);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY:
                    // this step is same as suggest-then-do-nothing - therefore drop-down into next case
                    // Kept for backword compatibility

                case GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_NONE:
                    // log for 'suggest-query'
                    stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY, logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSuggestQuery (stepResult, userRecord, logTime, stepLog);
                    logTime = stepResult.getEndTime ();

                    // next, log for step-info = 'select-none'
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSuggestSelectNone (stepResult);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_TERM:
                    // log for 'suggest-query'
                    stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY, logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSuggestQuery (stepResult, userRecord, logTime, stepLog);
                    logTime = stepResult.getEndTime ();

                    // next, log for step-info = 'select-term'
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSuggestSelectTerm (stepResult, userRecord, logTime, stepLog);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_CAT:
                    // log for 'suggest-query'
                    stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY, logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSuggestQuery (stepResult, userRecord, logTime, stepLog);
                    logTime = stepResult.getEndTime ();

                    // next, log for step-info = 'select-cat'
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSuggestSelectCategory (stepResult, userRecord, logTime, stepLog);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_SELECT_SUG_PROD:
                    // log for 'suggest-query'
                    stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_SUG_QUERY, logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSuggestQuery (stepResult, userRecord, logTime, stepLog);
                    logTime = stepResult.getEndTime ();
                    if (! (stepResult instanceof StepResultInvalidData)) {
                        // next, log for step-info = 'select-prod'
                        stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                        stepLog.setUserSegment (userRecord.getSegment());
                        stepResult = stepsHandler.handleStepSuggestSelectProduct (stepResult, userRecord, logTime, stepLog);
                        logTime = stepResult.getEndTime ();

                        if (! (stepResult instanceof StepResultInvalidData)) {
                            // next, log for browse-pdp
                            stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_BROWSE_PDP, logTime);
                            stepLog.setUserSegment (userRecord.getSegment());
                            stepResult = stepsHandler.handleStepBrowsePDP (stepResult, userRecord, logTime, stepLog);
                            logTime = this.stepsHandler.handleWidgetsOnPage (stepResult, userRecord, userCart, logTime, newSessionLog); 
                            stepResult.setEndTime(logTime);
                        }
                        logTime = stepResult.getEndTime ();
                    }
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_ATC:
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepATC (stepResult, userCart, userRecord, logTime, stepLog);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_CONVERT:
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepConvert (stepResult, userCart, userRecord, logTime, stepLog);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_SELECT_PID_FROM_LIST:
                    stepLog = newSessionLog.addStepLog (stepInfo.getStepId (), logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    stepResult = stepsHandler.handleStepSelectProductFromList (stepResult, userRecord, logTime, stepLog);
                    break;

                case GeneratorConstants.TRAFFIC_STEPID_EXIT:    // won't reach this case in normal flow
                    stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_EXIT, logTime);
                    stepLog.setUserSegment (userRecord.getSegment());
                    break;

                default:
                    MessageLogger.logError (String.format ("Unknow stepId = %d", stepInfo.getStepId()));
                    stepLog = null; // keep java happy
        } // end of switch stmt


        if (stepResult == null) { 
                StepResultInvalidData exceptionResult;

                MessageLogger.logError (String.format ("Unsupported prevResult, stepId = %d", stepInfo.getStepId()));
                // start from 'start_url'
                stepInfo = this.trafficSteps.lookupStep (GeneratorConstants.TRAFFIC_STEPID_START_URL);

                exceptionResult = new StepResultInvalidData ();
                exceptionResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/blank/...
                exceptionResult.setEndTime (logTime + 1000);    // some delay, for debugging
                exceptionResult.setMessage (String.format ("null stepResult from stepId = %d", stepInfo.getStepId()));
                stepResult = exceptionResult;
        } else if (stepResult instanceof StepResultInvalidData) {

                MessageLogger.logWarning ((String) stepResult.getData ());
                // start from 'start_url'
                stepInfo = this.trafficSteps.lookupStep (GeneratorConstants.TRAFFIC_STEPID_START_URL);
                
        } else {
                // next step
                stepInfo = this.trafficSteps.lookupNextStepAtRandom (stepInfo);
        }

        return (new JourneyStepOutcome (stepInfo, stepResult));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // internal class
    class JourneyStepOutcome {
        TrafficStepInfo stepInfo;
        StepResult stepResult;

        JourneyStepOutcome (TrafficStepInfo stepInfo, StepResult stepResult) {
            this.stepInfo = stepInfo;
            this.stepResult = stepResult;
        }
    }
}

/*****
// * } catch (ForcedSessionExitException fee) {
//                String msg;
//                msg = String.format ("Forced exit, visitorId = %s, stepId = %s\n",
//                                                        userRecord.getVisitorId (), stepInfo.getStepId());
//                MessageLogger.logWarning (msg);
//                // re-throw ForcedExit exception
//                // throw new ForcedSessionExitException(msg); CODE TO BE REMOVED after QA
//                StepResultInvalidData exceptionResult;
//                exceptionResult = new StepResultInvalidData ();
//                exceptionResult.setRefUrl (prevStepResult.getRefUrl ()); 
//                exceptionResult.setEndTime (logTime + 1000);    // some delay, for debugging
//                exceptionResult.setMessage (String.format ("Forced session exit, %s", fee.getMessage()));
//                stepResult = exceptionResult;
// DO WE NEED THIS CODE ?
//                 // if prev stepResult is invalid (eg, zero docs in search response), start again
//                 stepLog = newSessionLog.addStepLog (GeneratorConstants.TRAFFIC_STEPID_EXCEPTION_RESTART, logTime); // logTime may be incorrect in this case
//                 stepLog.setUserSegment (userRecord.getSegment());
//                 stepLog.setQuery ((String) stepResult.getData ());   // exception message if any
// 
//                 // start stepResult as if new journey, starting from home page
//                 stepResult = new StepResultVoid ();
//                 stepResult.setRefUrl (stepsHandler.selectStartRefUrl ()); // 'search-engine'/'social'/blank/...
//                 stepResult.setEndTime (logTime + 1000);
*/

