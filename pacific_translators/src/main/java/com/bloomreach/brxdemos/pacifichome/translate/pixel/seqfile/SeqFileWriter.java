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


public class SeqFileWriter {

    public SeqFileWriter () {
    }

    public void writeOutputFile (ArrayList<PixelLog> clonedPixelLogsList, String outputPath) throws Exception {

        Configuration configuration = new Configuration ();
        Writer.Option filePath = Writer.file (new Path (outputPath));
        Writer.Option keyClass = Writer.keyClass (Text.class);
        Writer.Option valueClass = Writer.valueClass (PwfPixelLog.class);
        Writer writer = SequenceFile.createWriter (configuration, 
                                                    filePath,
                                                    keyClass,
                                                    valueClass);

        Text key = new Text ("pacifichome");
        for (PixelLog pixelLog : clonedPixelLogsList) {
            writer.append (key, new PwfPixelLog (pixelLog));
        }

        writer.hflush ();
        writer.close ();
    }
}

