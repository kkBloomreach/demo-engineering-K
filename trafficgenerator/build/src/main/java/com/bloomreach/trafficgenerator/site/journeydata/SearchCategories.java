package com.bloomreach.trafficgenerator.site.journeydata;

// Uid to segment map is generated ONCE and saved to local file
// That generated map is loaded in this class + associated lookup methods
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.discoveryconnector.nonuseraccess.CategoryInfo;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildCategoryPagePixel;

public class SearchCategories {

    // private ProductFeed productFeed;
    private CategoryCollector categoryCollector;
    private ArrayList <CategoryInfo> searchCategories; // contains catInfo after 'exclusion'
    private ArrayList <String> excludeCatIds = null; // may be null

    public SearchCategories () {
    }

    public void setCategoryCollector (CategoryCollector categoryCollector) {
        this.categoryCollector = categoryCollector;
    }

    // category Ids to be excluded in generating search-category-ids
    public void setExcludeCategoryIds (ArrayList<String> excludeCatIds) {
        this.excludeCatIds = excludeCatIds;
    }

    public void doLoad () throws Exception {
        ArrayList <CategoryInfo> allCategoriesInfoList;

        allCategoriesInfoList = this.categoryCollector.getAllCategoryInfoList (); // may be null in rare cases
 
        this.searchCategories = new ArrayList <CategoryInfo> ();
        if (allCategoriesInfoList != null) {
            for (CategoryInfo srcCatInfo : allCategoriesInfoList) {
                if ((this.excludeCatIds != null) && (this.excludeCatIds.indexOf (srcCatInfo.getCatId ()) >= 0))
                    continue;   // skip this catId
                this.searchCategories.add (srcCatInfo);
            }
        }
    }

    public int getCategoriesCount () {
        return (this.searchCategories.size ()); 
    }

    public CategoryInfo selectCategoryAtRandom () {
        return selectCategoryAtRandom (null);
    }

    public CategoryInfo selectCategoryAtRandom (String currentUrl) {
        int randomIndex;
        CategoryInfo selectedCatInfo;
        String selectedCatPageUrl;

        if ((this.searchCategories != null) && (this.searchCategories.size() > 0)) {
            randomIndex = (int) (Math.random () * this.searchCategories.size());
            selectedCatInfo = this.searchCategories.get (randomIndex); 
            selectedCatPageUrl = BuildCategoryPagePixel.getCategoryPageUrl (selectedCatInfo.getCatId ());
            if ((currentUrl != null) && (currentUrl.equals (selectedCatPageUrl))) {
                randomIndex = (randomIndex + 1) % this.searchCategories.size ();
                selectedCatInfo = this.searchCategories.get (randomIndex); 
            }
        } else
            selectedCatInfo = null;

        return (selectedCatInfo);
    }


    public CategoryInfo selectRefinedSearchCategoryAtRandom (CategoryInfo priorSearchCatInfo) {
        int randomIndex;
        CategoryInfo selectedCatInfo;
        int maxAttempts = 5;
        int attemptNum = 0;

        if ((this.searchCategories != null) && (this.searchCategories.size() > 0)) {
            randomIndex = (int) (Math.random () * this.searchCategories.size());
            selectedCatInfo = this.searchCategories.get (randomIndex); 

            while (priorSearchCatInfo.getCatId().equals (selectedCatInfo.getCatId())) {
                // take the next searchCategory, round-robin
                randomIndex = (randomIndex + 1) % (this.searchCategories.size());
                selectedCatInfo = this.searchCategories.get (randomIndex); 
                if (attemptNum++ >= maxAttempts) {
                    MessageLogger.logError (String.format ("Could not get a different refined category for prior category: %s", priorSearchCatInfo.getCatId ()));
                    break;
                }
            }
        } else
            selectedCatInfo = null;
            
        return (selectedCatInfo);
    }
}

/***************
//     // lookup searchCategory. If it happens to match the one in parameter,
//     // skip that
//     public CategoryInfo selectRefinedSearchCategoryAtRandom_SAVE (CategoryInfo priorSearchCatInfo) {
//         int randomIndex;
//         CategoryInfo selectedCatInfo;
// 
//         randomIndex = (int) (Math.random () * this.searchCategories.size());
//         selectedCatInfo = this.searchCategories.get (randomIndex); 
//         if (priorSearchCatInfo.getCatId().equals (selectedCatInfo.getCatId())) {
//             // take the next searchCategory, round-robin
//             randomIndex = (randomIndex + 1) % (this.searchCategories.size());
//             selectedCatInfo = this.searchCategories.get (randomIndex); 
//         }
//         return (selectedCatInfo);
//     }
//     public CategoryInfo selectCategoryAtRandom_WITHCATID (String priorCatId) {
//         int randomIndex;
//         CategoryInfo selectedCatInfo;
// 
//         randomIndex = (int) (Math.random () * this.searchCategories.size());
//         selectedCatInfo = this.searchCategories.get (randomIndex); 
//         if ((priorCatId != null) && (priorCatId.equals (selectedCatInfo.getCatId ()))) {
//             randomIndex = (randomIndex + 1) % this.searchCategories.size ();
//             selectedCatInfo = this.searchCategories.get (randomIndex); 
//         }
// 
//         return (selectedCatInfo);
//     }
***********/

