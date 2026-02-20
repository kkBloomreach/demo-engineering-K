package com.bloomreach.brxdemos.pacifichome.translate.pixel.test;

import java.util.ArrayList;
import java.util.GregorianCalendar;
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

import com.bloomreach.brxdemos.pacifichome.translate.pixel.CloneCommandLine;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.CloneOnePixelLogFile;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.clone.OrderIdGenerator;

public class TestClonePixelLogFile {

    //private final static String PREPROCESSED_FEED_PATH = "./source/translated_feed.tsv";
    private final static String PREPROCESSED_FEED_PATH = "./source/translated_feed.xml";

    private String srcPath = "./data/source/0/part-00031";
    private String outputPath = "./data/output/0/output_part-00031";

    ProcessedFeed processedFeed = null;
    OrderIdGenerator orderIdGenerator;

    public static void main (String[] args) {
        TestClonePixelLogFile test;
        CloneCommandLine commandLine;

        commandLine = new CloneCommandLine ();
        test = new TestClonePixelLogFile ();
        try {
            test.doTest (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("pixelLogFile tester exception");
            System.exit (-1);
        }
    }

    private void doTest (CloneCommandLine commandLine) throws Exception {
        CloneOnePixelLogFile cloneFile;
        GregorianCalendar calendar;

        initSupportModules (commandLine.getRootDataDir(), PREPROCESSED_FEED_PATH);

        // orderIds are unique per-day
        calendar = new GregorianCalendar (2019, 10, 17, 07, 00); // Month has 31 days
        orderIdGenerator.setDate (calendar.get (GregorianCalendar.YEAR),
                                  calendar.get (GregorianCalendar.MONTH) + 1,
                                  calendar.get (GregorianCalendar.DAY_OF_MONTH));

        cloneFile = new CloneOnePixelLogFile ();
        cloneFile.setProcessedFeed (processedFeed);
        cloneFile.setOrderIdGenerator (orderIdGenerator);
 
        try {
            cloneFile.doClone (srcPath, outputPath);
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (-1);
        }

        System.exit (0);
    }

 
    // load processedFeed information
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

        orderIdGenerator = new OrderIdGenerator ();
    }

}


