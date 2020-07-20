/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.CurrentAdapterSerializer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaServiceExtension;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextListener;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStoreContextImpl implements DataStoreContext {

    private static final Logger LOG = LoggerFactory.getLogger(DataStoreContextImpl.class);

    private final Map<LogicalDatastoreType, DOMStore> datastores;
    private final SchemaContextHolder mockedSchemaContext;
    private final DOMNotificationRouter domNotificationRouter;
    private final DOMDataBroker domDataBroker;
    private final DataBroker dataBroker;
    private final NotificationService notificationService;
    private final NotificationPublishService notificationPublishService;


    public DataStoreContextImpl() {
        this.mockedSchemaContext = new SchemaContextHolder();
        this.datastores = createDatastores();
        this.domNotificationRouter = DOMNotificationRouter.create(16);
        this.domDataBroker = createDOMDataBroker();
        this.dataBroker = createDataBroker();
        this.notificationService = createNotificationService();
        this.notificationPublishService = createNotificationPublishService();
        this.mockedSchemaContext.listeners.streamListeners()
            .forEach(l -> l.onModelContextUpdated(this.mockedSchemaContext.schemaContext));
    }

    @Override
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    @Override
    public DOMDataBroker getDOMDataBroker() {
        return this.domDataBroker;
    }

    @Override
    public NotificationService createNotificationService() {
        return new BindingDOMNotificationServiceAdapter(this.mockedSchemaContext.adapterContext,
                this.domNotificationRouter);
    }

    @Override
    public NotificationPublishService createNotificationPublishService() {
        return new BindingDOMNotificationPublishServiceAdapter(this.mockedSchemaContext.adapterContext,
                this.domNotificationRouter);
    }

    @Override
    public EffectiveModelContext getSchemaContext() {
        return this.mockedSchemaContext.schemaContext;
    }

    @Override
    public BindingNormalizedNodeSerializer getBindingToNormalizedNodeCodec() {
        return this.mockedSchemaContext.bindingStreamCodecs;
    }

    @Override
    public NotificationService getNotificationService() {
        return this.notificationService;
    }

    @Override
    public NotificationPublishService getNotificationPublishService() {
        return this.notificationPublishService;
    }

    private DOMDataBroker createDOMDataBroker() {
        return new SerializedDOMDataBroker(this.datastores,
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    }

    private ListeningExecutorService getDataTreeChangeListenerExecutor() {
        return MoreExecutors.newDirectExecutorService();
    }

    private DataBroker createDataBroker() {
        return new BindingDOMDataBrokerAdapter(this.mockedSchemaContext.adapterContext, getDOMDataBroker());
    }

    private Map<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION, createConfigurationDatastore()).build();
    }

    private DOMStore createConfigurationDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", getDataTreeChangeListenerExecutor());
        this.mockedSchemaContext.registerSchemaContextListener(store);
        return store;
    }

    private DOMStore createOperationalDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", getDataTreeChangeListenerExecutor());
        this.mockedSchemaContext.registerSchemaContextListener(store);
        return store;
    }

    private static final class SchemaContextHolder implements DOMSchemaService, EffectiveModelContextProvider {

        private final EffectiveModelContext schemaContext;
        private final ListenerRegistry<EffectiveModelContextListener> listeners;
        private final BindingNormalizedNodeSerializer bindingStreamCodecs;
        private final CurrentAdapterSerializer bindingToNormalized;
        private final AdapterContext adapterContext;

        private SchemaContextHolder() {
            List<YangModuleInfo> moduleInfos = loadModuleInfos();
            this.schemaContext = BindingRuntimeHelpers.createEffectiveModel(moduleInfos);
            this.listeners = ListenerRegistry.create();
            BindingRuntimeContext bindingContext =
                    BindingRuntimeHelpers.createRuntimeContext();
            this.bindingStreamCodecs = new BindingCodecContext(bindingContext);
            this.bindingToNormalized = new CurrentAdapterSerializer(new BindingCodecContext(bindingContext));
            adapterContext = new ConstantAdapterContext(this.bindingToNormalized);
        }

        @Override
        public EffectiveModelContext getGlobalContext() {
            return this.schemaContext;
        }


        /**
         * Loads all {@link YangModelBindingProvider} on the classpath.
         *
         * @return list of known {@link YangModuleInfo}
         */
        private List<YangModuleInfo> loadModuleInfos() {
            List<YangModuleInfo> moduleInfos = new LinkedList<>();
            ServiceLoader<YangModelBindingProvider> yangProviderLoader =
                    ServiceLoader.load(YangModelBindingProvider.class);
            for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
                moduleInfos.add(yangModelBindingProvider.getModuleInfo());
                LOG.debug("Adding [{}] module into known modules", yangModelBindingProvider.getModuleInfo());
            }
            return moduleInfos;
        }

        @Override
        public ListenerRegistration<EffectiveModelContextListener> registerSchemaContextListener(
                EffectiveModelContextListener listener) {
            return this.listeners.register(listener);
        }

        @Override
        public @NonNull EffectiveModelContext getEffectiveModelContext() {
            return this.schemaContext;
        }

        @Override
        public @NonNull ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
            return ImmutableClassToInstanceMap.of();
        }


    }
}
