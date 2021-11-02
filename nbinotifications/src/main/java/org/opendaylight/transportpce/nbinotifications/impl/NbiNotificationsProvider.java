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
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.listener.NbiNotificationsListenerImpl;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NbiNotificationsService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.TapiNotificationService;
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
    private final JsonStringConverter<NotificationTapiService> converterTapiService;
    private final String subscriberServer;
    private ObjectRegistration<NbiNotificationsService> rpcRegistration;
    private ListenerRegistration<NbiNotificationsListener> listenerRegistration;
    private TopicManager topicManager = TopicManager.getInstance();
    private final NetworkTransactionService networkTransactionService;


    public NbiNotificationsProvider(List<String> publishersService, List<String> publishersAlarm,
                                    String subscriberServer, String publisherServer,
                                    RpcProviderService rpcProviderService, NotificationService notificationService,
                                    BindingDOMCodecServices bindingDOMCodecServices,
                                    NetworkTransactionService networkTransactionService) {
        this.rpcService = rpcProviderService;
        this.notificationService = notificationService;
        this.topicManager.setPublisherServer(publisherServer);
        converterService =  new JsonStringConverter<>(bindingDOMCodecServices);
        this.topicManager.setProcessConverter(converterService);
        for (String publisherService: publishersService) {
            LOG.info("Creating publisher for the following class {}", publisherService);
            this.topicManager.addProcessTopic(publisherService);
        }
        converterAlarmService = new JsonStringConverter<>(bindingDOMCodecServices);
        this.topicManager.setAlarmConverter(converterAlarmService);
        for (String publisherAlarm: publishersAlarm) {
            LOG.info("Creating publisher for the following class {}", publisherAlarm);
            this.topicManager.addAlarmTopic(publisherAlarm);
        }
        this.subscriberServer = subscriberServer;
        converterTapiService = new JsonStringConverter<>(bindingDOMCodecServices);
        LOG.info("baozhi tapi converter: {}", converterTapiService);
        this.topicManager.setTapiConverter(converterTapiService);
        this.networkTransactionService = networkTransactionService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("NbiNotificationsProvider Session Initiated");
        NbiNotificationsImpl nbiImpl = new NbiNotificationsImpl(converterService, converterAlarmService,
            subscriberServer, this.networkTransactionService, this.topicManager);
        rpcRegistration = rpcService.registerRpcImplementation(NbiNotificationsService.class, nbiImpl);
        rpcService.registerRpcImplementation(TapiNotificationService.class, nbiImpl);
        NbiNotificationsListenerImpl nbiNotificationsListener =
                new NbiNotificationsListenerImpl(this.topicManager.getProcessTopicMap(),
                        this.topicManager.getAlarmTopicMap(), this.topicManager.getTapiTopicMap());
        listenerRegistration = notificationService.registerNotificationListener(nbiNotificationsListener);
        this.topicManager.setNbiNotificationsListener(nbiNotificationsListener);
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
