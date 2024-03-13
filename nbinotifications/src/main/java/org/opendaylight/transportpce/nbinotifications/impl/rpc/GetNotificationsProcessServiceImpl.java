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
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceDeserializer;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsProcessServiceInput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsProcessServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsProcessServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.process.service.output.NotificationsProcessService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetNotificationsProcessServiceImpl implements GetNotificationsProcessService {
    private static final Logger LOG = LoggerFactory.getLogger(GetNotificationsProcessServiceImpl.class);

    private final JsonStringConverter<NotificationProcessService> converterService;
    private final String server;

    public GetNotificationsProcessServiceImpl(JsonStringConverter<NotificationProcessService> converterService,
            String server) {
        this.converterService = converterService;
        this.server = server;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationsProcessServiceOutput>> invoke(
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
        return RpcResultBuilder
            .success(
                new GetNotificationsProcessServiceOutputBuilder()
                    .setNotificationsProcessService(notificationServiceList)
                    .build())
            .buildFuture();
    }
}
