package com.bloomreach.analyticssimulator.simdata;

public class UidToSegmentRecord {
        String uid;
        String segment;
        String view;

        public UidToSegmentRecord (String uid, String view, String segment) {
            this.uid = uid;
            this.view = view;
            this.segment = segment;
        }

        public String getUid () {
            return this.uid;
        }

        public String getView () {
            return this.view;
        }

        public String getSegment () {
            return this.segment;
        }
}

