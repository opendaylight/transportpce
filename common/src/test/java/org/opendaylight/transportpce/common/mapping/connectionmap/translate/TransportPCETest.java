/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.translate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yangtools.yang.common.Uint32;

public class TransportPCETest {

    private Rev181019 rev181019;
    private Rev200529 rev200529;
    private TransportPCE transportPCE;

    @BeforeEach
    void setUp() {
        rev181019 = mock(Rev181019.class);
        rev200529 = mock(Rev200529.class);
        transportPCE = new TransportPCE(rev181019, rev200529);
    }

    @Test
    void toTransportPCErev181019DelegatesToRev181019Translator() {
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                .org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap = Map.of();

        Map<NodeConnectionMapKey, NodeConnectionMap> expected = Map.of(
            new NodeConnectionMapKey(Uint32.ONE),
            new NodeConnectionMapBuilder().setConnectionMapNumber(Uint32.ONE).build());

        when(rev181019.toTransportPCE(connectionMap)).thenReturn(expected);

        assertEquals(expected, transportPCE.toTransportPCErev181019(connectionMap));
        verify(rev181019).toTransportPCE(connectionMap);
        verifyNoInteractions(rev200529);
    }

    @Test
    void toTransportPCErev200529DelegatesToRev200529Translator() {
        Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                .org.openroadm.device.container.org.openroadm.device.ConnectionMapKey,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                .org.openroadm.device.container.org.openroadm.device.ConnectionMap> connectionMap = Map.of();

        Map<NodeConnectionMapKey, NodeConnectionMap> expected = Map.of(
            new NodeConnectionMapKey(Uint32.ONE),
            new NodeConnectionMapBuilder().setConnectionMapNumber(Uint32.ONE).build());

        when(rev200529.toTransportPCE(connectionMap)).thenReturn(expected);

        assertEquals(expected, transportPCE.toTransportPCErev200529(connectionMap));
        verify(rev200529).toTransportPCE(connectionMap);
        verifyNoInteractions(rev181019);
    }
}
