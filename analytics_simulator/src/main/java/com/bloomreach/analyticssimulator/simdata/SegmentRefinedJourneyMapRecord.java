package com.bloomreach.analyticssimulator.simdata;

// class to hold refinedJourney data
import java.util.ArrayList;

// example record in .tsv
//   s2s<tab>primary<tab>refined1<tab>refined2...
//  => search-to-search refinement, primaryquery, list-of-refined-queries
//  For category (c2c) record, values are catIds
public class SegmentRefinedJourneyMapRecord {

        String refineType;  // S2S, S2C, C2S, C2C (search->search, search->category, ...)
        String primary;  // may refer to search-term-id or category-record-id 
        ArrayList<String> refinedList;  // may refer to search-term-id or category-record-id
        String view;
        String segment;

        public SegmentRefinedJourneyMapRecord (String type, String primary, String view, String segment) {
            this.refineType = type;
            this.primary = primary;
            this.view = view;
            this.segment = segment;
            refinedList = new ArrayList <String> ();
        }

        public void addRefined (String refined) {
            refinedList.add (refined);
        }

        public String getRefineType () {
            return this.refineType;
        }

        public String getPrimary () {
            return this.primary;
        }

        public String getView () {
            return this.view;
        }

        public String getSegment () {
            return this.segment;
        }

        public ArrayList<String> getRefinedList () {
            return this.refinedList;
        }
}


