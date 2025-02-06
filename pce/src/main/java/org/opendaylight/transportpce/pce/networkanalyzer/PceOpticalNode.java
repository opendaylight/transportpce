/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.pce.SortPortsByName;
import org.opendaylight.transportpce.pce.networkanalyzer.port.Preference;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.reroute.request.input.Endpoints;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOtsiOtsigroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.mode.attributes.supported.operational.modes.OperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.mode.attributes.supported.operational.modes.OperationalModeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceOpticalNode implements PceNode {
    private static final Logger LOG = LoggerFactory.getLogger(PceOpticalNode.class);

    private boolean valid = true;

    private Node node;
    private NodeId nodeId;
    private String deviceNodeId;
    private OpenroadmNodeType nodeType;
    private AdminStates adminStates;
    private State state;
    private String serviceType;
    private PortMapping portMapping;

    private Map<String, OpenroadmTpType> availableSrgPp = new TreeMap<>();
    private Map<String, OpenroadmTpType> availableSrgCp = new TreeMap<>();
    private List<String> usedXpndrNWTps = new ArrayList<>();
    private List<PceLink> outgoingLinks = new ArrayList<>();
    private Map<String, String> clientPerNwTp = new HashMap<>();
    private final AvailFreqMapsKey freqMapKey = new AvailFreqMapsKey(GridConstant.C_BAND);
    private BitSet frequenciesBitSet;
    private String version;
    private BigDecimal slotWidthGranularity;
    private BigDecimal centralFreqGranularity;
    private Endpoints endpoints;

    public PceOpticalNode(String deviceNodeId, String serviceType, PortMapping portMapping, Node node,
        OpenroadmNodeType nodeType, String version, BigDecimal slotWidthGranularity,
                          BigDecimal centralFreqGranularity) {

        if (deviceNodeId != null
                && serviceType != null
                && portMapping != null
                && node != null
                && node.getNodeId() != null
                && nodeType != null
                && version != null
                && slotWidthGranularity != null) {
            this.deviceNodeId = deviceNodeId;
            this.serviceType = serviceType;
            this.portMapping = portMapping;
            this.node = node;
            this.nodeId = node.getNodeId();
            this.nodeType = nodeType;
            this.version = version;
            this.slotWidthGranularity = slotWidthGranularity;
            this.centralFreqGranularity = centralFreqGranularity;
            this.adminStates = node.augmentation(org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev230526.Node1.class).getAdministrativeState();
            this.state = node.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.common.network.rev230526.Node1.class).getOperationalState();
        } else {
            LOG.error("PceNode {} : one of parameters is not populated : nodeId, node type, slot width granularity",
                deviceNodeId);
            this.valid = false;
        }
    }

    public void initSrgTps(Preference portPreference) {
        this.availableSrgPp.clear();
        this.availableSrgCp.clear();
        if (!isValid()) {
            return;
        }
        LOG.debug("initSrgTpList: getting SRG tps from ROADM node {}", this.nodeId);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeTp =
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint> allTps = new ArrayList<>(nodeTp.nonnullTerminationPoint().values());
        if (allTps.isEmpty()) {
            LOG.error("initSrgTpList: ROADM TerminationPoint list is empty for node {}", this);
            this.valid = false;
            return;
        }
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint tp : allTps) {
            TerminationPoint1 cntp1 = tp.augmentation(TerminationPoint1.class);
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1 nttp1 = tp
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                        .TerminationPoint1.class);
            OpenroadmTpType type = cntp1.getTpType();
            LOG.debug("type = {} for tp {}", type.getName(), tp);

            switch (type) {
                case SRGTXRXCP:
                case SRGRXCP:
                case SRGTXCP:
                    if (State.InService.equals(cntp1.getOperationalState())) {
                        LOG.debug("initSrgTpList: adding SRG-CP tp = {} ", tp.getTpId().getValue());
                        this.availableSrgCp.put(tp.getTpId().getValue(), cntp1.getTpType());
                    }
                    break;
                case SRGRXPP:
                case SRGTXPP:
                case SRGTXRXPP:
                    LOG.debug("initSrgTpList: SRG-PP tp = {} found", tp.getTpId().getValue());
                    if (!portPreference.isPreferredPort(nodeId.getValue(), tp.getTpId().getValue())) {
                        LOG.warn("initSrgTpList: SRG-PP tp = {} is rejected by the client", tp.getTpId().getValue());
                        break;
                    }
                    if (isTerminationPointAvailable(nttp1)) {
                        LOG.debug("initSrgTpList: adding SRG-PP tp '{}'", tp.getTpId().getValue());
                        this.availableSrgPp.put(tp.getTpId().getValue(), cntp1.getTpType());
                        if (State.InService.equals(cntp1.getOperationalState())) {
                            LOG.debug("initSrgTpList: adding SRG-PP tp '{}'", tp.getTpId().getValue());
                            this.availableSrgPp.put(tp.getTpId().getValue(), cntp1.getTpType());
                        }
                    } else {
                        LOG.warn("initSrgTpList: SRG-PP tp = {} found is busy !!", tp.getTpId().getValue());
                    }
                    break;
                default:
                    break;
            }
        }
        if (this.availableSrgPp.isEmpty() || this.availableSrgCp.isEmpty()) {
            LOG.error("initSrgTpList: ROADM SRG TerminationPoint list is empty for node {}", this);
            this.valid = false;
            return;
        }
        LOG.debug("initSrgTpList: availableSrgPp size = {} && availableSrgCp size = {} in {}",
            this.availableSrgPp.size(), this.availableSrgCp.size(), this);
    }

    private boolean isTerminationPointAvailable(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1 nttp1) {
        byte[] availableByteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(availableByteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        return nttp1 == null || nttp1.getPpAttributes() == null
                || nttp1.getPpAttributes().getAvailFreqMaps() == null
                || !nttp1.getPpAttributes().getAvailFreqMaps().containsKey(freqMapKey)
                || nttp1.getPpAttributes().getAvailFreqMaps().get(freqMapKey).getFreqMap() == null
                || Arrays.equals(nttp1.getPpAttributes().getAvailFreqMaps().get(freqMapKey).getFreqMap(),
                        availableByteArray);
    }

    private boolean isTpWithGoodCapabilities(
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node
        .TerminationPoint tp) {
        Mapping mapping = this.portMapping.getMapping(deviceNodeId, tp.getTpId().getValue());
        if (mapping == null || mapping.getSupportedInterfaceCapability() == null) {
            return true;
        }
        switch (this.serviceType) {
            case "400GE":
                for (SupportedIfCapability ifCap : mapping.getSupportedInterfaceCapability()) {
                    if (ifCap.equals(IfOtsiOtsigroup.VALUE)) {
                        return true;
                    }
                }
                return false;
            case "100GE":
                return mapping.getSupportedInterfaceCapability().contains(IfOCH.VALUE)
                        || mapping.getSupportedInterfaceCapability().contains(IfOCHOTU4ODU4.VALUE);
            default:
                return true;
        }
    }

    public void initFrequenciesBitSet() {
        if (!isValid()) {
            return;
        }
        Node1 node1 = this.node.augmentation(Node1.class);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1 node11 =
                this.node.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1
                        .class);
        switch (this.nodeType) {
            case SRG :
                if (!State.InService.equals(node11.getOperationalState())) {
                    this.valid = false;
                    LOG.error("initWLlist: SRG node {} is OOS/degraded", this);
                    return;
                }
                if (!node1.getSrgAttributes().nonnullAvailFreqMaps().containsKey(freqMapKey)) {
                    LOG.error("initFrequenciesBitSet: SRG no cband available freq maps for node  {}", this);
                    this.valid = false;
                    return;
                }
                this.frequenciesBitSet = BitSet.valueOf(node1.getSrgAttributes()
                        .nonnullAvailFreqMaps().get(freqMapKey).getFreqMap());
                break;
            case DEGREE :
                if (!State.InService.equals(node11.getOperationalState())) {
                    this.valid = false;
                    LOG.error("initWLlist: Degree node {} is OOS/degraded", this);
                    return;
                }
                if (!node1.getDegreeAttributes().nonnullAvailFreqMaps().containsKey(freqMapKey)) {
                    LOG.error("initFrequenciesBitSet: DEG no cband available freq maps for node  {}", this);
                    this.valid = false;
                    return;
                }
                this.frequenciesBitSet = BitSet.valueOf(node1.getDegreeAttributes()
                        .nonnullAvailFreqMaps().get(freqMapKey).getFreqMap());
                break;
            case XPONDER :
                // at init all bits are set to false (unavailable)
                this.frequenciesBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
                //set all bits to true (available)
                this.frequenciesBitSet.set(0, GridConstant.EFFECTIVE_BITS);
                if (!State.InService.equals(node11.getOperationalState())) {
                    this.valid = false;
                    LOG.error("initWLlist: XPDR node {} is OOS/degraded", this);
                }
                break;
            default:
                LOG.error("initFrequenciesBitSet: unsupported node type {} in node {}", this.nodeType, this);
                break;
        }
    }

    public void initXndrTps(ServiceFormat serviceFormat) {
        LOG.debug("PceNod: initXndrTps for node : {}", this.nodeId);
        if (!isValid()) {
            return;
        }
        this.valid = false;
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeTp =
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint> allTps = new ArrayList<>(nodeTp.nonnullTerminationPoint().values());
        if (allTps.isEmpty()) {
            LOG.error("initXndrTps: XPONDER TerminationPoint list is empty for node {}", this);
            return;
        }
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
                .node.TerminationPoint tp : allTps) {
            TerminationPoint1 cntp1 = tp.augmentation(TerminationPoint1.class);
            if (cntp1 == null) {
                LOG.error("initXndrTps: {} - {} has no tp type", this.nodeId, tp.getTpId().toString());
                continue;
            }
            if (cntp1.getTpType() != OpenroadmTpType.XPONDERNETWORK) {
                LOG.debug("initXndrTps: {} is not an Xponder network port", cntp1.getTpType().getName());
                continue;
            }
            if (!isTpWithGoodCapabilities(tp)) {
                LOG.warn("initXndrTps: {} network port has not correct if-capabilities", tp.getTpId().getValue());
                continue;
            }
            if (!State.InService.equals(cntp1.getOperationalState())) {
                LOG.warn("initXndrTps: XPONDER tp = {} is OOS/degraded", tp.getTpId().getValue());
                continue;
            }
            if (endpoints == null
                    || (!endpoints.getAEndTp().equals(tp.getTpId().getValue())
                        && !endpoints.getZEndTp().equals(tp.getTpId().getValue()))) {
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1 nttp1 =
                        tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                                .TerminationPoint1.class);
                if (nttp1 != null && nttp1.getXpdrNetworkAttributes().getWavelength() != null) {
                    this.usedXpndrNWTps.add(tp.getTpId().getValue());
                    LOG.debug("initXndrTps: XPONDER tp = {} is used", tp.getTpId().getValue());
                    continue;
                }
            }
            // find Client of this network TP
            if (cntp1.getAssociatedConnectionMapTp() != null) {
                String client = cntp1.getAssociatedConnectionMapTp().iterator().next().getValue();
                if (client != null) {
                    this.clientPerNwTp.put(tp.getTpId().getValue(), client);
                    this.valid = true;
                } else {
                    LOG.error("Service Format {} not managed yet", serviceFormat.getName());
                }
            } else {
                this.valid = true;
            }
        }
        if (!isValid()) {
            LOG.error("initXndrTps: XPONDER doesn't have available wavelengths for node  {}", this);
        }
    }

    @Override
    public String getRdmSrgClient(String tp, String direction) {
        LOG.debug("getRdmSrgClient: Getting PP client for tp '{}' on node : {}", tp, this.nodeId);
        if (this.availableSrgPp.isEmpty()) {
            LOG.error("getRdmSrgClient: SRG TerminationPoint PP list is not available for node {}", this);
            return null;
        }
        OpenroadmTpType srgType = null;
        OpenroadmTpType cpType = this.availableSrgCp.get(tp);
        if (cpType == null) {
            LOG.error("getRdmSrgClient: tp {} not existed in SRG CPterminationPoint list", tp);
            return null;
        }
        switch (cpType) {
            case SRGTXRXCP:
                LOG.debug("getRdmSrgClient: Getting BI Directional PP port ...");
                // Take the first-element in the available PP key set
                if (availableSrgPp.entrySet().iterator().next().getKey()
                        // and check if the port is bidirectional
                        .contains("TXRX")) {
                    srgType = OpenroadmTpType.SRGTXRXPP;
                } else if (direction.equalsIgnoreCase("aToz")) {
                    srgType = OpenroadmTpType.SRGRXPP;
                } else {
                    srgType = OpenroadmTpType.SRGTXPP;
                }
                break;
            case SRGTXCP:
                LOG.debug("getRdmSrgClient: Getting UNI Rx PP port ...");
                srgType = OpenroadmTpType.SRGRXPP;
                break;
            case SRGRXCP:
                LOG.debug("getRdmSrgClient: Getting UNI Tx PP port ...");
                srgType = OpenroadmTpType.SRGTXPP;
                break;
            default:
                break;
        }
        LOG.debug("getRdmSrgClient:  Getting client PP for CP '{}'", tp);
        final OpenroadmTpType openType = srgType;
        Optional<String> client = this.availableSrgPp.entrySet()
                .stream().filter(pp -> pp.getValue().getName().equals(openType.getName()))
                .map(Map.Entry::getKey).min(new SortPortsByName());
        if (client.isEmpty()) {
            LOG.error("getRdmSrgClient: ROADM {} doesn't have PP Client for CP {}", this, tp);
            return null;
        }
        LOG.debug("getRdmSrgClient: client PP {} for CP {} found !", client, tp);
        return client.orElseThrow();
    }

    @Override
    public String getOperationalMode() {
        Node1 node1 = this.node.augmentation(Node1.class);
        if (node1 == null) {
            LOG.warn("No openroadm node available for node {}", node);
            return "";
        }
        switch (this.nodeType) {
            case SRG :
                if (node1.getSrgAttributes().getSupportedOperationalModes() == null
                        || node1.getSrgAttributes().getSupportedOperationalModes().stream().findFirst().isEmpty()) {
                    LOG.debug("getOperationalMode: SRG has no operational mode declared");
                    return StringConstants.UNKNOWN_MODE;
                } else {
                    LOG.debug("getOperationalMode: SRG has operational mode declared {}",
                        node1.getSrgAttributes().getSupportedOperationalModes().stream().findFirst().toString());
                    return node1.getSrgAttributes().getSupportedOperationalModes().stream().findFirst().toString();
                }
            case DEGREE :
                if (node1.getDegreeAttributes().getSupportedOperationalModes() == null
                        || node1.getDegreeAttributes().getSupportedOperationalModes().stream().findFirst().isEmpty()) {
                    LOG.debug("getOperationalMode: DEGREE has no operational mode declared");
                    return StringConstants.UNKNOWN_MODE;
                } else {
                    LOG.debug("getOperationalMode: DEGREE has operational mode declared {}",
                        node1.getDegreeAttributes().getSupportedOperationalModes().stream().findFirst().toString());
                    return node1.getDegreeAttributes().getSupportedOperationalModes().stream().findFirst().toString();
                }
            default:
                LOG.debug("getOperationalMode: Did not succeed retrieving Operational Mode for the node");
                return "";
        }
    }

    @Override
    public String getXponderOperationalMode(XpdrNetworkAttributes tp) {
        if (tp.getSupportedOperationalModes() == null) {
            LOG.warn("getOperationalMode: NetworkPort {} has no operational mode declared compatible with service type",
                tp);
            return StringConstants.UNKNOWN_MODE;
        }
        for (Map.Entry<OperationalModeKey, OperationalMode> mode : tp.getSupportedOperationalModes()
                .getOperationalMode().entrySet()) {
            if (mode.getKey().toString().contains(StringConstants.SERVICE_TYPE_RATE
                    .get(this.serviceType).toCanonicalString())) {
                LOG.info("getOperationalMode: NetworkPort {}  has {} operational mode declared", tp,
                    mode.getKey().toString());
                return mode.getKey().toString();
            }
        }
        LOG.warn("getOperationalMode: NetworkPort {}  has no operational mode declared compatible with service type",
            tp);
        return StringConstants.UNKNOWN_MODE;
    }

    public void validateAZxponder(String anodeId, String znodeId, ServiceFormat serviceFormat) {
        if (!isValid() || this.nodeType != OpenroadmNodeType.XPONDER) {
            return;
        }
        // Detect A and Z
        if (anodeId.contains(this.getSupNetworkNodeId()) || (znodeId.contains(this.getSupNetworkNodeId()))) {
            LOG.info("validateAZxponder: A or Z node detected == {}", nodeId.getValue());
            initXndrTps(serviceFormat);
            return;
        }
        LOG.debug("validateAZxponder: XPONDER == {} is ignored, supported by {} for aNodeId {} ", nodeId.getValue(),
            this.getSupNetworkNodeId(), anodeId);
        valid = false;
    }

    @Override
    public boolean checkTP(String tp) {
        return !this.usedXpndrNWTps.contains(tp);
    }

    public boolean isValid() {
        if (node == null || nodeId == null || nodeType == null || this.getSupNetworkNodeId() == null
                || this.getSupClliNodeId() == null || adminStates == null || state == null) {
            LOG.error("PceNode {},   nodeId {}  NodeType {} : one of parameters is not populated : nodeId, node type,"
                + " supporting nodeId, admin state, operational state", deviceNodeId, nodeId, nodeType);
            valid = false;
        }
        return valid;
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
    public NodeId getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "PceNode type=" + nodeType + " ID=" + nodeId.getValue() + " CLLI=" + this.getSupClliNodeId();
    }

    @Override
    public String getPceNodeType() {
        return "optical";
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
    public void addOutgoingLink(PceLink outLink) {
        this.outgoingLinks.add(outLink);
    }

    @Override
    public String getXpdrClient(String tp) {
        return this.clientPerNwTp.get(tp);
    }

    @Override
    public Map<String, List<Uint16>> getAvailableTribPorts() {
        return null;
    }

    @Override
    public OpenroadmNodeType getORNodeType() {
        return this.nodeType;
    }

    @Override
    public Map<String, List<Uint16>> getAvailableTribSlots() {
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getBitSetData()
    */
    @Override
    public BitSet getBitSetData() {
        return this.frequenciesBitSet;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getVersion()
    */
    @Override
    public String getVersion() {
        return this.version;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getSlotWidthGranularity()
    */
    @Override
    public BigDecimal getSlotWidthGranularity() {
        return slotWidthGranularity;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getCentralFreqGranularity()
     */
    @Override
    public BigDecimal getCentralFreqGranularity() {
        return centralFreqGranularity;
    }

    public void setEndpoints(Endpoints endpoints) {
        this.endpoints = endpoints;
    }

}
