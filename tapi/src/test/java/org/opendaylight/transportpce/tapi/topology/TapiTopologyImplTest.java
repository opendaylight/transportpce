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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
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
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.TPDR_100G.getBytes(Charset.forName("UTF-8")))
            .toString());
        assertEquals("incorrect topology uuid", topoUuid, topology.getUuid());
        assertEquals("Node list size should be 1", 1, topology.getNode().size());
        Name nodeName = topology.getNode().values().stream().findFirst().get().getName()
            .get(new NameKey("Tpdr100g node name"));
        assertEquals("Node name should be 'Tpdr100g over WDM node'", "Tpdr100g over WDM node", nodeName.getValue());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(nodeName.getValue().getBytes(Charset.forName("UTF-8")))
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

        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+DSR+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid link1Uuid =
            new Uuid(UUID.nameUUIDFromBytes("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid link2Uuid =
            new Uuid(UUID.nameUUIDFromBytes("OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());

        List<Link> links = topology.nonnullLink().values().stream()
            .filter(l -> l.getName().containsKey(new NameKey("otn link name")))
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkOtnLink(links.get(0), topoUuid, node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, link1Uuid,
            "ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
        checkOtnLink(links.get(1), topoUuid, node3Uuid, node4Uuid, tp3Uuid, tp4Uuid, link2Uuid,
            "OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
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
