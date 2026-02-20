package com.bloomreach.trafficgenerator.site.journey;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.EnvironmentConfig;

public abstract class StepBase {

    private String envType;

    protected StepBase () {
        envType = EnvironmentConfig.getEnvType ();  // get it once
    }

    // newUrl == null => url has NOT changed in this step
    // newUrl != null => url has changed in this step
    protected void setUrlHistory (StepResult prev, StepResult current, String newUrl) {
        if (newUrl != null) {
            current.setRefUrl (prev.getUrl ());
            current.setUrl (newUrl);
        } else {
            current.setRefUrl (prev.getRefUrl ());
            current.setUrl (prev.getUrl ());
        }

        if ((current.getUrl() == null) || (current.getRefUrl () == null)) {
            MessageLogger.logError ("Url or RefUrl is null in StepBase");
        }
    }

    // "simulate" real visitor session/steps in qa and release env
    protected void insertDuration (long duration) {
        // skip this in dev env
        if (this.envType.equals (EnvironmentConfig.ENV_TYPE_QA) || this.envType.equals (EnvironmentConfig.ENV_TYPE_RELEASE)) {
            try {
                Thread.currentThread().sleep (duration);
            } catch (InterruptedException ie) {
                MessageLogger.logWarning ("Step duration interrupted");
            }
        } else if (this.envType.equals (EnvironmentConfig.ENV_TYPE_DEV)) {
            try {
                    Thread.currentThread().sleep (150); // avoid BR's QPS limits
            } catch (InterruptedException ie) {
                    MessageLogger.logWarning ("Step duration interrupted");
            }
        }
    }
}

