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
import java.util.Collection;
import java.util.HashMap;
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
//    private final double osnr;
    private final Double length;
    private final Double cd;
    private final Double pmd2;
    private final Double spanLoss;
    private final Double powerCorrection;
    private final transient Span omsAttributesSpan;
    //meter per ms
    private static final double GLASSCELERITY = 2.99792458 * 1e5 / 1.5;
    private static final double PMD_CONSTANT = 0.04;

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
                this.length = calcLength(link);
                this.srlgList = MapUtils.getSRLG(link);
                this.latency = calcLatency(link);
                this.availableBandwidth = 0L;
                this.usedBandwidth = 0L;
                Map<String, Double> spanLossMap = calcSpanLoss(link);
                this.spanLoss = spanLossMap.get("SpanLoss");
                this.powerCorrection = spanLossMap.get("PoutCorrection");
                Map<String, Double> cdAndPmdMap = calcCDandPMD(link);
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

    //Compute the link latency : if the latency is not defined, the latency is computed from the length
    private Long calcLatency(Link link) {
        var augLinkLatency = link.augmentation(Link1.class).getLinkLatency();
        if (augLinkLatency != null) {
            return augLinkLatency.toJava();
        }
        Double linkLength = calcLength(link);
        if (linkLength == null) {
            LOG.debug("In PceLink: cannot compute the latency for the link {}", link.getLinkId().getValue());
            return 1L;
        }
        LOG.debug("In PceLink: The latency of link {} is extrapolated from link length and == {}",
            link.getLinkId(), linkLength / GLASSCELERITY);
        return (long) Math.ceil(linkLength / GLASSCELERITY);
    }

    private Double calcLength(Link link) {
        var augLinkLength = link.augmentation(Link1.class).getLinkLength();
        if (augLinkLength != null) {
            return augLinkLength.doubleValue();
        }
        if (this.omsAttributesSpan == null) {
            LOG.debug("In PceLink: cannot compute the length for the link {}", link.getLinkId().getValue());
            return null;
        }
        double linkLength = 0;
        Map<LinkConcatenationKey, LinkConcatenation> linkConcatenationMap =
            this.omsAttributesSpan.nonnullLinkConcatenation();
        for (Map.Entry<LinkConcatenationKey, LinkConcatenation> entry : linkConcatenationMap.entrySet()) {
            // Length is expressed in meter according to OpenROADM MSA
            if (entry == null || entry.getValue() == null || entry.getValue().getSRLGLength() == null) {
                LOG.debug("In PceLink: cannot compute the length for the link {}", link.getLinkId().getValue());
                return null;
            }
            linkLength += entry.getValue().getSRLGLength().doubleValue();
            LOG.debug("In PceLink: The length of the link {} == {}", link.getLinkId(), linkLength / 1000.0);
        }
        return linkLength / 1000.0;
    }

    //Calculate CD and PMD of the link from link length
    private Map<String, Double> calcCDandPMDfromLength() {
        return this.length == null
            ? new HashMap<>()
            : new HashMap<>(
                Map.of(
                    "CD", 16.5 * this.length,
                    "PMD2", Math.pow(this.length * PMD_CONSTANT, 2)));
    }

    //Calculate CD and PMD of the link
    private Map<String, Double> calcCDandPMD(Link link) {
        double linkCd = 0.0;
        double linkPmd2 = 0.0;
        if (this.omsAttributesSpan == null) {
            LOG.debug("In PceLink {} no OMS present, assume G.652 fiber, calculation based on fiber length of {} km",
                link.getLinkId(), this.length);
            return calcCDandPMDfromLength();
        }
        Map<LinkConcatenationKey, LinkConcatenation> linkConcatenationMap =
            this.omsAttributesSpan.nonnullLinkConcatenation();
        for (Map.Entry<LinkConcatenationKey, LinkConcatenation> entry : linkConcatenationMap.entrySet()) {
            // If the link-concatenation list is not populated or partially populated CD &
            // PMD shall be derived from link-length (expressed in km in OR topology)
            if (entry == null || entry.getValue() == null || entry.getValue().getSRLGLength() == null
                    || entry.getValue().augmentation(LinkConcatenation1.class).getFiberType() == null) {
                if (this.length > 0.0) {
                    LOG.debug("In PceLink: no OMS present; cd and PMD for the link {} extrapolated from link length {}"
                        + "assuming SMF fiber type", link.getLinkId().getValue(), this.length);
                    return calcCDandPMDfromLength();
                }
                // If Link-length upper attributes not present or incorrectly populated, no way
                // to calculate CD & PMD
                LOG.error("In PceLink: no Link length declared and no OMS present for the link {}."
                    + " No Way to compute CD and PMD", link.getLinkId().getValue());
                return Map.of();
            }
            // SRLG length is expressed in OR topology in meter
            var entryAug = entry.getValue().augmentation(LinkConcatenation1.class);
            linkCd += entry.getValue().getSRLGLength().doubleValue() / 1000.0
                * retrieveCdFromFiberType(entryAug.getFiberType());
            if (entryAug.getPmd() == null
                    || entryAug.getPmd().getValue().doubleValue() == 0.0
                    || entryAug.getPmd().getValue().toString().isEmpty()) {
                linkPmd2 += Math.pow(
                    entry.getValue().getSRLGLength().doubleValue() / 1000.0
                        * retrievePmdFromFiberType(entryAug.getFiberType()),
                    2);
            } else {
                linkPmd2 += Math.pow(entryAug.getPmd().getValue().doubleValue(), 2);
            }
        }
        LOG.debug("In PceLink: The CD and PMD2 of link {} are respectively {} ps and {} ps",
            link.getLinkId(), linkCd, linkPmd2);
        return Map.of("CD", linkCd, "PMD2", linkPmd2);
    }

    // compute default spanLoss and power correction from fiber length
    // when no OMS attribute defined
    private Map<String, Double> calcDefaultSpanLoss(Link link) {
        var augLinkLength = link.augmentation(Link1.class).getLinkLength();
        if (augLinkLength == null || augLinkLength.doubleValue() == 0) {
            LOG.error("In PceLink, no link length present or length declared = 0,"
                + " unable to calculate default span Loss ");
            return new HashMap<>();
        }
        long linkLength = augLinkLength.longValue();
        LOG.warn("In PceLink {}, assume G.652 fiber, calculation "
                + "based on fiber length of {} km and typical loss of 0.25dB per Km ",
            link.getLinkId(), linkLength);
        return new HashMap<>(
            Map.of(
                "SpanLoss", linkLength * 0.25,
                "PoutCorrection", retrievePower(FiberType.Smf)
            ));
    }

    // Compute the attenuation of a span from OMS attribute
    private Map<String, Double> calcSpanLoss(Link link) {
        if (this.omsAttributesSpan == null) {
            return calcDefaultSpanLoss(link);
        }
        Collection<LinkConcatenation> linkConcatenationList =
            this.omsAttributesSpan.nonnullLinkConcatenation().values();
        if (linkConcatenationList == null) {
            LOG.error("in PceLink : Null field in the OmsAttrubtesSpan");
            return calcDefaultSpanLoss(link);
        }
        Iterator<LinkConcatenation> linkConcatenationiterator = linkConcatenationList.iterator();
        if (!linkConcatenationiterator.hasNext()) {
            return calcDefaultSpanLoss(link);
        }
        // Reference of power to be launched at input of ROADM (dBm)
        return new HashMap<>(Map.of(
            "PoutCorrection", retrievePower(
                linkConcatenationiterator.next().augmentation(LinkConcatenation1.class).getFiberType()) - 2.0,
            "SpanLoss", this.omsAttributesSpan.getSpanlossCurrent().getValue().doubleValue()));
    }

    private double retrievePower(FiberType fiberType) {
        switch (fiberType) {
            case Smf:
                return 2;
            case Eleaf:
                return 1;
            case Truewavec:
                return -1;
            case Oleaf:
            case Dsf:
            case Truewave:
            case NzDsf:
            case Ull:
            default:
                return 0;
        }
    }

    private double retrievePmdFromFiberType(FiberType fiberType) {
        return fiberType.toString().equalsIgnoreCase("Dsf")
            ? 0.2
            : PMD_CONSTANT;
    }

    private double retrieveCdFromFiberType(FiberType fiberType) {
        switch (fiberType) {
            case Dsf:
                return 0.0;
            case Truewavec:
                return 3.0;
            case Eleaf:
            case Oleaf:
            case NzDsf:
                return 4.3;
            case Truewave:
                return 4.4;
            case Smf:
            case Ull:
            default:
                return 16.5;
        }
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
                                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev220630
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
