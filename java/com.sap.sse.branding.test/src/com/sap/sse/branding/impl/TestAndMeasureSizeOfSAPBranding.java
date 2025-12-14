package com.sap.sse.branding.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sse.branding.BrandingConfigurationService;
import com.sap.sse.branding.sap.SAPBrandingConfiguration;
import com.sap.sse.branding.shared.BrandingConfiguration;
import com.sap.sse.testutils.Measurement;
import com.sap.sse.testutils.MeasurementCase;
import com.sap.sse.testutils.MeasurementXMLFile;

public class TestAndMeasureSizeOfSAPBranding {
    private BrandingConfiguration brandingConfiguration;
    private BrandingConfigurationService brandingConfigurationService;
    private static MeasurementXMLFile measurementFile;
    private static MeasurementCase measurementCase;
    
    @BeforeAll
    public static void beforeAll() {
        measurementFile = new MeasurementXMLFile(TestAndMeasureSizeOfSAPBranding.class);
        measurementCase = measurementFile.addCase(TestAndMeasureSizeOfSAPBranding.class.getSimpleName());
    }
    
    @AfterAll
    public static void afterAll() throws IOException {
        measurementFile.write();
    }
    
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws InvalidSyntaxException {
        final BundleContext dummyBundleContext = mock(BundleContext.class);
        final Filter dummyFilter = mock(Filter.class);
        when(dummyFilter.match((Dictionary<String, ?>) ArgumentMatchers.any())).thenReturn(true);
        when(dummyBundleContext.createFilter(ArgumentMatchers.anyString())).thenReturn(dummyFilter);
        brandingConfiguration = new SAPBrandingConfiguration();
        final ServiceTracker<BrandingConfiguration, BrandingConfiguration> tracker = mock(ServiceTracker.class);
        when(tracker.getService()).thenReturn(brandingConfiguration);
        when(tracker.getService(ArgumentMatchers.any(ServiceReference.class))).thenReturn(brandingConfiguration);
        final ServiceReference<BrandingConfiguration>[] serviceReferences = new ServiceReference[] { mock(ServiceReference.class) };
        when(tracker.getServiceReferences()).thenReturn(serviceReferences);
        brandingConfigurationService = new BrandingConfigurationServiceImpl(dummyBundleContext, tracker);
        brandingConfigurationService.setActiveBrandingConfigurationById(brandingConfiguration.getId());
    }
    
    @Test
    public void testBrandingServiceAvailability() {
        assertEquals(brandingConfiguration.getId(), brandingConfigurationService.getActiveBrandingConfiguration().getId());
    }
    
    @Test
    public void measureBrandingClientScriptSize() {
        final Map<String, Object> brandingProperties = brandingConfigurationService.getBrandingConfigurationPropertiesForJspContext(Optional.empty());
        final String script = (String) brandingProperties.get(BrandingConfigurationService.BrandingConfigurationProperty.SCRIPT_FOR_CLIENT_CONFIGURATION_CONTEXT_TO_DOCUMENT_JSP_PROPERTY_NAME.getPropertyName());
        measurementCase.addMeasurement(new Measurement("Branding client script size in bytes", script.length()));
    }
}
