/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
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
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.srg.storage.Storage;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.WavelengthDuplicationType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yangtools.yang.common.Uint16;

class SrgNetworkNodeTest {

    @Test
    void networkNodeId() {

        SupportingNode s1 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("ROADM-B"))
                .setNetworkRef(new NetworkId(StringConstants.OPENROADM_NETWORK))
                .build();
        SupportingNode s2 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("NodeB"))
                .setNetworkRef(NetworkId.getDefaultInstance(StringConstants.CLLI_NETWORK))
                .build();

        Map<SupportingNodeKey, SupportingNode> supportingNodes = new HashMap<>();
        supportingNodes.put(s1.key(), s1);
        supportingNodes.put(s2.key(), s2);

        NetworkNode srgNetworkNode = new SrgNetworkNode();
        Assert.assertEquals(
                NodeId.getDefaultInstance("ROADM-B"),
                srgNetworkNode.supportingNode(
                        new NetworkId(StringConstants.OPENROADM_NETWORK),
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

        SharedRiskGroup srg1 = new SharedRiskGroupBuilder()
                .setSrgNumber(Uint16.valueOf(1))
                .setWavelengthDuplication(WavelengthDuplicationType.OnePerDegree)
                .build();

        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        sharedRiskGroupMap.put(srg1.key(), srg1);

        SharedRiskGroup srg2 = new SharedRiskGroupBuilder()
                .setSrgNumber(Uint16.valueOf(2))
                .setWavelengthDuplication(WavelengthDuplicationType.OnePerDegree)
                .build();

        sharedRiskGroupMap.put(srg2.key(), srg2);

        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getNodeId()).thenReturn(NodeId.getDefaultInstance("ROADM-B-SRG1"));

        SupportingNode s1 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("ROADM-B"))
                .setNetworkRef(new NetworkId(StringConstants.OPENROADM_NETWORK))
                .build();
        SupportingNode s2 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("NodeB"))
                .setNetworkRef(NetworkId.getDefaultInstance(StringConstants.CLLI_NETWORK))
                .build();

        Map<SupportingNodeKey, SupportingNode> supportingNodes = new HashMap<>();
        supportingNodes.put(s1.key(), s1);
        supportingNodes.put(s2.key(), s2);

        Mockito.when(node.getSupportingNode()).thenReturn(supportingNodes);

        NetworkNode srgNetworkNode = new SrgNetworkNode();
        NetworkId networkId = new NetworkId(StringConstants.OPENROADM_NETWORK);

        Storage storage = Mockito.mock(Storage.class);
        Mockito.when(storage.read("ROADM-B")).thenReturn(sharedRiskGroupMap);
        Assert.assertTrue(srgNetworkNode.contentionLess(networkId, node, storage));

    }

    @Test
    void contentionLessFalse() {

        SharedRiskGroup srg1 = new SharedRiskGroupBuilder()
                .setSrgNumber(Uint16.valueOf(1))
                .setWavelengthDuplication(WavelengthDuplicationType.OnePerSrg)
                .build();

        Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap = new HashMap<>();
        sharedRiskGroupMap.put(srg1.key(), srg1);

        SharedRiskGroup srg2 = new SharedRiskGroupBuilder()
                .setSrgNumber(Uint16.valueOf(2))
                .setWavelengthDuplication(WavelengthDuplicationType.OnePerDegree)
                .build();

        sharedRiskGroupMap.put(srg2.key(), srg2);

        Node node = Mockito.mock(Node.class);
        Mockito.when(node.getNodeId()).thenReturn(NodeId.getDefaultInstance("ROADM-B-SRG1"));

        SupportingNode s1 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("ROADM-B"))
                .setNetworkRef(NetworkId.getDefaultInstance(StringConstants.OPENROADM_NETWORK))
                .build();
        SupportingNode s2 = new SupportingNodeBuilder()
                .setNodeRef(NodeId.getDefaultInstance("NodeB"))
                .setNetworkRef(NetworkId.getDefaultInstance(StringConstants.CLLI_NETWORK))
                .build();

        Map<SupportingNodeKey, SupportingNode> supportingNodes = new HashMap<>();
        supportingNodes.put(s1.key(), s1);
        supportingNodes.put(s2.key(), s2);

        Mockito.when(node.getSupportingNode()).thenReturn(supportingNodes);

        NetworkNode srgNetworkNode = new SrgNetworkNode();
        NetworkId networkId = NetworkId.getDefaultInstance(StringConstants.OPENROADM_NETWORK);

        Storage storage = Mockito.mock(Storage.class);
        Mockito.when(storage.read("ROADM-B")).thenReturn(sharedRiskGroupMap);
        Assert.assertFalse(srgNetworkNode.contentionLess(networkId, node, storage));

    }
}
