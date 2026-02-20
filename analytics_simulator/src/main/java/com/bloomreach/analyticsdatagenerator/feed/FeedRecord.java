package com.bloomreach.analyticsdatagenerator.feed;

import java.util.ArrayList;

public class FeedRecord {

        String productId = "";
        String productName = "";
        String productPrice = "";
        String productSkuId = "";
        ArrayList<CategoryInfo> categoryInfoList = null;

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

        public String getProductName () {
            return this.productName;
        }

        public void setProductPrice (String price) {
            this.productPrice = price;
        }

        public String getProductPrice () {
            return this.productPrice;
        }

        public void setProductSkuId (String skuId) {
            this.productSkuId = skuId;
        }

        public String getProductSkuId () {
            return this.productSkuId;
        }

        // level = 0, 1, 2, ... for L0/L1/L2/... respectively
        public void setCategoryInfoList (ArrayList<CategoryInfo> categoryInfoList) {
            this.categoryInfoList = categoryInfoList;
        }

        public ArrayList <CategoryInfo> getCategoryInfoList () {
            return this.categoryInfoList;
        }
}

