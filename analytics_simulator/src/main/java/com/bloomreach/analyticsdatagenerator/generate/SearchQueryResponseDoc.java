package com.bloomreach.analyticsdatagenerator.generate;

public class SearchQueryResponseDoc {

    private String pid;
    private String variant; // only one variant. May be null if product has no variant
    private double price;
    private String brand;
    private String title;

    public SearchQueryResponseDoc (String pid, String variant, double price, String brand, String title) {
        this.pid = pid;
        this.price = price;
        this.brand = brand;
        this.title = title;
        this.variant = variant;
    }

    public String getPid () {
        return this.pid;
    }

    public String getVariant () {
        return this.variant;
    }

    public double getPrice () {
        return this.price;
    }

    public String getBrand () {
        return this.brand;
    }

    public String getTitle () {
        return this.title;
    }
}
