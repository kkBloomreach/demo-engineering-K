package com.bloomreach.trafficgenerator;

// various constants used for Generator. These are independent of specific acct
public class GeneratorConstants {

    // directories expected to be in the <acctDir> = -d <root>/accountName 
    public final static String INPUT_SITE_CONFIG_PATH = "./input/config/site.json";

    // feed to be processed for trafficGeneration. Feed modified daily
    // path relative to acctDir. Daily feed is 'possibly' altered version of original feed
    public final static String INPUT_ORIGINAL_FEED_DIR = "./input/feed/original";
    public final static String INPUT_DAILY_FEED_DIR = "./input/feed/daily";
    public final static String INPUT_DAILY_JSONL_FEED_FILE_NAME= "feed.jsonl";
    public final static String DATACONNECT_ACCESS_KEY_UNKNOWN = "UNKNOWN";

    // search_term_path used in DataGenerator which in turn generates
    // search_term_with_refinements. The latter is then used in TrafficGenerator
    // to trigger analytics data
    public final static String INPUT_SEARCH_TERM_PATH= "./input/journeydata/SearchTerms.tsv";
    public final static String INPUT_SEARCH_TERM_WITH_REFINEMENTS_PATH= "./input/journeydata/SearchTermsWithRefinements.tsv";
    public final static String INPUT_SUGGEST_TERM_PATH= "./input/journeydata/SuggestTerms.tsv";
    public final static String INPUT_ZERO_RESULT_SEARCH_TERM_PATH= "./input/journeydata/ZeroResultSearchTerms.tsv";
    public final static String INPUT_CAMPAIGNS_CONFIG_PATH= "./input/journeydata/campaigns/Campaigns.json";
    public final static String INPUT_TRAFFICSTEPS_PATH= "./input/journeydata/trafficsteps.tsv";
    public final static String INPUT_WIDGET_CONFIGS_PATH= "./input/journeydata/Widgets.json";
    public final static String INPUT_CUSTOM_JOURNEY_CONFIGS_PATH = "./input/journeydata/customjourney/CustomJourney.json";

    // account specific outputs, created in <acctDir>
    public final static String OUTPUT_DAILYLOG_DIR = "./output";
    public final static String OUTPUT_DAILYLOG_FILENAME_PREAMBLE = "generatorLog";
    public final static String OUTPUT_LOG_DIR = "./output";
    public final static String OUTPUT_LOG_FILENAME = "generatorMessages.log"; // rotated via log4j
    public final static String OUTPUT_WIDGETLOG_DIR = "./output";
    public final static String OUTPUT_WIDGETLOG_FILENAME_PREAMBLE = "generatorWidgetLog";
    public final static String OUTPUT_PIXELCOUNTLOG_DIR = "./output";
    public final static String OUTPUT_PIXELCOUNTLOG_FILENAME_PREAMBEL = "generatorPixelCountLog";
    public final static String OUTPUT_APICOUNTLOG_DIR = "./output";
    public final static String OUTPUT_APICOUNTLOG_FILENAME_PREAMBEL = "generatorApiCountLog";

    public final static int GENERATE_STATUS_OK     = 101;
    public final static int GENERATE_STATUS_ERROR  = 102;
    public final static int GENERATE_STATUS_REJECT = 103;

    // Generator API endpoints
    public final static String REGION_US = "US";
    public final static String REGION_EU = "EU";
    public final static String REALM_STAGING = "staging";
    public final static String REALM_PROD = "production"; // "prod" changed to "production" in V3

    public final static String PIXEL_API_ENDPOINT_US = "https://p.brsrvr.com/pix.gif";
    public final static String PIXEL_API_ENDPOINT_EU = "https://p-eu.brsrvr.com/pix.gif";
    // debugger api endpoints are now same as live. To debug events, include &debug=true in API call
    public final static String PIXEL_DEBUGGER_API_ENDPOINT_US = "https://p.brsrvr.com/pix.gif";
    public final static String PIXEL_DEBUGGER_API_ENDPOINT_EU = "https://p-eu.brsrvr.com/pix.gif";
    public final static String DISCOVERY_SEARCH_API_ENDPOINT_STAGING = "https://staging-core.dxpapi.com/api/v1/core/"; // end slash reqd
    public final static String DISCOVERY_SEARCH_API_ENDPOINT_PROD = "https://core.dxpapi.com/api/v1/core/";    // end slash reqd
    public final static String DISCOVERY_SUGGEST_API_ENDPOINT_STAGING = "https://staging-suggest.dxpapi.com/api/v2/suggest/"; // end slash reqd
    public final static String DISCOVERY_SUGGEST_API_ENDPOINT_PROD = "https://suggest.dxpapi.com/api/v2/suggest/";    // end slash reqd
    public final static String DISCOVERY_WIDGET_API_ENDPOINT_PROD = "https://pathways.dxpapi.com/api/v2/widgets/";    // end slash reqd
    public final static String DISCOVERY_WIDGET_API_ENDPOINT_STAGING = "https://pathways-staging.dxpapi.com/api/v2/widgets/"; // end slash reqd

    // feed index
    // public final static String DATACONNECT_API_ENDPOINT_STAGING = "https://api-staging.connect.bloomreach.com/dataconnect/api/v1";
    // public final static String DATACONNECT_API_ENDPOINT_PROD = "https://api.connect.bloomreach.com/dataconnect/api/v1";
    public final static String DATACONNECT_API_ENDPOINT_V3 = "https://discovery.bloomreach.com/dataconnect/api/v3";

    // traffic states/types/...
    public final static int TRAFFIC_STATE_LOW = 1;
    public final static int TRAFFIC_STATE_MEDIUM = 2;
    public final static int TRAFFIC_STATE_HIGH = 3;

    public final static int MAX_VISITOR_POOL_SIZE = 10000000; // visitor pool size
    // following numbers calculated based on the MAX_POOL_SIZE
    public final static int MAX_VISITORS_LOW_TRAFFIC_PER_TIMESLICE    = (int) (MAX_VISITOR_POOL_SIZE * 0.0001);
    public final static int MAX_VISITORS_MEDIUM_TRAFFIC_PER_TIMESLICE = (int) (MAX_VISITOR_POOL_SIZE * 0.0010);
    public final static int MAX_VISITORS_HIGH_TRAFFIC_PER_TIMESLICE   = (int) (MAX_VISITOR_POOL_SIZE * 0.0100);

    // MAX concurrent visitors allowed in the site. This is a heuristic number - to be adjusted upon experimentation
    // based on 'high-traffic-per-timeslice' value above. This 'max' affects VM resource availability. Too many 
    // concurrent users -> too many concurrent threads (collectively among all sites), which can cause the VM to crash
    public final static int MAX_CONCURRENT_SITE_VISITORS_ALLOWED = (int) (MAX_VISITORS_HIGH_TRAFFIC_PER_TIMESLICE * 0.25);

    // SOME 13 digits === 10*12; value must be numeric
    public final static String VISITOR_ID_TEMPLATE = "5600000000000";  // prev = 5678123411118

    // single visitor may execute multiple sessions in a single visit
    public final static int MAX_SESSIONS_PER_VISITOR = 5;

    // in Predefined journey, max ATC percentage (ie, % of sessions where visitor actually does an ATC)
    // After doing ATC, % of those which go to actual conversion
    public final static int MAX_ATC_PERCENT = 30;
    public final static int MAX_CONVERSION_PERCENT = 20;

    // for predefined journey, time-delay between subsequent sessions for a given visitor
    // Delay between steps (within a session) are defined below ("STEP_DURATION...")
    public final static long MEAN_TIME_BETWEEN_PREDEFINED_SESSIONS = 10 * 1000; // N seconds in millis

    // different session types currently supported
    // Note about RTCS: ALL pixels have cdp_segment info. All queries are 'segmented' queries. 
    // Note about 'zero' query: We generate 1% of total traffic to be 'zero result' query 
    // All other sessions are evenly distributed in the remaining 99% of total queries 
    public final static int UNDEFINED_SESSION_TYPE = -1;
    public final static int TERM_SEARCH_SESSION = 0;
    public final static int CATEGORY_SEARCH_SESSION = 1; 
    public final static int TERM_SEARCH_WITH_TERM_REFINEMENT_SESSION  = 2; 
    public final static int TERM_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION  = 3;
    public final static int CATEGORY_SEARCH_WITH_TERM_REFINEMENT_SESSION  = 4;
    public final static int CATEGORY_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION  = 5;
    public final static int ZERO_RESULT_QUERY_SESSION = 6;
    public final static int ZERO_RESULT_QUERY_WITH_TERM_REFINEMENT_SESSION = 7;
    public final static int ZERO_RESULT_QUERY_WITH_CATEGORY_REFINEMENT_SESSION = 8;
    public final static int SUGGEST_SESSION_SELECT_NONE = 9;
    public final static int SUGGEST_SESSION_SELECT_TERM = 10;
    public final static int SUGGEST_SESSION_SELECT_CATEGORY = 11;
    public final static int SUGGEST_SESSION_SELECT_PRODUCT = 12;
    public final static int TERM_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION = 13;  // 'partial' search
    public final static int TERM_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION = 14;
    public final static int CATEGORY_PARTIAL_SEARCH_WITH_TERM_REFINEMENT_SESSION = 15;
    public final static int CATEGORY_PARTIAL_SEARCH_WITH_CATEGORY_REFINEMENT_SESSION = 16;

    public final static int MAX_SESSION_TYPES = 17;


    // following constants must match the ones used in steps.tsv
    public final static int TRAFFIC_STEPID_BROWSE_PDP = 0;
    public final static int TRAFFIC_STEPID_VIEW_LIST = 1;
    public final static int TRAFFIC_STEPID_SEARCH_TERM = 2;
    public final static int TRAFFIC_STEPID_SEARCH_CAT = 3;
    public final static int TRAFFIC_STEPID_SUG_QUERY = 4;
    public final static int TRAFFIC_STEPID_SELECT_SUG_NONE = 5;
    public final static int TRAFFIC_STEPID_SELECT_SUG_TERM = 6;
    public final static int TRAFFIC_STEPID_SELECT_SUG_CAT = 7;
    public final static int TRAFFIC_STEPID_SELECT_SUG_PROD = 8;
    public final static int TRAFFIC_STEPID_ATC = 9;
    public final static int TRAFFIC_STEPID_CONVERT  = 10;
    public final static int TRAFFIC_STEPID_START_URL = 11;
    public final static int TRAFFIC_STEPID_SELECT_PID_FROM_LIST = 12;
    public final static int TRAFFIC_STEPID_INVALID_DATA = 13;
    public final static int TRAFFIC_STEPID_EXIT = 14;
    public final static int TRAFFIC_STEPID_EXCEPTION_RESTART = 99;

    public final static int TRAFFIC_WIDGET_ACTION_API_CALL = 201;
    public final static int TRAFFIC_WIDGET_ACTION_ATC = 202; 

    // step duration for each step - in milliseconds
    // Values adjusted to manage large-pool configuration (otherwise too many threads remain active, causing out-of-memory error)
    public final static int TRAFFIC_STEP_DURATION_BROWSE_PDP =      50 * 1000; // 60 * 1000;
    public final static int TRAFFIC_STEP_DURATION_VIEW_LIST =       50 * 1000; // 200 * 1000;
    public final static int TRAFFIC_STEP_DURATION_SEARCH_TERM =     50 * 1000;   // api response time
    public final static int TRAFFIC_STEP_DURATION_SEARCH_CAT =      50 * 1000;
    public final static int TRAFFIC_STEP_DURATION_SUG_QUERY =       50 * 1000; // 1000;
    public final static int TRAFFIC_STEP_DURATION_SELECT_SUG_NONE = 50 * 1000; // 5 * 1000;
    public final static int TRAFFIC_STEP_DURATION_SELECT_SUG_TERM = 50 * 1000; // 5 * 1000;
    public final static int TRAFFIC_STEP_DURATION_SELECT_SUG_CAT =  50 * 1000; // 5 * 1000;
    public final static int TRAFFIC_STEP_DURATION_SELECT_SUG_PROD = 50 * 1000; // 5 * 1000;
    public final static int TRAFFIC_STEP_DURATION_ATC =             50 * 1000; // 200 * 1000;
    public final static int TRAFFIC_STEP_DURATION_CONVERT  =        50 * 1000; // 300 * 1000;    // enter card-info etc
    public final static int TRAFFIC_STEP_DURATION_START_URL =       50 * 1000; // 30 * 1000;    
    public final static int TRAFFIC_STEP_DURATION_SELECT_PID_FROM_LIST = 50 * 1000; // 200 * 1000;
    public final static int TRAFFIC_STEP_DURATION_INVALID_DATA =    100;
    public final static int TRAFFIC_STEP_DURATION_EXIT =            100;

    public final static int WIDGET_ACTION_DURATION     =           1000; // some placeholder value
    
    // some upper limit to terminate a session
    public final static int MAX_STEP_COUNT_IN_RANDOM_SESSION = 10;

    // campaign search terms can be prepended with '*' to indicate their 'importance' if needed
    // eg, *sofa. For each such star, the term is replicated using this multiplier count
    public final static int SEARCHTERM_STAR_MULTIPLIER = 5;

    // low peroformance category threshold. if a randominly generated value is less
    // than this threshold, that visitor immediately exits that session 
    // (ie, takes no further actions in that session)
    public final static int LOW_PERFORMANCE_CATEGORY_EXIT_THRESHOLD = 60;

    // custom journey 
    public final static String CUSTOM_JOURNEY_TYPE_LPC = "LPC"; // low-performance-category journey

    // visitor device types = mobile / desktop / tablet
    // visitor is 'assigned' one of these at random, with different %'s
    public final static int DEVICE_TYPE_UNKNOWN = -1;
    public final static int DEVICE_TYPE_MOBILE = 1;
    public final static int DEVICE_TYPE_TABLET = 2;
    public final static int DEVICE_TYPE_DESKTOP = 3;
    public final static int DEVICE_TYPE_OTHER = 4;
    public final static int MAX_DEVICE_TYPES = 4;

    // note: apparently Analytics uses a third-party library "UAGen" to detect device type
    // from user-agent-string. Following samples obtained from Internet ("user-agent")
    public final static String USER_AGENT_MOBILE = "Mozilla/5.0 (Linux; U; Android 2.3.3; en-us; HTC_DesireS_S510e Build/GRI40) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

    public final static String USER_AGENT_TABLET = "Mozilla/5.0 (iPad; CPU OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1";

    public final static String USER_AGENT_DESKTOP = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Island";

    public final static String USER_AGENT_OTHER = "Mozilla/5.0 (Other; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    //////////////////////////////////
    // Constants for Data generator. openAI used to generate
    // 'refined search terms' given an initial 'search' term. (eg, "belt" -> generate refined search terms such as 'leather belt', ...
    public final static String OPENAPI_URL = "https://api.openai.com/v1/chat/completions";
    public final static String OPENAPI_KEY = "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";
    public final static String OPENAPI_MODEL = "gpt-4o";
}

/****
    // Special USER_ID (shared with SPA) to implement past-purchase widget support in SPA
    public final static String SPECIAL_VISITOR_ID = "1112223334445";

    // on these days, the "special" visitor UID is generated in slice#0
    public final static int SPECIAL_VISITOR_DAY_1 = 7;
    public final static int SPECIAL_VISITOR_DAY_2 = 24;

    // Some URL used as 'ref' url in start page
    // public final static String DEFAULT_REF_URL_AT_START = "https://www.bloomreach.com";

****/

