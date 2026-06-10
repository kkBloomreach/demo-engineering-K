// we collect only some necessary attributes from actual product record
// in this internal 'feedRecord' object
package com.bloomreach.trafficgenerator.site.feed;

import java.util.ArrayList;
public class FeedRecord {

        String productId = "";
        String productName = "";
        String productPrice = "";
        String productSalePrice = "";
        boolean productAvailability = false;
        String productUrl = null;
        String productStyle = null; // currently available only in PacificApparel catalog
        ArrayList <ProductVariantRecord> productVariants = null;
        ArrayList <ProductViewRecord> productViews = null;

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

        public void setProductAvailability (boolean avail) {
            this.productAvailability = avail;
        }

        public void setProductUrl (String url) {
            this.productUrl = url;
        }

        public void setProductViews (ArrayList<ProductViewRecord> views) {
            this.productViews = views; // used in PacificSupply feed
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

        public void setProductVariants (ArrayList <ProductVariantRecord> variants) {
            this.productVariants = variants;
        }

        // select one of the variants at random
        // if product has no variants, returns null
        public ProductVariantRecord getProductVariant () {
            ProductVariantRecord selVariant = null;
            if ((this.productVariants != null) && (this.productVariants.size() > 0)) {
                int randomIndx;

                randomIndx = (int) (Math.random () * this.productVariants.size ());
                selVariant = this.productVariants.get (randomIndx);
            }

            return selVariant;
        }

        // select one of the variants at random and return its skuid
        // if product has no variants, returns null
        public String getProductSkuId () {
            String skuid = null;
            ProductVariantRecord selVariant;

            selVariant = this.getProductVariant ();
            if (selVariant != null)
                skuid = selVariant.getSkuId ();
            return skuid;
        }

        public boolean isAvailable () {
            return this.productAvailability;
        }

        public String getProductUrl () {
            return this.productUrl;
        }

        public ArrayList<ProductViewRecord> getProductViews () {
            return this.productViews;
        }

        // this method for backward compatibility
        public ArrayList<String> getProductViewIds () {
            ArrayList <String> productViewIds = null;

            if ((this.productViews != null) && (this.productViews.size () > 0)) {
                productViewIds = new ArrayList <String> ();
                for (ProductViewRecord viewRecord : this.productViews)
                    productViewIds.add (viewRecord.getViewId ());
            }
            return productViewIds;
        }

        public void setProductStyle (String style) {
            this.productStyle = style;
        }

        public String getProductStyle () {
            return this.productStyle; // may be null or blank
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

