package com.bloomreach.analyticsdatagenerator.generate;

import java.util.ArrayList;

public class SearchQueryLogInfo {

        private String query;
        private String view;
        private String segment;
        private boolean isPrimary;
        private SearchQueryResponseInfo queryResponseInfo;

        public SearchQueryLogInfo (String query,
                                boolean isPrimary,
                                String view, String segment) {
            this.query = query;
            this.isPrimary = isPrimary;
            this.view = view;
            this.segment = segment;
            this.queryResponseInfo = null;  // set later via setter method
        }

        public String getQuery () {
            return this.query;
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
            SearchQueryLogInfo other;

            if (!(o instanceof SearchQueryLogInfo))
                return false;

            other = (SearchQueryLogInfo) o;
            if (other.getQuery ().equals (this.query) == false)
                return false;

            if ((other.isPrimary != this.isPrimary) ||
                (other.getView().equals (this.view) == false) ||
                (other.getSegment ().equals (this.segment) == false)) 
                return false;

            // queryResponseInfo not checked for 'equality'

            return true;
        }
}

