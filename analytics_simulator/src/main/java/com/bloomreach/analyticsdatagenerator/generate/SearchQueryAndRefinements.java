package com.bloomreach.analyticsdatagenerator.generate;

import java.util.ArrayList;

public class SearchQueryAndRefinements {

        private String primaryQuery;
        private ArrayList<String> refinements;
        private String view;
        private String segment;
        private boolean isPrimary;

        public SearchQueryAndRefinements (String primaryQuery,
                                       ArrayList<String> refinements,
                                       String view, String segment) {
            this.primaryQuery = primaryQuery;
            this.refinements = refinements;
            this.view = view;
            this.segment = segment;
        }

        public String getPrimaryQuery () {
            return this.primaryQuery;
        }

        public ArrayList<String> getRefinements () {
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
            SearchQueryAndRefinements other;

            if (!(o instanceof SearchQueryAndRefinements))
                return false;

            other = (SearchQueryAndRefinements) o;
            if (other.getPrimaryQuery ().equals (this.primaryQuery) == false)
                return false;

            if ((other.getView().equals (this.view) == false) ||
                (other.getSegment ().equals (this.segment) == false)) 
                return false;

            if (other.getRefinements().size () != this.refinements.size())
                return false;

            for (String otherQuery : other.getRefinements()) {
                if (this.refinements.contains (otherQuery) == false)
                    return false;
            }

            return true;
        }
}

