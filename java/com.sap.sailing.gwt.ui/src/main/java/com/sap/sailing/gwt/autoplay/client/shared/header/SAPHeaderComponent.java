package com.sap.sailing.gwt.autoplay.client.shared.header;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SailingHeaderWithAuthentication;
import com.sap.sailing.gwt.common.client.FullscreenUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.shared.components.AbstractCompositeComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

public class SAPHeaderComponent extends AbstractCompositeComponent<SAPHeaderComponentSettings> {
    private SAPHeaderComponentSettings settings;
    private final SAPHeaderComponentLifecycle componentLifecycle;
    
    private final SailingHeaderWithAuthentication sapHeader;
    
    public SAPHeaderComponent(Component<?> parent, ComponentContext<?> context,
            SAPHeaderComponentLifecycle componentLifecycle,
            UserService userService, PaywallResolver paywallResolver, SAPHeaderComponentSettings settings,
            StringMessages stringMessages, boolean startInAutoScreenMode) {
        super(parent, context);
        this.componentLifecycle = componentLifecycle;
        this.settings = settings;
        this.sapHeader = new SailingHeaderWithAuthentication(settings.getTitle());
        new FixedSailingAuthentication(userService, paywallResolver, sapHeader.getAuthenticationMenuView());

        initWidget(sapHeader);
        
        if (startInAutoScreenMode) {
            Scheduler.get().scheduleFixedPeriod(new RepeatingCommand() {
                public boolean execute () {
                    FullscreenUtil.requestFullscreen();
                    return false;
                }
              }, 1000);
        }
    }
    
    @Override
    public String getLocalizedShortName() {
        return componentLifecycle.getLocalizedShortName();
    }
    
    @Override
    public Widget getEntryWidget() {
        return this;
    }
    
    @Override
    public boolean hasSettings() {
        return componentLifecycle.hasSettings();
    }
    
    @Override
    public SAPHeaderComponentSettingsDialogComponent getSettingsDialogComponent(SAPHeaderComponentSettings settings) {
        return componentLifecycle.getSettingsDialogComponent(settings);
    }
    
    @Override
    public SAPHeaderComponentSettings getSettings() {
        return settings;
    }
    
    @Override
    public void updateSettings(SAPHeaderComponentSettings newSettings) {
        this.settings = newSettings;
        sapHeader.setHeaderTitle(settings.getTitle());
    }
    
    @Override
    public String getDependentCssClassName() {
        return "";
    }

    @Override
    public String getId() {
        return componentLifecycle.getComponentId();
    }
}
