package com.bloomreach.trafficgenerator.site.journeydata;

import java.util.ArrayList;

public class SearchTermWithRefinements {
    private String primary;
    private ArrayList<String> refinements;
    private int importance; // 1 -> 5

    public SearchTermWithRefinements (String primary, int importance, ArrayList<String> refinements) {
        this.primary = primary;
        this.refinements = refinements;
        this.importance = importance;
    }

    public String getPrimary () {
        return this.primary;
    }

    public ArrayList<String> getRefinements () {
        return this.refinements;
    }

    public int getImportance () {
        return this.importance;
    }
}

/***
//     public void setPrimary (String primary) {
//         this.primary = primary;
//     }
// 
//     public void setRefinements (ArrayList<String> refinements) {
//         this.refinements = refinements;
//     }
// 
//     public void setImportance (int importance) {
//         this.importance = importance;
//     }
***/

