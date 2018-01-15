package com.capitalone.dashboard.evaluator;

import com.capitalone.dashboard.ApiSettings;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AuditException;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.Widget;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.DashboardRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class Evaluator<T> {

    @Autowired
    protected ComponentRepository componentRepository;

    @Autowired
    protected DashboardRepository dashboardRepository;

    @Autowired
    protected ApiSettings settings;


    public  abstract Collection<T> evaluate(Dashboard dashboard, long beginDate, long endDate, Map<?, ?> data) throws AuditException;


    public abstract T evaluate(CollectorItem collectorItem, long beginDate, long endDate, Map<?, ?> data) throws AuditException, HygieiaException;

    /**
     * @param dashboard the dashboard
     * @param widgetName the widget name
     * @param collectorType the collector type
     * @return list of @CollectorItem for a given dashboard, widget name and collector type
     */
    List<CollectorItem> getCollectorItems(Dashboard dashboard, String widgetName, CollectorType collectorType) {
        List<Widget> widgets = dashboard.getWidgets();
        ObjectId componentId = widgets.stream().filter(widget -> widget.getName().equalsIgnoreCase(widgetName)).findFirst().map(Widget::getComponentId).orElse(null);

        if (componentId == null) return null;

        com.capitalone.dashboard.model.Component component = componentRepository.findOne(componentId);

        return component.getCollectorItems().get(collectorType);
    }


    public Dashboard getDashboard(String businessService, String businessComponent) {
        return dashboardRepository.findDashboardByConfigurationItemBusServNameAndConfigurationItemBusAppName(businessService, businessComponent);
    }
}
