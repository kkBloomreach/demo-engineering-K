package com.bloomreach.analyticsdatagenerator.generate;

import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;

public class CategoryQueryExecutor extends QueryExecutor {

    private final static String APICALL_TEMPLATE = "http://staging-core.dxpapi.com/api/v1/core/?account_id=$ACCT_ID&auth_key=$AUTH_KEY&domain_key=$DOMAIN_KEY&request_id=6031452347853&_br_uid_2=$BR_UID_2&url=www.bloomique.com&ref_url=www.bloomique.com&request_type=search&rows=10&start=0&fl=pid%2Ctitle%2Cbrand%2Cprice%2Csale_price%2Cpromotions%2Cthumb_image%2Csku_thumb_images%2Csku_swatch_images%2Csku_color_group%2Curl%2Cprice_range%2Csale_price_range%2Cdescription&q=$QUERY&search_type=category";

    public CategoryQueryExecutor (GeneratorInputData inputData) {
        super (APICALL_TEMPLATE, inputData);
    }
}

