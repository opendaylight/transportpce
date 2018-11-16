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

import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.node.termination.point.cp.attributes.UsedWavelengths;
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
    // private Set<String> availableSrgPp = new TreeSet<String>();
    private Map<String, OpenroadmTpType> availableSrgPp = new TreeMap<String, OpenroadmTpType>();
    private Map<String, OpenroadmTpType> availableSrgCp = new TreeMap<String, OpenroadmTpType>();
    private List<String> usedXpndrNWTps = new ArrayList<String>();
    private List<String> usedSrgPP = new ArrayList<String>();
    private List<PceLink> outgoingLinks = new ArrayList<PceLink>();
    private Map<String, String> clientPerNwTp = new HashMap<String, String>();
    private Map<String, String> clientPerPpTp = new HashMap<String, String>();

    public PceNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId) {
        this.node = node;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        if ((node == null) || (nodeId == null) || (nodeType == null)) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type");
            this.valid = false;
        }
    }

    public void initSrgTpList() {
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
        boolean used;
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
                .TerminationPoint tp : allTps) {
            used = true;
            TerminationPoint1 tp1 = tp.augmentation(TerminationPoint1.class);
            try {
                List<UsedWavelengths> usedWavelengths = tp1.getCpAttributes().getUsedWavelengths();
                if (usedWavelengths.isEmpty()) {
                    LOG.info("initSrgTpList: SRG-CP tp = {} found", tp.getTpId().getValue());
                    used = false;
                }
            } catch (NullPointerException e) {
                LOG.warn("initSrgTpList: 'usedWavelengths' for tp={} is null !", tp.getTpId().getValue());
                used = false;
            }
            if (!used) {
                if (tp1.getTpType().getName().contains("-PP")) {
                    LOG.info("initSrgTpList: adding tp '{}'", tp1.getTpType());
                    this.availableSrgPp.put(tp.getTpId().getValue(), tp1.getTpType());
                } else if (tp1.getTpType().getName().contains("-CP")) {
                    this.availableSrgCp.put(tp.getTpId().getValue(), tp1.getTpType());
                }
            }
        }
        if (this.availableSrgPp.isEmpty() && this.availableSrgCp.isEmpty()) {
            LOG.error("initSrgTpList: ROADM SRG TerminationPoint list is empty for node {}", this.toString());
            this.valid = false;
            return;
        }
        LOG.info("initSrgTpList: availableSrgPp size = {} && availableSrgCp size = {} in {}", this.availableSrgPp
                .size(), this.availableSrgCp.size(), this.toString());
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
    }

    public void initRdmSrgTps(Boolean aend) {
        LOG.info("initRdmSrgTps for node : {}", this.nodeId);
        initSrgTpList();
        if (!isValid()) {
            return;
        }
        this.valid = false;
        Optional<String> optTp = null;
        OpenroadmTpType srgType = OpenroadmTpType.SRGTXRXPP;
        OpenroadmTpType oppositeSrgType = null;
        Optional<String> oppositeTp = null;
        boolean unidir = false;
        optTp = this.availableSrgCp.entrySet().stream().filter(cp -> cp.getValue() == OpenroadmTpType.SRGTXRXCP)
                .map(Map.Entry::getKey).findFirst();
        if (!optTp.isPresent()) {
            srgType = null;
            unidir = true;
            LOG.info("UNI Directional ports ...");
            if (aend) {
                LOG.info("Tx port ...");
                optTp = this.availableSrgCp.entrySet().stream().filter(cp -> cp.getValue() == OpenroadmTpType.SRGTXCP)
                        .map(Map.Entry::getKey).findFirst();
                srgType = OpenroadmTpType.SRGRXPP;
                oppositeSrgType = OpenroadmTpType.SRGTXPP;
                oppositeTp = this.availableSrgCp.entrySet().stream()
                        .filter(cp -> cp.getValue() == OpenroadmTpType.SRGRXCP).map(Map.Entry::getKey).findFirst();
            } else {
                LOG.info("Rx port ...");
                optTp = this.availableSrgCp.entrySet().stream().filter(cp -> cp.getValue() == OpenroadmTpType.SRGRXCP)
                        .map(Map.Entry::getKey).findFirst();
                srgType = OpenroadmTpType.SRGTXPP;
                oppositeSrgType = OpenroadmTpType.SRGRXPP;
                oppositeTp = this.availableSrgCp.entrySet().stream()
                        .filter(cp -> cp.getValue() == OpenroadmTpType.SRGTXCP).map(Map.Entry::getKey).findFirst();
            }
        } else {
            LOG.info("BI Directional ports ...");
        }
        if (optTp.isPresent() && (srgType != null)) {
            String tp = optTp.get();
            if (!this.availableSrgPp.isEmpty()) {
                LOG.info("finding PP for CP {}", optTp.get());
                Optional<String> client = null;
                final OpenroadmTpType openType = srgType;
                client = this.availableSrgPp.entrySet().stream().filter(pp -> pp.getValue() == openType)
                        .map(Map.Entry::getKey).findFirst();
                if (!client.isPresent()) {
                    LOG.error("initRdmSrgTps: ROADM {} doesn't have defined Client {}", this.toString(), tp);
                    this.valid = false;
                    return;
                }
                if (unidir) {
                    final OpenroadmTpType oppositeOpType = oppositeSrgType;
                    String opTp = oppositeTp.get();
                    Optional<String> oppositeClient = this.availableSrgPp.entrySet().stream()
                            .filter(pp -> pp.getValue() == oppositeOpType)
                            .map(Map.Entry::getKey).findFirst();
                    if (!oppositeClient.isPresent()) {
                        LOG.error("initRdmSrgTps: ROADM {} doesn't have defined opposite Client {}",
                                this.toString(), tp);
                        this.valid = false;
                        return;
                    }
                    this.clientPerPpTp.put(opTp, oppositeClient.get());
                    LOG.info("initRdmSrgTps: client PP {} for oposite CP {} found !", client, tp);
                }
                this.valid = true;
                this.clientPerPpTp.put(tp, client.get());
                LOG.info("initRdmSrgTps: client PP {} for CP {} found !", client, tp);
            }
        }
        if (!isValid()) {
            this.valid = false;
            LOG.error("initRdmSrgTps: SRG TerminationPoint list is empty for node {}", this.toString());
            return;
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

    public String getRdmSrgClient(String tp) {
        LOG.info("Getting ROADM Client PP for CP {} : {}", tp, this.clientPerPpTp.get(tp));
        return this.clientPerPpTp.get(tp);
    }

    @Override
    public String toString() {
        return "PceNode type=" + this.nodeType + " ID=" + this.nodeId.getValue();
    }
}
