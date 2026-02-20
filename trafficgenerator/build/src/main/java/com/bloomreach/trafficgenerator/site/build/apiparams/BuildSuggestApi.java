package com.bloomreach.trafficgenerator.site.build.apiparams;


import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;

public class BuildSuggestApi extends BuildApiBase  {

    public BuildSuggestApi () {
    }

    public int build (ApiBRData apiData, 
                      UserRecord userRecord,
                      long logTime, 
                      String refUrl, 
                      String url, 
                      String selectedAqTerm) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonSuggestFields (apiData, userRecord, logTime);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            // common method in base class
            updateSuggestApiParams (apiData, 
                                    url, 
                                    refUrl, 
                                    selectedAqTerm,
                                    ApiBRData.DEFAULT_FL_LIST);  // query 

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }

}

