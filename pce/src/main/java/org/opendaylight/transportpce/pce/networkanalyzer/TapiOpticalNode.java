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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
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
    private Map<Uuid, PceTapiLink> pceInternalLinkMap = new HashMap<>();
    private static final String ROADMNODENAME = "roadm node name";
    private static final String ROADMNODETYPE = "ROADM";
    private static final String XPDRNODENAME = "otsi node name";
    private static final String NODETYPE = "Node Type";


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
            this.valid = false;
        } else {
            this.valid = true;
            this.serviceType = serviceType;
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
            LOG.debug("Node {} admin state is {}, operational state is {}",
                node.getName(), adminState, operationalState);
        }
    }

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

    private void initRoadmTps() {
        // T-API ROADMs are not disaggregated. A ROADM Node will include PPs and TTPs
        if (!this.valid) {
            return;
        }
        // Scan OwnedNEP from the Photonic layer ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances()
        // provides info on OMS/OTS/MC presence
        // NEP with OTS, no OMS are PPS --> if InService and no MC (occupancy)
        // --> put in srgOtsNep
        // NEP with OTS and OMS are TTPs --> In service --> Put them in mmDegOtsNep
        // Relies on checkOtsNepAvailable
        Map<DirectionType, OpenroadmTpType> direction;
        LOG.debug("TONline273: initRoadmIlaTps: getting tps from ROADM node {}", this.nodeName);
        // for each of the photonic OwnedNEP which Operational state is enable
        // and spectrum is not fully used
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNepList = this.node.getOwnedNodeEdgePoint();
        for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNep : ownedNepList.entrySet()) {
            if (!checkAvailableSpectrum(ownedNep.getValue().getUuid(), true)) {
                LOG.debug("TONline279: initRoadmIlaTps: the Onep {} is not identified as available",
                    ownedNep.getValue().getName().entrySet().iterator().next().getValue().getValue());
            }
            if (LayerProtocolName.PHOTONICMEDIA.equals(ownedNep.getValue().getLayerProtocolName())
                && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())
                && checkAvailableSpectrum(ownedNep.getValue().getUuid(), true)) {
                // If the NEP is an OTS NEP, and owns a Cep List
                //WARNING : old comment (not the case of PP NEPs, until they already support a service),
                //WARNING : new comment in fact shall be the case of PPs for which we call populateNep (withSip=true)
                //TODO: correct populate nep so that OTS Cep is created for PP and remove line 459-470
                // scans the list of CEP, adds to otsLcpList each CEP which owns an otsCEPspec
                // and fills allOtsNep with all information it retrieves from the CEP
                if (!ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                        .filter(sclpqi -> PHOTONICLAYERQUALIFIEROTS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                        .findAny().isEmpty()) {
                    var otsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                    if (ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class) == null
                            || ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList() == null) {
                        otsNep.setOperationalState(OperationalState.ENABLED);
                        otsNep.setAdminState(AdministrativeState.UNLOCKED);
                        otsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                        allOtsNep.add(otsNep);
                    } else {
                        for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep.getValue()
                                .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                                .entrySet()) {
                            var otsCep = new BasePceNep(cep.getValue().getUuid(), cep.getValue().getName());
                            //otsCep.setParentNep(cep.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid());
                            otsCep.setParentNep(ownedNep.getKey().getUuid());
                            otsCep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            otsLcpList.add(otsCep);
                            LOG.debug("TONline306: initRoadmIlaTps: OTS Cep added to otsLcpList  {}",
                                otsCep.getName());
                            otsNep.setCepOtsUuid(cep.getValue().getUuid());
                            otsNep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            otsNep.setOperationalState(OperationalState.ENABLED);
                            otsNep.setAdminState(AdministrativeState.UNLOCKED);
                            otsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                            if (cep.getValue().augmentation(ConnectionEndPoint2.class) != null
                                    && cep.getValue().augmentation(ConnectionEndPoint2.class)
                                    .getOtsMediaConnectionEndPointSpec() != null) {
                                otsNep.setCepOtsSpec(cep.getValue().augmentation(ConnectionEndPoint2.class)
                                    .getOtsMediaConnectionEndPointSpec());
                                // check if spectrum shall be populated here
                                //Goes out of the loop as soon it has found a OtsMediaConnectionEndPointSpec
                                break;
                            }
                        }
                        allOtsNep.add(otsNep);
                    }
                    LOG.debug("TONline329: initRoadmIlaTps: OTS nep added to AllOTSNep  {}", otsNep.getName());
                }
                // If the NEP is an OMS NEP, scans the list of CEP, Adds to
                // oMsLcpList each CEP which owns an omsCEPspec
                // and fills degOmsNep with all information it retrieves from
                // the CEP
                if (!ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                    .filter(sclpqi -> PHOTONICLAYERQUALIFIEROMS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                    .findAny().isEmpty()) {

                    LOG.debug("TONline339: initRoadmIlaTps: OMS NEP {} has been found", ownedNep.getValue().getName());
                    var omsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                    if (ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class) == null
                            || ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList() == null) {
                        omsNep.setOperationalState(OperationalState.ENABLED);
                        omsNep.setAdminState(AdministrativeState.UNLOCKED);
                        omsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                        omsNep.setTpType(OpenroadmTpType.DEGREETXRXTTP);
                        direction = calculateDirection(ownedNep.getValue(), null, TpType.TTP);
                        omsNep.setDirection(direction.keySet().iterator().next());
                        omsNep.setTpType(direction.values().iterator().next());
                        direction.clear();
                        degOmsNep.add(omsNep);
                        LOG.debug("TONline351: initRoadmIlaTps: OMS NEP {} NO augment", ownedNep.getValue().getName());
                    } else {
                        LOG.debug("TONline353: initRoadmIlaTps: OMS NEP {} has augment", ownedNep.getValue().getName());
                        for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep2 : ownedNep.getValue()
                                .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                                .entrySet()) {
                            var omsCep = new BasePceNep(cep2.getValue().getUuid(), cep2.getValue().getName());
                            //omsCep.setParentNep(cep2.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid());
                            omsCep.setParentNep(ownedNep.getKey().getUuid());
                            omsCep.setClientNep(cep2.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            omsLcpList.add(omsCep);
                            LOG.debug("TONline370: initRoadmIlaTps: OMS Cep added to omsLcpList  {}",
                                omsCep.getName());
                            omsNep.setCepOmsUuid(cep2.getValue().getUuid());
                            omsNep.setClientNep(cep2.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                .getNodeEdgePointUuid());
                            omsNep.setOperationalState(OperationalState.ENABLED);
                            omsNep.setAdminState(AdministrativeState.UNLOCKED);
                            omsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                            omsNep.setTpType(OpenroadmTpType.DEGREETXRXTTP);
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
                    LOG.debug("TONline386: initRoadmIlaTps: OMS nep added to degOMSNep  {}", omsNep.getName());
                }
            }
        }
        degOmsNep.stream().distinct().collect(Collectors.toList());
        // Loop to Fill mmSrgOtsNep and mmDegOtsNep from allOtsNep, relying on
        // information (parent NEP) present in
        // otsLcpList, and set direction, tpType and spectrum use
        LOG.debug("TONline394: Node {} initRoadmIlaTps: Purged degOmsNepList Nep {}", nodeName,
            degOmsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("TONline396: Node {} initRoadmIlaTps: degOmsNepUuidList Nep {}", nodeName,
            degOmsNep.stream().map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        LOG.debug("TONline398: Node {} initRoadmIlaTps: OtsCepList ClientNeps {}", nodeName,
            otsLcpList.stream().map(BasePceNep::getClientNep).collect(Collectors.toList()));
        boolean isDegreeNep;
        otsLcpList.stream().distinct().collect(Collectors.toList());
        for (BasePceNep otsCep : otsLcpList) {
            isDegreeNep = false;
            LOG.debug("TONline404: Node {} initRoadmIlaTps: scan OtsCepList : Ots Cep {}", nodeName, otsCep.getName());
            LOG.debug("TONline405: initRoadmIlaTps: scan OtsCepList : Ots CepClientNep {}", otsCep.getClientNep());
            for (BasePceNep omsNep : degOmsNep) {
                if (omsNep.getNepCepUuid().equals(otsCep.getClientNep())) {
                    // The OTS NEP is a degree NEP
                    LOG.debug("TONline409: initRoadmIlaTps: identifiedOMS nep {} as client of Ots Cep ",
                        omsNep.getName());
                    omsNep.setParentNep(otsCep.getParentNep());
                    BasePceNep otsNep = allOtsNep.stream().filter(bpn -> otsCep.getParentNep()
                        .equals(bpn.getNepCepUuid())).findFirst().orElseThrow();
                    LOG.debug("TONline414: initRoadmIlaTps: OTS nep {} in mmDegOTsNep has admin state {} ",
                        otsNep.getName(), otsNep.getAdminState());
                    direction = calculateDirection(
                        ownedNepList.entrySet().stream().filter(onep -> otsNep.getNepCepUuid()
                            .equals(onep.getValue().getUuid())).findFirst().orElseThrow()
                            .getValue(), null, TpType.TTP);
                    otsNep.setDirection(direction.keySet().iterator().next());
                    otsNep.setTpType(direction.values().iterator().next());
                    otsNep.setFrequencyBitset(buildBitsetFromSpectrum(otsNep.getNepCepUuid()));
                    otsNep.setCepOtsSpec(otsCep.getCepOtsSpec());
                    mmDegOtsNep.put(otsNep.getNepCepUuid(), otsNep);
                    //degOtsNep.add(otsNep);
                    LOG.debug("TONline426: initRoadmIlaTps: OTS nep from allOTsNep added to mmDegOTSNep  {}",
                        otsNep.getName());
                    isDegreeNep = true;
                    break;
                }
            }
            LOG.debug("TONline432: Node {} initRoadmIlaTps: no identifiedOMS nep as client of Ots Cep {} ",
                nodeName, otsCep.getName());
            if (!isDegreeNep) {
                BasePceNep otsNep = allOtsNep.stream().filter(bpn -> otsCep.getParentNep().equals(bpn.getNepCepUuid()))
                    .findFirst().orElseThrow();
                if (!checkOtsNepAvailable(otsNep.getNepCepUuid())) {
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
                    && checkOtsNepAvailable(bpn.getNepCepUuid())) {
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
        LOG.debug("TONline464: initRoadmIlaTps: mmDegOTSNEP  {}",
            mmDegOtsNep.values().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("TONline467: Node {} initRoadmIlaTps: mmDegOMSNEP  {}", nodeName,
            degOmsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("TONline469: Node {} initRoadmIlaTps: mmSrgOTSNEP  {}", nodeName,
            mmSrgOtsNep.values().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("TONline471: Node {} initRoadmIlaTps: allOtsNEP  {}", nodeName,
            allOtsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
    }

    private void initIlaTps() {
        if (!this.valid) {
            return;
        }
        // Scan Owned NEP from the Photonic layer ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances()
        // Provides info on OTS presence
        // Fill AllOtsNep with OTS Nep (Operational state ENABLED) and
        // OtsLcpList with OtsCep
        // Relies on checkOtsNepAvailable
        LOG.debug("initRoadmIlaTps: getting tps from ILA node {}", this.nodeUuid);
        // for each of the photonic OwnedNEP which Operational state is enable and spectrum is not fully used
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNepList = this.node.getOwnedNodeEdgePoint();
        for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> ownedNep : ownedNepList.entrySet()) {
            if (LayerProtocolName.PHOTONICMEDIA.equals(ownedNep.getValue().getLayerProtocolName())
                && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())
                && checkAvailableSpectrum(ownedNep.getValue().getUuid(), true)) {
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

    private void buildDefaultVirtualCtps() {
        // For Degrees, to each OTS NEP can be associated a virtual NEPs corresponding to Ctp
        // This method allows creating all corresponding CTPs whether they have already been created from node rule
        // group in buildVirtualCpsAndCtps() or not.
        if (mmDegOtsNep == null) {
            return;
        }
        // Initially : To be kept until ultimate debug
        // Use an index of 100 to avoid generating Virtual NEP with the same Id for different NEP, As the virtual NEPs
        // generated in buildVirtualCpsAndCtps follow during their creation an index numbering that is associated with
        // the NodeRuleGroup, which is not the case here
               //int degreeNumber = 100;

        LOG.debug("TONLine537 mmDegOtsNep {}", mmDegOtsNep.entrySet().stream()
            .map(nep -> nep.getValue().getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        for (Map.Entry<Uuid, BasePceNep> otsNep : mmDegOtsNep.entrySet()) {
            // For each Degree OTN NEP, if a virtual NEP was not already created, creates a virtual node corresponding
            // to the degree CTP
            if ((otsNep.getValue().getVirtualNep() == null || otsNep.getValue().getVirtualNep().isEmpty())
                && otsNep.getValue().getName().entrySet().iterator().next().getValue().getValue().contains("TTP")) {
                LOG.debug("TONLine545 input for createNodeOrVnepId {}",
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
                virtualNep.setParentNep(otsNep.getKey());
                mmDegOtsNep.put(virtualNep.getNepCepUuid(), virtualNep);
                //degOtsNep.add(virtualNep);
                //degreeNumber++;
            }
        }
    }

    private void buildVirtualCpsAndCtps() {
        Map<NodeRuleGroupKey, NodeRuleGroup> nrgList = this.node.getNodeRuleGroup();
        int virtualNepId = 0;
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
            LOG.debug("TONLine604 Evaluating NRG {}, fwblock = {}, blocking = {}",
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
                virtualNepId++;
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
                        virtualNep.setFrequencyBitset(mmSrgOtsNep.entrySet().stream()
                            .filter(bpn -> otsSrgNepKeyList.stream()
                                .map(NodeEdgePointKey::getNodeEdgePointUuid).collect(Collectors.toList())
                                .contains(bpn.getKey()))
                            .findAny().orElseThrow().getValue().getFrequenciesBitSet());
                        // contrary to DEG CTPs, do not define a unique
                        // Parent NEP as there are multiple PPs
                        mmSrgOtsNep.put(virtualNep.getNepCepUuid(), virtualNep);
                    }
                    for (Map.Entry<Uuid, BasePceNep> otsNep : mmSrgOtsNep.entrySet()) {
                    //for (BasePceNep otsNep : srgOtsNep) {
                        // TODO: verify consistency of the following if test
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
                        //for each otsNep (there are several deegrees, create a virtualNepId CTP
                        Map<Uuid, Name> vnepId2;
                        String otsNepName = otsNep.getValue()
                                .getName().entrySet().iterator().next().getValue().getValue();
                        vnepId2 = createNodeOrVnepId(otsNepName.split("\\-TTP")[0], "DEG", false);
                        // TODO: verify consistency of the following if test
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
                                virtualNep.setParentNep(otsNep.getKey());
                                virtualNep.setFrequencyBitset(otsNep.getValue().getFrequenciesBitSet());
                                tempVirtualBpnMap.put(vnepId2.entrySet().iterator().next().getKey(), virtualNep);
                                //degOtsNep.add(virtualNep);
                            }
                        }
                    }
                    mmDegOtsNep.putAll(tempVirtualBpnMap);
                    LOG.debug("TONLine722 Node {} mmDegOtsNep NepName in BuiltvirtualCP andCtps{}", nodeName,
                        mmDegOtsNep.entrySet().stream()
                            .map(nep -> nep.getValue().getName().entrySet().iterator().next().getValue().getValue())
                            .collect(Collectors.toList()));
                    LOG.debug("TONLine726 Node {} mmDegOtsNep VirtualNepName declared @end of BuiltvirtualCP andCtps{}",
                        nodeName,
                        mmDegOtsNep.entrySet().stream()
                            .map(nep -> nep.getValue().getVirtualNep()).collect(Collectors.toList()));
                    LOG.debug("TONLine730 Node {} DegOtsNep VirtualNepName declared (end of) BuiltvirtualCP andCtps{}",
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

    // Complement this method to have the PP order
    private Map<Uuid, Name> createNodeOrVnepId(String identifier, String extension, boolean isNode) {
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

    private void printBpnListImportantParameters() {
        Map<String, String> srgNepAndVnepMap = new HashMap<>();
        for (BasePceNep bpn : mmSrgOtsNep.values()) {
            srgNepAndVnepMap.put(bpn.getName().entrySet().iterator().next().getValue().getValue(),
                bpn.getVirtualNep() == null || bpn.getVirtualNep().isEmpty()
                    ? "xxxxx"
                    : bpn.getVirtualNep().entrySet().iterator().next().getValue().getValue());
        }
        LOG.debug("TONLine 775 :Names of NEPs and their associated Virtual NEP in SRGOTSNEP are {}", srgNepAndVnepMap);
        Map<String, String> degNepAndVnepMap = new HashMap<>();
        for (BasePceNep bpn : mmDegOtsNep.values()) {
            degNepAndVnepMap.put(bpn.getName().entrySet().iterator().next().getValue().getValue(),
                bpn.getVirtualNep() == null || bpn.getVirtualNep().isEmpty()
                    ? "xxxxx"
                    : bpn.getVirtualNep().entrySet().iterator().next().getValue().getValue());
        }
        LOG.debug("TONLine 783 : Names of NEPs and their associated Virtual NEP in DEGOTSNEP are {}", degNepAndVnepMap);
    }

    private void buildInternalLinksMap() {
        // Process for creation of link between CTPs is not the same as from CP to CTP.
        //  Between CTPs we need to examine the node rule group defined for OTS Nep considering virtual
        // CTP have already been created whereas for CP to CTP it is base on inter rule groups
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
                        LOG.debug("TONLine876 found a FWDING NRG for Node {} with NepMap {}", nodeName, nepMap);
                        if (NodeTypes.Rdm.equals(this.commonNodeType)) {
                            intLinkMap.putAll(createNrgPartialMesh(nepMap));
                        }
                    }
                }
            }
        }
        if (NodeTypes.Rdm.equals(this.commonNodeType)) {
            LOG.debug("TONLine822 At end of FWDING NRG scan (for Optical Bypass  in ROADM) IntLinkMap size = {}",
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

    private Map<Uuid, IntLinkObj> createNrgPartialMesh(Map<NodeEdgePointKey, NodeEdgePoint> nepMap) {
        // Create a Full Mesh between VirtualNEPs associated to all pairs of NEP of NepMap, without duplicate.
        // Consider that this is a partial mesh, as we create less Links than if we were creating all the links
        // between any combination of NEPs of the NepMap
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
            LOG.debug("TONLine904 node {} mmSrgOtsNep Uuid list {}", nodeName, mmSrgOtsNep.values().stream()
                .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
            LOG.debug("TONLine906 node {} mmDegOtsNep Uuid list {}", nodeName, mmDegOtsNep.values().stream()
                .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
            LOG.debug("TONLine908 node {} indexedNepList Uuid list {}", nodeName, indexedNepList);
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
            LOG.debug("TONLine999 createNrgPartialMesh : Org Nep {} found in {}", nepUuid, orgNodeType);
            if (orgBpn == null) {
                nepOrder++;
                continue;
            }
            Uuid orgVnepUuid = orgBpn.getVirtualNep().entrySet().iterator().next().getKey();
            String orgVnepName = orgBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
            LOG.debug("TON line931 createNrgPartialMesh, scanning NEP, orgVnep UUID is {}, orgVnep Name is {}",
                orgVnepUuid, orgVnepName);

            // The second for loop is scanning any Nep of higher rank than the
            // current NEP of the first for loop
            if (nepOrder > indexedNepList.size() - 1) {
                break;
            }
            LOG.debug("TON line939 nepOrder is {}, indexedNepList.size() is {}", nepOrder, indexedNepList.size());
            for (int nnO = nepOrder; nnO < indexedNepList.size(); nnO++) {
                LOG.debug("TON line941 entering loop for dest Vnep (createNrg Partial Mesh");
                String nepId = indexedNepList.get(nnO).getValue();
                LOG.debug("TON line943 nepId is {}", nepId);
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
                    LOG.debug("TONLine957 did not find in Deg/srgOtsNep indexNepList Nep {}", nnO);
                }
                LOG.debug("TONLine959 createNrgPartialMesh, destVnep {} found in {} for orgVnep with UUID {} in {}",
                    nepId, destNodeType, nepUuid, orgNodeType);
                if (destBpn == null) {
                    break;
                }
                Uuid destVnepUuid = destBpn.getVirtualNep().entrySet().iterator().next().getKey();
                String destVnepName = destBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
                LOG.debug("TON Line966 createNrgPartialMesh, scanning NEP, destinationVnep UUID is {},"
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
        LOG.debug("TON line988 createNrgPartialMesh, IntLinkMap is {}",
            intLinkMap.values().stream().map(IntLinkObj::getLinkId).collect(Collectors.toList()));
        return intLinkMap;
    }

    private Map<Uuid, Name> createLinkId(String orgName, String destName) {
        Map<Uuid, Name> vvLinkId = new HashMap<>();
        Name nameVlink = new NameBuilder().setValueName("VirtualLinkName")
            .setValue(String.join("+", orgName, "--", destName)).build();
        Uuid vvLinkUuid = new Uuid(UUID.nameUUIDFromBytes(nameVlink
            .getValue().getBytes(Charset.forName("UTF-8"))).toString());
        vvLinkId.put(vvLinkUuid, nameVlink);
        return vvLinkId;
    }

    private boolean isValidBpn(BasePceNep bpn) {
        if (aaPortId == null && zzPortId == null) {
            return true;
        }
        if (bpn.getSipUuid() == null) {
            LOG.debug("TONLine1021, null SIP for BPN {}", bpn.getName());
        }
        // Allows to qualify node validity whatever is the portId used : NEP Uuid or SIP Uuid. If SIP uuid
        // is used, Bpn that has SIP corresponding to the PortId provided in the request will be validated
        // and PCE can later rely on NEP rather than SIP (Easier to handle since SIP model in the context
        // does not include any reference to the NEP it is attached to!
        if ((aaPortId != null && bpn.getNepCepUuid().equals(aaPortId))
                || (zzPortId != null && bpn.getNepCepUuid().equals(zzPortId))
                || (aaPortId != null && bpn.getSipUuid() != null && bpn.getSipUuid().equals(aaPortId))
                || (zzPortId != null && bpn.getSipUuid() != null && bpn.getSipUuid().equals(zzPortId))) {
            return true;
        } else {
            return false;
        }
    }

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

    private Uuid getOnepSipUuid(OwnedNodeEdgePoint onep) {
        if (onep.getMappedServiceInterfacePoint() != null && !onep.getMappedServiceInterfacePoint().isEmpty()) {
            return onep.getMappedServiceInterfacePoint().entrySet().iterator().next().getKey()
                .getServiceInterfacePointUuid();
        }
        return null;
    }

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
        LOG.debug("TONLine1021 mmDegOtsNep outgoing Links {}",
            mmDegOtsNep.values().stream().map(bpn -> bpn.getConnectedLink()).collect(Collectors.toList()));
        LOG.debug("TONLine1023 mmSrgOtsNep outgoing Links {}",
            mmSrgOtsNep.values().stream().map(bpn -> bpn.getConnectedLink()).collect(Collectors.toList()));
    }

    private Map<Uuid, PceTapiOpticalNode> splitDegNodes() {
        LOG.debug("TONLine1028 Node {} mmDegOtsNep Uuid list {}", nodeName, mmDegOtsNep.values().stream()
            .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        LOG.debug("TONLine1030 Node {} mmDegOtsNep TpType list {}", nodeName, mmDegOtsNep.values().stream()
            .map(BasePceNep::getTpType).collect(Collectors.toList()));
        LOG.debug("TONLine1032 Node {} mmdegOtsNep Name list {}", nodeName, mmDegOtsNep.values().stream()
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
        LOG.debug("TONLine1045 Node {} vNepIdList {}", nodeName, vvNepIdList);
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
            LOG.debug("TONLine1072 Node {} create TapiOpticalNode {}", nodeName, nodeId);
            var degNode = new PceTapiOpticalNode(serviceType, this.node, OpenroadmNodeType.DEGREE,
                version, degXOtsNep, nodeId, nodeName.getValue(), mcCapability);
            LOG.debug("TONLine1075 CREATEDEGNODE {} of class {} ", nodeId, degNode.getClass());
            Map<Uuid, Uuid> vnepToSubNode = new HashMap<>();
            for (BasePceNep bpn : degXOtsNep) {
                if (vvNepUuid.equals(bpn.getNepCepUuid())) {
                    vnepToSubNode.put(vvNepUuid, nodeId.entrySet().iterator().next().getKey());
                }
            }
            degNode.initFrequenciesBitSet();
            LOG.debug("TONLine1082 Node {} NodeId {} Subnode Vnep List {}", nodeName, nodeId, vnepToSubNode);
            this.bindingVNepToSubnodeMap.putAll(vnepToSubNode);
            degMap.put(nodeId.keySet().iterator().next(), degNode);
            LOG.debug("TONLine1085 CREATEDEGNODE {}", degNode.getSupClliNodeId());
            LOG.debug("TONLine1085 CREATEDEGNODE {}", degNode.getAdminState());
            LOG.debug("TONLine1085 CREATEDEGNODE {}", degNode.getCentralFreqGranularity());
            LOG.debug("TONLine1085 CREATEDEGNODE {}", degNode.getNodeId());
            LOG.debug("TONLine1085 CREATEDEGNODE {}", degNode.getState());
            LOG.debug("TONLine1085 creating Node {}", nodeId);
        }
        return degMap;
    }

    private Map<Uuid, PceTapiOpticalNode> splitSrgNodes() {
        LOG.debug("TONLine1096 mmSrgOtsNep Uuid list {}", mmSrgOtsNep.values().stream()
            .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        LOG.debug("TONLine1098 mmSrgOtsNep TpType list {}", mmSrgOtsNep.values().stream()
            .map(BasePceNep::getTpType).collect(Collectors.toList()));
        LOG.debug("TONLine1100 mmSrgOtsNep Name list {}", mmSrgOtsNep.values().stream()
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
        LOG.debug("TONLine1112 vNepIdMap {}", vvNepIdList);
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
            LOG.debug("TONLine1140 new PceTapiON {}, list of nep is {}",
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
            LOG.debug("TONLine1151 CREATESRGNODE {}", srgNode);
            LOG.debug("TONLine1152 creating Node {}", nodeId);
        }
        return srgMap;
    }

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

    public void validateAZxponder() {
        LOG.info("TONLine1245 validateAZxponder: STEP1 entering validation for XPONDER == Id: {}, Name : {}",
            node.getUuid().toString(), node.getName().toString());
        if (!this.valid || this.commonNodeType != NodeTypes.Xpdr) {
            return;
        }
        LOG.info("TONLine1250 validateAZxponder: STEP2 entering validation for XPONDER == Id: {}, Name : {}",
            node.getUuid().toString(), node.getName().toString());
        // Detect A and Z
//        if (aaanodeId.getValue().contains(node.getUuid().toString())
//            || zzznodeId.getValue().contains(node.getUuid().toString())) {
        if (aaNodeId.getValue().equals(node.getUuid().getValue())
            || zzNodeId.getValue().equals(node.getUuid().getValue())) {
            LOG.debug("validateAZxponder TAPI: A or Z node detected == {}, {}", node.getUuid().toString(),
                node.getName().toString());
            initTapiXndrTps();
            LOG.info("TONLine1302: ValidateAZxponder for node : {}, nwOtsNep : {}", this.nodeName,
                nwOtsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
            allOtsNep.clear();
            allOtsNep.addAll(nwOtsNep);
            allOtsNep.addAll(clientDsrNep);
            allOtsNep.stream().distinct().collect(Collectors.toList());
            LOG.info("TONLine1308: ValidateAZxponder for node : {}, allOtsNep : {}", this.nodeName,
                allOtsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
            Map<Uuid, Name> nodeId = new HashMap<>();
            nodeId.put(nodeUuid, node.getName().entrySet().iterator().next().getValue());
            // Map<Uuid, String> xpdrKey = new HashMap<>();
            // xpdrKey.put(nodeId.keySet().iterator().next(), deviceNodeId);
            PceTapiOpticalNode xpdr = new PceTapiOpticalNode(serviceType, this.node,
                OpenroadmNodeType.XPONDER, version, allOtsNep, nodeId, deviceNodeId, mcCapability);
            xpdr.initXndrTps(servFormat);
            xpdr.initFrequenciesBitSet();
            LOG.info("TONLine1273 validateAZxponder: XPONDER == Id: {}, Name : {} is validated",
                node.getUuid().toString(), node.getName().toString());
            this.pceNodeMap.put(nodeId.keySet().iterator().next(), xpdr);
            return;
        }
        LOG.info("TONLine1278 validateAZxponder: XPONDER == Id: {}, Name : {} is ignored", node.getUuid().toString(),
            node.getName().toString());
        valid = false;
    }

    public void initTapiXndrTps() {
        // This function allows retrieving NEPs for Transponder
        // It identifies valid NETWORK TPs (the one that don't have any wavelength configured) checking
        //     ownedNodeEdgePoint/cep-list/connection-end-point/
        //     tapi-photonic-media:otsi-mc-connection-end-point-spec/otsi-termination-pac/selected-central-frequency
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
                if (ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                    .filter(sclpqi -> PHOTONICLAYERQUALIFIEROTSiMC.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                    .findAny() != null) {
                    // This might be the case of NetworkPorts that already have a provisioned Lambda (depends on imp)
                    if (checkUsedSpectrum(ownedNep.getValue().getUuid(), true)
                            || ownedNep.getValue().getAvailablePayloadStructure().stream()
                                .filter(aps -> aps.getCapacity().getValue().doubleValue() >= serviceRate)
                                .collect(Collectors.toList()).isEmpty()) {
                        invalidNwNepList.add(ownedNep.getKey().getUuid());
                        // TODO: at the end scan the list of invalid NW port and
                        // remove the NEP from which they client/Parent
                        continue;
                    }
                }
                if (!ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                        .filter(sclpqi -> PHOTONICLAYERQUALIFIEROTS.VALUE.equals(sclpqi.getLayerProtocolQualifier()))
                        .findAny().isEmpty()
                        && !ownedNep.getValue().getName().entrySet().stream()
                            .findFirst().orElseThrow().getValue().getValue().contains("OTSi")) {
                    LOG.info("TONLine1291 initXndrTps Found a Nep with OTSlpq {}", ownedNep.getValue().getName());
                    var otsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                    otsNep.setOperationalState(ownedNep.getValue().getOperationalState());
                    direction = calculateDirection(ownedNep.getValue(), null, TpType.NW);
                    otsNep.setDirection(direction.keySet().iterator().next());
                    otsNep.setTpType(direction.values().iterator().next());
                    otsNep.setLpq(PHOTONICLAYERQUALIFIEROTS.VALUE);
                    direction.clear();
                    otsNep.setAdminState(ownedNep.getValue().getAdministrativeState());
                    otsNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                    BitSet freqBitset = buildBitsetFromSpectrum(ownedNep.getValue().getUuid());
                    otsNep.setFrequencyBitset(freqBitset);
                    LOG.info("TONLine 1301 : FreqBitset for ONEP {} calculated to {}",
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
                                        invalidNwNepList.add(ownedNep.getKey().getUuid());
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
                            otsLcpNWList.add(otsCep);
                            otsNep.setCepOtsUuid(cep.getValue().getUuid());
                            if (cepClientNepMap != null) {
                                otsNep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                    .getNodeEdgePointUuid());
                            }
                            break;
                        }
                    }
                    nwOtsNep.add(otsNep);
                }
                // Client Port support DSR protocol layer
            } else if (LayerProtocolName.DSR.equals(ownedNep.getValue().getLayerProtocolName())
                && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())) {
                // TODO : activate following 6 lines of the code after capacity is correctly populated in the topology
                if (ownedNep.getValue().getAvailableCapacity() == null
                        || ownedNep.getValue().getAvailableCapacity().getTotalSize().getValue() == null
                        || !(ownedNep.getValue().getAvailableCapacity().getTotalSize().getValue().doubleValue() > 0)) {
                    // The DSR Nep is not eligible since it has already a service mapped on it with no available capa
                    continue;
                }
                var clientNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                direction = calculateDirection(ownedNep.getValue(), null, TpType.CLIENT);
                clientNep.setDirection(direction.keySet().iterator().next());
                clientNep.setTpType(direction.values().iterator().next());
                direction.clear();
                clientNep.setOperationalState(ownedNep.getValue().getOperationalState());
                clientNep.setAdminState(ownedNep.getValue().getAdministrativeState());
                clientNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                clientDsrNep.add(clientNep);
            } else if (LayerProtocolName.DIGITALOTN.equals(ownedNep.getValue().getLayerProtocolName())
                    || LayerProtocolName.ODU.equals(ownedNep.getValue().getLayerProtocolName())) {
                // For eODU NEP we fill dsrNepWithParentOdu Map to fill at a later step the vertically connected nep
                LOG.info("TON Line 1402 : Handling NEP {} ", ownedNep.getValue().getName());
                Uuid clientNepUuid = null;
                String nepType = "";
                if (ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class) != null
                        && ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList() != null
                        && !ownedNep.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList()
                            .getConnectionEndPoint().isEmpty()) {
                    for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep.getValue()
                        .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                        .entrySet()) {
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
                LOG.info("TON Line 1440 : Handling NEP {} of nepType {}", ownedNep.getValue().getName(), nepType);
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
                    LOG.info("TON Line 1451 : Adding ODU NEP {} of type {} to corresponding list of base pce node",
                        ownedNep.getValue().getName(), nepType);
                    var otnNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                    direction = calculateDirection(ownedNep.getValue(), null,
                        nepType.equals("eODU") ? TpType.CLIENT : TpType.NW);
                    otnNep.setDirection(direction.keySet().iterator().next());
                    otnNep.setTpType(direction.values().iterator().next());
                    direction.clear();
                    otnNep.setOperationalState(ownedNep.getValue().getOperationalState());
                    otnNep.setAdminState(ownedNep.getValue().getAdministrativeState());
                    otnNep.setSipUuid(getOnepSipUuid(ownedNep.getValue()));
                    if (nepType.equals("iOTU")) {
                        otuLcpList.add(otnNep);
                    } else {
                        oduLcpList.add(otnNep);
                    }
                }
            }
        }
        LOG.info("TONLine1459 clientDsrNep {}", clientDsrNep.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.info("TONLine1462 nwOtsNep {}", nwOtsNep.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.info("TONLine1465 otsLcpNWList {}", otsLcpNWList.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.info("TONLine1468 oduLcpList {}", oduLcpList.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.info("TONLine1471 otuLcpList {}", otuLcpList.stream()
            .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
            .collect(Collectors.toList()));
        LOG.info("TONLine1474 invalidNwNepList {}", invalidNwNepList);

        // Purge otsLcpNwList and nwOTsNEP list since NEP associated to OTS and
        // OTSI_MC may not appear in the expected order. We remove port that have a client in the invalidNwNepList, only
        // if the origin or destination port are network port : we keep Nw ports that are part of the path for services
        // of lower granularity
        for (Uuid nepUuid : invalidNwNepList) {
            if (aaPortId == null || zzPortId == null) {
                otsLcpNWList = otsLcpNWList.stream().filter(bpn -> !(bpn.getClientNep().equals(nepUuid))).distinct()
                    .collect(Collectors.toList());
                nwOtsNep = nwOtsNep.stream().filter(bpn -> !(bpn.getClientNep().equals(nepUuid))).distinct()
                    .collect(Collectors.toList());
            } else {
                otsLcpNWList = otsLcpNWList.stream()
                    .filter(bpn -> !(bpn.getClientNep().equals(nepUuid)) && isValidBpn(bpn))
                    .distinct()
                    .collect(Collectors.toList());
                nwOtsNep = nwOtsNep.stream()
                    .filter(bpn -> !(bpn.getClientNep().equals(nepUuid)) && isValidBpn(bpn))
                    .distinct()
                    .collect(Collectors.toList());
            }
        }
        LOG.info("TONLine 1491 : call populateBpnVerticalNep");
        populateBpnVerticalNep();
        // Having populated the list of vertically connected NEPs, we can populate the list of NRG that may define
        // the connections between NEPs at a specific level, and that will be propagated to the NEP of other levels
        // that are vertically connected to the NEPs involved in NRGs

        populateBpnNrgForXpdr();
        LOG.info("TONLine 1498 : call populateBpnNrgForXpdr Nrg List {}", nwOtsNep.stream()
            .map(BasePceNep::getNodeRuleGroupUuid).collect(Collectors.toList()));
        // Having propagated the list of NRGs to vertically connected NEPs, we can populate the list of NRG that may
        // define the connections between NEPs of client and network side to indirectNRGs of bpn, scanning IRGs.
        LOG.info("TONLine 1427 : call populateBpnVerticalNep");
        populateBpnIndirectNrgForXpdr();
        LOG.info("TONLine 1431 : call populateBpnNrgForXpdr Nrg List {}", nwOtsNep.stream()
            .map(BasePceNep::getindirectNrgUuid).collect(Collectors.toList()));
        String portType = "nw";
        if (aaPortId == null || zzPortId == null) {
            LOG.debug("InitTapiXndr launched for Node of Id {}, name {} with no specified port imposed for a/z end,"
                + "all valid NW ports validated", node.getUuid().toString(), node.getName().toString());
            return;
        }

        LOG.debug("TONLINE1406 : clientDsrNep includes : {}", clientDsrNep.stream().map(bpn -> bpn.getName())
            .collect(Collectors.toList()));
        Uuid portId = null;
        if (!nwOtsNep.stream().filter(bpn -> isValidBpn(bpn)).findAny().isEmpty()) {
            portId = nwOtsNep.stream()
                .filter(bpn -> isValidBpn(bpn)).findFirst().orElseThrow().getNepCepUuid();
            final Uuid finalPortId = portId;
            nwOtsNep.removeAll(nwOtsNep.stream()
                .filter(bpn -> (!isValidBpn(bpn) || (!bpn.getVerticallyConnectedNep().contains(finalPortId))))
                .collect(Collectors.toList()));
//            nwOtsNep = nwOtsNep.stream()
//                .filter(bpn -> (bpn.getNepCepUuid().equals(aportId) || bpn.getNepCepUuid().equals(zportId)))
//                .collect(Collectors.toList());
        } else if (!clientDsrNep.stream().filter(bpn -> isValidBpn(bpn)).findFirst().isEmpty()) {
            BasePceNep port = clientDsrNep.stream().filter(bpn -> isValidBpn(bpn)).findFirst().orElseThrow();
            portId = port.getNepCepUuid();
            LOG.info("TONLINE1461 : clientDsrNep includes : {}", clientDsrNep.stream().map(bpn -> bpn.getName())
                .collect(Collectors.toList()));
            portType = "client";
            LOG.debug("TONLine1427 InitTapiXndr launched for Node of Id {}, name {}, {} port {} validated for a/z end."
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
                LOG.info("TONLINE1477 : OtsNep nrgs : {}", nwNepNrgList);
                nwNepNrgList.stream().filter(nrgUuid -> portNrgList.contains(nrgUuid)).collect(Collectors.toList());
                if (nwNepNrgList.isEmpty()) {
                    nonConnectedNwBpn.add(otsNep);
                }
            }
            nwOtsNep.removeAll(nonConnectedNwBpn);
            LOG.info("TONLINE1483 : purged nwOtsNep includes : {}", nwOtsNep.stream().map(bpn -> bpn.getName())
                .collect(Collectors.toList()));

//            nwOtsNep = purgeXndrPortList(portId, nwOtsNep);

            final Uuid finalPortId = portId;
            LOG.info("TONLINE1585 : unpurged clientDsrNep includes : {}",clientDsrNep.stream().map(bpn -> bpn.getName())
                .collect(Collectors.toList()));
            clientDsrNep.removeAll(clientDsrNep.stream()
                .filter(bpn -> (!isValidBpn(bpn) || !bpn.getVerticallyConnectedNep().contains(finalPortId)))
                .collect(Collectors.toList()));
            LOG.info("TONLINE1591 : Purged clientDsrNep includes : {}",clientDsrNep.stream().map(bpn -> bpn.getName())
                .collect(Collectors.toList()));
//            clientDsrNep = clientDsrNep
//                .stream().filter(bpn -> (bpn.getNepCepUuid().equals(aportId) || bpn.getNepCepUuid().equals(zportId)))
//                .collect(Collectors.toList());
            LOG.debug("TONLine1435 A/Z is client port, nwOtsNep after purge is {}", nwOtsNep.stream()
                .map(nep -> nep.getName().entrySet().iterator().next().getValue().getValue())
                .collect(Collectors.toList()));
            return;
        } else {
            LOG.error("TONLine1440 InitTapiXndr launched for Node of Id {}, name {}, but no port validated for a/z end",
                node.getUuid().toString(), node.getName().toString());
            clientDsrNep.clear();
            nwOtsNep.clear();
            return;

        }
        // Applies to condition portId found in nwOtsNep, meaning service applies to NW port of the Xponder
        LOG.debug("TONLine1446 InitTapiXndr launched for Node of Id {}, name {}, {} port {} validated for a/z end",
            node.getUuid().toString(), node.getName().toString(), portType, portId);

        return;
    }

    // This method purges the nwOtsNep List removing all network ports that do not have connectivity to the designated
    // client port, analyzing the content of the Node-rule-groups and the Inter-rule groups
    //
    // Options considered for Xponder NodeRuleGroup (NRG) and InterRuleGroup (IRG) layout
    //
    // Option1 : | client ----\
    // Option2 : | client----\ \___NW Nx NRG (client---NW)
    //          Nx NRG { ... / N: Nbre of Tuple associating client and network port
    // | client----
    //       Typical configuration of Multi-port Transponder or Muxponder
    // | client----/ with no possibility of hair-pining between client ports
    // N: number of Network port
    //       Typical configuration for Muxponder/switchPonders with hair-pining capability between clients
    //
    // Option 3 : NRGi (client,...., client) Forwarding (Hair-pining) or not
    //            NRGk (NW,...,NW) Forwarding (Regen function creating loop between NW ports)or not
    //            IRG (NRGi,....NRGk) forwarding
    //      Typical configuration for Mux and SwitchPonders
    //
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
        LOG.debug("TONLine1529 purgeXndrPortList: nrgList (Client, no nw ports) contains {}", nrgofClientNepList);
        LOG.debug("TONLine1530 purgeXndrPortList: nrgOfNwNepMap (NW, no Client) contains {}", nrgOfNwNepMap);
        LOG.debug("TONLine1531 purgeXndrPortList: totalNwOtsNepIdList (NW & Client) contains {}", totalNwOtsNepIdList);
        // All NRGs have been analyzed
        if (!totalNwOtsNepIdList.isEmpty()) {
            // We found some NW ports associated with the client port in one or several NRG
            List<Uuid> totalNwOtsNepUuidList = totalNwOtsNepIdList.stream()
                .map(NodeEdgePointKey::getNodeEdgePointUuid).distinct()
                .collect(Collectors.toList());
            LOG.debug("TONLine1539 purgeXndrPortList: List(UUID) of NW Port connected to ClientPortId {}",
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
        LOG.debug("TONLine1551 purgeXndrPortList: did not succeed to find client port and NW ports in the same NRG");
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

    private void createLinksFromIlMap() {
        List<Map<Uuid, Name>> intLinkList = internalLinkMap.entrySet().stream()
            .map(Map.Entry<Uuid, IntLinkObj>::getValue)
            .collect(Collectors.toList())
                .stream()
                .map(IntLinkObj::getLinkId).collect(Collectors.toList());
        for (Map<Uuid, Name> map : intLinkList) {
            LOG.debug("TONLine1611 For Node {}, Link to be configure = {}",nodeName, map.values().toString());
        }

        for (Map.Entry<Uuid, IntLinkObj> linkTBC : internalLinkMap.entrySet()) {
            LOG.debug("TONLine1615 Link to be configure = {}",
                linkTBC.getValue().getLinkId().entrySet().iterator().next().getValue());
            LOG.debug("TONLine1617 bindingVNepToSubnodeMap = {}", bindingVNepToSubnodeMap);
            LOG.debug("TONLine1618 Link to be configure ORG = {} DEST = {}",
                linkTBC.getValue().getOrgTpUuid(), linkTBC.getValue().getDestTpUuid());
            LOG.debug("TONLine1620 Link to be configure PceNodeORG = {} PceNodeDEST = {}",
                bindingVNepToSubnodeMap.entrySet().stream()
                .filter(vts -> vts.getKey()
                    .equals(linkTBC.getValue().getDestTpUuid())).findFirst().orElseThrow().getValue(),
                bindingVNepToSubnodeMap.entrySet().stream()
                .filter(vts -> vts.getKey()
                    .equals(linkTBC.getValue().getOrgTpUuid())).findFirst().orElseThrow().getValue());
            this.pceInternalLinkMap.put(linkTBC.getKey(),
                new PceTapiLink(linkTBC.getValue().getLinkId().entrySet().iterator().next().getValue(),
                    linkTBC.getKey(), linkTBC.getValue().getOrgTpUuid(), linkTBC.getValue().getDestTpUuid(),
                    pceNodeMap.get(bindingVNepToSubnodeMap.entrySet().stream()
                        .filter(vts -> vts.getKey()
                            .equals(linkTBC.getValue().getOrgTpUuid())).findFirst().orElseThrow().getValue()),
                    pceNodeMap.get(bindingVNepToSubnodeMap.entrySet().stream()
                        .filter(vts -> vts.getKey()
                            .equals(linkTBC.getValue().getDestTpUuid())).findFirst().orElseThrow().getValue())));
        }
        LOG.debug("TONLine1637 pceInternalLinkMap = {} ",
            pceInternalLinkMap.values().stream().map(PceTapiLink::getLinkName).collect(Collectors.toList()));
    }

    // This method provides from a frequency expressed in Hz (T-API) the adjacent upper Slot Number in a Flexgrid
    // Slot Number = N + 285, N being an integer varying from -284 to 484 for C band as defined by IEEE.
    private int convertFreqToAdjacentSlotNumber(long frequency) {
        return (int) Math.round((frequency / GridConstant.HZ_TO_THZ - GridConstant.ANCHOR_FREQUENCY)
            / (GridConstant.GRANULARITY / 1000.0) + 285);
    }

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
        LOG.debug("TONLine 1672 : FreqBitset for ONEP {} calculated to {}", ooNep.getName(), freqBitSet);
        return freqBitSet;
    }

    private boolean checkOtsNepAvailable(Uuid otsNepUuid) {
        // Check that used spectrum is empty or null which is the condition of availability for a PP
        //TODO: provide implementation for this method
        return true;
    }

    private boolean isNepWithGoodCapabilities(Uuid nepUuid) {
        //TODO:implement this function from profiles
        // WARNING : shall no be develop according to presence or not of CEP which may depend on implementations
        //Furthermore for Muxponders and switchponders, the network port partially used must be part of the path from
        //client to destination : this means we must not reject a provisioned Network port supporting a lambda
        // The availability of a Nep is highly dependent on the layer : an OTS/OTSi port is no more available for
        //provisioning at the WDM layer if it is already supporting a lambda. Higher layer nep might however be valid
        //for multiplexing
//        if (StringConstants.UNKNOWN_MODE.equals(getXpdrOperationalMode(nepUuid))) {
//            return false;
//        }
        return true;
    }

    public String getXpdrOperationalMode(Uuid nepUuid) {
        // TODO: when 2.4 models available, retrieve Operational modes from
        // Profiles
        List<String> supportedOM = new ArrayList<>();

        if (supportedOM == null || supportedOM.isEmpty()) {
            LOG.warn("getOperationalMode: NetworkPort {} of Node {}  with Uuid {} has no operational mode declared ",
                nepUuid, this.nodeName, this.nodeUuid);
            return StringConstants.UNKNOWN_MODE;
        }
        for (String operationalMode : supportedOM) {
            if (operationalMode.contains(StringConstants.SERVICE_TYPE_RATE
                .get(this.serviceType).toCanonicalString())) {
                LOG.debug(
                    "getOperationalMode: NetworkPort {} of Node {}  with Uuid {}  has {} operational mode declared",
                    nepUuid, this.nodeName, this.nodeUuid, operationalMode);
                return operationalMode;
            }
        }
        LOG.warn("getOperationalMode: NetworkPort {} of Node {}  with Uuid {} has no operational mode declared"
            + "compatible with service type {}. Supported modes are : {} ",
            nepUuid, this.nodeName, this.nodeUuid, this.serviceType, supportedOM.toString());
        return StringConstants.UNKNOWN_MODE;
    }

    private boolean checkAvailableSpectrum(Uuid lcpUuid, boolean isNEP) {
        // Use to check that there are some availabilities in the spectrum
        // scans all spectrum portions defined in the available spectrum of the
        boolean availableCbandSpectrum = false;
        OwnedNodeEdgePoint ooNep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> lcpUuid.equals(onep.getKey().getUuid())).findFirst().orElseThrow().getValue();
        // TODO: Currently not used with something else than NEP. Complete function if needed
        // for other LCP, or remove isNEP
        LOG.debug("Analysing LCP of NEP {}", ooNep.getName());
        if (ooNep.augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                .OwnedNodeEdgePoint1.class) == null) {
            LOG.debug("TONline1732: checkAvailableSpectrum: noAugmentationONEP1 for nep {}", ooNep.getName());
            return false;
        }
        Map<AvailableSpectrumKey, AvailableSpectrum> aaSpectrum = ooNep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1.class)
            .getPhotonicMediaNodeEdgePointSpec().getSpectrumCapabilityPac().nonnullAvailableSpectrum();
        if (!(aaSpectrum == null || aaSpectrum.isEmpty())) {
            for (Map.Entry<AvailableSpectrumKey, AvailableSpectrum> as : aaSpectrum.entrySet()) {
                if (!(convertFreqToAdjacentSlotNumber(as.getValue().getLowerFrequency()
                    .longValue()) > GridConstant.EFFECTIVE_BITS
                    || convertFreqToAdjacentSlotNumber(as.getValue().getUpperFrequency().longValue()) <= 1)) {
                    // If the one of the boundaries is included in C band, this is a spectrum portion of interest
                    availableCbandSpectrum = true;
                    LOG.debug("TONline1745: checkAvailableSpectrum: nep {} has available spectrum", ooNep.getName());
                }
            }
        }
        return availableCbandSpectrum;
    }

    private boolean checkUsedSpectrum(Uuid lcpUuid, boolean isNEP) {
        // Use to check that a port is not already used : currently used for TSP NWTPs, but could be used also for PP
        // scans all spectrum portions defined in the occupied spectrum
        boolean usedCbandSpectrum = false;
        OwnedNodeEdgePoint ooNep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> lcpUuid.equals(onep.getKey().getUuid())).findFirst().orElseThrow().getValue();
        // Currently not used with something else than NEP. Complete function if needed for other LCP, or remove isNEP
        Map<OccupiedSpectrumKey, OccupiedSpectrum> ooSpectrum = ooNep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1.class)
            .getPhotonicMediaNodeEdgePointSpec().getSpectrumCapabilityPac().nonnullOccupiedSpectrum();
        if (!(ooSpectrum == null || ooSpectrum.isEmpty())) {
            usedCbandSpectrum = true;
        }
        return usedCbandSpectrum;
    }

    private void populateBpnVerticalNep() {
        // For all Bpn of nwOtsNep List, we populate the list of vertically connected NEPs
        LOG.info("TONLine 1797 : populateBpnVerticalNep");
        for (BasePceNep bpn : nwOtsNep) {
            List<Uuid> nepUuidList = new ArrayList<>();
            nepUuidList.add(bpn.getNepCepUuid());
            LOG.info("TONLine 1801 : populateBpnVerticalNep");
            bpn.setVerticallyConnectedNep(getVirtuallyConnectedNep(bpn.getNepCepUuid(), nepUuidList));
            LOG.info("TONline1841: List of vertical Neps for bpn {} includes {} elements", bpn.getName(),
                nepUuidList.size());
            LOG.info("TONline1843: List of vertical Neps for bpn {} includes {} elements", bpn.getName(),
                nepUuidList);
        }
        // For all Bpn of clientDsrNep List, we populate the list of vertically connected NEPs
        LOG.info("TONLine 1806 : populateBpnVerticalNep");
        for (BasePceNep bpn : clientDsrNep) {
            List<Uuid> nepUuidList = new ArrayList<>();
            if (this.dsrNepWithParentOdu.containsKey(bpn.getNepCepUuid())) {
                nepUuidList.add(this.dsrNepWithParentOdu.entrySet().stream()
                    .filter(uuidCouple -> uuidCouple.getKey().equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
                    .getValue());
            }
            bpn.setVerticallyConnectedNep(nepUuidList);
            LOG.info("TONline1803: List of vertical Neps for bpn {} includes {} elements", bpn.getName(),
                nepUuidList.size());
        }
    }

    private void populateBpnNrgForXpdr() {
        Map<Uuid, List<Uuid>> nrgNepMap = new HashMap<>();
        for (NodeRuleGroup nrg : node.getNodeRuleGroup().values()) {
            List<Uuid> nepList = new ArrayList<>();
            nepList.addAll(nrg.getNodeEdgePoint().values().stream()
                .map(NodeEdgePoint::getNodeEdgePointUuid).collect(Collectors.toList()));
            nrgNepMap.put(nrg.getUuid(), nepList);
        }
        LOG.info("TONLine 1824 : populateBpnNrgForXpdr");
        for (Map.Entry<Uuid, List<Uuid>> mapentry : nrgNepMap.entrySet()) {
            for (BasePceNep bpn : nwOtsNep) {
                List<Uuid> vertNepsUuidList = bpn.getVerticallyConnectedNep();
                vertNepsUuidList.add(bpn.getNepCepUuid());
                for (Uuid nepUuid : vertNepsUuidList) {
                    if (mapentry.getValue().contains(nepUuid)) {
                        bpn.setNodeRuleGroupUuid(mapentry.getKey());
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
                        break;
                    }
                }
            }
        }
    }

    private void populateBpnIndirectNrgForXpdr() {
        Map<Uuid, List<Uuid>> indNrgMap = new HashMap<>();
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
        LOG.info("TONLine 1865 : populateBpnIndirectNrgForXpdr");
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
        }
    }

    private List<Uuid> getVirtuallyConnectedNep(Uuid nepUuid, List<Uuid> connectedNepUuidList) {
        LOG.info("TONLine 1887 : populateBpnVerticalNep");
        // isDSR does not have a local significance : it remains the same during recursive calls and does not evolve.
        // It is used to fix the way of searching for connected NEP :
        // - if original NEP is DSR we search for parents
        // - if original NEP is not DSR we search for clients.
        OwnedNodeEdgePoint onep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(nep -> nep.getKey().getUuid().equals(nepUuid)).findAny().orElseThrow().getValue();
        if (onep.augmentation(OwnedNodeEdgePoint1.class) == null
            || onep.augmentation(OwnedNodeEdgePoint1.class).getCepList() == null
            || onep.augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                .isEmpty()) {
            LOG.info("TONLine 1896 : populateBpnVerticalNep");
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
                    getVirtuallyConnectedNep(clientNepUuid, connectedNepUuidList);
                } else {
                    return connectedNepUuidList;
                }
            }
        }
        return connectedNepUuidList;
    }

    private Map<Uuid, IntLinkObj> createIrgPartialMesh(
        Map<Integer, Map<NodeEdgePointKey, NodeEdgePoint>> intercoNepMap) {
        LOG.debug("TONLine1938 InterconnecMap {}", intercoNepMap);
        // Create a Full Mesh between VirtualNEPs associated to all combination of 2 NEPs from each Map of intercoNepMap
        // without duplicate. Consider that this is a partial mesh, as we create less Links than if we were creating
        // all the links between any combination of NEPs of the 2 maps of intercoNepMap
        Map<NodeEdgePointKey, NodeEdgePoint> nepMap1;
        nepMap1 = intercoNepMap.get(0);
        LOG.debug("TONLine1944 nepMap1 {}", nepMap1);
        Map<NodeEdgePointKey, NodeEdgePoint> nepMap2;
        nepMap2 = intercoNepMap.get(1);
        LOG.debug("TONLine1947 nepMap2 {}", nepMap2);
        LOG.debug("TONLine1948 mmSrgOtsNep {}", mmSrgOtsNep.values().stream()
            .map(BasePceNep::getNepCepUuid).collect(Collectors.toList()));
        LOG.debug("TONLine1950 mmDegOtsNep {}", mmDegOtsNep.values().stream()
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
            LOG.debug("TONLine1973 orgNodeType {}", orgNodeType);
            Uuid orgVnepUuid = orgBpn.getVirtualNep().entrySet().iterator().next().getKey();
            String orgVnepName = orgBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
            LOG.debug("TON Line 1977 createIrgPartialMesh, scanning NEP, origin Vnep Name is {}", orgVnepName);

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
                LOG.debug("TONLine2015 createIrgPartialMesh, scanning NEP, destination Vnep Name is {}", destVnepName);
            }
        }
        LOG.debug("TON Line 2018 createIrgPartialMesh, interlinkMap is {}", internLinkMap.values().stream()
            .map(IntLinkObj::getLinkId).collect(Collectors.toList()));
        return internLinkMap;
    }

    public Map<Uuid, PceTapiOpticalNode> getPceNodeMap() {
        return this.pceNodeMap;
    }

    public Boolean isValid() {
        return this.valid;
    }

    public NodeTypes getCommonNodeType() {
        return this.commonNodeType;
    }

    public List<BasePceNep> getDegOtsNep() {
        return this.mmDegOtsNep.values().stream().collect(Collectors.toList());
    }

    public List<BasePceNep> getDegOmsNep() {
        return this.degOmsNep.stream().distinct().collect(Collectors.toList());
    }

    public List<BasePceNep> getSrgOtsNep() {
        return this.mmSrgOtsNep.values().stream().collect(Collectors.toList());
    }

    public List<BasePceNep> getnetOtsNep() {
        return this.nwOtsNep.stream().distinct().collect(Collectors.toList());
    }

    public List<BasePceNep> getClientDsrNep() {
        return this.clientDsrNep.stream().distinct().collect(Collectors.toList());
    }

    public List<BasePceNep> getOduNep() {
        return this.oduLcpList.stream().distinct().collect(Collectors.toList());
    }

    public List<BasePceNep> getOtuNep() {
        return this.otuLcpList.stream().distinct().collect(Collectors.toList());
    }

    public Map<Uuid,IntLinkObj> getInternalLinkMap() {
        return this.internalLinkMap;
    }

    public Map<Uuid, PceTapiLink> getPceInternalLinkMap() {
        return this.pceInternalLinkMap;
    }

    private static final class IntLinkObj {
        private Map<Uuid, Name> linkId;
        private Uuid orgTpUuid;
        private Uuid destTpUuid;

        private IntLinkObj(Map<Uuid, Name> linkIid, Uuid orgTpUuid, Uuid destTpUuid) {
            this.orgTpUuid = orgTpUuid;
            this.destTpUuid = destTpUuid;
            this.linkId = linkIid;
        }

        private Uuid getOrgTpUuid() {
            return orgTpUuid;
        }

        private Uuid getDestTpUuid() {
            return destTpUuid;
        }

        private Map<Uuid, Name> getLinkId() {
            return linkId;
        }
    }

}
