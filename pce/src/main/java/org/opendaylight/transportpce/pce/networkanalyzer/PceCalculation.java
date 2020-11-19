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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
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
//import org.opendaylight.yangtools.yang.common.Decimal64;
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
    private String serviceFormatA = "";
    private String serviceFormatZ = "";
    private String serviceType = "";
    private Long serviceRate = 0L;

    private PceConstraints pceHardConstraints;

    ///////////// Intermediate data/////////////////
    private List<PceLink> addLinks = new ArrayList<>();
    private List<PceLink> dropLinks = new ArrayList<>();
    private HashSet<NodeId> azSrgs = new HashSet<>();

    private PceNode aendPceNode = null;
    private PceNode zendPceNode = null;

    private List<Link> allLinks = null;
    private List<Node> allNodes = null;

    // this List serves graph calculation
    private Map<NodeId, PceNode> allPceNodes = new HashMap<>();
    // this List serves calculation of ZtoA path description
    // TODO maybe better solution is possible
    private Map<LinkId, PceLink> allPceLinks = new HashMap<>();
    private Set<LinkId> linksToExclude = new HashSet<>();
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

    public void retrievePceNetwork() {

        LOG.info("In PceCalculation retrieveNetwork: ");

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
        if (input.getServiceAEnd().getServiceFormat() == null || input.getServiceZEnd().getServiceFormat() == null
            || input.getServiceAEnd().getServiceRate() == null) {
            LOG.error("Service Format and Service Rate are required for a path calculation");
            return false;
        }
        serviceFormatA = input.getServiceAEnd().getServiceFormat().getName();
        serviceFormatZ = input.getServiceZEnd().getServiceFormat().getName();
        serviceRate = input.getServiceAEnd().getServiceRate().toJava();

        LOG.info("parseInput: A and Z :[{}] and [{}]", anodeId, znodeId);
        if (!(serviceFormatA.equals(serviceFormatZ))) {
            LOG.info("parseInput: different service format for A and Z not handled, will use service format from Aend");
        } else if (serviceRate == 100L) {
            switch (serviceFormatA) {
                case "Ethernet":
                case "OC":
                    serviceType = "100GE";
                    break;
                case "OTU":
                    serviceType = "OTU4";
                    break;
                case "ODU":
                    serviceType = "ODU4";
                    break;
                default:
                    LOG.debug("parseInput: unsupported service type: Format {} Rate 100L", serviceFormatA);
                    break;
            }
            //switch(serviceRate) may seem a better option at first glance.
            //But switching on Long or long is not directly possible in Java.
            //And casting to int bumps the limit here.
            //Passing by ENUM or String are possible alternatives.
            //Maybe HashMap and similar options should also be considered here.
        } else if ("Ethernet".equals(serviceFormatA)) {
        //only rate 100L is currently supported except in Ethernet
            if (serviceRate == 10L) {
                serviceType = "10GE";
            } else if (serviceRate == 1L) {
                serviceType = "1GE";
            } else {
                LOG.debug("parseInput: unsupported service type: Format Ethernet Rate {}", serviceRate);
            }
        } else {
            LOG.debug("parseInput: unsupported service type: Format {} Rate {}",
                serviceFormatA, serviceRate);
        }
        if ("ODU4".equals(serviceType) || "10GE".equals(serviceType)  || "1GE".equals(serviceType)) {
            anodeId = input.getServiceAEnd().getTxDirection().getPort().getPortDeviceName();
            znodeId = input.getServiceZEnd().getTxDirection().getPort().getPortDeviceName();
        } else {
            anodeId = input.getServiceAEnd().getNodeId();
            znodeId = input.getServiceZEnd().getNodeId();
        }

        returnStructure.setRate(input.getServiceAEnd().getServiceRate().toJava());
        returnStructure.setServiceFormat(input.getServiceAEnd().getServiceFormat());
        return true;
    }

    private boolean readMdSal() {
        InstanceIdentifier<Network> nwInstanceIdentifier = null;
        Network nw = null;
        if (("OC".equals(serviceFormatA)) || ("OTU".equals(serviceFormatA)) || (("Ethernet".equals(serviceFormatA))
            && (serviceRate == 100L))) {

            LOG.info("readMdSal: network {}", NetworkUtils.OVERLAY_NETWORK_ID);
            nwInstanceIdentifier = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();
        } else if ("ODU".equals(serviceFormatA) || ("Ethernet".equals(serviceFormatA) && serviceRate == 10L)
            || ("Ethernet".equals(serviceFormatA) && serviceRate == 1L)) {
            LOG.info("readMdSal: network {}", NetworkUtils.OTN_NETWORK_ID);
            nwInstanceIdentifier = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID))).build();
        } else {
            LOG.info("readMdSal: service-rate {} / service-format not handled {}", serviceRate, serviceFormatA);
            return false;
        }

        try {
            Optional<Network> nwOptional =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier).get();
            if (nwOptional.isPresent()) {
                nw = nwOptional.get();
                LOG.debug("readMdSal: network nodes: nwOptional.isPresent = true {}", nw);
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
        allNodes = nw.nonnullNode().values().stream().sorted((n1, n2)
            -> n1.getNodeId().getValue().compareTo(n2.getNodeId().getValue())).collect(Collectors.toList());
        Network1 nw1 = nw.augmentation(Network1.class);
        if (nw1 != null) {
            allLinks = nw1.nonnullLink().values().stream().sorted((l1, l2)
                -> l1.getSource().getSourceTp().toString().compareTo(l2.getSource().getSourceTp().toString()))
                    .collect(Collectors.toList());
        } else {
            LOG.warn("no otn links in otn-topology");
        }
        if (allNodes == null || allNodes.isEmpty()) {
            LOG.error("readMdSal: no nodes ");
            return false;
        }
        LOG.info("readMdSal: network nodes: {} nodes added", allNodes.size());
        LOG.debug("readMdSal: network nodes: {} nodes added", allNodes);

        if (allLinks == null || allLinks.isEmpty()) {
            LOG.error("readMdSal: no links ");
            return false;
        }
        LOG.info("readMdSal: network links: {} links added", allLinks.size());
        LOG.debug("readMdSal: network links: {} links added", allLinks);

        return true;
    }

    private boolean analyzeNw() {

        LOG.debug("analyzeNw: allNodes size {}, allLinks size {}", allNodes.size(), allLinks.size());

        if (("100GE".equals(serviceType)) || ("OTU4".equals(serviceType))) {
            // 100GE service and OTU4 service are handled at the openroadm-topology layer
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
            // debug prints
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

        } else {
            // ODU4, 10GE/ODU2e or 1GE/ODU0 services are handled at openroadm-otn layer

            for (Node node : allNodes) {
                validateOtnNode(node);
            }

            LOG.info("analyzeNw: allPceNodes {}", allPceNodes);

            if (aendPceNode == null || zendPceNode == null) {
                LOG.error("analyzeNw: Error in reading nodes: A or Z do not present in the network");
                return false;
            }
            for (Link link : allLinks) {
                validateLink(link);
            }
        }

        LOG.info("analyzeNw: allPceNodes size {}, allPceLinks size {}", allPceNodes.size(), allPceLinks.size());

        if ((allPceNodes.size() == 0) || (allPceLinks.size() == 0)) {
            return false;
        }

        LOG.debug("analyzeNw: allPceNodes {}", allPceNodes);
        LOG.debug("analyzeNw: allPceLinks {}", allPceLinks);

        return true;
    }

    private boolean filteraddLinks(PceLink pcelink) {

        NodeId nodeId = pcelink.getSourceId();

        if (azSrgs.contains(nodeId)) {
            allPceLinks.put(pcelink.getLinkId(), pcelink);
            allPceNodes.get(nodeId).addOutgoingLink(pcelink);
            LOG.debug("analyzeNw: Add_LINK added to source and to allPceLinks {}", pcelink.getLinkId());
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
            LOG.debug("analyzeNw: Drop_LINK added to dest and to allPceLinks {}", pcelink.getLinkId());
            return true;
        }

        // remove the SRG from PceNodes, as it is not directly connected to A/Z
        allPceNodes.remove(pcelink.getDestId());
        LOG.debug("analyzeNw: SRG removed {}", nodeId.getValue());

        return false;
    }

    private boolean validateLink(Link link) {
        LOG.info("validateLink: link {} ", link);

        NodeId sourceId = link.getSource().getSourceNode();
        NodeId destId = link.getDestination().getDestNode();
        PceNode source = allPceNodes.get(sourceId);
        PceNode dest = allPceNodes.get(destId);
        State state = link.augmentation(Link1.class).getOperationalState();

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

        if (State.OutOfService.equals(state)) {
            LOG.debug("validateLink: Link is ignored due operational state - {}",
                    state.getName());
            return false;
        }

        if (("100GE".equals(serviceType)) || ("OTU4".equals(serviceType))) {
            // 100GE or OTU4 services are handled at WDM Layer
            PceLink pcelink = new PceLink(link, source, dest);
            if (!pcelink.isValid()) {
                dropOppositeLink(link);
                LOG.error(" validateLink: Link is ignored due errors in network data or in opposite link");
                return false;
            }
            LinkId linkId = pcelink.getLinkId();
            if (validateLinkConstraints(pcelink).equals(ConstraintTypes.HARD_EXCLUDE)) {
                dropOppositeLink(link);
                LOG.debug("validateLink: constraints : link is ignored == {}", linkId.getValue());
                return false;
            }
            switch (pcelink.getlinkType()) {
                case ROADMTOROADM:
                case EXPRESSLINK:
                    allPceLinks.put(linkId, pcelink);
                    source.addOutgoingLink(pcelink);
                    LOG.debug("validateLink: {}-LINK added to allPceLinks {}",
                        pcelink.getlinkType(), pcelink);
                    break;
                case ADDLINK:
                    pcelink.setClient(source.getRdmSrgClient(pcelink.getSourceTP().toString()));
                    addLinks.add(pcelink);
                    LOG.debug("validateLink: ADD-LINK saved  {}", pcelink);
                    break;
                case DROPLINK:
                    pcelink.setClient(dest.getRdmSrgClient(pcelink.getDestTP().toString()));
                    dropLinks.add(pcelink);
                    LOG.debug("validateLink: DROP-LINK saved  {}", pcelink);
                    break;
                case XPONDERINPUT:
                    // store separately all SRG links directly
                    azSrgs.add(sourceId);
                    // connected to A/Z
                    if (!dest.checkTP(pcelink.getDestTP().toString())) {
                        LOG.debug(
                            "validateLink: XPONDER-INPUT is rejected as NW port is busy - {} ", pcelink);
                        return false;
                    }
                    if (dest.getXpdrClient(pcelink.getDestTP().toString()) != null) {
                        pcelink.setClient(dest.getXpdrClient(pcelink.getDestTP().toString()));
                    }
                    allPceLinks.put(linkId, pcelink);
                    source.addOutgoingLink(pcelink);
                    LOG.debug("validateLink: XPONDER-INPUT link added to allPceLinks {}", pcelink);
                    break;
                // does it mean XPONDER==>>SRG ?
                case XPONDEROUTPUT:
                    // store separately all SRG links directly
                    azSrgs.add(destId);
                    // connected to A/Z
                    if (!source.checkTP(pcelink.getSourceTP().toString())) {
                        LOG.debug(
                            "validateLink: XPONDER-OUTPUT is rejected as NW port is busy - {} ", pcelink);
                        return false;
                    }
                    if (source.getXpdrClient(pcelink.getSourceTP().toString()) != null) {
                        pcelink.setClient(source.getXpdrClient(pcelink.getSourceTP().toString()));
                    }
                    allPceLinks.put(linkId, pcelink);
                    source.addOutgoingLink(pcelink);
                    LOG.debug("validateLink: XPONDER-OUTPUT link added to allPceLinks {}", pcelink);
                    break;
                default:
                    LOG.warn("validateLink: link type is not supported {}", pcelink);
            }
            return true;

        } else if (("ODU4".equals(serviceType)) || ("10GE".equals(serviceType)) || ("1GE".equals(serviceType))) {
            // ODU4, 1GE and 10GE services relying on ODU2, ODU2e or ODU0 services are handled at OTN layer
            PceLink pceOtnLink = new PceLink(link, source, dest);

            if (!pceOtnLink.isOtnValid(link, serviceType)) {
                dropOppositeLink(link);
                LOG.error(" validateLink: Link is ignored due errors in network data or in opposite link");
                return false;
            }

            LinkId linkId = pceOtnLink.getLinkId();
            if (validateLinkConstraints(pceOtnLink).equals(ConstraintTypes.HARD_EXCLUDE)) {
                dropOppositeLink(link);
                LOG.debug("validateLink: constraints : link is ignored == {}", linkId.getValue());
                return false;
            }

            switch (pceOtnLink.getlinkType()) {
                case OTNLINK:
                    if (dest.getXpdrClient(pceOtnLink.getDestTP().toString()) != null) {
                        pceOtnLink.setClient(dest.getXpdrClient(pceOtnLink.getDestTP().toString()));
                    }

                    allPceLinks.put(linkId, pceOtnLink);
                    source.addOutgoingLink(pceOtnLink);
                    LOG.info("validateLink: OTN-LINK added to allPceLinks {}", pceOtnLink);
                    break;
                default:
                    LOG.warn("validateLink: link type is not supported {}", pceOtnLink);
            }
            return true;

        } else {
            LOG.error(" validateLink: Unmanaged service type {}", serviceType);
            return false;
        }

    }

    private boolean validateNode(Node node) {
        LOG.debug("validateNode: node {} ", node);

        // PceNode will be used in Graph algorithm
        Node1 node1 = node.augmentation(Node1.class);
        if (node1 == null) {
            LOG.error("getNodeType: no Node1 (type) Augmentation for node: [{}]. Node is ignored", node.getNodeId());
            return false;
        }
        if (State.OutOfService.equals(node1.getOperationalState())) {
            LOG.error("getNodeType: node is ignored due to operational state - {}", node1.getOperationalState()
                    .getName());
            return false;
        }
        OpenroadmNodeType nodeType = node1.getNodeType();

        PceOpticalNode pceNode = new PceOpticalNode(node, nodeType, node.getNodeId(),
            input.getServiceAEnd().getServiceFormat(), "optical");
        pceNode.validateAZxponder(anodeId, znodeId);
        pceNode.initWLlist();

        if (!pceNode.isValid()) {
            LOG.warn(" validateNode: Node is ignored");
            return false;
        }

        if (validateNodeConstraints(pceNode).equals(ConstraintTypes.HARD_EXCLUDE)) {
            return false;
        }
        if ((pceNode.getSupNetworkNodeId().equals(anodeId) && (this.aendPceNode == null))
            && (Boolean.TRUE.equals(endPceNode(nodeType, pceNode.getNodeId(), pceNode)))) {
            this.aendPceNode = pceNode;
        }
        if ((pceNode.getSupNetworkNodeId().equals(znodeId) && (this.zendPceNode == null))
            && (Boolean.TRUE.equals(endPceNode(nodeType, pceNode.getNodeId(), pceNode)))) {
            this.zendPceNode = pceNode;
        }

        allPceNodes.put(pceNode.getNodeId(), pceNode);
        LOG.debug("validateNode: node is saved {}", pceNode.getNodeId().getValue());
        return true;
    }

    private boolean validateOtnNode(Node node) {

        LOG.info("validateOtnNode: {} ", node.getNodeId().getValue());
        // PceOtnNode will be used in Graph algorithm
        if (node.augmentation(Node1.class) != null) {
            OpenroadmNodeType nodeType = node.augmentation(Node1.class).getNodeType();

            PceOtnNode pceOtnNode = new PceOtnNode(node, nodeType, node.getNodeId(), "otn", serviceType);
            pceOtnNode.validateXponder(anodeId, znodeId);

            if (!pceOtnNode.isValid()) {
                LOG.warn(" validateOtnNode: Node {} is ignored", node.getNodeId().getValue());
                return false;
            }
            if (validateNodeConstraints(pceOtnNode).equals(ConstraintTypes.HARD_EXCLUDE)) {
                return false;
            }
            if (pceOtnNode.getNodeId().getValue().equals(anodeId) && this.aendPceNode == null) {
                this.aendPceNode = pceOtnNode;
            }
            if (pceOtnNode.getNodeId().getValue().equals(znodeId) && this.zendPceNode == null) {
                this.zendPceNode = pceOtnNode;
            }
            allPceNodes.put(pceOtnNode.getNodeId(), pceOtnNode);
            LOG.info("validateOtnNode: node {} is saved", node.getNodeId().getValue());
            return true;
        } else {
            LOG.error("ValidateOtnNode: no node-type augmentation. Node {} is ignored", node.getNodeId().getValue());
            return false;
        }

//        if (mode == "AZ") {
//            pceOtnNode.validateAZxponder(anodeId, znodeId);
//        } else if (mode == "intermediate") {
//            pceOtnNode.validateIntermediateSwitch();
//        } else {
//            LOG.error("validateOtnNode: unproper mode passed to the method : {} not supported", mode);
//            return null;
//        }
    }

    private ConstraintTypes validateNodeConstraints(PceNode pcenode) {
        if (pceHardConstraints.getExcludeSupNodes().isEmpty() && pceHardConstraints.getExcludeCLLI().isEmpty()) {
            return ConstraintTypes.NONE;
        }
        if (pceHardConstraints.getExcludeSupNodes().contains(pcenode.getSupNetworkNodeId())) {
            LOG.info("validateNodeConstraints: {}", pcenode.getNodeId().getValue());
            return ConstraintTypes.HARD_EXCLUDE;
        }
        if (pceHardConstraints.getExcludeCLLI().contains(pcenode.getSupClliNodeId())) {
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

        List<Long> constraints = new ArrayList<>(pceHardConstraints.getExcludeSRLG());
        constraints.retainAll(link.getsrlgList());
        if (!constraints.isEmpty()) {
            LOG.info("validateLinkConstraints: {}", link.getLinkId().getValue());
            return ConstraintTypes.HARD_EXCLUDE;
        }

        return ConstraintTypes.NONE;
    }

    private void dropOppositeLink(Link link) {
        LinkId opplink = MapUtils.extractOppositeLink(link);

        if (allPceLinks.containsKey(opplink)) {
            allPceLinks.remove(opplink);
        } else {
            linksToExclude.add(opplink);
        }
    }

    private Boolean endPceNode(OpenroadmNodeType openroadmNodeType, NodeId nodeId, PceOpticalNode pceNode) {
        switch (openroadmNodeType) {
            case SRG:
                pceNode.initSrgTps();
                this.azSrgs.add(nodeId);
                break;
            case XPONDER:
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

    public String getServiceType() {
        return serviceType;
    }

    public PceResult getReturnStructure() {
        return returnStructure;
    }

    private static void printNodesInfo(Map<NodeId, PceNode> allPceNodes) {
        allPceNodes.forEach(((nodeId, pceNode) -> {
            LOG.info("In printNodes in node {} : outgoing links {} ", pceNode.getNodeId().getValue(),
                    pceNode.getOutgoingLinks());
        }));
    }
}
