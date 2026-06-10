// this class is same as the one in Dispatch. We keep it separate just for convenience in queryexecutor
// NOTE: Discovery API response does not include product views (if any). Therefore view-info
// for any product cannot be collected via API response.

package com.bloomreach.trafficgenerator.site.discoveryconnector.nonuseraccess;

import java.util.ArrayList;

public class QueryExecutorSearchApiResponseDoc {

    private String pid;
    private String url;
    private double price;
    private double salePrice = 0.0; // optional in some feeds
    private String title;
    private boolean availability;
    private String style = null;   // optional - available in only some catalogs

    private ArrayList <QueryExecutorVariantRecord> variants = null; // optional

    public QueryExecutorSearchApiResponseDoc () {
    }

    public void setPid (String pid) {
        this.pid = pid;
    }

    public void setUrl (String url) {
        this.url = url;
    }

    public void setPrice (double price) {
        this.price = price;
    }

    public void setSalePrice (double salePrice) {
        this.salePrice = salePrice;
    }

    public void setTitle (String title) {
        this.title = title;
    }

    public void setVariants (ArrayList<QueryExecutorVariantRecord> variants) {
        this.variants = variants;
    }

    public void setAvailability (boolean availability) {
        this.availability = availability;
    }

    public void setStyle (String style) {
        this.style = style;
    }

    public String getPid () {
        return this.pid;
    } 

    public String getUrl () {
        return this.url;
    } 

    public double getPrice () {
        return this.price;
    } 

    // if sale_price is valid, return that. OR price
    public double getSalePrice () {
        if (this.salePrice >= 0.0)
            return this.salePrice;
        return this.price;
    } 

    public String getTitle () {
        return this.title;
    } 

    public ArrayList <QueryExecutorVariantRecord> getVariants () {
        return this.variants;
    }

    // select a variant at random 
    public QueryExecutorVariantRecord selectVariant () {
        // pick a variant at random
        QueryExecutorVariantRecord selVariant = null;

        if ((this.variants != null) && this.variants.size() > 0) {
            int randomIndx;

            randomIndx = (int) (Math.random () * this.variants.size ());
            selVariant = this.variants.get (randomIndx);
        }
        return selVariant;  // may be null if product has no variants
    }
 
    public boolean getAvailability () {
        return this.availability;
    } 

    public String getStyle () {
        return this.style;
    } 
}

/*****
//    // private ArrayList <QueryExecutorViewRecord> views = null; // views not available via Discovery API response
//    public void setViews (ArrayList<QueryExecutorViewRecord> views) {
//        this.views = views;
//    }
//
//    public ArrayList <QueryExecutorViewRecord> getViews () {
//        return this.views;
//    } 
//     public String getViewid () {
//         // pick a view at random
//         String viewid = null;
// 
//         if ((this.views != null) && this.views.size() > 0) {
//             int randomIndx;
//             QueryExecutorViewRecord selView;
// 
//             randomIndx = (int) (Math.random () * this.views.size ());
//             selView = this.views.get (randomIndx);
//             viewid = selView.getViewId ();
//         }
//         return viewid;
//     }
//     public String getSkuid () {
//         // pick a variant at random
//         String skuid = null;
// 
//         if ((this.variants != null) && this.variants.size() > 0) {
//             int randomIndx;
//             QueryExecutorVariantRecord selVariant;
// 
//             randomIndx = (int) (Math.random () * this.variants.size ());
//             selVariant = this.variants.get (randomIndx);
//             skuid = selVariant.getSkuId ();
//         }
//         return skuid;
//     } 
// 
****/

