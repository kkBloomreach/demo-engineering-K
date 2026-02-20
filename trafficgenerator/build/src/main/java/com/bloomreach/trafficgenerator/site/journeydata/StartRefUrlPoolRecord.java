package com.bloomreach.trafficgenerator.site.journeydata;

// class to hold startUrl record

public class StartRefUrlPoolRecord {
        String refUrlType; // 'search', 'social', 'home', 'blank'
        String refUrl;

        // type = product, category, home
        public StartRefUrlPoolRecord (String type, String refUrl) {
            this.refUrlType = type;
            this.refUrl = refUrl;
        }

        public String getRefUrlType () {
            return this.refUrlType;
        }

        public String getRefUrl () {
            return this.refUrl;
        }
}



