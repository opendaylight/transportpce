/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.pce.utils.NodeUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;

public class PceGraphEdgeTest {

    private Link link = null;
    private Node node = null;
    private PceLink pceLink = null;
    private PceGraphEdge pceGraphEdge = null;


    @Before
    public void setUp() {
        // Build Link
        link = NodeUtils.createRoadmToRoadm("OpenROADM-3-2-DEG1",
                "OpenROADM-3-1-DEG1",
                "DEG1-TTP-TX", "DEG1-TTP-RX").build();

        //  Link link=genereateLinkBuilder();
        node =  NodeUtils.getNodeBuilder(NodeUtils.geSupportingNodes()).build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("OpenROADM-3-2-DEG1"), ServiceFormat.Ethernet,
                "DEGREE");
        PceOpticalNode pceOpticalNode2 = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("OpenROADM-3-1-DEG1"), ServiceFormat.Ethernet,
                "DEGREE");

        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode2);
        pceLink.setClient("XPONDER-CLIENT");

        pceGraphEdge = new PceGraphEdge(pceLink);
    }

    @Test
    public void getLink() {
        Assert.assertEquals(pceGraphEdge.link().getLinkId(),
                pceLink.getLinkId());
    }
}
