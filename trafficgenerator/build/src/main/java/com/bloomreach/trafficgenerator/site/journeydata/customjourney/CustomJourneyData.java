package com.bloomreach.trafficgenerator.site.journeydata.customjourney;

public abstract class CustomJourneyData {
    private String customJourneyType;

    // specific 'target' for this custom journey (eg, catId for lowPerfCategory journey)
    private String customJourneyTarget; 

    protected CustomJourneyData (String journeyType, String customJourneyTarget) {
        this.customJourneyType = journeyType;
        this.customJourneyTarget = customJourneyTarget;
    }

    public String getCustomJourneyType () {
        return this.customJourneyType;
    }

    public String getCustomJourneyTarget () {
        return this.customJourneyTarget;
    }
}
