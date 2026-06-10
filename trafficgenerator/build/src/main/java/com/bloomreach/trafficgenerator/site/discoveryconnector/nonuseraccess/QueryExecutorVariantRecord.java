package com.bloomreach.trafficgenerator.site.discoveryconnector.nonuseraccess;

public class QueryExecutorVariantRecord {

    private String skuid;
    private double skuPrice = 0.0; // optional in some feeds
    private double skuSalePrice = 0.0; // optional in some feeds

    public QueryExecutorVariantRecord () {
    }

    public void setSkuId (String skuid) {
        this.skuid = skuid;
    }

    public void setSkuPrice (double skuPrice) {
        this.skuPrice = skuPrice;
    }

    public void setSkuSalePrice (double skuSalePrice) {
        this.skuSalePrice = skuSalePrice;
    }

    public String getSkuId () {
        return this.skuid;
    }

    public double getSkuPrice () {
        return this.skuPrice;
    }

    public double getSkuSalePrice () {
        return this.skuSalePrice;
    }
}
