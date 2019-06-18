/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
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
    private final String supNodeId;
    private final String clli;

    // wavelength calculation per node type
    private List<Long> availableWLindex = new ArrayList<Long>();
    private Map<String, OpenroadmTpType> availableSrgPp = new TreeMap<String, OpenroadmTpType>();
    private Map<String, OpenroadmTpType> availableSrgCp = new TreeMap<String, OpenroadmTpType>();
    private List<String> usedXpndrNWTps = new ArrayList<String>();
    private List<PceLink> outgoingLinks = new ArrayList<PceLink>();
    private Map<String, String> clientPerNwTp = new HashMap<String, String>();

    public PceNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId) {
        this.node = node;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.supNodeId = getSupNodeId(node);
        this.clli = MapUtils.getCLLI(node);

        if ((node == null) || (nodeId == null) || (nodeType == null)) {
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
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                                .rev180226.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint> allTps =
                nodeTp.getTerminationPoint();
        if (allTps == null) {
            LOG.error("initSrgTpList: ROADM TerminationPoint list is empty for node {}", this.toString());
            this.valid = false;
            return;
        }
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint tp : allTps) {
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
        return;
    }



/*    public PceNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId,
            String supNodeId, String clli) {
        this.node = node;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.supNodeId = supNodeId;
        this.clli = clli;

        if ((node == null) || (nodeId == null) || (nodeType == null)
                || (supNodeId == null) || (clli == null)) {
            LOG.error(
                    "PceNode: one of parameters is not populated : nodeId, node type, supporting nodeId");
            valid = false;
        }

        LOG.debug(" PceNode built :{}", this.toString());
    }
*/
    public void initWLlist() {
        this.availableWLindex.clear();
        if (!isValid()) {
            return;
        }
        Node1 node1 = this.node.augmentation(Node1.class);
        switch (this.nodeType) {
            case SRG :
                List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                    .AvailableWavelengths> srgAvailableWL =
                        node1.getSrgAttributes().getAvailableWavelengths();
                if (srgAvailableWL == null) {
                    this.valid = false;
                    LOG.error("initWLlist: SRG AvailableWavelengths is empty for node  {}", this.toString());
                    return;
                }
                for (org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                        .AvailableWavelengths awl : srgAvailableWL) {
                    this.availableWLindex.add(awl.getIndex());
                    LOG.debug("initWLlist: SRG next = {} in {}", awl.getIndex(), this.toString());
                }
                break;
            case DEGREE :
                List<org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes
                    .AvailableWavelengths> degAvailableWL = node1.getDegreeAttributes().getAvailableWavelengths();
                if (degAvailableWL == null) {
                    this.valid = false;
                    LOG.error("initWLlist: DEG AvailableWavelengths is empty for node  {}", this.toString());
                    return;
                }
                for (org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes
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
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeTp =
                this.node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                        .rev180226.Node1.class);
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint> allTps = nodeTp.getTerminationPoint();
        if (allTps == null) {
            this.valid = false;
            LOG.error("initXndrTps: XPONDER TerminationPoint list is empty for node {}", this.toString());
            return;
        }
        this.valid = false;
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .node.TerminationPoint tp : allTps) {
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
            client = this.availableSrgPp.entrySet().stream().filter(pp -> pp.getValue().getName() == openType.getName())
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

    private String getSupNodeId(Node inputNode) {
        String tempSupId = "";
        // TODO: supporting IDs exist as a List. this code takes just the
        // first element
        tempSupId = MapUtils.getSupNode(inputNode);
        if (tempSupId.equals("")) {
            LOG.error("getSupNodeId: Empty Supporting node for node: [{}]. Node is ignored", inputNode.getNodeId());
        }
        return tempSupId;
    }

    public void validateAZxponder(String anodeId, String znodeId) {
        if (!isValid()) {
            return;
        }

        if (this.nodeType != OpenroadmNodeType.XPONDER) {
            return;
        }

        // Detect A and Z
        if (this.supNodeId.equals(anodeId) || (this.supNodeId.equals(znodeId))) {

            LOG.info("validateAZxponder: A or Z node detected == {}", nodeId.getValue());
            initXndrTps();
            return;
        }

        LOG.debug("validateAZxponder: XPONDER is ignored == {}", nodeId.getValue());
        valid = false;
    }

    public String getXpdrClient(String tp) {
        return this.clientPerNwTp.get(tp);
    }

    public boolean checkTP(String tp) {
        return !(this.usedXpndrNWTps.contains(tp));
    }

    public boolean checkWL(long index) {
        return (this.availableWLindex.contains(index));
    }

    public boolean isValid() {
        if ((node == null) || (nodeId == null) || (nodeType == null) || (supNodeId == null) || (clli == null)) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type, supporting nodeId");
            valid = false;
        }
        return valid;
    }

    public List<Long> getAvailableWLs() {
        return availableWLindex;
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

    public NodeId getNodeId() {
        return nodeId;
    }

    public String getSupNodeIdPceNode() {
        return supNodeId;
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

}
