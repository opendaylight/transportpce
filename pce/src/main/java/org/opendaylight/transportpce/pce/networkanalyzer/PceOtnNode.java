/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODTUCnTs;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.SwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOtsiOtsigroup;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceOtnNode implements PceNode {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceOtnNode.class);
    ////////////////////////// OTN NODES ///////////////////////////
    /*
     * For This Class the node passed shall be at the otn-openroadm Layer
     */

    private boolean valid = true;

    private final Node node;
    private final NodeId nodeId;
    private final OpenroadmNodeType nodeType;
    private final String pceNodeType;
    private final String otnServiceType;
    private String modeType;
    // TODO: not adding state check in this class as otn topology has not been modified
    private final AdminStates adminStates;
    private final State state;

    private Map<String, List<Uint16>> tpAvailableTribPort = new TreeMap<>();
    private Map<String, List<Uint16>> tpAvailableTribSlot = new TreeMap<>();
    private Map<String, OpenroadmTpType> availableXponderTp = new TreeMap<>();
    private List<String> usedXpdrNWTps = new ArrayList<>();
    private List<TpId> availableXpdrNWTps;
    private List<TpId> usableXpdrNWTps;
    private List<String> usedXpdrClientTps = new ArrayList<>();
    private List<TpId> availableXpdrClientTps;
    private List<TpId> usableXpdrClientTps;

    private List<PceLink> outgoingLinks = new ArrayList<>();
    private Map<String, String> clientPerNwTp = new HashMap<>();

    public PceOtnNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId, String pceNodeType, String serviceType) {
        this.node = node;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.pceNodeType = pceNodeType;
        this.otnServiceType = serviceType;
        this.tpAvailableTribSlot.clear();
        this.usedXpdrNWTps.clear();
        this.availableXpdrNWTps = new ArrayList<>();
        this.usableXpdrNWTps = new ArrayList<>();
        this.usedXpdrClientTps.clear();
        this.availableXpdrClientTps = new ArrayList<>();
        this.usableXpdrClientTps = new ArrayList<>();
        this.adminStates = node.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                .Node1.class).getAdministrativeState();
        this.state = node.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                .Node1.class).getOperationalState();
        this.tpAvailableTribPort.clear();
        checkAvailableTribPort();
        this.tpAvailableTribSlot.clear();
        checkAvailableTribSlot();
        if ((node == null) || (nodeId == null) || (nodeType != OpenroadmNodeType.MUXPDR)
            && (nodeType != OpenroadmNodeType.SWITCH) && (nodeType != OpenroadmNodeType.TPDR)) {
            LOG.error("PceOtnNode: one of parameters is not populated : nodeId, node type");
            this.valid = false;
        }
    }

    public void initXndrTps(String mode) {
        LOG.info("PceOtnNode: initXndrTps for node {}", this.nodeId.getValue());
        this.availableXponderTp.clear();

        this.modeType = mode;

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeTp
            = this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.topology.rev180226.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
                .node.TerminationPoint> allTps = new ArrayList<>(nodeTp.nonnullTerminationPoint().values());
        this.valid = false;
        if (allTps.isEmpty()) {
            LOG.error("PceOtnNode: initXndrTps: XPONDER TerminationPoint list is empty for node {}", this);
            return;
        }

        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
                .node.TerminationPoint tp : allTps) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.@Nullable TerminationPoint1 ocnTp1
                = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                .TerminationPoint1.class);
            //TODO many nested if-structures below, this needs to be reworked
            if (OpenroadmTpType.XPONDERNETWORK.equals(ocnTp1.getTpType())) {
                TerminationPoint1 ontTp1;
                if (tp.augmentation(TerminationPoint1.class) != null) {
                    ontTp1 = tp.augmentation(TerminationPoint1.class);
                } else {
                    continue;
                }
                switch (this.otnServiceType) {
                    case StringConstants.SERVICE_TYPE_100GE_S:
                        // TODO verify the capability of network port to support ODU4 CTP interface creation
                    case StringConstants.SERVICE_TYPE_ODU4:
                    case StringConstants.SERVICE_TYPE_ODUC4:
                        if (!checkTpForOdtuTermination(ontTp1)) {
                            LOG.error("TP {} of {} does not allow ODU4 termination creation",
                                tp.getTpId().getValue(), node.getNodeId().getValue());
                            continue;
                        }
                        break;

                    case StringConstants.SERVICE_TYPE_10GE:
                        if (!checkOdtuTTPforLoOduCreation(ontTp1, 10)) {
                            LOG.error("TP {} of {} does not allow OD2e termination creation",
                                tp.getTpId().getValue(), node.getNodeId().getValue());
                            continue;
                        }
                        break;
                    case StringConstants.SERVICE_TYPE_100GE_M:
                        if (!checkOdtuTTPforLoOduCreation(ontTp1, 20)) {
                            LOG.error("TP {} of {} does not allow ODU4 termination creation",
                                tp.getTpId().getValue(), node.getNodeId().getValue());
                            continue;
                        }
                        break;
                    case StringConstants.SERVICE_TYPE_1GE:
                        if (!checkOdtuTTPforLoOduCreation(ontTp1, 1)) {
                            LOG.error("TP {} of {} does not allow ODU0 termination creation",
                                tp.getTpId().getValue(), node.getNodeId().getValue());
                            continue;
                        }
                        break;

                    default:
                        LOG.error("TP {} of {} does not allow any termination creation",
                            tp.getTpId().getValue(), node.getNodeId().getValue());
                        continue;
                }
                LOG.info("TP {} of XPONDER {} is validated", tp.getTpId(), node.getNodeId().getValue());
                this.availableXpdrNWTps.add(tp.getTpId());
            } else if (OpenroadmTpType.XPONDERCLIENT.equals(ocnTp1.getTpType())
                && (StringConstants.SERVICE_TYPE_10GE.equals(this.otnServiceType)
                    || StringConstants.SERVICE_TYPE_100GE_M.equals(this.otnServiceType)
                    || StringConstants.SERVICE_TYPE_100GE_S.equals(this.otnServiceType)
                    || StringConstants.SERVICE_TYPE_1GE.equals(this.otnServiceType))) {
                TerminationPoint1 ontTp1;
                if (tp.augmentation(TerminationPoint1.class) != null) {
                    ontTp1 = tp.augmentation(TerminationPoint1.class);
                } else {
                    continue;
                }
                if (checkClientTp(ontTp1)) {
                    LOG.info("TP {} of XPONDER {} is validated", tp.getTpId(), node.getNodeId().getValue());
                    this.availableXpdrClientTps.add(tp.getTpId());
                } else {
                    LOG.error("TP {} of {} does not allow lo-ODU (ODU2e or ODU0) termination creation",
                        tp.getTpId().getValue(), node.getNodeId().getValue());
                }
            }
        }

        if (((StringConstants.SERVICE_TYPE_ODU4.equals(this.otnServiceType)
                || StringConstants.SERVICE_TYPE_ODUC4.equals(this.otnServiceType)) && mode.equals("AZ"))
            || ((StringConstants.SERVICE_TYPE_10GE.equals(this.otnServiceType)
                    || StringConstants.SERVICE_TYPE_100GE_M.equals(this.otnServiceType)
                    || StringConstants.SERVICE_TYPE_1GE.equals(this.otnServiceType))
                && ((mode.equals("AZ") && checkSwPool(availableXpdrClientTps, availableXpdrNWTps, 1, 1))
                     || (mode.equals("intermediate") && checkSwPool(null, availableXpdrNWTps, 0, 2)))
               )
            || (StringConstants.SERVICE_TYPE_100GE_S.equals(this.otnServiceType)
                && (mode.equals("AZ") && checkSwPool(availableXpdrClientTps, availableXpdrNWTps, 1, 1)))
            || (StringConstants.SERVICE_TYPE_100GE_S.equals(this.otnServiceType)
                && (mode.equals("intermediate") && checkSwPool(availableXpdrClientTps, availableXpdrNWTps, 0, 2)))
            ) {
            this.valid = true;
        } else {
            this.valid = false;
        }
    }

    private boolean checkSwPool(List<TpId> clientTps, List<TpId> netwTps, int nbClient, int nbNetw) {
        if (clientTps != null && netwTps != null && nbClient == 1 && nbNetw == 1) {
            clientTps.sort(Comparator.comparing(TpId::getValue));
            netwTps.sort(Comparator.comparing(TpId::getValue));
            for (TpId nwTp : netwTps) {
                for (TpId clTp : clientTps) {
                    @Nullable
                    List<NonBlockingList> nblList = new ArrayList<>(node.augmentation(Node1.class).getSwitchingPools()
                        .nonnullOduSwitchingPools().values().stream().findFirst().get().getNonBlockingList().values());
                    for (NonBlockingList nbl : nblList) {
                        if (nbl.getTpList().contains(clTp) && nbl.getTpList().contains(nwTp)) {
                            usableXpdrClientTps.add(clTp);
                            usableXpdrNWTps.add(nwTp);
                        }
                        if (usableXpdrClientTps.size() >= nbClient && usableXpdrNWTps.size() >= nbNetw) {
                            clientPerNwTp.put(nwTp.getValue(), clTp.getValue());
                            return true;
                        }
                    }
                }
            }

        }
        if (netwTps != null && nbClient == 0 && nbNetw == 2) {
            netwTps.sort(Comparator.comparing(TpId::getValue));
            @Nullable
            List<NonBlockingList> nblList = new ArrayList<>(node.augmentation(Node1.class).getSwitchingPools()
                .nonnullOduSwitchingPools().values().stream().findFirst().get().getNonBlockingList().values());
            for (NonBlockingList nbl : nblList) {
                for (TpId nwTp : netwTps) {
                    if (nbl.getTpList().contains(nwTp)) {
                        usableXpdrNWTps.add(nwTp);
                    }
                    if (usableXpdrNWTps.size() >= nbNetw) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkTpForOdtuTermination(TerminationPoint1 ontTp1) {
        for (SupportedInterfaceCapability sic : ontTp1.getTpSupportedInterfaces().getSupportedInterfaceCapability()
                .values()) {
            LOG.info("in checkTpForOduTermination - sic = {}", sic.getIfCapType());
            if ((sic.getIfCapType().equals(IfOCHOTU4ODU4.class) || sic.getIfCapType().equals(IfOtsiOtsigroup.class))
                && (ontTp1.getXpdrTpPortConnectionAttributes() == null
                    || ontTp1.getXpdrTpPortConnectionAttributes().getTsPool() == null)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOdtuTTPforLoOduCreation(TerminationPoint1 ontTp1, int tsNb) {
        if (ontTp1.getXpdrTpPortConnectionAttributes() != null
            && ontTp1.getXpdrTpPortConnectionAttributes().getTsPool() != null
            && ontTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool() != null
            && (ontTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values()
                .stream().findFirst().get().getOdtuType()
                .equals(ODTU4TsAllocated.class)
                ||
                ontTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values()
                .stream().findFirst().get().getOdtuType()
                .equals(ODTUCnTs.class))
            && !ontTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values()
                .stream().findFirst().get().getTpnPool().isEmpty()
            && (ontTp1.getXpdrTpPortConnectionAttributes().getTsPool().size() >= tsNb)) {
            return true;
        }
        return false;
    }

    private boolean checkClientTp(TerminationPoint1 ontTp1) {
        for (SupportedInterfaceCapability sic : ontTp1.getTpSupportedInterfaces().getSupportedInterfaceCapability()
                .values()) {
            LOG.debug("in checkTpForOduTermination - sic = {}", sic.getIfCapType());
            switch (otnServiceType) {
                case StringConstants.SERVICE_TYPE_1GE:
                // we could also check the administrative status of the tp
                    if (sic.getIfCapType().equals(If1GEODU0.class)) {
                        return true;
                    }
                    break;
                case StringConstants.SERVICE_TYPE_10GE:
                    if (sic.getIfCapType().equals(If10GEODU2e.class)) {
                        return true;
                    }
                    break;
                case StringConstants.SERVICE_TYPE_100GE_T:
                case StringConstants.SERVICE_TYPE_100GE_M:
                case StringConstants.SERVICE_TYPE_100GE_S:
                    if (sic.getIfCapType().equals(If100GEODU4.class)) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    public void validateXponder(String anodeId, String znodeId) {
        if (!isValid()) {
            return;
        }
        if (this.nodeId.getValue().equals(anodeId) || (this.nodeId.getValue().equals(znodeId))) {
            initXndrTps("AZ");
        } else if (OpenroadmNodeType.SWITCH.equals(this.nodeType)) {
            initXndrTps("intermediate");
        } else {
            LOG.info("validateAZxponder: XPONDER is ignored == {}", nodeId.getValue());
            valid = false;
        }
    }

    public boolean validateSwitchingPoolBandwidth(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.networks.network.node.TerminationPoint tp1,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.networks.network.node.TerminationPoint tp2,
            Long neededBW) {
        if (this.nodeType != OpenroadmNodeType.TPDR) {
            return true;
        }
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Node1 node1 =
            node.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Node1.class);
        SwitchingPools sp = node1.getSwitchingPools();
        List<OduSwitchingPools> osp = new ArrayList<>(sp.nonnullOduSwitchingPools().values());
        for (OduSwitchingPools ospx : osp) {
            List<NonBlockingList> nbl = new ArrayList<>(ospx.nonnullNonBlockingList().values());
            for (NonBlockingList nbll : nbl) {
                if (nbll.getAvailableInterconnectBandwidth().toJava() >= neededBW && nbll.getTpList() != null
                        && nbll.getTpList().contains(tp1.getTpId()) && nbll.getTpList().contains(tp2.getTpId())) {
                    LOG.debug("validateSwitchingPoolBandwidth: couple  of tp {} x {} valid for crossconnection",
                        tp1.getTpId(), tp2.getTpId());
                    return true;
                }
            }
        }

        LOG.debug("validateSwitchingPoolBandwidth: No valid Switching pool for crossconnecting tp {} and {}",
            tp1.getTpId(), tp2.getTpId());
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
        initXndrTps("intermediate");
        if (!this.valid) {
            LOG.debug("validateIntermediateSwitch: Switch unusable for transit == {}", nodeId.getValue());
        } else {
            LOG.info("validateIntermediateSwitch: Switch usable for transit == {}", nodeId.getValue());
        }
    }

    public void checkAvailableTribPort() {
        List<TerminationPoint> networkTpList = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().values().stream()
            .filter(type -> type
                .augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1.class)
                .getTpType().equals(OpenroadmTpType.XPONDERNETWORK))
            .collect(Collectors.toList());

        for (TerminationPoint tp : networkTpList) {
            if (tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes() != null
                && tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getOdtuTpnPool() != null
                && (tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getOdtuTpnPool()
                    .values().stream().findFirst().get().getOdtuType().equals(ODTU4TsAllocated.class)
                    || tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getOdtuTpnPool()
                    .values().stream().findFirst().get().getOdtuType().equals(ODTUCnTs.class))) {
                @Nullable
                List<Uint16> tpnPool = tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes()
                    .getOdtuTpnPool().values().stream().findFirst().get().getTpnPool();
                if (tpnPool != null) {
                    tpAvailableTribPort.put(tp.getTpId().getValue(), tpnPool);
                }
            }
        }
    }

    public void checkAvailableTribSlot() {
        List<TerminationPoint> networkTpList = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().values().stream()
            .filter(type -> type
                .augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1.class)
                .getTpType().equals(OpenroadmTpType.XPONDERNETWORK))
            .collect(Collectors.toList());

        for (TerminationPoint tp : networkTpList) {
            if (tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes() != null
                && tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getTsPool() != null) {
                @Nullable
                List<Uint16> tsPool = tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes()
                    .getTsPool();
                tpAvailableTribSlot.put(tp.getTpId().getValue(), tsPool);
            }
        }
    }

    public boolean isValid() {
        if ((node == null) || (nodeId == null) || (nodeType == null) || (this.getSupNetworkNodeId() == null)
            || (this.getSupClliNodeId() == null)) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type, supporting nodeId");
            valid = false;
        }
        return valid;
    }

    public boolean isPceOtnNodeValid(final PceOtnNode pceOtnNode) {
        if (pceOtnNode == null || pceOtnNode.node == null
            || pceOtnNode.getNodeId() == null || pceOtnNode.nodeType == null || pceOtnNode.getSupNetworkNodeId() == null
            || pceOtnNode.getSupClliNodeId() == null || pceOtnNode.otnServiceType == null) {
            LOG.error(
                "PceOtnNode: one of parameters is not populated : nodeId, node type, supporting nodeId, otnServiceType"
            );
            return false;
        }

        if (!isNodeTypeValid(pceOtnNode)) {
            LOG.error("PceOtnNode node type: node type isn't one of MUXPDR or SWITCH or TPDR");
            return false;
        }

        return isOtnServiceTypeValid(pceOtnNode);
    }

    private boolean isOtnServiceTypeValid(PceOtnNode pceOtnNode) {
        if (pceOtnNode.modeType == null) {
            return false;
        }

        //Todo refactor Strings (mode and otnServiceType ) to enums
        if ((pceOtnNode.otnServiceType.equals(StringConstants.SERVICE_TYPE_ODU4)
                && pceOtnNode.modeType.equals("AZ"))) {
            return true;
        }

        if ((pceOtnNode.otnServiceType.equals(StringConstants.SERVICE_TYPE_10GE)
                || pceOtnNode.otnServiceType.equals(StringConstants.SERVICE_TYPE_1GE)
                || pceOtnNode.otnServiceType.equals(StringConstants.SERVICE_TYPE_100GE_S))
                && (isAz(pceOtnNode) || isIntermediate(pceOtnNode))) {
            return true;
        }

        return false;
    }

    private boolean isIntermediate(PceOtnNode pceOtnNode) {
        return pceOtnNode.modeType.equals("intermediate")
                && checkSwPool(null, pceOtnNode.availableXpdrNWTps, 0, 2);
    }

    private boolean isAz(PceOtnNode pceOtnNode) {
        return pceOtnNode.modeType.equals("AZ")
                && checkSwPool(pceOtnNode.availableXpdrClientTps, pceOtnNode.availableXpdrNWTps, 1, 1);
    }

    private boolean isNodeTypeValid(final PceOtnNode pceOtnNode) {
        return (pceOtnNode.nodeType == OpenroadmNodeType.MUXPDR)
                || (pceOtnNode.nodeType  == OpenroadmNodeType.SWITCH)
                || (pceOtnNode.nodeType  == OpenroadmNodeType.TPDR);
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
        return adminStates;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public String getXpdrClient(String tp) {
        return this.clientPerNwTp.get(tp);
    }

    @Override
    public String toString() {
        return "PceNode type=" + nodeType + " ID=" + nodeId.getValue() + " CLLI=" + this.getSupClliNodeId();
    }

    public void printLinksOfNode() {
        LOG.info(" outgoing links of node {} : {} ", nodeId.getValue(), this.getOutgoingLinks());
    }

    @Override
    public Map<String, List<Uint16>> getAvailableTribPorts() {
        return tpAvailableTribPort;
    }

    @Override
    public Map<String, List<Uint16>> getAvailableTribSlots() {
        return tpAvailableTribSlot;
    }

    public List<TpId> getUsableXpdrNWTps() {
        return usableXpdrNWTps;
    }

    public List<TpId> getUsableXpdrClientTps() {
        return usableXpdrClientTps;
    }

    @Override
    public String getPceNodeType() {
        return this.pceNodeType;
    }

    @Override
    public String getSupNetworkNodeId() {
        return MapUtils.getSupNetworkNode(this.node);
    }

    @Override
    public String getSupClliNodeId() {
        return MapUtils.getSupClliNode(this.node);
    }

    @Override
    public String getRdmSrgClient(String tp) {
        return null;
    }

    @Override
    public NodeId getNodeId() {
        return nodeId;
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
}
