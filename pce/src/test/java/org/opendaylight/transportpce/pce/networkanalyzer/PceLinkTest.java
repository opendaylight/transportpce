/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.pce.node.mccapabilities.NodeMcCapability;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.oms.attributes.SpanBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;

public class PceLinkTest extends AbstractTest {

    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";
    private PceLink pceLink = null;
    private String deviceNodeId = "device node";
    private String deviceNodeId2 = "device node 2";
    private String serviceType = "100GE";
    @Mock
    private PortMapping portMapping;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBuildPceLinkRoadmToRoadm() {
        Node node = getNodeBuilder(geSupportingNodes()).setNodeId(new NodeId("test")).build();
        pceLink = new PceLink(
            createRoadmToRoadm("srcNode", "destNode", "srcTp", "destTp").build(),
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()),
            new PceOpticalNode(deviceNodeId2, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()));
    }

    @Test
    void testBuildPceLinkRoadmToRoadmWithoutPMD() {
        Link link = createRoadmToRoadmWithoutPMD("srcNode", "destNode", "srcTp", "destTp").build();
        Node node = getNodeBuilder(geSupportingNodes()).setNodeId(new NodeId("test")).build();
        pceLink = new PceLink(
            link,
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()),
            new PceOpticalNode(deviceNodeId2, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()));
        assertNotNull(MapUtils.getOmsAttributesSpan(link));
        assertEquals(1, pceLink.getsrlgList().size());
        assertEquals(20.0, pceLink.getspanLoss(), 0.005, "Checking length loss");
        assertEquals(825.0, pceLink.getcd(), 0.005, "Checking length loss");
        assertEquals(4.0, pceLink.getpmd2(), 0.005, "Checking PMDvalue of link");
    }

    @Test
    void testBuildPceLinkRoadmToRoadmWithoutLinkLatency() {
        Node node = getNodeBuilder(geSupportingNodes()).setNodeId(new NodeId("test")).build();
        pceLink = new PceLink(
            createRoadmToRoadmWithoutLinkLatency("srcNode", "destNode", "srcTp", "destTp").build(),
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()),
            new PceOpticalNode(deviceNodeId2, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()));
    }

    @Test
    void testBuildPceLinkOTN() {
        // TODO: Modify with OTN node not PceOpticalNode
        Node node = getNodeBuilder(geSupportingNodes()).setNodeId(new NodeId("test")).build();
        pceLink = new PceLink(
            createOTNLink("srcNode", "destNode", "srcTp", "destTp").build(),
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.SWITCH, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()),
            new PceOpticalNode(deviceNodeId2, serviceType, portMapping, node,
                OpenroadmNodeType.SWITCH, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()));
    }

    @Test
    void testBuildPceLinkExponder() {
        Node node = getNodeBuilder(geSupportingNodes()).setNodeId(new NodeId("test")).build();
        pceLink = new PceLink(
            createXponderLink("srcNode", "destNode", "srcTp", "destTp").build(),
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.XPONDER, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()),
            new PceOpticalNode(deviceNodeId2, serviceType, portMapping, node,
                OpenroadmNodeType.SRG, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()));
    }

    @Test
    void testCalcSpanOSNR() {
        Link link = createRoadmToRoadm("srcNode", "destNode", "srcTp", "destTp").build();
        Node node = getNodeBuilder(geSupportingNodes()).setNodeId(new NodeId("test")).build();
        pceLink = new PceLink(
            link,
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()),
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()));
        assertNotNull(MapUtils.getOmsAttributesSpan(link));
        // assertNotNull(pceLink.getosnr());
        assertEquals(1, pceLink.getsrlgList().size());
        assertEquals(0.25, pceLink.getpmd2(), 0.005, "Checking PMDvalue of link");
        assertEquals(825, pceLink.getcd(), 0.005, "Checking CDvalue of link");
        // assertTrue(7.857119000000001 == pceLink.getosnr());
        assertNull(pceLink.getOppositeLink());
        assertNull(pceLink.getOppositeLink());
        assertNotNull(pceLink.getDestTP());
        assertNotNull(pceLink.getlinkType());
        assertNotNull(pceLink.getLinkId());
        assertNotNull(pceLink.getSourceId());
        assertNotNull(pceLink.getDestId());
        pceLink.setClientA("specific_client");
        assertTrue(pceLink.getClientA().equals("specific_client"));
        assertNotNull(pceLink.getClientA());
        assertNotNull(pceLink.getLatency());
        assertNotNull(pceLink.getAvailableBandwidth());
        assertNotNull(pceLink.getUsedBandwidth());
        assertNotNull(pceLink.getsourceNetworkSupNodeId());
        assertNotNull(pceLink.getdestNetworkSupNodeId());
        assertNotNull(pceLink.getSourceTP());
        assertNotNull(pceLink.getsourceCLLI());
        assertNotNull(pceLink.getdestCLLI());
        assertTrue(pceLink.toString().equals("PceLink type=" + pceLink.getlinkType()
            + " ID=" + pceLink.getLinkId().getValue() + " latency=" + pceLink.getLatency().intValue()));
    }

    @Test
    void testWrongSpanLoss() {
        Link link = createInvalidRoadmToRoadm("srcNode", "destNode", "srcTp", "destTp").build();
        Node node = getNodeBuilder(geSupportingNodes()).setNodeId(new NodeId("test")).build();
        pceLink = new PceLink(
            link,
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()),
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()));
        assertNull(MapUtils.getOmsAttributesSpan(link));
        assertNull(pceLink.getpmd2());
        assertNull(pceLink.getpowerCorrection());
        assertNull(pceLink.getcd());
    }

    @Test
    void testExtrapolatedPMD() {
        Link link = createRoadmToRoadmWithoutPMD("srcNode", "destNode", "srcTp", "destTp").build();
        Node node = getNodeBuilder(geSupportingNodes()).setNodeId(new NodeId("test")).build();
        pceLink = new PceLink(
            link,
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()),
            new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.DEGREE, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                new NodeMcCapability()));
        assertNotNull(MapUtils.getOmsAttributesSpan(link));
        assertEquals(1, pceLink.getsrlgList().size());
        assertEquals(4.0, pceLink.getpmd2(), 0.005, "Checking PMDvalue of link");
    }

    private static LinkBuilder createOTNLink(String srcNode, String destNode, String srcTp, String destTp) {
        return createLinkBuilder(false, false, 10000.0, srcNode, destNode, srcTp, destTp,
            new Link1Builder()
                .setLinkType(OpenroadmLinkType.OTNLINK)
                .setOperationalState(State.InService)
                .setAdministrativeState(AdminStates.InService));
    }

    private static LinkBuilder createXponderLink(String srcNode, String destNode, String srcTp, String destTp) {
        // create source link
        return createLinkBuilder(false, false, 10.0, srcNode, destNode, srcTp, destTp,
            new Link1Builder()
                .setLinkType(OpenroadmLinkType.XPONDERINPUT)
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService));
    }

    private static LinkBuilder createLinkBuilder(boolean pmdpresent, boolean omspresent, double length,
            String srcNode, String destNode, String srcTp, String destTp, Link1Builder link1Builder) {
        LinkId linkId = new LinkId(String.format(LINK_ID_FORMAT, srcNode, srcTp, destNode, destTp));
        LinkBuilder linkBuilder = new LinkBuilder()
            .setSource(
                new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(new TpId(srcTp)).build())
            .setDestination(
                new DestinationBuilder().setDestNode(new NodeId(destNode)).setDestTp(new TpId(destTp)).build())
            .setLinkId(linkId)
            .withKey(new LinkKey(linkId))
            .addAugmentation(link1Builder.build());
        if (omspresent) {
            LinkConcatenation linkConcatenation = new LinkConcatenationBuilder()
                .withKey(new LinkConcatenationKey(Uint32.valueOf(1)))
                .setSRLGLength(Decimal64.valueOf(length, RoundingMode.FLOOR))
                .addAugmentation(
                    pmdpresent
                        ? new LinkConcatenation1Builder().setFiberType(FiberType.Smf)
                                .setPmd(FiberPmd.getDefaultInstance("0.500")).build()
                        : new LinkConcatenation1Builder().setFiberType(FiberType.Smf).build())
                .build();
            linkBuilder.addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Link1Builder()
                    .setOMSAttributes(
                        new OMSAttributesBuilder()
                            .setSpan(new SpanBuilder()
                            // .setSpanlossCurrent(new RatioDB(Decimal64.valueOf("55")))
                            .setSpanlossCurrent(new RatioDB(Decimal64.valueOf("20")))
                            .setLinkConcatenation(Map.of(linkConcatenation.key(), linkConcatenation))
                            .build())
                        .build())
                    .build());
        }
        return linkBuilder;
    }

    private static LinkBuilder createRoadmToRoadm(String srcNode, String destNode, String srcTp, String destTp) {
        return createLinkBuilder(true, true, 50000.0, srcNode, destNode, srcTp, destTp, new Link1Builder()
                .setLinkLatency(Uint32.valueOf(100))
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService)
                .setLinkType(OpenroadmLinkType.ROADMTOROADM)
                .setLinkLength(Decimal64.valueOf(50.0, RoundingMode.FLOOR)));
    }

    private static LinkBuilder createRoadmToRoadmWithoutPMD(String srcNode, String destNode, String srcTp,
            String destTp) {
        return createLinkBuilder(false, true, 50000.0, srcNode, destNode, srcTp, destTp, new Link1Builder()
                .setLinkLatency(Uint32.valueOf(100))
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService)
                .setLinkType(OpenroadmLinkType.ROADMTOROADM)
                .setLinkLength(Decimal64.valueOf(50.0, RoundingMode.FLOOR)));
    }

    private static LinkBuilder createInvalidRoadmToRoadm(String srcNode, String destNode,
            String srcTp, String destTp) {
        return createLinkBuilder(false, false, 0.0, srcNode, destNode, srcTp, destTp, new Link1Builder()
                .setLinkLatency(Uint32.valueOf(100))
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService)
                .setLinkType(OpenroadmLinkType.ROADMTOROADM));
    }

    private static LinkBuilder createRoadmToRoadmWithoutLinkLatency(
            String srcNode, String destNode, String srcTp, String destTp) {
        return createLinkBuilder(true, true, 50000.0, srcNode, destNode, srcTp, destTp, new Link1Builder()
                .setLinkType(OpenroadmLinkType.ROADMTOROADM));
    }

    private Map<SupportingNodeKey, SupportingNode> geSupportingNodes() {
        SupportingNode supportingNode1 = new SupportingNodeBuilder()
            .setNodeRef(new NodeId("node 1"))
            .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
            .build();
        SupportingNode supportingNode2 = new SupportingNodeBuilder()
            .setNodeRef(new NodeId("node 2"))
            .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .build();
        return new HashMap<>(Map.of(
                supportingNode1.key(), supportingNode1,
                supportingNode2.key(), supportingNode2));
    }

    private NodeBuilder getNodeBuilder(Map<SupportingNodeKey, SupportingNode> supportingNodes1) {
        // update tp of nodes
        TerminationPoint xpdr = new TerminationPointBuilder()
            .withKey(new TerminationPointKey(new TpId("xpdr")))
            .addAugmentation(
                new TerminationPoint1Builder()
                    .setTpType(OpenroadmTpType.XPONDERNETWORK)
                    .setAdministrativeState(AdminStates.InService)
                    .setOperationalState(State.InService)
                    .build())
            .build();
        return new NodeBuilder()
            .setNodeId(new NodeId("node 1"))
            .withKey(new NodeKey(new NodeId("node 1")))
            .addAugmentation(new Node1Builder().setTerminationPoint(Map.of(xpdr.key(), xpdr)).build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                    .setOperationalState(State.InService).setAdministrativeState(AdminStates.InService).build())
            .setSupportingNode(supportingNodes1);
    }
}
