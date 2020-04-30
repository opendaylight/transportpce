/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import java.util.ArrayList;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
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
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenRoadmTopology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology.class);

    private OpenRoadmTopology() {
    }

    public static TopologyShard createTopologyShard(Nodes mappingNode) {
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
            List<Mapping> mappingList = new ArrayList<>(mappingNode.getMapping());
            mappingList.sort(Comparator.comparing(Mapping::getLogicalConnectionPoint));

            List<String> nodeShardList = new ArrayList<>();
            for (Mapping mapping : mappingList) {
                String str = mapping.getLogicalConnectionPoint().split("-")[0];
                if (!nodeShardList.contains(str)) {
                    nodeShardList.add(str);
                }
            }
            for (String str : nodeShardList) {
                List<Mapping> interList = mappingList.stream().filter(x -> x.getLogicalConnectionPoint().contains(str))
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
            for (Map.Entry<String, List<Mapping>> entry : mapDeg.entrySet()) {
                NodeBuilder ietfNode = createDegree(entry.getKey(), entry.getValue(), mappingNode.getNodeId(),
                    mappingNode.getNodeInfo().getNodeClli());
                nodes.add(ietfNode.build());
            }
            // create srg nodes
            for (Map.Entry<String, List<Mapping>> entry : mapSrg.entrySet()) {
                NodeBuilder ietfNode = createSrg(entry.getKey(), entry.getValue(), mappingNode.getNodeId(),
                    mappingNode.getNodeInfo().getNodeClli());
                nodes.add(ietfNode.build());
            }

            numOfDegrees = mapDeg.size();
            numOfSrgs = mapSrg.size();

            LOG.info("adding links numOfDegrees={} numOfSrgs={}", numOfDegrees, numOfSrgs);
            links.addAll(createNewLinks(nodes));
            LOG.info("created nodes/links: {}/{}", nodes.size(), links.size());
            return new TopologyShard(nodes, links);
        } else if (NodeTypes.Xpdr.getIntValue() ==  mappingNode.getNodeInfo().getNodeType().getIntValue()) {
            // Check if node is Xpdr is a Transponder
            List<Mapping> networkMappings = mappingNode.getMapping().stream().filter(k -> k.getLogicalConnectionPoint()
                .contains("NETWORK")).collect(Collectors.toList());
            List<Integer> tpdrList = new ArrayList<>();
            for (Mapping mapping : networkMappings) {
                List<Mapping> extractedMappings = null;
                Integer xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
                if (!tpdrList.contains(xpdrNb)) {
                    tpdrList.add(xpdrNb);
                    extractedMappings = mappingNode.getMapping().stream().filter(lcp -> lcp
                        .getLogicalConnectionPoint().contains("XPDR" + xpdrNb)).collect(Collectors.toList());
                    NodeBuilder ietfNode;
                    if (mapping.getXponderType() == null
                        || XpdrNodeTypes.Tpdr.getIntValue() == mapping.getXponderType().getIntValue()) {
                        LOG.info("creating xpdr node {} of type Tpdr in openroadm-topology",
                            mappingNode.getNodeId() + "-XPDR" + xpdrNb);
                        ietfNode = createXpdr(mappingNode.getNodeId(), mappingNode.getNodeInfo().getNodeClli(), xpdrNb,
                            extractedMappings, false);
                        nodes.add(ietfNode.build());
                    } else if (XpdrNodeTypes.Mpdr.getIntValue() == mapping.getXponderType().getIntValue()
                        || XpdrNodeTypes.Switch.getIntValue() == mapping.getXponderType().getIntValue()) {
                        LOG.info("creating xpdr node {} of type {} in openroadm-topology",
                            mappingNode.getNodeId() + "-XPDR" + xpdrNb, mapping.getXponderType().getName());
                        ietfNode = createXpdr(mappingNode.getNodeId(), mappingNode.getNodeInfo().getNodeClli(), xpdrNb,
                            extractedMappings, true);
                        nodes.add(ietfNode.build());
                    }
                }
            }
            if (nodes.isEmpty()) {
                return null;
            } else {
                return new TopologyShard(nodes, links);
            }
        }
        LOG.error("Device node Type not managed yet");
        return null;
    }

    private static NodeBuilder createXpdr(String nodeId, String clli, Integer xpdrNb, List<Mapping> mappings,
        boolean isOtn) {
        // Create openroadm-network-topo augmentation to set node type to Xponder
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 ocnNode1 =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.XPONDER).build();
        // Create ietf node setting supporting-node data
        NodeBuilder ietfNodeBldr = createTopoLayerNode(nodeId, clli);
        // set node-id
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-XPDR").append(xpdrNb).toString();
        ietfNodeBldr.setNodeId(new NodeId(nodeIdtopo))
            .withKey((new NodeKey(new NodeId(nodeIdtopo))))
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .Node1.class, ocnNode1);

        // Create tp-list
        List<TerminationPoint> tpList = new ArrayList<>();
        TerminationPointBuilder ietfTpBldr;
        for (Mapping m : mappings) {
            if (!isOtn) {
                ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint());
                // Add openroadm-network-topology tp augmentations
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                    .TerminationPoint1Builder ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http.org.openroadm
                    .common.network.rev181130.TerminationPoint1Builder();
                if (m.getPortQual().equals("xpdr-network")) {
                    ocnTp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
                    org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1 tpceTp1 =
                        new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129
                        .TerminationPoint1Builder().setAssociatedConnectionMapPort(m.getConnectionMapLcp()).build();
                    ietfTpBldr
                        .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network
                            .rev181130.TerminationPoint1.class, ocnTp1Bldr.build())
                        .addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129
                            .TerminationPoint1.class, tpceTp1);
                    tpList.add(ietfTpBldr.build());
                } else if (m.getPortQual().equals("xpdr-client")) {
                    ocnTp1Bldr.setTpType(OpenroadmTpType.XPONDERCLIENT);
                    org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1 tpceTp1 =
                        new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129
                        .TerminationPoint1Builder().setAssociatedConnectionMapPort(m.getConnectionMapLcp()).build();
                    ietfTpBldr
                        .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                            .TerminationPoint1.class, ocnTp1Bldr.build())
                        .addAugmentation(org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129
                            .TerminationPoint1.class, tpceTp1);
                    tpList.add(ietfTpBldr.build());
                }
            } else {
                if (m.getPortQual().equals("xpdr-network") || m.getPortQual().equals("switch-network")) {
                    ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint());
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                        .TerminationPoint1Builder ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http.org.openroadm
                        .common.network.rev181130.TerminationPoint1Builder().setTpType(OpenroadmTpType.XPONDERNETWORK);
                    ietfTpBldr
                        .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network
                            .rev181130.TerminationPoint1.class, ocnTp1Bldr.build());
                    tpList.add(ietfTpBldr.build());
                }
            }
        }
        // Create ietf node augmentation to support ietf tp-list
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .Node1Builder ietfNode1 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
            .rev180226.Node1Builder()
            .setTerminationPoint(tpList);
        ietfNodeBldr.addAugmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class,
            ietfNode1.build());
        return ietfNodeBldr;
    }

    private static NodeBuilder createDegree(String degNb, List<Mapping> degListMap, String nodeId, String clli) {
        // Create tp-list
        List<TerminationPoint> tpList = new ArrayList<>();
        TerminationPointBuilder ietfTpBldr;
        for (Mapping m : degListMap) {
            ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint());
            // Add openroadm-common-network tp type augmentations
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
        ietfTpBldr = createTpBldr(degNb + "-CTP-TXRX");
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1 ocnTp1 =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder()
            .setTpType(OpenroadmTpType.DEGREETXRXCTP).build();
        ietfTpBldr.addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1.class, ocnTp1);
        tpList.add(ietfTpBldr.build());
        // set degree-attributes
        DegreeAttributes degAtt = new DegreeAttributesBuilder()
            .setDegreeNumber(Uint16.valueOf(degNb.split("DEG")[1]))
            .setAvailableWavelengths(create96AvalWaveDegree()).build();
        // Create ietf node augmentation to support ietf tp-list
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .Node1Builder ietfNode1 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
            .rev180226.Node1Builder()
            .setTerminationPoint(tpList);
        // set node-id
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-").append(degNb).toString();
        Node1 ontNode1 = new Node1Builder().setDegreeAttributes(degAtt).build();
        // Create openroadm-common-network augmentation to set node type to DEGREE
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 ocnNode1 =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.DEGREE).build();
        // Create ietf node setting supporting-node data
        return createTopoLayerNode(nodeId, clli)
            .setNodeId(new NodeId(nodeIdtopo))
            .withKey((new NodeKey(new NodeId(nodeIdtopo))))
            .addAugmentation(Node1.class, ontNode1)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .Node1.class, ocnNode1)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                .rev180226.Node1.class,
            ietfNode1.build());
    }

    private static NodeBuilder createSrg(String srgNb, List<Mapping> srgListMap, String nodeId, String clli) {
        // Create tp-list
        List<TerminationPoint> tpList = new ArrayList<>();
        TerminationPointBuilder ietfTpBldr;
        for (Mapping m : srgListMap) {
            ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint());
            // Add openroadm-common-network tp type augmentations
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
        ietfTpBldr = createTpBldr(srgNb + "-CP-TXRX");
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1 ocnTp1 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network
            .rev181130.TerminationPoint1Builder().setTpType(OpenroadmTpType.SRGTXRXCP).build();
        ietfTpBldr.addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1.class, ocnTp1);
        tpList.add(ietfTpBldr.build());
        // Create openroadm-common-network augmentation to set node type to SRG
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 ocnNode1 =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder()
            .setNodeType(OpenroadmNodeType.SRG).build();
        // set srg-attributes
        SrgAttributes srgAttr = new SrgAttributesBuilder().setAvailableWavelengths(create96AvalWaveSrg()).build();
        Node1 ontNode1 = new Node1Builder().setSrgAttributes(srgAttr).build();
        // Create ietf node augmentation to support ietf tp-list
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .Node1Builder ietfNode1 = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
            .rev180226.Node1Builder()
            .setTerminationPoint(tpList);
        // Create ietf node setting supporting-node data
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-").append(srgNb).toString();
        return createTopoLayerNode(nodeId, clli)
            .setNodeId(new NodeId(nodeIdtopo))
            .withKey((new NodeKey(new NodeId(nodeIdtopo))))
            .addAugmentation(Node1.class, ontNode1)
            .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .Node1.class, ocnNode1)
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class, ietfNode1.build());
    }

    private static NodeBuilder createTopoLayerNode(String nodeId, String clli) {
        // Sets the value of Network-ref and Node-ref as a part of the supporting node
        // attribute
        SupportingNodeBuilder support1bldr = new SupportingNodeBuilder()
            .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID), new NodeId(nodeId)))
            .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
            .setNodeRef(new NodeId(nodeId));
        SupportingNodeBuilder support2bldr = new SupportingNodeBuilder()
            .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID), new NodeId(clli)))
            .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
            .setNodeRef(new NodeId(clli));
        ArrayList<SupportingNode> supportlist = new ArrayList<>();
        supportlist.add(support1bldr.build());
        supportlist.add(support2bldr.build());
        return new NodeBuilder().setSupportingNode(supportlist);
    }


    // This method returns a generic termination point builder for a given tpid
    private static TerminationPointBuilder createTpBldr(String tpId) {
        TpId tp = new TpId(tpId);
        return new TerminationPointBuilder().withKey(new TerminationPointKey(tp)).setTpId(tp);
    }

    private static LinkBuilder createLink(String srcNode, String destNode, String srcTp, String destTp) {
        //create source link
        SourceBuilder ietfSrcLinkBldr = new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(srcTp);
        //create destination link
        DestinationBuilder ietfDestLinkBldr = new DestinationBuilder().setDestNode(new NodeId(destNode))
            .setDestTp(destTp);
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);
        return new LinkBuilder()
            .setSource(ietfSrcLinkBldr.build())
            .setDestination(ietfDestLinkBldr.build())
            .setLinkId(linkId)
            .withKey(new LinkKey(linkId));
    }

    private static List<Link> createNewLinks(List<Node> nodes) {
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
                ietfAzLinkBldr.addAugmentation(Link1.class, ocnAzLinkBldr.build());
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
            LOG.debug("Link Id {} updated to have admin state down", linkId);
            return true;
        } else {
            LOG.debug("Link Id not found for Source {} and Dest {}", srcNode, dstNode);
            return false;
        }
    }

    // This method returns the linkBuilder object for given source and destination
    public static boolean deleteLinkLinkId(LinkId linkId , NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for LinkId: {}", linkId.getValue());
        try {
            InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class).child(Network.class,
                new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).augmentation(Network1.class)
                .child(Link.class, new LinkKey(linkId));
            java.util.Optional<Link> link =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID.build()).get();
            if (link.isPresent()) {
                LinkBuilder linkBuilder = new LinkBuilder(link.get());
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder link1Builder =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Link1Builder(
                    linkBuilder.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                    .Link1.class))
                    .setAdministrativeState(State.OutOfService);
                linkBuilder.removeAugmentation(Link1.class)
                    .addAugmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                        .Link1.class,link1Builder.build());
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
            LOG.error("Error deleting link {}", linkId.getValue(), e);
            return false;
        }
    }

    private static List<AvailableWavelengths> create96AvalWaveDegree() {
        List<AvailableWavelengths> waveList = new ArrayList<>();

        for (int i = 1; i < 97; i++) {
            AvailableWavelengthsBuilder avalBldr = new AvailableWavelengthsBuilder()
                .setIndex(Uint32.valueOf(i))
                .withKey(new AvailableWavelengthsKey(Uint32.valueOf(i)));
            waveList.add(avalBldr.build());
        }

        return waveList;
    }

    private static List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node
            .attributes.AvailableWavelengths> create96AvalWaveSrg() {

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
            .AvailableWavelengths> waveList = new ArrayList<>();

        for (int i = 1; i < 97; i++) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                .AvailableWavelengthsBuilder avalBldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.srg
                .rev181130.srg.node.attributes.AvailableWavelengthsBuilder()
                .setIndex(Uint32.valueOf(i))
                .withKey(new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                    .AvailableWavelengthsKey(Uint32.valueOf(i)));
            waveList.add(avalBldr.build());
        }
        return waveList;
    }
}
