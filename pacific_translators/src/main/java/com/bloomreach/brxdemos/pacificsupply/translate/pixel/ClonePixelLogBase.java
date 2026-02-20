package com.bloomreach.brxdemos.pacificsupply.translate.pixel;

// abstract base class for all clonePixel classes
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.PixelLogParam;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;

public abstract class ClonePixelLogBase {

    protected ClonePixelLogBase () {
    }

    // common fields to be cloned in ALL pixels
    protected int cloneCommonFields (PixelLog.Builder pixelLogBuilder, UidToViewIdMap uidViewIdMap, ProcessedFeed processedFeed) {

        // domain
        pixelLogBuilder.setDomain (ClonePixelConstants.PACIFICSUPPLY_DOMAIN);

        // acct id
        pixelLogBuilder.setAcctId (ClonePixelConstants.PACIFICSUPPLY_ACCOUNT_ID);

        // ref_url
        pixelLogBuilder.setRefUrl (ClonePixelConstants.HOMEPAGE_URL);

        // param acctId
        replacePixelLogParam (pixelLogBuilder, "acct_id", ClonePixelConstants.PACIFICSUPPLY_ACCOUNT_ID);

        // param ref_url
        replacePixelLogParam (pixelLogBuilder, "ref", ClonePixelConstants.HOMEPAGE_URL);

        // remove param domain_key
        removePixelLogParam (pixelLogBuilder, "domain_key");

        // if there is a df_ param, remove that param as well
        if (doesPixelLogParamExist (pixelLogBuilder, "df_domain_key") == true) {
            removePixelLogParam (pixelLogBuilder, "df_domain_key");
        }

        // update title if it contains "onesource"
        String srcTitle = pixelLogBuilder.getTitle ();
        if (StringUtils.isNotEmpty (srcTitle)) {
            srcTitle = srcTitle.toLowerCase ();
            if (srcTitle.indexOf ("onesource") >= 0) {
                pixelLogBuilder.setTitle (ClonePixelConstants.PACIFICSUPPLY_DEFAULT_TITLE);
                replacePixelLogParam (pixelLogBuilder, "title", ClonePixelConstants.PACIFICSUPPLY_DEFAULT_TITLE);
            }
        }

        // use ui->viewId map to get view_id associated with this uid
        String uid = pixelLogBuilder.getUid ();
        String viewId = lookupViewId (uid, uidViewIdMap);
        addPixelLogParam (pixelLogBuilder, "view_id", viewId);

        return (ClonePixelConstants.CLONE_STATUS_OK);
    }

    /**
     * Add PixelLogParam corresponding to the passed parameter key-value pair to the pixel builder.
     * @param pixelBuilder
     * @param paramKey
     * @param paramValue
     */
    protected void addPixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey, String paramValue) {

        PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
        paramBuilder.setKey(paramKey);
        paramBuilder.setValue(paramValue);
        pixelLogBuilder.addParams(paramBuilder.build());
    }

    // first obtain entire list of params in the pixel. Then look for specified key's value
    // Return null if not found
    protected String getPixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLog.PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                return (aParam.getValue ());
            }
        }

        return (null);
    }

    // replace an existing parameter's value
    // first obtain entire list of params in the pixel. Then look for specified key's index 
    // and use that to replace value. Looks like the "setParams" method does not have a
    // overloaded method where it takes a paramKey as an argument
    protected boolean replacePixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey, String paramValue) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                PixelLogParam.Builder paramBuilder = PixelLogParam.newBuilder();
                paramBuilder.setKey(paramKey);
                paramBuilder.setValue(paramValue);
                PixelLogParam newPixelLogParam = paramBuilder.build();

                pixelLogBuilder.setParams(i, newPixelLogParam);

                return (true);
            }
        }

        // specified paramKey not found in current paramsList
        return (false);
    }

    // remove an existing parameter
    // first obtain entire list of params in the pixel. Then look for specified key's index 
    // and use that to remove it. Looks like the "removeParams" method does not have a
    // overloaded method where it takes a paramKey as an argument
    protected boolean removePixelLogParam (PixelLog.Builder pixelLogBuilder, String paramKey) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                pixelLogBuilder.removeParams(i);
                return (true);
            }
        }

        // specified paramKey not found in current paramsList
        return (false);
    }

    // check if given paramKey exists. This is needed for params such as "df_domain_key"
    // which exist only if it contains "df_*" params
    protected boolean doesPixelLogParamExist (PixelLog.Builder pixelLogBuilder, String paramKey) {
        List<PixelLog.PixelLogParam> paramsList;

        paramsList = pixelLogBuilder.getParamsList ();
        for (int i = 0; i < paramsList.size (); i++) {
            PixelLogParam aParam;

            aParam = paramsList.get (i);
            if (aParam.getKey ().equals (paramKey)) {
                return (true);
            }
        }

        // specified paramKey not found in current paramsList
        return (false);
    }

    // generate cloned productId in exactly the same way it is done when feed is preProcessed
    protected String generateUniqPid  (String srcPid) {

        int pidIntValue;
        int generatedPid;

        try {
            pidIntValue = Integer.parseInt (srcPid);
            generatedPid = pidIntValue + ClonePixelConstants.FIXED_OFFSET_FOR_PID;
            return (Integer.toString (generatedPid));
        } catch (NumberFormatException nfe) {
            System.out.println ("Bad pid: " + srcPid);
        }
    
        return (null);
    }

    protected String generateProductUrl (String newPid) {
        return (ClonePixelConstants.PRODUCT_URL_PREFIX  + newPid);
    }

    protected String generateSearchPageUrl (String queryTerm) {
        return (ClonePixelConstants.SEARCH_PAGE_URL_PREFIX + queryTerm);
    }

    // given a uid in the pixel, determine viewId associated with that uid
    // Original source pixel don't have any viewIds. However API logs have them (mostly)
    // Thos API logs are pre-processed to generate a Ui->viewId map. Use that
    // map to get corresponding viewId. In some cases, even the apis don't have
    // a view_id for some uid's. In such cases, apply a simple heuristic to generate
    // view_id for a given uid
    private String lookupViewId (String uid, UidToViewIdMap uidViewIdMap) {
        String viewId;
        viewId = uidViewIdMap.lookupViewId (uid);

        if (viewId == null) {
            int uidIntValue;

            // mod the view_id by total# of views. Use the mod value
            // to index in the VIEW_ID_LIST
            int viewCount = ClonePixelConstants.VIEW_ID_LIST.length;

            try {
                uidIntValue = (int) Long.parseLong (uid);
                if (uidIntValue < 0)
                    uidIntValue = -uidIntValue;
            } catch (NumberFormatException nfe) {
                System.out.println ("cannot parse long UID value: "  + uid);
                uidIntValue = 0;
            }

            int indx = uidIntValue % viewCount;
            if (ClonePixelConstants.VIEW_ID_LIST [indx].equals ("master") == true)
                indx = indx - 1; // this assumes "master" is not the first entry in the VIEW_ID array

            viewId = ClonePixelConstants.VIEW_ID_LIST [indx];
        }

        return (viewId);
    }
}
