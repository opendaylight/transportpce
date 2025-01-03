/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.consumer.Subscriber;
import org.opendaylight.transportpce.nbinotifications.serialization.TapiNotificationDeserializer;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.get.notification.list.output.Notification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.get.notification.list.output.NotificationKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.context.NotifSubscription;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.context.NotifSubscriptionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.subscription.service.SubscriptionFilter;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.subscription.service.SubscriptionFilterKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetNotificationListImpl implements GetNotificationList {
    private static final Logger LOG = LoggerFactory.getLogger(GetNotificationListImpl.class);

    private final JsonStringConverter<NotificationTapiService> converterTapiService;
    private final String server;
    private final NetworkTransactionService networkTransactionService;
    private final TopicManager topicManager;

    public GetNotificationListImpl(JsonStringConverter<NotificationTapiService> converterTapiService, String server,
            NetworkTransactionService networkTransactionService, TopicManager topicManager) {
        this.converterTapiService = converterTapiService;
        this.server = server;
        this.networkTransactionService = networkTransactionService;
        this.topicManager = topicManager;
    }


    @Override
    public ListenableFuture<RpcResult<GetNotificationListOutput>> invoke(GetNotificationListInput input) {
        try {
            LOG.info("RPC getNotificationList received");
            if (input == null || input.getSubscriptionId() == null) {
                LOG.warn("Missing mandatory params for input {}", input);
                return RpcResultBuilder.<GetNotificationListOutput>failed()
                    .withError(ErrorType.RPC, "Missing input parameters")
                    .buildFuture();
            }
            Uuid notifSubsUuid = input.getSubscriptionId();
            Optional<NotifSubscription> optionalNotifSub = this.networkTransactionService
                .read(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class).augmentation(Context1.class)
                        .child(NotificationContext.class)
                        .child(NotifSubscription.class, new NotifSubscriptionKey(notifSubsUuid))
                        .build())
                .get();
            if (optionalNotifSub.isEmpty()) {
                return RpcResultBuilder.<GetNotificationListOutput>failed()
                    .withError(ErrorType.APPLICATION, "Notification subscription doesnt exist")
                    .buildFuture();
            }
            NotifSubscription notifSubscription = optionalNotifSub.orElseThrow();
            List<Notification> notificationTapiList = new ArrayList<>();
            for (Map.Entry<SubscriptionFilterKey, SubscriptionFilter> sfEntry :
                    notifSubscription.getSubscriptionFilter().entrySet()) {
                for (Uuid objectUuid:sfEntry.getValue().getRequestedObjectIdentifier()) {
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
            }
            LOG.info("TAPI notifications = {}", notificationTapiList);
            Map<NotificationKey, Notification> notificationMap = new HashMap<>();
            for (Notification notif:notificationTapiList) {
                notificationMap.put(notif.key(), notif);
            }
            return RpcResultBuilder
                .success(new GetNotificationListOutputBuilder().setNotification(notificationMap).build())
                .buildFuture();
        } catch (InterruptedException | ExecutionException | NoSuchElementException e) {
            LOG.error("Failed to get Notifications from Kafka", e);
        }
        return RpcResultBuilder.<GetNotificationListOutput>failed()
            .withError(ErrorType.APPLICATION, "Notifications couldnt be retrieved from Kafka server")
            .buildFuture();
    }

}
