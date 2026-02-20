package com.bloomreach.trafficgenerator.site.build.pixelparams;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import com.bloomreach.trafficgenerator.GeneratorConstants;
import com.bloomreach.trafficgenerator.MessageLogger;
import com.bloomreach.trafficgenerator.site.journeydata.templates.*;
import com.bloomreach.trafficgenerator.site.user.*;
import com.bloomreach.trafficgenerator.site.journey.Cart;
import com.bloomreach.trafficgenerator.site.journey.CartItem;
import com.bloomreach.trafficgenerator.site.journey.ProductDetails;
import com.bloomreach.trafficgenerator.site.journeylogs.StepLog;

public class BuildConversionPixel extends BuildPixelBase  {

    public BuildConversionPixel () {
    }

    public int build (PixelBRData pixelData, 
                      UserRecord userRecord, 
                      long logTime,
                      String refUrl,
                      String url,
                      StepLog stepLog, 
                      Cart userCart,
                      OrderIdGenerator orderIdGenerator,
                      boolean testData) throws Exception {
        // let base class update 'common' fields
        int buildStatus;

        buildStatus = setCommonFields (pixelData, userRecord, logTime, testData);
        if (buildStatus == GeneratorConstants.GENERATE_STATUS_OK) {
            ArrayList <CartItem> cartItems;
            double basketValue;
            StringBuffer cartBasketParamBuf;
            String cartBasketParam;
            String orderId;
            String logString;
 
            basketValue = 0.0;
            cartBasketParamBuf = new StringBuffer ();

            cartItems = userCart.getItems ();
            for (CartItem item : cartItems) {
                ProductDetails productDetails;
                int quantity;
                String basketParam;
                double salePrice;

                productDetails = item.getItem ();

                // Basket, basket_value. 
                // product sale_price
                salePrice = productDetails.getSalePrice ();

                // quantity
                quantity = item.getQuantity ();

                // basket value
                basketValue = basketValue + (salePrice * quantity);

                // basket 'param' string
                basketParam = prepareBasketParam (productDetails, quantity);
                cartBasketParamBuf.append (basketParam);
            }

            // total basket value
            basketValue = new BigDecimal (basketValue).setScale (2, RoundingMode.DOWN).doubleValue();
            pixelData.setParam (PixelBRData.PARAMNAME_BASKET_VALUE, Double.toString (basketValue));

            // basket 'param' string
            cartBasketParam = cartBasketParamBuf.toString ();
            MessageLogger.logDebug ("Conversion basket param: " + cartBasketParam);
            pixelData.setParam (PixelBRData.PARAMNAME_BASKET, cartBasketParam);

            // set order Id
            orderId = orderIdGenerator.allocateOrderId ();
            pixelData.setParam (PixelBRData.PARAMNAME_ORDER_ID, orderId);

            // set info in stepLog. This is needed to debug Insight reports 
            // (eg, insight shows conversion > 100%)
            logString = String.format ("basket = %s, value = %f, orderId = %s",
                                        cartBasketParam, basketValue, orderId);
            stepLog.setQuery (logString);
 
            // ref_url 
            pixelData.setParam (PixelBRData.PARAMNAME_REF_URL, refUrl);

            // url == conversion_page always
            pixelData.setParam (PixelBRData.PARAMNAME_URL, url);

            // is_conversion = 1
            pixelData.setParam (PixelBRData.PARAMNAME_IS_CONVERSION, "1");

            // for conversion pixel, pixelType is not used but setting it anyway...
            // pixeltype = pageview
            pixelData.setParam (PixelBRData.PARAMNAME_PIXEL_TYPE, PixelBRData.PIXEL_TYPE_PAGEVIEW);
            pixelData.setParam (PixelBRData.PARAMNAME_PAGE_TYPE, PixelBRData.PAGE_TYPE_OTHER);

            // update pixelCount
            updatePixelCountLog(PixelBRData.PIXEL_TYPE_PAGEVIEW, PixelBRData.PAGE_TYPE_OTHER);

            buildStatus = GeneratorConstants.GENERATE_STATUS_OK; 
        }

        return (buildStatus);
    }

    private String prepareBasketParam (ProductDetails productDetails, double quantity) {
        StringBuffer paramBuf;
        String productName;
        String productSkuId;
        String basketParam;

        productName = productDetails.getTitle ();
        productSkuId = productDetails.getSkuid ();

        paramBuf = new StringBuffer ();
        paramBuf.append ("!");  // start of one basket entity
        paramBuf.append ("i");
        paramBuf.append (productDetails.getPid());
        if (productSkuId != null) { 
            paramBuf.append ("'s");
            paramBuf.append (productSkuId);
        }
        paramBuf.append ("'n");
        paramBuf.append (productName);
        paramBuf.append ("'q");
        paramBuf.append (quantity);
        paramBuf.append ("'p"); // attrib name is 'p', value = salePrice
        paramBuf.append (productDetails.getSalePrice ());

        basketParam = new String (paramBuf);
        return (basketParam); // url-encoded when generating queryParam (see Dispatcher.java)
    }
}


