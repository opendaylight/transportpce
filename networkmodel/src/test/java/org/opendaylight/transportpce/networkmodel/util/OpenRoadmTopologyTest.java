/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.transportpce.networkmodel.util.test.NetworkmodelTestUtil;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;


@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class OpenRoadmTopologyTest {
    @Mock
    private NetworkTransactionService networkTransactionService;

    @Test
    public void createTopologyShardForDegreeTest() {
        Nodes mappingNode = NetworkmodelTestUtil.createMappingForRdm("ROADMA01", "nodeA", 2, 0);
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode);
        assertNotNull(topologyShard);
        assertEquals("Should contain 2 Degree nodes only", 2, topologyShard.getNodes().size());
        assertEquals("Should contain 2 links", 2, topologyShard.getLinks().size());
        List<Node> nodes = topologyShard.getNodes().stream()
            .sorted((n1, n2) -> n1.getNodeId().getValue().compareTo(n2.getNodeId().getValue()))
            .collect(Collectors.toList());
        for (Node node : nodes) {
            String nodeNb = node.getNodeId().getValue().substring(node.getNodeId().getValue().length() - 1);
            checkDegreeNode(nodeNb, node);
        }

        List<Link> links = topologyShard.getLinks().stream()
            .sorted((l1, l2) -> l1.getLinkId().getValue().compareTo(l2.getLinkId().getValue()))
            .collect(Collectors.toList());
        assertEquals("Should contain 2 express links", 2, links.size());
        assertEquals("ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX", links.get(0).getLinkId().getValue());
        assertEquals("ROADMA01-DEG1", links.get(0).getSource().getSourceNode().getValue());
        assertEquals("DEG1-CTP-TXRX", links.get(0).getSource().getSourceTp());
        assertEquals("ROADMA01-DEG2", links.get(0).getDestination().getDestNode().getValue());
        assertEquals("DEG2-CTP-TXRX", links.get(0).getDestination().getDestTp());
        assertEquals("ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX", links.get(1).getLinkId().getValue());
        assertEquals("ROADMA01-DEG2", links.get(1).getSource().getSourceNode().getValue());
        assertEquals("DEG2-CTP-TXRX", links.get(1).getSource().getSourceTp());
        assertEquals("ROADMA01-DEG1", links.get(1).getDestination().getDestNode().getValue());
        assertEquals("DEG1-CTP-TXRX", links.get(1).getDestination().getDestTp());
    }

    @Test
    public void createTopologyShardForSrgTest() {
        Nodes mappingNode = NetworkmodelTestUtil.createMappingForRdm("ROADMA01", "nodeA", 0, 1);
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode);
        assertNotNull(topologyShard);
        List<Node> nodes = topologyShard.getNodes();
        assertEquals("Should contain 1 SRG node only", 1, nodes.size());
        assertEquals("Should contain 0 link", 0, topologyShard.getLinks().size());
        checkSrgNode("1", nodes.get(0));
    }

    @Test
    public void createTopologyShardForCompleteRdmNodeTest() {
        Nodes mappingNode = NetworkmodelTestUtil.createMappingForRdm("ROADMA01", "nodeA", 2, 2);
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode);
        assertNotNull(topologyShard);
        assertEquals("Should contain 2 Deg and 2 SRG nodes", 4, topologyShard.getNodes().size());
        List<Link> addLinks = topologyShard.getLinks().stream()
            .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.ADDLINK))
            .collect(Collectors.toList());
        assertEquals("Should contain 4 add links", 4, addLinks.size());
        List<Link> dropLinks = topologyShard.getLinks().stream()
            .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.DROPLINK))
            .collect(Collectors.toList());
        assertEquals("Should contain 4 drop links", 4, dropLinks.size());
        List<Link> expressLinks = topologyShard.getLinks().stream()
            .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.EXPRESSLINK))
            .collect(Collectors.toList());
        assertEquals("Should contain 2 express links", 2, expressLinks.size());
    }

    @Test
    public void createTopologyShardForTpdrNodeTest() {
        Nodes mappingNode = NetworkmodelTestUtil.createMappingForXpdr("XPDRA01", "nodeA", 2, 2, null);
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode);
        assertNotNull(topologyShard);
        assertEquals("Should contain a single node", 1, topologyShard.getNodes().size());
        assertEquals("Should contain 0 link", 0, topologyShard.getLinks().size());
        checkTpdrNode(topologyShard.getNodes().get(0));
    }

    @Test
    public void createTopologyShardForTpdrNode2Test() {
        Nodes mappingNode = NetworkmodelTestUtil.createMappingForXpdr("XPDRA01", "nodeA", 2, 2, XpdrNodeTypes.Tpdr);
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode);
        assertNotNull(topologyShard);
        assertEquals("Should contain a single node", 1, topologyShard.getNodes().size());
        assertEquals("Should contain 0 link", 0, topologyShard.getLinks().size());
        checkTpdrNode(topologyShard.getNodes().get(0));
    }

    @Test
    public void createTopologyShardForMpdrNodeTest() {
        Nodes mappingNode = NetworkmodelTestUtil.createMappingForXpdr("XPDRA01", "nodeA", 2, 2, XpdrNodeTypes.Mpdr);
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode);
        assertNotNull(topologyShard);
        assertEquals("Should contain a single node", 1, topologyShard.getNodes().size());
        assertEquals("Should contain 0 link", 0, topologyShard.getLinks().size());
        checkOtnXpdrNode(topologyShard.getNodes().get(0));
    }

    @Test
    public void createTopologyShardForSwitchNodeTest() {
        Nodes mappingNode = NetworkmodelTestUtil.createMappingForXpdr("XPDRA01", "nodeA", 2, 2, XpdrNodeTypes.Switch);
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode);
        assertNotNull(topologyShard);
        assertEquals("Should contain a single node", 1, topologyShard.getNodes().size());
        assertEquals("Should contain 0 link", 0, topologyShard.getLinks().size());
        checkOtnXpdrNode(topologyShard.getNodes().get(0));
    }

    @Ignore
    @Test
    public void createTopologyShardForRdmWithoutClliTest() {
        Nodes mappingNode = NetworkmodelTestUtil.createMappingForRdm("ROADMA01", null, 2, 0);
        TopologyShard topologyShard = OpenRoadmTopology.createTopologyShard(mappingNode);
        assertNull("clli must not be null", topologyShard);
    }

    @Test
    public void deleteLinkOkTest() throws InterruptedException, ExecutionException {
        String srcNode = "ROADM-A1-DEG1";
        String dstNode = "ROADM-A1-SRG1";
        String srcTp = "DEG1-CTP-TXRX";
        String destTp = "SRG1-CP-TXRX";
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Link1 link1 =
            new Link1Builder().build();
        Link link = new LinkBuilder()
            .setLinkId(linkId)
            .setSource(new SourceBuilder().setSourceNode(new NodeId(srcNode)).setSourceTp(srcTp).build())
            .setDestination(new DestinationBuilder().setDestNode(new NodeId(dstNode)).setDestTp(destTp).build())
            .addAugmentation(link1)
            .build();
        InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Networks.class).child(Network.class,
            new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).augmentation(Network1.class)
            .child(Link.class, new LinkKey(linkId)).build();
        when(networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, linkIID))
            .thenReturn(new LinkFuture(link));

        Answer<FluentFuture<CommitInfo>> answer = new Answer<FluentFuture<CommitInfo>>() {

            @Override
            public FluentFuture<CommitInfo> answer(InvocationOnMock invocation) throws Throwable {
                return CommitInfo.emptyFluentFuture();
            }

        };
        when(networkTransactionService.commit()).then(answer);

        boolean result = OpenRoadmTopology.deleteLink("ROADM-A1-DEG1", "ROADM-A1-SRG1", "DEG1-CTP-TXRX", "SRG1-CP-TXRX",
            networkTransactionService);
        assertTrue("link deletion should be ok", result);
    }

    @Test
    public void deleteLinkNotOkTest() throws InterruptedException, ExecutionException {
        String srcNode = "ROADM-A1-DEG1";
        String dstNode = "ROADM-A1-SRG1";
        String srcTp = "DEG1-CTP-TXRX";
        String destTp = "SRG1-CP-TXRX";
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);

        InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Networks.class).child(Network.class,
            new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).augmentation(Network1.class)
            .child(Link.class, new LinkKey(linkId)).build();
        when(networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, linkIID)).thenReturn(new LinkFuture());

        boolean result = OpenRoadmTopology.deleteLink("ROADM-A1-DEG1", "ROADM-A1-SRG1", "DEG1-CTP-TXRX", "SRG1-CP-TXRX",
            networkTransactionService);
        assertFalse("link deletion should not be ok", result);
    }

    @Test
    public void deleteLinkExceptionTest() throws InterruptedException, ExecutionException {
        String srcNode = "ROADM-A1-DEG1";
        String dstNode = "ROADM-A1-SRG1";
        String srcTp = "DEG1-CTP-TXRX";
        String destTp = "SRG1-CP-TXRX";
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);

        InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(linkId)).build();
        when(networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, linkIID))
                .thenReturn(new InterruptedLinkFuture());
        boolean result = OpenRoadmTopology.deleteLink("ROADM-A1-DEG1", "ROADM-A1-SRG1", "DEG1-CTP-TXRX", "SRG1-CP-TXRX",
                networkTransactionService);
        verify(networkTransactionService, never()).merge(any(), any(), any());
        assertFalse("Result should be false du to InterruptedException", result);
    }

    private void checkDegreeNode(String nodeNb, Node node) {
        assertEquals("ROADMA01-DEG" + nodeNb, node.getNodeId().getValue());
        List<SupportingNode> supportingNodes = node.nonnullSupportingNode().values().stream()
            .sorted((sn1, sn2) -> sn1.getNetworkRef().getValue().compareTo(sn2.getNetworkRef().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, supportingNodes.size());
        assertEquals("clli-network", supportingNodes.get(0).getNetworkRef().getValue());
        assertEquals("nodeA", supportingNodes.get(0).getNodeRef().getValue());
        assertEquals("openroadm-network", supportingNodes.get(1).getNetworkRef().getValue());
        assertEquals("ROADMA01", supportingNodes.get(1).getNodeRef().getValue());
        assertEquals(OpenroadmNodeType.DEGREE, node.augmentation(Node1.class).getNodeType());
        assertEquals(Uint16.valueOf(nodeNb), node.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1.class)
            .getDegreeAttributes().getDegreeNumber());
        List<AvailFreqMaps> availFreqMapsValues = new ArrayList<>(node.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1.class)
            .getDegreeAttributes().getAvailFreqMaps().values());
        assertEquals(GridConstant.NB_OCTECTS, availFreqMapsValues.get(0).getFreqMap().length);
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        assertEquals(Arrays.toString(byteArray), Arrays.toString(availFreqMapsValues.get(0).getFreqMap()));
        List<TerminationPoint> tps = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .nonnullTerminationPoint().values().stream()
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, tps.size());
        assertEquals("DEG" + nodeNb + "-CTP-TXRX", tps.get(0).getTpId().getValue());
        assertEquals(OpenroadmTpType.DEGREETXRXCTP, tps.get(0).augmentation(TerminationPoint1.class).getTpType());
        assertEquals("DEG" + nodeNb + "-TTP-TXRX", tps.get(1).getTpId().getValue());
        assertEquals(OpenroadmTpType.DEGREETXRXTTP, tps.get(1).augmentation(TerminationPoint1.class).getTpType());
    }

    private void checkSrgNode(String nodeNb, Node node) {
        assertEquals("ROADMA01-SRG" + nodeNb, node.getNodeId().getValue());
        List<SupportingNode> supportingNodes = node.nonnullSupportingNode().values().stream()
            .sorted((sn1, sn2) -> sn1.getNetworkRef().getValue().compareTo(sn2.getNetworkRef().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, supportingNodes.size());
        assertEquals("clli-network", supportingNodes.get(0).getNetworkRef().getValue());
        assertEquals("nodeA", supportingNodes.get(0).getNodeRef().getValue());
        assertEquals("openroadm-network", supportingNodes.get(1).getNetworkRef().getValue());
        assertEquals("ROADMA01", supportingNodes.get(1).getNodeRef().getValue());
        assertEquals(OpenroadmNodeType.SRG, node.augmentation(Node1.class).getNodeType());
        List<AvailFreqMaps> availFreqMapsValues = new ArrayList<>(node.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1.class)
                .getSrgAttributes().getAvailFreqMaps().values());
        assertEquals(GridConstant.NB_OCTECTS, availFreqMapsValues.get(0).getFreqMap().length);
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        assertEquals(Arrays.toString(byteArray), Arrays.toString(availFreqMapsValues.get(0).getFreqMap()));
        List<TerminationPoint> tps = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .nonnullTerminationPoint().values().stream()
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        assertEquals(5, tps.size());
        assertEquals("SRG" + nodeNb + "-CP-TXRX", tps.get(0).getTpId().getValue());
        assertEquals(OpenroadmTpType.SRGTXRXCP, tps.get(0).augmentation(TerminationPoint1.class).getTpType());
        assertEquals("SRG" + nodeNb + "-PP3-TXRX", tps.get(3).getTpId().getValue());
        assertEquals(OpenroadmTpType.SRGTXRXPP, tps.get(3).augmentation(TerminationPoint1.class).getTpType());
    }

    private void checkTpdrNode(Node node) {
        assertEquals("XPDRA01-XPDR1", node.getNodeId().getValue());
        List<SupportingNode> supportingNodes = node.nonnullSupportingNode().values().stream()
            .sorted((sn1, sn2) -> sn1.getNetworkRef().getValue().compareTo(sn2.getNetworkRef().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, supportingNodes.size());
        assertEquals("clli-network", supportingNodes.get(0).getNetworkRef().getValue());
        assertEquals("nodeA", supportingNodes.get(0).getNodeRef().getValue());
        assertEquals("openroadm-network", supportingNodes.get(1).getNetworkRef().getValue());
        assertEquals("XPDRA01", supportingNodes.get(1).getNodeRef().getValue());
        assertEquals(OpenroadmNodeType.XPONDER, node.augmentation(Node1.class).getNodeType());
        List<TerminationPoint> tps = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .nonnullTerminationPoint().values().stream()
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        assertEquals(4, tps.size());
        assertEquals("XPDR1-CLIENT1", tps.get(0).getTpId().getValue());
        assertEquals(OpenroadmTpType.XPONDERCLIENT, tps.get(0).augmentation(TerminationPoint1.class).getTpType());
        assertEquals("XPDR1-NETWORK1", tps.get(0).augmentation(
            org.opendaylight.yang.gen.v1.http.transportpce.topology.rev201019.TerminationPoint1.class)
            .getAssociatedConnectionMapPort());
        assertEquals("XPDR1-NETWORK1", tps.get(2).getTpId().getValue());
        assertEquals(OpenroadmTpType.XPONDERNETWORK, tps.get(2).augmentation(TerminationPoint1.class).getTpType());
        assertEquals("XPDR1-CLIENT1", tps.get(2).augmentation(
            org.opendaylight.yang.gen.v1.http.transportpce.topology.rev201019.TerminationPoint1.class)
            .getAssociatedConnectionMapPort());
    }

    private void checkOtnXpdrNode(Node node) {
        assertEquals("XPDRA01-XPDR1", node.getNodeId().getValue());
        List<SupportingNode> supportingNodes = node.nonnullSupportingNode().values().stream()
            .sorted((sn1, sn2) -> sn1.getNetworkRef().getValue().compareTo(sn2.getNetworkRef().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, supportingNodes.size());
        assertEquals("clli-network", supportingNodes.get(0).getNetworkRef().getValue());
        assertEquals("nodeA", supportingNodes.get(0).getNodeRef().getValue());
        assertEquals("openroadm-network", supportingNodes.get(1).getNetworkRef().getValue());
        assertEquals("XPDRA01", supportingNodes.get(1).getNodeRef().getValue());
        assertEquals(OpenroadmNodeType.XPONDER, node.augmentation(Node1.class).getNodeType());
        List<TerminationPoint> tps = node.augmentation(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .nonnullTerminationPoint().values().stream()
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        assertEquals(2, tps.size());
        assertEquals("XPDR1-NETWORK1", tps.get(0).getTpId().getValue());
        assertEquals(OpenroadmTpType.XPONDERNETWORK, tps.get(0).augmentation(TerminationPoint1.class).getTpType());
    }

    private class LinkFuture implements  ListenableFuture<Optional<Link>> {

        private Link link;

        LinkFuture() {
        }

        LinkFuture(Link link) {
            this.link = link;
        }

        @Override
        public boolean cancel(boolean arg0) {
            return false;
        }

        @Override
        public Optional<Link> get() throws InterruptedException, ExecutionException {
            if (link != null) {
                return Optional.of(link);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Optional<Link> get(long arg0, TimeUnit arg1)
            throws InterruptedException, ExecutionException, TimeoutException {
            if (link != null) {
                return Optional.of(link);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public void addListener(Runnable listener, Executor executor) {
        }
    }

    private class InterruptedLinkFuture implements  ListenableFuture<Optional<Link>> {

        @Override
        public boolean cancel(boolean arg0) {
            return false;
        }

        @Override
        public Optional<Link> get() throws InterruptedException, ExecutionException {
            throw new InterruptedException("Interrupted");
        }

        @Override
        public Optional<Link> get(long arg0, TimeUnit arg1)
            throws InterruptedException, ExecutionException, TimeoutException {
            throw new InterruptedException("Interrupted");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public void addListener(Runnable listener, Executor executor) {
        }
    }
}
