package com.bloomreach.analyticssimulator.feed;

public class FeedRecord {

        String productId = "";
        String productName = "";
        String productPrice = "";
        String productSkuId = "";

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
}

