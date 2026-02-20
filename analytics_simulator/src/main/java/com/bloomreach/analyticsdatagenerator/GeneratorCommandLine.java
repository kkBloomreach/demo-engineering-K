package com.bloomreach.analyticsdatagenerator;

import java.io.File;
import org.apache.log4j.Logger;

public class GeneratorCommandLine {

    private String rootSourceDataDirPath = null;
    private String accountName = null;
    private String outputDirPath = null;
    private String messageLevel = "fatal";  // default

    public GeneratorCommandLine () {
    }

    public boolean parse (String[] args) {
        int i = 0;

        if ((args == null) || (args.length < 2)) {
            showHelp ();
            return false;
        }

        while (i < args.length) {
            if (args[i].trim().equals ("-d") == true) {
                rootSourceDataDirPath = args [i+1].trim();
                i = i + 2;
            } else if (args [i].trim().equals ("-a") == true) {
                accountName = args [i+1].trim();
                i = i + 2;
            } else if (args [i].trim().equals ("-o") == true) {
                outputDirPath = args [i+1].trim();
                i = i + 2;
            } else if (args [i].trim().equals ("-l") == true) {
                messageLevel = args [i+1].trim();   // 'info'/'debug'/'warn'/'error'
                i = i + 2;
            } else {
                showHelp ();
                return false;
            }
        }

        // enforce required args
        if (accountName == null) {
            showHelp ();
            return false;
        }
        if (rootSourceDataDirPath == null) {
            showHelp ();
            return false;
        } else {
            File dirFile;
            dirFile = new File (rootSourceDataDirPath);
            if ((dirFile.exists () == false) || (dirFile.isDirectory() == false)) {
                showHelp ();
                return false;
            }
        }
        // if no specific output dir, use 'root' of sourceData (ie, './data')
        if (outputDirPath == null)
            outputDirPath = rootSourceDataDirPath;

        return true;
    }

    public String getAccountName () {
        return accountName;
    }

    public String getRootSourceDataDir () {
        return rootSourceDataDirPath;
    }

    public String getOutputDir () {
        return outputDirPath;
    }

    public String getMessageLevel () {
        return messageLevel;
    }
        
    private void showHelp () {
        System.err.println ("DataGenerator " + 
                            "-a <accountName> " +
                            "-d <rootDataPath> " +
                            " [-o <outputDirPath>] " +
                            " [-l <info|debug|warn|error>] ");
    }

}

