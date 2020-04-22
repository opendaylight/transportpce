/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.netconf.shaded.exificient.grammars._2017.schemaforgrammars.DatatypeBasics;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.SortPortsByName;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev181130.networks.network.network.types.ClliNetwork;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PceOpticalNodeTest extends AbstractTest {

    private PceOpticalNode pceOpticalNode;

    @Before
    public void setUp() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        NodeId nodeId = new NodeId("node_test");
        List<SupportingNode> supportingNodes = new ArrayList<>();
        SupportingNode supporting_node_test = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("supporting_node_test"))
                .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                .build();
        SupportingNode clli_node_test = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("clli_node_test"))
                .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                .build();
        supportingNodes.add(supporting_node_test);
        supportingNodes.add(clli_node_test);
        nodeBuilder.setNodeId(nodeId);
        nodeBuilder.setSupportingNode(supportingNodes);
        Node node = nodeBuilder.build();
        OpenroadmNodeType nodeType = OpenroadmNodeType.ROADM;
        ServiceFormat serviceFormat = ServiceFormat.Ethernet ;
        String pceNodeType = "pceNodeType";
        pceOpticalNode = new PceOpticalNode(node, nodeType, nodeId, serviceFormat, pceNodeType);
    }

    @Test
    public void isValidTest() {
        Assert.assertTrue(pceOpticalNode.isValid());
    }

}