package com.bloomreach.trafficgenerator.site.journeydata;

// Use the manually prepared startUrl list
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.config.SiteConfig;
import com.bloomreach.trafficgenerator.site.feed.ProductFeed;
import com.bloomreach.trafficgenerator.site.feed.FeedRecord;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildSearchResultPagePixel;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildCategoryPagePixel;
import com.bloomreach.trafficgenerator.site.journeydata.queryexecutor.CategoryInfo;

public class StartUrlPool {

    public final static String URL_TYPE_PRODUCT = "product";
    public final static String URL_TYPE_CATEGORY = "category";
    public final static String URL_TYPE_HOME = "home";
    public final static String URL_TYPE_SEARCH = "search";
    public final static String URL_TYPE_OTHER = "other";
    public final static String URL_TYPE_THEMATIC = "thematic";

    ArrayList <StartUrlPoolRecord> startUrlPoolRecordList;
    StartUrlPoolRecord homepageStartUrlPoolRecord;  // one record for 'home'
    ProductFeed productFeed;
    CategoryCollector categoryCollector;
    SearchTerms searchTerms;

    public StartUrlPool () {
    }

    // when this class is used from SimulatePixelLogs, it will have already
    // loaded productFeed and therefore supplied to this class
    public void setProductFeed (ProductFeed productFeed) {
        this.productFeed = productFeed;
    }

    public void setCategoryCollector (CategoryCollector categoryCollector) {
        this.categoryCollector = categoryCollector;
    }

    public void setSearchTerms (SearchTerms searchTerms) {
        this.searchTerms = searchTerms;
    }

    public void doLoad () throws Exception {
        ArrayList<FeedRecord> availableProducts;
        StartUrlPoolRecord record;
        ArrayList <CategoryInfo> allCategories;
        ArrayList<String> blankViews = null;

        // not added to startUrlPool
        homepageStartUrlPoolRecord = new StartUrlPoolRecord (URL_TYPE_HOME, null, blankViews,
                                                             SiteConfig.getUrlConfigParam ("HOMEPAGE_URL"));

        availableProducts = this.productFeed.listAvailableProducts ();
        startUrlPoolRecordList = new ArrayList <StartUrlPoolRecord> ();
        for (FeedRecord feedRecord : availableProducts) {
            record = new StartUrlPoolRecord (URL_TYPE_PRODUCT, feedRecord.getProductId(), 
                                             feedRecord.getViews (), feedRecord.getUrl ());
            startUrlPoolRecordList.add (record);
        }

        allCategories = this.categoryCollector.getAllCategoryInfoList (); // may be null in rare cases
        if (allCategories != null) {
            for (CategoryInfo catInfo : allCategories) {
                String catUrl;

                // views are not needed for category startUrl. We set a value just for consistency
                // Assumption is, category structure is same among different views
                // catUrl = .../categories/<catId>, using same convention as SPA
                // 'prefix' in config file already has trailing '/'
                // catUrl = String.format ("%s%s", SiteConfig.getUrlConfigParam ("CATEGORY_URL_PREFIX"), 
                //                                  catInfo.getCatId ());

                catUrl = BuildCategoryPagePixel.getCategoryPageUrl (catInfo.getCatId ());
                record = new StartUrlPoolRecord (URL_TYPE_CATEGORY, catInfo.getCatId(), blankViews, catUrl);
                startUrlPoolRecordList.add (record);
            }
        }

        // Add searchUrls to pool
        for (String searchTerm : this.searchTerms.getAllSearchTerms()) {
            String searchPageUrl;

            searchPageUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (searchTerm);
            record = new StartUrlPoolRecord (URL_TYPE_SEARCH, searchTerm, blankViews, searchPageUrl);
            startUrlPoolRecordList.add (record);
        }
    }

    public ArrayList <StartUrlPoolRecord> getStartUrlPoolRecordList () {
        return (startUrlPoolRecordList);
    }

    // given the list of predefined startUrls, pick one at random
    // In order to give 'lot of weight' to homepage, we consider 
    // 'totalUrls' to be 5x of actual. Then, if random index is >= actualSize,
    // return homepage url
    public StartUrlPoolRecord selectStartUrlAtRandom () {
        int randomIndx;
        int totalStartUrlCount;
        StartUrlPoolRecord selectedStartUrlRecord;

        // pick one of the startUrls at random
        totalStartUrlCount = startUrlPoolRecordList.size ();
        randomIndx = (int) (Math.random () * (totalStartUrlCount*5)); // values include 0 to (but not including) total
        if (randomIndx >= totalStartUrlCount)
            selectedStartUrlRecord = homepageStartUrlPoolRecord;
        else {
            selectedStartUrlRecord = startUrlPoolRecordList.get (randomIndx);
        }

        return selectedStartUrlRecord;
    }

}

