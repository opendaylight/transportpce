/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.consumer.Subscriber;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceDeserializer;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceDeserializer;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsAlarmServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsAlarmServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsAlarmServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsProcessServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsProcessServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsProcessServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NbiNotificationsService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.get.notifications.alarm.service.output.NotificationsAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.get.notifications.process.service.output.NotificationsProcessService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.DeleteNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.DeleteNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetSupportedNotificationTypesInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetSupportedNotificationTypesOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.NotificationType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.ObjectType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.TapiNotificationService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.UpdateNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.UpdateNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.context.NotificationContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.context.NotificationContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.create.notification.subscription.service.output.SubscriptionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.create.notification.subscription.service.output.SubscriptionServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.context.NotifSubscription;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.context.NotifSubscriptionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.context.NotifSubscriptionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.subscription.service.SubscriptionFilter;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.subscription.service.SubscriptionFilterBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsImpl implements NbiNotificationsService, TapiNotificationService {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsImpl.class);
    private final JsonStringConverter<NotificationProcessService> converterService;
    private final JsonStringConverter<NotificationAlarmService> converterAlarmService;
    private final String server;
    private final NetworkTransactionService networkTransactionService;
    private final TopicManager topicManager;

    public NbiNotificationsImpl(JsonStringConverter<NotificationProcessService> converterService,
                                JsonStringConverter<NotificationAlarmService> converterAlarmService, String server,
                                NetworkTransactionService networkTransactionService, TopicManager topicManager) {
        this.converterService = converterService;
        this.converterAlarmService = converterAlarmService;
        this.server = server;
        this.networkTransactionService = networkTransactionService;
        this.topicManager = topicManager;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationsProcessServiceOutput>> getNotificationsProcessService(
            GetNotificationsProcessServiceInput input) {
        LOG.info("RPC getNotificationsService received");
        if (input == null || input.getIdConsumer() == null || input.getGroupId() == null) {
            LOG.warn("Missing mandatory params for input {}", input);
            return RpcResultBuilder.success(new GetNotificationsProcessServiceOutputBuilder().build()).buildFuture();
        }
        Subscriber<NotificationProcessService, NotificationsProcessService> subscriber = new Subscriber<>(
                input.getIdConsumer(), input.getGroupId(), server, converterService,
                NotificationServiceDeserializer.class);
        List<NotificationsProcessService> notificationServiceList = subscriber
                .subscribe(input.getConnectionType().getName(), NotificationsProcessService.QNAME);
        return RpcResultBuilder.success(new GetNotificationsProcessServiceOutputBuilder()
                .setNotificationsProcessService(notificationServiceList).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationsAlarmServiceOutput>> getNotificationsAlarmService(
            GetNotificationsAlarmServiceInput input) {
        LOG.info("RPC getNotificationsAlarmService received");
        if (input == null || input.getIdConsumer() == null || input.getGroupId() == null) {
            LOG.warn("Missing mandatory params for input {}", input);
            return RpcResultBuilder.success(new GetNotificationsAlarmServiceOutputBuilder().build()).buildFuture();
        }
        Subscriber<NotificationAlarmService, NotificationsAlarmService> subscriber = new Subscriber<>(
                input.getIdConsumer(), input.getGroupId(), server, converterAlarmService,
                NotificationAlarmServiceDeserializer.class);
        List<NotificationsAlarmService> notificationAlarmServiceList = subscriber
                .subscribe("alarm" + input.getConnectionType().getName(), NotificationsAlarmService.QNAME);
        return RpcResultBuilder.success(new GetNotificationsAlarmServiceOutputBuilder()
                .setNotificationsAlarmService(notificationAlarmServiceList).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetSupportedNotificationTypesOutput>>
        getSupportedNotificationTypes(GetSupportedNotificationTypesInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>>
            createNotificationSubscriptionService(CreateNotificationSubscriptionServiceInput input) {
        try {
            for (Uuid uuid:input.getSubscriptionFilter().getRequestedObjectIdentifier()) {
                LOG.info("Adding T-API topic: {} to Kafka server", uuid.getValue());
                this.topicManager.addTapiTopic(uuid.getValue());
            }

            InstanceIdentifier<NotificationContext> notificationcontextIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.notification.rev181210.context.NotificationContext.class)
                    .build();
            Optional<NotificationContext> notificationContextOptional
                = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, notificationcontextIID)
                .get();
            if (notificationContextOptional.isEmpty()) {
                LOG.error("Could not create TAPI notification subscription service");
                return RpcResultBuilder.<CreateNotificationSubscriptionServiceOutput>failed()
                    .withError(RpcError.ErrorType.RPC, "Could not read notification context")
                    .buildFuture();
            }
            SubscriptionFilter subscriptionFilter = new SubscriptionFilterBuilder()
                .setName(input.getSubscriptionFilter().getName())
                .setLocalId(input.getSubscriptionFilter().getLocalId())
                .setIncludeContent(input.getSubscriptionFilter().getIncludeContent())
                .setRequestedNotificationTypes(input.getSubscriptionFilter().getRequestedNotificationTypes())
                .setRequestedLayerProtocols(input.getSubscriptionFilter().getRequestedLayerProtocols())
                .setRequestedObjectIdentifier(input.getSubscriptionFilter().getRequestedObjectIdentifier())
                .setRequestedObjectTypes(input.getSubscriptionFilter().getRequestedObjectTypes())
                .build();
            SubscriptionService subscriptionService = new SubscriptionServiceBuilder()
                .setSubscriptionFilter(subscriptionFilter)
                .setSubscriptionState(input.getSubscriptionState()).build();
            Uuid notifSubscriptionUuid = new Uuid(UUID.randomUUID().toString());
            NotifSubscriptionKey notifSubscriptionKey = new NotifSubscriptionKey(notifSubscriptionUuid);
            List<NotificationType> notificationTypes = (subscriptionFilter.getRequestedNotificationTypes() != null)
                ? subscriptionFilter.getRequestedNotificationTypes()
                : new ArrayList<>(Arrays.asList(NotificationType.ALARMEVENT));
            List<ObjectType> objectTypes = (subscriptionFilter.getRequestedObjectTypes() != null)
                ? subscriptionFilter.getRequestedObjectTypes()
                : new ArrayList<>(Arrays.asList(ObjectType.CONNECTIVITYSERVICE));
            NotifSubscription notifSubscription = new NotifSubscriptionBuilder()
                .setSubscriptionState(subscriptionService.getSubscriptionState())
                .setSubscriptionFilter(subscriptionService.getSubscriptionFilter())
                .setUuid(notifSubscriptionUuid)
                .setSupportedNotificationTypes(notificationTypes)
                .setSupportedObjectTypes(objectTypes)
                .setName(subscriptionService.getName())
                .build();
            NotificationContext notificationContext = notificationContextOptional.get();
            Map<NotifSubscriptionKey, NotifSubscription> notifSubscriptions = new HashMap<>();
            if (notificationContext.getNotifSubscription() != null) {
                notifSubscriptions.putAll(notificationContext.getNotifSubscription());
            }
            notifSubscriptions.put(notifSubscriptionKey, notifSubscription);
            NotificationContext notificationContext1 = new NotificationContextBuilder()
                .setNotification(notificationContext.getNotification())
                .setNotifSubscription(notifSubscriptions)
                .build();
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, notificationcontextIID,
                notificationContext1);
            this.networkTransactionService.commit().get();
            CreateNotificationSubscriptionServiceOutput serviceOutput =
                new CreateNotificationSubscriptionServiceOutputBuilder()
                    .setSubscriptionService(subscriptionService)
                    .build();
            return RpcResultBuilder.success(serviceOutput).buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not create TAPI notification subscription service");
            return RpcResultBuilder.<CreateNotificationSubscriptionServiceOutput>failed()
                .withError(RpcError.ErrorType.RPC, "Could not read notification context").buildFuture();
        }
    }

    @Override
    public ListenableFuture<RpcResult<UpdateNotificationSubscriptionServiceOutput>>
        updateNotificationSubscriptionService(UpdateNotificationSubscriptionServiceInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteNotificationSubscriptionServiceOutput>>
        deleteNotificationSubscriptionService(DeleteNotificationSubscriptionServiceInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationSubscriptionServiceDetailsOutput>>
        getNotificationSubscriptionServiceDetails(GetNotificationSubscriptionServiceDetailsInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationSubscriptionServiceListOutput>>
            getNotificationSubscriptionServiceList(GetNotificationSubscriptionServiceListInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationListOutput>> getNotificationList(GetNotificationListInput input) {
        return null;
    }
}
