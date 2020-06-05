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
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfoBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingUtilsImplTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtilsImplTest.class);

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void getOpenRoadmVersionTest() {
        final MappingUtils mappingUtils = new MappingUtilsImpl(getDataBroker());
        final NodeInfo nodeInfo = new NodeInfoBuilder().setOpenroadmVersion(NodeInfo.OpenroadmVersion._121).build();
        final NodeInfo nodeInfo2 = new NodeInfoBuilder().setOpenroadmVersion(NodeInfo.OpenroadmVersion._221).build();
        Nodes nodes = new NodesBuilder().setNodeId("node3").build();
        InstanceIdentifier<NodeInfo> nodeInfoIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node")).child(NodeInfo.class).build();
        InstanceIdentifier<NodeInfo> nodeInfoIID2 = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node2")).child(NodeInfo.class).build();
        InstanceIdentifier<Nodes> nodeIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey("node3")).build();
        WriteTransaction wr = getDataBroker().newWriteOnlyTransaction();

        //Create a node version 1, a node version 2, and a node no version
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID, nodeInfo, true);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeInfoIID2, nodeInfo2, true);
        wr.merge(LogicalDatastoreType.CONFIGURATION, nodeIID, nodes, true);

        try {
            wr.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Failed to create nodeInfo {}", e);
        }

        //Test the versions are returned OK
        assertEquals(mappingUtils.getOpenRoadmVersion("node"), StringConstants.OPENROADM_DEVICE_VERSION_1_2_1);
        assertEquals(mappingUtils.getOpenRoadmVersion("node2"), StringConstants.OPENROADM_DEVICE_VERSION_2_2_1);
        assertNull(mappingUtils.getOpenRoadmVersion("node3"));


    }


}
