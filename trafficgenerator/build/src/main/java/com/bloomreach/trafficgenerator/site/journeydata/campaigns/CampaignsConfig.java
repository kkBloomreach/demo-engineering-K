package com.bloomreach.trafficgenerator.site.journeydata.campaigns;

import com.bloomreach.trafficgenerator.site.journeydata.queryexecutor.*;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.trafficgenerator.MessageLogger;

public class CampaignsConfig {

    private ArrayList <CampaignRecord> campaignRecordList;
    private String realm;   // staging | prod
 
    public CampaignsConfig () {
    }

    public void setRealm (String realm) {
        this.realm = realm;
    }

    public boolean load (String configPath) throws Exception {
        File configFile;

        configFile = new File (configPath);
        if (configFile.exists () == false)
            return false;

        this.campaignRecordList = new ArrayList <CampaignRecord> ();
        parseConfig (configFile);
        return true;
    }

    public ArrayList <CampaignRecord> getCampaignRecords () {
        return this.campaignRecordList;
    }

    private void parseConfig (File configFile) throws Exception {
        BufferedReader reader;
        String srcLine;
        JSONObject configJson;
        StringBuffer configBuf;
        Iterator<String> configKeys;
        CampaignRecord campaignRecord;
        JSONObject campaignJson;

        reader = new BufferedReader (new FileReader (configFile));
        configBuf = new StringBuffer ();
        while ((srcLine = reader.readLine ()) != null) {
            configBuf.append (srcLine);
        }
        reader.close();

        configJson = new JSONObject (configBuf.toString ());
        configKeys = configJson.keys ();    // 'comment' and campaign names
        while (configKeys.hasNext ()) {
            String key;

            key = configKeys.next ();
            if (key.startsWith ("__") == true)  // skip 'comment' keys
                continue;

            campaignJson = configJson.getJSONObject (key);
            campaignRecord = parseCampaignRecord (key, campaignJson);  // parse each campaign's config data
            if (campaignRecord != null)
                this.campaignRecordList.add (campaignRecord);
            else
                MessageLogger.logError (String.format ("Cannot parse campaign record: %s", key));
        }
    }

    private CampaignRecord parseCampaignRecord (String campaignName, JSONObject campaignJson) throws Exception {
        CampaignRecord campaignRecord;
        String startDate;
        String[] startValues;
        int startMonth;
        int startDay;
        int dayCount;
        float priceDiscount;
        JSONArray promotionTermsArray;
        ArrayList<String> productList = new ArrayList <String>();
        ArrayList<String> promotionTerms = new ArrayList <String>();
 
        startDate = campaignJson.getString ("startDate"); // month-day
        startValues = startDate.split ("-");
        startMonth = Integer.parseInt (startValues [0]);
        startDay = Integer.parseInt (startValues [1]);
        dayCount = campaignJson.getInt ("dayCount");
        priceDiscount = (float) (campaignJson.getDouble ("priceDiscount")); // JSONObject getFloat does not exist

        // product selections
        if (campaignJson.has ("productSelections")) {
            JSONObject productSelections;

            productSelections = campaignJson.getJSONObject ("productSelections");
            collectSelectedProducts (productSelections, productList);
        }

        // promotion terms
        if (campaignJson.has ("promotionTerms")) {
            promotionTermsArray = campaignJson.getJSONArray ("promotionTerms");
            promotionTerms = new ArrayList <String> ();
            for (int i = 0; i < promotionTermsArray.length(); i++) {
                String promotionTerm;

                promotionTerm = (String) promotionTermsArray.get (i);
                promotionTerms.add (promotionTerm.trim()); 
            }
        }

        if (productList.size() == 0) {
            MessageLogger.logWarning (String.format ("Product list is empty for campaign: %s", campaignName));
        } else {
            MessageLogger.logInfo (String.format ("Campaign %s has %d products", campaignName, productList.size()));
        }

        campaignRecord = new CampaignRecord (campaignName, startMonth, startDay, dayCount, priceDiscount, 
                                             productList, promotionTerms);

        return campaignRecord;
    }

    private void collectSelectedProducts (JSONObject productSelections, ArrayList<String> productList) throws Exception {
        String viewName = null;

        // "view" if any 
        if (productSelections.has ("view")) {
            viewName = productSelections.getString ("view");
        }

        if (productSelections.has ("include")) {
            JSONObject includeJSONObj;

            includeJSONObj = productSelections.getJSONObject ("include");
            collectIncludedProducts (includeJSONObj, viewName, productList);
        } else {
            MessageLogger.logError ("Cannot find product include list in campaign");
            return;
        }

        if (productSelections.has ("exclude")) {
            JSONObject excludeJSONObj;

            excludeJSONObj = productSelections.getJSONObject ("exclude");
            removeExcludedProducts (excludeJSONObj, viewName, productList);
        }
    }

    private void collectIncludedProducts (JSONObject includeJSONObj, String viewName, ArrayList<String> productList) throws Exception {

        // products-by-category
        if (includeJSONObj.has ("categories")) {
            JSONArray categoriesArray;
            CategoryQueryExecutor categoryQueryExecutor;

            categoryQueryExecutor = new CategoryQueryExecutor ();
            categoryQueryExecutor.setRealm (this.realm);
            categoryQueryExecutor.setView (viewName);

            categoriesArray = includeJSONObj.getJSONArray ("categories");
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject queryTuple;

                // name, value are optional
                queryTuple = categoriesArray.getJSONObject (i);    // { "q": "111222", "name": ..., "value": ...}
                collectProductsInCategory (queryTuple, productList, categoryQueryExecutor);
            }
        }

        // products by attrib-value match
        if (includeJSONObj.has ("search")) {
            JSONArray searchArray;
            SearchQueryExecutor searchQueryExecutor;

            searchQueryExecutor = new SearchQueryExecutor ();
            searchQueryExecutor.setRealm (this.realm);
            searchQueryExecutor.setView (viewName);

            searchArray = includeJSONObj.getJSONArray ("search");
            for (int i = 0; i < searchArray.length(); i++) {
                JSONObject searchTuple;

                // name, value are optional
                searchTuple = searchArray.getJSONObject (i);   // {"q": "XX", "name": "Z", "value": "123"}
                collectProductsBySearch (searchTuple, productList, searchQueryExecutor);
            }
        }

        // pid list
        if (includeJSONObj.has ("pids")) {
            JSONArray pidArray;

            pidArray = includeJSONObj.getJSONArray ("pids");
            for (int i = 0; i < pidArray.length (); i++) {
                String pid;

                pid = pidArray.getString (i).trim();    // "333444"
                if (productList.contains (pid) == false)
                    productList.add (pid);
            }
        }
    }

    // instead of using the Dispatch class, we make API call from this class itself
    // because Dispatch uses userRecord whereas campaigns are independent of any 'user'
    private void collectProductsInCategory (JSONObject queryTuple, ArrayList<String> productList, 
                                            CategoryQueryExecutor categoryQueryExecutor) throws Exception {
        QueryExecutorSearchApiResponse searchApiResponse;
        String catId;
        String fqParam = null;
        int start = 0;
        int maxRows = 100;

        catId = queryTuple.getString ("q");
        if (queryTuple.has ("attribute")) {
            String attrib;
            String value;

            attrib = queryTuple.getString ("attribute");
            value = queryTuple.getString ("value");
            fqParam = String.format ("%s:%s", attrib, value);
        }

        searchApiResponse = categoryQueryExecutor.getSearchResponse (catId, fqParam, start, maxRows);
        if (searchApiResponse != null) {
            int numFound;
            int remCount;

            numFound = searchApiResponse.getNumFound ();
            remCount = numFound;

            while (remCount > 0) {
                searchApiResponse = categoryQueryExecutor.getSearchResponse (catId, fqParam, start, maxRows);
                for (QueryExecutorSearchApiResponseDoc responseDoc : searchApiResponse.getResponseDocs ()) {
                    String pid;

                    pid = responseDoc.getPid ();
                    if (productList.contains (pid) == false)
                        productList.add (pid);
                }
                remCount = remCount - searchApiResponse.getResponseDocs().size(); 
                start = start + searchApiResponse.getResponseDocs().size();
            } 
        }
    }

    private void collectProductsBySearch (JSONObject queryTuple, ArrayList<String> productList,
                                          SearchQueryExecutor searchQueryExecutor) throws Exception {
        QueryExecutorSearchApiResponse searchApiResponse;
        String queryTerm;
        String fqParam = null;
        int start = 0;
        int maxRows = 100;

        queryTerm = queryTuple.getString ("q");
        if (queryTuple.has ("attribute")) {
            String attrib;
            String value;

            attrib = queryTuple.getString ("attribute");
            value = queryTuple.getString ("value");
            fqParam = String.format ("%s:%s", attrib, value);
        }

        searchApiResponse = searchQueryExecutor.getSearchResponse (queryTerm, fqParam, start, maxRows);
        if (searchApiResponse != null) {
            int numFound;
            int remCount;

            numFound = searchApiResponse.getNumFound ();
            remCount = numFound;

            while (remCount > 0) {
                searchApiResponse = searchQueryExecutor.getSearchResponse (queryTerm, fqParam, start, maxRows);
                for (QueryExecutorSearchApiResponseDoc responseDoc : searchApiResponse.getResponseDocs ()) {
                    String pid;

                    pid = responseDoc.getPid ();
                    if (productList.contains (pid) == false)
                        productList.add (pid);
                }
                remCount = remCount - searchApiResponse.getResponseDocs().size(); 
                start = start + searchApiResponse.getResponseDocs().size();
            }
        } 
    }

    private void removeExcludedProducts (JSONObject excludeJSONObj, String viewName, ArrayList<String> productList) throws Exception {

        // products-by-category
        if (excludeJSONObj.has ("categories")) {
            JSONArray categoriesArray;
            CategoryQueryExecutor categoryQueryExecutor;

            categoryQueryExecutor = new CategoryQueryExecutor ();
            categoryQueryExecutor.setRealm (this.realm);
            categoryQueryExecutor.setView (viewName);

            categoriesArray = excludeJSONObj.getJSONArray ("categories");
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject queryTuple;

                // name, value are optional
                queryTuple = categoriesArray.getJSONObject (i);    // { "q": "111222", "name": ..., "value": ...}
                removeProductsInCategory (queryTuple, productList, categoryQueryExecutor);
            }
        }

        // products by attrib-value match
        if (excludeJSONObj.has ("search")) {
            JSONArray searchArray;
            SearchQueryExecutor searchQueryExecutor;

            searchQueryExecutor = new SearchQueryExecutor ();
            searchQueryExecutor.setRealm (this.realm);
            searchQueryExecutor.setView (viewName);

            searchArray = excludeJSONObj.getJSONArray ("search");
            for (int i = 0; i < searchArray.length(); i++) {
                JSONObject searchTuple;

                // name, value are optional
                searchTuple = searchArray.getJSONObject (i);   // {"q": "XX", "name": "Z", "value": "123"}
                removeProductsBySearch (searchTuple, productList, searchQueryExecutor);
            }
        }

        // pid list
        if (excludeJSONObj.has ("pids")) {
            JSONArray pidArray;

            pidArray = excludeJSONObj.getJSONArray ("pids");
            for (int i = 0; i < pidArray.length (); i++) {
                String pid;

                pid = pidArray.getString (i).trim();    // "333444"
                if (productList.contains (pid) == true)
                    productList.remove (pid);
            }
        }
    }

    // instead of using the Dispatch class, we make API call from this class itself
    // because Dispatch uses userRecord whereas campaigns are independent of any 'user'
    private void removeProductsInCategory (JSONObject queryTuple, ArrayList<String> productList, 
                                            CategoryQueryExecutor categoryQueryExecutor) throws Exception {
        QueryExecutorSearchApiResponse searchApiResponse;
        String catId;
        String fqParam = null;
        int start = 0;
        int maxRows = 100;

        catId = queryTuple.getString ("q");
        if (queryTuple.has ("attribute")) {
            String attrib;
            String value;

            attrib = queryTuple.getString ("attribute");
            value = queryTuple.getString ("value");
            fqParam = String.format ("%s:%s", attrib, value);
        }

        searchApiResponse = categoryQueryExecutor.getSearchResponse (catId, fqParam, start, maxRows);
        if (searchApiResponse != null) {
            int numFound;
            int remCount;

            numFound = searchApiResponse.getNumFound ();
            remCount = numFound;

            while (remCount > 0) {
                searchApiResponse = categoryQueryExecutor.getSearchResponse (catId, fqParam, start, maxRows);
                for (QueryExecutorSearchApiResponseDoc responseDoc : searchApiResponse.getResponseDocs ()) {
                    String pid;

                    pid = responseDoc.getPid ();
                    if (productList.contains (pid) == true)
                        productList.remove (pid);
                }
                remCount = remCount - searchApiResponse.getResponseDocs().size(); 
                start = start + searchApiResponse.getResponseDocs().size();
            } 
        }
    }

    private void removeProductsBySearch (JSONObject queryTuple, ArrayList<String> productList,
                                          SearchQueryExecutor searchQueryExecutor) throws Exception {
        QueryExecutorSearchApiResponse searchApiResponse;
        String queryTerm;
        String fqParam = null;
        int start = 0;
        int maxRows = 100;

        queryTerm = queryTuple.getString ("q");
        if (queryTuple.has ("attribute")) {
            String attrib;
            String value;

            attrib = queryTuple.getString ("attribute");
            value = queryTuple.getString ("value");
            fqParam = String.format ("%s:%s", attrib, value);
        }

        searchApiResponse = searchQueryExecutor.getSearchResponse (queryTerm, fqParam, start, maxRows);
        if (searchApiResponse != null) {
            int numFound;
            int remCount;

            numFound = searchApiResponse.getNumFound ();
            remCount = numFound;

            while (remCount > 0) {
                searchApiResponse = searchQueryExecutor.getSearchResponse (queryTerm, fqParam, start, maxRows);
                for (QueryExecutorSearchApiResponseDoc responseDoc : searchApiResponse.getResponseDocs ()) {
                    String pid;

                    pid = responseDoc.getPid ();
                    if (productList.contains (pid) == true)
                        productList.remove (pid);
                }
                remCount = remCount - searchApiResponse.getResponseDocs().size(); 
                start = start + searchApiResponse.getResponseDocs().size();
            }
        } 
    }
}
