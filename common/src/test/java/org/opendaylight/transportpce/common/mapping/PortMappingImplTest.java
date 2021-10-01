/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.nodes.NodeInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PortMappingImplTest {

    DataBroker dataBroker = null;
    private PortMappingVersion710 portMappingVersion710;
    private PortMappingVersion221 portMappingVersion221;
    private PortMappingVersion121 portMappingVersion121;
    private PortMapping portMapping;

    @Before
    public void setUp() throws Exception {
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        dataBroker = dataStoreContext.getDataBroker();
        portMappingVersion710 = mock(PortMappingVersion710.class);
        portMappingVersion221 = mock(PortMappingVersion221.class);
        portMappingVersion121 = mock(PortMappingVersion121.class);
        portMapping = new PortMappingImpl(dataBroker, portMappingVersion710,
            portMappingVersion221, portMappingVersion121);
    }

    @Test
    public void createMappingDataTest() {
        //test create mapping version 1
        when(portMappingVersion121.createMappingData("node")).thenReturn(true);
        assertTrue(portMapping.createMappingData("node", OPENROADM_DEVICE_VERSION_1_2_1));

        //test create mapping version 2
        when(portMappingVersion221.createMappingData("node")).thenReturn(true);
        assertTrue(portMapping.createMappingData("node", OPENROADM_DEVICE_VERSION_2_2_1));

        //test create mapping version with wrong value
        assertFalse(portMapping.createMappingData("node", "test"));
    }


    @Test
    public void updateMappingTest() throws ExecutionException, InterruptedException {
        Mapping mapping = new MappingBuilder().setLogicalConnectionPoint("logicalConnectionPoint")
                .setPortDirection("1").setConnectionMapLcp("1").setPartnerLcp("1")
                .setPortQual("1").setSupportingCircuitPackName("1").setSupportingOms("1")
                .setSupportingOts("1").setSupportingPort("1").build();
        InstanceIdentifier<Mapping> portMappingIID = InstanceIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey("node"))
                .child(Mapping.class, new MappingKey("logicalConnectionPoint"))
                .build();
        InstanceIdentifier<NodeInfo> nodeInfoIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node")).child(NodeInfo.class).build();
        final NodeInfo nodeInfo = new NodeInfoBuilder().setOpenroadmVersion(OpenroadmNodeVersion._221).build();
        final NodeInfo nodeInfo2 = new NodeInfoBuilder().setOpenroadmVersion(OpenroadmNodeVersion._121).build();
        Nodes nodes = new NodesBuilder().setNodeId("node").setNodeInfo(nodeInfo).build();
        InstanceIdentifier<Nodes> nodeIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node")).build();
        //create node with portmapping and nodeifno version 2
        WriteTransaction wr = dataBroker.newWriteOnlyTransaction();
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeIID, nodes);
        wr.merge(LogicalDatastoreType.CONFIGURATION, portMappingIID, mapping);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID, nodeInfo);
        wr.commit().get();
        //test update port mapping version 2
        when(portMappingVersion221.updateMapping("node", mapping)).thenReturn(true);
        assertTrue("Update sould be ok", portMapping.updateMapping("node", mapping));

        //replace node nodefino version 1 instead of version 2
        WriteTransaction wr2 = dataBroker.newWriteOnlyTransaction();
        wr2.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID, nodeInfo2);
        wr2.commit().get();

        //test update portmapping version 1
        when(portMappingVersion121.updateMapping("node", mapping)).thenReturn(true);
        assertTrue(portMapping.updateMapping("node", mapping));

        //test get node that exists
        assertNotNull(portMapping.getNode("node"));

        //test get node that doesn't exist
        assertNull(portMapping.getNode("node2"));

        //test get portmapping for existing node
        assertEquals(portMapping
                .getMapping("node", "logicalConnectionPoint"), mapping);

        //test delete portmapping for existing node
        portMapping.deletePortMappingNode("node");

        //test get portmapping that was deleted above and doesn't exist anymore
        assertNull(portMapping.getMapping("node", "logicalConnectionPoint"));

    }

}
