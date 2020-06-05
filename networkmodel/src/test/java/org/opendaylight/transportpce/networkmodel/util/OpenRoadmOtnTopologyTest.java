/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.stream.JsonReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.transportpce.networkmodel.util.test.JsonUtil;
import org.opendaylight.transportpce.networkmodel.util.test.NetworkmodelTestUtil;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.networks.network.node.SwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181130.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yangtools.yang.common.QName;
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
        JsonReader portMappingReader = null;
        try {
            Reader reader = new FileReader("src/test/resources/portMapping.json");
            portMappingReader = new JsonReader(reader);
            Network portMapping = (Network) JsonUtil.getInstance().getDataObjectFromJson(portMappingReader,
                    QName.create("http://org/opendaylight/transportpce/portmapping", "2020-04-29", "network"));
            for (Nodes nodes : portMapping.getNodes()) {
                if (nodes.getNodeId().equals("XPDR-A1")) {
                    this.portMappingTpdr = nodes;
                } else {
                    this.portMappingSpdr = nodes;
                }
            }
            List<Mapping> mappingList = new ArrayList<>();
            mappingList.add(new MappingBuilder().setLogicalConnectionPoint("XPDR0-NETWORK0").build());
            this.portMappingBad = new NodesBuilder()
                .setNodeId(this.portMappingTpdr.getNodeId())
                .setNodeInfo(new NodeInfoBuilder(this.portMappingTpdr.getNodeInfo()).setNodeType(NodeTypes.Ila).build())
                .setMapping(mappingList)
                .build();
            LOG.info("tpdr portMapping = {}", this.portMappingTpdr.toString());
            LOG.info("spdr portMapping = {}", this.portMappingSpdr.toString());
            LOG.info("ila portMapping = {}", this.portMappingBad.toString());
        } catch (FileNotFoundException e) {
            LOG.error("Cannot init OpenRoadmOtnTopologyTest ", e);
            fail("Cannot init OpenRoadmOtnTopologyTest ");
        } finally {
            try {
                if (portMappingReader != null) {
                    portMappingReader.close();
                }
            } catch (IOException e) {
                LOG.warn("Cannot close reader ", e);
            }
        }
    }

    @Test
    public void createTopologyShardForTpdrTest() {
        TopologyShard topologyShard = OpenRoadmOtnTopology.createTopologyShard(this.portMappingTpdr);
        assertNotNull(topologyShard);
        assertEquals("Should contain a single node", 1, topologyShard.getNodes().size());
        assertEquals("Should contain no link", 0, topologyShard.getLinks().size());
        Node node = topologyShard.getNodes().get(0);
        assertEquals("XPDR-A1-XPDR1", node.getNodeId().getValue());
        // tests supporting nodes
        List<SupportingNode> supportingNodes = node.getSupportingNode().stream()
            .sorted((sn1, sn2) -> sn1.getNetworkRef().getValue().compareTo(sn2.getNetworkRef().getValue()))
            .collect(Collectors.toList());
        assertEquals(3, supportingNodes.size());
        LOG.info("supporting nodes = {}", supportingNodes.toString());
        assertEquals("clli-network", supportingNodes.get(0).getNetworkRef().getValue());
        assertEquals("NodeA", supportingNodes.get(0).getNodeRef().getValue());
        assertEquals("openroadm-network", supportingNodes.get(1).getNetworkRef().getValue());
        assertEquals("XPDR-A1", supportingNodes.get(1).getNodeRef().getValue());
        assertEquals("openroadm-topology", supportingNodes.get(2).getNetworkRef().getValue());
        assertEquals("XPDR-A1-XPDR1", supportingNodes.get(2).getNodeRef().getValue());
        assertEquals(OpenroadmNodeType.TPDR, node.augmentation(Node1.class).getNodeType());
        assertEquals(Uint16.valueOf(1), node.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1.class)
            .getXpdrAttributes().getXpdrNumber());
        //tests list of TPs
        List<TerminationPoint> tps = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().stream()
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        assertEquals(4, tps.size());
        //tests client tp
        assertEquals("XPDR1-CLIENT1", tps.get(0).getTpId().getValue());
        assertEquals("XPDR1-NETWORK1", tps.get(0).augmentation(TerminationPoint1.class)
            .getAssociatedConnectionMapPort());
        assertEquals(1, tps.get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getTpSupportedInterfaces().getSupportedInterfaceCapability().size());
        assertEquals(If100GE.class.getName(), tps.get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getTpSupportedInterfaces().getSupportedInterfaceCapability()
            .get(0).getIfCapType().getName());
        assertEquals(OpenroadmTpType.XPONDERCLIENT, tps.get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class)
            .getTpType());
        //tests network tp
        assertEquals("XPDR1-NETWORK1", tps.get(2).getTpId().getValue());
        assertEquals("XPDR1-CLIENT1", tps.get(2).augmentation(TerminationPoint1.class)
            .getAssociatedConnectionMapPort());
        assertEquals(1, tps.get(2).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getTpSupportedInterfaces().getSupportedInterfaceCapability().size());
        assertEquals(IfOCHOTU4ODU4.class.getName(), tps.get(2).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getTpSupportedInterfaces().getSupportedInterfaceCapability()
            .get(0).getIfCapType().getName());
        assertEquals(ODU4.class.getName(), tps.get(2).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getRate().getName());
        assertEquals(OpenroadmTpType.XPONDERNETWORK, tps.get(2).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class)
            .getTpType());
    }

    @Test
    public void createTopologyShardForSpdrTest() {
        TopologyShard topologyShard = OpenRoadmOtnTopology.createTopologyShard(this.portMappingSpdr);
        assertNotNull(topologyShard);
        assertEquals("Should contain two nodes", 2, topologyShard.getNodes().size());
        assertEquals("Should contain no link", 0, topologyShard.getLinks().size());
        List<Node> nodes = topologyShard.getNodes().stream()
            .sorted((n1, n2) -> n1.getNodeId().getValue().compareTo(n2.getNodeId().getValue()))
            .collect(Collectors.toList());
        for (Node node : nodes) {
            checkSpdrNode(node);
        }
    }

    @Test
    public void createOtnLinks1Test() {
        String nodeA = "SPDRA";
        String tpA = "XPDR1-NETWORK1";
        String nodeZ = "SPDRZ";
        String tpZ = "XPDR1-NETWORK1";
        List<Link> links = OpenRoadmOtnTopology.createOtnLinks(nodeA, tpA, nodeZ, tpZ, OtnLinkType.OTU4).getLinks();
        assertEquals("2 OTU4 links should have been created", 2, links.size());
        List<Link> sortedLinks = links.stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals("OTU4-SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(0).getLinkId().getValue());
        assertEquals("OTU4-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(1).getLinkId().getValue());
        assertEquals("SPDRA-XPDR1", sortedLinks.get(0).getSource().getSourceNode().getValue());
        assertEquals("SPDRZ-XPDR1", sortedLinks.get(0).getDestination().getDestNode().getValue());
        assertEquals("SPDRZ-XPDR1", sortedLinks.get(1).getSource().getSourceNode().getValue());
        assertEquals("SPDRA-XPDR1", sortedLinks.get(1).getDestination().getDestNode().getValue());
        assertEquals(Uint32.valueOf(100000), sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth());
        assertEquals(Uint32.valueOf(0), sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth());
        assertEquals(OpenroadmLinkType.OTNLINK, sortedLinks.get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class).getLinkType());
        assertEquals("OTU4-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1", sortedLinks.get(0)
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class)
            .getOppositeLink().getValue());
        assertEquals(OtnLinkType.OTU4, sortedLinks.get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1.class).getOtnLinkType());
    }

    @Test
    public void createOtnLinks2Test() {
        String nodeA = "SPDRA";
        String tpA = "XPDR1-NETWORK1";
        String nodeZ = "SPDRZ";
        String tpZ = "XPDR1-NETWORK1";
        TopologyShard topoShard = OpenRoadmOtnTopology.createOtnLinks(nodeA, tpA, nodeZ, tpZ, OtnLinkType.ODU0);
        assertNotNull(topoShard);
        assertNull(topoShard.getNodes());
        assertNull(topoShard.getLinks());
    }

    @Test
    public void createOtnLinks3Test() {
        TopologyShard topoShard = OpenRoadmOtnTopology.createOtnLinks(NetworkmodelTestUtil
            .createSuppOTNLinks(OtnLinkType.OTU4, 100000), NetworkmodelTestUtil.createTpList(false));
        assertNotNull("TopologyShard should never be null", topoShard);
        assertNull("list of nodes should be null", topoShard.getNodes());
        List<Link> sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals("list of links should contain 4 links", 4, sortedLinks.size());
        assertTrue(sortedLinks.get(2).getLinkId().getValue().startsWith("OTU4-"));
        assertEquals(Uint32.valueOf(0), sortedLinks.get(2).augmentation(Link1.class).getAvailableBandwidth());
        assertEquals(Uint32.valueOf(100000), sortedLinks.get(2).augmentation(Link1.class).getUsedBandwidth());

        assertEquals("ODU4-SPDRA-XPDR1-XPDR1-NETWORK1toSPDRZ-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(0).getLinkId().getValue());
        assertEquals("ODU4-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1",
            sortedLinks.get(1).getLinkId().getValue());
        assertEquals("SPDRA-XPDR1", sortedLinks.get(0).getSource().getSourceNode().getValue());
        assertEquals("SPDRZ-XPDR1", sortedLinks.get(0).getDestination().getDestNode().getValue());
        assertEquals("SPDRZ-XPDR1", sortedLinks.get(1).getSource().getSourceNode().getValue());
        assertEquals("SPDRA-XPDR1", sortedLinks.get(1).getDestination().getDestNode().getValue());
        assertEquals(Uint32.valueOf(100000), sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth());
        assertEquals(Uint32.valueOf(0), sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth());
        assertEquals(OpenroadmLinkType.OTNLINK, sortedLinks.get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class).getLinkType());
        assertEquals("ODU4-SPDRZ-XPDR1-XPDR1-NETWORK1toSPDRA-XPDR1-XPDR1-NETWORK1", sortedLinks.get(0)
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Link1.class)
            .getOppositeLink().getValue());
        assertEquals(OtnLinkType.ODTU4, sortedLinks.get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.transportpce.topology.rev200129.Link1.class).getOtnLinkType());

        assertEquals("list of TPs should contain 2 updated TPs", 2, topoShard.getTps().size());
        assertNotNull(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool());
        assertEquals(80, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool().size());
        assertNotNull(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool());
        assertEquals(80, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool().size());
    }

    @Test
    public void createOtnLinks4Test() {
        List<Link> otu4Links = NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.OTU4, 100000);
        List<Link> otu4LinksWithBadBWParam = new ArrayList<>();
        for (Link link : otu4Links) {
            otu4LinksWithBadBWParam.add(new LinkBuilder(link).removeAugmentation(Link1.class).build());
        }
        TopologyShard topoShard = OpenRoadmOtnTopology.createOtnLinks(otu4LinksWithBadBWParam,
            NetworkmodelTestUtil.createTpList(false));
        assertNotNull("TopologyShard should never be null", topoShard);
        assertNull("list of nodes should be null", topoShard.getNodes());
        assertNull("list of links should be null", topoShard.getLinks());
        assertNull("list of tps should be null", topoShard.getTps());

        otu4LinksWithBadBWParam.clear();
        topoShard = OpenRoadmOtnTopology.createOtnLinks(NetworkmodelTestUtil
            .createSuppOTNLinks(OtnLinkType.OTU4, 99000), NetworkmodelTestUtil.createTpList(false));
        assertNull("list of nodes should be null", topoShard.getNodes());
        assertNull("list of links should be null", topoShard.getLinks());
        assertNull("list of tps should be null", topoShard.getTps());
    }

    @Test
    public void deleteOtnLinks1Test() {
        TopologyShard topoShard = OpenRoadmOtnTopology.deleteOtnLinks(NetworkmodelTestUtil
            .createSuppOTNLinks(OtnLinkType.OTU4, 0), NetworkmodelTestUtil.createTpList(true));
        assertNotNull("TopologyShard should never be null", topoShard);
        assertEquals("list of links should contain 2 links", 2, topoShard.getLinks().size());
        assertEquals(Uint32.valueOf(100000), topoShard.getLinks().get(0).augmentation(Link1.class)
            .getAvailableBandwidth());
        assertEquals(Uint32.valueOf(0), topoShard.getLinks().get(0).augmentation(Link1.class).getUsedBandwidth());

        assertEquals("list of TPs should contain 2 updated TPs", 2, topoShard.getTps().size());
        assertNull(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool());
        assertNull(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool());
    }

    @Test
    public void deleteOtnLinks2Test() {
        List<Link> otu4Links = NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.OTU4, 0);
        List<Link> otu4LinksWithBadBWParam = new ArrayList<>();
        for (Link link : otu4Links) {
            otu4LinksWithBadBWParam.add(new LinkBuilder(link).removeAugmentation(Link1.class).build());
        }
        TopologyShard topoShard = OpenRoadmOtnTopology.deleteOtnLinks(otu4LinksWithBadBWParam,
            NetworkmodelTestUtil.createTpList(true));
        assertNotNull("TopologyShard should never be null", topoShard);
        assertNull("list of nodes should be null", topoShard.getNodes());
        assertNull("list of links should be null", topoShard.getLinks());
        assertNull("list of tps should be null", topoShard.getTps());
    }

    @Test
    public void updateOtnLinksFor10GTest() {
        // tests update for 10G creation
        TopologyShard topoShard = OpenRoadmOtnTopology.updateOtnLinks(NetworkmodelTestUtil
            .createSuppOTNLinks(OtnLinkType.ODTU4, 100000), NetworkmodelTestUtil.createTpList(true), "10G", (short)1,
            (short)1, false);
        assertNotNull("TopologyShard should never be null", topoShard);
        assertNull("list of nodes should be null", topoShard.getNodes());
        List<Link> sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals("list of links should contain 2 links", 2, sortedLinks.size());
        assertTrue(sortedLinks.get(0).getLinkId().getValue().startsWith("ODU4-"));
        assertEquals(Uint32.valueOf(90000), sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth());
        assertEquals(Uint32.valueOf(10000), sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth());

        assertEquals(72, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool().size());
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool(), not(hasItems(Uint16.valueOf(1), Uint16.valueOf(8))));
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool(), hasItem(Uint16.valueOf(9)));
        assertEquals(79, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool().size());
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool(), not(hasItem(Uint16.valueOf(1))));

        // tests update for 10G deletion
        sortedLinks.clear();
        topoShard = OpenRoadmOtnTopology.updateOtnLinks(topoShard.getLinks(), topoShard.getTps(), "10G", (short)1,
            (short)1, true);
        sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals("list of links should contain 2 links", 2, sortedLinks.size());
        assertTrue(sortedLinks.get(0).getLinkId().getValue().startsWith("ODU4-"));
        assertEquals(Uint32.valueOf(100000), sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth());
        assertEquals(Uint32.valueOf(0), sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth());

        assertEquals(80, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool().size());
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool(),
            hasItems(Uint16.valueOf(1), Uint16.valueOf(8), Uint16.valueOf(9)));
        assertEquals(80, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool().size());
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool(), hasItem(Uint16.valueOf(1)));
    }

    @Test
    public void updateOtnLinksFor1GCreationTest() {
        // tests update for 1G creation
        TopologyShard topoShard = OpenRoadmOtnTopology.updateOtnLinks(NetworkmodelTestUtil
            .createSuppOTNLinks(OtnLinkType.ODTU4, 100000), NetworkmodelTestUtil.createTpList(true), "1G", (short)1,
            (short)1, false);
        assertNotNull("TopologyShard should never be null", topoShard);
        assertNull("list of nodes should be null", topoShard.getNodes());
        List<Link> sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals("list of links should contain 2 links", 2, sortedLinks.size());
        assertTrue(sortedLinks.get(0).getLinkId().getValue().startsWith("ODU4-"));
        assertEquals(Uint32.valueOf(99000), sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth());
        assertEquals(Uint32.valueOf(1000), sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth());

        assertEquals(79, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool().size());
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool(), not(hasItem(Uint16.valueOf(1))));
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool(), hasItem(Uint16.valueOf(2)));
        assertEquals(79, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool().size());
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool(), not(hasItem(Uint16.valueOf(1))));

        // tests update for 1G deletion
        sortedLinks.clear();
        topoShard = OpenRoadmOtnTopology.updateOtnLinks(topoShard.getLinks(), topoShard.getTps(), "1G", (short)1,
            (short)1, true);
        sortedLinks = topoShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals("list of links should contain 2 links", 2, sortedLinks.size());
        assertTrue(sortedLinks.get(0).getLinkId().getValue().startsWith("ODU4-"));
        assertEquals(Uint32.valueOf(100000), sortedLinks.get(0).augmentation(Link1.class).getAvailableBandwidth());
        assertEquals(Uint32.valueOf(0), sortedLinks.get(0).augmentation(Link1.class).getUsedBandwidth());

        assertEquals(80, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool().size());
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getTsPool(),
            hasItems(Uint16.valueOf(1), Uint16.valueOf(2)));
        assertEquals(80, topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool().size());
        assertThat(topoShard.getTps().get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getOdtuTpnPool().get(0).getTpnPool(), hasItem(Uint16.valueOf(1)));
    }

    @Test
    public void updateOtnLinksForBadBWParam1Test() {
        List<Link> odu4Links = NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.ODTU4, 100000);
        List<Link> odu4LinksWithBadBWParam = new ArrayList<>();
        for (Link link : odu4Links) {
            odu4LinksWithBadBWParam.add(new LinkBuilder(link).removeAugmentation(Link1.class).build());
        }
        TopologyShard topoShard = OpenRoadmOtnTopology.updateOtnLinks(odu4LinksWithBadBWParam,
            NetworkmodelTestUtil.createTpList(true), "1G", (short)1, (short)1, false);
        assertNotNull("TopologyShard should never be null", topoShard);
        assertNull("list of nodes should be null", topoShard.getNodes());
        assertNull("list of links should be null", topoShard.getLinks());
        assertNull("list of tps should be null", topoShard.getTps());
    }

    @Test
    public void updateOtnLinksForBadBWParam2Test() {
        List<Link> odu4LinksWithBadBWParam = NetworkmodelTestUtil.createSuppOTNLinks(OtnLinkType.ODTU4, 8000);
        TopologyShard topoShard = OpenRoadmOtnTopology.updateOtnLinks(odu4LinksWithBadBWParam,
            NetworkmodelTestUtil.createTpList(true), "10G", (short)1, (short)1, false);
        assertNotNull("TopologyShard should never be null", topoShard);
        assertNull("list of nodes should be null", topoShard.getNodes());
        assertNull("list of links should be null", topoShard.getLinks());
        assertNull("list of tps should be null", topoShard.getTps());
    }

    private void checkSpdrNode(Node node) {
        Uint16 xpdrNb = node
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1.class)
            .getXpdrAttributes().getXpdrNumber();
        assertEquals("SPDR-SA1-XPDR" + xpdrNb, node.getNodeId().getValue());
        if (xpdrNb.equals(Uint16.valueOf(1))) {
            assertEquals(OpenroadmNodeType.MUXPDR, node.augmentation(Node1.class).getNodeType());
        } else if (xpdrNb.equals(Uint16.valueOf(2))) {
            assertEquals(OpenroadmNodeType.SWITCH, node.augmentation(Node1.class).getNodeType());
        }
        // tests supporting nodes
        List<SupportingNode> supportingNodes = node.getSupportingNode().stream()
            .sorted((sn1, sn2) -> sn1.getNetworkRef().getValue().compareTo(sn2.getNetworkRef().getValue()))
            .collect(Collectors.toList());
        assertEquals(3, supportingNodes.size());
        assertEquals("clli-network", supportingNodes.get(0).getNetworkRef().getValue());
        assertEquals("NodeSA", supportingNodes.get(0).getNodeRef().getValue());
        assertEquals("openroadm-network", supportingNodes.get(1).getNetworkRef().getValue());
        assertEquals("SPDR-SA1", supportingNodes.get(1).getNodeRef().getValue());
        assertEquals("openroadm-topology", supportingNodes.get(2).getNetworkRef().getValue());
        assertEquals("SPDR-SA1-XPDR" + xpdrNb, supportingNodes.get(2).getNodeRef().getValue());
        checkSpdrSwitchingPools(xpdrNb, node.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1.class)
            .getSwitchingPools());
        List<TerminationPoint> tpList = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().stream()
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        checkSpdrTpList(xpdrNb, tpList);
    }

    private void checkSpdrSwitchingPools(Uint16 xpdrNb, SwitchingPools sp) {
        assertEquals("switching-pools augmentation should contain a single odu-switching-pools", 1,
            sp.getOduSwitchingPools().size());
        assertEquals("switching-pool-number should be 1", Uint16.valueOf(1),
            sp.getOduSwitchingPools().get(0).getSwitchingPoolNumber());
        assertEquals("switching-pool-type should be non-blocking", "non-blocking",
            sp.getOduSwitchingPools().get(0).getSwitchingPoolType().getName());
        if (xpdrNb.equals(Uint16.valueOf(1))) {
            assertEquals("Mux should contain 4 non blocking list", 4,
                sp.getOduSwitchingPools().get(0).getNonBlockingList().size());
            assertEquals(Uint16.valueOf(1),
                sp.getOduSwitchingPools().get(0).getNonBlockingList().get(0).getNblNumber());
            List<NonBlockingList> nblList = sp.getOduSwitchingPools().get(0).getNonBlockingList().stream()
                .sorted((nbl1, nbl2) -> nbl1.getNblNumber().compareTo(nbl2.getNblNumber()))
                .collect(Collectors.toList());
            for (NonBlockingList nbl : nblList) {
                assertEquals(Uint32.valueOf(10), nbl.getAvailableInterconnectBandwidth());
                assertEquals(Uint32.valueOf(1000000000), nbl.getInterconnectBandwidthUnit());
                assertThat(nbl.getTpList(), hasSize(2));
                String nb = nbl.getNblNumber().toString();
                assertThat(nbl.getTpList(),
                    containsInAnyOrder(new TpId("XPDR1-NETWORK1"), new TpId("XPDR1-CLIENT" + nb)));
            }
        } else if (xpdrNb.equals(Uint16.valueOf(2))) {
            assertEquals("Switch should contain a single non blocking list", 1,
                sp.getOduSwitchingPools().get(0).getNonBlockingList().size());
            assertEquals(Uint16.valueOf(1),
                sp.getOduSwitchingPools().get(0).getNonBlockingList().get(0).getNblNumber());
            assertThat(sp.getOduSwitchingPools().get(0).getNonBlockingList().get(0).getTpList(), hasSize(8));
            assertThat(sp.getOduSwitchingPools().get(0).getNonBlockingList().get(0).getTpList(),
                containsInAnyOrder(new TpId("XPDR2-CLIENT1"), new TpId("XPDR2-NETWORK1"), new TpId("XPDR2-CLIENT2"),
                new TpId("XPDR2-NETWORK2"), new TpId("XPDR2-CLIENT3"), new TpId("XPDR2-NETWORK3"),
                new TpId("XPDR2-CLIENT4"), new TpId("XPDR2-NETWORK4")));
        }
    }

    private void checkSpdrTpList(Uint16 xpdrNb, List<TerminationPoint> tpList) {
        assertEquals(IfOCHOTU4ODU4.class, tpList.get(4).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getTpSupportedInterfaces().getSupportedInterfaceCapability().get(0).getIfCapType());
        assertEquals(ODU4.class, tpList.get(4).augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
            .getXpdrTpPortConnectionAttributes().getRate());
        assertEquals("openroadm-topology",
            tpList.get(4).getSupportingTerminationPoint().get(0).getNetworkRef().getValue());
        assertEquals("SPDR-SA1-XPDR" + xpdrNb,
            tpList.get(4).getSupportingTerminationPoint().get(0).getNodeRef().getValue());
        assertEquals("XPDR" + xpdrNb + "-NETWORK1", tpList.get(4).getSupportingTerminationPoint().get(0).getTpRef());
        if (xpdrNb.equals(Uint16.valueOf(1))) {
            assertEquals("should contain 5 TPs", 5, tpList.size());
            assertEquals("XPDR1-CLIENT1", tpList.get(0).getTpId().getValue());
            assertEquals("XPDR1-CLIENT2", tpList.get(1).getTpId().getValue());
            assertEquals("XPDR1-NETWORK1", tpList.get(4).getTpId().getValue());
            assertEquals(If10GEODU2e.class, tpList.get(2).augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
                .getTpSupportedInterfaces().getSupportedInterfaceCapability().get(0).getIfCapType());
            assertEquals(ODU2e.class, tpList.get(2).augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getRate());
            assertEquals(OpenroadmTpType.XPONDERCLIENT, tpList.get(2).augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class)
                .getTpType());
            assertEquals(OpenroadmTpType.XPONDERNETWORK, tpList.get(4).augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class)
                .getTpType());
        } else if (xpdrNb.equals(Uint16.valueOf(2))) {
            assertEquals("should contain 8 TPs", 8, tpList.size());
            assertEquals("XPDR2-CLIENT1", tpList.get(0).getTpId().getValue());
            assertEquals("XPDR2-CLIENT2", tpList.get(1).getTpId().getValue());
            assertEquals("XPDR2-NETWORK1", tpList.get(4).getTpId().getValue());
            assertEquals("XPDR2-NETWORK2", tpList.get(5).getTpId().getValue());
            assertEquals(If100GEODU4.class, tpList.get(2).augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
                .getTpSupportedInterfaces().getSupportedInterfaceCapability().get(0).getIfCapType());
            assertEquals(ODU4.class, tpList.get(2).augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.TerminationPoint1.class)
                .getXpdrTpPortConnectionAttributes().getRate());
            assertEquals(OpenroadmTpType.XPONDERCLIENT, tpList.get(2).augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class)
                .getTpType());
            assertEquals(OpenroadmTpType.XPONDERNETWORK, tpList.get(6).augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1.class)
                .getTpType());
        }
    }
}