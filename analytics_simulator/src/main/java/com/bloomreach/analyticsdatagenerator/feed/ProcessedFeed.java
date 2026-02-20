package com.bloomreach.analyticsdatagenerator.feed;

import java.util.ArrayList;
import com.bloomreach.analyticsdatagenerator.MessageLogger;

public abstract class ProcessedFeed {

    private final static int MAX_CATEGORY_DEPTH = 20;   // expected max category depth in feed

    private ArrayList <FeedRecord> parsedFeedRecordList;
    private ArrayList <CategoryInfo> allCategoryInfoList;

    // to be implemented by derived class
    public abstract void load (String productFilePath) throws Exception;

    // to be called by derived class upon successful parse
    protected void setParsedFeedRecordList (ArrayList<FeedRecord> parsedRecordList) {
        this.parsedFeedRecordList = parsedRecordList;

        // internally, immediately build a list of CategoryInfo objects. This is
        // needed later to prepare refUrl list and category-refinement list
        constructCategoryInfoList ();

        // debug list to see all available categories in the feed
        MessageLogger.logInfo ("\n--------\nDEBUG LIST of all categories in the feed:");
        for (CategoryInfo catInfo : allCategoryInfoList) {
            MessageLogger.logInfo (catInfo.getCatId () + "\t" + catInfo.getCatName());
        }
        MessageLogger.logInfo ("--------");


    }

    public boolean isProductInFeed (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (true); 
            }
        }

        return (false);
    }

    public String lookupProductPrice (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (aRecord.getProductPrice()); 
            }
        }

        return (null);
    }

    public String lookupProductName (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (aRecord.getProductName ()); 
            }
        }

        return (null);
    }

    public String lookupProductSkuId (String pid) {
        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                if (aRecord.getProductId ().equals (pid) == true)
                    return (aRecord.getProductSkuId ()); 
            }
        }

        return (null);
    }

    public ArrayList <CategoryInfo> getAllCategoryInfoList () {
        return allCategoryInfoList;
    }

    // deDup'd catInfo list
    private void constructCategoryInfoList () {
        allCategoryInfoList = new ArrayList <CategoryInfo> ();

        if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
            for (FeedRecord aRecord : parsedFeedRecordList) {
                ArrayList<CategoryInfo> catInfoList;

                // this product's category info. A product may be in multiple categories
                catInfoList = aRecord.getCategoryInfoList (); 
                for (CategoryInfo catInfo : catInfoList) {
                    if (allCategoryInfoList.contains (catInfo) == false) {
                        allCategoryInfoList.add (catInfo);
                    }
                }
            }
        }
    }
}


/**********
//     public ArrayList <CategoryInfo> listAllUniqCategories (int maxCount) {
//         ArrayList <CategoryInfo> retList = null;
// 
//         if (categoryInfoList
//         if ((parsedFeedRecordList != null) && (parsedFeedRecordList.size() > 0)) {
//             retList = new ArrayList<CategoryInfo> ();
// 
//             for (FeedRecord aRecord : parsedFeedRecordList) {
//                 ArrayList<CategoryInfo> catInfoList;
// 
//                 // this product's category info. A product may be in multiple categories
//                 catInfoList = aRecord.getCategoryInfoList (); 
//                 for (CategoryInfo catInfo : catInfoList) {
//                     if (retList.contains (catInfo) == false) {
//                         retList.add (catInfo);
//                         if (retList.size () >= maxCount)
//                             return retList;
//                     }
//                 }
//             }
//         }
// 
//         return retList;
//     }
//         for (FeedRecord aRecord : parsedFeedRecordList) {
//             ArrayList<CategoryInfo> catInfoList;
// 
//             // this product's category info. A product may be in multiple categories
//             catInfoList = aRecord.getCategoryInfoList (); 
//             for (CategoryInfo catInfo : catInfoList) {
//                 if (catInfo.getLevel () == level) {
//                     // use this as primaryCategory
//                     primaryCategory = catInfo;
// 
//                     // now collect all its children as its 'refinements"
//                     refinedCategoryList = collectRefinementsForCategory (catInfo);
//                     if (refinedCategoryList != null) {
//                         CategoryAndRefinements catAndRefinements;
// 
//                         catAndRefinements = new CategoryAndRefinements (primaryCategory, refinedCategoryList);
//                         // add this to the total-list-of-cat-and-refinements
//                         categoryAndRefinementsList.add (catAndRefinements);
// 
//                         // if we have collected 'maxCount', no need to continue
//                         if (categoryAndRefinementsList.size() == maxCount)
//                             break;
//                     }
//                 }
//             }
//     // go thru all categories in the catalog and collect all immediate children of the given
//     // primaryCategory
//     private ArrayList<CategoryInfo> collectRefinementsForCategory (CategoryInfo primaryCatInfo) {
//         ArrayList<CategoryInfo> refinementList;
// 
//         refinementList = new ArrayList <CategoryInfo> ();
//         for (FeedRecord aRecord : parsedFeedRecordList) {
//             ArrayList<CategoryInfo> catInfoList;
// 
//             // this product's category info. A product may be in multiple categories
//             catInfoList = aRecord.getCategoryInfoList (); 
//             for (CategoryInfo catInfo : catInfoList) {
//                 if (catInfo.getParentId () == primaryCatInfo.getCatId ()) {
//                     refinementList.add (catInfo);
//                 }
//             }
//         }
// 
//         return refinementList;
//     }
//     // collect "maxCount" of primary categories
//     // for each such category, collect 'maxRefinements' number of refinements (actual could be less)
//     public ArrayList <CategoryAndRefinements> listCategoriesAndRefinements (int maxCount, int maxRefinements) {
//         ArrayList <CategoryAndRefinements> categoryAndRefinementsList;
// 
//         categoryAndRefinementsList = new ArrayList <CategoryAndRefinements> ();
//         do {
//             int indx;
//             CategoryInfo primaryCategory;
//             ArrayList<CategoryInfo> refinedCategoryList;
//             CategoryAndRefinements catAndRefinements;
// 
//             indx = (int) (Math.random () * allCategoryInfoList.size());
//             primaryCategory = allCategoryInfoList.get (indx);
//             refinedCategoryList = collectRefinementsForCategory (primaryCategory, maxRefinements);  // refinements for this category
//             if ((refinedCategoryList != null) && (refinedCategoryList.size () > 0)) {
//                 catAndRefinements = new CategoryAndRefinements (primaryCategory, refinedCategoryList);
//                 // add this to the total-list-of-cat-and-refinedCategoryList
//                 categoryAndRefinementsList.add (catAndRefinements);
//             }
//         } while (categoryAndRefinementsList.size () < maxCount);
// 
//         return categoryAndRefinementsList;
//     }
//     // currently we just pick N random categories as 'refinements'. Not sure if making
//     // this algo any more intelligent will make much difference in analytics/reports/... -- TO BE DONE/VALIDATE
//     private ArrayList<CategoryInfo> collectRefinementsForCategory (CategoryInfo primaryCategory, int maxRefinements) {
//         ArrayList<CategoryInfo> refinements;
// 
//         refinements = new ArrayList <CategoryInfo> ();
//         do {
//             int indx;
//             CategoryInfo catInfo;
// 
//             indx = (int) (Math.random () * allCategoryInfoList.size ());
//             catInfo = allCategoryInfoList.get (indx);
//             if ((catInfo.equals (primaryCategory) == false) && (refinements.contains (catInfo) == false)) {
//                 refinements.add (catInfo);
//             }
//         } while (refinements.size () < maxRefinements);
// 
//         return refinements;
//    }
*********/
