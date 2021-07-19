/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
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
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.service.ServiceTypes;
import org.opendaylight.transportpce.pce.PceComplianceCheck;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
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
import org.opendaylight.yangtools.yang.common.Uint32;
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
    private Uint32 serviceRate = Uint32.valueOf(0);

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
    private PortMapping portMapping;

    private enum ConstraintTypes {
        NONE, HARD_EXCLUDE, HARD_INCLUDE, HARD_DIVERSITY, SOFT_EXCLUDE, SOFT_INCLUDE, SOFT_DIVERSITY;
    }

    private MappingUtils mappingUtils;

    public PceCalculation(PathComputationRequestInput input, NetworkTransactionService networkTransactionService,
            PceConstraints pceHardConstraints, PceConstraints pceSoftConstraints, PceResult rc,
            PortMapping portMapping) {
        this.input = input;
        this.networkTransactionService = networkTransactionService;
        this.returnStructure = rc;

        this.pceHardConstraints = pceHardConstraints;
        this.mappingUtils = new MappingUtilsImpl(networkTransactionService.getDataBroker());
        this.portMapping = portMapping;
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
        if (!PceComplianceCheck.checkString(input.getServiceAEnd().getServiceFormat().getName())
                || !PceComplianceCheck.checkString(input.getServiceZEnd().getServiceFormat().getName())
                || !PceComplianceCheck.checkString(input.getServiceAEnd().getServiceRate().toString())) {
            LOG.error("Service Format and Service Rate are required for a path calculation");
            return false;
        }
        serviceFormatA = input.getServiceAEnd().getServiceFormat().getName();
        serviceFormatZ = input.getServiceZEnd().getServiceFormat().getName();
        serviceRate = input.getServiceAEnd().getServiceRate();
        serviceType = ServiceTypes.getServiceType(serviceFormatA, serviceRate,
            (NodeTypes.Xpdr.equals(portMapping.getNode(input.getServiceAEnd().getNodeId()).getNodeInfo().getNodeType())
            && input.getServiceAEnd().getTxDirection() != null
            && input.getServiceAEnd().getTxDirection().getPort() != null
            && input.getServiceAEnd().getTxDirection().getPort().getPortName() != null)
                ? portMapping.getMapping(input.getServiceAEnd().getNodeId(),
                    input.getServiceAEnd().getTxDirection().getPort().getPortName())
                : null);

        LOG.info("parseInput: A and Z :[{}] and [{}]", anodeId, znodeId);

        getAZnodeId();

        returnStructure.setRate(input.getServiceAEnd().getServiceRate().toJava());
        returnStructure.setServiceFormat(input.getServiceAEnd().getServiceFormat());
        return true;
    }

    private void getAZnodeId() {
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC4:
            case StringConstants.SERVICE_TYPE_100GE_M:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
                anodeId = input.getServiceAEnd().getTxDirection().getPort().getPortDeviceName();
                znodeId = input.getServiceZEnd().getTxDirection().getPort().getPortDeviceName();
                break;
            default:
                anodeId = input.getServiceAEnd().getNodeId();
                znodeId = input.getServiceZEnd().getNodeId();
                break;
        }
    }

    private boolean readMdSal() {
        InstanceIdentifier<Network> nwInstanceIdentifier = null;
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_100GE_T:
            case StringConstants.SERVICE_TYPE_400GE:
            case StringConstants.SERVICE_TYPE_OTU4:
            case StringConstants.SERVICE_TYPE_OTUC4:
                LOG.info("readMdSal: network {}", NetworkUtils.OVERLAY_NETWORK_ID);
                nwInstanceIdentifier = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();
                break;
            case StringConstants.SERVICE_TYPE_100GE_M:
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC4:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
                LOG.info("readMdSal: network {}", NetworkUtils.OTN_NETWORK_ID);
                nwInstanceIdentifier = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID))).build();
                break;
            default:
                LOG.warn("readMdSal: unknown service-type for service-rate {} and service-format {}", serviceRate,
                    serviceFormatA);
                break;
        }

        if (readTopology(nwInstanceIdentifier) == null) {
            LOG.error("readMdSal: network is null: {}", nwInstanceIdentifier);
            return false;
        }

        allNodes = readTopology(nwInstanceIdentifier).nonnullNode().values().stream().sorted((n1, n2)
            -> n1.getNodeId().getValue().compareTo(n2.getNodeId().getValue())).collect(Collectors.toList());
        Network1 nw1 = readTopology(nwInstanceIdentifier).augmentation(Network1.class);
        if (nw1 == null) {
            LOG.warn("no otn links in otn-topology");
        } else {
            allLinks = nw1.nonnullLink().values().stream().sorted((l1, l2)
                -> l1.getSource().getSourceTp().toString().compareTo(l2.getSource().getSourceTp().toString()))
                    .collect(Collectors.toList());
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

    private Network readTopology(InstanceIdentifier<Network> nwInstanceIdentifier) {
        Network nw = null;
        try {
            Optional<Network> nwOptional =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier).get();
            if (nwOptional.isPresent()) {
                nw = nwOptional.get();
                LOG.debug("readMdSal: network nodes: nwOptional.isPresent = true {}", nw);
                networkTransactionService.close();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("readMdSal: Error reading topology {}", nwInstanceIdentifier);
            networkTransactionService.close();
            returnStructure.setRC(ResponseCodes.RESPONSE_FAILED);
            throw new RuntimeException(
                "readMdSal: Error reading from operational store, topology : " + nwInstanceIdentifier + " :" + e);
        }
        return nw;
    }

    private boolean analyzeNw() {

        LOG.debug("analyzeNw: allNodes size {}, allLinks size {}", allNodes.size(), allLinks.size());
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_100GE_T:
            case  StringConstants.SERVICE_TYPE_OTU4:
            case  StringConstants.SERVICE_TYPE_400GE:
            case  StringConstants.SERVICE_TYPE_OTUC4:
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
                break;

            default:
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
                break;
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

        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_100GE_T:
            case StringConstants.SERVICE_TYPE_OTU4:
            case StringConstants.SERVICE_TYPE_OTUC4:
            case StringConstants.SERVICE_TYPE_400GE:
                return processPceLink(link, sourceId, destId, source, dest);
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_100GE_M:
            case StringConstants.SERVICE_TYPE_ODUC4:
            case StringConstants.SERVICE_TYPE_1GE:
                return processPceOtnLink(link, source, dest);
            default:
                LOG.error(" validateLink: Unmanaged service type {}", serviceType);
                return false;
        }
    }

    private void validateNode(Node node) {
        LOG.debug("validateNode: node {} ", node);
        // PceNode will be used in Graph algorithm
        Node1 node1 = node.augmentation(Node1.class);
        if (node1 == null) {
            LOG.error("getNodeType: no Node1 (type) Augmentation for node: [{}]. Node is ignored", node.getNodeId());
            return;
        }
        if (State.OutOfService.equals(node1.getOperationalState())) {
            LOG.error("getNodeType: node is ignored due to operational state - {}", node1.getOperationalState()
                    .getName());
            return;
        }
        OpenroadmNodeType nodeType = node1.getNodeType();
        String deviceNodeId = MapUtils.getSupNetworkNode(node);
        // Should never happen but because of existing topology test files
        // we have to manage this case
        if (deviceNodeId == null || deviceNodeId.isBlank()) {
            deviceNodeId = node.getNodeId().getValue();
        }

        LOG.info("Device node id {} for {}", deviceNodeId, node);
        PceOpticalNode pceNode = new PceOpticalNode(deviceNodeId, this.serviceType, portMapping, node, nodeType,
            mappingUtils.getOpenRoadmVersion(deviceNodeId), getSlotWidthGranularity(deviceNodeId, node.getNodeId()),
            getCentralFreqGranularity(deviceNodeId, node.getNodeId()));
        pceNode.validateAZxponder(anodeId, znodeId, input.getServiceAEnd().getServiceFormat());
        pceNode.initFrequenciesBitSet();

        if (!pceNode.isValid()) {
            LOG.warn(" validateNode: Node is ignored");
            return;
        }
        if (validateNodeConstraints(pceNode).equals(ConstraintTypes.HARD_EXCLUDE)) {
            return;
        }
        if (endPceNode(nodeType, pceNode.getNodeId(), pceNode) && this.aendPceNode == null
            && isAZendPceNode(this.serviceFormatA, pceNode, anodeId, "A")) {
            this.aendPceNode = pceNode;
        }
        if (endPceNode(nodeType, pceNode.getNodeId(), pceNode) && this.zendPceNode == null
            && isAZendPceNode(this.serviceFormatZ, pceNode, znodeId, "Z")) {
            this.zendPceNode = pceNode;
        }

        allPceNodes.put(pceNode.getNodeId(), pceNode);
        LOG.debug("validateNode: node is saved {}", pceNode.getNodeId().getValue());
        return;
    }

    private boolean isAZendPceNode(String serviceFormat, PceOpticalNode pceNode, String azNodeId, String azEndPoint) {
        switch (serviceFormat) {
            case "Ethernet":
            case "OC":
                if (pceNode.getSupNetworkNodeId().equals(azNodeId)) {
                    return true;
                }
                return false;
            case "OTU":
                if ("A".equals(azEndPoint) && pceNode.getNodeId().getValue()
                    .equals(this.input.getServiceAEnd().getRxDirection().getPort().getPortDeviceName())) {
                    return true;
                }
                if ("Z".equals(azEndPoint) && pceNode.getNodeId().getValue()
                    .equals(this.input.getServiceZEnd().getRxDirection().getPort().getPortDeviceName())) {
                    return true;
                }
                return false;
            default:
                LOG.debug("Unsupported service Format {} for node {}", serviceFormat, pceNode.getNodeId().getValue());
                return false;
        }
    }

    private void validateOtnNode(Node node) {
        LOG.info("validateOtnNode: {} ", node.getNodeId().getValue());
        // PceOtnNode will be used in Graph algorithm
        if (node.augmentation(Node1.class) == null) {
            LOG.error("ValidateOtnNode: no node-type augmentation. Node {} is ignored", node.getNodeId().getValue());
            return;
        }

        OpenroadmNodeType nodeType = node.augmentation(Node1.class).getNodeType();

        PceOtnNode pceOtnNode = new PceOtnNode(node, nodeType, node.getNodeId(), "otn", serviceType);
        pceOtnNode.validateXponder(anodeId, znodeId);

        if (!pceOtnNode.isValid()) {
            LOG.warn(" validateOtnNode: Node {} is ignored", node.getNodeId().getValue());
            return;
        }
        if (validateNodeConstraints(pceOtnNode).equals(ConstraintTypes.HARD_EXCLUDE)) {
            return;
        }
        if (pceOtnNode.getNodeId().getValue().equals(anodeId) && this.aendPceNode == null) {
            this.aendPceNode = pceOtnNode;
        }
        if (pceOtnNode.getNodeId().getValue().equals(znodeId) && this.zendPceNode == null) {
            this.zendPceNode = pceOtnNode;
        }
        allPceNodes.put(pceOtnNode.getNodeId(), pceOtnNode);
        LOG.info("validateOtnNode: node {} is saved", node.getNodeId().getValue());
        return;
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
                pceNode.initXndrTps(input.getServiceAEnd().getServiceFormat());
                break;
            default:
                LOG.warn("endPceNode: Node {} is not SRG or XPONDER !", nodeId);
                return false;
        }

        if (!pceNode.isValid()) {
            LOG.error("validateNode : there are no available frequencies in node {}", pceNode.getNodeId().getValue());
            return false;
        }
        return true;
    }

    private boolean processPceLink(Link link, NodeId sourceId, NodeId destId, PceNode source, PceNode dest) {
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
                pcelink.setClient(source.getRdmSrgClient(pcelink.getSourceTP().getValue()));
                addLinks.add(pcelink);
                LOG.debug("validateLink: ADD-LINK saved  {}", pcelink);
                break;
            case DROPLINK:
                pcelink.setClient(dest.getRdmSrgClient(pcelink.getDestTP().getValue()));
                dropLinks.add(pcelink);
                LOG.debug("validateLink: DROP-LINK saved  {}", pcelink);
                break;
            case XPONDERINPUT:
                // store separately all SRG links directly
                azSrgs.add(sourceId);
                // connected to A/Z
                if (!dest.checkTP(pcelink.getDestTP().getValue())) {
                    LOG.debug(
                        "validateLink: XPONDER-INPUT is rejected as NW port is busy - {} ", pcelink);
                    return false;
                }
                if (dest.getXpdrClient(pcelink.getDestTP().getValue()) != null) {
                    pcelink.setClient(dest.getXpdrClient(pcelink.getDestTP().getValue()));
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
                if (!source.checkTP(pcelink.getSourceTP().getValue())) {
                    LOG.debug(
                        "validateLink: XPONDER-OUTPUT is rejected as NW port is busy - {} ", pcelink);
                    return false;
                }
                if (source.getXpdrClient(pcelink.getSourceTP().getValue()) != null) {
                    pcelink.setClient(source.getXpdrClient(pcelink.getSourceTP().getValue()));
                }
                allPceLinks.put(linkId, pcelink);
                source.addOutgoingLink(pcelink);
                LOG.debug("validateLink: XPONDER-OUTPUT link added to allPceLinks {}", pcelink);
                break;
            default:
                LOG.warn("validateLink: link type is not supported {}", pcelink);
        }
        return true;
    }

    private boolean processPceOtnLink(Link link, PceNode source, PceNode dest) {
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
                if (dest.getXpdrClient(pceOtnLink.getDestTP().getValue()) != null) {
                    pceOtnLink.setClient(dest.getXpdrClient(pceOtnLink.getDestTP().getValue()));
                }

                allPceLinks.put(linkId, pceOtnLink);
                source.addOutgoingLink(pceOtnLink);
                LOG.info("validateLink: OTN-LINK added to allPceLinks {}", pceOtnLink);
                break;
            default:
                LOG.warn("validateLink: link type is not supported {}", pceOtnLink);
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

    /**
     * Get mc capability slot width granularity for device.
     * @param deviceNodeId String
     * @param nodeId NodeId
     * @return slot width granularity
     */
    private BigDecimal getSlotWidthGranularity(String deviceNodeId, NodeId nodeId) {
        // nodeId: openroadm-topology level node
        // deviceNodeId: openroadm-network level node
        List<McCapabilities> mcCapabilities = mappingUtils.getMcCapabilitiesForNode(deviceNodeId);
        String[] params = nodeId.getValue().split("-");
        // DEGx or SRGx or XPDRx
        String moduleName = params[params.length - 1];
        for (McCapabilities mcCapabitility : mcCapabilities) {
            if (mcCapabitility.getMcNodeName().contains("XPDR")
                && mcCapabitility.getSlotWidthGranularity() != null) {
                return mcCapabitility.getSlotWidthGranularity().getValue();
            }
            if (mcCapabitility.getMcNodeName().contains(moduleName)
                    && mcCapabitility.getSlotWidthGranularity() != null) {
                return mcCapabitility.getSlotWidthGranularity().getValue();
            }
        }
        return GridConstant.SLOT_WIDTH_50;
    }

    /**
     * Get mc capability central-width granularity for device.
     * @param deviceNodeId String
     * @param nodeId NodeId
     * @return center-freq granularity
     */
    private BigDecimal getCentralFreqGranularity(String deviceNodeId, NodeId nodeId) {
        // nodeId: openroadm-topology level node
        // deviceNodeId: openroadm-network level node
        List<McCapabilities> mcCapabilities = mappingUtils.getMcCapabilitiesForNode(deviceNodeId);
        String[] params = nodeId.getValue().split("-");
        // DEGx or SRGx or XPDRx
        String moduleName = params[params.length - 1];
        for (McCapabilities mcCapabitility : mcCapabilities) {
            if (mcCapabitility.getMcNodeName().contains("XPDR")
                && mcCapabitility.getCenterFreqGranularity() != null) {
                return mcCapabitility.getCenterFreqGranularity().getValue();
            }
            if (mcCapabitility.getMcNodeName().contains(moduleName)
                && mcCapabitility.getCenterFreqGranularity() != null) {
                return mcCapabitility.getCenterFreqGranularity().getValue();
            }
        }
        return GridConstant.SLOT_WIDTH_50;
    }
}
