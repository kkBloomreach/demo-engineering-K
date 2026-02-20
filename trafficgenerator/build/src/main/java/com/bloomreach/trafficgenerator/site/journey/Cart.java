package com.bloomreach.trafficgenerator.site.journey;

import java.util.ArrayList;

public class Cart {

    private String userId;
    private ArrayList <CartItem> items;

    public Cart () {
        this.items = new ArrayList <CartItem> ();
    }

    public void setUserId (String userId) {
        this.userId = userId;
    }

    public void addItem (CartItem item) {
        this.items.add (item);
    }

    public String getUserId () {
        return this.userId;
    }

    public ArrayList<CartItem> getItems () {
        return this.items;
    }

    public void empty () {
        this.items.clear ();
    }
}
