package com.bloomreach.analyticsdatagenerator.generate;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;

import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;

public class ZeroResultQueryMapGenerator {

    private String outputPath;
    private BufferedWriter bufferedWriter = null;
    int id = 0;

    public ZeroResultQueryMapGenerator () {
    }

    // each UID simdata line has various fields eg, id, query, ...
    public void start (File outputFile) throws Exception {
        String headerLine;

        bufferedWriter = new BufferedWriter (new FileWriter (outputFile));
        //headerLine = String.format ("%s\t%s\t%s\t%s\t%s\t%s\n",
        //                           "id", "query", "view", "segment", "refinedSearchQuery", "refinedCategoryQuery");
        headerLine = String.format ("%s\t%s\n",
                                   "id", "query");
        bufferedWriter.write (headerLine);
    }

    // generate zeroResultQuery map
    // Currently zeroResult queries do NOT consider view/segment. All 
    // these queries are said to return zero-results irrespective of view or segment
    public void write (GeneratorInputData inputData) throws Exception {
        String[] zeroResultQueries;

        zeroResultQueries = inputData.getZeroResultQueryList ();
        for (int i = 0; i < zeroResultQueries.length; i++) {
            String query;
            String line;

            query = zeroResultQueries [i];
            line = String.format ("%s\t%s\n",
                                  this.id, query);
            bufferedWriter.write (line);
            this.id = this.id + 1;
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

