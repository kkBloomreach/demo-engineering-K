package com.bloomreach.analyticsdatagenerator.input;

import java.util.ArrayList;
import com.bloomreach.analyticsdatagenerator.feed.ProcessedFeed;

public class GeneratorInputData {

    int acctId;
    String authKey;
    String domainKey;

    int uidCount = 0;
    String[] views = null;
    String[] zeroResultQueries = null;
    String[] excludeCategories = null;
    String[] excludeProducts = null;
    ArrayList<SearchQueryInfo> searchQueryInfoList;
    ArrayList<SegmentInfo> segmentInfoList;
    ArrayList<RefUrlInfo> refUrlInfoList;

    // feed is processed from the .jsonl and then set here for other modules to use
    ProcessedFeed processedFeed;
 
    public GeneratorInputData () {
        searchQueryInfoList = new ArrayList <SearchQueryInfo> ();
        segmentInfoList = new ArrayList <SegmentInfo> ();
        refUrlInfoList = new ArrayList <RefUrlInfo> ();
    }

    public void setProcessedFeed (ProcessedFeed processedFeed) {
        this.processedFeed = processedFeed;
    }

    public ProcessedFeed getProcessedFeed () {
        return this.processedFeed;
    }

    public void setAcctId (int acctId) {
        this.acctId = acctId;
    }

    public int getAcctId () {
        return this.acctId;
    }

    public void setAuthKey (String authKey) {
        this.authKey = authKey;
    }

    public String getAuthKey () {
        return this.authKey;
    }

    public void setDomainKey (String domainKey) {
        this.domainKey = domainKey;
    }

    public String getDomainKey () {
        return this.domainKey;
    }

    public void setUidCount (int uidCount) {
        this.uidCount = uidCount;
    }

    public int getUidCount () {
        return this.uidCount;
    }

    public void setViews (String[] views) {
        this.views = views;
    }

    public boolean hasViews () {
        if ((this.views == null) || (this.views.length == 0))
            return false;

        if ((this.views.length == 1) && (this.views [0].equals ("NONE")))
            return false;

        return true;
    }

    public String[] getViews () {
        return this.views;
    }

    public void setZeroResultQueries (String[] zrqList) {
        this.zeroResultQueries = zrqList;
    }

    public String[] getZeroResultQueryList () {
        return this.zeroResultQueries;
    }

    public void setSearchQueryInfo (String query, String[] refinedQueries) {
        SearchQueryInfo searchQueryInfo;

        searchQueryInfo = new SearchQueryInfo (query, refinedQueries);
        searchQueryInfoList.add (searchQueryInfo); 
    }

    public String[] getPrimarySearchQueries () {
        String[] primaryQueries;

        primaryQueries = new String [this.searchQueryInfoList.size()];
        for (int i = 0; i < this.searchQueryInfoList.size(); i++) {
            primaryQueries [i] = this.searchQueryInfoList.get(i).getPrimarySearchQuery ();
        }

        return (primaryQueries);
    }

    public String[] getRefinedSearchQueries (String primarySearchQuery) {
        for (int i = 0; i < this.searchQueryInfoList.size(); i++) {
            if (this.searchQueryInfoList.get(i).getPrimarySearchQuery ().equals (primarySearchQuery))
                return (this.searchQueryInfoList.get(i).getRefinedSearchQueries ());
        }
        return null;    // internal error
    }

    public void setSegmentInfo (String segment, String segmentFq) {
        SegmentInfo segmentInfo;

        segmentInfo = new SegmentInfo (segment, segmentFq);
        segmentInfoList.add (segmentInfo); 
    }

    public String[] getSegments () {
        String[] segments;

        segments = new String [this.segmentInfoList.size()];
        for (int i = 0; i < this.segmentInfoList.size(); i++) {
            segments [i] = this.segmentInfoList.get(i).getSegment ();
        }

        return segments;
    }

    public String getSegmentFq (String segment) {
        for (int i = 0; i < this.segmentInfoList.size(); i++) {
            if (this.segmentInfoList.get(i).getSegment ().equals (segment))
                return (this.segmentInfoList.get(i).getSegmentFq ());
        }
        return null;    // internal error
    }

    public void setRefUrlInfo (String refType, String refValue) {
        RefUrlInfo refUrlInfo;

        refUrlInfo = new RefUrlInfo (refType, refValue);
        refUrlInfoList.add (refUrlInfo); 
    }

    public String[] getRefUrlTypes () {
        String[] refTypes;

        refTypes = new String [this.refUrlInfoList.size()];
        for (int i = 0; i < this.refUrlInfoList.size(); i++) {
            refTypes [i] = this.refUrlInfoList.get(i).getRefType ();
        }

        return refTypes;
    }

    public String getRefUrlValue (String refType) {
        for (int i = 0; i < this.refUrlInfoList.size(); i++) {
            if (this.refUrlInfoList.get(i).getRefType ().equals (refType))
                return (this.refUrlInfoList.get(i).getRefValue ());
        }
        return null;    // internal error
    }

    public void setExcludeCategories (String[] excludeCategories) {
        this.excludeCategories = excludeCategories;
    }

    public String[] getExcludeCategories () {
        return this.excludeCategories;
    }

    public String[] getExcludeProducts () {
        return this.excludeProducts;
    }

    public void setExcludeProducts (String[] excludeProducts) {
        this.excludeProducts = excludeProducts;
    }

    ///////////////
    class SearchQueryInfo {
        private String primarySearchQuery;
        private String[] refinedSearchQueries;

        public SearchQueryInfo (String primarySearchQuery, String[] refinedSearchQueries) {
            this.primarySearchQuery = primarySearchQuery;
            this.refinedSearchQueries = refinedSearchQueries;
        }

        public String getPrimarySearchQuery () {
            return this.primarySearchQuery;
        }

        public String[] getRefinedSearchQueries () {
            return this.refinedSearchQueries;
        }
    }

    ///////////////
    class SegmentInfo {
        private String segment;
        private String segmentFq;   // fq to use for this segment

        public SegmentInfo (String segment, String segmentFq) {
            this.segment = segment;
            this.segmentFq = segmentFq;
        }

        public String getSegment () {
            return this.segment;
        }

        public String getSegmentFq () {
            return this.segmentFq;
        }
    }

    ///////////////
    class RefUrlInfo {
        private String refType; // "home"
        private String refValue; // typically "/" 

        public RefUrlInfo (String refType, String refValue) {
            this.refType = refType;
            this.refValue = refValue;
        }

        public String getRefType () {
            return this.refType;
        }

        public String getRefValue () {
            return this.refValue;
        }
    }
}
