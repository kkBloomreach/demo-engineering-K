package com.bloomreach.trafficgenerator.site.journeydata.customjourney;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class CustomJourney {

    private final static String CUSTOM_JOURNEY_TYPE_LOW_PERF_CATEGORY = "LowPerformanceCategory";
    private final static String CUSTOM_JOURNEY_TYPE_PLACEHOLDER = "SomeOtherCustomJourney"; // temp place holder for debugging
    private final static String LPC_KEY_BOUNCE_RATE = "bounce_rate";
    private final static String LPC_KEY_SELECT_SEARCH_TERMS = "select_search_terms";

    private ArrayList<LPCCustomJourneyData> allLPCCustomJourneys;
    private ArrayList<CustomJourneyData> allPlaceholderCustomJourneys; // debugging

    public CustomJourney () {
    }

    public boolean load (String configPath) throws Exception {
        File configFile;

        configFile = new File (configPath);
        if (configFile.exists () == false)
            return false;

        this.allLPCCustomJourneys = new ArrayList <LPCCustomJourneyData> ();
        parseConfig (configFile);
        return true;
    }

    // given journeyType and specific target, returns journeyData (if any) for that selection
    public LPCCustomJourneyData getLPCCustomJourneyData (String catId) {
        if ((this.allLPCCustomJourneys != null) && (this.allLPCCustomJourneys.size() > 0)) {
            for (LPCCustomJourneyData journeyData : this.allLPCCustomJourneys) {
                if (journeyData.getCatId().equals(catId))
                    return journeyData;
            }
        }
        return null;
    }

    //////// INTERNAL METHODS
    private void parseConfig (File configFile) throws Exception {
        BufferedReader reader;
        String srcLine;
        JSONObject configJson;
        StringBuffer configBuf;
        Iterator<String> journeyKeys;

        reader = new BufferedReader (new FileReader (configFile));
        configBuf = new StringBuffer ();
        while ((srcLine = reader.readLine ()) != null) {
            configBuf.append (srcLine);
        }
        reader.close();

        configJson = new JSONObject (configBuf.toString ());
        journeyKeys = configJson.keys ();
        while (journeyKeys.hasNext()) {
            String key;
            
            key = journeyKeys.next ();
            if (key.startsWith ("__") == true)  // skip 'comment' keys
                continue;
            
            switch (key) {
                case CUSTOM_JOURNEY_TYPE_LOW_PERF_CATEGORY: // "LowPerformanceCategory"
                    allLPCCustomJourneys = parseLPCConfig (configJson.getJSONObject (key));
                    break;
                
                case CUSTOM_JOURNEY_TYPE_PLACEHOLDER:
                    CustomJourneyData journeyData;
                    journeyData = parsePlaceholder (configJson.getJSONObject(key));
                    if (journeyData != null) {
                        this.allPlaceholderCustomJourneys.add (journeyData);
                    }
                    break;
                
                default:
                    MessageLogger.logError(String.format ("Unknown custom journey type: %s", key));
            }
        }
    }

    // low-performance-category config
    private ArrayList <LPCCustomJourneyData> parseLPCConfig (JSONObject configJson) throws Exception {
        Iterator<String> configKeys;
        ArrayList <LPCCustomJourneyData> allLPCCustomJourneyData = new ArrayList <LPCCustomJourneyData> ();

        configKeys = configJson.keys ();
        while (configKeys.hasNext ()) { 
            Iterator<String> lpcKeys;
            String configKey;
            String lpcKey;
            JSONObject lpcJourneyDataJsonObj;
            LPCCustomJourneyData lpcCustomJourneyData;
            String catId; // aka journeyTarget
            int bounceRate = GeneratorConstants.LOW_PERFORMANCE_CATEGORY_EXIT_THRESHOLD;
            ArrayList<String> selectSearchTerms = new ArrayList <String> ();    // by default, empty

            configKey = configKeys.next ();
            if (configKey.startsWith ("__") == true)  // skip 'comment' keys
                continue;

            catId = configKey; // eg, "20900"
            lpcJourneyDataJsonObj = configJson.getJSONObject(catId);
            lpcKeys = lpcJourneyDataJsonObj.keys ();
            while (lpcKeys.hasNext()) {
                lpcKey = lpcKeys.next();
                if (lpcKey.startsWith ("__") == true)  // skip 'comment' keys
                    continue;

                switch (lpcKey) {
                    case LPC_KEY_BOUNCE_RATE:
                        bounceRate = lpcJourneyDataJsonObj.getInt(lpcKey);
                        break;

                    case LPC_KEY_SELECT_SEARCH_TERMS:
                        JSONArray selectTermsJsonArray;

                        selectTermsJsonArray = lpcJourneyDataJsonObj.getJSONArray(lpcKey);
                        for (int i = 0; i < selectTermsJsonArray.length(); i++)
                            selectSearchTerms.add (selectTermsJsonArray.getString (i));
                        break;

                    default:
                        MessageLogger.logError (String.format ("Unknown lowPerformance custom journey data: %s", lpcKey));
                }
            }
            lpcCustomJourneyData = new LPCCustomJourneyData(catId, bounceRate, selectSearchTerms);
            allLPCCustomJourneyData.add (lpcCustomJourneyData);
        }
        return allLPCCustomJourneyData;
    }

    private CustomJourneyData parsePlaceholder (JSONObject configObj) {
        return null;
    }
    
}
