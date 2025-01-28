/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.oms.attributes.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.link.concatenation.LinkConcatenationKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SupportingLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MapUtils {
    private static final String MAP_UTILS_NO_LINK_AUGMENTATION_AVAILABLE_MSG =
            "MapUtils: No Link augmentation available. {}";
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(MapUtils.class);

    private MapUtils() {
    }

    public static void mapDiversityConstraints(List<Node> allNodes, List<Link> allLinks,
            PceConstraints pceHardConstraints) {
        List<String> excClliNodes = pceHardConstraints.getExcludeClliNodes();
        List<String> excNodes = pceHardConstraints.getExcludeNodes();
        List<String> excSrlgLinks = pceHardConstraints.getExcludeSrlgLinks();

        LOG.info("mapDiversityConstraints before : ExcludeClliNodes {} \n ExcludeNodes {} \n ExcludeSrlgLinks {}",
                excClliNodes, excNodes, excSrlgLinks);

        for (Node node : allNodes) {
            if (excClliNodes.contains(node.getNodeId().getValue())) {
                LOG.debug("mapDiversityConstraints setExcludeCLLI for node {}", node.getNodeId().getValue());
                pceHardConstraints.setExcludeCLLI(List.of(getCLLI(node)));
            }

            if (excNodes.contains(node.getNodeId().getValue())) {
                LOG.debug("mapDiversityConstraints setExcludeSupNodes for node {}", node.getNodeId().getValue());
                pceHardConstraints.setExcludeSupNodes(Arrays.asList(getSupNetworkNode(node)));
            }
        }

        for (Link link : allLinks) {
            if (excSrlgLinks.contains(link.getLinkId().getValue())) {
                // zero SRLG means not populated as not OMS link
                List<Long> srlg = null;
                if (calcType(link) == OpenroadmLinkType.ROADMTOROADM) {
                    srlg = getSRLG(link);
                    if (!srlg.isEmpty()) {
                        pceHardConstraints.setExcludeSRLG(srlg);
                        LOG.debug("mapDiversityConstraints setExcludeSRLG {} for link {}",
                                srlg, link.getLinkId().getValue());
                    }
                }
            }
        }

        LOG.info("mapDiversityConstraints after : ExcludeCLLI {} \n ExcludeSupNodes {} \n ExcludeSRLG {}",
                pceHardConstraints.getExcludeCLLI(),
                pceHardConstraints.getExcludeSupNodes(),
                pceHardConstraints.getExcludeSRLG());

    }

    public static String getCLLI(Node node) {
        // TODO STUB retrieve CLLI from node. for now it is supporting node ID of the first supp node
        return node.nonnullSupportingNode().values().iterator().next().getNodeRef().getValue();
    }

    public static List<Long> getSRLG(Link link) {
        Span omsAttributesSpan = getOmsAttributesSpan(link);
        if (omsAttributesSpan == null) {
            LOG.debug("No concatenation for this link");
            return new ArrayList<>();
        }
        List<Long> srlgList = new ArrayList<>();
        Map<LinkConcatenationKey, LinkConcatenation> linkList = omsAttributesSpan.nonnullLinkConcatenation();
        for (LinkConcatenation lc : linkList.values()) {
            if (lc != null && lc.getSRLGId() != null) {
                srlgList.add(lc.getSRLGId().toJava());
            }
        }
        return srlgList;
    }

    public static List<Long> getSRLGfromLink(Link link) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1 linkC = link
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class);
        if (linkC == null) {
            LOG.error(MAP_UTILS_NO_LINK_AUGMENTATION_AVAILABLE_MSG, link.getLinkId().getValue());
            return new ArrayList<>();
        }
        List<Long> srlgList = new ArrayList<>();
        for (LinkConcatenation lc : linkC.nonnullLinkConcatenation().values()) {
            if (lc != null && lc.getSRLGId() != null) {
                srlgList.add(lc.getSRLGId().toJava());
            } else {
                LOG.debug("No concatenation or SLRG id for this link");
            }
        }
        return srlgList;
    }

    public static String getSupNetworkNode(Node node) {
        for (SupportingNode snode : node.nonnullSupportingNode().values()) {
            if (StringConstants.OPENROADM_NETWORK.equals(snode.getNetworkRef().getValue())) {
                return snode.getNodeRef().getValue();
            }
        }
        return null;
    }

    public static String getSupClliNode(Node node) {
        for (SupportingNode snode : node.nonnullSupportingNode().values()) {
            if (StringConstants.CLLI_NETWORK.equals(snode.getNetworkRef().getValue())) {
                return snode.getNodeRef().getValue();
            }
        }
        return null;
    }

    public static SortedMap<String, String> getAllSupNode(Node node) {
        TreeMap<String, String> allSupNodes = new TreeMap<>();
        for (SupportingNode supnode : node.nonnullSupportingNode().values()) {
            allSupNodes.put(supnode.getNetworkRef().getValue(),
                    supnode.getNodeRef().getValue());
        }
        return allSupNodes;
    }

    public static String getSupLink(Link link) {
        Iterator<SupportingLink> supportingLinkIterator = link.nonnullSupportingLink().values().iterator();
        if (!supportingLinkIterator.hasNext()) {
            return "";
        }
        SupportingLink first = supportingLinkIterator.next();
        if (first == null || first.getLinkRef() == null) {
            return "";
        }
        return first.getLinkRef().toString();
    }


    public static Long getAvailableBandwidth(Link link) {
        if (link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
            .Link1.class) != null
            && link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                .Link1.class).getAvailableBandwidth() != null) {
            return link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                .Link1.class).getAvailableBandwidth().toJava();
        } else {
            LOG.warn("MapUtils: no Available Bandwidth available for link {}", link.getLinkId());
            return 0L;
        }
    }

    public static Long getUsedBandwidth(Link link) {
        if (link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
            .Link1.class) != null
            && link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                .Link1.class).getUsedBandwidth() != null) {
            return link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                .Link1.class).getUsedBandwidth().toJava();
        } else {
            LOG.warn("MapUtils: no Available Bandwidth available for link {}", link.getLinkId());
            return 0L;
        }
    }

    public static OpenroadmLinkType calcType(Link link) {
        Link1 link1 = null;
        OpenroadmLinkType tmplType = null;
        // ID and type
        link1 = link.augmentation(Link1.class);
        if (link1 == null) {
            LOG.error(MAP_UTILS_NO_LINK_AUGMENTATION_AVAILABLE_MSG, link.getLinkId().getValue());
            return null;
        }

        tmplType = link1.getLinkType();

        if (tmplType == null) {
            LOG.error("MapUtils: No Link type available. {}", link.getLinkId().getValue());
            return null;
        }
        return tmplType;
    }

    public static Span getOmsAttributesSpan(Link link) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Link1 link1 = null;
        link1 =
            link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Link1.class);

        if (link1 == null) {
            LOG.error(MAP_UTILS_NO_LINK_AUGMENTATION_AVAILABLE_MSG, link.getLinkId().getValue());
            return null;
        }
        if (link1.getOMSAttributes() == null) {
            LOG.error("MapUtils: No Link getOMSAttributes available. {}", link.getLinkId().getValue());
            return null;
        }
        return link1.getOMSAttributes().getSpan();
    }

    public static LinkId extractOppositeLink(Link link) {
        var linkOpposite
            = link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class);
        if (linkOpposite == null) {
            LOG.error("No opposite link augmentation for network link {}", link);
            return null;
        }
        LOG.debug("PceLink: reading oppositeLink.  {}", linkOpposite);
        LinkId tmpoppositeLink = linkOpposite.getOppositeLink();
        if (tmpoppositeLink == null) {
            LOG.error("PceLink: Error reading oppositeLink. Link is ignored {}", link.getLinkId().getValue());
            return null;
        }
        return tmpoppositeLink;
    }

}
