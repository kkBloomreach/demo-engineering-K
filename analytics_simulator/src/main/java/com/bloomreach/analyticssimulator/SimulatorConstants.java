package com.bloomreach.analyticssimulator;

// various constants used for Simulator. These are independent of specific acct
public class SimulatorConstants {

    // directories expected to be in the '-d' command line parameter 
    // eg, ./data/source/templates
    public final static String PIXEL_TEMPLATE_DIR_PATH = "./source/simulator/templates";
    public final static String API_TEMPLATE_DIR_PATH = "./source/simulator/templates";
    public final static String ACCOUNT_CONFIG_DIR_PATH = "./source/simulator/config";

    public final static String OUTPUT_PIXELLOG_DIR = "./output/$ACCOUNTNAME/pixelLogs/";
    public final static String OUTPUT_APILOG_DIR = "./output/$ACCOUNTNAME/apiLogs/";
    public final static String OUTPUT_SIMULATION_STATS_DIR = "./output/$ACCOUNTNAME";
    public final static String OUTPUT_SIMULATION_STATS_FILENAME = "simulationStats.tsv";
    public final static String OUTPUT_SIMULATION_LOG_DIR = "./output/$ACCOUNTNAME";
    public final static String OUTPUT_SIMULATORLOG_FILENAME = "simulatorMessages.log";

    public final static int SIMULATE_STATUS_OK     = 101;
    public final static int SIMULATE_STATUS_ERROR  = 102;
    public final static int SIMULATE_STATUS_REJECT = 103;

    // for accts that don't use RTCS segment, simulation data uses this predefined
    // segment name
    public final static String SEGMENTATION_TYPE_RTCS = "RTCS";
    public final static String SEGMENTATION_TYPE_RBS = "RBS";
    public final static String SEGMENTATION_TYPE_NONE = "NONE";

}

