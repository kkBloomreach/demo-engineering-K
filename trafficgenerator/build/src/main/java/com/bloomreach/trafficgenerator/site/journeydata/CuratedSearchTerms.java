// CuratedSearchTerms are mainly for the purpose of consistent-demos, particularly
// for 1:1 personalization. Therefore, additional features suchas 'campaigns'
// are excluded from these curated
package com.bloomreach.trafficgenerator.site.journeydata;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildSearchResultPagePixel;

public class CuratedSearchTerms {

    private final static int MAX_SHUFFLE_COUNT = 5; // shuffle the terms list these many times

    ArrayList <CuratedSearchTermDetails> defaultCuratedSearchTermDetails;
    ArrayList <CuratedSearchTermDetails> replicatedDefaultCuratedSearchTermDetails;   // search terms replicated in this list

    public CuratedSearchTerms () {
    }

    public void setActiveCampaignRecord (CampaignRecord campaignRecord) {
        // UNUSED
        // campaigns are not supported when generating curated-search-traffic
    }

    // Load the predefined set of search terms with refinements
    public void doLoad (String srcFilePath) throws Exception {
        // load search-terms-with-refinements
        this.loadDefaultCuratedSearchTermDetails (srcFilePath);
    }

    // returns actual search terms, w/o star effect
    public int getSearchTermsCount () {
        return (this.defaultCuratedSearchTermDetails.size ()); 
    }

    // returns uniq list of initialQueries. Note that in 
    // curatedSearchTerm list, since initialQuery is followed by
    // multiple potential 'refined' queries
    // this is used to build startUrlPool containing searchUrls
    public ArrayList<String> getAllSearchTerms () {
        ArrayList <String> retList = new ArrayList <String> ();

        for (CuratedSearchTermDetails entry : this.defaultCuratedSearchTermDetails)
            if (retList.contains (entry.getInitialQuery()) == false)
                retList.add (entry.getInitialQuery ());
        return retList;
    }

    // no prior-search-term in order to select the next search-term
    public CuratedSearchTermDetails selectSearchTermAtRandom () {
        return selectSearchTermAtRandom (null);
    }

    // 'currentUrl' is used to make sure randomly-selected-search-term does not result in the same search_url 
    // Basically avoid a search-term-select from a page for THAT search-term
    public CuratedSearchTermDetails selectSearchTermAtRandom (String currentUrl) {
        int randomIndex;
        CuratedSearchTermDetails selectedCuratedSearchTermDetails;
        String selectedTermSearchPageUrl;
        ArrayList <CuratedSearchTermDetails> effectiveList;

        effectiveList = this.replicatedDefaultCuratedSearchTermDetails;

        randomIndex = (int) (Math.random () * effectiveList.size());
        selectedCuratedSearchTermDetails = effectiveList.get (randomIndex); 
        selectedTermSearchPageUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedCuratedSearchTermDetails.getInitialQuery ());
        if ((currentUrl != null) && (currentUrl.equals (selectedTermSearchPageUrl))) {
            randomIndex = (randomIndex + 1) % effectiveList.size ();
            selectedCuratedSearchTermDetails = effectiveList.get (randomIndex); 
            //debugging why pixel ref = url in some rare cases...
            //@@@ TEMP use logInfo. Change back to logDebug
            MessageLogger.logInfo(String.format ("SearchTerm selection avoid dups, randomIndex = %d, term1 = %s, term2 = %s, effectiveList size = %d",
                                                         randomIndex, 
                                                         effectiveList.get (randomIndex).getInitialQuery (),
                                                         selectedCuratedSearchTermDetails.getInitialQuery (),
                                                         effectiveList.size()));                     
        }

        return (selectedCuratedSearchTermDetails);
    }


    // lookup refinement searchTerm. If it happens to match the one in parameter, skip that
    public String selectRefinedSearchTermAtRandom (CuratedSearchTermDetails initialCuratedSearchTermDetails) {
        return (initialCuratedSearchTermDetails.getRefinedQuery());
    }

    public String selectCuratedPid (CuratedSearchTermDetails initialCuratedSearchTermDetails) {
        ArrayList<String> pidList;
        int randomIndx;

        pidList = initialCuratedSearchTermDetails.getPidList ();
        randomIndx = (int) (Math.random () * (pidList.size ()));
        return (pidList.get (randomIndx));
    }

    // for a initialQuery, once a pid is selected, get a co-view / co-bought pid
    public String selectCuratedCoviewPid (CuratedSearchTermDetails initialCuratedSearchTermDetails, String selectedPid) {
        ArrayList<String> pidList;
        int randomIndx;
        String coviewPid;

        pidList = initialCuratedSearchTermDetails.getPidList ();
        randomIndx = (int) (Math.random () * pidList.size ());
        coviewPid = pidList.get (randomIndx);
        if (coviewPid.equals (selectedPid)) {
            // try N attempts to find a pid not-equal-to selected pid
            for (int attempts = 0; attempts < 5; attempts++) {
                // find some other
                randomIndx = (int) (Math.random () * pidList.size());
                coviewPid = pidList.get (randomIndx);
                if (coviewPid.equals (selectedPid) == false)
                    break;
            }
        }
        return coviewPid;
    }

    // INTERNAL METHODS
    // load from searchTermsWithRefinement.tsv.
    private void loadDefaultCuratedSearchTermDetails (String srcFilePath) throws Exception { 
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        boolean headerLine;

        this.defaultCuratedSearchTermDetails = new ArrayList <CuratedSearchTermDetails> ();
        this.replicatedDefaultCuratedSearchTermDetails = new ArrayList <CuratedSearchTermDetails> (); 

        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            headerLine = true;
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;

                if (srcLine.trim().length() == 0)
                    continue;   // blank line

                // skip header line
                if (headerLine == true) {
                    headerLine = false;
                    continue;
                }

                tokens = srcLine.split ("\t");
                // column 0: primary search term
                // 1: importance
                // 2... : refined terms
                if ((tokens != null) && (tokens.length > 2 )) {
                    String initialQuery;
                    String refinedQuery;
                    int score;
                    int numFound; // not used
                    int importance = 1; // CuratedList - all terms are equally 'important'
                    String[] curatedPids;
                    ArrayList<String> pidList;
                    CuratedSearchTermDetails oneCuratedSearchTermDetails;

                    initialQuery = tokens [0];
                    refinedQuery = tokens [1];
                    score = Integer.parseInt (tokens[2]);
                    numFound = Integer.parseInt (tokens [3]);
                    curatedPids = tokens [4].split (",");
                    pidList = new ArrayList <String> ();
                    for (int i = 0; i < curatedPids.length; i++) {
                        pidList.add (curatedPids [i]);
                    }

                    oneCuratedSearchTermDetails = new CuratedSearchTermDetails (initialQuery, refinedQuery, score, pidList);
                    // first add the term to 'default' list, 
                    this.defaultCuratedSearchTermDetails.add (oneCuratedSearchTermDetails);
                    this.replicatedDefaultCuratedSearchTermDetails.add (oneCuratedSearchTermDetails);

                    /* All CuratedTerms are equally important
                    for (int i = 1; i < (importance*GeneratorConstants.SEARCHTERM_STAR_MULTIPLIER); i++) {
                        this.replicatedDefaultCuratedSearchTermDetails.add (oneCuratedSearchTermDetails);
                    }
                    */
                }
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading curatedSearchTerms: " + e.getMessage ());
            throw new Exception (e.getMessage());
        }

        if (srcReader != null) {
            try {
                srcReader.close ();
            }
            catch (Exception e)
            {
                MessageLogger.logError ("Src reader close exception: " + e.getMessage ());
            }
        }

        // shuffle the replicated list
        shuffleList (this.replicatedDefaultCuratedSearchTermDetails);
    }

    // we could use Collections.shuffle() method instead --)
    private void shuffleList (ArrayList <CuratedSearchTermDetails> listToShuffle) {
        // now shuffle the term list so that star-terms are spread around in the list
        int listSize = listToShuffle.size();
        for (int shuffleCount = 0; shuffleCount < MAX_SHUFFLE_COUNT; shuffleCount++) {
            for (int num = 0; num < listSize; num++) {
                int indx1;
                int indx2;
                CuratedSearchTermDetails temp;

                indx1 = (int) (Math.random () * listSize);
                indx2 = (int) (Math.random () * listSize);
                if (indx1 != indx2) {
                    temp = listToShuffle.get (indx1);
                    listToShuffle.set (indx1, listToShuffle.get (indx2));
                    listToShuffle.set (indx2, temp);
                }
            }
        }

    }
}

