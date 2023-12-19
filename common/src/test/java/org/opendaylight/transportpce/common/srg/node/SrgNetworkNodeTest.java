/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.node;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.srg.storage.Storage;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.Srg;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.srg.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.srg.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

class SrgNetworkNodeTest {

    @Test
    void networkNodeId() {

        SupportingNode s1 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("ROADM-B"))
                .setNetworkRef(NetworkId.getDefaultInstance(NetworkUtils.UNDERLAY_NETWORK_ID))
                .build();
        SupportingNode s2 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("NodeB"))
                .setNetworkRef(NetworkId.getDefaultInstance(NetworkUtils.CLLI_NETWORK_ID))
                .build();

        Map<SupportingNodeKey, SupportingNode> supportingNodes = new HashMap<>();
        supportingNodes.put(s1.key(), s1);
        supportingNodes.put(s2.key(), s2);

        NetworkNode srgNetworkNode = new SrgNetworkNode();
        Assert.assertEquals(
                NodeId.getDefaultInstance("ROADM-B"),
                srgNetworkNode.supportingNode(
                        NetworkId.getDefaultInstance(NetworkUtils.UNDERLAY_NETWORK_ID),
                        supportingNodes
                )
        );

    }

    @Test
    void number() {
        NetworkNode srgNetworkNode = new SrgNetworkNode();
        Assert.assertEquals(13, srgNetworkNode.number("ROADM-B-SRG13"));
    }

    @Test
    void contentionLessTrue() {

        CircuitPacksBuilder circuitPacksBuilder = new CircuitPacksBuilder();

        CircuitPacks circuitPacks = circuitPacksBuilder
                .setIndex(Uint32.valueOf(1))
                .setCircuitPackName("1/XC3")
                .build();

        SharedRiskGroup srg1 = new SharedRiskGroupBuilder()
                .setMaxAddDropPorts(Uint16.valueOf(16))
                .setSrgNumber(Uint16.valueOf(1))
                .setCircuitPacks(Map.of(circuitPacks.key(), circuitPacks))
                .setWavelengthDuplication(Srg.WavelengthDuplication.OnePerDegree)
                .build();

        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        sharedRiskGroupMap.put(srg1.key(), srg1);

        SharedRiskGroup srg2 = new SharedRiskGroupBuilder()
                .setMaxAddDropPorts(Uint16.valueOf(16))
                .setSrgNumber(Uint16.valueOf(2))
                .setCircuitPacks(Map.of(circuitPacks.key(), circuitPacks))
                .setWavelengthDuplication(Srg.WavelengthDuplication.OnePerDegree)
                .build();

        sharedRiskGroupMap.put(srg2.key(), srg2);

        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getNodeId()).thenReturn(NodeId.getDefaultInstance("ROADM-B-SRG1"));

        SupportingNode s1 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("ROADM-B"))
                .setNetworkRef(NetworkId.getDefaultInstance(NetworkUtils.UNDERLAY_NETWORK_ID))
                .build();
        SupportingNode s2 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("NodeB"))
                .setNetworkRef(NetworkId.getDefaultInstance(NetworkUtils.CLLI_NETWORK_ID))
                .build();

        Map<SupportingNodeKey, SupportingNode> supportingNodes = new HashMap<>();
        supportingNodes.put(s1.key(), s1);
        supportingNodes.put(s2.key(), s2);

        Mockito.when(node.getSupportingNode()).thenReturn(supportingNodes);

        NetworkNode srgNetworkNode = new SrgNetworkNode();
        NetworkId networkId = NetworkId.getDefaultInstance(NetworkUtils.UNDERLAY_NETWORK_ID);

        Storage storage = Mockito.mock(Storage.class);
        Mockito.when(storage.read("ROADM-B")).thenReturn(sharedRiskGroupMap);
        Assert.assertTrue(srgNetworkNode.contentionLess(networkId, node, storage));

    }

    @Test
    void contentionLessFalse() {

        CircuitPacksBuilder circuitPacksBuilder = new CircuitPacksBuilder();

        CircuitPacks circuitPacks = circuitPacksBuilder
                .setIndex(Uint32.valueOf(1))
                .setCircuitPackName("1/XC3")
                .build();

        SharedRiskGroup srg1 = new SharedRiskGroupBuilder()
                .setMaxAddDropPorts(Uint16.valueOf(16))
                .setSrgNumber(Uint16.valueOf(1))
                .setCircuitPacks(Map.of(circuitPacks.key(), circuitPacks))
                .setWavelengthDuplication(Srg.WavelengthDuplication.OnePerSRG)
                .build();

        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        sharedRiskGroupMap.put(srg1.key(), srg1);

        SharedRiskGroup srg2 = new SharedRiskGroupBuilder()
                .setMaxAddDropPorts(Uint16.valueOf(16))
                .setSrgNumber(Uint16.valueOf(2))
                .setCircuitPacks(Map.of(circuitPacks.key(), circuitPacks))
                .setWavelengthDuplication(Srg.WavelengthDuplication.OnePerDegree)
                .build();

        sharedRiskGroupMap.put(srg2.key(), srg2);

        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getNodeId()).thenReturn(NodeId.getDefaultInstance("ROADM-B-SRG1"));

        SupportingNode s1 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("ROADM-B"))
                .setNetworkRef(NetworkId.getDefaultInstance(NetworkUtils.UNDERLAY_NETWORK_ID))
                .build();
        SupportingNode s2 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("NodeB"))
                .setNetworkRef(NetworkId.getDefaultInstance(NetworkUtils.CLLI_NETWORK_ID))
                .build();

        Map<SupportingNodeKey, SupportingNode> supportingNodes = new HashMap<>();
        supportingNodes.put(s1.key(), s1);
        supportingNodes.put(s2.key(), s2);

        Mockito.when(node.getSupportingNode()).thenReturn(supportingNodes);

        NetworkNode srgNetworkNode = new SrgNetworkNode();
        NetworkId networkId = NetworkId.getDefaultInstance(NetworkUtils.UNDERLAY_NETWORK_ID);

        Storage storage = Mockito.mock(Storage.class);
        Mockito.when(storage.read("ROADM-B")).thenReturn(sharedRiskGroupMap);
        Assert.assertFalse(srgNetworkNode.contentionLess(networkId, node, storage));

    }
}