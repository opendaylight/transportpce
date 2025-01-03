/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.CreateNotificationSubscriptionServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.DeleteNotificationSubscriptionServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationListImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationSubscriptionServiceDetailsImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationSubscriptionServiceListImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationsAlarmServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationsProcessServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetSupportedNotificationTypesImpl;
import org.opendaylight.transportpce.nbinotifications.listener.NbiNotificationsHandler;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContext;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
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
    private Map<String, Publisher<NotificationProcessService>> publishersServiceMap;
    private Map<String, Publisher<NotificationAlarmService>> publishersAlarmMap;
    private Registration listenerRegistration;
    private Registration rpcRegistration;
    private NetworkTransactionService networkTransactionService;

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
        this.networkTransactionService = networkTransactionService;
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
        LOG.info("tapi converter: {}", converterTapiService);
        topicManager.setTapiConverter(converterTapiService);

        rpcRegistration = rpcProviderService.registerRpcImplementations(
                new GetNotificationsProcessServiceImpl(converterService, subscriberServer),
                new GetNotificationsAlarmServiceImpl(converterAlarmService, subscriberServer),
                new GetSupportedNotificationTypesImpl(this),
                new CreateNotificationSubscriptionServiceImpl(this, topicManager),
                new DeleteNotificationSubscriptionServiceImpl(networkTransactionService, topicManager),
                new GetNotificationSubscriptionServiceDetailsImpl(this),
                new GetNotificationSubscriptionServiceListImpl(this),
                new GetNotificationListImpl(converterTapiService, subscriberServer, networkTransactionService,
                        topicManager));

        NbiNotificationsHandler notificationsListener = new NbiNotificationsHandler(
            topicManager.getProcessTopicMap(), topicManager.getAlarmTopicMap(), topicManager.getTapiTopicMap());
        listenerRegistration = notificationService.registerCompositeListener(
            notificationsListener.getCompositeListener());
        topicManager.setNbiNotificationsListener(notificationsListener);
        publishersServiceMap = topicManager.getProcessTopicMap();
        publishersAlarmMap = topicManager.getAlarmTopicMap();
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

    public NotificationContext getNotificationContext() {
        LOG.info("Getting tapi notification context");
        try {
            Optional<NotificationContext> notificationContextOptional = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class)
                        .augmentation(Context1.class).child(NotificationContext.class)
                        .build())
                .get();
            if (notificationContextOptional.isPresent()) {
                return notificationContextOptional.orElseThrow();
            }
            LOG.debug("notification context is empty");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Caught exception getting Notification Context", e);
        }
        LOG.error("Could not get TAPI notification context");
        return null;
    }

    public boolean updateNotificationContext(NotificationContext notificationContext1) {
        try {
            this.networkTransactionService.merge(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class)
                        .augmentation(Context1.class).child(NotificationContext.class)
                        .build(),
                    notificationContext1);
            this.networkTransactionService.commit().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not update TAPI notification context");
        }
        return false;
    }

}
