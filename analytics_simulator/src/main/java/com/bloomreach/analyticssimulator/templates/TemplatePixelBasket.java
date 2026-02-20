package com.bloomreach.analyticssimulator.templates;

import com.bloomreach.proto.Aggregation.Basket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

public class TemplatePixelBasket {

    // use 'builder' method to construct object
    public TemplatePixelBasket() {
    }

    public Basket.Builder parseBasket(LineIterator useLineIterator) {
        return parseBasket(useLineIterator, false);
    }

    public Basket.Builder parseBasket(LineIterator useLineIterator, boolean throwException) {
        Basket.Builder templateBasketBuilder = Basket.newBuilder();

        // basket format in clone log file:
        // basket {
        //  item {
        //     prod_id: ...
        //     ...
        //  }
        //  item {
        //     ...
        //  }
        //  ...
        // }
        // The "basket {" line has already been read-in
        while (useLineIterator.hasNext ()) {
            String line;

            line = useLineIterator.nextLine ();
            // if all items have been processed
            if (line.indexOf ('}') >= 0)
                break;

            if (line.indexOf ("item") >= 0) {
                Basket.Item.Builder basketItem = parseOneItem (useLineIterator);
                if (basketItem != null)
                    templateBasketBuilder.addItem (basketItem);
            }
        }

        return templateBasketBuilder;
    }


    // "item {" line is already read-in
    private Basket.Item.Builder parseOneItem (LineIterator useLineIterator) {

        Basket.Item.Builder basketItemBuilder = Basket.Item.newBuilder ();

        // set defaults for all expected params
        basketItemBuilder.setProdId ("(not set)");
        basketItemBuilder.setQuantity (0.0);   // double value
        basketItemBuilder.setProdName ("(not set)");
        basketItemBuilder.setProdSku ("(not set)");
        basketItemBuilder.setPrice (0.0);  // double value

        while (useLineIterator.hasNext ()) {
            String line = useLineIterator.nextLine ();
            if (line.indexOf ('}') >= 0)    //end of one item
                break;

            int indx = line.indexOf (':');
            String keyName = line.substring (0, indx).trim ();
            String value = line.substring (indx+1).trim ();
            value = value.replaceAll ("\"", "");
            if (keyName.equals ("prod_id") == true) {
                basketItemBuilder.setProdId (value);
            }
            else if (keyName.equals ("quantity") == true) {
                double quantity;

                // Some merchants might pass the basket quantity using grouping separators, e.g. 2,718.28
                // Therefore remove ',' (currently only comma)
                if (value.indexOf (',') >= 0)
                    value = value.replace (",", "");
                try {
                    quantity = Double.parseDouble (value);
                    basketItemBuilder.setQuantity(quantity);
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException ("incorrect quantity: " + value);
                }
            }
            else if (keyName.equals ("prod_name") == true) {
                basketItemBuilder.setProdName (value);
            }
            else if (keyName.equals ("prod_sku") == true) {
                basketItemBuilder.setProdSku (value);
            }
            else if (keyName.equals ("price") == true) {
                double price;

                // Some merchants might pass the price using grouping separators, e.g. 2,718.28
                // Therefore remove ',' (currently only comma)
                if (value.indexOf (',') >= 0)
                    value = value.replace (",", "");
                try {
                    price = Double.parseDouble (value);
                    basketItemBuilder.setPrice (price);
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException ("incorrect price: " + value);
                }
            }
        }

        return basketItemBuilder;
    }
}

