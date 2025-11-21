package com.sap.sse.security.datamining;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;

import com.sap.sse.datamining.DataSourceProvider;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.impl.AbstractDataMiningActivator;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.security.datamining.data.HasPermissionOfUserContext;
import com.sap.sse.security.datamining.data.HasPermissionOfUserInUserGroupContext;
import com.sap.sse.security.datamining.data.HasPreferenceOfUserContext;
import com.sap.sse.security.datamining.data.HasPreferenceOfUserInUserGroupContext;
import com.sap.sse.security.datamining.data.HasRoleOfUserContext;
import com.sap.sse.security.datamining.data.HasRoleOfUserGroupContext;
import com.sap.sse.security.datamining.data.HasRoleOfUserInUserGroupContext;
import com.sap.sse.security.datamining.data.HasSessionContext;
import com.sap.sse.security.datamining.data.HasSubscriptionContext;
import com.sap.sse.security.datamining.data.HasUserContext;
import com.sap.sse.security.datamining.data.HasUserGroupContext;
import com.sap.sse.security.datamining.data.HasUserInUserGroupContext;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.subscription.Subscription;

/**
 * Handles all necessary registration for a datamining bundle that allows querying constructs from the "Security"
 * domain, such as {@link User}, {@link UserGroup} and {@link Subscription}. See also
 * https://wiki.sapsailing.com/wiki/info/landscape/typical-data-mining-scenarios for more information.
 * 
 * @author D043530 (Axel Uhl)
 *
 */
public class Activator extends AbstractDataMiningActivator {
    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/SecurityDataMining_StringMessages";
    
    private static Activator INSTANCE;
    
    private BundleContext context = null;

    private final ResourceBundleStringMessages sailingServerStringMessages;
    private final SecurityDataRetrievalChainDefinitions dataRetrieverChainDefinitions;
    private Collection<DataSourceProvider<?>> dataSourceProviders;
    private boolean dataSourceProvidersHaveBeenInitialized;
    
    public Activator() {
        dataRetrieverChainDefinitions = new SecurityDataRetrievalChainDefinitions();
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
        final Set<Class<?>> internalClasses = new HashSet<>();
        internalClasses.add(HasUserGroupContext.class);
        internalClasses.add(HasUserInUserGroupContext.class);
        internalClasses.add(HasRoleOfUserGroupContext.class);
        internalClasses.add(HasPermissionOfUserInUserGroupContext.class);
        internalClasses.add(HasPermissionOfUserContext.class);
        internalClasses.add(HasUserContext.class);
        internalClasses.add(HasRoleOfUserContext.class);
        internalClasses.add(HasRoleOfUserInUserGroupContext.class);
        internalClasses.add(HasPreferenceOfUserContext.class);
        internalClasses.add(HasPreferenceOfUserInUserGroupContext.class);
        internalClasses.add(HasSessionContext.class);
        internalClasses.add(HasSubscriptionContext.class);
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
        final Set<AggregationProcessorDefinition<?, ?>> aggregators = new HashSet<>();
        return aggregators;
    }
    
    private void initializeDataSourceProviders() {
        dataSourceProviders = new HashSet<>();
        dataSourceProviders.add(new SecurityServiceProvider(context));
    }
    
    public static Activator getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new Activator(); // probably non-OSGi case, as in test execution
        }
        return INSTANCE;
    }
}
