/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.Optional;
import java.util.TreeMap;

//import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.common.NetworkUtils;
//import org.opendaylight.transportpce.pce.SortPortsByName;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks
//.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODTU4TsAllocated;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.SwitchingPools;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If100GEODU4;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If10GEODU2e;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev181130.xpdr.otn.tp.attributes.OdtuTpnPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceOtnNode extends PceNode {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);
    ////////////////////////// OTN NODES ///////////////////////////
    /*
     * For This Class the node passed shall be at the otn-openroadm Layer
     */

    private boolean valid = true;

    private final String supNetworkNodeId;
    private final String supTopoNodeId;
    private final String clli;
    private final String otnServiceType;

    private Map<String, List<Integer>> tpAvailableTribPort = new TreeMap<String, List<Integer>>();
    private Map<String, List<Integer>> tpAvailableTribSlot = new TreeMap<String, List<Integer>>();
    private Map<String, OpenroadmTpType> availableXponderTp = new TreeMap<String, OpenroadmTpType>();
    private List<String> usedXpdrNWTps = new ArrayList<String>();
    private List<String> unusableXpdrNWTps = new ArrayList<String>();
    private List<String> usedXpdrClientTps = new ArrayList<String>();
    private List<String> unusableXpdrClientTps = new ArrayList<String>();
    private List<PceLink> outgoingLinks = new ArrayList<PceLink>();
    private Map<String, String> clientPerNwTp = new HashMap<String, String>();

    public PceOtnNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId, String serviceType) {
        super(node, nodeType, nodeId);
        this.supNetworkNodeId = getNetworkSupNodeId(node);
        this.supTopoNodeId = getTopoSupNodeId(node);
        this.clli = getClliSupNodeId(node);
        this.otnServiceType = serviceType;
        this.tpAvailableTribPort.clear();
        this.tpAvailableTribSlot.clear();
        this.usedXpdrNWTps.clear();
        this.unusableXpdrNWTps.clear();
        this.usedXpdrClientTps.clear();
        this.unusableXpdrClientTps.clear();

        if ((node == null) || (nodeId == null) || (nodeType == null)) {
            LOG.error("PceOtnNode: one of parameters is not populated : nodeId, node type");
            this.valid = false;
        }
    }

    public void initXndrTps(String mode) {
        LOG.info("initXndrTps for node : {}", this.nodeId);
        int availableNetworkTpNumber = 0;
        int availableClientTpNumber = 0;

        this.availableXponderTp.clear();

        if (!isValid()) {
            return;
        }
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeTp =
            this.node.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
            .network.node.TerminationPoint> allTps = nodeTp.getTerminationPoint();
        this.valid = false;
        if (allTps == null) {
            LOG.error("initXndrTps: XPONDER TerminationPoint list is empty for node {}", this.toString());
            return;
        }

        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                .network.node.TerminationPoint tp : allTps) {
            TerminationPoint1 otnTp1 = tp.augmentation(TerminationPoint1.class);
            //TODO many nested if-structures below, this needs to be reworked
            if (otnTp1.getTpType() == OpenroadmTpType.XPONDERNETWORK) {
                if (otnTp1.getXpdrTpPortConnectionAttributes().getWavelength() != null) {
                    this.usedXpdrNWTps.add(tp.getTpId().getValue());
                } else {
                    // find server of this network TP
                    String server = otnTp1.getXpdrTpPortConnectionAttributes().getTailEquipmentId();
                    if ((server.equals("")) || (server == null)) {
                        this.unusableXpdrNWTps.add(tp.getTpId().getValue());
                    } else {
                        // tp is not used and as a tail to server WDM layer
                        if (("10GE".equals(this.otnServiceType)) || ("1GE".equals(this.otnServiceType))) {
                            // LO-ODU needs to be created on a parent HO-ODU
                            // interface
                            List<OdtuTpnPool> presenceOdtu =
                                otnTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool();
                            if (presenceOdtu == null) {
                                this.unusableXpdrNWTps.add(tp.getTpId().getValue());
                            } else {
                                List<SupportedInterfaceCapability> sic =
                                    otnTp1.getTpSupportedInterfaces().getSupportedInterfaceCapability();
                                if ((findNetworkCompliantInterface(sic)) & (checkAvailableTribPort(tp))
                                    & (checkAvailableTribSlot(tp))) {
                                    this.availableXponderTp.put(tp.getTpId().getValue(),
                                        OpenroadmTpType.XPONDERNETWORK);
                                    availableNetworkTpNumber++;
                                }
                                /*
                                 * Add the retrieval of outgoing ingoing links
                                 * through an external function
                                 */
                            }
                        } else {
                            // service is HO service
                            List<SupportedInterfaceCapability> sic =
                                otnTp1.getTpSupportedInterfaces().getSupportedInterfaceCapability();
                            if (findNetworkCompliantInterface(sic)) {
                                this.availableXponderTp.put(tp.getTpId().getValue(), OpenroadmTpType.XPONDERNETWORK);
                                availableNetworkTpNumber++;
                                /*
                                 * Add the retrieval of outgoing ingoing links
                                 * through an external function
                                 */
                            } else {
                                this.unusableXpdrNWTps.add(tp.getTpId().getValue());

                            }

                        }

                    }

                }

                // The port is not a network port
            } else if (otnTp1.getTpType() == OpenroadmTpType.XPONDERCLIENT) {
                // For Client port we verify that it supports needed interfaces
                // TBD : How shall we check a client port is available and not
                // in use?
                List<SupportedInterfaceCapability> sic =
                    otnTp1.getTpSupportedInterfaces().getSupportedInterfaceCapability();
                if (findClientCompliantInterface(sic)) {
                    this.availableXponderTp.put(tp.getTpId().getValue(), OpenroadmTpType.XPONDERCLIENT);
                    availableClientTpNumber++;
                }
            }
            LOG.debug("initXndrTps: XPONDER tp = {} is used", tp.getTpId().getValue());
            LOG.error("initXndrTps: XPONDER {} NW TP doesn't have defined server ROADM SRG {}", this.toString(), tp
                .getTpId().getValue());
        }
        if ("AZ".equals(mode)) {
            if ((availableClientTpNumber >= 1) || (availableNetworkTpNumber >= 1)) {
                // for A and Z node we need to have one valid client port & one
                // valid network port
                this.valid = true;
            }
        } else if ("intermediate".equals(mode)) {
            if ((availableNetworkTpNumber >= 2)) {
                // for OTN switching node used in transit we need to have two
                // valid network ports
                this.valid = true;
            }
        }

        if (!isValid()) {
            LOG.debug("initXndrTps: XPONDER doesn't have the required ports available  {}", this.toString());
            return;
        } else {
            LOG.debug("initXndrTps: XPONDER {} is elligible", this.toString());
        }
    }

    private Boolean findClientCompliantInterface(List<SupportedInterfaceCapability> sic) {
        boolean compliant = false;
        for (SupportedInterfaceCapability sit : sic) {
            String interfacetype = sit.getIfCapType().toString();
            switch (interfacetype) {
                case "If1GEODU0":
                case "If1GE":
                    if ("1GE".equals(this.otnServiceType)) {
                        compliant = true;
                    }
                    break;
                case "If10GEODU2e":
                case "If10GE":
                    if ("10GE".equals(this.otnServiceType)) {
                        compliant = true;
                    }
                    break;
                case "If100GEODU4":
                case "If100GE":
                    if ("100GE".equals(this.otnServiceType)) {
                        compliant = true;
                    }
                    break;
                case "IfOTU4ODU4":
                case "IfOCHOTU4ODU4":
                    if (("OTU4".equals(this.otnServiceType)) || ("ODU4".equals(this.otnServiceType))) {
                        compliant = true;
                    }
                    break;
                default:
                    compliant = false;
                    break;
            }

        }
        return compliant;
    }

    private Boolean findNetworkCompliantInterface(List<SupportedInterfaceCapability> sic) {
        boolean compliant = false;
        for (SupportedInterfaceCapability sit : sic) {
            String interfacetype = sit.getIfCapType().toString();
            switch (interfacetype) {
                case "IfOTU4ODU4":
                case "IfOCHOTU4ODU4":
                    compliant = true;
                    break;
                case "IfOTU2ODU2":
                case "IfOCHOTU2ODU2":
                    if (("1GE".equals(this.otnServiceType)) || ("10GE".equals(this.otnServiceType))) {
                        compliant = true;
                    }
                    break;
                // add all use case with higher rate interfaces when it shows up
                default:
                    compliant = false;
                    break;
            }

        }
        return compliant;
    }

    private String getClliSupNodeId(Node inputNode) {
        TreeMap<String, String> allSupNodes = new TreeMap<String, String>();
        String tempNetworkSupNodeId = "";
        allSupNodes = MapUtils.getAllSupNode(inputNode);
        if (allSupNodes.get(NetworkUtils.CLLI_NETWORK_ID) == null) {
            LOG.error("getClliSupNodeId: No Supporting node at CLLI layer for node: [{}].", inputNode.getNodeId());
        } else {
            tempNetworkSupNodeId = allSupNodes.get(NetworkUtils.CLLI_NETWORK_ID);
        }
        return tempNetworkSupNodeId;
    }

    private String getNetworkSupNodeId(Node inputNode) {
        TreeMap<String, String> allSupNodes = new TreeMap<String, String>();
        String tempNetworkSupNodeId = "";
        allSupNodes = MapUtils.getAllSupNode(inputNode);
        if (allSupNodes.get(NetworkUtils.UNDERLAY_NETWORK_ID) == null) {
            LOG.error(
                "getNetworkSupNodeId: No Supporting node at NETWORK layer for node: [{}].", inputNode.getNodeId());
        } else {
            tempNetworkSupNodeId = allSupNodes.get(NetworkUtils.UNDERLAY_NETWORK_ID);
        }
        return tempNetworkSupNodeId;
    }

    private String getTopoSupNodeId(Node inputNode) {
        TreeMap<String, String> allSupNodes = new TreeMap<String, String>();
        String tempTopoSupNodeId = "";
        allSupNodes = MapUtils.getAllSupNode(inputNode);
        if (allSupNodes.get(NetworkUtils.OVERLAY_NETWORK_ID) == null) {
            LOG.error(
                "getTopologySupNodeId: No Supporting node at TOPOLOGY layer for node: [{}].", inputNode.getNodeId());
        } else {
            tempTopoSupNodeId = allSupNodes.get(NetworkUtils.OVERLAY_NETWORK_ID);
        }
        return tempTopoSupNodeId;
    }

    public void validateAZxponder(String anodeId, String znodeId) {
        if (!isValid()) {
            return;
        }
        if ((this.nodeType != OpenroadmNodeType.MUXPDR) & (this.nodeType != OpenroadmNodeType.SWITCH)
            & (this.nodeType != OpenroadmNodeType.TPDR)) {
            return;
        }
        // Detect A and Z, a/znodeId correspond to otn layer, supporting node
        // might be of Network or Topology layer
        if (this.nodeId.equals(anodeId) || (this.nodeId.equals(znodeId))) {
            initXndrTps("AZ");
            if (!this.valid) {
                LOG.debug("validateAZxponder: XPONDER unusable for A or Z == {}", nodeId.getValue());
            } else {
                LOG.info("validateAZxponder: A or Z node detected and validated == {}", nodeId.getValue());
            }
            return;
        } else {
            LOG.debug("validateAZxponder: XPONDER is ignored == {}", nodeId.getValue());
            valid = false;
        }

    }

    public boolean validateSwitchingPoolBandwidth(
            Node node,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                .network.node.TerminationPoint tp1,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                .network.node.TerminationPoint tp2,
            Long neededBW) {
        Long availableBW = 0L;
        if (this.nodeType != OpenroadmNodeType.TPDR) {
            return true;
        } else {
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1 node1 =
                node.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1.class);
            SwitchingPools sp = node1.getSwitchingPools();
            List<OduSwitchingPools> osp = new ArrayList<OduSwitchingPools>();
            osp = sp.getOduSwitchingPools();
            for (OduSwitchingPools ospx : osp) {
                List<NonBlockingList> nbl = ospx.getNonBlockingList();
                for (NonBlockingList nbll : nbl) {
                    if (nbll.getAvailableInterconnectBandwidth() >= neededBW) {
                        List<TpId> tplist = new ArrayList<TpId>(nbll.getTpList());
                        if ((tplist.contains(tp1.getTpId())) & (tplist.contains(tp2.getTpId()))) {
                            LOG.debug("validateSwitchingPoolBandwidth: couple  of tp {} x {} valid for crossconnection",
                                tp1.getTpId().toString(), tp2.getTpId().toString());
                            return true;
                        }
                    }
                }

            }
            LOG.debug("validateSwitchingPoolBandwidth: No valid Switching pool for crossconnecting tp {} and {}",
                tp1.getTpId().toString(), tp2.getTpId().toString());
            return false;
        }

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
        return;

    }

    public boolean checkAvailableTribPort(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                    .network.node.TerminationPoint tp) {
        boolean compatibleSupInt = false;
        TerminationPoint1 otnTp1 = tp.augmentation(TerminationPoint1.class);
        if (otnTp1.getTpType() == OpenroadmTpType.XPONDERNETWORK) {
            try {
                List<OdtuTpnPool> otpp = otnTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool();

                for (OdtuTpnPool otppi : otpp) {
                    if (otppi.getOdtuType().getClass().equals(ODTU4TsAllocated.class)) {
                        this.tpAvailableTribPort.put(tp.getTpId().getValue(), otppi.getTpnPool());
                        LOG.debug("checkAvailableTribPort: tp {} and his trib Ports have been added to "
                            + "tpAvailableTribPortMap", tp.getTpId().getValue());
                        compatibleSupInt = true;

                    }
                }
            } catch (NullPointerException e) {
                LOG.debug("checkAvailableTribPort: OdtuTpnPool not present for tp {} ", tp.getTpId().toString());
            }

        } else {
            LOG.debug("checkAvailableTribPort: tp {} has no odtu tpn Pool", tp.getTpId().getValue());
        }
        return compatibleSupInt;
    }

    public boolean checkAvailableTribSlot(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                .network.node.TerminationPoint tp) {
        boolean compatibleSupInt = false;
        TerminationPoint1 otnTp1 = tp.augmentation(TerminationPoint1.class);
        if (otnTp1.getTpType() == OpenroadmTpType.XPONDERNETWORK) {
            List<OdtuTpnPool> otpp;
            try {
                otpp = otnTp1.getXpdrTpPortConnectionAttributes().getOdtuTpnPool();

                for (OdtuTpnPool otppi : otpp) {
                    if (otppi.getOdtuType().getClass().equals(ODTU4TsAllocated.class)) {
                        this.tpAvailableTribSlot.put(
                            tp.getTpId().getValue(),
                            otnTp1.getXpdrTpPortConnectionAttributes().getTsPool());
                        LOG.debug(
                            "checkAvailableTribPort: tp {} and its trib Slots were added to tpAvailableTribSlotMap",
                            tp.getTpId().getValue());
                        compatibleSupInt = true;

                    }
                }

            } catch (NullPointerException e) {
                LOG.debug("checkAvailableTribSlot: OdtuTpnPool not present for tp {} ", tp.getTpId().toString());
            }
        } else {
            LOG.debug("checkAvailableTribPort: tp {} is not a network Port", tp.getTpId().getValue());
        }
        return compatibleSupInt;
    }

    public String getXpdrClient(String tp) {
        return this.clientPerNwTp.get(tp);
    }

    public boolean checkTP(String tp) {
        return !((this.usedXpdrNWTps.contains(tp)) || (this.usedXpdrClientTps.contains(tp))
            || (this.unusableXpdrNWTps.contains(tp)) || (this.unusableXpdrClientTps.contains(tp)));
    }

    public boolean isValid() {
        if ((node == null) || (nodeId == null) || (nodeType == null) || (supNetworkNodeId == null) || (clli == null)) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type, supporting nodeId");
            valid = false;
        }
        return valid;
    }

    public Map<String, OpenroadmTpType> getAvailableTps() {
        return availableXponderTp;
    }

    public void addOutgoingLink(PceLink outLink) {
        this.outgoingLinks.add(outLink);
    }

    public List<PceLink> getOutgoingLinks() {
        return outgoingLinks;
    }

    public String getClient(String tp) {
        return clientPerNwTp.get(tp);
    }

    public String getTopoSupNodeIdPceNode() {
        return supTopoNodeId;
    }

    public String getNetworkSupNodeIdPceNode() {
        return supNetworkNodeId;
    }

    public String getCLLI() {
        return clli;
    }

    public String toString() {
        return "PceNode type=" + nodeType + " ID=" + nodeId.getValue() + " CLLI=" + clli;
    }

    public void printLinksOfNode() {
        LOG.info(" outgoing links of node {} : {} ", nodeId.getValue(), this.getOutgoingLinks().toString());
    }

    public Map<String, List<Integer>> getAvailableTribPorts() {
        return tpAvailableTribPort;
    }

    public Map<String, List<Integer>> getAvailableTribSlots() {
        return tpAvailableTribSlot;
    }

}
