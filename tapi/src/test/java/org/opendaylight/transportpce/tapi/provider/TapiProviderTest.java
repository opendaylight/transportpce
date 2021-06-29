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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.listeners.TapiPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceHandlerListenerImpl;
import org.opendaylight.transportpce.tapi.topology.TapiNetconfTopologyListener;
import org.opendaylight.transportpce.tapi.topology.TapiPortMappingListener;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.TransportpceTapinetworkutilsService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TapiCommonService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.TapiTopologyService;

public class TapiProviderTest extends AbstractTest {
    public static NetworkTransactionService networkTransactionService;

    @Mock
    RpcProviderService rpcProviderRegistry;

    @Mock
    OrgOpenroadmServiceService serviceHandler;

    @Mock
    ServiceDataStoreOperations serviceDataStoreOperations;

    @Mock
    TapiListener tapiListener;

    @Mock
    TransportpceTapinetworkutilsService tapiNetworkUtils;

    @Mock
    TapiPortMappingListener tapiPortMappingListener;

    @Mock
    TapiNetconfTopologyListener topologyListener;

    @Mock
    TapiPceListenerImpl pceListenerImpl;

    @Mock
    TapiRendererListenerImpl rendererListenerImpl;

    @Mock
    TapiServiceHandlerListenerImpl serviceHandlerListenerImpl;

    private AutoCloseable closeable;

    @Before
    public void openMocks() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @BeforeClass
    public static void setUp() {
        networkTransactionService = new NetworkTransactionImpl(
            new RequestProcessor(getDataBroker()));
    }

    @Test
    public void testInitRegisterTapiToRpcRegistry() {
        TapiProvider provider =  new TapiProvider(getDataBroker(), rpcProviderRegistry, serviceHandler,
            serviceDataStoreOperations, tapiListener, networkTransactionService, topologyListener,
            tapiPortMappingListener, tapiNetworkUtils, pceListenerImpl, rendererListenerImpl,
            serviceHandlerListenerImpl, getNotificationService());

        provider.init();

        verify(rpcProviderRegistry, times(1))
            .registerRpcImplementation(any(), any(TapiConnectivityService.class));
        verify(rpcProviderRegistry, times(2))
            .registerRpcImplementation(any(), any(TapiTopologyService.class));
        verify(rpcProviderRegistry, times(2))
            .registerRpcImplementation(any(), any(TapiCommonService.class));
    }

    @After
    public void releaseMocks() throws Exception {
        closeable.close();
    }
}
