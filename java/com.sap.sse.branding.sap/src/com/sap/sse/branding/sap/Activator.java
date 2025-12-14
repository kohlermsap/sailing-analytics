package com.sap.sse.branding.sap;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sse.branding.BrandingConfigurationService;
import com.sap.sse.branding.shared.BrandingConfiguration;

public class Activator implements BundleActivator {
    public void start(BundleContext bundleContext) throws Exception {
        final Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put(BrandingConfigurationService.BRANDING_ID_PROPERTY_NAME, SAPBrandingConfiguration.ID);
        bundleContext.registerService(BrandingConfiguration.class, new SAPBrandingConfiguration(), dict);
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }
}
