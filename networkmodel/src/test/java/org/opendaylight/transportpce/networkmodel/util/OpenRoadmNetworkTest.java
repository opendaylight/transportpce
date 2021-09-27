/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;

public class OpenRoadmNetworkTest {

    @Test
    public void createXpdrNodeTest() {
        NodeInfo nodeInfo = computeNodeInfo(NodeTypes.Xpdr, "nodeA");
        Node createdNode = OpenRoadmNetwork.createNode("XPDRA01", nodeInfo);
        assertEquals("XPDRA01", createdNode.getNodeId().getValue());
        assertEquals("XPONDER", createdNode.augmentation(Node1.class).getNodeType().getName());

        supportingNodeTest(nodeInfo.getNodeClli(), createdNode);
    }

    @Test
    public void createRdmNodeTest() {
        NodeInfo nodeInfo = computeNodeInfo(NodeTypes.Rdm, "nodeA");
        Node createdNode = OpenRoadmNetwork.createNode("XPDRA01", nodeInfo);
        assertEquals("XPDRA01", createdNode.getNodeId().getValue());
        assertEquals("ROADM", createdNode.augmentation(Node1.class).getNodeType().getName());

        supportingNodeTest(nodeInfo.getNodeClli(), createdNode);
    }

    @Test
    public void createNodeWithBadNodeTypeTest() {
        NodeInfo nodeInfo = computeNodeInfo(NodeTypes.Ila, "nodeA");
        Node createdNode = OpenRoadmNetwork.createNode("XPDRA01", nodeInfo);
        assertEquals("XPDRA01", createdNode.getNodeId().getValue());
        assertNull("NodeType should be ROADM or XPONDER", createdNode.augmentation(Node1.class).getNodeType());

        supportingNodeTest(nodeInfo.getNodeClli(), createdNode);
    }

    @Ignore
    @Test
    public void createNodeWithoutClliTest() {
        NodeInfo nodeInfo = computeNodeInfo(NodeTypes.Xpdr, null);
        Node createdNode = OpenRoadmNetwork.createNode("XPDRA01", nodeInfo);
        assertEquals("XPDRA01", createdNode.getNodeId().getValue());
        assertEquals("XPONDER", createdNode.augmentation(Node1.class).getNodeType().getName());
        assertEquals(0, createdNode.getSupportingNode().size());
    }

    private NodeInfo computeNodeInfo(NodeTypes nodeType, String clli) {
        return new NodeInfoBuilder()
            .setNodeModel("NodeModel")
            .setNodeIpAddress(new IpAddress(new Ipv4Address("127.0.0.1")))
            .setNodeType(nodeType)
            .setNodeClli(clli)
            .setNodeVendor("VendorA")
            .setOpenroadmVersion(OpenroadmNodeVersion._121)
            .build();
    }

    private SupportingNode computeSupportingNode(String clli) {
        return new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
            .setNodeRef(new NodeId(clli))
            .build();
    }

    private void supportingNodeTest(String clli, Node createdNode) {
        SupportingNode supportingNode = computeSupportingNode(clli);
        assertEquals(1, createdNode.getSupportingNode().size());
        List<SupportingNode> supportingNodeList = new ArrayList<>(createdNode.nonnullSupportingNode().values());
        assertEquals(supportingNode, supportingNodeList.get(0));
    }
}