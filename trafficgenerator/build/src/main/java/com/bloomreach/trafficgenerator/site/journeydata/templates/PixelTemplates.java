package com.bloomreach.trafficgenerator.site.journeydata.templates;

// load pixel templates into PixelBRData object
import java.util.Hashtable;
import java.util.Enumeration;

import com.bloomreach.trafficgenerator.GeneratorConstants;

public class PixelTemplates {

    private PixelBRDataTemplate pixelTemplate;

    public PixelTemplates () {
        pixelTemplate = new PixelBRDataTemplate ();
    }

    // deviceType may be different for different users
    public PixelBRData loadPixelTemplate (int deviceType) throws Exception {
        PixelBRData brData;
        String userAgent;

        brData = this.pixelTemplate.copy ();

        // based on deviceType, initialize userAgent to be used in api call
        // based on visitor's deviceType, set the user-agent. It must be
        // same in pixel and api calls for consistency
        switch (deviceType) {
            case GeneratorConstants.DEVICE_TYPE_MOBILE:
                userAgent = GeneratorConstants.USER_AGENT_MOBILE;
                break;
            case GeneratorConstants.DEVICE_TYPE_TABLET:
                userAgent = GeneratorConstants.USER_AGENT_TABLET;
                break;
            case GeneratorConstants.DEVICE_TYPE_DESKTOP:
                userAgent = GeneratorConstants.USER_AGENT_DESKTOP;
                break;
            case GeneratorConstants.DEVICE_TYPE_OTHER:
            default:
                userAgent = GeneratorConstants.USER_AGENT_OTHER;
                break;
        }

        brData.setParam (PixelBRData.HEADER_USER_AGENT, userAgent); // adjust userAgent as per this user's deviceType
        return (brData);
    }

    class PixelBRDataTemplate {
        private Hashtable <String, String> templateKV;

        PixelBRDataTemplate () {
            templateKV = new Hashtable <String, String> ();
            templateKV.put ("__COMMENT1",      "attribute ignored if its name starts with __");
            templateKV.put ("__COMMENT2",      "attribute ignored if its value starts with ?");
            templateKV.put ("acct_id",         "?6413");
            templateKV.put ("cookie2",         "?uid=4416934103635:v=12.0:ts=1555946330315:hc=16789");
            templateKV.put ("ref",             "?https://pacifichome.bloomreach.com");
            templateKV.put ("url",             "?https://pacifichome.bloomreach.com");
            templateKV.put ("is_conversion",   "0");
            templateKV.put ("ptype",           "?event");
            templateKV.put ("sid",             "?undefined");
            templateKV.put ("domain",          "?bloomreach.com");
            templateKV.put ("version",         "17.0");
            templateKV.put ("prod_id",         "?4826567");
            templateKV.put ("uid",             "?4416934103635");
            templateKV.put ("test_data",       "?true");
            templateKV.put (PixelBRData.HEADER_USER_AGENT,    "?Adjusted_per_user");
        }

        PixelBRData copy () {
            PixelBRData pixelData;
            Enumeration <String> keys;

            pixelData = new PixelBRData ();
            keys = templateKV.keys ();
            while (keys.hasMoreElements ()) {
                String value;
                String key;

                key = (String) keys.nextElement ();
                value = (String) templateKV.get (key);
                pixelData.setParam (key, value);
            }
            return pixelData;
        }
    }
}

/********
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.json.JSONObject;
import org.json.JSONArray;

//     private String templateDirPath = null;
//     // directory containing all the template files
//     public void setTemplatesDir (String templateDirPath) {
//         this.templateDirPath = templateDirPath;
//     }
// 
//     public PixelBRData loadPixelTemplate_OLD () throws Exception {
//         PixelBRData brData;
// 
//         brData = loadPixelTemplateInternal (templateDirPath, GeneratorConstants.INPUT_PIXEL_TEMPLATES_FILE_NAME);
//         return (brData);
//     }
// 
//     private PixelBRData loadPixelTemplateInternal (String templateDirPath, String templatePath ) throws Exception {
//         File templateFile;
//         JSONObject templateJson;
//         PixelBRData templateBRData = null;
// 
//         templateFile = new File (templateDirPath, templatePath);
// 
//         try {
//             templateJson = parsePixelJsonTemplate (templateFile);
//         } catch (Exception e) {
//             MessageLogger.logError ("Exception in parse pixel template: " + e.getMessage());
//             return (null);
//         } 
// 
//         // Use the JSONobject values to populate brData and return that object
//         try {
//             templateBRData = preparePixelBRData (templateJson);
//         } catch (Exception e) {
//             MessageLogger.logError ("Exception in prepare template BRData: " + e.getMessage());
//             return (null);
//         }
// 
//         return (templateBRData);
//     }
// 
//     private JSONObject parsePixelJsonTemplate (File templateFile) throws Exception {
//         InputStream templateInputStream;
//         BufferedReader templateReader;
//         StringBuffer templateBuffer;
//         String templateLine;
//         JSONObject templateJson;
// 
//         templateInputStream = new FileInputStream (templateFile);
//         templateReader = new BufferedReader (new InputStreamReader (templateInputStream));
//         templateBuffer = new StringBuffer ();
//         while ((templateLine = templateReader.readLine ()) != null) {
//             templateBuffer.append (templateLine);
//         }    
//         templateInputStream.close ();
// 
//         templateJson = new JSONObject (templateBuffer.toString ());
//         return (templateJson); 
//     }
    // Use the JSONobject prepared by the parser and generate a PixelBRData object
    private PixelBRData preparePixelBRData (JSONObject templateJson) throws Exception {
        Iterator<String> templateKeys;
        PixelBRData brData;

        brData = new PixelBRData ();

        templateKeys = templateJson.keys ();
        while (templateKeys.hasNext()) {
            String value;
            String key;

            key = templateKeys.next ();
            // MessageLogger.logDebug ("template key: " + key);
            value = (String) templateJson.get (key);
            brData.setParam (key, value);
        }

        return brData;
    }
*******/

