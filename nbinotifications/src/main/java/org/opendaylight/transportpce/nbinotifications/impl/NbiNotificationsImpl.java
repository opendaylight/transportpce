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
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceDeserializer;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceDeserializer;
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
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsImpl implements NbiNotificationsService {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsImpl.class);
    private final JsonStringConverter<NotificationProcessService> converterService;
    private final JsonStringConverter<NotificationAlarmService> converterAlarmService;
    private final String server;

    public NbiNotificationsImpl(JsonStringConverter<NotificationProcessService> converterService,
                                JsonStringConverter<NotificationAlarmService> converterAlarmService, String server) {
        this.converterService = converterService;
        this.converterAlarmService = converterAlarmService;
        this.server = server;
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
}
