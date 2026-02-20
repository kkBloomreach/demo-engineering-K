// abstract base class for all cloneApi classes
package com.bloomreach.brxdemos.pacificsupply.translate.api;

import java.util.Hashtable;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.proto.MobileApi.ExParam;
import com.bloomreach.proto.MobileApi.ApiLog;
import com.bloomreach.proto.MobileApi.ApiLog.Builder;
import com.bloomreach.proto.MobileApi.CommonRequest;
import com.bloomreach.proto.MobileApi.ApiRequest;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonObject;
import javax.json.JsonException;
import javax.json.stream.JsonParsingException;
import javax.json.JsonObjectBuilder;

public abstract class CloneApiLogBase {

    protected CloneApiLogBase () {
    }

    // common fields to be cloned in ALL Apis
    protected int cloneCommonFields (ApiLog.Builder apiLogBuilder, ProcessedFeed processedFeed, Hashtable<String, String> uidToViewIdMap) {
        ApiRequest.Builder apiRequestBuilder;
        CommonRequest.Builder commonRequestBuilder;
        String requestDataJson;

        apiRequestBuilder = apiLogBuilder.getRequestBuilder ();
        commonRequestBuilder = apiRequestBuilder.getCommonBuilder ();

        // acct id
        commonRequestBuilder.setAccountId (CloneApiConstants.PACIFICSUPPLY_ACCOUNT_ID_INT);

        // auth key 
        commonRequestBuilder.setAuthKey (CloneApiConstants.PACIFICSUPPLY_AUTH_KEY);

        // request_data_json. It is empty in case of SUGGEST api calls
        requestDataJson = commonRequestBuilder.getRequestDataJson ();
        if (StringUtils.isNotEmpty (requestDataJson) == true) {
            String clonedRequestDataJson;

            clonedRequestDataJson = cloneRequestDataJson (requestDataJson, uidToViewIdMap);
            if (clonedRequestDataJson != null)
                commonRequestBuilder.setRequestDataJson (clonedRequestDataJson);
            else
                commonRequestBuilder.setRequestDataJson ("");   // some cloning error...
        }

        return (CloneApiConstants.CLONE_STATUS_OK);
    }


    private String cloneRequestDataJson (String origJsonString, Hashtable<String, String> uidToViewIdMap) {
        JsonReader requestDataReader;
        JsonObject requestDataObject;

        // System.out.println ("request data json: " + origJsonString);
        requestDataReader = Json.createReader (new StringReader (origJsonString));
        requestDataObject = null; // init

        try {
            requestDataObject = requestDataReader.readObject ();
        } catch (JsonParsingException jse) {
            System.out.println ("Cannot parse request data json. Skipped...");
        } catch (JsonException je) {
            System.out.println ("Bad request data json. Skipped...");
        }

        if (requestDataObject != null) {
            String viewId;
            String br_uid_2;
            String domainKey;
            JsonObject cloneRequestDataObject;
            JsonObjectBuilder cloneRequestDataObjectBuilder;
            String cloneRequestDataString;
            StringWriter cloneRequestDataStringWriter;
            JsonWriter cloneRequestDataJsonWriter;

            // domainKey = requestDataObject.getString ("domain_key");
            // System.out.println ("json string domain_key = " + domainKey);
            cloneRequestDataObjectBuilder = Json.createObjectBuilder();

            br_uid_2 = null;
            viewId = null;

            for (String key : requestDataObject.keySet ()) {
                if (key.equals ("domain_key") == true) {
                    cloneRequestDataObjectBuilder.add ("domain_key",  CloneApiConstants.MERCHANT_DOMAIN_KEY);
                } else if (key.equals ("url") == true) {
                    cloneRequestDataObjectBuilder.add ("url",  CloneApiConstants.HOMEPAGE_URL);
                } else if (key.equals ("account_id") == true) {
                    cloneRequestDataObjectBuilder.add ("account_id",  CloneApiConstants.PACIFICSUPPLY_ACCOUNT_ID);
                } else if (key.equals ("auth_key") == true) {
                    cloneRequestDataObjectBuilder.add ("auth_key",  CloneApiConstants.PACIFICSUPPLY_AUTH_KEY);
                } else if (key.equals ("ref") == true) {
                    cloneRequestDataObjectBuilder.add ("ref",  CloneApiConstants.HOMEPAGE_URL);
                } else if (key.equals ("ref_url") == true) {
                    cloneRequestDataObjectBuilder.add ("ref_url",  CloneApiConstants.HOMEPAGE_URL);
                } else {
                    cloneRequestDataObjectBuilder.add (key, requestDataObject.getString (key));
                    // besides storing back the br_uid_2 and view_id values, update the
                    // uid-to-viewId hashtable
                    if (key.equals ("_br_uid_2") == true) {
                       br_uid_2 = requestDataObject.getString (key);
                    } else if (key.equals ("view_id") == true) {
                       viewId = requestDataObject.getString (key);
                    }
                }
            }
            requestDataReader.close ();

            // if we have valid br_uid_2 and view_id values in this pixelLog,
            // set those in the uid-to-viewId hashtable
            if (StringUtils.isNotEmpty (br_uid_2) && StringUtils.isNotEmpty (viewId)) {
                updateUidToViewIdMap (br_uid_2, viewId, uidToViewIdMap);
            }

            // generate Json string from the newly built JsonObject
            cloneRequestDataStringWriter = new StringWriter ();
            cloneRequestDataObject = cloneRequestDataObjectBuilder.build ();
            cloneRequestDataJsonWriter = Json.createWriter (cloneRequestDataStringWriter);
            cloneRequestDataJsonWriter.write (cloneRequestDataObject);
            cloneRequestDataJsonWriter.close ();
            cloneRequestDataString = cloneRequestDataStringWriter.toString ();

            // System.out.println ("out request_data_json = " + cloneRequestDataString);
            return (cloneRequestDataString);
        }

        return (null); 
    }

    // generate cloned productId in exactly the same way it is done when feed is preProcessed
    protected String generateUniqPid  (String srcPid) {

        int pidIntValue;
        int generatedPid;

        try {
            pidIntValue = Integer.parseInt (srcPid);
            generatedPid = pidIntValue + CloneApiConstants.FIXED_OFFSET_FOR_PID;
            return (Integer.toString (generatedPid));
        } catch (NumberFormatException nfe) {
            System.out.println ("Bad pid: " + srcPid);
        }
    
        return (null);
    }

    // br_uid_2 syntax: "uid=2302684623707:v=12.0:ts=1589490345554:hc=308"
    private void updateUidToViewIdMap (String br_uid_2, String viewId, Hashtable<String, String> uidToViewIdMap) {
        int uidIndx;
        int colonIndx;
        String uidString;
        String selectedViewId;

        uidIndx = br_uid_2.indexOf ("uid=");
        colonIndx = br_uid_2.indexOf (":");
        uidString = br_uid_2.substring (uidIndx + "uid=".length(), colonIndx);

        // viewId can be a comma-separated list and some of the values
        // numeric. Split such values, ignore all numerical values and
        // if any value is remaining, select that
        selectedViewId = null;
        viewId = viewId.trim ();
        if (viewId.indexOf (',') >= 0) {
            String viewIdList[];
            viewIdList = viewId.split (",");
            for (int i = 0; i < viewIdList.length; i++) {
                String oneId;

                oneId = viewIdList [i].trim ();
                if (StringUtils.isEmpty (oneId))
                    continue;
                else if (StringUtils.isNumeric (oneId))
                    continue;
                else {
                    selectedViewId = oneId;
                    break;
                }
            }
        }
        else
            selectedViewId = viewId;

        if (uidToViewIdMap.get (uidString) == null)
            uidToViewIdMap.put (uidString, selectedViewId);
    }

}
