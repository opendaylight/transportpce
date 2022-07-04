/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import static org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes.Tpdr;

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
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmTpType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenRoadmTopology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology.class);

    private OpenRoadmTopology() {
    }

    public static TopologyShard createTopologyShard(Nodes mappingNode) {
        return createTopologyShard(mappingNode, true);
    }

    public static TopologyShard createTopologyShard(Nodes mappingNode, boolean firstMount) {
        int numOfDegrees;
        int numOfSrgs;
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();

        // Check if node is ROADM
        if (NodeTypes.Rdm.getIntValue() == mappingNode.getNodeInfo().getNodeType().getIntValue()) {
            LOG.info("creating rdm node in openroadmtopology for node {}",
                    mappingNode.getNodeId());
            // transform flat mapping list to per degree and per srg mapping lists
            Map<String, List<Mapping>> mapDeg = new HashMap<>();
            Map<String, List<Mapping>> mapSrg = new HashMap<>();
            List<Mapping> mappingList = new ArrayList<>(mappingNode.nonnullMapping().values());
            mappingList.sort(Comparator.comparing(Mapping::getLogicalConnectionPoint));
            List<String> nodeShardList = new ArrayList<>();
            for (Mapping mapping : mappingList) {
                String str = mapping.getLogicalConnectionPoint().split("-")[0];
                if (!nodeShardList.contains(str)) {
                    nodeShardList.add(str);
                }
            }
            for (String str : nodeShardList) {
                List<Mapping> interList =
                        mappingList.stream()
                                .filter(x -> x.getLogicalConnectionPoint().split("-")[0].equals(str))
                                .collect(Collectors.toList());
                if (str.contains("DEG")) {
                    mapDeg.put(str, interList);
                } else if (str.contains("SRG")) {
                    mapSrg.put(str, interList);
                } else {
                    LOG.error("unknow element");
                }
            }
            // create degree nodes
            for (Map.Entry<String, List<Mapping>> entry : mapDeg.entrySet()) {
                NodeBuilder ietfNode =
                        createDegree(entry.getKey(), entry.getValue(), mappingNode.getNodeId(),
                                mappingNode.getNodeInfo().getNodeClli(), firstMount);
                nodes.add(ietfNode.build());
            }
            // create srg nodes
            for (Map.Entry<String, List<Mapping>> entry : mapSrg.entrySet()) {
                NodeBuilder ietfNode =
                        createSrg(entry.getKey(), entry.getValue(), mappingNode.getNodeId(),
                                mappingNode.getNodeInfo().getNodeClli(), firstMount);
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
            List<Mapping> networkMappings =
                    mappingNode.nonnullMapping().values()
                            .stream().filter(k -> k.getLogicalConnectionPoint().contains("NETWORK"))
                            .collect(Collectors.toList());
            List<Integer> tpdrList = new ArrayList<>();
            for (Mapping mapping : networkMappings) {
                Integer xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
                if (!tpdrList.contains(xpdrNb)) {
                    tpdrList.add(xpdrNb);
                    List<Mapping> extractedMappings = mappingNode.nonnullMapping().values().stream()
                            .filter(lcp -> lcp.getLogicalConnectionPoint().contains("XPDR" + xpdrNb))
                            .collect(Collectors.toList());
                    Boolean lastArg;
                    String xpdrType;
                    switch (mapping.getXponderType() == null ? Tpdr : mapping.getXponderType()) {
                        case Tpdr :
                            lastArg = false;
                            xpdrType = "Tpdr";
                            break;
                        case Mpdr :
                        case Switch :
                            lastArg = true;
                            xpdrType = mapping.getXponderType().getName();
                            break;
                        default :
                            LOG.warn("cannot create xpdr node {} in openroadm-topology: type {} not supported",
                                 mappingNode.getNodeId() + "-XPDR" + xpdrNb, mapping.getXponderType().getName());
                            continue;
                    }
                    LOG.info("creating xpdr node {} of type {} in openroadm-topology",
                            mappingNode.getNodeId() + "-XPDR" + xpdrNb, xpdrType);
                    nodes.add(createXpdr(
                                    mappingNode.getNodeId(),
                                    mappingNode.getNodeInfo().getNodeClli(),
                                    xpdrNb,
                                    extractedMappings,
                                    lastArg)
                              .build());
                }
            }
            return nodes.isEmpty() ? null : new TopologyShard(nodes, links);
        }
        LOG.error("Device node Type not managed yet");
        return null;
    }

    private static NodeBuilder createXpdr(String nodeId, String clli, Integer xpdrNb, List<Mapping> mappings,
                                          boolean isOtn) {
        // Create ietf node setting supporting-node data
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-XPDR").append(xpdrNb).toString();
        NodeBuilder ietfNodeBldr = createTopoLayerNode(nodeId, clli)
                .setNodeId(new NodeId(nodeIdtopo))
                .withKey((new NodeKey(new NodeId(nodeIdtopo))))
                .addAugmentation(
                    // Create openroadm-network-topo augmentation to set node type to Xponder
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.Node1Builder()
                        .setNodeType(OpenroadmNodeType.XPONDER)
                        .setAdministrativeState(AdminStates.InService)
                        .setOperationalState(State.InService)
                        .build());

        // Create tp-map
        Map<TerminationPointKey, TerminationPoint> tpMap = new HashMap<>();
        for (Mapping m : mappings) {
            if (!isOtn) {
                // Add openroadm-network-topology tp augmentations
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.TerminationPoint1Builder
                        ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http
                            .org.openroadm.common.network.rev211210.TerminationPoint1Builder()
                                .setAdministrativeState(TopologyUtils.setNetworkAdminState(m.getPortAdminState()))
                                .setOperationalState(TopologyUtils.setNetworkOperState(m.getPortOperState()));
                if (m.getPortQual().equals("xpdr-network")) {
                    ocnTp1Bldr.setTpType(OpenroadmTpType.XPONDERNETWORK);
                    TerminationPoint ietfTp = createTpBldr(m.getLogicalConnectionPoint())
                            .addAugmentation(ocnTp1Bldr.build())
                            .addAugmentation(
                                new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123
                                        .TerminationPoint1Builder()
                                    .setAssociatedConnectionMapPort(m.getConnectionMapLcp())
                                    .build())
                            .build();
                    tpMap.put(ietfTp.key(),ietfTp);
                } else if (m.getPortQual().equals("xpdr-client")) {
                    ocnTp1Bldr.setTpType(OpenroadmTpType.XPONDERCLIENT);
                    TerminationPoint ietfTp = createTpBldr(m.getLogicalConnectionPoint())
                            .addAugmentation(ocnTp1Bldr.build())
                            .addAugmentation(
                                new org.opendaylight.yang.gen.v1.http.transportpce.topology.rev220123
                                        .TerminationPoint1Builder()
                                    .setAssociatedConnectionMapPort(m.getConnectionMapLcp())
                                    .build())
                            .build();
                    tpMap.put(ietfTp.key(),ietfTp);
                }
            } else {
                if (m.getPortQual().equals("xpdr-network") || m.getPortQual().equals("switch-network")) {
                    TerminationPoint ietfTp = createTpBldr(m.getLogicalConnectionPoint())
                        .addAugmentation(
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210
                                    .TerminationPoint1Builder()
                                .setTpType(OpenroadmTpType.XPONDERNETWORK)
                                .setAdministrativeState(TopologyUtils.setNetworkAdminState(m.getPortAdminState()))
                                .setOperationalState(TopologyUtils.setNetworkOperState(m.getPortOperState()))
                                .build())
                        .build();
                    tpMap.put(ietfTp.key(),ietfTp);
                }
            }
        }
        // Create ietf node augmentation to support ietf tp-list
        return ietfNodeBldr.addAugmentation(
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder()
                    .setTerminationPoint(tpMap)
                    .build());
    }

    private static NodeBuilder createDegree(String degNb, List<Mapping> degListMap, String nodeId, String clli,
                                            boolean firstMount) {
        // Create tp-list
        Map<TerminationPointKey,TerminationPoint> tpMap = new HashMap<>();
        for (Mapping m : degListMap) {
            // Add openroadm-common-network tp type augmentations
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.TerminationPoint1Builder
                ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210
                    .TerminationPoint1Builder()
                        .setAdministrativeState(TopologyUtils.setNetworkAdminState(m.getPortAdminState()))
                        .setOperationalState(TopologyUtils.setNetworkOperState(m.getPortOperState()));
            // Added states to degree port. TODO: add to mapping relation between abstracted and physical node states
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
            TerminationPoint ietfTp =  createTpBldr(m.getLogicalConnectionPoint())
                .addAugmentation(ocnTp1Bldr.build())
                .build();
            tpMap.put(ietfTp.key(),ietfTp);
        }
        // TODO remove intermediate variable
        // Add CTP to tp-list + added states. TODO: same comment as before with the relation between states
        TerminationPoint ietfTp = createTpBldr(degNb + "-CTP-TXRX")
                .addAugmentation(new org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev211210.TerminationPoint1Builder()
                         .setTpType(OpenroadmTpType.DEGREETXRXCTP)
                         .setAdministrativeState(AdminStates.InService)
                         .setOperationalState(State.InService)
                         .build())
                 .build();
        tpMap.put(ietfTp.key(),ietfTp);
        // set degree-attributes
        DegreeAttributesBuilder degAttBldr = new DegreeAttributesBuilder()
                .setDegreeNumber(Uint16.valueOf(degNb.split("DEG")[1]));
        if (firstMount) {
            degAttBldr.setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available());
        }
        DegreeAttributes degAtt = degAttBldr.build();
        // TODO remove intermediate variables
        // set node-id
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-").append(degNb).toString();
        // Create ietf node setting supporting-node data
        return createTopoLayerNode(nodeId, clli)
                .setNodeId(new NodeId(nodeIdtopo))
                .withKey((new NodeKey(new NodeId(nodeIdtopo))))
                .addAugmentation(
                        new Node1Builder().setDegreeAttributes(degAtt).build())
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http
                                .org.openroadm.common.network.rev211210.Node1Builder()
                            .setNodeType(OpenroadmNodeType.DEGREE)
                            .setAdministrativeState(AdminStates.InService)
                            .setOperationalState(State.InService)
                            .build())
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                                .Node1Builder()
                            .setTerminationPoint(tpMap)
                            .build());
    }

    private static NodeBuilder createSrg(String srgNb, List<Mapping> srgListMap, String nodeId, String clli,
                                         boolean firstMount) {
        // Create tp-list
    // TODO remove intermediate variable ietflbldr
        Map<TerminationPointKey,TerminationPoint> tpMap = new HashMap<>();
        for (Mapping m : srgListMap) {
            // Add openroadm-common-network tp type augmentations
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210
                .TerminationPoint1Builder ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev211210.TerminationPoint1Builder()
                        .setAdministrativeState(TopologyUtils.setNetworkAdminState(m.getPortAdminState()))
                        .setOperationalState(TopologyUtils.setNetworkOperState(m.getPortOperState()));
            // Added states to srg port. TODO: add to mapping relation between abstracted and physical node states
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
            TerminationPoint ietfTp = createTpBldr(m.getLogicalConnectionPoint())
                .addAugmentation(ocnTp1Bldr.build())
                .build();
            tpMap.put(ietfTp.key(),ietfTp);
        }
        // Add CP to tp-list + added states. TODO: same comment as before with the relation between states
    // TODO remove intermediate variable ietflbldr
        TerminationPoint ietfTp = createTpBldr(srgNb + "-CP-TXRX")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1
                                .http.org.openroadm.common.network.rev211210.TerminationPoint1Builder()
                            .setTpType(OpenroadmTpType.SRGTXRXCP)
                            .setAdministrativeState(AdminStates.InService)
                            .setOperationalState(State.InService)
                            .build())
                .build();
        tpMap.put(ietfTp.key(),ietfTp);
        // set srg-attributes
        SrgAttributesBuilder srgAttrBldr = new SrgAttributesBuilder();
        if (firstMount) {
            srgAttrBldr.setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available());
        }
        SrgAttributes srgAttr = srgAttrBldr.build();
        // Create ietf node augmentation to support ietf tp-list
    // TODO remove intermediate variables
        // Create ietf node setting supporting-node data
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-").append(srgNb).toString();
        return createTopoLayerNode(nodeId, clli)
                .setNodeId(new NodeId(nodeIdtopo))
                .withKey((new NodeKey(new NodeId(nodeIdtopo))))
                .addAugmentation(
                        new Node1Builder().setSrgAttributes(srgAttr).build())
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210
                                .Node1Builder()
                            .setNodeType(OpenroadmNodeType.SRG)
                            .setAdministrativeState(AdminStates.InService)
                            .setOperationalState(State.InService)
                            .build())
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                                .Node1Builder()
                            .setTerminationPoint(tpMap)
                            .build());
    }

    private static NodeBuilder createTopoLayerNode(String nodeId, String clli) {
        // Sets the value of Network-ref and Node-ref as a part of the supporting node
        // attribute
    // TODO remove intermediate variables
        SupportingNode support1 = new SupportingNodeBuilder()
                .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID), new NodeId(nodeId)))
                .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
                .setNodeRef(new NodeId(nodeId))
                .build();
        SupportingNode support2 = new SupportingNodeBuilder()
                .withKey(new SupportingNodeKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID), new NodeId(clli)))
                .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                .setNodeRef(new NodeId(clli))
                .build();
        Map<SupportingNodeKey, SupportingNode> supportlist = new HashMap<>();
        supportlist.put(support1.key(), support1);
        supportlist.put(support2.key(), support2);
        return new NodeBuilder().setSupportingNode(supportlist);
    }


    // This method returns a generic termination point builder for a given tpid
    private static TerminationPointBuilder createTpBldr(String tpId) {
        TpId tp = new TpId(tpId);
        return new TerminationPointBuilder().withKey(new TerminationPointKey(tp)).setTpId(tp);
    }

    private static LinkBuilder createLink(String srcNode, String destNode, String srcTp, String destTp) {
        // TODO remove intermediate variables
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);
        return new LinkBuilder()
                        .setSource(new SourceBuilder()
                                .setSourceNode(new NodeId(srcNode))
                                .setSourceTp(new TpId(srcTp))
                        .build())
                        .setDestination(new DestinationBuilder()
                                .setDestNode(new NodeId(destNode))
                                .setDestTp(new TpId(destTp))
                        .build())
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
                srcTp = nodes.get(i)
                        .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                .ietf.network.topology.rev180226.Node1.class)
                        .nonnullTerminationPoint().values().stream()
                        .filter(tp -> tp.getTpId().getValue().contains("CP") || tp.getTpId().getValue().contains("CTP"))
                        .findFirst().get().getTpId().getValue();
                destTp = nodes.get(j)
                        .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                .ietf.network.topology.rev180226.Node1.class)
                        .nonnullTerminationPoint().values().stream()
                        .filter(tp -> tp.getTpId().getValue().contains("CP") || tp.getTpId().getValue().contains("CTP"))
                        .findFirst().get().getTpId().getValue();
                Link1Builder ocnAzLinkBldr = new Link1Builder();
                int srcNodeType = nodes.get(i).augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.common.network.rev211210.Node1.class).getNodeType().getIntValue();
                int destNodeType = nodes.get(j).augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.common.network.rev211210.Node1.class).getNodeType().getIntValue();
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
                // Add state to link. Based on the operational state of the TPs at the edge of the link.
                // Similar to getting srcTp and destTp
                State srcTpState = nodes.get(i)
                        .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                            .ietf.network.topology.rev180226.Node1.class)
                        .getTerminationPoint().values().stream()
                        .filter(tp -> tp.getTpId().getValue().contains("CP") || tp.getTpId().getValue().contains("CTP"))
                        .findFirst().get()
                        .augmentation(org.opendaylight.yang.gen.v1.http
                                .org.openroadm.common.network.rev211210.TerminationPoint1.class)
                        .getOperationalState();
                State destTpState = nodes.get(j)
                        .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                            .ietf.network.topology.rev180226.Node1.class)
                        .getTerminationPoint().values().stream()
                        .filter(tp -> tp.getTpId().getValue().contains("CP") || tp.getTpId().getValue().contains("CTP"))
                        .findFirst().get()
                        .augmentation(org.opendaylight.yang.gen.v1.http
                                .org.openroadm.common.network.rev211210.TerminationPoint1.class)
                        .getOperationalState();
                if (State.InService.equals(srcTpState) && State.InService.equals(destTpState)) {
                    ocnAzLinkBldr.setAdministrativeState(AdminStates.InService)
                            .setOperationalState(State.InService);
                    ocnZaLinkBldr.setAdministrativeState(AdminStates.InService)
                            .setOperationalState(State.InService);
                } else {
                    ocnAzLinkBldr.setAdministrativeState(AdminStates.OutOfService)
                            .setOperationalState(State.OutOfService);
                    ocnZaLinkBldr.setAdministrativeState(AdminStates.OutOfService)
                            .setOperationalState(State.OutOfService);
                }
                // set opposite link augmentations
                LinkBuilder ietfAzLinkBldr = createLink(srcNode, destNode, srcTp, destTp);
                LinkBuilder ietfZaLinkBldr = createLink(destNode, srcNode, destTp, srcTp);
                ocnAzLinkBldr.setOppositeLink(ietfZaLinkBldr.getLinkId());
                ietfAzLinkBldr.addAugmentation(ocnAzLinkBldr.build());
                ocnZaLinkBldr.setOppositeLink(ietfAzLinkBldr.getLinkId());
                ietfZaLinkBldr.addAugmentation(ocnZaLinkBldr.build());
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
            InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class)
                .child(Link.class, new LinkKey(linkId));
            java.util.Optional<Link> link =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID.build()).get();
            if (link.isPresent()) {
                LinkBuilder linkBuilder = new LinkBuilder(link.get());
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.Link1Builder link1Builder =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210.Link1Builder(
                        linkBuilder
                            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev211210
                                .Link1.class));
                linkBuilder.removeAugmentation(Link1.class)
                        .addAugmentation(link1Builder.build());
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(),
                        linkBuilder.build());
                networkTransactionService.commit().get(1, TimeUnit.SECONDS);
                return true;
            // TODO use guard clause style to decrease indentation in the previous block
            } else {
                LOG.error("No link found for given LinkId: {}", linkId);
                return false;
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error deleting link {}", linkId.getValue(), e);
            return false;
        }
    }

    /**
     * Get a builder for instance identifier related to common network termination point.
     * @param nodeId String
     * @param tpId String
     * @return InstanceIdentifierBuilder
     */
    public static InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210
            .TerminationPoint1> createCommonNetworkTerminationPointIIDBuilder(String nodeId, String tpId) {
        return InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                         .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .Node1.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                         .network.node.TerminationPoint.class,
                    new TerminationPointKey(new TpId(tpId)))
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210
                        .TerminationPoint1.class);
    }

    /**
     * Get a builder for instance identifier related to network termination point.
     * @param nodeId String
     * @param tpId String
     * @return InstanceIdentifierBuilder
     */
    public static InstanceIdentifierBuilder<TerminationPoint1> createNetworkTerminationPointIIDBuilder(String nodeId,
                                                                                                       String tpId) {
        return InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.node.TerminationPoint.class,
                    new TerminationPointKey(new TpId(tpId)))
                .augmentation(TerminationPoint1.class);
    }

    /**
     * Get an instance identifier related to network node.
     * @param nodeId String
     * @return InstanceIdentifier
     */
    public static InstanceIdentifier<Node1> createNetworkNodeIID(String nodeId) {
        return InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                     new NodeKey(new NodeId(nodeId)))
                .augmentation(Node1.class).build();
    }

    /**
     * Get an instance identifier related to common network node.
     * @param nodeId String
     * @return InstanceIdentifier
     */
    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210
            .Node1> createCommonNetworkNodeIID(String nodeId) {
        return InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.Node1.class)
                .build();
    }
}
