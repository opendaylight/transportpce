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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.DataStoreContextImpl;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.tapi.utils.TopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.get.topology.details.output.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class TapiTopologyImplTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(TapiTopologyImplTest.class);

    private static ListeningExecutorService executorService;
    private static CountDownLatch endSignal;
    private static final int NUM_THREADS = 3;
    private static DataStoreContext dataStoreContextUtil;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        dataStoreContextUtil = new DataStoreContextImpl();
        TopologyDataUtils.writeTopologyFromFileToDatastore(dataStoreContextUtil,
            TopologyDataUtils.OPENROADM_TOPOLOGY_FILE, InstanceIdentifiers.OVERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(dataStoreContextUtil,
            TopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(dataStoreContextUtil);
        LOG.info("setup done");
    }

    @Test
    public void getTopologyDetailsForOpenroadmTopologyWhenSuccessful() throws ExecutionException, InterruptedException {
        GetTopologyDetailsInput input = TopologyDataUtils.buildGetTopologyDetailsInput(NetworkUtils.OVERLAY_NETWORK_ID);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(dataStoreContextUtil.getDataBroker());
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
        assertEquals("Nodes list size should be 1", 1, topology.getNode().size());
        assertEquals("Node name should be TAPI Ethernet Node",
            "TAPI Ethernet Node", topology.getNode().get(0).getName().get(0).getValue());
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes("Ethernet Topology".getBytes()).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes("TAPI Ethernet Node".getBytes()).toString());
        assertEquals("incorrect topology uuid", topoUuid, topology.getUuid());
        assertEquals("incorrect node uuid", nodeUuid, topology.getNode().get(0).getUuid());
        Uuid onep1Uuid = new Uuid(UUID.nameUUIDFromBytes("OwnedNodeEdgePoint 0".getBytes()).toString());
        Uuid onep2Uuid = new Uuid(UUID.nameUUIDFromBytes("OwnedNodeEdgePoint 1".getBytes()).toString());
        assertEquals("incorrect uuid for nep1",
            onep1Uuid, topology.getNode().get(0).getOwnedNodeEdgePoint().get(0).getUuid());
        assertEquals("incorrect uuid for nep1",
            onep2Uuid, topology.getNode().get(0).getOwnedNodeEdgePoint().get(1).getUuid());
    }

    @Test
    public void getTopologyDetailsForOtnTopologyWhenSuccessful() throws ExecutionException, InterruptedException {
        GetTopologyDetailsInput input = TopologyDataUtils.buildGetTopologyDetailsInput(NetworkUtils.OTN_NETWORK_ID);
        TapiTopologyImpl tapiTopoImpl = new TapiTopologyImpl(dataStoreContextUtil.getDataBroker());
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
        assertEquals("Node list size should be 4", 4, topology.getNode().size());
        assertEquals("Link list size should be 5", 5, topology.getLink().size());
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes("T0 - Multi-layer topology".getBytes()).toString());
        assertEquals("incorrect topology uuid", topoUuid, topology.getUuid());
        assertEquals("topology name should be T0 - Multi-layer topology",
            "T0 - Multi-layer topology",
            topology.getName().get(0).getValue());

        List<Node> nodes = topology.getNode().values().stream()
            .sorted((n1,n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(nodes.get(0), node1Uuid, false);
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(nodes.get(1), node2Uuid, true);
        Uuid node3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(nodes.get(2), node3Uuid, false);
        Uuid node4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(nodes.get(3), node4Uuid, true);

        List<Link> links = topology.getLink().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(links.get(0), topoUuid, node1Uuid, node3Uuid, "DSR+XPDR1-NETWORK1",
            "iOTSi+XPDR1-NETWORK1");
        checkTransitionalLink(links.get(1), topoUuid, node2Uuid, node4Uuid, "DSR+XPDR2-NETWORK1",
            "iOTSi+XPDR2-NETWORK1");
    }

    private void checkDsrNode(Node node, Uuid nodeUuid, boolean isSwitch) {
        assertEquals("incorrect node uuid", nodeUuid, node.getUuid());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, node.getAdministrativeState());
        assertEquals("life-cycle state should be INSTALLED", LifecycleState.INSTALLED, node.getLifecycleState());
        assertEquals("operational state should be ENABLED", OperationalState.ENABLED, node.getOperationalState());
        assertEquals("value-name should be 'dsr/odu node name'",
             "dsr/odu node name", node.getName().get(0).getValueName());
        assertEquals("dsr node should manage 2 protocol layers : dsr and odu",
            2, node.getLayerProtocolName().size());
        assertThat("dsr node should manage 2 protocol layers : dsr and odu",
            node.getLayerProtocolName(), hasItems(LayerProtocolName.DSR, LayerProtocolName.ODU));
        List<OwnedNodeEdgePoint> neps = node.getOwnedNodeEdgePoint().values().stream()
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        if (isSwitch) {
            assertEquals("Switch-DSR node should have 8 NEPs", 8, neps.size());
            OwnedNodeEdgePoint nep1 = neps.get(5);
            Uuid client4NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes("DSR+XPDR2-CLIENT4".getBytes(Charset.forName("UTF-8"))).toString());
            checkNepClient100G(nep1, client4NepUuid, "XPDR2-CLIENT4", "NodeEdgePoint_C4");
            OwnedNodeEdgePoint nep2 = neps.get(1);
            Uuid networkNepUuid = new Uuid(
                    UUID.nameUUIDFromBytes("DSR+XPDR2-NETWORK1".getBytes(Charset.forName("UTF-8"))).toString());
            checkNepNetworkODU4(nep2, networkNepUuid, "XPDR2-NETWORK1", "NodeEdgePoint_N1");
            List<NodeRuleGroup> nrgList = node.getNodeRuleGroup().values().stream()
                .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                .collect(Collectors.toList());
            checkNodeRuleGroupForSwitchDSR(nrgList, client4NepUuid, networkNepUuid, nodeUuid);
        } else {
            assertEquals("Mux-DSR node should have 5 NEPs", 5, neps.size());
            OwnedNodeEdgePoint nep1 = neps.get(0);
            Uuid client4NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes("DSR+XPDR1-CLIENT4".getBytes(Charset.forName("UTF-8"))).toString());
            checkNepClient10G(nep1, client4NepUuid, "XPDR1-CLIENT4", "NodeEdgePoint_C4");

            OwnedNodeEdgePoint nep2 = neps.get(1);
            Uuid networkNepUuid = new Uuid(
                    UUID.nameUUIDFromBytes("DSR+XPDR1-NETWORK1".getBytes(Charset.forName("UTF-8"))).toString());
            checkNepNetworkODU4(nep2, networkNepUuid, "XPDR1-NETWORK1", "NodeEdgePoint_N1");
            List<NodeRuleGroup> nrgList = node.getNodeRuleGroup().values().stream()
                .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                .collect(Collectors.toList());
            checkNodeRuleGroupForMuxDSR(nrgList, client4NepUuid, networkNepUuid, nodeUuid);
        }
    }

    private void checkOtsiNode(Node node, Uuid nodeUuid, boolean isSwitch) {
        assertEquals("incorrect node uuid", nodeUuid, node.getUuid());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, node.getAdministrativeState());
        assertEquals("life-cycle state should be INSTALLED", LifecycleState.INSTALLED, node.getLifecycleState());
        assertEquals("operational state should be ENABLED", OperationalState.ENABLED, node.getOperationalState());
        assertEquals("value-name should be 'dsr/odu node name'",
             "otsi node name", node.getName().get(0).getValueName());
        assertEquals("otsi node should manage a single protocol layer : PHOTONIC_MEDIA",
            1, node.getLayerProtocolName().size());
        assertEquals("otsi node should manage a single protocol layer : PHOTONIC_MEDIA",
            LayerProtocolName.PHOTONICMEDIA, node.getLayerProtocolName().get(0));
        List<OwnedNodeEdgePoint> neps = node.getOwnedNodeEdgePoint().values().stream()
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        if (isSwitch) {
            assertEquals("Switch-OTSi node should have 8 NEPs", 8, neps.size());
            OwnedNodeEdgePoint nep1 = neps.get(0);
            Uuid inepUuid = new Uuid(
                    UUID.nameUUIDFromBytes("iOTSi+XPDR2-NETWORK2".getBytes(Charset.forName("UTF-8"))).toString());
            checkNepOtsiNode(nep1, inepUuid, "XPDR2-NETWORK2", "iNodeEdgePoint_2");
            OwnedNodeEdgePoint nep2 = neps.get(5);
            Uuid enepUuid = new Uuid(
                    UUID.nameUUIDFromBytes("eOTSi+XPDR2-NETWORK2".getBytes(Charset.forName("UTF-8"))).toString());
            checkNepOtsiNode(nep2, enepUuid, "XPDR2-NETWORK2", "eNodeEdgePoint_2");
            List<NodeRuleGroup> nrgList = node.getNodeRuleGroup().values().stream()
                .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                .collect(Collectors.toList());
            checkNodeRuleGroupForSwitchOTSi(nrgList, enepUuid, inepUuid, nodeUuid);
        } else {
            assertEquals("Mux-OTSi node should have 2 NEPs", 2, neps.size());
            OwnedNodeEdgePoint nep1 = neps.get(0);
            Uuid enepUuid = new Uuid(
                    UUID.nameUUIDFromBytes("eOTSi+XPDR1-NETWORK1".getBytes(Charset.forName("UTF-8"))).toString());
            checkNepOtsiNode(nep1, enepUuid, "XPDR1-NETWORK1", "eNodeEdgePoint_1");
            OwnedNodeEdgePoint nep2 = neps.get(1);
            Uuid inepUuid = new Uuid(
                    UUID.nameUUIDFromBytes("iOTSi+XPDR1-NETWORK1".getBytes(Charset.forName("UTF-8"))).toString());
            checkNepOtsiNode(nep2, inepUuid, "XPDR1-NETWORK1", "iNodeEdgePoint_1");
            List<NodeRuleGroup> nrgList = node.getNodeRuleGroup().values().stream()
                .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                .collect(Collectors.toList());
            checkNodeRuleGroupForMuxOTSi(nrgList, enepUuid, inepUuid, nodeUuid);
        }
    }

    private void checkNepClient10G(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        assertEquals("value of client nep should be '" + portName + "'",
            portName, nep.getName().get(0).getValue());
        assertEquals("value-name of client nep for '" + portName + "' should be '" + nepName + "'",
            nepName, nep.getName().get(0).getValueName());
        assertEquals("Client nep should support 2 kind of cep",
            2, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("client nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(ODUTYPEODU2E.class, DIGITALSIGNALTYPE10GigELAN.class));
        assertEquals("client nep should be of ETH protocol type", LayerProtocolName.ETH, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep);
    }

    private void checkNepNetworkODU4(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        assertEquals("value of network nep should be '" + portName + "'",
            portName, nep.getName().get(0).getValue());
        assertEquals("value-name of client nep for '" + portName + "' should be '" + nepName + "'",
            nepName, nep.getName().get(0).getValueName());
        assertEquals("Network nep should support 1 kind of cep",
            1, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("network nep should support 1 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItem(ODUTYPEODU4.class));
        assertEquals("network nep should be of ODU protocol type", LayerProtocolName.ODU, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep);
    }

    private void checkNodeRuleGroupForMuxDSR(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid,
        Uuid nodeUuid) {
        assertEquals("muxponder DSR should contain 4 node rule group", 4, nrgList.size());
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals("each node-rule-group should contain 2 NEP for muxponder DSR",
                2, nodeRuleGroup.getNodeEdgePoint().size());
        }
        assertThat("node-rule-group nb 2 should be between nep-client4 and nep-network1",
            nrgList.get(1).getNodeEdgePoint().get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertThat("node-rule-group nb 2 should be between nep-client4 and nep-network1",
            nrgList.get(1).getNodeEdgePoint().get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertEquals("node-rule-group nb 2 should be between nep-client4 and nep-network1 of the same node",
            nrgList.get(1).getNodeEdgePoint().get(0).getNodeUuid(), nodeUuid);
        assertEquals("node-rule-group nb 2 should be between nep-client4 and nep-network1 of the same node",
            nrgList.get(1).getNodeEdgePoint().get(1).getNodeUuid(), nodeUuid);
        assertEquals("node-rule-group nb 2 should contain a single rule", 1, nrgList.get(1).getRule().size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", nrgList.get(1).getRule().get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, nrgList.get(1).getRule().get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, nrgList.get(1).getRule().get(0).getRuleType());
    }

    private void checkNodeRuleGroupForSwitchDSR(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid,
        Uuid nodeUuid) {
        assertEquals("Switch-DSR should contain a single node rule group", 1, nrgList.size());
        assertEquals("Switch-DSR node-rule-group should contain 8 NEP", 8, nrgList.get(0).getNodeEdgePoint().size());
        List<NodeEdgePoint> nrg = nrgList.get(0).getNodeEdgePoint().values().stream()
            .sorted((nrg1, nrg2) -> nrg1.getNodeEdgePointUuid().getValue()
                .compareTo(nrg2.getNodeEdgePointUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals("in the sorted node-rule-group, nep number 2 should be XPDR2-NETWORK1",
            networkNepUuid, nrg.get(1).getNodeEdgePointUuid());
        assertEquals("in the sorted node-rule-group, nep number 6 should be XPDR2-CLIENT4",
            clientNepUuid, nrg.get(5).getNodeEdgePointUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nrg.get(1).getNodeUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nrg.get(5).getNodeUuid());
        assertEquals("node-rule-group should contain a single rule", 1, nrgList.get(0).getRule().size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", nrgList.get(0).getRule().get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, nrgList.get(0).getRule().get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, nrgList.get(0).getRule().get(0).getRuleType());
    }

    private void checkNodeRuleGroupForMuxOTSi(List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid,
        Uuid nodeUuid) {
        assertEquals("Mux-OTSi should contain a single node rule group", 1, nrgList.size());
        assertEquals("Mux-OTSi node-rule-group should contain 2 NEP", 2, nrgList.get(0).getNodeEdgePoint().size());
        assertThat("Mux-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
            nrgList.get(0).getNodeEdgePoint().get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat("Mux-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
            nrgList.get(0).getNodeEdgePoint().get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nrgList.get(0).getNodeEdgePoint().get(0).getNodeUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nrgList.get(0).getNodeEdgePoint().get(1).getNodeUuid());
        assertEquals("node-rule-group should contain a single rule", 1, nrgList.get(0).getRule().size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", nrgList.get(0).getRule().get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, nrgList.get(0).getRule().get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, nrgList.get(0).getRule().get(0).getRuleType());
    }

    private void checkNodeRuleGroupForSwitchOTSi(List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid,
        Uuid nodeUuid) {
        assertEquals("Switch-OTSi should contain 4 node rule group", 4, nrgList.size());
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals("each node-rule-group should contain 2 NEP for Switch-OTSi",
                2, nodeRuleGroup.getNodeEdgePoint().size());
        }
        assertThat("Switch-OTSi node-rule-group nb 4 should be between eNEP and iNEP of XPDR2-NETWORK2",
            nrgList.get(1).getNodeEdgePoint().get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat("Switch-OTSi node-rule-group nb 4 should be between eNEP and iNEP of XPDR2-NETWORK2",
            nrgList.get(1).getNodeEdgePoint().get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nrgList.get(0).getNodeEdgePoint().get(0).getNodeUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nrgList.get(0).getNodeEdgePoint().get(1).getNodeUuid());
        assertEquals("node-rule-group should contain a single rule", 1, nrgList.get(0).getRule().size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", nrgList.get(0).getRule().get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, nrgList.get(0).getRule().get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, nrgList.get(0).getRule().get(0).getRuleType());
    }

    private void checkNepClient100G(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        assertEquals("value of client nep should be '" + portName + "'",
            portName, nep.getName().get(0).getValue());
        assertEquals("value-name of client nep for '" + portName + "' should be '" + nepName + "'",
            nepName, nep.getName().get(0).getValueName());
        assertEquals("Client nep should support 2 kind of cep",
            2, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("client nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(ODUTYPEODU4.class, DIGITALSIGNALTYPE100GigE.class));
        assertEquals("client nep should be of ETH protocol type", LayerProtocolName.ETH, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep);
    }

    private void checkNepOtsiNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        assertEquals("value of OTSi nep should be '" + portName + "'",
            portName, nep.getName().get(0).getValue());
        assertEquals("value-name of OTSi nep should be '" + nepName + "'",
            nepName, nep.getName().get(0).getValueName());
        assertEquals("OTSi nep should support 2 kind of cep",
            2, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("OTSi nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(PHOTONICLAYERQUALIFIEROMS.class, PHOTONICLAYERQUALIFIEROTSi.class));
        assertEquals("OTSi nep should be of PHOTONIC_MEDIA protocol type",
            LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName());
        assertEquals("OTSi nep should support one SIP", 1, nep.getMappedServiceInterfacePoint().size());
        checkCommonPartOfNep(nep);
    }

    private void checkCommonPartOfNep(OwnedNodeEdgePoint nep) {
        assertEquals("link port direction should be DIRECTIONAL",
            PortDirection.BIDIRECTIONAL, nep.getLinkPortDirection());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, nep.getAdministrativeState());
        assertEquals("termination state should be TERMINATED BIDIRECTIONAL",
            TerminationState.TERMINATEDBIDIRECTIONAL, nep.getTerminationState());
        assertEquals("life-cycle state should be INSTALLED", LifecycleState.INSTALLED, nep.getLifecycleState());
        assertEquals("client nep should support 1 SIP", 1, nep.getMappedServiceInterfacePoint().size());
        assertEquals("termination direction should be BIDIRECTIONAL",
            TerminationDirection.BIDIRECTIONAL, nep.getTerminationDirection());
        assertEquals("operational state of client nep should be ENABLED",
            OperationalState.ENABLED, nep.getOperationalState());
        assertEquals("link-port-role of client nep should be SYMMETRIC",
            PortRole.SYMMETRIC, nep.getLinkPortRole());
    }

    private void checkTransitionalLink(Link link, Uuid topoUuid, Uuid node1Uuid, Uuid node2Uuid, String tp1,
        String tp2) {
        Uuid linkUuid = new Uuid(UUID.nameUUIDFromBytes((tp1 + "--" + tp2).getBytes(Charset.forName("UTF-8")))
            .toString());
        assertEquals("bad uuid for link between DSR node " + tp1 + " and iOTSI port " + tp2, linkUuid, link.getUuid());
        assertEquals("Available capacity unit should be GBPS",
            CapacityUnit.GBPS, link.getAvailableCapacity().getTotalSize().getUnit());
        assertEquals("Available capacity -total size value should be 100",
            Uint64.valueOf(100), link.getAvailableCapacity().getTotalSize().getValue());
        assertEquals("transitional link should be between 2 nodes of protocol layers ODU and PHOTONIC_MEDIA",
            2, link.getTransitionedLayerProtocolName().size());
        assertThat("transitional link should be between 2 nodes of protocol layers ODU and PHOTONIC_MEDIA",
            link.getTransitionedLayerProtocolName(),
            hasItems(LayerProtocolName.ODU.getName(), LayerProtocolName.PHOTONICMEDIA.getName()));
        assertEquals("transitional link should be BIDIRECTIONAL",
            ForwardingDirection.BIDIRECTIONAL, link.getDirection());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topoUuid, link.getNodeEdgePoint().get(0).getTopologyUuid());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topoUuid, link.getNodeEdgePoint().get(1).getTopologyUuid());
        assertThat("transitional links should terminate on DSR node and Photonic node",
            link.getNodeEdgePoint().get(0).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        assertThat("transitional links should terminate on DSR node and Photonic node",
            link.getNodeEdgePoint().get(1).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        Uuid nep1Uuid = new Uuid(UUID.nameUUIDFromBytes(tp1.getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nep2Uuid = new Uuid(UUID.nameUUIDFromBytes(tp2.getBytes(Charset.forName("UTF-8"))).toString());
        assertThat("transitional links should terminate on " + tp1 + " and " + tp2 + " neps",
            link.getNodeEdgePoint().get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(nep1Uuid.getValue())).or(containsString(nep2Uuid.getValue())));
        assertThat("transitional links should terminate on DSR node and Photonic node",
            link.getNodeEdgePoint().get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(nep1Uuid.getValue())).or(containsString(nep2Uuid.getValue())));
    }
}
