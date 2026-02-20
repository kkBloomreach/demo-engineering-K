package com.bloomreach.brxdemos.pacifichome.translate.feed;
// Version 1.1.1.0
// 'stock_level' is a 'hidden' attribute in Connect to decide whether a product/sku
// is available. It is used even if it is not mapped in devStudio specifically
// Therefore, updated this code to make remove 'stock_level' attribute in output
// Version 1.1.0.0
//  Updated algo to match crumb -> crumbId map. Now uses recursive methods to do so
//  Changed product url format to use 'pacifichome.bloomreach.com' pattern
// Version: 1.0.6.0
//  fixed: Corrected logic to set Undefined crumbId value
//  changed: default UndefinedCrumbId from 99999 to 123
//  changed: crumb 'parent value' delimiter was ',', changed to '|'
// Version: 1.0.5.0
//  fixed: changed Image URLs to refer to pacific-demo-data.bloomreach.cloud/home/images
// Version: 1.0.4.0
//  fixed: removed reg-trademark, trademark, copyright
//  fixed: replaced special tokens 
// Version: 1.0.3.0
//  fixed: removed duplicate thumb_image and large_image tags

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Random;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class FeedTranslator {

    private final static String SOURCE_FEED_FILE_NAME = "./source/products.xml";
    private final static String CATEGORIES_FILE_NAME = "./source/categories.tsv";
    private final static String OUTPUT_FEED_FILE_NAME = "./output/output.xml";

    private final static String SOURCE_COMPANY_NAME = "worldmarket.com";
    private final static String REPLACED_COMPANY_NAME = "pacifichome";
    private final static String RESERVED_TEXT_1 = "World Market";
    private final static String REPLACED_RESERVED_TEXT_1 = "PacificHome";

    private final static String PRODUCT_NODE_NAME = "product";
    private final static String PRODUCT_SKU_FIELD_NAME = "skuid";
    private final static String OUTPUT_ROOT_NODE_NAME = "feed";
    private final static String OUTPUT_PRODUCTS_NODE_NAME = "products";

    private final static String KEY_VALUE_DELIMITER = "!!"; // delimiter used in key-value pairs from product.xml   
    // private final static String DEFAULT_URL_PREFIX = "https://cdn.brcdn.com/homeoasis.bloomreach.com_products/";
    // private final static String DEFAULT_URL_POSTFIX = ".html";
    private final static String DEFAULT_URL_PREFIX = "https://pacifichome.bloomreach.com/products/";
    private final static String DEFAULT_URL_POSTFIX = "";

    //private final static String DEFAULT_IMAGE_URL_PREFIX= "https://cdn.brcdn.com/homeoasis.bloomreach.com_products/";
    private final static String DEFAULT_IMAGE_URL_PREFIX= "https://pacific-demo-data.bloomreach.cloud/home/images/";
    private final static String DEFAULT_IMAGE_URL_POSTFIX = "_XXX_v1.tif";

    private final static String KEY_NAME_BREADCRUMB = "bread_crumb"; // used in current source xml
    private final static String KEY_NAME_BREADCRUMB_ID = "bread_crumb_id"; // used in current output xml
    private final static String KEY_NAME_GOOG_CATEGORY = "google_category"; // to be skipped if exists in src
    private final static String KEY_NAME_LEAF_CATEGORIES = "leaf_categories"; // to be skipped if exists in src
    private final static String KEY_NAME_PID = "pid";
    private final static String KEY_NAME_URL = "url";
    private final static String KEY_NAME_THUMB_IMAGE = "thumb_image";
    private final static String KEY_NAME_LARGE_IMAGE = "large_image";
    private final static String KEY_NAME_PRICE = "price";
    private final static String KEY_NAME_SALE_PRICE = "sale_price";
    private final static String KEY_NAME_SKU_THUMB_IMAGE = "sku_thumb_image";
    private final static String KEY_NAME_SKU_LARGE_IMAGE = "sku_large_image";
    private final static String KEY_NAME_TITLE = "title";
    private final static String KEY_NAME_STOCK_LEVEL = "stock_level";

    private final static String BREADCRUMB_VALUE_DELIMITER = ">";   // as currently used in source xml
    private final static String BREADCRUMB_PARENTVALUE_DELIMITER_IN = "|"; // as currently used in source xml
    private final static String BREADCRUMB_PARENTVALUE_DELIMITER_OUT = "|"; // used in generated output
    private final static String CRUMBID_DELIMITER_IN_CATEGORYINFO = "/"; // used in category.tsv

    private final static int    DEFAULT_BREADCRUMBID = 123;
 
    private final static String REGEX_REGISTERED_TM = "\\u00AE"; // "Home(regTM)" --> Home
    private final static Pattern  PATTERN_RTM = Pattern.compile (REGEX_REGISTERED_TM);

    private static int NEXT_UNDEFINED_CRUMB_ID = DEFAULT_BREADCRUMBID;

    // Param: source filename 
    public static void main(String[] args) {

        if ((args.length < 1)) {
            System.err.println ("Usage: FeedTranslator <root data dirpath>");
            return;
        }
 
        FeedTranslator feedTranslator = new FeedTranslator ();
        try {
            feedTranslator.doTranslate (args [0]);
        }
        catch (Exception e) {
            System.err.println ("Exception in processing XML source: " + e.getMessage ());
        }
    }


    private void doTranslate (String rootDirPath) throws Exception
    {
        File catFile;
        String catFilePath;
        File srcFeedFile;
        String srcFeedFilePath;

        HashMap<String, CategoryInfo> hCategoryMap = null;
        DocumentBuilder outDocBuilder = null;
        Document outDocument = null;
        Node outProductsNode;
 
        System.err.println(" > Start Converting Category TSV ....");
        catFile = new File (rootDirPath, CATEGORIES_FILE_NAME);
        catFilePath = catFile.getPath ();
        hCategoryMap = convertCategoryTSV (catFilePath);
        System.err.println(" ............................... Done Converting Taxonomy.");

        // prepare output xml node tree
        try { 
            Element outRootNode;

            outDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder ();
            outDocument = outDocBuilder.newDocument ();
            outRootNode = outDocument.createElement (OUTPUT_ROOT_NODE_NAME);
            outDocument.appendChild (outRootNode);
            
            // also add "products" node to root (feed) node
            outProductsNode = outDocument.createElement (OUTPUT_PRODUCTS_NODE_NAME);
            outRootNode.appendChild (outProductsNode);

        } catch (Exception e) {
            System.err.println ("Exception in creating output xml document");
            return;
        }

        srcFeedFile = new File (rootDirPath, SOURCE_FEED_FILE_NAME);
        srcFeedFilePath = srcFeedFile.getPath ();
        System.err.println(" > Start Converting source file...." + srcFeedFilePath);
        convertProductXML(srcFeedFilePath, hCategoryMap, outProductsNode, outDocument);
        System.err.println(" ............................... Done Converting Product File.");

        // finally write ("transform") root node to output file
        try {
                File outFile = new File (rootDirPath, OUTPUT_FEED_FILE_NAME);
                Result outResult = new StreamResult (outFile);
    
                Transformer outTransformer = TransformerFactory.newInstance ().newTransformer ();
                outTransformer.setOutputProperty (OutputKeys.INDENT, "yes");
                outTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                Source outSource = new DOMSource (outDocument);
                outTransformer.transform (outSource, outResult);

        } catch (Exception e) {
            System.err.println ("Exception in writing XML to output file: " + e.getMessage ());
        }
    
    }

    /**
     * 
     * returns 
     */
    private void convertProductXML (String srcFilePath, HashMap<String, CategoryInfo> categoryMap, Node outProductsNode, Document outDocument) throws Exception {

        Document srcDocument = null;
        NodeList nList = null; // list of ALL <product> nodes in src XML
        XPath xpath = null;

        try {
                DocumentBuilder srcDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder ();
                srcDocument = srcDocBuilder.parse(new File(srcFilePath));
                srcDocument.getDocumentElement().normalize();
        }
        catch (Exception e) {
            System.err.println("Could not construct documentBuilder" + e.getMessage());
            return;
        }

        try {
            // list of ALL <product> nodes in src XML
            nList = srcDocument.getElementsByTagName(PRODUCT_NODE_NAME);
            // System.err.println ("Product node list length: " + nList.getLength ());
        }
        catch (Exception e) {
            System.err.println("Could not collect product nodes" + e.getMessage());
            return;
        }

        // set up a xpath object once. It is needed to lookup duplicate SKUs
        xpath = XPathFactory.newInstance().newXPath ();

        // go thru all <product> nodes in the srcFile
        for (int count = 0; count < nList.getLength(); count++) {
            NodeList childNodes;

            Node aProductSkuNode = nList.item (count);
            // System.out.println ("DEBUG node name: " + aProductSkuNode.getNodeName ());

            try {
                ArrayList <String> hProductSkuKVMap;

                // translate srcXML node to outXML map
                hProductSkuKVMap = transferOneProductSkuNode (aProductSkuNode, categoryMap);

                // some SKUs are duplicate in this feed. If so, don't include it in the output
                if (isSkuDuplicate (xpath, outProductsNode, hProductSkuKVMap) == false) {
                    // generate XML node "product" for translated map and append to 'products' node
                    appendToProductsNode (hProductSkuKVMap, outProductsNode, outDocument);
                } 
            } catch (Exception e) {
                System.err.println ("Exception in translation of product node: " + aProductSkuNode.getNodeName ());
            }
        }
    }

    // Important node. For this feed, the "product node" is actually a SKU-node. Same product
    // with multiple skus have separate multiple 'nodes'
    private ArrayList<String> transferOneProductSkuNode (Node aProductSkuNode, HashMap<String, CategoryInfo> hCategoryMap) throws Exception {
        NodeList childNodes;
        ArrayList <String> hProductSkuKVList = new ArrayList <String>();
        Random priceRandomizer;
        String thisPid;
        DecimalFormat priceFormatter;
        Matcher  matcherRTM = null;

        priceRandomizer = new Random (); // used to calculate anonymized price from source price
        priceFormatter = new DecimalFormat ("0.00"); // max two decimals

        childNodes = aProductSkuNode.getChildNodes ();
        thisPid = null; // null until we see a value in source product node

        // System.out.println ("DEBUG child count = " + childNodes.getLength ());
        for (int childCount = 0; childCount < childNodes.getLength (); childCount++) {
            Node oneChildNode;

            oneChildNode = childNodes.item (childCount); // this 
            if (oneChildNode.getNodeType () == Node.ELEMENT_NODE) { 
                Element childElem;
                String childValue;
                String childName;
                String keyValue; // childName + delimiter + childValue

                childElem = (Element) oneChildNode;
                childName = oneChildNode.getNodeName ();
                childValue = childElem.getTextContent ();

                if  ((childValue == null) || (childValue.length () == 0)) {
                    childValue = "";
                    // System.out.println ("DEBUG child value: <blank>");
                    keyValue = childName + KEY_VALUE_DELIMITER + childValue;
                    hProductSkuKVList.add (keyValue);
                    continue;
                } else {
                    // handle 'special'/'reserved'/ characters/tokens
                    if (childValue.indexOf (SOURCE_COMPANY_NAME) >= 0) {
                        childValue = childValue.replaceAll (SOURCE_COMPANY_NAME, REPLACED_COMPANY_NAME);
                    } 
                    if (childValue.indexOf (RESERVED_TEXT_1) >= 0) {
                        childValue = childValue.replaceAll (RESERVED_TEXT_1, REPLACED_RESERVED_TEXT_1);
                    }

                }

                // now process specific keys in this product node
                if (childName.equalsIgnoreCase (KEY_NAME_PID) == true) {
                    thisPid = childValue;   // needed later
                    // System.out.println ("DEBUG: pid = " + thisPid);
                    keyValue = KEY_NAME_PID + KEY_VALUE_DELIMITER + childValue;
                    hProductSkuKVList.add (keyValue);
                } else if (childName.equalsIgnoreCase (KEY_NAME_BREADCRUMB) == true) {
                        String bread_crumbId;

                        // if key == bread_crumb, generate bread_crumbid
                        // childValue is of the form "a>b>c|x>y
                        // this method removes blanks in bread_crumb
                        // also generates bread_crumbid
                        // and then adds both into the keywordMap (arrayList)
                        processBreadCrumb (childValue, hCategoryMap, hProductSkuKVList);
                } else if (childName.equalsIgnoreCase (KEY_NAME_GOOG_CATEGORY) == true) {
                    continue; // skip 'google_category' element - causes problem with BR's feed processor
                } else if (childName.equalsIgnoreCase (KEY_NAME_LEAF_CATEGORIES) == true) {
                    continue; // skip 'google_category' element - causes problem with BR's feed processor
                } else if (childName.equalsIgnoreCase (KEY_NAME_URL) == true) {
                    String anonymUrl;

                    if (thisPid == null)
                        thisPid = "pid?";  // this should not happen; we should have seen 'pid' already
                    anonymUrl = DEFAULT_URL_PREFIX + thisPid + DEFAULT_URL_POSTFIX;
                    keyValue = KEY_NAME_URL + KEY_VALUE_DELIMITER + anonymUrl;
                    hProductSkuKVList.add (keyValue);
                } else if ((childName.equalsIgnoreCase (KEY_NAME_THUMB_IMAGE) == true) ||
                         (childName.equalsIgnoreCase (KEY_NAME_LARGE_IMAGE) == true)) {
                    String anonymUrl;

                    if (thisPid == null)
                        thisPid = "pid?";  // this should not happen; we should have seen 'pid' already
                    anonymUrl = DEFAULT_IMAGE_URL_PREFIX + thisPid + DEFAULT_IMAGE_URL_POSTFIX;

                    // same image url for both thumb and large image
                    if (childName.equalsIgnoreCase (KEY_NAME_THUMB_IMAGE) == true) {
                        keyValue = KEY_NAME_THUMB_IMAGE + KEY_VALUE_DELIMITER + anonymUrl;
                        hProductSkuKVList.add (keyValue);
                    } else {
                        keyValue = KEY_NAME_LARGE_IMAGE + KEY_VALUE_DELIMITER + anonymUrl;
                        hProductSkuKVList.add (keyValue);
                    }
                }
                else if ((childName.equalsIgnoreCase (KEY_NAME_SKU_THUMB_IMAGE) == true) ||
                         (childName.equalsIgnoreCase (KEY_NAME_SKU_LARGE_IMAGE) == true)) {
                    String anonymUrl;

                    // Note that only some pid's have multiple skus. 
                    if (thisPid == null)
                        thisPid = "pid?";  // this should not happen; we should have seen 'pid' already
                    anonymUrl = DEFAULT_IMAGE_URL_PREFIX + thisPid + DEFAULT_IMAGE_URL_POSTFIX;

                    // same image url for both thumb and large image
                    if (childName.equalsIgnoreCase (KEY_NAME_SKU_THUMB_IMAGE) == true) {
                        keyValue = KEY_NAME_SKU_THUMB_IMAGE + KEY_VALUE_DELIMITER + anonymUrl;
                        hProductSkuKVList.add (keyValue);
                    } else {
                        keyValue = KEY_NAME_SKU_LARGE_IMAGE + KEY_VALUE_DELIMITER + anonymUrl;
                        hProductSkuKVList.add (keyValue);
                    }
                } else if (childName.equalsIgnoreCase (KEY_NAME_PRICE) == true) {
                    float priceValue;
                    float salePriceValue;
                    float randomFactor;

                    // calculate random price in range { +/- 0.5} of the original price
                    randomFactor = priceRandomizer.nextFloat ();
                    if (randomFactor < 0.5)
                        randomFactor = 1.0F + randomFactor;
                    priceValue = Float.valueOf (childValue) * randomFactor;
 
                    // add "price", max 2 decimal places
                    keyValue = KEY_NAME_PRICE + KEY_VALUE_DELIMITER + priceFormatter.format (priceValue);
                    hProductSkuKVList.add (keyValue);

                    // add "sale_price", max 2 decimal places
                    randomFactor = priceRandomizer.nextFloat ();
                    if (randomFactor < 0.65)
                        randomFactor = 0.65F;
                    salePriceValue = priceValue * randomFactor;
                    keyValue = KEY_NAME_SALE_PRICE + KEY_VALUE_DELIMITER + priceFormatter.format (salePriceValue);
                    hProductSkuKVList.add (keyValue);
                } else if (childName.equalsIgnoreCase (KEY_NAME_SALE_PRICE) == true) {
                    continue; // anonymized sale price is calculated and added along with price
                } else if (childName.equalsIgnoreCase (KEY_NAME_TITLE) == true) {
                    // use regEx matcher to replace "Home{RTM}" (PacificHome is not a registered TM)
                    // In the feed, the reg-tm sign is sometimes specified as unicode-value
                    // and some other time as "&reg;". Therefore need to check both
                    matcherRTM = PATTERN_RTM.matcher (childValue);
                    childValue = matcherRTM.replaceAll ("");
                    childValue = childValue.replaceAll ("&reg;", "");
                    keyValue = childName + KEY_VALUE_DELIMITER + childValue;
                    hProductSkuKVList.add (keyValue);
                } else if (childName.equalsIgnoreCase (KEY_NAME_STOCK_LEVEL) == true) {
                    // skip this attribute (ie, don't include in output)
                } else { // none-of-the-above 'special' cases
                    // prepare a key-value string and append to arrayList
                    keyValue = childName + KEY_VALUE_DELIMITER + childValue;
                    hProductSkuKVList.add (keyValue);
                }
            }
         }

        // add specific fields needed by PacificHome but not in original feed
        // NONE at this time

        return (hProductSkuKVList);
    }

    // Incoming bread_crumb is of the form: a>b>c|x>y>z...
    private void processBreadCrumb (String bread_crumb, HashMap<String, CategoryInfo> hCategoryMap, ArrayList<String> hProductKVList) {
        String[] crumbsList;
        StringBuffer crumbsBuf; // buffer for 'crumbs'
        StringBuffer crumbIdBuf; // buffer for 'crumbs_id'

        crumbsBuf = new StringBuffer (); // buffer to hold crumbs string
        crumbIdBuf = new StringBuffer (); // buffer to hold generated crumbId string
        // crumbsList = bread_crumb.split (BREADCRUMB_PARENTVALUE_DELIMITER_IN);
        crumbsList = bread_crumb.split ("\\|"); // String.split regex requires pipeline char to be escaped

        // Each element in crumbList is a crumb-path, like: "X>Y>Z"
        for (int i = 0; i < crumbsList.length; i++) {
            String fullCrumb;
            String fullCrumbId;
            CategoryInfo catInfo;

            fullCrumb = crumbsList [i]; // "X>Y>Z"

            // check to see if this crumb-path has matching fullcrumbId obtained from category.tsv file
            // Note: products file contains crumbs with a 'space' character around the delimiter
            // EG, "X(blank)>(blank)Y(blank)>(blank)>...
            // Internally the categoryMap uses a key without these blank spaces. Therefore
            // remove the blanks surrounding ">"
            fullCrumb = fullCrumb.replaceAll (" > ", ">");

            // System.out.println ("DEBUG looking catinfo for: " + fullCrumb);

            catInfo = hCategoryMap.get (fullCrumb);
            if (catInfo == null) {
                // a crumb in product file does not have corresponding crumb-id info in category.tsv
                // Generate uniq numbers and store those in categoryMap for future reference
                catInfo = generateCategoryInfo (fullCrumb, hCategoryMap);
                // System.out.println ("\tDEBUG generated catinfo for: " + fullCrumb + ", fullCrumbId = " + catInfo.getFullCrumbId ());
            }
            // else
            // System.out.println ("\tDEBUG found catinfo for: " + fullCrumb + ", fullCrumbId = " + catInfo.getFullCrumbId ());

            fullCrumbId = catInfo.getFullCrumbId (); // eg, 111/222/333
            // replace the "/" to ">" since the crumb uses ">" as delimiter in product file
            // and to be consistent, use the same delimiter for crumbId as well
            fullCrumbId = fullCrumbId.replace (CRUMBID_DELIMITER_IN_CATEGORYINFO, BREADCRUMB_VALUE_DELIMITER);

            if (crumbsBuf.length () > 0) {
                crumbsBuf.append (BREADCRUMB_PARENTVALUE_DELIMITER_OUT);
                crumbIdBuf.append (BREADCRUMB_PARENTVALUE_DELIMITER_OUT);
            } 
            crumbsBuf.append (fullCrumb);   // 'crumb' name
            crumbIdBuf.append (fullCrumbId); // 'crumbs_id'
        }

        // generate strings from each string buffer and add to product key list
        {
            String keyValue;
            String finalCrumbs = new String (crumbsBuf);
            String finalCrumbId = new String (crumbIdBuf);

            keyValue = KEY_NAME_BREADCRUMB + KEY_VALUE_DELIMITER + finalCrumbs;
            hProductKVList.add (keyValue);
            keyValue = KEY_NAME_BREADCRUMB_ID + KEY_VALUE_DELIMITER + finalCrumbId;
            hProductKVList.add (keyValue);
        }

        return;
    }

    // generate full crumbId for a crumb found in product->bread_crumb. We need to generate 
    // this since category.tsv does not have this crumb
    // This 'generate' method adds each element in this crumb in hCategoryMap object as well
    // This is a recursive method
    private CategoryInfo generateCategoryInfo (String fullCrumb, HashMap <String, CategoryInfo> hCategoryMap) 
    {
        int delimIndex;
        String leafName;

        // System.out.println ("DEBUG. Generating catinfo for: " + fullCrumb);

        delimIndex = fullCrumb.lastIndexOf (BREADCRUMB_VALUE_DELIMITER);
        if (delimIndex < 0) {
            CategoryInfo topCatInfo;

            // we have reached to the 'top'. No additional recursion needed (ie, this is recursion-end-condition)
            leafName = fullCrumb;
            topCatInfo = hCategoryMap.get (leafName);
            if (topCatInfo == null) {
                topCatInfo = new CategoryInfo ();
                topCatInfo.setLeafName (leafName);
                topCatInfo.setLeafId (Integer.toString (NEXT_UNDEFINED_CRUMB_ID));
                topCatInfo.setParentCatId ("");
                topCatInfo.setFullCrumbId (topCatInfo.leafId);
                topCatInfo.setFullCrumbPath (leafName);
                NEXT_UNDEFINED_CRUMB_ID = NEXT_UNDEFINED_CRUMB_ID + 1;

                // System.out.println ("DEBUG Generated crumbid for: " + leafName + " = " + topCatInfo.getLeafId ());

                // add it to catMap
                hCategoryMap.put (topCatInfo.getFullCrumbPath (), topCatInfo);
            }
            return (topCatInfo); 
        } else {
            String parentFullCrumb;
            CategoryInfo parentCatInfo;
            CategoryInfo catInfo;

            leafName = fullCrumb.substring (delimIndex + 1);
            parentFullCrumb = fullCrumb.substring (0, delimIndex); // eg, "X/Y", "X", ...
            parentCatInfo = hCategoryMap.get (parentFullCrumb);
            if (parentCatInfo == null) {
                parentCatInfo = generateCategoryInfo (parentFullCrumb, hCategoryMap); // recursive call
            }

            catInfo = new CategoryInfo ();
            catInfo.setLeafName (leafName);
            catInfo.setLeafId (Integer.toString (NEXT_UNDEFINED_CRUMB_ID));
            catInfo.setParentCatId (parentCatInfo.getLeafId ());
            catInfo.setFullCrumbId (parentCatInfo.getFullCrumbId () + CRUMBID_DELIMITER_IN_CATEGORYINFO + catInfo.getLeafId ());
            catInfo.setFullCrumbPath (parentCatInfo.getFullCrumbPath () + BREADCRUMB_VALUE_DELIMITER  + leafName);
            NEXT_UNDEFINED_CRUMB_ID = NEXT_UNDEFINED_CRUMB_ID + 1;

            // System.out.println ("DEBUG Generated crumbid for: " + leafName + " = " + catInfo.getLeafId ());
            // add it to catMap
            hCategoryMap.put (catInfo.getFullCrumbPath (), catInfo);

            return (catInfo);
        }
    }

    // this 'update' is similar to 'generate' method above, except that the crumbIds are already available via category.tsv 
    // Note that the crumb is in the form "X>Y>Z" whereas crumbId is in the form "111/222/333" (delimiters are different because of syntax in category.tsv)
    private CategoryInfo updateCategoryInfo (String fullCrumb, String fullCrumbId, HashMap <String, CategoryInfo> hCategoryMap) {
        int crumbDelimIndex;
        int crumbIdDelimIndex;
        String leafName;
        String leafId;

        // System.out.println ("DEBUG. Updating catinfo for: " + fullCrumb + ", crumbid = " + fullCrumbId);

        crumbDelimIndex = fullCrumb.lastIndexOf (BREADCRUMB_VALUE_DELIMITER);
        crumbIdDelimIndex = fullCrumbId.lastIndexOf (CRUMBID_DELIMITER_IN_CATEGORYINFO);

        if (crumbDelimIndex < 0) {
            CategoryInfo topCatInfo;

            // we have reached to the 'top'. No additional recursion needed (ie, this is recursion-end-condition)
            leafName = fullCrumb;
            topCatInfo = hCategoryMap.get (leafName);
            if (topCatInfo == null) {
                topCatInfo = new CategoryInfo ();
                topCatInfo.setLeafName (leafName);
                if (fullCrumbId.charAt (0) == '/') // Skip leading '/' which is in category.tsv
                    fullCrumbId = fullCrumbId.substring (1);
                topCatInfo.setLeafId (fullCrumbId); 
                topCatInfo.setParentCatId ("");
                topCatInfo.setFullCrumbId (topCatInfo.leafId);
                topCatInfo.setFullCrumbPath (leafName);

                // System.out.println ("DEBUG Updated top crumbid for: " + leafName + " = " + topCatInfo.getLeafId ());

                // add it to catMap
                hCategoryMap.put (topCatInfo.getFullCrumbPath (), topCatInfo);
            }
            return (topCatInfo); 
        } else {
            String parentFullCrumb;
            CategoryInfo parentCatInfo;
            CategoryInfo catInfo;
            String parentFullCrumbId;

            leafName = fullCrumb.substring (crumbDelimIndex + 1);    // eg, "Z"
            parentFullCrumb = fullCrumb.substring (0, crumbDelimIndex); // eg, "X/Y", "X", ...

            leafId = fullCrumbId.substring (crumbIdDelimIndex + 1); // eg, "333"
            parentFullCrumbId = fullCrumbId.substring (0, crumbIdDelimIndex); // eg, 111/222

            parentCatInfo = hCategoryMap.get (parentFullCrumb);
            if (parentCatInfo == null) {
                parentCatInfo = updateCategoryInfo (parentFullCrumb, parentFullCrumbId, hCategoryMap); // recursive call
            }

            catInfo = new CategoryInfo ();
            catInfo.setLeafName (leafName);
            catInfo.setLeafId (leafId);
            catInfo.setParentCatId (parentCatInfo.getLeafId ());
            catInfo.setFullCrumbId (parentCatInfo.getFullCrumbId () + CRUMBID_DELIMITER_IN_CATEGORYINFO + catInfo.getLeafId ());
            catInfo.setFullCrumbPath (parentCatInfo.getFullCrumbPath () + BREADCRUMB_VALUE_DELIMITER  + leafName);

            // System.out.println ("DEBUG Updated crumbid for: " + leafName + " = " + catInfo.getLeafId () + 
            //                      ". Fullcrumb = " + catInfo.getFullCrumbPath() + ", fullcrumbId = " + catInfo.getFullCrumbId());
            // add it to catMap
            hCategoryMap.put (catInfo.getFullCrumbPath (), catInfo);

            return (catInfo);
        }
    }

    // This is a two-pass process. First read the crumbId paths from the .tsv file (eg, 111/222/333)
    // Then in second pass, build crumb-path for each crumb-id-path (eg, X>Y>Z
    // We do a two-pass approach because it is possible that a crumbId may refer to a parent that is not yet seen in category.tsv file
    private HashMap <String, CategoryInfo > convertCategoryTSV (String srcFilePath) throws Exception {

        HashMap <String, CategoryInfo> catIdMap = new HashMap <String, CategoryInfo> ();
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        HashMap <String, CategoryInfo> localCatInfoHashMap;

        // first pass - build localCatInfoList
        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);
            boolean headerLine;

            localCatInfoHashMap= new HashMap <String, CategoryInfo> ();
            headerLine = true;
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;

                // skip header line
                if (headerLine == true) {
                    headerLine = false;
                    continue;
                }

                tokens = srcLine.split ("\t");
                if ((tokens != null) && (tokens.length > 2)) {
                    CategoryInfo catInfo;
                    String leaf_id = tokens [0]; //eg, 1234
                    String leaf_name = tokens [1];    // eg, Gift
                    String parent_cat_id = tokens [2];  // eg, 6789
                    String full_crumbId = tokens [3]; // eg, 111/222/6789/1234

                    // System.err.println ("cat name, cat_id: " + cat_name + ", " + cat_id);
                    catInfo = new CategoryInfo (leaf_name, leaf_id, parent_cat_id, full_crumbId);
                    localCatInfoHashMap.put (leaf_id, catInfo); // key = leaf_id
                }
            }

        } catch (Exception e) {
            System.err.println ("File not found: " + srcFilePath);
            return (null);
        }

        if (srcReader != null)
        {
            try {
                srcReader.close ();
            }
            catch (Exception e)
            {
                System.err.println ("Src reader close exception: " + e.getMessage ());
            }
        }

        // second pass
        // for each 'breadcrumb_id' (eg, 111/222/...), find corresponding crumb
        // and generate a fullcrumb for it (eg, "X>Y>Z")
        // Then update categoryMap for itself and all its parents (if not done already)
        for (CategoryInfo catInfo : localCatInfoHashMap.values()) {

            String fullCrumbId;
            String fullCrumb;
            StringBuffer cumulativeCrumbPathBuf;
            String[] crumbIdList;

            fullCrumbId = catInfo.getFullCrumbId (); // eg, 111/222/6789/1234
            crumbIdList = fullCrumbId.split (CRUMBID_DELIMITER_IN_CATEGORYINFO);

            cumulativeCrumbPathBuf = new StringBuffer ();
            for (String aCrumbId : crumbIdList) {
                CategoryInfo aCatInfo;

                aCatInfo = localCatInfoHashMap.get (aCrumbId);
                if (aCatInfo != null) {
                    if (cumulativeCrumbPathBuf.length () > 0) 
                        // Note that the delimiter in crumb-path is ">" since that is the delimiter used in products->bread_crumb
                        cumulativeCrumbPathBuf.append (BREADCRUMB_VALUE_DELIMITER);

                    cumulativeCrumbPathBuf.append (aCatInfo.getLeafName());
                }
            }

            // set this fullCrumbPath in catInfo object
            // fullCrumb is like "X>Y>Z"
            fullCrumb = new String (cumulativeCrumbPathBuf);
            catInfo.setFullCrumbPath (fullCrumb);

            // add to catIdMap. "key" is fullcrumb associated with the catInfo
            // System.out.println ("DEBUG adding catinfo for: " + fullCrumb);

            // catIdMap.put (fullCrumb, catInfo);
            // update categoryMap for this crumb and all its parent(s) if not already done
            updateCategoryInfo (fullCrumb, catInfo.getFullCrumbId(), catIdMap);
        }

        return (catIdMap); 
    }

    // Some SKU values are used in multiple PIDs. While Bloomreach handles such situations
    // CommerceTool does not. Therefore, exclude sku if that is already used by some product
    // we have already processed
    private boolean isSkuDuplicate (XPath xpath, Node outProductsNode, ArrayList<String> hProductSkuKVList) {
        NodeList productSkuNodes = null;
        String xPathQuery;
        String skuid;

        skuid = null;
        for (int i = 0; i < hProductSkuKVList.size (); i++) {
            String keyVal;
            String key;
            String value;            
            int indx;
          
            keyVal = hProductSkuKVList.get (i); 
            indx = keyVal.indexOf (KEY_VALUE_DELIMITER); // delimiter used in key-value pairs from product.xml
            key = keyVal.substring (0,indx);
            if (key.equals (PRODUCT_SKU_FIELD_NAME) == true) {
                skuid = keyVal.substring (indx + KEY_VALUE_DELIMITER.length());
                break;
            }
        }

        // this particular node does not have a 'skuid' (which is really an error)
        if (skuid == null)
            return (false);

        xPathQuery = "//" + PRODUCT_NODE_NAME + "/" + PRODUCT_SKU_FIELD_NAME + "[text()='" + skuid + "']";
        // System.out.println ("DEBUG xpath query = " + xPathQuery);

        try {
            productSkuNodes = (NodeList) xpath.evaluate (xPathQuery, outProductsNode, XPathConstants.NODESET);
        } catch (Exception e) {
            System.err.println ("Error in xPath expression: " + e.getMessage());
            return (false);
        }

        if ((productSkuNodes == null) || (productSkuNodes.getLength () == 0))  
            return (false);
      
        System.out.println ("WARNING Found duplicate SKU = " + skuid); 
        return (true); 
    }

    private void appendToProductsNode (ArrayList <String> hProductKVList, Node outProductsNode, Document outDocument) throws Exception {
        Node newProductNode;

        newProductNode = outDocument.createElement (PRODUCT_NODE_NAME); // '<product>'
        outProductsNode.appendChild (newProductNode); // add '<product>' to the parent '<products>' node

        for (int i = 0; i < hProductKVList.size (); i++) {
            Element fieldElem;
            String keyVal;
            String key;
            String value;            
            int indx;
          
            keyVal = hProductKVList.get (i); 
            indx = keyVal.indexOf (KEY_VALUE_DELIMITER); // delimiter used in key-value pairs from product.xml
            key = keyVal.substring (0,indx);
            value = keyVal.substring (indx + KEY_VALUE_DELIMITER.length());

            // field ==> price, sale_price, title, ... 
            fieldElem = outDocument.createElement (key);
            newProductNode.appendChild (fieldElem); // add each 'field' to <product> node

            // add a textNode and its value to fieldElem
            fieldElem.appendChild (outDocument.createTextNode (value)); 
        }

    }

    // information captured from category.tsv file
    // The 'set' methods are used when we have to generate crumbIds ourselves since they are not in category.tsv file
    class CategoryInfo 
    {
        String leafName;
        String leafId;
        String parentCatId;
        String fullCrumbId; // eg, 111/222/333
        String fullCrumb; // eg, "X>Y>Z"

        public CategoryInfo () {
        }

        public CategoryInfo (String leafName, String leafId, String parentCatId, String fullCrumbId) {
            this.leafName = leafName;
            this.leafId = leafId;
            this.parentCatId = parentCatId;
            this.fullCrumbId = fullCrumbId;
        }

        public void setLeafName (String leafName) {
            this.leafName = leafName;
        }

        public String getLeafName () {
            return this.leafName;
        }

        public void setLeafId (String leafId) {
            this.leafId = leafId;
        }

        public String getLeafId () {
            return this.leafId;
        }

        public void setParentCatId (String parentCatId) {
            this.parentCatId = parentCatId;
        }

        public String getParentCatId () {
            return this.parentCatId;
        }

        public void setFullCrumbId (String fullCrumbId) {
            this.fullCrumbId = fullCrumbId;
        }

        public String getFullCrumbId () {
            return this.fullCrumbId;
        }

        public void setFullCrumbPath (String fullCrumbPath) {
            this.fullCrumb = fullCrumbPath;
        }

        public String getFullCrumbPath () {
            return (this.fullCrumb);
        }
    }
}

