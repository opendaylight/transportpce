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
import java.util.List;

import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenation;

import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.oms.attributes.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MapUtils {
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
                excClliNodes.toString(), excNodes.toString(), excSrlgLinks.toString());

        for (Node node : allNodes) {
            if (excClliNodes.contains(node.getNodeId().getValue())) {
                LOG.debug("mapDiversityConstraints setExcludeCLLI for node {}", node.getNodeId().getValue());
                pceHardConstraints.setExcludeCLLI(Arrays.asList(getCLLI(node)));
            }

            if (excNodes.contains(node.getNodeId().getValue())) {
                LOG.debug("mapDiversityConstraints setExcludeSupNodes for node {}", node.getNodeId().getValue());
                pceHardConstraints.setExcludeSupNodes(Arrays.asList(getSupNode(node)));
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
                                srlg.toString(), link.getLinkId().getValue());
                    }
                }
            }
        }

        LOG.info("mapDiversityConstraints after : ExcludeCLLI {} \n ExcludeSupNodes {} \n ExcludeSRLG {}",
                pceHardConstraints.getExcludeCLLI().toString(),
                pceHardConstraints.getExcludeSupNodes().toString(),
                pceHardConstraints.getExcludeSRLG().toString());

    }

    public static String getCLLI(Node node) {
        // TODO STUB retrieve CLLI from node. for now it is supporting node ID of the first supp node
        return node.getSupportingNode().get(0).getNodeRef().getValue();
    }

    public static List<Long> getSRLG(Link link) {
        List<Long> srlgList = new ArrayList<Long>();
        try {
            List<LinkConcatenation> linkList = getOmsAttributesSpan(link).getLinkConcatenation();
            for (LinkConcatenation lc : linkList) {
                srlgList.add(lc.getSRLGId());
            }
        } catch (NullPointerException e) {
            LOG.debug("No concatenation for this link");
        }
        return srlgList;
    }

    public static String getSupNode(Node node) {
        // TODO: supporting IDs exist as a List. this code takes just the first element
        return node.getSupportingNode().get(0).getNodeRef().getValue();
    }

    public static OpenroadmLinkType calcType(Link link) {
        Link1 link1 = null;
        OpenroadmLinkType tmplType = null;
        // ID and type
        link1 = link.augmentation(Link1.class);
        if (link1 == null) {
            LOG.error("MapUtils: No Link augmentation available. {}", link.getLinkId().getValue());
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
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1 link1 = null;
        Span tempSpan = null;
        link1 =
            link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1.class);

        if (link1 == null) {
            LOG.error("MapUtils: No Link augmentation available. {}", link.getLinkId().getValue());
        }
        try {
            tempSpan = link1.getOMSAttributes().getSpan();
        }
        catch (NullPointerException e) {
            LOG.error("MapUtils: No Link getOMSAttributes available. {}", link.getLinkId().getValue());
        }

        return tempSpan;
    }

    public static LinkId extractOppositeLink(Link link) {
        LinkId tmpoppositeLink = null;
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1 linkOpposite
            = link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class);
        tmpoppositeLink = linkOpposite.getOppositeLink();
        LOG.debug("PceLink: reading oppositeLink.  {}", linkOpposite.toString());
        if (tmpoppositeLink == null) {
            LOG.error("PceLink: Error reading oppositeLink. Link is ignored {}", link.getLinkId().getValue());
            return null;
        }
        return tmpoppositeLink;
    }


}
