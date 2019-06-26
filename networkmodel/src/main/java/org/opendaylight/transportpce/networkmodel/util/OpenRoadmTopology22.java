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

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.dto.NodeData;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.networks.network.network.types.OpenroadmCommonNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmTopology22 {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology22.class);
    private static final int DEFAULT_PORT_DIRECTION = -1;
    private static final int MAX_DEGREE = 20;
    private static final int MAX_SRG = 20;

    private NetworkTransactionService networkTransactionService;
    private final DeviceTransactionManager deviceTransactionManager;

    public OpenRoadmTopology22(NetworkTransactionService networkTransactionService,
                               DeviceTransactionManager deviceTransactionManager) {
        this.networkTransactionService = networkTransactionService;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    /**
     * This public method creates the OpenROADM Topology
     * Layer and posts it to the controller.
     */
    public void createTopoLayer() {
        try {
            Network openRoadmTopology = createOpenRoadmTopology();
            InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Networks.class).child(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)));

            this.networkTransactionService.put(LogicalDatastoreType.CONFIGURATION, nwIID.build(), openRoadmTopology);
            this.networkTransactionService.submit().get(1, TimeUnit.SECONDS);
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
        NetworkTypes1Builder topoNetworkTypesBldr = new NetworkTypes1Builder();
        topoNetworkTypesBldr.setOpenroadmCommonNetwork(new OpenroadmCommonNetworkBuilder().build());
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

    public TopologyShard createTopologyShard(Nodes mappingNode) {
        int numOfDegrees;
        int numOfSrgs;
        int portDirectionEnum = DEFAULT_PORT_DIRECTION;
        String nodeId = mappingNode.getNodeId();

        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        java.util.Optional<Info> deviceInfoOpt =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, infoIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        Info deviceInfo;
        if (deviceInfoOpt.isPresent()) {
            deviceInfo = deviceInfoOpt.get();
        } else {
            LOG.error("Unable to get device info for device {}!", nodeId);
            return null;
        }
        List<Node> nodes = new ArrayList<>();
        // Check if node is ROADM
        if (NodeTypes.Rdm.getName().equals(mappingNode.getNodeType().getName())) {

            /*
             * Adding Degree Node Get Degree Number -> x then get connection ports then find the port directions
             * to decide whether TX/RX/TXRX Get value for max degree from info subtree, required for iteration
             * if not present assume to be 20 (temporary)
             */

            Integer maxDegree;
            if (deviceInfo.getMaxDegrees() != null) {
                maxDegree = deviceInfo.getMaxDegrees();
            } else {
                maxDegree = MAX_DEGREE;
            }

            // Starting with degree Number = 1
            Integer degreeCounter = 1;

            while (degreeCounter <= maxDegree) {
                LOG.info("creating degree node {}/{}", degreeCounter, maxDegree);
                NodeData nodeData = createDegreeNode(nodeId, degreeCounter);
                if (nodeData != null) {
                    NodeBuilder tempNode = nodeData.getNodeBuilder();
                    portDirectionEnum = nodeData.getPortDirectionEnum();
                    nodes.add(tempNode.build());
                    degreeCounter++;
                }
                // null returned if Degree number= degreeCounter not present in the device
                else {
                    break;
                }
            }
            numOfDegrees = degreeCounter - 1;

            Integer maxSrg;
            if (deviceInfo.getMaxSrgs() != null) {
                maxSrg = deviceInfo.getMaxSrgs();
            } else {
                maxSrg = MAX_SRG;
            }

            // Starting with degree Number = 1
            Integer srgCounter = 1;

            while (srgCounter <= maxSrg) {
                LOG.info("creating SRG node {}/{}", srgCounter, maxSrg);
                NodeBuilder tempNode = createSrgNode(nodeId, srgCounter, portDirectionEnum);

                if (tempNode != null) {
                    nodes.add(tempNode.build());
                    srgCounter++;
                } else {
                    // null returned if Degree number= degreeCounter not present in the device
                    break;
                }
            }
            numOfSrgs = srgCounter - 1;


            LOG.info("adding links numOfDegrees={} numOfSrgs={}", numOfDegrees, numOfSrgs);
            List<Link> links = new ArrayList<>();
            links.addAll(createExpressLinks(nodeId, numOfDegrees, portDirectionEnum));
            links.addAll(createAddDropLinks(nodeId, numOfDegrees, numOfSrgs, portDirectionEnum));
            LOG.info("created nodes/links: {}/{}", nodes.size(), links.size());
            return new TopologyShard(nodes, links);
        } else if (NodeTypes.Xpdr.getName().equals(mappingNode.getNodeType().getName())) {
            // Check if node is XPONDER
            LOG.info("creating xpdr node in openroadmtopology for node {}", mappingNode.getNodeId());
            NodeBuilder ietfNode = createXpdr(mappingNode);
            nodes.add(ietfNode.build());
            List<Link> links = new ArrayList<>();
            return new TopologyShard(nodes, links);
        }
        LOG.error("Device node Type not managed yet");
        return null;
    }

    /**
     * This private method gets the list of circuit packs on a xponder. For each circuit pack on a
     * Xponder, it does a get on circuit-pack subtree with circuit-pack-name as key in order to get the
     * list of ports. It then iterates over the list of ports to get ports with port-qual as
     * xpdr-network/xpdr-client. The line and client ports are saved as:
     *
     * <p>
     * 1. LINEn
     *
     * <p>
     * 2. CLNTn
     */
    private int getNoOfClientPorts(String deviceId) {
        // Creating for Xponder Line and Client Ports
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.CONFIGURATION, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        // Variable to keep track of number of client ports
        int client = 1;
        if (deviceObject.isPresent()) {
            for (CircuitPacks cp : deviceObject.get().getCircuitPacks()) {
                if (cp.getPorts() != null) {
                    for (Ports port : cp.getPorts()) {
                        if ((port.getPortQual() != null) && (port.getPortQual().getIntValue() == 4)) {
                            client++;
                        }
                    }
                }
            }
        } else {
            return 0;
        }
        return client;
    }

    private NodeBuilder createXpdr(Nodes mappingNode) {
        // Create ietf node setting supporting-node data
        NodeBuilder ietfNodeBldr = createTopoLayerNode(mappingNode.getNodeId());
        // set node-id
        String nodeIdtopo = new StringBuilder().append(mappingNode.getNodeId()).append("-XPDR1").toString();
        ietfNodeBldr.setNodeId(new NodeId(nodeIdtopo));
        ietfNodeBldr.withKey((new NodeKey(new NodeId(nodeIdtopo))));
        // Create openroadm-network-topo augmentation to set node type to Xponder
        Node1Builder ontNode1Bldr = new Node1Builder();
        ontNode1Bldr.setNodeType(OpenroadmNodeType.XPONDER);
        ietfNodeBldr.addAugmentation(Node1.class, ontNode1Bldr.build());

        // Create tp-list
        List<TerminationPoint> tpList = new ArrayList<>();
        TerminationPointBuilder ietfTpBldr;
        for (Mapping m : mappingNode.getMapping()) {
            ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint());
            // Add openroadm-network-topology tp augmentations
            TerminationPoint1Builder ontTp1Bldr = new TerminationPoint1Builder();
            if (m.getPortQual().equals("xpdr-network")) {
                ontTp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
                XpdrNetworkAttributesBuilder xpdrNwAttrBldr = new XpdrNetworkAttributesBuilder();
                xpdrNwAttrBldr.setTailEquipmentId(m.getAssociatedLcp());
                ontTp1Bldr.setXpdrNetworkAttributes(xpdrNwAttrBldr.build());
                ietfTpBldr.addAugmentation(TerminationPoint1.class, ontTp1Bldr.build());
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625
                    .TerminationPoint1Builder tpceTp1Bldr = new org.opendaylight.yang.gen.v1.http.transportpce
                    .topology.rev190625.TerminationPoint1Builder();
                tpceTp1Bldr.setAssociatedConnectionMapPort(m.getAssociatedLcp());
                ietfTpBldr.addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625
                    .TerminationPoint1.class, tpceTp1Bldr.build());
                tpList.add(ietfTpBldr.build());
            } else if (m.getPortQual().equals("xpdr-client")) {
                ontTp1Bldr.setTpType(OpenroadmTpType.XPONDERCLIENT);
                XpdrClientAttributesBuilder xpdrNwAttrBldr = new XpdrClientAttributesBuilder();
                xpdrNwAttrBldr.setTailEquipmentId(m.getAssociatedLcp());
                ontTp1Bldr.setXpdrClientAttributes(xpdrNwAttrBldr.build());
                ietfTpBldr.addAugmentation(TerminationPoint1.class, ontTp1Bldr.build());
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625
                    .TerminationPoint1Builder tpceTp1Bldr = new org.opendaylight.yang.gen.v1.http.transportpce
                    .topology.rev190625.TerminationPoint1Builder();
                tpceTp1Bldr.setAssociatedConnectionMapPort(m.getAssociatedLcp());
                ietfTpBldr.addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625
                    .TerminationPoint1.class, tpceTp1Bldr.build());
                tpList.add(ietfTpBldr.build());
            }
        }

        // Create ietf node augmentation to support ietf tp-list
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .Node1Builder ietfNode1 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
            .rev180226.Node1Builder();
        ietfNode1.setTerminationPoint(tpList);
        ietfNodeBldr.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
            ietfNode1.build());
        return ietfNodeBldr;
    }


    private NodeData createDegreeNode(String nodeId, int degreeCounter) {
        // Create augmentation node to inorder to add degree
        Node1Builder node1bldr = new Node1Builder();
        // set node type to degree
        node1bldr.setNodeType(OpenroadmNodeType.DEGREE);

        // Get connection ports on degree number = degreeCounter in order to get port
        // direction
        List<ConnectionPorts> degreeConPorts = getDegreePorts(nodeId, degreeCounter);
        if (degreeConPorts == null || degreeConPorts.isEmpty()) {
            return null;
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
        // Get Port direction
        int portDirectionEnum = getPortDirection(nodeId, degreeConPorts.get(0).getCircuitPackName(),
                degreeConPorts.get(0).getPortName().toString());

        /*
         * if bi-directional then create 2 tp's :
         *
         * --> TTP-TXRX --> CTP-TXRX
         *
         * if uni-directional :
         *
         *     --> TTP-TX
         *     --> TTP-RX
         *     --> CTP-TX
         *     --> CTP-RX
         */
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder();
        TerminationPointBuilder tempTpBldr;

        List<TerminationPoint> tpList = new ArrayList<>();
        if (portDirectionEnum == 1 || portDirectionEnum == 2) {
            // ports are uni Directional on a degree, therefore 4 termination points
            // Create TTP-TX termination

            tempTpBldr = createTpBldr("DEG" + degreeCounter + "-TTP-TX");
            tp1Bldr.setTpType(OpenroadmTpType.DEGREETXTTP);
            tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
            tpList.add(tempTpBldr.build());

            // Create TTP-RX termination
            tp1Bldr = new TerminationPoint1Builder();
            tempTpBldr = createTpBldr("DEG" + degreeCounter + "-TTP-RX");
            tp1Bldr.setTpType(OpenroadmTpType.DEGREERXTTP);

            tempTpBldr.addAugmentation(TerminationPoint1.class,tp1Bldr.build());
            tpList.add(tempTpBldr.build());

            // Create CTP-TX termination
            tp1Bldr = new TerminationPoint1Builder();
            tempTpBldr = createTpBldr("DEG" + degreeCounter + "-CTP-TX");
            tp1Bldr.setTpType(OpenroadmTpType.DEGREETXCTP);
            tempTpBldr.addAugmentation(TerminationPoint1.class,tp1Bldr.build());
            tpList.add(tempTpBldr.build());

            // Create CTP-RX termination
            tp1Bldr = new TerminationPoint1Builder();
            tempTpBldr = createTpBldr("DEG" + degreeCounter + "-CTP-RX");
            tp1Bldr.setTpType(OpenroadmTpType.DEGREERXCTP);
            tempTpBldr.addAugmentation(TerminationPoint1.class,tp1Bldr.build());
            tpList.add(tempTpBldr.build());

        } else if (portDirectionEnum == 3) {
            // Ports are bi directional therefore 2 termination points
            // Create TTP-TXRX termination
            tp1Bldr = new TerminationPoint1Builder();
            tempTpBldr = createTpBldr("DEG" + degreeCounter + "-TTP-TXRX");
            tp1Bldr.setTpType(OpenroadmTpType.DEGREETXRXTTP);
            tempTpBldr.addAugmentation(TerminationPoint1.class,tp1Bldr.build());
            tpList.add(tempTpBldr.build());

            // Create CTP-TXRX termination
            tp1Bldr = new TerminationPoint1Builder();
            tempTpBldr = createTpBldr("DEG" + degreeCounter + "-CTP-TXRX");
            tp1Bldr.setTpType(OpenroadmTpType.DEGREETXRXCTP);
            tempTpBldr.addAugmentation(TerminationPoint1.class,tp1Bldr.build());
            tpList.add(tempTpBldr.build());

        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder tpNode1
                = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                .rev180226.Node1Builder();

        tpNode1.setTerminationPoint(tpList);

        nodebldr.addAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                tpNode1.build());
        return new NodeData(nodebldr, portDirectionEnum);
    }


    private NodeBuilder createSrgNode(String nodeId, int srgCounter, int portDirectionEnum) {
        // Create augmentation node to in order to add srg
        Node1Builder node1bldr = new Node1Builder();
        // set node type to SRG
        node1bldr.setNodeType(OpenroadmNodeType.SRG);

        node1bldr.setNodeType(OpenroadmNodeType.SRG);

        SrgAttributesBuilder srgAttrBldr = new SrgAttributesBuilder();
        srgAttrBldr.setAvailableWavelengths(create96AvalWaveSrg());
        node1bldr.setSrgAttributes(srgAttrBldr.build());
        // Create a generic Topo Layer node
        NodeBuilder nodebldr = createTopoLayerNode(nodeId);
        nodebldr.addAugmentation(Node1.class, node1bldr.build());


        // Get connection ports on degree number = degreeCounter in order to get port
        // direction
        int maxPpPorts = getMaxPp(nodeId, srgCounter);
        if (maxPpPorts == -1) {
            return null;
        }


        String nodeIdtopo = new StringBuilder().append(nodeId).append("-SRG").append(srgCounter).toString();
        nodebldr.setNodeId(new NodeId(nodeIdtopo));
        List<TerminationPoint> tpList = new ArrayList<>();

        TerminationPoint1Builder tp1Bldr;
        TerminationPointBuilder tempTpBldr;

        for (int i = 1; i <= maxPpPorts; i++) {
            if (portDirectionEnum == 1 || portDirectionEnum == 2) {
                if (i >= maxPpPorts / 2) {
                    break;
                }
                // ports are uni Directional on a degree, therefore 4 termination points
                // Create PP-TX termination
                tempTpBldr = createTpBldr("SRG" + srgCounter + "-PP" + i + "-TX");
                tp1Bldr = new TerminationPoint1Builder();
                tp1Bldr.setTpType(OpenroadmTpType.SRGTXPP);
                tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                tpList.add(tempTpBldr.build());

                // Create PP-RX termination
                tempTpBldr = createTpBldr("SRG" + srgCounter + "-PP" + i + "-RX");
                tp1Bldr = new TerminationPoint1Builder();
                tp1Bldr.setTpType(OpenroadmTpType.SRGRXPP);
                tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                tpList.add(tempTpBldr.build());

            } else if (portDirectionEnum == 3) {
                // Ports are bi directional therefore 2 termination points
                // Create PP-TXRX termination
                tempTpBldr = createTpBldr("SRG" + srgCounter + "-PP" + i + "-TXRX");
                tp1Bldr = new TerminationPoint1Builder();
                tp1Bldr.setTpType(OpenroadmTpType.SRGTXRXPP);
                tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                tpList.add(tempTpBldr.build());
            }
        }

        switch (portDirectionEnum) {
            case 1: // ports are uni Directional on a degree
            case 2:
                // Create CP-TX termination
                tempTpBldr = createTpBldr("SRG" + srgCounter + "-CP" + "-TX");
                tp1Bldr = new TerminationPoint1Builder();
                tp1Bldr.setTpType(OpenroadmTpType.SRGTXCP);
                tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                tpList.add(tempTpBldr.build());
                // Create CP-RX termination
                tempTpBldr = createTpBldr("SRG" + srgCounter + "-CP" + "-RX");
                tp1Bldr = new TerminationPoint1Builder();
                tp1Bldr.setTpType(OpenroadmTpType.SRGRXCP);
                tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                tpList.add(tempTpBldr.build());
                break;
            case 3:
                // Ports are bi directional therefore 2 termination points
                // Create CP-TXRX termination
                tempTpBldr = createTpBldr("SRG" + srgCounter + "-CP" + "-TXRX");
                tp1Bldr = new TerminationPoint1Builder();
                tp1Bldr.setTpType(OpenroadmTpType.SRGTXRXCP);
                tempTpBldr.addAugmentation(TerminationPoint1.class, tp1Bldr.build());
                tpList.add(tempTpBldr.build());
                break;
            default:
                LOG.error("No correponsding direction to the value: {}", portDirectionEnum);
                break;
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder tpNode1 =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder();

        tpNode1.setTerminationPoint(tpList);

        nodebldr.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
            tpNode1.build());

        return nodebldr;
    }

    /*
     * This method will return the TTP ports in the device for a given degree number to be used by the
     * node to create TTP and CTP termination point on the device
     */
    private List<ConnectionPorts> getDegreePorts(String deviceId, Integer degreeCounter) {
        List<ConnectionPorts> degreeConPorts = new ArrayList<>();
        LOG.info("Getting Connection ports for Degree Number {}", degreeCounter);
        InstanceIdentifier<Degree> deviceIID =
                InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Degree.class, new DegreeKey(degreeCounter));

        Optional<Degree> ordmDegreeObject =
                deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.CONFIGURATION, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        if (ordmDegreeObject.isPresent()) {
            degreeConPorts.addAll(new ArrayList<>(ordmDegreeObject.get().getConnectionPorts()));
        } else {
            LOG.info("Device has {} degree", (degreeCounter - 1));
            return Collections.emptyList();
        }
        return degreeConPorts;
    }

    private int getMaxPp(String deviceId, Integer srgCounter) {
        int maxPpPorts;
        LOG.info("Getting max pp ports for Srg Number {}", srgCounter);
        InstanceIdentifier<SharedRiskGroup> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(SharedRiskGroup.class, new SharedRiskGroupKey(srgCounter));
        Optional<SharedRiskGroup> ordmSrgObject =
                deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
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
        // Sets the value of Network-ref and Node-ref as a part of the supporting node
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

    // Return 0 for null/error values
    // Return 1 for tx
    // Return 2 for rx
    // Return 3 for bi-directional

    private int getPortDirection(String deviceId, String circuitPackName, String portName) {
        InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
                .child(Ports.class, new PortsKey(portName));
        LOG.info("Fetching Port Direction for port {} at circuit pack {}", portName, circuitPackName);
        Optional<Ports> portObject =
                deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL, portIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (portObject.isPresent()) {
            Ports port = portObject.get();
            if (port.getPortDirection() != null) {
                return port.getPortDirection().getIntValue();
            } else {
                LOG.warn("Port direction value missing for {} {}", circuitPackName, port.getPortName());
                return 0;
            }
        }
        return 0;
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





    private List<Link> createExpressLinks(String nodeId, int numOfDegrees, int portDirectionEnum) {
        LOG.info("creating express links {} {} {}", nodeId, numOfDegrees, portDirectionEnum);
        List<Link> links = new ArrayList<>();

        String srcNode;
        String destNode;

        String srcTp;
        String destTp;

        // ports are uni-directional
        if (portDirectionEnum == 1 || portDirectionEnum == 2) {
            LOG.info("creating uni-directional express links");
            for (int i = 1; i <= numOfDegrees; i++) {
                for (int j = i + 1; j <= numOfDegrees; j++) {

                    srcNode = nodeId + "-DEG" + i;
                    destNode = nodeId + "-DEG" + j;

                    // AtoZ direction
                    srcTp = "DEG" + i + "-CTP-TX";
                    destTp = "DEG" + j + "-CTP-RX";

                    LinkBuilder expLinkBldr = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp);

                    Link1Builder lnk1Bldr = new Link1Builder();
                    lnk1Bldr.setLinkType(OpenroadmLinkType.EXPRESSLINK);
                    expLinkBldr.addAugmentation(Link1.class, lnk1Bldr.build());
                    links.add(expLinkBldr.build());

                    // ZtoA direction
                    srcTp = "DEG" + i + "-CTP-RX";
                    destTp = "DEG" + j + "-CTP-TX";

                    expLinkBldr = TopologyUtils.createLink(destNode, srcNode, destTp, srcTp);
                    expLinkBldr.addAugmentation(Link1.class, lnk1Bldr.build());
                    links.add(expLinkBldr.build());

                }
            }
        }

        // ports are bi-directional
        if (portDirectionEnum == 3) {
            LOG.info("creating bi-directional express links");
            for (int i = 1; i <= numOfDegrees; i++) {
                for (int j = i + 1; j <= numOfDegrees; j++) {

                    srcNode = nodeId + "-DEG" + i;
                    destNode = nodeId + "-DEG" + j;

                    // AtoZ direction
                    srcTp = "DEG" + i + "-CTP-TXRX";
                    destTp = "DEG" + j + "-CTP-TXRX";

                    Link1Builder lnk1Bldr = new Link1Builder();
                    lnk1Bldr.setLinkType(OpenroadmLinkType.EXPRESSLINK);
                    LinkBuilder expLinkBldr = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp);
                    expLinkBldr.addAugmentation(Link1.class, lnk1Bldr.build());
                    links.add(expLinkBldr.build());

                    // ZtoA direction
                    expLinkBldr = TopologyUtils.createLink(destNode, srcNode, destTp, srcTp);
                    expLinkBldr.addAugmentation(Link1.class, lnk1Bldr.build());
                    links.add(expLinkBldr.build());
                }
            }
        }
        return links;
    }

    private List<Link> createAddDropLinks(String nodeId, int numOfDegrees, int numOfSrgs, int portDirectionEnum) {
        LOG.info("creating add-drop links {} {} {} {}", nodeId, numOfDegrees, numOfSrgs, portDirectionEnum);
        List<Link> links = new ArrayList<>();

        String srcNode;
        String destNode;

        String srcTp;
        String destTp;

        // ports are uni-directional
        if (portDirectionEnum == 1 || portDirectionEnum == 2) {
            LOG.info("creating uni-directional add-drop links");
            for (int i = 1; i <= numOfDegrees; i++) {
                for (int j = 1; j <= numOfSrgs; j++) {

                    srcNode = nodeId + "-DEG" + i;
                    destNode = nodeId + "-SRG" + j;

                    // drop links
                    srcTp = "DEG" + i + "-CTP-TX";
                    destTp = "SRG" + j + "-CP-RX";

                    LinkBuilder addDropLinkBldr = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp);
                    Link1Builder lnk1Bldr = new Link1Builder();
                    lnk1Bldr.setLinkType(OpenroadmLinkType.DROPLINK);
                    addDropLinkBldr.addAugmentation(Link1.class, lnk1Bldr.build());
                    links.add(addDropLinkBldr.build());

                    // add links direction
                    srcTp = "DEG" + i + "-CTP-RX";
                    destTp = "SRG" + j + "-CP-TX";

                    addDropLinkBldr = TopologyUtils.createLink(destNode, srcNode, destTp, srcTp);
                    lnk1Bldr.setLinkType(OpenroadmLinkType.ADDLINK);
                    addDropLinkBldr.addAugmentation(Link1.class, lnk1Bldr.build());
                    links.add(addDropLinkBldr.build());

                }
            }
        }
        // ports are bi-directional
        if (portDirectionEnum == 3) {
            LOG.info("creating bi-directional add-drop links");
            for (int i = 1; i <= numOfDegrees; i++) {
                for (int j = 1; j <= numOfSrgs; j++) {

                    srcNode = nodeId + "-DEG" + i;
                    destNode = nodeId + "-SRG" + j;

                    // drop links
                    srcTp = "DEG" + i + "-CTP-TXRX";
                    destTp = "SRG" + j + "-CP-TXRX";

                    LinkBuilder addDropLinkBldr = TopologyUtils.createLink(srcNode, destNode, srcTp, destTp);
                    Link1Builder lnk1Bldr = new Link1Builder();
                    lnk1Bldr.setLinkType(OpenroadmLinkType.DROPLINK);
                    addDropLinkBldr.addAugmentation(Link1.class, lnk1Bldr.build());
                    links.add(addDropLinkBldr.build());

                    // add link
                    addDropLinkBldr = TopologyUtils.createLink(destNode, srcNode, destTp, srcTp);
                    lnk1Bldr.setLinkType(OpenroadmLinkType.ADDLINK);
                    addDropLinkBldr.addAugmentation(Link1.class, lnk1Bldr.build());
                    links.add(addDropLinkBldr.build());
                }
            }
        }
        return links;
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
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).augmentation(Network1.class)
                .child(Link.class, new LinkKey(linkId));
            com.google.common.base.Optional<Link> link =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID.build()).get();
            if (link.isPresent()) {
                LinkBuilder linkBuilder = new LinkBuilder(link.get());
                Link1Builder link1Builder = new Link1Builder(linkBuilder.augmentation(org.opendaylight
                        .yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1.class));
                link1Builder.setAdministrativeState(State.OutOfService);
                linkBuilder.removeAugmentation(Link1.class);
                linkBuilder.addAugmentation(Link1.class,link1Builder.build());
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(),
                        linkBuilder.build());
                networkTransactionService.submit().get(1, TimeUnit.SECONDS);
                return true;
            } else {
                LOG.error("No link found for given LinkId: {}",
                        linkId);
                return false;
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
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

    private List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node
            .attributes.AvailableWavelengths> create96AvalWaveSrg() {

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                .AvailableWavelengths> waveList =
                new ArrayList<>();

        for (int i = 1; i < 97; i++) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                    .AvailableWavelengthsBuilder avalBldr =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node
                            .attributes.AvailableWavelengthsBuilder();
            avalBldr.setIndex((long) i);
            avalBldr.withKey(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node
                            .attributes.AvailableWavelengthsKey(
                            (long) i));
            waveList.add(avalBldr.build());
        }

        return waveList;
    }
}
