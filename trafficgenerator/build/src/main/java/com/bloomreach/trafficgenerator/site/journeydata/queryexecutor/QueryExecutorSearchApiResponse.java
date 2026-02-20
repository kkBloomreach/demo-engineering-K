// this class contains categoryInfo collected from facets in a "*" search query
// The similar class in Dispatch does not include categoryInfoList since it is not needed there
// This class is referenced in Campaigns as well as CategoryCollector 
package com.bloomreach.trafficgenerator.site.journeydata.queryexecutor;

import java.util.ArrayList;

public class QueryExecutorSearchApiResponse {

    private int numFound;
    private ArrayList <QueryExecutorSearchApiResponseDoc> responseDocs;
    private ArrayList <CategoryInfo> categoryList;

    public QueryExecutorSearchApiResponse () {
    }

    public void setNumFound (int numFound) {
        this.numFound = numFound;
    }

    public void addResponseDocs (ArrayList<QueryExecutorSearchApiResponseDoc> responseDocs) {
        if (this.responseDocs == null)
            this.responseDocs = new ArrayList <QueryExecutorSearchApiResponseDoc> ();
        this.responseDocs.addAll (responseDocs);
    }

    public void setResponseCategories (ArrayList <CategoryInfo> categoryList) {
        this.categoryList = categoryList;
    }

    public int getNumFound () {
        return this.numFound;
    } 

    public ArrayList <QueryExecutorSearchApiResponseDoc> getResponseDocs () {
        return this.responseDocs;
    }

    public ArrayList <CategoryInfo> getCategoryList () {
        return this.categoryList;
    }

    protected void removeExcludedProducts (ArrayList<String> excludeProducts) {
        for (String excludePid : excludeProducts) {
            QueryExecutorSearchApiResponseDoc excludeDoc;

            excludeDoc = lookupResponseDocToRemove (excludePid);
            if (excludeDoc != null) {
                this.responseDocs.remove (excludeDoc);
                this.numFound = this.numFound - 1;
            }
        }
    } 

    private QueryExecutorSearchApiResponseDoc lookupResponseDocToRemove (String excludePid) {
        for (QueryExecutorSearchApiResponseDoc responseDoc : this.responseDocs) {
            if (responseDoc.getPid ().equals (excludePid) == true) {
                return responseDoc;
            }
        }

        return null;
    }
}
