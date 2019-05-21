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
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.northbound.NetconfConfiguration;
import io.fd.honeycomb.notification.NotificationCollector;
import io.fd.honeycomb.notification.impl.NotificationProducerRegistry;
import io.fd.honeycomb.notification.impl.TranslationUtil;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.netconf.notifications.NetconfNotificationCollector;
import org.opendaylight.netconf.notifications.NotificationPublisherRegistration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HoneycombNotification2NetconfProvider
        extends ProviderTrait<HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf> {

    private static final Logger LOG = LoggerFactory.getLogger(HoneycombNotification2NetconfProvider.class);

    @Inject
    private DOMNotificationRouter notificationRouter;
    @Inject
    private SchemaService schemaService;
    @Inject
    private NetconfConfiguration cfgAttributes;
    @Inject
    private NotificationCollector hcNotificationCollector;
    @Inject
    private NetconfNotificationCollector netconfNotificationCollector;

    @Override
    protected HoneycombNotification2Netconf create() {
        final StreamNameType streamType = new StreamNameType(cfgAttributes.netconfNotificationStreamName.get());

        // Register as HONEYCOMB_NETCONF notification publisher under configured name
        final NotificationPublisherRegistration netconfNotifReg = netconfNotificationCollector
                .registerNotificationPublisher(new StreamBuilder().setName(streamType).setReplaySupport(false)
                        .setDescription(cfgAttributes.netconfNotificationStreamName.get()).build());

        // Notification Translator, get notification from HC producers and put into HONEYCOMB_NETCONF notification collector
        final DOMNotificationListener domNotificationListener =
                new TranslatingNotificationListener(netconfNotifReg, streamType, schemaService);

        // NotificationManager is used to provide list of available notifications (which are all of the notifications registered)
        // TODO HONEYCOMB-165 make available notifications configurable here so that any number of notification streams for netconf
        // can be configured on top of a single notification manager
        LOG.debug("Current notifications to be exposed over HONEYCOMB_NETCONF: {}",
                hcNotificationCollector.getNotificationTypes());
        List<SchemaPath> currentNotificationSchemaPaths = hcNotificationCollector.getNotificationTypes().stream()
                .map(notifType -> SchemaPath.create(true, NotificationProducerRegistry.getQName(notifType)))
                .collect(Collectors.toList());

        // Register as listener to HC'OPERATIONAL DOM notification service
        // TODO HONEYCOMB-166 This should only be triggered when HONEYCOMB_NETCONF notifications are activated
        // Because this way we actually start all notification producers
        // final Collection<QName> notificationQNames =
        ListenerRegistration<DOMNotificationListener> domNotifListenerReg = notificationRouter
                .registerNotificationListener(domNotificationListener, currentNotificationSchemaPaths);

        LOG.info("Exposing HONEYCOMB_NETCONF notification stream: {}", streamType.getValue());
        return new HoneycombNotification2Netconf(domNotifListenerReg, netconfNotifReg);
    }

    public static final class HoneycombNotification2Netconf {
        private final ListenerRegistration<DOMNotificationListener> domNotifListenerReg;
        private final NotificationPublisherRegistration netconfNotifReg;

        public HoneycombNotification2Netconf(final ListenerRegistration<DOMNotificationListener> domNotifListenerReg,
                                             final NotificationPublisherRegistration netconfNotifReg) {
            this.domNotifListenerReg = domNotifListenerReg;
            this.netconfNotifReg = netconfNotifReg;
        }

        public ListenerRegistration<DOMNotificationListener> getDomNotifListenerReg() {
            return domNotifListenerReg;
        }

        public NotificationPublisherRegistration getNetconfNotifReg() {
            return netconfNotifReg;
        }
    }

    private static final class TranslatingNotificationListener implements DOMNotificationListener {

        private static final Logger LOG = LoggerFactory.getLogger(TranslatingNotificationListener.class);

        private final NotificationPublisherRegistration netconfNotifReg;
        private final StreamNameType streamType;
        private final SchemaService schemaService;

        TranslatingNotificationListener(final NotificationPublisherRegistration netconfNotifReg,
                                               final StreamNameType streamType, final SchemaService schemaService) {
            this.netconfNotifReg = netconfNotifReg;
            this.streamType = streamType;
            this.schemaService = schemaService;
        }

        @Override
        public void onNotification(@Nonnull final DOMNotification notif) {
            LOG.debug("Propagating notification: {} into HONEYCOMB_NETCONF", notif.getType());
            netconfNotifReg.onNotification(streamType, TranslationUtil.notificationToXml(notif, schemaService.getGlobalContext()));
        }
    }
}
