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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.TransportpceTapinetworkutilsService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TapiCommonService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.TapiNotificationListener;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;

@ExtendWith(MockitoExtension.class)
public class TapiProviderTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private RpcProviderService rpcProviderRegistry;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private NetworkTransactionService networkTransactionService;
    @Mock
    private OrgOpenroadmServiceService serviceHandler;
    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperations;
    @Mock
    private TransportpceTapinetworkutilsService tapiNetworkUtils;
    @Mock
    private TapiNotificationListener tapiNetworkModelListenerImpl;
    @Mock
    private TapiNetworkModelService tapiNetworkModelServiceImpl;

    @Test
    void testInitRegisterTapiToRpcRegistry() {
        new TapiProvider(dataBroker, rpcProviderRegistry, notificationService, notificationPublishService,
                networkTransactionService, serviceHandler, serviceDataStoreOperations, tapiNetworkUtils,
                tapiNetworkModelListenerImpl, tapiNetworkModelServiceImpl);

        verify(rpcProviderRegistry, times(1)).registerRpcImplementation(any(), any(TapiConnectivityService.class));
        verify(rpcProviderRegistry, times(2)).registerRpcImplementation(any(), any(TapiTopologyService.class));
        verify(rpcProviderRegistry, times(2)).registerRpcImplementation(any(), any(TapiCommonService.class));
        verify(dataBroker, times(4)).registerDataTreeChangeListener(any(), any());
    }
}