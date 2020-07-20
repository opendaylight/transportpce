/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util.test;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200714.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.PortQual;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181019.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181019.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181019.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev181130.xpdr.otn.tp.attributes.OdtuTpnPoolBuilder;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.OtnLinkType;
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

    public static Nodes createMappingForRdm(String nodeId, String clli, int degNb, int srgNb) {
        List<Mapping> mappingList = new ArrayList<>();
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
        List<Mapping> mappingList = new ArrayList<>();
        createXpdrMappings(mappingList, networkPortNb, clientPortNb, xpdrNodeType);
        Nodes mappingNode = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr).setNodeClli(clli).build())
            .setMapping(mappingList)
            .build();
        LOG.info("mapping = {}", mappingNode.toString());
        return mappingNode;
    }

    public static List<Link> createSuppOTNLinks(OtnLinkType type, int availBW) {
        String prefix = null;
        if (OtnLinkType.OTU4.equals(type)) {
            prefix = "OTU4-";
        } else if (OtnLinkType.ODTU4.equals(type)) {
            prefix = "ODU4-";
        }
        Link linkAZ = new LinkBuilder()
            .setLinkId(new LinkId(prefix + "SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1"))
            .setSource(new SourceBuilder()
                    .setSourceNode(new NodeId("SPDRA-XPDR1"))
                    .setSourceTp("XPDR1-NETWORK1").build())
            .setDestination(new DestinationBuilder()
                    .setDestNode(new NodeId("SPDRZ-XPDR1"))
                    .setDestTp("XPDR1-NETWORK1").build())
            .addAugmentation(
                Link1.class,
                new Link1Builder()
                    .setLinkType(OpenroadmLinkType.OTNLINK)
                    .setOppositeLink(new LinkId(prefix + "SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1"))
                    .build())
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1.class,
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1Builder()
                    .setAvailableBandwidth(Uint32.valueOf(availBW))
                    .setUsedBandwidth(Uint32.valueOf(100000 - availBW))
                    .build())
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1.class,
                new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1Builder()
                    .setOtnLinkType(type)
                    .build())
            .build();
        Link linkZA = new LinkBuilder()
            .setLinkId(new LinkId(prefix + "SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1"))
            .setSource(new SourceBuilder()
                    .setSourceNode(new NodeId("SPDRZ-XPDR1"))
                    .setSourceTp("XPDR1-NETWORK1").build())
            .setDestination(new DestinationBuilder()
                    .setDestNode(new NodeId("SPDRA-XPDR1"))
                    .setDestTp("XPDR1-NETWORK1").build())
            .addAugmentation(
                Link1.class,
                new Link1Builder()
                    .setLinkType(OpenroadmLinkType.OTNLINK)
                    .setOppositeLink(new LinkId(prefix + "SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1"))
                    .build())
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1.class,
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1Builder()
                    .setAvailableBandwidth(Uint32.valueOf(availBW))
                    .setUsedBandwidth(Uint32.valueOf(100000 - availBW))
                    .build())
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1.class,
                new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1Builder()
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
        List<SupportedInterfaceCapability> supInterCapaList = new ArrayList<>();
        supInterCapaList.add(supCapa);
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
            xtpcaBldr.setOdtuTpnPool(
                    ImmutableList.of(
                        new OdtuTpnPoolBuilder().setOdtuType(ODTU4TsAllocated.class).setTpnPool(tpnPool).build()));
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
        List<SupportingTerminationPoint> supTermPointListA = new ArrayList<>();
        supTermPointListA.add(supTermPointA);
        TerminationPoint tpA = new TerminationPointBuilder()
            .setTpId(new TpId("XPDR1-NETWORK1"))
            .setSupportingTerminationPoint(supTermPointListA)
            .addAugmentation(TerminationPoint1.class, otnTp1)
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class,
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder()
                .setTpType(OpenroadmTpType.XPONDERNETWORK)
                .build())
            .build();
        SupportingTerminationPoint supTermPointZ = new SupportingTerminationPointBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNodeRef(new NodeId("SPDRZ-XPDR1"))
            .setTpRef("XPDR1-NETWORK1")
            .build();
        List<SupportingTerminationPoint> supTermPointListZ = new ArrayList<>();
        supTermPointListZ.add(supTermPointZ);
        TerminationPoint tpZ = new TerminationPointBuilder()
            .setTpId(new TpId("XPDR1-NETWORK1"))
            .setSupportingTerminationPoint(supTermPointListZ)
            .addAugmentation(TerminationPoint1.class, otnTp1)
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class,
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder()
                .setTpType(OpenroadmTpType.XPONDERNETWORK)
                .build())
            .build();
        List<TerminationPoint> tps = new ArrayList<>();
        tps.add(tpA);
        tps.add(tpZ);
        return tps;
    }

    private static List<Mapping> createDegreeMappings(List<Mapping> mappingList, int degNbStart, int degNbStop) {
        for (int i = degNbStart; i <= degNbStop; i++) {
            Mapping mapping = new MappingBuilder()
                .setLogicalConnectionPoint("DEG" + i + "-TTP-TXRX")
                .setPortDirection("bidirectional")
                .setSupportingPort("L1")
                .setSupportingCircuitPackName(i + "/0")
                .setSupportingOts("OTS-DEG" + i + "-TTP-TXRX")
                .setSupportingOms("OMS-DEG" + i + "-TTP-TXRX")
                .build();
            mappingList.add(mapping);
        }
        return mappingList;
    }

    private static List<Mapping> createSrgMappings(List<Mapping> mappingList, int srgNbStart, int srgNbStop) {
        for (int i = srgNbStart; i <= srgNbStop; i++) {
            for (int j = 1; j <= 4; j++) {
                Mapping mapping = new MappingBuilder()
                    .setLogicalConnectionPoint("SRG" + i + "-PP" + j + "-TXRX")
                    .setPortDirection("bidirectional")
                    .setSupportingPort("C" + j)
                    .setSupportingCircuitPackName(3 + i + "/0")
                    .build();
                mappingList.add(mapping);
            }
        }
        return mappingList;
    }

    private static List<Mapping> createXpdrMappings(List<Mapping> mappingList, int networkPortNb, int clientPortNb,
        XpdrNodeTypes xpdrNodeType) {
        for (int i = 1; i <= networkPortNb; i++) {
            List<Class<? extends SupportedIfCapability>> supportedIntf = new ArrayList<>();
            supportedIntf.add(IfOCH.class);
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
            mappingList.add(mappingBldr.build());
        }
        for (int i = 1; i <= clientPortNb; i++) {
            List<Class<? extends SupportedIfCapability>> supportedIntf = new ArrayList<>();
            supportedIntf.add(If100GE.class);
            Mapping mapping = new MappingBuilder()
                .setLogicalConnectionPoint("XPDR1-CLIENT" + i)
                .setPortDirection("bidirectional")
                .setSupportingPort("C1")
                .setSupportedInterfaceCapability(supportedIntf)
                .setConnectionMapLcp("XPDR1-NETWORK" + i)
                .setPortQual(PortQual.XpdrClient.getName())
                .setSupportingCircuitPackName("1/0/" + i + "-PLUG-CLIENT")
                .build();
            mappingList.add(mapping);
        }
        return mappingList;
    }

    private NetworkmodelTestUtil() {
    }
}
