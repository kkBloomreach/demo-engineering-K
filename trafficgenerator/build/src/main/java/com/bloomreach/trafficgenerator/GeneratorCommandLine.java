package com.bloomreach.trafficgenerator;

import java.io.File;
import com.bloomreach.trafficgenerator.GeneratorConstants;

public class GeneratorCommandLine {

    private String dataDirPath = null;
    private String accountName = null;
    private String messageLevel = "fatal";  // default
    private String realm = GeneratorConstants.REALM_STAGING;   //default
    private boolean testData = true;  //default
    private String envType = EnvironmentConfig.ENV_TYPE_DEV;    // default
    private boolean pixelDebug = false; // use 'debug=true' in pixel api (for event mgr)

    public GeneratorCommandLine () {
    }

    public boolean parse (String[] args) {
        int i = 0;

        if ((args == null) || (args.length < 2)) {
            showHelp ();
            return false;
        }

        while (i < args.length) {
            if (args[i].trim().equals ("-d") == true) {
                dataDirPath = args [i+1].trim();
                i = i + 2;
            } else if (args [i].trim().equals ("-a") == true) {
                accountName = args [i+1].trim();
                i = i + 2;
            } else if (args [i].trim().equals ("-l") == true) {
                messageLevel = args [i+1].trim();   // 'info'/'debug'/'warn'/'error'
                i = i + 2;
            } else if (args [i].trim().equals ("-r") == true) {
                realm = args [i+1].trim();   // 'staging'/'production'
                if (realm.equals ("prod") || realm.equals ("production"))
                    realm = GeneratorConstants.REALM_PROD;
                else if (realm.equals ("staging"))
                    realm = GeneratorConstants.REALM_STAGING;
                else {
                    showHelp ();
                    return false;
                }
                i = i + 2;
            } else if (args [i].trim().equals ("-e") == true) {
                envType = args [i+1].trim();   // 'dev'/'qa'/'release'
                i = i + 2;
            } else if (args [i].trim().equals ("-p") == true) {
                String pixelDataStr;
                pixelDataStr = args [i+1].trim(); // true|false
                if (pixelDataStr.toLowerCase ().startsWith ("t") == true)
                    pixelDebug = true;
                else if (pixelDataStr.toLowerCase ().startsWith ("f") == true)
                    pixelDebug = false;
                else {
                    showHelp ();
                    return false;
                }
                i = i + 2;
            } else if (args [i].trim().equals ("-t") == true) {
                String testDataStr;
                testDataStr = args [i+1].trim();   // true|false
                if (testDataStr.toLowerCase ().startsWith ("t") == true)
                    testData = true;
                else if (testDataStr.toLowerCase ().startsWith ("f") == true)
                    testData = false;
                else {
                    showHelp ();
                    return false;
                }
                i = i + 2;
            } else {
                showHelp ();
                return false;
            }
        }

        // enforce required args
        if (accountName == null) {
            showHelp ();
            return false;
        }
        if (dataDirPath == null) {
            showHelp ();
            return false;
        } else {
            File dirFile;
            dirFile = new File (dataDirPath);
            if ((dirFile.exists () == false) || (dirFile.isDirectory() == false)) {
                showHelp ();
                return false;
            }
        }

        return true;
    }

    public String getAccountName () {
        return accountName;
    }

    public String getDataDirPath () {
        return dataDirPath;
    }

    public String getMessageLevel () {
        return messageLevel;
    }
        
    public String getRealm () {
        return realm;
    }
       
    // in pixel, set test_data = true or false
    // Has no impact on discovery API calls 
    public boolean isTestData () {
        return testData;
    }

    public boolean isPixelDebug () {
        return pixelDebug;
    }

    public String getEnvType () {
        return envType;
    }
       
    private void showHelp () {
        System.out.println ("Generator " + 
                            "-a <accountName> " +
                            "-d <dataDirPath> " +
                            " [-l <info|debug|warn|error>] " +
                            " [-r <staging | production > ] " +
                            " [-t <true | false> ] " +
                            " [-e <dev | qa | release> ] " +
                            " [-p <true | false> ]"
                           );
    }

}


