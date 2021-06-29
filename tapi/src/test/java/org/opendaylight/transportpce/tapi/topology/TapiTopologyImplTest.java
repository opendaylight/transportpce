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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.Sip;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.SipKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetLinkDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetNodeEdgePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiTopologyImplTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyImplTest.class);

    private static ListeningExecutorService executorService;
    private static CountDownLatch endSignal;
    private static final int NUM_THREADS = 3;
    public static NetworkTransactionService networkTransactionService;
    public static TapiContext tapiContext;
    public static TopologyUtils topologyUtils;
    public static ConnectivityUtils connectivityUtils;
    public static ServiceDataStoreOperations serviceDataStoreOperations;
    public static TapiInitialORMapping tapiInitialORMapping;

    @BeforeClass
    public static void setUp() throws InterruptedException, ExecutionException {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE, InstanceIdentifiers.OVERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                TapiTopologyDataUtils.OPENROADM_NETWORK_FILE, InstanceIdentifiers.UNDERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                TapiTopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
                TapiTopologyDataUtils.PORTMAPPING_FILE);
        networkTransactionService = new NetworkTransactionImpl(
                new RequestProcessor(getDataStoreContextUtil().getDataBroker()));
        serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(getDataStoreContextUtil().getDataBroker());
        tapiContext = new TapiContext(networkTransactionService);
        topologyUtils = new TopologyUtils(networkTransactionService, getDataStoreContextUtil().getDataBroker());
        connectivityUtils = new ConnectivityUtils(serviceDataStoreOperations, new HashMap<>(), tapiContext);
        tapiInitialORMapping = new TapiInitialORMapping(topologyUtils, connectivityUtils,
                tapiContext, serviceDataStoreOperations);
        tapiInitialORMapping.performTopoInitialMapping();
        LOG.info("setup done");
    }

    @Test
    public void getTopologyDetailsForTransponder100GTopologyWhenSuccessful()
            throws ExecutionException, InterruptedException {
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(TopologyUtils.TPDR_100G);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(getDataBroker(), tapiContext, topologyUtils);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);
        endSignal.await();
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        assertNotNull("Topology should not be null", topology);
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.TPDR_100G.getBytes(StandardCharsets.UTF_8))
            .toString());
        assertEquals("incorrect topology uuid", topoUuid, topology.getUuid());
        assertEquals("Node list size should be 1", 1, topology.getNode().size());
        Name nodeName = topology.getNode().values().stream().findFirst().get().getName()
            .get(new NameKey("Tpdr100g node name"));
        assertEquals("Node name should be 'Tpdr100g over WDM node'", "Tpdr100g over WDM node", nodeName.getValue());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(nodeName.getValue().getBytes(StandardCharsets.UTF_8))
            .toString());
        assertEquals("incorrect node uuid", nodeUuid, topology.getNode().values().stream().findFirst().get().getUuid());
        long nb = topology.getNode().values().stream().findFirst().get().getOwnedNodeEdgePoint().size();
        assertEquals("'Transponder 100GE' node should have 2 neps", 2, nb);
        List<NodeRuleGroup> nrgList = topology.getNode().values().stream().findFirst().get().nonnullNodeRuleGroup()
            .values().stream().sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals("'Transponder 100GE' node should contain a single node rule groups", 1, nrgList.size());
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        assertEquals("'Transponder 100GE' node -rule-group should contain 2 NEPs", nb, nodeEdgePointList.size());
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals("node-rule-group should contain a single rule", 1, ruleList.size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", ruleList.get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, ruleList.get(0).getRuleType());
    }

    @Test
    public void getTopologyDetailsForOtnTopologyWithOtnLinksWhenSuccessful()
            throws ExecutionException, InterruptedException {
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(TopologyUtils.T0_MULTILAYER);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(getDataBroker(), tapiContext, topologyUtils);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);
        endSignal.await();
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        assertNotNull("Topology should not be null", topology);
        assertEquals("Node list size should be 13", 13, topology.getNode().size());
        long nb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("XPDR-A1-XPDR1"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("100G-tpdr"))))
            .count();
        assertEquals("XPDR-A1-XPDR1 should only have one client nep", 1, nb1);
        long nb2 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR1"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_C"))))
            .count();
        assertEquals("SPDR-SA1-XPDR1 (mux) should have 4 client neps", 4, nb2);
        long nb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR1"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_N"))))
            .count();
        assertEquals("SPDR-SA1-XPDR1 (mux) should have a single network nep", 1, nb3);
        long nb4 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR2"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_C"))))
            .count();
        assertEquals("SPDR-SA1-XPDR2 (switch) should have 4 client neps", 4, nb4);
        long nb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR2"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_N"))))
            .count();
        assertEquals("SPDR-SA1-XPDR2 (switch) should have 2 network neps", 2, nb5);
        assertEquals("Link list size should be 18", 18, topology.getLink().size());
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes("T0 - Multi-layer topology".getBytes()).toString());
        assertEquals("incorrect topology uuid", topoUuid, topology.getUuid());
        assertEquals("topology name should be T0 - Multi-layer topology",
            "T0 - Multi-layer topology",
            topology.nonnullName().values().stream().findFirst().get().getValue());

        long nbDsrOduNodes = topology.nonnullNode().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("dsr/odu node name"))).count();
        long nbPhotonicNodes = topology.nonnullNode().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("otsi node name"))).count();
        assertEquals("Node list should contain 6 DSR-ODU nodes", 6, nbDsrOduNodes);
        assertEquals("Node list should contain 7 Photonics nodes", 7, nbPhotonicNodes);
        long nbTransititionalLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("transitional link name"))).count();
        long nbOmsLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("OMS link name"))).count();
        long nbOtnLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("otn link name"))).count();
        assertEquals("Link list should contain 8 transitional links", 8, nbTransititionalLinks);
        assertEquals("Link list should contain 8 transitional links", 8, nbOmsLinks);
        assertEquals("Link list should contain 2 OTN links", 2, nbOtnLinks);

        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+DSR".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+OTSi".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR+XPDR1-NETWORK1"
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+DSR+XPDR1-NETWORK1"
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid tp3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid tp4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid link1Uuid =
            new Uuid(UUID.nameUUIDFromBytes("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid link2Uuid =
            new Uuid(UUID.nameUUIDFromBytes("OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(StandardCharsets.UTF_8)).toString());

        List<Link> links = topology.nonnullLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("otn link name")))
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkOtnLink(links.get(0), topoUuid, node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, link1Uuid,
            "ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
        checkOtnLink(links.get(1), topoUuid, node3Uuid, node4Uuid, tp3Uuid, tp4Uuid, link2Uuid,
            "OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
    }

    @Test
    public void getTopologyDetailsForFullTapiTopologyWithLinksWhenSuccessful()
            throws ExecutionException, InterruptedException {
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(
            TopologyUtils.T0_FULL_MULTILAYER);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(getDataBroker(), tapiContext, topologyUtils);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);
        endSignal.await();
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        assertNotNull("Topology should not be null", topology);
        // 2 Nodes per Xpdr/Spdr node (DSR-ODU & PHOT) + 1 Node per Roadm
        assertEquals("Node list size should be 18", 18, topology.getNode().size());
        long nb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("XPDR-A1-XPDR1+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("100G-tpdr"))))
            .count();
        // 2 client ports in configuration -> removed the checkTp so we have 2 NEPs
        assertEquals("XPDR-A1-XPDR1+DSR should only have two client neps", 2, nb1);
        long inb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("XPDR-A1-XPDR1+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals("XPDR-A1-XPDR1+DSR should only have two internal network neps", 2, inb1);
        long enb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("XPDR-A1-XPDR1+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint_N"))))
            .count();
        assertEquals("XPDR-A1-XPDR1+DSR should only have two external network neps", 2, enb1);
        long nb2 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR1+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_C"))))
            .count();
        assertEquals("SPDR-SA1-XPDR1+DSR (mux) should have 4 client neps", 4, nb2);
        long inb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR1+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals("SPDR-SA1-XPDR1+DSR (mux) should have a single internal network nep", 1, inb3);
        long enb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR1+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint_N"))))
            .count();
        assertEquals("SPDR-SA1-XPDR1+DSR (mux) should have a single external network nep", 1, enb3);
        long nb4 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR2+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("NodeEdgePoint_C"))))
            .count();
        assertEquals("SPDR-SA1-XPDR2+DSR (switch) should have 4 client neps", 4, nb4);
        long inb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR2+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint_N"))))
            .count();
        assertEquals("SPDR-SA1-XPDR2+DSR (switch) should have 4 internal network neps", 4, inb5);
        long enb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.DSR))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR2+DSR"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint_N"))))
            .count();
        assertEquals("SPDR-SA1-XPDR2+DSR (switch) should have 4 external network neps", 4, enb5);

        // Now lets check for the Photonic media nodes (same nodes as for DSR + 1 Roadm node)
        nb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("XPDR-A1-XPDR1+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint"))))
            .count();
        // 2 client ports in configuration -> removed the checkTp so we have 2 NEPs
        assertEquals("XPDR-A1-XPDR1+OTSi should only have two internal network neps", 2, nb1);
        inb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("XPDR-A1-XPDR1+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint"))))
            .count();
        assertEquals("XPDR-A1-XPDR1+OTSi should only have two external network neps", 2, inb1);
        enb1 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("XPDR-A1-XPDR1+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("PhotMedNodeEdgePoint"))))
            .count();
        assertEquals("XPDR-A1-XPDR1+OTSi should only have two photonic network neps", 2, enb1);
        nb2 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR1+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint"))))
            .count();
        assertEquals("SPDR-SA1-XPDR1+OTSi (mux) should have a single internal network nep", 1, nb2);
        inb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR1+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint"))))
            .count();
        assertEquals("SPDR-SA1-XPDR1+OTSi (mux) should have a single external network nep", 1, inb3);
        enb3 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR1+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("PhotMedNodeEdgePoint"))))
            .count();
        assertEquals("SPDR-SA1-XPDR1+OTSi (mux) should have a single photonic network nep", 1, enb3);
        nb4 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR2+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("iNodeEdgePoint"))))
            .count();
        assertEquals("SPDR-SA1-XPDR2+OTSi (switch) should have 4 internal network neps", 4, nb4);
        inb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR2+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("eNodeEdgePoint"))))
            .count();
        assertEquals("SPDR-SA1-XPDR2+OTSi (switch) should have 4 external network neps", 4, inb5);
        enb5 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals("SPDR-SA1-XPDR2+OTSi"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().containsKey(new NameKey("PhotMedNodeEdgePoint"))))
            .count();
        assertEquals("SPDR-SA1-XPDR2+OTSi (switch) should have 4 photonic network neps", 4, enb5);
        // We should have 3 neps per DEGREE-TTP port and 3 neps per SRG-PP port
        long inb6 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals(
                "ROADM-A1+PHOTONIC_MEDIA"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().values().stream().findFirst().get().getValue().contains("DEG")))
            .count();
        assertEquals("ROADM-A1+PHOTONIC_MEDIA (DEGREE) should have 6 network neps", 6, inb6);
        long enb6 = topology.getNode().values().stream()
            .filter(node -> node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA))
            .filter(node -> node.getName().values().stream().findFirst().get().getValue().equals(
                "ROADM-A1+PHOTONIC_MEDIA"))
            .flatMap(node -> node.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getName().values().stream().findFirst().get().getValue().contains("SRG")))
            .count();
        assertEquals("ROADM-A1+PHOTONIC_MEDIA (SRG) should have 24 network neps", 24, enb6);

        // Links in openroadm topology which include Roadm-to-Roadm and Xpdr-to-Roadm (ortopo / 2)
        // + transitional links -> 1 per network port of Xpdr + OTN links / 2
        assertEquals("Link list size should be 27", 27, topology.getLink().size());
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes("T0 - Full Multi-layer topology".getBytes()).toString());
        assertEquals("incorrect topology uuid", topoUuid, topology.getUuid());
        assertEquals("topology name should be T0 - Full Multi-layer topology",
            "T0 - Full Multi-layer topology",
            topology.nonnullName().values().stream().findFirst().get().getValue());

        long nbDsrOduNodes = topology.nonnullNode().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("dsr/odu node name"))).count();
        long nbPhotonicNodes = topology.nonnullNode().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("otsi node name"))).count();
        // In DSR/ODU we create one node per Xpdr (no filtering out)
        assertEquals("Node list should contain 8 DSR-ODU nodes", 8, nbDsrOduNodes);
        // We need to add the Roadms as Photonic nodes. Instead of 1 node as roadm infra we have 2 roadm nodes
        assertEquals("Node list should contain 8 Photonics nodes", 8, nbPhotonicNodes);
        long nbTransititionalLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("transitional link name"))).count();
        // Roadm-to-Roadm
        long nbOmsLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("OMS link name"))).count();
        // Xpdr-to-Roadm
        long nbOmsLinks1 = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("XPDR-RDM link name"))).count();
        long nbOtnLinks = topology.getLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("otn link name"))).count();
        // 1 transitional link per NETWORK port
        assertEquals("Link list should contain 16 transitional links", 16, nbTransititionalLinks);
        // 1 OMS per ROADM-to-ROADM link + Existing XPDR-tp-ROADM link in openroadm topology
        assertEquals("Link list should contain 9 OMS links", 9, nbOmsLinks + nbOmsLinks1);
        // Should we consider OTN links as links or connections??
        assertEquals("Link list should contain 2 OTN links", 2, nbOtnLinks);

        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+DSR".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(StandardCharsets.UTF_8))
            .toString());
        Uuid node4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+OTSi".getBytes(StandardCharsets.UTF_8))
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
            new Uuid(UUID.nameUUIDFromBytes("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid link2Uuid =
            new Uuid(UUID.nameUUIDFromBytes("OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(StandardCharsets.UTF_8)).toString());

        List<Link> links = topology.nonnullLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("otn link name")))
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkOtnLink(links.get(0), topoUuid, node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, link1Uuid,
            "ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
        checkOtnLink(links.get(1), topoUuid, node3Uuid, node4Uuid, tp3Uuid, tp4Uuid, link2Uuid,
            "OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
    }

    @Test
    public void getNodeAndNepsDetailsWhenSuccessful()
            throws ExecutionException, InterruptedException {
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(
            TopologyUtils.T0_FULL_MULTILAYER);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(getDataBroker(), tapiContext, topologyUtils);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);
        endSignal.await();
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        for (Node node:topology.getNode().values()) {
            String nodeName = node.getName().values().stream().findFirst().get().getValue();
            GetNodeDetailsInput input1 = TapiTopologyDataUtils.buildGetNodeDetailsInput(
                TopologyUtils.T0_FULL_MULTILAYER, nodeName);
            ListenableFuture<RpcResult<GetNodeDetailsOutput>> result1 = tapiTopoImpl.getNodeDetails(input1);
            result.addListener(new Runnable() {
                @Override
                public void run() {
                    endSignal.countDown();
                }
            }, executorService);
            endSignal.await();
            RpcResult<GetNodeDetailsOutput> rpcResult1 = result1.get();
            @Nullable
            Node node1 = rpcResult1.getResult().getNode();
            assertNotNull("Node should not be null", node1);
            for (OwnedNodeEdgePoint onep:node1.getOwnedNodeEdgePoint().values()) {
                String onepName = onep.getName().values().stream().findFirst().get().getValue();
                GetNodeEdgePointDetailsInput input2 = TapiTopologyDataUtils.buildGetNodeEdgePointDetailsInput(
                    TopologyUtils.T0_FULL_MULTILAYER, nodeName, onepName);
                ListenableFuture<RpcResult<GetNodeEdgePointDetailsOutput>> result2
                    = tapiTopoImpl.getNodeEdgePointDetails(input2);
                result.addListener(new Runnable() {
                    @Override
                    public void run() {
                        endSignal.countDown();
                    }
                }, executorService);
                endSignal.await();
                RpcResult<GetNodeEdgePointDetailsOutput> rpcResult2 = result2.get();
                org.opendaylight.yang.gen.v1
                    .urn.onf.otcc.yang.tapi.topology.rev181210.get.node.edge.point.details.output.NodeEdgePoint
                        onep1 = rpcResult2.getResult().getNodeEdgePoint();
                assertNotNull("Node Edge Point should not be null", onep1);
            }
        }
    }

    @Test
    public void getLinkDetailsWhenSuccessful()
            throws ExecutionException, InterruptedException {
        GetTopologyDetailsInput input = TapiTopologyDataUtils.buildGetTopologyDetailsInput(
            TopologyUtils.T0_FULL_MULTILAYER);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(getDataBroker(), tapiContext, topologyUtils);
        ListenableFuture<RpcResult<GetTopologyDetailsOutput>> result = tapiTopoImpl.getTopologyDetails(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);
        endSignal.await();
        RpcResult<GetTopologyDetailsOutput> rpcResult = result.get();
        @Nullable
        Topology topology = rpcResult.getResult().getTopology();
        for (Link link:topology.getLink().values()) {
            String linkName = link.getName().values().stream().findFirst().get().getValue();
            GetLinkDetailsInput input1 = TapiTopologyDataUtils.buildGetLinkDetailsInput(
                TopologyUtils.T0_FULL_MULTILAYER, linkName);
            ListenableFuture<RpcResult<GetLinkDetailsOutput>> result1 = tapiTopoImpl.getLinkDetails(input1);
            result.addListener(new Runnable() {
                @Override
                public void run() {
                    endSignal.countDown();
                }
            }, executorService);
            endSignal.await();
            RpcResult<GetLinkDetailsOutput> rpcResult1 = result1.get();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.link.details.output.Link link1
                = rpcResult1.getResult().getLink();
            assertNotNull("Link should not be null", link1);
        }
    }

    @Test
    public void getSipDetailsWhenSuccessful()
            throws ExecutionException, InterruptedException {
        GetServiceInterfacePointListInput input = TapiTopologyDataUtils.buildServiceInterfacePointListInput();
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(getDataBroker(), tapiContext, topologyUtils);
        ListenableFuture<RpcResult<GetServiceInterfacePointListOutput>> result = tapiTopoImpl
            .getServiceInterfacePointList(input);
        result.addListener(new Runnable() {
            @Override
            public void run() {
                endSignal.countDown();
            }
        }, executorService);
        endSignal.await();
        RpcResult<GetServiceInterfacePointListOutput> rpcResult = result.get();
        Map<SipKey, Sip> sipMap = rpcResult.getResult().getSip();
        for (Sip sip:sipMap.values()) {
            Uuid sipUuid = sip.getUuid();
            GetServiceInterfacePointDetailsInput input1 = TapiTopologyDataUtils
                .buildGetServiceInterfacePointDetailsInput(sipUuid);
            ListenableFuture<RpcResult<GetServiceInterfacePointDetailsOutput>> result1
                = tapiTopoImpl.getServiceInterfacePointDetails(input1);
            result.addListener(new Runnable() {
                @Override
                public void run() {
                    endSignal.countDown();
                }
            }, executorService);
            endSignal.await();
            RpcResult<GetServiceInterfacePointDetailsOutput> rpcResult1 = result1.get();
            org.opendaylight.yang.gen.v1
                .urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.details.output.Sip sip1
                    = rpcResult1.getResult().getSip();
            assertNotNull("Sip should not be null", sip1);
        }
    }

    private void checkOtnLink(Link link, Uuid topoUuid, Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid,
            Uuid linkUuid, String linkName) {
        assertEquals("bad name for the link", linkName, link.getName().get(new NameKey("otn link name")).getValue());
        assertEquals("bad uuid for link", linkUuid, link.getUuid());
        assertEquals("Available capacity unit should be MBPS",
            CapacityUnit.MBPS, link.getAvailableCapacity().getTotalSize().getUnit());
        String prefix = linkName.split("-")[0];
        if ("OTU4".equals(prefix)) {
            assertEquals("Available capacity -total size value should be 0",
                Uint64.valueOf(0), link.getAvailableCapacity().getTotalSize().getValue());
        } else if ("ODTU4".equals(prefix)) {
            assertEquals("Available capacity -total size value should be 100 000",
                Uint64.valueOf(100000), link.getAvailableCapacity().getTotalSize().getValue());
        }
        assertEquals("Total capacity unit should be GBPS",
            CapacityUnit.GBPS, link.getTotalPotentialCapacity().getTotalSize().getUnit());
        assertEquals("Total capacity -total size value should be 100",
            Uint64.valueOf(100), link.getTotalPotentialCapacity().getTotalSize().getValue());
        if ("OTU4".equals(prefix)) {
            assertEquals("otn link should be between 2 nodes of protocol layers PHOTONIC_MEDIA",
                LayerProtocolName.PHOTONICMEDIA.getName(), link.getLayerProtocolName().get(0).getName());
        } else if ("ODTU4".equals(prefix)) {
            assertEquals("otn link should be between 2 nodes of protocol layers ODU",
                LayerProtocolName.ODU.getName(), link.getLayerProtocolName().get(0).getName());
        }
        assertEquals("transitional link should be BIDIRECTIONAL",
            ForwardingDirection.BIDIRECTIONAL, link.getDirection());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topoUuid, nodeEdgePointList.get(0).getTopologyUuid());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topoUuid, nodeEdgePointList.get(1).getTopologyUuid());
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
        assertEquals("operational state should be ENABLED",
            OperationalState.ENABLED, link.getOperationalState());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, link.getAdministrativeState());
    }
}
