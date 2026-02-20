// generate pixelLogs for different sessions
// eg, search, category, search-to-search refined, ...
package com.bloomreach.analyticssimulator;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.proto.MobileApi.ApiLog;

import com.bloomreach.analyticssimulator.templates.*;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;
import com.bloomreach.analyticssimulator.build.pixellog.*;
import com.bloomreach.analyticssimulator.stats.*;
import com.bloomreach.analyticssimulator.build.apilog.*;

public class SessionSimulator {

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

    // templates for search, category api templates
    ApiTemplates apiTemplates;

    // orderId generator
    OrderIdGenerator orderIdGenerator;

    // processedFeed
    ProcessedFeed processedFeed;

    // Simulation stats
    SimulationStats simulationStats;

    public SessionSimulator () {
    }

    public void setProcessedFeed (ProcessedFeed processedFeed) {
        this.processedFeed = processedFeed;
    }

    public void setOrderIdGenerator (OrderIdGenerator generator) {
        this.orderIdGenerator = generator;
    }

    public void setSimulationStats (SimulationStats simulationStats) {
        this.simulationStats = simulationStats;
    }

    public void setSegmentQueryToPidMap (SegmentQueryToPidMap segmentQueryToPidMap) {
        this.segmentQueryToPidMap = segmentQueryToPidMap;
    }

    public void setSegmentCategoryToPidMap (SegmentCategoryToPidMap segmentCategoryToPidMap) {
        this.segmentCategoryToPidMap = segmentCategoryToPidMap;
    }

    public void setZeroResultQueryMap (ZeroResultQueryMap zeroResultQueryMap) {
        this.zeroResultQueryMap = zeroResultQueryMap;
    }

    public void setPixelTemplates (PixelTemplates pixelTemplates) {
        this.pixelTemplates = pixelTemplates;
    }

    public void setRefUrlPool (RefUrlPool refUrlPool) {
        this.refUrlPool = refUrlPool;
    }

    public void setSegmentRefinedJourneyMap (SegmentRefinedJourneyMap refinedJourneyMap) {
        this.refinedJourneyMap = refinedJourneyMap;
    }

    public void setApiTemplates (ApiTemplates apiTemplates) {
        this.apiTemplates = apiTemplates;
    }

    public boolean init (String rootDirPath) {
        // place holder
        return true;
    }

    // useSegmentQuery = true => use segment query <-> pid map to select specific pid
    public void constructSearchSession (UidToSegmentRecord selectedUidRecord, 
                                        ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs, 
                                        GregorianCalendar calendar, SessionStats sessionStats) throws Exception {
        String selectedPid;
        String startUrl;    // session-start-url
        SegmentQueryToPidRecord selectedQueryRecord;

        // startURL == initial session start url
        startUrl = refUrlPool.selectRefUrlAtRandom ();

        // Select a query (from list of predefined queries)
        selectedQueryRecord = segmentQueryToPidMap.selectPrimaryQueryAtRandom (selectedUidRecord.getView(), selectedUidRecord.getSegment());
        if (selectedQueryRecord == null) {
            MessageLogger.logError ("No consistent query available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        // Select a pid. 
        // Make sure the selected PID is associated with the 'segment' that the selected UID has
        selectedPid = segmentQueryToPidMap.selectPidAtRandom (selectedQueryRecord);

        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        // update sessionStat
        sessionStats.setSelectedPid (selectedPid);
        sessionStats.setPrimaryQuery (selectedQueryRecord.getQuery ());

        // for each such visit, generate product+atc+conversion pixelLog for a given PID
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulateSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                               startUrl, selectedQueryRecord, selectedPid);
    }

    // useSegmentQuery param is really not needed for this method. We keep it just to be
    // consistent with all other 'construct' method parameters
    public void constructCategorySession (UidToSegmentRecord selectedUidRecord, 
                                          ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs,
                                          GregorianCalendar calendar, SessionStats sessionStats) throws Exception {
        SegmentCategoryToPidRecord selectedCategoryRecord;
        String selectedPid;
        String startUrl;    // session-start-url

        // generate pixel logs: category page, product page, atc, conversion
        // startURL == initial session start url
        startUrl = refUrlPool.selectRefUrlAtRandom ();

        // select a category (from list of predefined categories)
        selectedCategoryRecord = segmentCategoryToPidMap.selectPrimaryCategoryAtRandom (selectedUidRecord.getView(),selectedUidRecord.getSegment());
        if (selectedCategoryRecord == null) {
            MessageLogger.logError ("No consistent category available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        // select a pid
        selectedPid = segmentCategoryToPidMap.selectPidAtRandom (selectedCategoryRecord);
        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        // update sessionStat
        sessionStats.setSelectedPid (selectedPid);
        sessionStats.setPrimaryQuery (selectedCategoryRecord.getCatName ());

        // for each such visit, generate category+product+atc+conversion pixelLog for a given cat, PID
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulateCategorySession (selectedUidRecord, 
                                 calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                 startUrl, selectedCategoryRecord, selectedPid);
    }

    public void constructZeroResultQuerySession (UidToSegmentRecord selectedUidRecord, 
                                               ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs,
                                               GregorianCalendar calendar, SessionStats sessionStats) throws Exception {
        String startUrl;    // session-start-url
        ZeroResultQueryRecord selectedZeroResultQueryRecord;

        // startURL == initial session start url
        startUrl = refUrlPool.selectRefUrlAtRandom ();

        // Select a query (from list of predefined queries)
        selectedZeroResultQueryRecord = zeroResultQueryMap.selectZeroResultQueryAtRandom ();

        // update sessionStat
        sessionStats.setSelectedPid ("-");
        sessionStats.setPrimaryQuery (selectedZeroResultQueryRecord.getQuery ());

        // for each such visit, generate event, search-result-page pixelLog. For zero-results,
        // simulate only 'partial' session (no ATC, Conversion)
        // PixelLogs are collected in the 'pixelLogs' arrayList
        // selectedQueryRecord is null (ie, no products are in ApiLog response section)
        simulatePartialSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                      startUrl, selectedZeroResultQueryRecord.getQuery(), null);
    }

    // session with S2S refinement
    // Based on engr comment, in S2S, the S2 needs to start immediately after search-result-page of S1
    // Also, S2.ref_url == S1.url
    // followed by a refined-query-search-session
    public void constructSearch2SearchRefinedSession (UidToSegmentRecord selectedUidRecord, 
                                                      ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs,
                                                      GregorianCalendar calendar, SessionStats sessionStats) throws Exception {

        String selectedPid;
        String startUrl1;    // session-start-url
        String startUrl2;    // session-start-url
        String selectedQueryStr;
        SegmentQueryToPidRecord selectedQueryRecord1;
        SegmentQueryToPidRecord selectedQueryRecord2;
        String refinedQuery;

        // step 1: primary search session
        startUrl1 = refUrlPool.selectRefUrlAtRandom ();

        // Select a query (from list of predefined queries)
        selectedQueryRecord1 = segmentQueryToPidMap.selectPrimaryQueryAtRandom (selectedUidRecord.getView(),selectedUidRecord.getSegment());
        if (selectedQueryRecord1 == null) {
            MessageLogger.logError ("No consistent query available: uid = " + selectedUidRecord.getUid () +
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        selectedQueryStr = selectedQueryRecord1.getQuery ();

        // Select a pid. 
        // Make sure the selected PID is associated with the 'segment' that the selected UID has
        selectedPid = segmentQueryToPidMap.selectPidAtRandom (selectedQueryRecord1);

        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        // for each such visit, generate only search-event->search-result-page (NO product+atc+conversion pixelLog)
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulatePartialSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                      startUrl1, selectedQueryStr, selectedQueryRecord1);


        // step 2: refined search session
        // startUrl2 == first query's search-result-page url 
        startUrl2 = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryStr); // ref_url for refined search

        refinedQuery = refinedJourneyMap.selectRefinedQueryAtRandom ("s2s", selectedQueryRecord1.getQuery(),
                                                                     selectedUidRecord.getView (), 
                                                                     selectedUidRecord.getSegment());
        if (refinedQuery == null) {
            MessageLogger.logError ("no refinedJourney for s2s, primaryQuery = " + selectedQueryRecord1.getQuery ());
            return;
        }

        // get segmentQueryRecord for the refinedJourneyId
        selectedQueryRecord2 = segmentQueryToPidMap.getQueryRecord (refinedQuery);
        if (selectedQueryRecord2 == null) {
            MessageLogger.logError ("no segmentQueryRecord for s2s, refined = " + refinedQuery);
            return;
        }

        // are segments consistent in primary, secondary ? debugging...
        MessageLogger.logDebug ("userSegment, primary segment, refined segment: " +
                            selectedUidRecord.getSegment () + ", " + 
                            selectedQueryRecord1.getSegment () + ", " + 
                            selectedQueryRecord2.getSegment ());

        // use secondQuery
        selectedQueryStr = selectedQueryRecord2.getQuery ();

        // Select a pid. 
        selectedPid = segmentQueryToPidMap.selectPidAtRandom (selectedQueryRecord2);

        // update sessionStat
        sessionStats.setSelectedPid (selectedPid);
        sessionStats.setPrimaryQuery (selectedQueryRecord1.getQuery ());
        sessionStats.setRefinedQuery (selectedQueryRecord2.getQuery ());


        // for second search, generate entire journey (search-event->search-result-page-> product+atc+conversion) pixelLog for a given PID
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulateSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                               startUrl2, selectedQueryRecord2, selectedPid);

        // summary
        MessageLogger.logDebug ("S2S: query1 = " + selectedQueryRecord1.getQuery () + 
                                       ", \n\tquery1 refUrl = " + startUrl1 +
                                       ", \n\tquery1 endUrl = " + startUrl2 +
                                       ", \n\tquery2 = " + selectedQueryRecord2.getQuery () +
                                       ", \n\tquery2 refUrl = " + startUrl2 +
                                       ", \n\tquery2 endUrl = "  + BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryStr)); 
    }


    // session with S2C refinement
    // Currently this is supported ONLY for segmentQuery (not segmentedQuery)
    // Based on engr comment, in S2C, the C needs to start immediately after search-result-page of S1
    // Also, C.ref_url == S1.url
    // *** TO BE DONE -- UseSegmentQuery even for refinedSession ***
    public void constructSearch2CategoryRefinedSession (UidToSegmentRecord selectedUidRecord, 
                                                        ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs,
                                                        GregorianCalendar calendar, SessionStats sessionStats) throws Exception {

        String selectedPid;
        String startUrl1;    // session-start-url
        String startUrl2;    // session-start-url
        String selectedQueryStr;
        SegmentQueryToPidRecord selectedQueryRecord;
        SegmentCategoryToPidRecord selectedCategoryRecord;

        // step 1: primary search session
        // for each such visit, generate only search-event->search-result-page (NO product+atc+conversion pixelLog)
        // PixelLogs are collected in the 'pixelLogs' arrayList
        startUrl1 = refUrlPool.selectRefUrlAtRandom ();

        // Select a query (from list of predefined queries)
        selectedQueryRecord = segmentQueryToPidMap.selectPrimaryQueryAtRandom (selectedUidRecord.getView(),selectedUidRecord.getSegment());
        if (selectedQueryRecord == null) {
            MessageLogger.logError ("No consistent query available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        selectedQueryStr = selectedQueryRecord.getQuery ();

        // Select a pid. 
        // Make sure the selected PID is associated with the 'segment' that the selected UID has
        selectedPid = segmentQueryToPidMap.selectPidAtRandom (selectedQueryRecord);

        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        simulatePartialSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                      startUrl1, selectedQueryStr, selectedQueryRecord);


        // step 2: refined category session
        // startUrl2 == first query's search-result-page url 
        startUrl2 = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryStr); // ref_url for refined search

        // for s2c, the 'refined category' is one of refined category records, selected at random
        // for category, we don't consider any specific view and/or segment
        selectedCategoryRecord = segmentCategoryToPidMap.getRefinedCategoryRecord (selectedUidRecord.getView(),
                                                                                   selectedUidRecord.getSegment());
        if (selectedCategoryRecord == null) {
            MessageLogger.logError ("no refinedCategoryRecord for s2c, query = " + selectedQueryStr);
            return;
        }

        // are segments consistent in primary, secondary ? debugging...
        MessageLogger.logDebug ("userSegment, primary segment, refined segment: " +
                            selectedUidRecord.getSegment () + ", " + 
                            selectedQueryRecord.getSegment () + ", " + 
                            selectedCategoryRecord.getSegment ());

        // select a pid
        selectedPid = segmentCategoryToPidMap.selectPidAtRandom (selectedCategoryRecord);
        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment ());
            return;
        }

        // update sessionStat
        sessionStats.setSelectedPid (selectedPid);
        sessionStats.setPrimaryQuery (selectedQueryRecord.getQuery ());
        sessionStats.setRefinedQuery (selectedCategoryRecord.getCatName ());

        // for each such visit, generate category+product+atc+conversion pixelLog for a given cat, PID
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulateCategorySession (selectedUidRecord, 
                                 calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                 startUrl2, selectedCategoryRecord, selectedPid);

        // summary
        MessageLogger.logDebug ("S2C: query1 = " + selectedQueryRecord.getQuery () + 
                                       ", \n\tquery1 refUrl = " + startUrl1 +
                                       ", \n\tquery1 endUrl = " + startUrl2 +
                                       ", \n\tcat = " + selectedCategoryRecord.getCatName () +
                                       ", \n\tcat2 refUrl = " + startUrl2 + 
                                       ", \n\tcat2 endUrl = "  + BuildCategoryPagePixel.getCategoryPageUrl (selectedCategoryRecord.getCatId ()) ); 
    }


    // session with C2S refinement
    // Currently this is supported ONLY for segmentQuery/segmentCategory (not segmentedQuery)
    // Based on engr comment, in C2S, the S needs to start immediately after category-page-view of C
    // Also, S.ref_url == C.url
    // *** TO BE DONE -- UseSegmentQuery even for refinedSession ***
    public void constructCategory2SearchRefinedSession (UidToSegmentRecord selectedUidRecord, 
                                                        ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs,
                                                        GregorianCalendar calendar, SessionStats sessionStats)
                                                        throws Exception {

        SegmentCategoryToPidRecord selectedCategoryRecord;
        SegmentQueryToPidRecord selectedQueryRecord;
        String selectedPid;
        String startUrl1;    // session-start-url
        String startUrl2;    // session-start-url
        String selectedQueryStr;

        // Step 1
        // generate pixel logs: category page. 
        // startURL == initial session start url
        startUrl1 = refUrlPool.selectRefUrlAtRandom ();

        // select a category (from list of predefined categories)
        selectedCategoryRecord = segmentCategoryToPidMap.selectPrimaryCategoryAtRandom (selectedUidRecord.getView(),selectedUidRecord.getSegment());
        if (selectedCategoryRecord == null) {
            MessageLogger.logError ("No consistent category available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        // select a pid
        selectedPid = segmentCategoryToPidMap.selectPidAtRandom (selectedCategoryRecord);
        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment ());
            return;
        }

        // generate only category page (no followups with product -> atc -> conversion)
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulatePartialCategorySession (selectedUidRecord, 
                                        calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                        startUrl1, selectedCategoryRecord, selectedPid);

        // Step 2 - refined search session
        // startUrl2 == category page url
        startUrl2 = BuildCategoryPagePixel.getCategoryPageUrl (selectedCategoryRecord.getCatId ()); // ref_url for next pixel

        // get segmentQueryRecord for the refinedJourneyId
        selectedQueryRecord = segmentQueryToPidMap.getRefinedQueryRecord (selectedUidRecord.getView(),
                                                                          selectedUidRecord.getSegment());
        if (selectedQueryRecord == null) {
            MessageLogger.logError ("no segmentQueryRecord for c2s, catId = " + selectedCategoryRecord.getCatId ());
            return;
        }

        // are segments consistent in primary, secondary ? debugging...
        MessageLogger.logDebug ("userSegment, primary segment, refined segment: " +
                            selectedUidRecord.getSegment () + ", " + 
                            selectedQueryRecord.getSegment () + ", " + 
                            selectedCategoryRecord.getSegment ());

        selectedQueryStr = selectedQueryRecord.getQuery ();

        // Select a pid. 
        selectedPid = segmentQueryToPidMap.selectPidAtRandom (selectedQueryRecord);

        // update sessionStat
        sessionStats.setSelectedPid (selectedPid);
        sessionStats.setPrimaryQuery (selectedCategoryRecord.getCatName ());
        sessionStats.setRefinedQuery (selectedQueryRecord.getQuery ());

        // for second search, generate entire journey (search-event->search-result-page-> product+atc+conversion) pixelLog for a given PID
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulateSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                               startUrl2, selectedQueryRecord, selectedPid);

        // summary
        MessageLogger.logDebug ("C2S: cat1 = " + selectedCategoryRecord.getCatName () + 
                                       ", \n\tcat1 refUrl = " + startUrl1 +
                                       ", \n\tcat1 endUrl = " + startUrl2 +
                                       ", \n\tquery = " + selectedQueryRecord.getQuery () +
                                       ", \n\tquery2 refUrl = " + startUrl2 +
                                       ", \n\tquery2 endUrl = "  + BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryStr)); 
    }


    // session with C2C refinement
    // Currently this is supported ONLY for segmentQuery/segmentCategory (not segmentedQuery)
    // Based on engr comment, in C2C, the C2 needs to start immediately after C1
    // Also, S.ref_url == C.url
    // useSegmentQuery param is really not needed for this method. We keep it just to be
    // consistent with all other 'construct' method parameters
    public void constructCategory2CategoryRefinedSession (UidToSegmentRecord selectedUidRecord, 
                                                        ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs,
                                                        GregorianCalendar calendar, SessionStats sessionStats) throws Exception {

        SegmentCategoryToPidRecord selectedCategoryRecord1;
        SegmentCategoryToPidRecord selectedCategoryRecord2;
        String selectedPid;
        String startUrl1;    // session-start-url
        String startUrl2;    // session-start-url
        String refinedCatId;

        // Step 1
        // generate pixel logs: category page. 
        // startURL == initial session start url
        startUrl1 = refUrlPool.selectRefUrlAtRandom ();

        // select a category (from list of predefined categories)
        selectedCategoryRecord1 = segmentCategoryToPidMap.selectPrimaryCategoryAtRandom (selectedUidRecord.getView(),selectedUidRecord.getSegment());
        if (selectedCategoryRecord1 == null) {
            MessageLogger.logError ("No consistent category available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment());
            return;
        }

        // select a pid
        selectedPid = segmentCategoryToPidMap.selectPidAtRandom (selectedCategoryRecord1);
        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment ());
            return;
        }

        // generate only category page (no followups with product -> atc -> conversion)
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulatePartialCategorySession (selectedUidRecord, 
                                        calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                        startUrl1, selectedCategoryRecord1, selectedPid);


        // step 2: refined category session
        // startUrl == first category page url
        startUrl2 = BuildCategoryPagePixel.getCategoryPageUrl (selectedCategoryRecord1.getCatId ()); // ref_url for refined category

        // get segmentCategoryRecord for the refinedJourney "c2c"
        refinedCatId = refinedJourneyMap.selectRefinedQueryAtRandom ("c2c", selectedCategoryRecord1.getCatId (),
                                                                     selectedUidRecord.getView (), 
                                                                     selectedUidRecord.getSegment());
        if (refinedCatId == null) {
            MessageLogger.logError ("no refinedJourney for c2c, primaryCatId = " + selectedCategoryRecord1.getCatId ());
            return;
        }

        selectedCategoryRecord2 = segmentCategoryToPidMap.getCategoryRecord (refinedCatId);
        if (selectedCategoryRecord2 == null) {
            MessageLogger.logError ("no segmentCategoryRecord for c2c, primaryCatId = " + selectedCategoryRecord1.getCatId());
            return;
        }

        // are segments consistent in primary, secondary ? debugging...
        MessageLogger.logDebug ("userSegment, primary segment, refined segment: " +
                            selectedUidRecord.getSegment () + ", " + 
                            selectedCategoryRecord1.getSegment () + ", " + 
                            selectedCategoryRecord2.getSegment ());

        // select a pid
        selectedPid = segmentCategoryToPidMap.selectPidAtRandom (selectedCategoryRecord2);
        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment ());
            return;
        }

        // update sessionStat
        sessionStats.setSelectedPid (selectedPid);
        sessionStats.setPrimaryQuery (selectedCategoryRecord1.getCatName ());
        sessionStats.setRefinedQuery (selectedCategoryRecord2.getCatName ());

        // for each such visit, generate category+product+atc+conversion pixelLog for a given cat, PID
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulateCategorySession (selectedUidRecord, 
                                 calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                 startUrl2, selectedCategoryRecord2, selectedPid);

        // summary
        MessageLogger.logDebug ("C2C: cat1 = " + selectedCategoryRecord1.getCatName () + 
                                       ", \n\tcat1 refUrl = " + startUrl1 +
                                       ", \n\tcat1 endUrl = " + startUrl2 +
                                       ", \n\tcat2 = " + selectedCategoryRecord2.getCatName () +
                                       ", \n\tcat2 refUrl = " + startUrl2 +
                                       ", \n\tcat2 endUrl = "  + BuildCategoryPagePixel.getCategoryPageUrl (selectedCategoryRecord2.getCatId ())); 
    }


    // session with Z2S refinement for zero-result-search session
    // Based on engr comment, in Z2S, the S2 needs to start immediately after search-result-page of S1
    // Also, S2.ref_url == S1.url
    // followed by a refined-query-search-session
    public void constructZeroResult2SearchRefinedSession (UidToSegmentRecord selectedUidRecord, 
                                                          ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs,
                                                          GregorianCalendar calendar, SessionStats sessionStats) throws Exception {

        String startUrl1;    // session-start-url
        ZeroResultQueryRecord selectedZeroResultQueryRecord;
        String startUrl2;
        SegmentQueryToPidRecord selectedQueryRecord2;
        String selectedQueryStr;
        String selectedPid;

        // startURL == initial session start url
        startUrl1 = refUrlPool.selectRefUrlAtRandom ();

        // Select a query (from list of predefined queries)
        selectedZeroResultQueryRecord = zeroResultQueryMap.selectZeroResultQueryAtRandom ();

        // update sessionStat
        sessionStats.setSelectedPid ("-");
        sessionStats.setPrimaryQuery (selectedZeroResultQueryRecord.getQuery ());

        // for each such visit, generate event, search-result-page pixelLog. For zero-results,
        // simulate only 'partial' session (no ATC, Conversion)
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulatePartialSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                         startUrl1, selectedZeroResultQueryRecord.getQuery(), null);

        // step 2: refined search session
        // startUrl2 == first query's search-result-page url 
        startUrl2 = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedZeroResultQueryRecord.getQuery()); // ref_url for refined search

        // get segmentQueryRecord for the refinedSearchId
        // for zero-result-record, the 'refined-search-query' is selected at random
        selectedQueryRecord2 = segmentQueryToPidMap.getRefinedQueryRecord (selectedUidRecord.getView(),
                                                                           selectedUidRecord.getSegment());
        if (selectedQueryRecord2 == null) {
            MessageLogger.logError ("no refined segmentQueryRecord for z2s, zeroQuery = " + selectedZeroResultQueryRecord.getQuery());
            return;
        }

        // are segments consistent in primary, secondary ? debugging...
        MessageLogger.logDebug ("userSegment, primary segment, refined segment: " +
                            selectedUidRecord.getSegment () + ", " + 
                            selectedQueryRecord2.getSegment ());

        // use secondQuery
        selectedQueryStr = selectedQueryRecord2.getQuery ();

        // Select a pid. 
        selectedPid = segmentQueryToPidMap.selectPidAtRandom (selectedQueryRecord2);

        // update sessionStat
        sessionStats.setSelectedPid (selectedPid);
        sessionStats.setPrimaryQuery (selectedZeroResultQueryRecord.getQuery ());
        sessionStats.setRefinedQuery (selectedQueryRecord2.getQuery ());


        // for second search, generate entire journey (search-event->search-result-page-> product+atc+conversion) pixelLog for a given PID
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulateSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                               startUrl2, selectedQueryRecord2, selectedPid);

        // summary
        MessageLogger.logDebug ("S2S: query1 = " + selectedZeroResultQueryRecord.getQuery () + 
                                       ", \n\tquery1 refUrl = " + startUrl1 +
                                       ", \n\tquery1 endUrl = " + startUrl2 +
                                       ", \n\tquery2 = " + selectedQueryRecord2.getQuery () +
                                       ", \n\tquery2 refUrl = " + startUrl2 +
                                       ", \n\tquery2 endUrl = "  + BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryStr)); 

    }

    public void constructZeroResult2CategoryRefinedSession (UidToSegmentRecord selectedUidRecord, 
                                                            ArrayList <PixelLog> collectedPixelLogs, ArrayList<ApiLog> collectedApiLogs,
                                                            GregorianCalendar calendar, SessionStats sessionStats) throws Exception {
        String startUrl1;    // session-start-url
        ZeroResultQueryRecord selectedZeroResultQueryRecord;
        String startUrl2;
        SegmentCategoryToPidRecord selectedCategoryRecord;
        String selectedQueryStr;
        String selectedPid;

        // startURL == initial session start url
        startUrl1 = refUrlPool.selectRefUrlAtRandom ();

        // Select a query (from list of predefined queries)
        selectedZeroResultQueryRecord = zeroResultQueryMap.selectZeroResultQueryAtRandom ();

        // update sessionStat
        sessionStats.setSelectedPid ("-");
        sessionStats.setPrimaryQuery (selectedZeroResultQueryRecord.getQuery ());

        // for each such visit, generate event, search-result-page pixelLog. For zero-results,
        // simulate only 'partial' session (no ATC, Conversion)
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulatePartialSearchSession (selectedUidRecord, calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                         startUrl1, selectedZeroResultQueryRecord.getQuery(), null);

        // step 2: refined category session
        // startUrl2 == first query's search-result-page url 
        startUrl2 = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedZeroResultQueryRecord.getQuery()); // ref_url for refined search

        // get segmentCategoryRecord for the refinedJourneyId
        // for zero-result-record, the 'refined-category-record-id' is selected at random
        selectedCategoryRecord = segmentCategoryToPidMap.getRefinedCategoryRecord (selectedUidRecord.getView(),
                                                                                   selectedUidRecord.getSegment());
        if (selectedCategoryRecord == null) {
            MessageLogger.logError ("no segmentCategoryRecord for z2c, zeroQuery = " + selectedZeroResultQueryRecord.getQuery());
            return;
        }

        // are segments consistent in primary, secondary ? debugging...
        MessageLogger.logDebug ("userSegment, primary segment, refined segment: " +
                            selectedUidRecord.getSegment () + ", " + 
                            selectedCategoryRecord.getSegment ());

        // select a pid
        selectedPid = segmentCategoryToPidMap.selectPidAtRandom (selectedCategoryRecord);
        if (selectedPid == null) {
            MessageLogger.logError ("No consistent pid available: uid = " + selectedUidRecord.getUid () +
                                        ", view =  " + selectedUidRecord.getView () + 
                                        ", segment =  " + selectedUidRecord.getSegment ());
            return;
        }

        // update sessionStat
        sessionStats.setSelectedPid (selectedPid);
        sessionStats.setPrimaryQuery (selectedZeroResultQueryRecord.getQuery ());
        sessionStats.setRefinedQuery (selectedCategoryRecord.getCatName ());

        // for each such visit, generate category+product+atc+conversion pixelLog for a given cat, PID
        // PixelLogs are collected in the 'pixelLogs' arrayList
        simulateCategorySession (selectedUidRecord, 
                                 calendar.getTimeInMillis(), collectedPixelLogs, collectedApiLogs,
                                 startUrl2, selectedCategoryRecord, selectedPid);

        // summary
        MessageLogger.logDebug ("S2C: query1 = " + selectedZeroResultQueryRecord.getQuery () + 
                                       ", \n\tquery1 refUrl = " + startUrl1 +
                                       ", \n\tquery1 endUrl = " + startUrl2 +
                                       ", \n\tcat = " + selectedCategoryRecord.getCatName () +
                                       ", \n\tcat2 refUrl = " + startUrl2 + 
                                       ", \n\tcat2 endUrl = "  + BuildCategoryPagePixel.getCategoryPageUrl (selectedCategoryRecord.getCatId ()) ); 

    }


    // INTERNAL METHODS
    private void simulateSearchSession (UidToSegmentRecord uidToSegmentRecord, long startTime, 
                                        ArrayList <PixelLog> collectedPixelLogs, ArrayList <ApiLog> collectedApiLogs,
                                        String refUrl, SegmentQueryToPidRecord selectedQueryRecord, String pid) throws Exception {
        PixelLog pixelLog;
        String productName;
        String productSkuId;
        ApiLog apiLog;

        // generate pixel logs: search event, search page, product page, atc, conversion

        // search event 
        pixelLog = buildSearchEventPixelFromTemplate (uidToSegmentRecord, startTime, refUrl, selectedQueryRecord.getQuery()); 
        collectedPixelLogs.add (pixelLog);

        // search API call
        apiLog = buildSearchApiFromTemplate (uidToSegmentRecord, startTime, refUrl, selectedQueryRecord);
        collectedApiLogs.add (apiLog);

        // search page
        pixelLog = buildSearchResultPagePixelFromTemplate (uidToSegmentRecord, startTime, refUrl, selectedQueryRecord.getQuery()); 
        collectedPixelLogs.add (pixelLog);

        // product page
        refUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedQueryRecord.getQuery()); // ref_url for next productPage pixel
        productName = processedFeed.lookupProductName (pid);
        productSkuId = processedFeed.lookupProductSkuId (pid);
        startTime = startTime + 5 * 60 * 1000; // 5 min delay
        pixelLog = buildProductPixelFromTemplate (uidToSegmentRecord, startTime, refUrl, pid, productName, productSkuId);
        collectedPixelLogs.add (pixelLog);

        // ATC. RefUrl for a ATC-event is the ref-url for the productPage where add-to-cart event is triggered 
        // Therefore, refUrl value remains same as above
        startTime = startTime + 8 * 60 * 1000; // 5 min delay
        pixelLog = buildAddToCartPixelFromTemplate (uidToSegmentRecord, startTime, refUrl, pid, productSkuId);
        collectedPixelLogs.add (pixelLog);

        // Conversion
        startTime = startTime + 10 * 60 * 1000; // 10 min delay
        pixelLog = buildConversionPixelFromTemplate (uidToSegmentRecord, startTime, pid);
        collectedPixelLogs.add (pixelLog);
    }


    // partialSearch -- journey only upto search-result-page
    // For zero-result-query, the 'selectedQueryRecord' is null (but query string is valid)
    private void simulatePartialSearchSession (UidToSegmentRecord uidToSegmentRecord, long startTime, 
                                              ArrayList <PixelLog> collectedPixelLogs,
                                              ArrayList <ApiLog> collectedApiLogs,
                                              String refUrl, String query,
                                              SegmentQueryToPidRecord selectedQueryRecord) throws Exception {
        PixelLog pixelLog;
        ApiLog apiLog;

        // generate pixel logs: search event, search page, product page, atc, conversion

        // search event 
        pixelLog = buildSearchEventPixelFromTemplate (uidToSegmentRecord, startTime, refUrl, query); 
        collectedPixelLogs.add (pixelLog);

        // search API call. "selectedQueryRecord" is null for zero-result-query session
        if (selectedQueryRecord != null) 
            apiLog = buildSearchApiFromTemplate (uidToSegmentRecord, startTime, refUrl, selectedQueryRecord);
        else
            apiLog = buildZeroResultSearchApiFromTemplate (uidToSegmentRecord, startTime, refUrl, query);
        collectedApiLogs.add (apiLog);

        // search result page
        pixelLog = buildSearchResultPagePixelFromTemplate (uidToSegmentRecord, startTime, refUrl, query); 
        collectedPixelLogs.add (pixelLog);
    }


    private void simulateCategorySession (UidToSegmentRecord uidToSegmentRecord, long startTime, 
                                          ArrayList <PixelLog> collectedPixelLogs, ArrayList <ApiLog> collectedApiLogs,
                                          String refUrl, SegmentCategoryToPidRecord selectedCategoryRecord,
                                          String pid) throws Exception {
        PixelLog pixelLog;
        String productName;
        String productSkuId;
        ApiLog apiLog;
        String catId;
        String catName;
        String catRelPath;

        catId = selectedCategoryRecord.getCatId();
        catName = selectedCategoryRecord.getCatName();
        catRelPath = selectedCategoryRecord.getCatRelPath (); 

        // build categoryApi
        apiLog = buildCategoryApiFromTemplate (uidToSegmentRecord, startTime, refUrl, selectedCategoryRecord);
        collectedApiLogs.add (apiLog);

        // category page
        pixelLog = buildCategoryPixelFromTemplate (uidToSegmentRecord, startTime, refUrl, catId, catName, catRelPath); 
        collectedPixelLogs.add (pixelLog);

        // product page
        refUrl = BuildCategoryPagePixel.getCategoryPageUrl (catId); // ref_url for next productPage pixel
        productName = processedFeed.lookupProductName (pid);
        productSkuId = processedFeed.lookupProductSkuId (pid);
        startTime = startTime + 5 * 60 * 1000; // 5 min delay
        pixelLog = buildProductPixelFromTemplate (uidToSegmentRecord, startTime, refUrl, pid, productName, productSkuId);
        collectedPixelLogs.add (pixelLog);

        // ATC. RefUrl for a ATC-event is the ref-url for the productPage where add-to-cart event is triggered 
        // Therefore, refUrl value remains same as above
        startTime = startTime + 8 * 60 * 1000; // 5 min delay
        pixelLog = buildAddToCartPixelFromTemplate (uidToSegmentRecord, startTime, refUrl, pid, productSkuId);
        collectedPixelLogs.add (pixelLog);

        // Conversion
        startTime = startTime + 10 * 60 * 1000; // 10 min delay
        pixelLog = buildConversionPixelFromTemplate (uidToSegmentRecord, startTime, pid);
        collectedPixelLogs.add (pixelLog);
    }

    // partialCategory -- journey only upto category-page-view
    private void simulatePartialCategorySession (UidToSegmentRecord uidToSegmentRecord, long startTime, 
                                          ArrayList <PixelLog> collectedPixelLogs, ArrayList <ApiLog> collectedApiLogs,
                                          String refUrl, SegmentCategoryToPidRecord selectedCategoryRecord, 
                                          String pid) throws Exception {
        PixelLog pixelLog;
        String catId;
        String catName;
        String catRelPath;
        ApiLog apiLog;

        catId = selectedCategoryRecord.getCatId();
        catName = selectedCategoryRecord.getCatName();
        catRelPath = selectedCategoryRecord.getCatRelPath (); 

        // build categoryApi
        apiLog = buildCategoryApiFromTemplate (uidToSegmentRecord, startTime, refUrl, selectedCategoryRecord);
        collectedApiLogs.add (apiLog);

        // category page
        pixelLog = buildCategoryPixelFromTemplate (uidToSegmentRecord, startTime, refUrl, catId, catName, catRelPath); 
        collectedPixelLogs.add (pixelLog);

    }


    // "query" is expected to be the selectedQuery
    private PixelLog buildSearchEventPixelFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                                  String refUrl, String query) throws Exception {
        PixelLog.Builder searchEventPixelLogBuilder;
        BuildSearchEventPixel buildSearchEventPixel;
        int buildStatus;

        // prepare a searchEvent pixelLog template
        searchEventPixelLogBuilder = pixelTemplates.loadSearchEventPixelTemplate ();

        // update template
        buildSearchEventPixel = new BuildSearchEventPixel ();
        buildStatus = buildSearchEventPixel.build (searchEventPixelLogBuilder, uidToSegmentRecord, logTime, refUrl, query);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (searchEventPixelLogBuilder.build());
        }

        return (null);
    }

    // "query" is expected to be the selectedQuery
    private PixelLog buildSearchResultPagePixelFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                                       String refUrl, String query) throws Exception {
        PixelLog.Builder searchResultPixelLogBuilder;
        BuildSearchResultPagePixel buildSearchResultPagePixel;
        int buildStatus;

        // prepare a searchResult pixelLog template
        searchResultPixelLogBuilder = pixelTemplates.loadSearchResultPagePixelTemplate ();

        // update template
        buildSearchResultPagePixel = new BuildSearchResultPagePixel ();
        buildStatus = buildSearchResultPagePixel.build (searchResultPixelLogBuilder, uidToSegmentRecord, logTime, refUrl, query);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (searchResultPixelLogBuilder.build());
        }

        return (null);
    }

    // "pid" is expected to be what is in pacificsupply feed (offset'd from the original pid value)
    private PixelLog buildProductPixelFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                              String refUrl, String pid, String productName, String productSkuId) throws Exception {
        PixelLog.Builder prodPixelLogBuilder;
        BuildProductPagePixel buildProdPagePixel;
        int buildStatus;

        // prepare a product page pixelLog template
        prodPixelLogBuilder = pixelTemplates.loadProductPixelTemplate ();

        // update template
        buildProdPagePixel = new BuildProductPagePixel ();
        buildStatus = buildProdPagePixel.build (prodPixelLogBuilder, uidToSegmentRecord, logTime, refUrl, pid, productName, productSkuId);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (prodPixelLogBuilder.build());
        }

        return (null);
    }
    
    private PixelLog buildCategoryPixelFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                               String refUrl, String catId, String catName, String catRelPath) throws Exception {
        PixelLog.Builder catPixelLogBuilder;
        BuildCategoryPagePixel buildCatPagePixel;
        int buildStatus;

        // prepare a category page pixelLog template
        catPixelLogBuilder = pixelTemplates.loadCategoryPixelTemplate ();

        // update template
        buildCatPagePixel = new BuildCategoryPagePixel ();
        buildStatus = buildCatPagePixel.build (catPixelLogBuilder, uidToSegmentRecord, logTime, 
                                                             refUrl, catId, catName, catRelPath);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (catPixelLogBuilder.build());
        }

        return (null);
    }


    private PixelLog buildAddToCartPixelFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                                String refUrl, String pid, String productSkuId) throws Exception {
        PixelLog.Builder atcPixelLogBuilder;
        BuildAddToCartPixel buildATCPixel;
        int buildStatus;

        // prepare a ATC page pixelLog template
        atcPixelLogBuilder = pixelTemplates.loadAddToCartPixelTemplate ();

        // update template
        buildATCPixel = new BuildAddToCartPixel ();
        buildStatus = buildATCPixel.build (atcPixelLogBuilder, uidToSegmentRecord, logTime, refUrl, pid, productSkuId);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (atcPixelLogBuilder.build());
        }

        return (null);
    }

    private PixelLog buildConversionPixelFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, String pid) throws Exception {
        PixelLog.Builder conversionPixelLogBuilder;
        BuildConversionPixel buildConversionPixel;
        int buildStatus;

        // prepare a conversion pixelLog template
        conversionPixelLogBuilder = pixelTemplates.loadConversionPixelTemplate ();

        // update template
        buildConversionPixel = new BuildConversionPixel ();
        buildStatus = buildConversionPixel.build (conversionPixelLogBuilder, uidToSegmentRecord, logTime, pid, processedFeed, orderIdGenerator);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (conversionPixelLogBuilder.build());
        }

        return (null);
    }


    // "query" is expected to be the selectedQuery
    private ApiLog buildSearchApiFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                               String refUrl, SegmentQueryToPidRecord selectedQueryRecord) throws Exception {
        ApiLog.Builder searchApiLogBuilder;
        BuildSearchApi buildSearchApi;
        int buildStatus;

        // prepare a search ApiLog template
        searchApiLogBuilder = apiTemplates.loadSearchApiTemplate ();

        // update template
        buildSearchApi = new BuildSearchApi ();
        buildStatus = buildSearchApi.build (searchApiLogBuilder, uidToSegmentRecord, logTime, refUrl, selectedQueryRecord, processedFeed);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return apiLog object from this builder
            return (searchApiLogBuilder.build());
        }

        return (null);
    }

    // "query" is expected to be the selectedQuery
    private ApiLog buildCategoryApiFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                               String refUrl, SegmentCategoryToPidRecord selectedCategoryRecord) throws Exception {
        ApiLog.Builder categoryApiLogBuilder;
        BuildCategoryApi buildCategoryApi;
        int buildStatus;

        // prepare a search ApiLog template
        categoryApiLogBuilder = apiTemplates.loadCategoryApiTemplate ();

        // update template
        buildCategoryApi = new BuildCategoryApi ();
        buildStatus = buildCategoryApi.build (categoryApiLogBuilder, uidToSegmentRecord, logTime, refUrl, selectedCategoryRecord, processedFeed);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return apiLog object from this builder
            return (categoryApiLogBuilder.build());
        }

        return (null);
    }

    // "query" is expected to be the selectedQuery
    // For zero-result, apiLog->response has no 'products'; numFound = 0
    private ApiLog buildZeroResultSearchApiFromTemplate (UidToSegmentRecord uidToSegmentRecord, long logTime, 
                                                         String refUrl, String query) throws Exception {
        ApiLog.Builder searchApiLogBuilder;
        BuildSearchApi buildSearchApi;
        int buildStatus;

        // prepare a search ApiLog template
        searchApiLogBuilder = apiTemplates.loadSearchApiTemplate ();

        // update template
        buildSearchApi = new BuildSearchApi ();
        buildStatus = buildSearchApi.buildZeroResult (searchApiLogBuilder, uidToSegmentRecord, logTime, refUrl, query, processedFeed);

        if (buildStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            // return apiLog object from this builder
            return (searchApiLogBuilder.build());
        }

        return (null);
    }

}

