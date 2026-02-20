package com.bloomreach.analyticssimulator.simdata;

// Use the manually prepared category -> pid list for each of the categories.
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.MessageLogger;
import com.bloomreach.analyticssimulator.feed.ProcessedFeed;

public class ZeroResultQueryMap {

    ArrayList <ZeroResultQueryRecord> zeroResultQueryRecordList;
    ProcessedFeed processedFeed;

    public ZeroResultQueryMap () {
    }

    // when this class is used from SimulatePixelLogs, it will have already
    // loaded processedFeed and therefore supplied to this class
    public void setProcessedFeed (ProcessedFeed processedFeed) {
        this.processedFeed = processedFeed;
    }

    public void doLoad (String srcFilePath) throws Exception {
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        int lineNum = 0;

        zeroResultQueryRecordList = new ArrayList <ZeroResultQueryRecord> ();

        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            // this src file does not have a 'headerLine' by itself
            // Each line has:
            // id, query segment refinedSearchQuery refinedCategoryQuery
            // The "refined..." values are id values in the SegmentQueryToPidMap and SegmentCategoryToPidMap 
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;
                ZeroResultQueryRecord record;
                String segment;
                int refinedSearchQueryId;
                int refinedCategoryQueryId;

                // skip header line
                if (lineNum == 0) {
                    lineNum = lineNum + 1;
                    continue;
                }

                if (srcLine.length () == 0)
                    continue;

                tokens = srcLine.split ("\t");

                // src file may have blank tokens (ie, blank lines)
                if (tokens.length == 0)
                    continue;

                // params: id query segment refinedSearchQueryId refinedCategoryQueryId
                // token[0] : id, token[1]: query
                record = new ZeroResultQueryRecord (Integer.parseInt (tokens[0]), 
                                                    tokens[1].trim());

                zeroResultQueryRecordList.add (record);
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading ZeroResultQueryMap: " + e.getMessage ());
        }

        if (srcReader != null)
        {
            try {
                srcReader.close ();
            }
            catch (Exception e)
            {
                MessageLogger.logError ("Src reader close exception: " + e.getMessage ());
            }
        }
    }

    public ArrayList <ZeroResultQueryRecord> getZeroResultQueryRecordList () {
        return (zeroResultQueryRecordList);
    }

    // given the list of predefined zeroResultQueries, pick one at random
    public ZeroResultQueryRecord selectZeroResultQueryAtRandom () {
        int randomIndx;
        int totalQueries;
        ZeroResultQueryRecord selectedRecord;

        totalQueries = zeroResultQueryRecordList.size ();
        randomIndx = (int) (Math.random () * totalQueries); // values include 0 to (but not including) total
        selectedRecord = zeroResultQueryRecordList.get (randomIndx);
        return (selectedRecord);
    }
}

