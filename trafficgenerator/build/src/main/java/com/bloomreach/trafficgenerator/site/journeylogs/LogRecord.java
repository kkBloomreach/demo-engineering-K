package com.bloomreach.trafficgenerator.site.journeylogs;

import java.io.FileWriter;
import java.io.IOException;

public class LogRecord {

    private String userId;
    private String viewId;
    private String userSegment;
    private String journeyType;
    private long   journeyStart;
    private long   stepStart;
    private int    sessionNum;
    private String sessionType;
    private String step; // atc, browse, ...
    private String query; // query as appropriate for a step
    private String stepResult;

    public LogRecord () {
        this.viewId = "None";   // these are optional fields
        this.userSegment = "None";
        this.query = "-";
    }

    public void setUserId (String userId) {
        this.userId = userId;
    }

    public void setViewId (String viewId) {
        this.viewId = viewId;
    }

    public void setUserSegment (String segment) {
        this.userSegment = segment;
    }

    public void setJourneyStart (long journeyStart) {
        this.journeyStart = journeyStart;
    }

    public void setStepStart (long stepStart) {
        this.stepStart = stepStart;
    }

    public void setJourneyType (String journeyType) {
        this.journeyType = journeyType;
    }

    public void setSessionNum (int sessionNum) {
        this.sessionNum = sessionNum;
    }

    public void setSessionType (String sessionType) {
        this.sessionType = sessionType;
    }

    public void setStep (String step) {
        this.step = step;
    }

    public void setQuery (String query) {
        this.query = query;
    }

    public void setStepResult (String stepResult) {
        this.stepResult = stepResult;
    }

    public static void writeHeader (FileWriter fileWriter) throws IOException {
        String header;

        header = prepareHeader ();
        fileWriter.write (header);
        fileWriter.flush ();
    }

    // data must be in same sequence as the header
    public void writeData (FileWriter fileWriter) throws IOException {
        StringBuffer sb;
        String dataString;

        sb = new StringBuffer ();
        sb.append ("\n" + 
                   this.userId       + "\t" +
                   this.viewId       + "\t" +
                   this.userSegment  + "\t" +
                   this.journeyType  + "\t" +
                   this.journeyStart + "\t" +
                   this.stepStart    + "\t" +
                   this.sessionNum   + "\t" +
                   this.sessionType  + "\t" +
                   this.step + "\t"  +
                   this.query + "\t" +
                   this.stepResult
                  );
        dataString = new String (sb);
        fileWriter.write (dataString);  // no need to flush each record individually
    }

    private static String prepareHeader () {
        StringBuffer sb;

        sb = new StringBuffer ();
        sb.append ("UserId" + "\t" +
                   "ViewId" + "\t" +
                   "User Segment"  + "\t" +
                   "JourneyType"   + "\t" +
                   "JourneyStart"  + "\t" +
                   "stepStart"     + "\t" +
                   "SessionNum"    + "\t" +
                   "SessionType"   + "\t" +
                   "Step" + "\t"   +
                   "Query" + "\t"  +
                   "StepResult"
                  );
        return new String (sb);
    }

}
