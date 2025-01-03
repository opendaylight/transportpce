/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import static org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes.Tpdr;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.mode.attributes.SupportedOperationalModesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.mode.attributes.supported.operational.modes.OperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.mode.attributes.supported.operational.modes.OperationalModeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.mode.attributes.supported.operational.modes.OperationalModeKey;
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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to implement the org-openroadm-network-topology layer.
 */
public final class OpenRoadmTopology {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmTopology.class);

    private static Map<String, OpenroadmTpType> PORTQUAL_ORD_TYPE_MAP = Map.of(
        "xpdr-network", OpenroadmTpType.XPONDERNETWORK,
        "switch-network", OpenroadmTpType.XPONDERNETWORK,
        "xpdr-client", OpenroadmTpType.XPONDERCLIENT
    );
    private static Map<String, List<OpenroadmTpType>> PORTDIR_ORD_TYPE_MAP = Map.of(
        "bidirectional", List.of(OpenroadmTpType.DEGREETXRXTTP, OpenroadmTpType.SRGTXRXPP),
        "tx", List.of(OpenroadmTpType.DEGREETXTTP, OpenroadmTpType.SRGTXPP),
        "rx", List.of(OpenroadmTpType.DEGREERXTTP, OpenroadmTpType.SRGRXPP)
    );

    private OpenRoadmTopology() {
    }

    /**
     * Create a Nodes and Links in the openroadm topology depending on the type of device.
     * @param mappingNode Abstracted view of the node retrieved from the portmapping data-store
     * @return Subset of the topology
     */
    public static TopologyShard createTopologyShard(Nodes mappingNode) {
        return createTopologyShard(mappingNode, true);
    }

    /**
     * Create a Nodes and Links in the openroadm topology depending on the type of device.
     * @param mappingNode Abstracted view of the node retrieved from the portmapping data-store
     * @param firstMount Allow to distinguish if this is a new node creation or a netconf session reinitialization
     * @return Subset of the topology
     */
    public static TopologyShard createTopologyShard(Nodes mappingNode, boolean firstMount) {
        switch (mappingNode.getNodeInfo().getNodeType()) {
            case Rdm :
                return createRdmTopologyShard(mappingNode, firstMount);
            case Xpdr :
                return createXpdrTopologyShard(mappingNode);
            default :
                LOG.error("Device node Type not managed yet");
                return null;
        }
    }

    /**
     * Create the Node and Link elements of the topology when the node is of ROADM type.
     * @param mappingNode Abstracted view of the node retrieved from the portmapping data-store
     * @param firstMount Allow to distinguish if this is a new node creation or a netconf session reinitialization
     * @return topology with new Node and Links
     */
    public static TopologyShard createRdmTopologyShard(Nodes mappingNode, boolean firstMount) {
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
        List<Node> nodes = new ArrayList<>();
        // create degree nodes
        for (Map.Entry<String, List<Mapping>> entry : mapDeg.entrySet()) {
            nodes.add(
                createDegree(entry.getKey(), entry.getValue(), mappingNode.getNodeId(),
                        mappingNode.getNodeInfo().getNodeClli(), firstMount)
                    .build());
        }
        // create srg nodes
        for (Map.Entry<String, List<Mapping>> entry : mapSrg.entrySet()) {
            nodes.add(
                createSrg(entry.getKey(), entry.getValue(), mappingNode.getNodeId(),
                        mappingNode.getNodeInfo().getNodeClli(), firstMount)
                    .build());
        }
        LOG.info("adding links numOfDegrees={} numOfSrgs={}", mapDeg.size(), mapSrg.size());
        List<Link> links = createNewLinks(nodes);
        LOG.info("created nodes/links: {}/{}", nodes.size(), links.size());
        return new TopologyShard(nodes, links);
    }

    /**
     * Create the Node and Link elements of the topology when the node is of XPDR type.
     * @param mappingNode Abstracted view of the node retrieved from the portmapping data-store
     * @return topology with new Node and Links
     */
    public static TopologyShard createXpdrTopologyShard(Nodes mappingNode) {
        List<Node> nodes = new ArrayList<>();
        List<Mapping> networkMappings =
                mappingNode.nonnullMapping().values()
                        .stream().filter(k -> k.getLogicalConnectionPoint().contains("NETWORK"))
                        .collect(Collectors.toList());
        List<Integer> tpdrList = new ArrayList<>();
        for (Mapping mapping : networkMappings) {
            Integer xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
            if (!tpdrList.contains(xpdrNb)) {
                tpdrList.add(xpdrNb);
                List<Mapping> extractedMappings = mappingNode.nonnullMapping().values()
                        .stream().filter(lcp -> lcp.getLogicalConnectionPoint().contains("XPDR" + xpdrNb))
                        .collect(Collectors.toList());
                Boolean isOtn;
                String xpdrType;
                switch (mapping.getXpdrType() == null ? Tpdr : mapping.getXpdrType()) {
                    case Tpdr :
                        isOtn = false;
                        xpdrType = "Tpdr";
                        break;
                    case Mpdr :
                    case Switch :
                        isOtn = true;
                        xpdrType = mapping.getXpdrType().getName();
                        break;
                    // Both regen and regen-uni are added here, though initial support is
                    // only for regen xpdr-type
                    case Regen:
                    case RegenUni:
                        isOtn = false;
                        xpdrType = mapping.getXpdrType().getName();
                        break;
                    default :
                        LOG.warn("cannot create xpdr node {} in openroadm-topology: type {} not supported",
                             mappingNode.getNodeId() + "-XPDR" + xpdrNb, mapping.getXpdrType().getName());
                        continue;
                }
                LOG.info("creating xpdr node {} of type {} in openroadm-topology",
                        mappingNode.getNodeId() + "-XPDR" + xpdrNb, xpdrType);
                nodes.add(createXpdr(
                                mappingNode.getNodeId(),
                                mappingNode.getNodeInfo().getNodeClli(),
                                xpdrNb,
                                extractedMappings,
                                isOtn)
                          .build());
            }
        }
        return nodes.isEmpty() ? null : new TopologyShard(nodes, new ArrayList<Link>());
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
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                        .setNodeType(OpenroadmNodeType.XPONDER)
                        .setAdministrativeState(AdminStates.InService)
                        .setOperationalState(State.InService)
                        .build());
        // Create tp-map
        Map<TerminationPointKey, TerminationPoint> tpMap = new HashMap<>();
        for (Mapping m : mappings) {
            if (!PORTQUAL_ORD_TYPE_MAP.containsKey(m.getPortQual())) {
                continue;
            }
            if (isOtn && m.getPortQual().equals("xpdr-client")) {
                continue;
            }
            var ocnTp1Bldr = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                    .TerminationPoint1Builder()
                .setTpType(PORTQUAL_ORD_TYPE_MAP.get(m.getPortQual()))
                .setAdministrativeState(TopologyUtils.setNetworkAdminState(m.getPortAdminState()))
                .setOperationalState(TopologyUtils.setNetworkOperState(m.getPortOperState()));
            if (!isOtn && m.getConnectionMapLcp() != null) {
                ocnTp1Bldr.setAssociatedConnectionMapTp(Set.of(new TpId(m.getConnectionMapLcp())));
            }

            TerminationPointBuilder ietfTpBldr = createTpBldr(m.getLogicalConnectionPoint())
                // Add openroadm-common tp augmentations
                .addAugmentation(ocnTp1Bldr.build());

            if (m.getPortQual().equals("xpdr-network") && m.getSupportedOperationalMode() != null
                    && !m.getSupportedOperationalMode().isEmpty()) {

                Map<OperationalModeKey, OperationalMode> mapSopm = new HashMap<>();
                for (String opMode : m.getSupportedOperationalMode()) {
                    mapSopm.put(new OperationalModeKey(opMode),
                        new OperationalModeBuilder()
                            .setModeId(opMode)
                            //TODO : fill SpectralWidth with from Catalog
                            .setSpectralWidth(new FrequencyGHz(Decimal64.valueOf(0.0, RoundingMode.DOWN)))
                            .build());
                }
                XpdrNetworkAttributes xna = new XpdrNetworkAttributesBuilder()
                    .setSupportedOperationalModes(
                        new SupportedOperationalModesBuilder().setOperationalMode(mapSopm).build())
                    .build();
                // Add openroadm-network-topology tp augmentations
                ietfTpBldr.addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                            .TerminationPoint1Builder()
                        .setXpdrNetworkAttributes(xna)
                        .build());
            }

            TerminationPoint ietfTp = ietfTpBldr.build();
            tpMap.put(ietfTp.key(),ietfTp);

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
            // Added states to degree port. TODO: add to mapping relation between abstracted and physical node states
            if (!PORTDIR_ORD_TYPE_MAP.containsKey(m.getPortDirection())) {
                LOG.error("impossible to set tp-type to {}", m.getLogicalConnectionPoint());
            }
            TerminationPoint ietfTp =  createTpBldr(m.getLogicalConnectionPoint())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1Builder()
                        .setTpType(PORTDIR_ORD_TYPE_MAP.get(m.getPortDirection()).get(0))
                        .setAdministrativeState(TopologyUtils.setNetworkAdminState(m.getPortAdminState()))
                        .setOperationalState(TopologyUtils.setNetworkOperState(m.getPortOperState()))
                        .build())
                .build();
            tpMap.put(ietfTp.key(),ietfTp);
        }
        // Add CTP to tp-list + added states. TODO: same comment as before with the relation between states
        TerminationPoint ietfTp = createTpBldr(degNb + "-CTP-TXRX")
                .addAugmentation(new org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev230526.TerminationPoint1Builder()
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
        // set node-id
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-").append(degNb).toString();
        // Create ietf node setting supporting-node data
        return createTopoLayerNode(nodeId, clli)
                .setNodeId(new NodeId(nodeIdtopo))
                .withKey((new NodeKey(new NodeId(nodeIdtopo))))
                .addAugmentation(new Node1Builder().setDegreeAttributes(degAtt).build())
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .Node1Builder()
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
        Map<TerminationPointKey,TerminationPoint> tpMap = new HashMap<>();
        for (Mapping m : srgListMap) {
            // Added states to srg port. TODO: add to mapping relation between abstracted and physical node states
            if (!PORTDIR_ORD_TYPE_MAP.containsKey(m.getPortDirection())) {
                LOG.error("impossible to set tp-type to {}", m.getLogicalConnectionPoint());
            }
            TerminationPoint ietfTp = createTpBldr(m.getLogicalConnectionPoint())
                .addAugmentation(
                    // Add openroadm-common-network tp type augmentations
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1Builder()
                        .setTpType(PORTDIR_ORD_TYPE_MAP.get(m.getPortDirection()).get(1))
                        .setAdministrativeState(TopologyUtils.setNetworkAdminState(m.getPortAdminState()))
                        .setOperationalState(TopologyUtils.setNetworkOperState(m.getPortOperState()))
                        .build())
                .build();
            tpMap.put(ietfTp.key(),ietfTp);
        }
        // Add CP to tp-list + added states. TODO: same comment as before with the relation between states
        TerminationPoint ietfTp = createTpBldr(srgNb + "-CP-TXRX")
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                        .TerminationPoint1Builder()
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
        // Create ietf node setting supporting-node data
        String nodeIdtopo = new StringBuilder().append(nodeId).append("-").append(srgNb).toString();
        return createTopoLayerNode(nodeId, clli)
            .setNodeId(new NodeId(nodeIdtopo))
            .withKey((new NodeKey(new NodeId(nodeIdtopo))))
            .addAugmentation(new Node1Builder().setSrgAttributes(srgAttr).build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
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
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);
        return new LinkBuilder()
                .setSource(
                    new SourceBuilder()
                        .setSourceNode(new NodeId(srcNode))
                        .setSourceTp(new TpId(srcTp))
                        .build())
                .setDestination(
                    new DestinationBuilder()
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
                        .findFirst().orElseThrow().getTpId().getValue();
                destTp = nodes.get(j)
                        .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                .ietf.network.topology.rev180226.Node1.class)
                        .nonnullTerminationPoint().values().stream()
                        .filter(tp -> tp.getTpId().getValue().contains("CP") || tp.getTpId().getValue().contains("CTP"))
                        .findFirst().orElseThrow().getTpId().getValue();
                Link1Builder ocnAzLinkBldr = new Link1Builder();
                Link1Builder ocnZaLinkBldr = new Link1Builder();
                int srcNodeType = nodes.get(i).augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.common.network.rev230526.Node1.class).getNodeType().getIntValue();
                int destNodeType = nodes.get(j).augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.common.network.rev230526.Node1.class).getNodeType().getIntValue();

                if (srcNodeType == 11 && destNodeType == 11) {
                    ocnAzLinkBldr.setLinkType(OpenroadmLinkType.EXPRESSLINK);
                    ocnZaLinkBldr.setLinkType(OpenroadmLinkType.EXPRESSLINK);
                } else if (srcNodeType == 11 && destNodeType == 12) {
                    ocnAzLinkBldr.setLinkType(OpenroadmLinkType.DROPLINK);
                    ocnZaLinkBldr.setLinkType(OpenroadmLinkType.ADDLINK);
                } else if (srcNodeType == 12 && destNodeType == 11) {
                    ocnAzLinkBldr.setLinkType(OpenroadmLinkType.ADDLINK);
                    ocnZaLinkBldr.setLinkType(OpenroadmLinkType.DROPLINK);
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
                        .findFirst().orElseThrow()
                        .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1.class)
                        .getOperationalState();
                State destTpState = nodes.get(j)
                        .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                            .ietf.network.topology.rev180226.Node1.class)
                        .getTerminationPoint().values().stream()
                        .filter(tp -> tp.getTpId().getValue().contains("CP") || tp.getTpId().getValue().contains("CTP"))
                        .findFirst().orElseThrow()
                        .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1.class)
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

    /**
     * Update the status of a link in the openroadm topology when we delete a service.
     * @param srcNode Node name at one link end
     * @param dstNode Node name at the other link end
     * @param srcTp Terminatin point id on srcNode
     * @param destTp Terminatin point id on dstNode
     * @param networkTransactionService Service that eases the transaction operations with data-stores
     * @return True if ok, False otherwise
     */
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

    /**
     * Update the status of a link in the openroadm topology when we delete a service.
     * @param linkId Id of the link to update
     * @param networkTransactionService Service that eases the transaction operations with data-stores
     * @return True if ok, False otherwise
     */
    public static boolean deleteLinkLinkId(LinkId linkId , NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for LinkId: {}", linkId.getValue());
        try {
            DataObjectIdentifier<Link> linkIID = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class)
                .child(Link.class, new LinkKey(linkId))
                .build();
            java.util.Optional<Link> link =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID).get();
            if (link.isEmpty()) {
                LOG.error("No link found for given LinkId: {}", linkId);
                return false;
            }
            LinkBuilder linkBuilder = new LinkBuilder(link.orElseThrow());
            networkTransactionService.merge(
                LogicalDatastoreType.CONFIGURATION,
                linkIID,
                linkBuilder
                    .removeAugmentation(Link1.class)
                    .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                            .Link1Builder(linkBuilder
                                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                                    .Link1.class))
                                .build())
                    .build());
            networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            return true;

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
    public static DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
            .TerminationPoint1> createCommonNetworkTerminationPointIIDBuilder(String nodeId, String tpId) {
        return DataObjectIdentifier
                .builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .Node1.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.node.TerminationPoint.class,
                    new TerminationPointKey(new TpId(tpId)))
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                        .TerminationPoint1.class)
                .build();
    }

    /**
     * Get an instance identifier related to network node.
     * @param nodeId String
     * @return InstanceIdentifier
     */
    public static DataObjectIdentifier<Node1> createNetworkNodeIID(String nodeId) {
        return DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                     new NodeKey(new NodeId(nodeId)))
                .augmentation(Node1.class)
                .build();
    }

    /**
     * Get an instance identifier related to common network node.
     * @param nodeId String
     * @return InstanceIdentifier
     */
    public static DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
            .Node1> createCommonNetworkNodeIID(String nodeId) {
        return DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                        .networks.network.Node.class,
                    new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                .build();
    }
}
