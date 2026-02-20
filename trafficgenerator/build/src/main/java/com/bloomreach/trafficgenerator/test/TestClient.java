package com.bloomreach.trafficgenerator.test;

import java.net.URL;
import java.io.*;
import java.net.HttpURLConnection;
//import javax.net.ssl.HttpsURLConnection;

public class TestClient {

    private final static String TARGET_ENDPOINT = "127.0.0.1:8080";

    public static void main (String[] args) {
        TestClient t;

        t = new TestClient ();
        try {
            t.doTest (args);
        } catch (Exception e) {
            // e.printStackTrace ();
            System.err.printf ("doTest Exception: %s\n", e.getMessage());
        }
    }

    private TestClient () {
    }

    private void doTest (String[] args) throws Exception {
        String serverUrlStr;
        URL serverUrl;
        HttpURLConnection conn;
        InputStream response;
        InputStreamReader responseReader;
        BufferedReader bufferedReader;
        String inputLine;
        int responseCode;

        System.out.println ("in doTest");
        // serverUrlStr = "http://127.0.0.1:8080/test.html";
        serverUrlStr = String.format ("http://%s/test.html", TARGET_ENDPOINT);
        serverUrl = new URL (serverUrlStr);
        conn = (HttpURLConnection) serverUrl.openConnection ();
        System.out.println ("waiting for response...");
        responseCode = conn.getResponseCode ();
        System.out.printf ("response code %s\n", responseCode);
 
        response = conn.getInputStream ();
        responseReader = new InputStreamReader (response);
        bufferedReader = new BufferedReader (responseReader);
        while ((inputLine = bufferedReader.readLine()) != null) {
            System.out.println (inputLine);
        } 
        System.out.println ("done doTest"); 
    }
}

