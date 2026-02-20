package com.bloomreach.analyticssimulator.simdata;

// class to hold zero result query info
// For each query (eg, 'engine oil') associate segment, corresponding 'refinedSearchQueryId' and refinedCategoryQueryId
// created manually. Therefore there is no 'generator' to generate this list

import java.util.ArrayList;

public class ZeroResultQueryRecord {
        int id;
        String  query;

        public ZeroResultQueryRecord (int id, String query) {
            this.id = id;
            this.query = query;
        }

        public int getId () {
            return this.id;
        }

        public String getQuery () {
            return this.query;
        }
}

