package com.bloomreach.trafficgenerator.site.journeylogs;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;

public class ApiCountLog {

    public final static String APITYPE_PIXEL = "pixel";
    public final static String APITYPE_CATEGORY_SEARCH = "category";
    public final static String APITYPE_SEARCH_TERM = "searchterm";
    public final static String APITYPE_SUGGEST = "suggest";
    public final static String APITYPE_WIDGET = "widget";

    private final static long COUNT_COLLECTION_TIMEBRACKET = 5 * 60; // N minutes in sec
    private Object collectorMutex;
    private ArrayList <ApiCountRecord> apiCounts;
    private ApiCountRecord apiCountRecord;
    private ApiCountRecord maxApiCountRecord; // holds 'max' counts across entire site
    private CollectorTimerThread collectorTimerThread;
    private FileWriter writer = null;

    public ApiCountLog () {
        apiCounts = new ArrayList <ApiCountRecord> ();
        collectorMutex = new Object ();
        collectorTimerThread = new CollectorTimerThread();
        maxApiCountRecord = new ApiCountRecord(); // NOT included in apiCounts arrayList
        maxApiCountRecord.recordTime = -1; // internal indicator for a 'max' count record
    }

    public void setLogPath  (String logPath) {
        try {
            writer = new FileWriter (logPath);
        } catch (Exception e) {
            MessageLogger.logError ("Exception creating apiCountCollector logWriter: " + e.getMessage());
        }
    }

    public void start () {
        this.collectorTimerThread.start ();
    }

    public void updateCount (String apiType) {
        synchronized (collectorMutex) {
            switch (apiType) {
                case APITYPE_PIXEL:
                    ++apiCountRecord.pixelCount;
                    maxApiCountRecord.pixelCount = Math.max(maxApiCountRecord.pixelCount, apiCountRecord.pixelCount);
                    break;
                case APITYPE_CATEGORY_SEARCH:
                    ++apiCountRecord.categoryCount;
                    maxApiCountRecord.categoryCount = Math.max(maxApiCountRecord.categoryCount, apiCountRecord.categoryCount);
                    break;
                case APITYPE_SEARCH_TERM:
                    ++apiCountRecord.searchtermCount;
                    maxApiCountRecord.searchtermCount = Math.max(maxApiCountRecord.searchtermCount, apiCountRecord.searchtermCount);
                    break;
                case APITYPE_SUGGEST:
                    ++apiCountRecord.suggestCount;
                    maxApiCountRecord.suggestCount = Math.max(maxApiCountRecord.suggestCount, apiCountRecord.suggestCount);
                    break;
                case APITYPE_WIDGET:
                    ++apiCountRecord.widgetCount;
                    maxApiCountRecord.widgetCount = Math.max(maxApiCountRecord.widgetCount, apiCountRecord.widgetCount);
                    break;
                default:
                    MessageLogger.logError (String.format ("ApiCountCollector, unknown apiType: %s", apiType));
                    break;
            }
        }
    }

    public void close () throws Exception {
        // close timer thread
        this.collectorTimerThread.interrupt();
        
        if (writer != null) {
            writeLog ();
            writer.close ();
            writer = null;
        }
    }

     // INTERNAL METHODS
     private void writeLog () throws Exception {
        if ((writer != null) && (apiCounts.size() > 0)) {
            String headerLine;
            String logLine;

            headerLine = buildLogHeader();
            writer.write (headerLine+"\n");
            try {
                // first write out 'maxCount' record
                logLine = buildLogLine(maxApiCountRecord);
                writer.write(logLine + "\n");

                // then individual apiCount log records
                for (ApiCountRecord countRecord: apiCounts) {
                    if (countRecord.isEmpty() == false) {
                        logLine = buildLogLine(countRecord);
                        writer.write (logLine + "\n");
                    }
                }
                writer.write ("\n");
                writer.flush();
            } catch (Exception e) {
                MessageLogger.logError ("Exception in writing apiCountCollector log record, " + e.getMessage());
            }
        }
    }

     private String buildLogHeader () {
        String headerLine;
        StringBuffer sb;

        sb = new StringBuffer();
        sb.append("Time");

        sb.append("\tPixelApiCount"); // +apiCountRecord.pixelCount); // updated for each pixel of given type
        sb.append("\tCategoryApiCount"); // +apiCountRecord.categoryCount);
        sb.append("\tTermApiCount"); // +apiCountRecord.searchtermCount);
        sb.append("\tSuggestApiCount"); //+apiCountRecord.suggestCount);
        sb.append("\tWidgetApiCount"); // +apiCountRecord.widgetCount);
        
        // count rate (per-second)
        sb.append("\tPixelApiCountPS"); 
        sb.append("\tCategoryApiCountPS"); 
        sb.append("\tTermApiCountPS"); 
        sb.append("\tSuggestApiCountPS");
        sb.append("\tWidgetApiCountPS"); 

        headerLine = new String (sb);
        return headerLine;
    }

    // logLine format must match header format
    private String buildLogLine (ApiCountRecord countRecord) {
        String line;
        StringBuffer sb;
        SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss,SSS"); // format to match date printed by logger

        sb = new StringBuffer();
        if (countRecord.recordTime < 0) // maxCountRecord has logTime = -1
            sb.append ("Max counts");
        else
            sb.append (dateFormat.format (countRecord.recordTime));

        sb.append("\t"+countRecord.pixelCount); // updated for each pixel of given type
        sb.append("\t"+countRecord.categoryCount);
        sb.append("\t"+countRecord.searchtermCount);
        sb.append("\t"+countRecord.suggestCount);
        sb.append("\t"+countRecord.widgetCount);

        // per-second
        sb.append("\t"+ getCountsPerSecond(countRecord.pixelCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.categoryCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.searchtermCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.suggestCount) );
        sb.append("\t"+ getCountsPerSecond(countRecord.widgetCount));

        line = new String (sb);
        return line;
    }

    private int getCountsPerSecond (int count) {
        float perSecond;
        int percent;

        perSecond = ((float) (count))/COUNT_COLLECTION_TIMEBRACKET;
        percent = (int) (perSecond);
        return percent;
    }

    class ApiCountRecord {
        long recordTime;    // actual time 
        int pixelCount; // updated for each pixel of given type
        int categoryCount;
        int searchtermCount;
        int suggestCount;
        int widgetCount;

        ApiCountRecord () { 
            recordTime = System.currentTimeMillis();
        }

        boolean isEmpty () {
            if ((pixelCount == 0) &&
                (categoryCount == 0) &&
                (searchtermCount == 0) &&
                (suggestCount == 0) &&
                (widgetCount == 0))
                return true;
            
            return false;
        }
    }

    class CollectorTimerThread extends Thread {
        CollectorTimerThread () {
            setName("ApiCountLog timer");
        }
        public void run () {
            do {
                try {
                    synchronized (collectorMutex) {
                        apiCountRecord = new ApiCountRecord ();
                        apiCounts.add (apiCountRecord);
                    }
                    sleep (COUNT_COLLECTION_TIMEBRACKET * 1000); // millisec
                } catch (InterruptedException ie) {
                    MessageLogger.logDebug( "ApiCountCollector interrupted");
                    break;
                }
            } while (isInterrupted() == false);
        }
    }

}
