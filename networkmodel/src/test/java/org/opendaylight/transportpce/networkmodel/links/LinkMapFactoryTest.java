/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.NodeConnectionMapKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.Destination;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.DestinationBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.DestinationKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.Source;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.node.connection.map.group.node.connection.map.SourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819.openroadm.connection.map.NetworkNodesBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;

class LinkMapFactoryTest {

    Logger logger;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
    }

    @Test
    @DisplayName("circuitConnections keeps destinations scoped to their own source")
    void circuitConnectionsKeepsDestinationsScopedToTheirOwnSource() {
        // Regression test: two separate wires (node-connection-map entries) on the same
        // node must not leak destinations into each other.
        NodeConnectionMap wire1 = nodeConnectionMap(1, "1/0", "2/0");
        NodeConnectionMap wire2 = nodeConnectionMap(2, "3/0", "4/0");
        NetworkNodes node = networkNodes("ROADM-B", wire1, wire2);

        Storage storage = mock(Storage.class);
        when(storage.read("ROADM-B")).thenReturn(Map.of(node.key(), node));

        Factory linkMapFactory = new LinkMapFactory(logger);
        Map<String, Set<String>> result = linkMapFactory.circuitConnections(storage, "ROADM-B");

        assertEquals(
            Map.of(
                "1/0", Set.of("2/0"),
                "3/0", Set.of("4/0")),
            result
        );
    }

    @Test
    @DisplayName("circuitConnections supports a source with multiple destinations")
    void circuitConnectionsSupportsFanOutDestinations() {
        // A single wire can legitimately report more than one destination (e.g. a
        // degree splitting to multiple SRG add/drop ports).
        NodeConnectionMap wire = nodeConnectionMap(1, "1/0", "2/0", "3/0");
        NetworkNodes node = networkNodes("ROADM-B", wire);

        Storage storage = mock(Storage.class);
        when(storage.read("ROADM-B")).thenReturn(Map.of(node.key(), node));

        Factory linkMapFactory = new LinkMapFactory(logger);
        Map<String, Set<String>> result = linkMapFactory.circuitConnections(storage, "ROADM-B");

        assertEquals(Map.of("1/0", Set.of("2/0", "3/0")), result);
    }

    @Test
    @DisplayName("circuitConnections returns an empty map when storage has no entry for the node")
    void circuitConnectionsReturnsEmptyMapWhenStorageHasNoEntryForNode() {
        Storage storage = mock(Storage.class);
        when(storage.read("ROADM-B")).thenReturn(Map.of());

        Factory linkMapFactory = new LinkMapFactory(logger);
        Map<String, Set<String>> result = linkMapFactory.circuitConnections(storage, "ROADM-B");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("connections translates circuit pack connections into node-level links")
    void connections() {
        Map<String, String> translation = new HashMap<>();
        translation.put("1/WSS", "ROADM-B-DEG1");
        translation.put("1/XC2", "ROADM-B-SRG3");
        translation.put("2/AWG", "ROADM-B-SRG10");
        translation.put("1/XC3", "ROADM-B-SRG4");
        translation.put("1/AWG", "ROADM-B-SRG1");
        translation.put("2/XC3", "ROADM-B-SRG13");
        translation.put("2/WSS", "ROADM-B-DEG2");
        translation.put("2/XC2", "ROADM-B-SRG12");

        Map<String, Set<String>> connections = new HashMap<>();
        connections.put("1/WSS", Set.of("1/XC2", "1/XC3", "1/AWG", "2/WSS"));
        connections.put("1/XC2", Set.of("1/WSS"));
        connections.put("2/AWG", Set.of("2/WSS"));
        connections.put("1/XC3", Set.of("1/WSS"));
        connections.put("1/AWG", Set.of("1/WSS"));
        connections.put("2/XC3", Set.of("2/WSS"));
        connections.put("2/XC2", Set.of("2/WSS"));
        connections.put("2/WSS", Set.of("1/WSS", "2/AWG", "2/XC3", "2/XC2"));

        Set<Link> expected = new HashSet<>();
        expected.add(new InterfaceLink("ROADM-B-SRG10", "ROADM-B-DEG2"));
        expected.add(new InterfaceLink("ROADM-B-DEG1",  "ROADM-B-SRG4"));
        expected.add(new InterfaceLink("ROADM-B-DEG2",  "ROADM-B-SRG13"));
        expected.add(new InterfaceLink("ROADM-B-DEG1",  "ROADM-B-SRG3"));
        expected.add(new InterfaceLink("ROADM-B-SRG13", "ROADM-B-DEG2"));
        expected.add(new InterfaceLink("ROADM-B-DEG1",  "ROADM-B-SRG1"));
        expected.add(new InterfaceLink("ROADM-B-DEG2",  "ROADM-B-SRG10"));
        expected.add(new InterfaceLink("ROADM-B-DEG2",  "ROADM-B-SRG12"));
        expected.add(new InterfaceLink("ROADM-B-SRG4",  "ROADM-B-DEG1"));
        expected.add(new InterfaceLink("ROADM-B-DEG1",  "ROADM-B-DEG2"));
        expected.add(new InterfaceLink("ROADM-B-SRG3",  "ROADM-B-DEG1"));
        expected.add(new InterfaceLink("ROADM-B-DEG2",  "ROADM-B-DEG1"));
        expected.add(new InterfaceLink("ROADM-B-SRG1",  "ROADM-B-DEG1"));
        expected.add(new InterfaceLink("ROADM-B-SRG12", "ROADM-B-DEG2"));

        Factory degreeFactory = new LinkMapFactory(logger);

        Set<Link> result = degreeFactory.connections(connections, translation);

        assertEquals(result, expected);
    }

    @Test
    @DisplayName("connections renders a non-empty, human-readable string")
    void connectionsAsString() {
        Map<String, String> translation = new HashMap<>();
        translation.put("1/WSS", "ROADM-B-DEG1");
        translation.put("1/XC2", "ROADM-B-SRG3");
        translation.put("2/AWG", "ROADM-B-SRG10");
        translation.put("1/XC3", "ROADM-B-SRG4");
        translation.put("1/AWG", "ROADM-B-SRG1");
        translation.put("2/XC3", "ROADM-B-SRG13");
        translation.put("2/WSS", "ROADM-B-DEG2");
        translation.put("2/XC2", "ROADM-B-SRG12");

        Map<String, Set<String>> connections = new HashMap<>();
        connections.put("1/WSS", Set.of("1/XC2", "1/XC3", "1/AWG", "2/WSS"));
        connections.put("1/XC2", Set.of("1/WSS"));
        connections.put("2/AWG", Set.of("2/WSS"));
        connections.put("1/XC3", Set.of("1/WSS"));
        connections.put("1/AWG", Set.of("1/WSS"));
        connections.put("2/XC3", Set.of("2/WSS"));
        connections.put("2/XC2", Set.of("2/WSS"));
        connections.put("2/WSS", Set.of("1/WSS", "2/AWG", "2/XC3", "2/XC2"));


        Factory linkMapFactory = new LinkMapFactory(logger);

        Set<Link> result = linkMapFactory.connections(connections, translation);

        String str = linkMapFactory.connections(result);
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    @DisplayName("circuitConnections keeps two nodes' connection maps independent")
    void connectionsForTwoNodes() {
        NetworkNodes roadmA01 = mockRoadmA01();
        NetworkNodes roadmB01 = mockRoadmB01();

        Storage storage = mock(Storage.class);
        when(storage.read("ROADMA01")).thenReturn(Map.of(roadmA01.key(), roadmA01));
        when(storage.read("ROADMB01")).thenReturn(Map.of(roadmB01.key(), roadmB01));

        Factory degreeFactory = new LinkMapFactory(logger);

        Map<String, Set<String>> circuitConnectionsA01 = degreeFactory.circuitConnections(storage, "ROADMA01");

        Map<String, Set<String>> expectedA01 = Map.of(
                "1/0", Set.of("2/0", "4/0", "5/0"),
                "2/0", Set.of("1/0", "4/0", "5/0"),
                "4/0", Set.of("1/0", "2/0"),
                "5/0", Set.of("1/0", "2/0")
        );

        assertEquals(expectedA01, circuitConnectionsA01);

        Map<String, Set<String>> circuitConnectionsB01 = degreeFactory.circuitConnections(storage, "ROADMB01");

        Map<String, Set<String>> expectedB01 = Map.of(
                "1/0", Set.of("2/0", "5/0", "7/0"),
                "2/0", Set.of("1/0", "5/0", "7/0"),
                "5/0", Set.of("1/0", "2/0"),
                "7/0", Set.of("1/0", "2/0")
        );

        assertEquals(expectedB01, circuitConnectionsB01);
    }

    private NetworkNodes mockRoadmA01() {
        //SRG1
        Destination srg1Destination = destination("4/0", "C2");
        Source srg1source = source("4/0", "C2");

        //SRG2
        Destination srg2Destination = destination("5/0", "C1");
        Source srg2Source = source("5/0", "C1");

        //DEG1
        Source deg1source = source("1/0", "L1");
        Destination deg1Dest = destination("1/0", "L1");

        // DEG2
        Source deg2source = source("2/0", "L1");
        Destination deg2Dest = destination("2/0", "L1");

        NodeConnectionMap deg1NodeConnection = new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.fromIntBits(1))
                .setSource(deg1source)
                .setDestination(
                        Map.of(
                                deg2Dest.key(), deg2Dest,
                                srg1Destination.key(), srg1Destination,
                                srg2Destination.key(), srg2Destination))
                .build();

        NodeConnectionMap deg2NodeConnection = new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.fromIntBits(2))
                .setSource(deg2source)
                .setDestination(
                        Map.of(
                                deg1Dest.key(), deg1Dest,
                                srg1Destination.key(), srg1Destination,
                                srg2Destination.key(), srg2Destination))
                .build();

        NodeConnectionMap srg1NodeConnection = new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.fromIntBits(3))
                .setSource(srg1source)
                .setDestination(
                        Map.of(
                                deg1Dest.key(), deg1Dest,
                                deg2Dest.key(), deg2Dest))
                .build();

        NodeConnectionMap srg2NodeConnection = new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.fromIntBits(4))
                .setSource(srg2Source)
                .setDestination(
                        Map.of(
                                deg1Dest.key(), deg1Dest,
                                deg2Dest.key(), deg2Dest))
                .build();

        return new NetworkNodesBuilder()
                .setNodeConnectionMap(
                        Map.of(
                                deg1NodeConnection.key(), deg1NodeConnection,
                                deg2NodeConnection.key(), deg2NodeConnection,
                                srg1NodeConnection.key(), srg1NodeConnection,
                                srg2NodeConnection.key(), srg2NodeConnection))
                .setNodeId("ROADMA01")
                .build();
    }

    private NetworkNodes mockRoadmB01() {
        //SRG1
        Destination srg1Destination = destination("7/0", "C2");
        Source srg1source = source("7/0", "C2");

        //SRG2
        Destination srg2Destination = destination("5/0", "C1");
        Source srg2Source = source("5/0", "C1");

        //DEG1
        Source deg1source = source("1/0", "L1");
        Destination deg1Dest = destination("1/0", "L1");

        // DEG2
        Source deg2source = source("2/0", "L1");
        Destination deg2Dest = destination("2/0", "L1");

        NodeConnectionMap deg1NodeConnection = new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.fromIntBits(1))
                .setSource(deg1source)
                .setDestination(
                        Map.of(
                                deg2Dest.key(), deg2Dest,
                                srg1Destination.key(), srg1Destination,
                                srg2Destination.key(), srg2Destination))
                .build();

        NodeConnectionMap deg2NodeConnection = new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.fromIntBits(2))
                .setSource(deg2source)
                .setDestination(
                        Map.of(
                                deg1Dest.key(), deg1Dest,
                                srg1Destination.key(), srg1Destination,
                                srg2Destination.key(), srg2Destination))
                .build();

        NodeConnectionMap srg1NodeConnection = new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.fromIntBits(3))
                .setSource(srg1source)
                .setDestination(
                        Map.of(
                                deg1Dest.key(), deg1Dest,
                                deg2Dest.key(), deg2Dest))
                .build();

        NodeConnectionMap srg2NodeConnection = new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.fromIntBits(4))
                .setSource(srg2Source)
                .setDestination(
                        Map.of(
                                deg1Dest.key(), deg1Dest,
                                deg2Dest.key(), deg2Dest))
                .build();

        return new NetworkNodesBuilder()
                .setNodeConnectionMap(
                        Map.of(
                                deg1NodeConnection.key(), deg1NodeConnection,
                                deg2NodeConnection.key(), deg2NodeConnection,
                                srg1NodeConnection.key(), srg1NodeConnection,
                                srg2NodeConnection.key(), srg2NodeConnection))
                .setNodeId("ROADMB01")
                .build();
    }

    private static @NonNull Source source(String circuitPackName, String portname) {
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819
                .node.connection.map.group.node.connection.map.SourceBuilder()
                .setCircuitPackName(circuitPackName)
                .setPortName(portname)
                .build();
    }

    private static @NonNull Destination destination(String circuitPackName, String portname) {
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev260819
                .node.connection.map.group.node.connection.map.DestinationBuilder()
                .setCircuitPackName(circuitPackName)
                .setPortName(portname)
                .build();
    }

    private static NodeConnectionMap nodeConnectionMap(
            long connectionMapNumber,
            String sourceCircuitPackName,
            String... destinationCircuitPackNames) {

        Source source = new SourceBuilder()
                .setCircuitPackName(sourceCircuitPackName)
                .setPortName("port")
                .build();

        Map<DestinationKey, Destination> destinations = new HashMap<>();
        for (String destinationCircuitPackName : destinationCircuitPackNames) {
            Destination destination = new DestinationBuilder()
                    .setCircuitPackName(destinationCircuitPackName)
                    .setPortName("port")
                    .build();
            destinations.put(destination.key(), destination);
        }

        return new NodeConnectionMapBuilder()
                .setConnectionMapNumber(Uint32.valueOf(connectionMapNumber))
                .setSource(source)
                .setDestination(destinations)
                .build();
    }

    private static NetworkNodes networkNodes(String nodeId, NodeConnectionMap... nodeConnectionMaps) {
        Map<NodeConnectionMapKey, NodeConnectionMap> map = new HashMap<>();
        for (NodeConnectionMap nodeConnectionMap : nodeConnectionMaps) {
            map.put(nodeConnectionMap.key(), nodeConnectionMap);
        }

        return new NetworkNodesBuilder()
                .setNodeId(nodeId)
                .setNodeConnectionMap(map)
                .build();
    }
}
