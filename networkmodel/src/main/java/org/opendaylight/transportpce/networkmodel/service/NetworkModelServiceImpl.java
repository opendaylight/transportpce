/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.DeviceConfig;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.networkmodel.R2RLinkDiscovery;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.transportpce.networkmodel.util.ClliNetwork;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmNetwork;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelServiceImpl implements NetworkModelService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelServiceImpl.class);
    private static final boolean CREATE_MISSING_PARENTS = true;

    private final DataBroker dataBroker;
    private final DeviceConfig deviceConfig;
    private final R2RLinkDiscovery linkDiscovery;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmTopology openRoadmTopology;
    private final PortMapping portMapping;
    private HashMap<String, TopologyShard> topologyShardMountedDevice;

    public NetworkModelServiceImpl(final DataBroker dataBroker, final DeviceConfig deviceConfig,
        final R2RLinkDiscovery linkDiscovery,
        DeviceTransactionManager deviceTransactionManager,
        OpenRoadmTopology openRoadmTopology, PortMapping portMapping) {
        this.dataBroker = dataBroker;
        this.deviceConfig = deviceConfig;
        this.linkDiscovery = linkDiscovery;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmTopology = openRoadmTopology;
        this.portMapping = portMapping;
        this.topologyShardMountedDevice = new HashMap<String, TopologyShard>();
    }

    @Override
    public void createOpenROADMnode(String nodeId) {
        try {
            LOG.info("creating OpenROADMNode: {} ", nodeId);
            LOG.info("Retreiving configuration from node {}.", nodeId);
            Info deviceInfo = this.deviceConfig.getDeviceInfo(nodeId);

            Node clliNode = null;
            if (deviceInfo != null && deviceInfo.getClli() != null) {
                clliNode = ClliNetwork.createNode(deviceInfo.getClli(), nodeId);
            } else {
                LOG.error("Unable to create clli node! Node id: {}", nodeId);
                return;
            }

            Node openRoadmNode = null;
            if (deviceInfo != null) {
                openRoadmNode = OpenRoadmNetwork.createNode(nodeId, deviceInfo);
            } else {
                LOG.error("Unable to create OpenRoadm node! Node id: {}", nodeId);
                return;
            }

            TopologyShard topologyShard = null;
            if (deviceInfo != null) {
                topologyShard = this.openRoadmTopology.createTopologyShard(nodeId, deviceInfo);
                LOG.debug("mapping = {}", topologyShard.getMappings().toString());
                if (!this.portMapping.createMappingData(deviceInfo, topologyShard.getMappings(), topologyShard
                    .getCpToDegreeList())) {
                    LOG.error("Error to create mapping data for {}", nodeId);
                } else {
                    if (NodeTypes.Rdm.getName().equals(deviceInfo.getNodeType().getName())) {
                        this.linkDiscovery.readLLDP(new NodeId(nodeId));
                    }
                }
            } else {
                LOG.error("Unable to create topology shard for node {}.", nodeId);
                return;
            }
            this.topologyShardMountedDevice.put(nodeId, topologyShard);

            WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
            LOG.info("creating node in {}", NetworkUtils.CLLI_NETWORK_ID);
            InstanceIdentifier<Node> iiClliNode = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
                .child(Node.class, clliNode.key())
                .build();
            writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, iiClliNode, clliNode,
                CREATE_MISSING_PARENTS);
            LOG.info("creating node in {}", NetworkUtils.UNDERLAY_NETWORK_ID);
            InstanceIdentifier<Node> iiOpenRoadmNode = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
                .child(Node.class, openRoadmNode.key())
                .build();
            writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmNode, openRoadmNode,
                CREATE_MISSING_PARENTS);
            for (Node openRoadmTopologyNode : topologyShard.getNodes()) {
                LOG.info("creating node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier
                    .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .child(Node.class, openRoadmTopologyNode.key())
                    .build();
                writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode,
                    openRoadmTopologyNode, CREATE_MISSING_PARENTS);
            }
            for (Link openRoadmTopologyLink : topologyShard.getLinks()) {
                LOG.info("creating link {} in {}", openRoadmTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                InstanceIdentifier<Link> iiOpenRoadmTopologyLink = InstanceIdentifier
                    .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, openRoadmTopologyLink.key())
                    .build();
                writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyLink,
                    openRoadmTopologyLink, CREATE_MISSING_PARENTS);
            }
            writeTransaction.submit().get();
            LOG.info("all nodes and links created");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("ERROR: ", e);
        }

    }

    @Override
    public void deleteOpenROADMnode(String nodeId) {
        try {
            LOG.info("deleteOpenROADMnode: {} ", nodeId);
            this.portMapping.deleteMappingData(nodeId);

            NodeKey nodeIdKey = new NodeKey(new NodeId(nodeId));
            WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
            LOG.info("deleting node in {}", NetworkUtils.CLLI_NETWORK_ID);
            InstanceIdentifier<Node> iiClliNode = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
                .child(Node.class, nodeIdKey)
                .build();
            writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, iiClliNode);
            LOG.info("deleting node in {}", NetworkUtils.UNDERLAY_NETWORK_ID);
            InstanceIdentifier<Node> iiOpenRoadmNode = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
                .child(Node.class, nodeIdKey)
                .build();
            writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmNode);
            TopologyShard topologyShard = this.topologyShardMountedDevice.get(nodeId);
            if (topologyShard != null) {
                LOG.info("TopologyShard for node '{}' is present", nodeId);
                for (Node openRoadmTopologyNode : topologyShard.getNodes()) {
                    LOG.info("deleting node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Node> iiOpenRoadmTopologyNode = InstanceIdentifier
                        .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .child(Node.class, openRoadmTopologyNode.key())
                        .build();
                    writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode);
                }
                for (Link openRoadmTopologyLink : topologyShard.getLinks()) {
                    LOG.info("deleting link {} in {}", openRoadmTopologyLink.getLinkId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Link> iiOpenRoadmTopologyLink = InstanceIdentifier
                        .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .augmentation(Network1.class)
                        .child(Link.class, openRoadmTopologyLink.key())
                        .build();
                    writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyLink);
                }
                this.topologyShardMountedDevice.remove(nodeId);
            } else {
                LOG.warn("TopologyShard for node '{}' is not present", nodeId);
            }
            writeTransaction.submit().get();
            LOG.info("all nodes and links deleted ! ");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error when trying to delete node : {}", nodeId, e);
        }
    }

    @Override
    public void setOpenROADMnodeStatus(String nodeId, ConnectionStatus connectionStatus) {
        // TODO Auto-generated method stub

    }

}
