package com.bloomreach.trafficgenerator.site.journey;

public class StepResultInvalidData extends StepResult {

    private String message; // message about why stepResult is invalid

    public StepResultInvalidData () {
        super ();
    }

    public void setMessage (String msg) {
        this.message = msg;
    }

    @Override
    public String getData () {
        return this.message;
    } 
}

