package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildPixelBase;
import com.bloomreach.trafficgenerator.site.build.pixelparams.OrderIdGenerator;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.DiscoveryUserAccess;
import com.bloomreach.trafficgenerator.site.feed.ProductFeed;
import com.bloomreach.trafficgenerator.site.journeydata.CategoryCollector;
import com.bloomreach.trafficgenerator.site.journeydata.SearchCategories;
import com.bloomreach.trafficgenerator.site.journeydata.SearchTerms;
import com.bloomreach.trafficgenerator.site.journeydata.StartRefUrlPool;
import com.bloomreach.trafficgenerator.site.journeydata.StartUrlPool;
import com.bloomreach.trafficgenerator.site.journeydata.SuggestTerms;
import com.bloomreach.trafficgenerator.site.journeydata.TrafficSteps;
import com.bloomreach.trafficgenerator.site.journeydata.ZeroResultSearchTerms;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.journeydata.customjourney.CustomJourney;
import com.bloomreach.trafficgenerator.site.journeydata.CuratedSearchTerms;
import com.bloomreach.trafficgenerator.site.journeydata.templates.ApiTemplates;
import com.bloomreach.trafficgenerator.site.journeydata.templates.PixelTemplates;
import com.bloomreach.trafficgenerator.site.journeylogs.ApiCountLog;
import com.bloomreach.trafficgenerator.site.journeylogs.PixelCountLog;
import com.bloomreach.trafficgenerator.site.journeylogs.WidgetLog;

public class JourneyBuilder {

    // productFeed
    private ProductFeed productFeed;

    // categoryCollector
    private CategoryCollector categoryCollector;

    // OrderId generator
    private OrderIdGenerator orderIdGenerator;

    // product selector 
    private ProductSelector productSelector;

    // startUrl pool
    private StartUrlPool startUrlPool;

    // startRefUrl pool
    private StartRefUrlPool startRefUrlPool;

    // searchTerms
    private SearchTerms searchTerms;

    // suggestTerms
    private SuggestTerms suggestTerms;

    // zeroResultQuery map
    private ZeroResultSearchTerms zeroResultSearchTerms;

    // search categories
    private SearchCategories searchCategories;

    // curated search terms
    private CuratedSearchTerms curatedSearchTerms;

    // templates for product / atc / conversion pixels
    private PixelTemplates pixelTemplates;

    // templates for search, category APIlogs
    private ApiTemplates apiTemplates;

    // generator client, instatiated once
    private DiscoveryUserAccess DiscoveryUserAccess;

    // campaignRecord, if any. Value may be null
    private CampaignRecord activeCampaignRecord;

    // trafficSteps
    private TrafficSteps trafficSteps;

    // widget handler
    private WidgetHandler widgetHandler;

    // custom journey
    private CustomJourney customJourney;

    // widget logger
    private WidgetLog widgetLog;

    // pixelCount and apiCount log - not saved in this class.
    // provide to PixelBase, DiscoveryUserAccess class as static object
    // private PixelCountLog pixelCountLog;

    // testData provided via commandLine
    private boolean testData;

    public JourneyBuilder () {
    }

    public void setProductFeed (ProductFeed productFeed) {
        this.productFeed = productFeed;
    }

    public void setCategoryCollector (CategoryCollector categoryCollector) {
        this.categoryCollector = categoryCollector;
    }

    public void setOrderIdGenerator (OrderIdGenerator generator) {
        this.orderIdGenerator = generator;
    }

    public void setProductSelector (ProductSelector productSelector) {
        this.productSelector = productSelector;
    }

    public void setSearchTerms (SearchTerms searchTerms) {
        this.searchTerms = searchTerms;
    }

    public void setSuggestTerms (SuggestTerms suggestTerms) {
        this.suggestTerms = suggestTerms;
    }

    public void setZeroResultSearchTerms (ZeroResultSearchTerms zeroResultSearchTerms) {
        this.zeroResultSearchTerms= zeroResultSearchTerms;
    }

    public void setSearchCategories (SearchCategories searchCategories) {
        this.searchCategories = searchCategories;
    }

    // curated-terms needed only for CuratedJourney
    public void setCuratedSearchTerms (CuratedSearchTerms curatedSearchTerms) {
        this.curatedSearchTerms = curatedSearchTerms;
    }

    public void setStartUrlPool (StartUrlPool startUrlPool) {
        this.startUrlPool = startUrlPool;
    }

    public void setStartRefUrlPool (StartRefUrlPool startRefUrlPool) {
        this.startRefUrlPool = startRefUrlPool;
    }

    public void setPixelTemplates (PixelTemplates pixelTemplates) {
        this.pixelTemplates = pixelTemplates;
    }

    public void setApiTemplates (ApiTemplates apiTemplates) {
        this.apiTemplates = apiTemplates;
    }

    public void setDispatcher (DiscoveryUserAccess DiscoveryUserAccess) {
        this.DiscoveryUserAccess = DiscoveryUserAccess;
    }

    public void setActiveCampaignRecord (CampaignRecord activeCampaignRecord) {
        this.activeCampaignRecord = activeCampaignRecord;
    }

    public void setTrafficSteps (TrafficSteps trafficSteps) {
        this.trafficSteps = trafficSteps;
    }

    public void setTestData (boolean testData)  {
        this.testData = testData;
    }

    public void setWidgetHandler (WidgetHandler widgetHandler)  {
        this.widgetHandler = widgetHandler;
    }

    public void setCustomJourney (CustomJourney customJourney) {
        this.customJourney = customJourney;
    }

    public void setWidgetLogger (WidgetLog widgetLog)  {
        this.widgetLog = widgetLog;
    }

    public void setPixelCountLog (PixelCountLog pixelCountLog) {
        // Since pixelCountLog is across entire site, unrelated to any specific 'journey'
        // set this value in PixelBase class once
        BuildPixelBase.setPixelCountLog(pixelCountLog);
    }

    public void setApiCountLog (ApiCountLog apiCountLog) {
        // Since apiCountLog is across entire site, unrelated to any specific 'journey'
        // set this value in DiscoveryUserAccess class once
        this.DiscoveryUserAccess.setApiCountLog(apiCountLog);
    }

    public PredefinedJourneyGenerator buildPredefinedJourneyGenerator () {
        PredefinedJourneyGenerator predefinedJourneyGenerator;
        StepsHandler stepsHandler;

        stepsHandler = prepareStepsHandler ();
 
        // prepare predefinedJourneyGenerator
        predefinedJourneyGenerator = new PredefinedJourneyGenerator ();

        predefinedJourneyGenerator.setTrafficSteps (trafficSteps);
        predefinedJourneyGenerator.setSearchTerms (searchTerms);
        predefinedJourneyGenerator.setZeroResultSearchTerms (zeroResultSearchTerms);
        predefinedJourneyGenerator.setSearchCategories (searchCategories);
        predefinedJourneyGenerator.setStepsHandler (stepsHandler);

        predefinedJourneyGenerator.init (); // init predefinedJourneyGenerator
        return predefinedJourneyGenerator;
    }

    public RandomJourneyGenerator buildRandomJourneyGenerator () {
        RandomJourneyGenerator randomJourneyGenerator;
        StepsHandler stepsHandler;

        stepsHandler = prepareStepsHandler ();

        // prepare randomJourneyGenerator
        randomJourneyGenerator = new RandomJourneyGenerator ();
        randomJourneyGenerator.setTrafficSteps (trafficSteps);
        randomJourneyGenerator.setStepsHandler (stepsHandler);

        randomJourneyGenerator.init (); // init randomJourneyGenerator
        return randomJourneyGenerator;
    }

    public CuratedJourneyGenerator buildCuratedJourneyGenerator () {
        CuratedJourneyGenerator curatedJourneyGenerator;
        StepsHandler stepsHandler;

        stepsHandler = prepareStepsHandler ();
 
        // prepare predefinedJourneyGenerator
        curatedJourneyGenerator = new CuratedJourneyGenerator ();
        curatedJourneyGenerator.setCuratedSearchTerms (curatedSearchTerms);
        curatedJourneyGenerator.setZeroResultSearchTerms (zeroResultSearchTerms);
        curatedJourneyGenerator.setSearchCategories (searchCategories);
        curatedJourneyGenerator.setStepsHandler (stepsHandler);

        curatedJourneyGenerator.init (); // init curatedJourneyGenerator
        return curatedJourneyGenerator;
    }

    private StepsHandler prepareStepsHandler () {
        StepsHandler stepsHandler;

        stepsHandler = new StepsHandler ();
        stepsHandler.setProductFeed (productFeed);
        stepsHandler.setCategoryCollector (categoryCollector);
        stepsHandler.setOrderIdGenerator (orderIdGenerator);
        stepsHandler.setProductSelector (productSelector);
        stepsHandler.setPixelTemplates (pixelTemplates);
        stepsHandler.setStartUrlPool (startUrlPool);
        stepsHandler.setStartRefUrlPool (startRefUrlPool);
        stepsHandler.setSearchTerms (searchTerms);
        stepsHandler.setSuggestTerms (suggestTerms);
        stepsHandler.setZeroResultSearchTerms (zeroResultSearchTerms);
        stepsHandler.setSearchCategories (searchCategories);
        stepsHandler.setCuratedSearchTerms (curatedSearchTerms);
        stepsHandler.setApiTemplates (apiTemplates);
        stepsHandler.setDispatcher (DiscoveryUserAccess);
        stepsHandler.setActiveCampaignRecord (activeCampaignRecord);
        stepsHandler.setCustomJourney (customJourney);
        stepsHandler.setTestData (testData);
        stepsHandler.setWidgetHandler (widgetHandler);
        stepsHandler.setWidgetLogger (widgetLog);

        return stepsHandler;
    }

}

