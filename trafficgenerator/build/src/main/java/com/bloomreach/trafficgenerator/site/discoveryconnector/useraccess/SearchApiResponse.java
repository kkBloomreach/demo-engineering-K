package com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess;

import java.util.ArrayList;

public class SearchApiResponse {

    private int numFound;
    private ArrayList <SearchApiResponseDoc> responseDocs;
    private SearchResponseMetadata searchResponseMetadata;

    public SearchApiResponse () {
    }

    public void setNumFound (int numFound) {
        this.numFound = numFound;
    }

    public void addResponseDocs (ArrayList<SearchApiResponseDoc> responseDocs) {
        if (this.responseDocs == null)
            this.responseDocs = new ArrayList <SearchApiResponseDoc> ();
        this.responseDocs.addAll  (responseDocs);
    }

    public void setSearchResponseMetadata (SearchResponseMetadata responseMetadata) {
        this.searchResponseMetadata = responseMetadata;
    }

    public int getNumFound () {
        return this.numFound;
    } 

    public ArrayList <SearchApiResponseDoc> getResponseDocs () {
        return this.responseDocs;
    }

    public SearchResponseMetadata getResponseMetadata () {
        return this.searchResponseMetadata;
    }

    protected void removeExcludedProducts (ArrayList<String> excludeProducts) {
        for (String excludePid : excludeProducts) {
            SearchApiResponseDoc excludeDoc;

            excludeDoc = lookupResponseDocToRemove (excludePid);
            if (excludeDoc != null) {
                this.responseDocs.remove (excludeDoc);
                this.numFound = this.numFound - 1;
            }
        }
    } 

    private SearchApiResponseDoc lookupResponseDocToRemove (String excludePid) {
        for (SearchApiResponseDoc responseDoc : this.responseDocs) {
            if (responseDoc.getPid ().equals (excludePid) == true) {
                return responseDoc;
            }
        }

        return null;
    }
}
