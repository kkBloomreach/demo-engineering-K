// we collect only some necessary attributes from actual product record
// in this internal 'feedRecord' object
package com.bloomreach.trafficgenerator.site.feed;

import java.util.ArrayList;
public class FeedRecord {

        String productId = "";
        String productName = "";
        String productPrice = "";
        String productSalePrice = "";
        String productSkuId = "";
        boolean availability = false;
        ArrayList<String> views = null;
        String url = null;

        public FeedRecord () {
        }

        public void setProductId (String pid) {
            this.productId = pid;
        }

        public String getProductId () {
            return this.productId;
        }

        public void setProductName (String name) {
            this.productName = name;
        }

        public void setAvailability (boolean avail) {
            this.availability = avail;
        }

        public void setUrl (String url) {
            this.url = url;
        }

        // param is comma-separated list of views
        public void setViews (ArrayList<String> views) {
            this.views = views; // used in PacificSupply feed
        }

        public String getProductName () {
            return this.productName;
        }

        public void setProductPrice (String price) {
            this.productPrice = price;
        }

        public String getProductPrice () {
            return this.productPrice;
        }

        public void setProductSalePrice (String salePrice) {
            this.productSalePrice = salePrice;
        }

        public String getProductSalePrice () {
            return this.productSalePrice;
        }

        public void setProductSkuId (String skuId) {
            this.productSkuId = skuId;
        }

        public String getProductSkuId () {
            return this.productSkuId;
        }

        public boolean isAvailable () {
            return this.availability;
        }

        public String getUrl () {
            return this.url;
        }

        public ArrayList<String> getViews () {
            return this.views;
        }

}

/*****
//         ArrayList<CategoryInfo> categoryInfoList = null;
//         // level = 0, 1, 2, ... for L0/L1/L2/... respectively
//         public void setCategoryInfoList (ArrayList<CategoryInfo> categoryInfoList) {
//             this.categoryInfoList = categoryInfoList;
//         }
// 
//         public ArrayList <CategoryInfo> getCategoryInfoList () {
//             return this.categoryInfoList;
//         }
*****/

