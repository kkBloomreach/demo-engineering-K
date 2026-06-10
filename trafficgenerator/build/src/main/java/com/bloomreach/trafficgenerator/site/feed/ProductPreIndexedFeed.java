// for accounts indexed via datahub, we need to use currently indexed data
// to construct a productfeed. This is because we don't have a non-VPN access
// to that feed file. Also, datahub automatically indexes the catalog (via discovery)
// therefore TrafficGenerator should not do that 
package com.bloomreach.trafficgenerator.site.feed;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.discoveryconnector.nonuseraccess.*;

public class ProductPreIndexedFeed extends ProductFeed {

    private String realm;   // staging / production

    public ProductPreIndexedFeed () {
    }

    public void setRealm (String realm) {
        this.realm = realm;
    }

    // override base class method
    public void load (String domainKey) throws Exception {
        ArrayList<FeedRecord> indexedFeedRecordList;

        MessageLogger.logDebug (" > Start constructing preIndexed feed....: " + domainKey);
        indexedFeedRecordList = collectCatalog ();
        MessageLogger.logDebug (" ............................... Done constructing preIndexed feed.");

        // set in base class
        super.setParsedFeedRecordList (indexedFeedRecordList);
    }

    private ArrayList <FeedRecord> collectCatalog () throws Exception {
        QueryExecutorSearchApiResponse totalSearchApiResponse;
        ArrayList<QueryExecutorSearchApiResponseDoc> responseDocs;
        ArrayList <FeedRecord> indexedFeedRecordList;

        totalSearchApiResponse = collectAllProductsBySearch ();
        responseDocs = totalSearchApiResponse.getResponseDocs ();
        indexedFeedRecordList = new ArrayList <FeedRecord> ();

        for (QueryExecutorSearchApiResponseDoc responseDoc : responseDocs) {
            FeedRecord indexedFeedRecord;

            indexedFeedRecord = constructFeedRecord (responseDoc);
            if (indexedFeedRecord != null) 
                indexedFeedRecordList.add (indexedFeedRecord);            
        }

        return indexedFeedRecordList; 
    }

    private QueryExecutorSearchApiResponse collectAllProductsBySearch () throws Exception {
        SearchQueryExecutor searchQueryExecutor;
        QueryExecutorSearchApiResponse totalSearchApiResponse;
        String queryTerm;
        String fqParam = null;
        int start = 0;
        int maxRows = 100;

        queryTerm = "*";    // star query. Acct must be enabled for star-query via HT
        searchQueryExecutor = new SearchQueryExecutor ();
        searchQueryExecutor.setRealm (this.realm);
        
        totalSearchApiResponse = searchQueryExecutor.getSearchResponse (queryTerm, fqParam, start, maxRows);
        if (totalSearchApiResponse != null) {
            int numFound;
            int remCount;
            int docCount;

            numFound = totalSearchApiResponse.getNumFound ();
            docCount = totalSearchApiResponse.getResponseDocs().size ();    // docs collected in first apicall above
            start = start + docCount;
            remCount = numFound - docCount;

            while (remCount > 0) {
                QueryExecutorSearchApiResponse remSearchApiResponse;

                remSearchApiResponse = searchQueryExecutor.getSearchResponse (queryTerm, start, maxRows);
                totalSearchApiResponse.addResponseDocs (remSearchApiResponse.getResponseDocs());    // accumulate response docs
                remCount = remCount - remSearchApiResponse.getResponseDocs().size();
                start = start + remSearchApiResponse.getResponseDocs().size();
            }
        }

        return totalSearchApiResponse;
    }

    private FeedRecord constructFeedRecord (QueryExecutorSearchApiResponseDoc responseDoc) {
        FeedRecord feedRecord;
        ArrayList <QueryExecutorVariantRecord> qeProductVariants; // qe -- queryExecutor
        ArrayList <ProductVariantRecord> productVariants = null;

        feedRecord = new FeedRecord ();
        feedRecord.setProductId (responseDoc.getPid ());
        feedRecord.setProductName (responseDoc.getTitle ());
        feedRecord.setProductPrice (Double.toString (responseDoc.getPrice ()));
        feedRecord.setProductSalePrice (Double.toString (responseDoc.getSalePrice ()));

        feedRecord.setProductAvailability (responseDoc.getAvailability ());
        feedRecord.setProductViews (null); // view info not available via QueryExec API response
        feedRecord.setProductUrl (responseDoc.getUrl ());
        feedRecord.setProductStyle (responseDoc.getStyle ());  // MAY be available for some catalogs

        // if product has variants
        qeProductVariants = responseDoc.getVariants ();
        if ((qeProductVariants != null) && (qeProductVariants.size () > 0)) { 
            productVariants = new ArrayList <ProductVariantRecord> ();

            // map QE-variant-record to product-variant-record
            for (QueryExecutorVariantRecord qeRecord : qeProductVariants) {
                // map qe object to product-variant object
                ProductVariantRecord productVariantRecord;

                productVariantRecord = new ProductVariantRecord ();
                productVariantRecord.setSkuId (qeRecord.getSkuId ());
                productVariantRecord.setSkuPrice (Double.toString (qeRecord.getSkuPrice ()));
                productVariantRecord.setSkuSalePrice (Double.toString (qeRecord.getSkuSalePrice ()));

                productVariants.add (productVariantRecord);
            }

            // update feed record
            feedRecord.setProductVariants (productVariants);
        }

        return feedRecord;
    }
}


