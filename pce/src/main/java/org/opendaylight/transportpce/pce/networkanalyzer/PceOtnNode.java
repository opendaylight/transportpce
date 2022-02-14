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
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.XpdrTpPortConnectionAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOtsiOtsigroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev200529.xpdr.otn.tp.attributes.OdtuTpnPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceOtnNode implements PceNode {
    ////////////////////////// OTN NODES ///////////////////////////
    /*
     * For This Class the node passed shall be at the otn-openroadm Layer
     */

    private static final Logger LOG = LoggerFactory.getLogger(PceOtnNode.class);
    private static final List<String> SERVICE_TYPE_ODU_LIST = List.of(
        StringConstants.SERVICE_TYPE_ODU4,
        StringConstants.SERVICE_TYPE_ODUC4,
        StringConstants.SERVICE_TYPE_ODUC3,
        StringConstants.SERVICE_TYPE_ODUC2);
    private static final List<OpenroadmNodeType> VALID_NODETYPES_LIST = List.of(
        OpenroadmNodeType.MUXPDR,
        OpenroadmNodeType.SWITCH,
        OpenroadmNodeType.TPDR);
    private static final Map<String, Class<? extends SupportedIfCapability>> SERVICE_TYPE_ETH_CLASS_MAP = Map.of(
        StringConstants.SERVICE_TYPE_1GE, If1GEODU0.class,
        StringConstants.SERVICE_TYPE_10GE, If10GEODU2e.class,
        StringConstants.SERVICE_TYPE_100GE_M, If100GEODU4.class,
        StringConstants.SERVICE_TYPE_100GE_S, If100GEODU4.class);
    private static final Map<String, Integer> SERVICE_TYPE_ETH_TS_NB_MAP = Map.of(
        StringConstants.SERVICE_TYPE_1GE, 1,
        StringConstants.SERVICE_TYPE_10GE, 10,
        StringConstants.SERVICE_TYPE_100GE_M, 20);
    private static final Map<String, String> SERVICE_TYPE_ETH_ODU_STRING_MAP = Map.of(
        StringConstants.SERVICE_TYPE_1GE, "ODU0",
        StringConstants.SERVICE_TYPE_10GE, "ODU2e",
        StringConstants.SERVICE_TYPE_100GE_M, "ODU4");

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
    private String clientPort;

    public PceOtnNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId, String pceNodeType, String serviceType,
            String clientPort) {
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
        this.adminStates = node
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class)
            .getAdministrativeState();
        this.state = node
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class)
            .getOperationalState();
        this.tpAvailableTribPort.clear();
        checkAvailableTribPort();
        this.tpAvailableTribSlot.clear();
        checkAvailableTribSlot();
        this.clientPort = clientPort;
        if (node == null || nodeId == null || nodeType == null || !VALID_NODETYPES_LIST.contains(nodeType)) {
            LOG.error("PceOtnNode: one of parameters is not populated : nodeId, node type");
            this.valid = false;
        }
    }

    public void initXndrTps(String mode) {
        LOG.info("PceOtnNode: initXndrTps for node {}", this.nodeId.getValue());
        this.availableXponderTp.clear();
        this.modeType = mode;
        List<TerminationPoint> allTps =
            new ArrayList<>(
                this.node.augmentation(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1.class)
                .nonnullTerminationPoint()
                .values());
        this.valid = false;
        if (allTps.isEmpty()) {
            LOG.error("PceOtnNode: initXndrTps: XPONDER TerminationPoint list is empty for node {}", this);
            return;
        }
        for (TerminationPoint tp : allTps) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1 ocnTp1
                = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                        .TerminationPoint1.class);
            if (ocnTp1 == null) {
                LOG.warn("null ocn TP {}", tp);
                continue;
            }
            //TODO many nested structures below, this needs to be reworked
            switch (ocnTp1.getTpType()) {
                case XPONDERNETWORK:
                    if (tp.augmentation(TerminationPoint1.class) == null) {
                        continue;
                    }
                    TerminationPoint1 ontTp1 = tp.augmentation(TerminationPoint1.class);
                    if (SERVICE_TYPE_ODU_LIST.contains(this.otnServiceType)
                            || StringConstants.SERVICE_TYPE_100GE_S.equals(this.otnServiceType)) {
                            // TODO verify the capability of network port to support ODU4 CTP interface creation
                        if (!checkTpForOdtuTermination(ontTp1)) {
                            LOG.error("TP {} of {} does not allow ODU4 termination creation",
                                tp.getTpId().getValue(), node.getNodeId().getValue());
                            continue;
                        }
                    } else if (SERVICE_TYPE_ETH_TS_NB_MAP.containsKey(this.otnServiceType)) {
                        if (!checkOdtuTTPforLoOduCreation(
                                ontTp1, SERVICE_TYPE_ETH_TS_NB_MAP.get(this.otnServiceType))) {
                            LOG.error("TP {} of {} does not allow {} termination creation",
                                tp.getTpId().getValue(),
                                SERVICE_TYPE_ETH_ODU_STRING_MAP.get(this.otnServiceType),
                                node.getNodeId().getValue());
                            continue;
                        }
                    } else {
                        LOG.error("TP {} of {} does not allow any termination creation",
                            tp.getTpId().getValue(), node.getNodeId().getValue());
                        continue;
                    }
                    LOG.info("TP {} of XPONDER {} is validated", tp.getTpId(), node.getNodeId().getValue());
                    this.availableXpdrNWTps.add(tp.getTpId());
                    break;

                case XPONDERCLIENT:
                    if (SERVICE_TYPE_ETH_CLASS_MAP.containsKey(otnServiceType)) {
                        if (tp.augmentation(TerminationPoint1.class) == null) {
                            continue;
                        }
                        if (checkClientTp(tp.augmentation(TerminationPoint1.class))) {
                            LOG.info("TP {} of XPONDER {} is validated", tp.getTpId(), node.getNodeId().getValue());
                            this.availableXpdrClientTps.add(tp.getTpId());
                        } else {
                            LOG.error("TP {} of {} does not allow lo-ODU (ODU2e or ODU0) termination creation",
                                tp.getTpId().getValue(), node.getNodeId().getValue());
                        }
                    }
                    break;

                default:
                    LOG.debug("unsupported ocn TP type {}", ocnTp1.getTpType());
            }
        }
        this.valid = SERVICE_TYPE_ODU_LIST.contains(this.otnServiceType)
                || SERVICE_TYPE_ETH_CLASS_MAP.containsKey(this.otnServiceType)
                    && isAzOrIntermediateAvl(availableXpdrClientTps, availableXpdrNWTps,
                        StringConstants.SERVICE_TYPE_100GE_S.equals(this.otnServiceType)
                            ? availableXpdrClientTps
                            : null);
                //TODO check whether it makes sense to pass twice availableXpdrClientTps tp isAzOrIntermediateAvl
                //     and to differentiate SERVICE_TYPE_100GE_S case
                //     better pass otnServiceType -> this should probably be refactored
    }

    private boolean isAzOrIntermediateAvl(List<TpId> clientTps, List<TpId> netwTps, List<TpId> clientTps0) {
        switch (modeType) {
            case "intermediate":
                return checkSwPool(clientTps0, netwTps, 0, 2);

            case "AZ":
                return checkSwPool(clientTps, netwTps, 1, 1);

            default:
                LOG.error("unknown mode type {}", modeType);
                return false;
        }
    }

    private boolean checkSwPool(List<TpId> clientTps, List<TpId> netwTps, int nbClient, int nbNetw) {
        if (netwTps == null) {
            return false;
        }
        if (clientTps != null && nbClient == 1 && nbNetw == 1) {
            clientTps.sort(Comparator.comparing(TpId::getValue));
            netwTps.sort(Comparator.comparing(TpId::getValue));
            for (TpId nwTp : netwTps) {
                for (TpId clTp : clientTps) {
                    for (NonBlockingList nbl : new ArrayList<>(node.augmentation(Node1.class).getSwitchingPools()
                            .nonnullOduSwitchingPools().values().stream().findFirst().get()
                                .getNonBlockingList().values())) {
                        if (nbl.getTpList().contains(clTp) && nbl.getTpList().contains(nwTp)) {
                            usableXpdrClientTps.add(clTp);
                            usableXpdrNWTps.add(nwTp);
                        }
                        if (usableXpdrClientTps.size() >= 1 && usableXpdrNWTps.size() >= 1
                            //since nbClient == 1 && nbNetw == 1...
                                && (this.clientPort == null || this.clientPort.equals(clTp.getValue()))) {
                            clientPerNwTp.put(nwTp.getValue(), clTp.getValue());
                            return true;
                        }
                    }
                }
            }
        }
        if (nbClient == 0 && nbNetw == 2) {
            netwTps.sort(Comparator.comparing(TpId::getValue));
            //TODO compared to above, nested loops are inverted below - does it make really sense ?
            //     there is room to rationalize things here
            for (NonBlockingList nbl : new ArrayList<>(node.augmentation(Node1.class).getSwitchingPools()
                    .nonnullOduSwitchingPools().values().stream().findFirst().get()
                        .getNonBlockingList().values())) {
                for (TpId nwTp : netwTps) {
                    if (nbl.getTpList().contains(nwTp)) {
                        usableXpdrNWTps.add(nwTp);
                    }
                    if (usableXpdrNWTps.size() >= 2) {
                    //since nbClient == 0 && nbNetw == 2...
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
        XpdrTpPortConnectionAttributes portConAttr = ontTp1.getXpdrTpPortConnectionAttributes();
        if (portConAttr == null
                || portConAttr.getTsPool() == null
                || portConAttr.getTsPool().size() < tsNb
                || portConAttr.getOdtuTpnPool() == null) {
            return false;
        }
        return checkFirstOdtuTpn(portConAttr.getOdtuTpnPool().values().stream().findFirst().get());
    }

    private boolean checkFirstOdtuTpn(OdtuTpnPool otPool) {
        return (otPool.getOdtuType().equals(ODTU4TsAllocated.class)
                || otPool.getOdtuType().equals(ODTUCnTs.class))
            && !otPool.getTpnPool().isEmpty();
    }

    private boolean checkClientTp(TerminationPoint1 ontTp1) {
        for (SupportedInterfaceCapability sic : ontTp1.getTpSupportedInterfaces().getSupportedInterfaceCapability()
                .values()) {
            LOG.debug("in checkTpForOduTermination - sic = {}", sic.getIfCapType());
            // we could also check the administrative status of the tp
            if (SERVICE_TYPE_ETH_CLASS_MAP.containsKey(otnServiceType)
                    && sic.getIfCapType().equals(SERVICE_TYPE_ETH_CLASS_MAP.get(otnServiceType))) {
                return true;
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

    public boolean validateSwitchingPoolBandwidth(TerminationPoint tp1, TerminationPoint tp2, Long neededBW) {
        if (this.nodeType != OpenroadmNodeType.TPDR) {
            return true;
        }
        Node1 node1 = node.augmentation(Node1.class);
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
        if (this.valid) {
            LOG.info("validateIntermediateSwitch: Switch usable for transit == {}", nodeId.getValue());
        } else {
            LOG.debug("validateIntermediateSwitch: Switch unusable for transit == {}", nodeId.getValue());
        }
    }

    public void checkAvailableTribPort() {
        for (TerminationPoint tp :
            node.augmentation(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1.class)
                .getTerminationPoint().values().stream()
                .filter(type -> type
                    .augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                            .TerminationPoint1.class)
                    .getTpType()
                    .equals(OpenroadmTpType.XPONDERNETWORK))
                .collect(Collectors.toList())) {
            XpdrTpPortConnectionAttributes portConAttr =
                tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes();
            if (portConAttr != null && portConAttr.getOdtuTpnPool() != null) {
                OdtuTpnPool otPool = portConAttr.getOdtuTpnPool().values().stream().findFirst().get();
                if (checkFirstOdtuTpn(otPool)) {
                    tpAvailableTribPort.put(tp.getTpId().getValue(), otPool.getTpnPool());
                }
            }
        }
    }

    public void checkAvailableTribSlot() {
        for (TerminationPoint tp :
            node.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().values().stream()
            .filter(type -> type
                .augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1.class)
                .getTpType().equals(OpenroadmTpType.XPONDERNETWORK))
            .collect(Collectors.toList())
        ) {
            XpdrTpPortConnectionAttributes portConAttr =
                tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes();
            if (portConAttr != null && portConAttr.getTsPool() != null) {
                tpAvailableTribSlot.put(tp.getTpId().getValue(), portConAttr.getTsPool());
            }
        }
    }

    public boolean isValid() {
        if (nodeId == null || nodeType == null
                || this.getSupNetworkNodeId() == null || this.getSupClliNodeId() == null) {
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
    public String getRdmSrgClient(String tp, String direction) {
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
