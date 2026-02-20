package com.bloomreach.brxdemos.pacifichome.translate.pixel.feed;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.transform.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Step 1 - Parse Category TSV to category map
 * Step 2 - Parse ProductFeed.xml to keyword-map  + merge catIds
 * Step 3 - Generate output xml using merged map
 */

public class ProcessedFeed {

    private final static String KEY_NAME_PID = "pid";
    private final static String KEY_NAME_PRICE = "price";
    private final static String KEY_NAME_TITLE = "title";
    private final static String KEY_NAME_BREADCRUMB = "bread_crumb";
    private final static String KEY_NAME_BREADCRUMB_ID = "bread_crumb_id";
    private final static String PRODUCT_NODE_NAME = "product";

    private ArrayList<FeedRecord> parsedFeedRecordList;

    // catId -> { FeedCrumbData }
    // Used to validate if catId in sourcePixel is infact in the translatedFeed
    private Hashtable <String, FeedCrumbData> parsedCatIdToCrumbData; 

    // leaf -> { FeedCrumbData }-- needed to resolve refUrl (for which we don't have a matching category pixel)
    private Hashtable <String, FeedCrumbData> parsedCrumbLeafToCrumbData; 
 
    public void load (String productFileName) throws Exception
    {
        System.err.println(" > Start Parsing source file...." + productFileName);
        parseProductXML(productFileName);
        System.err.println(" ............................... Done Parsing Product File.");

        System.out.println (" > Build list of all available crumbs ");
        postProcessBreadCrumbs ();
        System.out.println ("Total crumbs in feed: " + parsedCrumbLeafToCrumbData.size()); 
        System.out.println (" ..................... Done buildng list of available crumbs ");

    }

    public boolean isProductInFeed (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (true); 
            }
        }

        return (false);
    }

    public String lookupProductPrice (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (aRecord.getProductPrice()); 
            }
        }

        return (null);
    }

    public String lookupProductName (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (aRecord.getProductName ()); 
            }
        }

        return (null);
    }

    // given crumbId, return its fullcrumb-in-feed
    public FeedCrumbData getCrumbDataInFeedForId (String crumbId) {
        FeedCrumbData crumbData;

        crumbData = parsedCatIdToCrumbData.get (crumbId); 
        return (crumbData);   // if exists, the actual crumbData in feed
    }

    // given leaf-crumb (lower-case), return [full-crumb-in-feed, leafCrumbId]
    // IE, given "C", return ["A>B>C", "30"]
    public FeedCrumbData getCrumbDataInFeedForLeaf (String crumbLeaf) {
        FeedCrumbData crumbData;

        crumbData = parsedCrumbLeafToCrumbData.get (crumbLeaf.toLowerCase());
        return (crumbData); // may be null if crumb not in feed
    }

    /**
     * 
     * returns FeedRecord-list
     */
    private ArrayList<FeedRecord> parseProductXML (String srcFileName) throws Exception {
        Document srcDocument = null;
        NodeList nList = null; // list of ALL <product> nodes in src XML
        parsedFeedRecordList = new ArrayList <FeedRecord> ();

        try {
            DocumentBuilder srcDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder ();
            srcDocument = srcDocBuilder.parse(new File(srcFileName));
            srcDocument.getDocumentElement().normalize();
        }
        catch (Exception e) {
            System.err.println("Could not construct documentBuilder" + e.getMessage());
            return null;
        }

        try {
            // list of ALL <product> nodes in src XML
            nList = srcDocument.getElementsByTagName(PRODUCT_NODE_NAME);
            // System.err.println ("Product node list length: " + nList.getLength ());
        }
        catch (Exception e) {
            System.err.println("Could not collect product nodes" + e.getMessage());
            return null;
        }

        for (int count = 0; count < nList.getLength(); count++) {
            NodeList childNodes;

            Node aProductNode = nList.item (count);
            // System.out.println ("DEBUG node name: " + aProductNode.getNodeName ());
            try {
                FeedRecord parsedFeedRecord;

                // translate srcXML node to outXML map
                parsedFeedRecord = parseOneProductNode (aProductNode);
                parsedFeedRecordList.add (parsedFeedRecord);
            } catch (Exception e) {
                System.err.println ("Exception in parsing product node: " + aProductNode.getNodeName ());
            }
        }

        return (parsedFeedRecordList);
    }

    private FeedRecord parseOneProductNode (Node aProductNode) throws Exception {
        NodeList childNodes;
        String thisPid;
        FeedRecord parsedFeedRecord;

        thisPid = null; // null until we see a value in source product node
        parsedFeedRecord = new FeedRecord ();

        childNodes = aProductNode.getChildNodes ();
        // System.out.println ("DEBUG child count = " + childNodes.getLength ());
        for (int childCount = 0; childCount < childNodes.getLength (); childCount++) {
            Node oneChildNode;

            oneChildNode = childNodes.item (childCount); // this 
            if (oneChildNode.getNodeType () == Node.ELEMENT_NODE) { 
                Element childElem;
                String childValue;
                String childName;

                childElem = (Element) oneChildNode;
                childName = oneChildNode.getNodeName ();
                childValue = childElem.getTextContent ();

                if  ((childValue == null) || (childValue.length () == 0)) {
                    continue;
                } 

                // now process specific keys in this product node
                if (childName.equalsIgnoreCase (KEY_NAME_PID) == true) {
                    thisPid = childValue;   // needed later
                    parsedFeedRecord.setProductId (thisPid);
                } else if (childName.equalsIgnoreCase (KEY_NAME_PRICE) == true) {
                    parsedFeedRecord.setProductPrice (childValue);
                } else if (childName.equalsIgnoreCase (KEY_NAME_TITLE) == true) {
                    parsedFeedRecord.setProductName (childValue);
                } else if (childName.equalsIgnoreCase (KEY_NAME_BREADCRUMB) == true) {
                    // this is 'full' breadcrumb (ie multiple branches included)
                    // bread_crumb: A>B>C|X>Y. All strings saved 'as-is' (ie, no simplication, decode,...)
                    parsedFeedRecord.setBreadCrumb (childValue);
                } else if (childName.equalsIgnoreCase (KEY_NAME_BREADCRUMB_ID) == true) {
                    // bread_crumb_id: 10>20>30|50>60
                    parsedFeedRecord.setBreadCrumbId (childValue);
                }
            }
        }

        return (parsedFeedRecord);
    }

    private void postProcessBreadCrumbs () throws Exception {
        parsedCatIdToCrumbData  = new Hashtable <String, FeedCrumbData> ();
        parsedCrumbLeafToCrumbData = new Hashtable <String, FeedCrumbData> (); 

        for (FeedRecord aRecord : parsedFeedRecordList) {
            String breadCrumb;
            String breadCrumbId;
            String [] breadCrumbList;
            String [] breadCrumbIdList;

            breadCrumb = aRecord.getBreadCrumb ();
            breadCrumbId = aRecord.getBreadCrumbId ();

            // split each, delimiter = '|'
            breadCrumbList = breadCrumb.split ("\\|");
            breadCrumbIdList = breadCrumbId.split ("\\|");
            if (breadCrumbList.length != breadCrumbIdList.length) {
                System.out.println ("Crumb and crumbId mismatch, pid = " + aRecord.getProductId ());
                continue;
            }

            for (int i = 0; i < breadCrumbIdList.length; i++) {
                String[] crumbElements;
                String[] crumbIdElements;
                String fullCrumb = null;

                crumbElements = breadCrumbList [i].split (">");
                crumbIdElements = breadCrumbIdList [i].split (">");
                for (int j = 0; j < crumbIdElements.length; j++) {
                    String key;
                    String leafCrumb;
                    String leafCrumbId;
                    FeedCrumbData crumbData;

                    // NOTE: In the source feed, blanks in leaf-crumb are not encoded;
                    // IE, for leaf = "cutting boards", <blank> is preserved
                    leafCrumb = crumbElements [j];
                    leafCrumbId = crumbIdElements [j];

                    if (fullCrumb == null)
                        fullCrumb = leafCrumb;
                    else
                        fullCrumb = fullCrumb + ">" + leafCrumb;

                    // This is needed to attempt-to-resolve a refUrl for which we have not 
                    // seen corresponding category pixel
                    // A -> A , 10
                    // B -> A>B , 20
                    // C -> A>B>C , 30
                    key = leafCrumb.toLowerCase (); // "A" or "B" or "C", lowerCase
                    crumbData = new FeedCrumbData (fullCrumb, leafCrumb, leafCrumbId);
                    parsedCrumbLeafToCrumbData.putIfAbsent (leafCrumb, crumbData);  //key == leafCrumb

                    // for crumb = A>B>C, crumbId = 10>20>30, enter
                    //  10: A
                    //  20: A>B, B
                    //  30: A>B>C, C
                    parsedCatIdToCrumbData.putIfAbsent (leafCrumbId, crumbData); // key == crumbId
                }
            }
        }
    }

    class FeedRecord {

        String productId = "";
        String productName = "";
        String productPrice = "";
        String breadCrumb = ""; // A>B>C
        String breadCrumbId = ""; // 10>20>30

        FeedRecord () {
        }

        public void setProductId (String pid) {
            this.productId = pid;
        }

        public String getProductId () {
            return this.productId;
        }

        public void setProductName (String name) {
            this.productName = name;
        }

        public String getProductName () {
            return this.productName;
        }

        public void setProductPrice (String price) {
            this.productPrice = price;
        }

        public String getProductPrice () {
            return this.productPrice;
        }

        public void setBreadCrumb (String breadCrumb) {
            this.breadCrumb = breadCrumb;
        }

        public String getBreadCrumb () {
            return (this.breadCrumb);
        }

        public void setBreadCrumbId (String breadCrumbId) {
            this.breadCrumbId = breadCrumbId;
        }

        public String getBreadCrumbId () {
            return (this.breadCrumbId);
        }
    }

}
/*******
// 
//     private final static String SOURCE_COMPANY_NAME = "worldmarket.com";
//     private final static String REPLACED_COMPANY_NAME = "pacifichome";
//     private final static String RESERVED_TEXT_1 = "World Market";
//     private final static String REPLACED_RESERVED_TEXT_1 = "PacificHome";
// 
//     private final static String PRODUCT_NODE_NAME = "product";
//     private final static String OUTPUT_PRODUCTS_NODE_NAME = "products";
//     private final static String KEY_VALUE_DELIMITER = "!!"; // delimiter used in key-value pairs from product.xml   
//     // private final static String DEFAULT_URL_PREFIX = "https://cdn.brcdn.com/homeoasis.bloomreach.com_products/";
//     // private final static String DEFAULT_URL_POSTFIX = ".html";
//     private final static String DEFAULT_URL_PREFIX = "https://pacifichome.bloomreach.com/products/";
//     private final static String DEFAULT_URL_POSTFIX = "";
// 
//     //private final static String DEFAULT_IMAGE_URL_PREFIX= "https://cdn.brcdn.com/homeoasis.bloomreach.com_products/";
//     private final static String DEFAULT_IMAGE_URL_PREFIX= "https://pacific-demo-data.bloomreach.cloud/home/images/";
//     private final static String DEFAULT_IMAGE_URL_POSTFIX = "_XXX_v1.tif";
// 
//     private final static String KEY_NAME_BREADCRUMB = "bread_crumb"; // used in current source xml
//     private final static String KEY_NAME_GOOG_CATEGORY = "google_category"; // to be skipped if exists in src
//     private final static String KEY_NAME_LEAF_CATEGORIES = "leaf_categories"; // to be skipped if exists in src
//     private final static String KEY_NAME_PID = "pid";
//     private final static String KEY_NAME_URL = "url";
//     private final static String KEY_NAME_THUMB_IMAGE = "thumb_image";
//     private final static String KEY_NAME_LARGE_IMAGE = "large_image";
//     private final static String KEY_NAME_PRICE = "price";
//     private final static String KEY_NAME_SALE_PRICE = "sale_price";
//     private final static String KEY_NAME_SKU_THUMB_IMAGE = "sku_thumb_image";
//     private final static String KEY_NAME_SKU_LARGE_IMAGE = "sku_large_image";
//     private final static String KEY_NAME_TITLE = "title";
// 
//     private final static String BREADCRUMB_VALUE_DELIMITER = ">";   // as currently used in source xml
//     private final static String BREADCRUMB_PARENTVALUE_DELIMITER_IN = "|"; // as currently used in source xml
//     private final static String BREADCRUMB_PARENTVALUE_DELIMITER_OUT = "|"; // used in generated output
//     private final static String CRUMBID_DELIMITER_IN_CATEGORYINFO = "/"; // used in category.tsv
// 
//     private final static int    DEFAULT_BREADCRUMBID = 123;
//  
//     private final static String REGEX_REGISTERED_TM = "\\u00AE"; // "Home(regTM)" --> Home
//     private final static Pattern  PATTERN_RTM = Pattern.compile (REGEX_REGISTERED_TM);
// 
//     private static int NEXT_UNDEFINED_CRUMB_ID = DEFAULT_BREADCRUMBID;
// 
    // catId -> feedCrumb  -- needed to resolve category pixel urls

    private Hashtable <String, String> parsedCatIdToCrumbData; 

********/

