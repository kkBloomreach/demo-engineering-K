package com.bloomreach.analyticssimulator;

import java.io.File;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;


public class MessageLogger {

    private static Logger messageLogger = null;

    public MessageLogger () {
    }

    public void init (SimulatorCommandLine commandLine) {
        PatternLayout patternLayout;
        String conversionPattern;
        ConsoleAppender consoleAppender;
        FileAppender fileAppender;
        String logDirPath;
        File logDir;
        File logFile;

        patternLayout = new PatternLayout ();
        conversionPattern = "%-7p %d [%t] %c %x - %m%n";
        patternLayout.setConversionPattern (conversionPattern);

        consoleAppender = new ConsoleAppender ();
        consoleAppender.setLayout (patternLayout);
        consoleAppender.activateOptions ();


        logDirPath = commandLine.getOutputDir ();
        logDir = new File (logDirPath, SimulatorConstants.OUTPUT_SIMULATION_LOG_DIR.replace 
                                                        ("$ACCOUNTNAME", commandLine.getAccountName ())); 
        logDir.mkdirs ();
        logFile = new File (logDir, SimulatorConstants.OUTPUT_SIMULATORLOG_FILENAME);
 
        fileAppender = new FileAppender ();
        fileAppender.setFile (logFile.getPath());
        fileAppender.setLayout (patternLayout);
        fileAppender.activateOptions ();

        messageLogger = Logger.getRootLogger ();
        switch (commandLine.getMessageLevel()) {
            case "info": messageLogger.setLevel (Level.INFO); break;
            case "debug": messageLogger.setLevel (Level.DEBUG); break;
            case "warn": messageLogger.setLevel (Level.WARN); break;
            case "error": messageLogger.setLevel (Level.ERROR); break;
            default: messageLogger.setLevel (Level.FATAL); break;
        }
        // messageLogger.addAppender (consoleAppender);
        messageLogger.removeAppender ("CONSOLE");
        messageLogger.addAppender (fileAppender);
    }

    public static void logDebug (String msg) {
        if (messageLogger != null)
            messageLogger.debug (msg);
    }

    public static void logInfo (String msg) {
        if (messageLogger != null)
            messageLogger.info (msg);
    }

    public static void logWarning (String msg) {
        if (messageLogger != null)
            messageLogger.warn (msg);
    }

    public static void logError (String msg) {
        if (messageLogger != null)
            messageLogger.error (msg);
    }

    public static void logFatal (String msg) {
        if (messageLogger != null)
            messageLogger.fatal (msg);
    }
}
