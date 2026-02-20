package com.bloomreach.analyticssimulator.simdata;

// Use the manually prepared refUrl list
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.SimulatorConfig;
import com.bloomreach.analyticssimulator.MessageLogger;
import com.bloomreach.analyticssimulator.feed.ProcessedFeed;

public class RefUrlPool {

    ArrayList <RefUrlPoolRecord> refUrlPoolRecordList;
    ProcessedFeed processedFeed;

    public RefUrlPool () {
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

        refUrlPoolRecordList = new ArrayList <RefUrlPoolRecord> ();

        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            // this src file does not have a 'headerLine' by itself
            // Each line has:
            // urlType<tab>url
            // urlType's are "home"/"product"/"category"/
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;
                RefUrlPoolRecord record;
                String token0Lower;
                String fullRefUrl;
                String segment;
                String view;

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

                // params: urlType, url
                // token[0] : type
                // token[1] : view
                // token[2] : segment
                // token[3] : pid or catId
                token0Lower = tokens[0].trim().toLowerCase();
                view = tokens [1];
                segment = tokens [2];
                if (token0Lower.equals ("product") == true)
                    fullRefUrl = SimulatorConfig.getConfigParam ("PRODUCT_URL_PREFIX") + tokens[3];
                else if (token0Lower.equals ("category") == true)
                    fullRefUrl = SimulatorConfig.getConfigParam ("CATEGORY_URL_PREFIX") + tokens[3];
                else if (token0Lower.equals ("home") == true)
                    fullRefUrl = SimulatorConfig.getConfigParam ("HOMEPAGE_URL");
                else
                    fullRefUrl = "?";   // error
                record = new RefUrlPoolRecord (token0Lower, view, segment, fullRefUrl);
                refUrlPoolRecordList.add (record);
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading RegularCategoryToPidMap: " + e.getMessage ());
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

    public ArrayList <RefUrlPoolRecord> getRefUrlPoolRecordList () {
        return (refUrlPoolRecordList);
    }

    // given the list of predefined refUrls, pick one at random
    // In order to give 'lot of weight' to homepage, we consider 
    // 'totalUrls' to be 5x of actual. Then, if random index is >= actualSize,
    // set the index to 0 (which is homepage)
    public String selectRefUrlAtRandom () {
        int randomIndx;
        int totalRefUrlCount;
        RefUrlPoolRecord selectedRefUrlRecord;
        String refUrl;

        // pick one of the refUrls at random
        totalRefUrlCount = refUrlPoolRecordList.size ();
        randomIndx = (int) (Math.random () * (totalRefUrlCount*5)); // values include 0 to (but not including) total
        if (randomIndx >= totalRefUrlCount)
            randomIndx = 0; // index of 'home' page
        selectedRefUrlRecord = refUrlPoolRecordList.get (randomIndx);
        refUrl = selectedRefUrlRecord.getUrl ();
        return refUrl;  // assumes url has no 'query' parameters and hence does not need to be URLEncoded
    }

}

