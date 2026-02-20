package com.bloomreach.trafficgenerator.site.journeydata.queryexecutor;

public class SearchQueryExecutor extends QueryExecutor {

    private final static String APICALL_TEMPLATE = "$API_ENDPOINT?account_id=$ACCT_ID&auth_key=$AUTH_KEY&domain_key=$DOMAIN_KEY&request_id=7057573203767&_br_uid_2=$BR_UID_2&url=www.bloomique.com&ref_url=www.bloomique.com&request_type=search&rows=$MAX_ROWS&start=$START&fl=pid%2Cprice%2Csale_price%2Curl%2Ctitle%2Cskuid&q=$QUERY&search_type=keyword";
    public SearchQueryExecutor () {
        super (APICALL_TEMPLATE);
    }
}

// fl=pid%2Cprice%2Csale_price%2Curl%2Ctitle%2Cskuid

