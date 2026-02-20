package com.bloomreach.trafficgenerator.site.journeydata;

public class TrafficStepInfo {

    private int stepId;
    private String stepName;
    // example: value at [0] => probability of 0th step at the next step from current step
    // [0] = 10 => step0 has probabily 0.1
    private int[] nextStepProbabilities;    // array index == stepNum

    public TrafficStepInfo (int stepId, String stepName, int[] nextProbs) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.nextStepProbabilities = nextProbs;
    }

    public int getStepId () {
        return this.stepId;
    }

    public String getStepName () {
        return this.stepName;
    }

    public int[] getNextStepProbabilities () {
        return this.nextStepProbabilities;
    }

}

