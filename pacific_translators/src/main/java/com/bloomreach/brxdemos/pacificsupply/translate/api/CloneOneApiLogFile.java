// Read input sequence file(s), get apiLogs from them, clone each apiLog and write the clone'd logs to output file(s)
package com.bloomreach.brxdemos.pacificsupply.translate.api;

import java.util.ArrayList;
import java.util.Hashtable;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.bloomreach.proto.PWfMobileApiLog;
import com.bloomreach.proto.MobileApi.ApiLog;
import com.bloomreach.proto.MobileApi.ApiLog.Builder;
import com.bloomreach.proto.MobileApi.ApiRequest;
import com.bloomreach.proto.MobileApi.CommonRequest;
import com.bloomreach.proto.MobileApi.CommonRequest.RequestType;

public class CloneOneApiLogFile {

    ProcessedFeed processedFeed = null;
    Hashtable<String, String> uidToViewIdMap;

    public CloneOneApiLogFile (ProcessedFeed processedFeed, Hashtable<String, String> uidToViewIdMap) {
        this.processedFeed = processedFeed;
        this.uidToViewIdMap = uidToViewIdMap;
    }

    // returns cloned apiLogs from a single source logfile
    public ArrayList <ApiLog> doClone (String srcPath) throws Exception {
        ArrayList <ApiLog> srcApiLogs;

        // single source file contains multiple individual apiLogs
        srcApiLogs = readSourceFile (srcPath);
        if (srcApiLogs.size () > 0) {
            CloneSourceApiLogs cloneSourceApiLogs;
            ArrayList <ApiLog> clonedApiLogsList;

            clonedApiLogsList = clone (srcApiLogs);
            return (clonedApiLogsList);
        }

        return (null);
    }

    private ArrayList<ApiLog> readSourceFile (String srcPath) throws IOException, InstantiationException, IllegalAccessException {
        ArrayList <ApiLog> srcApiLogs = new ArrayList <ApiLog> ();

        Configuration configuration = new Configuration ();
        Reader.Option filePath = Reader.file (new Path (srcPath));
        Reader reader = new SequenceFile.Reader (configuration, filePath);

        Writable key = (Writable) reader.getKeyClass().newInstance ();
        Writable value = (Writable) reader.getValueClass ().newInstance ();
        while (reader.next (key, value))
        {
            Text keyText;
            ApiLog ApiLog;
            PWfMobileApiLog pwfValue;

            if ((key instanceof Text) && (value instanceof PWfMobileApiLog))
            {
                try
                {
                    pwfValue = (PWfMobileApiLog) value;
                    ApiLog = pwfValue.getProto ();
                    srcApiLogs.add (ApiLog);    // "key" is always pacificsupply
                }
                catch (Exception e)
                {
                    e.printStackTrace ();
                } 
            }
            else
                System.out.println ("key and/or value not expected classtype");
        }

        return (srcApiLogs);
    }

    private ArrayList <ApiLog> clone (ArrayList<ApiLog> srcApiLogs) {
        ArrayList <ApiLog> cloneApiLogList;

        // list of clone'd ApiLogs, returned from this method
        cloneApiLogList = new ArrayList <ApiLog> ();
        for (ApiLog apiLog : srcApiLogs) {
            int cloneStatus;
            ApiLog.Builder cloneApiLogBuilder;
            ApiRequest.Builder apiRequestBuilder;
            CommonRequest.Builder commonRequestBuilder;
            RequestType requestType;

            cloneStatus = CloneApiConstants.CLONE_STATUS_ERROR; // default set

            cloneApiLogBuilder = ApiLog.newBuilder (apiLog);

            apiRequestBuilder = cloneApiLogBuilder.getRequestBuilder ();
            commonRequestBuilder = apiRequestBuilder.getCommonBuilder ();
            requestType = commonRequestBuilder.getRequestType ();

            // looks like original APIs include only suggest, search and mlt. No category APIs
            switch (requestType) {
                case SUGGEST:
                    // System.out.println ("requestType = suggest");
                    CloneSuggestApiLog cloneSuggestApiLog;
                    cloneSuggestApiLog = new CloneSuggestApiLog ();
                    cloneStatus = cloneSuggestApiLog.cloneApiLog (cloneApiLogBuilder, processedFeed, uidToViewIdMap); 
                    break;

                case SEARCH:
                    // System.out.println ("requestType = search");
                    CloneSearchApiLog cloneSearchApiLog;
                    cloneSearchApiLog = new CloneSearchApiLog ();
                    cloneStatus = cloneSearchApiLog.cloneApiLog (cloneApiLogBuilder, processedFeed, uidToViewIdMap); 
                    break;

                case MLT:
                    // System.out.println ("requestType = mlt");
                    CloneMLTApiLog cloneMLTApiLog;
                    cloneMLTApiLog = new CloneMLTApiLog ();
                    cloneStatus = cloneMLTApiLog.cloneApiLog (cloneApiLogBuilder, processedFeed, uidToViewIdMap); 
                    break;

                default:
                    System.out.println ("requestType DEFAULT = " + requestType.name());
                    break;
            }

            // finally, if cloneStatus for this ApiLog is OK, build a ApiLog
            // object from the logBuilder and add it to the return-list
            if (cloneStatus == CloneApiConstants.CLONE_STATUS_OK) {
                ApiLog cloneApiLog;

                cloneApiLog = cloneApiLogBuilder.build ();
                cloneApiLogList.add (cloneApiLog);
            }
        }

        return (cloneApiLogList);
    }
}


