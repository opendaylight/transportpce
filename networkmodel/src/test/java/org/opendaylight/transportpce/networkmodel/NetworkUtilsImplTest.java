/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.DataStoreContextImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.networkmodel.util.TpceNetwork;
//import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRdmXpdrLinksInput;
//import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818
// .InitRdmXpdrLinksInputBuilder;
//import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRdmXpdrLinksOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.DeleteLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.DeleteLinkInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.DeleteLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRoadmNodesInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRoadmNodesInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRoadmNodesOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.TransportpceNetworkutilsService;
//import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.links.input.grouping
// .LinksInput;
//import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.links.input.grouping
// .LinksInputBuilder;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkUtilsImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtilsImpl.class);

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void deleteLinkTest() {
    }

    @Test
    public void initRoadmNodesTest() {
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

        //Initialize a new ROADM-to-ROADM link
        NetworkUtilsImpl networkUtils = new NetworkUtilsImpl(dataBroker);
        InitRoadmNodesInput input =
                new InitRoadmNodesInputBuilder().setRdmANode("node-a").setRdmZNode("node-z")
                        .setDegANum(Uint8.valueOf(1)).setDegZNum(Uint8.valueOf(1))
                        .setTerminationPointA("tp-a-TXRX").setTerminationPointZ("tp-z-TXRX").build();
        ListenableFuture<RpcResult<InitRoadmNodesOutput>> output = networkUtils.initRoadmNodes(input);
        try {
            assertTrue(output.get().isSuccessful());
            assertEquals(output.get().getResult().getResult(),
                    "Unidirectional Roadm-to-Roadm Link created successfully");
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to create Roadm 2 Roadm Link for topo layer ");
        }

        //Delete ROADM-to-ROADM link
        DeleteLinkInput deleteLinkInput = new DeleteLinkInputBuilder()
                .setLinkId("node-a-DEG1-tp-a-TXRXtonode-z-DEG1-tp-z-TXRX").build();
        ListenableFuture<RpcResult<DeleteLinkOutput>> delOutput =
                networkUtils.deleteLink(deleteLinkInput);
        try {
            assertTrue(delOutput.get().isSuccessful());
            assertEquals(delOutput.get().getResult().getResult(),"Link {} deleted successfully");
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to delete Roadm 2 Roadm Link ");
        }

        //Delete ROADM-to-ROADM link with empty LinkId
        DeleteLinkInput deleteLinkInput2 = new DeleteLinkInputBuilder()
                .setLinkId("").build();
        ListenableFuture<RpcResult<DeleteLinkOutput>> delOutput2 =
                networkUtils.deleteLink(deleteLinkInput2);
        try {
            assertTrue(delOutput2.get().isSuccessful());
            assertEquals(delOutput2.get().getResult().getResult(), "Fail");
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to delete Roadm 2 Roadm Link ");
        }
    }

    @Test
    public void initXpdrRdmLinksTest() {
    }

    @Test
    public void initRdmXpdrLinksTest() {
    }
}