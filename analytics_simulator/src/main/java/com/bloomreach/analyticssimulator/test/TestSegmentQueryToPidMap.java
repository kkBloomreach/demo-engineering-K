package com.bloomreach.analyticssimulator.test;

// *********** Test driver **********

// Use the manually prepared segment -> pid list for each of the segments.
// IMPORTANT - the pid's used in the manually generated list are "Original" pids. They need to be
// adjusted as per the rule applied for PacificSupply feed
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.ProcessedFeed;
import com.bloomreach.analyticssimulator.feed.ProcessedXMLFeed;

class TestSegmentQueryToPidMap {

    private final static String SEGMENT_TO_PID_MAP_PATH = "./data/resources/SegmentQueryToPidMap.tsv";
    public final static String SOURCE_PREPROCESSED_FEED_PATH = "./data/resources/product_out.xml";

    ArrayList <SegmentQueryToPidRecord> segmentQueryToPidRecordList;
    ProcessedFeed processedFeed;

    public static void main (String[] args) {

        TestSegmentQueryToPidMap tester;

        tester = new TestSegmentQueryToPidMap ();
        try {
            // load processed feed to check specific pid is in fact in the catalog
            tester.loadProcessedFeed ();
            tester.doTest ();
        } catch (Exception e) {
            System.out.println ("Exception in loading. " + e.getMessage());
            System.exit (-1);
        }
        System.exit (0);
    }

    // load processed feed records to ensure PIDs mentioned in the segment->pid map
    // are indeen in the feed
    private void loadProcessedFeed () throws Exception {

        processedFeed = new ProcessedXMLFeed ();
        processedFeed.load (SOURCE_PREPROCESSED_FEED_PATH);
    }

    private void doTest () throws Exception {
        SegmentQueryToPidMap loader;
        ArrayList <SegmentQueryToPidRecord> segmentQueryToPidRecordList;

        loader = new SegmentQueryToPidMap ();
        loader.setProcessedFeed (processedFeed);
        loader.doLoad (SEGMENT_TO_PID_MAP_PATH);
        segmentQueryToPidRecordList = loader.getSegmentPrimaryQueryToPidRecordList ();

        for (SegmentQueryToPidRecord record : segmentQueryToPidRecordList) {
            String segment;
            ArrayList <String> pidList;

            segment = record.getSegment ();
            pidList = record.getPidList ();
            System.out.println ("Segment: " + segment);
            for (String pid : pidList) {
                System.out.println ("\tpid: " + pid);
            }
        }
    }
}

