package com.bloomreach.analyticsdatagenerator.generate;

import java.util.ArrayList;

import com.bloomreach.analyticsdatagenerator.feed.CategoryInfo;

public class CategoryAndRefinements {

        private CategoryInfo primaryCategory;
        private ArrayList<CategoryInfo> refinements;
        private String view;
        private String segment;
        private boolean isPrimary;

        public CategoryAndRefinements (CategoryInfo primaryCategory,
                                       ArrayList<CategoryInfo> refinements,
                                       String view, String segment) {
            this.primaryCategory = primaryCategory;
            this.refinements = refinements;
            this.view = view;
            this.segment = segment;
        }

        public CategoryInfo getPrimaryCategory () {
            return this.primaryCategory;
        }

        public ArrayList<CategoryInfo> getRefinements () {
            return this.refinements;
        }

        public String getView () {
            return this.view;
        }

        public String getSegment () {
            return this.segment;
        }

        // this method is probably not called -- TO BE CHECKED
        public boolean equals (Object o) {
            CategoryAndRefinements other;

            if (!(o instanceof CategoryAndRefinements))
                return false;

            other = (CategoryAndRefinements) o;
            if (other.getPrimaryCategory ().equals (this.primaryCategory) == false)
                return false;

            if ((other.getView().equals (this.view) == false) ||
                (other.getSegment ().equals (this.segment) == false)) 
                return false;

            if (other.getRefinements().size () != this.refinements.size())
                return false;

            for (CategoryInfo otherCatInfo : other.getRefinements()) {
                if (this.refinements.contains (otherCatInfo) == false)
                    return false;
            }

            return true;
        }
}

