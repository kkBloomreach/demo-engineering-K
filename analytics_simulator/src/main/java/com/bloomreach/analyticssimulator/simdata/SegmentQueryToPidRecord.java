package com.bloomreach.analyticssimulator.simdata;

// class to hold QueryStr -> pids
// Currently 6 or 7 pids are associated with each query str. That mapping is
// created manually. Therefore there is no 'generator' to generate a query -> pid map

import java.util.ArrayList;

public class SegmentQueryToPidRecord {
        int id;
        String query;
        int numFound;
        boolean isPrimary;
        String segment;
        String view;
        ArrayList<String> pidList;

        public SegmentQueryToPidRecord (int id, String query, int numFound, boolean isPrimary, String view, String segment) {
            this.id = id;
            this.query = query;
            this.numFound = numFound;
            this.isPrimary = isPrimary;
            this.view = view;
            this.segment = segment;
            this.pidList = new ArrayList <String> ();
        }

        public void addPid (String pid) {
            this.pidList.add (pid);
        }

        public boolean getIsPrimary () {
            return this.isPrimary;
        }

        public int getId () {
            return this.id;
        }

        public String getQuery () {
            return this.query;
        }

        public int getNumFound () {
            return this.numFound;
        }

        public String getView () {
            return this.view;
        }

        public String getSegment () {
            return this.segment;
        }

        public ArrayList<String> getPidList () {
            return this.pidList;
        }
}

