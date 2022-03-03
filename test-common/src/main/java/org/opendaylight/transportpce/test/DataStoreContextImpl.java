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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.dom.adapter.BindingAdapterFactory;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.mdsal.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.FixedDOMSchemaService;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreFactory;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
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
    private BindingAdapterFactory adapterFactory ;


    public DataStoreContextImpl() {
        List<YangModuleInfo> moduleInfos = new LinkedList<>();
        ServiceLoader<YangModelBindingProvider> yangProviderLoader = ServiceLoader.load(YangModelBindingProvider.class);
        for (YangModelBindingProvider yangModelBindingProvider : yangProviderLoader) {
            moduleInfos.add(yangModelBindingProvider.getModuleInfo());
        }
        schemaCtx = BindingRuntimeHelpers.createEffectiveModel(moduleInfos);
        bindingDOMCodecServices = new BindingCodecContext(BindingRuntimeHelpers.createRuntimeContext());
        adapterFactory = new BindingAdapterFactory(new ConstantAdapterContext(bindingDOMCodecServices));
        domNotificationRouter = new DOMNotificationRouter(16);
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
    public NotificationService createNotificationService() {
        return adapterFactory.createNotificationService(domNotificationRouter);
    }

    @Override
    public NotificationPublishService createNotificationPublishService() {
        return adapterFactory.createNotificationPublishService(domNotificationRouter);
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
        return InMemoryDOMDataStoreFactory.create("CFG",
                FixedDOMSchemaService.of(bindingDOMCodecServices.getRuntimeContext()));
    }

    private DOMStore createOperationalDatastore() {
        return InMemoryDOMDataStoreFactory.create("OPER",
                FixedDOMSchemaService.of(bindingDOMCodecServices.getRuntimeContext()));
    }

    @Override
    public BindingDOMCodecServices getBindingDOMCodecServices() {
        return bindingDOMCodecServices;
    }


}
