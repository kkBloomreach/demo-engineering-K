package com.bloomreach.trafficgenerator.site.journeydata.queryexecutor;

public class CategoryInfo {
        int    level;
        String catId;
        String catName;
        String catPath; // full path, delimiter = "/"
        String parentCatId; // needed to build category refinement list

        public CategoryInfo (int level, String catId, String catName, String catPath, String parentCatId) {
            this.level = level;
            this.catId = catId;
            this.catName = catName;
            this.catPath = catPath;
            this.parentCatId = parentCatId;
        }

        public int getLevel () {
            return this.level;
        }

        public String getCatId () {
            return this.catId;
        }

        public String getCatName () {
            return this.catName;
        }

        public String getCatPath () {
            return this.catPath;
        }

        public String getParentCatId () {
            return this.parentCatId;
        }

        // equals used when returning uniq list of categories in the feed
        public boolean equals (Object o) {
            if (o instanceof CategoryInfo) {
                if (((CategoryInfo)o).catId.equals (this.catId))
                    return true;
            }

            return false;
        }
}


