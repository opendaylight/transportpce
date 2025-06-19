/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.pce.node.mccapabilities.McCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ClientNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ClientNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ConnectionEndPoint2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ConnectionEndPoint3;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIERMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULECANNOTFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMUSTFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SinkProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SourceProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TapiOpticalNode {
    private static final Logger LOG = LoggerFactory.getLogger(TapiOpticalNode.class);

    private boolean valid;
    private Node node;
    private Uuid nodeUuid;
    private String deviceNodeId;
    private Name nodeName;
    private NodeTypes commonNodeType;
    private AdministrativeState adminState;
    private OperationalState operationalState;
    private String serviceType;
    private Uuid aaNodeId;
    private Uuid zzNodeId;
    private Uuid aaPortId;
    private Uuid zzPortId;
    private ServiceFormat servFormat;
    private String version;
    private McCapability mcCapability;

    private List<BasePceNep> allOtsNep = new ArrayList<>();
    private Map<Uuid, BasePceNep> mmSrgOtsNep = new HashMap<>();
    private Map<Uuid, BasePceNep> mmDegOtsNep = new HashMap<>();
    private List<BasePceNep> degOmsNep = new ArrayList<>();
    private List<BasePceNep> nwOtsNep = new ArrayList<>();
    private List<BasePceNep> clientDsrNep = new ArrayList<>();
    private List<BasePceNep> omsLcpList = new ArrayList<>();
    private List<BasePceNep> otsLcpList = new ArrayList<>();
    private List<BasePceNep> oduLcpList = new ArrayList<>();
    private List<BasePceNep> otuLcpList = new ArrayList<>();
    private List<BasePceNep> otsLcpNWList = new ArrayList<>();
    private Map<Uuid,Uuid> bindingVNepToSubnodeMap = new HashMap<>();
    private List<Uuid> invalidNwNepList = new ArrayList<>();
    private Map<Uuid, Uuid> dsrNepWithParentOdu = new HashMap<>();
    private Map<Uuid,IntLinkObj> internalLinkMap = new HashMap<>();
    private Map<Uuid, PceTapiOpticalNode> pceNodeMap = new HashMap<>();
    private PceTapiOpticalNode pceTapiOptNodeXpdr;
    private PceTapiOtnNode pceTapiOtnNodeXpdr;
    private Map<Uuid, PceTapiLink> pceInternalLinkMap = new HashMap<>();
    private static final String ROADMNODENAME = "roadm node name";
    private static final String ROADMNODETYPE = "ROADM";
    private static final String XPDRNODENAME = "otsi node name";
    private static final String NODETYPE = "Node Type";
    private boolean isPhotonic = false;
    private static final List<String> SERVICE_TYPE_PHT_LIST = List.of(
        StringConstants.SERVICE_TYPE_OTU4,
        StringConstants.SERVICE_TYPE_OTUC4,
        StringConstants.SERVICE_TYPE_OTUC3,
        StringConstants.SERVICE_TYPE_OTUC2,
        StringConstants.SERVICE_TYPE_100GE_T);


    public enum DirectionType { SINK, SOURCE, BIDIRECTIONAL, UNIDIRECTIONAL, UNDEFINED }

    public enum TpType { PP, CP, CTP, TTP, NW, CLIENT }

    /*
     * Intermediate class used to process T-API NEP, before instantiating one (for TSP, AMPs) or several (ROADMs)
     * TapiPceOpticalNode. Indeed, T-API does not consider disaggregation of ROADM nodes, and does not differentiate
     * ADD-DROP (SRGs) from DEGREES.
     * In this class we perform preliminary operations required to split the ADD-DROP (SRG) part from the Degree part
     * in ROADMs.
     *  We create virtual TPs corresponding to CPs & CTPs and create virtual links between them from T-API
     * node-rule-groups and inter-rule-groups.
     *
     * Focus on NEP of Photonic Layer/photonic media (present in ROADMs and Xponders)
     * They all have an available and occupied spectrum
     * ROADMs :
     * Identify NEP associated with PP using supported/available CEP qualifier instances
     *      Create a list of PP-NEP for which CEP PhLayer/photonicMedia have no OMS, an OTS and
     *      are available (op-state=InService/no MC on PhLayer/MC)
     * Identify NEP associated with DEGR-TTP using supported/available CEP qualifier instances
     *      Create a list of TTP-NEP for which PhLayer/photonicMedia have an OMS/OTS and
     *      are available (op-state=InService/OMS/OTS)
     * XPonders :
     * Identify NEP associated with Network-port using supported/available CEP qualifier instances
     *      Create a list of Network-NEP for which CEP PhLayer/photonicMedia have no OMS,
     *      an OTS, which are available (no NMC/OTSI, op-state=InService on CEP PhLayer/MC) and for which we have
     *      a potential client port (using node-rule-group)
     */

    /**
     Intermediate class used to process T-API NEP, before instantiating one or several (for ROADMs) TapiPceOpticalNodes.
     * @param serviceType Type of service is used to determine which type (Optical/Otn) of PceNode is created and which
     *                    BasePceNep NEPs shall be created and validated
     * @param portMapping Pormapping of Nodes discovered through NETCONF SBI (only concerns Alien Transponders)
     * @param node        Node as present in the T-API topology
     * @param version     Node version passed to PceTapiOpticalNode
     * @param slotWidthGranularity     Granularity of the slot width to be used when enforcing new Grid implementation
     * @param centralFreqGranularity   Grid resolution to be used when enforcing new Grid implementation
     * @param anodeId     A Node Id as provided in service creation/reroute... request exercised through NBI (mandatory)
     * @param znodeId     Z Node Id as provided in service creation/reroute... request exercised through NBI (mandatory)
     * @param aportId     A Port Id as provided in service creation/reroute... request exercised through NBI (optional)
     * @param zportId     A Port Id as provided in service creation/reroute... request exercised through NBI (optional)
     * @param serviceFormat            OpenROADM Service Format (OCH, OTU...)
     * @param mcCapability             MediaChannel capability to be used when enforcing new Grid implementation
     */
    public TapiOpticalNode(String serviceType, PortMapping portMapping, Node node,
        String version, BigDecimal slotWidthGranularity, BigDecimal centralFreqGranularity,
        Uuid anodeId, Uuid znodeId, Uuid aportId, Uuid zportId, ServiceFormat serviceFormat,
        McCapability mcCapability) {
        // For T-API topology, all nodes in the OLS do not have any portmapping which may be
        // available only for OpenConfig Transponder : try to avoid relying on PortMapping
        if (serviceType == null
            || node == null
            || slotWidthGranularity == null) {
            LOG.error("TapiOpticalNode: one of parameters is not populated : slot width granularity  {} or"
                + "service type {} or node {}", slotWidthGranularity, serviceType,
                node == null ? "NULL" : node.getName());
            this.valid = false;
        } else if (!(OperationalState.ENABLED.equals(node.getOperationalState()))) {
            LOG.error("TapiOpticalNode: Node {} ignored since its operational state {} differs from ENABLED",
                node.getName().toString(), node.getOperationalState().toString());
            LOG.error("TapiOpticalNode Line 170: Operational state no set to ENABLED for {}", node.getName());
            this.valid = false;
        } else {
            this.valid = true;
            this.serviceType = serviceType;
            if (SERVICE_TYPE_PHT_LIST.contains(serviceType)) {
                this.isPhotonic = true;
            }
            this.node = node;
            if (ROADMNODETYPE.equals(node.getName().entrySet().stream()
                    .filter(name -> NODETYPE.equals(name.getKey().getValueName()))
                    .findAny().orElseThrow().getValue().getValue())) {
                this.nodeName = node.getName().entrySet().stream()
                    .filter(name -> ROADMNODENAME.equals(name.getKey().getValueName()))
                    .findAny().orElseThrow().getValue();
            } else {
                this.nodeName = node.getName().entrySet().stream()
                    .filter(name -> XPDRNODENAME.equals(name.getKey().getValueName()))
                    .findAny().orElseThrow().getValue();
            }
            LOG.info("TONLine184 NodeName = {}", nodeName);
            this.deviceNodeId = node.getUuid().getValue();
            this.nodeUuid = node.getUuid();
            this.version = version;
            this.adminState = node.getAdministrativeState();
            this.operationalState = node.getOperationalState();
            this.aaNodeId = anodeId;
            this.zzNodeId = znodeId;
            this.aaPortId = aportId;
            this.zzPortId = zportId;
            this.servFormat = serviceFormat;
            this.mcCapability = mcCapability;
            // First step is to qualify the node, determining its type
            LOG.info("TapiOpticalNode : Node {} admin state is {}, operational state is {}, Valid = {}",
                node.getName(), adminState, operationalState, valid);
        }
    }

    /**
     * Second method called to initialize the TapiOptical node and create corresponding PceOptical and PceOtn Nodes.
     * Called after the node has been qualified to initiate the PceNode Tps, build BasePceNep with required attributes,
     * and disaggregate ROADM nodes, splitting them into several Degrees and SRGs.
     */
    public void initialize() {
        qualifyNode();
        switch (commonNodeType) {
            case Rdm:
                initRoadmTps();
                buildVirtualCpsAndCtps();
                buildDefaultVirtualCtps();
                buildInternalLinksMap();
                this.pceNodeMap = splitDegNodes();
                this.pceNodeMap.putAll(splitSrgNodes());
                createLinksFromIlMap();
                break;
            case Xpdr:
                // for Xponder, PceNode are created and handled through validateAZxponder as for the OpenROADM PCE
                validateAZxponder();
                break;
            case Ila:
                initIlaTps();
                Map<Uuid, Name> ilaNodeId = new HashMap<>();
                ilaNodeId.put(nodeUuid, nodeName);
                // In case of ILA as this type of node is not defined at that time in OpenROADM, we use ORNodeType ROADM
                this.pceNodeMap.put(nodeUuid, new PceTapiOpticalNode(serviceType, node, OpenroadmNodeType.ROADM,
                    version, allOtsNep, ilaNodeId, deviceNodeId, mcCapability));
                break;
            default:
                break;
        }
    }

    /**
     * First method called to determine whether the node is a Xponder, an ILA or a ROADM.
     * The qualification is based on the LayerProtocolNames supported by the device, and LayerProtocolQualifiers
     * supported by the Node's NEPs : Only Xponders support DSR layer, ROADM includes OMS NEPs, whereas ILA only
     * include OTS NEPs (no OMS). Sets commonNodeType to Rdm/Ila/Xpdr.
     */
    private void qualifyNode() {
        LOG.debug("qualifyNode: calculates node type of {} from CEP characteristics", this.nodeUuid);
        if (!this.node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)) {
            LOG.debug("qualifyNode: Node {} is not assimilated to a Photonic Media Node", this.nodeUuid);
            this.valid = false;
            return;
        }
        boolean otsFound = false;
        boolean omsFound = false;
        if (this.node.getLayerProtocolName().contains(LayerProtocolName.DSR)
            || this.node.getLayerProtocolName().contains(LayerProtocolName.ODU)
            || this.node.getLayerProtocolName().contains(LayerProtocolName.DIGITALOTN)
            || this.node.getLayerProtocolName().contains(LayerProtocolName.ETH)) {
            this.commonNodeType = NodeTypes.Xpdr;
        } else {
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNepList = this.node.getOwnedNodeEdgePoint();
            for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNep : ownedNepList.entrySet()) {
                if (ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                    .filter(sclpqi -> PHOTONICLAYERQUALIFIEROMS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                    .findAny() != null) {
                    omsFound = true;
                    break;
                } else if (ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                    .filter(sclpqi -> PHOTONICLAYERQUALIFIEROMS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                    .findAny() != null) {
                    otsFound = true;
                }
            }
            if (omsFound) {
                this.commonNodeType = NodeTypes.Rdm;
            } else if (otsFound) {
                this.commonNodeType = NodeTypes.Ila;
            } else {
                LOG.debug("qualifyNode: Unidentified type for Node {} ", this.nodeUuid);
                this.valid = false;
                return;
            }
        }
        this.valid = true;
        LOG.debug("identifyNodeType: node type of {} is {}", this.nodeUuid, this.commonNodeType.getName());
        return;
    }

    /**
     * Populates BasePceNep Dictionaries associated with ROADM nodes for OTS and OMS tps and set their main attributes.
     * -Populate mmSrgOtsNep with BasePceNep (NEPs) associated to Add/Drop ("SRG") PPs
     * -Populate mmDegOtsNep with BasePceNep (NEPs) associated to Degree TTPs
     * -Populate mmDegOmsNep with BasePceNep (NEPs) associated to Degree TTPs
     * -Populate allOtsNep with BasePceNep (NEPs) associated to both Degree and SRG OTS tps
     * -Populate otsLcpList with BasePceNep (CEPs) associated to Degree TTPs which are used in a second step
     *  to complement attributes settings for OMS and OTS NEPs of Degrees
     */
    private void initRoadmTps() {
        // T-API ROADMs are not disaggregated. A ROADM Node will include PPs and TTPs
        if (!this.valid) {
            return;
        }
        // Scan OwnedNEP from the Photonic layer ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances()
        // provides info on OMS/OTS/MC presence
        // NEP with OTS, no OMS are PPS --> if InService and no MC (occupancy) --> put it in srgOtsNep
        // NEP with OTS and OMS are TTPs --> In service --> Put them in mmDegOtsNep
        // Relies on checkOtsNepAvailable
        Map<DirectionType, OpenroadmTpType> direction;
        LOG.debug("TONline328: initRoadmIlaTps: getting tps from ROADM node {}", this.nodeName);
        // for each of the photonic OwnedNEP which Operational state is enable
        // and spectrum is not fully used
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNepList = this.node.getOwnedNodeEdgePoint();
        for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNep : ownedNepList.entrySet()) {
            if (!checkAvailableSpectrum(ownedNep.getValue().getUuid())) {
                LOG.debug("TONline334: initRoadmIlaTps: the Onep {} is not identified as available",
                    ownedNep.getValue().getName().entrySet().iterator().next().getValue().getValue());
            }
            if (LayerProtocolName.PHOTONICMEDIA.equals(ownedNep.getValue().getLayerProtocolName())
                && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())
                && checkAvailableSpectrum(ownedNep.getValue().getUuid())) {

                if (!ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                        .filter(sclpqi -> PHOTONICLAYERQUALIFIEROTS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                        .findAny().isEmpty()) {
                    var otsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                    if (ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class) == null
                            || ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList() == null) {
                        otsNep.setOperationalState(OperationalState.ENABLED);
                        otsNep.setAdminState(AdministrativeState.UNLOCKED);
                        otsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                        otsNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                        otsNep.setLpq(PHOTONICLAYERQUALIFIEROTS.VALUE);
                        allOtsNep.add(otsNep);
                    } else {
                        for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep.getValue()
                                .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                                .entrySet()) {
                            var otsCep = new BasePceNep(cep.getValue().getUuid(), cep.getValue().getName());
                            otsCep.setParentNep(ownedNep.getKey().getUuid());
                            otsCep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            otsNep.setCepOtsUuid(cep.getValue().getUuid());
                            otsNep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            otsNep.setOperationalState(OperationalState.ENABLED);
                            otsNep.setAdminState(AdministrativeState.UNLOCKED);
                            otsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                            otsNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                            otsNep.setLpq(PHOTONICLAYERQUALIFIEROTS.VALUE);
                            if (cep.getValue().augmentation(ConnectionEndPoint2.class) != null
                                    && cep.getValue().augmentation(ConnectionEndPoint2.class)
                                    .getOtsMediaConnectionEndPointSpec() != null) {
                                otsNep.setCepOtsSpec(cep.getValue().augmentation(ConnectionEndPoint2.class)
                                    .getOtsMediaConnectionEndPointSpec());
                                otsCep.setCepOtsSpec(cep.getValue().augmentation(ConnectionEndPoint2.class)
                                    .getOtsMediaConnectionEndPointSpec());
                                otsLcpList.add(otsCep);
                                LOG.debug("TONline377: initRoadmIlaTps: OTS CepSpec {} added to otsNep {} in AllOtsNep",
                                    cep.getValue().augmentation(ConnectionEndPoint2.class)
                                        .getOtsMediaConnectionEndPointSpec(), otsNep.getName());
                                // TODO: check if spectrum shall be populated here
                                //Goes out of the loop as soon it has found a OtsMediaConnectionEndPointSpec
                                break;
                            }
                            otsLcpList.add(otsCep);
                            LOG.debug("TONline385: initRoadmIlaTps: OTS Cep added to otsLcpList  {}",
                                otsCep.getName());
                        }
                        allOtsNep.add(otsNep);
                    }
                    LOG.debug("TONline390: initRoadmIlaTps: OTS nep added to AllOTSNep  {}", otsNep.getName());
                }
                // If the NEP is an OMS NEP, scans the list of CEP, Adds to oMsLcpList each CEP which owns an omsCEPspec
                // and fills degOmsNep with all information it retrieves from the CEP
                if (!ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                    .filter(sclpqi -> PHOTONICLAYERQUALIFIEROMS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                    .findAny().isEmpty()) {

                    LOG.debug("TONline398: initRoadmIlaTps: OMS NEP {} has been found", ownedNep.getValue().getName());
                    var omsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                    if (ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class) == null
                            || ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList() == null) {
                        omsNep.setOperationalState(OperationalState.ENABLED);
                        omsNep.setAdminState(AdministrativeState.UNLOCKED);
                        omsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                        omsNep.setTpType(OpenroadmTpType.DEGREETXRXTTP);
                        omsNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                        omsNep.setLpq(PHOTONICLAYERQUALIFIEROMS.VALUE);
                        direction = calculateDirection(ownedNep.getValue(), null, TpType.TTP);
                        omsNep.setDirection(direction.keySet().iterator().next());
                        omsNep.setTpType(direction.values().iterator().next());
                        direction.clear();
                        degOmsNep.add(omsNep);
                        LOG.debug("TONline413: initRoadmIlaTps: OMS NEP {} NO augment", ownedNep.getValue().getName());
                    } else {
                        LOG.debug("TONline415: initRoadmIlaTps: OMS NEP {} has augment", ownedNep.getValue().getName());
                        for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep2 : ownedNep.getValue()
                                .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                                .entrySet()) {
                            var omsCep = new BasePceNep(cep2.getValue().getUuid(), cep2.getValue().getName());
                            //omsCep.setParentNep(cep2.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid());
                            omsCep.setParentNep(ownedNep.getKey().getUuid());
                            omsCep.setClientNep(cep2.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            omsLcpList.add(omsCep);
                            LOG.debug("TONline425: initRoadmIlaTps: OMS Cep added to omsLcpList  {}",
                                omsCep.getName());
                            omsNep.setCepOmsUuid(cep2.getValue().getUuid());
                            omsNep.setClientNep(cep2.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            omsNep.setOperationalState(OperationalState.ENABLED);
                            omsNep.setAdminState(AdministrativeState.UNLOCKED);
                            omsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                            omsNep.setTpType(OpenroadmTpType.DEGREETXRXTTP);
                            omsNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                            omsNep.setLpq(PHOTONICLAYERQUALIFIEROMS.VALUE);
                            direction = calculateDirection(ownedNep.getValue(), null, TpType.TTP);
                            omsNep.setDirection(direction.keySet().iterator().next());
                            omsNep.setTpType(direction.values().iterator().next());
                            direction.clear();
                            if (cep2.getValue().augmentation(ConnectionEndPoint3.class) != null
                                    && cep2.getValue().augmentation(ConnectionEndPoint3.class)
                                    .getOmsConnectionEndPointSpec() != null) {
                                omsNep.setCepOmsSpec(cep2.getValue().augmentation(ConnectionEndPoint3.class)
                                    .getOmsConnectionEndPointSpec());
                                //Goes out of the loop as soon it has found a OtsMediaConnectionEndPointSpec
                                break;
                            }
                        }
                        degOmsNep.add(omsNep);
                    }
                    LOG.debug("TONline452: initRoadmIlaTps: OMS nep added to degOMSNep  {}", omsNep.getName());
                }
            }
        }
        degOmsNep.stream().distinct().collect(Collectors.toList());
        // Loop to Fill mmSrgOtsNep and mmDegOtsNep from allOtsNep, relying on information (parent NEP) present in
        // otsLcpList, and set direction, tpType and spectrum use
        LOG.debug("TONline459: Node {} initRoadmIlaTps: Purged degOmsNepList Nep {}", nodeName,
            degOmsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("TONline461: Node {} initRoadmIlaTps: degOmsNepUuidList Nep {}", nodeName,
            degOmsNep.stream().map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        LOG.debug("TONline463: Node {} initRoadmIlaTps: OtsCepList ClientNeps {}", nodeName,
            otsLcpList.stream().map(BasePceNep::getClientNep).collect(Collectors.toList()));
        boolean isDegreeNep;
        otsLcpList.stream().distinct().collect(Collectors.toList());
        for (BasePceNep otsCep : otsLcpList) {
            isDegreeNep = false;
            LOG.debug("TONline469: Node {} initRoadmIlaTps: scan OtsCepList : Ots Cep {}", nodeName, otsCep.getName());
            LOG.debug("TONline470: initRoadmIlaTps: scan OtsCepList : Ots CepClientNep {}", otsCep.getClientNep());
            for (BasePceNep omsNep : degOmsNep) {
                if (omsNep.getNepCepUuid().equals(otsCep.getClientNep())) {
                    // The OTS NEP is a degree NEP
                    LOG.debug("TONline474: initRoadmIlaTps: identifiedOMS nep {} as client of Ots Cep ",
                        omsNep.getName());
                    omsNep.setParentNep(otsCep.getParentNep());
                    BasePceNep otsNep = allOtsNep.stream().filter(bpn -> otsCep.getParentNep()
                        .equals(bpn.getNepCepUuid())).findFirst().orElseThrow();
                    LOG.debug("TONline479: initRoadmIlaTps: OTS nep {} in mmDegOTsNep has admin state {} ",
                        otsNep.getName(), otsNep.getAdminState());
                    direction = calculateDirection(
                        ownedNepList.entrySet().stream().filter(onep -> otsNep.getNepCepUuid()
                            .equals(onep.getValue().getUuid())).findFirst().orElseThrow()
                            .getValue(), null, TpType.TTP);
                    otsNep.setDirection(direction.keySet().iterator().next());
                    otsNep.setTpType(direction.values().iterator().next());
                    otsNep.setFrequencyBitset(buildBitsetFromSpectrum(otsNep.getNepCepUuid()));
                    otsNep.setCepOtsSpec(otsCep.getCepOtsSpec());
                    LOG.debug("TONline489: initRoadmIlaTps: OTS CepSpec {} added to otsNep {} in AllOtsNep",
                        otsCep.getCepOtsSpec(), otsNep.getName());
                    mmDegOtsNep.put(otsNep.getNepCepUuid(), otsNep);
                    //degOtsNep.add(otsNep);
                    LOG.debug("TONline493: initRoadmIlaTps: OTS nep from allOTsNep added to mmDegOTSNep  {}",
                        otsNep.getName());
                    isDegreeNep = true;
                    break;
                }
            }
            LOG.debug("TONline499: Node {} initRoadmIlaTps: no identifiedOMS nep as client of Ots Cep {} ",
                nodeName, otsCep.getName());
            if (!isDegreeNep) {
                BasePceNep otsNep = allOtsNep.stream().filter(bpn -> otsCep.getParentNep().equals(bpn.getNepCepUuid()))
                    .findFirst().orElseThrow();
                if (!checkPPOtsNepAvailable(otsNep.getNepCepUuid())) {
                    continue;
                }
                direction = calculateDirection(
                    ownedNepList.entrySet().stream().filter(onep -> otsNep.getNepCepUuid().equals(onep.getValue()
                        .getUuid())).findFirst().orElseThrow().getValue(), null, TpType.PP);
                otsNep.setDirection(direction.keySet().iterator().next());
                otsNep.setTpType(direction.values().iterator().next());
                otsNep.setFrequencyBitset(buildBitsetFromSpectrum(otsNep.getNepCepUuid()));
                mmSrgOtsNep.put(otsNep.getNepCepUuid(), otsNep);
                LOG.debug("TONline447: initRoadmIlaTps: OTS nep from allOTsNep added to mmSrgOTSNep  {}",
                    otsNep.getName());
            }
        }
        for (BasePceNep bpn : allOtsNep) {
            if (!mmDegOtsNep.containsKey(bpn.getNepCepUuid()) && !mmSrgOtsNep.containsKey(bpn.getNepCepUuid())
                    && checkPPOtsNepAvailable(bpn.getNepCepUuid())) {
                direction = calculateDirection(
                    ownedNepList.entrySet().stream().filter(onep -> bpn.getNepCepUuid().equals(onep.getValue()
                        .getUuid())).findFirst().orElseThrow().getValue(), null, TpType.PP);
                bpn.setDirection(direction.keySet().iterator().next());
                bpn.setTpType(direction.values().iterator().next());
                bpn.setFrequencyBitset(buildBitsetFromSpectrum(bpn.getNepCepUuid()));
                mmSrgOtsNep.put(bpn.getNepCepUuid(), bpn);
                LOG.debug("TONline461: Node {} initRoadmIlaTps: OTS nep from allOTsNep added to mmSrgOTSNep  {}",
                    nodeName, bpn.getName());
            }
        }
        LOG.debug("TONline532: initRoadmIlaTps: mmDegOTSNEP  {}",
            mmDegOtsNep.values().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("TONline534: Node {} initRoadmIlaTps: mmDegOMSNEP  {}", nodeName,
            degOmsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("TONline536: Node {} initRoadmIlaTps: mmSrgOTSNEP  {}", nodeName,
            mmSrgOtsNep.values().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("TONline538: Node {} initRoadmIlaTps: allOtsNEP  {}", nodeName,
            allOtsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
    }

    /**
     * Populates BasePceNep Dictionaries associated with ILA nodes for OTS tps and set their main attributes.
     */
    private void initIlaTps() {
        if (!this.valid) {
            return;
        }
        // Scan Owned NEP from the Photonic.
        // Provides info on OTS presence.
        // Fill AllOtsNep with OTS Nep (Operational state ENABLED) and OtsLcpList with OtsCep
        // Relies on checkOtsNepAvailable
        LOG.debug("initRoadmIlaTps: getting tps from ILA node {}", this.nodeUuid);
        // for each of the photonic OwnedNEP which Operational state is enable and spectrum is not fully used
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNepList = this.node.getOwnedNodeEdgePoint();
        for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNep : ownedNepList.entrySet()) {
            if (LayerProtocolName.PHOTONICMEDIA.equals(ownedNep.getValue().getLayerProtocolName())
                && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())
                && checkAvailableSpectrum(ownedNep.getValue().getUuid())) {
                // If the NEP is an OTS NEP, scans the list of CEP, Adds to
                // otsLcpList each CEP which owns an otsCEPspec
                // and fills allOtsNep with all information it retrieves from
                // the CEP
                if (ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                    .filter(sclpqi -> PHOTONICLAYERQUALIFIEROTS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                    .findAny() != null) {
                    for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep.getValue()
                        .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint().entrySet()) {
                        if (cep.getValue().augmentation(ConnectionEndPoint2.class)
                            .getOtsMediaConnectionEndPointSpec() != null) {
                            var otsCep = new BasePceNep(cep.getValue().getUuid(), cep.getValue().getName());
                            otsCep.setParentNep(cep.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid());
                            otsCep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            otsLcpList.add(otsCep);
                            var otsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                            otsNep.setCepOtsUuid(cep.getValue().getUuid());
                            otsNep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            otsNep.setOperationalState(OperationalState.ENABLED);
                            otsNep.setAdminState(AdministrativeState.UNLOCKED);
                            otsNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                            otsNep.setLpq(PHOTONICLAYERQUALIFIEROTS.VALUE);
                            otsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                            otsNep.setCepOtsSpec(cep.getValue().augmentation(ConnectionEndPoint2.class)
                                .getOtsMediaConnectionEndPointSpec());
                            // check if spectrum shall be populated here
                            allOtsNep.add(otsNep);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * For Degrees, create default virtual NEPs corresponding to Ctps.
     *  To each OTS NEP can be associated a virtual NEPs corresponding to Ctp. This method allows creating all
     *  corresponding CTPs whether they have already been created from node rule group in buildVirtualCpsAndCtps()
     *  or not. Indeed, depending on the implementation followed by the manufacturers, RDM internal connectivity may
     *  follow different approaches, depending on how NodeRuleGroup and InterRuleGroup are handled.
     */
    private void buildDefaultVirtualCtps() {
        if (mmDegOtsNep == null) {
            return;
        }

        LOG.debug("TONLine609 mmDegOtsNep {}", mmDegOtsNep.entrySet().stream()
            .map(nep -> nep.getValue().getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        for (Map.Entry<Uuid, BasePceNep> otsNep : mmDegOtsNep.entrySet()) {
            // For each Degree OTN NEP, if a virtual NEP was not already created, creates a virtual node corresponding
            // to the degree CTP
            if ((otsNep.getValue().getVirtualNep() == null || otsNep.getValue().getVirtualNep().isEmpty())
                && otsNep.getValue().getName().entrySet().iterator().next().getValue().getValue().contains("TTP")) {
                LOG.debug("TONLine617 input for createNodeOrVnepId {}",
                    otsNep.getValue().getName().entrySet().iterator().next().getValue().getValue().split("\\-TTP")[0]);
                Map<Uuid, Name> vnepId = createNodeOrVnepId(
                    otsNep.getValue().getName().entrySet().iterator().next().getValue().getValue().split("\\-TTP")[0],
                    "DEG", false);
                // Add in mmDegOtsNep Map the virtual node associated to the block
                otsNep.getValue().setVirtualNep(vnepId);
                Map<NameKey, Name> vnepNameMap = new HashMap<>();
                vnepNameMap.put(vnepId.entrySet().iterator().next().getValue().key(),
                    vnepId.entrySet().iterator().next().getValue());
                BasePceNep virtualNep = new BasePceNep(vnepId.entrySet().iterator().next().getKey(), vnepNameMap);
                virtualNep.setDirection(DirectionType.BIDIRECTIONAL);
                virtualNep.setFrequencyBitset(otsNep.getValue().getFrequenciesBitSet());
                virtualNep.setOperationalState(OperationalState.ENABLED);
                virtualNep.setAdminState(AdministrativeState.UNLOCKED);
                virtualNep.setTpType(OpenroadmTpType.DEGREETXRXCTP);
                virtualNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                virtualNep.setLpq(PHOTONICLAYERQUALIFIERMC.VALUE);
                virtualNep.setParentNep(otsNep.getKey());
                mmDegOtsNep.put(virtualNep.getNepCepUuid(), virtualNep);
            }
        }
    }

    /**
     * For ROADMs' Degrees and Add/drop, create virtual NEPs corresponding to Cps (Add/drop) and Ctps (Degrees).
     *  This method is used in the process of disaggregating ROADMs to separate Add/Drops ("SRG") from Degrees.
     *  The purpose is to recreate intermediate points that may have specific constraints (Blocking architectures)
     *  and corresponds to intermediate tps in ROADMs. Rather than creating a full or partial mesh between ROADMs
     *  PPs and TTPs, links/edges will only be created between the virtual NEPs associated with Cps and Ctps.
     *  The creation of these virtual NEPs that are not modeled in T-API, is done looking at NodeRuleGroups
     *  and InterRuleGroups that define internal connectivity of the T-API ROADMs.
     *  This being done, T-API ROADMs will be represented in the graph by vertices with the same characteristics as
     *  the ones used for OpenROADM.
     */
    private void buildVirtualCpsAndCtps() {
        Map<NodeRuleGroupKey, NodeRuleGroup> nrgList = this.node.getNodeRuleGroup();
        // Make from list of OTS NEP a Map which simplifies its management
        for (Map.Entry<NodeRuleGroupKey, NodeRuleGroup> nrg : nrgList.entrySet()) {
            // For each NodeRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
            // and if this is the case store its key in fwRuleKeyList
            List<RuleKey> fwdRuleKeyList =
                nrg.getValue().getRule().entrySet().stream()
                    .filter(rule -> rule.getValue().getRuleType().contains(RuleType.FORWARDING))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            boolean fwdBlock = false;
            boolean blocking = false;
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Node Rule Group (NRG) does not contain any forwarding rule, we go to next NRG
                continue;
            } else {
                // If we have one or several forwarding rule(s)
                for (RuleKey rk : fwdRuleKeyList) {
                    if (FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE
                        .equals(nrg.getValue().getRule().get(rk).getForwardingRule())
                        || FORWARDINGRULEMUSTFORWARDACROSSGROUP.VALUE
                            .equals(nrg.getValue().getRule().get(rk).getForwardingRule())) {
                        // We are in Contention-less Add/drop Block or a degree, or in a node where forwarding condition
                        // is defined across all NEPs
                        fwdBlock = true;
                    } else if (FORWARDINGRULECANNOTFORWARDACROSSGROUP.VALUE
                        .equals(nrg.getValue().getRule().get(rk).getForwardingRule())) {
                        // We are in a regular Add/drop Block with contention
                        fwdBlock = true;
                        blocking = true;
                        LOG.debug("TONLine600 blocking true, found non forwarding condition");
                    }
                }
            }
            LOG.debug("TONLine687 Evaluating NRG {}, fwblock = {}, blocking = {}",
                nrg.getKey(), fwdBlock, blocking);
            // Handle now in the same way a non blocking SRG (Contentionless)
            // and a traditional blocking SRG
            if (fwdBlock) {
                // In case we have a forwarding condition defined, (blocking for Regular blocking add/drop block or
                // non blocking for ContentionLess Add/Drop), we check if it applies to OTS NEPs (finding them
                // in mmSrgOtsNep or mmDegOtsNep Map) and store keys of considered NEP in ots<Srg/Deg>NepKeyList
                List<NodeEdgePointKey> otsSrgNepKeyList = nrg.getValue().getNodeEdgePoint().entrySet().stream()
                    .filter(nep -> mmSrgOtsNep.containsKey(nep.getValue().getNodeEdgePointUuid()))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
                LOG.debug("TONLine615 otsSrgNepKeyList = {}", otsSrgNepKeyList);
                List<NodeEdgePointKey> otsDegNepKeyList = nrg.getValue().getNodeEdgePoint().entrySet().stream()
                    .filter(nep -> mmDegOtsNep.containsKey(nep.getValue().getNodeEdgePointUuid()))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
                if ((otsSrgNepKeyList == null || otsSrgNepKeyList.isEmpty())
                    && (otsDegNepKeyList == null || otsDegNepKeyList.isEmpty())) {
                    // the forwarding rule does neither apply to SRG OTS NEPs, nor to DEG OTS NEPs
                    continue;
                }
                // In the other case,the forwarding rule applies to OTS NEPs of either SRG or Degree
                // Create first an Id for one Virtual NEP associated to all NEPs of the Block
                if (!(otsSrgNepKeyList == null || otsSrgNepKeyList.isEmpty())) {
                    Map<Uuid, Name> vnepId1;
                    //vnepId1 = createNodeOrVnepId(String.valueOf(virtualNepId), "-SRG", false);
                    String otsNepName = mmSrgOtsNep.entrySet().stream()
                        .filter(bpn -> otsSrgNepKeyList.stream()
                                .map(NodeEdgePointKey::getNodeEdgePointUuid).collect(Collectors.toList())
                                .contains(bpn.getKey()))
                        .findAny().orElseThrow().getValue()
                            .getName().entrySet().iterator().next().getValue().getValue();
                    vnepId1 = createNodeOrVnepId(otsNepName.split("\\-PP")[0], "SRG", false);
                    if (blocking) {
                        LOG.debug("TONLine638 blocking true, entering creation of Vnep");
                        Map<NameKey, Name> vnepNameMap1 = new HashMap<>();
                        vnepNameMap1.put(vnepId1.entrySet().iterator().next().getValue().key(),
                            vnepId1.entrySet().iterator().next().getValue());
                        BasePceNep virtualNep = new BasePceNep(vnepId1.entrySet().iterator().next().getKey(),
                            vnepNameMap1);
                        virtualNep.setDirection(DirectionType.BIDIRECTIONAL);
                        virtualNep.setOperationalState(OperationalState.ENABLED);
                        virtualNep.setAdminState(AdministrativeState.UNLOCKED);
                        virtualNep.setTpType(OpenroadmTpType.SRGTXRXCP);
                        virtualNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                        virtualNep.setLpq(PHOTONICLAYERQUALIFIERMC.VALUE);
                        virtualNep.setFrequencyBitset(mmSrgOtsNep.entrySet().stream()
                            .filter(bpn -> otsSrgNepKeyList.stream()
                                .map(NodeEdgePointKey::getNodeEdgePointUuid).collect(Collectors.toList())
                                .contains(bpn.getKey()))
                            .findAny().orElseThrow().getValue().getFrequenciesBitSet());
                        // In contrary to DEG CTPs, do not define a unique Parent NEP as there are multiple PPs
                        mmSrgOtsNep.put(virtualNep.getNepCepUuid(), virtualNep);
                    }
                    for (Map.Entry<Uuid, BasePceNep> otsNep : mmSrgOtsNep.entrySet()) {
                        if (otsSrgNepKeyList.stream().filter(nepkey -> nepkey.getNodeEdgePointUuid()
                            .equals(otsNep.getKey())).findAny().isPresent()) {
                            // We store the NodeRuleGroup Id whatever is the
                            // forwarding condition
                            otsNep.getValue().setNodeRuleGroupUuid(nrg.getKey().getUuid());
                            // We store the virtual NEP Id, only if we have a blocking condition, as the possibility
                            // for a PP to forward traffic to a TTP may be coded in many different way, and we don't
                            // want to multiply the number of virtual NEP (used as extremity of links interconnecting
                            // Add/Drop to Degrees
                            if (blocking) {
                                LOG.debug("TONLine670 blocking true, setting Vnep for OTS nep");
                                // Add in srgOtsNep Map the virtual node associated to the block
                                otsNep.getValue().setVirtualNep(vnepId1);
                            }
                        }
                    }
                }
                LOG.debug("TONLine677 Node {} mmSrgOtsNep NepName in BuiltvirtualCP andCtps{}", nodeName,
                    mmSrgOtsNep.entrySet().stream()
                        .map(nep -> nep.getValue().getName().entrySet().iterator().next().getValue().getValue())
                        .collect(Collectors.toList()));
                LOG.debug("TONLine681 Node {} mmSrgOtsNep VirtNepName declared @ the end of BuiltvirtualCP andCtps{}",
                    nodeName,
                    mmSrgOtsNep.entrySet().stream()
                        .map(nep -> nep.getValue().getVirtualNep()).collect(Collectors.toList()));
                LOG.debug("TONLine685 Node {} SrgOtsNep VirtualNepName declared at the end of BuiltvirtualCP andCtps{}",
                    nodeName,
                    mmSrgOtsNep.values().stream().map(nep -> nep.getName().entrySet().iterator().next().getValue()
                        .getValue()).collect(Collectors.toList()));
                if (!(otsDegNepKeyList == null || otsDegNepKeyList.isEmpty()) && !blocking) {
                    Map<Uuid, BasePceNep> tempVirtualBpnMap = new HashMap<>();
                    for (Map.Entry<Uuid, BasePceNep> otsNep : mmDegOtsNep.entrySet()) {
                        //for each otsNep (there are several degrees, create a virtualNepId CTP
                        Map<Uuid, Name> vnepId2;
                        String otsNepName = otsNep.getValue()
                                .getName().entrySet().iterator().next().getValue().getValue();
                        vnepId2 = createNodeOrVnepId(otsNepName.split("\\-TTP")[0], "DEG", false);
                        if (otsDegNepKeyList.stream().filter(nepkey -> nepkey.getNodeEdgePointUuid()
                            .equals(otsNep.getKey())).findAny().isPresent()) {
                            otsNep.getValue().setNodeRuleGroupUuid(nrg.getKey().getUuid());
                            // We store the virtual NEP Id, only if we have a non blocking condition
                            if (!blocking) {
                                // Add in DegOtsNep Map the virtual node associated to the block
                                otsNep.getValue().setVirtualNep(vnepId2);
                                Map<NameKey, Name> vnepNameMap2 = new HashMap<>();
                                vnepNameMap2.put(vnepId2.entrySet().iterator().next().getValue().key(),
                                    vnepId2.entrySet().iterator().next().getValue());
                                BasePceNep virtualNep = new BasePceNep(vnepId2.entrySet().iterator().next().getKey(),
                                    vnepNameMap2);
                                virtualNep.setDirection(DirectionType.BIDIRECTIONAL);
                                virtualNep.setOperationalState(OperationalState.ENABLED);
                                virtualNep.setAdminState(AdministrativeState.UNLOCKED);
                                virtualNep.setTpType(OpenroadmTpType.DEGREETXRXCTP);
                                virtualNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                                virtualNep.setLpq(PHOTONICLAYERQUALIFIERMC.VALUE);
                                virtualNep.setParentNep(otsNep.getKey());
                                virtualNep.setFrequencyBitset(otsNep.getValue().getFrequenciesBitSet());
                                tempVirtualBpnMap.put(vnepId2.entrySet().iterator().next().getKey(), virtualNep);
                            }
                        }
                    }
                    mmDegOtsNep.putAll(tempVirtualBpnMap);
                    LOG.debug("TONLine803 Node {} mmDegOtsNep NepName in BuiltvirtualCP andCtps{}", nodeName,
                        mmDegOtsNep.entrySet().stream()
                            .map(nep -> nep.getValue().getName().entrySet().iterator().next().getValue().getValue())
                            .collect(Collectors.toList()));
                    LOG.debug("TONLine807 Node {} mmDegOtsNep VirtualNepName declared @end of BuiltvirtualCP andCtps{}",
                        nodeName,
                        mmDegOtsNep.entrySet().stream()
                            .map(nep -> nep.getValue().getVirtualNep()).collect(Collectors.toList()));
                    LOG.debug("TONLine811 Node {} DegOtsNep VirtualNepName declared (end of) BuiltvirtualCP andCtps{}",
                        nodeName,
                        mmDegOtsNep.values().stream().map(nep -> nep.getName().entrySet().iterator().next().getValue()
                            .getValue()).collect(Collectors.toList()));
                }
            } else {
                // No forwarding condition defined
                break;
            }
        }
    }

    /**
     * Create a Dictionary (couple) made from a name and a Uuid as the key for both nodes and NodeEdgePoints.
     * @param identifier    The name of the Termination Point (NodeEdgePoint),
     * @param extension     The extension is either SRG or DEG for Add/drop and Degrees,
     * @param isNode        Defines whether the Id is built for a NEP or a Node (different value name).
     * @return              A Map tp Name using the tp Uuid as the key
     */
    private Map<Uuid, Name> createNodeOrVnepId(String identifier, String extension, boolean isNode) {
        // Complement this method to have the PP order
        String tpType;
        if ("SRG".equals(extension)) {
            tpType = "CP";
        } else if ("DEG".equals(extension)) {
            tpType = "CTP";
        } else {
            tpType = "";
        }
        Name name;
        if (isNode) {
            name = new NameBuilder().setValueName("VirtualNodeName")
                .setValue(String.join("+", nodeName.getValue(), (extension + identifier)))
                .build();
        } else {
            name = new NameBuilder().setValueName("VirtualNepName")
                .setValue(String.join("-", identifier, tpType)).build();
        }
        Map<Uuid, Name> id = new HashMap<>();
        Uuid uuid = new Uuid(UUID.nameUUIDFromBytes(name.getValue().getBytes(Charset.forName("UTF-8"))).toString());
        id.put(uuid, name);
        return id;
    }

    /**
     * Generic function used to generate Debug LOG with the main parameters of created virtual NEPs in ROADMs.
     */
    private void printBpnListImportantParameters() {
        Map<String, String> srgNepAndVnepMap = new HashMap<>();
        for (BasePceNep bpn : mmSrgOtsNep.values()) {
            srgNepAndVnepMap.put(bpn.getName().entrySet().iterator().next().getValue().getValue(),
                bpn.getVirtualNep() == null || bpn.getVirtualNep().isEmpty()
                    ? "xxxxx"
                    : bpn.getVirtualNep().entrySet().iterator().next().getValue().getValue());
        }
        LOG.debug("TONLine 866 :Names of NEPs and their associated Virtual NEP in SRGOTSNEP are {}", srgNepAndVnepMap);
        Map<String, String> degNepAndVnepMap = new HashMap<>();
        for (BasePceNep bpn : mmDegOtsNep.values()) {
            degNepAndVnepMap.put(bpn.getName().entrySet().iterator().next().getValue().getValue(),
                bpn.getVirtualNep() == null || bpn.getVirtualNep().isEmpty()
                    ? "xxxxx"
                    : bpn.getVirtualNep().entrySet().iterator().next().getValue().getValue());
        }
        LOG.debug("TONLine 874 : Names of NEPs and their associated Virtual NEP in DEGOTSNEP are {}", degNepAndVnepMap);
    }

    /**
     * Creates internal links/edges between virtual NEPs representing CPs and CTPs in ROADMs
     *  This method is used in the process of disaggregating ROADMs to separate Add/Drops ("SRG") from Degrees.
     *  Rather than creating a full or partial mesh between ROADMs PPs and TTPs, links/edges are only be created
     *  between the virtual NEPs associated with Cps and Ctps.
     *  Process for creation of link between CTPs (bypass) is not the same as from CP to CTP (Add/Drop-links):
     *     Between CTPs we need to examine the node rule group defined for OTS NEPs considering virtual
     *     CTP have already been created, whereas for CP to CTP it is base on Inter Rule Groups.
     */
    private void buildInternalLinksMap() {
        printBpnListImportantParameters();
        Map<Uuid, IntLinkObj> intLinkMap = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nrgList = this.node.getNodeRuleGroup();
        for (Map.Entry<NodeRuleGroupKey, NodeRuleGroup> nrg : nrgList.entrySet()) {
            // For each NodeRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
            // and if this is the case store its key in fwRuleKeyList
            List<RuleKey> fwdRuleKeyList =
                nrg.getValue().getRule().entrySet().stream()
                    .filter(rule -> rule.getValue().getRuleType().contains(RuleType.FORWARDING))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Node Rule Group (NRG) does not contain any forwarding rule, we go to next NRG
                continue;
            } else {
                // If we have one or several forwarding rule(s)
                for (RuleKey rk : fwdRuleKeyList) {
                    if (FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE
                        .equals(nrg.getValue().getRule().get(rk).getForwardingRule())
                        || FORWARDINGRULEMUSTFORWARDACROSSGROUP.VALUE
                            .equals(nrg.getValue().getRule().get(rk).getForwardingRule())) {
                        Map<NodeEdgePointKey, NodeEdgePoint> nepMap = nrg.getValue().getNodeEdgePoint();
                        // Store in internalLinkMap all Links created between
                        // NEP for ROADMs
                        LOG.debug("TONLine910 found a FWDING NRG for Node {} with NepMap {}", nodeName, nepMap);
                        if (NodeTypes.Rdm.equals(this.commonNodeType)) {
                            intLinkMap.putAll(createNrgPartialMesh(nepMap));
                        }
                    }
                }
            }
        }
        if (NodeTypes.Rdm.equals(this.commonNodeType)) {
            LOG.debug("TONLine919 At end of FWDING NRG scan (for Optical Bypass  in ROADM) IntLinkMap size = {}",
                intLinkMap.keySet().size());
        }

        // In this part of the method we add to InternalLinkMap the links to be
        // created with regard to Inter-Rule-Group
        Map<InterRuleGroupKey, InterRuleGroup> irgList = this.node.getInterRuleGroup();
        for (Map.Entry<InterRuleGroupKey, InterRuleGroup> irg : irgList.entrySet()) {
            // For each InterRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
            // and if this is the case store its key in fwRuleKeyList
            List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.RuleKey>
                fwdRuleKeyList = irg.getValue().getRule().entrySet().stream()
                    .filter(rule -> rule.getValue().getRuleType().contains(RuleType.FORWARDING))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Inter Rule Group (IRG) does not contain any forwarding rule, we go to next IRG
                continue;
            } else {
                // If we have one or several forwarding rule(s)
                for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.RuleKey
                        rk : fwdRuleKeyList) {
                    if (FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE
                        .equals(irg.getValue().getRule().get(rk).getForwardingRule())
                        || FORWARDINGRULEMUSTFORWARDACROSSGROUP.VALUE
                            .equals(irg.getValue().getRule().get(rk).getForwardingRule())) {
                        Map<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> anrgMap = irg.getValue()
                            .getAssociatedNodeRuleGroup();
                        int indexMap = 0;
                        Map<Integer, Map<NodeEdgePointKey, NodeEdgePoint>> intercoNepMap = new HashMap<>();
                        for (Map.Entry<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> anrg : anrgMap.entrySet()) {
                            intercoNepMap.put(indexMap, nrgList.get(new NodeRuleGroupKey(anrg.getValue()
                                .getNodeRuleGroupUuid())).getNodeEdgePoint());
                            indexMap++;
                        }
                        Map<Integer, Map<NodeEdgePointKey, NodeEdgePoint>> subIntercoMapI = new HashMap<>();

                        if (NodeTypes.Rdm.equals(this.commonNodeType)) {
                            LOG.debug("TONLine859 IntLinkMap size = {}", intercoNepMap.keySet().size());
                            for (int index = 0 ; index < intercoNepMap.keySet().size() - 1; index++) {
                                for (int subIx = index + 1 ; subIx < intercoNepMap.keySet().size(); subIx ++) {
                                    LOG.debug("TONLine926 IntLinkMap Loop index = {} subIx = {} ", index, subIx);
                                    subIntercoMapI.clear();
                                    subIntercoMapI.put(0, intercoNepMap.get(index));
                                    subIntercoMapI.put(1, intercoNepMap.get(subIx));
                                    intLinkMap.putAll(createIrgPartialMesh(subIntercoMapI));
                                }
                            }
                            LOG.debug("TONLine869 IntLinkMap of {} links as follows orgTpList = {} dstTpList = {}",
                                intLinkMap.size(),
                                intLinkMap.values().stream()
                                    .map(IntLinkObj::getOrgTpUuid).collect(Collectors.toList()),
                                intLinkMap.values().stream()
                                    .map(IntLinkObj::getDestTpUuid).collect(Collectors.toList()));
                        }
                    }
                }
            }
        }
        this.internalLinkMap = intLinkMap;
        LOG.debug("TONLine881 BuildInternalLinkMap intlinkMap = {}", this.internalLinkMap);
    }

    /**
     * Create a Full Mesh of links between VirtualNEPs associated to all pairs of NEP of NepMap, without duplicate.
     *  Consider that this is a partial mesh, as we create less links than if we were creating all the links
     *  between any combination of NEPs of the NepMap.
     * @param nepMap    A dictionary of virtual NEPs using their Uuid as the key.
     * @return          A dictionary of IntLinkObj, defining internal Link Objects, build from an Id (Uuid, Name),
     *                  and the Uuid of the origin and destination of the internal link.
     */
    private Map<Uuid, IntLinkObj> createNrgPartialMesh(Map<NodeEdgePointKey, NodeEdgePoint> nepMap) {
        Map<Uuid, IntLinkObj> intLinkMap = new HashMap<>();
        List<String> uuidSortedList = new ArrayList<String>();
        int nepOrder = 0;
        // IndexedNepList is used to create the Mesh : it includes all nodes of NepMap with an index
        Map<Integer, Uuid> indexedNepList = new HashMap<Integer, Uuid>();
        for (Map.Entry<NodeEdgePointKey, NodeEdgePoint> nep : nepMap.entrySet()) {
            indexedNepList.put(nepOrder, nep.getKey().getNodeEdgePointUuid());
            nepOrder++;
        }
        nepOrder = 1;
        String orgNodeType = "";
        String destNodeType = "";
        // IndexedNepList is used to create the Mesh : for each Nep of the list, will create links between the Virtual
        // NEP associated with the current NEP of the first loop, and the virtualNEPs associated with any NEP of higher
        // rank in IndexedNepList
        for (Map.Entry<Integer, Uuid> nepUuid : indexedNepList.entrySet()) {
            LOG.debug("TONLine1006 node {} mmSrgOtsNep Uuid list {}", nodeName, mmSrgOtsNep.values().stream()
                .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
            LOG.debug("TONLine1008 node {} mmDegOtsNep Uuid list {}", nodeName, mmDegOtsNep.values().stream()
                .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
            LOG.debug("TONLine1010 node {} indexedNepList Uuid list {}", nodeName, indexedNepList);
            BasePceNep orgBpn = null;
            if (!mmSrgOtsNep.values().stream().filter(bpn -> nepUuid.getValue().equals(bpn.getNepCepUuid()))
                    .collect(Collectors.toList()).isEmpty()) {
                orgBpn = mmSrgOtsNep.values().stream().filter(bpn -> nepUuid.getValue().equals(bpn.getNepCepUuid()))
                    .findFirst().orElseThrow();
                orgNodeType = "SRG";
            } else if (!mmDegOtsNep.values().stream().filter(bpn -> nepUuid.getValue().equals(bpn.getNepCepUuid()))
                .collect(Collectors.toList()).isEmpty()) {
                orgBpn = mmDegOtsNep.values().stream().filter(bpn -> nepUuid.getValue().equals(bpn.getNepCepUuid()))
                    .findFirst().orElseThrow();
                orgNodeType = "DEG";
            } else {
                LOG.debug("Nep {} not included in srg/Deg Nep List, this nep may not be connected to existing link",
                    nepUuid);
            }
            LOG.debug("TONLine1026 createNrgPartialMesh : Org Nep {} found in {}", nepUuid, orgNodeType);
            if (orgBpn == null) {
                nepOrder++;
                continue;
            }
            Uuid orgVnepUuid = orgBpn.getVirtualNep().entrySet().iterator().next().getKey();
            String orgVnepName = orgBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
            LOG.debug("TON line1033 createNrgPartialMesh, scanning NEP, orgVnep UUID is {}, orgVnep Name is {}",
                orgVnepUuid, orgVnepName);

            // The second for loop is scanning any Nep of higher rank than the
            // current NEP of the first for loop
            if (nepOrder > indexedNepList.size() - 1) {
                break;
            }
            LOG.debug("TON line1041 nepOrder is {}, indexedNepList.size() is {}", nepOrder, indexedNepList.size());
            for (int nnO = nepOrder; nnO < indexedNepList.size(); nnO++) {
                LOG.debug("TON line1043 entering loop for dest Vnep (createNrg Partial Mesh");
                String nepId = indexedNepList.get(nnO).getValue();
                LOG.debug("TON line1044 nepId is {}", nepId);
                BasePceNep destBpn = null;
                if (!mmSrgOtsNep.values().stream().filter(bpn -> nepId.equals(bpn.getNepCepUuid().getValue()))
                        .collect(Collectors.toList()).isEmpty()) {
                    destBpn = mmSrgOtsNep.values().stream()
                        .filter(bpn -> nepId.equals(bpn.getNepCepUuid().getValue()))
                        .findFirst().orElseThrow();
                    destNodeType = "SRG";
                } else if (!mmDegOtsNep.values().stream().filter(bpn -> nepId.equals(bpn.getNepCepUuid().getValue()))
                        .collect(Collectors.toList()).isEmpty()) {
                    destBpn = mmDegOtsNep.values().stream().filter(bpn -> nepId.equals(bpn.getNepCepUuid().getValue()))
                        .findFirst().orElseThrow();
                    destNodeType = "DEG";
                } else {
                    LOG.debug("TONLine1059 did not find in Deg/srgOtsNep indexNepList Nep {}", nnO);
                }
                LOG.debug("TONLine1061 createNrgPartialMesh, destVnep {} found in {} for orgVnep with UUID {} in {}",
                    nepId, destNodeType, nepUuid, orgNodeType);
                if (destBpn == null) {
                    break;
                }
                Uuid destVnepUuid = destBpn.getVirtualNep().entrySet().iterator().next().getKey();
                String destVnepName = destBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
                LOG.debug("TON Line1068 createNrgPartialMesh, scanning NEP, destinationVnep UUID is {},"
                    + "destination Vnep Name is {}", destVnepUuid, destVnepName);
                uuidSortedList.clear();
                uuidSortedList.add(orgVnepUuid.toString());
                uuidSortedList.add(destVnepUuid.toString());
                // Assuming these links are bidirectional, sort the list in order to avoid creating 2 times the same
                // link Org-Dest & Dest-Org
                Collections.sort(uuidSortedList);
                if (orgVnepUuid.toString().equals(uuidSortedList.get(0))) {
                    Map<Uuid, Name> linkId = createLinkId(orgVnepName, destVnepName);
                    intLinkMap.put(linkId.entrySet().iterator().next().getKey(),
                        new IntLinkObj(linkId, orgVnepUuid, destVnepUuid));
                    addCpCtpOutgoingLink(orgVnepUuid, destVnepUuid, orgNodeType, destNodeType, linkId);
                } else {
                    Map<Uuid, Name> linkId = createLinkId(destVnepName, orgVnepName);
                    intLinkMap.put(linkId.entrySet().iterator().next().getKey(),
                        new IntLinkObj(linkId, destVnepUuid, orgVnepUuid));
                    addCpCtpOutgoingLink(destVnepUuid, orgVnepUuid, destNodeType, orgNodeType, linkId);
                }
            }
            nepOrder++;
        }
        LOG.debug("TON line1090 createNrgPartialMesh, IntLinkMap is {}",
            intLinkMap.values().stream().map(IntLinkObj::getLinkId).collect(Collectors.toList()));
        return intLinkMap;
    }

    /**
     * Create Ids for internal links between virtual NEPs.
     * @param orgName   Origin Termination point name,
     * @param destName  Destnation Termination point name,
     * @return          Internal Link Id as Map of Name using the link Uuid as a key.
     */
    private Map<Uuid, Name> createLinkId(String orgName, String destName) {
        Map<Uuid, Name> vvLinkId = new HashMap<>();
        Name nameVlink = new NameBuilder().setValueName("VirtualLinkName")
            .setValue(String.join("+", orgName, "--", destName)).build();
        Uuid vvLinkUuid = new Uuid(UUID.nameUUIDFromBytes(nameVlink
            .getValue().getBytes(Charset.forName("UTF-8"))).toString());
        vvLinkId.put(vvLinkUuid, nameVlink);
        return vvLinkId;
    }

    /**
     * Allows to qualify the validity of a NEP/CEP/SIP whatever is the portId used in the request: NEP/CEP/SIP Uuid.
     *   If CEP Uuid or SIP Uuid is used, Bpn that has CEP/SIP corresponding to the PortId provided in the request will
     *   be validated and PCE can later rely on NEP rather than CEP/SIP (Easier to handle notably since SIP model in
     *   the context does not include any reference to the NEP it is attached to!
     * @param bpn The BasePceNep to be tested,
     * @return True/False depending on whether the NEP/CEP/SIP is valid according to end points defined in the request.
     */
    private boolean isValidBpn(BasePceNep bpn) {
        if (aaPortId == null && zzPortId == null) {
            return true;
        }
        if (bpn.getSipUuid() == null) {
            LOG.debug("TONLine1021, null SIP for BPN {}", bpn.getName());
        }
        if ((aaPortId != null && bpn.getNepCepUuid().equals(aaPortId))
                || (zzPortId != null && bpn.getNepCepUuid().equals(zzPortId))
                || (aaPortId != null && bpn.getSipUuid() != null && bpn.getSipUuid().equals(aaPortId))
                || (zzPortId != null && bpn.getSipUuid() != null && bpn.getSipUuid().equals(zzPortId))
                || (aaPortId != null && getCepUuidFromParentNepUuid(bpn.getNepCepUuid()) != null
                    && getCepUuidFromParentNepUuid(bpn.getNepCepUuid()).equals(aaPortId))
                || (zzPortId != null && getCepUuidFromParentNepUuid(bpn.getNepCepUuid()) != null
                    && getCepUuidFromParentNepUuid(bpn.getNepCepUuid()).equals(zzPortId))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the child ConnectionEndPoint's Uuid of The Node Edge Point provided as an input.
     * @param nepUuid   Uuid of the Parent NodeEdgePoint
     * @return  The child CEP's Uuid
     */
    private Uuid getCepUuidFromParentNepUuid(Uuid nepUuid) {
        if (node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(nep -> nep.getKey().getUuid().equals(nepUuid)).collect(Collectors.toList()).isEmpty()) {
            return null;
        }
        OwnedNodeEdgePoint ownedNep = node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(nep -> nep.getKey().getUuid().equals(nepUuid)).findFirst().orElseThrow().getValue();
        if (ownedNep.augmentation(OwnedNodeEdgePoint1.class) != null
                && ownedNep.augmentation(OwnedNodeEdgePoint1.class).getCepList() != null
                && !ownedNep.augmentation(OwnedNodeEdgePoint1.class).getCepList()
                .getConnectionEndPoint().isEmpty()) {
            for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep
                .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                .entrySet()) {
                if (cep.getValue().getParentNodeEdgePoint() != null) {
                    return cep.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid();
                }
            }
        }
        return null;
    }

    /**
     * Allows to qualify the validity of a NEP whatever is the portId used in the request: NEP/SIP Uuid.
     *   Returns True if either provided Nep Uuid, or its associated SIP Uuid corresponds to one of the end points (A/Z)
     *   provided in the service request.
     * @param onep The NEP to be tested,
     * @return   True/False depending on whether the NEP or its SIP is valid according to End Points defined in the
     *           request.
     */
    private boolean isValidNep(OwnedNodeEdgePoint onep) {
        if (aaPortId == null && zzPortId == null) {
            return true;
        }
        Uuid nepUuid = onep.getUuid();
        Uuid sipUuid = getOnepSipUuid(onep);
        if ((aaPortId != null && nepUuid.equals(aaPortId))
                || (zzPortId != null && nepUuid.equals(zzPortId))
                || (aaPortId != null && sipUuid.equals(aaPortId))
                || (zzPortId != null && sipUuid.equals(zzPortId))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the Uuid of the MappedServiceInterfacePoint associated with the OwnedNodeEdgePoint provided as an input.
     * @param onep OwnedNodeEdgePoint hosting the SIP
     * @return     Its hosted SIP Uuid.
     */
    private Uuid getOnepSipUuid(OwnedNodeEdgePoint onep) {
        if (onep.getMappedServiceInterfacePoint() != null && !onep.getMappedServiceInterfacePoint().isEmpty()) {
            return onep.getMappedServiceInterfacePoint().entrySet().iterator().next().getKey()
                .getServiceInterfacePointUuid();
        }
        return null;
    }

    /**
     * Sets ConnectedInternalLinks attributes in BasePceNep that constitute ROADM internal Link extremities.
     *  The BasePceNep concerned are Ots Virtual NEP of Degrees' and SRGs of the ROADM
     * @param orgVnepId     Uuid of the origin Virtual NEP
     * @param destVnepId    Uuid of the destination Virtual NEP
     * @param orgNodeType   Uuid of the origin Node
     * @param destNodeType  Uuid of the destination Node
     * @param linkId        LinkId of the internal link to be set as the BasePceNep ConnectedInternalLinks
     */
    private void addCpCtpOutgoingLink(Uuid orgVnepId, Uuid destVnepId, String orgNodeType, String destNodeType,
        Map<Uuid, Name> linkId) {
        if ("SRG".equals(orgNodeType)) {
            mmSrgOtsNep.values().stream().filter(bpn -> orgVnepId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
                .setConnectedInternalLinks(linkId);
        }
        if ("DEG".equals(orgNodeType)) {
            mmDegOtsNep.values().stream().filter(bpn -> orgVnepId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
                .setConnectedInternalLinks(linkId);
        }
        if ("SRG".equals(destNodeType)) {
            mmSrgOtsNep.values().stream().filter(bpn -> destVnepId.equals(bpn.getNepCepUuid())).findFirst()
                .orElseThrow().setConnectedInternalLinks(linkId);
        }
        if ("DEG".equals(destNodeType)) {
            mmDegOtsNep.values().stream().filter(bpn -> destVnepId.equals(bpn.getNepCepUuid())).findFirst()
                .orElseThrow().setConnectedInternalLinks(linkId);
        }
        LOG.debug("TONLine1230 mmDegOtsNep outgoing Links {}",
            mmDegOtsNep.values().stream().map(bpn -> bpn.getConnectedLink()).collect(Collectors.toList()));
        LOG.debug("TONLine1232 mmSrgOtsNep outgoing Links {}",
            mmSrgOtsNep.values().stream().map(bpn -> bpn.getConnectedLink()).collect(Collectors.toList()));
    }

    /**
     * Generates all Degree disaggregated nodes associated with one ROADM.
     * @return  A map of PceTapiOpticalNode resulting from the disaggregation of the T-API ROADM node.
     */
    private Map<Uuid, PceTapiOpticalNode> splitDegNodes() {
        LOG.debug("TONLine1242 Node {} mmDegOtsNep Uuid list {}", nodeName, mmDegOtsNep.values().stream()
            .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        LOG.debug("TONLine1244 Node {} mmDegOtsNep TpType list {}", nodeName, mmDegOtsNep.values().stream()
            .map(BasePceNep::getTpType).collect(Collectors.toList()));
        LOG.debug("TONLine1246 Node {} mmdegOtsNep Name list {}", nodeName, mmDegOtsNep.values().stream()
            .map(BasePceNep::getName).collect(Collectors.toList()));
        List<Map<Uuid, Name>> vvNepIdList = new ArrayList<>();
        // List<PceTapiOpticalNode> degList = new ArrayList<>();
        Map<Uuid, PceTapiOpticalNode> degMap = new HashMap<>();
        // Build a list of VirtualNEP contained in mmDegOtsNep
        for (BasePceNep bpn : mmDegOtsNep.values()) {
            if (bpn.getTpType().equals(OpenroadmTpType.DEGREETXRXCTP)) {
                Map<Uuid, Name> vvNepIdMap = new HashMap<>();
                vvNepIdMap.put(bpn.getNepCepUuid(), bpn.getName().entrySet().iterator().next().getValue());
                vvNepIdList.add(vvNepIdMap);
            }
        }
        LOG.debug("TONLine1259 Node {} vNepIdList {}", nodeName, vvNepIdList);
        List<Integer> indexList = new ArrayList<>();
        int index;
        int subindex = 100;
        // For each virtual NEP of the list, Builds a degXOtsNep list of BasePceNode which is a subset of mmDegOtsNep,
        // containing all BasePceNep that have it declared as Virtual NEP
        for (Map<Uuid, Name> vvNepId : vvNepIdList) {
            Uuid vvNepUuid = vvNepId.keySet().stream().findFirst().orElseThrow();

            String vvNepString = vvNepId.entrySet().iterator().next().getValue().getValue();
            index = Integer.decode(vvNepString.substring(vvNepString.indexOf("+DEG", 0) + 4,
                vvNepString.indexOf("-CTP", 0) - 0));
            Map<Uuid, Name> nodeId;
            if (indexList.contains(index)) {
                LOG.debug("Error generating Node Index for {}, from Virtual NEP {}, as index {} is already used",
                    this.nodeName, vvNepString, Integer.toString(index));
                nodeId = createNodeOrVnepId(String.valueOf(subindex), "DEG", true);
                indexList.add(subindex);
                subindex++;
            } else {
                indexList.add(index);
                nodeId = createNodeOrVnepId(String.valueOf(index), "DEG", true);
            }
            // Creates a new PceTapiOpticalNode corresponding to the degree defined by this VirtualNep
            List<BasePceNep> degXOtsNep = mmDegOtsNep.values().stream()
                .filter(bpn -> bpn.getNepCepUuid().equals(vvNepUuid) || bpn.getVirtualNep().containsKey(vvNepUuid))
                .collect(Collectors.toList());
            LOG.debug("TONLine1286 Node {} create TapiOpticalNode {}", nodeName, nodeId);
            var degNode = new PceTapiOpticalNode(serviceType, this.node, OpenroadmNodeType.DEGREE,
                version, degXOtsNep, nodeId, nodeName.getValue(), mcCapability);
            LOG.debug("TONLine1289 CREATEDEGNODE {} of class {} ", nodeId, degNode.getClass());
            Map<Uuid, Uuid> vnepToSubNode = new HashMap<>();
            for (BasePceNep bpn : degXOtsNep) {
                if (vvNepUuid.equals(bpn.getNepCepUuid())) {
                    vnepToSubNode.put(vvNepUuid, nodeId.entrySet().iterator().next().getKey());
                }
            }
            degNode.initFrequenciesBitSet();
            LOG.debug("TONLine1297 Node {} NodeId {} Subnode Vnep List {}", nodeName, nodeId, vnepToSubNode);
            this.bindingVNepToSubnodeMap.putAll(vnepToSubNode);
            degMap.put(nodeId.keySet().iterator().next(), degNode);
            LOG.debug("TONLine1300 CREATEDEGNODE {}", degNode.getSupClliNodeId());
            LOG.debug("TONLine1301 CREATEDEGNODE {}", degNode.getAdminState());
            LOG.debug("TONLine1302 CREATEDEGNODE {}", degNode.getCentralFreqGranularity());
            LOG.debug("TONLine1303 CREATEDEGNODE {}", degNode.getNodeId());
            LOG.debug("TONLine1304 CREATEDEGNODE {}", degNode.getState());
            LOG.debug("TONLine1305 creating Node {}", nodeId);
        }
        return degMap;
    }

    /**
     * Generates all SRG disaggregated nodes associated with one ROADM.
     * @return  A map of PceTapiOpticalNode resulting from the disaggregation of the T-API ROADM node.
     */
    private Map<Uuid, PceTapiOpticalNode> splitSrgNodes() {
        LOG.debug("TONLine1315 mmSrgOtsNep Uuid list {}", mmSrgOtsNep.values().stream()
            .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        LOG.debug("TONLine1317 mmSrgOtsNep TpType list {}", mmSrgOtsNep.values().stream()
            .map(BasePceNep::getTpType).collect(Collectors.toList()));
        LOG.debug("TONLine1319 mmSrgOtsNep Name list {}", mmSrgOtsNep.values().stream()
            .map(BasePceNep::getName).collect(Collectors.toList()));
        List<Map<Uuid, Name>> vvNepIdList = new ArrayList<>();
        Map<Uuid, PceTapiOpticalNode> srgMap = new HashMap<>();
        // Build a list of VirtualNEP contained in mmSrgOtsNep
        for (BasePceNep bpn : mmSrgOtsNep.values()) {
            if (bpn.getTpType().equals(OpenroadmTpType.SRGTXRXCP)) {
                Map<Uuid, Name> vvNepIdMap = new HashMap<>();
                vvNepIdMap.put(bpn.getNepCepUuid(), bpn.getName().entrySet().iterator().next().getValue());
                vvNepIdList.add(vvNepIdMap);
            }
        }
        LOG.debug("TONLine1331 vNepIdMap {}", vvNepIdList);
        List<Integer> indexList = new ArrayList<>();
        int index;
        int subindex = 100;
        // For each virtual NEP of the list, Builds a srgXOtsNep list of BasePceNode which is a subset of mmSrgOtsNep,
        // containing all BasePceNep that have it declared as Virtual NEP
        for (Map<Uuid, Name> vvNepId : vvNepIdList) {
            Uuid vvNepUuid = vvNepId.keySet().stream().findFirst().orElseThrow();
            List<BasePceNep> srgXOtsNep = mmSrgOtsNep.values().stream()
                .filter(bpn -> bpn.getNepCepUuid().equals(vvNepUuid) || bpn.getVirtualNep().containsKey(vvNepUuid))
                .collect(Collectors.toList());
            String vvNepString = vvNepId.entrySet().iterator().next().getValue().getValue();
            index = Integer.decode(vvNepString.substring(vvNepString.indexOf("+SRG", 0) + 4,
                vvNepString.indexOf("-CP", 0) - 0));
            Map<Uuid, Name> nodeId;
            if (indexList.contains(index)) {
                LOG.debug("Error generating Node Index for {}, from Virtual NEP {}, as index {} is already used",
                    this.nodeName.toString(), vvNepString, Integer.toString(index));
                nodeId = createNodeOrVnepId(String.valueOf(subindex), "SRG", true);
                indexList.add(subindex);
                subindex++;
            } else {
                indexList.add(index);
                nodeId = createNodeOrVnepId(String.valueOf(index), "SRG", true);
            }
            // Creates a new PceTapiOpticalNode corresponding to the degree defined by this VirtualNep
            var srgNode = new PceTapiOpticalNode(serviceType, this.node, OpenroadmNodeType.SRG,
                version, srgXOtsNep, nodeId, nodeName.getValue(), mcCapability);
            LOG.debug("TONLine1359 new PceTapiON {}, list of nep is {}",
                srgNode.getNodeId(),
                srgNode.getListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
            Map<Uuid, Uuid> vnepToSubNode = new HashMap<>();
            for (BasePceNep bpn : srgXOtsNep) {
                if (vvNepUuid.equals(bpn.getNepCepUuid())) {
                    vnepToSubNode.put(vvNepUuid, nodeId.entrySet().iterator().next().getKey());
                }
            }
            this.bindingVNepToSubnodeMap.putAll(vnepToSubNode);
            srgNode.initSrgTps();
            srgNode.initFrequenciesBitSet();
            srgMap.put(nodeId.keySet().iterator().next(), srgNode);
            LOG.debug("TONLine1372 CREATESRGNODE {}", srgNode);
            LOG.debug("TONLine1373 creating Node {}", nodeId);
        }
        return srgMap;
    }

    /**
     * Retrieves and provides the direction of a CEP/NEP, as well as the corresponding OpenROADM tp type.
     * @param ownedNep  The OwnedNodeEdgePoint to evaluate (null if the item to qualify is a CEP)
     * @param cep       The ConnectionEndPoint to evaluate (null if the item to qualify is a NEP)
     * @param tpType    Termination Point type (PP, CP, CTP, TTP, NW, CLIENT)
     * @return          A dictionary with OpenROADM tp Type, and the Tapi directionType as key.
     */
    private Map<DirectionType, OpenroadmTpType> calculateDirection(
        OwnedNodeEdgePoint ownedNep, ConnectionEndPoint cep, TpType tpType) {
        String nodeType;
        String finalTpType = tpType.toString();
        Direction directionEnum;
        boolean isNep = false;
        switch (tpType) {
            case PP:
            case CP:
                nodeType = "SRG";
                break;
            case TTP:
            case CTP:
                nodeType = "DEGREE";
                break;
            case NW:
                nodeType = "XPONDER";
                finalTpType = "NETWORK";
                break;
            case CLIENT:
                nodeType = "XPONDER";
                finalTpType = "CLIENT";
                break;
            default:
                nodeType = "UNDEFINED";
        }
        // The default direction is set to BIDIRECTIONAL
        // direction = "BIDIRECTIONAL";
        if (ownedNep != null) {
            directionEnum = ownedNep.getDirection();
            isNep = true;
        } else if (cep != null) {
            directionEnum = cep.getDirection();
        } else {
            directionEnum = Direction.UNDEFINEDORUNKNOWN;
        }
        if (Direction.UNDEFINEDORUNKNOWN.equals(directionEnum)) {
            if (isNep) {
                LOG.debug("Error processing Nep {} as port direction is not defined/known",
                    ownedNep.getName().toString());
            } else {
                LOG.debug("Error processing Cep {} as connection direction is not defined/known",
                    cep.getName().toString());
            }
            return null;
        }
        String directionCode;
        switch (directionEnum) {
            case BIDIRECTIONAL:
                directionCode = "TXRX";
                break;
            case SINK:
                directionCode = "RX";
                break;
            case SOURCE:
                directionCode = "TX";
                break;
            default:
                directionCode = "TXRX";
        }
        directionCode = nodeType.equals("XPONDER") ? "" : directionCode;
        Map<DirectionType, OpenroadmTpType> dirTpType = new HashMap<>();
        dirTpType.put(DirectionType.valueOf(directionEnum.toString()),
            //OpenroadmTpType.
            OpenroadmTpType.valueOf(nodeType + directionCode + finalTpType));
        return dirTpType;
    }

    /**
     * Checks whether a node is Valid or not and instantiate either a PceTapiOpticalNode or a PceTapiOtnNode.
     *  A Xponder node is considered as Valid if it is identified as a potential end of the exercised service request.
     *  According to the service type, creates either a PceOpticalNode (Photonic layer) or a PceTapiOtnNode (OTN layer).
     *  Initialize the BasePceNeps (BPNs)corresponding to the tp of the TAPI Node, calling initTapiXndrTps.
     *  Complete the setting of BPNs, calling initXndrTps and initFrequencyBitset for PceTapiOpticalNodes,
     *  or validateXponder for TapiOtnNode.
     */
    public void validateAZxponder() {
        LOG.info("TONLine1462 validateAZxponder: STEP1 entering validation for XPONDER == Id: {}, Name : {}",
            node.getUuid().toString(), node.getName().toString());
        if (!this.valid || this.commonNodeType != NodeTypes.Xpdr) {
            return;
        }
        LOG.info("TONLine1467 validateAZxponder: STEP2 entering validation for XPONDER == Id: {}, Name : {}",
            node.getUuid().toString(), node.getName().toString());
        // Detect A and Z
        if (aaNodeId.getValue().equals(node.getUuid().getValue())
            || zzNodeId.getValue().equals(node.getUuid().getValue())) {
            LOG.debug("validateAZxponder TAPI: A or Z node detected == {}, {}", node.getUuid().toString(),
                node.getName().toString());
            LOG.debug("TONLine1474 : ValidateAZxponder : Node {}, is considered as valid end", this.nodeName);
            initTapiXndrTps();
            LOG.debug("TONLine1476: ValidateAZxponder for node : {}, nwOtsNep : {}", this.nodeName,
                nwOtsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
            allOtsNep.clear();
            allOtsNep.addAll(nwOtsNep);
            allOtsNep.addAll(clientDsrNep);
            allOtsNep.stream().distinct().collect(Collectors.toList());

            if (SERVICE_TYPE_PHT_LIST.contains(serviceType)) {
                LOG.info("TONLine1484: ValidateAZxponder for node : {}, allOtsNep : {}", this.nodeName,
                    allOtsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
                Map<Uuid, Name> nodeId = new HashMap<>();
                nodeId.put(nodeUuid, node.getName().entrySet().stream()
                    .filter(name -> !name.getKey().getValueName().equals("Node Type"))
                    .findFirst().orElseThrow().getValue());

                PceTapiOpticalNode xpdr = new PceTapiOpticalNode(serviceType, this.node,
                    OpenroadmNodeType.XPONDER, version, allOtsNep, nodeId, deviceNodeId, mcCapability);
                xpdr.initXndrTps(servFormat);
                xpdr.initFrequenciesBitSet();
                LOG.info("TapiOpticalNode: Node Id: {}, Name : {} has been created and validated",
                    node.getUuid().toString(), node.getName().toString());
                this.pceTapiOptNodeXpdr = xpdr;
                LOG.debug("TONLine1498 : pceTapiOpticalNodecreated  {}", this.pceTapiOptNodeXpdr.getNodeId());
                return;
            } else {
                PceTapiOtnNode otnXpdr = new PceTapiOtnNode(this.node, OpenroadmNodeType.XPONDER,
                    (aaNodeId.getValue().equals(node.getUuid().getValue()) ? aaNodeId : zzNodeId).getValue(),
                    serviceType,
                    (aaNodeId.getValue().equals(node.getUuid().getValue()) ? aaPortId : zzPortId),
                    this);
                otnXpdr.validateXponder(
                    (aaNodeId.getValue().equals(node.getUuid().getValue()) ? aaNodeId : zzNodeId).getValue());
                this.pceTapiOtnNodeXpdr = otnXpdr;
                LOG.info("pceTapiOtnNodecreated  {}", this.pceTapiOtnNodeXpdr.getNodeId());
                return;
            }

        }
        LOG.debug("TONLine1514 validateAZxponder: XPONDER == Id: {}, Name : {} is ignored", node.getUuid().toString(),
            node.getName().toString());
        valid = false;
    }

    /**
     * Initialize the BasePceNeps (BPNs)corresponding to the tp of the TAPI XPONDER Node.
     *  Scans TAPI Node's OwnedNodeEdgePoints, and create BPNs for valid termination points.
     *  This includes OTS NEPs and CEPs, iOTU NEPs and CEPs, DSR NEPs, ODU (eODU on client side, iODU on network side)
     *  NEPs and CEPs.
     *  Populates appropriate Lists of BPNs so that at the end :
     *  - invalidNWNepList contains NEPs vertically connected to OTSiMC NEPs (Lambda provisioned on corresponding port)
     *  - nwOtsNep contains OTS NEP (Network side)
     *  - clientDSR NEPs contains DSR NEPs, eODU and iODU NEPs and potentially CEPs (if ODU service already provisioned)
     *  - otsLcpNWList contains OTS CEPs
     *  - otuLcpList contains iOTU CEPs
     *  - oduLcpList contains eODU and iODU NEPs and potentially CEPs (if ODU service already provisioned).
     *  Calls populateBpnVerticalNep so that each BPN has a list of NEP and CEps it is vertically connected to.
     *  Calls populateBpnNrgForXpdr so that each BPN has a list of NRG it is included in.
     *  Calls populateBpnIndirectNrgForXpdr so that BPN has a list of NRG it is indirectly in visibility of, though IRG.
     *  Removes from nwOtsNep OTS NEP that are not in visibility of the a client portId (when specified in the request).
     */
    public void initTapiXndrTps() {
        if (!this.valid) {
            return;
        }
        Double serviceRate;
        if (("ODU4").equals(this.serviceType)) {
            serviceRate = 100.0;
        } else {
            serviceRate = StringConstants.SERVICE_TYPE_RATE.get(this.serviceType).doubleValue();
        }
        Map<DirectionType, OpenroadmTpType> direction;
        LOG.debug("initTapiXndrTps: service rate is {}", serviceRate);
        LOG.debug("initTapiXndrTps: getting tps from TSP node {}", this.nodeUuid);
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNepList = this.node.getOwnedNodeEdgePoint();
        for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNep : ownedNepList.entrySet()) {
            if (LayerProtocolName.PHOTONICMEDIA.equals(ownedNep.getValue().getLayerProtocolName())
                    && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())
                    && isNepWithGoodCapabilities(ownedNep.getValue().getUuid())) {
                LOG.debug("TONLine1554 initTapiXndrTps: scanning PhotonicNEP");
                if (ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                    .filter(sclpqi -> PHOTONICLAYERQUALIFIEROTSiMC.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                    .findAny() != null) {
                    // This might be the case of NetworkPorts that already have a provisioned Lambda (depends on imp)
                    if (!checkUnUsedSpectrum(ownedNep.getValue().getUuid())
                            || ownedNep.getValue().getAvailablePayloadStructure().stream()
                                .filter(aps -> aps.getCapacity().getValue().doubleValue() >= serviceRate)
                                .collect(Collectors.toList()).isEmpty()) {
                        LOG.debug("TONLine1563 initTapiXndrTps: isPhotonic = {}", isPhotonic);
                        if (isPhotonic) {
                            invalidNwNepList.add(ownedNep.getKey().getUuid());
                            continue;
                        }
                    }
                }
                if (!ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                        .filter(sclpqi -> PHOTONICLAYERQUALIFIEROTS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                        .findAny().isEmpty()
                        && !ownedNep.getValue().getName().entrySet().stream()
                            .findFirst().orElseThrow().getValue().getValue().contains("OTSi")) {
                    LOG.debug("TONLine1575 initXndrTps Found a Nep with OTSlpq {}", ownedNep.getValue().getName());
                    var otsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                    otsNep.setOperationalState(ownedNep.getValue().getOperationalState());
                    direction = calculateDirection(ownedNep.getValue(), null, TpType.NW);
                    otsNep.setDirection(direction.keySet().iterator().next());
                    otsNep.setTpType(direction.values().iterator().next());
                    otsNep.setLpn(LayerProtocolName.PHOTONICMEDIA);
                    otsNep.setLpq(PHOTONICLAYERQUALIFIEROTS.VALUE);
                    //direction.clear();
                    otsNep.setAdminState(ownedNep.getValue().getAdministrativeState());
                    otsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                    BitSet freqBitset = buildBitsetFromSpectrum(ownedNep.getValue().getUuid());
                    otsNep.setFrequencyBitset(freqBitset);
                    LOG.debug("TONLine 1588 : FreqBitset for ONEP {} calculated to {}",
                        ownedNep.getValue().getName(), freqBitset);

                    if (ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class) != null
                            && ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList() != null
                            && !ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList()
                                .getConnectionEndPoint().entrySet().isEmpty()) {
                        for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep.getValue()
                            .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint().entrySet()) {
                            //We check if there is OTS level (ConnectionEndPoint2.class) CEP to get its child NEP
                            //OTS level CEP shall exist even if no service is provisioned.
                            //If no service provisioned the client NEP shall theoretically not be parent of any CEP
                            //But the client CEP could be there (pre-provisionned)
                            var otsCep = new BasePceNep(cep.getValue().getUuid(), cep.getValue().getName());
                            otsCep.setParentNep(cep.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid());
                            otsCep.setTpType(direction.values().iterator().next());
                            @Nullable
                            Map<ClientNodeEdgePointKey, ClientNodeEdgePoint> cepClientNepMap = cep.getValue()
                                .getClientNodeEdgePoint();
                            if (cepClientNepMap != null) {
                                otsCep.setClientNep(cepClientNepMap.keySet().iterator().next().getNodeEdgePointUuid());
                                // If the client NEP of the CEP is already used we do not record the CEP in nwOtsCEP
                                if (invalidNwNepList.contains(cep.getValue().getClientNodeEdgePoint().keySet()
                                    .iterator().next().getNodeEdgePointUuid())) {
                                    //Having client Nep (OTSi_MC) in invladidNwNepList means the port already support a
                                    //wavelength service.
                                    //If a/z-portId has been set and his a network port, the OTS nep shall be placed in
                                    //invalidNwNepList
                                    if (isValidNep(ownedNep.getValue())) {
                                        LOG.info("TONLine1478 initTapiXndrTps: scanning PhotonicNEP");
                                        if (isPhotonic) {
                                            invalidNwNepList.add(ownedNep.getKey().getUuid());
                                        }
                                        continue;
                                    }
                                    //If not, ultimately we should check if the provisioned NW port can support
                                    //additional service (typically in the case of a switch/mux-Ponder). To simplify the
                                    //process, rather than checking bandwidth for additional service provisioning,
                                    //to reduce the dependency to potentially different implementations, we consider
                                    //that the network port is available (Nw port will be part of the path). Only non
                                    //valid client ports will be pruned.
                                }
                            }
                            LOG.debug("TONLine1631 : Adding CEP {} to otsLcpNWList", otsCep.getName());
                            otsLcpNWList.add(otsCep);
                            otsNep.setCepOtsUuid(cep.getValue().getUuid());
                            if (cepClientNepMap != null) {
                                otsNep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                    .getNodeEdgePointUuid());
                            }
                            break;
                        }
                    }
                    direction.clear();
                    LOG.debug("TONLine1642 : Adding NEP {} to nwOtsNep", ownedNep.getValue().getName());
                    nwOtsNep.add(otsNep);
                }
                // Client Port support DSR protocol layer
            } else if (LayerProtocolName.DSR.equals(ownedNep.getValue().getLayerProtocolName())
                && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())) {
                // TODO : activate following 6 lines of the code after capacity is correctly populated in the topology
                if (ownedNep.getValue().getAvailableCapacity() == null
                        || ownedNep.getValue().getAvailableCapacity().getTotalSize() == null
                        || ownedNep.getValue().getAvailableCapacity().getTotalSize().getValue() == null
                        || !(ownedNep.getValue().getAvailableCapacity().getTotalSize().getValue().doubleValue() > 0)) {
                    // The DSR Nep is not eligible since it has already a service mapped on it with no available capa
                    continue;
                }
                var clientNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                direction = calculateDirection(ownedNep.getValue(), null, TpType.CLIENT);
                clientNep.setDirection(direction.keySet().iterator().next());
                clientNep.setTpType(direction.values().iterator().next());
                clientNep.setLpn(LayerProtocolName.DSR);
                clientNep.setLpq(ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                    .findFirst().orElseThrow().getLayerProtocolQualifier());
                direction.clear();
                clientNep.setOperationalState(ownedNep.getValue().getOperationalState());
                clientNep.setAdminState(ownedNep.getValue().getAdministrativeState());
                clientNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                clientDsrNep.add(clientNep);
            } else if (LayerProtocolName.DIGITALOTN.equals(ownedNep.getValue().getLayerProtocolName())
                    || LayerProtocolName.ODU.equals(ownedNep.getValue().getLayerProtocolName())) {
                // For eODU NEP we fill dsrNepWithParentOdu Map to fill at a later step the vertically connected nep
                LOG.info("TON Line 1671 : Handling NEP {} ", ownedNep.getValue().getName());
                Uuid clientNepUuid = null;
                String nepType = "";
                ConnectionEndPoint refCep = null;
                if (ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class) != null
                        && ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList() != null
                        && !ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList()
                            .getConnectionEndPoint().isEmpty()) {
                    for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep.getValue()
                            .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint().entrySet()) {
                        refCep = cep.getValue();
                        if (!(cep.getValue().getClientNodeEdgePoint() == null)
                            && !cep.getValue().getClientNodeEdgePoint().isEmpty()) {
                            clientNepUuid = cep.getValue().getClientNodeEdgePoint().entrySet().iterator().next()
                                .getKey().getNodeEdgePointUuid();
                            break;
                        }
                    }
                }
                final Uuid fclientNepUuid = clientNepUuid;
                if (fclientNepUuid != null && LayerProtocolName.DSR.equals(ownedNepList.entrySet().stream()
                        .filter(onep -> onep.getKey().getUuid().equals(fclientNepUuid)).findFirst().orElseThrow()
                        .getValue().getLayerProtocolName())) {
                    // We are in the case of an eODU NEP
                    this.dsrNepWithParentOdu.put(fclientNepUuid, ownedNep.getKey().getUuid());
                    nepType = "eODU";
                } else {
                    // we are in the case of an iODU or iOTU
                    if (!ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                        .filter(sclpqi -> sclpqi.getLayerProtocolQualifier().equals(OTUTYPEOTU4.VALUE))
                        .collect(Collectors.toList()).isEmpty()
                        || !ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                        .filter(sclpqi -> sclpqi.getLayerProtocolQualifier().equals(OTUTYPEOTUCN.VALUE))
                        .collect(Collectors.toList()).isEmpty()) {
                        nepType = "iOTU";
                    } else {
                        nepType = "iODU";
                    }
                }
                LOG.info("TON Line 1710 : Handling NEP {} of nepType {}", ownedNep.getValue().getName(), nepType);
                // For All OTN Nep we fill the corresponding BasePceNep list
                if (OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())) {
                    if (!nepType.equals("iODU") && (ownedNep.getValue().getAvailableCapacity() == null
                            || ownedNep.getValue().getAvailableCapacity().getTotalSize().getValue() == null
                            || !(ownedNep.getValue().getAvailableCapacity().getTotalSize().getValue()
                                .doubleValue() > 0))) {
                        //The OTN NEP is not eligible since it has already a service mapped on it with no available capa
                        //In the specific case of iODU, available capacity may not be set since at initial creation, and
                        //corresponding BasPceNep shall however be considered.
                        continue;
                    }
                    LOG.info("TON Line 1722 : Adding ODU NEP {} of type {} to corresponding list of base pce node",
                        ownedNep.getValue().getName(), nepType);
                    var otnNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                    direction = calculateDirection(ownedNep.getValue(), null,
                        nepType.equals("eODU") ? TpType.CLIENT : TpType.NW);
                    otnNep.setDirection(direction.keySet().iterator().next());
                    otnNep.setLpn(LayerProtocolName.DIGITALOTN);
                    otnNep.setLpq(ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                        .findFirst().orElseThrow().getLayerProtocolQualifier());
                    otnNep.setTpType(direction.values().iterator().next());
                    direction.clear();
                    otnNep.setOperationalState(ownedNep.getValue().getOperationalState());
                    otnNep.setAdminState(ownedNep.getValue().getAdministrativeState());
                    otnNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));

                    BasePceNep otnCep = null;
                    if (refCep != null) {
                        otnCep  = new BasePceNep(refCep.getUuid(), refCep.getName());
                        otnCep.setParentNep(refCep.getParentNodeEdgePoint().getNodeEdgePointUuid());
                        @Nullable
                        Map<ClientNodeEdgePointKey, ClientNodeEdgePoint> cepClientNepMap = refCep
                            .getClientNodeEdgePoint();
                        if (cepClientNepMap != null) {
                            otnCep.setClientNep(cepClientNepMap.keySet().iterator().next().getNodeEdgePointUuid());
                        }
                        otnCep.setLpn(LayerProtocolName.DIGITALOTN);
                        otnCep.setLpq(refCep.getLayerProtocolQualifier());
                        otnCep.setOperationalState(refCep.getOperationalState());
                        // No admin state for cep, set to default UNLOCKED
                        otnCep.setAdminState(AdministrativeState.UNLOCKED);
                        direction = calculateDirection(null, refCep,
                            nepType.equals("eODU") ? TpType.CLIENT : TpType.NW);
                        otnCep.setDirection(direction.keySet().iterator().next());
                        otnCep.setTpType(direction.values().iterator().next());
                        direction.clear();
                    }

                    if (nepType.equals("iOTU")) {
                        otuLcpList.add(otnNep);
                        if (refCep != null && otnCep != null) {
                            otuLcpList.add(otnCep);
                        }
                    } else {
                        oduLcpList.add(otnNep);
                        if (refCep != null && otnCep != null) {
                            oduLcpList.add(otnCep);
                        }
                        if (nepType.equals("eODU") || (nepType.equals("iODU"))) {
                            clientDsrNep.add(otnNep);
                            //clientDsrNep.add(otnCep);
                        }
                    }
                }
            }
        }
        LOG.debug("TONLine1777 clientDsrNep {}", clientDsrNep.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.debug("TONLine1780 nwOtsNep {}", nwOtsNep.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.debug("TONLine1783 otsLcpNWList {}", otsLcpNWList.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.debug("TONLine1786 oduLcpList {}", oduLcpList.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.debug("TONLine1789 otuLcpList {}", otuLcpList.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.debug("TONLine1792 invalidNwNepList {}", invalidNwNepList);

        // Purge otsLcpNwList and nwOTsNEP list since NEP associated to OTS and
        // OTSI_MC may not appear in the expected order. We remove port that have a client in the invalidNwNepList, only
        // if the origin or destination port are network port : we keep Nw ports that are part of the path for services
        // of lower granularity
        for (Uuid nepUuid : invalidNwNepList) {
            if ((aaPortId != null && zzPortId != null)
                    && invalidNwNepList.contains(aaPortId) || invalidNwNepList.contains(zzPortId)) {
                otsLcpNWList = otsLcpNWList.stream()
                    .filter(bpn -> !(bpn.getClientNep().equals(nepUuid)) && isValidBpn(bpn)
                            && !bpn.getNepCepUuid().equals(nepUuid))
                    .distinct()
                    .collect(Collectors.toList());
                nwOtsNep = nwOtsNep.stream()
                    .filter(bpn -> !(bpn.getClientNep().equals(nepUuid)) && isValidBpn(bpn)
                        && !bpn.getNepCepUuid().equals(nepUuid))
                    .distinct()
                    .collect(Collectors.toList());
            }
        }

        LOG.debug("TONLine 1814 : call populateBpnVerticalNep");
        populateBpnVerticalNep();
        // Having populated the list of vertically connected NEPs, we can populate the list of NRG that may define
        // the connections between NEPs at a specific level, and that will be propagated to the NEP of other levels
        // that are vertically connected to the NEPs involved in NRGs

        populateBpnNrgForXpdr();
        LOG.debug("TONLine 1821 : call populateBpnNrgForXpdr Nrg List {}", nwOtsNep.stream()
            .map(BasePceNep::getNodeRuleGroupUuid).collect(Collectors.toList()));
        // Having propagated the list of NRGs to vertically connected NEPs, we can populate the list of NRG that may
        // define the connections between NEPs of client and network side to indirectNRGs of bpn, scanning IRGs.
        LOG.info("TONLine 1821 : call populateBpnVerticalNep");
        populateBpnIndirectNrgForXpdr();
        LOG.debug("TONLine 1827 : call populateBpnNrgForXpdr Nrg List {}", nwOtsNep.stream()
            .map(BasePceNep::getindirectNrgUuid).collect(Collectors.toList()));
        String portType = "nw";
        if (aaPortId == null || zzPortId == null) {
            LOG.debug("InitTapiXndr launched for Node of Id {}, name {} with no specified port imposed for a/z end,"
                + "all valid NW ports validated", node.getUuid().toString(), node.getName().toString());
            return;
        }

        LOG.debug("TONLINE1836 : clientDsrNep includes : {}", clientDsrNep.stream().map(bpn -> bpn.getName())
            .collect(Collectors.toList()));
        Uuid portId = null;
        if (!nwOtsNep.stream().filter(bpn -> isValidBpn(bpn)).findAny().isEmpty()) {
            portId = nwOtsNep.stream()
                .filter(bpn -> isValidBpn(bpn)).findFirst().orElseThrow().getNepCepUuid();
            final Uuid finalPortId = portId;
            nwOtsNep.removeAll(nwOtsNep.stream()
                .filter(bpn -> (!isValidBpn(bpn) || (!bpn.getVerticallyConnectedNep().contains(finalPortId))))
                .collect(Collectors.toList()));
        } else if (!clientDsrNep.stream().filter(bpn -> isValidBpn(bpn)).findFirst().isEmpty()) {
            BasePceNep port = clientDsrNep.stream().filter(bpn -> isValidBpn(bpn)).findFirst().orElseThrow();
            portId = port.getNepCepUuid();
            LOG.debug("TONLINE1849 : clientDsrNep includes : {}", clientDsrNep.stream().map(bpn -> bpn.getName())
                .collect(Collectors.toList()));
            portType = "client";
            LOG.debug("TONLine1852 InitTapiXndr launched for Node of Id {}, name {}, {} port {} validated for a/z end."
                + "Calling purgeXndrPortList to remove all NW ports that are not good candidates (not connected"
                + "to the client port of the service request",
                node.getUuid().toString(), node.getName().toString(), portType, portId);

            List<Uuid> portNrgList = port.getNodeRuleGroupUuid();
            portNrgList.addAll(port.getindirectNrgUuid());
            portNrgList.stream().distinct().collect(Collectors.toList());
            List<BasePceNep> nonConnectedNwBpn = new ArrayList<>();
            for (BasePceNep otsNep : nwOtsNep) {
                List<Uuid> nwNepNrgList = otsNep.getNodeRuleGroupUuid();
                nwNepNrgList.addAll(otsNep.getindirectNrgUuid());
                nwNepNrgList.stream().distinct().collect(Collectors.toList());
                LOG.debug("TONLINE1865 : OtsNep nrgs : {}", nwNepNrgList);
                nwNepNrgList.stream().filter(nrgUuid -> portNrgList.contains(nrgUuid)).collect(Collectors.toList());
                if (nwNepNrgList.isEmpty()) {
                    nonConnectedNwBpn.add(otsNep);
                }
            }
            nwOtsNep.removeAll(nonConnectedNwBpn);
            LOG.debug("TONLINE1872 : purged nwOtsNep includes : {}", nwOtsNep.stream().map(bpn -> bpn.getName())
                .collect(Collectors.toList()));

            final Uuid finalPortId = portId;
            LOG.debug("TONLINE1876 : unpurged clientDsrNep includes : {}",
                clientDsrNep.stream().map(bpn -> bpn.getName()).collect(Collectors.toList()));
            clientDsrNep.removeAll(clientDsrNep.stream()
                .filter(bpn -> (!isValidBpn(bpn) || !bpn.getVerticallyConnectedNep().contains(finalPortId)))
                .collect(Collectors.toList()));
            LOG.debug("TONLINE1881 : Purged clientDsrNep includes : {}",clientDsrNep.stream().map(bpn -> bpn.getName())
                .collect(Collectors.toList()));
            LOG.debug("TONLine1883 A/Z is client port, nwOtsNep after purge is {}", nwOtsNep.stream()
                .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
                .collect(Collectors.toList()));
            return;
        } else {
            LOG.error("TONLine1888 InitTapiXndr launched for Node of Id {}, name {}, but no port validated for a/z end",
                node.getUuid().toString(), node.getName().toString());
            clientDsrNep.clear();
            nwOtsNep.clear();
            return;

        }
        // Applies to condition portId found in nwOtsNep, meaning service applies to NW port of the Xponder
        LOG.debug("TONLine1896 InitTapiXndr launched for Node of Id {}, name {}, {} port {} validated for a/z end",
            node.getUuid().toString(), node.getName().toString(), portType, portId);

        return;
    }

    /**
     * Purges the nwOtsNep List removing all network ports that do not have connectivity to the designated client port.
     *   Done, analyzing the content of the Node-rule-groups and the Inter-rule groups.
     *   Options considered for Xponder NodeRuleGroup (NRG) and InterRuleGroup (IRG) layout
     *   Option1 : 1 NRG (client,...., client, NW, ..., NW) with enabled forwarding
     *      Typical configuration of Multi-port Transponder or Muxponder with hair-pining capability between clients,
     *      potentially multiple NW port that can be used for protection, and where any port can connect to any port.
     *   Option2 : Nx NRG (client---NW), with N: Nbre of Tuple associating client and network port}
     *     Typical configuration of Multi-port Transponder or Muxponder with no hairpining capability
     *   Option 3 : NRGi (client,...., client) Forwarding (Hair-pining) or not
     *              NRGk (NW,...,NW) Forwarding (Regen function creating loop between NW ports)or not
     *              IRG (NRGi,....NRGk) forwarding
     *     Typical configuration for Mux and Switch-Ponders
     * @param clientPortId  The clientPortId which corresponds to A or Z end port specified in the service request.
     * @param netOtsNep     A list of BasePceNep that corresponds to Network ports.
     * @return              Returns pruned netOtsNep NW port list where all port that are not in direct (NRG) or
     *                      indirect (IRG) visibility of the client port have been removed from the list.
     */
    public List<BasePceNep> purgeXndrPortList(Uuid clientPortId, List<BasePceNep> netOtsNep) {
        // nrgofClientNepList will contains Uuid of all NRG containing the clientPortId but no Network port
        List<Uuid> nrgofClientNepList = new ArrayList<>();
        // totalNwOtsNepIdList will contain all NW port key of the NW ports that appeared in a Nep List of a NRG with
        // the clientPortId
        List<NodeEdgePointKey> totalNwOtsNepIdList = new ArrayList<>();
        // nwOtsNepIdList intermediate list corresponding to a NRG used to build totalNwOtsNepIdList
        List<NodeEdgePointKey> nwOtsNepIdList = new ArrayList<>();
        // nrgOfNwNepMap will contain the Uuid of NRGs containing NW ports that are not associated to clientPortId and
        // the list of corresponding NW ports
        Map<Uuid, List<NodeEdgePointKey>> nrgOfNwNepMap = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nrgMap = this.node.getNodeRuleGroup();
        List<Uuid> nwOtsNepKeyList = netOtsNep.stream().map(BasePceNep::getNepCepUuid).distinct()
            .collect(Collectors.toList());
        for (Map.Entry<NodeRuleGroupKey, NodeRuleGroup> nrg : nrgMap.entrySet()) {
            // For each NodeRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
            // and if this is the case store its key in fwRuleKeyList
            List<RuleKey> fwdRuleKeyList =
                nrg.getValue().getRule().entrySet().stream()
                    .filter(rule -> rule.getValue().getRuleType().contains(RuleType.FORWARDING))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Node Rule Group (NRG) does not contain any forwarding
                // rule, we go to next NRG
                continue;
            } else {
                // We have one or several forwarding rule(s)
                if (nrg.getValue().getNodeEdgePoint().entrySet().stream()
                    .filter(nep -> nep.getKey().getNodeEdgePointUuid().equals(clientPortId))
                    .findAny().isPresent()) {
                    // if the nep list contains the clientPortId
                    // We create a list<NodeEdgePointKey> which includes Keys of
                    // all NW NEP recorded in the nwOtsNepList
                    nwOtsNepIdList.addAll(nrg.getValue().getNodeEdgePoint().entrySet().stream()
                        .filter(nep -> nwOtsNepKeyList.contains(nep.getKey().getNodeEdgePointUuid()))
                        .map(Map.Entry::getKey).distinct().collect(Collectors.toList()));
                    if (!nwOtsNepIdList.isEmpty()) {
                        // This is the case of Options 1 & 2 where we found the
                        // clientPort associated to NW NEP
                        totalNwOtsNepIdList.addAll(nwOtsNepIdList);
                        continue;
                    } else {
                        // This is the case of Option 3 where the client port is
                        // not associated to any NW ports of the
                        // nwOtsNepList. We record in nrgList the id of the nrg
                        // that includes the clientPortId
                        nrgofClientNepList.add(nrg.getKey().getUuid());
                    }
                } else {
                    // The clientPort is not found in the list of NEP
                    // corresponding to the NRG
                    nrgOfNwNepMap.put(nrg.getKey().getUuid(), nrg.getValue().getNodeEdgePoint().entrySet().stream()
                        .filter(nep -> nwOtsNepKeyList.contains(nep.getKey().getNodeEdgePointUuid()))
                        .map(Map.Entry::getKey).collect(Collectors.toList()));
                }
            }
            // When no forwarding Rule defined go to next NodeRuleGroup
        }
        LOG.debug("TONLine1978 purgeXndrPortList: nrgList (Client, no nw ports) contains {}", nrgofClientNepList);
        LOG.debug("TONLine1979 purgeXndrPortList: nrgOfNwNepMap (NW, no Client) contains {}", nrgOfNwNepMap);
        LOG.debug("TONLine1980 purgeXndrPortList: totalNwOtsNepIdList (NW & Client) contains {}", totalNwOtsNepIdList);
        // All NRGs have been analyzed
        if (!totalNwOtsNepIdList.isEmpty()) {
            // We found some NW ports associated with the client port in one or several NRG
            List<Uuid> totalNwOtsNepUuidList = totalNwOtsNepIdList.stream()
                .map(NodeEdgePointKey::getNodeEdgePointUuid).distinct()
                .collect(Collectors.toList());
            LOG.debug("TONLine1987 purgeXndrPortList: List(UUID) of NW Port connected to ClientPortId {}",
                totalNwOtsNepUuidList);
            netOtsNep = netOtsNep.stream()
                .filter(bpn -> totalNwOtsNepUuidList.contains(bpn.getNepCepUuid()))
                .collect(Collectors.toList());
            // function exit for option 1 & 2 : nwOtsNep contains only relevant
            // NW NEP that are associated to relevant
            // client port
            return netOtsNep;
        }
        // Did not succeed to find client port and NW ports in the same NRG,
        // need to process Inter Rule Group
        LOG.debug("TONLine1999 purgeXndrPortList: did not succeed to find client port and NW ports in the same NRG");
        Map<InterRuleGroupKey, InterRuleGroup> irgMap = this.node.getInterRuleGroup();
        for (Map.Entry<InterRuleGroupKey, InterRuleGroup> irg : irgMap.entrySet()) {
            // For each NodeRuleGroup [uuid], we check if some of the rule
            // [local-id] are a forwarding rule
            // and if this is the case store its key in fwRuleKeyList
            List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.RuleKey>
                    fwdRuleKeyList = irg.getValue().getRule().entrySet().stream()
                        .filter(rule -> rule.getValue().getRuleType().contains(RuleType.FORWARDING))
                        .map(Map.Entry::getKey).collect(Collectors.toList());
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Inter Rule Group (IRG) does not contain any forwarding
                // rule, we go to next IRG
                continue;
            } else {
                // If we have one or several forwarding rule(s)
                for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.RuleKey
                        rk : fwdRuleKeyList) {
                    if (FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE
                        .equals(irg.getValue().getRule().get(rk).getForwardingRule())
                        || FORWARDINGRULEMUSTFORWARDACROSSGROUP.VALUE
                            .equals(irg.getValue().getRule().get(rk).getForwardingRule())) {
                        Map<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> anrgMap = irg.getValue()
                            .getAssociatedNodeRuleGroup();
                        for (Uuid uuid : nrgofClientNepList) {
                            if (anrgMap.keySet().stream()
                                .filter(anrgK -> uuid.equals(anrgK.getNodeRuleGroupUuid()))
                                .findAny() != null) {
                                for (Map.Entry<Uuid, List<NodeEdgePointKey>> nrg : nrgOfNwNepMap.entrySet()) {
                                    if (anrgMap.keySet().stream()
                                        .filter(anrgK -> nrg.getKey().equals(anrgK.getNodeRuleGroupUuid()))
                                        .findAny() != null) {
                                        totalNwOtsNepIdList.addAll(nrg.getValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // We found some NW ports associated with the client port several NRG to which refer one or several IRG
        netOtsNep = netOtsNep.stream()
            .filter(bpn -> totalNwOtsNepIdList.stream()
                .filter(nepkey -> nepkey.getNodeEdgePointUuid().equals(bpn.getNepCepUuid())).findAny() != null)
            .distinct()
            .collect(Collectors.toList());
        // function exit for option 3 : nwOtsNep contains only relevant NW NEP that are associated to relevant
        // client port
        return netOtsNep;

    }

    /**
     * Instantiate PceTapiLinks for each internal links connecting disaggregated elements of a ROADM.
     *   From internalLinkMap, populates pceInternalLinkMap with PceTapiLink corresponding to ADD, DROP and BYPASS
     *   links that connect SRGs and Degrees (according to OpenROADM representation of ROADMs).
     */
    private void createLinksFromIlMap() {
        List<Map<Uuid, Name>> intLinkList = internalLinkMap.entrySet().stream()
            .map(Map.Entry<Uuid, IntLinkObj>::getValue)
            .collect(Collectors.toList())
                .stream()
                .map(IntLinkObj::getLinkId).collect(Collectors.toList());
        for (Map<Uuid, Name> map : intLinkList) {
            LOG.debug("TONLine2064 For Node {}, Link to be configure = {}",nodeName, map.values().toString());
        }

        for (Map.Entry<Uuid, IntLinkObj> linkTBC : internalLinkMap.entrySet()) {
            LOG.debug("TONLine2068 Link to be configure = {}",
                linkTBC.getValue().getLinkUuid().entrySet().iterator().next().getValue());
            LOG.debug("TONLine2070 bindingVNepToSubnodeMap = {}", bindingVNepToSubnodeMap);
            LOG.debug("TONLine2071 Link to be configure ORG = {} DEST = {}",
                linkTBC.getValue().getOrgTpUuid(), linkTBC.getValue().getDestTpUuid());
            LOG.debug("TONLine2073 Link to be configure PceNodeORG = {} PceNodeDEST = {}",
                bindingVNepToSubnodeMap.entrySet().stream()
                .filter(vts -> vts.getKey()
                    .equals(linkTBC.getValue().getDestTpUuid())).findFirst().orElseThrow().getValue(),
                bindingVNepToSubnodeMap.entrySet().stream()
                .filter(vts -> vts.getKey()
                    .equals(linkTBC.getValue().getOrgTpUuid())).findFirst().orElseThrow().getValue());
            this.pceInternalLinkMap.put(linkTBC.getKey(),
                new PceTapiLink(linkTBC.getValue().getLinkUuid().entrySet().iterator().next().getValue(),
                    linkTBC.getKey(), linkTBC.getValue().getOrgTpUuid(), linkTBC.getValue().getDestTpUuid(),
                    pceNodeMap.get(bindingVNepToSubnodeMap.entrySet().stream()
                        .filter(vts -> vts.getKey()
                            .equals(linkTBC.getValue().getOrgTpUuid())).findFirst().orElseThrow().getValue()),
                    pceNodeMap.get(bindingVNepToSubnodeMap.entrySet().stream()
                        .filter(vts -> vts.getKey()
                            .equals(linkTBC.getValue().getDestTpUuid())).findFirst().orElseThrow().getValue())));
        }
        LOG.debug("TONLine2090 pceInternalLinkMap = {} ",
            pceInternalLinkMap.values().stream().map(PceTapiLink::getLinkName).collect(Collectors.toList()));
    }

    /**
     * Convert Frequency to slot number according to IETF FlexGrid representation (RFC 7698).
     * @param frequency     Frequency associated to an optical channel expressed in Hz (T-API)
     * @return      the adjacent upper Slot Number in a FlexGrid Slot Number = N + 285,
     *              N being an integer varying from -284 to 484 for C band as defined by IETF.
     */
    private int convertFreqToAdjacentSlotNumber(long frequency) {
        return (int) Math.round((frequency / GridConstant.HZ_TO_THZ - GridConstant.ANCHOR_FREQUENCY)
            / (GridConstant.GRANULARITY / 1000.0) + 285);
    }

    /**
     * Sets a Binary corresponding to the spectrum occupancy, from NEP available spectrum attribute.
     * @param onepUuid  Uuid of the Node Edge Point supporting WDM channel
     * @return          A Binary corresponding to the spectrum occupancy according to IETF RFC 7698
     */
    private BitSet buildBitsetFromSpectrum(Uuid onepUuid) {
        // At init, sets all bits to false (unavailable/0) from 0 inclusive to highest index exclusive))
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        // to set all bits to 0 (used) or 1 (available)
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, false);
        // scans all spectrum portions defined in the available spectrum of the
        OwnedNodeEdgePoint ooNep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> onepUuid.equals(onep.getKey().getUuid())).findFirst().orElseThrow().getValue();
        Map<AvailableSpectrumKey, AvailableSpectrum> avSpectrum = ooNep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1.class)
            .getPhotonicMediaNodeEdgePointSpec().getSpectrumCapabilityPac().nonnullAvailableSpectrum();
        if (!(avSpectrum == null || avSpectrum.isEmpty())) {
            for (Map.Entry<AvailableSpectrumKey, AvailableSpectrum> as : avSpectrum.entrySet()) {
                int lowSlotNumber = convertFreqToAdjacentSlotNumber(as.getValue().getLowerFrequency().longValue());
                int highSlotNumber = convertFreqToAdjacentSlotNumber(as.getValue().getUpperFrequency().longValue());
                if (!(lowSlotNumber > GridConstant.EFFECTIVE_BITS || highSlotNumber <= 1)) {
                    // If the one of the boundaries is included in C band, this is a spectrum portion of interest
                    // Set all bits of available spectrum to true (1) from lower slot Number starting at 0 inclusive
                    // to highest index exclusive
                    freqBitSet.set(java.lang.Math.max(0, lowSlotNumber - 1),
                        java.lang.Math.min(highSlotNumber + 1, GridConstant.EFFECTIVE_BITS));
                }
            }
        }
        LOG.debug("TONLine 2134 : FreqBitset for ONEP {} calculated to {}", ooNep.getName(), freqBitSet);
        return freqBitSet;
    }

    /**
     * Check that used spectrum is empty or null which is the condition of availability for a PP.
     * @param otsNepUuid    Uuid of the NodeEdgePoint to be checked (a ROADM SRG PP).
     * @return  Returns true if no wavelength has been provisioned on the PP, false if the PP already support a channel.
     */
    private boolean checkPPOtsNepAvailable(Uuid otsNepUuid) {
        //TODO: complete the implemention of this method if something specific to PP tps handling comes out.
        return checkUnUsedSpectrum(otsNepUuid);
        //return true;
    }

    /**
     * Check that the NEP characteristics allow its validation.
     * @param   nepUuid Uuid of the NEP to check.
     * @return  true when NEP is relevant in the context.
     */
    private boolean isNepWithGoodCapabilities(Uuid nepUuid) {
        //Currently, the relevance of NEP capabilities is checked locally according to the type of NEP
        //TODO: If it appears that some generic charateristics associated to a NEP can be identified for NEP acceptance
        //or rejection, develop this method accordingly.
        return true;
    }

    /**
     * Retrieves port Operational mode from the standard/souce/sink-Profiles.
     * @param nepUuid   Uuid of the NEP associated to the network port.
     * @return First    Operational Mode that includes the rate of the service in its name, "UNKNOWN_MODE" if no
     *                  operational mode is populated in any of the NEPs' profiles.
     */
    public String getXpdrOperationalMode(Uuid nepUuid) {
        List<String> supportedOM = new ArrayList<>();
        OwnedNodeEdgePoint nep = node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> onep.getKey().getUuid().equals(nepUuid)).findFirst().orElseThrow().getValue();
        if (nep.getProfile() != null && !nep.getProfile().isEmpty()) {
            supportedOM.addAll(nep.getProfile().entrySet()
                .stream().map(Map.Entry::getKey).collect(Collectors.toList())
                .stream().map(ProfileKey::getProfileUuid).collect(Collectors.toList())
                .stream().map(Uuid::getValue).collect(Collectors.toList()));
        } else if (nep.getSourceProfile() != null && !nep.getSourceProfile().isEmpty()) {
            supportedOM.addAll(nep.getSourceProfile().entrySet()
                .stream().map(Map.Entry::getKey).collect(Collectors.toList())
                .stream().map(SourceProfileKey::getProfileUuid).collect(Collectors.toList())
                .stream().map(Uuid::getValue).collect(Collectors.toList()));
        } else if (nep.getSinkProfile() != null && !nep.getSinkProfile().isEmpty()) {
            supportedOM.addAll(nep.getSinkProfile().entrySet()
                .stream().map(Map.Entry::getKey).collect(Collectors.toList())
                .stream().map(SinkProfileKey::getProfileUuid).collect(Collectors.toList())
                .stream().map(Uuid::getValue).collect(Collectors.toList()));
        }
        if (supportedOM == null || supportedOM.isEmpty()) {
            LOG.warn("getOperationalMode: NetworkPort {} of Node {}  with Uuid {} has no operational mode declared ",
                nepUuid, this.nodeName, this.nodeUuid);
            return StringConstants.UNKNOWN_MODE;
        }
        for (String operationalMode : supportedOM) {
            if (operationalMode.contains(StringConstants.SERVICE_TYPE_RATE
                .get(this.serviceType).toCanonicalString())) {
                LOG.debug("TONLine2185: NetworkPort {} of Node {}  with Uuid {}  has {} operational mode declared",
                    nepUuid, this.nodeName, this.nodeUuid, operationalMode);
                return operationalMode;
            }
        }
        LOG.warn("getOperationalMode: NetworkPort {} of Node {}  with Uuid {} has no operational mode declared"
            + "compatible with service type {}. Supported modes are : {} ",
            nepUuid, this.nodeName, this.nodeUuid, this.serviceType, supportedOM.toString());
        return StringConstants.UNKNOWN_MODE;
    }

    /**
     * Checks that there are some availabilities in the spectrum of a NEP.
     *   Scans spectrum portions defined in the available spectrum and return true if a spectrum portion is found
     *   with a width that is greater than 50GHz.
     * @param lcpUuid   Uuid of the NEP
     * @return          True if a spectrum portion wider or equal to 50 GHz was found, false if this is not the case.
     */
    private boolean checkAvailableSpectrum(Uuid lcpUuid) {
        OwnedNodeEdgePoint ooNep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> lcpUuid.equals(onep.getKey().getUuid())).findFirst().orElseThrow().getValue();
        LOG.debug("Analysing LCP of NEP {}", ooNep.getName());
        if (ooNep.augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                .OwnedNodeEdgePoint1.class) == null) {
            LOG.debug("TONline2209: checkAvailableSpectrum: noAugmentationONEP1 for nep {}", ooNep.getName());
            return false;
        }
        Map<AvailableSpectrumKey, AvailableSpectrum> aaSpectrum = ooNep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1.class)
            .getPhotonicMediaNodeEdgePointSpec().getSpectrumCapabilityPac().nonnullAvailableSpectrum();
        if (!(aaSpectrum == null || aaSpectrum.isEmpty())) {
            for (Map.Entry<AvailableSpectrumKey, AvailableSpectrum> as : aaSpectrum.entrySet()) {
                long lowFreqIndex = convertFreqToAdjacentSlotNumber(as.getValue().getLowerFrequency().longValue());
                long highFreqIndex = convertFreqToAdjacentSlotNumber(as.getValue().getUpperFrequency().longValue());
                if (!(lowFreqIndex > GridConstant.EFFECTIVE_BITS || highFreqIndex <= 1)) {
                    // If both boundaries is included in C band, and corresponding width is greater than or equal to 50
                    // GHz this is a spectrum portion of interest
                    if (highFreqIndex - lowFreqIndex >= 8) {
                        LOG.debug("TONline2223: checkAvailSpectrum: nep {} has available spectrum", ooNep.getName());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks that a port is not already used looking at NEP used spectrum.
     *   Checks whether a port is used or not checking for the presence of a PhotonicMediaNodeEdgePointSpec and an
     *   occupied spectrum.
     * @param lcpUuid   Uuid of the NEP
     * @return          True if the port has a occupied spectrum, false if this is not the case.
     */
    private boolean checkUnUsedSpectrum(Uuid lcpUuid) {
        OwnedNodeEdgePoint ooNep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> lcpUuid.equals(onep.getKey().getUuid())).findFirst().orElseThrow().getValue();
        if (ooNep.augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                .OwnedNodeEdgePoint1.class) == null
                || ooNep.augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                    .OwnedNodeEdgePoint1.class).getPhotonicMediaNodeEdgePointSpec() == null
                || ooNep.augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                    .OwnedNodeEdgePoint1.class).getPhotonicMediaNodeEdgePointSpec()
                    .getSpectrumCapabilityPac() == null) {
            LOG.debug("TONline2249: checkUnUsedSpectrum: no PhotMediaNEPSpec for nep {}", ooNep.getName());
            return true;
        }
        Map<OccupiedSpectrumKey, OccupiedSpectrum> ooSpectrum = ooNep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1.class)
            .getPhotonicMediaNodeEdgePointSpec().getSpectrumCapabilityPac().nonnullOccupiedSpectrum();
        if (ooSpectrum == null || ooSpectrum.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Populates BasePceNep VerticallyConnectedNep attribute, so that BPNs own a list of other NEPs on the same port.
     *  NEPs have their own LayerProtocolName (Photonic/OTN/DSR) but the LayerProtocolQualifier information (OTS,
     *  OTSiMC, OTU, ODU, DSR, ...) is carried by CEPs which link NEPS the one to  the other.
     *  Thus, parent and client relationship is also carried by CEPs.
     *  In order to simplify some of required processings and because NRGs and IRGs are defined for NEPs that share the
     *  same specific sub-layer (associated with a LayerProtocolQualifier) we populate for each BPN a list of NEPs
     *  that are supported by the same port and stacked in the different sub-layers. Having this information helps to
     *  limit the adherence to the equipment manufacturer implementation.
     */
    private void populateBpnVerticalNep() {
        // For all Bpn of nwOtsNep List, we populate the list of vertically connected NEPs
        LOG.debug("TONLine 2273 : populateBpnVerticalNep");
        for (BasePceNep bpn : nwOtsNep) {
            List<Uuid> nepUuidList = new ArrayList<>();
            nepUuidList.add(bpn.getNepCepUuid());
            bpn.setVerticallyConnectedNep(getVerticallyConnectedNep(bpn.getNepCepUuid(), nepUuidList)
                .stream().distinct().collect(Collectors.toList()));
            LOG.info("TONline2279: List of vertical Neps for bpn {} includes {} elements", bpn.getName(),
                nepUuidList.size());
            LOG.info("TONline2281: List of vertical Neps for bpn {} includes {} elements", bpn.getName(),
                nepUuidList);
        }
        // For all Bpn of clientDsrNep List, we populate the list of vertically connected NEPs
        LOG.debug("TONLine 2285 : populateBpnVerticalNep dsrNepWithParentOdu contains {}", dsrNepWithParentOdu);
        for (BasePceNep bpn : clientDsrNep) {
            List<Uuid> nepUuidList = new ArrayList<>();
            // client DRS MAp may contain both DSR and eODU NEPs
            // Add to VerticallyConnectedNeps of DSR bpns the eODU associated NEP
            if (this.dsrNepWithParentOdu.containsKey(bpn.getNepCepUuid())) {
                nepUuidList.add(this.dsrNepWithParentOdu.entrySet().stream()
                    .filter(uuidCouple -> uuidCouple.getKey().equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
                    .getValue());
                bpn.setVerticallyConnectedNep(nepUuidList.stream().distinct().collect(Collectors.toList()));
                LOG.debug("TONLine 2295 : set ConnectedNep {} for Nep DSR {}",
                    nepUuidList.stream().distinct().collect(Collectors.toList()), bpn.getNepCepUuid());
            }
         // Add to VerticallyConnectedNeps of eODU bpns the DSR associated NEP
            if (this.dsrNepWithParentOdu.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList())
                    .contains(bpn.getNepCepUuid())) {
                nepUuidList.add(this.dsrNepWithParentOdu.entrySet().stream()
                    .filter(uuidCouple -> uuidCouple.getValue().equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
                    .getKey());
                bpn.setVerticallyConnectedNep(nepUuidList.stream().distinct().collect(Collectors.toList()));
                LOG.debug("TONLine 2305 : set ConnectedNep {} for Nep eODU {}",
                    nepUuidList.stream().distinct().collect(Collectors.toList()), bpn.getNepCepUuid());
            }
            LOG.debug("TONline2308: List of vertical Neps for bpn {} includes {} elements", bpn.getName(),
                nepUuidList.size());
        }
        for (BasePceNep bpn : oduLcpList) {
            List<Uuid> nwNepUuidList = new ArrayList<>();
            for (BasePceNep bpnNw : nwOtsNep) {
                if (bpnNw.getVerticallyConnectedNep().contains(bpn.getNepCepUuid())) {
                    nwNepUuidList.addAll(bpnNw.getVerticallyConnectedNep());
                }
            }
            nwNepUuidList.stream().distinct().collect(Collectors.toList());
            bpn.setVerticallyConnectedNep(nwNepUuidList);
            LOG.debug("TONline2320: List of vertical Neps for bpn {} includes {} elements", bpn.getName(),
                nwNepUuidList.size());
            LOG.debug("TONline2322: List of vertical Neps for bpn {} includes {} elements", bpn.getName(),
                nwNepUuidList);
        }
    }

    /**
     * Populates BasePceNep NodeRuleGroupUuid attribute (list of NRGs' Uuid the NEP/CEP is associated to).
     *   Scans the Node NRGs and build Maps of NEPs that belongs to the same NRG, with NRG Uuid as the key.
     *   From this Map, sets NodeRuleGroupUuid attribute.
     *   Using the list of vertically connected NEPs, propagates this list of NRGs' Uuid to NEPs/CEPs that share the
     *   same port, but are not directly included in the NRG's list of NEP.
     *   At the end the NodeRuleGroupUuid list will include the Uuids of all the NRGs the NEP/CEP directly/indirectly
     *   belongs to.
     */
    private void populateBpnNrgForXpdr() {
        Map<Uuid, List<Uuid>> nrgNepMap = new HashMap<>();
        for (NodeRuleGroup nrg : node.getNodeRuleGroup().values()) {
            List<Uuid> nepList = new ArrayList<>();
            nepList.addAll(nrg.getNodeEdgePoint().values().stream()
                .map(NodeEdgePoint::getNodeEdgePointUuid).collect(Collectors.toList()));
            nrgNepMap.put(nrg.getUuid(), nepList);
        }
        LOG.debug("TONLine 2344 : populateBpnNrgForXpdr");
        for (Map.Entry<Uuid, List<Uuid>> mapentry : nrgNepMap.entrySet()) {
            for (BasePceNep bpn : nwOtsNep) {
                List<Uuid> vertNepsUuidList = bpn.getVerticallyConnectedNep();
                vertNepsUuidList.add(bpn.getNepCepUuid());
                for (Uuid nepUuid : vertNepsUuidList) {
                    if (mapentry.getValue().contains(nepUuid)) {
                        bpn.setNodeRuleGroupUuid(mapentry.getKey());
                        LOG.debug("TONLine 2352 : populate Nrg For Nw nep {} with NRG Uuid = {}", bpn.getName(),
                            mapentry.getKey());
                        break;
                    }
                }
            }
            for (BasePceNep bpn : clientDsrNep) {
                List<Uuid> vertNepsUuidList = bpn.getVerticallyConnectedNep();
                vertNepsUuidList.add(bpn.getNepCepUuid());
                for (Uuid nepUuid : vertNepsUuidList) {
                    if (mapentry.getValue().contains(nepUuid)) {
                        bpn.setNodeRuleGroupUuid(mapentry.getKey());
                        LOG.debug("TONLine 2364 : populate Nrg For client nep {} with NRG Uuid = {}", bpn.getName(),
                            mapentry.getKey());
                        break;
                    }
                }
            }
            for (BasePceNep bpn : oduLcpList) {
                List<Uuid> vertNepsUuidList = new ArrayList<>(List.of(bpn.getNepCepUuid()));
                vertNepsUuidList.addAll(bpn.getVerticallyConnectedNep());
                for (Uuid nepUuid : vertNepsUuidList) {
                    if (mapentry.getValue().contains(nepUuid)) {
                        bpn.setNodeRuleGroupUuid(mapentry.getKey());
                        LOG.debug("TONLine 2376 : populate Nrg For ODU nep {} with NRG Uuid = {}", bpn.getName(),
                            mapentry.getKey());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Populates BasePceNep indirectNRGUuid attribute (list of NRGs' Uuid the NEP/CEP is associated to through an IRG).
     *   Scans the Node InterRuleGroups and build a Map of List(NRG-Uuid) that the IRG interconnects together with IRG
     *   Uuid as the key. From this Map, sets indirectNRGUuid attribute of BPNs in nwOtsNep, clienDsrNep and oduLcpList.
     *   This is done, checking in NodeRuleGroupUuid attribute of BasePceNep, if the NRG's Uuid are associated with a
     *   specific IRG.
     *   At the end the indirectNRGUuid list will include the Uuids of all the IRGs the NEP/CEP directly/indirectly
     *   belongs to.
     */
    private void populateBpnIndirectNrgForXpdr() {
        Map<Uuid, List<Uuid>> indNrgMap = new HashMap<>();
        if (node.getInterRuleGroup() == null) {
            // This is notably the case for regular transponders
            return;
        }
        for (InterRuleGroup irg : node.getInterRuleGroup().values()) {
            List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.RuleKey>
                fwdRuleKeyList = irg.getRule().entrySet().stream()
                    .filter(rule -> rule.getValue().getRuleType().contains(RuleType.FORWARDING))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Inter Rule Group (IRG) does not contain any forwarding rule, we go to next IRG
                continue;
            } else {
                List<Uuid> nrgList = new ArrayList<>();
                nrgList.addAll(irg.getAssociatedNodeRuleGroup().values().stream()
                    .map(AssociatedNodeRuleGroup::getNodeRuleGroupUuid).collect(Collectors.toList()));
                indNrgMap.put(irg.getUuid(), nrgList);
            }
        }
        LOG.debug("TONLine 2415 : populateBpnIndirectNrgForXpdr");
        for (Map.Entry<Uuid, List<Uuid>> mapentry : indNrgMap.entrySet()) {
            for (BasePceNep bpn : nwOtsNep) {
                List<Uuid> bpnNrgUuid = bpn.getNodeRuleGroupUuid();
                for (Uuid nrgUuid : bpnNrgUuid) {
                    if (mapentry.getValue().contains(nrgUuid)) {
                        bpn.setIndirectNRGUuid(mapentry.getValue());
                    }
                }
            }
            for (BasePceNep bpn : clientDsrNep) {
                List<Uuid> bpnNrgUuid = bpn.getNodeRuleGroupUuid();
                for (Uuid nrgUuid : bpnNrgUuid) {
                    if (mapentry.getValue().contains(nrgUuid)) {
                        bpn.setIndirectNRGUuid(mapentry.getValue());
                    }
                }
            }
            for (BasePceNep bpn : oduLcpList) {
                List<Uuid> bpnNrgUuid = bpn.getNodeRuleGroupUuid();
                if (bpnNrgUuid == null || bpnNrgUuid.isEmpty()) {
                    break;
                }
                for (Uuid nrgUuid : bpnNrgUuid) {
                    if (mapentry.getValue().contains(nrgUuid)) {
                        bpn.setIndirectNRGUuid(mapentry.getValue());
                    }
                }
            }
        }
    }

    /**
     * For a specified NEP, completes a list of NEPs that share the same port, using CEP kindred information.
     * @param nepUuid   Uuid of the specified NEP.
     * @param connectedNepUuidList  List of NEP that are direct/indirect parent/child of the specified NEP.
     * @return  completed connectedNepUuidList.
     */
    private List<Uuid> getVerticallyConnectedNep(Uuid nepUuid, List<Uuid> connectedNepUuidList) {
        LOG.debug("TONLine 2454 : populateBpnVerticalNep");
        OwnedNodeEdgePoint onep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(nep -> nep.getKey().getUuid().equals(nepUuid)).findAny().orElseThrow().getValue();
        if (onep.augmentation(OwnedNodeEdgePoint1.class) == null
            || onep.augmentation(OwnedNodeEdgePoint1.class).getCepList() == null
            || onep.augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                .isEmpty()) {
            LOG.debug("TONLine 2461 : populateBpnVerticalNep");
            return connectedNepUuidList;
        } else {
            for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : onep
                    .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                    .entrySet()) {
                Uuid clientNepUuid = null;
                if (!(cep.getValue().getClientNodeEdgePoint() == null)
                        && !cep.getValue().getClientNodeEdgePoint().isEmpty()) {
                    clientNepUuid = cep.getValue().getClientNodeEdgePoint().entrySet()
                        .stream().map(Map.Entry::getValue).collect(Collectors.toList()).stream()
                        .map(ClientNodeEdgePoint::getNodeEdgePointUuid)
                        .collect(Collectors.toList()).iterator().next();
                }
                Uuid parentNepUuid = null;
                if (!(cep.getValue().getParentNodeEdgePoint() == null)) {
                    parentNepUuid = cep.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid();
                }
                if (parentNepUuid != null) {
                    connectedNepUuidList.add(parentNepUuid);
                    connectedNepUuidList.stream().distinct().collect(Collectors.toList());
                }
                if (clientNepUuid != null) {
                    connectedNepUuidList.add(clientNepUuid);
                    connectedNepUuidList.stream().distinct().collect(Collectors.toList());
                    getVerticallyConnectedNep(clientNepUuid, connectedNepUuidList);
                } else {
                    return connectedNepUuidList;
                }
            }
        }
        return connectedNepUuidList;
    }

    /**
     * Creates a Full Mesh between VirtualNEPs considering all combination of 2 NEPs from input Map.
     * @param intercoNepMap     Map of NodeEdgePoint that correspond to virtual NEPs (either CPs, CTPs or TTPs).
     * @return               A dictionary of IntLinkObj, defining internal Link Objects, build from an Id (Uuid, Name),
     *                       and the Uuid of the origin and destination of the internal link.
     */
    private Map<Uuid, IntLinkObj> createIrgPartialMesh(
        Map<Integer, Map<NodeEdgePointKey, NodeEdgePoint>> intercoNepMap) {
        LOG.debug("TONLine2503 InterconnecMap {}", intercoNepMap);
        // without duplicate. Consider that this is a partial mesh, as we create less Links than if we were creating
        // all the links between any combination of NEPs of the 2 maps of intercoNepMap
        Map<NodeEdgePointKey, NodeEdgePoint> nepMap1;
        nepMap1 = intercoNepMap.get(0);
        LOG.debug("TONLine2508 nepMap1 {}", nepMap1);
        Map<NodeEdgePointKey, NodeEdgePoint> nepMap2;
        nepMap2 = intercoNepMap.get(1);
        LOG.debug("TONLine2511 nepMap2 {}", nepMap2);
        LOG.debug("TONLine2512 mmSrgOtsNep {}", mmSrgOtsNep.values().stream()
            .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        LOG.debug("TONLine2514 mmDegOtsNep {}", mmDegOtsNep.values().stream()
            .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        String orgNodeType;
        String destNodeType;
        BasePceNep orgBpn = null;
        Map<Uuid, IntLinkObj> internLinkMap = new HashMap<>();
        List<String> uuidSortedList = new ArrayList<String>();
        for (Map.Entry<NodeEdgePointKey, NodeEdgePoint> nep1 : nepMap1.entrySet()) {
            if (!mmSrgOtsNep.values().stream().filter(bpn -> nep1.getKey().getNodeEdgePointUuid()
                    .equals(bpn.getNepCepUuid())).collect(Collectors.toList()).isEmpty()) {
                orgBpn = mmSrgOtsNep.values().stream().filter(bpn -> nep1.getKey().getNodeEdgePointUuid()
                    .equals(bpn.getNepCepUuid())).findFirst().orElseThrow();
                orgNodeType = "SRG";
            } else if (!mmDegOtsNep.values().stream().filter(bpn -> nep1.getKey().getNodeEdgePointUuid()
                .equals(bpn.getNepCepUuid())).collect(Collectors.toList()).isEmpty()) {
                orgBpn = mmDegOtsNep.values().stream().filter(bpn -> nep1.getKey().getNodeEdgePointUuid()
                    .equals(bpn.getNepCepUuid())).findFirst().orElseThrow();
                orgNodeType = "DEG";
            } else {
                LOG.debug("Nep {} not included in interconnecMap, this nep may not be connected to existing link",
                    nep1.getKey());
                break;
            }
            LOG.debug("TONLine2537 orgNodeType {}", orgNodeType);
            Uuid orgVnepUuid = orgBpn.getVirtualNep().entrySet().iterator().next().getKey();
            String orgVnepName = orgBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
            LOG.debug("TON Line 2540 createIrgPartialMesh, scanning NEP, origin Vnep Name is {}", orgVnepName);

            BasePceNep destBpn = null;
            for (Map.Entry<NodeEdgePointKey, NodeEdgePoint> nep2 : nepMap2.entrySet()) {
                if (!mmSrgOtsNep.values().stream().filter(bpn -> nep2.getKey().getNodeEdgePointUuid()
                        .equals(bpn.getNepCepUuid())).collect(Collectors.toList()).isEmpty()) {
                    destBpn = mmSrgOtsNep.values().stream().filter(bpn -> nep2.getKey().getNodeEdgePointUuid()
                        .equals(bpn.getNepCepUuid())).findFirst().orElseThrow();
                    destNodeType = "SRG";
                } else if (!mmDegOtsNep.values().stream().filter(bpn -> nep2.getKey().getNodeEdgePointUuid()
                    .equals(bpn.getNepCepUuid())).collect(Collectors.toList()).isEmpty()) {
                    destBpn = mmDegOtsNep.values().stream().filter(bpn -> nep2.getKey().getNodeEdgePointUuid()
                        .equals(bpn.getNepCepUuid())).findFirst().orElseThrow();
                    destNodeType = "DEG";
                } else {
                    LOG.debug("Nep {} not included in interconnecMap, this nep may not be connected to existing link",
                        nep2.getKey());
                    break;
                }

                Uuid destVnepUuid = destBpn.getVirtualNep().entrySet().iterator().next().getKey();
                String destVnepName = destBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
                LOG.debug("createIrgPartialMesh, scanning NEP, destinationVnep UUID is {}, destination Vnep Name is {}",
                    destVnepUuid, destVnepName);
                uuidSortedList.clear();
                uuidSortedList.add(orgVnepUuid.toString());
                uuidSortedList.add(destVnepUuid.toString());
                Collections.sort(uuidSortedList);
                if (orgVnepUuid.toString().equals(uuidSortedList.get(0))) {
                    Map<Uuid, Name> linkId = createLinkId(orgVnepName, destVnepName);
                    internLinkMap.put(linkId.entrySet().iterator().next().getKey(),
                        new IntLinkObj(linkId, orgVnepUuid, destVnepUuid));
                    addCpCtpOutgoingLink(orgVnepUuid, destVnepUuid, orgNodeType, destNodeType, linkId);
                } else {
                    Map<Uuid, Name> linkId = createLinkId(destVnepName, orgVnepName);
                    internLinkMap.put(linkId.entrySet().iterator().next().getKey(),
                        new IntLinkObj(linkId, destVnepUuid, orgVnepUuid));
                    addCpCtpOutgoingLink(destVnepUuid, orgVnepUuid, destNodeType, orgNodeType, linkId);
                }
                LOG.debug("TONLine2579 createIrgPartialMesh, scanning NEP, destination Vnep Name is {}", destVnepName);
            }
        }
        LOG.debug("TON Line 2582 createIrgPartialMesh, interlinkMap is {}", internLinkMap.values().stream()
            .map(IntLinkObj::getLinkId).collect(Collectors.toList()));
        return internLinkMap;
    }

    /**
     * Provides a Map of PceTapiOticalNode instantiated when the service is of photonic type.
     * @return   A map of PceTapiOticalNode (Abstracted Node corresponding to a Graph Vertex for path computation in
     *           photonic layer). This Map contains only one Node in the case of Xponders and several Nodes
     *           (Degrees and SRGs) resulting from the disaggregation in the case of a ROADM Node.
     */
    public Map<Uuid, PceTapiOpticalNode> getPceNodeMap() {
        return this.pceNodeMap;
    }

    /**
     * Provides information on whether the node must be considered or not according to the service request.
     *  Validity depends on service type, and whether the node is identified as a service end of the exercised request.
     * @return true/false depending on whether the node is valid or not.
     */
    public Boolean isValid() {
        return this.valid;
    }

    /**
     * Provides node Common Node Type as defined in OpenROADM : RDM, XPDR, ILA, EXTPLUG.
     * @return NodeType as defined in OpenROADM.
     */
    public NodeTypes getCommonNodeType() {
        return this.commonNodeType;
    }

    /**
     * Provides a unique PceTapiOpticalNode instantiated if the TapiOpticalNode is identified as valid Xponder.
     * @return  A PceTapiOpticalNode (Abstracted Node corresponding to a Graph Vertex for path computation in the
     *          photonic layer).
     */
    public PceTapiOpticalNode getXpdrOpticalNode() {
        return this.pceTapiOptNodeXpdr;
    }

    /**
     * Provides a unique PceTapiOtnNode instantiated if the TapiOpticalNode is identified as valid, for OTN services.
     * @return  A PceTapiOtnNode: Abstracted XPONDER Node corresponding to a Graph Vertex for path computation in the
     *          OTN layer.
     */
    public PceTapiOtnNode getXpdrOtnNode() {
        return this.pceTapiOtnNodeXpdr;
    }

    /**
     * Provides a List of OTS BasePceNep (NEPs + CEPs) associated with a Degree (OTS TTPs and CTP).
     * @return  A list of OTS BasePceNep associated with a Degree
     */
    public List<BasePceNep> getDegOtsNep() {
        return this.mmDegOtsNep.values().stream().collect(Collectors.toList());
    }

    /**
     * Provides a List of OMS BasePceNep (NEPs + CEPs) associated with a Degree (OMS TTPs).
     * @return  A list of OMS BasePceNep associated with a Degree
     */
    public List<BasePceNep> getDegOmsNep() {
        return this.degOmsNep.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Provides a List of OTS BasePceNep (NEPs + CEPs) associated with a SRG (OTS PPs and CP).
     * @return  A list of OTS BasePceNep associated with a SRG
     */
    public List<BasePceNep> getSrgOtsNep() {
        return this.mmSrgOtsNep.values().stream().collect(Collectors.toList());
    }

    /**
     * Provides a List of OTS BasePceNep (NEPs + CEPs) associated with a XPONDER network port.
     * @return  A list of OTS BasePceNep associated with a XPONDER (OTS NEPs)
     */
    public List<BasePceNep> getnetOtsNep() {
        return this.nwOtsNep.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Provides a List of BasePceNep (NEPs) associated with a XPONDER.
     * @return  A list of BasePceNep associated with a XPONDER including DSR NEPs, eODU and iODU NEPs and potentially
     *          CEPs (if ODU service already provisioned).
     */
    public List<BasePceNep> getClientDsrNep() {
        return this.clientDsrNep.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Provides a List of ODU BasePceNep (NEPs + CEPs) associated with a XPONDER.
     * @return  A list of ODU BasePceNep associated with a XPONDER, including eODU and iODU NEPs and potentially CEPs
     *          (if ODU service already provisioned).
     */
    public List<BasePceNep> getOduCepAndNep() {
        return this.oduLcpList.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Provides a List of OTU BasePceNep (CEPs) associated with a XPONDER Network port.
     * @return  A list of iOTU BasePceNep associated with a XPONDER.
     */
    public List<BasePceNep> getOtuCepAndNep() {
        return this.otuLcpList.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Provides a Map internal links (IntLinkObj) joining virtual NEPs of disaggregated ROADM nodes.
     * @return  A dictionary of IntLinkObj, defining internal Link Objects, build from an Id (Uuid, Name),
     *          and the Uuid of the origin and destination of the internal link.
     */
    public Map<Uuid,IntLinkObj> getInternalLinkMap() {
        return this.internalLinkMap;
    }

    /**
     * Provides a Map internal links (PceTapiLink) joining virtual NEPs of disaggregated ROADM nodes.
     * @return  A dictionary of PceTapiLink.
     */
    public Map<Uuid, PceTapiLink> getPceInternalLinkMap() {
        return this.pceInternalLinkMap;
    }

    /**
     * Private Class used to aggregate link attributes.
     *   IntLinkObj, defines internal Link Objects, build from an Id (Uuid, Name), and the Uuid of the origin and
     *   destination of the internal link.
     */
    private static final class IntLinkObj {
        private Map<Uuid, Name> linkId;
        private Uuid orgTpUuid;
        private Uuid destTpUuid;

        private IntLinkObj(Map<Uuid, Name> linkIid, Uuid orgTpUuid, Uuid destTpUuid) {
            this.orgTpUuid = orgTpUuid;
            this.destTpUuid = destTpUuid;
            this.linkId = linkIid;
        }

        /**
         * Provides a Uuid corresponding to origin NEP of the Link (Photonic Layer).
         * @return  Origin NEP Uuid.
         */
        private Uuid getOrgTpUuid() {
            return orgTpUuid;
        }

        /**
         * Provides a Uuid corresponding to destination NEP of the Link (Photonic Layer).
         * @return  Destination NEP Uuid.
         */
        private Uuid getDestTpUuid() {
            return destTpUuid;
        }

        /**
         * Provides the LinkId of an internal link (Photonic Layer).
         * @return  a Map of the Link Name using the Link Uuid as a key. The map contains a unique element.
         */
        private Map<Uuid, Name> getLinkId() {
            return linkId;
        }
    }

}
