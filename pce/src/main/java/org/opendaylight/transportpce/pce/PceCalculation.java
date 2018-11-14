/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceCalculation {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);
    private DataBroker dataBroker = null;
    ///////////// data parsed from Input/////////////////
    private PathComputationRequestInput input;
    private String anodeId = "";
    private String znodeId = "";
    private PceConstraints pceHardConstraints;
    private PceConstraints pceSoftConstraints;
    ///////////// Intermediate data/////////////////
    private List<PceLink> addLinks = new ArrayList<PceLink>();
    private List<PceLink> dropLinks = new ArrayList<PceLink>();
    private HashSet<NodeId> azSrgs = new HashSet<NodeId>();
    private PceNode aendPceNode = null;
    private PceNode zendPceNode = null;
    private List<Link> allLinks = null;
    private List<Node> allNodes = null;
    // this List serves graph calculation
    private Map<NodeId, PceNode> allPceNodes = new HashMap<NodeId, PceNode>();
    // this List serves calculation of ZtoA path descritopn
    // TODO maybe better solution is possible
    private Map<LinkId, PceLink> allPceLinks = new HashMap<LinkId, PceLink>();
    private PceResult returnStructure;

    public PceResult getReturnStructure() {
        return this.returnStructure;
    }

    public enum NodeConstraint {
        NONE, HARD_EXCLUDE, HARD_INCLUDE, HARD_DIVERSITY, SOFT_EXCLUDE, SOFT_INCLUDE, SOFT_DIVERSITY;
    }

    // private static final String NETWORK_ID = "Transport Overlay";
    public PceCalculation(PathComputationRequestInput input, DataBroker dataBroker, PceConstraints pceHardConstraints,
            PceConstraints pceSoftConstraints, PceResult rc) {
        this.input = input;
        this.dataBroker = dataBroker;
        this.returnStructure = rc;
        this.pceHardConstraints = pceHardConstraints;
        this.pceSoftConstraints = pceSoftConstraints;
        parseInput();
    }

    // apply constraints to get applicable result
    public void calcPath() {
        LOG.info("In PceCalculation calcPath: ");
        if (!readMdSal()) {
            this.returnStructure.setRC(ResponseCodes.RESPONSE_FAILED);
            return;
        }
        if (!analyzeNw()) {
            this.returnStructure.setRC(ResponseCodes.RESPONSE_FAILED);
            return;
        }
        this.returnStructure.setRC(ResponseCodes.RESPONSE_OK);
        return;
    }

    private boolean parseInput() {
        this.anodeId = this.input.getServiceAEnd().getNodeId();
        this.znodeId = this.input.getServiceZEnd().getNodeId();
        LOG.info("parseInput: A and Z :[{}] and [{}]", this.anodeId, this.znodeId);
        this.returnStructure.setRate(this.input.getServiceAEnd().getServiceRate());
        return true;
    }

    private boolean readMdSal() {
        LOG.info("readMdSal: network {}", NetworkUtils.OVERLAY_NETWORK_ID);
        InstanceIdentifier<Network> nwInstanceIdentifier = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();
        ReadOnlyTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction();
        Network nw = null;
        try {
            Optional<Network> nwOptional =
                    readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier).get();
            if (nwOptional.isPresent()) {
                nw = nwOptional.get();
                LOG.debug("readMdSal: network nodes: nwOptional.isPresent = true {}", nw.toString());
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("readMdSal: Error reading topology {}", nwInstanceIdentifier);
            readOnlyTransaction.close();
            this.returnStructure.setRC(ResponseCodes.RESPONSE_FAILED);
            throw new RuntimeException(
                    "readMdSal: Error reading from operational store, topology : " + nwInstanceIdentifier + " :" + e);
        }
        readOnlyTransaction.close();
        if (nw == null) {
            LOG.error("readMdSal: network is null: {}", nwInstanceIdentifier);
            return false;
        }
        this.allNodes = nw.getNode();
        Network1 nw1 = nw.augmentation(Network1.class);
        this.allLinks = nw1.getLink();
        if ((this.allNodes == null) || this.allNodes.isEmpty()) {
            LOG.error("readMdSal: no nodes ");
            return false;
        }
        LOG.info("readMdSal: network nodes: {} nodes added", this.allNodes.size());
        if ((this.allLinks == null) || this.allLinks.isEmpty()) {
            LOG.error("readMdSal: no links ");
            return false;
        }
        LOG.info("readMdSal: network links: {} links added", this.allLinks.size());
        return true;
    }

    private boolean analyzeNw() {
        LOG.debug("analyzeNw: allNodes size {}, allLinks size {}", this.allNodes.size(), this.allLinks.size());
        for (Node node : this.allNodes) {
            validateNode(node);
        }
        LOG.info("analyzeNw: allPceNodes size {} : {}", this.allPceNodes.size(), this.allPceNodes.toString());
        if ((this.aendPceNode == null) || (this.zendPceNode == null)) {
            LOG.error("analyzeNw: Error in reading nodes: A or Z do not present in the network");
            return false;
        }
        for (Link link : this.allLinks) {
            validateLink(link);
        }
        LOG.debug("analyzeNw: AddLinks size {}, DropLinks size {}", this.addLinks.size(), this.dropLinks.size());
        // debug prints
        LOG.info("analyzeNw: AZSrgs size = {}", this.azSrgs.size());
        for (NodeId srg : this.azSrgs) {
            LOG.info("analyzeNw: A/Z Srgs SRG = {}", srg.getValue());
        }
        // debug prints
        for (PceLink link : this.addLinks) {
            filterAddLinks(link);
        }
        for (PceLink link : this.dropLinks) {
            filterDropLinks(link);
        }
        LOG.info("analyzeNw: allPceNodes size {}, allPceLinks size {}", this.allPceNodes.size(), this.allPceLinks
                .size());
        return true;
    }

    private boolean filterAddLinks(PceLink pcelink) {
        NodeId nodeId = pcelink.getSourceId();
        if (this.azSrgs.contains(nodeId)) {
            this.allPceLinks.put(pcelink.getLinkId(), pcelink);
            this.allPceNodes.get(nodeId).addOutgoingLink(pcelink);
            LOG.info("analyzeNw: Add_LINK added to source and to allPceLinks {}", pcelink.getLinkId().toString());
            return true;
        }
        // remove the SRG from PceNodes, as it is not directly connected to A/Z
        this.allPceNodes.remove(nodeId);
        LOG.info("analyzeNw: SRG removed {}", nodeId.getValue());
        return false;
    }

    private boolean filterDropLinks(PceLink pcelink) {
        NodeId nodeId = pcelink.getDestId();
        if (this.azSrgs.contains(nodeId)) {
            this.allPceLinks.put(pcelink.getLinkId(), pcelink);
            this.allPceNodes.get(nodeId).addOutgoingLink(pcelink);
            LOG.info("analyzeNw: Drop_LINK added to dest and to allPceLinks {}", pcelink.getLinkId().toString());
            return true;
        }
        // remove the SRG from PceNodes, as it is not directly connected to A/Z
        this.allPceNodes.remove(pcelink.getDestId());
        LOG.info("analyzeNw: SRG removed {}", nodeId.getValue());
        return false;
    }

    private boolean validateLink(Link link) {
        LOG.info("validateLink: link {} ", link.toString());
        NodeId sourceId = link.getSource().getSourceNode();
        NodeId destId = link.getDestination().getDestNode();
        PceNode source = this.allPceNodes.get(sourceId);
        PceNode dest = this.allPceNodes.get(destId);
        if (source == null) {
            LOG.warn("validateLink: source node is rejected by node validation - {}", link.getSource().getSourceNode()
                    .getValue());
            return false;
        }
        if (dest == null) {
            LOG.warn("validateLink: dest node is rejected by node validation - {}", link.getDestination().getDestNode()
                    .getValue());
            return false;
        }
        PceLink pcelink = new PceLink(link);
        if (!pcelink.isValid()) {
            LOG.error(" validateLink: Link is ignored due errors in network data ");
            return false;
        }
        LinkId linkId = pcelink.getLinkId();
        switch (pcelink.getLinkType()) {
            case ROADMTOROADM :
                this.allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.info("validateLink: ROADMTOROADM-LINK added to allPceLinks {}", pcelink.toString());
                break;
            case EXPRESSLINK :
                this.allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.info("validateLink: EXPRESS-LINK added to allPceLinks {}", pcelink.toString());
                break;
            case ADDLINK :
                pcelink.setClient(source.getRdmSrgClient(pcelink.getSourceTP().toString()));
                this.addLinks.add(pcelink);
                LOG.debug("validateLink: ADD-LINK saved  {}", pcelink.toString());
                break;
            case DROPLINK :
                pcelink.setClient(dest.getRdmSrgClient(pcelink.getDestTP().toString()));
                this.dropLinks.add(pcelink);
                LOG.info("validateLink: DROP-LINK saved  {}", pcelink.toString());
                break;
            case XPONDERINPUT :
                this.azSrgs.add(sourceId);
                // store separately all SRG links directly connected to A/Z
                if (!dest.checkTP(pcelink.getDestTP().toString())) {
                    LOG.debug("validateLink: XPONDER-INPUT is rejected as NW port is busy - {} ", pcelink.toString());
                    return false;
                }
                pcelink.setClient(dest.getXpdrClient(pcelink.getDestTP().toString()));
                this.allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.info("validateLink: XPONDER-INPUT link added to allPceLinks {}", pcelink.toString());
                break;
            case XPONDEROUTPUT :
                // does it mean XPONDER==>>SRG ?
                this.azSrgs.add(destId);
                // store separately all SRG links directly connected to A/Z
                if (!source.checkTP(pcelink.getSourceTP().toString())) {
                    LOG.debug("validateLink: XPONDER-OUTPUT is rejected as NW port is busy - {} ", pcelink.toString());
                    return false;
                }
                pcelink.setClient(source.getXpdrClient(pcelink.getSourceTP().toString()));
                this.allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.info("validateLink: XPONDER-OUTPUT link added to allPceLinks {}", pcelink.toString());
                break;
            default:
                LOG.warn("validateLink: link type is not supported {}", pcelink.toString());
        }
        return true;
    }

    private boolean validateNode(Node node) {
        String supNodeId = "";
        OpenroadmNodeType nodeType = null;
        NodeId nodeId = null;
        if (node == null) {
            LOG.error("validateNode: node is null, ignored ");
            return false;
        }
        try {
            // TODO: supporting IDs exist as a List. this code takes just the first element
            nodeId = node.getNodeId();
            supNodeId = node.getSupportingNode().get(0).getNodeRef().getValue();
            if (supNodeId.equals("")) {
                LOG.error("validateNode: Supporting node for node: [{}]. Node is ignored", nodeId.getValue());
                return false;
            }
            // extract node type
            Node1 node1 = node.augmentation(Node1.class);
            if (node1 == null) {
                LOG.error("validateNode: no Node1 (type) Augmentation for node: [{}]. Node is ignored", nodeId
                        .getValue());
                return false;
            }
            nodeType = node1.getNodeType();
            /** Catch exception 'RuntimeException' is not allowed. [IllegalCatch]. */
        } catch (NullPointerException e) {
            LOG.error("validateNode: Error reading supporting node or node type for node '{}'", nodeId, e);
            return false;
        }
        if (nodeType == OpenroadmNodeType.XPONDER) {
            // Detect A and Z
            if (supNodeId.equals(this.anodeId) || (supNodeId.equals(this.znodeId))) {
                LOG.info("validateNode: A or Z node detected == {}", node.getNodeId().getValue());
            } else {
                LOG.warn("validateNode: XPONDER is ignored == {}", node.getNodeId().getValue());
                return false;
            }
        }
        switch (validateNodeConstraints(nodeId.getValue(), supNodeId)) {
            case HARD_EXCLUDE :
                LOG.info("validateNode: constraints : node is ignored == {}", nodeId.getValue());
                return false;
            default:
                break;
        }
        PceNode pceNode = new PceNode(node, nodeType, nodeId);
        if (!pceNode.isValid()) {
            LOG.error(" validateNode: Node is ignored due errors in network data ");
            return false;
        }
        if (supNodeId.equals(this.anodeId)) {
            if (endPceNode(nodeType, nodeId, pceNode)) {
                if (!pceNode.isValid()) {
                    LOG.error("validateNode: There are no available wavelengths in node {}", nodeId.getValue());
                    return false;
                }
                this.aendPceNode = pceNode;
            }
        }
        if (supNodeId.equals(this.znodeId)) {
            if (endPceNode(nodeType, nodeId, pceNode)) {
                if (!pceNode.isValid()) {
                    LOG.error("validateNode: There are no available wavelengths in node {}", nodeId.getValue());
                    return false;
                }
                this.zendPceNode = pceNode;
            }
        }
        pceNode.initWLlist();
        if (!pceNode.isValid()) {
            LOG.error("validateNode: There are no available wavelengths in node {}", nodeId.getValue());
            return false;
        }
        this.allPceNodes.put(nodeId, pceNode);
        LOG.info("validateNode: node is saved {}", nodeId.getValue());
        return true;
    }

    private Boolean endPceNode(OpenroadmNodeType openroadmNodeType, NodeId nodeId, PceNode pceNode) {
        Boolean add = true;
        switch (openroadmNodeType) {
            case SRG :
                pceNode.initRdmSrgTps();
                this.azSrgs.add(nodeId);
                break;
            case XPONDER :
                pceNode.initXndrTps();
                break;
            default:
                add = false;
                LOG.warn("endPceNode: Node {} is not SRG or XPONDER !", nodeId);
                break;
        }
        return add;
    }

    private NodeConstraint validateNodeConstraints(String nodeId, String supNodeId) {
        if (this.pceHardConstraints.getExcludeNodes().contains(nodeId)) {
            return NodeConstraint.HARD_EXCLUDE;
        }
        if (this.pceHardConstraints.getExcludeNodes().contains(supNodeId)) {
            return NodeConstraint.HARD_EXCLUDE;
        }
        if (this.pceHardConstraints.getIncludeNodes().contains(nodeId)) {
            return NodeConstraint.HARD_INCLUDE;
        }
        if (this.pceHardConstraints.getIncludeNodes().contains(supNodeId)) {
            return NodeConstraint.HARD_INCLUDE;
        }
        return NodeConstraint.NONE;
    }

    public PceNode getaPceNode() {
        return this.aendPceNode;
    }

    public PceNode getzPceNode() {
        return this.zendPceNode;
    }

    public Map<NodeId, PceNode> getAllPceNodes() {
        return this.allPceNodes;
    }

    public Map<LinkId, PceLink> getAllPceLinks() {
        return this.allPceLinks;
    }
}
