package com.bloomreach.trafficgenerator.site.journeydata;

import java.util.ArrayList;

public class CuratedSearchTermDetails {

    private String initialQuery;
    private String refinedQuery;
    private int score;
    private ArrayList<String> pidList;

    public CuratedSearchTermDetails (String initialQuery, String refinedQuery, int score, ArrayList<String> pidList) {
        this.initialQuery = initialQuery;
        this.refinedQuery = refinedQuery;
        this.score = score;
        this.pidList = pidList; // not dup'd
    }

    public String getInitialQuery () {
        return this.initialQuery;
    }

    public String getRefinedQuery () {
        return this.refinedQuery;
    }

    public int getScore () {
        return this.score;
    }

    public ArrayList<String> getPidList () {
        return this.pidList;
    }
}
