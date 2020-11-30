/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
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
import java.util.Optional;
import java.util.TreeMap;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.pce.SortPortsByName;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
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
    private OpenroadmNodeType nodeType;

    // wavelength calculation per node type
    private List<Long> availableWLindex = new ArrayList<>();
    private Map<String, OpenroadmTpType> availableSrgPp = new TreeMap<>();
    private Map<String, OpenroadmTpType> availableSrgCp = new TreeMap<>();
    private List<String> usedXpndrNWTps = new ArrayList<>();
    private List<PceLink> outgoingLinks = new ArrayList<>();
    private Map<String, String> clientPerNwTp = new HashMap<>();
    private final AvailFreqMapsKey freqMapKey = new AvailFreqMapsKey(GridConstant.C_BAND);

    public PceOpticalNode(Node node, OpenroadmNodeType nodeType) {
        if (node != null && node.getNodeId() != null && nodeType != null) {
            this.node = node;
            this.nodeId = node.getNodeId();
            this.nodeType = nodeType;
        } else {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type");
            this.valid = false;
        }
    }

    public void initSrgTps() {
        this.availableSrgPp.clear();
        this.availableSrgCp.clear();
        if (!isValid()) {
            return;
        }
        LOG.info("initSrgTpList: getting SRG tps from ROADM node {}", this.nodeId);
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
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1 nttp1 = tp
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529
                        .TerminationPoint1.class);
            OpenroadmTpType type = cntp1.getTpType();
            LOG.info("type = {} for tp {}", type.getName(), tp);

            switch (type) {
                case SRGTXRXCP:
                case SRGRXCP:
                case SRGTXCP:
                    LOG.info("initSrgTpList: adding SRG-CP tp = {} ", tp.getTpId().getValue());
                    this.availableSrgCp.put(tp.getTpId().getValue(), cntp1.getTpType());
                    break;
                case SRGRXPP:
                case SRGTXPP:
                case SRGTXRXPP:
                    LOG.info("initSrgTpList: SRG-PP tp = {} found", tp.getTpId().getValue());
                    if (nttp1 == null || nttp1.getPpAttributes() == null
                            || nttp1.getPpAttributes().getUsedWavelength() == null
                            || nttp1.getPpAttributes().getUsedWavelength().values().isEmpty()) {
                        LOG.info("initSrgTpList: adding SRG-PP tp '{}'", tp.getTpId().getValue());
                        this.availableSrgPp.put(tp.getTpId().getValue(), cntp1.getTpType());
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
        LOG.info("initSrgTpList: availableSrgPp size = {} && availableSrgCp size = {} in {}",
            this.availableSrgPp.size(), this.availableSrgCp.size(), this);
    }

    public void initWLlist() {
        this.availableWLindex.clear();
        if (!isValid()) {
            return;
        }
        Node1 node1 = this.node.augmentation(Node1.class);
        byte[] freqMap;

        switch (this.nodeType) {
            case SRG :
                if (!node1.getSrgAttributes().nonnullAvailFreqMaps().containsKey(freqMapKey)) {
                    LOG.error("initWLlist: SRG no cband available freq maps for node  {}", this);
                    this.valid = false;
                    return;
                }
                freqMap = node1.getSrgAttributes().nonnullAvailFreqMaps().get(freqMapKey).getFreqMap();
                updateAvailableWlIndex(freqMap);
                break;
            case DEGREE :
                if (!node1.getDegreeAttributes().nonnullAvailFreqMaps().containsKey(freqMapKey)) {
                    LOG.error("initWLlist: DEG no cband available freq maps for node  {}", this);
                    this.valid = false;
                    return;
                }
                freqMap = node1.getDegreeAttributes().nonnullAvailFreqMaps().get(freqMapKey).getFreqMap();
                updateAvailableWlIndex(freqMap);
                break;
            case XPONDER :
                // HARD CODED 96
                for (long i = 1; i <= GridConstant.NB_OCTECTS; i++) {
                    this.availableWLindex.add(i);
                }
                break;
            default:
                LOG.error("initWLlist: unsupported node type {} in node {}", this.nodeType, this);
                break;
        }
        if (this.availableWLindex.isEmpty()) {
            LOG.debug("initWLlist: There are no available wavelengths in node {}", this);
            this.valid = false;
        }
        LOG.debug("initWLlist: availableWLindex size = {} in {}", this.availableWLindex.size(), this);
    }

    public void initXndrTps(ServiceFormat serviceFormat) {
        LOG.info("PceNod: initXndrTps for node : {}", this.nodeId);
        if (!isValid()) {
            return;
        }
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeTp =
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint> allTps = new ArrayList<>(nodeTp.nonnullTerminationPoint().values());
        if (allTps.isEmpty()) {
            this.valid = false;
            LOG.error("initXndrTps: XPONDER TerminationPoint list is empty for node {}", this);
            return;
        }
        this.valid = false;
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint tp : allTps) {
            TerminationPoint1 cntp1 = tp.augmentation(TerminationPoint1.class);
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1 nttp1 = tp
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529
                .TerminationPoint1.class);
            if (cntp1.getTpType() == OpenroadmTpType.XPONDERNETWORK) {
                if (nttp1 != null && nttp1.getXpdrNetworkAttributes().getWavelength() != null) {
                    this.usedXpndrNWTps.add(tp.getTpId().getValue());
                    LOG.info("initXndrTps: XPONDER tp = {} is used", tp.getTpId().getValue());
                } else {
                    this.valid = true;
                }
                // find Client of this network TP
                String client;
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev201019.TerminationPoint1 tpceTp1 =
                    tp.augmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev201019
                        .TerminationPoint1.class);
                if (tpceTp1 != null) {
                    client = tpceTp1.getAssociatedConnectionMapPort();
                    if (client != null) {
                        this.clientPerNwTp.put(tp.getTpId().getValue(), client);
                        this.valid = true;
                    } else {
                        LOG.error("initXndrTps: XPONDER {} NW TP doesn't have defined Client {}",
                            this, tp.getTpId().getValue());
                    }
                } else if (ServiceFormat.OTU.equals(serviceFormat)) {
                    LOG.info("Infrastructure OTU4 connection");
                    this.valid = true;
                } else {
                    LOG.error("Service Format {} not managed yet", serviceFormat.getName());
                }
            }
        }
        if (!isValid()) {
            LOG.error("initXndrTps: XPONDER doesn't have available wavelengths for node  {}", this);
        }
    }

    @Override
    public String getRdmSrgClient(String tp) {
        LOG.info("getRdmSrgClient: Getting PP client for tp '{}' on node : {}", tp, this.nodeId);
        OpenroadmTpType srgType = null;
        OpenroadmTpType cpType = this.availableSrgCp.get(tp);
        if (cpType == null) {
            LOG.error("getRdmSrgClient: tp {} not existed in SRG CPterminationPoint list", tp);
            return null;
        }
        switch (cpType) {
            case SRGTXRXCP:
                LOG.info("getRdmSrgClient: Getting BI Directional PP port ...");
                srgType = OpenroadmTpType.SRGTXRXPP;
                break;
            case SRGTXCP:
                LOG.info("getRdmSrgClient: Getting UNI Rx PP port ...");
                srgType = OpenroadmTpType.SRGRXPP;
                break;
            case SRGRXCP:
                LOG.info("getRdmSrgClient: Getting UNI Tx PP port ...");
                srgType = OpenroadmTpType.SRGTXPP;
                break;
            default:
                break;
        }
        LOG.info("getRdmSrgClient:  Getting client PP for CP '{}'", tp);
        if (!this.availableSrgPp.isEmpty()) {
            Optional<String> client = null;
            final OpenroadmTpType openType = srgType;
            client = this.availableSrgPp.entrySet()
                    .stream().filter(pp -> pp.getValue().getName().equals(openType.getName()))
                    .map(Map.Entry::getKey)
                    .sorted(new SortPortsByName())
                    .findFirst();
            if (!client.isPresent()) {
                LOG.error("getRdmSrgClient: ROADM {} doesn't have PP Client for CP {}", this, tp);
                return null;
            }
            LOG.info("getRdmSrgClient: client PP {} for CP {} found !", client, tp);
            return client.get();
        } else {
            LOG.error("getRdmSrgClient: SRG TerminationPoint PP list is not available for node {}", this);
            return null;
        }
    }


    public void validateAZxponder(String anodeId, String znodeId, ServiceFormat serviceFormat) {
        if (!isValid()) {
            return;
        }
        if (this.nodeType != OpenroadmNodeType.XPONDER) {
            return;
        }
        // Detect A and Z
        if (this.getSupNetworkNodeId().equals(anodeId) || (this.getSupNetworkNodeId().equals(znodeId))) {
            LOG.info("validateAZxponder: A or Z node detected == {}", nodeId.getValue());
            initXndrTps(serviceFormat);
            return;
        }
        LOG.debug("validateAZxponder: XPONDER is ignored == {}", nodeId.getValue());
        valid = false;
    }

    @Override
    public boolean checkTP(String tp) {
        return !this.usedXpndrNWTps.contains(tp);
    }

    @Override
    public boolean checkWL(long index) {
        return (this.availableWLindex.contains(index));
    }

    public boolean isValid() {
        if (node == null || nodeId == null || nodeType == null || this.getSupNetworkNodeId() == null
            || this.getSupClliNodeId() == null) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type, supporting nodeId");
            valid = false;
        }
        return valid;
    }

    @Override
    public List<PceLink> getOutgoingLinks() {
        return outgoingLinks;
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
    public Map<String, List<Uint16>> getAvailableTribSlots() {
        return null;
    }

    /**
     * Get available wave length from frequency map array.
     * @param freqMap byte[]
     */
    private void updateAvailableWlIndex(byte[] freqMap) {
        if (freqMap == null) {
            LOG.warn("No frequency map for node {}", node);
            this.valid = false;
            return;
        }
        long wlIndex = 1;
        for (int i = 0; i < freqMap.length; i++) {
            if (freqMap[i] == (byte)GridConstant.AVAILABLE_SLOT_VALUE) {
                LOG.debug("Adding channel {} to available wave length index",wlIndex);
                this.availableWLindex.add(wlIndex);
            }
            wlIndex++;
        }
    }
}
