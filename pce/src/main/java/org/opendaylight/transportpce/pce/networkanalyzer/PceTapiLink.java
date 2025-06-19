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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.pce.networkanalyzer.TapiOpticalNode.DirectionType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.link.rev230526.span.attributes.LinkConcatenation1.FiberType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OtsMediaConnectionEndPointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.impairments.ImpairmentRouteEntry;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.media.connection.end.point.spec.OtsImpairments;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({ "SE_BAD_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED",
    "SE_NO_SERIALVERSIONID" })

public class PceTapiLink implements Serializable, PceLink {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceTapiLink.class);
    ///////////////////////// LINKS ////////////////////
    /*
     * extension of Link to include constraints and Graph weight
     */
    double weight = 0;
    private boolean isValid = false;

    // this member is for XPONDER INPUT/OUTPUT links.
    // it keeps name of client corresponding to NETWORK TP

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
    private Map<ConnectionEndPointKey, ConnectionEndPoint> cepMap;
    private Map<NodeEdgePointKey, NodeEdgePoint> nepMap;
    private String client = "";
    private String orgNode = "";
    private LayerProtocolName lpn;
    private LAYERPROTOCOLQUALIFIER lpq;
    private ForwardingDirection direction;
    private Uuid oppositeLink;
    private final AdministrativeState adminStates;
    private final OperationalState opState;
    private Long latency;
    private Double availableBandwidth;
    private Double usedBandwidth;
    private final Set<String> srlgList;
    // source index will be set to reflect whether the source is NodeX (0) or NodeY (1)
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

    /**
     * PceTapiLink joining 2 PceTapiOpticalNodes is an abstraction of a physical link (Graph edge for path computation).
     *  As far as the 2 PceNodes on both end of the link are PceTapiOpticalNodes, the link corresponds to a physical
     *  optical link (Photonic layer). Considering OpenROADM representation, it can be an ADD/Drop/Express link for
     *  ROADM's internal links, a ROADM/ILA to ROADM/ILA link for physical optical links interconnecting ROADMs, or an
     *  XPONDER to ROADM link.
     * @param topologyId    A Uuid (TopologyKey) used to define the topology the link belongs to
     * @param link          A link of the T-API topology.
     * @param nodeX         A PceNode (TapiPceOpticalNode) corresponding to one end of the PceTapiLink,
     * @param nodeY         A PceNode (TapiPceOpticalNode) corresponding to the other end of the PceTapiLink,
     */
    public PceTapiLink(TopologyKey topologyId, Link link, PceNode nodeX, PceNode nodeY) {
        LOG.debug("PceLink: : PceLink start ");
        this.linkId = link.getUuid();
        this.linkName = link.getName().values().stream().findFirst().orElseThrow();
        this.topoId = topologyId;
        this.direction = link.getDirection();
        if (ForwardingDirection.BIDIRECTIONAL.equals(direction)) {
            this.oppositeLink = link.getUuid();
        }
        this.nepMap = link.getNodeEdgePoint();
        retrieveSrcDestNodeIds(topoId, link.getUuid(), link.getDirection(), nodeX, nodeY, true);
        LOG.info("PceTapiLInk Line 107 : Processing Link {}, SourceTp Uuid = {}, DestTpUuid = {}",
            link.getName(), sourceTpId, destTpId);

        this.adminStates = link.getAdministrativeState();
        this.opState = link.getOperationalState();
        if (!isValid) {
            this.srlgList = null;
            return;
        } else {
            qualifyLinkType(nodeX, nodeY);
        }

        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            retrieveEndPointSpecs(nodeX, nodeY);
            LOG.info("PCETAPILINK line 116 Calling QualifyLineLink");
            qualifyLineLink(link);
            this.isValid = isPhyValid();
            this.srlgList = TapiMapUtils.getSRLG(link);
            this.lpn = LayerProtocolName.PHOTONICMEDIA;
            this.lpq = null;
            this.availableBandwidth = 96.0;
            this.usedBandwidth = 0.0;
        } else {
            this.sourceOtsSpec = null;
            this.destOtsSpec = null;
            this.srlgList = null;
            this.latency = 0L;
            this.length = 0.0;
            this.availableBandwidth = TapiMapUtils.getAvailableBandwidth(link);
            this.usedBandwidth = TapiMapUtils.getUsedBandwidth(link);
            this.spanLoss = 0.0;
            this.powerCorrection = 0.0;
            this.cd = 0.0;
            this.pmd2 = 0.0;
        }

        LOG.debug("PceLink: created PceLink  {} for topo {}", linkId, topoId);
    }

    /**
     * PceTapiLink joining 2 TapiOtnNodes is an abstraction of an OTN link (Graph edge for path computation).
     *  As far as the 2 PceNodes on both end of the link are PceTapiOtnNodes, the link corresponds to an OTN link which
     *  can be of different sub-layers/LayerProtocolQualifier (OTU4, OTUCN, ODUCN, ODU4/3/2).
     *  In T-API topology, OTN links are Top-connections established between ConnectionEndPoints.
     * @param topologyId    A Uuid (TopologyKey) used to define the topology the link belongs to.
     * @param conn           A connection of the T-API connectivity-context.
     * @param nodeX         A PceNode (TapiPceOtnNode) corresponding to one end of the PceTapiLink,
     * @param nodeY         A PceNode (TapiPceOtnNode) corresponding to the other end of the PceTapiLink,
     * @param serviceType   The serviceType which is associated to a specific OTN-layer/LayerProtocolQualifier.
     */
    public PceTapiLink(TopologyKey topologyId, Connection conn, PceNode nodeX, PceNode nodeY, String serviceType) {
        LOG.debug("PceLink: : PceLink start ");
        //This is the constructor for OTN Link which correspond to connections in T-API
        this.linkId = conn.getUuid();
        this.linkName = conn.getName().values().stream().findFirst().orElseThrow();
        this.topoId = topologyId;
        this.direction = conn.getDirection();
        this.linkType = OpenroadmLinkType.OTNLINK;
        this.srlgList = null;
        this.lpn = conn.getLayerProtocolName();
        //Administrative state is not defined for connections : will assume it is UNLOCKED by default
        this.adminStates = AdministrativeState.UNLOCKED;
        this.opState = conn.getOperationalState();
        if (ForwardingDirection.BIDIRECTIONAL.equals(direction)) {
            this.oppositeLink = conn.getUuid();
        }
        if (!conn.getLayerProtocolName().equals(LayerProtocolName.DIGITALOTN)
                && !conn.getLayerProtocolName().equals(LayerProtocolName.ODU)) {
            this.isValid = false;
            return;
        }
        this.lpn = LayerProtocolName.DIGITALOTN;
        this.lpq = conn.getLayerProtocolQualifier();
        LOG.debug("PceTapiLInk Line 191, Connection lpq of OTN link is : {} ", lpq);
        if (!(TapiMapUtils.SERV_TYPE_OTN_LINK_TYPE.get(serviceType) != null
                && TapiMapUtils.LPN_OR_TAPI.get(TapiMapUtils.SERV_TYPE_OTN_LINK_TYPE.get(serviceType)) != null
                && TapiMapUtils.LPN_OR_TAPI.get(TapiMapUtils.SERV_TYPE_OTN_LINK_TYPE.get(serviceType))
                    .equals(this.lpq))) {
            this.isValid = false;
            return;
        }
        this.cepMap = conn.getConnectionEndPoint();
        LOG.info("PceLink Line 186: calling  retrieveSrcDestNodeIds for PceLink OTN {} ", linkId);
        retrieveSrcDestNodeIds(topoId, conn.getUuid(), conn.getDirection(), nodeX, nodeY, false);

        calculateOtnBandwidth(nodeX, nodeY);
        this.isValid = isOtnValid(serviceType);
        if (!isValid) {
            return;
        }

        this.latency = 0L;
        this.length = 0.0;
        this.sourceOtsSpec = null;
        this.destOtsSpec = null;
        this.spanLoss = 0.0;
        this.powerCorrection = 0.0;
        this.cd = 0.0;
        this.pmd2 = 0.0;

        LOG.debug("PceLink: created PceLink OTN {} for topo {}", linkId, topoId);
    }

    /**
     * Generic PceTapiLink joining 2 Tapi Nodes is an abstraction of topological link (Graph edge for path computation).
     *  Generic Link defined between 2 PceNodes that could be either a physical or an OTN link.
     * @param linkName      The link Name used in the T-API topology.
     * @param linkUuid      The link Uuid used to define uniquely the link in the T-API topology.
     * @param sourceTpUuid  The source Tp Uuid.
     * @param destTpUuid    The destination Tp Uuid.
     * @param nodeX         A PceNode corresponding to one end of the PceTapiLink,
     * @param nodeY         A PceNode corresponding to the other end of the PceTapiLink.
     */
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

    /**
     * Sets availableBandwidth from the source Tp AvailableCapacity container for OTN Links.
     * @param nodeX         A PceNode (TapiPceOtnNode) corresponding to one end of the PceTapiLink,
     * @param nodeY         A PceNode (TapiPceOtnNode) corresponding to the other end of the PceTapiLink.
     */
    private void calculateOtnBandwidth(PceNode nodeX, PceNode nodeY) {
        PceTapiOtnNode nodeXX = (PceTapiOtnNode) nodeX;
        PceTapiOtnNode nodeYY = (PceTapiOtnNode) nodeY;
        Double availableBandwidthSrc = 0.0;
        Double availableBandwidthDst = 0.0;
        switch (this.sourceIndex) {

            case 0:
                availableBandwidthSrc = nodeXX.getAvailableCapacityFromUuid(this.sourceTpId);
                availableBandwidthDst = nodeYY.getAvailableCapacityFromUuid(this.destTpId);
                break;
            case 1:
                availableBandwidthSrc = nodeYY.getAvailableCapacityFromUuid(this.sourceTpId);
                availableBandwidthDst = nodeXX.getAvailableCapacityFromUuid(this.destTpId);
                break;
            default:
                break;
        }
        if (availableBandwidthSrc == null && availableBandwidthDst != null) {
            availableBandwidthSrc = availableBandwidthDst;
        } else if (availableBandwidthSrc != null && availableBandwidthDst != null) {
            availableBandwidthSrc = Math.min(availableBandwidthSrc, availableBandwidthDst);
        }
        this.availableBandwidth = availableBandwidthSrc == null ? null : availableBandwidthSrc * 1000.0;
    }

    /**
     * Qualifies the link type from the Tp Type of the Origin NEP for physical optical links.
     * @param nodeX         A PceNode (TapiPceOpticalNode) corresponding to one end of the PceTapiLink,
     * @param nodeY         A PceNode (TapiPceOpticalNode) corresponding to the other end of the PceTapiLink.
     */
    private void qualifyLinkType(PceNode nodeX, PceNode nodeY) {

        LOG.debug("PCETAPILINK line 299 NodeXListofNEP {}", nodeX.getListOfNep().stream().map(BasePceNep::getNepCepUuid)
            .collect(Collectors.toList()));
        LOG.debug("PCETAPILINK line 301 NodeYListofNEP {}", nodeY.getListOfNep().stream().map(BasePceNep::getNepCepUuid)
            .collect(Collectors.toList()));
        LOG.debug("PCETAPILINK line 303 SourceTpId = {}, destTpId = {} and sourceIndex is {}", sourceTpId, destTpId,
            sourceIndex);
        BasePceNep sourceNep;
        BasePceNep destNep;
        if (sourceIndex == 0) {
            sourceNep = nodeX.getListOfNep().stream().filter(bpn -> sourceTpId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow();
            destNep = nodeY.getListOfNep().stream().filter(bpn -> destTpId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow();
        } else if (sourceIndex == 1) {
            sourceNep = nodeY.getListOfNep().stream().filter(bpn -> sourceTpId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow();
            destNep = nodeX.getListOfNep().stream().filter(bpn -> destTpId.equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow();
        } else {
            LOG.error("TapiPceLink: Error proceeding Link {} for which source and dest NEP can not be identified ",
                linkId.getValue());
            return;
        }
        OpenroadmTpType sourceTpType = sourceNep.getTpType();
        OpenroadmTpType destTpType = destNep.getTpType();
        if (sourceNep.getLpn().equals(LayerProtocolName.PHOTONICMEDIA)) {
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
                    this.linkType = null;
                    LOG.error("TapiPceLink: Error qualifying Link {} type. Set link type to null ",
                        linkId.getValue());
                    break;
            }
        } else {
            this.linkType = OpenroadmLinkType.OTNLINK;
        }
    }

    /**
     * Set main Node parameters for both source and Destination of the PceLink.
     * @param nodeX         A PceNode corresponding to one end of the PceTapiLink,
     * @param nodeY         A PceNode corresponding to the other end of the PceTapiLink.
     */
    private void setSrcDestIds(PceNode nodeX, PceNode nodeY) {
        switch (this.sourceIndex) {
            case 0:
                this.sourceNetworkSupNodeId = nodeX.getSupNetworkNodeId();
                this.destNetworkSupNodeId = nodeY.getSupNetworkNodeId();
                this.sourceNodeId = nodeX.getNodeUuid();
                this.destNodeId = nodeY.getNodeUuid();
                break;
            case 1:
                this.sourceNetworkSupNodeId = nodeY.getSupNetworkNodeId();
                this.destNetworkSupNodeId = nodeX.getSupNetworkNodeId();
                this.sourceNodeId = nodeY.getNodeUuid();
                this.destNodeId = nodeX.getNodeUuid();
                break;
            default:
                break;
        }
    }

    /**
     * Defines which ends is the source/destination, sets sourceindex  and source/destTpId accordingly.
     * @param topoIid       TopologyKey (Uuid) of the Topology the node belongs to.
     * @param linkUuid      Uuid of the Link,
     * @param dir           ForwardingDirection as defined by T-API,
     * @param nodeX         A PceNode corresponding to one end of the PceTapiLink,
     * @param nodeY         A PceNode corresponding to the other end of the PceTapiLink,
     * @param isLink        Boolean which defines whether the input parameters correspond to a link (true) or a
     *                      connection (false).
     */
    private void retrieveSrcDestNodeIds(TopologyKey topoIid, Uuid linkUuid, ForwardingDirection dir,
            PceNode nodeX, PceNode nodeY, boolean isLink) {
        int sourceindex;
        Uuid srcTpUuid = null;
        Uuid destTpUuid = null;
        DirectionType nepDirectionX = null;
        DirectionType nepDirectionY = null;
        String node0 = "";
        Uuid tpUuid0;
        Uuid tpUuid1;
        if (isLink) {
            tpUuid0 = nepMap.entrySet().stream().findFirst().orElseThrow().getKey().getNodeEdgePointUuid();
            tpUuid1 = nepMap.entrySet().stream().filter(nep -> nep.getKey().getNodeEdgePointUuid() != tpUuid0)
                .collect(Collectors.toList()).stream().findFirst().orElseThrow().getKey().getNodeEdgePointUuid();
        } else {
            tpUuid0 = cepMap.entrySet().stream().findFirst().orElseThrow().getKey().getConnectionEndPointUuid();
            tpUuid1 = cepMap.entrySet().stream().filter(cep -> cep.getKey().getConnectionEndPointUuid() != tpUuid0)
                .collect(Collectors.toList()).stream().findFirst().orElseThrow().getKey().getConnectionEndPointUuid();
        }
        if (nodeX.getListOfNep().stream().map(BasePceNep::getNepCepUuid).collect(Collectors.toList())
            .contains(tpUuid0)) {
            sourceindex = 0;
            node0 = "X";
            srcTpUuid = tpUuid0;
            destTpUuid = tpUuid1;
            nepDirectionX = nodeX.getListOfNep().stream()
                .filter(bpN -> bpN.getNepCepUuid().equals(tpUuid0)).findFirst().orElseThrow().getDirection();
            if (!nodeY.getListOfNep().stream().map(BasePceNep::getNepCepUuid).collect(Collectors.toList())
                .contains(destTpUuid)) {
                isValid = false;
                LOG.debug("TapiPceLink: Line  427 did not succeed finding Nep {} in Node Y Listof"
                    + " NEP for Bidir link {}", destTpUuid, linkUuid.getValue());
                return;
            }
        } else {
            if (nodeY.getListOfNep().stream().map(BasePceNep::getNepCepUuid).collect(Collectors.toList())
                .contains(tpUuid0)) {
                sourceindex = 1;
                node0 = "Y";
                destTpUuid = tpUuid1;
                srcTpUuid = tpUuid0;
                nepDirectionY = nodeY.getListOfNep().stream()
                    .filter(bpN -> bpN.getNepCepUuid().equals(tpUuid0)).findFirst().orElseThrow().getDirection();
                if (!nodeX.getListOfNep().stream().map(BasePceNep::getNepCepUuid).collect(Collectors.toList())
                    .contains(destTpUuid)) {
                    isValid = false;
                    LOG.debug("TapiPceLink: Line  443 did not succeed finding Nep {} in Node X Listof"
                        + " NEP for Bidir link {}", srcTpUuid, linkUuid.getValue());
                    return;
                }
            } else {
                isValid = false;
                LOG.debug("TapiPceLink: Line  449 did not succeed finding Xep {} and {} in neither nodeX, or Node Y"
                    + " Listof XEP for Bidir link {}", tpUuid0, tpUuid1, linkUuid.getValue());
                return;
            }
        }
        if (ForwardingDirection.BIDIRECTIONAL.equals(dir)) {
            this.oppositeLink = linkUuid;

        } else {
            // Unidirectional or undefined case
            if (node0.equals("X") && nepDirectionX != null
                && (DirectionType.SOURCE.equals(nepDirectionX) || DirectionType.BIDIRECTIONAL.equals(nepDirectionX))) {
                // sourceindex = 0, and we keep it as is as nothing needs to be changed
            } else if (node0.equals("X") && nepDirectionX != null && DirectionType.SINK.equals(nepDirectionX)) {
                // Need to change source index because first tp found is the one of Node0 = X, but the tp is RX
                sourceindex = 1;
                srcTpUuid = tpUuid1;
                destTpUuid = tpUuid0;
            } else if (node0.equals("Y") && nepDirectionY != null
                && (DirectionType.SOURCE.equals(nepDirectionY) || DirectionType.BIDIRECTIONAL.equals(nepDirectionY))) {
               // sourceindex = 1, and we keep it as is as nothing needs to be changed
            } else if (node0.equals("Y") && nepDirectionY != null && DirectionType.SINK.equals(nepDirectionY)) {
              // Need to change source index because first tp found is the one of Node0 = Y, but the tp is RX
                sourceindex = 0;
                srcTpUuid = tpUuid1;
                destTpUuid = tpUuid0;
            } else {
                LOG.error("TapiPceLink: Line 415 Error proceeding Link {} for which source and dest NEP can not be"
                    + " identified, set is valid to False ", linkUuid.getValue());
                isValid = false;
                return;
            }
        }
        // At the end sourceIndex = 0 <-> NodeX is the Source / sourceIndex = 1 <-> NodeY is the Source
        // Node0 defines the order of items (Cep/Nep) found in either CepMap for connection or NepMap for Link
        //            If Node0 = X, the first item of the xepMap is associated with NodeX
        //            If Node0 = Y, the first item of the xepMap is associated with NodeY
        this.sourceTpId = srcTpUuid;
        this.destTpId = destTpUuid;
        this.sourceIndex = sourceindex;
        this.orgNode = node0;
        LOG.info("PceTapiLink Line 429 : qualifying link {}, sourceindex = {} sourceTPId = {} destTpId = {}",
            linkName, sourceIndex, sourceTpId, destTpId);
        if (sourceIndex == 0 && nodeX.getListOfNep().stream().filter(bpn -> bpn.getNepCepUuid().equals(sourceTpId))
            .collect(Collectors.toList()).isEmpty()) {
            LOG.error("TapiPceLink: Line  420 Handling link {} sourceTp {} not found in NodeX {},"
                + " error on sourceIndex", linkUuid.getValue(), sourceTpId, nodeX.getNodeId());
        } else if (sourceIndex == 1
                && nodeY.getListOfNep().stream().filter(bpn -> bpn.getNepCepUuid().equals(sourceTpId))
                    .collect(Collectors.toList()).isEmpty()) {
            LOG.error("TapiPceLink: Line  425 Handling link {} sourceTp {} not found in NodeY {},"
                + " error on sourceIndex", linkUuid.getValue(), sourceTpId, nodeY.getNodeId());
        } else if (sourceIndex == 0 && nodeY.getListOfNep().stream()
                .filter(bpn -> bpn.getNepCepUuid().equals(destTpId))
                .collect(Collectors.toList()).isEmpty()) {
            LOG.error("TapiPceLink: Line  430 Handling link {} destTp {} not found in NodeY {}, error on sourceIndex",
                linkUuid.getValue(), destTpId, nodeY.getNodeId());
        } else if (sourceIndex == 1
                && nodeX.getListOfNep().stream().filter(bpn -> bpn.getNepCepUuid().equals(destTpId))
                    .collect(Collectors.toList()).isEmpty()) {
            LOG.error("TapiPceLink: Line  434 Handling link {} destTp {} not found in NodeX {}, error on sourceIndex",
                linkUuid.getValue(), destTpId, nodeX.getNodeId());
        } else {
            LOG.info("TapiPceLink: Line  437 Handling link {} sourceTp and destTp {} compatible with sourceIndex",
                linkUuid.getValue(), sourceTpId);
        }
        isValid = true;
        LOG.info("TapiPceLink: Line  404 Handling link {} directions -> is Valid True", linkUuid.getValue());
        setSrcDestIds(nodeX, nodeY);
    }

    /**
     * Retrieve the OtsMediaConnectionEndPointSpec of source/destination CEPs and sets link's params accordingly.
     * @param nodeX         A PceNode (TapiPceOpticalNode) corresponding to one end of the PceTapiLink,
     * @param nodeY         A PceNode (TapiPceOpticalNode) corresponding to the other end of the PceTapiLink.
     */
    private void retrieveEndPointSpecs(PceNode nodeX, PceNode nodeY) {
        if (sourceIndex == 0) {
            sourceOtsSpec = nodeX.getListOfNep().stream()
                .filter(bpn -> this.sourceTpId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow().getCepOtsSpec();
            destOtsSpec = nodeY.getListOfNep().stream()
                .filter(bpn -> this.destTpId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow().getCepOtsSpec();
            LOG.debug("TapiPceLink: Line  531 srcOtsSpec is {}, DestOtsSpec is {}", sourceOtsSpec, destOtsSpec);
        } else {
            destOtsSpec = nodeX.getListOfNep().stream()
                .filter(bpn -> this.destTpId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow().getCepOtsSpec();
            sourceOtsSpec = nodeY.getListOfNep().stream()
                .filter(bpn -> this.sourceTpId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow().getCepOtsSpec();
            LOG.debug("TapiPceLink: Line  537 srcOtsSpec is {}, DestOtsSpec is {}", sourceOtsSpec, destOtsSpec);
        }
        if ((sourceOtsSpec == null) && (destOtsSpec == null)) {
            LOG.error("TapiOpticalLink[retrieveEndPointSpecs]: Error retrieving OTS Cep Spec for link {} named {} ",
                linkId, linkName.toString());

        }
        return;
    }

    /**
     * Calculates latency from length for physical optical links.
     * @param fiberLength   The link length expressed in kms.
     * @return              Long the latency expressed in seconds.
     */
    private Long calcLatency(double fiberLength) {
        LOG.debug("In PceLink: The latency of link {} named {} is extrapolated from link length and == {}",
            linkId, linkName, fiberLength * 1000 / GLASSCELERITY);
        return (long) Math.ceil(length / GLASSCELERITY * 1E6);
    }

    /**
     * Compute the main parameters of line physical Optical Links from preset T-API CEP OtsMediaCepSpecs.
     *  Main link physical parameters includes length, loss, pmd and cd.
     * @param link The Link as defined in T-API topology.
     */
    private void qualifyLineLink(Link link) {
        Double dpmd2 = 0.0;
        Double dlength = 0.0;
        Double dtotalLoss = 0.0;
        String fiberType = "SMF";
        String efiberType = "SMF";
        boolean firstLoop = true;
        LOG.info("In PceTapiLink[Line 460], qualify link {} named {} with OTSCepSpec for SOURCE {} and DEST {}",
            link.getUuid(), linkName.toString(), sourceOtsSpec, destOtsSpec);
        if (sourceOtsSpec == null && destOtsSpec == null) {
            LOG.info("In PceTapiLink, on Link {} named  {} no OTS present, assume direct connection of 0 km",
                link.getUuid(), linkName);
            return;
        } else {
            List<OtsImpairments> otsImpairment;
            if (sourceOtsSpec != null) {
                otsImpairment = sourceOtsSpec.getOtsImpairments();
            } else {
                otsImpairment = destOtsSpec.getOtsImpairments();
            }
            OtsImpairments otsImp = otsImpairment.stream()
                .filter(otsI -> otsI.getIngressDirection() == true)
                .findAny().orElseThrow();
            List<ImpairmentRouteEntry> imRoEntry = otsImp.getImpairmentRouteEntry();
            if (imRoEntry != null) {
                for (ImpairmentRouteEntry ire : imRoEntry) {
                    dlength = dlength + ire.getOtsFiberSpanImpairments().getLength().doubleValue();
                    dpmd2 = dpmd2 + Math.pow(ire.getOtsFiberSpanImpairments().getPmd().doubleValue(), 2);
                    dtotalLoss = dtotalLoss + ire.getOtsFiberSpanImpairments().getTotalLoss().doubleValue();
                    if (firstLoop) {
                        fiberType = ire.getOtsFiberSpanImpairments().getFiberTypeVariety();
                        firstLoop = false;
                    }
                }
            }
            Double edpmd2 = 0.0;
            Double edlength = 0.0;
            Double edtotalLoss = 0.0;
            boolean efirstLoop = true;
            otsImp = otsImpairment.stream()
                .filter(otsI -> otsI.getIngressDirection() == false)
                .findAny().orElseThrow();
            imRoEntry = otsImp.getImpairmentRouteEntry();
            if (imRoEntry != null) {
                for (ImpairmentRouteEntry ire : imRoEntry) {
                    edlength = edlength + ire.getOtsFiberSpanImpairments().getLength().doubleValue();
                    edpmd2 = edpmd2 + Math.pow(ire.getOtsFiberSpanImpairments().getPmd().doubleValue(), 2);
                    edtotalLoss = edtotalLoss + ire.getOtsFiberSpanImpairments().getTotalLoss().doubleValue();
                    if (efirstLoop) {
                        efiberType = ire.getOtsFiberSpanImpairments().getFiberTypeVariety();
                        efirstLoop = false;
                    }
                }
            }
            if (dlength != 0.0 && edlength != 0.0) {
                dlength = Math.max(dlength, edlength);
                dpmd2 = Math.max(dpmd2, edpmd2);
                dtotalLoss = Math.max(dtotalLoss, edtotalLoss);
            } else if (dlength == 0.0 && edlength != 0.0) {
                dlength = edlength;
                dpmd2 = edpmd2;
                dtotalLoss = edtotalLoss;
            }
        }
        if (fiberType == null && efiberType == null) {
            fiberType = "SMF";
        } else if (fiberType == null && efiberType != null) {
            fiberType = efiberType;
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

    /**
     * Calculates typical launch power reference according to fiber type for line physical optical links.
     * @param fiberType     FiberType as coded in OpenROADM enumeration.
     * @return              Double corresponding to typical launch power expressed in dBm.
     */
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

    /**
     * Provides typical Chromatic Dispersion value (ps/km) according to fiber type for line physical optical links.
     * @param fiberType     FiberType as coded in OpenROADM enumeration.
     * @return              Double corresponding to typical Chromatic Dispersion expressed in ps/nm.
     */
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

    /**
     * Evaluates the validity of line physical optical links.
     *   Checks generic parameters partially relying on checkParams for parameters common to optical and OTN links.
     * @return      Boolean set to true if the link is considered as valid, false otherwise.
     */
    private boolean isPhyValid() {
        if ((this.linkId == null) || (this.linkType == null)
        // || (this.oppositeLink == null)
        ) {
            isValid = false;
            LOG.error("PceLink: No Link type or Link Id  or opposite link is available. Link is ignored {}", linkId);
        }
        isValid = checkParams();
        if (this.linkType == OpenroadmLinkType.ROADMTOROADM) {
            if ((this.length == null || this.length == 0.0)
                || (this.sourceOtsSpec == null && this.destOtsSpec == null)) {
                isValid = false;
                LOG.error("PceLink: Error reading Span for OMS link, and no available generic link information."
                    + " Link {} named {} is ignored ", linkId, linkName);
            }
        }
        return isValid;
    }

    /**
     * Evaluates the validity of OTN links.
     *   Checks generic parameters (available bandwidth, connection LayerProtocQualifier), partially relying on
     *   checkParams for parameters common to optical and Otn links.
     * @param serviceType   The serviceType used to check whether the evaluated OTN link (a connection) is valid or not
     *                      for supporting the corresponding service.
     * @return      Boolean set to true if the link is considered as valid, false otherwise.
     */
    private boolean isOtnValid(String serviceType) {
        // TODO: OTN not planned at initialization of T-API functionality
        // Function to be coded at a later step
        if (this.linkType != OpenroadmLinkType.OTNLINK) {
            LOG.error("PceLink: Not an OTN link. Link is ignored {}", linkId);
            return false;
        }
        if (this.availableBandwidth == null || this.availableBandwidth == 0L) {
            LOG.error("PceLink: No bandwidth available for OTN Link, link {}  is ignored ", linkId);
            return false;
        }

        Double neededBW;
        OtnLinkType neededType = null;
        switch (serviceType) {
            case "ODUC2":
                neededBW = 200000.0;
                // Add intermediate rate otn-link-type
                neededType = OtnLinkType.OTUC2;
                break;
            case "ODUC3":
                neededBW = 300000.0;
                // change otn-link-type
                neededType = OtnLinkType.OTUC3;
                break;
            case "ODUC4":
                neededBW = 400000.0;
                neededType = OtnLinkType.OTUC4;
                break;
            case "ODU4":
            case "100GEs":
            case "100GEm":
                neededBW = 100000.0;
                neededType = OtnLinkType.OTU4;
                break;
            // For ODU0 to ODU2, not really needed as created at the same time of DSR service
            // So will never have service create of these types
            case "ODU2":
            case "ODU2e":
            case "10GE":
                neededBW = 10000.0;
                neededType = OtnLinkType.ODU4;
                break;
            case "ODU0":
            case "1GE":
                neededBW = 1250.0;
                neededType = OtnLinkType.ODU4;
                break;
            case "ODU1":
                neededBW = 2500.0;
                neededType = OtnLinkType.ODU4;
                break;
            // For All Ethernet Services consider ODU4 which allows decoupling from line rate, and do not consider
            // intermediate encapsulation (ODTU...).
            // For Switch and mux mode, no setting of neededType which can depend on network interface rate.
            case "800GEm":
            case "800GEs":
            case "800GEt":
                neededBW = 800000.0;
                break;
            case "400GEm":
            case "400GEs":
                neededBW = 400000.0;
                break;
            case "400GEt":
                neededBW = 400000.0;
                neededType = OtnLinkType.ODUC4;
                break;
            default:
                LOG.error("PceLink: isOtnValid Link {} unsupported serviceType {} ", linkId, serviceType);
                return false;
        }
        boolean isOtnValid = false;
        if (TapiMapUtils.LPN_OR_TAPI.get(neededType) != null
                && TapiMapUtils.LPN_OR_TAPI.get(neededType).equals(this.lpq)
                && this.availableBandwidth >= neededBW) {
            //this.availableBandwidth = neededBW;
            isOtnValid = true;
            LOG.debug("PceLink Line 852: Selected Link {} has available bandwidth and is eligible for {} creation ",
                linkId, serviceType);
        }
        return isOtnValid && checkParams();
    }

    /**
     * Check generic parameters common to optical and OTN links to evaluate link validity.
     *   Checked parameters include : linkId, linkType, administrative and operational states, the presence of source
     *   and destination Node/tpIds.
     * @return  Boolean set to true if the link is considered as valid, false otherwise.
     */
    private boolean checkParams() {
        if ((this.linkId == null) || (this.linkType == null)) {
                    // || (this.oppositeLink == null)
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

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#setOppositeLinkUuid()
    */
    @Override
    public void setOppositeLinkUuid(Uuid oppositeLinkId) {
        this.oppositeLink = oppositeLinkId;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#isValid()
    */
    @Override
    public boolean isValid() {
        return this.isValid;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getOppositeLink()
    */
    @Override
    public LinkId getOppositeLink() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getOppositeLink()
    */
    @Override
    public Uuid getOppositeLinkUuid() {
        return oppositeLink;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getAdminStates()
    */
    @Override
    public AdminStates getAdminStates() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getAdministrativeState()
    */
    @Override
    public AdministrativeState getAdministrativeState() {
        return adminStates;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getOperationalState()
    */
    @Override
    public OperationalState getOperationalState() {
        return opState;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getState()
    */
    @Override
    public State getState() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getSourceTP()
    */
    @Override
    public TpId getSourceTP() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getSourceTPUuid()
    */
    @Override
    public Uuid getSourceTPUuid() {
        return sourceTpId;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getDestTP()
    */
    @Override
    public TpId getDestTP() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getDestTPUuid()
    */
    @Override
    public Uuid getDestTPUuid() {
        return destTpId;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getlinkType()
    */
    @Override
    public OpenroadmLinkType getlinkType() {
        return linkType;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getLinkId()
    */
    @Override
    public LinkId getLinkId() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getLinkUuid()
    */
    @Override
    public Uuid getLinkUuid() {
        return linkId;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getSourceId()
    */
    @Override
    public NodeId getSourceId() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getSourceUuid()
    */
    @Override
    public Uuid getSourceUuid() {
        return sourceNodeId;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getDestId()
    */
    @Override
    public NodeId getDestId() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getDestId()
    */
    @Override
    public Uuid getDestUuid() {
        return destNodeId;
    }

    /**
     * Provides the LinkName corresponding to the Link.
     * @return      Uuid of the link.
     */
    public Name getLinkName() {
        return this.linkName;
    }

    /**
     * Provides for OTN link the client Link if the link corresponds to a server.
     * @return String corresponding to the client link/connection.
     */
    public String getClient() {
        return client;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getLength()
    */
    @Override
    public Double getLength() {
        return length;
    }

    /**
     * Allows setting the client link (a TOP-connection) for OTN links.
     * @param client String corresponding to client link name.
     */
    public void setClient(String client) {
        this.client = client;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getLatency()
    */
    @Override
    public Double getLatency() {
        if (latency == null) {
            return 0.0;
        }
        return latency.doubleValue();
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getAvailableBandwidth()
    */
    @Override
    public Long getAvailableBandwidth() {
        return availableBandwidth.longValue();
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getUsedBandwidth()
    */
    @Override
    public Long getUsedBandwidth() {
        return usedBandwidth.longValue();
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getsourceNetworkSupNodeId()
    */
    @Override
    public String getsourceNetworkSupNodeId() {
        return sourceNetworkSupNodeId;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getdestNetworkSupNodeId()
    */
    @Override
    public String getdestNetworkSupNodeId() {
        return destNetworkSupNodeId;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getsrlgList()
    */
    @Override
    public List<Long> getsrlgList() {
        List<Long> convertedList = new ArrayList<>();
        boolean error = false;
        for (String entry : srlgList) {
            if(Long.valueOf(entry) != null) {
                convertedList.add(Long.valueOf(entry));
            } else {
                error = true;
                break;
            }
        }
        return (error) ? null : convertedList;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getspanLoss()
    */
    @Override
    public Double getspanLoss() {
        return spanLoss;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getcd()()
    */
    @Override
    public Double getcd() {
        return cd;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getpmd2()
    */
    @Override
    public Double getpmd2() {
        return pmd2;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceLink#getpowerCorrection()
    */
    @Override
    public Double getpowerCorrection() {
        return powerCorrection;
    }

    /**
     * Provides Layer Protocol Name of the link.
     * @return  LayerProtocolName of the link (either ETH, DSR, PHOTONIC_MEDIA or DIGITAL_OTN).
     */
    public LayerProtocolName getLpn() {
        return lpn;
    }

    /**
     * Provides implementation of .toString for Links.
     */
    @Override
    public String toString() {
        return "PceLink type=" + linkType + " ID=" + linkId.getValue() + " latency=" + latency + " Name=" + linkName;
    }
}
