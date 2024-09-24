/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.link.oms.attributes.Span;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@SuppressFBWarnings(
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
    private String clientA = "";
    private String clientZ = "";
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
    private final Double length;
    private final Double cd;
    private final Double pmd2;
    private final Double spanLoss;
    private final Double powerCorrection;
    private final transient Span omsAttributesSpan;

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
        switch (this.linkType) {
            case ROADMTOROADM:
                this.omsAttributesSpan = MapUtils.getOmsAttributesSpan(link);
                this.length = NetworkUtils.calcLength(link);
                this.srlgList = MapUtils.getSRLG(link);
                this.latency = NetworkUtils.calcLatency(link);
                this.availableBandwidth = 0L;
                this.usedBandwidth = 0L;
                Map<String, Double> spanLossMap = NetworkUtils.calcSpanLoss(link);
                this.spanLoss = spanLossMap.get("SpanLoss");
                this.powerCorrection = spanLossMap.get("PoutCorrection");
                Map<String, Double> cdAndPmdMap = NetworkUtils.calcCDandPMD(link);
                this.cd = cdAndPmdMap.get("CD");
                this.pmd2 = cdAndPmdMap.get("PMD2");
                break;
            case OTNLINK:
                this.availableBandwidth = MapUtils.getAvailableBandwidth(link);
                this.usedBandwidth = MapUtils.getUsedBandwidth(link);
                this.srlgList = MapUtils.getSRLGfromLink(link);
                this.latency = 0L;
                this.length = 0.0;
                this.omsAttributesSpan = null;
                this.spanLoss = 0.0;
                this.powerCorrection = 0.0;
                this.cd = 0.0;
                this.pmd2 = 0.0;
                break;
            default:
                this.omsAttributesSpan = null;
                this.srlgList = null;
                this.latency = 0L;
                this.length = 0.0;
                this.availableBandwidth = 0L;
                this.usedBandwidth = 0L;
                this.spanLoss = 0.0;
                this.powerCorrection = 0.0;
                this.cd = 0.0;
                this.pmd2 = 0.0;
                break;
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

    public String getClientA() {
        return clientA;
    }

    public Double getLength() {
        return length;
    }

    public void setClientA(String client) {
        this.clientA = client;
    }

    public String getClientZ() {
        return clientZ;
    }

    public void setClientZ(String client) {
        this.clientZ = client;
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

    public String getsourceCLLI() {
        return sourceCLLI;
    }

    public String getdestCLLI() {
        return destCLLI;
    }

    public Double getspanLoss() {
        return spanLoss;
    }

    public Double getcd() {
        return cd;
    }

    public Double getpmd2() {
        return pmd2;
    }

    public Double getpowerCorrection() {
        return powerCorrection;
    }

    public boolean isValid() {
        if (this.linkId == null || this.linkType == null || this.oppositeLink == null) {
            isValid = false;
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", linkId);
        }
        isValid = checkParams();
        if (this.linkType == OpenroadmLinkType.ROADMTOROADM && (this.length == null || this.length == 0.0)) {
            if (this.omsAttributesSpan == null) {
                isValid = false;
                LOG.error("PceLink: Error reading Span for OMS link, and no available generic link information."
                    + " Link is ignored {}", linkId);
            } else if (this.omsAttributesSpan.getSpanlossCurrent() == null) {
                isValid = false;
                LOG.error("PceLink: Error reading Span for OMS link, and no available generic link information."
                    + " Link is ignored {}", linkId);
            }
        }
        if (this.srlgList != null && this.srlgList.isEmpty()) {
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
                // change otn-link-type
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
        if (this.availableBandwidth >= neededBW
                && (neededType == null
                    || neededType.equals(
                        link.augmentation(
                                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923
                                    .Link1.class)
                            .getOtnLinkType()))) {
            LOG.debug("PceLink: Selected Link {} has available bandwidth and is eligible for {} creation ",
                linkId, serviceType);
        }
        return checkParams();
    }

    private boolean checkParams() {
        if (this.linkId == null || this.linkType == null || this.oppositeLink == null) {
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", linkId);
            return false;
        }
        if (this.adminStates == null || this.state == null) {
            LOG.error("PceLink: Link is not available. Link is ignored {}", linkId);
            return false;
        }
        if (this.sourceId == null || this.destId == null || this.sourceTP == null || this.destTP == null) {
            LOG.error("PceLink: No Link source or destination is available. Link is ignored {}", linkId);
            return false;
        }
        if (this.sourceNetworkSupNodeId.equals("") || this.destNetworkSupNodeId.equals("")) {
            LOG.error("PceLink: No Link source SuppNodeID or destination SuppNodeID is available. Link is ignored {}",
                linkId);
            return false;
        }
        if (this.sourceCLLI.equals("") || this.destCLLI.equals("")) {
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
