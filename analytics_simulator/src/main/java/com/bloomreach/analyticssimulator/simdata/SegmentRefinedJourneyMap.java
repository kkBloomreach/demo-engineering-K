// use a map to pick up a 'refined' search-term or category after an initial search or category visit
package com.bloomreach.analyticssimulator.simdata;

// Use the manually prepared refUrl list
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.MessageLogger;
import com.bloomreach.analyticssimulator.feed.ProcessedFeed;

public class SegmentRefinedJourneyMap {

    ArrayList <SegmentRefinedJourneyMapRecord> s2sRefinedJourneyRecordList;
    ArrayList <SegmentRefinedJourneyMapRecord> s2cRefinedJourneyRecordList;
    ArrayList <SegmentRefinedJourneyMapRecord> c2sRefinedJourneyRecordList;
    ArrayList <SegmentRefinedJourneyMapRecord> c2cRefinedJourneyRecordList;

    ProcessedFeed processedFeed;

    public SegmentRefinedJourneyMap () {
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

        s2sRefinedJourneyRecordList = new ArrayList <SegmentRefinedJourneyMapRecord> ();
        s2cRefinedJourneyRecordList = new ArrayList <SegmentRefinedJourneyMapRecord> ();
        c2sRefinedJourneyRecordList = new ArrayList <SegmentRefinedJourneyMapRecord> ();
        c2cRefinedJourneyRecordList = new ArrayList <SegmentRefinedJourneyMapRecord> ();

        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            // this src file does not have a 'headerLine' by itself
            // Each line has:
            // refineType<tab>primaryId<tab>refinedId
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;
                SegmentRefinedJourneyMapRecord record;
                String journeyType;
                String primaryQueryOrCatId;
                String view;
                String segment;

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

                // params: journeyType, primaryQuery(or category id), view, segment, refined*
                journeyType = tokens[0].toLowerCase (); // "s2s"/"c2c"/...
                primaryQueryOrCatId = tokens [1];
                view = tokens [2];
                segment = tokens [3];
                record = new SegmentRefinedJourneyMapRecord (journeyType, primaryQueryOrCatId, view, segment);

                for (int i = 4; i < tokens.length; i++) {
                    record.addRefined (tokens [i]); // token, for s2s, queryStr. For c2c, catId
                }
 
                switch (journeyType) {
                    case "s2s":
                        s2sRefinedJourneyRecordList.add (record);
                        break;
                    case "s2c":
                        s2cRefinedJourneyRecordList.add (record);
                        break;
                    case "c2s":
                        c2sRefinedJourneyRecordList.add (record);
                        break;
                    case "c2c":
                        c2cRefinedJourneyRecordList.add (record);
                        break;
                    default:
                        MessageLogger.logError ("unknown refinedJourney type SegmentRefinedJourneyMap : " + journeyType);
                        continue; // internal data error
                }
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logError ("Exception reading SegmentRefinedJourneyMap : " + e.getMessage ());
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

    public ArrayList <SegmentRefinedJourneyMapRecord> getSegmentRefinedJourneyMapRecordList (String refineType) {
        switch (refineType.toLowerCase()) {
            case "s2s": return (s2sRefinedJourneyRecordList);
            case "s2c": return (s2cRefinedJourneyRecordList);
            case "c2s": return (c2sRefinedJourneyRecordList);
            case "c2c": return (c2cRefinedJourneyRecordList);
        }
        return null;
    }

    // given the list of predefined refinedJourney map records, pick one at random
    // for given journeyType and primaryQuery (queryStr or catId depending on s2s or c2c)
    // JourneyType expected: S2S, S2C, C2S, C2C
    // Return value is queryStr for s2s, catId for c2c
    public String selectRefinedQueryAtRandom (String refineType, String primaryQuery, String view, String segment) {
        int randomIndx;
        int totalCount;
        ArrayList <SegmentRefinedJourneyMapRecord> selectedList = null;
        String refinedQuery = null;

        switch (refineType.toLowerCase()) {
            case "s2s": selectedList = s2sRefinedJourneyRecordList; break;
            case "s2c": selectedList = s2cRefinedJourneyRecordList; break;
            case "c2s": selectedList = c2sRefinedJourneyRecordList; break;
            case "c2c": selectedList = c2cRefinedJourneyRecordList; break;
        }

        if (selectedList != null) {
            ArrayList <String> refinedList = null;

            // get refined-query (or catId) list for given primaryQuery (or catId)
            for (int i = 0; i < selectedList.size(); i++) {
                SegmentRefinedJourneyMapRecord journeyMapRecord;

                journeyMapRecord = selectedList.get (i);
                if ((journeyMapRecord.getPrimary ().equals (primaryQuery)) &&
                    (journeyMapRecord.getView ().equals (view)) &&
                    (journeyMapRecord.getSegment ().equals (segment))) {
                        refinedList = journeyMapRecord.getRefinedList ();
                        break;
                }
            }

            if (refinedList == null) {
                MessageLogger.logError ("cannot get refined list for primaryQuery: " + primaryQuery + 
                                    ", view = " + view + ", segment = " + segment);
                return null;
            }

            // pick ONE of the refined from the refinedList
            totalCount = refinedList.size ();
            randomIndx = (int) (Math.random () * totalCount); // values include 0 to (but not including) total
            refinedQuery = refinedList.get (randomIndx);

            MessageLogger.logDebug ("refinement type = " + refineType + 
                                ", primaryQuery " + primaryQuery + ", refined = " + refinedQuery +
                                ", view = " + view + ", segment = " + segment);
        }

        return (refinedQuery); // queryStr for s2s, catId for c2c
    }
}

