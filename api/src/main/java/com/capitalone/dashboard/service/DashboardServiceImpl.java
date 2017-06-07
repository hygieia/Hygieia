package com.capitalone.dashboard.service;

import com.capitalone.dashboard.auth.AuthenticationUtil;
import com.capitalone.dashboard.auth.exceptions.DeleteLastAdminException;
import com.capitalone.dashboard.auth.exceptions.UserNotFoundException;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.*;
import com.capitalone.dashboard.util.UnsafeDeleteException;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;
    private final ComponentRepository componentRepository;
    private final CollectorRepository collectorRepository;
    private final CollectorItemRepository collectorItemRepository;
    private final CustomRepositoryQuery customRepositoryQuery;
    @SuppressWarnings("unused")
    private final PipelineRepository pipelineRepository; //NOPMD
    private final ServiceRepository serviceRepository;
    private final UserInfoRepository userInfoRepository;
    private final CmdbService cmdbService;

    @Autowired
    public DashboardServiceImpl(DashboardRepository dashboardRepository,
                                ComponentRepository componentRepository,
                                CollectorRepository collectorRepository,
                                CollectorItemRepository collectorItemRepository,
                                CustomRepositoryQuery customRepositoryQuery,
                                ServiceRepository serviceRepository,
                                PipelineRepository pipelineRepository,
                                UserInfoRepository userInfoRepository,
                                CmdbService cmdbService) {
        this.dashboardRepository = dashboardRepository;
        this.componentRepository = componentRepository;
        this.collectorRepository = collectorRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.customRepositoryQuery = customRepositoryQuery;
        this.serviceRepository = serviceRepository;
        this.pipelineRepository = pipelineRepository;   //TODO - Review if we need this param, seems it is never used according to PMD
        this.userInfoRepository = userInfoRepository;
        this.cmdbService = cmdbService;
    }

    @Override
    public Iterable<Dashboard> all() {
        Iterable<Dashboard> dashboards = dashboardRepository.findAll(new Sort(Sort.Direction.ASC, "title"));
        for(Dashboard dashboard: dashboards){
            ObjectId appObjectId = dashboard.getConfigurationItemAppObjectId();
            ObjectId compObjectId = dashboard.getConfigurationItemComponentObjectId();

            setAppAndComponentNameToDashboard(dashboard, appObjectId, compObjectId);
        }
        return dashboards;
    }

    @Override
    public Dashboard get(ObjectId id) {
        Dashboard dashboard = dashboardRepository.findOne(id);

        ObjectId appObjectId = dashboard.getConfigurationItemAppObjectId();
        ObjectId compObjectId = dashboard.getConfigurationItemComponentObjectId();

        setAppAndComponentNameToDashboard(dashboard, appObjectId, compObjectId);

        if (!dashboard.getApplication().getComponents().isEmpty()) {
            // Add transient Collector instance to each CollectorItem#2015

            Map<CollectorType, List<CollectorItem>> itemMap = dashboard.getApplication().getComponents().get(0).getCollectorItems();

            Iterable<Collector> collectors = collectorsFromItems(itemMap);

            for (List<CollectorItem> collectorItems : itemMap.values()) {
                for (CollectorItem collectorItem : collectorItems) {
                    collectorItem.setCollector(getCollector(collectorItem.getCollectorId(), collectors));
                }
            }
        }

        return dashboard;
    }
    @Override
    public DataResponse<Iterable<Dashboard>> getByApp(String app) {
        Cmdb cmdb =  cmdbService.configurationItemByConfigurationItem(app);

        Iterable<Dashboard> rt = dashboardRepository.findAllByConfigurationItemAppObjectId(cmdb.getId());

        return new DataResponse<>(rt, System.currentTimeMillis());
    }
    @Override
    public DataResponse<Iterable<Dashboard>> getByComponent(String component) {
        Cmdb cmdb =  cmdbService.configurationItemByConfigurationItem(component);

        Iterable<Dashboard> rt = dashboardRepository.findAllByConfigurationItemComponentObjectId(cmdb.getId());

        return new DataResponse<>(rt, System.currentTimeMillis());
    }
    @Override
    public DataResponse<Iterable<Dashboard>> getByComponentAndApp(String component, String app) {
        Cmdb cmdbCompItem =  cmdbService.configurationItemByConfigurationItem(component);
        Cmdb cmdbAppItem =  cmdbService.configurationItemByConfigurationItem(app);

        Iterable<Dashboard> rt = dashboardRepository.findAllByConfigurationItemAppObjectIdAndConfigurationItemComponentObjectId(cmdbAppItem.getId(),cmdbCompItem.getId());

        return new DataResponse<>(rt, System.currentTimeMillis());
    }
    public Dashboard create(Dashboard dashboard, boolean isUpdate) throws HygieiaException {
        Iterable<Component> components = null;

        if(!isUpdate) {
            components = componentRepository.save(dashboard.getApplication().getComponents());
        }

        try {

            duplicateDashboardErrorCheck(dashboard);
            return dashboardRepository.save(dashboard);

        } catch (Exception e) {
            //Exclude deleting of components if this is an update request
            if(!isUpdate) {
                componentRepository.delete(components);
            }

            if(e instanceof HygieiaException){
                throw e;
            }else{
                throw new HygieiaException("Failed creating dashboard.", HygieiaException.ERROR_INSERTING_DATA);
            }
        }
    }
    @Override
    public Dashboard create(Dashboard dashboard) throws HygieiaException {
        return create(dashboard, false);
    }
    @Override
    public Dashboard update(Dashboard dashboard) throws HygieiaException {
        return create(dashboard, true);
    }

    @Override
    public void delete(ObjectId id) {
        Dashboard dashboard = dashboardRepository.findOne(id);

        if (!isSafeDelete(dashboard)) {
            throw new UnsafeDeleteException("Cannot delete team dashboard " + dashboard.getTitle() + " as it is referenced by program dashboards.");
        }

        componentRepository.delete(dashboard.getApplication().getComponents());

        // Remove this Dashboard's services and service dependencies
        serviceRepository.delete(serviceRepository.findByDashboardId(id));
        for (com.capitalone.dashboard.model.Service service : serviceRepository.findByDependedBy(id)) { //NOPMD - using fully qualified or we pickup an incorrect spring class
            service.getDependedBy().remove(id);
            serviceRepository.save(service);
        }

        dashboardRepository.delete(dashboard);
    }

    private boolean isSafeDelete(Dashboard dashboard) {
        return !(dashboard.getType() == null || dashboard.getType().equals(DashboardType.Team)) || isSafeTeamDashboardDelete(dashboard);
    }

    private boolean isSafeTeamDashboardDelete(Dashboard dashboard) {
        boolean isSafe = false;
        List<Collector> productCollectors = collectorRepository.findByCollectorType(CollectorType.Product);
        if (productCollectors.isEmpty()) {
            return true;
        }

        Collector productCollector = productCollectors.get(0);

        CollectorItem teamDashboardCollectorItem = collectorItemRepository.findTeamDashboardCollectorItemsByCollectorIdAndDashboardId(productCollector.getId(), dashboard.getId().toString());

        //// TODO: 1/21/16 Is this safe? What if we add a new team dashbaord and quickly add it to a product and then delete it?
        if (teamDashboardCollectorItem == null) {
            return true;
        }

        if (dashboardRepository.findProductDashboardsByTeamDashboardCollectorItemId(teamDashboardCollectorItem.getId().toString()).isEmpty()) {
            isSafe = true;
        }
        return isSafe;
    }

    @Override
    public Component associateCollectorToComponent(ObjectId componentId, List<ObjectId> collectorItemIds) {
        if (componentId == null || collectorItemIds == null) {
            // Not all widgets gather data from collectors
            return null;
        }

        com.capitalone.dashboard.model.Component component = componentRepository.findOne(componentId); //NOPMD - using fully qualified name for clarity
        //we can not assume what collector item is added, what is removed etc so, we will
        //refresh the association. First disable all collector items, then remove all and re-add

        //First: disable all collectorItems of the Collector TYPEs that came in with the request.
        //Second: remove all the collectorItem association of the Collector Type  that came in
        HashSet<CollectorType> incomingTypes = new HashSet<>();
        HashMap<ObjectId, CollectorItem> toSaveCollectorItems = new HashMap<>();
        for (ObjectId collectorItemId : collectorItemIds) {
            CollectorItem collectorItem = collectorItemRepository.findOne(collectorItemId);
            Collector collector = collectorRepository.findOne(collectorItem.getCollectorId());
            if (!incomingTypes.contains(collector.getCollectorType())) {
                incomingTypes.add(collector.getCollectorType());
                List<CollectorItem> cItems = component.getCollectorItems(collector.getCollectorType());
                // Save all collector items as disabled for now
                if (!CollectionUtils.isEmpty(cItems)) {
                    for (CollectorItem ci : cItems) {
                        //if item is orphaned, disable it. Otherwise keep it enabled.
                        ci.setEnabled(!isLonely(ci, collector, component));
                        toSaveCollectorItems.put(ci.getId(), ci);
                    }
                }
                // remove all collector items of a type
                component.getCollectorItems().remove(collector.getCollectorType());
            }
        }

        //Last step: add collector items that came in
        for (ObjectId collectorItemId : collectorItemIds) {
            CollectorItem collectorItem = collectorItemRepository.findOne(collectorItemId);
            //the new collector items must be set to true
            collectorItem.setEnabled(true);
            Collector collector = collectorRepository.findOne(collectorItem.getCollectorId());
            component.addCollectorItem(collector.getCollectorType(), collectorItem);
            toSaveCollectorItems.put(collectorItemId, collectorItem);
            // set transient collector property
            collectorItem.setCollector(collector);
        }

        Set<CollectorItem> deleteSet = new HashSet<>();
        for (ObjectId id : toSaveCollectorItems.keySet()) {
            deleteSet.add(toSaveCollectorItems.get(id));
        }
        collectorItemRepository.save(deleteSet);
        componentRepository.save(component);
        return component;
    }


    private boolean isLonely(CollectorItem item, Collector collector, Component component) {
        List<Component> components = customRepositoryQuery.findComponents(collector, item);
        //if item is not attached to any component, it is orphaned.
        if (CollectionUtils.isEmpty(components)) return true;
        //if item is attached to more than 1 component, it is NOT orphaned
        if (components.size() > 1) return false;
        //if item is attached to ONLY 1 component it is the current one, it is going to be orphaned after this
        return (components.get(0).getId().equals(component.getId()));
    }

    @Override
    public Widget addWidget(Dashboard dashboard, Widget widget) {
        widget.setId(ObjectId.get());
        dashboard.getWidgets().add(widget);
        dashboardRepository.save(dashboard);
        return widget;
    }

    @Override
    public Widget getWidget(Dashboard dashboard, ObjectId widgetId) {
        return Iterables.find(dashboard.getWidgets(), new WidgetByIdPredicate(widgetId));
    }

    @Override
    public Widget updateWidget(Dashboard dashboard, Widget widget) {
        int index = dashboard.getWidgets().indexOf(widget);
        dashboard.getWidgets().set(index, widget);
        dashboardRepository.save(dashboard);
        return widget;
    }

    private static final class WidgetByIdPredicate implements Predicate<Widget> {
        private final ObjectId widgetId;

        public WidgetByIdPredicate(ObjectId widgetId) {
            this.widgetId = widgetId;
        }

        @Override
        public boolean apply(Widget widget) {
            return widget.getId().equals(widgetId);
        }
    }

    private Iterable<Collector> collectorsFromItems(Map<CollectorType, List<CollectorItem>> itemMap) {
        Set<ObjectId> collectorIds = new HashSet<>();
        for (List<CollectorItem> collectorItems : itemMap.values()) {
            for (CollectorItem collectorItem : collectorItems) {
                collectorIds.add(collectorItem.getCollectorId());
            }
        }

        return collectorRepository.findAll(collectorIds);
    }

    private Collector getCollector(final ObjectId collectorId, Iterable<Collector> collectors) {
        return Iterables.tryFind(collectors, new Predicate<Collector>() {
            @Override
            public boolean apply(Collector collector) {
                return collector.getId().equals(collectorId);
            }
        }).orNull();
    }

	@Override
	public List<Dashboard> getOwnedDashboards() {
		Set<Dashboard> myDashboards = new HashSet<Dashboard>();

		Owner owner = new Owner(AuthenticationUtil.getUsernameFromContext(), AuthenticationUtil.getAuthTypeFromContext());
        List<Dashboard> findByOwnersList = dashboardRepository.findByOwners(owner);
		getAppAndComponentNames(findByOwnersList);
        myDashboards.addAll(findByOwnersList);
       
		// TODO: This if check is to ensure backwards compatibility for dashboards created before AuthenticationTypes were introduced.
		if (AuthenticationUtil.getAuthTypeFromContext() == AuthType.STANDARD) {

            List<Dashboard> findByOwnersListOld = dashboardRepository.findByOwner(AuthenticationUtil.getUsernameFromContext());
            getAppAndComponentNames(findByOwnersListOld);
			myDashboards.addAll(findByOwnersListOld);
		}
		
		return Lists.newArrayList(myDashboards);
	}

    @Override
    public Iterable<UserInfo> getAllUsers() {
        return userInfoRepository.findByOrderByUsernameAsc();
    }

    @Override
    public Iterable<Owner> getOwners(ObjectId id) {
        Dashboard dashboard = get(id);
        return dashboard.getOwners();
    }

    @Override
    public UserInfo promoteToOwner(ObjectId dashboardId, String username, AuthType authType) {
        Dashboard dashboard = dashboardRepository.findOne(dashboardId);
        List<Owner> owners = dashboard.getOwners();
        Owner promotedOwner = new Owner(username, authType);
        owners.add(promotedOwner);
        dashboardRepository.save(dashboard);

        UserInfo user = userInfoRepository.findByUsernameAndAuthType(username, authType);
        if (user == null) {
            throw new UserNotFoundException(username, authType);
        }
        user.getAuthorities().add(UserRole.ROLE_ADMIN);

        return user;
    }

    @Override
    public UserInfo demoteFromOwner(ObjectId dashboardId, String username, AuthType authType) {
        Dashboard dashboard = dashboardRepository.findOne(dashboardId);
        int numberOfOwners = dashboard.getOwners().size();

        //get admin users
        Collection<UserInfo> adminUsers = userInfoRepository.findByAuthoritiesIn(UserRole.ROLE_ADMIN);

        numberOfOwners += adminUsers.size();

        if(numberOfOwners <= 1) {
            throw new DeleteLastAdminException();
        }

        Owner demotedOwner = new Owner(username, authType);
        dashboard.getOwners().remove(demotedOwner);
        dashboardRepository.save(dashboard);

        UserInfo user = userInfoRepository.findByUsernameAndAuthType(username, authType);
        if (user == null) {
            throw new UserNotFoundException(username, authType);
        }

        user.getAuthorities().remove(UserRole.ROLE_ADMIN);
        return user;

    }

	@Override
	public String getDashboardOwner(String dashboardTitle) {

		String dashboardOwner=dashboardRepository.findByTitle(dashboardTitle).get(0).getOwner();
		
		return dashboardOwner;
	}

    @SuppressWarnings("unused")
    private DashboardType getDashboardType(Dashboard dashboard) {
        if (dashboard.getType() != null) {
            return dashboard.getType();
        }
        return DashboardType.Team;
    }

    @Override
    public Component getComponent(ObjectId componentId){

        Component component = componentRepository.findOne(componentId);
        return component;
    }

    private void getAppAndComponentNames(List<Dashboard> findByOwnersList) {
        for(Dashboard dashboard: findByOwnersList){


            ObjectId appObjectId = dashboard.getConfigurationItemAppObjectId();
            ObjectId compObjectId = dashboard.getConfigurationItemComponentObjectId();
            setAppAndComponentNameToDashboard(dashboard, appObjectId, compObjectId);
        }
    }

    /**
     *  Sets business service, business application and valid flag for each to the give Dashboard
     * @param dashboard
     * @param appObjectId
     * @param compObjectId
     */
    private void setAppAndComponentNameToDashboard(Dashboard dashboard, ObjectId appObjectId, ObjectId compObjectId) {
        if(appObjectId != null && !"".equals(appObjectId)){

            Cmdb cmdb =  cmdbService.configurationItemsByObjectId(appObjectId);
            dashboard.setConfigurationItemAppName(cmdb.getConfigurationItem());
            dashboard.setValidAppName(cmdb.isValidConfigItem());
        }
        if(compObjectId != null && !"".equals(compObjectId)){
            Cmdb cmdb = cmdbService.configurationItemsByObjectId(compObjectId);
            dashboard.setConfigurationItemCompName(cmdb.getConfigurationItem());
            dashboard.setValidCompName(cmdb.isValidConfigItem());
        }
    }

    /**
     *  Takes Dashboard and checks to see if there is an existing Dashboard with the same business service and business application
     *  Throws error if found
     * @param dashboard
     * @throws HygieiaException
     */
    private void duplicateDashboardErrorCheck(Dashboard dashboard) throws HygieiaException {
        ObjectId appObjectId = dashboard.getConfigurationItemAppObjectId();
        ObjectId compObjectId = dashboard.getConfigurationItemComponentObjectId();

        if(appObjectId != null && compObjectId != null){
            Dashboard existingDashboard = dashboardRepository.findByConfigurationItemAppObjectIdAndConfigurationItemComponentObjectId(appObjectId, compObjectId);
            if(existingDashboard != null && !existingDashboard.getId().equals(dashboard.getId())){
                throw new HygieiaException("Existing Dashboard: " + existingDashboard.getTitle(), HygieiaException.DUPLICATE_DATA);
            }
        }
    }
}
