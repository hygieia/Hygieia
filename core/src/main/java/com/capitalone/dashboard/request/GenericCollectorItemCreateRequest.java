package com.capitalone.dashboard.request;

import javax.validation.constraints.NotNull;

/**
 * A request to create a Generic Collector Item.
 *
 */
public class GenericCollectorItemCreateRequest {
    private String hygieiaId; //A collector item id in hygieia for linking things
    @NotNull
    private String toolName;
    @NotNull
    private String rawData;
    @NotNull
    private String source;


    public String getHygieiaId() {
        return hygieiaId;
    }

    public void setHygieiaId(String hygieiaId) {
        this.hygieiaId = hygieiaId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
