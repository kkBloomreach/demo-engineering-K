package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

// Read input sequence file(s), get pixeLogs from them, clone each pixelLog and write the clone'd logs to output file(s)
import java.util.ArrayList;
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

import com.bloomreach.proto.PwfPixelLog;
import com.bloomreach.proto.Aggregation.PixelLog;

public class CloneOnePixelLogFile {

    ProcessedFeed processedFeed = null;
    UidToViewIdMap uidViewIdMap = null;

    public CloneOnePixelLogFile (ProcessedFeed processedFeed, UidToViewIdMap uidViewIdMap) {
        this.processedFeed = processedFeed;
        this.uidViewIdMap = uidViewIdMap;
    }

    public void doClone (String srcPath, String outputPath) throws Exception {

        ArrayList <PixelLog> srcPixelLogs;

        srcPixelLogs = readSourceFile (srcPath);
        if (srcPixelLogs.size () > 0) {
            CloneSourcePixelLogs cloneSourcePixelLogs;
            ArrayList <PixelLog> clonedPixelLogsList;

            cloneSourcePixelLogs = new CloneSourcePixelLogs (processedFeed, uidViewIdMap);
            clonedPixelLogsList = cloneSourcePixelLogs.clone (srcPixelLogs);

            if ((clonedPixelLogsList != null) && (clonedPixelLogsList.size () > 0)) 
                writeOutputFile (clonedPixelLogsList, outputPath);
        }
    }

    private ArrayList<PixelLog> readSourceFile (String srcPath) throws IOException, InstantiationException, IllegalAccessException {
        ArrayList <PixelLog> srcPixelLogs = new ArrayList <PixelLog> ();

        Configuration configuration = new Configuration ();
        Reader.Option filePath = Reader.file (new Path (srcPath));
        Reader reader = new SequenceFile.Reader (configuration, filePath);

        Writable key = (Writable) reader.getKeyClass().newInstance ();
        Writable value = (Writable) reader.getValueClass ().newInstance ();
        while (reader.next (key, value))
        {
            Text keyText;
            PixelLog pixelLog;
            PwfPixelLog pwfValue;

            if ((key instanceof Text) && (value instanceof PwfPixelLog))
            {
                try
                {
                    pwfValue = (PwfPixelLog) value;
                    pixelLog = pwfValue.getProto ();
                    srcPixelLogs.add (pixelLog);    // "key" is always pacificsupply
                }
                catch (Exception e)
                {
                    e.printStackTrace ();
                } 
            }
            else
                System.out.println ("key and/or value not expected classtype");
        }

        return (srcPixelLogs);
    }

    private void writeOutputFile (ArrayList<PixelLog> clonedPixelLogsList, String outputPath) throws Exception {

        Configuration configuration = new Configuration ();
        Writer.Option filePath = Writer.file (new Path (outputPath));
        Writer.Option keyClass = Writer.keyClass (Text.class);
        Writer.Option valueClass = Writer.valueClass (PwfPixelLog.class);
        Writer writer = SequenceFile.createWriter (configuration, 
                                                    filePath,
                                                    keyClass,
                                                    valueClass);

        Text key = new Text ("pacificsupply");
        for (PixelLog pixelLog : clonedPixelLogsList) {
            writer.append (key, new PwfPixelLog (pixelLog));
        }

        writer.hflush ();
        writer.close ();
    }
}

/***
//    public static void main (String[] args) {
//        CloneOnePixelLogFile cloneFile;
//
//        cloneFile = new CloneOnePixelLogFile ();
//
//        String srcPath = "./data/part-00031";
//        String outputPath = "./data/output_part-00031";
//
//        try {
//            cloneFile.doClone (srcPath, outputPath);
//        } catch (Exception e) {
//            e.printStackTrace ();
//            System.exit (-1);
//        }
//
//        System.exit (0);
//    }
***/

