package com.bloomreach.trafficgenerator.site.feed;

import java.util.ArrayList;

public abstract class ProductFeed {

    private ArrayList <FeedRecord> parsedFeedRecordList;
    private ArrayList <FeedRecord> availableProductRecordList;

    // to be implemented by derived class
    public abstract void load (String productFilePath) throws Exception;

    // to be called by derived class upon successful parse
    // include both available and not available records
    protected void setParsedFeedRecordList (ArrayList<FeedRecord> parsedRecordList) {
        this.parsedFeedRecordList = parsedRecordList;
        this.availableProductRecordList = collectAvailableProductRecords ();
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

    public String lookupProductSkuId (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (aRecord.getProductSkuId ()); 
            }
        }

        return (null);
    }

    public String lookupProductStyle (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (aRecord.getProductStyle ()); // may be null or blank 
            }
        }

        return (null);
    }

    public FeedRecord lookupProductRecord (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (aRecord);
            }
        }

        return (null);
    }

    public ArrayList <FeedRecord> listAvailableProducts () {
        return (this.availableProductRecordList);
    }
   

    // INTERNAL METHODS
    private ArrayList <FeedRecord> collectAvailableProductRecords () {
        ArrayList <FeedRecord> availableProducts;

        availableProducts = new ArrayList <FeedRecord> ();
        for (FeedRecord record : parsedFeedRecordList) {
            if (record.isAvailable () == true)
                availableProducts.add (record);
        }

        return availableProducts;
    }

}

