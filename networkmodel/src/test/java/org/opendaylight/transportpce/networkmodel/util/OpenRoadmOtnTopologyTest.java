/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.transportpce.networkmodel.util.test.NetworkmodelTestUtil;
import org.opendaylight.transportpce.test.converter.JsonDataConverter;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.SwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GEODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmOtnTopologyTest {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmOtnTopologyTest.class);
    private Nodes portMappingTpdr;
    private Nodes portMappingSpdr;
    private Nodes portMappingBad;

    public OpenRoadmOtnTopologyTest() {
        try (Reader reader = new FileReader("src/test/resources/portMapping.json", StandardCharsets.UTF_8)) {
            Network portMapping = (Network) new JsonDataConverter(null).deserialize(reader, Network.QNAME);
            for (Nodes nodes : portMapping.nonnullNodes().values()) {
                if (nodes.getNodeId().equals("XPDR-A1")) {
                    this.portMappingTpdr = nodes;
                } else {
                    this.portMappingSpdr = nodes;
                }
            }
            Map<MappingKey,Mapping> mappingList = new HashMap<>();
            Mapping mapping = new MappingBuilder().setLogicalConnectionPoint("XPDR0-NETWORK0").build();
            mappingList.put(mapping.key(),mapping);
            this.portMappingBad = new NodesBuilder()
                .setNodeId(this.portMappingTpdr.getNodeId())
                .setNodeInfo(new NodeInfoBuilder(this.portMappingTpdr.getNodeInfo()).setNodeType(NodeTypes.Ila).build())
                .setMapping(mappingList)
                .build();
            LOG.info("tpdr portMapping = {}", this.portMappingTpdr.toString());
            LOG.info("spdr portMapping = {}", this.portMappingSpdr.toString());
            LOG.info("ila portMapping = {}", this.portMappingBad.toString());
        } catch (IOException e) {
            LOG.error("Cannot init OpenRoadmOtnTopologyTest ", e);
            fail("Cannot init OpenRoadmOtnTopologyTest ");
        }
    }

    @Test
    void createTopologyShardForTpdrTest() {
        TopologyShard topologyShard = OpenRoadmOtnTopology.createTopologyShard(this.portMappingTpdr);
        assertNotNull(topologyShard, "TopologyShard should never be null");
        assertEquals(1, topologyShard.getNodes().size(), "Should contain a single node");
        assertEquals(0, topologyShard.getLinks().size(), "Should contain no link");
        Node node = topologyShard.getNodes().get(0);
        assertEquals("XPDR-A1-XPDR1", node.getNodeId().getValue());
        // tests supporting nodes
        List<SupportingNode> supportingNodes = node.nonnullSupportingNode().values().stream()
            .sorted((sn1, sn2) -> sn1.getNetworkRef().getValue().compareTo(sn2.getNetworkRef().getValue()))
            .collect(Collectors.toList());
        assertEquals(3, supportingNodes.size(),"Should contain 3 supporting nodes");
        assertEquals(supportingNodes.get(0).getNetworkRef().getValue(), "clli-network");
        assertEquals(supportingNodes.get(0).getNodeRef().getValue(), "NodeA");
        assertEquals(supportingNodes.get(1).getNetworkRef().getValue(), "openroadm-network");
        assertEquals(supportingNodes.get(1).getNodeRef().getValue(), "XPDR-A1");
        assertEquals(supportingNodes.get(2).getNetworkRef().getValue(), "openroadm-topology");
        assertEquals(supportingNodes.get(2).getNodeRef().getValue(), "XPDR-A1-XPDR1");
        assertEquals(OpenroadmNodeType.TPDR, node.augmentation(Node1.class).getNodeType());
        assertEquals(
            Uint16.valueOf(1),
            node.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Node1.class)
                    .getXpdrAttributes().getXpdrNumber());
        //tests list of TPs
        List<TerminationPoint> tps = node.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .nonnullTerminationPoint().values().stream()
                .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                .collect(Collectors.toList());
        assertEquals(4, tps.size(), "node should contain 4 TPs");
        //tests client tp
        assertEquals("XPDR1-CLIENT1", tps.get(0).getTpId().getValue());
        assertEquals(
            "XPDR1-NETWORK1",
            tps.get(0).augmentation(
                        TerminationPoint1.class).getAssociatedConnectionMapTp().iterator().next().getValue());
        assertEquals(
            1,
            tps.get(0).augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                    .getTpSupportedInterfaces().getSupportedInterfaceCapability().size(),
            "only If100GE interface capabitily expected");
        assertEquals(
            If100GE.VALUE,
            tps.get(0).augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                    .getTpSupportedInterfaces().nonnullSupportedInterfaceCapability().values().stream().findFirst()
                        .orElseThrow().getIfCapType());
        assertEquals(OpenroadmTpType.XPONDERCLIENT,tps.get(0).augmentation(TerminationPoint1.class).getTpType(),
            "first TP must be of type client");
        //tests network tp
        assertEquals("XPDR1-NETWORK1", tps.get(2).getTpId().getValue());
        assertEquals("XPDR1-CLIENT1", tps.get(2).augmentation(TerminationPoint1.class)
                    .getAssociatedConnectionMapTp().iterator().next().getValue());
        assertEquals(
            1,
            tps.get(2).augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                    .getTpSupportedInterfaces().getSupportedInterfaceCapability().size());
        assertEquals(
            IfOCH.VALUE,
            tps.get(2).augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                    .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().stream().findFirst()
                        .orElseThrow().getIfCapType());
        assertNull(
            tps.get(2).augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                    .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getRate(),
            "the rate should be null");
        assertEquals(OpenroadmTpType.XPONDERNETWORK,tps.get(2).augmentation(TerminationPoint1.class).getTpType(),
            "third TP must be of type network");
    }

    @Test
    void createTopologyShardForSpdrTest() {
        TopologyShard topologyShard = OpenRoadmOtnTopology.createTopologyShard(this.portMappingSpdr);
        assertNotNull(topologyShard, "TopologyShard should never be null");
        assertEquals(2, topologyShard.getNodes().size(), "Should contain two nodes");
        assertEquals(0, topologyShard.getLinks().size(), "Should contain no link");
        List<Node> nodes = topologyShard.getNodes().stream()
            .sorted((n1, n2) -> n1.getNodeId().getValue().compareTo(n2.getNodeId().getValue()))
            .collect(Collectors.toList());
        for (Node node : nodes) {
            checkSpdrNode(node);
        }
    }

    @Test
    void createOtnLinksForOTU4NormalTest() {
        String nodeA = "SPDRA";
        String tpA = "XPDR1-NETWORK1";
        String nodeZ = "SPDRZ";
        String tpZ = "XPDR1-NETWORK1";
        List<Link> links = OpenRoadmOtnTopology.createOtnLinks(nodeA, tpA, nodeZ, tpZ, OtnLinkType.OTU4).getLinks();
        assertEquals(2, links.size(), "2 OTU4 links should have been created");
        List<Link> sortedLinks = links.stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals(
            "OTU4-SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(0).getLinkId().getValue(),
            "name of OTU4 linkid AZ");
        assertEquals(
            "OTU4-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(1).getLinkId().getValue(),
            "name of OTU4 linkid ZA");
        assertEquals("SPDRA-XPDR1", sortedLinks.get(0).getSource().getSourceNode().getValue());
        assertEquals("SPDRZ-XPDR1", sortedLinks.get(0).getDestination().getDestNode().getValue());
        assertEquals("SPDRZ-XPDR1", sortedLinks.get(1).getSource().getSourceNode().getValue());
        assertEquals("SPDRA-XPDR1", sortedLinks.get(1).getDestination().getDestNode().getValue());
        assertEquals(
            Uint32.valueOf(100000),
            sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth(),
            "available BW at OTU4 creation should be 100G (100000)");
        assertEquals(
            Uint32.valueOf(0),
            sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth(),
            "used BW at OTU4 creation should be 0");
        assertEquals(
            OpenroadmLinkType.OTNLINK,
            sortedLinks.get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class)
                .getLinkType());
        assertEquals(
            "OTU4-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class)
                .getOppositeLink().getValue(),
            "opposite link must be present");
        assertEquals(
            OtnLinkType.OTU4,
            sortedLinks.get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.Link1.class)
                .getOtnLinkType(),
            "otn link type should be OTU4");
    }

    @Test
    void createOtnLinksForNotManagedOtnlinktypeTest() {
        String nodeA = "SPDRA";
        String tpA = "XPDR1-NETWORK1";
        String nodeZ = "SPDRZ";
        String tpZ = "XPDR1-NETWORK1";
        TopologyShard topoShard = OpenRoadmOtnTopology.createOtnLinks(nodeA, tpA, nodeZ, tpZ, OtnLinkType.ODU0);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertNull(topoShard.getNodes(), "TopologyShard should not contain any node");
        assertNull(topoShard.getLinks(), "TopologyShard should not contain any link");
    }

    @Test
    void createOtnLinksForODU4NormalTest() {
        TopologyShard topoShard = OpenRoadmOtnTopology
            .createOtnLinks(
                NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.OTU4, Uint32.valueOf(100000)),
                NetworkmodelTestUtil.createTpList(false), OtnLinkType.ODTU4);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertNull(topoShard.getNodes(), "list of nodes should be null");
        List<Link> sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals(4, sortedLinks.size(), "list of links should contain 4 links");
        assertTrue(sortedLinks.get(2).getLinkId().getValue().startsWith("OTU4-"), "link 3 should be of type OTU4");
        assertEquals(
            Uint32.valueOf(0),
            sortedLinks.get(2).augmentation(Link1.class).getAvailableBandwidth(),
            "after odu4 creation, available BW of supported OTU4 should be 0");
        assertEquals(
            Uint32.valueOf(100000),
            sortedLinks.get(2).augmentation(Link1.class).getUsedBandwidth(),
            "after odu4 creation, used BW of supported OTU4 should be 100 000");
        assertEquals(
            "ODTU4-SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(0).getLinkId().getValue());
        assertEquals(
            "ODTU4-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(1).getLinkId().getValue());
        assertEquals("SPDRA-XPDR1", sortedLinks.get(0).getSource().getSourceNode().getValue());
        assertEquals("SPDRZ-XPDR1", sortedLinks.get(0).getDestination().getDestNode().getValue());
        assertEquals("SPDRZ-XPDR1", sortedLinks.get(1).getSource().getSourceNode().getValue());
        assertEquals("SPDRA-XPDR1", sortedLinks.get(1).getDestination().getDestNode().getValue());
        assertEquals(
            Uint32.valueOf(100000),
            sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth(),
            "after odu4 creation, its available BW should be 100 000");
        assertEquals(
            Uint32.valueOf(0),
            sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth(),
            "after odu4 creation, its used BW should be 0");
        assertEquals(
            OpenroadmLinkType.OTNLINK,
            sortedLinks.get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class)
                .getLinkType());
        assertEquals(
            "ODTU4-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1.class)
                .getOppositeLink().getValue(),
            "opposite link must be present");
        assertEquals(
            OtnLinkType.ODTU4,
            sortedLinks.get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.Link1.class)
                .getOtnLinkType(),
            "otn link type should be ODTU4");

        assertEquals(2, topoShard.getTps().size(), "list of TPs should contain 2 updated TPs");
        assertNotNull(
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool(),
            "after ODU4 creation, its termination point should contain a TsPool list");
        assertEquals(
            80,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool().size(),
            "Ts pool list should be full, with 80 trib slots");
        assertNotNull(
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool(),
            "after ODU4 creation, its termination point should contain a TpnPool list");
        assertEquals(
            80,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool().size(),
            "Tpn pool list should be full, with 80 trib ports");
    }

    @Test
    void createOtnLinksForODU4WhenOTU4HaveBadBWParamsTest() {
        List<Link> otu4Links = NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.OTU4, Uint32.valueOf(100000));
        List<Link> otu4LinksWithBadBWParam = new ArrayList<>();
        for (Link link : otu4Links) {
            otu4LinksWithBadBWParam.add(new LinkBuilder(link).removeAugmentation(Link1.class).build());
        }
        TopologyShard topoShard = OpenRoadmOtnTopology.createOtnLinks(
                otu4LinksWithBadBWParam,
                NetworkmodelTestUtil.createTpList(false),
                OtnLinkType.OTU4);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertNull(topoShard.getNodes(), "list of nodes should be null");
        assertNull(topoShard.getLinks(), "list of links should be null");
        assertNull(topoShard.getTps(), "list of tps should be null");

        otu4LinksWithBadBWParam.clear();
        topoShard = OpenRoadmOtnTopology.createOtnLinks(
                NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.OTU4, Uint32.valueOf(99000)),
                NetworkmodelTestUtil.createTpList(false),
                OtnLinkType.OTU4);
        assertNull(topoShard.getNodes(), "list of nodes should be null");
        assertNull(topoShard.getLinks(), "list of links should be null");
        assertNull(topoShard.getTps(), "list of tps should be null");
    }

    @Test
    void deleteOtnLinksForODU4NormalTest() {
        TopologyShard topoShard = OpenRoadmOtnTopology.deleteOtnLinks(
                    NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.OTU4, Uint32.valueOf(0)),
                    NetworkmodelTestUtil.createTpList(true),
                    OtnLinkType.OTU4);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertEquals(2, topoShard.getLinks().size(), "list of links should contain 2 links");
        assertEquals(
            Uint32.valueOf(100000),
            topoShard.getLinks().get(0).augmentation(Link1.class).getAvailableBandwidth(),
            "after ODU4 deletion, available BW of supported OTU4 should be 100 000");
        assertEquals(
            Uint32.valueOf(0),
            topoShard.getLinks().get(0).augmentation(Link1.class).getUsedBandwidth(),
            "after ODU4 deletion, used BW of supported OTU4 should be 0");

        assertEquals(2, topoShard.getTps().size(), "list of TPs should contain 2 updated TPs");
        assertNull(
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool(),
            "after ODU4 deletion, its termination points should not contain any TsPool list");
        assertNull(
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool(),
            "after ODU4 deletion, its termination points should not contain any TpnPool list");
    }

    @Test
    void deleteOtnLinksForODU4WhenOTU4HaveBadBWParamsTest() {
        List<Link> otu4Links = NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.OTU4, Uint32.valueOf(0));
        List<Link> otu4LinksWithBadBWParam = new ArrayList<>();
        for (Link link : otu4Links) {
            otu4LinksWithBadBWParam.add(new LinkBuilder(link).removeAugmentation(Link1.class).build());
        }
        TopologyShard topoShard = OpenRoadmOtnTopology.deleteOtnLinks(
                otu4LinksWithBadBWParam,
                NetworkmodelTestUtil.createTpList(true),
                OtnLinkType.OTU4);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertNull(topoShard.getNodes(), "list of nodes should be null");
        assertNull(topoShard.getLinks(), "list of links should be null");
        assertNull(topoShard.getTps(), "list of tps should be null");
    }

    @Test
    void updateOtnLinksFor10GTest() {
        // tests update for 10G creation
        TopologyShard topoShard = OpenRoadmOtnTopology.updateOtnLinks(
                    NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.ODTU4, Uint32.valueOf(100000)),
                    NetworkmodelTestUtil.createTpList(true),
                    Uint32.valueOf(10), (short)1, (short)1, (short)8, false);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertNull(topoShard.getNodes(), "list of nodes should be null");
        List<Link> sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, sortedLinks.size(), "list of links should contain 2 links");
        assertTrue(sortedLinks.get(0).getLinkId().getValue().startsWith("ODTU4-"));
        assertEquals(
            Uint32.valueOf(90000),
            sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth(),
            "after 10G creation, available BW of supported ODU4 should be 90000");
        assertEquals(
            Uint32.valueOf(10000),
            sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth(),
            "after 10G creation, used BW of supported ODU4 should be 10000");

        assertEquals(
            72,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool().size(),
            "after 10G creation, 8 (over 80) trib slot should be occupied");
        assertThat(
            "trib slot 1-8 should no longer be present in Trib slot list",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool(),
            not(hasItems(Uint16.valueOf(1), Uint16.valueOf(8))));
        assertThat(
            "trib slot 9 should always be present in trib slot list",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool(),
            hasItem(Uint16.valueOf(9)));
        assertEquals(
            79,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool().size(),
            "after 10G creation, 1 (over 80) trib port should be occupied");
        assertThat(
            "trib port 1 should no longer be present",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool(),
            not(hasItem(Uint16.valueOf(1))));

        // tests update for 10G deletion
        sortedLinks.clear();
        topoShard = OpenRoadmOtnTopology.updateOtnLinks(topoShard.getLinks(), topoShard.getTps(), Uint32.valueOf(10),
                (short)1, (short)1, (short)8, true);
        sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, sortedLinks.size(), "list of links should contain 2 links");
        assertTrue(sortedLinks.get(0).getLinkId().getValue().startsWith("ODTU4-"));
        assertEquals(
            Uint32.valueOf(100000),
            sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth(),
            "after 10G deletion, available BW of supported ODU4 should be 100 000");
        assertEquals(
            Uint32.valueOf(0),
            sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth(),
            "after 10G deletion, used BW of supported ODU4 should be 0");

        assertEquals(
            80,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool().size(),
            "after 10G deletion, trib slot list should be full");
        assertThat(
            "after 10G deletion, trib slot list should contain items 1-8",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool(),
            hasItems(Uint16.valueOf(1), Uint16.valueOf(8), Uint16.valueOf(9)));
        assertEquals(
                80,
                topoShard.getTps().get(0).augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                            .TerminationPoint1.class)
                    .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                    .getTpnPool().size(),
                "after 10G deletion, trib port list should be full");
        assertThat(
            "after 10G deletion, trib port list should contain items 1",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool(),
            hasItem(Uint16.valueOf(1)));
    }

    @Test
    void updateOtnLinksFor1GCreationTest() {
        // tests update for 1G creation
        TopologyShard topoShard = OpenRoadmOtnTopology.updateOtnLinks(
                    NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.ODTU4, Uint32.valueOf(100000)),
                    NetworkmodelTestUtil.createTpList(true),
                    Uint32.valueOf(1), (short)1, (short)1, (short)1, false);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertNull(topoShard.getNodes(), "list of nodes should be null");
        List<Link> sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, sortedLinks.size(), "list of links should contain 2 links");
        assertTrue(sortedLinks.get(0).getLinkId().getValue().startsWith("ODTU4-"));
        assertEquals(
            Uint32.valueOf(99000),
            sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth(),
            "after 1G creation, available BW of supported ODU4 should be 99000");
        assertEquals(
            Uint32.valueOf(1000),
            sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth(),
            "after 1G creation, used BW of supported ODU4 should be 1000");

        assertEquals(
            79,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool().size(),
            "after 1G creation, 1 (over 80) trib slot should be occupied");
        assertThat(
            "trib slot 1 should no longer be present in Trib slot list",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool(),
            not(hasItem(Uint16.valueOf(1))));
        assertThat(
            "trib slot 2 should always be present in Trib slot list",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool(),
            hasItem(Uint16.valueOf(2)));
        assertEquals(
            79,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool().size(),
            "after 1G creation, 1 (over 80) trib port should be occupied");
        assertThat(
            "trib port 1 should no longer be present in Trib port list",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool(),
            not(hasItem(Uint16.valueOf(1))));

        // tests update for 1G deletion
        sortedLinks.clear();
        topoShard = OpenRoadmOtnTopology.updateOtnLinks(
                    topoShard.getLinks(),
                    topoShard.getTps(),
                    Uint32.valueOf(1), (short)1, (short)1, (short)1, true);
        sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, sortedLinks.size(), "list of links should contain 2 links");
        assertTrue(sortedLinks.get(0).getLinkId().getValue().startsWith("ODTU4-"));
        assertEquals(
            Uint32.valueOf(100000),
            sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth(),
            "after 1G deletion, available BW of supported ODU4 should be 100 000");
        assertEquals(
            Uint32.valueOf(0),
            sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth(),
            "after 1G deletion, used BW of supported ODU4 should be 0");

        assertEquals(
            80,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool().size(),
            "after 1G deletion, trib slot list should be full");
        assertThat(
            "after 1G deletion, trib slot list should contain items 1 and 2",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getTsPool(),
            hasItems(Uint16.valueOf(1), Uint16.valueOf(2)));
        assertEquals(
            80,
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool().size(),
            "after 1G deletion, trib port list should be full");
        assertThat(
            "after 1G deletion, trib port list should contain items 1",
            topoShard.getTps().get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().values().stream().findFirst().orElseThrow()
                .getTpnPool(),
            hasItem(Uint16.valueOf(1)));
    }

    @Test
    void updateOtnLinksForODU4WhenBWParamsNotPresentTest() {
        List<Link> odu4Links = NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.ODTU4, Uint32.valueOf(100000));
        List<Link> odu4LinksWithBadBWParam = new ArrayList<>();
        for (Link link : odu4Links) {
            odu4LinksWithBadBWParam.add(new LinkBuilder(link).removeAugmentation(Link1.class).build());
        }
        TopologyShard topoShard =
            OpenRoadmOtnTopology.updateOtnLinks(
                    odu4LinksWithBadBWParam,
                    NetworkmodelTestUtil.createTpList(true),
                    Uint32.valueOf(1), (short)1, (short)1, (short)10, false);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertNull(topoShard.getNodes(), "list of nodes should be null");
        assertNull(topoShard.getLinks(), "list of links should be null");
        assertNull(topoShard.getTps(), "list of tps should be null");
    }

    @Test
    void updateOtnLinksForODU4WhenAvailBWNotSufficientTest() {
        List<Link> odu4LinksWithBadBWParam = NetworkmodelTestUtil.createSuppOTNLinks(
                OtnLinkType.ODTU4,
                Uint32.valueOf(8000));
        TopologyShard topoShard = OpenRoadmOtnTopology.updateOtnLinks(
                    odu4LinksWithBadBWParam,
                    NetworkmodelTestUtil.createTpList(true),
                    Uint32.valueOf(10), (short)1, (short)1, (short)10, false);
        assertNotNull(topoShard, "TopologyShard should never be null");
        assertNull(topoShard.getNodes(), "list of nodes should be null");
        assertNull(topoShard.getLinks(), "list of links should be null");
        assertNull(topoShard.getTps(), "list of tps should be null");
    }

    private void checkSpdrNode(Node node) {
        Uint16 xpdrNb = node.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Node1.class)
            .getXpdrAttributes().getXpdrNumber();
        assertEquals("SPDR-SA1-XPDR" + xpdrNb, node.getNodeId().getValue());
        if (xpdrNb.equals(Uint16.valueOf(1))) {
            assertEquals(OpenroadmNodeType.MUXPDR, node.augmentation(Node1.class).getNodeType());
        } else if (xpdrNb.equals(Uint16.valueOf(2))) {
            assertEquals(OpenroadmNodeType.SWITCH, node.augmentation(Node1.class).getNodeType());
        }
        // tests supporting nodes
        List<SupportingNode> supportingNodes = node.nonnullSupportingNode().values().stream()
            .sorted((sn1, sn2) -> sn1.getNetworkRef().getValue().compareTo(sn2.getNetworkRef().getValue()))
            .collect(Collectors.toList());
        assertEquals(3, supportingNodes.size(), "Should contain 3 supporting nodes");
        assertEquals("clli-network", supportingNodes.get(0).getNetworkRef().getValue());
        assertEquals("NodeSA", supportingNodes.get(0).getNodeRef().getValue());
        assertEquals("openroadm-network", supportingNodes.get(1).getNetworkRef().getValue());
        assertEquals("SPDR-SA1", supportingNodes.get(1).getNodeRef().getValue());
        assertEquals("openroadm-topology", supportingNodes.get(2).getNetworkRef().getValue());
        assertEquals("SPDR-SA1-XPDR" + xpdrNb, supportingNodes.get(2).getNodeRef().getValue());
        checkSpdrSwitchingPools(
            xpdrNb,
            node.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Node1.class)
                .getSwitchingPools());
        List<TerminationPoint> tpList = node.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .nonnullTerminationPoint().values().stream()
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        checkSpdrTpList(xpdrNb, tpList);
    }

    private void checkSpdrSwitchingPools(Uint16 xpdrNb, SwitchingPools sp) {
        List<OduSwitchingPools> oduSwitchingPools = new ArrayList<>(sp.nonnullOduSwitchingPools().values());
        assertEquals(
            1,
            oduSwitchingPools.size(),
            "switching-pools augmentation should contain a single odu-switching-pools");
        assertEquals(
            Uint16.valueOf(1),
            oduSwitchingPools.get(0).getSwitchingPoolNumber(),
            "switching-pool-number should be 1");
        assertEquals(
            "non-blocking",
            oduSwitchingPools.get(0).getSwitchingPoolType().getName(),
            "switching-pool-type should be non-blocking");

        List<NonBlockingList> nonBlockingList =
                new ArrayList<>(oduSwitchingPools.get(0).nonnullNonBlockingList().values());
        if (xpdrNb.equals(Uint16.valueOf(1))) {
            assertEquals(4, nonBlockingList.size(), "Mux should contain 4 non blocking list");
            assertEquals(Uint16.valueOf(1), nonBlockingList.get(0).getNblNumber());
            List<NonBlockingList> nblList = oduSwitchingPools.get(0).nonnullNonBlockingList().values().stream()
                .sorted((nbl1, nbl2) -> nbl1.getNblNumber().compareTo(nbl2.getNblNumber()))
                .collect(Collectors.toList());
            for (NonBlockingList nbl : nblList) {
                assertEquals(
                    Uint32.valueOf(10),
                    nbl.getAvailableInterconnectBandwidth(),
                    "for a 10G mux, interconnect BW should be 10G");
                assertEquals(Uint32.valueOf(1000000000), nbl.getInterconnectBandwidthUnit());
                assertThat(
                    "for a 10G mux, non blocking list should contain 2 entries (client + network ports)",
                    nbl.getTpList(),
                    hasSize(2));
                String nb = nbl.getNblNumber().toString();
                assertThat(
                    nbl.getTpList(),
                    containsInAnyOrder(new TpId("XPDR1-NETWORK1"), new TpId("XPDR1-CLIENT" + nb)));
            }
        } else if (xpdrNb.equals(Uint16.valueOf(2))) {
            assertEquals(1, nonBlockingList.size(), "Switch should contain a single non blocking list");
            assertEquals(Uint16.valueOf(1), nonBlockingList.get(0).getNblNumber());
            assertThat(
                "for a 100G Switch, non blocking list should contain 8 entries (4 clients + 4 network ports)",
                nonBlockingList.get(0).getTpList(),
                hasSize(8));
            assertThat(
                nonBlockingList.get(0).getTpList(),
                containsInAnyOrder(
                    new TpId("XPDR2-CLIENT1"), new TpId("XPDR2-NETWORK1"), new TpId("XPDR2-CLIENT2"),
                    new TpId("XPDR2-NETWORK2"), new TpId("XPDR2-CLIENT3"), new TpId("XPDR2-NETWORK3"),
                    new TpId("XPDR2-CLIENT4"), new TpId("XPDR2-NETWORK4")));
        }
    }

    private void checkSpdrTpList(Uint16 xpdrNb, List<TerminationPoint> tpList) {
        LOG.info("tpList = {}", tpList);
        assertEquals(
            IfOCHOTU4ODU4.VALUE,
            tpList.get(4).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().stream().findFirst()
                .orElseThrow().getIfCapType(),
            "only IfOCHOTU4ODU4 interface capabitily expected");
        assertEquals(
            ODU4.VALUE,
            tpList.get(4).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getRate(),
            "the rate should be ODU4");
        assertEquals(
            "openroadm-topology",
            tpList.get(4).getSupportingTerminationPoint().values().stream().findFirst().orElseThrow().getNetworkRef()
                .getValue());
        assertEquals(
            "SPDR-SA1-XPDR" + xpdrNb,
            tpList.get(4).getSupportingTerminationPoint().values().stream().findFirst().orElseThrow().getNodeRef()
                .getValue());
        assertEquals(
            "XPDR" + xpdrNb + "-NETWORK1",
            tpList.get(4).getSupportingTerminationPoint().values().stream().findFirst().orElseThrow().getTpRef()
                .getValue());
        if (xpdrNb.equals(Uint16.valueOf(1))) {
            assertEquals(5, tpList.size(), "should contain 5 TPs");
            assertEquals("XPDR1-CLIENT1", tpList.get(0).getTpId().getValue());
            assertEquals("XPDR1-CLIENT2", tpList.get(1).getTpId().getValue());
            assertEquals("XPDR1-NETWORK1", tpList.get(4).getTpId().getValue());
            assertEquals(
                2,
                tpList.get(1).augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                            .TerminationPoint1.class)
                    .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().size(),
                "supported interface capability of tp-id XPDR1-CLIENT2 should contain 2 if-cap-type");
            assertEquals(
                3,
                tpList.get(2).augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                            .TerminationPoint1.class)
                    .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().size(),
                "supported interface capability of tp-id XPDR1-CLIENT3 should contain 3 if-cap-type");
            List<SupportedInterfaceCapability> sicListClient1 = tpList.get(0).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().stream()
                .collect(Collectors.toList());
            for (SupportedInterfaceCapability supportedInterfaceCapability : sicListClient1) {
                assertThat("tp should have 2 if-cap-type: if-10GE-ODU2e, if-10GE-ODU2",
                    String.valueOf(supportedInterfaceCapability.getIfCapType()),
                    either(containsString(String.valueOf(If10GEODU2e.VALUE)))
                        .or(containsString(String.valueOf(If10GEODU2.VALUE))));
            }
            List<SupportedInterfaceCapability> sicListClient3 = tpList.get(3).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().stream()
                .collect(Collectors.toList());
            for (SupportedInterfaceCapability supportedInterfaceCapability : sicListClient3) {
                assertThat("tp should have 3 if-cap-type: if-10GE-ODU2e, if-10GE-ODU2, if-10GE",
                    String.valueOf(supportedInterfaceCapability.getIfCapType()),
                    either(containsString(String.valueOf(If10GEODU2e.VALUE)))
                        .or(containsString(String.valueOf(If10GEODU2.VALUE)))
                        .or(containsString(String.valueOf(If10GE.VALUE))));
            }
            assertThat("the rate should be ODU2 or ODU2e",
                String.valueOf(tpList.get(2).augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                            .TerminationPoint1.class)
                    .getXpdrTpPortConnectionAttributes().getRate()),
                either(containsString(String.valueOf(ODU2e.VALUE)))
                    .or(containsString(String.valueOf(ODU2.VALUE))));
            assertEquals(
                OpenroadmTpType.XPONDERCLIENT,
                tpList.get(2).augmentation(TerminationPoint1.class).getTpType(),
                "TP should be of type client");
            assertEquals(
                OpenroadmTpType.XPONDERNETWORK,
                tpList.get(4).augmentation(TerminationPoint1.class).getTpType(),
                "TP should be of type network");
        } else if (xpdrNb.equals(Uint16.valueOf(2))) {
            assertEquals(8, tpList.size(), "should contain 8 TPs");
            assertEquals("XPDR2-CLIENT1", tpList.get(0).getTpId().getValue());
            assertEquals("XPDR2-CLIENT2", tpList.get(1).getTpId().getValue());
            assertEquals("XPDR2-NETWORK1", tpList.get(4).getTpId().getValue());
            assertEquals("XPDR2-NETWORK2", tpList.get(5).getTpId().getValue());
            assertEquals(
                IfOCHOTU4ODU4.VALUE,
                tpList.get(5).augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                            .TerminationPoint1.class)
                    .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().stream().findFirst()
                    .orElseThrow().getIfCapType(),
                "only IfOCHOTU4ODU4 interface capabitily expected");
            assertEquals(
                2,
                tpList.get(2).augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                            .TerminationPoint1.class)
                    .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().size(),
                "supported interface capability of tp should contain 2 IfCapType");
            List<SupportedInterfaceCapability> sicListClient3 = tpList.get(2).augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                        .TerminationPoint1.class)
                .getTpSupportedInterfaces().getSupportedInterfaceCapability().values().stream()
                .collect(Collectors.toList());
            for (SupportedInterfaceCapability supportedInterfaceCapability : sicListClient3) {
                assertThat(
                    "tp should have 2 if-cap-type: if-100GE-ODU4, if-100GE",
                    String.valueOf(supportedInterfaceCapability.getIfCapType()),
                    either(containsString(String.valueOf(If100GEODU4.VALUE)))
                        .or(containsString(String.valueOf(If100GE.VALUE))));
            }
            assertEquals(
                OpenroadmTpType.XPONDERCLIENT,
                tpList.get(2).augmentation(TerminationPoint1.class).getTpType(),
                "TP should be of type client");
            assertEquals(
                OpenroadmTpType.XPONDERNETWORK,
                tpList.get(6).augmentation(TerminationPoint1.class).getTpType(),
                "TP should be of type network");
        }
    }
}
