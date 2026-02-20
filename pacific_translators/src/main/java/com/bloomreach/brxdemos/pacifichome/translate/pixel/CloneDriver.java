package com.bloomreach.brxdemos.pacifichome.translate.pixel;

// Read input sequence file(s), get pixeLogs from them, clone each pixelLog and write the clone'd logs to output file(s)
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.clone.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.*;

public class CloneDriver {

    // NOTE: within "ROOT" dir (name = 'data'), subfolders are expected to have a predefined substructure
    // "ROOT" is provided as an argument to the application

    // data -> source -> "i" {i=0->30} -> {source, output}
    // data -> resources -> {processedFeed.tsv, ...}

    private final static String SOURCE_PIXELLOG_DIR = "./source/";
    private final static String PREPROCESSED_FEED_PATH = "./source/translated_feed.xml";
    private final static String PREPROCESSED_PROD_URL_PID_MAP_PATH = "./source/prod_url_pid_map.tsv";
    private final static String PREPROCESSED_CAT_URL_CRUMB_MAP_PATH = "./source/cat_urlleaf_crumb_map.tsv";
    private final static String OUTPUT_PIXELLOG_DIR = "./output/";
    private final static String VERSION = "1.3.2.0";

    // preProcessed feed
    private ProcessedFeed processedFeed;

    // orderId generator
    private OrderIdGenerator orderIdGenerator;

    // pre-generated url_pid_map - used to map source ref-product-urls 
    private ProductURLPidMapReader productUrlPidMapReader;

    // pre-generated urlLeaf_crumb_map - used to map source category-ref-urls and category pixel info
    private CategoryURLCrumbMapReader catUrlCrumbMapReader;

    // logFile cloner
    private CloneOnePixelLogFile cloneOnePixelLogFile;

    public static void main (String[] args) {
        CloneDriver cloneAllFiles;
        CloneCommandLine commandLine;

        System.out.println ("Analytics pixel translator, version: " + VERSION);

        commandLine = new CloneCommandLine ();
        if (commandLine.parse (args) == false) {
            System.exit (-1);   // help message already shown
        }

        cloneAllFiles = new CloneDriver ();
        try {
            // read external information - processedFeed 
            cloneAllFiles.init (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("Exception in cloneDriver initialization");
            System.exit (-1);
        }

        try {
            cloneAllFiles.doCloneAll (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("Exception in pixelLog cloning");
            System.exit (-1);
        }

        System.exit (0);
    }

    private CloneDriver () {
    }

    private void init (CloneCommandLine commandLine) throws Exception {
        String rootDirPath;

        rootDirPath = commandLine.getRootDataDir ();
        // processedFeed, orderIdGenerator, ...
        initSupportModules (rootDirPath, PREPROCESSED_FEED_PATH, PREPROCESSED_PROD_URL_PID_MAP_PATH,
                            PREPROCESSED_CAT_URL_CRUMB_MAP_PATH);

        // clone one log file. Single log file may contain multiple
        // individual pixelLogs
        cloneOnePixelLogFile = new CloneOnePixelLogFile ();
        cloneOnePixelLogFile.setProcessedFeed (processedFeed);
        cloneOnePixelLogFile.setOrderIdGenerator (orderIdGenerator);
        cloneOnePixelLogFile.setProductUrlPidMapReader (productUrlPidMapReader);
        cloneOnePixelLogFile.setCategoryUrlCrumbMapReader (catUrlCrumbMapReader);
    }

    private void doCloneAll (CloneCommandLine commandLine) throws Exception {
        PartFilenameFilter filenameFilter;
        String rootDataDirPath;
        int maxDaysToClone;
        int startAt;

        rootDataDirPath = commandLine.getRootDataDir ();
        maxDaysToClone = commandLine.getMaxDays ();
        startAt = commandLine.getStartAt (); // start from day = startAt; default 0
        filenameFilter  = new PartFilenameFilter (); // collect files with name part-*

        // start from 'startAt' (ie, 0, 1, 2, ...), and process "maxDays" (ie, 10, 20, ..) from that start
        for (int day = startAt; day < (startAt + maxDaysToClone); day++) {
            File oneSourceDir;
            File oneDestinationDir;
            String[] srcFilePathList;
            GregorianCalendar calendar;

            // src = <root>/source/{i}/source
            oneSourceDir = new File (rootDataDirPath, SOURCE_PIXELLOG_DIR + day); 

            // if srcdir does not exist, continue. 
            // This helps in unittests where we have only one source dir
            if (oneSourceDir.exists () == false) {
                continue;
            }

            // create destination directory if it does not already exist
            oneDestinationDir = new File (rootDataDirPath, OUTPUT_PIXELLOG_DIR + day);
            oneDestinationDir.mkdirs ();

            // orderIds are unique per-day
            // MONTH is 0-based value. DAY_OF_MONTH is 1-based value 
            calendar = new GregorianCalendar (2019, 10, day+1, 07, 00); // Month has 31 days
            orderIdGenerator.setDate (calendar.get (GregorianCalendar.YEAR),
                                      calendar.get (GregorianCalendar.MONTH) + 1,
                                      calendar.get (GregorianCalendar.DAY_OF_MONTH));

            srcFilePathList = oneSourceDir.list (filenameFilter);
            if ((srcFilePathList != null) && (srcFilePathList.length > 0)) {
                for (int j = 0; j < srcFilePathList.length; j++) {
                    String srcFileName;
                    File srcFile;
                    File destinationFile;

                    srcFile = new File (oneSourceDir, srcFilePathList [j]);
                    srcFileName = srcFile.getName ();
                    destinationFile = new File (oneDestinationDir, srcFileName); // output filename == src filename

                    System.out.println ("srcPath: " + srcFile.getPath()  + ", destPath = " + destinationFile.getPath ());

                    cloneOnePixelLogFile.doClone (srcFile.getPath(), destinationFile.getPath ());
                }
            }
        }
    }
 
    // load processedFeed information
    private void initSupportModules (String rootDir, String feedPath, String prodUrlPidMapPath, 
                                     String catUrlCrumbMapPath) {
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

        // orderId generator
        orderIdGenerator = new OrderIdGenerator ();

        // prodUrlPidMap reader
        productUrlPidMapReader = new ProductURLPidMapReader ();
        try {
            File prodUrlPidMapFile;
            prodUrlPidMapFile = new File (rootDir, prodUrlPidMapPath);
            productUrlPidMapReader.load (prodUrlPidMapFile.getPath());
        } catch (Exception e) {
            System.out.println ("ProductUrlPidMapReader exception: " + e.getMessage ());
            productUrlPidMapReader = null;
        }

        // cat urlLeaf->crumb map
        catUrlCrumbMapReader = new CategoryURLCrumbMapReader ();
        try {
            File catUrlCrumbMapFile;
            catUrlCrumbMapFile = new File (rootDir, catUrlCrumbMapPath);
            catUrlCrumbMapReader.load (catUrlCrumbMapFile.getPath());
        } catch (Exception e) {
            System.out.println ("Category URLCrumbMapReader exception: " + e.getMessage ());
            catUrlCrumbMapReader = null;
        } 
    }


    // filename filter used to extract filelist
    class PartFilenameFilter implements FilenameFilter {

        public PartFilenameFilter () {
        }

        public boolean accept (File srcDir, String fileName) {
            if (fileName.endsWith (".log") == true)
                return false;
            if (fileName.startsWith ("part-") == true)
                return (true);
            return (false);
        }
    }
}

