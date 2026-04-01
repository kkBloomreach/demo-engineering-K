package com.bloomreach.trafficgenerator.site.journeydata.templates;

// "BRData" similar to br_trk's br_data object in java script
// Information to be sent via pix.gif is collected in this object

import java.util.HashMap;
import java.util.Map;

import com.bloomreach.trafficgenerator.MessageLogger;

import java.net.URLEncoder;

public class ApiBRData {

    // predefined param names as they are required in BR
    public final static String PARAMNAME_ACCOUNT_ID = "account_id";
    public final static String PARAMNAME_AUTH_KEY = "auth_key";
    public final static String PARAMNAME_DOMAIN_KEY = "domain_key";
    public final static String PARAMNAME_REQUEST_ID = "request_id";
    public final static String PARAMNAME_BR_UID2 = "_br_uid_2";
    public final static String PARAMNAME_URL = "url";
    public final static String PARAMNAME_REF_URL = "ref_url";
    public final static String PARAMNAME_REQUEST_TYPE = "request_type";
    public final static String PARAMNAME_ROWS = "rows";
    public final static String PARAMNAME_START = "start";
    public final static String PARAMNAME_FL = "fl";
    public final static String PARAMNAME_FIELDS = "fields"; // used in widget API call
    public final static String PARAMNAME_Q = "q";
    public final static String PARAMNAME_SEARCH_TYPE  = "search_type";
    public final static String PARAMNAME_RBS_SEGMENT = "segment";
    public final static String PARAMNAME_USER_ID = "user_id";
    public final static String PARAMNAME_VIEW_ID = "view_id";
    public final static String PARAMNAME_CDP_SEGMENTS = "cdp_segments";
    public final static String PARAMNAME_BOOST = "boost";
    public final static String PARAMNAME_CATALOG_VIEWS = "catalog_views";   // used in suggest api call
    public final static String PARAMNAME_WIDGET_WRID = "wrid"; // available in widget-api response
    public final static String PARAMNAME_WIDGET_WQ = "wq"; // optional for non-query widgets
    public final static String PARAMNAME_WIDGET_WID = "wid"; // widget id
    public final static String PARAMNAME_WIDGET_WTY = "wty"; // widget type (available in widget api response)
    public final static String PARAMNAME_WIDGET_ITEMID = "item_ids"; // item_id for product/category/content item
    public final static String PARAMNAME_WIDGET_SKU = "sku"; // sku for product item

    public final static String SEGMENT_CUSTOMER_PROFILE = "customer_profile";
    // fl_list must include all attribs used in building API responseDoc (see Dispatcher.java)
    // 'style' needed for PacificApparel only
    public final static String DEFAULT_FL_LIST = "pid,price,title,sale_price,url,sku_thumb_image,skuid,sku_price,sku_sale_price,style";
    public final static String MAX_ROWS = "120";  // rows in API response; start is 0 by default
    public final static String SEARCH_TYPE_CATEGORY  = "category";
    public final static String SEARCH_TYPE_KEYWORD = "keyword";
    public final static String ATTRIB_NAME_CAMPAIGN = "campaign"; // attrib name in feed

    public final static String HEADER_USER_AGENT = "user_agent";

    private HashMap <String, String> brData;

    public ApiBRData () {
        this.brData = new HashMap <String, String> ();
    }

    public void setParam (String key, String value) {
        this.brData.put (key, value);
    }

    // this method used to report value in warning/error message
    public String getParam (String key) {
        return ((String) this.brData.get (key));
    }

    // given all api params, generate a query string to be
    // sent in pix.gif API call
    public String constructQueryParams () {
        StringBuffer paramStrBuf = null;

        // attribute name starting with "_" ignored. These are used for "COMMENTS"
        // values starting with ? are ignored
        for (Map.Entry <String, String> entry : this.brData.entrySet ()) {
            String name = entry.getKey ();
            String value = entry.getValue ();
            String encodedValue;

            if ((name.startsWith ("__") == true) || (value.startsWith ("?") == true))
                continue;

            try {
                encodedValue = URLEncoder.encode (value, "UTF-8");
            } catch (Exception e) {
                MessageLogger.logWarning( ("Exception in encoding api query param"));
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


