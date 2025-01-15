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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.OCPortMapping;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev240923.DataModelEnum;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.or.network.augmentation.rev240923.LinkClassEnum;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.link.tp.LinkTpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.termination.point.SupportingTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev241009.ConnectionOper.ConnectionStatus;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the NetworkModelService to ease the data manipulation on OpenROADM topology models.
 */
@Component(immediate = true)
public class NetworkModelServiceImpl implements NetworkModelService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelServiceImpl.class);

    private NetworkTransactionService networkTransactionService;
    private final R2RLinkDiscovery linkDiscovery;
    private final PortMapping portMapping;
    private final OCPortMapping ocPortMapping;
    private Map<String, TopologyShard> topologyShardMountedDevice;
    private Map<String, TopologyShard> otnTopologyShardMountedDevice;
    // Variables for creating and sending topology update notification
    private final NotificationPublishService notificationPublishService;
    private Map<TopologyChangesKey, TopologyChanges> topologyChanges;
    private TopologyUpdateResult notification = null;

    /**
     * Instantiate the NetworkModelServiceImpl.
     * @param dataBroker Provides access to the conceptual data tree store. Used here to instantiate R2RLinkDiscovery
     * @param deviceTransactionManager Manages data transactions with the netconf devices
     * @param networkTransactionService Service that eases the transaction operations with data-stores
     * @param portMapping Store the abstraction view of the netconf OpenROADM-device
     * @param ocPortMapping Store the abstraction view of the netconf OpenConfig device
     * @param notificationPublishService Notification broker which allows to submit a notifications
     */
    @Activate
    public NetworkModelServiceImpl(@Reference DataBroker dataBroker,
            @Reference DeviceTransactionManager deviceTransactionManager,
            @Reference final NetworkTransactionService networkTransactionService,
            @Reference PortMapping portMapping,
            @Reference OCPortMapping ocPortMapping,
            @Reference final NotificationPublishService notificationPublishService) {

        this.networkTransactionService = networkTransactionService;
        this.linkDiscovery = new R2RLinkDiscovery(dataBroker, deviceTransactionManager, networkTransactionService);
        this.portMapping = portMapping;
        this.ocPortMapping = ocPortMapping;
        this.topologyShardMountedDevice = new HashMap<String, TopologyShard>();
        this.otnTopologyShardMountedDevice = new HashMap<String, TopologyShard>();
        this.notificationPublishService = notificationPublishService;
        this.topologyChanges = new HashMap<TopologyChangesKey, TopologyChanges>();
        LOG.debug("NetworkModelServiceImpl instantiated");
    }

    @Override
    public void createOpenRoadmNode(String nodeId, String openRoadmVersion) {
        try {
            LOG.info("createOpenROADMNode: {} ", nodeId);

            boolean firstMount;
            if (portMapping.getNode(nodeId) == null) {
                firstMount = true;
            } else {
                LOG.info("{} already exists in portmapping but was reconnected", nodeId);
                firstMount = false;
            }

            if (!portMapping.createMappingData(nodeId, openRoadmVersion)) {
                LOG.warn("Could not generate port mapping for {} skipping network model creation", nodeId);
                return;
            }
            Nodes mappingNode = portMapping.getNode(nodeId);
            NodeInfo nodeInfo = mappingNode.getNodeInfo();
            // node creation in clli-network
            addNodeInClliNetwork(nodeId, nodeInfo);
            // node creation in openroadm-network
            addNodeInOpenroadmNetwork(nodeId, nodeInfo);
            // nodes/links creation in openroadm-topology
            addNodeInOpenroadmTopology(mappingNode, firstMount);
            // nodes/links creation in otn-topology
            if (NodeTypes.Xpdr.equals(nodeInfo.getNodeType())
                    && !OpenroadmNodeVersion._121.equals(nodeInfo.getOpenroadmVersion())) {
                addNodeInOtnTopology(nodeId);
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
    public void createOpenConfigNode(String nodeId, String openConfigVersion, IpAddress ipAddress) {
        LOG.info("create openconfig node {}", nodeId);
        if (!ocPortMapping.createMappingData(nodeId, openConfigVersion, ipAddress)) {
            LOG.error("could not generate portmapping {}", nodeId);
        }
        Nodes mappingNode = portMapping.getNode(nodeId);
        NodeInfo nodeInfo = portMapping.getNode(nodeId).getNodeInfo();
        // node creation in clli-network
        addNodeInClliNetwork(nodeId, nodeInfo);
        // node creation in openroadm-network
        addNodeInOpenroadmNetwork(nodeId, nodeInfo);
        // nodes/links creation in openroadm-topology
        addNodeInOpenroadmTopology(mappingNode, true);
        // nodes/links creation in otn-topology
        if (NodeTypes.Xpdr.equals(nodeInfo.getNodeType())) {
            addNodeInOtnTopology(nodeId);
        }

        try {
            networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error adding openconfig node in openroadm network layers");
        }
    }

    @Override
    public void setOpenRoadmNodeStatus(String nodeId, ConnectionStatus connectionStatus) {
        LOG.info("setOpenROADMNodeStatus: {} {}", nodeId, connectionStatus.name());
        /*
          TODO: set connection status of the device in model,
          TODO: so we don't need to keep it in memory (Set<String> currentMountedDevice)
          TODO: unfortunately there is no connection status OpenROADM in network models
          TODO: waiting for new model version
         */
    }

    /**
     * This Method is used to delete OpenROADM node from openroadm network layers and portmapping datastores.
     * @param nodeId
     *     unique node ID of OpenConfig node.
     *
     * @return result of node deletion from network and portmapping datastore
     */
    @Override
    public boolean deleteOpenRoadmnode(String nodeId) {
        if (!this.portMapping.isNodeExist(nodeId)) {
            return false;
        }
        OpenroadmNodeVersion deviceVersion = this.portMapping.getNode(nodeId).getNodeInfo().getOpenroadmVersion();
        LOG.info("deleteOpenROADMnode: {} version {}", nodeId, deviceVersion.getName());
        removeNodeFromOpenroadmNetwork(nodeId);
        removeNodeFromOpenroadmTopology(nodeId);
        NodeTypes nodeType = this.portMapping.getNode(nodeId).getNodeInfo().getNodeType();
        if (NodeTypes.Xpdr.equals(nodeType) && !OpenroadmNodeVersion._121.equals(deviceVersion)) {
            removeNodeFromOtnTopology(nodeId);
        }
        try {
            this.networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            LOG.info("all nodes and links deleted in topologies! ");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error when trying to delete node : {}", nodeId, e);
            return false;
        }
        this.portMapping.deletePortMappingNode(nodeId);
        return true;
    }

    /**
     * This Method is used to delete openconfig node from openroadm network layers and portmapping datastores.
     * @param nodeId
     *     unique node ID of OpenConfig node.
     *
     * @return result of node deletion from network and portmapping datastore
     */
    @Override
    public boolean deleteOpenConfignode(String nodeId) {
        if (!this.portMapping.isNodeExist(nodeId)) {
            return false;
        }
        NodeInfo nodeInfo = this.portMapping.getNode(nodeId).getNodeInfo();
        LOG.info("deleteOpenConfignode: {} version {}", nodeId, nodeInfo.getOpenconfigVersion().getName());

        removeNodeFromOpenroadmNetwork(nodeId);
        removeNodeFromOpenroadmTopology(nodeId);
        if (NodeTypes.Xpdr.equals(nodeInfo.getNodeType())) {
            removeNodeFromOtnTopology(nodeId);
        }
        try {
            this.networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            LOG.info("all nodes and links deleted in topologies! ");

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error when trying to delete node : {}", nodeId, e);
            return false;
        }
        this.portMapping.deletePortMappingNode(nodeId);
        return true;
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
                .get().orElseThrow();
            if (openroadmTopology.augmentation(Network1.class) != null) {
                openroadmTopologyLinks = openroadmTopology.augmentation(Network1.class).getLink();
            }
            otnTopology = this.networkTransactionService
                .read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OTN_NETWORK_II)
                .get().orElseThrow();
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
                    DataObjectIdentifier<Link> iiTopologyLink = DataObjectIdentifier.builder(Networks.class)
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
                    DataObjectIdentifier<TerminationPoint> iiTopologyTp = DataObjectIdentifier.builder(Networks.class)
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
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1
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
                    DataObjectIdentifier<Link> iiTopologyLink = DataObjectIdentifier.builder(Networks.class)
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
                    DataObjectIdentifier<TerminationPoint> iiTopologyTp = DataObjectIdentifier.builder(Networks.class)
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
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1
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
            case OTUC2:
            case OTUC3:
            case OTUC4:
                otnTopologyShard = OpenRoadmOtnTopology.createOtnLinks(notifLink, null, null, linkType);
                break;
            case ODTU4:
            case ODUC2:
            case ODUC3:
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
                DataObjectIdentifier<Link> iiOtnTopologyLink = DataObjectIdentifier.builder(Networks.class)
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
                DataObjectIdentifier<TerminationPoint> iiOtnTopologyTp = DataObjectIdentifier.builder(Networks.class)
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
            case OTUC2:
            case OTUC3:
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
            case ODUC2:
            case ODUC3:
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
                DataObjectIdentifier<Link> iiOtnTopologyLink = DataObjectIdentifier.builder(Networks.class)
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
                DataObjectIdentifier<TerminationPoint> iiOtnTopologyTp = DataObjectIdentifier.builder(Networks.class)
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
            notifLink, List<String> suppLinks, Uint32 serviceRate, Short tribPortNb, Short minTribSoltNb,
            Short maxTribSlotNb, boolean isDeletion) {

        List<LinkTp> linkTerminations = new ArrayList<>();
        List<Link> supportedOdu4Links = null;
        if (notifLink != null) {
            // retreive termination-points to be updated
            linkTerminations.add(new LinkTpBuilder()
                    .setNodeId(notifLink.getATermination().getNodeId())
                    .setTpId(notifLink.getATermination().getTpId())
                    .build());
            linkTerminations.add(new LinkTpBuilder()
                    .setNodeId(notifLink.getZTermination().getNodeId())
                    .setTpId(notifLink.getZTermination().getTpId())
                    .build());
            // retreive supported links
            supportedOdu4Links = getSupportingOdu4Links(linkTerminations, serviceRate);
        } else if (suppLinks != null) {
             // retreive supported links
            List<LinkId> linkIdList = new ArrayList<>();
            if (suppLinks != null) {
                suppLinks.forEach(lk -> linkIdList.add(new LinkId(lk)));
            }
            supportedOdu4Links = getOtnLinks(linkIdList);
            // retreive termination-points to be updated
            for (Link link : supportedOdu4Links) {
                LinkTp atermination = new LinkTpBuilder()
                    .setNodeId(link.getSource().getSourceNode().getValue())
                    .setTpId(link.getSource().getSourceTp().getValue())
                    .build();
                linkTerminations.add(atermination);
            }
        } else {
            LOG.error("Impossible to update OTN links and their associated termination points in otn-topology");
            return;
        }
        List<TerminationPoint> tps = getOtnNodeTps(linkTerminations);
        TopologyShard otnTopologyShard;
        otnTopologyShard = OpenRoadmOtnTopology.updateOtnLinks(supportedOdu4Links, tps, serviceRate, tribPortNb,
            minTribSoltNb, maxTribSlotNb, isDeletion);
        if (otnTopologyShard.getLinks() != null) {
            for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                LOG.info("creating and updating otn links {} in {}", otnTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                DataObjectIdentifier<Link> iiOtnTopologyLink = DataObjectIdentifier.builder(Networks.class)
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
                DataObjectIdentifier<TerminationPoint> iiOtnTopologyTp = DataObjectIdentifier.builder(Networks.class)
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
                DataObjectIdentifier<Link> iiOtnTopologyLink = DataObjectIdentifier.builder(Networks.class)
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

    @Override
    public void createTapiExtNodeAtInit() {
        var clliExt1 = new org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.Node1Builder()
            .setClli("TAPI-SBI-ABS-NODE").build();
        var commonExt1 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
            .setAdministrativeState(AdminStates.InService)
            .setOperationalState(State.InService)
            .setNodeType(OpenroadmNodeType.ROADM)
            .build();
        Node tapiExt = new NodeBuilder().setNodeId(new NodeId("TAPI-SBI-ABS-NODE"))
            .addAugmentation(clliExt1)
            .addAugmentation(commonExt1)
            .build();
        DataObjectIdentifier<Node> iiTapiExtClliNode = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
            .build();
        LOG.info("OR Topo initialization, creating new CLLI TAPI-SBI-ABS-NODE Node in DataStore");
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiTapiExtClliNode, tapiExt);
        networkTransactionService.commit();

        String topoUuid = new Uuid(UUID.nameUUIDFromBytes("SBI - Multi-layer - TAPI topology"
                .getBytes(Charset.forName("UTF-8"))).toString()).toString();
        var tapiExt1 = new org.opendaylight.yang.gen.v1.http.org
            .opendaylight.transportpce.or.network.augmentation.rev240923.Node1Builder()
                .setYangDataModel(DataModelEnum.TapiExt)
                .setTopologyUuid(topoUuid)
                .build();

        DataObjectIdentifier<Node> iiTapiExtNetworkNode = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
            .build();
        LOG.info("OR Topo initialization, creating new NETWORK TAPI-SBI-ABS-NODE Node in DataStore with TopoUUID {}",
            topoUuid);
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiTapiExtNetworkNode,
            createTapiNodeBuilder(NetworkUtils.CLLI_NETWORK_ID, "TAPI-SBI-ABS-NODE")
                .addAugmentation(tapiExt1)
                .addAugmentation(clliExt1)
                .addAugmentation(commonExt1)
                .build());
        networkTransactionService.commit();

        DataObjectIdentifier<Node> iiTapiExtTopologyNode = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
            .build();
        LOG.info("OR Topo initialization, creating new TOPOLOGY TAPI-SBI-ABS-NODE Node in DataStore with TopoUUID {}",
            topoUuid);
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiTapiExtTopologyNode,
            createTapiNodeBuilder(NetworkUtils.UNDERLAY_NETWORK_ID, "TAPI-SBI-ABS-NODE")
                .addAugmentation(tapiExt1)
                .addAugmentation(clliExt1)
                .addAugmentation(commonExt1)
                .build());
        networkTransactionService.commit();

        DataObjectIdentifier<Node> iiTapiExtOtnNode = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
            .build();
        LOG.info("OR Topo initialization, creating new OTN TAPI-SBI-ABS-NODE Node in DataStore with TopoUUID {}",
            topoUuid);
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiTapiExtOtnNode,
            createTapiNodeBuilder(NetworkUtils.OVERLAY_NETWORK_ID, "TAPI-SBI-ABS-NODE")
                .addAugmentation(tapiExt1)
                .addAugmentation(clliExt1)
                .addAugmentation(commonExt1)
                .build());
        networkTransactionService.commit();
    }

    @Override
    public void deleteTapiExtNode() {
        String networkLayer = NetworkUtils.CLLI_NETWORK_ID;
        try {
            DataObjectIdentifier<Node> iiTapiExtClliNode = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
                .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
                .build();
            LOG.info("Following tapi feature desinstallation, Deleting CLLI TAPI-SBI-ABS-NODE Node in DataStore");
            networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiTapiExtClliNode);

            networkLayer = NetworkUtils.UNDERLAY_NETWORK_ID;
            DataObjectIdentifier<Node> iiTapiExtNetworkNode = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
                .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
                .build();
            networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiTapiExtNetworkNode);

            networkLayer = NetworkUtils.OVERLAY_NETWORK_ID;
            DataObjectIdentifier<Node> iiTapiExtTopologyNode = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
                .build();
            networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiTapiExtTopologyNode);

            networkLayer = NetworkUtils.OTN_NETWORK_ID;
            DataObjectIdentifier<Node> iiTapiExtOtnNode = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                .child(Node.class, new NodeKey(new NodeId("TAPI-SBI-ABS-NODE")))
                .build();
            networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiTapiExtOtnNode);
            networkTransactionService.commit().get(1, TimeUnit.SECONDS);

            DataObjectIdentifier<Network> nwInstanceIdentifier = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).build();

            Optional<Network> nwOptional =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier).get();
            networkLayer = NetworkUtils.OVERLAY_NETWORK_ID + "Tapi-Links";
            if (nwOptional.isPresent()) {
                Network1 nw = nwOptional.orElseThrow().augmentation(Network1.class);
                if (nw == null) {
                    LOG.warn("TAPI-SBI-ABS-NODE succesfully deleted, no associated Links found in Datastore");
                    return;
                }
                List<LinkId> tapiLinkIdList = nw.nonnullLink().values().stream()
                    .filter(l -> l.augmentationOrElseThrow(org.opendaylight.yang.gen.v1.http.org.opendaylight
                        .transportpce.or.network.augmentation.rev240923.Link1.class).getLinkClass()
                        .equals(LinkClassEnum.AlienToTapi)
                        || l.augmentationOrElseThrow(org.opendaylight.yang.gen.v1.http.org.opendaylight
                            .transportpce.or.network.augmentation.rev240923.Link1.class).getLinkClass()
                                .equals(LinkClassEnum.InterDomain))
                    .map(Link::getLinkId)
                    .collect(Collectors.toList());
                if (tapiLinkIdList == null || tapiLinkIdList.isEmpty()) {
                    LOG.warn("TAPI-SBI-ABS-NODE succesfully deleted, no associated Links found in Datastore");
                    return;
                }
                for (LinkId linkId : tapiLinkIdList) {
                    DataObjectIdentifier<Link> iiORNetworkLink = DataObjectIdentifier.builder(Networks.class)
                        .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                        .augmentation(Network1.class)
                        .child(Link.class, new LinkKey(linkId))
                        .build();
                    networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiORNetworkLink);
                }
                networkTransactionService.commit().get(1, TimeUnit.SECONDS);
                LOG.info("TAPI-SBI-ABS-NODE and associated Links succesfully deleted from Datastore");
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error trying to delete TAPI-SBI-ABS-NODE and associated Links at {} Level in Datastore",
                networkLayer,e);
        }
    }

    private void addNodeInClliNetwork(String nodeId, NodeInfo nodeInfo) {
        Node clliNode = ClliNetwork.createNode(nodeId, nodeInfo);
        DataObjectIdentifier<Node> iiClliNode = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)))
            .child(Node.class, clliNode.key())
            .build();
        LOG.info("creating node in {}", NetworkUtils.CLLI_NETWORK_ID);
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiClliNode, clliNode);

    }

    private void addNodeInOpenroadmNetwork(String nodeId, NodeInfo nodeInfo) {
        Node openroadmNetworkNode = OpenRoadmNetwork.createNode(nodeId, nodeInfo);
        DataObjectIdentifier<Node> iiopenroadmNetworkNode = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
            .child(Node.class, openroadmNetworkNode.key())
            .build();
        LOG.info("creating node in {}", NetworkUtils.UNDERLAY_NETWORK_ID);
        networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiopenroadmNetworkNode,
            openroadmNetworkNode);
    }

    private void addNodeInOpenroadmTopology(Nodes mappingNode, boolean firstMount) {
        // nodes/links creation in openroadm-topology
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode, firstMount);
        if (topologyShard != null) {
            this.topologyShardMountedDevice.put(mappingNode.getNodeId(), topologyShard);
            for (Node openRoadmTopologyNode : topologyShard.getNodes()) {
                LOG.info("creating node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                DataObjectIdentifier<Node> iiOpenRoadmTopologyNode = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .child(Node.class, openRoadmTopologyNode.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode,
                    openRoadmTopologyNode);
            }
            for (Link openRoadmTopologyLink : topologyShard.getLinks()) {
                LOG.info("creating link {} in {}", openRoadmTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                DataObjectIdentifier<Link> iiOpenRoadmTopologyLink = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, openRoadmTopologyLink.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyLink,
                    openRoadmTopologyLink);
            }
        } else {
            LOG.error("Unable to create openroadm-topology shard for node {}!", mappingNode.getNodeId());
        }
    }

    private void addNodeInOtnTopology(String nodeId) {
        TopologyShard otnTopologyShard = OpenRoadmOtnTopology.createTopologyShard(portMapping.getNode(nodeId));
        if (otnTopologyShard != null) {
            this.otnTopologyShardMountedDevice.put(nodeId, otnTopologyShard);
            for (Node otnTopologyNode : otnTopologyShard.getNodes()) {
                LOG.info("creating otn node {} in {}", otnTopologyNode.getNodeId().getValue(),
                    NetworkUtils.OTN_NETWORK_ID);
                DataObjectIdentifier<Node> iiOtnTopologyNode = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, otnTopologyNode.key())
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, iiOtnTopologyNode, otnTopologyNode);
            }
            for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                LOG.info("creating otn link {} in {}", otnTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                DataObjectIdentifier<Link> iiOtnTopologyLink = DataObjectIdentifier.builder(Networks.class)
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

    private void removeNodeFromOpenroadmNetwork(String nodeId) {
        LOG.info("deleting node {} in {}", nodeId, NetworkUtils.UNDERLAY_NETWORK_ID);
        DataObjectIdentifier<Node> iiopenroadmNetworkNode = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId(nodeId)))
            .build();
        this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiopenroadmNetworkNode);
    }

    private void removeNodeFromOpenroadmTopology(String nodeId) {
        TopologyShard topologyShard = this.topologyShardMountedDevice.get(nodeId);
        if (topologyShard != null) {
            for (Node openRoadmTopologyNode : topologyShard.getNodes()) {
                LOG.info("deleting node {} in {}", openRoadmTopologyNode.getNodeId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                DataObjectIdentifier<Node> iiOpenRoadmTopologyNode = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .child(Node.class, openRoadmTopologyNode.key())
                    .build();
                this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyNode);
            }
            for (Link openRoadmTopologyLink : topologyShard.getLinks()) {
                LOG.info("deleting link {} in {}", openRoadmTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OVERLAY_NETWORK_ID);
                DataObjectIdentifier<Link> iiOpenRoadmTopologyLink = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, openRoadmTopologyLink.key())
                    .build();
                this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiOpenRoadmTopologyLink);
            }
        } else {
            LOG.warn("TopologyShard for node '{}' is not present", nodeId);
        }
    }

    private void removeNodeFromOtnTopology(String nodeId) {
        TopologyShard otnTopologyShard = this.otnTopologyShardMountedDevice.get(nodeId);
        if (otnTopologyShard != null) {
            LOG.info("suppression de otnTopologyShard = {}", otnTopologyShard.toString());
            for (Node otnTopologyNode : otnTopologyShard.getNodes()) {
                LOG.info("deleting node {} in {}", otnTopologyNode.getNodeId().getValue(),
                    NetworkUtils.OTN_NETWORK_ID);
                DataObjectIdentifier<Node> iiotnTopologyNode = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .child(Node.class, otnTopologyNode.key())
                    .build();
                this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiotnTopologyNode);
            }
            for (Link otnTopologyLink : otnTopologyShard.getLinks()) {
                LOG.info("deleting link {} in {}", otnTopologyLink.getLinkId().getValue(),
                    NetworkUtils.OTN_NETWORK_ID);
                DataObjectIdentifier<Link> iiotnTopologyLink = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(Link.class, otnTopologyLink.key())
                    .build();
                this.networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, iiotnTopologyLink);
            }
        }
    }

    private List<Link> getOtnLinks(List<LinkId> linkIds) {
        List<Link> links = new ArrayList<>();
        for (LinkId linkId : linkIds) {
            DataObjectIdentifier<Link> iiLink = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
                .augmentation(Network1.class)
                .child(Link.class, new LinkKey(linkId))
                .build();
            ListenableFuture<Optional<Link>> linkOptLf = networkTransactionService
                .read(LogicalDatastoreType.CONFIGURATION, iiLink);
            if (linkOptLf.isDone()) {
                try {
                    if (linkOptLf.get().isPresent()) {
                        links.add(linkOptLf.get().orElseThrow());
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
        DataObjectIdentifier<TerminationPoint> iiTpA = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)))
            .child(Node.class, new NodeKey(new NodeId(nodeTopoA)))
            .augmentation(Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(new TpId(tpA)))
            .build();
        Optional<TerminationPoint> tpAOpt = Optional.empty();
        DataObjectIdentifier<TerminationPoint> iiTpZ = DataObjectIdentifier.builder(Networks.class)
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
            tps.add(tpAOpt.orElseThrow());
            tps.add(tpZOpt.orElseThrow());
        }
        return tps;
    }

    private List<TerminationPoint> getOtnNodeTps(List<LinkTp> linkTerminations) {
        List<TerminationPoint> tps = new ArrayList<>();
        for (LinkTp linkTp : linkTerminations) {
            String tp = linkTp.getTpId();
            String nodeId = formatNodeName(linkTp.getNodeId(), tp);
            DataObjectIdentifier<TerminationPoint> iiTp = DataObjectIdentifier.builder(Networks.class)
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
                        tps.add(tpOpt.orElseThrow());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Error retreiving tp {} of node {} from otn-topology", tp, nodeId, e);
                }
            } else {
                LOG.error("error getting node termination points from the datastore");
            }
        }
        return tps;
    }

    private void deleteLinks(List<Link> links) {
        for (Link otnTopologyLink : links) {
            LOG.info("deleting link {} from {}", otnTopologyLink.getLinkId().getValue(),
                NetworkUtils.OTN_NETWORK_ID);
            DataObjectIdentifier<Link> iiOtnTopologyLink = DataObjectIdentifier.builder(Networks.class)
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
        DataObjectIdentifier<Network1> iiOtnTopologyLinks = DataObjectIdentifier.builder(Networks.class)
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
        if (netw1Opt.isPresent() && netw1Opt.orElseThrow().getLink() != null) {
            odu4links = netw1Opt
                .orElseThrow()
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
                .equals(nodeId) && lk.getSource().getSourceTp().getValue().equals(tp)).findFirst().orElseThrow();
            if (!links.contains(slink)) {
                links.add(slink);
            }
            Link dlink = odu4links.stream().filter(lk -> lk.getDestination().getDestNode().getValue()
                .equals(nodeId) && lk.getDestination().getDestTp().getValue().equals(tp)).findFirst().orElseThrow();
            if (!links.contains(dlink)) {
                links.add(dlink);
            }
        }
        LOG.debug("odu4oduC4links = {}", links);
        return links;
    }

    private NodeBuilder createTapiNodeBuilder(String supportingLayer, String nodeId) {
        SupportingNodeBuilder supNBd = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(supportingLayer))
            .setNodeRef(new NodeId(nodeId));
        return new NodeBuilder()
            .setNodeId(new NodeId("TAPI-SBI-ABS-NODE"))
            .setSupportingNode(new HashMap<>(
                Map.of(new SupportingNodeKey(supNBd.build().key()), supNBd.build())));

    }

    private String convertNetconfNodeIdToTopoNodeId(String nodeId, String tpId) {
        return new StringBuilder(nodeId).append("-").append(tpId.split("-")[0]).toString();
    }

    private static String formatNodeName(String nodeName, String tpName) {
        return nodeName.contains("-XPDR")
            ? nodeName
            : new StringBuilder(nodeName).append("-").append(tpName.split("-")[0]).toString();
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
