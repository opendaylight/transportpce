/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import static org.junit.Assert.assertNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsAlarmServiceInputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsAlarmServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsProcessServiceInputBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.GetNotificationsProcessServiceOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationProcessService;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class NbiNotificationsImplTest extends AbstractTest {
    private NbiNotificationsImpl nbiNotificationsImpl;

    @Before
    public void setUp() {
        JsonStringConverter<NotificationProcessService> converter = new JsonStringConverter<>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
        JsonStringConverter<NotificationAlarmService> converterAlarm = new JsonStringConverter<>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
        nbiNotificationsImpl = new NbiNotificationsImpl(converter, converterAlarm,"localhost:8080");
    }

    @Test
    public void getNotificationsServiceEmptyDataTest() throws InterruptedException, ExecutionException {
        ListenableFuture<RpcResult<GetNotificationsProcessServiceOutput>> result =
                nbiNotificationsImpl.getNotificationsProcessService(
                        new GetNotificationsProcessServiceInputBuilder().build());
        assertNull("Should be null", result.get().getResult().getNotificationsProcessService());
    }

    @Test
    public void getNotificationsServiceTest() throws InterruptedException, ExecutionException {
        GetNotificationsProcessServiceInputBuilder builder = new GetNotificationsProcessServiceInputBuilder()
                .setGroupId("groupId")
                .setIdConsumer("consumerId")
                .setConnectionType(ConnectionType.Service);
        ListenableFuture<RpcResult<GetNotificationsProcessServiceOutput>> result =
                nbiNotificationsImpl.getNotificationsProcessService(builder.build());
        assertNull("Should be null", result.get().getResult().getNotificationsProcessService());
    }

    @Test
    public void getNotificationsAlarmServiceTest() throws InterruptedException, ExecutionException {
        GetNotificationsAlarmServiceInputBuilder builder = new GetNotificationsAlarmServiceInputBuilder()
                .setGroupId("groupId")
                .setIdConsumer("consumerId")
                .setConnectionType(ConnectionType.Service);
        ListenableFuture<RpcResult<GetNotificationsAlarmServiceOutput>> result =
                nbiNotificationsImpl.getNotificationsAlarmService(builder.build());
        assertNull("Should be null", result.get().getResult().getNotificationsAlarmService());
    }
}
