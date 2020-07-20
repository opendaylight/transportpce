/*
 * Copyright © 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.dto.OtnTopoNode;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.OduRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1Builder;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev181130.xpdr.otn.tp.attributes.OdtuTpnPoolBuilder;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.OtnLinkType;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPointBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenRoadmOtnTopology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmOtnTopology.class);
    private static final String CLIENT = "-CLIENT";
    private static final String NETWORK = "-NETWORK";
    private static final String XPDR = "-XPDR";
    private static final int NB_TRIB_PORTS = 80;
    private static final int NB_TRIB_SLOTS = 80;
    private static final int NB_TRIB_SLOT_PER_10GE = 8;

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

    public static TopologyShard createOtnLinks(String nodeA, String tpA, String nodeZ, String tpZ,
        OtnLinkType linkType) {
        List<Link> links = null;
        if (OtnLinkType.OTU4.equals(linkType)) {
            links = initialiseOtnLinks(nodeA, tpA, nodeZ, tpZ, linkType, "OTU4");
        }
        return new TopologyShard(null, links);
    }

    public static TopologyShard createOtnLinks(List<Link> suppOtu4Links, List<TerminationPoint> oldTps) {
        List<Link> links = new ArrayList<>();
        for (Link link : suppOtu4Links) {
            if (link.augmentation(Link1.class) != null
                && link.augmentation(Link1.class).getAvailableBandwidth().equals(Uint32.valueOf(100000))) {
                links.add(updateOtnLinkBwParameters(link, 0L, 100000L));
            } else {
                LOG.error("Error with otn parameters of supported link {}", link.getLinkId().getValue());
            }
        }
        if (links.size() == 2) {
            links.addAll(initialiseOtnLinks(suppOtu4Links.get(0).getSource().getSourceNode().getValue(),
                suppOtu4Links.get(0).getSource().getSourceTp().toString(),
                suppOtu4Links.get(0).getDestination().getDestNode().getValue(),
                suppOtu4Links.get(0).getDestination().getDestTp().toString(),
                OtnLinkType.ODTU4, "ODU4"));
        }
        List<TerminationPoint> tps = new ArrayList<>();
        for (TerminationPoint tp : oldTps) {
            tps.add(updateTp(tp, true));
        }
        if (links.size() == 4 && tps.size() == 2) {
            return new TopologyShard(null, links, tps);
        } else {
            return new TopologyShard(null, null, null);
        }
    }

    public static TopologyShard updateOtnLinks(List<Link> suppOdu4Links, List<TerminationPoint> oldTps,
        String serviceRate, Short tribPortNb, Short tribSoltNb, boolean isDeletion) {
        List<Link> links = new ArrayList<>();
        Long bwIncr = 10000L;
        if ("1G".equals(serviceRate)) {
            bwIncr = 1000L;
        }
        for (Link link : suppOdu4Links) {
            if (link.augmentation(Link1.class) != null && link.augmentation(Link1.class).getAvailableBandwidth() != null
                && link.augmentation(Link1.class).getUsedBandwidth() != null) {
                Uint32 avlBw = link.augmentation(Link1.class).getAvailableBandwidth();
                Uint32 usedBw = link.augmentation(Link1.class).getUsedBandwidth();
                if (avlBw.toJava() < bwIncr) {
                    bwIncr = 0L;
                }
                if (isDeletion) {
                    links.add(updateOtnLinkBwParameters(link, avlBw.toJava() + bwIncr,
                        usedBw.toJava() - bwIncr));
                } else {
                    links.add(updateOtnLinkBwParameters(link, avlBw.toJava() - bwIncr,
                        usedBw.toJava() + bwIncr));
                }
            } else {
                LOG.error("Error with otn parameters of supported link {}", link.getLinkId().getValue());
            }
        }
        List<TerminationPoint> tps = new ArrayList<>();
        for (TerminationPoint tp : oldTps) {
            if (bwIncr != 0) {
                tps.add(updateNodeTpTsPool(tp, serviceRate, tribPortNb, tribSoltNb, isDeletion));
            }
        }
        if (!links.isEmpty() && !tps.isEmpty()) {
            return new TopologyShard(null, links, tps);
        } else {
            LOG.error("unable to update otn links");
            return new TopologyShard(null, null, null);
        }
    }

    public static TopologyShard deleteOtnLinks(List<Link> suppOtu4Links, List<TerminationPoint> oldTps) {
        List<Link> links = new ArrayList<>();
        for (Link link : suppOtu4Links) {
            if (link.augmentation(Link1.class) != null) {
                links.add(updateOtnLinkBwParameters(link, 100000L, 0L));
            } else {
                LOG.error("Error with otn parameters of supported link {}", link.getLinkId().getValue());
            }
        }
        List<TerminationPoint> tps = new ArrayList<>();
        for (TerminationPoint tp : oldTps) {
            tps.add(updateTp(tp, false));
        }
        if (links.size() == 2 && tps.size() == 2) {
            return new TopologyShard(null, links, tps);
        } else {
            return new TopologyShard(null, null, null);
        }
    }

    private static List<Link> initialiseOtnLinks(String nodeA, String tpA, String nodeZ, String tpZ,
        OtnLinkType linkType, String linkIdPrefix) {
        List<Link> links = new ArrayList<>();
        org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1 tpceLink1
            = new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1Builder()
            .setOtnLinkType(linkType).build();
        Link1 otnLink1 = new Link1Builder()
            .setAvailableBandwidth(Uint32.valueOf(100000))
            .setUsedBandwidth(Uint32.valueOf(0))
            .build();
        // create link A-Z
        String nodeATopo;
        String nodeZTopo;
        if (nodeA.contains(XPDR) && nodeZ.contains(XPDR)) {
            nodeATopo = nodeA;
            nodeZTopo = nodeZ;
        } else {
            nodeATopo = nodeA + "-" + tpA.split("-")[0];
            nodeZTopo = nodeZ + "-" + tpZ.split("-")[0];
        }
        LinkBuilder ietfLinkAZBldr = TopologyUtils.createLink(nodeATopo, nodeZTopo, tpA, tpZ, linkIdPrefix);
        ietfLinkAZBldr
            .addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1.class, tpceLink1)
            .addAugmentation(Link1.class, otnLink1)
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class,
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder(
                        ietfLinkAZBldr.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class))
                    .setLinkType(OpenroadmLinkType.OTNLINK)
                    .build());
        links.add(ietfLinkAZBldr.build());
        // create link Z-A
        LinkBuilder ietfLinkZABldr = TopologyUtils.createLink(nodeZTopo, nodeATopo, tpZ, tpA, linkIdPrefix);
        ietfLinkZABldr
            .addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1.class, tpceLink1)
            .addAugmentation(Link1.class, otnLink1)
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class,
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder(
                        ietfLinkZABldr.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class))
                    .setLinkType(OpenroadmLinkType.OTNLINK)
                    .build());
        links.add(ietfLinkZABldr.build());
        return links;
    }

    private static Link updateOtnLinkBwParameters(Link link, Long availBw, Long usedBw) {
        LOG.debug("in updateOtnLinkBwParameters with availBw = {}, usedBw = {}", availBw, usedBw);
        LinkBuilder updatedLinkBldr = new LinkBuilder(link);
        Link1Builder updatedLink1Bldr = new Link1Builder(link.augmentation(Link1.class))
            .setAvailableBandwidth(Uint32.valueOf(availBw))
            .setUsedBandwidth(Uint32.valueOf(usedBw));
        updatedLinkBldr.addAugmentation(Link1.class, updatedLink1Bldr.build());
        return updatedLinkBldr.build();
    }

    private static TerminationPoint updateTp(TerminationPoint originalTp, boolean addingTsTpnPoolTermination) {
        LOG.debug("in updateTp");
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(originalTp);
        TerminationPoint1Builder otnTp1Bldr = new TerminationPoint1Builder(
            tpBldr.augmentation(TerminationPoint1.class));
        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder(otnTp1Bldr
            .getXpdrTpPortConnectionAttributes());
        if (addingTsTpnPoolTermination) {
            List<Uint16> tsPool = new ArrayList<>();
            for (int i = 0; i < NB_TRIB_SLOTS; i++) {
                tsPool.add(Uint16.valueOf(i + 1));
            }
            xtpcaBldr.setTsPool(tsPool);
            List<Uint16> tpnPool = new ArrayList<>();
            for (int i = 1; i <= NB_TRIB_PORTS; i++) {
                tpnPool.add(Uint16.valueOf(i));
            }
            xtpcaBldr.setOdtuTpnPool(ImmutableList.of(new OdtuTpnPoolBuilder().setOdtuType(ODTU4TsAllocated.class)
                .setTpnPool(tpnPool).build()));
        } else {
            xtpcaBldr.setTsPool(null);
            xtpcaBldr.setOdtuTpnPool(null);
        }
        return tpBldr.addAugmentation(TerminationPoint1.class,
            otnTp1Bldr.setXpdrTpPortConnectionAttributes(xtpcaBldr.build()).build()).build();
    }

    private static TerminationPoint updateNodeTpTsPool(TerminationPoint tp, String serviceRate, Short tribPortNb,
        Short tribSlotNb, boolean isDeletion) {
        LOG.debug("in updateNodeTpTsPool");
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(tp);
        @Nullable
        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder(
            tpBldr.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes());
        List<Uint16> tsPool = new ArrayList<>(xtpcaBldr.getTsPool());
        switch (serviceRate) {
            case "1G":
                if (isDeletion) {
                    tsPool.add(Uint16.valueOf(tribSlotNb));
                } else {
                    tsPool.remove(Uint16.valueOf(tribSlotNb));
                }
                break;
            case "10G":
                if (isDeletion) {
                    for (int i = 0; i < NB_TRIB_SLOT_PER_10GE; i++) {
                        tsPool.add(Uint16.valueOf(tribSlotNb + i));
                    }
                } else {
                    for (int i = 0; i < NB_TRIB_SLOT_PER_10GE; i++) {
                        tsPool.remove(Uint16.valueOf(tribSlotNb + i));
                    }
                }
                break;
            default:
                LOG.error("error updating tpn and ts pool for tp {}", tp.getTpId().getValue());
                break;
        }
        xtpcaBldr.setTsPool(tsPool);
        List<Uint16> tpnPool;
        if (xtpcaBldr.getOdtuTpnPool().get(0).getTpnPool() != null) {
            tpnPool = new ArrayList<>(xtpcaBldr.getOdtuTpnPool().get(0).getTpnPool());
            if (isDeletion) {
                tpnPool.add(Uint16.valueOf(tribPortNb));
            } else {
                tpnPool.remove(Uint16.valueOf(tribPortNb));
            }
        } else {
            tpnPool = new ArrayList<>();
        }
        xtpcaBldr.setOdtuTpnPool(ImmutableList.of(new OdtuTpnPoolBuilder().setOdtuType(ODTU4TsAllocated.class)
            .setTpnPool(tpnPool).build()));

        tpBldr.addAugmentation(TerminationPoint1.class,
            new TerminationPoint1Builder(tp.augmentation(TerminationPoint1.class))
                .setXpdrTpPortConnectionAttributes(xtpcaBldr.build()).build());
        return tpBldr.build();
    }

    private static Map<Integer, OtnTopoNode> convertPortMappingToOtnNodeList(Nodes mappingNode) {
        List<Mapping> networkMappings = mappingNode.getMapping().stream().filter(k -> k.getLogicalConnectionPoint()
            .contains("NETWORK")).collect(Collectors.toList());
        Map<Integer, OtnTopoNode> xpdrMap = new HashMap<>();
        for (Mapping mapping : networkMappings) {
            Integer xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
            if (!xpdrMap.containsKey(xpdrNb)) {
                List<Mapping> xpdrNetMaps = mappingNode.getMapping().stream().filter(k -> k.getLogicalConnectionPoint()
                    .contains("XPDR" + xpdrNb + NETWORK)).collect(Collectors.toList());
                List<Mapping> xpdrClMaps = mappingNode.getMapping().stream().filter(k -> k.getLogicalConnectionPoint()
                    .contains("XPDR" + xpdrNb + CLIENT)).collect(Collectors.toList());
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
        // create otn-topology node augmentation
        XpdrAttributes xpdrAttr = new XpdrAttributesBuilder()
            .setXpdrNumber(Uint16.valueOf(node.getXpdrNb()))
            .build();
        Node1 otnNodeAug = new Node1Builder()
            .setXpdrAttributes(xpdrAttr)
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 ocnNodeAug =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.TPDR).build();
        // create ietf node augmentation to add TP list
        List<TerminationPoint> tpList = new ArrayList<>();
        // creation of tps
        createTP(tpList, node, OpenroadmTpType.XPONDERCLIENT, If100GE.class, false);
        createTP(tpList, node, OpenroadmTpType.XPONDERNETWORK, IfOCHOTU4ODU4.class, true);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpList)
            .build();

        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(Node1.class, otnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class,
                ocnNodeAug)
            .addAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                ietfNodeAug)
            .build();
    }

    private static Node createMuxpdr(OtnTopoNode node) {
        // create otn-topology node augmentation
        // TODO: will need to be completed
        TpBandwidthSharing tpBwSh = new TpBandwidthSharingBuilder().build();
        XpdrAttributes xpdrAttr = new XpdrAttributesBuilder()
            .setXpdrNumber(Uint16.valueOf(node.getXpdrNb()))
            .build();

        List<NonBlockingList> nblList = new ArrayList<>();
        for (int i = 1; i <= node.getNbTpClient(); i++) {
            List<TpId> tpList = new ArrayList<>();
            TpId tpId = new TpId("XPDR" + node.getXpdrNb() + CLIENT + i);
            tpList.add(tpId);
            tpId = new TpId("XPDR" + node.getXpdrNb() + "-NETWORK1");
            tpList.add(tpId);
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(i))
                .setTpList(tpList)
                .setAvailableInterconnectBandwidth(Uint32.valueOf(node.getNbTpNetwork() * 10L))
                .setInterconnectBandwidthUnit(Uint32.valueOf(1000000000))
                .build();
            nblList.add(nbl);
        }
        OduSwitchingPools oduSwitchPool = new OduSwitchingPoolsBuilder()
            .setSwitchingPoolNumber(Uint16.valueOf(1))
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

        // create ietf node augmentation to add TP list
        List<TerminationPoint> tpList = new ArrayList<>();
        // creation of tps
        createTP(tpList, node, OpenroadmTpType.XPONDERCLIENT, If10GEODU2e.class, true);
        createTP(tpList, node, OpenroadmTpType.XPONDERNETWORK, IfOCHOTU4ODU4.class, true);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpList)
            .build();

        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(Node1.class, otnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class,
                ocnNodeAug)
            .addAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                ietfNodeAug)
            .build();
    }

    private static Node createSwitch(OtnTopoNode node) {
        List<TpId> tpl = new ArrayList<>();
        TpId tpId = null;
        for (int i = 1; i <= node.getNbTpClient(); i++) {
            tpId = new TpId("XPDR" + node.getXpdrNb() + CLIENT + i);
            tpl.add(tpId);
        }
        for (int i = 1; i <= node.getNbTpNetwork(); i++) {
            tpId = new TpId("XPDR" + node.getXpdrNb() + NETWORK + i);
            tpl.add(tpId);
        }
        List<NonBlockingList> nblList = new ArrayList<>();
        NonBlockingList nbl = new NonBlockingListBuilder()
            .setNblNumber(Uint16.valueOf(1))
            .setTpList(tpl)
            .build();
        nblList.add(nbl);

        OduSwitchingPools oduSwitchPool = new OduSwitchingPoolsBuilder()
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .setSwitchingPoolType(SwitchingPoolTypes.NonBlocking)
            .setNonBlockingList(nblList)
            .build();
        List<OduSwitchingPools> oduSwitchPoolList = new ArrayList<>();
        oduSwitchPoolList.add(oduSwitchPool);
        SwitchingPools switchingPools = new SwitchingPoolsBuilder()
            .setOduSwitchingPools(oduSwitchPoolList)
            .build();

        // create otn-topology node augmentation
        // TODO: will need to be completed
        TpBandwidthSharing tpBwSh = new TpBandwidthSharingBuilder().build();
        XpdrAttributes xpdrAttr = new XpdrAttributesBuilder()
            .setXpdrNumber(Uint16.valueOf(node.getXpdrNb()))
            .build();

        Node1 otnNodeAug = new Node1Builder()
            .setTpBandwidthSharing(tpBwSh)
            .setXpdrAttributes(xpdrAttr)
            .setSwitchingPools(switchingPools)
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 ocnNodeAug =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.SWITCH).build();

        // create ietf node augmentation to add TP list
        List<TerminationPoint> tpList = new ArrayList<>();
        // creation of tps
        createTP(tpList, node, OpenroadmTpType.XPONDERCLIENT, If100GEODU4.class, true);
        createTP(tpList, node, OpenroadmTpType.XPONDERNETWORK, IfOCHOTU4ODU4.class, true);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpList)
            .build();

        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(Node1.class, otnNodeAug)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class,
                ocnNodeAug)
            .addAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                ietfNodeAug)
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
            // openroadm-otn-topoology augmentation
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
                TpId tpId = new TpId("XPDR" + node.getXpdrNb() + NETWORK + i);
                if (node.getXpdrNetConnectionMap().get(tpId.getValue()) != null) {
                    tpceTp1Bldr.setAssociatedConnectionMapPort(node.getXpdrNetConnectionMap().get(tpId.getValue()));
                }
                SupportingTerminationPoint stp = new SupportingTerminationPointBuilder()
                    .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
                    .setNodeRef(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
                    .setTpRef("XPDR" + node.getXpdrNb() + NETWORK + i)
                    .build();
                List<SupportingTerminationPoint> supportTpList = new ArrayList<>();
                supportTpList.add(stp);
                tpList.add(buildIetfTp(tpceTp1Bldr, otnTp1, tpType, tpId, supportTpList));
            } else if (OpenroadmTpType.XPONDERCLIENT.equals(tpType)) {
                TpId tpId = new TpId("XPDR" + node.getXpdrNb() + CLIENT + i);
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
            .setNodeRef(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID),
                new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
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

    private static TerminationPoint buildIetfTp(
        org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1Builder tpceTp1Bldr,
        TerminationPoint1 otnTp1, OpenroadmTpType tpType, TpId tpId,
        List<SupportingTerminationPoint> supportTpList) {

        TerminationPointBuilder ietfTpBldr = new TerminationPointBuilder();
        if (tpceTp1Bldr.getAssociatedConnectionMapPort() != null) {
            ietfTpBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1.class,
                tpceTp1Bldr.build());
        }
        if (supportTpList != null) {
            ietfTpBldr.setSupportingTerminationPoint(supportTpList);
        }
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1 ocnTp =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder()
            .setTpType(tpType).build();

        ietfTpBldr.setTpId(tpId)
            .withKey(new TerminationPointKey(tpId))
            .addAugmentation(TerminationPoint1.class, otnTp1)
            .addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class,
                ocnTp);
        return ietfTpBldr.build();
    }
}
