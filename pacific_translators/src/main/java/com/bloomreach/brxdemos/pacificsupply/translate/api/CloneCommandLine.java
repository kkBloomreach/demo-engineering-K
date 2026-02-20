package com.bloomreach.analyticssimulator;

import java.io.File;

public class SimulatorCommandLine {

    private final static int DEFAULT_MAX_DAYS = 31;

    private String rootSourceDataDirPath = null;
    private int    maxDays = DEFAULT_MAX_DAYS;
    private int    startAt = 0; // start from day 0 by default
    private int    maxUsers = -1; // means, use ALL userIds defined
    private String accountName = null;
    private String outputDirPath = null;
    private String messageLevel = "fatal";  // default

    public SimulatorCommandLine () {
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
            } else if (args [i].trim().equals ("-n") == true) {
                String maxDaysStr = args [i+1].trim();
                i = i + 2;
                try {
                    maxDays = Integer.parseInt (maxDaysStr);
                } catch (Exception e) {
                    showHelp ();
                    return false;
                }
            } else if (args [i].trim().equals ("-s") == true) {
                String startAtStr = args [i+1].trim ();
                i = i + 2;
                try {
                    startAt = Integer.parseInt (startAtStr);
                } catch (Exception e) {
                    showHelp ();
                    return false;
                }
            } else if (args [i].trim().equals ("-u") == true) {
                String maxUserStr = args [i+1].trim ();
                i = i + 2;
                try {
                    maxUsers = Integer.parseInt (maxUserStr);
                } catch (Exception e) {
                    showHelp ();
                    return false;
                }
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

    public int getMaxDays () {
        return maxDays;
    }

    public int getStartAt () {
        return startAt;
    }

    public int getMaxUsers () {
        return maxUsers;
    }

    public String getMessageLevel () {
        return messageLevel;
    }
        
    private void showHelp () {
        System.out.println ("Simulator " + 
                            "-a <accountName> " +
                            "-d <rootDataPath> " +
                            " [-o <outputDirPath>] " +
                            " [-n <maxDaysToSimulate>] " +
                            " [-s <startAtToSimulate>] " +
                            " [-u <maxUsers> ] " + 
                            " [-l <info|debug|warn|error>] ");
    }

}


