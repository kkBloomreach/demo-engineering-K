// currently a very simplistic approach to log widget actions (api-calls, add-to-cart, ...)
package com.bloomreach.trafficgenerator.site.journeylogs;

import java.util.ArrayList;
import java.io.FileWriter;

import com.bloomreach.trafficgenerator.MessageLogger;

public class WidgetLog {
    private final static int MAX_LOGRECORD_COUNT = 1000;

    private ArrayList<String> logLines;
    private FileWriter writer = null;

    public WidgetLog () {
        logLines = new ArrayList <String> ();
    }

    public void setLogPath  (String logPath) {
        String logHeader;

        try {
            writer = new FileWriter (logPath);
            logHeader = String.format ("%s\t%s\t%s\t%s\t%s\t%s\t%s\n", "LogType", "LogTime", "UserId", "Url", "WidgetId", "ItemId", "NumFound");
            writer.write (logHeader);
            writer.flush();
        } catch (Exception e) {
            MessageLogger.logError ("Exception creating widget logWriter: " + e.getMessage());
        }
    }

    public synchronized void addApiRecord (String userId, String pageUrl, String widgetId, String itemId, int numFound, long logTime) {
        String logLine;

        if (logLines.size () > MAX_LOGRECORD_COUNT) {
            writeLogs ();
            logLines.clear();
        }

        logLine = buildLogLine ("Api", userId, pageUrl, widgetId, itemId, numFound, logTime);
        logLines.add (logLine);
        return;
    }

    public synchronized void addClickRecord (String userId, String pageUrl, String widgetId, String itemId, int numFound, long logTime) {
        String logLine;

        if (logLines.size () > MAX_LOGRECORD_COUNT) {
            writeLogs ();
            logLines.clear();
        }

        logLine = buildLogLine ("Click", userId, pageUrl, widgetId, itemId, numFound, logTime);
        logLines.add (logLine);

        return;
    }

    public synchronized void addATCRecord (String userId, String pageUrl, String widgetId, String itemId, int numFound, long logTime) {
        String logLine;

        if (logLines.size () > MAX_LOGRECORD_COUNT) {
            writeLogs ();
            logLines.clear();
        }

        logLine = buildLogLine ("ATC", userId, pageUrl, widgetId, itemId, numFound, logTime);
        logLines.add (logLine);
        
        return;
    }

    public void close () throws Exception {
        if (writer != null) {
            writeLogs (); // remaining logs
            writer.close ();
            writer = null;
        }
    }

    // INTERNAL METHODS
    // logLine format must match header format
    private String buildLogLine (String logType, String userId, String pageUrl, String widgetId, String itemId, int numFound, long logTime) {
        String line;

        if (itemId == null)
            itemId = "-";
        line = String.format ("%s\t%s\t%s\t%s\t%s\t%s\t%s\n", logType, logTime, userId, pageUrl, widgetId, itemId, numFound);
        return line;
    }

    private void writeLogs () {
        if ((writer != null) && (logLines.size() > 0)) {
            try {   
                for (String logLine : logLines) {
                    writer.write (logLine);
                }
                writer.flush();
            } catch (Exception e) {
                MessageLogger.logError ("Exception in writing widget log record, " + e.getMessage());
            }
        }
    }

}
