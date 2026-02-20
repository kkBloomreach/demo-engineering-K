package com.bloomreach.trafficgenerator.test;

import com.bloomreach.trafficgenerator.site.feed.ProductFeed;
import com.bloomreach.trafficgenerator.site.feed.ProductJsonlFeed;

public class TestProductFeed {

    // private static String TEST_PID = "15489"; // some test pid
    // private static String TEST_PID = "68303"; // some test pid
    private static String TEST_PID = "KIT0703"; // some test pid

    // Param: source filename 
    public static void main(String[] args) {

        if ((args.length < 1)) {
            System.err.println ("Usage: ProductFeed <preProcessed_feed_filename>");
            return;
        }
 
        ProductFeed feedProcessor = new ProductJsonlFeed ();
        try {
            feedProcessor.load (args [0]);
        }
        catch (Exception e) {
            System.err.println ("Exception in processing sourceFeed: " + e.getMessage ());
        }

        boolean check = feedProcessor.isProductInFeed (TEST_PID);
        System.out.println ("isProductInFeed = " + check);

        String price = feedProcessor.lookupProductPrice (TEST_PID);
        System.out.println ("ProductPrice = " + price);

        String name = feedProcessor.lookupProductName (TEST_PID);
        System.out.println ("ProductName = " + name);
    }
}

// import com.bloomreach.trafficgenerator.site.feed.ProcessedXMLFeed;
// ProductFeed feedProcessor = new ProcessedXMLFeed ();

