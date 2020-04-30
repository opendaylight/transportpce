/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.SwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.IfOCHOTU4ODU4;
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

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeTp
            = this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.topology.rev180226.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint> allTps = nodeTp.getTerminationPoint();
        this.valid = false;
        if (allTps == null) {
            LOG.error("PceOtnNode: initXndrTps: XPONDER TerminationPoint list is empty for node {}", this);
            return;
        }

        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint tp : allTps) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.@Nullable TerminationPoint1 ocnTp1
                = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1.class);
            //TODO many nested if-structures below, this needs to be reworked
            if (OpenroadmTpType.XPONDERNETWORK.equals(ocnTp1.getTpType()) && this.otnServiceType.equals("ODU4")) {
                TerminationPoint1 ontTp1;
                if (tp.augmentation(TerminationPoint1.class) != null) {
                    ontTp1 = tp.augmentation(TerminationPoint1.class);
                } else {
                    continue;
                }
                if (checkTpForOdtuTermination(ontTp1)) {
                    LOG.info("TP {} of XPONDER {} is validated", tp.getTpId(), node.getNodeId().getValue());
                    this.availableXpdrNWTps.add(tp.getTpId());
                } else {
                    LOG.error("TP {} of {} does not allow ODU4 termination creation", tp.getTpId().getValue(),
                        node.getNodeId().getValue());
                }
            } else if (OpenroadmTpType.XPONDERNETWORK.equals(ocnTp1.getTpType())
                && (this.otnServiceType.equals("10GE") || this.otnServiceType.equals("1GE"))) {
                TerminationPoint1 ontTp1;
                if (tp.augmentation(TerminationPoint1.class) != null) {
                    ontTp1 = tp.augmentation(TerminationPoint1.class);
                } else {
                    continue;
                }
                if ("10GE".equals(otnServiceType) && checkOdtuTTPforLoOduCreation(ontTp1, 10)
                    || "1GE".equals(otnServiceType) && checkOdtuTTPforLoOduCreation(ontTp1, 1)) {
                    LOG.info("TP {} of XPONDER {} is validated", tp.getTpId(), node.getNodeId().getValue());
                    this.availableXpdrNWTps.add(tp.getTpId());
                } else {
                    if ("10GE".equals(otnServiceType)) {
                        LOG.error("TP {} of {} does not allow OD2e termination creation", tp.getTpId().getValue(),
                            node.getNodeId().getValue());
                    } else if ("1GE".equals(otnServiceType)) {
                        LOG.error("TP {} of {} does not allow ODU0 termination creation", tp.getTpId().getValue(),
                            node.getNodeId().getValue());
                    } else {
                        LOG.error("TP {} of {} does not allow any termination creation", tp.getTpId().getValue(),
                            node.getNodeId().getValue());
                    }
                }
            } else if (OpenroadmTpType.XPONDERCLIENT.equals(ocnTp1.getTpType())
                && (this.otnServiceType.equals("10GE") || this.otnServiceType.equals("1GE"))) {
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

        if ((this.otnServiceType.equals("ODU4") && mode.equals("AZ"))
            || ((this.otnServiceType.equals("10GE") || this.otnServiceType.equals("1GE"))
                && mode.equals("AZ") && checkSwPool(availableXpdrClientTps, availableXpdrNWTps, 1, 1))
            || ((this.otnServiceType.equals("10GE") || this.otnServiceType.equals("1GE"))
                && mode.equals("intermediate") && checkSwPool(null, availableXpdrNWTps, 0, 2))) {
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
                    List<NonBlockingList> nblList = node.augmentation(Node1.class).getSwitchingPools()
                        .getOduSwitchingPools().get(0).getNonBlockingList();
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
        if (clientTps == null && netwTps != null && nbClient == 0 && nbNetw == 2) {
            netwTps.sort(Comparator.comparing(TpId::getValue));
            @Nullable
            List<NonBlockingList> nblList = node.augmentation(Node1.class).getSwitchingPools().getOduSwitchingPools()
                .get(0).getNonBlockingList();
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
        for (SupportedInterfaceCapability sic : ontTp1.getTpSupportedInterfaces().getSupportedInterfaceCapability()) {
            LOG.debug("in checkTpForOduTermination - sic = {}", sic.getIfCapType());
            if (sic.getIfCapType().equals(IfOCHOTU4ODU4.class)
                && ontTp1.getXpdrTpPortConnectionAttributes().getTsPool() == null) {
                return true;
            }
        }
        return false;
    }

    private boolean checkOdtuTTPforLoOduCreation(TerminationPoint1 ontTp1, int tsNb) {
        if (ontTp1.getXpdrTpPortConnectionAttributes() != null
            && ontTp1.getXpdrTpPortConnectionAttributes().getTsPool() != null
            && ontTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool() != null
            && ontTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getOdtuType()
                .equals(ODTU4TsAllocated.class)
            && ontTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool().isEmpty()
            && (ontTp1.getXpdrTpPortConnectionAttributes().getTsPool().size() >= tsNb)) {
            return true;
        }
        return false;
    }

    private boolean checkClientTp(TerminationPoint1 ontTp1) {
        for (SupportedInterfaceCapability sic : ontTp1.getTpSupportedInterfaces().getSupportedInterfaceCapability()) {
            LOG.debug("in checkTpForOduTermination - sic = {}", sic.getIfCapType());
            switch (otnServiceType) {
                case "1GE":
                // we could also check the administrative status of the tp
                    if (sic.getIfCapType().equals(If1GEODU0.class)) {
                        return true;
                    }
                    break;
                case "10GE":
                    if (sic.getIfCapType().equals(If10GEODU2e.class)) {
                        return true;
                    }
                    break;
                case "100GE":
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
        if (OpenroadmNodeType.SWITCH.equals(this.nodeType)) {
            initXndrTps("intermediate");
        }
        if (this.nodeId.getValue().equals(anodeId) || (this.nodeId.getValue().equals(znodeId))) {
            initXndrTps("AZ");
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
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1 node1 =
            node.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1.class);
        SwitchingPools sp = node1.getSwitchingPools();
        List<OduSwitchingPools> osp = sp.getOduSwitchingPools();
        for (OduSwitchingPools ospx : osp) {
            List<NonBlockingList> nbl = ospx.getNonBlockingList();
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
            .getTerminationPoint().stream()
            .filter(type -> type
                .augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class)
                .getTpType().equals(OpenroadmTpType.XPONDERNETWORK))
            .collect(Collectors.toList());

        for (TerminationPoint tp : networkTpList) {
            if (tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getOdtuTpnPool() != null
                && tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0)
                    .getOdtuType().equals(ODTU4TsAllocated.class)) {
                @Nullable
                List<Uint16> tpnPool = tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes()
                    .getOdtuTpnPool().get(0).getTpnPool();
                if (tpnPool != null) {
                    tpAvailableTribPort.put(tp.getTpId().getValue(), tpnPool);
                }
            }
        }
    }

    public void checkAvailableTribSlot() {
        List<TerminationPoint> networkTpList = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().stream()
            .filter(type -> type
                .augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class)
                .getTpType().equals(OpenroadmTpType.XPONDERNETWORK))
            .collect(Collectors.toList());

        for (TerminationPoint tp : networkTpList) {
            if (tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getTsPool() != null) {
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

    @Override
    public void addOutgoingLink(PceLink outLink) {
        this.outgoingLinks.add(outLink);
    }

    @Override
    public List<PceLink> getOutgoingLinks() {
        return outgoingLinks;
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

    @Override
    public boolean checkWL(long index) {
        return false;
    }
}
