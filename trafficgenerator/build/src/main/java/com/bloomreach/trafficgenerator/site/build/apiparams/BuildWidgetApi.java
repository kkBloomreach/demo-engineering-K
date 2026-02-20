package com.bloomreach.trafficgenerator.site.build.apiparams;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;

public class BuildWidgetApi extends BuildApiBase  {

    public BuildWidgetApi () {
    }

    public int build (ApiBRData apiData, 
                      UserRecord userRecord,
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String wq,    // query (optional for non-query widgets)
                      String wid,   // widget id
                      String itemId,// optional
                      CampaignRecord currentCampaignRecord) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonWidgetFields (apiData, userRecord, currentCampaignRecord, logTime);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            String segment;

            segment = userRecord.getSegment ();

            // common method in base class
            updateWidgetApiParams (apiData, 
                                   url, 
                                   refUrl, 
                                   wid,   // widget id
                                   wq,    // query (optional for non-query widgets)
                                   itemId,// optional
                                   ApiBRData.DEFAULT_FL_LIST,
                                   segment);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }
}

