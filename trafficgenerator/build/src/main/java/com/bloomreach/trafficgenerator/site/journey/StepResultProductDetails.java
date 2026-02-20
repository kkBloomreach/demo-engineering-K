package com.bloomreach.trafficgenerator.site.journey;

public class StepResultProductDetails extends StepResult {

    private ProductDetails selectedProductDetails;

    public StepResultProductDetails () {
        super ();
    }

    // data provided via a searchApi response
    public void setProductDetails (ProductDetails selectedProductDetails) {
        this.selectedProductDetails = selectedProductDetails;
    }

    @Override
    public ProductDetails getData () {
        return this.selectedProductDetails;
    } 
}

