package com.bloomreach.analyticssimulator.stats;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.FileWriter;

import com.bloomreach.analyticssimulator.simdata.*;

public class SimulationStats {

    private String version;
    private Date simulationDate;
    private UidToSegmentMap uidToSegmentMap;
    private SegmentQueryToPidMap segmentQueryToPidMap;
    private SegmentCategoryToPidMap segmentCategoryToPidMap;

    private List<DailyStats> dailyStats;
    private String[] segmentNames;
 
    public SimulationStats () {
        simulationDate = new Date ();
        dailyStats = new ArrayList <DailyStats> ();
    }

    public void setVersion (String version) {
        this.version = version;
    }

    public void setUidToSegmentMap (UidToSegmentMap uidToSegmentMap) {
        this.uidToSegmentMap = uidToSegmentMap;
    }

    public void setSegmentQueryToPidMap (SegmentQueryToPidMap segmentQueryToPidMap) {
        this.segmentQueryToPidMap = segmentQueryToPidMap;
    }

    public void setSegmentCategoryToPidMap (SegmentCategoryToPidMap segmentCategoryToPidMap) {
        this.segmentCategoryToPidMap = segmentCategoryToPidMap;
    }

    public void setSegments (String[] segmentNames) {
        this.segmentNames = segmentNames;
    }

    public DailyStats addNewDay (int dayNum) {
        DailyStats newDailyStats;

        newDailyStats = new DailyStats (dayNum);
        dailyStats.add (newDailyStats);
        return (newDailyStats);
    }

    public void logSimulationStats (String statsLogPath) {
        String stats;
        FileWriter fileWriter;

        try {
            fileWriter = new FileWriter (statsLogPath);
            writeSimulationStats (fileWriter);
            fileWriter.flush ();
            fileWriter.close ();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    private void writeSimulationStats (FileWriter fileWriter) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat ("yyyy/MM/dd");
        String dateString;
 
        dateString = dateFormat.format (this.simulationDate);
        fileWriter.write ("Simulation version: " + this.version + "\n");
        fileWriter.write ("Simulation date: " + dateString + "\n");
        fileWriter.write ("Total user count: " + uidToSegmentMap.getSimulatedUidCount () + "\n");
        fileWriter.write ("Segments: " + Arrays.toString (this.segmentNames) + "\n"); 

        for (DailyStats aDailyStat : dailyStats) {
            aDailyStat.writeDailyStats (fileWriter); 
        }
    }
}

