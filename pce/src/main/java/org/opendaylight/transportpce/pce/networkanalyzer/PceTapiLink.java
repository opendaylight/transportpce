/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.pce.networkanalyzer.TapiOpticalNode.DirectionType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev220630.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev211210.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPoint;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.OtsConnectionEndPointSpec;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.TapiPhotonicMediaData;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePoint;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({"SE_BAD_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED",
    "SE_NO_SERIALVERSIONID"})

public class PceTapiLink implements Serializable {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceTapiLink.class);
    ///////////////////////// LINKS ////////////////////
    /*
     * extension of Link to include constraints and Graph weight
     */
    double weight = 0;
    private boolean isValid = true;

    // this member is for XPONDER INPUT/OUTPUT links.
    // it keeps name of client corresponding to NETWORK TP
    private Map<NameKey, Name> linkName;
    private final Uuid linkId;
    private OpenroadmLinkType linkType;
    private TopologyKey topoId;
    private Uuid sourceNodeId;
    private Uuid destNodeId;
    private Uuid sourceTpId;
    private Uuid destTpId;
//    private transient PceNode sourceNode;
//    private transient PceNode destNode;
    private String sourceNetworkSupNodeId;
    private String destNetworkSupNodeId;
    private String client = "";
    private ForwardingDirection direction;
    private final Uuid oppositeLink;
    private final AdministrativeState adminStates;
    private final OperationalState opState;
    private final Long latency;
    private final Double availableBandwidth;
    private final Double usedBandwidth;
    private final Set<String> srlgList;
    // source index will be set to reflect whether the source is NodeX (0) or NodeY
    // (1)
    private int sourceIndex;
    private final Double length;
    private final Double cd;
    private final Double pmd2;
    private final Double spanLoss;
    private final Double powerCorrection;
    private final transient List<OtsConnectionEndPointSpec> otsSpec;
    // meter per ms
    private static final double GLASSCELERITY = 2.99792458 * 1e5 / 1.5;
    private static final double PMD_CONSTANT = 0.04;

    public PceTapiLink(TopologyKey topoId, Link link, PceNode nodeX, PceNode nodeY) {
        LOG.debug("PceLink: : PceLink start ");
        this.linkId = link.getUuid();
        this.linkName = link.getName();
        this.topoId = topoId;
        this.direction = link.getDirection();

        retrieveSrcDestNodeIds(topoId, link, nodeX, nodeY);
        qualifyLinkType(link, nodeX, nodeY);

        this.oppositeLink = calcOpposite(link);

        this.adminStates = link.getAdministrativeState();
        this.opState = link.getOperationalState();

        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            this.otsSpec = retrieveEndPointSpec(nodeX, nodeY);
            this.length = calcLength(link);
            this.srlgList = TapiMapUtils.getSRLG(link);
            this.latency = calcLatency(link);
            this.availableBandwidth = 0.0;
            this.usedBandwidth = 0.0;
            Map<String, Double> spanLossMap = calcSpanLoss(link);
            this.spanLoss = spanLossMap.get("SpanLoss");
            this.powerCorrection = spanLossMap.get("PoutCorrection");
            Map<String, Double> cdAndPmdMap = calcCDandPMD(link);
            this.cd = cdAndPmdMap.get("CD");
            this.pmd2 = cdAndPmdMap.get("PMD2");
        } else if (this.linkType == OpenroadmLinkType.OTNLINK) {
            this.availableBandwidth = TapiMapUtils.getAvailableBandwidth(link);
            this.usedBandwidth = TapiMapUtils.getUsedBandwidth(link);
            this.srlgList = TapiMapUtils.getSRLG(link);
            this.latency = 0L;
            this.length = 0.0;
            this.otsSpec = null;
            this.spanLoss = 0.0;
            this.powerCorrection = 0.0;
            this.cd = 0.0;
            this.pmd2 = 0.0;
        } else {
            this.otsSpec = null;
            this.srlgList = null;
            this.latency = 0L;
            this.length = 0.0;
            this.availableBandwidth = 0.0;
            this.usedBandwidth = 0.0;
            this.spanLoss = 0.0;
            this.powerCorrection = 0.0;
            this.cd = 0.0;
            this.pmd2 = 0.0;
        }
        LOG.debug("PceLink: created PceLink  {}", linkId);
    }

    private void qualifyLinkType(Link link, PceNode nodeX, PceNode nodeY) {
        OpenroadmTpType sourceTpType = OpenroadmTpType.EXTPLUGGABLETP;
        OpenroadmTpType destTpType = OpenroadmTpType.EXTPLUGGABLETP;
        if (sourceIndex == 0) {
            sourceTpType = nodeX.getListOfNep().stream()
                .filter(bpn -> sourceNodeId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow().getTpType();
            destTpType = nodeY.getListOfNep().stream()
                .filter(bpn -> sourceNodeId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow().getTpType();
        } else if (sourceIndex == 1) {
            sourceTpType = nodeY.getListOfNep().stream()
                .filter(bpn -> sourceNodeId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow().getTpType();
            destTpType = nodeX.getListOfNep().stream()
                .filter(bpn -> sourceNodeId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow().getTpType();
        } else {
            LOG.error("TapiPceLink: Error proceeding Link {} for which source and dest NEP can not be identified ",
                link.getUuid().getValue());
        }
        switch (sourceTpType) {
            case SRGTXRXCP:
            case SRGTXCP:
                this.linkType = OpenroadmLinkType.ADDLINK;
                break;
            case DEGREETXRXCTP:
            case DEGREETXCTP:
                if (destTpType.equals(OpenroadmTpType.DEGREERXCTP)
                        || destTpType.equals(OpenroadmTpType.DEGREETXRXCTP)) {
                    this.linkType = OpenroadmLinkType.EXPRESSLINK;
                } else {
                    this.linkType = OpenroadmLinkType.DROPLINK;
                }
                break;
            case SRGTXRXPP:
            case SRGTXPP:
                this.linkType = OpenroadmLinkType.XPONDERINPUT;
                break;
            case XPONDERNETWORK:
            case EXTPLUGGABLETP:
                this.linkType = OpenroadmLinkType.XPONDEROUTPUT;
                break;
            case DEGREETXRXTTP:
            case DEGREETXTTP:
                this.linkType = OpenroadmLinkType.ROADMTOROADM;
                break;
            case XPONDERPORT:
            default:
                this.linkType = OpenroadmLinkType.OTNLINK;
                LOG.error("TapiPceLink: Error qualifying Link {} type. Set link type to unmanaged OTNLINK ",
                    link.getUuid().getValue());
                break;
        }
    }

    private void setSrcDestIds(Link link, PceNode nodeX, PceNode nodeY) {
        switch (this.sourceIndex) {
            case 0:
                this.sourceNetworkSupNodeId = nodeX.getSupNetworkNodeId();
                this.destNetworkSupNodeId = nodeY.getSupNetworkNodeId();
                this.sourceNodeId = nodeX.getNodeUuid();
                this.destNodeId = nodeY.getNodeUuid();
                this.sourceTpId = link.getNodeEdgePoint().keySet().stream()
                    .filter(key -> key.getNodeUuid().equals(nodeX.getNodeUuid()))
                    .findFirst().orElseThrow().getNodeEdgePointUuid();
                this.destTpId = link.getNodeEdgePoint().keySet().stream()
                    .filter(key -> key.getNodeUuid().equals(nodeY.getNodeUuid()))
                    .findFirst().orElseThrow().getNodeEdgePointUuid();
                break;
            case 1:
                this.sourceNetworkSupNodeId = nodeY.getSupNetworkNodeId();
                this.destNetworkSupNodeId = nodeX.getSupNetworkNodeId();
                this.sourceNodeId = nodeY.getNodeUuid();
                this.destNodeId = nodeX.getNodeUuid();
                this.sourceTpId = link.getNodeEdgePoint().keySet().stream()
                    .filter(key -> key.getNodeUuid().equals(nodeY.getNodeUuid()))
                    .findFirst().orElseThrow().getNodeEdgePointUuid();
                this.destTpId = link.getNodeEdgePoint().keySet().stream()
                    .filter(key -> key.getNodeUuid().equals(nodeX.getNodeUuid()))
                    .findFirst().orElseThrow().getNodeEdgePointUuid();
                break;
            default:
                break;
        }
    }

    private void retrieveSrcDestNodeIds(TopologyKey topoIid, Link link, PceNode nodeX, PceNode nodeY) {
        int sourceindex;
        if (ForwardingDirection.BIDIRECTIONAL.equals(link.getDirection())) {
            // In case of Bidirectional link we don't care of who is the source or the
            // destination
            sourceindex = 0;
        } else {
            // Unidirectional or undefined case
            Uuid nep0Uuid = link.getNodeEdgePoint().entrySet().iterator().next().getKey().getNodeEdgePointUuid();
            DirectionType nepDirectionX = nodeX.getListOfNep().stream()
                .filter(bpN -> bpN.getNepCepUuid().equals(nep0Uuid))
                .findFirst().orElseThrow().getDirection();
            DirectionType nepDirectionY = nodeY.getListOfNep().stream()
                .filter(bpN -> bpN.getNepCepUuid().equals(nep0Uuid))
                .findFirst().orElseThrow().getDirection();
            if (nepDirectionY == null && DirectionType.SOURCE.equals(nepDirectionX)) {
                sourceindex = 0;
            } else if (nepDirectionY == null && DirectionType.SINK.equals(nepDirectionX)) {
                sourceindex = 1;
            } else if (nepDirectionX == null && DirectionType.SOURCE.equals(nepDirectionY)) {
                sourceindex = 1;
            } else if (nepDirectionX == null && DirectionType.SINK.equals(nepDirectionY)) {
                sourceindex = 0;
            } else {
                LOG.error("TapiPceLink: Error proceeding Link {} for which source and dest NEP can not be identified ",
                    link.getUuid().getValue());
                return;
            }
        }
        this.sourceIndex = sourceindex;
        setSrcDestIds(link, nodeX, nodeY);
    }
//              TODO : Change Pom of PCE to have tapi utils : Loop warning : pce is in tapi Pom
//                identify how to remove this dependency
//                //TapiContext requires a networkTransaction Service, see if we can handle it in a different way
//                // from Map generated in TapiOpticalNode adding parameters while handling outgoing links
//                OwnedNodeEdgePoint nep1 = TapiContext.getTapiNEP(topoId.getUuid(), nep.getKey().getNodeUuid(),
//                        nep.getKey().getNodeEdgePointUuid());
//                  if (nep1.getSinkProfile() == null || nep1.getSinkProfile().isEmpty()) {
//                      if (nep1.getSourceProfile() == null || nep1.getSourceProfile().isEmpty()) {
//                          LOG.error("TapiPceLink: Error proceeding Link {} for which NEP as no sync/"
//                              + "Source defined. Assuming 1st NEP is the source ", link.getUuid().getValue());
//                      } else {
//                          this.sourceNodeId = entry.getKey().getNodeUuid();
//                          this.sourceTpId = entry.getKey().getNodeEdgePointUuid();
//                      }
//                  } else {
//                      this.destNodeId = entry.getKey().getNodeUuid();
//                      this.destTpId =  entry.getKey().getNodeEdgePointUuid();
//                  }


    private List<OtsConnectionEndPointSpec> retrieveEndPointSpec(PceNode nodeX, PceNode nodeY) {
        List<OtsConnectionEndPointSpec> otsCepSpecList = new ArrayList<>();
        OtsConnectionEndPointSpec otsCepSpec1 = nodeX.getListOfNep().stream()
            .filter(bpn -> this.sourceTpId.equals(bpn.getNepCepUuid()))
            .findFirst().orElseThrow().getCepOtsSpec();
        OtsConnectionEndPointSpec otsCepSpec2 = nodeY.getListOfNep().stream()
            .filter(bpn -> this.sourceTpId.equals(bpn.getNepCepUuid()))
            .findFirst().orElseThrow().getCepOtsSpec();
        if ((otsCepSpec1 == null) && (otsCepSpec2 == null)) {
            LOG.error("Error retrieving OTS Cep Spec for link {} named {} ", linkId, linkName.toString());
            return null;
        } else if (otsCepSpec1 != null) {
            otsCepSpecList.add(otsCepSpec1);
        }
        if (otsCepSpec2 != null && direction == ForwardingDirection.BIDIRECTIONAL) {
            otsCepSpecList.add(otsCepSpec2);
        }
        return otsCepSpecList;
    }

    //Retrieve the opposite link
    private Uuid calcOpposite(Link link) {
        Uuid tmpoppositeLink = TapiMapUtils.extractOppositeLink(link);
        if (tmpoppositeLink == null) {
            LOG.error("PceLink: Error calcOpposite. Link for link {} named {}", link.getUuid().getValue(),
                    link.getName().toString());
            //isValid = false;
        }
        return tmpoppositeLink;
    }

    //Compute the link latency : if the latency is not defined, the latency is computed from the length
    private Long calcLatency(Link link) {
        Double linkLength = calcLength(link);
        if (linkLength == null) {
            LOG.debug("In PceLink: cannot compute the latency for the link {}", link.getUuid().getValue());
            return 1L;
        }
        LOG.debug("In PceLink: The latency of link {} is extrapolated from link length and == {}",
            link.getUuid(), linkLength / GLASSCELERITY);
        return (long) Math.ceil(linkLength / GLASSCELERITY);
    }

    private Double calcLength(Link link) {
        // TODO when 2.4 models available
        LOG.error("Uncomplete method");
        double linkLength = 0;
        return (linkLength / 1000.0);
    }

    //Calculate CD and PMD of the link from link length
    private Map<String, Double> calcCDandPMDfromLength() {
        Map<String, Double> cdAndPmd = new HashMap<>();
        if (this.length != null) {
            cdAndPmd.put("CD", 16.5 * this.length);
            cdAndPmd.put("PMD2", Math.pow(this.length * PMD_CONSTANT, 2));
        }
        return cdAndPmd;
    }

    //Calculate CD and PMD of the link
    private Map<String, Double> calcCDandPMD(Link link) {
        double linkCd = 0.0;
     // TODO when 2.4 models available
        LOG.error("Uncomplete method");
        if (this.otsSpec == null) {
            LOG.debug("In PceLink {} no OTS present, assume G.652 fiber, calculation based on fiber length of {} km",
                link.getUuid(), this.length);
            return calcCDandPMDfromLength();
        }
        linkCd = 0 * retrieveCdFromFiberType(FiberType.Smf);
//        Map<LinkConcatenationKey, LinkConcatenation> linkConcatenationMap = this.omsAttributesSpan
//            .nonnullLinkConcatenation();
//        for (Map.Entry<LinkConcatenationKey, LinkConcatenation> entry : linkConcatenationMap.entrySet()) {
//            // If the link-concatenation list is not populated or partially populated CD &
//            // PMD shall be derived from link-length (expressed in km in OR topology)
//            if (entry == null || entry.getValue() == null || entry.getValue().getSRLGLength() == null
//                    || entry.getValue().augmentation(LinkConcatenation1.class).getFiberType() == null) {
//                if (this.length > 0.0) {
//                    LOG.debug("In PceLink: no OMS present; cd & PMD for the link {} extrapolated from link length {}"
//                        + "assuming SMF fiber type", link.getUuid().getValue(), this.length);
//                    return calcCDandPMDfromLength();
//                }
//                // If Link-length upper attributes not present or incorrectly populated, no way
//                // to calculate CD & PMD
//                LOG.error("In PceLink: no Link length declared and no OMS present for the link {}."
//                    + " No Way to compute CD and PMD", link.getUuid().getValue());
//                return Map.of();
//            }
//            // SRLG length is expressed in OR topology in meter
//            linkCd += entry.getValue().getSRLGLength().doubleValue() / 1000.0 * retrieveCdFromFiberType(
//                entry.getValue().augmentation(LinkConcatenation1.class).getFiberType());
//            if (entry.getValue().augmentation(LinkConcatenation1.class).getPmd() == null
//                   || entry.getValue().augmentation(LinkConcatenation1.class).getPmd().getValue().doubleValue() == 0.0
//                    || entry.getValue().augmentation(LinkConcatenation1.class).getPmd().getValue()
//                    .toString().isEmpty()) {
//                linkPmd2 += Math.pow(entry.getValue().getSRLGLength().doubleValue() / 1000.0
//                    * retrievePmdFromFiberType(entry.getValue().augmentation(LinkConcatenation1.class)
//                    .getFiberType()),2);
//            } else {
//                linkPmd2 += Math
//                   .pow(entry.getValue().augmentation(LinkConcatenation1.class).getPmd().getValue().doubleValue(), 2);
//            }
//        }
        double linkPmd2 = 0.0;
        LOG.debug("In PceLink: The CD and PMD2 of link {} are respectively {} ps and {} ps", link.getUuid(), linkCd,
            linkPmd2);
        return Map.of("CD", linkCd,"PMD2", linkPmd2);
    }

    // compute default spanLoss and power correction from fiber length
    // when no OMS attribute defined
    private Map<String, Double> calcDefaultSpanLoss(Link link) {
        // TODO when 2.4 models available
        LOG.error("Uncomplete method");
        Map<String, Double> omsExtrapolatedCharac = new HashMap<>();
//        Link1 link1 = link.augmentation(Link1.class);
//        if (link1.getLinkLength() == null || link1.getLinkLength().doubleValue() == 0) {
//            LOG.error("In PceLink, no link length present or length declared = 0,"
//                + " unable to calculate default span Loss ");
//            return omsExtrapolatedCharac;
//        }
        long linkLength = 0;
        LOG.warn("In PceLink {}, assume G.652 fiber, calculation "
            + "based on fiber length of {} km and typical loss of 0.25dB per Km ",
            link.getUuid(), linkLength);
        omsExtrapolatedCharac.put("SpanLoss", linkLength * 0.25);
        omsExtrapolatedCharac.put("PoutCorrection", retrievePower(FiberType.Smf));
        return omsExtrapolatedCharac;
    }

    // Compute the attenuation of a span from OMS attribute
    private Map<String, Double> calcSpanLoss(Link link) {
        // TODO when 2.4 models available
        LOG.error("Uncomplete method");
        if (this.otsSpec == null) {
            return calcDefaultSpanLoss(link);
        }
//        Collection<LinkConcatenation> linkConcatenationList = this.omsAttributesSpan.nonnullLinkConcatenation()
//            .values();
//        if (linkConcatenationList == null) {
//            LOG.error("in PceLink : Null field in the OmsAttrubtesSpan");
//            return calcDefaultSpanLoss(link);
//        }
//        Iterator<LinkConcatenation> linkConcatenationiterator = linkConcatenationList.iterator();
//        if (!linkConcatenationiterator.hasNext()) {
//            return calcDefaultSpanLoss(link);
//        }
        // Reference of power to be launched at input of ROADM (dBm)
        Map<String, Double> omsCharacteristics = new HashMap<>();
//        omsCharacteristics.put("PoutCorrection",
//            retrievePower(linkConcatenationiterator.next().augmentation(LinkConcatenation1.class)
//                .getFiberType()) - 2.0);
//        // span loss of the span
//        omsCharacteristics.put("SpanLoss", this.omsAttributesSpan.getSpanlossCurrent().getValue().doubleValue());
        return omsCharacteristics;
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

//    private double retrievePmdFromFiberType(FiberType fiberType) {
//        if (fiberType.toString().equalsIgnoreCase("Dsf")) {
//            return 0.2;
//        } else {
//            return PMD_CONSTANT;
//        }
//    }

    private double retrieveCdFromFiberType(FiberType fiberType) {
        double cdPerKm;
        switch (fiberType) {
            case Smf:
                cdPerKm = 16.5;
                break;
            case Eleaf:
                cdPerKm = 4.3;
                break;
            case Truewavec:
                cdPerKm = 3.0;
                break;
            case Oleaf:
                cdPerKm = 4.3;
                break;
            case Dsf:
                cdPerKm = 0.0;
                break;
            case Truewave:
                cdPerKm = 4.4;
                break;
            case NzDsf:
                cdPerKm = 4.3;
                break;
            case Ull:
                cdPerKm = 16.5;
                break;
            default:
                cdPerKm = 16.5;
                break;
        }
        return cdPerKm;
    }

    public Uuid getOppositeLink() {
        return oppositeLink;
    }

    public AdministrativeState getAdminStates() {
        return adminStates;
    }

    public OperationalState getState() {
        return opState;
    }

    public Uuid getSourceTP() {
        return sourceTpId;
    }

    public Uuid getDestTP() {
        return destTpId;
    }

    public OpenroadmLinkType getlinkType() {
        return linkType;
    }

    public Uuid getLinkId() {
        return linkId;
    }

    public Uuid getSourceId() {
        return sourceNodeId;
    }

    public Uuid getDestId() {
        return destNodeId;
    }

    public Map<NameKey,Name> getLinkName() {
        return this.linkName;
    }

    public String getClient() {
        return client;
    }

    public Double getLength() {
        return length;
    }

    public void setClient(String client) {
        this.client = client;
    }

    // Double for transformer of JUNG graph
    public Double getLatency() {
        return latency.doubleValue();
    }

    public Double getAvailableBandwidth() {
        return availableBandwidth;
    }

    public Double getUsedBandwidth() {
        return usedBandwidth;
    }

    public String getsourceNetworkSupNodeId() {
        return sourceNetworkSupNodeId;
    }

    public String getdestNetworkSupNodeId() {
        return destNetworkSupNodeId;
    }

    public Set<String> getsrlgList() {
        return srlgList;
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
        if ((this.linkId == null) || (this.linkType == null) || (this.oppositeLink == null)) {
            isValid = false;
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", linkId);
        }
        isValid = checkParams();
        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            if ((this.length == null || this.length == 0.0)
                    && this.otsSpec == null) {
                isValid = false;
                LOG.error("PceLink: Error reading Span for OMS link, and no available generic link information."
                    + " Link {} named {} is ignored ", linkId, linkName);
            } else if ((this.length == null || this.length == 0.0)) {
                   // && this.otsSpec.getSpanlossCurrent() == null) {
                isValid = false;
                LOG.error("PceLink: Error reading Span for OTS Spec, and no available generic link information."
                    + " Link {} named {} is ignored ", linkId, linkName);
            }
        }
//        if (this.srlgList != null && this.srlgList.isEmpty()) {
//            isValid = false;
//            LOG.error("PceLink: Empty srlgList for OMS link. Link is ignored {}", linkId);
//        }
        return isValid;
    }

    public boolean isOtnValid(Link link, String serviceType) {
        // TODO: OTN not planned at initialization of T-API functionality
        // Function to be coded at a later step
        if (this.linkType != OpenroadmLinkType.OTNLINK) {
            LOG.error("PceLink: Not an OTN link. Link is ignored {}", linkId);
            return false;
        }
//      Next line to be changed (set to solve compilation issue)
        OtnLinkType otnLinkType = OtnLinkType.OTU4;
//            link
//            .augmentation(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils
//                    .rev220630.Link1.class)
//            .getOtnLinkType();
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
        if ((this.adminStates == null) || (this.opState == null)) {
            LOG.error("PceLink: Link is not available. Link is ignored {}", linkId);
            return false;
        }
        if ((this.sourceNodeId == null) || (this.destNodeId == null) || (this.sourceTpId == null)
                || (this.destTpId == null)) {
            LOG.error("PceLink: No Link source or destination is available. Link is ignored {}", linkId);
            return false;
        }
        if ((this.sourceNetworkSupNodeId.equals("")) || (this.destNetworkSupNodeId.equals(""))) {
            LOG.error("PceLink: No Link source SuppNodeID or destination SuppNodeID is available. Link is ignored {}",
                linkId);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PceLink type=" + linkType + " ID=" + linkId.getValue() + " latency=" + latency;
    }
}
