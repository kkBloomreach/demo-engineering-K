package com.bloomreach.trafficgenerator.site.journeydata.queryexecutor;

import java.net.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;

public abstract class QueryExecutor {

    private final static String DEFAULT_UID = "111222333444";
    private final static String API_ENDPOINT_STAGING = "https://staging-core.dxpapi.com/api/v1/core/";
    private final static String API_ENDPOINT_PROD = "https://core.dxpapi.com/api/v1/core/";
    private final static int MAX_START_ROW_ALLOWED = 10000; // Bloomreach does not allow "startRow > 10000" in api call.
    private final static long MTB_API_CALL = 100; // just so that Discovery does not return status 429

    private String apiTemplate; // search or category api call template, from derived class
    private String realm;
    private String view = null;
    private String segment = null;  // currently not used

    protected QueryExecutor (String apiTemplate) {
        this.apiTemplate = apiTemplate;
    }

    public void setRealm (String realm) {
        this.realm = realm;
    }

    public void setSegment (String segment) {
        this.segment = segment;
    }

    public void setView (String view) {
        this.view = view;
    }

    // query is either keyword or catId, as per the derived class
    public QueryExecutorSearchApiResponse getSearchResponse (String query, int start, int numRows ) throws Exception {
        return (getSearchResponse (query, null, start, numRows));
    }

    public QueryExecutorSearchApiResponse getSearchResponse (String query, String fqParam, int start, int numRows) throws Exception {
        String apiCall;
        QueryExecutorSearchApiResponse searchApiResponse;

        apiCall = constructAPICall (query, fqParam, start, numRows);
        searchApiResponse = performQuery (apiCall, numRows, query);  // 'query' included for debugging 
        return (searchApiResponse);  // may be null
    }

    // query, fqParam expected to be urlEncoded as needed
    private String constructAPICall (String query, String fqParam, int start, int numRows) throws Exception {
        String apiCall;
        String cookieStr;

        apiCall = this.apiTemplate;
        if (this.realm.equals ("staging"))
            apiCall = apiCall.replace ("$API_ENDPOINT", API_ENDPOINT_STAGING);
        else
            apiCall = apiCall.replace ("$API_ENDPOINT", API_ENDPOINT_PROD);
        apiCall = apiCall.replace ("$ACCT_ID", SiteConfig.getAccountConfigParam ("ACCOUNT_ID"));
        apiCall = apiCall.replace ("$AUTH_KEY", SiteConfig.getAccountConfigParam ("AUTH_KEY"));
        apiCall = apiCall.replace ("$DOMAIN_KEY", SiteConfig.getAccountConfigParam ("DOMAIN"));
        apiCall = apiCall.replace ("$QUERY", URLEncoder.encode (query, "UTF-8"));    // in case query has blank space etc
        // apiCall = apiCall.replace ("$START", Integer.toString (start));  // changed below for each loop
        // apiCall = apiCall.replace ("$MAX_ROWS", Integer.toString (numRows));
       
        cookieStr = generateCookieString ();
        apiCall = apiCall.replace ("BR_UID_2", cookieStr);

        if ((this.view != null) && (this.view.equals ("NONE") == false)) {
            apiCall = apiCall + "&view=" + URLEncoder.encode (this.view, "UTF-8");
        }

        if (fqParam != null)
            apiCall = apiCall + "&fq=" + URLEncoder.encode (fqParam, "UTF-8");

        MessageLogger.logDebug ("QueryExecutor APICall: " + apiCall);
        return (apiCall);
    }

    // for the purpose of generating campaign products, we don't currently use 'segment'
    // since that essentially changes ranking, but not recall set
    private String generateCookieString () {
        String cookieStr;

        cookieStr = "uid=" + DEFAULT_UID + ":v=12.0:ts=" + Long.toString (System.currentTimeMillis ()) + ":hc=1";
        return cookieStr;
    }

    private QueryExecutorSearchApiResponse performQuery (String apiCall, int numRows, String query) throws Exception {
        QueryExecutorSearchApiResponse totalSearchApiResponse;
        int startRow = 0;
        int responseDocCount = 0;
        String apiCallDup;

        totalSearchApiResponse = new QueryExecutorSearchApiResponse ();
        do {
            JSONObject receivedJson = null;
            apiCallDup = apiCall;   // dup it so we can apply below 'replace' in each loop
            apiCallDup = apiCallDup.replace ("$START", Integer.toString (startRow));
            apiCallDup = apiCallDup.replace ("$MAX_ROWS", Integer.toString (numRows));
            try {
                receivedJson =  dispatchSearchApi (apiCallDup);    // 'user-agent' not necessary in this case

                // when start = 0, we collect category list. Assumption is the category-list
                // remains same in subsequent api calls for the same query
                if (startRow == 0) {
                    JSONObject receivedFacetCountsJson;
                    ArrayList<CategoryInfo> allCategoryList;

                    receivedFacetCountsJson = receivedJson.getJSONObject ("facet_counts");
                    allCategoryList = collectCategoryList (receivedFacetCountsJson);   // categories from facet list
                    totalSearchApiResponse.setResponseCategories (allCategoryList);
                }
            } catch (Exception e) {
                throw new Exception (String.format ("SearchApi dispatcher exception: %s, startRow = %d", e.getMessage (), startRow));
            }

            if (receivedJson == null) 
                throw new Exception (String.format ("SearchApi responseJson null: startRow = %d", startRow));
            else {
                try {
                    // 'combine' returns responseDocCount in single API call
                    responseDocCount = combineSearchApiResponse (receivedJson, totalSearchApiResponse);
                    MessageLogger.logDebug (String.format ("\tQueryExecutor searchApi received response, query = %s, numFound = %d, startRow = %d, DocCount = %s\n", 
                                                           query, totalSearchApiResponse.getNumFound (), startRow, responseDocCount));
                } catch (Exception e) {
                    throw new Exception (String.format ("SearchApi combineApiResponse exception: %s, startRow = %d", e.getMessage (), startRow));
                }

                startRow = startRow + numRows;
                try {
                    Thread.currentThread().sleep(MTB_API_CALL);
                } catch (InterruptedException e) {
                    MessageLogger.logWarning( "QueryExecutor apicall interrupted");
                }
            } 
        } while ((responseDocCount >= numRows) && (startRow < MAX_START_ROW_ALLOWED));

        if (startRow >= MAX_START_ROW_ALLOWED) {
            MessageLogger.logDebug ("QueryExecutor stop collecting API response due to Bloomreach startRow limit: " + MAX_START_ROW_ALLOWED);
        }

        return totalSearchApiResponse;
    }

    // individual searchApi call 
    // queryParams expected to be urlEncoded
    private JSONObject dispatchSearchApi (String apiCall) throws Exception {
        URL serverUrl;
        HttpURLConnection conn;
        InputStream response;
        InputStreamReader responseReader;
        BufferedReader bufferedReader;
        String inputLine;
        int responseCode;
        StringBuffer receivedBuf;
        JSONObject receivedJson;

        serverUrl = new URL (apiCall);
        conn = (HttpURLConnection) serverUrl.openConnection ();
        conn.setRequestMethod ("GET");

        MessageLogger.logDebug (String.format ("QueryExecutor searchApi dispatcher waiting for response, serverUrl %s\n", URLDecoder.decode (apiCall, "UTF-8")));
        responseCode = conn.getResponseCode ();
        if (responseCode != 200) {
            conn.disconnect ();
            throw new Exception (String.format ("QueryExecutor searchApi dispatcher received bad response code %s\n", responseCode));
        }

        // prepare JSONObject using API response data
        response = conn.getInputStream ();
        responseReader = new InputStreamReader (response);
        bufferedReader = new BufferedReader (responseReader);
        receivedBuf = new StringBuffer ();
        while ((inputLine = bufferedReader.readLine()) != null) {
            receivedBuf.append (inputLine); 
        }
        response.close ();
        conn.disconnect ();

        receivedJson = new JSONObject (new String (receivedBuf));
        return receivedJson;    // entire received object
    }

    // receivedJson is entire received object in a single API call
    // combine this responce into 'total'
    private int combineSearchApiResponse (JSONObject receivedJson, QueryExecutorSearchApiResponse totalSearchApiResponse) throws Exception {
        JSONObject receivedResponseJson;
        JSONArray receivedResponseDocs;
        ArrayList<QueryExecutorSearchApiResponseDoc> searchApiResponseDocList;

        receivedResponseJson = receivedJson.getJSONObject ("response");
        receivedResponseDocs = (JSONArray) receivedResponseJson.getJSONArray ("docs");

        searchApiResponseDocList = new ArrayList <QueryExecutorSearchApiResponseDoc> ();
        for (int i = 0; i < receivedResponseDocs.length (); i++) {
            JSONObject receivedDoc;
            QueryExecutorSearchApiResponseDoc searchApiResponseDoc;
            String skuid = null;

            receivedDoc = receivedResponseDocs.getJSONObject (i);
            searchApiResponseDoc = new QueryExecutorSearchApiResponseDoc ();

            searchApiResponseDoc.setPid (receivedDoc.getString ("pid"));
            searchApiResponseDoc.setPrice (receivedDoc.getDouble ("price"));
            searchApiResponseDoc.setSalePrice(receivedDoc.getDouble ("sale_price"));
            searchApiResponseDoc.setUrl (receivedDoc.getString ("url"));
            searchApiResponseDoc.setTitle (receivedDoc.getString ("title"));
            if (receivedDoc.has ("variants")) {
                JSONArray variantsArray;

                variantsArray = receivedDoc.getJSONArray ("variants");
                if (variantsArray.length () > 0) {
                    JSONObject selVariant;
                    int randomIndx;

                    randomIndx = (int) (Math.random () * variantsArray.length());
                    selVariant = variantsArray.getJSONObject (randomIndx);
                    skuid = (String) selVariant.get ("skuid");
                    searchApiResponseDoc.setSkuid (skuid);
                }
            }
            searchApiResponseDoc.setSkuid (skuid); // may be null
            searchApiResponseDocList.add (searchApiResponseDoc);   // collect
        }

        // add (combine) this reponseDocList to 'total' list
        totalSearchApiResponse.setNumFound (receivedResponseJson.getInt ("numFound"));    // numFound
        totalSearchApiResponse.addResponseDocs (searchApiResponseDocList);

        return searchApiResponseDocList.size(); // number of doc's in this API response
    }

    // facet keyword may be 'facet_fields' (v1 format) or 'facets' (v2 format)
    ArrayList <CategoryInfo> collectCategoryList (JSONObject receivedFacetCountsJson) throws Exception {
        ArrayList <CategoryInfo> categoryInfoList = null;
        JSONArray categoryJsonArray;

        if (receivedFacetCountsJson.has ("facet_fields")) {
            JSONObject facetFieldsJson;

            // old response format
            // facet_fields: { "category": [ {}, {}, ... ] }
            facetFieldsJson = receivedFacetCountsJson.getJSONObject ("facet_fields");
            if (facetFieldsJson.has("category")) {
                categoryJsonArray = facetFieldsJson.getJSONArray ("category");
                categoryInfoList = parseCategoryJsonArray (categoryJsonArray);
            } else {
                MessageLogger.logWarning("category facet missing in facet_fields");
            }
        } else if (receivedFacetCountsJson.has ("facets")) {
            JSONArray facetsJsonArray;

            // new response format
            // facets [ {"name": ..., "value": [] }, {}, {} ]. Look for facet named "category"
            facetsJsonArray = receivedFacetCountsJson.getJSONArray ("facets");
            for (int i = 0; i < facetsJsonArray.length(); i++) {
                String facetName;

                facetName = facetsJsonArray.getJSONObject (i).getString ("name");
                if (facetName.equals ("category")) {
                    categoryJsonArray = facetsJsonArray.getJSONObject (i).getJSONArray ("value");
                    categoryInfoList = parseCategoryJsonArray (categoryJsonArray);
                    break;  // don't need to continue looking at remaining facets
                }
            }
        } else {
            MessageLogger.logError ("QueryExecutor unknown facet fields in API response");
        }

        return categoryInfoList;
    }

    private ArrayList <CategoryInfo> parseCategoryJsonArray (JSONArray categoryJsonArray) throws Exception {
        ArrayList <CategoryInfo> categoryList;

        categoryList = new ArrayList <CategoryInfo> ();
        for (int i = 0; i < categoryJsonArray.length(); i++) {
            JSONObject categoryJson;
            CategoryInfo categoryInfo;

            categoryJson = categoryJsonArray.getJSONObject (i);
            categoryInfo = parseCategoryJson (categoryJson);
            if (categoryList.contains (categoryInfo) == false)
                categoryList.add (categoryInfo);
        }

        return categoryList;
    }

    private CategoryInfo parseCategoryJson (JSONObject categoryJson) throws Exception {
        String catId;
        String catName;
        String parentCatId;
        String treePath;
        int level;
        String catFullPath = null;
        String[] nodeList;
        CategoryInfo categoryInfo;

        catId = categoryJson.getString ("cat_id");
        catName = categoryJson.getString ("cat_name");
        parentCatId = categoryJson.getString ("parent");

        // tree_path: "/<catId>,<catName>/<catId>,<catName>/...
        // tree_path includes catId (ie, it is full path that includes childId too)
        treePath = categoryJson.getString ("tree_path");
        treePath = treePath.substring (1);  // skip leading '/'
        nodeList = treePath.split ("/");
        for (int j = 0; j < nodeList.length; j++) {
            String idAndName;
            String nodeId;
            String nodeName;
            int indx;

            idAndName = nodeList [j];
            if (idAndName.startsWith ("/"))
                idAndName = idAndName.substring (1);

            indx = idAndName.indexOf (",");    // this assumes nodeId,Name themselves do not include a ','
            nodeId = idAndName.substring (0, indx);
            nodeName = idAndName.substring (indx+1);
 
            if (catFullPath == null)
                catFullPath = nodeName;
            else
                catFullPath = catFullPath + "/" + nodeName; // delimiter = "/"
        } 

        // level == nodeList.length (0, 1, ...)
        level = nodeList.length;

        categoryInfo = new CategoryInfo (level, catId, catName, catFullPath, parentCatId);
        return categoryInfo;
    }
}

// 
// 
// 
// =================
//     private SearchApiResponse performQuery_ORIG (String apiCall) throws Exception {
//         URL apiURL;
//         HttpURLConnection urlConn;
//         BufferedReader respReader;
//         InputStream inStream;
//         StringBuffer respBuffer;
//         String respText;
//         String pid = null;
//         SearchApiResponse searchApiResponse = null;
//         
//         apiURL = new URL (apiCall);
//         urlConn = (HttpURLConnection) apiURL.openConnection (); 
//         urlConn.setRequestMethod ("GET");
//         urlConn.setRequestProperty ("Content-Type", "application/json");
// 
//         if (urlConn.getResponseCode () == 200) {
//             String inputLine;
//             JSONObject respJson;
//             JSONArray respDocs;
//             JSONObject respObj;
//             JSONObject respDoc0;
// 
//             inStream = urlConn.getInputStream ();
//             respReader = new BufferedReader (new InputStreamReader (inStream));
//             respBuffer = new StringBuffer ();
//             while ((inputLine = respReader.readLine ()) != null) {
//                 respBuffer.append (inputLine);
//             }
//             inStream.close ();
// 
//             respJson = new JSONObject (respBuffer.toString ());
//             searchApiResponse = buildSearchApiResponse (respJson);
//         } else {
//             MessageLogger.logError ("Got notOK status = " + urlConn.getResponseCode());
//         }
// 
//         urlConn.disconnect ();
//         return (searchApiResponse);
//     }
// 
//     private SearchApiResponse buildSearchApiResponse (JSONObject receivedJson) throws Exception {
//         SearchApiResponse searchApiResponse;
//         JSONObject receivedResponseJson;
//         JSONObject receivedFacetCountsJson;
//         ArrayList<SearchApiResponseDoc> allSearchApiResponseDocs;
//         ArrayList<CategoryInfo> allCategoryList;
// 
//         receivedResponseJson = receivedJson.getJSONObject ("response");
//         allSearchApiResponseDocs = collectResponseDocs (receivedResponseJson);  // docs
// 
//         receivedFacetCountsJson = receivedJson.getJSONObject ("facet_counts");
//         allCategoryList = collectCategoryList (receivedFacetCountsJson);   // categories from facet list
// 
//         searchApiResponse = new SearchApiResponse ();
//         searchApiResponse.setNumFound (receivedResponseJson.getInt ("numFound"));    // numFound
//         searchApiResponse.setResponseDocs (allSearchApiResponseDocs);
//         searchApiResponse.setResponseCategories (allCategoryList);
// 
//         return searchApiResponse;
//     }
// 
//     private ArrayList <SearchApiResponseDoc> collectResponseDocs (JSONObject receivedResponseJson) throws Exception {
//         JSONArray receivedResponseDocs;
//         ArrayList<SearchApiResponseDoc> allSearchApiResponseDocs;
// 
//         receivedResponseDocs = (JSONArray) receivedResponseJson.getJSONArray ("docs");
//         allSearchApiResponseDocs = new ArrayList <SearchApiResponseDoc> ();
// 
//         for (int i = 0; i < receivedResponseDocs.length (); i++) {
//             JSONObject receivedDoc;
//             SearchApiResponseDoc searchApiResponseDoc;
//             String skuid = null;
// 
//             receivedDoc = receivedResponseDocs.getJSONObject (i);
//             searchApiResponseDoc = new SearchApiResponseDoc ();
// 
//             // make sure apiTemplate->fl has corresponding attribs 
//             searchApiResponseDoc.setPid (receivedDoc.getString ("pid"));
//             searchApiResponseDoc.setPrice (receivedDoc.getDouble ("price"));
//             searchApiResponseDoc.setSalePrice(receivedDoc.getDouble ("sale_price"));
//             searchApiResponseDoc.setUrl (receivedDoc.getString ("url"));
//             searchApiResponseDoc.setTitle (receivedDoc.getString ("title"));
//             if (receivedDoc.has ("variants")) {
//                 JSONArray variantsArray;
// 
//                 variantsArray = receivedDoc.getJSONArray ("variants");
//                 if (variantsArray.length () > 0) {
//                     JSONObject selVariant;
//                     int randomIndx;
// 
//                     randomIndx = (int) (Math.random () * variantsArray.length());
//                     selVariant = variantsArray.getJSONObject (randomIndx);
//                     skuid = (String) selVariant.get ("skuid");
//                     searchApiResponseDoc.setSkuid (skuid);
//                 }
//             }
//             searchApiResponseDoc.setSkuid (skuid); // may be null
//             allSearchApiResponseDocs.add (searchApiResponseDoc);   // collect
//         }
//         return allSearchApiResponseDocs;
//     }
// //=================
// 
