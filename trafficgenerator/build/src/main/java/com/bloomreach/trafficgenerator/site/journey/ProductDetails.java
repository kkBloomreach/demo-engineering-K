package com.bloomreach.trafficgenerator.site.journey;

public class ProductDetails {

    private String pid;
    private String url;
    private double price;
    private double salePrice;
    private String title;
    private String skuid;

    public ProductDetails () {
    }

    public void setPid (String pid) {
        this.pid = pid;
    }

    public String getPid () {
        return this.pid;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public String getUrl () {
        return this.url;
    }

    public void setPrice (double price) {
        this.price = price;
    }

    public double getPrice () {
        return this.price;
    }

    public void setSalePrice (double salePrice) {
        this.salePrice = salePrice;
    }

    public double getSalePrice () {
        return this.salePrice;
    }

    public void setTitle (String title) {
        this.title = title;
    }

    public String getTitle () {
        return this.title;
    }

    public void setSkuid (String skuid) {
        this.skuid = skuid;
    }

    public String getSkuid () {
        return this.skuid;
    }
}

