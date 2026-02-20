package com.bloomreach.brxdemos.pacifichome.translate.pixel.feed;

// -- for any leaf, holds its fullCrumb and its own leafCrumbId
public class FeedCrumbData {
    String fullCrumb; // A>B
    String leafCrumb; // B
    String leafCrumbId; // 20

    public FeedCrumbData (String fullCrumb, String leafCrumb, String leafCrumbId) {
        this.fullCrumb = fullCrumb;
        this.leafCrumb = leafCrumb;
        this.leafCrumbId = leafCrumbId;
    }

    public String getFullCrumb () {
        return this.fullCrumb;
    }

    public String getLeafCrumb () {
        return this.leafCrumb;
    }

    public String getLeafCrumbId () {
        return this.leafCrumbId;
    }
}

