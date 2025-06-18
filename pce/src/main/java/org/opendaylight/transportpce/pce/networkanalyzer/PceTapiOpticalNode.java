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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.pce.SortPortsByName;
import org.opendaylight.transportpce.pce.node.mccapabilities.McCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SinkProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SourceProfileKey;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceTapiOpticalNode implements PceNode {
    private static final Logger LOG = LoggerFactory.getLogger(PceTapiOpticalNode.class);

    private boolean valid = true;
    private Node node;
    private Uuid nodeUuid;
    //deviceNodeId is used to keep trace of original node before disaggregation
    private String deviceNodeId;
    private Name nodeName;
    private OpenroadmNodeType nodeType;
    private AdministrativeState adminState;
    private OperationalState operationalState;
    private String serviceType;
    private McCapability mcCapability;
    private Map<String, OpenroadmTpType> availableSrgPp = new TreeMap<>();
    private Map<String, OpenroadmTpType> availableSrgCp = new TreeMap<>();
    private List<String> usedXpndrNWTps = new ArrayList<>();
    private List<PceLink> outgoingLinks = new ArrayList<>();
    private Map<String, String> clientPerNwTp = new HashMap<>();
    private BitSet frequenciesBitSet;
    private String version;

    private List<String> availableXpndrNWTps = new ArrayList<>();
    private List<BasePceNep> listOfNep = new ArrayList<>();

    /**
     * PceTapiOpticalNode is an abstracted Node in photonic layer corresponding to a Graph Vertex for path computation.
     *  Corresponds to the Photonic part of a XPONDER, or to one of the disaggregated elements of a ROADM (SRG/Degree).
     * @param serviceType   The service type which for PceTapiOpticalNodes is a photonic service : 100GEt, OTU4, OTUCN.
     * @param node          The Associated node in Tapi-Topology DataStore from which the Pce Node is abstracted.
     * @param nodeType      The Node Type as in OpenROADM topology layer. Currently handled : SRG, DEGREE, XPONDER.
     * @param version       The Node Version.
     * @param nepList       A list of the Node's relevant BasePceNep.
     * @param nodeId        A Map of node name associated to its Uuid (Contains a single element).
     * @param deviceNodeId  The node name
     * @param mcCapability  Media Channel Capabilities as defined in portMapping (SlotWidth/CenterFrequency Granularity)
     */
    public PceTapiOpticalNode(String serviceType, Node node, OpenroadmNodeType nodeType, String version,
            List<BasePceNep> nepList, Map<Uuid, Name> nodeId, String deviceNodeId, McCapability mcCapability) {


        this.serviceType = serviceType;
        this.node = node;
        this.nodeName = nodeId.entrySet().iterator().next().getValue();
        this.nodeType = nodeType;
        this.nodeUuid = nodeId.entrySet().iterator().next().getKey();
        this.deviceNodeId = deviceNodeId;
        this.version = version;
        this.listOfNep = nepList;
        this.adminState = node.getAdministrativeState();
        this.operationalState = node.getOperationalState();
        this.valid = true;
        this.mcCapability = mcCapability;
    }

    /**
     * Checks for SRGs availability of NEPs and CEPs and class them into availableSrgPp and availableSrgCp.
     */
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
            this.availableSrgPp.put(bpn.getNepCepUuid().getValue(),bpn.getTpType());
        }
        for (BasePceNep bpn : cpOtsNep) {
            this.availableSrgCp.put(bpn.getNepCepUuid().getValue(),bpn.getTpType());
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

    /**
     * Initialize the binaries coding spectrum use for the different WDM Optical ports of a PceNode.
     */
    public void initFrequenciesBitSet() {
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        // to set all bits to 0 (used/false) or 1 (available/true)
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        List<BitSet> bitsetList;
        LOG.debug("PTONLine153 Entering InitFreqBitset, for Node {}, nodeType {}", deviceNodeId, nodeType);
        switch (this.nodeType) {
            case SRG :
                bitsetList = listOfNep.stream()
                    .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.SRGRXCP)
                        || bpn.getTpType().equals(OpenroadmTpType.SRGTXCP)
                        || bpn.getTpType().equals(OpenroadmTpType.SRGTXRXCP))
                .map(BasePceNep::getFrequenciesBitSet)
                .collect(Collectors.toList());
                for (BitSet bitSet : bitsetList) {
                    if (bitSet == null) {
                        continue;
                    }
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
                    if (bitSet == null) {
                        continue;
                    }
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
                    if (bitSet == null) {
                        continue;
                    }
                    freqBitSet.and(bitSet);
                }
                this.frequenciesBitSet = freqBitSet;
                break;
            default:
                LOG.error("initFrequenciesBitSet: unsupported node type {} in node {}", this.nodeType, this);
                break;
        }
        LOG.debug("PTONLine202 InitFreqBitset, for Node {}, FreqBitset = {}",deviceNodeId, freqBitSet);
    }

    /**
     * Checks for XPONDERS availability of NEPs and CEPs and class them into clientOtsNep and nwOtsNep.
     *   NetworkNeps are filtered according to their ability to handle OTS LayerProtcolQualifier.
     *   List clientOtsNep is filled only with Client NEPs that are in visibility of Network through valid Network NEPs.
     *   Internal connectivity is checked using BasePceNep getNodeRuleGroupUuid and checking Node IRGs for switch and
     *   Muxponders.
     * @param serviceFormat Service Format, not used for optical Node, included to have common signatures of the method
     *                      towards the different kind of nodes.
     */
    public void initXndrTps(ServiceFormat serviceFormat) {
        LOG.debug("PTONLine 215: initXndrTps for node : {}, Uuid : {}", this.nodeName, this.nodeUuid);
        LOG.debug("PTONLine216: initXndrTps for node : {}, ListOfNep : {}", this.nodeName,
            listOfNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
        if (!isValid()) {
            LOG.debug("PTONLine 219: initXndrTps Non valid node : {}, Uuid : {}", this.nodeName, this.nodeUuid);
            return;
        }
        if (listOfNep.isEmpty()) {
            LOG.error("PceTapiOpticalNode initXndrTps: Xponder TerminationPoint list is empty for node : {}, Uuid : {}",
                 this.nodeName, this.nodeUuid);
            this.valid = false;
            return;
        }
        LOG.debug("PTONLine 228: nwOtsNep Protocols : {}", listOfNep.stream()
            .map(BasePceNep::getLpn).collect(Collectors.toList()));
        List<BasePceNep> nwOtsNep = listOfNep.stream()
            .filter(bpn -> ((bpn.getTpType().equals(OpenroadmTpType.XPONDERNETWORK)
                    || bpn.getTpType().equals(OpenroadmTpType.EXTPLUGGABLETP)
                    && (bpn.getLpq() != null
                    && bpn.getLpq().equals(PHOTONICLAYERQUALIFIEROTS.VALUE)))))
            .collect(Collectors.toList());
        List<BasePceNep> clientOtsNep = listOfNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.XPONDERCLIENT))
            .collect(Collectors.toList());
        for (BasePceNep bpn : nwOtsNep) {
            if (!this.usedXpndrNWTps.contains(bpn.getNepCepUuid().getValue())) {
                this.availableXpndrNWTps.add(bpn.getNepCepUuid().getValue());
            }
        }
        for (BasePceNep cbpn : clientOtsNep) {
            List<Uuid> clientNrgUuidList = cbpn.getNodeRuleGroupUuid();
            LOG.debug("PTONLine 244: clientNrgUuidList for bpn {} is : {}", cbpn.getName(), clientNrgUuidList);
            if (clientNrgUuidList == null || clientNrgUuidList.isEmpty()) {
                continue;
            }
            Uuid nwConnectedNepUuid = null;
            //First we check if a NW NEP includes a NRG common to the client ots NEP
            for (BasePceNep nwbpn : nwOtsNep) {
                List<Uuid> nwNrgUuidList = nwbpn.getNodeRuleGroupUuid();
                if (nwNrgUuidList == null || nwNrgUuidList.isEmpty()) {
                    continue;
                }
                List<Uuid> commonNrgUuidList = clientNrgUuidList.stream()
                    .filter(uuid -> nwNrgUuidList.contains(uuid))
                    .collect(Collectors.toList());
                //if we have common NRGs the NW Nep is  sharing the same NRGs and we can leave NW NEP Loop
                if (commonNrgUuidList != null && !commonNrgUuidList.isEmpty()) {
                    nwConnectedNepUuid = nwbpn.getNepCepUuid();
                    //As soon we have found a NW NEP sharing an NRG with the client NEP we stop scanning
                    //clientNrgUuidList
                    break;
                }
            }
            //If at the end of the scan of potential NW NEP candidate we identified one (Muxponder case)
            if (nwConnectedNepUuid != null) {
                //We add to clientPerNwNep the client Nep Uuid as well as the connected NW NEP Uuid
                this.clientPerNwTp.put(cbpn.getNepCepUuid().getValue(), nwConnectedNepUuid.getValue());
            //Otherwise we may have a different option such as for a switch and we need to find a IRG that interconnects
            //NRGs of Client to NRGs of Network ports
            } else {
                for (Uuid nrgUuid : clientNrgUuidList) {
                //  for each NRG we build a list of IRG key, that corresponds to IRG that includes the client NRG
                    List<InterRuleGroupKey> irgKeyList = node.getInterRuleGroup().entrySet().stream()
                          .filter(irg -> !irg.getValue().getAssociatedNodeRuleGroup().entrySet().stream()
                              .map(Map.Entry::getKey)
                              .filter(key -> key.getNodeRuleGroupUuid().equals(nrgUuid))
                              .collect(Collectors.toList()).isEmpty())
                          .collect(Collectors.toList())
                          .stream().map(Entry::getKey).collect(Collectors.toList());

                    if (irgKeyList != null && !irgKeyList.isEmpty()) {
                        //For each of IRG key, we check if corresponding IRG would include an NRG that corresponds
                        //to one of the NW NEP, meaning it would be interconnected to the client NEP via the IRG that
                        //interconnects their respective NRGs
                        for (BasePceNep nwbpn : nwOtsNep) {
                            List<Uuid> nwNrgUuidList = nwbpn.getNodeRuleGroupUuid();
                            LOG.debug("PTONLine 289: nwNrgUuidList : {}", nwNrgUuidList);
                            if (nwNrgUuidList == null || nwNrgUuidList.isEmpty()) {
                                continue;
                            }
                            for (Uuid nwNrgUuid : nwNrgUuidList) {
                                    //For each NRG of the NW NEP under analysis we build a list of IRG key,
                                    //that corresponds to IRG that includes the NW Nep's NRG
                                List<InterRuleGroupKey> nwIrgKeyList = node.getInterRuleGroup().entrySet().stream()
                                    .filter(irg -> !irg.getValue().getAssociatedNodeRuleGroup().entrySet().stream()
                                        .map(Map.Entry::getKey)
                                        .filter(key -> key.getNodeRuleGroupUuid().equals(nwNrgUuid))
                                        .collect(Collectors.toList()).isEmpty())
                                    .collect(Collectors.toList())
                                    .stream().map(Entry::getKey).collect(Collectors.toList());
                                for (InterRuleGroupKey nwIrgKey : nwIrgKeyList) {
                                    if (irgKeyList.contains(nwIrgKey)) {
                                        nwConnectedNepUuid = nwbpn.getNepCepUuid();
                                        break;
                                    }
                                }
                                if (nwConnectedNepUuid != null) {
                                    break;
                                }
                            }
                            if (nwConnectedNepUuid != null) {
                                break;
                            }
                        }
                    }
                    if (nwConnectedNepUuid != null) {
                        break;
                    }
                }
                if (nwConnectedNepUuid != null) {
                    this.clientPerNwTp.put(cbpn.getNepCepUuid().getValue(), nwConnectedNepUuid.getValue());
                    LOG.info("Found NW Nep {} connected to Client NEP {}", nwConnectedNepUuid, cbpn.getNepCepUuid());
                } else {
                    LOG.info("Did not succed finding a NW Nep connected to Client NEP {}", cbpn.getNepCepUuid());
                }
            }
        }
        LOG.debug("PTONLine330/initXndrTps: ListOfNep {}",
            listOfNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("PTONLine332/initXndrTps: clientOtsNep {}",
            clientOtsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("PTONLine334/initXndrTps: nwOtsNep {}",
            nwOtsNep.stream().map(BasePceNep::getName).collect(Collectors.toList()));
        LOG.debug("PTONLine336/initXndrTps: availableXpndrNWTps {}", availableXpndrNWTps);
        LOG.debug("PTONLine337/initXndrTps: clientPerNwTp {}", clientPerNwTp);


    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getRdmSrgClient()
     */
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

    /**
     * Check that basic generic parameters of the node are available.
     * @return  True if all required parameters have been set for the node.
     */
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

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getOperationalMode()
     */
    @Override
    public String getOperationalMode() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getXponderOperationnalMode()
     */
    @Override
    public String getXponderOperationalMode(XpdrNetworkAttributes tp) {
        return null;
    }

    /**
     * Retrieves port Operational mode from the standard/souce/sink-Profiles.
     * @param nepUuid   Uuid of the NEP associated to the network port.
     * @return First    Operational Mode that includes the rate of the service in its name, "UNKNOWN_MODE" if no
     *                  operational mode is populated in any of the NEPs' profiles.
     */
    @Override
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

    public void setUsedXpndrNWTps(List<String> listOfUsedNep) {
        this.usedXpndrNWTps.addAll(listOfUsedNep);
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#checkTP()
     */
    @Override
    public boolean checkTP(String tp) {
        return !this.usedXpndrNWTps.contains(tp);
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getOutgoingLinks()
     */
    @Override
    public List<PceLink> getOutgoingLinks() {
        return outgoingLinks;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getAdminStates()
     */
    @Override
    public AdminStates getAdminStates() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getAdminState()
     */
    @Override
    public AdministrativeState getAdminState() {
        return adminState;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getState()
     */
    @Override
    public State getState() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getOperationalState()
     */
    @Override
    public OperationalState getOperationalState() {
        return operationalState;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getNodeId()
     */
    @Override
    public NodeId getNodeId() {
        return  new NodeId(nodeName.getValue());
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getNodeUuid()
     */
    @Override
    public Uuid getNodeUuid() {
        return nodeUuid;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#toString()
     */
    @Override
    public String toString() {
        return "PceNode type=" + nodeType + " ID=" + nodeUuid.getValue() + " Name=" + node.getName().toString();
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getPceNodeType()
     */
    @Override
    public String getPceNodeType() {
        return "optical";
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getSupNetworkNodeId()
     */
    @Override
    public String getSupNetworkNodeId() {
        return deviceNodeId;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getSupClliNodeId()
     */
    @Override
    public String getSupClliNodeId() {
        return deviceNodeId;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#addOutgoingLink()
     */
    @Override
    public void addOutgoingLink(PceLink outLink) {
        this.outgoingLinks.add(outLink);
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getXpdrNWfromClient()
     */
    @Override
    public String getXpdrNWfromClient(String tp) {
        return this.clientPerNwTp.entrySet().stream()
            .filter(elt -> tp.equals(elt.getKey())).findFirst().orElseThrow().getValue();
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getXpdrAvailNW()
     */
    public List<String> getXpdrAvailNW() {
        return this.availableXpndrNWTps;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getAvailableTribPorts()
     */
    @Override
    public Map<String, List<Uint16>> getAvailableTribPorts() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getORNodeType()
     */
    @Override
    public OpenroadmNodeType getORNodeType() {
        return this.nodeType;
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getAvailableTribSlots()
     */
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
        return mcCapability.slotWidthGranularity();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#
     * getCentralFreqGranularity()
     */
    @Override
    public BigDecimal getCentralFreqGranularity() {
        return mcCapability.centerFrequencyGranularity();
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getMinSlots()
     */
    @Override
    public int getMinSlots() {
        return mcCapability.minSlots();
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getMaxSlots()
     */
    @Override
    public int getMaxSlots() {
        return mcCapability.maxSlots();
    }

    /*
     * (non-Javadoc)
     * @see org.opendaylight.transportpce.pce.networkanalyzer.PceNode#getListOfNep()
     */
    @Override
    public List<BasePceNep> getListOfNep() {
        return this.listOfNep;
    }

}
