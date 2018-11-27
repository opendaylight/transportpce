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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.DeviceConfig;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.networkmodel.dto.NodeData;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.CpToDegreeBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev170929.degree.node.attributes.AvailableWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev170929.degree.node.attributes.AvailableWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev170929.degree.node.attributes.AvailableWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.CircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.PortConfig;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.network.types.OpenroadmTopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NetworkTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmTopology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology.class);
    private static final int DEFAULT_PORT_DIRECTION = -1;
    private static final int MAX_DEGREE = 20;
    private static final int MAX_SRG = 20;

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final PortMapping portMapping;
    private final DeviceConfig deviceConfig;

    public OpenRoadmTopology(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
        PortMapping portMapping, DeviceConfig deviceConfig) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.portMapping = portMapping;
        this.deviceConfig = deviceConfig;
    }

    /**
     * This public method creates the OpenROADM Topology Layer and posts it to
     * the controller.
     *
     * @param controllerdb
     *            controller databroker
     */
    public void createTopoLayer(DataBroker controllerdb) {
        try {
            Network openRoadmTopology = createOpenRoadmTopology();
            InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));
            WriteTransaction wrtx = controllerdb.newWriteOnlyTransaction();
            wrtx.put(LogicalDatastoreType.CONFIGURATION, nwIID.build(), openRoadmTopology);
            wrtx.submit().get(1, TimeUnit.SECONDS);
            LOG.info("OpenRoadm-Topology created successfully.");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("Failed to create OpenRoadm-Topology", e);
        }
    }

    /**
     * Create empty OpenROADM topology.
     */
    private Network createOpenRoadmTopology() {
        NetworkBuilder nwBuilder = new NetworkBuilder();
        NetworkId nwId = new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID);
        nwBuilder.setNetworkId(nwId);
        nwBuilder.withKey(new NetworkKey(nwId));
        // set network type to Transport Underlay
        NetworkTypes1Builder topoNetworkTypesBldr = new NetworkTypes1Builder();
        topoNetworkTypesBldr.setOpenroadmTopology(new OpenroadmTopologyBuilder().build());
        NetworkTypesBuilder nwTypeBuilder = new NetworkTypesBuilder();
        nwTypeBuilder.addAugmentation(NetworkTypes1.class, topoNetworkTypesBldr.build());
        nwBuilder.setNetworkTypes(nwTypeBuilder.build());
        // Array to store nodes in the topolayer of a roadm/Xponder
        Network1Builder nwBldr1 = new Network1Builder();
        // adding expressLinks
        nwBldr1.setLink(Collections.emptyList());
        nwBuilder.addAugmentation(Network1.class, nwBldr1.build());
        nwBuilder.setNode(Collections.emptyList());
        return nwBuilder.build();
    }

    public TopologyShard createTopologyShard(String nodeId, Info deviceInfo) {
        int numOfDegrees = 0;
        int numOfSrgs = 0;
        List<Node> nodes = new ArrayList<>();
        List<Mapping> localportMapList = new ArrayList<Mapping>();

        // Check if node is ROADM
        if (NodeTypes.Rdm.equals(deviceInfo.getNodeType())) {

            /*
             * Adding Degree Node Get Degree Number -> x then get connection
             * ports then find the port directions to decide whether TX/RX/TXRX
             * Get value for max degree from info subtree, required for
             * iteration if not present assume to be 20 (temporary)
             */

            Integer maxDegree;
            if (deviceInfo.getMaxDegrees() != null) {
                maxDegree = deviceInfo.getMaxDegrees();
            } else {
                maxDegree = MAX_DEGREE;
            }
            Map<String, String> interfaceList = getEthInterfaceList(nodeId);
            interfaceList.forEach((key, val) -> LOG.debug("clé = {}, val = {}", key, val));
            List<CpToDegree> localcpDegreelist = new ArrayList<CpToDegree>();

            // Starting with degree Number = 1
            Integer degreeCounter = 1;
            while (degreeCounter <= maxDegree) {
                LOG.info("creating degree node {}/{}", degreeCounter, maxDegree);
                NodeData nodeData = createDegreeNode(nodeId, degreeCounter, interfaceList);
                if (nodeData != null) {
                    NodeBuilder tempNode = nodeData.getNodeBuilder();
                    nodes.add(tempNode.build());
                    localportMapList.addAll(nodeData.getPortMapList());
                    LOG.debug("mapping added for DEG{}", degreeCounter);
                    localcpDegreelist.addAll(nodeData.getDegreeCpList());
                    LOG.debug("globalDegreeCpList = {}", localcpDegreelist.toString());
                    numOfDegrees++;
                }
                degreeCounter++;
            }
            LOG.info("{} has {} degrees", nodeId, numOfDegrees);

            Integer maxSrg;
            if (deviceInfo.getMaxSrgs() != null) {
                maxSrg = deviceInfo.getMaxSrgs();
            } else {
                maxSrg = MAX_SRG;
            }

            // Starting with srg Number = 1
            Integer srgCounter = 1;
            while (srgCounter <= maxSrg) {
                LOG.info("creating SRG node {}/{}", srgCounter, maxSrg);
                NodeData tempNode = createSrgNode(nodeId, srgCounter);

                if (tempNode != null) {
                    nodes.add(tempNode.getNodeBuilder().build());
                    localportMapList.addAll(tempNode.getPortMapList());
                    numOfSrgs++;
                }
                srgCounter++;
            }
            LOG.info("{} has {} SRGs", nodeId, numOfSrgs);

            LOG.info("adding links numOfDegrees={} numOfSrgs={}", numOfDegrees, numOfSrgs);
            List<Link> links = new ArrayList<>();
            links.addAll(createLinks(nodeId, nodes));
            LOG.info("created nodes/links: {}/{}", nodes.size(), links.size());
            return new TopologyShard(nodes, links, localportMapList, localcpDegreelist);

        } else if (NodeTypes.Xpdr.equals(deviceInfo.getNodeType())) {
            List<ConnectionMap> connectionMapList = this.deviceConfig.getConnectionMap(nodeId);
            connectionMapList.sort(Comparator.comparingDouble(ConnectionMap::getConnectionMapNumber));
            NodeData nodeData = createXpdrNode(nodeId, connectionMapList);
            if (nodeData != null) {
                NodeBuilder tempNode = nodeData.getNodeBuilder();
                nodes.add(tempNode.build());
                localportMapList.addAll(nodeData.getPortMapList());
            }
            List<Link> links = new ArrayList<>();
            return new TopologyShard(nodes, links, localportMapList, null);
        }
        return null;
    }

    /**
     * This method creates on xpdr node inside the openroadm-topology according
     * to the device configuration.
     *
     * @param nodeId
     *            device id
     * @param connectionMapList
     *            list of connection map from device operational datastore in
     *            order to associate a client port to the network port
     *
     * @author Gilles Thouenon (gilles.thouenon@orange.com)
     */
    private NodeData createXpdrNode(String nodeId, List<ConnectionMap> connectionMapList) {
        // Create org-openroadm augmentation node
        Node1Builder node1bldr = new Node1Builder();
        node1bldr.setNodeType(OpenroadmNodeType.XPONDER);

        // Create a generic Topo Layer node
        NodeBuilder nodebldr = createTopoLayerNode(nodeId);
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-XPDR1").toString();
        nodebldr.setNodeId(new NodeId(nodeIdtopo));
        nodebldr.withKey(new NodeKey(new NodeId(nodeIdtopo)));
        nodebldr.addAugmentation(Node1.class, node1bldr.build());

        List<TerminationPoint> tpList = new ArrayList<>();
        List<Mapping> mappingList = new ArrayList<>();

        // to keep memory of cp+port already handled...
        List<String> cpIdList = new ArrayList();
        int portClientIndex = 0;
        int portNetworkIndex = 0;
        String srcLcp = null;
        String destLcp = null;
        Mapping mapping;

        for (ConnectionMap connectionMap : connectionMapList) {
            String srcCpId = connectionMap.getSource().getCircuitPackName() + connectionMap.getSource().getPortName()
                .toString();
            String destCpId;
            TerminationPoint1Builder srcTp1Bldr = new TerminationPoint1Builder();
            TerminationPointBuilder srcTpBldr;
            boolean srcHandling = false;

            for (Destination dest : connectionMap.getDestination()) {
                destCpId = dest.getCircuitPackName() + dest.getPortName().toString();
                TerminationPoint1Builder destTp1Bldr = new TerminationPoint1Builder();
                TerminationPointBuilder destTpBldr;
                boolean destHandling = false;

                // handling of connection map source data
                if (!cpIdList.contains(srcCpId)) {
                    Ports srcPort = this.deviceConfig.getDevicePorts(nodeId, connectionMap.getSource()
                        .getCircuitPackName(), connectionMap.getSource().getPortName().toString());
                    if (srcPort != null && srcPort.getPortQual() != null) {
                        srcHandling = true;
                        switch (srcPort.getPortQual().getIntValue()) {
                            case 4:
                                portClientIndex++;
                                srcLcp = "XPDR1-CLIENT" + portClientIndex;
                                // tp creation
                                srcTp1Bldr.setTpType(OpenroadmTpType.XPONDERCLIENT);
                                break;
                            case 3:
                                portNetworkIndex++;
                                srcLcp = "XPDR1-NETWORK" + portNetworkIndex;
                                // tp creation
                                srcTp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
                                break;
                            default:
                                break;
                        }
                        // mapping creation
                        mapping = portMapping.createMappingObject(nodeId, srcPort, connectionMap.getSource()
                            .getCircuitPackName(), srcLcp);
                        mappingList.add(mapping);
                        cpIdList.add(srcCpId);
                    } else {
                        LOG.error("Error retreiving Port {} of circuit-pack {} from {}", connectionMap.getSource()
                            .getPortName().toString(), connectionMap.getSource().getCircuitPackName(), nodeId);
                    }
                }

                // handling of connection map destination data
                if (!cpIdList.contains(destCpId)) {
                    Ports destPort = this.deviceConfig.getDevicePorts(nodeId, dest.getCircuitPackName(), dest
                        .getPortName().toString());
                    if (destPort != null && destPort.getPortQual() != null) {
                        destHandling = true;
                        switch (destPort.getPortQual().getIntValue()) {
                            case 4:
                                portClientIndex++;
                                destLcp = "XPDR1-CLIENT" + portClientIndex;
                                // tp creation
                                destTp1Bldr.setTpType(OpenroadmTpType.XPONDERCLIENT);
                                break;
                            case 3:
                                portNetworkIndex++;
                                destLcp = "XPDR1-NETWORK" + portNetworkIndex;
                                // tp creation
                                destTp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
                                break;
                            default:
                                break;
                        }
                        // mapping creation
                        mapping = portMapping.createMappingObject(nodeId, destPort, dest.getCircuitPackName(), destLcp);
                        mappingList.add(mapping);
                        cpIdList.add(destCpId);
                    } else {
                        LOG.error("Error retreiving Port {} of circuit-pack {} from {}", connectionMap.getSource()
                            .getPortName().toString(), connectionMap.getSource().getCircuitPackName(), nodeId);
                    }
                }

                // complement TP creations with tail-equipment attributes
                if (srcHandling) {
                    switch (srcTp1Bldr.getTpType().getIntValue()) {
                        case 11:
                            XpdrClientAttributesBuilder clntAttBldr = new XpdrClientAttributesBuilder()
                                .setTailEquipmentId(destLcp);
                            srcTp1Bldr.setXpdrClientAttributes(clntAttBldr.build());
                            break;
                        case 10:
                            XpdrNetworkAttributesBuilder nwtAttBldr = new XpdrNetworkAttributesBuilder()
                                .setTailEquipmentId(destLcp);
                            srcTp1Bldr.setXpdrNetworkAttributes(nwtAttBldr.build());
                            break;
                        default:
                            LOG.error("error setting attributes to TP");
                            break;
                    }
                    srcTpBldr = createTpBldr(srcLcp);
                    srcTpBldr.addAugmentation(TerminationPoint1.class, srcTp1Bldr.build());
                    tpList.add(srcTpBldr.build());
                }
                if (destHandling) {
                    switch (destTp1Bldr.getTpType().getIntValue()) {
                        case 11:
                            XpdrClientAttributesBuilder clntAttBldr = new XpdrClientAttributesBuilder()
                                .setTailEquipmentId(srcLcp);
                            destTp1Bldr.setXpdrClientAttributes(clntAttBldr.build());
                            break;
                        case 10:
                            XpdrNetworkAttributesBuilder nwtAttBldr = new XpdrNetworkAttributesBuilder()
                                .setTailEquipmentId(srcLcp);
                            destTp1Bldr.setXpdrNetworkAttributes(nwtAttBldr.build());
                            break;
                        default:
                            LOG.error("error setting attributes to TP");
                            break;
                    }
                    destTpBldr = createTpBldr(destLcp);
                    destTpBldr.addAugmentation(TerminationPoint1.class, destTp1Bldr.build());
                    tpList.add(destTpBldr.build());
                }

            }
        }
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1Builder tpNode1 =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1Builder();
        tpNode1.setTerminationPoint(tpList);
        nodebldr.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1.class,
            tpNode1.build());

        return new NodeData(nodebldr, mappingList, null);
    }

    /**
     * This method creates a rdm degree node inside the openroadm-topology
     * according to the device configuration.
     *
     * @param nodeId
     *            device id
     * @param degreeCounter
     *            number of the current degree
     * @param interfList
     *            list of CpToDegree to be updated for portmapping
     *
     * @author Gilles Thouenon (gilles.thouenon@orange.com)
     */
    private NodeData createDegreeNode(String nodeId, int degreeCounter, Map<String, String> interfList) {
        // Create openroadm-topology-node augmentation in order to add degree
        Node1Builder node1bldr = new Node1Builder();
        // set node type to degree
        node1bldr.setNodeType(OpenroadmNodeType.DEGREE);

        Degree degree;
        if (this.deviceConfig.getDeviceDegree(nodeId, degreeCounter) == null) {
            return null;
        } else {
            degree = this.deviceConfig.getDeviceDegree(nodeId, degreeCounter);
        }

        // Get connection ports on degree number = degreeCounter in order to get
        // port
        // direction
        List<ConnectionPorts> degreeConPorts;
        if (degree.getConnectionPorts() == null) {
            LOG.info("degree.connectionPort est null");
            return null;
        } else {
            degreeConPorts = degree.getConnectionPorts();
        }
        DegreeAttributesBuilder degAttBldr = new DegreeAttributesBuilder();
        degAttBldr.setDegreeNumber(degreeCounter);
        degAttBldr.setAvailableWavelengths(create96AvalWaveDegree());
        node1bldr.setDegreeAttributes(degAttBldr.build());

        String nodeIdtopo = new StringBuilder(nodeId).append("-DEG").append(degreeCounter).toString();
        // Create a generic Topo Layer node
        NodeBuilder nodebldr = createTopoLayerNode(nodeId);
        nodebldr.setNodeId(new NodeId(nodeIdtopo));
        // Ad degree node specific augmentation
        nodebldr.addAugmentation(Node1.class, node1bldr.build());

        List<TerminationPoint> tpList = new ArrayList<>();
        List<Mapping> mappingList = new ArrayList<>();
        // Get Port direction
        for (ConnectionPorts connectionPorts : degreeConPorts) {
            String circuitPackName = connectionPorts.getCircuitPackName();
            Ports port = this.deviceConfig.getDevicePorts(nodeId, circuitPackName, connectionPorts.getPortName()
                .toString());
            int portDirectionEnum = 0;
            if (port.getPortDirection() != null) {
                portDirectionEnum = port.getPortDirection().getIntValue();
            }

            /*
             * if bi-directional then create 2 tp's :
             *
             * --> TTP-TXRX --> CTP-TXRX
             *
             * if uni-directional :
             *
             * --> TTP-TX --> TTP-RX --> CTP-TX --> CTP-RX
             */
            TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder();
            TerminationPointBuilder tempTpBldr;

            String logicalConnectionPoint;
            Mapping mapping;

            switch (portDirectionEnum) {
                case 1:
                    // ports are uni Directional on a degree, therefore 4
                    // termination
                    // points
                    // Create TTP-TX termination
                    logicalConnectionPoint = "DEG" + degreeCounter + "-TTP-TX";
                    tempTpBldr = createTpBldr(logicalConnectionPoint);
                    tp1Bldr.setTpType(OpenroadmTpType.DEGREETXTTP);
                    tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                    tpList.add(tempTpBldr.build());
                    // creation of the mapping
                    mapping = portMapping.createMappingObject(nodeId, port, circuitPackName, logicalConnectionPoint);
                    mappingList.add(mapping);
                    break;
                case 2:
                    // Create TTP-RX termination
                    logicalConnectionPoint = "DEG" + degreeCounter + "-TTP-RX";
                    tempTpBldr = createTpBldr(logicalConnectionPoint);
                    tp1Bldr.setTpType(OpenroadmTpType.DEGREERXTTP);
                    tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                    tpList.add(tempTpBldr.build());
                    // creation of the mapping
                    mapping = portMapping.createMappingObject(nodeId, port, circuitPackName, logicalConnectionPoint);
                    mappingList.add(mapping);
                    break;
                case 3:
                    // Ports are bi directional therefore 2 termination points
                    // Create TTP-TXRX termination
                    logicalConnectionPoint = "DEG" + degreeCounter + "-TTP-TXRX";
                    tempTpBldr = createTpBldr(logicalConnectionPoint);
                    tp1Bldr.setTpType(OpenroadmTpType.DEGREETXRXTTP);
                    tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                    tpList.add(tempTpBldr.build());
                    // creation of the mapping
                    mapping = portMapping.createMappingObject(nodeId, port, circuitPackName, logicalConnectionPoint);
                    mappingList.add(mapping);
                    break;
                default:
                    LOG.error("Error with port-direction for port {} of circuit pack {}", port.getPortName(),
                        circuitPackName);
                    break;
            }
        }

        // Create a single CTP-TXRX termination for the whole degree node
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder();
        TerminationPointBuilder tempTpBldr = createTpBldr("DEG" + degreeCounter + "-CTP-TXRX");
        tp1Bldr.setTpType(OpenroadmTpType.DEGREETXRXCTP);
        tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
        tpList.add(tempTpBldr.build());

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1Builder tpNode1 =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1Builder();

        tpNode1.setTerminationPoint(tpList);

        nodebldr.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1.class,
            tpNode1.build());

        // complement mapping with cp-to-degree
        List<CpToDegree> cpList = new ArrayList<>();
        if (degree.getCircuitPacks() != null) {
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.CircuitPacks cp : degree
                .getCircuitPacks()) {
                if (interfList.containsKey(cp.getCircuitPackName())) {
                    CpToDegree cpToDeg = new CpToDegreeBuilder()
                        .setCircuitPackName(cp.getCircuitPackName())
                        .setDegreeNumber(Integer.toUnsignedLong(degreeCounter))
                        .setInterfaceName(interfList.get(cp.getCircuitPackName()))
                        .build();
                    cpList.add(cpToDeg);
                }
            }
        }
        return new NodeData(nodebldr, mappingList, cpList);
    }

    /**
     * This method creates a rdm srg node inside the openroadm-topology
     * according to the device configuration.
     *
     * @param nodeId
     *            device id
     * @param srgCounter
     *            number of the current srg
     *
     * @author Gilles Thouenon (gilles.thouenon@orange.com)
     */
    private NodeData createSrgNode(String nodeId, int srgCounter) {
        // Create augmentation node in order to add SRG
        Node1Builder node1bldr = new Node1Builder();
        // set node type to SRG
        node1bldr.setNodeType(OpenroadmNodeType.SRG);

        SrgAttributesBuilder srgAttrBldr = new SrgAttributesBuilder();
        srgAttrBldr.setAvailableWavelengths(create96AvalWaveSrg());
        node1bldr.setSrgAttributes(srgAttrBldr.build());

        // Create a generic Topo Layer node
        NodeBuilder nodebldr = createTopoLayerNode(nodeId);
        nodebldr.addAugmentation(Node1.class, node1bldr.build());

        // Get connection ports on degree number = degreeCounter in order to get
        // port
        // direction
        int maxPpPorts = getMaxPp(nodeId, srgCounter);
        if (maxPpPorts == -1) {
            return null;
        }
        SharedRiskGroup srg = this.deviceConfig.getDeviceSrg(nodeId, srgCounter);
        if (srg == null) {
            LOG.warn("SRG {} does not exist", srgCounter);
            return null;
        }

        String nodeIdtopo = new StringBuilder().append(nodeId).append("-SRG").append(srgCounter).toString();
        nodebldr.setNodeId(new NodeId(nodeIdtopo));

        List<TerminationPoint> tpList = new ArrayList<>();
        List<Mapping> mappingList = new ArrayList<>();

        TerminationPoint1Builder tp1Bldr;
        TerminationPointBuilder tempTpBldr;

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks> srgCpList = srg
            .getCircuitPacks();
        for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.srg.CircuitPacks
                circuitPacks : srgCpList) {
            String circuitPackName = circuitPacks.getCircuitPackName();
            CircuitPack cp = this.deviceConfig.getDeviceCp(nodeId, circuitPackName);
            if (cp.getPorts() == null) {
                continue;
            }

            List<Ports> listPorts = cp.getPorts();
            Collections.sort(listPorts, new SortByName());

            int portIndex = 0;
            for (Ports port : listPorts) {
                if (Port.PortQual.RoadmExternal.equals(port.getPortQual())) {
                    portIndex++;
                    String logicalConnectionPoint;
                    Mapping mapping;
                    switch (port.getPortDirection().getIntValue()) {
                        case 1:
                            logicalConnectionPoint = "SRG" + srgCounter + "-PP" + portIndex + "-TX";
                            tempTpBldr = createTpBldr(logicalConnectionPoint);
                            tp1Bldr = new TerminationPoint1Builder();
                            tp1Bldr.setTpType(OpenroadmTpType.SRGTXPP);
                            tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                            tpList.add(tempTpBldr.build());
                            // creation of the mapping
                            mapping = portMapping.createMappingObject(nodeId, port, circuitPackName,
                                logicalConnectionPoint);
                            mappingList.add(mapping);
                            break;
                        case 2:
                            logicalConnectionPoint = "SRG" + srgCounter + "-PP" + portIndex + "-RX";
                            tempTpBldr = createTpBldr(logicalConnectionPoint);
                            tp1Bldr = new TerminationPoint1Builder();
                            tp1Bldr.setTpType(OpenroadmTpType.SRGRXPP);
                            tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                            tpList.add(tempTpBldr.build());
                            // creation of the mapping
                            mapping = portMapping.createMappingObject(nodeId, port, circuitPackName,
                                logicalConnectionPoint);
                            mappingList.add(mapping);
                            break;
                        case 3:
                            logicalConnectionPoint = "SRG" + srgCounter + "-PP" + portIndex + "-TXRX";
                            tempTpBldr = createTpBldr(logicalConnectionPoint);
                            tp1Bldr = new TerminationPoint1Builder();
                            tp1Bldr.setTpType(OpenroadmTpType.SRGTXRXPP);
                            tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                            tpList.add(tempTpBldr.build());
                            // creation of the mapping
                            mapping = portMapping.createMappingObject(nodeId, port, circuitPackName,
                                logicalConnectionPoint);
                            mappingList.add(mapping);
                            break;
                        default:
                            LOG.error("No correponsding direction to the port {} of circuitpack", port.getPortName(),
                                circuitPackName);
                            break;
                    }
                }
            }
        }

        // Create CP-TXRX termination
        tempTpBldr = createTpBldr("SRG" + srgCounter + "-CP" + "-TXRX");
        tp1Bldr = new TerminationPoint1Builder();
        tp1Bldr.setTpType(OpenroadmTpType.SRGTXRXCP);
        tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
        tpList.add(tempTpBldr.build());

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1Builder tpNode1 =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1Builder();

        tpNode1.setTerminationPoint(tpList);

        nodebldr.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1.class,
            tpNode1.build());

        return new NodeData(nodebldr, mappingList, null);
    }

    private int getMaxPp(String deviceId, Integer srgCounter) {
        int maxPpPorts;
        LOG.info("Getting max pp ports for Srg Number {}", srgCounter);
        InstanceIdentifier<SharedRiskGroup> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(SharedRiskGroup.class, new SharedRiskGroupKey(srgCounter));
        Optional<SharedRiskGroup> ordmSrgObject = this.deviceTransactionManager.getDataFromDevice(deviceId,
            LogicalDatastoreType.OPERATIONAL, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (ordmSrgObject.isPresent()) {
            if (ordmSrgObject.get().getMaxAddDropPorts() != null) {
                maxPpPorts = ordmSrgObject.get().getMaxAddDropPorts();
            } else {
                LOG.info("Max add drop ports absent");
                return -1;
            }
        } else {
            LOG.info("SRG  absent");
            return -1;
        }
        return maxPpPorts;
    }

    private NodeBuilder createTopoLayerNode(String nodeId) {
        // Sets the value of Network-ref and Node-ref as a part of the
        // supporting node
        // attribute
        SupportingNodeBuilder supportbldr = new SupportingNodeBuilder();
        supportbldr.withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID), new NodeId(nodeId)));
        supportbldr.setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID));
        supportbldr.setNodeRef(new NodeId(nodeId));
        ArrayList<SupportingNode> supportlist = new ArrayList<>();
        supportlist.add(supportbldr.build());
        NodeBuilder nodebldr = new NodeBuilder();
        nodebldr.setSupportingNode(supportlist);
        return nodebldr;
    }

    // This method returns a generic termination point builder for a given tpid
    private TerminationPointBuilder createTpBldr(String tpId) {
        TerminationPointBuilder tpBldr = new TerminationPointBuilder();
        TpId tp = new TpId(tpId);
        TerminationPointKey tpKey = new TerminationPointKey(tp);
        tpBldr.withKey(tpKey);
        tpBldr.setTpId(tp);
        return tpBldr;
    }

    // This method returns the linkBuilder object for given source and
    // destination.
    public LinkBuilder createLink(String srcNode, String dstNode, String srcTp, String destTp, boolean opposite) {
        LOG.info("creating link for {}-{}", srcNode, dstNode);
        // Create Destination for link
        DestinationBuilder dstNodeBldr = new DestinationBuilder();
        dstNodeBldr.setDestTp(destTp);
        dstNodeBldr.setDestNode(new NodeId(dstNode));
        // Create Source for the link
        SourceBuilder srcNodeBldr = new SourceBuilder();
        srcNodeBldr.setSourceNode(new NodeId(srcNode));
        srcNodeBldr.setSourceTp(srcTp);
        LinkBuilder lnkBldr = new LinkBuilder();
        // set link builder attribute
        lnkBldr.setDestination(dstNodeBldr.build());
        lnkBldr.setSource(srcNodeBldr.build());
        lnkBldr.setLinkId(LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp));
        lnkBldr.withKey(new LinkKey(lnkBldr.getLinkId()));
        if (opposite) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder lnk1Bldr =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder();
            LinkId oppositeLinkId = LinkIdUtil.getOppositeLinkId(srcNode, srcTp, dstNode, destTp);
            lnk1Bldr.setOppositeLink(oppositeLinkId);
            lnkBldr.addAugmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1.class,
                lnk1Bldr.build());
        }
        return lnkBldr;
    }

    /**
     * This method creates links between openroadm-topology nodes.
     *
     * @param nodeId
     *            device id
     * @param nodes
     *            list of nodes
     *
     * @author Gilles Thouenon (gilles.thouenon@orange.com)
     */
    private List<Link> createLinks(String nodeId, List<Node> nodes) {
        List<Link> links = new ArrayList<>();
        List<Node> listDegeeNodes = nodes.stream().filter(node -> node.getNodeId().toString().contains("DEG")).collect(
            Collectors.toList());
        LOG.info("creating express links for {} between {} degrees", nodeId, listDegeeNodes.size());
        String srcNode;
        String destNode;
        String srcTp;
        String destTp;

        for (int i = 0; i < listDegeeNodes.size(); i++) {
            for (int j = i + 1; j < listDegeeNodes.size(); j++) {
                srcNode = listDegeeNodes.get(i).getNodeId().getValue();
                destNode = listDegeeNodes.get(j).getNodeId().getValue();
                // AtoZ direction
                srcTp = listDegeeNodes.get(i).augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev150608.Node1.class)
                    .getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains("CTP")).findFirst()
                    .get().getTpId().getValue();
                destTp = listDegeeNodes.get(j).augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev150608.Node1.class)
                    .getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains("CTP")).findFirst()
                    .get().getTpId().getValue();

                Link1Builder oorAzlnkBldr = new Link1Builder();
                oorAzlnkBldr.setLinkType(OpenroadmLinkType.EXPRESSLINK);
                LinkBuilder ietfAzlnkBldr = createLink(srcNode, destNode, srcTp, destTp, false);
                ietfAzlnkBldr.addAugmentation(Link1.class, oorAzlnkBldr.build());

                // ZtoA direction
                LinkBuilder ietfZalnkBldr = createLink(destNode, srcNode, destTp, srcTp, false);
                ietfZalnkBldr.addAugmentation(Link1.class, oorAzlnkBldr.build());

                // add opposite link augmentations
                org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder opplnkBldr =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder();
                opplnkBldr.setOppositeLink(ietfZalnkBldr.getLinkId());
                ietfAzlnkBldr.addAugmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1.class, opplnkBldr
                        .build());
                opplnkBldr =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder();
                opplnkBldr.setOppositeLink(ietfAzlnkBldr.getLinkId());
                ietfZalnkBldr.addAugmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1.class, opplnkBldr
                        .build());

                links.add(ietfAzlnkBldr.build());
                links.add(ietfZalnkBldr.build());
            }
        }

        List<Node> listSrgNodes = nodes.stream().filter(node -> node.getNodeId().toString().contains("SRG")).collect(
            Collectors.toList());
        LOG.info("creating add/drop links for {} between {} degrees and {} srgs", nodeId, listDegeeNodes.size(),
            listSrgNodes.size());
        for (int i = 0; i < listDegeeNodes.size(); i++) {
            for (int j = 0; j < listSrgNodes.size(); j++) {
                srcNode = listDegeeNodes.get(i).getNodeId().getValue();
                destNode = listSrgNodes.get(j).getNodeId().getValue();

                // drop links
                srcTp = listDegeeNodes.get(i).augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev150608.Node1.class)
                    .getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains("CTP")).findFirst()
                    .get().getTpId().getValue();
                destTp = listSrgNodes.get(j).augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev150608.Node1.class)
                    .getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains("CP")).findFirst()
                    .get().getTpId().getValue();

                LinkBuilder ietfDropLinkBldr = createLink(srcNode, destNode, srcTp, destTp, false);
                Link1Builder oorlnk1Bldr = new Link1Builder();
                oorlnk1Bldr.setLinkType(OpenroadmLinkType.DROPLINK);
                ietfDropLinkBldr.addAugmentation(Link1.class, oorlnk1Bldr.build());

                // add links direction
                LinkBuilder ietfaddLinkBldr = createLink(destNode, srcNode, destTp, srcTp, false);
                oorlnk1Bldr = new Link1Builder();
                oorlnk1Bldr.setLinkType(OpenroadmLinkType.ADDLINK);
                ietfaddLinkBldr.addAugmentation(Link1.class, oorlnk1Bldr.build());

                // add opposite link augmentations
                org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder opplnkBldr =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder();
                opplnkBldr.setOppositeLink(ietfaddLinkBldr.getLinkId());
                ietfDropLinkBldr.addAugmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1.class, opplnkBldr
                        .build());
                opplnkBldr =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1Builder();
                opplnkBldr.setOppositeLink(ietfDropLinkBldr.getLinkId());
                ietfaddLinkBldr.addAugmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.opposite.links.rev170929.Link1.class, opplnkBldr
                        .build());

                links.add(ietfDropLinkBldr.build());
                links.add(ietfaddLinkBldr.build());
            }
        }
        return links;
    }

    // This method returns the linkBuilder object for given source and
    // destination.
    public boolean deleteLink(String srcNode, String dstNode, Integer srcDegId,
        Integer destDegId, String srcTp, String destTp) {
        LOG.info("deleting link for {}-{}", srcNode, dstNode);
        try {
            LinkId linkId = LinkIdUtil.buildLinkId(srcNode + "-DEG" + srcDegId,
                srcTp, dstNode + "-DEG" + destDegId, destTp);
            LOG.info("Link is for the link is {}", linkId.getValue());
            InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));
            WriteTransaction wrtx = this.dataBroker.newWriteOnlyTransaction();
            wrtx.delete(LogicalDatastoreType.CONFIGURATION, linkIID.build());
            LOG.info("Deleted");
            wrtx.submit().get(1, TimeUnit.SECONDS);
            LOG.info("Submitted");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private List<AvailableWavelengths> create96AvalWaveDegree() {
        List<AvailableWavelengths> waveList = new ArrayList<>();

        for (int i = 1; i < 97; i++) {
            AvailableWavelengthsBuilder avalBldr = new AvailableWavelengthsBuilder();
            avalBldr.setIndex((long) i);
            avalBldr.withKey(new AvailableWavelengthsKey((long) i));
            waveList.add(avalBldr.build());
        }

        return waveList;
    }

    private List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev170929.srg.node.attributes
        .AvailableWavelengths> create96AvalWaveSrg() {

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev170929.srg.node.attributes
            .AvailableWavelengths> waveList = new ArrayList<>();

        for (int i = 1; i < 97; i++) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev170929.srg.node.attributes
                .AvailableWavelengthsBuilder avalBldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.srg
                .rev170929.srg.node.attributes.AvailableWavelengthsBuilder();
            avalBldr.setIndex((long) i);
            avalBldr.withKey(new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev170929.srg.node.attributes
                .AvailableWavelengthsKey((long) i));
            waveList.add(avalBldr.build());
        }

        return waveList;
    }

    // method copied/pasted from PortMappingImpl.java
    private Map<String, String> getEthInterfaceList(String nodeId) {
        Map<String, String> cpToInterfaceMap = new HashMap<>();
        InstanceIdentifier<Protocols> protocolsIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(Protocols.class);
        Optional<Protocols> protocolObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.OPERATIONAL, protocolsIID, Timeouts.DEVICE_READ_TIMEOUT,
            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!protocolObject.isPresent() || (protocolObject.get().augmentation(Protocols1.class) == null)) {
            LOG.warn("LLDP subtree is missing : isolated openroadm device");
            return cpToInterfaceMap;
        }
        List<PortConfig> portConfigList = protocolObject.get().augmentation(Protocols1.class).getLldp().getPortConfig();
        if (!portConfigList.isEmpty()) {
            for (PortConfig portConfig : portConfigList) {
                if (portConfig.getAdminStatus().equals(PortConfig.AdminStatus.Txandrx)) {
                    InstanceIdentifier<Interface> interfaceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(Interface.class, new InterfaceKey(portConfig.getIfName()));
                    Optional<Interface> interfaceObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                        LogicalDatastoreType.OPERATIONAL, interfaceIID, Timeouts.DEVICE_READ_TIMEOUT,
                        Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                    if (interfaceObject.isPresent()
                        && (interfaceObject.get().getSupportingCircuitPackName() != null)) {
                        String supportingCircuitPackName = interfaceObject.get().getSupportingCircuitPackName();
                        cpToInterfaceMap.put(supportingCircuitPackName, portConfig.getIfName());
                        InstanceIdentifier<CircuitPacks> circuitPacksIID = InstanceIdentifier
                            .create(OrgOpenroadmDevice.class).child(CircuitPacks.class,
                                new CircuitPacksKey(supportingCircuitPackName));
                        Optional<CircuitPacks> circuitPackObject = this.deviceTransactionManager.getDataFromDevice(
                            nodeId, LogicalDatastoreType.OPERATIONAL, circuitPacksIID, Timeouts.DEVICE_READ_TIMEOUT,
                            Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                        // if (circuitPackObject.isPresent()
                        // && (circuitPackObject.get().getParentCircuitPack() !=
                        // null)) {
                        // cpToInterfaceMap.put(circuitPackObject.get().getParentCircuitPack()
                        // .getCircuitPackName(), portConfig.getIfName());
                        // }
                    }
                } else {
                    LOG.warn("PortConfig Admin Status is not equal Txandrx");
                }
            }
        } else {
            LOG.warn("Couldnt find port config under LLDP for Node : {}", nodeId);
        }
        return cpToInterfaceMap;
    }

}
