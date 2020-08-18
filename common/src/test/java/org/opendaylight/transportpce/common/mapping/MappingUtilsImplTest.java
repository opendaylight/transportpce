/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.DataStoreContextImpl;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Ignore
public class MappingUtilsImplTest {

    private DataBroker dataBroker = null;

    @Before
    public void setUp() throws Exception {
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        dataBroker = dataStoreContext.getDataBroker();
    }

    @Test
    public void getOpenRoadmVersionTest() throws ExecutionException, InterruptedException {
        final MappingUtils mappingUtils = new MappingUtilsImpl(dataBroker);
        final NodeInfo nodeInfo = new NodeInfoBuilder().setOpenroadmVersion(NodeInfo.OpenroadmVersion._121).build();
        final NodeInfo nodeInfo2 = new NodeInfoBuilder().setOpenroadmVersion(NodeInfo.OpenroadmVersion._221).build();
        Nodes nodes = new NodesBuilder().setNodeId("nodes").setNodeInfo(nodeInfo).build();
        Nodes nodes2 = new NodesBuilder().setNodeId("nodes2").setNodeInfo(nodeInfo2).build();
        Nodes nodes3 = new NodesBuilder().setNodeId("nodes3").build();
        InstanceIdentifier<Nodes> nodeIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("nodes")).build();
        InstanceIdentifier<Nodes> nodeIID2 = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("nodes2")).build();
        InstanceIdentifier<Nodes> nodeIID3 = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("nodes3")).build();
        WriteTransaction wr = dataBroker.newWriteOnlyTransaction();

        //Create a node version 1, a node version 2, and a node no version
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeIID, nodes);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeIID2, nodes2);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeIID3, nodes3);
        wr.commit().get();
        //Test the versions are returned OK
        assertEquals("NodeInfo with nodes as id should be 1.2.1 version",
                StringConstants.OPENROADM_DEVICE_VERSION_1_2_1,
                mappingUtils.getOpenRoadmVersion("nodes"));
        assertEquals("NodeInfo with nodes as id should be 2.2.1 version",
                StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                mappingUtils.getOpenRoadmVersion("nodes2"));
        assertNull("NodeInfo with nodes3 as id should not exist", mappingUtils.getOpenRoadmVersion("nodes3"));
    }


}
