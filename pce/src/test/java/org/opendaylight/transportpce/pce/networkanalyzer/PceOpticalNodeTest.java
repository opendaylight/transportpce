/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;


public class PceOpticalNodeTest extends AbstractTest {

    private PceOpticalNode pceOpticalNode;

    @Before
    public void setUp() {
        List<SupportingNode> supportingNodes = new ArrayList<>();
        SupportingNode supportingNodeTest = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("supporting_node_test"))
                .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                .build();
        SupportingNode clliNodeTest = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("clli_node_test"))
                .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                .build();
        supportingNodes.add(supportingNodeTest);
        supportingNodes.add(clliNodeTest);
        NodeId nodeId = new NodeId("node_test");
        Node node = new NodeBuilder()
                .setNodeId(nodeId)
                .setSupportingNode(supportingNodes)
                .build();
        OpenroadmNodeType nodeType = OpenroadmNodeType.ROADM;
        ServiceFormat serviceFormat = ServiceFormat.Ethernet;
        String pceNodeType = "pceNodeType";
        pceOpticalNode = new PceOpticalNode(node, nodeType, nodeId, serviceFormat, pceNodeType);
    }

    @Test
    public void isValidTest() {
        Assert.assertTrue(pceOpticalNode.isValid());
    }

}
