package com.bloomreach.brxdemos.pacifichome.translate.pixel.seqfile;

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

public class SeqFileReader {
    public SeqFileReader () {
    }

    public ArrayList<PixelLog> readSourceFile (String srcPath) throws IOException, InstantiationException, IllegalAccessException {
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
                    srcPixelLogs.add (pixelLog);    // "key" is always pacifichome
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
}

