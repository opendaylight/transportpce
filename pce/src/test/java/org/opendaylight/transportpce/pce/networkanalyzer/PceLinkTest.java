/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev181130.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.oms.attributes.SpanBuilder;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;



public class PceLinkTest extends AbstractTest {

    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";
    private PceLink pceLink = null;

    @Before
    public void setup() {

    }

    @Test
    public void testBuildPceLinkRoadmToRoadm() {
        Link link = createRoadmToRoadm("srcNode",
                "destNode",
                "srcTp", "destTp").build();

        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode);
    }

    @Test
    public void testBuildPceLinkRoadmToRoadmWithoutLinkLatency() {
        Link link = createRoadmToRoadmWithoutLinkLatency("srcNode",
                "destNode",
                "srcTp", "destTp").build();

        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode);
    }

    @Test
    public void testBuildPceLinkOTN() {
        Link link = createOTNLink("srcNode",
                "destNode",
                "srcTp", "destTp").build();

        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode);

    }

    @Test
    public void testBuildPceLinkExponder() {
        Link link = createXponderLink("srcNode",
                "destNode",
                "srcTp", "destTp").build();

        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode);
    }

    @Test
    public void testCalcSpanOSNR() {
        Link link = createRoadmToRoadm("srcNode",
                "destNode",
                "srcTp", "destTp").build();

        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode);
        Assert.assertNotNull(MapUtils.getOmsAttributesSpan(link));
        Assert.assertNotNull(pceLink.calcSpanOSNR());
        Assert.assertEquals(0, pceLink.getsrlgList().size());
        Assert.assertTrue(7.857119000000001 == pceLink.calcSpanOSNR());
        Assert.assertNull(pceLink.getOppositeLink());
        Assert.assertNull(pceLink.getOppositeLink());
        Assert.assertNotNull(pceLink.getDestTP());
        Assert.assertNotNull(pceLink.getlinkType());
        Assert.assertNotNull(pceLink.getLinkId());
        Assert.assertNotNull(pceLink.getSourceId());
        Assert.assertNotNull(pceLink.getDestId());
        pceLink.setClient("specific_client");
        Assert.assertTrue(pceLink.getClient().equals("specific_client"));
        Assert.assertNotNull(pceLink.getClient());
        Assert.assertNotNull(pceLink.getLatency());
        Assert.assertNotNull(pceLink.getAvailableBandwidth());
        Assert.assertNotNull(pceLink.getUsedBandwidth());
        Assert.assertNotNull(pceLink.getsourceNetworkSupNodeId());
        Assert.assertNotNull(pceLink.getdestNetworkSupNodeId());
        Assert.assertNotNull(pceLink.getosnr());
        Assert.assertNotNull(pceLink.getSourceTP());
        Assert.assertNotNull(pceLink.getsourceCLLI());
        Assert.assertNotNull(pceLink.getdestCLLI());
        Assert.assertTrue(pceLink.toString().equals("PceLink type=" + pceLink.getlinkType()
                + " ID=" + pceLink.getLinkId().getValue() + " latency=" + pceLink.getLatency().intValue()));

    }

    @Test(expected = NullPointerException.class)
    public void testPceOTNNode() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();
        Assert.assertNotNull(node.augmentation(Node1.class));
        // OpenroadmNodeType nodeType = node.augmentation(Node1.class).;

        PceOtnNode pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.SRG, node.getNodeId(), "otn", "serviceType");

    }

    private static LinkBuilder createOTNLink(String srcNode, String destNode, String srcTp, String destTp) {
        Link1Builder link1Builder = new Link1Builder()
                .setLinkType(OpenroadmLinkType.OTNLINK);

        //create source link
        return createLinkBuilder(srcNode, destNode, srcTp, destTp, link1Builder);

    }

    private static LinkBuilder createXponderLink(String srcNode, String destNode, String srcTp, String destTp) {
        Link1Builder link1Builder = new Link1Builder()
                .setLinkType(OpenroadmLinkType.XPONDERINPUT);

        //create source link
        return createLinkBuilder(srcNode, destNode, srcTp, destTp, link1Builder);

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

        LinkId oppositeLinkId = new LinkId("opposite");
        //For setting up attributes for openRoadm augment
        LinkConcatenation linkConcatenation = new LinkConcatenationBuilder()
                .setSRLGLength(Uint32.valueOf(20))
                .setFiberType(LinkConcatenation.FiberType.Dsf)
                .build();
        OMSAttributesBuilder omsAttributesBuilder =
                new OMSAttributesBuilder()
                        .setSpan(new SpanBuilder()
                                .setSpanlossCurrent(new RatioDB(new BigDecimal("55")))
                                .setLinkConcatenation(Map.of(linkConcatenation.key(),
                                        linkConcatenation
                                )).build()).setOppositeLink(oppositeLinkId);


        LinkBuilder linkBuilder = new LinkBuilder()
                .setSource(ietfSrcLinkBldr.build())
                .setDestination(ietfDestLinkBldr.build())
                .setLinkId(linkId)
                .withKey(new LinkKey(linkId));

        linkBuilder.addAugmentation(link1Builder.build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder linkBuilderNetworkLink
                = new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder()
                .setOMSAttributes(omsAttributesBuilder
                        .build());


        linkBuilder.addAugmentation(linkBuilderNetworkLink.build());

        return linkBuilder;
    }

    private static LinkBuilder createRoadmToRoadm(String srcNode, String destNode, String srcTp, String destTp) {
        Link1Builder link1Builder = new Link1Builder()
                .setLinkLatency(Uint32.valueOf(100))
                .setLinkType(OpenroadmLinkType.ROADMTOROADM);
        return createLinkBuilder(srcNode, destNode, srcTp, destTp, link1Builder);

    }

    private static LinkBuilder createRoadmToRoadmWithoutLinkLatency(
            String srcNode, String destNode, String srcTp, String destTp) {
        Link1Builder link1Builder = new Link1Builder()
                .setLinkType(OpenroadmLinkType.ROADMTOROADM);
        return createLinkBuilder(srcNode, destNode, srcTp, destTp, link1Builder);

    }

    private Map<SupportingNodeKey,SupportingNode> geSupportingNodes() {
        Map<SupportingNodeKey,SupportingNode> supportingNodes1 = new HashMap<>();
        SupportingNode supportingNode1 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 1"))
                .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                .build();
        supportingNodes1
                .put(supportingNode1.key(),supportingNode1);

        SupportingNode supportingNode2 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 2"))
                .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                .build();
        supportingNodes1
                .put(supportingNode2.key(),supportingNode2);
        return supportingNodes1;
    }

    private NodeBuilder getNodeBuilder(Map<SupportingNodeKey,SupportingNode> supportingNodes1) {


        //update tp of nodes
        TerminationPointBuilder xpdrTpBldr = new TerminationPointBuilder();
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder();

        tp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1 =
                new Node1Builder().setTerminationPoint(Map.of(xpdr.key(),xpdr)).build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node 1"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1)
                .setSupportingNode(supportingNodes1);
    }

}
