/*
 * Copyright (c) 2018 AT&T and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.test.common;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.ClassPool;

public class DataStoreContextImpl implements DataStoreContext {

    private static final Logger LOG = LoggerFactory.getLogger(DataStoreContextImpl.class);

//    private final Map<LogicalDatastoreType, DOMStore> datastores;
    private final SchemaContextHolder mockedSchemaContext;
//    private final DOMNotificationRouter domNotificationRouter;
//    private final DOMDataBroker domDataBroker;
//    private final DataBroker dataBroker;
//    private final NotificationService notificationService;
//    private final NotificationPublishService notificationPublishService;

    public DataStoreContextImpl() {
        this(false);
    }

    public DataStoreContextImpl(boolean fromClasspath) {
        this.mockedSchemaContext = new SchemaContextHolder(fromClasspath);
//        this.datastores = createDatastores();
//        this.domNotificationRouter = DOMNotificationRouter.create(16);
//        this.domDataBroker = createDOMDataBroker();
//        this.dataBroker = createDataBroker();
//        this.notificationService = createNotificationService();
//        this.notificationPublishService = createNotificationPublishService();
//        for (ListenerRegistration<SchemaContextListener> listener : this.mockedSchemaContext.listeners) {
//            listener.getInstance().onGlobalContextUpdated(this.mockedSchemaContext.schemaContext);
//        }
    }

//    @Override
//    public DataBroker getDataBroker() {
//        return this.dataBroker;
//    }
//
//    @Override
//    public DOMDataBroker getDOMDataBroker() {
//        return this.domDataBroker;
//    }
//
//    @Override
//    public NotificationService createNotificationService() {
//        return new BindingDOMNotificationServiceAdapter(this.mockedSchemaContext.bindingStreamCodecs,
//                this.domNotificationRouter);
//    }
//
//    @Override
//    public NotificationPublishService createNotificationPublishService() {
//        return new BindingDOMNotificationPublishServiceAdapter(this.mockedSchemaContext.bindingToNormalized,
//                this.domNotificationRouter);
//    }

    @Override
    public SchemaContext getSchemaContext() {
        return this.mockedSchemaContext.schemaContext;
    }

    @Override
    public BindingNormalizedNodeCodecRegistry getBindingToNormalizedNodeCodec() {
        return this.mockedSchemaContext.bindingStreamCodecs;
    }

//    @Override
//    public NotificationService getNotificationService() {
//        return this.notificationService;
//    }
//
//    @Override
//    public NotificationPublishService getNotificationPublishService() {
//        return this.notificationPublishService;
//    }
//
//    private DOMDataBroker createDOMDataBroker() {
//        return new SerializedDOMDataBroker(this.datastores,
//                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
//    }
//
//    private ListeningExecutorService getDataTreeChangeListenerExecutor() {
//        return MoreExecutors.newDirectExecutorService();
//    }
//
//    private DataBroker createDataBroker() {
//        return new BindingDOMDataBrokerAdapter(getDOMDataBroker(), this.mockedSchemaContext.bindingToNormalized);
//    }
//
//    private Map<LogicalDatastoreType, DOMStore> createDatastores() {
//        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
//                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
//                .put(LogicalDatastoreType.CONFIGURATION, createConfigurationDatastore()).build();
//    }
//
//    private DOMStore createConfigurationDatastore() {
//        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", getDataTreeChangeListenerExecutor());
//        this.mockedSchemaContext.registerSchemaContextListener(store);
//        return store;
//    }
//
//    private DOMStore createOperationalDatastore() {
//        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", getDataTreeChangeListenerExecutor());
//        this.mockedSchemaContext.registerSchemaContextListener(store);
//        return store;
//    }

    private class SchemaContextHolder implements DOMSchemaService, SchemaContextProvider {

        private final SchemaContext schemaContext;
        private final ListenerRegistry<SchemaContextListener> listeners;
        private final BindingNormalizedNodeCodecRegistry bindingStreamCodecs;
        private final BindingToNormalizedNodeCodec bindingToNormalized;
        private final ModuleInfoBackedContext moduleInfoBackedCntxt;

        private SchemaContextHolder(boolean fromClasspath) {
            List<YangModuleInfo> moduleInfos = loadModuleInfos();
            this.moduleInfoBackedCntxt = ModuleInfoBackedContext.create();
            this.schemaContext = getSchemaContext(moduleInfos);
            this.listeners = ListenerRegistry.create();
            this.bindingStreamCodecs = createBindingRegistry();
            GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
            this.bindingToNormalized = new BindingToNormalizedNodeCodec(loading, this.bindingStreamCodecs);
            registerSchemaContextListener(this.bindingToNormalized);
        }

        @Override
        public SchemaContext getSchemaContext() {
            return this.schemaContext;
        }

        /**
         * Get the schemacontext from loaded modules on classpath.
         *
         * @param moduleInfos a list of Yang module Infos
         * @return SchemaContext a schema context
         */
        private SchemaContext getSchemaContext(List<YangModuleInfo> moduleInfos) {
            this.moduleInfoBackedCntxt.addModuleInfos(moduleInfos);
            Optional<SchemaContext> tryToCreateSchemaContext =
                    this.moduleInfoBackedCntxt.tryToCreateSchemaContext().toJavaUtil();
            if (!tryToCreateSchemaContext.isPresent()) {
                LOG.error("Could not create the initial schema context. Schema context is empty");
                throw new IllegalStateException();
            }
            return tryToCreateSchemaContext.get();
        }

        @Override
        public SchemaContext getGlobalContext() {
            return this.schemaContext;
        }

        @Override
        public SchemaContext getSessionContext() {
            return this.schemaContext;
        }

        @Override
        public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
                SchemaContextListener listener) {
            return this.listeners.register(listener);
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

        /**
         * Creates binding registry.
         *
         * @return BindingNormalizedNodeCodecRegistry the resulting binding registry
         */
        private BindingNormalizedNodeCodecRegistry createBindingRegistry() {
            BindingRuntimeContext bindingContext = BindingRuntimeContext.create(this.moduleInfoBackedCntxt, this.schemaContext);
            BindingNormalizedNodeCodecRegistry bindingNormalizedNodeCodecRegistry =
                    new BindingNormalizedNodeCodecRegistry(
                            StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault())));
            bindingNormalizedNodeCodecRegistry.onBindingRuntimeContextUpdated(bindingContext);
            return bindingNormalizedNodeCodecRegistry;
        }
    }
}
