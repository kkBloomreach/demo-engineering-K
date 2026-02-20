package com.bloomreach.analyticssimulator.build.pixellog;

import java.io.IOException;
import java.net.URLEncoder;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.analytics.pixel.CommonFields;
import com.bloomreach.proto.Aggregation.Basket;
import com.bloomreach.proto.Aggregation.Basket.Item;

import com.bloomreach.analyticssimulator.SimulatorConstants;
import com.bloomreach.analyticssimulator.SimulatorConfig;
import com.bloomreach.analyticssimulator.MessageLogger;
import com.bloomreach.analyticssimulator.simdata.*;
import com.bloomreach.analyticssimulator.feed.*;

public class BuildConversionPixel extends BuildPixelBase  {

    private final static int MAX_QUANTITY = 10; // quantity added in cart

    public BuildConversionPixel () {
    }

    public int build (PixelLog.Builder pixelLogBuilder, UidToSegmentRecord uidToSegmentRecord, 
                              long logTime, String pid, ProcessedFeed processedFeed, OrderIdGenerator orderIdGenerator) throws Exception {
        // let base class update 'common' fields
        int simulateStatus;

        simulateStatus = setCommonFields (pixelLogBuilder, uidToSegmentRecord, logTime);
        if (simulateStatus == SimulatorConstants.SIMULATE_STATUS_OK) {
            int intQuantity;
            double quantity;
            double basketValue;
            Basket.Builder simulatedBasketBuilder;
            String orderId;
            String refUrl;
            String url;
            String basketParam;

            // Basket, basket_value. 
            // product price
            String priceStr = processedFeed.lookupProductPrice (pid);
            // MessageLogger.logDebug ("DEBUG price = " + priceStr + ", pid = " + pid );
            double priceAsDouble = 0.0;
            try {
                priceAsDouble = Double.parseDouble (priceStr);
            } catch (NumberFormatException nfe) {
                // should not happen
            }

            // quantity = 1.0; // Currently quantity = 1
            intQuantity = 1 + (int) (Math.random () * MAX_QUANTITY); // values include 0 to (but not including) total
            quantity = new BigDecimal (intQuantity).setScale (2).doubleValue();
            simulatedBasketBuilder = prepareBasketBuilder (pid, priceAsDouble, quantity, processedFeed);

            basketValue = priceAsDouble * quantity;
            basketValue = new BigDecimal (basketValue).setScale (2, RoundingMode.DOWN).doubleValue();
            pixelLogBuilder.setBasket (simulatedBasketBuilder);
            pixelLogBuilder.setBasketValue (basketValue);

            // basket 'param' string
            basketParam = prepareBasketParam (pid, priceAsDouble, quantity, processedFeed);
            MessageLogger.logDebug ("Conversion basket param: " + basketParam);
            replacePixelLogParam (pixelLogBuilder, "basket", basketParam);

            // set order Id
            orderId = orderIdGenerator.allocateOrderId ();
            pixelLogBuilder.setOrderId (orderId);
            replacePixelLogParam (pixelLogBuilder, "order_id", orderId);

            // ref_url == atcPageUrl, always
            refUrl = SimulatorConfig.getConfigParam ("ATC_PAGE_URL");
            pixelLogBuilder.setRefUrl (refUrl);
            replacePixelLogParam (pixelLogBuilder, "ref", refUrl);

            // url == conversion_page always
            url = SimulatorConfig.getConfigParam ("CONVERSION_PAGE_URL");
            pixelLogBuilder.setUrl (url);
            replacePixelLogParam (pixelLogBuilder, "url", url);

            simulateStatus = SimulatorConstants.SIMULATE_STATUS_OK; 
        }

        return (simulateStatus);
    }

    // given a pid and quantity, prepare basket.builder object
    // Currently the basket has only one item (ie, customer has purchased only one product, perhaps with 
    // multiple quantities of it
    private Basket.Builder prepareBasketBuilder (String pid, double price, double quantity, ProcessedFeed processedFeed) {

        Basket.Builder basketBuilder;
        Basket.Item.Builder basketItemBuilder;
        String productName;
        String productSkuId;
        String encodedProductName;

        // product name
        productName = processedFeed.lookupProductName (pid);
        encodedProductName = URLEncoder.encode (productName);
        productSkuId = processedFeed.lookupProductSkuId (pid);
        if (productSkuId == null)
            productSkuId = "";
        else
            productSkuId = URLEncoder.encode (productSkuId);

        // prepare an 'item builder' first
        basketItemBuilder = Basket.Item.newBuilder ();
        basketItemBuilder.setProdId (pid);
        basketItemBuilder.setPrice (price);
        basketItemBuilder.setProdName (encodedProductName);
        basketItemBuilder.setQuantity (quantity);
        basketItemBuilder.setProdSku(productSkuId);

        // then add item-builder in a 'basket-builder'
        basketBuilder = Basket.newBuilder ();
        basketBuilder.addItem (basketItemBuilder);

        // then return the 'basket-builder'
        return (basketBuilder);
    }

    private String prepareBasketParam (String pid, double price, double quantity, ProcessedFeed processedFeed) {
        StringBuffer paramBuf;
        String productName;
        String productSkuId;
        String basketParam;

        productName = processedFeed.lookupProductName (pid);
        productSkuId = processedFeed.lookupProductSkuId (pid);

        paramBuf = new StringBuffer ();
        paramBuf.append ("i");
        paramBuf.append (pid);
        if (productSkuId != null) { 
            paramBuf.append ("'s");
            // is it necessary to encode here if whole buf is encoded below ? TO BE CHECKED
            productSkuId = URLEncoder.encode (productSkuId);
            paramBuf.append (productSkuId);
        }
        paramBuf.append ("'n");
        paramBuf.append (productName);
        paramBuf.append ("'q");
        paramBuf.append (quantity);
        paramBuf.append ("'p");
        paramBuf.append (price);

        basketParam = URLEncoder.encode (new String (paramBuf));
        basketParam = "!" + basketParam;
        return (basketParam);
    }
}


