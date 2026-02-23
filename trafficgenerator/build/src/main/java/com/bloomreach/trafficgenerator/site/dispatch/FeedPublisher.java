package com.bloomreach.trafficgenerator.site.dispatch;

import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.config.*;

public class FeedPublisher {

    private final static int DATACONNECT_OPERATION_SUCCESS = 0;
    private final static int DATACONNECT_OPERATION_FAIL = -1;
    private final static int DATACONNECT_OPERATION_INTERRUPTED = -2;
    private final static int DATACONNECT_OPERATION_CONTINUE_WAIT = 1;
    private final static int WAIT_INTERRUPTED = -3;
    private final static long MTB_CHECKSTATUS = 15 * 1000;    // N seconds in millisecs

    private final static int DEFAULT_READ_TIMEOUT = 5 * 60 * 1000; // timeout = N min, in millisec

    public FeedPublisher () {
    }

    // both ingest and index are done in this method
    public void publish (String feedFilePath, String realm) throws Exception {
        String jobId;
        int waitStatus;
        String dcEndpoint;
        String bearerStr;

        MessageLogger.logDebug (String.format ("feed file path = %s, realm = %s", feedFilePath, realm));
        dcEndpoint = GeneratorConstants.DATACONNECT_API_ENDPOINT_V3;

        // create once
        bearerStr = String.format ("%s %s", "Bearer", SiteConfig.getAccountConfigParam ("DATACONNECT_ACCESS_KEY"));

        try {
            jobId = ingestFeed (feedFilePath, dcEndpoint, realm, bearerStr);
            MessageLogger.logDebug (String.format ("Ingest jobId = %s", jobId));
        } catch (Exception e) {
            MessageLogger.logError (String.format ("Feed ingestion unsuccessful: %s", e.getMessage()));
            throw new Exception ("Feed ingest status unsuccessful");
        }

        try {
            waitStatus = waitForCompletion (jobId, dcEndpoint, realm, bearerStr);
            if (waitStatus != DATACONNECT_OPERATION_SUCCESS) 
                throw new Exception ("Feed ingest status unsuccessful: " + waitStatus);
        } catch (Exception e) {
            MessageLogger.logError (String.format ("WaitForCompletion unsuccessful: %s", e.getMessage()));
            throw new Exception ("dataConnection operation unsuccessful");
        }

        try {
            jobId = indexFeed (dcEndpoint, realm, bearerStr);
            MessageLogger.logDebug (String.format ("Index jobId = %s", jobId));
        } catch (Exception e) {
            MessageLogger.logError (String.format ("Feed index unsuccessful: %s", e.getMessage()));
            throw new Exception ("Feed index status unsuccessful");
        }

        try {
            waitStatus = waitForCompletion (jobId, dcEndpoint, realm, bearerStr);
            if (waitStatus != DATACONNECT_OPERATION_SUCCESS) 
                throw new Exception ("Feed ingest status unsuccessful: " + waitStatus);
        } catch (Exception e) {
            MessageLogger.logError (String.format ("WaitForCompletion unsuccessful: %s", e.getMessage()));
            throw new Exception ("dataConnection operation unsuccessful");
        }
    }

    private String ingestFeed (String feedFilePath, String dcEndpoint, String realm, String bearerStr) throws Exception {
        HttpsURLConnection conn;
        String outputFeedStr;
        String jobId;

        MessageLogger.logDebug ("start ingestFeed");

        {
            FileReader fileReader;
            BufferedReader bufferedFeedReader;
            StringBuffer outputBuffer;
            String srcLine;

            fileReader = new FileReader (feedFilePath);
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

            serverUrlStr = String.format ("%s/accounts/%s/catalogs/%s/environments/%s/records", dcEndpoint,
                                                                             SiteConfig.getAccountConfigParam ("ACCOUNT_NAME"),
                                                                             SiteConfig.getAccountConfigParam ("CATALOG_NAME"),
                                                                             realm);
            serverUrl = new URL (serverUrlStr);
            conn = (HttpsURLConnection) serverUrl.openConnection ();
            conn.setRequestMethod ("PUT");
            conn.setDoOutput (true);
            conn.setRequestProperty ("Content-Type", "application/json-patch+jsonlines");
            conn.setRequestProperty ("Authorization", bearerStr);
            conn.setRequestProperty ("Accept", "application/json");
            conn.setReadTimeout (DEFAULT_READ_TIMEOUT);
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
            JSONObject receivedJsonData;

            MessageLogger.logDebug ("ingest waiting for response");
            responseCode = conn.getResponseCode ();
            if (responseCode != 200) {
                MessageLogger.logError (String.format ("Bad ingest response code: %s", responseCode));
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
            receivedJsonData = receivedJson.getJSONObject ("data");
            jobId = (String) receivedJsonData.get ("job_id"); 
        }

        conn.disconnect ();
        MessageLogger.logDebug ("ingestFeed complete"); 
        return jobId;
    }

    private int waitForCompletion (String jobId, String dcEndpoint, String realm, String bearerStr) throws Exception {
        int opStat;

        do {
            try {
                Thread.currentThread().sleep (MTB_CHECKSTATUS);
                opStat = checkJobStatus (jobId, dcEndpoint, realm, bearerStr);
            } catch (InterruptedException ie) {
                opStat = WAIT_INTERRUPTED;
                break;
            } 
        } while (opStat == DATACONNECT_OPERATION_CONTINUE_WAIT);

        return opStat;
    }
         
    private int checkJobStatus (String jobId, String dcEndpoint, String realm, String bearerStr) throws Exception {
        HttpsURLConnection conn;
        int opStat;

        {
            String serverUrlStr;
            URL serverUrl;

            serverUrlStr = String.format ("%s/accounts/%s/catalogs/%s/environments/%s/jobs/%s", dcEndpoint, 
                                                                            SiteConfig.getAccountConfigParam ("ACCOUNT_NAME"),
                                                                            SiteConfig.getAccountConfigParam ("CATALOG_NAME"),
                                                                            realm, jobId);

            serverUrl = new URL (serverUrlStr);
            conn = (HttpsURLConnection) serverUrl.openConnection ();
            conn.setRequestMethod ("GET");
            conn.setRequestProperty ("Content-Type", "application/json-patch+jsonlines");
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
            JSONObject receivedJsonData;
            JSONObject receivedJsonDataJob;
            String status;

            MessageLogger.logDebug ("waiting for checkstatus response...");
            responseCode = conn.getResponseCode ();
            if (responseCode != 200) {
                MessageLogger.logError (String.format ("Bad checkstatus response code: %s", responseCode)); 
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
            receivedJsonData = receivedJson.getJSONObject ("data");
            receivedJsonDataJob = receivedJsonData.getJSONObject ("job"); 
            status = (String) receivedJsonDataJob.get ("status"); 
            switch (status) {
                case "creating": // 'creating': doesn't exist in V3 ???
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
        MessageLogger.logDebug ("\t -- done checkJobStatus"); 
        return opStat;
    }

    private String indexFeed (String dcEndpoint, String realm, String bearerStr) throws Exception {
        HttpsURLConnection conn;
        String jobId;

        MessageLogger.logDebug ("start indexFeed");

        {
            String serverUrlStr;
            URL serverUrl;

            serverUrlStr = String.format ("%s/accounts/%s/catalogs/%s/environments/%s/indexes", dcEndpoint,
                                                                             SiteConfig.getAccountConfigParam ("ACCOUNT_NAME"),
                                                                             SiteConfig.getAccountConfigParam ("CATALOG_NAME"),
                                                                             realm);
            serverUrl = new URL (serverUrlStr);
            conn = (HttpsURLConnection) serverUrl.openConnection ();
            conn.setRequestMethod ("POST");
            conn.setRequestProperty ("Content-Type", "application/json-patch+jsonlines");
            conn.setRequestProperty ("Authorization", bearerStr);
            conn.setRequestProperty ("Accept", "application/json");
            conn.setReadTimeout (DEFAULT_READ_TIMEOUT);
        }

        {
            InputStream response;
            InputStreamReader responseReader;
            BufferedReader bufferedReader;
            String inputLine;
            int responseCode;
            StringBuffer receivedBuf;
            JSONObject receivedJson;
            JSONObject receivedJsonData;

            MessageLogger.logDebug ("waiting for index response...");
            responseCode = conn.getResponseCode ();
            if (responseCode != 200) {
                MessageLogger.logError (String.format ("Bad index response code: %s", responseCode));
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
            receivedJsonData = receivedJson.getJSONObject ("data");
            jobId = (String) receivedJsonData.get ("job_id"); 
        }

        return jobId;
    }

}

