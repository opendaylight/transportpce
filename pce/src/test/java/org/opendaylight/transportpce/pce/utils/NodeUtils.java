/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.FiberPmd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.amplified.link.SectionElementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.span.attributes.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.span.attributes.LinkConcatenationKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.link.oms.attributes.AmplifiedLinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.link.oms.attributes.SpanBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
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
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class NodeUtils {

    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";

    public static LinkBuilder createLinkBuilder(
            String srcNode, String destNode, String srcTp, String destTp, Link1Builder link1Builder) {
        SourceBuilder ietfSrcLinkBldr =
                new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(new TpId(srcTp));
        //create destination link
        DestinationBuilder ietfDestLinkBldr =
                new DestinationBuilder().setDestNode(new NodeId(destNode)).setDestTp(new TpId(destTp));
        LinkId linkId = new LinkId(String.format(LINK_ID_FORMAT, srcNode, srcTp, destNode, destTp));

        LinkId oppositeLinkId = new LinkId("OpenROADM-3-2-DEG1-to-OpenROADM-3-1-DEG1");
        //For setting up attributes for openRoadm augment
        OMSAttributesBuilder omsAttributesBuilder =
                new OMSAttributesBuilder();

        // Augementation
        Augmentation<Link> aug11 = new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529
                .Link1Builder()
                .setAmplified(true)
                .setOMSAttributes(new OMSAttributesBuilder()
                        .setSpan(new SpanBuilder().build())
                        .build())
                .build();

        return new LinkBuilder()
                .setSource(ietfSrcLinkBldr.build())
                .setDestination(ietfDestLinkBldr.build())
                .setLinkId(linkId)
                .addAugmentation(aug11)
                .withKey(new LinkKey(linkId))
                .addAugmentation(link1Builder.setOppositeLink(oppositeLinkId).build());
    }

    public static LinkBuilder createRoadmToRoadm(String srcNode, String destNode, String srcTp, String destTp) {
        Link1Builder link1Builder = new Link1Builder()
                .setLinkLatency(Uint32.valueOf(30))
                .setLinkType(OpenroadmLinkType.ROADMTOROADM)
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService);
        return createLinkBuilder(srcNode, destNode, srcTp, destTp, link1Builder);

    }

    public static Map<SupportingNodeKey, SupportingNode> geSupportingNodes() {
        Map<SupportingNodeKey, SupportingNode> supportingNodes1 = new HashMap<>();
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

    public static NodeBuilder getNodeBuilder(Map<SupportingNodeKey,SupportingNode> supportingNodes1) {


        //update tp of nodes
        TerminationPointBuilder xpdrTpBldr = new TerminationPointBuilder()
                .withKey(new TerminationPointKey(new TpId("xpdr")));
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder tp11Bldr =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder()
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService);

        tp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        xpdrTpBldr.addAugmentation(tp11Bldr.build());
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1 =
            new Node1Builder().setTerminationPoint(Map.of(xpdr.key(),xpdr)).build();
        Node1 node11 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1Builder()
            .setAdministrativeState(AdminStates.InService)
            .setOperationalState(State.InService).build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node 1"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1)
                .addAugmentation(node11)
                .setSupportingNode(supportingNodes1);
    }

    private Link genereateLinkBuilder() {

        Map<LinkConcatenationKey,LinkConcatenation> linkConcentationValues = new HashMap<>();
        LinkConcatenation linkConcatenation = new LinkConcatenationBuilder()
                .setFiberType(LinkConcatenation.FiberType.Truewave)
                .setPmd(new FiberPmd(BigDecimal.ONE))
                .setSRLGId(Uint32.valueOf(1))
                .setSRLGLength(Uint32.valueOf(1))
                .build();
        LinkConcatenation linkConcatenation2 = new LinkConcatenationBuilder()
                .setFiberType(LinkConcatenation.FiberType.Truewave)
                .setPmd(new FiberPmd(BigDecimal.ONE))
                .setSRLGId(Uint32.valueOf(1))
                .setSRLGLength(Uint32.valueOf(1))
                .build();
        linkConcentationValues.put(linkConcatenation.key(),linkConcatenation);
        linkConcentationValues.put(linkConcatenation2.key(),linkConcatenation2);

        Map<AmplifiedLinkKey,AmplifiedLink>
                amplifiedLinkValues = new HashMap<>();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLink al =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes
                        .AmplifiedLinkBuilder().setSectionElement(new SectionElementBuilder()
                        .setSectionElement(new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529
                                .amplified.link.attributes.amplified.link.section.element.section.element
                                .SpanBuilder()
                                .setSpan(
                                        new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified
                                                .link.attributes.amplified.link.section.element.section
                                                .element.span.SpanBuilder()
                                                .setAdministrativeState(AdminStates.InService)
                                                .setAutoSpanloss(true)
                                                .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                                .setLinkConcatenation(linkConcentationValues)
                                                .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                                .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                                .build())
                                .build())
                        .build())
                        .setSectionEltNumber(Uint16.valueOf(1)).build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes.AmplifiedLink al2 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link.attributes
                        .AmplifiedLinkBuilder().setSectionElement(new SectionElementBuilder()
                        .setSectionElement(
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529.amplified.link
                                        .attributes.amplified.link.section.element.section.element.SpanBuilder()
                                        .setSpan(
                                                new org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev200529
                                                        .amplified.link
                                                        .attributes.amplified.link.section.element.section.element.span
                                                        .SpanBuilder()
                                                        .setAdministrativeState(AdminStates.InService)
                                                        .setAutoSpanloss(true)
                                                        .setEngineeredSpanloss(new RatioDB(BigDecimal.ONE))
                                                        .setLinkConcatenation(linkConcentationValues)
                                                        .setSpanlossBase(new RatioDB(BigDecimal.ONE))
                                                        .setSpanlossCurrent(new RatioDB(BigDecimal.ONE))
                                                        .build())
                                        .build())
                        .build())
                        .setSectionEltNumber(Uint16.valueOf(1)).build();

        amplifiedLinkValues.put(al.key(),al);
        amplifiedLinkValues.put(al2.key(),al2);
        Augmentation<Link> aug11 =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Link1Builder()
                        .setAmplified(true)
                        .setOMSAttributes(new OMSAttributesBuilder()
                                .setAmplifiedLink(new AmplifiedLinkBuilder()
                                        .setAmplifiedLink(amplifiedLinkValues)
                                        .build())
                                .setSpan(new SpanBuilder().build())
                                .build())
                        .build();
        Augmentation<Link> aug111 = new Link1Builder().setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService).build();

        TransactionUtils.getNetworkForSpanLoss();
        return new LinkBuilder()
                .setLinkId(new LinkId("OpenROADM-3-1-DEG1-to-OpenROADM-3-2-DEG1"))
                .setSource(
                        new SourceBuilder()
                                .setSourceNode(new NodeId("OpenROADM-3-2-DEG1"))
                                .setSourceTp(new TpId("DEG1-TTP-TX")).build())
                .setDestination(
                        new DestinationBuilder()
                                .setDestNode(new NodeId("OpenROADM-3-1-DEG1"))
                                .setDestTp(new TpId("DEG1-TTP-RX")).build())
                .addAugmentation(aug11)
                .addAugmentation(aug111)
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

    public static NodeBuilder getOTNNodeBuilder(Map<SupportingNodeKey,SupportingNode> supportingNodes1,
                                                OpenroadmTpType openroadmTpType) {

        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder
                tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder(openroadmTpType);
        xpdrTpBldr
                .addAugmentation(tp1Bldr.build());

        xpdrTpBldr.addAugmentation(createAnother2TerminationPoint(openroadmTpType).build());
        xpdrTpBldr.addAugmentation(createAnotherTerminationPoint(openroadmTpType).build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1 node1 = getNode1();
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();


        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeIetf =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();

        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1Rev180226)
                .addAugmentation(node1)
                .addAugmentation(nodeIetf)
                .setSupportingNode(supportingNodes1);
    }

    public static NodeBuilder getOTNNodeBuilderEmpty(Map<SupportingNodeKey,SupportingNode> supportingNodes1,
                                                     OpenroadmTpType openroadmTpType) {

        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder tp1Bldr =
                getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder(openroadmTpType);
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        xpdrTpBldr.addAugmentation(createAnotherTerminationPoint(openroadmTpType).build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1 node1 = getNode1Empty();
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1Rev180226)
                .addAugmentation(node1)
                .setSupportingNode(supportingNodes1);
    }

    private static org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1 getNode1() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1Builder()
                .setSrgAttributes(getSrgAttributes())
                .setDegreeAttributes(getDegAttributes())
                .build();
    }

    private static org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1 getNode1Empty() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1Builder()
                .setSrgAttributes(getEmptySrgAttributes())
                .setDegreeAttributes(getEmptyDegAttributes())
                .build();
    }

    private static DegreeAttributes getDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available())
                .build();
    }

    private static SrgAttributes getSrgAttributes() {
        return new SrgAttributesBuilder().setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available()).build();
    }

    private static DegreeAttributes getEmptyDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailFreqMaps(Map.of())
                .build();
    }

    private static SrgAttributes getEmptySrgAttributes() {
        return new SrgAttributesBuilder().setAvailFreqMaps(Map.of()).build();
    }

    private static TerminationPointBuilder getTerminationPointBuilder(OpenroadmTpType openroadmTpType) {
        return new TerminationPointBuilder()
                .setTpId(new TpId("2"))
                .addAugmentation(createOTNTerminationPoint(openroadmTpType).build());
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder
        getTerminationPoint1Builder(OpenroadmTpType openroadmTpType) {

        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder()
                .setTpType(openroadmTpType);

    }

    private static org.opendaylight.yang.gen
            .v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1Builder createAnotherTerminationPoint(
            OpenroadmTpType openroadmTpType
    ) {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1Builder()
                .setCtpAttributes((new CtpAttributesBuilder()).build())
                .setCpAttributes((new CpAttributesBuilder()).build())
                .setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(Map.of()).build())
                .setXpdrClientAttributes((new XpdrClientAttributesBuilder()).build())
                .setXpdrPortAttributes((new XpdrPortAttributesBuilder()).build())
                .setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder()
                        .setTailEquipmentId("destNode" + "--" + "destTp").build());
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529
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

        Map<SupportedInterfaceCapabilityKey,SupportedInterfaceCapability> supIfCapaList = new HashMap<>();
        supIfCapaList.put(supIfCapa.key(),supIfCapa);
        supIfCapaList.put(supIfCapa1.key(),supIfCapa1);
        supIfCapaList.put(supIfCapa2.key(),supIfCapa2);
        supIfCapaList.put(supIfCapa3.key(),supIfCapa3);

        TpSupportedInterfaces tpSupIf = new TpSupportedInterfacesBuilder()
                .setSupportedInterfaceCapability(supIfCapaList)
                .build();

        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder();

        return new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529
                .TerminationPoint1Builder()
                .setTpSupportedInterfaces(tpSupIf)
                .setXpdrTpPortConnectionAttributes(xtpcaBldr.build());
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
            .TerminationPoint1Builder createAnother2TerminationPoint(OpenroadmTpType openroadmTpType) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                .TerminationPoint1Builder()
                .setOperationalState(State.InService)
                .setAdministrativeState(AdminStates.InService)
                .setTpType(openroadmTpType);
    }

}
