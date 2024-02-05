/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsImpl;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.context.NotifSubscriptionKey;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetNotificationSubscriptionServiceDetailsImpl implements GetNotificationSubscriptionServiceDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetNotificationSubscriptionServiceDetailsImpl.class);

    private NbiNotificationsImpl nbiNotifications;

    public GetNotificationSubscriptionServiceDetailsImpl(NbiNotificationsImpl nbiNotifications) {
        this.nbiNotifications = nbiNotifications;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationSubscriptionServiceDetailsOutput>> invoke(
            GetNotificationSubscriptionServiceDetailsInput input) {
        if (input == null || input.getUuid() == null) {
            LOG.warn("Missing mandatory params for input {}", input);
            return RpcResultBuilder.<GetNotificationSubscriptionServiceDetailsOutput>failed()
                .withError(ErrorType.RPC, "Missing input parameters").buildFuture();
        }
        Uuid notifSubsUuid = input.getUuid();
        NotificationContext notificationContext = nbiNotifications.getNotificationContext();
        if (notificationContext == null) {
            return RpcResultBuilder.<GetNotificationSubscriptionServiceDetailsOutput>failed()
                .withError(ErrorType.APPLICATION, "Notification context is empty")
                .buildFuture();
        }
        if (notificationContext.getNotifSubscription() == null) {
            return RpcResultBuilder.success(new GetNotificationSubscriptionServiceDetailsOutputBuilder()
                .setSubscriptionService(new org.opendaylight.yang.gen.v1
                    .urn.onf.otcc.yang.tapi.notification.rev221121.get.notification.subscription.service
                        .details.output.SubscriptionServiceBuilder().build()).build()).buildFuture();
        }
        if (!notificationContext.getNotifSubscription().containsKey(new NotifSubscriptionKey(notifSubsUuid))) {
            return RpcResultBuilder.<GetNotificationSubscriptionServiceDetailsOutput>failed()
                .withError(ErrorType.APPLICATION,
                    "Notification subscription service doesnt exist").buildFuture();
        }
        return RpcResultBuilder.success(new GetNotificationSubscriptionServiceDetailsOutputBuilder()
            .setSubscriptionService(new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.notification.rev221121.get.notification.subscription.service.details.output
                .SubscriptionServiceBuilder(notificationContext.getNotifSubscription().get(
                    new NotifSubscriptionKey(notifSubsUuid))).build()).build()).buildFuture();
    }

}
