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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
    private Set<String> availableSrgPp = new TreeSet<String>();
    private List<String> usedXpndrNWTps = new ArrayList<String>();
    private List<String> usedRdmCpTps = new ArrayList<String>();
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

    public void initSrgPpList() {
        this.availableSrgPp.clear();
        if (!isValid()) {
            return;
        }
        LOG.info("getting SRG-PP tps from ROADM node {}", this.nodeId);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1 nodeTp =
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                                .rev150608.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
            .TerminationPoint> allTps = nodeTp.getTerminationPoint();
        if (allTps == null) {
            LOG.error("initXndrTps: ROADM TerminationPoint list is empty for node {}", this.toString());
            this.valid = false;
            return;
        }
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
                .TerminationPoint tp : allTps) {
            TerminationPoint1 tp1 = tp.augmentation(TerminationPoint1.class);
            if (tp1.getTpType() == OpenroadmTpType.SRGTXRXPP) {
                this.availableSrgPp.add(tp.getTpId().getValue());
            }
        }
        if (this.availableSrgPp.isEmpty()) {
            LOG.error("initSrgPpList: ROADM SRG PP TerminationPoint list is empty for node {}", this.toString());
            this.valid = false;
            return;
        }
        LOG.info("initSrgPpList: availableSrgPp size = {} in {}", this.availableSrgPp.size(), this.toString());
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
                    .AvailableWavelengths> srgAvailableWL = node1.getSrgAttributes().getAvailableWavelengths();
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
                    .AvailableWavelengths> degAvailableWL =
                        node1.getDegreeAttributes().getAvailableWavelengths();
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

    public void initRdmSrgTps() {
        LOG.info("initRdmSrgTps for node : {}", this.nodeId);
        initSrgPpList();
        if (!isValid()) {
            return;
        }
        LOG.info("initRdmSrgTps: getting terminationPoint list for node {}", this.toString());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1 nodeTp =
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                        .rev150608.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
            .TerminationPoint> allTps = nodeTp.getTerminationPoint();
        if (allTps == null) {
            this.valid = false;
            LOG.error("initRdmSrgTps: SRG TerminationPoint list is empty for node {}", this.toString());
            return;
        }
        this.valid = false;
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
                .TerminationPoint tp : allTps) {
            TerminationPoint1 tp1 = tp.augmentation(TerminationPoint1.class);
            if (tp1.getTpType() == OpenroadmTpType.SRGTXRXCP) {
                try {
                    List<UsedWavelengths> usedWavelengths = tp1.getCpAttributes().getUsedWavelengths();
                    if (!usedWavelengths.isEmpty()) {
                        this.usedRdmCpTps.add(tp.getTpId().getValue());
                        LOG.warn("initRdmSrgTps: SRG-CP tp = {} is used", tp.getTpId().getValue());
                    } else {
                        LOG.info("initRdmSrgTps: SRG-CP tp = {} found", tp.getTpId().getValue());
                        this.valid = true;
                    }
                } catch (NullPointerException e) {
                    LOG.warn("'usedWavelengths' for tp={} is null !", tp.getTpId().getValue());
                    this.valid = true;
                }
            }
            if (tp1.getTpType() == OpenroadmTpType.SRGTXRXCP) {
                // Find an available PP of this CP
                if (!this.availableSrgPp.isEmpty()) {
                    LOG.info("finding PP for CP {}", tp.getTpId().getValue());
                    Iterator<String> iterator = this.availableSrgPp.iterator();
                    while (iterator.hasNext()) {
                        String client = iterator.next();
                        if ((client.equals("")) || (client == null)) {
                            LOG.error("initRdmSrgTps: ROADM {} doesn't have defined Client {}", this.toString(), tp
                                    .getTpId().getValue());
                            this.valid = false;
                        } else {
                            this.valid = true;
                            this.clientPerPpTp.put(tp.getTpId().getValue(), client);
                            LOG.info("initRdmSrgTps: client PP {} for CP {} found !", client, tp.getTpId().getValue());
                            iterator.remove();
                            break;
                        }
                    }
                } else {
                    LOG.error("initRdmSrgTps: ROADM {} doesn't have available PP", this.nodeId.getValue());
                    this.valid = false;
                    return;
                }
            }
        }
        if (!isValid()) {
            LOG.error("initRdmSrgTps: SRG doesn't have available wavelengths for node  {}", this.toString());
            return;
        }
    }

    public void initNodeTps() {
        if (!isValid()) {
            return;
        }
        switch (this.nodeType) {
            case SRG :
                initSrgPpList();
                initRdmSrgTps();
                break;
            case XPONDER :
                initXndrTps();
                break;
            default:
                this.valid = true;
                LOG.warn("initNodeTps: Node TerminationPoints list is not SRG or XPONDER or DEGREE!");
                break;
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
        LOG.info("Getting ROADM Client PP for CP {}", tp, this.clientPerPpTp.get(tp));
        return this.clientPerPpTp.get(tp);
    }

    @Override
    public String toString() {
        return "PceNode type=" + this.nodeType + " ID=" + this.nodeId.getValue();
    }
}
