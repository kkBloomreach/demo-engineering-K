package com.bloomreach.trafficgenerator.visitor;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.visitor.slicemanager.*;

public class VisitorCreatorThread extends Thread {

    // ensure dispatch frequency not too low - otherwise site handler may get overloaded and skip a visitor
    private final static int MIN_TIME_BETWEEN_VISITOR_DISPATCH = 5 * 1000;   //millisecs

    private VisitorSignal visitorSignal;
    private SliceManager sliceManager;
    private GregorianCalendar calendar;
    private String specialVisitorId = null;
    private ArrayList <Integer> specialVisitDays = null;
    private int totalVisitorsInDay = 0;
    private ArrayList<Long> activeVisitorIdsInADay;   // visitorIds already created
    private String activeVisitorIdTemplate; // constructed using POOL_SIZE value

    public VisitorCreatorThread () {
    }

    protected void setVisitorSignal (VisitorSignal visitorSignal) {
        this.visitorSignal = visitorSignal;
    }

    protected void setSliceManager (SliceManager sliceManager) {
        this.sliceManager = sliceManager;
    }

    protected void setCalendar (GregorianCalendar calendar) {
        this.calendar = calendar;
    }

    public void setSpecialVisitorData (String specialVisitorId, ArrayList<Integer> specialVisitDays) {
        this.specialVisitorId = specialVisitorId;
        this.specialVisitDays = specialVisitDays;
    }

    // single thread used for all timeslices in a day
    public void run () {

        activeVisitorIdsInADay = new ArrayList <Long>();   // visitorIds already created
        activeVisitorIdTemplate = prepareActiveVisitorIdTemplate ();    // build it once

        try {
            dispatchDailyVisitors (); // returns after all timeslices are exhausted
        } catch (InterruptedException e) {
            MessageLogger.logWarning ("VisitorCreator interrupted");
        } catch (Exception e) {
            MessageLogger.logError (String.format ("VisitorCreator exception: %s", e.getMessage()));
        }

        // terminate this thread (end-of-day)
        MessageLogger.logInfo (String.format ("Finish VisitorCreator thread, total visitors in day: %d", totalVisitorsInDay));
    }

    // build a 13-digit string (as per BR docs === 10*12). Use POOL_SIZE defined in Consts.java
    // eg, 5678123000000 (last N digits == POOL_SIZE string length)
    private String prepareActiveVisitorIdTemplate () {
        String fullVisitorIdTemplate;
        String maxPoolSizeStr;
        String actualVisitorIdTemplate;
        int templateHeadLen;
        String templateHead;
        int maxPoolSizeStrLen;
        StringBuilder templateBuilder;

        fullVisitorIdTemplate = GeneratorConstants.VISITOR_ID_TEMPLATE; // 13 digit long
        maxPoolSizeStr = String.format ("%s", GeneratorConstants.MAX_VISITOR_POOL_SIZE);
        maxPoolSizeStrLen = maxPoolSizeStr.length();

        templateHeadLen = fullVisitorIdTemplate.length() - maxPoolSizeStrLen;
        templateHead = fullVisitorIdTemplate.substring (0, templateHeadLen);

        templateBuilder = new StringBuilder (templateHead);
        for (int i = 0; i < maxPoolSizeStrLen; i++)
            templateBuilder.append ("0");
        actualVisitorIdTemplate = templateBuilder.toString ();

        return actualVisitorIdTemplate;
    }

    private void dispatchDailyVisitors () throws Exception {
        int maxTimeslices;

        maxTimeslices = this.sliceManager.getMaxTimeslices (); // all slices during single day
        for (int sliceNum = 0; sliceNum < maxTimeslices; sliceNum++) {
            Timeslice timeslice;

            timeslice = sliceManager.getTimeslice (sliceNum);

            try {
                MessageLogger.logDebug (String.format ("start visitor dispatch in slice %d", sliceNum));
                dispatchVisitorsInTimeslice (timeslice);
            } catch (Exception e) {
                MessageLogger.logWarning (String.format ("ActualVisitor dispatchVisitor exception: %s", e.getMessage()));
                // e.printStackTrace ();
                break;
            }
//@@ break; // single thread debug
        }
    }

    private void dispatchVisitorsInTimeslice (Timeslice timeslice) throws Exception {
        int maxVisitorsInTimeslice;
        int actualVisitorsInTimeslice;
        long sliceDuration;
        long meanTimeBetweenVisitors;
        long arrivalTime;

        // maxVisitors in this slice
        switch (timeslice.getTrafficState ()) {
            case GeneratorConstants.TRAFFIC_STATE_LOW:
                    maxVisitorsInTimeslice = GeneratorConstants.MAX_VISITORS_LOW_TRAFFIC_PER_TIMESLICE;
                    break;

            case GeneratorConstants.TRAFFIC_STATE_MEDIUM:
                    maxVisitorsInTimeslice = GeneratorConstants.MAX_VISITORS_MEDIUM_TRAFFIC_PER_TIMESLICE;
                    break;

            case GeneratorConstants.TRAFFIC_STATE_HIGH:
                    maxVisitorsInTimeslice = GeneratorConstants.MAX_VISITORS_HIGH_TRAFFIC_PER_TIMESLICE;
                    break;

            default:
                    maxVisitorsInTimeslice = GeneratorConstants.MAX_VISITORS_LOW_TRAFFIC_PER_TIMESLICE;
        }

        actualVisitorsInTimeslice = ((int)(Math.random () * maxVisitorsInTimeslice)) + 1;
        MessageLogger.logDebug (String.format ("Timeslice num = %d, start = %s, state = %s, Actual visitors: %s", 
                                                timeslice.getSliceNum(), timeslice.getVirtualStart(), timeslice.getTrafficState(), actualVisitorsInTimeslice));

        sliceDuration = timeslice.getVirtualEnd () - timeslice.getVirtualStart ();
        meanTimeBetweenVisitors = sliceDuration / actualVisitorsInTimeslice;
        meanTimeBetweenVisitors = Math.max (MIN_TIME_BETWEEN_VISITOR_DISPATCH, meanTimeBetweenVisitors);
        MessageLogger.logDebug (String.format ("Visitor meanTimeBetweenVisitors = %d", meanTimeBetweenVisitors));

        arrivalTime = timeslice.getVirtualStart ();    // initialize
        for (int actual = 0; (actual < actualVisitorsInTimeslice) && (isInterrupted() == false); actual++) {
            Visitor visitor;

            visitor = constructVisitor (actual, arrivalTime);
            MessageLogger.logDebug (String.format ("\tactual visitor in timeslice %s at time = %s", actual, arrivalTime));
            this.visitorSignal.setVisitor (visitor);

            // wake up listener of this signal obj
            synchronized (this.visitorSignal) {
                this.visitorSignal.notify ();   
            }
            MessageLogger.logDebug (String.format ("\tVisitor signal notified, visitorId = %s", visitor.getVisitorId ()));
            
            // wait before creating next visitor (or exiting this loop)                 
            try {
                sleep (meanTimeBetweenVisitors);
            } catch (InterruptedException ie) {
                MessageLogger.logWarning ("ActualVisitor sleep interrupted");
                break;
            }

            arrivalTime = arrivalTime + meanTimeBetweenVisitors;
//@@@ break; // single thread debug
        }

       totalVisitorsInDay += actualVisitorsInTimeslice;
    }

    private Visitor constructVisitor (int visitorNum, long arrivalTime) {
        Visitor visitor;
        int randomVal;  // for device type selection
        String fullVisitorIdStr = null;
        int visitorDeviceType;

        // in order to generate past-purchase-widget-data (shown in SPA), we generate
        // a visitor N number of times each month, with "SPECIAL" visitor UID. 
        // SpecialId should be 13-digit as required in BR algo's
        if (this.specialVisitorId != null) {
            int dayOfMonth;

            dayOfMonth = this.calendar.get (GregorianCalendar.DAY_OF_MONTH);
            if (this.specialVisitDays.contains ((Integer) dayOfMonth)) {
                long specialVisitorIdLong = Long.valueOf (this.specialVisitorId);
                if (activeVisitorIdsInADay.contains (specialVisitorIdLong) == false) {
                    fullVisitorIdStr = this.specialVisitorId;
                    this.activeVisitorIdsInADay.add (specialVisitorIdLong);
                }
            }
        }

        if (fullVisitorIdStr == null) {
            long randomVisitorId;
            String randomVisitorIdStr;
            int loopCount = 0;
            long maxAttempts = GeneratorConstants.MAX_VISITOR_POOL_SIZE;
            int actualTemplateHeadLen;
            String actualVisitorIdHead;
 
            // make sure visitorId is not re-used if one is already 'visiting site'
            do {
                randomVisitorId = ((long) (Math.random () * GeneratorConstants.MAX_VISITOR_POOL_SIZE)) + 1;
                loopCount++;
            } while ((activeVisitorIdsInADay.contains (randomVisitorId)) && (loopCount < maxAttempts));

            if (loopCount >= maxAttempts) 
                MessageLogger.logWarning ("Could not generate unique visitorId");
 
            this.activeVisitorIdsInADay.add (randomVisitorId);
            randomVisitorIdStr = Long.toString (randomVisitorId);

            // use the actualVisitorIdTemplate to construct this visitor's id-string 
            actualTemplateHeadLen = this.activeVisitorIdTemplate.length() - randomVisitorIdStr.length();
            actualVisitorIdHead = this.activeVisitorIdTemplate.substring (0, actualTemplateHeadLen);
            fullVisitorIdStr = String.format ("%s%s", actualVisitorIdHead, randomVisitorIdStr);
        }

        // select a deviceType for visitor (so that insights reports show different charts for that)
        // mobile 20%, tablet 20%, desktop 55%, other 5%
        randomVal = (int)(Math.random () * 100);

        if ((randomVal >= 0) && (randomVal < 20))
            visitorDeviceType = GeneratorConstants.DEVICE_TYPE_MOBILE;
        else if ((randomVal >= 20) && (randomVal < 40))
            visitorDeviceType = GeneratorConstants.DEVICE_TYPE_TABLET;
        else if ((randomVal >= 40) && (randomVal < 95))
            visitorDeviceType = GeneratorConstants.DEVICE_TYPE_DESKTOP;
        else
            visitorDeviceType = GeneratorConstants.DEVICE_TYPE_OTHER;

        visitor = new Visitor (fullVisitorIdStr, arrivalTime, visitorDeviceType);
        return visitor;
    }
}
