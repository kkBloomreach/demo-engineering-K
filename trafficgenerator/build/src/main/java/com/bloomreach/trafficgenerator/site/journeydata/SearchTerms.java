package com.bloomreach.trafficgenerator.site.journeydata;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journeydata.campaigns.CampaignRecord;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildSearchResultPagePixel;

public class SearchTerms {

    private final static int MAX_SHUFFLE_COUNT = 5; // shuffle the terms list these many times

    ArrayList <SearchTermWithRefinements> defaultSearchTermsWithRefinements;
    CampaignRecord activeCampaignRecord;   // may be null

    ArrayList <SearchTermWithRefinements> replicatedDefaultSearchTermsWithRefinements;   // search terms replicated in this list
    ArrayList <SearchTermWithRefinements> replicatedCampaignSearchTermsWithRefinements;   // campaign promoted-terms replicated in this list

    public SearchTerms () {
    }

    public void setActiveCampaignRecord (CampaignRecord campaignRecord) {
        activeCampaignRecord = campaignRecord;
    }

    // Load the predefined set of search terms with refinements
    public void doLoad (String srcFilePath) throws Exception {
        // load search-terms-with-refinements
        this.loadDefaultSearchTermsWithRefinements (srcFilePath);

        // load promoted terms from campaign record
        if (this.activeCampaignRecord != null) {
            this.loadCampaignSearchTerms ();
        } 
        return;
    }

    // returns actual search terms, w/o star effect
    public int getSearchTermsCount () {
        return (this.defaultSearchTermsWithRefinements.size ()); 
    }

    // returns actual search terms, w/o importance effect
    // this is used to build startUrlPool containing searchUrls
    public ArrayList<String> getAllSearchTerms () {
        ArrayList <String> retList = new ArrayList <String> ();

        for (SearchTermWithRefinements entry : this.defaultSearchTermsWithRefinements)
            retList.add (entry.getPrimary ());
        return retList;
    }

    // no prior-search-term in order to select the next search-term
    public SearchTermWithRefinements selectSearchTermAtRandom () {
        return selectSearchTermAtRandom (null);
    }

    // 'currentUrl' is used to make sure randomly-selected-search-term does not result in the same search_url 
    // Basically avoid a search-term-select from a page for THAT search-term
    public SearchTermWithRefinements selectSearchTermAtRandom (String currentUrl) {
        int randomIndex;
        SearchTermWithRefinements selectedTermWithRefinements;
        String selectedTermSearchPageUrl;
        ArrayList <SearchTermWithRefinements> effectiveList;

        // even in an active campaign, actual list of 'campaign products' may be zero
        if ((this.activeCampaignRecord != null) && (this.replicatedCampaignSearchTermsWithRefinements.size() > 0)) {
            double randomListSelectionIndex;

            // When in a campaign, that 'campaignSearchTerms' list is used 'more often' than the default list
            randomListSelectionIndex = Math.random ();
            if (randomListSelectionIndex < 0.7)
                effectiveList = this.replicatedCampaignSearchTermsWithRefinements;
            else
                effectiveList = this.replicatedDefaultSearchTermsWithRefinements;
        } else {
            effectiveList = this.replicatedDefaultSearchTermsWithRefinements;
        }

        randomIndex = (int) (Math.random () * effectiveList.size());
        selectedTermWithRefinements = effectiveList.get (randomIndex); 
        selectedTermSearchPageUrl = BuildSearchResultPagePixel.getSearchResultPageUrl (selectedTermWithRefinements.getPrimary ());
        if ((currentUrl != null) && (currentUrl.equals (selectedTermSearchPageUrl))) {
            randomIndex = (randomIndex + 1) % effectiveList.size ();
            selectedTermWithRefinements = effectiveList.get (randomIndex); 
            //debugging why pixel ref = url in some rare cases...
            //@@@ TEMP use logInfo. Change back to logDebug
            MessageLogger.logInfo(String.format ("SearchTerm selection avoid dups, randomIndex = %d, term1 = %s, term2 = %s, effectiveList size = %d",
                                                         randomIndex, 
                                                         effectiveList.get (randomIndex).getPrimary(),
                                                         selectedTermWithRefinements.getPrimary(),
                                                         effectiveList.size()));                     
        }

        return (selectedTermWithRefinements);
    }


    // lookup refinement searchTerm. If it happens to match the one in parameter, skip that
    public String selectRefinedSearchTermAtRandom (SearchTermWithRefinements primaryTermWithRefinements) {
        int randomIndex;
        String selectedTerm;
        ArrayList <String> refinements;
        int maxAttempts = 5;
        int attemptNum = 0;

        refinements = primaryTermWithRefinements.getRefinements ();
        randomIndex = (int) (Math.random () * refinements.size());
        selectedTerm = refinements.get (randomIndex); 
        while (primaryTermWithRefinements.getPrimary().equals (selectedTerm)) {
            // take the next searchTerm, round-robin
            randomIndex = ((randomIndex + 1) % refinements.size());
            selectedTerm = refinements.get (randomIndex); 
            if (attemptNum++ >= maxAttempts) {
                MessageLogger.logError (String.format ("Could not get a different refined query for prior term: %s", primaryTermWithRefinements.getPrimary ()));
                break;
            }
        }
        return (selectedTerm);
    }

    // INTERNAL METHODS
    // load from searchTermsWithRefinement.tsv.
    private void loadDefaultSearchTermsWithRefinements (String srcFilePath) throws Exception { 
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        boolean headerLine;

        this.defaultSearchTermsWithRefinements = new ArrayList <SearchTermWithRefinements> ();
        this.replicatedDefaultSearchTermsWithRefinements = new ArrayList <SearchTermWithRefinements> (); 

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
                    String primaryTerm;
                    int importance;
                    ArrayList<String> refinements;
                    SearchTermWithRefinements oneSearchTermWithRefinements;

                    primaryTerm = tokens [0];
                    importance = Integer.valueOf (tokens [1]); // 1->5
                    refinements = new ArrayList <String> ();
                    for (int i = 2; i < tokens.length; i++) {
                        refinements.add (tokens [i]);
                    }

                    oneSearchTermWithRefinements = new SearchTermWithRefinements (primaryTerm, importance, refinements);
                    // first add the term to 'default' list, 
                    this.defaultSearchTermsWithRefinements.add (oneSearchTermWithRefinements);
                    this.replicatedDefaultSearchTermsWithRefinements.add (oneSearchTermWithRefinements);

                    for (int i = 1; i < (importance*GeneratorConstants.SEARCHTERM_STAR_MULTIPLIER); i++) {
                        this.replicatedDefaultSearchTermsWithRefinements.add (oneSearchTermWithRefinements);
                    }
                }
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading searchTermsWithRefinements: " + e.getMessage ());
            throw new Exception (e.getMessage());
        }

        if (srcReader != null)
        {
            try {
                srcReader.close ();
            }
            catch (Exception e)
            {
                MessageLogger.logError ("Src reader close exception: " + e.getMessage ());
            }
        }

        // shuffle the replicated list
        shuffleList (this.replicatedDefaultSearchTermsWithRefinements);
    }

    // load from campaigns info
    // For campaigns, the 'refinement' list is list-of-all-campaign-search-terms
    private void loadCampaignSearchTerms () {
        ArrayList<String> campaignSearchTerms;
        SearchTermWithRefinements oneSearchTermWithRefinements;

        this.replicatedCampaignSearchTermsWithRefinements = new ArrayList <SearchTermWithRefinements> (); 

        campaignSearchTerms = activeCampaignRecord.getSearchTerms ();
        for (String inputTerm : campaignSearchTerms) {
            int replicationCount = 0;
            String actualTerm;

            // look for 'star' terms (  eg, *sofa). Currently max 3 stars are supported
            if (inputTerm.startsWith ("***")) {
                replicationCount = 3 * GeneratorConstants.SEARCHTERM_STAR_MULTIPLIER;
                actualTerm = inputTerm.substring (3);
            }
            else if (inputTerm.startsWith ("**")) {
                replicationCount = 2 * GeneratorConstants.SEARCHTERM_STAR_MULTIPLIER;
                actualTerm = inputTerm.substring (2);
            }
            else if (inputTerm.startsWith ("*")) {
                replicationCount = 1 * GeneratorConstants.SEARCHTERM_STAR_MULTIPLIER;
                actualTerm = inputTerm.substring (1);
            }
            else {
                replicationCount = 1;
                actualTerm = inputTerm;
            }

            // MessageLogger.logDebug ("Campaign search term: " + actualTerm);
            // Note - 'refinements' for campaign search term comprises the entire list of that campaign's promotion-terms
            oneSearchTermWithRefinements = new SearchTermWithRefinements (actualTerm, replicationCount, campaignSearchTerms);
            this.replicatedCampaignSearchTermsWithRefinements.add (oneSearchTermWithRefinements);
            for (int i = 1; i < replicationCount; i++) {
                this.replicatedCampaignSearchTermsWithRefinements.add (oneSearchTermWithRefinements);
            }
        }

        // shuffle the replicated list
        shuffleList (this.replicatedCampaignSearchTermsWithRefinements);
    }

    private void shuffleList (ArrayList <SearchTermWithRefinements> listToShuffle) {
        // now shuffle the term list so that star-terms are spread around in the list
        int listSize = listToShuffle.size();
        for (int shuffleCount = 0; shuffleCount < MAX_SHUFFLE_COUNT; shuffleCount++) {
            for (int num = 0; num < listSize; num++) {
                int indx1;
                int indx2;
                SearchTermWithRefinements temp;

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

/***********
//     // load from searchTerms.tsv
//     private void loadDefaultSearchTerms (String srcFilePath) throws Exception { 
//         File srcFile = null;
//         FileReader srcReader = null;
//         BufferedReader srcBufferedReader = null;
//         String srcLine;
//         boolean headerLine;
// 
//         this.searchTerms = new ArrayList <String> ();
//         this.replicatedSearchTerms = new ArrayList <String> ();
// 
//         try {       
//             srcFile = new File (srcFilePath);
//             srcReader = new FileReader (srcFile);
//             srcBufferedReader = new BufferedReader (srcReader);
// 
//             headerLine = true;
//             while ((srcLine = srcBufferedReader.readLine ()) != null) {
//                 String[] tokens;
// 
//                 // skip header line
//                 if (headerLine == true) {
//                     headerLine = false;
//                     continue;
//                 }
// 
//                 if (srcLine.length () == 0) // blank line
//                     continue;
// 
//                 tokens = srcLine.split ("\t");
//                 // expected tokens: search_term
//                 if ((tokens != null) && (tokens.length == 1)) {
//                     // column 0: search term
//                     if ((tokens[0] != null) && (tokens[0].length() > 0)) {
//                         String inputTerm = tokens [0];
//                         int replicationCount = 0;
//                         String actualTerm;
// 
//                         // look for 'star' terms (  eg, *sofa). Currently max 3 stars are supported
//                         if (inputTerm.startsWith ("***")) {
//                             replicationCount = 3 * GeneratorConstants.SEARCHTERM_STAR_MULTIPLIER;
//                             actualTerm = inputTerm.substring (3);
//                         }
//                         else if (inputTerm.startsWith ("**")) {
//                             replicationCount = 2 * GeneratorConstants.SEARCHTERM_STAR_MULTIPLIER;
//                             actualTerm = inputTerm.substring (2);
//                         }
//                         else if (inputTerm.startsWith ("*")) {
//                             replicationCount = 1 * GeneratorConstants.SEARCHTERM_STAR_MULTIPLIER;
//                             actualTerm = inputTerm.substring (1);
//                         }
//                         else {
//                             replicationCount = 0;
//                             actualTerm = inputTerm;
//                         }
// 
//                         // first add the term to 'default' list, 
//                         this.searchTerms.add (actualTerm);
//                         this.replicatedSearchTerms.add (actualTerm);
// 
//                         if (replicationCount > 0) {
//                             for (int i = 0; i < replicationCount; i++) {
//                                 this.replicatedSearchTerms.add (actualTerm);
//                             }
//                         }
// 
//                         // MessageLogger.logDebug ("Search term: " + actualTerm);
//                     }
//                 }
//             }
//             srcReader.close ();
//             srcBufferedReader.close ();
//         } catch (Exception e) {
//             MessageLogger.logError ("Exception reading UiSegmentMap: " + e.getMessage ());
//         }
// 
//         if (srcReader != null)
//         {
//             try {
//                 srcReader.close ();
//             }
//             catch (Exception e)
//             {
//                 MessageLogger.logError ("Src reader close exception: " + e.getMessage ());
//             }
//         }
// 
//         // shuffle the replicated list
//         shuffleList (this.replicatedSearchTerms);
//     }
// 
// 
//     // When in a campaign, that list is used 'more often' than the default list
//     // The "prior" is used to make sure we don't select the same term that was used immediately prior
//     // Basically avoid a search-term-select from a page for THAT search-term
//     public SearchTermWithRefinements selectSearchTermAtRandom_WITHTERM (String priorSearchTerm) {
//         int randomIndex;
//         SearchTermWithRefinements selectedTermWithRefinements;
//         String selectedTerm;
//         ArrayList <SearchTermWithRefinements> effectiveList;
// 
//         if (this.activeCampaignRecord != null) {
//             double randomListSelectionIndex;
// 
//             randomListSelectionIndex = Math.random ();
//             if (randomListSelectionIndex < 0.7)
//                 effectiveList = this.replicatedCampaignSearchTermsWithRefinements;
//             else
//                 effectiveList = this.replicatedDefaultSearchTermsWithRefinements;
//         } else {
//             effectiveList = this.replicatedDefaultSearchTermsWithRefinements;
//         }
// 
//         randomIndex = (int) (Math.random () * effectiveList.size());
//         selectedTermWithRefinements = effectiveList.get (randomIndex); 
//         if ((priorSearchTerm != null) && (priorSearchTerm.equals (selectedTermWithRefinements.getPrimary()))) {
//             randomIndex = (randomIndex + 1) % effectiveList.size ();
//             selectedTermWithRefinements = effectiveList.get (randomIndex); 
//         }
// 
//         return (selectedTermWithRefinements);
//     }
****/

