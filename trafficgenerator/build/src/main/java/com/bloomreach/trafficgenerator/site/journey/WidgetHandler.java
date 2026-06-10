package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.journeydata.WidgetConfigs;
import com.bloomreach.trafficgenerator.site.journeydata.WidgetRecord;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.journeydata.templates.PixelBRData;
import com.bloomreach.trafficgenerator.site.journeydata.templates.ApiBRData;
import com.bloomreach.trafficgenerator.site.journeydata.templates.ApiTemplates;
import com.bloomreach.trafficgenerator.site.journeydata.templates.PixelTemplates;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.WidgetApiResponse;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.WidgetResponseMetadata;
import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.SearchApiResponseDoc;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.DiscoveryUserAccess;
import com.bloomreach.trafficgenerator.site.build.apiparams.BuildWidgetApi;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildWidgetViewEventPixel;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildWidgetClickEventPixel;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildWidgetATCEventPixel;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.journeylogs.WidgetLog;

public class WidgetHandler {

    private WidgetConfigs widgetConfigs;

    public WidgetHandler () {
    }

    public void setWidgetConfigs (WidgetConfigs widgetConfigs) {
        this.widgetConfigs = widgetConfigs;
    }

    public long handleWidgetsOnPage (StepResult stepResult, UserRecord userRecord, long logTime, 
                                     Cart cart,
                                     CampaignRecord campaignRecord,
                                     ApiTemplates apiTemplates,
                                     PixelTemplates pixelTemplates,
                                     DiscoveryUserAccess DiscoveryUserAccess,
                                     boolean testData,
                                     WidgetLog widgetLog) {
        String currentPageType;
        ArrayList<WidgetRecord> widgetsOnPage;

        currentPageType = detectPageType (stepResult.getUrl ());
        if (currentPageType == null) 
            return logTime;

        // given page type, get list of widgets for that pagetype
        widgetsOnPage = this.widgetConfigs.getWidgetsOnPage (currentPageType);

        if ((widgetsOnPage == null) || (widgetsOnPage.size() == 0)) // no widgets on this page
            return logTime;

        // even if there are multiple widgets in a page, we call widgetAPI sequentially
        // for each of them. That results in time-delay between each widget API call. Therefore we don't
        // need to add more sleep time to avoid pixels being dropped due to time-difference
        // between those pixels
        for (WidgetRecord widgetRecord : widgetsOnPage) {
            try {
                handleWidgetTraffic (stepResult, userRecord, logTime, widgetRecord,
                                     campaignRecord, cart, apiTemplates, pixelTemplates, DiscoveryUserAccess, testData, widgetLog);
            } catch (Exception e) {
                MessageLogger.logError (String.format ("HandleWidget exception: %s", e.getMessage()));
                // continue to handle other widgets if any
            }
        }

        return logTime + GeneratorConstants.WIDGET_ACTION_DURATION;
    }

    // trigger 'view' pixels for all widgets
    private void handleWidgetTraffic (StepResult stepResult, UserRecord userRecord, long logTime, 
                                      WidgetRecord widgetRecord,
                                      CampaignRecord campaignRecord,
                                      Cart cart,
                                      ApiTemplates apiTemplates,
                                      PixelTemplates pixelTemplates,
                                      DiscoveryUserAccess DiscoveryUserAccess,
                                      boolean testData,
                                      WidgetLog widgetLog) throws Exception {
        WidgetApiResponse widgetApiResponse = null;
        String exceptionMsg;
        double random;
        String itemId;

        // NOTE: Due to potential high QPS caused by widget traffic, need to restrict such calls
        // JIRA: https://bloomreach.atlassian.net/browse/PACIFICAPPAREL-44?isEligibleForUserSurvey=true
        // For now, generate widget traffic only for 20% 
        random = Math.random ();
        if (random > 0.80)
            return;

        // for item-based widgets, need to get itemId for api call.
        // if specific itemId not specified in widget config, pick up from url - should be product-page-url
        if (widgetRecord.getItemId() != null)
            itemId = widgetRecord.getItemId ();
        else
            itemId = detectItemId (widgetRecord.getWidgetCode(), stepResult.getUrl());

        // first make widget API call. Response contains metadata to use in building widgetpixel
        try {
            widgetApiResponse = getWidgetApiResponse (userRecord, logTime, 
                                                      stepResult.getRefUrl (), stepResult.getUrl (),
                                                      widgetRecord,
                                                      itemId,
                                                      campaignRecord,
                                                      apiTemplates,
                                                      DiscoveryUserAccess); 
            // "widget-api" logs not collected here because the log file gets too large. 
            // Actual api calls are written to the api-log-files as usual (in debug mode)
            // widgetLog.addApiRecord (userRecord.getUserId(), stepResult.getUrl(), widgetRecord.getWid(), 
            //                           itemId, widgetApiResponse.getNumFound(), logTime);
        } catch (Exception e) {
            exceptionMsg = String.format ("Exception in getWidgetApiResponse, widgetId = %s, msg = %s", widgetRecord.getWid (), e.getMessage());
            MessageLogger.logError (exceptionMsg);
            throw new Exception (exceptionMsg);
        }

        if (widgetApiResponse == null)
            return;

        if (widgetApiResponse.getNumFound() == 0) {
            MessageLogger.logWarning(String.format ("Widget API response got zero results, widgetid = %s", widgetRecord.getWid()));
        } else {
            MessageLogger.logDebug(String.format ("Widget API response for widgetId = %s, got numFound = %d", widgetRecord.getWid(), widgetApiResponse.getNumFound()));
        }
        // for each widget, trigger view pixel - always
        try {
            dispatchWidgetViewPixel (userRecord, logTime, stepResult.getRefUrl (), stepResult.getUrl (),
                                     widgetRecord, widgetApiResponse,
                                     pixelTemplates,
                                     DiscoveryUserAccess, 
                                     testData);
        } catch (Exception e) {
            exceptionMsg = String.format ("Exception in dispatchWidgetViewPixel, widgetId = %s", widgetRecord.getWid ());
            MessageLogger.logError (exceptionMsg);
            throw new Exception (exceptionMsg);
        }

        // for a % of traffic, trigger 'click' pixel -- currently 10%
        random = Math.random ();
        if (random < 0.1) {
            ArrayList<SearchApiResponseDoc> responseDocs;
            SearchApiResponseDoc selectedResponseDoc;
            int indx;

            // select a pid from widgetApiResponse
            responseDocs = widgetApiResponse.getResponseDocs ();
            if (responseDocs.size() > 0) {
                indx = (int) (Math.random () * responseDocs.size());
                selectedResponseDoc = responseDocs.get (indx);

                try {
                    dispatchWidgetClickPixel (userRecord, logTime, stepResult.getRefUrl (), stepResult.getUrl(),
                                              widgetRecord, widgetApiResponse,
                                              selectedResponseDoc.getPid (),
                                              pixelTemplates,
                                              DiscoveryUserAccess, 
                                              testData);
                    widgetLog.addClickRecord (userRecord.getUserId(), stepResult.getUrl(), widgetRecord.getWid(), 
                                                 selectedResponseDoc.getPid(), widgetApiResponse.getNumFound(), logTime);
                } catch (Exception e) {
                    exceptionMsg = String.format ("Exception in dispatchWidgetClickPixel, widgetId = %s", widgetRecord.getWid ());
                    MessageLogger.logError (exceptionMsg);
                    throw new Exception (exceptionMsg);
                }
            }
        }

        // for a much smaller traffic, trigger ATC pixel = 1%
        random = Math.random ();
        if (random < 0.01) {
            ArrayList <SearchApiResponseDoc> responseDocs;
            SearchApiResponseDoc selectedResponseDoc;
            int indx;

            // select a pid from widgetApiResponse
            responseDocs = widgetApiResponse.getResponseDocs ();
            if (responseDocs.size() > 0) {
                indx = (int) (Math.random () * responseDocs.size());
                selectedResponseDoc = responseDocs.get (indx);
                ProductDetails selectedProductDetails;
                CartItem cartItem;

                // add selected product to cart
                selectedProductDetails = constructProductDetails (selectedResponseDoc);
                cartItem = new CartItem();
                cartItem.setItem(selectedProductDetails);
                cartItem.setQuantity(1); // quantity always = 1
                cart.addItem(cartItem);

                try {
                    dispatchWidgetATCPixel (userRecord, logTime, stepResult.getRefUrl (), stepResult.getUrl(),
                                            widgetRecord, widgetApiResponse,
                                            selectedResponseDoc.getPid (), selectedResponseDoc.getSkuid (),
                                            cart,
                                            pixelTemplates,
                                            DiscoveryUserAccess, 
                                            testData);
                    widgetLog.addATCRecord (userRecord.getUserId(), stepResult.getUrl(), widgetRecord.getWid(), 
                                               selectedProductDetails.getPid(), widgetApiResponse.getNumFound(), logTime);
                } catch (Exception e) {
                    exceptionMsg = String.format ("Exception in dispatchWidgetATCPixel, widgetId = %s", widgetRecord.getWid ());
                    MessageLogger.logError (exceptionMsg);
                    throw new Exception (exceptionMsg);
                }
            }
        }
    }

    private WidgetApiResponse getWidgetApiResponse (UserRecord userRecord, long logTime, String refUrl, String url,
                                                    WidgetRecord widgetRecord,
                                                    String itemId,
                                                    CampaignRecord campaignRecord,
                                                    ApiTemplates apiTemplates,
                                                    DiscoveryUserAccess DiscoveryUserAccess) throws Exception {
        ApiBRData widgetApiData;
        BuildWidgetApi apiBuilder;
        int buildStatus;
        WidgetApiResponse widgetApiResponse = null;

        widgetApiData = apiTemplates.loadApiTemplate (userRecord.getDeviceType());
        apiBuilder = new BuildWidgetApi ();
        buildStatus = apiBuilder.build (widgetApiData, userRecord, logTime, refUrl, url,
                                        widgetRecord.getQuery (),
                                        widgetRecord.getWid (),
                                        itemId,
                                        campaignRecord);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            widgetApiResponse = DiscoveryUserAccess.getWidgetApiResponse (widgetApiData, widgetRecord.getWidgetCode ());
            if (widgetApiResponse == null) {
                MessageLogger.logError ("Widget api response is null for widgetId: " + widgetRecord.getWid ());
            }
        }

        return widgetApiResponse; 
    }

    private void dispatchWidgetViewPixel (UserRecord userRecord, long logTime, String refUrl, String url,
                                        WidgetRecord widgetRecord,
                                        WidgetApiResponse widgetApiResponse,
                                        PixelTemplates pixelTemplates,
                                        DiscoveryUserAccess DiscoveryUserAccess, 
                                        boolean testData) throws Exception {
        PixelBRData pixelData;
        BuildWidgetViewEventPixel pixelBuilder;
        int buildStatus;
        WidgetResponseMetadata metadata;

        // prepare a product page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildWidgetViewEventPixel ();
        metadata = widgetApiResponse.getWidgetResponseMetadata ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime,
                                          refUrl, url,
                                          metadata.getId (),
                                          metadata.getRid (),
                                          metadata.getType (), 
                                          widgetRecord.getQuery (), // optional
                                          testData);

        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        }
    }

    private void dispatchWidgetClickPixel (UserRecord userRecord, long logTime, String refUrl, String url,
                                        WidgetRecord widgetRecord,
                                        WidgetApiResponse widgetApiResponse,
                                        String itemId,
                                        PixelTemplates pixelTemplates,
                                        DiscoveryUserAccess DiscoveryUserAccess, 
                                        boolean testData) throws Exception {
        PixelBRData pixelData;
        BuildWidgetClickEventPixel pixelBuilder;
        int buildStatus;
        WidgetResponseMetadata metadata;

        // prepare a product page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildWidgetClickEventPixel ();
        metadata = widgetApiResponse.getWidgetResponseMetadata ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime,
                                          refUrl, url,
                                          metadata.getId (),
                                          metadata.getRid (),
                                          metadata.getType (), 
                                          itemId,
                                          testData);

        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        }
    }

    private void dispatchWidgetATCPixel (UserRecord userRecord, long logTime, String refUrl, String url,
                                        WidgetRecord widgetRecord,
                                        WidgetApiResponse widgetApiResponse,
                                        String itemId, String skuid,
                                        Cart cart,
                                        PixelTemplates pixelTemplates,
                                        DiscoveryUserAccess DiscoveryUserAccess, 
                                        boolean testData) throws Exception {
        PixelBRData pixelData;
        BuildWidgetATCEventPixel pixelBuilder;
        int buildStatus;
        WidgetResponseMetadata metadata;

        // prepare a product page pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildWidgetATCEventPixel ();
        metadata = widgetApiResponse.getWidgetResponseMetadata ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime,
                                          refUrl, url,
                                          metadata.getId (),
                                          metadata.getRid (),
                                          metadata.getType (), 
                                          itemId,
                                          skuid,
                                          testData);

        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            DiscoveryUserAccess.dispatchPixel (pixelData);
        }
    }

    // given current url, use known pattern(s) to detect page type
    // currently only product, category and homepage types are supported
    private String detectPageType (String url) {
        String pageType;

        if (url.indexOf ("products") > 0)
            pageType = WidgetConfigs.PAGEMAP_PAGETYPE_PRODUCTPAGE;
        else if (url.indexOf ("categories") > 0)
            pageType = WidgetConfigs.PAGEMAP_PAGETYPE_CATEGORYPAGE;
        else
            pageType = WidgetConfigs.PAGEMAP_PAGETYPE_HOMEPAGE;

        return pageType;
    }

    private String detectItemId (String widgetCode, String url) throws Exception {
        String itemId = null;

        switch (widgetCode) {
            case WidgetConfigs.WCODE_RECO_ITEM_EXP:
            case WidgetConfigs.WCODE_RECO_ITEM_FREQ_BOUGHT:
            case WidgetConfigs.WCODE_RECO_ITEM_FREQ_VIEWED:
            case WidgetConfigs.WCODE_RECO_ITEM_SIMILAR:
            case WidgetConfigs.WCODE_RECO_VISUAL_RECO:
                int indx;
                String tail;

                indx = url.indexOf ("/products/");
                if (indx >= 0) {
                    indx = url.lastIndexOf("/");
                    tail = url.substring(indx + 1);
                    // remove trailing "___*" if any
                    indx = tail.indexOf("_");
                    if (indx >= 0) {
                        itemId = tail.substring (0, indx);
                    } else {
                        itemId = tail;
                    }
                }
                break;
        
            default:
                break;
        }

        // ensure we have valid itemId for item-id-widgets
        switch (widgetCode) {
            case WidgetConfigs.WCODE_RECO_ITEM_EXP:
            case WidgetConfigs.WCODE_RECO_ITEM_FREQ_BOUGHT:
            case WidgetConfigs.WCODE_RECO_ITEM_FREQ_VIEWED:
            case WidgetConfigs.WCODE_RECO_ITEM_SIMILAR:
            case WidgetConfigs.WCODE_RECO_VISUAL_RECO:
                if (itemId == null) {
                    MessageLogger.logError("ItemId not available for item-based-widget");
                    throw new Exception ("ItemId not available for item-based-widget");
                }
            default:
                break;
        }
        return itemId;
    }

    // in order to add selected product to cart
    // don't need to include productStyle here because it has no impact on add-to-cart
    private ProductDetails constructProductDetails (SearchApiResponseDoc selectedResponseDoc) {
        ProductDetails productDetails;

        productDetails = new ProductDetails();
        productDetails.setPid(selectedResponseDoc.getPid());
        productDetails.setUrl(selectedResponseDoc.getUrl());
        productDetails.setTitle(selectedResponseDoc.getTitle());
        productDetails.setSalePrice(selectedResponseDoc.getSalePrice());
        productDetails.setSkuid(selectedResponseDoc.getSkuid());
        productDetails.setPrice(selectedResponseDoc.getPrice());

        return productDetails;

    }
}
/**
// String tmp_url = "https://pacific.bloomreach.com/home/products/88241___573992";
// stepResult.setUrl(tmp_url);
// String tmp_userId = "1112223334445";
// userRecord.setUserId(tmp_userId);
*/

