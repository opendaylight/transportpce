/*
 * Copyright © 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.dto.OtnTopoNode;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200128.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200128.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.OduRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.SwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.SwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.TpBandwidthSharing;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.TpBandwidthSharingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.XpdrAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.XpdrAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev181130.SwitchingPoolTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPointBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenRoadmOtnTopology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmOtnTopology.class);

    private OpenRoadmOtnTopology() {
    }

    public static TopologyShard createTopologyShard(Nodes mappingNode) {
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        Map<Integer, OtnTopoNode> xpdrMap = convertPortMappingToOtnNodeList(mappingNode);
        for (OtnTopoNode node : xpdrMap.values()) {
            switch (node.getNodeType()) {
                case Tpdr:
                    nodes.add(createTpdr(node));
                    break;
                case Mpdr:
                    nodes.add(createMuxpdr(node));
                    break;
                case Switch:
                    nodes.add(createSwitch(node));
                    break;
                default:
                    LOG.error("unknown otn node type {}", node.getNodeType().getName());
                    return null;
            }
        }
        return new TopologyShard(nodes, links);
    }

    private static Map<Integer, OtnTopoNode> convertPortMappingToOtnNodeList(Nodes mappingNode) {
        List<Mapping> networkMappings = mappingNode.getMapping().stream().filter(k -> k.getLogicalConnectionPoint()
            .contains("NETWORK")).collect(Collectors.toList());
        Map<Integer, OtnTopoNode> xpdrMap = new HashMap<>();
        for (Mapping mapping : networkMappings) {
            Integer xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
            if (!xpdrMap.containsKey(xpdrNb)) {
                List<Mapping> xpdrNetMaps = mappingNode.getMapping().stream().filter(k -> k.getLogicalConnectionPoint()
                    .contains("XPDR" + xpdrNb + "-NETWORK")).collect(Collectors.toList());
                List<Mapping> xpdrClMaps = mappingNode.getMapping().stream().filter(k -> k.getLogicalConnectionPoint()
                    .contains("XPDR" + xpdrNb + "-CLIENT")).collect(Collectors.toList());
                OtnTopoNode otnNode = null;
                if (mapping.getXponderType() != null) {
                    otnNode = new OtnTopoNode(mappingNode.getNodeId(), mappingNode.getNodeInfo().getNodeClli(), xpdrNb,
                        mapping.getXponderType(), fillConnectionMapLcp(xpdrNetMaps), fillConnectionMapLcp(xpdrClMaps));
                } else {
                    otnNode = new OtnTopoNode(mappingNode.getNodeId(), mappingNode.getNodeInfo().getNodeClli(), xpdrNb,
                        XpdrNodeTypes.Tpdr, fillConnectionMapLcp(xpdrNetMaps), fillConnectionMapLcp(xpdrClMaps));
                }
                xpdrMap.put(xpdrNb, otnNode);
            }
        }
        LOG.debug("there are {} xpdr to build", xpdrMap.size());
        xpdrMap.forEach((k, v) -> LOG.debug("xpdr {} = {} - {} - {} - {}", k, v.getNodeId(), v.getNodeType(),
            v.getNbTpClient(), v.getNbTpNetwork()));
        return xpdrMap;
    }

    private static Map<String, String> fillConnectionMapLcp(List<Mapping> mappingList) {
        Map<String, String> xpdrConnectionMap = new HashMap<>();
        for (Mapping map : mappingList) {
            if (map.getConnectionMapLcp() != null) {
                xpdrConnectionMap.put(map.getLogicalConnectionPoint(), map.getConnectionMapLcp());
            } else {
                xpdrConnectionMap.put(map.getLogicalConnectionPoint(), null);
            }
        }
        return xpdrConnectionMap;
    }

    private static Node createTpdr(OtnTopoNode node) {
        //create otn-topology node augmentation
        XpdrAttributes xpdrAttr = new XpdrAttributesBuilder()
            .setXpdrNumber(Integer.valueOf(node.getXpdrNb()))
            .build();
        Node1 otnNodeAug = new Node1Builder()
            .setXpdrAttributes(xpdrAttr)
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 ocnNodeAug =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.TPDR).build();
        //create ietf node augmentation to add TP list
        List<TerminationPoint> tpList = new ArrayList<>();
        // creation of tps
        createTP(tpList, node, OpenroadmTpType.XPONDERCLIENT, If100GE.class, false);
        createTP(tpList, node, OpenroadmTpType.XPONDERNETWORK, IfOCHOTU4ODU4.class, true);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpList)
            .build();

        //return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(Node1.class, otnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class,
                ocnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class, ietfNodeAug)
            .build();
    }

    private static Node createMuxpdr(OtnTopoNode node) {
        //create otn-topology node augmentation
        TpBandwidthSharing tpBwSh = new TpBandwidthSharingBuilder().build();    // to be completed
        XpdrAttributes xpdrAttr = new XpdrAttributesBuilder()
            .setXpdrNumber(Integer.valueOf(node.getXpdrNb()))
            .build();

        List<NonBlockingList> nblList = new ArrayList<>();
        for (int i = 1; i <= node.getNbTpClient(); i++) {
            List<TpId> tpList = new ArrayList<>();
            TpId tpId = new TpId("XPDR" + node.getXpdrNb() + "-CLIENT" + i);
            tpList.add(tpId);
            tpId = new TpId("XPDR" + node.getXpdrNb() + "-NETWORK1");
            tpList.add(tpId);
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(i)
                .setTpList(tpList)
                .setAvailableInterconnectBandwidth(Long.valueOf(node.getNbTpNetwork() * 10))
                .setInterconnectBandwidthUnit(Long.valueOf(1000000000))
                .build();
            nblList.add(nbl);
        }
        OduSwitchingPools oduSwitchPool = new OduSwitchingPoolsBuilder()
            .setSwitchingPoolNumber(Integer.valueOf(1))
            .setSwitchingPoolType(SwitchingPoolTypes.NonBlocking)
            .setNonBlockingList(nblList)
            .build();
        List<OduSwitchingPools> oduSwitchPoolList = new ArrayList<>();
        oduSwitchPoolList.add(oduSwitchPool);
        SwitchingPools switchingPools = new SwitchingPoolsBuilder()
            .setOduSwitchingPools(oduSwitchPoolList)
            .build();
        Node1 otnNodeAug = new Node1Builder()
            .setTpBandwidthSharing(tpBwSh)
            .setXpdrAttributes(xpdrAttr)
            .setSwitchingPools(switchingPools)
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 ocnNodeAug =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.MUXPDR).build();

        //create ietf node augmentation to add TP list
        List<TerminationPoint> tpList = new ArrayList<>();
        // creation of tps
        createTP(tpList, node, OpenroadmTpType.XPONDERCLIENT, If10GEODU2e.class, true);
        createTP(tpList, node, OpenroadmTpType.XPONDERNETWORK, IfOCHOTU4ODU4.class, true);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpList)
            .build();

        //return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(Node1.class, otnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class,
                ocnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class, ietfNodeAug)
            .build();
    }

    private static Node createSwitch(OtnTopoNode node) {
        List<TpId> tpl = new ArrayList<>();
        TpId tpId = null;
        for (int i = 1; i <= node.getNbTpClient(); i++) {
            tpId = new TpId("XPDR" + node.getXpdrNb() + "-CLIENT" + i);
            tpl.add(tpId);
        }
        for (int i = 1; i <= node.getNbTpNetwork(); i++) {
            tpId = new TpId("XPDR" + node.getXpdrNb() + "-NETWORK" + i);
            tpl.add(tpId);
        }
        List<NonBlockingList> nblList = new ArrayList<>();
        NonBlockingList nbl = new NonBlockingListBuilder()
            .setNblNumber(Integer.valueOf(1))
            .setTpList(tpl)
            .build();
        nblList.add(nbl);

        OduSwitchingPools oduSwitchPool = new OduSwitchingPoolsBuilder()
            .setSwitchingPoolNumber(Integer.valueOf(1))
            .setSwitchingPoolType(SwitchingPoolTypes.NonBlocking)
            .setNonBlockingList(nblList)
            .build();
        List<OduSwitchingPools> oduSwitchPoolList = new ArrayList<>();
        oduSwitchPoolList.add(oduSwitchPool);
        SwitchingPools switchingPools = new SwitchingPoolsBuilder()
            .setOduSwitchingPools(oduSwitchPoolList)
            .build();

        //create otn-topology node augmentation
        TpBandwidthSharing tpBwSh = new TpBandwidthSharingBuilder().build();    // to be completed
        XpdrAttributes xpdrAttr = new XpdrAttributesBuilder()
            .setXpdrNumber(Integer.valueOf(node.getXpdrNb()))
            .build();

        Node1 otnNodeAug = new Node1Builder()
            .setTpBandwidthSharing(tpBwSh)
            .setXpdrAttributes(xpdrAttr)
            .setSwitchingPools(switchingPools)
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 ocnNodeAug =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.SWITCH).build();

        //create ietf node augmentation to add TP list
        List<TerminationPoint> tpList = new ArrayList<>();
        // creation of tps
        createTP(tpList, node, OpenroadmTpType.XPONDERCLIENT, If100GEODU4.class, true);
        createTP(tpList, node, OpenroadmTpType.XPONDERNETWORK, IfOCHOTU4ODU4.class, true);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpList)
            .build();

        //return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(Node1.class, otnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class,
                ocnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class, ietfNodeAug)
            .build();
    }

    private static void createTP(List<TerminationPoint> tpList, OtnTopoNode node, OpenroadmTpType tpType,
        Class<? extends SupportedIfCapability> ifCapType, boolean withRate) {
        long nbTps = 0;
        if (OpenroadmTpType.XPONDERCLIENT.equals(tpType)) {
            nbTps = node.getNbTpClient();
        } else if (OpenroadmTpType.XPONDERNETWORK.equals(tpType)) {
            nbTps = node.getNbTpNetwork();
        } else {
            LOG.warn("Wrong tp-type {}, cannot create tp {}", tpType, tpType.getName());
        }

        for (int i = 1; i <= nbTps; i++) {
            //openroadm-otn-topoology augmentation
            SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(ifCapType)
                .build();
            List<SupportedInterfaceCapability> supIfCapaList = new ArrayList<>();
            supIfCapaList.add(supIfCapa);
            TpSupportedInterfaces tpSupIf = new TpSupportedInterfacesBuilder()
                .setSupportedInterfaceCapability(supIfCapaList)
                .build();

            XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder();
            if (withRate) {
                xtpcaBldr.setRate(fixRate(ifCapType));
            }
            TerminationPoint1 otnTp1 = new TerminationPoint1Builder()
                .setTpSupportedInterfaces(tpSupIf)
                .setXpdrTpPortConnectionAttributes(xtpcaBldr.build())
                .build();
            org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1Builder tpceTp1Bldr =
                new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1Builder();
            if (OpenroadmTpType.XPONDERNETWORK.equals(tpType)) {
                TpId tpId = new TpId("XPDR" + node.getXpdrNb() + "-NETWORK" + i);
                if (node.getXpdrNetConnectionMap().get(tpId.getValue()) != null) {
                    tpceTp1Bldr.setAssociatedConnectionMapPort(node.getXpdrNetConnectionMap().get(tpId.getValue()));
                }
                SupportingTerminationPoint stp = new SupportingTerminationPointBuilder()
                    .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
                    .setNodeRef(new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb()))
                    .setTpRef("XPDR" + node.getXpdrNb() + "-NETWORK" + i)
                    .build();
                List<SupportingTerminationPoint> supportTpList = new ArrayList<>();
                supportTpList.add(stp);
                tpList.add(buildIetfTp(tpceTp1Bldr, otnTp1, tpType, tpId, supportTpList));
            } else if (OpenroadmTpType.XPONDERCLIENT.equals(tpType)) {
                TpId tpId = new TpId("XPDR" + node.getXpdrNb() + "-CLIENT" + i);
                if (node.getXpdrCliConnectionMap().get(tpId.getValue()) != null) {
                    tpceTp1Bldr.setAssociatedConnectionMapPort(node.getXpdrCliConnectionMap().get(tpId.getValue()));
                }
                tpList.add(buildIetfTp(tpceTp1Bldr, otnTp1, tpType, tpId, null));
            }
        }
    }

    private static Class<? extends OduRateIdentity> fixRate(Class<? extends SupportedIfCapability> ifCapaType) {
        switch (ifCapaType.getSimpleName()) {
            case "If100GEODU4":
            case "IfOCHOTU4ODU4":
                return ODU4.class;
            case "If1GEODU0":
                return ODU0.class;
            case "If10GEODU2e":
                return ODU2e.class;
            default:
                return null;
        }
    }

    private static List<SupportingNode> createSupportingNodes(OtnTopoNode node) {
        SupportingNode suppNode1 = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .setNodeRef(new NodeId(node.getNodeId()))
            .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID),
                new NodeId(node.getNodeId())))
            .build();
        SupportingNode suppNode2 = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNodeRef(new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb()))
            .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID),
                new NodeId(node.getNodeId() + "-XPDR" + node.getXpdrNb())))
            .build();
        SupportingNode suppNode3 = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
            .setNodeRef(new NodeId(node.getClli()))
            .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID),
                new NodeId(node.getClli())))
            .build();
        List<SupportingNode> suppNodeList = new ArrayList<>();
        suppNodeList.add(suppNode1);
        suppNodeList.add(suppNode2);
        suppNodeList.add(suppNode3);
        return suppNodeList;
    }

    private static TerminationPoint buildIetfTp(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129
        .TerminationPoint1Builder tpceTp1Bldr, TerminationPoint1 otnTp1, OpenroadmTpType tpType, TpId tpId,
        List<SupportingTerminationPoint> supportTpList) {

        TerminationPointBuilder ietfTpBldr = new TerminationPointBuilder();
        if (tpceTp1Bldr.getAssociatedConnectionMapPort() != null) {
            ietfTpBldr.addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129
                .TerminationPoint1.class, tpceTp1Bldr.build());
        }
        if (supportTpList != null) {
            ietfTpBldr.setSupportingTerminationPoint(supportTpList);
        }
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1 ocnTp =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1Builder().setTpType(tpType).build();

        ietfTpBldr.setTpId(tpId)
            .withKey(new TerminationPointKey(tpId))
            .addAugmentation(TerminationPoint1.class, otnTp1)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1.class, ocnTp);
        return ietfTpBldr.build();
    }
}
