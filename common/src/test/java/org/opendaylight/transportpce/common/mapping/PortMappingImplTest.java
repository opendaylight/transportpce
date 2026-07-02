/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_7_1;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.nodes.NodeInfoBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class PortMappingImplTest {

    DataBroker dataBroker = null;
    private PortMappingVersion710 portMappingVersion710;
    private PortMappingVersion221 portMappingVersion221;
    private PortMapping portMapping;
    private OCPortMappingVersion200 ocPortMappingVersion200;

    @BeforeEach
    void setUp() {
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        dataBroker = dataStoreContext.getDataBroker();
        portMappingVersion710 = mock(PortMappingVersion710.class);
        portMappingVersion221 = mock(PortMappingVersion221.class);
        ocPortMappingVersion200 = mock(OCPortMappingVersion200.class);
        portMapping = new PortMappingImpl(dataBroker, portMappingVersion710,
            portMappingVersion221, ocPortMappingVersion200);
    }

    @Test
    void createMappingDataTest() {

        //test create mapping version 2.2.1
        when(portMappingVersion221.createMappingData("node")).thenReturn(true);
        assertTrue(portMapping.createMappingData("node", OPENROADM_DEVICE_VERSION_2_2_1, null));

        //test create mapping version 7.1.0
        when(portMappingVersion710.createMappingData("node")).thenReturn(true);
        assertTrue(portMapping.createMappingData("node", OPENROADM_DEVICE_VERSION_7_1, null));

        //test create mapping version with wrong value
        assertFalse(portMapping.createMappingData("node", "test", null));
    }

    @Test
    void updateMappingTest() throws ExecutionException, InterruptedException {
        Mapping mapping = new MappingBuilder().setLogicalConnectionPoint("logicalConnectionPoint")
                .setPortDirection("1").setConnectionMapLcp("1").setPartnerLcp("1")
                .setPortQual("1").setSupportingCircuitPackName("1").setSupportingOms("1")
                .setSupportingOts("1").setSupportingPort("1").build();
        DataObjectIdentifier<Mapping> portMappingIID = DataObjectIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey("node"))
                .child(Mapping.class, new MappingKey("logicalConnectionPoint"))
                .build();
        DataObjectIdentifier<NodeInfo> nodeInfoIID = DataObjectIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey("node")).child(NodeInfo.class)
                .build();
        final NodeInfo nodeInfo221 = new NodeInfoBuilder().setOpenroadmVersion(OpenroadmNodeVersion._221).build();
        final NodeInfo nodeInfo710 = new NodeInfoBuilder().setOpenroadmVersion(OpenroadmNodeVersion._71).build();
        Nodes nodes = new NodesBuilder().setNodeId("node").setNodeInfo(nodeInfo221).build();
        DataObjectIdentifier<Nodes> nodeIID = DataObjectIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey("node"))
                .build();
        //create node with portmapping and nodeinfo version 2.2.1
        WriteTransaction wr = dataBroker.newWriteOnlyTransaction();
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeIID, nodes);
        wr.merge(LogicalDatastoreType.CONFIGURATION, portMappingIID, mapping);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID, nodeInfo221);
        wr.commit().get();
        //test update port mapping version 2.2.1
        when(portMappingVersion221.updateMapping("node", mapping)).thenReturn(true);
        assertTrue(portMapping.updateMapping("node", mapping), "Update sould be ok");

        //replace node nodeinfo version 7.1.0 instead of version 2.2.1
        WriteTransaction wr2 = dataBroker.newWriteOnlyTransaction();
        wr2.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID, nodeInfo710);
        wr2.commit().get();

        //test update portmapping version 7.1.0
        when(portMappingVersion710.updateMapping("node", mapping)).thenReturn(true);
        assertTrue(portMapping.updateMapping("node", mapping));

        //test get node that exists
        assertNotNull(portMapping.getNode("node"));

        //test get node that doesn't exist
        assertNull(portMapping.getNode("node2"));

        //test get portmapping for existing node
        assertEquals(portMapping.getMapping("node", "logicalConnectionPoint"), mapping);

        //test delete portmapping for existing node
        portMapping.deletePortMappingNode("node");

        //test get portmapping that was deleted above and doesn't exist anymore
        assertNull(portMapping.getMapping("node", "logicalConnectionPoint"));
    }
}
