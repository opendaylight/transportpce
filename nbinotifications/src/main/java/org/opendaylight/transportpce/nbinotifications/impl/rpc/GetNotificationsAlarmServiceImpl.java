/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.consumer.Subscriber;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceDeserializer;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsAlarmServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsAlarmServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsAlarmServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.alarm.service.output.NotificationsAlarmService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetNotificationsAlarmServiceImpl implements GetNotificationsAlarmService {
    private static final Logger LOG = LoggerFactory.getLogger(GetNotificationsAlarmServiceImpl.class);

    private final JsonStringConverter<NotificationAlarmService> converterAlarmService;
    private final String server;

    public GetNotificationsAlarmServiceImpl(JsonStringConverter<NotificationAlarmService> converterAlarmService,
            String server) {
        this.converterAlarmService = converterAlarmService;
        this.server = server;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationsAlarmServiceOutput>> invoke(
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
