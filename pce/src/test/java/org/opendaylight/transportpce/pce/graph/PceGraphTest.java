/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOtnNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.utils.NodeUtils;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.RoutingConstraintsSp;
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

        node = NodeUtils.getNodeBuilder(NodeUtils.geSupportingNodes()).build();

        pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("OpenROADM-3-2-DEG1"), ServiceFormat.Ethernet,
                "DEGREE");
        pceOpticalNode.checkWL(1);
        pceOpticalNode.checkWL(2);

        pceOpticalNode2 = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("OpenROADM-3-1-DEG1"), ServiceFormat.Ethernet,
                "DEGREE");
        pceOpticalNode2.checkWL(1);
        pceOpticalNode2.checkWL(2);
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
    public void clacPathPropagationDelay() {
        pceHardConstraints.setPceMetrics(RoutingConstraintsSp.PceMetric.PropagationDelay);
        pceGraph.setConstrains(pceHardConstraints, null);

        Assert.assertEquals(pceGraph.calcPath(), true);
        Assert.assertEquals(Optional.ofNullable(pceGraph.getPathAtoZ().get(0).getLatency()),
                Optional.ofNullable(30.0));
        Assert.assertEquals(pceGraph.getReturnStructure().getRate(), -1);
    }

    @Test
    public void clacPath100GE() {
        pceOpticalNode.checkWL(1);
        pceGraph = new PceGraph(pceOpticalNode, pceOpticalNode2, allPceNodes,
                pceHardConstraints,
                null, rc,
                "100GE");

        Assert.assertEquals(pceGraph.calcPath(), false);
    }

    @Test(expected = Exception.class)
    public void clacPath10GE2() {
        pceGraph = getOtnPceGraph("10GE");
        Assert.assertEquals(pceGraph.calcPath(), false);
    }

    @Test(expected = Exception.class)
    public void clacPath1GE() {
        pceGraph = getOtnPceGraph("1GE");
        Assert.assertEquals(pceGraph.calcPath(), false);
    }

    private PceGraph getOtnPceGraph(String type) {
        // Build Link
        link = NodeUtils.createRoadmToRoadm("optical",
                "optical2",
                "DEG1-TTP-TX", "DEG1-TTP-RX").build();


        node = NodeUtils.getOTNNodeBuilder(NodeUtils.geSupportingNodes(), OpenroadmTpType.XPONDERNETWORK).build();

        PceOtnNode  pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OTU.getName(), "DEGREE");
        pceOtnNode.validateXponder("optical", "sl");
        pceOtnNode.validateXponder("not optical", "sl");
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();


        PceOtnNode pceOtnNode2 = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical2"), ServiceFormat.OTU.getName(), "DEGREE");
        pceOtnNode2.validateXponder("optical", "sl");
        pceOtnNode2.validateXponder("not optical", "sl");
        pceOtnNode2.initXndrTps("AZ");
        pceOtnNode2.initXndrTps("mode");
        pceOtnNode2.checkAvailableTribPort();
        pceOtnNode2.checkAvailableTribSlot();

        pceLink = new PceLink(link, pceOtnNode, pceOtnNode2);
        pceLink.setClient("XPONDER-CLIENT");

        pceLink.getDestId();
        pceOtnNode.addOutgoingLink(pceLink);

        // init PceHardContraints
        pceHardConstraints = new PceConstraints();
        // pceHardConstraints.setp
        allPceNodes = Map.of(new NodeId("optical"), pceOtnNode,
                new NodeId("optical2"), pceOtnNode2);
        rc = new PceResult();
        PceGraph otnPceGraph = new PceGraph(pceOtnNode, pceOtnNode2, allPceNodes,
                pceHardConstraints,
                null, rc,
                type);

        return otnPceGraph;
    }
}

