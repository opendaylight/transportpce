/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev170929.span.attributes.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.link.oms.attributes.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MapUtils {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(MapUtils.class);

    private MapUtils() {
    }

    public static String getCLLI(Node node) {
        // TODO STUB retrieve CLLI from node. for now it is supporting node ID of the first supp node
        return node.getSupportingNode().get(0).getNodeRef().getValue();
    }

    public static List<Long> getSRLG(Link link) {
        List<Long> srlgList = new ArrayList<Long>();
        Span span = getOmsAttributesSpan(link);
        if (span != null) {
            List<LinkConcatenation> linkList = span.getLinkConcatenation();
            for (LinkConcatenation lc : linkList) {
                srlgList.add(lc.getSRLGId());
            }
        } else {
            LOG.error("MapUtils: No LinkConcatenation for link : {}", link);
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
        Link1 link1 = null;
        Span tempSpan = null;
        link1 = link.augmentation(Link1.class);
        if (link1 == null) {
            LOG.error("MapUtils: No Link augmentation available. {}", link.getLinkId().getValue());
            return null;
        }
        try {
            tempSpan = link1.getOMSAttributes().getSpan();
            if (tempSpan == null) {
                LOG.error("MapUtils: No Link getOMSAttributes available. {}", link.getLinkId().getValue());
                return null;
            }
        } catch (NullPointerException e) {
            LOG.error("MapUtils: No Link getOMSAttributes available. {}", link.getLinkId().getValue());
            return null;
        }
        return tempSpan;
    }

}
