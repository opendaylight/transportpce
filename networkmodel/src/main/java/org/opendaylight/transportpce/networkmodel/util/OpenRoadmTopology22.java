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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200113.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200113.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.networks.network.network.types.OpenroadmCommonNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsKey;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmTopology22 {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology22.class);

    private NetworkTransactionService networkTransactionService;

    public OpenRoadmTopology22(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
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
            this.networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            LOG.info("OpenRoadm-Topology created successfully.");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("Failed to create OpenRoadm-Topology", e);
        }
    }

    /**
     * Create empty OpenROADM topology.
     */
    private Network createOpenRoadmTopology() {
        NetworkId nwId = new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID);
        NetworkTypes1Builder topoNetworkTypesBldr = new NetworkTypes1Builder()
            .setOpenroadmCommonNetwork(new OpenroadmCommonNetworkBuilder().build());
        NetworkTypesBuilder nwTypeBuilder = new NetworkTypesBuilder()
            .addAugmentation(NetworkTypes1.class, topoNetworkTypesBldr.build());
        // Array to store nodes in the topolayer of a roadm/Xponder
        Network1Builder nwBldr1 = new Network1Builder()
            .setLink(Collections.emptyList());
        NetworkBuilder nwBuilder = new NetworkBuilder()
            .setNetworkId(nwId)
            .withKey(new NetworkKey(nwId))
            .setNetworkTypes(nwTypeBuilder.build())
            // adding expressLinks
            .addAugmentation(Network1.class, nwBldr1.build())
            .setNode(Collections.emptyList());
        return nwBuilder.build();
    }

    public TopologyShard createTopologyShard(Nodes mappingNode) {
        int numOfDegrees;
        int numOfSrgs;
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();

        // Check if node is ROADM
        if (NodeTypes.Rdm.getIntValue() == mappingNode.getNodeInfo().getNodeType().getIntValue()) {
            LOG.info("creating rdm node in openroadmtopology for node {}", mappingNode.getNodeId());
            // transform flat mapping list to per degree and per srg mapping lists
            Map<String, List<Mapping>> mapDeg = new HashMap<>();
            Map<String, List<Mapping>> mapSrg = new HashMap<>();
            List<Mapping> mappingList = mappingNode.getMapping();
            mappingList.sort(Comparator.comparing(Mapping::getLogicalConnectionPoint));

            List<String> nodeShardList = new ArrayList<>();
            for (Mapping mapping : mappingList) {
                String str = mapping.getLogicalConnectionPoint().split("-")[0];
                if (!nodeShardList.contains(str)) {
                    nodeShardList.add(str);
                }
            }
            for (String str : nodeShardList) {
                List<Mapping> interList = new ArrayList<>();
                interList = mappingList.stream().filter(x -> x.getLogicalConnectionPoint().contains(str))
                    .collect(Collectors.toList());
                if (str.contains("DEG")) {
                    mapDeg.put(str, interList);
                } else if (str.contains("SRG")) {
                    mapSrg.put(str,  interList);
                } else {
                    LOG.error("unknown element");
                }
            }
            // create degree nodes
            for (String k : mapDeg.keySet()) {
                NodeBuilder ietfNode = createDegree(k, mapDeg.get(k), mappingNode.getNodeId());
                nodes.add(ietfNode.build());
            }
            // create srg nodes
            for (String k : mapSrg.keySet()) {
                NodeBuilder ietfNode = createSrg(k, mapSrg.get(k), mappingNode.getNodeId());
                nodes.add(ietfNode.build());
            }

            numOfDegrees = mapDeg.size();
            numOfSrgs = mapSrg.size();

            LOG.info("adding links numOfDegrees={} numOfSrgs={}", numOfDegrees, numOfSrgs);
            links.addAll(createNewLinks(nodes));
            LOG.info("created nodes/links: {}/{}", nodes.size(), links.size());
            return new TopologyShard(nodes, links);
        } else if (NodeTypes.Xpdr.getIntValue() ==  mappingNode.getNodeInfo().getNodeType().getIntValue()) {
            // Check if node is XPONDER
            LOG.info("creating xpdr node in openroadmtopology for node {}", mappingNode.getNodeId());
            NodeBuilder ietfNode = createXpdr(mappingNode);
            nodes.add(ietfNode.build());
            return new TopologyShard(nodes, links);
        }
        LOG.error("Device node Type not managed yet");
        return null;
    }

    private NodeBuilder createXpdr(Nodes mappingNode) {
        // set node-id
        String nodeIdtopo = new StringBuilder().append(mappingNode.getNodeId()).append("-XPDR1").toString();
        // Create openroadm-network-topo augmentation to set node type to Xponder
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .Node1Builder ocnNode1Bldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .Node1Builder().setNodeType(OpenroadmNodeType.XPONDER);
        // Create ietf node setting supporting-node data
        NodeBuilder ietfNodeBldr = createTopoLayerNode(mappingNode.getNodeId())
                .setNodeId(new NodeId(nodeIdtopo))
                .withKey((new NodeKey(new NodeId(nodeIdtopo))))
                .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                    .Node1.class, ocnNode1Bldr.build());

        // Create tp-list
        List<TerminationPoint> tpList = new ArrayList<>();
        TerminationPointBuilder ietfTpBldr;
        for (Mapping m : mappingNode.getMapping()) {
            ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint());
            // Add openroadm-network-topology tp augmentations
            TerminationPoint1Builder ontTp1Bldr = new TerminationPoint1Builder();
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1Builder ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.common
                .network.rev181130.TerminationPoint1Builder();
            if (m.getPortQual().equals("xpdr-network")) {
                XpdrNetworkAttributesBuilder xpdrNwAttrBldr = new XpdrNetworkAttributesBuilder()
                        .setTailEquipmentId(m.getConnectionMapLcp());
                ontTp1Bldr.setXpdrNetworkAttributes(xpdrNwAttrBldr.build());
                ocnTp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625.TerminationPoint1Builder tpceTp1Bldr =
                    new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625.TerminationPoint1Builder()
                        .setAssociatedConnectionMapPort(m.getConnectionMapLcp());
                ietfTpBldr.addAugmentation(TerminationPoint1.class, ontTp1Bldr.build())
                    .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                        .TerminationPoint1.class, ocnTp1Bldr.build())
                    .addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625
                        .TerminationPoint1.class, tpceTp1Bldr.build());
                tpList.add(ietfTpBldr.build());
            } else if (m.getPortQual().equals("xpdr-client")) {
                XpdrClientAttributesBuilder xpdrNwAttrBldr = new XpdrClientAttributesBuilder()
                        .setTailEquipmentId(m.getConnectionMapLcp());
                ontTp1Bldr.setXpdrClientAttributes(xpdrNwAttrBldr.build());
                ocnTp1Bldr.setTpType(OpenroadmTpType.XPONDERCLIENT);
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625.TerminationPoint1Builder tpceTp1Bldr =
                    new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625.TerminationPoint1Builder()
                        .setAssociatedConnectionMapPort(m.getConnectionMapLcp());
                ietfTpBldr.addAugmentation(TerminationPoint1.class, ontTp1Bldr.build())
                    .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                        .TerminationPoint1.class, ocnTp1Bldr.build())
                    .addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev190625
                        .TerminationPoint1.class, tpceTp1Bldr.build());
                tpList.add(ietfTpBldr.build());
            }
        }

        // Create ietf node augmentation to support ietf tp-list
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .Node1Builder ietfNode1 =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
                .setTerminationPoint(tpList);
        ietfNodeBldr.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
            ietfNode1.build());
        return ietfNodeBldr;
    }

    private NodeBuilder createDegree(String degNb, List<Mapping> degListMap, String nodeId) {
        // Create tp-list
        List<TerminationPoint> tpList = new ArrayList<>();
        TerminationPointBuilder ietfTpBldr;
        for (Mapping m : degListMap) {
            ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint());
            // Add openroadm-network-topology tp augmentations
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1Builder ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.common
                .network.rev181130.TerminationPoint1Builder();
            switch (m.getPortDirection()) {
                case "bidirectional":
                    ocnTp1Bldr.setTpType(OpenroadmTpType.DEGREETXRXTTP);
                    break;
                case "tx":
                    ocnTp1Bldr.setTpType(OpenroadmTpType.DEGREETXTTP);
                    break;
                case "rx":
                    ocnTp1Bldr.setTpType(OpenroadmTpType.DEGREERXTTP);
                    break;
                default:
                    LOG.error("impossible to set tp-type to {}", m.getLogicalConnectionPoint());
            }
            ietfTpBldr.addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1.class, ocnTp1Bldr.build());
            tpList.add(ietfTpBldr.build());
        }
        // Add CTP to tp-list
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder ocnTp1Bldr =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder()
            .setTpType(OpenroadmTpType.DEGREETXRXCTP);
        ietfTpBldr = createTpBldr(degNb + "-CTP-TXRX").addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm
            .common.network.rev181130.TerminationPoint1.class, ocnTp1Bldr.build());
        tpList.add(ietfTpBldr.build());

        // Create ietf node augmentation to support ietf tp-list
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .Node1Builder ietfNode1 =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
                .setTerminationPoint(tpList);

        // set node-id
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-").append(degNb).toString();
        // set degree-attributes
        DegreeAttributesBuilder degAttBldr = new DegreeAttributesBuilder()
                .setDegreeNumber(new Integer(degNb.split("DEG")[1]))
                .setAvailableWavelengths(create96AvalWaveDegree());
        // Create openroadm-network-topo augmentation to set node type to DEGREE
        Node1Builder ontNode1Bldr = new Node1Builder()
                .setDegreeAttributes(degAttBldr.build());
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder ocnNode1Bldr =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.DEGREE);

        // Create ietf node setting supporting-node data
        NodeBuilder ietfNodeBldr = createTopoLayerNode(nodeId)
            .setNodeId(new NodeId(nodeIdtopo))
            .withKey((new NodeKey(new NodeId(nodeIdtopo))))
            .addAugmentation(Node1.class, ontNode1Bldr.build())
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .Node1.class, ocnNode1Bldr.build())
            .addAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                ietfNode1.build());
        return ietfNodeBldr;
    }

    private NodeBuilder createSrg(String srgNb, List<Mapping> srgListMap, String nodeId) {
        // Create tp-list
        List<TerminationPoint> tpList = new ArrayList<>();
        TerminationPointBuilder ietfTpBldr;
        for (Mapping m : srgListMap) {
            ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint());
            // Add openroadm-network-topology tp augmentations
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1Builder ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.common
                .network.rev181130.TerminationPoint1Builder();
            switch (m.getPortDirection()) {
                case "bidirectional":
                    ocnTp1Bldr.setTpType(OpenroadmTpType.SRGTXRXPP);
                    break;
                case "tx":
                    ocnTp1Bldr.setTpType(OpenroadmTpType.SRGTXPP);
                    break;
                case "rx":
                    ocnTp1Bldr.setTpType(OpenroadmTpType.SRGRXPP);
                    break;
                default:
                    LOG.error("impossible to set tp-type to {}", m.getLogicalConnectionPoint());
            }
            ietfTpBldr.addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1.class, ocnTp1Bldr.build());
            tpList.add(ietfTpBldr.build());
        }
        // Add CP to tp-list
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder ocnTp1Bldr =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder()
            .setTpType(OpenroadmTpType.SRGTXRXCP);
        ietfTpBldr = createTpBldr(srgNb + "-CP-TXRX").addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm
            .common.network.rev181130.TerminationPoint1.class, ocnTp1Bldr.build());
        tpList.add(ietfTpBldr.build());

        // Create ietf node augmentation to support ietf tp-list
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .Node1Builder ietfNode1 =
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
                .setTerminationPoint(tpList);

        // set node-id
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-").append(srgNb).toString();
        // set srg-attributes
        SrgAttributesBuilder srgAttrBldr = new SrgAttributesBuilder().setAvailableWavelengths(create96AvalWaveSrg());
        // Create openroadm-network-topo augmentation to set node type to DEGREE
        Node1Builder ontNode1Bldr = new Node1Builder()
                .setSrgAttributes(srgAttrBldr.build());
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder ocnNode1Bldr =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.SRG);

        // Create ietf node setting supporting-node data
        NodeBuilder ietfNodeBldr = createTopoLayerNode(nodeId)
            .setNodeId(new NodeId(nodeIdtopo))
            .withKey((new NodeKey(new NodeId(nodeIdtopo))))
            .addAugmentation(Node1.class, ontNode1Bldr.build())
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1.class,
                ocnNode1Bldr.build())
            .addAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
                ietfNode1.build());
        return ietfNodeBldr;
    }

    private NodeBuilder createTopoLayerNode(String nodeId) {
        // Sets the value of Network-ref and Node-ref as a part of the supporting node
        // attribute
        SupportingNodeBuilder supportbldr = new SupportingNodeBuilder()
                .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID), new NodeId(nodeId)))
                .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                .setNodeRef(new NodeId(nodeId));
        ArrayList<SupportingNode> supportlist = new ArrayList<>();
        supportlist.add(supportbldr.build());
        NodeBuilder nodebldr = new NodeBuilder().setSupportingNode(supportlist);
        return nodebldr;
    }


    // This method returns a generic termination point builder for a given tpid
    private TerminationPointBuilder createTpBldr(String tpId) {
        TpId tp = new TpId(tpId);
        TerminationPointKey tpKey = new TerminationPointKey(tp);
        TerminationPointBuilder tpBldr = new TerminationPointBuilder().withKey(tpKey).setTpId(tp);
        return tpBldr;
    }

    private LinkBuilder createLink(String srcNode, String destNode, String srcTp, String destTp) {
        //create source link
        SourceBuilder ietfSrcLinkBldr = new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(srcTp);
        //create destination link
        DestinationBuilder ietfDestLinkBldr = new DestinationBuilder()
                .setDestNode(new NodeId(destNode))
                .setDestTp(destTp);
        LinkBuilder ietfLinkBldr = new LinkBuilder()
                .setSource(ietfSrcLinkBldr.build())
                .setDestination(ietfDestLinkBldr.build())
                .setLinkId(LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp));
        ietfLinkBldr.withKey(new LinkKey(ietfLinkBldr.getLinkId()));
        return ietfLinkBldr;
    }

    private List<Link> createNewLinks(List<Node> nodes) {
        List<Link> links = new ArrayList<>();
        String srcNode;
        String destNode;
        String srcTp;
        String destTp;
        for (int i = 0; i < nodes.size() - 1; i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                srcNode = nodes.get(i).getNodeId().getValue();
                destNode = nodes.get(j).getNodeId().getValue();
                // A to Z direction
                srcTp = nodes.get(i).augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                    .network.topology.rev180226.Node1.class).getTerminationPoint().stream()
                    .filter(tp -> tp.getTpId().getValue().contains("CP") || tp.getTpId().getValue().contains("CTP"))
                    .findFirst().get().getTpId().getValue();
                destTp = nodes.get(j).augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                    .network.topology.rev180226.Node1.class).getTerminationPoint().stream()
                    .filter(tp -> tp.getTpId().getValue().contains("CP") || tp.getTpId().getValue().contains("CTP"))
                    .findFirst().get().getTpId().getValue();
                Link1Builder ocnAzLinkBldr = new Link1Builder();
                int srcNodeType = nodes.get(i).augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common
                    .network.rev181130.Node1.class).getNodeType().getIntValue();
                int destNodeType = nodes.get(j).augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common
                    .network.rev181130.Node1.class).getNodeType().getIntValue();
                //The previous 2 lines generate warnings.
                //Casting (nodes.get(i or j).augmentation(Node1.class)) to
                //(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1)
                //breaks the portmapping
                if (srcNodeType == 11 && destNodeType == 11) {
                    ocnAzLinkBldr.setLinkType(OpenroadmLinkType.EXPRESSLINK);
                } else if (srcNodeType == 11 && destNodeType == 12) {
                    ocnAzLinkBldr.setLinkType(OpenroadmLinkType.DROPLINK);
                } else if (srcNodeType == 12 && destNodeType == 11) {
                    ocnAzLinkBldr.setLinkType(OpenroadmLinkType.ADDLINK);
                } else {
                    continue;
                }
                // Z to A direction
                Link1Builder ocnZaLinkBldr = new Link1Builder();
                if (srcNodeType == 11 && destNodeType == 11) {
                    ocnZaLinkBldr.setLinkType(OpenroadmLinkType.EXPRESSLINK);
                } else if (destNodeType == 11 && srcNodeType == 12) {
                    ocnZaLinkBldr.setLinkType(OpenroadmLinkType.DROPLINK);
                } else if (destNodeType == 12 && srcNodeType == 11) {
                    ocnZaLinkBldr.setLinkType(OpenroadmLinkType.ADDLINK);
                } else {
                    continue;
                }
                // set opposite link augmentations
                LinkBuilder ietfAzLinkBldr = createLink(srcNode, destNode, srcTp, destTp);
                LinkBuilder ietfZaLinkBldr = createLink(destNode, srcNode, destTp, srcTp);
                ocnAzLinkBldr.setOppositeLink(ietfZaLinkBldr.getLinkId());
                ietfAzLinkBldr.addAugmentation(Link1.class, ocnAzLinkBldr.build())
                    .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                        .Link1.class, ocnAzLinkBldr.build());
                ocnZaLinkBldr.setOppositeLink(ietfAzLinkBldr.getLinkId());
                ietfZaLinkBldr.addAugmentation(Link1.class, ocnZaLinkBldr.build());
                links.add(ietfAzLinkBldr.build());
                links.add(ietfZaLinkBldr.build());
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
            java.util.Optional<Link> link =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID.build()).get();
            if (link.isPresent()) {
                LinkBuilder linkBuilder = new LinkBuilder(link.get());
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                    .Link1Builder link1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology
                    .rev181130.Link1Builder(linkBuilder.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .network.topology.rev181130.Link1.class)).setAdministrativeState(State.OutOfService);
                linkBuilder.removeAugmentation(Link1.class).addAugmentation(org.opendaylight.yang.gen.v1.http.org
                    .openroadm.network.topology.rev181130.Link1.class,link1Builder.build());
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(),
                        linkBuilder.build());
                networkTransactionService.commit().get(1, TimeUnit.SECONDS);
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
            AvailableWavelengthsBuilder avalBldr = new AvailableWavelengthsBuilder()
                    .setIndex((long) i)
                    .withKey(new AvailableWavelengthsKey((long) i));
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
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                        .AvailableWavelengthsBuilder()
                        .setIndex((long) i)
                        .withKey(
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                                .AvailableWavelengthsKey((long) i));
            waveList.add(avalBldr.build());
        }

        return waveList;
    }
}
