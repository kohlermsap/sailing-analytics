package com.sap.sailing.polars.datamining;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;

import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.provider.RacingEventServiceProvider;
import com.sap.sailing.polars.datamining.components.aggregators.PolarBackendDataAggregationProcessor;
import com.sap.sailing.polars.datamining.components.aggregators.PolarDataAggregationProcessor;
import com.sap.sailing.polars.datamining.data.HasBackendPolarBoatClassContext;
import com.sap.sailing.polars.datamining.data.HasCompetitorPolarContext;
import com.sap.sailing.polars.datamining.data.HasFleetPolarContext;
import com.sap.sailing.polars.datamining.data.HasGPSFixPolarContext;
import com.sap.sailing.polars.datamining.data.HasLeaderboardPolarContext;
import com.sap.sailing.polars.datamining.data.HasLegPolarContext;
import com.sap.sailing.polars.datamining.data.HasRaceColumnPolarContext;
import com.sap.sse.datamining.DataSourceProvider;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.impl.AbstractDataMiningActivator;
import com.sap.sse.i18n.ResourceBundleStringMessages;

/**
 * Handles all necessary registration for a datamining bundle. See
 * https://wiki.sapsailing.com/wiki/info/landscape/typical-data-mining-scenarios for more information.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class Activator extends AbstractDataMiningActivator {

    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/Polars_StringMessages";
    
    private static Activator INSTANCE;
    
    private BundleContext context = null;

    private final ResourceBundleStringMessages sailingServerStringMessages;
    private final PolarsDataRetrievalChainDefinitions dataRetrieverChainDefinitions;
    private Collection<DataSourceProvider<?>> dataSourceProviders;
    private boolean dataSourceProvidersHaveBeenInitialized;
    
    public Activator() {
        dataRetrieverChainDefinitions = new PolarsDataRetrievalChainDefinitions();
        sailingServerStringMessages = ResourceBundleStringMessages.create(STRING_MESSAGES_BASE_NAME, getClassLoader(),
                StandardCharsets.UTF_8.name());
    }

    @Override
    public void start(BundleContext context) throws Exception {
        INSTANCE = this;
        this.context = context;
        dataSourceProvidersHaveBeenInitialized = false;
        super.start(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        this.context = null;
        INSTANCE = null;
        super.stop(context);
    }
    
    @Override
    public ResourceBundleStringMessages getStringMessages() {
        return sailingServerStringMessages;
    }

    @Override
    public Iterable<Class<?>> getClassesWithMarkedMethods() {
        Set<Class<?>> internalClasses = new HashSet<>();
        internalClasses.add(HasLeaderboardGroupContext.class);
        internalClasses.add(HasLeaderboardPolarContext.class);
        internalClasses.add(HasRaceColumnPolarContext.class);
        internalClasses.add(HasFleetPolarContext.class);
        internalClasses.add(HasCompetitorPolarContext.class);
        internalClasses.add(HasLegPolarContext.class);
        internalClasses.add(HasGPSFixPolarContext.class);
        internalClasses.add(HasBackendPolarBoatClassContext.class);
        return internalClasses;
    }

    @Override
    public Iterable<DataRetrieverChainDefinition<?, ?>> getDataRetrieverChainDefinitions() {
        return dataRetrieverChainDefinitions.getDataRetrieverChainDefinitions();
    }
    
    @Override
    public Iterable<DataSourceProvider<?>> getDataSourceProviders() {
        if (!dataSourceProvidersHaveBeenInitialized) {
            initializeDataSourceProviders();
            dataSourceProvidersHaveBeenInitialized = true;
        }
        return dataSourceProviders;
    }
    
    @Override
    public Iterable<AggregationProcessorDefinition<?, ?>> getAggregationProcessorDefinitions() {
        HashSet<AggregationProcessorDefinition<?, ?>> aggregators = new HashSet<>();
        aggregators.add(PolarDataAggregationProcessor.getDefinition());
        aggregators.add(PolarBackendDataAggregationProcessor.getDefinition());
        return aggregators;
    }
    
    private void initializeDataSourceProviders() {
        dataSourceProviders = new HashSet<>();
        dataSourceProviders.add(new RacingEventServiceProvider(context));
    }
    
    public static Activator getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new Activator(); // probably non-OSGi case, as in test execution
        }
        
        return INSTANCE;
    }


}
