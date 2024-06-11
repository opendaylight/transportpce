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
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.pce.SortPortsByName;
//import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.reroute
//.request.input.Endpoints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.available.freq.map.AvailFreqMapsKey;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOCH;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOCHOTU4ODU4;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOtsiOtsigroup;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Node;
import org.opendaylight.yangtools.yang.common.Uint16;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.end.point.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceTapiOpticalNode implements PceNode {
    private static final Logger LOG = LoggerFactory.getLogger(PceTapiOpticalNode.class);

    private boolean valid = true;
    private Node node;
    private Uuid nodeUuid;
    //deviceNodeId is used to keep trace of original node before disaggregation
    private String deviceNodeId;
    // see if Need global or local class for Name
    private Name nodeName;
    private OpenroadmNodeType nodeType;
    private AdministrativeState adminState;
    private OperationalState operationalState;
    private String serviceType;
    private Map<String, OpenroadmTpType> availableSrgPp = new TreeMap<>();
    private Map<String, OpenroadmTpType> availableSrgCp = new TreeMap<>();
    private List<String> usedXpndrNWTps = new ArrayList<>();
    private List<PceLink> outgoingLinks = new ArrayList<>();
    private Map<String, String> clientPerNwTp = new HashMap<>();
//    private final AvailFreqMapsKey freqMapKey = new AvailFreqMapsKey(GridConstant.C_BAND);
    private BitSet frequenciesBitSet;
    private String version;
    private BigDecimal slotWidthGranularity;
    private BigDecimal centralFreqGranularity;
//    private Endpoints endpoints;

//    private Map<Name, Uuid> availableAddDropNep = new TreeMap<>();
//    private Map<Name, Uuid> availableAddDropNepPp = new TreeMap<>();
//    private Map<Uuid, Uuid> availableDegreeNepTtp = new TreeMap<>();
//    private Map<Name, Uuid> usedXpndrNWTps = new TreeMap<>();
    private List<String> availableXpndrNWTps = new ArrayList<>();
//    private List<String> outgoingInternalLinks = new ArrayList<>();


    private List<BasePceNep> listOfNep = new ArrayList<>();
    /*
     * Complete description
     */

    public PceTapiOpticalNode(String serviceType, Node node, OpenroadmNodeType nodeType,
            String version, BigDecimal slotWidthGranularity, BigDecimal centralFreqGranularity,
            List<BasePceNep> nepList, Map<Uuid, Name> nodeId, String deviceNodeId) {


        this.serviceType = serviceType;
        this.node = node;
//        this.nodeName = node.getName().entrySet().iterator().next().getValue();
        this.nodeName = nodeId.entrySet().iterator().next().getValue();
        this.nodeType = nodeType;
        this.nodeUuid = nodeId.entrySet().iterator().next().getKey();
        this.deviceNodeId = deviceNodeId;
        this.version = version;
        this.slotWidthGranularity = slotWidthGranularity;
        this.centralFreqGranularity = centralFreqGranularity;
        this.listOfNep = nepList;
        this.adminState = node.getAdministrativeState();
        this.operationalState = node.getOperationalState();
        this.valid = false;
    }

    public void initSrgTps() {
        this.availableSrgPp.clear();
        this.availableSrgCp.clear();
        if (!isValid()) {
            return;
        }
        LOG.debug("initSrgTpList for Tapi: getting SRG tps from ROADM for node : {}, Uuid : {}",
            this.nodeName, this.nodeUuid);
        if (listOfNep.isEmpty()) {
            LOG.error("initSrgTpList: ROADM TerminationPoint list is empty for node : {}, Uuid : {}",
                 this.nodeName, this.nodeUuid);
            this.valid = false;
            return;
        }
        List<BasePceNep> ppOtsNep = listOfNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.SRGRXPP)
                || bpn.getTpType().equals(OpenroadmTpType.SRGTXPP) || bpn.getTpType().equals(OpenroadmTpType.SRGTXRXPP))
            .collect(Collectors.toList());
        List<BasePceNep> cpOtsNep = listOfNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.SRGRXCP)
                || bpn.getTpType().equals(OpenroadmTpType.SRGTXCP) || bpn.getTpType().equals(OpenroadmTpType.SRGTXRXCP))
            .collect(Collectors.toList());
        for (BasePceNep bpn : ppOtsNep) {
            this.availableSrgPp.put(bpn.getNepCepUuid().toString(),bpn.getTpType());
        }
        for (BasePceNep bpn : cpOtsNep) {
            this.availableSrgCp.put(bpn.getNepCepUuid().toString(),bpn.getTpType());
        }

        if (this.availableSrgPp.isEmpty() || this.availableSrgCp.isEmpty()) {
            LOG.error("initSrgTpList: ROADM SRG TerminationPoint list is empty for node : {}, Uuid : {}",
                this.nodeName, this.nodeUuid);
            this.valid = false;
            return;
        }
        LOG.debug("initSrgTpList: availableSrgPp size = {} && availableSrgCp size = {} in {}",
            this.availableSrgPp.size(), this.availableSrgCp.size(), this);
    }

    public void initFrequenciesBitSet() {
        if (!isValid()) {
            return;
        }
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        // to set all bits to 0 (used/false) or 1 (available/true)
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        List<BitSet> bitsetList;
        switch (this.nodeType) {
            case SRG :
                bitsetList = listOfNep.stream()
                    .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.SRGRXCP)
                        || bpn.getTpType().equals(OpenroadmTpType.SRGTXCP)
                        || bpn.getTpType().equals(OpenroadmTpType.SRGTXRXCP))
                .map(BasePceNep::getFrequenciesBitSet)
                .collect(Collectors.toList());
                for (BitSet bitSet : bitsetList) {
                    freqBitSet.and(bitSet);
                }
                this.frequenciesBitSet = freqBitSet;
                break;
            case DEGREE :
                bitsetList = listOfNep.stream()
                .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.DEGREERXTTP)
                    || bpn.getTpType().equals(OpenroadmTpType.DEGREETXTTP)
                    || bpn.getTpType().equals(OpenroadmTpType.DEGREETXRXTTP))
                .map(BasePceNep::getFrequenciesBitSet)
                .collect(Collectors.toList());
                for (BitSet bitSet : bitsetList) {
                    freqBitSet.and(bitSet);
                }
                this.frequenciesBitSet = freqBitSet;
                break;
            case XPONDER :
                bitsetList = listOfNep.stream()
                .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.XPONDERNETWORK))
                .map(BasePceNep::getFrequenciesBitSet)
                .collect(Collectors.toList());
                for (BitSet bitSet : bitsetList) {
                    freqBitSet.and(bitSet);
                }
                this.frequenciesBitSet = freqBitSet;
                break;
            default:
                LOG.error("initFrequenciesBitSet: unsupported node type {} in node {}", this.nodeType, this);
                break;
        }
    }

    public void initXndrTps(ServiceFormat serviceFormat) {
        LOG.debug("PceTapiOticalNode: initXndrTps for node : {}, Uuid : {}", this.nodeName, this.nodeUuid);
        if (!isValid()) {
            return;
        }
        if (listOfNep.isEmpty()) {
            LOG.error("PceTapiOticalNode/initXndrTps: Xponder TerminationPoint list is empty for node : {}, Uuid : {}",
                 this.nodeName, this.nodeUuid);
            this.valid = false;
            return;
        }

        List<BasePceNep> nwOtsNep = listOfNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.XPONDERNETWORK)
                || bpn.getTpType().equals(OpenroadmTpType.EXTPLUGGABLETP))
            .collect(Collectors.toList());
        List<BasePceNep> clientOtsNep = listOfNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.XPONDERCLIENT))
            .collect(Collectors.toList());
        for (BasePceNep bpn : nwOtsNep) {
            this.availableXpndrNWTps.add(bpn.getNepCepUuid().toString());
        }
        for (BasePceNep bpn : clientOtsNep) {
            // TODO: See if we can find a clever way to select network port rather than using the first
            this.clientPerNwTp.put(bpn.getNepCepUuid().toString(), this.availableXpndrNWTps.get(0));
        }
    }

    @Override
    public String getRdmSrgClient(String tp, String direction) {
        LOG.debug("TapiOpticalNode/getRdmSrgClient: Getting PP client for tp '{}' on node : {}, Uuid : {}",
            tp, this.nodeName, this.nodeUuid);
        OpenroadmTpType cpType = this.availableSrgCp.get(tp);
        if (cpType == null) {
            LOG.error("getRdmSrgClient: tp {} not found in SRG CPterminationPoint list", tp);
            return null;
        }
        List<BasePceNep> ppList = listOfNep.stream()
            .filter(bpn -> bpn.getVirtualNep().containsKey(new Uuid(UUID.fromString(tp).toString()))
                && (bpn.getTpType().equals(OpenroadmTpType.SRGRXPP) || bpn.getTpType().equals(OpenroadmTpType.SRGTXPP)
                || bpn.getTpType().equals(OpenroadmTpType.SRGTXRXPP)))
            .collect(Collectors.toList());
        LOG.debug("TapiOpticalNode/getRdmSrgClient:  Getting client PP for CP '{}'", tp);
        if (ppList.isEmpty()) {
            LOG.error("TapiOpticalNode/getRdmSrgClient: SRG TerminationPoint PP list is not available for node : {},"
                + " Uuid : {}", this.nodeName, this.nodeUuid);
            return null;
        }
        OpenroadmTpType srgType = null;
        switch (cpType) {
            case SRGTXRXCP:
                LOG.debug("TapiOpticalNode/getRdmSrgClient: Getting BI Directional PP port ...");
                // Take the first-element in the available PP key set
                if (ppList.iterator().next().getTpType().equals(OpenroadmTpType.SRGTXRXPP)) {
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
        final OpenroadmTpType openType = srgType;
//        Map<String, Map<NameKey, Name>> cpAssociatedSrgPp = new HashMap<>();
//        for (BasePceNep bpn : ppList.stream()
//                .filter(bpn -> bpn.getTpType().equals(openType))
//                .collect(Collectors.toList())) {
//            cpAssociatedSrgPp.put(bpn.getUuid().toString(), bpn.getName());
//        }
        // Map<String, OpenroadmTpType> cpAssociatedSrgPp = new HashMap<>();
        // for (BasePceNep bpn : ppList.stream()
        //      .filter(bpn -> bpn.getTpType().equals(openType))
        //      .collect(Collectors.toList())) {
        //    cpAssociatedSrgPp.put(bpn.getUuid().toString(), bpn.getTpType());
        // }
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
        //TODO: Adapt function since sorting port on Uuid (this is the Key used in TAPI may not be the best option
        // Also with T-API we try to use as much as possible port of transponder :
        // Identify if this method was used
        //only in case of Tunnel from PP to PP . Not easy to figure how it is used for TSP connected to PP
// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

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
        return null;
    }

    @Override
    public String getXponderOperationalMode(XpdrNetworkAttributes tp) {
        return null;
    }

    @Override
    public String getXpdrOperationalMode(Uuid nepUuid) {
        // TODO: when 2.4 models available, retrieve Operational modes from Profiles
        List<String> supportedOM = new ArrayList<>();

        if (supportedOM == null || supportedOM.isEmpty()) {
            LOG.warn("getOperationalMode: NetworkPort {} of Node {}  with Uuid {} has no operational mode declared ",
                nepUuid, this.nodeName, this.nodeUuid);
            return StringConstants.UNKNOWN_MODE;
        }
        for (String operationalMode : supportedOM) {
            if (operationalMode.contains(StringConstants.SERVICE_TYPE_RATE
                .get(this.serviceType).toCanonicalString())) {
                LOG.info(
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

    @Override
    public boolean checkTP(String tp) {
        return !this.usedXpndrNWTps.contains(tp);
    }

    public boolean isValid() {
        String nodeNName;
        if (node == null) {
            nodeNName = "NULL_NODE";
        } else {
            nodeNName = node.getName().toString();
        }
        if (node == null || nodeUuid == null || nodeType == null || adminState == null || operationalState == null) {
            LOG.error("TapiPceNode {},   nodeUuid {}  NodeType {} : one of parameters is not populated : nodeId, "
                + "node type, administrative state, operational state", nodeNName, nodeUuid, nodeType);
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
        return null;
    }

    @Override
    public AdministrativeState getAdminState() {
        return adminState;
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public OperationalState getOperationalState() {
        return operationalState;
    }

    @Override
    public NodeId getNodeId() {
        return  new NodeId(nodeName.toString());
    }

    @Override
    public Uuid getNodeUuid() {
        return nodeUuid;
    }

    @Override
    public String toString() {
        return "PceNode type=" + nodeType + " ID=" + nodeUuid.getValue() + " Name=" + node.getName().toString();
    }

    @Override
    public String getPceNodeType() {
        return "optical";
    }

    @Override
    public String getSupNetworkNodeId() {
        return deviceNodeId;
    }

    @Override
    public String getSupClliNodeId() {
        return deviceNodeId;
    }

    @Override
    public void addOutgoingLink(PceLink outLink) {
        this.outgoingLinks.add(outLink);
    }

    @Override
    public String getXpdrClient(String tp) {
        return this.clientPerNwTp.get(tp);
    }

    public List<String> getXpdrAvailNW() {
        return this.availableXpndrNWTps;
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
     * @see
     * org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getBitSetData()
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
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#
     * getSlotWidthGranularity()
     */
    @Override
    public BigDecimal getSlotWidthGranularity() {
        return slotWidthGranularity;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#
     * getCentralFreqGranularity()
     */
    @Override
    public BigDecimal getCentralFreqGranularity() {
        return centralFreqGranularity;
    }

    @Override
    public List<BasePceNep> getListOfNep() {
        return this.listOfNep;
    }

//    public void setEndpoints(Endpoints endpoints) {
//        this.endpoints = endpoints;
//    }
}
