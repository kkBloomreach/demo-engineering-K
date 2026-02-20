package com.bloomreach.trafficgenerator.site;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;

public class SiteVisitorMonitor  {
    private Object mutex;
    private int activeVisitorCount;

    SiteVisitorMonitor () {
        activeVisitorCount = 0;
        mutex = new Object ();
    }

    public synchronized boolean canVisitorEnter () {
        synchronized (mutex) {
            if (activeVisitorCount < GeneratorConstants.MAX_CONCURRENT_SITE_VISITORS_ALLOWED) {
                return true;
            }
        }

        MessageLogger.logDebug (String.format ("Visitor ignored at time = %d, active visitor count = %d",
                                                    (int) (System.currentTimeMillis()/1000), activeVisitorCount));
        return false;
    }
    
    public synchronized void enterVisitor (String userId) {
        synchronized (mutex) {
            ++activeVisitorCount;
            MessageLogger.logDebug (String.format ("Visitor entered userId = %s at time = %d, active visitor count = %d",
                                                    userId, (int) (System.currentTimeMillis()/1000), activeVisitorCount));
        }
    }

    public synchronized void exitVisitor (String userId) {
        synchronized (mutex) {
            --activeVisitorCount;
            MessageLogger.logDebug (String.format ("Visitor exited userId = %s at time = %d, active visitor count = %d",
                                                    userId, (int) (System.currentTimeMillis()/1000), activeVisitorCount));
        }
    }
}

