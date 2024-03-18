/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.CreateNotificationSubscriptionServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.DeleteNotificationSubscriptionServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationListImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationSubscriptionServiceDetailsImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationSubscriptionServiceListImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationsAlarmServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetNotificationsProcessServiceImpl;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.GetSupportedNotificationTypesImpl;
import org.opendaylight.transportpce.test.AbstractTest;


public class NbiNotificationsProviderTest  extends AbstractTest {
    public static NetworkTransactionService networkTransactionService;

    @Mock
    RpcProviderService rpcProviderRegistry;
    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void initTest() {
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        new NbiNotificationsProvider("localhost:8080", "localhost:8080",
                rpcProviderRegistry, notificationService, getDataStoreContextUtil().getBindingDOMCodecServices(),
                networkTransactionService);
        verify(rpcProviderRegistry, times(1)).registerRpcImplementations(
                any(GetNotificationsProcessServiceImpl.class),
                any(GetNotificationsAlarmServiceImpl.class),
                any(GetSupportedNotificationTypesImpl.class),
                any(CreateNotificationSubscriptionServiceImpl.class),
                any(DeleteNotificationSubscriptionServiceImpl.class),
                any(GetNotificationSubscriptionServiceDetailsImpl.class),
                any(GetNotificationSubscriptionServiceListImpl.class),
                any(GetNotificationListImpl.class));
        verify(notificationService, times(1))
                .registerCompositeListener(any(NotificationService.CompositeListener.class));
    }
}
