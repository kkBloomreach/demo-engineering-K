package com.bloomreach.trafficgenerator.test;

import java.util.ArrayList;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;

public class TestSiteConfig {

    // private static String TEST_PID = "15489"; // some test pid
    // private static String TEST_PID = "68303"; // some test pid
    private static String TEST_PID = "KIT0703"; // some test pid

    // Param: source filename 
    public static void main(String[] args) {
        String value;
        ArrayList<String> valueList;

        if ((args.length < 1)) {
            System.err.println ("Usage:TestSiteConfig <config_filename>");
            return;
        }
 
        SiteConfig config = new SiteConfig ();
        try {
            if (config.load (args [0]) == false) {
                System.err.println ("SiteConfig load failed");
                return;
            }
            System.out.println ("SiteConfig load successful");
        }
        catch (Exception e) {
            System.err.println ("Exception in processing siteconfig : " + e.getMessage ());
            return;
        }

        try {
            value = SiteConfig.getAccountConfigParam ("ACCOUNT_ID");
            if (value == null)
                System.err.println ("Cannot find param value: " + "ACCOUNT_ID");
            else
                System.out.println ("accountconfig value: " + value);
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            value = SiteConfig.getUrlConfigParam ("ATC_PAGE_URL");
            if (value == null)
                System.err.println ("Cannot find param value: " + "ATC_PAGE_URL");
            else
                System.out.println ("urlConfig value: " + value);
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            value = SiteConfig.getProductCatalogConfigParam ("ORIGINAL_CATALOG_PATH");
            if (value == null)
                System.err.println ("Cannot find param value: " + "ORIGINAL_CATALOG_PATH");
            else
                System.out.println ("productCatalogConfig value: " + value);
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            valueList = SiteConfig.getViews ();
            if ((valueList == null) || (valueList.size() == 0))
                System.err.println ("Cannot find views ");
            else
                System.out.println ("views count: " + valueList.size());
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            valueList = SiteConfig.getExcludeProducts ();
            if ((valueList == null) || (valueList.size() == 0))
                System.err.println ("Cannot find excludedProducts");
            else
                System.out.println ("excludedProducts count: " + valueList.size());
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            valueList = SiteConfig.getExcludeCategoryIds ();
            if ((valueList == null) || (valueList.size() == 0))
                System.err.println ("Cannot find excludedCategories");
            else
                System.out.println ("excludedCategories count: " + valueList.size());
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            value = SiteConfig.getSegmentationType ();
            if (value == null)
                System.err.println ("Cannot find segmentation type");
            else
                System.out.println ("segmentation type: " + value);
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            valueList = SiteConfig.getRTSSegmentNames ();
            if ((valueList == null) || (valueList.size () == 0))
                System.err.println ("Cannot find RTS segments");
            else
                System.out.println ("RTS segments count: " + valueList.size());
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            value = SiteConfig.getRTSKeyValuePair ("BUDGET");
            if (value == null)
                System.err.println ("Cannot find RTS keyvalue pair");
            else
                System.out.println ("RTS keyvalue pair: " + value);
        } catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            valueList = SiteConfig.getRBSCustomerProfileNames ();
            if ((valueList == null) || (valueList.size () == 0))
                System.err.println ("Cannot find BSCustomerProfileNames");
            else
                System.out.println ("ProfileName count: " + valueList.size());
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }
}


