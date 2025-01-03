/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.dom.adapter.BindingAdapterFactory;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.spi.AdapterFactory;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.RouterDOMNotificationService;
import org.opendaylight.mdsal.dom.broker.RouterDOMPublishNotificationService;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.FixedDOMSchemaService;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreFactory;
import org.opendaylight.yangtools.binding.data.codec.impl.di.DefaultBindingDOMCodecFactory;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.binding.meta.YangModelBindingProvider;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class DataStoreContextImpl implements DataStoreContext {

    private final Map<LogicalDatastoreType, DOMStore> datastores;
    private final DOMNotificationRouter domNotificationRouter;
    private final DOMDataBroker domDataBroker;
    private final DataBroker dataBroker;
    private final NotificationService notificationService;
    private final NotificationPublishService notificationPublishService;
    private EffectiveModelContext schemaCtx;
    private BindingDOMCodecServices bindingDOMCodecServices;
    private AdapterFactory adapterFactory;
    private DOMNotificationService domNotificationService;
    private DOMNotificationPublishService domNotificationPublishService;

    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR")
    public DataStoreContextImpl() {
        Set<YangModuleInfo> moduleInfos = new HashSet<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            moduleInfos.add(yangModelBindingProvider.getModuleInfo());
        }
        schemaCtx = BindingRuntimeHelpers.createEffectiveModel(moduleInfos);
        bindingDOMCodecServices = new DefaultBindingDOMCodecFactory()
                .createBindingDOMCodec(BindingRuntimeHelpers.createRuntimeContext());
        adapterFactory = new BindingAdapterFactory(new ConstantAdapterContext(bindingDOMCodecServices));
        domNotificationRouter = new DOMNotificationRouter(16);
        domNotificationService = new RouterDOMNotificationService(domNotificationRouter);
        domNotificationPublishService = new RouterDOMPublishNotificationService(domNotificationRouter);
        datastores = createDatastores();
        domDataBroker = createDOMDataBroker();
        dataBroker = createDataBroker();
        notificationService = createNotificationService();
        notificationPublishService = createNotificationPublishService();
    }

    @Override
    public DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    public DOMDataBroker getDOMDataBroker() {
        return domDataBroker;
    }

    @Override
    public final NotificationService createNotificationService() {
        return adapterFactory.createNotificationService(domNotificationService);
    }

    @Override
    public final NotificationPublishService createNotificationPublishService() {
        return adapterFactory.createNotificationPublishService(domNotificationPublishService);
    }

    @Override
    public EffectiveModelContext getSchemaContext() {
        return schemaCtx;
    }

    @Override
    public NotificationService getNotificationService() {
        return notificationService;
    }

    @Override
    public NotificationPublishService getNotificationPublishService() {
        return notificationPublishService;
    }

    private DOMDataBroker createDOMDataBroker() {
        return new SerializedDOMDataBroker(datastores,
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    }

    private DataBroker createDataBroker() {
        return adapterFactory.createDataBroker(getDOMDataBroker());
    }

    private Map<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION, createConfigurationDatastore()).build();
    }

    private DOMStore createConfigurationDatastore() {
        return InMemoryDOMDataStoreFactory.create("DOM-CFG", new FixedDOMSchemaService(schemaCtx));
    }

    private DOMStore createOperationalDatastore() {
        return InMemoryDOMDataStoreFactory.create("DOM-OPER", new FixedDOMSchemaService(schemaCtx));
    }

    @Override
    public BindingDOMCodecServices getBindingDOMCodecServices() {
        return bindingDOMCodecServices;
    }


}
