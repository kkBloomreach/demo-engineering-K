package com.bloomreach.trafficgenerator.site.discoveryconnector.nonuseraccess;

import com.bloomreach.trafficgenerator.site.journeydata.templates.ApiBRData;

public class SearchQueryExecutor extends QueryExecutor {

    public SearchQueryExecutor () {
        super (ApiBRData.SEARCH_TYPE_KEYWORD);
    }
}


/**
    private final static String APICALL_TEMPLATE = "$API_ENDPOINT?account_id=$ACCT_ID&auth_key=$AUTH_KEY&domain_key=$DOMAIN_KEY&request_id=7057573203767&_br_uid_2=$BR_UID_2&url=www.bloomique.com&ref_url=www.bloomique.com&request_type=search&rows=$MAX_ROWS&start=$START&fl=pid%2Cprice%2Csale_price%2Curl%2Ctitle%2Cskuid&q=$QUERY&search_type=keyword";
    public SearchQueryExecutor () {
        ApiBRData apiData;

        apiData = new ApiBRData ();
apiCall = apiCall.replace ("$ACCT_ID", SiteConfig.getAccountConfigParam ("ACCOUNT_ID"));
        apiCall = apiCall.replace ("$AUTH_KEY", SiteConfig.getAccountConfigParam ("AUTH_KEY"));
        apiCall = apiCall.replace ("$DOMAIN_KEY", SiteConfig.getAccountConfigParam ("DOMAIN"));
        apiCall = apiCall.replace ("$START", Integer.toString (start));  // again changed below for each loop
        apiCall = apiCall.replace ("$MAX_ROWS", Integer.toString (numRows));

    }

**/
