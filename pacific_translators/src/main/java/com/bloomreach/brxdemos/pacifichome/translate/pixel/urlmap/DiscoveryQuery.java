package com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap;

import java.net.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.CloneConstants;

public class DiscoveryQuery {

    private final static String APICALL_TEMPLATE = "http://core.dxpapi.com/api/v1/core/?account_id=$ACCT_ID&auth_key=$AUTH_KEY&domain_key=$DOMAIN_KEY&request_id=7057573203767&_br_uid_2=uid%3D783645148319%3Av%3D11.8%3Ats%3D1658160725665%3Ahc%3D169&url=www.bloomique.com&ref_url=www.bloomique.com&request_type=search&rows=10&start=0&fl=pid&q=$QUERY&search_type=keyword";

    public DiscoveryQuery () {
    }

    public String getPid (String productLabel) throws Exception {
        String apiCall;
        String pid;

        apiCall = constructAPICall (productLabel);
        pid = performQuery (apiCall);

        return (pid);   // may be null
    }

    // product label expected to be urlEncoded
    private String constructAPICall (String productLabel) {
        String apiCall;

        apiCall = APICALL_TEMPLATE;
        apiCall = apiCall.replace ("$ACCT_ID", CloneConstants.PACIFICHOME_ACCOUNT_ID);
        apiCall = apiCall.replace ("$AUTH_KEY", CloneConstants.PACIFICHOME_AUTH_KEY);
        apiCall = apiCall.replace ("$DOMAIN_KEY", CloneConstants.PACIFICHOME_DOMAIN_KEY);
        apiCall = apiCall.replace ("$QUERY", productLabel); 

        //System.out.println (apiCall);
        return (apiCall);
    }

    private String performQuery (String apiCall) throws Exception {
        URL apiURL;
        HttpURLConnection urlConn;
        BufferedReader respReader;
        InputStream inStream;
        StringBuffer respBuffer;
        String respText;
        String pid = null;
        
        apiURL = new URL (apiCall);
        urlConn = (HttpURLConnection) apiURL.openConnection (); 
        urlConn.setRequestMethod ("GET");
        urlConn.setRequestProperty ("Content-Type", "application/json");

        if (urlConn.getResponseCode () == 200) {
            String inputLine;
            JSONObject respJson;
            JSONArray respDocs;
            JSONObject respObj;
            JSONObject respDoc0;

            // System.out.println ("Got status = 200");
            inStream = urlConn.getInputStream ();
            respReader = new BufferedReader (new InputStreamReader (inStream));
            respBuffer = new StringBuffer ();
            while ((inputLine = respReader.readLine ()) != null) {
                respBuffer.append (inputLine);
            }
            inStream.close ();

            respJson = new JSONObject (respBuffer.toString ());
            respObj = respJson.getJSONObject ("response");
            respDocs = respObj.getJSONArray ("docs");
            if (respDocs.length() > 0) {
                respDoc0 = respDocs.getJSONObject(0);
                pid = respDoc0.getString ("pid");
            }
        } else {
            System.out.println ("Got notOK status = " + urlConn.getResponseCode());
        }

        return (pid);
    }
}
