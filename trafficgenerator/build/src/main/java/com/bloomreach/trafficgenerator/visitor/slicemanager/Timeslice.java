package com.bloomreach.trafficgenerator.visitor.slicemanager;

public class Timeslice {

    private int  sliceNum;
    private long calendarStart;
    private long calendarEnd;
    private long virtualStart;
    private long virtualEnd;
    private int trafficState;

    public Timeslice (int sliceNum, long virtualStart, long virtualEnd, long calendarStart, long calendarEnd, int trafficState) {
        this.sliceNum = sliceNum;
        this.calendarStart = calendarStart;
        this.calendarEnd = calendarEnd;
        this.virtualStart = virtualStart;
        this.virtualEnd = virtualEnd;
        this.trafficState = trafficState;
    }

    public int getSliceNum () {
        return sliceNum;
    } 

    public long getCalendarStart () {
        return calendarStart;
    } 

    public long getCalendarEnd () {
        return calendarEnd;
    }

    public long getVirtualStart () {
        return virtualStart;
    } 

    public long getVirtualEnd () {
        return virtualEnd;
    }

    public int getTrafficState () {
        return trafficState;
    }

    public String toString () {
        String res = String.format ("num = %d, virtualStart = %d, virtualEnd = %d, calendarStart = %d, calendarEnd = %d, trafficStat = %s", 
                                    sliceNum, virtualStart, virtualEnd, calendarStart, calendarEnd, trafficState);
        return res;
    } 
}
