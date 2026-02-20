package com.bloomreach.analyticsdatagenerator.generate;

public class SearchQueryResponseInfo {

    private int numFound;
    private SearchQueryResponseDoc[] queryResponseDocs;

    public SearchQueryResponseInfo (int numFound, SearchQueryResponseDoc[] queryResponseDocs) {
        this.numFound = numFound;
        this.queryResponseDocs = queryResponseDocs;
    }

    public int getNumFound () {
        return this.numFound;
    }

    public SearchQueryResponseDoc [] getQueryResponseDocs () {
        return this.queryResponseDocs;
    }

    // if a excluedPid is to be removed from API response, this method is called
    public void removeExcludedPidIfInResponse (String[] excludeProducts) {
        int delCount = 0;
        int newNumFound;
        SearchQueryResponseDoc [] newQueryResponseDocs;

        if ((this.numFound == 0) || (this.queryResponseDocs == null) || (this.queryResponseDocs.length == 0))
            return;

        for (int e = 0; e < excludeProducts.length; e++) { // "e" --> excludeList
            for (int q = 0; q < this.queryResponseDocs.length; q++) {   // "q" --> queryResponseDocs
                if (this.queryResponseDocs [q] != null) {   // may have been set to null earlier loop
                    if (excludeProducts [e].equals (this.queryResponseDocs [q].getPid()) == true) {
                        this.queryResponseDocs [q] = null;
                        delCount += 1;
                    }
                } 
            }
        }

        if (delCount == 0)
            return;

        newNumFound = this.numFound - delCount;
        newQueryResponseDocs = new SearchQueryResponseDoc [newNumFound];
        for (int d = 0, n = 0; d < this.queryResponseDocs.length; d++) {
            if (this.queryResponseDocs [d] != null) {
                newQueryResponseDocs [n] = this.queryResponseDocs [d];
                n += 1;
            }
        }

        this.numFound = newNumFound;
        this.queryResponseDocs = newQueryResponseDocs; 
    }
}
 
