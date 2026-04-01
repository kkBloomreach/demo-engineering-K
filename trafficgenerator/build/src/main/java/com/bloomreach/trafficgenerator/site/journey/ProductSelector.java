// If this account uses RTS, select a product appropriate for user's current RTS segment
// If it is not an RTS account, select a product at random

// The product selection (eg, high/low value etc) in this code must match the
// rules specified in site.json

package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.user.UserRecord;
import com.bloomreach.trafficgenerator.site.config.SiteConfig;
import com.bloomreach.trafficgenerator.site.build.pixelparams.BuildProductPagePixel;

public class ProductSelector {
    // RTS is currently evaluated only for these account ids
    private final static String accountId_pacifichome = "6413";
    private final static String accountId_pacificapparel = "7529";
    private final static String accountId_sandbox_sales01 = "6742";
    private ProductSelector actualProductSelector = null;

    public ProductSelector () {
    }

    public void prepareSelector () throws Exception {
        String acctId;

        // During dev/debug, even for PacificHome/Apparel, RTS might not be configured...
        if (SiteConfig.getSegmentationType ().equals (SiteConfig.SEGMENTATION_TYPE_NONE)) {
            this.actualProductSelector = new ProductSelector_Default ();
        } else {
            acctId = SiteConfig.getAccountConfigParam ("ACCOUNT_ID");
            switch (acctId) {
                case accountId_pacifichome: 
                    this.actualProductSelector = new ProductSelector_PacificHome ();
                    break;

                case accountId_pacificapparel: 
                    this.actualProductSelector = new ProductSelector_PacificApparel ();
                    break;

                case accountId_sandbox_sales01: 
                    this.actualProductSelector = new ProductSelector_SandboxSales01 ();
                    break;

                default:
                    // NON-RTS account -- productSelector remains null
                    this.actualProductSelector = new ProductSelector_Default ();
            }
        }
    }

    public ProductDetails selectProduct (StepResult prevStepResult,
                                         UserRecord userRecord,
                                         ArrayList<ProductDetails> productList) throws Exception {
        ProductDetails selectedProduct = null;

        selectedProduct = this.actualProductSelector.selectProduct (prevStepResult, userRecord, productList);
        return selectedProduct; // may be null if segment-appropriate product not found
    }

    // ~~~~~~~~~~~~~~
    class ProductSelector_Default extends ProductSelector {

        ProductSelector_Default () {
        }

        public ProductDetails selectProduct (StepResult prevStepResult, 
                                             UserRecord userRecord, 
                                             ArrayList<ProductDetails> productList) throws Exception {
            int randomIndx;
            ProductDetails selectedProduct = null;
            String selectedPDPUrl;

            randomIndx = (int) (Math.random () * productList.size ());
            selectedProduct = productList.get (randomIndx); 
            selectedPDPUrl = BuildProductPagePixel.getProductPageUrl (selectedProduct.getPid(), 
                                                                      selectedProduct.getSkuid ());

            // avoid selecting a product while on THAT product page
            if (prevStepResult.getUrl ().equals (selectedPDPUrl)) {
                if (productList.size() > 1) {
                    randomIndx = (randomIndx + 1) % productList.size ();
                    selectedProduct = productList.get (randomIndx); 
                } else {
                    selectedProduct = null; // only one available product and that too has same url
                }
            }

            return selectedProduct;
        }
    }

    // ~~~~~~~~~~~~~~
    class ProductSelector_PacificHome extends ProductSelector {

        // these strings must match the ones defined in site config -> RTS segment names
        private final static String SEGMENT_NAME_LOW_VALUE_CUSTOMERS = "Low Value Customers";
        private final static String SEGMENT_NAME_HIGH_VALUE_CUSTOMERS = "High Value Customers";

        ProductSelector_PacificHome () {
        }

        public ProductDetails selectProduct (StepResult prevStepResult,
                                             UserRecord userRecord, 
                                             ArrayList<ProductDetails> productList) throws Exception {
            String currentSegment;
            ProductDetails selectedProduct = null;

            currentSegment = userRecord.getSegment ();
            if (currentSegment != null) {
                if (currentSegment.equals (SEGMENT_NAME_LOW_VALUE_CUSTOMERS) == true) {
                    selectedProduct = this.selectBudgetProduct (prevStepResult, productList);
                } else if (currentSegment.equals (SEGMENT_NAME_HIGH_VALUE_CUSTOMERS) == true) {
                    selectedProduct = this.selectLuxuryProduct (prevStepResult, productList);
                } else {
                    MessageLogger.logError (String.format ("Unknown segment name: %s", currentSegment));
                }
            }

            return selectedProduct; 
        }

        private ProductDetails selectBudgetProduct (StepResult prevStepResult,
                                                    ArrayList<ProductDetails> productList) throws Exception {
            ArrayList <ProductDetails> budgetProductList;
            int randomIndx;
            ProductDetails selectedProduct = null;
            String selectedPDPUrl;

            budgetProductList = new ArrayList <ProductDetails> ();
            for (ProductDetails aProduct : productList) {
                if (aProduct.getSalePrice () <= 175.00) {
                    budgetProductList.add (aProduct);
                }
            }

            if (budgetProductList.size () == 0)
                return null;

            randomIndx = (int) (Math.random () * budgetProductList.size ());
            selectedProduct = budgetProductList.get (randomIndx); 
            selectedPDPUrl = BuildProductPagePixel.getProductPageUrl (selectedProduct.getPid(), 
                                                                      selectedProduct.getSkuid ());
            // avoid selecting a product while on THAT product page
            if (prevStepResult.getUrl ().equals (selectedPDPUrl)) {
                if (budgetProductList.size() > 1) {
                    randomIndx = (randomIndx + 1) % budgetProductList.size ();
                    selectedProduct = budgetProductList.get (randomIndx); 
                } else {
                    selectedProduct = null; // only one available product and that too has same url
                }
            }

            return selectedProduct;
        }

        private ProductDetails selectLuxuryProduct (StepResult prevStepResult,
                                                    ArrayList<ProductDetails> productList) throws Exception {
            ArrayList <ProductDetails> luxuryProductList;
            int randomIndx;
            ProductDetails selectedProduct = null;
            String selectedPDPUrl;

            luxuryProductList = new ArrayList <ProductDetails> ();
            for (ProductDetails aProduct : productList) {
                if (aProduct.getSalePrice () > 175.00) {
                    luxuryProductList.add (aProduct);
                }
            }

            if (luxuryProductList.size () == 0)
                return null;

            randomIndx = (int) (Math.random () * luxuryProductList.size ());
            selectedProduct = luxuryProductList.get (randomIndx); 
            selectedPDPUrl = BuildProductPagePixel.getProductPageUrl (selectedProduct.getPid(), 
                                                                      selectedProduct.getSkuid ());
            // avoid selecting a product while on THAT product page
            if (prevStepResult.getUrl ().equals (selectedPDPUrl)) {
                if (luxuryProductList.size() > 1) {
                    randomIndx = (randomIndx + 1) % luxuryProductList.size ();
                    selectedProduct = luxuryProductList.get (randomIndx); 
                } else {
                    selectedProduct = null; // only one available product and that too has same url
                }
            }

            return selectedProduct;
        }
    }

    // ~~~~~~~~~~~~~~~~
    class ProductSelector_PacificApparel extends ProductSelector {

        // these strings must match the ones defined in site config -> RTS segment names
        private final static String SEGMENT_NAME_LOW_VALUE_CUSTOMERS = "Low Value Customers";
        private final static String SEGMENT_NAME_HIGH_VALUE_CUSTOMERS = "High Value Customers";

        ProductSelector_PacificApparel () {
        }

        public ProductDetails selectProduct (StepResult prevStepResult,
                                             UserRecord userRecord, 
                                             ArrayList<ProductDetails> productList) throws Exception {
            String currentSegment;
            ProductDetails selectedProduct = null;

            currentSegment = userRecord.getSegment ();
            if (currentSegment != null) {
                if (currentSegment.equals (SEGMENT_NAME_LOW_VALUE_CUSTOMERS) == true) {
                    selectedProduct = this.selectLowValueProduct (prevStepResult, productList);
                } else if (currentSegment.equals (SEGMENT_NAME_HIGH_VALUE_CUSTOMERS) == true) {
                    selectedProduct = this.selectHighValueProduct (prevStepResult, productList);
                } else {
                    MessageLogger.logError (String.format ("Unknown segment name: %s", currentSegment));
                }
            }

            return selectedProduct; 
        }

        private ProductDetails selectLowValueProduct (StepResult prevStepResult,
                                                      ArrayList<ProductDetails> productList) throws Exception {
            ArrayList <ProductDetails> lowValueProductList;
            int randomIndx;
            ProductDetails selectedProduct = null;
            String selectedPDPUrl;

            lowValueProductList = new ArrayList <ProductDetails> ();
            for (ProductDetails aProduct : productList) {
                if (aProduct.getSalePrice () <= 100.00) {
                    lowValueProductList.add (aProduct);
                }
            }

            if (lowValueProductList.size () == 0)
                return null;

            randomIndx = (int) (Math.random () * lowValueProductList.size ());
            selectedProduct = lowValueProductList.get (randomIndx); 
            selectedPDPUrl = BuildProductPagePixel.getProductPageUrl (selectedProduct.getPid(), 
                                                                      selectedProduct.getSkuid ());

            // avoid selecting a product while on THAT product page
            if (prevStepResult.getUrl ().equals (selectedPDPUrl)) {
                if (lowValueProductList.size() > 1) {
                    randomIndx = (randomIndx + 1) % lowValueProductList.size ();
                    selectedProduct = lowValueProductList.get (randomIndx); 
                } else {
                    selectedProduct = null; // only one available product and that too has same url
                }
            }

            return selectedProduct;
        }

        private ProductDetails selectHighValueProduct (StepResult prevStepResult,
                                                       ArrayList<ProductDetails> productList) throws Exception {
            ArrayList <ProductDetails> highValueProductList;
            int randomIndx;
            ProductDetails selectedProduct = null;
            String selectedPDPUrl;

            highValueProductList = new ArrayList <ProductDetails> ();
            for (ProductDetails aProduct : productList) {
                if (aProduct.getSalePrice () > 100.00) {
                    highValueProductList.add (aProduct);
                }
            }

            if (highValueProductList.size () == 0)
                return null;

            randomIndx = (int) (Math.random () * highValueProductList.size ());
            selectedProduct = highValueProductList.get (randomIndx); 
            selectedPDPUrl = BuildProductPagePixel.getProductPageUrl (selectedProduct.getPid(), 
                                                                      selectedProduct.getSkuid ());
            // avoid selecting a product while on THAT product page
            if (prevStepResult.getUrl ().equals (selectedPDPUrl)) {
                if (highValueProductList.size() > 1) {
                    randomIndx = (randomIndx + 1) % highValueProductList.size ();
                    selectedProduct = highValueProductList.get (randomIndx); 
                } else {
                    selectedProduct = null; // only one available product and that too has same url
                }
            }

            return selectedProduct;
        }
    }

    // ~~~~~~~~~~~~~~~~
    class ProductSelector_SandboxSales01 extends ProductSelector {

        // these strings must match the ones defined in site config -> RTS segment names
        private final static String SEGMENT_NAME_FORMAL = "Formal";
        private final static String SEGMENT_NAME_CASUAL = "Casual";
        private final static String SEGMENT_NAME_ATHLETIC = "Athletic";
        private final static String SEGMENT_NAME_DESIGNER = "Designer";
        private final static String SEGMENT_NAME_OTHER = "Other";

        ProductSelector_SandboxSales01 () {
        }

        public ProductDetails selectProduct (StepResult prevStepResult,
                                             UserRecord userRecord, 
                                             ArrayList<ProductDetails> productList) throws Exception {
            String currentSegment;
            ProductDetails selectedProduct = null;

            currentSegment = userRecord.getSegment ();
            if (currentSegment != null) {
                switch (currentSegment) {
                    case SEGMENT_NAME_FORMAL:
                        selectedProduct = this.selectProductWithStyle (prevStepResult, productList, "Formal");
                        break;
                    case SEGMENT_NAME_CASUAL:
                        selectedProduct = this.selectProductWithStyle (prevStepResult, productList, "Casual");
                        break;
                    case SEGMENT_NAME_ATHLETIC:
                        selectedProduct = this.selectProductWithStyle (prevStepResult, productList, "Athletic");
                        break;
                    case SEGMENT_NAME_DESIGNER:
                        selectedProduct = this.selectProductWithStyle (prevStepResult, productList, "Designer");
                        break;
                    case SEGMENT_NAME_OTHER:
                        selectedProduct = this.selectProductWithoutStyle (prevStepResult, productList);
                        break;
                    default:
                        MessageLogger.logError (String.format ("Unknown segment name: %s", currentSegment));
                }
                
            } 

            return selectedProduct; 
        }

        private ProductDetails selectProductWithStyle (StepResult prevStepResult,
                                                       ArrayList<ProductDetails> productList,
                                                       String requiredStyle) throws Exception {
            ArrayList <ProductDetails> selectedProductList;
            int randomIndx;
            ProductDetails selectedProduct = null;
            String selectedPDPUrl;
            String productStyle;

            selectedProductList = new ArrayList <ProductDetails> ();
            for (ProductDetails aProduct : productList) {
                productStyle = aProduct.getStyle ();
                if ((productStyle != null) && (productStyle.equals (requiredStyle)))  {
                    selectedProductList.add (aProduct);
                }
            }

            if (selectedProductList.size () == 0)
                return null;

            randomIndx = (int) (Math.random () * selectedProductList.size ());
            selectedProduct = selectedProductList.get (randomIndx); 
            selectedPDPUrl = BuildProductPagePixel.getProductPageUrl (selectedProduct.getPid(), 
                                                                      selectedProduct.getSkuid ());

            // avoid selecting a product while on THAT product page
            if (prevStepResult.getUrl ().equals (selectedPDPUrl)) {
                if (selectedProductList.size() > 1) {
                    randomIndx = (randomIndx + 1) % selectedProductList.size ();
                    selectedProduct = selectedProductList.get (randomIndx); 
                } else {
                    selectedProduct = null; // only one available product and that too has same url
                }
            }

            return selectedProduct;
        }

        // for 'other' segment - style is none of 'formal'/'casual'/'designer'/'athletic'
        // or style is null or blank
        private ProductDetails selectProductWithoutStyle (StepResult prevStepResult,
                                                          ArrayList<ProductDetails> productList) throws Exception {
            ArrayList <ProductDetails> selectedProductList;
            int randomIndx;
            ProductDetails selectedProduct = null;
            String selectedPDPUrl;
            String productStyle;

            selectedProductList = new ArrayList <ProductDetails> ();
            for (ProductDetails aProduct : productList) {
                productStyle = aProduct.getStyle ();
                if ((productStyle == null) || (productStyle.equals (""))){
                    selectedProduct = aProduct;
                    return selectedProduct; // no need to look further
                } else {
                    if ((productStyle.equals ("Formal") == false) &&
                        (productStyle.equals ("Casual") == false) &&
                        (productStyle.equals ("Athletic") == false) &&
                        (productStyle.equals ("Designer") == false)) {
                            selectedProductList.add (aProduct);
                    }
                } 
            }

            if (selectedProductList.size () == 0)
                return null;

            randomIndx = (int) (Math.random () * selectedProductList.size ());
            selectedProduct = selectedProductList.get (randomIndx); 
            selectedPDPUrl = BuildProductPagePixel.getProductPageUrl (selectedProduct.getPid(), 
                                                                      selectedProduct.getSkuid ());

            // avoid selecting a product while on THAT product page
            if (prevStepResult.getUrl ().equals (selectedPDPUrl)) {
                if (selectedProductList.size() > 1) {
                    randomIndx = (randomIndx + 1) % selectedProductList.size ();
                    selectedProduct = selectedProductList.get (randomIndx); 
                } else {
                    selectedProduct = null; // only one available product and that too has same url
                }
            }

            return selectedProduct;
        }
    }
}
