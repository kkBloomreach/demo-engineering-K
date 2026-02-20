package com.bloomreach.trafficgenerator.site.user;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.visitor.Visitor;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;
import com.bloomreach.trafficgenerator.MessageLogger;

public class UserManager {

    private final static String USER_ID_PREAMBLE = "tg1";
    private String specialVisitorId;

    public UserManager () {
    }

    public void setSpecialVisitorId (String specialVisitorId) {
        this.specialVisitorId = specialVisitorId;
    }

    public UserRecord login (Visitor visitor) {
        UserRecord userRecord;
        String visitorId;
        String user_id;
        String userSegment;
        String userView;

        visitorId = visitor.getVisitorId();
        // for a special visitor, we keep user_id == visitor_id.
        // this is because the same id is then used in SPA to display past-purchase widget results
        if ((this.specialVisitorId != null) && (visitorId.equals (this.specialVisitorId)))
            user_id = this.specialVisitorId;
        else
            user_id = constructUserId (visitorId);

        userSegment = getUserSegmentAtRandom (visitorId);
        userView = getUserView (visitorId);

        userRecord = new UserRecord ();
        userRecord.setVisitorId (visitorId);
        userRecord.setUserId (user_id);
        userRecord.setSegment (userSegment);
        userRecord.setView (userView);
        userRecord.setDeviceType (visitor.getDeviceType());

        return (userRecord);
    }

    // during a user session, internally we change user segment (RTS)
    // 20% of that user's sessions. Assumption is, a typical visitor MAY view/purchase/...
    // a 'high-value' product once-in-a-while even though they are in a 'low-value' segment
    // (OR, vice-versa). Such action would cause Engagement to reset that user's segment.
    // This is a static method and it is called ONLY at the beginning of a session
    public static void updateUserSegment (UserRecord userRecord) {
        String segmentationType;

        segmentationType = SiteConfig.getSegmentationType ();
        if ((segmentationType != null) && (segmentationType.equals (SiteConfig.SEGMENTATION_TYPE_RTS))) {
            int randomIndx;

            randomIndx = (int) (Math.random () * 10);
            if (randomIndx < 2) {
                String newSegment;
                ArrayList<String> segmentNames;

                segmentNames = SiteConfig.getRTSSegmentNames ();
                if (segmentNames != null) {
                    long visitorIdValue;
                    int segmentNum;

                    try {
                        visitorIdValue = Long.valueOf (userRecord.getVisitorId());
                    } catch (NumberFormatException nfe) {
                        MessageLogger.logWarning (String.format ("Visitor id must be numeric: %s", userRecord.getVisitorId()));
                        visitorIdValue = -1;
                    }

                    segmentNum = (int) (visitorIdValue % segmentNames.size());
                    newSegment = segmentNames.get (segmentNum);
                    userRecord.setSegment (newSegment);
                }
            }
        }
    }

    public void logout (UserRecord userRecord) {
        // currently does nothing
    }

    // 'user_id' is different from visitor->visitorId
    // Following is some algo to generate user_id
    // For a given visitor_id, corresponding user_id must always remain same
    private String constructUserId (String visitorId) {
        String userId;
        int strLenHalf;
        String visitorIdHead;
        String visitorIdTail;
        String acctId;

        strLenHalf = (int) (visitorId.length()/2);
        visitorIdHead = visitorId.substring (0, strLenHalf);
        visitorIdTail = visitorId.substring (strLenHalf);
        acctId = SiteConfig.getAccountConfigParam ("ACCOUNT_ID");

        userId = String.format ("%s-%s-%s-%s", USER_ID_PREAMBLE, acctId, visitorIdTail, visitorIdHead);
        return userId;
    }

    // segmentNum = visitorId mod total_segments/total_profiles; 
    // segment = total_segments [segmentNum]
    private String getUserSegmentAtRandom (String visitorId) {
        String segmentationType;
        ArrayList <String> segmentNames;
        int segmentNum;
        String userSegment = "NONE";

        segmentationType = SiteConfig.getSegmentationType ();
        switch (segmentationType) {
            case SiteConfig.SEGMENTATION_TYPE_RTS:
                segmentNames = SiteConfig.getRTSSegmentNames ();
                break;         
            case SiteConfig.SEGMENTATION_TYPE_RBS:
                segmentNames = SiteConfig.getRBSCustomerProfileNames ();
                break;         
            default:
                segmentNames = null;    // site has neither RTS or RBS
        }

        if (segmentNames != null) {
            long visitorIdValue;

            try {
                visitorIdValue = Long.valueOf (visitorId);
            } catch (NumberFormatException nfe) {
                MessageLogger.logWarning (String.format ("Visitor id must be numeric: %s", visitorId));
                visitorIdValue = -1;
            }

            segmentNum = (int) (visitorIdValue % segmentNames.size());
            userSegment = segmentNames.get (segmentNum);
        }

        return (userSegment);
    }

    // view
    private String getUserView (String visitorId) {
        ArrayList <String> views;
        String userView = "NONE";   // default

        views = SiteConfig.getViews ();
        if ((views != null) && (views.size() > 0)) {
            long visitorIdValue;
            int indx;

            try {
                visitorIdValue = Long.valueOf (visitorId);
            } catch (NumberFormatException nfe) {
                MessageLogger.logWarning (String.format ("Visitor id must be numeric: %s", visitorId));
                visitorIdValue = -1;
            }

            indx = (int) (visitorIdValue % views.size());
            userView = views.get (indx);
        }

        return (userView);
    }
}
