/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.listener.NbiNotificationsListenerImpl;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NbiNotificationsService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsProvider.class);
    private static Map<String, Publisher> publishersMap =  new HashMap<>();

    private final RpcProviderService rpcService;
    private ObjectRegistration<NbiNotificationsService> rpcRegistration;
    private ListenerRegistration<NbiNotificationsListener> listenerRegistration;
    private NotificationService notificationService;
    private final JsonStringConverter<org.opendaylight.yang.gen.v1
        .nbi.notifications.rev201130.NotificationService> converter;
    private final String suscriberServer;


    public NbiNotificationsProvider(List<String> topics,
            String suscriberServer, String publisherServer,
            RpcProviderService rpcProviderService, NotificationService notificationService,
            BindingDOMCodecServices bindingDOMCodecServices) {
        this.rpcService = rpcProviderService;
        this.notificationService = notificationService;
        converter =  new JsonStringConverter<>(bindingDOMCodecServices);
        for (String topic: topics) {
            LOG.info("Creating publisher for topic {}", topic);
            publishersMap.put(topic, new Publisher(topic, publisherServer, converter));
        }
        this.suscriberServer = suscriberServer;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("NbiNotificationsProvider Session Initiated");
        rpcRegistration = rpcService.registerRpcImplementation(NbiNotificationsService.class,
                new NbiNotificationsImpl(converter, suscriberServer));
        listenerRegistration = notificationService.registerNotificationListener(
                new NbiNotificationsListenerImpl(publishersMap));
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        for (Publisher publisher : publishersMap.values()) {
            publisher.close();
        }
        rpcRegistration.close();
        listenerRegistration.close();
        LOG.info("NbiNotificationsProvider Closed");
    }

}
