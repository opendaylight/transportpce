/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.mdsal.common.api.CommitInfo.emptyFluentFuture;

import com.google.common.util.concurrent.Futures;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.listeners.TapiNetworkModelNotificationHandler;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelService;

@ExtendWith(MockitoExtension.class)
public class TapiProviderTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private RpcProviderService rpcProviderService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RpcService rpcService;
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private NetworkTransactionService networkTransactionService;
    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperations;
    @Mock
    private TapiNetworkModelNotificationHandler tapiNetworkModelNotificationHandler;
    @Mock
    private TapiNetworkModelService tapiNetworkModelServiceImpl;

    @Test
    void testInitRegisterTapiToRpcRegistry() {
        when(networkTransactionService.read(any(), any())).thenReturn(Futures.immediateFuture(Optional.empty()));
        doReturn(emptyFluentFuture()).when(networkTransactionService).commit();
        new TapiProvider(dataBroker, rpcProviderService, rpcService, notificationService, notificationPublishService,
                networkTransactionService, serviceDataStoreOperations,
                tapiNetworkModelNotificationHandler, tapiNetworkModelServiceImpl);

        verify(rpcProviderService, times(2)).registerRpcImplementations(any());
        verify(dataBroker, times(4)).registerTreeChangeListener(any(), any());
    }
}
