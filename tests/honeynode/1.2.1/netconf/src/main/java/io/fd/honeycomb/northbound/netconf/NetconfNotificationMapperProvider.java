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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener;
import org.opendaylight.netconf.mdsal.notification.impl.CapabilityChangeNotificationProducer;
import org.opendaylight.netconf.mdsal.notification.impl.NetconfNotificationOperationServiceFactory;
import org.opendaylight.netconf.mdsal.notification.impl.NotificationToMdsalWriter;
import org.opendaylight.netconf.notifications.NetconfNotificationCollector;
import org.opendaylight.netconf.notifications.NetconfNotificationRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetconfNotificationMapperProvider extends ProviderTrait<NetconfOperationServiceFactory> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfNotificationMapperProvider.class);

    public static final InstanceIdentifier<Capabilities> capabilitiesIdentifier =
            InstanceIdentifier.create(NetconfState.class).child(Capabilities.class).builder().build();
    @Inject
    private NetconfNotificationCollector notificationCollector;
    @Inject
    private NetconfNotificationRegistry notificationRegistry;
    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF)
    private BindingAwareBroker bindingAwareBroker;
    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF)
    private DataBroker dataBroker;
    @Inject
    private NetconfOperationServiceFactoryListener aggregator;
    @Inject
    private ShutdownHandler shutdownHandler;

    @Override
    protected NetconfNotificationOperationServiceFactory create() {
        LOG.trace("Initializing NotificationToMdsalWriter");
        final NotificationToMdsalWriter writer = new NotificationToMdsalWriter(notificationCollector, dataBroker);
        writer.start();

        LOG.trace("Initializing CapabilityChangeNotificationProducer");
        final CapabilityChangeNotificationProducer capabilityChangeNotificationProducer =
            new CapabilityChangeNotificationProducer(notificationCollector, dataBroker);

        LOG.trace("Providing NetconfNotificationOperationServiceFactory");
        final NetconfNotificationOperationServiceFactory netconfNotificationOperationServiceFactory =
            new NetconfNotificationOperationServiceFactory(notificationRegistry, aggregator);

        shutdownHandler.register("netconf-notification-service-factory", netconfNotificationOperationServiceFactory);
        shutdownHandler.register("capability-change-notification-producer",
            capabilityChangeNotificationProducer::close);
        shutdownHandler.register("notification-to-mdsal-writer", writer);
        return netconfNotificationOperationServiceFactory;
    }
}
