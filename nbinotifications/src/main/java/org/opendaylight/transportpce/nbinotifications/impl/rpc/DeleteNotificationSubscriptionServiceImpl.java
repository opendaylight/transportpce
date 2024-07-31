/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.DeleteNotificationSubscriptionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.DeleteNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.DeleteNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.DeleteNotificationSubscriptionServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContext;
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


public class DeleteNotificationSubscriptionServiceImpl implements DeleteNotificationSubscriptionService {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteNotificationSubscriptionServiceImpl.class);

    private final NetworkTransactionService networkTransactionService;
    private final TopicManager topicManager;

    public DeleteNotificationSubscriptionServiceImpl(
            NetworkTransactionService networkTransactionService, TopicManager topicManager) {
        this.networkTransactionService = networkTransactionService;
        this.topicManager = topicManager;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteNotificationSubscriptionServiceOutput>> invoke(
            DeleteNotificationSubscriptionServiceInput input) {
        try {
            if (input == null || input.getUuid() == null) {
                LOG.warn("Missing mandatory params for input {}", input);
                return RpcResultBuilder.<DeleteNotificationSubscriptionServiceOutput>failed()
                    .withError(ErrorType.RPC, "Missing input parameters")
                    .buildFuture();
            }
            Uuid notifSubsUuid = input.getUuid();
            DataObjectIdentifier<NotifSubscription> notifSubscriptionIID = DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(NotificationContext.class)
                .child(NotifSubscription.class, new NotifSubscriptionKey(notifSubsUuid))
                .build();
            Optional<NotifSubscription> optionalNotifSub = this.networkTransactionService
                .read(LogicalDatastoreType.OPERATIONAL, notifSubscriptionIID)
                .get();
            if (optionalNotifSub.isEmpty()) {
                return RpcResultBuilder.<DeleteNotificationSubscriptionServiceOutput>failed()
                    .withError(ErrorType.APPLICATION, "Notification subscription doesnt exist")
                    .buildFuture();
            }
            NotifSubscription notifSubscription = optionalNotifSub.orElseThrow();
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, notifSubscriptionIID);
            this.networkTransactionService.commit().get();
            for (Map.Entry<SubscriptionFilterKey, SubscriptionFilter> sfEntry :
                    notifSubscription.getSubscriptionFilter().entrySet()) {
                for (Uuid objectUuid:sfEntry.getValue().getRequestedObjectIdentifier()) {
                    this.topicManager.deleteTapiTopic(objectUuid.getValue());
                }
            }
            return RpcResultBuilder
                .success(new DeleteNotificationSubscriptionServiceOutputBuilder().build())
                .buildFuture();
        } catch (InterruptedException | ExecutionException | NoSuchElementException e) {
            LOG.error("Failed to delete Notification subscription service", e);
        }
        return RpcResultBuilder.<DeleteNotificationSubscriptionServiceOutput>failed()
            .withError(ErrorType.APPLICATION, "Failed to delete notification subscription service")
            .buildFuture();
    }
}
