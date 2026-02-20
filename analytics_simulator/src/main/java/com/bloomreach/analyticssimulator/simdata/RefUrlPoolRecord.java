package com.bloomreach.analyticssimulator.simdata;

// class to hold refUrl's
import java.util.ArrayList;

public class RefUrlPoolRecord {
        String urlType;
        String view; 
        String segment;
        String url;

        public RefUrlPoolRecord (String type, String view, String segment, String url) {
            this.urlType = type;
            this.url = url;
        }

        public String getUrlType () {
            return this.urlType;
        }

        public String getView () {
            return this.view;
        }

        public String getSegment () {
            return this.segment;
        }

        public String getUrl () {
            return this.url;
        }
}


