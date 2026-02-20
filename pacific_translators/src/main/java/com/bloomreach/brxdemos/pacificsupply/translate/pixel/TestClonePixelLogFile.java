package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

import java.util.ArrayList;
import java.io.IOException;
import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.bloomreach.proto.PwfPixelLog;
import com.bloomreach.proto.Aggregation.PixelLog;

public class TestClonePixelLogFile {

    private final static String PREPROCESSED_FEED_PATH = "./source/translated_feed.tsv";
    private final static String UI_TO_VIEWID_MAP_PATH = "./source/UidViewIdMap.tsv";

    private String srcPath = "./data/source/0/part-00031";
    private String outputPath = "./data/output/0/output_part-00031";

    ProcessedFeed processedFeed = null;
    UidToViewIdMap uidViewIdMap = null;

    public static void main (String[] args) {

        TestClonePixelLogFile test;

        test = new TestClonePixelLogFile ();
        try {
            test.doTest (args[0]);
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("pixelLogFile tester exception");
            System.exit (-1);
        }
    }

    private void doTest (String rootDir) throws Exception {
        CloneOnePixelLogFile cloneFile;

        initSupportModules (rootDir, PREPROCESSED_FEED_PATH, UI_TO_VIEWID_MAP_PATH);
        cloneFile = new CloneOnePixelLogFile (processedFeed, uidViewIdMap); 

        try {
            cloneFile.doClone (srcPath, outputPath);
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (-1);
        }

        System.exit (0);
    }

 
    // load processedFeed and UIToViewIdMap information
    private void initSupportModules (String rootDir, String feedPath, String uidViewIdPath) {

        // processed feed
        processedFeed = new ProcessedFeed ();
        try {
            File processedFeedFile;
            processedFeedFile = new File (rootDir, feedPath);
            processedFeed.load (processedFeedFile.getPath());
        } catch (Exception e) {
            System.out.println ("ProcessedFeed exception: " + e.getMessage ());
            processedFeed = null;
        }

        // uidViewId map
        uidViewIdMap = new UidToViewIdMap ();
        try {
            File uidViewIdMapFile;
            uidViewIdMapFile = new File (rootDir, uidViewIdPath);
            uidViewIdMap.load (uidViewIdMapFile.getPath());
        } catch (Exception e) {
            System.out.println ("UIDViewIdMap exception: " + e.getMessage ());
            uidViewIdMap = null;
        }
    }

}


