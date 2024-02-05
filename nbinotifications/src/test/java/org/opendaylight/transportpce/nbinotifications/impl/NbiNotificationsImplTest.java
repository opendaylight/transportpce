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
import org.opendaylight.transportpce.nbinotifications.impl.rpc.CreateNotificationSubscriptionServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationListImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationsAlarmServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationsProcessServiceImpl;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationListOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class NbiNotificationsImplTest extends AbstractTest {
    private NbiNotificationsImpl nbiNotificationsImpl;
    public static NetworkTransactionService networkTransactionService;
    private TopicManager topicManager;
    private JsonStringConverter<NotificationProcessService> converterProcess;
    private JsonStringConverter<NotificationAlarmService> converterAlarm;
    private JsonStringConverter<NotificationTapiService> converterTapi;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        topicManager = TopicManager.getInstance();
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        converterProcess = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        converterAlarm = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        converterTapi = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        topicManager.setTapiConverter(converterTapi);
        NotificationServiceDataUtils.createTapiContext(networkTransactionService);

        nbiNotificationsImpl = new NbiNotificationsImpl(converterProcess, converterAlarm, converterTapi,
            "localhost:8080", networkTransactionService, topicManager);
    }

    @Test
    void getNotificationsServiceEmptyDataTest() throws InterruptedException, ExecutionException {
        ListenableFuture<RpcResult<GetNotificationsProcessServiceOutput>> result =
                new GetNotificationsProcessServiceImpl(converterProcess, "localhost:8080").invoke(
                        new GetNotificationsProcessServiceInputBuilder().build());
        assertNull(result.get().getResult().getNotificationsProcessService(), "Should be null");
    }

    @Test
    void getNotificationsServiceTest() throws InterruptedException, ExecutionException {
        ListenableFuture<RpcResult<GetNotificationsProcessServiceOutput>> result =
                new GetNotificationsProcessServiceImpl(converterProcess, "localhost:8080")
            .invoke(new GetNotificationsProcessServiceInputBuilder()
                    .setGroupId("groupId")
                    .setIdConsumer("consumerId")
                    .setConnectionType(ConnectionType.Service)
                    .build());
        assertNull(result.get().getResult().getNotificationsProcessService(), "Should be null");
    }

    @Test
    void getNotificationsAlarmServiceTest() throws InterruptedException, ExecutionException {
        ListenableFuture<RpcResult<GetNotificationsAlarmServiceOutput>> result =
                new GetNotificationsAlarmServiceImpl(converterAlarm, "localhost:8080")
            .invoke(new GetNotificationsAlarmServiceInputBuilder()
                    .setGroupId("groupId")
                    .setIdConsumer("consumerId")
                    .setConnectionType(ConnectionType.Service)
                    .build());
        assertNull(result.get().getResult().getNotificationsAlarmService(), "Should be null");
    }

    @Test
    void createTapiNotificationSubscriptionServiceTest() throws InterruptedException, ExecutionException {
        ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> result =
                new CreateNotificationSubscriptionServiceImpl(nbiNotificationsImpl, topicManager)
            .invoke(NotificationServiceDataUtils.buildNotificationSubscriptionServiceInputBuilder().build());
        assertNotNull(result.get().getResult().getSubscriptionService().getUuid().toString(),
            "Should receive UUID for subscription service");
    }

    @Test
    void getTapiNotificationsServiceTest() throws InterruptedException, ExecutionException {
        ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> result =
                new CreateNotificationSubscriptionServiceImpl(nbiNotificationsImpl, topicManager)
            .invoke(NotificationServiceDataUtils.buildNotificationSubscriptionServiceInputBuilder().build());
        ListenableFuture<RpcResult<GetNotificationListOutput>> result2 =
                new GetNotificationListImpl(converterTapi, "localhost:8080", networkTransactionService, topicManager)
            .invoke(new GetNotificationListInputBuilder()
                    .setTimeRange(null)
                    .setSubscriptionId(result.get().getResult().getSubscriptionService().getUuid())
                    .build());
        assertNull(result2.get().getResult().getNotification(), "Should be null");
    }
}
