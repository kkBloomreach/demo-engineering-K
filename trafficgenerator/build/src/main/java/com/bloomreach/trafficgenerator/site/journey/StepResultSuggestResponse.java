
package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.SuggestApiResponse;

public class StepResultSuggestResponse extends StepResult {

    private SuggestApiResponse suggestApiResponse;
    private String aqTerm;  // term used to make suggest API call. "aq" as specified in Suggest event pixel

    public StepResultSuggestResponse () {
        super ();
    }

    public void setSuggestApiResponse (SuggestApiResponse suggestApiResponse) {
        this.suggestApiResponse = suggestApiResponse;
    }

    public void setAqTerm (String aqTerm) {
        this.aqTerm = aqTerm;
    }

    public String getAqTerm () {
        return this.aqTerm;
    }

    @Override
    public SuggestApiResponse getData () {
        return this.suggestApiResponse;
    } 
}

