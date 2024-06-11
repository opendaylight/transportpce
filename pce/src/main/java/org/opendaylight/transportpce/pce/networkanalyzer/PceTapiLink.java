/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.pce.networkanalyzer.TapiOpticalNode.DirectionType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev220630.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OtsMediaConnectionEndPointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.impairments.ImpairmentRouteEntry;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.media.connection.end.point.spec.OtsImpairments;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({ "SE_BAD_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED",
    "SE_NO_SERIALVERSIONID" })

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

    // private Map<NameKey, Name> linkName;
    private Name linkName;
    private final Uuid linkId;
    private OpenroadmLinkType linkType;
    private TopologyKey topoId;
    private Uuid sourceNodeId;
    private Uuid destNodeId;
    private Uuid sourceTpId;
    private Uuid destTpId;
    private String sourceNetworkSupNodeId;
    private String destNetworkSupNodeId;
    private String client = "";
    private ForwardingDirection direction;
    private Uuid oppositeLink;
    private final AdministrativeState adminStates;
    private final OperationalState opState;
    private Long latency;
    private final Double availableBandwidth;
    private final Double usedBandwidth;
    private final Set<String> srlgList;
    // source index will be set to reflect whether the source is NodeX (0) or
    // NodeY
    // (1)
    private int sourceIndex;
    private Double length;
    private Double cd;
    private Double pmd2;
    private Double spanLoss;
    private Double powerCorrection;
    private transient OtsMediaConnectionEndPointSpec sourceOtsSpec;
    private transient OtsMediaConnectionEndPointSpec destOtsSpec;
    // meter per ms
    private static final double GLASSCELERITY = 2.99792458 * 1e5 / 1.5;
    private static final double PMD_CONSTANT = 0.04;

    public PceTapiLink(TopologyKey topologyId, Link link, PceNode nodeX, PceNode nodeY) {
        LOG.debug("PceLink: : PceLink start ");
        this.linkId = link.getUuid();
        this.linkName = link.getName().values().stream().findFirst().orElseThrow();
        this.topoId = topologyId;
        this.direction = link.getDirection();

        retrieveSrcDestNodeIds(topoId, link, nodeX, nodeY);
        qualifyLinkType(nodeX, nodeY);

        this.oppositeLink = calcOpposite(link);

        this.adminStates = link.getAdministrativeState();
        this.opState = link.getOperationalState();

        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            retrieveEndPointSpecs(nodeX, nodeY);
            qualifyLineLink(link);
            this.srlgList = TapiMapUtils.getSRLG(link);
            this.availableBandwidth = 0.0;
            this.usedBandwidth = 0.0;
        } else if (this.linkType == OpenroadmLinkType.OTNLINK) {
            this.availableBandwidth = TapiMapUtils.getAvailableBandwidth(link);
            this.usedBandwidth = TapiMapUtils.getUsedBandwidth(link);
            this.srlgList = TapiMapUtils.getSRLG(link);
            this.latency = 0L;
            this.length = 0.0;
            this.sourceOtsSpec = null;
            this.destOtsSpec = null;
            this.spanLoss = 0.0;
            this.powerCorrection = 0.0;
            this.cd = 0.0;
            this.pmd2 = 0.0;
        } else {
            this.sourceOtsSpec = null;
            this.destOtsSpec = null;
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
        LOG.debug("PceLink: created PceLink  {} for topo {}", linkId, topoId);
    }

    public PceTapiLink(Name linkName, Uuid linkUuid, Uuid sourceTpUuid, Uuid destTpUuid, PceNode nodeX, PceNode nodeY) {

        LOG.debug("PceLink: : PceLink start ");
        this.sourceIndex = 0;
        this.linkId = linkUuid;
        this.linkName = linkName;
        this.direction = ForwardingDirection.BIDIRECTIONAL;
        this.sourceNodeId = nodeX.getNodeUuid();
        this.destNodeId = nodeY.getNodeUuid();
        this.sourceTpId = sourceTpUuid;
        this.destTpId = destTpUuid;
        this.oppositeLink = linkUuid;
        this.adminStates = AdministrativeState.UNLOCKED;
        this.opState = OperationalState.ENABLED;
        this.sourceOtsSpec = null;
        this.destOtsSpec = null;
        this.srlgList = null;
        this.latency = 0L;
        this.length = 0.0;
        this.availableBandwidth = 0.0;
        this.usedBandwidth = 0.0;
        this.spanLoss = 0.0;
        this.powerCorrection = 0.0;
        this.cd = 0.0;
        this.pmd2 = 0.0;
        qualifyLinkType(nodeX, nodeY);

        LOG.debug("PceLink: created PceLink  {}", linkId);
    }

    private void qualifyLinkType(PceNode nodeX, PceNode nodeY) {

        LOG.info("PCETAPILINK line 167 NodeXListofNEP {}", nodeX.getListOfNep().stream().map(BasePceNep::getNepCepUuid)
            .collect(Collectors.toList()));
        LOG.info("PCETAPILINK line 167 NodeYListofNEP {}", nodeY.getListOfNep().stream().map(BasePceNep::getNepCepUuid)
            .collect(Collectors.toList()));
        LOG.info("PCETAPILINK line 173 SourceTpId = {}, destTpId = {}", sourceTpId, destTpId);
        OpenroadmTpType sourceTpType = OpenroadmTpType.EXTPLUGGABLETP;
        OpenroadmTpType destTpType = OpenroadmTpType.EXTPLUGGABLETP;
        if (sourceIndex == 0) {
            sourceTpType = nodeX.getListOfNep().stream().filter(bpn -> sourceTpId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow().getTpType();
            destTpType = nodeY.getListOfNep().stream().filter(bpn -> destTpId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow().getTpType();
        } else if (sourceIndex == 1) {
            sourceTpType = nodeY.getListOfNep().stream().filter(bpn -> sourceTpId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow().getTpType();
            destTpType = nodeX.getListOfNep().stream().filter(bpn -> destTpId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow().getTpType();
        } else {
            LOG.error("TapiPceLink: Error proceeding Link {} for which source and dest NEP can not be identified ",
                linkId.getValue());
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
                    linkId.getValue());
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
            // In case of Bidirectional link we don't care of who is the source
            // or the
            // destination
            sourceindex = 0;
        } else {
            // Unidirectional or undefined case
            Uuid nep0Uuid = link.getNodeEdgePoint().entrySet().iterator().next().getKey().getNodeEdgePointUuid();
            DirectionType nepDirectionX = nodeX.getListOfNep().stream()
                .filter(bpN -> bpN.getNepCepUuid().equals(nep0Uuid)).findFirst().orElseThrow().getDirection();
            DirectionType nepDirectionY = nodeY.getListOfNep().stream()
                .filter(bpN -> bpN.getNepCepUuid().equals(nep0Uuid)).findFirst().orElseThrow().getDirection();
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

    private void retrieveEndPointSpecs(PceNode nodeX, PceNode nodeY) {
        OtsMediaConnectionEndPointSpec otsCepSpecX = nodeX.getListOfNep().stream()
            .filter(bpn -> this.sourceTpId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow().getCepOtsSpec();
        OtsMediaConnectionEndPointSpec otsCepSpecY = nodeY.getListOfNep().stream()
            .filter(bpn -> this.sourceTpId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow().getCepOtsSpec();
        if ((otsCepSpecX == null) && (otsCepSpecY == null)) {
            LOG.error("TapiOpticalLink[retrieveEndPointSpecs]: Error retrieving OTS Cep Spec for link {} named {} ",
                linkId, linkName.toString());
            return;
        } else if (sourceIndex == 0) {
            sourceOtsSpec = otsCepSpecX;
            destOtsSpec = otsCepSpecY;
        } else if (sourceIndex == 1) {
            sourceOtsSpec = otsCepSpecY;
            destOtsSpec = otsCepSpecX;
        } else {
            LOG.error("TapiOpticalLink{retrieveEndPointSpecs}: Error retrieving OTS Cep Spec for link {} named {}: "
                + "undetermined direction for the link ", linkId, linkName.toString());
        }
        return;
    }

    // Retrieve the opposite link
    private Uuid calcOpposite(Link link) {
        Uuid tmpoppositeLink = TapiMapUtils.extractOppositeLink(link);
        if (tmpoppositeLink == null) {
            LOG.error("PceLink: Error calcOpposite. Link for link {} named {}", link.getUuid().getValue(),
                link.getName().toString());
            // isValid = false;
        }
        return tmpoppositeLink;
    }

    // Compute the link latency : if the latency is not defined, the latency is
    // computed from the length
    private Long calcLatency(double fiberLength) {
        LOG.debug("In PceLink: The latency of link {} named {} is extrapolated from link length and == {}",
            linkId, linkName, fiberLength * 1000 / GLASSCELERITY);
        return (long) Math.ceil(length * 1000 / GLASSCELERITY);
    }

    // Compute the main parameters of Line Links from T-API CEP OTS
    // specification
    private void qualifyLineLink(Link link) {
        Double dpmd2 = 0.0;
        Double dlength = 0.0;
        Double dtotalLoss = 0.0;
        String fiberType = "SMF";
        boolean firstLoop = true;
        if (sourceOtsSpec == null && destOtsSpec == null) {
            LOG.debug("In PceTapiLink, on Link {} named  {} no OTS present, assume direct connection of 0 km",
                link.getUuid(), linkName);
        } else {
            List<OtsImpairments> otsImpairment;
            if (sourceOtsSpec != null) {
                otsImpairment = sourceOtsSpec.getOtsImpairments();
            } else {
                otsImpairment = destOtsSpec.getOtsImpairments();
            }
            List<ImpairmentRouteEntry> imRoEnt;
            for (OtsImpairments otsImp : otsImpairment) {
                imRoEnt = otsImp.getImpairmentRouteEntry();
                for (ImpairmentRouteEntry ire : imRoEnt) {
                    dlength = dlength + ire.getOtsFiberSpanImpairments().getLength().doubleValue();
                    dpmd2 = dpmd2 + Math.pow(ire.getOtsFiberSpanImpairments().getPmd().doubleValue(), 2);
                    dtotalLoss = dtotalLoss + ire.getOtsFiberSpanImpairments().getTotalLoss().doubleValue();
                    if (firstLoop) {
                        fiberType = ire.getOtsFiberSpanImpairments().getFiberTypeVariety();
                        firstLoop = false;
                    }
                }
            }
            if (ForwardingDirection.BIDIRECTIONAL.equals(direction)) {
                Double ddpmd2 = 0.0;
                Double ddlength = 0.0;
                Double ddtotalLoss = 0.0;
                otsImpairment = destOtsSpec.getOtsImpairments();
                for (OtsImpairments otsImp : otsImpairment) {
                    imRoEnt = otsImp.getImpairmentRouteEntry();
                    for (ImpairmentRouteEntry ire : imRoEnt) {
                        ddlength = ddlength + ire.getOtsFiberSpanImpairments().getLength().doubleValue();
                        ddpmd2 = ddpmd2 + Math.pow(ire.getOtsFiberSpanImpairments().getPmd().doubleValue(), 2);
                        ddtotalLoss = ddtotalLoss + ire.getOtsFiberSpanImpairments().getTotalLoss().doubleValue();
                    }
                }
                dlength = Math.max(dlength, ddlength);
                dpmd2 = Math.max(dpmd2, ddpmd2);
                dtotalLoss = Math.max(dtotalLoss, ddtotalLoss);
            }
            if (fiberType == null) {
                fiberType = "SMF";
            }
        }
        FiberType orFiberType = StringConstants.FIBER_TYPES_TABLE.get(fiberType);
        if (orFiberType == null) {
            orFiberType = FiberType.Smf;
            LOG.warn("In PceTapiLink[qualifyLineLink], no information retrieved on link {} named {} about fiber "
                + " type --> will assume SMF fiber is used", link.getUuid(), linkName.toString());
        }

        if (dlength == 0.0) {
            LOG.warn(
                "In PceTapiLink[qualifyLineLink], no information retrieved on link {} named {} about length"
                    + " or length equal to 0 km --> will assume length of 0 km",
                link.getUuid(), linkName.toString());
            this.cd = 0.0;
            if (dtotalLoss == 0.0) {
                LOG.warn(
                    "In PceTapiLink[qualifyLineLink], no information retrieved on link {} named {} about length"
                        + " (or length equal to 0 km) and Loss --> will assume loss of 0 dB",
                    link.getUuid(), linkName.toString());

            }
        } else {
            // Length is different from 0, can calculate CD from length
            this.cd = dlength * retrieveCdFromFiberType(orFiberType);
            if (dtotalLoss == 0.0) {
                dtotalLoss = 1 + 0.25 * dlength;
                LOG.warn("In PceTapiLink[qualifyLineLink], no information retrieved on link {} named {} about loss"
                    + " --> will assume loss of 0.25 dB/km + 1 dB for connectors which gives a total loss of {} dB",
                    link.getUuid(), linkName.toString(), dtotalLoss);
            }
        }
        this.length = dlength;
        this.latency = calcLatency(dlength);
        this.spanLoss = dtotalLoss;
        this.powerCorrection = retrievePower(orFiberType) - 2.0;
        if (dpmd2 == 0.0) {
            dpmd2 = Math.pow(this.length * PMD_CONSTANT, 2);
        }
        this.pmd2 = dpmd2;
        return;
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

    public boolean isValid() {
        if ((this.linkId == null) || (this.linkType == null)
        // || (this.oppositeLink == null)
        ) {
            isValid = false;
            LOG.error("PceLink: No Link type or opposite link is available. Link is ignored {}", linkId);
        }
        isValid = checkParams();
        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            if ((this.length == null || this.length == 0.0)
                || (this.sourceOtsSpec == null && this.destOtsSpec == null)) {
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
        return isValid;
    }

    public boolean isOtnValid(Link link, String serviceType) {
        // TODO: OTN not planned at initialization of T-API functionality
        // Function to be coded at a later step
        if (this.linkType != OpenroadmLinkType.OTNLINK) {
            LOG.error("PceLink: Not an OTN link. Link is ignored {}", linkId);
            return false;
        }
        // TODO: Next line to be changed (set to solve compilation issue)
        OtnLinkType otnLinkType = OtnLinkType.OTU4;
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
                // TODO: Here link type needs to be changed, based on the
                // line-rate
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
        if ((this.linkId == null) || (this.linkType == null)
        // || (this.oppositeLink == null)
        ) {
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

    public void setOppositeLink(Uuid oppositeLinkId) {
        this.oppositeLink = oppositeLinkId;
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

    public Name getLinkName() {
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

    @Override
    public String toString() {
        return "PceLink type=" + linkType + " ID=" + linkId.getValue() + " latency=" + latency + " Name=" + linkName;
    }
}
