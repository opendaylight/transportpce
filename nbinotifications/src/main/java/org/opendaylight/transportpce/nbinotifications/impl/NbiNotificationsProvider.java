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
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceSerializer;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NbiNotificationsService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationProcessService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsProvider.class);
    private static Map<String, Publisher<NotificationProcessService>> publishersServiceMap =  new HashMap<>();
    private static Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap =  new HashMap<>();
    private final RpcProviderService rpcService;
    private final NotificationService notificationService;
    private final JsonStringConverter<NotificationProcessService> converterService;
    private final JsonStringConverter<NotificationAlarmService> converterAlarmService;
    private final String subscriberServer;
    private ObjectRegistration<NbiNotificationsService> rpcRegistration;
    private ListenerRegistration<NbiNotificationsListener> listenerRegistration;


    public NbiNotificationsProvider(List<String> publishersService, List<String> publishersAlarm,
            String subscriberServer, String publisherServer,
            RpcProviderService rpcProviderService, NotificationService notificationService,
            BindingDOMCodecServices bindingDOMCodecServices) {
        this.rpcService = rpcProviderService;
        this.notificationService = notificationService;
        converterService =  new JsonStringConverter<>(bindingDOMCodecServices);
        for (String publisherService: publishersService) {
            LOG.info("Creating publisher for the following class {}", publisherService);
            publishersServiceMap.put(publisherService, new Publisher<>(publisherService, publisherServer,
                    converterService, NotificationServiceSerializer.class));
        }
        converterAlarmService = new JsonStringConverter<>(bindingDOMCodecServices);
        for (String publisherAlarm: publishersAlarm) {
            LOG.info("Creating publisher for the following class {}", publisherAlarm);
            publishersAlarmMap.put(publisherAlarm, new Publisher<>(publisherAlarm, publisherServer,
                    converterAlarmService, NotificationAlarmServiceSerializer.class));
        }
        this.subscriberServer = subscriberServer;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("NbiNotificationsProvider Session Initiated");
        rpcRegistration = rpcService.registerRpcImplementation(NbiNotificationsService.class,
                new NbiNotificationsImpl(converterService, converterAlarmService, subscriberServer));
        listenerRegistration = notificationService.registerNotificationListener(
                new NbiNotificationsListenerImpl(publishersServiceMap, publishersAlarmMap));
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        for (Publisher<NotificationProcessService> publisher : publishersServiceMap.values()) {
            publisher.close();
        }
        for (Publisher<NotificationAlarmService> publisherAlarm : publishersAlarmMap.values()) {
            publisherAlarm.close();
        }
        rpcRegistration.close();
        listenerRegistration.close();
        LOG.info("NbiNotificationsProvider Closed");
    }

}
