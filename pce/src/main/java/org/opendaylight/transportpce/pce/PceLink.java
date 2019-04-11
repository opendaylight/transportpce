/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.link.oms.attributes.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
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
    private final List<Long> srlg;
    private final double osnr;
    private final Span omsAttributesSpan;

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

        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            this.omsAttributesSpan = MapUtils.getOmsAttributesSpan(link);
            this.srlg = MapUtils.getSRLG(link);
            this.osnr = retrieveOSNR();
        } else {
            this.omsAttributesSpan = null;
            this.srlg = null;
            this.osnr = 0L;
        }

        LOG.debug("PceLink: created PceLink  {}", toString());
    }

    private OpenroadmLinkType calcType(Link link) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.@Nullable Link1 link1 = null;
        OpenroadmLinkType tmplType = null;

        // ID and type
        link1 = link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
            .Link1.class);
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
        Link1 linkOpposite = link.augmentation(Link1.class);
        if (linkOpposite.getOppositeLink() != null) {
            tmpoppositeLink = linkOpposite.getOppositeLink();
        } else {
            LOG.error("link {} has no opposite link", link.getLinkId().getValue());
        }
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

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public double retrieveOSNR() {
        double sum = 0;        // sum of 1 over the span OSNRs (linear units)
        double linkOsnrDb;     // link OSNR, in dB
        double linkOsnrLu;     // link OSNR, in dB
        double spanOsnrDb;     // span OSNR, in dB
        double spanOsnrLu;     // span OSNR, in linear units
        double ampNoise = 5.5; // default amplifier noise value, in dB
        double loss;           // fiber span measured loss, in dB
        double power;          // launch power, in dB
        double constantA = 38.97293;
        double constantB = 0.72782;
        double constantC = -0.532331;
        double constactD = -0.019549;
        double upperBoundOSNR = 33;
        double lowerBoundOSNR = 0.1;

        if (omsAttributesSpan ==  null) {
            return 0L; // indicates no data or N/A
        }
        loss = omsAttributesSpan.getSpanlossCurrent().getValue().doubleValue();
        switch (omsAttributesSpan.getLinkConcatenation().get(0).getFiberType()) {
            case Smf:
                power = 2;
                break;

            case Eleaf:
                power = 1;
                break;

            case Oleaf:
                power = 0;
                break;

            case Dsf:
                power = 0;
                break;

            case Truewave:
                power = 0;
                break;

            case Truewavec:
                power = -1;
                break;

            case NzDsf:
                power = 0;
                break;

            case Ull:
                power = 0;
                break;

            default:
                power = 0;
                break;
        }
        spanOsnrDb = constantA + constantB * power + constantC * loss + constactD * power * loss;
        if (spanOsnrDb > upperBoundOSNR) {
            spanOsnrDb =  upperBoundOSNR;
        } else if (spanOsnrDb < lowerBoundOSNR) {
            spanOsnrDb = lowerBoundOSNR;
        }
        spanOsnrLu = Math.pow(10, (spanOsnrDb / 10.0));
        sum = PceConstraints.CONST_OSNR / spanOsnrLu;
        linkOsnrLu = sum;
        //link_OSNR_dB = 10 * Math.log10(1 / sum);
        LOG.debug("In retrieveOSNR: link OSNR is {} dB", linkOsnrLu);
        return linkOsnrLu;
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
