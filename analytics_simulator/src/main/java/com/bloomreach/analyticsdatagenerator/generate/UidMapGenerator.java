package com.bloomreach.analyticsdatagenerator.generate;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;

import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;

public class UidMapGenerator {

    private String outputPath;
    private BufferedWriter bufferedWriter = null;

    public UidMapGenerator () {
    }

    // each UID simdata line has various fields eg, id, query, ...
    public void start (File outputFile) throws Exception {
        String headerLine;

        bufferedWriter = new BufferedWriter (new FileWriter (outputFile));
        headerLine = String.format ("%s\t%s\t%s\n",
                                    "uid", "view", "segment");
        bufferedWriter.write (headerLine);
    }

    public void write (GeneratorInputData inputData) throws Exception {
        String line;
        long uidBase;
        String[] views;
        String[] segments;
        int v = 0;
        int s = 0;

        uidBase = (long) 9555555550000L;
        views = inputData.getViews ();
        segments = inputData.getSegments ();

        for (int u = 0; u < inputData.getUidCount(); u++) {
            line = String.format ("%s\t%s\t%s\n",
                                   Long.toString (uidBase+u), views[v], segments [s]);
            v = (v + 1) % views.length;
            s = (s + 1) % segments.length;
            bufferedWriter.write (line);
        }
    }

    public void close () throws Exception {
        if (bufferedWriter != null) {
            bufferedWriter.flush ();
            bufferedWriter.close ();
            bufferedWriter = null;
        }
    }
}

