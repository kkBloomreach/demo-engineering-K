package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

public class StepResultProductList extends StepResult {

    private ArrayList <ProductDetails> productList;

    public StepResultProductList () {
        super ();
    }

    // data provided via a searchApi response
    public void setProductList (ArrayList<ProductDetails> productList) {
        this.productList = productList;
    }

    @Override
    public ArrayList<ProductDetails> getData () {
        return this.productList;
    } 
}

