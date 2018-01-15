package com.capitalone.dashboard.response;

import com.capitalone.dashboard.model.CollectorItemConfigHistory;
import com.capitalone.dashboard.status.BuildAuditStatus;

import java.util.List;

public class BuildAuditResponse extends AuditReviewResponse<BuildAuditStatus> {
    private String environment;
    private List<CollectorItemConfigHistory> configHistory;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<CollectorItemConfigHistory> getConfigHistory() {
        return configHistory;
    }

    public void setConfigHistory(List<CollectorItemConfigHistory> configHistory) {
        this.configHistory = configHistory;
    }
}
