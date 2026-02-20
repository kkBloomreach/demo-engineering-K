package com.bloomreach.trafficgenerator.site.journeydata;

// class to hold startUrl record
public class StartUrlPoolRecord {
        String urlType; // 'product', 'category', 'search', 'home', 'other', 'thematic?'
        String id;  // productId/catId/searchTerm/... - depending on urlType
        String[] views; 
        String url;

        // type = product, category, home
        public StartUrlPoolRecord (String type, String id, String[] views, String url) {
            this.urlType = type;
            this.id = id;
            this.url = url;
            this.views = views;
        }

        public String getUrlType () {
            return this.urlType;
        }

        public String getId () {
            return this.id;
        }

        public String getUrl () {
            return this.url;
        }

        public String[] getViews () {
            return this.views;
        }
}



