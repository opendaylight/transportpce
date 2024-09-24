/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util.test;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.PortQual;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.otn.tp.attributes.OdtuTpnPool;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.otn.tp.attributes.OdtuTpnPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPointBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetworkmodelTestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkmodelTestUtil.class);

    public static Nodes createMappingForRdm(String nodeId, String clli, int degNb, List<Integer> srgNbs) {
        Map<MappingKey,Mapping> mappingList = new HashMap<>();
        createDegreeMappings(mappingList, 1, degNb);
        for (Integer integer : srgNbs) {
            createSrgMappings(mappingList, integer.intValue());
        }
        return new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder().setNodeType(NodeTypes.Rdm).setNodeClli(clli).build())
            .setMapping(mappingList)
            .build();
    }

    public static Nodes createMappingForXpdr(
            String nodeId, String clli, int networkPortNb, int clientPortNb, XpdrNodeTypes xpdrNodeType) {
        Map<MappingKey,Mapping> mappingMap = new HashMap<>();
        createXpdrMappings(mappingMap, networkPortNb, clientPortNb, xpdrNodeType);
        Nodes mappingNode = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr).setNodeClli(clli).build())
            .setMapping(mappingMap)
            .build();
        LOG.info("mapping = {}", mappingNode.toString());
        return mappingNode;
    }

    public static List<Link> createSuppOTNLinks(OtnLinkType type, Uint32 availBW) {
        return new ArrayList<>(List.of(
            //linkAZ,
            new LinkBuilder()
                .setLinkId(new LinkId(type.getName() + "-SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1"))
                .setSource(
                    new SourceBuilder()
                        .setSourceNode(new NodeId("SPDRA-XPDR1"))
                        .setSourceTp(new TpId("XPDR1-NETWORK1")).build())
                .setDestination(
                    new DestinationBuilder()
                        .setDestNode(new NodeId("SPDRZ-XPDR1"))
                        .setDestTp(new TpId("XPDR1-NETWORK1")).build())
                .addAugmentation(
                    new Link1Builder()
                        .setLinkType(OpenroadmLinkType.OTNLINK)
                        .setOppositeLink(
                            new LinkId(type.getName() + "-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1"))
                        .build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                            .Link1Builder()
                        .setAvailableBandwidth(availBW)
                        .setUsedBandwidth(Uint32.valueOf(100000 - availBW.intValue()))
                        .build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923
                            .Link1Builder()
                        .setOtnLinkType(type)
                        .build())
                .build(),
            //linkZA
            new LinkBuilder()
                .setLinkId(new LinkId(type.getName() + "-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1"))
                .setSource(
                    new SourceBuilder()
                        .setSourceNode(new NodeId("SPDRZ-XPDR1"))
                        .setSourceTp(new TpId("XPDR1-NETWORK1"))
                        .build())
                .setDestination(
                    new DestinationBuilder()
                        .setDestNode(new NodeId("SPDRA-XPDR1"))
                        .setDestTp(new TpId("XPDR1-NETWORK1"))
                        .build())
                .addAugmentation(
                    new Link1Builder()
                        .setLinkType(OpenroadmLinkType.OTNLINK)
                        .setOppositeLink(
                            new LinkId(type.getName() + "-SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1"))
                        .build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                            .Link1Builder()
                        .setAvailableBandwidth(availBW)
                        .setUsedBandwidth(Uint32.valueOf(100000 - availBW.intValue()))
                        .build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923
                            .Link1Builder()
                        .setOtnLinkType(type)
                        .build())
                .build()
            ));
    }

    public static List<TerminationPoint> createTpList(boolean withTpnTsPool) {
        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder()
            .setRate(ODU4.VALUE);
        if (withTpnTsPool) {
            Set<Uint16> tsPool = new HashSet<>();
            for (int i = 0; i < 80; i++) {
                tsPool.add(Uint16.valueOf(i + 1));
            }
            xtpcaBldr.setTsPool(tsPool);
            Set<Uint16> tpnPool = new HashSet<>();
            for (int i = 1; i <= 80; i++) {
                tpnPool.add(Uint16.valueOf(i));
            }
            OdtuTpnPool odtuTpn =
                new OdtuTpnPoolBuilder().setOdtuType(ODTU4TsAllocated.VALUE).setTpnPool(tpnPool).build();
            xtpcaBldr.setOdtuTpnPool(ImmutableMap.of(odtuTpn.key(),odtuTpn));
        }
        SupportedInterfaceCapability supCapa =
            new SupportedInterfaceCapabilityBuilder().setIfCapType(IfOCHOTU4ODU4.VALUE).build();
        TerminationPoint1 otnTp1 = new TerminationPoint1Builder()
            .setTpSupportedInterfaces(
                new TpSupportedInterfacesBuilder()
                    .setSupportedInterfaceCapability(new HashMap<>(Map.of(supCapa.key(),supCapa)))
                    .build())
            .setXpdrTpPortConnectionAttributes(xtpcaBldr.build())
            .build();
        SupportingTerminationPoint supTermPointA = new SupportingTerminationPointBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNodeRef(new NodeId("SPDRA-XPDR1"))
            .setTpRef(new TpId("XPDR1-NETWORK1"))
            .build();
        SupportingTerminationPoint supTermPointZ = new SupportingTerminationPointBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNodeRef(new NodeId("SPDRZ-XPDR1"))
            .setTpRef(new TpId("XPDR1-NETWORK1"))
            .build();
        return new ArrayList<>(List.of(
            //tpA
            new TerminationPointBuilder()
                .setTpId(new TpId("XPDR1-NETWORK1"))
                .setSupportingTerminationPoint(Map.of(supTermPointA.key(), supTermPointA))
                .addAugmentation(otnTp1)
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1Builder()
                        .setTpType(OpenroadmTpType.XPONDERNETWORK)
                        .build())
                .build(),
            //tpZ
            new TerminationPointBuilder()
                .setTpId(new TpId("XPDR1-NETWORK1"))
                .setSupportingTerminationPoint(Map.of(supTermPointZ.key(), supTermPointZ))
                .addAugmentation(otnTp1)
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1Builder()
                        .setTpType(OpenroadmTpType.XPONDERNETWORK)
                        .build())
                .build()));
    }

    private static Map<MappingKey,Mapping> createDegreeMappings(
            Map<MappingKey,Mapping> mappingMap, int degNbStart, int degNbStop) {
        for (int i = degNbStart; i <= degNbStop; i++) {
            Mapping mapping = new MappingBuilder()
                .setLogicalConnectionPoint("DEG" + i + "-TTP-TXRX")
                .setPortDirection("bidirectional")
                .setSupportingPort("L1")
                .setSupportingCircuitPackName(i + "/0")
                .setSupportingOts("OTS-DEG" + i + "-TTP-TXRX")
                .setSupportingOms("OMS-DEG" + i + "-TTP-TXRX")
                .build();
            mappingMap.put(mapping.key(),mapping);
        }
        return mappingMap;
    }

    private static Map<MappingKey,Mapping> createSrgMappings(Map<MappingKey,Mapping> mappingMap, int srgNb) {
        for (int j = 1; j <= 4; j++) {
            Mapping mapping = new MappingBuilder()
                .setLogicalConnectionPoint("SRG" + srgNb + "-PP" + j + "-TXRX")
                .setPortDirection("bidirectional")
                .setSupportingPort("C" + j)
                .setSupportingCircuitPackName(3 + srgNb + "/0")
                .build();
            mappingMap.put(mapping.key(),mapping);
        }
        return mappingMap;
    }

    private static Map<MappingKey,Mapping> createXpdrMappings(
            Map<MappingKey,Mapping> mappingMap, int networkPortNb, int clientPortNb, XpdrNodeTypes xpdrNodeType) {
        for (int i = 1; i <= networkPortNb; i++) {
            MappingBuilder mappingBldr = new MappingBuilder()
                .setLogicalConnectionPoint("XPDR1-NETWORK" + i)
                .setPortDirection("bidirectional")
                .setSupportingPort("1")
                .setSupportedInterfaceCapability(new HashSet<>(Set.of(IfOCH.VALUE)))
                .setConnectionMapLcp("XPDR1-CLIENT" + i)
                .setPortQual(PortQual.XpdrNetwork.getName())
                .setSupportingCircuitPackName("1/0/" + i + "-PLUG-NET");
            if (xpdrNodeType != null) {
                mappingBldr.setXpdrType(xpdrNodeType);
            }
            Mapping mapping = mappingBldr.build();
            mappingMap.put(mapping.key(),mapping);
        }
        for (int i = 1; i <= clientPortNb; i++) {
            Mapping mapping = new MappingBuilder()
                .setLogicalConnectionPoint("XPDR1-CLIENT" + i)
                .setPortDirection("bidirectional")
                .setSupportingPort("C1")
                .setSupportedInterfaceCapability(new HashSet<>(Set.of(If100GE.VALUE)))
                .setConnectionMapLcp("XPDR1-NETWORK" + i)
                .setPortQual(PortQual.XpdrClient.getName())
                .setSupportingCircuitPackName("1/0/" + i + "-PLUG-CLIENT")
                .build();
            mappingMap.put(mapping.key(),mapping);
        }
        return mappingMap;
    }

    private NetworkmodelTestUtil() {
    }
}
