package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.site.dispatch.SearchApiResponse;

public class StepResultSearchResponse extends StepResult {

    private SearchApiResponse searchApiResponse = null;
    // keep the actual search term/catId so that selecting subsequent term (or cat) is not the same as this
    private String searchTerm = null;  // term for which we have this response
    private String searchCatId = null; // catId for which we have this response

    public StepResultSearchResponse () {
        super ();
    }

    public void setSearchApiResponse (SearchApiResponse searchApiResponse) {
        this.searchApiResponse = searchApiResponse;
    }

    public void setSearchTerm (String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public void setSearchCatId (String searchCatId) {
        this.searchCatId = searchCatId;
    }

    public String getSearchTerm () {
        return (this.searchTerm); // may be null
    }

    public String getSearchCatId () {
        return (this.searchCatId); // may be null
    }

    @Override
    public SearchApiResponse getData () {
        return this.searchApiResponse;
    } 
}

