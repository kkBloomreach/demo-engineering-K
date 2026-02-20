package com.bloomreach.trafficgenerator.site;

import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.io.File;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.EnvironmentConfig;
import com.bloomreach.trafficgenerator.site.journeydata.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.*;
import com.bloomreach.trafficgenerator.site.journeydata.customjourney.*;
import com.bloomreach.trafficgenerator.site.feed.*;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.journeylogs.*;
import com.bloomreach.trafficgenerator.site.dispatch.*;
import com.bloomreach.trafficgenerator.site.config.*;
import com.bloomreach.trafficgenerator.site.journey.*;
import com.bloomreach.trafficgenerator.site.user.*;

import com.bloomreach.trafficgenerator.visitor.Visitor;
import com.bloomreach.trafficgenerator.visitor.VisitorSignal;

public class Site {

    private File siteRootDir;
    private GregorianCalendar calendar;
    private VisitorSignal visitorSignal;

    private UserManager userManager;
    private JourneyBuilder journeyBuilder;  // builds individual journeyGenerator, one-per-visitor
    private VisitorListenerThread listenerThread;
    private DailyLog dailyLog;
    private WidgetLog widgetLog;
    private PixelCountLog pixelCountLog;
    private ApiCountLog apiCountLog;
    private SiteVisitorMonitor siteVisitorMonitor;  // control number of active visitors in the site

    private int debugTotalVisitors = 0;

    public Site () {
    }

    public void setRootDir (File siteRootDir) {
       this.siteRootDir = siteRootDir; 
    }

    // site-wide calendar
    public void setCalendar (GregorianCalendar calendar) {
        this.calendar = calendar;
    }

    public void setVisitorSignal (VisitorSignal visitorSignal) {
        this.visitorSignal = visitorSignal;
    }

    // param 'siteRootDir' in turn has sub folders (eg, 'config')
    public void init (String realm, boolean testData, boolean pixelDebug) throws Exception {
        // Read site-specific-config 
        if (this.siteRootDir.exists () == false) {
            throw new Exception ("Cannot find siteRoot = " + this.siteRootDir.getPath());
        }

        if (initSiteData (realm, testData, pixelDebug) == false) {
            throw new Exception ("Init site failed, rootDir = " + this.siteRootDir.getPath());
        }
    }

    public String getSpecialVisitorId () {
        return SiteConfig.getSpecialVisitorId ();   // may be null
    }

    public ArrayList<Integer> getSpecialVisitDays () {
        return SiteConfig.getSpecialVisitDays ();   // may be null
    }

    public void open () throws Exception {
        // start listener thread to welcome site visitors
        this.listenerThread = new VisitorListenerThread ();
        this.listenerThread.setName ("Site VisitorHandler");
        this.listenerThread.setSiteVisitorMonitor (this.siteVisitorMonitor);
        this.listenerThread.setJourneyBuilder (this.journeyBuilder);
        this.listenerThread.setUserManager (this.userManager);
        this.listenerThread.setDailyLog (this.dailyLog);
        this.listenerThread.setSpecialVisitorId (SiteConfig.getSpecialVisitorId ());    //may be null
        this.listenerThread.setVisitorSignal (this.visitorSignal);
        this.listenerThread.start ();
    }

    public void close () throws Exception {
        if (this.listenerThread != null)
            this.listenerThread.interrupt ();

        this.listenerThread = null;
        this.dailyLog.closeUserLog ();
        this.widgetLog.close ();
        this.pixelCountLog.close();
        this.apiCountLog.close();
        
        //debug print totalVisitors, atc's, conversions. Helps to quickly check daily visitor stats
        MessageLogger.logInfo (String.format ( "TotalVisitors: %d, total ATCs: %d, total Conversions: %d", 
                                                debugTotalVisitors, 
                                                StepsHandler.debugTotalATCs, 
                                                StepsHandler.debugTotalConversions));
    }

    // use siteConfig params to load site data. 
    // IMPORTANT - sequence of init's is important due to cross-dependencies
    private boolean initSiteData (String realm, boolean testData, boolean pixelDebug) throws Exception {
        File configFile = null;
        SiteConfig siteConfig = null;
        CampaignRecord activeCampaignRecord = null;

        // loads config.json file and creates a static object in SiteConfig class
        siteConfig = new SiteConfig ();
        configFile = new File (this.siteRootDir, GeneratorConstants.INPUT_SITE_CONFIG_PATH);
        if (siteConfig.load (configFile.getPath ()) == false) {
            throw new Exception ("Cannot find siteConfig");
        }

        // campaigns map - need to do this before alterFeed since campaign, if any,
        // will require price/sale-price changes
        CampaignsConfig campaignsConfig = new CampaignsConfig ();
        try {
            File campaignsConfigFile;

            campaignsConfig.setRealm (realm);   // set this before load since realm is used to make API calls
            campaignsConfigFile = new File (this.siteRootDir, GeneratorConstants.INPUT_CAMPAIGNS_CONFIG_PATH);
            if (campaignsConfigFile.exists ()) {
                if (campaignsConfig.load (campaignsConfigFile.getPath()) == false) {
                    MessageLogger.logError ("campaigns load failed");
                    return false;
                }
                // see if we are 'in a campaign'
                activeCampaignRecord = detectActiveCampaignIfAny (campaignsConfig);
            }
        } catch (Exception e) {
            MessageLogger.logError ("campaigns map exception: " + e.getMessage ());
            return false;
        }

        // alter feed as per siteConfig and campaigns map (if any). 
        try {
            alterFeed (realm, activeCampaignRecord);
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in feed alteration: " + e);
            return false;
        }

        // productFeed - needed to pick up product's price for conversion pixel
        // IMPORTANT - Do this after alterFeed
        ProductFeed productFeed = new DailyProductFeed ();
        try {
            File productFeedDir;
            File productFeedFile;
            productFeedDir = new File (this.siteRootDir, GeneratorConstants.INPUT_DAILY_FEED_DIR);
            productFeedFile = new File (productFeedDir, GeneratorConstants.INPUT_DAILY_JSONL_FEED_FILE_NAME);
            productFeed.load (productFeedFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("ProductFeed exception: " + e.getMessage ());
            return false;
        }

        // categoryCollector - need to collect category list using API call (instead of feed)
        CategoryCollector categoryCollector = new CategoryCollector ();
        try {
            categoryCollector.collectCategories (realm);
        } catch (Exception e) {
            MessageLogger.logError ("CategoryCollector exception: " + e.getMessage ());
            return false;
        }

        // searchTerms, expects terms-with-refinements
        SearchTerms searchTerms = new SearchTerms ();
        try {
            File searchTermsFile;
            searchTermsFile = new File (this.siteRootDir, GeneratorConstants.INPUT_SEARCH_TERM_WITH_REFINEMENTS_PATH);
            // provide ActiveCampaignRecord to searchTerms- campaign may be null
            // do this call before doLoad since doLoad uses activeCampaignRecord 
            searchTerms.setActiveCampaignRecord (activeCampaignRecord);

            searchTerms.doLoad (searchTermsFile.getPath());

        } catch (Exception e) {
            MessageLogger.logError ("SearchTerms exception: " + e.getMessage ());
            return false;
        }

        // suggestTerms
        SuggestTerms suggestTerms = new SuggestTerms ();
        try {
            File suggestTermsFile;
            suggestTermsFile = new File (this.siteRootDir, GeneratorConstants.INPUT_SUGGEST_TERM_PATH);
            suggestTerms.doLoad (suggestTermsFile.getPath());

        } catch (Exception e) {
            MessageLogger.logError ("SearchTerms exception: " + e.getMessage ());
            return false;
        }

        // zeroResultSearchTerms 
        ZeroResultSearchTerms zeroResultSearchTerms = new ZeroResultSearchTerms ();
        try {
            File zeroResultSearchTermsFile;
            zeroResultSearchTermsFile = new File (this.siteRootDir, GeneratorConstants.INPUT_ZERO_RESULT_SEARCH_TERM_PATH);
            zeroResultSearchTerms.doLoad (zeroResultSearchTermsFile.getPath());

        } catch (Exception e) {
            MessageLogger.logError ("ZeroResultSearchTerms exception: " + e.getMessage ());
            return false;
        }

        // searchCategories -- Uses productFeed
        SearchCategories searchCategories = new SearchCategories ();
        try {
            searchCategories.setCategoryCollector (categoryCollector);
            searchCategories.setExcludeCategoryIds (siteConfig.getExcludeCategoryIds());
            searchCategories.doLoad ();

        } catch (Exception e) {
            MessageLogger.logError ("SearchCatgeries exception: " + e.getMessage ());
            return false;
        }

        // startUrl pool - loads from productFeed and categoryCollector
        StartUrlPool startUrlPool = new StartUrlPool ();
        try {
            startUrlPool.setProductFeed (productFeed);
            startUrlPool.setCategoryCollector (categoryCollector);
            startUrlPool.setSearchTerms (searchTerms);
            startUrlPool.doLoad ();

        } catch (Exception e) {
            MessageLogger.logError ("startUrlPool exception: " + e.getMessage ());
            return false;
        }

        // startRefUrl pool - loads social, search-engine urls
        StartRefUrlPool startRefUrlPool = new StartRefUrlPool ();
        try {
            startRefUrlPool.doLoad ();
        } catch (Exception e) {
            MessageLogger.logError ("startRefUrlPool exception: " + e.getMessage ());
            return false;
        }

        // widget configs
        WidgetConfigs widgetConfigs = new WidgetConfigs ();
        try {
            File widgetConfigsFile;

            widgetConfigsFile = new File (this.siteRootDir, GeneratorConstants.INPUT_WIDGET_CONFIGS_PATH);
            if (widgetConfigsFile.exists ()) {
                if (widgetConfigs.load (widgetConfigsFile.getPath()) == false) {
                    MessageLogger.logError ("widgetConfigs load failed");
                    return false;
                }
            }
        } catch (Exception e) {
            MessageLogger.logError ("widgetConfigs exception: " + e.getMessage ());
            return false;
        }

        // custom journeys
        CustomJourney customJourney = new CustomJourney();
        try {
            File customJourneyConfigFile;

            customJourneyConfigFile = new File (this.siteRootDir, GeneratorConstants.INPUT_CUSTOM_JOURNEY_CONFIGS_PATH);
            if (customJourneyConfigFile.exists()) {
                if (customJourney.load (customJourneyConfigFile.getPath()) == false) {
                    MessageLogger.logError ("customJourney config load failed");
                    return false;
                }
            }
        } catch (Exception e) {
            MessageLogger.logError ("customJourney configs exception: " + e.getMessage ());
            return false;
        }

        // productSelector - select product based on RTS-rules (or, default)
        ProductSelector productSelector = new ProductSelector ();
        try {
            productSelector.prepareSelector ();
        } catch (Exception e) {
            MessageLogger.logError ("productSelector exception: " + e.getMessage ());
            return false;
        }

        // widgetTraffic handler
        WidgetHandler widgetHandler = new WidgetHandler ();
        try {
            widgetHandler.setWidgetConfigs (widgetConfigs);
        } catch (Exception e) {
            MessageLogger.logError ("widgetHandler exception: " + e.getMessage ());
            return false;
        }

        // OrderId generator - updated for each day
        OrderIdGenerator orderIdGenerator = new OrderIdGenerator ();
        orderIdGenerator.setDate (this.calendar.get (GregorianCalendar.YEAR),
                                  this.calendar.get (GregorianCalendar.MONTH)+1,
                                  this.calendar.get (GregorianCalendar.DAY_OF_MONTH));

        // network dispatcher
        Dispatcher dispatcher = new Dispatcher ();
        try {
            dispatcher.setRealm (realm);
            dispatcher.setRegion (SiteConfig.getAccountConfigParam ("REGION"));
            dispatcher.setPixelDebug (pixelDebug);
            dispatcher.setExcludeProducts (siteConfig.getExcludeProducts ());
            dispatcher.init ();
        } catch (Exception e) {
            MessageLogger.logError ("Dispatcher exception: " + e.getMessage ());
            return false;
        }

        // siteStats (specific to an account). Set class-level
        this.dailyLog = new DailyLog ();
        try {
            File dailyLogDir;
            File dailyLogFile;
            String dailyLogFileName;

            dailyLogDir = new File (siteRootDir, GeneratorConstants.OUTPUT_DAILYLOG_DIR);
            dailyLogDir.mkdirs ();
            dailyLogFileName = String.format ("%s_%s_%s_%s.tsv", GeneratorConstants.OUTPUT_DAILYLOG_FILENAME_PREAMBLE, 
                                                                 this.calendar.get (GregorianCalendar.DAY_OF_MONTH),
                                                                 this.calendar.get (GregorianCalendar.MONTH)+1,
                                                                 this.calendar.get (GregorianCalendar.YEAR));
            dailyLogFile = new File (dailyLogDir, dailyLogFileName);
            this.dailyLog.setLogPath (dailyLogFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("DailyLog creation exception: " + e.getMessage ());
            return false;
        }

        // widget logger
        this.widgetLog = new WidgetLog ();
        try {
            File widgetLogDir;
            File widgetLogFile;
            String widgetLogFileName;

            widgetLogDir = new File (siteRootDir, GeneratorConstants.OUTPUT_WIDGETLOG_DIR);
            widgetLogDir.mkdirs();
            widgetLogFileName = String.format ("%s_%s_%s_%s.tsv", GeneratorConstants.OUTPUT_WIDGETLOG_FILENAME_PREAMBLE, 
                                                                 this.calendar.get (GregorianCalendar.DAY_OF_MONTH),
                                                                 this.calendar.get (GregorianCalendar.MONTH)+1,
                                                                 this.calendar.get (GregorianCalendar.YEAR));
            widgetLogFile = new File (widgetLogDir, widgetLogFileName);
            this.widgetLog.setLogPath(widgetLogFile.getPath());
        } catch (Exception e) {
            MessageLogger.logError ("WidgetLog creation exception: " + e.getMessage ());
            return false;
        }

        // pixelCountLog
        this.pixelCountLog = new PixelCountLog();
        try {
            File pixelCountLogDir;
            File pixelCountLogFile;
            String pixelCountLogFileName;

            pixelCountLogDir = new File (siteRootDir, GeneratorConstants.OUTPUT_PIXELCOUNTLOG_DIR);
            pixelCountLogDir.mkdirs();
            pixelCountLogFileName = String.format ("%s_%s_%s_%s.tsv", GeneratorConstants.OUTPUT_PIXELCOUNTLOG_FILENAME_PREAMBEL, 
                                                                 this.calendar.get (GregorianCalendar.DAY_OF_MONTH),
                                                                 this.calendar.get (GregorianCalendar.MONTH)+1,
                                                                 this.calendar.get (GregorianCalendar.YEAR));
            pixelCountLogFile = new File (pixelCountLogDir, pixelCountLogFileName);
            this.pixelCountLog.setLogPath(pixelCountLogFile.getPath());
            this.pixelCountLog.start(); // get ready to start collecting counts
        } catch (Exception e) {
            MessageLogger.logError ("pixelCountLog creation exception: " + e.getMessage ());
            return false;
        }

        // apiCountLog
        this.apiCountLog = new ApiCountLog();
        try {
            File apiCountLogDir;
            File apiCountLogFile;
            String apiCountLogFileName;

            apiCountLogDir = new File (siteRootDir, GeneratorConstants.OUTPUT_APICOUNTLOG_DIR);
            apiCountLogDir.mkdirs();
            apiCountLogFileName = String.format ("%s_%s_%s_%s.tsv", GeneratorConstants.OUTPUT_APICOUNTLOG_FILENAME_PREAMBEL, 
                                                                 this.calendar.get (GregorianCalendar.DAY_OF_MONTH),
                                                                 this.calendar.get (GregorianCalendar.MONTH)+1,
                                                                 this.calendar.get (GregorianCalendar.YEAR));
            apiCountLogFile = new File (apiCountLogDir, apiCountLogFileName);
            this.apiCountLog.setLogPath(apiCountLogFile.getPath());
            this.apiCountLog.start(); // get ready to start collecting counts
        } catch (Exception e) {
            MessageLogger.logError ("apiCountLog creation exception: " + e.getMessage ());
            return false;
        }

        // pixel templates
        PixelTemplates pixelTemplates = new PixelTemplates ();

        // api templates, used to generate proper api calls
        ApiTemplates apiTemplates = new ApiTemplates ();

        // traffic steps are really NOT 'site' specific. However, for now initialize here
        TrafficSteps trafficSteps = new TrafficSteps ();
        try {
            File trafficStepsFile;
            trafficStepsFile = new File (this.siteRootDir, GeneratorConstants.INPUT_TRAFFICSTEPS_PATH);
            trafficSteps.doLoad (trafficStepsFile.getPath());

        } catch (Exception e) {
            MessageLogger.logError ("Trafficsteps exception: " + e.getMessage ());
            return false;
        }

        // userManager (to login new visitor)
        this.userManager = new UserManager ();
        this.userManager.setSpecialVisitorId (SiteConfig.getSpecialVisitorId ());   // may be null

        // site visitor monitor 
        this.siteVisitorMonitor = new SiteVisitorMonitor ();
 
        // journeyBuilder - this needs other objects created above
        JourneyBuilder journeyBuilder;
        try {
            journeyBuilder = new JourneyBuilder ();

            journeyBuilder.setActiveCampaignRecord (activeCampaignRecord);
            journeyBuilder.setProductFeed (productFeed);
            journeyBuilder.setCategoryCollector (categoryCollector);
            journeyBuilder.setTestData (testData);
            journeyBuilder.setSearchTerms (searchTerms);
            journeyBuilder.setSearchCategories (searchCategories);
            journeyBuilder.setSuggestTerms (suggestTerms);
            journeyBuilder.setZeroResultSearchTerms (zeroResultSearchTerms);
            journeyBuilder.setProductSelector (productSelector);
            journeyBuilder.setWidgetHandler (widgetHandler);
            journeyBuilder.setOrderIdGenerator (orderIdGenerator);
            journeyBuilder.setDispatcher (dispatcher);
            journeyBuilder.setPixelTemplates (pixelTemplates);
            journeyBuilder.setApiTemplates (apiTemplates);
            journeyBuilder.setTrafficSteps (trafficSteps);
            journeyBuilder.setStartUrlPool (startUrlPool);
            journeyBuilder.setStartRefUrlPool (startRefUrlPool);
            journeyBuilder.setWidgetHandler (widgetHandler);
            journeyBuilder.setCustomJourney (customJourney);
            journeyBuilder.setWidgetLogger (widgetLog);
            journeyBuilder.setPixelCountLog(pixelCountLog);
            journeyBuilder.setApiCountLog (apiCountLog);

            // set at class level
            this.journeyBuilder = journeyBuilder;
        } catch (Exception e) {
            e.printStackTrace ();
            MessageLogger.logFatal ("Exception in init journeyBuilder: " + e);
            throw new Exception ("JourneyBuilder build failed");
        }

        return true;
    }

    // returns campaignRecord if any is currently active
    private CampaignRecord detectActiveCampaignIfAny (CampaignsConfig campaignsConfig)  {
        CampaignRecord activeCampaignRecord = null;  

        if (campaignsConfig != null) {
            long midnightTime;
            GregorianCalendar detectionCalendar;    // used only to detect campaign if any

            detectionCalendar = new GregorianCalendar ();
            detectionCalendar.set (GregorianCalendar.HOUR, 0); // midnight, 0:0:1 (hr:min:sec)
            detectionCalendar.set (GregorianCalendar.MINUTE, 0);
            detectionCalendar.set (GregorianCalendar.SECOND, 1);
            midnightTime = detectionCalendar.getTimeInMillis ();

            for (CampaignRecord campaignRecord : campaignsConfig.getCampaignRecords ()) {
                int campaignStartMonth;
                int campaignStartDay;
                long campaignStartInMillis;
                long campaignEndInMillis;

                campaignStartMonth = campaignRecord.getStartMonth ();
                campaignStartDay = campaignRecord.getStartDay ();
                detectionCalendar.set (GregorianCalendar.MONTH, campaignStartMonth-1); // month starts from 0
                detectionCalendar.set (GregorianCalendar.DAY_OF_MONTH, campaignStartDay);
                campaignStartInMillis = detectionCalendar.getTimeInMillis ();

                detectionCalendar.add (GregorianCalendar.DAY_OF_MONTH, campaignRecord.getDayCount ());
                campaignEndInMillis = detectionCalendar.getTimeInMillis ();

                if ((midnightTime >= campaignStartInMillis) && (midnightTime < campaignEndInMillis)) {
                    MessageLogger.logInfo (String.format ("Detected current campaign: %s", campaignRecord.getCampaignName ()));
                    activeCampaignRecord = campaignRecord;
                    break;
                }
            }
        }

        return activeCampaignRecord;    // may be null
    }

    private void alterFeed (String realm, CampaignRecord activeCampaignRecord) throws Exception {
        String envType;

        // if we have a dataConnect API key, first alter the feed as needed and then index it
        // If we don't have dataConnect API key, alter the feed but cannot index it. Alter
        // side-effect is to copy 'original' feed to 'daily' 
        envType = EnvironmentConfig.getEnvType ();
        // skip this in dev env
        if (envType.equals (EnvironmentConfig.ENV_TYPE_QA) || envType.equals (EnvironmentConfig.ENV_TYPE_RELEASE)) {
            FeedAlterator feedAlterator;

            feedAlterator = new FeedAlterator ();
            feedAlterator.setCampaignRecord (activeCampaignRecord); // param may be null
            feedAlterator.alterFeed (this.siteRootDir.getPath(), realm);
        }
    }

    //////// 
    // internal site's own listener thread
    class VisitorListenerThread extends Thread {
        private VisitorSignal visitorSignal;
        private DailyLog dailyLog;
        private UserManager userManager;
        private JourneyBuilder journeyBuilder;
        private String specialVisitorId;
        private SiteVisitorMonitor siteVisitorMonitor;

        VisitorListenerThread () {
            setDaemon (true);
        }

        public void setSiteVisitorMonitor (SiteVisitorMonitor siteVisitorMonitor) {
            this.siteVisitorMonitor = siteVisitorMonitor;
        }

        public void setVisitorSignal (VisitorSignal visitorSignal) {
            this.visitorSignal = visitorSignal;
        }

        public void setDailyLog (DailyLog dailyLog) {
            this.dailyLog = dailyLog;
        }

        public void setUserManager (UserManager userManager) {
            this.userManager = userManager;
        }

        public void setJourneyBuilder (JourneyBuilder journeyBuilder) {
            this.journeyBuilder = journeyBuilder;
        }

        public void setSpecialVisitorId (String specialVisitorId) {
            this.specialVisitorId = specialVisitorId;
        }

        public void run () {
            do {
                Visitor visitor;
                try {
                    synchronized (this.visitorSignal) {
                        this.visitorSignal.wait ();
                        visitor = this.visitorSignal.getVisitor ();
                        startNewVisitorThread (visitor);
                    }                    
                } catch (InterruptedException e) {
                    MessageLogger.logWarning ("Site visitorListner thread interrupted");
                    break;
                } catch (Exception e) {
                    e.printStackTrace ();
                    MessageLogger.logError ("Site visitorListner thread exception");
                    break;
                }
            } while (isInterrupted() == false);

            MessageLogger.logInfo ("Finish site visitor listener"); 
        }

        private void startNewVisitorThread (Visitor visitor) {
            VisitorHandlerThread visitorHandlerThread;
            UserRecord userRecord;
            UserLog userLog;    // collect user journey, sessions, steps, ...

            if (this.siteVisitorMonitor.canVisitorEnter () == true) {
                // currently, all visitors perform 'login'
                userRecord = this.userManager.login (visitor);
                userLog = this.dailyLog.addUserLog (userRecord);    // user's journey log etc

                visitorHandlerThread = new VisitorHandlerThread ();
                visitorHandlerThread.setName ("UserId: " + userRecord.getUserId ());
                visitorHandlerThread.setSiteVisitorMonitor (this.siteVisitorMonitor);
                visitorHandlerThread.setJourneyBuilder (this.journeyBuilder);
                visitorHandlerThread.setUserRecord (userRecord);
                visitorHandlerThread.setUserArrivalTime (visitor.getArrivalTime ());
                visitorHandlerThread.setUserLog (userLog); 
                visitorHandlerThread.setSpecialVisitorId (this.specialVisitorId); 
                visitorHandlerThread.start ();
                this.siteVisitorMonitor.enterVisitor (userRecord.getUserId());
                // for quick check of daily visitor count
                debugTotalVisitors = debugTotalVisitors + 1;
            } else {
                MessageLogger.logWarning ("Visitor entry not allowed, active visitors exceed limit");
            }
        }
    }
}

