package com.bloomreach.trafficgenerator.site.journeydata.templates;

// load api templates into ApiBRData object
import java.util.Hashtable;
import java.util.Enumeration;

import com.bloomreach.trafficgenerator.GeneratorConstants;

public class ApiTemplates {

    private ApiBRDataTemplate apiTemplate;

    public ApiTemplates () {
        apiTemplate = new ApiBRDataTemplate ();
    }

    // deviceType may be different for different users
    public ApiBRData loadApiTemplate (int deviceType) throws Exception {
        ApiBRData brData;
        String userAgent;

        brData = this.apiTemplate.copy ();

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

        brData.setParam (ApiBRData.HEADER_USER_AGENT, userAgent); // adjust userAgent as per this user's deviceType
        return (brData);
    }

    class ApiBRDataTemplate {
        private Hashtable <String, String> templateKV;

        ApiBRDataTemplate () {
            templateKV = new Hashtable <String, String> ();
            templateKV.put ("__COMMENT1",    "attribute ignored if its name starts with __");
            templateKV.put ("__COMMENT2",    "attribute ignored if its value starts with ?");
            templateKV.put ("account_id",    "?6413");
            templateKV.put ("_br_uid_2",     "?uid=4416934103635:v=12.0:ts=1555946330315:hc=16789");
            templateKV.put ("ref_url",       "?https://pacifichome.bloomreach.com");
            templateKV.put ("url",           "?https://pacifichome.bloomreach.com");
            templateKV.put ("domain",        "?bloomreach.com");
            templateKV.put ("user_id",       "?4416934103635");
            templateKV.put (ApiBRData.HEADER_USER_AGENT,    "?Adjusted_per_user");
        }

        ApiBRData copy () {
            ApiBRData apiData;
            Enumeration <String> keys;

            apiData = new ApiBRData ();
            keys = templateKV.keys ();
            while (keys.hasMoreElements ()) {
                String value;
                String key;

                key = (String) keys.nextElement ();
                value = (String) templateKV.get (key);
                apiData.setParam (key, value);
            }
            return apiData;
        }
    }
}

/*****
    // These template files are expected to be in the 'templateDir"
    // Since the APILog syntax is complex, we have converted one log file to JSON
    // format manually and then use JSON to parse it.
    private final static String FILENAME_API_TEMPLATE_JSON = "template_api.json";

    private String templateDirPath = null;

    // directory containing all the template files
    public void setTemplatesDir (String templateDirPath) {
        this.templateDirPath = templateDirPath;
    }

    private ApiBRData loadApiTemplateInternal (String templateDirPath, String templatePath ) throws Exception {
        File templateFile;
        JSONObject templateJson;
        ApiBRData templateBRData = null;

        templateFile = new File (templateDirPath, templatePath);

        try {
            templateJson = parseApiJsonTemplate (templateFile);
        } catch (Exception e) {
            MessageLogger.logError ("Exception in parse api template: " + e.getMessage());
            return (null);
        } 

        // Use the JSONobject values to populate brData and return that object
        try {
            templateBRData = prepareApiBRData (templateJson);
        } catch (Exception e) {
            MessageLogger.logError ("Exception in prepare template BRData: " + e.getMessage());
            return (null);
        }

        return (templateBRData);
    }

    private JSONObject parseApiJsonTemplate (File templateFile) throws Exception {
        InputStream templateInputStream;
        BufferedReader templateReader;
        StringBuffer templateBuffer;
        String templateLine;
        JSONObject templateJson;

        templateInputStream = new FileInputStream (templateFile);
        templateReader = new BufferedReader (new InputStreamReader (templateInputStream));
        templateBuffer = new StringBuffer ();
        while ((templateLine = templateReader.readLine ()) != null) {
            templateBuffer.append (templateLine);
        }    
        templateInputStream.close ();

        templateJson = new JSONObject (templateBuffer.toString ());
        return (templateJson); 
    }

    // Use the JSONobject prepared by the parser and generate a ApiBRData object
    private ApiBRData prepareApiBRData (JSONObject templateJson) throws Exception {
        Iterator<String> templateKeys;
        ApiBRData brData;

        brData = new ApiBRData ();

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

    // Use the JSONobject prepared by the parser and generate a ApiBRData object
    private ApiBRData prepareApiBRData (JSONObject templateJson) throws Exception {
        Iterator<String> templateKeys;
        ApiBRData brData;

        brData = new ApiBRData ();

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
****/
