/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.utils;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.OMSAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.oms.attributes.SpanBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class NodeUtils {

    private static final String LINK_ID_FORMAT = "%1$s-%2$sto%3$s-%4$s";

    public static LinkBuilder createLinkBuilder(boolean omsPresent,
            String srcNode, String destNode, String srcTp, String destTp, Link1Builder link1Builder) {
        LinkId linkId = new LinkId(String.format(LINK_ID_FORMAT, srcNode, srcTp, destNode, destTp));
        var oppLink1 = link1Builder
            .setOppositeLink(new LinkId(String.format(LINK_ID_FORMAT, destNode, destTp, srcNode, srcTp)))
            .build();
        LinkBuilder linkBldr =
            new LinkBuilder()
                .setSource(
                    new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(new TpId(srcTp)).build())
                .setDestination(
                    new DestinationBuilder().setDestNode(new NodeId(destNode)).setDestTp(new TpId(destTp)).build())
                .setLinkId(linkId);
        if (!omsPresent) {
            return linkBldr
                .withKey(new LinkKey(linkId))
                .addAugmentation(oppLink1);
        }
        LinkConcatenation linkConcatenation = new LinkConcatenationBuilder()
            .withKey(new LinkConcatenationKey(Uint32.valueOf(1)))
            .setSRLGLength(Decimal64.valueOf(50000, RoundingMode.FLOOR))
            .addAugmentation(new LinkConcatenation1Builder().setFiberType(FiberType.Smf).build())
            .build();
        return linkBldr
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Link1Builder()
                        .setAmplified(false)
                        .setOMSAttributes(
                            new OMSAttributesBuilder()
                                .setSpan(
                                    new SpanBuilder()
                                        .setSpanlossCurrent(new RatioDB(Decimal64.valueOf("20")))
                                        .setLinkConcatenation(Map.of(linkConcatenation.key(), linkConcatenation))
                                        .build())
                                .build())
                        .build())
                .withKey(new LinkKey(linkId))
                .addAugmentation(oppLink1);
    }

    public static LinkBuilder createRoadmToRoadm(String srcNode, String destNode, String srcTp, String destTp) {
        return createLinkBuilder(true, srcNode, destNode, srcTp, destTp,
            new Link1Builder()
                .setLinkLatency(Uint32.valueOf(2))
                .setLinkLength(Decimal64.valueOf("50.0"))
                .setLinkType(OpenroadmLinkType.ROADMTOROADM)
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService));
    }

    public static LinkBuilder createAdd(String srcNode, String destNode, String srcTp, String destTp) {
        return createLinkBuilder(false, srcNode, destNode, srcTp, destTp,
            new Link1Builder()
               .setLinkLatency(Uint32.valueOf(0))
               .setLinkLength(Decimal64.valueOf("0.01"))
               .setLinkType(OpenroadmLinkType.ADDLINK)
               .setAdministrativeState(AdminStates.InService)
               .setOperationalState(State.InService));
    }

    public static LinkBuilder createDrop(String srcNode, String destNode, String srcTp, String destTp) {
        return createLinkBuilder(false, srcNode, destNode, srcTp, destTp,
            new Link1Builder()
                .setLinkLatency(Uint32.valueOf(0))
                .setLinkLength(Decimal64.valueOf("0.01"))
                .setLinkType(OpenroadmLinkType.DROPLINK)
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService));
    }

    public static LinkBuilder createXpdrToSrg(String srcNode, String destNode, String srcTp, String destTp) {
        return createLinkBuilder(false, srcNode, destNode, srcTp, destTp,
            new Link1Builder()
                .setLinkLatency(Uint32.valueOf(0))
                .setLinkLength(Decimal64.valueOf("0.01"))
                .setLinkType(OpenroadmLinkType.XPONDEROUTPUT)
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService));
    }

    public static LinkBuilder createSrgToXpdr(String srcNode, String destNode, String srcTp, String destTp) {
        return createLinkBuilder(false, srcNode, destNode, srcTp, destTp,
            new Link1Builder()
                .setLinkLatency(Uint32.valueOf(0))
                .setLinkLength(Decimal64.valueOf("0.01"))
                .setLinkType(OpenroadmLinkType.XPONDERINPUT)
                .setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService));
    }

    public static Map<SupportingNodeKey, SupportingNode> geSupportingNodes() {
        SupportingNode supportingNode1 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 1"))
                .setNetworkRef(new NetworkId(StringConstants.CLLI_NETWORK))
                .build();
        SupportingNode supportingNode2 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 2"))
                .setNetworkRef(new NetworkId(StringConstants.OPENROADM_NETWORK))
                .build();
        return new HashMap<>(
            Map.of(
                supportingNode1.key(),supportingNode1,
                supportingNode2.key(),supportingNode2
            ));
    }

    public static NodeBuilder getNodeBuilder(Map<SupportingNodeKey,SupportingNode> supportingNodes1) {
        //update tp of nodes
        TerminationPoint xpdrNw =
            new TerminationPointBuilder()
                .withKey(new TerminationPointKey(new TpId("xpdrNWTXRX")))
                .addAugmentation(
                    new TerminationPoint1Builder()
                        .setTpType(OpenroadmTpType.XPONDERNETWORK)
                        .build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1Builder()
                        .setAdministrativeState(AdminStates.InService)
                        .setOperationalState(State.InService)
                        .build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                            .TerminationPoint1Builder()
                        .setXpdrNetworkAttributes(
                            new XpdrNetworkAttributesBuilder()
                                .setState(State.InService)
                                .build())
                        .build())
                .build();
        TerminationPoint xpdrClient =
            new TerminationPointBuilder()
                .withKey(new TerminationPointKey(new TpId("xpdrClientTXRX")))
                .addAugmentation(
                    new TerminationPoint1Builder()
                        .setTpType(OpenroadmTpType.XPONDERCLIENT)
                        .build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1Builder()
                        .setAdministrativeState(AdminStates.InService)
                        .setOperationalState(State.InService)
                        .build())
                .build();
        return new NodeBuilder()
                .setNodeId(new NodeId("XPDR1"))
                .withKey(new NodeKey(new NodeId("XPDR1")))
                .addAugmentation(
                    new Node1Builder()
                        .setTerminationPoint(Map.of(xpdrNw.key(),xpdrNw, xpdrClient.key(), xpdrClient))
                        .build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                        .setAdministrativeState(AdminStates.InService)
                        .setOperationalState(State.InService)
                        .build())
                .setSupportingNode(supportingNodes1);
    }

    public static NodeBuilder getDegNodeBuilder(Map<SupportingNodeKey, SupportingNode> supportingNodes, String nodeId) {
        // update tp of nodes
        TerminationPoint degTTP = new TerminationPointBuilder()
            .withKey(new TerminationPointKey(new TpId("DEG1-TTP-TXRX")))
            .addAugmentation(
                new TerminationPoint1Builder()
                    .setTpType(OpenroadmTpType.DEGREETXRXTTP)
                    .setAdministrativeState(AdminStates.InService)
                    .setOperationalState(State.InService)
                    .build())
            .build();
        TerminationPoint degCTP = new TerminationPointBuilder()
            .withKey(new TerminationPointKey(new TpId("DEG1-CTP-TXRX")))
            .addAugmentation(
                new TerminationPoint1Builder()
                    .setTpType(OpenroadmTpType.DEGREETXRXCTP)
                    .setAdministrativeState(AdminStates.InService)
                    .setOperationalState(State.InService)
                    .build())
            .build();
        return new NodeBuilder()
            .setNodeId(new NodeId(nodeId))
            .withKey(new NodeKey(new NodeId(nodeId)))
            .addAugmentation(
                new Node1Builder()
                    .setTerminationPoint(Map.of(degTTP.key(), degTTP, degCTP.key(), degCTP))
                .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                    .setOperationalState(State.InService).setAdministrativeState(AdminStates.InService).build())
            .setSupportingNode(supportingNodes);
    }

    public static NodeBuilder getSrgNodeBuilder(Map<SupportingNodeKey, SupportingNode> supportingNodes, String nodeId) {
        // update tp of nodes
        TerminationPoint srgPP = new TerminationPointBuilder()
            .withKey(new TerminationPointKey(new TpId("SRG1-PP-TXRX")))
            .addAugmentation(
                new TerminationPoint1Builder()
                    .setTpType(OpenroadmTpType.SRGTXRXPP)
                    .setAdministrativeState(AdminStates.InService)
                    .setOperationalState(State.InService)
                    .build())
            .build();
        TerminationPoint srgCP = new TerminationPointBuilder()
            .withKey(new TerminationPointKey(new TpId("SRG1-CP-TXRX")))
            .addAugmentation(
                new TerminationPoint1Builder()
                    .setTpType(OpenroadmTpType.SRGTXRXCP)
                    .setAdministrativeState(AdminStates.InService)
                    .setOperationalState(State.InService)
                    .build())
            .build();
        return new NodeBuilder()
            .setNodeId(new NodeId(nodeId))
            .withKey(new NodeKey(new NodeId(nodeId)))
            .addAugmentation(
                new Node1Builder()
                    .setTerminationPoint(Map.of(srgPP.key(), srgPP, srgCP.key(), srgCP)).build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                    .setOperationalState(State.InService).setAdministrativeState(AdminStates.InService).build())
            .setSupportingNode(supportingNodes);
    }

    // OTN network node
    public static List<SupportingNode> getOTNSupportingNodes() {
        return new ArrayList<>(
            List.of(
                new SupportingNodeBuilder()
                    .setNodeRef(new NodeId("node 1"))
                    .setNetworkRef(new NetworkId(StringConstants.CLLI_NETWORK))
                    .build(),
                new SupportingNodeBuilder()
                        .setNodeRef(new NodeId("node 2"))
                        .setNetworkRef(new NetworkId(StringConstants.OPENROADM_NETWORK))
                        .build()));
    }

    public static NodeBuilder getOTNNodeBuilder(Map<SupportingNodeKey,SupportingNode> supportingNodes1,
            OpenroadmTpType openroadmTpType) {
        TerminationPoint xpdr = getTerminationPointBuilder(openroadmTpType)
                .addAugmentation(getTerminationPoint1Builder(openroadmTpType).build())
                .addAugmentation(createAnother2TerminationPoint(openroadmTpType).build())
                .addAugmentation(createAnotherTerminationPoint(openroadmTpType).build())
                .build();
        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(new Node1Builder().setTerminationPoint(Map.of(xpdr.key(),xpdr)).build())
                .addAugmentation(getNode1())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build())
                .setSupportingNode(supportingNodes1);
    }

    public static NodeBuilder getOTNNodeBuilderEmpty(Map<SupportingNodeKey,SupportingNode> supportingNodes1,
            OpenroadmTpType openroadmTpType) {
        TerminationPoint xpdr = getTerminationPointBuilder(openroadmTpType)
            .addAugmentation(getTerminationPoint1Builder(openroadmTpType).build())
            .addAugmentation(createAnotherTerminationPoint(openroadmTpType).build())
            .build();
        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(
                    new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build())
                .addAugmentation(getNode1Empty())
                .setSupportingNode(supportingNodes1);
    }

    private static org.opendaylight.yang.gen.v1.http
            .org.openroadm.network.topology.rev230526.Node1 getNode1() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder()
                .setSrgAttributes(getSrgAttributes())
                .setDegreeAttributes(getDegAttributes())
                .build();
    }

    private static org.opendaylight.yang.gen.v1.http
            .org.openroadm.network.topology.rev230526.Node1 getNode1Empty() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder()
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

    private static org.opendaylight.yang.gen.v1.http
            .org.openroadm.common.network.rev230526.TerminationPoint1Builder getTerminationPoint1Builder(
                OpenroadmTpType openroadmTpType) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder()
                .setTpType(openroadmTpType);

    }

    private static org.opendaylight.yang.gen.v1.http
            .org.openroadm.network.topology.rev230526.TerminationPoint1Builder createAnotherTerminationPoint(
                OpenroadmTpType openroadmTpType) {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder()
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

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
            .TerminationPoint1Builder createOTNTerminationPoint(OpenroadmTpType openroadmTpType) {
        SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(IfOCHOTU4ODU4.VALUE)
                .build();
        SupportedInterfaceCapability supIfCapa1 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If100GEODU4.VALUE)
                .build();
        SupportedInterfaceCapability supIfCapa2 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If10GEODU2e.VALUE)
                .build();
        SupportedInterfaceCapability supIfCapa3 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If1GEODU0.VALUE)
                .build();
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                .TerminationPoint1Builder()
                .setTpSupportedInterfaces(
                    new TpSupportedInterfacesBuilder()
                        .setSupportedInterfaceCapability(
                            new HashMap<>(
                                Map.of(
                                    supIfCapa.key(),supIfCapa,
                                    supIfCapa1.key(),supIfCapa1,
                                    supIfCapa2.key(),supIfCapa2,
                                    supIfCapa3.key(),supIfCapa3))
                         )
                        .build()
                )
                .setXpdrTpPortConnectionAttributes(new XpdrTpPortConnectionAttributesBuilder().build());
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
            .TerminationPoint1Builder createAnother2TerminationPoint(OpenroadmTpType openroadmTpType) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                .TerminationPoint1Builder()
                .setOperationalState(State.InService)
                .setAdministrativeState(AdminStates.InService)
                .setTpType(openroadmTpType);
    }

    private NodeUtils() {
        throw new IllegalStateException("Utility class");
    }
}
