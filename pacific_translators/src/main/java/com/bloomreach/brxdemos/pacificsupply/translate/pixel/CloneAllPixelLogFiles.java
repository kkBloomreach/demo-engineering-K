package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

// Read input sequence file(s), get pixeLogs from them, clone each pixelLog and write the clone'd logs to output file(s)
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;

import org.apache.log4j.Logger;

public class CloneAllPixelLogFiles {

    // NOTE: within "ROOT" dir (name = 'data'), subfolders are expected to have a predefined substructure
    // "ROOT" is provided as an argument to the application

    // data -> source -> "i" {i=0->30} -> {source, output}
    // data -> resources -> {processedFeed.tsv, uidViewIdmap.tsv, ...}

    private final static String SOURCE_PIXELLOG_DIR = "./source/";
    private final static String PREPROCESSED_FEED_PATH = "./source/translated_feed.tsv";
    private final static String OUTPUT_PIXELLOG_DIR = "./output/";
    private final static String UID_TO_VIEWID_MAP_PATH = "./source/UidToViewIdMap.tsv";

    private ProcessedFeed processedFeed;
    private UidToViewIdMap uidViewIdMap;

    public static void main (String[] args) {
        CloneAllPixelLogFiles cloneAllFiles;

        if (args.length < 1) {
            System.err.println ("CloneAllPixelLogFiles <root-dir-path>");
            System.exit (-1);
        }

        cloneAllFiles = new CloneAllPixelLogFiles ();
        try {
            // read external information - processedFeed and UItoViewIdMap
            cloneAllFiles.initSupportModules (args[0], PREPROCESSED_FEED_PATH, UID_TO_VIEWID_MAP_PATH);
        } catch (Exception e) {
            System.out.println ("Exception in support module initialization");
            System.exit (-1);
        }

        try {
            cloneAllFiles.doCloneAll (args [0]);
        } catch (Exception e) {
            System.out.println ("Exception in pixelLog cloning");
            System.exit (-1);
        }

        System.exit (0);
    }

    private CloneAllPixelLogFiles () {
    }

    private void doCloneAll (String rootDataDirPath) throws Exception {
        PartFilenameFilter filenameFilter = new PartFilenameFilter (); // collect files with name part-*

        // for (int i = 0; i < 1; i++) {
        for (int i = 0; i < 31; i++) {
            File oneSourceDir;
            File oneDestinationDir;
            String[] srcFilePathList;

            // src = <root>/source/{i}/source
            oneSourceDir = new File (rootDataDirPath, SOURCE_PIXELLOG_DIR + i); 

            // if srcdir does not exist, continue. This helps in unittests where we have only one source dir
            if (oneSourceDir.exists () == false) {
                continue;
            }

            // create destination directory if it does not already exist
            oneDestinationDir = new File (rootDataDirPath, OUTPUT_PIXELLOG_DIR + i);
            oneDestinationDir.mkdirs ();

            srcFilePathList = oneSourceDir.list (filenameFilter);
            if ((srcFilePathList != null) && (srcFilePathList.length > 0)) {

                for (int j = 0; j < srcFilePathList.length; j++) {
                    String srcFileName;
                    File srcFile;
                    File destinationFile;
                    CloneOnePixelLogFile cloneOnePixelLogFile;

                    srcFile = new File (oneSourceDir, srcFilePathList [j]);
                    srcFileName = srcFile.getName ();
                    destinationFile = new File (oneDestinationDir, srcFileName); // output filename == src filename

                    System.out.println ("srcPath: " + srcFile.getPath()  + ", destPath = " + destinationFile.getPath ());

                    cloneOnePixelLogFile = new CloneOnePixelLogFile (processedFeed, uidViewIdMap);
                    cloneOnePixelLogFile.doClone (srcFile.getPath(), destinationFile.getPath ());
                }
            }
        }
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


    // filename filter used to extract filelist
    class PartFilenameFilter implements FilenameFilter {

        public PartFilenameFilter () {
        }

        public boolean accept (File srcDir, String fileName) {

            if (fileName.startsWith ("part-") == true)
                return (true);
            return (false);
        }
    }
}

