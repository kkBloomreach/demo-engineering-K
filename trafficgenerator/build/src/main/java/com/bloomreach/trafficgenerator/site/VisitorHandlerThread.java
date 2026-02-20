package com.bloomreach.trafficgenerator.site;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journey.*;
import com.bloomreach.trafficgenerator.site.journeylogs.*;
import com.bloomreach.trafficgenerator.site.user.*;

public class VisitorHandlerThread extends Thread {
    private UserRecord userRecord;
    private JourneyBuilder journeyBuilder;
    private UserLog userLog;
    private long userArrivalTime;
    private String specialVisitorId;
    private SiteVisitorMonitor siteVisitorMonitor;

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

    public void run () {
        try {
            MessageLogger.logDebug (String.format ("Site VisitorHandler thread started, visitorId = %s", this.userRecord.getVisitorId ()));
            performVisitorJourney ();
        } catch (InterruptedException ie) {
            MessageLogger.logWarning ("Site VisitorHandler thread interrupted");
            // this.siteVisitorMonitor.exitVisitor (this.userRecord.getUserId());
        } catch (CustomJourneyException fse) {
            MessageLogger.logDebug (String.format ("CustomJourneyException, visitorId = %s", this.userRecord.getVisitorId ()));
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logError ("Site VisitorHandler thread exception: " + e.getMessage());
            // this.siteVisitorMonitor.exitVisitor (this.userRecord.getUserId());
        }

        this.siteVisitorMonitor.exitVisitor (this.userRecord.getUserId());
        MessageLogger.logDebug (String.format ("Site VisitorHandler thread completed, visitorId = %s", this.userRecord.getVisitorId ()));
    }

    private void performVisitorJourney () throws Exception {
        int randomIndx;

        // Note - every visitor has own Generator (avoid thread conflict)
        // if visitor is "SPECIAL_VISITOR", always perform Predefined journey so that
        // past purchase data etc are sure to be generated
        
        if ((this.specialVisitorId != null) && (this.userRecord.getVisitorId ().equals (this.specialVisitorId))) {
            PredefinedJourneyGenerator predefinedJourneyGenerator;  

            predefinedJourneyGenerator = this.journeyBuilder.buildPredefinedJourneyGenerator (); 
            generatePixelAndApiDataForOneVisitor (predefinedJourneyGenerator);
        } else {
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
}

