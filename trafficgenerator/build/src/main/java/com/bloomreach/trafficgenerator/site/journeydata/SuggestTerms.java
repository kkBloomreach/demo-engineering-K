package com.bloomreach.trafficgenerator.site.journeydata;

// Uid to segment map is generated ONCE and saved to local file
// That generated map is loaded in this class + associated lookup methods
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;

public class SuggestTerms {

    ArrayList <String> suggestTerms;

    public SuggestTerms () {
    }

    // Load the predefined set of search terms
    public void doLoad (String srcFilePath) throws Exception {
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        boolean headerLine;

        this.suggestTerms = new ArrayList <String> ();
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
                // expected tokens: search_term
                if ((tokens != null) && (tokens.length == 1)) {
                    // column 0: search term
                    if ((tokens[0] != null) && (tokens[0].length() > 0)) {
                        this.suggestTerms.add (tokens [0]);
                        // MessageLogger.logDebug ("Suggest term: " + tokens [0]);
                    }
                }
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading UiSegmentMap: " + e.getMessage ());
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
    }

    public int getSuggestTermsCount () {
        return (this.suggestTerms.size ()); 
    }

    public String selectSuggestTermAtRandom () {
        int randomIndex;
        String selectedTerm;

        randomIndex = (int) (Math.random () * this.suggestTerms.size());
        selectedTerm = this.suggestTerms.get (randomIndex); 
        return (selectedTerm);
    }
}

