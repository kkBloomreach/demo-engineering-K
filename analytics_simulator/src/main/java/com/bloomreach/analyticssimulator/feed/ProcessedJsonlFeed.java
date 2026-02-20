package com.bloomreach.analyticssimulator.feed;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.analyticssimulator.MessageLogger;

public class ProcessedJsonlFeed extends ProcessedFeed {

    private final static String KEY_NAME_PID = "pid";
    private final static String KEY_NAME_SKUID = "skuid";
    private final static String KEY_NAME_PRICE = "price";
    private final static String KEY_NAME_TITLE = "title";
    private final static String PRODUCT_NODE_NAME = "product";

    // override base class method
    // filepath to jsonl feed
    public void load (String productFilePath) throws Exception
    {
        ArrayList<FeedRecord> parsedFeedRecordList;

        MessageLogger.logDebug (" > Start Parsing Jsonl source file....: " + productFilePath);
        parsedFeedRecordList = parseCatalog (productFilePath);
        MessageLogger.logDebug (" ............................... Done Parsing Product File.");

        // set in base class
        super.setParsedFeedRecordList (parsedFeedRecordList);
    }

    /**
     * 
     * returns ArrayList<FeedRecord>
     */
    private ArrayList <FeedRecord> parseCatalog (String srcFilePath) throws Exception {
        BufferedReader reader;
        String srcLine;
        JSONObject recordJson;
        ArrayList <FeedRecord> parsedFeedRecordList;
        FeedRecord feedRecord;

        parsedFeedRecordList = new ArrayList <FeedRecord> ();

        reader = new BufferedReader (new FileReader (srcFilePath));
        while ((srcLine = reader.readLine ()) != null) {
            recordJson = new JSONObject (srcLine);
            feedRecord = parseSourceRecord (recordJson);
            parsedFeedRecordList.add (feedRecord);
        }

        return (parsedFeedRecordList);
    }

    private FeedRecord parseSourceRecord (JSONObject sourceRecordJson) throws Exception {
        FeedRecord feedRecord;
        JSONObject valueJson;
        JSONObject attributesJson;
        JSONObject variantsJson;
        double price;

        feedRecord = new FeedRecord ();
        valueJson = (JSONObject) sourceRecordJson.get ("value");
        attributesJson = (JSONObject) valueJson.get ("attributes");
        if (valueJson.has ("variants") == true) 
            variantsJson = (JSONObject) valueJson.get ("variants");
        else
            variantsJson = null;

        feedRecord.setProductId ((String) attributesJson.get (KEY_NAME_PID));
        feedRecord.setProductName ((String) attributesJson.get (KEY_NAME_TITLE));
        price = (double) attributesJson.get (KEY_NAME_PRICE);
        feedRecord.setProductPrice (Double.toString (price));

        // default skuId == pid
        feedRecord.setProductSkuId ((String) attributesJson.get (KEY_NAME_PID));   // pid == skuid
        // if product has skus, just take the first
        if (variantsJson != null) {
            Iterator productSkus = variantsJson.keys ();
            if (productSkus.hasNext ()) {
                feedRecord.setProductSkuId ((String) (productSkus.next ()));
            }
        }

        return (feedRecord);
    }
}

