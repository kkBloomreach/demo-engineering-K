package com.bloomreach.trafficgenerator.site.build.apiparams;


import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;

public class BuildSearchApi extends BuildApiBase  {

    public BuildSearchApi () {
    }

    public int build (ApiBRData apiData, 
                      UserRecord userRecord,
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String selectedSearchTerm,
                      CampaignRecord currentCampaignRecord) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonSearchFields (apiData, userRecord, currentCampaignRecord, logTime);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String segment;

            segment = userRecord.getSegment ();

            // common method in base class
            updateSearchApiParams (apiData, 
                             ApiBRData.SEARCH_TYPE_KEYWORD,
                             url, 
                             refUrl, 
                             selectedSearchTerm,  // query 
                             ApiBRData.DEFAULT_FL_LIST,
                             segment);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }
}

/**
//            url = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedSearchTerm);
**/
