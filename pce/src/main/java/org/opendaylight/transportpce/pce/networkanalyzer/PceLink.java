/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev220630.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev211210.span.attributes.LinkConcatenation1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev211210.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.networks.network.link.oms.attributes.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.link.concatenation.LinkConcatenation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.link.concatenation.LinkConcatenationKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SE_NO_SERIALVERSIONID",
    justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class PceLink implements Serializable {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceLink.class);
    ///////////////////////// LINKS ////////////////////
    /*
     * extension of Link to include constraints and Graph weight
     */
    double weight = 0;
    private boolean isValid = true;

    // this member is for XPONDER INPUT/OUTPUT links.
    // it keeps name of client corresponding to NETWORK TP
    private String client = "";
    private final LinkId linkId;
    private final OpenroadmLinkType linkType;
    private final NodeId sourceId;
    private final NodeId destId;
    private final TpId sourceTP;
    private final TpId destTP;
    private final String sourceNetworkSupNodeId;
    private final String destNetworkSupNodeId;
    private final String sourceCLLI;
    private final String destCLLI;
    private final LinkId oppositeLink;
    private final AdminStates adminStates;
    private final State state;
    private final Long latency;
    private final Long availableBandwidth;
    private final Long usedBandwidth;
    private final List<Long> srlgList;
    private final double osnr;
    private final transient Span omsAttributesSpan;
    //meter per ms
    private static final double CELERITY = 2.99792458 * 1e5;
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

        this.sourceNetworkSupNodeId = source.getSupNetworkNodeId();
        this.destNetworkSupNodeId = dest.getSupNetworkNodeId();

        this.sourceCLLI = source.getSupClliNodeId();
        this.destCLLI = dest.getSupClliNodeId();

        this.linkType = MapUtils.calcType(link);

        this.oppositeLink = calcOpposite(link);

        this.adminStates = link.augmentation(Link1.class).getAdministrativeState();
        this.state = link.augmentation(Link1.class).getOperationalState();

        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            this.omsAttributesSpan = MapUtils.getOmsAttributesSpan(link);
            this.srlgList = MapUtils.getSRLG(link);
            this.latency = calcLatency(link);
            this.osnr = calcSpanOSNR();
            this.availableBandwidth = 0L;
            this.usedBandwidth = 0L;
        } else if (this.linkType == OpenroadmLinkType.OTNLINK) {
            this.availableBandwidth = MapUtils.getAvailableBandwidth(link);
            this.usedBandwidth = MapUtils.getUsedBandwidth(link);
            this.srlgList = MapUtils.getSRLGfromLink(link);
            this.osnr = 0.0;
            this.latency = 0L;
            this.omsAttributesSpan = null;
        } else {
            this.omsAttributesSpan = null;
            this.srlgList = null;
            this.latency = 0L;
            //infinite OSNR in DB
            this.osnr = 100L;
            this.availableBandwidth = 0L;
            this.usedBandwidth = 0L;
        }
        LOG.debug("PceLink: created PceLink  {}", linkId);
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

    //Compute the link latency : if the latency is not defined, the latency is computed from the omsAttributesSpan
    private Long calcLatency(Link link) {
        Link1 link1 = link.augmentation(Link1.class);
        if (link1.getLinkLatency() != null) {
            return link1.getLinkLatency().toJava();
        }
        if (this.omsAttributesSpan == null) {
            return 1L;
        }
        double tmp = 0;
        Map<LinkConcatenationKey, LinkConcatenation> linkConcatenationMap = this.omsAttributesSpan
                .nonnullLinkConcatenation();
        for (Map.Entry<LinkConcatenationKey, LinkConcatenation> entry : linkConcatenationMap.entrySet()) {
            // Length is expressed in meter and latency is expressed in ms according to OpenROADM MSA
            if (entry == null || entry.getValue() == null || entry.getValue().getSRLGLength() == null) {
                LOG.debug("In PceLink: cannot compute the latency for the link {}", link.getLinkId().getValue());
                return 1L;
            }
            tmp += entry.getValue().getSRLGLength().doubleValue() / CELERITY;
            LOG.debug("In PceLink: The latency of link {} == {}", link.getLinkId(), tmp);
        }
        return (long) Math.ceil(tmp);
    }

    //Compute the OSNR of a span
    private double calcSpanOSNR() {
        if (this.omsAttributesSpan == null) {
            return 0L;
        }
        Collection<LinkConcatenation> linkConcatenationList =
            this.omsAttributesSpan.nonnullLinkConcatenation().values();
        if (linkConcatenationList == null) {
            LOG.error("in PceLink : Null field in the OmsAttrubtesSpan");
            return 0L;
        }
        Iterator<LinkConcatenation> linkConcatenationiterator = linkConcatenationList.iterator();
        if (!linkConcatenationiterator.hasNext()) {
            return 0L;
        }
        if (this.omsAttributesSpan.getSpanlossCurrent() == null) {
            LOG.error("in PceLink : Spanloss is null");
            return 0L;
        }
        // power on the output of the previous ROADM (dBm)
        double pout = retrievePower(linkConcatenationiterator.next().augmentation(LinkConcatenation1.class)
            .getFiberType());
        // span loss (dB)
        double spanLoss = this.omsAttributesSpan.getSpanlossCurrent().getValue().doubleValue();
        // power on the input of the current ROADM (dBm)
        double pin = pout - spanLoss;
        double spanOsnrDb = NOISE_MASK_A * pin + NOISE_MASK_B;
        if (spanOsnrDb > UPPER_BOUND_OSNR) {
            spanOsnrDb = UPPER_BOUND_OSNR;
        } else if (spanOsnrDb < LOWER_BOUND_OSNR) {
            spanOsnrDb = LOWER_BOUND_OSNR;
        }
        return spanOsnrDb;
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

    public AdminStates getAdminStates() {
        return adminStates;
    }

    public State getState() {
        return state;
    }

    public TpId getSourceTP() {
        return sourceTP;
    }

    public TpId getDestTP() {
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

    public Long getUsedBandwidth() {
        return usedBandwidth;
    }

    public String getsourceNetworkSupNodeId() {
        return sourceNetworkSupNodeId;
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
        if ((this.linkId == null) || (this.linkType == null) || (this.oppositeLink == null)) {
            isValid = false;
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", linkId);
        }
        isValid = checkParams();
        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            if (this.omsAttributesSpan == null) {
                isValid = false;
                LOG.error("PceLink: Error reading Span for OMS link. Link is ignored {}", linkId);
            } else if (this.omsAttributesSpan.getSpanlossCurrent() == null) {
                isValid = false;
                LOG.error("PceLink: Error reading Spanloss for OMS link. Link is ignored {}", linkId);
            }
        }
        if ((this.srlgList != null) && (this.srlgList.isEmpty())) {
            isValid = false;
            LOG.error("PceLink: Empty srlgList for OMS link. Link is ignored {}", linkId);
        }
        return isValid;
    }

    public boolean isOtnValid(Link link, String serviceType) {

        if (this.linkType != OpenroadmLinkType.OTNLINK) {
            LOG.error("PceLink: Not an OTN link. Link is ignored {}", linkId);
            return false;
        }

        OtnLinkType otnLinkType = link
            .augmentation(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils
                    .rev220630.Link1.class)
            .getOtnLinkType();
        if (this.availableBandwidth == 0L) {
            LOG.error("PceLink: No bandwidth available for OTN Link, link {}  is ignored ", linkId);
            return false;
        }

        long neededBW;
        OtnLinkType neededType = null;
        switch (serviceType) {
            case "ODUC2":
                if (this.usedBandwidth != 0L) {
                    return false;
                }
                neededBW = 200000L;
                // Add intermediate rate otn-link-type
                neededType = OtnLinkType.OTUC2;
                break;
            case "ODUC3":
                if (this.usedBandwidth != 0L) {
                    return false;
                }
                neededBW = 300000L;
                // hange otn-link-type
                neededType = OtnLinkType.OTUC3;
                break;
            case "ODUC4":
                if (this.usedBandwidth != 0L) {
                    return false;
                }
                neededBW = 400000L;
                neededType = OtnLinkType.OTUC4;
                break;
            case "ODU4":
            case "100GEs":
                if (this.usedBandwidth != 0L) {
                    return false;
                }
                neededBW = 100000L;
                neededType = OtnLinkType.OTU4;
                break;
            case "ODU2":
            case "ODU2e":
                neededBW = 12500L;
                break;
            case "ODU0":
                neededBW = 1250L;
                break;
            case "ODU1":
                neededBW = 2500L;
                break;
            case "100GEm":
                neededBW = 100000L;
                // TODO: Here link type needs to be changed, based on the line-rate
                neededType = OtnLinkType.ODUC4;
                break;
            case "10GE":
                neededBW = 10000L;
                neededType = OtnLinkType.ODTU4;
                break;
            case "1GE":
                neededBW = 1000L;
                neededType = OtnLinkType.ODTU4;
                break;
            default:
                LOG.error("PceLink: isOtnValid Link {} unsupported serviceType {} ", linkId, serviceType);
                return false;
        }

        if ((this.availableBandwidth >= neededBW)
            && ((neededType == null) || (neededType.equals(otnLinkType)))) {
            LOG.debug("PceLink: Selected Link {} has available bandwidth and is eligible for {} creation ",
                linkId, serviceType);
        }

        return checkParams();
    }

    private boolean checkParams() {
        if ((this.linkId == null) || (this.linkType == null) || (this.oppositeLink == null)) {
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", linkId);
            return false;
        }
        if ((this.adminStates == null) || (this.state == null)) {
            LOG.error("PceLink: Link is not available. Link is ignored {}", linkId);
            return false;
        }
        if ((this.sourceId == null) || (this.destId == null) || (this.sourceTP == null) || (this.destTP == null)) {
            LOG.error("PceLink: No Link source or destination is available. Link is ignored {}", linkId);
            return false;
        }
        if ((this.sourceNetworkSupNodeId.equals("")) || (this.destNetworkSupNodeId.equals(""))) {
            LOG.error("PceLink: No Link source SuppNodeID or destination SuppNodeID is available. Link is ignored {}",
                linkId);
            return false;
        }
        if ((this.sourceCLLI.equals("")) || (this.destCLLI.equals(""))) {
            LOG.error("PceLink: No Link source CLLI or destination CLLI is available. Link is ignored {}", linkId);
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "PceLink type=" + linkType + " ID=" + linkId.getValue() + " latency=" + latency;
    }
}
