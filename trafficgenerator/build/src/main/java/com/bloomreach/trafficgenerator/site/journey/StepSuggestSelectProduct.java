package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.dispatch.Dispatcher;
import com.bloomreach.trafficgenerator.site.dispatch.SuggestProductInfo;
import com.bloomreach.trafficgenerator.site.build.pixelparams.*;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeylogs.StepLog;
import com.bloomreach.trafficgenerator.site.feed.ProductFeed;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepSuggestSelectProduct extends StepBase {

    public StepSuggestSelectProduct () {
    }

    // note - result is 'select a product'
    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  StepLog stepLog,
                                  String selectedAqTerm, // "aq"
                                  ArrayList<SuggestProductInfo> suggestProductInfoList,
                                  CampaignRecord activeCampaignRecord,
                                  PixelTemplates pixelTemplates,
                                  ApiTemplates apiTemplates,
                                  Dispatcher dispatcher,
                                  ProductFeed productFeed,
                                  ProductSelector productSelector,
                                  boolean testData) throws Exception {

        StepResultProductDetails thisStepResult;
        ProductDetails selectedProdDetails;
        StepResultInvalidData inputInvalid;

        MessageLogger.logDebug ("Handle step suggest select product from list");

        if (suggestProductInfoList == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSuggestSelectProduct, null suggestedProducts");
            inputInvalid.setEndTime (prevStepResult.getEndTime () + 1000);
            return inputInvalid;
        } else if (suggestProductInfoList.size () == 0) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSuggestSelectProduct, empty suggestedProducts");
            inputInvalid.setEndTime (prevStepResult.getEndTime () + 1000);
            return inputInvalid;
        }

        selectedProdDetails = selectProductFromSuggestedProductList (prevStepResult, userRecord, suggestProductInfoList, 
                                                                     productFeed, productSelector);

        if (selectedProdDetails == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSuggestSelectProduct, null suggestProductDetails");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } 

        stepLog.setQuery (selectedProdDetails.getPid ());
        handleStepInternal (prevStepResult, userRecord, logTime, 
                            selectedAqTerm, selectedProdDetails,
                            activeCampaignRecord,
                            pixelTemplates, apiTemplates, dispatcher, testData);


        thisStepResult = new StepResultProductDetails ();
        super.setUrlHistory (prevStepResult, thisStepResult, null);
        thisStepResult.setProductDetails (selectedProdDetails);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_SELECT_SUG_CAT);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_SELECT_SUG_CAT);

        return (thisStepResult);
    }

    private void handleStepInternal (StepResult prevStepResult,
                                                  UserRecord userRecord,
                                                  long logTime,
                                                  String selectedAqTerm,
                                                  ProductDetails selectedProdDetails,
                                                  CampaignRecord activeCampaignRecord,
                                                  PixelTemplates pixelTemplates,
                                                  ApiTemplates apiTemplates,
                                                  Dispatcher dispatcher,
                                                  boolean testData) throws Exception {
        PixelBRData suggestEventPixelData;

        // trigger suggest event pixel first. The refUrl, url do not change for SuggestEvent
        // NOTE: The "query" term is currently set to 'pid'. Not sure if it should be the product-title instead
        // @@@ TO BE CHECKED
        suggestEventPixelData = buildSuggestEventPixelFromTemplate (userRecord, logTime, 
                                                                    prevStepResult.getRefUrl(), // refUrl for suggestEvent
                                                                    prevStepResult.getUrl (),   // url for suggestEvent
                                                                    selectedAqTerm, 
                                                                    selectedProdDetails.getPid (), // 'query' term
                                                                    pixelTemplates, testData);
        if (suggestEventPixelData == null) {
            MessageLogger.logWarning ("Failed to build suggest event pixel");
        } else {
            dispatcher.dispatchPixel (suggestEventPixelData);
        }

        // NOTE: Actual product-page-pixel is not triggered in this class. The BrowsePDP class
        // does that

        return;
    }

    private PixelBRData buildSuggestEventPixelFromTemplate (UserRecord userRecord, 
                                                                long logTime, 
                                                                String refUrl,
                                                                String url, 
                                                                String selectedAqTerm,
                                                                String selectedQueryPid,
                                                                PixelTemplates pixelTemplates,
                                                                boolean testData)  throws Exception {
        PixelBRData pixelData;
        BuildSuggestEventPixel pixelBuilder;
        int buildStatus;

        // prepare a suggestEvent pixelLog template
        pixelData = pixelTemplates.loadPixelTemplate (userRecord.getDeviceType());

        // update template
        pixelBuilder = new BuildSuggestEventPixel ();
        buildStatus = pixelBuilder.build (pixelData, userRecord, logTime, refUrl, url,
                                          selectedAqTerm, selectedQueryPid, testData);
 
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // return pixelLog object from this builder
            return (pixelData);
        }

        return (null);
    }

    private ProductDetails selectProductFromSuggestedProductList (StepResult prevStepResult,
                                                                  UserRecord userRecord, 
                                                                  ArrayList <SuggestProductInfo> suggestProductInfoList,
                                                                  ProductFeed productFeed,
                                                                  ProductSelector productSelector) throws Exception {
        ArrayList <ProductDetails> productDetailsList;
        ProductDetails selectedProductDetails = null;

        productDetailsList = new ArrayList <ProductDetails> ();
        for (SuggestProductInfo suggestProductInfo : suggestProductInfoList) {
            ProductDetails productDetails;
            String skuid;
            Double productPrice;
            String productStyle;

            // suggest api -> by default, suggested product info does not include product's sku (if any)
            // Therefore use feed record to get its sku. It may be null if product has no skus
            skuid = productFeed.lookupProductSkuId (suggestProductInfo.getPid ());

            // suggest-product-info has no 'price', get it from the feed
            try {
                productPrice = Double.valueOf (productFeed.lookupProductPrice (suggestProductInfo.getPid ()));
            } catch (NumberFormatException nfe) {
                productPrice = 0.01;
            } catch (Exception e) {
                productPrice = 0.02;
            }

            // by default suggest-product-info does not have product's 'style' (currently available in PacificApparel catalog )
            // get it from productFeed if it is available
            productStyle = productFeed.lookupProductStyle (suggestProductInfo.getPid());

            productDetails = new ProductDetails ();
            productDetails.setPid (suggestProductInfo.getPid ());
            productDetails.setUrl (suggestProductInfo.getUrl ());   // will have a _br_psugg_ added in the url
            productDetails.setTitle (suggestProductInfo.getTitle ());
            productDetails.setSalePrice (suggestProductInfo.getSalePrice ());
            productDetails.setSkuid (skuid);
            productDetails.setPrice (productPrice);
            productDetails.setStyle (productStyle);

            productDetailsList.add (productDetails);
        }

        // from this 'productList', select a product (using segment-rules if any)
        if (productDetailsList.size () > 0) 
            selectedProductDetails = productSelector.selectProduct (prevStepResult, userRecord, productDetailsList);
            
        return selectedProductDetails; // may be null if no appropriate product available
    }
}

