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
    private final static String KEY_NAME_VARIANTS = "variants"; 
    private final static String KEY_NAME_VIEWS = "views"; // attrib name in PacificSupply feed
    private final static String KEY_NAME_URL = "url"; // attrib name in PacificSupply feed
    private final static String KEY_NAME_CATEGORY_PATHS = "category_paths";
    private final static String KEY_NAME_SKU_PRICE = "sku_price";
    private final static String KEY_NAME_SKU_SALE_PRICE = "sku_sale_price";
    private final static String KEY_NAME_STYLE = "style"; // attrib name in PacificApparel

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

    // parse single product source record
    private FeedRecord parseSourceRecord (JSONObject sourceRecordJson) throws Exception {
        FeedRecord feedRecord;
        JSONObject valueJson;
        JSONObject attributesJson;
        double price;
        String pid;

        feedRecord = new FeedRecord ();
        valueJson = (JSONObject) sourceRecordJson.get ("value");
        attributesJson = (JSONObject) valueJson.get ("attributes");

        // 'pid' is NOT a required field in catalog - obtain it from 'Path' value if it does not exist
        // @@@ currently use "path" tail-string as pid -- VALIDATE - TO BE DONE, 
        if (attributesJson.has (KEY_NAME_PID))
            pid = attributesJson.getString (KEY_NAME_PID);
        else {
            String path;
            int rindx;

            path = sourceRecordJson.getString (KEY_NAME_PATH);
            rindx = path.lastIndexOf ("/");
            pid = path.substring (rindx+1);
        }

        feedRecord.setProductId (pid);
        feedRecord.setProductName ((String) attributesJson.get (KEY_NAME_TITLE));
        feedRecord.setProductAvailability ((boolean) attributesJson.getBoolean (KEY_NAME_AVAILABILITY));
        feedRecord.setProductUrl ((String) attributesJson.get (KEY_NAME_URL));

        price = (double) attributesJson.getDouble (KEY_NAME_PRICE);
        feedRecord.setProductPrice (Double.toString (price));
        if (attributesJson.has (KEY_NAME_SALE_PRICE)) {
            double salePrice;

            salePrice = (double) attributesJson.getDouble (KEY_NAME_SALE_PRICE);
            feedRecord.setProductSalePrice (Double.toString (salePrice));
        } else {
            feedRecord.setProductSalePrice (Double.toString (price));  // sale_price = price
        }

        // style is optional -- needed to define RTS rule for an account
        if (attributesJson.has (KEY_NAME_STYLE)) {
            feedRecord.setProductStyle (attributesJson.optString (KEY_NAME_STYLE)); // value may ne null
        }

        // views if any
        if (valueJson.has (KEY_NAME_VIEWS)) {
            JSONObject viewsJson;
            Iterator<String> viewIds;
            ArrayList<ProductViewRecord> productViews;

            viewsJson = valueJson.getJSONObject (KEY_NAME_VIEWS);
            viewIds = viewsJson.keys ();
            productViews = new ArrayList <ProductViewRecord> ();
            while (viewIds.hasNext ()) {
                ProductViewRecord productViewRecord;
                JSONObject productViewJson;
                String viewId;

                viewId = viewIds.next ();
                productViewJson = viewsJson.getJSONObject (viewId);
                productViewRecord = new ProductViewRecord ();
                productViewRecord.setViewId (viewId);
                if (productViewJson.has (KEY_NAME_PRICE))
                    productViewRecord.setViewPrice (productViewJson.getString (KEY_NAME_PRICE));
                if (productViewJson.has (KEY_NAME_SALE_PRICE))
                    productViewRecord.setViewSalePrice (productViewJson.getString (KEY_NAME_SALE_PRICE));
                productViews.add (productViewRecord);
            }
            feedRecord.setProductViews (productViews);

            // views = new ArrayList <String>();
            // while (viewIds.hasNext())
            //     views.add ((String) viewIds.next());
            // feedRecord.setProductViews (views);
        }

        // variants if any
        if (valueJson.has (KEY_NAME_VARIANTS)) {
            JSONObject variantsJson;
            Iterator<String> skuIds;
            ArrayList<ProductVariantRecord> productVariants;

            variantsJson = (JSONObject) valueJson.get ("variants");
            skuIds = variantsJson.keys ();
            productVariants = new ArrayList <ProductVariantRecord> ();
            while (skuIds.hasNext ()) {
                ProductVariantRecord productVariantRecord;
                JSONObject productVariantJson;
                String skuId;

                skuId = skuIds.next ();
                productVariantJson = variantsJson.getJSONObject (skuId);
                productVariantRecord = new ProductVariantRecord ();
                productVariantRecord.setSkuId (skuId);
                if (productVariantJson.has (KEY_NAME_SKU_PRICE))
                    productVariantRecord.setSkuPrice (productVariantJson.getString (KEY_NAME_SKU_PRICE));
                if (productVariantJson.has (KEY_NAME_SKU_SALE_PRICE))
                    productVariantRecord.setSkuSalePrice (productVariantJson.getString (KEY_NAME_SKU_SALE_PRICE));
                productVariants.add (productVariantRecord);
            }
            feedRecord.setProductVariants (productVariants);

            // default skuId == pid
            // feedRecord.setProductSkuId (pid);
            // if product has skus, just take the first
            // it is used to build default PDP url
            // if (variantsJson != null) {
            //      Iterator <String> productVariants = variantsJson.keys ();
            //      if (productVariants.hasNext ()) {
            //          feedRecord.setProductSkuId ((String) (productVariants.next ())); // variant_id same as sku_id
            //      }
            // }
        }        

        return (feedRecord);
    }
}

