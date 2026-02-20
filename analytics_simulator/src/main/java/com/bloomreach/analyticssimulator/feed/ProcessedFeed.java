package com.bloomreach.analyticssimulator.feed;

import java.util.ArrayList;

public abstract class ProcessedFeed {

    private ArrayList <FeedRecord> parsedFeedRecordList;

    // to be implemented by derived class
    public abstract void load (String productFilePath) throws Exception;

    // to be called by derived class upon successful parse
    protected void setParsedFeedRecordList (ArrayList<FeedRecord> parsedRecordList) {
        this.parsedFeedRecordList = parsedRecordList;
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
}


