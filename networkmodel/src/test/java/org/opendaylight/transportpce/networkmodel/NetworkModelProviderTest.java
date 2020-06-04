/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.DataStoreContextImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.networkmodel.util.TpceNetwork;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.TransportpceNetworkutilsService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

public class NetworkModelProviderTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void initTest() {
        DataStoreContextImpl dataStoreContext = new DataStoreContextImpl();
        DataBroker dataBroker = dataStoreContext.getDataBroker();
        RequestProcessor requestProcessor = new RequestProcessor(dataBroker);
        NetworkTransactionService networkTransactionService = new NetworkTransactionImpl(requestProcessor);
        RpcProviderService rpcProviderService = mock(RpcProviderService.class);
        TransportpceNetworkutilsService networkutilsService = mock(TransportpceNetworkutilsService.class);
        NetConfTopologyListener topologyListener = mock(NetConfTopologyListener.class);
        TpceNetwork tpceNetwork = mock(TpceNetwork.class);
        ObjectRegistration<TransportpceNetworkutilsService> networkutilsServiceRpcRegistration =
                mock(ObjectRegistration.class);

        //Create a new NetworkModelProvider Object
        NetworkModelProvider networkModelProvider = new NetworkModelProvider(networkTransactionService, dataBroker,
                rpcProviderService, networkutilsService, topologyListener);

        //Init; create the toopologies, register for RPC Service and for Netconf Topology Listener
        when(rpcProviderService.registerRpcImplementation(
                TransportpceNetworkutilsService.class, networkutilsService))
                .thenReturn(networkutilsServiceRpcRegistration);
        networkModelProvider.init();

        //Destory; clean registeration of RPC Service and Netconf Topology Listener
        networkModelProvider.close();
    }

}
