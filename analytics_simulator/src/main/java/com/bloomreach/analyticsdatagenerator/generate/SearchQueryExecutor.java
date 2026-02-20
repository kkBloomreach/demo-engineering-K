package com.bloomreach.analyticsdatagenerator.generate;

import java.net.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;

public class SearchQueryExecutor extends QueryExecutor {

    private final static String APICALL_TEMPLATE = "http://staging-core.dxpapi.com/api/v1/core/?account_id=$ACCT_ID&auth_key=$AUTH_KEY&domain_key=$DOMAIN_KEY&request_id=7057573203767&_br_uid_2=$BR_UID_2&url=www.bloomique.com&ref_url=www.bloomique.com&request_type=search&rows=10&start=0&fl=pid,price,title,brand,sku_swatch_images,skuid&q=$QUERY&search_type=keyword";

    public SearchQueryExecutor (GeneratorInputData inputData) {
        super (APICALL_TEMPLATE, inputData);
    }
}

