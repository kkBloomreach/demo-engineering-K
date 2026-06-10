// alter feed using feed alteration records then publish it (ie, ingest + index)
package com.bloomreach.trafficgenerator.site.feed;

import java.util.ArrayList;
import java.io.*;
import java.math.BigDecimal;
import java.util.Iterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.json.JSONObject;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildProductPagePixel;

public class FeedAlterator {

    private final static String DAILY_FEED_FILENAME_TEMP = "feed_temp.jsonl";

    private final static String KEY_NAME_PATH = "path";
    private final static String KEY_NAME_VALUE = "value";
    private final static String KEY_NAME_ATTRIBUTES = "attributes";
    private final static String KEY_NAME_VARIANTS = "variants";

    private final static String KEY_NAME_PID = "pid";
    private final static String KEY_NAME_SKUID = "skuid";
    private final static String KEY_NAME_URL = "url";
    private final static String KEY_NAME_PRICE = "price";
    private final static String KEY_NAME_SALE_PRICE = "sale_price";
    private final static String KEY_NAME_SALE_PRICE_RANGE_MIN = "sale_price_range_min";
    private final static String KEY_NAME_SALE_PRICE_RANGE_MAX = "sale_price_range_max";
    private final static String KEY_NAME_VELO_SKU_SALE_PRICE = "velo_sku_sale_price";

    private final static String KEY_NAME_CAMPAIGN = "campaign";

    private CampaignRecord campaignRecord = null;

    public FeedAlterator () {
    }

    // param may be null
    public void setCampaignRecord (CampaignRecord campaignRecord) {
        this.campaignRecord = campaignRecord;
    }

    // param = <rootDir>/accountName
    // alter feed and then publish it
    public void alterAndPublishFeed (String accountDirPath, String realm) throws Exception {
        String originalFeedDir; // original
        File originalFeedFile;
        String dailyFeedDirPath;
        FeedPublisher feedPublisher;
        File dailyFeedOutputFile = null;
        String dataConnectKey = null;

        originalFeedDir = String.format ("%s/%s", accountDirPath, GeneratorConstants.INPUT_ORIGINAL_FEED_DIR);
        // account specific path
        originalFeedFile = new File (originalFeedDir, SiteConfig.getProductCatalogConfigParam ("ORIGINAL_CATALOG_PATH"));
        if (originalFeedFile.exists () == false) {
            throw new Exception ("Cannot find original feed file: " + originalFeedFile.getPath());
        }

        MessageLogger.logDebug (String.format ("Processing original feed file: %s", originalFeedFile.getPath()));
        dailyFeedDirPath = String.format ("%s/%s", accountDirPath, GeneratorConstants.INPUT_DAILY_FEED_DIR);
        // NOTE: Even if campaignRecord is null, go thru feed alterations so that product-urls are
        // adjusted for each account
        try {
            dailyFeedOutputFile = alterFeedInternal (originalFeedFile, dailyFeedDirPath);
        } catch (Exception e) {
            MessageLogger.logError (String.format ("Failed to create daily feed file"));
            throw new Exception (e.getMessage());
        }

        if (dailyFeedOutputFile == null) {
            MessageLogger.logError (String.format ("No daily feed file, nothing to publish..."));
            return; 
        }
 
        // after alter, publish the feed (ingest, index)
        dataConnectKey = SiteConfig.getAccountConfigParam ("DATACONNECT_ACCESS_KEY");
        if (dataConnectKey.equals (GeneratorConstants.DATACONNECT_ACCESS_KEY_UNKNOWN) == true) {
            MessageLogger.logDebug ("Catalog not indexed - DataConnect access key not available");
        } else { 
            MessageLogger.logDebug ("Successfully processed original feed file. Start publish feed");
            feedPublisher = new FeedPublisher ();
            try {
                feedPublisher.publish (dailyFeedOutputFile.getPath (), realm);
            } catch (Exception e) {
                MessageLogger.logError (String.format ("Failed to publish daily feed file"));
                // NOTE: Even if feed publish fails, we continue to 'open' the shop
                // An Error is logged above however. We could send an alert in some form (email/slack/...)
                // email/slack if env == release -- TO BE DONE @@@
                // throw new Exception (e.getMessage());
            }
        }

        MessageLogger.logDebug ("End publish feed");
    }

    // this method is called even if there is no valid campaign record
    private File alterFeedInternal (File originalFeedFile, String dailyFeedDirPath) throws Exception {
        BufferedReader reader;
        String srcLine;
        String tmpFeedOutputFileName;   // tmp output
        File tmpFeedOutputFile;
        BufferedWriter tmpOutputWriter;
        int outputProductCount = 0;

        ArrayList <String> campaignProducts; // campaign
        float priceDiscount;
        double discountFactor;

        if (this.campaignRecord != null) {
            campaignProducts = this.campaignRecord.getProductList ();
            priceDiscount = this.campaignRecord.getPriceDiscount ();
            discountFactor = 1.0 - (priceDiscount/100); 
        } else {
            campaignProducts = null;
            discountFactor = 0.0;
        }

        // initially output to tmp file
        tmpFeedOutputFileName = DAILY_FEED_FILENAME_TEMP;
        tmpFeedOutputFile = new File (dailyFeedDirPath, tmpFeedOutputFileName);
        tmpOutputWriter = new BufferedWriter (new FileWriter (tmpFeedOutputFile));

        reader = new BufferedReader (new FileReader (originalFeedFile));
        while ((srcLine = reader.readLine ()) != null) {
            JSONObject outputProductJson;
            String outputProductJsonStr;

            outputProductJson = new JSONObject (srcLine); // start with output = original

            // alter one product
            try {
                alterProductInternal (outputProductJson, campaignProducts, discountFactor); // campaignProducts may be null
            } catch (Exception e) {
                MessageLogger.logError (String.format ("Failed to alter product"));
                tmpOutputWriter.close ();
                reader.close ();
                throw new Exception (e.getMessage());
            }

            // add outputStr to outputBuffer
            outputProductJsonStr = outputProductJson.toString ();
            if (outputProductCount == 0) {
                tmpOutputWriter.write (outputProductJsonStr);
            } else {
                tmpOutputWriter.newLine ();
                tmpOutputWriter.write (outputProductJsonStr);
            }
            outputProductCount++;
        }
        reader.close ();
        tmpOutputWriter.flush ();
        tmpOutputWriter.close ();

        // if everything successful, 'move' tmpFile to actual file and return that file
        {
            Path dailyFeedOutputPath;
            Path tmpOutputPath;
            File dailyFeedOutputFile;

            tmpOutputPath = tmpFeedOutputFile.toPath ();

            dailyFeedOutputFile = new File (dailyFeedDirPath, GeneratorConstants.INPUT_DAILY_JSONL_FEED_FILE_NAME);
            dailyFeedOutputPath = dailyFeedOutputFile.toPath ();

            Files.move (tmpOutputPath, dailyFeedOutputPath, StandardCopyOption.REPLACE_EXISTING);  // atomic move
            return dailyFeedOutputFile;
        }
    }

    private void alterProductInternal (JSONObject outputProductJson, ArrayList <String> campaignProducts, double discountFactor) throws Exception {
        String pid;
        String skuid;
        String url;
        JSONObject outputValueJson;
        JSONObject outputAttribsJson;

        outputValueJson = outputProductJson.getJSONObject (KEY_NAME_VALUE);
        outputAttribsJson = outputValueJson.getJSONObject (KEY_NAME_ATTRIBUTES);
       
        if (outputAttribsJson.has (KEY_NAME_PID)) {
            pid = outputAttribsJson.getString (KEY_NAME_PID);
        } else {
            String path;
            int rIndx;

            // pid is no longer a required attribute in the feed. Use path value to define one
            path = outputProductJson.getString (KEY_NAME_PATH);
            rIndx = path.lastIndexOf("/");
            if (rIndx >= 0) {
                pid = path.substring(rIndx+1);
            } else {
                pid = path;
            }
            outputAttribsJson.put (KEY_NAME_PID, pid);
        }
        if ((campaignProducts != null) && (campaignProducts.contains (pid) == true)) {
            // alter product's own and variant's sale-prices
            alterProductAndVariantsForCampaign (outputProductJson, discountFactor);
        } 

        // change productUrl to match site's domain. The original feed has
        // url = default_domain which needs to change as per this site
        // This is done irrespective of campaign-or-no-campaign
        // Bloomreach SPA requires product-page url to have "<pid>___"
        // Therefore, we build it as <pid>___<pid>
        skuid = pid;
        url = BuildProductPagePixel.getProductPageUrl (pid, skuid);
        outputAttribsJson.put (KEY_NAME_URL, url);

        return; // debug bkpt
    }

    // for ONE product, alter price, sale_price for product and its variants (if any)
    private void alterProductAndVariantsForCampaign (JSONObject outputProductJson, double discountFactor) throws Exception {
        JSONObject outputValueJson;
        JSONObject outputAttribsJson;
        double origPrice;
        double discountedPrice;
        String salePriceStr;
        double salePrice;

        outputValueJson = outputProductJson.getJSONObject (KEY_NAME_VALUE);
        outputAttribsJson = outputValueJson.getJSONObject (KEY_NAME_ATTRIBUTES);

        // add 'campaign' attribute for this product 
        outputAttribsJson.put (KEY_NAME_CAMPAIGN, campaignRecord.getCampaignName());

        // product's own sale_price
        origPrice = (double) outputAttribsJson.getDouble (KEY_NAME_PRICE);
        discountedPrice = origPrice * discountFactor; 

        // following code is to get around a JSONObject bug (or issue) that
        // a floating value gets 'put' as a double
        salePriceStr = String.format ("%.2f", discountedPrice);
        salePrice = Double.parseDouble (salePriceStr);

        outputAttribsJson.put (KEY_NAME_SALE_PRICE, salePrice);
        outputAttribsJson.put (KEY_NAME_SALE_PRICE_RANGE_MIN, salePrice);   // currently both min,max = sale_price
        outputAttribsJson.put (KEY_NAME_SALE_PRICE_RANGE_MAX, salePrice);

        // variants if any
        if (outputValueJson.has (KEY_NAME_VARIANTS) == true) {
            JSONObject variantsJson;
            Iterator<String> variantIds;

            variantsJson = outputValueJson.getJSONObject (KEY_NAME_VARIANTS);
            variantIds = variantsJson.keys ();
            while (variantIds.hasNext () == true) {
                JSONObject oneVariantJson;
                JSONObject oneVariantAttribsJson;
                String variantId;
                String origPriceStr;
                Object origPriceValue;

                variantId = variantIds.next ();
                oneVariantJson = (JSONObject) variantsJson.get (variantId);
                oneVariantAttribsJson = oneVariantJson.getJSONObject (KEY_NAME_ATTRIBUTES);

                // In some catalogs, price-value for variants is a string, sometimes float/double
                origPriceValue = oneVariantAttribsJson.get (KEY_NAME_PRICE);
                try {
                    if (origPriceValue instanceof String) {
                        origPrice = Double.parseDouble ((String) origPriceValue);
                    } else if (origPriceValue instanceof Float) {
                        origPrice = ((Float)origPriceValue).doubleValue ();
                    } else if (origPriceValue instanceof Double) {
                        origPrice = (Double) origPriceValue; 
                    } else if (origPriceValue instanceof BigDecimal) {
                        origPrice = ((BigDecimal)origPriceValue).doubleValue();
                    } else {
                        MessageLogger.logWarning(String.format ("Incorrect format for variant price: %s", (String) origPriceValue));
                        origPrice = 0.05; // some non-zero value
                    }
                } catch (Exception nfe) {
                    MessageLogger.logError( "Exception in parsing variant price");
                    origPrice = 0.05; // some non-zero value
                }

                discountedPrice = origPrice * discountFactor; 
                salePriceStr = String.format ("%.2f", discountedPrice);
                salePrice = Double.parseDouble (salePriceStr);
                oneVariantAttribsJson.put (KEY_NAME_SALE_PRICE, salePrice);
                oneVariantAttribsJson.put (KEY_NAME_VELO_SKU_SALE_PRICE, salePrice);
            }
        }
    }
}

