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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationTapiService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "org.opendaylight.transportpce.nbinotifications")
public class NbiNotificationsProvider {

    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition
        String suscriberServer() default "";
        @AttributeDefinition
        String publisherServer() default "";
    }

    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsProvider.class);
    private static Map<String, Publisher<NotificationProcessService>> publishersServiceMap =  new HashMap<>();
    private static Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap =  new HashMap<>();
    private ListenerRegistration<NbiNotificationsListener> listenerRegistration;
    private Registration rpcRegistration;

    @Activate
    public NbiNotificationsProvider(@Reference RpcProviderService rpcProviderService,
            @Reference NotificationService notificationService,
            @Reference BindingDOMCodecServices bindingDOMCodecServices,
            @Reference NetworkTransactionService networkTransactionService,
            final Configuration configuration) {
        this(configuration.suscriberServer(), configuration.publisherServer(), rpcProviderService, notificationService,
                bindingDOMCodecServices, networkTransactionService);
    }

    public NbiNotificationsProvider(String subscriberServer, String publisherServer,
            RpcProviderService rpcProviderService, NotificationService notificationService,
            BindingDOMCodecServices bindingDOMCodecServices, NetworkTransactionService networkTransactionService) {
        List<String> publishersServiceList = List.of("PceListener", "ServiceHandlerOperations", "ServiceHandler",
                "RendererListener");
        TopicManager topicManager = TopicManager.getInstance();
        topicManager.setPublisherServer(publisherServer);
        JsonStringConverter<NotificationProcessService> converterService =
            new JsonStringConverter<>(bindingDOMCodecServices);
        topicManager.setProcessConverter(converterService);
        for (String publisherService: publishersServiceList) {
            LOG.info("Creating publisher for the following class {}", publisherService);
            topicManager.addProcessTopic(publisherService);
        }
        JsonStringConverter<NotificationAlarmService> converterAlarmService =
                new JsonStringConverter<>(bindingDOMCodecServices);
        topicManager.setAlarmConverter(converterAlarmService);
        List<String> publishersAlarmList = List.of("ServiceListener");
        for (String publisherAlarm: publishersAlarmList) {
            LOG.info("Creating publisher for the following class {}", publisherAlarm);
            topicManager.addAlarmTopic(publisherAlarm);
        }
        JsonStringConverter<NotificationTapiService> converterTapiService =
                new JsonStringConverter<>(bindingDOMCodecServices);
        LOG.info("baozhi tapi converter: {}", converterTapiService);
        topicManager.setTapiConverter(converterTapiService);

        NbiNotificationsImpl nbiImpl = new NbiNotificationsImpl(converterService, converterAlarmService,
            converterTapiService, subscriberServer, networkTransactionService, topicManager);
        rpcRegistration = rpcProviderService.registerRpcImplementations(nbiImpl.registerRPCs());
        NbiNotificationsListenerImpl nbiNotificationsListener = new NbiNotificationsListenerImpl(
                topicManager.getProcessTopicMap(), topicManager.getAlarmTopicMap(), topicManager.getTapiTopicMap());
        listenerRegistration = notificationService.registerNotificationListener(nbiNotificationsListener);
        topicManager.setNbiNotificationsListener(nbiNotificationsListener);
        LOG.info("NbiNotificationsProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    @Deactivate
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
