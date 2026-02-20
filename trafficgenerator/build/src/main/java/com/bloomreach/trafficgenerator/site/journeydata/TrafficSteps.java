package com.bloomreach.trafficgenerator.site.journeydata;

// Uid to segment map is generated ONCE and saved to local file
// That generated map is loaded in this class + associated lookup methods
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;

public class TrafficSteps {

    private final static int SHUFFLED_ARRAY_LENGTH = 100;
    private final static int MAX_SHUFFLE_COUNT = 5; // shuffle the array these many times

    ArrayList <TrafficStepInfo> trafficSteps;
    private ArrayList <TrafficInfoWithShuffledNextSteps> shuffledNextStepsList;

    public TrafficSteps () {
    }

    // Load the predefined set of search terms
    public void doLoad (String srcFilePath) throws Exception {
        this.trafficSteps = null;

        try {
            loadTrafficSteps (srcFilePath);
        } catch (Exception e) {
            MessageLogger.logError (String.format ("Failed to load traffic steps, path = %s", srcFilePath));
            throw new Exception (e.getMessage());
        }

        // after loading, shuffle the nextSteps array for each trafficStep
        shuffleNextSteps ();
    }

    public int getTrafficStepsCount () {
        return (this.trafficSteps.size ()); 
    }

    public ArrayList<TrafficStepInfo> getAllTrafficSteps () {
        return (this.trafficSteps); 
    }

    // lookupStep -- NOT at-random; return the specific step for param stepId
    public TrafficStepInfo lookupStep (int stepId) {
        for (TrafficStepInfo stepInfo : this.trafficSteps) {
            if (stepInfo.getStepId () == stepId)
                return stepInfo;
        }

        MessageLogger.logError (String.format ("Unknown stepId %s", stepId));
        return null;
    }

    // given current-step, lookup nextStep at random, using probability info
    public TrafficStepInfo lookupNextStepAtRandom (TrafficStepInfo trafficStepInfo) {
        return (lookupNextStepAtRandom (trafficStepInfo.getStepId ()));
    }

    public TrafficStepInfo lookupNextStepAtRandom (int currentStepId) {
        TrafficInfoWithShuffledNextSteps currentStepInfo = null;
        TrafficInfoWithShuffledNextSteps nextStepInfo = null;
        int[] shuffledNextStepIds;
        int randomIndx;
        int nextStepId;
 
        // first lookup shuffledInfo for currentStepId
        for ( TrafficInfoWithShuffledNextSteps info : this.shuffledNextStepsList) {
            if (info.getStepInfo ().getStepId () == currentStepId) {
                currentStepInfo = info;
                break;
            }
        }

        if (currentStepInfo == null) {
            MessageLogger.logError (String.format ("Cannot find step info for stepId = %s", currentStepId));
            return (null);
        }

        // for the currentStepInfo, get its shuffled nextStepIdList
        shuffledNextStepIds = currentStepInfo.getShuffledNextStepIds ();    // length = SHUFFLED_ARRAY_LENGTH
        randomIndx = (int) (Math.random () * SHUFFLED_ARRAY_LENGTH);
        nextStepId = shuffledNextStepIds [randomIndx];

        // get stepInfo for randomIndx
        // first lookup shuffledInfo for currentStepId
        for ( TrafficInfoWithShuffledNextSteps info : this.shuffledNextStepsList) {
            if (info.getStepInfo ().getStepId () == nextStepId) {
                nextStepInfo = info;
                break;
            }
        }

        if (nextStepInfo == null) {
            MessageLogger.logError (String.format ("Cannot find next step info for stepId = %s", currentStepId));
            return (null);
        }

        MessageLogger.logDebug (String.format ("from step %s, next step %s", currentStepId, nextStepInfo.getStepInfo().getStepId()));
        return (nextStepInfo.getStepInfo ());
    }

    // INTERNAL METHODS
    private void loadTrafficSteps (String srcFilePath) throws Exception {
        File srcFile = null;
        FileReader srcReader = null;
        BufferedReader srcBufferedReader = null;
        String srcLine;
        boolean headerLine;

        this.trafficSteps = new ArrayList <TrafficStepInfo> ();
        try {       
            srcFile = new File (srcFilePath);
            srcReader = new FileReader (srcFile);
            srcBufferedReader = new BufferedReader (srcReader);

            headerLine = true;
            while ((srcLine = srcBufferedReader.readLine ()) != null) {
                String[] tokens;
                int stepId;
                int probCount;
                int[] probabilities;
                TrafficStepInfo trafficStepInfo;

                if (srcLine.trim().length() == 0)
                    continue;   // blank line

                // skip header line
                if (headerLine == true) {
                    headerLine = false;
                    continue;
                }

                tokens = srcLine.split ("\t");
                if ((tokens == null) || (tokens.length == 0))
                    continue;   // blank line

                // expected tokens:
                // stepId, stepName, input, 'result type', 'total', [list-of-ints] 
                // 'total' is just sum of probablities in each row, should add to 100
                stepId = Integer.parseInt (tokens [0]);
                probCount = tokens.length - 5;
                probabilities = new int [probCount];
                for (int i = 0; i < probCount; i++) {
                    probabilities [i] = Integer.parseInt (tokens [i + 5]);
                }

                trafficStepInfo = new TrafficStepInfo (stepId, tokens [1], probabilities);
                this.trafficSteps.add (trafficStepInfo);
            }
            srcReader.close ();
            srcBufferedReader.close ();
        } catch (Exception e) {
            MessageLogger.logError ("Exception reading traffic steps: " + e.getMessage ());
            this.trafficSteps = null;
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
    
    // for each traffic step, build a int[] (length = 100) using numbers in stepInfo
    // and then shuffle that int[]. That array is then used to randomly select
    // next step for a given 'current step'
    private void shuffleNextSteps () {
        this.shuffledNextStepsList = new ArrayList <TrafficInfoWithShuffledNextSteps> ();

        for (TrafficStepInfo trafficStepInfo : trafficSteps) {
            int[] nextStepProbabilities;
            int[] shuffledNextStepsArray = new int [SHUFFLED_ARRAY_LENGTH];
            int shuffleArrayIndx = 0;
            TrafficInfoWithShuffledNextSteps trafficInfoWithShuffledNextSteps;

            // first fill up shuffleArray with repeated value of next-step-id
            nextStepProbabilities = trafficStepInfo.getNextStepProbabilities (); // array of prob's for nextSteps (eg, 20, 30, 0,...)
            for (int nxtStepId = 0; nxtStepId < nextStepProbabilities.length; nxtStepId++) {
                int probValue;

                probValue = nextStepProbabilities [nxtStepId]; // value like 20, 30, 0, ...
                for (int val = 0; val < probValue; val++) {
                    shuffledNextStepsArray [shuffleArrayIndx++] = nxtStepId;    // repeat stepId as many times as 'val'
                }
            }

            // then shuffle those values 
            for (int shuffleCount = 0; shuffleCount < MAX_SHUFFLE_COUNT; shuffleCount++) {
                for (int num = 0; num < SHUFFLED_ARRAY_LENGTH; num++) {
                    int indx1;
                    int indx2;
                    int temp;

                    indx1 = (int) (Math.random () * SHUFFLED_ARRAY_LENGTH);
                    indx2 = (int) (Math.random () * SHUFFLED_ARRAY_LENGTH);
                    if (indx1 != indx2) {
                        temp = shuffledNextStepsArray [indx1];
                        shuffledNextStepsArray [indx1] = shuffledNextStepsArray [indx2];
                        shuffledNextStepsArray [indx2] = temp;
                    }
                }
            }

            trafficInfoWithShuffledNextSteps = new  TrafficInfoWithShuffledNextSteps (trafficStepInfo, shuffledNextStepsArray);
            this.shuffledNextStepsList.add (trafficInfoWithShuffledNextSteps);
        }
    }
 
    // shuffle each step's next-step-list based on probability info in trafficSteps data
    class TrafficInfoWithShuffledNextSteps {

        TrafficStepInfo stepInfo;
        int[] shuffledNextStepIds;

        TrafficInfoWithShuffledNextSteps (TrafficStepInfo stepInfo, int[] shuffledNextStepIds) {
            this.stepInfo = stepInfo;
            this.shuffledNextStepIds = shuffledNextStepIds; // array of 100 ints
        }

        TrafficStepInfo getStepInfo () {
            return this.stepInfo;
        }

        int[] getShuffledNextStepIds () {
            return this.shuffledNextStepIds;
        }
    }
}


