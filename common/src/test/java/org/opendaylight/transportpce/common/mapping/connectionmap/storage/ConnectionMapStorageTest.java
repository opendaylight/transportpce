/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.Translate;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.transportpce.test.DataStoreContextImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.OpenroadmConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodesKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class ConnectionMapStorageTest {

    private DataBroker dataBroker;
    private Translate translate;
    private Rev181019 rev181019;
    private Rev200529 rev200529;
    private ConnectionMapStorage connectionMapStorage;

    @BeforeEach
    void setUp() {
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        dataBroker = dataStoreContext.getDataBroker();
        translate = mock(Translate.class);
        rev181019 = mock(Rev181019.class);
        rev200529 = mock(Rev200529.class);
        connectionMapStorage = new ConnectionMapStorage(dataBroker, translate, rev181019, rev200529);
    }

    @Test
    void saveRev181019DelegatesToRev181019Storage() {
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap = Map.of();

        when(rev181019.save(dataBroker, translate, "node1", connectionMap)).thenReturn(true);

        assertTrue(connectionMapStorage.saveRev181019("node1", connectionMap));
        verify(rev181019).save(dataBroker, translate, "node1", connectionMap);
        verifyNoInteractions(rev200529);
    }

    @Test
    void saveRev200529DelegatesToRev200529Storage() {
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                .org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                .org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap = Map.of();

        when(rev200529.save(dataBroker, translate, "node1", connectionMap)).thenReturn(true);

        assertTrue(connectionMapStorage.saveRev200529("node1", connectionMap));
        verify(rev200529).save(dataBroker, translate, "node1", connectionMap);
        verifyNoInteractions(rev181019);
    }

    @Test
    void readReturnsStoredNode() throws Exception {
        writeNode("node1");

        Map<NetworkNodesKey, NetworkNodes> result = connectionMapStorage.read("node1");

        assertEquals(1, result.size());
        assertEquals("node1", result.get(new NetworkNodesKey("node1")).getNodeId());
    }

    @Test
    void readReturnsEmptyMapWhenNodeIsMissing() {
        assertTrue(connectionMapStorage.read("missing-node").isEmpty());
    }

    private void writeNode(String nodeId) throws Exception {
        NetworkNodes networkNodes = new NetworkNodesBuilder().setNodeId(nodeId).build();
        DataObjectIdentifier<NetworkNodes> nodesIID = DataObjectIdentifier
            .builder(OpenroadmConnectionMap.class)
            .child(NetworkNodes.class, new NetworkNodesKey(nodeId))
            .build();

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, nodesIID, networkNodes);
        writeTransaction.commit().get();
    }
}
