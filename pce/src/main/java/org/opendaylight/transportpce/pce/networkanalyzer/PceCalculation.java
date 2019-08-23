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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceCalculation {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);
    private NetworkTransactionService networkTransactionService = null;

    ///////////// data parsed from Input/////////////////
    private PathComputationRequestInput input;
    private String anodeId = "";
    private String znodeId = "";

    private PceConstraints pceHardConstraints;

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
    // this List serves calculation of ZtoA path description
    // TODO maybe better solution is possible
    private Map<LinkId, PceLink> allPceLinks = new HashMap<LinkId, PceLink>();
    private Set<LinkId> linksToExclude = new HashSet<LinkId>();
    private PceResult returnStructure;

    private enum ConstraintTypes {
        NONE, HARD_EXCLUDE, HARD_INCLUDE, HARD_DIVERSITY, SOFT_EXCLUDE, SOFT_INCLUDE, SOFT_DIVERSITY;
    }

    public PceCalculation(PathComputationRequestInput input, NetworkTransactionService networkTransactionService,
            PceConstraints pceHardConstraints, PceConstraints pceSoftConstraints, PceResult rc) {
        this.input = input;
        this.networkTransactionService = networkTransactionService;
        this.returnStructure = rc;

        this.pceHardConstraints = pceHardConstraints;
        parseInput();
    }

    public void calcPath() {

        LOG.info("In PceCalculation calcPath: ");

        if (!readMdSal()) {
            returnStructure.setRC(ResponseCodes.RESPONSE_FAILED);
            return;
        }

        MapUtils.mapDiversityConstraints(allNodes, allLinks, pceHardConstraints);

        if (!analyzeNw()) {
            returnStructure.setRC(ResponseCodes.RESPONSE_FAILED);
            return;
        }
        printNodesInfo(allPceNodes);

        returnStructure.setRC(ResponseCodes.RESPONSE_OK);
        return;
    }

    private boolean parseInput() {
        anodeId = input.getServiceAEnd().getNodeId();
        znodeId = input.getServiceZEnd().getNodeId();
        LOG.info("parseInput: A and Z :[{}] and [{}]", anodeId, znodeId);
        returnStructure.setRate(input.getServiceAEnd().getServiceRate());
        return true;
    }

    private boolean readMdSal() {
        LOG.info("readMdSal: network {}", NetworkUtils.OVERLAY_NETWORK_ID);
        InstanceIdentifier<Network> nwInstanceIdentifier = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();
        Network nw = null;
        try {
            Optional<Network> nwOptional =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier).get();
            if (nwOptional.isPresent()) {
                nw = nwOptional.get();
                LOG.debug("readMdSal: network nodes: nwOptional.isPresent = true {}", nw.toString());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("readMdSal: Error reading topology {}", nwInstanceIdentifier);
            networkTransactionService.close();
            returnStructure.setRC(ResponseCodes.RESPONSE_FAILED);
            throw new RuntimeException(
                    "readMdSal: Error reading from operational store, topology : " + nwInstanceIdentifier + " :" + e);
        }
        networkTransactionService.close();

        if (nw == null) {
            LOG.error("readMdSal: network is null: {}", nwInstanceIdentifier);
            return false;
        }
        allNodes = nw.getNode().stream().sorted((n1, n2) -> n1.getNodeId().getValue().compareTo(n2.getNodeId()
            .getValue())).collect(Collectors.toList());
        Network1 nw1 = nw.augmentation(Network1.class);

        allLinks = nw1.getLink().stream().sorted((l1, l2) -> l1.getSource().getSourceTp().toString().compareTo(l2
            .getSource().getSourceTp().toString())).collect(Collectors.toList());
        if (allNodes == null || allNodes.isEmpty()) {
            LOG.error("readMdSal: no nodes ");
            return false;
        }
        LOG.info("readMdSal: network nodes: {} nodes added", allNodes.size());
        LOG.debug("readMdSal: network nodes: {} nodes added", allNodes.toString());

        if (allLinks == null || allLinks.isEmpty()) {
            LOG.error("readMdSal: no links ");
            return false;
        }
        LOG.info("readMdSal: network links: {} links added", allLinks.size());
        LOG.debug("readMdSal: network links: {} links added", allLinks.toString());

        return true;
    }

    private boolean analyzeNw() {

        LOG.debug("analyzeNw: allNodes size {}, allLinks size {}", allNodes.size(), allLinks.size());

        for (Node node : allNodes) {
            validateNode(node);
        }
        LOG.debug("analyzeNw: allPceNodes size {}", allPceNodes.size());

        if (aendPceNode == null || zendPceNode == null) {
            LOG.error("analyzeNw: Error in reading nodes: A or Z do not present in the network");
            return false;
        }

        for (Link link : allLinks) {
            validateLink(link);
        }

        LOG.debug("analyzeNw: addLinks size {}, dropLinks size {}", addLinks.size(), dropLinks.size());

        // debug prints
        LOG.debug("analyzeNw: azSrgs size = {}", azSrgs.size());
        for (NodeId srg : azSrgs) {
            LOG.debug("analyzeNw: A/Z Srgs SRG = {}", srg.getValue());
        }
        // debug prints

        for (PceLink link : addLinks) {
            filteraddLinks(link);
        }
        for (PceLink link : dropLinks) {
            filterdropLinks(link);
        }

        LOG.info("analyzeNw: allPceNodes size {}, allPceLinks size {}", allPceNodes.size(), allPceLinks.size());

        if ((allPceNodes.size() == 0) || (allPceLinks.size() == 0)) {
            return false;
        }

        LOG.debug("analyzeNw: allPceNodes {}", allPceNodes.toString());
        LOG.debug("analyzeNw: allPceLinks {}", allPceLinks.toString());

        return true;
    }

    private boolean filteraddLinks(PceLink pcelink) {

        NodeId nodeId = pcelink.getSourceId();

        if (azSrgs.contains(nodeId)) {
            allPceLinks.put(pcelink.getLinkId(), pcelink);
            allPceNodes.get(nodeId).addOutgoingLink(pcelink);
            LOG.debug("analyzeNw: Add_LINK added to source and to allPceLinks {}", pcelink.getLinkId().toString());
            return true;
        }

        // remove the SRG from PceNodes, as it is not directly connected to A/Z
        allPceNodes.remove(nodeId);
        LOG.debug("analyzeNw: SRG removed {}", nodeId.getValue());

        return false;
    }

    private boolean filterdropLinks(PceLink pcelink) {

        NodeId nodeId = pcelink.getDestId();

        if (azSrgs.contains(nodeId)) {
            allPceLinks.put(pcelink.getLinkId(), pcelink);
            allPceNodes.get(nodeId).addOutgoingLink(pcelink);
            LOG.debug("analyzeNw: Drop_LINK added to dest and to allPceLinks {}", pcelink.getLinkId().toString());
            return true;
        }

        // remove the SRG from PceNodes, as it is not directly connected to A/Z
        allPceNodes.remove(pcelink.getDestId());
        LOG.debug("analyzeNw: SRG removed {}", nodeId.getValue());

        return false;
    }

    private boolean validateLink(Link link) {

        LOG.debug("validateLink: link {} ", link.toString());

        if (linksToExclude.contains(link.getLinkId())) {
            LOG.info("validateLink: Link is ignored due opposite link problem - {}", link.getLinkId().getValue());
            return false;
        }

        NodeId sourceId = link.getSource().getSourceNode();
        NodeId destId = link.getDestination().getDestNode();
        PceNode source = allPceNodes.get(sourceId);
        PceNode dest = allPceNodes.get(destId);

        if (source == null) {
            LOG.debug("validateLink: Link is ignored due source node is rejected by node validation - {}",
                    link.getSource().getSourceNode().getValue());
            return false;
        }
        if (dest == null) {
            LOG.debug("validateLink: Link is ignored due dest node is rejected by node validation - {}",
                    link.getDestination().getDestNode().getValue());
            return false;
        }

        PceLink pcelink = new PceLink(link, source, dest);
        if (!pcelink.isValid()) {
            dropOppositeLink(link);
            LOG.error(" validateLink: Link is ignored due errors in network data or in opposite link");
            return false;
        }

        LinkId linkId = pcelink.getLinkId();

        switch (validateLinkConstraints(pcelink)) {
            case HARD_EXCLUDE :
                dropOppositeLink(link);
                LOG.debug("validateLink: constraints : link is ignored == {}", linkId.getValue());
                return false;
            default:
                break;
        }

        switch (pcelink.getlinkType()) {
            case ROADMTOROADM :
                allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.debug("validateLink: ROADMTOROADM-LINK added to allPceLinks {}", pcelink.toString());
                break;
            case EXPRESSLINK :
                allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.debug("validateLink: EXPRESS-LINK added to allPceLinks {}", pcelink.toString());
                break;
            case ADDLINK :
                pcelink.setClient(source.getRdmSrgClient(pcelink.getSourceTP().toString(), true));
                addLinks.add(pcelink);
                LOG.debug("validateLink: ADD-LINK saved  {}", pcelink.toString());
                break;
            case DROPLINK :
                pcelink.setClient(dest.getRdmSrgClient(pcelink.getDestTP().toString(), false));
                dropLinks.add(pcelink);
                LOG.debug("validateLink: DROP-LINK saved  {}", pcelink.toString());
                break;
            case XPONDERINPUT :
                // store separately all SRG links directly
                azSrgs.add(sourceId);
                // connected to A/Z
                if (!dest.checkTP(pcelink.getDestTP().toString())) {
                    LOG.debug("validateLink: XPONDER-INPUT is rejected as NW port is busy - {} ", pcelink.toString());
                    return false;
                }
                pcelink.setClient(dest.getClient(pcelink.getDestTP().toString()));
                allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.debug("validateLink: XPONDER-INPUT link added to allPceLinks {}", pcelink.toString());
                break;
            // does it mean XPONDER==>>SRG ?
            case XPONDEROUTPUT :
                // store separately all SRG links directly
                azSrgs.add(destId);
                // connected to A/Z
                if (!source.checkTP(pcelink.getSourceTP().toString())) {
                    LOG.debug("validateLink: XPONDER-OUTPUT is rejected as NW port is busy - {} ", pcelink.toString());
                    return false;
                }
                pcelink.setClient(source.getClient(pcelink.getSourceTP().toString()));
                allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.debug("validateLink: XPONDER-OUTPUT link added to allPceLinks {}", pcelink.toString());
                break;
            default:
                LOG.warn("validateLink: link type is not supported {}", pcelink.toString());

        }

        return true;
    }

    private boolean validateNode(Node node) {
        LOG.debug("validateNode: node {} ", node.toString());

        // PceNode will be used in Graph algorithm
        Node1 node1 = node.augmentation(Node1.class);
        if (node1 == null) {
            LOG.error("getNodeType: no Node1 (type) Augmentation for node: [{}]. Node is ignored", node.getNodeId());
        }
        OpenroadmNodeType nodeType = node1.getNodeType();

        PceNode pceNode = new PceNode(node,nodeType,node.getNodeId());
        pceNode.validateAZxponder(anodeId, znodeId);
        pceNode.initWLlist();

        if (!pceNode.isValid()) {
            LOG.warn(" validateNode: Node is ignored");
            return false;
        }

        switch (validateNodeConstraints(pceNode)) {
            case HARD_EXCLUDE :
                return false;

            default :
                break;
        }

        if (pceNode.getSupNodeIdPceNode().equals(this.anodeId)) {
            if (this.aendPceNode != null) {
                LOG.debug("aendPceNode already gets: {}", this.aendPceNode);
            } else if (endPceNode(nodeType,pceNode.getNodeId(), pceNode)) {
                this.aendPceNode = pceNode;
            }
            // returning false otherwise would break E2E test
        }
        if (pceNode.getSupNodeIdPceNode().equals(this.znodeId)) {
            if (this.zendPceNode != null) {
                LOG.debug("zendPceNode already gets: {}", this.zendPceNode);
            } else if (endPceNode(nodeType,pceNode.getNodeId(), pceNode)) {
                this.zendPceNode = pceNode;
            }
            // returning false otherwise would break E2E test
        }

        allPceNodes.put(pceNode.getNodeId(), pceNode);
        LOG.debug("validateNode: node is saved {}", pceNode.getNodeId().getValue());
        return true;
    }

    private ConstraintTypes validateNodeConstraints(PceNode pcenode) {

        if (pceHardConstraints.getExcludeSupNodes().isEmpty() && pceHardConstraints.getExcludeCLLI().isEmpty()) {
            return ConstraintTypes.NONE;
        }

        if (pceHardConstraints.getExcludeSupNodes().contains(pcenode.getSupNodeIdPceNode())) {
            LOG.info("validateNodeConstraints: {}", pcenode.getNodeId().getValue());
            return ConstraintTypes.HARD_EXCLUDE;
        }
        if (pceHardConstraints.getExcludeCLLI().contains(pcenode.getCLLI())) {
            LOG.info("validateNodeConstraints: {}", pcenode.getNodeId().getValue());
            return ConstraintTypes.HARD_EXCLUDE;
        }

        return ConstraintTypes.NONE;
    }

    private ConstraintTypes validateLinkConstraints(PceLink link) {
        if (pceHardConstraints.getExcludeSRLG().isEmpty()) {
            return ConstraintTypes.NONE;
        }

        // for now SRLG is the only constraint for link
        if (link.getlinkType() != OpenroadmLinkType.ROADMTOROADM) {
            return ConstraintTypes.NONE;
        }

        List<Long> constraints = new ArrayList<Long>(pceHardConstraints.getExcludeSRLG());
        constraints.retainAll(link.getsrlgList());
        if (!constraints.isEmpty()) {
            LOG.info("validateLinkConstraints: {}", link.getLinkId().getValue());
            return ConstraintTypes.HARD_EXCLUDE;
        }

        return ConstraintTypes.NONE;
    }

    private void dropOppositeLink(Link link) {
        LinkId opplink = MapUtils.extractOppositeLink(link);

        PceLink oppPceLink = allPceLinks.get(opplink);
        if (oppPceLink != null) {
            allPceLinks.remove(oppPceLink);
        } else {
            linksToExclude.add(opplink);
        }
    }

    private Boolean endPceNode(OpenroadmNodeType openroadmNodeType, NodeId nodeId, PceNode pceNode) {
        switch (openroadmNodeType) {
            case SRG :
                pceNode.initSrgTps();
                this.azSrgs.add(nodeId);
                break;
            case XPONDER :
                pceNode.initXndrTps();
                break;
            default:
                LOG.warn("endPceNode: Node {} is not SRG or XPONDER !", nodeId);
                return false;
        }

        if (!pceNode.isValid()) {
            LOG.error("validateNode : there are no availaible wavelengths in node {}", pceNode.getNodeId().getValue());
            return false;
        }
        return true;
    }

    public PceNode getaendPceNode() {
        return aendPceNode;
    }

    public PceNode getzendPceNode() {
        return zendPceNode;
    }

    public Map<NodeId, PceNode> getAllPceNodes() {
        return this.allPceNodes;
    }

    public Map<LinkId, PceLink> getAllPceLinks() {
        return this.allPceLinks;
    }

    public PceResult getReturnStructure() {
        return returnStructure;
    }

    private static void printNodesInfo(Map<NodeId, PceNode> allpcenodes) {
        Iterator<Map.Entry<NodeId, PceNode>> nodes = allpcenodes.entrySet().iterator();
        while (nodes.hasNext()) {
            PceNode pcenode = nodes.next().getValue();
            List<PceLink> links = pcenode.getOutgoingLinks();
            LOG.info("In printNodes in node {} : outgoing links {} ", pcenode.getNodeId().getValue(), links.toString());
        }
    }

    /*private static void printLinksInfo(Map<LinkId, PceLink> allpcelinks) {
        Iterator<Map.Entry<LinkId, PceLink>> links = allpcelinks.entrySet().iterator();
        while (links.hasNext()) {
            LOG.info("In printLinksInfo link {} : ", links.next().getValue().toString());
        }
    }*/

}
