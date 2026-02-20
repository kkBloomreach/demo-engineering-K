package com.bloomreach.trafficgenerator.visitor;

import com.bloomreach.trafficgenerator.GeneratorConstants;

public class Visitor  {

    private String visitorId;
    private long arrivalTime;
    private int  deviceType = GeneratorConstants.DEVICE_TYPE_UNKNOWN;

    protected Visitor (String visitorId, long arrivalTime, int deviceType) {
        this.visitorId = visitorId;
        this.arrivalTime = arrivalTime;
        this.deviceType = deviceType;   //mobile / tablet / desktop
    }

    public String getVisitorId () {
        return this.visitorId;
    }

    public long getArrivalTime () {
        return this.arrivalTime;
    }

    public int getDeviceType () {
        return this.deviceType;
    }
}
