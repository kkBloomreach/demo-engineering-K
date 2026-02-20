package com.bloomreach.trafficgenerator.site.feed;

public class DailyProductFeed extends ProductJsonlFeed {

    public DailyProductFeed () {
    }

    // override base class method
    // filepath to jsonl feed
    public void load (String productFilePath) throws Exception
    {
        super.load (productFilePath);
    }
}

