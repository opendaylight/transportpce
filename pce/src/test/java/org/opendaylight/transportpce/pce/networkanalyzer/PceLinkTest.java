/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder;
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




public class PceLinkTest extends AbstractTest {

    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";
    private static final Long WAVE_LENGTH = 20L;
    private PceLink pceLink = null;

    @Before
    public void setup() {

    }

    @Test(expected = NullPointerException.class)
    public void buildPceOpticalNodeRoadmTest() {
        Link link = createLink("srcNode",
                "destNode",
                "srcTp", "destTp").build();
        Link1 link1 = link.augmentation(Link1.class);
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("optical"), ServiceFormat.OMS, "test");


        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode);

    }

    @Test
    public void testPceOpticalNode() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.ROADM, new NodeId("optical"), ServiceFormat.OMS, "test");
        pceOpticalNode.initSrgTps();
        pceOpticalNode.initXndrTps();
        pceOpticalNode.initWLlist();
        Assert.assertFalse(pceOpticalNode.isValid());
        Assert.assertFalse(pceOpticalNode.checkWL(12));
        Assert.assertTrue(pceOpticalNode.checkTP("testTP"));
    }

    @Test(expected = NullPointerException.class)
    public void testPceOTNNode() {
        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        Node node = node1Builder.build();
        Assert.assertNotNull(node.augmentation(Node1.class));
        // OpenroadmNodeType nodeType = node.augmentation(Node1.class).;

        PceOtnNode pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.SRG, node.getNodeId(), "otn", "serviceType");

        //pceOtnNode.validateXponder(anodeId, znodeId);
        /*
        PceOtnNode pceOtnNode =
                new PceOtnNode(node, OpenroadmNodeType.DEGREE,
                        new NodeId("optical"),
                        ServiceFormat.OTM.getName(), "test");*/
        // pceOtnNode.initXndrTps("mode");
       /* pceOtnNode.initSrgTps();
        pceOtnNode.initXndrTps();
        pceOtnNode.initWLlist();
        Assert.assertFalse(pceOtnNode.isValid());
        Assert.assertFalse(pceOtnNode.checkWL(12));
        Assert.assertTrue(pceOtnNode.checkTP("testTP"));*/
    }

    private static LinkBuilder createLink(String srcNode, String destNode, String srcTp, String destTp) {
        //create source link
        SourceBuilder ietfSrcLinkBldr = new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(srcTp);
        //create destination link
        DestinationBuilder ietfDestLinkBldr = new DestinationBuilder().setDestNode(new NodeId(destNode))
                .setDestTp(destTp);
        LinkId linkId = new LinkId(String.format(LINK_ID_FORMAT, srcNode, srcTp, destNode, destTp));

        /*LinkId oppositeLinkId = new LinkId("opposite");
        //For setting up attributes for openRoadm augment
        OMSAttributesBuilder omsAttributesBuilder = new OMSAttributesBuilder().setOppositeLink(oppositeLinkId);
        Link1Builder link1Builder = new Link1Builder().setOMSAttributes(omsAttributesBuilder.build());
*/
        LinkBuilder linkBuilder = new LinkBuilder()
                .setSource(ietfSrcLinkBldr.build())
                .setDestination(ietfDestLinkBldr.build())
                .setLinkId(linkId)
                .withKey(new LinkKey(linkId));
        // linkBuilder.addAugmentation(Link1.class, link1Builder.build());
        return linkBuilder;
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
