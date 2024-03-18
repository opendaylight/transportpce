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
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsProvider;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.subscription.service.SubscriptionFilterKey;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CreateNotificationSubscriptionServiceImpl implements CreateNotificationSubscriptionService {
    private static final Logger LOG = LoggerFactory.getLogger(CreateNotificationSubscriptionServiceImpl.class);

    private NbiNotificationsProvider nbiNotifications;
    private final TopicManager topicManager;

    public CreateNotificationSubscriptionServiceImpl(NbiNotificationsProvider nbiNotifications,
            TopicManager topicManager) {
        this.nbiNotifications = nbiNotifications;
        this.topicManager = topicManager;
    }

    @Override
    public ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> invoke(
            CreateNotificationSubscriptionServiceInput input) {
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
        Map<SubscriptionFilterKey, SubscriptionFilter> sfmap = new HashMap<>();
        sfmap.put(subscriptionFilter.key(), subscriptionFilter);
        SubscriptionService subscriptionService = new SubscriptionServiceBuilder()
            .setSubscriptionFilter(sfmap)
            .setSubscriptionState(input.getSubscriptionState())
            .setUuid(notifSubscriptionUuid)
            .build();

        NotifSubscriptionKey notifSubscriptionKey = new NotifSubscriptionKey(notifSubscriptionUuid);
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
        notifSubscriptions.put(notifSubscriptionKey, notifSubscription);
        NotificationContext notificationContext1 = new NotificationContextBuilder()
            .setNotification(notificationContext == null ? new HashMap<>() : notificationContext.getNotification())
            .setNotifSubscription(notifSubscriptions)
            .build();
        if (!nbiNotifications.updateNotificationContext(notificationContext1)) {
            LOG.error("Failed to update Notification context");
            return RpcResultBuilder.<CreateNotificationSubscriptionServiceOutput>failed()
                .withError(ErrorType.RPC, "Failed to update notification context").buildFuture();
        }
        CreateNotificationSubscriptionServiceOutput serviceOutput =
            new CreateNotificationSubscriptionServiceOutputBuilder()
                .setSubscriptionService(subscriptionService)
                .build();
        return RpcResultBuilder.success(serviceOutput).buildFuture();
    }

}
