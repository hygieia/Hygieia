package com.capitalone.dashboard.service;

import com.capitalone.dashboard.model.AuditStatus;
import org.springframework.stereotype.Component;

@Component
public interface AuditStatusService {

    /**
     * Fetches audit status for all the dashboards, sorted by timestamp.
     *
     * @return all dashboardAuditStatuses
     */
    Iterable<AuditStatus> all();
}
