package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class StepSelectProductFromList extends StepBase {

    public StepSelectProductFromList () {
    }

    public StepResult handleStep (StepResultProductList prevStepResult,
                                  UserRecord userRecord,
                                  long logTime,
                                  ArrayList<ProductDetails> productList,
                                  ProductSelector productSelector) throws Exception {
        StepResultProductDetails thisStepResult;
        ProductDetails selectedProduct;
        StepResultInvalidData inputInvalid;

        MessageLogger.logDebug ("Handle step select product from list");

        if (productList == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSelectProductFromList, null productList");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        } else if (productList.size () == 0) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSelectProductFromList, empty productList");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        selectedProduct = handleStepInternal (prevStepResult, userRecord, logTime, productList, productSelector);
        // selected product may be null (EG, if productList does not include product corresponding to RTS rule)
        if (selectedProduct == null) {
            inputInvalid = new StepResultInvalidData ();
            inputInvalid.setRefUrl (prevStepResult.getRefUrl ());
            inputInvalid.setUrl (prevStepResult.getUrl ());
            inputInvalid.setMessage ("StepSelectProductFromList, no acceptable product available in the product list");
            inputInvalid.setEndTime (logTime + 1000);
            return inputInvalid;
        }

        thisStepResult = new StepResultProductDetails ();
        super.setUrlHistory (prevStepResult, thisStepResult, null); // 'select product' step itself does not change url
        thisStepResult.setProductDetails (selectedProduct);

        super.insertDuration (GeneratorConstants.TRAFFIC_STEP_DURATION_SELECT_PID_FROM_LIST);
        thisStepResult.setEndTime (logTime + GeneratorConstants.TRAFFIC_STEP_DURATION_SELECT_PID_FROM_LIST);

        return (thisStepResult);
    }

    private ProductDetails handleStepInternal (StepResult prevStepResult, 
                                               UserRecord userRecord,
                                               long logTime,
                                               ArrayList<ProductDetails> productList,
                                               ProductSelector productSelector) throws Exception {
        ProductDetails selectedProduct;

        selectedProduct = productSelector.selectProduct (prevStepResult, userRecord, productList);
        return selectedProduct;
    }
}

