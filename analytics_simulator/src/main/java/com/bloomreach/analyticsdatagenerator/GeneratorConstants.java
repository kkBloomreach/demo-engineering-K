package com.bloomreach.analyticsdatagenerator;

public class GeneratorConstants {

    // input directories expected to be in the -d command line parameter
    // eg, -d<dir>/source/generator/<accountName>.json
    public final static String INPUT_DIR_PATH = "./source/generator/config";

    // acountName provided via commandLine
    public final static String OUTPUT_DIR_PATH = "./output/$ACCOUNTNAME";

    // individual filenames - match the corresponding ones in Simulator config .json
    public final static String FILENAME_UID_MAP_OUTPUT = "UidMap.tsv";
    public final static String FILENAME_SEARCH_QUERY_TO_PID_MAP_OUTPUT = "SearchQueryToPidMap.tsv";
    public final static String FILENAME_CATEGORY_QUERY_TO_PID_MAP_OUTPUT = "CategoryQueryToPidMap.tsv";
    public final static String FILENAME_REFURL_POOL_MAP_OUTPUT = "RefUrlPoolMap.tsv";
    public final static String FILENAME_ZERO_RESULT_QUERY_MAP_OUTPUT = "ZeroResultQueryMap.tsv";
    public final static String FILENAME_REFINED_QUERY_MAP_OUTPUT = "RefinedJourneyMap.tsv";
    public final static String FILENAME_MESSAGELOGS_OUTPUT = "GeneratorMessages.log";

    // internally used constants to generate output.tsv's
    public final static int MAX_PRODUCT_REFURLS = 20;
    public final static int MAX_CATEGORY_REFURLS = 20;

    public final static int MAX_SEARCH_QUERY_COUNT = 10;
    public final static int MAX_REFINEMENTS_PER_QUERY = 5;
    public final static int MIN_REQUIRED_NUM_FOUND = 5; // minimum Pids needed in response for any query (or catId)

    public final static int MAX_CATEGORY_QUERY_COUNT = 10;
    public final static int MAX_REFINEMENTS_PER_CATEGORY = 5;
    // public final static int MIN_REQUIRED_NUM_FOUND = 5;

}
