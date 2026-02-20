// reference doc
// https://logging.apache.org/log4j/2.x/manual/customconfig.html

package com.bloomreach.trafficgenerator;

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


public class MessageLogger {

    private static Logger messageLogger = null;

    public MessageLogger () {
    }

    public void init (String logDirPath, String logLevel) throws Exception {
        try {
            initInternal (logDirPath, logLevel);
            messageLogger = LogManager.getLogger (MessageLogger.class);
        } catch (Exception e) {
            System.err.printf ("Failed to create messageLogger, error = %s\n" + e.getMessage());
            messageLogger = null;
        }
    }

    public static void logDebug (String msg) {
        if (messageLogger != null)
            messageLogger.debug (msg);
        else
            System.out.printf ("DEBUG: %s\n", msg);
    }

    public static void logInfo (String msg) {
        if (messageLogger != null)
            messageLogger.info (msg);
        else
            System.out.printf ("INFO: %s\n", msg);
    }

    public static void logWarning (String msg) {
        if (messageLogger != null)
            messageLogger.warn (msg);
        else
            System.out.printf ("WARN: %s\n", msg);
    }

    public static void logError (String msg) {
        if (messageLogger != null)
            messageLogger.error (msg);
        else
            System.err.printf ("ERROR: %s\n", msg);
    }

    public static void logFatal (String msg) {
        if (messageLogger != null)
            messageLogger.fatal (msg);
        else
            System.err.printf ("FATAL: %s\n", msg);
    }

    // flush and close all logs -- DOES NOT HELP to flush tail-end of logs --:(
    public static void close () {
        LogManager.shutdown ();
    }

    private void initInternal (String logDirPath, String logLevel) throws Exception {
        String logMessagePattern;
        File logDir;
        String fileNameRollingPattern;
        File logFile;
 
        ConfigurationBuilder <BuiltConfiguration> configurationBuilder;
        LayoutComponentBuilder layoutBuilder;
        ComponentBuilder sizeBasedPolicyBuilder;
        ComponentBuilder rollingNamePolicyBuilder;
        AppenderComponentBuilder appenderBuilder;
        RootLoggerComponentBuilder rootLoggerBuilder;

        logDir = new File (logDirPath);
        logDir.mkdirs ();

        // primary message builder object
        configurationBuilder = ConfigurationBuilderFactory.newConfigurationBuilder ();

        // appender, along with its attributes + component
        appenderBuilder = configurationBuilder.newAppender ("LogToRollingFile", "RollingFile");
        logFile = new File (logDirPath, GeneratorConstants.OUTPUT_LOG_FILENAME);
        appenderBuilder.addAttribute ("fileName", logFile.getPath());
        fileNameRollingPattern = String.format ("%s%s", logFile.getPath(), "-%d{MM-dd-yy-HH-mm}.log");
        appenderBuilder.addAttribute ("filePattern", fileNameRollingPattern);
        appenderBuilder.addAttribute("immediateFlush", true);

        // file name pattern
        logMessagePattern = "%-7p %d [%t] %c %x - %m%n";
        layoutBuilder = configurationBuilder.newLayout ("PatternLayout");
        layoutBuilder.addAttribute ("pattern", logMessagePattern); 
        appenderBuilder.add (layoutBuilder);

        // rolling-path policy, based on size
        // rolling-policy 'refers to' size-based-policy. Then the rolling-policy added to rolling-file-appender -- such a mess...
        sizeBasedPolicyBuilder = configurationBuilder.newComponent ("SizeBasedTriggeringPolicy");
        sizeBasedPolicyBuilder.addAttribute ("size", "5MB");
        rollingNamePolicyBuilder = configurationBuilder.newComponent ("Policies");
        rollingNamePolicyBuilder.addComponent (sizeBasedPolicyBuilder);
        appenderBuilder.addComponent (rollingNamePolicyBuilder);

        // add appender to configurationBuilder
        configurationBuilder.add (appenderBuilder);

        // create a 'root' logger
        switch (logLevel) {
            case "info": rootLoggerBuilder = configurationBuilder.newRootLogger (Level.INFO); break;
            case "debug": rootLoggerBuilder = configurationBuilder.newRootLogger (Level.DEBUG); break;
            case "warn": rootLoggerBuilder = configurationBuilder.newRootLogger (Level.WARN); break;
            case "error": rootLoggerBuilder = configurationBuilder.newRootLogger (Level.ERROR); break;
            default: rootLoggerBuilder = configurationBuilder.newRootLogger (Level.FATAL);
        }

        // default 'root', set this as 'appenderRef' for configurationBuilder
        // then add its child (configurationBuilder.build)
        rootLoggerBuilder.add (configurationBuilder.newAppenderRef ("LogToRollingFile"));
        configurationBuilder.add (rootLoggerBuilder);

        // configurationBuilder name, level
        // Note: log4j itself writes many "DEBUG" logs to CONSOLE. In order to avoid seeing those in our
        // log file/console, use Level.INFO
        configurationBuilder.setConfigurationName ("DefaultRollingFileLogger");
        configurationBuilder.setStatusLevel (Level.INFO); 

        // finally build a configuration object and let Configurator associate it with LoggerContext
        Configurator.initialize (configurationBuilder.build());  

        // xml out
        // configurationBuilder.writeXmlConfiguration (System.out);
    }

}

