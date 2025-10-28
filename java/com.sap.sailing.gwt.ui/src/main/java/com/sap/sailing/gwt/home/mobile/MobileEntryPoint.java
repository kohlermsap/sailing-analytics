package com.sap.sailing.gwt.home.mobile;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.mobile.app.MobileActivityMapper;
import com.sap.sailing.gwt.home.mobile.app.MobileApplicationClientFactory;
import com.sap.sailing.gwt.home.shared.SwitchingEntryPoint;
import com.sap.sailing.gwt.home.shared.app.ApplicationHistoryMapper;
import com.sap.sailing.gwt.home.shared.app.ResettableNavigationPathDisplay;
import com.sap.sailing.gwt.home.shared.app.SailingActivityManager;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceHelper;
import com.sap.sailing.gwt.ui.client.ServerConfigurationService;
import com.sap.sailing.gwt.ui.client.ServerConfigurationServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.landscape.common.RemoteServiceMappingConstants;
import com.sap.sse.gwt.client.EntryPointHelper;
import com.sap.sse.gwt.client.ServiceRoutingProvider;
import com.sap.sse.gwt.client.mvp.AbstractMvpEntryPoint;
import com.sap.sse.gwt.resources.CommonControlsCSS;

/**
 * Smartphone EntryPoint for the Home module. Be aware that this EntryPoint isn't directly started but instead the
 * {@link SwitchingEntryPoint} decides about which version of the application will be shown.
 */
public class MobileEntryPoint extends AbstractMvpEntryPoint<StringMessages, MobileApplicationClientFactory> {

    private SailingServiceAsync sailingService;

    @Override
    public void doOnModuleLoad() {
        Document.get().getBody().addClassName(SharedResources.INSTANCE.mainCss().mobile());
        CommonControlsCSS.ensureInjected();
        ServerConfigurationServiceAsync serverConfigService = GWT.create(ServerConfigurationService.class);
        EntryPointHelper.registerASyncService((ServiceDefTarget) serverConfigService, RemoteServiceMappingConstants.serverConfigurationServiceRemotePath);
        serverConfigService.isStandaloneServer(new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                createMobileApplication(result);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                createMobileApplication(false);
            }
        });
    }

    private void createMobileApplication(boolean isStandaloneServer) {
        final MobileApplicationClientFactory clientFactory = new MobileApplicationClientFactory(isStandaloneServer,
                getSailingService());
        ApplicationHistoryMapper applicationHistoryMapper = GWT.create(ApplicationHistoryMapper.class);
        initMvp(clientFactory, applicationHistoryMapper, new MobileActivityMapper(clientFactory));
    }

    @Override
    protected StringMessages createStringMessages() {
        return GWT.create(StringMessages.class);
    }
    
    @Override
    protected ActivityManager createActivityManager(ActivityMapper activityMapperRegistry, MobileApplicationClientFactory clientFactory) {
        SailingActivityManager<ResettableNavigationPathDisplay> sailingActivityManager = new SailingActivityManager<>(activityMapperRegistry, clientFactory.getEventBus());
        sailingActivityManager.setNavigationPathDisplay(clientFactory.getNavigationPathDisplay());
        return sailingActivityManager;
    }

    private SailingServiceAsync getSailingService() {
        if (sailingService == null) {
            if (this instanceof ServiceRoutingProvider) {
                sailingService = SailingServiceHelper.createSailingServiceInstance((ServiceRoutingProvider) this);
            } else {
                sailingService = SailingServiceHelper.createSailingServiceInstance();
            }
        }
        return sailingService;
    }

}
