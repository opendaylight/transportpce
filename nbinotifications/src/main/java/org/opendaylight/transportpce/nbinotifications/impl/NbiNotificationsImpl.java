/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.consumer.Subscriber;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceDeserializer;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceDeserializer;
import org.opendaylight.transportpce.nbinotifications.serialization.TapiNotificationDeserializer;
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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiService;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.DeleteNotificationSubscriptionServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetSupportedNotificationTypesInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetSupportedNotificationTypesOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetSupportedNotificationTypesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.NotificationType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.ObjectType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.TapiNotificationService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.UpdateNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.UpdateNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.context.NotificationContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.context.NotificationContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.create.notification.subscription.service.output.SubscriptionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.create.notification.subscription.service.output.SubscriptionServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.list.output.Notification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.list.output.NotificationKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.subscription.service.list.output.SubscriptionServiceKey;
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
    private final JsonStringConverter<NotificationTapiService> converterTapiService;
    private final String server;
    private final NetworkTransactionService networkTransactionService;
    private final TopicManager topicManager;

    public NbiNotificationsImpl(JsonStringConverter<NotificationProcessService> converterService,
                                JsonStringConverter<NotificationAlarmService> converterAlarmService,
                                JsonStringConverter<NotificationTapiService> converterTapiService, String server,
                                NetworkTransactionService networkTransactionService, TopicManager topicManager) {
        this.converterService = converterService;
        this.converterAlarmService = converterAlarmService;
        this.converterTapiService = converterTapiService;
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
        NotificationContext notificationContext = getNotificationContext();
        if (notificationContext == null) {
            return RpcResultBuilder.<GetSupportedNotificationTypesOutput>failed()
                .withError(RpcError.ErrorType.APPLICATION, "Couldnt get Notification Context from Datastore")
                .buildFuture();
        }
        if (notificationContext.getNotifSubscription() == null) {
            return RpcResultBuilder.success(new GetSupportedNotificationTypesOutputBuilder()
                .setSupportedNotificationTypes(new ArrayList<>())
                .setSupportedObjectTypes(new ArrayList<>()).build()).buildFuture();
        }
        List<NotificationType> notificationTypeList = new ArrayList<>();
        List<ObjectType> objectTypeList = new ArrayList<>();
        for (NotifSubscription notifSubscription:notificationContext.getNotifSubscription().values()) {
            if (notifSubscription.getSupportedNotificationTypes() != null) {
                for (NotificationType notificationType:notifSubscription.getSupportedNotificationTypes()) {
                    if (!notificationTypeList.contains(notificationType)) {
                        notificationTypeList.add(notificationType);
                    }
                }
            }
            if (notifSubscription.getSupportedObjectTypes() != null) {
                for (ObjectType objectType:notifSubscription.getSupportedObjectTypes()) {
                    if (!objectTypeList.contains(objectType)) {
                        objectTypeList.add(objectType);
                    }
                }
            }
        }
        return RpcResultBuilder.success(new GetSupportedNotificationTypesOutputBuilder()
            .setSupportedNotificationTypes(notificationTypeList)
            .setSupportedObjectTypes(objectTypeList).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>>
            createNotificationSubscriptionService(CreateNotificationSubscriptionServiceInput input) {
        for (Uuid uuid:input.getSubscriptionFilter().getRequestedObjectIdentifier()) {
            LOG.info("Adding T-API topic: {} to Kafka server", uuid.getValue());
            this.topicManager.addTapiTopic(uuid.getValue());
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
        Uuid notifSubscriptionUuid = new Uuid(UUID.randomUUID().toString());
        SubscriptionService subscriptionService = new SubscriptionServiceBuilder()
            .setSubscriptionFilter(subscriptionFilter)
            .setSubscriptionState(input.getSubscriptionState())
            .setUuid(notifSubscriptionUuid)
            .build();

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
        NotificationContext notificationContext = getNotificationContext();
        Map<NotifSubscriptionKey, NotifSubscription> notifSubscriptions = new HashMap<>();
        if (notificationContext != null && notificationContext.getNotifSubscription() != null) {
            notifSubscriptions.putAll(notificationContext.getNotifSubscription());
        }
        notifSubscriptions.put(notifSubscriptionKey, notifSubscription);
        NotificationContext notificationContext1 = new NotificationContextBuilder()
            .setNotification(notificationContext == null ? new HashMap<>() : notificationContext.getNotification())
            .setNotifSubscription(notifSubscriptions)
            .build();
        if (!updateNotificationContext(notificationContext1)) {
            LOG.error("Failed to update Notification context");
            return RpcResultBuilder.<CreateNotificationSubscriptionServiceOutput>failed()
                .withError(RpcError.ErrorType.RPC, "Failed to update notification context").buildFuture();
        }
        CreateNotificationSubscriptionServiceOutput serviceOutput =
            new CreateNotificationSubscriptionServiceOutputBuilder()
                .setSubscriptionService(subscriptionService)
                .build();
        return RpcResultBuilder.success(serviceOutput).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<UpdateNotificationSubscriptionServiceOutput>>
            updateNotificationSubscriptionService(UpdateNotificationSubscriptionServiceInput input) {
        // TODO --> Not yet implemented
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteNotificationSubscriptionServiceOutput>>
            deleteNotificationSubscriptionService(DeleteNotificationSubscriptionServiceInput input) {
        try {
            if (input == null || input.getSubscriptionIdOrName() == null) {
                LOG.warn("Missing mandatory params for input {}", input);
                return RpcResultBuilder.<DeleteNotificationSubscriptionServiceOutput>failed()
                    .withError(RpcError.ErrorType.RPC, "Missing input parameters").buildFuture();
            }
            Uuid notifSubsUuid = getUuidFromIput(input.getSubscriptionIdOrName());
            InstanceIdentifier<NotifSubscription> notifSubscriptionIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(NotificationContext.class).child(NotifSubscription.class,
                    new NotifSubscriptionKey(notifSubsUuid)).build();
            Optional<NotifSubscription> optionalNotifSub = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, notifSubscriptionIID).get();

            if (!optionalNotifSub.isPresent()) {
                return RpcResultBuilder.<DeleteNotificationSubscriptionServiceOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION,
                        "Notification subscription doesnt exist").buildFuture();
            }
            NotifSubscription notifSubscription = optionalNotifSub.get();
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, notifSubscriptionIID);
            this.networkTransactionService.commit().get();
            for (Uuid objectUuid:notifSubscription.getSubscriptionFilter().getRequestedObjectIdentifier()) {
                this.topicManager.deleteTapiTopic(objectUuid.getValue());
            }
            return RpcResultBuilder.success(new DeleteNotificationSubscriptionServiceOutputBuilder().build())
                .buildFuture();
        } catch (InterruptedException | ExecutionException | NoSuchElementException e) {
            LOG.error("Failed to delete Notification subscription service", e);
        }
        return RpcResultBuilder.<DeleteNotificationSubscriptionServiceOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION,
                "Failed to delete notification subscription service").buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationSubscriptionServiceDetailsOutput>>
            getNotificationSubscriptionServiceDetails(GetNotificationSubscriptionServiceDetailsInput input) {
        if (input == null || input.getSubscriptionIdOrName() == null) {
            LOG.warn("Missing mandatory params for input {}", input);
            return RpcResultBuilder.<GetNotificationSubscriptionServiceDetailsOutput>failed()
                .withError(RpcError.ErrorType.RPC, "Missing input parameters").buildFuture();
        }
        Uuid notifSubsUuid = getUuidFromIput(input.getSubscriptionIdOrName());
        NotificationContext notificationContext = getNotificationContext();
        if (notificationContext == null) {
            return RpcResultBuilder.<GetNotificationSubscriptionServiceDetailsOutput>failed()
                .withError(RpcError.ErrorType.APPLICATION, "Notification context is empty")
                .buildFuture();
        }
        if (notificationContext.getNotifSubscription() == null) {
            return RpcResultBuilder.success(new GetNotificationSubscriptionServiceDetailsOutputBuilder()
                .setSubscriptionService(new org.opendaylight.yang.gen.v1
                    .urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.subscription.service
                        .details.output.SubscriptionServiceBuilder().build()).build()).buildFuture();
        }
        if (!notificationContext.getNotifSubscription().containsKey(new NotifSubscriptionKey(notifSubsUuid))) {
            return RpcResultBuilder.<GetNotificationSubscriptionServiceDetailsOutput>failed()
                .withError(RpcError.ErrorType.APPLICATION,
                    "Notification subscription service doesnt exist").buildFuture();
        }
        return RpcResultBuilder.success(new GetNotificationSubscriptionServiceDetailsOutputBuilder()
            .setSubscriptionService(new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.notification.rev181210.get.notification.subscription.service.details.output
                .SubscriptionServiceBuilder(notificationContext.getNotifSubscription().get(
                    new NotifSubscriptionKey(notifSubsUuid))).build())).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationSubscriptionServiceListOutput>>
            getNotificationSubscriptionServiceList(GetNotificationSubscriptionServiceListInput input) {
        NotificationContext notificationContext = getNotificationContext();
        if (notificationContext == null) {
            return RpcResultBuilder.<GetNotificationSubscriptionServiceListOutput>failed()
                .withError(RpcError.ErrorType.APPLICATION, "Notification context is empty")
                .buildFuture();
        }
        if (notificationContext.getNotifSubscription() == null) {
            return RpcResultBuilder.success(new GetNotificationSubscriptionServiceListOutputBuilder()
                .setSubscriptionService(new HashMap<>()).build()).buildFuture();
        }
        Map<SubscriptionServiceKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang
            .tapi.notification.rev181210.get.notification.subscription.service.list.output.SubscriptionService>
                notifSubsMap = new HashMap<>();
        for (NotifSubscription notifSubscription:notificationContext.getNotifSubscription().values()) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang
                .tapi.notification.rev181210.get.notification.subscription.service.list.output.SubscriptionService
                    subscriptionService = new org.opendaylight.yang.gen.v1
                        .urn.onf.otcc.yang.tapi.notification.rev181210.get.notification.subscription.service
                            .list.output.SubscriptionServiceBuilder(notifSubscription).build();
            notifSubsMap.put(subscriptionService.key(), subscriptionService);
        }
        return RpcResultBuilder.success(new GetNotificationSubscriptionServiceListOutputBuilder()
            .setSubscriptionService(notifSubsMap).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationListOutput>> getNotificationList(GetNotificationListInput input) {
        try {
            LOG.info("RPC getNotificationList received");
            if (input == null || input.getSubscriptionIdOrName() == null) {
                LOG.warn("Missing mandatory params for input {}", input);
                return RpcResultBuilder.<GetNotificationListOutput>failed().withError(RpcError.ErrorType.RPC,
                    "Missing input parameters").buildFuture();
            }
            Uuid notifSubsUuid = getUuidFromIput(input.getSubscriptionIdOrName());
            InstanceIdentifier<NotifSubscription> notifSubscriptionIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(NotificationContext.class).child(NotifSubscription.class,
                    new NotifSubscriptionKey(notifSubsUuid)).build();
            Optional<NotifSubscription> optionalNotifSub = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, notifSubscriptionIID).get();

            if (!optionalNotifSub.isPresent()) {
                return RpcResultBuilder.<GetNotificationListOutput>failed()
                    .withError(RpcError.ErrorType.APPLICATION,
                        "Notification subscription doesnt exist").buildFuture();
            }
            NotifSubscription notifSubscription = optionalNotifSub.get();
            List<Notification> notificationTapiList = new ArrayList<>();
            for (Uuid objectUuid:notifSubscription.getSubscriptionFilter().getRequestedObjectIdentifier()) {
                if (!this.topicManager.getTapiTopicMap().containsKey(objectUuid.getValue())) {
                    LOG.warn("Topic doesnt exist for {}", objectUuid.getValue());
                    continue;
                }
                LOG.info("Going to get notifications for topic {}", objectUuid.getValue());
                Subscriber<NotificationTapiService, Notification> subscriber = new Subscriber<>(
                    objectUuid.getValue(), objectUuid.getValue(), server, converterTapiService,
                    TapiNotificationDeserializer.class);
                notificationTapiList.addAll(subscriber.subscribe(objectUuid.getValue(), Notification.QNAME));
            }
            LOG.info("TAPI notifications = {}", notificationTapiList);
            Map<NotificationKey, Notification> notificationMap = new HashMap<>();
            for (Notification notif:notificationTapiList) {
                notificationMap.put(notif.key(), notif);
            }
            return RpcResultBuilder.success(new GetNotificationListOutputBuilder()
                .setNotification(notificationMap).build()).buildFuture();
        } catch (InterruptedException | ExecutionException | NoSuchElementException e) {
            LOG.error("Failed to get Notifications from Kafka", e);
        }
        return RpcResultBuilder.<GetNotificationListOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION,
                "Notifications couldnt be retrieved from Kafka server").buildFuture();
    }

    private NotificationContext getNotificationContext() {
        LOG.info("Getting tapi notification context");
        try {
            InstanceIdentifier<NotificationContext> notificationcontextIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(NotificationContext.class).build();
            Optional<NotificationContext> notificationContextOptional
                = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, notificationcontextIID).get();
            if (!notificationContextOptional.isPresent()) {
                LOG.error("Could not get TAPI notification context");
                return null;
            }
            return notificationContextOptional.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not get TAPI notification context");
        }
        return null;
    }

    private boolean updateNotificationContext(NotificationContext notificationContext1) {
        try {
            InstanceIdentifier<NotificationContext> notificationcontextIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(NotificationContext.class).build();
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, notificationcontextIID,
                notificationContext1);
            this.networkTransactionService.commit().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not update TAPI notification context");
        }
        return false;
    }

    private Uuid getUuidFromIput(String serviceIdOrName) {
        try {
            UUID.fromString(serviceIdOrName);
            LOG.info("Given attribute {} is a UUID", serviceIdOrName);
            return new Uuid(serviceIdOrName);
        } catch (IllegalArgumentException e) {
            LOG.info("Given attribute {} is not a UUID", serviceIdOrName);
            return new Uuid(UUID.nameUUIDFromBytes(serviceIdOrName.getBytes(StandardCharsets.UTF_8)).toString());
        }
    }
}
