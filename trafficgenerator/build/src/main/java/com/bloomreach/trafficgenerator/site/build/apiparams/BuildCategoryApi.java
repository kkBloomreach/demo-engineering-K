package com.bloomreach.trafficgenerator.site.build.apiparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;

public class BuildCategoryApi extends BuildApiBase  {

    public BuildCategoryApi () {
    }

    public int build (ApiBRData apiData, 
                      UserRecord userRecord,
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String selectedCatId,
                      CampaignRecord activeCampaignRecord) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonSearchFields (apiData, userRecord, activeCampaignRecord, logTime);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String segment;
            String catId;

            catId = selectedCatId;
            segment = userRecord.getSegment ();

            // common method in base class
            updateSearchApiParams (apiData, 
                             ApiBRData.SEARCH_TYPE_CATEGORY,
                             url, 
                             refUrl, 
                             catId,  // query 
                             ApiBRData.DEFAULT_FL_LIST,
                             segment);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }

}

