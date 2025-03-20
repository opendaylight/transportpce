/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPEGigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMUSTFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PceTapiOtnNode implements PceNode {


    private static final Logger LOG = LoggerFactory.getLogger(PceTapiOtnNode.class);
    private static final List<String> SERVICE_TYPE_ODU_LIST = List.of(
        StringConstants.SERVICE_TYPE_ODU4,
        StringConstants.SERVICE_TYPE_ODUC4,
        StringConstants.SERVICE_TYPE_ODUC3,
        StringConstants.SERVICE_TYPE_ODUC2);
    private static final List<OpenroadmNodeType> VALID_NODETYPES_LIST = List.of(
        OpenroadmNodeType.XPONDER,
        OpenroadmNodeType.MUXPDR,
        OpenroadmNodeType.SWITCH,
        OpenroadmNodeType.TPDR);
    private static final Map<String, SupportedIfCapability> SERVICE_TYPE_ETH_CLASS_MAP = Map.of(
        StringConstants.SERVICE_TYPE_1GE, If1GEODU0.VALUE,
        StringConstants.SERVICE_TYPE_10GE, If10GEODU2e.VALUE,
        StringConstants.SERVICE_TYPE_100GE_M, If100GEODU4.VALUE,
        StringConstants.SERVICE_TYPE_100GE_S, If100GEODU4.VALUE);
    private static final Map<String, Integer> SERVICE_TYPE_ETH_TS_NB_MAP = Map.of(
        StringConstants.SERVICE_TYPE_1GE, 1,
        StringConstants.SERVICE_TYPE_10GE, 10,
        StringConstants.SERVICE_TYPE_100GE_M, 20);
    private static final Map<String, String> SERVICE_TYPE_ETH_ODU_STRING_MAP = Map.of(
        StringConstants.SERVICE_TYPE_1GE, "ODU0",
        StringConstants.SERVICE_TYPE_10GE, "ODU2e",
        StringConstants.SERVICE_TYPE_100GE_M, "ODU4");
    private static final Map<String, Map<String, Map<LAYERPROTOCOLQUALIFIER, Uint64>>> LPN_MAP;

    static {
        LPN_MAP = new HashMap<>(Map.of(
            "ETH", new HashMap<>(Map.of(
                "If1GEODU0", Map.of(
                    ODUTYPEODU0.VALUE, Uint64.ZERO, DIGITALSIGNALTYPEGigE.VALUE, Uint64.ZERO),
                "If10GEODU2e", Map.of(
                    ODUTYPEODU2E.VALUE, Uint64.ZERO, DIGITALSIGNALTYPE10GigELAN.VALUE, Uint64.ZERO),
                "If10GEODU2", Map.of(
                    ODUTYPEODU2.VALUE, Uint64.ZERO, DIGITALSIGNALTYPE10GigELAN.VALUE, Uint64.ZERO),
                "If10GE", Map.of(DIGITALSIGNALTYPE10GigELAN.VALUE, Uint64.ZERO),
                "If100GEODU4", Map.of(
                    ODUTYPEODU4.VALUE, Uint64.ZERO, DIGITALSIGNALTYPE100GigE.VALUE, Uint64.ZERO),
                "If100GE", Map.of(DIGITALSIGNALTYPE100GigE.VALUE, Uint64.ZERO),
                //"IfOCH", Map.of(ODUTYPEODU4.VALUE, Uint64.ZERO))),
                "IfOCH", Map.of(ODUTYPEODU4.VALUE, Uint64.ZERO, OTUTYPEOTU4.VALUE, Uint64.ZERO))),
            "OTU", new HashMap<>(Map.of(
                "IfOCHOTUCnODUCn",
                    Map.of(OTUTYPEOTUCN.VALUE, Uint64.ONE),
                "IfOCH",
                    Map.of(OTUTYPEOTU4.VALUE, Uint64.ONE),
                "IfOCHOTU4ODU4",
                    Map.of(OTUTYPEOTU4.VALUE, Uint64.ZERO))),
            "ODU", new HashMap<>(Map.of(
                "If1GEODU0", Map.of(ODUTYPEODU0.VALUE, Uint64.ZERO),
                "If10GEODU2e", Map.of(ODUTYPEODU2E.VALUE, Uint64.ZERO),
                "If10GEODU2", Map.of(ODUTYPEODU2.VALUE, Uint64.ZERO),
                "If100GEODU4", Map.of(ODUTYPEODU4.VALUE, Uint64.ZERO),
                "IfOCHOTUCnODUCn", Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4), ODUTYPEODUCN.VALUE, Uint64.ONE),
                "IfOCH", Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4)),
                "IfOCHOTU4ODU4", Map.of(ODUTYPEODU4.VALUE, Uint64.ZERO))),
            "DIGITAL_OTN", new HashMap<>(Map.of(
                "If1GEODU0", Map.of(ODUTYPEODU0.VALUE, Uint64.ZERO),
                "If10GEODU2e", Map.of(ODUTYPEODU2E.VALUE, Uint64.ZERO),
                "If10GEODU2", Map.of(ODUTYPEODU2.VALUE, Uint64.ZERO),
                "If100GEODU4", Map.of(ODUTYPEODU4.VALUE, Uint64.ZERO),
                "IfOCHOTUCnODUCn",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4), ODUTYPEODUCN.VALUE, Uint64.ONE,
                        OTUTYPEOTUCN.VALUE, Uint64.ONE),
                "IfOCH",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4)),
                "IfOCHOTU4ODU4",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.ZERO, OTUTYPEOTU4.VALUE, Uint64.ZERO))),
            "PHOTONIC_MEDIA", new HashMap<>(Map.of(
                "IfOCHOTUCnODUCn",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4), ODUTYPEODUCN.VALUE, Uint64.ONE,
                        OTUTYPEOTUCN.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTSiMC.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTS.VALUE, Uint64.ONE),
                "IfOCH",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(4),
                        PHOTONICLAYERQUALIFIEROTSiMC.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTS.VALUE, Uint64.ONE),
                "IfOCHOTU4ODU4",
                    Map.of(ODUTYPEODU4.VALUE, Uint64.ZERO, OTUTYPEOTU4.VALUE, Uint64.ZERO,
                        PHOTONICLAYERQUALIFIEROTSiMC.VALUE, Uint64.ONE,
                        PHOTONICLAYERQUALIFIEROTS.VALUE, Uint64.ONE)
             ))
            ));
        LPN_MAP.put("DSR", LPN_MAP.get("ETH"));
        LPN_MAP.get("ODU").put("If10GE", LPN_MAP.get("ODU").get("If10GEODU2"));
        LPN_MAP.get("ODU").put("If100GE", LPN_MAP.get("ODU").get("If100GEODU4"));
        LPN_MAP.get("DIGITAL_OTN").put("If10GE", LPN_MAP.get("ODU").get("If10GEODU2"));
        LPN_MAP.get("DIGITAL_OTN").put("If100GE", LPN_MAP.get("ODU").get("If100GEODU4"));
        LPN_MAP.get("PHOTONIC_MEDIA").put("IfOtsiOtucnOducn", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTUCnODUCn"));
        LPN_MAP.get("PHOTONIC_MEDIA").put("IfOCHOTUCnODUCnRegen", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTUCnODUCn"));
        LPN_MAP
            .get("PHOTONIC_MEDIA").put("IfOCHOTUCnODUCnUniregen", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTUCnODUCn"));
        LPN_MAP.get("PHOTONIC_MEDIA").put("IfOCHOTU4ODU4Regen", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTU4ODU4"));
        LPN_MAP.get("PHOTONIC_MEDIA").put("IfOCHOTU4ODU4Uniregen", LPN_MAP.get("PHOTONIC_MEDIA").get("IfOCHOTU4ODU4"));
    }

    private static final String SCL_ODU = "ODU";
    private static final String SCL_OTU = "OTU";
    private static final String INTERMEDIATE_MODETYPE = "intermediate";
    private static final String AZ_MODETYPE = "AZ";

    private boolean valid = true;

    private final Node node;
    private final String nodeId;
    private final OpenroadmNodeType nodeType;
//    private final String pceNodeType;
    private final String otnServiceType;
    private String modeType;
    private AdministrativeState adminState;
    private OperationalState operState;

    private Map<String, List<Uint16>> tpAvailableTribPort = new TreeMap<>();
    private Map<String, List<Uint16>> tpAvailableTribSlot = new TreeMap<>();
    private Map<String, OpenroadmTpType> availableXponderTp = new TreeMap<>();
    private List<String> usedXpdrNWTps = new ArrayList<>();
    private List<BasePceNep> availableXpdrNWTps;
    private List<BasePceNep> usableXpdrNWTps;
    private List<String> usedXpdrClientTps = new ArrayList<>();
    private List<BasePceNep> availableXpdrClientTps;
    private List<BasePceNep> usableXpdrClientTps;

    private List<PceLink> outgoingLinks = new ArrayList<>();
    private Map<String, String> clientPerNwTp = new HashMap<>();
    private Uuid clientPortId;
    private String supConLayer;
    private TapiOpticalNode tapiON;

    public PceTapiOtnNode(Node node, OpenroadmNodeType nodeType, String deviceNodeId, String serviceType,
        Uuid clientPort, TapiOpticalNode ton) {
        this.node = node;
        this.tapiON = ton;
        this.nodeId = deviceNodeId;
        this.nodeType = nodeType;
//        this.pceNodeType = pceNodeType;
        this.otnServiceType = serviceType;
        this.tpAvailableTribSlot.clear();
        this.usedXpdrNWTps.clear();
        this.availableXpdrNWTps = new ArrayList<>();
        this.usableXpdrNWTps = new ArrayList<>();
        this.usedXpdrClientTps.clear();
        this.availableXpdrClientTps = new ArrayList<>();
        this.usableXpdrClientTps = new ArrayList<>();
        this.adminState = node.getAdministrativeState();
        this.operState = node.getOperationalState();
//        this.tpAvailableTribPort.clear();
//        initializeAvailableTribPort();
//        this.tpAvailableTribSlot.clear();
//        initializeAvailableTribSlot();
        this.modeType = null;
        this.clientPortId = clientPort;
        if (node == null
                || deviceNodeId == null
                || nodeType == null
                || !VALID_NODETYPES_LIST.contains(nodeType)) {
            LOG.error("PceTapiOtnNode: one of parameters is not populated : nodeId, node type");
            this.valid = false;
        }
        if (!SERVICE_TYPE_ETH_CLASS_MAP.containsKey(serviceType)
                && !SERVICE_TYPE_ODU_LIST.contains(serviceType)) {
            LOG.error("PceOtnNode: unsupported OTN Service Type {}", serviceType);
            this.valid = false;
        }
    }

    public void initXndrTps(String mode) {
        LOG.debug("PceTapiOtnNode: initXndrTps for node {}", this.nodeId);
        this.availableXpdrClientTps.clear();
        this.availableXpdrNWTps.clear();
        this.availableXponderTp.clear();
        this.modeType = mode;
        this.valid = false;

        if (SERVICE_TYPE_ODU_LIST.contains(otnServiceType)) {
            // We put OtuNep in clientDsr Nep the connection will between iOTU NEP. It is an NW NEP but doesn't need
            // to be identified as such (no pair of client-NW NEPs)
            availableXpdrClientTps = tapiON.getOtuNep();
            if (availableXpdrClientTps.isEmpty()) {
                return;
            }
            supConLayer = SCL_OTU;
            LOG.debug("Supporting connection layer is {}", supConLayer);
        } else if (SERVICE_TYPE_ETH_CLASS_MAP.containsKey(otnServiceType)) {
            availableXpdrClientTps = tapiON.getOduNep().stream()
                .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.XPONDERCLIENT))
                .collect(Collectors.toList());
            availableXpdrNWTps = tapiON.getOduNep().stream()
                .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.XPONDERNETWORK))
                .collect(Collectors.toList());
            int clientListSize = availableXpdrClientTps.size();
            if (availableXpdrClientTps.isEmpty()) {
                LOG.error("PceOtnNode: initXndrTps: XPONDER {} has no available eODU TP", nodeId);
                return;
            }
            availableXpdrClientTps.addAll(tapiON.getClientDsrNep().stream().distinct()
                .collect(Collectors.toList()));
            if (availableXpdrClientTps.size() == clientListSize) {
                LOG.error("PceOtnNode: initXndrTps: XPONDER {} has no available DSR TP", nodeId);
                return;
            }
            if (availableXpdrNWTps.isEmpty()) {
                LOG.error("PceOtnNode: initXndrTps: XPONDER {} has no available iODU TP", nodeId);
                return;
            }
            supConLayer = SCL_ODU;
        } else {
            LOG.error("PceOtnNode: initXndrTps: Unidentified Service Type {}", otnServiceType);
        }
        // Purge availableXpder-NW/Client-Tps from ports that are not directly or undirectly connected to clientPort
        availableXpdrClientTps.removeAll(availableXpdrClientTps.stream()
            .filter(bpn -> (!isValidBpn(bpn) || (!bpn.getVerticallyConnectedNep().contains(clientPortId))))
            .collect(Collectors.toList()));
        if (!availableXpdrClientTps.isEmpty()) {
            for (BasePceNep bpn : availableXpdrClientTps) {
                if (!isValidTp(bpn)) {
                    availableXpdrClientTps.remove(bpn);
                }
            }
        }
        if (availableXpdrNWTps != null && !availableXpdrNWTps.isEmpty()) {
            availableXpdrNWTps.removeAll(availableXpdrNWTps.stream()
                .filter(bpn -> !bpn.getVerticallyConnectedNep().contains(clientPortId) || isValidTp(bpn))
                .collect(Collectors.toList()));
        }

        if (SERVICE_TYPE_ETH_CLASS_MAP.containsKey(otnServiceType)) {
            this.valid = checkSwPool(availableXpdrNWTps, availableXpdrClientTps);
        }

    }

    private boolean isValidTp(BasePceNep bpn) {
        OwnedNodeEdgePoint onep = node.getOwnedNodeEdgePoint().entrySet().stream()
            .filter(nep -> nep.getKey().getUuid().equals(bpn.getNepCepUuid()))
            .findFirst().orElseThrow().getValue();
        if (SERVICE_TYPE_ODU_LIST.contains(otnServiceType)) {
            LAYERPROTOCOLQUALIFIER expectedLpn = LPN_MAP.get("DIGITAL_OTN")
                .get(SERVICE_TYPE_ETH_CLASS_MAP.get(otnServiceType).toString()).entrySet().stream()
                .filter(entry -> entry.getKey().toString().contains("ODU"))
                .findFirst().orElseThrow().getKey();
            return onep.getAvailablePayloadStructure().get(0).getMultiplexingSequence().contains(expectedLpn)
                && (onep.getAvailablePayloadStructure().get(0).getCapacity().getValue().doubleValue() > 0.0);
        } else if (SERVICE_TYPE_ETH_CLASS_MAP.containsKey(otnServiceType)) {
            LAYERPROTOCOLQUALIFIER expectedLpn = LPN_MAP.get("ETH")
                .get(SERVICE_TYPE_ETH_CLASS_MAP.get(otnServiceType).toString()).entrySet().stream()
                .filter(entry -> !entry.getKey().toString().contains("ODU"))
                .findFirst().orElseThrow().getKey();
            return onep.getAvailablePayloadStructure().get(0).getMultiplexingSequence().contains(expectedLpn)
                && (onep.getAvailablePayloadStructure().get(0).getCapacity().getValue().doubleValue() > 0.0);
        } else {
            LOG.warn("in checkTp of TapiOTNNode, Unidentified service type {}", otnServiceType);
        }

        return false;
    }

    public void validateXponder(String anodeId) {
        if (!isValid()) {
            return;
        }
        if (this.nodeId.equals(anodeId)) {
            initXndrTps(AZ_MODETYPE);
        } else if (OpenroadmNodeType.SWITCH.equals(this.nodeType)) {
            initXndrTps(INTERMEDIATE_MODETYPE);
        } else {
            LOG.warn("validateAZxponder: XPONDER is ignored == {}", nodeId);
            valid = false;
        }
    }

    private boolean isValidBpn(BasePceNep bpn) {
        if (clientPortId == null) {
            return true;
        }
        if (bpn.getSipUuid() == null) {
            LOG.debug("TONLine1021, null SIP for BPN {}", bpn.getName());
        }
        // Allows to qualify node validity whatever is the portId used : NEP Uuid, CEP Uuid or SIP Uuid. If CEP Uuid
        // or SIP Uuid is used, Bpn that has CEP/SIP corresponding to the PortId provided in the request will be
        // validated and PCE can later rely on NEP rather than CEP/SIP (Easier to handle notably since SIP model in
        //  the context does not include any reference to the NEP it is attached to!
        if ((clientPortId != null && bpn.getNepCepUuid().equals(clientPortId))
                || (clientPortId != null && bpn.getSipUuid() != null && bpn.getSipUuid().equals(clientPortId))
                || (clientPortId != null && getCepUuidFromParentNepUuid(bpn.getNepCepUuid()) != null
                    && getCepUuidFromParentNepUuid(bpn.getNepCepUuid()).equals(clientPortId))) {
            return true;
        } else {
            return false;
        }
    }

    private Uuid getCepUuidFromParentNepUuid(Uuid nepUuid) {
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

//    private Uuid getNepUuidFromCepUuid(Uuid cepUuid) {
//        for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> entry : node.getOwnedNodeEdgePoint().entrySet()) {
//            if (entry.getValue().augmentation(OwnedNodeEdgePoint1.class) != null
//                    && entry.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList() != null
//                    && !entry.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList()
//                    .getConnectionEndPoint().isEmpty()) {
//                if (!entry.getValue().augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
//                    .entrySet().stream()
//                    .filter(cep -> cep.getKey().getUuid().equals(cepUuid)
//                            && cep.getValue().getParentNodeEdgePoint() != null
//                            && cep.getValue().getParentNodeEdgePoint().getNodeEdgePointUuid()
//                                .equals(entry.getKey().getUuid()))
//                    .collect(Collectors.toList()).isEmpty()) {
//                    return entry.getKey().getUuid();
//                }
//            }
//        }
//        return null;
//    }

    private boolean checkAZSwPool(List<BasePceNep> netwTps, List<BasePceNep> clientTps) {
        // Check first if client Tps and Network tps have some common nrg with a Forwarding rule MAY/MUST
        // meaning client are connected to Nw port
        List<BasePceNep> eoduBpnList = clientTps.stream()
            .filter(bpn -> !bpn.getLpn().equals(LayerProtocolName.DSR)).collect(Collectors.toList());
        List<Uuid> clientNrgList = new ArrayList<>();
        for (BasePceNep bpn : eoduBpnList) {
            clientNrgList.addAll(bpn.getNodeRuleGroupUuid());
        }
        clientNrgList = clientNrgList.stream().distinct().collect(Collectors.toList());
        List<Uuid> nwNrgList = new ArrayList<>();
        for (BasePceNep bpn : netwTps) {
            nwNrgList.addAll(bpn.getNodeRuleGroupUuid());
        }
        nwNrgList = nwNrgList.stream().distinct().collect(Collectors.toList());
        for (Uuid clientNrg : clientNrgList) {
            if (nwNrgList.contains(clientNrg) && !node.getNodeRuleGroup().entrySet().stream()
                    .filter(nrg -> nrg.getKey().getUuid().equals(clientNrg)).findFirst().orElseThrow().getValue()
                        .getRule().entrySet().stream()
                            .filter(rule -> rule.getValue().getForwardingRule() != null
                                && (rule.getValue().getForwardingRule()
                                    .equals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                                || rule.getValue().getForwardingRule()
                                    .equals(FORWARDINGRULEMUSTFORWARDACROSSGROUP.VALUE)))
                            .collect(Collectors.toList())
                            .isEmpty()) {
                if (node.getNodeRuleGroup().entrySet().stream()
                        .filter(nrg -> nrg.getKey().getUuid().equals(clientNrg)).findFirst().orElseThrow().getValue()
                        .getAvailableCapacity().getTotalSize().getValue().doubleValue() > 0.0) {
                    // We found in NwNrgList a NRG that is shared with the client Tps, with Forwarding True, and a
                    // a bandwidth that allows for further connections
                    // We remove form availableXpdrNwTps any of the bpn that do not contain this SRG
                    availableXpdrNWTps.stream()
                        .filter(bpn -> !bpn.getNodeRuleGroupUuid().contains(clientNrg)).collect(Collectors.toList());
                    // We add in relevant list usableXpdrClient/NWTps the bpn that have the NRG in their list
                    usableXpdrClientTps.addAll(clientTps.stream()
                        .filter(bpn -> bpn.getNodeRuleGroupUuid().contains(clientNrg)).collect(Collectors.toList()));
                    usableXpdrNWTps.addAll(netwTps.stream()
                        .filter(bpn -> bpn.getNodeRuleGroupUuid().contains(clientNrg)).collect(Collectors.toList()));
                    return true;
                }
            }
        }

        // Being there means we did not find a common nrg with both one of the eODU and one iODU
        // Check if client Tps and Network tps have some nrgs that are interconnected through an IRG
        // with a Forwarding rule MAY/MUST meaning client are connected to Nw port
        for (Map.Entry<InterRuleGroupKey, InterRuleGroup> irg :node.getInterRuleGroup().entrySet()) {
            boolean nrgPresentInClient = false;
            boolean nrgPresentInNw = false;
            if (!irg.getValue().getRule().entrySet().stream()
                    .filter(rule -> rule.getValue().getForwardingRule() != null
                        && (rule.getValue().getForwardingRule()
                            .equals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                        || rule.getValue().getForwardingRule()
                            .equals(FORWARDINGRULEMUSTFORWARDACROSSGROUP.VALUE)))
                    .collect(Collectors.toList()).isEmpty()) {
                List<Uuid> clientIrgNrgUuidList = new ArrayList<>();
                List<Uuid> nwIrgNrgUuidList = new ArrayList<>();
                for (Map.Entry<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> anrg : irg.getValue()
                        .getAssociatedNodeRuleGroup().entrySet()) {
                    if (nwNrgList.contains(anrg.getKey().getNodeRuleGroupUuid())) {
                        nrgPresentInNw = true;
                        nwIrgNrgUuidList.add(anrg.getKey().getNodeRuleGroupUuid());
                    }
                    if (clientNrgList.contains(anrg.getKey().getNodeRuleGroupUuid())) {
                        nrgPresentInClient = true;
                        clientIrgNrgUuidList.add(anrg.getKey().getNodeRuleGroupUuid());
                    }
                }
                if (nrgPresentInNw && nrgPresentInClient
                        && irg.getValue().getAvailableCapacity().getTotalSize().getValue().doubleValue() > 0.0) {
                    List<Uuid> uuidToKeep = new ArrayList<>();
                    for (BasePceNep bpn : availableXpdrNWTps) {
                        for (Uuid nrgUuid : bpn.getNodeRuleGroupUuid()) {
                            if (nwIrgNrgUuidList.contains(nrgUuid)) {
                                uuidToKeep.add(bpn.getNepCepUuid());
                            }
                        }
                    }
                    availableXpdrNWTps = availableXpdrNWTps.stream()
                        .filter(bpn -> uuidToKeep.contains(bpn.getNepCepUuid()))
                        .collect(Collectors.toList());
                    usableXpdrNWTps.addAll(availableXpdrNWTps);
                    uuidToKeep.clear();
                    for (BasePceNep bpn : availableXpdrClientTps) {
                        for (Uuid nrgUuid : bpn.getNodeRuleGroupUuid()) {
                            if (clientIrgNrgUuidList.contains(nrgUuid)) {
                                uuidToKeep.add(bpn.getNepCepUuid());
                            }
                        }
                    }
                    availableXpdrClientTps = availableXpdrClientTps.stream()
                        .filter(bpn -> uuidToKeep.contains(bpn.getNepCepUuid()))
                        .collect(Collectors.toList());
                    usableXpdrClientTps.addAll(availableXpdrClientTps);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkSwPool(List<BasePceNep> netwTps, List<BasePceNep> clientTps) {

        if (SERVICE_TYPE_ODU_LIST.contains(this.otnServiceType)) {
            return true;
        }
        if (!SERVICE_TYPE_ETH_CLASS_MAP.containsKey(this.otnServiceType)) {
            return false;
        }
        if (netwTps == null) {
            return false;
        }
        switch (modeType) {

            case INTERMEDIATE_MODETYPE:
                return checkIntermediateSwPool(netwTps);

            case AZ_MODETYPE:
                if (clientTps == null) {
                    return false;
                }
                return checkAZSwPool(netwTps, clientTps);

            default:
                LOG.error("Unsupported mode type {}", modeType);
                return false;
        }
    }


    private boolean checkIntermediateSwPool(List<BasePceNep> netwTps) {
        for (BasePceNep bpn1 : netwTps) {
            for (BasePceNep bpn2 : netwTps) {
                if (bpn2.getNepCepUuid().equals(bpn1.getNepCepUuid())) {
                    continue;
                }
                for (Uuid nrgUuid : bpn2.getNodeRuleGroupUuid()) {
                    if (bpn1.getNodeRuleGroupUuid().contains(nrgUuid) && !node.getNodeRuleGroup().entrySet().stream()
                            .filter(nrg -> !nrg.getKey().getUuid().equals(nrgUuid)).findFirst().orElseThrow().getValue()
                            .getRule().entrySet().stream()
                                .filter(rule -> rule.getValue().getForwardingRule() != null
                                    && (rule.getValue().getForwardingRule()
                                        .equals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                                    || rule.getValue().getForwardingRule()
                                        .equals(FORWARDINGRULEMUSTFORWARDACROSSGROUP.VALUE)))
                            .collect(Collectors.toList())
                            .isEmpty()) {
                        usableXpdrNWTps = availableXpdrNWTps.stream()
                            .filter(bpn -> bpn.getNepCepUuid().equals(bpn1.getNepCepUuid())
                                || bpn.getNepCepUuid().equals(bpn2.getNepCepUuid()))
                            .collect(Collectors.toList());
                        return true;
                    }
                }
            }
        }

        // Being there means we did not find a common nrg with both nw ports.
        // Check if 2 Network tps have some nrgs that are interconnected through an IRG
        // with a Forwarding rule MAY/MUST meaning client are connected to Nw port
        for (BasePceNep bpn1 : netwTps) {
            List<Uuid> nw1NrgList = bpn1.getNodeRuleGroupUuid();
            for (BasePceNep bpn2 : netwTps) {
                if (bpn2.getNepCepUuid().equals(bpn1.getNepCepUuid())) {
                    continue;
                }
                List<Uuid> nw2NrgList = bpn2.getNodeRuleGroupUuid();
                for (Map.Entry<InterRuleGroupKey, InterRuleGroup> irg :node.getInterRuleGroup().entrySet()) {
                    boolean nrgPresentInNw1 = false;
                    boolean nrgPresentInNw2 = false;
                    if (!irg.getValue().getRule().entrySet().stream()
                            .filter(rule -> rule.getValue().getForwardingRule() != null
                                && (rule.getValue().getForwardingRule()
                                    .equals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                                || rule.getValue().getForwardingRule()
                                    .equals(FORWARDINGRULEMUSTFORWARDACROSSGROUP.VALUE)))
                            .collect(Collectors.toList()).isEmpty()) {
                        for (Map.Entry<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> anrg : irg.getValue()
                                .getAssociatedNodeRuleGroup().entrySet()) {
                            if (nw1NrgList.contains(anrg.getKey().getNodeRuleGroupUuid())) {
                                nrgPresentInNw1 = true;
                                //nw1IrgNrgUuidList.add(anrg.getKey().getNodeRuleGroupUuid());
                            }
                            if (nw2NrgList.contains(anrg.getKey().getNodeRuleGroupUuid())) {
                                nrgPresentInNw2 = true;
                                //nw2IrgNrgUuidList.add(anrg.getKey().getNodeRuleGroupUuid());
                            }
                        }
                        if (nrgPresentInNw1 && nrgPresentInNw2 && irg.getValue().getAvailableCapacity()
                                    .getTotalSize().getValue().doubleValue() > 0.0) {
                            usableXpdrNWTps = availableXpdrNWTps.stream()
                                .filter(bpn -> bpn.getNepCepUuid().equals(bpn1.getNepCepUuid())
                                    || bpn.getNepCepUuid().equals(bpn2.getNepCepUuid()))
                                .collect(Collectors.toList());
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public void validateIntermediateSwitch() {
        if (!isValid()) {
            return;
        }
        if (this.nodeType != OpenroadmNodeType.SWITCH) {
            return;
        }
        // Validate switch for use as an intermediate XPONDER on the path
        initXndrTps(INTERMEDIATE_MODETYPE);
        if (this.valid) {
            LOG.debug("validateIntermediateSwitch: Switch usable for transit == {}", nodeId);
        } else {
            LOG.debug("validateIntermediateSwitch: Switch unusable for transit == {}", nodeId);
        }
    }

    public boolean isValid() {
        if (nodeId == null
                || nodeType == null
                || this.getSupNetworkNodeId() == null
                || this.getSupClliNodeId() == null) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type, supporting nodeId");
            valid = false;
        }
        return valid;
    }

    @Override
    public void addOutgoingLink(PceLink outLink) {
        this.outgoingLinks.add(outLink);
    }

    @Override
    public List<PceLink> getOutgoingLinks() {
        return outgoingLinks;
    }

    @Override
    public AdminStates getAdminStates() {
        return null;
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public String getXpdrNWfromClient(String tp) {
        return this.clientPerNwTp.get(tp);
    }

    @Override
    public String toString() {
        return "PceNode type=" + nodeType + " ID=" + nodeId + " CLLI=" + this.getSupClliNodeId();
    }

    public void printLinksOfNode() {
        LOG.info(" outgoing links of node {} : {} ", nodeId, this.getOutgoingLinks());
    }

    @Override
    public Map<String, List<Uint16>> getAvailableTribPorts() {
        return tpAvailableTribPort;
    }

    @Override
    public Map<String, List<Uint16>> getAvailableTribSlots() {
        return tpAvailableTribSlot;
    }

    public List<BasePceNep> getUsableXpdrNWTps() {
        return availableXpdrNWTps;
    }

    public List<BasePceNep> getUsableXpdrClientTps() {
        return availableXpdrClientTps;
    }

    @Override
    public String getPceNodeType() {
//        return this.pceNodeType;
        return null;
    }

    @Override
    public String getSupNetworkNodeId() {
        return nodeId;
    }

    @Override
    public String getSupClliNodeId() {
        return nodeId;
    }

    @Override
    public String getRdmSrgClient(String tp, String direction) {
        return null;
    }

    @Override
    public NodeId getNodeId() {
        return new NodeId(nodeId);
    }

    @Override
    public boolean checkTP(String tp) {
        return false;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getVersion()
    */
    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BitSet getBitSetData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getXponderOperationalMode(XpdrNetworkAttributes tp) {
        return null;
    }

    @Override
    public String getOperationalMode() {
        return null;
    }

    @Override
    public OpenroadmNodeType getORNodeType() {
        return this.nodeType;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getSlotWidthGranularity()
    */
    @Override
    public BigDecimal getSlotWidthGranularity() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getCentralFreqGranularity()
     */
    @Override
    public BigDecimal getCentralFreqGranularity() {
        return null;
    }

    @Override
    public int getMinSlots() {
        return 0;
    }

    @Override
    public int getMaxSlots() {
        return 468;
    }

    @Override
    public AdministrativeState getAdminState() {
        return adminState;
    }

    @Override
    public OperationalState getOperationalState() {
        return operState;
    }

    @Override
    public  String getXpdrOperationalMode(Uuid nepUuid) {
        return null;
    }

    @Override
    public Uuid getNodeUuid() {
        return node.getUuid();
    }

    @Override
    public List<BasePceNep> getListOfNep() {
        return null;
    }
}
