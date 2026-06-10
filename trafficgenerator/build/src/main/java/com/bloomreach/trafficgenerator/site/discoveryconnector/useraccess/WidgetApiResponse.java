package com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess;

import java.util.ArrayList;

// widget api response same as searchApi response except metadata has different content
public class WidgetApiResponse {

    private int numFound;
    private ArrayList <SearchApiResponseDoc> responseDocs;
    private WidgetResponseMetadata widgetResponseMetadata;

    public WidgetApiResponse () {
    }

    public void setNumFound (int numFound) {
        this.numFound = numFound;
    }

    public void addResponseDocs (ArrayList<SearchApiResponseDoc> responseDocs) {
        if (this.responseDocs == null)
            this.responseDocs = new ArrayList <SearchApiResponseDoc> ();
        this.responseDocs.addAll  (responseDocs);
    }

    public void setWidgetResponseMetadata (WidgetResponseMetadata responseMetadata) {
        this.widgetResponseMetadata = responseMetadata;
    }

    public int getNumFound () {
        return this.numFound;
    } 

    public ArrayList <SearchApiResponseDoc> getResponseDocs () {
        return this.responseDocs;
    }

    public WidgetResponseMetadata getWidgetResponseMetadata () {
        return this.widgetResponseMetadata;
    }
}

