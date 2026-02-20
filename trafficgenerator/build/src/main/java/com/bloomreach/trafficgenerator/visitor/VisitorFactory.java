package com.bloomreach.trafficgenerator.visitor;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import com.bloomreach.trafficgenerator.visitor.slicemanager.*;

public class VisitorFactory {

    private long duration;  // duration for this factory to run (typically 1 day)
    private VisitorSignal visitorSignal;
    private GregorianCalendar calendar;
    private String specialVisitorId;
    private ArrayList<Integer> specialVisitDays;

    private SliceManager sliceManager;
    private VisitorCreatorThread visitorCreatorThread;

    public VisitorFactory () {
    }

    public void setDuration (long duration) {
        this.duration = duration;
    }

    public void setVisitorSignal (VisitorSignal visitorSignal) {
        this.visitorSignal = visitorSignal;
    }

    public void setCalendar (GregorianCalendar calendar) {
        this.calendar = calendar;
    }

    public void setSpecialVisitorData (String specialVisitorId, ArrayList<Integer> specialVisitDays) {
        this.specialVisitorId = specialVisitorId;
        this.specialVisitDays = specialVisitDays;
    }

    // duration == how long should this factory run (typically one day)
    public void init () throws Exception {
        this.sliceManager = new SliceManager ();
        this.sliceManager.setTimespan (this.calendar, this.duration);
        this.sliceManager.init ();
    }

    public void start () {
        // start visitorCreator thread
        this.visitorCreatorThread = new VisitorCreatorThread ();
        this.visitorCreatorThread.setDaemon (true);
        this.visitorCreatorThread.setName ("VisitorCreator");
        this.visitorCreatorThread.setVisitorSignal (visitorSignal);
        this.visitorCreatorThread.setSliceManager (this.sliceManager);
        this.visitorCreatorThread.setCalendar (this.calendar);
        this.visitorCreatorThread.setSpecialVisitorData (this.specialVisitorId, this.specialVisitDays);
        this.visitorCreatorThread.start (); // start creator thread
    }

    public void stop () {
        if ((this.visitorCreatorThread != null) && (this.visitorCreatorThread.isAlive () == true))
            this.visitorCreatorThread.interrupt ();
    }
}
