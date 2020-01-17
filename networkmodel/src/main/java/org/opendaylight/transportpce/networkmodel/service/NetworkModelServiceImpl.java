/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.R2RLinkDiscovery;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.transportpce.networkmodel.util.ClliNetwork;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmNetwork;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmOtnTopology;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200113.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200113.network.nodes.NodeInfo.OpenroadmVersion;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelServiceImpl implements NetworkModelService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelServiceImpl.class);
    private static final boolean CREATE_MISSING_PARENTS = true;

    private NetworkTransactionService networkTransactionService;
    private final R2RLinkDiscovery linkDiscovery;
    private final PortMapping portMapping;
    private HashMap<String,TopologyShard> topologyShardMountedDevice;
    private HashMap<String,TopologyShard> otnTopologyShardMountedDevice;

    public NetworkModelServiceImpl(final NetworkTransactionService networkTransactionService,
        final R2RLinkDiscovery linkDiscovery, PortMapping portMapping) {

        this.networkTransactionService = networkTransactionService;
        this.linkDiscovery = linkDiscovery;
        this.portMapping = portMapping;
        this.topologyShardMountedDevice = new HashMap<String,TopologyShard>();
        this.otnTopologyShardMountedDevice = new HashMap<String,TopologyShard>();
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
            if (nodeInfo.getNodeType().getIntValue() == 1) {
                this.linkDiscovery.readLLDP(new NodeId(nodeId), openRoadmVersion);
            }
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
                for (Node openRoadmTopologyNode: topologyShard.getNodes()) {
                    LOG.info("creating node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                            NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .child(Node.class, openRoadmTopologyNode.key())
                        .build();
                    networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode,
                        openRoadmTopologyNode, CREATE_MISSING_PARENTS);
                }
                for (Link openRoadmTopologyLink: topologyShard.getLinks()) {
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
                TopologyShard otnTopologyShard = OpenRoadmOtnTopology.createTopologyShard(portMapping.getNode(nodeId));
                if (otnTopologyShard != null) {
                    this.otnTopologyShardMountedDevice.put(nodeId, otnTopologyShard);
                    for (Node otnTopologyNode: otnTopologyShard.getNodes()) {
                        LOG.info("creating otn node {} in {}", otnTopologyNode.getNodeId().getValue(),
                                NetworkUtils.OTN_NETWORK_ID);
                        InstanceIdentifier<Node> iiOtnTopologyNode = InstanceIdentifier.builder(Networks.class)
                                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                                .child(Node.class, otnTopologyNode.key())
                                .build();
                        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyNode,
                            otnTopologyNode);
                    }
                    for (Link otnTopologyLink: otnTopologyShard.getLinks()) {
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
            networkTransactionService.commit().get();
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

    /* (non-Javadoc)
     * @see org.opendaylight.transportpce.networkmodel.service.NetworkModelService#deleteOpenROADMnode(java.lang.String)
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
                for (Node openRoadmTopologyNode: topologyShard .getNodes()) {
                    LOG.info("deleting node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .child(Node.class, openRoadmTopologyNode.key())
                        .build();
                    this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode);
                }
                for (Link openRoadmTopologyLink: topologyShard.getLinks()) {
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
                LOG.info("suppression de otnTopologyShard = {}", otnTopologyShard.toString());
                if (otnTopologyShard != null) {
                    for (Node otnTopologyNode: otnTopologyShard .getNodes()) {
                        LOG.info("deleting node {} in {}", otnTopologyNode.getNodeId().getValue(),
                            NetworkUtils.OTN_NETWORK_ID);
                        InstanceIdentifier<Node> iiotnTopologyNode = InstanceIdentifier.builder(Networks.class)
                            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                            .child(Node.class, otnTopologyNode.key())
                            .build();
                        this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiotnTopologyNode);
                    }
                    for (Link otnTopologyLink: otnTopologyShard.getLinks()) {
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
}
