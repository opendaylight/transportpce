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
import org.opendaylight.transportpce.nbinotifications.serialization.TapiNotificationDeserializer;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsAlarmServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsAlarmServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsAlarmServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsProcessServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsProcessServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsProcessServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsTapiServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsTapiServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.GetNotificationsTapiServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.NbiNotificationsService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.NotificationTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.get.notifications.alarm.service.output.NotificationsAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.get.notifications.process.service.output.NotificationsProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211021.get.notifications.tapi.service.output.NotificationsTapiService;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NbiNotificationsImpl implements NbiNotificationsService {
    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsImpl.class);
    private final JsonStringConverter<NotificationProcessService> converterService;
    private final JsonStringConverter<NotificationAlarmService> converterAlarmService;
    private final JsonStringConverter<NotificationTapiService> converterTapiService;
    private final String server;
    private final TopicManager topicManager;

    public NbiNotificationsImpl(JsonStringConverter<NotificationProcessService> converterService,
                                JsonStringConverter<NotificationAlarmService> converterAlarmService,
                                JsonStringConverter<NotificationTapiService> converterTapiService, String server,
                                TopicManager topicManager) {
        this.converterService = converterService;
        this.converterAlarmService = converterAlarmService;
        this.server = server;
        this.topicManager = topicManager;
        this.converterTapiService = converterTapiService;
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
    public ListenableFuture<RpcResult<GetNotificationsTapiServiceOutput>> getNotificationsTapiService(
            GetNotificationsTapiServiceInput input) {
        LOG.info("RPC getNotificationsAlarmService received");
        if (input == null || input.getSubscriptionIdOrName() == null) {
            LOG.warn("Missing mandatory params for input {}", input);
            return RpcResultBuilder.<GetNotificationsTapiServiceOutput>failed().withError(RpcError.ErrorType.RPC,
                "Null input parameters").buildFuture();
        }
        // TODO: SubscriptionIdOrName must be a string of type UUID
        if (!this.topicManager.getTapiTopicMap().containsKey(input.getSubscriptionIdOrName())) {
            LOG.warn("Missing mandatory params for input {}", input);
            return RpcResultBuilder.<GetNotificationsTapiServiceOutput>failed()
                .withError(RpcError.ErrorType.APPLICATION, "Topic doesnt exist").buildFuture();
        }
        Subscriber<NotificationTapiService, NotificationsTapiService> subscriber = new Subscriber<>(
            input.getSubscriptionIdOrName(), input.getSubscriptionIdOrName(), server, converterTapiService,
            TapiNotificationDeserializer.class);
        List<NotificationsTapiService> notificationsTapiServiceList = subscriber
            .subscribe(input.getSubscriptionIdOrName(), NotificationsTapiService.QNAME);
        return RpcResultBuilder.success(new GetNotificationsTapiServiceOutputBuilder()
            .setNotificationsTapiService(notificationsTapiServiceList).build()).buildFuture();
    }
}
