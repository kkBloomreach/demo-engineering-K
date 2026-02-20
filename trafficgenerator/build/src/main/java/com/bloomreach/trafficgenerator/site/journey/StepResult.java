package com.bloomreach.trafficgenerator.site.journey;

public abstract class StepResult {

    private String url;
    private String refUrl;
    private long endTime;

    public StepResult () {
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public String getUrl () {
        return this.url;
    }

    public void setRefUrl (String refUrl) {
        this.refUrl = refUrl;
    }

    public String getRefUrl () {
        return this.refUrl;
    }

    public long getEndTime () {
        return this.endTime;
    }

    protected void setEndTime (long endTime)  {
        this.endTime = endTime;
    }

    protected abstract Object getData (); 
}


