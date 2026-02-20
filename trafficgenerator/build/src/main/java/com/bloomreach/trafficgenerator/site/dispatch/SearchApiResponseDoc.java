package com.bloomreach.trafficgenerator.site.dispatch;

public class SearchApiResponseDoc {

    private String pid;
    private String url;
    private double price;
    private double salePrice;
    private String title;
    private String skuid;   // one of the many sku's selected at random

    public SearchApiResponseDoc () {
    }

    public void setPid (String pid) {
        this.pid = pid;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public void setPrice (double price) {
        this.price = price;
    }

    public void setSalePrice (double salePrice) {
        this.salePrice = salePrice;
    }

    public void setTitle (String title) {
        this.title = title;
    }

    public void setSkuid (String skuid) {
        this.skuid = skuid;
    }

    public String getPid () {
        return this.pid;
    } 

    public String getUrl () {
        return this.url;
    } 

    public double getPrice () {
        return this.price;
    } 

    public double getSalePrice () {
        return this.salePrice;
    } 

    public String getTitle () {
        return this.title;
    } 

    public String getSkuid () {
        return this.skuid;
    } 
}
