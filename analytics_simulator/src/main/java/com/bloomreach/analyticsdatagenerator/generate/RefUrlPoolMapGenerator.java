package com.bloomreach.analyticsdatagenerator.generate;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;

import com.bloomreach.analyticsdatagenerator.GeneratorConstants;
import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;
import com.bloomreach.analyticsdatagenerator.feed.ProcessedFeed;
import com.bloomreach.analyticsdatagenerator.feed.CategoryInfo;

public class RefUrlPoolMapGenerator {

    private String outputPath;
    private BufferedWriter bufferedWriter = null;

    public RefUrlPoolMapGenerator () {
    }

    // each UID simdata line has various fields eg, id, query, ...
    public void start (File outputFile) throws Exception {
        String headerLine;

        bufferedWriter = new BufferedWriter (new FileWriter (outputFile));
        headerLine = String.format ("%s\t%s\t%s\t%s\n",
                                    "urlType", "view", "segment", "value");
        bufferedWriter.write (headerLine);
    }

    // generate refUrls for home, categories, product
    // for product, use list of queries in inputData and write MAX # of refUrls
    public void write (GeneratorInputData inputData) throws Exception {
        SearchQueryExecutor queryExecutor;
        String line;

        queryExecutor = new SearchQueryExecutor (inputData);
        // home
        writeHomeRefUrl (inputData, queryExecutor);

        // categories - uses processedFeed data 
        writeCategoryRefUrl (inputData);

        // products
        writeProductRefUrl (inputData, queryExecutor);
    }

    public void close () throws Exception {
        if (bufferedWriter != null) {
            bufferedWriter.flush ();
            bufferedWriter.close ();
            bufferedWriter = null;
        }
    }

    private void writeHomeRefUrl (GeneratorInputData inputData, SearchQueryExecutor queryExecutor) throws Exception {
        String[] views;
        String[] segments;
        String refUrlValue;

        views = inputData.getViews ();
        segments = inputData.getSegments ();
        refUrlValue = inputData.getRefUrlValue ("home");  // refType = home; expect only one value = "/"

        for (int v = 0; v < views.length; v++) {
            for (int s = 0; s < segments.length; s++) {
                String line;

                line = String.format ("%s\t%s\t%s\t%s\n",
                                      "home", views [v], segments [s], refUrlValue);
                bufferedWriter.write (line);
            }
        }
    }

    private void writeProductRefUrl (GeneratorInputData inputData, SearchQueryExecutor queryExecutor) throws Exception {
        String[] views;
        String[] segments;
        ArrayList<String> allQueries;
        String[] primaryQueries;
        int maxUrlCount = 0;

        // collect all primary + refinedQueries in one line
        allQueries = new ArrayList<String> ();
        primaryQueries = inputData.getPrimarySearchQueries ();
        for (int i = 0; i < primaryQueries.length; i++) {
            String[] refinedQueries;

            allQueries.add (primaryQueries [i]);
            refinedQueries = inputData.getRefinedSearchQueries (primaryQueries[i]);
            for (int r = 0; r < refinedQueries.length; r++)
                allQueries.add (refinedQueries [r]);
        }

        views = inputData.getViews ();
        segments = inputData.getSegments ();

        maxUrlCount = Math.min (GeneratorConstants.MAX_PRODUCT_REFURLS, allQueries.size());
        for (int i = 0; i < maxUrlCount; i++) {
            String query;

            query = allQueries.get (i); // could use random index -- TO BE DONE
            for (int v = 0; v < views.length; v++) {
                for (int s = 0; s < segments.length; s++) {
                    String segmentFq;
                    SearchQueryResponseInfo queryResponseInfo;
 
                    segmentFq = inputData.getSegmentFq (segments [s]);
                    queryResponseInfo = queryExecutor.getQueryResponseInfo (query, views [v], segments [s], segmentFq);
                    if (queryResponseInfo.getNumFound () > 0) {
                        String pid;
                        String variant;
                        String value;
                        String line;
                        SearchQueryResponseDoc[] queryResponseDocs;

                        queryResponseDocs = queryResponseInfo.getQueryResponseDocs ();
                        pid = queryResponseDocs [0].getPid ();
                        variant = queryResponseDocs [0].getVariant (); // may be null
                        if (variant == null)
                            value = pid + "___" + pid;
                        else
                            value = pid + "___" + variant;

                        line = String.format ("%s\t%s\t%s\t%s\n",
                                              "product", views [v], segments [s], value);
                        bufferedWriter.write (line);
                    }
                }
            } 
        }
    }

    // use info from feed to pick MAX categories
    // Although view and segment are not needed for category refUrls, we include them just
    // to keep the refUrl.tsv file format consistent among all refUrls
    private void writeCategoryRefUrl (GeneratorInputData inputData) throws Exception {
        ArrayList <CategoryInfo> allCategories;
        String[] views;
        String[] segments;
        int maxUrlCount = 0;
        ArrayList <CategoryInfo> alreadyListed;

        views = inputData.getViews ();
        segments = inputData.getSegments ();

        allCategories = inputData.getProcessedFeed ().getAllCategoryInfoList ();
        maxUrlCount = Math.min (GeneratorConstants.MAX_CATEGORY_REFURLS, allCategories.size());
        alreadyListed = new ArrayList <CategoryInfo> ();

        for (int i = 0; i < maxUrlCount; i++) {
            CategoryInfo catInfo;
            int indx;

            indx = (int) (Math.random () * allCategories.size());
            catInfo = allCategories.get (indx);
            if (alreadyListed.contains (catInfo) == false) { // avoid duplicate refUrl pool entries
                alreadyListed.add (catInfo);
                for (int v = 0; v < views.length; v++) {
                    for (int s = 0; s < segments.length; s++) {
                        String line;

                        line = String.format ("%s\t%s\t%s\t%s\n",
                                              "category", views [v], segments [s], catInfo.getCatId());
                        bufferedWriter.write (line);
                    }
                }
            } 
        }
    }

}

