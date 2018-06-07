/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.northbound.netconf;

import com.google.inject.Singleton;
import com.google.inject.binder.AnnotatedElementBuilder;
import com.google.inject.name.Names;

import io.fd.honeycomb.infra.distro.data.BindingDataBrokerProvider;
import io.fd.honeycomb.infra.distro.data.DataStoreProvider;
import io.fd.honeycomb.infra.distro.data.HoneycombNotificationManagerProvider;
import io.fd.honeycomb.infra.distro.data.InmemoryDOMDataBrokerProvider;
import io.fd.honeycomb.northbound.NetconfConfiguration;
import io.fd.honeycomb.northbound.NorthboundPrivateModule;
import io.fd.honeycomb.notification.NotificationCollector;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.netconf.api.NetconfServerDispatcher;
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.netconf.impl.osgi.AggregatedNetconfOperationServiceFactory;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener;
import org.opendaylight.netconf.notifications.NetconfNotificationCollector;
import org.opendaylight.netconf.notifications.NetconfNotificationListener;
import org.opendaylight.netconf.notifications.NetconfNotificationRegistry;
import org.opendaylight.netconf.notifications.impl.NetconfNotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfModule extends NorthboundPrivateModule<NetconfConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfModule.class);

    public static final String HONEYCOMB_NETCONF = "honeycomb-netconf";
    public static final String HONEYCOMB_NETCONF_MAPPER_AGGR = "netconf-mapper-aggregator";
    public static final String HONEYCOMB_NETCONF_MAPPER_NOTIF = "netconf-mapper-notification";
    public static final String HONEYCOMB_NETCONF_MAPPER_CORE = "netconf-mapper-honeycomb";
    public static final String HONEYCOMB_NETCONF_MAPPER_OPER = "netconf-mapper-monitoring";

    public NetconfModule() {
        super(new NetconfConfigurationModule(), NetconfConfiguration.class);
    }

    @Override
    protected void configure() {
        if (!getConfiguration().isNetconfEnabled()) {
            LOG.debug("Netconf disabled, skipping initialization");
            return;
        }
        install(getConfigurationModule());
        LOG.info("Starting NETCONF Northbound");
        // Create inmemory data store for HONEYCOMB_NETCONF config metadata
        bind(InMemoryDOMDataStore.class).annotatedWith(Names.named(InmemoryDOMDataBrokerProvider.CONFIG))
                .toProvider(
                        new DataStoreProvider(InmemoryDOMDataBrokerProvider.CONFIG, LogicalDatastoreType.CONFIGURATION))
                .in(Singleton.class);

        // Create inmemory data store for HONEYCOMB_NETCONF operational metadata
        bind(InMemoryDOMDataStore.class).annotatedWith(Names.named(InmemoryDOMDataBrokerProvider.OPERATIONAL))
                .toProvider(new DataStoreProvider(InmemoryDOMDataBrokerProvider.OPERATIONAL,
                        LogicalDatastoreType.OPERATIONAL))
                .in(Singleton.class);
        // Wrap datastores as DOMDataBroker
        bind(DOMDataBroker.class).toProvider(InmemoryDOMDataBrokerProvider.class).in(Singleton.class);

        // Wrap DOMDataBroker as BA data broker
        bind(DataBroker.class).annotatedWith(Names.named(HONEYCOMB_NETCONF)).toProvider(BindingDataBrokerProvider.class)
                .in(Singleton.class);
        expose(DataBroker.class).annotatedWith(Names.named(HONEYCOMB_NETCONF));

        // Wrap BA data broker as BindingAwareBroker (requied by HONEYCOMB_NETCONF)
        bind(BindingAwareBroker.class).annotatedWith(Names.named(HONEYCOMB_NETCONF))
                .toProvider(NetconfBindingBrokerProvider.class).in(Singleton.class);

        // Create netconf operation service factory aggregator to aggregate different services
        AggregatedNetconfOperationServiceFactory factory = new AggregatedNetconfOperationServiceFactory();
        bind(NetconfOperationServiceFactory.class).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_AGGR))
                .toInstance(factory);
        bind(NetconfOperationServiceFactoryListener.class).toInstance(factory);

        // Create netconf notification manager
        NetconfNotificationManager manager = new NetconfNotificationManager();
        bind(NetconfNotificationCollector.class).toInstance(manager);
        bind(NetconfNotificationRegistry.class).toInstance(manager);
        bind(NetconfNotificationListener.class).toInstance(manager);

        // Netconf notification service factory
        bind(NetconfOperationServiceFactory.class).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_NOTIF))
                .toProvider(NetconfNotificationMapperProvider.class).asEagerSingleton();
        expose(NetconfOperationServiceFactory.class).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_NOTIF));

        // Netconf core part - mapping between Honeycomb and Netconf
        bind(NetconfOperationServiceFactory.class).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_CORE))
                .toProvider(NetconfMdsalMapperProvider.class).asEagerSingleton();
        expose(NetconfOperationServiceFactory.class).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_CORE));

        // Netconf monitoring service factory
        bind(NetconfMonitoringService.class).toProvider(NetconfMonitoringServiceProvider.class).in(Singleton.class);
        bind(NetconfOperationServiceFactory.class).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_OPER))
                .toProvider(NetconfMonitoringMapperProvider.class).asEagerSingleton();
        expose(NetconfOperationServiceFactory.class).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_OPER));

        // Create HC notification manager + HC2Netconf translator
        bind(NotificationCollector.class).toProvider(HoneycombNotificationManagerProvider.class).in(Singleton.class);
        bind(HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf.class)
                .toProvider(HoneycombNotification2NetconfProvider.class).asEagerSingleton();
        expose(HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf.class);

        configureServer();
    }

    /**
     * Provide HONEYCOMB_NETCONF TCP and SSH servers.
     */
    private AnnotatedElementBuilder configureServer() {
        bind(NioEventLoopGroup.class).toProvider(NettyThreadGroupProvider.class).in(Singleton.class);
        bind(Timer.class).toInstance(new HashedWheelTimer());
        bind(NetconfServerDispatcher.class).toProvider(NetconfServerDispatcherProvider.class).in(Singleton.class);
        bind(NetconfTcpServerProvider.NetconfTcpServer.class).toProvider(NetconfTcpServerProvider.class)
                .asEagerSingleton();
        expose(NetconfTcpServerProvider.NetconfTcpServer.class);
        bind(NetconfSshServerProvider.NetconfSshServer.class).toProvider(NetconfSshServerProvider.class)
                .asEagerSingleton();
        return expose(NetconfSshServerProvider.NetconfSshServer.class);
    }
}
