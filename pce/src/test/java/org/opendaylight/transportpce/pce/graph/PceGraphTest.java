/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.utils.NodeUtils;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;


public class PceGraphTest {

    private Link link = null;
    private Node node = null;
    private PceLink pceLink = null;
    private PceGraph pceGraph = null;
    private PathComputationRequestInput requestInput = PceTestData.getPCE_test2_request_54();
    private PceConstraints pceHardConstraints = null;
    private PceResult rc = null;
    private PceOpticalNode pceOpticalNode = null;
    private PceOpticalNode pceOpticalNode2 = null;
    private Map<NodeId, PceNode> allPceNodes = null;

    @Before
    public void setUp() {
        // Build Link
        link = NodeUtils.createRoadmToRoadm("OpenROADM-3-2-DEG1",
                "OpenROADM-3-1-DEG1",
                "DEG1-TTP-TX", "DEG1-TTP-RX").build();

        node =  NodeUtils.getNodeBuilder(NodeUtils.geSupportingNodes()).build();

        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("OpenROADM-3-2-DEG1"), ServiceFormat.Ethernet,
                "DEGREE");

        pceOpticalNode2 = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("OpenROADM-3-1-DEG1"), ServiceFormat.Ethernet,
                "DEGREE");

        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode2);
        pceLink.setClient("XPONDER-CLIENT");

        pceLink.getDestId();
        pceOpticalNode.addOutgoingLink(pceLink);

        // init PceHardContraints
        pceHardConstraints = new PceConstraints();
        // pceHardConstraints.setp
        allPceNodes = Map.of(new NodeId("OpenROADM-3-2-DEG1"), pceOpticalNode,
                new NodeId("OpenROADM-3-1-DEG1"), pceOpticalNode2);
        rc = new PceResult();
        pceGraph = new PceGraph(pceOpticalNode, pceOpticalNode2, allPceNodes,
                pceHardConstraints,
                null, rc,
                "ODU4");
    }

    @Test
    public void clacPath() {

        Assert.assertEquals(pceGraph.calcPath(), true);
    }

    @Test
    public void clacPathFalse() {
        pceGraph = new PceGraph(pceOpticalNode, pceOpticalNode2, allPceNodes,
                pceHardConstraints,
                null, rc,
                "100GE");

        Assert.assertEquals(pceGraph.calcPath(), false);
    }

    @Test(expected = Exception.class)
    public void clacPath1GE() {
        pceGraph = new PceGraph(pceOpticalNode, pceOpticalNode2, allPceNodes,
                pceHardConstraints,
                null, rc,
                "10GE");

        Assert.assertEquals(pceGraph.calcPath(), false);
    }
}

