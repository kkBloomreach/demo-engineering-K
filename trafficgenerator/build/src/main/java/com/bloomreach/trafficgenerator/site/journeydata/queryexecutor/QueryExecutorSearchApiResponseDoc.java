// this class is same as the one in Dispatch. We keep it separate just for convenience in queryexecutor
package com.bloomreach.trafficgenerator.site.journeydata.queryexecutor;

public class QueryExecutorSearchApiResponseDoc {

    private String pid;
    private String url;
    private double price;
    private double salePrice;
    private String title;
    private String skuid;   // one of the many sku's selected at tandom

    public QueryExecutorSearchApiResponseDoc () {
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
