package com.bloomreach.analyticssimulator;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;

public class SimulatorConfig {

    private static JSONObject simulatorConfig = null;

    public SimulatorConfig () {
    }

    public boolean load (String sourceRootDirPath, String accountName) throws Exception {
        File configFile;
        String fileName;

        fileName = accountName + ".json";
        configFile = new File (sourceRootDirPath, fileName);
        if (configFile.exists () == false)
            return false;

        simulatorConfig = parseConfig (configFile);
        return true;
    }

    public static String getConfigParam (String paramName) {
        String paramValue = null;

        if (simulatorConfig == null)
            return null;

        try {
            paramValue = (String) simulatorConfig.get (paramName);
        } catch (Exception e) {
            MessageLogger.logError ("Cannot find param in simulatorConfig: " + paramName);
        }

        return (paramValue);
    }

    public static String getSegmentationType () {
        if (simulatorConfig.has ("RTCS") == true) 
            return (SimulatorConstants.SEGMENTATION_TYPE_RTCS);
        else if (simulatorConfig.has ("RBS") == true) 
            return (SimulatorConstants.SEGMENTATION_TYPE_RBS);
        return (SimulatorConstants.SEGMENTATION_TYPE_NONE);
    }

    // account may/may-not have any RTCS segments. If they are,
    // return segment names. This is used to list them in statistical report
    // returns null if account has no RTCS segments
    public static String[] getRTCSSegmentNames () {
        JSONObject rtcsJson = null;
        String[] segmentNames;
        ArrayList<String> segmentNameList;

        if (simulatorConfig.has ("RTCS") == false) {
            MessageLogger.logInfo ("No RTCS param in simulatorConfig");
            return null;
        }

        try {
            rtcsJson = (JSONObject) simulatorConfig.get ("RTCS");
        } catch (Exception e) {
            MessageLogger.logWarning ("No RTCS param in simulatorConfig");
        }
        if (rtcsJson == null) 
            return null;

        segmentNameList = new ArrayList <String> ();
        Iterator keys = rtcsJson.keys ();
        while (keys.hasNext ()) {
            segmentNameList.add ((String) keys.next());
        }
        segmentNames = segmentNameList.toArray (new String[0]);
        return segmentNames;
    }

    // for the given segment, return A:B where A is segmentationId, B is segmentId
    public static String getRTCSKeyValuePair (String segment) {
        JSONObject rtcsJson = null;
        JSONObject segmentJson = null;
        String segmentationId = null;
        String segmentId = null;
        String keyValuePair = null;

        try {
            rtcsJson = (JSONObject) simulatorConfig.get ("RTCS");
        } catch (Exception e) {
            MessageLogger.logWarning ("No RTCS param in simulatorConfig");
        }

        if (rtcsJson == null) 
            return null;

        Iterator<String> keys = rtcsJson.keys ();
        while (keys.hasNext ()) {
            if (keys.next().equals (segment) == true) {
                try {
                    segmentJson = (JSONObject) rtcsJson.get (segment);
                    segmentationId = (String) rtcsJson.get ("SEGMENTATION_ID");
                    segmentId = (String) segmentJson.get ("SEGMENT_ID");
                    break;
                } catch (Exception e) {
                    MessageLogger.logError ("No RTCS segment with name: " + segment);
                }
            }
        }

        if (segmentationId == null || segmentId == null)
            return null;

        keyValuePair = segmentationId + ":" + segmentId;
        return keyValuePair;
    }

    // currently only 'customer-profile' is supported
    public String[] getRBSCustomerProfileNames () {
        JSONObject rbsJson = null;
        JSONObject customerProfileJson = null;
        String[] segmentNames;
        ArrayList<String> segmentNameList;

        if (simulatorConfig.has ("RBS") == false) {
            MessageLogger.logInfo ("No RBS param in simulatorConfig");
            return null;
        }

        try {
            rbsJson = (JSONObject) simulatorConfig.get ("RBS");
            customerProfileJson = rbsJson.getJSONObject ("CUSTOMER_PROFILE");
        } catch (Exception e) {
            MessageLogger.logError ("No RBS param and/or CustomerProfile in simulatorConfig");
        }
        if ((rbsJson == null) || (customerProfileJson == null))
            return null;

        segmentNameList = new ArrayList <String> ();
        Iterator keys = customerProfileJson.keys ();
        while (keys.hasNext ()) {
            segmentNameList.add ((String) keys.next());
        }
        segmentNames = segmentNameList.toArray (new String[0]);
        return segmentNames;
    }

    // returned value is used in pixelLogBuilder.setCustomerProfile API
    public static String getRBSCustomerProfileValue (String segment) {
        JSONObject rbsJson = null;
        JSONObject customerProfileJson = null;
        JSONObject segmentJson = null;
        String customerProfileName = null;
        String keyValuePair = null;

        if (simulatorConfig.has ("RBS") == false) {
            MessageLogger.logInfo ("No RBS param in simulatorConfig");
            return null;
        }

        try {
            rbsJson = (JSONObject) simulatorConfig.get ("RBS");
            customerProfileJson = rbsJson.getJSONObject ("CUSTOMER_PROFILE");
        } catch (Exception e) {
            MessageLogger.logError ("No RBS param and/or CustomerProfile in simulatorConfig");
        }
        if ((rbsJson == null) || (customerProfileJson == null))
            return null;

        Iterator<String> keys = customerProfileJson.keys ();
        while (keys.hasNext ()) {
            String nextKey;

            nextKey = keys.next ();
            if (nextKey.equals (segment) == true) {
                try {
                    customerProfileName = nextKey; // eg "Milwaukee"
                    break;
                } catch (Exception e) {
                    MessageLogger.logError ("No RBS segment with name: " + segment);
                }
            }
        }

        /*** pixelLogBuilder.setCustomerProfile (<profileName>)
        if (customerProfileName != null)
            keyValuePair = "customer_profile" + "=" + customerProfileName;

        return (keyValuePair);  // could be null if segment not in config.json
        **/

        return (customerProfileName);
    }

    private JSONObject parseConfig (File configFile) throws Exception {
        BufferedReader reader;
        String srcLine;
        JSONObject configJson;
        StringBuffer configBuf;

        reader = new BufferedReader (new FileReader (configFile));
        configBuf = new StringBuffer ();
        while ((srcLine = reader.readLine ()) != null) {
            configBuf.append (srcLine);
        }

        configJson = new JSONObject (configBuf.toString ());
        return (configJson);
    }
}
