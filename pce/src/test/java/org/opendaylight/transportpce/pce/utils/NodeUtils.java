/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.utils;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev181130.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev181130.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsBuilder;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.common.Uint32;

public class NodeUtils {

    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";

    public static LinkBuilder createLinkBuilder(
            String srcNode, String destNode, String srcTp, String destTp, Link1Builder link1Builder) {
        SourceBuilder ietfSrcLinkBldr =
                new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(srcTp);
        //create destination link
        DestinationBuilder ietfDestLinkBldr =
                new DestinationBuilder().setDestNode(new NodeId(destNode)).setDestTp(destTp);
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

    public static LinkBuilder createRoadmToRoadm(String srcNode, String destNode, String srcTp, String destTp) {
        Link1Builder link1Builder = new Link1Builder()
                .setLinkLatency(30L)
                .setLinkType(OpenroadmLinkType.ROADMTOROADM);
        return createLinkBuilder(srcNode, destNode, srcTp, destTp, link1Builder);

    }

    public static List<SupportingNode> geSupportingNodes() {
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

    public static NodeBuilder getNodeBuilder(List<SupportingNode> supportingNodes1) {


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
                                .setAmplifiedLink(new AmplifiedLinkBuilder()
                                        .setAmplifiedLink(amplifiedLinkValues)
                                        .build())
                                .setOppositeLink(new LinkId("link 1"))
                                .setSpan(new SpanBuilder().build())
                                .setTEMetric(Long.valueOf(123)).build())
                        .setOperationalState(State.InService).build();

        TransactionUtils.getNetworkForSpanLoss();
        return new LinkBuilder()
                .setLinkId(new LinkId("OpenROADM-3-1-DEG1-to-OpenROADM-3-2-DEG1"))
                .setSource(
                        new SourceBuilder()
                                .setSourceNode(new NodeId("OpenROADM-3-2-DEG1"))
                                .setSourceTp("DEG1-TTP-TX").build())
                .setDestination(
                        new DestinationBuilder()
                                .setDestNode(new NodeId("OpenROADM-3-1-DEG1"))
                                .setDestTp("DEG1-TTP-RX").build())
                .addAugmentation(Link1.class, aug11)
                .build();


    }

    // OTN network node
    public static List<SupportingNode> getOTNSupportingNodes() {
        List<SupportingNode> supportingNodes1 = new ArrayList<>();
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

    public static NodeBuilder getOTNNodeBuilder(List<SupportingNode> supportingNodes1,
                                                OpenroadmTpType openroadmTpType) {

        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder
                tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder(openroadmTpType);
        xpdrTpBldr
                .addAugmentation(
                        org.opendaylight
                                .yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class,
                        tp1Bldr.build());

        xpdrTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                        .TerminationPoint1.class,
                createAnother2TerminationPoint(openroadmTpType).build());
        xpdrTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1.class,
                createAnotherTerminationPoint(openroadmTpType).build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 node1 = getNode1();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(ImmutableList.of(xpdrTpBldr.build()))
                        .build();


        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeIetf =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1Builder()
                        .setTerminationPoint(ImmutableList.of(xpdrTpBldr.build()))
                        .build();

        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(
                        Node1.class, node1Rev180226)
                .addAugmentation(
                        org.opendaylight
                                .yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1.class,
                        node1)
                .addAugmentation(
                        org.opendaylight
                                .yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                        nodeIetf
                )
                .setSupportingNode(supportingNodes1);
    }

    public static NodeBuilder getOTNNodeBuilderEmpty(List<SupportingNode> supportingNodes1,
                                                     OpenroadmTpType openroadmTpType) {

        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder tp1Bldr =
                getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder(openroadmTpType);
        xpdrTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1.class,
                        tp1Bldr.build());
        xpdrTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                        .TerminationPoint1.class,
                createAnotherTerminationPoint(openroadmTpType).build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 node1 = getNode1Empty();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(ImmutableList.of(xpdrTpBldr.build()))
                        .build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(
                        Node1.class, node1Rev180226)
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1.class,
                        node1)
                .setSupportingNode(supportingNodes1);
    }

    private static org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 getNode1() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder()
                .setSrgAttributes(getSrgAttributes())
                .setDegreeAttributes(getDegAttributes())
                .build();
    }

    private static org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1 getNode1Empty() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder()
                .setSrgAttributes(getEmptySrgAttributes())
                .setDegreeAttributes(getEmptyDegAttributes())
                .build();
    }

    private static DegreeAttributes getDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailableWavelengths(
                        Collections.singletonList(new AvailableWavelengthsBuilder()
                                .setIndex(20L)
                                .build()))
                .build();
    }

    private static SrgAttributes getSrgAttributes() {
        return new SrgAttributesBuilder().setAvailableWavelengths(create96AvalWaveSrg()).build();
    }

    private static DegreeAttributes getEmptyDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailableWavelengths(
                        new ArrayList<>())
                .build();
    }

    private static SrgAttributes getEmptySrgAttributes() {
        List<org.opendaylight.yang.gen
                .v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengths>
                waveList = new ArrayList<>();
        return new SrgAttributesBuilder().setAvailableWavelengths(waveList).build();
    }

    private static TerminationPointBuilder getTerminationPointBuilder(OpenroadmTpType openroadmTpType) {
        return new TerminationPointBuilder()
                .setTpId(new TpId("2"))
                .addAugmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130
                                .TerminationPoint1.class,
                        createOTNTerminationPoint(openroadmTpType).build());
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder
        getTerminationPoint1Builder(OpenroadmTpType openroadmTpType) {

        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder()
                .setTpType(openroadmTpType);

    }

    private static org.opendaylight.yang.gen
            .v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder createAnotherTerminationPoint(
            OpenroadmTpType openroadmTpType
    ) {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder()
                .setTpType(openroadmTpType)
                .setCtpAttributes((new CtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build())
                .setCpAttributes((new CpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build())
                .setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build())
                .setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build())
                .setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(new ArrayList<>()).build())
                .setXpdrClientAttributes((new XpdrClientAttributesBuilder()).build())
                .setXpdrPortAttributes((new XpdrPortAttributesBuilder()).build())
                .setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder()
                        .setTailEquipmentId("destNode" + "--" + "destTp").build());
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130
            .TerminationPoint1Builder createOTNTerminationPoint(OpenroadmTpType openroadmTpType) {

        SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(IfOCHOTU4ODU4.class)
                .build();

        SupportedInterfaceCapability supIfCapa1 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If100GEODU4.class)
                .build();
        SupportedInterfaceCapability supIfCapa2 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If10GEODU2e.class)
                .build();
        SupportedInterfaceCapability supIfCapa3 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If1GEODU0.class)
                .build();

        List<SupportedInterfaceCapability> supIfCapaList = new ArrayList<>();
        supIfCapaList.add(supIfCapa);
        supIfCapaList.add(supIfCapa1);
        supIfCapaList.add(supIfCapa2);
        supIfCapaList.add(supIfCapa3);

        TpSupportedInterfaces tpSupIf = new TpSupportedInterfacesBuilder()
                .setSupportedInterfaceCapability(supIfCapaList)
                .build();

        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder();

        return new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130
                .TerminationPoint1Builder()
                .setTpType(openroadmTpType)
                .setTpSupportedInterfaces(tpSupIf)
                .setXpdrTpPortConnectionAttributes(xtpcaBldr.build());
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1Builder createAnother2TerminationPoint(OpenroadmTpType openroadmTpType) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1Builder()
                .setTpType(openroadmTpType);
    }

    private static List<org.opendaylight.yang.gen
            .v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengths> create96AvalWaveSrg() {

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengths>
                waveList = new ArrayList<>();

        for (int i = 1; i < 97; i++) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                    .AvailableWavelengthsBuilder avalBldr
                = new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                    .AvailableWavelengthsBuilder()
                        .setIndex(Uint32.valueOf(i))
                        .withKey(new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                            .AvailableWavelengthsKey(Uint32.valueOf(i)));
            waveList.add(avalBldr.build());
        }
        return waveList;
    }

}
