package com.bloomreach.brxdemos.pacifichome.translate.pixel.clone;

import java.io.IOException;
import java.util.List;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.bloomreach.proto.Aggregation.PixelLog;
import com.bloomreach.proto.Aggregation.PixelLog.Builder;
import com.bloomreach.proto.Aggregation.Basket;
import com.bloomreach.proto.Aggregation.Basket.Item;
import com.bloomreach.analytics.pixel.CommonFields;

import com.bloomreach.brxdemos.pacifichome.translate.pixel.feed.*;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.CloneConstants;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.ProductURLPidMapReader;
import com.bloomreach.brxdemos.pacifichome.translate.pixel.urlmap.CategoryURLCrumbMapReader;

public class CloneConversionPixel extends ClonePixelLogBase  {

    private DecimalFormat dfFormatter = new DecimalFormat ("0.00"); // use to set basket_value

    public CloneConversionPixel () {
    }

    public int clonePixel (PixelLog.Builder pixelLogBuilder, 
                           ProcessedFeed processedFeed, OrderIdGenerator orderIdGenerator, 
                           ProductURLPidMapReader productUrlPidMapReader, CategoryURLCrumbMapReader catUrlCrumbMapReader) {
    
        // let base class update 'common' fields
        int cloneStatus;

        cloneStatus = cloneCommonFields (pixelLogBuilder, processedFeed, productUrlPidMapReader, catUrlCrumbMapReader);
        if (cloneStatus == CloneConstants.CLONE_STATUS_OK) { 
            double totalBasketValue = 0.0;
            String orderId;

            Basket srcBasket = pixelLogBuilder.getBasket ();
            if (srcBasket == null) {
                System.out.println ("Conversion pixel has no basket");
                return (CloneConstants.CLONE_STATUS_ERROR);
            }

            Basket.Builder cloneBasketBuilder = srcBasket.newBuilder ();
            List<Item> basketItemList = srcBasket.getItemList (); // items in the "src basket"
            for (int i = 0; i < basketItemList.size (); i++) {
                Item basketItem;
                Basket.Item.Builder cloneItemBuilder;

                basketItem = basketItemList.get (i);

                // note - cloneBasketItem method returns a 'builder' (not the item itself)
                cloneItemBuilder = cloneBasketItem (basketItem, processedFeed);
                if (cloneItemBuilder != null) {
                    double quantity;
                    double price;

                    cloneBasketBuilder.addItem (cloneItemBuilder); 

                    // update total basket value
                    quantity = cloneItemBuilder.getQuantity ();
                    price = cloneItemBuilder.getPrice ();
                    totalBasketValue = totalBasketValue + (quantity * price);
                } else {
                    continue; // error already reported
                }
            } 

            // if there items in the cloneBasket, add it to the pixelLog
            List<Item> cloneBasketItemList = cloneBasketBuilder.getItemList ();
            if ((cloneBasketItemList != null) && (cloneBasketItemList.size () > 0)) {

                // set the 'Basket" in the pixelLogBuilder
                pixelLogBuilder.setBasket (cloneBasketBuilder.build());

                // update total basket value in conversion pixel
                pixelLogBuilder.setBasketValue (totalBasketValue);
                // set 'basket_value' param as well
                replacePixelLogParam (pixelLogBuilder, "basket_value", dfFormatter.format (totalBasketValue));

                // temporarily, set "basket" param to blank value
                // In reality we will need to construct a string similar to the 'basket' string in pixel
                replacePixelLogParam (pixelLogBuilder, "basket", "");
 
                cloneStatus = CloneConstants.CLONE_STATUS_OK;
            } else {
                System.out.println ("Basket is empty in clone conversion pixel");
                cloneStatus = CloneConstants.CLONE_STATUS_REJECT;
            }

            // update url, orderId if cloneStatus so far is "OK"
            if (cloneStatus == CloneConstants.CLONE_STATUS_OK) {
                // conversion page url 
                // url 
                pixelLogBuilder.setUrl (CloneConstants.CHECKOUT_PAGE_URL); 
                replacePixelLogParam (pixelLogBuilder, "url", CloneConstants.CHECKOUT_PAGE_URL);

                // orderId
                // Apparently worldmarket itself has duplicate orderIds. BR analytics drops such duplicates
                orderId = orderIdGenerator.allocateOrderId ();
                pixelLogBuilder.setOrderId (orderId);
                replacePixelLogParam (pixelLogBuilder, "order_id", orderId);
                // System.out.printf ("DEBUG order_id: %s\n", orderId);
            }
        }

        return (cloneStatus);
    }

    private Basket.Item.Builder cloneBasketItem (Basket.Item srcBasketItem, ProcessedFeed processedFeed) {
        String srcProdId;
        String srcProdName;
        String translatedProdId;
        String productPrice;
        double priceAsDouble;
        Basket.Item.Builder cloneBasketItemBuilder;

        // for each basket.item...

        // prodId
        srcProdId = srcBasketItem.getProdId ();
        if (StringUtils.isEmpty (srcProdId) == true) {
            System.out.println ("basket item has no prodid ... skipping");
            return (null);
        }

        translatedProdId = generateUniqPid (srcProdId);
        if (translatedProdId == null) {
            System.out.println ("Conversion product not in processed feed");
            return (null);
        }

        // prodName
        srcProdName = srcBasketItem.getProdName ();
        if (StringUtils.isNotEmpty (srcProdName) == true) {
            if (srcProdName.indexOf ("World Market") >= 0) {
                srcProdName = srcProdName.replaceAll ("World Market", "PacificHome");
            }
        }

        // this product's price in the processedFeed
        productPrice = processedFeed.lookupProductPrice (translatedProdId);
        if (productPrice != null) {
            try {
                priceAsDouble = Double.parseDouble (productPrice);
            } catch (NumberFormatException nfe) {
                System.out.println ("Basket item price not a valid number: " + productPrice);
                return (null);
            }
        } else {
            System.out.println ("Basket item price not found in processedFeed: prodId = " + translatedProdId);
            return (null);
        }


        // create a new Basket.Item.Builder, set necessary values in it and
        // then add the item in cloneBasket. Note that the "item.builder" itself is
        // added to basketBuilder. (not the item, but the item.builder)
        cloneBasketItemBuilder = Basket.Item.newBuilder (srcBasketItem);
        cloneBasketItemBuilder.setProdId (translatedProdId);
        cloneBasketItemBuilder.setPrice (priceAsDouble);
        cloneBasketItemBuilder.setProdName  (srcProdName);

        return (cloneBasketItemBuilder);
    }
}


