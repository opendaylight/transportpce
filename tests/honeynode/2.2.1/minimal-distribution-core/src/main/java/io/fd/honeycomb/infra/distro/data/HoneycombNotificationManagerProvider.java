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

package io.fd.honeycomb.infra.distro.data;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.honeycomb.notification.impl.HoneycombNotificationCollector;
import io.fd.honeycomb.notification.impl.NotificationProducerRegistry;
import io.fd.honeycomb.notification.impl.NotificationProducerTracker;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;

public final class HoneycombNotificationManagerProvider extends ProviderTrait<NotificationCollector> {

    @Inject
    private DOMNotificationRouter notificationRouter;
    @Inject(optional = true)
    private Set<ManagedNotificationProducer> notificationProducers = new HashSet<>();
    @Inject
    private BindingToNormalizedNodeCodec codec;

    @Override
    protected HoneycombNotificationCollector create() {
        // Create the registry to keep track of what'OPERATIONAL registered
        NotificationProducerRegistry notificationProducerRegistry =
                new NotificationProducerRegistry(Lists.newArrayList(notificationProducers));

        // Create BA version of notification service (implementation is free from ODL)
        BindingDOMNotificationPublishServiceAdapter bindingDOMNotificationPublishServiceAdapter =
                new BindingDOMNotificationPublishServiceAdapter(codec, notificationRouter);

        // Create Collector on top of BA notification service and registry
        HoneycombNotificationCollector honeycombNotificationCollector =
                new HoneycombNotificationCollector(bindingDOMNotificationPublishServiceAdapter,
                        notificationProducerRegistry);

        // Create tracker, responsible for starting and stopping registered notification producers whenever necessary
        NotificationProducerTracker notificationProducerTracker =
                new NotificationProducerTracker(notificationProducerRegistry, honeycombNotificationCollector,
                        notificationRouter);

        // DOMNotificationService is already provided by DOMBroker injected into RESTCONF, however RESTCONF
        // only supports data-change notification, nothing else. So currently (Beryllium-SR2) honeycomb notifications
        // won't be available over RESTCONF.

        return honeycombNotificationCollector;
    }
}
