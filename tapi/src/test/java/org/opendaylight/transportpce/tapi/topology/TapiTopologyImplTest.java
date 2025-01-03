/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.impl.rpc.GetLinkDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetNodeDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetNodeEdgePointDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetServiceInterfacePointDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetServiceInterfacePointListImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetTopologyDetailsImpl;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.get.service._interface.point.list.output.Sip;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.get.service._interface.point.list.output.SipKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetLinkDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeEdgePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.topology.details.output.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class TapiTopologyImplTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyImplTest.class);

    @Mock
    private RpcService rpcService;
    private static final int NUM_THREADS = 3;
    private static NetworkTransactionService networkTransactionService;
    private static TapiContext tapiContext;
    private static TopologyUtils topologyUtils;
    private static ConnectivityUtils connectivityUtils;
    private static ServiceDataStoreOperations serviceDataStoreOperations;
    private static TapiInitialORMapping tapiInitialORMapping;
    private static TapiLink tapiLink;

    @BeforeAll
    static void setUp() throws InterruptedException, ExecutionException {
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        new CountDownLatch(1);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE, InstanceIdentifiers.OVERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_NETWORK_FILE, InstanceIdentifiers.UNDERLAY_NETWORK_II.toIdentifier());
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.PORTMAPPING_FILE);
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapiLink = new TapiLinkImpl(networkTransactionService, new TapiContext(networkTransactionService));
        serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(getDataStoreContextUtil().getDataBroker());
        tapiContext = new TapiContext(networkTransactionService);
        topologyUtils = new TopologyUtils(networkTransactionService, getDataStoreContextUtil().getDataBroker(),
            tapiLink);
        connectivityUtils = new ConnectivityUtils(serviceDataStoreOperations, new HashMap<>(), tapiContext,
            networkTransactionService,
            new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER.getBytes(StandardCharsets.UTF_8))
                .toString()));
        tapiInitialORMapping = new TapiInitialORMapping(topologyUtils, connectivityUtils,
            tapiContext, serviceDataStoreOperations);
        tapiInitialORMapping.performTopoInitialMapping();
        LOG.info("setup done");
    }

    @Test
    void getTopologyDetailsForTransponder100GTopologyWhenSuccessful() throws ExecutionException, InterruptedException {
        Uuid topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.TPDR_100G.getBytes(
            Charset.forName("UTF-8"))).toString());
        LOG.info("TPDR100GUuid = {}", topologyUuid);
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(topologyUuid);
//        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(networkTransactionService, tapiContext, topologyUtils,
//            tapiLink);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = new GetTopologyDetailsImpl(tapiContext,
                topologyUtils, tapiLink, networkTransactionService)
            .invoke(input);
        LOG.info("RESULT of getTopoDetailsTopo/name = {}", result.get().getResult().getTopology().getName().toString());
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        assertNotNull(topology, "Topology should not be null");
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.TPDR_100G.getBytes(StandardCharsets.UTF_8))
            .toString());
        assertEquals(topoUuid, topology.getUuid(), "incorrect topology uuid");
        assertEquals(1, topology.getNode().size(), "Node list size should be 1");
        Name nodeName = topology.getNode().values().stream().findFirst().orElseThrow().getName()
            .get(new NameKey("Tpdr100g node name"));
        assertEquals("Tpdr100g over WDM node", nodeName.getValue(), "Node name should be 'Tpdr100g over WDM node'");
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(nodeName.getValue().getBytes(StandardCharsets.UTF_8))
            .toString());
        assertEquals(nodeUuid, topology.getNode().values().stream().findFirst().orElseThrow().getUuid(),
            "incorrect node uuid");
        long nb = topology.getNode().values().stream().findFirst().orElseThrow().getOwnedNodeEdgePoint().size();
        assertEquals(2, nb, "'Transponder 100GE' node should have 2 neps");
        List<NodeRuleGroup> nrgList = topology.getNode().values().stream().findFirst().orElseThrow()
            .nonnullNodeRuleGroup().values().stream()
            .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals(1, nrgList.size(), "'Transponder 100GE' node should contain a single node rule groups");
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        assertEquals(nb, nodeEdgePointList.size(), "'Transponder 100GE' node -rule-group should contain 2 NEPs");
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals(1, ruleList.size(), "node-rule-group should contain a single rule");
        assertEquals("forward", ruleList.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType().iterator().next(),
            "the rule type should be 'FORWARDING'");
    }

    @Test
    void getTopologyDetailsForOtnTopologyWithOtnLinksWhenSuccessful() throws ExecutionException, InterruptedException {
        Uuid topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        LOG.info("T0MultilayerUuid = {}", topologyUuid);
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(topologyUuid);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = new GetTopologyDetailsImpl(tapiContext,
                topologyUtils, tapiLink, networkTransactionService)
            .invoke(input);
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        assertNotNull(topology, "Topology should not be null");
        for (Map.Entry<
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> entry :
                topology.getNode().entrySet()) {
            LOG.debug("NODESDETECTED = {}",entry.getValue().getName().toString());
        }
        assertEquals(7, topology.getNode().size(), "Node list size should be 7");
        long nb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("XPDR-A1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("100G-tpdr"))))
            .count();
        assertEquals(1, nb1, "XPDR-A1-XPDR1 should only have one client nep");
        long nb2 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_C"))))
            .count();
        assertEquals(4, nb2, "SPDR-SA1-XPDR1 (mux) should have 4 client neps");
        long nb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals(1, nb3, "SPDR-SA1-XPDR1 (mux) should have a single network nep");
        long nb4 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR2+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_C"))))
            .count();
        assertEquals(4, nb4, "SPDR-SA1-XPDR2 (switch) should have 4 client neps");
        long nb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR2+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals(2, nb5, "SPDR-SA1-XPDR2 (switch) should have 2 network neps");
        long nb7 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)
                && node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("otsi node name")).getValue()
                .equals("XPDR-A1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint"))))
            .count();
        assertEquals(1, nb7, "XPDR-A1-XPDR1 should only have 1 OTSI network nep");
        long nb8 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)
                && node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("otsi node name")).getValue()
                .equals("SPDR-SA1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint"))))
            .count();
        assertEquals(1, nb8, "SPDR-SA1-XPDR1 (mux) should have a single OTSI network nep");
        long nb9 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)
                && node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("otsi node name")).getValue()
                .equals("SPDR-SA1-XPDR2+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint"))))
            .count();
        assertEquals(2, nb9, "SPDR-SA1-XPDR2 (switch) should have 2 OTSI network nep");

        assertEquals(10, topology.getLink().size(), "Link list size should be 10, no more transitionnal links");
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes("T0 - Multi-layer topology".getBytes()).toString());
        assertEquals(topoUuid, topology.getUuid(), "incorrect topology uuid");
        assertEquals(
            "T0 - Multi-layer topology",
            topology.nonnullName().values().stream().findFirst().orElseThrow().getValue(),
            "topology name should be T0 - Multi-layer topology");

        long nbDsrOduNodes = topology.nonnullNode().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("dsr/odu node name"))).count();
        long nbPhotonicNodes = topology.nonnullNode().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("otsi node name"))).count();
        assertEquals(6, nbDsrOduNodes, "Node list should contain 6 DSR-ODU nodes");
        assertEquals(7, nbPhotonicNodes, "Node list should contain 7 Photonics node");
        long nbOtsLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("OTS link name"))).count();
        long nbOtnLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("otn link name"))).count();
        assertEquals(8, nbOtsLinks, "Link list should contain 8 OTS links");
        assertEquals(2, nbOtnLinks, "Link list should contain 2 OTN links");

        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+XPONDER".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+XPONDER".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+XPONDER".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+XPONDER".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1"
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1"
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid tp3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid tp4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid link1Uuid =
            new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1toSPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1"
                .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid link2Uuid =
            new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1toSPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1"
                .getBytes(StandardCharsets.UTF_8)).toString());

        List<Link> links = topology.nonnullLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("otn link name")))
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkOtnLink(links.get(0), topoUuid, node3Uuid, node4Uuid, tp3Uuid, tp4Uuid, link1Uuid,
            "SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1toSPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1");
        checkOtnLink(links.get(1), topoUuid, node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, link2Uuid,
            "SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1toSPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1");

    }

    @Test
    void getTopologyDetailsForFullTapiTopologyWithLinksWhenSuccessful()
            throws ExecutionException, InterruptedException {
        Uuid topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        LOG.info("T0FullMultilayerUuid = {}", topologyUuid);
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(topologyUuid);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = new GetTopologyDetailsImpl(tapiContext,
                topologyUtils, tapiLink, networkTransactionService)
            .invoke(input);
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        assertNotNull(topology, "Topology should not be null");
        // 2 ROADM Nodes, 2 XPDR Nodes, 6 SPDR Nodes
        assertEquals(10, topology.getNode().size(), "Node list size should be 10");
        List<Map<NameKey, Name>> nodeNames = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .map(Node::getName).collect(Collectors.toList());
        LOG.info("TopologyNodes = {}", nodeNames.toString());
        Node nodeTsp = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("XPDR-A1-XPDR1+XPONDER"))
            .findAny().orElseThrow();
        LOG.debug("XPDRA1 NEPs = {}", nodeTsp.getOwnedNodeEdgePoint().toString());
        long nb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("XPDR-A1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("100G-tpdr"))))
            .count();
        // 2 client ports in configuration -> removed the checkTp so we have 2 NEPs
        assertEquals(2, nb1, "XPDR-A1-XPDR1+XPONDER should only have two client neps");
        long inb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("XPDR-A1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals(2, inb1, "XPDR-A1-XPDR1+XPONDER should only have two internal network neps");
        long enb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("XPDR-A1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint_N"))))
            .count();
        assertEquals(2, enb1, "XPDR-A1-XPDR1+XPONDER should only have two external network neps");
        long nb2 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_C"))))
            .count();
        assertEquals(4, nb2, "SPDR-SA1-XPDR1+XPONDER (mux) should have 4 client neps");
        long inb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals(1, inb3, "SPDR-SA1-XPDR1+XPONDER (mux) should have a single internal network nep");
        long enb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint_N"))))
            .count();
        assertEquals(4, enb3, "SPDR-SA1-XPDR1+XPONDER (mux) should have 4 external network nep");
        long nb4 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR2+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_C"))))
            .count();
        assertEquals(4, nb4, "SPDR-SA1-XPDR2+XPONDER (switch) should have 4 client neps");
        long inb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR2+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals(4, inb5, "SPDR-SA1-XPDR2+XPONDER (switch) should have 4 internal network neps");
        long enb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint_N"))))
            .count();
        assertEquals(4, enb5, "SPDR-SA1-XPDR2+XPONDER (switch) should have 4 external network neps");

        inb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("XPDR-A1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint_N"))))
            .count();
        assertEquals(2, inb1, "XPDR-A1-XPDR1+XPONDER should only have two external network neps");
        enb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("XPDR-A1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals(2, enb1, "XPDR-A1-XPDR1+XPONDER should only have two photonic network neps");
        inb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR1+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint"))))
            .count();
        assertEquals(1, inb3, "SPDR-SA1-XPDR1+XPONDER (mux) should have a single external network nep");
        inb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("dsr/odu node name")).getValue()
                .equals("SPDR-SA1-XPDR2+XPONDER"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint"))))
            .count();
        assertEquals(4, inb5, "SPDR-SA1-XPDR2+XPONDER (switch) should have 4 external network neps");
        long inb6 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)
                && !node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("roadm node name")).getValue()
                .equals("ROADM-A1+PHOTONIC_MEDIA"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().values().stream().findAny().orElseThrow().getValue().contains("DEG")))
            .count();
        assertEquals(4, inb6, "ROADM-A1+PHOTONIC_MEDIA (DEGREE) should have 4 network neps");
        long enb6 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)
                && !node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().get(new NameKey("roadm node name")).getValue()
                .equals("ROADM-A1+PHOTONIC_MEDIA"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().values().stream().findAny().orElseThrow().getValue().contains("SRG")))
            .count();
        assertEquals(8, enb6, "ROADM-A1+PHOTONIC_MEDIA (SRG) should have 8 network neps (OTS)");

        // Links in openroadm topology which include Roadm-to-Roadm and Xpdr-to-Roadm (ortopo / 2)
        // + transitional links -> 0 per network port of Xpdr + OTN links / 2
        List<String> linkList = new ArrayList<>();
        for (Map.Entry<LinkKey, Link> entry : topology.getLink().entrySet()) {
            linkList.add(entry.getValue().getName().entrySet().iterator().next().getValue().toString());
        }
        assertEquals(9, topology.getLink().size(), "Link list size should be 8 XPDR To SRG and 1 DEG2A-DEG1C");
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes("T0 - Full Multi-layer topology".getBytes()).toString());
        assertEquals(topoUuid, topology.getUuid(), "incorrect topology uuid");
        assertEquals(
            "T0 - Full Multi-layer topology",
            topology.nonnullName().values().stream().findFirst().orElseThrow().getValue(),
            "topology name should be T0 - Full Multi-layer topology");

        long nbDsrOduNodes = topology.nonnullNode().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("dsr/odu node name"))).count();
        long nbPhotonicNodes = topology.nonnullNode().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("otsi node name"))).count();
        // In DSR/ODU we create one node per Xpdr (no filtering out)
        assertEquals(8, nbDsrOduNodes, "Node list should contain 8 DSR-ODU nodes");
        // We need to add the Roadms as Photonic nodes. Instead of 1 node as roadm infra we have 2 roadm nodes
        assertEquals(8, nbPhotonicNodes, "Node list should contain 8 Photonics nodes");
        // Roadm-to-Roadm
        long nbOmsLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("OMS link name"))).count();
        // Xpdr-to-Roadm
        long nbOmsLinks1 = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("XPDR-RDM link name"))).count();
        // 1 OMS per ROADM-to-ROADM link + Existing XPDR-tp-ROADM link in openroadm topology
        assertEquals(9, nbOmsLinks + nbOmsLinks1, "Link list should contain 9 OMS links");
    }

    @Test
    void getNodeAndNepsDetailsWhenSuccessful() throws ExecutionException, InterruptedException {
        Uuid topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(topologyUuid);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = new GetTopologyDetailsImpl(tapiContext,
                    topologyUtils, tapiLink, networkTransactionService)
                .invoke(input);
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        for (Node node:topology.getNode().values()) {
            Uuid nodeUuid = node.getUuid();
            GetNodeDetailsInput input1 = TapiTopologyDataUtils.buildGetNodeDetailsInput(topologyUuid, nodeUuid);
            ListenableFuture<RpcResult<GetNodeDetailsOutput>> result1 = new GetNodeDetailsImpl(tapiContext)
                    .invoke(input1);
            RpcResult<GetNodeDetailsOutput> rpcResult1 = result1.get();
            @Nullable
            Node node1 = rpcResult1.getResult().getNode();
            assertNotNull(node1, "Node should not be null");
            for (OwnedNodeEdgePoint onep:node1.getOwnedNodeEdgePoint().values()) {
                Uuid onepUuid = onep.getUuid();
                GetNodeEdgePointDetailsInput input2 = TapiTopologyDataUtils.buildGetNodeEdgePointDetailsInput(
                    topologyUuid, nodeUuid, onepUuid);
                ListenableFuture<RpcResult<GetNodeEdgePointDetailsOutput>> result2 =
                        new GetNodeEdgePointDetailsImpl(tapiContext).invoke(input2);
                RpcResult<GetNodeEdgePointDetailsOutput> rpcResult2 = result2.get();
                org.opendaylight.yang.gen.v1
                    .urn.onf.otcc.yang.tapi.topology.rev221121.get.node.edge.point.details.output.NodeEdgePoint
                    onep1 = rpcResult2.getResult().getNodeEdgePoint();
                assertNotNull(onep1, "Node Edge Point should not be null");
            }
        }
    }

    @Test
    void getLinkDetailsWhenSuccessful() throws ExecutionException, InterruptedException {
        Uuid topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(topologyUuid);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = new GetTopologyDetailsImpl(tapiContext,
                topologyUtils, tapiLink, networkTransactionService)
            .invoke(input);
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        for (Link link:topology.getLink().values()) {
            Uuid linkUuid = link.getUuid();
            GetLinkDetailsInput input1 = TapiTopologyDataUtils.buildGetLinkDetailsInput(
                topologyUuid, linkUuid);
            ListenableFuture<RpcResult<GetLinkDetailsOutput>> result1 = new GetLinkDetailsImpl(tapiContext)
                    .invoke(input1);
            RpcResult<GetLinkDetailsOutput> rpcResult1 = result1.get();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.link.details.output.Link link1
                = rpcResult1.getResult().getLink();
            assertNotNull(link1, "Link should not be null");
        }
    }

    @Test
    void getSipDetailsWhenSuccessful() throws ExecutionException, InterruptedException {
        GetServiceInterfacePointListInput input = TapiTopologyDataUtils.buildServiceInterfacePointListInput();
        ListenableFuture<RpcResult<GetServiceInterfacePointListOutput>> result =
                new GetServiceInterfacePointListImpl(tapiContext).invoke(input);
        RpcResult<GetServiceInterfacePointListOutput> rpcResult = result.get();
        Map<SipKey, Sip> sipMap = rpcResult.getResult().getSip();
        for (Sip sip:sipMap.values()) {
            Uuid sipUuid = sip.getUuid();
            GetServiceInterfacePointDetailsInput input1 = TapiTopologyDataUtils
                .buildGetServiceInterfacePointDetailsInput(sipUuid);
            ListenableFuture<RpcResult<GetServiceInterfacePointDetailsOutput>> result1 =
                    new GetServiceInterfacePointDetailsImpl(tapiContext).invoke(input1);
            RpcResult<GetServiceInterfacePointDetailsOutput> rpcResult1 = result1.get();
            org.opendaylight.yang.gen.v1
                .urn.onf.otcc.yang.tapi.common.rev221121.get.service._interface.point.details.output.Sip sip1
                = rpcResult1.getResult().getSip();
            assertNotNull(sip1, "Sip should not be null");
        }
    }

    private void checkOtnLink(Link link, Uuid topoUuid, Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid,
            Uuid linkUuid, String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("otn link name")).getValue(), "bad name for the link");
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link");
        assertEquals(CAPACITYUNITGBPS.VALUE, link.getAvailableCapacity().getTotalSize().getUnit(),
            "Available capacity unit should be MBPS");
        String prefix = linkName.split("-")[0];
        if ("OTU4".equals(prefix)) {
            assertEquals(Uint64.valueOf(0), link.getAvailableCapacity().getTotalSize().getValue(),
                "Available capacity -total size value should be 0");
        } else if ("ODTU4".equals(prefix)) {
            assertEquals(Uint64.valueOf(100000), link.getAvailableCapacity().getTotalSize().getValue(),
                "Available capacity -total size value should be 100 000");
        }
        assertEquals(CAPACITYUNITGBPS.VALUE, link.getTotalPotentialCapacity().getTotalSize().getUnit(),
            "Total capacity unit should be GBPS");
        assertEquals(Decimal64.valueOf("100"), link.getTotalPotentialCapacity().getTotalSize().getValue(),
            "Total capacity -total size value should be 100");
        if ("OTU4".equals(prefix)) {
            assertEquals("otn link should be between 2 nodes of protocol layers PHOTONIC_MEDIA",
                LayerProtocolName.PHOTONICMEDIA.getName(),
                link.getLayerProtocolName().stream().findFirst().orElseThrow().getName());
        } else if ("ODTU4".equals(prefix)) {
            assertEquals("otn link should be between 2 nodes of protocol layers ODU",
                LayerProtocolName.ODU.getName(),
                link.getLayerProtocolName().stream().findFirst().orElseThrow().getName());
        }
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(),
            "transitional link should be BIDIRECTIONAL");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals(topoUuid, nodeEdgePointList.get(0).getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertEquals(topoUuid, nodeEdgePointList.get(1).getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertThat("otn links should terminate on two distinct nodes",
            nodeEdgePointList.get(0).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        assertThat("otn links should terminate on two distinct nodes",
            nodeEdgePointList.get(1).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        assertThat("otn links should terminate on two distinct tps",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(tp1Uuid.getValue())).or(containsString(tp2Uuid.getValue())));
        assertThat("otn links should terminate on two distinct tps",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(tp1Uuid.getValue())).or(containsString(tp2Uuid.getValue())));
        assertEquals(OperationalState.ENABLED, link.getOperationalState(), "operational state should be ENABLED");
        assertEquals(AdministrativeState.UNLOCKED, link.getAdministrativeState(),
            "administrative state should be UNLOCKED");
    }
}