package com.bloomreach.trafficgenerator.site.user;


public class UserRecord {

    String visitorId;
    String segment; // aka 'profile' in RBS world
    String view;
    String userId; // different from visitorId
    int deviceType;   // use corresponding userAgent in pixel,api calls

    public UserRecord () {
        this.visitorId = null;
        this.segment = "NONE";
        this.view = "NONE";
        this.userId = "NONE";
        this.deviceType = -1;
    }

    // note - param type is String
    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public void setSegment (String segment) {
        this.segment = segment;
    }

    public void setView (String view) {
        this.view = view;
    }

    public void setUserId (String userId) {
        this.userId = userId;
    }

    public void setDeviceType (int deviceType) {
        this.deviceType = deviceType;
    }

    public String getVisitorId () {
        return this.visitorId;
    }

    public String getView () {
        return this.view;
    }

    public String getSegment () {
        return this.segment;
    }

    public String getUserId () {
        return this.userId;
    }

    public int getDeviceType () {
        return this.deviceType;
    }
}

