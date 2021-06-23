/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.consumer.Subscriber;
import org.opendaylight.transportpce.nbinotifications.consumer.SubscriberAlarm;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.GetNotificationsAlarmServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.GetNotificationsAlarmServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.GetNotificationsAlarmServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.GetNotificationsServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.GetNotificationsServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.GetNotificationsServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NbiNotificationsService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.alarm.service.output.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsImpl implements NbiNotificationsService {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsImpl.class);
    private final JsonStringConverter<org.opendaylight.yang.gen.v1
        .nbi.notifications.rev201130.NotificationService> converterService;
    private final JsonStringConverter<org.opendaylight.yang.gen.v1
            .nbi.notifications.rev201130.NotificationAlarmService> converterAlarmService;
    private final String server;

    public NbiNotificationsImpl(JsonStringConverter<org.opendaylight.yang.gen.v1
            .nbi.notifications.rev201130.NotificationService> converterService,
                                JsonStringConverter<org.opendaylight.yang.gen.v1
            .nbi.notifications.rev201130.NotificationAlarmService> converterAlarmService, String server) {
        this.converterService = converterService;
        this.converterAlarmService = converterAlarmService;
        this.server = server;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationsServiceOutput>> getNotificationsService(
            GetNotificationsServiceInput input) {
        LOG.info("RPC getNotificationsService received");
        if (input == null || input.getIdConsumer() == null || input.getGroupId() == null) {
            LOG.warn("Missing mandatory params for input {}", input);
            return RpcResultBuilder.success(new GetNotificationsServiceOutputBuilder().build()).buildFuture();
        }
        Subscriber subscriber = new Subscriber(input.getIdConsumer(), input.getGroupId(), server, converterService);
        List<NotificationService> notificationServiceList = subscriber
                .subscribeService(input.getConnectionType().getName());
        GetNotificationsServiceOutputBuilder output = new GetNotificationsServiceOutputBuilder()
                .setNotificationService(notificationServiceList);
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationsAlarmServiceOutput>> getNotificationsAlarmService(
            GetNotificationsAlarmServiceInput input) {
        LOG.info("RPC getNotificationsAlarmService received");
        if (input == null || input.getIdConsumer() == null || input.getGroupId() == null) {
            LOG.warn("Missing mandatory params for input {}", input);
            return RpcResultBuilder.success(new GetNotificationsAlarmServiceOutputBuilder().build()).buildFuture();
        }
        SubscriberAlarm subscriberAlarm = new SubscriberAlarm(input.getIdConsumer(), input.getGroupId(), server,
                converterAlarmService);
        List<NotificationAlarmService> notificationAlarmServiceList = subscriberAlarm
                .subscribeAlarm("alarm" + input.getConnectionType().getName());
        GetNotificationsAlarmServiceOutputBuilder output = new GetNotificationsAlarmServiceOutputBuilder()
                .setNotificationAlarmService(notificationAlarmServiceList);
        return RpcResultBuilder.success(output.build()).buildFuture();
    }
}
