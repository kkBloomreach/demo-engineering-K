package com.bloomreach.analyticsdatagenerator.generate;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;

import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;
import com.bloomreach.analyticsdatagenerator.feed.CategoryInfo;

public class RefinedJourneyMapGenerator {

    private String outputPath;
    private BufferedWriter bufferedWriter = null;

    public RefinedJourneyMapGenerator () {
    }

    // each UID simdata line has various fields eg, id, query, ...
    public void start (File outputFile) throws Exception {
        String headerLine;

        bufferedWriter = new BufferedWriter (new FileWriter (outputFile));
        // list of 'refined' queries is dynamic (ie, not a fixed number of entries)
        // view and segment are not included in this list. Simulator uses the
        // view, segment same as the primary query's view, segment
        headerLine = String.format ("%s\t%s\t%s\t%s\t%s\n",
                                    "journeyType", "primary", "view", "segment", "refined...");
        bufferedWriter.write (headerLine);
    }

    public void write (GeneratorInputData inputData, 
                       ArrayList<SearchQueryAndRefinements> searchQueryAndRefinementsList, 
                       ArrayList<CategoryAndRefinements> categoryAndRefinementsList) throws Exception {
        String line;

        // write journeyType s2s
        for (SearchQueryAndRefinements oneQueryAndRefinements : searchQueryAndRefinementsList) {
            String primaryQuery;
            ArrayList<String> refinements;
            StringBuffer refinedQueriesBuf;

            primaryQuery = oneQueryAndRefinements.getPrimaryQuery ();

            refinedQueriesBuf = new StringBuffer ();
            refinements = oneQueryAndRefinements.getRefinements ();
            for (String aRefinedQuery: refinements) {
                refinedQueriesBuf.append ("\t" + aRefinedQuery);
            }

            // example: "s2s"<tab>query1<tab>view<tab>segment<tab>refine1<tab>refine2<tab>...\n
            line = String.format ("%s\t%s\t%s\t%s%s\n",
                                   "s2s", primaryQuery, 
                                   oneQueryAndRefinements.getView(), oneQueryAndRefinements.getSegment(), refinedQueriesBuf.toString());
            bufferedWriter.write (line);
        }

        // write c2c list
        for (CategoryAndRefinements oneCatAndRefinements : categoryAndRefinementsList) {
            CategoryInfo primaryCategoryInfo;
            ArrayList<CategoryInfo> refinements;
            StringBuffer refinedCategoriesBuf;

            primaryCategoryInfo = oneCatAndRefinements.getPrimaryCategory ();

            refinedCategoriesBuf = new StringBuffer ();
            refinements = oneCatAndRefinements.getRefinements ();
            for (CategoryInfo aRefinedCatInfo : refinements) {
                refinedCategoriesBuf.append ("\t" + aRefinedCatInfo.getCatId ());
            }
 
            // example: "c2c"<tab>primaryCatId<tab>view<tab>segment<tab>refineCatId1<tab>refineCatId2<tab>...\n
            line = String.format ("%s\t%s\t%s\t%s%s\n",
                                   "c2c", primaryCategoryInfo.getCatId(), 
                                   oneCatAndRefinements.getView(), oneCatAndRefinements.getSegment(), refinedCategoriesBuf.toString());
            bufferedWriter.write (line);
        }

        // write s2c, c2s lists -- TO BE DONE (Is this needed ?)
    }

    public void close () throws Exception {
        if (bufferedWriter != null) {
            bufferedWriter.flush ();
            bufferedWriter.close ();
            bufferedWriter = null;
        }
    }
}

