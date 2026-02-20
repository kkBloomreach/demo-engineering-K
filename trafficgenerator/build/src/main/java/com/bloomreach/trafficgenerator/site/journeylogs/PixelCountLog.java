package com.bloomreach.trafficgenerator.site.journeylogs;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journeydata.templates.PixelBRData;

public class PixelCountLog {

    private final static long COUNT_COLLECTION_TIMEBRACKET = 5 * 60; // N minutes in sec
    private Object collectorMutex;
    private ArrayList <PixelCountRecord> pixelCounts;
    private PixelCountRecord pixelCountRecord;
    private PixelCountRecord maxPixelCountRecord; // holds 'max' counts across entire site
    private CollectorTimerThread collectorTimerThread;
    private FileWriter writer = null;

    public PixelCountLog () {
        pixelCounts = new ArrayList <PixelCountRecord> ();
        collectorMutex = new Object ();
        collectorTimerThread = new CollectorTimerThread();
        maxPixelCountRecord = new PixelCountRecord(); // NOT included in pixelCounts arrayList
        maxPixelCountRecord.recordTime = -1; // internal indicator for a 'max' count record
    }

    public void setLogPath  (String logPath) {
        try {
            writer = new FileWriter (logPath);
        } catch (Exception e) {
            MessageLogger.logError ("Exception creating pixelCountCollector logWriter: " + e.getMessage());
        }
    }

    public void start () {
        this.collectorTimerThread.start ();
    }

    public void updateCount (String type, String pageType, String eventGroup, String eventType) {
        synchronized (collectorMutex) {
            pixelCountRecord.type = type;

            switch (type) {
                case PixelBRData.PIXEL_TYPE_PAGEVIEW:
                    pixelCountRecord.pageType = pageType;
                    switch (pageType) {
                        case PixelBRData.PAGE_TYPE_HOME: 
                            ++pixelCountRecord.homePageCount;
                            maxPixelCountRecord.homePageCount = Math.max(maxPixelCountRecord.homePageCount, pixelCountRecord.homePageCount);
                            break;
                        case PixelBRData.PAGE_TYPE_PRODUCT: 
                            ++pixelCountRecord.productPageCount; 
                            maxPixelCountRecord.productPageCount = Math.max(maxPixelCountRecord.productPageCount, pixelCountRecord.productPageCount);
                            break;
                        case PixelBRData.PAGE_TYPE_CATEGORY: 
                            ++pixelCountRecord.categoryPageCount;
                            maxPixelCountRecord.categoryPageCount = Math.max(maxPixelCountRecord.categoryPageCount, pixelCountRecord.categoryPageCount);
                            break;
                        case PixelBRData.PAGE_TYPE_SEARCH: 
                            ++pixelCountRecord.searchResultPageCount;
                            maxPixelCountRecord.searchResultPageCount = Math.max(maxPixelCountRecord.searchResultPageCount, pixelCountRecord.searchResultPageCount);
                            break;
                        case PixelBRData.PAGE_TYPE_OTHER: 
                            ++pixelCountRecord.otherPageCount;
                            maxPixelCountRecord.otherPageCount = Math.max(maxPixelCountRecord.otherPageCount, pixelCountRecord.otherPageCount);
                            break;
                        case PixelBRData.PAGE_TYPE_THEMATIC: 
                            ++pixelCountRecord.thematicPageCount;
                            maxPixelCountRecord.thematicPageCount = Math.max(maxPixelCountRecord.thematicPageCount, pixelCountRecord.thematicPageCount);
                            break;
                        default:
                            MessageLogger.logError (String.format ("PixelCountCollector, unknown pageType: %s", pageType));
                            break;
                    }
                break;

                case PixelBRData.PIXEL_TYPE_EVENT:
                    pixelCountRecord.eventGroup = eventGroup;
                    pixelCountRecord.eventType = eventType;
                    switch (eventGroup) {
                        case PixelBRData.EVENT_GROUP_CART:
                            switch (eventType) {
                                case PixelBRData.EVENT_ETYPE_CLICKADD: 
                                    ++pixelCountRecord.atcCount; 
                                    maxPixelCountRecord.atcCount = Math.max(maxPixelCountRecord.atcCount, pixelCountRecord.atcCount);
                                    break;
                                case PixelBRData.EVENT_ETYPE_WIDGET_ATC: 
                                    ++pixelCountRecord.widgetATCCount; 
                                    maxPixelCountRecord.widgetATCCount = Math.max(maxPixelCountRecord.widgetATCCount, pixelCountRecord.widgetATCCount);
                                    break;
                                default:
                                    MessageLogger.logError (String.format ("PixelCountCollector, unknown eventType %s in eventGroup: %s", eventType, eventGroup));
                                    break;
                            }
                            break;
                        case PixelBRData.EVENT_GROUP_SUGGEST:
                            switch (eventType) {
                                case PixelBRData.EVENT_ETYPE_SUBMIT: 
                                    ++pixelCountRecord.suggestEventCount; 
                                    maxPixelCountRecord.suggestEventCount = Math.max(maxPixelCountRecord.suggestEventCount, pixelCountRecord.suggestEventCount);
                                    break;
                                case PixelBRData.EVENT_ETYPE_CLICK: 
                                    ++pixelCountRecord.searchEventCount; 
                                    maxPixelCountRecord.searchEventCount = Math.max(maxPixelCountRecord.searchEventCount, pixelCountRecord.searchEventCount);
                                    break;
                                default:
                                    MessageLogger.logError (String.format ("PixelCountCollector, unknown eventType %s in eventGroup: %s", eventType, eventGroup));
                                    break;
                            }
                            break;
                        case PixelBRData.EVENT_GROUP_WIDGET:
                            switch (eventType) {
                                case PixelBRData.EVENT_ETYPE_WIDGET_VIEW: 
                                    ++pixelCountRecord.widgetViewCount; 
                                    maxPixelCountRecord.widgetViewCount = Math.max(maxPixelCountRecord.widgetViewCount, pixelCountRecord.widgetViewCount);
                                    break;
                                case PixelBRData.EVENT_ETYPE_WIDGET_CLICK: 
                                    ++pixelCountRecord.widgetClickCount; 
                                    maxPixelCountRecord.widgetClickCount = Math.max(maxPixelCountRecord.widgetClickCount, pixelCountRecord.widgetClickCount);
                                    break;
                                
                                default:
                                    MessageLogger.logError (String.format ("PixelCountCollector, unknown eventType %s in eventGroup: %s", eventType, eventGroup));
                                    break;
                            }
                            break;
                        default:
                            MessageLogger.logError (String.format ("PixelCountCollector, unknown eventGroup: %s", eventGroup));
                            break;
                    }
                    break;
                default:
                    MessageLogger.logError (String.format ("PixelCountCollector, unknown pixelType: %s", type));
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
        if ((writer != null) && (pixelCounts.size() > 0)) {
            String headerLine;
            String logLine;

            headerLine = buildLogHeader();
            writer.write (headerLine+"\n");
            try {
                // first write out 'maxCount' record
                logLine = buildLogLine(maxPixelCountRecord);
                writer.write(logLine + "\n");

                // then individual pixelCount log records
                for (PixelCountRecord countRecord: pixelCounts) {
                    if (countRecord.isEmpty() == false) {
                        logLine = buildLogLine(countRecord);
                        writer.write (logLine + "\n");
                    }
                }
                writer.write ("\n");
                writer.flush();
            } catch (Exception e) {
                MessageLogger.logError ("Exception in writing pixelCountCollector log record, " + e.getMessage());
            }
        }
    }

     private String buildLogHeader () {
        String headerLine;
        StringBuffer sb;

        sb = new StringBuffer();
        sb.append("Time");
        // sb.append("\tType"); // pixelCountRecord.type);
        // sb.append("\tPageType"); // +pixelCountRecord.pageType);    // if type == pageview
        // sb.append("\tEventGroup"); // +pixelCountRecord.eventGroup);  // if type == event
        // sb.append("\tEventType"); // +pixelCountRecord.eventType);   // if type == event

        sb.append("\tHomepageCount"); // +pixelCountRecord.homePagecount); // updated for each pixel of given type
        sb.append("\tProductPageCount"); // +pixelCountRecord.productPageCount);
        sb.append("\tCategoryPageCount"); // +pixelCountRecord.categoryPageCount);
        sb.append("\tOtherPageCount"); //+pixelCountRecord.otherPageCount);
        sb.append("\tSearchResultPageCount"); // +pixelCountRecord.searchResultPageCount);
        sb.append("\tThematicPageCount"); // +pixelCountRecord.thematicPageCount);
        sb.append("\tContentPageCount"); // +pixelCountRecord.contentPageCount);
        sb.append("\tSearchEventCount"); // +pixelCountRecord.searchEventCount);
        sb.append("\tSuggestEventCount"); // +pixelCountRecord.suggestEventCount);
        sb.append("\tATCCount"); // +pixelCountRecord.atcCount);
        sb.append("\tWidgetViewCount"); // +pixelCountRecord.widgetViewCount);
        sb.append("\tWidgetClickCount"); // +pixelCountRecord.widgetClickCount);
        sb.append("\tWidgetATCCount"); // +pixelCountRecord.widgetATCCount);

        // count rate (per-second)
        sb.append("\tHomepageCountPS"); 
        sb.append("\tProductPageCountPS"); 
        sb.append("\tCategoryPageCountPS"); 
        sb.append("\tOtherPageCountPS");
        sb.append("\tSearchResultPageCountPS"); 
        sb.append("\tThematicPageCountPS"); 
        sb.append("\tContentPageCountPS"); 
        sb.append("\tSearchEventCountPS"); 
        sb.append("\tSuggestEventCountPS"); 
        sb.append("\tATCCountPS"); 
        sb.append("\tWidgetViewCountPS"); 
        sb.append("\tWidgetClickCountPS"); 
        sb.append("\tWidgetATCCountPS"); 

        headerLine = new String (sb);
        return headerLine;
    }

    

    // logLine format must match header format
    private String buildLogLine (PixelCountRecord countRecord) {
        String line;
        StringBuffer sb;
        SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss,SSS"); // format to match date printed by logger

        sb = new StringBuffer();
        if (countRecord.recordTime < 0) // maxCountRecord has logTime = -1
            sb.append ("Max counts");
        else
            sb.append (dateFormat.format (countRecord.recordTime));
        // sb.append("\t"+countRecord.type);
        // sb.append("\t"+countRecord.pageType);    // if type == pageview
        // sb.append("\t"+countRecord.eventGroup);  // if type == event
        // sb.append("\t"+countRecord.eventType);   // if type == event

        sb.append("\t"+countRecord.homePageCount); // updated for each pixel of given type
        sb.append("\t"+countRecord.productPageCount);
        sb.append("\t"+countRecord.categoryPageCount);
        sb.append("\t"+countRecord.otherPageCount);
        sb.append("\t"+countRecord.searchResultPageCount);
        sb.append("\t"+countRecord.thematicPageCount);
        sb.append("\t"+countRecord.contentPageCount);
        sb.append("\t"+countRecord.searchEventCount);
        sb.append("\t"+countRecord.suggestEventCount);
        sb.append("\t"+countRecord.atcCount);
        sb.append("\t"+countRecord.widgetViewCount);
        sb.append("\t"+countRecord.widgetClickCount);
        sb.append("\t"+countRecord.widgetATCCount);

        // per-second
        sb.append("\t"+ getCountsPerSecond(countRecord.homePageCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.productPageCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.categoryPageCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.otherPageCount) );
        sb.append("\t"+ getCountsPerSecond(countRecord.searchResultPageCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.thematicPageCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.contentPageCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.searchEventCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.suggestEventCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.atcCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.widgetViewCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.widgetATCCount));
        sb.append("\t"+ getCountsPerSecond(countRecord.widgetATCCount));

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

    class PixelCountRecord {
        long recordTime;    // actual time 
        String type;        // 'pageview'/'event'
        String pageType;    // if type == pageview
        String eventGroup;  // if type == event
        String eventType;   // if type == event
        int homePageCount; // updated for each pixel of given type
        int productPageCount;
        int categoryPageCount;
        int otherPageCount;
        int searchResultPageCount;
        int thematicPageCount;
        int contentPageCount;
        int searchEventCount;
        int suggestEventCount;
        int atcCount;
        int widgetViewCount;
        int widgetClickCount;
        int widgetATCCount;

        PixelCountRecord () { 
            recordTime = System.currentTimeMillis();
        }

        boolean isEmpty () {
            if ((homePageCount == 0) &&
                (productPageCount == 0) &&
                (categoryPageCount == 0) &&
                (otherPageCount == 0) &&
                (searchResultPageCount == 0) &&
                (thematicPageCount == 0) &&
                (contentPageCount == 0) &&
                (searchEventCount == 0) &&
                (suggestEventCount == 0) &&
                (atcCount == 0) &&
                (widgetViewCount == 0) &&
                (widgetClickCount == 0) &&
                (widgetATCCount == 0))
                return true;
            
            return false;
        }
    }

    class CollectorTimerThread extends Thread {
        CollectorTimerThread () {
            setName("PixelCountLog timer");
        }
        public void run () {
            do {
                try {
                    synchronized (collectorMutex) {
                        pixelCountRecord = new PixelCountRecord ();
                        pixelCounts.add (pixelCountRecord);
                    }
                    sleep (COUNT_COLLECTION_TIMEBRACKET * 1000); // millisec
                } catch (InterruptedException ie) {
                    MessageLogger.logDebug( "EventCountCollector interrupted");
                    break;
                }
            } while (isInterrupted() == false);
        }
    }

}
