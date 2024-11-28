/*
 * Copyright © 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.dto.OtnTopoNode;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPoolsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODTUCnTs;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OdtuTypeIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OduRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.SwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.TpBandwidthSharingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.XpdrAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev191129.SwitchingPoolTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.otn.tp.attributes.OdtuTpnPool;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.otn.tp.attributes.OdtuTpnPoolBuilder;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPointKey;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenRoadmOtnTopology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmOtnTopology.class);
    private static final String CLIENT = "-CLIENT";
    private static final String NETWORK = "-NETWORK";
    private static final String XPDR = "-XPDR";
    private static final String OTN_PARAMS_ERROR = "Error with otn parameters of supported link {}";
    private static final int NB_TRIB_PORTS = 80;
    private static final int NB_TRIB_SLOTS = 80;

    private static final Map<String, OduRateIdentity> RATE_MAP = Map.of(
        "If100GEODU4", ODU4.VALUE,
        "IfOCHOTU4ODU4", ODU4.VALUE,
        "If1GEODU0", ODU0.VALUE,
        "If10GEODU2", ODU2.VALUE,
        "If10GEODU2e", ODU2e.VALUE);
    private static final Map<OtnLinkType, Long> OTNLINKTYPE_BW_MAP = Map.of(
        OtnLinkType.ODTU4, 100000L,
        OtnLinkType.ODUC4, 400000L,
        OtnLinkType.ODUC3, 300000L,
        OtnLinkType.ODUC2, 200000L);
    private static final Map<OtnLinkType, Long> OTNLINKTYPE_OTU_BW_MAP = Map.of(
        OtnLinkType.OTU4, 100000L,
        OtnLinkType.OTUC4, 400000L);
    private static final Map<Uint32, Long> SERVICERATE_BWINCR_MAP = Map.of(
        Uint32.valueOf(1), 1000L,
        Uint32.valueOf(10), 10000L,
        Uint32.valueOf(100), 100000L);
    private static final Map<Uint32, OdtuTypeIdentity> SERVICERATE_ODTUTYPECLASS_MAP = Map.of(
        Uint32.valueOf(1), ODTU4TsAllocated.VALUE,
        Uint32.valueOf(10), ODTU4TsAllocated.VALUE,
        Uint32.valueOf(100), ODTUCnTs.VALUE);

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
                case Regen:
                case RegenUni:
                    // TODO: Need to revisit this method
                    nodes.add(createRegen(node));
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

        return new TopologyShard(
            null,
            OTNLINKTYPE_OTU_BW_MAP.containsKey(linkType)
                ? initialiseOtnLinks(nodeA, tpA, nodeZ, tpZ, linkType)
                : null);
    }

    public static TopologyShard createOtnLinks(
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp
                .Link notifLink,
            OtnLinkType linkType) {

        return new TopologyShard(
            null,
            initialiseOtnLinks(notifLink.getATermination().getNodeId(),
            notifLink.getATermination().getTpId(),
            notifLink.getZTermination().getNodeId(),
            notifLink.getZTermination().getTpId(),
            linkType));
    }

    public static TopologyShard createOtnLinks(
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp
                .Link notifLink,
            List<Link> supportedOtu4links, List<TerminationPoint> supportedTPs, OtnLinkType linkType) {

        if (OTNLINKTYPE_OTU_BW_MAP.containsKey(linkType)) {
            return new TopologyShard(
                null,
                initialiseOtnLinks(
                    notifLink.getATermination().getNodeId(), notifLink.getATermination().getTpId(),
                    notifLink.getZTermination().getNodeId(), notifLink.getZTermination().getTpId(), linkType));
        } else if (OTNLINKTYPE_BW_MAP.containsKey(linkType)) {
            List<Link> links = initialiseOtnLinks(
                notifLink.getATermination().getNodeId(), notifLink.getATermination().getTpId(),
                notifLink.getZTermination().getNodeId(), notifLink.getZTermination().getTpId(), linkType);
            links.addAll(updateOtnLinkBwParameters(supportedOtu4links, linkType));
            List<TerminationPoint> updatedTPs = new ArrayList<>();
            for (TerminationPoint tp : supportedTPs) {
                updatedTPs.add(updateTp(tp, true, linkType));
            }
            return new TopologyShard(null, links, updatedTPs);
        } else {
            return null;
        }
    }

    public static TopologyShard createOtnLinks(List<Link> suppOtuLinks, List<TerminationPoint> oldTps,
            OtnLinkType linkType) {

        List<Link> links = new ArrayList<>();
        for (Link link : suppOtuLinks) {
            if (link.augmentation(Link1.class) == null) {
                LOG.error("Error with OTN parameters of supported link {}", link.getLinkId().getValue());
                continue;
            }
            if (!OTNLINKTYPE_BW_MAP.containsKey(linkType)) {
                LOG.error("Error with link {} : unsupported OTN link type", link.getLinkId().getValue());
                continue;
            }
            if (link.augmentation(Link1.class).getAvailableBandwidth().longValue() < OTNLINKTYPE_BW_MAP.get(linkType)) {
                LOG.error("Error with link {} : unsufficient available bandwith", link.getLinkId().getValue());
                continue;
            }
            links.add(updateOtnLinkBwParameters(link, 0L, OTNLINKTYPE_BW_MAP.get(linkType)));
        }
        if (links.size() == 2) {
            links.addAll(initialiseOtnLinks(suppOtuLinks.get(0).getSource().getSourceNode().getValue(),
                suppOtuLinks.get(0).getSource().getSourceTp().getValue(),
                suppOtuLinks.get(0).getDestination().getDestNode().getValue(),
                suppOtuLinks.get(0).getDestination().getDestTp().getValue(),
                linkType));
        }
        List<TerminationPoint> tps = new ArrayList<>();
        for (TerminationPoint tp : oldTps) {
            tps.add(updateTp(tp, true, linkType));
        }
        return links.size() == 4 && tps.size() == 2
            ?  new TopologyShard(null, links, tps)
            :  new TopologyShard(null, null, null);
    }

    public static TopologyShard updateOtnLinks(List<Link> suppOduLinks, List<TerminationPoint> oldTps,
            Uint32 serviceRate, Short tribPortNb, Short minTribSlotNb, Short maxTribSlotNb, boolean isDeletion) {

        List<Link> links = new ArrayList<>();
        if (!SERVICERATE_BWINCR_MAP.containsKey(serviceRate)) {
            LOG.warn("Error with not managed service rate {}", serviceRate.toString());
            return new TopologyShard(null, null, null);
        }
        Long bwIncr = SERVICERATE_BWINCR_MAP.get(serviceRate);
        for (Link link : suppOduLinks) {
            if (link.augmentation(Link1.class) == null
                    || link.augmentation(Link1.class).getAvailableBandwidth() == null
                    || link.augmentation(Link1.class).getUsedBandwidth() == null) {
                LOG.error(OTN_PARAMS_ERROR, link.getLinkId().getValue());
            } else {
                Uint32 avlBw = link.augmentation(Link1.class).getAvailableBandwidth();
                Uint32 usedBw = link.augmentation(Link1.class).getUsedBandwidth();
                if (avlBw.toJava() < bwIncr) {
                    bwIncr = 0L;
                }
                links.add(
                    isDeletion
                        ? updateOtnLinkBwParameters(link, avlBw.toJava() + bwIncr, usedBw.toJava() - bwIncr)
                        : updateOtnLinkBwParameters(link, avlBw.toJava() - bwIncr, usedBw.toJava() + bwIncr)
                );
            }
        }
        List<TerminationPoint> tps = new ArrayList<>();
        for (TerminationPoint tp : oldTps) {
            if (bwIncr > 0L) {
                tps.add(updateNodeTpTsPool(tp, serviceRate, tribPortNb, minTribSlotNb, maxTribSlotNb, isDeletion));
            }
        }
        if (links.isEmpty() || tps.isEmpty()) {
            LOG.error("unable to update otn links");
            return new TopologyShard(null, null, null);
        } else {
            return new TopologyShard(null, links, tps);
        }
    }

    public static TopologyShard updateOtnLinks(List<Link> suppOtuLinks, boolean isDeletion) {

        List<Link> links = new ArrayList<>();
        for (Link link : suppOtuLinks) {
            if (link.augmentation(Link1.class) == null
                || link.augmentation(Link1.class).getAvailableBandwidth() == null
                || link.augmentation(Link1.class).getUsedBandwidth() == null) {
                LOG.error(OTN_PARAMS_ERROR, link.getLinkId().getValue());
            } else {
                links.add(
                    isDeletion
                        ? updateOtnLinkBwParameters(link, Long.valueOf(100000), Long.valueOf(0))
                        : updateOtnLinkBwParameters(link, Long.valueOf(0), Long.valueOf(100000))
                );
            }
        }
        if (links.isEmpty()) {
            LOG.error("unable to update otn links");
            return new TopologyShard(null, null, null);
        } else {
            return new TopologyShard(null, links, null);
        }
    }

    public static TopologyShard deleteOtnLinks(List<Link> suppOtuLinks, List<TerminationPoint> oldTps,
            OtnLinkType linkType) {

        List<Link> links = new ArrayList<>();
        for (Link link : suppOtuLinks) {
            if (link.augmentation(Link1.class) == null
                    || link.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923
                                    .Link1.class) == null) {
                LOG.error(OTN_PARAMS_ERROR, link.getLinkId().getValue());
                return new TopologyShard(null, null, null);
            }
            OtnLinkType otnLinkType = link.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.Link1.class)
                    .getOtnLinkType();
            if (!OTNLINKTYPE_OTU_BW_MAP.containsKey(otnLinkType)) {
            //TODO shouldn't other link type listed in OTNLINKTYPE_BW_MAP be handled too ?
                LOG.warn("Unexpected otn-link-type {} for link {}", otnLinkType, link.getLinkId());
                continue;
            }
            links.add(updateOtnLinkBwParameters(link, OTNLINKTYPE_OTU_BW_MAP.get(otnLinkType) , 0L));
        }
        List<TerminationPoint> tps = new ArrayList<>();
        for (TerminationPoint tp : oldTps) {
            tps.add(updateTp(tp, false, linkType));
        }
        return
            links.isEmpty() || tps.isEmpty()
                ? new TopologyShard(null, null, null)
                : new TopologyShard(null, links, tps);
    }

    private static List<Link> initialiseOtnLinks(String nodeA, String tpA, String nodeZ, String tpZ,
            OtnLinkType linkType) {

        List<Link> links = new ArrayList<>();
        String nodeATopo = formatNodeName(nodeA, tpA);
        String nodeZTopo = formatNodeName(nodeZ, tpZ);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.Link1 tpceLink1
            = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.Link1Builder()
                .setOtnLinkType(linkType).build();
        Link1Builder otnLink1Bldr = new Link1Builder()
            .setUsedBandwidth(Uint32.valueOf(0));
        if (OTNLINKTYPE_OTU_BW_MAP.containsKey(linkType)) {
            otnLink1Bldr.setAvailableBandwidth(Uint32.valueOf(OTNLINKTYPE_OTU_BW_MAP.get(linkType)));
        } else if (OTNLINKTYPE_BW_MAP.containsKey(linkType)) {
            otnLink1Bldr.setAvailableBandwidth(Uint32.valueOf(OTNLINKTYPE_BW_MAP.get(linkType)));
        } else {
            LOG.error("unable to set available bandwidth to unknown link type");
        }
        // create link A-Z
        LinkBuilder ietfLinkAZBldr = TopologyUtils.createLink(nodeATopo, nodeZTopo, tpA, tpZ, linkType.getName());
        links.add(ietfLinkAZBldr
            .addAugmentation(tpceLink1)
            .addAugmentation(otnLink1Bldr.build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder(
                        ietfLinkAZBldr.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class))
                    .setLinkType(OpenroadmLinkType.OTNLINK)
                    .setOperationalState(State.InService)
                    .setAdministrativeState(AdminStates.InService)
                    .build())
            .build());
        // create link Z-A
        LinkBuilder ietfLinkZABldr = TopologyUtils.createLink(nodeZTopo, nodeATopo, tpZ, tpA, linkType.getName());
        links.add(ietfLinkZABldr
            .addAugmentation(tpceLink1)
            .addAugmentation(otnLink1Bldr.build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder(
                        ietfLinkZABldr.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class))
                    .setLinkType(OpenroadmLinkType.OTNLINK)
                    .setOperationalState(State.InService)
                    .setAdministrativeState(AdminStates.InService)
                    .build())
            .build());
        return links;
    }

    private static Link updateOtnLinkBwParameters(Link link, Long availBw, Long usedBw) {

        LOG.debug("in updateOtnLinkBwParameters with availBw = {}, usedBw = {}", availBw, usedBw);
        return new LinkBuilder(link)
            .addAugmentation(
                new Link1Builder(link.augmentation(Link1.class))
                    .setAvailableBandwidth(Uint32.valueOf(availBw))
                    .setUsedBandwidth(Uint32.valueOf(usedBw))
                    .build())
            .build();
    }

    private static List<Link> updateOtnLinkBwParameters(List<Link> supportedLinks, OtnLinkType linkType) {

        LOG.debug("in updateOtnLinkBwParameters with supportedLinks = {}, linkType = {}", supportedLinks, linkType);
        List<Link> updatedlinks = new ArrayList<>();
        for (Link link : supportedLinks) {
            updatedlinks.add(
                new LinkBuilder(link)
                    .addAugmentation(new Link1Builder(link.augmentation(Link1.class))
                        .setAvailableBandwidth(Uint32.valueOf(0))
                        .setUsedBandwidth(
                            OTNLINKTYPE_BW_MAP.containsKey(linkType)
                                ? Uint32.valueOf(OTNLINKTYPE_BW_MAP.get(linkType))
                                : Uint32.valueOf(0))
                        .build())
                    .build());
        }
        return updatedlinks;
    }

    private static TerminationPoint updateTp(TerminationPoint originalTp, boolean addingTsTpnPoolTermination,
            OtnLinkType linkType) {

        LOG.debug("in updateTp");
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(originalTp);
        TerminationPoint1Builder otnTp1Bldr =
            new TerminationPoint1Builder(tpBldr.augmentation(TerminationPoint1.class));
        XpdrTpPortConnectionAttributesBuilder xtpcaBldr =
            new XpdrTpPortConnectionAttributesBuilder(otnTp1Bldr.getXpdrTpPortConnectionAttributes());
        if (addingTsTpnPoolTermination) {
            Set<Uint16> tsPool = new HashSet<>();
            for (int i = 1; i <= NB_TRIB_SLOTS; i++) {
                tsPool.add(Uint16.valueOf(i));
            }
            xtpcaBldr.setTsPool(tsPool);
            Set<Uint16> tpnPool = new HashSet<>();
            int nbTribPort = NB_TRIB_PORTS;
            if (OtnLinkType.ODUC4.equals(linkType)) {
                nbTribPort = 4;
            }
            for (int i = 1; i <= nbTribPort; i++) {
                tpnPool.add(Uint16.valueOf(i));
            }
            OdtuTpnPool oduTpnPool = new OdtuTpnPoolBuilder()
                .setOdtuType(ODTU4TsAllocated.VALUE)
                .setTpnPool(tpnPool)
                .build();
            xtpcaBldr.setOdtuTpnPool(ImmutableMap.of(oduTpnPool.key(),oduTpnPool));
        } else {
            xtpcaBldr.setTsPool(null);
            xtpcaBldr.setOdtuTpnPool(ImmutableMap.of());
        }
        return tpBldr.addAugmentation(otnTp1Bldr.setXpdrTpPortConnectionAttributes(xtpcaBldr.build()).build()).build();
    }

    private static TerminationPoint updateNodeTpTsPool(TerminationPoint tp, Uint32 serviceRate, Short tribPortNb,
            Short minTribSlotNb, Short maxTribSlotNb, boolean isDeletion) {

        LOG.debug("in updateNodeTpTsPool");
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(tp);
        @Nullable
        XpdrTpPortConnectionAttributesBuilder xtpcaBldr =
            new XpdrTpPortConnectionAttributesBuilder(
                tpBldr.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes());
        Set<Uint16> tsPool = new HashSet<>(xtpcaBldr.getTsPool());
        if (isDeletion) {
            for (int i = minTribSlotNb; i <= maxTribSlotNb; i++) {
                tsPool.add(Uint16.valueOf(i));
            }
        } else {
            for (int i = minTribSlotNb; i <= maxTribSlotNb; i++) {
                tsPool.remove(Uint16.valueOf(i));
            }
        }
        xtpcaBldr.setTsPool(tsPool);
        Set<Uint16> tpnPool;
        List<OdtuTpnPool> odtuTpnPoolValues = new ArrayList<>(xtpcaBldr.getOdtuTpnPool().values());
        if (odtuTpnPoolValues.get(0).getTpnPool() == null) {
            tpnPool = new HashSet<>();
        } else {
            tpnPool = new HashSet<>(odtuTpnPoolValues.get(0).getTpnPool());
            if (isDeletion) {
                tpnPool.add(Uint16.valueOf(tribPortNb));
            } else {
                tpnPool.remove(Uint16.valueOf(tribPortNb));
            }
        }
        OdtuTypeIdentity odtuType;
        if (SERVICERATE_ODTUTYPECLASS_MAP.containsKey(serviceRate)) {
            odtuType = SERVICERATE_ODTUTYPECLASS_MAP.get(serviceRate);
        } else {
            odtuType = null;
            LOG.warn("Unable to set the odtu-type");
        }
        OdtuTpnPool odtuTpnPool = new OdtuTpnPoolBuilder()
            .setOdtuType(odtuType)
            .setTpnPool(tpnPool)
            .build();
        return tpBldr
            .addAugmentation(
                new TerminationPoint1Builder(tp.augmentation(TerminationPoint1.class))
                    .setXpdrTpPortConnectionAttributes(
                        xtpcaBldr
                            .setOdtuTpnPool(ImmutableMap.of(odtuTpnPool.key(),odtuTpnPool))
                            .build())
                    .build())
            .build();
    }

    private static Map<Integer, OtnTopoNode> convertPortMappingToOtnNodeList(Nodes mappingNode) {

        List<Mapping> networkMappings =
            mappingNode.nonnullMapping().values().stream()
                .filter(k -> k.getLogicalConnectionPoint().contains("NETWORK"))
                .collect(Collectors.toList());
        Map<Integer, OtnTopoNode> xpdrMap = new HashMap<>();
        for (Mapping mapping : networkMappings) {
            Integer xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
            if (!xpdrMap.containsKey(xpdrNb)) {
                List<Mapping> xpdrNetMaps =
                    mappingNode.nonnullMapping().values().stream()
                        .filter(k -> k.getLogicalConnectionPoint().contains("XPDR" + xpdrNb + NETWORK))
                        .collect(Collectors.toList());
                List<Mapping> xpdrClMaps =
                    mappingNode.nonnullMapping().values().stream()
                        .filter(k -> k.getLogicalConnectionPoint().contains("XPDR" + xpdrNb + CLIENT))
                        .collect(Collectors.toList());
                xpdrMap.put(
                    xpdrNb,
                    new OtnTopoNode(
                        mappingNode.getNodeId(),
                        mappingNode.getNodeInfo().getNodeClli(),
                        xpdrNb,
                        mapping.getXpdrType() == null
                            ? XpdrNodeTypes.Tpdr
                            : mapping.getXpdrType(),
                        fillConnectionMapLcp(xpdrNetMaps),
                        fillConnectionMapLcp(xpdrClMaps),
                        xpdrNetMaps,
                        xpdrClMaps
                    ));
            }
        }
        LOG.debug("there are {} xpdr to build", xpdrMap.size());
        xpdrMap.forEach((k, v) -> LOG.debug("xpdr {} = {} - {} - {} - {}",
            k, v.getNodeId(), v.getNodeType(), v.getNbTpClient(), v.getNbTpNetwork()));
        return xpdrMap;
    }

    private static Map<String, String> fillConnectionMapLcp(List<Mapping> mappingList) {

        Map<String, String> xpdrConnectionMap = new HashMap<>();
        for (Mapping map : mappingList) {
            xpdrConnectionMap.put(map.getLogicalConnectionPoint(), map.getConnectionMapLcp());
        }
        return xpdrConnectionMap;
    }

    private static Node createTpdr(OtnTopoNode node) {
        Map<TerminationPointKey,TerminationPoint> tpMap = new HashMap<>();
        createTP(tpMap, node, OpenroadmTpType.XPONDERCLIENT, false);
        createTP(tpMap, node, OpenroadmTpType.XPONDERNETWORK, true);
        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(
                new Node1Builder()
                    .setXpdrAttributes(
                        new XpdrAttributesBuilder()
                            .setXpdrNumber(Uint16.valueOf(node.getXpdrNb()))
                            .build())
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                    .setNodeType(OpenroadmNodeType.TPDR)
                    .setOperationalState(State.InService)
                    .setAdministrativeState(AdminStates.InService)
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .Node1Builder()
                        .setTerminationPoint(tpMap)
                        .build())
            .build();
    }

    // TODO: This is same as createTpdr. After Open ROADM network models are updated,
    //    we will revisit this method to include regen based data.
    private static Node createRegen(OtnTopoNode node) {
        Map<TerminationPointKey,TerminationPoint> tpMap = new HashMap<>();
        createTP(tpMap, node, OpenroadmTpType.XPONDERCLIENT, false);
        createTP(tpMap, node, OpenroadmTpType.XPONDERNETWORK, true);
        // return ietfNode
        return new NodeBuilder()
                .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
                .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
                .setSupportingNode(createSupportingNodes(node))
                .addAugmentation(
                        new Node1Builder()
                                .setXpdrAttributes(
                                        new XpdrAttributesBuilder()
                                                .setXpdrNumber(Uint16.valueOf(node.getXpdrNb()))
                                                .build())
                                .build())
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                                .setNodeType(OpenroadmNodeType.TPDR)
                                .setOperationalState(State.InService)
                                .setAdministrativeState(AdminStates.InService)
                                .build())
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                                .Node1Builder()
                                .setTerminationPoint(tpMap)
                                .build())
                .build();
    }

    private static Node createMuxpdr(OtnTopoNode node) {

        // prepare otn-topology node augmentation
        // TODO: will need to be completed
        Map<NonBlockingListKey, NonBlockingList> nbMap = new HashMap<>();
        for (int i = 1; i <= node.getNbTpClient(); i++) {
            Set<TpId> tpList = new HashSet<>();
            tpList.add(new TpId("XPDR" + node.getXpdrNb() + CLIENT + i));
            tpList.add(new TpId("XPDR" + node.getXpdrNb() + "-NETWORK1"));
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(i))
                .setTpList(tpList)
                .setAvailableInterconnectBandwidth(Uint32.valueOf(node.getNbTpNetwork() * 10L))
                .setInterconnectBandwidthUnit(Uint32.valueOf(1000000000))
                .build();
            nbMap.put(nbl.key(),nbl);
        }
        OduSwitchingPools oduSwitchPool = new OduSwitchingPoolsBuilder()
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .setSwitchingPoolType(SwitchingPoolTypes.NonBlocking)
            .setNonBlockingList(nbMap)
            .build();
        Map<TerminationPointKey, TerminationPoint> tpMap = new HashMap<>();
        createTP(tpMap, node, OpenroadmTpType.XPONDERCLIENT, true);
        createTP(tpMap, node, OpenroadmTpType.XPONDERNETWORK, true);
        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(
                new Node1Builder()
                    .setTpBandwidthSharing(new TpBandwidthSharingBuilder().build())
                    .setXpdrAttributes(
                        new XpdrAttributesBuilder()
                            .setXpdrNumber(Uint16.valueOf(node.getXpdrNb()))
                            .build())
                    .setSwitchingPools(
                        new SwitchingPoolsBuilder()
                            .setOduSwitchingPools(Map.of(oduSwitchPool.key(),oduSwitchPool))
                            .build())
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                    .setNodeType(OpenroadmNodeType.MUXPDR)
                    .setAdministrativeState(AdminStates.InService)
                    .setOperationalState(State.InService)
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .Node1Builder()
                        .setTerminationPoint(tpMap)
                        .build())
            .build();
    }

    private static Node createSwitch(OtnTopoNode node) {

        Set<TpId> tpl = new HashSet<>();
        for (int i = 1; i <= node.getNbTpClient(); i++) {
            tpl.add(new TpId("XPDR" + node.getXpdrNb() + CLIENT + i));
        }
        for (int i = 1; i <= node.getNbTpNetwork(); i++) {
            tpl.add(new TpId("XPDR" + node.getXpdrNb() + NETWORK + i));
        }
        Map<NonBlockingListKey, NonBlockingList> nbMap = new HashMap<>();
        NonBlockingList nbl = new NonBlockingListBuilder()
            .setNblNumber(Uint16.valueOf(1))
            .setTpList(tpl)
            .build();
        nbMap.put(nbl.key(),nbl);
        OduSwitchingPools oduSwitchPool = new OduSwitchingPoolsBuilder()
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .setSwitchingPoolType(SwitchingPoolTypes.NonBlocking)
            .setNonBlockingList(nbMap)
            .build();
        Map<OduSwitchingPoolsKey, OduSwitchingPools> oduSwitchPoolList = new HashMap<>();
        oduSwitchPoolList.put(oduSwitchPool.key(),oduSwitchPool);
        Map<TerminationPointKey, TerminationPoint> tpMap = new HashMap<>();
        createTP(tpMap, node, OpenroadmTpType.XPONDERCLIENT, true);
        createTP(tpMap, node, OpenroadmTpType.XPONDERNETWORK, true);
        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(
                new Node1Builder()
                    .setTpBandwidthSharing(new TpBandwidthSharingBuilder().build())
                    .setXpdrAttributes(
                        new XpdrAttributesBuilder()
                            .setXpdrNumber(Uint16.valueOf(node.getXpdrNb()))
                            .build())
                    .setSwitchingPools(
                        new SwitchingPoolsBuilder()
                            .setOduSwitchingPools(oduSwitchPoolList)
                            .build())
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                    .setNodeType(OpenroadmNodeType.SWITCH)
                    .setOperationalState(State.InService)
                    .setAdministrativeState(AdminStates.InService)
                    .build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .Node1Builder()
                        .setTerminationPoint(tpMap)
                        .build())
            .build();
    }

    private static void createTP(Map<TerminationPointKey, TerminationPoint> tpMap,
            OtnTopoNode node, OpenroadmTpType tpType, boolean withRate) {

        List<Mapping> mappings = null;
        switch (tpType) {
            case XPONDERNETWORK:
                mappings = node.getXpdrNetMappings();
                break;
            case XPONDERCLIENT:
                mappings = node.getXpdrClMappings();
                break;
            default:
                LOG.error("Error with Termination Point type {}", tpType);
                return;
        }
        fillTpMap(tpMap, node, tpType, withRate, mappings);
    }

    private static void fillTpMap(Map<TerminationPointKey, TerminationPoint> tpMap, OtnTopoNode node,
            OpenroadmTpType tpType, boolean withRate, List<Mapping> mappings) {

        for (Mapping mapping : mappings) {
            // openroadm-otn-topoology augmentation
            Map<SupportedInterfaceCapabilityKey, SupportedInterfaceCapability> supIfMap = new HashMap<>();
            TerminationPoint1Builder otnTp1Bldr = new TerminationPoint1Builder();
            if (mapping.getSupportedInterfaceCapability() == null) {
                LOG.warn("mapping {} of node {} has no if-cap-type",
                    mapping.getLogicalConnectionPoint(), node.getNodeId());
            } else {
                XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder();
                for (org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability
                        supInterCapa : mapping.getSupportedInterfaceCapability()) {
                    SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                        .withKey(new SupportedInterfaceCapabilityKey(supInterCapa))
                        .setIfCapType(supInterCapa)
                        .build();
                    supIfMap.put(supIfCapa.key(), supIfCapa);
                }
                otnTp1Bldr.setTpSupportedInterfaces(
                    new TpSupportedInterfacesBuilder()
                        .setSupportedInterfaceCapability(supIfMap)
                        .build()
                );
                //TODO: It is not logical to assign a priori one of the possible rate to the TP.
                //Would be worth assigning per default "unallocated" at the tp creation step,
                //and updating it with correct rate when it supports a specific service.
                if (withRate) {
                    otnTp1Bldr.setXpdrTpPortConnectionAttributes(
                        xtpcaBldr.setRate(fixRate(mapping.getSupportedInterfaceCapability())).build());
                }
            }
            setclientNwTpAttr(
                tpMap,
                node,
                new TpId(mapping.getLogicalConnectionPoint()),
                tpType,
                otnTp1Bldr.build(),
                mapping);
        }
    }

    private static void setclientNwTpAttr(Map<TerminationPointKey, TerminationPoint> tpMap, OtnTopoNode node, TpId tpId,
            OpenroadmTpType tpType, TerminationPoint1 otnTp1, Mapping mapping) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder cnTP1BLdr
                = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                    .TerminationPoint1Builder();
        switch (tpType) {
            case XPONDERNETWORK:
                if (node.getXpdrNetConnectionMap().get(tpId.getValue()) != null) {
                    cnTP1BLdr.setAssociatedConnectionMapTp(Set.of(
                            new TpId(node.getXpdrNetConnectionMap().get(tpId.getValue()))));
                }
                SupportingTerminationPoint stp = new SupportingTerminationPointBuilder()
                    .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
                    .setNodeRef(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
                    .setTpRef(tpId)
                    .build();
                TerminationPoint ietfTpNw =
                    buildIetfTp(cnTP1BLdr, otnTp1, tpType, tpId, Map.of(stp.key(), stp), mapping);
                tpMap.put(ietfTpNw.key(),ietfTpNw);
                break;
            case XPONDERCLIENT:
                if (node.getXpdrCliConnectionMap().get(tpId.getValue()) != null) {
                    cnTP1BLdr.setAssociatedConnectionMapTp(Set.of(
                            new TpId(node.getXpdrCliConnectionMap().get(tpId.getValue()))));
                }
                TerminationPoint ietfTpCl = buildIetfTp(cnTP1BLdr, otnTp1, tpType, tpId, null, mapping);
                tpMap.put(ietfTpCl.key(),ietfTpCl);
                break;
            default:
                LOG.error("Undefined tpType for Termination point {} of {}", tpId.getValue(), node.getNodeId());
                break;
        }
    }

    private static OduRateIdentity fixRate(
            Set<org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability> list) {
        for (org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability
                supIfCap: list) {
            String simpleName = supIfCap.toString().split("\\{")[0];
            if (RATE_MAP.containsKey(simpleName)) {
                return RATE_MAP.get(simpleName);
            }
        }
        return null;
    }

    private static Map<SupportingNodeKey,SupportingNode> createSupportingNodes(OtnTopoNode node) {

        SupportingNode suppNode1 = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .setNodeRef(new NodeId(node.getNodeId()))
            .withKey(
                new SupportingNodeKey(
                    new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID),
                    new NodeId(node.getNodeId())))
            .build();
        SupportingNode suppNode2 = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
            .setNodeRef(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(
                new SupportingNodeKey(
                    new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID),
                    new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .build();
        SupportingNode suppNode3 = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
            .setNodeRef(new NodeId(node.getClli()))
            .withKey(
                new SupportingNodeKey(
                    new NetworkId(NetworkUtils.CLLI_NETWORK_ID),
                    new NodeId(node.getClli())))
            .build();
        Map<SupportingNodeKey,SupportingNode> suppNodeMap = new HashMap<>();
        suppNodeMap.put(suppNode1.key(),suppNode1);
        suppNodeMap.put(suppNode2.key(),suppNode2);
        suppNodeMap.put(suppNode3.key(),suppNode3);
        return suppNodeMap;
    }

    private static TerminationPoint buildIetfTp(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder cnTP1BLdr,
            TerminationPoint1 otnTp1, OpenroadmTpType tpType, TpId tpId,
            Map<SupportingTerminationPointKey, SupportingTerminationPoint> supportTpMap, Mapping mapping) {

        TerminationPointBuilder ietfTpBldr = new TerminationPointBuilder();
        if (supportTpMap != null) {
            ietfTpBldr.setSupportingTerminationPoint(supportTpMap);
        }
        return ietfTpBldr
            .setTpId(tpId)
            .withKey(new TerminationPointKey(tpId))
            .addAugmentation(otnTp1)
            .addAugmentation(cnTP1BLdr.setTpType(tpType)
                    .setAdministrativeState(TopologyUtils.setNetworkAdminState(mapping.getPortAdminState()))
                    .setOperationalState(TopologyUtils.setNetworkOperState(mapping.getPortOperState()))
                    .build())
                .build();
    }

    private static String formatNodeName(String nodeName, String tpName) {

        return nodeName.contains(XPDR)
                ? nodeName
                : new StringBuilder(nodeName).append("-").append(tpName.split("-")[0]).toString();
    }
}
