package com.bloomreach.trafficgenerator.site.journeydata;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.trafficgenerator.MessageLogger;

public class WidgetConfigs {

    // widget "CODEs" used internally to simplify case-stmts etc
    public final static String WCODE_PATHWAY_CATEGORY = "PATHWAY_CATEGORY";
    public final static String WCODE_PATHWAY_KEYWORD =  "PATHWAY_KEYWORD";
    public final static String WCODE_RECO_GLOBAL_BESTSELLER = "RECO_GLOBAL_BESTSELLER";
    public final static String WCODE_RECO_GLOBAL_TRENDING = "RECO_GLOBAL_TRENDING";
    public final static String WCODE_RECO_ITEM_EXP =  "RECO_ITEM_EXP";
    public final static String WCODE_RECO_ITEM_FREQ_BOUGHT = "RECO_ITEM_FREQ_BOUGHT";
    public final static String WCODE_RECO_ITEM_FREQ_VIEWED = "RECO_ITEM_FREQ_VIEWED";
    public final static String WCODE_RECO_ITEM_SIMILAR = "RECO_ITEM_SIMILAR";
    public final static String WCODE_RECO_PERS_PAST_PURCHASE = "RECO_PERS_PAST_PURCHASE";
    public final static String WCODE_RECO_PERS_RECENTLY_VIEWED = "RECO_PERS_RECENTLY_VIEWED";
    public final static String WCODE_RECO_VISUAL_UPLOAD = "RECO_VISUAL_UPLOAD"; // this is not supported
    public final static String WCODE_RECO_VISUAL_RECO = "RECO_VISUAL_RECO"; 

    public final static String PAGEMAP_PAGETYPE_HOMEPAGE               = "homepage";
    public final static String PAGEMAP_PAGETYPE_PRODUCTPAGE            = "productpage";
    public final static String PAGEMAP_PAGETYPE_CATEGORYPAGE           = "categorypage";
    public final static String PAGEMAP_PAGETYPE_SEARCHRESULTPAGE       = "searchresultpage";
    public final static String PAGEMAP_PAGETYPE_CONTENTPAGE            = "contentpage";
    public final static String PAGEMAP_PAGETYPE_CONVERSIONPAGE         = "conversionpage";
    public final static String PAGEMAP_PAGETYPE_THEMATICPAGE           = "thematicpage";
    public final static String PAGEMAP_PAGETYPE_OTHERPAGE              = "other";

    // variables to parse widget-config.json file 
    private final static String CONFIG_TYPE_WIDGETS = "widgets";
    private final static String CONFIG_TYPE_PAGEMAP = "pagemap";

    private final static String WIDGET_TYPE_PATHWAYS                    = "pathways";
    private final static String WIDGET_TYPE_RECO                        = "recommendations";
    private final static String WIDGET_TYPE_PATHWAYS_CATEGORY           = "category";
    private final static String WIDGET_TYPE_PATHWAYS_KEYWORD            = "keyword";
    private final static String WIDGET_TYPE_RECO_GLOBAL                 = "global";
    private final static String WIDGET_TYPE_RECO_GLOBAL_BESTSELLER      = "bestseller";
    private final static String WIDGET_TYPE_RECO_GLOBAL_TRENDING        = "trending";
    private final static String WIDGET_TYPE_RECO_ITEM                   = "item";
    private final static String WIDGET_TYPE_RECO_ITEM_EXP_DRIVEN        = "experience_driven";
    private final static String WIDGET_TYPE_RECO_ITEM_FREQ_BOUGHT       = "frequently_bought";
    private final static String WIDGET_TYPE_RECO_ITEM_FREQ_VIEWED       = "frequently_viewed";
    private final static String WIDGET_TYPE_RECO_ITEM_SIMILAR           = "similar_products";
    private final static String WIDGET_TYPE_RECO_PERSONAL               = "personalization";
    private final static String WIDGET_TYPE_RECO_PERSONALIZE_PAST_PURCHASE = "past_purchase";
    private final static String WIDGET_TYPE_RECO_PERSONALIZE_RECENTLY_VIEWED = "recently_viewed";
    private final static String WIDGET_TYPE_RECO_VISUAL                 = "visual";
    private final static String WIDGET_TYPE_RECO_VISUAL_RECO            = "recommendation";
    private final static String WIDGET_TYPE_RECO_VISUAL_UPLOAD          = "upload";

    private final static String WIDGET_CONFIG_KEY_WID                   = "wid";
    private final static String WIDGET_CONFIG_KEY_WID_UNDEFINED         = "UNDEFINED";
    private final static String WIDGET_CONFIG_KEY_WCODE                 = "WCODE";


    private ArrayList <WidgetRecord> allWidgetsInConfig; // pathways and reco's
    private ArrayList <WidgetRecord> homepageWidgets = null;
    private ArrayList <WidgetRecord> productpageWidgets = null;
    private ArrayList <WidgetRecord> categorypageWidgets = null;
    private ArrayList <WidgetRecord> searchresultpageWidgets = null;
    private ArrayList <WidgetRecord> contentpageWidgets = null;
    private ArrayList <WidgetRecord> conversionpageWidgets = null;
    private ArrayList <WidgetRecord> thematicpageWidgets = null;
    private ArrayList <WidgetRecord> otherpageWidgets = null;
 
    public WidgetConfigs () {
    }

    public boolean load (String configPath) throws Exception {
        File configFile;

        configFile = new File (configPath);
        if (configFile.exists () == false)
            return false;

        this.allWidgetsInConfig = new ArrayList <WidgetRecord> ();
        parseConfig (configFile);
        return true;
    }

    public ArrayList <WidgetRecord> getAllWidgets () {
        return this.allWidgetsInConfig;
    }

    public ArrayList <WidgetRecord> getWidgetsOnPage (String pageType) {
        ArrayList <WidgetRecord> widgetsOnPage = null;

        switch (pageType) {
            case PAGEMAP_PAGETYPE_HOMEPAGE: 
                widgetsOnPage = homepageWidgets; 
                break;
            case PAGEMAP_PAGETYPE_PRODUCTPAGE: 
                widgetsOnPage = productpageWidgets; 
                break;

            case PAGEMAP_PAGETYPE_CATEGORYPAGE:
                widgetsOnPage = categorypageWidgets;
                break;

            case PAGEMAP_PAGETYPE_SEARCHRESULTPAGE:
                widgetsOnPage = searchresultpageWidgets;
                break;

            case PAGEMAP_PAGETYPE_CONTENTPAGE:
                widgetsOnPage = contentpageWidgets;
                break;

            case PAGEMAP_PAGETYPE_CONVERSIONPAGE:
                widgetsOnPage = conversionpageWidgets;
                break;

            case PAGEMAP_PAGETYPE_THEMATICPAGE:
                widgetsOnPage = thematicpageWidgets;
                break;

            case PAGEMAP_PAGETYPE_OTHERPAGE:
                widgetsOnPage = otherpageWidgets;
                break;

            default:
                MessageLogger.logWarning (String.format ("WidgetConfigs, unknown pagetype %s", pageType));
                widgetsOnPage = null;
        }
        return widgetsOnPage; 
    }

    //////// INTERNAL METHODS
    private void parseConfig (File configFile) throws Exception {
        BufferedReader reader;
        String srcLine;
        JSONObject configJson;
        StringBuffer configBuf;

        reader = new BufferedReader (new FileReader (configFile));
        configBuf = new StringBuffer ();
        while ((srcLine = reader.readLine ()) != null) {
            configBuf.append (srcLine);
        }
        reader.close();

        configJson = new JSONObject (configBuf.toString ());
        // first parse all the widgets
        if (configJson.has (CONFIG_TYPE_WIDGETS)) {
            parseWidgetDetails (configJson.getJSONObject (CONFIG_TYPE_WIDGETS)); 
        }

        // then parse pagemap which collects page-specific widgets
        if (configJson.has (CONFIG_TYPE_PAGEMAP)) {
            parsePagemap (configJson.getJSONObject (CONFIG_TYPE_PAGEMAP));
        }
    }

    private void parseWidgetDetails (JSONObject configJson) throws Exception {
        Iterator<String> configKeys;
        String key;

        configKeys = configJson.keys ();
        while (configKeys.hasNext ()) {     // 'pathways', 'recommendations', 'comments'
            key = configKeys.next ();
            if (key.startsWith ("__") == true)  // skip 'comment' keys
                continue;

            switch (key) {
                case WIDGET_TYPE_PATHWAYS:
                    parsePathwaysWidgets (configJson.getJSONObject (WIDGET_TYPE_PATHWAYS));
                    break;
                case WIDGET_TYPE_RECO:
                    parseRecommendationWidgets (configJson.getJSONObject (WIDGET_TYPE_RECO));
                    break;
                default:
                    MessageLogger.logError (String.format ("Unknown widget type: %s", key));
            }
        }
    }

    private void parsePagemap (JSONObject configJson) throws Exception {
        Iterator<String> configKeys;

        configKeys = configJson.keys ();
        while (configKeys.hasNext ()) {     // 'pathways', 'recommendations', 'comments'
            JSONArray pageWidgetCodes;
            String pagetype;

            pagetype = configKeys.next ();  // "homepage", "productspage", ...
            if (pagetype.startsWith ("__") == true)  // skip 'comment' keys
                continue;

            switch (pagetype) {
                case PAGEMAP_PAGETYPE_HOMEPAGE:
                    pageWidgetCodes = configJson.getJSONArray (PAGEMAP_PAGETYPE_HOMEPAGE);
                    if (pageWidgetCodes.length () > 0) 
                        this.homepageWidgets = collectWidgetsOnPage (pageWidgetCodes);
                    break;

                case PAGEMAP_PAGETYPE_PRODUCTPAGE:
                    pageWidgetCodes = configJson.getJSONArray (PAGEMAP_PAGETYPE_PRODUCTPAGE);
                    if (pageWidgetCodes.length () > 0) 
                        this.productpageWidgets = collectWidgetsOnPage (pageWidgetCodes);
                    break;

                case PAGEMAP_PAGETYPE_CATEGORYPAGE:
                    pageWidgetCodes = configJson.getJSONArray (PAGEMAP_PAGETYPE_CATEGORYPAGE);
                    if (pageWidgetCodes.length () > 0) 
                        this.categorypageWidgets = collectWidgetsOnPage (pageWidgetCodes);
                    break;

                case PAGEMAP_PAGETYPE_SEARCHRESULTPAGE:
                    pageWidgetCodes = configJson.getJSONArray (PAGEMAP_PAGETYPE_SEARCHRESULTPAGE);
                    if (pageWidgetCodes.length () > 0) 
                        this.searchresultpageWidgets = collectWidgetsOnPage (pageWidgetCodes);
                    break;

                case PAGEMAP_PAGETYPE_CONTENTPAGE:
                    pageWidgetCodes = configJson.getJSONArray (PAGEMAP_PAGETYPE_CONTENTPAGE);
                    if (pageWidgetCodes.length () > 0) 
                        this.contentpageWidgets = collectWidgetsOnPage (pageWidgetCodes);
                    break;

                case PAGEMAP_PAGETYPE_CONVERSIONPAGE:
                    pageWidgetCodes = configJson.getJSONArray (PAGEMAP_PAGETYPE_CONVERSIONPAGE);
                    if (pageWidgetCodes.length () > 0) 
                        this.conversionpageWidgets = collectWidgetsOnPage (pageWidgetCodes);
                    break;

                case PAGEMAP_PAGETYPE_THEMATICPAGE:
                    pageWidgetCodes = configJson.getJSONArray (PAGEMAP_PAGETYPE_THEMATICPAGE);
                    if (pageWidgetCodes.length () > 0) 
                        this.thematicpageWidgets = collectWidgetsOnPage (pageWidgetCodes);
                    break;

                case PAGEMAP_PAGETYPE_OTHERPAGE:
                    pageWidgetCodes = configJson.getJSONArray (PAGEMAP_PAGETYPE_OTHERPAGE);
                    if (pageWidgetCodes.length () > 0) 
                        this.otherpageWidgets = collectWidgetsOnPage (pageWidgetCodes);
                    break;

                default:
                    MessageLogger.logError (String.format ("Unknown pagemap pagetype: %s", pagetype));
            }
        }
    }

    // param is list-of-widgetcodes associated with a pagetype.
    // Single page can have multiple types of widgets on it (eg, past-purchase, freq-viewed, ...)
    private ArrayList <WidgetRecord> collectWidgetsOnPage (JSONArray pageWidgetCodes) {
        ArrayList <WidgetRecord> allWidgetRecordsOnPage = new ArrayList <WidgetRecord> ();

        for (int i  = 0; i < pageWidgetCodes.length(); i++ ) {
            ArrayList <WidgetRecord> widgetRecords;

            widgetRecords = lookupWidgetRecords ((String) pageWidgetCodes.get (i));
            if ((widgetRecords != null) && (widgetRecords.size() > 0)) {
                allWidgetRecordsOnPage.addAll (widgetRecords);
            }
        }

        return allWidgetRecordsOnPage; // may be null/empty/non-empty
    }

    // there can be multiple widgets with the same wcode (eg, WCODE_RECENTLY_VIEWED)
    private ArrayList<WidgetRecord> lookupWidgetRecords (String wcode) {
        ArrayList <WidgetRecord> records = new ArrayList <WidgetRecord> ();

        for (WidgetRecord record : this.allWidgetsInConfig) {
            if (record.getWidgetCode ().equals (wcode)) {
                records.add (record);
            }
        }

        return records; // may be empty
    }

    private void parsePathwaysWidgets (JSONObject pathwaysJson) throws Exception {
        Iterator<String> configKeys;

        configKeys = pathwaysJson.keys ();    // 'category', 'keyword'
        while (configKeys.hasNext ()) {
            String key;

            key = configKeys.next ();
            if (key.startsWith ("__") == true)  // skip 'comment' keys
                continue;
            switch (key) {
                case WIDGET_TYPE_PATHWAYS_CATEGORY:
                    JSONArray categoriesJsonArray;

                    categoriesJsonArray = pathwaysJson.getJSONArray (WIDGET_TYPE_PATHWAYS_CATEGORY);
                    parsePathwaysCategories (categoriesJsonArray);
                    break;

                case WIDGET_TYPE_PATHWAYS_KEYWORD:
                    JSONArray keywordsJsonArray;

                    keywordsJsonArray = pathwaysJson.getJSONArray (WIDGET_TYPE_PATHWAYS_KEYWORD);
                    parsePathwaysKeywords (keywordsJsonArray);
                    break;

                default:
                    MessageLogger.logError (String.format ("Unknown pathways widget: %s", key)); 
            }
        }
    }

    private void parseRecommendationWidgets (JSONObject recommendationsJson) throws Exception {
        Iterator<String> configKeys;

        configKeys = recommendationsJson.keys ();    // 'global', 'item', ...
        while (configKeys.hasNext ()) {
            String key;

            key = configKeys.next ();
            if (key.startsWith ("__") == true)  // skip 'comment' keys
                continue;
            switch (key) {
                case WIDGET_TYPE_RECO_GLOBAL:
                    JSONObject globalJson;
                    Iterator<String> globalConfigKeys;
                    String globalKey;
 
                    globalJson = recommendationsJson.getJSONObject ("global");
                    globalConfigKeys = globalJson.keys ();
                    while (globalConfigKeys.hasNext ()) {
                        globalKey = globalConfigKeys.next ();
                        if (globalKey.startsWith ("__"))
                            continue;
                        switch (globalKey) {
                            case WIDGET_TYPE_RECO_GLOBAL_BESTSELLER:
                                parseRecoGlobalBestSeller (globalJson.getJSONArray (WIDGET_TYPE_RECO_GLOBAL_BESTSELLER));
                                break;
                            case WIDGET_TYPE_RECO_GLOBAL_TRENDING:
                                parseRecoGlobalTrending (globalJson.getJSONArray (WIDGET_TYPE_RECO_GLOBAL_TRENDING));
                                break;
                            default:
                                MessageLogger.logError (String.format ("Unknown global recommendation widget: %s", globalKey));
                        }
                    }
                    break;

                case WIDGET_TYPE_RECO_ITEM:
                    JSONObject itemJson;
                    Iterator<String> itemConfigKeys;
                    String itemKey;

                    itemJson = recommendationsJson.getJSONObject ("item");
                    itemConfigKeys = itemJson.keys ();
                    while (itemConfigKeys.hasNext ()) {
                        itemKey = itemConfigKeys.next ();
                        if (itemKey.startsWith ("__"))
                            continue;
                        switch (itemKey) {
                            case WIDGET_TYPE_RECO_ITEM_EXP_DRIVEN:
                                parseRecoItemExpDriven (itemJson.getJSONArray (WIDGET_TYPE_RECO_ITEM_EXP_DRIVEN));
                                break;

                            case WIDGET_TYPE_RECO_ITEM_FREQ_BOUGHT:
                                parseRecoItemFreqBought (itemJson.getJSONArray (WIDGET_TYPE_RECO_ITEM_FREQ_BOUGHT));
                                break;

                            case WIDGET_TYPE_RECO_ITEM_FREQ_VIEWED:
                                parseRecoItemFreqViewed (itemJson.getJSONArray (WIDGET_TYPE_RECO_ITEM_FREQ_VIEWED));
                                break;

                            case WIDGET_TYPE_RECO_ITEM_SIMILAR:
                                parseRecoItemSimilar (itemJson.getJSONArray (WIDGET_TYPE_RECO_ITEM_SIMILAR));
                                break;
                            default:
                                MessageLogger.logError (String.format ("Unknown item recommendation widget: %s", itemKey));
                        }
                    }
                    break;

                case WIDGET_TYPE_RECO_PERSONAL:
                    JSONObject personalizationJson;
                    Iterator<String> personalizationConfigKeys;
                    String personalizationKey;

                    personalizationJson = recommendationsJson.getJSONObject ("personalization");
                    personalizationConfigKeys = personalizationJson.keys ();
                    while (personalizationConfigKeys.hasNext ()) {
                        personalizationKey = personalizationConfigKeys.next ();
                        if (personalizationKey.startsWith ("__"))
                            continue;
                        switch (personalizationKey) {
                            case WIDGET_TYPE_RECO_PERSONALIZE_PAST_PURCHASE:
                                parseRecoPersonalizePastPurchase (personalizationJson.getJSONArray (WIDGET_TYPE_RECO_PERSONALIZE_PAST_PURCHASE));
                                break;
                            case WIDGET_TYPE_RECO_PERSONALIZE_RECENTLY_VIEWED:
                                parseRecoPersonalizeRecentlyViewed (personalizationJson.getJSONArray (WIDGET_TYPE_RECO_PERSONALIZE_RECENTLY_VIEWED));
                                break;
                            default:
                                MessageLogger.logError (String.format ("Unknown personalization recommendation widget: %s", personalizationKey));
                        }
                    }
                    break;

                case WIDGET_TYPE_RECO_VISUAL:
                    JSONObject visualJson;
                    Iterator<String> visualConfigKeys;
                    String visualKey;

                    visualJson = recommendationsJson.getJSONObject ("visual");
                    visualConfigKeys = visualJson.keys ();
                    while (visualConfigKeys.hasNext ()) {
                        visualKey = visualConfigKeys.next ();
                        if (visualKey.startsWith ("__"))
                            continue;
                        switch (visualKey) {
                            case WIDGET_TYPE_RECO_VISUAL_UPLOAD:
                                parseRecoVisualUpload (visualJson.getJSONArray (WIDGET_TYPE_RECO_VISUAL_UPLOAD));
                                break;
                            case WIDGET_TYPE_RECO_VISUAL_RECO:
                                parseRecoVisualReco (visualJson.getJSONArray (WIDGET_TYPE_RECO_VISUAL_RECO));
                                break;
                            default:
                                MessageLogger.logError (String.format ("Unknown visual recommendation widget: %s", visualKey));
                        }
                    }
                    break;
            }   // end switch RECO widgets
        } // end while eRECO widgets
    } // end parse RECO widgets

    private void parsePathwaysCategories (JSONArray categoriesJsonArray) {
        for (int i = 0; i < categoriesJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) categoriesJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                if (widgetJson.has ("cat_id")) {
                    String value;

                    value = widgetJson.getString ("cat_id");
                    if (value.equals ("UNDEFINED") == false)
                        widgetRecord.setCatId (value);
                }
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parsePathwaysKeywords (JSONArray keywordsJsonArray) {
        for (int i = 0; i < keywordsJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) keywordsJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                if (widgetJson.has ("query")) {
                    String value;
    
                    value = widgetJson.getString ("query");
                    if (value.equals ("UNDEFINED") == false)
                        widgetRecord.setQuery (value);
                }
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoGlobalBestSeller (JSONArray bestsellerJsonArray) {
        for (int i = 0; i < bestsellerJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) bestsellerJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoGlobalTrending (JSONArray trendingJsonArray) {
        for (int i = 0; i < trendingJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) trendingJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoItemExpDriven (JSONArray expJsonArray) {
        for (int i = 0; i < expJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) expJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                if (widgetJson.has ("item_id")) {
                    String value;

                    value = widgetJson.getString ("item_id");
                    if (value.equals ("UNDEFINED") == false)
                        widgetRecord.setItemId (value);
                }
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoItemFreqBought (JSONArray freqboughtJsonArray) {
        for (int i = 0; i < freqboughtJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) freqboughtJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                if (widgetJson.has ("item_id")) {
                    String value;

                    value = widgetJson.getString ("item_id");
                    if (value.equals ("UNDEFINED") == false)
                        widgetRecord.setItemId (value);
                }
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoItemFreqViewed (JSONArray freqviewedJsonArray) {
        for (int i = 0; i < freqviewedJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) freqviewedJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                if (widgetJson.has ("item_id")) {
                    String value;

                    value = widgetJson.getString ("item_id");
                    if (value.equals ("UNDEFINED") == false)
                        widgetRecord.setItemId (value);
                }
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoItemSimilar (JSONArray similarJsonArray) {
        for (int i = 0; i < similarJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) similarJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                if (widgetJson.has ("item_id")) {
                    String value;

                    value = widgetJson.getString ("item_id");
                    if (value.equals ("UNDEFINED") == false)
                        widgetRecord.setItemId (value);
                }
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoPersonalizePastPurchase (JSONArray pastpurchaseJsonArray) {
        for (int i = 0; i < pastpurchaseJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) pastpurchaseJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoPersonalizeRecentlyViewed (JSONArray recentviewedJsonArray) {
        for (int i = 0; i < recentviewedJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) recentviewedJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoVisualUpload (JSONArray vissearchJsonArray) {
        for (int i = 0; i < vissearchJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) vissearchJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

    private void parseRecoVisualReco (JSONArray visrecoJsonArray) {
        for (int i = 0; i < visrecoJsonArray.length(); i++) {
            JSONObject widgetJson;

            widgetJson = (JSONObject) visrecoJsonArray.get (i);
            if (widgetJson.has (WIDGET_CONFIG_KEY_WID)) {
                String widValue;
                WidgetRecord widgetRecord;

                widValue = widgetJson.getString (WIDGET_CONFIG_KEY_WID);
                if (widValue.equals (WIDGET_CONFIG_KEY_WID_UNDEFINED))  // skip 'UNDEFINED' wid
                    continue;
                widgetRecord = new WidgetRecord (widgetJson.getString (WIDGET_CONFIG_KEY_WCODE), widgetJson.getString (WIDGET_CONFIG_KEY_WID));
                if (widgetJson.has ("item_id")) {
                    String value;

                    value = widgetJson.getString ("item_id");
                    if (value.equals ("UNDEFINED") == false)
                        widgetRecord.setItemId (value);
                }
                this.allWidgetsInConfig.add (widgetRecord);
            }
        }
    }

}
