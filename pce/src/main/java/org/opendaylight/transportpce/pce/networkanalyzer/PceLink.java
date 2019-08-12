/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.List;

import org.opendaylight.transportpce.pce.constraints.PceConstraints;
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
    private static final Logger LOG = LoggerFactory.getLogger(PceLink.class);

    ///////////////////////// LINKS ////////////////////
    /*
     * extension of Link to include constraints and Graph weight
     */

    double weight = 0;

    private boolean isValid = true;

    // this member is for XPONDER INPUT/OUTPUT links.
    // it keeps name of client correcponding to NETWORK TP
    private String client = "";

    private final LinkId linkId;
    private final OpenroadmLinkType linkType;
    private final NodeId sourceId;
    private final NodeId destId;
    private final Object sourceTP;
    private final Object destTP;
    private final String sourceSupNodeId;
    private final String destSupNodeId;
    private final String sourceCLLI;
    private final String destCLLI;
    private final LinkId oppositeLink;
    private final Long latency;
    private final List<Long> srlgList;
    private final double osnr;
    private final Span omsAttributesSpan;

    public PceLink(Link link, PceNode source, PceNode dest) {
        LOG.debug("PceLink: : PceLink start ");

        this.linkId = link.getLinkId();

        this.sourceId = link.getSource().getSourceNode();
        this.destId = link.getDestination().getDestNode();

        this.sourceTP = link.getSource().getSourceTp();
        this.destTP = link.getDestination().getDestTp();

        this.sourceSupNodeId = source.getSupNodeIdPceNode();
        this.destSupNodeId = dest.getSupNodeIdPceNode();

        this.sourceCLLI = source.getCLLI();
        this.destCLLI = dest.getCLLI();

        this.linkType = MapUtils.calcType(link);

        this.oppositeLink = calcOpposite(link);
        this.latency = calcLatency(link);

        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            this.omsAttributesSpan = MapUtils.getOmsAttributesSpan(link);
            this.srlgList = MapUtils.getSRLG(link);
            this.osnr = retrieveOSNR();
        } else {
            this.omsAttributesSpan = null;
            this.srlgList = null;
            this.osnr = 0.0;
        }


        LOG.debug("PceLink: created PceLink  {}", toString());
    }

    /*private OpenroadmLinkType calcType(Link link) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.@Nullable Link1 link1 = null;
        OpenroadmLinkType tmplinkType = null;

        // ID and type
        link1 = link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
            .Link1.class);
        if (link1 == null) {
            this.isValid = false;
            LOG.error("PceLink: No Link augmentation available. Link is ignored {}", this.linkId);
            return null;
        }

        tmplinkType = link1.getLinkType();
        if (tmplinkType == null) {
            this.isValid = false;
            LOG.error("PceLink: No Link type available. Link is ignored {}", this.linkId);
            return null;
        }
        return tmplinkType;
    }*/

    private LinkId calcOpposite(Link link) {
        // opposite link

        LinkId tmpoppositeLink = MapUtils.extractOppositeLink(link);
        if (tmpoppositeLink == null) {
            LOG.error("PceLink: Error calcOpposite. Link is ignored {}", link.getLinkId().getValue());
            isValid = false;
        }
        return tmpoppositeLink;
    }

    private Long calcLatency(Link link) {
        Long tmplatency = 1L;
        Link1 link1 = null;
        // latency
        link1 = link.augmentation(Link1.class);
        try {
            tmplatency = link1.getLinkLatency();
        } catch (NullPointerException e) {
            LOG.debug("the latency does not exist for this link");
        }
        return tmplatency;
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public double retrieveOSNR() {
        // sum of 1 over the span OSNRs (linear units)
        double sum = 0;
        // link OSNR, in dB
        //double linkOsnrDb;
        // link OSNR, in dB
        double linkOsnrLu;
        // span OSNR, in dB
        double spanOsnrDb;
        // span OSNR, in linear units
        double spanOsnrLu;
        // default amplifier noise value, in dB
        //double ampNoise = 5.5;
        // fiber span measured loss, in dB
        double loss;
        // launch power, in dB
        double power;
        double constantA = 38.97293;
        double constantB = 0.72782;
        double constantC = -0.532331;
        double constactD = -0.019549;
        double upperBoundosnr = 33;
        double lowerBoundosnr = 0.1;

        if (omsAttributesSpan ==  null) {
            // indicates no data or N/A
            return 0L;
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
        if (spanOsnrDb > upperBoundosnr) {
            spanOsnrDb =  upperBoundosnr;
        } else if (spanOsnrDb < lowerBoundosnr) {
            spanOsnrDb = lowerBoundosnr;
        }
        spanOsnrLu = Math.pow(10, (spanOsnrDb / 10.0));
        sum = PceConstraints.CONST_OSNR / spanOsnrLu;
        linkOsnrLu = sum;
        LOG.debug("In retrieveosnr: link osnr is {} dB", linkOsnrLu);
        return linkOsnrLu;
    }

    public LinkId getOppositeLink() {
        return oppositeLink;
    }

    public Object getSourceTP() {
        return sourceTP;
    }

    public Object getDestTP() {
        return destTP;
    }

    public OpenroadmLinkType getlinkType() {
        return linkType;
    }

    public LinkId getLinkId() {
        return linkId;
    }

    public NodeId getSourceId() {
        return sourceId;
    }

    public NodeId getDestId() {
        return destId;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    // Double for transformer of JUNG graph
    public Double getLatency() {
        return latency.doubleValue();
    }

    public String getsourceSupNodeId() {
        return sourceSupNodeId;
    }

    public String getdestSupNodeId() {
        return destSupNodeId;
    }

    public List<Long> getsrlgList() {
        return srlgList;
    }

    public double getosnr() {
        return osnr;
    }

    public String getsourceCLLI() {
        return sourceCLLI;
    }

    public String getdestCLLI() {
        return destCLLI;
    }

    public boolean isValid() {
        if ((this.linkId == null) || (this.linkType == null)
                || (this.oppositeLink == null)) {
            isValid = false;
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", linkId);
        }
        if ((this.sourceId == null) || (this.destId == null)
                || (this.sourceTP == null) || (this.destTP == null)) {
            isValid = false;
            LOG.error("PceLink: No Link source or destination is available. Link is ignored {}", linkId);
        }
        if ((this.sourceSupNodeId.equals("")) || (this.destSupNodeId.equals(""))) {
            isValid = false;
            LOG.error("PceLink: No Link source SuppNodeID or destination SuppNodeID is available. Link is ignored {}",
                linkId);
        }
        if ((this.sourceCLLI.equals("")) || (this.destCLLI.equals(""))) {
            isValid = false;
            LOG.error("PceLink: No Link source CLLI or destination CLLI is available. Link is ignored {}", linkId);
        }
        if ((this.omsAttributesSpan == null) && (this.linkType == OpenroadmLinkType.ROADMTOROADM)) {
            isValid = false;
            LOG.error("PceLink: Error reading Span for OMS link. Link is ignored {}", linkId);
        }
        if ((this.srlgList != null) && (this.srlgList.isEmpty())) {
            isValid = false;
            LOG.error("PceLink: Empty srlgList for OMS link. Link is ignored {}", linkId);
        }
        return isValid;
    }

    public String toString() {
        return "PceLink type=" + linkType + " ID=" + linkId.getValue() + " latecy=" + latency;
    }

}
