package com.bloomreach.analyticsdatagenerator.generate;

import java.util.ArrayList;

import com.bloomreach.analyticsdatagenerator.feed.CategoryInfo;

public class CategoryLogInfo {

        private CategoryInfo categoryInfo;
        private String view;
        private String segment;
        private boolean isPrimary;
        private SearchQueryResponseInfo queryResponseInfo;

        public CategoryLogInfo (CategoryInfo categoryInfo,
                                boolean isPrimary,
                                String view, String segment) {
            this.categoryInfo = categoryInfo;
            this.isPrimary = isPrimary;
            this.view = view;
            this.segment = segment;
            this.queryResponseInfo = null;  // set later via setter method
        }

        public CategoryInfo getCategoryInfo () {
            return this.categoryInfo;
        }

        public boolean isPrimary () {
            return this.isPrimary;
        }

        public String getView () {
            return this.view;
        }

        public String getSegment () {
            return this.segment;
        }

        public void setQueryResponseInfo (SearchQueryResponseInfo queryResponseInfo) {
            this.queryResponseInfo = queryResponseInfo;
        }

        public SearchQueryResponseInfo getQueryResponseInfo () {
            return this.queryResponseInfo;
        }

        public boolean equals (Object o) {
            CategoryLogInfo other;

            if (!(o instanceof CategoryLogInfo))
                return false;

            other = (CategoryLogInfo) o;
            if (other.getCategoryInfo ().equals (this.categoryInfo) == false)
                return false;

            if ((other.isPrimary != this.isPrimary) ||
                (other.getView().equals (this.view) == false) ||
                (other.getSegment ().equals (this.segment) == false)) 
                return false;

            // queryResponseInfo not checked for 'equality'

            return true;
        }
}

