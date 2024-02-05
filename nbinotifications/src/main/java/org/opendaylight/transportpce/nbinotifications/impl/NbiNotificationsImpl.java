/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.DeleteNotificationSubscriptionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationSubscriptionServiceList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetSupportedNotificationTypes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsImpl {
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

    public ImmutableClassToInstanceMap<Rpc<?, ?>> registerRPCs() {
        return ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(GetNotificationsProcessService.class, new GetNotificationsProcessServiceImpl(converterService, server))
            .put(GetNotificationsAlarmService.class,
                    new GetNotificationsAlarmServiceImpl(converterAlarmService, server))
            .put(GetSupportedNotificationTypes.class, new GetSupportedNotificationTypesImpl(this))
            .put(CreateNotificationSubscriptionService.class,
                    new CreateNotificationSubscriptionServiceImpl(this, topicManager))
            .put(DeleteNotificationSubscriptionService.class,
                    new DeleteNotificationSubscriptionServiceImpl(networkTransactionService, topicManager))
            .put(GetNotificationSubscriptionServiceDetails.class,
                    new GetNotificationSubscriptionServiceDetailsImpl(this))
            .put(GetNotificationSubscriptionServiceList.class, new GetNotificationSubscriptionServiceListImpl(this))
            .put(GetNotificationList.class,
                    new GetNotificationListImpl(converterTapiService, server, networkTransactionService, topicManager))
            .build();
    }

    public NotificationContext getNotificationContext() {
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
            return notificationContextOptional.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not get TAPI notification context");
        }
        return null;
    }

    public boolean updateNotificationContext(NotificationContext notificationContext1) {
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

}
