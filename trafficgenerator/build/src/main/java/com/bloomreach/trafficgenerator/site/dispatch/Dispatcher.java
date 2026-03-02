// NOTE: For "Search", we collect ALL response docs. For "Suggest", we just make one api call.
// Max number of suggest docs is limited via HT
// NOTE: This class must be thread safe. Same object is used by all threads
package com.bloomreach.trafficgenerator.site.dispatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;  // for debugging only
import java.util.ArrayList;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journeydata.WidgetConfigs;
import com.bloomreach.trafficgenerator.site.journeydata.templates.ApiBRData;
import com.bloomreach.trafficgenerator.site.journeydata.templates.PixelBRData;
import com.bloomreach.trafficgenerator.site.journeylogs.ApiCountLog;

public class Dispatcher {

    private final static int MAX_START_ROW_ALLOWED = 10000; // Bloomreach does not allow "startRow > 10000" in api call.

    ArrayList <String> excludeProducts;
    String region;
    String realm;   //staging/prod
    boolean pixelDebug;
    String pixelEndpoint;
    String searchApiEndpoint;
    String suggestApiEndpoint;
    String widgetApiEndpoint;
    ApiCountLog apiCountLog; // used to monitor QPS

    public Dispatcher () {
        this.excludeProducts = null;    // default
    }

    // region = US, EU
    public void setRegion (String region) throws Exception{
        this.region = region;
    }

    public void setRealm (String realm) throws Exception{
        this.realm = realm;
    }

    public void setPixelDebug (boolean pixelDebug) throws Exception{
        this.pixelDebug = pixelDebug;
    }

    public void setExcludeProducts (ArrayList<String> excludeProducts) {
        this.excludeProducts = excludeProducts; // may be null OR empty
    }

    public void setApiCountLog (ApiCountLog apiCountLog) {
        this.apiCountLog = apiCountLog;
    }

    public void init () throws Exception {
        if ((this.region.equals (GeneratorConstants.REGION_US) == false) &&
            (this.region.equals (GeneratorConstants.REGION_EU) == false)) {
            throw new Exception (String.format ("Dispatcher incorrect region: %s", this.region)); 
        }

        if ((this.realm.equals (GeneratorConstants.REALM_STAGING) == false) &&
            (this.realm.equals (GeneratorConstants.REALM_PROD) == false)) {
            throw new Exception (String.format ("Dispatcher incorrect realm: %s", this.realm)); 
        }

        // pixel api endpoint
        if (region.equals (GeneratorConstants.REGION_US)) {
            if (this.pixelDebug == true)
                this.pixelEndpoint = GeneratorConstants.PIXEL_DEBUGGER_API_ENDPOINT_US;
            else
                this.pixelEndpoint = GeneratorConstants.PIXEL_API_ENDPOINT_US;
        }
        else if (region.equals (GeneratorConstants.REGION_EU)) {
            if (this.pixelDebug == true)
                this.pixelEndpoint = GeneratorConstants.PIXEL_DEBUGGER_API_ENDPOINT_EU;
            else
                this.pixelEndpoint = GeneratorConstants.PIXEL_API_ENDPOINT_EU;
        }

        // search, suggest api endpoint
        if (realm.equals (GeneratorConstants.REALM_STAGING)) {
            this.searchApiEndpoint = GeneratorConstants.DISCOVERY_SEARCH_API_ENDPOINT_STAGING;
            this.suggestApiEndpoint = GeneratorConstants.DISCOVERY_SUGGEST_API_ENDPOINT_STAGING;
        }
        else if (realm.equals (GeneratorConstants.REALM_PROD)) {
            this.searchApiEndpoint = GeneratorConstants.DISCOVERY_SEARCH_API_ENDPOINT_PROD;
            this.suggestApiEndpoint = GeneratorConstants.DISCOVERY_SUGGEST_API_ENDPOINT_PROD;
        }

        // widget api endpoint
        if (realm.equals (GeneratorConstants.REALM_STAGING)) {
            this.widgetApiEndpoint = GeneratorConstants.DISCOVERY_WIDGET_API_ENDPOINT_STAGING;
        }
        else if (realm.equals (GeneratorConstants.REALM_PROD)) {
            this.widgetApiEndpoint = GeneratorConstants.DISCOVERY_WIDGET_API_ENDPOINT_PROD;
        }

    }

    public void dispatchPixel (PixelBRData pixelData) throws Exception {
        String queryParams;

        queryParams = pixelData.constructQueryParams ();
        ensureRefUrlDifferentFromUrl (pixelData);   // check for refUrl == url
        this.apiCountLog.updateCount(ApiCountLog.APITYPE_PIXEL);
        try {
            dispatchPixelApi (this.pixelEndpoint, queryParams, pixelData.getParam (PixelBRData.HEADER_USER_AGENT));
        } catch (Exception e) {
            e.printStackTrace ();
            throw new Exception (String.format ("Pixel dispatcher exception: %s", e.getMessage ()));
        }
    }


    // make as many individual searchApi calls as needed, combine all results into single ('total') response
    // Return value may be null in case of exception
    public SearchApiResponse getSearchApiResponse (ApiBRData apiData)  {
        SearchApiResponse totalSearchApiResponse = null;

        validateSearchApiParams  (apiData);   // currently check q != <blank>
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

        // remove 'excluded-products' (if any) from the response data
        if ((totalSearchApiResponse != null) && (totalSearchApiResponse.getNumFound () > 0)) {
            if ((this.excludeProducts != null) && (this.excludeProducts.size () > 0)) {
                totalSearchApiResponse.removeExcludedProducts (this.excludeProducts);
            }
        }

        MessageLogger.logDebug ("SearchApi dispatcher - done"); 
        return totalSearchApiResponse; // may be null in case of exception
    }

    public SuggestApiResponse getSuggestApiResponse (ApiBRData apiData)  throws Exception {
        String queryParams;
        SuggestApiResponse suggestApiResponse = null;

        validateSuggestApiParams  (apiData);   // currently check q != <blank>
        queryParams = apiData.constructQueryParams ();
        this.apiCountLog.updateCount(ApiCountLog.APITYPE_SUGGEST);
        try {
            suggestApiResponse = dispatchSuggestApi (this.suggestApiEndpoint, queryParams, apiData.getParam (ApiBRData.HEADER_USER_AGENT));
        } catch (Exception e) {
            MessageLogger.logError (String.format ("SuggestApi dispatcher exception: %s, queryTerm = %s",
                                                   e.getMessage (), apiData.getParam (ApiBRData.PARAMNAME_Q)));
            throw new Exception (String.format ("SuggestApi dispatcher exception: %s, queryTerm = %s",
                                                 e.getMessage (), apiData.getParam (ApiBRData.PARAMNAME_Q)));
        }

        return suggestApiResponse;
    }

    // widgetCode == "wcode" (eg, WCODE_PATHWAY_CATEGORY). It is not included in the api call itself
    // widgetApi end point is extended based on wcode (eg, '/category', '/personalize', ...)
    public WidgetApiResponse getWidgetApiResponse (ApiBRData apiData, String widgetCode)  throws Exception {
        String queryParams;
        String actualApiEndpoint;
        String exceptionMsg;
        JSONObject responseJson;
        WidgetApiResponse widgetApiResponse = null;
        String widgetId;

        validateWidgetApiParams  (apiData);   // currently check widgetId != <blank>

        // different widgetTypes have different extensions. Also widgetId is part of apiendpoint
        widgetId = apiData.getParam (ApiBRData.PARAMNAME_WIDGET_WID);
        actualApiEndpoint = constructActualWidgetApiEndpoint (widgetCode, widgetId);
        if (actualApiEndpoint == null) {
            exceptionMsg = String.format ("Cannot construct actual widgetApi endpoint for widgetCode =  %s", widgetCode);
            MessageLogger.logError (exceptionMsg);
            throw new Exception (exceptionMsg);
        }

        apiData.setParam (ApiBRData.PARAMNAME_START, "0"); 
        apiData.setParam (ApiBRData.PARAMNAME_ROWS, ApiBRData.MAX_ROWS);
        queryParams = apiData.constructQueryParams ();
        try {
            // make only one api call (don't need to get entire resultset)
            responseJson = dispatchQueryApi (actualApiEndpoint, queryParams, apiData.getParam (ApiBRData.HEADER_USER_AGENT),
                                             ApiCountLog.APITYPE_WIDGET);
        } catch (Exception e) {
            exceptionMsg = String.format ("WidgetApi dispatcher exception: widgetId %s, msg = %s", widgetId, e.getMessage ());
            MessageLogger.logError (exceptionMsg);
            throw new Exception (exceptionMsg);
        }

        if (responseJson == null) {
            exceptionMsg = String.format ("WidgetApi responseJson null: widgetId = %s", apiData.getParam (ApiBRData.PARAMNAME_WIDGET_WID)); 
            throw new Exception (exceptionMsg);
        }
        else {
            WidgetResponseMetadata widgetResponseMetadata;
            ParsedQueryApiResponse parsedQueryApiResponse;

            parsedQueryApiResponse = parseQueryApiResponse (responseJson); 
            widgetApiResponse = new WidgetApiResponse ();
            widgetApiResponse.setNumFound (parsedQueryApiResponse.getNumFound ());
            widgetApiResponse.addResponseDocs (parsedQueryApiResponse.getResponseDocs ());

            widgetResponseMetadata = new WidgetResponseMetadata ();
            widgetResponseMetadata.setResponseJson (parsedQueryApiResponse.getResponseMetadataJson ()); // contains respone.id,wid,wrid,... 
            widgetApiResponse.setWidgetResponseMetadata (widgetResponseMetadata);
        }
        return widgetApiResponse;
    }

    ///////////
    // INTERNAL METHODS
    private void ensureRefUrlDifferentFromUrl (PixelBRData pixelData) {
        String ref, url, pixelType;
        pixelType = pixelData.getParam (PixelBRData.PARAMNAME_PIXEL_TYPE);
        if (pixelType.equals ("pageview")) {
            ref = pixelData.getParam (PixelBRData.PARAMNAME_REF_URL);
            url = pixelData.getParam (PixelBRData.PARAMNAME_URL);
            if (ref.equals (url) == true) {
                try {
                    String userId = pixelData.getParam (PixelBRData.PARAMNAME_USER_ID);
                    method_to_capture_stacktrace (ref, userId);
                } catch (Exception e) {
                    MessageLogger.logDebug (String.format ("Pageview pixel has ref = url: %s", ref));
                    e.printStackTrace ();
                }
            }
            if (url.equals ("") == true) {
                try {
                    String userId = pixelData.getParam (PixelBRData.PARAMNAME_USER_ID);
                    method_to_capture_stacktrace (ref, userId);
                } catch (Exception e) {
                    MessageLogger.logDebug (String.format ("Pageview pixel api call has BLANK url"));
                    e.printStackTrace ();
                }
            }
        }
    }

    private void validateSearchApiParams  (ApiBRData apiData) {
        String q;

        q = apiData.getParam (ApiBRData.PARAMNAME_Q);
        if ((q == null) || (q.equals (""))) {
            try {
                String ref = apiData.getParam (PixelBRData.PARAMNAME_REF_URL);
                String userId = ""; // not available in searchApi call
                method_to_capture_stacktrace (ref, userId);
            } catch (Exception e) {
                MessageLogger.logDebug (String.format ("Search api call has BLANK query"));
                e.printStackTrace ();
            }
        }
    }

    private void validateSuggestApiParams  (ApiBRData apiData) {
        String q;

        q = apiData.getParam (ApiBRData.PARAMNAME_Q);
        if ((q == null) || (q.equals (""))) {
            try {
                String ref = apiData.getParam (PixelBRData.PARAMNAME_REF_URL);
                String userId = ""; // not available in searchApi call
                method_to_capture_stacktrace (ref, userId);
            } catch (Exception e) {
                MessageLogger.logDebug (String.format ("Suggest api call has BLANK query"));
                e.printStackTrace ();
            }
        }
    }

    private void validateWidgetApiParams  (ApiBRData apiData) {
        String wid;

        wid = apiData.getParam (ApiBRData.PARAMNAME_WIDGET_WID);
        if ((wid == null) || (wid.equals (""))) {
            try {
                String ref = apiData.getParam (PixelBRData.PARAMNAME_REF_URL);
                String userId = ""; // not available in searchApi call
                method_to_capture_stacktrace (ref, userId);
            } catch (Exception e) {
                MessageLogger.logDebug (String.format ("Widget api call has BLANK widgetId"));
                e.printStackTrace ();
            }
        }
    }

    private String constructActualWidgetApiEndpoint (String widgetCode, String widgetId) {
        String extension = null;
        String actualApiEndpoint = null;

        switch (widgetCode) {
            case WidgetConfigs.WCODE_PATHWAY_CATEGORY:
                extension = "category";
                break;
            case WidgetConfigs.WCODE_PATHWAY_KEYWORD:
                extension = "keyword";
                break;
            case WidgetConfigs.WCODE_RECO_GLOBAL_BESTSELLER:
            case WidgetConfigs.WCODE_RECO_GLOBAL_TRENDING:
                extension = "global";
                break;
            case WidgetConfigs.WCODE_RECO_ITEM_FREQ_BOUGHT:
            case WidgetConfigs.WCODE_RECO_ITEM_FREQ_VIEWED:
            case WidgetConfigs.WCODE_RECO_ITEM_SIMILAR:
            case WidgetConfigs.WCODE_RECO_ITEM_EXP:
                extension = "item";
                break;
            case WidgetConfigs.WCODE_RECO_PERS_PAST_PURCHASE:
            case WidgetConfigs.WCODE_RECO_PERS_RECENTLY_VIEWED:
                extension = "personalized";
                break;
            case WidgetConfigs.WCODE_RECO_VISUAL_UPLOAD: // not supported
                extension = "visual/upload";
                break;
            case WidgetConfigs.WCODE_RECO_VISUAL_RECO:
                extension = "visual/search";
                break;
            default: extension = null; 
                break;
        }

        if (extension != null) {
            actualApiEndpoint = String.format ("%s%s/%s", this.widgetApiEndpoint, extension, widgetId);
        } 

        return actualApiEndpoint;
    }


    // queryParams expected to be urlEncoded
    private void dispatchPixelApi (String endPoint, 
                                   String queryParams,
                                   String userAgent) throws Exception {
        String serverUrlStr;
        URL serverUrl;
        HttpsURLConnection conn;
        int responseCode;

        if (this.pixelDebug == true)
            serverUrlStr = String.format ("%s?%s&debug=true", endPoint, queryParams);
        else
            serverUrlStr = String.format ("%s?%s", endPoint, queryParams);
        MessageLogger.logDebug (String.format ("Pixel dispatcher serverUrl %s\n", URLDecoder.decode (serverUrlStr, "UTF-8")));

        serverUrl = new URL (serverUrlStr);
        conn = (HttpsURLConnection) serverUrl.openConnection ();
        conn.setRequestMethod ("GET");
        conn.setRequestProperty ("User-Agent", userAgent);

        MessageLogger.logDebug ("Pixel dispatcher waiting for response...");
        responseCode = conn.getResponseCode ();
        MessageLogger.logDebug (String.format ("Pixel dispatcher received response code %s\n", responseCode));
        if (responseCode != 200) {
            MessageLogger.logError (String.format ("Pixel dispatcher received bad response code %s\n", responseCode));
            conn.disconnect ();
            return;
        }

        conn.disconnect ();
        MessageLogger.logDebug ("Pixel dispatcher - done"); 
        return;
    }

    private SearchApiResponse collectSearchApiResponse (ApiBRData apiData) throws Exception {
        String queryParams;
        SearchApiResponse totalSearchApiResponse;
        int startRow = 0;
        int numRows = Integer.valueOf (ApiBRData.MAX_ROWS);
        int responseDocCount = 0;
        String apiType; // 'category'/'searchterm'
        String searchType;

        searchType = apiData.getParam(ApiBRData.PARAMNAME_SEARCH_TYPE);
        if (searchType.equals (ApiBRData.SEARCH_TYPE_CATEGORY))
            apiType = ApiCountLog.APITYPE_CATEGORY_SEARCH;
        else
            apiType = ApiCountLog.APITYPE_SEARCH_TERM;

        totalSearchApiResponse = new SearchApiResponse ();
        do {
            JSONObject receivedJson = null;

            apiData.setParam (ApiBRData.PARAMNAME_START, Integer.toString (startRow));
            apiData.setParam (ApiBRData.PARAMNAME_ROWS, Integer.toString (numRows));
            queryParams = apiData.constructQueryParams ();

            try {
                receivedJson =  dispatchQueryApi (this.searchApiEndpoint, 
                                                   queryParams, 
                                                   apiData.getParam (ApiBRData.HEADER_USER_AGENT),
                                                   apiType);
            } catch (Exception e) {
                throw new Exception (String.format ("SearchApi dispatcher exception: %s, startRow = %d", e.getMessage (), startRow));
            }

            if (receivedJson == null) 
                throw new Exception (String.format ("SearchApi responseJson null: startRow = %d", startRow));
            else {
                try {
                    ParsedQueryApiResponse parsedQueryApiResponse;
                    SearchResponseMetadata searchResponseMetadata;

                    // parse responseJson and 'combine' that result with 'total'
                    parsedQueryApiResponse = parseQueryApiResponse (receivedJson); 
                    totalSearchApiResponse.setNumFound (parsedQueryApiResponse.getNumFound ());  // will be same value in all individual responses
                    totalSearchApiResponse.addResponseDocs (parsedQueryApiResponse.getResponseDocs ());

                    searchResponseMetadata = new SearchResponseMetadata ();
                    searchResponseMetadata.setResponseMetadataJson (parsedQueryApiResponse.getResponseMetadataJson ());
                    totalSearchApiResponse.setSearchResponseMetadata (searchResponseMetadata);

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
            } 
        } while ((responseDocCount >= numRows) && (startRow < MAX_START_ROW_ALLOWED)); 

        if (startRow >= MAX_START_ROW_ALLOWED) {
            MessageLogger.logDebug ("Stop collecting API response due to Bloomreach startRow limit: " + MAX_START_ROW_ALLOWED);
        }

        return totalSearchApiResponse;
    }


    // individual queryApi call 
    // queryParams expected to be urlEncoded
    // apiType used for QPS tracking
    private JSONObject dispatchQueryApi ( String endPoint, 
                                           String queryParams,
                                           String userAgent,
                                           String apiType) throws Exception {
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

        this.apiCountLog.updateCount(apiType); // collect count to track QPS

        serverUrlStr = String.format ("%s?%s", endPoint, queryParams);
        serverUrl = new URL (serverUrlStr);
        conn = (HttpsURLConnection) serverUrl.openConnection ();
        conn.setRequestMethod ("GET");
        conn.setRequestProperty ("User-Agent", userAgent);

        MessageLogger.logDebug (String.format ("SearchApi dispatcher waiting for response, serverUrl %s\n", 
                                                URLDecoder.decode (serverUrlStr, "UTF-8")));
        responseCode = conn.getResponseCode ();
        if (responseCode != 200) {
            conn.disconnect ();
            throw new Exception (String.format ("SearchApi dispatcher received bad response code %s\n", responseCode));
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
        ArrayList<SearchApiResponseDoc> searchApiResponseDocList;
        ParsedQueryApiResponse parsedQueryApiResponse;

        receivedResponseJson = receivedJson.getJSONObject ("response");
        receivedResponseDocs = (JSONArray) receivedResponseJson.getJSONArray ("docs");

        searchApiResponseDocList = new ArrayList <SearchApiResponseDoc> ();
        for (int i = 0; i < receivedResponseDocs.length (); i++) {
            JSONObject receivedDoc;
            SearchApiResponseDoc searchApiResponseDoc;
            String skuid = null;

            receivedDoc = receivedResponseDocs.getJSONObject (i);
            searchApiResponseDoc = new SearchApiResponseDoc ();

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
                    Double skuPrice = null;
                    Double skuSalePrice = null;

                    // select one of the variants at random
                    randomIndx = (int) (Math.random () * variantsArray.length());
                    selVariant = variantsArray.getJSONObject (randomIndx);
                    skuid = (String) selVariant.get ("skuid");
                    if (selVariant.has ("sku_price"))
                        skuPrice = selVariant.getDouble ("sku_price");
                    if (selVariant.has ("sku_sale_price"))
                        skuSalePrice = selVariant.getDouble ("sku_sale_price");

                    searchApiResponseDoc.setSkuid (skuid);
                    // set product-level price, sale_price = selected variant's price, sale_price
                    if (skuPrice != null)
                        searchApiResponseDoc.setPrice (skuPrice);
                    if (skuSalePrice != null)
                        searchApiResponseDoc.setSalePrice (skuSalePrice);
                }
            }
            searchApiResponseDoc.setSkuid (skuid); // may be null
            searchApiResponseDocList.add (searchApiResponseDoc);   // collect
        }

        parsedQueryApiResponse = new ParsedQueryApiResponse ();
        parsedQueryApiResponse.setNumFound ( receivedResponseJson.getInt ("numFound"));    // numFound
        parsedQueryApiResponse.addResponseDocs (searchApiResponseDocList);
        if (receivedJson.has ("metadata")) 
            parsedQueryApiResponse.setResponseMetadataJson (receivedJson.getJSONObject ("metadata"));

        return parsedQueryApiResponse;
    }

    // queryParams expected to be urlEncoded
    private SuggestApiResponse dispatchSuggestApi (String endPoint, 
                                                   String queryParams,
                                                   String userAgent) throws Exception {
        String serverUrlStr;
        URL serverUrl;
        HttpsURLConnection conn;
        InputStream response;
        InputStreamReader responseReader;
        BufferedReader bufferedReader;
        String inputLine;
        int responseCode;
        SuggestApiResponse suggestApiResponse = null;
        StringBuffer receivedBuf;
        JSONObject receivedJson;

        serverUrlStr = String.format ("%s?%s", endPoint, queryParams);
        serverUrl = new URL (serverUrlStr);
        conn = (HttpsURLConnection) serverUrl.openConnection ();
        conn.setRequestMethod ("GET");
        conn.setRequestProperty ("User-Agent", userAgent);

        MessageLogger.logDebug (String.format ("Suggest api dispatcher waiting for response, serverUrl %s\n", 
                                                    URLDecoder.decode (serverUrlStr,"UTF-8")));
        responseCode = conn.getResponseCode ();
        if (responseCode != 200) {
            MessageLogger.logError (String.format ("Suggest api dispatcher received bad response code %s\n", responseCode));
            conn.disconnect ();
            return suggestApiResponse;
        }

        response = conn.getInputStream ();
        responseReader = new InputStreamReader (response);
        bufferedReader = new BufferedReader (responseReader);
        receivedBuf = new StringBuffer ();
        while ((inputLine = bufferedReader.readLine()) != null) {
            receivedBuf.append (inputLine); 
        }
        response.close ();

        receivedJson = new JSONObject (new String (receivedBuf));
        suggestApiResponse = parseSuggestApiResponse (receivedJson);
        conn.disconnect ();
        MessageLogger.logDebug (String.format ("Suggest api dispatcher received suggest response, done"));
        return suggestApiResponse;
    }

    private SuggestApiResponse parseSuggestApiResponse (JSONObject receivedJson) throws Exception {
        JSONArray suggestionGroupsJsonArray;
        JSONObject suggestionGroupsJson;
        SuggestApiResponse suggestApiResponse;

        suggestApiResponse = new SuggestApiResponse ();

        suggestionGroupsJsonArray = receivedJson.getJSONArray ("suggestionGroups");
        suggestionGroupsJson = (JSONObject) suggestionGroupsJsonArray.get (0); // by default, take 0th element
        if (suggestionGroupsJson.has ("querySuggestions")) {
            JSONArray querySuggestions;
            ArrayList<String> suggestTerms;

            querySuggestions = suggestionGroupsJson.getJSONArray ("querySuggestions");
            suggestTerms = new ArrayList <String> ();
            for (int i = 0; i < querySuggestions.length(); i++) {
                JSONObject aQuerySuggestion;

                aQuerySuggestion = (JSONObject) querySuggestions.get (i);
                suggestTerms.add ((String) aQuerySuggestion.get ("query"));
            }

            suggestApiResponse.setSuggestTerms (suggestTerms);
        }

        // suggest products
        if (suggestionGroupsJson.has ("searchSuggestions")) {
            JSONArray searchSuggestions;
            ArrayList<SuggestProductInfo> suggestProducts;

            searchSuggestions = suggestionGroupsJson.getJSONArray ("searchSuggestions");
            suggestProducts = new ArrayList <SuggestProductInfo> ();
            for (int i = 0; i < searchSuggestions.length(); i++) {
                JSONObject suggestProduct;
                SuggestProductInfo productInfo;

                suggestProduct = (JSONObject) searchSuggestions.get (i);
                productInfo = new SuggestProductInfo ();
                productInfo.setPid ((String) suggestProduct.get ("pid"));
                productInfo.setTitle ((String) suggestProduct.get ("title"));
                productInfo.setUrl ((String) suggestProduct.get ("url"));
                productInfo.setSalePrice (suggestProduct.getDouble ("sale_price"));
                if (suggestProduct.has ("variants")) {
                    JSONArray variantsArray;
                    JSONObject firstVariant;
                    String skuid;

                    variantsArray = suggestProduct.getJSONArray ("variants");
                    if (variantsArray.length() > 0) {
                        firstVariant = (JSONObject) variantsArray.get (0);
                        skuid = (String) firstVariant.get ("skuid");
                        productInfo.setSkuid (skuid);
                    }
                }
                suggestProducts.add (productInfo);
            }

            suggestApiResponse.setSuggestProducts (suggestProducts);
        }

        // suggest catagories ("attributeSuggestions" in actual API response) 
        if (suggestionGroupsJson.has ("attributeSuggestions")) {
            JSONArray attribSuggestions;
            ArrayList<String> suggestCategories;

            attribSuggestions = suggestionGroupsJson.getJSONArray ("attributeSuggestions");
            suggestCategories = new ArrayList <String> ();
            for (int i = 0; i < attribSuggestions.length(); i++) {
                JSONObject aAttribSuggestion;

                aAttribSuggestion = (JSONObject) attribSuggestions.get (i);
                if (aAttribSuggestion.get ("attributeType").equals ("category"))
                    suggestCategories.add ((String) aAttribSuggestion.get ("value"));
            }

            suggestApiResponse.setSuggestCategories (suggestCategories);
        }

        return suggestApiResponse;
    }

    private void method_to_capture_stacktrace (String ref, String userId) throws Exception {
        String consoleMsg;
        SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss,SSS"); // format to match date printed by logger
        consoleMsg = String.format ("ref = url: date = %s, userId = %s, thread: %s, ref = %s\n", 
                                            dateFormat.format (new Date()), userId, Thread.currentThread().getName(), ref);
        // Use System.out so that message is printed in console output along with stack trace
        System.out.print (consoleMsg);
        throw new Exception ("method to capture stacktrace");
    }

}

/********
//     // individual widgetApi call  -- almost exactly same as dispatchSearchApi call, except we don't 'collect' all docs
//     // queryParams expected to be urlEncoded
//     private JSONObject dispatchWidgetApi ( String endPoint, 
//                                            String queryParams,
//                                            String userAgent) throws Exception {
//         String serverUrlStr;
//         URL serverUrl;
//         HttpsURLConnection conn;
//         InputStream response;
//         InputStreamReader responseReader;
//         BufferedReader bufferedReader;
//         String inputLine;
//         int responseCode;
//         StringBuffer receivedBuf;
//         JSONObject receivedJson;
// 
//         serverUrlStr = String.format ("%s?%s", endPoint, queryParams);
//         serverUrl = new URL (serverUrlStr);
//         conn = (HttpsURLConnection) serverUrl.openConnection ();
//         conn.setRequestMethod ("GET");
//         conn.setRequestProperty ("User-Agent", userAgent);
// 
//         MessageLogger.logDebug (String.format ("WidgetApi dispatcher waiting for response, serverUrl %s\n", 
//                                                 URLDecoder.decode (serverUrlStr)));
//         responseCode = conn.getResponseCode ();
//         if (responseCode != 200) {
//             conn.disconnect ();
//             throw new Exception (String.format ("WidgetApi dispatcher received bad response code %s\n", responseCode));
//         }
// 
//         // prepare JSONObject using API response data
//         response = conn.getInputStream ();
//         responseReader = new InputStreamReader (response);
//         bufferedReader = new BufferedReader (responseReader);
//         receivedBuf = new StringBuffer ();
//         while ((inputLine = bufferedReader.readLine()) != null) {
//             receivedBuf.append (inputLine); 
//         }
//         response.close ();
//         conn.disconnect ();
// 
//         receivedJson = new JSONObject (new String (receivedBuf));
//         return receivedJson;    // entire received object
//     }
****/ 
