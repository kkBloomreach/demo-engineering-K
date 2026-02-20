package com.bloomreach.analyticssimulator.test;

import java.io.IOException;

import com.bloomreach.analyticssimulator.simdata.*;

// Test driver
public class TestSimulatedUidToSegmentMap {

    private final static String SIMULATED_UI_TO_SEGMENT_MAP_PATH = "./data/resources/SimulatedUidToSegmentMap.tsv";

    public static void main (String[] args) {

        UidToSegmentMap uidToSegmentMap;

        uidToSegmentMap = new UidToSegmentMap ();
        try {
            uidToSegmentMap.doLoad (SIMULATED_UI_TO_SEGMENT_MAP_PATH);
            System.out.println ("Simulated UidToSegment Map load successful. ");
        } catch (Exception e) {
            System.out.println ("Exception in loading. " + e.getMessage());
            System.exit (-1);
        }
        System.exit (0);
    }
}


