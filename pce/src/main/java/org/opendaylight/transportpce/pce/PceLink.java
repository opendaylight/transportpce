/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceLink {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);

    ///////////////////////// LINKS ////////////////////
    /*
     * extension of Link to include constraints and Graph weight
     */
    // double capacity = 1;

    double weight = 0;

    private boolean isValid = true;

    // this member is for XPONDER INPUT/OUTPUT links.
    // it keeps name of client corresponding to NETWORK TP
    private String client = "";

    private final LinkId linkId;
    private final OpenroadmLinkType linkType;
    private final NodeId sourceId;
    private final NodeId destId;
    private final Object sourceTP;
    private final Object destTP;
    private final LinkId oppositeLink;
    private final Long latency;

    public PceLink(Link link) {
        LOG.debug("PceLink: : PceLink start ");

        this.linkId = link.getLinkId();

        this.sourceId = link.getSource().getSourceNode();
        this.destId = link.getDestination().getDestNode();

        this.sourceTP = link.getSource().getSourceTp();
        this.destTP = link.getDestination().getDestTp();

        this.linkType = calcType(link);

        this.oppositeLink = calcOpposite(link);
        this.latency = calcLatency(link);

        LOG.debug("PceLink: created PceLink  {}", toString());
    }

    private OpenroadmLinkType calcType(Link link) {
        Link1 link1 = null;
        OpenroadmLinkType tmplType = null;

        // ID and type
        link1 = link.augmentation(Link1.class);
        if (link1 == null) {
            this.isValid = false;
            LOG.error("PceLink: No Link augmentation available. Link is ignored {}", this.linkId);
            return null;
        }

        tmplType = link1.getLinkType();
        if (tmplType == null) {
            this.isValid = false;
            LOG.error("PceLink: No Link type available. Link is ignored {}", this.linkId);
            return null;
        }
        return tmplType;
    }

    private LinkId calcOpposite(Link link) {
        // opposite link
        LinkId tmpoppositeLink = null;
        org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1 linkOpposite = link
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1.class);
        tmpoppositeLink = linkOpposite.getOppositeLink();
        LOG.debug("PceLink: reading oppositeLink.  {}", linkOpposite.toString());
        if (tmpoppositeLink == null) {
            this.isValid = false;
            LOG.error("PceLink: Error reading oppositeLink. Link is ignored {}", this.linkId);
            return null;
        }
        return tmpoppositeLink;
    }

    private Long calcLatency(Link link) {
        Long tmplatency = (long)0;
        Link1 link1 = null;
        // latency
        link1 = link.augmentation(Link1.class);
        tmplatency = link1.getLinkLatency();
        if (tmplatency == null) {
            tmplatency = (long) 0;
        }
        return tmplatency;

    }


    public LinkId getOppositeLink() {
        return this.oppositeLink;
    }

    public Object getSourceTP() {
        return this.sourceTP;
    }

    public Object getDestTP() {
        return this.destTP;
    }

    public OpenroadmLinkType getLinkType() {
        return this.linkType;
    }

    public LinkId getLinkId() {
        return this.linkId;
    }

    public NodeId getSourceId() {
        return this.sourceId;
    }

    public NodeId getDestId() {
        return this.destId;
    }

    public String getClient() {
        return this.client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    // Double for transformer of JUNG graph
    public Double getLatency() {
        return this.latency.doubleValue();
    }

    public boolean isValid() {
        if ((this.linkId == null) || (this.linkType == null) || (this.oppositeLink == null)) {
            this.isValid = false;
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", this.linkId);
        }
        if ((this.sourceId == null) || (this.destId == null) || (this.sourceTP == null) || (this.destTP == null)) {
            this.isValid = false;
            LOG.error("PceLink: No Link source or destination is available. Link is ignored {}", this.linkId);
        }

        return this.isValid;
    }

    @Override
    public String toString() {
        return "PceLink type=" + this.linkType + " ID=" + this.linkId.toString() + " latecy=" + this.latency;
    }

}
