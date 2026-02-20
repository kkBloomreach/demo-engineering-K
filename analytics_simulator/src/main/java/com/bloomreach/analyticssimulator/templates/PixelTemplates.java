package com.bloomreach.analyticssimulator.templates;

// load pixel templates into PixelLog.Builder object
// Currently we have three templates - product page, atc, conversion
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.bloomreach.proto.Aggregation.Basket;
import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.proto.Aggregation.PixelLog.CustomVariable;
import com.bloomreach.proto.Aggregation.PixelLog.PixelLogParam;
import com.bloomreach.proto.Aggregation.PixelLog.TrafficSource;
import com.bloomreach.analytics.pixel.CommonFields;
import com.bloomreach.analytics.pixel.PixelCatalog;
import com.bloomreach.analytics.ReferrerTypeParser;

import com.bloomreach.analyticssimulator.MessageLogger;

public class PixelTemplates {

    // these files are expected to be in the 'templateDir"
    private final static String FILENAME_PRODUCT_PAGE_PIXEL = "./simtemplate_prodpixel.txt";
    private final static String FILENAME_ATC_PIXEL = "./simtemplate_atcpixel.txt";
    private final static String FILENAME_CONVERSION_PIXEL = "./simtemplate_convpixel.txt";
    private final static String FILENAME_SEARCH_EVENT_PIXEL = "./simtemplate_searcheventpixel.txt";
    private final static String FILENAME_SEARCHRESULT_PAGE_PIXEL = "./simtemplate_searchresultpagepixel.txt";
    private final static String FILENAME_CATEGORY_PAGE_PIXEL = "./simtemplate_catpixel.txt";

    private String templateDirPath = null;

    HashMap<String, String> cloneKeyValues = null;
    HashMap<String, String> cloneParams = null;
    Basket.Builder pixelBasketBuilder = null; // builder for a basket in a conversion pixel

    public PixelTemplates () {
    }

    // directory containing all the template files
    public void setTemplatesDir (String templateDirPath) {
        this.templateDirPath = templateDirPath;
    }

    public PixelLog.Builder loadProductPixelTemplate () throws Exception {
        PixelLog.Builder prodPixelLogBuilder;

        prodPixelLogBuilder = loadPixelTemplate (templateDirPath, FILENAME_PRODUCT_PAGE_PIXEL);
        return (prodPixelLogBuilder);
    }

    public PixelLog.Builder loadAddToCartPixelTemplate () throws Exception {
        PixelLog.Builder prodPixelLogBuilder;

        prodPixelLogBuilder = loadPixelTemplate (templateDirPath, FILENAME_ATC_PIXEL);
        return (prodPixelLogBuilder);
    }

    public PixelLog.Builder loadConversionPixelTemplate () throws Exception {
        PixelLog.Builder prodPixelLogBuilder;

        prodPixelLogBuilder = loadPixelTemplate (templateDirPath, FILENAME_CONVERSION_PIXEL);
        return (prodPixelLogBuilder);
    }

    public PixelLog.Builder loadSearchEventPixelTemplate () throws Exception {
        PixelLog.Builder searchEventPixelLogBuilder;

        searchEventPixelLogBuilder = loadPixelTemplate (templateDirPath, FILENAME_SEARCH_EVENT_PIXEL);
        return (searchEventPixelLogBuilder);
    }

    public PixelLog.Builder loadSearchResultPagePixelTemplate () throws Exception {
        PixelLog.Builder searchPagePixelLogBuilder;

        searchPagePixelLogBuilder = loadPixelTemplate (templateDirPath, FILENAME_SEARCHRESULT_PAGE_PIXEL);
        return (searchPagePixelLogBuilder);
    }

    public PixelLog.Builder loadCategoryPixelTemplate () throws Exception {
        PixelLog.Builder catPixelLogBuilder;

        catPixelLogBuilder = loadPixelTemplate (templateDirPath, FILENAME_CATEGORY_PAGE_PIXEL);
        return (catPixelLogBuilder);
    }

    private PixelLog.Builder loadPixelTemplate (String templateDirPath, String templatePath ) throws Exception {
        File templateFile;
        LineIterator lineIterator;
        PixelLog.Builder templatePixelLogBuilder = null;

        cloneKeyValues = new HashMap <String, String>();
        cloneParams = new HashMap <String, String>();
        pixelBasketBuilder = null; // builder for a basket in a conversion pixel

        templateFile = new File (templateDirPath, templatePath);
        lineIterator = FileUtils.lineIterator (templateFile, "UTF-8");

        try {
            parsePixelLogFile (lineIterator);
        } catch (Exception e) {
            MessageLogger.logError ("Exception in load template pixel template");
            return (null);
        } finally {
            lineIterator.close ();
        }

        // Use the HashMap values to populate basic (common) fields in a pixelBuilder
        // and return that builder object
        try {
            templatePixelLogBuilder = preparePixelLogBuilder ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception in prepare template pixel builder");
            return (null);
        }

        return (templatePixelLogBuilder);
    }

    /**
     * Parses the incoming clone log file and returns a PixelLog.Builder.
    */
    private void parsePixelLogFile (LineIterator lineIterator) throws IOException {

        // a line can have any of these forms
        //  key : value
        //  params -, followed by key:value and then a closing curly
        //  basket
        while (lineIterator.hasNext ()) {
            String line;
            int indx;
            String keyName;
            String value;
            String paramKeyName;
            String paramValue;

            line = lineIterator.nextLine ();

            // a blank line indicates end-of-one-pixelbloc
            if (line.length () == 0) {
                break;
            }

            if (line.indexOf (':') >= 0) {
                // parse key-value
                indx = line.indexOf (':');
                keyName = line.substring (0,indx).trim ();
                keyName = keyName.replaceAll ("\"", "");
                value = line.substring (indx+1).trim ();
                value = value .replaceAll ("\"", "");

                cloneKeyValues.put (keyName, value); 

            } else if (line.indexOf ("params {") >= 0) {
                line = lineIterator.nextLine ();
                indx = line.indexOf (':');
                paramKeyName = line.substring (indx+1).trim ();
                paramKeyName = paramKeyName.replaceAll ("\"", "");

                line = lineIterator.nextLine ();
                indx = line.indexOf (':');
                paramValue = line.substring (indx+1).trim ();
                paramValue = paramValue.replaceAll ("\"", "");

                // trailing curly
                line = lineIterator.nextLine ();

                cloneParams.put (paramKeyName, paramValue);
            } else if (line.indexOf ("basket {") >= 0) {
                TemplatePixelBasket templatePixelBasket;

                templatePixelBasket = new TemplatePixelBasket ();

                // this method will have parsed trailing 'curly'
                pixelBasketBuilder  = templatePixelBasket.parseBasket(lineIterator);
            }
        }

    }

    // this is a common method used by all templets (product,atc,conversion, searchPage)
    // Use the hashMap prepared by the parser and generate a PixelLog.Builder object
    private PixelLog.Builder preparePixelLogBuilder () throws Exception {
        String value;
        PixelLog.Builder pixelLogBuilder;
        String merchantUrl;

        pixelLogBuilder = PixelLog.newBuilder ();
        merchantUrl = null;

        // use the HashMap entries to set specific values in PixelLog object (via the pixelLog builder)
        // date
        value = cloneKeyValues.get ("date");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setDate (value);
        }

        // log time
        value = cloneKeyValues.get ("log_time");
        if (StringUtils.isNotEmpty (value)) {
            try {
                long timeValue;

                timeValue = Long.parseLong (value);
                pixelLogBuilder.setLogTime (timeValue);
            } catch (NumberFormatException nfe) {
                pixelLogBuilder.setLogTime (0L);
            }
        }

        // status code (HTTP status code)
        value = cloneKeyValues.get ("status_code");
        if (StringUtils.isNotEmpty (value)) {
            try {
                int statusCode = Integer.parseInt (value);
                pixelLogBuilder.setStatusCode (statusCode);
            } catch (NumberFormatException nfe) {
                pixelLogBuilder.setStatusCode (999);
            }
        }

        // user agent
        value = cloneKeyValues.get ("user_agent");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setUserAgent (value);
        }

        // url
        value = cloneKeyValues.get ("url");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setUrl(value);
            merchantUrl = value; // used below
        }

        // orig_ref 
        value = cloneKeyValues.get ("orig_ref");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setRefUrl (value);
        } else {
            value = cloneKeyValues.get ("orig_ref_url");
            if (StringUtils.isNotEmpty (value)) {
                pixelLogBuilder.setRefUrl (value);
            }
        }

        // canonical url
        value = cloneKeyValues.get ("can_url");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setRelCanonicalUrl (value);
        } else {
            value = cloneKeyValues.get ("rc");
            if (StringUtils.isNotEmpty (value)) {
                int rc = NumberUtils.toInt (value, 0);
                if (rc == 1 && StringUtils.isNotEmpty(merchantUrl)) {
                    pixelLogBuilder.setRelCanonicalUrl(merchantUrl);
                }
            }
        }

        // log type
        value = cloneKeyValues.get ("log_type");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setLogType (value);
        }

        // acctId
        value = cloneKeyValues.get ("acct_id");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setAcctId (value);
        }

        // cat
        value = cloneKeyValues.get ("cat");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCat (URLEncoder.encode (value, "utf-8"));
        } else {
            value = cloneKeyValues.get ("category");
            if (StringUtils.isNotEmpty (value)) {
                pixelLogBuilder.setCat (URLEncoder.encode (value, "utf-8"));
            }
        }

        // cat_id is to be set via setParams() method
        // there is no "setCatId" method in pixelLogBuilder
        // value = cloneKeyValues.get ("cat_id");
        // if (StringUtils.isNotEmpty (value)) {
        //     pixelLogBuilder.setCatId (value);
        // }

        // is_paid
        value = cloneKeyValues.get ("is_paid");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setIsPaid (value);
        }

        // prod_id
        value = cloneKeyValues.get ("prod_id");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setProdId (value);
        }

        // prod_name
        value = cloneKeyValues.get ("prod_name");
        if (StringUtils.isNotEmpty (value)) {
            value = StringEscapeUtils.unescapeHtml (value);
            pixelLogBuilder.setProdName (value);
        }

        // order id
        value = cloneKeyValues.get ("order_id");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setOrderId (value);
        }

        // path
        value = cloneKeyValues.get ("path");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setBrWidgetDivPath (value);
        }

        // reg_price
        value = cloneKeyValues.get ("reg_price");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setRegPrice (value);
        }

        // sale_price
        value = cloneKeyValues.get ("sale_price");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setSalePrice (value);
        }

        // currency
        value = cloneKeyValues.get ("currency");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCurrency (value);
        }

        // sale_off
        value = cloneKeyValues.get ("sale_off");
        if (StringUtils.isNotEmpty (value)) {
            int saleOff = 0;
            try {
                saleOff = Integer.parseInt (value);
            } catch (Exception e) {
                saleOff = -1;
            }
            pixelLogBuilder.setSaleOff (saleOff);
        }

        // sale_end
        value = cloneKeyValues.get ("sale_end");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setSaleEnd (value);
        }

        // link
        value = cloneKeyValues.get ("link");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setLink (value);
        }

        // briu
        value = cloneKeyValues.get ("briu");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setBriu (value);
        }

        // A/B testing
        value = cloneKeyValues.get ("ab");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setAbTest (value);
        }

        // query
        value = cloneKeyValues.get ("query");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setQuery (value);
        }

        // session id
        value = cloneKeyValues.get ("session_id");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setSessionId (value);
        }

        // sid
        value = cloneKeyValues.get ("sid");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setRid (value);
        }

        // title
        value = cloneKeyValues.get ("title");
        if (StringUtils.isNotEmpty (value)) {
            value = StringEscapeUtils.unescapeHtml (value);
            pixelLogBuilder.setTitle (value);
        }

        // cookie
        value = cloneKeyValues.get ("cookie");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCookie (value);
        }

        // cookie2
        value = cloneKeyValues.get ("oc2");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCookie2 (value);
        } else {
            value = cloneKeyValues.get ("cookie2");
            if (StringUtils.isNotEmpty (value)) {
                pixelLogBuilder.setCookie2 (value);
            }
        }
 
        // uId (parsed from cookie)
        value = cloneKeyValues.get ("uid");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setUid (value);
        }

        // tpc
        value = cloneKeyValues.get ("br_tpc");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCookieTpc (value);
        }

        // time
        value = cloneKeyValues.get ("time");
        if (StringUtils.isNotEmpty (value)) {
            try {
                pixelLogBuilder.setTime (Integer.parseInt (value));
            } catch (NumberFormatException nfe) {
            }
        }

        // ptype
        value = cloneKeyValues.get ("ptype");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setPtype (value);
        }

        // isConversion: 0 or 1
        value = cloneKeyValues.get ("is_conversion");
        if (StringUtils.isNotEmpty (value)) {
            try {
                pixelLogBuilder.setIsConversion (Integer.parseInt (value));
            } catch (NumberFormatException nfe) {
            }
        }

        // basket_value (only when is_conversion = 1)
        value = cloneKeyValues.get ("basket_value");
        if (StringUtils.isNotEmpty (value)) {
            double basketValue;

            try {
                basketValue = Double.parseDouble (value);
            } catch (NumberFormatException nfe) {
                basketValue = 0.0;
            }
            pixelLogBuilder.setBasketValue (basketValue);
        }

        // domain
        value = cloneKeyValues.get ("domain");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setDomain (value);
        }

        // ref_url
        value = cloneKeyValues.get ("ref_url");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setRefUrl (value);
        }

        // traffic source
        value = cloneKeyValues.get ("traffic_source");
        if (StringUtils.isNotEmpty (value)) {
            TrafficSource ts;

            ts = ReferrerTypeParser.parseTrafficSource (pixelLogBuilder.getRefUrl (), pixelLogBuilder.getDomain ()); 
            pixelLogBuilder.setTrafficSource (ts);
        }

        // ref_query ???

        // Universal pixel parameters
        value = cloneKeyValues.get ("page_type");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setUniPageType (value);
        }

        // page_labels
        value = cloneKeyValues.get ("page_labels");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setPageLabels (value);
        }

        // event type
        value = cloneKeyValues.get ("event_type");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setEventType (value);
        }

        // event action
        value = cloneKeyValues.get ("event_action");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setEventAction (value);
        }

        // event labels
        value = cloneKeyValues.get ("event_labels");
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setEventLabels (value);
        }

        // event value
        value = cloneKeyValues.get ("event_value");
        if (StringUtils.isNotEmpty (value)) {
            try {
                double eventValue = Double.parseDouble (value);
                pixelLogBuilder.setEventValue (eventValue);
            } catch (Exception e) {
            }
        }

        // default log_type
        // if logType has not been set, set it by default to 'pageview'
        value = cloneKeyValues.get ("log_type");
        if (StringUtils.isEmpty (value)) {
            pixelLogBuilder.setLogType ("pageview");
        }

        // content search related fields
        value = cloneKeyValues.get (CommonFields.ITEM_ID);
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setItemId (value);
        }

        // CATALOGS
        value = cloneKeyValues.get (CommonFields.CATALOGS);
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCatalogs (PixelCatalog.parse (value));
        }

        // VERSION
        value = cloneKeyValues.get (CommonFields.VERSION);
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setVersion (value);
        }

        // for Segments - COUNTRY
        value = cloneKeyValues.get (CommonFields.CUSTOMER_COUNTRY);
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCustomerCountry (StringUtils.lowerCase (value));
        }

        // USER_ID
        value = cloneKeyValues.get (CommonFields.USER_ID);
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setUserId (StringUtils.lowerCase (value));
        }

        // CUSTOMER_TIER
        value = cloneKeyValues.get (CommonFields.CUSTOMER_TIER);
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCustomerTier (StringUtils.lowerCase (value));
        }

        // CUSTOMER_PROFILE
        value = cloneKeyValues.get (CommonFields.CUSTOMER_PROFILE);
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCustomerProfile (StringUtils.lowerCase (value));
        }

        // CUSTOMER GEO
        value = cloneKeyValues.get (CommonFields.CUSTOMER_GEO);
        if (StringUtils.isNotEmpty (value)) {
            pixelLogBuilder.setCustomerGeo (StringUtils.lowerCase (value));
        }

        // basket - available only for conversion pixel
        // Basket 'items' have been parsed into 'pixelBasketBuilder' object earlier
        if (pixelBasketBuilder != null) { 
            pixelLogBuilder.setBasket (pixelBasketBuilder);
        }


        // "dynamic" (aka all-other) parameters
        for (String paramKey : cloneParams.keySet ()) {
            String paramValue;

            paramValue = cloneParams.get (paramKey);
            addPixelLogParam (pixelLogBuilder, paramKey, paramValue);
        }

        return pixelLogBuilder;
    }

    /**
     * Add PixelLogParam corresponding to the passed parameter key-value pair to the pixel builder.
     * @param pixelBuilder
     * @param paramKey
     * @param paramValue
     */
    private void addPixelLogParam (PixelLog.Builder pixelBuilder, String paramKey, String paramValue) {
        if (paramKey.equals("prod_name") || paramKey.equals("title")) {
            paramValue = StringEscapeUtils.unescapeHtml(paramValue);
        }

        PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
        paramBuilder.setKey(paramKey);
        paramBuilder.setValue(paramValue);
        pixelBuilder.addParams(paramBuilder.build());
    }
}

