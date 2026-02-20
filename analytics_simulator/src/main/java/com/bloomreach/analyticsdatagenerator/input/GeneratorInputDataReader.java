package com.bloomreach.analyticsdatagenerator.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.analyticsdatagenerator.MessageLogger;
import com.bloomreach.analyticsdatagenerator.feed.*;

public class GeneratorInputDataReader {

    File inputFile = null;

    public GeneratorInputDataReader () {
    }

    public GeneratorInputData read (File inputFile) throws Exception {
        JSONObject inputJson;
        GeneratorInputData inputData = null;

        try {
            this.inputFile = inputFile;
            inputJson = readInputJsonFile (inputFile);
            inputData = parseInputJson (inputJson);
        } catch (Exception e) {
            MessageLogger.logError ("Exception in parse input: " + e.getMessage());
        }
        return (inputData);  // may be null
    }

    private JSONObject readInputJsonFile (File inputFile) throws Exception {
        FileInputStream inputStream;
        BufferedReader bufferedReader;
        StringBuffer inputBuffer;
        String inputLine;
        JSONObject inputJson;

        inputStream = new FileInputStream (inputFile);
        bufferedReader = new BufferedReader (new InputStreamReader (inputStream));
        inputBuffer = new StringBuffer ();

        while ((inputLine = bufferedReader.readLine ()) != null) {
            inputBuffer.append (inputLine);
        }
        bufferedReader.close ();

        inputJson = new JSONObject (inputBuffer.toString());
        return inputJson;
    }

    private GeneratorInputData parseInputJson (JSONObject inputJson) throws Exception {
        GeneratorInputData inputData;
        JSONArray viewsArray;
        JSONArray excludeCategoriesArray;
        JSONArray excludeProductsArray;
        int intValue;
        String strValue;
        String[] views;
        String[] excludeCategories;
        String[] excludeProducts;
        JSONObject refUrlsJson;
        JSONObject segmentsJson;
        JSONObject queriesJson;
        Iterator<String> queries;
        Iterator<String> refTypes;
        Iterator<String> segmentKeys;
        JSONArray zeroResultQueriesJson;
        String [] zeroResultQueries;
        String feedFileName;
        ProcessedFeed processedFeed;

        inputData = new GeneratorInputData ();

        // processedFeed. Do this first just to make sure we have valid feed before reading
        // thru rest of the config .json
        strValue = inputJson.getString ("feedFile");
        processedFeed = readSourceFeed (strValue);
        if (processedFeed == null)
            return null;
        inputData.setProcessedFeed (processedFeed);

        // acct info
        intValue = inputJson.getInt ("account_id");
        inputData.setAcctId (intValue);

        strValue = inputJson.getString ("auth_key");
        inputData.setAuthKey (strValue);

        strValue = inputJson.getString ("domain_key");
        inputData.setDomainKey (strValue);

        // uidcount
        intValue = inputJson.getInt ("uidcount");
        inputData.setUidCount (intValue);

        // views array
        MessageLogger.logInfo ("Parse views");
        viewsArray =  inputJson.getJSONArray  ("views");
        if (viewsArray != null) {
            views = new String [viewsArray.length()];
            for (int i = 0; i < viewsArray.length(); i++) {
                views [i] = viewsArray.getString (i);
            }
            inputData.setViews (views);
        }

        // refUrls
        MessageLogger.logInfo ("Parse refUrls");
        refUrlsJson = inputJson.getJSONObject ("refUrls");
        refTypes = refUrlsJson.keys ();  // "home". Values for "category". "product" refUrls generated dynamically
        while (refTypes.hasNext ()) {
            String refType;
            String refValue;

            refType = refTypes.next (); // "home"
            refValue = refUrlsJson.getString (refType);
            inputData.setRefUrlInfo (refType, refValue);
        }

        // search queries
        MessageLogger.logInfo ("Parse search queries");
        queriesJson = inputJson.getJSONObject ("search_queries");
        queries = queriesJson.keys ();
        while (queries.hasNext ()) {
            String query;
            JSONArray refinedQueriesJson;
            String[] refinedQueries;

            query = queries.next ();
            refinedQueriesJson = queriesJson.getJSONArray (query);
            refinedQueries = new String [refinedQueriesJson.length()];
            for (int i = 0; i < refinedQueriesJson.length(); i++) {
                refinedQueries [i] = refinedQueriesJson.getString (i);
            }

            inputData.setSearchQueryInfo (query, refinedQueries);
        }

        // excludedCategories array
        MessageLogger.logInfo ("Parse Exclude categories");
        excludeCategoriesArray =  inputJson.getJSONArray  ("exclude_categories");
        if (excludeCategoriesArray != null) {
            excludeCategories = new String [excludeCategoriesArray.length()];
            for (int i = 0; i < excludeCategoriesArray.length(); i++) {
                excludeCategories [i] = excludeCategoriesArray.getString (i);
            }
            inputData.setExcludeCategories (excludeCategories);
        }

        // excludedProducts array
        MessageLogger.logInfo ("Parse Exclude products");
        excludeProductsArray =  inputJson.getJSONArray  ("exclude_products");
        if (excludeProductsArray != null) {
            excludeProducts = new String [excludeProductsArray.length()];
            for (int i = 0; i < excludeProductsArray.length(); i++) {
                excludeProducts [i] = excludeProductsArray.getString (i);
            }
            inputData.setExcludeProducts (excludeProducts);
        }

        // segments and associated info (eg, "fq" to use in Api call)
        MessageLogger.logInfo ("Parse segments");
        segmentsJson = inputJson.getJSONObject ("segments");
        segmentKeys = segmentsJson.keys ();
        while (segmentKeys.hasNext ()) {
            String key;
            JSONObject segmentInfoJson;
            String segmentFq;

            key = segmentKeys.next ();
            segmentInfoJson = segmentsJson.getJSONObject (key);
            segmentFq = segmentInfoJson.getString ("fq");    
            inputData.setSegmentInfo (key, segmentFq);
        }

        // zeroResultQueries
        MessageLogger.logInfo ("Parse zeroResultQueries");
        zeroResultQueriesJson = inputJson.getJSONArray ("zeroResultQueries");
        zeroResultQueries = new String [zeroResultQueriesJson.length()];
        for (int i = 0; i < zeroResultQueriesJson.length(); i++) {
            zeroResultQueries [i] = zeroResultQueriesJson.getString (i);
        }

        inputData.setZeroResultQueries (zeroResultQueries);

        return inputData;
    }

    private ProcessedFeed readSourceFeed (String feedFileName) throws Exception {
        String inputDir;
        File inputFile;
        ProcessedJsonlFeed processedFeed;
        
        // feedFile name expected to be relative to this config.json location itself
        inputDir = this.inputFile.getParent ();
        inputFile = new File (inputDir, feedFileName);
        if (inputFile.exists () == false) {
            MessageLogger.logInfo ("Source feed file does not exist: " + inputFile.getPath());
            return null;
        }

        processedFeed = new ProcessedJsonlFeed ();
        processedFeed.load (inputFile.getPath());
        if (processedFeed == null) {
            MessageLogger.logError ("Error in reading source feed");
            return null;
        }

        return processedFeed;
    }
}
