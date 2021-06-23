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

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;

public class ClliNetworkTest {

    @Test
    public void createNodeTest() {
        //prepare data test
        NodeInfo nodeInfo = computeNodeInfoBuilder().build();
        Node1 clliAugmentation = new Node1Builder()
                .setClli(nodeInfo .getNodeClli())
                .build();

        //run test
        Node createdNode = ClliNetwork.createNode("XPDRA01", nodeInfo);
        assertNull("SupportingNode should be null", createdNode.getSupportingNode());
        assertEquals("Node key should be equals to NodeClliId ", new NodeKey(new NodeId("NodeClliId")),
            createdNode.key());
        assertEquals("Expect org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.Node1 augmentation",
            clliAugmentation, createdNode.augmentation(Node1.class));
    }

    @Test
    public void createNodeWithNullDeviceIdTest() {
        Node createdNode = ClliNetwork.createNode(null, computeNodeInfoBuilder().build());
        assertNull("SupportingNode should be null", createdNode.getSupportingNode());
        assertEquals("Node id should be equals to NodeClliId ", new NodeId("NodeClliId"), createdNode.getNodeId());
    }

    private NodeInfoBuilder computeNodeInfoBuilder() {
        NodeInfoBuilder nodeBldr = new NodeInfoBuilder()
            .setNodeClli("NodeClliId")
            .setNodeModel("NodeModel")
            .setNodeIpAddress(new IpAddress(new Ipv4Address("127.0.0.1")))
            .setNodeType(NodeTypes.forValue(NodeTypes.Xpdr.getIntValue()))
            .setNodeVendor("VendorA")
            .setOpenroadmVersion(OpenroadmNodeVersion._121);
        return nodeBldr;
    }

    private NodeInfoBuilder computeNodeInfoBuilderWithoutClli() {
        NodeInfoBuilder nodeBldr = new NodeInfoBuilder()
            .setNodeModel("NodeModel")
            .setNodeIpAddress(new IpAddress(new Ipv4Address("127.0.0.1")))
            .setNodeType(NodeTypes.forValue(NodeTypes.Xpdr.getIntValue()))
            .setNodeVendor("VendorA")
            .setOpenroadmVersion(OpenroadmNodeVersion._121);
        return nodeBldr;
    }
}
