/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;


import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.gnpy.JerseyServer;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumer;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumerImpl;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;

@RunWith(MockitoJUnitRunner.class)
public class PceSendingPceRPCsTest extends AbstractTest {

    private PceSendingPceRPCs pceSendingPceRPCs;
    private NetworkTransactionImpl networkTransaction;
    private Mapping mapping;
    @Mock
    private BindingDOMCodecServices bindingDOMCodecServices;
    private JerseyServer jerseyServer = new JerseyServer();
    private DataBroker dataBroker;
    private GnpyConsumer gnpyConsumer;
    @Mock
    private PortMapping portMapping;


    @Before
    public void setUp() {
        this.dataBroker = getNewDataBroker();
        networkTransaction = new NetworkTransactionImpl(new RequestProcessor(this.dataBroker));
        PceTestUtils.writeNetworkInDataStore(this.dataBroker);
        gnpyConsumer = new GnpyConsumerImpl("http://localhost:9998",
                "mylogin", "mypassword", getDataStoreContextUtil().getBindingDOMCodecServices());
        pceSendingPceRPCs = new PceSendingPceRPCs(PceTestData.getPCE_test1_request_54(),
                        networkTransaction, gnpyConsumer, portMapping);
        mapping = new MappingBuilder().setLogicalConnectionPoint("logicalConnectionPoint").setPortQual("xpdr-client")
            .build();
        NodeInfo info = new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr).build();
        Nodes node = new NodesBuilder().withKey(new NodesKey("node")).setNodeId("node").setNodeInfo(info).build();
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(mapping);
        when(portMapping.getNode(anyString())).thenReturn(node);
    }

    @Test
    public void cancelResourceReserve() {
        pceSendingPceRPCs.cancelResourceReserve();
        Assert.assertTrue("Success should equal to true", pceSendingPceRPCs.getSuccess());
    }

    @Test
    public void pathComputationTest() throws Exception {
        jerseyServer.setUp();
        pceSendingPceRPCs =
                new PceSendingPceRPCs(PceTestData.getGnpyPCERequest("XPONDER-1", "XPONDER-2"),
                        networkTransaction, gnpyConsumer, portMapping);
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(mapping);
        pceSendingPceRPCs.pathComputation();
        Assert.assertTrue(gnpyConsumer.isAvailable());
        jerseyServer.tearDown();

    }

    @Test
    public void checkMessage() {
        Assert.assertNull(pceSendingPceRPCs.getMessage());
    }

    @Test
    public void responseCodeTest() {
        Assert.assertNull(pceSendingPceRPCs.getResponseCode());
    }

    @Test
    public void gnpyAtoZ() {
        Assert.assertNull(pceSendingPceRPCs.getGnpyAtoZ());
    }

    @Test
    public void getGnpyZtoA() {
        Assert.assertNull(pceSendingPceRPCs.getGnpyZtoA());
    }


}
