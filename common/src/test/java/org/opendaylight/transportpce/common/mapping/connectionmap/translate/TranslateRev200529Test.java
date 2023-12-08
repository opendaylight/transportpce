/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.translate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.ConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.connection.map.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.connection.map.Source;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.connection.map.SourceBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class TranslateRev200529Test {

    private final TranslateRev200529 translateRev200529 = new TranslateRev200529();

    @Test
    void toTransportPCETranslatesSourceAndDestination() {
        Source source = new SourceBuilder().setCircuitPackName("circuit-pack-1").setPortName("port-1").build();
        Destination destination = new DestinationBuilder()
            .setCircuitPackName("circuit-pack-2")
            .setPortName("port-2")
            .build();

        ConnectionMap connectionMap = new ConnectionMapBuilder()
            .setConnectionMapNumber(Uint32.ONE)
            .setSource(source)
            .setDestination(Map.of(destination.key(), destination))
            .build();

        Map<NodeConnectionMapKey, NodeConnectionMap> result = translateRev200529
            .toTransportPCE(Map.of(connectionMap.key(), connectionMap));

        assertEquals(1, result.size());
        NodeConnectionMap translated = result.get(new NodeConnectionMapKey(Uint32.ONE));
        assertEquals(Uint32.ONE, translated.getConnectionMapNumber());
        assertEquals("circuit-pack-1", translated.getSource().getCircuitPackName());
        assertEquals("port-1", translated.getSource().getPortName());
        assertEquals(1, translated.getDestination().size());
        var translatedDestination = translated.getDestination().values().iterator().next();
        assertEquals("circuit-pack-2", translatedDestination.getCircuitPackName());
        assertEquals("port-2", translatedDestination.getPortName());
    }

    @Test
    void toTransportPCEWithNullDestinationYieldsNullDestinationMap() {
        Source source = new SourceBuilder().setCircuitPackName("circuit-pack-1").setPortName("port-1").build();

        ConnectionMap connectionMap = new ConnectionMapBuilder()
            .setConnectionMapNumber(Uint32.ONE)
            .setSource(source)
            .build();

        Map<NodeConnectionMapKey, NodeConnectionMap> result = translateRev200529
            .toTransportPCE(Map.of(connectionMap.key(), connectionMap));

        NodeConnectionMap translated = result.get(new NodeConnectionMapKey(Uint32.ONE));
        assertNull(translated.getDestination());
    }

    @Test
    void toTransportPCEWithEmptyInputYieldsEmptyResult() {
        Map<ConnectionMapKey, ConnectionMap> connectionMap = Map.of();

        assertTrue(translateRev200529.toTransportPCE(connectionMap).isEmpty());
    }
}
