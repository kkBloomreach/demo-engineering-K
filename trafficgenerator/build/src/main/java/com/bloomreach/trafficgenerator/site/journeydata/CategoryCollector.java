// collect category list using queryExecutor (instead of the feed)
// This is to collect DynamicCategories besides the categories in the feed
package com.bloomreach.trafficgenerator.site.journeydata;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.journeydata.queryexecutor.*;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journeydata.templates.ApiBRData;

public class CategoryCollector {

    private final static String STAR_QUERY = "*";
    private ArrayList <CategoryInfo> allCategoryInfoList = null;

    public CategoryCollector () {
    }

    public void collectCategories (String realm) throws Exception {
        QueryExecutorSearchApiResponse searchApiResponse = null;

        try {
            searchApiResponse = executeSearchQuery (realm);
        } catch (Exception e) {
            MessageLogger.logError ("Category collector, exception in executeSearch");
        }

        if (searchApiResponse == null) {
            MessageLogger.logError ("Category collector, API response is null");
            return;
        }

        this.allCategoryInfoList = searchApiResponse.getCategoryList (); // may be null
    }

    public ArrayList <CategoryInfo> getAllCategoryInfoList () {
        return this.allCategoryInfoList;
    }

    public CategoryInfo lookupCategoryInfo (String catId) {
        if (this.allCategoryInfoList != null) {
            for (CategoryInfo aCatInfo : this.allCategoryInfoList) {
                if (aCatInfo.getCatId ().equals (catId)) 
                    return aCatInfo;
            }
        }

        // potentially possible because currently we can have max 200 category info 
        // due to API limitation
        MessageLogger.logWarning (String.format ("Could not lookup categoryInfo for catId = %s", catId));
        return null;    
    }

    // fullPath: "A/B/C", case sensitive
    public CategoryInfo lookupCategoryInfoByFullPath (String fullPath) {
        if (this.allCategoryInfoList != null) {
            for (CategoryInfo aCatInfo : this.allCategoryInfoList) {
                if (aCatInfo.getCatPath ().equals (fullPath)) 
                    return aCatInfo;
            }
        }

        // potentially possible because currently we can have max 200 category info 
        // due to API limitation
        MessageLogger.logWarning (String.format ("Could not lookup categoryInfo for catpath = %s", fullPath));
        return null;    
    }

    // INTERNAL METHODS
    private QueryExecutorSearchApiResponse executeSearchQuery (String realm) throws Exception {
        SearchQueryExecutor searchQueryExecutor;
        QueryExecutorSearchApiResponse searchApiResponse;

        searchQueryExecutor = new SearchQueryExecutor ();
        searchQueryExecutor.setRealm (realm);

        searchApiResponse = searchQueryExecutor.getSearchResponse (STAR_QUERY, 0, Integer.valueOf (ApiBRData.MAX_ROWS));    // no fq. start, max are not needed
        return searchApiResponse;
    }
}

