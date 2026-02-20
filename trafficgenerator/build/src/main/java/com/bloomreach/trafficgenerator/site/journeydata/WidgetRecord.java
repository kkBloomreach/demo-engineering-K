package com.bloomreach.trafficgenerator.site.journeydata;

public class WidgetRecord {
    private String widgetCode;  // internally used for case stmt
    private String wid;
    private String catId  = null;       // only for pathway-category widget
    private String itemId = null;      // only for specific widgets
    private String query  = null;      // only for specific widgets

    public WidgetRecord (String widgetCode, String wid) {
        this.widgetCode = widgetCode;
        this.wid = wid;
    }

    public void setCatId (String catId) {
        this.catId = catId;
    }

    public void setItemId (String itemId) {
        this.itemId = itemId;
    }

    public void setQuery (String query) {
        this.query = query;
    }

    public String getWidgetCode () {
        return this.widgetCode;
    }

    public String getWid () {
        return this.wid;
    }

    public String getCatId () {
        return this.catId;
    }
    
    public String getItemId () {
        return this.itemId;
    }

    public String getQuery () {
        return this.query;
    }

}
