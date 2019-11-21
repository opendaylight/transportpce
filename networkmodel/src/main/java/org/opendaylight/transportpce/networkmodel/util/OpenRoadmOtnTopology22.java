/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev191115.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev191115.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.networks.network.network.types.OpenroadmCommonNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Xponder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.xponder.XpdrPort;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.SwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev181130.SwitchingPoolTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NetworkTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmOtnTopology22 {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology22.class);

    private NetworkTransactionService networkTransactionService;
    private final DeviceTransactionManager deviceTransactionManager;

    public OpenRoadmOtnTopology22(NetworkTransactionService networkTransactionService,
                               DeviceTransactionManager deviceTransactionManager) {
        this.networkTransactionService = networkTransactionService;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    /**
     * This public method creates the OTN OpenROADM Topology
     * Layer and posts it to the controller.
     */
    public void createTopoLayer() {
        try {
            Network openRoadmOtnTopology = createOpenRoadmOtnTopology();
            InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Networks.class).child(Network.class,
                    new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID)));

            this.networkTransactionService.put(LogicalDatastoreType.CONFIGURATION, nwIID.build(), openRoadmOtnTopology);
            this.networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            LOG.info("OpenRoadm-OTN-Topology created successfully.");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("Failed to create OpenRoadm-OTN-Topology", e);
        }
    }

    /**
     * Create empty OpenROADM topology.
     */
    private Network createOpenRoadmOtnTopology() {
        NetworkId nwId = new NetworkId(NetworkUtils.OTN_NETWORK_ID);
        NetworkTypes1Builder topoNetworkTypesBldr = new NetworkTypes1Builder()
                .setOpenroadmCommonNetwork(new OpenroadmCommonNetworkBuilder().build());
        NetworkTypesBuilder nwTypeBuilder = new NetworkTypesBuilder()
                .addAugmentation(NetworkTypes1.class, topoNetworkTypesBldr.build());
        // Array to store nodes in the topolayer of a roadm/Xponder
        Network1Builder nwBldr1 = new Network1Builder()
                // adding expressLinks
                .setLink(Collections.emptyList());
        NetworkBuilder nwBuilder = new NetworkBuilder()
                .setNetworkId(nwId)
                .withKey(new NetworkKey(nwId))
                .setNetworkTypes(nwTypeBuilder.build())
                .addAugmentation(Network1.class, nwBldr1.build())
                .setNode(Collections.emptyList());
        return nwBuilder.build();
    }

    public TopologyShard createTopologyShard(Nodes mappingNode) {
        List<Node> nodes = new ArrayList<>();

        LOG.info("Topology create request received for Node {}",mappingNode.getNodeId());
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        //  .child(Xponder.class,new XponderKey(2));
        Optional<OrgOpenroadmDevice> deviceOptional = deviceTransactionManager
            .getDataFromDevice(mappingNode.getNodeId(),LogicalDatastoreType.OPERATIONAL, deviceIID,
            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        OrgOpenroadmDevice device;
        List<Xponder> xponders;
        if (deviceOptional.isPresent()) {
            device = deviceOptional.get();
            xponders = device.getXponder();
        } else {
            LOG.error("Unable to get xponder for the device {}", mappingNode.getNodeId());
            return null;
        }
        LOG.info("Xponder subtree is found and now calling create XPDR");
        if (NodeTypes.Xpdr.getIntValue() ==  mappingNode.getNodeInfo().getNodeType().getIntValue()) {
            // Check if node is XPONDER
            LOG.info("creating xpdr node in openroadmotntopology for node {}", mappingNode.getNodeId());
            if (xponders != null) {
                for (Xponder xponder: xponders) {
                    LOG.info("Calling create XPDR for xponder number {}",xponder.getXpdrNumber());
                    NodeBuilder ietfNode = createXpdr(xponder,mappingNode);
                    nodes.add(ietfNode.build());
                }
            }
            LOG.info("Coming to return topology");
            return new TopologyShard(nodes, null);
        }
        LOG.error("Device node Type not managed yet");
        return null;
    }

    private NodeBuilder createXpdr(Xponder xponder,Nodes mappingNode) {
        // set node-id
        String nodeIdtopo = new StringBuilder().append(mappingNode.getNodeId()).append("-XPDR" + xponder
            .getXpdrNumber()).toString();
        LOG.info("Node is {}",nodeIdtopo);

        // Create ietf node setting supporting-node data
        NodeBuilder ietfNodeBldr = createOtnTopoLayerNode(mappingNode.getNodeId())
            .setNodeId(new NodeId(nodeIdtopo))
            .withKey((new NodeKey(new NodeId(nodeIdtopo))));
        // Create openroadm-network-topo augmentation to set node type to Xponder
        Node1Builder ontNode1Bldr = new Node1Builder()
            .setNodeType(OpenroadmNodeType.XPONDER);
        if (xponder.getXpdrType().equals(XpdrNodeTypes.Switch)) {
            LOG.info("Xponder type is OTN switch and it is calling switching pool");
            SwitchingPoolsBuilder switchingPoolsBuilder = new SwitchingPoolsBuilder()
                .setOduSwitchingPools(getSwitchingPools(mappingNode.getNodeId(), mappingNode));
            ontNode1Bldr.setSwitchingPools(switchingPoolsBuilder.build());
            LOG.info("Switching pool object is created {}",
                switchingPoolsBuilder.build().getOduSwitchingPools().size());
        }
        ietfNodeBldr.addAugmentation(Node1.class, ontNode1Bldr.build());

        // Create tp-list
        LOG.info("Now createing TP list");
        List<TerminationPoint> tpList = new ArrayList<>();
        TerminationPointBuilder ietfTpBldr;

        for (XpdrPort xponderPort: xponder.getXpdrPort()) {
            Mapping ma = mappingNode.getMapping().stream().filter(x -> x.getSupportingCircuitPackName()
                    .equals(xponderPort.getCircuitPackName()) && x.getSupportingPort()
                    .equals(xponderPort.getPortName())).collect(Collectors.toList()).get(0);
            ietfTpBldr = createTpBldr(ma.getLogicalConnectionPoint());
            TerminationPoint1Builder ontTp1Bldr = new TerminationPoint1Builder();
            if (ma.getPortQual().equals("xpdr-network") || ma.getPortQual().equals("switch-network")) {
                ontTp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
                XpdrTpPortConnectionAttributesBuilder xpdrTpBuilder = new XpdrTpPortConnectionAttributesBuilder();
                if (ma.getPortQual().equals("switch-network") && ma.getRate() != null) {
                    if (ma.getRate().equals("10G")) {
                        xpdrTpBuilder.setRate(ODU2e.class);
                    }
                    else if (ma.getRate().equals("1G")) {
                        xpdrTpBuilder.setRate(ODU0.class);
                    }
                    else if (ma.getRate().equals("100G")) {
                        xpdrTpBuilder.setRate(ODU4.class);
                        List<Integer> tpSlots = new ArrayList<Integer>();
                        IntStream.range(1, 81).forEach(nbr -> tpSlots.add(nbr));
                        xpdrTpBuilder.setTsPool(tpSlots);
                    }
                } else {
                    LOG.warn("no rate in portmapping for lcp {} of {}", ma.getLogicalConnectionPoint(),
                        mappingNode.getNodeId());
                }
                xpdrTpBuilder.setTailEquipmentId(ma.getAssociatedLcp());
                ontTp1Bldr.setXpdrTpPortConnectionAttributes(xpdrTpBuilder.build());
                ietfTpBldr.addAugmentation(TerminationPoint1.class, ontTp1Bldr.build());
                tpList.add(ietfTpBldr.build());
            } else if (ma.getPortQual().equals("xpdr-client") || ma.getPortQual().equals("switch-client")) {
                ontTp1Bldr.setTpType(OpenroadmTpType.XPONDERCLIENT);
                XpdrTpPortConnectionAttributesBuilder xpdrTpBuilder = new XpdrTpPortConnectionAttributesBuilder();
                if (ma.getPortQual().equals("switch-client") && ma.getRate() != null) {
                    if (ma.getRate().equals("10G")) {
                        xpdrTpBuilder.setRate(ODU2e.class);
                    }
                    else if (ma.getRate().equals("1G")) {
                        xpdrTpBuilder.setRate(ODU0.class);
                    }
                    else if (ma.getRate().equals("100G")) {
                        xpdrTpBuilder.setRate(ODU4.class);
                    }
                } else {
                    LOG.warn("no rate in portmapping for lcp {} of {}", ma.getLogicalConnectionPoint(),
                        mappingNode.getNodeId());
                }
                xpdrTpBuilder.setTailEquipmentId(ma.getAssociatedLcp());
                ontTp1Bldr.setXpdrTpPortConnectionAttributes(xpdrTpBuilder.build());
                ietfTpBldr.addAugmentation(TerminationPoint1.class, ontTp1Bldr.build());
                tpList.add(ietfTpBldr.build());
            }
        }
        for (int i = 0;i < tpList.size();i++) {
            LOG.info("Tps are {},{}",tpList.get(i).getTpId());
        }
        // Create ietf node augmentation to support ietf tp-list
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder tpNode1 =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder();
        tpNode1.setTerminationPoint(tpList);
        ietfNodeBldr.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
            tpNode1.build());

        /**Node1Builder ietfNode1 = new Node1Builder();
        ietfNodeBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                ietfNode1.build());**/
        LOG.info("Now all TPS and network node is ready and calling return");
        return ietfNodeBldr;
    }

    private NodeBuilder createOtnTopoLayerNode(String nodeId) {
        // Sets the value of Network-ref and Node-ref as a part of the supporting node
        // attribute
        LOG.info("Settting up supporting node and interface");
        SupportingNodeBuilder supportbldr = new SupportingNodeBuilder()
            .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID), new NodeId(nodeId)))
            .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .setNodeRef(new NodeId(nodeId));
        ArrayList<SupportingNode> supportlist = new ArrayList<>();
        supportlist.add(supportbldr.build());
        NodeBuilder nodebldr = new NodeBuilder()
            .setSupportingNode(supportlist);
        LOG.info("Returning node builder");
        return nodebldr;
    }

    // This method returns a generic termination point builder for a given tpid
    private TerminationPointBuilder createTpBldr(String tpId) {
        return new TerminationPointBuilder()
            .withKey(new TerminationPointKey(new TpId(tpId)))
            .setTpId(new TpId(tpId));
    }

    private LinkBuilder createLink(String srcNode, String destNode, String srcTp, String destTp) {
        //create source link
        SourceBuilder ietfSrcLinkBldr = new SourceBuilder()
            .setSourceNode(new NodeId(srcNode))
            .setSourceTp(srcTp);
        //create destination link
        DestinationBuilder ietfDestLinkBldr = new DestinationBuilder()
            .setDestNode(new NodeId(destNode))
            .setDestTp(destTp);

        LinkBuilder ietfLinkBldr = new LinkBuilder()
            .setSource(ietfSrcLinkBldr.build())
            .setDestination(ietfDestLinkBldr.build())
            .setLinkId(LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp));
        return ietfLinkBldr.withKey(new LinkKey(ietfLinkBldr.getLinkId()));
    }

    private List<OduSwitchingPools> getSwitchingPools(String nodeId,Nodes mappingNode) {
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        //  .child(Xponder.class,new XponderKey(2));
        Optional<OrgOpenroadmDevice> deviceOptional = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        OrgOpenroadmDevice device;
        List<OduSwitchingPools> oduSwitchingPools = new ArrayList<>();
        if (deviceOptional.isPresent()) {
            device = deviceOptional.get();
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org
                .openroadm.device.OduSwitchingPools odupool:device.getOduSwitchingPools()) {
                OduSwitchingPoolsBuilder oduSwitchingPoolsBuilder = new OduSwitchingPoolsBuilder();
                List<NonBlockingList> nonBlockingLists = new ArrayList<>();
                for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org
                    .openroadm.device.odu.switching.pools.NonBlockingList nbList: odupool.getNonBlockingList()) {
                    NonBlockingListBuilder nonBlockingListBuilder = new NonBlockingListBuilder()
                        .setNblNumber(nbList.getNblNumber())
                        .setAvailableInterconnectBandwidth(nbList.getInterconnectBandwidth())
                        .setInterconnectBandwidthUnit(nbList.getInterconnectBandwidth());
                    List<TpId> tpList = new ArrayList<>();
                    if (nbList.getPortList() != null) {
                        for (PortList portList:nbList.getPortList()) {
                            LOG.info("Inside switching pool now collecting ports {},{},{}", portList.getPortName(),
                                portList.getCircuitPackName(), nbList.getNblNumber());
                            Mapping mapping = mappingNode.getMapping().stream()
                                .filter(x -> x.getSupportingCircuitPackName().equals(portList.getCircuitPackName())
                                && x.getSupportingPort().equals(portList.getPortName()))
                                .collect(Collectors.toList()).get(0);
                            LOG.info("Mapping TP is coming is {}",mapping);
                            tpList.add(new TpId(mapping.getLogicalConnectionPoint()));
                        }
                        nonBlockingListBuilder.setTpList(tpList);
                        nonBlockingLists.add(nonBlockingListBuilder.build());
                    } else {
                        LOG.warn("no portList for non-blocking-list {} of {}", nbList.getNblNumber(), nodeId);
                    }
                }
                oduSwitchingPoolsBuilder.setNonBlockingList(nonBlockingLists)
                    .setSwitchingPoolNumber(odupool.getSwitchingPoolNumber())
                    .setSwitchingPoolType(SwitchingPoolTypes.forValue(odupool.getSwitchingPoolType().getIntValue()));
                oduSwitchingPools.add(oduSwitchingPoolsBuilder.build());
            }
            return oduSwitchingPools;
        } else {
            LOG.error("Unable to get xponder for the device {}", nodeId);
            return null;
        }
    }

    // This method returns the linkBuilder object for given source and destination
    public static boolean deleteLink(String srcNode, String dstNode, String srcTp, String destTp,
                                     NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for {}-{}", srcNode, dstNode);
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);
        if (deleteLinkLinkId(linkId, networkTransactionService)) {
            LOG.debug("Link Id {} updated to have admin state down");
            return true;
        } else {
            LOG.debug("Link Id not found for Source {} and Dest {}", srcNode, dstNode);
            return false;
        }
    }

    // This method returns the linkBuilder object for given source and destination
    public static boolean deleteLinkLinkId(LinkId linkId , NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for LinkId: {}", linkId);
        try {
            InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class).child(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OTN_NETWORK_ID))).augmentation(Network1.class)
                .child(Link.class, new LinkKey(linkId));
            java.util.Optional<Link> link =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID.build()).get();
            if (link.isPresent()) {
                Link1Builder link1Builder = new Link1Builder().setAdministrativeState(State.OutOfService);
                LinkBuilder linkBuilder = new LinkBuilder(link.get())
                    .removeAugmentation(Link1.class)
                    .addAugmentation(Link1.class,link1Builder.build());
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(),
                    linkBuilder.build());
                networkTransactionService.commit().get(1, TimeUnit.SECONDS);
                return true;
            } else {
                LOG.error("No link found for given LinkId: {}", linkId);
                return false;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }
}
