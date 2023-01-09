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
//import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Optional;
//import java.util.Optional;
//import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.mapping.PortMapping;
//import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.path.computation.reroute.
//request.input.Endpoints;
//import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmTpType;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.eth.rev181210.ConnectionEndPoint1;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint1;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint2;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint3;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint4;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint5;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint6;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint7;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.ConnectionEndPoint8;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.GridType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.pool.capability.pac.AvailableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.pool.capability.pac.AvailableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.pool.capability.pac.OccupiedSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.pool.capability.pac.OccupiedSpectrumKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.context.topology.context.topology
//  .node.owned.node.edge.point.cep.list.connection.end.point.OtsConnectionEndPointSpec;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.context.topology.context.topology
//  .node.owned.node.edge.point.cep.list.connection.end.point.OtsConnectionEndPointSpecBuilder;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.node.edge.point.*;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.node.edge.point
//  .spec.*;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.node.edge.point
//  .spec.McPool;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.pool.*;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.pool.capability.*;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.media.channel.properties.pac.
//  OccupiedSpectrum;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.context.topology.context.topology
//  .node.owned.node.edge.point.cep.list.connection.end.point.MediaChannelConnectionEndPointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.context.topology.context.topology
//  .node.owned.node.edge.point.cep.list.connection.end.point.OtsiConnectionEndPointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.inter.rule.group
//  .AssociatedNodeRuleGroup;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.inter.rule.group
//  .AssociatedNodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.InterRuleGroup;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.InterRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.end.point.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiOpticalNode {
    private static final Logger LOG = LoggerFactory.getLogger(TapiOpticalNode.class);

    private boolean valid;
    private Node node;
    private Uuid nodeUuid;
    private String deviceNodeId;
    // see if Need global or local class for Name
    private Name nodeName;
//    private LayerProtocolName lpn;
//    private OpenroadmNodeType orNodeType;
    private NodeTypes commonNodeType;
    private AdministrativeState adminState;
    private OperationalState operationalState;
    private String serviceType;
    private Uuid aaNodeId;
    private Uuid zzNodeId;
    private Uuid aaPortId;
    private Uuid zzPortId;
    private ServiceFormat servFormat;

//    private Map<Uuid, Name> usedXpndrNWTps = new TreeMap<>();
//    private Map<Uuid, Name> availableXpndrNWTps = new TreeMap<>();
//    private List<PceLink> outgoingLinks = new ArrayList<>();
//    private Map<String, String> clientPerNwTp = new HashMap<>();
//    private final AvailFreqMapsKey freqMapKey = new AvailFreqMapsKey(GridConstant.C_BAND);
//    private BitSet frequenciesBitSet;
    private String version;
    private BigDecimal slotWidthGranularity;
    private BigDecimal centralFreqGranularity;
//    private Endpoints endpoints;

    private List<BasePceNep> allOtsNep = new ArrayList<>();
    private List<BasePceNep> srgOtsNep = new ArrayList<>();
    private List<BasePceNep> degOtsNep = new ArrayList<>();
    private List<BasePceNep> degOmsNep = new ArrayList<>();
    private List<BasePceNep> nwOtsNep = new ArrayList<>();
    private List<BasePceNep> clientDsrNep = new ArrayList<>();
    private List<BasePceNep> omsLcpList = new ArrayList<>();
    private List<BasePceNep> otsLcpList = new ArrayList<>();
    private List<BasePceNep> otsLcpNWList = new ArrayList<>();
    private List<Uuid> invalidNwNepList = new ArrayList<>();
    private Map<Uuid, Name> internalLinkMap = new HashMap<>();
    private Map<Map<Uuid, String>, PceTapiOpticalNode> pceNodeMap = new HashMap<>();


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
            Uuid anodeId, Uuid znodeId, Uuid aportId, Uuid zportId, ServiceFormat serviceFormat) {
        // For T-API topology, all nodes in the OLS do not have any portmapping which maybe
        // available only for OpenConfig Transponder : try to avoid relying on PortMapping
        if (serviceType == null
                || node == null
                || slotWidthGranularity == null) {
            LOG.error("TapiOpticalNode: one of parameters is not populated : node, slot width granularity");
            this.valid = false;
        } else if (!(OperationalState.ENABLED.equals(node.getOperationalState()))) {
            LOG.error("TapiOpticalNode: Node {} ignored since its operational state {} differs from ENABLED",
                node.getName().toString(), node.getOperationalState().toString());
            this.valid = false;
        } else {
            this.valid = true;
            this.serviceType = serviceType;
            this.node = node;
            this.deviceNodeId = node.getUuid().toString();
            this.nodeUuid = node.getUuid();
            this.version = version;
            this.slotWidthGranularity = slotWidthGranularity;
            this.centralFreqGranularity = centralFreqGranularity;
            this.adminState = node.getAdministrativeState();
            this.operationalState = node.getOperationalState();
            this.aaNodeId = anodeId;
            this.zzNodeId = znodeId;
            this.aaPortId = aportId;
            this.zzPortId = zportId;
            this.servFormat = serviceFormat;
            // First step is to qualify the node, determining its type
            qualifyNode();
        }
    }

    public void initialize() {
        switch (commonNodeType) {
            case Rdm:
                initRoadmIlaTps();
                buildVirtualCpsAndCtps();
                buildDefaultVirtualCtps();
                buildInternalLinksMap();
                this.pceNodeMap = splitDegNodes();
                this.pceNodeMap.putAll(splitSrgNodes());
                createLinksFromIlMap();
                break;
            case Xpdr:
                validateAZxponder(servFormat, aaNodeId, zzNodeId, aaPortId, zzPortId);
                //for Xponder, PceNode are created and handled through validateAZxponder as for the OpenROADM PCE
                break;
            case Ila:
                // In case of ILA as this type of node is not defined at that time in OpenROADM, we use ROADM
                //  var ilaNode = new PceTapiOpticalNode(serviceType, node, OpenroadmNodeType.ROADM,
                //  version, slotWidthGranularity, centralFreqGranularity, clientNwDsrNep);
                break;
            default:
                break;
        }
    }

    private  void qualifyNode() {
        LOG.debug("qualifyNode: calculates node type of {} from CEP characteristics", this.nodeUuid);
        if (!this.node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)) {
            LOG.debug("qualifyNode: Node {} is not assimilated to a Photonic Media Node", this.nodeUuid);
            this.valid = false;
            return;
        }
        // TODO: Will need to change "ODU" to DIGITAL_OTN when model 2.4 available
        if (this.node.getLayerProtocolName().contains(LayerProtocolName.DSR)
                || this.node.getLayerProtocolName().contains(LayerProtocolName.ODU)
                || this.node.getLayerProtocolName().contains(LayerProtocolName.ETH)) {
            this.commonNodeType = NodeTypes.Xpdr;
        } else {
            this.commonNodeType = NodeTypes.Rdm;
        // TODO : Need to consider ILA when model 2.4 available
//            Replace else by
//            else if (this.node.getSupportedCepLayerQualifierInstances.contains(PHOTONICLAYERQUALIFIEROMS.VALUE)) {
//                this.commonNodeType = NodeTypes.Rdm;
//            } else if (this.node.getSupportedCepLayerQualifierInstances.contains(PHOTONICLAYERQUALIFIEROTS.VALUE)) {
//                this.commonNodeType = NodeTypes.Ila;
//            } else {
//                LOG.debug("qualifyNode: Unidentified type for Node {} ", this.nodeUuid);
//                this.valid = false;
//                return;
//            }
        }
        this.valid = true;
        LOG.debug("identifyNodeType: node type of {} is {}", this.nodeUuid, this.commonNodeType.getName());
        return;
    }

    private void initRoadmIlaTps() {
        //T-API ROADMs are not disaggregated. A ROADM Node will include PPs and TTPs
        if (!this.valid) {
            return;
        }
        // Scaning Owned NEP from the Photonic layer
        // ownedNep.getValue().getSupportedCepLayerProtocolQualifierInstances() provides info on OMS/OTS/MC presence
        // NEP with OTS, no OMS are PPS --> if InService and no MC (occupancy) --> put in srgOtsNep
        // NEP with OTS and OMS are TTPs  --> In service --> Put them in degOtsNep
        // Relies on checkOtsNepAvailable
        Map<DirectionType, OpenroadmTpType> direction;
        LOG.debug("initRoadmIlaTps: getting tps from ROADM node {}", this.nodeUuid);
        // for each of the photonic OwnedNEP which Operational state is enable
        // and spectrum is not fully used
        Map<OwnedNodeEdgePointKey,OwnedNodeEdgePoint> ownedNepList = this.node.getOwnedNodeEdgePoint();
        for (Map.Entry<OwnedNodeEdgePointKey,OwnedNodeEdgePoint> ownedNep : ownedNepList.entrySet()) {
            if (LayerProtocolName.PHOTONICMEDIA.equals(ownedNep.getValue().getLayerProtocolName())
                && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())
                && checkAvailableSpectrum(ownedNep.getValue().getUuid(), true)) {
                //If the NEP is an OTS NEP, scans the list of CEP, Adds to otsLcpList each CEP which owns an otsCEPspec
                //and fills allOtsNep with all information it retrieves from the CEP

                // If condition to be modified T-API2.4 where SupportedCepLayerProtQualifier is a list (not leaflist)
                if (ownedNep.getValue().getSupportedCepLayerProtocolQualifier()
                        .contains(PHOTONICLAYERQUALIFIEROTS.VALUE)) {
                    for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep.getValue()
                        .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint().entrySet()) {
                        if (cep.getValue().augmentation(ConnectionEndPoint4.class).getOtsConnectionEndPointSpec()
                                != null) {
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
                            otsNep.setCepOtsSpec(cep.getValue().augmentation(ConnectionEndPoint4.class)
                                .getOtsConnectionEndPointSpec());
                            //check if spectrum shall be populated here
                            allOtsNep.add(otsNep);
                            break;
                        }
                    }
                }
                //If the NEP is an OMS NEP, scans the list of CEP, Adds to oMsLcpList each CEP which owns an omsCEPspec
                //and fills degOmsNep with all information it retrieves from the CEP

                // If condition to be modified T-API2.4 where SupportedCepLayerProtQualifier is a list (not leaflist)
                if (ownedNep.getValue().getSupportedCepLayerProtocolQualifier()
                        .contains(PHOTONICLAYERQUALIFIEROMS.VALUE)) {
                    for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep2 : ownedNep.getValue()
                        .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint().entrySet()) {
                        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                        // TODO: No OMS in 2.1.x : check what is the right augmentation for getOmsConnectionEndPointSpec
                        //in 2.4 and change getOTSepspec to getOMSepspec
                        if (cep2.getValue().augmentation(ConnectionEndPoint4.class).getOtsConnectionEndPointSpec()
                                != null) {
                            //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                            var omsCep = new BasePceNep(cep2.getValue().getUuid(), cep2.getValue().getName());
                            omsCep.setParentNep(cep2.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid());
                            omsCep.setClientNep(cep2.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                    .getNodeEdgePointUuid());
                            omsLcpList.add(omsCep);
                            var omsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                            omsNep.setCepOmsUuid(cep2.getValue().getUuid());
                            //TODO: Qualify next line which seems to be wrong : the parent nep of the cep is the NEP
                            //itself, not its parent NEP!!!! See if used somewhere ortherwise remove the line
                            omsNep.setParentNep(cep2.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid());
                            omsNep.setClientNep(cep2.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                    .getNodeEdgePointUuid());
                            omsNep.setOperationalState(OperationalState.ENABLED);
                            omsNep.setTpType(OpenroadmTpType.DEGREETXRXTTP);
                            direction = calculateDirection(ownedNep.getValue(), null, TpType.TTP);
                            omsNep.setDirection(direction.keySet().iterator().next());
                            omsNep.setTpType(direction.values().iterator().next());
                            direction.clear();
                            //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                            // TODO: No OMS in 2.4.1 : check what is the right augmentation for
                            //getOmsConnectionEndPointSpec in 2.4 and activate the following lines
                            //omsNep.setCepOmsSpec(cep2.getValue().augmentation(ConnectionEndPoint4.class)
                            //      .getOmsConnectionEndPointSpec());
                            //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                            degOmsNep.add(omsNep);
                            break;
                        }
                    }
                }
            }
        }
        // Loop to Fill srgOtsNep and degOtsNep from allOtsNep, relying on information (parent NEP) present in
        // otsLcpList, and set direction, tpType and spectrum use
        boolean isDegreeNep;
        for (BasePceNep otsCep : otsLcpList) {
            isDegreeNep = false;
            for (BasePceNep omsNep : degOmsNep) {
                if (omsNep.getNepCepUuid() == otsCep.getClientNep()) {
                    // The OTS NEP is a degree NEP
                    BasePceNep otsNep = allOtsNep.stream().filter(bpn -> otsCep.getParentNep()
                        .equals(bpn.getNepCepUuid())).findFirst().orElseThrow();
                    direction = calculateDirection(
                            ownedNepList.entrySet().stream().filter(onep -> otsNep.getNepCepUuid()
                                .equals(onep.getValue().getUuid())).findFirst().orElseThrow()
                                .getValue(), null, TpType.TTP);
                    otsNep.setDirection(direction.keySet().iterator().next());
                    otsNep.setTpType(direction.values().iterator().next());
                    otsNep.setFrequencyBitset(buildBitsetFromSpectrum(otsNep.getNepCepUuid()));
                    otsNep.setCepOtsSpec(otsCep.getCepOtsSpec());
                    degOtsNep.add(otsNep);
                    isDegreeNep = true;
                    break;
                }
            }
            if (!isDegreeNep) {
                BasePceNep otsNep = allOtsNep.stream().filter(bpn -> otsCep.getParentNep().equals(bpn.getNepCepUuid()))
                    .findFirst().orElseThrow();
                if (!checkOtsNepAvailable(otsNep.getNepCepUuid())) {
                    break;
                }
                direction = calculateDirection(
                    ownedNepList.entrySet().stream().filter(onep -> otsNep.getNepCepUuid().equals(onep.getValue()
                    .getUuid())).findFirst().orElseThrow().getValue(), null, TpType.PP);
                //direction = calculateDirection(ownedNepList.get(otsNep.getUuid()), null, TpType.PP);
                otsNep.setDirection(direction.keySet().iterator().next());
                otsNep.setTpType(direction.values().iterator().next());
                otsNep.setFrequencyBitset(buildBitsetFromSpectrum(otsNep.getNepCepUuid()));
                srgOtsNep.add(otsNep);
            }
        }
    }

    private void buildDefaultVirtualCtps() {
        // For Degrees, to each OTS NEP can be associated a virtual NEPs corresponding to Ctp
        // This method allows creating all corresponding CTPs whether they have already been created from node rule
        // group in buildVirtualCpsAndCtps() or not.
        if (degOtsNep == null) {
            return;
        }
        // Use an index of 100 to avoid generating Virtual NEP with the same Id for different NEP, As the virtual NEPs
        //generated in buildVirtualCpsAndCtps follow during their creation an index numbering that is associated with
        // the NodeRuleGroup, which is not the case here
        int degreeNumber = 100;
        for (BasePceNep otsNep : degOtsNep) {
            // For each Degree OTN NEP, if a virtual NEP was not already created, creates a virtual node corresponding
            // to the degree CTP
            if (otsNep.getVirtualNep() == null || otsNep.getVirtualNep().isEmpty()) {
                Map<Uuid,Name> vnepId = createNodeOrVnepId(String.valueOf(degreeNumber), "-DEG", false);
                // Add in DegOtsNep Map the virtual node associated to the block
                otsNep.setVirtualNep(vnepId);
                Map<NameKey,Name> vnepNameMap = new HashMap<>();
                vnepNameMap.put(vnepId.entrySet().iterator().next().getValue().key(),
                    vnepId.entrySet().iterator().next().getValue());
                BasePceNep virtualNep = new BasePceNep(vnepId.entrySet().iterator().next().getKey(), vnepNameMap);
                virtualNep.setDirection(DirectionType.BIDIRECTIONAL);
                virtualNep.setOperationalState(OperationalState.ENABLED);
                virtualNep.setTpType(OpenroadmTpType.DEGREETXRXCTP);
                virtualNep.setParentNep(otsNep.getNepCepUuid());
                degOtsNep.add(virtualNep);
                degreeNumber++;
            }
        }
    }

    private void buildVirtualCpsAndCtps() {
        Map<NodeRuleGroupKey, NodeRuleGroup> nrgList = this.node.getNodeRuleGroup();
        int virtualNepId = 0;
        //Make from list of OTS NEP a Map which simplifies its management
        Map<Uuid, BasePceNep> mmSrgOtsNep = srgOtsNep.stream()
            .collect(Collectors.toMap(BasePceNep::getNepCepUuid, Function.identity()));
        Map<Uuid, BasePceNep> mmDegOtsNep = degOtsNep.stream()
            .collect(Collectors.toMap(BasePceNep::getNepCepUuid, Function.identity()));
        for (Map.Entry<NodeRuleGroupKey, NodeRuleGroup> nrg : nrgList.entrySet()) {
            // For each NodeRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
            // and if this is the case store its key in fwRuleKeyList
            List<RuleKey> fwdRuleKeyList =
                nrg.getValue().getRule().entrySet().stream()
                .filter(rule -> RuleType.FORWARDING.equals(rule.getValue().getRuleType()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
            boolean fwdBlock = false;
            boolean blocking = false;
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Node Rule Group (NRG) does not contain any forwarding rule, we go to next NRG
                break;
            } else {
                // If we have one or several forwarding rule(s)
                for (RuleKey rk : fwdRuleKeyList) {
                    if (ForwardingRule.MAYFORWARDACROSSGROUP
                            .equals(nrg.getValue().getRule().get(rk).getForwardingRule())
                            || ForwardingRule.MUSTFORWARDACROSSGROUP
                            .equals(nrg.getValue().getRule().get(rk).getForwardingRule())) {
                       // We are in Contention-less Add/drop Block or a degree, or in a node where forwarding condition
                       // is defined across all NEPs
                        fwdBlock = true;
                    } else if (ForwardingRule.CANNOTFORWARDACROSSGROUP
                            .equals(nrg.getValue().getRule().get(rk).getForwardingRule())) {
                       // We are in a regular Add/drop Block with contention
                        fwdBlock = true;
                        blocking = true;
                    }
                }
            }
            // Handle now in the same way a non blocking SRG (Contentionless) and a traditional blocking SRG
            if (fwdBlock) {
                // In case we have a forwarding condition defined, (blocking for Regular blocking add/drop block or
                // non blocking  for ContentionLess Add/Drop), we check if it applies to OTS NEPs (finding them
                // in mSrgOtsNep or mDegOtsNep Map) and store keys of considered NEP in ots<Srg/Deg>NepKeyList
                List<NodeEdgePointKey> otsSrgNepKeyList = nrg.getValue().getNodeEdgePoint().entrySet().stream()
                    .filter(nep -> mmSrgOtsNep.containsKey(nep.getValue().getNodeEdgePointUuid()))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
                List<NodeEdgePointKey> otsDegNepKeyList = nrg.getValue().getNodeEdgePoint().entrySet().stream()
                    .filter(nep -> mmDegOtsNep.containsKey(nep.getValue().getNodeEdgePointUuid()))
                    .map(Map.Entry::getKey).collect(Collectors.toList());
                if ((otsSrgNepKeyList == null || otsSrgNepKeyList.isEmpty())
                        && (otsDegNepKeyList == null || otsDegNepKeyList.isEmpty())) {
                    //the forwarding rule does neither apply to SRG OTS NEPs, nor to DEG OTS NEPs
                    break;
                }
                // In the other case,the forwarding rule applies to OTS NEPs of either SRG or Degree
                // Create first an Id for one Virtual NEP associated to all NEPs of the Block
                virtualNepId ++;
                if (!(otsSrgNepKeyList == null || otsSrgNepKeyList.isEmpty())) {
                    Map<Uuid,Name> vnepId1;
                    vnepId1 = createNodeOrVnepId(String.valueOf(virtualNepId), "-SRG", false);
                    for (BasePceNep otsNep : srgOtsNep) {
                        //TODO: verify consistency of the following if test
                        if (otsSrgNepKeyList.stream().filter(nepkey -> nepkey.getNodeEdgePointUuid()
                                .equals(otsNep.getNepCepUuid())).findAny().isPresent()) {
                            // We store the NodeRuleGroup Id whatever is the forwarding condition
                            otsNep.setNodeRuleGroupUuid(nrg.getKey().getUuid());
                            // We store the virtual NEP Id, only if we have a blocking condition, as the possibility
                            // for a PP to forward traffic to a TTP may be coded in many different way, and we don't
                            // want to multiply the number of virtual NEP (used as extremity of links interconnecting
                            // Add/Drop to Degrees
                            if (blocking) {
                                // Add in srgOtsNep Map the virtual node associated to the block
                                otsNep.setVirtualNep(vnepId1);
                                Map<NameKey,Name> vnepNameMap1 = new HashMap<>();
                                vnepNameMap1.put(vnepId1.entrySet().iterator().next().getValue().key(),
                                    vnepId1.entrySet().iterator().next().getValue());
                                BasePceNep virtualNep = new BasePceNep(vnepId1.entrySet().iterator().next().getKey(),
                                    vnepNameMap1);
                                virtualNep.setDirection(DirectionType.BIDIRECTIONAL);
                                virtualNep.setOperationalState(OperationalState.ENABLED);
                                virtualNep.setTpType(OpenroadmTpType.SRGTXRXCP);
                                // contrary to DEG CTPs, do not define a unique Parent NEP as there are multiple PPs
                                srgOtsNep.add(virtualNep);
                            }
                        }
                    }
                }
                if (!(otsDegNepKeyList == null || otsDegNepKeyList.isEmpty()) && !blocking) {
                    Map<Uuid,Name> vnepId2;
                    vnepId2 = createNodeOrVnepId(String.valueOf(virtualNepId), "-DEG", false);
                    for (BasePceNep otsNep : degOtsNep) {
                        //TODO: verify consistency of the following if test
                        if (otsDegNepKeyList.stream().filter(nepkey -> nepkey.getNodeEdgePointUuid()
                                .equals(otsNep.getNepCepUuid())).findAny().isPresent()) {
                            otsNep.setNodeRuleGroupUuid(nrg.getKey().getUuid());
                            // We store the virtual NEP Id, only if we have a non blocking condition
                            if (!blocking) {
                                // Add in DegOtsNep Map the virtual node associated to the block
                                otsNep.setVirtualNep(vnepId2);
                                Map<NameKey,Name> vnepNameMap2 = new HashMap<>();
                                vnepNameMap2.put(vnepId2.entrySet().iterator().next().getValue().key(),
                                    vnepId2.entrySet().iterator().next().getValue());
                                BasePceNep virtualNep = new BasePceNep(vnepId2.entrySet().iterator().next().getKey(),
                                    vnepNameMap2);
                                virtualNep.setDirection(DirectionType.BIDIRECTIONAL);
                                virtualNep.setOperationalState(OperationalState.ENABLED);
                                virtualNep.setTpType(OpenroadmTpType.DEGREETXRXCTP);
                                virtualNep.setParentNep(otsNep.getNepCepUuid());
                                degOtsNep.add(virtualNep);
                            }
                        }
                    }
                }
            } else {
                // No forwarding condition defined
                break;
            }
        }
    }

    // Complement this method to have the PP order
    private Map<Uuid,Name> createNodeOrVnepId(String indentifier, String extension, boolean isNode) {
        String tpType;
        if ("-SRG".equals(extension)) {
            tpType = "-CP";
        } else if ("-DEG".equals(extension)) {
            tpType = "-CTP";
        } else {
            tpType = "";
        }
        Name name;
        if (isNode) {
            name = new NameBuilder().setValueName("VirtualNepName")
                .setValue(String.join("+", nodeName.getValue(), extension, indentifier))
                .build();
        } else {
            name = new NameBuilder().setValueName("VirtualNepName")
                .setValue(String.join("+", nodeName.getValue(), extension, indentifier,
                tpType)).build();
        }
        Map<Uuid,Name> id = new HashMap<>();
        Uuid uuid = new Uuid(UUID.nameUUIDFromBytes(name.getValue().getBytes(Charset.forName("UTF-8"))).toString());
        id.put(uuid, name);
        return id;
    }

    private void buildInternalLinksMap() {
        //TODO: NEED TO COMPLEMENT WITH INTER RULE GROUP activating last part of the code for this method
        // Process for creation of link between CTPs is not the same as from CP to CTP. Between CTPs we need to
        // examine the node rule group defined for OTS Nep considering virtual CTP have already been created
        // whereas for CP to CTP it is base on inter rule groups
        Map<Uuid, Name> intLinkMap = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nrgList = this.node.getNodeRuleGroup();
        for (Map.Entry<NodeRuleGroupKey, NodeRuleGroup> nrg : nrgList.entrySet()) {
            // For each NodeRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
            // and if this is the case store its key in fwRuleKeyList
            List<RuleKey> fwdRuleKeyList =
                nrg.getValue().getRule().entrySet().stream()
                .filter(rule -> RuleType.FORWARDING.equals(rule.getValue().getRuleType()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
            //boolean fwdBlock = false;
            //boolean blocking = false;
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Node Rule Group (NRG) does not contain any forwarding rule, we go to next NRG
                continue;
            } else {
                // If we have one or several forwarding rule(s)
                for (RuleKey rk : fwdRuleKeyList) {
                    if (ForwardingRule.MAYFORWARDACROSSGROUP
                        .equals(nrg.getValue().getRule().get(rk).getForwardingRule())
                        || ForwardingRule.MUSTFORWARDACROSSGROUP
                            .equals(nrg.getValue().getRule().get(rk).getForwardingRule())) {
                        Map<NodeEdgePointKey, NodeEdgePoint> nepMap = nrg.getValue().getNodeEdgePoint();
                        // Store in internalLinkMap all Links created between NEP whether its a Full
                        // mesh (Xponders)
                        // or a partial mesh (ROADMs)
                        switch (this.commonNodeType) {
                            case Rdm:
                                intLinkMap.putAll(createNrgPartialMesh(nepMap));
                                break;
                            // Changed strategy for Xponder : detect client port and keep only the NW port
                            // which are connected
                            // to the client port checking NRGs and IRGs in purgeXndrPortList
                            // case Xpdr:
                            // internalLinkMap.putAll(createXpdrFullMesh(nepMap));
                            // break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        // In this part of the method we add to InternalLinkMap the links to be created with regard to Inter-Rule-Group
        // SHOULD ACTIVATE The Code between XXXXXXXX / XXXXXXX when 2.4 Models become available
        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
//        Map<InterRuleGroupKey, InterRuleGroup> irgList = this.node.getInterRuleGroup();
//        for (Map.Entry<InterRuleGroupKey, InterRuleGroup> irg : irgList.entrySet()) {
//            // For each InterRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
//            // and if this is the case store its key in fwRuleKeyList
//            fwdRuleKeyList =
//                irg.getValue().getRule().entrySet().stream()
//                .filter(rule -> RuleType.FORWARDING.equals(rule.getValue().getRuleType()))
//                .map(Map.Entry::getKey).collect(Collectors.toList());
//            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
//                // If the Inter Rule Group (IRG) does not contain any forwarding rule, we go to next IRG
//                continue;
//            } else {
//                // If we have one or several forwarding rule(s)
//                for (RuleKey rk : fwdRuleKeyList) {
//                    if (ForwardingRule.MAYFORWARDACROSSGROUP
//                            .equals(irg.getValue().getRule().get(rk).getForwardingRule())
//                            || ForwardingRule.MUSTFORWARDACROSSGROUP
//                            .equals(irg.getValue().getRule().get(rk).getForwardingRule())) {
//                         Map<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> anrgMap = irg.getValue()
//                             .getAssociatedNodeRuleGroup();
//                         int indexMap = 0;
//                         Map<Integer, Map<NodeEdgePointKey, NodeEdgePoint>> intercoNepMap = new HashMap<>();
//                         for (Map.Entry<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup>
//                                 anrg : anrgMap.entrySet()) {
//                             intercoNepMap.put(indexMap, nrgList.get(anrg.getValue()
//                                    .getNodeRuleGroupUuid()).getNodeEdgePoint());
//                             indexMap++;
//                         }
//                         if (indexMap > 2) {
//                           LOG.error("Error managing InterRuleGroup in node {}, as InterRule Group defined from more "
//                               + "than 2 node-rule-groups is not currently managed. Additional node-rule-groups will "
//                                 + "not be considered!", this.nodeName);
//                         }
//                         switch (this.commonNodeType) {
//                            case Rdm:
//                                internalLinkMap.putAll(createIrgPartialMesh(intercoNepMap));
//                                break;
//                            case Xpdr:
//                                internalLinkMap.putAll(createXpdrIrgPartialMesh(intercoNepMap));
//                                break;
//                            default:
//                                break;
//                        }
//                    }
//                }
//            }
//        }
        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        this.internalLinkMap = intLinkMap;
    }

    private Map<Uuid, Name> createNrgPartialMesh(Map<NodeEdgePointKey, NodeEdgePoint> nepMap) {
        // Create a Full Mesh between VirtualNEPs associated to all pairs of NEP of NepMap, without duplicate.
        // Consider that this is a partial mesh, as we create less Links than if we were creating all the links
        // between any combination of NEPs of the NepMap
        Map<Uuid, Name> intLinkMap = new HashMap<>();
        List<String> uuidSortedList = new ArrayList<String>();
        int nepOrder = 0;
        // IndexedNepList is used to create the Mesh : it includes all nodes of NepMap with an index
        Map<Integer,Uuid> indexedNepList = new HashMap<Integer,Uuid>();
        for (Map.Entry<NodeEdgePointKey, NodeEdgePoint> nep : nepMap.entrySet()) {
            indexedNepList.put(nepOrder, nep.getKey().getNodeEdgePointUuid());
            nepOrder++;
        }
        nepOrder = 1;
        String orgNodeType;
        String destNodeType;
        // IndexedNepList is used to create the Mesh : for each Nep of the list, will create links between the Virtual
        // NEP associated with the current NEP of the first loop,  and the virtualNEPs associated with any NEP of higher
        // rank in IndexedNepList
        for (Map.Entry<Integer, Uuid> nepUuid : indexedNepList.entrySet()) {
            BasePceNep orgBpn = srgOtsNep.stream().filter(bpn -> nepUuid.getValue().equals(bpn.getNepCepUuid()))
                .findFirst().orElseThrow();
            orgNodeType = "SRG";
            if (orgBpn == null) {
                orgBpn = degOtsNep.stream().filter(bpn -> nepUuid.getValue().equals(bpn.getNepCepUuid()))
                    .findFirst().orElseThrow();
                orgNodeType = "DEG";
            }
            if (orgBpn == null) {
                break;
            }
            Uuid orgVnepUuid = orgBpn.getVirtualNep().entrySet().iterator().next().getKey();
            String orgVnepName = orgBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();

            // The second for loop is scanning any Nep of higher rank than the current NEP of the first for loop
            for (int nnO = nepOrder; nnO < indexedNepList.size() - 1; nnO++) {
                String nepId = indexedNepList.get(nnO).getValue();
                BasePceNep destBpn = srgOtsNep.stream()
                    .filter(bpn -> nepId.equals(bpn.getNepCepUuid().toString()))
                    .findFirst().orElseThrow();
                destNodeType = "SRG";
                if (destBpn == null) {
                    destBpn = degOtsNep.stream()
                        .filter(bpn -> nepId.equals(bpn.getNepCepUuid().toString()))
                        .findFirst().orElseThrow();
                    destNodeType = "DEG";
                }
                if (destBpn == null) {
                    break;
                }
                Uuid destVnepUuid = orgBpn.getVirtualNep().entrySet().iterator().next().getKey();
                String destVnepName = destBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
                uuidSortedList.clear();
                uuidSortedList.add(orgVnepUuid.toString());
                uuidSortedList.add(destVnepUuid.toString());
                // Assuming these links are bidirectional, sort the list in order to avoid creating 2 times the same
                // link Org-Dest & Dest-Org
                Collections.sort(uuidSortedList);
                if (orgVnepUuid.toString().equals(uuidSortedList.get(0))) {
                    Map<Uuid, Name> linkId = createLinkId(orgVnepName, destVnepName);
                    intLinkMap.putAll(linkId);
                    addCpCtpOutgoingLink(orgVnepUuid, destVnepUuid, orgNodeType, destNodeType, linkId);
                } else {
                    Map<Uuid, Name> linkId = createLinkId(destVnepName, orgVnepName);
                    intLinkMap.putAll(linkId);
                    addCpCtpOutgoingLink(destVnepUuid, orgVnepUuid, destNodeType, orgNodeType, linkId);
                }
            }
            nepOrder++;
        }
        // Remove comment on line below : eclipse bug generating error on n when activated
        return intLinkMap;
    }

    private Map<Uuid,Name> createLinkId(String orgName, String destName) {
        Map<Uuid,Name> vvLinkId = new HashMap<>();
        Name nameVlink = new NameBuilder().setValueName("VirtualLinkName")
            .setValue(String.join("+", orgName, "--", destName)).build();
        Uuid vvLinkUuid = new Uuid(UUID.nameUUIDFromBytes(nameVlink
            .getValue().getBytes(Charset.forName("UTF-8"))).toString());
        vvLinkId.put(vvLinkUuid, nameVlink);
        return vvLinkId;
    }

    private void addCpCtpOutgoingLink(Uuid orgVnepId, Uuid destVnepId, String orgNodeType, String destNodeType,
            Map<Uuid, Name> linkId) {
        if ("SRG".equals(orgNodeType)) {
            srgOtsNep.stream().filter(bpn -> orgVnepId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
            .setConnectedInternalLinks(linkId);
        }
        if ("DEG".equals(orgNodeType)) {
            degOtsNep.stream().filter(bpn -> orgVnepId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
            .setConnectedInternalLinks(linkId);
        }
        if ("SRG".equals(destNodeType)) {
            srgOtsNep.stream().filter(bpn -> destVnepId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
            .setConnectedInternalLinks(linkId);
        }
        if ("DEG".equals(destNodeType)) {
            degOtsNep.stream().filter(bpn -> destVnepId.equals(bpn.getNepCepUuid())).findFirst().orElseThrow()
            .setConnectedInternalLinks(linkId);
        }
    }

    private Map<Map<Uuid, String>, PceTapiOpticalNode> splitDegNodes() {
        List<Map<Uuid, Name>> vvNepIdList = new ArrayList<>();
        //List<PceTapiOpticalNode> degList = new ArrayList<>();
        Map<Map<Uuid, String>, PceTapiOpticalNode> degMap = new HashMap<>();
        Map<Uuid,Name> vvNepIdMap = new HashMap<>();
        // Build a list of VirtualNEP contained in degOtsNep
        for (BasePceNep bpn: degOtsNep) {
            if (bpn.getTpType().equals(OpenroadmTpType.DEGREETXRXCTP)) {
                vvNepIdMap.clear();
                vvNepIdMap.put(bpn.getNepCepUuid(), bpn.getName().entrySet().iterator().next().getValue());
                vvNepIdList.add(vvNepIdMap);
            }
        }
        List<Integer> indexList = new ArrayList<>();
        int index;
        int subindex = 100;
        // For each virtual NEP of the list, Builds a degXOtsNep list of BasePceNode which is a subset of degOtsNep,
        // containing all BasePceNep that have it declared as Virtual NEP
        for (Map<Uuid, Name> vvNepId : vvNepIdList) {
            Uuid vvNepUuid = vvNepId.keySet().stream().findFirst().orElseThrow();
            List<BasePceNep> degXOtsNep = degOtsNep.stream()
                .filter(bpn -> bpn.getNepCepUuid().equals(vvNepUuid) || bpn.getVirtualNep().containsKey(vvNepUuid))
                .collect(Collectors.toList());
            String vvNepString = vvNepId.entrySet().iterator().next().getValue().getValue();
            index = Integer.decode(vvNepString.substring(vvNepString.indexOf("-DEG", 0) + 1,
                vvNepString.indexOf("-CTP", 0) - 1));
            Map<Uuid, Name> nodeId;
            if (indexList.contains(index)) {
                LOG.debug("Error generating Node Index for {}, from Virtual NEP {}, as index {} is already used",
                    this.nodeName.toString(), vvNepString, Integer.toString(index));
                nodeId = createNodeOrVnepId(String.valueOf(subindex), "DEG", true);
                indexList.add(subindex);
                subindex++;
            } else {
                indexList.add(index);
                nodeId = createNodeOrVnepId(String.valueOf(index), "DEG", true);
            }
            //Creates a new PceTapiOpticalNode corresponding to the degree defined by this VirtualNep
            var degNode = new PceTapiOpticalNode(serviceType, this.node, OpenroadmNodeType.DEGREE,
                version, slotWidthGranularity, centralFreqGranularity, degXOtsNep, nodeId, deviceNodeId);
            //degList.add(degNode);
            Map<Uuid, String> degKey = new HashMap<>();
            degKey.put(nodeId.keySet().iterator().next(), deviceNodeId);
            degMap.put(degKey, degNode);
        }
        return degMap;
    }

    private Map<Map<Uuid, String>, PceTapiOpticalNode> splitSrgNodes() {
        //List<PceTapiOpticalNode> srgList = new ArrayList<>();
        List<Map<Uuid, Name>> vvNepIdList = new ArrayList<>();
        Map<Map<Uuid, String>, PceTapiOpticalNode> srgMap = new HashMap<>();
        // Build a list of VirtualNEP contained in srgOtsNep
        for (BasePceNep bpn: srgOtsNep) {
            if (bpn.getTpType().equals(OpenroadmTpType.SRGTXRXCP)) {
                Map<Uuid,Name> vvNepIdMap = new HashMap<>();
                vvNepIdMap.put(bpn.getNepCepUuid(), bpn.getName().entrySet().iterator().next().getValue());
                vvNepIdList.add(vvNepIdMap);
            }
        }
        List<Integer> indexList = new ArrayList<>();
        int index;
        int subindex = 100;
        // For each virtual NEP of the list, Builds a srgXOtsNep list of BasePceNode which is a subset of srgOtsNep,
        // containing all BasePceNep that have it declared as Virtual NEP
        for (Map<Uuid, Name> vvNepId : vvNepIdList) {
            Uuid vvNepUuid = vvNepId.keySet().stream().findFirst().orElseThrow();
            List<BasePceNep> srgXOtsNep = srgOtsNep.stream()
                .filter(bpn -> bpn.getNepCepUuid().equals(vvNepUuid) || bpn.getVirtualNep().containsKey(vvNepUuid))
                .collect(Collectors.toList());
            String vvNepString = vvNepId.entrySet().iterator().next().getValue().getValue();
            index = Integer.decode(vvNepString.substring(vvNepString.indexOf("-SRG", 0) + 1,
                vvNepString.indexOf("-CP", 0) - 1));
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
            //Creates a new PceTapiOpticalNode corresponding to the degree defined by this VirtualNep
            var srgNode = new PceTapiOpticalNode(serviceType, this.node, OpenroadmNodeType.SRG,
                version, slotWidthGranularity, centralFreqGranularity, srgXOtsNep, nodeId, deviceNodeId);
            Map<Uuid, String> srgKey = new HashMap<>();
            srgKey.put(nodeId.keySet().iterator().next(), deviceNodeId);
            srgMap.put(srgKey, srgNode);
            //srgList.add(srgNode);
        }
        return srgMap;
    }

    private Map<DirectionType, OpenroadmTpType> calculateDirection(
            OwnedNodeEdgePoint ownedNep, ConnectionEndPoint cep, TpType tpType) {
        String nodeType;
        String directionCode;
        PortDirection directionEnum;
        boolean isNep = false;
        switch (tpType) {
            case PP:
            case CP:
                nodeType = "SRG-";
                break;
            case TTP:
            case CTP:
                nodeType = "DEGREE-";
                break;
            case NW:
                nodeType = "NW-";
                break;
            case CLIENT:
                nodeType = "CLIENT-";
                break;
            default:
                nodeType = "UNDEFINED-";
        }
        // The default direction is set to BIDIRECTIONAL
        // direction = "BIDIRECTIONAL";
        if (ownedNep != null) {
            // TODO: change in 2.4 to getPortdirection
            directionEnum = ownedNep.getLinkPortDirection();
            isNep = true;
        } else if (cep != null) {
            // TODO: change in 2.4 to getPortdirection
            directionEnum = cep.getConnectionPortDirection();
        } else {
            directionEnum = PortDirection.UNIDENTIFIEDORUNKNOWN;
        }
        if (PortDirection.UNIDENTIFIEDORUNKNOWN.equals(directionEnum)) {
            if (isNep) {
                LOG.debug("Error processing Nep {} as port direction is not defined/known",
                    ownedNep.getName().toString());
            } else {
                LOG.debug("Error processing Cep {} as connection direction is not defined/known",
                    cep.getName().toString());
            }
            return null;
        }
        switch (directionEnum) {
            case BIDIRECTIONAL:
                directionCode = "TXRX-";
                break;
            // To be de-activated when we migrate to 2.4
            case INPUT:
                directionCode = "RX-";
                break;
            case OUTPUT:
                directionCode = "TX-";
                break;
            // To be activated when we migrate to 2.4
            // case SINK:
            // directionCode = "RX-";
            // break;
            // case SOURCE:
            // directionCode = "TX-";
            // break;
            default:
                directionCode = "TXRX-";
        }
        Map<DirectionType, OpenroadmTpType> dirTpType = new HashMap<>();
        dirTpType.put(DirectionType.valueOf(directionEnum.toString()),
            OpenroadmTpType.valueOf(nodeType + directionCode + tpType.toString()));
        return dirTpType;
    }

    public void validateAZxponder(ServiceFormat serviceFormat, Uuid aaanodeId, Uuid zzznodeId,
            Uuid aaaPortId, Uuid zzzPortId) {
        if (!this.valid || this.commonNodeType != NodeTypes.Xpdr) {
            return;
        }
        // Detect A and Z
        if (aaanodeId.getValue().contains(node.getUuid().toString())
                || zzznodeId.getValue().contains(node.getUuid().toString())) {
            LOG.info("validateAZxponder TAPI: A or Z node detected == {}, {}", node.getUuid().toString(),
                node.getName().toString());
            initTapiXndrTps(serviceFormat, aaanodeId, zzznodeId, aaaPortId, zzzPortId);
            allOtsNep.clear();
            allOtsNep.addAll(nwOtsNep);
            allOtsNep.addAll(clientDsrNep);
            Map<Uuid, Name> nodeId = new HashMap<>();
            nodeId.put(nodeUuid, node.getName().entrySet().iterator().next().getValue());
            Map<Uuid, String> xpdrKey = new HashMap<>();
            xpdrKey.put(nodeId.keySet().iterator().next(), deviceNodeId);
            this.pceNodeMap.put(xpdrKey,new PceTapiOpticalNode(serviceType, this.node, OpenroadmNodeType.XPONDER,
                 version, slotWidthGranularity, centralFreqGranularity, allOtsNep, nodeId, deviceNodeId));
            return;
        }
        LOG.debug("validateAZxponder: XPONDER == Id: {}, Name : {} is ignored", node.getUuid().toString(),
            node.getName().toString());
        valid = false;
    }

    public void initTapiXndrTps(ServiceFormat serviceFormat, Uuid anodeId, Uuid znodeId,
            Uuid aportId, Uuid zportId) {
        //This function allows retrieving NEPs for Transponder
        //it identifies valid NETWORK TPs (the one that don't have any wavelength configured)
        //checking ownedNodeEdgePoint/cep-list/connection-end-point/
        //      tapi-photonic-media:otsi-mc-connection-end-point-spec/otsi-termination-pac/selected-central-frequency
        // TODO: need to modify the following code to get the right augmentation as R2.4 of T-API becomes available
        // Not .augmentation(ConnectionEndPoint1.class).getOtsiConnectionEndPointSpec()
        // but .augmentation(ConnectionEndPointXXXX.class).getOtsiMcConnectionEndPointSpec()
        if (!this.valid) {
            return;
        }
        Map<DirectionType, OpenroadmTpType> direction;
        LOG.debug("initTapiXndrTps: getting tps from TSP node {}", this.nodeUuid);
        Map<OwnedNodeEdgePointKey,OwnedNodeEdgePoint> ownedNepList = this.node.getOwnedNodeEdgePoint();
        for (Map.Entry<OwnedNodeEdgePointKey,OwnedNodeEdgePoint> ownedNep : ownedNepList.entrySet()) {
            if (LayerProtocolName.PHOTONICMEDIA.equals(ownedNep.getValue().getLayerProtocolName())
                && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())
                && isNepWithGoodCapabilities(ownedNep.getValue().getUuid())) {
                if (ownedNep.getValue().getSupportedCepLayerProtocolQualifier()
                        .contains(PHOTONICLAYERQUALIFIEROTSi.VALUE)) {
                    // This the case for NetworkPorts that already have a provisioned Lambda
                    // Check whether supportedCepLayerProtocolQualifier corresponds to what can be supported by the node
                    // or what is already provisioned on the node (current assumption)
                    // TODO: Need to change OTSI to OTSI Mc in 2.4
                    invalidNwNepList.add(ownedNep.getKey().getUuid());
                    //TODO: at the end scan the list of invalid NW port and remove the NEP from which they client/Parent
                    continue;
                }
                if (checkUsedSpectrum(ownedNep.getValue().getUuid(), true)) {
                    invalidNwNepList.add(ownedNep.getKey().getUuid());
                    continue;
                }
                if (ownedNep.getValue().getSupportedCepLayerProtocolQualifier()
                        .contains(PHOTONICLAYERQUALIFIEROTS.VALUE)) {
                    for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> cep : ownedNep.getValue()
                        .augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint().entrySet()) {
                        //We check if there is OTS level (ConnectionEndPoint4.class) CEP to get its child NEP
                        // Which shall exist even if no service is provisioned.
                        // if no service provisioned the client NEP shall not be parent of any CEP
                        if (cep.getValue().augmentation(ConnectionEndPoint4.class).getOtsConnectionEndPointSpec()
                                != null) {
                            var otsCep = new BasePceNep(cep.getValue().getUuid(), cep.getValue().getName());
                            otsCep.setParentNep(cep.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid());
                            otsCep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                    .getNodeEdgePointUuid());
                            otsLcpNWList.add(otsCep);
                            //If the client NEP of the CEP is already used we do not record the CEP in nwOtsCEP
                            if (invalidNwNepList.contains(cep.getValue().getClientNodeEdgePoint().keySet().iterator()
                                .next().getNodeEdgePointUuid())) {
                                break;
                            }
                            var otsNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                            otsNep.setCepOtsUuid(cep.getValue().getUuid());
                            otsNep.setClientNep(cep.getValue().getClientNodeEdgePoint().keySet().iterator().next()
                                    .getNodeEdgePointUuid());
                            otsNep.setOperationalState(ownedNep.getValue().getOperationalState());
                            direction = calculateDirection(ownedNep.getValue(), null, TpType.CLIENT);
                            otsNep.setDirection(direction.keySet().iterator().next());
                            otsNep.setTpType(direction.values().iterator().next());
                            direction.clear();
                            otsNep.setAdminState(ownedNep.getValue().getAdministrativeState());
                            otsNep.setFrequencyBitset(buildBitsetFromSpectrum(ownedNep.getValue().getUuid()));
                            nwOtsNep.add(otsNep);
                            break;
                        }
                    }
                }
            // Client Port support DSR protocol layer
            } else if (LayerProtocolName.DSR.equals(ownedNep.getValue().getLayerProtocolName())
                    && OperationalState.ENABLED.equals(ownedNep.getValue().getOperationalState())) {
                var clientNep = new BasePceNep(ownedNep.getValue().getUuid(), ownedNep.getValue().getName());
                direction = calculateDirection(ownedNep.getValue(), null, TpType.CLIENT);
                clientNep.setDirection(direction.keySet().iterator().next());
                clientNep.setTpType(direction.values().iterator().next());
                direction.clear();
                clientNep.setOperationalState(ownedNep.getValue().getOperationalState());
                //check if spectrum shall be populated here
                clientDsrNep.add(clientNep);
            }
        }
        //Purge otsLcpNwList and nwOTsNEP list since NEP associated to OTS and OTSIMC may not appear in the
        //expected order
        for (Uuid nepUuid : invalidNwNepList) {
            otsLcpNWList = otsLcpNWList.stream()
                .filter(bpn -> !(bpn.getClientNep().equals(nepUuid)))
                .collect(Collectors.toList());
            nwOtsNep = nwOtsNep.stream()
                .filter(bpn -> !(bpn.getClientNep().equals(nepUuid)))
                .collect(Collectors.toList());
        }
        String portType = "nw";
        if (aportId == null || zportId == null) {
            LOG.info("InitTapiXndr launched for Node of Id {}, name {} with no specified port imposed for a/z end,"
                + "all valid NW ports validated", node.getUuid().toString(), node.getName().toString());
            return;
        }
        Uuid portId = nwOtsNep
            .stream().filter(bpn -> (bpn.getNepCepUuid().equals(aportId) || bpn.getNepCepUuid().equals(zportId)))
            .findFirst().orElseThrow()
            .getNepCepUuid();
        if (portId == null) {
            portId = clientDsrNep
                .stream().filter(bpn -> (bpn.getNepCepUuid().equals(aportId) || bpn.getNepCepUuid().equals(zportId)))
                .findFirst().orElseThrow()
                .getNepCepUuid();
            if (portId != null) {
                portType = "client";
                LOG.debug("InitTapiXndr launched for Node of Id {}, name {}, {} port {} validated for a/z end."
                    + "Calling purgeXndrPortList to remove all NW ports that are not good candidates (not connected"
                    + "to the client port of the service request",
                    node.getUuid().toString(), node.getName().toString(), portType, portId);
                nwOtsNep = purgeXndrPortList(portId, nwOtsNep);
                return;
            } else {
                portType = "";
                LOG.error("InitTapiXndr launched for Node of Id {}, name {}, but no port validated for a/z end",
                    node.getUuid().toString(), node.getName().toString());
                return;
            }
        }
        // Applies to condition portId found in nwOtsNep, meaning service applies to NW
        // port of the Xponder
        LOG.debug("InitTapiXndr launched for Node of Id {}, name {}, {} port {} validated for a/z end",
            node.getUuid().toString(), node.getName().toString(), portType, portId);
        BasePceNep bpNEP = nwOtsNep.stream()
            .filter(bpn -> (bpn.getNepCepUuid().equals(aportId) || bpn.getNepCepUuid().equals(zportId)))
            .findAny().orElseThrow();
        nwOtsNep.clear();
        nwOtsNep.add(bpNEP);
        return;
    }

    //This method purges the nwOtsNep List removing all network ports that do not have connectivity to the designated
    // client port, analyzing the content of the Node-rule-groups and the Inter-rule groups
    //
    // Options considered for Xponder NodeRuleGroup (NRG) and InterRuleGroup (IRG) layout
    //
    // Option1 :   | client ----\            Option2 :
    //             | client----\ \___NW                  Nx NRG (client---NW)
    //      Nx NRG {      ...    /                       N: Nbre of Tuple associating client and network port
    //             | client----//                    Typical configuration of Multi-port Transponder or Muxponder
    //             | client----/                     with no possibility of hair-pining between client ports
    //     N: number of Network port
    //     Typical configuration for Muxponder/switchPonders
    //     with hair-pining capability between clients
    //
    // Option 3 : NRGi (client,...., client) Forwarding (Hair-pining) or not
    //            NRGk (NW,...,NW) Forwarding (Regen function creating loop between NW ports)or not
    //            IRG (NRGi,....NRGk) forwarding
    //          Typical configuration for Mux and SwitchPonders
    //
    public List<BasePceNep> purgeXndrPortList(Uuid clientPortId, List<BasePceNep> netOtsNep) {
        // nrgList will contains Uuid of all NRG containing the clientPortId but no Network port
        List<Uuid> nrgList = new ArrayList<>();
        // totalNwOtsNepIdList will contain all NW port key of the NW ports that appeared in a Nep List of a NRG with
        // the clientPortId
        List<NodeEdgePointKey> totalNwOtsNepIdList = new ArrayList<>();
        // nwOtsNepIdList intermediate list corresponding to a NRG used to build totalNwOtsNepIdList
        List<NodeEdgePointKey> nwOtsNepIdList;
        // nrgOfNwNepMap will contain the Uuid of NRGs containing NW ports that are not associated to clientPortId and
        // the list of corresponding NW ports
        Map<Uuid, List<NodeEdgePointKey>> nrgOfNwNepMap = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nrgMap = this.node.getNodeRuleGroup();
//        final Map<Uuid, BasePceNep> mNwNep = nwOtsNep.stream()
//            .collect(Collectors.toMap(BasePceNep::getUuid, Function.identity()));
//        final List<BasePceNep> nwNep = nwOtsNep;
        List<Uuid> nwOtsNepKeyList = netOtsNep.stream().map(BasePceNep::getNepCepUuid)
            .collect(Collectors.toList());
        for (Map.Entry<NodeRuleGroupKey, NodeRuleGroup> nrg : nrgMap.entrySet()) {
            // For each NodeRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
            // and if this is the case store its key in fwRuleKeyList
            List<RuleKey> fwdRuleKeyList =
                nrg.getValue().getRule().entrySet().stream()
                .filter(rule -> RuleType.FORWARDING.equals(rule.getValue().getRuleType()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
            if (fwdRuleKeyList == null || fwdRuleKeyList.isEmpty()) {
                // If the Node Rule Group (NRG) does not contain any forwarding rule, we go to next NRG
                break;
            } else {
                // We have one or several forwarding rule(s)
                if (nrg.getValue().getNodeEdgePoint().entrySet().stream()
                        .filter(nep -> nep.getKey().getNodeEdgePointUuid().equals(clientPortId))
                        .findAny().isPresent()) {
                    // if the nep list contains the clientPortId
                    // We create a list<NodeEdgePointKey> which includes Keys of all NW NEP recorded in the nwOtsNepList
                    nwOtsNepIdList = nrg.getValue().getNodeEdgePoint().entrySet().stream()
                        .filter(nep -> nwOtsNepKeyList.contains(nep.getKey().getNodeEdgePointUuid()))
                        .map(Map.Entry::getKey).collect(Collectors.toList());
                    if (!nwOtsNepIdList.isEmpty()) {
                        //This is the case of Options 1 & 2 where we found the clientPort associated to NW NEP
                        totalNwOtsNepIdList.addAll(nwOtsNepIdList);
                        continue;
                    } else {
                        //This is the case of Option 3 where the client port is not associated to any NW ports of the
                        //nwOtsNepList. We record in nrgList the id of the nrg that includes the clientPortId
                        nrgList.add(nrg.getKey().getUuid());
                    }
                } else {
                     // The clientPort is not found in the list of NEP corresponding to the NRG
                    nrgOfNwNepMap.put(nrg.getKey().getUuid(), nrg.getValue().getNodeEdgePoint().entrySet().stream()
                        .filter(nep -> nwOtsNepKeyList.contains(nep.getKey().getNodeEdgePointUuid()))
                        .map(Map.Entry::getKey).collect(Collectors.toList()));
                }
            }
            // When no forwarding Rule defined go to next NodeRuleGroup
        }
        LOG.debug("purgeXndrPortList: nrgList contains {}", nrgList.toString());
        LOG.debug("purgeXndrPortList: nrgOfNwNepMap contains {}", nrgOfNwNepMap.toString());
        // All NRGs have been analyzed
        if (!totalNwOtsNepIdList.isEmpty()) {
            // We found some NW ports associated with the client port in one or several NRG
            List<Uuid> totalNwOtsNepUuidList = totalNwOtsNepIdList.stream()
                .map(NodeEdgePointKey::getNodeEdgePointUuid)
                .collect(Collectors.toList());
            netOtsNep = netOtsNep.stream()
                .filter(bpn -> totalNwOtsNepUuidList.contains(bpn.getNepCepUuid()))
                .collect(Collectors.toList());
            //function exit for option 1 & 2 : nwOtsNep contains only relevant NW NEP that are associated to relevant
            //client port
            return netOtsNep;
        }
        // Did not succeed to find client port and NW ports in the same NRG, need to process Inter Rule Group


        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        //To be activated after the migration to TAPI 2.4 (IRG not handled at the same level in 2.1 and 2.4
//        InterRuleGroup<KeyRuleGroupKey, InterRuleGroup> irgMap = this.node.getInterRuleGroup();
//        for (Map.Entry<InterRuleGroupKey, InterRuleGroup> irg : nrgMap.entrySet()) {
//            // For each NodeRuleGroup [uuid], we check if some of the rule [local-id] are a forwarding rule
//            // and if this is the case store its key in fwRuleKeyList
//            List<RuleKey> fwdRuleKeyList =
//                    irg.getValue().getRule().entrySet().stream()
//                        .filter(rule -> RuleType.FORWARDING.equals(rule.getValue().getRuleType()))
//                        .map(Map.Entry::getKey).collect(Collectors.toList());
//                for (RuleKey rk : fwdRuleKeyList) {
//                    if (ForwardingRule.MAYFORWARDACROSSGROUP
//                            .equals(irg.getValue().getRule().get(rk).getForwardingRule())
//                            || ForwardingRule.MUSTFORWARDACROSSGROUP
//                            .equals(irg.getValue().getRule().get(rk).getForwardingRule())) {
//                       // We are in Contention-less Add/drop Block or a degree, or in a node where FWding condition
//                       // is defined across all NEPs
//                       AssociatedNodeRuleGroup anrg = irg.getValue().getAssociatedNodeRuleGroup();
//                       for (Uuid uuid : nrgList) {
//                           if (anrg.contains(uuid)) {
//                               for (Map.Entry<Uuid, List<NodeEdgePointKey>>  nrg : nrgOfNwNepMap.entrySet()) {
//                                   if (anrg.contains(nrg.getKey())) {
//                                       totalNwOtsNepIdList.addAll(nrg.getValue());
//                                   }
//                               }
//                           }
//                       }
//                    }
//                }
//        }
//        // We found some NW ports associated with the client port several NRG to which refer one or several IRG
//        // TO VERIFY : can we do a contains Uuid whereas the list is not a simple list of Uuid but a list of tripple
//        // Key from which only one is an Uuid
//        netOtsNep = netOtsNep.stream()
//            .filter(bpn -> totalNwOtsNepIdList.contains(bpn.getUuid()))
//            .collect(Collectors.toList());
//        //function exit for option 3 : nwOtsNep contains only relevant NW NEP that are associated to relevant
//        //client port
        return netOtsNep;
        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    }

    private void createLinksFromIlMap() {
        //TODO: complete this methode
    }

    // This method provides from a frequency expressed in Hz (T-API) the adjacent upper Slot Number in a Flexgrid
    // Slot Number = N + 285, N being an integer varying from -284 to 484 for C band as defined by IEEE.
    private int convertFreqToAdjacentSlotNumber(long frequency) {
        return (int) Math.round((frequency / GridConstant.HZ_TO_THZ - GridConstant.ANCHOR_FREQUENCY)
            / (GridConstant.GRANULARITY / 1000.0) + 285);
    }

    private BitSet buildBitsetFromSpectrum(Uuid onepUuid) {
        //At init, sets all bits to false (unavailable/0) from 0 inclusive to highest index exclusive))
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        // to set all bits to 0 (used) or 1 (available)
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, false);
        //scans all spectrum portions defined in the available spectrum of the
        OwnedNodeEdgePoint ooNep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> onepUuid.equals(onep.getKey().getUuid())).findFirst().orElseThrow().getValue();
        // TODO: Change in 2.4 type from MediaChannelNodeEdgePointSpec (2.1.1) to  photonicMediaNodeEdgePointSpec (2.4)
        Map<AvailableSpectrumKey, AvailableSpectrum> avSpectrum = ooNep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.OwnedNodeEdgePoint1.class)
            .getMediaChannelNodeEdgePointSpec().getMcPool().nonnullAvailableSpectrum();
        if (!(avSpectrum == null || avSpectrum.isEmpty())) {
            for (Map.Entry<AvailableSpectrumKey, AvailableSpectrum> as : avSpectrum.entrySet()) {
                int lowSlotNumber = convertFreqToAdjacentSlotNumber(as.getValue().getLowerFrequency().longValue());
                int highSlotNumber = convertFreqToAdjacentSlotNumber(as.getValue().getUpperFrequency().longValue());
                if (!(lowSlotNumber > GridConstant.EFFECTIVE_BITS || highSlotNumber <= 1)) {
                    // If the one of the boundaries is included in C band, this is a spectrum portion of interest
                    // Set all bits of available spectrum to true (1) from lower slot Number starting at 0 inclusive
                    // to highest index exclusive
                    freqBitSet.set(java.lang.Math.max(0, lowSlotNumber - 1),
                        java.lang.Math.min(highSlotNumber - 1, GridConstant.EFFECTIVE_BITS - 1));
                }
            }
        }
        return freqBitSet;
    }

    private boolean checkOtsNepAvailable(Uuid otsNepUuid) {
        // Check that used spectrum is empty or null which is the condition of availability for a PP
        return true;
    }

    private boolean isNepWithGoodCapabilities(Uuid nepUuid) {
        if (StringConstants.UNKNOWN_MODE.equals(getXpdrOperationalMode(nepUuid))) {
            return false;
        }
        return true;
    }

    public String getXpdrOperationalMode(Uuid nepUuid) {
        // TODO: when 2.4 models available, retrieve Operational modes from Profiles
        List<String> supportedOM = new ArrayList<>();

        if (supportedOM == null || supportedOM.isEmpty()) {
            LOG.warn("getOperationalMode: NetworkPort {} of Node {}  with Uuid {} has no operational mode declared ",
                nepUuid, this.nodeName.toString(), this.nodeUuid);
            return StringConstants.UNKNOWN_MODE;
        }
        for (String operationalMode : supportedOM) {
            if (operationalMode.contains(StringConstants.SERVICE_TYPE_RATE
                .get(this.serviceType).toCanonicalString())) {
                LOG.info(
                    "getOperationalMode: NetworkPort {} of Node {}  with Uuid {}  has {} operational mode declared",
                    nepUuid, this.nodeName.toString(), this.nodeUuid, operationalMode);
                return operationalMode;
            }
        }
        LOG.warn("getOperationalMode: NetworkPort {} of Node {}  with Uuid {} has no operational mode declared"
            + "compatible with service type {}. Supported modes are : {} ",
            nepUuid, this.nodeName.toString(), this.nodeUuid, this.serviceType, supportedOM.toString());
        return StringConstants.UNKNOWN_MODE;
    }

    private boolean checkAvailableSpectrum(Uuid lcpUuid, boolean isNEP) {
        //Use to check that there are some availabilities in the spectrum
        //scans all spectrum portions defined in the available spectrum of the
        boolean availableCbandSpectrum = false;
        OwnedNodeEdgePoint ooNep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> lcpUuid.equals(onep.getKey().getUuid())).findFirst().orElseThrow().getValue();
        // TODO: Change in 2.4 type from MediaChannelNodeEdgePointSpec (2.1.1) to  photonicMediaNodeEdgePointSpec (2.4)
        // Currently not used with something else than NEP. Complete function if needed for other LCP, or remove isNEP
        Map<AvailableSpectrumKey, AvailableSpectrum> aaSpectrum = ooNep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.OwnedNodeEdgePoint1.class)
            .getMediaChannelNodeEdgePointSpec().getMcPool().nonnullAvailableSpectrum();
        if (!(aaSpectrum == null || aaSpectrum.isEmpty())) {
            for (Map.Entry<AvailableSpectrumKey, AvailableSpectrum> as : aaSpectrum.entrySet()) {
                if (!(convertFreqToAdjacentSlotNumber(as.getValue().getLowerFrequency().longValue())
                        > GridConstant.EFFECTIVE_BITS
                        || convertFreqToAdjacentSlotNumber(as.getValue().getUpperFrequency().longValue()) <= 1)) {
                    // If the one of the boundaries is included in C band, this is a spectrum portion of interest
                    availableCbandSpectrum = true;
                }
            }
        }
        return availableCbandSpectrum;
    }

    private boolean checkUsedSpectrum(Uuid lcpUuid, boolean isNEP) {
        // Use to check that a port is not already used : currently used for TSP NWTPs,
        // but could be used also for PP
        // scans all spectrum portions defined in the occupied spectrum
        boolean usedCbandSpectrum = true;
        OwnedNodeEdgePoint ooNep = this.node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(onep -> lcpUuid.equals(onep.getKey().getUuid())).findFirst().orElseThrow().getValue();
        // TODO: Change in 2.4 type from MediaChannelNodeEdgePointSpec (2.1.1) to
        // photonicMediaNodeEdgePointSpec (2.4)
        // Currently not used with something else than NEP. Complete function if needed
        // for other LCP, or remove isNEP
        Map<OccupiedSpectrumKey, OccupiedSpectrum> ooSpectrum = ooNep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.OwnedNodeEdgePoint1.class)
            .getMediaChannelNodeEdgePointSpec().getMcPool().nonnullOccupiedSpectrum();
        if (!(ooSpectrum == null || ooSpectrum.isEmpty())) {
            usedCbandSpectrum = false;
        }
        return usedCbandSpectrum;
    }

//    private Map<Uuid, Name> createIrgPartialMesh(Map<Integer, Map<NodeEdgePointKey, NodeEdgePoint>> intercoNepMap) {
//      //Create a Full Mesh between VirtualNEPs associated to all combination of 2 NEPs from each Map of intercoNepMap
//        // without duplicate. Consider that this is a partial mesh, as we create less Links than if we were creating
//        // all the links between any combination of NEPs of the 2 maps of intercoNepMap
//        Map<Uuid, Name> internLinkMap = new HashMap<>();
//        List<String> uuidSortedList = new ArrayList<String>();
//        Map<NodeEdgePointKey, NodeEdgePoint> nepMap1 = new HashMap<>();
//        nepMap1 = intercoNepMap.get(0);
//        Map<NodeEdgePointKey, NodeEdgePoint> nepMap2 = new HashMap<>();
//        nepMap2 = intercoNepMap.get(1);
//        String orgNodeType;
//        String destNodeType;
//        for (Map.Entry<NodeEdgePointKey, NodeEdgePoint> nep1 : nepMap1.entrySet()) {
//            BasePceNep orgBpn = srgOtsNep.stream().filter(bpn -> nep1.getKey().getNodeEdgePointUuid()
//                .equals(bpn.getUuid())).findFirst().get();
//            orgNodeType = "SRG";
//            if (orgBpn == null) {
//                orgBpn = degOtsNep.stream().filter(bpn -> nep1.getKey().getNodeEdgePointUuid()
//                    .equals(bpn.getUuid())).findFirst().get();
//                orgNodeType = "DEG";
//            }
//            if (orgBpn == null) {
//                break;
//            }
//            Uuid orgVnepUuid = orgBpn.getVirtualNep().entrySet().iterator().next().getKey();
//            String orgVnepName = orgBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
//            for (Map.Entry<NodeEdgePointKey, NodeEdgePoint> nep2 : nepMap2.entrySet()) {
//                BasePceNep destBpn = srgOtsNep.stream().filter(bpn -> nep2.getKey().getNodeEdgePointUuid()
//                    .equals(bpn.getUuid())).findFirst().get();
//                destNodeType = "SRG";
//                if (destBpn == null) {
//                    destBpn = degOtsNep.stream().filter(bpn -> nep2.getKey().getNodeEdgePointUuid()
//                        .equals(bpn.getUuid())).findFirst().get();
//                    destNodeType = "DEG";
//                }
//                if (destBpn == null) {
//                    break;
//                }
//                Uuid destVnepUuid = destBpn.getVirtualNep().entrySet().iterator().next().getKey();
//                String destVnepName = destBpn.getVirtualNep().entrySet().iterator().next().getValue().getValue();
//                uuidSortedList.clear();
//                uuidSortedList.add(orgVnepUuid.toString());
//                uuidSortedList.add(destVnepUuid.toString());
//                Collections.sort(uuidSortedList);
//                if (orgVnepUuid.toString().equals(uuidSortedList.get(0))) {
//                    Map<Uuid, Name> linkId = createLinkId(orgVnepName, destVnepName);
//                    internLinkMap.putAll(linkId);
//                    addCpCtpOutgoingLink(orgVnepUuid, destVnepUuid, orgNodeType, destNodeType, linkId);
//                } else {
//                    Map<Uuid, Name> linkId = createLinkId(destVnepName, orgVnepName);
//                    internLinkMap.putAll(linkId);
//                    addCpCtpOutgoingLink(destVnepUuid, orgVnepUuid, destNodeType, orgNodeType, linkId);
//                }
//            }
//        }
//        return internLinkMap;
//    }

    public Map<Map<Uuid, String>, PceTapiOpticalNode> getPceNodeMap() {
        return this.pceNodeMap;
    }
}
