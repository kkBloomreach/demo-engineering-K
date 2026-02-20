// generate pixelLogs
package com.bloomreach.analyticssimulator;

// Change log (since 2.0.0.0)
// 2.0.0.1 - added multiple quantities in add-to-cart, minimum 1
// 2.0.0.2 - added sku in product and atc pixels
// 2.0.0.3 - set 'basket' string in conversion pixel (previously it was taken as-is from conversion pixel template)
// 2.1.0.0 - added simulationStats
// 2.2.0.0 - changed segmentation ID and budget/luxury segment id's
// 2.3.0.0 - added zero result query
// 2.4.0.0 - added APIlogs
// 2.4.1.0 - added -u in command line to set 'maxUsers' to use in simulation; helps debugging
// 2.4.2.0 - moved to different package structure
// 2.5.0.0 - introduced simulatorConfig 
// 2.5.1.0 - add pacificsupply config (without views)
// 3.0.0.0 - Use analyticsDataGenerator's output
// 3.1.0.0 - Use log4j for logging
// 3.1.1.0 - Added customer_profile in apiLogs via request_data_json
// 3.1.2.0 - Added URLEncode for category and product names (api and pixelLogs). Changed log filenames to 4xxxxx

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.net.URL;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.SequenceFile.Writer.Option;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.proto.PwfPixelLog;
import com.bloomreach.proto.MobileApi.ApiLog;
import com.bloomreach.proto.PWfMobileApiLog;

import com.bloomreach.analyticssimulator.templates.*;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;
import com.bloomreach.analyticssimulator.build.pixellog.*;
import com.bloomreach.analyticssimulator.stats.*;

public class Simulator {

    private final static String VERSION = "3.1.2.0";

    // NOTE: within "ROOT" dir (name = 'data'), subfolders are expected to have a predefined substructure
    // "ROOT" is provided as an argument to the application
    //      <root>/data/source -> {processedFeed.tsv, ...}
    
    // from the total UID's, the "max" user count  we expect will visit the site on a daily basis 
    private final static float MAX_VISITOR_FACTOR = 0.6f; // 60%

    // how many times a single visitor might visit the site in a single day
    // Should be < 6 due to algo used below
    private final static int MAX_VISITS_PER_UID_PER_DAY = 5;

    // different session types currently supported
    // Note: In RTCS world, ALL pixels have segment info. 
    // All queries are 'segmented' queries. 
    // Note about 'zero' query. We simulate 1% of total traffic to be 'zero result' query 
    // All other sessions are evenly distributed in the remaining 99% of total queries 
    // (see algo note below)
    private final static int SEGMENT_QUERY_SEARCH_SESSION = 0;
    private final static int SEGMENT_CATEGORY_SESSION = 1; 
    private final static int SEGMENT_SEARCH_WITH_SEARCH_REFINEMENT_SESSION  = 2; 
    private final static int SEGMENT_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION  = 3;
    private final static int SEGMENT_CATEGORY_WITH_SEARCH_REFINEMENT_SESSION  = 4;
    private final static int SEGMENT_CATEGORY_WITH_CATEGORY_REFINEMENT_SESSION  = 5;
    private final static int ZERO_RESULT_QUERY_SESSION = 6;
    private final static int ZERO_RESULT_QUERY_WITH_SEARCH_REFINEMENT_SESSION = 7;
    private final static int ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION = 8;
    private final static int MAX_SESSION_TYPES = 9;

    // number of pixelLogs written to a single part- file
    private final static int MAX_PIXELLOGS_PER_FILE = 100;
    private final static int MAX_APILOGS_PER_FILE = 100;

    // processedFeed
    ProcessedFeed processedFeed;

    // OrderId generator
    OrderIdGenerator orderIdGenerator;

    // UID list
    UidToSegmentMap uidToSegmentMap;

    // refUrl pool
    RefUrlPool refUrlPool;

    // map of segment to {pid} list
    SegmentQueryToPidMap segmentQueryToPidMap;

    // segmentCategory to {pid} list
    SegmentCategoryToPidMap segmentCategoryToPidMap;

    // refinedJourneyMap
    SegmentRefinedJourneyMap refinedJourneyMap;

    // zeroResultQuery map
    ZeroResultQueryMap zeroResultQueryMap;

    // templates for product / atc / conversion pixels
    PixelTemplates pixelTemplates;

    // templates for search, category APIlogs
    ApiTemplates apiTemplates;

    // iessionSimulator
    SessionSimulator sessionSimulator;

    // simulationStats
    SimulationStats simulationStats;

    // for reporting purpose during dev/debug
    int totalQuerySessionCount = 0;
    int totalCategorySessionCount = 0;
    int totalSearch2SearchCount = 0;
    int totalSearch2CategoryCount = 0;
    int totalCategory2SearchCount = 0;
    int totalCategory2CategoryCount = 0;
    int totalZeroResultQueryCount = 0;
    int totalZeroResult2SearchQueryCount = 0;
    int totalZeroResult2CategoryQueryCount  = 0;

    public Simulator () {
    }

    public static void main (String[] args) {

        SimulatorCommandLine commandLine;
        Simulator simulator;
        MessageLogger messageLogger;

        System.out.println ("Analytics Data Simulator, version: " + VERSION);

        commandLine = new SimulatorCommandLine ();
        if (commandLine.parse (args) == false) {
            // help message already shown
            System.exit (-1);
        }

        try {
            messageLogger = new MessageLogger ();
            messageLogger.init (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("Exception in creating messageLogger: " + e.getMessage ());
            // don't exit even if this fails
        }

        simulator = new Simulator ();
        try {
            // read simulation data - processedFeed, templates, ...
            simulator.init (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in simulator initialization: " + e);
            System.exit (-1);
        }

        try {
            simulator.doSimulate (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in simulator : " + e);
            System.exit (-1);
        }

        System.exit (0);
    }


    private void init (SimulatorCommandLine commandLine) throws Exception {
        String rootDirPath;
        String accountName;
        SimulatorConfig simulatorConfig;
        File configDir;

        rootDirPath = commandLine.getRootSourceDataDir ();

        // init account-specific configs 
        initSimulatorConfigs (rootDirPath);

        // init simulator configs independent of specific account
        // loads config.json file and creates a static object in SimulatorConfig class
        accountName = commandLine.getAccountName ().trim();
        simulatorConfig = new SimulatorConfig ();
        configDir = new File (rootDirPath, SimulatorConstants.ACCOUNT_CONFIG_DIR_PATH);
        if (simulatorConfig.load (configDir.getPath(), accountName) == false) {
            throw new Exception ("Cannot find simulatorConfig for account: " + accountName);
        }
        // use param values in simulatorConfig to load account-specific values (eg, home-page-url)
        initAccountConfigs (rootDirPath);


        // prepare sessionSimulator
        sessionSimulator = new SessionSimulator ();
        sessionSimulator.setProcessedFeed (this.processedFeed);
        sessionSimulator.setOrderIdGenerator (this.orderIdGenerator);
        sessionSimulator.setSegmentQueryToPidMap (this.segmentQueryToPidMap);
        sessionSimulator.setSegmentCategoryToPidMap (this.segmentCategoryToPidMap);
        sessionSimulator.setPixelTemplates (this.pixelTemplates);
        sessionSimulator.setRefUrlPool (this.refUrlPool);
        sessionSimulator.setSegmentRefinedJourneyMap (this.refinedJourneyMap);
        sessionSimulator.setZeroResultQueryMap (this.zeroResultQueryMap);
        sessionSimulator.setApiTemplates (this.apiTemplates);
        sessionSimulator.setSimulationStats (this.simulationStats);

        sessionSimulator.init (rootDirPath); // init sessionSimulator
    }

    private void doSimulate (SimulatorCommandLine commandLine) throws Exception {
        String rootDirPath;
        int maxDaysToSimulate;
        int startAt;
        int maxUsers;
        int totalUidCount;

        rootDirPath = commandLine.getRootSourceDataDir ();
        maxDaysToSimulate = commandLine.getMaxDays ();
        startAt = commandLine.getStartAt ();
        maxUsers = commandLine.getMaxUsers ();  // see if maxUser count set via command arg
        if (maxUsers < 0) {
            // given 'total' uids, assume X% of them visit the site on a given day
            totalUidCount = uidToSegmentMap.getSimulatedUidCount ();
            maxUsers = (int) (totalUidCount * MAX_VISITOR_FACTOR);
        }
        MessageLogger.logDebug ("maxUsers = " + maxUsers);

        for (int day = startAt; day < (startAt + maxDaysToSimulate); day++) {
            ArrayList <PixelLog> simulatedPixelLogs;
            ArrayList <ApiLog> simulatedApiLogs;
            String outputDirPath;
            File outputDirFile;
            DailyStats dailyStats;
            String logDirName;

            dailyStats = simulationStats.addNewDay (day);    // add new day in SimulationStats

            // for search and category pages, trigger corresponding APIcalls
            // before the pageview pixel is triggered
            simulatedPixelLogs = new ArrayList <PixelLog> ();
            simulatedApiLogs = new ArrayList <ApiLog> ();
            simulatePixelAndApiLogsForOneDay (day, maxUsers, simulatedPixelLogs, simulatedApiLogs, dailyStats);

            // write collected pixel logs to output in seqfile format.
            // output log paths: 
            //   pixelLogs <output>/accountname/pixelLogs/<day>
            //   apiLogs <output>/accountname/apiLogs/<day>
            outputDirPath = commandLine.getOutputDir ();
            logDirName = SimulatorConstants.OUTPUT_PIXELLOG_DIR;
            logDirName = logDirName.replace ("$ACCOUNTNAME", commandLine.getAccountName ());
            outputDirFile = new File (outputDirPath, logDirName + day);
            MessageLogger.logDebug ("output dir path = " + outputDirFile.getPath());
            outputDirFile.mkdirs ();
            writePixelLogsToOutput (simulatedPixelLogs, outputDirFile);

            // write collected api logs to output in seqfile format
            logDirName = SimulatorConstants.OUTPUT_APILOG_DIR;
            logDirName = logDirName.replace ("$ACCOUNTNAME", commandLine.getAccountName ());
            outputDirFile = new File (outputDirPath, logDirName + day);
            MessageLogger.logDebug ("output dir path = " + outputDirFile.getPath());
            outputDirFile.mkdirs ();
            writeApiLogsToOutput (simulatedApiLogs, outputDirFile);
        }

        MessageLogger.logInfo ("Total counts: " +
                            "SearchSessionCount = " + totalQuerySessionCount + 
                            ", CategorySessionCount = " + totalCategorySessionCount +
                            ", Search2SearchSessionCount = " + totalSearch2SearchCount +
                            ", Search2CategorySessionCount = " + totalSearch2CategoryCount +
                            ", Category2SearchSessionCount = " + totalCategory2SearchCount +
                            ", Category2CategorySessionCount = " + totalCategory2CategoryCount +
                            ", ZeroResultQuerySessionCount = " + totalZeroResultQueryCount +
                            ", ZeroResult2SearchQuerySessionCount = " + totalZeroResult2SearchQueryCount +
                            ", ZeroResult2CategorySessionCount = " + totalZeroResult2CategoryQueryCount);

        // print simulationStats
        // <output>/<account>/stats.tsv
        {
            String statsDirPath;
            File statsDir;
            File statsFile;

            statsDirPath = SimulatorConstants.OUTPUT_SIMULATION_STATS_DIR;
            statsDirPath = statsDirPath.replace ("$ACCOUNTNAME", commandLine.getAccountName ());
            statsDir = new File (commandLine.getOutputDir(), statsDirPath);
            statsDir.mkdirs ();
            statsFile = new File (statsDir, SimulatorConstants.OUTPUT_SIMULATION_STATS_FILENAME);
            simulationStats.logSimulationStats (statsFile.getPath());
        }
    }


    // dayNum goes from 0 to 30, representing d1 -> d31
    private void simulatePixelAndApiLogsForOneDay (int dayNum, int maxUsers, ArrayList<PixelLog> collectedPixelLogs, 
                                                   ArrayList<ApiLog> collectedApiLogs, DailyStats dailyStats) throws Exception {
        GregorianCalendar calendar;

        // 8am on July 2020 {dayNum: 0, 1, ..}, 8am start
        // 'day' value in the for-loop above starts from 0 onwards
        calendar = new GregorianCalendar (2022, 03, dayNum+1, 07, 00);

        // update orderIdgenerator for each day
        orderIdGenerator.setDate (calendar.get (GregorianCalendar.YEAR),
                                  calendar.get (GregorianCalendar.MONTH)+1,
                                  calendar.get (GregorianCalendar.DAY_OF_MONTH));

        for (int visitor = 0; visitor < maxUsers; visitor++) {
            UidToSegmentRecord selectedUidRecord;
            int numVisitsInDay;

            // get a randomly selected user from the total UID set
            selectedUidRecord = uidToSegmentMap.selectUidAtRandom ();

            // a single visitor may visit the site multiple times in a single day 
            numVisitsInDay = (int) (Math.random () * MAX_VISITS_PER_UID_PER_DAY) + 4; // +4 to ensure at least 2~3 visits occurs

            for (int numVisit = 1; numVisit < numVisitsInDay; numVisit++) {
                int sessionType;
                SessionStats newSessionStat;

                MessageLogger.logDebug ("visitor = " + visitor + ", numVisit = " + numVisit);

                calendar.add (GregorianCalendar.HOUR, numVisit + 2);

                // session type. In order to ensure zero-result-query is 1% max, we generate a random number
                // between 0 to 99. If value == 99 -> zero-result-query-session (ie, 1%). If not 99, mod the randomNumber
                // to select one of the normal (non-zero) sessions 
                sessionType = (int) (Math.random () * 100); // values include 0 to 99
                if (sessionType == 97)
                    sessionType = ZERO_RESULT_QUERY_SESSION;
                else if (sessionType == 98)
                    sessionType = ZERO_RESULT_QUERY_WITH_SEARCH_REFINEMENT_SESSION;
                else if (sessionType == 99)
                    sessionType = ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION;
                else
                    sessionType = sessionType % (MAX_SESSION_TYPES - 3); // subtract 3 for the zero-query-sessions

                // add sessionInfo to dailyStats
                newSessionStat = dailyStats.addSessionStat (sessionType, selectedUidRecord.getView(), 
                                                            selectedUidRecord.getSegment(), selectedUidRecord.getUid());

                switch (sessionType) {
                    case SEGMENT_QUERY_SEARCH_SESSION:
                        MessageLogger.logInfo ("Construct search query session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructSearchSession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalQuerySessionCount++; 
                        break;
                    case SEGMENT_CATEGORY_SESSION: 
                        MessageLogger.logInfo ("Construct category query session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructCategorySession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalCategorySessionCount++; 
                        break;
                    case SEGMENT_SEARCH_WITH_SEARCH_REFINEMENT_SESSION: 
                        MessageLogger.logInfo ("Construct searchquery + searchquery refined session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructSearch2SearchRefinedSession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalSearch2SearchCount++; 
                        break;
                    case SEGMENT_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION:
                        MessageLogger.logInfo ("Construct searchquery + category refined session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructSearch2CategoryRefinedSession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalSearch2CategoryCount++; 
                        break;
                    case SEGMENT_CATEGORY_WITH_SEARCH_REFINEMENT_SESSION:
                        MessageLogger.logInfo ("Construct category + searchquery refined session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructCategory2SearchRefinedSession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalCategory2SearchCount++; 
                        break;
                    case SEGMENT_CATEGORY_WITH_CATEGORY_REFINEMENT_SESSION:
                        MessageLogger.logInfo ("Construct category + category refined session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructCategory2CategoryRefinedSession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalCategory2CategoryCount++; 
                        break;
                    case ZERO_RESULT_QUERY_SESSION:
                        MessageLogger.logInfo ("Construct zeroQuery session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructZeroResultQuerySession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalZeroResultQueryCount++; 
                        break;
                    case ZERO_RESULT_QUERY_WITH_SEARCH_REFINEMENT_SESSION:
                        MessageLogger.logInfo ("Construct zeroQuery + search refined session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructZeroResult2SearchRefinedSession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalZeroResult2SearchQueryCount++; 
                        break;
                    case ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION:
                        MessageLogger.logInfo ("Construct zeroQuery + category refined session, segment = " + selectedUidRecord.getSegment()); 
                        sessionSimulator.constructZeroResult2CategoryRefinedSession (selectedUidRecord, collectedPixelLogs, collectedApiLogs, calendar, newSessionStat); 
                        totalZeroResult2CategoryQueryCount++; 
                        break;
                    default: MessageLogger.logInfo ("ERROR Unknown session type: " + sessionType); continue;
                }
            }
        }
    }

    // write a "MAX" number of pixelLogs to a single output file. 
    // Therefore, multiple output files MAY get generated to write the entire set of simulated pixelLogs
    private void writePixelLogsToOutput (ArrayList<PixelLog> simulatedPixelLogs, File outputDirFile) throws Exception {
        for (int count = 0, filenameCounter = 0; count < simulatedPixelLogs.size (); count = count + MAX_PIXELLOGS_PER_FILE, filenameCounter++) {
            File outputFile;
            int remainingCount;

            // make sure filenames start above 40000 to avoid collision with translated part files
            outputFile = new File (outputDirFile, "part-" + (40000 + filenameCounter));
            MessageLogger.logDebug ("outputFile: " + outputFile.getPath());
            remainingCount = Math.min (MAX_PIXELLOGS_PER_FILE, simulatedPixelLogs.size () - count);
            writePixellogOutputFile (simulatedPixelLogs, count, remainingCount, outputFile.getPath());
        }
    }

    private void writePixellogOutputFile (ArrayList<PixelLog> clonedPixelLogsList, int startIndx, int remainingCount, String outputPath) throws Exception {

        Configuration configuration = new Configuration ();
        Writer.Option filePath = Writer.file (new Path (outputPath));
        Writer.Option keyClass = Writer.keyClass (Text.class);
        Writer.Option valueClass = Writer.valueClass (PwfPixelLog.class);
        Writer writer = SequenceFile.createWriter (configuration,
                                                    filePath,
                                                    keyClass,
                                                    valueClass);

        Text key = new Text (SimulatorConfig.getConfigParam ("HOMEPAGE_TITLE").toLowerCase());
        for (int i = startIndx; i < (startIndx + remainingCount); i++) {
            PixelLog pixelLog;

            pixelLog = clonedPixelLogsList.get (i);
            writer.append (key, new PwfPixelLog (pixelLog));
        }

        writer.hflush ();
        writer.close ();
    }

    // write a "MAX" number of apiLogs to a single output file. 
    // Therefore, multiple output files MAY get generated to write the entire set of simulated apiLogs
    private void writeApiLogsToOutput (ArrayList<ApiLog> simulatedApiLogs, File outputDirFile) throws Exception {
        for (int count = 0, filenameCounter = 0; count < simulatedApiLogs.size (); count = count + MAX_APILOGS_PER_FILE, filenameCounter++) {
            File outputFile;
            int remainingCount;

            // make sure filenames start above 40000 to avoid collision with translated part files
            outputFile = new File (outputDirFile, "part-" + (40000 + filenameCounter));
            MessageLogger.logDebug ("outputFile: " + outputFile.getPath());
            remainingCount = Math.min (MAX_APILOGS_PER_FILE, simulatedApiLogs.size () - count);
            writeApilogOutputFile (simulatedApiLogs, count, remainingCount, outputFile.getPath());
        }
    }

    private void writeApilogOutputFile (ArrayList<ApiLog> clonedApiLogsList, int startIndx, int remainingCount, String outputPath) throws Exception {

        Configuration configuration = new Configuration ();
        Writer.Option filePath = Writer.file (new Path (outputPath));
        Writer.Option keyClass = Writer.keyClass (Text.class);
        Writer.Option valueClass = Writer.valueClass (PWfMobileApiLog.class);
        Writer writer = SequenceFile.createWriter (configuration,
                                                    filePath,
                                                    keyClass,
                                                    valueClass);

        Text key = new Text (SimulatorConfig.getConfigParam ("HOMEPAGE_TITLE").toLowerCase());
        for (int i = startIndx; i < (startIndx + remainingCount); i++) {
            ApiLog apiLog;

            apiLog = clonedApiLogsList.get (i);
            writer.append (key, new PWfMobileApiLog (apiLog));
        }

        writer.hflush ();
        writer.close ();
    }

    // Init simulator's own configs, independent of which account it is,
    private void initSimulatorConfigs (String rootDirPath) throws Exception {

        // pixel templates. As needed, pixelLog.builder objects are
        // generated from the templates
        pixelTemplates = new PixelTemplates ();
        try {
            File templateDir;
            templateDir = new File (rootDirPath, SimulatorConstants.PIXEL_TEMPLATE_DIR_PATH);
            pixelTemplates.setTemplatesDir (templateDir.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("pixelTemplate exception: " + e.getMessage ());
            pixelTemplates = null;
        }

        // apiLog templates. As needed, apiLog.builder objects are
        // generated from the templates
        apiTemplates = new ApiTemplates ();
        try {
            File templateDir;
            templateDir = new File (rootDirPath, SimulatorConstants.API_TEMPLATE_DIR_PATH);
            apiTemplates.setTemplatesDir (templateDir.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("apiTemplate exception: " + e.getMessage ());
            apiTemplates = null;
        }

        // OrderId generator - updated for each day
        orderIdGenerator = new OrderIdGenerator ();
    }

    // use simulatorConfig params to load account-specific simulator objects
    private void initAccountConfigs (String rootDirPath) throws Exception {
        String paramValue;

        // processedFeed - needed to pick up product's price for conversion pixel
        processedFeed = new ProcessedJsonlFeed ();
        try {
            File processedFeedFile;
            paramValue = SimulatorConfig.getConfigParam ("PREPROCESSED_JSONL_FEED_PATH");
            processedFeedFile = new File (rootDirPath, paramValue);
            processedFeed.load (processedFeedFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("ProcessedFeed exception: " + e.getMessage ());
            processedFeed = null;
        }
    
        //uid to segment map
        uidToSegmentMap = new UidToSegmentMap ();
        try {
            File uidToSegmentMapFile;
            paramValue = SimulatorConfig.getConfigParam ("SIMULATED_UID_TO_SEGMENT_MAP_PATH");
            uidToSegmentMapFile = new File (rootDirPath, paramValue);
            uidToSegmentMap.doLoad (uidToSegmentMapFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("UidToSegmentMap exception: " + e.getMessage ());
            uidToSegmentMap = null;
        }

        // segment to {pid}* map
        segmentQueryToPidMap = new SegmentQueryToPidMap ();
        try {
            File segmentQueryToPidMapFile;
            paramValue = SimulatorConfig.getConfigParam ("SEGMENTQUERY_TO_PID_MAP_PATH");
            segmentQueryToPidMapFile = new File (rootDirPath, paramValue);
            segmentQueryToPidMap.setProcessedFeed (processedFeed);   // must set it before .load
            segmentQueryToPidMap.doLoad (segmentQueryToPidMapFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("SegmentQueryToPidMap exception: " + e.getMessage ());
            segmentQueryToPidMap = null;
        }

        // segmentCategory to {pid}* map
        segmentCategoryToPidMap = new SegmentCategoryToPidMap ();
        try {
            File segmentCategoryToPidMapFile;
            paramValue = SimulatorConfig.getConfigParam ("SEGMENTCATEGORY_TO_PID_MAP_PATH");
            segmentCategoryToPidMapFile = new File (rootDirPath, paramValue);
            segmentCategoryToPidMap.setProcessedFeed (processedFeed);   // must set it before .load
            segmentCategoryToPidMap.doLoad (segmentCategoryToPidMapFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("SegmentCategoryToPidMap exception: " + e.getMessage ());
            segmentCategoryToPidMap = null;
        }

        // refUrl pool
        refUrlPool = new RefUrlPool ();
        try {
            File refUrlPoolFile;
            paramValue = SimulatorConfig.getConfigParam ("REFURL_POOL_PATH");
            refUrlPoolFile = new File (rootDirPath, paramValue);
            refUrlPool.setProcessedFeed (processedFeed);   // DO we need this ??? 
            refUrlPool.doLoad (refUrlPoolFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("refUrlPool exception: " + e.getMessage ());
            refUrlPool = null;
        }

        // refinedJourny map
        refinedJourneyMap = new SegmentRefinedJourneyMap ();
        try {
            File refinedJourneyMapFile;
            paramValue = SimulatorConfig.getConfigParam ("REFINED_JOURNEY_MAP_PATH");
            refinedJourneyMapFile = new File (rootDirPath, paramValue);
            refinedJourneyMap.setProcessedFeed (processedFeed);   // must set it before .load
            refinedJourneyMap.doLoad (refinedJourneyMapFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("SegmentRefinedJourneyMap exception: " + e.getMessage ());
            refinedJourneyMap = null;
        }

        // zeroResultQuery map
        zeroResultQueryMap = new ZeroResultQueryMap ();
        try {
            File zeroResultQueryMapFile;
            paramValue = SimulatorConfig.getConfigParam ("ZERO_RESULT_QUERY_MAP_PATH");
            zeroResultQueryMapFile = new File (rootDirPath, paramValue);
            zeroResultQueryMap.setProcessedFeed (processedFeed);   // must set it before .load
            zeroResultQueryMap.doLoad (zeroResultQueryMapFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("ZeroResultQueryMap exception: " + e.getMessage ());
            zeroResultQueryMap = null;
        }

        // simulationStats
        {
            String[] rtcsSegments;

            simulationStats = new SimulationStats ();
            simulationStats.setVersion(VERSION);
            simulationStats.setUidToSegmentMap (uidToSegmentMap);
            simulationStats.setSegmentQueryToPidMap (segmentQueryToPidMap);
            simulationStats.setSegmentCategoryToPidMap (segmentCategoryToPidMap);
            rtcsSegments = SimulatorConfig.getRTCSSegmentNames ();
            if (rtcsSegments != null) {
                simulationStats.setSegments (rtcsSegments);
            }
        }
    }
}

/**
{
Class klass = com.bloomreach.proto.Aggregation.PixelLog.Builder.class;
URL location = klass.getResource ('/' + klass.getName().replace ('.', '/') + ".class");
System.out.println ("***klass: GeneratedMessage" + ", location = " + location.toString());
}
**/
