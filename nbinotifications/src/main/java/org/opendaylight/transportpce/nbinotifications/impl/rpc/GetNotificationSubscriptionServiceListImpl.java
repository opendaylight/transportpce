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
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsProvider;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.get.notification.subscription.service.list.output.SubscriptionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.get.notification.subscription.service.list.output.SubscriptionServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.get.notification.subscription.service.list.output.SubscriptionServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.context.NotifSubscription;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


public class GetNotificationSubscriptionServiceListImpl implements GetNotificationSubscriptionServiceList {

    private NbiNotificationsProvider nbiNotifications;

    public GetNotificationSubscriptionServiceListImpl(NbiNotificationsProvider nbiNotifications) {
        this.nbiNotifications = nbiNotifications;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationSubscriptionServiceListOutput>> invoke(
            GetNotificationSubscriptionServiceListInput input) {
        NotificationContext notificationContext = nbiNotifications.getNotificationContext();
        if (notificationContext == null) {
            return RpcResultBuilder.<GetNotificationSubscriptionServiceListOutput>failed()
                .withError(ErrorType.APPLICATION, "Notification context is empty")
                .buildFuture();
        }
        if (notificationContext.getNotifSubscription() == null) {
            return RpcResultBuilder
                .success(new GetNotificationSubscriptionServiceListOutputBuilder()
                    .setSubscriptionService(new HashMap<>())
                    .build())
                .buildFuture();
        }
        Map<SubscriptionServiceKey, SubscriptionService> notifSubsMap = new HashMap<>();
        for (NotifSubscription notifSubscription:notificationContext.getNotifSubscription().values()) {
            SubscriptionService subscriptionService = new SubscriptionServiceBuilder(notifSubscription).build();
            notifSubsMap.put(subscriptionService.key(), subscriptionService);
        }
        return RpcResultBuilder
            .success(
                new GetNotificationSubscriptionServiceListOutputBuilder().setSubscriptionService(notifSubsMap).build())
            .buildFuture();
    }

}
