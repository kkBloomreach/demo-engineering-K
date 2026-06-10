package com.bloomreach.trafficgenerator.site.feed;

public class ProductViewRecord {

    private String viewid;
    private String viewPrice = null; // optional
    private String viewSalePrice = null; // optional

    public ProductViewRecord () {
    }

    public void setViewId (String viewid) {
        this.viewid = viewid;
    }

    public void setViewPrice (String viewPrice) {
        this.viewPrice = viewPrice;
    }

    public void setViewSalePrice (String viewSalePrice) {
        this.viewSalePrice = viewSalePrice;
    }

    public String getViewId () {
        return this.viewid;
    }

    public String getViewPrice () {
        return this.viewPrice;  // may be null
    }

    public String getViewSalePrice () {
        return this.viewSalePrice;  // may be null
    }
}
