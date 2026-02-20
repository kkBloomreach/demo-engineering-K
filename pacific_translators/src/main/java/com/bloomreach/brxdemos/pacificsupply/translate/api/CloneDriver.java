// Read input sequence file(s), get apiLogs from them, clone each and write the clone'd logs to output file(s)
package com.bloomreach.brxdemos.pacificsupply.translate.api;

import java.util.ArrayList;
import java.util.Hashtable;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import com.bloomreach.brxdemos.pacificsupply.translate.api.feed.ProcessedFeed;

// NOTE for execution: 
// "ROOT" is provided as an argument to the application
// Within "ROOT" dir (name = 'data'), subfolders are expected to have a predefined substructure
// data -> source -> "i" {i=0->30} 
// data -> output (output generated within this folder)
// data -> source -> translated_feed.tsv

public class CloneDriver {
    private ProcessedFeed processedFeed;

    // key=uid, value=viewId
    private Hashtable <String, String> uidToViewIdMap;

    public static void main (String[] args) {
        CloneDriver cloneDriver;

        if (args.length < 1) {
            System.err.println ("CloneDriver <root-dir-path>");
            System.exit (-1);
        }

        cloneDriver = new CloneDriver ();
        try {
            // read external information - processedFeed and set up UidtoViewIdMap
            cloneDriver.initSupportModules (args[0], PREPROCESSED_FEED_PATH);
        } catch (Exception e) {
            System.out.println ("Exception in support module initialization");
            System.exit (-1);
        }

        try {
            cloneDrive.doCloneAll (args [0]);
        } catch (Exception e) {
            System.out.println ("Exception in apiLog cloning");
            System.exit (-1);
        }

        System.exit (0);
    }

    private CloneDriver () {
    }

    private void doCloneAll (String rootDataDirPath) throws Exception {
        PartFilenameFilter filenameFilter = new PartFilenameFilter (); // collect files with name part-*

        // for (int i = 0; i < 1; i++) {  // == for testing
        for (int i = 0; i < 31; i++) {
            File oneSourceDir;
            File oneDestinationDir;
            String[] srcFilePathList;

            // src = <root>/source/{i}
            oneSourceDir = new File (rootDataDirPath, SOURCE_APILOG_DIR + i); 

            // if srcdir does not exist, continue. This helps in unittests where we have only one source dir
            if (oneSourceDir.exists () == false) {
                continue;
            }

            // create destination directory if it does not already exist
            oneDestinationDir = new File (rootDataDirPath, OUTPUT_APILOG_DIR + i);
            oneDestinationDir.mkdirs ();

            // get list of all 'part-*' source log files
            srcFilePathList = oneSourceDir.list (filenameFilter);
            if ((srcFilePathList != null) && (srcFilePathList.length > 0)) {

                for (int j = 0; j < srcFilePathList.length; j++) {
                    String srcFileName;
                    File srcFile;
                    File destinationFile;
                    CloneOneApiLogFile cloneOneApiLogFile;
                    ArrayList <ApiLog> clonedApiLogsList;

                    srcFile = new File (oneSourceDir, srcFilePathList [j]);
                    srcFileName = srcFile.getName ();
                    destinationFile = new File (oneDestinationDir, srcFileName); // output filename == src filename

                    System.out.println ("srcPath: " + srcFile.getPath()  + ", destPath = " + destinationFile.getPath ());

                    cloneOneApiLogFile = new CloneOneApiLogFile (processedFeed, uidToViewIdMap);
                    // sometimes the source file itself is 'not good' (eg, length = 0). In that case, skip that
                    try {
                        clonedApiLogsList = cloneOneApiLogFile.doClone (srcFile.getPath());
                        if ((clonedApiLogsList != null) && (clonedApiLogsList.size () > 0)) 
                            writeOutputFile (clonedApiLogsList, destinationFile.getPath());
                    } catch (Exception e) {
                        System.err.println ("Exception in cloning. srcPath: " + srcFile.getPath() + ", skipping...");
                    }
                }
            }
        }

        // finally write ALL uidToViewIdMap to local file (.tsv format)
        {
            File destinationDir;
            File uidToViewIdMapFile;

            destinationDir = new File (rootDataDirPath, OUTPUT_APILOG_DIR);
            uidToViewIdMapFile = new File (destinationDir, UID_TO_VIEWID_MAP_FILENAME);
            writeUidToViewIdMap (uidToViewIdMap, uidToViewIdMapFile);
        }
    }
 
    // load translatedFeed information
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

        // hashtable to collect uid -> viewId map
        uidToViewIdMap = new Hashtable <String, String> ();
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


    private void writeOutputFile (ArrayList<ApiLog> clonedApiLogsList, String outputPath) throws Exception {

        Configuration configuration = new Configuration ();
        Writer.Option filePath = Writer.file (new Path (outputPath));
        Writer.Option keyClass = Writer.keyClass (Text.class);
        Writer.Option valueClass = Writer.valueClass (PWfMobileApiLog.class);
        Writer writer = SequenceFile.createWriter (configuration, 
                                                    filePath,
                                                    keyClass,
                                                    valueClass);

        Text key = new Text ("pacificsupply");
        for (ApiLog ApiLog : clonedApiLogsList) {
            writer.append (key, new PWfMobileApiLog (ApiLog));
        }

        writer.hflush ();
        writer.close ();
    }

    private void writeUidToViewIdMap (Hashtable<String, String> uidToViewIdMap, File uidToViewIdMapFile) throws Exception {
        PrintWriter printWriter;

        printWriter = new PrintWriter (uidToViewIdMapFile);
        // header line
        printWriter.print ("uid\tview_id\n"); 

        for (String key : uidToViewIdMap.keySet()) {
            String value = uidToViewIdMap.get (key);
            printWriter.print (key + "\t" + value + "\n");
        }

        printWriter.flush ();
        printWriter.close ();
    }
}

