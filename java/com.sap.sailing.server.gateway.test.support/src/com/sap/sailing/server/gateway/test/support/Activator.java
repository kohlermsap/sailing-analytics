package com.sap.sailing.server.gateway.test.support;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.branding.BrandingConfigurationService;
import com.sap.sse.util.ServiceTrackerFactory;

public class Activator implements BundleActivator {
    private static Activator INSTANCE;
    private ServiceTracker<BrandingConfigurationService, BrandingConfigurationService> brandingConfigurationServiceTracker;
    
    @Override
    public void start(BundleContext context) throws Exception {
        INSTANCE = this;
        brandingConfigurationServiceTracker = ServiceTrackerFactory.createAndOpen(context, BrandingConfigurationService.class);
    }

    static BrandingConfigurationService getBrandingConfigurationService() {
        final BrandingConfigurationService result;
        if (INSTANCE == null || INSTANCE.brandingConfigurationServiceTracker == null) {
            result = null;
        } else {
            result = INSTANCE.brandingConfigurationServiceTracker.getService();
        }
        return result;
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
