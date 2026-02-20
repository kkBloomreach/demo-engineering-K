package com.bloomreach.trafficgenerator.site.journeydata.campaigns;

import java.util.ArrayList;

public class CampaignRecord {
    private String campaignName;
    private int    startMonth;
    private int    startDay;
    private int    dayCount;
    private float  priceDiscount;
    private ArrayList <String> productList; // list of pid values
    private ArrayList <String> searchTerms;

    public CampaignRecord (String campaign, int startMonth, int startDay, int dayCount, float priceDiscount, 
                            ArrayList<String> productList, ArrayList<String> searchTerms) {
        this.campaignName = campaign;
        this.startMonth = startMonth;
        this.startDay = startDay;
        this.dayCount = dayCount;
        this.priceDiscount = priceDiscount;
        this.productList = productList;
        this.searchTerms = searchTerms;
    }

    public int getStartMonth () {
        return this.startMonth;
    }

    public int getStartDay () {
        return this.startDay;
    }

    public int getDayCount () {
        return this.dayCount;
    }

    public String getCampaignName () {
        return this.campaignName;
    }

    public float getPriceDiscount () {
        return this.priceDiscount;
    }

    public ArrayList<String> getProductList () {
        return this.productList;
    }

    public ArrayList <String> getSearchTerms () {
        return this.searchTerms;
    }
}
