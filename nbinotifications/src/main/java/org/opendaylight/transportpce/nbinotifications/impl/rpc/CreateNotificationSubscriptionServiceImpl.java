/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsImpl;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.create.notification.subscription.service.output.SubscriptionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.create.notification.subscription.service.output.SubscriptionServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.context.NotifSubscription;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.context.NotifSubscriptionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.context.NotifSubscriptionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.subscription.service.SubscriptionFilter;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.subscription.service.SubscriptionFilterBuilder;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CreateNotificationSubscriptionServiceImpl implements CreateNotificationSubscriptionService {
    private static final Logger LOG = LoggerFactory.getLogger(CreateNotificationSubscriptionServiceImpl.class);

    private NbiNotificationsImpl nbiNotifications;
    private final TopicManager topicManager;

    public CreateNotificationSubscriptionServiceImpl(NbiNotificationsImpl nbiNotifications, TopicManager topicManager) {
        this.nbiNotifications = nbiNotifications;
        this.topicManager = topicManager;
    }

    @Override
    public ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> invoke(
            CreateNotificationSubscriptionServiceInput input) {
        for (Uuid uuid : input.getSubscriptionFilter().getRequestedObjectIdentifier()) {
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
            .setSubscriptionFilter(new HashMap<>(Map.of(subscriptionFilter.key(), subscriptionFilter)))
            .setSubscriptionState(input.getSubscriptionState())
            .setUuid(notifSubscriptionUuid)
            .build();
        NotifSubscription notifSubscription = new NotifSubscriptionBuilder()
            .setSubscriptionState(subscriptionService.getSubscriptionState())
            .setSubscriptionFilter(subscriptionService.getSubscriptionFilter())
            .setUuid(notifSubscriptionUuid)
//            Following 2 items are no more in notification-context with T-API 2.4
//            .setSupportedNotificationTypes(notificationTypes)
//            .setSupportedObjectTypes(objectTypes)
            .setName(subscriptionService.getName())
            .build();
        NotificationContext notificationContext = nbiNotifications.getNotificationContext();
        Map<NotifSubscriptionKey, NotifSubscription> notifSubscriptions = new HashMap<>();
        if (notificationContext != null && notificationContext.getNotifSubscription() != null) {
            notifSubscriptions.putAll(notificationContext.getNotifSubscription());
        }
        notifSubscriptions.put(new NotifSubscriptionKey(notifSubscriptionUuid), notifSubscription);
        if (!nbiNotifications.updateNotificationContext(
            new NotificationContextBuilder()
                .setNotification(notificationContext == null ? new HashMap<>() : notificationContext.getNotification())
                .setNotifSubscription(notifSubscriptions)
                .build())) {
            LOG.error("Failed to update Notification context");
            return RpcResultBuilder.<CreateNotificationSubscriptionServiceOutput>failed()
                .withError(ErrorType.RPC, "Failed to update notification context")
                .buildFuture();
        }
        return RpcResultBuilder
                .success(
                    new CreateNotificationSubscriptionServiceOutputBuilder()
                        .setSubscriptionService(subscriptionService)
                        .build())
                .buildFuture();
    }
}
