package com.bloomreach.brxdemos.pacifichome.translate.pixel;

// various constants used for pixelLogs
public class CloneConstants {

    public final static String HOMEPAGE_URL = "https://pacific.bloomreach.com/home";
    public final static String HOMEPAGE_TITLE = "PacificHome";
    public final static String PACIFICHOME_DOMAIN = "bloomreach.com";
    public final static String PACIFICHOME_CHANNEL = "home";    // included in each URL
    public final static String PACIFICHOME_DEFAULT_TITLE = "PacificHome";
    public final static String CHECKOUT_PAGE_URL = "https://pacific.bloomreach.com/home/orders";

    public final static String PACIFICHOME_ACCOUNT_ID = "6413";
    public final static String SEARCH_PAGE_URL_PREFIX = "https://pacific.bloomreach.com/home/search?_sq=";
    public final static String SEARCH_PAGE_URL_PREFIX_NORMALIZED = "https://pacific.bloomreach.com/home/search/$QUERY?_sq=";

    // pacifichome has both english and french domains. In order to get
    // analytics processed correctly, need to include 'domain_key'. Currently
    // we have analytics for only 'english' site 
    // NOTE: "PACIFICHOME_DOMAIN" and "PACIFICHOME_DOMAIN_KEY" are different from
    // each other
    public final static String PACIFICHOME_DOMAIN_KEY = "pacifichome";
    public final static String PACIFICHOME_AUTH_KEY = "bcvpynhij980k0y1";

    public final static int CLONE_STATUS_OK     = 101;
    public final static int CLONE_STATUS_ERROR  = 102;
    public final static int CLONE_STATUS_REJECT = 103;

    // this offset value must match the one used for preProcessed feed
    public final static int FIXED_OFFSET_FOR_PID = 0; // zero offset of PacificHome

    // actual product page url is like: 
    // https://pacific.bloomreach.com/products/<pid>
    public final static String PRODUCT_URL_PREFIX = "https://pacific.bloomreach.com/home/products/";

    // prefix for category page urls
    // actual url: <PREFIX> + seo-friendly-crumb
    public final static String CATEGORY_URL_PREFIX = "https://pacific.bloomreach.com/home/categories/";

    // fixed add-to-cart page url
    public final static String ATC_PAGE_URL = "https://pacific.bloomreach.com/home/cartpage";
    public final static String CONVERSION_PAGE_URL = "https://pacific.bloomreach.com/home/thankyou";

}
