package com.bloomreach.trafficgenerator.site.journeydata.templates;

// "BRData" similar to br_trk's br_data object in java script
// Information to be sent via pix.gif is collected in this object

import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;

import com.bloomreach.trafficgenerator.MessageLogger;

public class PixelBRData {

    // predefined param names as they are required in BR
    public final static String PARAMNAME_ACCOUNT_ID = "acct_id";
    public final static String PARAMNAME_AUTH_KEY = "auth_key";
    public final static String PARAMNAME_DOMAIN_KEY = "domain_key";
    public final static String PARAMNAME_REQUEST_ID = "request_id";
    public final static String PARAMNAME_PAGE_TITLE = "title";
    // public final static String PARAMNAME_BR_UID2 = "_br_uid_2";
    public final static String PARAMNAME_COOKIE2 = "cookie2";   // cookie param in pixelData
    public final static String PARAMNAME_URL = "url";
    public final static String PARAMNAME_REF_URL = "ref";   // pixel 'refUrl' param name = ref
    public final static String PARAMNAME_SEARCH_Q = "q"; // used in search event
    public final static String PARAMNAME_SEARCH_AQ = "aq"; // used in suggest event
    public final static String PARAMNAME_SEARCH_TERM = "search_term";   // used in search result page
    public final static String PARAMNAME_RBS_SEGMENT = "segment";
    public final static String PARAMNAME_USER_ID = "user_id";
    public final static String PARAMNAME_VIEW_ID = "view_id";
    public final static String PARAMNAME_CDP_SEGMENTS = "cdp_segments";
    public final static String PARAMNAME_PROD_ID = "prod_id";
    public final static String PARAMNAME_PROD_NAME = "prod_name";
    public final static String PARAMNAME_CAT_NAME = "cat";
    public final static String PARAMNAME_CAT_ID = "cat_id";
    public final static String PARAMNAME_SKU = "sku";
    public final static String PARAMNAME_BASKET_VALUE = "basket_value";
    public final static String PARAMNAME_BASKET = "basket";
    public final static String PARAMNAME_IS_CONVERSION = "is_conversion";
    public final static String PARAMNAME_ORDER_ID= "order_id";
    public final static String PARAMNAME_TEST_DATA = "test_data";
    public final static String PARAMNAME_PAGE_TYPE = "ptype"; // "homepage", "product", "category", ...
    public final static String PARAMNAME_PIXEL_TYPE = "type"; // "pageview", "event"
    public final static String PARAMNAME_EVENT_GROUP = "group"; // "suggest", "cart", "product", "widget"
    public final static String PARAMNAME_EVENT_ETYPE = "etype"; // "click", "click-add", "submit", "quickview"
    public final static String PARAMNAME_WIDGET_WRID = "wrid"; // available in widget-api response
    public final static String PARAMNAME_WIDGET_WQ = "wq"; // optional for non-query widgets
    public final static String PARAMNAME_WIDGET_WID = "wid"; // widget id
    public final static String PARAMNAME_WIDGET_WTY = "wty"; // widget type (available in widget api response)
    public final static String PARAMNAME_WIDGET_ITEMID = "item_id"; // item_id for product/category/content item
    public final static String PARAMNAME_WIDGET_SKU = "sku"; // sku for product item
    
    public final static String SEGMENT_CUSTOMER_PROFILE = "customer_profile";
    public final static String PAGE_TYPE_HOME = "homepage";
    public final static String PAGE_TYPE_PRODUCT = "product";
    public final static String PAGE_TYPE_CATEGORY = "category";
    public final static String PAGE_TYPE_SEARCH = "search";
    public final static String PAGE_TYPE_OTHER = "other";
    public final static String PAGE_TYPE_CONTENT = "content";
    public final static String PAGE_TYPE_THEMATIC = "thematic";
    public final static String PIXEL_TYPE_PAGEVIEW = "pageview";
    public final static String PIXEL_TYPE_EVENT = "event";
    public final static String EVENT_GROUP_SUGGEST = "suggest"; // used for suggest and search event
    public final static String EVENT_GROUP_CART = "cart";       // used for atc
    public final static String EVENT_GROUP_WIDGET = "widget";       // used for widget
    public final static String EVENT_ETYPE_CLICKADD = "click-add"; // used in atc
    public final static String EVENT_ETYPE_SUBMIT = "submit";   // used in search event
    public final static String EVENT_ETYPE_CLICK = "click";   // used in suggest event
    public final static String EVENT_ETYPE_WIDGET_VIEW = "widget-view";   // used in widget-view event
    public final static String EVENT_ETYPE_WIDGET_CLICK = "widget-click";   // used in widget-click event
    public final static String EVENT_ETYPE_WIDGET_ATC = "widget-add";   // used in widget-click event
    public final static String HEADER_USER_AGENT = "user_agent";

    private HashMap <String, String> brData;

    public PixelBRData () {
        this.brData = new HashMap <String, String> ();
    }

    public void setParam (String key, String value) {
        this.brData.put (key, value);
    }

    // this method is primarily for debugging purpose...
    public String getParam (String key) {
        return (this.brData.get (key));
    }

    // given all pixel params, generate a query string to be
    // sent in pix.gif API call
    public String constructQueryParams () {
        StringBuffer paramStrBuf = null;

        // attribute name starting with "_" ignored. These are used for "COMMENTS"
        // values starting with ? are ignored
        for (Map.Entry <String, String> entry : this.brData.entrySet ()) {
            String name = entry.getKey ();
            String value = entry.getValue ();
            String encodedValue;

            //@@@ TO DO
            // For some reason, rarely, one of these is null which causes NullPtr exception
            // Need to hunt sometime...
            if ((name == null) || (value == null)) {
                MessageLogger.logError  ("PixelBRData, name or value is null");
                if (name != null)
                    MessageLogger.logError ("PixelBRData name not null, = " + name);
                else
                    MessageLogger.logError ("PixelBRData name is null");

                if (value != null)
                    MessageLogger.logError ("PixelBRData value not null, = " + value);
                else
                    MessageLogger.logError ("PixelBRData value is null");
                continue; 
            }

            if ((name.startsWith ("__") == true) || (value.startsWith ("?") == true))
                continue;

            try {
                encodedValue = URLEncoder.encode (value, "UTF-8");
            } catch (Exception e) {
                MessageLogger.logWarning ("Exception in encoding pixel parameter");
                encodedValue = "";
            }
            String kvPair = name + '=' + encodedValue;
            if (paramStrBuf == null) {
                paramStrBuf = new StringBuffer ();
            } else {
                paramStrBuf.append ("&");
            }
            paramStrBuf.append (kvPair);
        }
        return new String (paramStrBuf);   // note - leading '?' used in URL is not in this string
    }
}


