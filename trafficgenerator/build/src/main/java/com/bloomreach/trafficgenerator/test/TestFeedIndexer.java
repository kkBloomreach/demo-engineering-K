package com.bloomreach.trafficgenerator.test;

import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

public class TestFeedIndexer {

    private final static String DC_ENDPOINT_STAGING = "https://api-staging.connect.bloomreach.com/dataconnect/api/v1";
    private final static String REALM = "staging";
    private final static String ACCOUNT_ID = "6475";
    private final static String CATALOG_NAME = "pacific_supply_mindcurv";
    private final static String DC_ACCESS_KEY = "pacific_supply_mindcurv-staging-814bfe11-bfcc-4138-b7ea-d01a81462fe2";
    // private final static String FILE_JSONL_FEED  = "/Users/kirankundargi/tmp/brlab/test/trafficgenerator/data/pacific_supply_mindcurv/input/feed/daily/feed.jsonl";
    private final static String FILE_JSONL_FEED  = "/Users/kirankundargi/tmp/brlab/test/trafficgenerator/data/pacific_supply_mindcurv/input/feed/daily/testfeed.jsonl";

    private final static int DATACONNECT_OPERATION_SUCCESS = 0;
    private final static int DATACONNECT_OPERATION_FAIL = -1;
    private final static int DATACONNECT_OPERATION_INTERRUPTED = -2;
    private final static int DATACONNECT_OPERATION_CONTINUE_WAIT = 1;
    private final static int WAIT_INTERRUPTED = -3;
    private final static long MTB_CHECKSTATUS = 500;    // millisecs

    public static void main (String[] args) {
        TestFeedIndexer t;

        t = new TestFeedIndexer ();
        try {
            t.doTest (args);
        } catch (Exception e) {
            e.printStackTrace ();
            System.err.printf ("doTest Exception: %s\n", e.getMessage());
        }
    }

    private TestFeedIndexer () {
    }

    private void doTest (String[] args) throws Exception {
        String jobId = null;
        int waitStatus;

/**
        try {
            jobId = doIngest (args);
        } catch (Exception e) {
            System.err.printf ("WaitForCompletion unsuccessful: %s\n", e.getMessage());
            throw new Exception ("dataConnection operation unsuccessful");
        }

        if (jobId == null)
            throw new Exception ("Ingest operation failed");
 
        try {
            waitStatus = waitForCompletion (jobId);
            if (waitStatus != DATACONNECT_OPERATION_SUCCESS)
                throw new Exception ("Feed ingest status unsuccessful: " + waitStatus);
        } catch (Exception e) {
            System.err.printf ("WaitForCompletion unsuccessful: %s\n", e.getMessage());
            throw new Exception ("dataConnection operation unsuccessful");
        }
**/

        jobId = doIndex  ();
        if (jobId == null)
            throw new Exception ("Index operation failed");

        try {
            waitStatus = waitForCompletion (jobId);
            if (waitStatus != DATACONNECT_OPERATION_SUCCESS)
                throw new Exception ("Feed ingest status unsuccessful: " + waitStatus);
        } catch (Exception e) {
            System.err.printf ("WaitForCompletion unsuccessful: %s\n", e.getMessage());
            throw new Exception ("dataConnection operation unsuccessful");
        }

        System.out.println ("doTest complete");
    }

    private String doIngest (String[] args) throws Exception {
        HttpsURLConnection conn;
        String outputFeedStr;

        String jobId;

        System.out.println ("in doIngest");

        {
            FileReader fileReader;
            BufferedReader bufferedFeedReader;
            StringBuffer outputBuffer;
            String srcLine;

            fileReader = new FileReader (FILE_JSONL_FEED);
            bufferedFeedReader = new BufferedReader (fileReader);
            outputBuffer = new StringBuffer ();
            while ((srcLine = bufferedFeedReader.readLine()) != null) {
                // dataOutStream.writeBytes (srcLine);
                outputBuffer.append (srcLine);
                outputBuffer.append ("\n");
            }
 
            bufferedFeedReader.close ();
            fileReader.close ();

            outputFeedStr = outputBuffer.toString ();
        }

        {
            String serverUrlStr;
            URL serverUrl;
            String bearerStr;

            serverUrlStr = String.format ("%s/accounts/%s/catalogs/%s/products", DC_ENDPOINT_STAGING, ACCOUNT_ID, CATALOG_NAME);
            serverUrl = new URL (serverUrlStr);
            conn = (HttpsURLConnection) serverUrl.openConnection ();
            conn.setRequestMethod ("PUT");
            conn.setDoOutput (true);
            conn.setRequestProperty ("Content-Type", "application/json-patch+jsonlines");
            bearerStr = String.format ("%s %s", "Bearer", DC_ACCESS_KEY);   // "Bearer asdsadsa"
            conn.setRequestProperty ("Authorization", bearerStr);
            conn.setRequestProperty ("Accept", "application/json");
        }

        {
            OutputStream outStream;
            OutputStreamWriter streamWriter;
 
            outStream = conn.getOutputStream ();
            streamWriter = new OutputStreamWriter (outStream);
            streamWriter.write (outputFeedStr);

            streamWriter.flush ();
            outStream.flush ();
        }

        {
            InputStream response;
            InputStreamReader responseReader;
            BufferedReader bufferedReader;
            String inputLine;
            int responseCode;
            StringBuffer receivedBuf;
            JSONObject receivedJson;

            System.out.println ("waiting for response...");
            responseCode = conn.getResponseCode ();
            System.out.printf ("response code %s\n", responseCode);
            if (responseCode != 200) {
                System.err.printf ("Bad response code: %s", responseCode);
                return null;
            }
 
            response = conn.getInputStream ();
            responseReader = new InputStreamReader (response);
            bufferedReader = new BufferedReader (responseReader);
            receivedBuf = new StringBuffer ();

            while ((inputLine = bufferedReader.readLine()) != null) {
                receivedBuf.append (inputLine);
            }

            receivedJson = new JSONObject (new String (receivedBuf));
            jobId = (String) receivedJson.get ("jobId"); 
        }

        conn.disconnect ();
        System.out.println ("done doIngest"); 
        return jobId;
    }

    private int waitForCompletion (String jobId) throws Exception {
        int opStat;

        do {
            try {
                Thread.currentThread().sleep (MTB_CHECKSTATUS);
                opStat = checkJobStatus (jobId);
            } catch (InterruptedException ie) {
                opStat = WAIT_INTERRUPTED;
                break;
            } 
        } while (opStat == DATACONNECT_OPERATION_CONTINUE_WAIT);

        return opStat;
    }
         
    private int checkJobStatus (String jobId) throws Exception {
        HttpsURLConnection conn;
        int opStat;

        {
            String serverUrlStr;
            URL serverUrl;
            String bearerStr;

            serverUrlStr = String.format ("%s/jobs/%s", DC_ENDPOINT_STAGING, jobId);
            serverUrl = new URL (serverUrlStr);
            conn = (HttpsURLConnection) serverUrl.openConnection ();
            conn.setRequestMethod ("GET");
            conn.setRequestProperty ("Content-Type", "application/json-patch+jsonlines");
            bearerStr = String.format ("%s %s", "Bearer", DC_ACCESS_KEY);   // "Bearer asdsadsa"
            conn.setRequestProperty ("Authorization", bearerStr);
            conn.setRequestProperty ("Accept", "application/json");
        }

        {
            InputStream response;
            InputStreamReader responseReader;
            BufferedReader bufferedReader;
            String inputLine;
            int responseCode;
            StringBuffer receivedBuf;
            JSONObject receivedJson;
            String status;

            System.out.println ("waiting for response...");
            responseCode = conn.getResponseCode ();
            System.out.printf ("response code %s\n", responseCode);
            if (responseCode != 200) {
                System.err.printf ("Bad response code: %s", responseCode); 
                return DATACONNECT_OPERATION_FAIL;
            }
 
            response = conn.getInputStream ();
            responseReader = new InputStreamReader (response);
            bufferedReader = new BufferedReader (responseReader);
            receivedBuf = new StringBuffer ();
            while ((inputLine = bufferedReader.readLine()) != null) {
                receivedBuf.append (inputLine);
            }

            receivedJson = new JSONObject (new String (receivedBuf));
            status = (String) receivedJson.get ("status"); 
            switch (status) {
                case "creating":
                case "queued":
                case "running":
                    opStat = DATACONNECT_OPERATION_CONTINUE_WAIT;
                    break;

                case "success":
                    opStat = DATACONNECT_OPERATION_SUCCESS;
                    break;
 
                case "failed":
                case "skipped":
                case "killed":
                    opStat = DATACONNECT_OPERATION_FAIL;
                    break;

                default:
                    opStat = DATACONNECT_OPERATION_FAIL;    // "unknown" status
            }
        }

        conn.disconnect ();
        System.out.println ("done checkJobStatus"); 
        return opStat;
    }

    private String doIndex () throws Exception {
        HttpsURLConnection conn;
        String jobId;

        System.out.println ("in doIndex");

        {
            String serverUrlStr;
            URL serverUrl;
            String bearerStr;

            serverUrlStr = String.format ("%s/accounts/%s/catalogs/%s/indexes", DC_ENDPOINT_STAGING, ACCOUNT_ID, CATALOG_NAME);
            serverUrl = new URL (serverUrlStr);
            conn = (HttpsURLConnection) serverUrl.openConnection ();
            conn.setRequestMethod ("POST");
            conn.setRequestProperty ("Content-Type", "application/json-patch+jsonlines");
            bearerStr = String.format ("%s %s", "Bearer", DC_ACCESS_KEY);   // "Bearer asdsadsa"
            conn.setRequestProperty ("Authorization", bearerStr);
            conn.setRequestProperty ("Accept", "application/json");
        }

        {
            InputStream response;
            InputStreamReader responseReader;
            BufferedReader bufferedReader;
            String inputLine;
            int responseCode;
            StringBuffer receivedBuf;
            JSONObject receivedJson;

            System.out.println ("waiting for response...");
            responseCode = conn.getResponseCode ();
            System.out.printf ("response code %s\n", responseCode);
            if (responseCode != 200) {
                System.err.printf ("Bad response code: %s", responseCode);
                return null;
            }
 
            response = conn.getInputStream ();
            responseReader = new InputStreamReader (response);
            bufferedReader = new BufferedReader (responseReader);
            receivedBuf = new StringBuffer ();

            while ((inputLine = bufferedReader.readLine()) != null) {
                receivedBuf.append (inputLine);
            }

            receivedJson = new JSONObject (new String (receivedBuf));
            jobId = (String) receivedJson.get ("jobId"); 
        }

        return jobId;
    }
}

