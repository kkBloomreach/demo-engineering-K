// generate traffic
// NOTE: within "ROOT" dir (name = 'data'), subfolders are expected to have a predefined substructure
// "ROOT" is provided as an argument to the application
//  <root>/data/source -> {productFeed.tsv, ...}
package com.bloomreach.trafficgenerator;

import java.util.GregorianCalendar;
import java.io.File;

import com.bloomreach.trafficgenerator.visitor.*;
import com.bloomreach.trafficgenerator.site.Site;

public class Generator {

    // factory to instantiate visitors
    private VisitorFactory visitorFactory;

    // site (represents 'store')
    private Site site;

    // signal object used between visitor and site threads
    private VisitorSignal visitorSignal;

    // single calendar for visitor, site,
    private GregorianCalendar calendar;

    public Generator () {
    }

    public static void main (String[] args) {

        GeneratorCommandLine commandLine;
        Generator generator;
        MessageLogger messageLogger;

        System.out.printf ("Traffic Generator, version: %s\n", GeneratorVersion.VERSION);

        commandLine = new GeneratorCommandLine ();
        if (commandLine.parse (args) == false) {
            // help message already shown
            System.exit (-1);
        }

        try {
            String logDirPath;

            // per-account log directory
            logDirPath = String.format ("%s/%s/%s", commandLine.getDataDirPath (), 
                                                    commandLine.getAccountName ().trim(),
                                                    GeneratorConstants.OUTPUT_LOG_DIR);
            messageLogger = new MessageLogger ();
            messageLogger.init (logDirPath, commandLine.getMessageLevel());
            MessageLogger.logInfo (String.format ("Traffic Generator, version: %s\n", GeneratorVersion.VERSION));
        } catch (Exception e) {
            e.printStackTrace ();
            System.out.println ("Exception in creating messageLogger: " + e.getMessage ());
            // don't exit even if this fails
        }

        generator = new Generator ();
        try {
            // read generator data - productFeed, templates, ...
            generator.init (commandLine);
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in generator initialization: " + e.getMessage());
            MessageLogger.close ();
            System.exit (-1);
        }

        // start to generate traffic
        try {
            generator.start ();
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in starting traffic generator : " + e);
            MessageLogger.close ();
            System.exit (-1);
        }

        // close at end (typically end-of-day)
        try {
            generator.closeAtEnd  ();
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in closing traffic generator : " + e);
        }

        MessageLogger.logInfo ("Generator closed");
        MessageLogger.close ();
        System.exit (0);
    }

    // init generator's own data as well as account-specific info
    private void init (GeneratorCommandLine commandLine) throws Exception {
        String rootDirPath;
        String accountName;
        String envType;
        String realm;
        boolean testData;
        boolean pixelDebug;
        boolean curatedJourney;

        rootDirPath = commandLine.getDataDirPath ();  // provided in command line as "-d <arg>"
        accountName = commandLine.getAccountName ().trim();
        envType = commandLine.getEnvType ().trim();
        realm = commandLine.getRealm();
        testData = commandLine.isTestData ();
        pixelDebug = commandLine.isPixelDebug ();
        curatedJourney = commandLine.isCuratedJourney ();

        // init generators own variables etc for this account
        if (initGeneratorData (rootDirPath, accountName, envType, realm, testData, pixelDebug, curatedJourney) == false) {
            MessageLogger.logError ("Generator init unsuccessful");
            throw new Exception ("Generator init unsuccessful");
        }
    }

    private void start () throws Exception {

        try {
            site.open ();
            Thread.currentThread().sleep (100); // just to make sure waiter is waiting...
        } catch (InterruptedException ie) {
            MessageLogger.logDebug ("Generator preparation sleep interrupted...");
        }

        // let visitors start arriving
        visitorFactory.start ();
    }

    private void closeAtEnd  () throws Exception {
        // wait for 'closing time' (total duration - N minutes)
        try {
                long closingTime;

                closingTime = EnvironmentConfig.getEnvParamLong ("TOTAL_GENERATOR_DURATION") + 
                              EnvironmentConfig.getEnvParamLong ("DELAY_BEFORE_GENERATOR_EXIT");
                MessageLogger.logDebug (String.format ("Generator will close in %d millisec", closingTime));
                Thread.currentThread().sleep (closingTime);
        } catch (InterruptedException ie) {
            MessageLogger.logDebug ("Generator closing interrupted...");
        }

        MessageLogger.logDebug ("Generator closing now ...");

        // stop visitorFactory
        visitorFactory.stop ();

        try {
            Thread.currentThread().sleep (500); 
        } catch (InterruptedException ie) {
            MessageLogger.logDebug ("Generator closing interrupted...");
        }

        // then close site
        try {
            site.close ();
        } catch (Exception e) {
            MessageLogger.logDebug ("Site close exception: " + e.getMessage());
        }

        try {
            Thread.currentThread().sleep (500);
        } catch (InterruptedException ie) {
            MessageLogger.logDebug ("Generator closing interrupted...");
        }

        // finally close the MessageLogger
        MessageLogger.close();
    }

    // Init generator's own configs, independent of which account it is,
    private boolean initGeneratorData (String rootDirPath, 
                                       String accountName, 
                                       String envType,
                                       String realm,
                                       boolean testData,
                                       boolean pixelDebug,
                                       boolean curatedJourney) throws Exception {

        this.calendar = new GregorianCalendar ();    // single calendar for visitor, site
        this.calendar.set (GregorianCalendar.HOUR, 0); // midnight, 0:0:1 (hr:min:sec)
        this.calendar.set (GregorianCalendar.MINUTE, 0);
        this.calendar.set (GregorianCalendar.SECOND, 1);

        // environment - dev/qa/release 
        EnvironmentConfig envConfig = new EnvironmentConfig ();
        envConfig.setEnvType (envType);
 
        // signal between visitor and site
        this.visitorSignal = new VisitorSignal ();

        // visitor factory
        this.visitorFactory = new VisitorFactory ();
        try {
            this.visitorFactory.setCalendar (this.calendar);
            this.visitorFactory.setDuration (EnvironmentConfig.getEnvParamLong ("TOTAL_GENERATOR_DURATION"));
            this.visitorFactory.setVisitorSignal (this.visitorSignal);
            this.visitorFactory.init ();
        } catch (Exception e) {
            MessageLogger.logError ("visitorFactory exception: " + e.getMessage ());
            this.visitorFactory = null;
            return false;
        }

        // Site (represents 'store')
        this.site = new Site ();
        try {
            File siteRootDir;

            siteRootDir = new File (rootDirPath, accountName);
            this.site.setRootDir (siteRootDir);
            this.site.setCalendar (this.calendar);
            this.site.setVisitorSignal (this.visitorSignal);
            this.site.init (realm, testData, pixelDebug, curatedJourney);
        } catch (Exception e) {
            MessageLogger.logError ("Site exception: " + e.getMessage ());
            this.site = null;
            return false;
        }

        // special visitor data is in site.config, which is parsed in Site.java
        // provide that special-visitor-data to visitorFactory
        this.visitorFactory.setSpecialVisitorData (this.site.getSpecialVisitorId (),
                                                   this.site.getSpecialVisitDays ());
        return true;
    }
}

