// Read input sequence file(s), get apiLogs from them, clone each apiLog and write the clone'd logs to output file(s)
package com.bloomreach.brxdemos.pacificsupply.translate.api;

import java.util.Hashtable;
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

public class TestCloneApiLogFile {

    public final static String PREPROCESSED_FEED_PATH = "./resources/full_preprocessed_08032020.tsv";

    private ProcessedFeed processedFeed;
    private Hashtable<String, String> uidToViewIdMap;

    public static void main (String[] args) {

        TestCloneApiLogFile tester;

        tester = new TestCloneApiLogFile ();
        tester.doTest ();
    }

    TestCloneApiLogFile () {
    }

    private void doTest () {
        CloneOneApiLogFile cloneOneFile;

        initSupportModules ("./data", PREPROCESSED_FEED_PATH);
 
        cloneOneFile = new CloneOneApiLogFile (processedFeed, uidToViewIdMap);

        // String srcPath = "./data/part-00031";
        // String outputPath = "./data/output_part-00031";
        String srcPath = "./data/api_logs/25/source/part-00760";
        String outputPath = "./data/api_logs/25/output/part-00760";

        try {
            cloneOneFile.doClone (srcPath, outputPath);
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (-1);
        }

        System.exit (0);
    }

    // load processedFeed and UIToViewIdMap information
    private void initSupportModules (String rootDir, String feedPath) {

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

        uidToViewIdMap = new Hashtable<String, String> ();
    }
}

