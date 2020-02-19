/*
 * Copyright (c) 2016, 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.distro.data;

import com.google.inject.PrivateModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.fd.honeycomb.data.ModifiableDataManager;
import io.fd.honeycomb.data.ReadableDataManager;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.data.init.ShutdownHandler;
import io.fd.honeycomb.impl.EmptyDomMountService;
import io.fd.honeycomb.infra.distro.data.config.WriterRegistryProvider;
import io.fd.honeycomb.infra.distro.data.oper.ReadableDTDelegProvider;
import io.fd.honeycomb.infra.distro.data.oper.ReaderRegistryProvider;
import io.fd.honeycomb.rpc.RpcRegistry;
import io.fd.honeycomb.rpc.RpcRegistryBuilder;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

public class ConfigAndOperationalPipelineModule extends PrivateModule {

    public static final String HONEYCOMB_CONFIG_NONPERSIST = "honeycomb-config-nopersist";
    public static final String HONEYCOMB_CONFIG = "honeycomb-config";

    @Override
    protected void configure() {
        bind(ShutdownHandler.class).toProvider(ShutdownHandlerProvider.class).in(Singleton.class);
        expose(ShutdownHandler.class);

        // Mount point service is required by notification service and restconf
        bind(DOMMountPointService.class).to(EmptyDomMountService.class).in(Singleton.class);
        expose(DOMMountPointService.class);

        // Expose registries for plugin reader/writer factories
        bind(WriterRegistry.class).toProvider(WriterRegistryProvider.class).in(Singleton.class);
        expose(WriterRegistry.class);
        bind(ReaderRegistry.class).toProvider(ReaderRegistryProvider.class).in(Singleton.class);
        expose(ReaderRegistry.class);

        // Non persisting data tree for config
        bind(DataTree.class).annotatedWith(Names.named(HONEYCOMB_CONFIG_NONPERSIST))
                .toProvider(DataTreeProvider.ConfigDataTreeProvider.class).in(Singleton.class);
        expose(DataTree.class).annotatedWith(Names.named(HONEYCOMB_CONFIG_NONPERSIST));
        // Persisting data tree wrapper for config
        bind(DataTree.class).annotatedWith(Names.named(HONEYCOMB_CONFIG))
                .toProvider(PersistingDataTreeProvider.ConfigPersistingDataTreeProvider.class).in(Singleton.class);
        expose(DataTree.class).annotatedWith(Names.named(HONEYCOMB_CONFIG));

        // Config Data Tree manager working on top of config data tree + writer registry
        bind(ModifiableDataManager.class).toProvider(ModifiableDTDelegProvider.class).in(Singleton.class);
        // Operational Data Tree manager working on top of reader registry
        bind(ReadableDataManager.class).toProvider(ReadableDTDelegProvider.class).in(Singleton.class);
        expose(ReadableDataManager.class);

        // DOMDataBroker wrapper on top of data tree managers
        HoneycombDOMDataBrokerProvider domBrokerProvider = new HoneycombDOMDataBrokerProvider();
        bind(DOMDataBroker.class).annotatedWith(Names.named(HONEYCOMB_CONFIG)).toProvider(domBrokerProvider).in(Singleton.class);
        expose(DOMDataBroker.class).annotatedWith(Names.named(HONEYCOMB_CONFIG));

        // BA version of data broker
        bind(DataBroker.class).annotatedWith(Names.named(HONEYCOMB_CONFIG)).toProvider(HoneycombBindingDataBrokerProvider.class)
                .in(Singleton.class);
        expose(DataBroker.class).annotatedWith(Names.named(HONEYCOMB_CONFIG));

        // Create initializer to init persisted config data
        bind(DataTreeInitializer.class).annotatedWith(Names.named(HONEYCOMB_CONFIG))
                .toProvider(PersistedConfigInitializerProvider.class)
                .in(Singleton.class);
        expose(DataTreeInitializer.class).annotatedWith(Names.named(HONEYCOMB_CONFIG));

        configureNotifications();
        configureRpcs();
    }

    private void configureNotifications() {
        // Create notification service
        bind(DOMNotificationRouter.class).toProvider(DOMNotificationServiceProvider.class).in(Singleton.class);
        expose(DOMNotificationRouter.class);
    }

    private void configureRpcs() {
        // Create rpc service
        bind(DOMRpcService.class).toProvider(HoneycombDOMRpcServiceProvider.class).in(Singleton.class);
        expose(DOMRpcService.class);

        bind(RpcRegistryBuilder.class).toProvider(RpcRegistryBuilderProvider.class).in(Singleton.class);
        expose(RpcRegistryBuilder.class);

        bind(RpcRegistry.class).toProvider(RpcRegistryProvider.class).in(Singleton.class);
        expose(RpcRegistry.class);
    }
}
