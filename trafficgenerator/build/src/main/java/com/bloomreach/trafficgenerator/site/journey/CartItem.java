package com.bloomreach.trafficgenerator.site.journey;


public class CartItem {

    private ProductDetails item;
    private int quantity;

    public CartItem () {
    }

    public void setItem (ProductDetails productDetails) {
        this.item = productDetails;
    }

    public void setQuantity (int quantity) {
        this.quantity = quantity;
    }

    public ProductDetails getItem () {
        return this.item;
    }

    public int getQuantity () {
        return this.quantity;
    }
}

