// NOTE:Search query to pid map generator uses both APIcalls
package com.bloomreach.analyticsdatagenerator.generate;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.analyticsdatagenerator.GeneratorConstants;
import com.bloomreach.analyticsdatagenerator.MessageLogger;
import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;

public class SearchQueryToPidMapGenerator {

    private BufferedWriter bufferedWriter = null;
    private int id = 0;
    private GeneratorInputData inputData;

    public SearchQueryToPidMapGenerator () {
    }

    // each SearchQuery simdata line has various fields eg, id, query, ...
    public void start (File outputFile) throws Exception {
        String headerLine;

        bufferedWriter = new BufferedWriter (new FileWriter (outputFile));
        headerLine = String.format ("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                                    "id", "query", "numFound", "primary", "view", "segment", "pid", "pid", "pid", "pid", "pid");
        bufferedWriter.write (headerLine);
    }

    // this method returns the dynamically generated query->refinement map.
    // that map is used later to write refinementJourney map
    // In order to make sure we get MIN_REQUIRED for both primary and its refinements, we
    // first evaluate that and then write those entries
    public ArrayList<SearchQueryAndRefinements> write (GeneratorInputData inputData) throws Exception {
        String[] views;
        String[] segments;
        SearchQueryExecutor queryExecutor;
        String[] allPrimarySearchQueries;    // obtained from input data
        ArrayList<SearchQueryAndRefinements> allSearchQueryAndRefinementsList;
        ArrayList<SearchQueryLogInfo> allSearchQueryLogInfoList;

        this.inputData = inputData;

        views = inputData.getViews ();
        segments = inputData.getSegments ();
        allPrimarySearchQueries = inputData.getPrimarySearchQueries ();
        queryExecutor = new SearchQueryExecutor (inputData);
        allSearchQueryLogInfoList = new ArrayList <SearchQueryLogInfo> ();

        // construct all
        allSearchQueryAndRefinementsList = prepareAllSearchQueryAndRefinementsList (allPrimarySearchQueries, views, segments, 
                                                                                    allSearchQueryLogInfoList, 
                                                                                    inputData.getExcludeProducts(), queryExecutor);

        // verify we have atleast some queries for each view+segment combination
        verifyMinimumData (allSearchQueryLogInfoList, views, segments);

        // actually write out all query-info-logs to .tsv
        writeSearchQueryPids (allSearchQueryLogInfoList);

        // return query->refinements list. It is later used to generate refinedJourney map
        return (allSearchQueryAndRefinementsList);
    }

    public void close () throws Exception {
        if (bufferedWriter != null) {
            bufferedWriter.flush ();
            bufferedWriter.close ();
            bufferedWriter = null;
        }
    }


    private ArrayList <SearchQueryAndRefinements> prepareAllSearchQueryAndRefinementsList (String[] allPrimarySearchQueries, 
                                                                                     String[] views, String[] segments, 
                                                                                     ArrayList<SearchQueryLogInfo> allSearchQueryLogInfoList,
                                                                                     String[] excludeProducts,
                                                                                     SearchQueryExecutor queryExecutor) throws Exception {
        ArrayList<SearchQueryAndRefinements> allSearchQueryAndRefinementsList;

        allSearchQueryAndRefinementsList = new ArrayList <SearchQueryAndRefinements> ();  // contains primaryQuery -> refinedQuries list

        // go thru all primaryQueries and build primary->refinements list
        for (int i = 0; i < allPrimarySearchQueries.length; i++) {
            String primaryQuery;
            String[] refinedQueries;

            primaryQuery = allPrimarySearchQueries [i];
            refinedQueries = this.inputData.getRefinedSearchQueries (primaryQuery);

            // for this primaryQuery, generate its refinedQuery list
            for (int v = 0; v < views.length; v++) {
                for (int s = 0; s < segments.length; s++) {
                    SearchQueryAndRefinements searchQueryAndRefinements;

                    // first prepare a SearchQueryAndRefinements object to ensure we have MIN pids
                    // then use that object to write out QueryPid list. The same queryAndRefinements
                    // is later used to write refinedURL list where the queries need to be consistent with those in the queryPid list
                    searchQueryAndRefinements = prepareOneSearchQueryAndRefinements (primaryQuery, views [v], segments [s], 
                                                                                     refinedQueries, allSearchQueryLogInfoList,
                                                                                     excludeProducts, queryExecutor);
                    if (searchQueryAndRefinements != null) {
                        // add to the entire list of searchQueryAndRefinementsList if not already in that list
                        allSearchQueryAndRefinementsList.add (searchQueryAndRefinements);
                    } // else warning already issued
                }
            }
        }

        MessageLogger.logDebug ("SearchQueryPid. Number of entries: " + allSearchQueryAndRefinementsList.size());
        return allSearchQueryAndRefinementsList;  // happens if we cannot generate necessary amount of entries
    }

    private SearchQueryAndRefinements prepareOneSearchQueryAndRefinements (String primaryQuery, String view, String segment, 
                                                                           String[] allRefinedQueries, 
                                                                           ArrayList<SearchQueryLogInfo> allSearchQueryLogInfoList,
                                                                           String[] excludeProducts,
                                                                           SearchQueryExecutor queryExecutor) throws Exception {
        SearchQueryLogInfo primaryQueryLogInfo;
        ArrayList<SearchQueryLogInfo> refinementsLogInfo; 
        String segmentFq;
        SearchQueryResponseInfo primaryQueryResponseInfo;
            
        segmentFq = this.inputData.getSegmentFq (segment);

        // PRIMARY
        primaryQueryLogInfo = new SearchQueryLogInfo (primaryQuery, true, view, segment);
        if (allSearchQueryLogInfoList.contains (primaryQuery) == true) 
            return null;    // this 'primary' query has been logged. Happens if user specifies same query in input .json
 
        primaryQueryResponseInfo = queryExecutor.getQueryResponseInfo (primaryQuery, view, segment, segmentFq);
        if (primaryQueryResponseInfo == null) {
            MessageLogger.logWarning ("SearchQueryResponse is null"); 
            return null;
        } 

        // if a pid in queryResponse is to be excluded, do that now. Adjusts its internal data as needed
        primaryQueryResponseInfo.removeExcludedPidIfInResponse (excludeProducts);
        if (primaryQueryResponseInfo.getNumFound () < GeneratorConstants.MIN_REQUIRED_NUM_FOUND) {
            MessageLogger.logWarning ("SearchQueryToPid Insufficient number of products: primaryQuery " + primaryQuery + 
                                        ", view = " + view + ", segment = " + segment + ", fq = " + segmentFq + 
                                        ", numFound = " + primaryQueryResponseInfo.getNumFound ());
            return null;
        }
        primaryQueryLogInfo.setQueryResponseInfo (primaryQueryResponseInfo);

        // REFINEMENTS
        refinementsLogInfo = new ArrayList <SearchQueryLogInfo> ();
        for (int i = 0; i < allRefinedQueries.length; i++) {
            String query;
            SearchQueryLogInfo queryLogInfo;

            query = allRefinedQueries [i];
            queryLogInfo = new SearchQueryLogInfo (query, false, view, segment);
            if ((query.equals (primaryQuery) == false) && (refinementsLogInfo.contains (queryLogInfo) == false)) {
                // see if this queryInfo is in the allInfoLogList list
                if (allSearchQueryLogInfoList.contains (queryLogInfo) == true) {
                    int indx;

                    // use queryResponseInfo from the earlier entry - avoid another API call
                    indx = allSearchQueryLogInfoList.indexOf (queryLogInfo);
                    queryLogInfo = allSearchQueryLogInfoList.get (indx);
                } else {
                    SearchQueryResponseInfo queryResponseInfo;

                    queryResponseInfo = queryExecutor.getQueryResponseInfo (query, view, segment, segmentFq);
                    if (queryResponseInfo == null) {
                        MessageLogger.logWarning ("SearchQueryResponse is null"); 
                        continue;
                    }
 
                    // if a pid in queryResponse is to be excluded, do that now
                    queryResponseInfo.removeExcludedPidIfInResponse (excludeProducts);
                    if (queryResponseInfo.getNumFound () < GeneratorConstants.MIN_REQUIRED_NUM_FOUND) {
                        MessageLogger.logWarning ("SearchQueryToPid Insufficient number of products: refinedQuery " + query + 
                                            ", view = " + view + ", segment = " + segment + ", fq = " + segmentFq + 
                                            ", numFound = " + queryResponseInfo.getNumFound ());
                        continue;   // unusable refined query - not included in generated data 
                    }
                    queryLogInfo.setQueryResponseInfo (queryResponseInfo);
                }

                // add this queryLogInfo as refinedLogInfo for current primary
                refinementsLogInfo.add (queryLogInfo);

                // if we have needed number of refined entries for this query, return
                if (refinementsLogInfo.size () == GeneratorConstants.MAX_REFINEMENTS_PER_QUERY)
                    break;  // we got enough refinement entries for given primaryQuery
            }
        }

        if (refinementsLogInfo.size () == GeneratorConstants.MAX_REFINEMENTS_PER_QUERY) {
            SearchQueryAndRefinements searchQueryAndRefinements;
            ArrayList<String> refinements;

            // we have primary AND its refinements OK for given view and segment
            // add primary and all the refined logLists to alreadyLoggedList
            allSearchQueryLogInfoList.add (primaryQueryLogInfo);

            refinements = new ArrayList <String> ();
            for (SearchQueryLogInfo aQueryLogInfo : refinementsLogInfo) {
                if (allSearchQueryLogInfoList.contains (aQueryLogInfo) == false)
                    allSearchQueryLogInfoList.add (aQueryLogInfo);

                // also add refinedQuery to refinements list. This list is included in SearchQueryAndRefinements
                refinements.add (aQueryLogInfo.getQuery ());
            }

            searchQueryAndRefinements = new SearchQueryAndRefinements (primaryQuery, refinements, view, segment); 
            return searchQueryAndRefinements;
        }

        // insufficient refinements for current primary
        MessageLogger.logWarning ("Cannot generate sufficient refinements for query: " + primaryQuery);
        return null;
    }

    private void verifyMinimumData (ArrayList<SearchQueryLogInfo> allSearchQueryLogInfo, String[] views, String[] segments) {
        for (int v = 0; v < views.length; v++) {
            for (int s = 0; s < segments.length; s++) {
                boolean exists;

                // see if we have a searchQueryLogInfo for this view+segment
                exists = lookupSearchQueryLogInfo (allSearchQueryLogInfo, views[v], segments[s]);
                if (exists == false) {
                    MessageLogger.logWarning ("No searchQueryData generated for view = " + views [v] + ", segment = " + segments [s]);
                } 
            }
        } 
    }

    private boolean lookupSearchQueryLogInfo (ArrayList<SearchQueryLogInfo> allSearchQueryLogInfo, String view, String segment) {
        for (SearchQueryLogInfo searchQueryLogInfo : allSearchQueryLogInfo) {
            if ((searchQueryLogInfo.getView ().equals (view) == true) && (searchQueryLogInfo.getSegment ().equals (segment))) {
                return true;
            }
        }
        return false;
    }

    private void writeSearchQueryPids (ArrayList<SearchQueryLogInfo> allSearchQueryLogInfo) throws Exception {
        for (SearchQueryLogInfo aLogInfo : allSearchQueryLogInfo) {
            SearchQueryResponseInfo queryResponseInfo;

            queryResponseInfo = aLogInfo.getQueryResponseInfo ();
            writeOneQueryPidMap (aLogInfo.getQuery (), aLogInfo.isPrimary(), aLogInfo.getView (), aLogInfo.getSegment (),
                                 queryResponseInfo.getNumFound (), queryResponseInfo.getQueryResponseDocs ()); 
        }
    }

    private void writeOneQueryPidMap (String query, boolean isPrimary, String view, String segment,
                                      int numFound, SearchQueryResponseDoc[] queryResponseDocs) throws Exception {
        String line;
        String primary;
        StringBuffer pidBuf;
        int minPidsToWrite;

        if (isPrimary == true)
            primary = "1";
        else
            primary = "0";

        // currently we write only MIN_ pids per query
        pidBuf = new StringBuffer ();
        minPidsToWrite = Math.min (GeneratorConstants.MIN_REQUIRED_NUM_FOUND, queryResponseDocs.length);
        for (int i = 0; i < minPidsToWrite; i++) {
            pidBuf.append ("\t" + queryResponseDocs[i].getPid());
        }
 
        line = String.format ("%s\t%s\t%s\t%s\t%s\t%s%s\n",
                               id, query, numFound, primary, view, segment,
                               pidBuf.toString ());

        bufferedWriter.write (line);
        id = id + 1;
    }
}


