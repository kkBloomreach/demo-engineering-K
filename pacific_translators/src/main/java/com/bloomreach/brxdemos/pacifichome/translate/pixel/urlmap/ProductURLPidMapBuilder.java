package com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap;

// for ref_url, if it refers to a product, that value is like:
// https://www.worldmarket.com/product/dove-gray-leilani-dining-chairs-set-of-2.do?sortby=ourPicks
// On the other hand, in PacificHome, product urls are like:
// https://pacifichome.bloomreach.com/products/home/<pid>_<sku>
// Therefore, mapping source ref_url to the output requires a source-url-value -> translated-url-value
// We prepare this map once and then reuse it each time translator is executed. That is to improve
// translator performance.
// This mapping process is multi-step
// step1: execute a shell script to generate a pixelLog, grep ptype, url, prod_id from source -> save to file
// step2: run this java class to read step1 file and build a .tsv with: {url, prod_id} map
// step3; During translation, use the above .tsv file to lookup pid for a ref_url in source and construct 
// output-style product url

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.ProcessedFeed;

public class ProductURLPidMapBuilder {

    private static String VERSION = "0.2.2";

    // PRODUCT_URL_MAP_IN file has lines output by grep. File format (including the '---')
    // [---]
    // url: "url-string
    // ptype: "product"
    // prod_id: pid_value
    // ---
    private static String FILENAME_PRODUCT_URL_MAP_IN = "source/prod_pid_url_extracted_data.txt";

    // ref_url_list has lines such as:
    // ref_url: "http://www.worldmarket.com/product/aiden+furniture+collection.do?&from=fn"
    private static String FILENAME_PROD_REFURL_LIST_IN = "source/prod_refurl_extracted_data_sorted.txt";

    // output .tsv. Format: {'url', 'pid'}
    private static String FILENAME_PRODUCT_URL_MAP_OUT = "source/prod_url_pid_map.tsv";

    // process feed file name
    private static String PREPROCESSED_FEED_PATH = "./source/translated_feed.xml";

    // processedFeed
    private ProcessedFeed processedFeed;

    public static void main (String[] args) {
        ProductURLPidMapBuilder urlPidMapBuilder;

        System.out.println ("ProductUrlPidMapBuilder version " + VERSION);

        if ((args == null) || (args.length < 1)) {
            System.out.println ("ProductURLPidMapBuilder rootDataDir");
            System.exit (-1);
        }

        urlPidMapBuilder = new ProductURLPidMapBuilder ();

        try {
            urlPidMapBuilder.init (args[0]);
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (-1);
        }

        try {
            urlPidMapBuilder.generateMap (args[0]);
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (-1);
        }

        System.exit (0);
    }

    private ProductURLPidMapBuilder () {
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
        HashMap <String, String> urlPidMap;
        File srcMapFile;
        File outputFile;
        File srcRefUrlsFile;
        ArrayList<String> extra_urls_list;

        srcMapFile = new File (rootDataDir, FILENAME_PRODUCT_URL_MAP_IN);
        if (srcMapFile.exists () == false) {
            throw new Exception ("Source file does not exist: " + srcMapFile.getPath());
        }

        System.out.println ("Building product url-pid map"); 
        urlPidMap = buildMap (srcMapFile);
        if (urlPidMap.size () == 0) {
            throw new Exception ("Could not generate url-pid-map");
        }

        System.out.println ("Crosscheck refUrls with product url-pid map"); 
        // cross-check url-pid map versus ref-url list
        srcRefUrlsFile = new File (rootDataDir, FILENAME_PROD_REFURL_LIST_IN);
        extra_urls_list = crossCheckRefUrls (urlPidMap, srcRefUrlsFile);

        // if extra ref urls exist, make discovery API calls to extend urlPidMap
        if (extra_urls_list.size () > 0) {
            DiscoveryQuery discoveryQuery;

            System.out.println ("Perform discoveryAPI calls for " + extra_urls_list.size() + " urls");
            discoveryQuery = new DiscoveryQuery ();
            extendPidUrlMap (extra_urls_list, urlPidMap, discoveryQuery);
        }

        // finally output the urlPidMap to .tsv
        System.out.println ("Saving url-pid map");
        outputFile = new File (rootDataDir, FILENAME_PRODUCT_URL_MAP_OUT);
        saveMap (urlPidMap, outputFile);
    }

    private HashMap <String, String> buildMap (File srcMapFile) throws Exception {
        BufferedReader reader;
        String srcLine;
        HashMap <String, String> urlPidMapList;

        urlPidMapList = new HashMap <String, String> ();  // key = url, value = pid
        try {
            reader = new BufferedReader (new FileReader (srcMapFile));
            srcLine = reader.readLine ();
            while (srcLine != null) {
                String urlLine;
                String ptypeLine;
                String prodIdLine;

                if (srcLine.startsWith ("--") == true) {
                    urlLine = reader.readLine ();
                } else {
                    urlLine = srcLine;
                }

                ptypeLine = reader.readLine ();
                prodIdLine = reader.readLine ();

                if ((urlLine != null) && (prodIdLine != null)) {
                    prepareMapEntry (urlLine, prodIdLine, urlPidMapList);
                }

                srcLine = reader.readLine ();
            }
            reader.close ();
        } catch (IOException ie) {
            ie.printStackTrace ();
        }

        System.out.println ("product url-pid map count: " + urlPidMapList.entrySet().size()); 
        return (urlPidMapList);
    }

    private void prepareMapEntry (String urlLine, String prodIdLine, HashMap<String, String> urlPidMapList) throws Exception {
        int dblQuotIndx;
        int doIndx;
        int rDblQuotIndx; // reverse-dbl-quote-indx
        String url;
        String pid;

        // extract actual url from urlLine. Some urlLines have '/category/' even though
        // ptype is product. Skip those (strange cases)
        if (urlLine.indexOf ("/category/") < 0) {

            // actual url
            dblQuotIndx = urlLine.indexOf ('"');
            doIndx = urlLine.indexOf (".do");
            if (doIndx > 0) {
                url = urlLine.substring (dblQuotIndx+1, doIndx);
            } else {
                url = urlLine.substring (dblQuotIndx+1);
            }

            // prodId
            dblQuotIndx = prodIdLine.indexOf ('"');
            rDblQuotIndx = prodIdLine.lastIndexOf ('"');
            pid = prodIdLine.substring (dblQuotIndx+1, rDblQuotIndx);

            // check to make sure this pid is in processedFeed
            pid = verifyPidInProcessedFeed (pid);
            if (pid != null) {
                if (urlPidMapList.containsKey (url) == false)  // avoid duplicates
                    urlPidMapList.put (url, pid); // key = url, value = pid
            }
        }
    }

    private String verifyPidInProcessedFeed (String pid) {
        // strange case - some 'pid' values are really multiple values (eg, 100,200,300)
        // split those and get a pid that is in processedFeed
        if (pid.indexOf (",") > 0) {
            String[] pidList;

            pidList = pid.split (",");
            for (int i = 0; i < pidList.length; i++) {
                if (processedFeed.isProductInFeed (pidList[i].trim()) == true)
                    return (pidList [i].trim());
            }
        } else {
            if (processedFeed.isProductInFeed (pid.trim()) == true)
                return (pid.trim()); 
        }

        return null; // this pid is not in processedFeed
    }

    // refUrl list <-> urlPidMap
    private ArrayList<String> crossCheckRefUrls (HashMap<String, String> urlPidMap, File srcRefUrlsFile) throws Exception {
        
        BufferedReader reader;
        String srcLine;
        ArrayList<String> extra_urls_list;

        extra_urls_list = new ArrayList <String> ();  
        try {
            reader = new BufferedReader (new FileReader (srcRefUrlsFile));
            srcLine = reader.readLine ();
            // srcLine format: 
            // ref_url: "http://www.worldmarket.com/product/aiden+furniture+collection.do?&from=fn"
            while (srcLine != null) {
                int dblQuotIndx;
                int doIndx;
                int qIndx;
                String refUrl;

                // pick only '/product/' refUrls
                if (srcLine.indexOf ("/product/") < 0) {
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
                if ((refUrl.startsWith ("https://www.worldmarket.com/product/") == false) &&
                    (refUrl.startsWith ("http://www.worldmarket.com/product/") == false)) {
                    srcLine = reader.readLine ();
                    continue;
                }
 
                // see if this refUrl is already in urlPidMap. If not, add it to extra_url_list
                if (urlPidMap.containsKey (refUrl) == false)
                    extra_urls_list.add (refUrl);

                srcLine = reader.readLine ();
            }
        } catch (Exception e) {
            e.printStackTrace ();
            // even with exception, continue to check other refUrls
        }

        return (extra_urls_list);
    }

    // for refUrls that are not already in urlPidMap, make discoveryApi call
    // to get corresponding pid. Then add that url, pid to urlPidMap
    private void extendPidUrlMap (ArrayList<String> extra_urls_list, HashMap <String, String> urlPidMap,
                                  DiscoveryQuery discoveryQuery) throws Exception {

        int debugCount = 0;
        int callCount = 0;
        long timeNow = 0;
        int minutesNow = 0;

        timeNow = System.currentTimeMillis ();
        minutesNow = (int) (timeNow / (1000 * 60));
        System.out.println ("Discovery API call count: " + callCount + ", minutesNow = " + minutesNow);
        for (String refUrl : extra_urls_list) {
            String prodLabel;
            int rIndx;
            String prodId;

            rIndx = refUrl.lastIndexOf ('/');
            prodLabel = refUrl.substring (rIndx+1);
            // following mainly to track progress
            ++callCount;
            if (callCount - ((callCount / 100)*100) == 0) {
                timeNow = System.currentTimeMillis ();
                minutesNow = (int) (timeNow / (1000 * 60));
                System.out.println ("Discovery API call count: " + callCount + ", minutesNow = " + minutesNow);
            }
            prodId = discoveryQuery.getPid (prodLabel);
            if (prodId != null) {
                urlPidMap.put (refUrl, prodId);
                // @@@@ DEBUG - REMOVE FOR RELEASE
                // if (debugCount++ > 10)
                //     break;
            }
        }
        System.out.println ("post crossCheck, product url-pid map count: " + urlPidMap.entrySet().size()); 
    }
 
    private void saveMap (HashMap <String, String> urlPidMapList, File outputFile) throws Exception {
        PrintWriter writer;

        try {
            writer = new PrintWriter (new FileWriter (outputFile));
            // header line
            writer.printf ("%s\t%s\n", "url", "pid");
            for (Map.Entry <String, String> aMap : urlPidMapList.entrySet()) {
                String pid;
                String url;

                url = aMap.getKey ();
                pid = aMap.getValue ();

                writer.printf ("%s\t%s\n", url, pid);
            }
            writer.flush ();
            writer.close ();
        } catch (IOException ie) {
            ie.printStackTrace ();
        }
    }
}
