package com.bloomreach.trafficgenerator.site.journeylogs;

import java.util.ArrayList;
import java.io.FileWriter;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.EnvironmentConfig;
import com.bloomreach.trafficgenerator.site.user.UserRecord;

public class DailyLog {

    private ArrayList<UserLog> userLogs;
    private FileWriter dailyLogWriter;
    private int maxUserLogsInMemory;    // different for different environments
    private SaveLock saveLock;
 
    public DailyLog () {
        userLogs = new ArrayList <UserLog> ();
        saveLock = new SaveLock ();
    }

    public void setLogPath (String logPath) throws Exception {
        this.dailyLogWriter = new FileWriter (logPath);
        // write header once
        LogRecord.writeHeader (this.dailyLogWriter);
        // different for different environments for dev/qa/release
        maxUserLogsInMemory = (int) EnvironmentConfig.getEnvParamLong ("MAX_USER_LOGS_IN_MEMORY");
    }

    public UserLog addUserLog (UserRecord userRecord) {
        UserLog userLog;

        synchronized (this.saveLock) {
            // After every N userLogs, write to file and clean up
            // in-memory list. This is to manage memory usage
            if (userLogs.size () > this.maxUserLogsInMemory) {
                try {
                    saveUserLog ();
                    performCleanup ();
                } catch (Exception e) {
                    e.printStackTrace ();
                    MessageLogger.logError ("Error in saving or cleanup user logs");
                }
            }

            userLog = new UserLog (userRecord, saveLock);
            this.userLogs.add (userLog);
        }

        return userLog;
    }

    // write all remaining log records for all users (their journeys, sessions, steps, ...)
    // this method called once at the time a site is closed
    public void closeUserLog () throws Exception {
        synchronized (this.saveLock) {
            saveUserLog ();
        }
        this.dailyLogWriter.close ();
        this.dailyLogWriter = null;
    }

    // write log records collected so far (their journeys, sessions, steps, ...)
    private void saveUserLog () throws Exception {
        for (UserLog userLog : this.userLogs) {
            ArrayList <LogRecord> userLogRecords;

            userLogRecords = userLog.prepareLogRecords ();
            // immediately write the log records to store file
            if ((userLogRecords != null) && (userLogRecords.size () > 0)) {
                for (LogRecord logRecord : userLogRecords) {
                    logRecord.writeData (this.dailyLogWriter);
                }
            } 
        }
        this.dailyLogWriter.flush ();
    }

    // after saving records, for each userLog, remove all its journey/session/step
    // logs EXCEPT the last entry (since that may still be 'in use')
    // This is to save memory usage
    private void performCleanup () throws Exception {
        for (UserLog userLog : this.userLogs) {
            userLog.performCleanup ();
            // note - userLog object itself is not removed from the list
            // since the user may still be 'active' on the site
        }
    }

}

