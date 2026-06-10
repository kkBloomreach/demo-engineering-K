package com.bloomreach.trafficgenerator.site.discoveryconnector.useraccess;

import org.json.JSONObject;

// metadata returned in api response for WidgetApi call
public class WidgetResponseMetadata {
    private String id;  // wigetId
    private String name;
    private String description;
    private String type;
    private String rid;

    public WidgetResponseMetadata () {
    }

    public void setResponseJson (JSONObject responseJson) {
        // parse responseJson -> "widget"
        if (responseJson.has ("widget")) {
            JSONObject widgetMetadata;

            widgetMetadata = responseJson.getJSONObject ("widget");
            this.id = widgetMetadata.getString ("id");
            this.name = widgetMetadata.getString ("name");
            this.description = widgetMetadata.getString ("description");
            this.type = widgetMetadata.getString ("type");
            this.rid = widgetMetadata.getString ("rid");
        }
    }

    public String getId () {
        return this.id;
    }

    public String getName () {
        return this.name;
    }

    public String getDescription () {
        return this.description;
    }

    public String getType () {
        return this.type;
    } 

    public String getRid () {
        return this.rid;
    }
}
