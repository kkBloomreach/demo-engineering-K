package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

// various constants used for cloning
public class ClonePixelConstants {

    public final static String HOMEPAGE_URL = "https://pacificsupply.bloomreach.com";
    public final static String HOMEPAGE_TITLE = "PacificSupply";
    public final static String PACIFICSUPPLY_DOMAIN = "bloomreach.com";
    public final static String PACIFICSUPPLY_DEFAULT_TITLE = "PacificSupply";
    public final static String CHECKOUT_PAGE_URL = "https://pacificsupply.bloomreach.com/orders";

    public final static String PACIFICSUPPLY_ACCOUNT_ID = "6370";
    public final static String SEARCH_PAGE_URL_PREFIX = "https://pacificsupply.bloomreach.com/search?_sq=";

    public final static int CLONE_STATUS_OK     = 101;
    public final static int CLONE_STATUS_ERROR  = 102;
    public final static int CLONE_STATUS_REJECT = 103;

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

    // this offset value must match the one used for preProcessed feed
    public final static int FIXED_OFFSET_FOR_PID = 0x1abc;

    // actual product page url is like: 
    // https://pacificsupply.bloomreach.com/products/<pid>
    public final static String PRODUCT_URL_PREFIX = "https://pacificsupply.bloomreach.com/products/";

    // prefix for category page urls
    // actual url: <PREFIX> + seo-friendly-crumb
    public final static String CATEGORY_URL_PREFIX = "https://pacificsupply.bloomreach.com/categories/";
}
