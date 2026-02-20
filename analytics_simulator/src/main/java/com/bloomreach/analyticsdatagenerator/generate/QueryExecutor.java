package com.bloomreach.analyticsdatagenerator.generate;

import java.net.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;
import com.bloomreach.analyticsdatagenerator.MessageLogger;

public abstract class QueryExecutor {

    private final static String DEFAULT_UID = "111222333444";

    private GeneratorInputData inputData;
    private String apiTemplate;

    protected QueryExecutor (String apiTemplate, GeneratorInputData inputData) {
        this.apiTemplate = apiTemplate;
        this.inputData = inputData;
    }

    // query is either keyword or catId, as per the derived class
    public SearchQueryResponseInfo getQueryResponseInfo (String query, String view, String segment, String fqParam) throws Exception {
        String apiCall;
        ApiResponseInfo apiResponseInfo;

        apiCall = constructAPICall (query, view, segment, fqParam);
        apiResponseInfo = performQuery (apiCall);
        if (apiResponseInfo != null) {
            SearchQueryResponseDoc[] queryResponseDocs = null;
            SearchQueryResponseInfo queryResponseInfo;

            queryResponseDocs = getSampleResponseDocs (apiResponseInfo); // may be null if apiResponse has 0 docs
            queryResponseInfo = new SearchQueryResponseInfo (apiResponseInfo.getNumFound (), queryResponseDocs);
            if (apiResponseInfo.getNumFound () == 0) {
                MessageLogger.logInfo ("Empty result. Query = " + query + ", view = " + view + ", segment = " + segment + 
                                    ", fq= " + fqParam + ", numFound = 0");
            }
            return queryResponseInfo;
        } else {
            MessageLogger.logError ("null apiResponseInfo. Query = " + query + ", view = " + view + ", segment = " + segment + ", fq= " + fqParam);
        }
        return (null);  // server response error
    }

    // query, fqParam expected to be urlEncoded as needed
    private String constructAPICall (String query, String view, String segment, String fqParam) {
        String apiCall;
        String cookieStr;

        apiCall = this.apiTemplate;
        apiCall = apiCall.replace ("$ACCT_ID", Integer.toString (this.inputData.getAcctId ()));
        apiCall = apiCall.replace ("$AUTH_KEY", this.inputData.getAuthKey ());
        apiCall = apiCall.replace ("$DOMAIN_KEY", this.inputData.getDomainKey ());
        apiCall = apiCall.replace ("$QUERY", URLEncoder.encode (query));    // in case query has blank space etc
       
        cookieStr = generateCookieString (segment);
        apiCall = apiCall.replace ("BR_UID_2", cookieStr);

        if ((view != null) && (view.equals ("NONE") == false)) {
            apiCall = apiCall + "&view=" + URLEncoder.encode (view);
        }

        if (fqParam != null)
            apiCall = apiCall + "&fq=" + fqParam;

        MessageLogger.logDebug ("APICall: " + apiCall);
        return (apiCall);
    }

    // for the purpose of generating simulation data, we don't use 'segment'
    // since that essentially changes ranking, but not recall set
    private String generateCookieString (String segment) {
        String cookieStr;

        cookieStr = "uid=" + DEFAULT_UID + ":v=12.0:ts=" + Long.toString (System.currentTimeMillis ()) + ":hc=1";
        return cookieStr;
    }

    private ApiResponseInfo performQuery (String apiCall) throws Exception {
        URL apiURL;
        HttpURLConnection urlConn;
        BufferedReader respReader;
        InputStream inStream;
        StringBuffer respBuffer;
        String respText;
        String pid = null;
        
        apiURL = new URL (apiCall);
        urlConn = (HttpURLConnection) apiURL.openConnection (); 
        urlConn.setRequestMethod ("GET");
        urlConn.setRequestProperty ("Content-Type", "application/json");

        if (urlConn.getResponseCode () == 200) {
            String inputLine;
            JSONObject respJson;
            JSONArray respDocs;
            JSONObject respObj;
            JSONObject respDoc0;
            ApiResponseInfo responseInfo;

            inStream = urlConn.getInputStream ();
            respReader = new BufferedReader (new InputStreamReader (inStream));
            respBuffer = new StringBuffer ();
            while ((inputLine = respReader.readLine ()) != null) {
                respBuffer.append (inputLine);
            }
            inStream.close ();

            respJson = new JSONObject (respBuffer.toString ());
            respObj = respJson.getJSONObject ("response");
            respDocs = respObj.getJSONArray ("docs");
            responseInfo = new ApiResponseInfo (respObj.getInt ("numFound"), respDocs);
            return (responseInfo);
        } else {
            MessageLogger.logError ("Got notOK status = " + urlConn.getResponseCode());
        }

        return (null);
    }

    // convert JSONArray to individual records. May return null if respDocs.length = 0
    private SearchQueryResponseDoc[] getSampleResponseDocs (ApiResponseInfo apiResponseInfo) throws Exception {
        JSONArray respDocs = apiResponseInfo.getResponseDocs ();

        if (respDocs.length() > 0) {
            SearchQueryResponseDoc[] retList = new SearchQueryResponseDoc [respDocs.length()];

            for (int i = 0; i < respDocs.length(); i++) {
                JSONObject oneRespDoc;
                SearchQueryResponseDoc queryResponseDoc;
                String variantId = null;
                JSONArray variantsArray;

                oneRespDoc = respDocs.getJSONObject(i);

                // if product has variants, take the first one. Needed to generate refUrl as "pid__variant"
                if (oneRespDoc.has ("variants")) {
                    variantsArray = oneRespDoc.getJSONArray ("variants");
                    if (variantsArray.length() > 0) {
                        JSONObject firstVariantJson;

                        firstVariantJson = variantsArray.getJSONObject (0);
                        if (firstVariantJson.has ("skuid"))
                            variantId = firstVariantJson.getString ("skuid");
                    }
                }

                queryResponseDoc = new SearchQueryResponseDoc (
                                                oneRespDoc.getString ("pid"),
                                                variantId,
                                                oneRespDoc.getDouble ("price"),
                                                oneRespDoc.getString ("brand"),
                                                oneRespDoc.getString ("title"));
                retList [i] = queryResponseDoc;
            }

            return (retList);
        }

        MessageLogger.logWarning ("Insufficient number of docs in result: " + apiResponseInfo.getNumFound ());
        return (null);
    }

    ////////////////////////////
    class ApiResponseInfo {
        int numFound = 0;
        JSONArray responseDocs = null;

        ApiResponseInfo (int numFound, JSONArray respDocs) {
            this.numFound = numFound;
            this.responseDocs = respDocs;
        }

        public int getNumFound () {
            return this.numFound;
        }

        public JSONArray getResponseDocs () {
            return this.responseDocs;
        }
    }
}
