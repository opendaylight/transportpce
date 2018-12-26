/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceNode {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);
    ////////////////////////// NODES ///////////////////////////
    /*
     */
    private boolean valid = true;
    private final Node node;
    private final NodeId nodeId;
    private final OpenroadmNodeType nodeType;
    // wavelength calculation per node type
    private List<Long> availableWLindex = new ArrayList<Long>();
    private Map<String, OpenroadmTpType> availableSrgPp = new TreeMap<String, OpenroadmTpType>();
    private Map<String, OpenroadmTpType> availableSrgCp = new TreeMap<String, OpenroadmTpType>();
    private List<String> usedXpndrNWTps = new ArrayList<String>();
    private List<PceLink> outgoingLinks = new ArrayList<PceLink>();
    private Map<String, String> clientPerNwTp = new HashMap<String, String>();
    private PortMapping portMapping;
    private boolean isUnidir;

    public PceNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId, PortMapping portMapping) {
        this.node = node;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        if ((node == null) || (nodeId == null) || (nodeType == null)) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type");
            this.valid = false;
        }
        this.portMapping = portMapping;
        this.isUnidir = false;
    }

    public void initSrgTps() {
        this.availableSrgPp.clear();
        this.availableSrgCp.clear();
        if (!isValid()) {
            return;
        }
        LOG.info("initSrgTpList: getting SRG tps from ROADM node {}", this.nodeId);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1 nodeTp =
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                                .rev150608.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
            .TerminationPoint> allTps =
                nodeTp.getTerminationPoint();
        if (allTps == null) {
            LOG.error("initSrgTpList: ROADM TerminationPoint list is empty for node {}", this.toString());
            this.valid = false;
            return;
        }
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
                .TerminationPoint tp : allTps) {
            TerminationPoint1 tp1 = tp.augmentation(TerminationPoint1.class);
            OpenroadmTpType type = tp1.getTpType();
            switch (type) {
                case SRGTXRXCP:
                case SRGRXCP:
                case SRGTXCP:
                    LOG.info("initSrgTpList: adding SRG-CP tp = {} ", tp.getTpId().getValue());
                    this.availableSrgCp.put(tp.getTpId().getValue(), tp1.getTpType());
                    break;
                case SRGRXPP:
                case SRGTXPP:
                case SRGTXRXPP:
                    boolean used = true;
                    LOG.info("initSrgTpList: SRG-PP tp = {} found", tp.getTpId().getValue());
                    try {
                        List<UsedWavelength> usedWavelengths = tp1.getPpAttributes().getUsedWavelength();
                        if (usedWavelengths.isEmpty()) {
                            used = false;
                        }
                    } catch (NullPointerException e) {
                        LOG.warn("initSrgTpList: 'usedWavelengths' for tp={} is null !", tp.getTpId().getValue());
                        used = false;
                    }
                    if (!used) {
                        LOG.info("initSrgTpList: adding SRG-PP tp '{}'", tp.getTpId().getValue());
                        this.availableSrgPp.put(tp.getTpId().getValue(), tp1.getTpType());
                    } else {
                        LOG.warn("initSrgTpList: SRG-PP tp = {} found is busy !!");
                    }
                    break;
                default:
                    break;
            }
        }
        if (this.availableSrgPp.isEmpty() && this.availableSrgCp.isEmpty()) {
            LOG.error("initSrgTpList: ROADM SRG TerminationPoint list is empty for node {}", this.toString());
            this.valid = false;
            return;
        }
        LOG.info("initSrgTpList: availableSrgPp size = {} && availableSrgCp size = {} in {}", this.availableSrgPp
                .size(), this.availableSrgCp.size(), this.toString());
        this.isUnidir();
        return;
    }

    public void initWLlist() {
        this.availableWLindex.clear();
        if (!isValid()) {
            return;
        }
        Node1 node1 = this.node.augmentation(Node1.class);
        switch (this.nodeType) {
            case SRG :
                List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev170929.srg.node.attributes
                    .AvailableWavelengths> srgAvailableWL =
                        node1.getSrgAttributes().getAvailableWavelengths();
                if (srgAvailableWL == null) {
                    this.valid = false;
                    LOG.error("initWLlist: SRG AvailableWavelengths is empty for node  {}", this.toString());
                    return;
                }
                for (org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev170929.srg.node.attributes
                        .AvailableWavelengths awl : srgAvailableWL) {
                    this.availableWLindex.add(awl.getIndex());
                    LOG.debug("initWLlist: SRG next = {} in {}", awl.getIndex(), this.toString());
                }
                break;
            case DEGREE :
                List<org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev170929.degree.node.attributes
                    .AvailableWavelengths> degAvailableWL = node1.getDegreeAttributes().getAvailableWavelengths();
                if (degAvailableWL == null) {
                    this.valid = false;
                    LOG.error("initWLlist: DEG AvailableWavelengths is empty for node  {}", this.toString());
                    return;
                }
                for (org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev170929.degree.node.attributes
                            .AvailableWavelengths awl : degAvailableWL) {
                    this.availableWLindex.add(awl.getIndex());
                    LOG.debug("initWLlist: DEGREE next = {} in {}", awl.getIndex(), this.toString());
                }
                break;
            case XPONDER :
                // HARD CODED 96
                for (long i = 1; i <= 96; i++) {
                    this.availableWLindex.add(i);
                }
                break;
            default:
                LOG.error("initWLlist: unsupported node type {} in node {}", this.nodeType, this.toString());
                break;
        }
        if (this.availableWLindex.size() == 0) {
            LOG.debug("initWLlist: There are no available wavelengths in node {}", this.toString());
            this.valid = false;
        }
        LOG.debug("initWLlist: availableWLindex size = {} in {}", this.availableWLindex.size(), this.toString());
        return;
    }

    public void initXndrTps() {
        LOG.info("initXndrTps for node : {}", this.nodeId);
        if (!isValid()) {
            return;
        }
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1 nodeTp =
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                        .rev150608.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
            .TerminationPoint> allTps = nodeTp.getTerminationPoint();
        if (allTps == null) {
            this.valid = false;
            LOG.error("initXndrTps: XPONDER TerminationPoint list is empty for node {}", this.toString());
            return;
        }
        this.valid = false;
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
                    .TerminationPoint tp : allTps) {
            TerminationPoint1 tp1 = tp.augmentation(TerminationPoint1.class);
            if (tp1.getTpType() == OpenroadmTpType.XPONDERNETWORK) {
                if (tp1.getXpdrNetworkAttributes().getWavelength() != null) {
                    this.usedXpndrNWTps.add(tp.getTpId().getValue());
                    LOG.debug("initXndrTps: XPONDER tp = {} is used", tp.getTpId().getValue());
                } else {
                    this.valid = true;
                }
                // find Client of this network TP
                String client = tp1.getXpdrNetworkAttributes().getTailEquipmentId();
                if ((client.equals("")) || (client == null)) {
                    LOG.error("initXndrTps: XPONDER {} NW TP doesn't have defined Client {}", this.toString(), tp
                            .getTpId().getValue());
                    this.valid = false;
                }
                this.clientPerNwTp.put(tp.getTpId().getValue(), client);
            }
        }
        if (!isValid()) {
            LOG.error("initXndrTps: XPONDER doesn't have available wavelengths for node  {}", this.toString());
            return;
        }
        this.isUnidir();
    }

    private void isUnidir() {
        LOG.info("isUnidir: getting port direction...");
        switch (nodeType) {
            case XPONDER:
                LOG.info("isUnidir: getting port direction on XPONDER-NETWORK port ...");
                Optional<String> tp = this.clientPerNwTp.keySet().stream().findFirst();
                Mapping mapping = this.portMapping.getMapping(node.getSupportingNode().get(0).getNodeRef().getValue(),
                    tp.get());
                if (mapping != null) {
                    if (mapping.getPortDirection().compareTo(Direction.Bidirectional) != 0) {
                        this.isUnidir = true;
                        LOG.warn("isUnidir: XPONDER NETWORK have UNIDIR port...");
                    }
                } else {
                    LOG.error("isUnidir: Cannot get mapping for tp '{}'", tp.get());
                }
                break;
            case SRG:
                LOG.info("isUnidir: getting port direction on ROADM SRG-PP port ...");
                List<OpenroadmTpType> resultTx = this.availableSrgPp.values().stream()
                        .filter(pp -> pp == OpenroadmTpType.SRGTXPP).collect(Collectors.toList());
                List<OpenroadmTpType> resultRx = this.availableSrgPp.values().stream()
                        .filter(pp -> pp == OpenroadmTpType.SRGRXPP).collect(Collectors.toList());
                if (!resultTx.isEmpty() && !resultRx.isEmpty()) {
                    this.isUnidir = true;
                    LOG.warn("isUnidir: SRG node have UNIDIR port...");
                }
                break;
            default:
                this.isUnidir = false;
                break;
        }
    }

    public String getRdmSrgClient(String tp, Boolean aend) {
        LOG.info("getRdmSrgClient: Getting PP client for tp '{}' on node : {}", tp, this.nodeId);
        OpenroadmTpType srgType = null;
        OpenroadmTpType cpType = this.availableSrgCp.get(tp);
        if (cpType == null) {
            LOG.error("getRdmSrgClient: tp {} not existed in SRG CPterminationPoint list");
            return null;
        }
        switch (cpType) {
            case SRGTXRXCP:
                LOG.info("getRdmSrgClient: Getting BI or UNI Directional PP port ...");
                if (this.isUnidir) {
                    LOG.info("getRdmSrgClient: Getting UNI PP Port");
                    if (aend) {
                        LOG.info("getRdmSrgClient: Getting UNI Rx PP port ...");
                        srgType = OpenroadmTpType.SRGRXPP;
                    } else {
                        LOG.info("getRdmSrgClient: Getting UNI Tx PP port ...");
                        srgType = OpenroadmTpType.SRGTXPP;
                    }
                } else {
                    LOG.info("getRdmSrgClient: Getting BI PP Port");
                    srgType = OpenroadmTpType.SRGTXRXPP;
                }
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
            client = this.availableSrgPp.entrySet().stream().filter(pp -> pp.getValue() == openType)
                    .map(Map.Entry::getKey)
                    .sorted(new SortPortsByName())
                    .findFirst();
            if (!client.isPresent()) {
                LOG.error("getRdmSrgClient: ROADM {} doesn't have PP Client for CP {}", this.toString(), tp);
                return null;
            }
            LOG.info("getRdmSrgClient: client PP {} for CP {} found !", client, tp);
            return client.get();
        } else {
            LOG.error("getRdmSrgClient: SRG TerminationPoint PP list is not available for node {}", this.toString());
            return null;
        }
    }

    public boolean checkTP(String tp) {
        if (this.usedXpndrNWTps.contains(tp)) {
            return false;
        }
        return true;
    }

    public boolean checkWL(long index) {
        if (this.availableWLindex.contains(index)) {
            return true;
        }
        return false;
    }

    public boolean isValid() {
        return this.valid;
    }

    public List<Long> getAvailableWLs() {
        return this.availableWLindex;
    }

    public void addOutgoingLink(PceLink outLink) {
        this.outgoingLinks.add(outLink);
    }

    public List<PceLink> getOutgoingLinks() {
        return this.outgoingLinks;
    }

    public String getXpdrClient(String tp) {
        return this.clientPerNwTp.get(tp);
    }

    public OpenroadmNodeType getPceNodeType() {
        return this.nodeType;
    }

    public boolean getUnidir() {
        return this.isUnidir;
    }

    @Override
    public String toString() {
        return "PceNode type=" + this.nodeType + " ID=" + this.nodeId.getValue();
    }
}