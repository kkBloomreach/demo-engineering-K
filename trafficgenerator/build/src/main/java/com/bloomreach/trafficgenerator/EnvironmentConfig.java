package com.bloomreach.trafficgenerator;

// various constants used for deployment. Deployment can be 'dev'/'qa'/'release'
public class EnvironmentConfig {

    // must be same as those specified in command line
    public final static String ENV_TYPE_DEV = "dev";
    public final static String ENV_TYPE_QA  = "qa";
    public final static String ENV_TYPE_RELEASE  = "release";

    private static String envType  = null;
    private static EnvParams envParams;

    public EnvironmentConfig () {
    }

    public void setEnvType (String environmentType) throws Exception {
        switch (environmentType) {
            case ENV_TYPE_DEV: 
                    envParams = new DevEnvParams ();
                    break;
            case ENV_TYPE_QA: 
                    envParams = new QAEnvParams ();
                    break;
            case ENV_TYPE_RELEASE:
                    envParams = new ReleaseEnvParams ();
                    break;
            default:
                MessageLogger.logError ("Unknown environment type: " + envType);
                throw new Exception ("Unknown environment type: " + envType);
        }
        envType = environmentType;
    }

    public static String getEnvType () {
        return envType;
    }

    public static long getEnvParamLong (String paramName) {
        return (envParams.getEnvParamLong (paramName));
    }


    abstract class EnvParams {
        abstract long getEnvParamLong (String paramName);
    }

    class ReleaseEnvParams extends EnvParams {
        private long TOTAL_GENERATOR_DURATION = 23 * 60 * 60 * 1000;  // 23 hrs
        private long TRAFFIC_SLICE_DURATION   = 1 * 60 * 60 * 1000; // N hrs 
        // Generator app exit delay after "total duration"
        private long DELAY_BEFORE_GENERATOR_EXIT = 5 * 60 * 1000; 
        // in order to manage memory usage, userLogs are written out 
        // after every N userLogs
        private long MAX_USER_LOGS_IN_MEMORY = 50;

        ReleaseEnvParams () {
        }

        long getEnvParamLong (String paramName) {
            if (paramName.equals ("TOTAL_GENERATOR_DURATION"))
                return this.TOTAL_GENERATOR_DURATION;
            else if (paramName.equals ("TRAFFIC_SLICE_DURATION"))
                return this.TRAFFIC_SLICE_DURATION;
            else if (paramName.equals ("DELAY_BEFORE_GENERATOR_EXIT"))
                return this.DELAY_BEFORE_GENERATOR_EXIT;
            else if (paramName.equals ("MAX_USER_LOGS_IN_MEMORY"))
                return this.MAX_USER_LOGS_IN_MEMORY;

            MessageLogger.logError ("Unknow environment parameter: " + paramName); 
            return -1;
        }
    }

    class DevEnvParams extends EnvParams {
        private long TOTAL_GENERATOR_DURATION = 5 * 60 * 1000;  // N minutes
        private long TRAFFIC_SLICE_DURATION   = 1 * 60 * 1000; // N minutes
        // Generator app exit delay after "total duration"
        private long DELAY_BEFORE_GENERATOR_EXIT = 1 * 60 * 1000; 
        // in order to manage memory usage, userLogs are written out 
        // after every N userLogs
        private long MAX_USER_LOGS_IN_MEMORY = 1;


        DevEnvParams () {
        }

        long getEnvParamLong (String paramName) {
            if (paramName.equals ("TOTAL_GENERATOR_DURATION"))
                return this.TOTAL_GENERATOR_DURATION;
            else if (paramName.equals ("TRAFFIC_SLICE_DURATION"))
                return this.TRAFFIC_SLICE_DURATION;
            else if (paramName.equals ("DELAY_BEFORE_GENERATOR_EXIT"))
                return this.DELAY_BEFORE_GENERATOR_EXIT;
            else if (paramName.equals ("MAX_USER_LOGS_IN_MEMORY"))
                return this.MAX_USER_LOGS_IN_MEMORY;

            MessageLogger.logError ("Unknow environment parameter: " + paramName); 
            return -1;
        }
    }

    class QAEnvParams extends EnvParams {
        private long TOTAL_GENERATOR_DURATION = 5 * 60 * 60 * 1000;  // N hrs
        private long TRAFFIC_SLICE_DURATION   = 1 * 60 * 60 * 1000; // N hrs
        // Generator app exit delay after "total duration"
        private long DELAY_BEFORE_GENERATOR_EXIT = 2 * 60 * 1000; 
        // in order to manage memory usage, userLogs are written out 
        // after every N userLogs
        private long MAX_USER_LOGS_IN_MEMORY = 5;

        QAEnvParams () {
        }

        long getEnvParamLong (String paramName) {
            if (paramName.equals ("TOTAL_GENERATOR_DURATION"))
                return this.TOTAL_GENERATOR_DURATION;
            else if (paramName.equals ("TRAFFIC_SLICE_DURATION"))
                return this.TRAFFIC_SLICE_DURATION;
            else if (paramName.equals ("DELAY_BEFORE_GENERATOR_EXIT"))
                return this.DELAY_BEFORE_GENERATOR_EXIT;
            else if (paramName.equals ("MAX_USER_LOGS_IN_MEMORY"))
                return this.MAX_USER_LOGS_IN_MEMORY;

            MessageLogger.logError ("Unknow environment parameter: " + paramName); 
            return -1;
        }
    }
}

