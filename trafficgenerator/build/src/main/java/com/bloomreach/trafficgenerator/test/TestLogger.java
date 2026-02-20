package com.bloomreach.trafficgenerator.test;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import com.bloomreach.trafficgenerator.GeneratorConstants;

public class TestLogger {

    private static Logger messageLogger_static;
    private Logger messageLogger;

    public static void main (String[] args) {
        TestLogger testLogger = null;

        if (args.length < 1) {
            System.err.println ("Must provide directory path, eg: /tmp");
            System.exit (-1);
        }

        try {
            testLogger = new TestLogger ();
            testLogger.init (args[0], "debug");
        } catch (Exception e) {
            e.printStackTrace ();
            System.exit (-1);
        }

        try {
            for (int i = 0; i < 10; i++) {
                testLogger.logDebug ("Test Logger Debug " + i);
                testLogger.logError ("Test Logger Error " + i);
            }
        } catch (Exception e) {
            e.printStackTrace ();
        }

        testLogger.close ();
 
        System.out.println ("Finish...");
        System.exit (0);
    }

    private TestLogger () {
    }

    public void init (String logDirPath, String logLevel) throws Exception {
        try {
            initInternal (logDirPath, logLevel);
            this.messageLogger = LogManager.getLogger (TestLogger.class);
        } catch (Exception e) {
            System.err.printf ("Failed to create messageLogger, error = %s\n", e.getMessage ());
            throw new Exception ("Init failed");
        }
    }

    public void logDebug (String msg) {
        if (this.messageLogger != null) 
            this.messageLogger.debug (msg);
        else
            System.out.printf ("Debug message: %s\n" + msg);
    }

    public void logError (String msg) {
        if (this.messageLogger != null) 
            this.messageLogger.error (msg);
        else
            System.out.printf ("Error message: %s\n" + msg);
    }

    public void close () {
        try {
            LogManager.shutdown ();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    public void initInternal (String logDirPath, String logLevel) throws Exception {
        String logMessagePattern;
        File logDir;
        String fileNameRollingPattern;
        File logFile;
 
        ConfigurationBuilder <BuiltConfiguration> messageBuilder;
        LayoutComponentBuilder layoutBuilder;
        ComponentBuilder sizeBasedPolicyBuilder;
        ComponentBuilder rollingNamePolicyBuilder;
        AppenderComponentBuilder appenderBuilder;
        RootLoggerComponentBuilder rootLoggerBuilder;

        logDir = new File (logDirPath);
        logDir.mkdirs ();

        // primary message builder object
        messageBuilder = ConfigurationBuilderFactory.newConfigurationBuilder ();

        // appender, along with its attributes + component
        appenderBuilder = messageBuilder.newAppender ("LogToRollingFile", "RollingFile");
        logFile = new File (logDirPath, GeneratorConstants.OUTPUT_LOG_FILENAME);
        appenderBuilder.addAttribute ("fileName", logFile.getPath()); // GeneratorConstants.OUTPUT_LOG_FILENAME);
        fileNameRollingPattern = String.format ("%s%s", logFile.getPath(), "-%d{MM-dd-yy}.log");
        appenderBuilder.addAttribute ("filePattern", fileNameRollingPattern);

        logMessagePattern = "%-7p %d [%t] %c %x - %m%n";
        layoutBuilder = messageBuilder.newLayout ("PatternLayout");
        layoutBuilder.addAttribute ("pattern", logMessagePattern); 
        appenderBuilder.add (layoutBuilder);

        // rolling-path policy, based on size
        sizeBasedPolicyBuilder = messageBuilder.newComponent ("SizeBasedTriggeringPolicy");
        sizeBasedPolicyBuilder.addAttribute ("size", "1MB");
        rollingNamePolicyBuilder = messageBuilder.newComponent ("Policies");
        rollingNamePolicyBuilder.addComponent (sizeBasedPolicyBuilder);
        appenderBuilder.addComponent (rollingNamePolicyBuilder);

        // add appender to message builder
        messageBuilder.add (appenderBuilder);

        switch (logLevel) {
            case "info": rootLoggerBuilder = messageBuilder.newRootLogger (Level.INFO); break;
            case "debug": rootLoggerBuilder = messageBuilder.newRootLogger (Level.DEBUG); break;
            case "warn": rootLoggerBuilder = messageBuilder.newRootLogger (Level.WARN); break;
            case "error": rootLoggerBuilder = messageBuilder.newRootLogger (Level.ERROR); break;
            default: rootLoggerBuilder = messageBuilder.newRootLogger (Level.FATAL);
        }

        // default 'root', set this as 'appenderRef' for messageBuilder
        rootLoggerBuilder.add (messageBuilder.newAppenderRef ("LogToRollingFile"));
        messageBuilder.add (rootLoggerBuilder);

        // finally configure 
        // message builder name, level
        messageBuilder.setConfigurationName ("DefaultRollingFileLogger");
        messageBuilder.setStatusLevel (Level.DEBUG); 
        Configurator.initialize (messageBuilder.build());

        // xml out
        // messageBuilder.writeXmlConfiguration (System.out);
    }
}
