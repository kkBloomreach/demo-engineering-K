// Forced to exit a session, eg, in category-search, if selected category is set as 'low-perf'
// exit the session more often so that its analytics data shows 'poor performance'
package com.bloomreach.trafficgenerator.site.journey;

public class ForcedSessionExitException extends Exception {
    public ForcedSessionExitException (String message) {
        super(message);
    }

}
