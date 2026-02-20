package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

// feed file read in via this class. Feed information is needed to retrieve actual price, sale_price
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

public class ProcessedFeed {

    private final static String CRUMBS_DELIMITER = "|";
    private final static String CRUMBS_ID_DELIMITER = "|";

    // following are column indices in the preprocessed feed. These must match the actual index values
    private final static int CRUMB_RECORD_INDEX = 0;
    private final static int CRUMB_ID_RECORD_INDEX = 1;
    private final static int PRICE_RECORD_INDEX = 401;
    private final static int PID_RECORD_INDEX = 404;
    private final static int VIEWID_RECORD_INDEX = 409;

    // we store only the necessary fields in the product record in this arrayList
    private ArrayList <FeedRecord> processedFeedList;
    private ArrayList <CrumbToIdMap> processedCrumbToIdMap;

    public ProcessedFeed () {
        processedFeedList = new ArrayList <FeedRecord> ();
        processedCrumbToIdMap = new ArrayList <CrumbToIdMap> ();
    }

    // read feed.tsv file and populate internal arrayList
    // We store only the needed fields from the full feed
    // Also populate crumbs->crumbsId map. It is used to
    // clone category page pixels

    // read tsv file and populate the viewId map
    public void load (String feedFilePath) throws Exception {

        // first load specific columns from the processedFeed file
        loadFeedFile (feedFilePath);

        // build internal crumbId -> crumb map. This is needed for category pixels
        buildCrumbIdToCrumbMap ();
    }

    // check if given pid is in the processedFeed
    public boolean isProductInFeed (String pid) {
        for (FeedRecord fr : processedFeedList) {
            if (fr.getProductId ().equals (pid) == true)
                return (true);
        }

        return (false);
    }

    public String lookupProductPrice (String pid, String viewId) {

        for (FeedRecord fr : processedFeedList) {
            if (fr.getProductId ().equals (pid) == true) {
                return (fr.getProductPrice (viewId));
            }
        }

        return (null);

    }

    // given leafCrumb id, return its fullCrumb 
    public String lookupCrumb (String leafCrumbId) {

        for (CrumbToIdMap cr : processedCrumbToIdMap) {
            if (cr.getLeafCrumbId ().equals (leafCrumbId) == true)
                return (cr.getCumulativeCrumb ());
        }

        return (null);
    }

    private void loadFeedFile (String feedFilePath) throws Exception {
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        boolean headerLine;

        try {       
            srcFile = new File (feedFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            headerLine = true;
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;

                // skip header line
                if (headerLine == true) {
                    headerLine = false;
                    continue;
                }

                tokens = srcLine.split ("\t");
                if (tokens != null) {
                    FeedRecord feedRec;

                    String prodId = tokens [PID_RECORD_INDEX].trim();
                    String priceList = tokens [PRICE_RECORD_INDEX].trim();
                    String viewIdList = tokens [VIEWID_RECORD_INDEX].trim();
                    String fullCrumb = tokens [CRUMB_RECORD_INDEX].trim();
                    String fullCrumbId = tokens [CRUMB_ID_RECORD_INDEX].trim();

                    feedRec = new FeedRecord (prodId, priceList, fullCrumb, fullCrumbId, viewIdList);
                    processedFeedList.add (feedRec);
                }
            }
        } catch (Exception e) {
            System.err.println ("File not found: " + feedFilePath + ". Message = " + e.getMessage ());
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
    }

    private void buildCrumbIdToCrumbMap () {

        for (FeedRecord feedRecord : processedFeedList) {
            String [] fullCrumbList;
            String [] fullCrumbIdList;
            String cumulativeCrumb;
            String cumulativeCrumbId;
            String parentCrumb;
            String parentCrumbId;

            fullCrumbList = feedRecord.getFullCrumbList ();
            fullCrumbIdList = feedRecord.getFullCrumbIdList ();
            cumulativeCrumb = "";
            cumulativeCrumbId = "";

            for (int i = 0; i < fullCrumbList.length; i++) {
                if (i == 0) {
                    cumulativeCrumb = fullCrumbList [0];
                    cumulativeCrumbId = fullCrumbIdList [0];
                    parentCrumb = "";
                    parentCrumbId = "";
                } else {
                    cumulativeCrumb = cumulativeCrumb + CRUMBS_DELIMITER + fullCrumbList [i];
                    cumulativeCrumbId = cumulativeCrumbId + CRUMBS_ID_DELIMITER + fullCrumbIdList [i];
                    parentCrumb = fullCrumbList [i-1];
                    parentCrumbId = fullCrumbIdList [i-1];
                }

                // prepare a "crumb record" and store it in local crumb-to-id arrayList
                // if the cumulativeCrumb had not yet been entered in the list
                if (lookupCumulativeCrumbInMap (cumulativeCrumb) == null) {

                    CrumbToIdMap crumbToIdMap;

                    crumbToIdMap = new CrumbToIdMap (cumulativeCrumb, cumulativeCrumbId, 
                                                     fullCrumbList [i], fullCrumbIdList [i],  // leafCrumb, leafCrumbId
                                                     parentCrumb, parentCrumbId);

                    processedCrumbToIdMap.add (crumbToIdMap);
                }
            }
        }
    }

    private CrumbToIdMap lookupCumulativeCrumbInMap (String cumulativeCrumb) {
        for (CrumbToIdMap aMap : processedCrumbToIdMap) {
            if (aMap.getCumulativeCrumb ().equals (cumulativeCrumb) == true)
                return aMap;
        }

        return (null);
    }

    class FeedRecord {

        String productId = "";
        String[]  productPrices = {""}; // price is per-view
        String[]  views = {""};
        String[] fullCrumbList = {""}; // a | b | c
        String[] fullCrumbIdList = {""}; // 10 | 20 | 30

        FeedRecord (String prodId, String productPrices, String fullCrumb, String fullCrumbId, String viewIds) {
            // split pricelist and viewlist in construction itself
            this.productId = prodId;
            this.productPrices = productPrices.split  ("\\|");
            this.views = viewIds.split ("\\|");

            this.fullCrumbList = fullCrumb.split ("\\|");
            this.fullCrumbIdList = fullCrumbId.split ("\\|");
        }

        public String getProductId () {
            return this.productId;
        }

        public String getProductPrice (String viewId) {

            for (int i = 0; i < this.views.length; i++) {
                if (this.views [i].equals (viewId)) {
                    return (this.productPrices [i]);
                }
            }

            return (null);
        }

        public String[] getFullCrumbList () {
            return (this.fullCrumbList);
        }

        public String[] getFullCrumbIdList () {
            return (this.fullCrumbIdList);
        }
    }

    class CrumbToIdMap {

        String cumulativeCrumb;
        String cumulativeCrumbId;
        String leafCrumb;
        String leafCrumbId;
        String parentCrumb;
        String parentCrumbId;

        CrumbToIdMap (String cumulativeCrumb, String cumulativeCrumbId, String leafCrumb, String leafCrumbId,
                     String parentCrumb, String parentCrumbId) {

            this.cumulativeCrumb = cumulativeCrumb;
            this.cumulativeCrumbId = cumulativeCrumbId;
            this.leafCrumb = leafCrumb;
            this.leafCrumbId = leafCrumbId;
            this.parentCrumb = parentCrumb;
            this.parentCrumbId = parentCrumbId;
        }

        public String getCumulativeCrumb () {
            return this.cumulativeCrumb;
        }

        public String getCumulativeCrumbId () {
            return this.cumulativeCrumbId;
        }

        public String getLeafCrumb () {
            return this.leafCrumb;
        }

        public String getLeafCrumbId () {
            return this.leafCrumbId;
        }

        public String getParentCrumb () {
            return this.parentCrumb;
        }

        public String getParentCrumbId () {
            return this.parentCrumbId;
        }

    }
}

