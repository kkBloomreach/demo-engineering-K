package com.bloomreach.trafficgenerator.site.build.pixelparams;

// given a year, month, date, sequentially allocate orderIds for that day

public class OrderIdGenerator {

    private final static int FIRST_ORDER_ID = 1;

    private int nextOrderId;
    private int year;
    private int month;
    private int day;
 
    public OrderIdGenerator () {
    }

    public void setDate (int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;

        nextOrderId = FIRST_ORDER_ID;
    }

    public String allocateOrderId () {
        String orderId;

        orderId = new String (this.year + "_" + this.month + "_" + this.day + "_" + nextOrderId);
        nextOrderId++;

        return (orderId);
    }
}
