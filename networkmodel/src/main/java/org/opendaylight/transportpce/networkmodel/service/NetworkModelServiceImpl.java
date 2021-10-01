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
import org.opendaylight.transportpce.common.InstanceIdentifiers;
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
import org.opendaylight.transportpce.networkmodel.util.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChanges;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.topology.update.result.TopologyChangesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.link.tp.LinkTpBuilder;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.OtnLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
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
    // Variables for creating and sending topology update notification
    private final NotificationPublishService notificationPublishService;
    private Map<TopologyChangesKey, TopologyChanges> topologyChanges;
    private TopologyUpdateResult notification = null;

    public NetworkModelServiceImpl(final NetworkTransactionService networkTransactionService,
            final R2RLinkDiscovery linkDiscovery, PortMapping portMapping,
            final NotificationPublishService notificationPublishService) {

        this.networkTransactionService = networkTransactionService;
        this.linkDiscovery = linkDiscovery;
        this.portMapping = portMapping;
        this.topologyShardMountedDevice = new HashMap<String, TopologyShard>();
        this.otnTopologyShardMountedDevice = new HashMap<String, TopologyShard>();
        this.notificationPublishService = notificationPublishService;
        this.topologyChanges = new HashMap<TopologyChangesKey, TopologyChanges>();
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
            this.portMapping.deletePortMappingNode(nodeId);

            this.networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            LOG.info("all nodes and links deleted ! ");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error when trying to delete node : {}", nodeId, e);
        }
    }

    @Override
    public void updateOpenRoadmTopologies(String nodeId, Mapping mapping) {
        LOG.info("update OpenRoadm topologies after change update from: {} ", nodeId);
        this.topologyChanges.clear();
        Network openroadmTopology = null;
        Network otnTopology = null;
        Map<LinkKey, Link> openroadmTopologyLinks = null;
        Map<LinkKey, Link> otnTopologyLinks = null;
        try {
            openroadmTopology = this.networkTransactionService
                .read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OVERLAY_NETWORK_II)
                .get().get();
            if (openroadmTopology.augmentation(Network1.class) != null) {
                openroadmTopologyLinks = openroadmTopology.augmentation(Network1.class).getLink();
            }
            otnTopology = this.networkTransactionService
                .read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OTN_NETWORK_II)
                .get().get();
            if (otnTopology.augmentation(Network1.class) != null) {
                otnTopologyLinks = otnTopology.augmentation(Network1.class).getLink();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error when trying to update node : {}", nodeId, e);
        }
        if (openroadmTopology == null || otnTopology == null) {
            LOG.warn("Error getting topologies from datastore");
            return;
        }
        String abstractNodeid = String.join("-", nodeId, mapping.getLogicalConnectionPoint().split("-")[0]);
        // nodes/links update in openroadm-topology
        if (openroadmTopology.getNode() != null) {
            TopologyShard topologyShard = TopologyUtils.updateTopologyShard(abstractNodeid, mapping,
                openroadmTopology.getNode(), openroadmTopologyLinks);
            if (topologyShard.getLinks() != null) {
                for (Link link : topologyShard.getLinks()) {
                    LOG.info("updating links {} in {}", link.getLinkId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Link> iiTopologyLink = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .augmentation(Network1.class)
                        .child(Link.class, link.key())
                        .build();
                    networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiTopologyLink, link);
                }
            }
            if (topologyShard.getTps() != null) {
                for (TerminationPoint tp : topologyShard.getTps()) {
                    LOG.info("updating TP {} in openroadm-topology", tp.getTpId().getValue());
                    InstanceIdentifier<TerminationPoint> iiTopologyTp = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .child(Node.class, new NodeKey(new NodeId(abstractNodeid)))
                        .augmentation(Node1.class)
                        .child(TerminationPoint.class, new TerminationPointKey(tp.getTpId()))
                        .build();
                    networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiTopologyTp, tp);
                    TopologyChanges tc = new TopologyChangesBuilder()
                        .withKey(new TopologyChangesKey(abstractNodeid, tp.getTpId().getValue()))
                        .setNodeId(abstractNodeid)
                        .setTpId(tp.getTpId().getValue())
                        .setState(tp.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1
                                .class).getOperationalState())
                        .build();
                    if (!this.topologyChanges.containsKey(tc.key())) {
                        this.topologyChanges.put(tc.key(), tc);
                    }
                }
            }
        }
        // nodes/links update in otn-topology
        if (otnTopology.getNode() != null
            && otnTopology.getNode().containsKey(new NodeKey(new NodeId(abstractNodeid)))) {
            TopologyShard otnShard = TopologyUtils.updateTopologyShard(abstractNodeid, mapping,
                otnTopology.getNode(), otnTopologyLinks);
            if (otnShard.getLinks() != null) {
                for (Link link : otnShard.getLinks()) {
                    LOG.info("updating links {} in {}", link.getLinkId().getValue(),
                        NetworkUtils.OVERLAY_NETWORK_ID);
                    InstanceIdentifier<Link> iiTopologyLink = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                        .augmentation(Network1.class)
                        .child(Link.class, link.key())
                        .build();
                    networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiTopologyLink, link);
                }
            }
            if (otnShard.getTps() != null) {
                for (TerminationPoint tp : otnShard.getTps()) {
                    LOG.info("updating TP {} in otn-topology", tp.getTpId().getValue());
                    InstanceIdentifier<TerminationPoint> iiTopologyTp = InstanceIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                        .child(Node.class, new NodeKey(new NodeId(abstractNodeid)))
                        .augmentation(Node1.class)
                        .child(TerminationPoint.class, new TerminationPointKey(tp.getTpId()))
                        .build();
                    networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiTopologyTp, tp);
                    TopologyChanges tc = new TopologyChangesBuilder()
                        .withKey(new TopologyChangesKey(abstractNodeid, tp.getTpId().getValue()))
                        .setNodeId(abstractNodeid)
                        .setTpId(tp.getTpId().getValue())
                        .setState(tp.augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1
                                .class).getOperationalState())
                        .build();
                    if (!this.topologyChanges.containsKey(tc.key())) {
                        this.topologyChanges.put(tc.key(), tc);
                    }
                }
            }
        }
        // commit datastore updates
        try {
            networkTransactionService.commit().get();
            sendNotification();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error updating openroadm-topology", e);
        }
    }

    @Override
    public void createOtnLinks(
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link
        notifLink, List<String> suppLinks, OtnLinkType linkType) {

        TopologyShard otnTopologyShard;
        switch (linkType) {
            case OTU4:
            case OTUC4:
                otnTopologyShard = OpenRoadmOtnTopology.createOtnLinks(notifLink, null, null, linkType);
                break;
            case ODTU4:
            case ODUC4:
                List<LinkId> linkIdList = new ArrayList<>();
                if (suppLinks != null) {
                    suppLinks.forEach(lk -> linkIdList.add(new LinkId(lk)));
                }
                List<Link> supportedOtu4links = getOtnLinks(linkIdList);
                String nodeTopoA = convertNetconfNodeIdToTopoNodeId(notifLink.getATermination().getNodeId(),
                    notifLink.getATermination().getTpId());
                String nodeTopoZ = convertNetconfNodeIdToTopoNodeId(notifLink.getZTermination().getNodeId(),
                    notifLink.getZTermination().getTpId());
                List<TerminationPoint> tps = getOtnNodeTps(nodeTopoA, notifLink.getATermination().getTpId(), nodeTopoZ,
                    notifLink.getZTermination().getTpId());
                otnTopologyShard = OpenRoadmOtnTopology.createOtnLinks(notifLink, supportedOtu4links, tps, linkType);
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
    public void deleteOtnLinks(
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link
        notifLink, List<String> suppLinks, OtnLinkType linkType) {

        TopologyShard otnTopologyShard;
        String nodeTopoA = convertNetconfNodeIdToTopoNodeId(notifLink.getATermination().getNodeId(),
            notifLink.getATermination().getTpId());
        String nodeTopoZ = convertNetconfNodeIdToTopoNodeId(notifLink.getZTermination().getNodeId(),
            notifLink.getZTermination().getTpId());
        String tpA = notifLink.getATermination().getTpId();
        String tpZ = notifLink.getZTermination().getTpId();
        List<Link> otuLinks;
        List<LinkId> linkIdList = new ArrayList<>();
        switch (linkType) {
            case OTU4:
            case OTUC4:
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoA, tpA, nodeTopoZ, tpZ, linkType.getName()));
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoZ, tpZ, nodeTopoA, tpA, linkType.getName()));
                otuLinks = getOtnLinks(linkIdList);
                if (checkLinks(otuLinks)) {
                    deleteLinks(otuLinks);
                } else {
                    LOG.error("Error deleting OTU4 links");
                }
                otnTopologyShard = new TopologyShard(null, null);
                break;
            case ODTU4:
            case ODUC4:
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoA, tpA, nodeTopoZ, tpZ, linkType.getName()));
                linkIdList.add(LinkIdUtil.buildOtnLinkId(nodeTopoZ, tpZ, nodeTopoA, tpA, linkType.getName()));
                List<Link> oduLinks = getOtnLinks(linkIdList);
                List<TerminationPoint> tps = getOtnNodeTps(nodeTopoA, tpA, nodeTopoZ, tpZ);
                if (checkLinks(oduLinks) && checkTerminationPoints(tps)) {
                    deleteLinks(oduLinks);
                    linkIdList.clear();
                    if (suppLinks != null) {
                        suppLinks.forEach(lk -> linkIdList.add(new LinkId(lk)));
                    }
                    otuLinks = getOtnLinks(linkIdList);

                    otnTopologyShard = OpenRoadmOtnTopology.deleteOtnLinks(otuLinks, tps, linkType);
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
    public void updateOtnLinks(
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link
            notifLink, Uint32 serviceRate, Short tribPortNb, Short minTribSoltNb, Short maxTribSlotNb,
            boolean isDeletion) {

        LinkTp atermination = new LinkTpBuilder()
            .setNodeId(notifLink.getATermination().getNodeId())
            .setTpId(notifLink.getATermination().getTpId())
            .build();
        LinkTp ztermination = new LinkTpBuilder()
            .setNodeId(notifLink.getZTermination().getNodeId())
            .setTpId(notifLink.getZTermination().getTpId())
            .build();
        List<LinkTp> linkTerminations = new ArrayList<>();
        linkTerminations.add(atermination);
        linkTerminations.add(ztermination);

        List<Link> supportedOdu4Links = getSupportingOdu4Links(linkTerminations, serviceRate);
        List<TerminationPoint> tps = getOtnNodeTps(linkTerminations);
        TopologyShard otnTopologyShard;
        otnTopologyShard = OpenRoadmOtnTopology.updateOtnLinks(supportedOdu4Links, tps, serviceRate, tribPortNb,
            minTribSoltNb, maxTribSlotNb, isDeletion);
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

    @Override
    public void updateOtnLinks(List<String> suppLinks, boolean isDeletion) {
        List<LinkId> linkIdList = new ArrayList<>();
        if (suppLinks != null) {
            suppLinks.forEach(lk -> linkIdList.add(new LinkId(lk)));
        }
        List<Link> supportedOtu4links = getOtnLinks(linkIdList);

        TopologyShard otnTopologyShard = OpenRoadmOtnTopology.updateOtnLinks(supportedOtu4links, isDeletion);
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
        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error adding OTN links in otn-topology", e);
        }
        LOG.info("OTN links updated");
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

    private List<TerminationPoint> getOtnNodeTps(List<LinkTp> linkTerminations) {
        List<TerminationPoint> tps = new ArrayList<>();
        for (LinkTp linkTp : linkTerminations) {
            String tp = linkTp.getTpId();
            String nodeId = new StringBuilder(linkTp.getNodeId()).append("-")
                .append(tp.split("-")[0]).toString();
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

    private List<Link> getSupportingOdu4Links(List<LinkTp> nodesTopoTps, Uint32 serviceRate) {
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
                .stream().filter(lk -> lk.getLinkId().getValue()
                    .startsWith(Uint32.valueOf(100).equals(serviceRate) ? "ODUC4" : "ODTU4"))
                .collect(Collectors.toList());
        }
        if (odu4links == null) {
            return null;
        }
        List<Link> links = new ArrayList<>();
        for (LinkTp linkTp : nodesTopoTps) {
            String tp = linkTp.getTpId();
            String nodeId = new StringBuilder(linkTp.getNodeId()).append("-")
                .append(tp.split("-")[0]).toString();
            Link slink = odu4links.stream().filter(lk -> lk.getSource().getSourceNode().getValue()
                .equals(nodeId) && lk.getSource().getSourceTp().getValue().equals(tp)).findFirst().get();
            if (!links.contains(slink)) {
                links.add(slink);
            }
            Link dlink = odu4links.stream().filter(lk -> lk.getDestination().getDestNode().getValue()
                .equals(nodeId) && lk.getDestination().getDestTp().getValue().equals(tp)).findFirst().get();
            if (!links.contains(dlink)) {
                links.add(dlink);
            }
        }
        LOG.debug("odu4oduC4links = {}", links);
        return links;
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

    private String convertNetconfNodeIdToTopoNodeId(String nodeId, String tpId) {
        return new StringBuilder(nodeId).append("-").append(tpId.split("-")[0]).toString();
    }

    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "false positive, this method is used by public updateOpenRoadmNetworkTopology")
    private void sendNotification() {
        if (topologyChanges.isEmpty()) {
            LOG.warn("Empty Topology Change List. No updates in topology");
            return;
        }
        this.notification = new TopologyUpdateResultBuilder()
            .setTopologyChanges(topologyChanges)
            .build();
        try {
            notificationPublishService.putNotification(this.notification);
        } catch (InterruptedException e) {
            LOG.error("Notification offer rejected. Error={}", e.getMessage());
        }
    }
}
