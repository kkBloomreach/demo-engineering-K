package com.bloomreach.trafficgenerator.site.feed;

public class ProductVariantRecord {

    private String skuId;
    private String skuPrice = null; // optional in some feeds
    private String skuSalePrice = null; // optional in some feeds

    public ProductVariantRecord () {
    }

    public void setSkuId (String skuid) {
        this.skuId = skuid;
    }

    public void setSkuPrice (String skuPrice) {
        this.skuPrice = skuPrice;
    }

    public void setSkuSalePrice (String skuSalePrice) {
        this.skuSalePrice = skuSalePrice;
    }

    public String getSkuId () {
        return this.skuId;
    }

    public String getSkuPrice () {
        return this.skuPrice;
    }

    public String getSkuSalePrice () {
        return this.skuSalePrice;
    }
}
