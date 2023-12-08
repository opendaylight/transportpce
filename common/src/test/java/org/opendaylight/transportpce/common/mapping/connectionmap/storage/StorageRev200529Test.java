/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.Translate;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.OpenroadmConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;

public class StorageRev200529Test {

    private DataBroker dataBroker;
    private Translate translate;
    private StorageRev200529 storageRev200529;

    @BeforeEach
    void setUp() {
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        dataBroker = dataStoreContext.getDataBroker();
        translate = mock(Translate.class);
        storageRev200529 = new StorageRev200529();
    }

    @Test
    void saveTranslatesAndPersistsConnectionMap() throws Exception {
        ConnectionMap connectionMap = new ConnectionMapBuilder().setConnectionMapNumber(Uint32.ONE).build();
        Map<ConnectionMapKey, ConnectionMap> input = Map.of(connectionMap.key(), connectionMap);

        var destination = new DestinationBuilder()
            .setCircuitPackName("circuit-pack-2")
            .setPortName("port-2")
            .build();
        NodeConnectionMap translated = new NodeConnectionMapBuilder()
            .setConnectionMapNumber(Uint32.ONE)
            .setSource(new SourceBuilder().setCircuitPackName("circuit-pack-1").setPortName("port-1").build())
            .setDestination(Map.of(destination.key(), destination))
            .build();
        Map<NodeConnectionMapKey, NodeConnectionMap> expected = Map.of(translated.key(), translated);

        when(translate.toTransportPCErev200529(input)).thenReturn(expected);

        assertTrue(storageRev200529.save(dataBroker, translate, "node1", input));

        NetworkNodes storedNode = readNode("node1");
        assertEquals("node1", storedNode.getNodeId());
        assertEquals(expected, storedNode.getNodeConnectionMap());
    }

    @Test
    void saveWithNullConnectionMapDoesNotInvokeTranslatorAndPersistsEmptyMap() throws Exception {
        assertTrue(storageRev200529.save(dataBroker, translate, "node2", null));

        verifyNoInteractions(translate);
        assertNull(readNode("node2").getNodeConnectionMap());
    }

    @Test
    void saveWithEmptyConnectionMapDoesNotInvokeTranslator() throws Exception {
        assertTrue(storageRev200529.save(dataBroker, translate, "node3", Map.of()));

        verifyNoInteractions(translate);
        assertNull(readNode("node3").getNodeConnectionMap());
    }

    private NetworkNodes readNode(String nodeId) throws Exception {
        DataObjectIdentifier<NetworkNodes> nodesIID = DataObjectIdentifier
            .builder(OpenroadmConnectionMap.class)
            .child(NetworkNodes.class, new NetworkNodesKey(nodeId))
            .build();

        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            return readTransaction.read(LogicalDatastoreType.OPERATIONAL, nodesIID).get().orElseThrow();
        }
    }
}
