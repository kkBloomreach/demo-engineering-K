package com.bloomreach.analyticssimulator.feed;

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

import com.bloomreach.analyticssimulator.MessageLogger;

/**
 * Step 1 - Parse Category TSV to category map
 * Step 2 - Parse ProductFeed.xml to keyword-map  + merge catIds
 * Step 3 - Generate output xml using merged map
 */

public class ProcessedXMLFeed extends ProcessedFeed {

    private final static String KEY_NAME_PID = "pid";
    private final static String KEY_NAME_SKUID = "skuid";
    private final static String KEY_NAME_PRICE = "price";
    private final static String KEY_NAME_TITLE = "title";
    private final static String PRODUCT_NODE_NAME = "product";

    // override base class method
    public void load (String productFilePath) throws Exception
    {
        ArrayList<FeedRecord> parsedFeedRecordList;

        MessageLogger.logDebug (" > Start Parsing source file....: " + productFilePath);
        parsedFeedRecordList = parseCatalog (productFilePath);
        MessageLogger.logDebug (" ............................... Done Parsing Product File.");

        // set in base class
        super.setParsedFeedRecordList (parsedFeedRecordList);
    }

    /**
     * 
     * returns ArrayList<FeedRecord>
     */
    private ArrayList<FeedRecord> parseCatalog (String srcFilePath) throws Exception {
        Document srcDocument = null;
        NodeList nList = null; // list of ALL <product> nodes in src XML
        ArrayList <FeedRecord> parsedFeedRecordList = new ArrayList <FeedRecord> ();

        try {
            DocumentBuilder srcDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder ();
            srcDocument = srcDocBuilder.parse(new File(srcFilePath));
            srcDocument.getDocumentElement().normalize();
        }
        catch (Exception e) {
            MessageLogger.logError ("Could not construct documentBuilder" + e.getMessage());
            return null;
        }

        try {
            // list of ALL <product> nodes in src XML
            nList = srcDocument.getElementsByTagName(PRODUCT_NODE_NAME);
            MessageLogger.logDebug ("Product node list length: " + nList.getLength ());
        }
        catch (Exception e) {
            MessageLogger.logError ("Could not collect product nodes" + e.getMessage());
            return null;
        }

        for (int count = 0; count < nList.getLength(); count++) {
            NodeList childNodes;

            Node aProductNode = nList.item (count);
            MessageLogger.logDebug ("node name: " + aProductNode.getNodeName ());
            try {
                FeedRecord parsedFeedRecord;

                // translate srcXML node to outXML map
                parsedFeedRecord = parseOneProductNode (aProductNode);
                parsedFeedRecordList.add (parsedFeedRecord);
            } catch (Exception e) {
                MessageLogger.logError ("Exception in parsing product node: " + aProductNode.getNodeName ());
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
        MessageLogger.logDebug ("child count = " + childNodes.getLength ());
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
                } else if (childName.equalsIgnoreCase (KEY_NAME_SKUID) == true) {
                    parsedFeedRecord.setProductSkuId (childValue);
                } 
            }
        }

        return (parsedFeedRecord);
    }
}

