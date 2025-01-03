/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.impl.rpc.CreateConnectivityServiceImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.DeleteConnectivityServiceImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectionDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectivityServiceDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectivityServiceListImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetLinkDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetNodeDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetNodeEdgePointDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetServiceInterfacePointDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetServiceInterfacePointListImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetTopologyDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetTopologyListImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiNetworkModelNotificationHandler;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelService;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;

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
    private NetworkModelService networkModelService;
    @Mock
    private TapiNetworkModelNotificationHandler tapiNetworkModelNotificationHandler;
    @Mock
    private TapiNetworkModelService tapiNetworkModelServiceImpl;
    @Mock
    private TapiLink tapilink;
    @Mock
    private TapiContext tapiContext;

    @Test
    void testInitRegisterTapiToRpcRegistry() {
        when(networkTransactionService.read(any(), any())).thenReturn(Futures.immediateFuture(Optional.empty()));
//        doReturn(emptyFluentFuture()).when(networkTransactionService).commit();
        new TapiProvider(dataBroker, rpcProviderService, rpcService, notificationService, notificationPublishService,
                networkTransactionService, serviceDataStoreOperations, networkModelService,
                tapiNetworkModelNotificationHandler, tapiNetworkModelServiceImpl, tapilink, tapiContext);

        verify(rpcProviderService, times(1)).registerRpcImplementations(
                any(CreateConnectivityServiceImpl.class),
                any(GetConnectivityServiceDetailsImpl.class),
                any(GetConnectionDetailsImpl.class),
                any(DeleteConnectivityServiceImpl.class),
                any(GetConnectivityServiceListImpl.class),
                any(GetNodeDetailsImpl.class),
                any(GetTopologyDetailsImpl.class),
                any(GetNodeEdgePointDetailsImpl.class),
                any(GetLinkDetailsImpl.class),
                any(GetTopologyListImpl.class),
                any(GetServiceInterfacePointDetailsImpl.class),
                any(GetServiceInterfacePointListImpl.class));
        verify(dataBroker, times(4)).registerTreeChangeListener(any(), any(), any());
    }
}
