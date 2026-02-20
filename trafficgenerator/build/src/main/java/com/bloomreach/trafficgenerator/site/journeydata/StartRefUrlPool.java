package com.bloomreach.trafficgenerator.site.journeydata;

// Use the manually prepared startRefUrl list
// StartREFUrl is different from StartURL. The starting-ref-url is used in analytics
// to detect different channels (eg, social, direct, navigation, ...)
// See https://bloomreach.atlassian.net/browse/AN-3811

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.config.SiteConfig;

public class StartRefUrlPool {

    public final static String REFURL_TYPE_SEARCH_ENGINE = "search_engine"; // eg, google.com
    public final static String REFURL_TYPE_SOCIAL = "social";   // eg, facebook.com
    public final static String REFURL_TYPE_HOME = "home";   // eg, site's own home page
    public final static String REFURL_TYPE_BLANK = "";   // eg, coming from some email message/just-type-url-in-browser/...

    private final static String[] REFURLS_SEARCH = { "https://www.google.com",
                                                     "https://r.search.yahoo.com",
                                                     "https://www.bing.com",
                                                     "https://search.engine0.com" };    // some url containing 'search'

    private final static String[] REFURLS_SOCIAL = { "https://www.pintrest.com",
                                                   "https://www.twitter.com" ,
                                                   "https://www.facebook.com",
                                                   "https://www.youtube.com",
                                                   "https://www.instagram.com",
                                                   "https://www.shopstyle.com",
                                                   "https://www.tiktok.com" };


    ArrayList <StartRefUrlPoolRecord> startRefUrlPoolRecordList;
    StartRefUrlPoolRecord homepageStartRefUrlPoolRecord;  // one record for 'home'
    StartRefUrlPoolRecord blankStartRefUrlPoolRecord;  // one record for 'blank'

    public StartRefUrlPool () {
    }

    public void doLoad () throws Exception {
        StartRefUrlPoolRecord record;

        // blank url-- not added to startRefUrlPool
        blankStartRefUrlPoolRecord = new StartRefUrlPoolRecord (REFURL_TYPE_BLANK, ""); 

        // site's own homepage url-- not added to startRefUrlPool
        homepageStartRefUrlPoolRecord = new StartRefUrlPoolRecord (REFURL_TYPE_HOME, 
                                                                   SiteConfig.getUrlConfigParam ("HOMEPAGE_URL"));

        startRefUrlPoolRecordList = new ArrayList <StartRefUrlPoolRecord> ();
        for (String refUrl: REFURLS_SEARCH) {
            record = new StartRefUrlPoolRecord (REFURL_TYPE_SEARCH_ENGINE, refUrl);
            startRefUrlPoolRecordList.add (record);
        }

        for (String refUrl: REFURLS_SOCIAL) {
            record = new StartRefUrlPoolRecord (REFURL_TYPE_SOCIAL, refUrl);
            startRefUrlPoolRecordList.add (record);
        }
    }

    // given the list of predefined startRefUrls, pick one at random
    // In order to give 'lot of weight' to homepage, we consider 
    // 'totalUrls' to be 5x of actual. Then, if random index is >= actualSize,
    // return homepage as refUrl
    public StartRefUrlPoolRecord selectStartRefUrlAtRandom () {
        int randomIndx;
        int totalStartRefUrlCount;
        StartRefUrlPoolRecord selectedStartRefUrlRecord;

        // pick one of the startRefUrls at random
        totalStartRefUrlCount = startRefUrlPoolRecordList.size ();
        randomIndx = (int) (Math.random () * (totalStartRefUrlCount*5)); // values include 0 to (but not including) total
        if (randomIndx >= totalStartRefUrlCount)
            // selectedStartRefUrlRecord = homepageStartRefUrlPoolRecord;
            selectedStartRefUrlRecord = blankStartRefUrlPoolRecord;
        else {
            selectedStartRefUrlRecord = startRefUrlPoolRecordList.get (randomIndx);
        }

        return selectedStartRefUrlRecord;
    }
}

