package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.journeydata.customjourney.CustomJourneyData;

public class CustomJourneyException extends RuntimeException {
    private CustomJourneyData journeyData;

    public CustomJourneyException (CustomJourneyData journeyData) {
        super ("Handle custom journey: " + journeyData.getCustomJourneyType());
        this.journeyData = journeyData;
    }

    public CustomJourneyData getCustomJourneyData () {
        return this.journeyData;
    }
}
