package com.bloomreach.analyticsdatagenerator.feed;

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

import com.bloomreach.analyticsdatagenerator.MessageLogger;

public class ProcessedJsonlFeed extends ProcessedFeed {

    private final static String KEY_NAME_PID = "pid";
    private final static String KEY_NAME_SKUID = "skuid";
    private final static String KEY_NAME_PRICE = "price";
    private final static String KEY_NAME_TITLE = "title";
    private final static String KEY_NAME_CATEGORY_PATHS = "category_paths";
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
        JSONArray categoryPathsJsonArray;
        double price;
        ArrayList<CategoryInfo> categoryInfoList;

        feedRecord = new FeedRecord ();
        valueJson = (JSONObject) sourceRecordJson.get ("value");
        attributesJson = (JSONObject) valueJson.get ("attributes");
        if (valueJson.has ("variants"))
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

        // collect categoryPaths. This is needed to generate category RefUrls as well as
        // refinedCategoryQueries which are then used to generate necessary data for simulation
        categoryPathsJsonArray = attributesJson.getJSONArray (KEY_NAME_CATEGORY_PATHS);
        categoryInfoList = collectCategoryInfoList (categoryPathsJsonArray);
        feedRecord.setCategoryInfoList (categoryInfoList);

        return (feedRecord);
    }

    // a product may be in multiple categories, each with its own category path
    // For reference, sample category_paths in .jsonl
    /***
        "category_paths": [
        [
          {
            "id": "123",
            "name": "Marketing Groups"
          },
          {
            "id": "223",
            "name": "All Flat Rate - PDP Banner"
          }
        ],
        [
          {
            "id": "123",
            "name": "Marketing Groups"
          },
          {
            "id": "178",
            "name": "2023 30% off Living/Home Office Furniture"
          }
        ],
        [
            ...
        ]
    ***/
    private ArrayList<CategoryInfo> collectCategoryInfoList (JSONArray categoryPathsJsonArray) throws Exception {
        ArrayList<CategoryInfo> catInfoList = new ArrayList<CategoryInfo> ();

        for (int n = 0; n < categoryPathsJsonArray.length(); n++) {
            JSONArray categoryPathArray;
            String fullPath = null;
            String parentCatId = null;

            categoryPathArray = categoryPathsJsonArray.getJSONArray (n);
            // categoryPath in turn is list of individual JSONObjects
            // representing individual elements in that path
            for (int l = 0; l < categoryPathArray.length(); l++) {
                JSONObject pathJsonObj;
                CategoryInfo catInfo;
                String catId;
                String catName;

                pathJsonObj = categoryPathArray.getJSONObject (l);
                catId = pathJsonObj.getString ("id");
                catName = pathJsonObj.getString ("name");
                
                if (fullPath == null)
                    fullPath = catName;
                else
                    fullPath = fullPath + "/" + catName;

                // level, id, name, path
                // for top category, parentId is null
                catInfo = new CategoryInfo (l, catId, catName, fullPath, parentCatId);

                // current catId is child's parentId
                parentCatId = catId;

                catInfoList.add (catInfo);
            }
        }

        return catInfoList;
    }
}

