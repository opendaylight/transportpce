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
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.R2RLinkDiscovery;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.transportpce.networkmodel.util.ClliNetwork;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmFactory;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmNetwork;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.Nodes.OpenroadmVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelServiceImpl implements NetworkModelService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelServiceImpl.class);
    private static final boolean CREATE_MISSING_PARENTS = true;

    private NetworkTransactionService networkTransactionService;
    //private final R2RLinkDiscoveryFactoryMethod linkDiscovery;
    private final R2RLinkDiscovery linkDiscovery;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmFactory openRoadmFactory;
    private final PortMapping portMapping;
    private HashMap<String,TopologyShard> topologyShardMountedDevice;

    public NetworkModelServiceImpl(final NetworkTransactionService networkTransactionService,
        final R2RLinkDiscovery linkDiscovery, DeviceTransactionManager deviceTransactionManager,
            OpenRoadmFactory openRoadmFactory, PortMapping portMapping) {

        this.networkTransactionService = networkTransactionService;
        this.linkDiscovery = linkDiscovery;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmFactory = openRoadmFactory;
        this.portMapping = portMapping;
        this.topologyShardMountedDevice = new HashMap<String,TopologyShard>();
    }

    public void init() {
        LOG.info("init ...");
    }

    public void close() {
    }

    @Override
    public void createOpenROADMnode(String nodeId, String openRoadmVersion) {
        try {
            LOG.info("createOpenROADMNode: {} ", nodeId);

            if (!portMapping.createMappingData(nodeId, openRoadmVersion)) {
                LOG.warn("Could not generate port mapping for {} skipping network model creation", nodeId);
                return;
            }
            this.linkDiscovery.readLLDP(new NodeId(nodeId), openRoadmVersion);

            Node clliNode = ClliNetwork.createNode(this.deviceTransactionManager, nodeId, openRoadmVersion);
            if (clliNode == null) {
                LOG.error("Unable to create clli node! Node id: {}", nodeId);
                return;
            }

            InstanceIdentifier<Node> iiClliNode = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new
                            NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
                .child(Node.class, clliNode.key())
                .build();


            LOG.info("creating node in {}", NetworkUtils.CLLI_NETWORK_ID);
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiClliNode, clliNode,
                CREATE_MISSING_PARENTS);

            Node openRoadmNode = OpenRoadmNetwork.createNode(nodeId, this.deviceTransactionManager, openRoadmVersion);
            if (openRoadmNode == null) {
                LOG.error("Unable to create OpenRoadm node! Node id: {}", nodeId);
                return;
            }
            InstanceIdentifier<Node> iiOpenRoadmNode = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new
                            NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
                .child(Node.class, openRoadmNode.key())
                .build();

            LOG.info("creating node in {}", NetworkUtils.UNDERLAY_NETWORK_ID);
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmNode, openRoadmNode,
                CREATE_MISSING_PARENTS);


            TopologyShard topologyShard = openRoadmFactory.createTopologyShardVersionControl(nodeId);

            if (topologyShard == null) {
                LOG.error("Unable to create topology shard for node {}!", nodeId);
                return;
            }
            this.topologyShardMountedDevice.put(nodeId, topologyShard);

            for (Node openRoadmTopologyNode: topologyShard.getNodes()) {
                LOG.info("creating node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier
                    .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .child(Node.class, openRoadmTopologyNode.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode,
                    openRoadmTopologyNode, CREATE_MISSING_PARENTS);
            }
            for (Link openRoadmTopologyLink: topologyShard.getLinks()) {
                LOG.info("creating link {} in {}", openRoadmTopologyLink.getLinkId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Link> iiOpenRoadmTopologyLink = InstanceIdentifier
                    .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, openRoadmTopologyLink.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyLink,
                    openRoadmTopologyLink, CREATE_MISSING_PARENTS);
            }
            networkTransactionService.submit().get();
            //linkDiscovery.readLLDP(nodeId);
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
            @Nullable
            OpenroadmVersion deviceVersion = this.portMapping.getNode(nodeId).getOpenroadmVersion();
            LOG.info("deleteOpenROADMnode: {} version {}", nodeId, deviceVersion.getName());
            this.portMapping.deleteMappingData(nodeId);

            NodeKey nodeIdKey = new NodeKey(new NodeId(nodeId));

            LOG.info("deleting node in {}", NetworkUtils.CLLI_NETWORK_ID);
            InstanceIdentifier<Node> iiClliNode = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
                .child(Node.class, nodeIdKey)
                .build();
            this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiClliNode);

            LOG.info("deleting node in {}", NetworkUtils.UNDERLAY_NETWORK_ID);
            InstanceIdentifier<Node> iiOpenRoadmNode = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
                .child(Node.class, nodeIdKey)
                .build();
            this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmNode);

            TopologyShard topologyShard = this.topologyShardMountedDevice.get(nodeId);
            if (topologyShard != null) {
                LOG.info("TopologyShard for node '{}' is present", nodeId);
                for (Node openRoadmTopologyNode: topologyShard .getNodes()) {
                    LOG.info("deleting node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                            NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier
                        .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .child(Node.class, openRoadmTopologyNode.key())
                        .build();
                    this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode);
                }
                for (Link openRoadmTopologyLink: topologyShard.getLinks()) {
                    LOG.info("deleting link {} in {}", openRoadmTopologyLink.getLinkId().getValue(),
                            NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Link> iiOpenRoadmTopologyLink = InstanceIdentifier
                        .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .augmentation(Network1.class)
                        .child(Link.class, openRoadmTopologyLink.key())
                        .build();
                    this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyLink);
                }
            } else {
                LOG.warn("TopologyShard for node '{}' is not present", nodeId);
            }
            this.networkTransactionService.submit().get(1, TimeUnit.SECONDS);
            LOG.info("all nodes and links deleted ! ");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error when trying to delete node : {}", nodeId, e);
        }
    }
}
