/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.utils.NotificationServiceDataUtils;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsAlarmServiceInputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsAlarmServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsProcessServiceInputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.GetNotificationsProcessServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationListOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class NbiNotificationsImplTest extends AbstractTest {
    private NbiNotificationsImpl nbiNotificationsImpl;
    public static NetworkTransactionService networkTransactionService;
    private TopicManager topicManager;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        topicManager = TopicManager.getInstance();
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        JsonStringConverter<NotificationProcessService> converter = new JsonStringConverter<>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
        JsonStringConverter<NotificationAlarmService> converterAlarm = new JsonStringConverter<>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
        JsonStringConverter<NotificationTapiService> converterTapi = new JsonStringConverter<>(
            getDataStoreContextUtil().getBindingDOMCodecServices());
        topicManager.setTapiConverter(converterTapi);
        NotificationServiceDataUtils.createTapiContext(networkTransactionService);

        nbiNotificationsImpl = new NbiNotificationsImpl(converter, converterAlarm, converterTapi,
            "localhost:8080", networkTransactionService, topicManager);
    }

    @Test
    void getNotificationsServiceEmptyDataTest() throws InterruptedException, ExecutionException {
        ListenableFuture<RpcResult<GetNotificationsProcessServiceOutput>> result =
                nbiNotificationsImpl.getNotificationsProcessService(
                        new GetNotificationsProcessServiceInputBuilder().build());
        assertNull(result.get().getResult().getNotificationsProcessService(), "Should be null");
    }

    @Test
    void getNotificationsServiceTest() throws InterruptedException, ExecutionException {
        GetNotificationsProcessServiceInputBuilder builder = new GetNotificationsProcessServiceInputBuilder()
                .setGroupId("groupId")
                .setIdConsumer("consumerId")
                .setConnectionType(ConnectionType.Service);
        ListenableFuture<RpcResult<GetNotificationsProcessServiceOutput>> result =
                nbiNotificationsImpl.getNotificationsProcessService(builder.build());
        assertNull(result.get().getResult().getNotificationsProcessService(), "Should be null");
    }

    @Test
    void getNotificationsAlarmServiceTest() throws InterruptedException, ExecutionException {
        GetNotificationsAlarmServiceInputBuilder builder = new GetNotificationsAlarmServiceInputBuilder()
                .setGroupId("groupId")
                .setIdConsumer("consumerId")
                .setConnectionType(ConnectionType.Service);
        ListenableFuture<RpcResult<GetNotificationsAlarmServiceOutput>> result =
                nbiNotificationsImpl.getNotificationsAlarmService(builder.build());
        assertNull(result.get().getResult().getNotificationsAlarmService(), "Should be null");
    }

    @Test
    void createTapiNotificationSubscriptionServiceTest() throws InterruptedException, ExecutionException {
        CreateNotificationSubscriptionServiceInputBuilder builder
            = NotificationServiceDataUtils.buildNotificationSubscriptionServiceInputBuilder();
        ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> result =
            nbiNotificationsImpl.createNotificationSubscriptionService(builder.build());
        assertNotNull(result.get().getResult().getSubscriptionService().getUuid().toString(),
            "Should receive UUID for subscription service");
    }

    @Test
    void getTapiNotificationsServiceTest() throws InterruptedException, ExecutionException {
        CreateNotificationSubscriptionServiceInputBuilder builder
            = NotificationServiceDataUtils.buildNotificationSubscriptionServiceInputBuilder();
        ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> result =
            nbiNotificationsImpl.createNotificationSubscriptionService(builder.build());
        GetNotificationListInputBuilder builder1 = new GetNotificationListInputBuilder()
            .setTimeRange(null)
            .setSubscriptionId(result.get().getResult().getSubscriptionService().getUuid());
        ListenableFuture<RpcResult<GetNotificationListOutput>> result1 =
            nbiNotificationsImpl.getNotificationList(builder1.build());
        assertNull(result1.get().getResult().getNotification(), "Should be null");
    }
}
