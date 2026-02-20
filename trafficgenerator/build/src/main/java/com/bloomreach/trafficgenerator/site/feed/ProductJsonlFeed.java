package com.bloomreach.trafficgenerator.site.feed;

import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Iterator;
import java.util.ArrayList;

import org.json.JSONObject;

import com.bloomreach.trafficgenerator.MessageLogger;

public class ProductJsonlFeed extends ProductFeed {

    private final static String KEY_NAME_PATH = "path";
    private final static String KEY_NAME_PID = "pid";
    private final static String KEY_NAME_SKUID = "skuid";
    private final static String KEY_NAME_PRICE = "price";
    private final static String KEY_NAME_SALE_PRICE = "sale_price";
    private final static String KEY_NAME_TITLE = "title";
    private final static String KEY_NAME_AVAILABILITY = "availability";
    private final static String KEY_NAME_VIEWS = "view_id"; // attrib name in PacificSupply feed
    private final static String KEY_NAME_URL = "url"; // attrib name in PacificSupply feed
    private final static String KEY_NAME_CATEGORY_PATHS = "category_paths";

    // override base class method
    // filepath to jsonl feed
    public void load (String productFilePath) throws Exception {
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
        reader.close();

        return (parsedFeedRecordList);
    }

    private FeedRecord parseSourceRecord (JSONObject sourceRecordJson) throws Exception {
        FeedRecord feedRecord;
        JSONObject valueJson;
        JSONObject attributesJson;
        JSONObject variantsJson;
        double price;
        String pid;

        feedRecord = new FeedRecord ();
        valueJson = (JSONObject) sourceRecordJson.get ("value");
        attributesJson = (JSONObject) valueJson.get ("attributes");
        if (valueJson.has ("variants") == true) 
            variantsJson = (JSONObject) valueJson.get ("variants");
        else
            variantsJson = null;

        // 'pid' is NOT a required field in catalog - obtain it from 'Path' value if it does not exist
        // @@@ currently use "path" tail-string as pid -- VALIDATE - TO BE DONE, 
        if (attributesJson.has (KEY_NAME_PID))
            pid = (String) attributesJson.get (KEY_NAME_PID);
        else {
            String path;
            int rindx;

            path = (String) sourceRecordJson.get (KEY_NAME_PATH);
            rindx = path.lastIndexOf ("/");
            pid = path.substring (rindx+1);
        }

        feedRecord.setProductId (pid);
        feedRecord.setProductName ((String) attributesJson.get (KEY_NAME_TITLE));
        feedRecord.setAvailability ((boolean) attributesJson.getBoolean (KEY_NAME_AVAILABILITY));
        feedRecord.setUrl ((String) attributesJson.get (KEY_NAME_URL));
        if (attributesJson.has (KEY_NAME_VIEWS)) 
            feedRecord.setViews ((String) attributesJson.get (KEY_NAME_VIEWS));

        price = (double) attributesJson.getDouble (KEY_NAME_PRICE);
        feedRecord.setProductPrice (Double.toString (price));

        if (attributesJson.has (KEY_NAME_SALE_PRICE)) {
            double salePrice;

            salePrice = (double) attributesJson.getDouble (KEY_NAME_SALE_PRICE);
            feedRecord.setProductSalePrice (Double.toString (salePrice));
        }

        // default skuId == pid
        feedRecord.setProductSkuId (pid);
        // if product has skus, just take the first
        if (variantsJson != null) {
            Iterator <String> productSkus = variantsJson.keys ();
            if (productSkus.hasNext ()) {
                feedRecord.setProductSkuId ((String) (productSkus.next ()));
            }
        }

        return (feedRecord);
    }
}

