// NOTE: Category query to pid map generator uses both APIcalls and processedFeed
package com.bloomreach.analyticsdatagenerator.generate;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Hashtable;

import org.json.JSONObject;
import org.json.JSONArray;

import com.bloomreach.analyticsdatagenerator.GeneratorConstants;
import com.bloomreach.analyticsdatagenerator.MessageLogger;
import com.bloomreach.analyticsdatagenerator.input.GeneratorInputData;
import com.bloomreach.analyticsdatagenerator.feed.*;

public class CategoryQueryToPidMapGenerator {

    private BufferedWriter bufferedWriter = null;
    private int id = 0;
    private GeneratorInputData inputData;

    public CategoryQueryToPidMapGenerator () {
    }

    // each CategoryQuery simdata line has various fields eg, id, query, ...
    public void start (File outputFile) throws Exception {
        String headerLine;

        bufferedWriter = new BufferedWriter (new FileWriter (outputFile));
        headerLine = String.format ("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
                                    "id", "catname", "catid", "catpath", "numFound", "primary", "view", "segment", "pid", "pid", "pid", "pid", "pid");
        bufferedWriter.write (headerLine);
    }

    // this method returns the dynamically generated category->refinement map.
    // that map is used later to write refinementJourney map
    // In order to make sure we get MIN_REQUIRED for both primary and its refinements, we
    // first evaluate that and then write those entries
    // NOTE: Based on actual runs, it takes too long to go thru entire list of categories in the feed
    public ArrayList<CategoryAndRefinements> write (GeneratorInputData inputData) throws Exception {
        String[] views;
        String[] segments;
        CategoryQueryExecutor queryExecutor;
        ArrayList<CategoryInfo> allCategoryInfoList;    // obtained from the feed
        ArrayList<CategoryInfo> allCleanCategoryInfoList;    // obtained from the feed
        ArrayList<CategoryAndRefinements> allCategoryAndRefinementsList;
        ArrayList<CategoryLogInfo> allCatLogInfoList;

        this.inputData = inputData;

        views = inputData.getViews ();
        segments = inputData.getSegments ();
        allCategoryInfoList = inputData.getProcessedFeed().getAllCategoryInfoList ();

        // before using the full category list, remove any exclusions specified in input json
        // This is because the feed may have strange catgories that shouldn't generate pixel / api logs
        allCleanCategoryInfoList = removeExcludedCategories (allCategoryInfoList, inputData.getExcludeCategories());

        queryExecutor = new CategoryQueryExecutor (inputData);
        allCatLogInfoList = new ArrayList <CategoryLogInfo> ();

        // construct all - populates allCatLogInfoList
        allCategoryAndRefinementsList = prepareAllCategoryAndRefinementsList (allCleanCategoryInfoList, 
                                                                              views, segments, allCatLogInfoList, 
                                                                              inputData.getExcludeProducts (), queryExecutor);

        // verify MINIMUM data for simulation - ie, there are atLeast N entries for each view+segment combination
        verifyMinimumData (allCatLogInfoList, views, segments);

        // actually write out all cat-info-logs to .tsv
        writeCategoryQueryPids (allCatLogInfoList);

        // return cat->refinements list. It is later used to generate refinedJourney map
        return (allCategoryAndRefinementsList);
    }

    public void close () throws Exception {
        if (bufferedWriter != null) {
            bufferedWriter.flush ();
            bufferedWriter.close ();
            bufferedWriter = null;
        }
    }

    // excludeList has catId's
    private ArrayList<CategoryInfo> removeExcludedCategories (ArrayList<CategoryInfo> allCategoryInfoList, String[] excludeCategoryList) {
        ArrayList<CategoryInfo> cleanCategoryInfoList;

        cleanCategoryInfoList = new ArrayList <CategoryInfo> ();
        for (CategoryInfo catInfoInFeed : allCategoryInfoList) {
            if (isExcluded (catInfoInFeed.getCatId(), excludeCategoryList) == false)
                cleanCategoryInfoList.add (catInfoInFeed);
        }

        return cleanCategoryInfoList;
    }

    private boolean isExcluded (String catId, String[] excludeCategoryList) {
        for (int i = 0; i < excludeCategoryList.length; i++) {
            if (catId.equals (excludeCategoryList [i]) == true)
                return true;
        }

        return false;
    }

    private ArrayList <CategoryAndRefinements> prepareAllCategoryAndRefinementsList (ArrayList<CategoryInfo> allCleanCategoryInfoList, 
                                                                                     String[] views, String[] segments, 
                                                                                     ArrayList<CategoryLogInfo> allCatLogInfoList,
                                                                                     String[] excludeProducts,
                                                                                     CategoryQueryExecutor queryExecutor) throws Exception {
        ArrayList<CategoryAndRefinements> allCategoryAndRefinementsList;

        allCategoryAndRefinementsList = new ArrayList <CategoryAndRefinements> ();  // contains primaryCatInfo -> refinedCatInfo list

        // go thru all catInfo from the feed and build primary->refinements list for "MAX" number of primaryCategories
        for (int v = 0; v < views.length; v++) {
            for (int s = 0; s < segments.length; s++) {
                // try to generate MAX number of catelogQuery entries for thie view+segment combination
                for (int c = 0; c < GeneratorConstants.MAX_CATEGORY_QUERY_COUNT; c++) {
                    CategoryLogInfo primaryCatLogInfo;

                    primaryCatLogInfo = selectPrimaryCategory (allCleanCategoryInfoList, views [v], segments [s],
                                                               allCatLogInfoList, excludeProducts, queryExecutor);
                    if (primaryCatLogInfo != null) {
                        CategoryAndRefinements categoryAndRefinements;

                        MessageLogger.logDebug ("CategoryQuery generating... primaryCatId = " + primaryCatLogInfo.getCategoryInfo().getCatId () + 
                                            ", view = " + views [v] + ", segment = " + segments [s]);

                        // for the given primaryCat, build refinements for it, for given view + segment combination 
                        categoryAndRefinements = prepareOneCategoryAndRefinements (primaryCatLogInfo, views [v], segments [s], 
                                                                                   allCleanCategoryInfoList, allCatLogInfoList,
                                                                                   excludeProducts, queryExecutor);
                        if (categoryAndRefinements != null) {
                            MessageLogger.logDebug ("CategoryQuery generated, primaryCatId = " + primaryCatLogInfo.getCategoryInfo().getCatId () + 
                                                ", view = " + views [v] + ", segment = " + segments [s]);

                            // add to the entire list of categoryAndRefinementsList if not already in that list
                            allCategoryAndRefinementsList.add (categoryAndRefinements);
                        } // else warning already issued 
                    } else {
                        // for the given view+segment combination, we cann't even generate another primaryCat; exit the inner loop
                        MessageLogger.logWarning ("CategoryQuery, cannot find primaryCategory for view = " + views[v] + ", segment = " + segments[s]);
                        break;
                    }
                }
            }
        }

        MessageLogger.logInfo ("CategoryToQueryPid. Number of entries: " + allCategoryAndRefinementsList.size());
        return allCategoryAndRefinementsList;  
    }

    // PRIMARY
    private CategoryLogInfo selectPrimaryCategory (ArrayList <CategoryInfo> allCleanCategoryInfoList, String view, String segment, 
                                                   ArrayList <CategoryLogInfo> allCatLogInfoList, String[] excludeProducts,
                                                   CategoryQueryExecutor queryExecutor) throws Exception {
        String segmentFq;
        CategoryLogInfo primaryCatLogInfo = null;

        segmentFq = this.inputData.getSegmentFq (segment);
        for (int i = 0; i < allCleanCategoryInfoList.size(); i++) {
            int indx;
            CategoryInfo primaryCatInfo;
            SearchQueryResponseInfo primaryQueryResponseInfo;

            indx = (int) (Math.random () * allCleanCategoryInfoList.size ());
            primaryCatInfo = allCleanCategoryInfoList.get (indx);   // at random, pick one category as 'primary'
            
            primaryCatLogInfo = new CategoryLogInfo (primaryCatInfo, true, view, segment);
            if (allCatLogInfoList.contains (primaryCatLogInfo) == true) 
                continue;    // this 'primary' catInfo has already been logged. It happens if randomly a previous entry is again picked
 
            primaryQueryResponseInfo = queryExecutor.getQueryResponseInfo (primaryCatInfo.getCatId(), view, segment, segmentFq);
            if (primaryQueryResponseInfo == null) {
                MessageLogger.logDebug ("CategoryQueryResponse is null"); 
                continue;
            }

            // remove any 'excludedPid' in APIresponse if any. Adjusts its internal data as needed
            primaryQueryResponseInfo.removeExcludedPidIfInResponse (excludeProducts);
            // check if we have sufficient #pids...
            if (primaryQueryResponseInfo.getNumFound () < GeneratorConstants.MIN_REQUIRED_NUM_FOUND) {
                MessageLogger.logDebug ("CategoryQueryToPid Insufficient number of products: primaryCatId " + primaryCatInfo.getCatId() + 
                                        ", view = " + view + ", segment = " + segment + ", fq = " + segmentFq + 
                                        ", numFound = " + primaryQueryResponseInfo.getNumFound ());
                continue; 
            }

            // this logInfo is added to 'allLogInfo' only after we find refined entries for it as well
            primaryCatLogInfo.setQueryResponseInfo (primaryQueryResponseInfo);
            break;
        } 
        return primaryCatLogInfo;            
    }

    private CategoryAndRefinements prepareOneCategoryAndRefinements (CategoryLogInfo primaryCatLogInfo, String view, String segment, 
                                                                     ArrayList<CategoryInfo> allCleanCategoryInfoList, 
                                                                     ArrayList<CategoryLogInfo> allCatLogInfoList,
                                                                     String[] excludeProducts,
                                                                     CategoryQueryExecutor queryExecutor) throws Exception {
        ArrayList<CategoryLogInfo> refinementsLogInfo; 
        String segmentFq;
            
        segmentFq = this.inputData.getSegmentFq (segment);

        // REFINEMENTS
        refinementsLogInfo = new ArrayList <CategoryLogInfo> ();
        for (int i = 0; i < allCleanCategoryInfoList.size(); i++) {
            int indx;
            CategoryInfo catInfo;
            CategoryLogInfo catLogInfo;

            indx = (int) (Math.random () * allCleanCategoryInfoList.size ());
            catInfo = allCleanCategoryInfoList.get (indx);
            catLogInfo = new CategoryLogInfo (catInfo, false, view, segment);
            if ((catInfo.equals (primaryCatLogInfo.getCategoryInfo()) == false) && (refinementsLogInfo.contains (catLogInfo) == false)) {
                // see if this catInfo is in the catInfoLogList list
                if (allCatLogInfoList.contains (catLogInfo) == true) {
                    // use queryResponseInfo from the earlier entry - avoid another API call
                    indx = allCatLogInfoList.indexOf (catLogInfo);
                    catLogInfo = allCatLogInfoList.get (indx);
                } else {
                    SearchQueryResponseInfo queryResponseInfo;

                    queryResponseInfo = queryExecutor.getQueryResponseInfo (catInfo.getCatId(), view, segment, segmentFq);
                    if (queryResponseInfo == null) {
                        MessageLogger.logDebug ("CategoryQueryResponse is null"); 
                        continue;
                    } 

                    // remove any excludedPids in responseInfo
                    queryResponseInfo.removeExcludedPidIfInResponse (excludeProducts);
                    if (queryResponseInfo.getNumFound () < GeneratorConstants.MIN_REQUIRED_NUM_FOUND) {
                        MessageLogger.logDebug ("CategoryQueryToPid Insufficient number of products: refinementCatId " + catInfo.getCatId() + 
                                            ", view = " + view + ", segment = " + segment + ", fq = " + segmentFq + 
                                            ", numFound = " + queryResponseInfo.getNumFound ());
                        continue;   // look for another catInfo for refined-entry 
                    }
                    catLogInfo.setQueryResponseInfo (queryResponseInfo);
                }

                // add this catInfo as refinedCat for current primary
                refinementsLogInfo.add (catLogInfo);

                // if we have needed number of refined entries for this cat, return
                if (refinementsLogInfo.size () == GeneratorConstants.MAX_REFINEMENTS_PER_CATEGORY)
                    break;  // we got enough refinement entries for given primaryCatInfo
            }
        }

        if (refinementsLogInfo.size () == GeneratorConstants.MAX_REFINEMENTS_PER_CATEGORY) {
            CategoryAndRefinements categoryAndRefinements;
            ArrayList<CategoryInfo> refinements;

            // we have primary AND its refinements OK for given view and segment
            // add primary and all the refined logLists to alreadyLoggedList
            allCatLogInfoList.add (primaryCatLogInfo);

            refinements = new ArrayList <CategoryInfo> ();
            for (CategoryLogInfo aCatLogInfo : refinementsLogInfo) {
                if (allCatLogInfoList.contains (aCatLogInfo) == false)
                    allCatLogInfoList.add (aCatLogInfo);

                // also add CatInfo to refinements list. This list is included in CategoryAndRefinements
                refinements.add (aCatLogInfo.getCategoryInfo());
            }

            categoryAndRefinements = new CategoryAndRefinements (primaryCatLogInfo.getCategoryInfo(), refinements, view, segment); 
            return categoryAndRefinements;
        }

        // insufficient refinements for current primary
        MessageLogger.logWarning ("Cannot generate sufficient refinements for catId: " + primaryCatLogInfo.getCategoryInfo().getCatId ());
        return null;
    }

    private void verifyMinimumData (ArrayList<CategoryLogInfo> allCategoryLogInfo, String[] views, String[] segments) {
        for (int v = 0; v < views.length; v++) {
            for (int s = 0; s < segments.length; s++) {
                boolean exists;

                // see if we have a catLogInfo for this view+segment
                exists = lookupCatLogInfo (allCategoryLogInfo, views[v], segments[s]);
                if (exists == false) {
                    MessageLogger.logWarning ("No categoryQueryData generated for view = " + views [v] + ", segment = " + segments [s]);
                } 
            }
        } 
    }

    private boolean lookupCatLogInfo (ArrayList<CategoryLogInfo> allCategoryLogInfo, String view, String segment) {
        for (CategoryLogInfo catLogInfo : allCategoryLogInfo) {
            if ((catLogInfo.getView ().equals (view) == true) && (catLogInfo.getSegment ().equals (segment))) {
                return true;
            }
        }
        return false;
    }

    private void writeCategoryQueryPids (ArrayList<CategoryLogInfo> allCategoryLogInfo) throws Exception {
        for (CategoryLogInfo aLogInfo : allCategoryLogInfo) {
            SearchQueryResponseInfo queryResponseInfo;

            queryResponseInfo = aLogInfo.getQueryResponseInfo ();
            writeOneQueryPidMap (aLogInfo.getCategoryInfo (), aLogInfo.isPrimary(), aLogInfo.getView (), aLogInfo.getSegment (),
                                 queryResponseInfo.getNumFound (), queryResponseInfo.getQueryResponseDocs ()); 
        }
    }

    private void writeOneQueryPidMap (CategoryInfo catInfo, boolean isPrimary, String view, String segment,
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
 
        line = String.format ("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s%s\n",
                               id, catInfo.getCatName(), catInfo.getCatId(), catInfo.getCatPath(), numFound, primary, view, segment,
                               pidBuf.toString ());


        bufferedWriter.write (line);
        id = id + 1;
    }
}

