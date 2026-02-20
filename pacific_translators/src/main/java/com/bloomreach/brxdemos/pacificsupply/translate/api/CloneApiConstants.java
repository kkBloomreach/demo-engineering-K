// various constants used for cloning
package com.bloomreach.brxdemos.pacificsupply.translate.api;

public class CloneApiConstants {

    public final static String PACIFICSUPPLY_ACCOUNT_ID = "6370";
    public final static int    PACIFICSUPPLY_ACCOUNT_ID_INT = 6370;
    public final static String PACIFICSUPPLY_AUTH_KEY = "1vjobidilg5gcbpn";
    public final static String MERCHANT_DOMAIN_KEY = "pacific_supply";
    public final static String SITE_DOMAIN_KEY = ""; // blank
    public final static String HOMEPAGE_URL = "https://pacificsupply.bloomreach.com";

    public final static int CLONE_STATUS_OK     = 101;
    public final static int CLONE_STATUS_ERROR  = 102;
    public final static int CLONE_STATUS_REJECT = 103;

    // this offset value must match the one used for preProcessed feed
    public final static int FIXED_OFFSET_FOR_PID = 0x1abc;



    public final static String HOMEPAGE_TITLE = "PacificSupply";
    public final static String PACIFICSUPPLY_DOMAIN = "bloomreach.com";
    public final static String PACIFICSUPPLY_DEFAULT_TITLE = "PacificSupply";

    public final static String SEARCH_PAGE_URL_PREFIX = "https://pacificsupply.bloomreach.com/search?_sq=";

    public final static String DEFAULT_VIEW_ID = "master";
    public final static String VIEW_ID_LIST [] = { "ContractorCommercial",
                                                    "ContractorIndustrial",
                                                    "ContractorResidential",
                                                    "Fabrication",
                                                    "MaintenanceElectrical",
                                                    "MaintenanceIndustrial",
                                                    "OEM",
                                                    "Utility",
                                                    "master"
                                                 };

    // actual product page url is like: 
    // https://pacificsupply.bloomreach.com/products/<pid>
    public final static String PRODUCT_URL_PREFIX = "https://pacificsupply.bloomreach.com/products/";

    // prefix for category page urls
    // actual url: <PREFIX> + seo-friendly-crumb
    public final static String CATEGORY_URL_PREFIX = "https://pacificsupply.bloomreach.com/categories/";

    // constants used in clone packages
    public final static String SOURCE_APILOG_DIR = "./source/"; 
    public final static String PREPROCESSED_FEED_PATH = "./source/translated_feed.tsv";
    public final static String OUTPUT_APILOG_DIR = "./output/"; 
 
    // while processing each apiLog, extract the uid and viewId values in it
    // then store the entire map to a local file. It is then needed to populate
    // corresponding uid<->viewId values in pixel logs. (Original merchant's pixel logs
    // did not include viewId values --:( )
    public final static String UID_TO_VIEWID_MAP_FILENAME = "UidToViewIdMap.tsv"; 

}
