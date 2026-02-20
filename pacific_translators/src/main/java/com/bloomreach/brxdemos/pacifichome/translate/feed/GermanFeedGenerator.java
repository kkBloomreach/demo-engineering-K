package com.bloomreach.brxdemos.pacifichome.translate.feed;

// Version 1.0.2
// Given the preProcessed .xml, add german-translations for 'title' and 'description' columns
// Currently actual text translations have been made using Google translation
// Detail steps are in README file

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
 * Step 1 - Parse German translated TSV to germanFeed map
 * Step 2 - Parse PreProcessed.xml to keyword-map  + merge german title, description columns
 * Step 3 - Generate output xml using merged map
 * Step 4 - Write german translation info (in case any title/desc was not found and hence needs to be translated)
 */

public class GermanFeedGenerator {

    // this is translated-feed-file (from worldmarket to pacifichome)
    private final static String TRANSLATED_ENGLISH_FEED_FILE_NAME = "./output/output.xml";
    // for 'germaninfo_in' and 'germaninfo_out', see notes in README
    private final static String GERMAN_TRANSLATED_INFO_IN_FILE_NAME = "./source/germaninfo_in.tsv";
    private final static String GERMAN_TRANSLATED_INFO_OUT_FILE_NAME = "./output/germaninfo_out.tsv";
    private final static String GERMAN_OUTPUT_FEED_FILE_NAME = "./output/germanoutput.xml";

    private final static String PRODUCT_NODE_NAME = "product";
    private final static String OUTPUT_ROOT_NODE_NAME = "feed";
    private final static String OUTPUT_PRODUCTS_NODE_NAME = "products";

    private final static String KEY_NAME_PID = "pid";
    private final static String KEY_NAME_TITLE = "title";
    private final static String KEY_NAME_TITLE_GERMAN = "title_de";
    private final static String KEY_NAME_DESCRIPTION = "description";
    private final static String KEY_NAME_DESCRIPTION_GERMAN = "description_de";

    private final static String KEY_VALUE_DELIMITER = "!!";

    private final static int    COLNUM_PID_INGERMANFEED = 0; // colnum in german feed, starting at 0
    private final static int    COLNUM_TITLE_EN_INGERMANFEED = 1; // colnum in german feed, actual english title
    private final static int    COLNUM_TITLE_DE_INGERMANFEED = 2; // colnum in german feed, starting at 0
    private final static int    COLNUM_DESCRIPTION_EN_INGERMANFEED = 3; // colnum in german feed, actual english description
    private final static int    COLNUM_DESCRIPTION_DE_INGERMANFEED = 4; // colnum in german feed, starting at 0

    // Param: source filename 
    public static void main(String[] args) {

        if ((args.length < 1)) {
            System.err.println ("Usage: PrepareGermanFeed <root dir path>");
            return;
        }
 
        GermanFeedGenerator germanFeedGenerator = new GermanFeedGenerator ();
        try {
            germanFeedGenerator.doGenerate (args [0]);
        }
        catch (Exception e) {
            System.err.println ("Exception in processing XML source: " + e.getMessage ());
        }
    }


    private void doGenerate (String rootDirPath) throws Exception
    {
        File translatedEnglishFeedFile;
        String translatedEnglishFeedFilePath;
        File germanInfoFile;
        String germanInfoFilePath;

        HashMap<String, TranslationInfo> hGermanTitleInfoMap = null;
        HashMap<String, TranslationInfo> hGermanDescInfoMap = null;
        ArrayList <HashMap<String, TranslationInfo>> hGermanInfoMapArray; // two elements, first is title-info-hashmap, second is desc-info-hashmap
        DocumentBuilder outDocBuilder = null;
        Document outDocument = null;
        Node outProductsNode;
 
        System.err.println(" > Start reading German TSV ....");
        germanInfoFile = new File (rootDirPath, GERMAN_TRANSLATED_INFO_IN_FILE_NAME);
        germanInfoFilePath = germanInfoFile.getPath ();
        hGermanInfoMapArray = processGermanInfoFile (germanInfoFilePath);
        hGermanTitleInfoMap = hGermanInfoMapArray.get (0);
        hGermanDescInfoMap = hGermanInfoMapArray.get (1);
        System.err.println(" ............................... Done processing german info.");

        // prepare output xml node tree.
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

        translatedEnglishFeedFile = new File (rootDirPath, TRANSLATED_ENGLISH_FEED_FILE_NAME);
        translatedEnglishFeedFilePath = translatedEnglishFeedFile.getPath ();
        System.err.println(" > Start reading translated english file...." + translatedEnglishFeedFilePath);
        convertProductXML(translatedEnglishFeedFilePath, hGermanTitleInfoMap, hGermanDescInfoMap, outProductsNode, outDocument);
        System.err.println(" ............................... Done reading translated english feed file.");

        // write ("transform") root node to output file
        try {
                File outProductFile = new File (rootDirPath, GERMAN_OUTPUT_FEED_FILE_NAME);
                System.err.println(" > Start writing german feed output file...." + outProductFile.getPath());
                Result outResult = new StreamResult (outProductFile);
    
                Transformer outTransformer = TransformerFactory.newInstance ().newTransformer ();
                outTransformer.setOutputProperty (OutputKeys.INDENT, "yes");
                outTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                Source outSource = new DOMSource (outDocument);
                outTransformer.transform (outSource, outResult);

                System.err.println(" ............................... Done writing german feed file.");
        } catch (Exception e) {
            System.err.println ("Exception in writing XML to output file: " + e.getMessage ());
        }
   
        // Also write german info "out", in case any title/description needs to be manually edited
        // Details are in README
        try {
                File outGermanInfoFile = new File (rootDirPath, GERMAN_TRANSLATED_INFO_OUT_FILE_NAME);
                System.err.println(" > Start writing german info output file...." + outGermanInfoFile.getPath());
                FileWriter germanInfoWriter = new FileWriter (outGermanInfoFile);
                String header;

                // header line
                header = new String ("pid" + "\t" + "title" + "\t" + "title_de"  + "\t" + "description" + "\t" + "description_de" + "\n");
                germanInfoWriter.write (header);

                for (String aPid : hGermanTitleInfoMap.keySet ()) {
                    TranslationInfo ttlInfo;
                    TranslationInfo descInfo;
                    String trOutRow;
                    String ttl_en;
                    String ttl_de;
                    String desc_en;
                    String desc_de;

                    // In some cases, input feed itself may not have <title> or <description>
                    // tag. In that case, there is nothing to translate...
                    ttlInfo = hGermanTitleInfoMap.get (aPid);
                    if (ttlInfo != null) {
                        ttl_en = ttlInfo.getText_en ();
                        ttl_de = ttlInfo.getText_de ();
                    } else {
                        ttl_en = "none";
                        ttl_de = "none";
                    }
           
                    descInfo = hGermanDescInfoMap.get (aPid);
                    if (descInfo != null) {
                        desc_en = descInfo.getText_en ();
                        desc_de = descInfo.getText_de ();
                    } else {
                        desc_en = "none";
                        desc_de = "none";
                    }
 
                    trOutRow = new String (aPid + "\t" + ttl_en  + "\t" + ttl_de + "\t" +
                                           desc_en + "\t" + desc_de + "\n");
                    germanInfoWriter.write (trOutRow);
                }

                germanInfoWriter.flush ();
                germanInfoWriter.close ();

                System.err.println(" ............................... Done writing german info output file.");
        } catch (Exception e) {
            e.printStackTrace ();
            System.err.println ("Exception in writing german info to output file: " + e.getMessage ());
        }

         
    }

    /**
     * 
     * 
     */
    private void convertProductXML (String srcFileName, HashMap<String, TranslationInfo> titleInfoMap, 
                                    HashMap<String, TranslationInfo> descInfoMap, 
                                    Node outProductsNode, Document outDocument) throws Exception {

        Document srcDocument = null;
        NodeList nList = null; // list of ALL <product> nodes in src XML

        try {
                DocumentBuilder srcDocBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder ();
                srcDocument = srcDocBuilder.parse(new File(srcFileName));
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

        for (int count = 0; count < nList.getLength(); count++) {
            NodeList childNodes;

            Node aProductNode = nList.item (count);
            // System.out.println ("node name: " + aProductNode.getNodeName ());

            try {
                ArrayList <String> hTranslatedNode;

                // translate srcXML node to outXML map
                hTranslatedNode = transferOneProductNode (aProductNode, titleInfoMap, descInfoMap);

                // generate XML node "product" for translated map and append to 'products' node
                appendToProductsNode (hTranslatedNode, outProductsNode, outDocument);

            } catch (Exception e) {
                System.err.println ("Exception in translation of product node: " + aProductNode.getNodeName ());
                e.printStackTrace ();
            }
        }
    }

    private ArrayList<String> transferOneProductNode (Node aProductNode, 
                                                      HashMap<String, TranslationInfo> titleInfoMap,
                                                      HashMap<String, TranslationInfo> descInfoMap
                                                     ) throws Exception {
        NodeList childNodes;
        ArrayList <String> hProductKVList = new ArrayList <String>();
        String thisPid;

        childNodes = aProductNode.getChildNodes ();
        thisPid = null; // null until we see a value in source product node

        // System.out.println ("child count = " + childNodes.getLength ());
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
                    // System.out.println ("child value: <blank>");
                    keyValue = childName + KEY_VALUE_DELIMITER + childValue;
                    hProductKVList.add (keyValue);
                    continue;
                } 

                // now process specific keys in this product node
                if (childName.equalsIgnoreCase (KEY_NAME_PID) == true) {
                    thisPid = childValue;   // needed later
                    keyValue = KEY_NAME_PID + KEY_VALUE_DELIMITER + childValue;
                    hProductKVList.add (keyValue);
                } 
                else if (childName.equalsIgnoreCase (KEY_NAME_TITLE) == true) {
                        // if key == title, get corresponding german title
                        // Add both (original and german) title to output
                        // and then add both into the keywordMap (arrayList)
                        // Also, english-title string is passed to this method so that, if german translation
                        // is not available, it can be written out for subsequent translation 
                        addGermanTitle (thisPid, titleInfoMap, hProductKVList, childValue);

                        // also add original title to output
                        keyValue = KEY_NAME_TITLE + KEY_VALUE_DELIMITER + childValue;
                        hProductKVList.add (keyValue);
                } else if (childName.equalsIgnoreCase (KEY_NAME_DESCRIPTION) == true) {
                        // if key == description, get corresponding german description
                        // Add both (original and german) description to output
                        // and then add both into the keywordMap (arrayList)
                        addGermanDescription (thisPid, descInfoMap, hProductKVList, childValue);

                        // also add original description to output
                        keyValue = KEY_NAME_DESCRIPTION + KEY_VALUE_DELIMITER + childValue;
                        hProductKVList.add (keyValue);
                } else { // none-of-the-above 'special' cases
                    // prepare a key-value string and append to arrayList
                    keyValue = childName + KEY_VALUE_DELIMITER + childValue;
                    hProductKVList.add (keyValue);
                }
            }
         }

         return (hProductKVList);
    }

    // add german title field 
    private void addGermanTitle (String pid, HashMap<String, TranslationInfo> titleInfoMap, ArrayList<String> hProductKVList, String title_en) {
        String germanTitle;
        TranslationInfo ttlInfo;
        String keyValue;

        ttlInfo = titleInfoMap.get (pid); // key = pid
        if (ttlInfo == null) {
            System.err.println ("Title translation missing for: " + pid);
            ttlInfo = new TranslationInfo (title_en, "tbd");
            titleInfoMap.put (pid, ttlInfo);
        } 
        keyValue = KEY_NAME_TITLE_GERMAN + KEY_VALUE_DELIMITER + ttlInfo.getText_de ();
        hProductKVList.add (keyValue);
    }

    // add german description field 
    private void addGermanDescription (String pid, HashMap<String, TranslationInfo> descInfoMap, ArrayList<String> hProductKVList, String desc_en) {
        String germanDescription;
        TranslationInfo descInfo;
        String keyValue;

        descInfo = descInfoMap.get (pid); // key = pid
        if (descInfo == null) {
            System.err.println ("Description translation missing for: " + pid);
            descInfo = new TranslationInfo (desc_en, "tbd");
            descInfoMap.put (pid, descInfo);
        } 

        keyValue = KEY_NAME_DESCRIPTION_GERMAN + KEY_VALUE_DELIMITER + descInfo.getText_de ();
        hProductKVList.add (keyValue);
    }


    // this feed file should be in TSV format and have 5 columns
    // pid, english-title, german-title, english-description, german-description
    // Returns array of two HashMap objects. 
    // 0th is TitleTranslationInfo. Next DescriptionTranslationInfo
    private ArrayList <HashMap<String, TranslationInfo>> processGermanInfoFile (String srcFileName) throws Exception {

        ArrayList <HashMap <String, TranslationInfo>> hashMapArray;

        HashMap <String, TranslationInfo> titleInfoMap = new HashMap <String, TranslationInfo> ();
        HashMap <String, TranslationInfo> descInfoMap = new HashMap <String, TranslationInfo> ();
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;

        try {       
            srcFile = new File (srcFileName);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            while ((srcLine = srcBufferedReader.readLine ()) != null) {

                String[] tokens;

                tokens = srcLine.split ("\t");
                if ((tokens != null) && (tokens.length > COLNUM_DESCRIPTION_DE_INGERMANFEED)) {
                    String pid_de;
                    String ttl_en;
                    String ttl_de;
                    String desc_en;
                    String desc_de;
                    TranslationInfo ttlInfo;
                    TranslationInfo descInfo;

                    pid_de = tokens [COLNUM_PID_INGERMANFEED]; 
                    ttl_en = tokens [COLNUM_TITLE_EN_INGERMANFEED]; 
                    ttl_de = tokens [COLNUM_TITLE_DE_INGERMANFEED]; 
                    desc_en = tokens [COLNUM_DESCRIPTION_EN_INGERMANFEED]; 
                    desc_de = tokens [COLNUM_DESCRIPTION_DE_INGERMANFEED]; 

                    // create objects containing title_de, description_de
                    // and add it to hashMap. key = pid, value = this object
                    // System.err.println ("title_de: " + ttl_de + ", description_de: " +  desc_de);
                    ttlInfo = new TranslationInfo (ttl_en, ttl_de);
                    descInfo = new TranslationInfo (desc_en, desc_de);
                    titleInfoMap.put (pid_de, ttlInfo);
                    descInfoMap.put (pid_de, descInfo);
                }
            }
        } catch (Exception e) {
            System.err.println ("File not found: " + srcFileName);
            return (null);
        }

        if (srcReader != null) {
            try {
                srcReader.close ();
            }
            catch (Exception e) {
                System.err.println ("Src reader close exception: " + e.getMessage ());
            }
        }

        // return array of two 'hashMap' objects
        hashMapArray = new ArrayList <HashMap <String, TranslationInfo>> ();
        hashMapArray.add (titleInfoMap);
        hashMapArray.add (descInfoMap);

        return (hashMapArray); 
    }

    private void appendToProductsNode (ArrayList <String> hProductKVList, Node outProductsNode, Document outDocument) throws Exception {
        Node newProductNode;

        newProductNode = outDocument.createElement (PRODUCT_NODE_NAME);
        outProductsNode.appendChild (newProductNode);

        for (int i = 0; i < hProductKVList.size (); i++) {
            Element attribElem;
            String keyVal;
            String key;
            String value;            
            int indx;
          
            keyVal = hProductKVList.get (i); 
            indx = keyVal.indexOf (KEY_VALUE_DELIMITER); // delimiter used in key-value pairs from product.xml
            key = keyVal.substring (0,indx);
            value = keyVal.substring (indx + KEY_VALUE_DELIMITER.length());
 
            attribElem = outDocument.createElement (key);
            newProductNode.appendChild (attribElem);

            attribElem.appendChild (outDocument.createTextNode (value));
        }

    }

    // Inner class
    class TranslationInfo 
    {
        String text_en = null;
        String text_de = null;

        TranslationInfo (String txt_en, String txt_de) {
            text_en = txt_en;
            text_de = txt_de;
        }

        public String getText_en () {
            return (text_en);
        }

        public String getText_de () {
            return (text_de);
        }
    }
}

