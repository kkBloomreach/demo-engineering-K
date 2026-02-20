package com.bloomreach.trafficgenerator.visitor.slicemanager;

// Manages timeslices in a day
// Quantifies traffic 'load' for each timeslice in a day (low/medium/high)

import java.util.GregorianCalendar;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.EnvironmentConfig;

public class SliceManager {

    private GregorianCalendar calendar;
    private long duration;  // total time to be divided into individual timeslices
    private Timeslice[] timeslices;

    public SliceManager () {
    }

    public void setTimespan (GregorianCalendar calendar, long duration) {
        this.calendar = calendar;
        this.duration = duration;
    }

    public void init () throws Exception {
        long millisInTimeslice;
        int maxSlices;
        int availableSlices;
        long calendarStartTime;
        long virtualStartTime;  // subtract calendarStartTime from calendar.getTime()

        // duration in millisecs 
        millisInTimeslice = EnvironmentConfig.getEnvParamLong ("TRAFFIC_SLICE_DURATION");
        maxSlices = (int) (this.duration / millisInTimeslice);
        availableSlices = maxSlices - 2;    // last two slices not used for traffic
        if (availableSlices == 0) {
            throw new Exception ("There are zero available slices");
        }

        this.timeslices = new Timeslice [availableSlices];
        calendarStartTime = this.calendar.getTimeInMillis ();
        virtualStartTime = 0; // calendarStartTime - calendarStartTime
        for (int i = 0; i < this.timeslices.length; i++) {
            Timeslice timeslice;
            int trafficState;
            long calendarEndTime;
            long virtualEndTime;
 
            trafficState = evalTrafficState (virtualStartTime); 
            calendarEndTime = calendarStartTime + millisInTimeslice;
            virtualEndTime = virtualStartTime + millisInTimeslice;
            timeslice = new Timeslice (i, virtualStartTime, virtualEndTime, calendarStartTime, calendarEndTime, trafficState);
            this.timeslices [i] = timeslice; 

            virtualStartTime = virtualEndTime;
        }
    }

    public int getMaxTimeslices () {
        return this.timeslices.length;
    }

    public Timeslice getTimeslice (int sliceNum) {
        return this.timeslices [sliceNum];
    }

    // traffic state
    // block0: 0 -> 7hr = low
    // block1: 7 -> 10hr = medium
    // block2: 10 -> 21hr = high
    // block3: 21 -> 24hr = low
    private int evalTrafficState (long virtualStartTime) {
        long block0Start = 0;
        long block1Start = 7 * 60 * 60 * 1000;
        long block2Start = 10 * 60 * 60 * 1000;
        long block3Start = 21 * 60 * 60 * 1000;

        if ((virtualStartTime >= block0Start) && (virtualStartTime < block1Start))
            return GeneratorConstants.TRAFFIC_STATE_LOW;
        else if ((virtualStartTime >= block1Start) && (virtualStartTime < block2Start))
            return GeneratorConstants.TRAFFIC_STATE_MEDIUM;
        else if ((virtualStartTime >= block2Start) && (virtualStartTime < block3Start))
            return GeneratorConstants.TRAFFIC_STATE_HIGH;
        else
            return GeneratorConstants.TRAFFIC_STATE_LOW;
    }
}
