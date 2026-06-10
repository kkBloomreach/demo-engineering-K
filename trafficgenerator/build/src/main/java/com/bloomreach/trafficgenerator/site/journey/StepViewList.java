package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess.*;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepViewList extends StepBase {

    public StepViewList () {
    }

    // build viewList from search response (term or category)
    public StepResult handleStep (StepResult prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  SearchApiResponse searchApiResponse) throws Exception {

        StepResultProductList thisStepResult;
        StepResultInvalidData inputInvalid;
        ArrayList <ProductDetails> productList;

        MessageLogger.logDebug ("Handle step view list from search response");
        if ((searchApiResponse == null) || (searchApiResponse.getResponseDocs () == null)) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepViewList, null api response");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } else if (searchApiResponse.getResponseDocs().size () == 0) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepViewList, empty search api response docs");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        // since above responseDocs.size > 0, this productList.size will also be > 0
        productList = handleStepInternal (prevStepResult, userRecord, logTime, searchApiResponse);
        thisStepResult = new StepResultProductList ();
        super.setUrlHistory (prevStepResult, thisStepResult, null);
        thisStepResult.setProductList (productList);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_VIEW_LIST);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_VIEW_LIST);

        return (thisStepResult);
    }

    private ArrayList <ProductDetails> handleStepInternal (StepResult prevStepResult,
                                                           UserRecord userRecord,
                                                           long logTime,
                                                           SearchApiResponse searchApiResponse) throws Exception {

        ArrayList <SearchApiResponseDoc> allResponseDocs;
        ArrayList <ProductDetails> productList;

        // build selectedProductDetail from searchApi response
        allResponseDocs = searchApiResponse.getResponseDocs (); // 'docs' from API response
        productList = new ArrayList <ProductDetails> ();
        for (SearchApiResponseDoc responseDoc : allResponseDocs) {
            ProductDetails pidDetails;

            pidDetails = new ProductDetails ();
            pidDetails.setPid (responseDoc.getPid ());
            pidDetails.setUrl (responseDoc.getUrl ());
            pidDetails.setPrice (responseDoc.getPrice());
            pidDetails.setSalePrice (responseDoc.getSalePrice ());
            pidDetails.setTitle (responseDoc.getTitle ());
            pidDetails.setSkuid (responseDoc.getSkuid ());
            pidDetails.setStyle (responseDoc.getStyle());

            productList.add (pidDetails);
        }

        return productList;
    }
}


