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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.dto.OtnTopoNode;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220114.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220114.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.OduSwitchingPoolsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODTUCnTs;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OdtuTypeIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OduRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.SwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.SwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.TpBandwidthSharing;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.TpBandwidthSharingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.XpdrAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.XpdrAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev191129.SwitchingPoolTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev200529.xpdr.otn.tp.attributes.OdtuTpnPool;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev200529.xpdr.otn.tp.attributes.OdtuTpnPoolBuilder;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.OtnLinkType;
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
        if (OtnLinkType.OTU4.equals(linkType) || OtnLinkType.OTUC4.equals(linkType)) {
            links = initialiseOtnLinks(nodeA, tpA, nodeZ, tpZ, linkType);
        }
        return new TopologyShard(null, links);
    }

    public static TopologyShard createOtnLinks(
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp
                .Link notifLink,
            OtnLinkType linkType) {

        return new TopologyShard(null, initialiseOtnLinks(notifLink.getATermination().getNodeId(),
            notifLink.getATermination().getTpId(), notifLink.getZTermination().getNodeId(),
            notifLink.getZTermination().getTpId(), linkType));
    }

    public static TopologyShard createOtnLinks(
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp
                .Link notifLink,
            List<Link> supportedOtu4links,
            List<TerminationPoint> supportedTPs,
            OtnLinkType linkType) {

        List<Link> links;
        switch (linkType) {
            case OTU4:
            case OTUC4:
                links = initialiseOtnLinks(
                        notifLink.getATermination().getNodeId(), notifLink.getATermination().getTpId(),
                        notifLink.getZTermination().getNodeId(), notifLink.getZTermination().getTpId(), linkType);
                return new TopologyShard(null, links);
            case ODTU4:
            case ODUC4:
                links = initialiseOtnLinks(
                    notifLink.getATermination().getNodeId(), notifLink.getATermination().getTpId(),
                    notifLink.getZTermination().getNodeId(), notifLink.getZTermination().getTpId(), linkType);
                links.addAll(updateOtnLinkBwParameters(supportedOtu4links, linkType));
                List<TerminationPoint> updatedTPs = new ArrayList<>();
                for (TerminationPoint tp : supportedTPs) {
                    updatedTPs.add(updateTp(tp, true, linkType));
                }
                return new TopologyShard(null, links, updatedTPs);
            default:
                return null;
        }
    }

    public static TopologyShard createOtnLinks(List<Link> suppOtuLinks, List<TerminationPoint> oldTps,
            OtnLinkType linkType) {
        List<Link> links = new ArrayList<>();
        for (Link link : suppOtuLinks) {
            if (link.augmentation(Link1.class) == null) {
                LOG.error("Error with otn parameters of supported link {}", link.getLinkId().getValue());
                continue;
            }
            if (OtnLinkType.ODTU4.equals(linkType)
                && link.augmentation(Link1.class).getAvailableBandwidth().equals(Uint32.valueOf(100000))) {
                links.add(updateOtnLinkBwParameters(link, 0L, 100000L));
            } else if (OtnLinkType.ODUC4.equals(linkType)
                && link.augmentation(Link1.class).getAvailableBandwidth().equals(Uint32.valueOf(400000))) {
                links.add(updateOtnLinkBwParameters(link, 0L, 400000L));
            } else if (OtnLinkType.ODUC3.equals(linkType)
                && link.augmentation(Link1.class).getAvailableBandwidth().equals(Uint32.valueOf(300000))) {
                links.add(updateOtnLinkBwParameters(link, 0L, 300000L));
            } else if (OtnLinkType.ODUC2.equals(linkType)
                && link.augmentation(Link1.class).getAvailableBandwidth().equals(Uint32.valueOf(200000))) {
                links.add(updateOtnLinkBwParameters(link, 0L, 200000L));
            } else {
                LOG.error("Unsupported OTN Link Type link or unsufficient available bandwith: {}",
                    link.getLinkId().getValue());
            }
            //TODO use a Map here instead of multiple else-if-blocks
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
        if (links.size() == 4 && tps.size() == 2) {
            return new TopologyShard(null, links, tps);
        } else {
            return new TopologyShard(null, null, null);
        }
    }

    public static TopologyShard updateOtnLinks(List<Link> suppOduLinks, List<TerminationPoint> oldTps,
        Uint32 serviceRate, Short tribPortNb, Short minTribSlotNb, Short maxTribSlotNb, boolean isDeletion) {
        List<Link> links = new ArrayList<>();
        Long bwIncr;
        switch (serviceRate.intValue()) {
            case 1:
                bwIncr = 1000L;
                break;
            case 10:
                bwIncr = 10000L;
                break;
            case 100:
                bwIncr = 100000L;
                break;
            default:
                LOG.warn("Error with not managed service rate {}", serviceRate.toString());
                return new TopologyShard(null, null, null);
        }
        for (Link link : suppOduLinks) {
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
                tps.add(updateNodeTpTsPool(tp, serviceRate, tribPortNb, minTribSlotNb, maxTribSlotNb, isDeletion));
            }
        }
        if (!links.isEmpty() && !tps.isEmpty()) {
            return new TopologyShard(null, links, tps);
        } else {
            LOG.error("unable to update otn links");
            return new TopologyShard(null, null, null);
        }
    }

    public static TopologyShard updateOtnLinks(List<Link> suppOtuLinks, boolean isDeletion) {
        List<Link> links = new ArrayList<>();
        for (Link link : suppOtuLinks) {
            if (link.augmentation(Link1.class) == null
                || link.augmentation(Link1.class).getAvailableBandwidth() == null
                || link.augmentation(Link1.class).getUsedBandwidth() == null) {
                LOG.error("Error with otn parameters of supported link {}", link.getLinkId().getValue());
            } else {
                if (isDeletion) {
                    links.add(updateOtnLinkBwParameters(link, Long.valueOf(100000), Long.valueOf(0)));
                } else {
                    links.add(updateOtnLinkBwParameters(link, Long.valueOf(0), Long.valueOf(100000)));
                }
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
        OtnLinkType otnLinkType = null;
        for (Link link : suppOtuLinks) {
            if (link.augmentation(Link1.class) != null && link.augmentation(
                    org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.Link1.class) != null) {
                otnLinkType = link.augmentation(
                        org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.Link1.class).getOtnLinkType();
                if (OtnLinkType.OTU4.equals(otnLinkType)) {
                    links.add(updateOtnLinkBwParameters(link, 100000L, 0L));
                } else if (OtnLinkType.OTUC4.equals(otnLinkType)) {
                    links.add(updateOtnLinkBwParameters(link, 400000L, 0L));
                } else {
                    LOG.warn("Unexpected otn-link-type {} for link {}", otnLinkType, link.getLinkId());
                }
            } else {
                LOG.error("Error with otn parameters of supported link {}", link.getLinkId().getValue());
                return new TopologyShard(null, null, null);
            }
        }
        List<TerminationPoint> tps = new ArrayList<>();
        for (TerminationPoint tp : oldTps) {
            tps.add(updateTp(tp, false, linkType));
        }
        if (!links.isEmpty() && !tps.isEmpty()) {
            return new TopologyShard(null, links, tps);
        } else {
            return new TopologyShard(null, null, null);
        }
    }

    private static List<Link> initialiseOtnLinks(String nodeA, String tpA, String nodeZ, String tpZ,
            OtnLinkType linkType) {
        List<Link> links = new ArrayList<>();
        String nodeATopo = formatNodeName(nodeA, tpA);
        String nodeZTopo = formatNodeName(nodeZ, tpZ);
        org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.Link1 tpceLink1
            = new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.Link1Builder()
            .setOtnLinkType(linkType).build();
        Link1Builder otnLink1Bldr = new Link1Builder()
            .setUsedBandwidth(Uint32.valueOf(0));
        switch (linkType) {
            case OTU4:
            case ODTU4:
                otnLink1Bldr.setAvailableBandwidth(Uint32.valueOf(100000));
                break;
            case OTUC4:
            case ODUC4:
                otnLink1Bldr.setAvailableBandwidth(Uint32.valueOf(400000));
                break;
            default:
                LOG.error("unable to set available bandwidth to unknown link type");
                break;
        }
        // create link A-Z
        LinkBuilder ietfLinkAZBldr = TopologyUtils.createLink(nodeATopo, nodeZTopo, tpA, tpZ, linkType.getName());
        ietfLinkAZBldr
            .addAugmentation(tpceLink1)
            .addAugmentation(otnLink1Bldr.build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1Builder(
                        ietfLinkAZBldr.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class))
                        .setLinkType(OpenroadmLinkType.OTNLINK)
                        .setOperationalState(State.InService)
                        .setAdministrativeState(AdminStates.InService)
                        .build());
        links.add(ietfLinkAZBldr.build());
        // create link Z-A
        LinkBuilder ietfLinkZABldr = TopologyUtils.createLink(nodeZTopo, nodeATopo, tpZ, tpA, linkType.getName());
        ietfLinkZABldr
            .addAugmentation(tpceLink1)
            .addAugmentation(otnLink1Bldr.build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1Builder(
                        ietfLinkZABldr.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class))
                        .setLinkType(OpenroadmLinkType.OTNLINK)
                        .setOperationalState(State.InService)
                        .setAdministrativeState(AdminStates.InService)
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
        updatedLinkBldr.addAugmentation(updatedLink1Bldr.build());
        return updatedLinkBldr.build();
    }

    private static List<Link> updateOtnLinkBwParameters(List<Link> supportedLinks, OtnLinkType linkType) {
        LOG.debug("in updateOtnLinkBwParameters with supportedLinks = {}, linkType = {}", supportedLinks, linkType);
        Uint32 usedBw;
        switch (linkType) {
            case ODTU4:
                usedBw = Uint32.valueOf(100000);
                break;
            case ODUC4:
                usedBw = Uint32.valueOf(400000);
                break;
            default:
                usedBw = Uint32.valueOf(0);
                break;
        }
        List<Link> updatedlinks = new ArrayList<>();
        for (Link link : supportedLinks) {
            LinkBuilder updatedLinkBldr = new LinkBuilder(link);
            updatedLinkBldr.addAugmentation(new Link1Builder(link.augmentation(Link1.class))
                .setAvailableBandwidth(Uint32.valueOf(0))
                .setUsedBandwidth(usedBw)
                .build());
            updatedlinks.add(updatedLinkBldr.build());
        }
        return updatedlinks;
    }

    private static TerminationPoint updateTp(TerminationPoint originalTp, boolean addingTsTpnPoolTermination,
            OtnLinkType linkType) {
        LOG.debug("in updateTp");
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(originalTp);
        TerminationPoint1Builder otnTp1Bldr = new TerminationPoint1Builder(
            tpBldr.augmentation(TerminationPoint1.class));
        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder(otnTp1Bldr
            .getXpdrTpPortConnectionAttributes());
        if (addingTsTpnPoolTermination) {
            List<Uint16> tsPool = new ArrayList<>();
            for (int i = 1; i <= NB_TRIB_SLOTS; i++) {
                tsPool.add(Uint16.valueOf(i));
            }
            xtpcaBldr.setTsPool(tsPool);
            List<Uint16> tpnPool = new ArrayList<>();
            int nbTribPort = NB_TRIB_PORTS;
            if (OtnLinkType.ODUC4.equals(linkType)) {
                nbTribPort = 4;
            }
            for (int i = 1; i <= nbTribPort; i++) {
                tpnPool.add(Uint16.valueOf(i));
            }
            OdtuTpnPool oduTpnPool = new OdtuTpnPoolBuilder().setOdtuType(ODTU4TsAllocated.class)
                .setTpnPool(tpnPool).build();
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
        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder(
            tpBldr.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes());
        List<Uint16> tsPool = new ArrayList<>(xtpcaBldr.getTsPool());
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
        List<Uint16> tpnPool;
        List<OdtuTpnPool> odtuTpnPoolValues = new ArrayList<>(xtpcaBldr.getOdtuTpnPool().values());
        if (odtuTpnPoolValues.get(0).getTpnPool() != null) {
            tpnPool = new ArrayList<>(odtuTpnPoolValues.get(0).getTpnPool());
            if (isDeletion) {
                tpnPool.add(Uint16.valueOf(tribPortNb));
            } else {
                tpnPool.remove(Uint16.valueOf(tribPortNb));
            }
        } else {
            tpnPool = new ArrayList<>();
        }
        Class<? extends OdtuTypeIdentity> odtuType = null;
        switch (serviceRate.intValue()) {
            case 1:
            case 10:
                odtuType = ODTU4TsAllocated.class;
                break;
            case 100:
                odtuType = ODTUCnTs.class;
                break;
            default:
                LOG.warn("Unable to set the odtu-type");
                break;
        }
        OdtuTpnPool odtuTpnPool = new OdtuTpnPoolBuilder().setOdtuType(odtuType)
            .setTpnPool(tpnPool).build();
        xtpcaBldr.setOdtuTpnPool(ImmutableMap.of(odtuTpnPool.key(),odtuTpnPool));

        tpBldr.addAugmentation(new TerminationPoint1Builder(tp.augmentation(TerminationPoint1.class))
                .setXpdrTpPortConnectionAttributes(xtpcaBldr.build()).build());
        return tpBldr.build();
    }

    private static Map<Integer, OtnTopoNode> convertPortMappingToOtnNodeList(Nodes mappingNode) {
        List<Mapping> networkMappings = mappingNode.nonnullMapping().values()
                .stream().filter(k -> k.getLogicalConnectionPoint()
            .contains("NETWORK")).collect(Collectors.toList());
        Map<Integer, OtnTopoNode> xpdrMap = new HashMap<>();
        for (Mapping mapping : networkMappings) {
            Integer xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
            if (!xpdrMap.containsKey(xpdrNb)) {
                List<Mapping> xpdrNetMaps = mappingNode.nonnullMapping().values()
                        .stream().filter(k -> k.getLogicalConnectionPoint()
                    .contains("XPDR" + xpdrNb + NETWORK)).collect(Collectors.toList());
                List<Mapping> xpdrClMaps = mappingNode.nonnullMapping().values()
                        .stream().filter(k -> k.getLogicalConnectionPoint()
                    .contains("XPDR" + xpdrNb + CLIENT)).collect(Collectors.toList());
                OtnTopoNode otnNode = null;
                if (mapping.getXponderType() != null) {
                    otnNode = new OtnTopoNode(mappingNode.getNodeId(), mappingNode.getNodeInfo().getNodeClli(), xpdrNb,
                        mapping.getXponderType(), fillConnectionMapLcp(xpdrNetMaps), fillConnectionMapLcp(xpdrClMaps),
                        xpdrNetMaps, xpdrClMaps);
                } else {
                    otnNode = new OtnTopoNode(mappingNode.getNodeId(), mappingNode.getNodeInfo().getNodeClli(), xpdrNb,
                        XpdrNodeTypes.Tpdr, fillConnectionMapLcp(xpdrNetMaps), fillConnectionMapLcp(xpdrClMaps),
                        xpdrNetMaps, xpdrClMaps);
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
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1 ocnNodeAug =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1Builder()
                    .setNodeType(OpenroadmNodeType.TPDR)
                    .setOperationalState(State.InService)
                    .setAdministrativeState(AdminStates.InService)
                    .build();
        // create ietf node augmentation to add TP list
        Map<TerminationPointKey,TerminationPoint> tpMap = new HashMap<>();
        // creation of tps
        createTP(tpMap, node, OpenroadmTpType.XPONDERCLIENT, false);
        createTP(tpMap, node, OpenroadmTpType.XPONDERNETWORK, true);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpMap)
            .build();

        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(otnNodeAug)
            .addAugmentation(ocnNodeAug)
            .addAugmentation(ietfNodeAug)
            .build();
    }

    private static Node createMuxpdr(OtnTopoNode node) {
        // create otn-topology node augmentation
        // TODO: will need to be completed
        TpBandwidthSharing tpBwSh = new TpBandwidthSharingBuilder().build();
        XpdrAttributes xpdrAttr = new XpdrAttributesBuilder()
            .setXpdrNumber(Uint16.valueOf(node.getXpdrNb()))
            .build();

        Map<NonBlockingListKey, NonBlockingList> nbMap = new HashMap<>();
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
            nbMap.put(nbl.key(),nbl);
        }
        OduSwitchingPools oduSwitchPool = new OduSwitchingPoolsBuilder()
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .setSwitchingPoolType(SwitchingPoolTypes.NonBlocking)
            .setNonBlockingList(nbMap)
            .build();
        SwitchingPools switchingPools = new SwitchingPoolsBuilder()
            .setOduSwitchingPools(Map.of(oduSwitchPool.key(),oduSwitchPool))
            .build();
        Node1 otnNodeAug = new Node1Builder()
            .setTpBandwidthSharing(tpBwSh)
            .setXpdrAttributes(xpdrAttr)
            .setSwitchingPools(switchingPools)
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1 ocnNodeAug =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1Builder()
                    .setNodeType(OpenroadmNodeType.MUXPDR)
                    .setAdministrativeState(AdminStates.InService)
                    .setOperationalState(State.InService)
                    .build();

        // create ietf node augmentation to add TP list
        Map<TerminationPointKey, TerminationPoint> tpMap = new HashMap<>();
        // creation of tps
        createTP(tpMap, node, OpenroadmTpType.XPONDERCLIENT, true);
        createTP(tpMap, node, OpenroadmTpType.XPONDERNETWORK, true);

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpMap)
            .build();

        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(otnNodeAug)
            .addAugmentation(ocnNodeAug)
            .addAugmentation(ietfNodeAug)
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
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1 ocnNodeAug =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1Builder()
                    .setNodeType(OpenroadmNodeType.SWITCH)
                    .setOperationalState(State.InService)
                    .setAdministrativeState(AdminStates.InService)
                    .build();

        // create ietf node augmentation to add TP list
        Map<TerminationPointKey, TerminationPoint> tpMap = new HashMap<>();
        // creation of tps
        createTP(tpMap, node, OpenroadmTpType.XPONDERCLIENT, true);
        createTP(tpMap, node, OpenroadmTpType.XPONDERNETWORK, true);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 ietfNodeAug =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
            .setTerminationPoint(tpMap)
            .build();

        // return ietfNode
        return new NodeBuilder()
            .setNodeId(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
            .withKey(new NodeKey(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb())))
            .setSupportingNode(createSupportingNodes(node))
            .addAugmentation(otnNodeAug)
            .addAugmentation(ocnNodeAug)
            .addAugmentation(ietfNodeAug)
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
            if (mapping.getSupportedInterfaceCapability() != null) {
                XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder();
                for (Class<? extends SupportedIfCapability> supInterCapa : mapping.getSupportedInterfaceCapability()) {
                    SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                        .withKey(new SupportedInterfaceCapabilityKey(supInterCapa))
                        .setIfCapType(supInterCapa)
                        .build();
                    supIfMap.put(supIfCapa.key(), supIfCapa);
                }
                TpSupportedInterfaces tpSupIf = new TpSupportedInterfacesBuilder()
                    .setSupportedInterfaceCapability(supIfMap)
                    .build();
                otnTp1Bldr.setTpSupportedInterfaces(tpSupIf);
                //TODO: It is not logical to assign a priori one of the possible rate to the TP.
                //Would be worth assigning per default "unallocated" at the tp creation step,
                //and updating it with correct rate when it supports a specific service.
                if (withRate) {
                    xtpcaBldr.setRate(fixRate(mapping.getSupportedInterfaceCapability()));
                    otnTp1Bldr.setXpdrTpPortConnectionAttributes(xtpcaBldr.build());
                }
            } else {
                LOG.warn("mapping {} of node {} has no if-cap-type", mapping.getLogicalConnectionPoint(),
                    node.getNodeId());
            }
            org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.TerminationPoint1Builder tpceTp1Bldr =
                new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.TerminationPoint1Builder();
            TpId tpId = new TpId(mapping.getLogicalConnectionPoint());
            setclientNwTpAttr(tpMap, node, tpId, tpType, otnTp1Bldr.build(), tpceTp1Bldr, mapping);
        }
    }

    private static void setclientNwTpAttr(Map<TerminationPointKey, TerminationPoint> tpMap, OtnTopoNode node, TpId tpId,
            OpenroadmTpType tpType, TerminationPoint1 otnTp1,
            org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.TerminationPoint1Builder tpceTp1Bldr,
            Mapping mapping) {
        switch (tpType) {
            case XPONDERNETWORK:
                if (node.getXpdrNetConnectionMap().get(tpId.getValue()) != null) {
                    tpceTp1Bldr.setAssociatedConnectionMapPort(node.getXpdrNetConnectionMap().get(tpId.getValue()));
                }
                SupportingTerminationPoint stp = new SupportingTerminationPointBuilder()
                    .setNetworkRef(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))
                    .setNodeRef(new NodeId(node.getNodeId() + XPDR + node.getXpdrNb()))
                    .setTpRef(tpId)
                    .build();
                TerminationPoint ietfTpNw = buildIetfTp(tpceTp1Bldr, otnTp1, tpType, tpId, Map.of(stp.key(), stp),
                    mapping);
                tpMap.put(ietfTpNw.key(),ietfTpNw);
                break;
            case XPONDERCLIENT:
                if (node.getXpdrCliConnectionMap().get(tpId.getValue()) != null) {
                    tpceTp1Bldr.setAssociatedConnectionMapPort(node.getXpdrCliConnectionMap().get(tpId.getValue()));
                }
                TerminationPoint ietfTpCl = buildIetfTp(tpceTp1Bldr, otnTp1, tpType, tpId, null, mapping);
                tpMap.put(ietfTpCl.key(),ietfTpCl);
                break;
            default:
                LOG.error("Undefined tpType for Termination point {} of {}", tpId.getValue(), node.getNodeId());
                break;
        }
    }

    private static Class<? extends OduRateIdentity> fixRate(List<Class<? extends SupportedIfCapability>> list) {
        Map<String, Class<? extends OduRateIdentity>> rateMap = Map.of(
            "If100GEODU4", ODU4.class,
            "IfOCHOTU4ODU4", ODU4.class,
            "If1GEODU0", ODU0.class,
            "If10GEODU2", ODU2.class,
            "If10GEODU2e", ODU2e.class);
        for (Class<? extends SupportedIfCapability> class1 : list) {
            if (rateMap.containsKey(class1.getSimpleName())) {
                return rateMap.get(class1.getSimpleName());
            }
        }
        return null;
    }

    private static Map<SupportingNodeKey,SupportingNode> createSupportingNodes(OtnTopoNode node) {
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
        Map<SupportingNodeKey,SupportingNode> suppNodeMap = new HashMap<>();
        suppNodeMap.put(suppNode1.key(),suppNode1);
        suppNodeMap.put(suppNode2.key(),suppNode2);
        suppNodeMap.put(suppNode3.key(),suppNode3);
        return suppNodeMap;
    }

    private static TerminationPoint buildIetfTp(
        org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123.TerminationPoint1Builder tpceTp1Bldr,
        TerminationPoint1 otnTp1, OpenroadmTpType tpType, TpId tpId,
        Map<SupportingTerminationPointKey, SupportingTerminationPoint> supportTpMap, Mapping mapping) {

        TerminationPointBuilder ietfTpBldr = new TerminationPointBuilder();
        if (tpceTp1Bldr.getAssociatedConnectionMapPort() != null) {
            ietfTpBldr.addAugmentation(tpceTp1Bldr.build());
        }
        if (supportTpMap != null) {
            ietfTpBldr.setSupportingTerminationPoint(supportTpMap);
        }
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1 ocnTp =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder()
                    .setTpType(tpType)
                    .setAdministrativeState(TopologyUtils.setNetworkAdminState(mapping.getPortAdminState()))
                    .setOperationalState(TopologyUtils.setNetworkOperState(mapping.getPortOperState()))
                    .build();

        return ietfTpBldr.setTpId(tpId)
                .withKey(new TerminationPointKey(tpId))
                .addAugmentation(otnTp1)
                .addAugmentation(ocnTp)
                .build();
    }

    private static String formatNodeName(String nodeName, String tpName) {
        return nodeName.contains(XPDR)
                ? nodeName
                : new StringBuilder(nodeName).append("-").append(tpName.split("-")[0]).toString();
    }
}
