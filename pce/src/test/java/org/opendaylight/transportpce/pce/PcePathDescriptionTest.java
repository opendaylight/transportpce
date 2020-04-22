/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;

public class PcePathDescriptionTest extends AbstractTest {

    private PcePathDescription pcePathDescription;
    private PceResult pceResult;
    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";
    private static final Long WAVE_LENGTH = 20L;
    private PceLink pceLink = null;

    @Before
    public void setUp() {
        // Build Link
        Link link = createRoadmToRoadm("OpenROADM-3-2-DEG1",
                "OpenROADM-3-1-DEG1",
                "DEG1-TTP-TX", "DEG1-TTP-RX").build();

        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("optical"), ServiceFormat.OMS, "test");

        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode);

        pceResult = new PceResult();
        pceResult.setRC("200");
        pcePathDescription = new PcePathDescription(List.of(pceLink), null, pceResult);

    }

    // TODO add span to the link
    @Test(expected = Exception.class)
    public void buildDescriptionsTest() {
        pcePathDescription.buildDescriptions();
        Assert.assertEquals(pcePathDescription.getReturnStructure().getMessage(), "No path available by PCE");
    }

    private static LinkBuilder createLinkBuilder(
            String srcNode, String destNode, String srcTp, String destTp, Link1Builder link1Builder) {
        SourceBuilder ietfSrcLinkBldr =
                new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(srcTp);
        //create destination link
        DestinationBuilder ietfDestLinkBldr =
                new DestinationBuilder().setDestNode(new NodeId(destNode))
                        .setDestTp(destTp);
        LinkId linkId = new LinkId(String.format(LINK_ID_FORMAT, srcNode, srcTp, destNode, destTp));

        LinkId oppositeLinkId = new LinkId("OpenROADM-3-2-DEG1-to-OpenROADM-3-1-DEG1");
        //For setting up attributes for openRoadm augment
        OMSAttributesBuilder omsAttributesBuilder =
                new OMSAttributesBuilder().setOppositeLink(oppositeLinkId);


        LinkBuilder linkBuilder = new LinkBuilder()
                .setSource(ietfSrcLinkBldr.build())
                .setDestination(ietfDestLinkBldr.build())
                .setLinkId(linkId)
                .withKey(new LinkKey(linkId));

        linkBuilder.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class,
                link1Builder.build());
        return linkBuilder;
    }

    private static LinkBuilder createRoadmToRoadm(String srcNode, String destNode, String srcTp, String destTp) {
        Link1Builder link1Builder = new Link1Builder()
                .setLinkLatency(30L)
                .setLinkType(OpenroadmLinkType.ROADMTOROADM);
        return createLinkBuilder(srcNode, destNode, srcTp, destTp, link1Builder);

    }

    private List<SupportingNode> geSupportingNodes() {
        List<SupportingNode> supportingNodes1 = new ArrayList<>();
        /* supportingNodes1
                .add(new SupportingNodeBuilder()
                        .setNodeRef(new NodeId("node 1"))
                        .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                        .build());*/

        supportingNodes1
                .add(new SupportingNodeBuilder()
                        .setNodeRef(new NodeId("node 2"))
                        .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                        .build());
        return supportingNodes1;
    }

    private NodeBuilder getNodeBuilder(List<SupportingNode> supportingNodes1) {


        //update tp of nodes
        TerminationPointBuilder xpdrTpBldr = new TerminationPointBuilder();
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder();

        tp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
        xpdrTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1 =
                new Node1Builder().setTerminationPoint(ImmutableList.of(xpdrTpBldr.build())).build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node 1"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(
                        Node1.class, node1)
                .setSupportingNode(supportingNodes1);
    }
}
