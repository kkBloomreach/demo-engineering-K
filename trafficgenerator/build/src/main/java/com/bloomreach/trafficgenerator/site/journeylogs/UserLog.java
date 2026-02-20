package com.bloomreach.trafficgenerator.site.journeylogs;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.user.UserRecord;

public class UserLog {

    private UserRecord userRecord;
    private ArrayList<JourneyLog> journeyLogs; 
    private SaveLock saveLock;

    public UserLog (UserRecord userRecord, SaveLock saveLock) {
        this.userRecord = userRecord;
        this.journeyLogs = new ArrayList <JourneyLog> ();
        this.saveLock = saveLock;
    }

    // journeyType = "predefined", "random"
    public JourneyLog addJourneyLog (String journeyType, long startTime) {
        JourneyLog journeyLog;

        synchronized (this.saveLock) {
            journeyLog = new JourneyLog (journeyType, startTime, saveLock);
            this.journeyLogs.add (journeyLog);
        }
        return journeyLog;
    }

    public String getUserId () {
        return this.userRecord.getUserId ();
    }

    // Collect all user logs and update each with user info 
    // method called only within this package - not otherwise
    protected ArrayList <LogRecord> prepareLogRecords () {
        ArrayList <LogRecord> userLogRecords;

        userLogRecords = new ArrayList <LogRecord> ();
        for (JourneyLog journeyLog : this.journeyLogs) {
            ArrayList <LogRecord> journeyLogRecords;

            journeyLogRecords = journeyLog.prepareLogRecords ();
            for (LogRecord logRecord : journeyLogRecords) {
                logRecord.setUserId (this.userRecord.getUserId());
                userLogRecords.add (logRecord);
            }
        }
        return userLogRecords;
    }

    protected void performCleanup () throws Exception {
        int removeCount;

        for (JourneyLog journeyLog : this.journeyLogs) {
            journeyLog.performCleanup ();
        }

        // remove all-but-the-last (or 'current') journey from journeyLogs list
        removeCount = this.journeyLogs.size () - 1;
        for (int i = 0; i < removeCount; i++) {
            this.journeyLogs.remove (0);    // remove at 0
        }
    }
}
