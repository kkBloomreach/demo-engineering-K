package com.bloomreach.trafficgenerator.site.feed;

public class OriginalProductFeed extends ProductJsonlFeed {

    public OriginalProductFeed () {
    }

    // override base class method
    // filepath to jsonl feed
    public void load (String productFilePath) throws Exception
    {
        super.load (productFilePath);
    }
}

