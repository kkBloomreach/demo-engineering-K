package com.bloomreach.trafficgenerator.site.journeydata.customjourney;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.GeneratorConstants;

public class LPCCustomJourneyData extends CustomJourneyData {
    private String catId;
    private int bounceRate;
    private ArrayList<String> selectSearchTerms;

    public LPCCustomJourneyData (String catId, int bounceRate, ArrayList<String> searchTerms) {
        super (GeneratorConstants.CUSTOM_JOURNEY_TYPE_LPC, catId);
        this.catId = catId;
        this.bounceRate = bounceRate;
        this.selectSearchTerms = searchTerms;
    }

    public String getCatId () {
        return this.catId;
    }

    public int getBounceRate () {
        return this.bounceRate;
    }

    public ArrayList<String> getSelectSearchTerms () {
        return this.selectSearchTerms;
    }
}
