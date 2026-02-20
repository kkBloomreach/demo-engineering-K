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

public class SegmentCategoryToPidMap {

    ArrayList <SegmentCategoryToPidRecord> segmentPrimaryCategoryToPidRecordList;
    ArrayList <SegmentCategoryToPidRecord> segmentRefinedCategoryToPidRecordList;
    ProcessedFeed processedFeed;

    public SegmentCategoryToPidMap () {
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

        segmentPrimaryCategoryToPidRecordList = new ArrayList <SegmentCategoryToPidRecord> ();
        segmentRefinedCategoryToPidRecordList = new ArrayList <SegmentCategoryToPidRecord> ();

        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            // this src file does not have a 'headerLine' by itself
            // Each line has:
            // catName<tab>catId<tab>5 pids (last pid may be "-")
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;
                SegmentCategoryToPidRecord record;
                boolean isPrimary;
                String view;
                String segment;
                int numFound;

                // skip header line
                if (lineNum == 0) {
                    lineNum = lineNum + 1;
                    continue;
                }

                if (srcLine.length () == 0)
                    continue;

                // for some categories some pids are not specified. In that
                // case the input record has "-" in that place. Skip those columns
                tokens = srcLine.split ("\t");

                // src file may have blank tokens (ie, blank lines)
                if (tokens.length == 0)
                    continue;

                // params: catName, catId, catRelPath, isPrimary, segment
                // token[0] : id, token[1]: catName, token[2]: catid , token[3]: relPath, token[4]: numFound,
                // token[5]: isPrimary, token[6]: view, token[7]:segment, <pid>*
                numFound = Integer.parseInt (tokens[4]);
                isPrimary = (Integer.parseInt (tokens[5])) == 1 ? true : false; 
                view = tokens [6];
                segment = tokens [7];
                record = new SegmentCategoryToPidRecord (Integer.parseInt (tokens[0]), 
                                                         tokens[1].trim(), tokens[2], tokens[3], numFound, isPrimary, view, segment); 


                for (int i = 8; i < tokens.length; i++) {
                    if (tokens [i].indexOf ("-") < 0) {
                        try {
                            String pidValue = tokens [i]; 
                            if (processedFeed.isProductInFeed (pidValue) == true) {
                                MessageLogger.logDebug ("pid map: orig pid: " + pidValue);
                                record.addPid (pidValue);
                            } else
                                MessageLogger.logWarning ("Suggested pid is not in processed feed: " + tokens[i]);
                        } catch (Exception e) {
                            MessageLogger.logError ("Bad pid value: " + tokens [i]);
                        }
                    }
                }

                // primary and secondary queries kept in separate arrayLists
                if (record.getPidList().size () > 0) {
                    if (isPrimary == true)
                        segmentPrimaryCategoryToPidRecordList.add (record);
                    else
                        segmentRefinedCategoryToPidRecordList.add (record);
                }
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading SegmentCategoryToPidMap: " + e.getMessage ());
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

    public ArrayList <SegmentCategoryToPidRecord> getSegmentPrimaryCategoryToPidRecordList () {
        return (segmentPrimaryCategoryToPidRecordList);
    }

    public ArrayList <SegmentCategoryToPidRecord> getSegmentRefinedCategoryToPidRecordList () {
        return (segmentRefinedCategoryToPidRecordList);
    }

    // given the list of predefined queries, pick one at random
    // "Random" selection is only for primary query. The refined-query is
    // randomized in RefinedJourneyMap class
    public SegmentCategoryToPidRecord selectPrimaryCategoryAtRandom (String userView, String userSegment) {
        int totalCategories;
        int randomIndx;
        SegmentCategoryToPidRecord selectedCategoryRecord;
        boolean found = false;
        int attempt = 0;

        totalCategories = segmentPrimaryCategoryToPidRecordList.size();
        randomIndx = (int) (Math.random () * totalCategories); // values include 0 to (but not including) total
        selectedCategoryRecord = segmentPrimaryCategoryToPidRecordList.get (randomIndx);
        if ((selectedCategoryRecord.getSegment().equals (userSegment) == true) && (selectedCategoryRecord.getView ().equals (userView)))
            return (selectedCategoryRecord);

        // lookup an immediate subsequent record that does have required view and segment
        for (int i = 0; i < segmentPrimaryCategoryToPidRecordList.size(); i++) {
            randomIndx = (randomIndx + 1) % segmentPrimaryCategoryToPidRecordList.size();    // round-robbin
            selectedCategoryRecord = segmentPrimaryCategoryToPidRecordList.get (randomIndx);
            if ((selectedCategoryRecord.getView().equals (userView)) && (selectedCategoryRecord.getSegment().equals (userSegment)))
                return (selectedCategoryRecord);
        }

        MessageLogger.logError ("selectPrimaryCategoryRecord. Couldn't pick category for required segment: " + userSegment + 
                            ", view = " + userView);
        return null;
    }

    // given the list of predefined categories, pick one at random
    // "Random" selection is only for primary query. The refined-catId is
    // randomized in RefinedJourneyMap class
    public SegmentCategoryToPidRecord selectPrimaryCategoryAtRandom_OLD (String userView, String userSegment) {
        int randomIndx;
        int totalCategories;
        SegmentCategoryToPidRecord selectedCategoryRecord;
        boolean found = false;
        int attempt = 0;

        // attempt max N times; otherwise return null
        while (!found && attempt++ < 1000) {
            // first pick one of the categories at random - use only primaryCat list
            // Make sure the segment in query record is same as user's segment
            totalCategories = segmentPrimaryCategoryToPidRecordList.size ();
            randomIndx = (int) (Math.random () * totalCategories); // values include 0 to (but not including) total
            selectedCategoryRecord = segmentPrimaryCategoryToPidRecordList.get (randomIndx);
            if ((selectedCategoryRecord.getSegment().equals (userSegment) == true) && 
                (selectedCategoryRecord.getView ().equals (userView)))
                return (selectedCategoryRecord);
        }

        MessageLogger.logError ("selectPrimaryCategory. Couldn't pick category  for required segment: " + userSegment + 
                            ", view = " + userView);
        return (null);  // couldn't get category with required userSegment !!!
    }


    // IMPORTANT. Selected pid must be in the specific catName
    public String selectPidAtRandom (SegmentCategoryToPidRecord record) {
        ArrayList<String> pidListInRecord;
        int recordIndx;
        String selectedPid;

        pidListInRecord = record.getPidList ();

        // generate a random index in this list
        recordIndx = (int) (Math.random () * pidListInRecord.size ());
        selectedPid = pidListInRecord.get (recordIndx);
        MessageLogger.logDebug ("catName = " + record.getCatName () + ", pid = " + selectedPid);
        return (selectedPid);
    }

    // for c2c, a refined-cat-id has already been picked from the refined list
    // return the catToPid list for that refined-cat-id
    public SegmentCategoryToPidRecord getCategoryRecord (String catId) {
        SegmentCategoryToPidRecord selectedRecord = null;

        for (SegmentCategoryToPidRecord record : segmentRefinedCategoryToPidRecordList) {
            if (record.getCatId ().equals (catId)) {
                selectedRecord = record;
                break;
            }
        }

        return (selectedRecord);
    }

    // for s2c, the 'target' refined category is picked at-random
    public SegmentCategoryToPidRecord getRefinedCategoryRecord (String view, String segment) {
        int indx;
        SegmentCategoryToPidRecord selectedRecord = null;
        
        indx = (int) (Math.random() * segmentRefinedCategoryToPidRecordList.size());
        selectedRecord = segmentRefinedCategoryToPidRecordList.get (indx);
        if ((selectedRecord.getView().equals (view)) && 
            (selectedRecord.getSegment().equals (segment)))
                return (selectedRecord);

        // lookup an immediate subsequent record that does have required view and segment
        for (int i = 0; i < segmentRefinedCategoryToPidRecordList.size(); i++) {
            indx = (indx + 1) % segmentRefinedCategoryToPidRecordList.size();    // round-robbin
            selectedRecord = segmentRefinedCategoryToPidRecordList.get (indx);
            if ((selectedRecord.getView().equals (view)) && 
                (selectedRecord.getSegment().equals (segment)))
                return (selectedRecord);
        }

        MessageLogger.logError ("getRefinedCategory. Couldn't pick category for required segment: " + segment + 
                            ", view = " + view);
        return null;
    }
}

