package com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess;

import java.util.ArrayList;
import org.json.JSONObject;

// class used only within this package
public class ParsedQueryApiResponse {

    private int numFound;
    private ArrayList <SearchApiResponseDoc> responseDocs;
    private JSONObject responseMetadataJson;    // has different contents for different types of query responses

    public ParsedQueryApiResponse () {
    }

    public void setNumFound (int numFound) {
        this.numFound = numFound;
    }

    public void addResponseDocs (ArrayList<SearchApiResponseDoc> responseDocs) {
        if (this.responseDocs == null)
            this.responseDocs = new ArrayList <SearchApiResponseDoc> ();
        this.responseDocs.addAll  (responseDocs);
    }

    public void setResponseMetadataJson (JSONObject responseMetadataJson) {
        this.responseMetadataJson = responseMetadataJson;
    }

    public int getNumFound () {
        return this.numFound;
    } 

    public ArrayList <SearchApiResponseDoc> getResponseDocs () {
        return this.responseDocs;
    }

    public JSONObject getResponseMetadataJson () {
        return this.responseMetadataJson;
    }
}
