/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.DataStoreContextImpl;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.nodes.NodeInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


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
        Nodes nodes = new NodesBuilder().setNodeId("node3").build();
        InstanceIdentifier<NodeInfo> nodeInfoIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node")).child(NodeInfo.class).build();
        InstanceIdentifier<NodeInfo> nodeInfoIID2 = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node2")).child(NodeInfo.class).build();
        InstanceIdentifier<Nodes> nodeIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node3")).build();
        WriteTransaction wr = dataBroker.newWriteOnlyTransaction();

        //Create a node version 1, a node version 2, and a node no version
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID, nodeInfo, true);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID2, nodeInfo2, true);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeIID, nodes, true);
        wr.commit().get();
        //Test the versions are returned OK
        assertEquals("They have the same openroadmVersion",
                mappingUtils.getOpenRoadmVersion("node"), StringConstants.OPENROADM_DEVICE_VERSION_1_2_1);
        assertEquals("They have the same openroadmVersion",
                mappingUtils.getOpenRoadmVersion("node2"), StringConstants.OPENROADM_DEVICE_VERSION_2_2_1);
        assertNull("node3 isn't exists", mappingUtils.getOpenRoadmVersion("node3"));
        assertNotNull("node is existed", mappingUtils.getOpenRoadmVersion("node"));
        assertNotNull("node2 is existed", mappingUtils.getOpenRoadmVersion("node2"));
    }


}
