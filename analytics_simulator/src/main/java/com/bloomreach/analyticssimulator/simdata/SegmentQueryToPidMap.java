package com.bloomreach.analyticssimulator.simdata;

// Use the manually prepared query -> pid list for each of the queries.
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.MessageLogger;
import com.bloomreach.analyticssimulator.feed.ProcessedFeed;

public class SegmentQueryToPidMap {

    ArrayList <SegmentQueryToPidRecord> segmentPrimaryQueryToPidRecordList;
    ArrayList <SegmentQueryToPidRecord> segmentRefinedQueryToPidRecordList;
    ProcessedFeed processedFeed;

    public SegmentQueryToPidMap () {
    }

    // when this class is used from SimulatePixelLogs, it will have already
    // loaded processedFeed and therefore supplied to this class
    public void setProcessedFeed (ProcessedFeed processedFeed) {
        this.processedFeed = processedFeed;
    }

    public void doLoad (String srcFilePath) throws Exception {
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        int lineNum = 0;

        segmentPrimaryQueryToPidRecordList = new ArrayList <SegmentQueryToPidRecord> ();
        segmentRefinedQueryToPidRecordList = new ArrayList <SegmentQueryToPidRecord> ();

        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            // this src file does not have a 'headerLine' by itself
            // Each line has:
            // query<tab>5 pids (last pid may be "-")
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;
                SegmentQueryToPidRecord record;
                boolean isPrimary;
                String segment;
                int numFound;
                String view;

                // skip header line
                if (lineNum == 0) {
                    lineNum = lineNum + 1;
                    continue;
                }

                if (srcLine.length () == 0)
                    continue;

                // for some queries some pids are not specified. In that
                // case the input record has "-" in that place. Skip those columns
                tokens = srcLine.split ("\t");

                // src file may have blank tokens (ie, blank lines)
                if (tokens.length == 0)
                    continue;

                // params: id, querystr, numFound, primary-or-refined query, view, segment, pid*
                numFound = Integer.parseInt (tokens [2]);
                isPrimary = (Integer.parseInt (tokens[3])) == 1 ? true : false; 
                view = tokens[4];
                segment = tokens[5];
                record = new SegmentQueryToPidRecord (Integer.parseInt (tokens[0]), 
                                                        tokens[1].trim(), 
                                                        numFound, 
                                                        isPrimary, 
                                                        view, 
                                                        segment);

                for (int i = 6; i < tokens.length; i++) {
                    if (tokens [i].indexOf ("-") < 0) {
                        try {
                            String pidValue = tokens [i]; 
                            if (processedFeed.isProductInFeed (pidValue) == true) {
                                MessageLogger.logDebug ("pid map: orig pid: " + pidValue);
                                record.addPid (pidValue);
                            } else
                                MessageLogger.logWarning ("suggested pid is not in processed feed: " + tokens[i]);
                        } catch (Exception e) {
                            MessageLogger.logError ("Bad pid value: " + tokens [i]);
                        }
                    }
                }

                if (record.getPidList().size () > 0) {
                    // primary and refined-query records are kept in two separate ArrayLists
                    if (isPrimary == true)
                        segmentPrimaryQueryToPidRecordList.add (record);
                    else
                        segmentRefinedQueryToPidRecordList.add (record);
                }
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading SegmentQueryToPidMap: " + e.getMessage ());
        }

        if (srcReader != null)
        {
            try {
                srcReader.close ();
            }
            catch (Exception e)
            {
                MessageLogger.logError ("Src reader close exception: " + e.getMessage ());
            }
        }
    }

    // all primary queries
    public ArrayList <SegmentQueryToPidRecord> getSegmentPrimaryQueryToPidRecordList () {
        return (segmentPrimaryQueryToPidRecordList);
    }

    // all refined queries
    public ArrayList <SegmentQueryToPidRecord> getSegmentRefinedQueryToPidRecordList () {
        return (segmentRefinedQueryToPidRecordList);
    }

    // given the list of predefined queries, pick one at random
    // "Random" selection is only for primary query. The refined-query is
    // randomized in RefinedJourneyMap class
    public SegmentQueryToPidRecord selectPrimaryQueryAtRandom (String userView, String userSegment) {
        int totalQueries;
        int queryIndx;
        SegmentQueryToPidRecord selectedQuery;
        boolean found = false;
        int attempt = 0;

        totalQueries = segmentPrimaryQueryToPidRecordList.size();
        queryIndx = (int) (Math.random () * totalQueries); // values include 0 to (but not including) total
        selectedQuery = segmentPrimaryQueryToPidRecordList.get (queryIndx);
        if ((selectedQuery.getSegment().equals (userSegment) == true) && (selectedQuery.getView ().equals (userView)))
            return (selectedQuery);

        // lookup an immediate subsequent record that does have required view and segment
        for (int i = 0; i < segmentPrimaryQueryToPidRecordList.size(); i++) {
            queryIndx = (queryIndx + 1) % segmentPrimaryQueryToPidRecordList.size();    // round-robbin
            selectedQuery = segmentPrimaryQueryToPidRecordList.get (queryIndx);
            if ((selectedQuery.getView().equals (userView)) && (selectedQuery.getSegment().equals (userSegment)))
                return (selectedQuery);
        }

        MessageLogger.logError ("selectPrimary query. Couldn't pick query for required segment: " + userSegment + 
                            ", view = " + userView);
        return null;
    }


    // given the list of predefined queries, pick one at random
    // "Random" selection is only for primary query. The refined-query is
    // randomized in RefinedJourneyMap class
    public SegmentQueryToPidRecord selectPrimaryQueryAtRandom_OLD (String userView, String userSegment) {
        int totalQueries;
        int queryIndx;
        SegmentQueryToPidRecord selectedQuery;
        boolean found = false;
        int attempt = 0;

        // attempt max N times; otherwise return null
        while (!found && attempt++ < 1000) {
            // First pick one of the queries at random -- Use only "primaryQuery" list
            // Make sure the segment in query record is same as user's segment
            totalQueries = segmentPrimaryQueryToPidRecordList.size();
            queryIndx = (int) (Math.random () * totalQueries); // values include 0 to (but not including) total
            selectedQuery = segmentPrimaryQueryToPidRecordList.get (queryIndx);
            if ((selectedQuery.getSegment().equals (userSegment) == true) && 
                (selectedQuery.getView ().equals (userView)))
                return (selectedQuery);
        }

        MessageLogger.logError ("selectPrimary query. Couldn't pick query for required segment: " + userSegment + 
                            ", view = " + userView);
        return (null);  // couldn't get query with required userSegment !!!
    }


    public String selectPidAtRandom (SegmentQueryToPidRecord record) {
        ArrayList<String> pidListInRecord;
        int recordIndx;
        String selectedPid;

        pidListInRecord = record.getPidList ();

        // generate a random index in this list
        recordIndx = (int) (Math.random () * pidListInRecord.size ());
        selectedPid = pidListInRecord.get (recordIndx);
        MessageLogger.logDebug ("query = " + record.getQuery () + ", pid = " + selectedPid);
        return (selectedPid);
    }

    // param is queryStr. This method is used for s2s refinement
    // for s2s, a refined-query has already been picked from the refined list
    // return the qryToPid list for that refined-query
    public SegmentQueryToPidRecord getQueryRecord (String query) {
        SegmentQueryToPidRecord selectedRecord = null;

        for (SegmentQueryToPidRecord record : segmentRefinedQueryToPidRecordList) {
            if (record.getQuery ().equals (query)) {
                selectedRecord = record;
                break;
            }
        }

        return (selectedRecord);
    }

    // for c2s, the 'target' refined query is picked at-random
    public SegmentQueryToPidRecord getRefinedQueryRecord (String view, String segment) {
        int indx;
        SegmentQueryToPidRecord selectedRecord = null;
        
        indx = (int) (Math.random() * segmentRefinedQueryToPidRecordList.size());
        selectedRecord = segmentRefinedQueryToPidRecordList.get (indx);
        if ((selectedRecord.getView().equals (view)) && 
            (selectedRecord.getSegment().equals (segment)))
                return (selectedRecord);

        // lookup an immediate subsequent record that does have required view and segment
        for (int i = 0; i < segmentRefinedQueryToPidRecordList.size(); i++) {
            indx = (indx + 1) % segmentRefinedQueryToPidRecordList.size();    // round-robbin
            selectedRecord = segmentRefinedQueryToPidRecordList.get (indx);
            if ((selectedRecord.getView().equals (view)) && 
                (selectedRecord.getSegment().equals (segment)))
                return (selectedRecord);
        }

        MessageLogger.logError ("getRefinedquery. Couldn't pick query for required segment: " + segment + 
                            ", view = " + view);
        return null;
    }
}

