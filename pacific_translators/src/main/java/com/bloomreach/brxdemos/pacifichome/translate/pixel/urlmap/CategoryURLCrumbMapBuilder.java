package com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap;

// for category urls, 
// full url is of the form: 
//      url: https://www.worldmarket.com/category/home-decor-pillows/pillows/throw-pillows.do
// actual 'cat_id' in in pixel is of the form:
//      key: "cat_id"   value: "117419"
//      -- this is slightly strange because we preprocess cat_id line in the shell script 
//      -- see gencaturlmap.sh
// step1:
//  build a map between uri (string after domain in url) -> cat_id
//      home-decor-pillows/pillows/throw-pillows --> 116886
// step2:
//  process all 'category' refUrls, check if url-leaf is already in above map
//  If not, attempt to get that value from processedFeed
// step3:
//  save to .tsv
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.URLDecoder;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.ProcessedFeed;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.FeedCrumbData;

public class CategoryURLCrumbMapBuilder {

    private static String VERSION = "0.3.0";

    // CATEGORY_URL_CRUMBID_MAP_IN file has lines output by grep. File format (including the '---')
    // [--]
    // url: "https://www.worldmarket.com/category/sale.do"
    // ptype: "category"
    // cat: "Sale"
    // key: "cat_id"   value: "117419"
    // [--]

    private static String FILENAME_CATEGORY_URL_CRUMBID_MAP_IN = "source/cat_url_crumbid_extracted_data.txt";

    // ref_url_list has lines such as:
    // ref_url: "http://httpsmacyswww.worldmarket.com/category/furniture/living-room/console-tables.do"
    // the trailing ".do" may/may-not exist
    private static String FILENAME_CAT_REFURL_LIST_IN = "source/cat_refurl_extracted_data_sorted.txt";

    // output .tsv. Format: {'leaf', 'crumb', 'crumbId' }
    // NOTE: map is between the 'urlleaf' -> crumb (not the entire url)
    private static String FILENAME_PRODUCT_URLLEAF_CRUMB_MAP_OUT = "source/cat_urlleaf_crumb_map.tsv";

    // process feed file name
    private static String PREPROCESSED_FEED_PATH = "./source/translated_feed.xml";

    // processedFeed
    private ProcessedFeed processedFeed;

    public static void main (String[] args) {
        CategoryURLCrumbMapBuilder mapBuilder;

        System.out.println ("CategoryURLCrumbMapBuilder version " + VERSION);

        if ((args == null) || (args.length < 1)) {
            System.out.println ("CategoryURLCrumbMapBuilder rootDataDir");
            System.exit (-1);
        }

        mapBuilder = new CategoryURLCrumbMapBuilder ();

        try {
            mapBuilder.init (args[0]);
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (-1);
        }

        try {
            mapBuilder.generateMap (args[0]);
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (-1);
        }

        System.exit (0);
    }

    private CategoryURLCrumbMapBuilder () {
    }

    private void init (String rootDataDir) throws Exception {
        // processedFeed
        processedFeed = new ProcessedFeed ();
        try {
            File processedFeedFile;
            processedFeedFile = new File (rootDataDir, PREPROCESSED_FEED_PATH);
            processedFeed.load (processedFeedFile.getPath());
        } catch (Exception e) {
            System.out.println ("ProcessedFeed exception: " + e.getMessage ());
        }
    }

    private void generateMap (String rootDataDir) throws Exception {
        HashMap <String, FeedCrumbData> urlLeafCrumbDataMap;
        File srcMapFile;
        File outputFile;
        File srcRefUrlsFile;
        ArrayList<String> extra_urlLeafs_list;

        srcMapFile = new File (rootDataDir, FILENAME_CATEGORY_URL_CRUMBID_MAP_IN);
        if (srcMapFile.exists () == false) {
            throw new Exception ("Source file does not exist: " + srcMapFile.getPath());
        }

        System.out.println ("Building category urlLeaf-crumbid map"); 
        urlLeafCrumbDataMap = buildMap (srcMapFile);
        if (urlLeafCrumbDataMap.size () == 0) {
            throw new Exception ("Could not generate urlLeaf-crumbid-map");
        }

        System.out.println ("Crosscheck refUrls with category urlLeaf-crumbid map"); 
        // cross-check url-pid map versus ref-url list
        srcRefUrlsFile = new File (rootDataDir, FILENAME_CAT_REFURL_LIST_IN);
        extra_urlLeafs_list = crossCheckRefUrls (urlLeafCrumbDataMap, srcRefUrlsFile);

        // if extra ref urls exist, use processedFeed calls to extend urlLeafCrumbDataMap
        if (extra_urlLeafs_list.size () > 0) {
            extendUrlLeafCrumbMap (extra_urlLeafs_list, urlLeafCrumbDataMap);
        }

        // finally output the urlLeafCrumbDataMap to .tsv
        System.out.println ("Saving urlLeaf-crumbid map");
        outputFile = new File (rootDataDir, FILENAME_PRODUCT_URLLEAF_CRUMB_MAP_OUT);
        saveMap (urlLeafCrumbDataMap, outputFile);
    }

    private HashMap <String, FeedCrumbData> buildMap (File srcMapFile) throws Exception {
        BufferedReader reader;
        String srcLine;
        HashMap <String, FeedCrumbData> urlLeafCrumbDataMap;

        urlLeafCrumbDataMap = new HashMap <String, FeedCrumbData> ();  // key = urlLeaf, value = crumbid
        try {
            reader = new BufferedReader (new FileReader (srcMapFile));
            srcLine = reader.readLine ();
            while (srcLine != null) {
                String urlLine;
                String ptypeLine;
                String catLine;
                String catIdLine;

                if (srcLine.startsWith ("--") == true) {
                    urlLine = reader.readLine ();
                } else {
                    urlLine = srcLine;
                }

                ptypeLine = reader.readLine ();
                catLine = reader.readLine (); // this is cat in pixel; value may not be same as crumb in feed
                catIdLine = reader.readLine ();

                if ((urlLine != null) && (catLine != null) && (catIdLine != null)) {
                    prepareMapEntry (urlLine, catLine, catIdLine, urlLeafCrumbDataMap);
                }

                srcLine = reader.readLine ();
            }
            reader.close ();
        } catch (IOException ie) {
            ie.printStackTrace ();
        }

        System.out.println ("category urlLeaf-crumb map count: " + urlLeafCrumbDataMap.entrySet().size()); 
        return (urlLeafCrumbDataMap);
    }

    private void prepareMapEntry (String urlLine, String catLine, String catIdLine, 
                                  HashMap<String, FeedCrumbData> urlLeafCrumbDataMap) throws Exception {
        int dblQuotIndx;
        int doIndx;
        int rDblQuotIndx; // reverse-dbl-quote-indx
        int rSlashIndx;
        String url;
        String urlLeaf;
        String cat;
        String catId;
        FeedCrumbData crumbData;
        String valueString;
        int valueIndx;

        // extract actual url from urlLine. 
        if (urlLine.indexOf ("/category/") > 0) {
            // actual url
            dblQuotIndx = urlLine.indexOf ('"');
            doIndx = urlLine.indexOf (".do");
            if (doIndx > 0) {
                url = urlLine.substring (dblQuotIndx+1, doIndx);
            } else {
                url = urlLine.substring (dblQuotIndx+1);
            }

            // many urls startWith non-worldmarket domain - skip those
            if ((url.startsWith ("http://www.worldmarket.com/category/") == false) && 
                (url.startsWith ("https://www.worldmarket.com/category/") == false))
                    return;

            // extract urlLeaf
            rSlashIndx = url.lastIndexOf ("/");
            if (rSlashIndx > 0)
                urlLeaf = url.substring (rSlashIndx+1);
            else
                urlLeaf = url;

            // in urlLeaf, replace '-' to blank
            urlLeaf = urlLeaf.replaceAll ("-", " "); // replace embedded '-'

            // cat 
            dblQuotIndx = catLine.indexOf ('"');
            rDblQuotIndx = catLine.lastIndexOf ('"');
            cat = catLine.substring (dblQuotIndx+1, rDblQuotIndx);

            // cat_id 
            // sample line: key: "cat_id"   value: "117419"
            valueString = "value: \"";  // look for 'value: "'
            valueIndx = catIdLine.indexOf (valueString);
            rDblQuotIndx = catIdLine.lastIndexOf ('"');
            catId = catIdLine.substring (valueIndx + valueString.length(), rDblQuotIndx);

            // check to make sure this catid is in processedFeed
            crumbData = verifyCatIdInProcessedFeed (catId);
            if (crumbData != null) {
                urlLeafCrumbDataMap.putIfAbsent (urlLeaf, crumbData); // urlLeaf -> crumbData
            }
        }
    }

    private FeedCrumbData verifyCatIdInProcessedFeed (String catId) {
        FeedCrumbData crumbData;

        crumbData = processedFeed.getCrumbDataInFeedForId (catId.trim());
        return crumbData; // may be null if this catid is not in processedFeed
    }

    // refUrl list <-> urlLeafCrumbDataMap
    private ArrayList<String> crossCheckRefUrls (HashMap<String, FeedCrumbData> urlLeafCrumbDataMap, File srcRefUrlsFile) 
                                                                                                    throws Exception {
        
        BufferedReader reader;
        String srcLine;
        ArrayList<String> extra_urlLeafs_list;

        extra_urlLeafs_list = new ArrayList <String> ();  
        try {
            reader = new BufferedReader (new FileReader (srcRefUrlsFile));
            srcLine = reader.readLine ();
            // srcLine format: 
            // ref_url: "http://httpsmacyswww.worldmarket.com/category/furniture/living-room/console-tables.do"
            while (srcLine != null) {
                int dblQuotIndx;
                int doIndx;
                int qIndx;
                int rSlashIndx;
                String refUrl;
                String refUrlLeaf;

                // pick only '/category/' refUrls
                if (srcLine.indexOf ("/category/") < 0) {
                    srcLine = reader.readLine ();
                    continue;
                }

                dblQuotIndx = srcLine.indexOf ('"');
                doIndx = srcLine.indexOf (".do");
                if (doIndx > 0) 
                    refUrl = srcLine.substring (dblQuotIndx+1, doIndx);
                else {
                    qIndx = srcLine.indexOf ('?');
                    if (qIndx > 0)
                        refUrl = srcLine.substring (dblQuotIndx+1, qIndx);
                    else
                        refUrl = srcLine.substring (dblQuotIndx+1); // entire string after dblQuot
                }

                // many product refUrls start from google / costplus / ...
                // Ignore those since they don't affect user-journey within pacifichome site
                if ((refUrl.startsWith ("https://www.worldmarket.com/category/") == false) &&
                    (refUrl.startsWith ("http://www.worldmarket.com/category/") == false)) {
                    srcLine = reader.readLine ();
                    continue;
                }

                // extract urlLeaf
                rSlashIndx = refUrl.lastIndexOf ("/");
                refUrlLeaf = refUrl.substring (rSlashIndx+1); 
                refUrlLeaf = URLDecoder.decode (refUrlLeaf);    // some refUrls are URLEncoded
                refUrlLeaf = refUrlLeaf.replaceAll ("-", " "); // remove embedded '-' 
                // see if this refUrlLeaf is already in urlLeafCrumbDataMap. If not, add it to extra_url_list
                if (urlLeafCrumbDataMap.containsKey (refUrlLeaf) == false)
                    extra_urlLeafs_list.add (refUrlLeaf);

                srcLine = reader.readLine ();
            }
        } catch (Exception e) {
            e.printStackTrace ();
            // even with exception, continue to check other refUrls
        }

        return (extra_urlLeafs_list);
    }

    // for refUrls that are not already in urlLeafCrumbDataMap, check processedFeed
    // to get corresponding crumb. Then add that urlLeaf, crumb to urlLeafCrumbDataMap
    private void extendUrlLeafCrumbMap (ArrayList<String> extra_urlLeafs_list, 
                                        HashMap <String, FeedCrumbData> urlLeafCrumbDataMap) throws Exception {

        long timeNow = 0;
        int minutesNow = 0;
        int debugCount = 0;
        int callCount = 0;

        timeNow = System.currentTimeMillis ();
        minutesNow = (int) (timeNow / (1000 * 60));
        System.out.println ("refUrlLeaf check call count: " + callCount + ", minutesNow = " + minutesNow);
        for (String refUrlLeaf : extra_urlLeafs_list) {
            FeedCrumbData crumbData;

            // following mainly to track progress
            ++callCount;
            if (callCount - ((callCount / 5000)*5000) == 0) {
                timeNow = System.currentTimeMillis ();
                minutesNow = (int) (timeNow / (1000 * 60));
                System.out.println ("refUrlLeaf check call count: " + callCount + ", minutesNow = " + minutesNow);
            }

            crumbData = processedFeed.getCrumbDataInFeedForLeaf (refUrlLeaf);
            if (crumbData != null) {
                urlLeafCrumbDataMap.put (refUrlLeaf, crumbData);
                // @@@@ DEBUG - REMOVE FOR RELEASE
                // if (debugCount++ > 10)
                //     break;
            }
        }
        System.out.println ("post crossCheck, category urlLeaf-crumb map count: " + urlLeafCrumbDataMap.entrySet().size()); 
    }
 
    private void saveMap (HashMap <String, FeedCrumbData> urlLeafCrumbDataMap, File outputFile) throws Exception {
        PrintWriter writer;

        try {
            writer = new PrintWriter (new FileWriter (outputFile));
            // header line
            writer.printf ("%s\t%s\t%s\n", "urlLeaf", "crumb", "crumbId");
            for (Map.Entry <String, FeedCrumbData> aMap : urlLeafCrumbDataMap.entrySet()) {
                String crumb;
                String urlLeaf;
                String crumbId;
                FeedCrumbData crumbData;

                urlLeaf = aMap.getKey ();
                crumbData = (FeedCrumbData) aMap.getValue ();
                crumb = crumbData.getFullCrumb ();
                crumbId = crumbData.getLeafCrumbId ();

                writer.printf ("%s\t%s\t%s\n", urlLeaf, crumb, crumbId);
            }
            writer.flush ();
            writer.close ();
        } catch (IOException ie) {
            ie.printStackTrace ();
        }
    }

}
