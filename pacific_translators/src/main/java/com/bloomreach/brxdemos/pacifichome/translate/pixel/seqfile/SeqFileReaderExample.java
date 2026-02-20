package com.bloomreach.brxdemos.pacifichome.translate.pixel.seqfile;

// example to write a hadoop seq file

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import com.bloomreach.proto.PwfPixelLog;
import com.bloomreach.proto.Aggregation.Basket;
import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.CustomVariable;
import com.bloomreach.proto.Aggregation.PixelLog.PixelLogParam;
import com.bloomreach.proto.Aggregation.PixelLog.TrafficSource;
import com.bloomreach.analytics.pixel.CommonFields;
import com.bloomreach.analytics.pixel.PixelCatalog;
import com.bloomreach.analytics.ReferrerTypeParser;

public class SeqFileReaderExample 
{
    public static void main (String[] args)
    {
        SeqFileReaderExample seqReader;

        seqReader = new SeqFileReaderExample ();
        try
        {
            seqReader.doRead ();
        }
        catch (IOException e)
        {
            e.printStackTrace ();
            System.exit (-1);
        }
        catch (InstantiationException ie)
        {
            ie.printStackTrace ();
            System.exit (-1);
        }
        catch (IllegalAccessException iae)
        {
            iae.printStackTrace ();
            System.exit (-1);
        }


        System.exit (0);
    }

    private void doRead () throws IOException, InstantiationException, IllegalAccessException
    {
        // String path = "./seq.out";
        String path = "./part-00031";

        SeqFileWriterExample fileWriter = new SeqFileWriterExample ();

        Configuration configuration = new Configuration ();
        Reader.Option filePath = Reader.file (new Path (path));
        Reader reader = new SequenceFile.Reader (configuration, 
                                                 filePath);

        Writable key = (Writable) reader.getKeyClass().newInstance ();
        Writable value = (Writable) reader.getValueClass ().newInstance ();
        while (reader.next (key, value))
        {
            Text keyText;
            PixelLog pixelLog;
            PwfPixelLog pwfValue;

/*
            if (key instanceof Text)
                keyText = (Text) key;
            else
                System.out.println ("key not instance of Text");

            if (value instanceof PwfPixelLog)
                pwfValue = (PwfPixelLog) value;
            else
                System.out.println ("value not instance of PwfPixelLog");
*/
            if ((key instanceof Text) && (value instanceof PwfPixelLog))
            {
                try
                {
                    pwfValue = (PwfPixelLog) value;
                    pixelLog = pwfValue.getProto ();
                    fileWriter.doCloneAndCollect ((Text) key, pixelLog);
                }
                catch (IOException ie)
                {
                    ie.printStackTrace ();
                } 
                catch (Exception e)
                {
                    e.printStackTrace ();
                } 
            }
            else
                System.out.println ("key and/or value not expected classtype");

            System.out.println ("input key: " + key.toString () + ", value = " + value.toString());
        }

        try
        {
            fileWriter.doWrite ();
        }
        catch (Exception e)
        {
            e.printStackTrace ();
        }

        reader.close ();
    }
}
