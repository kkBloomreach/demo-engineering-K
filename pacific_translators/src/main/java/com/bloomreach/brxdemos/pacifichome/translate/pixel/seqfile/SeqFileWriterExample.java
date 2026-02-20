package com.bloomreach.brxdemos.pacifichome.translate.pixel.seqfile;

// example to write a hadoop seq file

import java.io.IOException;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.bloomreach.proto.PwfPixelLog;
import com.bloomreach.proto.Aggregation.Basket;
import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.proto.Aggregation.PixelLog.CustomVariable;
import com.bloomreach.proto.Aggregation.PixelLog.PixelLogParam;
import com.bloomreach.proto.Aggregation.PixelLog.TrafficSource;
import com.bloomreach.analytics.pixel.CommonFields;
import com.bloomreach.analytics.pixel.PixelCatalog;
import com.bloomreach.analytics.ReferrerTypeParser;

public class SeqFileWriterExample 
{
    private ArrayList <ClonePixelLogEntry> pixelLogList = new ArrayList <ClonePixelLogEntry> ();

    public static void main (String[] args)
    {
        SeqFileWriterExample seqWriter;

        seqWriter = new SeqFileWriterExample ();
        try
        {
            seqWriter.doWrite ();
        }
        catch (IOException e)
        {
            e.printStackTrace ();
            System.exit (-1);
        }

        System.exit (0);
    }

    public SeqFileWriterExample ()
    {
    }

    public void doCloneAndCollect (Text key, PixelLog pixelLog) throws Exception 
    {
        Builder pixelLogBuilder = PixelLog.newBuilder (pixelLog);
        PixelLog clonedPixelLog = pixelLogBuilder.build (); 
        pixelLogList.add (new ClonePixelLogEntry (key, clonedPixelLog));
    }

    public void doWrite () throws IOException 
    {
        if (pixelLogList.size () == 0)
            return;

        String path = "./seq.out";

        Configuration configuration = new Configuration ();
        Writer.Option filePath = Writer.file (new Path (path));
        Writer.Option keyClass = Writer.keyClass (Text.class);
        Writer.Option valueClass = Writer.valueClass (PwfPixelLog.class);
        Writer writer = SequenceFile.createWriter (configuration, 
                                                    filePath,
                                                    keyClass,
                                                    valueClass);

        for (Iterator <ClonePixelLogEntry> iter = pixelLogList.iterator (); iter.hasNext ();)
        {
            ClonePixelLogEntry logEntry = iter.next ();
            Text key = logEntry.getKey ();
            PixelLog pixelLog = logEntry.getPixelLog ();
            writer.append (key, new PwfPixelLog (pixelLog));
        }

        writer.hflush ();
        writer.close ();
    }

    class ClonePixelLogEntry 
    {
        Text key;
        PixelLog pixelLog;

        public ClonePixelLogEntry (Text key, PixelLog pixelLog)
        {
            this.key = key;
            this.pixelLog = pixelLog;
        }

        public Text getKey ()
        {
            return this.key;
        }

        public PixelLog getPixelLog ()
        {
            return this.pixelLog;
        }
    }
}
