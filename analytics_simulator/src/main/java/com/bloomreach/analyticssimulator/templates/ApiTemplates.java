package com.bloomreach.analyticssimulator.templates;

// load api templates into ApiLog.Builder object
// Currently we have these templates - search API 
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URLEncoder;

import org.json.JSONObject;
import org.json.JSONArray;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;


import com.bloomreach.proto.MobileApi.ExParam;
import com.bloomreach.proto.MobileApi.ApiLog;
import com.bloomreach.proto.MobileApi.ApiLog.Builder;
import com.bloomreach.proto.MobileApi.CommonRequest;
import com.bloomreach.proto.MobileApi.SearchRequest;
import com.bloomreach.proto.MobileApi.ApiRequest;
import com.bloomreach.proto.MobileApi.Pagination;
import com.bloomreach.proto.MobileApi.FieldName;
import com.bloomreach.proto.MobileApi.CommonRequest.RequestType;
import com.bloomreach.proto.MobileApi.SearchRequest.SearchType;
import com.bloomreach.proto.MobileApi.CommonRequest.ResponseType;
import com.bloomreach.proto.MobileApi.ApiResponse;
import com.bloomreach.proto.MobileApi.CommonResponse;
import com.bloomreach.proto.MobileApi.SearchResponse;
import com.bloomreach.proto.MobileApi.ProductResponse;
import com.bloomreach.proto.MobileApi.ResponseMetrics;

import com.bloomreach.analyticssimulator.MessageLogger;

public class ApiTemplates {

    // These template files are expected to be in the 'templateDir"
    // Since the APILog syntax is complex, we have converted one log file to JSON
    // format manually and then use JSON to parse it.
    private final static String FILENAME_SEARCH_APILOG_JSON = "simtemplate_searchapi.json";
    private final static String FILENAME_CATEGORY_APILOG_JSON = "simtemplate_categoryapi.json";

    private String templateDirPath = null;

    HashMap<String, String> cloneKeyValues = null;
    HashMap<String, String> cloneParams = null;

    public ApiTemplates () {
    }

    // directory containing all the template files
    public void setTemplatesDir (String templateDirPath) {
        this.templateDirPath = templateDirPath;
    }

    public ApiLog.Builder loadSearchApiTemplate () throws Exception {
        ApiLog.Builder searchApiLogBuilder;

        searchApiLogBuilder = loadApiLogTemplate (templateDirPath, FILENAME_SEARCH_APILOG_JSON);
        return (searchApiLogBuilder);
    }

    public ApiLog.Builder loadCategoryApiTemplate () throws Exception {
        ApiLog.Builder categoryApiLogBuilder;

        categoryApiLogBuilder = loadApiLogTemplate (templateDirPath, FILENAME_CATEGORY_APILOG_JSON);
        return (categoryApiLogBuilder);
    }

    /**
     * Parses the incoming template log file (JSON format) and returns a ApiLog.Builder.
    */
    private ApiLog.Builder loadApiLogTemplate (String templateDirPath, String templatePath ) throws Exception {
        File templateFile;
        JSONObject apiTemplateJson;
        ApiLog.Builder templateApiLogBuilder = null;

        templateFile = new File (templateDirPath, templatePath);

        try {
            apiTemplateJson = parseApiJsonLogFile (templateFile);
        } catch (Exception e) {
            MessageLogger.logError ("Exception in parse api template: " + e.getMessage());
            return (null);
        } 

        // Use the JSONobject values to populate apiLogBuilder
        // and return that builder object
        try {
            templateApiLogBuilder = prepareApiLogBuilder (apiTemplateJson);
        } catch (Exception e) {
            MessageLogger.logError ("Exception in prepare template api builder: " + e.getMessage());
            return (null);
        }

        return (templateApiLogBuilder);
    }

    private JSONObject parseApiJsonLogFile (File templateFile) throws Exception {
        InputStream templateInputStream;
        BufferedReader templateReader;
        StringBuffer templateBuffer;
        String templateLine;
        JSONObject templateJson;

        templateInputStream = new FileInputStream (templateFile);
        templateReader = new BufferedReader (new InputStreamReader (templateInputStream));
        templateBuffer = new StringBuffer ();
        while ((templateLine = templateReader.readLine ()) != null) {
            templateBuffer.append (templateLine);
        }    
        templateInputStream.close ();

        templateJson = new JSONObject (templateBuffer.toString ());
        return (templateJson); 
    }


    // this is a common method used by all templets 
    // Use the JSONobject prepared by the parser and generate a ApiLog.Builder object
    private ApiLog.Builder prepareApiLogBuilder (JSONObject templateJson) throws Exception {
        Iterator<String> templateKeys;
        String value;
        ApiLog.Builder apiLogBuilder;

        apiLogBuilder = ApiLog.newBuilder ();

        templateKeys = templateJson.keys ();
        while (templateKeys.hasNext ()) {
            String key = templateKeys.next ();
            MessageLogger.logDebug ("template key: " + key);
            switch (key) {
                case "request": 
                        JSONObject requestTemplateJson;
                        requestTemplateJson = (JSONObject) templateJson.get (key);
                        processRequestTemplate (requestTemplateJson, apiLogBuilder); 
                        break;

                case "response": 
                        JSONObject responseTemplateJson;
                        responseTemplateJson = (JSONObject) templateJson.get (key);
                        processResponseTemplate (responseTemplateJson, apiLogBuilder); 
                        break;
                default: MessageLogger.logWarning ("Unknown key in apiTemplate: " + key);
            }
        }

        return apiLogBuilder;
    }

    private void processRequestTemplate (JSONObject requestTemplateJson, ApiLog.Builder apiLogBuilder) throws Exception {
        ApiRequest.Builder apiRequestBuilder;
        CommonRequest.Builder commonRequestBuilder;
        SearchRequest.Builder searchRequestBuilder;
        Iterator<String> templateKeys;

        apiRequestBuilder = apiLogBuilder.getRequestBuilder ();
        commonRequestBuilder = apiRequestBuilder.getCommonBuilder ();
        searchRequestBuilder = apiRequestBuilder.getSearchBuilder ();

        templateKeys = requestTemplateJson.keys ();
        while (templateKeys.hasNext ()) {
            String key = templateKeys.next ();
            MessageLogger.logDebug ("requestTemplate key: " + key);
            switch (key) {
                case "common":
                        JSONObject requestCommonTemplateJson;
                        requestCommonTemplateJson = (JSONObject) requestTemplateJson.get ("common");
                        processRequestCommonTemplate (requestCommonTemplateJson, commonRequestBuilder); 
                        break;
                case "search": 
                        JSONObject requestSearchTemplateJson;
                        requestSearchTemplateJson = (JSONObject) requestTemplateJson.get ("search");
                        processRequestSearchTemplate (requestSearchTemplateJson, searchRequestBuilder); 
                        break;
                case "widget":
                        MessageLogger.logDebug ("request -> widget skipped"); 
                        break;
                default: MessageLogger.logWarning ("Unknown key in apiTemplate: " + key);
            }
        }
    }

    private void processRequestCommonTemplate (JSONObject requestCommonTemplateJson, CommonRequest.Builder commonRequestBuilder) throws Exception {
        Iterator<String> templateKeys;

        templateKeys = requestCommonTemplateJson.keys ();
        while (templateKeys.hasNext ()) {
            int intValue;
            String strValue;
            long longValue;

            String key = templateKeys.next ();
            MessageLogger.logDebug ("requestCommonTemplate key: " + key);
            switch (key) {
                case "account_id":
                        intValue = (int) requestCommonTemplateJson.get (key);
                        MessageLogger.logDebug ("request->common->account_id: " + intValue);
                        commonRequestBuilder.setAccountId (intValue);
                        break;
                case "auth_key":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setAuthKey (strValue);
                        break;
                case "request_id":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setRequestId (strValue);
                        break;
                case "user_agent":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setUserAgent (strValue);
                        break;
                case "url":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setUrl (strValue);
                        break;
                case "ref_url":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setRefUrl (strValue);
                        break;
                case "cookie2":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setCookie2 (strValue);
                        break;
                case "request_type":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        if (strValue.equals ("SEARCH") == true) {
                            commonRequestBuilder.setRequestType (RequestType.SEARCH);
                        } else {
                            MessageLogger.logWarning ("Unknown request type: " + strValue);
                        }
                        break;
                case "log_time":
                        longValue = (long) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setLogTime(longValue);
                        break;
                case "request_path":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setRequestPath (strValue);
                        break;
                case "request_data_json":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setRequestDataJson (strValue);
                        break;
                case "wt":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        if (strValue.equals ("JSON") == true) {
                            commonRequestBuilder.setWt (ResponseType.JSON);
                        } else {
                            MessageLogger.logWarning ("Unknown request->common->response type: " + strValue);
                        }
                        break;
                case "site_domain_key":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setSiteDomainKey (strValue);
                        break;
                case "catalog_name":
                        strValue = (String) requestCommonTemplateJson.get (key);
                        commonRequestBuilder.setCatalogName (strValue);
                        break;
            }
        }
    }

    private void processRequestSearchTemplate (JSONObject requestSearchTemplateJson, SearchRequest.Builder searchRequestBuilder) throws Exception {
        Iterator<String> templateKeys;

        templateKeys = requestSearchTemplateJson.keys ();
        while (templateKeys.hasNext ()) {
            int intValue;
            String strValue;
            long longValue;

            String key = templateKeys.next ();
            MessageLogger.logDebug ("requestSearchTemplate key: " + key);
            switch (key) {
                case "search_type":
                        strValue = (String) requestSearchTemplateJson.get (key);
                        if (strValue.equals ("KEYWORD") == true) {
                            searchRequestBuilder.setSearchType (SearchType.KEYWORD);
                        } else if (strValue.equals ("CATEGORY") == true) {
                            searchRequestBuilder.setSearchType (SearchType.CATEGORY);
                        } else {
                            MessageLogger.logWarning ("Unknown search type: " + strValue);
                        }
                        break;
                case "query":
                        strValue = (String) requestSearchTemplateJson.get (key);
                        searchRequestBuilder.setQuery (strValue);
                        break;
                case "pagination":
                        int start;
                        int rows;
                        JSONObject paginationJSONObj;
                        Pagination.Builder paginationBuilder;

                        paginationJSONObj = (JSONObject) requestSearchTemplateJson.get (key);
                        start = (int) paginationJSONObj.get ("start");
                        rows = (int) paginationJSONObj.get ("rows");
                        paginationBuilder = Pagination.newBuilder (); 
                        paginationBuilder.setStart (start);
                        paginationBuilder.setRows (rows);
                        searchRequestBuilder.setPagination (paginationBuilder.buildPartial ());
                        break;
                case "fields":
                        String name;
                        String external_name;
                        JSONObject fieldNameJSONObj;
                        FieldName.Builder fieldNameBuilder;

                        fieldNameJSONObj = (JSONObject) requestSearchTemplateJson.get (key);
                        name = (String) fieldNameJSONObj.get ("name");
                        external_name = (String) fieldNameJSONObj.get ("external_name");
                        fieldNameBuilder = FieldName.newBuilder (); 
                        fieldNameBuilder.setName (name);
                        fieldNameBuilder.setExternalName (external_name);
                        searchRequestBuilder.addFields (fieldNameBuilder.buildPartial ()); 
                        break;
            }
        }
        // some other fields that have default or blank values in the template
        searchRequestBuilder.setWt (""); // wt: ""
        searchRequestBuilder.setMm (""); // mm: ""
        searchRequestBuilder.setFacetLimit (200); // facet_limit: 200
        searchRequestBuilder.setView (""); // view: ""
        searchRequestBuilder.setDfq (""); // dfq: ""
        searchRequestBuilder.setBrTags (""); // br_tags: ""
    }

    private void processResponseTemplate (JSONObject responseTemplateJson, ApiLog.Builder apiLogBuilder) throws Exception {
        ApiResponse.Builder apiResponseBuilder;
        CommonResponse.Builder commonResponseBuilder;
        SearchResponse.Builder searchResponseBuilder;
        ResponseMetrics.Builder respMetricsBuilder;
        Iterator<String> templateKeys;

        apiResponseBuilder = apiLogBuilder.getResponseBuilder ();
        commonResponseBuilder = apiResponseBuilder.getCommonBuilder ();
        searchResponseBuilder = apiResponseBuilder.getSearchBuilder ();
        respMetricsBuilder = apiResponseBuilder.getRespMetricsBuilder ();

        templateKeys = responseTemplateJson.keys ();
        while (templateKeys.hasNext ()) {
            String key = templateKeys.next ();
            MessageLogger.logDebug ("responseTemplate key: " + key);
            switch (key) {
                case "common":
                    JSONObject responseCommonTemplateJson;
                    responseCommonTemplateJson = (JSONObject) responseTemplateJson.get (key);
                    processResponseCommonTemplate (responseCommonTemplateJson, commonResponseBuilder);
                    break;
                case "search":
                    JSONObject responseSearchTemplateJson;
                    responseSearchTemplateJson = (JSONObject) responseTemplateJson.get (key);
                    processResponseSearchTemplate (responseSearchTemplateJson, searchResponseBuilder);
                    break;
                case "resp_metrics": 
                    JSONObject respMetricsTemplateJson;
                    respMetricsTemplateJson = (JSONObject) responseTemplateJson.get (key);
                    processRespMetricsTemplate (respMetricsTemplateJson, respMetricsBuilder);
                    break;
                default:
                    MessageLogger.logWarning ("Unknown key in response template: " + key);
            }
        }
    }

    private void processResponseCommonTemplate (JSONObject responseCommonTemplateJson, CommonResponse.Builder commonResponseBuilder) throws Exception {
        Iterator<String> templateKeys;

        templateKeys = responseCommonTemplateJson.keys ();
        while (templateKeys.hasNext ()) {
            int intValue;
            String strValue;
            long longValue;

            String key = templateKeys.next ();
            MessageLogger.logDebug ("responseCommonTemplate key: " + key);
            switch (key) {
                case "response_id":
                        strValue = (String) responseCommonTemplateJson.get (key);
                        commonResponseBuilder.setResponseId (strValue);
                        break;
                case "log_time":
                        longValue = (long) responseCommonTemplateJson.get (key);
                        commonResponseBuilder.setLogTime (longValue);
                        break;
                case "num_results":
                        intValue = (int) responseCommonTemplateJson.get (key);
                        commonResponseBuilder.setNumResults (intValue);
                        break;
                default:
                    MessageLogger.logWarning ("Unknown key in response common template: " + key);
            }
        }
    }

    private void processResponseSearchTemplate (JSONObject responseSearchTemplateJson, SearchResponse.Builder searchResponseBuilder) throws Exception {
        Iterator<String> templateKeys;

        templateKeys = responseSearchTemplateJson.keys ();
        while (templateKeys.hasNext ()) {
            int intValue;
            String strValue;
            long longValue;

            String key = templateKeys.next ();
            MessageLogger.logDebug ("responseSearchTemplate key: " + key);
            switch (key) {
                case "num_results":
                        intValue = (int) responseSearchTemplateJson.get (key);
                        searchResponseBuilder.setNumResults (intValue);
                        break;
                case "products": 
                        String product_id;
                        String product_name;
                        double sale_price;
                        JSONObject productsJSONObj;
                        ProductResponse.Builder productsBuilder;

                        productsJSONObj = (JSONObject) responseSearchTemplateJson.get (key);
                        product_id = (String) productsJSONObj.get ("product_id");
                        product_name = (String) productsJSONObj.get ("product_name");
                        sale_price = (double) productsJSONObj.get ("sale_price");
                        // although we have processed this 'products' section, don't add it to the
                        // the searchResponseBuilder since the sample product in the template
                        // is not necessarily a valid 'response' for different queries
                        // productsBuilder = ProductResponse.newBuilder (); 
                        // productsBuilder.setProductId (product_id);
                        // productsBuilder.setProductName (product_name);
                        // productsBuilder.setSalePrice (sale_price);
                        // searchResponseBuilder.addProducts (productsBuilder.buildPartial ());
                        break;
                default:
                    MessageLogger.logError ("Unknown key in response search template: " + key);
            }
        }
    }

    private void processRespMetricsTemplate (JSONObject responseMetricsTemplateJson, ResponseMetrics.Builder respMetricsBuilder) throws Exception {

        int resp_status_code;
        int app_latency_in_ms;
        int json_response_byte_size;

        resp_status_code = (int) responseMetricsTemplateJson.get ("resp_status_code");
        app_latency_in_ms = (int) responseMetricsTemplateJson.get ("app_latency_in_ms");
        json_response_byte_size = (int) responseMetricsTemplateJson.get ("json_response_byte_size");
        respMetricsBuilder.setRespStatusCode (resp_status_code);
        respMetricsBuilder.setAppLatencyInMs (app_latency_in_ms);
        respMetricsBuilder.setJsonResponseByteSize (json_response_byte_size);
    }
}


