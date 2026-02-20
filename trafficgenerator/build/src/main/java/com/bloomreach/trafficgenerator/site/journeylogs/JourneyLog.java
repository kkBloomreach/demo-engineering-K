package com.bloomreach.trafficgenerator.site.journeylogs;

import java.util.ArrayList;

public class JourneyLog {

    private String journeyType; //predefined, random
    private long startTime;
    private ArrayList<SessionLog> sessionLogs; 
    private SaveLock saveLock;

    public JourneyLog (String journeyType, long startTime, SaveLock saveLock) {
        this.journeyType = journeyType;
        this.startTime = startTime;
        this.sessionLogs = new ArrayList <SessionLog> ();
        this.saveLock = saveLock;
    }

    // session type = 'search', 'suggest', ...
    public SessionLog addSessionLog (int sessionType) {
        SessionLog sessionLog;
        synchronized (this.saveLock) {
            sessionLog = new SessionLog (this.sessionLogs.size() + 1, sessionType, saveLock);  // 1, 2, 3, ...
            this.sessionLogs.add (sessionLog);
        }
        return sessionLog;
    }

    // Collect all sessionLogs and update each with session's own info
    protected ArrayList <LogRecord> prepareLogRecords () {
        ArrayList <LogRecord> journeyLogRecords;

        journeyLogRecords = new ArrayList <LogRecord> ();
        for (SessionLog sessionLog : this.sessionLogs) {
            ArrayList <LogRecord> sessionLogRecords;
            sessionLogRecords = sessionLog.prepareLogRecords ();
            for (LogRecord sessionLogRecord : sessionLogRecords) {
                sessionLogRecord.setJourneyType (this.journeyType);
                sessionLogRecord.setJourneyStart (this.startTime);
                journeyLogRecords.add (sessionLogRecord);
            }
        }
        return journeyLogRecords;
    }

    protected void performCleanup () throws Exception {
        int removeCount;

        for (SessionLog sessionLog : this.sessionLogs) {
            sessionLog.performCleanup ();
        }

        // remove all-but-the-last (or 'current') session from sessionLogs list
        removeCount = this.sessionLogs.size () - 1;
        for (int i = 0; i < removeCount; i++) {
            this.sessionLogs.remove (0);    // remove at 0
        }
    }
}
