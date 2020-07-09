/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.R2RLinkDiscovery;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.transportpce.networkmodel.util.ClliNetwork;
import org.opendaylight.transportpce.networkmodel.util.LinkIdUtil;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmNetwork;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmOtnTopology;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.TopologyUpdateResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.topology.update.result.Changes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.topology.update.result.ChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfo.OpenroadmVersion;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.topology.types.rev200512.TopologyChangeNotificationTypes;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.OtnLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelServiceImpl implements NetworkModelService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelServiceImpl.class);
    private static final boolean CREATE_MISSING_PARENTS = true;

    private NetworkTransactionService networkTransactionService;
    private final NotificationPublishService notificationPublishService;
    private final R2RLinkDiscovery linkDiscovery;
    private final PortMapping portMapping;
    private Map<String, TopologyShard> topologyShardMountedDevice;
    private Map<String, TopologyShard> otnTopologyShardMountedDevice;
    private List<Changes> changes;
    TopologyUpdateResult notification = null;
    private Map<String, org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State> linkHashmap;
    private Map<String,
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State> tpStateHashmap;

    public NetworkModelServiceImpl(final NetworkTransactionService networkTransactionService,
        final R2RLinkDiscovery linkDiscovery, PortMapping portMapping,
                                   NotificationPublishService notificationPublishService) {

        this.networkTransactionService = networkTransactionService;
        this.linkDiscovery = linkDiscovery;
        this.portMapping = portMapping;
        this.topologyShardMountedDevice = new HashMap<String, TopologyShard>();
        this.otnTopologyShardMountedDevice = new HashMap<String, TopologyShard>();
        this.changes = new ArrayList<>();
        this.notificationPublishService = notificationPublishService;
        // This hashmap includes the nodes and a boolean to represent if it has been updated or not in the topology
        this.linkHashmap = new HashMap<String,
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State>();
        this.tpStateHashmap = new HashMap<String,
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State>();
    }

    public void init() {
        LOG.info("init ...");
    }

    public void close() {
    }

    @Override
    public void createOpenRoadmNode(String nodeId, String openRoadmVersion) {
        try {
            LOG.info("createOpenROADMNode: {} ", nodeId);

            if (!portMapping.createMappingData(nodeId, openRoadmVersion)) {
                LOG.warn("Could not generate port mapping for {} skipping network model creation", nodeId);
                return;
            }
            NodeInfo nodeInfo = portMapping.getNode(nodeId).getNodeInfo();
            // node creation in clli-network
            Node clliNode = ClliNetwork.createNode(nodeId, nodeInfo);
            InstanceIdentifier<Node> iiClliNode = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
                .child(Node.class, clliNode.key())
                .build();
            LOG.info("creating node in {}", NetworkUtils.CLLI_NETWORK_ID);
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiClliNode, clliNode,
                CREATE_MISSING_PARENTS);

            // node creation in openroadm-network
            Node openroadmNetworkNode = OpenRoadmNetwork.createNode(nodeId, nodeInfo);
            InstanceIdentifier<Node> iiopenroadmNetworkNode = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
                .child(Node.class, openroadmNetworkNode.key())
                .build();
            LOG.info("creating node in {}", NetworkUtils.UNDERLAY_NETWORK_ID);
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiopenroadmNetworkNode,
                openroadmNetworkNode, CREATE_MISSING_PARENTS);

            // nodes/links creation in openroadm-topology
            TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(portMapping.getNode(nodeId));
            if (topologyShard != null) {
                this.topologyShardMountedDevice.put(nodeId, topologyShard);
                for (Node openRoadmTopologyNode : topologyShard.getNodes()) {
                    LOG.info("creating node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .child(Node.class, openRoadmTopologyNode.key())
                        .build();
                    networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode,
                        openRoadmTopologyNode, CREATE_MISSING_PARENTS);
                }
                for (Link openRoadmTopologyLink : topologyShard.getLinks()) {
                    LOG.info("creating link {} in {}", openRoadmTopologyLink.getLinkId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Link> iiOpenRoadmTopologyLink = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .augmentation(Network1.class)
                        .child(Link.class, openRoadmTopologyLink.key())
                        .build();
                    networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyLink,
                        openRoadmTopologyLink, CREATE_MISSING_PARENTS);
                }
            } else {
                LOG.error("Unable to create openroadm-topology shard for node {}!", nodeId);
            }

            // nodes/links creation in otn-topology
            if (nodeInfo.getNodeType().getIntValue() == 2 && (nodeInfo.getOpenroadmVersion().getIntValue() != 1)) {
                createOpenRoadmOtnNode(nodeId);
            }
            networkTransactionService.commit().get();
            LOG.info("all nodes and links created");
            // LLDP neighbors
            if (nodeInfo.getNodeType().getIntValue() == 1) {
                this.linkDiscovery.readLLDP(new NodeId(nodeId), openRoadmVersion);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("ERROR: ", e);
        }
    }

    // update topology based on the port state change (for the moment)
    @Override
    public void updateOpenRoadmNode(String nodeId, CircuitPacks circuitPacks) {
        // Clear class variables for new notification
        this.tpStateHashmap.clear();
        this.linkHashmap.clear();
        this.changes.clear();

        List<Link> linkList = new ArrayList<>();
        List<Node> nodesList = new ArrayList<>();
        try {
            InstanceIdentifier.InstanceIdentifierBuilder<Network1> network1IID =
                    InstanceIdentifier.builder(Networks.class)
                            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                            .augmentation(Network1.class);
            InstanceIdentifier.InstanceIdentifierBuilder<Network> networkIID = InstanceIdentifier.builder(
                    Networks.class).child(Network.class, new NetworkKey(
                    new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
            Optional<Network> networkOptional = this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,
                    networkIID.build()).get();
            Optional<Network1> network1Optional =
                    this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,
                            network1IID.build()).get();
            if (network1Optional.isPresent()) {
                // Update Links
                linkList = network1Optional.get().getLink();
            }
            if (networkOptional.isPresent()) {
                // Update Nodes
                nodesList = networkOptional.get().getNode();
            }
        } catch (InterruptedException e) {
            LOG.error("Couldn't get list of links in the network.", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("Couldn't get list of links in the network.", e);
        }
        String cpackType = circuitPacks.getCircuitPackType();
        // remember that this works for external ports of the circuit pack. The internal cases are a bit different,
        // as every port is mapped to the same tp. Check cross-connect!
        switch (cpackType) {
            case "ADDDROP":
                // Circuit pack of a SRG. It has Client ports and AD-DEG ports
                LOG.info("ADDROP circuit pack modified");
                setTpStateHashmap(circuitPacks);
                break;
            case "WSSDEG":
                // Circuit pack of a DEG. It has Line port and Client ports
                LOG.info("WSSDEG circuit pack modified");
                setTpStateHashmap(circuitPacks);
                updateTopologyTPs(nodesList, nodeId);
                updateTopologyLinks(linkList, nodesList);
                sendNotifications(TopologyChangeNotificationTypes.OpenroadmTopologyUpdate, this.changes);
                break;
            case "port":
                LOG.info("PORT circuit pack modified");
                break;
            case "pluggable":
                LOG.info("Pluggable circuit pack modified");
                break;
            default:
                LOG.error("Circuit pack type not recognized = {}", cpackType);
        }
    }

    @Override
    public void setOpenRoadmNodeStatus(String nodeId, NetconfNodeConnectionStatus.ConnectionStatus connectionStatus) {
        LOG.info("setOpenROADMNodeStatus: {} {}", nodeId, connectionStatus.name());
        /*
          TODO: set connection status of the device in model,
          TODO: so we don't need to keep it in memory (Set<String> currentMountedDevice)
          TODO: unfortunately there is no connection status OpenROADM in network models
          TODO: waiting for new model version
         */
    }

    /*
     @see org.opendaylight.transportpce.networkmodel.service.NetworkModelService# deleteOpenROADMnode(java.lang.String)
     */

    @Override
    public void deleteOpenRoadmnode(String nodeId) {
        try {
            NodeKey nodeIdKey = new NodeKey(new NodeId(nodeId));

            LOG.info("deleting node in {}", NetworkUtils.UNDERLAY_NETWORK_ID);
            InstanceIdentifier<Node> iiopenroadmNetworkNode = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
                .child(Node.class, nodeIdKey)
                .build();
            this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiopenroadmNetworkNode);

            TopologyShard topologyShard = this.topologyShardMountedDevice.get(nodeId);
            if (topologyShard != null) {
                for (Node openRoadmTopologyNode : topologyShard.getNodes()) {
                    LOG.info("deleting node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .child(Node.class, openRoadmTopologyNode.key())
                        .build();
                    this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode);
                }
                for (Link openRoadmTopologyLink : topologyShard.getLinks()) {
                    LOG.info("deleting link {} in {}", openRoadmTopologyLink.getLinkId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Link> iiOpenRoadmTopologyLink = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .augmentation(Network1.class)
                        .child(Link.class, openRoadmTopologyLink.key())
                        .build();
                    this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyLink);
                }
            } else {
                LOG.warn("TopologyShard for node '{}' is not present", nodeId);
            }
            @Nullable
            OpenroadmVersion deviceVersion = this.portMapping.getNode(nodeId).getNodeInfo().getOpenroadmVersion();
            @Nullable
            NodeTypes nodeType = this.portMapping.getNode(nodeId).getNodeInfo().getNodeType();
            if (nodeType.getIntValue() == 2 && deviceVersion.getIntValue() != 1) {
                TopologyShard otnTopologyShard = this.otnTopologyShardMountedDevice.get(nodeId);
                if (otnTopologyShard != null) {
                    LOG.info("suppression de otnTopologyShard = {}", otnTopologyShard.toString());
                    for (Node otnTopologyNode : otnTopologyShard.getNodes()) {
                        LOG.info("deleting node {} in {}", otnTopologyNode.getNodeId().getValue(),
                            NetworkUtils.OTN_NETWORK_ID);
                        InstanceIdentifier<Node> iiotnTopologyNode = InstanceIdentifier.builder(Networks.class)
                            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                            .child(Node.class, otnTopologyNode.key())
                            .build();
                        this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiotnTopologyNode);
                    }
                    for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                        LOG.info("deleting link {} in {}", otnTopologyLink.getLinkId().getValue(),
                            NetworkUtils.OTN_NETWORK_ID);
                        InstanceIdentifier<Link> iiotnTopologyLink = InstanceIdentifier.builder(Networks.class)
                            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                            .augmentation(Network1.class)
                            .child(Link.class, otnTopologyLink.key())
                            .build();
                        this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiotnTopologyLink);
                    }
                }
            }

            LOG.info("deleteOpenROADMnode: {} version {}", nodeId, deviceVersion.getName());
            this.portMapping.deleteMappingData(nodeId);

            this.networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            LOG.info("all nodes and links deleted ! ");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error when trying to delete node : {}", nodeId, e);
        }
    }

    @Override
    public void createOtnLinks(String nodeA, String tpA, String nodeZ, String tpZ, OtnLinkType linkType) {
        TopologyShard otnTopologyShard;
        switch (linkType) {
            case OTU4:
                otnTopologyShard = OpenRoadmOtnTopology.createOtnLinks(nodeA, tpA, nodeZ, tpZ, linkType);
                break;
            case ODTU4:
                String nodeTopoA = new StringBuilder(nodeA).append("-").append(tpA.split("-")[0]).toString();
                String nodeTopoZ = new StringBuilder(nodeZ).append("-").append(tpZ.split("-")[0]).toString();
                List<LinkId> linkIdList = new ArrayList<>();
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoA, tpA, nodeTopoZ, tpZ, "OTU4"));
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoZ, tpZ, nodeTopoA, tpA, "OTU4"));
                List<Link> supportedOtu4links = getOtnLinks(linkIdList);
                List<TerminationPoint> tps = getOtnNodeTps(nodeTopoA, tpA, nodeTopoZ, tpZ);

                otnTopologyShard = OpenRoadmOtnTopology.createOtnLinks(supportedOtu4links, tps);
                break;
            default:
                LOG.error("unknown otn link type {}", linkType);
                otnTopologyShard = new TopologyShard(null, null);
        }
        if (otnTopologyShard.getLinks() != null) {
            for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                LOG.info("creating and updating otn links {} in {}", otnTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Link> iiOtnTopologyLink = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, otnTopologyLink.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink,
                    otnTopologyLink, CREATE_MISSING_PARENTS);
            }
        }
        if (otnTopologyShard.getTps() != null) {
            for (TerminationPoint otnTopologyTp : otnTopologyShard.getTps()) {
                LOG.info("updating otn nodes TP {} in otn-topology", otnTopologyTp.getTpId().getValue());
                InstanceIdentifier<TerminationPoint> iiOtnTopologyTp = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, new NodeKey(otnTopologyTp.getSupportingTerminationPoint().get(0).getNodeRef()))
                    .augmentation(Node1.class)
                    .child(TerminationPoint.class, new TerminationPointKey(otnTopologyTp.getTpId()))
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyTp, otnTopologyTp);
            }
        }
        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error adding OTN links in otn-topology", e);
        }
        LOG.info("OTN links created");
    }

    @Override
    public void deleteOtnLinks(String nodeA, String tpA, String nodeZ, String tpZ, OtnLinkType linkType) {
        TopologyShard otnTopologyShard;
        String nodeTopoA = new StringBuilder(nodeA).append("-").append(tpA.split("-")[0]).toString();
        String nodeTopoZ = new StringBuilder(nodeZ).append("-").append(tpZ.split("-")[0]).toString();
        List<Link> otu4Links;
        List<LinkId> linkIdList = new ArrayList<>();
        switch (linkType) {
            case OTU4:
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoA, tpA, nodeTopoZ, tpZ, "OTU4"));
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoZ, tpZ, nodeTopoA, tpA, "OTU4"));
                otu4Links = getOtnLinks(linkIdList);
                if (checkLinks(otu4Links)) {
                    deleteLinks(otu4Links);
                } else {
                    LOG.error("Error deleting OTU4 links");
                }
                otnTopologyShard = new TopologyShard(null, null);
                break;
            case ODTU4:
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoA, tpA, nodeTopoZ, tpZ, "ODU4"));
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoZ, tpZ, nodeTopoA, tpA, "ODU4"));
                List<Link> odu4Links = getOtnLinks(linkIdList);
                List<TerminationPoint> tps = getOtnNodeTps(nodeTopoA, tpA, nodeTopoZ, tpZ);
                if (checkLinks(odu4Links) && checkTerminationPoints(tps)) {
                    deleteLinks(odu4Links);
                    linkIdList.clear();
                    linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoA, tpA, nodeTopoZ, tpZ, "OTU4"));
                    linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoZ, tpZ, nodeTopoA, tpA, "OTU4"));
                    otu4Links = getOtnLinks(linkIdList);
                    otnTopologyShard = OpenRoadmOtnTopology.deleteOtnLinks(otu4Links, tps);
                } else {
                    LOG.error("Error deleting ODU4 links");
                    otnTopologyShard = new TopologyShard(null, null);
                }
                break;
            default:
                LOG.error("unknown otn link type {}", linkType);
                otnTopologyShard = new TopologyShard(null, null);
        }
        if (otnTopologyShard.getLinks() != null) {
            for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                LOG.info("deleting and updating otn links {} in {}", otnTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Link> iiOtnTopologyLink = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, otnTopologyLink.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink, otnTopologyLink,
                    CREATE_MISSING_PARENTS);
            }
        }
        if (otnTopologyShard.getTps() != null) {
            for (TerminationPoint otnTopologyTp : otnTopologyShard.getTps()) {
                LOG.info("updating otn nodes TP {} in otn-topology", otnTopologyTp.getTpId().getValue());
                InstanceIdentifier<TerminationPoint> iiOtnTopologyTp = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, new NodeKey(otnTopologyTp.getSupportingTerminationPoint().get(0).getNodeRef()))
                    .augmentation(Node1.class)
                    .child(TerminationPoint.class, new TerminationPointKey(otnTopologyTp.getTpId()))
                    .build();
                networkTransactionService.put(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyTp, otnTopologyTp);
            }
        }
        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error deleting OTN links in otn-topology", e);
        }
        LOG.info("OTN links deletion terminated");
    }

    @Override
    public void updateOtnLinks(List<String> nodeTps, String serviceRate, Short tribPortNb, Short tribSoltNb,
        boolean isDeletion) {
        List<Link> supportedOdu4Links = getSupportingOdu4Links(nodeTps);
        List<TerminationPoint> tps = getOtnNodeTps(nodeTps);
        TopologyShard otnTopologyShard;
        otnTopologyShard = OpenRoadmOtnTopology.updateOtnLinks(supportedOdu4Links, tps, serviceRate, tribPortNb,
            tribSoltNb, isDeletion);
        if (otnTopologyShard.getLinks() != null) {
            for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                LOG.info("creating and updating otn links {} in {}", otnTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Link> iiOtnTopologyLink = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, new LinkKey(new LinkId(otnTopologyLink.getLinkId().getValue())))
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink,
                    otnTopologyLink, CREATE_MISSING_PARENTS);
            }
        }
        if (otnTopologyShard.getTps() != null) {
            for (TerminationPoint otnTopologyTp : otnTopologyShard.getTps()) {
                LOG.info("updating otn nodes TP {} in otn-topology", otnTopologyTp.getTpId().getValue());
                InstanceIdentifier<TerminationPoint> iiOtnTopologyTp = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, new NodeKey(otnTopologyTp.getSupportingTerminationPoint().get(0).getNodeRef()))
                    .augmentation(Node1.class)
                    .child(TerminationPoint.class, new TerminationPointKey(
                        new TpId(otnTopologyTp.getTpId().getValue())))
                    .build();
                if (isDeletion) {
                    networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyTp, otnTopologyTp);
                } else {
                    networkTransactionService.put(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyTp, otnTopologyTp);
                }
            }
        }
        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error updating OTN links in otn-topology", e);
        }
    }

    private List<Link> getOtnLinks(List<LinkId> linkIds) {
        List<Link> links = new ArrayList<>();
        for (LinkId linkId : linkIds) {
            InstanceIdentifier<Link> iiLink = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                .augmentation(Network1.class)
                .child(Link.class, new LinkKey(linkId))
                .build();
            ListenableFuture<Optional<Link>> linkOptLf = networkTransactionService
                .read(LogicalDatastoreType.CONFIGURATION, iiLink);
            if (linkOptLf.isDone()) {
                try {
                    if (linkOptLf.get().isPresent()) {
                        links.add(linkOptLf.get().get());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Error retreiving OTN links from otn-topology", e);
                }
            } else {
                LOG.error("Error retreiving link {} from otn-topology", linkId.getValue());
            }
        }
        return links;
    }

    private boolean checkLinks(List<Link> links) {
        boolean canBeDeleted = true;
        if (links.isEmpty()) {
            return false;
        } else {
            for (Link link : links) {
                if (link.augmentation(Link1.class) != null
                    && !link.augmentation(Link1.class).getUsedBandwidth().equals(Uint32.valueOf(0))) {
                    canBeDeleted = false;
                }
            }
        }
        return canBeDeleted;
    }

    private boolean checkTerminationPoints(List<TerminationPoint> tps) {
        boolean canBeDeleted = true;
        if (tps.isEmpty()) {
            return false;
        } else {
            for (TerminationPoint tp : tps) {
                if (tp.augmentation(TerminationPoint1.class) != null
                    && tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getTsPool() != null
                    && tp.augmentation(TerminationPoint1.class).getXpdrTpPortConnectionAttributes().getTsPool()
                    .size() != 80) {
                    canBeDeleted = false;
                }
            }
        }
        return canBeDeleted;
    }

    private List<TerminationPoint> getOtnNodeTps(String nodeTopoA, String tpA, String nodeTopoZ, String tpZ) {
        List<TerminationPoint> tps = new ArrayList<>();
        InstanceIdentifier<TerminationPoint> iiTpA = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId(nodeTopoA)))
            .augmentation(Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(tpA)))
            .build();
        Optional<TerminationPoint> tpAOpt = Optional.empty();
        InstanceIdentifier<TerminationPoint> iiTpZ = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId(nodeTopoZ)))
            .augmentation(Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(tpZ)))
            .build();
        Optional<TerminationPoint> tpZOpt = Optional.empty();

        if (networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, iiTpA).isDone()
            && networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, iiTpZ).isDone()) {
            try {
                tpAOpt = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, iiTpA).get();
                tpZOpt = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, iiTpZ).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error retreiving tp {} of node {} or tp {} from node {} from otn-topology", tpA, nodeTopoA,
                    tpZ, nodeTopoZ, e);
            }
        } else {
            LOG.error("error getting node termination points from the datastore");
        }

        if (tpAOpt.isPresent() && tpZOpt.isPresent()) {
            tps.add(tpAOpt.get());
            tps.add(tpZOpt.get());
        }
        return tps;
    }

    private List<TerminationPoint> getOtnNodeTps(List<String> nodeTopoTps) {
        List<TerminationPoint> tps = new ArrayList<>();
        for (String str : nodeTopoTps) {
            String nodeId = str.split("--")[0];
            String tp = str.split("--")[1];
            InstanceIdentifier<TerminationPoint> iiTp = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                .child(Node.class, new NodeKey(new NodeId(nodeId)))
                .augmentation(Node1.class)
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(tp)))
                .build();
            Optional<TerminationPoint> tpOpt;
            if (networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, iiTp).isDone()) {
                try {
                    tpOpt = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, iiTp).get();
                    if (tpOpt.isPresent()) {
                        tps.add(tpOpt.get());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Error retreiving tp {} of node {} from otn-topology", tp, nodeId, e);
                }
            } else {
                LOG.error("error getting node termination points from the datastore");
            }
        }
        if (tps.isEmpty()) {
            LOG.warn("returning null");
            return null;
        } else {
            LOG.info("returning tps = {}", tps.toString());
            return tps;
        }
    }

    private void deleteLinks(List<Link> links) {
        for (Link otnTopologyLink : links) {
            LOG.info("deleting link {} from {}", otnTopologyLink.getLinkId().getValue(),
                NetworkUtils.OVERLAY_NETWORK_ID);
            InstanceIdentifier<Link> iiOtnTopologyLink = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                .augmentation(Network1.class)
                .child(Link.class, otnTopologyLink.key())
                .build();
            networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink);
        }
        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error deleting OTN links from otn-topology", e);
        }
    }

    private List<Link> getSupportingOdu4Links(List<String> nodesTopoTps) {
        InstanceIdentifier<Network1> iiOtnTopologyLinks = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
            .augmentation(Network1.class)
            .build();
        ListenableFuture<Optional<Network1>> netw1Fl = networkTransactionService
            .read(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLinks);
        Optional<Network1> netw1Opt = Optional.empty();
        if (netw1Fl.isDone()) {
            try {
                netw1Opt = netw1Fl.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error retreiving list of links from otn-topology", e);
            }
        }
        List<Link> odu4links = null;
        if (netw1Opt.isPresent() && netw1Opt.get().getLink() != null) {
            odu4links = netw1Opt.get().getLink().stream().filter(lk -> lk.getLinkId().getValue().startsWith("ODU4"))
                .collect(Collectors.toList());
        }
        List<Link> links = new ArrayList<>();
        if (odu4links != null) {
            for (String str : nodesTopoTps) {
                String[] nodeAndTp = str.split("--");
                if (nodeAndTp.length >= 2) {
                    String nodeId = nodeAndTp[0];
                    String tp = nodeAndTp[1];
                    Link slink = odu4links.stream().filter(lk -> lk.getSource().getSourceNode().getValue()
                        .equals(nodeId) && lk.getSource().getSourceTp().toString().equals(tp))
                        .findFirst().get();
                    if (!links.contains(slink)) {
                        links.add(slink);
                    }
                    Link dlink = odu4links.stream().filter(lk -> lk.getDestination().getDestNode().getValue()
                        .equals(nodeId) && lk.getDestination().getDestTp().toString().equals(tp))
                        .findFirst().get();
                    if (!links.contains(dlink)) {
                        links.add(dlink);
                    }
                }
            }
            LOG.debug("odu4links = {}", links.toString());
            return links;
        } else {
            return null;
        }
    }

    private void createOpenRoadmOtnNode(String nodeId) {
        TopologyShard otnTopologyShard = OpenRoadmOtnTopology.createTopologyShard(portMapping.getNode(nodeId));
        if (otnTopologyShard != null) {
            this.otnTopologyShardMountedDevice.put(nodeId, otnTopologyShard);
            for (Node otnTopologyNode : otnTopologyShard.getNodes()) {
                LOG.info("creating otn node {} in {}", otnTopologyNode.getNodeId().getValue(),
                    NetworkUtils.OTN_NETWORK_ID);
                InstanceIdentifier<Node> iiOtnTopologyNode = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, otnTopologyNode.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyNode,
                    otnTopologyNode);
            }
            for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                LOG.info("creating otn link {} in {}", otnTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Link> iiOtnTopologyLink = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, otnTopologyLink.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink,
                    otnTopologyLink, CREATE_MISSING_PARENTS);
            }
        } else {
            LOG.error("Unable to create OTN topology shard for node {}!", nodeId);
        }

    }

    private void updateTopologyLinks(List<Link> linkList, List<Node> nodesList) {
        for (Link link : linkList) {
            String srcTp = link.getSource().getSourceTp().toString();
            String dstTp = link.getDestination().getDestTp().toString();
            String srcNode = link.getSource().getSourceNode().getValue();
            String dstNode = link.getDestination().getDestNode().getValue();
            State linkState = link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network
                    .rev181130.Link1.class).getOperationalState();
            if (linkState.equals(State.InService)) {
                if (this.linkHashmap.containsKey(srcTp) && this.linkHashmap.containsKey(dstTp)) {
                    // Both tp seem to be changed
                    if (org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State.OutOfService
                            .equals(this.linkHashmap.get(srcTp)) || org.opendaylight.yang.gen.v1.http.org.openroadm
                            .common.types.rev161014.State.OutOfService.equals(this.linkHashmap.get(dstTp))) {
                        // Change link state to OOS
                        updateLinkToOOS(link);
                    }
                } else if (this.linkHashmap.containsKey(srcTp) && this.linkHashmap.get(srcTp).equals(org.opendaylight
                        .yang.gen.v1.http.org.openroadm.common.types.rev161014.State.OutOfService)) {
                    // Source tp has been updated
                    updateLinkToOOS(link);
                } else if (this.linkHashmap.containsKey(dstTp) && this.linkHashmap.get(dstTp).equals(org.opendaylight
                        .yang.gen.v1.http.org.openroadm.common.types.rev161014.State.OutOfService)) {
                    // Destination tp has been updated
                    updateLinkToOOS(link);
                }
            } else {
                if (this.linkHashmap.containsKey(srcTp) && this.linkHashmap.containsKey(dstTp)) {
                    if (this.linkHashmap.get(srcTp).equals(org.opendaylight.yang.gen.v1.http.org.openroadm
                            .common.types.rev161014.State.InService) && this.linkHashmap.get(dstTp).equals(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State.InService)) {
                        // Change link state to OOS
                        updateLinkToIS(link);
                    }
                } else if (this.linkHashmap.containsKey(srcTp)) {
                    // Check that the other TP is inService with function
                    if (checkOtherEndPointStatus(dstNode, dstTp, nodesList)) {
                        updateLinkToIS(link);
                    } else {
                        LOG.warn("Link {} doesnt have both TPs in Service. No update", link.getLinkId().getValue());
                    }

                } else if (this.linkHashmap.containsKey(dstTp)) {
                    // Check if the other TP is inService with function
                    if (checkOtherEndPointStatus(srcNode, srcTp, nodesList)) {
                        updateLinkToIS(link);
                    } else {
                        LOG.warn("Link {} doesnt have both TPs in Service. No update", link.getLinkId().getValue());
                    }
                }
            }
        }
    }

    private boolean checkOtherEndPointStatus(String nodeId, String tp, List<Node> nodeList) {
        // check status of tps from other nodes within the link
        boolean inService = false;
        String nodeIdValue = null;
        String tpId = null;
        for (Node node : nodeList) {
            nodeIdValue = node.getNodeId().getValue();
            if (nodeIdValue != null) {
                if (nodeIdValue.equals(nodeId)) {
                    List<TerminationPoint> tpList = node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params
                            .xml.ns.yang.ietf.network.topology.rev180226.Node1.class).getTerminationPoint();
                    if (tpList != null) {
                        for (TerminationPoint terminationPoint : tpList) {
                            tpId = terminationPoint.getTpId().getValue();
                            if (tpId != null) {
                                if (tpId.equals(tp)) {
                                    State tpState = terminationPoint.augmentation(org.opendaylight.yang.gen.v1.http
                                            .org.openroadm.common.network.rev181130.TerminationPoint1.class)
                                            .getOperationalState();
                                    if (State.InService.equals(tpState)) {
                                        inService = true;
                                    }
                                    break;
                                }
                                tpId = null;
                            }
                        }
                    }
                    break;
                }
                nodeIdValue = null;
            }
        }
        LOG.info("Going to return that the link can be changed to inService = {}", inService);
        return inService;
    }

    private void updateLinkToIS(Link link) {
        this.changes.add(new ChangesBuilder().setId(link.getLinkId().getValue()).setState(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.State.InService).build());
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1 link1 = new Link1Builder()
                .setOperationalState(State.InService).setAdministrativeState(AdminStates.InService).build();
        Link updatedLink = new LinkBuilder().withKey(link.key()).setLinkId(link.getLinkId())
                .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class,
                        link1).build();
        InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID =
                InstanceIdentifier.builder(Networks.class).child(Network.class, new NetworkKey(new NetworkId(
                        NetworkUtils.OVERLAY_NETWORK_ID))).augmentation(Network1.class).child(Link.class, link.key());
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(), updatedLink);
        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException e) {
            LOG.error("Couldnt commit changed to openroadm topology.", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("Couldnt commit changed to openroadm topology.", e);
        }
    }

    private void updateLinkToOOS(Link link) {
        this.changes.add(new ChangesBuilder().setId(link.getLinkId().getValue()).setState(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.State.OutOfService).build());
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1 link1 = new Link1Builder()
                .setOperationalState(State.OutOfService).setAdministrativeState(AdminStates.OutOfService).build();
        Link updatedLink = new LinkBuilder().withKey(link.key()).setLinkId(link.getLinkId())
                .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class,
                        link1).build();
        InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .augmentation(Network1.class).child(Link.class, link.key());
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(), updatedLink);
        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException e) {
            LOG.error("Couldnt commit changed to openroadm topology.", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("Couldnt commit changed to openroadm topology.", e);
        }
    }

    private void updateTopologyTPs(List<Node> nodesList, String nodeId) {
        String nodeIdvalue = null;
        String tpId = null;
        for (Node node : nodesList) {
            nodeIdvalue = node.getNodeId().getValue();
            if (nodeIdvalue != null) {
                if (nodeIdvalue.contains(nodeId)) {
                    if (node.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                            .Node1.class).getOperationalState().equals(State.InService)) {
                        List<TerminationPoint> tpList = node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params
                                .xml.ns.yang.ietf.network.topology.rev180226.Node1.class).getTerminationPoint();
                        List<TerminationPoint> updatedtpList = new ArrayList<TerminationPoint>();
                        if (tpList != null) {
                            for (TerminationPoint tp : tpList) {
                                tpId = tp.getTpId().getValue();
                                String tpState = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common
                                        .network.rev181130.TerminationPoint1.class).getOperationalState().getName();
                                // If the state of the tp in the topology shard is different, that means that we have
                                // found the port with new state
                                if (tpId != null) {
                                    if (this.tpStateHashmap.containsKey(tpId)) {
                                        if (!this.tpStateHashmap.get(tpId).getName().equals(tpState)) {
                                            State newState = null;
                                            AdminStates newAdminState = null;
                                            if (this.tpStateHashmap.get(tpId).equals(org.opendaylight.yang.gen.v1.http
                                                    .org.openroadm.common.types.rev161014.State.InService)) {
                                                newState = State.InService;
                                                newAdminState = AdminStates.InService;
                                                this.linkHashmap.put(tpId,
                                                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.types
                                                                .rev161014.State.InService);
                                                this.changes.add(new ChangesBuilder()
                                                        .setId(node.getNodeId().getValue() + "-" + tpId).setState(
                                                                org.opendaylight.yang.gen.v1.http.org.openroadm.common
                                                                        .types.rev181019.State.InService).build());
                                            } else {
                                                newState = State.OutOfService;
                                                newAdminState = AdminStates.OutOfService;
                                                this.linkHashmap.put(tpId,
                                                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.types
                                                                .rev161014.State.OutOfService);
                                                this.changes.add(new ChangesBuilder()
                                                        .setId(node.getNodeId().getValue() + "-" + tpId).setState(
                                                                org.opendaylight.yang.gen.v1.http.org.openroadm.common
                                                                        .types.rev181019.State.OutOfService).build());
                                            }
                                            TerminationPoint auxtp = new TerminationPointBuilder().withKey(tp.key())
                                                    .setTpId(tp.getTpId()).addAugmentation(
                                                            org.opendaylight.yang.gen.v1.http.org.openroadm.common
                                                                    .network.rev181130.TerminationPoint1.class,
                                                            new org.opendaylight.yang.gen.v1.http.org.openroadm
                                                                    .common.network.rev181130.TerminationPoint1Builder()
                                                                    .setOperationalState(newState)
                                                                    .setAdministrativeState(newAdminState).build())
                                                    .build();
                                            updatedtpList.add(auxtp);
                                        } else {
                                            LOG.debug("TP {} has the same state", tpId);
                                        }
                                    } else {
                                        LOG.debug("TP {} doesnt exist in hashmap", tpId);
                                    }
                                    tpId = null;
                                }
                            }
                        }
                        if (updatedtpList != null) {
                            // Node update
                            Node aux = new NodeBuilder()
                                    .setNodeId(node.getNodeId())
                                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
                                                    .yang.ietf.network.topology.rev180226.Node1.class,
                                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml
                                                    .ns.yang.ietf.network.topology.rev180226.Node1Builder()
                                                    .setTerminationPoint(updatedtpList).build()).build();
                            //merge to datastore
                            InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier.builder(
                                    Networks.class).child(Network.class, new NetworkKey(
                                    new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).child(Node.class, node.key())
                                    .build();
                            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode,
                                    aux);
                            try {
                                networkTransactionService.commit().get();
                            } catch (InterruptedException e) {
                                LOG.error("Couldnt commit changed to openroadm topology.", e);
                                Thread.currentThread().interrupt();
                            } catch (ExecutionException e) {
                                LOG.error("Couldnt commit changed to openroadm topology.", e);
                            }
                        }
                    } else {
                        LOG.warn("Node {} is OutOfService", node.getNodeId().getValue());
                    }
                }
                nodeIdvalue = null;
            }
        }
    }

    private void setTpStateHashmap(CircuitPacks circuitPacks) {
        List<Ports> portsList = circuitPacks.getPorts();
        if (portsList != null) {
            for (Ports ports : portsList) {
                String lcp = ports.getLogicalConnectionPoint();
                if (lcp != null) {
                    if (!this.tpStateHashmap.containsKey(lcp)) {
                        this.tpStateHashmap.put(lcp, ports.getOperationalState());
                    }
                } else {
                    LOG.warn("Port {} without mapping point", ports.getPortName());
                }
            }
        }
    }

    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "false positive, this method is used by public updateopenroadmnode")
    private void sendNotifications(TopologyChangeNotificationTypes topologyChangeNotificationTypes,
                                   List<Changes> changesList) {
        TopologyUpdateResultBuilder topologyUpdateResultBuilder = new TopologyUpdateResultBuilder()
                .setNotificationType(topologyChangeNotificationTypes);
        if (changesList != null) {
            topologyUpdateResultBuilder.setChanges(changesList);
            this.notification = topologyUpdateResultBuilder.build();
            try {
                notificationPublishService.putNotification(this.notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected: ", e);
            }
        } else {
            LOG.info("No notification created as the list of changes is empty");
        }
    }
}
