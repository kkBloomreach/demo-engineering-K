package com.bloomreach.analyticssimulator.simdata;

// Uid to segment map is generated ONCE and saved to local file
// That generated map is loaded in this class + associated lookup methods
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.MessageLogger;
import com.bloomreach.analyticssimulator.feed.ProcessedFeed;

public class UidToSegmentMap {

    ArrayList <UidToSegmentRecord> uidToSegmentList;

    public UidToSegmentMap () {
    }

    // Load the uid->segment map that has already been generated (see Generate... class)
    public void doLoad (String srcFilePath) throws Exception {
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        boolean headerLine;

        uidToSegmentList = new ArrayList <UidToSegmentRecord> ();
        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            headerLine = true;
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;

                // skip header line
                if (headerLine == true) {
                    headerLine = false;
                    continue;
                }

                tokens = srcLine.split ("\t");
                // expected tokens: uid, view, segment
                if ((tokens != null) && (tokens.length == 3)) {
                    // column 0: uid
                    // column 1: view 
                    // column 2: segment 
                    if ((tokens[0] != null) && (tokens[1] != null) && (tokens[2] != null))
                    {
                        UidToSegmentRecord record;

                        record = new UidToSegmentRecord (tokens[0].trim(), tokens[1].trim(), tokens[2].trim());
                        uidToSegmentList.add (record);

                        MessageLogger.logDebug ("Record: " + tokens[0] + ", " + tokens[1]);
                    }
                }
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading UiSegmentMap: " + e.getMessage ());
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

    public int getSimulatedUidCount () {
        return (uidToSegmentList.size ()); 
    }

    public UidToSegmentRecord selectUidAtRandom () {
        int randomIndex;
        UidToSegmentRecord selectedRecord;

        randomIndex = (int) (Math.random () * uidToSegmentList.size());
        selectedRecord = uidToSegmentList.get (randomIndex); 
        return (selectedRecord);
    }

    public UidToSegmentRecord lookupUidToSegmentRecord (String uid) {

        for (UidToSegmentRecord record : uidToSegmentList) {
            if (record.getUid ().equals (uid) == true) {
                return (record);
            }
        }

        return (null);
    }

    public ArrayList <UidToSegmentRecord> getUidToSegmentList () {
        return (uidToSegmentList);
    }

}

