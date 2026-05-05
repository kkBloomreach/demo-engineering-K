package com.bloomreach.trafficgenerator.site;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journey.*;
import com.bloomreach.trafficgenerator.site.journeylogs.*;
import com.bloomreach.trafficgenerator.site.user.*;

public class VisitorHandlerThread extends Thread {

    private final static int JOURNEY_TYPE_NONE = 0;
    private final static int JOURNEY_TYPE_PREDEFINED = 1;
    private final static int JOURNEY_TYPE_RANDOM = 2;
    private final static int JOURNEY_TYPE_CURATED = 3;

    private UserRecord userRecord;
    private JourneyBuilder journeyBuilder;
    private UserLog userLog;
    private long userArrivalTime;
    private String specialVisitorId;
    private SiteVisitorMonitor siteVisitorMonitor;
    private boolean curatedJourney = false;

    public VisitorHandlerThread () {
        setDaemon (true);
    }

    public void setSiteVisitorMonitor (SiteVisitorMonitor siteVisitorMonitor) {
        this.siteVisitorMonitor = siteVisitorMonitor;
    }

    public void setUserRecord (UserRecord userRecord) {
        this.userRecord = userRecord;
    }

    public void setJourneyBuilder (JourneyBuilder journeyBuilder) {
        this.journeyBuilder = journeyBuilder;
    }

    public void setUserArrivalTime (long arrivalTime) {
        this.userArrivalTime = arrivalTime;
    }

    public void setUserLog (UserLog userLog) {
        this.userLog = userLog;
    }

    public void setSpecialVisitorId (String specialVisitorId) {
        this.specialVisitorId = specialVisitorId;
    }

    public void setCuratedJourney (boolean curatedJourney) {
        this.curatedJourney = curatedJourney;
    }

    public void run () {
        try {
            MessageLogger.logDebug (String.format ("Site VisitorHandler thread started, visitorId = %s", this.userRecord.getVisitorId ()));
            performVisitorJourney ();
        } catch (InterruptedException ie) {
            MessageLogger.logWarning ("Site VisitorHandler thread interrupted");
        } catch (CustomJourneyException fse) {
            // for specific acct, some specific categories are set to cause immediate exit
            // (eg, demo_shopify). This is needed so that SCs can show insight reports
            // that explain effect of bounce
            MessageLogger.logDebug (String.format ("CustomJourneyException, visitorId = %s", this.userRecord.getVisitorId ()));
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logError ("Site VisitorHandler thread exception: " + e.getMessage());
        }

        this.siteVisitorMonitor.exitVisitor (this.userRecord.getUserId());
        MessageLogger.logDebug (String.format ("Site VisitorHandler thread completed, visitorId = %s", this.userRecord.getVisitorId ()));
    }

    // Note - every visitor has own Generator (avoid thread conflict)
    private void performVisitorJourney () throws Exception {
        int journeyType;

        journeyType = selectJourneyType ();
        switch (journeyType) {
            case JOURNEY_TYPE_PREDEFINED:
                PredefinedJourneyGenerator predefinedJourneyGenerator;  

                predefinedJourneyGenerator = this.journeyBuilder.buildPredefinedJourneyGenerator (); 
                generatePixelAndApiDataForOneVisitor (predefinedJourneyGenerator);
                break;
            
            case JOURNEY_TYPE_RANDOM:
                RandomJourneyGenerator randomJourneyGenerator;

                randomJourneyGenerator = this.journeyBuilder.buildRandomJourneyGenerator (); 
                generatePixelAndApiDataForOneVisitor (randomJourneyGenerator);
                break;

            case JOURNEY_TYPE_CURATED:
                CuratedJourneyGenerator curatedJourneyGenerator;

                curatedJourneyGenerator = this.journeyBuilder.buildCuratedJourneyGenerator ();
                generatePixelAndApiDataForOneVisitor (curatedJourneyGenerator);
               break;
            
            default:
                MessageLogger.logError ("Unknown journeyType");
                return;
        }
    }

    private int selectJourneyType () {
        int journeyType = JOURNEY_TYPE_NONE;    // default

        // if visitor is "SPECIAL_VISITOR", always perform Predefined journey so that
        // past purchase data etc are sure to be generated
        if ((this.specialVisitorId != null) && (this.userRecord.getVisitorId ().equals (this.specialVisitorId))) {
            journeyType = JOURNEY_TYPE_PREDEFINED;
        } else if (this.curatedJourney) {
            // if curatedJourney, ALL visitors to this site(domain) use curatedJourney
            // This is so that the 1:1(V2) pipeline learns consistent interactions
            // instead of some-curated and some-random
            journeyType = JOURNEY_TYPE_CURATED;
        } else {
            int randomIndx;

            // based on a random factor, either use 'predefined' or 'random' session generator
            // index < 7 => 70% predefined, 30% random
            randomIndx = (int) (Math.random () * 10);
            if (randomIndx < 7) { 
                journeyType = JOURNEY_TYPE_PREDEFINED;
            } else {
                journeyType = JOURNEY_TYPE_RANDOM;
            }
        }
        return journeyType;
    }

    private void generatePixelAndApiDataForOneVisitor (PredefinedJourneyGenerator predefinedJourneyGenerator) throws Exception {
        JourneyLog journeyLog;

        MessageLogger.logDebug (String.format ("Start visitor predefined sessions: visitorId = %s", this.userRecord.getVisitorId()));
        journeyLog = this.userLog.addJourneyLog ("predefined", this.userArrivalTime);
        predefinedJourneyGenerator.startJourney (this.userRecord,
                                                 this.userArrivalTime,
                                                 journeyLog);
    }

    private void generatePixelAndApiDataForOneVisitor (RandomJourneyGenerator randomJourneyGenerator) throws Exception {
        JourneyLog journeyLog;

        MessageLogger.logDebug (String.format ("Start visitor random sessions: visitorId = %s", this.userRecord.getVisitorId()));
        journeyLog = this.userLog.addJourneyLog ("random", this.userArrivalTime);

        // continue generating data till visitor exits the site (or till MAX step-count)
        randomJourneyGenerator.startJourney (this.userRecord,
                                             this.userArrivalTime,
                                             journeyLog);
    }

    private void generatePixelAndApiDataForOneVisitor (CuratedJourneyGenerator curatedJourneyGenerator) throws Exception {
        JourneyLog journeyLog;

        MessageLogger.logDebug (String.format ("Start visitor curated sessions: visitorId = %s", this.userRecord.getVisitorId()));
        journeyLog = this.userLog.addJourneyLog ("curated", this.userArrivalTime);

        // continue generating data till visitor exits the site (or till MAX step-count)
        curatedJourneyGenerator.startJourney (this.userRecord,
                                              this.userArrivalTime,
                                              journeyLog);
    }
}

/* 
private void performVisitorJourney_SAVE () throws Exception {
        // Note - every visitor has own Generator (avoid thread conflict)
        // if visitor is "SPECIAL_VISITOR", always perform Predefined journey so that
        // past purchase data etc are sure to be generated
        if ((this.specialVisitorId != null) && (this.userRecord.getVisitorId ().equals (this.specialVisitorId))) {
            PredefinedJourneyGenerator predefinedJourneyGenerator;  

            predefinedJourneyGenerator = this.journeyBuilder.buildPredefinedJourneyGenerator (); 
            generatePixelAndApiDataForOneVisitor (predefinedJourneyGenerator);
        } else if (this.curatedJourney) {
            /-*
            CuratedJourneyGenerator curatedJourneyGenerator;

            curatedJourneyGenerator = this.journeyBuilder.buildCuratedJourneyGenerator ();
            generatePixelAndApiDataForOneVisitor (curatedJourneyGenerator);
            *-/
        } else {
            int randomIndx;

            randomIndx = (int) (Math.random () * 10);

            // based on a random factor, either use 'predefined' or 'random' session generator
            // index < 7 => 70% predefined, 30% random
            if (randomIndx < 7) { 
                PredefinedJourneyGenerator predefinedJourneyGenerator;  

                predefinedJourneyGenerator = this.journeyBuilder.buildPredefinedJourneyGenerator (); 
                generatePixelAndApiDataForOneVisitor (predefinedJourneyGenerator);
            } else {
                RandomJourneyGenerator randomJourneyGenerator;
                randomJourneyGenerator = this.journeyBuilder.buildRandomJourneyGenerator (); 
                generatePixelAndApiDataForOneVisitor (randomJourneyGenerator);
            }
        }
    }
*/

