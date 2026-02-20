package com.bloomreach.trafficgenerator.site.config;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Enumeration;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.trafficgenerator.MessageLogger;

public class SiteConfig {

    public final static String SEGMENTATION_TYPE_RTS = "RTS";
    public final static String SEGMENTATION_TYPE_RBS = "RBS";
    public final static String SEGMENTATION_TYPE_NONE = "NONE";

    private static JSONObject siteConfig = null;
    private static Hashtable <String, String> accountConfig = null;
    private static Hashtable <String, String> urlsConfig = null;
    private static Hashtable <String, String> productCatalogConfig = null;
    private static ArrayList <String> views = null;
    private static ArrayList <String> excludeProducts = null;
    private static ArrayList <String> excludeCategoryIds = null;
    private static RTSConfig rtsConfig = null;
    private static RBSConfig rbsConfig = null;
    private static VisitorConfig visitorConfig = null;

    public SiteConfig () {
    }

    public boolean load (String accountConfigPath) throws Exception {
        File configFile;

        configFile = new File (accountConfigPath);
        if (configFile.exists () == false)
            return false;

        siteConfig = parseConfig (configFile);

        if (siteConfig.has ("ACCOUNT")) {
            accountConfig = parseConfigParams (siteConfig.getJSONObject ("ACCOUNT"));
        }

        if (siteConfig.has ("URLS")) {
            urlsConfig = parseConfigParams (siteConfig.getJSONObject ("URLS"));
        }

        if (siteConfig.has ("PRODUCT_CATALOG")) {
            productCatalogConfig = parseConfigParams (siteConfig.getJSONObject ("PRODUCT_CATALOG"));
        }

        if (siteConfig.has ("VIEWS")) {
            views = parseConfigList (siteConfig.getJSONArray ("VIEWS"));
        }

        if (siteConfig.has ("EXCLUDE_PRODUCTS")) {
            excludeProducts = parseConfigList (siteConfig.getJSONArray ("EXCLUDE_PRODUCTS"));
        }

        if (siteConfig.has ("EXCLUDE_CATEGORIES")) {
            excludeCategoryIds = parseConfigList (siteConfig.getJSONArray ("EXCLUDE_CATEGORIES"));
        }

        if (siteConfig.has ("RTS")) {
            rtsConfig = parseRTS (siteConfig.getJSONObject ("RTS"));
        }

        if (siteConfig.has ("RBS")) {
            rbsConfig = parseRBS (siteConfig.getJSONObject ("RBS"));
        }

        if (siteConfig.has ("VISITOR")) {
            visitorConfig = parseVisitor (siteConfig.getJSONObject ("VISITOR"));
        }

        if ((accountConfig == null) || (urlsConfig == null) || (productCatalogConfig == null)) { 
            throw new Exception ("Cannot find essential siteConfigs");
        }
   
        if ((rtsConfig != null) && (rbsConfig != null)) {
            throw new Exception ("Found both RTS and RBS segmentations; only one is allowed") ;
        }
 
        return true;
    }

    public static String getAccountConfigParam (String paramName) {
        String value = null;

        if (accountConfig != null)
            value = accountConfig.get (paramName);

        if (value == null)
            MessageLogger.logError ("Account parameter not found: " + paramName);

        return value;
    }

    public static String getUrlConfigParam (String paramName) {
        String value = null;
        String coreUrl = null;
        String fullUrl = null;

        if (urlsConfig != null)
            value = urlsConfig.get (paramName);

        if (value == null) {
            MessageLogger.logError ("URL parameter not found: " + paramName);
            value = ""; // placeholder if param not found
        }

        coreUrl = urlsConfig.get ("SITE_CORE_URL");
        if (coreUrl == null)
            MessageLogger.logError ("coreURL parameter not found");

        // assumes coreUrl does not have trailing slash and value has leading slash
        fullUrl = String.format ("%s%s", coreUrl, value); 
        return fullUrl;
    }

    public static String getProductCatalogConfigParam (String paramName) {
        String value = null;

        if (productCatalogConfig != null)
            value = productCatalogConfig.get (paramName);

        if (value == null)
            MessageLogger.logError ("ProductCatalog parameter not found: " + paramName);

        return value;
    }

    public static ArrayList <String> getViews () {
        return  views;
    }

    public static ArrayList <String> getExcludeProducts () {
        return  excludeProducts;
    }

    public static ArrayList <String> getExcludeCategoryIds () {
        return  excludeCategoryIds;
    }

    public static String getSegmentationType () {
        if (rtsConfig != null)
            return (SEGMENTATION_TYPE_RTS);
        if (rbsConfig != null)
            return (SEGMENTATION_TYPE_RBS);
        return (SEGMENTATION_TYPE_NONE);
    }

    // account may/may-not have any RTS segments. If they are, return segment names. 
    public static ArrayList<String> getRTSSegmentNames () {
        ArrayList<String> segmentNames;
        Hashtable <String, String> segment_and_idlist;
        Enumeration<String> enumNames;

        if (rtsConfig == null) {
            MessageLogger.logInfo ("No RTS in siteConfig");
            return null;
        }

        segment_and_idlist = rtsConfig.getSegmentAndIdList ();
        segmentNames = new ArrayList <String> ();
        enumNames = segment_and_idlist.keys ();
        while (enumNames.hasMoreElements ()) {
            segmentNames.add (enumNames.nextElement ());
        }

        return segmentNames;
    }

    // for the given segment, return A:B where A is segmentationId, B is segmentId
    public static String getRTSKeyValuePair (String segment) {
        String segmentationId = null;
        String segmentId = null;
        String keyValuePair = null;
        Hashtable <String, String> segment_and_idlist;

        if (rtsConfig == null) {
            MessageLogger.logInfo ("No RTS in siteConfig");
            return null;
        }

        segmentationId = rtsConfig.getSegmentationId ();
        segment_and_idlist = rtsConfig.getSegmentAndIdList ();
        segmentId = segment_and_idlist.get (segment);   // returns value like 64e74.... 
        if (segmentationId == null || segmentId == null) {
            MessageLogger.logWarning ("RTS segment not found: " + segment);
            return null;
        }

        keyValuePair = segmentationId + ":" + segmentId;
        return keyValuePair;
    }

    // currently only 'customer-profile' is supported
    public static ArrayList <String> getRBSCustomerProfileNames () {
        if (rbsConfig == null) {
            MessageLogger.logInfo ("No RBS in siteConfig");
            return null;
        }

        return rbsConfig.getProfileNames ();
    }

    public static String getSpecialVisitorId () {
        if (visitorConfig == null) {
            MessageLogger.logInfo ("No special visitor defined in siteConfig");
            return null;
        }
        return visitorConfig.getSpecialVisitorId ();
    }

    public static ArrayList<Integer> getSpecialVisitDays () {
        if (visitorConfig == null) {
            MessageLogger.logInfo ("No special visit days defined in siteConfig");
            return null;
        }
        return visitorConfig.getSpecialVisitDays ();
    }

    ///// INTERNAL METHODS
    private JSONObject parseConfig (File configFile) throws Exception {
        BufferedReader reader;
        String srcLine;
        JSONObject configJson;
        StringBuffer configBuf;

        reader = new BufferedReader (new FileReader (configFile));
        configBuf = new StringBuffer ();
        while ((srcLine = reader.readLine ()) != null) {
            configBuf.append (srcLine);
        }
        reader.close ();

        configJson = new JSONObject (configBuf.toString ());
        return (configJson);
    }

    // This is a 'common' method used by various config sections that have only a 'list-of-strings'
    // returns Hashtable
    private Hashtable <String, String> parseConfigParams (JSONObject jsonObject) throws Exception {
        Iterator<String> keys;
        Hashtable <String, String> configParams;

        keys = jsonObject.keys ();
        configParams = new Hashtable <String, String> ();
        while (keys.hasNext ()) {
            String nextKey;
            String value;

            nextKey = keys.next ();
            value = jsonObject.getString (nextKey);
            configParams.put (nextKey, value);
        }

        return configParams;
    }

    // This is a 'common' method used by various config sections that have only a 'list-of-strings'
    // returns ArrayList
    private ArrayList <String> parseConfigList (JSONObject jsonObject) throws Exception {
        Iterator<String> keys;
        ArrayList <String> configList;

        keys = jsonObject.keys ();
        configList = new ArrayList <String> ();
        while (keys.hasNext ()) {
            String nextKey;

            nextKey = keys.next ();
            configList.add (nextKey);
        }

        return configList;
    }

    // same as above except param is JSONArray
    private ArrayList <String> parseConfigList (JSONArray jsonArray) throws Exception {
        ArrayList <String> configList;

        configList = new ArrayList <String> ();
        for (int i = 0; i < jsonArray.length (); i++) {
            configList.add ((String) jsonArray.get (i));
        }

        return configList;
    }

    private RTSConfig parseRTS (JSONObject jsonObject) throws Exception {
        Hashtable <String, String> segment_and_idlist = null;
        String segmentationId = null;
        RTSConfig rtsConfig;

        Iterator<String> keys = jsonObject.keys ();
        segment_and_idlist = new Hashtable <String, String> ();

        while (keys.hasNext ()) {
            String nextKey;
            
            nextKey = (String) keys.next ();
            if (nextKey.equals ("SEGMENTATION_ID"))
                segmentationId = jsonObject.getString (nextKey);
            else {
                JSONObject segmentObject;
                String segmentId;

                segmentObject = jsonObject.getJSONObject (nextKey);
                segmentId = segmentObject.getString ("SEGMENT_ID");
                segment_and_idlist.put (nextKey, segmentId);    // eg, "BUDGET"->"64e7e..." segmentId
            }
        }

        if ((segmentationId == null) || (segment_and_idlist == null))
            throw new Exception ("RTS Configuration, cannot find SegmentationId and/or segments");

        rtsConfig = new RTSConfig (segmentationId, segment_and_idlist);
        return rtsConfig;
    }

    private RBSConfig parseRBS (JSONObject jsonObject) throws Exception {
        ArrayList<String> profileNames;
        RBSConfig rbsConfig;

        if (jsonObject.has ("CUSTOMER_PROFILES")) {
            JSONArray profilesArray;

            profilesArray = jsonObject.getJSONArray ("CUSTOMER_PROFILES");
            profileNames = new ArrayList <String> ();
            for (int i = 0; i < profilesArray.length (); i++) {
                profileNames.add ((String) profilesArray.get (i));
            }
        } else
            throw new Exception ("RBS Configuration, cannot find customerProfiles");

        rbsConfig = new RBSConfig (profileNames);
        return rbsConfig;
    }

    private VisitorConfig parseVisitor (JSONObject jsonObject) throws Exception {
        String specialVisitorId = null;
        ArrayList<Integer> visitDays = null;
        VisitorConfig visitorConfig;

        if (jsonObject.has ("SPECIAL_VISITOR_ID")) {
            specialVisitorId = jsonObject.getString ("SPECIAL_VISITOR_ID");
        }

        if (jsonObject.has ("SPECIAL_VISIT_DAYS")) {
            JSONArray visitDaysArray;

            visitDaysArray = jsonObject.getJSONArray ("SPECIAL_VISIT_DAYS");
            visitDays = new ArrayList <Integer> ();
            for (int i = 0; i < visitDaysArray.length (); i++) {
                visitDays.add ((int) visitDaysArray.get (i));
            }
        }

        if ((specialVisitorId == null) || (visitDays == null) || (visitDays.size() == 0)) 
            throw new Exception ("VisitorConfiguration, cannot find special visitor information");

        visitorConfig = new VisitorConfig (specialVisitorId, visitDays);
        return visitorConfig;
    }

    // ~~~~~~ INTERNAL CLASSES
    class RTSConfig {
        String segmentationId;
        Hashtable <String, String> segment_and_idlist;

        RTSConfig (String segmentationId, Hashtable<String, String> segment_and_idlist) {
            this.segmentationId = segmentationId;
            this.segment_and_idlist = segment_and_idlist;
        }

        String getSegmentationId () {
            return this.segmentationId;
        }

        Hashtable <String, String> getSegmentAndIdList () {
            return this.segment_and_idlist;
        } 
    }

    class RBSConfig {
        ArrayList <String> profileNames;

        RBSConfig (ArrayList <String> profileNames) {
            this.profileNames = profileNames;
        }

        ArrayList <String> getProfileNames () {
            return this.profileNames;
        }
    }

    class VisitorConfig {
        String specialVisitorId;
        ArrayList <Integer> specialVisitDays;

        VisitorConfig (String specialVisitorId, ArrayList <Integer> specialVisitDays) {
            this.specialVisitorId = specialVisitorId;
            this.specialVisitDays = specialVisitDays;
        }

        String getSpecialVisitorId () {
            return this.specialVisitorId;
        }

        ArrayList <Integer> getSpecialVisitDays () {
            return this.specialVisitDays;
        }
    }
}

/***********
// 
// 
//     private SiteConfigParams parseSite (JSONObject jsonObject) throws Exception {
//         Iterator<String> keys;
//         Hashtable <String, String> urls = null;
//         String catalogPath = null;
//         ArrayList <String> views = null;
//         ArrayList <String> excludedProducts = null;
//         ArrayList <String> excludedCategories = null;
//         SiteConfigParams siteConfigParams;
// 
//         if (jsonObject.has ("URLS")) {
//             JSONObject urlsObject;
// 
//             urls = new Hashtable <String, String> ();
//             urlsObject = jsonObject.getJSONObject ("URLS");
//             keys = urlsObject.keys ();
//             while (keys.hasNext ()) {
//                 String nextKey;
//                 String value;
// 
//                 nextKey = keys.next ();
//                 value = jsonObject.getString (nextKey);
//                 urls.put (nextKey, value);
//            } 
//         }
// 
//         if (jsonObject.has ("PRODUCT_CATALOG")) {
//             JSONObject catalogObject;
// 
//             catalogObject = jsonObject.getJSONObject ("PRODUCT_CATALOG");
//             catalogPath = catalogObject.getString ("ORIGINAL_CATALOG_PATH");
//        
//             if (catalogObject.has ("VIEWS")) {
//                 JSONArray viewsArray;
// 
//                 viewsArray = catalogObject.getJSONArray ("VIEWS");
//                 views = new ArrayList <String> ();
//                 for (int i = 0; i < viewsArray.length (); i++) {
//                     views.add ((String) viewsArray.get (i));
//                 }
//             } 
//         }
// 
//         if (jsonObject.has ("EXCLUSIONS")) {
//             JSONObject exclusionsObject;
// 
//             exclusionsObject = jsonObject.getJSONObject ("EXCLUSIONS");
//             if (exclusionsObject.has ("EXCLUDE_PRODUCTS")) {
//                 JSONArray excludedProductsArray;
// 
//                 excludedProductsArray = exclusionsObject.getJSONArray ("EXCLUDE_PRODUCTS");
//                 excludedProducts = new ArrayList <String> ();
//                 for (int i = 0; i < excludedProductsArray.length (); i++) {
//                     excludedProducts.add ((String) excludedProductsArray.get (i));
//                 }
//             }
// 
//             if (exclusionsObject.has ("EXCLUDE_CATEGORIES")) {
//                 JSONArray excludedCategoriesArray;
// 
//                 excludedCategoriesArray = exclusionsObject.getJSONArray ("EXCLUDE_CATEGORIES");
//                 excludedCategories = new ArrayList <String> ();
//                 for (int i = 0; i < excludedCategoriesArray.length (); i++) {
//                     excludedCategories.add ((String) excludedCategoriesArray.get (i));
//                 }
//             }
//         }
// 
//         siteConfigParams = new SiteConfigParams (urls,
//                                                 catalogPath,
//                                                 views,
//                                                 excludedProducts,
//                                                 excludedCategories);
//         return siteConfigParams;
//     }
// 
//     class SiteConfigParams {
// 
//         Hashtable <String, String> urls;
//         String catalogPath;
//         ArrayList <String> views;
//         ArrayList <String> excludedProducts;
//         ArrayList <String> excludedCategories;
// 
//         SiteConfigParams (Hashtable <String, String> urls,
//                           String catalogPath,
//                           ArrayList<String> views,
//                           ArrayList<String> excludedProducts,
//                           ArrayList<String> excludedCategories) {
//             this.urls = urls;
//             this.catalogPath = catalogPath;
//             this.views = views;
//             this.excludedProducts = excludedProducts;
//             this.excludedCategories = excludedCategories;
//         }
// 
//         Hashtable<String, String> getUrls () {
//             return this.urls;
//         }
// 
//         String getCatalogPath () {
//             return this.catalogPath;
//         }
// 
//         ArrayList <String> getViews () {
//             return this.views;
//         }
// 
//         ArrayList <String> getExcludedProducts () {
//             return this.excludedProducts;
//         }
// 
//         ArrayList <String> getExcludedCategories () {
//             return this.excludedCategories;
//         }
// 
//     }
//     private VisitorConfigParams parseSite (JSONObject jsonObject) throws Exception {
//         Iterator<String> keys;
// 
//         if (jsonObject.has ("SEGMENTATION")) {
//             JSONObject segmentationObject;
// 
//             segmentationObject = jsonObject.getJSONObject ("SEGMENTATION");
//             if (segmentationObject.has ("RTCS")) {
//                 rtcsParams = new Hashtable <String, String> ();
// 
//             }
//         }
//     }
// 
//     class VisitorConfigParams {
//         Hashtable <String, String> rtcsParams = null;
//         ArrayList <String> rbsCustomerProfiles = null;
//     }
// 
//     private Hashtable <String, String> parseAccount (JSONObject jsonObject) throws Exception {
//         Iterator<String> keys;
//         Hashtable <String, String> accountConfig;
// 
//         keys = jsonObject.keys ();
//         accountConfig = new Hashtable <String, String> ();
//         while (keys.hasNext ()) {
//             String nextKey;
//             String value;
// 
//             nextKey = keys.next ();
//             value = jsonObject.getString (nextKey);
//             accountConfig.put (nextKey, value);
//         }
// 
//        return accountConfig;
//     }
// 
//     private Hashtable <String, String> parseAnalytics (JSONObject jsonObject) throws Exception {
//         Iterator<String> keys;
//         Hashtable <String, String> analyticsConfig;
// 
//         keys = jsonObject.keys ();
//         analyticsConfig = new Hashtable <String, String> ();
//         while (keys.hasNext ()) {
//             String nextKey;
//             String value;
// 
//             nextKey = keys.next ();
//             value = jsonObject.getString (nextKey);
//             analyticsConfig.put (nextKey, value);
//         }
// 
//         return analyticsConfig;
//     }
// 
//     private Hashtable <String, String> parseUrls (JSONObject jsonObject) throws Exception {
//         Iterator<String> keys;
//         Hashtable <String, String> urlsConfig;
// 
//         keys = jsonObject.keys ();
//         urlsConfig = new Hashtable <String, String> ();
//         while (keys.hasNext ()) {
//             String nextKey;
//             String value;
// 
//             nextKey = keys.next ();
//             value = jsonObject.getString (nextKey);
//             urlsConfig.put (nextKey, value);
//         }
// 
//         return urlsConfig;
//     }
// 
//     private Hashtable <String, String> parseUrls (JSONObject jsonObject) throws Exception {
//         Iterator<String> keys;
//         Hashtable <String, String> productCatalogConfig;
// 
//         keys = jsonObject.keys ();
//         productCatalogConfig = new Hashtable <String, String> ();
//         while (keys.hasNext ()) {
//             String nextKey;
//             String value;
// 
//             nextKey = keys.next ();
//             value = jsonObject.getString (nextKey);
//             productCatalogConfig.put (nextKey, value);
//         }
// 
//         return productCatalogConfig;
//     }
// 
//     private Hashtable <String, String> parseUrls (JSONObject jsonObject) throws Exception {
//         return (parseConfigList (jsonObject));
//     }
//        if (siteConfig.has ("ANALYTICS")) {
//            analyticsConfig = parseConfigParams (siteConfig.getJSONObject ("ANALYTICS"));
//        }
//    private static Hashtable <String, String> analyticsConfig = null;
//    public static String getAnalyticsConfigParam (String paramName) {
//        String value = null;
//
//        if (analyticsConfig != null)
//            value = analyticsConfig.get (paramName);
//
//        return value;
//    }
// private static ArrayList <String> lowPerformanceCategoryIds = null;
// if (siteConfig.has ("LOW_PERFORMANCE_CATEGORIES")) {
//            lowPerformanceCategoryIds = parseConfigList (siteConfig.getJSONArray ("LOW_PERFORMANCE_CATEGORIES"));
//        }
// public static ArrayList <String> getLowPeformanceCategoryIds () {
//        return lowPerformanceCategoryIds;
//    }
****/
