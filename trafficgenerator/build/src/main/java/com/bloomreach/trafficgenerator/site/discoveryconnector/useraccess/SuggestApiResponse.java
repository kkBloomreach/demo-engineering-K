package com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess;

import java.util.ArrayList;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildSearchResultPagePixel;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildCategoryPagePixel;

public class SuggestApiResponse {

    private ArrayList <String> suggestTerms;
    private ArrayList <SuggestProductInfo> suggestProducts; 
    private ArrayList <String> suggestCategories; // contains catIds

    public SuggestApiResponse () {
    }

    public void setSuggestTerms (ArrayList<String> suggestTerms) {
        this.suggestTerms = suggestTerms;
    }

    public void setSuggestProducts (ArrayList<SuggestProductInfo> suggestProducts) {
        this.suggestProducts = suggestProducts;
    }

    public void setSuggestCategories (ArrayList<String> suggestCategories) {
        this.suggestCategories = suggestCategories;
    }

    public ArrayList<String> getSuggestTerms () {
        return this.suggestTerms;
    }

    public ArrayList<SuggestProductInfo> getSuggestProducts () {
        return this.suggestProducts;
    }

    public ArrayList<String> getSuggestCategories () {
        return this.suggestCategories;
    }

    // a 'term' is needed even if a product or category is selected from suggest
    // api response. Therefore we have this selectRandom method in this class itself
    public String selectSuggestResponseTermAtRandom (String currentUrl) {
        String selectedTerm = null;

        if ((this.suggestTerms != null) && (this.suggestTerms.size() > 0)) {
            int randomIndx;
            String selectedTermSearchResultPageUrl;

            randomIndx = (int) (Math.random () * this.suggestTerms.size());
            selectedTerm = this.suggestTerms.get (randomIndx);
            selectedTermSearchResultPageUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedTerm);
            if ((currentUrl != null) && (currentUrl.equals (selectedTermSearchResultPageUrl))) {
                // if there is only one suggested term which also happens to match the one 
                // we have, return null (ie, there is no available term)
                if (this.suggestTerms.size () > 1) {
                    randomIndx = (randomIndx + 1) % this.suggestTerms.size();
                    selectedTerm = this.suggestTerms.get (randomIndx);
                } else {
                    selectedTerm = null; // only one term in response and that too has same url
                }
            }
        }

        return selectedTerm;
    }

    // a 'term' is needed even if a product or category is selected from suggest
    // api response. Therefore we have this selectRandom method in this class itself
    public String selectSuggestResponseCategoryAtRandom (String currentUrl) {
        String selectedCategory = null;

        if ((this.suggestCategories != null) && (this.suggestCategories.size() > 0)) {
            int randomIndx;
            String selectedCatPageUrl;

            randomIndx = (int) (Math.random () * this.suggestCategories.size());
            selectedCategory = this.suggestCategories.get (randomIndx);
            selectedCatPageUrl = BuildCategoryPagePixel.getCategoryPageUrl (selectedCategory);
            if ((currentUrl != null) && (currentUrl.equals (selectedCatPageUrl))) {
                // if there is only one suggested category which also happens to match the one 
                // we have, return null (ie, there is no available category)
                if (this.suggestCategories.size () > 1) {
                    randomIndx = (randomIndx + 1) % this.suggestCategories.size();
                    selectedCategory = this.suggestCategories.get (randomIndx);
                } else {
                    selectedCategory = null; // only one cat in response and that too has same url
                }
            }
        }

        return selectedCategory;
    }
}

