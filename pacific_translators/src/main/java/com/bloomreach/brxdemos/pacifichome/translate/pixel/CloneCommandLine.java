package com.bloomreach.brxdemos.pacifichome.translate.pixel;

import java.io.File;

public class CloneCommandLine {

    private final static int DEFAULT_MAX_DAYS = 31;
    private String rootDirPath = "./data"; // default for testing purpose;
    private int    maxDays = DEFAULT_MAX_DAYS;
    private int    startAt = 0; // start from day 0

    public CloneCommandLine () {
    }

    public boolean parse (String[] args) {
        int i = 0;

        if ((args == null) || (args.length < 2)) {
            showHelp ();
            return false;
        }

        while (i < args.length) {
            if (args[i].trim().equals ("-d") == true) {
                rootDirPath = args [i+1].trim();
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
                String startAtStr = args [i+1].trim();
                i = i + 2;
                try {
                    startAt = Integer.parseInt (startAtStr);
                } catch (Exception e) {
                    showHelp ();
                    return false;
                }
            } else {
                showHelp ();
                return false;
            }
        }

        // enforce required args
        if (rootDirPath == null) {
            showHelp ();
            return false;
        } else {
            File dirFile;
            dirFile = new File (rootDirPath);
            if ((dirFile.exists () == false) || (dirFile.isDirectory() == false)) {
                showHelp ();
                return false;
            }
        }

        return true;
    }

    public String getRootDataDir () {
        return rootDirPath;
    }

    public int getMaxDays () {
        return maxDays;
    }

    public int getStartAt () {
        return startAt;
    }

    private void showHelp () {
        System.err.println ("Clone -d <rootDataPath> [-n <maxDaysToProcess>] [-s <startAtToProcess>] ");
    }

}
