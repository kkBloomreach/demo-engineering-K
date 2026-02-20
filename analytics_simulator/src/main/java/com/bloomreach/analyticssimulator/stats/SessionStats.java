package com.bloomreach.analyticssimulator.stats;

import java.io.FileWriter;

public class SessionStats {

    private int sessionType;
    private String view; // view
    private String segment; // segment name
    private String uid; // which user
    private String selectedPrimaryQuery; // query - either search or catId
    private String selectedRefinedQuery; // query - either search or catId
    private String selectedPid; // which pid
    private int conversionQuantity; // quantity converted
 
    public SessionStats (int sessionType, String view, String segment, String uid) {
        this.sessionType = sessionType;
        this.view = view;
        this.segment = segment;
        this.uid = uid;

        this.selectedPrimaryQuery = null;
        this.selectedRefinedQuery = null;
        this.selectedPid = null;
        this.conversionQuantity = -1;
    }

    public void setPrimaryQuery (String query) {
        this.selectedPrimaryQuery = query;
    }

    public void setRefinedQuery (String query) {
        this.selectedRefinedQuery = query;
    }

    public void setSelectedPid (String pid) {
        this.selectedPid = pid;
    }

    public int getSessionType () {
        return (this.sessionType);
    }

    public String getSegment () {
        return (this.segment);
    }

    public void writeSessionStat (int sessionNum, FileWriter fileWriter) throws Exception {
        String line;

        line = String.format ("%d\t%s", sessionNum, this.toString());
        fileWriter.write (line);
    }

    public String toString () {
        StringBuffer sb;
        String sessionName = "";
        String refinedQuery = "-";

        // following map must match the one used in Simulator.java
        switch (this.sessionType) {
            case 0: sessionName = "s"; break;
            case 1: sessionName = "c"; break;
            case 2: sessionName = "s2s"; break;
            case 3: sessionName = "s2c"; break;
            case 4: sessionName = "c2s"; break;
            case 5: sessionName = "c2c"; break;
            case 6: sessionName = "z"; break;   // zero-result
            case 7: sessionName = "z2s"; break;   // zero-result
            case 8: sessionName = "z2c"; break;   // zero-result
            default: sessionName = "?";
        }
      
        // refinedQuery may be null (for "s", "c" sessionType) 
        if (this.selectedRefinedQuery == null)
            refinedQuery = "-";
        else
            refinedQuery = this.selectedRefinedQuery;
 
        sb = new StringBuffer ();
        sb.append (sessionName + "\t" +
                   this.view + "\t" + 
                   this.segment + "\t" + 
                   this.uid + "\t" +
                   this.selectedPid + "\t" +
                   this.selectedPrimaryQuery + "\t" +
                   refinedQuery);

        return (new String (sb));
    }

    // static method
    public static String prepareSessionHeader () {
        StringBuffer sb;

        sb = new StringBuffer ();
        // header fields must match those used in 'toString' method
        sb.append ("Session#" + "\t" +
                   "SessionType" + "\t" +
                   "View" + "\t" +
                   "Segment" + "\t" +
                   "UserId" + "\t" +
                   "Pid" + "\t" +
                   "PrimaryQuery" + "\t" +
                   "RefinedQuery");
        return (new String (sb));
    }
}

