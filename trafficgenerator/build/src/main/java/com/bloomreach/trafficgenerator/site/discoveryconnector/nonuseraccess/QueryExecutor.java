package com.bloomreach.trafficgenerator.site.discoveryconnector.nonuseraccess;

import java.net.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;
import com.bloomreach.trafficgenerator.site.journeydata.templates.ApiBRData;

public abstract class QueryExecutor {

    private final static String DEFAULT_UID = "111222333444";
    private final static String API_ENDPOINT_STAGING = "https://staging-core.dxpapi.com/api/v1/core/";
    private final static String API_ENDPOINT_PROD = "https://core.dxpapi.com/api/v1/core/";
    private final static int MAX_START_ROW_ALLOWED = 10000; // Bloomreach does not allow "startRow > 10000" in api call.
    private final static long MTB_API_CALL = 100; // just so that Discovery does not return status 429
    

    private String searchType;  // keyword / category, set by derived classes
    private String realm;
    private String view = null;
    private String segment = null;  // currently not used
    private String searchApiEndPoint;

    protected QueryExecutor (String searchType) {
        this.searchType = searchType;
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
        ApiBRData apiData;
        QueryExecutorSearchApiResponse searchApiResponse;

        if (this.realm.equals ("staging"))
            this.searchApiEndPoint = API_ENDPOINT_STAGING;
        else
            this.searchApiEndPoint = API_ENDPOINT_PROD;

        // update query etc in apiData object
        apiData = constructApiData (query, fqParam, start, numRows);

        // collect api response
        searchApiResponse = getSearchApiResponse (apiData); 
        return (searchApiResponse);  // may be null
    }

    // INTERNAL METHODS
    // query, fqParam expected to be urlEncoded as needed
    private ApiBRData constructApiData  (String query, String fqParam, int start, int numRows) throws Exception {
        ApiBRData apiData;
        String cookieStr;

        apiData = new ApiBRData ();
        apiData.setParam (ApiBRData.HEADER_USER_AGENT, GeneratorConstants.USER_AGENT_OTHER);
        apiData.setParam (ApiBRData.PARAMNAME_REQUEST_ID, generateRequestId());

        apiData.setParam (ApiBRData.PARAMNAME_ACCOUNT_ID, SiteConfig.getAccountConfigParam ("ACCOUNT_ID"));
        apiData.setParam (ApiBRData.PARAMNAME_AUTH_KEY, SiteConfig.getAccountConfigParam ("AUTH_KEY"));
        apiData.setParam (ApiBRData.PARAMNAME_DOMAIN_KEY, SiteConfig.getAccountConfigParam ("DOMAIN"));
        apiData.setParam (ApiBRData.PARAMNAME_URL, SiteConfig.getUrlConfigParam ("SITE_CORE_URL"));
        apiData.setParam (ApiBRData.PARAMNAME_REF_URL, SiteConfig.getUrlConfigParam ("SITE_CORE_URL"));
        apiData.setParam (ApiBRData.PARAMNAME_START, Integer.toString (start));
        apiData.setParam (ApiBRData.PARAMNAME_ROWS, Integer.toString (numRows));

        cookieStr = generateCookieString ();
        apiData.setParam (ApiBRData.PARAMNAME_BR_UID2, cookieStr);

        // search type - keyword / category
        apiData.setParam (ApiBRData.PARAMNAME_REQUEST_TYPE, ApiBRData.REQUEST_TYPE_SEARCH);
        apiData.setParam (ApiBRData.PARAMNAME_SEARCH_TYPE, this.searchType);

        // q
        apiData.setParam (ApiBRData.PARAMNAME_Q, URLEncoder.encode (query, "UTF-8"));

        // view_id
        if ((this.view != null) && (this.view.equals ("NONE") == false)) {
            apiData.setParam (ApiBRData.PARAMNAME_VIEW_ID, URLEncoder.encode (this.view, "UTF-8"));
        }

        // fq
        if (fqParam != null)
            apiData.setParam (ApiBRData.PARAMNAME_FQ, URLEncoder.encode (fqParam, "UTF-8"));

        // fl
        apiData.setParam (ApiBRData.PARAMNAME_FL, ApiBRData.DEFAULT_FL_LIST);
        return (apiData);
    }

    // for the purpose of generating campaign products, we don't currently use 'segment'
    // since that essentially changes ranking, but not the recall set
    private String generateCookieString () {
        String cookieStr;

        cookieStr = "uid=" + DEFAULT_UID + ":v=17.0:ts=" + Long.toString (System.currentTimeMillis ()) + ":hc=1";
        return cookieStr;
    }

    private String generateRequestId () {
        long reqIdLong;
        double multFactor;

        multFactor = Math.pow (10, 13);
        reqIdLong = (long) ((Math.random () + 1) * multFactor);
        return (Long.toString (reqIdLong));
    }

    // make as many individual searchApi calls as needed, combine all results into single ('total') response
    // Return value may be null in case of exception
    public QueryExecutorSearchApiResponse getSearchApiResponse (ApiBRData apiData)  {
        QueryExecutorSearchApiResponse totalSearchApiResponse = null;

        try {
            totalSearchApiResponse = collectSearchApiResponse (apiData); 
        } catch (Exception e) {
            String consoleMsg;
            SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss,SSS"); // format to match date printed by logger
            consoleMsg = String.format ("SearchApi exception: message = %s, date = %s, thread: %s, query = %s, ref_url = %s, url = %s\n", 
                                            e.getMessage (),
                                            dateFormat.format (new Date()), Thread.currentThread().getName(), 
                                            apiData.getParam (ApiBRData.PARAMNAME_Q),
                                            apiData.getParam (ApiBRData.PARAMNAME_REF_URL), 
                                            apiData.getParam (ApiBRData.PARAMNAME_URL));
            e.printStackTrace ();
            MessageLogger.logError (consoleMsg);
            totalSearchApiResponse = null;
        }

        MessageLogger.logDebug ("SearchApi DiscoveryUserAccess - done"); 
        return totalSearchApiResponse; // may be null in case of exception
    }

    private QueryExecutorSearchApiResponse collectSearchApiResponse (ApiBRData apiData) throws Exception {
        String queryParams;
        QueryExecutorSearchApiResponse totalSearchApiResponse;
        int startRow = 0;
        int numRows = Integer.valueOf (ApiBRData.MAX_ROWS);
        int responseDocCount = 0;

        totalSearchApiResponse = new QueryExecutorSearchApiResponse ();
        do {
            JSONObject receivedJson = null;

            apiData.setParam (ApiBRData.PARAMNAME_START, Integer.toString (startRow));
            apiData.setParam (ApiBRData.PARAMNAME_ROWS, Integer.toString (numRows));
            queryParams = apiData.constructQueryParams ();

            try {
                receivedJson =  dispatchQueryApi (this.searchApiEndPoint, 
                                                  queryParams);
            } catch (Exception e) {
                throw new Exception (String.format ("SearchApi DiscoveryUserAccess exception: %s, startRow = %d", e.getMessage (), startRow));
            }

            if (receivedJson == null) 
                throw new Exception (String.format ("SearchApi responseJson null: startRow = %d", startRow));

            // when start = 0, we collect category list. Assumption is the category-list
            // remains same in subsequent api calls for the same query
            // category-list is needed by CategoryCollector
            if (startRow == 0) {
                JSONObject receivedFacetCountsJson;
                ArrayList<CategoryInfo> allCategoryList;

                receivedFacetCountsJson = receivedJson.getJSONObject ("facet_counts");
                allCategoryList = collectCategoryList (receivedFacetCountsJson);   // categories from facet list
                totalSearchApiResponse.setResponseCategories (allCategoryList);
            }

            try {
                ParsedQueryApiResponse parsedQueryApiResponse;

                // parse responseJson and 'combine' that result with 'total'
                parsedQueryApiResponse = parseQueryApiResponse (receivedJson); 
                totalSearchApiResponse.setNumFound (parsedQueryApiResponse.getNumFound ());  // will be same value in all individual responses
                totalSearchApiResponse.addResponseDocs (parsedQueryApiResponse.getResponseDocs ());

                responseDocCount = parsedQueryApiResponse.getResponseDocs().size(); // number of doc's in this API response
                MessageLogger.logDebug (String.format ("\tSearchApi received response, query = %s, numFound = %d, startRow = %d, DocCount = %s\n", 
                                                           apiData.getParam (ApiBRData.PARAMNAME_Q), 
                                                           totalSearchApiResponse.getNumFound (), 
                                                           startRow, responseDocCount));
            } catch (Exception e) {
                throw new Exception (String.format ("SearchApi combineApiResponse exception: %s, startRow = %d", e.getMessage (), startRow));
            }

            startRow = startRow + numRows;
            Thread.currentThread().sleep( 500); // avoid BR's QPS limits between repeated api calls
        } while ((responseDocCount >= numRows) && (startRow < MAX_START_ROW_ALLOWED)); 

        if (startRow >= MAX_START_ROW_ALLOWED) {
            MessageLogger.logDebug ("Stop collecting API response due to Bloomreach startRow limit: " + MAX_START_ROW_ALLOWED);
        }

        return totalSearchApiResponse;
    }

    // individual queryApi call 
    // queryParams expected to be urlEncoded
    private JSONObject dispatchQueryApi (String endPoint, 
                                         String queryParams) throws Exception {
        String serverUrlStr;
        URL serverUrl;
        HttpsURLConnection conn;
        InputStream response;
        InputStreamReader responseReader;
        BufferedReader bufferedReader;
        String inputLine;
        int responseCode;
        StringBuffer receivedBuf;
        JSONObject receivedJson;

        serverUrlStr = String.format ("%s?%s", endPoint, queryParams);
        serverUrl = new URL (serverUrlStr);
        conn = (HttpsURLConnection) serverUrl.openConnection ();
        conn.setRequestMethod ("GET");
        MessageLogger.logDebug (String.format ("SearchApi DiscoveryUserAccess waiting for response, serverUrl %s\n", 
                                                URLDecoder.decode (serverUrlStr, "UTF-8")));
        responseCode = conn.getResponseCode ();
        if (responseCode != 200) {
            conn.disconnect ();
            throw new Exception (String.format ("SearchApi DiscoveryUserAccess received bad response code %s\n", responseCode));
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
    private ParsedQueryApiResponse parseQueryApiResponse (JSONObject receivedJson) throws Exception {
        JSONObject receivedResponseJson;
        JSONArray receivedResponseDocs;
        ArrayList<QueryExecutorSearchApiResponseDoc> searchApiResponseDocList;
        ParsedQueryApiResponse parsedQueryApiResponse;

        receivedResponseJson = receivedJson.getJSONObject ("response");
        receivedResponseDocs = (JSONArray) receivedResponseJson.getJSONArray ("docs");

        searchApiResponseDocList = new ArrayList <QueryExecutorSearchApiResponseDoc> ();
        for (int i = 0; i < receivedResponseDocs.length (); i++) {
            JSONObject receivedDoc;
            QueryExecutorSearchApiResponseDoc searchApiResponseDoc;
            String [] flParamList;

            receivedDoc = receivedResponseDocs.getJSONObject (i);
            // skip receivedDoc if docType == "content"
            if (receivedDoc.has ("docType") && receivedDoc.getString ("docType").equals ("content")){
                continue;
            }
            searchApiResponseDoc = new QueryExecutorSearchApiResponseDoc ();
            searchApiResponseDoc.setAvailability (true);    // all docs in API response are 'available' products

            // from the flList, if response has that attribute...
            flParamList = ApiBRData.DEFAULT_FL_LIST.split (",");
            for (int j = 0; j < flParamList.length; j++) {
                String flParam = flParamList [j];

                if (receivedDoc.has (flParam)) {
                    switch (flParam) {
                        case "pid": searchApiResponseDoc.setPid (receivedDoc.optString (flParam)); 
                        break;

                        case "price": searchApiResponseDoc.setPrice (receivedDoc.optDouble (flParam)); 
                        break;

                        case "sale_price": searchApiResponseDoc.setSalePrice (receivedDoc.optDouble (flParam)); 
                        break;

                        case "url": searchApiResponseDoc.setUrl (receivedDoc.optString (flParam)); 
                        break;

                        case "title": searchApiResponseDoc.setTitle (receivedDoc.optString (flParam)); 
                        break;

                        case "style": searchApiResponseDoc.setStyle (receivedDoc.optString (flParam)); 
                        break;
                    }
                }
            }

            if (receivedDoc.has ("variants")) {
                JSONArray variantsArray;

                variantsArray = receivedDoc.getJSONArray ("variants");
                if (variantsArray.length () > 0) {
                    ArrayList <QueryExecutorVariantRecord> productVariants;

                    productVariants = new ArrayList <QueryExecutorVariantRecord> ();
                    for (int k = 0; k < variantsArray.length (); k++) {
                        QueryExecutorVariantRecord variantRecord;
                        JSONObject srcVariant;

                        srcVariant = variantsArray.getJSONObject (k);
                        variantRecord = new QueryExecutorVariantRecord ();

                        variantRecord.setSkuId ((String) srcVariant.get ("skuid"));
                        if (srcVariant.has ("sku_price"))
                            variantRecord.setSkuPrice (srcVariant.getDouble ("sku_price"));
                        if (srcVariant.has ("sku_sale_price"))
                            variantRecord.setSkuSalePrice (srcVariant.getDouble ("sku_sale_price"));

                        productVariants.add (variantRecord);
                    }

                    // variant records -> api response doc
                    searchApiResponseDoc.setVariants (productVariants);
                }
            }

            // NOTE re: views -- Discovery API response does NOT include views for any product
            // therefore we cannot collect that info

            // collect this specific doc into doc-list
            searchApiResponseDocList.add (searchApiResponseDoc);
        }

        parsedQueryApiResponse = new ParsedQueryApiResponse ();
        parsedQueryApiResponse.setNumFound ( receivedResponseJson.getInt ("numFound"));    // numFound
        parsedQueryApiResponse.addResponseDocs (searchApiResponseDocList);

        return parsedQueryApiResponse;
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



