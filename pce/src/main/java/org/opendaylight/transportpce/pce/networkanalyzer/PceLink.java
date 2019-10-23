/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.List;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev181130.span.attributes.LinkConcatenation.FiberType;
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
    private boolean isOtnValid = true;

    // this member is for XPONDER INPUT/OUTPUT links.
    // it keeps name of client corresponding to NETWORK TP
    private String client = "";
    private final LinkId linkId;
    private final OpenroadmLinkType linkType;
    private final NodeId sourceId;
    private final NodeId destId;
    private final Object sourceTP;
    private final Object destTP;
    private final String sourceSupNodeId;
    private final String destSupNodeId;
    private final String sourceNetworkSupNodeId;
    private final String destNetworkSupNodeId;
    private final String sourceCLLI;
    private final String destCLLI;
    private final LinkId oppositeLink;
    private final Long latency;
    private final Long availableBandwidth;
    private final List<Long> srlgList;
    private final double osnr;
    private final Span omsAttributesSpan;
    private static final double CELERITY = 2.99792458 * 1e5; //meter per ms
    private static final double NOISE_MASK_A = 0.571429;
    private static final double NOISE_MASK_B = 39.285714;
    private static final double UPPER_BOUND_OSNR = 33;
    private static final double LOWER_BOUND_OSNR = 0.1;

    public PceLink(Link link, PceNode source, PceNode dest) {
        LOG.debug("PceLink: : PceLink start ");

        this.linkId = link.getLinkId();

        this.sourceId = link.getSource().getSourceNode();
        this.destId = link.getDestination().getDestNode();

        this.sourceTP = link.getSource().getSourceTp();
        this.destTP = link.getDestination().getDestTp();

        this.sourceSupNodeId = source.getSupNodeIdPceNode();
        this.destSupNodeId = dest.getSupNodeIdPceNode();
        this.sourceNetworkSupNodeId = source.getSupNetworkNodeIdPceNode();
        this.destNetworkSupNodeId = dest.getSupNetworkNodeIdPceNode();

        this.sourceCLLI = source.getClliSupNodeId();
        this.destCLLI = dest.getClliSupNodeId();

        this.linkType = MapUtils.calcType(link);

        this.oppositeLink = calcOpposite(link);

        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            this.omsAttributesSpan = MapUtils.getOmsAttributesSpan(link);
            this.srlgList = MapUtils.getSRLG(link);
            this.latency = calcLatency(link);
            this.osnr = calcSpanOSNR();
            this.availableBandwidth = 0L;
        } else if (this.linkType == OpenroadmLinkType.OTNLINK) {
            this.availableBandwidth = MapUtils.getAvailableBandwidth(link);
            this.srlgList = MapUtils.getSRLGfromLink(link);
            this.osnr = 0.0;
            this.latency = 0L;
            this.omsAttributesSpan = null;
        } else {
            this.omsAttributesSpan = null;
            this.srlgList = null;
            this.latency = 0L;
            this.osnr = 100L; //infinite OSNR in DB
            this.availableBandwidth = 0L;
        }
        LOG.debug("PceLink: created PceLink  {}", toString());
    }

    //Retrieve the opposite link
    private LinkId calcOpposite(Link link) {
        LinkId tmpoppositeLink = MapUtils.extractOppositeLink(link);
        if (tmpoppositeLink == null) {
            LOG.error("PceLink: Error calcOpposite. Link is ignored {}", link.getLinkId().getValue());
            isValid = false;
        }
        return tmpoppositeLink;
    }

    //Compute the link latency : if the latency is not defined, the latency it is computed from the omsAttributesSpan
    private Long calcLatency(Link link) {
        Link1 link1 = null;
        link1 = link.augmentation(Link1.class);
        Long tmplatency = link1.getLinkLatency();
        if (tmplatency != null) {
            return tmplatency;
        }

        try {
            double tmp = 0;
            for (int i = 0; i < this.omsAttributesSpan.getLinkConcatenation().size(); i++) {
                //Length is expressed in meter and latency is expressed in ms according to OpenROADM MSA
                tmp += this.omsAttributesSpan.getLinkConcatenation().get(i).getSRLGLength() / CELERITY;
                LOG.info("In PceLink: The latency of link {} == {}",link.getLinkId(),tmplatency);
            }
            tmplatency = (long) Math.ceil(tmp);
        } catch (NullPointerException e) {
            LOG.debug("In PceLink: cannot compute the latency for the link {}",link.getLinkId().getValue());
            tmplatency = 1L;
        }
        return tmplatency;
    }

    //Compute the OSNR of a span
    public double calcSpanOSNR() {
        try {
            double pout; //power on the output of the previous ROADM (dBm)
            pout = retrievePower(this.omsAttributesSpan.getLinkConcatenation().get(0).getFiberType());
            double spanLoss = this.omsAttributesSpan.getSpanlossCurrent().getValue().doubleValue(); // span loss (dB)
            double pin = pout - spanLoss; //power on the input of the current ROADM (dBm)
            double spanOsnrDb;
            spanOsnrDb = NOISE_MASK_A * pin + NOISE_MASK_B;
            if (spanOsnrDb > UPPER_BOUND_OSNR) {
                spanOsnrDb =  UPPER_BOUND_OSNR;
            } else if (spanOsnrDb < LOWER_BOUND_OSNR) {
                spanOsnrDb = LOWER_BOUND_OSNR;
            }
            return spanOsnrDb;
        } catch (NullPointerException e) {
            LOG.error("in PceLink : Null field in the OmsAttrubtesSpan");
            return 0L;
        }
    }

    private double retrievePower(FiberType fiberType) {
        double power;
        switch (fiberType) {
            case Smf:
                power = 2;
                break;
            case Eleaf:
                power = 1;
                break;
            case Truewavec:
                power = -1;
                break;
            case Oleaf:
            case Dsf:
            case Truewave:
            case NzDsf:
            case Ull:
            default:
                power = 0;
                break;
        }
        return power;
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

    public Long getAvailableBandwidth() {
        return availableBandwidth;
    }


    public String getsourceSupNodeId() {
        return sourceSupNodeId;
    }

    public String getsourceNetworkSupNodeId() {
        return sourceNetworkSupNodeId;
    }

    public String getdestSupNodeId() {
        return destSupNodeId;
    }

    public String getdestNetworkSupNodeId() {
        return destNetworkSupNodeId;
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

    public boolean isOtnValid(Link link, String oduType) {
        if (this.linkType == OpenroadmLinkType.OTNLINK) {
            isOtnValid = false;
            Long availableBW = MapUtils.getAvailableBandwidth(link);
            if ((availableBW == 0L) || (availableBW == null)) {
                LOG.error("PceLink: No bandwidth available or not valid OTN Link, Link {}  is ignored ", linkId);
            } else if (("ODU4".equals(oduType)) && (availableBW == 100000L)) {
                isOtnValid = true;
                LOG.debug("PceLink: Selected OTU4 Link {} is eligible for ODU creation OTN Link", linkId);
            } else if (("ODU2".equals(oduType)) || ("ODU2e".equals(oduType)) && (availableBW >= 12500L)) {
                isOtnValid = true;
                LOG.debug("PceLink: Selected ODU4 Link {} has available bandwidth and is eligible for {} creation ",
                    linkId, oduType);
            } else if (("ODU0".equals(oduType)) && (availableBW >= 1250L)) {
                isOtnValid = true;
                LOG.debug("PceLink: Selected ODU4 Link {} has available bandwidth and is eligible for {} creation ",
                    linkId, oduType);
            } else if (("ODU1".equals(oduType)) && (availableBW >= 2500L)) {
                isOtnValid = true;
                LOG.debug("PceLink: Selected ODU4 Link {} has available bandwidth and is eligible for {} creation ",
                    linkId, oduType);
            } else {
                isOtnValid = false;
                LOG.error(
                    "PceLink: Selected OTN Link {} is not eligible for ODU creation: not enough available bandwidth",
                    linkId);
            }

        } else {
            isOtnValid = false;
            LOG.error("PceLink: Not an OTN link. Link is ignored {}", linkId);
        }

        if ((this.linkId == null) || (this.linkType == null)
                || (this.oppositeLink == null)) {
            isOtnValid = false;
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", linkId);
        }
        if ((this.sourceId == null) || (this.destId == null)
                || (this.sourceTP == null) || (this.destTP == null)) {
            isOtnValid = false;
            LOG.error("PceLink: No Link source or destination is available. Link is ignored {}", linkId);
        }
        if ((this.sourceNetworkSupNodeId.equals("")) || (this.destNetworkSupNodeId.equals(""))) {
            isOtnValid = false;
            LOG.error("PceLink: No Link source SuppNodeID or destination SuppNodeID is available. Link is ignored {}",
                linkId);
        }
        if ((this.sourceCLLI.equals("")) || (this.destCLLI.equals(""))) {
            isOtnValid = false;
            LOG.error("PceLink: No Link source CLLI or destination CLLI is available. Link is ignored {}", linkId);
        }

        return isOtnValid;
    }

    public String toString() {
        return "PceLink type=" + linkType + " ID=" + linkId.getValue() + " latency=" + latency;
    }

}
