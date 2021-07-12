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
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.PortQual;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev200529.xpdr.otn.tp.attributes.OdtuTpnPool;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev200529.xpdr.otn.tp.attributes.OdtuTpnPoolBuilder;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.OtnLinkType;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPointKey;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetworkmodelTestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkmodelTestUtil.class);

    public static Nodes createMappingForRdm(String nodeId, String clli, int degNb, int srgNb) {
        Map<MappingKey,Mapping> mappingList = new HashMap<>();
        createDegreeMappings(mappingList, 1, degNb);
        createSrgMappings(mappingList, 1, srgNb);
        Nodes mappingNode = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder().setNodeType(NodeTypes.Rdm).setNodeClli(clli).build())
            .setMapping(mappingList)
            .build();
        return mappingNode;
    }

    public static Nodes createMappingForXpdr(String nodeId, String clli, int networkPortNb, int clientPortNb,
        XpdrNodeTypes xpdrNodeType) {
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
        Link linkAZ = new LinkBuilder()
            .setLinkId(new LinkId(type.getName() + "-SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1"))
            .setSource(new SourceBuilder()
                    .setSourceNode(new NodeId("SPDRA-XPDR1"))
                    .setSourceTp("XPDR1-NETWORK1").build())
            .setDestination(new DestinationBuilder()
                    .setDestNode(new NodeId("SPDRZ-XPDR1"))
                    .setDestTp("XPDR1-NETWORK1").build())
            .addAugmentation(
                new Link1Builder()
                    .setLinkType(OpenroadmLinkType.OTNLINK)
                    .setOppositeLink(new LinkId(type.getName()
                        + "-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1"))
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Link1Builder()
                    .setAvailableBandwidth(availBW)
                    .setUsedBandwidth(Uint32.valueOf(100000 - availBW.intValue()))
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.Link1Builder()
                    .setOtnLinkType(type)
                    .build())
            .build();
        Link linkZA = new LinkBuilder()
            .setLinkId(new LinkId(type.getName() + "-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1"))
            .setSource(new SourceBuilder()
                    .setSourceNode(new NodeId("SPDRZ-XPDR1"))
                    .setSourceTp("XPDR1-NETWORK1").build())
            .setDestination(new DestinationBuilder()
                    .setDestNode(new NodeId("SPDRA-XPDR1"))
                    .setDestTp("XPDR1-NETWORK1").build())
            .addAugmentation(
                new Link1Builder()
                    .setLinkType(OpenroadmLinkType.OTNLINK)
                    .setOppositeLink(new LinkId(type.getName()
                        + "-SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1"))
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Link1Builder()
                    .setAvailableBandwidth(availBW)
                    .setUsedBandwidth(Uint32.valueOf(100000 - availBW.intValue()))
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.Link1Builder()
                    .setOtnLinkType(type)
                    .build())
            .build();
        List<Link> links = new ArrayList<>();
        links.add(linkAZ);
        links.add(linkZA);
        return links;
    }

    public static List<TerminationPoint> createTpList(boolean withTpnTsPool) {
        SupportedInterfaceCapability supCapa = new SupportedInterfaceCapabilityBuilder()
            .setIfCapType(IfOCHOTU4ODU4.class)
            .build();
        Map<SupportedInterfaceCapabilityKey,SupportedInterfaceCapability> supInterCapaList =
                new HashMap<>();
        supInterCapaList.put(supCapa.key(),supCapa);
        TpSupportedInterfaces tpSuppInter = new TpSupportedInterfacesBuilder()
            .setSupportedInterfaceCapability(supInterCapaList)
            .build();
        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder()
            .setRate(ODU4.class);
        if (withTpnTsPool) {
            List<Uint16> tsPool = new ArrayList<>();
            for (int i = 0; i < 80; i++) {
                tsPool.add(Uint16.valueOf(i + 1));
            }
            xtpcaBldr.setTsPool(tsPool);
            List<Uint16> tpnPool = new ArrayList<>();
            for (int i = 1; i <= 80; i++) {
                tpnPool.add(Uint16.valueOf(i));
            }
            OdtuTpnPool odtuTpn = new OdtuTpnPoolBuilder()
                    .setOdtuType(ODTU4TsAllocated.class)
                    .setTpnPool(tpnPool).build();
            xtpcaBldr.setOdtuTpnPool(ImmutableMap.of(odtuTpn.key(),odtuTpn));
        }
        TerminationPoint1 otnTp1 = new TerminationPoint1Builder()
            .setTpSupportedInterfaces(tpSuppInter)
            .setXpdrTpPortConnectionAttributes(xtpcaBldr.build())
            .build();
        SupportingTerminationPoint supTermPointA = new SupportingTerminationPointBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNodeRef(new NodeId("SPDRA-XPDR1"))
            .setTpRef("XPDR1-NETWORK1")
            .build();
        Map<SupportingTerminationPointKey,SupportingTerminationPoint> supTermPointMapA =
                Map.of(supTermPointA.key(), supTermPointA);
        TerminationPoint tpA = new TerminationPointBuilder()
            .setTpId(new TpId("XPDR1-NETWORK1"))
            .setSupportingTerminationPoint(supTermPointMapA)
            .addAugmentation(otnTp1)
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder()
                .setTpType(OpenroadmTpType.XPONDERNETWORK)
                .build())
            .build();
        SupportingTerminationPoint supTermPointZ = new SupportingTerminationPointBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNodeRef(new NodeId("SPDRZ-XPDR1"))
            .setTpRef("XPDR1-NETWORK1")
            .build();
        Map<SupportingTerminationPointKey,SupportingTerminationPoint> supTermPointMapZ =
                Map.of(supTermPointZ.key(), supTermPointZ);
        TerminationPoint tpZ = new TerminationPointBuilder()
            .setTpId(new TpId("XPDR1-NETWORK1"))
            .setSupportingTerminationPoint(supTermPointMapZ)
            .addAugmentation(otnTp1)
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder()
                .setTpType(OpenroadmTpType.XPONDERNETWORK)
                .build())
            .build();
        List<TerminationPoint> tps = new ArrayList<>();
        tps.add(tpA);
        tps.add(tpZ);
        return tps;
    }

    private static Map<MappingKey,Mapping> createDegreeMappings(Map<MappingKey,Mapping> mappingMap,
            int degNbStart, int degNbStop) {
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

    private static Map<MappingKey,Mapping> createSrgMappings(Map<MappingKey,Mapping> mappingMap,
            int srgNbStart, int srgNbStop) {
        for (int i = srgNbStart; i <= srgNbStop; i++) {
            for (int j = 1; j <= 4; j++) {
                Mapping mapping = new MappingBuilder()
                    .setLogicalConnectionPoint("SRG" + i + "-PP" + j + "-TXRX")
                    .setPortDirection("bidirectional")
                    .setSupportingPort("C" + j)
                    .setSupportingCircuitPackName(3 + i + "/0")
                    .build();
                mappingMap.put(mapping.key(),mapping);
            }
        }
        return mappingMap;
    }

    private static Map<MappingKey,Mapping> createXpdrMappings(Map<MappingKey,Mapping> mappingMap,
            int networkPortNb, int clientPortNb,
        XpdrNodeTypes xpdrNodeType) {
        for (int i = 1; i <= networkPortNb; i++) {
            List<Class<? extends org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327
                .SupportedIfCapability>> supportedIntf = new ArrayList<>();
            supportedIntf.add(org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOCH.class);
            MappingBuilder mappingBldr = new MappingBuilder()
                .setLogicalConnectionPoint("XPDR1-NETWORK" + i)
                .setPortDirection("bidirectional")
                .setSupportingPort("1")
                .setSupportedInterfaceCapability(supportedIntf)
                .setConnectionMapLcp("XPDR1-CLIENT" + i)
                .setPortQual(PortQual.XpdrNetwork.getName())
                .setSupportingCircuitPackName("1/0/" + i + "-PLUG-NET");
            if (xpdrNodeType != null) {
                mappingBldr.setXponderType(xpdrNodeType);
            }
            Mapping mapping = mappingBldr.build();
            mappingMap.put(mapping.key(),mapping);
        }
        for (int i = 1; i <= clientPortNb; i++) {
            List<Class<? extends org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327
                .SupportedIfCapability>> supportedIntf = new ArrayList<>();
            supportedIntf.add(org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If100GE.class);
            Mapping mapping = new MappingBuilder()
                .setLogicalConnectionPoint("XPDR1-CLIENT" + i)
                .setPortDirection("bidirectional")
                .setSupportingPort("C1")
                .setSupportedInterfaceCapability(supportedIntf)
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
