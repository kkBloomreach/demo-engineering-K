package com.bloomreach.analyticssimulator.stats;

import java.util.List;
import java.util.ArrayList;
import java.io.FileWriter;

public class DailyStats {
    private int dayNum;
    private List<SessionStats> sessionStats;
  
    public DailyStats (int dayNum) {
        this.dayNum = dayNum;
        sessionStats = new ArrayList <SessionStats> ();
    }

    public SessionStats addSessionStat (int sessionType, String view, String segment, String uid) {
        SessionStats newSessionStats;
        newSessionStats = new SessionStats (sessionType, view, segment, uid);
        sessionStats.add (newSessionStats);
        return (newSessionStats);
    }

    public void writeDailyStats (FileWriter fileWriter) throws Exception {
        int sessionNum = 0;
        String sessionHeader;

        // day's summary
        writeDailySummary (fileWriter);
        fileWriter.write ("\n");

        // individual session stats 
        sessionHeader = SessionStats.prepareSessionHeader ();
        fileWriter.write ("\t" + sessionHeader + "\n");

        for (SessionStats aSessionStat : sessionStats) {
            fileWriter.write ("\t");
            aSessionStat.writeSessionStat (sessionNum++, fileWriter);
            fileWriter.write ("\n");
        }
    }

    private void writeDailySummary (FileWriter fileWriter) throws Exception {
        int totalBudgetSessionCount = 0;
        int totalLuxurySessionCount = 0;

        for (SessionStats aSessionStat : sessionStats) {
            if (aSessionStat.getSegment ().equals ("BUDGET") == true)
                ++totalBudgetSessionCount;
            else if (aSessionStat.getSegment ().equals ("LUXURY") == true)
                ++totalLuxurySessionCount;
        }

        fileWriter.write ("Day: " + dayNum + "\n");
        fileWriter.write ("Total budget sessions: " + totalBudgetSessionCount + "\n");
        fileWriter.write ("Total luxury sessions: " + totalLuxurySessionCount + "\n");
    } 
}

