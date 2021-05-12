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
import java.util.Objects;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.OrdTopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.OrdTopologyChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.OrdTopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.transportpce.d._interface.ord.topology.types.rev201116.TopologyNotificationTypes;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.OtnLinkType;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelServiceImpl implements NetworkModelService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelServiceImpl.class);

    private NetworkTransactionService networkTransactionService;
    private final R2RLinkDiscovery linkDiscovery;
    private final PortMapping portMapping;
    private Map<String, TopologyShard> topologyShardMountedDevice;
    private Map<String, TopologyShard> otnTopologyShardMountedDevice;
    // Maps that include topology component changed with its new operational state <id, state>
    private Map<String, State> linksChanged;
    private Map<String, State> terminationPointsChanged;
    // Variables for creating and sending topology update notification
    private final NotificationPublishService notificationPublishService;
    private Map<OrdTopologyChangesKey, OrdTopologyChanges> topologyChanges;
    private TopologyUpdateResult notification = null;

    public NetworkModelServiceImpl(final NetworkTransactionService networkTransactionService,
            final R2RLinkDiscovery linkDiscovery, PortMapping portMapping,
            final NotificationPublishService notificationPublishService) {

        this.networkTransactionService = networkTransactionService;
        this.linkDiscovery = linkDiscovery;
        this.portMapping = portMapping;
        this.topologyShardMountedDevice = new HashMap<String, TopologyShard>();
        this.otnTopologyShardMountedDevice = new HashMap<String, TopologyShard>();
        this.linksChanged = new HashMap<String, State>();
        this.terminationPointsChanged = new HashMap<String, State>();
        this.notificationPublishService = notificationPublishService;
        this.topologyChanges = new HashMap<OrdTopologyChangesKey, OrdTopologyChanges>();
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
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiClliNode, clliNode);

            // node creation in openroadm-network
            Node openroadmNetworkNode = OpenRoadmNetwork.createNode(nodeId, nodeInfo);
            InstanceIdentifier<Node> iiopenroadmNetworkNode = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
                .child(Node.class, openroadmNetworkNode.key())
                .build();
            LOG.info("creating node in {}", NetworkUtils.UNDERLAY_NETWORK_ID);
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiopenroadmNetworkNode,
                openroadmNetworkNode);

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
                        openRoadmTopologyNode);
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
                        openRoadmTopologyLink);
                }
            } else {
                LOG.error("Unable to create openroadm-topology shard for node {}!", nodeId);
            }
            // nodes/links creation in otn-topology
            if (nodeInfo.getNodeType().getIntValue() == 2 && (nodeInfo.getOpenroadmVersion().getIntValue() != 1)) {
                createOpenRoadmOtnNode(nodeId);
            }
            networkTransactionService.commit().get();
            // neighbour links through LLDP
            if (nodeInfo.getNodeType().getIntValue() == 1) {
                this.linkDiscovery.readLLDP(new NodeId(nodeId), openRoadmVersion);
            }
            LOG.info("all nodes and links created");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("ERROR: ", e);
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
            OpenroadmNodeVersion deviceVersion = this.portMapping.getNode(nodeId).getNodeInfo().getOpenroadmVersion();
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
    public void updateOpenRoadmNetworkTopology(String nodeId, CircuitPacks changedCpack) {
        // Clear maps for each NETCONF notification received
        this.linksChanged.clear();
        this.terminationPointsChanged.clear();
        this.topologyChanges.clear();
        // 1. Get the list links and nodes of the current openroadm network topology
        List<Link> linkList = null;
        List<Node> nodesList = null;
        try {
            InstanceIdentifier.InstanceIdentifierBuilder<Network1> network1IID =
                InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .augmentation(Network1.class);
            InstanceIdentifier.InstanceIdentifierBuilder<Network> networkIID =
                InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
            Optional<Network> networkOptional = this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,
                networkIID.build()).get();
            Optional<Network1> network1Optional =
                this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, network1IID.build()).get();
            if (network1Optional.isPresent()) {
                // Links list
                linkList = new ArrayList<>(Objects.requireNonNull(network1Optional.get().getLink()).values());
            }
            if (networkOptional.isPresent()) {
                // Nodes list
                nodesList = new ArrayList<>(Objects.requireNonNull(networkOptional.get().getNode()).values());
            }
        } catch (InterruptedException e) {
            LOG.error("Couldn't get list of links in the network. Error={}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("Couldn't get list of links in the network. Error={}", e.getMessage());
        }
        /* 2. For simplicity the update is only considered in the case of a WSSDEG circuit pack change where client and
        line ports (external ports) of a node are included and there is a 1-to-1 port mapping to the nodes TPs. The
        mapping between ports and TPs internal of a node is a bit different as the is a 1-to-many port mapping */
        String cpackType = changedCpack.getCircuitPackType();
        switch (cpackType) {
            case "ADDROP":
                LOG.info("ADDROP circuit pack modified");
                setTerminationPointsChangedMap(changedCpack, nodeId);
                // setTpStateHashmap(changedCpack);
                break;
            case "WSSDEG":
                LOG.info("WSSDEG circuit pack modified");
                setTerminationPointsChangedMap(changedCpack, nodeId);
                // 3. Update the termination points of the node that sent a NETCONF notification
                updateOpenRoadmNetworkTopologyTPs(nodesList, nodeId);
                // 4. Update the links of the topology affected by the changes on TPs (if any)
                updateOpenRoadmNetworkTopologyLinks(linkList, nodesList);
                // Send notification to service handler
                sendNotification(TopologyNotificationTypes.OpenroadmTopologyUpdate, this.topologyChanges);
                break;
            case "port":
                LOG.info("port circuit pack modified");
                break;
            case "pluggable":
                LOG.info("pluggable circuit pack modified");
                break;
            default:
                LOG.warn("Circuitp pack of type {} not recognized", cpackType);
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
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink, otnTopologyLink);
            }
        }
        if (otnTopologyShard.getTps() != null) {
            for (TerminationPoint otnTopologyTp : otnTopologyShard.getTps()) {
                LOG.info("updating otn nodes TP {} in otn-topology", otnTopologyTp.getTpId().getValue());
                List<SupportingTerminationPoint> supportingTerminationPoint =
                    new ArrayList<>(otnTopologyTp.nonnullSupportingTerminationPoint().values());
                InstanceIdentifier<TerminationPoint> iiOtnTopologyTp = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, new NodeKey(supportingTerminationPoint.get(0).getNodeRef()))
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
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink, otnTopologyLink);
            }
        }
        if (otnTopologyShard.getTps() != null) {
            for (TerminationPoint otnTopologyTp : otnTopologyShard.getTps()) {
                LOG.info("updating otn nodes TP {} in otn-topology", otnTopologyTp.getTpId().getValue());
                List<SupportingTerminationPoint> supportingTerminationPoint =
                    new ArrayList<>(otnTopologyTp.nonnullSupportingTerminationPoint().values());
                InstanceIdentifier<TerminationPoint> iiOtnTopologyTp = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, new NodeKey(supportingTerminationPoint.get(0).getNodeRef()))
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
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink, otnTopologyLink);
            }
        }
        if (otnTopologyShard.getTps() != null) {
            for (TerminationPoint otnTopologyTp : otnTopologyShard.getTps()) {
                LOG.info("updating otn nodes TP {} in otn-topology", otnTopologyTp.getTpId().getValue());
                List<SupportingTerminationPoint> supportingTerminationPoint =
                    new ArrayList<>(otnTopologyTp.nonnullSupportingTerminationPoint().values());
                InstanceIdentifier<TerminationPoint> iiOtnTopologyTp = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, new NodeKey(supportingTerminationPoint.get(0).getNodeRef()))
                    .augmentation(Node1.class)
                    .child(TerminationPoint.class, new TerminationPointKey(new TpId(otnTopologyTp.getTpId()
                        .getValue())))
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
        if (links.isEmpty()) {
            return false;
        }
        for (Link link : links) {
            if (link.augmentation(Link1.class) != null
                    && !link.augmentation(Link1.class).getUsedBandwidth().equals(Uint32.valueOf(0))) {
                return false;
            }
        }
        return true;
    }

    private boolean checkTerminationPoints(List<TerminationPoint> tps) {
        if (tps.isEmpty()) {
            return false;
        }
        for (TerminationPoint tp : tps) {
            if (tp.augmentation(TerminationPoint1.class) != null && tp.augmentation(TerminationPoint1.class)
                    .getXpdrTpPortConnectionAttributes().getTsPool() != null && tp.augmentation(TerminationPoint1.class)
                    .getXpdrTpPortConnectionAttributes().getTsPool().size() != 80) {
                return false;
            }
        }
        return true;
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
            odu4links = netw1Opt
                .get()
                .nonnullLink().values()
                .stream().filter(lk -> lk.getLinkId().getValue().startsWith("ODU4"))
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
                        .equals(nodeId) && lk.getSource().getSourceTp().toString().equals(tp)).findFirst().get();
                    if (!links.contains(slink)) {
                        links.add(slink);
                    }
                    Link dlink = odu4links.stream().filter(lk -> lk.getDestination().getDestNode().getValue()
                        .equals(nodeId) && lk.getDestination().getDestTp().toString().equals(tp)).findFirst().get();
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
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyNode, otnTopologyNode);
            }
            for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                LOG.info("creating otn link {} in {}", otnTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Link> iiOtnTopologyLink = InstanceIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, otnTopologyLink.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyLink, otnTopologyLink);
            }
        } else {
            LOG.error("Unable to create OTN topology shard for node {}!", nodeId);
        }

    }

    private void setTerminationPointsChangedMap(CircuitPacks changedCpack, String nodeId) {
        List<Ports> portsList = new ArrayList<>(Objects.requireNonNull(changedCpack.getPorts()).values());
        for (Ports port : portsList) {
            String lcp = port.getLogicalConnectionPoint();
            if (lcp != null) {
                String abstractNodeid = nodeId + "-" + lcp.split("-")[0];
                if (!this.terminationPointsChanged.containsKey(abstractNodeid + "-" + lcp)) {
                    LOG.info("Node id {}, LCP {}", abstractNodeid, lcp);
                    this.terminationPointsChanged.put(abstractNodeid + "-" + lcp,
                            State.forValue(port.getOperationalState().getIntValue()));
                }
            }
        }
    }

    private void updateOpenRoadmNetworkTopologyTPs(List<Node> nodesList, String nodeId) {
        /* 1. The nodes in nodesList are abstract nodes (i.e. ROADMA01-DEG1) and we have the id of the node that has
        a change (i.e. ROADMA01). So we only need to look for the abstract nodes that belong to the physical node. */
        String abstractNodeId;
        for (Node node : nodesList) {
            abstractNodeId = Objects.requireNonNull(node.getNodeId()).getValue();
            // Checking if the node is operationally inService
            if (abstractNodeId.contains(nodeId) && node.augmentation(org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev200529.Node1.class)
                    .getOperationalState().equals(State.InService)) {
                /* 2. Check if the state of the termination points from the topology shard are equal to the state of
                the termination points in the previously created map. */
                List<TerminationPoint> tpList = new ArrayList<>(Objects.requireNonNull(node.augmentation(Node1.class))
                    .getTerminationPoint().values());
                Map<TerminationPointKey, TerminationPoint> updatedTpMap = new HashMap<>();
                for (TerminationPoint tp : tpList) {
                    String tpId = Objects.requireNonNull(tp.getTpId()).getValue();
                    State tpState = Objects.requireNonNull(tp.augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.common.network.rev200529.TerminationPoint1.class)).getOperationalState();
                    String key = abstractNodeId + "-" + tpId;
                    if (this.terminationPointsChanged.containsKey(key)
                            && !this.terminationPointsChanged.get(key).equals(tpState)) {
                        // The state of a termination point has changed... updating
                        State newTpOperationalState = null;
                        AdminStates newTpAdminState = null;
                        /* 3. If the TP has changed its state, it has to be added to the links Map, as a Link state
                        is defined by the state of the TPs that model the link. */
                        switch (this.terminationPointsChanged.get(key)) {
                            case InService:
                                newTpAdminState = AdminStates.InService;
                                newTpOperationalState = State.InService;
                                // Add TP and state inService to the links Map
                                this.linksChanged.put(key, State.InService);
                                // Update topology change list for service handler notification
                                this.topologyChanges.put(
                                    new OrdTopologyChangesKey(node.getNodeId().getValue() + "-" + tpId),
                                    new OrdTopologyChangesBuilder()
                                        .setId(node.getNodeId().getValue() + "-" + tpId)
                                        .setState(newTpOperationalState)
                                        .build());
                                break;
                            case OutOfService:
                                newTpAdminState = AdminStates.OutOfService;
                                newTpOperationalState = State.OutOfService;
                                // Add TP and state outOfService to the links Map
                                this.linksChanged.put(key, State.OutOfService);
                                // Update topology change list for service handler notification
                                this.topologyChanges.put(
                                    new OrdTopologyChangesKey(node.getNodeId().getValue() + "-" + tpId),
                                    new OrdTopologyChangesBuilder()
                                        .setId(node.getNodeId().getValue() + "-" + tpId)
                                        .setState(newTpOperationalState)
                                        .build());
                                break;
                            case Degraded:
                                LOG.warn("Operational state Degraded not handled");
                                break;
                            default:
                                LOG.warn("Unrecognized state!");
                        }
                        // 4. Add modified TP to the updated List.
                        TerminationPoint updTp = new TerminationPointBuilder().withKey(tp.key())
                            .setTpId(tp.getTpId())
                            .addAugmentation(new TerminationPoint1Builder()
                                .setAdministrativeState(newTpAdminState)
                                .setOperationalState(newTpOperationalState)
                                .build())
                            .build();
                        updatedTpMap.put(tp.key(), updTp);
                    }
                    // 5. Update the list of termination points of the corresponding node and merge to the datastore.
                    if (!updatedTpMap.isEmpty()) {
                        Node updNode = new NodeBuilder().setNodeId(node.getNodeId()).addAugmentation(new Node1Builder()
                            .setTerminationPoint(updatedTpMap).build()).build();
                        InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier.builder(
                            Networks.class).child(Network.class, new NetworkKey(
                                    new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).child(Node.class, node.key())
                            .build();
                        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode,
                            updNode);
                        try {
                            networkTransactionService.commit().get();
                        } catch (InterruptedException e) {
                            LOG.error("Couldnt commit change to openroadm topology.", e);
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            LOG.error("Couldnt commit change to openroadm topology.", e);
                        }
                    }
                }
            }
        }
    }

    private void updateOpenRoadmNetworkTopologyLinks(List<Link> linkList, List<Node> nodesList) {
        for (Link link : linkList) {
            String srcTp = link.getSource().getSourceTp().toString();
            String dstTp = link.getDestination().getDestTp().toString();
            String srcNode = link.getSource().getSourceNode().getValue();
            String dstNode = link.getDestination().getDestNode().getValue();
            State linkState = link.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.common.network.rev200529.Link1.class).getOperationalState();
            String srcKey = srcNode + "-" + srcTp;
            String dstKey = dstNode + "-" + dstTp;
            /* 1. Check the current state of the source and dest tps of the link. If these tps exist on the links Map
            and the states are different, then we need to update the link state accordingly.
            There are several cases depending on the current link state:
                - TPs were both inService and one of them (or both) is (are) now outOfService --> link to outOfService
                - TPs were both outOfService and both of them are now inService --> link to inService
            However, if only one TP exists on the Link map, we will need to check the state of the other end in order to
            make a decision: i.e. we cannot assume that if a TP has changed from outOfService to inService the link will
            become inService, as this can only happen if both TPs are inService, therefore we need to check the other
            end. */
            switch (linkState) {
                case InService:
                    if (this.linksChanged.containsKey(srcKey) && this.linksChanged.containsKey(dstKey)) {
                        // Both TPs of the link have been updated. If one of them is outOfService --> link outOfService
                        if (State.OutOfService.equals(this.linksChanged.get(srcKey)) || State.OutOfService.equals(this
                                .linksChanged.get(dstKey))) {
                            updateLinkStates(link, State.OutOfService, AdminStates.OutOfService);
                        }
                    } else if (this.linksChanged.containsKey(srcKey) && State.OutOfService.equals(this.linksChanged
                            .get(srcKey))) {
                        // Source TP has been changed to outOfService --> link outOfService
                        updateLinkStates(link, State.OutOfService, AdminStates.OutOfService);
                    } else if (this.linksChanged.containsKey(dstKey) && State.OutOfService.equals(this.linksChanged
                            .get(dstKey))) {
                        // Destination TP has been changed to outOfService --> link outOfService
                        updateLinkStates(link, State.OutOfService, AdminStates.OutOfService);
                    }
                    break;
                case OutOfService:
                    if (this.linksChanged.containsKey(srcKey) && this.linksChanged.containsKey(dstKey)) {
                        // Both TPs of the link have been updated. If both of them are inService --> link inService
                        if (State.InService.equals(this.linksChanged.get(srcKey)) || State.InService.equals(this
                                .linksChanged.get(dstKey))) {
                            updateLinkStates(link, State.InService, AdminStates.InService);
                        }
                    } else if (this.linksChanged.containsKey(srcKey) && State.InService.equals(this.linksChanged
                            .get(srcKey))) {
                        // Source TP has been changed to inService --> check the second TP and update link to inService
                        // only if both TPs are inService
                        if (tpInService(dstNode, dstTp, nodesList)) {
                            updateLinkStates(link, State.InService, AdminStates.InService);
                        }
                    } else if (this.linksChanged.containsKey(dstKey) && State.InService.equals(this.linksChanged
                            .get(dstKey))) {
                        // Destination TP has been changed to to inService --> check the second TP and update link to
                        // inService only if both TPs are inService
                        if (tpInService(srcNode, srcTp, nodesList)) {
                            updateLinkStates(link, State.InService, AdminStates.InService);
                        }
                    }
                    break;
                case Degraded:
                    LOG.warn("Link state degraded not handled");
                    break;
                default:
                    LOG.warn("Unrecognized state!");
            }
        }
    }

    private boolean tpInService(String nodeId, String tpId, List<Node> nodesList) {
        // Check the node with dstNode id and check the state of the TP with id dstTP id
        for (Node node : nodesList) {
            if (Objects.requireNonNull(node.getNodeId()).getValue().equals(nodeId)) {
                List<TerminationPoint> tpList = new ArrayList<>(Objects.requireNonNull(Objects.requireNonNull(node
                    .augmentation(Node1.class)).getTerminationPoint()).values());
                for (TerminationPoint tp : tpList) {
                    if (Objects.requireNonNull(tp.getTpId()).getValue().equals(tpId)) {
                        if (State.InService.equals(tp.augmentation(org.opendaylight.yang.gen.v1.http
                                .org.openroadm.common.network.rev200529.TerminationPoint1.class)
                                .getOperationalState())) {
                            // The second TP is also inService
                            return true;
                        }
                        break;
                    }
                }
                break;
            }
        }
        return false;
    }

    private void updateLinkStates(Link link, State state, AdminStates adminStates) {
        // TODO: add change to list of changes
        // Update topology change list
        this.topologyChanges.put(new OrdTopologyChangesKey(link.getLinkId().getValue()),
                new OrdTopologyChangesBuilder().setId(link.getLinkId().getValue()).setState(state).build());
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1 link1 = new Link1Builder()
            .setOperationalState(state).setAdministrativeState(adminStates).build();
        Link updLink = new LinkBuilder().withKey(link.key()).addAugmentation(link1).build();
        InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .augmentation(Network1.class).child(Link.class, link.key());
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(), updLink);
        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException e) {
            LOG.error("Couldnt commit changed to openroadm topology. Error={}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("Couldnt commit changed to openroadm topology. Error={}", e.getMessage());
        }
    }

    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "false positive, this method is used by public updateOpenRoadmNetworkTopology")
    private void sendNotification(TopologyNotificationTypes notificationType,
                                  Map<OrdTopologyChangesKey, OrdTopologyChanges> topologyChangesMap) {
        if (topologyChangesMap.isEmpty()) {
            LOG.warn("Empty Topology Change map. No updates in topology");
            return;
        }
        TopologyUpdateResultBuilder topologyUpdateResultBuilder = new TopologyUpdateResultBuilder()
                .setNotificationType(notificationType).setOrdTopologyChanges(topologyChangesMap);
        this.notification = topologyUpdateResultBuilder.build();
        try {
            notificationPublishService.putNotification(this.notification);
        } catch (InterruptedException e) {
            LOG.error("Notification offer rejected. Error={}", e.getMessage());
        }
    }
}
