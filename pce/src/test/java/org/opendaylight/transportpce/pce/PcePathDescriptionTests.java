/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.networkanalyzer.MapUtils;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev181130.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev181130.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.amplified.link.SectionElementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.oms.attributes.AmplifiedLinkBuilder;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;

public class PcePathDescriptionTests extends AbstractTest {

    private PcePathDescription pcePathDescription;
    private PceResult pceResult;
    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";
    private static final Long WAVE_LENGTH = 20L;
    private PceLink pceLink = null;
    private Link link = null;
    private Node node = null;

    @Before
    public void setUp() {
        // Build Link
        link = createRoadmToRoadm("OpenROADM-3-2-DEG1",
                "OpenROADM-3-1-DEG1",
                "DEG1-TTP-TX", "DEG1-TTP-RX").build();

        //  Link link=genereateLinkBuilder();

        NodeBuilder node1Builder = getNodeBuilder(geSupportingNodes());
        node = node1Builder.build();

        PceOpticalNode pceOpticalNode = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("OpenROADM-3-2-DEG1"), ServiceFormat.Ethernet,
                "DEGREE");
        PceOpticalNode pceOpticalNode2 = new PceOpticalNode(node,
                OpenroadmNodeType.SRG, new NodeId("OpenROADM-3-1-DEG1"), ServiceFormat.Ethernet,
                "DEGREE");

        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode2);
        pceLink.setClient("XPONDER-CLIENT");

        pceResult = new PceResult();
        pceResult.setRC("200");
        pceResult.setRate(Long.valueOf(1));
        Map<LinkId, PceLink> map = Map.of(new LinkId("OpenROADM-3-1-DEG1-to-OpenROADM-3-2-DEG1"), pceLink);
        pcePathDescription = new PcePathDescription(List.of(pceLink),
                map, pceResult);

    }

    // TODO fix opposite link
    @Test(expected = Exception.class)
    public void buildDescriptionsTest() {

        pcePathDescription.buildDescriptions();
        Assert.assertEquals(pcePathDescription.getReturnStructure().getMessage(), "No path available by PCE");
    }

    @Test
    public void mapUtil() {
        PceConstraints pceConstraintsCalc = new PceConstraintsCalc(PceTestData
                .getPCERequest(), new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())))
                .getPceHardConstraints();
        MapUtils.mapDiversityConstraints(List.of(node), List.of(link), pceConstraintsCalc);
        MapUtils.getSupLink(link);
        MapUtils.getAllSupNode(node);
        MapUtils.getSRLGfromLink(link);
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

        // Augementation
        Augmentation<Link> aug11 = new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                .Link1Builder()
                .setAdministrativeGroup(Long.valueOf(123))
                .setAdministrativeState(State.InService)
                .setAmplified(true)
                .setLinkLatency(Long.valueOf(123))
                .setLinkLength(BigDecimal.valueOf(123))
                .setOMSAttributes(new OMSAttributesBuilder()
                        .setOppositeLink(new LinkId("OpenROADM-3-2-DEG1-to-OpenROADM-3-1-DEG1"))
                        .setSpan(new SpanBuilder().build())
                        .setTEMetric(Long.valueOf(123)).build())
                .setOperationalState(State.InService).build();

        LinkBuilder linkBuilder = new LinkBuilder()
                .setSource(ietfSrcLinkBldr.build())
                .setDestination(ietfDestLinkBldr.build())
                .setLinkId(linkId)
                .addAugmentation(Link1.class, aug11)
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
//
        supportingNodes1
                .add(new SupportingNodeBuilder()
                        .setNodeRef(new NodeId("node 1"))
                        .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                        .build());

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

    private Link genereateLinkBuilder() {

        List<LinkConcatenation> linkConcentationValues = new ArrayList<>();
        LinkConcatenation linkConcatenation = new LinkConcatenationBuilder()
                .setFiberType(LinkConcatenation.FiberType.Truewave)
                .setPmd(new FiberPmd(BigDecimal.ONE))
                .setSRLGId(Long.valueOf(1))
                .setSRLGLength(Long.valueOf(1))
                .build();
        LinkConcatenation linkConcatenation2 = new LinkConcatenationBuilder()
                .setFiberType(LinkConcatenation.FiberType.Truewave)
                .setPmd(new FiberPmd(BigDecimal.ONE))
                .setSRLGId(Long.valueOf(1))
                .setSRLGLength(Long.valueOf(1))
                .build();
        linkConcentationValues.add(linkConcatenation);
        linkConcentationValues.add(linkConcatenation2);

        List<AmplifiedLink>
                amplifiedLinkValues = new ArrayList<>();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLink al =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes
                        .AmplifiedLinkBuilder().setSectionElement(new SectionElementBuilder()
                        .setSectionElement(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130
                                .amplified.link.attributes.amplified.link.section.element.section.element
                                .SpanBuilder()
                                .setSpan(
                                        new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified
                                                .link.attributes.amplified.link.section.element.section
                                                .element.span.SpanBuilder()
                                                .setAdministrativeState(AdminStates.InService)
                                                .setAutoSpanloss(true)
                                                .setClfi("clfi")
                                                .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                                .setLinkConcatenation(linkConcentationValues)
                                                .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                                .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                                .build())
                                .build())
                        .build())
                        .setSectionEltNumber(Integer.valueOf(1)).build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes.AmplifiedLink al2 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link.attributes
                        .AmplifiedLinkBuilder().setSectionElement(new SectionElementBuilder()
                        .setSectionElement(
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.amplified.link
                                        .attributes.amplified.link.section.element.section.element.SpanBuilder()
                                        .setSpan(
                                                new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130
                                                        .amplified.link
                                                        .attributes.amplified.link.section.element.section.element.span
                                                        .SpanBuilder()
                                                        .setAdministrativeState(AdminStates.InService)
                                                        .setAutoSpanloss(true)
                                                        .setClfi("clfi")
                                                        .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                                        .setLinkConcatenation(linkConcentationValues)
                                                        .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                                        .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                                        .build())
                                        .build())
                        .build())
                        .setSectionEltNumber(Integer.valueOf(1)).build();

        amplifiedLinkValues.add(al);
        amplifiedLinkValues.add(al2);
        Augmentation<Link> aug11 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder()
                        .setAdministrativeGroup(Long.valueOf(123))
                        .setAdministrativeState(State.InService)
                        .setAmplified(true)
                        .setLinkLatency(Long.valueOf(123))
                        .setLinkLength(BigDecimal.valueOf(123))
                        .setOMSAttributes(new OMSAttributesBuilder()
                                .setAmplifiedLink(new AmplifiedLinkBuilder().setAmplifiedLink(amplifiedLinkValues)
                                        .build())
                                .setOppositeLink(new LinkId("link 1"))
                                .setSpan(new SpanBuilder().build())
                                .setTEMetric(Long.valueOf(123)).build())
                        .setOperationalState(State.InService).build();

        TransactionUtils.getNetworkForSpanLoss();
        return new LinkBuilder().setLinkId(new LinkId("OpenROADM-3-1-DEG1-to-OpenROADM-3-2-DEG1"))
                .setSource(new SourceBuilder().setSourceNode(new NodeId("OpenROADM-3-2-DEG1"))
                        .setSourceTp("DEG1-TTP-TX").build())
                .setDestination(
                        new DestinationBuilder().setDestNode(new NodeId("OpenROADM-3-1-DEG1"))
                                .setDestTp("DEG1-TTP-RX").build())
                .addAugmentation(Link1.class, aug11)
                .build();


    }
}