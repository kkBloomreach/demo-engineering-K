package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildHomePagePixel;
import com.bloomreach.trafficgenerator.site.build.pixelparams.OrderIdGenerator;
import com.bloomreach.trafficgenerator.site.dispatch.Dispatcher;
import com.bloomreach.trafficgenerator.site.dispatch.SearchApiResponse;
import com.bloomreach.trafficgenerator.site.dispatch.SuggestApiResponse;
import com.bloomreach.trafficgenerator.site.feed.FeedRecord;
import com.bloomreach.trafficgenerator.site.feed.ProductFeed;
import com.bloomreach.trafficgenerator.site.journeydata.CategoryCollector;
import com.bloomreach.trafficgenerator.site.journeydata.SearchCategories;
import com.bloomreach.trafficgenerator.site.journeydata.SearchTermWithRefinements;
import com.bloomreach.trafficgenerator.site.journeydata.SearchTerms;
import com.bloomreach.trafficgenerator.site.journeydata.StartRefUrlPool;
import com.bloomreach.trafficgenerator.site.journeydata.StartUrlPool;
import com.bloomreach.trafficgenerator.site.journeydata.StartUrlPoolRecord;
import com.bloomreach.trafficgenerator.site.journeydata.SuggestTerms;
import com.bloomreach.trafficgenerator.site.journeydata.ZeroResultSearchTerms;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.journeydata.customjourney.CustomJourney;
import com.bloomreach.trafficgenerator.site.journeydata.customjourney.LPCCustomJourneyData;
import com.bloomreach.trafficgenerator.site.journeydata.queryexecutor.CategoryInfo;
import com.bloomreach.trafficgenerator.site.journeydata.templates.ApiTemplates;
import com.bloomreach.trafficgenerator.site.journeydata.templates.PixelTemplates;
import com.bloomreach.trafficgenerator.site.journeylogs.SessionLog;
import com.bloomreach.trafficgenerator.site.journeylogs.StepLog;
import com.bloomreach.trafficgenerator.site.journeylogs.WidgetLog;
import com.bloomreach.trafficgenerator.site.user.UserRecord;

public class StepsHandler {

    // startUrl pool
    StartUrlPool startUrlPool;

    // startRefUrl pool
    StartRefUrlPool startRefUrlPool;

    // searchTerms 
    SearchTerms searchTerms;

    // suggestTerms 
    SuggestTerms suggestTerms;

    // zeroResultQuery map
    ZeroResultSearchTerms zeroResultSearchTerms;

    // searchCategories
    SearchCategories searchCategories;

    // templates for product / atc / conversion pixels
    PixelTemplates pixelTemplates;

    // templates for search, category api templates
    ApiTemplates apiTemplates;

    // orderId generator
    OrderIdGenerator orderIdGenerator;

    // product selector
    ProductSelector productSelector;

    // productFeed
    ProductFeed productFeed;

    // categoryCollector
    CategoryCollector categoryCollector;

    // Dispatcher - send API calls to server
    Dispatcher dispatcher;

    // current campaign record, may be null
    CampaignRecord activeCampaignRecord;

    // testData - true/false
    boolean testData;

    // widget configs
    WidgetHandler widgetHandler;

    // widget logger
    WidgetLog widgetLog;

    // custom journey
    CustomJourney customJourney;

    // Helps to quickly check daily triffic
    public static int debugTotalATCs = 0;
    public static int debugTotalConversions = 0;

    public StepsHandler () {
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

    public void setDispatcher (Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void setActiveCampaignRecord (CampaignRecord activeCampaignRecord) {
        this.activeCampaignRecord = activeCampaignRecord;
    }

    public void setWidgetHandler (WidgetHandler widgetHandler) {
        this.widgetHandler = widgetHandler;
    }

    public void setWidgetLogger (WidgetLog widgetLog) {
        this.widgetLog = widgetLog;
    }

    public void setTestData (boolean testData)  {
        this.testData = testData;
    }

    public void setCustomJourney (CustomJourney customJourney) {
        this.customJourney = customJourney;
    }

    // returns search/social/home/blank/... for starting ref-url
    public String selectStartRefUrl () {
        return (this.startRefUrlPool.selectStartRefUrlAtRandom ().getRefUrl());
    }

    // widgetHandler - handling widgets is independent of individual journey steps
    // This method is called from predefined/random journey handlers for each step
    public long handleWidgetsOnPage (StepResult stepResult, UserRecord userRecord, Cart cart, long logTime, SessionLog sessionLog) {
        logTime = this.widgetHandler.handleWidgetsOnPage (stepResult,
                                                          userRecord,
                                                          logTime,
                                                          cart,
                                                          this.activeCampaignRecord,
                                                          this.apiTemplates,
                                                          this.pixelTemplates,
                                                          this.dispatcher,
                                                          this.testData,
                                                          this.widgetLog); 
        return logTime;
    }

    public StepResult handleStepOpenUrl (StepResult prevStepResult,
                                          UserRecord userRecord, 
                                          long logTime,
                                          StepLog stepLog) throws Exception {
        StepResult stepResult = null;
        StartUrlPoolRecord startUrlPoolRecord;

        startUrlPoolRecord = this.startUrlPool.selectStartUrlAtRandom ();  // select random url

        // session 'begin', setUrl
        if (prevStepResult.getUrl () == null)
            prevStepResult.setUrl ("");

        switch (startUrlPoolRecord.getUrlType()) {
            case StartUrlPool.URL_TYPE_PRODUCT:
                String pid;
                FeedRecord feedRecord;

                // NOTE: handleStepBrowsePDP does not make a Discovery api call. All necessary
                // info is already availabe in productDetails object

                pid = startUrlPoolRecord.getId ();  // pid
                feedRecord = this.productFeed.lookupProductRecord (pid);
                if (feedRecord != null) {
                    ProductDetails productDetails;

                    productDetails = translateFeedRecordToProductDetails (feedRecord);    //internal utility method

                    stepResult = handleStepBrowsePDP (prevStepResult,
                                                      userRecord,
                                                      logTime,
                                                      stepLog,
                                                      productDetails);
                } else {
                    MessageLogger.logWarning ("Unknown product in startUrlPool, pid = " + startUrlPoolRecord.getId ());
                }
                break;

            case StartUrlPool.URL_TYPE_CATEGORY:
                String catId;
                CategoryInfo catInfo;

                // NOTE: StepSearchCategory internally triggers a discovery API call, that implies
                // visitor is effectively already 'on-the-site'
                // Therefore set the 'url' to be on-the-site (by default, homepage). The ref remains unchanged
                prevStepResult.setUrl (BuildHomePagePixel.getHomePageUrl ());

                catId = startUrlPoolRecord.getId ();  // pid
                catInfo = this.categoryCollector.lookupCategoryInfo (catId);
                if (catInfo != null) {
                    stepResult = handleStepSearchCategory (prevStepResult,
                                                           userRecord,
                                                           logTime,
                                                           stepLog,
                                                           catInfo);
                } else {
                    MessageLogger.logWarning ("Unknown catId in url, catId = " + startUrlPoolRecord.getId ());
                }
                break;

            case StartUrlPool.URL_TYPE_HOME:
                // NOTE: handleStart with "homepage" does not make a Discovery api call. 
                stepResult = handleStepStartUrl (prevStepResult,
                                                   userRecord,
                                                   logTime,
                                                   stepLog,
                                                   StartUrlPool.URL_TYPE_HOME);
                break;

            case StartUrlPool.URL_TYPE_SEARCH:
                String searchTerm;

                // Note: StepSearchTerm internally triggers a 'searchEvent', that implies
                // visitor is effectively already 'on-the-site' (so that an event can occur).
                // Therefore set the 'url' to be on-the-site (by default, homepage). The ref remains unchanged
                prevStepResult.setUrl (BuildHomePagePixel.getHomePageUrl ());

                // this step internally triggers a 'search-event' which is really not correct (or needed)...
                searchTerm = startUrlPoolRecord.getId ();  // searchTerm
                stepResult = handleStepSearchTerm (prevStepResult,
                                                   userRecord,
                                                   logTime,
                                                   stepLog,
                                                   searchTerm);
                break;

            case StartUrlPool.URL_TYPE_OTHER:
                // NOTE: handleStart with "other page" does not make a Discovery api call. 
                stepResult = handleStepStartUrl (prevStepResult,
                                                   userRecord,
                                                   logTime,
                                                   stepLog,
                                                   StartUrlPool.URL_TYPE_OTHER);
                break;

            default:
                MessageLogger.logError ("StartUrl, urlType not supported yet: " + startUrlPoolRecord.getUrlType());
        }

        // if we have problem in "OpenUrl" itself, force a 'homepage' as startUrl
        // This can happen if a selected category or search term returns zero results
        if ((stepResult == null) || (stepResult instanceof StepResultInvalidData)) {
            StepResultVoid defaultData;
            long endTime;

            defaultData = new StepResultVoid ();
            defaultData.setRefUrl ("");
            defaultData.setUrl (BuildHomePagePixel.getHomePageUrl ());
            if (stepResult == null)
                endTime = prevStepResult.getEndTime ();
            else
                endTime = stepResult.getEndTime ();
            defaultData.setEndTime (endTime);
            stepResult = defaultData;
        } 

        return stepResult; 
    }

    // this method is called when productDetails in prevStepResult
    public StepResult handleStepBrowsePDP (StepResult prevStepResult,
                                            UserRecord userRecord,
                                            long logTime,
                                            StepLog stepLog ) throws Exception {
        StepResult stepResult = null;
        if (prevStepResult instanceof StepResultProductDetails) {
            ProductDetails productDetails;

            productDetails = (ProductDetails) prevStepResult.getData ();
            stepResult = handleStepBrowsePDP (prevStepResult,
                                              userRecord,
                                              logTime,
                                              stepLog, 
                                              productDetails);
        } else {
            StepResultInvalidData invalidData;
            MessageLogger.logWarning (String.format ("Incorrect prevStepResult data in %s ", "handleStepBrowsePDP"));
            invalidData = new StepResultInvalidData ();
            invalidData.setRefUrl (prevStepResult.getRefUrl ());
            invalidData.setUrl (prevStepResult.getUrl ());
            invalidData.setMessage ("Null stepResult in handleStepOpenUrl");
            invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
            stepResult = invalidData;
        }

        return stepResult; 
    }

    // this method is called when processig a PDP-url
    public StepResult handleStepBrowsePDP (StepResult prevStepResult,
                                            UserRecord userRecord,
                                            long logTime,
                                            StepLog stepLog,
                                            ProductDetails productDetails) throws Exception {
        StepBrowsePDP step;
        StepResult stepResult = null;
        String logQuery;

        step = new StepBrowsePDP ();
        
        logQuery = productDetails.getPid ();
        if (productDetails.getSkuid() != null)
            logQuery = logQuery + "(" + productDetails.getSkuid () + ")";
        stepLog.setQuery (logQuery);

        stepResult = step.handleStep (prevStepResult,
                                      userRecord,
                                      logTime,
                                      productDetails,
                                      this.pixelTemplates,
                                      this.dispatcher,
                                      this.testData);

        return stepResult; 
    }


    public StepResult handleStepStartUrl (StepResult prevStepResult,
                                          UserRecord userRecord,
                                          long logTime,
                                          StepLog stepLog,
                                          String urlType) throws Exception {
        StepStartUrl step;
        StepResult stepResult = null;

        stepLog.setQuery (urlType); // "home", "other", "thematic"
        step = new StepStartUrl ();
        stepResult = step.handleStep (prevStepResult,
                                      userRecord,
                                      logTime,
                                      urlType,
                                      this.pixelTemplates,
                                      this.dispatcher,
                                      this.testData);
        return stepResult; 
    }

    public StepResult handleStepViewList (StepResult prevStepResult,
                                           UserRecord userRecord,
                                           long logTime,
                                           StepLog stepLog) throws Exception {
        StepViewList step;
        StepResult stepResult = null;

        step = new StepViewList ();
        if (prevStepResult instanceof StepResultSearchResponse) {
            StepResultSearchResponse stepResultSearchResponse;
            SearchApiResponse searchApiResponse;

            stepResultSearchResponse = (StepResultSearchResponse) prevStepResult;
            searchApiResponse = (SearchApiResponse) stepResultSearchResponse.getData ();
            if (searchApiResponse != null)  // else 'query' string = '-' in the log file
                stepLog.setQuery (String.format ("numFound = %d", searchApiResponse.getNumFound ()));
            stepResult = step.handleStep (prevStepResult,
                                          userRecord, 
                                          logTime,
                                          searchApiResponse);
        } else {
            StepResultInvalidData invalidData;
            MessageLogger.logWarning (String.format ("Incorrect prevStepResult data in %s ", "handleStepViewList"));
            invalidData = new StepResultInvalidData ();
            invalidData.setRefUrl (prevStepResult.getRefUrl ());
            invalidData.setUrl (prevStepResult.getUrl ());
            invalidData.setMessage ("Null stepResult in handleStepViewList");
            invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
            stepResult = invalidData;
        }

        return stepResult; 
    }

    // this method is called when search-term is NOT already known 
    public StepResult handleStepSearchTerm (StepResult prevStepResult,
                                             UserRecord userRecord,
                                             long logTime,
                                             StepLog stepLog) throws Exception {
        StepResult stepResult = null;
        SearchTermWithRefinements selectedTerm;

        selectedTerm = this.searchTerms.selectSearchTermAtRandom (prevStepResult.getUrl());
        stepResult = handleStepSearchTerm (prevStepResult,
                                           userRecord,
                                           logTime,
                                           stepLog,
                                           selectedTerm.getPrimary());
        return stepResult;
    }

    // this method is called when search-term is already known
    public StepResult handleStepSearchTerm (StepResult prevStepResult,
                                             UserRecord userRecord,
                                             long logTime,
                                             StepLog stepLog,
                                             String selectedTerm) throws Exception {
        StepSearchTerm step;
        StepResult stepResult = null;

        stepLog.setQuery (selectedTerm);
        step = new StepSearchTerm ();

        stepResult = step.handleStep (prevStepResult,
                                      userRecord,
                                      logTime,
                                      selectedTerm,
                                      this.activeCampaignRecord,
                                      this.pixelTemplates,
                                      this.apiTemplates,
                                      this.dispatcher,
                                      this.testData);
        return stepResult; 
    }

    // this method is called to terminate zeroSearch session
    // it is same as actual searchSession, except that is not logged in journeylog
    public StepResult handleStepTerminateZeroSearchSession (StepResult prevStepResult,
                                                            UserRecord userRecord,
                                                            long logTime) throws Exception {
        StepSearchTerm step;
        StepResult stepResult;
        SearchTermWithRefinements selectedTerm;

        selectedTerm = this.searchTerms.selectSearchTermAtRandom (prevStepResult.getUrl());    // this term is not logged
        step = new StepSearchTerm ();
        stepResult = step.handleStep (prevStepResult,
                                      userRecord,
                                      logTime,
                                      selectedTerm.getPrimary (),
                                      this.activeCampaignRecord,
                                      this.pixelTemplates,
                                      this.apiTemplates,
                                      this.dispatcher,
                                      this.testData);
        return stepResult; 
    }

    // this method is called when there is no prior catInfo
    public StepResult handleStepSearchCategory (StepResult prevStepResult,
                                                 UserRecord userRecord,
                                                 long logTime,
                                                 StepLog stepLog) throws Exception {
        StepResult stepResult = null;
        CategoryInfo selectedCatInfo;

        selectedCatInfo = this.searchCategories.selectCategoryAtRandom (prevStepResult.getUrl());
        stepResult = handleStepSearchCategory (prevStepResult,
                                               userRecord,
                                               logTime,
                                               stepLog,
                                               selectedCatInfo);
        return stepResult;
    }

    // this method is called with 'catInfo' already known 
    public StepResult handleStepSearchCategory (StepResult prevStepResult,
                                                 UserRecord userRecord,
                                                 long logTime,
                                                 StepLog stepLog,
                                                 CategoryInfo selectedCatInfo ) throws Exception {
        StepSearchCategory step;
        StepResult stepResult = null;
        LPCCustomJourneyData lpcCustomJourneyData;

        // selectedCatInfo may be null in some rare cases
        // eg, someone messed up tools->category facet that causes no categories in API response
        if (selectedCatInfo == null) {
            StepResultInvalidData inputInvalid;

            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("handleStepSearchCategory, null categoryInfo");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } 

        stepLog.setQuery (selectedCatInfo.getCatId ());

       

        step = new StepSearchCategory ();
        stepResult = step.handleStep (prevStepResult,
                                      userRecord,
                                      logTime,
                                      selectedCatInfo,
                                      this.activeCampaignRecord,
                                      this.pixelTemplates,
                                      this.apiTemplates,
                                      this.dispatcher,
                                      this.testData);
                                       
        // category-page pixel is already dispatched (ie, category 'browse' session has started)
        // if selected category happens to be 'low-perf-category'..., bounce from that session
        lpcCustomJourneyData = customJourney.getLPCCustomJourneyData(selectedCatInfo.getCatId());
        if (lpcCustomJourneyData != null) {
            int rand;

            rand = (int) (Math.random () * 100);
            if (rand < lpcCustomJourneyData.getBounceRate()) {
                throw new CustomJourneyException(lpcCustomJourneyData);
            }
        }
        return stepResult; 
    }

    public StepResult handleStepSuggestQuery (StepResult prevStepResult,
                                               UserRecord userRecord,
                                               long logTime,
                                               StepLog stepLog) throws Exception {
        StepSuggestQuery step;
        StepResult stepResult = null;
        String selectedAqTerm;  // "aq" as specified in suggest event pixel

        selectedAqTerm = this.suggestTerms.selectSuggestTermAtRandom ();

        stepLog.setQuery (selectedAqTerm);
        step = new StepSuggestQuery ();
        stepResult = step.handleStep (prevStepResult,
                                      userRecord,
                                      logTime,
                                      selectedAqTerm,
                                      this.pixelTemplates,
                                      this.apiTemplates,
                                      this.dispatcher,
                                      this.testData);
        return stepResult; 
    }

    // upon suggest response, user has selected none of the suggested term/category/product
    public StepResult handleStepSuggestSelectNone (StepResult prevStepResult) throws Exception {
        StepResultVoid stepResult = null;

        stepResult = new StepResultVoid ();
        stepResult.setUrl (prevStepResult.getUrl ());
        stepResult.setRefUrl (prevStepResult.getRefUrl ());
        stepResult.setEndTime (prevStepResult.getEndTime () + 1000); // some delay, for debugging

        return stepResult; 
    }

    // select a term from suggestions, execute search API call, return that response
    public StepResult handleStepSuggestSelectTerm (StepResult prevStepResult,
                                                    UserRecord userRecord,
                                                    long logTime,
                                                    StepLog stepLog) throws Exception {
        StepResult stepResult = null;
        StepSuggestSelectTerm step;

        if (prevStepResult instanceof StepResultSuggestResponse) {
            StepResultSuggestResponse stepResultSuggestResponse;
            SuggestApiResponse suggestApiResponse;

            stepResultSuggestResponse = (StepResultSuggestResponse) prevStepResult;
            suggestApiResponse = (SuggestApiResponse) stepResultSuggestResponse.getData ();
            if (suggestApiResponse != null) {
                String aqTerm;
                String queryTerm;

                aqTerm = stepResultSuggestResponse.getAqTerm (); // 'aq' == term used to make suggest api call
                // select a term from suggest API response. Select a term that does not result in 'currentUrl'
                queryTerm = suggestApiResponse.selectSuggestResponseTermAtRandom(prevStepResult.getUrl()); 

                stepLog.setQuery (queryTerm);
                step = new StepSuggestSelectTerm ();
                stepResult = step.handleStep (prevStepResult,
                                              userRecord,
                                              logTime,
                                              aqTerm,
                                              queryTerm,
                                              this.activeCampaignRecord,
                                              this.pixelTemplates,
                                              this.apiTemplates,
                                              this.dispatcher,
                                              this.testData);
            } else {
                StepResultInvalidData invalidData;
                MessageLogger.logWarning (String.format ("null SuggestApiResponse in %s ", "handleStepSuggestSelectTerm"));
                invalidData = new StepResultInvalidData ();
                invalidData.setRefUrl (prevStepResult.getRefUrl ());
                invalidData.setUrl (prevStepResult.getUrl ());
                invalidData.setMessage ("Incorrect prevStepResult in handleStepSuggestSelectTerm");
                invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
                stepResult = invalidData;
            }
        } else {
            StepResultInvalidData invalidData;
            MessageLogger.logWarning (String.format ("Incorrect prevStepResult data in %s ", "handleStepSuggestSelectTerm"));
            invalidData = new StepResultInvalidData ();
            invalidData.setRefUrl (prevStepResult.getRefUrl ());
            invalidData.setUrl (prevStepResult.getUrl ());
            invalidData.setMessage ("Incorrect prevStepResult in handleStepSuggestSelectTerm");
            invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
            stepResult = invalidData;
        }

        return stepResult; 
    }

    public StepResult handleStepSuggestSelectCategory (StepResult prevStepResult,
                                                        UserRecord userRecord,
                                                        long logTime,
                                                        StepLog stepLog) throws Exception {
        StepResult stepResult = null;
        StepSuggestSelectCategory step;

        if (prevStepResult instanceof StepResultSuggestResponse) {
            StepResultSuggestResponse stepResultSuggestResponse;
            SuggestApiResponse suggestApiResponse;

            stepResultSuggestResponse = (StepResultSuggestResponse) prevStepResult;
            suggestApiResponse = (SuggestApiResponse) stepResultSuggestResponse.getData ();
            if (suggestApiResponse != null) {
                String aqTerm;
                String queryCategory;

                aqTerm = stepResultSuggestResponse.getAqTerm (); // 'aq' == term used to make suggest api call
                // select a category from suggest API response. Select a catId that does not result in currentUrl
                queryCategory = suggestApiResponse.selectSuggestResponseCategoryAtRandom(prevStepResult.getUrl()); 

                stepLog.setQuery (queryCategory);
                step = new StepSuggestSelectCategory ();
                stepResult = step.handleStep (prevStepResult,
                                              userRecord,
                                              logTime,
                                              aqTerm,
                                              queryCategory,  // selected category
                                              this.activeCampaignRecord,
                                              this.pixelTemplates,
                                              this.apiTemplates,
                                              this.dispatcher,
                                              this.categoryCollector,
                                              this.testData);
            } else {
                StepResultInvalidData invalidData;
                MessageLogger.logWarning (String.format ("null SuggestApiResponse in %s ", "handleStepSuggestSelectCategory"));
                invalidData = new StepResultInvalidData ();
                invalidData.setRefUrl (prevStepResult.getRefUrl ());
                invalidData.setUrl (prevStepResult.getUrl ());
                invalidData.setMessage ("Incorrect prevStepResult in handleStepSuggestSelectCategory");
                invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
                stepResult = invalidData;
            }
        } else {
            StepResultInvalidData invalidData;
            MessageLogger.logWarning (String.format ("Incorrect prevStepResult data in %s ", "handleStepSuggestSelectCategory"));
            invalidData = new StepResultInvalidData ();
            invalidData.setRefUrl (prevStepResult.getRefUrl ());
            invalidData.setUrl (prevStepResult.getUrl ());
            invalidData.setMessage ("Incorrect prevStepResult in handleStepSuggestSelectCategory");
            invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
            stepResult = invalidData;
        }

        return stepResult; 
    }


    public StepResult handleStepSuggestSelectProduct  (StepResult prevStepResult,
                                                        UserRecord userRecord,
                                                        long logTime,
                                                        StepLog stepLog) throws Exception {
        StepResult stepResult = null;
        StepSuggestSelectProduct step;

        if (prevStepResult instanceof StepResultSuggestResponse) {
            StepResultSuggestResponse stepResultSuggestResponse;
            SuggestApiResponse suggestApiResponse;

            stepResultSuggestResponse = (StepResultSuggestResponse) prevStepResult;
            suggestApiResponse = (SuggestApiResponse) stepResultSuggestResponse.getData ();
            if (suggestApiResponse != null) {
                String aqTerm;

                aqTerm = stepResultSuggestResponse.getAqTerm (); // 'aq' == term used to make suggest api call

                step = new StepSuggestSelectProduct ();
                stepResult = step.handleStep (prevStepResult,
                                              userRecord,
                                              logTime,
                                              stepLog,
                                              aqTerm,
                                              suggestApiResponse.getSuggestProducts (), // suggested product info list
                                              this.activeCampaignRecord,
                                              this.pixelTemplates,
                                              this.apiTemplates,
                                              this.dispatcher,
                                              this.productFeed,
                                              this.productSelector,
                                              this.testData);
            } else {
                StepResultInvalidData invalidData;
                MessageLogger.logWarning (String.format ("null SuggestApiResponse in %s ", "handleStepSuggestSelectProduct"));
                invalidData = new StepResultInvalidData ();
                invalidData.setRefUrl (prevStepResult.getRefUrl ());
                invalidData.setUrl (prevStepResult.getUrl ());
                invalidData.setMessage ("Incorrect prevStepResult in handleStepSuggestSelectProduct");
                invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
                stepResult = invalidData;
            }
        } else {
            StepResultInvalidData invalidData;
            MessageLogger.logWarning (String.format ("Incorrect prevStepResult data in %s ", "handleStepSuggestSelectProduct"));
            invalidData = new StepResultInvalidData ();
            invalidData.setRefUrl (prevStepResult.getRefUrl ());
            invalidData.setUrl (prevStepResult.getUrl ());
            invalidData.setMessage ("Incorrect prevStepResult in handleStepSuggestSelectProduct");
            invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
            stepResult = invalidData;
        }

        return stepResult; 
    }


    public StepResult handleStepATC (StepResult prevStepResult,
                                      Cart userCart, 
                                      UserRecord userRecord, 
                                      long logTime,
                                      StepLog stepLog ) throws Exception {
        StepResult stepResult = null;
        StepATC step;

        if (prevStepResult instanceof StepResultProductDetails) {
            StepResultProductDetails stepResultProductDetails;
            ProductDetails productDetails;
            String logQuery;
            
            stepResultProductDetails = (StepResultProductDetails) prevStepResult;
            productDetails = (ProductDetails) stepResultProductDetails.getData ();
            
            logQuery = productDetails.getPid ();
            if (productDetails.getSkuid() != null)
                logQuery = logQuery + "(" + productDetails.getSkuid () + ")";
            stepLog.setQuery (logQuery);

            step = new StepATC ();
            stepResult = step.handleStep ((StepResultProductDetails) prevStepResult, userRecord, logTime, productDetails,
                                          userCart, this.pixelTemplates, this.dispatcher, this.testData);
            
            debugTotalATCs = debugTotalATCs + 1;
        } else {
            StepResultInvalidData invalidData;
            MessageLogger.logWarning (String.format ("Incorrect prevStepResult data in %s ", "handleStepATC"));
            invalidData = new StepResultInvalidData ();
            invalidData.setRefUrl (prevStepResult.getRefUrl ());
            invalidData.setUrl (prevStepResult.getUrl ());
            invalidData.setMessage ("Null stepResult in handleStepATC");
            invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
            stepResult = invalidData;
        }

        return stepResult; 
    }

    public StepResult handleStepConvert (StepResult prevStepResult,
                                          Cart userCart, 
                                          UserRecord userRecord, 
                                          long logTime,
                                          StepLog stepLog ) throws Exception {
        StepResult stepResult = null;
        StepConvert step;

        step = new StepConvert ();
        try {
            stepResult = step.handleStep (prevStepResult, userRecord, logTime, stepLog, userCart, 
                                          this.orderIdGenerator, this.pixelTemplates, this.dispatcher, this.testData);
            // helps to quickly check daily conversions
            debugTotalConversions = debugTotalConversions + 1;
        } catch (Exception e) {
            MessageLogger.logWarning ("StepConvert exception:  " + e.getMessage ());
        } finally {
            // clear out all items in current Cart. We do this even if stepResult may be unsuccessful
            userCart.empty ();
        }

        if (stepResult == null) {   // in case some Exception occur'd
            StepResultInvalidData invalidData;
            MessageLogger.logWarning (String.format ("Exception in %s ", "handleStepConvert"));
            invalidData = new StepResultInvalidData ();
            invalidData.setRefUrl (prevStepResult.getRefUrl ());
            invalidData.setUrl (prevStepResult.getUrl ());
            invalidData.setMessage ("Exception in handleStepConvert");
            invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
            stepResult = invalidData;
        }

        return stepResult;
    }

    public StepResult handleStepSelectProductFromList (StepResult prevStepResult,
                                                       UserRecord userRecord, 
                                                       long logTime,
                                                       StepLog stepLog ) throws Exception {
        StepResult stepResult = null;
        StepSelectProductFromList step;

        if (prevStepResult instanceof StepResultProductList) {
            StepResultProductList stepResultProductList;
            ArrayList<ProductDetails> productList;

            stepResultProductList = (StepResultProductList) prevStepResult;
            productList = (ArrayList <ProductDetails>) stepResultProductList.getData ();

            step = new StepSelectProductFromList ();
            stepResult = step.handleStep ((StepResultProductList) prevStepResult, userRecord, logTime, productList, this.productSelector); 
        } else {
            StepResultInvalidData invalidData;
            MessageLogger.logWarning (String.format ("Incorrect prevStepResult data in %s ", "handleStepSelectProductFromList"));

            invalidData = new StepResultInvalidData ();
            invalidData.setRefUrl (prevStepResult.getRefUrl ());
            invalidData.setUrl (prevStepResult.getUrl ());
            invalidData.setMessage ("Null stepResult in handleStepSelectProductFromList");
            invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
            stepResult = invalidData;
        }

        return stepResult; 
    }

    //internal utility method -- data provided via a feed record
    private ProductDetails translateFeedRecordToProductDetails (FeedRecord feedRecord) {
        ProductDetails productDetails;

        productDetails = new ProductDetails ();
        productDetails.setPid (feedRecord.getProductId ());
        productDetails.setUrl (feedRecord.getUrl ());
        try {
            productDetails.setPrice (Double.valueOf (feedRecord.getProductPrice ()));
        } catch (NumberFormatException nfe) {
            // leave price undefined...
        }

        try {
            productDetails.setSalePrice (Double.valueOf (feedRecord.getProductSalePrice ()));
        } catch (NumberFormatException nfe) {
            // leave sale_price undefined...
        }

        productDetails.setTitle (feedRecord.getProductName ());
        productDetails.setSkuid (feedRecord.getProductSkuId ());
        return productDetails;
    }

}

/******
//     // select product from suggested product list and use 'handleBrowsePDP' to open the page, dispatch pixel etc
//     public StepResult handleStepSuggestSelectProduct_PREV (StepResult prevStepResult,
//                                                        UserRecord userRecord,
//                                                        long logTime,
//                                                        StepLog stepLog) throws Exception {
//         StepResult stepResult = null;
//         StepResultInvalidData invalidData;
// 
//         if (prevStepResult instanceof StepResultSuggestResponse) {
//             StepResultSuggestResponse stepResultSuggestResponse;
//             SuggestApiResponse suggestApiResponse;
//             ArrayList <SuggestProductInfo> suggestedProducts;
//             ProductDetails selectedProductDetails;
//             int randomIndx;
//             String selectedSkuid;
// 
//             stepResultSuggestResponse = (StepResultSuggestResponse) prevStepResult;
//             suggestApiResponse = (SuggestApiResponse) stepResultSuggestResponse.getData ();
//             if (suggestApiResponse != null) {
//                 suggestedProducts = suggestApiResponse.getSuggestProducts ();
//                 if (suggestedProducts == null) {
//                     invalidData = new StepResultInvalidData ();
//                     invalidData.setRefUrl (prevStepResult.getRefUrl ());
//                     invalidData.setUrl (prevStepResult.getUrl ());
//                     invalidData.setMessage ("StepSuggestSelectProduct, null suggestedProducts");
//                     invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
//                     return invalidData;
//                 } else if (suggestedProducts.size () == 0) {
//                     invalidData = new StepResultInvalidData ();
//                     invalidData.setRefUrl (prevStepResult.getRefUrl ());
//                     invalidData.setUrl (prevStepResult.getUrl ());
//                     invalidData.setMessage ("StepSuggestSelectProduct, empty suggestedProducts");
//                     invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
//                     return invalidData;
//                 }
// 
//                 // use productSelector to select segment-specific product (if segmentation is in effect, otherwise default)
//                 selectedProductDetails = this.selectProductFromSuggestedProductList (userRecord, suggestedProducts);
//                 if (selectedProductDetails == null) {
//                     invalidData = new StepResultInvalidData ();
//                     invalidData.setRefUrl (prevStepResult.getRefUrl ());
//                     invalidData.setUrl (prevStepResult.getUrl ());
//                     invalidData.setMessage ("StepSuggestSelectProduct, no acceptable product available in the product list");
//                     invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
//                     return invalidData;
//                 }
// 
//                 stepLog.setQuery (selectedProductDetails.getPid());
//                 stepResult = handleStepBrowsePDP (prevStepResult,
//                                                   userRecord,
//                                                   logTime,
//                                                   stepLog,
//                                                   selectedProductDetails);
//             } else {
//                 MessageLogger.logWarning (String.format ("null SuggestApiResponse in %s ", "handleStepSuggestSelectProduct"));
//                 invalidData = new StepResultInvalidData ();
//                 invalidData.setRefUrl (prevStepResult.getRefUrl ());
//                 invalidData.setUrl (prevStepResult.getUrl ());
//                 invalidData.setMessage ("Incorrect prevStepResult in handleStepSuggestSelectProduct");
//                 invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
//                 stepResult = invalidData;
//             }
//         } else {
//             MessageLogger.logWarning (String.format ("Incorrect prevStepResult data in %s ", "handleStepSuggestSelectProduct"));
//             invalidData = new StepResultInvalidData ();
//             invalidData.setRefUrl (prevStepResult.getRefUrl ());
//             invalidData.setUrl (prevStepResult.getUrl ());
//             invalidData.setMessage ("Null stepResult in handleStepSuggestSelectProduct");
//             invalidData.setEndTime (prevStepResult.getEndTime () + 1000);
//             stepResult = invalidData;
//         }
// 
//         return stepResult; 
//    }
//     // TO BE REMOVED
//     private ProductDetails selectProductFromSuggestedProductList_UNUSED (UserRecord userRecord, 
//                                                                   ArrayList <SuggestProductInfo> suggestProductInfoList) throws Exception {
//         ArrayList <ProductDetails> productDetailsList;
//         ProductDetails selectedProductDetails = null;
// 
//         productDetailsList = new ArrayList <ProductDetails> ();
//         for (SuggestProductInfo suggestProductInfo : suggestProductInfoList) {
//             ProductDetails productDetails;
//             String skuid;
//             Double productPrice;
// 
//             // suggest api -> by default, suggested product info does not include product's sku (if any)
//             // Therefore use feed record to get its sku. It may be null if product has no skus
//             skuid = this.productFeed.lookupProductSkuId (suggestProductInfo.getPid ());
// 
//             // suggest-product-info has no 'price', get it from the feed
//             try {
//                 productPrice = Double.valueOf (this.productFeed.lookupProductPrice (suggestProductInfo.getPid ()));
//             } catch (NumberFormatException nfe) {
//                 productPrice = 0.01;
//             }
// 
//             productDetails = new ProductDetails ();
//             productDetails.setPid (suggestProductInfo.getPid ());
//             productDetails.setUrl (suggestProductInfo.getUrl ());
//             productDetails.setTitle (suggestProductInfo.getTitle ());
//             productDetails.setSalePrice (suggestProductInfo.getSalePrice ());
//             productDetails.setSkuid (skuid);
//             productDetails.setPrice (productPrice);
// 
//             productDetailsList.add (productDetails);
//         }
// 
//         // from this 'productList', select a product (using segment-rules if any)
//         if (productDetailsList.size () > 0) 
//             selectedProductDetails = this.productSelector.selectProduct (null, userRecord, productDetailsList);
//             
//         return selectedProductDetails; // may be null if no appropriate product available
//     }
//
//        lowPerformanceCategoryIds = SiteConfig.getLowPeformanceCategoryIds();
//        if ((lowPerformanceCategoryIds != null) && (lowPerformanceCategoryIds.size() > 0)) {
//           if (lowPerformanceCategoryIds.contains (selectedCatInfo.getCatId())) {
//                int rand;
//
//                rand = (int) (Math.random () * 100);
//                if (rand < GeneratorConstants.LOW_PERFORMANCE_CATEGORY_THRESHOLD) {
//                    throw new ForcedSessionExitException(selectedCatInfo.getCatId());
//                }
//            }
//        }
************/

