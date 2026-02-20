package com.bloomreach.analyticssimulator.simdata;

// class to hold CategoryStr -> pids
// Currently 6 or 7 pids are associated with each Category. That mapping is
// created manually. Therefore there is no 'generator' to generate a category -> pid map

import java.util.ArrayList;

public class SegmentCategoryToPidRecord {
        int id;
        String catName;
        String catId;
        String catRelPath;
        int numFound;
        boolean isPrimary;
        String view;
        String segment;
        ArrayList<String> pidList;

        public SegmentCategoryToPidRecord (int id, String catName, String catId, String catRelPath, 
                                           int numFound, boolean isPrimary, String view, String segment) {
            this.id = id;
            this.catName = catName;
            this.catId = catId;
            this.catRelPath = catRelPath;
            this.numFound = numFound;
            this.isPrimary = isPrimary;
            this.view = view;
            this.segment = segment;
            this.pidList = new ArrayList <String> ();
        }

        public void addPid (String pid) {
            this.pidList.add (pid);
        }

        public int getId () {
            return this.id;
        }

        public String getCatName () {
            return this.catName;
        }

        public String getCatId () {
            return this.catId;
        }

        public String getCatRelPath () {
            return this.catRelPath;
        }

        public int getNumFound () {
            return this.numFound;
        }

        public boolean getIsPrimary () {
            return this.isPrimary;
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

