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
import org.opendaylight.transportpce.pce.SortPortsByName;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceOpticalNode implements PceNode {
    private static final Logger LOG = LoggerFactory.getLogger(PceOpticalNode.class);

    private boolean valid = true;

    private final Node node;
    private final NodeId nodeId;
    private final OpenroadmNodeType nodeType;
    private final ServiceFormat serviceFormat;
    private final String pceNodeType;
    private AdminStates adminStates;
    private State state;

    // wavelength calculation per node type
    private List<Long> availableWLindex = new ArrayList<>();
    private Map<String, OpenroadmTpType> availableSrgPp = new TreeMap<>();
    private Map<String, OpenroadmTpType> availableSrgCp = new TreeMap<>();
    private List<String> usedXpndrNWTps = new ArrayList<>();
    private List<PceLink> outgoingLinks = new ArrayList<>();
    private Map<String, String> clientPerNwTp = new HashMap<>();

    public PceOpticalNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId, ServiceFormat serviceFormat,
        String pceNodeType) {
        this.node = node;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.serviceFormat = serviceFormat;
        this.pceNodeType = pceNodeType;
        if (node != null) {
            this.adminStates = node.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                    .Node1.class).getAdministrativeState();
            this.state = node.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                    .Node1.class).getOperationalState();
        }

        if ((node == null) || (nodeId == null) || (nodeType == null) || (adminStates == null) || state == null) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type, adminstate, state");
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
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1 nttp1 = tp
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                .TerminationPoint1.class);
            OpenroadmTpType type = cntp1.getTpType();
            LOG.info("type = {} for tp {}", type.getName(), tp);

            switch (type) {
                case SRGTXRXCP:
                case SRGRXCP:
                case SRGTXCP:
                    if (State.InService.equals(cntp1.getOperationalState())) {
                        LOG.info("initSrgTpList: adding SRG-CP tp = {} ", tp.getTpId().getValue());
                        this.availableSrgCp.put(tp.getTpId().getValue(), cntp1.getTpType());
                    }
                    break;
                case SRGRXPP:
                case SRGTXPP:
                case SRGTXRXPP:
                    boolean used = true;
                    LOG.info("initSrgTpList: SRG-PP tp = {} found", tp.getTpId().getValue());
                    try {
                        List<UsedWavelength> usedWavelengths =
                            new ArrayList<>(nttp1.getPpAttributes().getUsedWavelength().values());
                        if (usedWavelengths.isEmpty()) {
                            used = false;
                        }
                    } catch (NullPointerException e) {
                        LOG.warn("initSrgTpList: 'usedWavelengths' for tp={} is null !", tp.getTpId().getValue());
                        used = false;
                    }
                    if (!used) {
                        if (State.InService.equals(cntp1.getOperationalState())) {
                            LOG.info("initSrgTpList: adding SRG-PP tp '{}'", tp.getTpId().getValue());
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
        LOG.info("initSrgTpList: availableSrgPp size = {} && availableSrgCp size = {} in {}",
            this.availableSrgPp.size(), this.availableSrgCp.size(), this);
    }

    public void initWLlist() {
        this.availableWLindex.clear();
        if (!isValid()) {
            return;
        }
        Node1 node1 = this.node.augmentation(Node1.class);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 node11 =
                this.node.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1
                        .class);
        switch (this.nodeType) {
            case SRG :
                if (State.InService.equals(node11.getOperationalState())) {
                    List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                            .AvailableWavelengths> srgAvailableWL =
                            new ArrayList<>(node1.getSrgAttributes().nonnullAvailableWavelengths().values());
                    if (srgAvailableWL.isEmpty()) {
                        this.valid = false;
                        LOG.error("initWLlist: SRG AvailableWavelengths is empty for node  {}", this);
                        return;
                    }
                    for (org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                            .AvailableWavelengths awl : srgAvailableWL) {
                        this.availableWLindex.add(awl.getIndex().toJava());
                        LOG.debug("initWLlist: SRG next = {} in {}", awl.getIndex(), this);
                    }
                } else {
                    this.valid = false;
                    LOG.error("initWLlist: SRG node {} is OOS/degraded", this);
                    return;
                }
                break;
            case DEGREE :
                if (State.InService.equals(node11.getOperationalState())) {
                    List<org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes
                            .AvailableWavelengths> degAvailableWL =
                            new ArrayList<>(node1.getDegreeAttributes().nonnullAvailableWavelengths().values());
                    if (degAvailableWL.isEmpty()) {
                        this.valid = false;
                        LOG.error("initWLlist: DEG AvailableWavelengths is empty for node  {}", this);
                        return;
                    }
                    for (org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes
                            .AvailableWavelengths awl : degAvailableWL) {
                        this.availableWLindex.add(awl.getIndex().toJava());
                        LOG.debug("initWLlist: DEGREE next = {} in {}", awl.getIndex(), this);
                    }
                } else {
                    this.valid = false;
                    LOG.error("initWLlist: Degree node {} is OOS/degraded", this);
                    return;
                }
                break;
            case XPONDER :
                if (State.InService.equals(node11.getOperationalState())) {
                    // HARD CODED 96
                    for (long i = 1; i <= 96; i++) {
                        this.availableWLindex.add(i);
                    }
                } else {
                    this.valid = false;
                    LOG.error("initWLlist: XPDR node {} is OOS/degraded", this);
                    return;
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

    public void initXndrTps() {
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
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1 nttp1 = tp
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                .TerminationPoint1.class);
            if (cntp1.getTpType() == OpenroadmTpType.XPONDERNETWORK) {
                if (State.InService.equals(cntp1.getOperationalState())) {
                    if (nttp1 != null && nttp1.getXpdrNetworkAttributes().getWavelength() != null) {
                        this.usedXpndrNWTps.add(tp.getTpId().getValue());
                        LOG.info("initXndrTps: XPONDER tp = {} is used", tp.getTpId().getValue());
                    } else {
                        this.valid = true;
                    }
                    // find Client of this network TP
                    String client;
                    org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1 tpceTp1 =
                            tp.augmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129
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
                    } else if (ServiceFormat.OTU.equals(this.serviceFormat)) {
                        LOG.info("Infrastructure OTU4 connection");
                        this.valid = true;
                    } else {
                        LOG.error("Service Format {} not managed yet", this.serviceFormat.getName());
                    }
                } else {
                    LOG.warn("initXndrTps: XPONDER tp = {} is OOS/degraded", tp.getTpId().getValue());
                    this.valid = false;
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


    public void validateAZxponder(String anodeId, String znodeId) {
        if (!isValid()) {
            return;
        }
        if (this.nodeType != OpenroadmNodeType.XPONDER) {
            return;
        }
        // Detect A and Z
        if (this.getSupNetworkNodeId().equals(anodeId) || (this.getSupNetworkNodeId().equals(znodeId))) {
            LOG.info("validateAZxponder: A or Z node detected == {}", nodeId.getValue());
            initXndrTps();
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
            || this.getSupClliNodeId() == null || adminStates == null || state == null) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type, supporting nodeId, "
                    + "admin state, operational state");
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
}
